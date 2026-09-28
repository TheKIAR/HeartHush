# Secount — persistent session rule

This is a Compose Multiplatform app (Windows + Android, same UI in `android/shared`).

## Mandatory rule for EVERY prompt in this project

Whenever the user asks for any change (code, UI, logic, fix, feature):

1. Make the code change.
2. ALWAYS rebuild all 3 binaries, even if the user doesn't explicitly ask:
   - `.\build.bat` → rebuilds `Secount.jar` + `Secount.exe` (takes ~2 min)
   - `android\build-apk.bat` → rebuilds `Secount-debug.apk` (takes ~2 min)
3. ALWAYS commit and push everything in the same turn:
   - `git add -A`
   - `git commit -m "<short description>"`
   - `git push` (use long timeout — LFS upload of ~100 MB can take 3-5 min; retry if timeout, check with `git status -sb` for `ahead 1`)
4. Binaries are tracked in Git LFS (`Secount.jar`, `Secount.exe`, `Secount-debug.apk`) — never skip them.
5. Never leave the repo `ahead` of origin. If push times out, run `git push` again until `git status -sb` shows clean.

Do NOT ask the user whether to rebuild/push — just do it.
Only skip rebuild+push for pure questions / read-only exploration with zero file changes.
