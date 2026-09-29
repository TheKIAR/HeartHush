@echo off
setlocal EnableDelayedExpansion
REM Secount launcher - picks a Java 17+ runtime explicitly.
REM (Double-clicking the .jar directly fails on machines where .jar
REM files are still associated with an old Java 8.)
set "JAVACMD="
set "JAVAFLAGS="
REM Prefer the build JDK 17 first: Skiko/Compose runs warning-free on 17.
REM Newer JDKs (25+) print "restricted method System::load" warnings.
if exist "%LOCALAPPDATA%\Secount\tools\jdk-17\bin\java.exe" set "JAVACMD=%LOCALAPPDATA%\Secount\tools\jdk-17\bin\java.exe"
if not defined JAVACMD if exist "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" set "JAVACMD=C:\Program Files\Java\jdk-25.0.2\bin\java.exe"
if defined JAVACMD if exist "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" if "!JAVACMD!"=="C:\Program Files\Java\jdk-25.0.2\bin\java.exe" set "JAVAFLAGS=--enable-native-access=ALL-UNNAMED"
if not defined JAVACMD (
  set "VERLINE="
  for /f "tokens=*" %%a in ('java -version 2^>^&1 ^| findstr /i "version"') do set "VERLINE=%%a"
  if defined VERLINE (
    echo !VERLINE! | findstr /c:"\"1." >nul
    if errorlevel 1 (
      set "JAVACMD=java"
    ) else (
      echo Found Java is too old: !VERLINE!
    )
  )
)
if not defined JAVACMD (
  echo No Java 17+ runtime found. Install a recent JDK, or run build.bat once.
  pause
  exit /b 1
)
if defined JAVAFLAGS (
  "!JAVACMD!" !JAVAFLAGS! -jar "%~dp0Secount.jar"
) else (
  "!JAVACMD!" -jar "%~dp0Secount.jar"
)
