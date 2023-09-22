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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import bridge.Icon;
import bridge.JMenu;

public class SliderMenu extends JMenu implements MouseListener,Icon
{
	
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	public Icon getIcon() { return(this); }
	private Slider slider = null;
	private InternationalStrings s = null;
	public SliderMenu(Slider s) 
	{ 	super(); 
		slider = s;
		addMouseListener(this);
	}
	int preferredW = 100;
	int preferredH = 20;
	
	public void paint(Graphics g)
	{	int w = getWidth();
		int h = getHeight();
		if(slider!=null)
		{	G.SetRect(slider,0,0,w-1,h-1);// the -1 is empirically determined, unclear why it's right
			slider.draw(g,null,s);
		}
		else
		{	
			GC.fillRect(g,Color.black,0,0,w,h);
			GC.setColor(g,Color.red);
			GC.drawLine(g,0,0,w,h);
		}
	}
	public Dimension getPreferredSize() { return(new Dimension(preferredW,preferredH));}
	
	public void mouseClicked(MouseEvent e) { ;processMouse(e,true); processMouse(e,false);}
	public void mousePressed(MouseEvent e) {  }
	public void mouseReleased(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

	
	private void processMouse(MouseEvent e,boolean down)
	{
		if(slider!=null)
		{
		int x = e.getX();
		int y = e.getY();
		HitPoint hp = new HitPoint(x,y);
		hp.down = down;
		slider.draw(null,hp,s);
		repaint();
		setSelected(down);
		}
	}
	protected void processMouseEvent(MouseEvent e) 
	{	try { 
		int id = e.getID();
            switch(id)
            {
            case MouseEvent.MOUSE_ENTERED:	
            	// inhibit mouse entered to avoid auto-selection moving
            	// between items on the jmenubar
            	break;
            default: super.processMouseEvent(e);
            }
		}
		catch (ArrayIndexOutOfBoundsException err) 
		{ G.print("error in java menu "+err);
		}
        }

	public void paintIcon(Component c, Graphics g, int x, int y) {
		if(slider!=null)
		{
			G.SetRect(slider,x,y,preferredW,preferredH);
			slider.draw(g,null,s);
		}
	}
	public int getIconWidth() {
		return(preferredW);
	}
	public int getIconHeight() {
		return(preferredH);
	}
	

}
