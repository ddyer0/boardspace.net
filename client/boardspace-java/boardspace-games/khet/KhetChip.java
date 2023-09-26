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
package khet;


import lib.Image;
import lib.ImageLoader;
import lib.G;
import lib.Random;
import online.game.cell.Geometry;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class KhetChip extends chip<KhetChip> implements KhetConstants
{	
	private int chipIndex;
	int colorIndex() { return(chipIndex>=N_STANDARD_CHIPS?0:1);}
	private int rotation;
	private String name = "";
	private KhetPieceType type;
	private KhetChip next_rot;
	private KhetChip prev_rot;
	private int[] bounces;
	public KhetPieceType getType() { return(type); }
	public int chipNumber() { return(chipIndex); }
	public int getRotation() { return(rotation); }
	enum KhetPieceType
    {	Pharoh(100),
    	Djed(50),
    	Pyramid(25),
    	Annubus(15),
    	Eye(50),
    	Sphinx(1),
    	Blast(1);		// not a piece but a graphic effect
    	double woodValue;
    	KhetPieceType(int rr) { woodValue = rr ; }
    }
	public boolean isPharoh() { return(type==KhetPieceType.Pharoh); }
	public boolean isSphinx() { return(type==KhetPieceType.Sphinx); }
	public int laserDirection()
	{
		G.Assert(isSphinx(), "only the sphinx can shoot");
		switch(chipIndex % N_STANDARD_CHIPS)
		{
		default: throw G.Error("Not expecting colorIndex");
		case SPHINX_R0_INDEX:
			return(KhetBoard.CELL_DOWN());
		case SPHINX_R1_INDEX:
			return(KhetBoard.CELL_LEFT());
		case SPHINX_R2_INDEX:
			return(KhetBoard.CELL_UP());
		case SPHINX_R3_INDEX:
			return(KhetBoard.CELL_RIGHT());
		}
	}
	public double woodValue() { return(type.woodValue); }
	public KhetChip getInverted()
	{
		if(next_rot!=null) { return(next_rot.next_rot); }	// flip 2 
		return(this);
	}
	public KhetChip getAltChip(int set)
	{	return(((set&1)!=0) ? getInverted() : this);	// this is used to get the rotated chip when viewing the inverted board
	}
	
	public KhetChip getRotated(int dir)
	{	G.Assert(next_rot!=null,"Can't rotate %s", type);
		switch(dir)
		{
		default: throw G.Error("Undefined rotaton");
		case RotateCW: 	return(next_rot);
		case RotateCCW: return(prev_rot);
		}
	}
	public boolean canRotate() { return(next_rot!=null); } 
	public boolean canRotateToward(KhetCell c)
	{
		if((type==KhetPieceType.Sphinx) && (c==null)) { return(false); }
		return(canRotate());
	}
	public boolean canMove() { return(type!=KhetPieceType.Sphinx); }	// all other pieces can move
	public boolean isEye() { return(type==KhetPieceType.Eye); }
	public boolean canSwapWith(KhetChip other)
		{ return( ((type==KhetPieceType.Djed) || (type==KhetPieceType.Eye))
					&& ((other.type==KhetPieceType.Pyramid)||(other.type==KhetPieceType.Annubus) )
				); }
	public int bounceDirection(int incoming) 
		{
		G.Assert((incoming&1)==1,"direction should be odd");
		return(bounces[(incoming+Geometry.Oct.n)%Geometry.Oct.n/2]); 
		}
	
    static final int FIRST_CHIP_INDEX = 0;
    static final int PHAROH_INDEX = FIRST_CHIP_INDEX;
    static final int DJED_R0_INDEX = PHAROH_INDEX + 1;
    static final int DJED_R1_INDEX = DJED_R0_INDEX + 1;
    static final int EYE_R0_INDEX = DJED_R1_INDEX + 1;
    static final int EYE_R1_INDEX = EYE_R0_INDEX + 1;
    static final int PYRAMID_R0_INDEX = EYE_R1_INDEX+1;
    static final int PYRAMID_R1_INDEX = PYRAMID_R0_INDEX+1;
    static final int PYRAMID_R2_INDEX = PYRAMID_R1_INDEX+1;
    static final int PYRAMID_R3_INDEX = PYRAMID_R2_INDEX+1;
    static final int ANNUBUS_R0_INDEX = PYRAMID_R3_INDEX+1;
    static final int ANNUBUS_R1_INDEX = ANNUBUS_R0_INDEX+1;
    static final int ANNUBUS_R2_INDEX = ANNUBUS_R1_INDEX+1;
    static final int ANNUBUS_R3_INDEX = ANNUBUS_R2_INDEX+1;
    static final int SPHINX_R0_INDEX = ANNUBUS_R3_INDEX+1;
    static final int SPHINX_R1_INDEX = SPHINX_R0_INDEX+1;
    static final int SPHINX_R2_INDEX = SPHINX_R1_INDEX+1;
    static final int SPHINX_R3_INDEX = SPHINX_R2_INDEX+1;
    
    
    static final int BLAST_INDEX = (SPHINX_R3_INDEX+1)*2;

    static final int N_STANDARD_CHIPS = SPHINX_R3_INDEX+1;
    static KhetChip Blast = null;
    
    static final int[][] KHET_CLASSIC_START = {
    	{0,SPHINX_R2_INDEX,'J',1},
    	{1,SPHINX_R0_INDEX,'A',8},
    	{1,PYRAMID_R0_INDEX,'A',4},
    	{1,PYRAMID_R3_INDEX,'A',5},
       	{0,PYRAMID_R1_INDEX,'J',4},
    	{0,PYRAMID_R2_INDEX,'J',5},
    	{0,DJED_R0_INDEX,'E',4},
       	{0,DJED_R1_INDEX,'F',4},
       	{1,DJED_R0_INDEX,'F',5},
       	{1,DJED_R1_INDEX,'E',5},
    	{0,ANNUBUS_R2_INDEX,'D',1},
       	{0,PHAROH_INDEX,'E',1},
    	{0,ANNUBUS_R2_INDEX,'F',1},
    	{1,ANNUBUS_R0_INDEX,'E',8},
    	{1,PHAROH_INDEX,'F',8},
       	{1,ANNUBUS_R0_INDEX,'G',8},
	
       	{0,PYRAMID_R2_INDEX,'C',1},
       	{0,PYRAMID_R2_INDEX,'C',4},
       	{0,PYRAMID_R1_INDEX,'C',5},
       	{0,PYRAMID_R2_INDEX,'D',6},
       	{0,PYRAMID_R3_INDEX,'H',2},
       	
       	{1,PYRAMID_R1_INDEX,'C',7},
       	{1,PYRAMID_R0_INDEX,'G',3},
       	{1,PYRAMID_R3_INDEX,'H',4},
       	{1,PYRAMID_R0_INDEX,'H',8},
       	{1,PYRAMID_R0_INDEX,'H',5}
    };
    static final int[][] KHET_IHMOTEP_START = {
    	{0,SPHINX_R2_INDEX,'J',1},
    	{1,SPHINX_R0_INDEX,'A',8},
    	{0,PYRAMID_R2_INDEX,'B',4},
    	{0,PYRAMID_R1_INDEX,'B',5},
    	{0,DJED_R0_INDEX,'C',1},
    	{0,ANNUBUS_R2_INDEX,'D',1},
    	{0,PYRAMID_R1_INDEX,'D',3},
    	{0,PYRAMID_R2_INDEX,'D',6},
    	{0,PHAROH_INDEX,'E',1},
    	{0,EYE_R0_INDEX,'E',4},
    	{0,PYRAMID_R0_INDEX,'E',5},
    	{0,ANNUBUS_R2_INDEX,'F',1},
    	{0,PYRAMID_R1_INDEX,'J',4},
    	{0,PYRAMID_R2_INDEX,'J',5},
    	{1,PYRAMID_R0_INDEX,'A',4},
    	{1,PYRAMID_R3_INDEX,'A',5},
    	{1,ANNUBUS_R0_INDEX,'E',8},
    	{1,PHAROH_INDEX,'F',8},
    	{1,EYE_R0_INDEX,'F',5},
    	{1,DJED_R0_INDEX,'H',8},
    	{1,PYRAMID_R2_INDEX,'F',4},
    	{1,PYRAMID_R0_INDEX,'G',3},
    	{1,PYRAMID_R3_INDEX,'G',6},
    	{1,ANNUBUS_R0_INDEX,'G',8},
    	{1,PYRAMID_R3_INDEX,'I',4},
    	{1,PYRAMID_R0_INDEX,'I',5}
    	
    };

    /* constructor */
	private KhetChip(String na,int pla,Image im,long rv,double scl[],KhetPieceType tt,int []bo,int ro)
	{	name = na;
		chipIndex=pla;
		image = im;
		randomv = rv;
		scale = scl;
		type = tt;
		bounces = bo;
		rotation = ro;
	}
	public String toString()
	{	return("<"+ name+" #"+chipIndex+">");
	}
	public String contentsString() 
	{ return(name); 
	}
		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private KhetChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	
    	
    	{0.687,0.624,1.572},		// red pharoh
    	{0.664,0.630,1.615},		// red djed r0
    	{0.728,0.670,1.523},		// red djed r1
    	{0.722,0.626,1.855},		// red eye r0
    	{0.674,0.608,1.867},		// red eye r1
    	{0.618,0.520,1.838},		// red pyramid r0
    	{0.670,0.485,1.780},		// red pyramid r1
    	{0.653,0.572,1.826},		// red pyramid r2
    	{0.658,0.502,1.606},		// red pyramid r3
    	{0.664,0.630,1.491},		// red annubus r0
    	{0.699,0.601,1.641},		// red annubus r1
    	{0.728,0.624,1.826},		// red annubus r2
    	{0.707,0.584,1.846},		// red annubus r3
    	{0.614,0.548,1.530},		// red sphinx r0
    	{0.662,0.596,1.349},		// red sphinx r1
    	{0.650,0.584,1.590},		// red sphinx r2
    	{0.692,0.596,1.530},		// red sphinx r3   	
    	
    	{0.687,0.583,1.468},		// silver pharoh
    	{0.710,0.670,1.569},		// silver djed r0
    	{0.722,0.666,1.723},		// silver djed r1
    	{0.746,0.668,1.903},		// silver eye r0
    	{0.653,0.600,1.938},		// silver eye r1
    	{0.578,0.497,1.826},		// silver pyramid r0
    	{0.682,0.477,1.744},		// silver pyramid r1
    	{0.653,0.595,1.734},		// silver pyramid r2
    	{0.682,0.531,1.560},		// silver pyramid r3
    	
    	{0.687,0.618,1.445},		// silver annubus r0
    	{0.705,0.606,1.572},		// silver annubus r1
    	{0.682,0.618,1.745},		// silver annubus r2
    	{0.653,0.537,1.884},		// silver annubus r3
    	
    	{0.586,0.576,1.533},		// silver sphinx r0
    	{0.666,0.626,1.402},		// silver sphinx r1
    	{0.692,0.569,1.533},		// silver sphinx r2
    	{0.635,0.518,1.544},		// silver sphinx r3
    	
    	{ 0.5,0.5,0.75},	// blast
    	};
     
    static final int Smoke = -1;
    static final int Absorb = -2;
    static private int BOUNCES[][] =
    {	
    	
    	{Smoke,Smoke,Smoke,Smoke},		//  gold pharoh
    	{KhetBoard.CELL_DOWN(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_UP(),KhetBoard.CELL_LEFT()},		//  djed r0
    	{KhetBoard.CELL_UP(),KhetBoard.CELL_LEFT(),KhetBoard.CELL_DOWN(),KhetBoard.CELL_RIGHT()},		//  djed r1
    	{KhetBoard.CELL_DOWN(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_UP(),KhetBoard.CELL_LEFT()},		//  eye r0
    	{KhetBoard.CELL_UP(),KhetBoard.CELL_LEFT(),KhetBoard.CELL_DOWN(),KhetBoard.CELL_RIGHT()},		//  eye r1
    	{KhetBoard.CELL_DOWN(),KhetBoard.CELL_RIGHT(),Smoke,Smoke},		//  pyramid r0
    	{Smoke,KhetBoard.CELL_LEFT(),KhetBoard.CELL_DOWN(),Smoke},		//  pyramid r1
    	{Smoke,Smoke,KhetBoard.CELL_UP(),KhetBoard.CELL_LEFT()},		// djed r2
    	{KhetBoard.CELL_UP(),Smoke,Smoke,KhetBoard.CELL_RIGHT()},		// djed r3
      	{Smoke,Absorb,Smoke,Smoke},		// annubus r0
    	{Smoke,Smoke,Absorb,Smoke},		// annubus r1
       	{Smoke,Smoke,Smoke,Absorb},		// annubus r2
       	{Absorb,Smoke,Smoke,Smoke},		// annubus r3
       	
       	{KhetBoard.CELL_LEFT(),KhetBoard.CELL_UP(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_DOWN()},	// sphinx passes through
    	{KhetBoard.CELL_LEFT(),KhetBoard.CELL_UP(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_DOWN()},	// sphinx passes through 
    	{KhetBoard.CELL_LEFT(),KhetBoard.CELL_UP(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_DOWN()},	// sphinx passes through 
    	{KhetBoard.CELL_LEFT(),KhetBoard.CELL_UP(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_DOWN()},	// sphinx passes through 
 	
    	{Smoke,Smoke,Smoke,Smoke},		//  silver pharoh
    	{KhetBoard.CELL_DOWN(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_UP(),KhetBoard.CELL_LEFT()},		//  djed r0
    	{KhetBoard.CELL_UP(),KhetBoard.CELL_LEFT(),KhetBoard.CELL_DOWN(),KhetBoard.CELL_RIGHT()},		//  djed r1
    	{KhetBoard.CELL_DOWN(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_UP(),KhetBoard.CELL_LEFT()},		//  eye r0
    	{KhetBoard.CELL_UP(),KhetBoard.CELL_LEFT(),KhetBoard.CELL_DOWN(),KhetBoard.CELL_RIGHT()},		//  eye r1
    	{KhetBoard.CELL_DOWN(),KhetBoard.CELL_RIGHT(),Smoke,Smoke},		//  pyramid r0
    	{Smoke,KhetBoard.CELL_LEFT(),KhetBoard.CELL_DOWN(),Smoke},		//  pyramid r1
    	{Smoke,Smoke,KhetBoard.CELL_UP(),KhetBoard.CELL_LEFT()},		// djed r2
    	{KhetBoard.CELL_UP(),Smoke,Smoke,KhetBoard.CELL_RIGHT()},		// djed r3
    	
       	{Smoke,Absorb,Smoke,Smoke},		// annubus r0
    	{Smoke,Smoke,Absorb,Smoke},		// annubus r1
       	{Smoke,Smoke,Smoke,Absorb},		// annubus r2
       	{Absorb,Smoke,Smoke,Smoke},		// annubus r3
  
    	{KhetBoard.CELL_LEFT(),KhetBoard.CELL_UP(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_DOWN()},	// sphinx passes through 
    	{KhetBoard.CELL_LEFT(),KhetBoard.CELL_UP(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_DOWN()},	// sphinx passes through 
    	{KhetBoard.CELL_LEFT(),KhetBoard.CELL_UP(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_DOWN()},	// sphinx passes through 
    	{KhetBoard.CELL_LEFT(),KhetBoard.CELL_UP(),KhetBoard.CELL_RIGHT(),KhetBoard.CELL_DOWN()},	// sphinx passes through 
 
    	{Absorb,Absorb,Absorb,Absorb}	// laser 
    	
   	};
     
    static private int ROTATIONS[] =
    {	
    	
    	KhetBoard.CELL_DOWN(),		//  gold pharoh
    	KhetBoard.CELL_DOWN(),		//  djed r0
    	KhetBoard.CELL_UP(),			//  djed r1
    	KhetBoard.CELL_DOWN(),		//  eye r0
    	KhetBoard.CELL_UP(),			//  eye r1
    	KhetBoard.CELL_DOWN(),		//  pyramid r0
    	KhetBoard.CELL_LEFT(),		//  pyramid r1
    	KhetBoard.CELL_UP(),			//  pyramid r2
    	KhetBoard.CELL_RIGHT(),		//  pyramid r3
    	KhetBoard.CELL_DOWN(),		//  annubus r0
    	KhetBoard.CELL_LEFT(),		//  annubus r1
    	KhetBoard.CELL_UP(),			//  annubus r2
    	KhetBoard.CELL_RIGHT(),		//  annubus r3
    	KhetBoard.CELL_DOWN(),		//  sphinx r0
    	KhetBoard.CELL_LEFT(),		//  sphinx r1
    	KhetBoard.CELL_UP(),			//  sphinx r2
    	KhetBoard.CELL_RIGHT(),		//  sphinx r3    	
	
    	
    	KhetBoard.CELL_DOWN(),		//  gold pharoh
    	KhetBoard.CELL_DOWN(),		//  djed r0
    	KhetBoard.CELL_UP(),			//  djed r1
    	KhetBoard.CELL_DOWN(),		//  eye r0
    	KhetBoard.CELL_UP(),			//  eye r1
    	KhetBoard.CELL_DOWN(),		//  pyramid r0
    	KhetBoard.CELL_LEFT(),		//  pyramid r1
    	KhetBoard.CELL_UP(),			//  pyramid r2
    	KhetBoard.CELL_RIGHT(),		//  pyramid r3
    	KhetBoard.CELL_DOWN(),		//  annubus r0
    	KhetBoard.CELL_LEFT(),		//  annubus r1
    	KhetBoard.CELL_UP(),			//  annubus r2
    	KhetBoard.CELL_RIGHT(),		//  annubus r3
    	KhetBoard.CELL_DOWN(),		//  sphinx r0
    	KhetBoard.CELL_LEFT(),		//  sphinx r1
    	KhetBoard.CELL_UP(),			//  sphinx r2
    	KhetBoard.CELL_RIGHT(),		//  sphinx r3    	

 
    	-1	// laser 
    	
   	};
	public static KhetChip getChip(int color)
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color]);
	}
	
	public static KhetChip getChip(int pl,int index)
	{
		return(CANONICAL_PIECE[FIRST_CHIP_INDEX+((pl^1)*N_STANDARD_CHIPS)+index]);
	}
  /* pre load images and create the canonical pieces
   * 
   */
 
   static private final String[] ImageNames = 
       {
	   "red-pharoh",
	   "red-scarab-r0","red-scarab-r1",
	   "red-eye-r0","red-eye-r1",
	   "red-pyramid-r0","red-pyramid-r1","red-pyramid-r2","red-pyramid-r3"   ,
	   "red-annubus-r0","red-annubus-r1","red-annubus-r2","red-annubus-r3",
	   "red-sphinx-r0","red-sphinx-r1","red-sphinx-r2","red-sphinx-r3",
	   
	   "silver-pharoh",
	   "silver-scarab-r0","silver-scarab-r1",
	   "silver-eye-r0","silver-eye-r1",
	   "silver-pyramid-r0","silver-pyramid-r1","silver-pyramid-r2","silver-pyramid-r3",
	   "silver-annubus-r0","silver-annubus-r1","silver-annubus-r2","silver-annubus-r3",
	   "silver-sphinx-r0","silver-sphinx-r1","silver-sphinx-r2","silver-sphinx-r3",
	   
	   "blast"
       };
   static private final KhetPieceType TYPES[] = 
   	{  KhetPieceType.Pharoh,	
	   KhetPieceType.Djed,KhetPieceType.Djed,
	   KhetPieceType.Eye,KhetPieceType.Eye,
	   KhetPieceType.Pyramid,KhetPieceType.Pyramid,KhetPieceType.Pyramid,KhetPieceType.Pyramid,
	   KhetPieceType.Annubus,KhetPieceType.Annubus,KhetPieceType.Annubus,KhetPieceType.Annubus,
	   KhetPieceType.Sphinx,KhetPieceType.Sphinx,KhetPieceType.Sphinx,KhetPieceType.Sphinx,
	   
	   KhetPieceType.Pharoh,	
	   KhetPieceType.Djed,KhetPieceType.Djed,
	   KhetPieceType.Eye,KhetPieceType.Eye,
	   KhetPieceType.Pyramid,KhetPieceType.Pyramid,KhetPieceType.Pyramid,KhetPieceType.Pyramid,
	   KhetPieceType.Annubus,KhetPieceType.Annubus,KhetPieceType.Annubus,KhetPieceType.Annubus,
	   KhetPieceType.Sphinx,KhetPieceType.Sphinx,KhetPieceType.Sphinx,KhetPieceType.Sphinx,
	   
	   KhetPieceType.Blast	// blast
   	};                                         
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(Blast==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        KhetChip CC[] = new KhetChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new KhetChip(ImageNames[i],i-FIRST_CHIP_INDEX,IM[i],rv.nextLong(),SCALES[i],TYPES[i],BOUNCES[i],ROTATIONS[i]); 
        	}
        CANONICAL_PIECE = CC;
        // set up the rotations
        for(int pl=0;pl<=1;pl++)
        {	{KhetChip d0 = getChip(pl,DJED_R0_INDEX);
        	KhetChip d1 = getChip(pl,DJED_R1_INDEX);
        	d0.next_rot = d0.prev_rot = d1;
        	d1.next_rot = d1.prev_rot = d0;
        	}
	        {
	        KhetChip d0 = getChip(pl,EYE_R0_INDEX);
	    	KhetChip d1 = getChip(pl,EYE_R1_INDEX);
	    	d0.next_rot = d0.prev_rot = d1;
	    	d1.next_rot = d1.prev_rot = d0;
	        } 	
        	for(int i=0;i<4;i++) 
        	{	KhetChip p0 = getChip(pl,PYRAMID_R0_INDEX+i);
        		p0.next_rot = getChip(pl,PYRAMID_R0_INDEX+(i+1)%4);
        		p0.prev_rot = getChip(pl,PYRAMID_R0_INDEX+(i+3)%4);
        		
        		KhetChip p1 = getChip(pl,ANNUBUS_R0_INDEX+i);
        		p1.next_rot = getChip(pl,ANNUBUS_R0_INDEX+(i+1)%4);
        		p1.prev_rot = getChip(pl,ANNUBUS_R0_INDEX+(i+3)%4);

        		KhetChip p2 = getChip(pl,SPHINX_R0_INDEX+i);
        		p2.next_rot = getChip(pl,SPHINX_R0_INDEX+(i+1)%4);
        		p2.prev_rot = getChip(pl,SPHINX_R0_INDEX+(i+3)%4);
       	}
        }
        Blast = CANONICAL_PIECE[BLAST_INDEX];
        check_digests(CC);
		}
	}


}
