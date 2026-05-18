-- Run this in Supabase SQL Editor before using the Android app.
-- Replace the demo invite code before sharing the canteen registration flow.

create extension if not exists pgcrypto;

create table if not exists public.canteen_invite_codes (
    code text primary key,
    is_active boolean not null default true,
    created_at timestamptz not null default now()
);

insert into public.canteen_invite_codes (code)
values ('CANTEEN-2026')
on conflict (code) do nothing;

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    full_name text,
    role text not null default 'student' check (role in ('student', 'canteen')),
    phone text,
    created_at timestamptz not null default now()
);

create table if not exists public.student_access_requests (
    id uuid primary key default gen_random_uuid(),
    full_name text not null,
    email text not null,
    password_hash text not null,
    status text not null default 'pending' check (status in ('pending', 'approved', 'denied')),
    requested_at timestamptz not null default now(),
    reviewed_at timestamptz,
    reviewed_by uuid references public.profiles(id),
    user_id uuid references auth.users(id) on delete set null
);

create unique index if not exists student_access_requests_active_email_idx
on public.student_access_requests (email)
where status in ('pending', 'approved');

create table if not exists public.food_items (
    id uuid primary key default gen_random_uuid(),
    name text not null unique,
    description text,
    category text,
    price numeric(10, 2) not null check (price >= 0),
    image_url text,
    is_available boolean not null default true,
    created_at timestamptz not null default now()
);

create table if not exists public.orders (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null references public.profiles(id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'preparing', 'ready', 'completed', 'cancelled')),
    total_amount numeric(10, 2) not null check (total_amount >= 0),
    notes text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.order_items (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references public.orders(id) on delete cascade,
    food_item_id uuid not null references public.food_items(id),
    item_name text not null,
    item_price numeric(10, 2) not null check (item_price >= 0),
    quantity integer not null check (quantity > 0)
);

create or replace function public.current_user_role()
returns text
language sql
stable
security definer
set search_path = public, auth
as $$
    select case
        when exists (
            select 1
            from auth.users u
            where u.id = auth.uid()
              and lower(u.email) = 'admin@gmail.com'
        ) then 'canteen'
        else (
            select role
            from public.profiles
            where id = auth.uid()
        )
    end
$$;

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists orders_touch_updated_at on public.orders;
create trigger orders_touch_updated_at
before update on public.orders
for each row execute function public.touch_updated_at();

create or replace function public.fill_order_item_snapshot()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    food_name text;
    food_price numeric(10, 2);
begin
    select name, price
    into food_name, food_price
    from public.food_items
    where id = new.food_item_id and is_available = true;

    if food_name is null then
        raise exception 'Food item is not available';
    end if;

    new.item_name := food_name;
    new.item_price := food_price;
    return new;
end;
$$;

drop trigger if exists order_items_fill_snapshot on public.order_items;
create trigger order_items_fill_snapshot
before insert on public.order_items
for each row execute function public.fill_order_item_snapshot();

create or replace function public.recalculate_order_total()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    target_order_id uuid;
begin
    if tg_op = 'DELETE' then
        target_order_id := old.order_id;
    else
        target_order_id := new.order_id;
    end if;

    update public.orders
    set total_amount = coalesce(
        (
            select sum(item_price * quantity)
            from public.order_items
            where order_id = target_order_id
        ),
        0
    )
    where id = target_order_id;

    if tg_op = 'DELETE' then
        return old;
    end if;

    return new;
end;
$$;

drop trigger if exists order_items_recalculate_total on public.order_items;
create trigger order_items_recalculate_total
after insert or update or delete on public.order_items
for each row execute function public.recalculate_order_total();

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    requested_role text := coalesce(new.raw_user_meta_data ->> 'role', 'student');
    requested_invite text := new.raw_user_meta_data ->> 'invite_code';
    resolved_role text := 'student';
begin
    if requested_role = 'canteen'
       and exists (
           select 1
           from public.canteen_invite_codes
           where code = requested_invite and is_active = true
       )
    then
        resolved_role := 'canteen';
    end if;

    insert into public.profiles (id, full_name, role)
    values (
        new.id,
        nullif(new.raw_user_meta_data ->> 'full_name', ''),
        resolved_role
    )
    on conflict (id) do update
    set full_name = excluded.full_name;

    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

create or replace function public.prepare_student_access_request()
returns trigger
language plpgsql
security definer
set search_path = public, extensions
as $$
begin
    new.email := lower(trim(new.email));
    new.full_name := trim(new.full_name);

    if new.full_name = '' then
        raise exception 'Full name is required';
    end if;

    if new.email !~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$' then
        raise exception 'Valid email is required';
    end if;

    if new.password_hash is null or length(new.password_hash) < 6 then
        raise exception 'Password must be at least 6 characters';
    end if;

    if new.password_hash !~ '^\$2[abxy]\$' then
        new.password_hash := crypt(new.password_hash, gen_salt('bf'));
    end if;

    new.status := coalesce(new.status, 'pending');
    return new;
end;
$$;

drop trigger if exists student_access_requests_prepare on public.student_access_requests;
create trigger student_access_requests_prepare
before insert or update of full_name, email, password_hash
on public.student_access_requests
for each row execute function public.prepare_student_access_request();

create or replace function public.submit_student_access_request(
    full_name text,
    email text,
    password_hash text
)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
begin
    insert into public.student_access_requests (full_name, email, password_hash)
    values (full_name, email, password_hash);
end;
$$;

create or replace function public.approve_student_access_request()
returns trigger
language plpgsql
security definer
set search_path = public, auth, extensions
as $$
declare
    target_user_id uuid;
begin
    if new.status = 'approved' and old.status is distinct from 'approved' then
        select id
        into target_user_id
        from auth.users
        where lower(email) = new.email
        limit 1;

        if target_user_id is null then
            target_user_id := gen_random_uuid();

            insert into auth.users (
                instance_id,
                id,
                aud,
                role,
                email,
                encrypted_password,
                email_confirmed_at,
                confirmation_token,
                recovery_token,
                email_change_token_new,
                email_change,
                email_change_token_current,
                email_change_confirm_status,
                phone_change,
                phone_change_token,
                reauthentication_token,
                raw_app_meta_data,
                raw_user_meta_data,
                is_super_admin,
                created_at,
                updated_at,
                is_sso_user,
                is_anonymous
            )
            values (
                '00000000-0000-0000-0000-000000000000',
                target_user_id,
                'authenticated',
                'authenticated',
                new.email,
                new.password_hash,
                now(),
                '',
                '',
                '',
                '',
                '',
                0,
                '',
                '',
                '',
                jsonb_build_object('provider', 'email', 'providers', jsonb_build_array('email')),
                jsonb_build_object('role', 'student', 'full_name', new.full_name, 'email_verified', true),
                false,
                now(),
                now(),
                false,
                false
            );
        else
            update auth.users
            set
                instance_id = coalesce(instance_id, '00000000-0000-0000-0000-000000000000'),
                encrypted_password = new.password_hash,
                email_confirmed_at = coalesce(email_confirmed_at, now()),
                confirmation_token = '',
                raw_app_meta_data = jsonb_build_object('provider', 'email', 'providers', jsonb_build_array('email')),
                raw_user_meta_data = coalesce(raw_user_meta_data, '{}'::jsonb)
                    || jsonb_build_object('role', 'student', 'full_name', new.full_name, 'email_verified', true),
                updated_at = now()
            where id = target_user_id;
        end if;

        insert into auth.identities (
            provider_id,
            user_id,
            identity_data,
            provider,
            last_sign_in_at,
            created_at,
            updated_at
        )
        values (
            target_user_id::text,
            target_user_id,
            jsonb_build_object(
                'sub', target_user_id::text,
                'email', new.email,
                'email_verified', true,
                'phone_verified', false
            ),
            'email',
            now(),
            now(),
            now()
        )
        on conflict (provider_id, provider) do update
        set
            identity_data = excluded.identity_data,
            updated_at = now();

        insert into public.profiles (id, full_name, role)
        values (target_user_id, new.full_name, 'student')
        on conflict (id) do update
        set full_name = excluded.full_name,
            role = 'student';

        new.user_id := target_user_id;
        new.reviewed_at := coalesce(new.reviewed_at, now());
    elsif new.status = 'denied' and old.status is distinct from 'denied' then
        new.reviewed_at := coalesce(new.reviewed_at, now());
    end if;

    return new;
end;
$$;

drop trigger if exists student_access_requests_approve on public.student_access_requests;
create trigger student_access_requests_approve
before update of status
on public.student_access_requests
for each row execute function public.approve_student_access_request();

create or replace function public.auto_confirm_email_user()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
    if new.email is not null and new.email_confirmed_at is null then
        new.email_confirmed_at := now();
    end if;

    if new.email_confirmed_at is not null then
        new.confirmation_token := '';
    end if;

    return new;
end;
$$;

drop trigger if exists on_auth_user_auto_confirm_email on auth.users;
create trigger on_auth_user_auto_confirm_email
before insert on auth.users
for each row execute function public.auto_confirm_email_user();

update auth.users
set
    email_confirmed_at = coalesce(email_confirmed_at, now()),
    confirmation_token = ''
where email is not null
  and email_confirmed_at is null;

alter table public.profiles enable row level security;
alter table public.student_access_requests enable row level security;
alter table public.food_items enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;
alter table public.canteen_invite_codes enable row level security;

drop policy if exists profiles_select_own_or_canteen on public.profiles;
create policy profiles_select_own_or_canteen
on public.profiles
for select
to authenticated
using (id = auth.uid() or public.current_user_role() = 'canteen');

drop policy if exists student_access_requests_insert_public on public.student_access_requests;
create policy student_access_requests_insert_public
on public.student_access_requests
for insert
to anon, authenticated
with check (status = 'pending');

drop policy if exists student_access_requests_manage_canteen on public.student_access_requests;
create policy student_access_requests_manage_canteen
on public.student_access_requests
for all
to authenticated
using (public.current_user_role() = 'canteen')
with check (public.current_user_role() = 'canteen');

drop policy if exists student_access_requests_select_own on public.student_access_requests;
create policy student_access_requests_select_own
on public.student_access_requests
for select
to authenticated
using (user_id = auth.uid());

drop policy if exists student_access_requests_select_public_none on public.student_access_requests;
create policy student_access_requests_select_public_none
on public.student_access_requests
for select
to anon
using (false);

drop policy if exists food_items_select_visible on public.food_items;
create policy food_items_select_visible
on public.food_items
for select
to authenticated
using (is_available = true or public.current_user_role() = 'canteen');

drop policy if exists food_items_manage_canteen on public.food_items;
create policy food_items_manage_canteen
on public.food_items
for all
to authenticated
using (public.current_user_role() = 'canteen')
with check (public.current_user_role() = 'canteen');

drop policy if exists orders_select_own_or_canteen on public.orders;
create policy orders_select_own_or_canteen
on public.orders
for select
to authenticated
using (student_id = auth.uid() or public.current_user_role() = 'canteen');

drop policy if exists orders_insert_student_own on public.orders;
create policy orders_insert_student_own
on public.orders
for insert
to authenticated
with check (student_id = auth.uid() and public.current_user_role() in ('student', 'canteen'));

drop policy if exists orders_update_canteen on public.orders;
create policy orders_update_canteen
on public.orders
for update
to authenticated
using (public.current_user_role() = 'canteen')
with check (public.current_user_role() = 'canteen');

drop policy if exists order_items_select_own_or_canteen on public.order_items;
create policy order_items_select_own_or_canteen
on public.order_items
for select
to authenticated
using (
    public.current_user_role() = 'canteen'
    or exists (
        select 1
        from public.orders o
        where o.id = order_id and o.student_id = auth.uid()
    )
);

drop policy if exists order_items_insert_student_own_order on public.order_items;
create policy order_items_insert_student_own_order
on public.order_items
for insert
to authenticated
with check (
    exists (
        select 1
        from public.orders o
        where o.id = order_id
          and o.student_id = auth.uid()
          and o.status = 'pending'
    )
);

grant usage on schema public to anon, authenticated;
grant select on public.profiles to authenticated;
grant execute on function public.submit_student_access_request(text, text, text) to anon, authenticated;
grant insert on public.student_access_requests to anon, authenticated;
grant select, update on public.student_access_requests to authenticated;
grant select on public.food_items to authenticated;
grant insert, update, delete on public.food_items to authenticated;
grant select, insert on public.orders to authenticated;
grant update (status) on public.orders to authenticated;
grant select, insert on public.order_items to authenticated;

insert into public.food_items (name, description, category, price, is_available)
values
    ('Veg Sandwich', 'Grilled bread, chutney, vegetables', 'Snacks', 45.00, true),
    ('Masala Dosa', 'Dosa with potato masala and chutney', 'South Indian', 60.00, true),
    ('Poha', 'Light breakfast with peanuts and sev', 'Breakfast', 30.00, true),
    ('Tea', 'Hot tea', 'Beverages', 12.00, true),
    ('Cold Coffee', 'Chilled coffee with milk', 'Beverages', 55.00, true),
    ('Samosa', 'Crispy potato samosa', 'Snacks', 18.00, true)
on conflict (name) do nothing;
