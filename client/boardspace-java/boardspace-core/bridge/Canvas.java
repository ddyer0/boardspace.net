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
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import lib.CanvasRotater;
import lib.CanvasRotaterProtocol;
import lib.ExtendedHashtable;
import lib.G;
import lib.Graphics;
import lib.LFrameProtocol;
import lib.SizeProvider;
import lib.TextContainer;

@SuppressWarnings("serial")
public abstract class Canvas extends java.awt.Canvas 
	implements SizeProvider , CanvasRotaterProtocol, WindowListener, MouseListener,MouseMotionListener,MouseWheelListener
{	public Canvas() 
		{ super(); 
		}
	@SuppressWarnings("unused")
	public LFrameProtocol myFrame = null;
	public Canvas(LFrameProtocol frame)
	{	super();
		myFrame = frame;
		rotater = frame.getCanvasRotater();
	}

	public void shutDown()
	 {	
		 LFrameProtocol f = myFrame;
		 if(f!=null) { f.killFrame(); }
	 }

	public void init(ExtendedHashtable h,LFrameProtocol f)
	{	myFrame = f;
		rotater = f.getCanvasRotater();
		
	}
	public void paint(java.awt.Graphics g)
	{	//if(!isDoubleBuffered()) { setDoubleBuffered(true); }
		paint(Graphics.create(g));
	}
	public void update(java.awt.Graphics g)
	{	update(Graphics.create(g));
	}
	public void update(lib.Graphics g)
	{
		actualUpdate(g);
	}
	public void actualUpdate(lib.Graphics g)
	{
		super.update(g.getGraphics());
	}
	public void paint(lib.Graphics g)
	{ 
		actualPaint(g);
	}
	public void actualPaint(Graphics g)
	{	// in windows based on Container, this would paint the components
	}
	public void actualRepaint(int n)
	{	super.repaint(n);
	}
	public void actualRepaint() { super.repaint(); }
	public Component add(Component c) { throw G.Error("shouldn't be called"); }
	public Component getComponent() { return(this); }
	public void removeThis() 
	{	Container par = getParent();
		if(par!=null) { par.remove(this); }
	}
	public void setLayout(LayoutManager m) {};
	public Component getMediaComponent() { return(this); }
	public void requestFocus(TextContainer p) { requestFocus(); }
	
	// support for rotater buttons
	CanvasRotater rotater = new CanvasRotater();
	public CanvasRotater getCanvasRotater() { return rotater; }

	public int getCanvasRotation()
		{ return rotater.getCanvasRotation(); 
		}
	public void setCanvasRotation(int n) 
		{ rotater.setCanvasRotation(n); }
	public boolean rotateCanvas(Graphics g) { return rotater.rotateCanvas(g,this); }
	public void unrotateCanvas(Graphics g) {  rotater.unrotateCanvas(g,this); }
	public int rotateCanvasX(int x,int y) { return rotater.rotateCanvasX(x,y,this); }
	public int rotateCanvasY(int x,int y) { return rotater.rotateCanvasY(x,y,this); }
	public int unrotateCanvasX(int x,int y) { return rotater.unrotateCanvasX(x,y,this); }
	public int unrotateCanvasY(int x,int y) { return rotater.unrotateCanvasY(x,y,this); }
	public int getRotatedWidth() { return rotater.getRotatedWidth(this); }
	public int getRotatedHeight() { return rotater.getRotatedHeight(this); }
	public int getRotatedLeft() { return rotater.getRotatedLeft(this); }
	public int getRotatedTop() { return rotater.getRotatedTop(this); }
	
	/* for window listener */
	public void windowOpened(WindowEvent e) { }
	public void windowClosing(WindowEvent e) {	}
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) {	}
	public void windowActivated(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) {	}
	
	public void mouseClicked(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mouseWheelMoved(MouseWheelEvent e) {}
}
