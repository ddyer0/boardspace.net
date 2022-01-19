package online.game;

import online.game.cell.Geometry;

/**
 * support a square board with no diagonal links.  For a board with diagonal connections, 
 * @see rectBoard
 * @see trackBoard
 * @author ddyer
 *
 */
//
// square geometry board without diagonal connections
//
public abstract class squareBoard<CELLTYPE extends cell<CELLTYPE>> extends square_geo_board<CELLTYPE> {
	
	private static int sq_dxs[] = { -1, 0, 1, 0};
	private static int sq_dys[] = {  0, 1, 0, -1};
	private static String sq_direction_names[]  = {"Left","Up","Right","Down"};

	/** the direction to move visually left from this cell to the next cell */
	protected static final int CELL_LEFT = 0;
	/** the direction to move visually up from this cell to the next cell */
	protected static final int CELL_UP = 1;
	/** the direction to move visually right from this cell to the next cell */
	protected static final int CELL_RIGHT = 2;
	/** the direction to move visually down from this cell to the next cell */
	protected static final int CELL_DOWN = 3;
	/** the amount to add to make a quarter turn clockwise */
	protected static final int CELL_QUARTER_TURN = 1;
	/** the amount to add to a direction to make a half turn */
	protected static final int CELL_HALF_TURN = 2;
	/** the about to add to a direction to make a full circle */
	protected static final int CELL_FULL_TURN  = 4;
	
	public static final int CELL_DOWN() { return(CELL_DOWN); }
	public static final int CELL_RIGHT() { return(CELL_RIGHT); }
	public static final int CELL_LEFT() { return(CELL_LEFT); }
	public static final int CELL_UP() { return(CELL_UP); }
	public static final int CELL_FULL_TURN() { return(CELL_FULL_TURN); }
	public static final int CELL_HALF_TURN() { return(CELL_HALF_TURN); }
	public static final int CELL_QUARTER_TURN() { return(CELL_QUARTER_TURN); }
	
	/**
	 * construct a square geometry board with a plain rectangular shape.
	 */
	public void  initBoard(int ncol,int nrow)
	{	
		dxs = sq_dxs;
		dys = sq_dys;
		DIRECTION_NAMES = 	sq_direction_names;

		geometry = Geometry.Square;
		super.initBoard(ncol,nrow);

	}
	/**
	 * initialize a square geometry board with an irregular outline, similar to 
	 * what is common with hex geometry boards, except this works by constructing
	 * a full board then removing the unwanted cells.
	 * @param nc
	 * @param fc
	 */
    public void initBoard(int nc[],int fc[])
    {	int ncols = nc.length;
    	int nrows = 0;
    	geometry = Geometry.Square;
    	for(int i=0;i<ncols;i++) { nrows = Math.max(nrows,nc[i]+fc[i]-1); }
    	initBoard(ncols,nrows);
    	// remove excess cells
    	for(int col=0;col<ncols;col++)
    	{	int remove = fc[col]-1;
    		int skip = nc[col];
    		CELLTYPE c = getCell((char)('A'+col),1);
    		while(remove-- > 0)
    			{ CELLTYPE rem = c;
    			  c = c.exitTo(CELL_UP);
    			  removeCell(rem);
    			}
    		while(skip-- >0)
    		{	c = c.exitTo(CELL_UP);
    		}
    		while(c!=null)
    		{	CELLTYPE rem = c;
    			  c = c.exitTo(CELL_UP);
    			  removeCell(rem);
    		}
    	}
    }
}
