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

import bridge.Color;

public class Location implements LocationProvider {
	int x;
	int y;
	Color color = null;
	LocationProvider next = null;
	public LocationProvider getNext() { return next; }
	public void setNext(LocationProvider c) { next = c; }
	public int getX() { return x; }
	public int getY() { return y; }
	public Location(int xx,int yy) { x = xx; y=yy; }
	public Location(int xx,int yy,Color c) { this(xx,yy); color = c; }
	public Color getColor() { return color; }
}
