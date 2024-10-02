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

import lib.CanvasRotater;
import lib.CanvasRotaterProtocol;
import lib.ExtendedHashtable;
import lib.G;
import lib.Graphics;
import lib.LFrameProtocol;
import lib.PinchEvent;
import lib.SizeProvider;

public class Canvas extends Component implements WindowListener,MouseListener,MouseWheelListener,MouseMotionListener,
		CanvasRotaterProtocol,SizeProvider
{	
	private CanvasRotater rotater = null;
	public LFrameProtocol myFrame = null;
	public Canvas(LFrameProtocol frame)
	{	
		super();
		myFrame = frame;
		rotater = frame.getCanvasRotater();
	}

	public void init(ExtendedHashtable h,LFrameProtocol f)
	{
		myFrame = f;
		rotater = f.getCanvasRotater();
	}
	public void shutDown()
	{
		 LFrameProtocol f = myFrame;
		 if(f!=null) { f.killFrame(); }
	}
	public boolean doSound() { return myFrame.doSound(); }
	
	public Canvas() { super(); }
	
	public void paint(Graphics g) { G.Error("Should be overridden");}
	
	public void paint(com.codename1.ui.Graphics g)
	{	
		paint(Graphics.create(g,this));
	}

	/* dummy methods to be overridden */
	public void windowOpened(WindowEvent e) { }
	public void windowClosing(WindowEvent e) {	}
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) {  }
	public void windowDeiconified(WindowEvent e) { }
	public void windowActivated(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }

	public void mouseClicked(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }

	public void mouseWheelMoved(MouseWheelEvent e) { }

	public void mouseDragged(MouseEvent e) { }
	public void mouseMoved(MouseEvent e) { }
	public void mousePinched(PinchEvent e) { }
	public int rotateCanvasX(int x,int y) { return rotater.rotateCanvasX(x,y,this); }
	public int rotateCanvasY(int x,int y) { return rotater.rotateCanvasY(x,y,this); }

	public int getCanvasRotation() { return rotater.getCanvasRotation(); }
	public void setCanvasRotation(int n) 
		{ rotater.setCanvasRotation(n); }
	public boolean rotateCanvas(Graphics g) { return rotater.rotateCanvas(g,this); }
	public void unrotateCanvas(Graphics g) {  rotater.unrotateCanvas(g,this); }
	public int unrotateCanvasX(int x,int y) { return rotater.unrotateCanvasX(x,y,this); }
	public int unrotateCanvasY(int x,int y) { return rotater.unrotateCanvasY(x,y,this); }
	public int getRotatedWidth() { return rotater.getRotatedWidth(this); }
	public int getRotatedHeight() { return rotater.getRotatedHeight(this); }
	public int getRotatedTop() { return rotater.getRotatedTop(this); }
	public int getRotatedLeft() { return rotater.getRotatedLeft(this); }
}
