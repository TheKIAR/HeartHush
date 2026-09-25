# Special Count

A futuristic Java Swing countdown application for important dates and moments.

## ✨ Features

- Live countdown with days, hours, minutes and seconds.
- Yearly or one-time countdowns.
- Smart dark/glass futuristic interface with an animated grid background.
- Admin mode for creating, editing, deleting and testing countdowns.
- **Secret message system:** an admin can prepare a private message for a countdown. When that countdown reaches zero, the app presents **"You have a secret message"** first, with an **OPEN MESSAGE** button.
- If the app is opened on the special day, the secret prompt appears again for that app session.
- Secret text is stored locally with the countdown data and is never displayed as the normal public message.
- Existing alarm sound/popup support.
- Java 8-compatible source with a Java 21 CI build that packages the executable JAR.

## 🔐 Admin message workflow

1. Log in as admin.
2. Create or edit a countdown.
3. Enable **"Enable secret message at zero"**.
4. Write the private message in **SECRET MESSAGE (ADMIN ONLY)**.
5. Save.
6. On the target date, the app opens the secret prompt first.

The actual secret can be added later by editing the countdown; the feature is already built into the app.

## ▶ Run

Double-click `CountdownApp.jar` or use:

```bat
java -jar CountdownApp.jar
```

## 🧪 Validation

GitHub Actions compiles the source with `--release 8`, runs `countdown.SelfTest`, and rebuilds `CountdownApp.jar` automatically after source changes.
