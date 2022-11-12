@echo off
if "%1" == "" goto HELP
if "%2" == "" goto HELP
if "%3" == "" goto HELP
if "%4" == "" goto HELP
rem
rem See sign4j.README.txt and https://ebourg.github.io/jsign/ for more information.
rem 

sign4j.exe java -jar jsign-2.0.jar --alias "%1" --keystore "%3" --storepass "%2" "%4"
goto END

:HELP
echo Usage: sign.bat certificate-alias store-password keystore filename.exe

:END
