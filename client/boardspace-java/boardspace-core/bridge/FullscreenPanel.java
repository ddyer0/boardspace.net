package bridge;

import java.awt.Component;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.security.AccessControlException;

import lib.CanvasRotater;
import lib.CanvasRotaterProtocol;
import lib.G;
import lib.Graphics;
import lib.MenuInterface;
import lib.MenuParentInterface;
import lib.SizeProvider;
// dummy class for standard java, does not change the frame size
//
//dummy class for standard java, does not change the frame size
//note that if this uses JPanel instead of Panel, the inescapable 
//double buffering used by swing will kick in.  This can cause
//flashing refreshes due to interactions with our repaint manager,
//if it ever has to skip a frame refresh due to process interlocks.
//
public class FullscreenPanel extends Panel implements MenuParentInterface, CanvasRotaterProtocol,SizeProvider
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public FullscreenPanel()
	{
		super();
		//setOpaque(false);
	}
	public void show(MenuInterface menu, int x, int y) throws AccessControlException 
	{
		G.show(this, menu, x, y);
	}
	public void actualPaint(Graphics g)
	{
		super.paint(g.getGraphics());
	}
	public void paint(java.awt.Graphics g)
	{	paint(Graphics.create(g));
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
