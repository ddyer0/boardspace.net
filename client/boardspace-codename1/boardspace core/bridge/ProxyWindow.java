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

import java.util.EventListener;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;

import lib.AwtComponent;
import lib.CanvasRotater;
import lib.CanvasRotaterProtocol;
import lib.ExtendedHashtable;
import lib.G;
import lib.LFrameProtocol;
import lib.MenuInterface;
import lib.NullLayoutProtocol;
import lib.SizeProvider;

// our component is a codename1 container, so it can have popup menus as children
public abstract class ProxyWindow implements SizeProvider,EventListener,AwtComponent,NullLayoutProtocol,CanvasRotaterProtocol, WindowListener
{	public Graphics getGraphics() { throw G.Error("Not implented, not implementedable"); }
	protected ComponentProxy theComponent = new ComponentProxy(this);
	public ComponentProxy getComponent() { return(theComponent); }
	public Component getMediaComponent() { return theComponent; }
	public Font getFont() { return(theComponent.getFont()); }
	public void requestFocus() {  theComponent.requestFocus(); }
	public boolean hasFocus() { return theComponent.hasFocus(); }
	public void setFocusable(boolean v) { theComponent.setFocusable(v); }
	public Dimension getPreferredSize()
	{
		return getComponent().getPreferredSize();
	}
	public void addMouseMotionListener(MouseMotionListener who) 
		{ theComponent.addMouseMotionListener(who); }
	public void addMouseListener(MouseListener who) 
		{ theComponent.addMouseListener(who); }
	public void addMouseWheelListener(MouseWheelListener who)
		{
		  theComponent.addMouseWheelListener(who);
		}
	public void removeMouseListener(MouseListener who)
	{
			theComponent.removeMouseListener(who);
	}
	public void addKeyListener(KeyListener who)
	{
		theComponent.addKeyListener(who);
	}
	public void removeKeyListener(KeyListener who)
	{
		theComponent.removeKeyListener(who);
	}
	public void addFocusListener(FocusListener who) 
	{ 	theComponent.addFocusListener(who); }
	public void removeFocusListener(FocusListener who)
	{
		theComponent.removeFocusListener(who);
	}


	public void removeMouseMotionListener(MouseMotionListener who)
	{
			theComponent.removeMouseMotionListener(who);
	}
	public void pointerHover(int[] x,int[] y) 
		{ theComponent.pointerHover(x,y); 
		}
	public boolean pinch(double f,int x,int y) {  return(false);};
	public FontMetrics getFontMetrics(Font f) { return(G.getFontMetrics(f)); }

	// constructor
	public ProxyWindow() {  }
	
	public void init(ExtendedHashtable info,LFrameProtocol frame)
	{
		rotater = frame.getCanvasRotater();
	}
	// methods to be overridden:
	public void paint(lib.Graphics g) 
	{ 
	  theComponent.actualPaint(g.getGraphics());
	}

	public void actualPaint(lib.Graphics g)
	{
		theComponent.actualPaint(g.getGraphics());
	}
	public void actualRepaint(int n)
	{
		theComponent.repaint(n);
	}
	public void actualRepaint()
	{
		theComponent.repaint();
	}
	// trampoline methods
	public void setVisible(boolean to) { theComponent.setVisible(to); }
	public void setEnabled(boolean b) {  theComponent.setEnabled(b); } 
	
	public Style getStyle() { return theComponent.getStyle(); }
	public void setSize(int size_x2, int size_y2) 
	{ 	theComponent.setWidth(size_x2);
		theComponent.setHeight(size_y2); 
	}


	public void setLocation(Point componentLocation) 
	{ 	theComponent.setX(componentLocation.getX());
		theComponent.setY(componentLocation.getY());
	}
	public void setLocation(int x,int y)
	{	theComponent.setX(x);
		theComponent.setY(y);
	}
	public void invalidate() { theComponent.invalidate(); }
	
	/*
	 * scroll and zoom are implemented locally, unknown to and independent
	 * of the codename1 window system.
	 */
	public int getX() { return(theComponent.getX()); }
	public int getY() { return(theComponent.getY()); }
	public int getWidth() { return(theComponent.getWidth()); }
	public int getHeight() { return(theComponent.getHeight()); }
	public Rectangle getBounds() { return new Rectangle(getX(),getY(),getWidth(),getHeight()); }
	public void setBounds(int l, int t, int w, int h) 
	{	theComponent.setX(l);
		theComponent.setY(t);
		theComponent.setWidth(w);
		theComponent.setHeight(h);
	}
	
	
	public lib.Image createImage(int w,int h) { return(lib.Image.createImage(w,h)); }
	public void setBackground(Color c) { getStyle().setBgColor(c.getRGB()); }
	public void setForeground(Color c) { getStyle().setFgColor(c.getRGB());  }
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }
	
	public void setFont(Font f) { getStyle().setFont(f); }
	public Dimension getSize() { return(getBounds().getSize()); }
	public void setBounds(Rectangle r)
	{
		setBounds(G.Left(r),G.Top(r),G.Width(r),G.Height(r));
	}
	
	public String isValid() { return("valid=?"); }
	public void validate() { }
	public void update() { 	}
	
	public void repaint() 
	{ 	if(MasterForm.canRepaintLocally(theComponent))
		{ 
		  theComponent.repaint();
		} 
	}
	public void repaint(int tm) 
	{ theComponent.repaint(tm); 
	}
	
	public void remove(PopupMenu tmenu) 
	{ 	// this just needs to capture and ignore the remove event.	
	}
	public ProxyWindow add(PopupMenu tmenu) 
	{ 	// this just needs to capture and ignore the add event.
		return(this);
	}
	

	public void requestFocus(KeyListener l)
	{	//theComponent.requestFocus();
		MasterForm.getMasterForm().setFocused(theComponent);
	}
	
	public void show(MenuInterface menu, int x, int y) throws AccessControlException {
		G.show(getComponent(),menu,x,y);
	}
	public String getName() { return theComponent.getName(); }
	public void setName(String n) { theComponent.setName(n); }
	public void removeThis() 
	{ 	com.codename1.ui.Container par = theComponent.getParent();
		if(par!=null) { par.removeComponent(getComponent()); }
	}
	
	// support for rotater buttons
	private CanvasRotater rotater = null;
	public int getCanvasRotation() { return rotater.getCanvasRotation(); }
	public void setCanvasRotation(int n) 
		{ rotater.setCanvasRotation(n); }
	public boolean rotateCanvas(lib.Graphics g) { return rotater.rotateCanvas(g,this); }
	public void unrotateCanvas(lib.Graphics g) {  rotater.unrotateCanvas(g,this); }
	public int rotateCanvasX(int x,int y) { return rotater.rotateCanvasX(x,y,this); }
	public int rotateCanvasY(int x,int y) { return rotater.rotateCanvasY(x,y,this); }
	public int unrotateCanvasX(int x,int y) { return rotater.unrotateCanvasX(x,y,this); }
	public int unrotateCanvasY(int x,int y) { return rotater.unrotateCanvasY(x,y,this); }
	public int getRotatedWidth() { return rotater.getRotatedWidth(this); }
	public int getRotatedHeight() { return rotater.getRotatedHeight(this); }
	public int getRotatedTop() { return rotater.getRotatedTop(this); }
	public int getRotatedLeft() { return rotater.getRotatedLeft(this); }
	
	/* dummy methods to be overridden */
	public void windowActivated(WindowEvent w) {}
	public void windowClosed(WindowEvent w) {}
	public void windowClosing(WindowEvent w) {}
	public void windowOpened(WindowEvent w) {}
	public void windowDeactivated(WindowEvent w) {}
	public void windowIconified(WindowEvent w) {}
	public void windowDeiconified(WindowEvent w) {}
	
}
