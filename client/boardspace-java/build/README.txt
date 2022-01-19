
Steps to deploy a new version of Boardspace to the server

the binaries from which the server builds are made live in boardspace-html/htdocs/java/v101/
scripts compile the development sources and deposit .class files there.   Any changed images
or other resources have to be manually copied there.

I use a program called "kawa" to comple the development sources to the v101 directory.

The compiled sources have to be packaged into jar files, and the jar files signed.  This is
done by a batch file "make-online-jar.bat".  This deposits the final jars in java/v102
For convenience, the manifest files (for the .jars) and the script that builds the jars
are in the "text" folder of the kawa projects.

Upload the v102 folder to the server.   Rename it to a new directory.  edit the "jws" link
on the server to point to the new directory.  edit cgi-bin/include.pl on the server to 
point to the new directory.

The v102 version just built can be tested by running v102/onlineLobby.jar

Running boardspace.net from your desktop should now trigger a refresh of the cache
in boardspace.net.jarcache and run the new client code.


----

Adding new games or assets

Images or other assets need to be copied from development locations to java/v101

If new folders are added to the hierarchy, the manifest files in v101 have to be
edited to include them, and the make-online-jar script modified to make a new 
jar file.


