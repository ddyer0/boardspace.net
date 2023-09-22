/* copyright notice */package rithmomachy;

import lib.*;
import online.game.stackCell;

class CellStack extends OStack<RithmomachyCell>
{
	public RithmomachyCell[] newComponentArray(int n) { return(new RithmomachyCell[n]); }
}
public class RithmomachyCell extends stackCell<RithmomachyCell,RithmomachyChip> implements RithmomachyConstants
{
	public RithmomachyChip[] newComponentArray(int n) { return(new RithmomachyChip[n]); }
	// constructor
	public RithmomachyCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = RithId.BoardLocation;
	}
	public RithmomachyCell(Random r) { super(r); }
	
	public RithId rackLocation() { return((RithId)rackLocation); }

	public void moveStack(RithmomachyCell ot)
	{	for(int i=0;i<ot.height();i++) { addChip(ot.chipAtIndex(i)); }
		ot.reInit();
	}
	public int getDistanceMask()
	{	int mask = 0;
		for(int i=0;i<=chipIndex;i++)
		{	RithmomachyChip ch = chipAtIndex(i);
			mask |= (1 << ch.moveDistance());
		}
		return(mask);
	}
	public boolean isCapturedBySiege(int initial_direction)
	{	int myColor = topChip().colorIndex();
		for(int direction = initial_direction; direction<geometry.n; direction+=2)
		{	RithmomachyCell d = exitTo(direction);
			if(d!=null)
			{	RithmomachyChip top = d.topChip();
				if((top==null) || (top.colorIndex()==myColor)) { return(false); }
			}
		}
		// surrounded on 4 sides by enemy or off-board cells
		return(true);
	}
	
	// add ch, and filter down until the chips are in order of value
	// this is not a full sort, but enough to keep the pyramids in 
	// their canonical stack order
	public void insertInOrder(RithmomachyChip ch)
	{	addChip(ch);
		int value = ch.value;
		int ind = chipIndex-1;
		while(ind>=0 && chipStack[ind].value<value) 
		{	chipStack[ind+1] = chipStack[ind];
			chipStack[ind]=ch;
			ind--;
		}
	}
	public boolean isCapturedBySiege()
	{	return(isCapturedBySiege(0) || isCapturedBySiege(1));
	}
	public boolean canBeCapturedByEncounter(int value)
	{	int index = chipIndex;
		if(index>0) { if(stackValue()==value) { return(true); }}
		while(index>=0) { if(chipStack[index--].value==value) { return(true); }}
		return(false);
	}
	public RithmomachyChip chipWithValue(int value)
	{	int index = chipIndex;
		while(index>=0) 
			{ RithmomachyChip ch = chipStack[index--];
			  if(ch.value==value) { return(ch); }
			}
		return(null);
	}
	public int stackValue()
	{
		int v = 0;
		for(int i=chipIndex; i>=0; i--) { v += chipStack[i].value; }
		return(v);
	}
	public RithmomachyChip removeChipNumber(int n)
	{
		for(int i=0;i<height();i++)
		{
			RithmomachyChip ch = chipAtIndex(i);
			if(ch.chipNumber()==n) { return(removeChipAtIndex(i)); }
		}
		throw G.Error("chip number %d not found",n);
	}
	public int subsetValue(int sub)
	{	int v = 0;
		int i=0;
		while((sub != 0) && (i<height()))
		{	if((sub&1)!=0) {  v += chipAtIndex(i).value; }
			i++;
			sub = sub>>1;
		}
		return(v);
	}
	
	public boolean canAttackAsPyramid()  { return(height()>1); }
	public boolean canCaptureByEncounter()
	{	for(int i=0; i<=chipIndex;i++)
		{	RithmomachyChip ch = chipAtIndex(i);
			if(ch.canCaptureByEncounter()) { return(true); }
		}
		return(false);
	}

	public void sort()
	{	Sort.sort(chipStack,0,chipIndex);
	}

}
