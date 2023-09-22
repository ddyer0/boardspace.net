/* copyright notice */package dipole;
import lib.Random;


import online.game.stackCell;

public class DipoleCell extends stackCell<DipoleCell,DipoleChip> implements DipoleConstants
{	
	public DipoleChip[] newComponentArray(int n) { return(new DipoleChip[n]);}
	public boolean isDark = false;
	// constructor
	public DipoleCell(char c,int r) 
	{	super(Geometry.Oct,c,r);
		rackLocation = DipoleId.BoardLocation;
	}
	public DipoleId rackLocation() { return((DipoleId)rackLocation); }
	public DipoleCell(Random r,DipoleId rack,int ro) { super(r,rack); col='@'; row=ro; }
	public int stackBaseLevel() { return(1); }


	static boolean sameCell(DipoleCell c,DipoleCell d) { return((c==null)?(d==null):c.sameCell(d)); }

}
