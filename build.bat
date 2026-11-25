@echo off
setlocal
cd /d "%~dp0"

set "JAVA_HOME=D:\code\JDK"
set "TOMCAT_HOME=C:\xampp\tomcat"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ============================================
echo  Dompetku JSP - build
echo ============================================

if not exist "lib\mysql-connector-j-8.0.33.jar" (
    echo [deps] Downloading MySQL Connector/J...
    curl -L -o "lib\mysql-connector-j-8.0.33.jar" "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar"
    if errorlevel 1 goto :error
)
if not exist "lib\jstl-1.2.jar" (
    echo [deps] Downloading JSTL 1.2...
    curl -L -o "lib\jstl-1.2.jar" "https://repo1.maven.org/maven2/javax/servlet/jstl/1.2/jstl-1.2.jar"
    if errorlevel 1 goto :error
)
if not exist "lib\pdfbox-2.0.30.jar" (
    echo [deps] Downloading PDFBox...
    curl -L -o "lib\pdfbox-2.0.30.jar" "https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox/2.0.30/pdfbox-2.0.30.jar"
    if errorlevel 1 goto :error
)
if not exist "lib\fontbox-2.0.30.jar" (
    echo [deps] Downloading FontBox...
    curl -L -o "lib\fontbox-2.0.30.jar" "https://repo1.maven.org/maven2/org/apache/pdfbox/fontbox/2.0.30/fontbox-2.0.30.jar"
    if errorlevel 1 goto :error
)
if not exist "lib\commons-logging-1.2.jar" (
    echo [deps] Downloading Commons Logging...
    curl -L -o "lib\commons-logging-1.2.jar" "https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar"
    if errorlevel 1 goto :error
)
if not exist "lib\javax.mail-1.6.2.jar" (
    echo [deps] Downloading JavaMail...
    curl -L -o "lib\javax.mail-1.6.2.jar" "https://repo1.maven.org/maven2/com/sun/mail/javax.mail/1.6.2/javax.mail-1.6.2.jar"
    if errorlevel 1 goto :error
)
if not exist "lib\activation-1.1.1.jar" (
    echo [deps] Downloading Activation...
    curl -L -o "lib\activation-1.1.1.jar" "https://repo1.maven.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar"
    if errorlevel 1 goto :error
)
if not exist "webapp\assets\js\chart.umd.min.js" (
    echo [deps] Downloading Chart.js...
    curl -L -o "webapp\assets\js\chart.umd.min.js" "https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"
    if errorlevel 1 goto :error
)

echo [1/5] Cleaning build folder...
if exist build rmdir /s /q build
mkdir build\classes
mkdir build\webapp

echo [2/5] Compiling Java sources against Tomcat 8.5 (Java 8 bytecode)...
dir /s /b src\main\java\*.java > build\sources.txt
"%JAVA_HOME%\bin\javac.exe" --release 8 -encoding UTF-8 -Xlint:-options ^
  -cp "%TOMCAT_HOME%\lib\servlet-api.jar;%TOMCAT_HOME%\lib\tomcat-jdbc.jar;lib\*" ^
  -d build\classes @build\sources.txt
if errorlevel 1 goto :error

echo [3/5] Copying resources and webapp...
xcopy /e /i /y /q src\main\resources\* build\classes\ >nul
xcopy /e /i /y /q webapp\* build\webapp\ >nul
mkdir build\webapp\WEB-INF\classes
xcopy /e /i /y /q build\classes\* build\webapp\WEB-INF\classes\ >nul
mkdir build\webapp\WEB-INF\lib

echo [4/5] Bundling libraries...
copy /y lib\*.jar build\webapp\WEB-INF\lib\ >nul

echo [5/5] Packaging dompetku.war...
"%JAVA_HOME%\bin\jar.exe" cf build\dompetku.war -C build\webapp .
if errorlevel 1 goto :error

echo.
echo BUILD OK -^> build\dompetku.war
exit /b 0

:error
echo.
echo BUILD FAILED. Periksa pesan error di atas.
exit /b 1
