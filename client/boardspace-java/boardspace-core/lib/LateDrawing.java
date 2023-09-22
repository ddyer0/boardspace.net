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
 * interface for entities that are to be drawn at the end of the repaint process.
 * This allows them to appear "on top" of everything else without complicated
 * considerations of overlaps and drawing order.
 * @author Ddyer
 *
 */
public interface LateDrawing {
	/**
	 * draw the element
	 * @param g
	 */
	public void draw(Graphics g); 
}