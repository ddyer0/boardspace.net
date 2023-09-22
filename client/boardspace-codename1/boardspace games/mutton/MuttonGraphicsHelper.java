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
package mutton;

import com.codename1.ui.geom.Rectangle;

import lib.Graphics;
import lib.Image;
import lib.G;
import lib.StringStack;
import online.game.commonCanvas;


public class MuttonGraphicsHelper implements MuttonConstants {

	// Static instance provided by the static factory method getInstance()
	private static MuttonGraphicsHelper myInstance = null;
	private int NUM_IMAGES = ImageNames.length;

	// The data for each of the images
	private Image [] images         = new Image [NUM_IMAGES];
	public  int [] imageWidths      = new int   [NUM_IMAGES];
	public  int [] imageHeights     = new int   [NUM_IMAGES];
	private int [] imagePin         = new int   [NUM_IMAGES];
	// Types of pinning supported
	private static final int UL_CORNER = 0;
	private static final int CENTERED = 1;

	// Data about the images to be loaded by the graphics helper
	private static final int [][] imageData = {
		{IMG_ALPHABET_BLACK, 26, 1, CENTERED},
		{IMG_ALPHABET_RED, 26, 1, CENTERED},
		{IMG_SHEEP, 6, 2, CENTERED},
		{IMG_WOLVES, 6, 2, CENTERED},
		{IMG_BOARD_TILE, 1, 1, CENTERED},
		{IMG_HISTORY_DEAD_HEADER, 3, 1, CENTERED},
		{IMG_HISTORY_DEAD_SHEEP, 1, 1, CENTERED},
		{IMG_HISTORY_SUSPECT_SHEEP, 1, 1, CENTERED},
		{IMG_ACTIVE_HIGHLIGHT, 1, 1, CENTERED},
		{IMG_CELL_HIGHLIGHTS, 5, 1, CENTERED},
		{IMG_BLOOD_HIGHLIGHT, 1, 1, CENTERED},
		{IMG_NUMBERS, 10, 1, CENTERED},
		{IMG_ARROWS, 2, 2, UL_CORNER},
		{IMG_FARMER_FACES, 3, 1, UL_CORNER}
	};


	/**
	 * Constructor which creates the graphics.
	 *
	 * This is private because the graphics helper should only be retrieved
	 * via the getInstance() factory method.
	 *
	 */
	private MuttonGraphicsHelper (commonCanvas theRoot) {
		// Start the loading of all of the images
		StringStack imageNames = new StringStack();
		for (int i=0; i<imageData.length; i++) {
			int index = imageData[i][0];
			imageNames.addElement(ImageNames[index]);
		}
		images = theRoot.loader.load_images(ImageDir,imageNames.toArray());
		
		// Compute the width & heights for the images
		for (int i=0; i<imageData.length; i++) {
			int index = imageData[i][0];
			imageWidths[index]  = images[index].getWidth() / imageData[i][1];
			imageHeights[index] = images[index].getHeight() / imageData[i][2];
			imagePin[index]     = imageData[i][3];
		}
	}

	/**
	 * A static factory for returning the single graphics helper instance.
	 */
	public static MuttonGraphicsHelper getInstance (commonCanvas theRoot) {
		if (myInstance == null) {
			myInstance = new MuttonGraphicsHelper(theRoot);
		}

		return myInstance;
	}


	/**
	 * Paint an image
	 *
	 * @param g               The graphics area to draw on
	 * @param (x, y)          Position in g to paint the image
	 * @param imageId         The id of the source image
	 * @param tx, ty          Source tile within the source image
	 */
	public void paintImage (
		Graphics g,
		int x, int y,
		int imageId,
		int tx, int ty) {

		// Get width & height
		int w = imageWidths[imageId];
		int h = imageHeights[imageId];

		// Adjust the destination points, if the image is to be centered
		if (imagePin[imageId] == CENTERED) {
			x -= (w / 2);
			y -= (h / 2);
		}

		// Calculate source rectangle
		int sx = tx * w;
		int sy = ty * h;
	images[imageId].drawImage(g,     // Src image
			    x, y, x+w, y+h,              // Dest rect
			    sx, sy, sx+w, sy+h);
	}

	/**
	 * Paint an image scaled by a scaling factor
	 *
	 * @param g               The graphics area to draw on
	 * @param baseRect        Position in g to contain the drawing
	 * @param x		          Position in baseRect to paint the image
	 * @param y
	 * @param imageId         The id of the source image
	 * @param tx
	 * @param ty          Source tile within the source image
	 * @param scalingFactor   The amount to scale the destination image
	 */
	public void paintImage (
		Graphics g,
		Rectangle baseRect,
		int x, int y,
		int imageId,
		int tx, int ty,
		double scalingFactor) {

		// Get width & height
		int w = imageWidths[imageId];
		int h = imageHeights[imageId];

		// Adjust the destination points, if the image is to be centered
		if (imagePin[imageId] == CENTERED) {
			x -= (w * scalingFactor / 2.0);
			y -= (h * scalingFactor / 2.0);
		}

		// Calculate source rectangle
		int sx = tx * w;
		int sy = ty * h;

		// Scale the destination rectangle
		int destW = (int) (w * scalingFactor);
		int destH = (int) (h * scalingFactor);
		
	images[imageId].drawImage(g,     // Src image
		    G.Left(baseRect)+x, G.Top(baseRect)+y, G.Left(baseRect)+x+destW, G.Top(baseRect)+y+destH,      // Dest rect
		    sx, sy, sx+w, sy+h);
		    
	}
	/**
	 * Draw a number on the screen, using the IMG_NUMBERS images
	 *
	 * @param g            The graphics area to draw on
	 * @param baseRect     Position in g to contain the drawing
	 * @param x
	 * @param y	      
	 * @param number       The number to draw.
	 */
	public void paintNumber (Graphics g, Rectangle baseRect, int x, int y, int number, double scalingFactor) {
		if (number > 9) {
			// Two-digit number
			int spacing = (int) (6.0 * scalingFactor);
			paintImage (g, baseRect, x-spacing, y, IMG_NUMBERS, (number / 10), 0, scalingFactor);
			paintImage (g, baseRect, x+spacing, y, IMG_NUMBERS, (number % 10), 0, scalingFactor);
		} else {
			// One-digit number
			paintImage (g, baseRect, x, y, IMG_NUMBERS, number, 0, scalingFactor);
		}
	}
}