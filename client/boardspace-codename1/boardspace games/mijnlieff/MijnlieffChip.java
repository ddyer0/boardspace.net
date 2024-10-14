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
package mijnlieff;

import lib.DrawableImageStack;
import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import mijnlieff.MijnlieffConstants.MColor;
import mijnlieff.MijnlieffConstants.MijnlieffId;
import online.game.chip;

import common.CommonConfig;
class ChipStack extends OStack<MijnlieffChip>
{
	public MijnlieffChip[] newComponentArray(int n) { return(new MijnlieffChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class MijnlieffChip extends chip<MijnlieffChip> implements CommonConfig
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public MijnlieffId id;
	public MColor color;
	public String contentsString() { return(id==null ? file : id.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private MijnlieffChip(String na,double[]sc,MColor mc,MijnlieffId con)
	{	
		scale=sc;
		file = na;
		id = con;
		color = mc;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		otherChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private MijnlieffChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		otherChips.push(this);
	}
	
	public int chipNumber() { return(id==null?-1:id.ordinal()); }
	


	static double ChipScale[] = {0.5,0.5,1.0};
	
	static public MijnlieffChip Dark_push = new MijnlieffChip("dark-push",ChipScale,MColor.Dark,MijnlieffId.Dark_Push);
	static public MijnlieffChip Dark_pull = new MijnlieffChip("dark-pull",ChipScale,MColor.Dark,MijnlieffId.Dark_Pull);
	static public MijnlieffChip Dark_diagonal = new MijnlieffChip("dark-diagonal",ChipScale,MColor.Dark,MijnlieffId.Dark_Diagonal);
	static public MijnlieffChip Dark_orthogonal = new MijnlieffChip("dark-orthogonal",ChipScale,MColor.Dark,MijnlieffId.Dark_Orthogonal);

	static public MijnlieffChip Light_push = new MijnlieffChip("light-push",ChipScale,MColor.Light,MijnlieffId.Light_Push);
	static public MijnlieffChip Light_pull = new MijnlieffChip("light-pull",ChipScale,MColor.Light,MijnlieffId.Light_Pull);
	static public MijnlieffChip Light_diagonal = new MijnlieffChip("light-diagonal",ChipScale,MColor.Light,MijnlieffId.Light_Diagonal);
	static public MijnlieffChip Light_orthogonal = new MijnlieffChip("light-orthogonal",ChipScale,MColor.Light,MijnlieffId.Light_Orthogonal);

	
	// indexes into the balls array, usually called the rack
    static final MijnlieffChip getChip(int n) { return(MijnlieffId.values()[n].chip); }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public MijnlieffChip backgroundTile = new MijnlieffChip("background-tile-nomask",null);
    static public MijnlieffChip backgroundReviewTile = new MijnlieffChip("background-review-tile-nomask",null);
   
 
    public static MijnlieffChip Icon = new MijnlieffChip("hex-icon-nomask",null);

    static private double noScale[] = {0.50,0.50,1};
    static public MijnlieffChip board = new MijnlieffChip("board",noScale);
    
    
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images into the stack of
     * chips we've built
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!imagesLoaded)
		{	imagesLoaded = forcan.load_masked_images(Dir,otherChips);
		Image.registerImages(otherChips);
		}
	}   

}
