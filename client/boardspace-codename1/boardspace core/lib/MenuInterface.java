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

import com.codename1.ui.Font;

import bridge.ActionListener;

public interface MenuInterface {
	public NativeMenuItemInterface add(String item,ActionListener listener);
	public NativeMenuItemInterface add(Text item,DrawingObject parent,ActionListener listener);
	public NativeMenuInterface getNativeMenu();	
	public void add(MenuInterface item);
	public boolean isVisible();
	public void show(MenuParentInterface parent, int x, int y);
	public void setVisible(boolean b);
	public MenuInterface newSubMenu(String msg);
	public boolean useSimpleMenu();
	public int getItemCount();
	public void setFont(Font f);
	public Font getFont();
	public void setNColumns(int n);
	public int getNColumns();
	
}
