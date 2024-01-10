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

import bridge.AccessControlException;
import bridge.ActionEvent;
import bridge.Container;
import bridge.Frame;
import bridge.JMenu;
import bridge.JMenuBar;
import bridge.JPopupMenu;
import bridge.MasterForm;
import bridge.MasterPanel;
import bridge.ProxyWindow;

public class TabFrame extends Frame implements TopFrameProtocol,SizeProvider {
	public TabFrame() 
		{ super(); 
		}
	public Container getParentContainer() { return (Container)getParent(); }
	public TabFrame(String tab) 
		{ super(tab); 
		}
	// support for rotater buttons
	private CanvasRotater rotater = new CanvasRotater();
	private boolean enableRotater = true;
	public CanvasRotater getCanvasRotater() { return rotater; }
	public void setEnableRotater(boolean v) { enableRotater = v;}
	public DeferredEventManager canSavePanZoom = null;
	private boolean useMenuBar = !G.isCodename1();		// if true, use the local menu bar
	public JMenuBar jMenuBar = null;

	JPopupMenu popupMenuBar = null;


	public void setJMenuBar(JMenuBar m) { jMenuBar = m;  }
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

}
