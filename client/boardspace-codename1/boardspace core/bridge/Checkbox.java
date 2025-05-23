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
import lib.AwtComponent;

public class Checkbox extends com.codename1.ui.CheckBox implements ActionProvider,AwtComponent
{	private final MouseAdapter mouse = new MouseAdapter(this);
	public void addItemListener(ItemListener m) {mouse.addItemListener(m); }
	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(this))
		{ 
		  super.repaint();
		} 
	}
	public Font getFont() { return(SystemFont.getFont(getStyle())); }
	public Checkbox(String string, boolean b) { super(string); setSelected(b); }
	public Checkbox(boolean b) { super();  setSelected(b); }
	public void setBackground(Color color) { getStyle().setBgColor(color.getRGB()); }
	public void setForeground(Color color) { getStyle().setFgColor(color.getRGB()); }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	public void setState(boolean b) { setSelected(b); }
	public FontMetrics getFontMetrics(Font f) {
		return lib.Font.getFontMetrics(f);
	}
}
