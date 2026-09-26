@echo off
REM Ensures JDK 17 + Gradle 8.13 for HeartHush builds.
REM Exports: JDK17, GRADLE_HOME, ANDROID_HOME, JAVA_HOME. Fails with errorlevel 1.
set TOOLS=%LOCALAPPDATA%\HeartHush\tools
set JDK17=%TOOLS%\jdk-17
set GRADLE_HOME=%TOOLS%\gradle-8.13

if not exist "%JDK17%\bin\java.exe" (
  echo Downloading JDK 17 ...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "New-Item -ItemType Directory -Force \"$env:LOCALAPPDATA\HeartHush\tools\" | Out-Null; Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile \"$env:LOCALAPPDATA\HeartHush\tools\jdk17.zip\"; Expand-Archive -Path \"$env:LOCALAPPDATA\HeartHush\tools\jdk17.zip\" -DestinationPath \"$env:LOCALAPPDATA\HeartHush\tools\jdk17-tmp\" -Force; Get-ChildItem \"$env:LOCALAPPDATA\HeartHush\tools\jdk17-tmp\" | Select-Object -First 1 | ForEach-Object { Move-Item $_.FullName \"$env:LOCALAPPDATA\HeartHush\tools\jdk-17\" -Force }; Remove-Item -Recurse -Force \"$env:LOCALAPPDATA\HeartHush\tools\jdk17-tmp\", \"$env:LOCALAPPDATA\HeartHush\tools\jdk17.zip\""
  if errorlevel 1 (
    echo JDK DOWNLOAD FAILED
    exit /b 1
  )
)
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo Downloading Gradle 8.13 ...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.13-bin.zip' -OutFile \"$env:LOCALAPPDATA\HeartHush\tools\gradle.zip\"; Expand-Archive -Path \"$env:LOCALAPPDATA\HeartHush\tools\gradle.zip\" -DestinationPath \"$env:LOCALAPPDATA\HeartHush\tools\" -Force; Remove-Item \"$env:LOCALAPPDATA\HeartHush\tools\gradle.zip\""
  if errorlevel 1 (
    echo GRADLE DOWNLOAD FAILED
    exit /b 1
  )
)

if not defined ANDROID_HOME if not defined ANDROID_SDK_ROOT (
  if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
)
set "JAVA_HOME=%JDK17%"
exit /b 0
