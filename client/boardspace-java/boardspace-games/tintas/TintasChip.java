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
package tintas;

import lib.G;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;

class ChipStack extends OStack<TintasChip>
{
	public TintasChip[] newComponentArray(int n) { return(new TintasChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by tintas;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class TintasChip extends chip<TintasChip> implements TintasConstants
{
	private int index = 0;
	TintasChip altChip = null;
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static ChipStack allChips = new ChipStack();
	private static boolean imagesLoaded = false;
	public TintasId id;
	
	public TintasChip getAltChip(int set) 
	{ return ( ((set==0)||(altChip==null)) ? this : altChip); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private TintasChip(String na,double[]sc,TintasId con)
	{	index = allChips.size();
		scale=sc;
		file = na;
		id = con;
		randomv = r.nextLong();
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private TintasChip(String na,double[]sc)
	{	index = allChips.size();
		scale=sc;
		file = na;
		randomv = r.nextLong();
		allChips.push(this);
	}
	
	public int chipNumber() { return(index); }
    
	// put these first so they get index starting at 0;
    static public TintasChip Red = new TintasChip("red",new double[]{0.543,0.467,1.18});
    static public TintasChip White = new TintasChip("white",new double[]{0.533,0.526,1.27});
    static public TintasChip Yellow = new TintasChip("yellow",new double[]{0.557,0.486,1.304});
    static public TintasChip Green = new TintasChip("green",new double[]{0.51,0.517,1.245});
    static public TintasChip Purple = new TintasChip("purple",new double[]{0.543,0.467,1.466});
    static public TintasChip Blue = new TintasChip("blue",new double[]{0.543,0.51,1.25});
    static public TintasChip Aqua = new TintasChip("aqua",new double[]{0.543,0.467,1.18});
    static public TintasChip Pawn = new TintasChip("pawn",new double[]{0.548,0.603,1.467});
 
	// put these first so they get index starting at 0;
    static public TintasChip Red_np = new TintasChip("red-np",new double[]{0.543,0.467,1.2});
    static public TintasChip White_np = new TintasChip("white-np",new double[]{0.533,0.526,1.2});
    static public TintasChip Yellow_np = new TintasChip("yellow-np",new double[]{0.557,0.486,1.2});
    static public TintasChip Green_np = new TintasChip("green-np",new double[]{0.51,0.517,1.2});
    static public TintasChip Purple_np = new TintasChip("purple-np",new double[]{0.543,0.467,1.2});
    static public TintasChip Blue_np = new TintasChip("blue-np",new double[]{0.543,0.51,1.2});
    static public TintasChip Aqua_np = new TintasChip("aqua-np",new double[]{0.543,0.467,1.2});
    static public TintasChip Pawn_np = new TintasChip("pawn-np",new double[]{0.606,0.496,1.76});
   

    static final TintasChip board = new TintasChip("board",new double[]{0.5,0.5,1.0},null);
    static final TintasChip board_np = new TintasChip("board-np",new double[]{0.5,0.5,1.0},null);
    
    /* plain images with no mask can be noted by naming them -nomask */
    static public TintasChip backgroundTile = new TintasChip("background-tile-nomask",null);
    static public TintasChip backgroundReviewTile = new TintasChip("background-review-tile-nomask",null);
    static public TintasChip Icon = new TintasChip("tintas-icon-nomask",null);

	public static TintasChip Chips[] = { Red,White,Blue,Purple,Yellow,Green,Aqua};
	public static TintasChip AltChips[] = { Red_np,White_np,Blue_np,Purple_np,Yellow_np,Green_np,Aqua_np};
	public static int nColors = Chips.length;
	
    // indexes into the balls array, usually called the rack
    static final TintasChip getChip(int n) 
    { for(int lim=allChips.size()-1; lim>=0; lim--) 
    	{ TintasChip ch = allChips.elementAt(lim);
    	  if(ch.index==n) { return(ch); }
    	}
    	throw G.Error("Chip %s not found",n);
    }
  
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
		forcan.load_masked_images(Dir,allChips);
		for(int i=0;i<Chips.length;i++) { Chips[i].altChip = AltChips[i]; }
		Pawn.altChip = Pawn_np; 
		imagesLoaded = true;
		}
	}   
}
