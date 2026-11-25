@echo off
setlocal
cd /d "%~dp0"
set "TOMCAT_HOME=C:\xampp\tomcat"

if not exist "build\dompetku.war" (
    echo WAR belum ada. Jalankan build.bat terlebih dahulu.
    exit /b 1
)

echo [deploy] Menyalin WAR ke %TOMCAT_HOME%\webapps ...
if exist "%TOMCAT_HOME%\webapps\dompetku" rmdir /s /q "%TOMCAT_HOME%\webapps\dompetku"
copy /y "build\dompetku.war" "%TOMCAT_HOME%\webapps\dompetku.war" >nul
if errorlevel 1 (
    echo Gagal menyalin WAR. Pastikan Tomcat tidak mengunci file.
    exit /b 1
)
echo [deploy] Selesai. Tomcat akan auto-deploy saat dijalankan / restart.
