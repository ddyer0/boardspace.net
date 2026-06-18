Deployment and signing

The unmodified file boardspace.jar should be directly executable on any platform that
has java installed.  In practice, that's not quite universal, and in any case, boardspace.jar
will have only a generic java icon.

To create a native executable for windows, with a nice icon, use Launch4J and a matching signing procedure.  
Details in pc-signing/README.

On Macs, the best tool is "jar2app", which creates a mac ".app" bundle, which
can be signed using the apple developer key (also used to sign the ios app).
Use "applications/utils/diskutility" to create a .dmg and store the app in.

On Macs, jar2app is mostly superceeded by jpackage, seen notes in mac signing folder

