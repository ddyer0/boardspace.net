package bridge;

import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;

import lib.CanvasRotater;
import lib.CanvasRotaterProtocol;
import lib.G;
import lib.Graphics;
import lib.MenuInterface;
import lib.MenuParentInterface;
import lib.NullLayout;
import lib.NullLayoutProtocol;
import lib.SizeProvider;

import com.codename1.ui.Component;

public class FullscreenPanel extends JPanel implements FullScreen,NullLayoutProtocol,MenuParentInterface, CanvasRotaterProtocol,SizeProvider
{
	public FullscreenPanel() 
	{ setOpaque(false);
	  setLayout(new NullLayout(this)); 
	}
	public void doNullLayout(Container parent)
	{
		setLocalBounds(0,0,getWidth(),getHeight());
	}

	public com.codename1.ui.Container add(Component p)
	{
		return super.add(p);
	}
	
	//
    // these are still used by Tantrix. 
	// get here when calling back to paint the components of a container
	// the affected windows are the main lobby, the main game frame, and the chat applet
	// lobby and game are containers with no contents, the chat, if present, is a container
	// with contents.
	//
	public void actualPaint(Graphics g)
	{
		int y = getY();
		int x = getX();
		//
		// codename1 containers do not have the x,y applied.  They're added
		// when flipping to lib.Graphics paint, and have to be removed here.
		//
		g.translate(-x, -y);
		super.paint(g.getGraphics());
		g.translate(x, y);
		
	}
	public void paint(com.codename1.ui.Graphics g)
	{
		//
		// this is probably not completely correct. Paint in Codename1 containers is
		// called with the X,Y of the container not included in g, so where we break
		// from codename1, we have to apply it to meet the usual expectation.  This
		// is important for tantrix lobby, where the lobby is a 
		int y = getY();
		int x = getX();
		g.translate(x, y);
		paint(lib.Graphics.create(g));
		g.translate(-x, -y);
	}
	public void paint(lib.Graphics g)
	{
		actualPaint(g);
	}
	public void setLocalBounds(int l,int t,int w, int h)
	{	
		for(int nc = getComponentCount()-1 ; nc>=0; nc--)
		{
			Component c = getComponentAt(nc);
			int cw = c.getWidth();
			int ch = c.getHeight();
			if((c instanceof FullScreen)
					&& ((cw!=w)||(ch!=h)))
			{	Dimension minSz = ((FullScreen)c).getMinimumSize();
				int aw = Math.max(minSz.getWidth(),w);
				int ah = Math.max(minSz.getHeight(),h);
				((FullScreen)c).setBounds(0, 0, aw, ah);
			}
		}
	}
	public void show(MenuInterface menu, int x, int y) throws AccessControlException 
	{
		G.show(this, menu, x, y);
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
