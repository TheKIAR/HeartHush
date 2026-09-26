@echo off
setlocal EnableDelayedExpansion
REM HeartHush launcher - picks a Java 17+ runtime explicitly.
REM (Double-clicking the .jar directly fails on machines where .jar
REM files are still associated with an old Java 8.)
set "JAVACMD="
if exist "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" set "JAVACMD=C:\Program Files\Java\jdk-25.0.2\bin\java.exe"
if not defined JAVACMD if exist "%LOCALAPPDATA%\HeartHush\tools\jdk-17\bin\java.exe" set "JAVACMD=%LOCALAPPDATA%\HeartHush\tools\jdk-17\bin\java.exe"
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
"!JAVACMD!" -jar "%~dp0HeartHush.jar"
