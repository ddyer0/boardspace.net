
#
# re-signs the boardspace.exe created by jbuilder.  First copy the installed pacakge from c:\program files\boardspace
# then make the executable writable
#
.\signtool.exe sign /p BSkeystore /f "g:\share\usr\ddyer\crypto\Boardspace-2020.p12" ".\Boardspace\Boardspace.exe"
#
# then make the executable not writable
#
# then re-package with some msi maker.
#
