package online.common;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import lib.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import bridge.JMenu;
import bridge.JMenuItem;
import bridge.JPopupMenu;
import bridge.Config;
import bridge.FullscreenPanel;
import bridge.XJMenu;
import lib.CanvasRotaterProtocol;
import lib.DeferredEventManager;
import lib.G;
import lib.NullLayout;
import lib.NullLayoutProtocol;
import lib.RootAppletProtocol;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.MenuParentInterface;
import lib.NativeMenuItemInterface;
import lib.XFrame;


public class LPanel extends FullscreenPanel implements LFrameProtocol, WindowListener,NullLayoutProtocol,LobbyConstants
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
    private boolean dontKill = false;
    private boolean pleaseKill = false;
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
    public LPanel(String inStr,XFrame frame,
        commonPanel inLobby)
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
    public void initMenus()
    {	initMenu(actions);
    	initMenu(options);
        boolean defaultSound = Config.Default.getBoolean(Config.Default.sound);
        soundCheckBox = addOption(s.get(SoundMessage), defaultSound,null);
        if(theLobby instanceof online.common.commonLobby)
        {	soundCheckBox.setForeground(Color.blue); 
        }
    }
    public void initMenu(JMenu m)
    {	if(m!=null)
    	{
    	while(m.getItemCount()>0)
    		{
    		m.remove(m.getItem(0));
    		}
    	}
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
    public boolean doSound()
    {
        return (soundCheckBox.isSelected());
    }
    public void setDoSound(boolean v) 
    { soundCheckBox.setSelected(v); 
    }
    
    public JCheckBoxMenuItem addOption(String text,
        boolean initial, JMenu m,DeferredEventManager e)
    {	JCheckBoxMenuItem b = new JCheckBoxMenuItem(text);
        b.setState(initial);
        m.add(b);
        if(e!=null) { b.addItemListener(e); }
        return (b);
    }

    public JCheckBoxMenuItem addOption(String text, boolean initial,DeferredEventManager e)
    {
        return (addOption(text, initial, options,e));
    }
    public JMenu addChoiceMenu(String text, DeferredEventManager e)
    {	JMenu item = new XJMenu(text,false);
    	options.add(item);
        if(e!=null) { item.addActionListener(e); }
        return(item);
    }

    public JMenuItem addAction(JMenuItem b,DeferredEventManager e)
    {	actions.add(b);
    	addActionInternal(b,e);
    	return(b);
    }
    public JMenu addAction(JMenu b,DeferredEventManager e)
    {	actions.add(b);
    	addActionInternal(b,e);
    	return(b);
    }
    private void addActionInternal(NativeMenuItemInterface b,DeferredEventManager e)
    {
    	try{
    	if(e!=null) { b.addActionListener(e); }
    	if (!needActions)
    	{
    		if(theFrame!=null) { theFrame.addToMenuBar(actions); }
    		needActions = true;
        }}
    	catch (Throwable err)
        {
            G.print("AddAction got an error: " + err);
        }
    }

    public  JMenuItem addAction(String text,DeferredEventManager e)
    {
    	JMenuItem b = new JMenuItem(text);
        addAction(b,e);
        return(b);
    }

    public void removeAction(JMenu m)
    {
        if (m != null)
        {
        	try
            { //error trap for ms explorer bug
            	actions.remove(m); 
 
                if (actions.getItemCount() == 0)
                {
                	theFrame.removeFromMenuBar(actions);
                    needActions = false;
                }
            }
           	catch (Throwable err)
            {
                G.print("RemoveAction got an error: " + err);
            }

        }
    }

    public void removeAction(JMenuItem m)
    {
        if (m != null)
        {
        	try
            { //error trap for ms explorer bug
            	actions.remove(m); 
 
                if (actions.getItemCount() == 0)
                {
                	theFrame.removeFromMenuBar(actions);
                    needActions = false;
                }
            }
           	catch (Throwable err)
            {
                G.print("RemoveAction got an error: " + err);
            }

        }
    }
  
   
    public void addToMenuBar(JMenu m)
    {
    	addToMenuBar(m,null);
    }

    public void addToMenuBar(JMenu m,DeferredEventManager l)
    {	
        if(theFrame!=null) { theFrame.addToMenuBar(m); }
    	if(l!=null) 
    		{ m.addItemListener(l);  
    		}
     }
    public void addAction(JMenu m,JMenuItem mi,DeferredEventManager e)
    {	
    	m.add(mi);
    	mi.addActionListener(e);
    }
    public JMenuItem addAction(JMenu m,String mname,DeferredEventManager e)
    {	JMenuItem mi = new JMenuItem(mname);
    	addAction(m,mi,e);
    	return(mi);
    }
    
    public void removeFromMenuBar(JMenu m)
    {	
    	theFrame.removeFromMenuBar(m);
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
    boolean recurse=false;
    public void setInitialBounds(int inx,int iny,int inw,int inh)
    {
    	if(theFrame!=null) 
    		{ if(G.isCodename1()) 
    			{ theFrame.expand(inw,inh);
    			} 
    			else if(theLobby!=null)
    			{ 	int fx = inx;
    				int fy = iny;
    				int fw = inw;
    				int fh = inh;
    	            Preferences prefs = Preferences.userRoot();
    	            String suffix ="-"+getTitle();
    	            String bounds = prefs.get(OnlineConstants.FRAMEBOUNDS+suffix,null);
    	            if(bounds!=null) { 
    	            	String split[] = G.split(bounds,',');
    	            	if(split!=null && split.length==4)
    	            	{
    	            		fx = G.IntToken(split[0]);
    	            		fy = G.IntToken(split[1]);
    	            		fw = G.IntToken(split[2]);
    	            		fh = G.IntToken(split[3]);
    	            	}
    	            	
    	            }
    	            //
    	            // make sure the bounds are minimally acceptable
    	            int screenW = G.getScreenWidth();
    	            int screenH = G.getScreenHeight();
    	            fw = Math.max(screenW/5,Math.min(fw,screenW));
    	            fh = Math.max(screenH/5,Math.min(fh,screenH));
    	            fx = Math.max(0,Math.min(screenW-fw,fx));
    	            fy = Math.max(0,Math.min(screenH-fh,fy));
    				theFrame.setBounds(fx,fy,fw,fh);   			
    			} 
    		}
    }

    public boolean DontKill()
    {
        return (dontKill);
    }
 
    public void setDontKill(boolean v)
    {
        dontKill = v;
        if (pleaseKill && (dontKill == false))
        {
            killFrame();
        }
    }

    // the please kill/don't kill logic is so that during critical
    // phases (ie; scoring a game) the frame will resist closing
    // and that under whatever circumstances, it only closes once.
    public void killFrame()
    { 
        pleaseKill = true;			// remember some tried to do it
        //
        // record the default position and size
        if((theLobby!=null) && !G.isCodename1())
        {
            String suffix = "-"+ getTitle();
	       	Preferences prefs = Preferences.userRoot();
        	Rectangle dim = theFrame.getBounds();
        	prefs.put(OnlineConstants.FRAMEBOUNDS+suffix,
        			""+G.Left(dim)+","+G.Top(dim)+","+G.Width(dim)+","+G.Height(dim));
        	
        }
        //G.print("Killframe");
        if ((dontKill == false)		// temporarily inhibited?
        		&&(killed==false))	// already killed
        {	RootAppletProtocol p = G.getRoot();
        	commonPanel l = theLobby;
            killed = true; //prevent kill loops
            //System.out.println("JFrame killed");
            if (l != null)
            {
            l.requestShutdown();
            }
            if (p != null)
            {
            p.killFrame(this);
            }
            //dispose();
            if(theFrame!=null)
            {	theFrame.setVisible(false);
            	theFrame.dispose();
            }
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
       killFrame();
    }

    public void windowClosed(WindowEvent e)
    {
       killFrame();
    }

  
 
	public void setCanSavePanZoom(DeferredEventManager m) {
		theFrame.setCanSavePanZoom(m);
	}
	public void setHasSavePanZoom(boolean v) {
		theFrame.setHasSavePanZoom(v);;
	}
}