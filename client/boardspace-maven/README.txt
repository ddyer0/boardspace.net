These build projects are believed to be correct, but haven't been used to make
actual deployments.


Steps to adapt a Hello World script to use

import the project
import the .launch files

* very important * change the default java ide in the eclipse workspace to be a jdk-1.8 distribution

fix the errors by ignoring the maven errors and updating the out of date mavin projectes

add the real source directories using "link source" in the common project
add the resource files to \commomn\src\main\resources
get rid of the css and guibuilder directories

add the codenameone_settings.properties to common, next to the .classpath and .project
  --- edit the launch class name on the line codename1.mainName=
replace the main class in common\src\main\java\
add the native components to javase, android, and ios in src/main/java

add build path resource exclusions to remove *.bak

