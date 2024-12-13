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

class FixedTopLayout extends com.codename1.ui.layouts.BorderLayout
{
	FixedTopLayout() {  }
	public void layoutContainer(com.codename1.ui.Container parent)
	{	//G.print("Layout master, scale ",isScaleEdges());
		MasterToolBar.getSafeAreaHeight();
		super.layoutContainer(parent);
	}
}
class MasterToolBar extends Toolbar
{
	MasterToolBar()
	{
		super();
		setLayout(new FixedTopLayout());
		setUIID("TitleAreaMasterForm");	// our own structure with a margin on top

	}
	static int savedNotch = -1;
	
	static int getSafeAreaHeight()
	{
		Rectangle rect = Display.getInstance().getDisplaySafeArea(new Rectangle());
		int current = rect.getY();
		if(G.isIOS())
		{
			// this is a kludge to paper over a codenameone bug, that iphones
			// return an impossible dissonant safe area when held horizontally
			// it also fails to swap displaywidth and displayheight
		MasterForm mf = MasterForm.masterForm;
		if (mf!= null)
		{
		int disw = mf.getWidth();
		int dish = mf.getHeight();
		int rw = rect.getWidth();
		int rh = rect.getHeight();
		boolean dissonent = (rw<rh != disw<dish);
		// G.print("disw=",disw," dish=",dish," ",dissonent);
		
		G.print("initial safe x=",rect.getX()," y=",rect.getY()," w=",rect.getWidth()," h=",rect.getHeight());

		if(dissonent) { 
			current = (disw>dish) ? 0 : Math.max(0,savedNotch);
			rect.setY(current);
			rect.setX(current==0 ? Math.max(0,savedNotch) : 0);
			rect.setWidth(rh);
			rect.setHeight(rw);
			G.print("safe area dissonent, ",rw,"x",rh," vs ",disw,"x",dish);
			}
			else
			{
			savedNotch = Math.max(rect.getX(),current);
			}
		}}
		// this is a hack for the simulator, set the safe area by modding the official rect
		// rect.setY(150);
		G.print("final safe x=",rect.getX()," y=",rect.getY()," w=",rect.getWidth()," h=",rect.getHeight());
		G.print("current ",current);
		return current;
	}
	int remembered = -1;
	public Dimension getPreferredSize()
	{	// this is the essential repair to the toolbar logic.  Include
		// the height of the safe area in the toolbar size.  Other kludgery
		// in com.codename1.ui.Container will add a margin corresponding to
		// the safe area.
		Style s = getStyle();
		s.setPaddingUnitTop(Style.UNIT_TYPE_PIXELS);
		s.setPaddingTop(getSafeAreaHeight());
		Dimension sz = super.getPreferredSize();
		if(G.isIOS() && savedNotch>=0)
		{
		// this is complete kludgery - iphones get it right the first time
		// so remember the answer and reuse it. At least until codename1's notch
		// logic is fixed.
		if(remembered<0) { remembered = sz.getHeight()-savedNotch; }
		else {
			int oldsize = sz.getHeight();
			int newsize = remembered+getSafeAreaHeight();
			if(oldsize!=newsize)
			{
				sz.setHeight(newsize);
				G.print("kludge:  toolbar size changed from ",oldsize," to ",newsize);
			}
		}}
		G.print("toolbar preferred size ",sz.getWidth(),"x",sz.getHeight());
		return sz;
	}
}
@SuppressWarnings("rawtypes")
public class MasterForm extends Form implements com.codename1.ui.events.ActionListener,Config
{	static MasterForm masterForm = null;
	private static MasterPanel masterPanel = null;
	JPanel tabs = new JPanel(new TabLayout(),"ContainerMasterForm");
	JPanel menus = new JPanel(new TabLayout(),"ContainerMasterForm");
	JPanel centers = new JPanel(new TabLayout(),"ContainerMasterForm");
	Spacer spacer = null;
	String appname = "unnamed";
	private MasterToolBar toolBar;
	boolean recordEvents = true;
	public void adjustTabStyles()
	{
		toolBar.setShouldCalcPreferredSize(true);
	}
	public com.codename1.ui.Container add(com.codename1.ui.Component c)
	{
		return super.add(c);
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
	  new BoxLayout(this,BoxLayout.Y_AXIS);
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
}
