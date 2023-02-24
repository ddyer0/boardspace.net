package bridge;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Panel;
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
public class FullscreenPanel extends Panel implements FullScreen,MenuParentInterface,NullLayoutProtocol
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public FullscreenPanel()
	{
		super();
		setLayout(new NullLayout(this)); 
		//setOpaque(false);
	}
	public void doNullLayout(Container parent)
	{
		setLocalBounds(0,0,getWidth(),getHeight());
	}
	public void setLocalBounds(int l,int t,int w, int h)
	{	
		for(int nc = getComponentCount()-1 ; nc>=0; nc--)
		{
			Component c = getComponent(nc);
			int cw = c.getWidth();
			int ch = c.getHeight();
			if((c instanceof FullScreen)
					&& ((cw!=w)||(ch!=h)))
			{	Dimension minSz = ((FullScreen)c).getMinimumSize();
				int aw = Math.max((int)minSz.getWidth(),w);
				int ah = Math.max((int)minSz.getHeight(),h);
				((FullScreen)c).setBounds(0, 0, aw, ah);
			}
		}
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
