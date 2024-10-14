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
package spangles;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

public class SpanglesChip extends chip<SpanglesChip>
{
	public int index = 0;
	public int colorIndex = 0;
	
	// constructor
	private SpanglesChip(int i,Image im,String na,double[]sc,long ran,String nam)
	{	index = i;
		colorIndex = i/2;
		scale=sc;
		image=im;
		file = na;
		randomv = ran;
	}
	public int chipNumber() { return(index); }
	
    static final double[][] SCALES=
    {   {0.5,0.611,2.65},	// white up
    	{0.49,0.472,2.79},	// white down
    	{0.52,0.629,2.57},	// yellow up
    	{0.50,0.509,2.83}	// yellow down
   };
    //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
    static final String[] ImageNames = 
        {   "white-up", 
            "white-down",
            "yellow-up",
            "yellow-down"
        };
    static final String names[]={"YU","YD","WU","WD"};
    public String chipName() { return(file); }
	// call from the viewer's preloadImages
    static SpanglesChip CANONICAL_PIECE[] = null;
    static final SpanglesChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    static final SpanglesChip getChip(int color,int dir)
    {	return(CANONICAL_PIECE[color*2+dir]);
    }
    static final int nChips = ImageNames.length;
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(CANONICAL_PIECE==null)
		{
		Random rv = new Random(5312324);
        Image IM[]=forcan.load_masked_images(Dir,ImageNames);
        SpanglesChip CC[] = new SpanglesChip[nChips];
        for(int i=0;i<nChips;i++) 
        	{CC[i]=new SpanglesChip(i,IM[i],ImageNames[i],SCALES[i],rv.nextLong(),names[i]); 
        	}
        CANONICAL_PIECE = CC;
        Image.registerImages(CC);
        check_digests(CC);
		}
	}   
}
