This folder contains customization and resources for LastGameBoard. 

#1 The most important output is LastGameBoard.aar, which should be copied to the native/android
directory.  It has the effect of smuggling resources into the apk's "res" directory,
which is not directly supported by codename1.

The "lastgameboard" directory is the prototype for this, which when zipped and renamed,
becomes a hand-constructed android library file.

#2 various metatadata properties have to be added to the apk to make the app appear
in the lastgameboard launcher, and provide the collateral that makes it pretty.  This
should be placed in the "android.xapplication" build hint.   The prototypes for this
metadata is in metadata.txt   The web documentation for this is 
https://lastgameboard.atlassian.net/wiki/spaces/DC/pages/757563417/Displaying+your+Game+in+Gameboard+Experience#Game-Information


#3 to turn off the player drawers, a drawers_off function was added to bridge/platform.java.
The key native android code to implement this is 

	public static final String ANALYTICS_SENDER_PACKAGE = "com.lastgameboard.gameboardservicetest";    
	public static String ACTION_CHANGE_VISIBILITY = "com.lastgameboard.gameboardservice.drawer.action_CHANGE_DRAWER_VISIBLITY";
	public static String EXTRA_CHANGE_VISIBILITY = "com.lastgameboard.gameboardservice.drawer.key.CHANGE_DRAWER_VISIBLITY_STATE";

	public static void setDrawerVisibility(Application application, boolean z) 
		{
	        setDrawerVisibility(application.getApplicationContext(), z);
	    }

	public static void setDrawerVisibility(Context context, boolean z) 
		{
	        Intent intent = new Intent();
	        intent.setAction(ACTION_CHANGE_VISIBILITY);
	        intent.putExtra(EXTRA_CHANGE_VISIBILITY, z ? 1 : 0);
	        intent.setComponent(new ComponentName(ANALYTICS_SENDER_PACKAGE, "com.lastgameboard.gameboardservice.drawer.DrawerVisibilityBroadcastReceiver"));
	        context.sendBroadcast(intent);
	    }

 This was cribbed by analyzing the chess_23.apk, decompiled by a web application "jasx".  


#4 manipulation from a pc using abb.  Make sure the pc is on the same wireless network.

  adb connect <ip>
  adb install -g "c:\users\ddyer\Downloads\Develop-release(2).apk"
  adb pm list packages
  adb shell dumpsys package dev.boardspace	// check permissions
  adb shell pm grant dev.boardspace android.permission.INTERACT_ACROSS_USERS

drawer-off

   adb shell am broadcast -n com.lastgameboard.gameboardservicetest/com.lastgameboard.gameboardservice.drawer.DrawerVisibilityBroadcastReceiver -a com.lastgameboard.gameboardservice.drawer.action_CHANGE_DRAWER_VISIBLITY --ei com.lastgameboard.gameboardservice.drawer.key.CHANGE_DRAWER_VISIBLITY_STATE 0

this can be emulated in the app by code that executes it, but that requires android.permission.INTERACT_ACROSS_USERS
which has to be a permission request in the apk, supplied by the build hint

	android.xpermissions=<uses-permission android:name="android.permission.INTERACT_ACROSS_USERS" value="true" />

and granted by using -g when installing
	adb install -g "c:\users\ddyer\Downloads\Develop-release(2).apk"
or separately
	adb shell pm grant dev.boardspace android.permission.INTERACT_ACROSS_USERS
Check the state of permissions
	adb shell dumpsys package dev.boardspace

this can be emulated in the app by code that executes it, but that requires android.permission.INTERACT_ACROSS_USERS
which has to be a permission request in the apk, supplied by the build hint

	android.xpermissions=<uses-permission android:name="android.permission.INTERACT_ACROSS_USERS" value="true" />

and granted by using -g when installing
	adb install -g "c:\users\ddyer\Downloads\Develop-release(2).apk"
or separately
	adb shell pm grant dev.boardspace android.permission.INTERACT_ACROSS_USERS
Check the state of permissions
	adb shell dumpsys package dev.boardspace

Cribbed from decompiling chess_23.apk, the simpler way is

    

#5 chess_23 apk is sample apk from the store.

