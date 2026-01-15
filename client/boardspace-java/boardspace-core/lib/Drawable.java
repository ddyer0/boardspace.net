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
import java.awt.Rectangle;

/**
 * a Drawable object can be anything that implements drawChip, used for the purpose of displaying
 * an animation element or icon. The primary use of this interface is for {@link lib.DrawableImage} and it's subclasses;
 * {@link online.game.chip} {@link online.game.cell} and {@link online.game.stackCell}.
 * other uses include animation sprites, and {@link lib.MultiGlyph}, a container for several Drawable objects drawn together.
 * 
 * @author ddyer
 *
 */
public interface Drawable {
	public static String NotHelp = "|";			// directive to pass the string through rather than draw it, instead of helptext
	public static String NotHelpDraw = "||";	// directive to pass rest of the string through for drawing
	/**
	 * implements the drawing method, which is used to draw this object.  This is the only thing
	 * that implementors or Drawable are required to implement.
	 *   
	 * @param gc	the graphics object for drawing
	 * @param c		the DrawingObject being drawn on, usually an exCanvas or null
	 * @param size	the width, in pixels, to draw the object as
	 * @param posx	the x position of the center of the object
	 * @param posy	the y position of the center of the object
	 * @param msg	text to superimpose after drawing the object, or in combination with a HitPoint the help context
	 */
	public void drawChip(Graphics gc,DrawingObject c,int size, int posx,int posy,String msg);
	/**
	 * 
	 * @param gc	the graphics object for drawing
	 * @param c		the DrawingObject being drawn on, usually an exCanvas or null
	 * @param r		the rectangle to fill
	 * @param msg	text to superimpose after drawing the object.
	 */
	public default void drawChip(Graphics gc,DrawingObject c,Rectangle r,String msg)
	{
		drawChip(gc,c,Math.min(G.Height(r),G.Width(r)),G.centerX(r),G.centerY(r),msg);
	}
	
	/**
	 * rotate x,y around the current center px, py and remember it.  This is used
	 * to set current_center_x and current_center_y for animations.
	 * @param displayRotation
	 * @param x
	 * @param y
	 * @param px
	 * @param py
	 */
	public default void rotateCurrentCenter(double displayRotation,int x,int y,int px,int py) {};
	/**
	 * the rotation to use during active animations, which normally is arranged
	 * to default to the orientation of the destination
	 * @return
	 */
	public default double activeAnimationRotation() { return 0; };
	/**
	 * This is a specialization used in animations; when a piece is being animated between two locations,
	 * it is actually already sitting at the destination.  This is used by the code displaying the destination
	 * to make the destination disappear until the animation is finished.
	 * @return the height of the stack for the purpose of reducing the height of the destination target.
	 */
	public default int animationHeight() { return 0; }
	public default String getName() { return "drawable"; }
	public int getWidth();
	public int getHeight();
	
    /**
     * this method is made visible so you can tweak the radius that constitutes a hit.
     * Note that the target is a smaller square, not a circle.
     * @param pt the mouse location
     * @param x  x position of the center of the chip
     * @param y  y position of the center of the chip
     * @param SQW square cell width of the chip
     * @param SQH is the cell height of the chip
     * @param sscale the scale applied to width and height
     * @return true of point is inside the square centered at x,y
     * @see HitPoint#drawChipHighlight drawChipHighlight
     * @see online.game.chipCell#drawChip drawChip
     * @see online.game.stackCell#drawStack drawStack
     */
    public default boolean pointInsideCell(HitPoint pt,int x,int y,int SQW,int SQH,double sscale)
    {	return(G.pointNearCenter(pt, x, y, (int)(SQW*sscale*0.5),(int)(SQH*sscale*0.5)));
    }
    /**
     * this method is made visible so you can tweak the radius that constitutes a hit.
     * Note that the target is a 2/3 smaller square, not a circle.
     * @param pt the mouse location
     * @param x  x position of the chip
     * @param y  y position of the chip
     * @param SQW square cell width of the chip
     * @param SQH is the cell height of the chip
     * @return true of point is inside the square centered at x,y
     * @see HitPoint#drawChipHighlight drawChipHighlight
     * @see online.game.chipCell#drawChip drawChip
     * @see online.game.stackCell#drawStack drawStack
     */
    public default boolean pointInsideCell(HitPoint pt,int x,int y,int SQW,int SQH)
    {	return(pointInsideCell(pt, x, y, SQW,SQH,0.66));
    }
    /**
     * register a hit on this cell.  
     * @param highlight
     * @param e_x
     * @param e_y
     * @return true if this is the hit object (even if not new)
     */
    public default boolean registerChipHit(CellId rackLocation,HitPoint highlight,int e_x,int e_y,int e_w,int e_h)
    {
      // if the id is null don't trigger a hit, but do return true to indicate
      // it would have been.  This has the effect of setting help text or allowing
      // the caller to take some other action.
      if(rackLocation!=null)
      {
 	  highlight.hitObject = this; 
  	  highlight.hit_x = e_x;
  	  highlight.hit_y = e_y;
  	  highlight.hit_width = e_w;
  	  highlight.hit_height = e_h;
  	  highlight.hitCode = rackLocation;
      }
  	  return(true);
    }

	 /**
	  * do highlight detection for {@link #drawChip} and {@link online.game.stackCell#drawStack drawStack}  This uses {@link #pointInsideCell} to 
	  * determine if the point is inside, and calls {@link #registerChipHit} to mark the hit in the highlight object.
	 * @param highlight
	 * @param e_x
	 * @param e_y
	 * @param squareWidth
	  * 
	  * @return true if this is the first hit
	  */
   public default boolean findChipHighlight(CellId rackLocation,HitPoint highlight,int e_x,int e_y,int squareWidth,int squareHeight,double sscale)
	 	{	
	        if(pointInsideCell(highlight, e_x, e_y, squareWidth,squareHeight,sscale))
	        {	// this is carefully balanced so we do not re-evaluate the selection if it is
	      	// already established.  We return TRUE if this is the selection that was established,
	      	// either this time or previously
	        	return(registerChipHit(rackLocation,highlight,e_x,e_y,squareWidth,squareHeight));
	        }
	        return(false);

	    }
	/**
	 * test the highlight point for a hit on this piece, as adjusted by the image aspect
	 * ratio, the scale, and offsets for this chip.  If a hit is declared, {@link #registerChipHit} is called
	 * @param highlight the current pointer location
	 * @param squareWidth the width of the space to fit into
	 * @param squareHeight the height of the space to fit into
	 * @param racklocation the {@link lib.CellId} of the object being tested
	 * @param e_x the center x to draw
	 * @param e_y the center y to draw
	 * @return true if this chip is hit
	 */
   public default boolean findChipHighlight(CellId rackLocation,HitPoint highlight,int e_x,int e_y,int squareWidth,int squareHeight)
   {
   	return findChipHighlight(rackLocation,highlight,e_x,e_y,squareWidth,squareHeight,0.66);
   }

   
	/**
	 * draw stock art to fill the specified rectangle, return true if it is hit
	 * and set hitpoint with the specified id and tool tip
	 * @param gc
	 * @param canvas the DrawingObject being drawn on, usually an exCanvas or null
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param tooltip
	 * @param sscale  sensitive area scale factor
	 * @return true if the highlight point was hit and rackLocation is not null
	 */
	public default boolean drawChip(Graphics gc,DrawingObject canvas,Rectangle r,HitPoint highlight,CellId rackLocation,Text tooltip,double sscale)
	{	return drawChip(gc,canvas,r,highlight,rackLocation,tooltip,sscale,1);
	}
	/**
	 * draw stock art to fill the specified rectangle, return true if it is hit
	 * and set hitpoint with the specified id and tool tip
	 * @param gc
	 * @param canvas the DrawingObject being drawn on, usually an exCanvas or null
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param tooltip
	 * @param sscale  sensitive area scale factor
	 * @return true if the highlight point was hit and rackLocation is not null
	 */
	public default boolean drawChip(Graphics gc,DrawingObject canvas,Rectangle r,HitPoint highlight,CellId rackLocation,String tooltip,double sscale)
	{	return drawChip(gc,canvas,r,highlight,rackLocation,TextChunk.create(tooltip),sscale);
	}

	

	/**
	 * draw stock art to fill the specified rectangle, return true if it is hit
	 * and set hitpoint with the specified id and tool tip.  Uses the default
	 * sensitive area scale factor of 1.3
	 * @param gc
	 * @param canvas the DrawingObject being drawn on, usually an exCanvas or null
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param tooltip
	 * @return true if the highlight point was hit and rackLocation is not null
	 */
	public default boolean drawChip(Graphics gc,DrawingObject canvas,Rectangle r,HitPoint highlight,CellId rackLocation)
	{	return drawChip(gc,canvas,r,highlight,rackLocation,(String)null,1.3);
	}

	/**
	 * draw stock art to fill the specified rectangle, return true if it is hit
	 * and set hitpoint with the specified id and tool tip.  Uses the default
	 * sensitive area scale factor of 1.3
	 * @param gc
	 * @param canvas the DrawingObject being drawn on, usually an exCanvas or null
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param tooltip
	 * @return true if the highlight point was hit and rackLocation is not null
	 */
	public default boolean drawChip(Graphics gc,DrawingObject canvas,Rectangle r,HitPoint highlight,CellId rackLocation,String tooltip)
	{	return drawChip(gc,canvas,r,highlight,rackLocation,tooltip,1.3);
	}

	/**
	 * draw stock art to fill the specified rectangle, return true if it is hit
	 * and set hitpoint with the specified id and tool tip.  Uses the default
	 * sensitive area scale factor of 1.3
	 * @param gc
	 * @param canvas the DrawingObject being drawn on, usually an exCanvas or null
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param tooltip
	 * @return true if the highlight point was hit and rackLocation is not null
	 */
	public default boolean drawChip(Graphics gc,DrawingObject canvas,Rectangle r,HitPoint highlight,CellId rackLocation,Text tooltip)
	{	return drawChip(gc,canvas,r,highlight,rackLocation,tooltip,1.3);
	}
	
	public default double getAspectRatio(DrawingObject DrawOn)
	{
		return 1.0;
	}

	/**
	 * draw a chip and test for mouse sensitivity.  If the highlight is hit, the width is 
	 * multiplied by "expansion" to give a visual "pop" to indicate the hit.  As a special
	 * hack, 
	 * 	if the help text starts with NotHelpDraw the rest of the string is drawn instead of used as help text. 
	 *    This is used by a few widgets to display text inside icons.
	 *  if the help text starts with NotHelp, the string is passed through instead of used as help text.
	 *    This is used in conjunction with drawChip methods to decorate or alter the images being drawn.
	 * 
	 * @param gc			// the graphics to draw with
	 * @param drawOn		// the DrawingObject being drawn on, usually an exCanvas or null
	 * @param squareWidth	// the overall scale of the object
	 * @param e_x			// the center of the object
	 * @param e_y			// the center y of the object
	 * @param highlight		// the mouse point, or null.  Receives hit information
	 * @param rackLocation	// the cellId to use for this if hit
	 * @param helptext		// label to print on top
	 * @param sscale		// sensitive area scale factor, default 0.66
	 * @param expansion		// 	size expansion factor (if hit and rackLocation not null)
	 * @return true if this chip is hit and rackLocation is not null
	 */
	public default boolean drawChip(Graphics gc,DrawingObject drawOn,int squareWidth,int e_x,
							int e_y,HitPoint highlight,CellId rackLocation,String helptext,double sscale,double expansion)
	{ 
      double aspect = getAspectRatio(drawOn);
      boolean val = findChipHighlight(rackLocation,highlight,e_x,e_y,squareWidth,(int)(squareWidth/aspect),sscale);
      String draw = helptext!=null && helptext.startsWith(NotHelp) 
    		  			? helptext.startsWith(NotHelpDraw) ? helptext.substring(NotHelpDraw.length()) : helptext
    		  			: null;
      String help = draw==null ? helptext : null;
      drawChip(gc,drawOn,(int)((val&&rackLocation!=null)?expansion*squareWidth:squareWidth),e_x,e_y,draw);
      if(val)
      	{
    	  highlight.setHelpText(help);
      	}
      return(val && (rackLocation!=null));
	}

	/**
	 * draw a chip and test for mouse sensitivity.  If the highlight is hit, the width is 
	 * increased by 1/3 to give a visual "pop" to indicate the hit.
	 * @param gc			// the graphics to draw with
	 * @param drawOn		// the DrawingObject being drawn on, usually an exCanvas or null
 	 * @param squareWidth	// the overall scale of the object
	 * @param e_x			// the center of the object
	 * @param e_y			// the center y of the object
	 * @param highlight		// the mouse point, or null.  Receives hit information
	 * @param rackLocation	// the cellId to use for this if hit
	 * @param helptext		// label to print on top

	 * @return true if this chip is hit and rackLocation is not null
	 */
	public default boolean drawChip(Graphics gc,DrawingObject drawOn,int squareWidth,int e_x,int e_y,HitPoint highlight,CellId rackLocation,
			String helptext)
	{
		return(drawChip(gc,drawOn,squareWidth,e_x,e_y,highlight,rackLocation,helptext,0.66,1.33));
	}
	/**
	 * draw stock art to fill the width of the rectangle, based on the width of the rectangle
	 * @param gc
	 * @param canvas
	 * @param r
	 * @param label
	 * @param scale
	 */
	public default void drawChip(Graphics gc,DrawingObject canvas,Rectangle r,String label,double scale)
	{	
		drawChip(gc,canvas,(int)(Math.max(G.Height(r),G.Width(r))*scale),G.centerX(r),G.centerY(r),label);
	}
	/**
	 * 	 * draw a chip and test for mouse sensitivity.  If the highlight is hit, the width is 
		 * multiplied by "expansion" to give a visual "pop" to indicate the hit.  As a special
		 * hack, 
		 * 	if the help text starts with NotHelpDraw the rest of the string is drawn instead of used as help text. 
		 *    This is used by a few widgets to display text inside icons.
		 *  if the help text starts with NotHelp, the string is passed through instead of used as help text.
		 *    This is used in conjunction with drawChip methods to decorate or alter the images being drawn.
		 * 
	 * @param gc
	 * @param drawOn the DrawingObject being drawn on, usually an exCanvas or null
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param helptext
	 * @param sscale
	 * @param expansion
	 * @return
	 */
	public default boolean drawChip(Graphics gc,DrawingObject drawOn,Rectangle r,HitPoint highlight,CellId rackLocation,String helptext,double sscale,double expansion)
	{
		return drawChip(gc,drawOn,r,highlight,rackLocation,TextChunk.create(helptext),sscale,expansion);
	}
	/**
	 * 	 * draw a chip and test for mouse sensitivity.  If the highlight is hit, the width is 
		 * multiplied by "expansion" to give a visual "pop" to indicate the hit.  As a special
		 * hack, 
		 * 	if the help text starts with NotHelpDraw the rest of the string is drawn instead of used as help text. 
		 *    This is used by a few widgets to display text inside icons.
		 *  if the help text starts with NotHelp, the string is passed through instead of used as help text.
		 *    This is used in conjunction with drawChip methods to decorate or alter the images being drawn.
		 * 
	 * @param gc
	 * @param drawOn the DrawingObject being drawn on, usually an exCanvas or null
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param helptext
	 * @param sscale
	 * @param expansion
	 * @return
	 */
	public default boolean drawChip(Graphics gc,DrawingObject drawOn,Rectangle r,HitPoint highlight,CellId rackLocation,Text helptext,double sscale,double expansion)
		{
		boolean val = findChipHighlight(rackLocation,highlight,G.centerX(r),G.centerY(r),G.Width(r),G.Height(r),sscale);
		int sz = (int)(Math.max(G.Height(r),G.Width(r))*(val ? expansion : 1));
		drawChip(gc,drawOn,sz,G.centerX(r),G.centerY(r),null);
		if(val)
		{	highlight.setHelpText(helptext);
			if(expansion<=1)
				{highlight.spriteColor = Color.red;
				 highlight.spriteRect = r;
				}
		}
		return(val && (rackLocation!=null));
		}



}

