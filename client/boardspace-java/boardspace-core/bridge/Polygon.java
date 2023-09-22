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
