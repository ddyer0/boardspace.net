package zip.boardspace;
import lib.BSDate;
import lib.G;
import lib.GC;
import lib.Http;
import lib.NullLayout;
import online.common.OnlineConstants;
import util.JWSApplication;
import bridge.Color;
import bridge.Frame;
import bridge.FullScreen;
import bridge.Panel;
import bridge.Config;

import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;
import lib.Graphics;
import lib.Image;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

interface stuff {
	public String connectHost = "boardspace.net" ; //;
	public String serverHost = "boardspace.net";
	public String developHost = "local.boardspace.net";
	public String game = "arimaa";
	public String viewer = "arimaa.ArimaaViewer";
	public String dir = "file:file://g:/share/projects/boardspace-html/htdocs/arimaa/arimaagames/";
	public boolean launchLobby = true;
	public boolean console = true;
	public boolean debug = false;	// for development version only
}

class Splash extends Panel implements FullScreen,Runnable,Config
{	Image splash ;
	Frame frame;
	
	public Splash(Image im) 
	{ splash = im;
	  frame = new Frame(); 
	  frame.setLayout(new NullLayout(frame));
	  frame.add(this);
	}

	public void paint(Graphics gr)
	{	int w = getWidth();
		int h = getHeight();
		GC.setColor(gr,Color.gray);
		GC.fillRect(gr,0,0,w,h);
		if(splash!=null) {splash.centerImage(gr, 0,0,w,h); }
	}
	public static void doSplash()
	{	Image splash = Image.getImage(SPLASHSCREENIMAGE);
		if(splash==null) { G.infoBox("splash screen missing",SPLASHSCREENIMAGE); }
		Splash comp = new Splash(splash);
		comp.setVisible(true);
		new Thread(comp,"launcher").start();
	}

	@Override
	public void run() {
		frame.show();
		G.doDelay(10000);	
		frame.remove();
	}
}

class BoardspaceLauncher implements Runnable,stuff
{	
	boolean develop = false;
	public BoardspaceLauncher(boolean dev) {develop=dev; }
	public void launchLobby()
	{	
	    //test login window
        String params[] = new String[]
        		{"servername",connectHost,
        		 "develophost",developHost,
        		 "development",""+develop,
        		 "extraactions",""+develop,
        		 //"playtable","true",
        		 G.DEBUG,""+develop,
        		};
        util.JWSApplication.runLobby(params);
 	}
	
	public void launchGame()
	{	
	       //test login window
        String params[] = new String[]
        		{"servername",connectHost,
        		Config.PROTOCOL,(develop?"http":"https"),
        		G.DEBUG,""+develop,
        		OnlineConstants.EXTRAACTIONS,"true",
        		OnlineConstants.EXTRAMOUSE,"true",
        		OnlineConstants.GAMETYPE,game,
        		OnlineConstants.VIEWERCLASS,viewer,
          		OnlineConstants.REVIEWERDIR+"0",dir,
        		//"playersingame","3",
        		//"GameType","Exxit",
        		//"viewerClass","exxit.ExxitGameViewer",
       		
         		//"GameType","Yspahan",
           		//"viewerClass","yspahan.YspahanViewer",
           		
        		//"GameType","BreakingAway",
           		//"viewerClass","breakingaway.BreakingAwayViewer",
          		
         		//"GameType","Arimaa",
           		//"viewerClass","arimaa.ArimaaViewer",
           		
         		//"GameType","Hex",
           		//"viewerClass","hex.HexGameViewer",
        		
           		//"GameType","Hive",
           		//"viewerClass","hive.HiveGameViewer",

           		//"GameType","Euphoria",
           		//"viewerClass","euphoria.EuphoriaViewer",
        		
         		// "GameType","Kamisado",
          		// "viewerClass","kamisado.KamisadoViewer",
            		
       		
         		 //"GameType","Checkers-turkish",
          		 //"viewerClass","checkerboard.CheckerGameViewer",
        		
           		//"GameType","Go-19",
        		//"viewerClass","goban.GoViewer",
           		//"reviewerdir0","file:///g:/share/projects/boardspace-html/htdocs/goban/gogames",
           	        		
        		"localfile","true",
        		"randomseed","01234",
        		"serverkey","1.2.3.4",
        		"reviewonly","true",
         		"final","true",
        		
        		
        		};
        JWSApplication app = new JWSApplication();
        app.runMain(params); 
 
	}
	public void doit()
	{	Http.setHostName(connectHost);
		try {
			Log.setReportingLevel(Log.REPORTING_DEBUG);
			//MasterForm form = MasterForm.getMasterForm();
			//form.show();
			if(launchLobby)
				{
				Splash.doSplash();
				launchLobby(); 
				} 
			else
				{ launchGame(); }
		}
		catch (Throwable err)
		{G.printStackTrace(err);
		 Http.postError(this,"outermost run",err);
		}
	}
	public void run() {
		doit(); 
		G.hardExit();

	}
}


public class Launch  implements stuff {
	Form current = null;
	public boolean isDevelopmentVersion() { return(false); }
	public void runBoardspace()
	{	if(console /*&& isDevelopmentVersion()*/) { G.createConsole(); }
		new Thread(new BoardspaceLauncher(isDevelopmentVersion()),"launcher").start();
	}

	public void init(Object context) {
        try {
            CN.updateNetworkThreadCount(getNetworkThreadCount());
            theme = UIManager.initFirstTheme(getThemeName());
            // Enable Toolbar on all Forms by default
            Toolbar.setGlobalToolbar(true);
        } catch (Throwable e) {
            G.infoBox("opening /theme "+e,G.getStackTrace(e));
        }
 	}
	
	public void start() {
		Http.setHostName(connectHost);
		//no longer needed as of 5/2017
		//G.namedClasses = NamedClasses.classes;
		G.setGlobalStatus(G.GlobalStatus.awake);
		if (current != null) 
		{   G.print("Launcher restart "+new BSDate().toString());
			current.show();
			return;
		}
		G.print("Launcher start "+new BSDate().toString());
		runBoardspace();
		
	}

	public void stop() {
		current = Display.getInstance().getCurrent();
		G.setGlobalStatus(G.GlobalStatus.asleep);
		G.print("Launcher stop " +new BSDate().toString());
	}

	public void destroy() {
		current = Display.getInstance().getCurrent();
		G.setGlobalStatus(G.GlobalStatus.asleep);
		G.print("Launcher destroy "+new BSDate().toString());
	}
	
    private Resources theme;

    /**
     * Invoked when the app is "cold launched", this acts like a constructor
     *
     * @param context some OSs might pass a native object representing platform internal information
     */


    /**
     * Returns the default number of network thread count
     * @return currently two threads
     */
    protected int getNetworkThreadCount() {
        return 2;
    }

    /**
     * Returns the name of the global theme file, by default it's "/theme". Can be overriden by subclasses to
     * load a different file name
     * @return "/theme"
     */
    protected String getThemeName() {
        return "/theme";
    }

    /**
     * The theme instance
     * @return the theme
     */
    public Resources getTheme() {
        return theme;
    }


}
