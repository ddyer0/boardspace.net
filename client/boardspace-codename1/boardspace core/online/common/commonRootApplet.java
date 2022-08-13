package online.common;

import bridge.ThreadDeath;
import bridge.URL;

import java.util.StringTokenizer;

import common.GameInfo;
import lib.CanvasProtocol;
import lib.ConnectionManager;
import lib.ExtendedHashtable;
import lib.G;
import lib.Http;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.RootAppletProtocol;
import lib.UrlResult;
import lib.XFrame;
import rpc.RpcReceiver;
import udp.PlaytableServer;
import udp.PlaytableStack;
import udp.UDPService;


/**
 * this is the common implementation of many things that are nominally
 * handled by the applet root.
 * @author ddyer
 *
 */
@SuppressWarnings("deprecation")
public class commonRootApplet implements  RootAppletProtocol, Runnable,  LobbyConstants
{
	// this should be the complete set of applet parameters that
	// can ever be supplied, except for an indefinite number of
	// reviewer directories
	static final String DATALOCATION = "datalocation";

	String DefaultParameters[][] = {
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

	public commonRootApplet()
	{	// apply the defaults
		for(String p[] : DefaultParameters)
    	{		
			G.putGlobal(p[0],p[1]);
    	}
        G.setRoot(this);
     }

    LFrameProtocol myLF = null;
    commonPanel myL;



    public void init()
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
             	String params = "&tagname=gamedir";
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
            Http.postError(this, "Error in RootApplet init", err);
        }
    }

    public void initSharedInfo()
    {
        Http.setDefaultProtocol(G.getString(PROTOCOL,null));
        InternationalStrings.initLanguage();
    }
    public LFrameProtocol NewLPanel(String name, commonPanel a,XFrame fr)
    {	LFrameProtocol lf = new LPanel(name, fr, a);
    	return(lf);
    }
    public void StartLframe()
    {	try {
    	initSharedInfo();
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
        myL = (commonPanel) G.MakeInstance(classname);
        if (myL != null)
        {	XFrame fr = new XFrame();
    	 
        	String rootname = isVNC
            					? server.getHostName()
            					: isTable 
            						? UDPService.getPlaytableName()
            						: G.getString(OnlineConstants.GAMETYPE,offlineLauncher?"Offline Launcher" :"Lobby");
            myLF = NewLPanel(rootname, myL,fr);
             
            ExtendedHashtable sharedInfo = G.getGlobals();
            myL.init(sharedInfo,myLF);
            
            if(isVNC|isTable|offlineLauncher)
            	{ 
            	  if(isVNC && server.isRpc())
            	  {	// starting a rpc style viewer, the window will change but we want to establish a connection
            		RpcReceiver.start(server,sharedInfo,myL,myLF);
            	  }
            	  else 
            	  {
            	  CanvasProtocol viewer = isVNC 
            			  					? (CanvasProtocol)G.MakeInstance("vnc.AuxViewer")
            			  					: (CanvasProtocol)G.MakeInstance("online.common.SeatingViewer");
            	  // init first, then add to the frame, to avoid races in lockAndLoadImages
            	  viewer.init(sharedInfo,myLF);
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
            myLF.setInitialBounds(fx,fy,fw,fh );
                      
      	 	if(fr!=null) { fr.setVisible(true); } 

            myL.run();
            //System.out.println("root start");
          }
    	}
    	catch (Throwable e)
    	{	Http.postError(this,"outer run",e);
    	}	
    }


    /** come here from closing the lobby or any of the games that it supervises */
    public void killFrame(LFrameProtocol inLF)
    {	commonPanel l = myL;
        if ((l != null) && (inLF != myLF))
        	{
            l.killFrame(inLF); //killing some frame that the child created
            }

    }
    public void run()
    {	init();
    	StartLframe();
    }
    public void runLframe()
    {	// use this to restrict the stack size
    	//Thread t = new Thread(null,this,getClass().getName(),1000000);
    	Thread t = new Thread(this,"Root Process");
    	t.start();
    	try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }


    public LFrameProtocol NewLFrame(String name, commonPanel a)
    {	
    	XFrame fr = new XFrame();
    	LFrameProtocol lf = new LPanel(name, fr, a);
    	return(lf);
    }


}