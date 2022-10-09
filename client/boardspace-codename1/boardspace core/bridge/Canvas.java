package bridge;

import com.codename1.ui.geom.Rectangle;

import lib.CanvasRotater;
import lib.CanvasRotaterProtocol;
import lib.G;
import lib.Graphics;

public class Canvas extends Component implements CanvasRotaterProtocol,WindowListener,MouseListener
{	

	public void paint(Graphics g) { G.Error("Should be overridden");}
	
	public void paint(com.codename1.ui.Graphics g)
	{	
		paint(Graphics.create(g));
	}
	// support for rotater buttons
	CanvasRotaterProtocol rotater = new CanvasRotater(this);
	public int getCanvasRotation() { return rotater.getCanvasRotation(); }
	public boolean quarterTurn() { return rotater.quarterTurn(); }
	public void setCanvasRotation(int n) { rotater.setCanvasRotation(n); }
	public boolean rotateCanvas(lib.Graphics g) { return rotater.rotateCanvas(g); }
	public void unrotateCanvas(lib.Graphics g) {  rotater.unrotateCanvas(g); }
	public int rotateCanvasX(int x,int y) { return rotater.rotateCanvasX(x,y); }
	public int rotateCanvasY(int x,int y) { return rotater.rotateCanvasY(x,y); }
	public int unrotateCanvasX(int x,int y) { return rotater.unrotateCanvasX(x,y); }
	public int unrotateCanvasY(int x,int y) { return rotater.unrotateCanvasY(x,y); }
	public Rectangle getRotatedBounds() { return rotater.getRotatedBounds(); }

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
}
