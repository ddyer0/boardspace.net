package lib;

import bridge.Color;
import bridge.FontMetrics;
import bridge.Icon;
import online.common.exCanvas;

/**
 * this is an extension to {@link TextChunk} which draws {@link lib.Drawable} objects instead of text.
 * each "glyph" has an associated text string, whose size determines the amount of horizontal
 * space the glyph will occupy.   The glyphs have an additional set of scales and offsets which
 * can be adjusted to fit them into text lines.
 * 
 * @author Ddyer
 *
 */
public class TextGlyph extends TextChunk implements Text
{	
	private Drawable art = null;		// stockart, or more likely chips
	private exCanvas canvas = null;		// the viewer/canvas we draw on
	private double localHScale = 1.0;	// adjustment to the line height
	private double localScale = 1.0;	// adjustment to the string width
	private double xoff = 0.0;			// x position adjustment, relative to the width
	private double yoff = 0.0;			// y position adjustment, relative to the width (glyphs are normally square)
	private String visibleText = null;	// visible text drawn on the glyph
	
	public Text clone()
	{	TextGlyph copy = cloneSimple();
		copy.copyFrom(this);
		return(copy);
	}
	public Icon getIcon() 
	{ 	FontMetrics fm = G.getFontMetrics(canvas.getFont());
		return(new TextIcon(this,width(fm),height(fm))); 
	}
	/**
	 * Create a glyph object,
	 * 
	 * @param text the text which reserves space for the glyph
	 * @param art the StockArt that becomes the glyph
	 * @param can the canvas (viewer) where the glyph will be drawn
	 * @param scl an array of scale factors, with up to 4 elements: line size multiplier, width multiplier, x-offset, y-offset
	 */
	private TextGlyph(String text,Drawable art,exCanvas can,double scl[])
	{	super(text,null,false);
		this.art = art;
		this.canvas = can;
		switch(scl.length)
		{	default:
		case 4: this.yoff = scl[3];
			//$FALL-THROUGH$
		case 3: this.xoff = scl[2];
			//$FALL-THROUGH$
		case 2: this.localScale = scl[1];
			//$FALL-THROUGH$
		case 1: this.localHScale = scl[0];
			//$FALL-THROUGH$
		case 0: break;
		}
	}
	public TextGlyph cloneSimple()
	{	TextGlyph vis = new TextGlyph(replacementData,art,canvas,new double[]{localHScale,localScale,xoff,yoff});
		vis.visibleText = visibleText;
		return(vis);		
	}
	/**
	 * Create a glyph object
	 * 
	 * @param str the text which reserves space for the glyph
	 * @param art the StockArt that becomes the glyph
	 * @param can the canvas (viewer) where the glyph will be drawn
	 * @param scl an array of scale factors, with up to 4 elements: linesize multiplier, width multiplier, x-offset, y-offset
	 * @return a Text object
	 */
	public static TextGlyph create(String str,Drawable art,exCanvas can,double... scl)
	{	return( new TextGlyph(str,art,can,scl));
	}
	/**
	 * Create a glyph object which is intended to replace text using colorize.
	 * 
	 * @param str the match string
	 * @param replacement the replacement string (for sizing purposes)
	 * @param art the glyph
	 * @param can the canvas
	 * @param scl the scaling info
	 * @return a new Text object
	 */
	public static TextGlyph create(String str,String replacement,Drawable art,exCanvas can,double... scl)
	{	TextGlyph w = new TextGlyph(str,art,can,scl);
		w.replacementData = replacement;
		return(w);
	}
	/**
	 * Create a glyph with text overlaid
	 * 
	 * @param str	match text for colorize
	 * @param replacement replacement text for sizing
	 * @param visible the visible text
	 * @param art the stockart to be presented
	 * @param can the canvas
	 * @param scl the scale info
	 * @return a new Text object
	 */
	public static TextGlyph create(String str,String replacement,String visible,Drawable art,exCanvas can,double... scl)
	{	TextGlyph w = new TextGlyph(str,art,can,scl);
		w.replacementData = replacement;
		w.visibleText = visible;
		return(w);
	}
	// overrides the standard method, multiplies the line height
	public int singleChunkHeight(FontMetrics myFM) { return((int)(myFM.getHeight()*localHScale)); }
	
	/** draw the glyph instead of the text */
    public int drawTextChunk(Graphics inG,FontMetrics myFM,Color baseColor,int drawX,int drawY)
    {	if(down!=null) { return(down.drawTextLine(inG,myFM,baseColor,drawX,drawY)); }
    	if(art==null) { return( super.drawTextChunk(inG,myFM,baseColor,drawX,drawY)); }
    	int width = myFM.stringWidth(getString());
    	if(inG!=null) { art.drawChip(inG,canvas,
    			(int)(width*localScale),
    			(int)(drawX+width*(0.5+xoff)),
    			(int)(drawY+width*yoff),visibleText); }
    	return(width+drawX);
    }
}
