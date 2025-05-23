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

import com.codename1.ui.Font;
import com.codename1.ui.TextField;

public class JPasswordField extends TextField implements AwtComponent
{	MouseAdapter mouse = new MouseAdapter(this);
	public void addActionListener(ActionListener who) { mouse.addActionListener(who); }
	public Font getFont() { return(SystemFont.getFont(getStyle())); }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(this))
		{ 
		  super.repaint();
		} 
	}
	public JPasswordField(int i) 
		{ super(i);
	      setHint("Password");
	      setConstraint(TextField.PASSWORD);
		}

	public void setActionCommand(String oK) 
	{
	}


	public String getPassword() {
		return(getText());
	}
	public FontMetrics getFontMetrics(Font f) {
		return lib.Font.getFontMetrics(f);
	}
}
