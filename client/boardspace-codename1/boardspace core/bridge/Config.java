/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package bridge;

import common.CommonConfig;

public interface Config extends CommonConfig{
    static boolean NAMEGUESTS = true;
    static final String LANGUAGEPATH = "/languages/translations/";
    static final String SOUNDPATH = "/bsdata/data/";
    static final String IMAGEPATH = "/bsdata/images/";
    static boolean REMOTEVNC = false;
    static boolean REMOTERPC = true;
    
    // this is intended only fordebugging event triggering, not for actual use.
    static boolean WAITFOREVER = false;
	static final String SoundFormat = ".wav";
    static final String SPLASHSCREENIMAGE = IMAGEPATH + "homepage-splash.jpg";
    static final String SMALL_MAP_LOC_URL = IMAGEPATH + "earthshadesmall.jpg";	
    static final String IconsDir = "/appdata/icons/";
    // if true, use the NativeSockets implementation for networking
    // to make the overall code structure more like the codename1 implementation
    static final boolean USE_NATIVE_SOCKETS = true; 
   public static final int standardDisplayDensity = 96;
   public static final String DICEPATH = "/dice/";
   static final String feedbackUrl = "https://boardspace.net/cgi-bin/feedback.cgi";

   public static final String FONT_FAMILIES[] =  { "Serif","SansSerif","Monospaced"};
   // separate data file cache isn't used in the main line java, only in the codename1 branch
   public static final String BlacklistedDataFiles[] = {""};
   
   public static int DEFAULT_SCROLL_BAR_WIDTH = 25;			// default size, should still be scaled by G.getDisplayScale()
   public static final int MenuTextSize = 14;
   //
	// version 7.78 added new logic to the resource manager, so raw files can act as a single
	// resource.  This allows the dictionaries to be loaded without the .res files unfortunate
	// requirement that everything fit in memory.  The dictionarydefs.res can be removed from
	// the /java/appdata/ directory when version 7.77 is extinct
	//
	static final String DictionaryDir = 
			// /appdata/ loads the definitions as raw resource files
			// /appdata/dictionarydefs/ loads definitions from a standard .res file
			"/appdata/";
}