/* copyright notice */package raj;

import lib.Graphics;

import lib.OStack;
import lib.Random;
import online.game.cell;
import online.game.chip;
import online.game.chipCell;
import online.common.exCanvas;
import online.game.stackCell;

class CellStack extends OStack<RajCell>
{
	public RajCell[] newComponentArray(int n) { return(new RajCell[n]); }
}
/**
 * specialized cell used for the game raj, not for all games using a raj board.
 * <p>
 * the game raj needs only a single object on each cell, or empty.
 *  @see chipCell 
 *  @see stackCell
 * 
 * @author ddyer
 *
 */
public class RajCell extends stackCell<RajCell,RajChip> implements RajConstants
{	public RajChip[] newComponentArray(int n) { return new RajChip[n]; }
	boolean showCardFace = false;
	boolean showPrizeBack = false;
	RajColor color = null;			// color restriction on this cell
	int sweep_counter = 0;
	public RajCell(Random r,RajId rack) { super(r,rack); setCurrentCenter(1, 1); }		// construct a cell not on the board
	public RajCell(RajId rack,char c,int r) 		// construct a cell on the board
	{	super(cell.Geometry.Square,c,r);
		rackLocation = rack;
		setCurrentCenter( 1,1);
	};
	public void reInit() { color = null; super.reInit();sweep_counter = 0; }
	public RajCell(RajId rack) { super(rack); setCurrentCenter(1,1); }
	public RajCell(Random r,RajId rack,char co) { super(r,rack); col = co;setCurrentCenter( 1, 1); }
	public RajId rackLocation() { return((RajId)rackLocation); }
	public int totalPrizeValue()
	{	// only valid for stacks of prizes
		int val = 0;
		for(int lim=height()-1; lim>=0; lim--)
		{
			val += chipAtIndex(lim).prizeValue();
		}
		return(val);
	}

	// constructor a cell not on the board, with a chip.  Used to construct the pool chips
	public RajCell()
	{	super();
		onBoard=false;
	}

	public void drawChip(Graphics gc,exCanvas drawOn,chip<?> piece,int SQUARESIZE,double xscale,int e_x,int e_y,String thislabel)
    {	RajChip ch = (RajChip)piece;
    	RajViewer can = (RajViewer)drawOn;
    	if((ch!=null) && ch.isPrize() && showPrizeBack)
    	{	RajChip back = RajChip.getPrize(0);
    		super.drawChip(gc,drawOn, back, SQUARESIZE, xscale, e_x,e_y,thislabel);
    	}
    	else if((ch!=null) && ch.isCard() && (ch.isFront() || showCardFace || "#".equals(thislabel)))
    	{	// the label "#" marks a card we want to show the number of.
    		RajChip front = ch.getCardFront();
       		super.drawChip(gc,drawOn, front, SQUARESIZE, xscale, e_x,e_y,null);
       		ch.drawChip(gc,can.cardDeckFont,SQUARESIZE,e_x,e_y);
    		
    	}
    	else 
    	{ super.drawChip(gc, drawOn, piece, SQUARESIZE, xscale, e_x,e_y,thislabel); 
    	}
    }
    public static boolean sameCell(RajCell c,RajCell d)
    {	return((c==null)?d==null:c.sameCell(d));
    }
}