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

import com.codename1.ui.Command;
import com.codename1.ui.Font;

import lib.FontManager;
import lib.Image;
import com.codename1.ui.geom.Point;

public class Button extends com.codename1.ui.Button implements ActionProvider
{	private final MouseAdapter mouse = new MouseAdapter(this);
	public void addActionListener(ActionListener m) { mouse.addActionListener(m); }
	Image theImage = null;
	public Button(Image image) { super(image.getSystemImage()); theImage = image; }
	public Image getImage() { return theImage; }
	public Button(String label)
	{ super(label);
	}
	public void setBackground(Color color) { getStyle().setBgColor(color.getRGB()); }
	public void setForeground(Color color) { getStyle().setFgColor(color.getRGB()); }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	static {
		// this fixes the mysterious button ALL CAPS behavior that appeared 
		// on android on 9/2017
		setCapsTextDefault(false);
	}
	public Font getFont() { return(FontManager.getFont(getStyle())); };
	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(this))
		{ 
		  super.repaint();
		} 
	}
	public Point getLocation() { return(new Point(getX(),getY())); }
	public void setFont(Font f) { getStyle().setFont(f); }
	public String getLabel() { return(getText()); }
	public void setLabel(String t) { setText(t);  }
	
	public void setActionCommand(String oK) {
		setCommand(new Command(oK));
	}
	public String getActionCommand() 
		{ Command com = getCommand();
		  return((com==null) ? null : com.toString());
		}

	public void setBounds(int l,int t,int w,int h) 
	{ setX(l);
	  setY(t);
	  setWidth(w);
	  setHeight(h);
	}

	public FontMetrics getFontMetrics(Font f) {
		return FontManager.getFontMetrics(f);
	}
	public void pointerPressed(int x,int y)
	{	if(contains(x,y)) { x = getX()+1; y=getY()+1; }
		super.pointerPressed(x,y);
	}
	public void pointerReleased(int x,int y)
	{	if(contains(x,y)) { x = getX()+1; y=getY()+1; }
		super.pointerReleased(x,y);
	}
}	
