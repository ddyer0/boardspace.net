package util;

import bridge.*;
import bridge.ThreadDeath;
import lib.DataCache;
import lib.ExtendedHashtable;
import lib.G;
import lib.Http;
import lib.OfflineGames;
import lib.RootAppletProtocol;
import lib.SoundManager;
import online.common.OnlineConstants;
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

public class JWSApplication implements Config 
{
		public JWSApplication() { 
	        SoundManager.getInstance();		// get it warmed up
		}
		public boolean runnable = true;
		public void runLogin(String serverName,RootAppletProtocol realRoot)
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
			boolean webStarted = impersonate || login.initFromWebStart(serverName);
        	G.putGlobal(WebStarted,webStarted?"true":"false");
        	boolean vnc =  G.getBoolean(G.VNCCLIENT,false);
        	if(vnc || ( webStarted && !G.offline()))
        	{	
        		runApp(realRoot);					
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
		
		public void runOffline(String serverName,RootAppletProtocol realRoot)
		{	
			OfflineGames.pruneOfflineGames(90);
	    	// the global state table can be contaminated during play.  In case of 
			// a loop around, we save the original contents and restore it.
			ExtendedHashtable savedGlobals = G.getGlobals().copy();
				
			runApp(realRoot);					
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
		
		public void runApp(RootAppletProtocol realRoot)
		{	
			realRoot.init();
	        realRoot.StartLframe();
		}

		public void runMain(String args[])
		{ 	
			// must be first because it loads the defaults
			RootAppletProtocol realRoot = new online.common.commonRootApplet();
			G.setGlobalDefaultFont();	// set a reasonable global default
			// copy the web start parameters
			
			
			for(int i=0; i<args.length; i+=2)
			{	String par = args[i].toLowerCase();
				String arg = args[i+1];
				if(arg!=null) 
					{ G.putGlobal(par,arg);
					  //System.out.println("put "+par+" "+arg);
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
			  		if(G.offline())
			  		{
			  			runOffline(serverName,realRoot);
			  		}
			  		else 
			  		{ 
			  			runLogin(serverName,realRoot); } 
			  		}
			  	while(G.isCodename1());
	        }
	        else 
	        { 	runApp(realRoot);
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
