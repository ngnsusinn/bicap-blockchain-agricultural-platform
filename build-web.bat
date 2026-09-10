@echo off
setlocal
title BICAP - Build Frontend vao Spring Boot (1 port)

echo ============================================================
echo  Build 2 ung dung React va dong goi vao Spring Boot (port 8080)
echo    - Farm Portal  : http://localhost:8080/
echo    - Admin Web    : http://localhost:8080/admin/
echo ============================================================

set STATIC=src\main\resources\static

echo.
echo [1/4] Build Farm Portal (frontend)...
pushd frontend
if not exist node_modules call npm install
call npm run build
if errorlevel 1 (popd & echo [ERROR] Farm Portal build that bai & exit /b 1)
popd

echo.
echo [2/4] Build Admin Web (admin-web)...
pushd admin-web
if not exist node_modules call npm install
call npm run build
if errorlevel 1 (popd & echo [ERROR] Admin Web build that bai & exit /b 1)
popd

echo.
echo [3/4] Lap rap static resources...
if exist "%STATIC%" rmdir /s /q "%STATIC%"
mkdir "%STATIC%"
rem robocopy: exit code 0-7 = OK, >=8 = loi
robocopy "frontend\dist" "%STATIC%" /E /NFL /NDL /NJH /NJS >nul
if errorlevel 8 (echo [ERROR] Copy Farm Portal dist that bai & exit /b 1)
mkdir "%STATIC%\admin"
robocopy "admin-web\dist" "%STATIC%\admin" /E /NFL /NDL /NJH /NJS >nul
if errorlevel 8 (echo [ERROR] Copy Admin Web dist that bai & exit /b 1)

echo.
echo [4/4] Hoan tat! Chay backend:
echo     mvn spring-boot:run
echo     hoac: run-backend.bat
echo.
echo truy cap:
echo     Farm Portal : http://localhost:8080/
echo     Admin Web   : http://localhost:8080/admin/
echo.
endlocal
