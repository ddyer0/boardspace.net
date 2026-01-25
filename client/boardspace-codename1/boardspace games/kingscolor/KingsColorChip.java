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
package kingscolor;

import lib.DrawableImageStack;
import lib.G;
import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import lib.exCanvas;
import online.game.chip;
import common.CommonConfig;
import kingscolor.KingsColorConstants.GridColor;
import kingscolor.KingsColorConstants.ColorId;
import kingscolor.KingsColorConstants.PieceType;

class ChipStack extends OStack<KingsColorChip>
{
	public KingsColorChip[] newComponentArray(int n) { return(new KingsColorChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by pushfight;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class KingsColorChip extends chip<KingsColorChip> implements CommonConfig
{

	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack stoneChips = new DrawableImageStack();
	private static DrawableImageStack chessChips = new DrawableImageStack();
	private static DrawableImageStack otherChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public ColorId colorId;
	public PieceType pieceType;
	private int index = 0;
	public String contentsString() { return(colorId==null ? file : colorId.name()+" "+pieceType.name()); }

	// constructor for the chips on the board, which are the only things that are digestable.
	private KingsColorChip(String na,double[]sc,ColorId con)
	{	
		scale=sc;
		file = na;
		colorId = con;
		if(con!=null) { con.chip = this; }
		randomv = r.nextLong();
		index = stoneChips.size();
		stoneChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private KingsColorChip(String na,double[]sc)
	{	
		scale=sc;
		file = na;
		index = otherChips.size();
		otherChips.push(this);
	}
	
	public KingsColorChip(String string, ColorId black2, PieceType king, double[] ds) {
		file = string;
		colorId = black2;
		pieceType = king;
		scale = ds;
		randomv = r.nextLong();
		index = chessChips.size();
		chessChips.push(this);
	}  
	public int chipNumber() 
	{ 	G.Assert(pieceType!=null,"must be a chess PieceType");
		return(index); 
	}
	public static KingsColorChip getChip(int n)
	{
		return((KingsColorChip)chessChips.elementAt(n));
	}

    /* plain images with no mask can be noted by naming them -nomask */
    static public KingsColorChip backgroundTile = new KingsColorChip("background-tile-nomask",null);
    static public KingsColorChip backgroundReviewTile = new KingsColorChip("background-review-tile-nomask",null);
   
    static private double hexScale[] = {0.50,0.50,1.60};
    static public KingsColorChip hexTile = new KingsColorChip("hextile",hexScale,null);
    static public KingsColorChip hexTile_green = new KingsColorChip("hextile-green",hexScale,null);
    static public KingsColorChip hexTile_light = new KingsColorChip("hextile-light",hexScale,null);
    static public KingsColorChip hexTile_pen = new KingsColorChip("hextile-pen",hexScale,null);

    static {
    	GridColor.Green.chip = hexTile_green;
    	GridColor.Light.chip = hexTile_light;
    	GridColor.Gray.chip = hexTile;
    }
    
    public static KingsColorChip Icon = new KingsColorChip("kingscolor-icon-nomask",null);

	public static KingsColorChip whiteRook = new KingsColorChip("white-rook",
			ColorId.White,PieceType.Rook,
			new double []{0.464,0.494,1.0});
	public static KingsColorChip whiteBishop = new KingsColorChip("white-bishop",
			ColorId.White,PieceType.Bishop,
			new double []{0.527,0.545,1.0});
	public static KingsColorChip whiteQueen = new KingsColorChip("white-queen",
			ColorId.White,PieceType.Queen,
			new double []{0.505,0.508,0.978});
	public static KingsColorChip whiteKing = new KingsColorChip("white-king",
			ColorId.White,PieceType.King,
			new double []{0.505,0.49,0.891});

	public static KingsColorChip Whites[] = {
		whiteKing,whiteQueen,whiteRook,whiteBishop	
		};
	public static KingsColorChip blackRook = new KingsColorChip("black-rook",
			ColorId.Black,PieceType.Rook,
			new double []{0.464,0.494,1.0});
	public static KingsColorChip blackBishop = new KingsColorChip("black-bishop",
			ColorId.Black,PieceType.Bishop,
			new double []{0.527,0.545,1.0});
	public static KingsColorChip blackQueen = new KingsColorChip("black-queen",
			ColorId.Black,PieceType.Queen,
			new double []{0.505,0.508,0.978});
	public static KingsColorChip blackKing = new KingsColorChip("black-king",
			ColorId.Black,PieceType.King,
			new double []{0.505,0.49,0.891});
	
	public static KingsColorChip Blacks[] = {
			blackKing,blackQueen,blackRook,blackBishop	
			};
	public static KingsColorChip getChip(ColorId c,PieceType k)
	{
		KingsColorChip chips[] = c==ColorId.White ? Whites : Blacks;
		for(KingsColorChip chip : chips)
		{
			if(chip.pieceType == k) { return(chip); }
		}
		throw G.Error("No chip found for %s %s", c,k);
	}
   
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
		String chessDir = G.replace(Dir,"kingscolor","chess");
		imagesLoaded = forcan.load_masked_images(StonesDir,stoneChips)
				& forcan.load_masked_images(chessDir,chessChips)
				& forcan.load_masked_images(Dir,otherChips);
		if(imagesLoaded)
		{
			Image.registerImages(stoneChips);
			Image.registerImages(chessChips);
			Image.registerImages(otherChips);
		}
		}
	}   
	
	public double getChipRotation(exCanvas canvas)
	{	int chips = exCanvas.getAltChipset(canvas); // in rare circumstances, canvas is null
		double rotation = 0;
		// alternate chipsets for chess, 1= white upside down 2=black upside down
		// this coding comes from getAltChipSet()
		switch(chips)
		{
		default:
				break;
		case 1:	if(colorId==ColorId.White) { rotation=Math.PI; }
				break;
		case 2: if(colorId==ColorId.Black) { rotation=Math.PI; }
				break;
		}
		return(rotation);
	}
	//
	// alternate chipsets for playtable.  The normal presentation is ok for side by side play
	// slightly disconcerting for face to face play.  This supports two alternates, one
	// with white pieces inverted, one with pieces facing left and right
	//
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{	
		drawRotatedChip(gc,canvas,getChipRotation(canvas),SQUARESIZE,xscale,cx,cy,label);
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
