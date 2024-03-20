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
package gametimer;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.OStack;
import online.game.chip;
import common.CommonConfig;
class ChipStack extends OStack<GameTimerChip>
{
	public GameTimerChip[] newComponentArray(int n) { return(new GameTimerChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class GameTimerChip extends chip<GameTimerChip> implements CommonConfig
{
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	
	// constructor for all the other random artwork.
	private GameTimerChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	

    /* plain images with no mask can be noted by naming them -nomask */
    static public GameTimerChip backgroundTile = new GameTimerChip("background-tile-nomask",null);
    static public GameTimerChip backgroundReviewTile = new GameTimerChip("background-review-tile-nomask",null);
   
 
    public static GameTimerChip Icon = new GameTimerChip("hex-icon-nomask",null);

    
   
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images into the stack of
     * chips we've built
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!imagesLoaded)
		{	
		imagesLoaded = forcan.load_masked_images(Dir,otherChips);
		}
	}   

}
