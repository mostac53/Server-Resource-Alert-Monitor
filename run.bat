@echo off
cd /d "%~dp0"
if not exist Main.class (
  echo Main.class not found. Compiling first...
  javac *.java
  if errorlevel 1 (
    echo.
    echo Compilation failed.
    pause
    exit /b 1
  )
)
java Main
pause
