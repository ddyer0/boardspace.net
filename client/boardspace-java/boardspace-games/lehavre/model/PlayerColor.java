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
package lehavre.model;

import java.awt.Color;

/**
 *
 *	The <code>PlayerColor</code> enum represents the color of a player's game pieces.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/7
 */
public enum PlayerColor
{
	Red(198, 50, 32),
	Purple(118, 64, 71),
	Green(69, 133, 27),
	White(242, 209, 154),
	Blue(34, 86, 139);

	/** The red, green and blue values. */
	private final int red, green, blue;

	/**
	 *	The initializer method.
	 *	@param red the red value
	 *	@param green the green value
	 *	@param blue the blue value
	 */
	PlayerColor(int red, int green, int blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	/**
	 *	Returns the red value.
	 *	@return the red value
	 */
	public int getRed() {
		return red;
	}

	/**
	 *	Returns the green value.
	 *	@return the green value
	 */
	public int getGreen() {
		return green;
	}

	/**
	 *	Returns the blue value.
	 *	@return the blue value
	 */
	public int getBlue() {
		return blue;
	}

	/**
	 *	Returns the color object.
	 *	@return the color object
	 */
	public Color toColor() {
		return new Color(red, green, blue);
	}
}