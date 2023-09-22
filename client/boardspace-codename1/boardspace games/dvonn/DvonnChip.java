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
package dvonn;

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
public class DvonnChip extends chip<DvonnChip>
{	
	public int colorIndex;
	String colorString ;
	DvonnChip altChip = null;
	public String colorString() { return(colorString); }
    static final int FIRST_CHIP_INDEX = 0;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX;
    static final int BLACK_CHIP_INDEX = WHITE_CHIP_INDEX+1;
    static final int DVONN_CHIP_INDEX = BLACK_CHIP_INDEX+1;
    public boolean isDvonn() { return(colorIndex==DVONN_CHIP_INDEX); }
	public int pieceNumber() { return(colorIndex); }
	public DvonnChip getAltChip(int set) {return ( ((set==0)||(altChip==null)) ? this : altChip); }
	private DvonnChip(int pla,String fil,Image im,double []sc,long rv,String co)
	{	colorIndex=pla;
		file = fil;
		image = im;
		scale = sc;
		randomv = rv;
		colorString = co;
	}
		
    public static DvonnChip CANONICAL_PIECE[] = null;	// created by preload_images
    
	private static double SCALES[][] =
    {	
    	{0.564,0.550,1.10},		// white chip
       	{0.530,0.513,1.10},		// dark chip
       	{0.542,0.517,1.30},		// dvonn chip
       	
       	{0.576,0.498,1.056},		// white flat chip
       	{0.561,0.464,0.933},		// dark flat chip
       	{0.542,0.508,1.049}		// dvonn flat chip
           };
	
	public static DvonnChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}

  /* pre load images and create the canonical pieces
   * 
   */
 
   static final String[] ImageFileNames = 
       {"white","black","red","white-flat","black-flat","red-flat"};
   public static final char WhiteChipName = 'W';
   public static final char BlackChipName = 'B';
   static final String[] chipColorString = { "W", "B", "D","W", "B", "D"};

	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageFileNames.length;
         Image IM[]=forcan.load_masked_images(ImageDir,ImageFileNames);
        DvonnChip CC[] = new DvonnChip[nColors];
        Random r = new Random(6385683);
        for(int i=0;i<nColors;i++) 
        	{CC[i]=new DvonnChip(i,ImageFileNames[i],IM[i],SCALES[i],r.nextLong(),chipColorString[i]); 
        	}
        for(int i=0;i<nColors/2;i++)
        { CC[i].altChip = CC[i+nColors/2];
        }
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}


}
