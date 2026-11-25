@echo off
setlocal
cd /d "%~dp0"
set "MYSQL_HOME=C:\xampp\mysql"

echo [reset] Menghapus dan membuat ulang database dompetku...
"%MYSQL_HOME%\bin\mysql.exe" -u root -e "DROP DATABASE IF EXISTS dompetku; CREATE DATABASE dompetku CHARACTER SET utf8mb4;"
if errorlevel 1 exit /b 1
"%MYSQL_HOME%\bin\mysql.exe" -u root dompetku < database\schema.sql
if errorlevel 1 exit /b 1
"%MYSQL_HOME%\bin\mysql.exe" -u root dompetku < database\seed.sql
if errorlevel 1 exit /b 1
echo [reset] Database siap dengan data demo.
