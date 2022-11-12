
rem Signing the windows launcher (for boadspace.exe)
c:
cd \Program Files (x86)\Windows Kits\10\App Certification Kit
rem signtool sign /p BSkeystore /f "g:\share\usr\ddyer\crypto\Boardspace-2020.p12" "g:\share\projects\boardspace-html\htdocs\java\launcher\boardspace.exe"
signtool sign /p BSkeystore /f "g:\share\usr\ddyer\crypto\Boardspace-2020.p12" "g:\share\projects\boardspace-html\htdocs\java\launcher\Boardspace-1.0.msi"
signtool sign /p BSkeystore /f "g:\share\usr\ddyer\crypto\Boardspace-2020.p12" "g:\share\projects\boardspace-java\deploy-and-sign\boardspace.net\boardspace.net.exe"
g:




