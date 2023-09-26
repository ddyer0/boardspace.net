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
package tumble;

import bridge.Config;
import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

public class TumbleChip extends chip<TumbleChip> implements Config
{
	int chipNumber;
	public TumbleCell location=null;
	public TumbleChip(int pla,int idx,String fil,Image im,double []sc,long rv)
	{	
		chipNumber=idx;
		file = fil;
		image = im;
		randomv = rv;
		scale = sc;
	}
	boolean isChip() { return(chipNumber<2); }
	boolean isTile() { return(chipNumber>=2); }
	
    static final String[] ImageNames = 
       {
     	"white-chip-np","black-chip-np",
     	"light-tile","dark-tile"
       };
	static final int FIRST_TILE_INDEX = 2;
    static final int N_STANDARD_TILES = 2;
    static final int N_STANDARD_CHIPS = 2;
    static final int FIRST_CHIP_INDEX = 0;
    static final int BLACK_CHIP_INDEX = FIRST_CHIP_INDEX+1;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX;

	private static double SCALES[][] = 
	{
	 {0.514,0.432,1.38},	// white chip
	 {0.514,0.432,1.38},	// dark chip
	 {0.5,0.5,0.98},	// light square
	 {0.5,0.5,0.98},	// dark square
	};
	static TumbleChip[]CANONICAL_PIECE = null;
	static TumbleChip getChip(int n) { return(CANONICAL_PIECE[n]);}
	static TumbleChip getTile(int n) { return(CANONICAL_PIECE[FIRST_TILE_INDEX+n]); }
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
		Random r = new Random(4096230);
        Image IM[]=forcan.load_masked_images(StonesDir,ImageNames);
        TumbleChip CC[] = new TumbleChip[nColors];
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new TumbleChip((i<2?i:-1),i,ImageNames[i],IM[i],SCALES[i],r.nextLong()); 
        	}
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}

}
