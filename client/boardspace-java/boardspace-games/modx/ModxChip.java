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
package modx;

import lib.*;
import online.game.chip;
/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too.
 * 
 */
class ChipStack extends OStack<ModxChip> implements Digestable
{
	public ModxChip[] newComponentArray(int n) { return(new ModxChip[n]); }

	public long Digest(Random r) {
		long v = 0;
		for(int i=0;i<size();i++) { v ^= elementAt(i).Digest(r)*(i+12456); }
		return(v);	}
}
public class ModxChip extends chip<ModxChip> implements ModxConstants
	{	
	enum PieceType { Joker, X, Flat }
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static ChipStack allChips = new ChipStack();
	private static boolean imagesLoaded = false;
	public PieceType type;
	private int chipIndex;
	public int chipNumber() { return(chipIndex); }
	public static ModxChip getChipNumber(int id)
	{	return(allChips.elementAt(id));
	}

	public ModxId id = null;		// chips/images that are expected to be visible to the user interface should have an ID
	public int colorIndex() { return(id.colorIndex); }
	// constructor for chips not expected to be part of the UI
	private ModxChip(String na,double scl[])
	{	file = na;
		chipIndex=allChips.size();
		randomv = r.nextLong();
		scale = scl;
		allChips.push(this);
	}
	
	// constructor for chips expected to be part of the UI
	private ModxChip(String na,double scl[],ModxId uid,PieceType ptype)
	{	this(na,scl);
		id = uid;
		type = ptype;
	}

	public String toString()
	{	return("<"+ id+" #"+chipIndex+">");
	}
	public String contentsString() 
	{ return(id==null?"":id.shortName); 
	}

	public static ModxChip board = new ModxChip("board",new double[]{0.5,0.5,1.0});
	public static ModxChip board_np = new ModxChip("board-np",new double[]{0.5,0.5,1.0});

	static private double chipPos[] = new double[]{0.553,0.583,0.822};
	static private double flatPos[] = new double[]{0.539,0.559,0.835};
	static private ModxChip chips[] = 
		{
		new ModxChip("red-x",chipPos,ModxId.Red_Chip_Pool,PieceType.X),
		new ModxChip("black-x",chipPos,ModxId.Black_Chip_Pool,PieceType.X),
		new ModxChip("yellow-x",chipPos,ModxId.Yellow_Chip_Pool,PieceType.X),
		new ModxChip("orange-x",chipPos,ModxId.Orange_Chip_Pool,PieceType.X),
		};

	static private ModxChip flats[] = 
		{
		new ModxChip("red-flat",flatPos,ModxId.Red_Flat_Pool,PieceType.Flat),
		new ModxChip("black-flat",flatPos,ModxId.Black_Flat_Pool,PieceType.Flat),
		new ModxChip("yellow-flat",flatPos,ModxId.Yellow_Flat_Pool,PieceType.Flat),
		new ModxChip("orange-flat",flatPos,ModxId.Orange_Flat_Pool,PieceType.Flat),
		};
	
	static public ModxChip Joker = new ModxChip("joker",chipPos,ModxId.Joker_Pool,PieceType.Joker);
	
	static public boolean isJoker(ModxChip c) { return(c==Joker); }
	public boolean isJoker() { return(this==Joker); }
	static public boolean isFlat(ModxChip p) { return(p==null ? false : p.isFlat());}  
	public boolean isFlat() { return(type==PieceType.Flat); }
	static public boolean isX(ModxChip p) { return(p==null ? false : p.isX()); }
	public boolean isX() { return(type==PieceType.X); }
	static public boolean isXorJoker(ModxChip p) { return(p==null ? false : (p.isX()||p.isJoker())); }
	static public ModxChip Icon = new ModxChip("modx-icon-nomask",chipPos,ModxId.Joker_Pool,PieceType.Joker);
	
	public static ModxChip getChip(int color)
	{	return(chips[color]);
	}

	public static ModxChip getFlat(int color)
	{	return(flats[color]);
	}
	
	public static ModxChip backgroundTile = new ModxChip( "background-tile-nomask",null);
	public static ModxChip backgroundReviewTile = new ModxChip( "background-review-tile-nomask",null);
	public static ModxChip liftIcon = new ModxChip( "lift-icon-nomask",null);
	

	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(ImageDir,allChips);
		}
	}

	public boolean matches(ModxCell cell,ModxCell filled)
	{	if(cell==null) { return(false); }
		if(cell==filled) { return(true); }
		ModxChip top = cell.topChip();
		switch(type)
		{
		case Flat: throw G.Error("Not expected");
		case Joker:
		case X: return((top==this) || (top==Joker));
		default: return(false);
		}
	}


}
