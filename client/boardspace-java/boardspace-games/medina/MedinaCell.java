/* copyright notice */package medina;


import lib.Random;
import medina.MedinaConstants.MedinaId;
import lib.G;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<MedinaCell>
{
	public MedinaCell[] newComponentArray(int n) { return(new MedinaCell[n]); }
}
public class MedinaCell extends stackCell<MedinaCell,MedinaChip> 
{	public MedinaChip[]newComponentArray(int n) { return(new MedinaChip[n]); }
	public int sweep_counter=0;
	public Cluster cluster;			// cluster including this cell
	
	public int moveWalled = 0;		// for wall-claiming moves
	public int movePlaced = 0;		// for claimed clusters, when the claim came.
	public MedinaChip storageFor = null;
	public void reInit()
	{	super.reInit();
		cluster = null;
		storageFor = null;
		sweep_counter = 0;
		movePlaced = 0;
		moveWalled = 0;
	}
	public boolean sameCell(MedinaCell other)
	{	return(super.sameCell(other)
				&& (moveWalled==other.moveWalled)
				&& (movePlaced==other.movePlaced)
			); 
	}
	public int towerNumber() 
	{	G.Assert(topChip().isTower(),"is a tower");
		if(col=='A') 
			{ if(row>1) { return(1); } 
			  if(row==1) { return(4); }
			}
		else if(col>'A')
		{ if(row>1) { return(3); }
		  if(row==1) { return(2); }
		}
		throw G.Error("shoulnd't get here");
	}
	public void copyFrom(MedinaCell ot)
	{	movePlaced = ot.movePlaced;
		moveWalled = ot.moveWalled;
		super.copyFrom(ot);
	}
	// constructor
	public MedinaCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = MedinaId.BoardLocation;
	}
	public MedinaCell(Random r) { super(r); }
	// create a cell not on the board, owned by some player
	public MedinaCell(Random r,MedinaId rack,int player,int ro,MedinaChip sto) 
		{ super(r);
		  col = (char)('A'+player);
		  row = ro;
		  onBoard=false;
		  rackLocation = rack;
		  storageFor = sto;
		}	
	public MedinaId rackLocation() { return((MedinaId)rackLocation); }

}
