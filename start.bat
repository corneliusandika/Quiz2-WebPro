@echo off
setlocal
cd /d "%~dp0"

set "JAVA_HOME=C:\Program Files\Java\jre-1.8"
set "CATALINA_HOME=C:\xampp\tomcat"
set "MYSQL_HOME=C:\xampp\mysql"
set "DOMPETKU_SEED_DEMO=true"
rem HTTPS production: set DOMPETKU_SECURE_COOKIES=true in the server environment.

echo ============================================
echo  Dompetku JSP - start
echo ============================================

if not exist "build\dompetku.war" (
    echo [1/4] WAR belum ada, menjalankan build...
    call build.bat
    if errorlevel 1 exit /b 1
) else (
    echo [1/4] WAR ditemukan.
)

echo [2/4] Memastikan MySQL berjalan...
"%MYSQL_HOME%\bin\mysqladmin.exe" -u root ping >nul 2>&1
if errorlevel 1 (
    start "Dompetku MySQL" /min "%MYSQL_HOME%\bin\mysqld.exe" --defaults-file="%MYSQL_HOME%\bin\my.ini" --standalone
    echo      Menunggu MySQL siap...
    for /l %%i in (1,1,30) do (
        "%MYSQL_HOME%\bin\mysqladmin.exe" -u root ping >nul 2>&1
        if not errorlevel 1 goto mysql_ready
        ping -n 2 127.0.0.1 >nul
    )
    echo      MySQL tidak merespons. Jalankan XAMPP Control Panel lalu coba lagi.
    exit /b 1
) else (
    echo      MySQL sudah berjalan.
)
:mysql_ready

echo [3/4] Deploy WAR ke Tomcat...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo      Menghentikan Tomcat lama agar file tidak terkunci...
    call "%CATALINA_HOME%\bin\shutdown.bat" >nul 2>&1
    for /l %%i in (1,1,15) do (
        netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
        if errorlevel 1 goto tomcat_stopped
        ping -n 2 127.0.0.1 >nul
    )
)
:tomcat_stopped
call deploy.bat
if errorlevel 1 exit /b 1

echo [4/4] Menjalankan Tomcat 8.5 dengan Java 8...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if errorlevel 1 (
    start "Dompetku Tomcat" /min cmd /c "%CATALINA_HOME%\bin\catalina.bat run"
) else (
    echo      Port 8080 sudah dipakai. Pastikan itu Tomcat Dompetku.
)

echo.
echo Menunggu aplikasi siap di http://localhost:8080/dompetku/ ...
for /l %%i in (1,1,45) do (
    curl -s -o nul -w "%%{http_code}" http://localhost:8080/dompetku/login | findstr "200" >nul
    if not errorlevel 1 goto app_ready
    ping -n 2 127.0.0.1 >nul
)
echo Aplikasi belum merespons. Periksa C:\xampp\tomcat\logs.
pause
exit /b 1

:app_ready
echo.
echo ============================================
echo  Dompetku siap!
echo  URL   : http://localhost:8080/dompetku/
echo  Login : demo@dompetku.test / password
echo ============================================
start "" "http://localhost:8080/dompetku/"
