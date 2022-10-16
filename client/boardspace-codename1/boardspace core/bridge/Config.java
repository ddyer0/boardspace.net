package bridge;

import common.CommonConfig;

public interface Config extends CommonConfig{
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
    static final String DictionaryDefsDir = "/appdata/dictionarydefs/";
    static final String IconsDir = "/appdata/icons/";
    // if true, use the NativeSockets implementation for networking
    // to make the overall code structure more like the codename1 implementation
    static final boolean USE_NATIVE_SOCKETS = true; 
   public static final int standardDisplayDensity = 96;
   public static final String DICEPATH = "/dice/";
   static final String feedbackUrl = "https://boardspace.net/cgi-bin/feedback.cgi";

   public static final String FONT_FAMILIES[] =  { "Serif","SansSerif","Monospaced"};

}