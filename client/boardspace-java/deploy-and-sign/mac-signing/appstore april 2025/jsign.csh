#!/bin/csh

#
# this script signs (and re-signs!) boardspace and tantric apps prepared
# by package.  The deficiencies in package signing have been reported (1/2021)
#
# it also runs productbuild to prepare the apps for upload to the apple store.
# the net product is was accepted by the app store (1/2021).  There are some
# abnormalities installing from the app store on development machines, which
# have been reported. to apple
#
# other steps in this process (there are many!) include creating the 
# apps in the app store, allocating product ids, registering as a 
# developer, acquiring signing keys ...
#

codesign --force -s "3rd Party Mac Developer" --entitlements boardspace/boardspace.app.entitlements boardspace.app/Contents/MacOS/boardspace
codesign --force -s "3rd Party Mac Developer" --entitlements boardspace/boardspace.app.entitlements boardspace.app/Contents/runtime/Contents/Home/lib/jspawnhelper
codesign --force -s "3rd Party Mac Developer" boardspace.app/Contents/runtime/Contents/MacOS/libjli.dylib
codesign --force -s "3rd Party Mac Developer" boardspace.app/Contents/runtime/Contents/Home/lib/*.dylib boardspace.app/Contents/runtime/Contents/Home/lib/server/*.dylib
codesign --force -s "3rd Party Mac Developer" --entitlements boardspace/boardspace.app.entitlements boardspace.app


productbuild --component boardspace.app /Applications --sign "3rd Party Mac Developer Installer"  --identifier "com.boardspace.app.launcher" boardspace.pkg


ls -al tantrix/tantrix.app.entitlements
codesign --force -s "3rd Party Mac Developer" --entitlements tantrix/tantrix.app.entitlements tantrix.app/Contents/MacOS/tantrix
codesign --force -s "3rd Party Mac Developer" --entitlements tantrix/tantrix.app.entitlements tantrix.app/Contents/runtime/Contents/Home/lib/jspawnhelper
codesign --force -s "3rd Party Mac Developer" tantrix.app/Contents/runtime/Contents/MacOS/libjli.dylib
codesign --force -s "3rd Party Mac Developer" tantrix.app/Contents/runtime/Contents/Home/lib/*.dylib tantrix.app/Contents/runtime/Contents/Home/lib/server/*.dylib
codesign --force -s "3rd Party Mac Developer" --entitlements tantrix/tantrix.app.entitlements tantrix.app



productbuild --component tantrix.app /Applications --sign "3rd Party Mac Developer Installer"  --identifier "com.tantrix.app.launcher" tantrix.pkg




