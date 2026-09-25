# Special Count ♥ Valentine Edition

A Java Swing countdown app redesigned around a romantic Valentine's theme: animated hearts, glowing glass cards, live countdowns, admin controls, and a private secret-message reveal.

## 💗 Main screen

The countdown is now the main focus of the app. Each event shows:
- Large live **days / hours / minutes / seconds** countdown.
- Target date and public message.
- Clear **COUNTDOWN LIVE** / **COUNTDOWN REACHED ZERO** state.
- Featured events appear first.
- Valentine's Day is automatically added if an older local data file does not already contain one.

Valentine's Day is observed on **February 14**.

## 💌 Secret message

An admin can prepare a private message for any countdown.

When the target day reaches zero:
1. The app shows **YOU HAVE A SECRET MESSAGE** first.
2. The user sees an **OPEN MESSAGE** button.
3. The actual admin-written message is revealed only after the button is pressed.
4. Opening the app on the target day also triggers the secret-message prompt.

The secret-message fields are persisted correctly in the local event file.

## 🔐 Admin access

Click **ADMIN LOGIN** at the bottom of the main window.

- First-run password: `admin123`
- After login, **PASSWORD** appears in the admin controls.
- Use it to change the administrator password.
- Admin mode unlocks create, edit, delete, ring/test and secret-message controls.

## 🌹 Valentine interface

- Floating animated hearts.
- Pink, rose, burgundy and warm-gold palette.
- Glowing background effects.
- Glass-style cards.
- Live clock.
- Romantic labels and micro-interactions.
- The countdown remains readable and prominent instead of being hidden inside the admin UI.

## ▶ Run

Double-click `CountdownApp.jar` or:

```bat
java -jar CountdownApp.jar
```

GitHub Actions compiles the project with Java 8 compatibility, runs the headless self-test, and rebuilds `CountdownApp.jar` automatically.
