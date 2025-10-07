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
package checkerboard;

import bridge.Config;
import checkerboard.CheckerConstants.CheckerId;
import lib.AR;
import lib.Drawable;
import lib.DrawableImageStack;
import lib.G;
import lib.Image;
import lib.ImageLoader;
import lib.MultiGlyph;
import lib.Random;
import online.game.chip;

public class CheckerChip extends chip<CheckerChip> implements Config
	{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static DrawableImageStack allArt = new DrawableImageStack();
	
	private static boolean imagesLoaded = false;

	enum ChipColor { White,Black };

	public int chipNumber() 
	{	return(AR.indexOf(chips,this));
	}
	public static CheckerChip getChipNumber(int id)
	{	return(chips[id]);
	}

	public CheckerId id = null;		// chips/images that are expected to be visible to the user interface should have an ID
	public ChipColor color = null;

	// constructor for chips not expected to be part of the UI
	private CheckerChip(String na,double scl[],DrawableImageStack art)
	{	file = na;
		randomv = r.nextLong();
		scale = scl;
		art.push(this);
	}
	// constructor for chips expected to be part of the UI
	private CheckerChip(String na,double scl[],CheckerId uid,ChipColor co,DrawableImageStack art)
	{	this(na,scl,art);
		id = uid;
		color = co;
	}


	
	public String toString()
	{	return("<"+ id+" "+file+">");
	}
	public String contentsString() 
	{ return(id==null?"":id.shortName); 
	}

	static private CheckerChip tiles[] =
		{
		new CheckerChip("light-tile",new double[]{0.5,0.5,1.0},allChips),	
    	new CheckerChip("dark-tile",new double[]{0.5,0.5,1.0},allChips),
		};
	
	public static CheckerChip white = new CheckerChip("white-chip-np",new double[]{0.53,0.430,1.38},CheckerId.White_Chip_Pool,ChipColor.White,allChips);
	public static CheckerChip black = new CheckerChip("black-chip-np",new double[]{0.53,0.402,1.38},CheckerId.Black_Chip_Pool,ChipColor.Black,allChips); 
	public static CheckerChip whiteKing = new CheckerChip("white-king-np",new double[]{0.53,0.430,1.38},CheckerId.White_King,ChipColor.White,allChips);
	public static CheckerChip blackKing = new CheckerChip("black-king-np",new double[]{0.53,0.402,1.38},CheckerId.Black_King,ChipColor.Black,allChips); 

	public static CheckerChip getKing(CheckerChip ch)
	{
		if(ch==white) { return whiteKing; }
		if(ch==black) { return blackKing; }
		throw G.Error("shoudln't ask for king %s",ch);
	}
	public static CheckerChip unKing(CheckerChip ch)
	{
		if(ch==whiteKing) { return white; }
		if(ch==blackKing) { return black; }
		throw G.Error("shoudln't ask for unking %s",ch);
	}
	static private CheckerChip chips[] = 
		{
		white,
    	black,
    	whiteKing,
    	blackKing,
		};
	
	private static MultiGlyph blackKingGlyph = new MultiGlyph();
	private static MultiGlyph whiteKingGlyph = new MultiGlyph();
	
	static {
		blackKingGlyph.append(black,new double[]{1.0,0,0});
		blackKingGlyph.append(black,new double[]{1.0,0,-0.1});
		whiteKingGlyph.append(white,new double[]{1.0,0,0});
		whiteKingGlyph.append(white,new double[]{1.0,0,-0.1});
	}
	
	public Drawable getKing()
	{	switch(id)
		{case White_Chip_Pool:	return(whiteKingGlyph); 
		case Black_Chip_Pool: return(blackKingGlyph);
		default: return(null);
		}
	}
 
	public static CheckerChip getTile(int color)
	{	return(tiles[color]);
	}
	public static CheckerChip getChip(int color)
	{	return(chips[color]);
	}

	
	public static CheckerChip backgroundTile = new CheckerChip( "background-tile-nomask",null,allChips);
	public static CheckerChip backgroundReviewTile = new CheckerChip( "background-review-tile-nomask",null,allChips);
	public static CheckerChip liftIcon = new CheckerChip( "lift-icon-nomask",null,null,null,allChips);	
	public static CheckerChip international = new CheckerChip("international",null,null,null,allArt);
	public static CheckerChip anti = new CheckerChip("antidraughts",null,null,null,allArt);
	public static CheckerChip frisian = new CheckerChip("frisian",null,null,null,allArt);
	public static CheckerChip turkish = new CheckerChip("turkish",null,null,null,allArt);
	public static CheckerChip american = new CheckerChip("american",null,null,null,allArt);
	public static CheckerChip russian = new CheckerChip("russian",null,null,null,allArt);
	public static CheckerChip bashni = new CheckerChip("bashni",null,null,null,allArt);
	public static CheckerChip stacks = new CheckerChip("stacks",null,null,null,allArt);
	public static CheckerChip dameo = new CheckerChip("dameo",null,null,null,allArt);
	
	public static CheckerChip CheckerIcon = new CheckerChip("checkers-icon-nomask",null,null,null,allArt);
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(StonesDir,allChips)
					&& forcan.load_masked_images(ImageDir, allArt)
				;
		if(imagesLoaded) { Image.registerImages(allChips); Image.registerImages(allArt);}
		check_digests(allChips);
		}
	}


}
