#!/bin/csh
./jar2app boardspace.jar -i boardspace.icns
codesign -s "3rd" boardspace.app/Contents/MacOS/JavaAppLauncher
codesign -s "3rd" boardspace.app/Contents/Resources/boardspace.icns
codesign -s "3rd" boardspace.app/Contents/Java/boardspace.jar
codesign -s "3rd" boardspace.app

./jar2app Tantrix.jar -i tantrix.icns
codesign -s "3rd" Tantrix.app/Contents/MacOS/JavaAppLauncher
codesign -s "3rd" Tantrix.app/Contents/Resources/tantrix.icns
codesign -s "3rd" Tantrix.app/Contents/Java/Tantrix.jar
codesign -s "3rd" Tantrix.app

then use "diskutility" to package them as dmgs
