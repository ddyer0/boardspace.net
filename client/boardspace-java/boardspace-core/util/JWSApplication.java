package util;

import vnc.VNCService;

import bridge.Config;
import lib.ExtendedHashtable;
import lib.G;
import lib.Http;
import lib.OfflineGames;
import lib.Plog;
import lib.RootAppletProtocol;
import lib.SoundManager;
import lib.StringStack;
import online.common.OnlineConstants;
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

public class JWSApplication implements Config,Runnable
{
		public boolean runnable = true;
		public void runLogin(String serverName,RootAppletProtocol realRoot)
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
	        	
		}
		
		public void runApp(RootAppletProtocol realRoot)
		{	
			realRoot.runLframe();
		}
		
		public void runMain(String args[])
		{ 	
			// must be first because it loads the defaults
			RootAppletProtocol realRoot = new online.common.commonRootApplet();
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
			G.print("Screen "+G.screenSize()+" = "+G.screenDiagonal());
	    	G.putGlobal(RELEASEHOST,serverName);
	    	G.putGlobal(SERVERNAME,serverName);
	    	String lang=prefs.get(langKey,"english"); 
			if(lang!=null) { G.putGlobal(G.LANGUAGE,lang); }
	    	Http.setHostName(serverName);
	    	if(!G.getBoolean(OnlineConstants.REVIEWONLY,false) &&  G.getString(OnlineConstants.VIEWERCLASS,null)==null)
	        {	boolean isTable = G.isTable();
	    		G.setOffline(isTable);
	    		boolean startoff = isTable;
			  	do { 
			  		startoff = G.offline();
			  		if(G.offline()) { runOffline(serverName,realRoot); }
			  		else { runLogin(serverName,realRoot); }
			  	} while(startoff!=G.offline());
	        }
	        else 
	        { 	
	            runApp(realRoot);
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
			G.setPlatformName(G.getOS());
			runLobby(args);
	     	}
	
		public void run()
		{	StringStack args = new StringStack();
			G.setPlatformName(G.getOS()+" Executable Jar");
			int idx = 0;
			do {
				String aa = System.getProperty("mainargs-"+idx);
				if(aa==null) { break; }
				args.push(aa);
				idx++;
			} while(true);
			runLobby(args.toArray());
			//System.exit(0);
		}
}