/* copyright notice */package qyshinsu;
import lib.Random;

import lib.OStack;
import online.game.*;
class CellStack extends OStack<QyshinsuCell>
{
	public QyshinsuCell[] newComponentArray(int n) { return(new QyshinsuCell[n]); }
}
public class QyshinsuCell extends stackCell<QyshinsuCell,QyshinsuChip> implements QyshinsuConstants
{	public QyshinsuChip[] newComponentArray(int n) { return(new QyshinsuChip[n]); }
	public QyshinsuCell(char column,int rown)
	{	super(Geometry.Oct,column,rown);
		rackLocation = QIds.BoardLocation;
	}
	public QyshinsuCell(Random r,char column,int rown)
	{	super(r,Geometry.Oct,column,rown);
	}
	public QIds rackLocation() { return((QIds)rackLocation); }
}
