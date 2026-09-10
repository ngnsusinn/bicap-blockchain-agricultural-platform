@echo off
title BICAP Frontend
echo ==========================================
echo Starting BICAP Frontend (React + Vite)...
echo ==========================================

rem Load environment variables from .env if it exists in the current folder
if exist .env (
    echo Loading environment variables from .env...
    for /f "usebackq tokens=* eol=#" %%i in (".env") do (
        set "%%i"
    )
)

cd frontend

if not exist node_modules (
    echo node_modules not found. Installing dependencies...
    call npm install
)

echo.
echo Launching frontend server using Vite...
call npm run dev
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Frontend failed to start or stopped with exit code %ERRORLEVEL%.
    pause
)
