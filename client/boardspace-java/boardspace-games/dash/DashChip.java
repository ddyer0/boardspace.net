
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
package dash;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

public class DashChip extends chip<DashChip> implements DashConstants
{	
	DashColor color = null;
	public DashColor getColor() { return(color); }
	public int colorIndex() { return(color==null ? -1 : color.ordinal()); }
	
	int chipNumber;
	public int colorInfo[] = null;
	public DashChip(DashColor pla,int idx,int []ci,Image im,double sc[],long rn)
	{	color=pla;
		colorInfo = ci;
		chipNumber=idx;
		image = im;
		scale = sc;
		randomv = rn;
		if(pla!=null) { pla.chip = this; }
	}
	


	final int intersectionColor()
	{	if(colorInfo!=null) { return(colorInfo[1]); }
		return(0);
	}


    static final String[] chipColorString = { "L", "D" };

	public String toString()
	{	String cs = "";
		if(colorInfo!=null) 
			{ for(int i=0;i<colorInfo.length;i++) { cs += " "+colorInfo[i]; }
			  return("<chip #"+chipNumber+cs+">");
			}
		return("<chip "+color+">"); 
	}
	
    static private DashChip CANONICAL_PIECE[] = null;	// created by preload_images
    static final int DARK_TILE_INDEX = 0;
    static final int LIGHT_TILE_INDEX = 2;
    static final int N_STANDARD_TILES = 16;
    static final int WHITE_CHIP_INDEX = N_STANDARD_TILES;
    static final int BLACK_CHIP_INDEX = WHITE_CHIP_INDEX+1;

    // note this numbering scheme for the first four
    // tiles has the property that n^1 turns an illegal placement
    // into a legal placement, and n^3 converts a tile into it's
    // acceptable flipped replacement 
    static final int R0_tile_index = 0;
    static final int R1_tile_index = 2;
    
    static final int NW_tile_index = 4;	// the corner tiles are placed explicitly, and
    static final int NE_tile_index = 5;	// act as the seeds that determine the whole board
    static final int SE_tile_index = 6;
    static final int SW_tile_index = 7;
    
    static final int RA_tile_index = 8;	// the side tiles are placed on the correct side
    static final int RB_tile_index = 10;// and a/b flipped to match the existing corners
    static final int LA_tile_index = 12;
    static final int LB_tile_index = 14;
    static final int TA_tile_index = 9;
    static final int TB_tile_index = 11;
    static final int BA_tile_index = 13;
    static final int BB_tile_index = 15;
    
    static final int D = 1;	//dark colored corner
    static final int L = 2;	//light colored corner
    static final int N = 0; //neutral corner
    static final int X = -1; // no connection
    // colors clockwise from NW NE SE SW X
    // X indicates the color that connects through
    static final int[] TileColors[] =
    {
    {D,L,D,L,L},		// 0 "light-tile-r0", 1,3 are connected
    {L,D,L,D,D},		// 1 "dark-tile-r0",
    {L,D,L,D,L},		// 2 "light-tile-r1",
    {D,L,D,L,D},		// 3 "dark-tile-r1",
    // the rest of the tiles are edge tiles, which do not connect
    {N,N,L,N,X},		// 4 "light-tile-nw",  
    {N,N,N,D,X},		// 5 "dark-tile-ne",
    {L,N,N,N,X},		// 6 "light-tile-se",
    {N,D,N,N,X},		// 7 "dark-tile-sw",
    {D,N,N,L,X},		// 8 "light-tile-ra",
    {N,N,L,D,X},		// 9 "dark-tile-ta",
    {L,N,N,D,X},		// 10 "light-tile-rb",
    {N,N,D,L,X},		// 11 "dark-tile-tb",
    {N,L,D,N,X},		// 12 "light-tile-la",
    {L,D,N,N,X},		// 13 "dark-tile-ba",
    {N,D,L,N,X},		// 14 "light-tile-lb",
    {D,L,N,N,X},		// 15 "dark-tile-bb",
    null,null,			// blank chips
    null,null
    };
    // in the current artwork, there are only two actual tiles
    // the "dark center" and "light center".  All the tiles are 
    // repaintins of one of these
    static double light_scale[] = {0.584,0.476,1.53};
    static double dark_scale[] = {0.544,0.440,1.722};
	static double SCALES[][] = 
	{light_scale,	dark_scale,	// light r0 dark r0
	 light_scale,	dark_scale,	// light r1 dark r1
	 light_scale,	dark_scale,	// light nw dark ne
	 light_scale,	dark_scale,	// light r1 dark r1
	 light_scale,	dark_scale,	// light r1 dark r1
	 light_scale,	dark_scale,	// light r1 dark r1
	 light_scale,	dark_scale,	// light r1 dark r1
	 light_scale,	dark_scale,	// light r1 dark r1
	 // these are off-center to match the placement of the truchet tiles
	 {0.35,0.691,1.6},	// red chip	
	 {0.35,0.668,1.6},	// yellow chip
	 {0.35,0.691,1.6},	// blue chip
	 {0.35,0.668,1.6},	// green chip
	};
	
   static final String[] MaskFileNames = 
    {
	"light-tile-mask","dark-tile-mask",
	"light-tile-mask","dark-tile-mask",
	"light-tile-mask","dark-tile-mask",
	"light-tile-mask","dark-tile-mask",
	"light-tile-mask","dark-tile-mask",
	"light-tile-mask","dark-tile-mask",
	"light-tile-mask","dark-tile-mask",
	"light-tile-mask","dark-tile-mask",
	"red-runner-flat-mask","yellow-runner-flat-mask","blue-runner-flat-mask","green-runner-flat-mask",
   };

    static final String[] ImageNames = 
    {
	"light-tile-r0","dark-tile-r0",
	"light-tile-r1","dark-tile-r1",
	
	"light-tile-nw","dark-tile-ne",
	"light-tile-se","dark-tile-sw",

	"light-tile-ra","dark-tile-ta",
	"light-tile-rb","dark-tile-tb",
	"light-tile-la","dark-tile-ba",
	"light-tile-lb","dark-tile-bb",
	"red-runner-flat","yellow-runner-flat","blue-runner-flat","green-runner-flat",

    };
    static public DashChip getChip(int i) { return(CANONICAL_PIECE[i]); }
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		Random r = new Random(833063);
		int nColors = ImageNames.length;
        Image IM[] = forcan.load_images(ImageDir, ImageNames, 
        				forcan.load_images(ImageDir, MaskFileNames)); // load the main images

        DashChip CC[]=new DashChip[nColors];
        for(int i=0;i<nColors;i++)
        {
        DashColor cl = i>=N_STANDARD_TILES ? DashColor.values()[i-N_STANDARD_TILES] : null;
        CC[i] = new DashChip(cl,i,TileColors[i],IM[i],SCALES[i],r.nextLong());	
        }
       CANONICAL_PIECE = CC;
       check_digests(CC);
		}
	}

}
