/* copyright notice */package tablut;

import lib.OStack;
import lib.Random;
import online.game.*;

class CellStack extends OStack<TabCell>
{
	public TabCell[] newComponentArray(int n) { return(new TabCell[n]); }
}

// specialized cell used for the game tablut hnetafl & breakthru
//
public class TabCell extends chipCell<TabCell,TabChip> implements TabConstants
{	
	int sweep_counter;		// the sweep counter for which blob is accurate
	int sweep_score;
	public boolean centerArea = false;
	public boolean centerSquare = false;
	public boolean flagArea = false;
	public boolean winForGold = false;
	public int activeAnimationHeight() { return(onBoard ? super.activeAnimationHeight() : 0); }
	public void copyFrom(TabCell c)
	{
		super.copyFrom(c);
		flagArea = c.flagArea;
		centerSquare = c.centerSquare;
		centerArea = c.centerArea;
		winForGold = c.winForGold;
	}
	// constructor for cells in the board
	public TabCell(char c,int r) 
	{	super(cell.Geometry.Oct,c,r);
		rackLocation=TabId.BoardLocation;
	};
	TabId rackLocation() { return((TabId)rackLocation); }
	// constructor for dingletons with contents
	public TabCell(Random r,TabChip chipv,TabId loc) { super(r,loc); chip=chipv;  }

}
