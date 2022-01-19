package yinsh.common;

import lib.Drawable;
import lib.OStack;
import online.game.ccell;

class CellStack extends OStack<YinshCell>
{
	public YinshCell[] newComponentArray(int sz) {
		return(new YinshCell[sz]);
	}
	
}
public class YinshCell extends ccell<YinshCell> 
{	public YinshCell(char co) { super(Geometry.Standalone,'@',0); contents = co; onBoard=false; }
	public YinshCell(char ch,int ro) { super(Geometry.Hex,ch,ro); }

public YinshChip topChip() 
{	return YinshChip.getChip(YinshChip.pieceIndex(contents));
}
	
public Drawable animationChip(int depth) 
{ return(YinshChip.getChip((depth==-2)?YinshChip.flippedPieceIndex(contents):YinshChip.pieceIndex(contents))); 
}


}
