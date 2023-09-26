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
package gounki;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests. 
 */
public class GounkiChip extends chip<GounkiChip>
{	
	private int colorIndex;					// the player owner of this chip color
	private int chipIndex;						// unique identity of this chip
	private String name = "";
	static final int N_CHIP_TYPES = 2;			// number of different chip types (round, square in this case)
	static final int N_CHIP_IMAGES = 3;			// number of chip images of each chip type
	static final int SQUARE_CHIP_INDEX = 1;
	static final int ROUND_CHIP_INDEX = 0;
	GounkiChip altChip = null;
	public GounkiChip getAltChip(int set) { return ((set!=0 && (altChip!=null)) ? altChip : this); }
	
	public int chipNumber() { return(chipIndex); }
	public int getColorIndex() { return(colorIndex); }
	public boolean isSquare() { return((chipIndex&1)==SQUARE_CHIP_INDEX); }
	public boolean isRound() { return((chipIndex&1)==ROUND_CHIP_INDEX); }
	public int chipTypeIndex() { return(chipIndex&1); }
	
	
	private GounkiChip(int ch,String na,Image im,double scl[])
	{	name = na;
		chipIndex = ch;
		scale = scl;
		colorIndex=chipIndex&1;
		image = im;
	}
	
	private GounkiChip(int ch,String na,Image im,double scl[],Random rv,GounkiChip CC[],GounkiChip alt)
	{	int pla = ch/(N_CHIP_TYPES*N_CHIP_IMAGES);
		int plaChipn = ch%(N_CHIP_TYPES*N_CHIP_IMAGES);
		int generation = plaChipn/N_CHIP_TYPES;
		int typ = plaChipn%N_CHIP_TYPES;
		name = na;
		altChip = alt;
		chipIndex = ch;
		colorIndex=pla;
		image = im;
		CC[ch] = this;
		//
		// the unusual characteristic of this chip is that there are several chip numbers
		// for each chip, but they get identical randomv so they hash the same.
		//
		randomv = (generation==0)
			? rv.nextLong() 	// new unique type
			: CC[ pla*N_CHIP_TYPES*N_CHIP_IMAGES+typ].randomv;
		scale = scl;
	}
	public String toString()
	{	return("<"+ name+" #"+chipIndex+">");
	}
	static private double AltScales[][] = {
			{0.516,0.460,1.34},	// light round
			{0.516,0.460,1.34},	// dark round
			{0.50,0.500,0.98},	// light square
			{0.50,0.500,0.98},	// dark square
	};

	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private GounkiChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	{0.590,0.500,1.38},	// white chip r1
    	{0.540,0.500,1.16},	// white chip s1
    	{0.590,0.500,1.38},	// white chip r2
    	{0.540,0.500,1.16},	// white chip s2
    	{0.590,0.500,1.38},	// white chip r3
    	{0.540,0.500,1.16},	// white chip s3
      	{0.590,0.500,1.38},	// black chip r1
    	{0.540,0.500,1.16},	// black chip s1
     	{0.590,0.500,1.38},	// black chip r2
    	{0.540,0.500,1.16},	// black chip s2
     	{0.590,0.500,1.38},	// black chip r3
    	{0.540,0.500,1.16},	// black chip s3
   };
     
 
	public static GounkiChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}
	public static GounkiChip getChip(int player,int ind)
	{	return(CANONICAL_PIECE[ccPlayerIndex[player]+ind]);
	}
	static int ccPlayerIndex[]={0,N_CHIP_TYPES*N_CHIP_IMAGES};
	
  /* pre load images and create the canonical pieces
   * 
   */
   static final String[] AltNames = { "light-round-np","dark-round-np","light-square-np","dark-square-np"};
   static final int ChipMap[] = {
	   0,2,0,2,0,2,
	   1,3,1,3,1,3
   };
   
   static final String[] ImageNames = 
       {"white-chip-r1", "white-chip-s1","white-chip-r2","white-chip-s2","white-chip-r3","white-chip-s3",
	   "black-chip-r1","black-chip-s1","black-chip-r2","black-chip-s2","black-chip-r3","black-chip-s3"};
 
   static final int NCHIPTYPES = ImageNames.length;
   
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		Image alt[] = forcan.load_masked_images(ImageDir,AltNames);
		GounkiChip altChip[] = new GounkiChip[alt.length];
		for(int i=0;i<altChip.length;i++)
		{
			altChip[i] = new GounkiChip(i+100,AltNames[i],alt[i],AltScales[i]);
		}
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        GounkiChip CC[] = new GounkiChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	new GounkiChip(i,ImageNames[i],IM[i],SCALES[i],rv,CC,altChip[ChipMap[i]]); 
        	}
        CANONICAL_PIECE = CC;
        // digests for the set of gounki chips are not expected to be unique
        // check_digests(CC);
		}
	}
	public String contentsString() { return(isSquare()?" square":" round"); }


}
