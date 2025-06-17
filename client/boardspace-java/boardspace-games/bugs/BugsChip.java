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
package bugs;

import lib.CompareTo;
import lib.DrawableImageStack;
import lib.GC;
import lib.Graphics;
import lib.HitPoint;
import lib.Image;
import lib.ImageLoader;
import lib.ImageStack;
import lib.OStack;
import lib.Random;
import lib.exCanvas;
import online.game.chip;

import java.awt.Color;
import java.awt.Font;
import bugs.BugsConstants.BugsId;
import bugs.data.Profile;
import common.CommonConfig;
class ChipStack extends OStack<BugsChip>
{
	public BugsChip[] newComponentArray(int n) { return(new BugsChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class BugsChip extends chip<BugsChip> implements CommonConfig,CompareTo<BugsChip>
{	static int BUGOFFSET =      0x10000;
	static int TAXONOMYOFFSET = 0x20000;
	static DrawableImageStack bugCards = new DrawableImageStack();
	
	public BugsChip() {}
	public boolean isBugCard() { return false; }
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public BugsId id;
	public String contentsString() { return(id==null ? file : id.name()); }
	public String name() { return id.name(); }
	// constructor for the chips on the board, which are the only things that are digestable.
	private BugsChip(String na,double[]sc,BugsId con)
	{	
		scale=sc;
		file = na;
		id = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		otherChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private BugsChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	
	static public BugsChip PrarieTile = new BugsChip("prarie",new double[]{0.50,0.510,1.44},BugsId.Prarie);
	static public BugsChip GroundTile = new BugsChip("ground",new double[]{0.50,0.510,1.44},BugsId.Ground);
	static public BugsChip MarshTile = new BugsChip("marsh",new double[]{0.50,0.510,1.44},BugsId.Marsh);
	static public BugsChip ForestTile = new BugsChip("forest",new double[]{0.50,0.510,1.44},BugsId.Forest);

	static BugsChip Tiles[] = {
			PrarieTile,GroundTile,MarshTile,ForestTile,
	};
	static public BugsChip Yellow = new BugsChip("yellow-cube",new double[]{0.50,0.510,1.44});
	static public BugsChip Green = new BugsChip("green-cube",new double[]{0.47,0.49,1.44});
	static public BugsChip Blue = new BugsChip("blue-cube",new double[]{0.50,0.510,1.44});
	static public BugsChip Red = new BugsChip("red-cube",new double[]{0.47,0.49,1.44});

	static BugsChip getGoalCard(int n) { return null; }
	static BugsChip getBugCard(int n) { return null; }
	
    // indexes into the balls array, usually called the rack
    static final BugsChip getChip(int n) 
    { return(
    		n>=TAXONOMYOFFSET
    			? GoalCard.getGoalCard(n)
    			: n>=BUGOFFSET 
    				? BugCard.getBugCard(n) 
    				: BugsId.values()[n].chip); 
    }
    /**
     * this is the basic hook to substitute an alternate chip for display.  The canvas getAltChipSet
     * method is called from drawChip, and the result is passed here to substitute a different chip.
     * 
     */
	public BugsChip getAltChip(int set)
	{	
		return this;
	}


    /* plain images with no mask can be noted by naming them -nomask */
    static public BugsChip backgroundTile = new BugsChip("background-tile-nomask",null);
    static public BugsChip backgroundReviewTile = new BugsChip("background-review-tile-nomask",null);
   

    public static BugsChip Icon = new BugsChip("hex-icon-nomask",null);

    static double defaultScale[] = {0.5,0.5,1.0};
    public static BugsChip Parasite = new BugsChip("parasite.png",defaultScale);
    public static BugsChip Scavenger = new BugsChip("scavenger.png",defaultScale);
    public static BugsChip Vegetarian = new BugsChip("vegetarian.png",defaultScale);
    public static BugsChip Predator = new BugsChip("carnivore.png",defaultScale);
    public static BugsChip Wings = new BugsChip("wings.png",defaultScale);
    public static BugsChip Negavore = new BugsChip("negavore.png",defaultScale);
   
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
		otherChips.autoloadMaskGroup(Dir,"tile-mask",Tiles);
		imagesLoaded = forcan.load_masked_images(Dir,otherChips);
		Image.registerImages(otherChips);
		}
	}   
	/**
	 * this is a debugging interface to provide information about images memory consumption
	 * in the "show images" option.
	 * It's especially useful in applications that are very image intensive.
	 * @param imstack
	 * @return size of images (in megabytes)
	 */
	public static double imageSize(ImageStack imstack)
	    {	double sum = otherChips.imageSize(imstack);
	    	return(sum);
	    }
	   
	/*
	// override for drawChip can draw extra ornaments or replace drawing entirely
	public void drawChip(Graphics gc,
	            exCanvas canvas,
	            int SQUARESIZE,
	            double xscale,
	            int cx,
	            int cy,
	            java.lang.String label)
	    {	super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, label);

	    }
	 */
	
	public static BugsChip cardBack = new BugsChip("cards",new double[] {0.5,0.5,1.0});
	public static BugsChip goalCardBack = new BugsChip("goals",new double[] {0.5,0.5,1.0});
	public static BugsChip yellowBack = new BugsChip("yellowbackground-nomask",new double[] {0.5,0.5,1.0});
	public static BugsChip brownBack = new BugsChip("brownbackground-nomask",new double[] {0.5,0.5,1.0});
	public static BugsChip greenBack = new BugsChip("greenbackground-nomask",new double[] {0.5,0.5,1.0});
	public static BugsChip blueBack = new BugsChip("bluebackground-nomask",new double[] {0.5,0.5,1.0});
	public static BugsChip blankBack = new BugsChip("blank-card",new double[] {0.5,0.5,1.0});
	
	public static String BACK = NotHelp+"_back_";	// the | causes it to be passed in rather than used as a tooltip
	public static String NORMAL = NotHelp+"_normal_";
	public static String NOHIT = NotHelp+"_nohit_";
	/*
    public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{
		boolean isBack = BACK.equals(label);
		if(cardBack!=null && isBack)
		{
		 cardBack.drawChip(gc,canvas,SQUARESIZE, xscale, cx, cy,null);
		}
		else
		{ super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, label);
		}
	}
	*/
	public int compareTo(BugsChip o) {
		return 0;
	}
	public Profile getProfile() { return null; }
	
	public boolean drawChip(Graphics gc,exCanvas canvas,BugsCell c,HitPoint highlight,int squareWidth,double scale,int cx,int cy,String label)
	{
		boolean hit = drawChip(gc,canvas,squareWidth,cx,cy,highlight,c.rackLocation(),label,1.0,1.0);
		if(hit) { highlight.hitObject = c;}
		return hit;
	}

	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{	if(image!=null)
		{	super.drawChip(gc,canvas,SQUARESIZE,xscale,cx,cy,label);
		}
		else
		{
		actualDrawChip(gc,canvas.standardPlainFont(),null,null,SQUARESIZE,cx,cy,null);
		}
	}
	public boolean actualDrawChip(Graphics gc, Font baseFont,
			HitPoint highlight, BugsId id,
			int SQUARESIZE,	int cx, int cy,HitPoint hitAny) 
	{	GC.frameRect(gc,Color.blue,cx-SQUARESIZE/2,cy-SQUARESIZE/4,SQUARESIZE,SQUARESIZE/2);
		return false;
	}


}
