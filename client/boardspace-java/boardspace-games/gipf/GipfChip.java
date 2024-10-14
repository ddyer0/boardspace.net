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
package gipf;

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
public class GipfChip extends chip<GipfChip>
{	
	public int drawingIndex;		// index for drawing
	public int colorIndex;			// index for owning player
    static final int FIRST_CHIP_INDEX = 0;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX;
    static final int BLACK_CHIP_INDEX = WHITE_CHIP_INDEX+3;
	public int pieceNumber() { return(drawingIndex); }
	static final char WhiteChipName = 'W';
	static final char BlackChipName = 'B';
	GipfChip altChip = null;
	private GipfChip(int pla,String na,Image im,double []sc,long ran)
	{	colorIndex = drawingIndex=pla;
		file = na;
		image = im;
		randomv = ran;
		scale = sc;
	}
    private static GipfChip CANONICAL_PIECE[] = null;	// created by preload_images
    public GipfChip getAltChip(int cc) { return ( ((altChip==null)||(cc==0)) ? this : altChip); }
    
    private static double SCALES[][] =
    {	{0.57,0.41,1.63},
    	{0.54,0.43,1.65}   	
    };
    private static double ALTSCALES[][] =
    {	{0.5,0.5,1.3},
    	{0.5,0.5,1.4}   	
    };
	
	public static GipfChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}

  /* pre load images and create the canonical pieces
   * 
   */
 
	   private static final String ImageNames[] =
	   	{"white","black"};
	   
	   private static final String FlatImageNames[] =
		   	{"white-flat","black-flat"};
	    
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        Image FL[]=forcan.load_masked_images(ImageDir,FlatImageNames);
        GipfChip CC[] = new GipfChip[nColors];
        Random rv = new Random(4953);
        for(int i=0;i<nColors;i++)
        	{ GipfChip ch = new GipfChip(i,ImageNames[i],IM[i],SCALES[i],rv.nextLong());
        	  GipfChip al = new GipfChip(i,ImageNames[i],FL[i],ALTSCALES[i],0);
        	  ch.altChip = al;
        	  CC[i] = ch;
        	}
        CANONICAL_PIECE = CC;
        Image.registerImages(CC);
        check_digests(CC);
		}
	}


}