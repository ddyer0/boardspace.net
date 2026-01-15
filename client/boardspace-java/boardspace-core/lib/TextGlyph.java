/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package lib;

import java.awt.Color;
import java.awt.FontMetrics;
import bridge.Icon;

/**
 * this is an extension to {@link TextChunk} which draws {@link lib.Drawable} objects instead of text.
 * each "glyph" has an associated text string, whose size determines the amount of horizontal
 * space the glyph will occupy.   The glyphs have an additional set of scales and offsets which
 * can be adjusted to fit them into text lines.
 * 
 * @author Ddyer
 *
 */
public class TextGlyph extends TextChunk implements Text,Icon
{	
	private Drawable art = null;		// stockart, or more likely chips
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
	public Icon getIcon(DrawingObject drawon) 
	{ 	canvas = drawon;
		return this;
		//FontMetrics fm = FontManager.getFontMetrics(canvas.getFont());
		//return(new TextIcon(this,width(fm),height(fm))); 
	}
	// the scale args are <yscale> <xscale> <xoffset> <yoffset>
	private void parseScale(double scl[])
	{
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

	/**
	 * Create a glyph object, where the glyph fits into a line of text.
	 * this mode is used in the legacy "shortMoveText" methods that mix
	 * icons with text, but should the pure icon method should be used
	 * going forward.
	 * 
	 * @param text the text which reserves space for the glyph
	 * @param art the StockArt that becomes the glyph
	 * @param can the canvas (viewer) where the glyph will be drawn
	 * @param scl an array of scale factors, with up to 4 elements: line size multiplier, width multiplier, x-offset, y-offset
	 */
	private TextGlyph(String text,Drawable artwork,DrawingObject can,double scl[])
	{	super(text,null,false);
		art = artwork;
		canvas = can;
		parseScale(scl);
	}
	/**
	 * create a pure icon from drawable artwork with an alternative string.
	 * the alternative string is use for awt menus, which ought to be considered
	 * an emergency fallback.  The better choice is to set useSimpleMenu
	 * scale is relative to the font height of the drawing canvas
	 * 
	 * @param artwork
	 * @param can
	 * @param scl
	 */
	public TextGlyph(Drawable artwork,String alt, DrawingObject can, double[] scl) {
		art = artwork;
		data =alt;
		replacementData = null;
		canvas = can;
		parseScale(scl);
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
	public static TextGlyph create(String str,Drawable art,DrawingObject can,double... scl)
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
	public static TextGlyph create(String str,String replacement,Drawable art,DrawingObject can,double... scl)
	{	TextGlyph w = new TextGlyph(str,art,can,scl);
		w.replacementData = replacement;
		return(w);
	}

	
	/**
	 * Create a glyph object which is intended to replace text using colorize.
	 * 
	 * @param art the glyph
	 * @param can the canvas
	 * @param scl the scaling info for the glyph, relative to the font height
	 * @return a new Text object
	 */
	public static TextGlyph create(Drawable art,DrawingObject can,double... scl)
	{
		return new TextGlyph(art,null,can,scl);
	}
	
	/**
	 * Create a glyph object which is intended to replace text using colorize.
	 * 
	 * @param art the glyph
	 * @param can the canvas
	 * @param scl the scaling info for the glyph, relative to the font height the scale args are <yscale> <xscale> <xoffset> <yoffset>
	 * @return a new Text object
	 */
	public static TextGlyph create(Drawable art,String alt,DrawingObject can,double... scl)
	{
		return new TextGlyph(art,alt,can,scl);
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
	public static TextGlyph create(String str,String replacement,String visible,Drawable art,DrawingObject can,double... scl)
	{	TextGlyph w = new TextGlyph(str,art,can,scl);
		w.replacementData = replacement;
		w.visibleText = visible;
		return(w);
	}

	public int chunkWidth(FontMetrics myFM)
	{
		if(replacementData==null && art!=null)
		{ int h = myFM.getHeight();
		  int iconw = (int)(h*localScale);
		  return iconw;
		}else
		{ return super.chunkWidth(myFM);
		}
	}
	public int singleChunkHeight(FontMetrics myFM)
	{
		// overrides the standard method, multiplies the line height
		return((int)(myFM.getHeight()*localHScale)); 
	}

	/** draw the glyph instead of the text */
    public int drawTextChunk(Graphics inG,FontMetrics myFM,Color baseColor,int drawX,int drawY)
    {	if(down!=null) { return(down.drawTextLine(inG,myFM,baseColor,drawX,drawY)); }
    	if(art==null) {
    		int width = myFM.stringWidth(replacementData);
    		return( super.drawTextChunk(inG,myFM,baseColor,
    						drawX+(int)(xoff*width),drawY+(int)(width*yoff))); 
    		}
    	// drawing artwork
    	{ 	if(replacementData==null)	// based on the icon only
    		{
    		int h = myFM.getHeight();
    		int iconw = (int)(h*localScale);
    		art.drawChip(inG,canvas,
    				iconw,
	    			drawX+(int)(iconw*(0.5+xoff)),
	    			drawY-(int)(h*(0.5+yoff)),
	    			visibleText); 
    		return iconw+drawX;
    		}
    		else
    		{	
    		int width = myFM.stringWidth(replacementData);
    		int height = myFM.getHeight();
    		art.drawChip(inG,canvas,
    	    			(int)(width*localScale),
    	    			(int)(drawX+width*(0.5+xoff)),
    	    			(int)(drawY+height*yoff),visibleText); 
    		 return(width+drawX);
    		}
    	}
    }

}
