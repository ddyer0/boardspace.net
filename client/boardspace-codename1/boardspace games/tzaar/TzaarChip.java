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
package tzaar;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class TzaarChip extends chip<TzaarChip>
{	enum ChipColor {Black,White};
	public int drawingIndex;		// index for drawing
	public int typeIndex;			// index for piece type
	ChipColor color;			// index for owning player
	public int typeMask = 0;		// bitmask so we can check win conditions quickly
	public TzaarChip altChip = null;			// alternate without perspective
	static final int TOTT_TYPE_MASK = 1;
	static final int TZAAR_TYPE_MASK = 2;
	static final int TZARRA_TYPE_MASK = 4;
	static final int ALLTYPES_MASK = TOTT_TYPE_MASK|TZAAR_TYPE_MASK|TZARRA_TYPE_MASK;
    static final int FIRST_CHIP_INDEX = 0;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX;
    static final int BLACK_CHIP_INDEX = WHITE_CHIP_INDEX+3;
    static final int[] TYPEMASKS = { TOTT_TYPE_MASK,TZAAR_TYPE_MASK,TZARRA_TYPE_MASK};
 	public int pieceNumber() { return(drawingIndex); }
	static final int W_TOTT_INDEX = 0;
	static final int W_TZARRA_INDEX = 1;
	static final int W_TZAAR_INDEX = 2;
	static final int B_TOTT_INDEX = 3;
	static final int B_TZARRA_INDEX = 4;
	static final int B_TZAAR_INDEX = 5;
	public TzaarChip getAltChip(int set) { return ( ((set==0)||(altChip==null)) ? this : altChip); }
	static final int Initial_Count[] = {15,9,6};
	static final int NTYPES = Initial_Count.length;
	
 
	private TzaarChip(int pla,String na,Image im,double sc[],long rv)
	{	drawingIndex=pla;
		file = na;
	 	typeIndex = pla%NTYPES;
	 	color = (pla/NTYPES==0) ? ChipColor.White : ChipColor.Black;
	 	typeMask = TYPEMASKS[typeIndex];
	 	image=im;
	 	scale = sc;
	 	randomv = rv;
	}
		
    private static TzaarChip CANONICAL_PIECE[] = null;	// created by preload_images
    
    private static double SCALES[] = {0.473,0.47,1.2};

    private static double ALTSCALES[][] =
    {	{0.473,0.47,1.05},
    	{0.473,0.47,1.0},
    	{0.473,0.47,1.0},
    	{0.473,0.47,1.05},
    	{0.473,0.47,1.0},
    	{0.473,0.47,1.0}
            };
	
	public static TzaarChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}
	public static TzaarChip getChip(int type,int player)
	{	return(CANONICAL_PIECE[type+player*3]);
	}

   static final String[] ImageFileNames = 
       {"white-tott","white-tzarra","white-tzaar",
	   "black-tott","black-tzarra","black-tzaar"};
   static final String[] ImageFileNamesFlat = 
       {"white-tott-flat","white-tzarra-flat","white-tzaar-flat",
	   "black-tott-flat","black-tzarra-flat","black-tzaar-flat"};
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageFileNames.length;
		Random r = new Random(6399563);
        Image IM[]=forcan.load_masked_images(ImageDir,ImageFileNames);
        Image IMAlt[]=forcan.load_masked_images(ImageDir,ImageFileNamesFlat);

        TzaarChip CC[] = new TzaarChip[nColors];
        for(int i=0;i<nColors;i++)
        	{ TzaarChip chip = new TzaarChip(i,ImageFileNames[i],IM[i],SCALES,r.nextLong());
        	  TzaarChip alt = new TzaarChip(i,ImageFileNamesFlat[i],IMAlt[i],ALTSCALES[i],0);
        	  chip.altChip = alt;
        	  CC[i] = chip;
        	}
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}


}
