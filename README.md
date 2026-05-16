# College Canteen Android App

Kotlin + Jetpack Compose based Android app for a college canteen.

## Features

- Student and canteen role based login
- Student registration
- Canteen login only; staff accounts are managed by admin
- Student menu browsing
- Cart and food ordering
- Student order history
- Canteen order dashboard
- Order status flow: received, preparing, packed, handed over
- Canteen menu availability toggle
- Supabase Auth, Postgres, and RLS policies

## Project Setup

1. Open this folder in Android Studio with JDK 17 or newer.
2. Create a `local.properties` file using `local.properties.example`:

```properties
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_your_key_here
```

3. In Supabase, open SQL Editor and run:

```sql
-- File: supabase/schema.sql
```

4. Change the demo canteen invite code in `supabase/schema.sql` before real use:

```sql
insert into public.canteen_invite_codes (code)
values ('CANTEEN-2026');
```

5. Run the app from Android Studio, or from terminal after Java is installed:

```powershell
.\gradlew.bat assembleDebug
```

## Supabase Notes

- Use the publishable key in the mobile app, not the service role key.
- The schema enables RLS on all app tables.
- Students can place and view only their own orders.
- Canteen users can view orders, update order status, and manage menu availability.
- Canteen users should be created by an admin or database setup script. The app does not show canteen registration.

## Important

For production, keep canteen onboarding stricter. An invite code is acceptable for a college prototype, but admin approval or a server-side function is better for real deployment.
