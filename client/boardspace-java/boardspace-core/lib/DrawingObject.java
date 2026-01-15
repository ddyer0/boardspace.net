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
	public default exCanvas getCanvas() { return null; }
	public static exCanvas getCanvas(DrawingObject c) { return c==null ? null : c.getCanvas(); }
	public default Font getFont()
	{
		exCanvas c = getCanvas();
		return c==null ? FontManager.getGlobalDefaultFont() : c.getFont();
	}
}
