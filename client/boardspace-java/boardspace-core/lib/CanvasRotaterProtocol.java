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
 * the general scheme for rotating individual windows is to keep the physical window
 * unchanged, but arrange for the contents to be drawn in a rotated way, and the mouse
 * coordinates to be similarly transformed.   
 * 
 * For rotation 2, which is upside-down, x and y are moved diagonally across the screen.
 * For rotation 1 and 3, the width and height are swapped and x or y reversed.  
 * 
 * The drawing and mouse handling code is all oriented to the  rotated screen size, so they
 * have to be careful to ignore the physical size.   the setLocalBounds() layout receives
 * the rotated width and height.
 * 
 * @author ddyer
 *
 */
public interface CanvasRotaterProtocol {
	/**
	 * get the canvas rotation in quater turn units
	 * @return
	 */
	public int getCanvasRotation();
	/**
	 * set the canvas rotatation in quarter turn units
	 * @param quarter_turns
	 */
	public void setCanvasRotation(int quarter_turns);

	public int rotateCanvasX(int x,int y);
	/**
	 * translate a coordinate from a physical window into the rotated space
	 * @param x
	 * @param y
	 * @return
	 */
	public int rotateCanvasY(int x,int y);
	/**
	 * reverse a coordinate in rotated space back to physical space
	 * @param x
	 * @param y
	 * @return
	 */
	public int unrotateCanvasX(int x,int y);
	/**
	 * reverse a coordinate in rotated space back to physical space
	 * @param x
	 * @param y
	 * @return
	 */
	public int unrotateCanvasY(int x,int y);
	/**
	 * get the effective width of the canvas adjusted for rotation (if any)
	 * @return
	 */
	public int getRotatedWidth();
	/**
	 * get the effective height of the canvas adjusted for rotation (if any)
	 * @return
	 */
	public int getRotatedHeight();
	/**
	 * get the effective left coordinate of the window
	 * @return
	 */
	public int getRotatedLeft();
	/** get the effective top coordinate of the window
	 * 
	 * @return
	 */
	public int getRotatedTop();
	
}
