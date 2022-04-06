package common;

import bridge.Color;
import bridge.Preferences;
import lib.G;

public interface CommonConfig {
	static final String DEFAULT_SERVERNAME = "Boardspace.net";
	static boolean TIMEDROBOTS = false;
	
	// set TRUE oct 2018.  for 2.82
	// This should be completely safe by now
	// since approximately version 2.12 was the last version that
	// didn't handle utf8 
	static boolean USE_COLORMAP = true;	// if true, use colormap info in live games
	static boolean USE_CHATWIDGET = true;	// if true, use the chat widget instead of a chat window
	static boolean SHOW_SITE_MESSAGES = true;	// if true, show messages from the site database on login
	static boolean offlineTableLauncher = true;	// present the table launcher for all offline games
	static boolean StringsHaveDataFiles = true;	// InternationalStrings uses files
	// not complete. Needs replacement for chat windows and text input windows first
	// also arithmetic for pointing and dragging when rotated
	static boolean PINCHROTATION = false;		
	static final String defaultProtocol = "https:";
	static final int NORMAL_HTTP_PORT = 80;
    static final int ALTERNATE_HTTP_PORT = 4321;
    static final int RpcPort = 5004;
	static final int BitmapSharingPort = 5003;
	static final int UdpBroadcastPort = 5002;
	
	static final String StonesDir = "/stones/images/";
	static final String standingsURL = "/cgi-bin/boardspace_rankings.cgi";
	static final String mapDataURL = "/cgi-bin/tlib/gs_locations.cgi";
	static final String rankingURL = "/cgi-bin/uni_rank.cgi";
	// for deep tessting only
	// static final String showURL = "/cgi-bin/util/show-form.pl";
	static final String loginURL = "/cgi-bin/login.cgi";
	// this url is used to post errors to be logged by the server when there is no connection
	// and also to fetch various types of information from the server
	static final String getEncryptedURL = "/cgi-bin/bs_query.cgi";
	static final String editURL = "/cgi-bin/edit.cgi";
	static final String getPicture = "/cgi-bin/tlib/getpicture.cgi";
	static final String uploadPicture = defaultProtocol + "//boardspace.net/english/pictureupload.html";
	static final String homepageUrl = "/english/links-page.shtml";
	static final String recoverPasswordUrl = "/cgi-bin/lost_password.cgi";
	static final String feedbackUrl = "mailto:gamemaster@boardspace.net";
	static final String postRegisterUrl = "/cgi-bin/bs_register.cgi" ;	// where to send a registration form
	
	static final String DataCacheUrl = "/cgi-bin/applettag.cgi?tagname=appdata";
	static final String[] BlacklistedDataFiles = {"euphoria-recruits.res"};
	
    public static final String guestName = "guest";
    // bs_uni1 had just the basic scoring
    // bs_uni2 adds master scoring
    // bs_uni3 adds ladder scoring
    static final String recordKeeperURL = "/cgi-bin/bs_uni3.cgi";
    // bs_uni4 is used for 3-6 player scoring
    // bs_uni5 adds ladder scoring
    static final String recordKeeper4URL = "/cgi-bin/bs_uni5.cgi";
    static final String LANGUAGECLASS = "online.language.";
	static final String DefaultLanguageName = "english";
	static final String DefaultLanguageClass = LANGUAGECLASS + DefaultLanguageName + "Strings";
    static final String PasswordParameterName = "password";
    static final String PlatformParameterName = "platform";
    static final String TimezoneParameterName = "timezone";
    static final String IdentityParameterName = "identity";
    static final String EmailParameterName = "email";
    static final String RealnameParameterName = "realName";
    
	 public static final String PREFERREDLANGUAGE = "Preferred Language";
 
    //
    // progress note on the proxy feature.  It's not complete.  It's not turned on
    // in the production server, but it is on in the development environment, so uncomment
    // this PROXY_HTTP_PORT to resume development
	/**
	 * this is the standard list of web server sockets, which are tried one at a time.
	 * Normally, this list only contains the default http port, 80
	 */
    static final int[] web_server_sockets = 
    	{ //PROXY_HTTP_PORT,
    	  NORMAL_HTTP_PORT ,
    	//  ALTERNATE_HTTP_PORT 
    	};
	static final String keyPrefix = "/login/";
	public static final String langKey = keyPrefix + "langKey";
	static final String regPrefix = "/register/";
    public static final int standardWindowHeight = 600; 	// used for scaling overall font size
    public static final int standardWindowWidth = 600;
	public static final int standardFontHeight = 12;
    public static final int minFontHeight = 8;
    public static final int maxFontHeight = 40;
     
	Color FrameBackgroundColor = new Color(0xdee3f6);	// boardspace blue
	String DEVELOPHOST = "develophost";
	String RELEASEHOST = "releasehost";
	String SERVERNAME = "serverName";
	String TESTSERVER = "testserver";
	String REVIEWONLY = "reviewonly";
	String SERVERKEY = "serverkey";
	String PROTOCOL = "protocol";
	String WebStarted = "webstarted";	// true if we are started by java web start

	String icon_image_name = "boardspace-icon-small.png";

	String lobby_icon_image_name = "lobby-icon.png";
	// fuel for expandClassName
	static final String LOBBYCLASSBASE = "online.common";
	static final String GAMECLASSBASE = "online";
	static final String DefaultGameClass = "zertz.classname";
	static final String installerPackageName = "com.boardspace";
    static final String amazonAppStoreUrl = "amzn://apps/android?p="+installerPackageName;
    static final String androidAppStoreUrl = "https://play.google.com/store/apps/details?id="+installerPackageName;
    static final String iosAppStoreUrl = "https://itunes.apple.com/us/app/id1089274351";
	static final String APPNAME = "Boardspace.net";
	static Preferences prefs = Preferences.userRoot();
	
	// defaults that are remembered between sessions
	static enum Default
	   {  
		   sound("true"),
		   announce("true"),
		   fontsize(""+standardFontHeight),
		   ticktock("true"),
		   ;
		   String value;
		   Default(String v) { value = v; }
		   private static void save(Default name,String v)
		   {
			   prefs.put("Defaults_"+name.name(),v);
		   }
		   public static boolean getBoolean(Default n)
		   {	
		   		String v = prefs.get("Defaults_"+n.name(),n.value);
		   		return(G.BoolToken(v));
		   }
		   public static void setBoolean(Default v,boolean val)
		   {	save(v,val?"true":"false");
		   }
		   public static String getString(Default n)
		   {	
		   		String v = prefs.get("Defaults_"+n.name(),n.value);
		   		return(v);
		   }
		   public static void setString(Default n,String value)
		   {	
		   		save(n,value==null?"":value);
		   }
		   public static int getInt(Default n)
		   {	
		   		String v = prefs.get("Defaults_"+n.name(),n.value);
		   		return(G.IntToken(v));
		   }
		   public static void setInt(Default n,int value)
		   {	
		   		save(n,""+value);
		   }
	   }
	public final static Color bsBlue = new Color(0xdee3f6);		// boardspace blue
	public final static Color bsOrange = new Color(220,95,65);// boardspace orange
	public final static Color bsPurple = new Color(0x60,0x22,0x84);	// boardspace purple
	public final static Color bsBrown = new Color(180,124,94);	// boardspace brown
	
	// this list corresponds to the enum in the translations table
	public final static String SupportedLanguages[] = 	
		{
		"english",
		"catala",
		"chinese",
		"chinese-traditional",
		"czech",
		"dutch",
		"esperanto",
		"french",
		"german",
		"greek",
		"japanese",
		"norwegian",
		"polish",
		"portuguese",
		"romanian",
		"russian",
		"spanish",
		"swedish",
		}; 
}
