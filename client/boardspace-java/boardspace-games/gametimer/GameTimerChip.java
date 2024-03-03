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
import lib.Random;
import online.game.chip;
import common.CommonConfig;
import gametimer.GameTimerConstants.GameTimerId;
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

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public GameTimerId id;
	public String contentsString() { return(id==null ? file : id.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private GameTimerChip(String na,double[]sc,GameTimerId con)
	{	
		scale=sc;
		file = na;
		id = con;
		randomv = r.nextLong();
		stoneChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private GameTimerChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	
     
    /**
     * this is the basic hook to substitute an alternate chip for display.  The canvas getAltChipSet
     * method is called from drawChip, and the result is passed here to substitute a different chip.
     * 
     */
	public GameTimerChip getAltChip(int set)
	{	
		return this;
	}


    /* plain images with no mask can be noted by naming them -nomask */
    static public GameTimerChip backgroundTile = new GameTimerChip("background-tile-nomask",null);
    static public GameTimerChip backgroundReviewTile = new GameTimerChip("background-review-tile-nomask",null);
   
    static private double hexScale[] = {0.50,0.50,1.60};
    static private double hexScaleNR[] = {0.58,0.50,1.54};
    static public GameTimerChip hexTile = new GameTimerChip("hextile",hexScale,null);
    static public GameTimerChip hexTile_1 = new GameTimerChip("hextile-1",hexScale,null);
    static public GameTimerChip hexTile_2 = new GameTimerChip("hextile-2",hexScale,null);
    static public GameTimerChip hexTile_3 = new GameTimerChip("hextile-3",hexScale,null);
    static public GameTimerChip hexTileNR = new GameTimerChip("hextile-nr",hexScaleNR,null);
    static public GameTimerChip hexTileNR_1 = new GameTimerChip("hextile-nr-1",hexScaleNR,null);
    static public GameTimerChip hexTileNR_2 = new GameTimerChip("hextile-nr-2",hexScaleNR,null);
    static public GameTimerChip hexTileNR_3 = new GameTimerChip("hextile-nr-3",hexScaleNR,null);

    static {
    	hexTile.alternates = new GameTimerChip[] {hexTile,hexTile_1,hexTile_2,hexTile_3};
       	hexTileNR.alternates = new GameTimerChip[] {hexTileNR,hexTileNR_1,hexTileNR_2,hexTileNR_3};
    }

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
		imagesLoaded = forcan.load_masked_images(StonesDir,stoneChips)
				& forcan.load_masked_images(Dir,otherChips);
		}
	}   
	/*
	// override for drawChip can draw extra ornaments or replace drawing entirely
	public void drawChip(Graphics gc,
	            exCanvas canvas,
	            int SQUARESIZE,
	            double xscale,
	            int cx,
	            int cy,
	            java.lang.String label)
	    {	super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, label);

	    }
	 */
}
