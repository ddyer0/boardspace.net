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
import java.awt.Container;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.security.AccessControlException;

import lib.G;
import lib.Graphics;
import lib.MenuInterface;
import lib.MenuParentInterface;
import lib.NullLayout;
// dummy class for standard java, does not change the frame size
//
//dummy class for standard java, does not change the frame size
//note that if this uses JPanel instead of Panel, the inescapable 
//double buffering used by swing will kick in.  This can cause
//flashing refreshes due to interactions with our repaint manager,
//if it ever has to skip a frame refresh due to process interlocks.
//
import lib.NullLayoutProtocol;
public class FullscreenPanel extends Panel implements MenuParentInterface,NullLayoutProtocol
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Rectangle getFrameBounds() { return getBounds(); }
	public void addC(Component c) { add(c); }
	public FullscreenPanel()
	{
		super();
		setLayout(new NullLayout(this)); 
		//setOpaque(false);
	}
	public void doNullLayout()
	{
		setLocalBounds(0,0,getWidth(),getHeight());
	}
	
	public void setFrameBounds(int l,int t,int w,int h) { setBounds(l,t,w,h); }
	
	public void setLocalBounds(int l,int t,int w, int h)
	{	
		for(int nc = getComponentCount()-1 ; nc>=0; nc--)
		{
			Component c = getComponent(nc);
			int cw = c.getWidth();
			int ch = c.getHeight();
			if((cw!=w)||(ch!=h))
			{	// set the size of components to be full size
				c.setBounds(0, 0, w, h);
				// if the component isn't a frame, invoke nulllayout if appropriate
				if(!(c instanceof Container)
					&& c instanceof NullLayoutProtocol)
				{
					((NullLayoutProtocol)c).doNullLayout();
				}
			}
		}
	}
	public void show(MenuInterface menu, int x, int y) throws AccessControlException 
	{
		showNative(menu, x, y);
	}
	public void showNative(MenuInterface menu, int x, int y) throws AccessControlException 
	{
		G.show(this, menu, x, y);
	}
	
	public void actualPaint(Graphics g)
	{
		super.paint(g.getGraphics());
	}
	public void paint(java.awt.Graphics g)
	{	paint(Graphics.create(g,this));
	}
	public void paint(Graphics g)
	{
		actualPaint(g);
	}
	

	public void windowOpened(WindowEvent e) {
		
	}


	public void windowClosing(WindowEvent e) {
		
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
		
	}

	public void windowDeiconified(WindowEvent e) {
	
	}

	public void windowActivated(WindowEvent e) {
	
	}

	public void windowDeactivated(WindowEvent e) {
		
	}
	
	public void setFocused(Component p)
	{
		// this is a dummy method, the codename1 version does something.
		// this fixes a problem where the fileselector came up "dead" after
		// a selection, which appears to be a focus problem.
	}
	/*
	public void update(java.awt.Graphics g)
	{	System.out.println("Update "+this);
	
	}
	public void paintComponent(java.awt.Graphics g)
	{
		System.out.println("PaintComponent "+this);
	}
	*/
}
