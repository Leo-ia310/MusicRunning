@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% neq 0 (
  echo Gradle is not installed. Install Gradle 8.2 or newer to run this project locally.
  exit /b 1
)

gradle %*
