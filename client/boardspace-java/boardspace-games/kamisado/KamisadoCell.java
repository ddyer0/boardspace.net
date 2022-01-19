package kamisado;

import lib.Random;
import kamisado.KamisadoConstants.KColor;
import kamisado.KamisadoConstants.KamisadoId;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<KamisadoCell>
{
	public KamisadoCell[] newComponentArray(int n) { return(new KamisadoCell[n]); }
}
public class KamisadoCell extends stackCell<KamisadoCell,KamisadoChip>
{   private static KColor cellColors[][] =
    {
    	{KColor.Brown,KColor.Green,KColor.Red,KColor.Yellow,		// row 1 and reverse 8 
    	 KColor.Pink,KColor.Purple,KColor.Blue,KColor.Orange},	
    	{KColor.Purple,KColor.Brown,KColor.Yellow,KColor.Blue,		// row 2 and reverse 7
    	 KColor.Green,KColor.Pink,KColor.Orange,KColor.Red},
    	{KColor.Blue,KColor.Yellow,KColor.Brown,KColor.Purple,		// row 3 and reverse 6
    	 KColor.Red,KColor.Orange,KColor.Pink,KColor.Green},
    	{KColor.Yellow,KColor.Red,KColor.Green,KColor.Brown,		// row 4 and reverse 5
    	 KColor.Orange,KColor.Blue,KColor.Purple,KColor.Pink}
    };
	private KColor cellColor=null;
	public KColor getColor() { return(cellColor); }
	public KamisadoChip[] newComponentArray(int n) { return(new KamisadoChip[n]); }
	// constructor
	public KamisadoCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = KamisadoId.BoardLocation;
		int ncol = row<=4?(col-'A'):('H'-col);
		int nrow = row<=4 ? row-1 : 8-row;
		cellColor = row<=0?null:cellColors[nrow][ncol];
	}
	public KamisadoCell(Random r) { super(r); }

}
