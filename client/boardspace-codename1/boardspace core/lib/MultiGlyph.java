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

/**
 * a container to contain multiple glyphs which implement {@link lib.Drawable}, and draw them as one.
 * These are used in menus and in {@link lib.Text} objects to present compound icons.  
 * @author Ddyer
 *
 */
public class MultiGlyph implements Drawable {
	
	public void rotateCurrentCenter(double amount,int x,int y,int cx,int cy) {};
 	public double activeAnimationRotation() { return(0); }
	class SingleGlyph
	{	Drawable drawable;
		double scl[] = {1.0,0,0};
		SingleGlyph next = null;
		SingleGlyph(Drawable d,double sc[]) { drawable = d; if(sc!=null) { scl = sc;}} 
		
		public void draw(Graphics gc, DrawingObject c, int size, int posx, int posy,	String msg) 
		{
			drawable.draw(gc,c,
					(int)(size*scl[0]),
					(int)(posx+size*scl[1]),
					(int)(posy+size*scl[2]),
					null);
		}
	}
	
	SingleGlyph drawable = null;
	public String getName() { return(toString()); }
	/** default constructor */
	static public MultiGlyph create()
	{	MultiGlyph m = new MultiGlyph();
		return(m);
	}
	public int getWidth() { return(0); }
	public int getHeight() { return(0); }
	/**
	 * append a drawable object to be drawn before this object
	 * @param d the drawable object
	 * @param scl the scale for the prepended object, { x , y, scale }
	 */
	public void append(Drawable d,double scl[])
	{	SingleGlyph g = new SingleGlyph(d,scl);
		if(drawable==null) { drawable = g; }
		else { SingleGlyph s = drawable;  
				while(s.next!=null) { s = s.next; }
				s.next = g;
		}
	}
	/**
	 * prepend a drawable object to be drawn before this object
	 * @param d the drawable object
	 * @param scl the scale for the prepended object, { x , y, scale }
	 */
	public void prepend(Drawable d,double scl[])
	{	SingleGlyph g = new SingleGlyph(d,scl);
		g.next = drawable;
		drawable = g;
	}

	/*
	 * (non-Javadoc)
	 * @see lib.Drawable#drawChip(Graphics, exCanvas, int, int, int, java.lang.String)
	 */
	public void draw(Graphics gc, DrawingObject c, int size, int posx, int posy,	String msg) 
	{
		if(drawable!=null)
		{	
			SingleGlyph g = drawable;
			while(g!=null)
			{	g.draw(gc,c,size,posx,posy,msg);
				g = g.next;
			}
		}
	}
/*
 * (non-Javadoc)
 * @see lib.Drawable#animationHeight()
 */
	public int animationHeight() {
		int h = 0;
		SingleGlyph g = drawable;
		while(g!=null) {  h = Math.max(h,g.drawable.animationHeight()); g = g.next; }
		return(h);
	}

}
