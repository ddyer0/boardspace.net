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
package exxit;

import lib.Random;
import lib.AR;
import lib.G;
import lib.OStack;
import online.game.cell;
import online.game.chip;

class CellStack extends OStack<ExxitCell>
{
	public ExxitCell[] newComponentArray(int n) { return(new ExxitCell[n]); }
}
/**
 * 
 * @author ddyer
 * this isn't a fully standard implementation of a stack cell, 
 * it would be built on "stackCell" to do that.  This is just
 * enough cell ancestry to allow animations to work.
 *
 */
//
// specialized cell used for the game exxit, strongly 
// related to those for hive, gobblet and hex
public class ExxitCell extends cell<ExxitCell> implements ExxitConstants
{	
	private ExxitPiece pieces[]=null;	// the pieces stacked on this cell
	private int pieceIndex=-1;			// index into pieces[]
	public int sweep_counter=0;			// used checking for valid boards
	int blobNumber=0;					//the blob number containing this cell
	String cellName="";					//the history name for this cell
	
	public void reInit()
	{
		super.reInit();
		AR.setValue(pieces,null);
		sweep_counter = 0;
		pieceIndex = -1;
		blobNumber = 0;
		cellName ="";
	}
	// short term storage, don't set piece location
	// this is used in exchange moves
	public void push(ExxitPiece p)
	{	pieces[++pieceIndex] = p;
	}
	// inverse of push.  Only use this for the undoCell piece
	public ExxitPiece pop()
	{	if(pieceIndex>=0) {	return(pieces[pieceIndex--]); }
		return(null);
	}
	public int height() { return(pieceIndex+1); }
	
	// constructor
	public ExxitCell(char c,int r)
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = ExxitId.BoardLocation;
		pieces = new ExxitPiece[ExxitGameBoard.MAX_STACK_HEIGHT];
	}
	// constructor
	public ExxitCell(char c,int r,int height)
	{	super(cell.Geometry.Hex,c,r);
		pieces = new ExxitPiece[height];
	}
	public ExxitId rackLocation() { return((ExxitId)rackLocation); }
	public void doInit()
	{	pieceIndex=-1;
		for(int i=0;i<pieces.length;i++) { pieces[i]=null; }
		sweep_counter=0;
		cellName = "";
	}
	// true if this cell contains a tile and nothing else
	public boolean isEmptyTile()
	{	ExxitPiece p = topPiece();
		return((p!=null) && (p.typecode==TILE_TYPE)); 
	}
	// true if this cell has a tile on the bottom
	public boolean isTileOnBoard()
	{
		if(pieceIndex>=0)
		{	ExxitPiece p = pieceAtIndex(0);
			return(p.typecode==TILE_TYPE);
		}
		return(false);
	}
	
	// clone the dynamic state of the cell from a cell on another board
	void copyFrom(ExxitCell other,ExxitPiece allPieces[],ExxitPiece allTiles[])
	{	super.copyFrom(other);
		pieceIndex = other.pieceIndex;
		cellName = other.cellName;
		for(int i=0;i<pieces.length;i++)
		{	ExxitPiece o = (i<=pieceIndex)?other.pieces[i]:null;
			if(o!=null) 
			{ o = (o.typecode==TILE_TYPE)?allTiles[o.seq]:allPieces[o.seq]; 
			  o.location = this; 
			}
			pieces[i]=o;
		}
	}
	public void copyAllFrom(ExxitCell c)
	{ throw G.Error("shouldn't be called");
	}
	public void copyFrom(ExxitCell c)
	{ throw G.Error("shouldn't be called");
	}
	public boolean sameContents(ExxitCell other)
	{
		return(samePieces(other));
	}

	public static boolean sameCell(ExxitCell c,ExxitCell d)
	{
		return((c==null)?(d==null):c.sameCell(d));
	}
	// a cell can exchange if it doesn't have a tile on the bottom, 
	// and if it is adjacent to two cells which do have tiles on the bottom
	public boolean canExchange()
	{	int nTiles = 0;
		if(onBoard && (pieceIndex>=0))
		{ ExxitPiece p = pieceAtIndex(0);
		  if(p.typecode==CHIP_TYPE)
		  {
			  for(int dir = 0;dir<6;dir++)
			  {	ExxitCell c = exitTo(dir);
			  	if(c!=null && c.isTileOnBoard()) { nTiles++; }
			  }
		  }
		}
		return(nTiles>=2);
	}

	// true if this board cell has the same stack as the other
	boolean samePieces(ExxitCell other)
	{	for(int i=0;i<pieceIndex;i++)
		{ if((pieces[i].typecode!=other.pieces[i].typecode) 
				|| (pieces[i].seq!=other.pieces[i].seq)) { return(false); }
		}
		return(true);
	}

	// add a new bug to the cell, and set its location
	public void addPiece(ExxitPiece newbug) 
	{ 	G.Assert((pieceIndex<0)||(onBoard==false) || (newbug.typecode==CHIP_TYPE),"can move to cell");
		pieces[++pieceIndex] = newbug;
		newbug.location=this;
	}
	public chip<?> topChip() 
	{ return(topPiece());
	}

	// get the top piece or null
	public ExxitPiece topPiece() { return((pieceIndex>=0)?pieces[pieceIndex]:null); }
	// get a specific piece
	public ExxitPiece pieceAtIndex(int i)
	{	return(pieces[i]);
	}
	// remove the top piece
	public ExxitPiece removeTop()
	{	G.Assert((pieceIndex>=0),"there is no piece here");
		ExxitPiece oldc = pieces[pieceIndex--];
		oldc.location = null;
		return(oldc);
	}
	// remove the top piece, which (as a consistancy check) must be oldcup.
	public void removeTop(ExxitPiece oldCup)
	{	G.Assert(topPiece()==oldCup,"removing wrong insect");
		removeTop();
	}
	
	// generate a digest of the cell, 
	public long Digest(Random r) 
		{ return(super.Digest(r)+DigestContents());
		}
	public long DigestContents()
	{	int val = 1;
		for(int i=0;i<=pieceIndex;i++) 
		  { long pd = pieces[i].Digest();
		    val*=4;
		    val += pd;
		    //System.out.println("Pd "+pd + " = "+val);
		  }
		  return(val);
		}
	public String contentsString()
	{	String msg="";
		for(int i=pieceIndex;i>=0;i--)
		{	msg += pieces[i].prettyName;
		}
		return(msg);
	}



}
