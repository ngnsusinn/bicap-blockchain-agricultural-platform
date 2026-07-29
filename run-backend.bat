@echo off
title BICAP Backend
echo ==========================================
echo Starting BICAP Backend (Spring Boot)...
echo ==========================================

rem Load environment variables from .env if it exists
if exist .env (
    echo Loading environment variables from .env...
    for /f "usebackq tokens=* eol=#" %%i in (".env") do (
        set "%%i"
    )
)

echo.
echo Launching backend using Maven...
call mvn spring-boot:run
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Backend failed to start or stopped with exit code %ERRORLEVEL%.
    pause
)
