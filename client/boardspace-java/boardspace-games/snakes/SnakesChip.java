/* copyright notice */package snakes;

import java.awt.Component;
import lib.Image;
import lib.ImageLoader;
import lib.Random;

import lib.G;

import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */


public class SnakesChip extends chip<SnakesChip> implements Cloneable
{	
	private int colorIndex;
	private String name = "";
	public int chipNumber() { return(colorIndex); }

	static final int FIRST_TILE_INDEX = 0;
    static final int N_STANDARD_TILES = 2;
    static final int N_STANDARD_CHIPS = 16;
    static final int FIRST_CHIP_INDEX = N_STANDARD_TILES;

    
    Coverage cover[] = null;
    private SnakesChip rotated[] = new SnakesChip[3];
    static Image[][] rotatedImages = new Image[3][];		// rotated images
    
    // get a rotated tile.  Rotated tiles are used only for drawing, the tile on the board is always
    // the unrotated one.
    public SnakesChip getRotated(Component forcom,int steps)
    {	if(steps==0) { return(this); }
    	SnakesChip rot = rotated[steps-1];
    	try {
    	if(rot==null)
    	{	rot = rotated[steps-1] = (SnakesChip)clone();
    		rot.image = rotatedImages[steps-1][chipNumber()];
    		double sx = scale[0];
    		double sy = scale[1];
    		double sc = scale[2];
    		int w = image.getWidth();
    		int h = image.getHeight();
    		rot.scale = new double[3];
    		
    		switch(steps)
    		{
    		// complement the x,y offsets so the tile appears in the expected place.
    		case 1:  
    			rot.scale[0] = 1.0-sy; 
    			rot.scale[1] = sx;
    			rot.scale[2] = sc*h/w;
    			break;
    		case 2:  
    			rot.scale[0] = 1.0-sx;
    			rot.scale[1] = 1.0-sy;
    			rot.scale[2] = sc;
    			break;
    		case 3: 
    			rot.scale[0] = sy;
    			rot.scale[1] = 1-sx; 			
    			rot.scale[2] = sc*h/w;
    			break;
			default:
				break;
    		}
    	}} catch (CloneNotSupportedException err) {};
    	return(rot);
    }

	private SnakesChip(String na,int pla,Image im,long rv,double scl[],Coverage[]m)
	{	name = na;
		colorIndex=pla;
		image = im;
		randomv = rv;
		scale = scl;
		cover = m;
	}
	public String toString()
	{	return("<"+ name+" #"+colorIndex+">");
	}
	public String contentsString() 
	{ return(name); 
	}

		
	// note, do not make these private, as some optimization failure
	// tries to access them from outside.
    static private SnakesChip CANONICAL_PIECE[] = null;	// created by preload_images
    static private double SCALES[][] =
    {	{0.5,0.45,0.95},		// light square
    	{0.5,0.45,0.95},		// dark square
    	
    	// these are ad-hoc adjustments for the size and position of the tile so they fall
    	// on the squares they are intended to be seen on.  Adjusting these numbers compensates
    	// for the arbitrary image size and position of the desired part of the image within
    	// the larger image including border space.
    	{0.5,0.686,0.915},			// s-2-1
    	{0.523,0.675,1.074},		// s-2-2
    	{0.537,0.680,1.129},		// s-2-3
    	{0.523,0.682,1.083},		// s-2-4
    	{0.537,0.695,1.083},		// s-2-5
    	{0.537,0.669,1.083},		// s-2-6
    	{0.537,0.653,1.083},		// s-2-7
    	{0.537,0.695,1.133},		// s-2-8
    	
    	{0.302,0.710,1.939},		// s-3-1
    	{0.302,0.710,1.939},		// s-3-2
    	{0.302,0.710,1.939},		// s-3-3
    	{0.302,0.710,1.939},		// s-3-4
    	{0.302,0.710,1.939},		// s-3-5
    	{0.302,0.710,1.939},		// s-3-6
    	{0.302,0.710,1.939},		// s-3-7
    	{0.302,0.710,1.939},		// s-3-8

    };
     

	public static SnakesChip getTile(int color)		// get the base tiless for the board
	{	return(CANONICAL_PIECE[FIRST_TILE_INDEX+color]);
	}
	public static SnakesChip getChip(int color)		// get the snake tiles by number
	{	return(CANONICAL_PIECE[FIRST_CHIP_INDEX+color]);
	}
  /* pre load images and create the canonical pieces
   * 
   */
 
   static final String[] ImageNames = 
       {"light-tile","dark-tile",
	   // artwork for the red snake tiles, these correspond to the the map of their parts
	   // found in Coverage.java, so both have to be changed in randem.
	    "red-2-1","red-2-2","red-2-3","red-2-4","red-2-5","red-2-6","red-2-7","red-2-8",
	    "red-3-1","red-3-2","red-3-3","red-3-4","red-3-5","red-3-6","red-3-7","red-3-8"
       };
 
    
   static int chipNumber(SnakesChip ch) { return((ch==null)?-1:ch.chipNumber()); } 
    
   // special rules implemented by random logic.  Generally, identify a pattern and only allow
   // it to be placed in specific rotations.
   	static boolean  ALL = true;
   	static boolean TEST = false;			// if true, allow everything but record reasons
    static boolean SINGLE_SYMMETRIC = true;	// only 2 rotations
    static boolean SQUARE_2X2 = ALL && true;		// only one rotation
    static boolean SINGLE_U =  ALL && true;			// only 2 rotations
    static boolean STICKS = ALL && true;			// 2 3 unit snakes
    static boolean SSNAKE = ALL && false;			// U + 13 becomes U + 14.  Not quite always
    static boolean SWAPSNAKE = ALL && true; 		// swap head/tail 
    public boolean applySpecialRules(SnakesCell c,int rot)
    {	// c is the home position of a proposed placement which is known to be valid
    	// so all the cells we test as exits are known to exist, but the expected chip
    	// may or may not have been placed.
    	switch(chipNumber())
    	{
    	default: throw G.Error("Not expecting chip "+this);
    	case 0:
    		// head and tail in a single cell
    		{
    		SnakesCell ex1 = c.exitTo(rot+SnakesBoard.CELL_RIGHT());
    		SnakesChip ex1Top = ex1.topChip();
    		int exTopn = chipNumber(ex1Top);

    		if (SQUARE_2X2 && (exTopn==4))	// joins to the hoop
    		{
	    		SnakesCell ex2 = ex1.exitTo(rot+SnakesBoard.CELL_UP()); 
	    		if(ex2.topChip()==ex1Top) 
	    			{ 	boolean include = rot==0;
	    				if(!include) { c.declined = "SQUARE_2x2 0"; }
	    				return(include || TEST); 
	    			}
    		}
    		
    		if(SINGLE_U && (exTopn==5)) 		// left hook + right hook form a U 3x2
    			{
    			SnakesCell ex3 = ex1.exitTo(rot+SnakesBoard.CELL_UP());
    			if(chipNumber(ex3.topChip())==7)	// right hook 
    				{ boolean include = rot<2;
    				  if(!include) { c.declined = "SINGLE_U 0"; }
    				  return(include || TEST);  
    				}
    			}
    		if(STICKS && (exTopn==6))
    			{	SnakesCell up =  c.exitTo(rot+SnakesBoard.CELL_UP());
    				SnakesCell upRight = up.exitTo(rot+SnakesBoard.CELL_RIGHT());
    				if(chipNumber(upRight.topChip())==2) 
    					{ boolean include = rot<2;
    					  if(!include) { c.declined = "sticks 0"; }
    					  return(include || TEST);
    					}
       			}
    		}
    		break;
    	case 2:
    		if(STICKS)
    		{	SnakesCell down = c.exitTo(rot+SnakesBoard.CELL_DOWN());
    			int downTopn = chipNumber(down.topChip());
    			if(downTopn==0)
				{	SnakesCell downRight = c.exitTo(rot+SnakesBoard.CELL_RIGHT());
					if(chipNumber(downRight.topChip())==6)
					{	SnakesCell down2Right = down.exitTo(rot+SnakesBoard.CELL_RIGHT());
						if(chipNumber(down2Right.topChip())==downTopn)
						{	boolean include = (rot==0)||(rot==1);
							if(!include) { c.declined = "STICKS 2"; }
							return(include || TEST);
						}
					}
				}
    			if(downTopn==4)
    				{	SnakesCell downRight = c.exitTo(rot+SnakesBoard.CELL_LEFT());
    					if(chipNumber(downRight.topChip())==6)
    					{	SnakesCell down2Right = down.exitTo(rot+SnakesBoard.CELL_LEFT());
    						if(chipNumber(down2Right.topChip())==downTopn)
    						{
    							return(false);
    						}
    					}
    				}
    		}
    		if(SWAPSNAKE)
    		{	SnakesCell next = c.exitTo(rot+SnakesBoard.CELL_DOWN());
    			SnakesCell end = next.otherEnd(c);
    			if(end!=null)
    				{ SnakesChip endTop = end.topChip(); 
    				  SnakesCell up = c.exitTo(rot+SnakesBoard.CELL_UP());
    				  if(chipNumber(endTop)==6) 
    				  {	boolean include = (end.col<up.col) || ((end.col==up.col)?(end.row>up.row) : false);
    				    if(!include) { c.declined = "SWAPSNAKE 2"; }
    				    return(include || TEST);
    				  }
    				}
    		}
    		break;
    	case 1:	// this is the S shaped body segment
    	case 3:	// this single snake.
    		if(SINGLE_SYMMETRIC) { return(rot<2); }	// only in first 2 rotations
    		break;
    	case 4:
	    	// u shape, inverse of logic for snake 0
			{
			SnakesCell ex1 = c.exitTo(rot+SnakesBoard.CELL_LEFT());
			SnakesChip ex1Top = ex1.topChip();
			int ex1Topn = chipNumber(ex1Top);
			if(STICKS && (ex1Topn==2))
			{
				SnakesCell up = c.exitTo(rot+SnakesBoard.CELL_UP());
				SnakesCell ex2 = up.exitTo(rot+SnakesBoard.CELL_LEFT());
				if(chipNumber(ex2.topChip())==6) 
					{ c.declined = "STICKS 4";
					  return(false || TEST); 
					}
			}
			if(SQUARE_2X2 && (ex1Topn==0))	// joins to the hoop
				{
				SnakesCell ex2 = ex1.exitTo(rot+SnakesBoard.CELL_UP()); 
				if(ex2.topChip()==ex1Top) 
					{ boolean include = (rot==0);
					  if(!include) { c.declined = "SQUARE 2x2 4"; }
					  return(include || TEST);
					}
				}
			if(SSNAKE)
				{
				// in this special case, we don't care which end of the opposite piece
				// we attach to, one is the one we intend to test for, and the other forms
				// a useless shape.
				if(ex1Topn==14)
				{	boolean include = (rot==0) || (rot==3);
					if(!include) { c.declined = "ssnake 4a"; }
					return(include || TEST);
					
				}
				SnakesCell up = c.exitTo(rot+SnakesBoard.CELL_UP());
				SnakesCell upleft = up.exitTo(rot+SnakesBoard.CELL_LEFT());
				if(chipNumber(upleft.topChip())==13) 
					{ boolean include = (rot==0)||(rot==3);
					  if(!include) { c.declined = "ssnake 4b"; }
					  return(include || TEST); 
					}
			}}
			break;
    	case 5:
     		// left hook
    		if(SINGLE_U)	// look for joins the head/tail to form a U shaped shake
    		{
    		SnakesCell up1 = c.exitTo(rot+SnakesBoard.CELL_UP());
     		SnakesCell up2 = up1.exitTo(rot+SnakesBoard.CELL_UP());
     		SnakesChip up2Top = up2.topChip();
    		if(chipNumber(up2Top)==0)
    			{
    			SnakesCell up2Right = up2.exitTo(rot+SnakesBoard.CELL_RIGHT());
    			SnakesChip up2RightTop = up2Right.topChip();
    			if(up2RightTop==up2Top)	// other part of the chip
					{
					SnakesCell upRight = up1.exitTo(rot+SnakesBoard.CELL_RIGHT());
					SnakesChip upRightTop = upRight.topChip();
					if((upRightTop!=null) && (upRightTop.chipNumber()==7))
						{boolean include = (rot==0) || (rot==3);
						if(!include) { c.declined = "SINGLE_U 5"; }
						return(include || TEST);
					}
    			}
    		}}
    		
    		break;
    	case 6:
    		if(STICKS)
    		{	SnakesCell up = c.exitTo(rot+SnakesBoard.CELL_UP());
    			SnakesCell up2 = up.exitTo(rot+SnakesBoard.CELL_UP());
    			int up2Topn = chipNumber(up2.topChip());
    			if(up2Topn==0)
				{
				SnakesCell upRight = up.exitTo(rot+SnakesBoard.CELL_RIGHT() );
				if(chipNumber(upRight.topChip())==2)
				{	SnakesCell up2Right = up2.exitTo(rot+SnakesBoard.CELL_RIGHT());
					if(chipNumber(up2Right.topChip())==up2Topn)
					{	boolean include = (rot==2)||(rot==3);
					 	if(!include) { c.declined = "sticks 6a"; }
						return(include || TEST);
					}
				}
				}
    			else if(up2Topn==4)
    			{
					SnakesCell upRight = up.exitTo(rot+SnakesBoard.CELL_RIGHT());
					if(chipNumber(upRight.topChip())==2)
					{	SnakesCell up2Right = up2.exitTo(rot+SnakesBoard.CELL_RIGHT());
						if(chipNumber(up2Right.topChip())==up2Topn)
						{	c.declined = "sticks 6b";
							return(false || TEST);
						}
					}
    			}
    		}
    		if(SWAPSNAKE)
    		{	SnakesCell next0 = c.exitTo(rot+SnakesBoard.CELL_UP());
    			SnakesCell next = next0.exitTo(rot+SnakesBoard.CELL_UP());
    			SnakesCell end = next.otherEnd(next0);
    			if(end!=null)
    				{ SnakesChip endTop = end.topChip(); 
    				  if(chipNumber(endTop)==2) 
    				  { boolean include = (c.col<end.col) || ((c.col==end.col)?(c.row>end.row):false);
    				  	if(!include) { c.declined = "swapsnake 6"; }
    				    return(include || TEST);
    				  }
    				}
    		}
    		break;
    	case 7:
    		if(SINGLE_U)	// right hook, look for joins left hook and head/tail to form a 3x2
  			{
    		SnakesCell up1 = c.exitTo(rot+SnakesBoard.CELL_UP());
    		SnakesCell up2 = up1.exitTo(rot+SnakesBoard.CELL_UP());
    		SnakesChip up2Top = up2.topChip();
    		if(chipNumber(up2Top)==0)
    			{
    				SnakesCell up2Left = up2.exitTo(rot+SnakesBoard.CELL_LEFT());
     					SnakesChip up2LeftTop = up2Left.topChip();
     						if(up2LeftTop==up2Top)	// other part of the chip
    						{
    						SnakesCell upLeft = up1.exitTo(rot+SnakesBoard.CELL_LEFT());
    						SnakesChip upLeftTop = upLeft.topChip();
    						if((upLeftTop!=null) && (upLeftTop.chipNumber()==5))
    						{	boolean include = (rot==0) || (rot==3);
    							if(!include) { c.declined = "single u 7"; }
    							return(include || TEST);
    						}
    			}
    		}}
    		break;
    	// three cells
    	case 8:
    	case 9:
    	case 10:
    	case 11:
    	case 12:
    	case 13:
    	case 14:
    		if(SSNAKE)
    		{ SnakesCell right = c.exitTo(rot+SnakesBoard.CELL_RIGHT());
    		  if(chipNumber(right.topChip())==4)
    		  {	boolean include = (rot==0) || (rot==3);
    		  	if(!include) { c.declined = "ssnake 14"; } 
    		  	return(include || TEST);
    		  }
    		}
    		break;
    		
    	case 15:
   			if(SSNAKE)
   			{ SnakesCell right = c.exitTo(rot+SnakesBoard.CELL_UP());
   				if(chipNumber(right.topChip())==4)
   				{	boolean include = (rot==0) || (rot==1);
   					if(!include) { c.declined = "ssnake 15"; }
   					return(include || TEST);
   				}
   			}
    		break;
    	}
    	return(true);
    }
    
    // apply a subset of the special filtering rules that are applicable even
    // when there are givens.
    public boolean applyGivensRules(SnakesCell c,int rot)
    {	// c is the home position of a proposed placement which is known to be valid
    	// so all the cells we test as exits are known to exist, but the expected chip
    	// may or may not have been placed.
    	switch(chipNumber())
    	{
    	default: break;
    	case 0:
    		// head and tail in a single cell
    		{
    		SnakesCell ex1 = c.exitTo(rot+SnakesBoard.CELL_RIGHT());
    		SnakesChip ex1Top = ex1.topChip();
    		int exTopn = chipNumber(ex1Top);

    		if (SQUARE_2X2 && (exTopn==4))	// joins to the hoop
    		{
	    		SnakesCell ex2 = ex1.exitTo(rot+SnakesBoard.CELL_UP()); 
	    		if(ex2.topChip()==ex1Top) 
	    			{ 	boolean include = (rot==0) || (rot==1);		// allow 2 different rotations
	    				if(!include) { c.declined = "SQUARE_2x2 0"; }
	    				return(include || TEST); 
	    			}
    		}
    		
    		}
    		break;

    	case 1:	// this is the S shaped body segment
    	case 3:	// this single snake.
    		if(SINGLE_SYMMETRIC) { return(rot<2); }	// only in first 2 rotations
    		break;
    	}
    	return(true);
    }
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        SnakesChip CC[] = new SnakesChip[nColors];
        Random rv = new Random(343535);		// an arbitrary number, just change it
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new SnakesChip(ImageNames[i],i-FIRST_CHIP_INDEX,IM[i],rv.nextLong(),SCALES[i],Coverage.coverMap[i]); 
        	}
        CANONICAL_PIECE = CC;
        
        // load the rotated images.  Each of the rotated images and masks have the same name with -n appended
        // there are 3 sets of rotated images.  When needed they'll be made in chips.
        for(int rot=1; rot<4; rot++) 
        {	int len = ImageNames.length-FIRST_CHIP_INDEX;
         	String names[] = new String[len];
        	for(int i=0;i<len;i++) { names[i] = ImageNames[i+FIRST_CHIP_INDEX] + "-" + rot; }
        	rotatedImages[rot-1] = forcan.load_masked_images(ImageDir,names);
        }
        check_digests(CC);
		}
	}


}
