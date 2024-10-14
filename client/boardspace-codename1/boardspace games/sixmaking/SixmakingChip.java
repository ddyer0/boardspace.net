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
package sixmaking;

import lib.Drawable;
import lib.DrawableImageStack;
import lib.Image;
import lib.ImageLoader;
import lib.MultiGlyph;
import lib.Random;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too.
 * 
 */
public class SixmakingChip extends chip<SixmakingChip> implements SixmakingConstants
{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;


	private int chipIndex;
	public int chipNumber() { return(chipIndex); }
	public static SixmakingChip getChipNumber(int id)
	{	return((SixmakingChip)allChips.elementAt(id));
	}

	public SixmakingId id = null;		// chips/images that are expected to be visible to the user interface should have an ID

	// constructor for chips not expected to be part of the UI
	private SixmakingChip(String na,double scl[])
	{	file = na;
		chipIndex=allChips.size();
		randomv = r.nextLong();
		scale = scl;
		allChips.push(this);
	}
	// constructor for chips expected to be part of the UI
	private SixmakingChip(String na,double scl[],SixmakingId uid)
	{	this(na,scl);
		id = uid;
	}
	
	public String toString()
	{	return("<"+ id+" #"+chipIndex+">");
	}
	public String contentsString() 
	{ return(id==null?"":id.shortName); 
	}
	public static SixmakingChip white = new SixmakingChip("white-chip-np",new double[]{0.538,0.460,1.38},SixmakingId.White_Chip_Pool);
	public static SixmakingChip black = new SixmakingChip("black-chip-np",new double[]{0.538,0.460,1.38},SixmakingId.Black_Chip_Pool); 
	
	public static SixmakingChip white_pawn = new SixmakingChip("white-pawn",new double[]{0.5,0.35,0.31},null);
	public static SixmakingChip black_pawn = new SixmakingChip("black-pawn",new double[]{0.5,0.35,0.31},null);

	public static SixmakingChip white_rook = new SixmakingChip("white-rook",new double[]{0.47,0.30,0.31},null);
	public static SixmakingChip black_rook = new SixmakingChip("black-rook",new double[]{0.47,0.30,0.31},null);

	public static SixmakingChip white_knight = new SixmakingChip("white-knight",new double[]{0.5,0.35,0.35},null);
	public static SixmakingChip black_knight = new SixmakingChip("black-knight",new double[]{0.5,0.35,0.35},null);

	public static SixmakingChip white_bishop = new SixmakingChip("white-bishop",new double[]{0.56,0.35,0.35},null);
	public static SixmakingChip black_bishop = new SixmakingChip("black-bishop",new double[]{0.56,0.35,0.35},null);

	public static SixmakingChip white_queen = new SixmakingChip("white-queen",new double[]{0.5,0.32,0.33},null);
	public static SixmakingChip black_queen = new SixmakingChip("black-queen",new double[]{0.5,0.32,0.33},null);

	public static SixmakingChip white_king = new SixmakingChip("white-king",new double[]{0.45,0.35,0.35},null);
	public static SixmakingChip black_king = new SixmakingChip("black-king",new double[]{0.45,0.35,0.35},null);

	public static SixmakingChip flat = new SixmakingChip("flat",new double[]{0.55,0.55,1.4},null);
	public static SixmakingChip blank = new SixmakingChip("blank",new double[]{0.3,0.60,0.1},null);
	
	public static SixmakingChip chessIconOff = new SixmakingChip("chessIconOff-nomask",new double[]{0.3,0.60,0.1},null);
	public static SixmakingChip chessIconOn = new SixmakingChip("chessIconOn-nomask",new double[]{0.3,0.60,0.1},null);

	public static SixmakingChip caps[][] =
			{{white_pawn,white_rook,white_knight,white_bishop,white_queen,white_king},
		{black_pawn,black_rook,black_knight,black_bishop,black_queen,black_king}};
	
	public int playerIndex() 
		{ if(this==white) { return(0); }
		  if(this==black) { return(1); }
		  return(-1);
		}
	static private SixmakingChip chips[] = 
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
	// kings for use in animations, not for use on the board.
	static public MultiGlyph kings[] = 
		{
		whiteKing,
		blackKing
		};
	
	public Drawable getKing()
	{
		return(kings[playerIndex()]);
	}
 
	public static SixmakingChip getChip(int color)
	{	return(chips[color]);
	}

	public static SixmakingChip board = new SixmakingChip("tileboard5",null);
	public static SixmakingChip board_np = new SixmakingChip("board-np",null);
	
	public static SixmakingChip backgroundTile = new SixmakingChip( "background-tile-nomask",null);
	public static SixmakingChip backgroundReviewTile = new SixmakingChip( "background-review-tile-nomask",null);
	public static SixmakingChip liftIcon = new SixmakingChip( "lift-icon-nomask",null);
	

	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(ImageDir,allChips);
		if(imagesLoaded) { Image.registerImages(allChips); }
		}
	}


}
