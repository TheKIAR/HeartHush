@echo off
setlocal EnableDelayedExpansion
REM ============================================================
REM  HeartHush Android APK build - the SAME shared UI as the
REM  Windows app. Result: HeartHush-debug.apk, ready to install.
REM ============================================================
call "%~dp0tools.bat"
if errorlevel 1 exit /b 1

call "!GRADLE_HOME!\bin\gradle.bat" -p "%~dp0." :androidApp:assembleDebug
if errorlevel 1 (
  echo APK BUILD FAILED
  exit /b 1
)
copy /y "%~dp0androidApp\build\outputs\apk\debug\androidApp-debug.apk" "%~dp0..\HeartHush-debug.apk" >nul
echo.
echo APK OK - install HeartHush-debug.apk on your phone to test.
