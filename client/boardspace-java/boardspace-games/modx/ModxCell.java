package modx;

import lib.Random;

import lib.OStack;
import online.game.stackCell;

class CellStack extends OStack<ModxCell>
{
	public ModxCell[] newComponentArray(int n) { return(new ModxCell[n]); }
}
public class ModxCell extends stackCell<ModxCell,ModxChip> implements ModxConstants
{
	int sweep_counter;
	
	public ModxChip[] newComponentArray(int n) { return(new ModxChip[n]); }
	// constructor
	public ModxCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = ModxId.BoardLocation;
	}
	
	public ModxCell(ModxCell from)
	{
		super();
		copyAllFrom(from);
	}
	/** upcast the cell id to our local type */
	public ModxId rackLocation() { return((ModxId)rackLocation); }
	public ModxCell(Random r) { super(r); }
	public ModxCell(Random r,ModxId id) { super(r,id); }
	public boolean isEmpty() { return(chipIndex<0); }

	boolean hasTwo(ModxChip forchip,int direction,ModxCell filled)
	   {	ModxCell left = exitTo(direction);
	   		return (forchip.matches(left,filled)
	   				&& forchip.matches(left.exitTo(direction), filled));
	   }
	
	boolean hasOne(ModxChip forchip,int direction,ModxCell filled)
	{
		return(forchip.matches(exitTo(direction),filled));
	}
	void markOne(int sweep,int direction)
	{	exitTo(direction).sweep_counter = sweep;
	}
	void markTwo(int sweep,int direction)
	{	ModxCell left = exitTo(direction);
		left.sweep_counter = sweep;
		left.exitTo(direction).sweep_counter=sweep;
	}
	
}
