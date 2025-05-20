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
package bridge;

import lib.G;
import lib.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;

public class Polygon
{
	GeneralPath path = new GeneralPath();
	public Polygon() { }
	
	public void reset() { path.reset(); }
	
	public void addPoint(int x,int y) 
	{ 	if(path.getCurrentPoint()==null) 
			{ path.moveTo(x,y); } 
			else { path.lineTo(x,y); }
	}
	public void fill(Graphics g)
	{
		g.getGraphics().fillShape(path);
	}
	public void frame(Graphics g)
	{
		Stroke stroke = new Stroke(
                1,
                Stroke.CAP_BUTT,
                Stroke.JOIN_ROUND, 1f
            );
 
		// Draw the shape
        g.getGraphics().drawShape(path, stroke);
		
	}
	public Rectangle getBounds() {
		return(path.getBounds());
	}
	public boolean contains(int i, int j) { return(path.getBounds().contains(i,j)); }
	/**
	 * add a rectangle as 4 new points in a polygon
	 * @param p
	 * @param rect
	 */
	public void addRect(Rectangle rect)
	{   int l = G.Left(rect);
		int t = G.Top(rect);
		int b = G.Bottom(rect);
		int r = G.Right(rect);
		addPoint(l,t);
		addPoint(r,t);
	    addPoint(r,b);
	    addPoint(l,b);
	    addPoint(l,t);	
	}
	public void framePolygon(Graphics inG)
	{
		if(inG!=null) { frame(inG); }
	}
	public void fillPolygon(Graphics inG)
	{
		if(inG!=null) { fill(inG); }
	}
}
