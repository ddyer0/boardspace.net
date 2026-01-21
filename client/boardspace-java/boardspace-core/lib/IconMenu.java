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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import bridge.JMenu;

public class IconMenu extends JMenu {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int prefWidth = 30;
	int prefHeight = 20;
	public String text = null;
	public void setText(String n) { text = n; repaint(); }
	public IconMenu(Image ic) { super(); icon = ic; }
	private Image icon = null;
	public Image getImage() 
	{ return(icon); 
	}
	private boolean state = false;
	protected void fireItemStateChanged(ItemEvent event) 
	{
		if(isSelected())
			{ super.fireItemStateChanged(event); 
			}
	}

	
	public void setSelected(boolean t) 
	{	super.setSelected(t);
		if(t) 
			{ super.setSelected(false); }
	}
	   
	public void changeIcon(Image ic,boolean st)
	{
		if(icon!=ic || st!=state)
		{
			icon = ic;
			state = st;
			repaint();
		}
	}
	public boolean getState() { return(state); }

	public Dimension getPreferredSize() { return(new Dimension(prefWidth,prefHeight)); }

	public void paint(Graphics g)
	{
		int w = getWidth();
		int h = getHeight();
		if(icon!=null)
		{	// for some reason "this" doesn't work reliably as an image observer
		icon.centerImage(g,0,0,w,h);
			if(text!=null)
			{
				GC.Text(g, true, 0, 0, w, h,Color.black,null,text);
			}
		}
		else
		{
		GC.fillRect(g,Color.black,0,0,w,h);
		GC.setColor(g,Color.red);
		GC.drawLine(g,0,0,w,h);
		}

	}
	public void paintIcon(Component c, Graphics g, int x, int y) {
		icon.drawImage(g,x,y,prefWidth,prefHeight);		
		}

	public int getIconWidth() {
		return(prefWidth);
	}

	public int getIconHeight() {
		return(prefHeight);
	}

}
