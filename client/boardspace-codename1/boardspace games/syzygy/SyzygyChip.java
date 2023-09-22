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
package syzygy;
import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;

class ChipStack extends OStack<SyzygyChip>
{
	public SyzygyChip[] newComponentArray(int n) { return(new SyzygyChip[n]); }
}
public class SyzygyChip extends chip<SyzygyChip>
{
	public int index = 0;
	public String name = "";
	public int value = 0;
	// constructor
	
	private SyzygyChip(int i0,Image im,String na,double[]sc,long ran,String nam,int va)
	{	index = i0;
		scale=sc;
		value = va;
		image=im;
		file = na;
		randomv = ran;
		name = nam;
		
	}
	public int chipNumber() { return(index); }

	static final double[][] SCALES=
    {   	// left
    	{0.5,0.5,1.39},	// planet1
    	{0.50,0.50,1.39},	// planet2
    	{0.50,0.50,1.39},	// planet3
     	{0.5,0.500,1.39},	// planet4
    	{0.5,0.500,2.0},	// asteroid
      };
	static final int values[] = {1,2,3,4,0,0,0,0};
	static final int PLANET1_INDEX = 0;
	static final int PLANET2_INDEX = 1;
	static final int PLANET3_INDEX = 2;
	static final int PLANET4_INDEX= 3;
	static final int ASTEROID_INDEX = 4;
   //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
    static final String[] ImageNames = 
        {   "yellowplanet", 
            "greenplanet",
            "blueplanet",
            "redplanet",
             "asteroid"
        };

    static final String names[]={"yellow","green","blue","red","asteroid"};
    
    static long Digest(Random r,SyzygyChip c)
    {
    	return(c==null ? r.nextLong() : c.Digest(r));
    }
    public String chipName() { return(name); }
	// call from the viewer's preloadImages
    static SyzygyChip CANONICAL_PIECE[] = null;
    static final SyzygyChip getChip(int n) { return(CANONICAL_PIECE[n]); }
    static final int nChips = 4;
    static SyzygyChip Asteroid = null;
 	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(CANONICAL_PIECE==null)
		{
		int nImages = ImageNames.length;
		Random rv = new Random(5312324);
        Image IM[]=forcan.load_masked_images(Dir,ImageNames);
        SyzygyChip CC[] = new SyzygyChip[nImages];
        for(int i=0;i<nImages;i++) 
        	{CC[i]=new SyzygyChip(i,IM[i],ImageNames[i],SCALES[i],rv.nextLong(),names[i],values[i]); 
        	}
        Asteroid = CC[ASTEROID_INDEX];
        CANONICAL_PIECE = CC;
        check_digests(CC);
		}
	}   
}
