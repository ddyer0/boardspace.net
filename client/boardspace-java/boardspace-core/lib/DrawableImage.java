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
import online.common.exCanvas;


/**
 * DrawableImage is an extension on Image, with support for loading, masking, scaling and displaying
 * the images. See class {@link lib.StockArt} for a collection of useful images.
 * <p>
 * The general characteristics of DrawableImage is that they are immutable, scalable images
 * that are used by the program.  Generally, one copy of the image is loaded into
 * a static variable at initialization time, and used throughout the rest of the program.
 * 
 * Machinery in the canvas creates scaled copies and takes care of drawing the image with
 * an appropriate size.  The canvas also has an "image substitution" mechanism so alternate
 * chipsets can be used without changing the actual contents of the board.
 *  
 * @author ddyer
 *
 */

public class DrawableImage<T> implements Drawable,StackIterator<T>
{
	public void rotateCurrentCenter(double amount,int x,int y,int cx,int cy) {};
 	public double activeAnimationRotation() { return(0); }
 	public int getWidth() { return(0); }
 	public int getHeight() { return(0); }
	// implement "StackIterator".  This allows a singleton piece of art
	// to be morped into a stack of art (if manipulated correctly)
	public int size() { return(1); }		// stack size is always 1
	@SuppressWarnings("unchecked")
	public T elementAt(int n) { return((T)this); }	// return this item as its own zero'th element
	@SuppressWarnings("unchecked")
	public StackIterator<T> push(T item)			// add a new item, which converts us to a stack of items
	{ 	
		StackIterator<T> stack = (StackIterator<T>)new DrawableImageStack();
		stack.push((T)this);
		return(stack.push(item));
	}
	@SuppressWarnings("unchecked")
	public StackIterator<T> insertElementAt(T item,int at)
	{
		StackIterator<T> stack = (StackIterator<T>)new DrawableImageStack();
		stack.push((T)this);
		return(stack.insertElementAt(item, at));
	}
	public StackIterator<T> remove(T item) 
	{
		return(item==this ? null : this);
	}
	public StackIterator<T> remove(int n)
	{	return((n==0) ? null : this);
	}
	
	
	/** the file from which this image was loaded */
	@SuppressWarnings("rawtypes")
	
	public String file = null;
	
	/** An array of 3 elements, x offset, y offset and scale factor. 
	 * <br>
	 * These parameters are used to normalize the location and size of the image
	 * relative to its center.  This array is normally set up during construction
	 * and not changed.
	 */
	public double scale[]={0.5,0.5,1.0};
	/**
	 * get the scale factor array for this image, which is the normal x,y,scale
	 * to display the image.  The neutral values would be {0.5,0.5,1.0}
	 * <p>
	 * These scale factors are normally hand-tuned so the default size and 
	 * positioning of the artwork is as desired - the alternative to using
	 * these scale factors is to actually edit the image with photoshop.
	 * <p>
	 * Note that if you stay within the system, you'll never need to see or use
	 * these scale factors, they're just an internal detail you tune so the images
	 * look right.
	 * @return an array double[3]
	 */
	public double[] getScale() { return(scale); };
	/** the underlying image for this StockArt.  Normally this is set by a static initializer,
	 * but sometimes it's useful to delay loading.  This probably shouldn't be used to implement
	 * changeable images.  
	 */
	public Image image=null;
	public String getName() { return(image!=null ? image.getName() : file); }
	public Image getImage() { return(image); }
	
	/** get the underlying image for actual use.  You can override this method to implement delayed loading
	 *  or dynamically generated images 
	 * @param canvas an exCanvas or null
	 */
	public Image getImage(ImageLoader canvas)
	{   // canvas may be null
		return(image); 
	}
	/** the default constructor */
	public DrawableImage() { };
	/** default method for animations */
	public int animationHeight() { return(1); }
	public String toString() { return("#<stock "+file+">"); }
	/** Override this method to define "alternate image set" behavior, for example
	 * if you had a set of red/green chips as an alternative to the standard white/black.
	 * 
	 * @param chipset
	 * @see online.common.exCanvas#getAltChipset getAltChipSet
	 * @return a new instance of this class, or this instance
	 */
	@SuppressWarnings("unchecked")
	public DrawableImage<T> getAltChip(int chipset) { return(this); }
	/**
	 * this is the default method called to help build a meaningful, brief value
	 * for {@link Object#toString toString}.  The default is usually the file name from
	 * which the artwork was loaded.
	 * @return a reasonably brief name string
	 */
	public String contentsString() { return(file+" "); }



    /**
     * draw the image (or it's alternate) and a text string to label it. The string is drawn
	 * centered according to the x,y offset. The image is drawn centered at the x,y
	 * specified, and scaled to fill the specified square size, as adjusted by the
	 * image's internal offset and scale factors.
     * @param gc a graphics or null
     * @param canvas the canvas to draw on
     * @param SQUARESIZE
     * @param cx
     * @param cy
     * @param label the string label, or null
     * @param artCenter if true, draw text at teh artwork center rather than the image center.
 	 * @see exCanvas#drawImage drawImage
    */
	public void drawChipFancyText(Graphics gc,exCanvas canvas,int SQUARESIZE,int cx,int cy,String label,boolean artCenter)
	{	if(gc!=null)
		{
		  DrawableImage<T> alt = getAltChip(canvas.getAltChipset());
	 	  double pscale[]=alt.getScale();
	      // use this to tune piece position
	      canvas.adjustScales(pscale,alt);
	      alt.drawChipImage(gc,canvas,cx,cy,pscale,SQUARESIZE,1.0,0.0,label,artCenter);
		}
	}
	/** 
	 * 
	 * @param can a canvas
	 * @return get the actual aspect ratio of the image, width/height
	 */
	public double getAspectRatio(exCanvas can)
	{	DrawableImage<?> alt = getAltChip(can.getAltChipset());
		Image im = alt.getImage(can.loader());
		double v = ((im==null)?1.0 : Math.max(1.0,im.getWidth())/Math.max(1.0,im.getHeight()));
		return(v);
	}
	/**
	 * draw an image (or it's alternate) and a text string to label it.  The string is drawn
	 * centered according to the x,y offset. The image is drawn centered at the x,y
	 * specified, and scaled to fill the specified square size, as adjusted by the
	 * image's internal offset and scale factors.
	 * @param gc the graphics object, or null
	 * @param canvas the canvas we're drawing on
	 * @param SQUARESIZE 
	 * @param cx
	 * @param cy
	 * @param label the text label or null
	 * @see exCanvas#drawImage drawImage
	 */
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,int cx,int cy,String label)
	{	drawChip(gc,canvas,SQUARESIZE,1.0,cx,cy,label);
	}

	/**
	 * get the offset needed to put the image at the visual center when displayed at the
	 * specified width.  This is needed when images are being drawn outside their usual
	 * framework.
	 * @param width
	 * @return an offset int in x
	 */
	public int local_x_offset(int width)
	{	double sc[] = getScale();
		return((int)(sc[2]*(sc[0]-0.5)*width));
	}
	/**
	 * get the offset needed to put the image at the visual center when displayed at the
	 * specified width.  This is needed when images are being drawn outside their usual
	 * framework.
	 * @param height
	 * @return and offset int in y
	 */
	public int local_y_offset(int height)
	{	double sc[] = getScale();
		return((int)(sc[2]*(sc[1]-0.5)*height));
	}

/**
 * this is the lowest level overridable "drawChip" method
 * @param gc
 * @param canvas
 * @param SQUARESIZE
 * @param xscale
 * @param cx
 * @param cy
 * @param label
 */
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{
		drawChipInternal(gc,canvas,SQUARESIZE,xscale,cx,cy,label);
	}
	/** draw a chip with a rotated graphics 
	 * 
	 * @param gc
	 * @param canvas
	 * @param rotation
	 * @param SQUARESIZE
	 * @param xscale
	 * @param cx
	 * @param cy
	 * @param label
	 */
	public void drawRotatedChip(Graphics gc,exCanvas canvas,double rotation,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{	if(rotation!=0) { GC.setRotation(gc, rotation, cx, cy); }
		drawChipInternal(gc,canvas,SQUARESIZE,xscale,cx,cy,label);
		if(rotation!=0) { GC.setRotation(gc, -rotation, cx, cy); }
	}

	/**
	 * this is the bottom level of the chip "draw" line.  It substitutes a different piece of stockart
	 * based on canvas.getAltChipSet(), and scales the scale factors as required by the "show aux slider"
	 * option.
	 * @param gc
	 * @param canvas
	 * @param SQUARESIZE
	 * @param xscale
	 * @param cx
	 * @param cy
	 * @param label
	 */
	public final void drawChipInternal(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{if(gc!=null)
	  {
	  DrawableImage<?> alt = getAltChip(canvas==null?0:canvas.getAltChipset());
 	  double pscale[]=alt.getScale();
      // use this to tune piece position
      if(canvas!=null) { canvas.adjustScales(pscale,alt); }
      alt.drawChipImage(gc,canvas,cx,cy,pscale,
    		  SQUARESIZE,xscale,0.0,label,true);
 	  }
	} 
	/**
	 * this is the lowest level overridable method, which actually passes to the canvas for drawing.  Stock art which constructs
	 * its image in complex ways can override this method.  The standard definition of this method is
	 * canvas.drawImage(gc, getImage(), pscale,cx, cy, SQUARESIZE, xscale,yscale,label,artcenter);
	 * 
	 * @param gc
	 * @param canvas
	 * @param cx
	 * @param cy
	 * @param pscale
	 * @param SQUARESIZE
	 * @param xscale
	 * @param jitter
	 * @param label
	 * @param artcenter
	 */
	public void drawChipImage(Graphics gc,exCanvas canvas,int cx,int cy,double pscale[],
			int SQUARESIZE,double xscale,double jitter,String label,boolean artcenter)
	{     canvas.drawImage(gc, getImage(canvas.loader), pscale,cx, cy, SQUARESIZE, xscale,jitter,label,artcenter);
	}
	/**
	 * this is called to draw the intersection of two tiles which are part of a polyomino group.
	 * The x,y are the center of the intersection 
	 * @param gc
	 * @param canvas
	 * @param SQUARESIZE
	 * @param xscale
	 * @param vertical
	 * @param cx
	 * @param cy
	 * @param label
	 */
	public void drawChipTween(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,boolean vertical,int cx,int cy,String label)
	{	return;
	} 
	/**
	 * draw stock art to fill the height of the specified rectangle
	 * @param gc
	 * @param canvas
	 * @param r
	 * @param label
	 */
	public void drawChipH(Graphics gc,exCanvas canvas,Rectangle r,String label)
	{	drawChip(gc,canvas,G.Height(r),(double)G.Width(r)/G.Height(r),G.centerX(r),G.centerY(r),label);
	}
	/**
	 * draw stock art to fill the width of the rectangle, based on the width of the rectangle
	 * @param gc
	 * @param canvas
	 * @param r
	 * @param label
	 */
	public void drawChip(Graphics gc,exCanvas canvas,Rectangle r,String label)
	{	
		int sz = G.Width(r);
		Image im = getImage(canvas.loader());
		if(im!=null)
			{
			double aspect = (double)im.getWidth()/im.getHeight();
			int hr = G.Height(r);
			int h = (int)(sz/aspect);
			if(h>hr) 
				{ sz = (int)(hr*aspect); 
				}
			}

		drawChip(gc,canvas,sz,1,G.centerX(r),G.centerY(r),label);
	}
	/**
	 *  return a new rectangle that will snugly fix this chip centered on the supplied rectangle
	 * @param canvas
	 * @param r
	 * @return
	 */
	public Rectangle getSnugRectangle(exCanvas canvas,Rectangle r)
	{	Image im = getImage(canvas.loader());
		if(im!=null) { return(im.getSnugRectangle(r));}
		return(r);
	}
	/**
	 * return a rectangle that will snugly fix this chip centered on the supplied rectangle
	 * @param canvas
	 * @param left
	 * @param top
	 * @param w
	 * @param h
	 * @return
	 */
	public Rectangle getSnugRectangle(exCanvas canvas,int left,int top,int w,int h)
	{	Image im = getImage(canvas.loader());
		if(im!=null) { return(im.getSnugRectangle(left,top,w,h));}
		return(new Rectangle(left,top,w,h));
	}

	/**
	 * draw stock art to fill the width of the rectangle, based on the width of the rectangle
	 * @param gc
	 * @param canvas
	 * @param r
	 * @param label
	 * @param scale
	 */
	public void drawChip(Graphics gc,exCanvas canvas,Rectangle r,String label,double scale)
	{	
		drawChip(gc,canvas,(int)(Math.max(G.Height(r),G.Width(r))*scale),1,G.centerX(r),G.centerY(r),label);
	}

	/**
	 * draw stock art to fill the specified rectangle, return true if it is hit
	 * and set hitpoint with the specified id and tool tip
	 * @param gc
	 * @param canvas
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param sscale  sensitive area scale factor
	 * @param tooltip
	 * @return true if the highlight point was hit
	 */
	public boolean drawChip(Graphics gc,exCanvas canvas,Rectangle r,HitPoint highlight,CellId rackLocation,double sscale,String tooltip)
	{	return drawChip(gc,canvas,r,highlight,rackLocation,sscale,TextChunk.create(tooltip));
	}
	

	
	/**
	 * draw stock art to fill the specified rectangle, return true if it is hit
	 * and set hitpoint with the specified id and tool tip
	 * @param gc
	 * @param canvas
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param sscale  sensitive area scale factor
	 * @param tooltip
	 * @return true if the highlight point was hit
	 */
	public boolean drawChip(Graphics gc,exCanvas canvas,Rectangle r,HitPoint highlight,CellId rackLocation,double sscale,Text tooltip)
	{
		boolean val = findChipHighlight(rackLocation,highlight,G.centerX(r),G.centerY(r),G.Width(r),G.Height(r),sscale);
		drawChip(gc,canvas,r,null);
		if(val)
		{	highlight.setHelpText(tooltip);
			highlight.spriteColor = Color.red;
    		highlight.spriteRect = r;
    		highlight.hitObject = this;
		}
		return(val);
	}
	/**
	 * draw stock art to fill the specified rectangle, return true if it is hit
	 * and set hitpoint with the specified id and tool tip.  Uses the default
	 * sensitive area scale factor of 1.3
	 * @param gc
	 * @param canvas
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param tooltip
	 * @return true if the highlight point was hit
	 */
	public boolean drawChip(Graphics gc,exCanvas canvas,Rectangle r,HitPoint highlight,CellId rackLocation)
	{	return drawChip(gc,canvas,r,highlight,rackLocation,1.3,(Text)null);
	}

	/**
	 * draw stock art to fill the specified rectangle, return true if it is hit
	 * and set hitpoint with the specified id and tool tip.  Uses the default
	 * sensitive area scale factor of 1.3
	 * @param gc
	 * @param canvas
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param tooltip
	 * @return true if the highlight point was hit
	 */
	public boolean drawChip(Graphics gc,exCanvas canvas,Rectangle r,HitPoint highlight,CellId rackLocation,String tooltip)
	{	return drawChip(gc,canvas,r,highlight,rackLocation,1.3,tooltip);
	}

	/**
	 * draw stock art to fill the specified rectangle, return true if it is hit
	 * and set hitpoint with the specified id and tool tip.  Uses the default
	 * sensitive area scale factor of 1.3
	 * @param gc
	 * @param canvas
	 * @param r
	 * @param highlight
	 * @param rackLocation
	 * @param tooltip
	 * @return true if the highlight point was hit
	 */
	public boolean drawChip(Graphics gc,exCanvas canvas,Rectangle r,HitPoint highlight,CellId rackLocation,Text tooltip)
	{	return drawChip(gc,canvas,r,highlight,rackLocation,1.3,tooltip);
	}

	/**
	 * draw a chip and test for mouse sensitivity.  If the highlight is hit, the width is 
	 * increased by 1/3 to give a visual "pop" to indicate the hit.
	 * @param gc			// the graphics to draw with
	 * @param drawOn		// the canvas to do the drawing
	 * @param highlight		// the mouse point, or null.  Receives hit information
	 * @param rackLocation	// the cellId to use for this if hit
	 * @param squareWidth	// the overall scale of the object
	 * @param e_x			// the center of the object
	 * @param e_y			// the center y of the object
	 * @param thislabel		// label to print on top
	 * @return true if this chip is hit
	 */
	public boolean drawChip(Graphics gc,exCanvas drawOn,HitPoint highlight,CellId rackLocation,
			int squareWidth,int e_x,int e_y,String thislabel)
	{
		return(drawChip(gc,drawOn,highlight,rackLocation,squareWidth,e_x,e_y,thislabel,0.66,1.33));
	}
	
	/**
	 * draw a chip and test for mouse sensitivity.  If the highlight is hit, the width is 
	 * increased by 1/3 to give a visual "pop" to indicate the hit.
	 * @param gc			// the graphics to draw with
	 * @param drawOn		// the canvas to do the drawing
	 * @param highlight		// the mouse point, or null.  Receives hit information
	 * @param rackLocation	// the cellId to use for this if hit
	 * @param tooltip		// the tool tip to show
	 * @param squareWidth	// the overall scale of the object
	 * @param e_x			// the center of the object
	 * @param e_y			// the center y of the object
	 * @param thislabel		// label to print on top
	 * @return true if this chip is hit
	 */
	public boolean drawChip(Graphics gc,exCanvas drawOn,HitPoint highlight,CellId rackLocation,String toolTip,
			int squareWidth,int e_x,int e_y,String thislabel)
	{
		if(drawChip(gc,drawOn,highlight,rackLocation,squareWidth,e_x,e_y,thislabel,0.66,1.33))
		{
			highlight.setHelpText(toolTip);
			return(true);
		}
		return false;
	}
	
	/**
	 * draw a chip and test for mouse sensitivity.  If the highlight is hit, the width is 
	 * multiplied by "expansion" to give a visual "pop" to indicate the hit.
	 * @param gc			// the graphics to draw with
	 * @param drawOn		// the canvas to do the drawing
	 * @param highlight		// the mouse point, or null.  Receives hit information
	 * @param rackLocation	// the cellId to use for this if hit
	 * @param squareWidth	// the overall scale of the object
	 * @param e_x			// the center of the object
	 * @param e_y			// the center y of the object
	 * @param thislabel		// label to print on top
	 * @param sscale		// sensitive area scale factor, default 0.66
	 * @param expansion		// 	size expansion factor
	 * @return true if this chip is hit
	 */
	public boolean drawChip(Graphics gc,exCanvas drawOn,HitPoint highlight,CellId rackLocation,
							int squareWidth,int e_x,int e_y,String thislabel,double sscale,double expansion)
	{ 
      double aspect = getAspectRatio(drawOn);
      boolean val = findChipHighlight(rackLocation,highlight,e_x,e_y,(int)(squareWidth*aspect),squareWidth,sscale);
      drawChip(gc,drawOn,(int)(val?expansion*squareWidth:squareWidth),1.0,e_x,e_y,thislabel);
      return(val);
 	}	


	/**
	 * get the rectangle containing this chip, as drawn with the specified width location.
	 * This takes into account the x,y,size scale of the chip and the actual aspect ratio
	 * of the chip.
	 * @param drawOn
	 * @param width
	 * @param x
	 * @param y
	 * @return the containing rectangle
	 */
	public Rectangle getChipRectangle(exCanvas drawOn,int width,int x,int y)
	{	double aspect = getAspectRatio(drawOn);
		int squareWidth = (int)(scale[2]*width);
		int squareHeight = (int)(scale[2]*width/aspect);
		int dx = x-(int)(scale[0]*squareWidth);
		int dy = y-(int)(scale[1]*squareHeight);
		return(new Rectangle(dx,dy,squareWidth,squareHeight));
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
    public boolean findChipHighlight(CellId rackLocation,HitPoint highlight,int e_x,int e_y,int squareWidth,int squareHeight)
    {
    	return findChipHighlight(rackLocation,highlight,e_x,e_y,squareWidth,squareHeight,0.66);
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
	    public boolean findChipHighlight(CellId rackLocation,HitPoint highlight,int e_x,int e_y,int squareWidth,int squareHeight,double sscale)
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
	    public boolean pointInsideCell(HitPoint pt,int x,int y,int SQW,int SQH)
	    {	return(pointInsideCell(pt, x, y, SQW,SQH,0.66));
	    }
	    
	    /**
	     * this method is made visible so you can tweak the radius that constitutes a hit.
	     * Note that the target is a smaller square, not a circle.
	     * @param pt the mouse location
	     * @param x  x position of the chip
	     * @param y  y position of the chip
	     * @param SQW square cell width of the chip
	     * @param SQH is the cell height of the chip
	     * @param sscale the scale applied to width and height
	     * @return true of point is inside the square centered at x,y
	     * @see HitPoint#drawChipHighlight drawChipHighlight
	     * @see online.game.chipCell#drawChip drawChip
	     * @see online.game.stackCell#drawStack drawStack
	     */
	    public boolean pointInsideCell(HitPoint pt,int x,int y,int SQW,int SQH,double sscale)
	    {	return(G.pointNearCenter(pt, x, y, (int)(SQW*sscale*0.5),(int)(SQH*sscale*0.5)));
	    }
	    
	    /**
	     * register a hit on this cell.  
	     * @param highlight
	     * @param e_x
	     * @param e_y
	     * @return true if this is the hit object (even if not new)
	     */
	    public boolean registerChipHit(CellId rackLocation,HitPoint highlight,int e_x,int e_y,int e_w,int e_h)
	    	{
      	  highlight.hitObject = this; 
      	  highlight.hit_x = e_x;
      	  highlight.hit_y = e_y;
      	  highlight.hit_width = e_w;
      	  highlight.hit_height = e_h;
      	  highlight.hitCode = rackLocation;
      	  return(true);
	    }
	   
/**
 * load a stack of drawables and return true if successful.	    
 * @param forcan the canvas to do the loading
 * @param dir the directory (or resource path) to load from 
 * @param chips the chips ready to be loaded
 * @return true if loaded successfully
 */
public static boolean load_masked_images(ImageLoader forcan, String dir,DrawableImage<?>[]chips)
{	return(load_images(forcan,dir,chips,null));
}
/**
 * load an array of drawables and return true if successful.	    
 * @param forcan the canvas to do the loading
 * @param dir the directory (or resource path) to load from 
 * @param chips the chips ready to be loaded
 * @param mask a single mask image for all the images in the list
 * @return true if the images were successfully loaded
 */
public static boolean load_images(ImageLoader forcan, String dir,DrawableImage<?>[]chips,Image mask)
{	String names[] = new String[chips.length];
	for(int i=0;i<names.length;i++)
	{ if(chips[i].image==null) 
		{ names[i]=chips[i].file; 
		}
	}
	Image images[] = mask==null 
				? forcan.load_masked_images(dir,names)
				: forcan.load_images(dir,names,mask);
	for(int i=0;i<names.length;i++)
	{if(chips[i].image==null) { chips[i].image=images[i]; }
	}   
	return(true);
}
/**
 * this is a debugging hack to display all the images, so you can see 
 * what's missing and what's being dynamically loaded	    
 * @param gc
 * @param can
 * @param allChips
 * @param r
 */
public static void showGrid(Graphics gc,exCanvas can,HitPoint hp,Drawable allChips[],Rectangle r)
{
int sz = allChips.length;
int w = G.Width(r);
int h = G.Height(r);
int cell = (int)Math.sqrt(w*h/sz);
int ypos = G.Top(r)+cell/2;
int xpos = G.Left(r)+cell/2;
int xpos0 = xpos;
GC.fillRect(gc, Color.lightGray,r);
try { 
	Image.setChangeDates(false);	// keep the images from being "used" by this display
	for(int i=0;i<sz;i++)
	{ 	int dsize = Math.min(Math.max(w, h),cell);
		Drawable im = allChips[i];
		String name = im.getName();
		boolean scaled = name.indexOf("{scaled}")>=0;
		if(scaled)
		{	// box the scaled images so we can distinguish the cache from the originals
			dsize=2*dsize/3;
			GC.frameRect(gc, Color.blue,xpos-dsize/2-2,ypos-dsize/2-2,dsize+4,dsize+4);
		}
		im.drawChip(gc, can, dsize, xpos, ypos, null);
		HitPoint.setHelpText(hp, dsize,xpos,ypos,im.getName()+" "+im.getWidth()+"x"+im.getHeight());
		xpos += cell;
		if((xpos+cell/2)>(xpos0+w)) { xpos = xpos0; ypos+=cell; }
	}
	}
	finally {
		Image.setChangeDates(true);
	}
}

}
