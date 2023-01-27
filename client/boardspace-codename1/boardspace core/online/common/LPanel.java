package online.common;

import bridge.*;
import lib.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Insets;

import lib.CanvasRotaterProtocol;
import lib.DeferredEventManager;
import lib.G;
import lib.NullLayout;
import lib.NullLayoutProtocol;
import lib.InternationalStrings;
import lib.MenuParentInterface;
import lib.XFrame;


public class LPanel extends FullscreenPanel implements WindowListener,NullLayoutProtocol,LobbyConstants
{
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
    public XFrame theFrame=null;
    public commonPanel theLobby;
    public MenuParentInterface getMenuParent() { return(theLobby); }
    public void dispose() { if(theFrame!=null) { theFrame.dispose(); }}
    //	boolean didshow=false;
    public JMenu options = null;
    public JMenu actions = null;
    boolean needActions = false;
    public JCheckBoxMenuItem soundCheckBox = null;
    private InternationalStrings s = null;
    private boolean killed = false;
 
    static final String SoundMessage = "Sound";
    static final String OptionsMessage = "Options";
    static final String ActionsMessage = "Actions";

    public static String LPanelMessages[] = {
            OptionsMessage,
            ActionsMessage,
           SoundMessage,

    };
    
    public void setCanvasRotater(CanvasRotaterProtocol r) { if(theFrame!=null) { theFrame.setCanvasRotater(r); }}
    public CanvasRotaterProtocol getCanvasRotater() { return ((theFrame!=null) ? null : theFrame.getCanvasRotater()); }
    public void setIconAsImage(Image m)
    {
    	if(theFrame!=null) { theFrame.setIconAsImage(m); }
    }
    public Dimension getPreferredSize()
    {	int w = 9999;
    	int h = 9999;
    	if(theFrame!=null) { w = getParent().getWidth(); h=getParent().getHeight(); }
    	return(new Dimension(w,h));
    }
    public boolean killed()
    	{ return(killed || ((theFrame!=null)?theFrame.killed():false)); 
    	}
 
    /* constructor */
    public LPanel(String inStr,XFrame frame, commonPanel inLobby)
    {
        super();	
        //setOpaque(false);
        new BoxLayout(this,BoxLayout.Y_AXIS);
        theFrame = frame;
        theLobby = inLobby;
        s = G.getTranslations();
        setLayout(new NullLayout(this));
        //setResizable(true);
     	// this is a workaround to force all JPopupMenu to be "heavyweight"
     	// which somehow avoids the menu clipping problem.
     	JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        options = new XJMenu(s.get(OptionsMessage),true);
        actions = new XJMenu(s.get(ActionsMessage),true);
        
        if(frame!=null)
        {	frame.addToMenuBar(options);
        	frame.setContentPane(this);//frame.add(this);
        	frame.addWindowListener(this);
        }
        setTitle(inStr);	// defer this until after the menus are added
        add(theLobby);
    }
    
    public void setVisible(boolean v)
    {
    	super.setVisible(v);
    	if(theFrame!=null) { theFrame.setVisible(true); }
    }


    public void setSize(int w,int h)
    {
    	if(theFrame!=null) { theFrame.setSize(w,h); }
    }

	public void doNullLayout(Container parent)
    {
		setLocalBounds(0,0,getWidth(),getHeight());
    }

    Dimension oldsize = new Dimension(0,0);
    public void setLocalBounds(int l,int t,int w,int h)
    {	
        if ((theLobby != null) 
        		&& w>0
        		&& h>0
        		&& ((w!=G.Width(oldsize))||(h!=G.Height(oldsize))))
        {
        Insets in = getInsets();
        int barheight = 0;//myMenuBar.getHeight(); we use frame menu bars which have zero height for us
        oldsize = new Dimension(w,h);
        	theLobby.setBounds( in.left, barheight+in.top,
        			w-in.left-in.right, h-in.top-in.bottom-barheight);
        }
    }
    	            	
    public void requestFocus()
    {
    	theFrame.requestFocus();
    }
    public void addKeyListener(KeyListener who)
    {
    	if(theFrame!=null) { theFrame.addKeyListener(who); }
    }
    public void removeKeyListener(KeyListener who)
    {
    	if(theFrame!=null) { theFrame.removeKeyListener(who); }
    }
    public void addWindowListener(WindowListener who)
    {	
    	if(theFrame!=null) { theFrame.addWindowListener(who); }
    }
	public void setTitle(String str) {
		if(theFrame!=null) { theFrame.setTitle(str); }
	}
	public String getTitle()
	{
		return(theFrame!=null? theFrame.getTitle() : "");
	}
  

    public void windowClosing(WindowEvent e)
    {
       theFrame.killFrame();
       if(theLobby!=null) { theLobby.requestShutdown(); }
    }

    public void windowClosed(WindowEvent e)
    {
       theFrame.killFrame();
       if(theLobby!=null) { theLobby.requestShutdown(); }
    }


 
    public void setCanSavePanZoom(DeferredEventManager m) {
		theFrame.setCanSavePanZoom(m);
	}
	public void setHasSavePanZoom(boolean v) {
		theFrame.setHasSavePanZoom(v);;
	}

}