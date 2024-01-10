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
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.security.AccessControlException;

import javax.swing.JMenuBar;

import lib.CanvasRotater;
import lib.DeferredEventManager;
import lib.G;
import lib.Graphics;
import lib.Http;
import lib.Image;
import lib.SizeProvider;
import lib.TopFrameProtocol;

@SuppressWarnings("serial")
public class JFrame extends javax.swing.JFrame implements TopFrameProtocol,SizeProvider
{	
	private boolean useMenuBar = true;		// if true, use the local menu bar
	private boolean closeable = true;
	public void setCloseable(boolean v) { closeable = v; }
	public boolean getCloseable() { return(closeable); }
	public JMenuBar jMenuBar = null;
	JPopupMenu popupMenuBar = null;
	public DeferredEventManager canSavePanZoom = null;
	public void setCanSavePanZoom(DeferredEventManager v) {
		canSavePanZoom = v;		
	}

	public boolean hasSavePanZoom = false;
	public void setHasSavePanZoom(boolean v) {
		hasSavePanZoom = v;		
	}
	
	public JFrame(String name) 
	{ 
	  super(name); 
	}
	public JFrame() 
	{ super();
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
		setIconImage(im==null ? null : im.getImage());
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

}
