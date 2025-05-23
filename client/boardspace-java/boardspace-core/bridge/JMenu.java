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
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;

import lib.G;
import lib.Graphics;
import lib.NativeMenuInterface;
import lib.NativeMenuItemInterface;
import lib.Plog;
import lib.SizeProvider;

@SuppressWarnings("serial")
public class JMenu extends javax.swing.JMenu implements NativeMenuInterface,NativeMenuItemInterface,SizeProvider
{	public JMenu(String m) { super(m); }
	public JMenu() { super(); }
	public JMenu(String m,Font f)
	{
		this(m);
		setFont(f==null ? lib.Font.menuFont() : f);
	}
	public void paintIcon(Component c, Graphics g, int x, int y) { G.Error("should be overridden");}
	public void paintIcon(Component c, java.awt.Graphics g, int x, int y)
	{
		paintIcon(c,Graphics.create(g,c),x,y);
	}
	public Icon getNativeIcon() { return(null); }
	public NativeMenuItemInterface getMenuItem(int n) 
	{
		return((NativeMenuItemInterface)getItem(n));
	}
	public void show(Component window, int x, int y) {
		G.Error("Can't show directly");
	}
	public void hide(Component window)
	{
		G.Error("Can't hide directly");
	}
	public NativeMenuInterface getSubmenu() { return(this); }
	public int getNativeWidth() { return(JMenu.Width(this)); }
	public int getNativeHeight() { return(JMenu.Height(this)); }
	public void paint(java.awt.Graphics g) { paint(Graphics.create(g,this)); }
	public void paint(Graphics g) { super.paint(g.getGraphics()); }
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
		FontMetrics fm = lib.Font.getFontMetrics(f);
		return(fm.getHeight()*5/4);
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
		FontMetrics fm = lib.Font.getFontMetrics(f);
		return(fm.stringWidth(str));
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
            	if(getItemCount()==0) { break; }
            case MouseEvent.MOUSE_PRESSED:
            	//G.infoBox("button",G.getStackTrace());
            default: 
            	super.processMouseEvent(e);
            }
		}
		catch (ArrayIndexOutOfBoundsException err) 
			{ Plog.log.addLog("error in java menu ",err);
			}
        }
	
}
