package lib;

/**
 * interface for entities that are to be drawn at the end of the repaint process.
 * This allows them to appear "on top" of everything else without complicated
 * considerations of overlaps and drawing order.
 * @author Ddyer
 *
 */
public interface LateDrawing {
	/**
	 * draw the element
	 * @param g
	 */
	public void draw(Graphics g); 
}