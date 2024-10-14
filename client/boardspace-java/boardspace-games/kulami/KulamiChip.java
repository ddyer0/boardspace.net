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
package kulami;

import kulami.KulamiConstants.KulamiId;
import lib.DrawableImageStack;
import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;

class ChipStack extends OStack<KulamiChip>
{
	public KulamiChip[] newComponentArray(int n) { return(new KulamiChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by Kulami;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class KulamiChip extends chip<KulamiChip> 
{
	private int index = 0;
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public KulamiId id;
	
	// constructor for the chips on the board, which are the only things that are digestable.
	private KulamiChip(String na,double[]sc,KulamiId con)
	{	index = allChips.size();
		scale=sc;
		file = na;
		id = con;
		randomv = r.nextLong();
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private KulamiChip(String na,double[]sc)
	{	index = allChips.size();
		scale=sc;
		file = na;
		allChips.push(this);
	}
	
	public int chipNumber() { return(index); }
	
	private static double redScale[]={0.581,0.432,1.34};
	public static KulamiChip Red = new KulamiChip("red-ball",redScale,KulamiId.Red_Chip_Pool);
	
	private static double blackScale[] = {0.512,0.432,1.34};
	public static KulamiChip Black = new KulamiChip("black-ball",blackScale,KulamiId.Black_Chip_Pool);

	public static KulamiChip CANONICAL_PIECE[] = { Red,Black };

    // indexes into the balls array, usually called the rack
    static final KulamiChip getChip(int n) { return(CANONICAL_PIECE[n]); }
       
    // these scale numbers are chosen to adjust the size and position of chips drawn on
    // the board at their nominal locations - in the upper-left corner of the area they
    // cover.
    static final KulamiChip board_3x2 = new KulamiChip("board-3x2",new double[] {0.162,0.255,5.616},KulamiId.SubBoard);
    static final KulamiChip board_2x3 = new KulamiChip("board-2x3",new double[] {0.269 ,0.207,4.592},KulamiId.SubBoard);
    static final KulamiChip board_2x2H = new KulamiChip("board-2x2-h",new double[] {0.216,0.251,3.648},KulamiId.SubBoard);
    static final KulamiChip board_2x2V = new KulamiChip("board-2x2-v",new double[] {0.307,0.275,4.807},KulamiId.SubBoard);
    static final KulamiChip board_3x1 = new KulamiChip("board-3x1",new double[] {0.163,0.422,5.526},KulamiId.SubBoard);
    static final KulamiChip board_1x3 = new KulamiChip("board-1x3",new double[] { 0.436,0.173,3.099},KulamiId.SubBoard);
    static final KulamiChip board_2x1 = new KulamiChip("board-2x1",new double[] {0.222,0.455,3.8},KulamiId.SubBoard);
    static final KulamiChip board_1x2 = new KulamiChip("board-1x2",new double[] { 0.382,0.260,2.396},KulamiId.SubBoard);

    /* plain images with no mask can be noted by naming them -nomask */
    static public KulamiChip backgroundTile = new KulamiChip("background-tile-nomask",null);
    static public KulamiChip backgroundReviewTile = new KulamiChip("background-review-tile-nomask",null);
   
 	public static KulamiChip Icon = new KulamiChip("icon-nomask",blackScale);

    
   
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
		if(imagesLoaded) { Image.registerImages(allChips); }
		}
	}   
}
