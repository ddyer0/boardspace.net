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
package twixt;

import lib.Image;
import lib.DrawableImageStack;
import lib.G;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

/**
 * this is a specialization of {@link chip} to represent the stones used by twixt;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class TwixtChip extends chip<TwixtChip> implements TwixtConstants
{
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	
	private int index = 0;
	public int chipNumber() { return(index); }		// used only for sprite ids
	public TwixtId id;								// enum of element types
	private PieceColor color = null;				// red or black
	private PieceType type = null; 					// bridge or peg

	public PieceColor color() { return(color); }
	public boolean isBridge() { return(type==PieceType.Bridge); }
	public boolean isPeg() { return(type==PieceType.Peg); }
	
	public boolean ghost = false;					// ghosts are "not really there" copies of pieces
	public boolean isGhost() { return(ghost); }
	public TwixtChip ghosted = null;				// ghosted alternative for self
	public TwixtChip getGhosted() { G.Assert(ghosted!=null,"ghost not present"); return(ghosted); }
	
	private TwixtChip altChips[] = null;			// alternate visuals for rotated or flattened boards,
	private TwixtChip altImage = null;				// alternate source for the image for this chip
	
	// constructor for the chips on the board, which are the only things that are digestable.
	private TwixtChip(String na,double[]sc,PieceColor pl,PieceType pt,TwixtId con)
	{	index = allChips.size();
		scale=sc;
		file = na;
		id = con;
		type = pt;
		color = pl;
		randomv = r.nextLong();
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private TwixtChip(String na,double[]sc)
	{	index = allChips.size();
		scale=sc;
		file = na;
		allChips.push(this);
	}
	
	// constructor for ghost copies of chips.
	private TwixtChip(TwixtChip na)
	{	index = na.index;
		altImage = na;
		scale=na.scale;
		type = na.type;
		id = na.id;
		color = na.color;
		randomv = r.nextLong();
		ghost = true;
		ghosted = this;
		file = na.file+"-ghost";
	}
	// constructor for all the other random artwork.
	private TwixtChip(TwixtChip na,double[]sc)
	{	index = allChips.size();
		altImage = na;
		scale=sc;
		file = na.file+"-rev";

	}
	
	// get the image, possibly after constructing it.
	public Image getImage(ImageLoader s) 
	{ 	if(image==null && (altImage!=null)) 
			{ Image im = altImage.getImage(s); 
			  if(ghost) { im = im.makeTransparent(0.25); }
			  image = im;
			}
		return(image); 
	}
	
	/**
	 * the primary images are pegs and 4 directions of bridges.
	 * alternates are flattened pegs, pegs with no end hooks, and bridges projecting downward instead of upward
	 */

    static public TwixtChip red_peg = new TwixtChip("red-peg",new double[]{0.583,0.623,1.686},PieceColor.Red,PieceType.Peg,TwixtId.Red_Peg);
    static public TwixtChip red_peg_flat = new TwixtChip("red-peg-flat",new double[] {0.575,0.397,1.3});

    // upward slant bridges for the normal view
    static public TwixtChip red_bridge_30_truncated = new TwixtChip("red-bridge-30-truncated",new double[]{0.848,0.625,3.27});
    static public TwixtChip red_bridge_60_truncated = new TwixtChip("red-bridge-60-truncated",new double[]{0.659,0.802,3.366});
    static public TwixtChip red_bridge_120_truncated = new TwixtChip("red-bridge-120-truncated",new double[]{0.333,0.856,2.947});
    static public TwixtChip red_bridge_150_truncated = new TwixtChip("red-bridge-150-truncated",new double[]{0.252,0.660,3.57});
    // downward slant bridges for the 
    static public TwixtChip red_bridge_30_reversed = new TwixtChip(red_bridge_30_truncated,new double[]{0.156,0.303,3.112});
    static public TwixtChip red_bridge_60_reversed = new TwixtChip(red_bridge_60_truncated,new double[]{0.349,0.178,3.393});
    static public TwixtChip red_bridge_120_reversed = new TwixtChip(red_bridge_120_truncated,new double[]{0.688,0.156,2.988});
    static public TwixtChip red_bridge_150_reversed = new TwixtChip(red_bridge_150_truncated,new double[]{0.847,0.350,3.552});

    static public TwixtChip red_bridge_30 = new TwixtChip("red-bridge-30",new double[]{0.5,0.5,2.977},PieceColor.Red,PieceType.Bridge,TwixtId.Red_Bridge_30);
    static public TwixtChip red_bridge_60 = new TwixtChip("red-bridge-60",new double[]{0.5,0.5,2.506},PieceColor.Red,PieceType.Bridge,TwixtId.Red_Bridge_60);
    static public TwixtChip red_bridge_120 = new TwixtChip("red-bridge-120",new double[]{0.5,0.5,2.574},PieceColor.Red,PieceType.Bridge,TwixtId.Red_Bridge_120);
    static public TwixtChip red_bridge_150 = new TwixtChip("red-bridge-150",new double[]{0.5,0.5,2.851},PieceColor.Red,PieceType.Bridge,TwixtId.Red_Bridge_150);

    // indexes into the balls array, usually called the rack
    static public TwixtChip black_peg = new TwixtChip("black-peg",new double[]{0.558,0.534,1.728},PieceColor.Black,PieceType.Peg,TwixtId.Black_Peg);
    static public TwixtChip black_peg_flat = new TwixtChip("black-peg-flat",new double[] {0.623,0.363,1.335});
    // upward facing bars for the normal view
    static public TwixtChip black_bridge_30_truncated = new TwixtChip("black-bridge-30-truncated",new double[]{0.831,0.659,3.142});
    static public TwixtChip black_bridge_60_truncated = new TwixtChip("black-bridge-60-truncated",new double[]{0.699,0.839,2.807});
    static public TwixtChip black_bridge_120_truncated = new TwixtChip("black-bridge-120-truncated",new double[]{0.301,0.872,2.968});
    static public TwixtChip black_bridge_150_truncated = new TwixtChip("black-bridge-150-truncated",new double[]{0.194,0.675,3.242}/*new double[]{0.183,0.647,2.981}*/);

    // downward angled bridges for the reverse view
    static public TwixtChip black_bridge_30_reversed = new TwixtChip(black_bridge_30_truncated,new double[]{0.164,0.322,3.067});
    static public TwixtChip black_bridge_60_reversed = new TwixtChip(black_bridge_60_truncated,new double[]{0.344,0.119,3.058});
    static public TwixtChip black_bridge_120_reversed = new TwixtChip(black_bridge_120_truncated,new double[]{0.658,0.152,2.932});
    static public TwixtChip black_bridge_150_reversed = new TwixtChip(black_bridge_150_truncated,new double[]{0.868,0.341,3.166});

    static public TwixtChip black_bridge_30 = new TwixtChip("black-bridge-30",new double[]{0.5,0.5,3.097},PieceColor.Black,PieceType.Bridge,TwixtId.Black_Bridge_30);
    static public TwixtChip black_bridge_60 = new TwixtChip("black-bridge-60",new double[]{0.5,0.5,2.435},PieceColor.Black,PieceType.Bridge,TwixtId.Black_Bridge_60);
    static public TwixtChip black_bridge_120 = new TwixtChip("black-bridge-120",new double[]{0.5,0.5,2.57},PieceColor.Black,PieceType.Bridge,TwixtId.Black_Bridge_120);
    static public TwixtChip black_bridge_150 = new TwixtChip("black-bridge-150",new double[]{0.5,0.5,3.01},PieceColor.Black,PieceType.Bridge,TwixtId.Black_Bridge_150);

    public static TwixtChip post[] = { red_peg,black_peg};
    public static TwixtChip blackChips[] = {black_peg, black_bridge_30, black_bridge_60,black_bridge_120,black_bridge_150};
    public static TwixtChip redChips[] = {red_peg, red_bridge_30, red_bridge_60,red_bridge_120,red_bridge_150};
    public static TwixtChip blackBridges[] = { black_bridge_30, black_bridge_60,black_bridge_120,black_bridge_150};
    public static TwixtChip redBridges[] = { red_bridge_30, red_bridge_60,red_bridge_120,red_bridge_150};

    static {
    	// limits of static initialization - these ought to be in the class definition for PieceColor
    	// but there's a recursive dependency
    	PieceColor.Red.peg = red_peg;
    	PieceColor.Red.bridges = redBridges;
    	PieceColor.Black.peg = black_peg;
    	PieceColor.Black.bridges = blackBridges;
    }
 

    // ghost copies of the main pieces
    static public TwixtChip red_peg_flat_ghost = new TwixtChip(red_peg_flat);
    static public TwixtChip red_peg_ghost = new TwixtChip(red_peg);
    // upward slant bridges for the normal view
    static public TwixtChip red_bridge_30_truncated_ghost = new TwixtChip(red_bridge_30_truncated);
    static public TwixtChip red_bridge_60_truncated_ghost = new TwixtChip(red_bridge_60_truncated);
    static public TwixtChip red_bridge_120_truncated_ghost = new TwixtChip(red_bridge_120_truncated);
    static public TwixtChip red_bridge_150_truncated_ghost = new TwixtChip(red_bridge_150_truncated);
    // downward slant bridges for the 
    static public TwixtChip red_bridge_30_reversed_ghost = new TwixtChip(red_bridge_30_reversed);
    static public TwixtChip red_bridge_60_reversed_ghost = new TwixtChip(red_bridge_60_reversed);
    static public TwixtChip red_bridge_120_reversed_ghost = new TwixtChip(red_bridge_120_reversed);
    static public TwixtChip red_bridge_150_reversed_ghost = new TwixtChip(red_bridge_150_reversed);
 
    static public TwixtChip red_bridge_30_ghost = new TwixtChip(red_bridge_30);
    static public TwixtChip red_bridge_60_ghost = new TwixtChip(red_bridge_60);
    static public TwixtChip red_bridge_120_ghost = new TwixtChip(red_bridge_120);
    static public TwixtChip red_bridge_150_ghost = new TwixtChip(red_bridge_150);

    static public TwixtChip black_peg_flat_ghost = new TwixtChip(black_peg_flat);
    static public TwixtChip black_peg_ghost = new TwixtChip(black_peg);
    // upward facing bars for the normal view
    static public TwixtChip black_bridge_30_truncated_ghost = new TwixtChip(black_bridge_30_truncated);
    static public TwixtChip black_bridge_60_truncated_ghost = new TwixtChip(black_bridge_60_truncated);
    static public TwixtChip black_bridge_120_truncated_ghost = new TwixtChip(black_bridge_120_truncated);
    static public TwixtChip black_bridge_150_truncated_ghost = new TwixtChip(black_bridge_150_truncated);

    // downward angled bridges for the reverse view
    static public TwixtChip black_bridge_30_reversed_ghost = new TwixtChip(black_bridge_30_reversed);
    static public TwixtChip black_bridge_60_reversed_ghost = new TwixtChip(black_bridge_60_reversed);
    static public TwixtChip black_bridge_120_reversed_ghost = new TwixtChip(black_bridge_120_reversed);
    static public TwixtChip black_bridge_150_reversed_ghost = new TwixtChip(black_bridge_150_reversed);

    static public TwixtChip black_bridge_30_ghost = new TwixtChip(black_bridge_30);
    static public TwixtChip black_bridge_60_ghost = new TwixtChip(black_bridge_60);
    static public TwixtChip black_bridge_120_ghost = new TwixtChip(black_bridge_120);
    static public TwixtChip black_bridge_150_ghost = new TwixtChip(black_bridge_150);

    static public TwixtChip GuidelinesOn = new TwixtChip("guidelines-nomask",new double[]{0.5,0.5,1.0});
    static public TwixtChip GuidelinesOff = new TwixtChip("noguidelines-nomask",new double[]{0.5,0.5,1.0});
    
 	
    public TwixtChip[] getBridges()
    {	return(color().getBridges());
    }

    static final TwixtChip getChip(int n) 
    { 	if(n>=0 && n<allChips.size()) return((TwixtChip)allChips.elementAt(n));
    	return(null);
    }
    static final TwixtChip getChip(TwixtId n) 
    { 	if(n!=null)
    	{
    	for(int lim = allChips.size()-1; lim>=0; lim--)
    	{ TwixtChip ch = (TwixtChip)allChips.elementAt(lim);
    	  if(ch.id==n) { return(ch);}
    	}}
    	return(null);
    }
    
    public TwixtChip getAltChip(int charset)
    {	int set = charset%100;
		boolean post = charset>=100;
		if(isPeg())
		{
		 if(post) { return(altChips[0]); }
		}
		else if(altChips!=null)
    		{	
    			return(altChips[set]); 
    		}
    	return(this);
    }

    /* plain images with no mask can be noted by naming them -nomask */
    static public TwixtChip backgroundTile = new TwixtChip("background-tile-nomask",new double[]{0.5,0.5,1.0});
    static public TwixtChip backgroundReviewTile = new TwixtChip("background-review-tile-nomask",new double[]{0.5,0.5,1.0});
 
    static public TwixtChip board = new TwixtChip("board-p",new double[]{0.5,0.5,1.0});
    static public TwixtChip board_np = new TwixtChip("board-np",new double[]{0.5,0.5,1.0});
    // these are coverups for the board to change the color of the border bars when it is rotated.
    static public TwixtChip left = new TwixtChip("left",new double[]{0.5,0.5,1.0});
    static public TwixtChip top = new TwixtChip("top",new double[]{0.5,0.5,1.0});
    static public TwixtChip right = new TwixtChip("right",new double[]{0.5,0.5,1.0});
    static public TwixtChip bottom = new TwixtChip("bottom",new double[]{0.5,0.5,1.0});
    
    // rotation icons
    static public TwixtChip rotate_cw_1 = new TwixtChip("rotate-cw-1-nomask",new double[]{0.5,0.5,1.0});
    static public TwixtChip rotate_cw_2 = new TwixtChip("rotate-cw-2-nomask",new double[]{0.5,0.5,1.0});
    
    static {
    	red_peg.altChips = new TwixtChip[] { red_peg_flat };
    	black_peg.altChips = new TwixtChip[] { black_peg_flat };
    	// set up the alternate chips for rotated bridges.
    	// the basic problem is that the bridges are actually attached to one of the posts, and
    	// as the board is rotated they need to be projected in a different direction.  The default
    	// is up.  The second complication is that bridges on the board are drawn as sticks without
    	// the claws at the end, so there are a total of 8 alternates for each of 4 bridge directions.
    	red_bridge_30.altChips = new TwixtChip[] {
    			red_bridge_30,red_bridge_30,red_bridge_120,red_bridge_120,
        		red_bridge_30_truncated,red_bridge_30_reversed,red_bridge_120_reversed,red_bridge_120_truncated
    		};
    	red_bridge_60.altChips = new TwixtChip[] {
    			red_bridge_60,red_bridge_60,red_bridge_150,red_bridge_150,
        		red_bridge_60_truncated,red_bridge_60_reversed,red_bridge_150_reversed,red_bridge_150_truncated
    		};
    	red_bridge_120.altChips = new TwixtChip[] {
    			red_bridge_120,red_bridge_120,red_bridge_30,red_bridge_30,
        		red_bridge_120_truncated,red_bridge_120_reversed,red_bridge_30_truncated,red_bridge_30_reversed
    		};
    	red_bridge_150.altChips = new TwixtChip[] {
    			red_bridge_150,red_bridge_150,red_bridge_60,red_bridge_60,
        		red_bridge_150_truncated,red_bridge_150_reversed,red_bridge_60_truncated,red_bridge_60_reversed
    		};   	
    	black_bridge_30.altChips = new TwixtChip[] {
    			black_bridge_30,black_bridge_30,black_bridge_120,black_bridge_120,
        		black_bridge_30_truncated,black_bridge_30_reversed,black_bridge_120_reversed,black_bridge_120_truncated
    		};
    	black_bridge_60.altChips = new TwixtChip[] {
    			black_bridge_60,black_bridge_60,black_bridge_150,black_bridge_150,
        		black_bridge_60_truncated,black_bridge_60_reversed,black_bridge_150_reversed,black_bridge_150_truncated
    		};
    	black_bridge_120.altChips = new TwixtChip[] {
    			black_bridge_120,black_bridge_120,black_bridge_30,black_bridge_30,
        		black_bridge_120_truncated,black_bridge_120_reversed,black_bridge_30_truncated,black_bridge_30_reversed
    		};
    	black_bridge_150.altChips = new TwixtChip[] {
    			black_bridge_150,black_bridge_150,black_bridge_60,black_bridge_60,
        		black_bridge_150_truncated,black_bridge_150_reversed,black_bridge_60_truncated,black_bridge_60_reversed
    		};
    	}
    
    // same alt-chip logic for the ghost family
    static {
    	red_peg_ghost.altChips = new TwixtChip[] { red_peg_flat_ghost };
    	black_peg_ghost.altChips = new TwixtChip[] { black_peg_flat_ghost };
    	// set up the alternate chips for rotated bridges.
    	// the basic problem is that the bridges are actually attached to one of the posts, and
    	// as the board is rotated they need to be projected in a different direction.  The default
    	// is up.  The second complication is that bridges on the board are drawn as sticks without
    	// the claws at the end, so there are a total of 8 alternates for each of 4 bridge directions.
    	red_bridge_30_ghost.altChips = new TwixtChip[] {
    			red_bridge_30_ghost,red_bridge_30_ghost,red_bridge_120_ghost,red_bridge_120_ghost,
        		red_bridge_30_truncated_ghost,red_bridge_30_reversed_ghost,red_bridge_120_reversed_ghost,red_bridge_120_truncated_ghost
    		};
    	red_bridge_60_ghost.altChips = new TwixtChip[] {
    			red_bridge_60_ghost,red_bridge_60_ghost,red_bridge_150_ghost,red_bridge_150_ghost,
        		red_bridge_60_truncated_ghost,red_bridge_60_reversed_ghost,red_bridge_150_reversed_ghost,red_bridge_150_truncated_ghost
    		};
    	red_bridge_120_ghost.altChips = new TwixtChip[] {
    			red_bridge_120_ghost,red_bridge_120_ghost,red_bridge_30_ghost,red_bridge_30_ghost,
        		red_bridge_120_truncated_ghost,red_bridge_120_reversed_ghost,red_bridge_30_truncated_ghost,red_bridge_30_reversed_ghost
    		};
    	red_bridge_150_ghost.altChips = new TwixtChip[] {
    			red_bridge_150_ghost,red_bridge_150_ghost,red_bridge_60_ghost,red_bridge_60_ghost,
        		red_bridge_150_truncated_ghost,red_bridge_150_reversed_ghost,red_bridge_60_truncated_ghost,red_bridge_60_reversed_ghost
    		};   	
    	black_bridge_30_ghost.altChips = new TwixtChip[] {
    			black_bridge_30_ghost,black_bridge_30_ghost,black_bridge_120_ghost,black_bridge_120_ghost,
        		black_bridge_30_truncated_ghost,black_bridge_30_reversed_ghost,black_bridge_120_reversed_ghost,black_bridge_120_truncated_ghost
    		};
    	black_bridge_60_ghost.altChips = new TwixtChip[] {
    			black_bridge_60_ghost,black_bridge_60_ghost,black_bridge_150_ghost,black_bridge_150_ghost,
        		black_bridge_60_truncated_ghost,black_bridge_60_reversed_ghost,black_bridge_150_reversed_ghost,black_bridge_150_truncated_ghost
    		};
    	black_bridge_120_ghost.altChips = new TwixtChip[] {
    			black_bridge_120_ghost,black_bridge_120_ghost,black_bridge_30_ghost,black_bridge_30_ghost,
        		black_bridge_120_truncated_ghost,black_bridge_120_reversed_ghost,black_bridge_30_truncated_ghost,black_bridge_30_reversed_ghost
    		};
    	black_bridge_150_ghost.altChips = new TwixtChip[] {
    			black_bridge_150_ghost,black_bridge_150_ghost,black_bridge_60_ghost,black_bridge_60_ghost,
        		black_bridge_150_truncated_ghost,black_bridge_150_reversed_ghost,black_bridge_60_truncated_ghost,black_bridge_60_reversed_ghost
    		};
    	
    	// mark the primary chips with their ghosts
    	red_peg.ghosted = red_peg_ghost;
    	black_peg.ghosted = black_peg_ghost;
    	red_bridge_30.ghosted = red_bridge_30_ghost;
    	red_bridge_60.ghosted = red_bridge_60_ghost;
    	red_bridge_120.ghosted = red_bridge_120_ghost;
    	red_bridge_150.ghosted = red_bridge_150_ghost;   	
    	black_bridge_30.ghosted = black_bridge_30_ghost;
    	black_bridge_60.ghosted = black_bridge_60_ghost;
    	black_bridge_120.ghosted = black_bridge_120_ghost;
    	black_bridge_150.ghosted = black_bridge_150_ghost;
 
    	}
    static public TwixtChip Icon = new TwixtChip("twixt-icon-nomask",new double[]{0.5,0.5,1.0});
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images into the stack of
     * chips we've built
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!imagesLoaded)
		{	
		imagesLoaded = forcan.load_masked_images(Dir,allChips);
		if(imagesLoaded) { Image.registerImages(allChips); }
		}
	}   
}
