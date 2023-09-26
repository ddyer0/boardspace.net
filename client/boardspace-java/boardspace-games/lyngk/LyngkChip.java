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
package lyngk;

import lib.DrawableImageStack;
import lib.G;
import lib.ImageLoader;
import lib.Random;
import lib.StockArt;
import lyngk.LyngkConstants.LyngkId;
import online.game.chip;


/**
 * this is a specialization of {@link chip} to represent the stones used by lyngk;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class LyngkChip extends chip<LyngkChip>
{
	private int index = 0;
	private LyngkChip altChip = null; 
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public LyngkId id;
	public int maskColor = 0;


	// constructor for the chips on the board, which are the only things that are digestable.
	private LyngkChip(String na,double[]sc,LyngkId con,LyngkChip alt)
	{	index = allChips.size();
		altChip = alt;
		scale=sc;
		file = na;
		id = con;
		randomv = r.nextLong();
		// white chips don't count, duplicates are allowed
		maskColor = (id==LyngkId.White_Chip) ? 0 : (1<<id.ordinal());
		allChips.push(this);
	}
	// constructor for the chips made from stock elements
	private LyngkChip(StockArt e,double[]sc,LyngkId con)
	{	index = allChips.size();
		scale=sc;
		file = e.file;
		image = e.image;
		id = con;
		randomv = r.nextLong();
	}
	
	// constructor for all the other random artwork.
	private LyngkChip(String na,double[]sc)
	{	index = allChips.size();
		scale=sc;
		file = na;
		allChips.push(this);
	}
	
	public int chipNumber() { return(index); }
	
	private static double whiteFlatScale[]={0.598,0.450,1.3};
	public static LyngkChip WhiteFlat = new LyngkChip("white-flat",whiteFlatScale);
	private static double whiteScale[]={0.598,0.450,1.3};
	public static LyngkChip White = new LyngkChip("white",whiteScale,LyngkId.White_Chip,WhiteFlat);
	
	private static double blackFlatScale[] = {0.612,0.458,1.3};
	public static LyngkChip BlackFlat = new LyngkChip("black-flat",blackFlatScale);
	private static double blackScale[] = {0.612,0.458,1.3};
	public static LyngkChip Black = new LyngkChip("black",blackScale,LyngkId.Black_Chip,BlackFlat);

	private static double redFlatScale[] = {0.583,0.458,1.3};
	public static LyngkChip RedFlat = new LyngkChip("red-flat",redFlatScale);
	private static double redScale[] = {0.583,0.458,1.3};
	public static LyngkChip Red = new LyngkChip("red",redScale,LyngkId.Red_Chip,RedFlat);
	 
	private static double greenFlatScale[] = {0.598,0.458,1.3};
	public static LyngkChip GreenFlat = new LyngkChip("green-flat",greenFlatScale);
	private static double greenScale[] = {0.598,0.458,1.3};
	public static LyngkChip Green = new LyngkChip("green",greenScale,LyngkId.Green_Chip,GreenFlat);

	private static double blueFlatScale[] = {0.631,0.458,1.3};
	public static LyngkChip BlueFlat = new LyngkChip("blue-flat",blueFlatScale);
	private static double blueScale[] = {0.631,0.458,1.3};
	public static LyngkChip Blue = new LyngkChip("blue",blueScale,LyngkId.Blue_Chip,BlueFlat);

	private static double ivoryFlatScale[] = {0.598,0.458,1.3};
	public static LyngkChip IvoryFlat = new LyngkChip("ivory-flat",ivoryFlatScale);
	private static double ivoryScale[] = {0.598,0.458,1.3};
	public static LyngkChip Ivory = new LyngkChip("ivory",ivoryScale,LyngkId.Ivory_Chip,IvoryFlat);

	public static double BlankScale[] = {0.5,0.5,1.0};
	public static LyngkChip swing_cw = new LyngkChip("swing-cw-nomask",BlankScale,LyngkId.RotateRect,null);
	public static LyngkChip swing_ccw = new LyngkChip("swing-ccw-nomask",BlankScale,LyngkId.RotateRect,null);
	public static LyngkChip lift = new LyngkChip("lift-icon-nomask",BlankScale,LyngkId.LiftRect,null);

	public static LyngkChip board = new LyngkChip("board",null);
	public static LyngkChip boardFlat = new LyngkChip("board-flat",null);

	public static LyngkChip CANONICAL_PIECE[] = { White,Black,Red,Green,Blue,Ivory };
	public static int nColors = CANONICAL_PIECE.length;
	
    // indexes into the balls array, usually called the rack
    static final LyngkChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    static final LyngkChip getChip(LyngkId id)
    {	for(LyngkChip c : CANONICAL_PIECE) { if(c.id==id) { return(c); }}
    	throw G.Error("No chip for %s",id);
    }
    public LyngkChip getAltChip(int set) { return ( ((set==0)||(altChip==null)) ? this : altChip); }

    /* plain images with no mask can be noted by naming them -nomask */
    static public LyngkChip backgroundTile = new LyngkChip("background-tile-nomask",null);
    static public LyngkChip backgroundReviewTile = new LyngkChip("background-review-tile-nomask",null);
   
    
 
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
		imagesLoaded = forcan.load_masked_images(Dir,allChips);
		}
	}   
}
