package lib;

import java.awt.Font;

/**
 * this is an interface for the companion object of a Drawable object. These objects
 * have no particular requirements, but they are carriers for whatever other context
 * the Drawable might require.   For historical reasons, the most common DrawingObject
 * is an exCanvas, so that gets a bit of special treatment.  In any case, it should always
 * be valid to pass a null.
 */
public interface DrawingObject {
	/** WARNING ** adding this as a default method works incorrectly for android
	 * and CN1 says it can't be fixed.  So don't do it.    
	 * public default exCanvas getCanvas() { return null; }                                                                             
	 * @return
	 */
	public exCanvas getCanvas();
	public static exCanvas getCanvas(DrawingObject c) { return c==null ? null : c.getCanvas(); }
	public default Font getFont()
	{
		exCanvas c = getCanvas();
		return c==null ? FontManager.getGlobalDefaultFont() : c.getFont();
	}
}
