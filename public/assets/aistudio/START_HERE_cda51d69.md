# MedPrep — START HERE (sirf 4 kaam)

## Kaam 1: Supabase mein database banayein (ek baar, 5 minute)
1. supabase.com kholein -> apna project -> left menu **SQL Editor** -> **New query**.
2. Neeche ki 3 files **ek ek karke** kholein, poora text copy karein, SQL Editor mein paste karein, **Run** dabayein:
   1. `D:\MedPrep\docs\supabase_schema.sql`
   2. `D:\MedPrep\docs\supabase_update_1.sql`
   3. `D:\MedPrep\docs\supabase_update_2.sql`
   (Har file ke baad "Success" aana chahiye.)
3. **Authentication -> Providers -> Email**: "Confirm email" ko **OFF** kar dein (testing aasan ho jati hai).

## Kaam 2: App phone par chalayein
1. `D:\PRODUCTIVITY APP DATA\android studio\bin\studio64.exe` se Android Studio kholein.
2. **Open** -> folder `D:\MedPrep\app` chunein. Pehli baar sync mein kuch minute lagte hain.
3. Phone par Settings -> About phone -> "Build number" 7 baar dabayein -> Developer options -> **USB debugging** on. Phone USB se laga dein.
4. Upar phone ka naam chunein -> **Run** (hara ▶ button).
5. App mein **Sign up** karein (naam, email, password).

## Kaam 3: Khud ko admin banayein
Supabase -> SQL Editor mein yeh likhein (apni email daal kar) aur Run karein:

    update public.profiles set role = 'admin'
    where id = (select id from auth.users where email = 'APNI_EMAIL_YAHAN');

## Kaam 4: Questions daalein
1. `D:\MedPrep\admin-web\start-admin.bat` par double-click karein (window khuli rakhein). Browser mein dashboard khulega.
2. Usi email/password se login karein.
3. **Import Excel** -> "Download template" -> Excel mein apne questions bharein (correct column mein A/B/C/D) -> file upload -> Import.
4. **MCQs** page par jakar Publish karein (ya import karte waqt "published" chunein).
5. Phone mein app -> **Profile -> Sync now**. Questions aa jayenge, aur ab internet ke baghair bhi chalenge.

## Optional: Google se login
`D:\MedPrep\docs\google_signin_setup.md` dekhein.
