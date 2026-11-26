@echo off
rem Build migs.jar from the command line, without Eclipse.
rem Needs a JDK 16 or later (javac and jar) on the PATH.
rem Compiles into build\classes, not bin, so it doesn't disturb Eclipse's output folder.
setlocal
cd /d "%~dp0.."

where javac >nul 2>nul
if errorlevel 1 (
  echo javac not found: install a JDK 16 or later and put its bin folder on the PATH
  exit /b 1
)

if exist build\classes rmdir /s /q build\classes
mkdir build\classes

rem all the sources are in one package; javac expands the wildcard itself
javac -Xlint:all --release 16 -encoding UTF-8 -d build\classes src\net\boardspace\migs\*.java
if errorlevel 1 (
  echo compile failed
  exit /b 1
)

jar --create --file migs.jar --main-class net.boardspace.migs.Migs -C build\classes .
if errorlevel 1 (
  echo jar failed
  exit /b 1
)

echo built migs.jar
endlocal
