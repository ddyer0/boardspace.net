package syzygy;

import lib.Random;

import lib.G;
import lib.OStack;
import online.game.*;
import syzygy.SyzygyConstants.SyzId;
class CellStack extends OStack<SyzygyCell>
{
	public SyzygyCell[] newComponentArray(int n) { return(new SyzygyCell[n]); }
}
//
// specialized cell used for the game hex, not for all games using a hex board.
//
// the game hex needs only a char to indicate the contents of the board.  Other
// games commonly add a more complex structue.   Games with square geometry
// instead of hex can use Geometry.Oct instead of Hex_Geometry
//
public class SyzygyCell extends chipCell<SyzygyCell,SyzygyChip> 
{	public SyzygyChip[] newComponentArray(int n) { return(new SyzygyChip[n]); }
	public SyzygyCell nextPlaced;	// next placed chip on the board
	public int sweep_counter=0;
	public SyzygyCell(char c,int r,SyzId rack) 		// construct a cell on the board
	{	super(cell.Geometry.Hex,c,r);
		rackLocation = rack;
	};
	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public SyzygyCell(Random r,SyzId loc,int ro)
	{	super();
		rackLocation = loc;
		row = ro;
		onBoard=false;
	}
	public SyzId rackLocation() { return((SyzId)rackLocation); }

	public void reInit()
	{	super.reInit();
		sweep_counter = 0;
		nextPlaced = null;
	}

	
	// return the approxumate distance squared btween this and other
	// this==other = 1
	// this adjacent to other = 2
	// this method doesn't compensate for oddness in the coordinate system which make the number of cell-steps
	// that you would have to take for larger distances not correspond to the numerical distance.
	public int disSQ(SyzygyCell other)
	{	if(this==other) { return(1); }
		for(int dir=0;dir<geometry.n;dir++) 
			{ if(exitTo(dir)==other) { return(2); }
			}
		int dx = row-other.row;
		int dy = col-other.col;
		return(dx*dx+dy*dy+2);
	}

	static public boolean sameCell(SyzygyCell c,SyzygyCell d)
	{	return((c==null)?(d==null):c.sameCell(d));
	}

	int numberOccupiedAdjacent(SyzygyCell center)
	{	int n=0;
		for(int i=0;i<geometry.n;i++)
		{	SyzygyCell c = exitTo(i);
			if((c.topChip()!=null)&&c.adjacentCell(center)) { n++; }
		}
		return(n);
	}
	
	// return true if this chip is not free to move.
    public boolean isPinnedCell()
    {	if(onBoard)
		{
		int len = nAdjacentCells();
		SyzygyChip prev = (exitTo(len-1)).topChip(); 
		for(int i=0;i<len;i++) 
			{
			SyzygyChip adj = exitTo(i).topChip();
			if((adj==null)&&(prev==null)) 
				{ return(false); }
			prev = adj;
			}
		}
		return(true);
    }
 SyzygyCell isGatedCell_v0(SyzygyCell empty,SyzygyCell anchor)
 {	SyzygyCell prev = exitTo(-1);
 	SyzygyChip prevTop = prev.topChip();
 	SyzygyCell cur = exitTo(0);
 	SyzygyChip curTop = cur.topChip();
 	for(int i=1;i<=geometry.n;i++)
 	{	SyzygyCell next = exitTo(i);
 		SyzygyChip nexTop = next.topChip();
 		if((next!=empty) && (prev!=empty) && (nexTop!=null)&&(prevTop!=null)) 
 			{ SyzygyCell val = (next==anchor)?prev:next;
 			  return(val);
 			}
 		prev = cur;
 		prevTop = curTop;
 		cur = next;
 		curTop = nexTop;
 	}
 	return(null);
 }
 
 SyzygyCell isGatedCell_v1(SyzygyCell empty,SyzygyCell anchor)
 {	SyzygyCell prev = exitTo(-1);
 	SyzygyChip prevTop = prev.topChip();
 	SyzygyCell cur = exitTo(0);
 	SyzygyChip curTop = cur.topChip();
 	for(int i=1;i<=geometry.n;i++)
 	{	SyzygyCell next = exitTo(i);
 		SyzygyChip nexTop = next.topChip();
 		if((next!=empty) 
 			&& (prev!=empty)
 			&& (nexTop!=null)
 			&&(prevTop!=null)
 			&&(curTop==null)) 
 			{ SyzygyCell val = (next==anchor)?prev:next;
 			  return(val);
 			}
 		prev = cur;
 		prevTop = curTop;
 		cur = next;
 		curTop = nexTop;
 	}
 	return(null);
 }
 
 SyzygyCell isGatedCell_v2(SyzygyCell empty,SyzygyCell anchor)
 {	SyzygyCell prev = exitTo(-1);
 	SyzygyChip prevTop = prev.topChip();
 	SyzygyCell cur = exitTo(0);
 	SyzygyChip curTop = cur.topChip();
 	for(int i=1;i<=geometry.n;i++)
 	{	SyzygyCell next = exitTo(i);
 		SyzygyChip nexTop = next.topChip();
 		if((next!=empty) 
 			&& (prev!=empty)
 			&& (nexTop!=null)
 			&&(prevTop!=null)
 			&&(curTop==null)) 
 			{ if(next==anchor)
 				{	SyzygyCell val = null;
 					i --;
 					while(prevTop!=null)
 					{	i--;
 						val = prev;
 						prev = exitTo(i);
 						prevTop = prev.topChip();
 					}
 					return(val);
 				}
 				else 
 				{	SyzygyCell val = next;
					while(nexTop!=null)
					{	i++;
						val = next;
						next = exitTo(i);
						nexTop = next.topChip();
					}
 					return val;
 				}
 			  
 			}
 		prev = cur;
 		prevTop = curTop;
 		cur = next;
 		curTop = nexTop;
 	}
 	return(null);
 }
 
 public boolean isEmptyOr(SyzygyCell empty)
 {
	 return((this==empty)||(topChip()==null));
 }
 boolean topChipFilled(SyzygyCell filled)
 {	if(this==filled) { return(true); }
 	return(topChip()!=null);
 }
// pick a new anchor, which will be adjacent to this cell, and
// as far away as possible from the old anchor.  Normally we 
// will be adjacent, but in cases where the shape is concave,
// we may be oppsite.   Caution for wrap around!  Use directions
// rather than coordinates
public SyzygyCell pickNewAnchor(SyzygyCell oldAnchor,SyzygyCell filled)
{	int anchorDirection =0;
	SyzygyCell newAnchor = null;
	while(anchorDirection<nAdjacentCells())
		{ if(exitTo(anchorDirection)==oldAnchor) { newAnchor = oldAnchor; break;} 
		  anchorDirection++;
		}
	G.Assert(newAnchor!=null,"didn't find the anchor adjacent");
	// either the clockwise or counterclockwise direction from the old anchor must
	// be empty, or possibly both are empty
	SyzygyCell cw = exitTo(anchorDirection+1);
	SyzygyCell ccw = exitTo(anchorDirection-1);
	if(cw.topChipFilled(filled))
	{	//G.Assert(ccw.topChip()==null,"should be empty");
		int step = anchorDirection+2;
		do { if(cw!=filled) { newAnchor = cw; }
			 cw = exitTo(step++);
		} while(cw.topChipFilled(filled));
	}
	if(ccw.topChipFilled(filled))
	{
		int step = anchorDirection-2;
		do { if(ccw!=filled) { newAnchor = ccw; }
			 ccw = exitTo(step--);
		} while(ccw.topChipFilled(filled));
	}
	return(newAnchor);
}
public boolean adjacentCell(SyzygyCell s)
{	if(s==null) { return(true); }
    	if(onBoard)
    	{
		int len = nAdjacentCells();
		for(int i=0;i<len;i++) 
			{
			SyzygyCell adjto = exitTo(i);
			if(s==adjto){ return(true); }
			}
    	}
    	return(false);
    }   
	// return a bitmask indicating the set of legal move distances.
	int slitherDistanceMask()
	{	int mask = 0;
		{	for(int dir=0;dir<geometry.n; dir++)
			{	SyzygyCell other = exitTo(dir);
				if(other!=null)
				{	SyzygyChip top = other.topChip();
					if((top!=null)&&(top.value>0)) { mask |= (1<<top.value); }
				}
			}
		}
		return(mask);
	}
}
