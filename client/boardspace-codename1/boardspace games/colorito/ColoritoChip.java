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
package colorito;

import colorito.ColoritoConstants.Variation;
import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.Random;
import lib.exCanvas;
import online.game.chip;
/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class ColoritoChip extends chip<ColoritoChip> 
{	enum ChipColor {red,yellow,blue,green,neutral};
	public ChipColor color;
	private String name = "";
	private ColoritoChip numbered[] = null;
	int colorIndex;
	
	public int chipNumber() { return(color.ordinal()); }
	public int copyNumber=0;
	static final int FIRST_TILE_INDEX = 0;
    static final int N_STANDARD_TILES = 5;
    static final int N_STANDARD_CHIPS = 1;
    static final int BLANK_CHIP_INDEX = 0;
    static final int FIRST_CHIP_INDEX = N_STANDARD_TILES;
    static final int BLACK_CHIP_INDEX = FIRST_CHIP_INDEX+1;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX;

	private ColoritoChip(String na,ChipColor col,Image im,Random R,double scl[],boolean number)
	{	name = na;
		color=col;
		image = im;
		randomv = R.nextLong();
		scale = scl;
		colorIndex = color.ordinal()<2?0:1;
		if(number)
		{
			numbered = new ColoritoChip[Variation.Colorito_10.cols*2];
			for(int i=1;i<=numbered.length;i++)
			{	numbered[i-1] = new ColoritoChip(na,col,im,R.nextLong(),scl,i);
			}
		}
	}
	private ColoritoChip(String na,ChipColor col,Image im,long rv,double scl[],int number)
	{	name = na;
		color=col;
		image = im;
		randomv = rv;
		scale = scl;
		colorIndex = color.ordinal()<2?1:0;
		copyNumber = number;
	}
	public String toString()
	{	return("<"+ name+" #"+color+((copyNumber>0)?""+copyNumber:"")+">");
	}
	public String contentsString() 
	{ return(name); 
	}
		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private ColoritoChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	{0.5,0.5,0.98},		// light blue square
    	{0.5,0.5,0.98},		// dark blue square
    	{0.5,0.5,0.98},		// red square
    	{0.5,0.5,0.98},		// yellow square
    	{0.5,0.5,0.98},		// neutral square
       	{0.527,0.430,1.38},	// red chip
    	{0.500,0.402,1.38},	// yellow chip
       	{0.527,0.430,1.38},	// blue chip
    	{0.500,0.402,1.38},	// green chip
    };
     
    public void drawChip(Graphics gc,
            exCanvas canvas,
            int SQUARESIZE,
            double xscale,
            int cx,
            int cy,
            java.lang.String label)
    {	super.drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,copyNumber>0?""+copyNumber:label);
    }
	public static ColoritoChip getTile(int color)
	{	return(CANONICAL_PIECE[FIRST_TILE_INDEX+color]);
	}
	public static ColoritoChip getChip(int color)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color]);
	}
	public static ColoritoChip getChip(int color,int number)
	{	ColoritoChip c = getChip(color);
		return(c.numbered[number-1]);
	}
  /* pre load images and create the canonical pieces
   * 
   */
 
   static final String[] ImageNames = 
       { "red-tile","yellow-tile","green-tile","blue-tile", "neutral-tile",
	   	 "red-chip-np","yellow-chip-np","green-chip-np","blue-chip-np",};
   static final ChipColor ChipColors[] =
	   { ChipColor.red,ChipColor.yellow,ChipColor.blue,ChipColor.green,ChipColor.neutral,
	     ChipColor.red,ChipColor.yellow,ChipColor.green,ChipColor.blue};
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        ColoritoChip CC[] = new ColoritoChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new ColoritoChip(ImageNames[i],ChipColors[i],IM[i],rv,SCALES[i],i>=N_STANDARD_TILES); 
        	}
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}


}
