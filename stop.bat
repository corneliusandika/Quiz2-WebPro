@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Java\jre-1.8"
set "CATALINA_HOME=C:\xampp\tomcat"
set "MYSQL_HOME=C:\xampp\mysql"

echo [stop] Menghentikan Tomcat...
call "%CATALINA_HOME%\bin\shutdown.bat"

echo [stop] Menghentikan MySQL...
"%MYSQL_HOME%\bin\mysqladmin.exe" -u root shutdown 2>nul

echo [stop] Selesai.
