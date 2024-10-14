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
package ordo;

import bridge.Config;
import lib.AR;
import lib.Drawable;
import lib.DrawableImageStack;
import lib.Image;
import lib.ImageLoader;
import lib.MultiGlyph;
import lib.Random;
import online.game.chip;
import ordo.OrdoConstants.OrdoId;

public class OrdoChip extends chip<OrdoChip> implements Config
	{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static DrawableImageStack allArt = new DrawableImageStack();
	
	private static boolean imagesLoaded = false;


	public int chipNumber() 
	{	return(AR.indexOf(chips,this));
	}
	public static OrdoChip getChipNumber(int id)
	{	return(chips[id]);
	}

	public OrdoId id = null;		// chips/images that are expected to be visible to the user interface should have an ID

	// constructor for chips not expected to be part of the UI
	private OrdoChip(String na,double scl[],DrawableImageStack art)
	{	file = na;
		randomv = r.nextLong();
		scale = scl;
		art.push(this);
	}
	// constructor for chips expected to be part of the UI
	private OrdoChip(String na,double scl[],OrdoId uid,DrawableImageStack art)
	{	this(na,scl,art);
		id = uid;
	}
	

	
	public String toString()
	{	return("<"+ id+" "+file+">");
	}
	public String contentsString() 
	{ return(id==null?"":id.shortName); 
	}

	static private OrdoChip tiles[] =
		{
		new OrdoChip("light-tile",new double[]{0.5,0.5,1.0},allChips),	
    	new OrdoChip("dark-tile",new double[]{0.5,0.5,1.0},allChips),
		};
	
	public static OrdoChip white = new OrdoChip("white-chip-np",new double[]{0.53,0.430,1.38},OrdoId.White_Chip_Pool,allChips);
	public static OrdoChip black = new OrdoChip("black-chip-np",new double[]{0.53,0.402,1.38},OrdoId.Black_Chip_Pool,allChips); 

	static private OrdoChip chips[] = 
		{
		white,
    	black,
		};
	
	public static MultiGlyph blackKing = new MultiGlyph();
	public static MultiGlyph whiteKing = new MultiGlyph();
	
	static {
		blackKing.append(black,new double[]{1.0,0,0});
		blackKing.append(black,new double[]{1.0,0,-0.1});
		whiteKing.append(white,new double[]{1.0,0,0});
		whiteKing.append(white,new double[]{1.0,0,-0.1});
	}
	
	public Drawable getKing()
	{	switch(id)
		{case White_Chip_Pool:	return(whiteKing); 
		case Black_Chip_Pool: return(blackKing);
		default: return(null);
		}
	}
 
	public static OrdoChip getTile(int color)
	{	return(tiles[color]);
	}
	public static OrdoChip getChip(int color)
	{	return(chips[color]);
	}

	
	public static OrdoChip backgroundTile = new OrdoChip( "background-tile-nomask",null,allChips);
	public static OrdoChip backgroundReviewTile = new OrdoChip( "background-review-tile-nomask",null,allChips);
	
	public static OrdoChip OrdoIcon = new OrdoChip("ordo-icon-nomask",null,allArt);
 
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(StonesDir,allChips)
					&& forcan.load_masked_images(ImageDir, allArt)
				;
		Image.registerImages(allChips);
		Image.registerImages(allArt);
		check_digests(allChips);
		}
	}


}
