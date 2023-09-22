/* copyright notice */package lehavre.model;

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