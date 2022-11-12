
#
# signs the boardspace.exe created by launch4j. 
#
# sign.bat and the sign4j.exe it uses are from sign4j in the launch4j sources
# boardspace-2020-l4j is a copy of the usual signing certificate, with the 
# alias changed to just "dave" to avoid spaces in the name
#
.\sign.bat "dave" BSkeystore "g:\share\usr\ddyer\crypto\Boardspace-2020-l4j.p12" "g:\share\projects\boardspace-html\htdocs\java\launcher\Boardspace.exe"
