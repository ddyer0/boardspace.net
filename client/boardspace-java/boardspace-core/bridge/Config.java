package bridge;

import common.CommonConfig;

public interface Config extends CommonConfig
{
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
   public static final String DictionaryDefsDir = "/dictionary/words/";
   public static final String DICEPATH = "/dice/images/";
   static final String IconsDir = "/icons/";
   public static final String FONT_FAMILIES[] = { "Serif","SansSerif","Monospaced","TimesRoman" ,"Helvetica" , "Courier" ,"Dialog", "DialogInput"};

}