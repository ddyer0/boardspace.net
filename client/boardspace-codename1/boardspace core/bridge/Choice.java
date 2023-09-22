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

import lib.AwtComponent;
import lib.G;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Font;

public class Choice<TYPE> extends ComboBox<TYPE> implements AwtComponent , ActionProvider
{
	MouseAdapter mouse = new MouseAdapter(this);
	public void addItemListener(ItemListener m) {mouse.addItemListener(m); }
	public void addActionListener(ActionListener m) { mouse.addActionListener(m); }
	public Font getFont() { return(G.getFont(getStyle())); }	
	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(this))
		{ 
		  super.repaint();
		} 
	}
	public void select(TYPE string) {
		int nitems = size();
		for(int i=0;i<nitems;i++)
		{
			if(string.equals(getModel().getItemAt(i))) { setSelectedIndex(i); break; }
		}
		
	}
	public void add(TYPE string) {
		addItem(string);		
	}

	public void setForeground(Color c) { getStyle().setFgColor(c.getRGB()); }
	public void setBackground(Color c) { getStyle().setBgColor(c.getRGB()); }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	public void select(int index) { setSelectedIndex(index);	}
	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}
}
