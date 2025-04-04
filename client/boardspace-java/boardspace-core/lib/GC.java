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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import bridge.MasterForm;
import bridge.Polygon;

public class GC {

	public static Rectangle getClipBounds(Graphics gc)
	{
		return(gc!=null ? gc.getClipBounds() : null);
	}
	
	public static Shape getClip(Graphics gc)
	{
		return(gc!=null ? gc.getClip() : null);
	}

	/** 
	 * 
	 * @param gc the current gc or null
	 * @param sh the next clipping region or null
	 * normally sh would be the old value received from combinedClip
	 */
	public static Rectangle setClip(Graphics gc,Shape sh)
	{	Rectangle val = null;
		if(gc!=null) 
			{ val = gc.setClip(sh); 
			}
		return(val);
	}


	/**
	     * set the clipping rectangle of the graphics to include one rectangle
	     * and exclude the other.  This is used to create the cutout clipping
	     * region for the chat window in a way that is compatible with java 1.1
	     * 
	     * @param gc
	     * @param include
	     * @param exclude
	     */
	    public static void setClip(Graphics gc,Rectangle include,Rectangle exclude)
	    {  
	       if(gc!=null) { gc.setClip(include,exclude); }
	    }
	    
	/**
	 * set the color if gc is not null
	 * @param gc
	 * @param c
	 */
	static public void setColor(Graphics gc,Color c)
	{	if(gc!=null) { gc.setColor(c); }
	}

	public static Color getColor(Graphics inG) 
	{ if(inG!=null) { return(inG.getColor()); } else { return(null); }
	}

	static public void fillOval(Graphics gc,int x,int y,int w,int h)
	{
		if(gc!=null) { gc.fillOval(x,y, w, h); }
	}

	static public void frameOval(Graphics gc,int x,int y,int w,int h)
	{
		if(gc!=null) { gc.frameOval(x,y, w, h); }
	}

	/**
	 * draw the msg string with the character baseline at x,y
	 * 
	 * @param inG
	 * @param msg
	 * @param x
	 * @param y
	 */
	static public void Text(Graphics inG,String msg,int x,int y)
	{
		if(inG!=null) { inG.Text(msg,x,y); }
	}

	/**
	    * set the font if gc is not null
	    * @param gc
	    * @param f
	    */
	   static public void setFont(Graphics gc,Font f)
	   {	if(gc!=null && (f!=null)) { gc.setFont(f); }
	   }

	static public FontMetrics getFontMetrics(Graphics inG)
	   {   if(inG!=null) { return(inG.getFontMetrics()); }
	   	   return(null);
	   }

	static public boolean canRepaintLocally(Graphics g) { return(MasterForm.canRepaintLocally(g)); }

	/**
	 * get the current x translation of the graphics
	 * 
	 * @param g
	 * @return
	 */
	public static int getTranslateX(Graphics g0)
	{	return(g0!=null ? g0.getTranslateX() : 0);
	}

	/**
	 * get the current y translation of the graphics
	 * 
	 * @param g
	 * @return
	 */
	public static int getTranslateY(Graphics g0)
	{	return(g0!=null ? g0.getTranslateY() : 0);
	}

	public static void drawFatLine(Graphics g1,int fx,int fy,int tx,int ty,double strokeWidth)
	{
		if(g1!=null)
			{
			g1.drawFatLine(fx,fy,tx,ty,strokeWidth);
			}
	}
	/**
	 * draw an arrow from ox,oy to dest_x,dest_y, with an arrowhead at the destination with length "ticksize"
	 * using line thickness specified (rounded up to 1).
	 * @param g1
	 * @param ox
	 * @param oy
	 * @param dest_x
	 * @param dest_y
	 * @param ticksize
	 * @param thickness
	 */
	static public void drawArrow(Graphics g1, int ox, int oy, int dest_x,  int dest_y, int ticksize,double thickness)
	{	if(g1!=null)
		{
		g1.drawArrow(ox, oy, dest_x, dest_y, ticksize, thickness);
		}
	}
	/**
	 * set the current rotation, in radians
	 * @param g
	 * @param ang
	 */
	public static void setRotation(Graphics g,double ang)
	{	if((g!=null) && (ang!=0))
		{
			g.setRotation(ang);
		}
	}
	/**
	 * set the current rotation, in radians, centered on x, y
	 * @param g
	 * @param ang
	 * @param cx
	 * @param cy
	 */
	public static void setRotation(Graphics g,double ang,int cx,int cy)
	{	if((g!=null) && (ang!=0))
		{
			g.setRotation(ang,cx,cy);
		}
	}
	/**
	 * get the current rotation in radians
	 * 
	 * @param g
	 * @return
	 */
	public static double getRotation(Graphics g)
	{
		return g==null ? 0 : g.getRotation();
	}
	/**
	 * reset the matrix transformation to an identity matrix
	 * @param g
	 */
	public static void resetAffine(Graphics g)
	{	if(g!=null)
		{
		g.resetAffine();
		}
	}

	/**
	 * fill a rectangle with the current color
	 * 
	 * @param g
	 * @param left
	 * @param top
	 * @param w
	 * @param h
	 */
	static public void fillRect(Graphics g, int left, int top, int w,  int h)
	    {	if(g!=null)
	    	{
	        g.fillRect(left, top, w, h);
	    	}
	    }
	/**
	 * get the bounds of a specified substring, which might span multiple lines
	 * @param g
	 * @param fm
	 * @param line
	 * @param firstChar
	 * @param lastChar
	 * @return
	 */
	public static Rectangle getStringBounds(Graphics g, FontMetrics fm, String line, int firstChar, int lastChar)
	{
		Rectangle2D d = Graphics.getStringBounds(g, fm, line, firstChar, lastChar);
		return(new Rectangle((int)d.getX(),(int)d.getY(),(int)d.getWidth(),(int)d.getHeight()));
	}
/**
 * get the bounds of a string, which might span multiple lines
 * @param g
 * @param fm
 * @param line
 * @return
 */
	public static Rectangle getStringBounds(Graphics g, FontMetrics fm, String line) {
		Rectangle2D d = Graphics.getStringBounds(g, fm, line);
		return(new Rectangle((int)d.getX(),(int)d.getY(),(int)d.getWidth(),(int)d.getHeight()));
	}

	/** draw a tooltip bubble with the pointer pointing to x,y, constrained
	 * to appear in rectangle R.  Text may be multiple lines separated by \n  The
	 * position of the pointer is adjusted appropriately if the x,y is too close
	 * to the edges of the rectangle.
	 * should be brief.
	 * @param inG	the graphics to draw
	 * @param x		the x of the focus of attention
	 * @param y		the y of the focus of attention
	 * @param msg   the text to be displayed
	 * @param r		the bounding rectangle
	 * @param rot	the rotation angle to display the box
	 */
	static public void drawBubble(Graphics inG, int x, int y, String msg, Rectangle r,double rotation)
	{ drawBubble(inG,x,y,TextChunk.split(msg),r,rotation);
	}

	/** draw a tooltip bubble with the pointer pointing to x,y, constrained
	 * to appear in rectangle R.  Text may be multiple lines separated by \n  The
	 * position of the pointer is adjusted appropriately if the x,y is too close
	 * to the edges of the rectangle.
	 * should be brief.
	 * @param inG	the graphics to draw
	 * @param x		the x of the focus of attention
	 * @param y		the y of the focus of attention
	 * @param msg   the text to be displayed
	 * @param r		the bounding rectangle
	 * @param rotation	the rotation angle to display the box
	 */
	static public void drawBubble(Graphics inG, int x, int y, Text msg, Rectangle r,double rotation)
	{ if(inG!=null)
	  {
		inG.drawBubble(x, y, msg, r, rotation);
	}}

	/** draw a text string, resized downward if necessary to fit the box.
	 *
	 * @param inG the graphics to draw
	 * @param center if true, center the text
	 * @param R a rectangle
	 * @param color foreground color (or null)
	 * @param bg background color (or null)
	 * @param str the string to draw
	 * @return the text width (only if inG is not null)
	 */
	static public int Text(Graphics inG, boolean center, Rectangle R,
	    Color color, Color bg, String str)
	{
	    return (Text(inG, center, G.Left(R), G.Top(R), G.Width(R), G.Height(R), color, bg, str));
	}

	/** draw a text string, resized downward if necessary to fit the box.
	*
	* @param inG the graphics to draw
	* @param rotation rotated angle for the text
	* @param center if true, center the text
	* @param R a rectangle
	* @param color foreground color (or null)
	* @param bg background color (or null)
	* @param str the string to draw
	* @return the text width(only if inG is not null)
	*/
	static public int Text(Graphics inG, double rotation,boolean center, Rectangle R,
	        Color color, Color bg, String str)
	    {	int cx = G.centerX(R);
	    	int cy = G.centerY(R);
	    	setRotation(inG, rotation, cx, cy);
	        int v = Text(inG, center, G.Left(R), G.Top(R), G.Width(R), G.Height(R), color, bg, str);
	        setRotation(inG,-rotation,cx, cy);
	        return(v);
	    }

	/**
	 * draw a text string, resized downward if necessary to fit in the box
	 * the string may be multiple lines separated by \n
	 * @param inG
	 * @param center
	 * @param R
	 * @param voff
	 * @param color
	 * @param bg
	 * @param str
	 * @return the text width (only if inG is not null)
	 */
	static public int Text(Graphics inG, boolean center, Rectangle R,int voff,
	        Color color, Color bg, String str)
	    {
	        return (Text(inG, center, G.Left(R), G.Top(R)+voff, G.Width(R), G.Height(R), color, bg, str));
	    }

	/**
	 * draw a text string, resized downward if necessary to fit in the box
	 * the string may be multiple lines separated by \n
	 * @param inG
	 * @param center
	 * @param R
	 * @param voff
	 * @param color
	 * @param bg
	 * @param str
	 * @return the text width (only if inG is not null)
	 */
	static public int Text(Graphics inG, boolean center, Rectangle R,int voff,
	        Color color, Color bg, Text str)
	    {
	        return (Text(inG, center, G.Left(R), G.Top(R)+voff, G.Width(R), G.Height(R), color, bg, str));
	    }

	/** draw a text string, resized downward if necessary to fit the box.
	 * the string may be multiple lines separated by \n
	 * if width<0, center the text at xpos without resizing
	 * if height=0, center the text at ypos
	 *
	 * @param inG the graphics to draw
	 * @param center if true, center the text
	 * @param inX x location of the left edge
	 * @param inY y location of the upper-left corner
	 * @param inWidth box width
	 * @param inHeight box height
	 * @param inColour foreground color (or null)
	 * @param bgColor background color (or null)
	 * @param inStr the string to draw
	 * @return the text width
	 */
	static public int Text(Graphics inG, boolean center, int inX, int inY,
	    int inWidth, int inHeight, Color inColour, Color bgColor, String inStr)
	{  	return(TextChunk.split(inStr).draw(inG,center,inX,inY,inWidth,inHeight,inColour,bgColor));
	}
/**
 * Draw right justified text
 * @param inG
 * @param inX
 * @param inY
 * @param inWidth
 * @param inHeight
 * @param inColour
 * @param bgColor
 * @param inStr
 * @return
 */
	static public int TextRight(Graphics inG, int inX, int inY,
		    int inWidth, int inHeight, Color inColour, Color bgColor, String inStr)
		{  	return(TextChunk.split(inStr).drawRight(inG,inX,inY,inWidth,inHeight,inColour,bgColor));
		}
	/**
	 * Draw right justified text
	 * 
	 * @param inG
	 * @param r
	 * @param inColour
	 * @param bgColor
	 * @param inStr
	 * @return
	 */
	static public int TextRight(Graphics inG, Rectangle r, Color inColour, Color bgColor, String inStr)
		{  	return(TextChunk.split(inStr).drawRight(inG,r,inColour,bgColor));
		}

	/**
	 * draw outlined text.  This is done by drawing the text multiple times using
	 * the outline color, then finally using the specified main color.
	 * 
	 * @param gc the graphics
	 * @param center if true, center in the box
	 * @param ax the x for the box left
	 * @param ay the y for the box top
	 * @param inWidth box width
	 * @param inHeight box height
	 * @param inColour the main color for the text
	 * @param outlineColor the outline color for the text
	 * @param inStr the text to draw
	 * @return the width of the text actually drawn
	 */
	static public int drawOutlinedText(Graphics gc, boolean center, int ax, int ay,
	        int inWidth, int inHeight, Color inColour, Color outlineColor, String inStr)
	{
		Text(gc,center,ax-1,ay-1,inWidth,inHeight,outlineColor,null,inStr);
	    Text(gc,center,ax+1,ay+1,inWidth,inHeight,outlineColor,null,inStr);
	    Text(gc,center,ax+1,ay-1,inWidth,inHeight,outlineColor,null,inStr);
	    Text(gc,center,ax-1,ay+1,inWidth,inHeight,outlineColor,null,inStr);
	    return 2+Text(gc,center,ax,ay,inWidth,inHeight,inColour,null,inStr);
	}

	/** draw a text string, resized downward if necessary to fit the box.
	 * the string may be multiple lines separated by \n
	 * if width<0, center the text at xpos without resizing
	 * if height=0, center the text at ypos
	 *
	 * @param inG the graphics to draw
	 * @param center if true, center the text
	 * @param inX x location of the left edge
	 * @param inY y location of the upper-left corner
	 * @param inWidth box width
	 * @param inHeight box height
	 * @param inColour foreground color (or null)
	 * @param bgColor background color (or null)
	 * @param inStr the string to draw
	 * @return the text width
	 */
	static public int Text(Graphics inG, boolean center, int inX, int inY,
	        int inWidth, int inHeight, Color inColour, Color bgColor, Text inStr)
	    {  return(inStr.draw(inG,center,inX,inY,inWidth,inHeight,inColour,bgColor));
	    }

	/** print the elapsed time in a box
	 *
	 * @param gc
	 * @param dest
	 * @param timeString
	 * @param foreground_color
	 * @param table_color
	 * @param standardBoldFont
	 */
	static public void printTimeC(Graphics gc, Rectangle dest,
	    String timeString, Color foreground_color, Color table_color, Font standardBoldFont)
	{
	    int inXOffset = G.Left(dest);
	    int inYOffset = G.Top(dest);
	    int inW = G.Width(dest);
	    int inH = G.Height(dest);
	
	    Font old = getFont(gc);
	    setFont(gc,standardBoldFont);
	    Text(gc, false, inXOffset + 3, inYOffset, inW, inH, foreground_color,
	        table_color, timeString);
	    setFont(gc,old);
	}

	/**
	     * 
	     * @param g
	     * @param left
	     * @param top
	     * @param width
	     * @param height
	     * @param rx
	     * @param ry
	     */
	   static public void fillRoundRect(Graphics g,int left,int top,int width,int height,int rx,int ry)
	   {
	   	if(g!=null) { g.fillRoundRect(left,top,width,height,rx,ry); }
	   }

	/**
	    * 
	    * @param g
	    * @param left
	    * @param top
	    * @param width
	    * @param height
	    * @param rx
	    * @param ry
	    */
	   static public void frameRoundRect(Graphics g,int left,int top,int width,int height,int rx,int ry)
	   {
	   	if(g!=null) { g.drawRoundRect(left,top,width,height,rx,ry); }
	   }
	    private static double intensity(int x, int y, double radius)
	    {
	        return (Math.sqrt(Math.min(1.0,
	                0.6 * (radius - (Math.sqrt((x * x) + (y * y)))))));
	    }

	/**
	 * fill a rectangle with a rectangle
	 * @param g a graphics object or null
	 * @param r a rectangle
	 */
	    static public void fillRect(Graphics g, Rectangle r)
	    {
	        if(g!=null) { g.fillRect(G.Left(r), G.Top(r), G.Width(r), G.Height(r)); }
	    }

	static void drawAASymline(Graphics inG, int fromX, int fromY,
	    int centerX, int centerY, double radius, Color fore, Color back,
	    boolean filled)
	{
	    int br = back.getRed();
	    int bg = back.getGreen();
	    int bb = back.getBlue();
	    int fr = fore.getRed();
	    int fg = fore.getGreen();
	    int fb = fore.getBlue();
	    int dr = fr - br;
	    int dg = fg - bg;
	    int db = fb - bb;
	    int x = -fromX;
	    int y = -fromY;
	
	    while (x <= 0)
	    {
	        double density = 1.0 - intensity(x, y, radius);
	        int newr = Math.min(255, (int) (fr - (density * dr)));
	        int newg = Math.min(255, (int) (fg - (density * dg)));
	        int newb = Math.min(255, (int) (fb - (density * db)));
	        setColor(inG,new Color(newr, newg, newb));
	        frameRect(inG,centerX + x, centerY - fromY, 1, 1);
	        frameRect(inG,centerX - x, centerY - fromY, 1, 1);
	        frameRect(inG,centerX + x, centerY + fromY, 1, 1);
	        frameRect(inG,centerX - x, centerY + fromY, 1, 1);
	
	        if (density < 0.1)
	        {
	            break;
	        }
	
	        x++;
	    }
	
	    setColor(inG,fore);
	    
	    if (filled && (x <= 0))
	    {
	        inG.drawRect(centerX + x, centerY - y, (x * -2) + 1, 1);
	        inG.drawRect(centerX + x, centerY + y, (x * -2) + 1, 1);
	    }
	}

	/** draw a circle with antialiased edges.
	 *
	 * @param inG
	 * @param centerX duh
	 * @param centerY duh
	 * @param radius duh
	 * @param fg main color for the circle
	 * @param bg the color to blend with
	 * @param filled true for a filled circle
	 */
	static public void DrawAACircle(Graphics inG, int centerX, int centerY,
	    int radius, Color fg, Color bg, boolean filled)
	{	if(inG!=null)
		{
	    int x;
	    int y;
	    int d;
	
	    x = 0;
	    y = radius;
	    d = 1 - radius;
	
	    while (y > x)
	    {
	        if (d < 0)
	        { // Select E
	            d += ((x * 2) + 3);
	            x++;
	        }
	        else
	        {
	            d += (((x - y) * 2) + 5); // Select SE
	            x++;
	            y--;
	        }
	
	        drawAASymline(inG, x, y, centerX, centerY, radius + 0.55, fg, bg,
	            filled);
	        drawAASymline(inG, y, x, centerX, centerY, radius + 0.55, fg, bg,
	            filled);
	    }
	    if(filled) { fillRect(inG,centerX-radius+1,centerY-1,radius*2-1,3); }
		}
	}

	// codename1 breaks when lots of calls to drawAAcircle are done
	// see issue #3388
	static CachedObject<Image>circleCache = null;

	/** draw an anti aliased circle from a small cache.  Use this for small
	 * decorator circles that are drawn many times the same size and color
	 * @param g graphics object
	 * @param x
	 * @param y
	 * @param radius
	 * @param color
	 * @param bgcolor
	 * @param filled
	 * @return
	 */
	static public void cacheAACircle(Graphics g,int X,int Y,int radius,Color color,Color bgcolor,boolean filled)
	{	if(circleCache==null) { circleCache = new CachedObject<Image>(20); }
		Image found = circleCache.find(radius,color,bgcolor,filled);
		if(found == null)
			{
	    	Image im = Image.createTransparentImage(radius*2+4,radius*2+4);
	    	Graphics gr = im.getGraphics();
	    	DrawAACircle(gr,radius+2,radius+2,radius,color,bgcolor,filled);
	    	circleCache.add(new CachedObject<Image>(im,radius,color,bgcolor,filled));
	    	found = im;
			}
	found.drawImage(g,X-radius-2,Y-radius-2);    	
	}

	/**
	 * draw a pseudo 3d rectangle in black white and foreground/background colors
	 * @param g
	 * @param inX
	 * @param inY
	 * @param inW
	 * @param inH
	 * @param background
	 * @param foreground
	 */
	static public void Draw3DRect(Graphics g, int inX, int inY, int inW, int inH,Color background,Color foreground)
	{	if(g!=null) { g.draw3DRect( inX, inY, inW, inH, background, foreground); }
    }


	/**
	 * frame a rectangle with specified color
	 * @param g a graphics object or null
	 * @param c a color
	 * @param left
	 * @param top
	 * @param w
	 * @param h
	 */
	    static public void frameRect(Graphics g, Color c, int left, int top, int w,
	        int h)
	    {	
	        setColor(g,c);
	        frameRect(g,left, top, w-1 , h-1 );
	    }

	/**
	 * 
	 * @param g
	 * @param left
	 * @param top
	 * @param w
	 * @param h
	 */
	static public void frameRect(Graphics g, int left, int top, int w, int h)
	    {	if(g!=null)
	    	{
	        g.drawRect(left, top, w , h );
	    	}
	    }

	/**
	 * frame a rectangle with a color
	 * @param g a graphics or null
	 * @param c a color
	 * @param r
	 */
	    static public void frameRect(Graphics g, Color c, Rectangle r)
	    {	
	        setColor(g,c);
	        frameRect(g,G.Left(r), G.Top(r), G.Width(r), G.Height(r));
	    }

	/**
	 * handle a round button, draw a highlight if the mouse is over it. 
	 * If width<height, text is rotated by pi/2 or -pi/2
	 * @param gc
	 * @param r
	 * @param highlight
	 * @param msg
	 * @param HighlightColor
	 * @param rackBackGroundColor
	 * @return true of this button was hit
	 */
	    static public boolean handleRoundButton(Graphics gc, Rectangle r,
	        HitPoint highlight, String msg, Color HighlightColor,
	        Color rackBackGroundColor)
	    {	return handleRoundButton(gc,0,r,highlight,
	    		TextChunk.create(msg),Color.black,
	    		Color.black,HighlightColor,rackBackGroundColor);
	    }

	/**
	 * 
	 *  draw a button with text, light it up if it's over the mouse point "p"
	 * if it is lighted, note it as the hitObject
	 * If width<height, text is rotated by pi/2 or -pi/2
	 *
	* @param inG
	 * @param r
	 * @param highlight
	 * @param text
	 * @param HighlightColor
	 * @param BackgroundColor
	 * @return true of this button was hit
	 */
	 static public boolean handleSquareButton(Graphics inG, Rectangle r,
	    HitPoint highlight, String text, Color HighlightColor,  Color BackgroundColor)
	{
		return(handleSquareButton(inG,0,r,highlight,TextChunk.create(text),Color.black,Color.white,HighlightColor,BackgroundColor));
	}

	/**
	 *  draw a button with text, light it up if it's over the mouse point "p"
	 * if it is lighted, note it as the hitObject
	 * 
	 * @param inG
	 * @param r
	 * @param highlight
	 * @param text
	 * @param HighlightColor
	 * @param BackgroundColor
	 * @return true if the button was hit
	 */
	 static public boolean handleSquareButton(Graphics inG, Rectangle r,
	    	        HitPoint highlight, Text text, Color textColor, Color frameColor, Color HighlightColor,  Color BackgroundColor)
	     {
	        boolean inbutton = (highlight != null) &&
	            r.contains(G.Left(highlight), G.Top(highlight));
	
	        if (inG != null)
	        {	inG.drawTextButton(r,text,textColor,frameColor,
	        		inbutton&&highlight.upCode!=MouseState.LAST_IS_IDLE ? HighlightColor : BackgroundColor);
	        }
	
	        return (inbutton);
	    }

	/**
	 * draw a button with text, light it up if it's over the mouse point "p"
	 * if it is lighted, note it as the hitObject
	 * @param inG
	 * @param rotation
	 * @param r
	 * @param highlight
	 * @param text
	 * @param HighlightColor
	 * @param BackgroundColor
	 * @return true if the button was hit
	 */
	     static public boolean handleRoundButton(Graphics inG,double rotation,Rectangle r,
	    		 HitPoint highlight, String text, Color HighlightColor, Color BackgroundColor)
	     {
	    	 return(handleRoundButton(inG,rotation,r,highlight,
	    			 TextChunk.create(text),Color.black,
	    			 Color.black,HighlightColor,BackgroundColor));
	     }

	/**
	 *  * draw a button with text, light it up if it's over the mouse point "p"
	 * if it is lighted, note it as the hitObject
	 * @param inG
	 * @param rotation
	 * @param r
	 * @param highlight
	 * @param text
	 * @param HighlightColor
	 * @param BackgroundColor
	 * @return true if the button was hit
	 */
	  static public boolean handleSquareButton(Graphics inG,double rotation,Rectangle r,
	    		 HitPoint highlight, String text, Color HighlightColor, Color BackgroundColor)
	     {
	    	 return(handleSquareButton(inG,rotation,r,highlight,TextChunk.create(text),
	    			 Color.black,Color.white,HighlightColor,BackgroundColor));
	     }

	/**
	  * draw a square button, rotating the rectangle and text
	  * @param inG
	  * @param rotation
	  * @param r
	  * @param highlight
	  * @param text
	  * @param HighlightColor
	  * @param BackgroundColor
	  * @return true if the button was hit
	  */
	 static public boolean handleSquareButton(Graphics inG,double rotation,Rectangle r,
			 HitPoint highlight, Text text, Color textColor,Color frameColor,Color HighlightColor, Color BackgroundColor)
	 {	Rectangle cr = r ;
	 	int cx = G.centerX(r);
		int cy = G.centerY(r);
		double rotation2 = rotation+G.autoRotate(r);
		if(rotation2!=0)
	 		{
	 		 setRotation(inG,rotation2, cx, cy);
	 		 cr = G.copy(null,r);
	 		 G.setRotation(cr,rotation2,cx,cy);
	 		 G.setRotation(highlight, rotation2, cx, cy);
	 		}
	 	boolean v = handleSquareButton(inG,cr,highlight,text,textColor,frameColor,HighlightColor,BackgroundColor);
	 	if(rotation2!=0)
	 		{ setRotation(inG, -rotation2, cx, cy); 
	 		  G.setRotation(highlight, -rotation2, cx, cy);
	 		} 
	 	return(v);
	 }

	/**
	  * draw round edged button, rotating the rectangle and text
	  *  
	  * @param inG
	  * @param rotation
	  * @param r rectangle to display in
	  * @param highlight the mouse location
	  * @param text	the contents of the button
	  * @param HighlightColor color for "mouse present"
	  * @param BackgroundColor color for "mouse absent"
	  * @return true of the button was hit
	  */
	 static public boolean handleRoundButton(Graphics inG,double rotation,Rectangle r,
			 HitPoint highlight, Text text,Color textColor,
			 Color frameColor,Color HighlightColor, Color BackgroundColor)
	 {	int cx = G.centerX(r);
	 	int cy = G.centerY(r);
	 	Rectangle cr = r;
	 	if(G.Width(cr)>0 && G.Height(cr)>0)
	 	{
	 	double rotation2 = rotation + G.autoRotate(r);
	 	if(rotation2!=0)
	 	{
	 		cr = G.copy(null,r);
	 		G.setRotation(highlight,rotation2,cx,cy);
	 		G.setRotation(cr,rotation2,cx,cy);
	 		setRotation(inG,rotation2, cx, cy);
	 	}
	 	boolean v = handleRoundButton(inG,cr,-1,highlight,text,textColor,frameColor,
	 					HighlightColor,BackgroundColor);
	 	if(rotation2!=0) 
	 		{ setRotation(inG, -rotation2, cx, cy);   
	 		  G.setRotation(highlight,-rotation2,cx,cy);
	 		}
	 	return(v);
	 	}
	 	return false;
	 }

	/**
	  * draw round edged button, text as specified
	  * @param gc
	  * @param r rectangle to display in
	  * @param highlight the mouse location
	  * @param text	the contents of the button
	  * @param HighlightColor color for "mouse present"
	  * @param BackgroundColor color for "mouse absent"
	  * @return true of the button was hit
	  */
	 static public boolean handleRoundButton(Graphics gc,Rectangle r, int bevel,
			 HitPoint highlight, Text text, Color textColor,
			 Color frameColor, Color HighlightColor, Color BackgroundColor)
	 {
		 boolean inbutton = (highlight != null) && r.contains(G.Left(highlight), G.Top(highlight));
	
	     if (gc != null)
	        {	Color cl = inbutton&&highlight.upCode!=MouseState.LAST_IS_IDLE ? HighlightColor : BackgroundColor;
	        	gc.drawRoundTextButton(r,bevel, text, textColor,frameColor,cl);
	        }
	
	     return (inbutton);
	 }


	/** draw an animated circle with an arrow, to indicate life in the connection
	     * @param gc the graphics object
	     * @param dest the destination rectangle
	     * @param last the tick value for time of this display
	     * @param progress step counter, which becomes the angle of display arrow
	     */
	
	   public static void draw_anim(Graphics gc,Rectangle dest,int size0,long last,int progress)
	    {
	        int inXOffset = G.centerX(dest);
	        int inYOffset = G.centerY(dest);
	        long now = G.Date();
	        int size = size0&~1;		// make size even
	        if (last == 0)
	        {
	            last = now;
	        }
	
	        float dif = (float) Math.min(1.0, (now - last) / (1000 * 60.0));
	
	        if (dif >= 0.0f)
	        {
	            int anim_y = inYOffset;
	            int anim_x = inXOffset;
	            int sz2 = size/2;
	            double pos = ((-2 * Math.PI) / 16) * (progress % 16);
	            double angx = Math.sin(pos);
	            double angy = Math.cos(pos);
	            
	            setColor(gc,Color.blue);
	            Color newColor = new Color(dif, (float) Math.min(1.0, 2.0f - (2 * dif)),0.0f);
	            setColor(gc,newColor);
	            fillOval(gc,anim_x - sz2, anim_y -sz2, size,size);
	            setColor(gc,Color.black);
	            frameOval(gc,anim_x - sz2, anim_y -sz2, size,size);
	            int dx = (int)Math.round((sz2+2) * angx);
	            int dy = (int)Math.round((sz2+2) * angy);
	            drawLine(gc,anim_x-dx,anim_y-dy,
	                anim_x + dx, 
	                anim_y + dy);
	       }
	    }

	/**
	* draw a polygon with a border and fill, and with text drawing in the center
	* 
	* @param inG
	* @param inX
	* @param inY
	* @param inPoly
	* @param fillColour
	* @param trimColour
	* @param inText
	* @param textColour
	*/
	public static void myDrawPolygon(Graphics inG, int inX, int inY, Polygon inPoly, Color fillColour, Color trimColour, String inText, Color textColour) 
	{
	    translate(inG,inX,inY);
	    setColor(inG,fillColour);
	    inPoly.fillPolygon(inG);
	    setColor(inG,trimColour);
	    inPoly.framePolygon(inG);
	    if (inText != null) {
	       Rectangle b = inPoly.getBounds();
	       int w = G.Width(b) - 4;
	       Text(inG,true,-w/2,-G.Height(b)/2,w,G.Height(b),textColour,null,inText);
	    }
	    translate(inG,-inX,-inY);
	  }

	/** combine the current clipping rectangle with a new left-top-w-h.  This
	 * should be used to clip to a subrectangle of the current master rectangle
	 * @param gc	the current gc or null
	 * @param left
	 * @param top
	 * @param w
	 * @param h
	 * @return the original clipping rectangle, or null
	 */
	public static Rectangle combinedClip(Graphics gc,int left,int top,int w,int h)
	{	
		if(gc!=null)
		{ 
			return gc.combinedClip(left,top,w,h);
		}
		return(null);
	}

	/**
	 * combine the current clipping rectangle with the new one. This
	 * should be used to clip to a sub rectangle of the current master rectangle
	 * @param gc	the current gc or null
	 * @param r
	 * @return the original clipping rectangle, or null
	 */
	public static Rectangle combinedClip(Graphics gc,Rectangle r)
	{
		return(combinedClip(gc,G.Left(r),G.Top(r),G.Width(r),G.Height(r)));
	}

	public static Rectangle setClip(Graphics gc,int left,int top,int w,int h)
	{	Rectangle r = getClipBounds(gc);
		// IOS behaves badly with negative width or height
		if(gc!=null) { gc.setClip(left,top,Math.max(0, w),Math.max(0, h)); }
		return(r);
	}

	static public void drawLine(Graphics gc,int xpos,int ypos,int nxpos,int nypos)
	   {
		   if(gc!=null) { gc.drawLine(xpos,ypos,nxpos,nypos); }
	   }

	/**
	 * set the x,y translation of the graphics
	 * 
	 * @param gc
	 * @param x
	 * @param y
	 */
	static public void translate(Graphics gc,int x,int y)
	{	if(gc!=null) { gc.translate(x,y); }
	}

	public static Font getFont(Graphics g) { return((g!=null) ? g.getFont() : null); }

	/**
	 * fill a rectangle centered on x,y with color c
	 * @param f
	 * @param c
	 * @param x
	 * @param y
	 * @param s
	 */
	static public void fillRect(Graphics f,Color c,int x,int y,int s)
	{	setColor(f,c);
		fillRect(f,x-s/2,y-s/2,s,s);
	}

	/**
	 * fill a rectangle with a specified color
	 * @param g
	 * @param c
	 * @param r
	 */
	static public void fillRect(Graphics g, Color c,Rectangle r)
	{	setColor(g,c);
	    fillRect(g,G.Left(r), G.Top(r), G.Width(r), G.Height(r)); 
	}

	/**
	 * fill a rectangle with a color
	 * @param g
	 * @param c
	 * @param left
	 * @param top
	 * @param width
	 * @param height
	 */
	static public void fillRect(Graphics g,Color c,int left,int top,int width,int height)
	{
		setColor(g,c);
		fillRect(g,left,top,width,height);
	}

	/*
	 * rotate all objects being drawn inside a rectangle, nominally a board rectangle.
	 * the hitpoint "select" is also rotated in the board centered coordinate system,
	 * and cells drawn in the new context will have their current_center_x and current_center_y
	 * rotated in to true coordinates.  The next effect will be that after leaving the board-drawing
	 * context, residual drawing such as sprites and animations will only see true x,y coordinates.
	 */
	public static void setRotatedContext(Graphics gc,Rectangle rect,HitPoint select,double rotation)
	{	if(rotation!=0)
		{
		int cx = G.centerX(rect);
		int cy = G.centerY(rect);
		setRotatedContext(gc,cx,cy,select,rotation);
		}
	}

	/**
	 * rotate items to be painted near the mouse.  This is done in
	 * a context where "select" is an unrotated point in x,y but the
	 * item being drawn should be rotated.
	 *
	 * @param gc
	 * @param cx
	 * @param cy
	 * @param select
	 * @param rotation
	 */
	public static void setRotatedContext(Graphics gc,int cx,int cy,HitPoint select,double rotation)
	{	if(rotation!=0)
		{
		if(gc!=null)
		{
			gc.setRotatedContext(cx,cy,select,rotation);
		}
		else if(select!=null)
		{
			select.setRotatedContext(rotation,cx,cy);
		}}
	}

	/**
	 * rotate items to be painted near the mouse.  This is done in
	 * a context where "select" is an unrotated point in x,y but the
	 * item being drawn should be rotated.
	 *
	 * @param gc
	 * @param select
	 * @param rotation
	 */
	public static void setRotatedContext(Graphics gc,HitPoint select,double rotation)
	{	if(rotation!=0)
		{
		if(gc!=null && select!=null)
		{
			gc.setRotatedContext(select, rotation);
		}
		else if(select!=null)
		{
			select.setRotatedContext(rotation);
		}}
	}

	/**
	 * reverse the effect of setRotatedContext
	 * @param gc
	 * @param select
	 */
	public static void unsetRotatedContext(Graphics gc,HitPoint select)
	{	if(gc!=null) { gc.unsetRotatedContext(); }
		else if(select!=null) { select.unsetRotatedContext(); }
	}
	
	public static void drawButton(Graphics inG, int xoffset, int yoffset, int width, int height,
			Color vcrbuttoncolor) {
		if(inG!=null) { inG.drawButton(xoffset,yoffset,width,height,Color.white,vcrbuttoncolor); }

	}

	public static void setOpacity(Graphics gc,double op)
	{	if(gc!=null) { gc.setOpactity(op); }
	}
	 /**
	  * return true if no part of the rectangle is visible.  This is intended to be used
	  * when drawing images, to determine if the image will be visible before any loading,
	  * reloading, or elaborate scaling is invoked.	 If the gc is null, all rectangles are invisible.
	  * @param x
	  * @param y
	  * @param w
	  * @param h
	  * @return
	  */
	 public static boolean isNotVisible(Graphics gc,int x,int y,int w,int h)
	 {	if(gc==null) { return false; }
	 	return gc.isNotVisible(x,y,w,h);
	 }
}
