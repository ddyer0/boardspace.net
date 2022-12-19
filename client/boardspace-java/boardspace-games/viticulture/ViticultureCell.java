package viticulture;

import lib.Sort;
import lib.StackIterator;

import lib.Graphics;
import lib.Drawable;
import lib.G;
import lib.HitPoint;
import lib.OStack;
import online.common.exCanvas;
import online.game.*;

class CellStack extends OStack<ViticultureCell>
{
	public ViticultureCell[] newComponentArray(int n) { return(new ViticultureCell[n]); }
}
/**
 * specialized cell used for the game Viticulture
 * <p>
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class ViticultureCell extends stackCell<ViticultureCell,ViticultureChip> 
	implements ViticultureConstants
{	
	boolean selected;		// the sweep counter for which blob is accurate
	boolean fixedDisplay = false;
	ChipType contentType = ChipType.Any;
	int cost;
	ViticultureCell [] parentRow = null;
	int season = -1;
	public String toolTip = null;
	int tipOffset = 0;

	public int activeAnimationHeight()
	{
		return (fixedDisplay ? 0 : super.activeAnimationHeight()); 
	}
	public String toString()
	{
		return("<Cell "+rackLocation+" "+col+row+" "+contentsString()+">");
	}
	public void initRobotValues() 
	{
	}
	public int drawStackTickSize(int sz) { return(0); }
	public boolean labelAllChips() { 
		switch(contentType)
		{	case YellowCard:
			case BlueCard:
			case GreenCard:
			case Card:
			case PurpleCard:
			case StructureCard:
				return(true); 
			default: return(false);
		}
		}	// we use labels to dignal drawing the back
	public ViticultureCell() { super(); }
	public ViticultureCell(ViticultureId id) { super(id); }			// cell not on the board
	public ViticultureCell(ViticultureId id,ChipType con) { super(id); contentType=con; }			// cell not on the board

	public ViticultureCell(ViticultureId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Standalone,rack,c,r);
	};
	
	public static int UICode(ViticultureId rack,char col,int row)
	{
		int v = row;
		v = v*'Z'+col;
		v = v*ViticultureId.values().length;
		v = v+rack.ordinal();
		return(v);
	}
	public int uiCode()
	{	return(UICode(rackLocation(),col,row));
	}
	// cell in an array
	public ViticultureCell(ViticultureId id,int row)	// construct a cell not on the board
	{
		super(cell.Geometry.Standalone,id,'@',row);
		onBoard = false;
	}
	/** upcast racklocation to our local type */
	public ViticultureId rackLocation() { return((ViticultureId)rackLocation); }
	public boolean isSelectable()
	{
		switch(rackLocation())
		{
		case Choice_0: return(false);
		case Choice_1: return(false);
		default: return(true);
		}
	}

	public boolean isSelected()
	{
		return(selected);
	}
	/** sameCell is called at various times as a consistency check
	 * 
	 * @param other
	 * @return true if this cell is in the same location as other (but presumably on a different board)
	 */
	public boolean sameCell(ViticultureCell other)
	{	return(super.sameCell(other)
				// check the values of any variables that define "sameness"
				// && (moveClaimed==other.moveClaimed)
			); 
	}
	/** copyFrom is called when cloning boards
	 * 
	 */
	public void copyFrom(ViticultureCell ot)
	{	
		// copy any variables that need copying
		super.copyFrom(ot);
	}
	public void addChip(ViticultureChip ch)
	{	switch(rackLocation())
		{			
		case RedWine:
		case WhiteWine:
		case RedGrape:
		case WhiteGrape:
		case RoseWine:
		case Champaign:
			G.Assert(height()==0,"can't stack on %s",ch);
			break;
		default: break;
		}
		super.addChip(ch);
	}


	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public ViticultureCell(ViticultureChip cont)
	{	super();
		addChip(cont);
		onBoard=false;
	}

	
	public ViticultureChip[] newComponentArray(int size) {
		return(new ViticultureChip[size]);
	}
	
	// support for StackIterator interface
	public StackIterator<ViticultureCell> push(ViticultureCell item) {
		CellStack se = new CellStack();
		se.push(this);
		se.push(item);
		return(se);
	}
	
	/**
	 * for stacks, point at individual items by tracking the edges, not the centers
	 */
	public boolean pointAtCenters(double xp,double yp) { return(false); }
	
	// this is a bit tricky.  The usual pointInsideCell uses fuzzy circles around x,y
	// which works find in most cases, but when pointing to particular cards in a stack
	// of cards, it makes it nonintuitive to point to a particular card.  This changes
	// the logic for stacks, so when pointing at cards (in uproot mode for example) you
	// get the exact card.
	public boolean pointInsideCell(HitPoint pt,int x,int y,int SQW,int SQH)
	{
		if(height()>1)
		{
			return G.pointInRect(pt, x-SQW/2,y-SQH/2,SQW,SQH);
		}
		else
		{
			return super.pointInsideCell(pt, x, y, SQW, SQH);
		}
	}

	public boolean drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,HitPoint highlight,int SQUARESIZE,int e_x,int e_y,String thislabel)
	{ if(piece!=null && ViticultureChip.BACK.equals(thislabel))
		{
	     return(super.drawChip(gc,
	    		 ((ViticultureChip) piece).cardBack,drawOn,highlight,SQUARESIZE,1.0,e_x,e_y,null));
		}
	else
		{
	     return(super.drawChip(gc,
	    		  piece,drawOn,highlight,SQUARESIZE,1.0,e_x,e_y,thislabel));
		}
	}

	// animate the backs of cards
	public Drawable animationChip(int depth) 
	{ 	int h = height();
		ViticultureChip top = (depth<h) ? chipAtIndex(h-depth-1) : topChip();
		return(top!=null && top.cardBack!=null ? top.cardBack : top);
	}
	public void sortWorkers()
	{	Sort.sort(chipStack,0,height()-1,false);
	}
	
	public long CardDigest1() 
	{ if(randomv==0) 
		{ 
		return(rackLocation.name().hashCode()+col*200+row+1); 
		}
		return(randomv); 
	}
	/**
	 * generate a digest of the stack.  This version considers the order of the 
	 * items to be significant.  This version without a specific random context
	 * should be used only for the "default" digest of an item in it's usual context.
	 * 
	 * This is a copy of the old no-argument Digest(), preserved for backward compatibility
	 */
	public long CardDigest() 
	{ long val0=CardDigest1();
	  long val = val0;
	  for(int i=0;i<=chipIndex;i++) 
	  	{ 
	  	  val += chipStack[i].Digest()*val0*(i+1);
	  	}
	  return(val);
	}
	public void addFixedChip(ViticultureChip victoryPoint_1) {
		fixedDisplay = true;
		addChip(victoryPoint_1);
	}


}