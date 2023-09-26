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
package volo;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

/**
 * this is a specialization of {@link chip} to represent the stones used by volo
 * 
 * @author ddyer
 *
 */
public class VoloChip extends chip<VoloChip>
{
	private int index = 0;
	// constructor
	private VoloChip(int i,Image im,String na,double[]sc,long ran)
	{	index = i;
		scale=sc;
		image=im;
		file = na;
		randomv = ran;
	}
	public int chipNumber() { return(index); }
	
    static final double[][] SCALES=
    {   {0.59,0.51,2.07},	// orange stone
    	{0.59,0.51,2.07},	// blue stone
    	{0.5,0.47,1.2},
    	{0.5,0.47,1.2}
    };
    //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
    static final String[] ImageNames = 
        {   "yellow", 
            "blue",
            "orangeo",
            "blueo"
        };
	// call from the viewer's preloadImages
    static VoloChip CANONICAL_PIECE[] = null;
    static VoloChip Blue = null;
    static VoloChip Orange = null;
    static VoloChip BlueO = null;
    static VoloChip OrangeO = null;
    // indexes into the balls array, usually called the rack
    static final VoloChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images (all two of them) into
     * a static array of VoloChip which are used by all instances of the
     * game.
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(CANONICAL_PIECE==null)
		{
		Random rv = new Random(5312324);
		int nColors = ImageNames.length;
		// load the main images, their masks, and composite the mains with the masks
		// to make transparent images that are actually used.
        Image IM[]=forcan.load_masked_images(Dir,ImageNames);
        VoloChip CC[] = new VoloChip[nColors];
        for(int i=0;i<nColors;i++) 
        	{CC[i]=new VoloChip(i,IM[i],ImageNames[i],SCALES[i],rv.nextLong()); 
        	}
        Blue = CC[1];
        Orange = CC[0];
        BlueO = CC[2];
        OrangeO = CC[3];
        CANONICAL_PIECE = CC;
        check_digests(CC);	// verify that the chips have different digests
		}
	}   
}
