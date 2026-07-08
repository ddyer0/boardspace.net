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
import java.awt.Font;
import lib.FontManager;
import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;

@SuppressWarnings("serial")
public class PopupMenu extends java.awt.PopupMenu implements NativeMenuInterface,NativeMenuItemInterface
{	public MenuItem label = null;
	public boolean isLabel() { return false;}
	public PopupMenu(String m)
	{ super(m==null?"":m); 
	  if(m!=null && !"".equals(m))
	  {
		  MenuItem title = new MenuItem(m);
		  title.setFont(getFont().deriveFont(Font.BOLD));
		  title.setEnabled(false);
		  title.setIsLabel(true);
		  add(title);
		  label = title;
		  addSeparator();

	  }
	}
	public void setFont(Font g)
	{
		super.setFont(g);
		if(label!=null) { label.setFont(g.deriveFont(Font.BOLD)); }
	}
	//public Component add(Component c) { return add(c); }
	public PopupMenu(String m,Font f) { this(m); setFont(f==null ? FontManager.menuFont() : f); }
	public NativeMenuItemInterface getMenuItem(int n) 
	{ Object m = getItem(n);
	  return(m instanceof NativeMenuItemInterface ? (NativeMenuItemInterface)m : null); 
	}
	public NativeMenuInterface getSubmenu() { return(this); }
	public Icon getNativeIcon() {	return null;	}
	public String getText() { return(getLabel()); }
	public int getNativeHeight() { return(JMenu.Height(this));	}
	public int getNativeWidth() {	return(JMenu.Width(this)); }
	public void show(Component window,int x,int y)
	{
		window.add(this);
		super.show(window,x,y);
	}
	public void hide(Component window)
	{
		window.remove(this);
	}
	public Font getFont() 
	{
		Font f = super.getFont();
		if(f==null) { f = FontManager.getGlobalDefaultFont(); }
		return(f);
	}
	private int ncols = 1;
	public boolean useSimpleMenu() { return ncols>1; }
	public void setNColumns(int n) {
		ncols = n;
	}

}
