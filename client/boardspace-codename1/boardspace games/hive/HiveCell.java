package hive;

import lib.Random;
import hive.HiveConstants.HiveId;
import hive.HiveConstants.PieceType;
import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<HiveCell>
{
	public HiveCell[] newComponentArray(int n) { return(new HiveCell[n]); }
}
//
// specialized cell used for the this game.
//
public class HiveCell extends stackCell<HiveCell,HivePiece>
{	public HivePiece[] newComponentArray(int n) { return(new HivePiece[n]); }
	public int sweep_counter=0;		// used checking for valid hives
	public int overland_gradient = 0;		// used in evaluation
	public int slither_gradient = 0;		// used in evaluation
	public boolean pillbug_dest = false;	// used in move generator
	// constructor for board cells
	public HiveCell(char c,int r)
	{	super(Geometry.Hex,c,r);
		rackLocation = HiveId.BoardLocation;
	}
	  
	public HiveId rackLocation() { return((HiveId)rackLocation); }
	
	public long simpleDigest()
	{
		long v = 0;
		for(int i=0;i<height();i++)
		{	v += v*i+chipAtIndex(i).Digest();
		}
		return(v);
	}
	
	public long Digest()
	{	long v = simpleDigest();
		if(onBoard)
			{
			int na = 0;
			for(int i=0;i<3;i++) 
			{ HiveCell ad = exitTo(i);
			  if(ad!=null)
			  {	long av = ad.simpleDigest();
			    if(av!=0) { v = v*(i+4)+av; na++; }
			  }
			  
			}
			if(na==0) { v^=randomv; }	// if it's an isolated cell, include the exact location
			}
		return(v);
	}
	public long Digest(Random r)
	{	return(r.nextLong()*simpleDigest());
	}
	

	// constructor for other cells
	public HiveCell(HiveId rack,char c,int r,long rv)
	{
		super(Geometry.Standalone,c,r);
		rackLocation = rack;
		onBoard = false;
		randomv = rv;
	}

	public boolean isSurrounded()
	{	for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = exitTo(lim);
		  if(c.height()==0) { return(false); }
		}
		return(true);
	}
	// return the number of adjacent cells owned by a player
	public int nOwnColorAdjacent(HiveId color)
	{	int n=0;
		for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = exitTo(lim);
		  HivePiece bug = c.topChip();
		  if((bug!=null) && (bug.color==color)) { n++; }
		}
		return(n);
	}
	// return the number of adjacent cells owned by a player
	public int nOtherColorAdjacent(HiveId color)
	{	int n=0;
		for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = exitTo(lim);
		  HivePiece bug = c.topChip();
		  if((bug!=null) && (bug.color!=color)) { n++; }
		}
		return(n);
	}
	// return the number of adjacent cells owned by a player
	public int nOccupiedAdjacent()
	{	int n=0;
		for(int lim=geometry.n-1;lim>=0;lim--)
		{ HiveCell c = exitTo(lim);
		  if(c.height()>0) 
		  	{ n++; }
		}
		return(n);
	}	

	public boolean isAdjacentToAnt()
	{
		for(int dir=0;dir<geometry.n;dir++)
		{
			HiveCell adj = exitTo(dir);
			if(adj!=null)
			{	HivePiece top = adj.topChip();
				if(top!=null && (top.type==PieceType.ANT)) { return(true); }
			}
		}
		return(false);
	}
	
	public boolean isAdjacentToDanger(HiveId pl)
	{
		for(int dir=0;dir<geometry.n;dir++)
		{
			HiveCell adj = exitTo(dir);
			int adjHeight = adj.height();
			switch(adjHeight)
			{
			case 0: break;
			case 1: HivePiece top = adj.topChip();
				if(top.color!=pl)
				{
					switch(top.type)
					{
					default: break;
					case MOSQUITO:
					case PILLBUG:
					case BEETLE: return(true);

					}
				}
				break;
			case 2: if(topChip().color!=pl) { return(true); }	// beetle or mosquito we don't own
				break;
			default:
				break;
			}
		}
		return(false);
	}
	
	public String contentsString()
	{	String msg="";
		for(int i=chipIndex;i>=0;i--)
		{	msg += chipStack[i].exactBugName();
		}
		return(msg);
	}

    public boolean isPinnedCell()
    {	if(height()>1) { return(false); }
    	if(onBoard)
		{
		int lim = geometry.n;
		int prevh = exitTo(lim-1).height(); 
		for(int i=0;i<lim;i++) 
			{
			int h = exitTo(i).height();
			if((h==0)&&(prevh==0)) 
				{ return(false); }
			prevh = h;
			}
		}
		return(true);
    }

    public boolean adjacentCell(HiveCell s,HiveCell empty)
    {
    	if(onBoard)
    	{
		for(int lim=geometry.n;lim>=0;lim--) 
			{
			HiveCell adjto = exitTo(lim);
			if( (adjto.height() > 0) &&  (adjto!=empty) && s.isAdjacentTo(adjto)){ return(true); }
			}
    	}
    	return(false);
    }
    // 
    // true if this cell is adjacent to a pillbug of either color
    // this is used to check if mosquitos have the pillbug power
    //
    boolean isAdjacentToPillbug()
    {
		for(int mdir=0,lim=geometry.n;mdir<lim;mdir++)
		{
			HiveCell madj = exitTo(mdir);
			if(HivePiece.isPillbug(madj.topChip()))
				{ return(true);		// mosquito gets pillbug power from either color
				}
		}
		return(false);
    }

}
