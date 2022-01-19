package bridge;

import java.awt.Panel;
import java.awt.event.WindowEvent;
import java.security.AccessControlException;

import lib.G;
import lib.Graphics;
import lib.MenuInterface;
import lib.MenuParentInterface;
// dummy class for standard java, does not change the frame size
//
//dummy class for standard java, does not change the frame size
//note that if this uses JPanel instead of Panel, the inescapable 
//double buffering used by swing will kick in.  This can cause
//flashing refreshes due to interactions with our repaint manager,
//if it ever has to skip a frame refresh due to process interlocks.
//
public class FullscreenPanel extends Panel implements MenuParentInterface
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
