package tzaar;
import lib.Random;

import online.game.*;
import lib.OStack;
class CellStack extends OStack<TzaarCell>
{
	public TzaarCell[] newComponentArray(int n) { return(new TzaarCell[n]); }
}
public class TzaarCell extends stackCell<TzaarCell,TzaarChip> implements TzaarConstants
{	public TzaarChip[] newComponentArray(int n) { return(new TzaarChip[n]); }
	// constructor
	public TzaarCell(char c,int r,Geometry geom) 
	{	super(geom,c,r);
		if(geom==Geometry.Hex)
			{ rackLocation = TzaarId.BoardLocation;
			  onBoard=true;
			}
		else { onBoard=false; }
	}
	public TzaarId rackLocation() { return((TzaarId)rackLocation); }
	public TzaarCell(Random r,char c,int ro) 
	{	super(r,Geometry.Standalone,c,ro);
	}

	// randomize a stack of chips. This could be modernized, but it would break
	// existing tzaar games in the archives.
	public void randomize(long key)
	{	Random r = new Random(key);
		for(int j=0;j<5;j++)
		{	for(int i=0;i<=chipIndex;i++)
			{	TzaarChip ch = chipStack[i];
				int alt = Random.nextInt(r,(chipIndex+1));
				chipStack[i]=chipStack[alt];
				chipStack[alt]=ch;
			}
		}	
	}


}
