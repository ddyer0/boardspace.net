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

import com.codename1.ui.Font;

import lib.NativeMenuItemInterface;

public class JMenu extends Menu
{
	public JMenu() { }
	public JMenu(String msg) { super(msg);  }
	public JMenu(String msg,Font f) { this(msg); setFont(f==null ? lib.FontManager.menuFont() : f); }
	public void add(Menu jsubmenu) { super.add(jsubmenu); }
	public boolean isVisible() { return(false); }
	public void setSelected(boolean v) {}
	public boolean isSelected() { return(false); }
	public void addMouseListener(MouseListener l) {}
	public void addMouseMotionListener(MouseMotionListener l) {}
	public void repaint() { }
	public static int Height(NativeMenuItemInterface mi)
	{	Icon ic = mi.getNativeIcon();
		if(ic!=null) 
		{
		return(ic.getIconHeight());	
		}
		else
		{
		String str = mi.getText();
		if(str==null) { str="xxxx"; }
		Font f = mi.getFont();
		FontMetrics fm = lib.FontManager.getFontMetrics(f);
		return(fm.getHeight());
		}
	}
	public static int Width(NativeMenuItemInterface mi)
	{	Icon ic = mi.getNativeIcon();
		if(ic!=null) 
		{
		return(ic.getIconWidth());	
		}
		else
		{
		String str = mi.getText();
		if(str==null) { str="xxxx"; }
		Font f = mi.getFont();
		FontMetrics fm = lib.FontManager.getFontMetrics(f);
		return(fm.stringWidth(str));
		}
	}
	int ncols = 1;
	public void setNColumns(int n) {
		ncols = n;
	}
	public boolean useSimpleMenu() {
		return ncols>1;
	}

}
