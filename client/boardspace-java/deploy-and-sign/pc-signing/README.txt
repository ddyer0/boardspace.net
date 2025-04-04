
PC LAUNCHER APPS

these should be rebuilt when there is a significant change to the launcher jar, Boardspace.jar and/or Tantrix.jar,
or (much longer term) when there is a major change to the jvm needed to run boardspace.

Start with a freshly build boardspace.jar in the \v102 folder

use launch4j and "boardspace-l4j.xml" to produce a new unsigned\boardspace.exe, which is the simplest
launcher that uses the jvm the user has installed.  If there is no jvm to be
found, it will prompt the user to install java.    Using the usual signing tools
on this doesn't work (corrupts the jar file) but there is a workaround buried 
in the launch4j sources, embodied in l4jsign.bat

Sign with l4jsign-yubikey.bat

use launch4j and boardspace-embedded.xml to generate unsigned/boardspace.net.exe
sign with l4jsign-yubikey-embedded.bat
compress to boardspace.net.zip


use "jbuild.bat" to produce a .msi bundled with a jvm - this uses the official
jpackage technology which works but which doesn't handle signing issues.
In addition to a newish jdk, this will require something called "wix toolset"
to be installed and in the path. https://wixtoolset.org/releases/


finally, replace boardspace.exe into boardspace.zip

zip ../boardspace.net directory as boardspace.net.zip

can test in a clean virtualbox environment, download a vm from windows

--

Notes

(obsolete 7/2021 -- jsmooth doesn't work with a modern virgin install)
use "boardspace.jsmooth" to produce a new boardspace.exe, which is the simplest
launcher that uses the jvm the user has installed.  If there is no jvm to be
found, it will prompt the user to install java


ICONS:  create .ico files using https://www.aconvert.com

Subsequent deployment steps:

The delivery files are in htdocs/java/launcher/
copy the new boardspace.jar and boardspace.exe there
create a new .zip containing boardspace.exe


