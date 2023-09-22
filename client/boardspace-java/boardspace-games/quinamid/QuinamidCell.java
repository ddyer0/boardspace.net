/* copyright notice */package quinamid;
import lib.OStack;
import lib.Random;

import online.game.chipCell;

class CellStack extends OStack<QuinamidCell>
{
	public QuinamidCell[] newComponentArray(int n) { return(new QuinamidCell[n]); }
}

public class QuinamidCell extends chipCell<QuinamidCell,QuinamidChip> implements QuinamidConstants
{	QuinamidCell upLink = null;	// link to the EdgeBoard paired cell
	public QuinamidChip[] newComponentArray(int n) { return(new QuinamidChip[n]); }
	// constructor
	public QuinamidCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = QIds.BoardLocation;
	}
	public QuinamidCell(Random r,QIds loc,char col,int ro)
	{	super(r,Geometry.Standalone,col,ro);
		onBoard = false;
		rackLocation = loc;
	}
	public QuinamidCell(Random r,QIds loc) { super(r,loc); }
	public QIds rackLocation() { return((QIds)rackLocation); }
	public String toString()
	{ 	return("<QuinamidCell "+col+row
			+((upLink==null)
				?" hidden"
				:(" <-> "+upLink.col+upLink.row))
			+" "+contentsString()
			+" >");
	}
	
	public boolean winningLine(int dir,QuinamidChip forChip)
	{	int dis = 0;
		QuinamidCell c = this;
		while ((c!=null) && (c.chip==forChip)) { dis++; c = c.exitTo(dir); }
		if(dis<5) { c = exitTo(dir+geometry.n/2); if((c!=null) && (c.chip==forChip)) { dis++; }}
		return(dis>=5);
	}

	public static boolean sameCell(QuinamidCell c,QuinamidCell d) { return((c==null)?(d==null):c.sameCell(d)); }
}
