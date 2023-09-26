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
package proteus;

import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;


class ChipStack extends OStack<ProteusChip>
{
	public ProteusChip[] newComponentArray(int n) { return(new ProteusChip[n]); }
}
/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too.
 * 
 */
public class ProteusChip extends chip<ProteusChip> implements ProteusConstants
{	
	private static Random r = new Random(343535);	// this gives each chip a unique random value for Digest()
	private static ChipStack allChips = new ChipStack();
	private static boolean imagesLoaded = false;

	Shape shape=null;
	PieceColor pieceColor = null;
	Goal goal = null;
	Trade trade = null;
	Move move = null;
	public String getDesc() 
		{ if(goal!=null) { return(goal.desc); }
		  if(trade!=null) { return(trade.desc); }
		  if(move!=null) { return(move.desc); }
		  return("");
		}
	private int chipIndex;
	
	public boolean isTile() 
	{	if(pieceColor!=null)
		{switch(pieceColor)
		{
		case Player_White:
		case Player_Black: return(false);
		default: return(true);
		}}
		return(false);
	}
	public int chipNumber() { return(chipIndex); }
	public static ProteusChip getChipNumber(int id)
	{	return(allChips.elementAt(id));
	}

	// constructor for chips not expected to be part of the UI
	private ProteusChip(String na,double scl[])
	{	file = na;
		chipIndex=allChips.size();
		randomv = r.nextLong();
		scale = scl;
		allChips.push(this);
	}
	// constructor for chips expected to be part of the UI
	private ProteusChip(String na,double scl[],Shape sh,PieceColor ac)
	{	this(na,scl);
		shape = sh;
		pieceColor = ac;
	}
	private ProteusChip(String na,double scl[],Shape sh,PieceColor ac,Goal go)
	{	this(na,scl,sh,ac);
		goal = go;
	}
	private ProteusChip(String na,double scl[],Shape sh,PieceColor ac,Trade tr)
	{	this(na,scl,sh,ac);
		trade = tr;
	}
	private ProteusChip(String na,double scl[],Shape sh,PieceColor ac,Move mo)
	{	this(na,scl,sh,ac);
		move = mo;
	}


	public String toString()
	{	return("<"+ ((shape==null)?"":" "+shape)+((pieceColor==null)?"":" "+pieceColor)+" #"+chipIndex+">");
	}
	public String contentsString() 
	{ return(toString()); 
	}
	
	public static ProteusChip board = new ProteusChip("board",new double[]{0.5,0.5,1.0});
	public static ProteusChip backgroundTile = new ProteusChip("background-tile-nomask",new double[]{0.5,0.5,1.0});
	public static ProteusChip backgroundReviewTile = new ProteusChip("background-review-tile-nomask",new double[]{0.5,0.5,1.0});
	
	public static ProteusChip redCircle = new ProteusChip("red-oval",new double[]{0.54,0.515,0.578},
				Shape.circle,PieceColor.Move_Red,Move.king);
	public static ProteusChip redSquare = new ProteusChip("red-square",new double[]{0.54,0.515,0.578},
			Shape.square,PieceColor.Move_Red,Move.rook);
	public static ProteusChip redTriangle = new ProteusChip("red-triangle",new double[]{0.54,0.515,0.578},
			Shape.triangle,PieceColor.Move_Red,Move.bishop);
	
	public static ProteusChip yellowCircle = new ProteusChip("yellow-oval",new double[]{0.54,0.515,0.578},
				Shape.circle,PieceColor.Goal_Yellow,Goal.three);
	public static ProteusChip yellowSquare = new ProteusChip("yellow-square",new double[]{0.54,0.515,0.578},
			Shape.square,PieceColor.Goal_Yellow,Goal.color);
	public static ProteusChip yellowTriangle = new ProteusChip("yellow-triangle",new double[]{0.54,0.515,0.578},
			Shape.triangle,PieceColor.Goal_Yellow,Goal.shape);
	
	
	public static ProteusChip blueCircle = new ProteusChip("blue-oval",new double[]{0.54,0.515,0.578},
				Shape.circle,PieceColor.Trade_Blue,Trade.polarity);
	public static ProteusChip blueSquare = new ProteusChip("blue-square",new double[]{0.54,0.515,0.578},
				Shape.square,PieceColor.Trade_Blue,Trade.color);

	public static ProteusChip blueTriangle = new ProteusChip("blue-triangle",new double[]{0.54,0.515,0.578},
				Shape.triangle,PieceColor.Trade_Blue,Trade.shape);

	public static ProteusChip blackCircle = new ProteusChip("black-circle",new double[]{0.6,0.5,0.487},
				Shape.circle,PieceColor.Player_Black);
	public static ProteusChip blackTriangle = new ProteusChip("black-triangle",new double[]{0.6,0.5,0.487},
				Shape.triangle,PieceColor.Player_Black);
	public static ProteusChip blackSquare = new ProteusChip("black-square",new double[]{0.6,0.5,0.487},
				Shape.square,PieceColor.Player_Black);

	public static ProteusChip whiteCircle = new ProteusChip("white-circle",new double[]{0.6,0.5,0.484},
				Shape.circle,PieceColor.Player_White);
	public static ProteusChip whiteTriangle = new ProteusChip("white-triangle",new double[]{0.6,0.5,0.484},
				Shape.triangle,PieceColor.Player_White);
	public static ProteusChip whiteSquare = new ProteusChip("white-square",new double[]{0.6,0.5,0.484},
				Shape.square,PieceColor.Player_White);

	public static ProteusChip Icon = new ProteusChip("proteus-icon-nomask",new double[]{0.6,0.5,0.484},
			Shape.square,PieceColor.Player_White);

	public static ProteusChip MainChips[] = {
		redCircle,redSquare,blueTriangle,
		yellowCircle,yellowSquare,redTriangle,
		blueCircle,blueSquare,yellowTriangle,
	};
	public static ProteusChip BlackChips[] = { blackCircle,blackTriangle,blackSquare};
	public static ProteusChip WhiteChips[] = { whiteCircle,whiteTriangle,whiteSquare};
	public static ProteusChip PlayerChips[][] = { BlackChips,WhiteChips };
	
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = forcan.load_masked_images(ImageDir,allChips);
		}
	}


}
