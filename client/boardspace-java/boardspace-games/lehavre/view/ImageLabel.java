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
package lehavre.view;

import java.awt.*;
import javax.swing.*;

import lehavre.main.NetworkInterface;

/**
 *
 *	The <code>ImageLabel</code> class is a normal JLabel that shows a smaller version
 *	of a given image when displayed, but the full size image in its tooltip window.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/14
 */
public class ImageLabel
extends JLabel
{
	static final long serialVersionUID =1L;
	/** The path to the images. */
	protected static final String IMAGE_PATH = "/lehavre/images/%s/%s";

	/** The full size image. */
	protected Image fullImage = null;

	/** The full size image dimensions. */
	protected int fullWidth, fullHeight;

	/**
	 *	Creates a new <code>ImageLabel</code> instance
	 *	from the given image file.
	 *	@param language the language version
	 *	@param path the path to the image file
	 *	@param tooltip provide true to add the tooltip
	 */
	public ImageLabel(NetworkInterface net,String language, String path, boolean tooltip) {
		createIcon(net,language, path, tooltip);
	}

	/**
	 *	Creates a new <code>ImageLabel</code> instance
	 *	from the given image file.
	 *	@param language the language version
	 *	@param path the path to the image file
	 */
	public ImageLabel(NetworkInterface net,String language, String path) {
		this(net,language, path, true);
	}

	/**
	 *	Creates the icon from the given image file.
	 *	@param language the language version
	 *	@param file the path to the image file
	 *	@param tooltip provide true to add a tooltip
	 */
	protected void createIcon(NetworkInterface net,String language, String file, boolean tooltip) {
		boolean neutral = (language == null);
		if(neutral) language = "neutral";
		String path = String.format(IMAGE_PATH, language, file);
		if(!neutral && !net.fileExists(path)) path = String.format(IMAGE_PATH, "en", file);
		Image full = net.getImage(path);
		Image actual = tooltip ? net.getScaledImage(full,0.333) : full;
		// rather than use prescaled images at smaller size, the interface ought
		// to keep the fullsize image and ask for a dynamically scaled copy at 
		// the currently appropriate size.
		ImageIcon icon = new ImageIcon(actual);
		setIcon(icon);
		setOpaque(false);
		setSize(icon.getIconWidth(), icon.getIconHeight());
		if(tooltip) {
			path = String.format(IMAGE_PATH, language, file);
			if(!neutral && !(net.fileExists(path))) path = String.format(IMAGE_PATH, "en", file);
			icon = new ImageIcon(net.getImage(path));
			fullWidth = icon.getIconWidth();
			fullHeight = icon.getIconHeight();
			fullImage = icon.getImage();
			setToolTipText("");
		}
	}

	/**
	 *	Creates the tooltip window showing the full size image.
	 */
	@SuppressWarnings("serial")
	public JToolTip createToolTip() {
		if(fullImage == null) return super.createToolTip();
		return new JToolTip() {
			{
				setBorder(null);
				setOpaque(false);
			}

			protected void paintComponent(Graphics g) {
				g.drawImage(fullImage, 0, 0, this);
			}

			public Dimension getPreferredSize() {
				return new Dimension(fullWidth, fullHeight);
			}
		};
	}
}