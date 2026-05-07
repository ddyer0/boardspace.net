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
package bridge;

import lib.ErrorX;
import lib.G;
import lib.Http;
import lib.NullLayout;
import lib.NullLayoutProtocol;
import lib.Plog;
import lib.SizeProvider;
import lib.TopFrameProtocol;

import java.util.Vector;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;

import lib.Graphics;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;


class MasterToolBar extends Toolbar
{
	MasterToolBar()
	{
		super();
		setLayout(new BorderLayout());
		setUIID("TitleAreaMasterForm");	// our own structure with a margin on top

	}
	static int savedNotch = -1;
}
@SuppressWarnings("rawtypes")
public class MasterForm extends Form implements com.codename1.ui.events.ActionListener,Config,NullLayoutProtocol
{	static MasterForm masterForm = null;
	private static MasterPanel masterPanel = null;
	JPanel tabs = new JPanel(new TabLayout(),"ContainerMasterForm");
	JPanel menus = new JPanel(new TabLayout(),"ContainerMasterForm");
	JPanel centers = new JPanel(new TabLayout(),"ContainerMasterForm");
	Spacer spacer = null;
	String appname = "unnamed";
	private MasterToolBar toolBar;
	boolean recordEvents = true;
	
	//
	// galaxy s24 results, display w = 1440 h = 3120
	//
	// normal : 0,113, 1440, 2839 (also upside down) = 168 bottom
	// horizontal ccw  112,84,2840, 1356 = 168 right
	// horizontal cw  168, 84, 2840, 1356 = 112 right

	//  big tablet actual size 1920x1080 
	//  screen size landscape 1920x1008
	//  master size landscape 1920x1008
	//  screen size portrait 1080x1848
	//  master size portrait 1080x1848
	
	// phone actual size 1080x2408
	// master size 1080x2199
	// master safe 0,0 1089x2199 (top and bottom safe areas excluded)
	
	// ipad actual size 1536x2048
	// master portrait size 1536x1945
	// master portrait safe 0,40 1536x1945
	
	// android 16 tablet landscape 1280x800 screen=1280x800 safe 0,24 1280x720 frame=1280x720 panel=0,0 1280x720
	//		             portrait  800x1280 screen=800x1280 safe 0,24 800x1200 frame=800x1200 panel=0,0 800x1200
	//
	// android 16 phone  landscape 3120x1440                safe 112,84 2840x1356 frame 3120x1440 (twist cw)
	//				     landscape 3120x1440			    safe 168,84 2840x1356 frame 
	//					 portrait  1440x3120				safe 0,113  1440x2839  frame 1440x3120 
	//
	// online platform Android 
	// vertical  screen=1440x3120 safe 0,154 1440x2798 frame=1440x2696 panel=0,0 1440x2696 ip 81.2.66.243
	// horizontal cw [2026/04/27 21:19:04] offline platform Android 26.97 screen=1440x3120 safe 0,154 1440x2798 frame=1440x2696 panel=0,0 1440x2696 ip 81.2.66.243

	public static Rectangle getSafe()
	{	Rectangle r = Display.getInstance().getDisplaySafeArea(new Rectangle());
		return new Rectangle(G.Left(r),G.Top(r),G.Width(r),G.Height(r));
	}
	public static String getSafeBounds()
	{
		Rectangle s = getSafe();
		return G.concat(s.getX(),",",s.getY()," ",s.getWidth(),"x",s.getHeight());
	}
	public void adjustTabStyles()
	{
		toolBar.setShouldCalcPreferredSize(true);
	}

	@SuppressWarnings("deprecation")
	public void initGlobalToolbar()
	{
		toolBar = new MasterToolBar();
		setToolBar(toolBar);
	}

	public void showStyles()
	{
		Rectangle rect = Display.getInstance().getDisplaySafeArea(new Rectangle());
		int w = Display.getInstance().getDisplayWidth();
		int h = Display.getInstance().getDisplayHeight();
		G.print("Display w=",w," h=",h," safe l=",G.Left(rect)," t=",G.Top(rect)," r=",G.Right(rect)," b=",G.Bottom(rect));
		{
		Rectangle bounds = getBounds();
		G.print("form l=",G.Left(bounds)," t=",G.Top(bounds)," r=",G.Right(bounds)," b=",G.Bottom(bounds));
		}
		
		{
			Style c = menus.getStyle();
			G.print("Menus Style ",c," margin ",c.getMarginTop()," ",c.getMarginBottom(),
					  	" pad ",c.getPaddingTop(),"+",c.getPaddingBottom());
		}
		{
			Style c = toolBar.getStyle();
			Rectangle bounds = toolBar.getBounds(new Rectangle());
			G.print("toolbar l=",G.Left(bounds)," t=",G.Top(bounds)," r=",G.Right(bounds)," b=",G.Bottom(bounds));
			G.print("Toolbar Style ",c," margin ",c.getMarginTop()," ",c.getMarginBottom(),
					" pad ",c.getPaddingTop(),"+",c.getPaddingBottom());
		}
		{
			Style c = getStyle();
			G.print("Form Style ",c," margin ",c.getMarginTop()," ",c.getMarginBottom(),
					" pad ",c.getPaddingTop(),"+",c.getPaddingBottom());
		}

	}
	//
	// this is for an experiment in progress, to catch keys by pulling
	// them into an invisible text window.
	//
	TextArea keys = new TextArea(1,20);
	@SuppressWarnings("deprecation")
	private MasterForm(String app) 
	{ super(app);
	  setLayout(new NullLayout(this));
	  UIManager man = UIManager.getInstance();
	  man.setLookAndFeel(new BSLookAndFeel(man));
	  showStyles();
	  setAllowEnableLayoutOnPaint(true);	// 1/2020 added this incantation to avoid "blank screen" on startup
	  //G.print("Display drag "+Display.getInstance().getDragStartPercentage());
	  // note the codename1 default was 3 = 3% of the screen
	  // this also requires the individual components to implement getDragRegionStatus()
	  //
	  Display.getInstance().setDragStartPercentage(1);
	  Display.getInstance().setMultiKeyMode(true);

	  appname = app; 
	  //new BoxLayout(this,BoxLayout.Y_AXIS);
	  Display.getInstance().addEdtErrorHandler(this);
	  // there is some bug related to the status bar, present at the top of IOS devices.
	  // constant "paintsTitleBarBool" inhibits adding a spacer to compensate for it.
	  // but there's a bug induced by clicking on the chat "to user" button that puts it
	  // there anyway, and messes up the status bar.  Hence we avoid the whole mess and
	  // add our own status bar spacer.
	  if(!G.isIOS())
	  {
		Style s = toolBar.getStyle();
		s.setMargin(0,0,0,0);			// remove the margin except on ios
		s.setPadding(0,0,0,0);
	  }
//	  com.codename1.ui.Label l = getTitleComponent();
//	  setTitle("");	  
//	  l.setUIID("LabelMasterForm");
	  //l.getStyle().setOpacity(255);
	  //l.getStyle().setBgColor(Config.FrameBackgroundColor.getRGB());
	  //toolBar.add("North",spacer);
	  toolBar.add("West",tabs);
	  toolBar.add("East",menus);
	  toolBar.add("Center",centers);  
	  TextField.setUseNativeTextInput(false);
	}
	public void setFocused(com.codename1.ui.Component p)
	{	//G.print("set focused ",p,p==null?"":p.isFocusable());
		super.setFocused(p);
		//G.print(getFocused());
	}
	public void requestFocus()
	{	//G.print("request focused ");
		requestFocus(null);
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
	public void setVisible(boolean v)
	{	
		super.setVisible(v);
	}
	public void paint(com.codename1.ui.Graphics g)
	{
		int w = getWidth();
		// paint the space above the top bar with its own background color
		// this covers the bug in ios that we avoid by creating our own spacer.
		g.setColor(toolBar.getStyle().getBgColor());
		g.fillRect(0,0,w,toolBar.getY());
		super.paint(g);

	}
	private void changeTitle()
	{ 
	  com.codename1.ui.Label title = getTitleComponent();
	  //Style style = title.getStyle();
	  //FontManager font = style.getFont();
	  //style.setFont(font.derive(12,FontManager.STYLE_PLAIN));
	  String name = appname;
	  if(G.isSimulator())
	  {
		  name+=" "+getWidth()+"x"+getHeight();
	  }
	  title.setText(name);
	}
	public void setWidth(int n)
	{	Plog.log.addLog("master set width "+n);
		Plog.log.addLog("stack: ",G.getStackTrace());
		super.setWidth(n);
	}
	public void setHeight(int n)
	{	//Plog.log.addLog("master set height "+n);
		super.setHeight(n);
	}
	public void setSize(Dimension d)
	{	Plog.log.addLog("master set size "+d);
		Plog.log.addLog("stack: ",G.getStackTrace());
		super.setSize(d);
	}
	private Rectangle oldbounds = null; 
	public void layoutContainer()
	{	Rectangle newbounds = getBounds();
		if(!newbounds.equals(oldbounds))
		{
		oldbounds = newbounds;
		//adjustSpacer();
		super.layoutContainer();
		}
	}

	public void showInEdt()
	{	try {
		MasterPanel p = getMasterPanel();
		super.show();
		changeTitle();
		int w = getWidth();
		@SuppressWarnings("deprecation")
		int titleh = getTitleArea().getHeight();
		int h = Math.min(getSafe().getHeight(),getHeight())-titleh;		
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
	masterForm.getMenus().addC(m);
	masterForm.adjustTabStyles();
}

	public void show()
	{	if(!isVisible()) 
		{ 	G.runInEdt(new Runnable () {	public void run() { showInEdt(); } });
		}
		getMasterPanel().masterFrameShow();
	}
	public static synchronized MasterForm getMasterForm()
	{
		if(masterForm==null) 
			{ masterForm=new MasterForm(Config.APPNAME); 
			}
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
	
	public static String getPanelBounds()
	{
		if(masterPanel==null) { return ""; }
		return G.concat(masterPanel.getX(),",",masterPanel.getY()," ",masterPanel.getWidth(),"x",masterPanel.getHeight());
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
		if(masterForm.toolBar!=null) { if(getMyChildContaining(masterForm.toolBar,c)!=null) { return(true); }}
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
	public static void moveToFront(TopFrameProtocol c)
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
	//
	// this is used to fatten up the top menu bar and exit boxes, without
	// changing the visual space.  In effect, the top few pixels of the
	// screen are not usable for boxes, only for display.
	//
	private Point moveNearerMenu(int x,int y)
	{	Point p = moveNearerBar(getMenus(),x,y);
		if(p==null) { p = moveNearerBar(getTabs(),x,y); }
		return p;
	}
	private Point moveNearerBar(Container menus,int x,int y)
	{
		try {
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
				if(G.pointInRect(x, y,ax-2,0,aw+4,ah+ay+ah/2))
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
	public void pointerHover(int x[],int y[])
	{	//as of 12/2022, this is called on the simulator but not on real devices
		//G.print("Form Hover ",x[0]," ",y[0]);
		super.pointerHover(x,y);
	}
	
	/*
	private String toString(int ar[])
	{
		StringBuilder b = new StringBuilder();
		b.append("len "+ar.length);
		b.append("[");
		for(int i=0;i<ar.length;i++) { b.append(" "); b.append(ar[i]); }
		b.append("]");
		return b.toString();
	}*/
	public void pointerPressed(int xa[],int ya[])
	{	if(xa!=null && xa.length>0)
		{
		//if(ya[0]<=20)
		//{
		//	G.print("Low Y ",toString(xa),toString(ya));
		//}

		Point p = moveNearerMenu(xa[0],ya[0]);
		//G.print("mpress ",xa[0]," ",ya[0]);
		if(p!=null) 
			{  xa[0]=p.getX(); ya[0]=p.getY(); 
			   // G.print("press moved ",xa[0]," ",ya[0]);
			}
		super.pointerPressed(xa,ya); 
		}
	}
	public void pointerPressed(int x,int y)
	{
		//G.print("\npressed ",x," ",y);
		//G.print(G.getStackTrace());
		super.pointerPressed(x,y); 
	}
	public void pointerReleased(int x,int y)
	{
		//G.print("\nreleased ",x," ",y);
		//G.print(G.getStackTrace());
		super.pointerReleased(x,y); 
	}
	public void pointerDragged(int x,int y)
	{
		//G.print("\ndragged ",x," ",y);
		//G.print(G.getStackTrace());
		super.pointerDragged(x,y); 
	}
	public void pointerDragged(int xa[],int ya[])
	{	if(xa!=null && xa.length>0)
		{
			//G.print("\ndraggs ",xa.length," ",xa[0]," ",ya[0]);
			//G.print(G.getStackTrace());
			super.pointerDragged(xa,ya);
		}
	}
	public void pointerReleased(int xa[],int ya[])
	{	if(xa!=null && xa.length>0)
		{
		//G.startLog("pointer released");
		//G.print("mrelease ",xa[0]," ",ya[0]);
		Point p = moveNearerMenu(xa[0],ya[0]);
		if(p!=null) 
			{ xa[0]=p.getX(); ya[0]=p.getY(); 
			  //G.print("release moved ",xa[0]," ",ya[0]);
			}
		super.pointerReleased(xa,ya);
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
	{	//Plog.log.addLog("add key listener ",myrunner);
		if(keylisteners==null) { keylisteners = new Vector<KeyListener>(); }
		if(!keylisteners.contains(myrunner)) { keylisteners.addElement(myrunner); }
	}
	public void removeKeyListener(KeyListener myrunner)
	{	//Plog.log.addLog("remove key listener ",myrunner);
		if(keylisteners!=null) { keylisteners.remove(myrunner); }
	}
	// 
	// this is for an experiment in progress, to use cn1 keylistener facility to catch
	// every key separately
	//
	public void addKeyListener(int keyCode, com.codename1.ui.events.ActionListener listener)
	{
		super.addKeyListener(keyCode,listener);
	}
	public void fireKeyEvent(int keycode,boolean pressed)
	{	//Plog.log.addLog("KeyEvent ",keycode,"(0x",Integer.toHexString((keycode&0xff)),")",
		//		focusedListener," ",pressed);
		if(keylisteners!=null)
		{	int code = keycode;
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
				k.keyReleased(event);
				k.keyTyped(event);
				}
				else 
				{
				// codename1 has a bug, some "up" events never arrive, so put them all in "down"
				// issue #3660
				//	k.keyReleased(event);
				//	k.keyTyped(event);
				}
			}}
		}
	}

	public void keyPressed(int keycode)
	{	fireKeyEvent(keycode,true);
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
	public com.codename1.ui.Container getTitleBar() { return toolBar; }
	
	// the spacer is an invisible window on the top of iphone-X screens, meant to occupy
	// the divot which contains the status bar.  When switching to landscape mode, the 
	// divot moves to the left instead of top, and it's accounted for by the way the 
	// masterpanel does it's layout.
	public void adjustSpacer()
	{	if(spacer==null) { spacer = new Spacer(); }
		Rectangle safe = getSafeArea();
		//int sx = safe.getX();
		int sy = (int)(safe.getY()*0.66);
		int sw = safe.getWidth();
		int spw = spacer.getWidth();
		int sph = spacer.getHeight();
		if(spw!=sw || sph!=sy)
			{
			//int sh = safe.getHeight();
			//G.print("layout title safe "+sx+" "+sy+" "+sw+"x"+sh);
			spacer.setWidth(sw);
			spacer.setHeight(sy); 
			toolBar.setShouldCalcPreferredSize(true);
			Container mp = masterPanel;
			if(mp!=null) { mp.setShouldCalcPreferredSize(true); }
			setShouldCalcPreferredSize(true);
			}
	}

	public void doNullLayout() {
		Rectangle safe = getSafe();
		int safeX = safe.getX();
		int safeY = safe.getY();
		int safeW = safe.getWidth();
		int safeH = safe.getHeight();
		// manually layout the title bar and contents pane to avoid the safe area
		com.codename1.ui.Container bar = getTitleBar();
		com.codename1.ui.Container content = getContentPane();
		//Rectangle sz = getBounds();
		Dimension barsize = bar.getPreferredSize();
		int barH = barsize.getHeight();
		// bar sits just below the safe area
		bar.setX(safeX);
		bar.setY(safeY);
		bar.setWidth(safeW);
		bar.setHeight(barH);
		// content covers the rest of the safe area
		content.setX(safeX);
		content.setY(safeY+barH);
		content.setWidth(safeW);
		if(G.isIOS()) {
			// ignore the bottom safe area on idevices, it's actually usable
			content.setHeight(getHeight()-barH-safeY);
		}
		else
		{	// bottom safe area on androids is the nav bar
			content.setHeight(safeH-barH);
		}
		
	}
}
