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
package che;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import lib.StockArt;
import online.game.chip;

/**
 * This class is derived from "CheChip" in Che.  There is more documentation about
 * the representation and motivation for the chip class there.
 * 
 * @author ddyer
 *
 */
public class CheChip extends chip<CheChip>
{	public int sweep_counter = 0;
	public int index = 0;			// index into the chips array
	public String name = "";
	public int colorInfo[] = null;
	enum ChipColor { light, dark }
	public ChipColor dotColor;
	// constructor
	private CheChip(int i,int []color,Image im,String na,double[]sc,long ran,String nam)
	{	index = i;
		scale=sc;
		image=im;
		dotColor = ChipColor.values()[index/2];
		colorInfo = color;
		file = na;
		randomv = ran;
		name = nam;
	}
	

	final int intersectionColor()
	{	if(colorInfo!=null) { return(colorInfo[1]); }
		return(0);
	}
	public int chipNumber() { return(index); }
	
	// these numbers make all the tiles the same size, and center them so the tile is
	// centered in the cell.  These are the same chips as used in Che, but slightly
	// resized and recentered to avoid the "bottom line" artifact at large scales.
    static final double[][] SCALES=
    {   {0.576388,0.4861111,1.79166},	// sw-ne light
    	{0.575757,0.485507,1.7999},	// nw-se light
    	{0.61805555,0.4444,1.775},	// sw-ne darkt
    	{0.61805,0.442,1.75}	// nw-se dark
    };
    //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
    static final String[] ImageNames = 
        {   "light-tile-r0", // sw-ne light
            "light-tile-r1",// nw-se light
            "dark-tile-r0",// sw-ne darkt
            "dark-tile-r1"// nw-se dark
        };
    static final String[] ObliqueNames = 
    {   "o-light-tile-r0", // sw-ne light
        "o-light-tile-r1",// nw-se light
        "o-dark-tile-r0",// sw-ne darkt
        "o-dark-tile-r1"// nw-se dark
    };
  
    static final int D = 1;	//dark colored corner
    static final int L = 2;	//light colored corner
    static final int N = 0; //neutral corner
    static final int X = -1; // no connection
    
    static final int[] TileColors[] =
    {
    {D,L,D,L,L},		// 0 "light-tile-r0", 1,3 are connected
    {L,D,L,D,L},		// 2 "light-tile-r1",
    {L,D,L,D,D},		// 1 "dark-tile-r0",
    {D,L,D,L,D},		// 3 "dark-tile-r1",
 
    };
    static final String names[]={"L0","L1","R0","R1"};
    public String chipName() { return(name); }
 	// call from the viewer's preloadImages
    static CheChip CANONICAL_PIECE[] = null;
    static StockArt OBLIQUE_CHIPS[] = null;
    
    static final CheChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    static final int nChips = ImageNames.length;
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(OBLIQUE_CHIPS==null)
		{
		Random rv = new Random(5324324);
        Image IM[]=forcan.load_masked_images(Dir,ImageNames);
        CheChip CC[] = new CheChip[nChips];
        for(int i=0;i<nChips;i++) 
        	{CC[i]=new CheChip(i,TileColors[i],IM[i],ImageNames[i],SCALES[i],rv.nextLong(),names[i]); 
        	}
       CANONICAL_PIECE = CC;
       check_digests(CC);
       // these are pictures used to show the stack of remaining chips
       Image.registerImages(CC);
       OBLIQUE_CHIPS = StockArt.preLoadArt(forcan,Dir,ObliqueNames,SCALES);
       Image.registerImages(OBLIQUE_CHIPS);
		}
	}   
}
