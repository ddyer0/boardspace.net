package bridge;

import lib.ErrorX;
import lib.G;
import lib.Http;
import lib.SizeProvider;

import java.util.Vector;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import lib.Graphics;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

class FixedTopLayout extends BorderLayout
{
	int height;
	FixedTopLayout(int h) { height=h;}
	public Dimension getPreferredSize(Container parent) {
		return(new Dimension(G.getFrameWidth(),(int)(height*G.getDisplayScale())));
	}
	
}
@SuppressWarnings("rawtypes")
public class MasterForm extends Form implements com.codename1.ui.events.ActionListener
	,Config
{	private static MasterForm masterForm = null;
	private static MasterPanel masterPanel = null;
	bridge.Container tabs = new bridge.Container();
	bridge.Container menus = new bridge.Container();
	String appname = "unnamed";
	private com.codename1.ui.Container titleBar;
	boolean recordEvents = true;
	
	@SuppressWarnings("deprecation")
	private MasterForm(String app) 
	{ super(app);
	  new BoxLayout(this,BoxLayout.Y_AXIS);
	  UIManager man = UIManager.getInstance();
	  man.setLookAndFeel(new BSLookAndFeel(man));
	  tabs.setUIID("ContainerMasterForm");
	  menus.setUIID("ContainerMasterForm");
	  setAllowEnableLayoutOnPaint(true);	// 1/2020 added this incantation to avoid "blank screen" on startup
	  //G.print("Display drag "+Display.getInstance().getDragStartPercentage());
	  // note the codename1 default was 3 = 3% of the screen
	  // this also requires the individual components to implement getDragRegionStatus()
	  //
	  Display.getInstance().setDragStartPercentage(1);
	  appname = app; 
	  //new BoxLayout(this,BoxLayout.Y_AXIS);
	  tabs.setLayout(new TabLayout());
	  new BoxLayout(menus,BoxLayout.X_AXIS);
	  Display.getInstance().addEdtErrorHandler(this);
	  titleBar = getTitleArea();
	  // there is some bug related to the status bar, present at the top of IOS devices.
	  // constant "paintsTitleBarBool" inhibits adding a spacer to compensate for it.
	  // but there's a bug induced by clicking on the chat "to user" button that puts it
	  // there anyway, and messes up the status bar.  Hence we avoid the whole mess and
	  // add our own status bar spacer.
	  titleBar.setUIID("TitleAreaMasterForm");	// our own structure with a margin on top
	  if(!G.isIOS())
	  {
		Style s = titleBar.getStyle();
		s.setMargin(0,0,0,0);			// remove the margin except on ios
		s.setPadding(0,0,0,0);
	  }
	  titleBar.setLayout(new FixedTopLayout(25));
	  com.codename1.ui.Label l = getTitleComponent();
	  l.setUIID("LabelMasterForm");
	  l.getStyle().setOpacity(255);
	  l.getStyle().setBgColor(Config.FrameBackgroundColor.getRGB());
	  
	  titleBar.add("Center",tabs);
	  titleBar.add("East",menus);
	  
	}
	public void setFocused(com.codename1.ui.Component p)
	{	
		super.setFocused(p);
	}
	public void requestFocus()
	{	requestFocus(null);
		super.requestFocus();
	}
	//public void setFocused(KeyListener p)
	//{	focusedListener = p;
	//}
	public void requestFocus(KeyListener k)
	{	
		focusedListener = k;
		com.codename1.ui.TextArea eos = getEditOnShow();
		
		if(k!=null) 
			{ if(eos==null) 
				{ G.print("Setting edit on show");
				  eos = new com.codename1.ui.TextArea()
							{ public void keyPressed(int code) { G.print("pressed "+code); }
							};
				  this.add(eos);
				  eos.setVisible(true);
				  setEditOnShow(eos);
				}
			  eos.requestFocus();
			  G.print("Start editing");
			  getEditOnShow().startEditingAsync();

			}
		else if(eos!=null)
			{
			G.print("stop editing "+eos.getText());
			eos.stopEditing(); 
			}
	}
	
	public void paint(com.codename1.ui.Graphics g)
	{
		int w = getWidth();
		// paint the space above the top bar with its own background color
		// this covers the bug in ios that we avoid by creating our own spacer.
		g.setColor(titleBar.getStyle().getBgColor());
		g.fillRect(0,0,w,titleBar.getY());
		super.paint(g);

	}
	private void changeTitle()
	{ 
	  com.codename1.ui.Label title = getTitleComponent();
	  //Style style = title.getStyle();
	  //Font font = style.getFont();
	  //style.setFont(font.derive(12,Font.STYLE_PLAIN));
	  String name = appname;
	  if(G.isSimulator())
	  {
		  name+=" "+getWidth()+"x"+getHeight();
	  }
	  title.setText(name);
	}
	public void setWidth(int n)
	{
		super.setWidth(n);
	}
	public void setHeight(int n)
	{	//G.print("master set height "+n);
		super.setHeight(n);
	}
	public void setSize(Dimension d)
	{	//G.print("master set size "+d);
		super.setSize(d);
	}
	public void layoutContainer()
	{	
		super.layoutContainer();
		//Rectangle bb = getBounds();
		//Http.postError(this,"Layout "+bb,null);
	}
	public void showInEdt()
	{	try {
		MasterPanel p = getMasterPanel();
		super.show();
		changeTitle();
		int w = getWidth();
		@SuppressWarnings("deprecation")
		int titleh = getTitleArea().getHeight();
		int h = getHeight()-titleh;
		p.setX(0);
		p.setY(titleh);
		p.setSize(new Dimension(w,h));
		setScrollable(false);
		p.setVisible(true);
		}
		catch (ThreadDeath err) { throw err;}
		catch (Throwable err) { Http.postError(this,"showing master form",err); }
		
	}
	
public bridge.Container getTabs() { return(tabs); }
public bridge.Container getMenus() { return(menus); }

public void addToMenus(JButton m)
{	m.setUIID("ButtonMasterForm");
	masterForm.getMenus().add(m);
}
	
	public void show()
	{	if(!isVisible()) 
		{ 	G.runInEdt(new Runnable () {	public void run() { showInEdt(); } });
		}
	}
	public static MasterForm getMasterForm()
	{
		if(masterForm==null) { masterForm=new MasterForm(Config.APPNAME); }
		return(masterForm);
	}
	private void addMasterPanel()
	{	try {
	  if(masterPanel==null)
		  {MasterPanel mp = masterPanel = new MasterPanel(this);
		  add(mp);
		  mp.setVisible(true);
		  masterPanel = mp;
		  }
		}
		catch (ThreadDeath err) { throw err;}
		catch (Throwable err) { Http.postError(this,"adding master panel",err); }
	}
	public static MasterPanel getMasterPanel()
	{	final MasterForm form = getMasterForm();
		if(masterPanel==null)
			{
			G.runInEdt(new Runnable() { public void run() { form.addMasterPanel(); }});
			}
		  return(masterPanel);
	}
	public void actionPerformed(ActionEvent evt) {
		if(evt==null) { G.print(Http.stackTrace("null event in error catcher!")); }
		else
		{
		Object er = evt.getSource();

		switch(evt.getEventType())
		{
		default:
		{
		
		if(er instanceof Throwable)
		{
		Throwable error = (Throwable)er;
		Display.getInstance().removeEdtErrorHandler(this);
		Http.postError(this,"last chance error catcher",error);
		Display.getInstance().addEdtErrorHandler(this);
		}
		else { Http.postError(this,"unexpected event from ",new ErrorX(er.toString())); }
		}}}
	}
	
	public static com.codename1.ui.Component getMyChildContaining(com.codename1.ui.Component p,com.codename1.ui.Component c)
	{	if(p==c) { return(null); }
		com.codename1.ui.Component par = c.getParent();
		if(par==p) { return(c); }
		else { if(par==null) { return(null); }
			   return(getMyChildContaining(p,par));
		}
	}
	public static boolean canRepaintLocally(com.codename1.ui.Component c)
	{	// this will never create the masters.  It's intended to return true if
		// we're still in the process of initialization.
		if((masterForm ==null) || (masterPanel==null)) { return(true); }
		if(masterForm.titleBar!=null) { if(getMyChildContaining(masterForm.titleBar,c)!=null) { return(true); }}
		return(masterPanel.canRepaintLocally(c));
	}
	public static boolean canRepaintLocally(Graphics g)
	{
		// this will never create the masters.  It's intended to return true if
		// we're still in the process of initialization.
		if((masterForm ==null) || (masterPanel==null)) { return(true); }
		return(masterPanel.canRepaintLocally(g));
	}
	
	public static boolean isInFront(com.codename1.ui.Component c)
	{
		if((masterForm==null)||(masterPanel==null)){ return true;}
		return masterPanel.isInFront(c);
	}
	public static boolean isCompletelyVisible(com.codename1.ui.Component c)
	{
		if((masterForm==null)||(masterPanel==null)){ return true;}
		return masterPanel.isCompletelyVisible(c);
	}
	public static boolean isPartlyVisible(com.codename1.ui.Component c)
	{
		if((masterForm==null)||(masterPanel==null)){ return true;}
		return masterPanel.isPartlyVisible(c);
	}

	public static boolean isEmpty()
	{	return(masterPanel!=null ? (masterPanel.getComponentCount()==0) : true);
	}
	public static void moveToFront(com.codename1.ui.Component c)
	{	if(masterPanel!=null) { masterPanel.moveToFront(c); }
	}
	// the size of the master frame height available to users.
	public static int getFrameWidth()
	{	if(masterPanel!=null) { return(masterPanel.getWidth()); }
		return(G.getScreenWidth());
	}
	// the size of the master frame height available to users
	public static int getFrameHeight()
	{	if(masterPanel!=null) { return(masterPanel.getHeight()); }
		return(G.getScreenHeight());
	}
	private Point moveNearerMenu(int x,int y)
	{	try {
		Container menus = getMenus();
		if(menus!=null)
		{	
			for(int lim = menus.getComponentCount()-1; lim>=0; lim--)
			{
				com.codename1.ui.Component m = menus.getComponentAt(lim);
				if(m!=null)
				{
				int ax = m.getAbsoluteX();
				int ay = m.getAbsoluteY();
				int aw = m.getWidth();
				int ah = m.getHeight();
				if(G.pointInRect(x, y,ax-2,ay-2,aw+4,ah+4))
				{	//if(!G.pointInRect(x,y,ax,ay,aw,ah)) { G.print("moved to "+m); }
					return(new Point(ax+aw/2,ay+ah/2));
				}}
			}
		}
		}
		catch (Throwable err) 
		{ G.print("Error in moveNearerMenu "+err); 
		}
		return(null);
	}
	public void pointerPressed(int xa[],int ya[])
	{	if(xa!=null && xa.length>0)
		{
		Point p = moveNearerMenu(xa[0],ya[0]);
		if(p!=null) { pointerPressed(p.getX(),p.getY()); }
		else { super.pointerPressed(xa,ya); }
		}
	}
	public void pointerReleased(int xa[],int ya[])
	{	if(xa!=null && xa.length>0)
		{
		//G.startLog("pointer released");
		Point p = moveNearerMenu(xa[0],ya[0]);
		if(p!=null) { pointerReleased(p.getX(),p.getY()); }
		else { super.pointerReleased(xa,ya); }
		}
	}
	private int globalRotation=(useNative() && G.getScreenWidth()<G.getScreenHeight()) ? 1 : 0;
	private static boolean useNative() 
	{	return(G.isAndroid());
	}
	public static int getGlobalRotation()
	{	return(getMasterForm().globalRotation); 
	}
	public static void setGlobalRotation(int n) {
		MasterForm f = getMasterForm();
		f.globalRotation = n&3;
		if(useNative())
		{	G.setOrientation((n&1)!=0,(n&2)!=0);
		}
	}

    /* support for rotating windows */
	public static int translateX(SizeProvider client,int x) 
	{ return( useNative() ? x 
			: ((getGlobalRotation())&2)!=0
				? client.getWidth()-x
				: x);
	}
	public static int translateY(SizeProvider client,int y) 
	{ return( useNative() ? y 
				: ((getGlobalRotation())&2)!=0
					? client.getHeight()-y
					: y);
	}

	public static boolean rotateNativeCanvas(com.codename1.ui.Component client,com.codename1.ui.Graphics g)
	{	boolean v = !useNative() && rotateNativeCanvas(g,client.getWidth()/2,client.getHeight()/2);;
		//if(v) { System.out.println("ro "+client+" "+g); }
		return v;
	}
	public static boolean rotateCanvas(Component client,Graphics g)
	{	boolean v = !useNative() && rotateNativeCanvas(g==null?null:g.getGraphics(),client.getWidth()/2,client.getHeight()/2);;
		//if(v) { System.out.println("ro "+client+" "+g); }
		return v;
	}
	static com.codename1.ui.Graphics rotated = null;
	public static boolean rotateNativeCanvas(com.codename1.ui.Graphics g,int cx,int cy)
	{
		if((g!=null ) && (g!=rotated) && !useNative() )
		{
		int rot = getGlobalRotation();
		if(rot!=0)
		{ 	rotated = g;
			g.resetAffine();
			g.rotateRadians((float)(rot*Math.PI)/2,g.getTranslateX()+cx,g.getTranslateY()+cy); 
 			return(true);
		}}
		return(false);
	}
	
	public static void unrotateNativeCanvas(com.codename1.ui.Component client,com.codename1.ui.Graphics g)
	{	//System.out.println("un "+client+" "+g);
		if(!useNative()) { unrotateCanvas(g,client.getWidth()/2,client.getHeight()/2); }
	}
	
	public static void unrotateCanvas(Component client,Graphics g)
	{	//System.out.println("un "+client+" "+g);
		if(!useNative()) { unrotateCanvas(g==null?null:g.getGraphics(),client.getWidth()/2,client.getHeight()/2); }
	}
	public static void unrotateCanvas(Graphics g,int cx,int cy)
	{
		unrotateCanvas(g.getGraphics(),cx,cy);
	}
	public static void unrotateCanvas(com.codename1.ui.Graphics g,int cx,int cy)
	{	if(!useNative())
		{
 			int rot = getGlobalRotation();
 			if(rot!=0)
 			{
 			if(rotated==null)
 			{
 				System.out.println("not rotated");
 			}
 			rotated = null;
 			g.rotateRadians((float)-(rot*Math.PI)/2,g.getTranslateX()+cx,g.getTranslateY()+cy); 
 			}

		}
	}
	Vector<KeyListener> keylisteners = null;
	KeyListener focusedListener = null;
	public void addKeyListener(KeyListener myrunner)
	{	
		if(keylisteners==null) { keylisteners = new Vector<KeyListener>(); }
		if(!keylisteners.contains(myrunner)) { keylisteners.addElement(myrunner); }
	}
	public void removeKeyListener(KeyListener myrunner)
	{
		if(keylisteners!=null) { keylisteners.remove(myrunner); }
	}
	public void fireKeyEvent(int keycode,boolean pressed)
	{	//G.print("KeyEvent "+keycode+" "+focusedListener+" "+pressed);
		if(keylisteners!=null)
		{	int code = keycode;
			//System.out.println("key "+Integer.toHexString((keycode&0xff))+" "+keycode);
			switch(keycode)
			{
			case -90: code = '\r'; break;
			default: break;
			}
			for(KeyListener k : keylisteners)
			{	KeyEvent event = new KeyEvent(code);
				//if(k==focusedListener)
				{
				if(pressed)
				{
				k.keyPressed(event);
				}
				else 
				{
				k.keyReleased(event);
				k.keyTyped(event);
				}
			}}
		}
	}
	public void keyPressed(int keycode)
	{	
		fireKeyEvent(keycode,true);
		super.keyPressed(keycode);;
	}
	public void keyReleased(int keycode)
	{	
		fireKeyEvent(keycode,false);
		super.keyReleased(keycode);
	}
	   public void paintBackgrounds(Graphics g)
	    {	//System.out.println("master paintBackgrounds "+this);
	    }
	    public void paintComponentBackground(Graphics g)
	    { //G.print("master paintComponentBackground" );
	    }
}