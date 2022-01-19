package bridge;

import java.awt.Rectangle;

import lib.G;
import lib.Graphics;

@SuppressWarnings("serial")
public class Polygon extends java.awt.Polygon 
{

	/**
	 * add a rectangle as 4 new points in a polygon
	 * @param p
	 * @param r
	 */
	public void addRect(Rectangle rect)
	{   int l = G.Left(rect);
		int t = G.Top(rect);
		int r = l+G.Width(rect);
		int b = t+G.Height(rect);
		addPoint(l,t);
	    addPoint(r,t);
	    addPoint(r,b);
	    addPoint(l,b);
	    addPoint(l,t);	
	}

	public void fillPolygon(Graphics inG)
	{
		if(inG!=null) { inG.fillPolygon(this); }
	}

	public void framePolygon(Graphics inG)
	{
		if(inG!=null) { inG.framePolygon(this); }
	}

}
