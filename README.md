# HeartHush ♥ Countdown Studio

A Kotlin + Swing countdown app for **any kind of countdown** — birthdays, exams, weddings,
holidays, trips, work deadlines, **Android app / game launches**, and Valentine's Day too.

100% Kotlin — same `countdown` package and same types (`EventItem`, `EventStore`,
`FuturisticUI`, `MainFrame`, `Main`, `SelfTest`, ...), same `events.json` format.

## ✨ What's new — generic + upgraded GUI

- **Works for any occasion**: every countdown has a `category` (Birthday, Exam, Wedding,
  Holiday, Trip, App Release, Game Launch, Work...), an emoji `icon`, and its own
  `accent color`. Old event files load fine — missing fields get sensible defaults.
- **7 themes**: Valentine, Midnight Android, Ocean, Sunset, Forest, Lavender,
  Porcelain Light. Pick from the toolbar, saved automatically.
- **Modern Material-style cards**: app-icon bubble, category pill, `X days left` pill,
  secret-armed pill, progress bar with %, live monospaced countdown, accent edge.
- **Toolbar**: live search, filter (All / Today / Next 7 / Next 30 / Past / Featured /
  With secret), sort (next / A–Z / newest / biggest), category filter, theme picker.
- **Stats header**: total • today • this week • next event + live clock.
- **Admin extras**: `DUPLICATE` any countdown, `+ SAMPLES` adds birthday/exam/app/trip/
  wedding examples, per-countdown accent swatches + icon picker in the editor.
- **Rounded buttons** with hover/press states, soft shadows, themed dialogs, glowing
  background with floating hearts/dots per theme.

## 💗 Main screen

Each event shows:
- Large live **days / hours / minutes / seconds** countdown.
- Icon, category, target date and public message.
- Progress toward the day + `Today! / Tomorrow / N days left`.
- Clear **LIVE** / **DAY IS HERE** state.
- Featured ★ events appear first.

Valentine's Day is still seeded automatically if missing (Feb 14).

## 💌 Secret message

An admin can prepare a private message for any countdown.

When the target day reaches zero:
1. The app shows **YOU HAVE A MESSAGE** first.
2. The user sees an **OPEN MESSAGE** button.
3. The actual admin-written message is revealed only after the button is pressed.
4. Opening the app on the target day also triggers the prompt.

## 🔐 Admin access

Click **ADMIN LOGIN** at the bottom of the main window.

- First-run password: `admin123`
- After login, **PASSWORD** appears in the admin controls.
- Use it to change the administrator password.
- Admin mode unlocks create, edit, duplicate, delete, ring/test and secret-message controls.

## ▶ Run

Double-click `HeartHush.jar` or:

```bat
java -jar HeartHush.jar
```

The JAR is a fat jar (kotlin-stdlib bundled), so no extra setup is needed.

## 🛠 Build (Kotlin)

```bat
build.bat
```

This compiles `src\countdown\*.kt` with `kotlinc -jvm-target 1.8`, runs the headless
self-test (`countdown.SelfTest`), and rebuilds `HeartHush.jar`. The Kotlin 1.9.24
compiler is auto-downloaded on first run if `kotlinc` is not on `PATH`.

GitHub Actions does the same on every push to `main` and commits the rebuilt JAR.

## 📱 Android app (test APK)

`HeartHush-debug.apk` is a native Android port for testing on your phone:

- Same countdown logic and **same `events.json` schema** (files are interchangeable
  with the desktop app), same `admin123` password scheme.
- Live countdown list with search, filters, and sorting.
- Admin-gated add / edit / duplicate / delete, secret-message reveal, alarm + snooze.
- Requires Android 8.0 (API 26) or newer. Install by copying the APK to your phone
  and opening it (allow "install unknown apps" when asked).

Build it yourself with:

```bat
android\build-apk.bat
```

This reuses your local Android SDK, auto-downloads JDK 17 + Gradle on first run,
and copies the result to `HeartHush-debug.apk`.
