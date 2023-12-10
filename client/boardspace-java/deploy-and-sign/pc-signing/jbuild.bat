rem
rem this builds an installer using the official jpackage method
rem the magic number for sha1 has to be the fingerprint of the current signing certificate
rem
mkdir temp
copy g:\share\projects\boardspace-html\htdocs\java\v102\boardspace.jar temp\boardspace.jar
F:/java/jdk-16/bin/jpackage.exe  --win-shortcut --win-menu -i "g:/share/projects/boardspace-java/deploy-and-sign/pc-signing/temp/" --main-jar "boardspace.jar" -t "msi" --app-version 1.0 --copyright "Dave Dyer" --description "Desktop launcher for Boardspace.net" --icon boardspace_icon_128-indexed.ico --name "Boardspace"
rm -rf temp
copy Boardspace-1.0.msi unsigned\
del Boardspace-1.0.msi
copy unsigned\Boardspace-1.0.msi signed\Boardspace-1.0.msi
signtool sign /sha1 970b8ca191f7fc94e387f93d2e51b6a2f0fb4335 /fd SHA256 /tr "http://timestamp.digicert.com" signed\Boardspace-1.0.msi
