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

import com.codename1.ui.layouts.FlowLayout;

public class JMenuBar extends Container 
{	public JMenuBar() { setLayout(new FlowLayout()); setOpaque(true); setBackground(new Color(0.85f,0.85f,1.0f)); }
	public void add(Menu m) { 	add(m.getMenu()); }
	public void remove(Menu actions) { remove(actions.getMenu()); 	}
	
}
