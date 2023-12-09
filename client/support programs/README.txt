
Kawa is a long-obsolete windows Java IDE, which still works but ought to be replaced.
It's contribution to the build process is that it provides a guaranteed independent
use of a particular version of javac to produce the .class files actually used.
It requires a registration key, which is "Dave Dyer" "I@FC"

launch4j is a a utility that packages a java jar file inside a windows exe.  
see https://launch4j.sourceforge.net/

sign4j is a hidden utilty inside the package with launch4j

itmstransporter is a windows based uploader for ios builds - the only way
to send a .ipa to the apple store without using a mac.  Unfortunately apple
broke the process in 2023 and hasn't repaired it.
see https://help.apple.com/itc/transporteruserguide/en.lproj/static.html

upload.cmd is a sample command file to invoke itmstransporter

wix is a toolset for generating windows installers
https://wixtoolset.org/
https://github.com/wixtoolset/



