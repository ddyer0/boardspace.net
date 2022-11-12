Deployment and signing

use create an executable jar from Miniloader.java.  This is done as part
of the jar building script.  The executable jar should be directly executable
on any platform which has java installed, but it's still subject to malware
warnings, and will have a generic "java" icon.

Packaging to a native executable, with a regular icon, and some prompt
to download and install java.

On PCs, the best tool is "jsmooth", which creates boardspace.exe, which can be 
signed using the jar signing certificate and stored in a .zip.  So far, no 
additional benefit seems to come from packaging boardspace.exe into an installer
and then into a .msi

 1) build the usual jar distribution, which is currently deployed in boardspace-html\htdocs\java\v102
 2) run the pc\boardspace.jsmooth, hit the gear to build the executable
 3) open a shell to the pc\ directory, run the signer batch file, which deploys the signed
    exe and installer directly to \v102

On Macs, the best tool is "jar2app", which creates a mac ".app" bundle, which
can be signed using the apple developer key (also used to sign the ios app).
Use "applications/utils/diskutility" to create a .dmg and store the app in.

On Macs, jar2app is mostly superceeded by jpackage, seen notes in mac signing folder


