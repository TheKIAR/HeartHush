@echo off
setlocal
rmdir /s /q classes 2>nul
mkdir classes
javac --release 8 -encoding UTF-8 -d classes src\countdown\*.java
if errorlevel 1 (
  echo BUILD FAILED
  exit /b 1
)
java -cp classes countdown.SelfTest
if errorlevel 1 (
  echo SELF-TEST FAILED
  exit /b 1
)
echo Main-Class: countdown.Main> manifest.txt
"C:\Program Files\Java\jdk-25.0.2\bin\jar.exe" cfm CountdownApp.jar manifest.txt -C classes .
echo.
echo BUILD OK - run with run.bat or: java -jar CountdownApp.jar
