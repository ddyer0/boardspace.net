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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.security.AccessControlException;
import java.util.prefs.Preferences;

import javax.swing.JMenuBar;

import lib.CanvasRotater;
import lib.DeferredEventManager;
import lib.Font;
import lib.G;
import lib.Graphics;
import lib.Http;
import lib.Image;
import lib.ImageConsumer;
import lib.SizeProvider;
import lib.TopFrameProtocol;
import lib.ImageLoader;

@SuppressWarnings("serial")
class JBar extends JMenuBar
{	
	public void paint(java.awt.Graphics gc)
	{
		super.paint(gc);
	}
	public void setFrameBounds(int x,int y,int w,int h)
	{
		super.setBounds(x,y,w,h);
	}
	public Dimension getPreferredSize()
	{
		Dimension s = super.getPreferredSize();
		int h = (int)(2.5*Font.getFontSize(Font.getGlobalDefaultFont()));
		return new Dimension((int)s.getWidth(),(int)Math.max(h,s.getHeight()));
		
	}
}

@SuppressWarnings("serial")
public class JFrame extends javax.swing.JFrame 
	implements TopFrameProtocol,SizeProvider,Config,WindowListener,ImageConsumer
{	ImageLoader loader = new ImageLoader(this);
	static final String FIXEDSIZE = "fixedsize";
	private boolean useMenuBar = true;		// if true, use the local menu bar
	private boolean closeable = true;
	public void setCloseable(boolean v) { closeable = v; }
	public boolean getCloseable() { return(closeable); }
	public JMenuBar jMenuBar = null;
	JPopupMenu popupMenuBar = null;
	public DeferredEventManager canSavePanZoom = null;
	public Rectangle getFrameBounds() { return super.getBounds();}
	public void setFrameBounds(int l,int t, int w,int h) { super.setBounds(l,t,w,h); }
	public void setCanSavePanZoom(DeferredEventManager v) {
		canSavePanZoom = v;		
	}

public void validate()
{
	super.validate();
}
public void validateTree()
{
	super.validateTree();
}
	public boolean hasSavePanZoom = false;
	public void setHasSavePanZoom(boolean v) {
		hasSavePanZoom = v;		
	}
	
	public JFrame(String name) 
	{ 
	  super(name); 
	  setContentPane(new FullscreenPanel());
	  addWindowListener(this);
	}
	public JFrame() 
	{ super();
	  setContentPane(new FullscreenPanel());
	  addWindowListener(this);
	}
	
	private CanvasRotater rotater = new CanvasRotater();
	public boolean enableRotater = true;
	public void setEnableRotater(boolean v) {
		enableRotater = v;		
	}

	public Image getIconAsImage() 
	{	java.awt.Image im = super.getIconImage();
		return(im==null ? null : Image.createImage(im,"frame icon")); 
	}
	
	public void changeImageIcon(Image im) {
		setIconImage(im==null ? null : im.getSystemImage());
	}

	public void paint(Graphics g) 
	{ 	super.paint(g.getGraphics()); 
	}
	public void setOpaque(boolean v)
	{	// don't do anything
	}
	
	/*
	public void update(Graphics g)
	{ super.update(g); 
	}
	*/
	public CanvasRotater getCanvasRotater() {
		return rotater;
	}
	public int getRotatedWidth() { return getWidth(); }
	public int getRotatedHeight() { return getHeight(); }
	public int getRotatedLeft() { return getX(); }
	public int getRotatedTop() { return getY(); }
	public void addC(Component p) {
			add(p);	
	}
	public void addC(String where, Component p) {
		add(where,p);
	}

	public void setLayoutManager(LayoutManager m) {
		setLayout(m);
	}
	public void packAndCenter()
	{
		pack();
		setLocationRelativeTo(null);
	}
	
	
	public boolean hasCommand(String cmd)
	{	if(("rotate".equals(cmd) || "twist3".equals(cmd) || "twist".equals(cmd))) { return(enableRotater); }
		if("close".equals(cmd)) { return(true); }
		if("actionmenu".equals(cmd)) 
			{ return(popupMenuBar!=null); 
			}
		if("savepanzoom".equals(cmd)) { return(canSavePanZoom!=null); }
		if("restorepanzoom".equals(cmd)) { return(hasSavePanZoom); }
		return(false);
	}
	
	// this is used in the codename1 branch
	public void buttonMenuBar(ActionEvent evt,int x,int y)
	{	String cmd = evt.getActionCommand().toString();
		if("twist3".equals(cmd))
		{
			if(rotater!=null) { rotater.setCanvasRotation(rotater.getCanvasRotation()+1);revalidate();  }			
		}
		else if("twist".equals(cmd))
		{
			if(rotater!=null) { rotater.setCanvasRotation(rotater.getCanvasRotation()-1); revalidate(); }
		}
		else if("rotate".equals(cmd))
		{
			if(rotater!=null) { rotater.setCanvasRotation(rotater.getCanvasRotation()+2); revalidate(); }
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

	public void setJMenuBar(JMenuBar m)
	{ jMenuBar = m; super.setJMenuBar(m); 
	}
	public void addToMenuBar(JMenu m)
	{
		addToMenuBar(m,null);
	}
	

	public void addToMenuBar(JMenu m,DeferredEventManager l)
	{	
		if(useMenuBar)
		{	if(jMenuBar==null) {  setJMenuBar(new JBar()); }
			m.setVisible(true);
			if(jMenuBar.getComponentIndex(m)<0)
				{
				jMenuBar.add(m);
				}
		}
		else {
			boolean isNew = popupMenuBar==null;
			if(isNew) 
				{ popupMenuBar=new JPopupMenu();
				 
				}
			if(popupMenuBar.getComponentIndex(m)<0)
				{
				popupMenuBar.add(m);
				}
			if(isNew) {  MasterForm.getMasterPanel().adjustTabStyles(); }
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
	public String tabName() {
		return getName();
	}
	public Container getParentContainer() {
		return getParent();
	}
	@Override
	public void addC(ProxyWindow w) {
		G.Error("Not expected");
		
	}
	private int lastKnownWidth = -1;
	private int lastKnownHeight = -1;
	public void screenResized()
	{	if(G.isCheerpj())
		{int w = lastKnownWidth;
		int h = lastKnownHeight;
		lastKnownWidth = G.getScreenWidth();
		lastKnownHeight = G.getScreenHeight();
		if(lastKnownWidth!=w || lastKnownHeight!=h)
		  {
		   setInitialBounds(getX(),getY(),getWidth(),getHeight());
		  }
		}
	}
	public void setInitialBounds(int inx,int iny,int inw,int inh)
	{
		int fx = inx;
		int fy = iny;
		int fw = inw;
		int fh = inh;
        Preferences prefs = Preferences.userRoot();
        String suffix ="-"+getTitle();
        String bounds = G.getBoolean(FIXEDSIZE,false)
        					? null 
        					: prefs.get(FRAMEBOUNDS+suffix,null);
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
        lastKnownWidth = G.getScreenWidth();
        lastKnownHeight = G.getScreenHeight();
        fw = Math.max(lastKnownWidth/5,Math.min(fw,lastKnownWidth));
        fh = Math.max(lastKnownHeight/5,Math.min(fh,lastKnownHeight));
        fx = Math.max(0,Math.min(lastKnownWidth-fw,fx));
        fy = Math.max(0,Math.min(lastKnownHeight-fh,fy));
		setFrameBounds(fx,fy,fw,fh);   			
	} 	
	
	public void windowClosing(WindowEvent e) {
        String suffix = "-"+ getTitle();
       	Preferences prefs = Preferences.userRoot();
    	prefs.put(FRAMEBOUNDS+suffix,G.concat(getX(),",",getY(),",",getWidth(),",",getHeight()));
	}
	public void windowOpened(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowActivated(WindowEvent e) { }
 	public void windowDeactivated(WindowEvent e) {	}
 	
 	public void setSize(int w,int h)
 	{	super.setSize(w,h);
 	}

 	public void moveToFront() {
		MasterForm.moveToFront(this);
	}
	public void setLowMemory(String string) {
	}
	public Component getMediaComponent() {
		return this;
	}

}

