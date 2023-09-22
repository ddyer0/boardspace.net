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
package iro;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import lib.G;
import lib.Image;
import online.game.chip;
import common.CommonConfig;
import iro.IroConstants.IroId;
class ChipStack extends OStack<IroChip>
{
	public IroChip[] newComponentArray(int n) { return(new IroChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class IroChip extends chip<IroChip> implements CommonConfig
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public IroId id;
	enum IColor { Red, Blue, Yellow, Black;	};
	
	public String contentsString() { return(id==null ? file : id.name()); }
	IColor colors[] = null;
	int number = 0;
	IroChip altChip = null;
	int playerIndex = -1;
	private IroChip(IroChip from,String n,Image im)
	{
		from.altChip = this;
		altChip = from;
		file = n;
		image = im;
		id = from.id;
		number = from.number;
		randomv = from.randomv;
		scale = from.scale;
		colors = from.colors;
		playerIndex = from.playerIndex;
	};
	private static double [] defaultScale = {0.5,0.5,1 };
	
	// constructor for the chips on the board, which are the only things that are digestable.
	private IroChip(String na,IroId ty)
	{	
		scale=defaultScale;
		file = na;
		id = ty;
		
		if(id==IroId.Black) { playerIndex = 1;}
		if(id==IroId.White) { playerIndex = 0; }

		randomv = r.nextLong();
		int idx = na.indexOf("-")+1;
		if(ty!=null)
		{
		int sz  = id==IroId.Tile ? 4 : 3;
		colors = new IColor[sz];
		for(int i=0;i<sz;i++) {
			switch (na.charAt(idx++))
			{
			case 'r': colors[i] = IColor.Red;  break;
			case 'b': colors[i] = IColor.Blue;  break;
			case 'x': colors[i] = IColor.Black; break;
			case 'y': colors[i] = IColor.Yellow; break;
			default: G.Error("Not expecting char ",idx);
			}
		}
		}
		number = stoneChips.size();
		stoneChips.push(this);

	}
	
	static IroChip tiles[] = {
		new IroChip("tile-rbxy",IroId.Tile),
		new IroChip("tile-rbyx",IroId.Tile),
		new IroChip("tile-rxby",IroId.Tile),
		new IroChip("tile-rxyb",IroId.Tile),
		new IroChip("tile-rybx",IroId.Tile),
		new IroChip("tile-ryxb",IroId.Tile),
	};
	static IroChip bchips[] = {
		new IroChip("chip-bbr",IroId.Black),
		new IroChip("chip-bby",IroId.Black),
		new IroChip("chip-bbx",IroId.Black),
	
		new IroChip("chip-rrb",IroId.Black),
		new IroChip("chip-rrx",IroId.Black),
		new IroChip("chip-rry",IroId.Black),

		new IroChip("chip-xxb",IroId.Black),
		new IroChip("chip-xxr",IroId.Black),
		new IroChip("chip-xxy",IroId.Black),

		new IroChip("chip-yyb",IroId.Black),
		new IroChip("chip-yyr",IroId.Black),
		new IroChip("chip-yyx",IroId.Black),

		
		new IroChip("chip-brx",IroId.Black),
		new IroChip("chip-bry",IroId.Black),
		new IroChip("chip-bxy",IroId.Black),
		new IroChip("chip-ryx",IroId.Black),
	};
	
	static IroChip wchips[] = {
			new IroChip("light-bbr",IroId.White),
			new IroChip("light-bby",IroId.White),
			new IroChip("light-bbx",IroId.White),
		
			new IroChip("light-rrb",IroId.White),
			new IroChip("light-rrx",IroId.White),
			new IroChip("light-rry",IroId.White),

			new IroChip("light-xxb",IroId.White),
			new IroChip("light-xxr",IroId.White),
			new IroChip("light-xxy",IroId.White),

			new IroChip("light-yyb",IroId.White),
			new IroChip("light-yyr",IroId.White),
			new IroChip("light-yyx",IroId.White),

			
			new IroChip("light-brx",IroId.White),
			new IroChip("light-bry",IroId.White),
			new IroChip("light-bxy",IroId.White),
			new IroChip("light-rxy",IroId.White),
		};

	
	public int chipNumber() { return number; }
	
    // indexes into the balls array, usually called the rack
    static final IroChip getChip(int n) { return((IroChip)stoneChips.elementAt(n)); }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public IroChip backgroundTile = new IroChip("background-tile-nomask",null);
    static public IroChip backgroundReviewTile = new IroChip("background-review-tile-nomask",null);
 
 
    public static IroChip Icon = new IroChip("icon-nomask",null);

    public static IroChip ColorBlind = new IroChip("colorblind-nomask",null);
    public static IroChip Normal = new IroChip("normal-nomask",null);
    
    static private void loadTiles(ImageLoader forcan,String dir)
    {	String names[] = new String[tiles.length];
    	String altNames[] = new String[tiles.length]; 
    	Image mask = forcan.load_image(dir,"tile-mask");
    	for(int i=0;i<names.length;i++) { names[i]=tiles[i].getName(); altNames[i]=tiles[i].getName()+"-s"; }
		Image im[] = forcan.load_images(dir,names,mask);
		Image altim[] = forcan.load_images(dir,altNames,mask);
		for(int i=0;i<names.length;i++) 
			{ 
			  tiles[i].image = im[i];
			  new IroChip(tiles[i],altNames[i],altim[i]);			  
			}
    }
    static private void loadChips(ImageLoader forcan,String dir,IroChip chips[])
    {
    	for(IroChip chip : chips)
    	{
    		Image mask = forcan.load_image(dir,chip.file+"-mask");
    		String names[] = new String[] { chip.file,chip.file+"-s"};
    		Image main[] = forcan.load_images(dir,names,mask); 
    		chip.image = main[0];
    		new IroChip(chip,names[1],main[1]);
    	}
    }
   
    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images into the stack of
     * chips we've built
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String dir)
	{	if(!imagesLoaded)
		{	
		loadTiles(forcan,dir);
		loadChips(forcan,dir,bchips);
		loadChips(forcan,dir,wchips);
		imagesLoaded = forcan.load_masked_images(dir,stoneChips);
		}
	}   
	
	public IroChip getAltChip(int set)
	{	if(set!=0 && altChip!=null) { return altChip; }
		return this;
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
}
