@echo off
setlocal EnableDelayedExpansion
REM ============================================================
REM  HeartHush Windows app - the SAME shared UI as the Android APK.
REM  Double-click HeartHush.jar afterwards: no install needed.
REM ============================================================
call "%~dp0android\tools.bat"
if errorlevel 1 exit /b 1

call "!GRADLE_HOME!\bin\gradle.bat" -p "%~dp0android" :desktopApp:packageUberJarForCurrentOS
if errorlevel 1 (
  echo DESKTOP BUILD FAILED
  exit /b 1
)
copy /y "%~dp0android\desktopApp\build\compose\jars\desktopApp-windows-x64-1.0.0.jar" "%~dp0HeartHush.jar" >nul
echo.
echo BUILD OK - double-click HeartHush.jar (same UI as the Android app).
