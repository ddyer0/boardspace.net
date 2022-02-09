package lib;

import online.common.exCanvas;
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
		
		public void draw(Graphics gc, exCanvas c, int size, int posx, int posy,	String msg) 
		{
			drawable.drawChip(gc,c,
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
	 * @see lib.Drawable#drawChip(Graphics, online.common.exCanvas, int, int, int, java.lang.String)
	 */
	public void drawChip(Graphics gc, exCanvas c, int size, int posx, int posy,	String msg) 
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
