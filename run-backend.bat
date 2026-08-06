@echo off
setlocal EnableExtensions EnableDelayedExpansion
title BICAP Backend
echo ==========================================
echo Starting BICAP Backend (Spring Boot)...
echo ==========================================

rem Default to local H2 for development so the app can start reliably on Windows.
set "SPRING_DATASOURCE_URL=jdbc:h2:mem:bicap_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL"
set "SPRING_DATASOURCE_USERNAME=sa"
set "SPRING_DATASOURCE_PASSWORD="
set "SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver"
set "SERVER_PORT=8080"
set "JWT_SECRET=dGVzdC1qd3Qtc2VjcmV0LWtleS1hdC1sZWFzdC0zMi1ieXRlcw=="
set "SEPAY_API_KEY=test-sepay-api-key"
set "DDL_AUTO=create"

echo Using local H2 database for development.
echo.
echo Launching backend using Maven...
call mvn spring-boot:run
if !ERRORLEVEL! NEQ 0 (
    echo.
    echo [ERROR] Backend failed to start or stopped with exit code !ERRORLEVEL!.
    pause
)
endlocal
