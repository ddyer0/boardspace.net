package tammany;

import lib.Random;
import lib.HitPoint;
import lib.OStack;
import online.game.*;

class CellStack extends OStack<TammanyCell>
{
	public TammanyCell[] newComponentArray(int n) { return(new TammanyCell[n]); }
}
/**
 * specialized cell used for the game tammany, not for all games using a tammany board.
 * <p>
 * the game tammany needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class TammanyCell extends stackCell<TammanyCell,TammanyChip> implements TammanyConstants
{	double center_x;		// location relative to the board rectangle
	double center_y;		// y location relative to the board rectangle
	Zone zone = null;		// zone on the board 1,2 or 3
	TammanyChip boss = null;	// dominant boss (for elections)
	String label = null;
	// constructor for player singletons
	public TammanyCell(Random r,TammanyId rack,TammanyChip bossChip,char ch) 
	{ super(r,rack); 		// construct a cell not on the board
	  geometry = Geometry.Standalone;
	  boss = bossChip;
	  col = ch;
	}
	// constructor for player arrays
	public TammanyCell(Random r,TammanyId rack,TammanyChip bossChip,char ch,int idx) 
	{ super(r,rack); 		// construct a cell not on the board
	  geometry = Geometry.Standalone;
	  boss = bossChip;
	  col = ch;
	  row = idx;
	}	
	// constructor for singleton cells
	public TammanyCell(TammanyId rack,double cx,double cy) 		// construct a cell on the board
	{	super(rack);
		geometry = Geometry.Standalone;
		center_x = cx;
		center_y = cy;
	};
	
	// constructor for array cells
	public TammanyCell(TammanyId rack,int idx,double cx,double cy) 		// construct a cell on the board
	{	super(rack);
		geometry = Geometry.Standalone;
		row = idx;
		center_x = cx;
		center_y = cy;
	};
	// 
	// this fattens up the target for all the stacks on the board and player boards.
	// fatter is better for tablets being used by people with fat fingers.
	//
	public boolean pointInsideCell(HitPoint pt,
	                               int x,
	                               int y,
	                               int SQx,int SQy)
	{
		return(super.pointInsideCell(pt,x,y,(int)(SQx*1.2),(int)(SQy*1.2)));
	}
	
	public boolean containsChip(TammanyChip ch)
	{
		for(int lim=height()-1; lim>=0; lim--)
		{
			if(chipAtIndex(lim)==ch) { return(true); }
		}
		return(false);
	}
	
	// return a boss if there is only one other boss, ie; someone that slander will win against.
	public TammanyChip otherBossSingle(TammanyChip ch)
	{
		int n=0;
		TammanyChip other = null;
		for(int lim=height()-1; lim>=0; lim--)
		{
			TammanyChip b = chipAtIndex(lim);
			if(b!=ch && b.isBoss()) 
				{ n++; other = b; 
				}
		}
		return(n==1 ? other : null);
	}
	// constructor for ward cells
	public TammanyCell(TammanyId rack,int ro,Zone z,double cx,double cy) 		// construct an array cell on the board
	{	super(rack);
		row = ro;
		zone = z;
		geometry = Geometry.Network;
		center_x = cx;
		center_y = cy;
	};
	
	/** upcast racklocation to our local type */
	public TammanyId rackLocation() { return((TammanyId)rackLocation); }

	public static boolean sameCell(TammanyCell f[],TammanyCell g[])
	{	if(f.length!=g.length) { return(false); }
		for(int i=0;i<f.length;i++) { if (!f[i].sameCell(g[i])) { return(false); }}
		return(true);
	}

	static void copyFrom(TammanyCell f[],TammanyCell of[])
	{	for(int i=0;i<f.length; i++) { f[i].copyFrom(of[i]); }
	}
	/**
	 * reset back to the same state as when newly created.  This is used
	 * when reinitializing a board.
	 */
	public void reInit()
	{	super.reInit();
		label = null;
	}

	public static boolean isEmpty(TammanyCell ar[])
	{
		for(TammanyCell c : ar) { if(c.height()>0) { return(false); }}
		return(true);
	}
	

	public TammanyChip[] newComponentArray(int size) {
		return new TammanyChip[size];
	}
	public int countChips(TammanyChip ch)
	{
		int n = 0;
		for(int lim=height()-1; lim>=0; lim--)
		{
			if(chipAtIndex(lim)==ch) { n++; }
		}
		return(n);
	}
	public boolean hasOtherBosses(TammanyChip boss)
	{
		for(int lim=height()-1; lim>=0; lim--)
		{	TammanyChip ch = chipAtIndex(lim);
			if(ch.isBoss()&&(ch!=boss)) { return(true); }
		}
		return(false);
	}

	//public static void reInit(TammanyCell c[])
	//{
	//	for(TammanyCell d : c) { if(d!=null) { d.reInit(); }}
	//}
	//public static void reInit(TammanyCell c[][])
	//{	for(TammanyCell d[] : c)
	//	{	TammanyCell.reInit(d);
	//	}
	//}
	
}