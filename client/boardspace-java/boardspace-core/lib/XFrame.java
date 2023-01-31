package lib;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.security.AccessControlException;
import java.util.prefs.Preferences;

import bridge.Config;
import bridge.JFrame;
import bridge.JMenu;
import bridge.JMenuItem;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuBar;
import bridge.JPopupMenu;

import bridge.MasterForm;
import bridge.XJMenu;

public class XFrame extends JFrame implements WindowListener,SizeProvider,LFrameProtocol
{
	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private static final long serialVersionUID = 01L;
    static final String FRAMEBOUNDS = "framebounds";
	private boolean useMenuBar = !G.isCodename1();		// if true, use the local menu bar
	private boolean closeable = true;
	public void setCloseable(boolean v) { closeable = v; }
	public boolean getCloseable() { return(closeable); }
	public JMenuBar jMenuBar = null;
	JPopupMenu popupMenuBar = null;
	public DeferredEventManager canSavePanZoom = null;
	public boolean hasSavePanZoom = false;
	
    public JCheckBoxMenuItem soundCheckBox = null;
    public JMenu options = null;
    public JMenu actions = null;
    
    private InternationalStrings s = null;
    static final String SoundMessage = "Sound";
    static final String OptionsMessage = "Options";
    static final String ActionsMessage = "Actions";

    public static String XFrameMessages[] = {
            OptionsMessage,
            ActionsMessage,
           SoundMessage,

    };

	public void setCanSavePanZoom(DeferredEventManager m) 
	{ 	canSavePanZoom = m;
		MasterForm.getMasterPanel().adjustTabStyles(); 
	}
	public void setHasSavePanZoom(boolean v) 
	{ hasSavePanZoom = v; 
	  MasterForm.getMasterPanel().adjustTabStyles(); 
	}
	Label title;
	String name="Unnamed";
	/** constructor */
	public XFrame(String string) 
	{ 	super(string);
		initStuff(string);
	}
	/** constructor */
	public XFrame()
	{	super();
		initStuff("");


	}
	private void initStuff(String n)
	{ name = n;
	  s = G.getTranslations();
	  //setResizable(true);
   	  // this is a workaround to force all JPopupMenu to be "heavyweight"
   	  // which somehow avoids the menu clipping problem.
	  JPopupMenu.setDefaultLightWeightPopupEnabled(false);
      options = new XJMenu(s.get(OptionsMessage),true);
      actions = new XJMenu(s.get(ActionsMessage),true);

	  addWindowListener(this);
	  setOpaque(false);
	  initTitleBar();
	}
	
	public void setVisible(boolean v)
	{	
		if(v && (getWidth()<100 || getHeight()<100))
		{
			 double scale = G.getDisplayScale();
			 int w = (int)(scale*450);
			 int h = (int)(scale*430);
			 setBounds( 0, 0, w, h); 
		}
		super.setVisible(v);
	}

	public void setTitle(String n) 
	{ name = n;
	  if(title==null) { title = new Label(name); }
	  if(n!=null && G.isSimulator()) { title.setText(n+" "+getWidth()+"x"+getHeight()); }
	  MasterForm.getMasterPanel().setTabName(this,name,getIconAsImage());
	}
	public String getTitle()
	{
		return(name);
	}

	
	public void initTitleBar()
	{
		title = new Label(name);
	
	}
	public boolean hasCommand(String cmd)
	{	if(("rotate".equals(cmd) || "twist3".equals(cmd) || "twist".equals(cmd))) { return(rotater!=null); }
		if("close".equals(cmd)) { return(true); }
		if("actionmenu".equals(cmd)) 
			{ return(popupMenuBar!=null); 
			}
		if("savepanzoom".equals(cmd)) { return(canSavePanZoom!=null); }
		if("restorepanzoom".equals(cmd)) { return(hasSavePanZoom); }
		return(false);
	}
	CanvasRotaterProtocol rotater = null;
	public CanvasRotaterProtocol getCanvasRotater() { return rotater; }
	public void setCanvasRotater(CanvasRotaterProtocol r)  
	{	rotater=r; 
		MasterForm.getMasterPanel().adjustTabStyles();
	}
	public void buttonMenuBar(ActionEvent evt,int x,int y)
	{	String cmd = evt.getActionCommand().toString();
		if("twist3".equals(cmd))
		{
			if(rotater!=null) { rotater.setCanvasRotation(rotater.getCanvasRotation()+1); }			
		}
		else if("twist".equals(cmd))
		{
			if(rotater!=null) { rotater.setCanvasRotation(rotater.getCanvasRotation()-1); }
		}
		else if("rotate".equals(cmd))
		{
			if(rotater!=null) { rotater.setCanvasRotation(rotater.getCanvasRotation()+2); }
		}
		else if("actionmenu".equals(cmd))
			{if(popupMenuBar!=null)
		{
			try 
			{ 
				popupMenuBar.show(this,
						MasterForm.translateX(this, x),
						MasterForm.translateY(this, y)
						);
			} catch (AccessControlException e) {}
			}}
		else if("close".equals(cmd))
		{
			if(getCloseable()) 
				{ dispose(); 
				}
		}
		else if("savepanzoom".equals(cmd) || "restorepanzoom".equals(cmd))
		{	if(canSavePanZoom!=null)
			{	canSavePanZoom.deferActionEvent(evt);
		}
	}
		else if(G.debug()) {
			Http.postError(this,"unexpected action event: "+cmd,null);
		}
	}

	public void setJMenuBar(JMenuBar m) { jMenuBar = m; super.setJMenuBar(m); }
	public void addToMenuBar(JMenu m)
	{
		addToMenuBar(m,null);
	}
	public void addToMenuBar(JMenu m,DeferredEventManager l)
	{	
		if(useMenuBar)
		{	if(jMenuBar==null) {  setJMenuBar(new JMenuBar()); }
			m.setVisible(true);
			if(jMenuBar.getComponentIndex(m)<0)
				{
				jMenuBar.add(m);
				}
		}
		else {
			if(popupMenuBar==null) { popupMenuBar=new JPopupMenu();}
			if(popupMenuBar.getComponentIndex(m)<0)
				{
				popupMenuBar.add(m);
				}
		}
		if(l!=null) { m.addItemListener(l); }
	}

	
	public void removeFromMenuBar(JMenu m)
	{	
		if(useMenuBar)
		{
			if(jMenuBar!=null) { jMenuBar.remove(m); }
		}
		else {
			if(popupMenuBar!=null) { popupMenuBar.remove(m); }
		}
	}
	
	private Rectangle oldBounds = null;
	private int minimumWidth=300;
	private int minimumHeight=300;
	public void expand(int minw,int minh)
	{	//minimumWidth = minw;		// this was an exeriment to set minimums greater than available size
		//minimumHeight = minh;		// which created scrollable frames
		expand();
	}
	public Dimension getMinimumSize()
	{
		return(new Dimension(minimumWidth,minimumHeight));
	}

	public void expand()
	{
		if(oldBounds!=null)
		{ setBounds(G.Left(oldBounds),G.Top(oldBounds),G.Width(oldBounds),G.Height(oldBounds));
		  oldBounds = null; 
		}
		else 
		{ Container parent = getParent();
		// parent can legitimately be null if the window is closing
		if(parent!=null)
			{
			int w = parent.getWidth();
			int h = parent.getHeight();
			oldBounds = new Rectangle(getX(),getY(),getWidth(),getHeight());
			title.setText(name+" "+getWidth()+"x"+getHeight());
			setBounds(0,0,Math.max(minimumWidth,w),Math.max(minimumHeight,h));
			if(minimumWidth>w || minimumHeight>h)
			{
				G.print("Oversize "+name+" "+minimumWidth+"x"+minimumHeight+" "+w+"x"+h);
			}
			}
		}
	}


	public void windowOpened(WindowEvent e) {
		
	}
	public boolean killed = false;
	public boolean killed() 
	{
		return(killed);
	}

	public void dispose()
	{	killed=true;
		super.dispose();		
	}

	public void windowClosing(WindowEvent e) {
		killed = true;
	}

	public void windowClosed(WindowEvent e) {
		killed=true;
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {		
	}

	public void windowActivated(WindowEvent e) {		
	}

	public void windowDeactivated(WindowEvent e) {		
	}
	
	// for lframeprotocol
	public void show(MenuInterface menu, int x, int y) throws AccessControlException {
		G.show(this, menu, x, y);		
	}
	
	public void setInitialBounds(int inx,int iny,int inw,int inh)
	{
		if(G.isCodename1()) 
			{ expand(inw,inh);
			} 
			else 
			{ 	int fx = inx;
				int fy = iny;
				int fw = inw;
				int fh = inh;
	            Preferences prefs = Preferences.userRoot();
	            String suffix ="-"+getTitle();
	            String bounds = prefs.get(FRAMEBOUNDS+suffix,null);
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
				setBounds(fx,fy,fw,fh);   			
			} 	
	}

	public boolean doSound() {
	    return (soundCheckBox.isSelected());
	}
	public void setDoSound(boolean enable) {
		soundCheckBox.setSelected(enable); 
	}

 
	public JCheckBoxMenuItem addOption(String text, boolean initial, JMenu m,DeferredEventManager e)
    {	JCheckBoxMenuItem b = new JCheckBoxMenuItem(text);
        b.setState(initial);
        m.add(b);
        if(e!=null) { b.addItemListener(e); }
        addToMenuBar(options); 
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
        addToMenuBar(options);
        return(item);
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
    	addToMenuBar(actions); 
    	}
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
                	removeFromMenuBar(actions);
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
                	removeFromMenuBar(actions);
                }
            }
           	catch (Throwable err)
            {
                G.print("RemoveAction got an error: " + err);
            }

        }
    }
		  

    // the please kill/don't kill logic is so that during critical
    // phases (ie; scoring a game) the frame will resist closing
    // and that under whatever circumstances, it only closes once.
	private boolean pleaseKill = false;
	private boolean dontKill = false;

	public void killFrame()
    { 
        pleaseKill = true;			// remember some tried to do it
        //
        // record the default position and size
        if(!G.isCodename1())
        {
            String suffix = "-"+ getTitle();
	       	Preferences prefs = Preferences.userRoot();
        	Rectangle dim = getBounds();
        	prefs.put(FRAMEBOUNDS+suffix,
        			""+G.Left(dim)+","+G.Top(dim)+","+G.Width(dim)+","+G.Height(dim));
        	
        }
        //G.print("Killframe");
        if ((dontKill == false)		// temporarily inhibited?
        		&&(killed==false))	// already killed
        {	RootAppletProtocol p = G.getRoot();
        	if (p != null)
	            {
	            p.killFrame(this);
	            }
            //dispose();
            setVisible(false);
            dispose();
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

	public MenuParentInterface getMenuParent() {
		return this;
	}
	
    public void initMenus()
    {	initMenu(actions);
    	initMenu(options);
        boolean defaultSound = Config.Default.getBoolean(Config.Default.sound);
        soundCheckBox = addOption(s.get(SoundMessage), defaultSound,null);
        soundCheckBox.setForeground(Color.blue);
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

}