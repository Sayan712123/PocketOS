# PocketOS

Single lightweight APK (Kotlin, no Compose, ~1 MB) for POCO M4 5G / Android 8+.

## Download the APKs

**[⬇ miui-dialer.apk (v1.1.0)](https://github.com/Sayan712123/PocketOS/releases/download/v1.1.0/miui-dialer.apk)** — 1.5 MB
standalone MIUI-style Phone app (see below)

**[⬇ pocketos.apk (v1.1.0)](https://github.com/Sayan712123/PocketOS/releases/download/v1.1.0/pocketos.apk)** — 104 KB
PocketOS hub / launcher / mini apps

Copy them to the phone and install (allow unknown sources). Signed with the Android debug key:
fine for personal sideloading, not for Play Store upload.

## MIUI Dialer (`:miuidialer` module)

A **separate app** (`com.pocketos.miuidialer`, label *Phone*) that mimics the Xiaomi / POCO MIUI
Phone app. It is not part of the PocketOS hub — install it on its own.

**UI**
- White MIUI look, `#3482FF` accent, three bottom tabs: **Recents / Contacts / Dialpad**.
- Dialpad: circular keys with letter captions (ABC, DEF …), pill search field, green call button,
  backspace with long-press-to-clear, `0` long-press inserts `+`, live contact-name match while typing.
- Recents: call-log rows with direction arrow (↙ incoming, ↗ outgoing, ✕ missed), duration,
  time (HH:mm / Yesterday / dd/MM/yy) and a per-row call button.
- Contacts: coloured initial avatars, name + number, per-row call button, search.
- **Call screen**: full-screen dark gradient, large avatar with initial, name + number,
  `Incoming call` / `Calling…` / live `mm:ss` timer, and circular actions —
  Decline/Answer, Mute, Keypad, Speaker, End call.

**How the real call screen works**
`MiuInCallService` binds `android.telecom.InCallService` (with `IN_CALL_SERVICE_UI` and
`IN_CALL_SERVICE_RINGING` metadata), so Android hands every incoming/outgoing call to this app
and the MIUI screen is drawn instead of the system one. This only happens once the app holds the
**default-dialer role** — open the app and tap **Set default** on the blue banner
(`ROLE_DIALER` on Android 10+, `ACTION_CHANGE_DEFAULT_DIALER` below).

**Permissions** it asks for: contacts, call log, phone state, call phone, answer phone calls.

**Not included:** dual-SIM picker, call recording, video calls, in-call keypad DTMF entry
(the Keypad button is a placeholder), and the MIUI dialer's own settings screens.

## Build on GitHub (no Android Studio needed)

The workflow in `.github/workflows/build.yml` runs on every push to `main`:

1. **build** job — JDK 17 + Gradle 8.7 → `gradle :app:assembleRelease` and
   `gradle :miuidialer:assembleRelease` → uploads artifacts **PocketOS-apk** and **MIUIDialer-apk**.
2. **release** job — downloads both artifacts and publishes them to GitHub Releases as `v1.1.0`
   (`pocketos.apk`, `miui-dialer.apk`).

You can also trigger it manually: **Actions → Build APK → Run workflow**.

If the build fails, open the red run, copy the error lines and send them over — they can be fixed.

## What is inside
- **PocketOS hub**: icon packs (iOS, Round, Mono, Light), Web Apps (URL -> home-screen icon), shortcuts to default-app settings.
- **PocketOS Launcher**: iOS-style home (all apps on pages, dock), applies the selected icon pack to every app instantly. Swipe down on the upper-right part of the home screen to open Control Center.
- **Control Center**: Wi-Fi/Bluetooth/Airplane/Sound panels, flashlight, brightness and volume sliders.
- **Apps**: Calculator, Clock (clock, stopwatch, timer), Dialer, Contacts, Messages, Notes, Files.
- Each module has its own icon, its own Recents card, and Calculator / Clock / Notes / Web Apps can be opened several times at once.

## Known limits of this first version
- Recents replacement (iPhone gesture) is NOT included yet; needs an Accessibility service.
- The **PocketOS hub's own** Dialer places calls but its in-call screen is the system one. For the
  MIUI call screen use the separate `miui-dialer.apk` app above. Messages reads/sends SMS, no MMS,
  sent messages show in the thread only after you become the default SMS app (not implemented yet).
- Timer works only while the app is open; alarms use the system clock app.
- Pinned shortcuts: the launcher asks you to confirm each one. Web Apps share cookies (one WebView profile).
- Brightness slider needs the "Modify system settings" permission once; Files needs "All files access".
- Not compiled/tested by the author on a device; first build may need small fixes.

## Remove default apps
See `ANTIGRAVITY_PROMPT.md`.
