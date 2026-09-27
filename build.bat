@echo off
setlocal EnableDelayedExpansion
REM ============================================================
REM  HeartHush Windows app - the SAME shared UI as the Android APK.
REM  Builds HeartHush.jar (no install) + HeartHush.exe (installer).
REM ============================================================
call "%~dp0android\tools.bat"
if errorlevel 1 exit /b 1

call "!GRADLE_HOME!\bin\gradle.bat" -p "%~dp0android" :desktopApp:packageUberJarForCurrentOS :desktopApp:packageExe
if errorlevel 1 (
  echo DESKTOP BUILD FAILED
  exit /b 1
)
if exist "%~dp0android\desktopApp\build\compose\jars\HeartHush-windows-x64-1.0.0.jar" (
  copy /y "%~dp0android\desktopApp\build\compose\jars\HeartHush-windows-x64-1.0.0.jar" "%~dp0HeartHush.jar" >nul
) else (
  copy /y "%~dp0android\desktopApp\build\compose\jars\desktopApp-windows-x64-1.0.0.jar" "%~dp0HeartHush.jar" >nul
)
copy /y "%~dp0android\desktopApp\build\compose\binaries\main\exe\HeartHush-1.0.0.exe" "%~dp0HeartHush.exe" >nul
echo.
echo BUILD OK - HeartHush.jar (same UI as the Android app) + HeartHush.exe installer.
