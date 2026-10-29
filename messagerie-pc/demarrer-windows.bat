@echo off
cd /d "%~dp0"
if not exist "target\messagerie-pc-1.0.0.jar" (
  echo Compilation Maven necessaire : mvn clean package
  pause
  exit /b 1
)
java -jar "target\messagerie-pc-1.0.0.jar"
if errorlevel 1 pause
