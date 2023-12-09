
rem #
rem # signs the boardspace.exe created by launch4j. 
rem #
rem # sign.bat and the sign4j.exe it uses are from sign4j in the launch4j sources
rem # boardspace-2020-l4j is a copy of the usual signing certificate, with the 
rem # alias changed to just "dave" to avoid spaces in the name
rem #
copy g:\share\projects\boardspace-java\deploy-and-sign\pc-signing\unsigned\Boardspace.exe g:\share\projects\boardspace-java\deploy-and-sign\pc-signing\signed\boardspace.exe\Boardspace.exe
.\sign.bat "dave" BSkeystore "g:\share\usr\ddyer\crypto\Boardspace-2020-l4j.p12" "g:\share\projects\boardspace-java\deploy-and-sign\pc-signing\signed\Boardspace.exe"
