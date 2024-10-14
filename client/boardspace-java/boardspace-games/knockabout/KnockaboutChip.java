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
package knockabout;

import lib.Image;
import lib.ImageLoader;
import knockabout.KnockaboutConstants.KnockId;
import lib.G;
import lib.Random;
import online.common.OnlineConstants;
import online.game.*;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class KnockaboutChip extends chip<KnockaboutChip> implements OnlineConstants
{	public int index;
	public int numberShowing;		// index for piece type
	public KnockId color;			// index for owning player
	public int numSides;			// number of sides this die
	public int pieceNumber() { return(index); }
    static final int FIRST_CHIP_INDEX = 0;
    static final int WHITE_CHIP_INDEX = FIRST_CHIP_INDEX;
    static final int BLACK_CHIP_INDEX = WHITE_CHIP_INDEX+3;
	static final char WhiteChipName = 'W';
	static final char BlackChipName = 'B';
	static String colorNames[] = {"B","W","Y"};
	static int D4_OFFSET = 0;
	static int D6_OFFSET = 8;
	static int D8_OFFSET = D6_OFFSET+12;
	static int dieOff[]={D4_OFFSET,9999,D6_OFFSET,9999,D8_OFFSET};
	static KnockaboutChip getChip(int n) { return(CANONICAL_PIECE[n]); }
	static KnockaboutChip getChip(KnockId pl,int die, int face)
	{	int pindex = 0;
		switch(pl)
		{	case WhiteDie:	pindex = 0; break;
			case BlackDie: pindex = 1; break;
			default: throw G.Error("Not index not defined");
		}
		return(getChip(pindex*die+face-1+dieOff[die-4]));	
	}
	public int add(int n) 
		{ int next = (numberShowing+n-1)%numSides;
		  if(next<0) { next+= numSides; }
		  return(next+1);
		}
		  
	private KnockaboutChip(int idx,Image im,double[]sc,String na,int npla,int nsides,int nsho,long rr)
	{	image=im;
		scale = sc;
		file=na;
		index = idx;
		color = npla==0 ? KnockId.WhiteDie:KnockId.BlackDie;
		numSides = nsides;
		numberShowing = nsho;
		randomv = rr;
	}

		
    static KnockaboutChip CANONICAL_PIECE[] = null;	// created by preload_images
    
    private static double SCALES[][] =
    {	{0.6,0.47,1.45},	// white d4
    	{0.6,0.47,1.3},
    	{0.6,0.47,1.33},
    	{0.6,0.47,1.338},
    	
    	{0.6,0.47,1.433},	// black d4
    	{0.6,0.47,1.306},
    	{0.6,0.47,1.466},
    	{0.6,0.47,1.3},
    	
    	//{0.6,0.47,1.2},	// yellow d4
    	//{0.6,0.47,1.2},
    	//{0.6,0.47,1.2},
    	//{0.6,0.47,1.2},
    	
    	{0.6,0.47,1.392},	// white d6
    	{0.6,0.47,1.366},
    	{0.6,0.47,1.266},
    	{0.6,0.47,1.363},
       	{0.6,0.47,1.46},
    	{0.6,0.47,1.28},
 
       	{0.6,0.47,1.4},	// black d6
    	{0.6,0.47,1.453},
    	{0.6,0.47,1.35},
    	{0.6,0.47,1.5},
       	{0.6,0.47,1.56},
    	{0.6,0.47,1.35},
    	
       	//{0.6,0.47,1.2},	// yellow d6
    	//{0.6,0.47,1.2},
    	//{0.6,0.47,1.2},
    	//{0.6,0.47,1.2},
       	//{0.6,0.47,1.2},
    	//{0.6,0.47,1.2},

      	{0.6,0.47,1.3},	// white d8
    	{0.6,0.47,1.37},
    	{0.6,0.47,1.3},
    	{0.6,0.47,1.3},
       	{0.53,0.487,1.35},
    	{0.6,0.47,1.238},
      	{0.6,0.47,1.4},
    	{0.6,0.47,1.28},

      	{0.6,0.47,1.307},	// black d8
    	{0.6,0.47,1.307},
    	{0.6,0.47,1.3},
    	{0.6,0.47,1.3},
       	{0.6,0.47,1.33},
    	{0.6,0.47,1.2},
      	{0.6,0.47,1.3},
    	{0.6,0.47,1.25}

      	//,{0.6,0.47,1.2}	// yellow d8
    	//,{0.6,0.47,1.2}
    	//,{0.6,0.47,1.2}
    	//,{0.6,0.47,1.2}
       	//,{0.6,0.47,1.2}
    	//,{0.6,0.47,1.2}
      	//,{0.6,0.47,1.2}
    	//,{0.6,0.47,1.2}

    };
	

  /* pre load images and create the canonical pieces
   * 
   */

   static final String[] ImageFileNames = 
       {
	   "white-d4-1","white-d4-2","white-d4-3","white-d4-4"
	   ,"black-d4-1","black-d4-2","black-d4-3","black-d4-4"
	  // ,"yellow-d4-1","yellow-d4-2","yellow-d4-3","yellow-d4-4"
	   
	   ,"white-d6-1","white-d6-2","white-d6-3","white-d6-4","white-d6-5","white-d6-6"
	   ,"black-d6-1","black-d6-2","black-d6-3","black-d6-4","black-d6-5","black-d6-6"
	  // ,"yellow-d6-1","yellow-d6-2","yellow-d6-3","yellow-d6-4","yellow-d6-5","yellow-d6-6"
	   
	   ,"white-d8-1","white-d8-2","white-d8-3","white-d8-4"
	   ,"white-d8-5","white-d8-6","white-d8-7","white-d8-8"
	   ,"black-d8-1","black-d8-2","black-d8-3","black-d8-4"
	   ,"black-d8-5","black-d8-6","black-d8-7","black-d8-8"
	  // ,"yellow-d8-1","yellow-d8-2","yellow-d8-3","yellow-d8-4"
	  // ,"yellow-d8-5","yellow-d8-6","yellow-d8-7","yellow-d8-8"
	   
      };
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageFileNames.length;
        Image IM[]=forcan.load_masked_images(DICEPATH,ImageFileNames);
        KnockaboutChip CC[] = new KnockaboutChip[nColors];
  
        Random rv = new Random(1343535);

        
        for(int die = 4,idx=0; die<=8;  die+=2)
        { for(int pl = 0; pl<=1; pl++)
        	{ for(int num = 1; num<=die; num++)
        		{ CC[idx] = new KnockaboutChip(idx,IM[idx],SCALES[idx],ImageFileNames[idx],
        						pl,die,num,rv.nextLong());
        		  idx++;
        		}
        	}
        }

        CANONICAL_PIECE = CC;
        Image.registerImages(CC);
        check_digests(CC);
		}
	}
}
