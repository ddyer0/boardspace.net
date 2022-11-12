rem
rem this builds an installer using the official jpackage method
rem
copy g:\share\projects\tantrix.com\htdocs\Tantrix\TGame\TG-jar\Tantrix.jar Tantrix.jar
rem
pwd
C:/java/jdk-16/bin/jpackage.exe --win-shortcut --win-menu -i Tantrix --main-jar "Tantrix.jar" -t "msi" --app-version 1.0 --copyright "Dave Dyer" --description "Desktop launcher for Tantrix.com" 
rem --icon ..\images\tantrix_icon_multi.ico



