/* copyright notice */package chess;

import lib.Random;
import chess.ChessConstants.ChessId;
import lib.OStack;
import online.game.PlacementProvider;
import online.game.stackCell;

class CellStack extends OStack<ChessCell>
{
	public ChessCell[] newComponentArray(int n) { return(new ChessCell[n]); }
}
public class ChessCell extends stackCell<ChessCell,ChessChip> implements PlacementProvider
{	int sweep_counter = 0;

	// 
	// support for placement history
	//
	int lastPlaced = -1;
	int lastEmptied = -1;
	int lastCaptured = -1;
	ChessChip lastContents;


	public void copyFrom(ChessCell ot)
	{	super.copyFrom(ot);
		lastPlaced = ot.lastPlaced;
		lastEmptied = ot.lastEmptied;
		lastCaptured = ot.lastCaptured;
		lastContents = ot.lastContents;

	}
	public void reInit()
	{	super.reInit();
		lastPlaced = -1;
		lastEmptied = -1;
		lastCaptured = -1;
		lastContents = null;
	}
	public int getLastPlacement(boolean empty) {
		return empty ? lastEmptied : lastPlaced;
	}

	public ChessChip[] newComponentArray(int n) { return(new ChessChip[n]); }
	// constructor
	public ChessCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = ChessId.BoardLocation;
	}
	public ChessCell(ChessId rack,int r) 
	{ super(Geometry.Standalone,rack);
	  row = r;
	}
	public ChessCell(ChessId rack) { super(Geometry.Standalone,rack); }
	
	/** upcast the cell id to our local type */
	public ChessId rackLocation() { return((ChessId)rackLocation); }
	
	/** this differs from the default method because the base level chip is a tile, which we ignore */
	public boolean isEmpty() { return(chipIndex<=(onBoard?0:-1)); }
	public ChessCell(Random r) { super(r); }
	
	public int drawStackTickSize(int sz) { return(0); }
	
	public ChessChip topChip() { return((!onBoard || (height()>1)) ? super.topChip() : null); } 

}
