# HeartHush ♥ Countdown Studio

One app, two homes — **the exact same UI** on Windows and Android — for
**any kind of countdown**: birthdays, exams, weddings, holidays, trips, work
deadlines, **Android app / game launches**, and Valentine's Day too.

100% Kotlin, built with Compose Multiplatform: `android/shared` holds all logic
plus every screen, `android/androidApp` is the APK entry point, and
`android/desktopApp` is the Windows entry point. Same `events.json` schema on
both, so files are interchangeable.

## ✨ Features

- **Works for any occasion**: every countdown has a `category` (Birthday, Exam, Wedding,
  Holiday, Trip, App Release, Game Launch, Work...), an emoji `icon`, and its own
  `accent color`. Old event files load fine — missing fields get sensible defaults.
- **Modern Material cards**: category pill, `X days left`, secret-armed pill,
  progress bar, live monospaced countdown, per-card accent color.
- **Search, filter** (All / Today / Next 7 days / Featured / With secret)
  **and sort** (next / A–Z / biggest), live stats header.
- **Create anything**: duplicate any countdown, one-tap sample data, per-countdown
  accent picker + icon picker in the editor.
- **Secret messages**: revealed only at zero through an explicit OPEN step.
- Featured ★ events appear first; Valentine's Day is seeded if missing (Feb 14).
- **Online pairing**: connect two devices with 6-letter codes. Countdowns sent
  `To partner` appear on both devices, but the message is revealed only on the
  partner's device at zero. Disconnect needs both sides to agree.
- **App PIN**: whole app sits behind a PIN. First run is `1234`, changeable in PIN settings.

## 🔐 PIN + Connect

- First-run PIN is `1234`. Enter it to unlock. Use **PIN** to change it or lock now.
- Tap **Connect** to see your 6-letter code. Tell it to your partner, enter THEIR
  code, and when both sides have entered each other's codes you are linked.
- Create with `Send to: To partner (...)` to send a countdown. You see the timer,
  they get the message at zero. `✉ TO PARTNER` / `✉ DELIVERED` shows the receipt.
- **Disconnect** only severs when both agree: one side requests, the other must
  tap AGREE. Declining keeps you connected.

## ▶ Run on Windows (no install)

Double-click `run.bat` (it picks a Java 17+ runtime for you):

```bat
run.bat
```

> Do **not** double-click `HeartHush.jar` directly if your `.jar` files are
> associated with an old Java 8 — it will fail with
> `UnsupportedClassVersionError`. `run.bat` avoids that. `HeartHush.jar`
> shows the exact same UI as the Android app, packaged with everything it needs.

## 📱 Run on Android (test APK)

`HeartHush-debug.apk` — copy it to your phone, open it, allow "install unknown
apps" once. Requires Android 8.0 (API 26) or newer.

## 🛠 Build

Windows app (same UI as the APK):

```bat
build.bat
```

Android APK:

```bat
android\build-apk.bat
```

Both reuse your local Android SDK and auto-download JDK 17 + Gradle on first run.
Shared-logic tests: `gradle -p android :shared:desktopTest`.

GitHub Actions runs the tests and rebuilds `HeartHush-debug.apk` on every push
to `main`. (`HeartHush.jar` is Windows-only, so it is built and committed locally.)
