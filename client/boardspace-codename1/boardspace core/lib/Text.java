package lib;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;
import bridge.Color;
import bridge.FontMetrics;
import bridge.Icon;
/**
 * Text is an interface that represents a block of text, possibly with multiple lines, possibly colorized
 * or possibly mixed with or replaced by arbitrary artwork drawn with the {@link lib.Drawable} interface.
 * 
 * This is used primarily in drawing game logs, either by creating the log strings using 
 * {@link lib.TextChunk} and {@link lib.TextGlyph}, or by creating the log strings normally
 * and colorizing them using the {@link #colorize} interface.
 * 
 * 
 * @author Ddyer
 *
 */
public interface Text {
	
	/** get the first line of text */
	public abstract Text firstLine();
	/** get the next line of text, or null */
	public abstract Text nextLine();
	/** get the first chunk of text */
	public abstract Text firstChunk();
	/** get the next chunk of text, or null */
	public abstract Text nextChunk();
	/** get the last chunk of text */
	public abstract Text lastChunk();
	/** get the last line of text */
	public abstract Text lastLine();
	/** count the lines of text */
	public abstract int nLines();
	/** draw a the current line of this text object */
	public int drawTextLine(Graphics inG,FontMetrics myFM,Color baseColor,int drawX,int drawY);
	/** draw a single chunk of text */
	public int drawTextChunk(Graphics inG,FontMetrics myFM,Color baseColor,int drawX,int drawY);
	
	/** get the width of this text, the maximum of all lines width */
	public abstract int width(FontMetrics myFM);
	/** get height of this text, the sum of all lines height */
	public abstract int height(FontMetrics myFM);
	/** get the width of an individual chunk */
	public abstract int chunkWidth(FontMetrics myFM);
	/** get the height of an individual chunk */
	public abstract int chunkHeight(FontMetrics myFM);
	/** get the height of an individual line */
	public abstract int lineHeight(FontMetrics myFM); 
	/** get the width of an individual line */
	public abstract int lineWidth(FontMetrics myFM);
	/** create copy of this chunk */
	public Text cloneSimple();
	/** create a complete copy of this chunk 
	 */
	public Text clone();
	
	/**
	 * colorize this chunk, replacing words with translated colored words.  The matched words will be separated from the rest of the text by non-letter non-digit 
	 * characters, so "brown" will not match "brownie" in the text.  Normally, the text will be already translated, 
	 * and the translations should be supplied to the colorized words will be matched against their translated equivalents.
	 * @param s	the current translations
	 * @param coloredChunks the untranslated list of words to color
	 */
	public abstract void colorize(InternationalStrings s,
			Text... coloredChunks);

	/**
	 * get the color associated with this chunk, which may be null
	 * @return the color of the text
	 */
	public abstract Color getColor();

	/**
	 * get the text associated with this chunk.  For complex chunks that have been joined or colorized,
	 * this is the text for the entire collection.
	 * @return a string representation of the text
	 */
	public abstract String getString();

	/** get the number of characters in this chunk */
	public abstract int length();
	/** compare to a string */
	public abstract boolean equals(String str);
	/** get a character from this chunk */
	public abstract char charAt(int idx);
	/** append a string to this chunk */
	public abstract void append(String str);
	/** append a Text chunk to this chunk */
	public abstract void append(Text str);
	/** set the next chunk in this chain*/
	public abstract void setNext(Text ch);
	/**
	 * set inG to a suitable font size to print inStr in a specified box size. 
	 * This is the underlying font sizing algorithm used by {@link G#Text} 
	 * if inWidth < 0, no fitting is done and the current font size of not changed.
	 * @param inG
	 * @param inWidth
	 * @param inHeight
	 * @return the original inG font
	 */
	public abstract Font selectFontSize(Graphics inG, int inWidth, int inHeight);

	/** draw the colorized text string, resized downward if necessary to fit the box.  The chunk may be
	 *  composed of multiple lines that were split, and multiple chunks that were colorized.
	 * if width<0, center the text at xpos without resizing
	 * if height=0, center the text at ypos
	 *
	 * @param inG the graphics to draw
	 * @param center if true, center the text
	 * @param inX x location of the left edge
	 * @param inY y location of the upper-left corner
	 * @param inWidth box width
	 * @param inHeight box height
	 * @param fgColour foreground color (or null)
	 * @param bgColor background color (or null)
	 * @param fit if true, shrink the text to fit the box
	 * @return the text width
	 */
	public abstract int draw(Graphics inG,boolean center, int inX,
			int inY, int inWidth, int inHeight, Color fgColour, Color bgColor,boolean fit);
	
	
	/** draw the colorized text string, resized downward if necessary to fit the box.  The chunk may be
	 *  composed of multiple lines that were split, and multiple chunks that were colorized.
	 * if width<0, center the text at xpos without resizing
	 * if height=0, center the text at ypos
	 *
	 * @param inG the graphics to draw
	 * @param center if true, center the text
	 * @param inX x location of the left edge
	 * @param inY y location of the upper-left corner
	 * @param inWidth box width
	 * @param inHeight box height
	 * @param fgColour foreground color (or null)
	 * @param bgColor background color (or null)
	 * @return the text width
	 */
	public abstract int draw(Graphics inG,boolean center, int inX,
			int inY, int inWidth, int inHeight, Color fgColour, Color bgColor);
	
	/** draw the colorized text string, right justified, resized downward if necessary to fit the box.  
	 * The chunk may be composed of multiple lines that were split, and multiple chunks that were colorized.
	 * if width<0, center the text at xpos without resizing
	 * if height=0, center the text at ypos
	 *
	 * @param inG the graphics to draw
	 * @param center if true, center the text
	 * @param inX x location of the left edge
	 * @param inY y location of the upper-left corner
	 * @param inWidth box width
	 * @param inHeight box height
	 * @param fgColour foreground color (or null)
	 * @param bgColor background color (or null)
	 * @return the text width
	 */
	public abstract int drawRight(Graphics inG, int inX, int inY,
			int inWidth, int inHeight, Color fgColour, Color bgColor);

	/**
	 * draw the colorized text string, right justified, resized downward if necessary to fit the box.  
	 * The chunk may be composed of multiple lines that were split, and multiple chunks that were colorized.
	 * if width<0, center the text at xpos without resizing
	 * if height=0, center the text at ypos
	 * 
	 * @param inG
	 * @param r
	 * @param fgColour
	 * @param bgColor
	 * @return
	 */
	public abstract int drawRight(Graphics inG, Rectangle r, Color fgColour, Color bgColor);
	
	/** draw the colorized text string, resized downward if necessary to fit the box.  The chunk may be
	 *  composed of multiple lines that were split, and multiple chunks that were colorized.
	 * if width<0, center the text at xpos without resizing
	 * if height=0, center the text at ypos
	 *
	 * @param inG the graphics to draw
	 * @param rotation rotation in radians
	 * @param center if true, center the text
	 * @param r the rectangle bounding box
	 * @param fgColour foreground color (or null)
	 * @param bgColor background color (or null)
	 * @param boolean fit if fit is true, fit the text in the box by adjusting the font size down
	 * @return the text width
	 */	
	 public abstract int draw(Graphics inG, double rotation, boolean center, Rectangle r, Color fgColour, Color bgColor,boolean fit);

		/** draw the colorized text string, resized downward if necessary to fit the box.  The chunk may be
		 *  composed of multiple lines that were split, and multiple chunks that were colorized.
		 * if width<0, center the text at xpos without resizing
		 * if height=0, center the text at ypos
		 *
		 * @param inG the graphics to draw
		 * @param rotation the rotation, in radians
		 * @param center if true, center the text
		 * @param r the rectangle bounding box
		 * @param fgColour foreground color (or null)
		 * @param bgColor background color (or null)
		 * @return the text width
		 */	
	public abstract int draw(Graphics inG, double rotation, boolean center, Rectangle r, Color fgColour, Color bgColor);

	 public abstract Icon getIcon(); 
}