@REM Maven Wrapper script for Windows
@REM Usage: mvnw.cmd <maven goals>
@echo off
setlocal

set WRAPPER_JAR=%~dp0\.mvn\wrapper\maven-wrapper.jar
set WRAPPER_URL=https://repo1.maven.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip

set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if not exist "%JAVA_EXE%" (
    echo Error: JAVA_HOME is not valid. Cannot find java.exe at %JAVA_EXE%
    exit /b 1
)

set MAVEN_HOME=%~dp0\.mvn\wrapper\apache-maven-3.9.6

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Maven...
    mkdir "%~dp0\.mvn\wrapper\temp" 2>nul
    powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%~dp0\.mvn\wrapper\temp\maven.zip'"
    powershell -Command "Expand-Archive -Path '%~dp0\.mvn\wrapper\temp\maven.zip' -DestinationPath '%~dp0\.mvn\wrapper' -Force"
    del /q "%~dp0\.mvn\wrapper\temp\maven.zip"
    rmdir "%~dp0\.mvn\wrapper\temp"
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
