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
import java.awt.Font;
import java.awt.GridLayout;

import lib.FontManager;
import lib.G;
import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;
import lib.Plog;

@SuppressWarnings("serial")
public class JPopupMenu extends javax.swing.JPopupMenu implements NativeMenuInterface
{	public JPopupMenu() { super(); }
	JLabel label = null;
	public JPopupMenu(String msg)
	{ super(msg); 
	  if(msg!=null && !"".equals(msg))
	  {
		JLabel title = new JLabel(msg);
		title.setFont(getFont().deriveFont(Font.BOLD));
		add(title);
		addSeparator();
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		label = title;
	  }
	}
	public void setFont(Font f)
	{
		super.setFont(f);
		if(label!=null)
		{
			label.setFont(f.deriveFont(Font.BOLD));
		}
	}
	public JPopupMenu(String msg,Font f) { this(msg); setFont(f==null ? FontManager.menuFont() : f); }
	public int getItemCount() { return(getComponentCount()); }
	public Component add(Component s) { return super.add(s); }
	public NativeMenuItemInterface getMenuItem(int n) 
	{ Component c = getComponent(n);
	  return(c instanceof NativeMenuItemInterface ? (NativeMenuItemInterface)c : null);
	}

	public void hide(Component window)
	{
		if(window instanceof Container) { ((Container)window).remove(this); }
	}

	public void show(Object invoker, int x, int y) 
	{	// this is an attempt to paper over some glitches with JPopupMenu
		// which seems to be not entirely thread safe.  The crashes occur
		// when setSelectedPath is called as part of setVisible.  This
		// attempts to prevent the crash by doing it in advance [ddyer 1/2023 ]
		try {
		super.show(MasterForm.getMasterForm(),x,y);
		}
		catch (Exception e)
		{	// threading issues in swing menus cause random errors
			Plog.log.addLog("Error showing menu ",e);
			G.doDelay(100);
			super.show(MasterForm.getMasterForm(),x,y);			
		}
	}
	public void setNColumns(int n) {
		setLayout(new GridLayout(0,n));	
	}
	public boolean useSimpleMenu() { return false; }


}

