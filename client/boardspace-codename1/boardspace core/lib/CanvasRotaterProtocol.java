package lib;

import com.codename1.ui.geom.Rectangle;

/**
 * the general scheme for rotating individual windows is to keep the physical window
 * unchanged, but arrange for the contents to be drawn in a rotated way, and the mouse
 * coordinates to be similarly transformed.   
 * 
 * For rotation 2, which is upside-down, x and y are moved diagonally across the screen.
 * For rotation 1 and 3, the width and height are swapped and x or y reversed.  
 * 
 * The drawing and mouse handling code is all oriented to the  rotated screen size, so they
 * have to be careful to ignore the physical size.   the setLocalBounds() layout receives
 * the rotated width and height.
 * 
 * @author ddyer
 *
 */
public interface CanvasRotaterProtocol {
	/**
	 * get the canvas rotation in quater turn units
	 * @return
	 */
	public int getCanvasRotation();
	/**
	 * 
	 * @return true if the current rotation is a quater turn rotation
	 */
	public boolean quarterTurn();
	/**
	 * set the canvas rotatation in quarter turn units
	 * @param quarter_turns
	 */
	public void setCanvasRotation(int quarter_turns);
	/**
	 * transform the coordinate system corresponding to the quarter turn rotation.  Note
	 * that for odd numbers, this effectively swaps the width and height
	 * @param g
	 * @return
	 */
	public boolean rotateCanvas(Graphics g);
	/**
	 * undo the rotation of the graphics space which was done by rotateCanvas
	 * @param g
	 */
	public void unrotateCanvas(Graphics g);
	/**
	 * translate a coordinate from a physical window into the rotated space
	 * @param x
	 * @param y
	 * @return
	 */
	public int rotateCanvasX(int x,int y);
	/**
	 * translate a coordinate from a physical window into the rotated space
	 * @param x
	 * @param y
	 * @return
	 */
	public int rotateCanvasY(int x,int y);
	/**
	 * reverse a coordinate in rotated space back to physical space
	 * @param x
	 * @param y
	 * @return
	 */
	public int unrotateCanvasX(int x,int y);
	/**
	 * reverse a coordinate in rotated space back to physical space
	 * @param x
	 * @param y
	 * @return
	 */
	public int unrotateCanvasY(int x,int y);
	/**
	 * return the window boundary, adjusted for the rotation (if any)
	 * @return
	 */
	public Rectangle getRotatedBounds();
}
