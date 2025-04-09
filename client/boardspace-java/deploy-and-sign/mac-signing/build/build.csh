#!/bin/csh
#
# this uses jar2app to create a mac executable.  It works fine, but
# doesn't include a JVM, and isn't acceptable to the app store.
# these apps can be run, but require separately installing java,
# and some manual mother-may-i to allow it to run.
#
./jar2app.py boardspace.jar -i boardspace.icns -n "boardspace" -d "boardspace.net" -b "com.boardspace.launcher" -v 2.25



