package quinamid;
import lib.Random;

import online.game.chipCell;


public class QuinamidCell2 extends chipCell<QuinamidCell2,QuinamidChip> implements QuinamidConstants
{	QuinamidCell2 upLink = null;	// link to the EdgeBoard paired cell
	public QuinamidChip[] newComponentArray(int n) { return(new QuinamidChip[n]); }
	// constructor
	public QuinamidCell2(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = QIds.BoardLocation;
	}
	public QuinamidCell2(Random r,QIds loc,char col,int ro)
	{	super(r,Geometry.Standalone,col,ro);
		onBoard = false;
		rackLocation = loc;
	}
	public QuinamidCell2(Random r,QIds loc) { super(r,loc); }
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
		QuinamidCell2 c = this;
		while ((c!=null) && (c.chip==forChip)) { dis++; c = c.exitTo(dir); }
		if(dis<5) { c = exitTo(dir+geometry.n/2); if((c!=null) && (c.chip==forChip)) { dis++; }}
		return(dis>=5);
	}

	public static boolean sameCell(QuinamidCell2 c,QuinamidCell2 d) { return((c==null)?(d==null):c.sameCell(d)); }
}
