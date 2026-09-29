# Secount ♥ Countdown Studio

**Se**cret + **Count**: secrets revealed only at zero, letter by letter.

One app, two homes — **the exact same UI** on Windows and Android — for
**any kind of countdown**: birthdays, exams, weddings, holidays, trips, work
deadlines, app / game launches.

100% Kotlin, built with Compose Multiplatform: `android/shared` holds all logic
plus every screen, `android/androidApp` is the APK entry point, and
`android/desktopApp` is the Windows entry point. Same `events.json` schema on
both, so files are interchangeable.

## ✨ Features

- **Works for any occasion**: every countdown has a `category` (Birthday, Exam, Wedding,
  Holiday, Trip, App Release, Game Launch, Work...), an emoji `icon`, and its own
  `accent color`. Old event files load fine — missing fields get sensible defaults.
- **Mine / Inbox tabs**: `♥ Mine` holds countdowns you created. `💌 Inbox` holds
  partner surprises — they appear only on D-day, then vanish after the day ends.
  Yearly surprises return each year (old secret shows unless a new one was sent).
- **Modern Material cards**: category pill, `X days left`, secret-armed pill,
  `💬 replies` pill, `✉ DELIVERED` / `👁 SEEN` with AM/PM timestamps,
  progress bar, live monospaced countdown, per-card accent color.
- **Search, filter** (All / Today / Next 7 days / Featured / With secret / Past / To partner)
  **and sort** (next / A–Z / biggest / newest), List/Calendar views, live stats header.
- **Create anything**: duplicate any countdown, per-countdown accent picker +
  icon picker, photo attach, 12-hour AM/PM time entry. New countdowns default
  `To partner` when linked (`Just me` kept as an option).
- **Secret messages**: letter-by-letter reveal (`H` → `Hi` → …) with replay,
  explicit OPEN step, photo included, `✉ Message` + `🎁 Secret` labelled.
- **Conversation**: reply thread with `You` / `Partner` labels, auto-opens on
  new reply, survives edits via thread merge.
- **Online pairing**: connect two devices with 6-letter codes (+ QR / copy-paste).
  Edits re-send versioned, deletes sync to both sides, offline replies/deletes/seens/photos retry.
  Photos sync as encrypted 600px thumbnails. Disconnect needs both sides to agree.
- **App PIN + biometric**: whole app sits behind a PIN (first run `1234`,
  changeable, backoff lockout). Fingerprint/face unlock on Android. Locks only
  on Home/background — never during photo picking, and drafts survive locking.
- **Backup**: plain or password-encrypted (`ENC1…`) export/import.

## 🔐 PIN + Connect

- First-run PIN is `1234`. Enter it to unlock. Use **PIN** to change it, lock now,
  or use fingerprint/face where available.
- Tap **Connect** to see your 6-letter code (+ QR / pairing text). Tell it to your
  partner, enter THEIR code, and when both sides have entered each other's codes
  you are linked.
- Create with `Send to: To partner (...)` (default when linked) to send a countdown.
  You keep the timer + 365-day yearly countdown, they see nothing until zero — then
  they get a secret notification plus a sealed 🎁 card in **💌 Inbox** with OPEN MESSAGE.
  The letter writes itself one character at a time. They can never edit your countdown,
  only reply. `✉ TO PARTNER` / `✉ DELIVERED • 3:30 PM` / `👁 SEEN • …` shows receipts.
- **Disconnect** only severs when both agree: one side requests, the other must
  tap AGREE. Declining keeps you connected. Sever is re-announced for a few minutes
  so both sides always end up disconnected together.

## ▶ Run on Windows (no install)

`Secount.exe` — run the installer, then launch Secount from the Start menu.
No Java needed (runtime is bundled). Or double-click `run.bat` (it picks a
Java 17+ runtime for you):

```bat
run.bat
```

> Do **not** double-click `Secount.jar` directly if your `.jar` files are
> associated with an old Java 8 — it will fail with
> `UnsupportedClassVersionError`. `run.bat` avoids that. `Secount.jar`
> shows the exact same UI as the Android app, packaged with everything it needs.

## 📱 Run on Android (test APK)

`Secount-debug.apk` — copy it to your phone, open it, allow "install unknown
apps" once. Requires Android 8.0 (API 26) or newer.

## 🛠 Build

Windows app (same UI as the APK):

```bat
build.bat
```

Builds `Secount.jar` + `Secount.exe` (same UI as the APK).

Android APK:

```bat
android\build-apk.bat
```

Both reuse your local Android SDK and auto-download JDK 17 + Gradle on first run.
Shared-logic tests: `gradle -p android :shared:desktopTest`.

GitHub Actions (Windows runner) runs the tests and rebuilds
`Secount.jar` + `Secount.exe` + `Secount-debug.apk` on every push
to `main`.
