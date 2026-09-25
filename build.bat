@echo off
setlocal EnableDelayedExpansion
REM ============================================================
REM  HeartHush Kotlin build - compiles src\countdown\*.kt,
REM  runs the headless SelfTest, rebuilds HeartHush.jar (fat jar
REM  with kotlin-stdlib bundled, so java -jar works everywhere).
REM  The Kotlin compiler is auto-downloaded on first run.
REM ============================================================
set KOTLIN_VER=1.9.24
set KOTLIN_DIR=%LOCALAPPDATA%\HeartHush\kotlin
set KOTLINC=

where kotlinc >nul 2>nul
if %errorlevel%==0 (
  for /f "delims=" %%i in ('where kotlinc') do if not defined KOTLINC set "KOTLINC=%%i"
)
if not defined KOTLINC if exist "%KOTLIN_DIR%\kotlinc\bin\kotlinc.bat" (
  set "KOTLINC=%KOTLIN_DIR%\kotlinc\bin\kotlinc.bat"
)
if not defined KOTLINC (
  echo Kotlin compiler not found - downloading v%KOTLIN_VER% ...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "New-Item -ItemType Directory -Force \"$env:LOCALAPPDATA\HeartHush\kotlin\" | Out-Null; Invoke-WebRequest -Uri 'https://github.com/JetBrains/kotlin/releases/download/v%KOTLIN_VER%/kotlin-compiler-%KOTLIN_VER%.zip' -OutFile \"$env:LOCALAPPDATA\HeartHush\kotlin\kotlin-compiler.zip\"; Expand-Archive -Path \"$env:LOCALAPPDATA\HeartHush\kotlin\kotlin-compiler.zip\" -DestinationPath \"$env:LOCALAPPDATA\HeartHush\kotlin\" -Force"
  if errorlevel 1 (
    echo DOWNLOAD FAILED - install Kotlin manually and put kotlinc on PATH.
    exit /b 1
  )
  set "KOTLINC=%KOTLIN_DIR%\kotlinc\bin\kotlinc.bat"
)

REM kotlinc 1.9.x cannot start on very new JDKs - fall back to the
REM Java 8 runtime for the compiler itself when needed.
call "!KOTLINC!" -version >nul 2>nul
if errorlevel 1 (
  if exist "C:\Program Files\Java\jre1.8.0_503" (
    echo Retrying compiler with Java 8 runtime...
    set "JAVA_HOME=C:\Program Files\Java\jre1.8.0_503"
    call "!KOTLINC!" -version >nul 2>nul
    if errorlevel 1 (
      echo KOTLIN COMPILER FAILED TO START
      exit /b 1
    )
  ) else (
    echo KOTLIN COMPILER FAILED TO START
    exit /b 1
  )
)

for %%i in ("!KOTLINC!") do set "STDLIB=%%~dpi..\lib\kotlin-stdlib.jar"

REM locate jar.exe (not always on PATH)
set JARJAR=jar
where jar >nul 2>nul
if errorlevel 1 (
  set JARJAR=
  if exist "%JAVA_HOME%\bin\jar.exe" set "JARJAR=%JAVA_HOME%\bin\jar.exe"
  if not defined JARJAR if exist "C:\Program Files\Java\jdk-25.0.2\bin\jar.exe" set "JARJAR=C:\Program Files\Java\jdk-25.0.2\bin\jar.exe"
  if not defined JARJAR if exist "C:\Program Files\Java\latest\jdk-25\bin\jar.exe" set "JARJAR=C:\Program Files\Java\latest\jdk-25\bin\jar.exe"
  if not defined JARJAR (
    echo jar.exe NOT FOUND - install a JDK.
    exit /b 1
  )
)

set KTFILES=
for %%f in (src\countdown\*.kt) do set "KTFILES=!KTFILES! src\countdown\%%~nxf"
if not defined KTFILES (
  echo NO KOTLIN SOURCES FOUND in src\countdown\
  exit /b 1
)
rmdir /s /q classes 2>nul
mkdir classes
call "!KOTLINC!" !KTFILES! -jvm-target 1.8 -d classes
if errorlevel 1 (
  echo BUILD FAILED
  exit /b 1
)
java -cp "classes;!STDLIB!" countdown.SelfTest
if errorlevel 1 (
  echo SELF-TEST FAILED
  exit /b 1
)
REM fat jar: bundle kotlin-stdlib so java -jar works everywhere
pushd classes
"!JARJAR!" xf "!STDLIB!"
rmdir /s /q META-INF 2>nul
popd
echo Main-Class: countdown.Main> manifest.txt
"!JARJAR!" cfm HeartHush.jar manifest.txt -C classes .
echo.
echo BUILD OK - run with run.bat or: java -jar HeartHush.jar
