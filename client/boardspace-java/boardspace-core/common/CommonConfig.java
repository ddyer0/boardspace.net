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
package common;

import java.awt.Color;
import java.util.prefs.Preferences;

import lib.G;
import lib.UniversalConstants;

public interface CommonConfig extends UniversalConstants {
	static final String DEFAULT_SERVERNAME = "Boardspace.net";
	static boolean TIMEDROBOTS = false;
	static final String cheerpjTextFile = "cheerpj.txt";
	static boolean TURNBASED = true;

	static String PRELOAD = "preload";		// key for a "preload" option passed from the miniloader
	static boolean PRELOAD_DEFAULT = true;	// still true

	static boolean USE_CHATWIDGET = true;	// if true, use the chat widget instead of a chat window
	static boolean SHOW_SITE_MESSAGES = true;	// if true, show messages from the site database on login
	static boolean offlineTableLauncher = true;	// present the table launcher for all offline games
	static boolean StringsHaveDataFiles = true;	// InternationalStrings uses files
	static boolean AllowOfflineRobots = false;
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
	static final String seatingHelpUrl = "/english/offline-launcher-help.html";
	
	// for deep tessting only
	// static final String showURL = "/cgi-bin/util/show-form.pl";
	static final String loginURL = "/cgi-bin/login.cgi";
	// this url is used to post errors to be logged by the server when there is no connection
	// and also to fetch various types of information from the server
	static final String getEncryptedURL = "/cgi-bin/bs_query.cgi";
	//
	// ops to support offline turn based games
	//
	static final String getTurnbasedURL = "/cgi-bin/bs_offline_ops.cgi";
	static final String turnbasedHelpURL = "/english/about_turnbased.html";
	
	static final String editURL = "/cgi-bin/edit.cgi";
	static final String getPicture = "/cgi-bin/tlib/getpicture.cgi";
	static final String uploadPicture = defaultProtocol + "//boardspace.net/english/pictureupload.html";
	static final String homepageUrl = "/english/links-page.shtml";
	static final String recoverPasswordUrl = "/cgi-bin/lost_password.cgi";
	static final String messagesUrl = "/cgi-bin/messageboard.cgi";
	static final String forumsUrl = "/BB/";
	static final String tournamentUrl = "/cgi-bin/tournament-signup.cgi";
	static final String getAppUrl = "/english/login-jws.shtml";
	static final String postRegisterUrl = "/cgi-bin/bs_register.cgi" ;	// where to send a registration form

	static final String DataCacheUrl = "/cgi-bin/applettag.cgi?tagname=appdata";
	
    public static final String guestName = "guest";
    // bs_uni2 adds master scoring
    // bs_uni3 adds ladder scoring
    static final String recordKeeperURL = "/cgi-bin/bs_uni3.cgi";
    // bs_uni12 is used for 3-12 player scoring
    // bs_uni4 is used for 3-6 player scoring
    // bs_uni5 adds ladder scoring
    static final String recordKeeper4URL = "/cgi-bin/bs_uni12.cgi";
    // bs_uni1 is a new script used for crosswordle, and probably other 1 player games
    static final String recordKeeper1URL = "/cgi-bin/bs_uni1_score.cgi";
    
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
	static final String loginKeyPrefix = "/login/";
	public static final String langKey = loginKeyPrefix + "langKey";
	public String loginNameKey = loginKeyPrefix + "namekey";
	public String loginUidKey = loginKeyPrefix + "uidkey";
	
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
	String GAMESERVERNAME = "gameservername";
	String TESTVERSION = "testversion";
	String SERVERKEY = "serverkey";
	String PROTOCOL = "protocol";
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
    static final String FRAMEBOUNDS = "framebounds";
	static Preferences prefs = Preferences.userRoot();
	
	// defaults that are remembered between sessions
	static enum Default
	   {  
		   sound("true"),
		   announce("true"),
		   fontsize(""+standardFontHeight),
		   ticktock("true"),
		   colorblind("false"),
		   autodone("false"),
		   boardMax("false"),
		   showgrid("false"),
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
		"italian",
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
