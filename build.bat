@echo off
setlocal EnableDelayedExpansion
REM ============================================================
REM  Secount Windows app - the SAME shared UI as the Android APK.
REM  Builds Secount.jar (no install) + Secount.exe (installer).
REM ============================================================
call "%~dp0android\tools.bat"
if errorlevel 1 exit /b 1

call "!GRADLE_HOME!\bin\gradle.bat" -p "%~dp0android" :desktopApp:packageUberJarForCurrentOS :desktopApp:packageExe
if errorlevel 1 (
  echo DESKTOP BUILD FAILED
  exit /b 1
)
if exist "%~dp0android\desktopApp\build\compose\jars\Secount-windows-x64-1.0.0.jar" (
  copy /y "%~dp0android\desktopApp\build\compose\jars\Secount-windows-x64-1.0.0.jar" "%~dp0Secount.jar" >nul
) else (
  copy /y "%~dp0android\desktopApp\build\compose\jars\desktopApp-windows-x64-1.0.0.jar" "%~dp0Secount.jar" >nul
)
copy /y "%~dp0android\desktopApp\build\compose\binaries\main\exe\Secount-1.0.0.exe" "%~dp0Secount.exe" >nul
echo.
echo BUILD OK - Secount.jar (same UI as the Android app) + Secount.exe installer.
