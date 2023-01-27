package bridge;

import java.awt.Component;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import lib.CanvasRotater;
import lib.CanvasRotaterProtocol;
import lib.G;
import lib.Graphics;
import lib.LFrameProtocol;
import lib.SizeProvider;
import lib.TextContainer;

@SuppressWarnings("serial")
public abstract class Canvas extends java.awt.Canvas 
	implements SizeProvider , CanvasRotaterProtocol, WindowListener, MouseListener,MouseMotionListener,MouseWheelListener
{	public Canvas() { super(); }
	public Canvas(LFrameProtocol frame)
	{	super();
		frame.setCanvasRotater(this);
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
	CanvasRotaterProtocol rotater = new CanvasRotater(this);
	public int getCanvasRotation() { return rotater.getCanvasRotation(); }
	public boolean quarterTurn() { return rotater.quarterTurn(); }
	public void setCanvasRotation(int n) { rotater.setCanvasRotation(n); }
	public boolean rotateCanvas(Graphics g) { return rotater.rotateCanvas(g); }
	public void unrotateCanvas(Graphics g) {  rotater.unrotateCanvas(g); }
	public int rotateCanvasX(int x,int y) { return rotater.rotateCanvasX(x,y); }
	public int rotateCanvasY(int x,int y) { return rotater.rotateCanvasY(x,y); }
	public int unrotateCanvasX(int x,int y) { return rotater.unrotateCanvasX(x,y); }
	public int unrotateCanvasY(int x,int y) { return rotater.unrotateCanvasY(x,y); }
	public Rectangle getRotatedBounds() { return rotater.getRotatedBounds(); }
	
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
