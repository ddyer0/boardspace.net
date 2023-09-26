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
package lehavre.view.labels;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

import lehavre.main.NetworkInterface;
import lehavre.model.*;
import lehavre.view.*;
import lehavre.view.menus.*;

/**
 *
 *	The <code>RoundLabel</code> class is a specialized label
 *	to display round cards in the main window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/26
 */
public final class RoundLabel
extends ImageLabel
{
	static final long serialVersionUID =1L;
	/** The path to the round cards image directory. */
	private static final String ROUNDS_PATH = "cards/rounds/%s";

	/**
	 *	Creates a new <code>RoundLabel</code> instance for the given round card.
	 *	@param language the language version
	 *	@param menu the round card menu
	 *	@param round the round card
	 */
	public RoundLabel(NetworkInterface net,String language, RoundMenu menu, Round round) {
		super(net,language, String.format(ROUNDS_PATH, round));
		int n = round.getPlayerCount() - 1;
		int width = getWidth();
		int height = getHeight();
		double fx = (width / 80.0);
		double fy = (height / 123.0);
		Image im = createIcon(((ImageIcon)getIcon()).getImage(), width, height, 4, new int[]{20, 40, 61, 81, 102}[n], 72, 16, 2, fx, fy);
		setIcon(new ImageIcon(im));
		fx = (fullWidth / 233.0);
		fy = (fullHeight / 359.0);
		fullImage = createIcon(fullImage, fullWidth, fullHeight, 12, new int[]{57, 117, 176, 236, 295}[n], 209, 51, 4, fx, fy);
		setComponentPopupMenu(menu);
	}

	/**
	 *	Creates the icon for the label. This method is capable of creating
	 *	both, the small and the large icon. Also, it draws a rectangle that
	 *	highlights the line that shows the data for the proper player count.
	 *	@param image the image for the icon
	 *	@param width the width of the icon
	 *	@param height the height of the icon
	 *	@param dx the x-offset of the rectangle
	 *	@param dy the y-offset of the rectangle
	 *	@param w the width of the rectangle
	 *	@param h the height of the rectangle
	 *	@param s the stroke width of the rectangle
	 *	@param f the zoom factor
	 *	@return the updated image
	 */
	private Image createIcon(Image image, int width, int height, int dx, int dy, int w, int h, int s, double fx, double fy) {
		// create image
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.drawImage(image, 0, 0, this);
		g.setColor(new GUIHelper("main").getColor("Round"));
		// create rectangle
		dx = zoom(dx, fx);
		dy = zoom(dy, fy);
		w = zoom(w, fx);
		h = zoom(h, fy);
		s = zoom(s, Math.min(fx, fy));
		for(int i = 0; i < s; i++) g.drawRect(dx - i, dy - i, w + 2 * i, h + 2 * i);
		return img;
	}

	/**
	 *	Enlarges the given value by the given zoom factor.
	 *	@param value the value to enlarge
	 *	@param factor the zoom factor
	 *	@return the enlarged value
	 */
	private int zoom(int value, double factor) {
		return (int)Math.round(value * factor);
	}
}