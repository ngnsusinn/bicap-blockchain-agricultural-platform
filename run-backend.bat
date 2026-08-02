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

rem If no external DB is configured, set local H2 defaults for development so the app can start
if "%SPRING_DATASOURCE_URL%"=="" (
    echo No SPRING_DATASOURCE_URL set — using H2 in-memory for local dev
    set "SPRING_DATASOURCE_URL=jdbc:h2:mem:bicap_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL"
    set "SPRING_DATASOURCE_USERNAME=sa"
    set "SPRING_DATASOURCE_PASSWORD="
)

rem If using H2 URL ensure driver class matches (avoid mismatched MySQL driver)
echo %SPRING_DATASOURCE_URL% | findstr /I "jdbc:h2:" >nul
if %ERRORLEVEL%==0 (
    echo Detected H2 JDBC URL — forcing H2 driver
    set "SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver"
)

rem Ensure required secrets for local dev are present (fallbacks)
if "%JWT_SECRET%"=="" (
    echo Setting fallback JWT_SECRET for local dev
    set "JWT_SECRET=dGVzdC1qd3Qtc2VjcmV0LWtleS1hdC1sZWFzdC0zMi1ieXRlcw=="
)
if "%SEPAY_API_KEY%"=="" (
    echo Setting fallback SEPAY_API_KEY for local dev
    set "SEPAY_API_KEY=test-sepay-api-key"
)

rem Default DDL behavior for local runs
if "%DDL_AUTO%"=="" (
    rem For local development with H2 use 'create' to ensure schema builds cleanly
    set "DDL_AUTO=create"
)

echo.
echo Launching backend using Maven...
call mvn spring-boot:run
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Backend failed to start or stopped with exit code %ERRORLEVEL%.
    pause
)
