#!/bin/csh

rm -rf tantrix.app
./jpackager -i tantrix --runtime-image runtime  --mac-app-category games --mac-app-store --mac-entitlements tantrix/tantrix.app.entitlements --main-jar Tantrix.jar -t "app-image" --app-version $1 --copyright "Colour of Strategy Ltd." --description  "desktop launcher for tantrix.com"  --icon tantrix.icns --name "tantrix" --mac-package-name "Tantrix.com"  --mac-package-identifier "com.tantrix.app.launcher"
vtool -set-build-version macos 10.4 8.0 -output tantrix.app/Contents/MacOS/tantrix tantrix.app/Contents/MacOS/tantrix
vtool -show-build-version tantrix.app/Contents/MacOS/tantrix

rm -rf boardspace.app
./jpackager -i boardspace --runtime-image runtime  --mac-app-category games  --mac-app-store --mac-entitlements boardspace/boardspace.app.entitlements --main-jar boardspace.jar -t "app-image" --app-version $1 --copyright "Dave Dyer" --description  "desktop launcher for boardspace.net"  --icon boardspace.icns --name "boardspace" --mac-package-name "Boardspace.net"  --mac-package-identifier "com.boardspace.launcher" 
vtool -set-build-version macos 10.4 8.0 -output boardspace.app/Contents/MacOS/boardspace boardspace.app/Contents/MacOS/boardspace
vtool -show-build-version boardspace.app/Contents/MacOS/boardspace

