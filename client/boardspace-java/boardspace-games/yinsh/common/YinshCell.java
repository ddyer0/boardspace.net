package yinsh.common;

import lib.Drawable;
import lib.OStack;
import online.game.PlacementProvider;
import online.game.ccell;

class CellStack extends OStack<YinshCell>
{
	public YinshCell[] newComponentArray(int sz) {
		return(new YinshCell[sz]);
	}
	
}
public class YinshCell extends ccell<YinshCell> implements PlacementProvider
{	
	int lastPlaced = -1;
	int lastEmptied = -1;
	int lastRing = 0;
	public void reInit() 
	{
		super.reInit();
		lastPlaced = -1;
		lastEmptied = -1;
		lastRing = 0;
	}
	public void copyFrom(YinshCell other)
	{	super.copyFrom(other);
		lastPlaced = other.lastPlaced;
		lastEmptied = other.lastEmptied;
		lastRing = other.lastRing;
	}
	public YinshCell(char co) { super(Geometry.Standalone,'@',0); contents = co; onBoard=false; }
	public YinshCell(char ch,int ro) { super(Geometry.Hex,ch,ro); }

public YinshChip topChip() 
{	return YinshChip.getChip(YinshChip.pieceIndex(contents));
}
	
public Drawable animationChip(int depth) 
{ return(YinshChip.getChip((depth==-2)?YinshChip.flippedPieceIndex(contents):YinshChip.pieceIndex(contents))); 
}

public int getLastPlacement(boolean empty) {
	return empty ? lastEmptied : lastPlaced;
}


}
