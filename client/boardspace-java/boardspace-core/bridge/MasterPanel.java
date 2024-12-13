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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JPanel;

import lib.G;
import lib.NullLayout;
import lib.NullLayoutProtocol;
import lib.TabFrame;
import lib.TopFrameProtocol;


import lib.Graphics;
import lib.Image;


/**
 * This is the top level container that contains all the full-sized windows,
 * which can be navigated using a tab interface.
 * 
 * @author Ddyer
 *
 */
@SuppressWarnings("serial")
public class MasterPanel extends JPanel implements NullLayoutProtocol,ActionListener,Config
{	public boolean fancyPaint = true;
	private boolean useTabs = G.useTabInterface();		// if true use the tab and menu bars supplied by the masterform
	MasterForm masterForm ;
	Image menuImage = Image.getImage(IMAGEPATH + "menubar-nomask.png");
	Image closeImage = Image.getImage(IMAGEPATH + "closebox-nomask.png");
	Image paperClipImage = Image.getImage(IMAGEPATH+"paperclip-nomask.png");
	Image paperClipSideImage = Image.getImage(IMAGEPATH+"paperclipside-nomask.png");
	Image rotateImage = Image.getImage(IMAGEPATH+"rotate-nomask.png");
	Image rotateQImage = Image.getImage(IMAGEPATH+"rotateq-nomask.png");
	Image rotate3QImage = Image.getImage(IMAGEPATH+"rotateqc-nomask.png");
	JButton menuButton = new JButton("actionmenu",menuImage);
	JButton closeButton = new JButton("close",closeImage);
	JButton paperclipButton = new JButton("savepanzoom",paperClipImage);
	JButton paperclipSideButton = new JButton("restorepanzoom",paperClipSideImage);
	JButton rotateButton = new JButton("rotate",rotateImage);
	JButton twistButton = new JButton("twist",rotateQImage);
	JButton twist3Button = new JButton("twist3",rotate3QImage);
	// the order here defines the order from left to right
	// clipsidebutton is leftmost so it can appear and disappear and look natural
	JButton playtableButtons[] = { paperclipSideButton,paperclipButton,twist3Button,rotateButton,twistButton,menuButton,closeButton };
	public MasterPanel(MasterForm m)
	{	masterForm = m;
		if(useTabs)
		{
		for(JButton b : playtableButtons)
			{
			addToMenus(b);
			b.addActionListener(this);
			}
		}
		setOpaque(false);
		setBackground(Color.gray);
		setLayout(new NullLayout(this));	
	}
	public void addToMenus(JButton m)
	{	if(useTabs)
		{	masterForm.addToMenus(m);
		}
	}
	public void addC(Component cc)
	{	G.Assert(cc.getParent()==null,"already has a parent %s",cc);
		super.add(cc,0);
		if(cc instanceof TopFrameProtocol) 
			{ 
			  addTab((TopFrameProtocol)cc); 
			}
	}

	public void remove(int n)
	{
		Component cc = getComponent(n);
		super.remove(n);
		if(cc instanceof TopFrameProtocol) { removeTab((TopFrameProtocol)cc); }
	
	}
	/**
	 * move c or whichever of its parents is relevant to the back
	 * @param c
	 */
	public void moveToBack(Component cc)
	{	Component c = getMyParent(cc);
		cc.setVisible(false);
		if(c!=null)
			{
			super.remove(getComponentZOrder(c));
			super.add(c,getComponentCount());
			}
	}

	/**
	 * get the one of my children which is c or one of it's parents
	 * @return 
	 */
	public Component getMyParent(Component c)
	{	Component par = c.getParent();
		if(par==this) { return(c); }
		if(par==null) { return(null); }
		return(getMyParent(par));
	}
	/**
	 * move this window or whichever of its parents in relevant to the front 
	 * @param c
	 */
	public void moveToFront(Component cc)
	{	
		Component c = getMyParent(cc);
		if(c!=null && c!=getTopWindow())
		{
		super.remove(getComponentZOrder(c));
		super.add(c,0);
		c.setVisible(true);
		}
		cc.setVisible(true);
	}
	public void moveToFront(TopFrameProtocol cc)
	{
		moveToFront((Component)cc);
	}
	public Component getMyChildContaining(Component c)
	{	return(MasterForm.getMyChildContaining(this, c));
	}
	public Dimension getPreferredSize()
	{	Container parent = getParent();
		return(new Dimension(parent.getWidth(),parent.getHeight()));
	}
	
	//
	// get the component bounds, clipped to our bounds.
	// we allow oversized windows, but the consideration of visibility
	// is limited to the part that can actually be seen.
	//
	private Rectangle getClippedComponentBounds(Component c,Component wrt)
	{	int x = c.getX();
		int y = c.getY();
		int w = c.getWidth();
		int h = c.getHeight();
		if(x<0) { w = w+x; x=0; }
		if(y<0) { h = h+y; y=0; }
		if((w-x)>getWidth()) { w = getWidth()-x; }
		if((h-y)>getHeight()) { h = getHeight()-y; }
		while((c!=wrt) && (c!=null))
		{	c = c.getParent();
			if(c!=null) 
				{ x += c.getX();
				  y += c.getY();
				}
		}
		return(new Rectangle(x,y,w,h));
	}
	public Component safeGetComponentAt(int i)
	{	// getComponentAt will get an error if something is removed 
		// from the hierarchy unexpectedly.  It's a crummy API but what we have.
		try { return(getComponent(i)); }
		catch (IndexOutOfBoundsException err) { return(null); }
	}
	
	public void printParents(String msg,Component p)
	{
		if(p.getParent()!=null)
		{
			while(p!=null) { msg += "\n"+p; p=p.getParent(); }
			msg += "\n";
			G.print(msg);
		}
	}

	public boolean isInFront(Component target)
	{	Component child = getMyChildContaining(target);
		if(child==null)
			{
			//printParents("Front",target);
			return(false); 
			}
		Component top = getTopWindow();
		return(child==top);
	}
	//
	// note that for our purposes, the item at index 0 is the front window.
	//
	public boolean isCompletelyVisible(Component target)
	{	if(this==target) { return(true); }
		if(!useTabs) { return true; }
		Component child = getMyChildContaining(target);
		if(child==null) 
			{ //printParents("vis",target);
			return(false); 
			}
		
		Rectangle targetRect = getClippedComponentBounds(target,child);
		for(int cc = 0,last = getComponentCount(); cc<last; cc++)
		{
			Component other = safeGetComponentAt(cc);
			if(other!=null)
			{
			if(other==child) 
				{ return(true); }
			Rectangle otherRect = getClippedComponentBounds(other,other);
			if(otherRect.intersects(targetRect)) 
				{ return(false); }
			}
		}
		return(false);
	}
	//
	// if not completely visible, is partly visible
	// note that for our purposes, the item at index 0 is the front window.
	//
	public boolean isPartlyVisible(Component target)
	{	if(!useTabs) { return true; }
		Component child = getMyChildContaining(target);
		if(child==null)  { return(false); }
		Rectangle targetRect = getClippedComponentBounds(target,child);
		for(int cc = 0,last = getComponentCount(); cc<last; cc++)
		{
			Component other = safeGetComponentAt(cc);
			if(other!=null)
			{
			if(other==child) { return(false); }
			Rectangle otherRect = getClippedComponentBounds(other,other);
			if(otherRect.contains(targetRect)) { return(false); }
			if(otherRect.intersects(targetRect)) { return(true); }
			}
		}
		return(true);
	}
	
	public boolean overlappingWindows()
	{	for(int cc = getComponentCount()-1; cc>=0; cc--)
		{	Component c = safeGetComponentAt(cc);
			if((c!=null) && isPartlyVisible(c)) { return(true); }
		}
		return(false);
	}

	// don't call this directly, use MasterForm.canRepaintLocally(x);
	protected boolean canRepaintLocally(Component c)
	{	if((!fancyPaint || !overlappingWindows()) && isCompletelyVisible(c)) { return(true); }
		if(c.isVisible())
		{	// if it wants to be visible
			if(isCompletelyVisible(c)) 
				{ return(true);}
			else if(isPartlyVisible(c)) 
				{ repaint(); }	// we repaint from the top
		}
		return(false);
	}
	
	private Graphics topLevelGraphics = null;
	protected boolean canRepaintLocally(Graphics g)
	{
		return(!fancyPaint || (g==topLevelGraphics));
	}
	boolean paintFromTop = false;
	public static void shouldPaintFromTop(Component c)
	{	MasterPanel panel = MasterForm.getMasterPanel();
		G.Assert(panel.paintFromTop || !panel.isPartlyVisible(c),"should be on top");
	}
	Image offScreen = null;
	Image createOffScreen() 
	{	int w = getWidth();
		int h = getHeight();
		if((offScreen!=null) 
				&& (offScreen.getWidth()==w)
				&& (offScreen.getHeight()==h))
		{ }
		else { offScreen = lib.Image.createImage(w,h); }
		if(offScreen==null) { G.print("create offscreen "+w+","+h+" failed");}
		return(offScreen);
	}
	@SuppressWarnings("unused")
	private String componentsSummary(String msg)
	{	
		for(int i=0,cc = getComponentCount(); i<cc; i++)
		{	msg += "\n "+i+": "+getComponent(i);
		}
		return(msg);
	}

	public void paint(java.awt.Graphics g)
	{			
		Image off = null;
		paintFromTop = true;
		
		if(	fancyPaint 
				&& overlappingWindows()
				&& ((off=createOffScreen())!=null))
		{	//G.print(componentsSummary("background Paint from the top"));
			Graphics offG = off.getGraphics();
			topLevelGraphics = offG;
			super.paint(offG.getGraphics());
			topLevelGraphics = null;
			g.drawImage(off.getImage(),0,0,this);
		}
		else 
			{ //String msg = "standard paint from the top "+overlappingWindows();
			  //msg = componentsSummary(msg);
			  //G.print(msg);
			  
			  super.paint(g); 
			}
		paintFromTop = false;
	}
	
	//
	// manage the tabs housed in the master form
	//
	// the title bar of the master form houses a list of the frames that
	// the masterpanel (that's us) has as compoents.  Here we manage
	// that list and keep it in sync with our actual compoenents.
	//
	private Hashtable<TopFrameProtocol,JButton> tabFrames = new Hashtable<TopFrameProtocol,JButton>();
	
	// add a new frame to the tab list
	public void addTab(TopFrameProtocol f)
	{	if (useTabs)
		{
		setTabName(f,f.tabName(),f.getIconAsImage());
		}
	}

	public Image getTabImage(Image im)
	{	Image res[] = new Image[1];
		if(im!=null)
		{	
			Container tabs = masterForm.getTabs();
			int h = Math.max(20,tabs.getHeight()*2/3);
			Runnable r = new Runnable(){ public void run() 
			{
			int imw = im.getWidth();
			int imh = im.getHeight();
			int neww = (int)(imw*((double)h/imh));
			res[0] = im.getScaledInstance(neww, h,Image.ScaleType.SCALE_SMOOTH);	
			}};
			G.runInEdt(r);	
		}
		return res[0];		
	}

	private JButton getTabButton(TopFrameProtocol f,Image im,String newName)
	{
		JButton b = null;
		if(im!=null)
		{	
			b = new JButton(im);
		}
		else 
		{	b = new JButton(newName!=null ? newName : "unnamed");
		}
		return b;
	}

	// change the name of a tab.  This is done when one of the frames
	// changes its name.
	public void setTabName(TopFrameProtocol f,String newName,Image im)
	{	JButton old = tabFrames.get(f);
		JButton b = getTabButton(f,im,newName);
		Container tabs = masterForm.getTabs();
		if(old!=null) { tabs.remove(old); }
		tabs.add(b);
		tabFrames.put(f,b);
		b.addActionListener(this);
		adjustTabStyles();
		masterForm.getTabs().revalidate();		
	}
	// 
	// when we click on one of the tab buttons, select the corresponding frame
	//
	public void selectFrame(TopFrameProtocol fr)
	{	moveToFront(fr);
	}
	public boolean handleTabAction(Object ob)
	{	for(Enumeration<TopFrameProtocol>frames = tabFrames.keys(); frames.hasMoreElements(); )
		{ TopFrameProtocol fr = frames.nextElement();
		  JButton b = tabFrames.get(fr);
		  if(b==ob)
		  {	selectFrame(fr);
		    return(true);
		  }
		}
		return(false);
	}
	
	public Component getTopWindow()
	{
		int cc = getComponentCount();
		return(cc<=0 ? null : safeGetComponentAt(0));
	}

	public void buttonTopWindow(ActionEvent evt,String cmd)
	{
		Component f = getTopWindow();
		if(f instanceof TabFrame)
		{
			TabFrame fr = (TabFrame) f;
			if(handleTabAction(evt.getSource())) {}
			else {
				JButton b = (JButton)evt.getSource();
				int x = G.getAbsoluteX(b);
				int y = G.getAbsoluteY(b);
				int w = b.getWidth();
				int h = b.getHeight();
				fr.buttonMenuBar(evt,x+w/2,y+h); }
		}
	}
		

	// clean up when one of the frames is closed out.
	public void removeTab(TopFrameProtocol f)
	{	
		JButton l = tabFrames.remove(f);
		Container tabs = masterForm.getTabs();
		if(l!=null) 
			{ tabs.remove(l); 
			}
		adjustTabStyles();
	}
	public void adjustTabStyles()
	{
		Component front = getTopWindow();
		for(Enumeration<TopFrameProtocol>frames = tabFrames.keys(); frames.hasMoreElements(); )
		{	TopFrameProtocol f = frames.nextElement();
			JButton l = tabFrames.get(f);
			if(l!=null)
			{	//Style st = l.getStyle();
				//st.setOpacity(f==front?255:128);
			}
		}
		if(front instanceof TabFrame)
		{	TabFrame f = (TabFrame)front;
			for(JButton b :  playtableButtons )
			{	String com = b.getCommandString();
				boolean v = f.hasCommand(com.toString());
				b.setVisible(v);
			}
		}
		else {
			for(JButton b :  playtableButtons ) { b.setVisible(false); }
		}
		Component bar = MasterForm.getMasterForm().getJMenuBar();
		if(bar!=null) { bar.repaint(); }
	}

	public void actionPerformed(ActionEvent evt) 
	{
		buttonTopWindow(evt,evt.getActionCommand());
	}
	
	public void doNullLayout()
	{
		setLocalBounds(0,0,getWidth(),getHeight());
	}
	public void setLocalBounds(int l,int t,int w0, int h)
	{	Rectangle safe = MasterForm.getMasterForm().getSafeArea();
		int sx = (int)safe.getX();
		//int sy = safe.getY();
		//int sw = safe.getWidth();
		//int sh = safe.getHeight();
		int x = (int)(sx*0.66);
		int w = w0-sx;
		for(int nc = getComponentCount()-1 ; nc>=0; nc--)
		{
			Component c = getComponent(nc);
			int cw = c.getWidth();
			int ch = c.getHeight();
			if((c instanceof TopFrameProtocol)
					&& ((cw!=w)||(ch!=h)))
			{	
				c.setBounds(x,0,w,h);
			}
		}
	}
	
	public void remove(Component cc)
	{	Component top = getTopWindow();
		super.remove(cc);
		if(cc instanceof TopFrameProtocol) { removeTab((TopFrameProtocol)cc); }
		if(cc==top)
		{	Component newtop = getTopWindow();
			if(newtop!=null)
				{newtop.setVisible(true);
				 newtop.repaint();
				}
		}
	}

	}
	
