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
package util;

import vnc.VNCService;

import java.net.URL;
import java.util.StringTokenizer;

import bridge.Config;
import common.GameInfo;
import lib.CanvasProtocol;
import lib.ExtendedHashtable;
import lib.G;
import lib.Http;
import lib.InternationalStrings;
import lib.OfflineGames;
import lib.Plog;
import lib.SoundManager;
import lib.StringStack;
import lib.UrlResult;
import lib.XFrame;
import lib.commonPanel;
import online.common.LPanel;
import online.common.LobbyConstants;
import online.common.OnlineConstants;
import rpc.RpcReceiver;
import udp.PlaytableServer;
import udp.PlaytableStack;
import udp.UDPService;

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
public class JWSApplication implements Config,Runnable,LobbyConstants
{
	
	// this should be the complete set of applet parameters that
	// can ever be supplied, except for an indefinite number of
	// reviewer directories
	static final String DATALOCATION = "datalocation";


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
        		String info = G.getString(GameInfo.GAMEINFO, null);
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
        String gn = G.getString(GameInfo.GAMENAME,null);
        String gt = G.getString(GameInfo.GAMETYPE, null);
        boolean isViewer = (gn!=null)
        					|| (gt!=null);
        if(isViewer) { G.setOffline(true); }
    	boolean offline = G.offline() ;
        boolean isVNC = G.getBoolean(G.VNCCLIENT,false);
        PlaytableServer server = isVNC ? PlaytableStack.getSelectedServer() : null;
    	boolean offlineLauncher = !isVNC && offline && !isViewer && offlineTableLauncher;
        boolean isTable = offline && !offlineLauncher && G.isTable() ;
        String classname = isViewer
        					? "G:game.Game" 
        					: isVNC|isTable|offlineLauncher
        						? "lib.commonPanel"
        					: G.getString(DefaultGameClass,"online.common.commonLobby");
        G.print("StartLframe "+classname);
        G.setIdString(""+G.getCodeBase());
        commonPanel myL = (commonPanel) G.MakeInstance(classname);
        G.print("Myl "+myL);
        if (myL != null)
        {	XFrame fr = new XFrame();
    	 	
            String rootname = isVNC
            					? server.getHostName()
            					: isTable 
            						? UDPService.getPlaytableName()
            						: G.getString(GameInfo.GAMETYPE,
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
      	 	G.print("running "+myL);
            myL.run();
            //System.out.println("root start");
          }
    }
    	catch (Throwable e)
    	{	G.print("Error in FrameLauncher outer loop ",e,e.getStackTrace());
    		Http.postError(null,"FrameLauncher outer run",e);
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
			
			// remove this for cheerpj debugging, it doesn't work and doesn't cause a trap
			if(!G.isCheerpj()) { UDPService.start(false);	} // listen for tables
			G.doDelay(1000);
			UDPService.stop();			
			Login login = new Login();
			// change this to skip the login and return true for impersonation login
			boolean impersonate = G.debug() && G.getBoolean("Impersonation",false);
			boolean webStarted = impersonate || login.initFromWebStart();
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
	        	
		}
		
		public void runMain(String args[])
		{ 	
			// must be first because it loads the defaults
			G.setGlobalDefaultFont();	// set a reasonable global default
			// copy the web start parameters
			for(int i=0; i<args.length-1; i+=2)
			{	String par = (args[i].toLowerCase());
				String arg = args[i+1];
				if(arg!=null) 
					{ G.putGlobal(par,arg);
					  Plog.log.addLog("put arg " ,par," ",arg);
					}
			}
			String serverName = G.getString(SERVERNAME,DEFAULT_SERVERNAME);
			//System.out.println("Servername "+serverName);
			G.print("Screen "+G.screenSize()+" = "+G.screenDiagonal());
	    	G.putGlobal(RELEASEHOST,serverName);
	    	G.putGlobal(SERVERNAME,serverName);
	    	String lang=prefs.get(langKey,"english"); 
			if(lang!=null) { G.putGlobal(G.LANGUAGE,lang); }
	    	Http.setHostName(serverName);
	    	String vc = G.getString(OnlineConstants.VIEWERCLASS,null);
	    	boolean gb = G.getBoolean(OnlineConstants.REVIEWONLY,false);
	    	if(!gb &&  vc==null)
	        {	boolean isTable = G.isTable();
	    		G.setOffline(isTable);
	    		boolean startoff = isTable;
			  	do { 
			  		startoff = G.offline();
			  		if(G.offline()) { runOffline(serverName); }
			  		else { runLogin(serverName); }
			  	} while(startoff!=G.offline());
	        }
	        else 
	        { 	
	            runLframe();
	    	}
		}
		// main methods cause IOS builds to fail by failing to launch
		public static void runLobby(String [] args)
	     	{	
			G.classForName("online.game.Game",false);
			UDPService.stop(); 
			VNCService.stopVNCServer();
			JWSApplication app = new JWSApplication();
			SoundManager.getInstance();		// get it warmed up
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
		/*
		public String getJarDir()
		{	Class<?> cl = this.getClass();
			URL url = cl.getResource(cl.getSimpleName() + ".class");
		    if(url!=null) { 
		    	String name = url.toString();
		    	int idx = name.indexOf("!");
		    	if(idx>0) { name = name.substring(0,idx); }
	    		int idx2 = name.lastIndexOf("/");
		    	if(idx2>0)
		    		{	return(name.substring(0,idx2+1));
		    		}
		    }
			return(null);
		}
		public String getDirectory(String dir)
		{
			String urlStr = ((dir.startsWith("jar:")) ? dir.substring(4) : dir);
			try {
				URL url = new URL(urlStr);
				String host = url.getHost();
				String path = url.getPath();
				String res[] = Http.getURL(host,path,null);
				if(res!=null) 
					{ if(res[0]!=null) 
						{ System.out.println("Error from directory "+res[0]); 
						}
					  return(res[1]); 
					}
			}
	    	catch (ThreadDeath err) { throw err;}
			catch (Throwable err)
			{
				G.print("Error in getDirectory: "+err);
			}
			return(null);
		}
		*/
		
		// main methods cause IOS builds to fail by failing to launch
		public static void main(String [] args)
	     	{	
			// this is the entry point for ordinary testing
			G.setPlatformName(G.getOS());
			runLobby(args);
			System.exit(0);
	     	}
	
		public void run()
		{	// this is the entry point from the miniloader
			try {
			StringStack args = new StringStack();
			int idx = 0;
			String classes = "Executable Jar";
			String ts = "";
			String tt = "";
			String ml = " loader=old";
			String mini = null;
			do {
				String aa = System.getProperty("mainargs-"+idx);
				idx++;
				if(aa==null) { break; }
				
				String av = System.getProperty("mainargs-"+idx);
				idx++;
				
				args.push(aa);
				args.push(av);
				
				if(OnlineConstants.CLASSDIR.equals(aa)) { classes = " "+av; }
				if(OnlineConstants.TESTSERVER.equals(aa)) { ts = " TestServer"; }
				if(OnlineConstants.TESTVERSION.equals(aa)) { tt = " Test"; }
				if("miniloader".equals(aa)) { mini = av;  ml = " loader="+av; }
			
			} while(true);
	
			int jv = G.javaMajorVersion();
			if((jv>=18) && (mini==null)) // the first miniloader that reports here is 1.3
			{
				G.infoBox("boardspace launcher","your version of the boardspace launcher is old.\nPlease install a new one from Boardspace.net");
			}
			String os =G.getOS()+ " "+classes+ts+tt+ml;
			G.setPlatformName(os);
			runLobby(args.toArray());
			} catch (Throwable e)
			{
				System.out.println("error in launch "+e+" "+e.getStackTrace());
			}
			System.exit(0);
		}
}