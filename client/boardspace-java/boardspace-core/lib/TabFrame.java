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
package lib;


import java.awt.Container;
import java.awt.event.ActionEvent;
import java.security.AccessControlException;
import javax.swing.JMenuBar;

import bridge.Config;
import bridge.JMenu;
import bridge.JPanel;
import bridge.JPopupMenu;
import bridge.MasterForm;
import bridge.MasterPanel;
import bridge.ProxyWindow;

@SuppressWarnings("serial")
public class TabFrame extends JPanel
	implements TopFrameProtocol,SizeProvider,Config
{
	ImageLoader loader = new ImageLoader(this);
	public TabFrame() 
		{ super(); 
		  StockArt.preloadImages(loader,IMAGEPATH);
		}
	public Container getParentContainer() { return (Container)getParent(); }
	public TabFrame(String tab) 
		{ super(tab);
		  StockArt.preloadImages(loader,IMAGEPATH);
		}
	// support for rotater buttons
	private CanvasRotater rotater = new CanvasRotater();
	private boolean enableRotater = true;
	public CanvasRotater getCanvasRotater() { return rotater; }
	public void setEnableRotater(boolean v) { enableRotater = v;}
	public DeferredEventManager canSavePanZoom = null;
	private boolean useMenuBar = false;		// if true, use the local menu bar
	public JMenuBar jMenuBar = null;

	JPopupMenu popupMenuBar = null;


	public void setJMenuBar(JMenuBar m) { jMenuBar = m;  }
	public void addToMenuBar(JMenu m)
	{	
		addToMenuBar(m,null);
	}
	public void setVisible(boolean vis)
	{	
		super.setVisible(vis);
		addTabToPanel();

	}
	// this was troublesome - it was in "JPanel" and caused the source tabs
	// to disappear from the fileselector, because it was added to the
	// master panel.  I Moved it up, to here, and added
	// a catch to "addC" to throw an error if it tries to move a window that
	// already has a parent.
	public void addTabToPanel()
	{
		MasterForm mf =MasterForm.getMasterForm();
		if(!mf.isVisible()) 
			{ 
			mf.setVisible(true); 
			}
		// defer adding this panel to the master until it's supposed to be seen
		// doing this in the constructor caused mysterious "blank" windows that
		// could be fixed by window-level operations such as resizing or minimizing
		MasterPanel mp = MasterForm.getMasterPanel();
		if(mp.getComponentZOrder(this)<0)
			{ mp.addC(this);
			}
	MasterForm.getMasterPanel().adjustTabStyles();
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
	
 	public boolean hasSavePanZoom = false;

	public void setCanSavePanZoom(DeferredEventManager m) 
	{ 	canSavePanZoom = m;
		MasterForm.getMasterPanel().adjustTabStyles(); 
	}
	public void setHasSavePanZoom(boolean v) 
	{ hasSavePanZoom = v; 
	  MasterForm.getMasterPanel().adjustTabStyles(); 
	}
	private boolean closeable = true;
	public void setCloseable(boolean v) { closeable = v; }
	public boolean getCloseable() { return(closeable); }

	// this is used in the codename1 branch
	public void buttonMenuBar(ActionEvent evt,int x,int y)
	{	String cmd = evt.getActionCommand();
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
				popupMenuBar.show(evt.getSource(),
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
	private Image iconImage = null;
	
	public void changeImageIcon(Image im) 
	{ MasterPanel p =  MasterForm.getMasterPanel();
	  iconImage = p.getTabImage(im);
	  MasterForm.getMasterPanel().setTabName(this,getTitle(),iconImage);
	}
	
	public Image getIconAsImage() 
	{ 

	  return(iconImage); 
	}
	public void packAndCenter()
	{
		// nothing needed for this implementation
	}
	
	public String tabName() {
		return getName();
	}	
	public void addC(ProxyWindow w) {
		addC(w.getComponent());
	}

	public void setInitialBounds(int inx,int iny,int inw,int inh)
	{
		Container parent = getParentContainer();
		// parent can legitimately be null if the window is closing
		if(parent!=null)
		{
			int w = parent.getWidth();
			int h = parent.getHeight();
			setFrameBounds(0,0,Math.max(300,w),Math.max(300,h));
		}


	}
	public void moveToFront() {
		MasterForm.moveToFront(this);
		
	}
	public void setTitle(String n) {
		MasterForm.getMasterPanel().setTabName(this,n,getIconAsImage());
	}
}
