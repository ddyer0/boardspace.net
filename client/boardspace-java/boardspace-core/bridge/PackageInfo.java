package bridge;

/**

The general scheme for boardspace to coexist as mobile and desktop is the the mobile/codename1 branch
has a "compatibility" package to make it look like AWT generation java, and all the classes to implement
this compatibility are in this, the "bridge" package.

Eventually, I decided to meet closer to the middle, so the bridge package in the desktop branch
has grown.  In effect, the two bridge packages constitute a "boardspace compatible" support 
layer.

The big picture for the bridge package is that none of the code is common between the
desktop and mobile versions, but all of the apis are common.

*/