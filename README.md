# PocketOS

Single lightweight APK (Kotlin, no Compose, ~1 MB) for POCO M4 5G / Android 8+.

## Build on GitHub (no Android Studio needed)
1. Create a new GitHub repo, upload everything in this folder (keep `.github/`).
2. Open **Actions → Build APK** (it runs on every push; or press *Run workflow*).
3. When it turns green, open the run and download artifact **PocketOS-apk**.
4. Unzip, copy `app-release.apk` to the phone, install (allow unknown sources).

If the build fails, copy the red error lines and send them to Claude — it will fix them.

## What is inside
- **PocketOS hub**: icon packs (iOS, Round, Mono, Light), Web Apps (URL -> home-screen icon), shortcuts to default-app settings.
- **PocketOS Launcher**: iOS-style home (all apps on pages, dock), applies the selected icon pack to every app instantly. Swipe down on the upper-right part of the home screen to open Control Center.
- **Control Center**: Wi-Fi/Bluetooth/Airplane/Sound panels, flashlight, brightness and volume sliders.
- **Apps**: Calculator, Clock (clock, stopwatch, timer), Dialer, Contacts, Messages, Notes, Files.
- Each module has its own icon, its own Recents card, and Calculator / Clock / Notes / Web Apps can be opened several times at once.

## Known limits of this first version
- Recents replacement (iPhone gesture) is NOT included yet; needs an Accessibility service.
- Dialer places calls but the in-call screen is the system one. Messages reads/sends SMS, no MMS, sent messages show in the thread only after you become the default SMS app (not implemented yet).
- Timer works only while the app is open; alarms use the system clock app.
- Pinned shortcuts: the launcher asks you to confirm each one. Web Apps share cookies (one WebView profile).
- Brightness slider needs the "Modify system settings" permission once; Files needs "All files access".
- Not compiled/tested by the author on a device; first build may need small fixes.

## Remove default apps
See `ANTIGRAVITY_PROMPT.md`.
