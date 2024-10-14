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
package tamsk;

import lib.DrawableImageStack;
import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;
import tamsk.TamskConstants.TamskId;
import common.CommonConfig;
class ChipStack extends OStack<TamskChip>
{
	public TamskChip[] newComponentArray(int n) { return(new TamskChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class TamskChip extends chip<TamskChip> implements CommonConfig
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public TamskId id;
	public String contentsString() { return(id==null ? file : id.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private TamskChip(String na,double[]sc,TamskId con)
	{	
		scale=sc;
		file = na;
		id = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		otherChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private TamskChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		randomv = r.nextLong();
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	
	static public TamskChip Black = new TamskChip("black",new double[]{0.510,0.530,1.65},TamskId.Black);
	static public TamskChip White = new TamskChip("white",new double[]{0.510,0.530,1.65},TamskId.White);
	static public TamskChip Neutral = new TamskChip("gray",new double[]{0.510,0.530,1.65},TamskId.Neutral);

	static final TamskChip ring_4_4 = new TamskChip("ring-4-4",new double[] {0.50,0.56,2.79});
	static final TamskChip ring_4_3 = new TamskChip("ring-4-3",new double[] {0.53,0.55,2.48});
	static final TamskChip ring_4_2 = new TamskChip("ring-4-2",new double[] {0.5,0.55,2.52});
	static final TamskChip ring_4_1 = new TamskChip("ring-4-1",new double[] {0.48,0.6,2.63});
	
	static final TamskChip ring_3_3 = new TamskChip("ring-3-3",new double[] {0.51,0.58,2.8});
	static final TamskChip ring_3_2 = new TamskChip("ring-3-2",new double[] {0.52,0.59,2.31});
	static final TamskChip ring_3_1 = new TamskChip("ring-3-1",new double[] {0.50,0.56,2.34});
	
	static final TamskChip ring_2_2 = new TamskChip("ring-2-2",new double[] {0.51,0.52,2.73});
	static final TamskChip ring_2_1 = new TamskChip("ring-2-1",new double[] {0.54,0.62,2.17});
	
	static final TamskChip ring_1_1 = new TamskChip("ring-1-1",new double[] {0.52,0.53,2.7});
	
	static final TamskChip board = new TamskChip("board-oblique",new double[] {0.49,0.59,2.73});
	
	static final TamskChip max4[] = { null, ring_4_1, ring_4_2,ring_4_3,ring_4_4};
	static final TamskChip max3[] = { null, ring_3_1, ring_3_2,ring_3_3};
	static final TamskChip max2[] = { null, ring_2_1, ring_2_2};
	static final TamskChip max1[] = { null, ring_1_1 };
	static final TamskChip max0[] = { };
	
	static final TamskChip Ring = new TamskChip("ring",new double[] {0.5,0.5,2.3},TamskId.Ring);

	static public final TamskChip maxRings[][] = {max0,max1, max2, max3, max4 };
	public static final TamskChip NoSand = new TamskChip("nosand-nomask",new double[] {0.50,0.50,1});
	public static final TamskChip Sand = new TamskChip("sand-nomask",new double[] {0.50,0.50,1});
	public static TamskChip getRingOverlay(TamskCell c)
	{ TamskChip rings[] = maxRings[c.maxRings];
	  return rings [Math.min(rings.length-1,Math.max(0,c.stackTopLevel()))]; 
	}
	// indexes into the balls array, usually called the rack
    static final TamskChip getChip(int n) 
    	{ return(TamskId.values()[n].chip); 
    	}
    
    /**
     * this is the basic hook to substitute an alternate chip for display.  The canvas getAltChipSet
     * method is called from drawChip, and the result is passed here to substitute a different chip.
     * 
     */
	public TamskChip getAltChip(int set)
	{	
		return this;
	}


    /* plain images with no mask can be noted by naming them -nomask */
    static public TamskChip backgroundTile = new TamskChip("background-tile-nomask",null);
    static public TamskChip backgroundReviewTile = new TamskChip("background-review-tile-nomask",null);
   

    public static TamskChip Icon = new TamskChip("hex-icon-nomask",null);

    
   
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
		if(imagesLoaded) { Image.registerImages(otherChips); }
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
