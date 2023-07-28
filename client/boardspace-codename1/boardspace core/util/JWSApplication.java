package util;

import java.util.StringTokenizer;

import bridge.*;
import bridge.ThreadDeath;
import common.GameInfo;
import lib.CanvasProtocol;
import lib.ConnectionManager;
import lib.DataCache;
import lib.ExtendedHashtable;
import lib.G;
import lib.Http;
import lib.InternationalStrings;
import lib.OfflineGames;
import lib.Plog;
import lib.SoundManager;
import lib.UrlResult;
import lib.XFrame;
import online.common.LPanel;
import online.common.LobbyConstants;
import online.common.OnlineConstants;
import online.common.commonPanel;
import rpc.RpcReceiver;
import udp.PlaytableServer;
import udp.PlaytableStack;
import udp.UDPService;
import vnc.VNCService;

/**
 * this is an entry point for Java Web Start where JWS thinks it is launching
 * and application.  In this case, there is no intrinsic value for getCodeBase
 * or getDocumentBase, so we have to supply one all the time.
 *  
 * this can also be used in the mode where lots of parameters are supplied so no
 * web server is needed. The decision is made based on "serverkey" parameter, for
 * which a dummy value bypasses contacting the real server.
 * 
 * Overview of Java Web Start implementation for Boardspace.
 * 
 * The applets run a login dialog, and submits the result to the regular login form
 * at Boardspace.net, with the addition of a "from jws" tag.  The effect of this tag
 * is to make the output succinct and parseable by the applet, which parses the
 * result and creates parameters which match those which the regular login parameter
 * would have received from the web form.
 * 
 * The running applet is completely unaware it was started in any unusual way,
 * except when it terminates it needs to clean up by exiting the java vm
 * 
 * The .jnlp files which launch the web start process are in /java/ just
 * above the active class hierarchies.  They depend on a small server side
 * include to get the current class hierarchy.
 * 
 * JWS logins are annotated as JWS+ in the game.login log file.
 * 
 * @author ddyer
 *
 */
public class JWSApplication implements Config,LobbyConstants
{

	// this should be the complete set of applet parameters that
	// can ever be supplied, except for an indefinite number of
	// reviewer directories
	static final String DATALOCATION = "datalocation";

	static String DefaultParameters[][] = {
			{PLAYERS_IN_GAME,"2"},
			{CHATWIDGET,""+USE_CHATWIDGET},
			{BOARDCHATPERCENT,"25"},
			{G.DEBUG, "false"},
			{RANDOMSEED,"-1"},
	        {GAMEINDEX,"-1"},	// game index for save game
	        {G.LANGUAGE,DefaultLanguageName},
	        {LOBBYPORT,"-1"},
	        {TESTSERVER, "false"},
	        {PICTURE, "false"},
	        {WebStarted,"false"},
	        {ConnectionManager.UID, "0"},
	        {ConnectionManager.BANNERMODE, "N"},
	        {ConnectionManager.USERNAME,"me"},
	        {UIDRANKING,""},
	        {GAMESPLAYED,"0"},
	        {DefaultGameClass,"online.common.commonLobby"},	// this directs the master class
	        //
	        // all these null values need to be here, because they
	        // trigger the applet client to look for the parameter
	        // among the applet parameters.
	        //
			{GAMENAME,null},		// a file name, not a game type
			{GAMETYPE,null},
			{VIEWERCLASS,null},
			{G.VNCCLIENT,"false"},
			{G.ALLOWOFFLINEROBOTS,"false"},
			{GAMEINFO,null},
			{IMAGELOCATION,null},	// used by the player map 
	        {DATALOCATION,null},	// used by the player map 
	        {SMALL_MAP_CENTER_X,null},// used by the player map 
			{SMALL_MAP_CENTER_Y,null},// used by the player map 
			
			{EXTRAMOUSE,null},	// debugging and admin options
			{EXTRAACTIONS,null},
			{FAVORITES,null},	// favorite games list, from login
			{COUNTRY,null},		// country of origin, from login
			{LATITUDE,null},	// current location, from login
			{LOGITUDE,null},
			{PROTOCOL,null},	// http or https
			{SERVERNAME,null},	// the server we connect to
			{SERVERKEY,null},	// permission token to connect
			{FRAMEWIDTH,null},
			{FRAMEHEIGHT,null},
	    };

	public static void setDefaults()
	{	// apply the defaults
		for(String p[] : DefaultParameters)
    	{		
			G.putGlobal(p[0],p[1]);
    	}
 	}
 
  
	private static void init()
    {
       //System.out.println("init");
        try
        {
            //do a definitive test in G.Composite instead
            //if(isJDK_GEQ(1,4)) { G.jdk_supports_getpixels=true; }
            String serverName = G.getString(SERVERNAME,null);

            if (serverName == null)
            {
                URL codebase = G.getCodeBase();
                serverName = codebase.getHost();
            }
            
            Http.setHostName(serverName);
            serverName = Http.getHostName(); 	// setHostName blesses and normalizes the name

            if(G.offline())
            {	// get a list of supported games for offline viewing
             	String params = "&tagname=gamedir"+G.platformString();
             	// unencrypted version is getInfoURL
               	UrlResult result = Http.postEncryptedURL(serverName,getEncryptedURL,params,web_server_sockets);            	
        		if(result.error==null)
        		{	
        			StringTokenizer tok = new StringTokenizer(result.text,"\n\r,");
        			while(tok.hasMoreTokens())
        			{
        				int idx = G.IntToken(tok);
        				tok.nextToken();	// skip the name
        				String dir = tok.nextToken();
        				G.putGlobal(REVIEWERDIR+idx,dir);
        			}
         		}
        		String info = G.getString(GAMEINFO, null);
        		if(info!=null) { GameInfo.parseInfo(info); }
            }

        }
    	catch (ThreadDeath err) { throw err;}
        catch (Throwable err)
        {	
            Http.postError(null, "Error in FrameLauncher init", err);
        }
    }

 
    public static void StartLframe()
    {	try {
        Http.setDefaultProtocol(G.getString(PROTOCOL,null));
        InternationalStrings.initLanguage();
        boolean isViewer = (G.getString(OnlineConstants.GAMENAME,null)!=null)
        					|| (G.getString(OnlineConstants.GAMETYPE, null)!=null);
        if(isViewer) { G.setOffline(true); }
    	boolean offline = G.offline() ;
        boolean isVNC = G.getBoolean(G.VNCCLIENT,false);
        PlaytableServer server = isVNC ? PlaytableStack.getSelectedServer() : null;
    	boolean offlineLauncher = !isVNC && offline && !isViewer && offlineTableLauncher;
        boolean isTable = offline && !offlineLauncher && G.isTable() ;
        String classname = isViewer
        					? "G:game.Game" 
        					: isVNC|isTable|offlineLauncher
        						? "online.common.commonPanel"
        					: G.getString(DefaultGameClass,"online.common.commonLobby");
 
        G.setIdString(""+G.getCodeBase());
        commonPanel myL = (commonPanel) G.MakeInstance(classname);
        if (myL != null)
        {	XFrame fr = new XFrame();
    	 	
            String rootname = isVNC
            					? server.getHostName()
            					: isTable 
            						? UDPService.getPlaytableName()
            						: G.getString(OnlineConstants.GAMETYPE,
            								G.getTranslations().get(offlineLauncher?LauncherName :LobbyName));
            ExtendedHashtable sharedInfo = G.getGlobals();
            myL.init(sharedInfo,fr);
            // create the free standing frame
            new LPanel(rootname, fr,myL);
             
            if(isVNC|isTable|offlineLauncher)
            	{ 
            	  if(isVNC && server.isRpc())
            	  {	// starting a rpc style viewer, the window will change but we want to establish a connection
            		RpcReceiver.start(server,sharedInfo,myL,fr);
            	  }
            	  else 
            	  {
            	  CanvasProtocol viewer = isVNC 
            			  					? (CanvasProtocol)G.MakeInstance("vnc.AuxViewer")
            			  					: (CanvasProtocol)G.MakeInstance("online.common.SeatingViewer");
            	  // init first, then add to the frame, to avoid races in lockAndLoadImages
            	  viewer.init(sharedInfo,fr);
            	  myL.setCanvas(viewer);
            	  }
            	}
            int fx = 5;
            int fy = 10;
            int fw = G.tableWidth();
            int fh = G.tableHeight();
            
            if(G.debug()&&(G.isTable()))
            {      	
            }
            else
            {
            double sc = G.getDisplayScale();
            fx = 100;
            fy = 50;
            fw = (int)(sc*G.getInt(OnlineConstants.FRAMEWIDTH,offlineLauncher?1000 : DEFAULTWIDTH));
            fh = (int)(sc*G.getInt(OnlineConstants.FRAMEHEIGHT,offlineLauncher ? 700 : DEFAULTHEIGHT));
            }
            fr.setInitialBounds(fx,fy,fw,fh );
                      
      	 	if(fr!=null) { fr.setVisible(true); } 

            myL.run();
            //System.out.println("root start");
          }
    }
    	catch (Throwable e)
    	{	Http.postError(null,"FrameLauncher outer run",e);
    	}	
    }

    public void runLframe()
    {	init();
		StartLframe();
    }

		public boolean runnable = true;
		public void runLogin(String serverName)
		{	
			// the global state table can be contaminated during play.  In case of 
			// a loop around, we save the original contents and restore it.
			ExtendedHashtable savedGlobals = G.getGlobals().copy();
	    	UDPService.start(false);	// listen for tables
		  	G.doDelay(1000);
			UDPService.stop();
			Login login = new Login();
			// change this to skip the login and return true for impersonation login
			boolean impersonate = G.debug() && G.getBoolean("Impersonation",false);
			boolean webStarted = impersonate || login.initFromWebStart();
        	G.putGlobal(WebStarted,webStarted?"true":"false");
        	boolean vnc =  G.getBoolean(G.VNCCLIENT,false);
        	if(vnc || ( webStarted && !G.offline()))
        	{	
				runLframe();					
			}
        	if(vnc) { G.setOffline(false); }
        	// 
        	// restore the globals to their initial state, except serverName
        	//
        	serverName = G.getString(SERVERNAME,serverName);
        	savedGlobals.put(G.LANGUAGE,G.getString(G.LANGUAGE, DefaultLanguageName));
        	savedGlobals.put(SERVERNAME,serverName);
        	G.setGlobals(savedGlobals.copy());
        	
        	
  			// this delay is to avoid the mysterious "gc lockup" when recycling the login
			// window. It shouldn't be necessary, but IOS gets caught when closing down
			// the previous window is still in progress and a new one is created.
	        G.waitAWhile(this,500);
		}
		
		public void runOffline(String serverName)
		{	
			OfflineGames.pruneOfflineGames(90);
	    	// the global state table can be contaminated during play.  In case of 
			// a loop around, we save the original contents and restore it.
			ExtendedHashtable savedGlobals = G.getGlobals().copy();
				
		  	runLframe();					
			// 
        	// restore the globals to their initial state, except serverName
        	//
        	serverName = G.getString(SERVERNAME,serverName);
        	savedGlobals.put(SERVERNAME, serverName);
        	G.setGlobals(savedGlobals.copy());
        	
        	// this delay is to avoid the mysterious "gc lockup" when recycling the login
			// window. It shouldn't be necessary, but IOS gets caught when closing down
			// the previous window is still in progress and a new one is created.
	        G.waitAWhile(this,100);
		}
		

		public void runMain(String args[])
		{ 	
			// must be first because it loads the defaults
			setDefaults();
			G.setGlobalDefaultFont();	// set a reasonable global default
			// copy the web start parameters
			for(int i=0; i<args.length-1; i+=2)
			{	String par = args[i].toLowerCase();
				String arg = args[i+1];
				if(arg!=null) 
					{ G.putGlobal(par,arg);
					  Plog.log.addLog("put arg " ,par," ",arg);
					}
			}
			String serverName = G.getString(SERVERNAME,DEFAULT_SERVERNAME);
			//System.out.println("Servername "+serverName);
			G.print("Screen: ",G.screenSize()," = ",G.screenDiagonal());
			G.print("w ",G.getScreenWidth()," h ",G.getScreenHeight()," dpi ",G.getRealScreenDPI()," ppi ",G.getPPI()," scale ",G.getDisplayScale());
			G.print(G.screendpiDetails);
			if(G.debug()) 
    		{ 
    			G.print("\nOS: ",G.getOSInfo(),"\nPlaytable ",G.isTable(),
					 "\nreal playtable: ",G.isRealPlaytable(),
					 "\nreal InfinityTable: ",G.isRealInfinityTable(),
					 "\nreal LastGameBoard:",G.isRealLastGameBoard(),
					 "\n");

    		}
			G.setDrawers(false);	// for lastgameboard

			G.getOSInfo();
			G.print("Ask fontsize "+G.defaultFontSize," get ",G.getFontSize(G.getGlobalDefaultFont()));
			G.print(Component.getHierarchy(MasterForm.getMasterPanel()));
			G.print("\n");
			G.print(Component.getHierarchy(MasterForm.getMasterForm().getTitleBar()));
			G.print("\n");
	    	//G.print(G.getPackages());
	    	G.putGlobal(RELEASEHOST,serverName);
	    	G.putGlobal(SERVERNAME,serverName);
	    	String lang=prefs.get(langKey,"english"); 
			if(lang!=null) { G.putGlobal(G.LANGUAGE,lang); }
	    	Http.setHostName(serverName);
	    	DataCache.construct();
	        if(G.getString(OnlineConstants.VIEWERCLASS,null)==null)
	        {	boolean isTable = G.isTable();
			  	G.setOffline(isTable);

			  	do { 
			  		boolean wasOff = G.offline();
			  		if(wasOff)
			  		{	
			  			runOffline(serverName);
			  		}
			  		else 
			  		{	
			  			runLogin(serverName); 
			  		}
			  	if(G.offline()==wasOff) { 
			  		// didn't change modes, revert to the default
			  		G.setOffline(isTable);
			  	}}
			  	while(G.isCodename1());
	        }
	        else 
	        { 	runLframe();
	    	}
		}
		// main methods cause IOS builds to fail by failing to launch
		public static void runLobby(String [] args)
	     	{	
			UDPService.stop(); 
			VNCService.stopVNCServer();
			JWSApplication app = new JWSApplication();
			try {
				app.runMain(args);
			}
	    	catch (ThreadDeath err) { throw err;}
			catch (Throwable err)
				{
				G.printStackTrace(err);
				Http.postError("JWSApplication","Unexpected error",err);
				}
			finally
			{
			UDPService.stop();
			VNCService.stopVNCServer();
			SoundManager.stop();
			}

	     	}
			

}
