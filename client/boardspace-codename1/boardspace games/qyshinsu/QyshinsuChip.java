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
package qyshinsu;

import lib.Graphics;
import lib.Image;

import lib.Random;
import lib.exCanvas;
import lib.ImageLoader;
import lib.OStack;
import online.game.*;

class ChipStack extends OStack<QyshinsuChip>
{
	public QyshinsuChip[] newComponentArray(int n) { return(new QyshinsuChip[n]); }
}
/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class QyshinsuChip extends chip<QyshinsuChip>
{	
	public int chipIndex;
	public int typeIndex;
	private int colorIndex;
	public int colorIndex() { return(colorIndex); }
	public int pieceNumber() { return(chipIndex); }
	
    static final int N_STANDARD_CHIPS = 6;
    static final int FIRST_CHIP_INDEX = 0;
    static final int BLACK_CHIP_INDEX = FIRST_CHIP_INDEX+0;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX+N_STANDARD_CHIPS;
    static final int GHOST_CHIP_INDEX = WHITE_CHIP_INDEX+N_STANDARD_CHIPS;
    static final int DARK_MASK_INDEX = GHOST_CHIP_INDEX+1;

    static final String[] chipColorString = { "D" ,"L"};
    static final double CommonScale[] = {0.5,0.5,1.0};
	private QyshinsuChip(int pla,long rv)
	{	chipIndex=pla;
		typeIndex = pla%N_STANDARD_CHIPS;
		colorIndex = pla/N_STANDARD_CHIPS;
		scale = CommonScale;
		randomv = rv;
	}
	public String toString()
	{	return("<"+ COLORNAMES[chipIndex]+">");
	}
	public String contentsString()
	{	return("["+chipColorString[colorIndex]+typeIndex+"]");
	}

		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
	static Image IMAGES[] = null;	// loaded by preload_images
    static QyshinsuChip CANONICAL_PIECE[] = null;	// created by preload_images
    static final String COLORNAMES[] = 
    	{"B Old Stone",
    	 "B 1 Stone",
    	 "B 2 Stone",
    	 "B 3 Stone",
    	 "B 4 Stone",
    	 "B 5 Stone",
    	 "W Old Stone",
    	 "W 1 Stone",
    	 "W 2 Stone",
    	 "W 3 Stone",
    	 "W 4 Stone",
    	 "W 5 Stone",
    	 "Ghost Stone",
    	 "Dark Mask"};

    static double SCALES[][] =
    {	{0.5,0.5,1.15},		// black 0
    	{0.536,0.5,1.15},		// black 1
    	{0.536,0.477,1.15},		// black 2
    	{0.526,0.500,1.15},		// black 3
    	{0.529,0.470,1.15},		// black 4
    	{0.5,0.5,1.15},		// black 5
    	
       	{0.515,0.472,1.15},		// white 0
       	{0.514,0.500,1.15},		// white 1
       	{0.544,0.485,1.15},		// white 2
       	{0.500,0.484,1.15},		// white 3
       	{0.536,0.5,1.15},		// white 4
       	{0.522,0.492,1.15},		// white 5
       	{0.5,0.5,1.15},		// ghost stone
      	{0.5,0.5,1.15}		// dark mask
      	
    };
   public static final int nChips = COLORNAMES.length;
    
 	
	public double[] getScale()
	{	return(SCALES[chipIndex]);
	}
	public Image getImage(ImageLoader canvas)
	{	return(IMAGES[chipIndex]);
	}
	public static Image getImage(int n)
	{	return(IMAGES[n]);
	}
	public static double[] getScale(int idx)
	{
		return(SCALES[idx]);
	}
	public static QyshinsuChip getChip(int color)
	{	return(CANONICAL_PIECE[color]);
	}

  /* pre load images and create the canonical pieces
   * 
   */
 
   static final String[] ImageFileNames = 
       {"black-0","black-1","black-2","black-3","black-4","black-5",
	   "white-0","white-1","white-2","white-3","white-4","white-5",
	   "ghost","dark-ring"
	   };
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		Random r = new Random(234736);
        Image [] icemasks = forcan.load_images(ImageDir,ImageFileNames,"-mask");
        Image IM[]=forcan.load_images(ImageDir,ImageFileNames,icemasks);
        QyshinsuChip CC[] = new QyshinsuChip[nChips];
        for(int i=0;i<nChips;i++) {CC[i]=new QyshinsuChip(i,r.nextLong()); }
        IMAGES = IM;
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}
	
	public void drawChipImage(Graphics gc,exCanvas canvas,int cx,int cy,double pscale[],
			int SQUARESIZE,double xscale,double jitter,String label,boolean artcenter)
	{    super.drawChipImage(gc,canvas,cx,cy,pscale,SQUARESIZE,xscale,jitter,label,artcenter);
		if((chipIndex>=BLACK_CHIP_INDEX)
			&&(chipIndex<(BLACK_CHIP_INDEX+N_STANDARD_CHIPS))
			&& ((QyshinsuViewer)canvas).colorBlind)
		{ CANONICAL_PIECE[DARK_MASK_INDEX].drawChipImage(gc,canvas,cx,cy,pscale,SQUARESIZE,xscale,jitter,null,artcenter);	
		}
	}

}
