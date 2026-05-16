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
set search_path = public
as $$
    select role from public.profiles where id = auth.uid()
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

alter table public.profiles enable row level security;
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
with check (student_id = auth.uid() and public.current_user_role() = 'student');

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
    ('Tea', 'Hot cutting chai', 'Beverages', 12.00, true),
    ('Cold Coffee', 'Chilled coffee with milk', 'Beverages', 55.00, true),
    ('Samosa', 'Crispy potato samosa', 'Snacks', 18.00, true)
on conflict (name) do nothing;
