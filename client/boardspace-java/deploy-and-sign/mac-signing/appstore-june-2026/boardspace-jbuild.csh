#!/bin/csh
set echo
set key = "3rd party mac developer application"
set ikey = "3rd Party Mac Developer Installer"

xattr -cr boardspace/
cp boardspace/boardspace.app.entitlements mac.entitlements
cp boardspace/sandbox.entitlements sandbox.entitlements

rm -rf boardspace.app
rm -rf boardspace-raw.app

./jpackager -i boardspace \
    --mac-app-category games \
    --mac-entitlements mac.entitlements \
    --mac-app-store \
    --main-jar boardspace.jar \
    -t "app-image" \
    --app-version $1 \
    --copyright "Dave Dyer" \
    --description "desktop launcher for boardspace.net" \
    --icon boardspace.icns \
    --name "boardspace" \
    --mac-package-name "Boardspace.net" \
    --mac-package-identifier "com.boardspace.launcher"


vtool -set-build-version macos 10.4 8.0 -output boardspace.app/Contents/MacOS/boardspace boardspace.app/Contents/MacOS/boardspace

vtool -show-build-version boardspace.app/Contents/MacOS/boardspace

#cp boardspace/boardspace.provisionprofile boardspace.app/Contents/embedded.provisionprofile

# Sign loose dylibs, frameworks, and Java executable binaries from the inside out
foreach file (`find boardspace.app/Contents/runtime -type f \( -name "*.dylib" -o -name "*.so" \)`)
    codesign --force --timestamp --options runtime --sign "$key" "$file"
end

codesign --force \
         --sign "$key" \
         --entitlements sandbox.entitlements \
         "boardspace.app/Contents/MacOS/boardspace"


codesign --force  \
         --sign "$key" \
         --entitlements sandbox.entitlements \
          "boardspace.app/Contents/Runtime/Contents/Home/lib/jspawnhelper"


codesign --force \
         --sign "$key" \
         --entitlements sandbox.entitlements \
         "boardspace.app"

codesign -d --entitlements - boardspace.app
codesign --verify --deep --verbose=4 "boardspace.app"

productbuild --component boardspace.app \
	 /Applications \
	--sign "3rd Party Mac Developer Installer"  \
	--identifier "com.boardspace.app.launcher" \
	boardspace.pkg 
