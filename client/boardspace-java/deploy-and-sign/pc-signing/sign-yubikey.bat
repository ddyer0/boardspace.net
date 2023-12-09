#
# rem this signing script works with a certificate hidden in my yubikey.
#
@echo on
if "%1" == "" goto HELP
if "%2" == "" goto HELP
if "%3" == "" goto HELP
rem
rem See sign4j.README.txt and https://ebourg.github.io/jsign/ for more information.
rem this version uses the standard windows signtool to do the signing 
rem 
copy %1 %2
rem #
rem #
sign4j.exe --verbose signtool sign /sha1 %3 /fd SHA256 /tr "http://timestamp.digicert.com"  %2

goto END

:HELP
echo Usage: sign.bat inputfile outputfile fingerprint

:END
