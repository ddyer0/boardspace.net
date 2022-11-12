rem
rem this builds an installer using the official jpackage method
rem

copy g:\share\projects\boardspace-html\htdocs\java\v102\boardspace.jar boardspace.jar
F:/java/jdk-16/bin/jpackage.exe --win-shortcut --win-menu -i boardspace --main-jar "boardspace.jar" -t "msi" --app-version 1.0 --copyright "Dave Dyer" --description "Desktop launcher for Boardspace.net" --icon boardspace_icon_128-indexed.ico
copy Boardspace-1.0.msi g:\share\projects\boardspace-html\htdocs\java\launcher\
