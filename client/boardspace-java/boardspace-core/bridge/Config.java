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

public interface Config extends CommonConfig
{  static boolean NAMEGUESTS = true;
   static final String SOUNDPATH = "/bfms/";
   static final String IMAGEPATH = "/online/images/";
   static boolean REMOTEVNC = false;
   static boolean REMOTERPC = true;
   
   // this is intended only fordebugging event triggering, not for actual use.
   static boolean WAITFOREVER = false;
   
   static final String LANGUAGEPATH = "/languages/translations/";
   static final String SoundFormat = ".au";
   static final String SMALL_MAP_LOC_URL = IMAGEPATH + "earthshadesmall.jpg";

   // if true, use the NativeSockets implementation for networking
    // to make the overall code structure more like the codename1 implementation
   static final boolean USE_NATIVE_SOCKETS = true; 
   public static final int standardDisplayDensity = 96;
 
   // dictionary defs are loaded from the same resource as the basic words
   public static final String DICEPATH = "/dice/images/";
   static final String IconsDir = "/icons/";
   public static final String FONT_FAMILIES[] = { "Serif","SansSerif","Monospaced","TimesRoman" ,"Helvetica" , "Courier" ,"Dialog", "DialogInput"};
   static final String feedbackUrl = "https://boardspace.net/cgi-bin/feedback.cgi";
   // files no longer needed, but which might still be in the online java/appdata/ folder
   
   static final String[] BlacklistedDataFiles = {""};
   
   static final String DictionaryDir = "/dictionary/words/";
}