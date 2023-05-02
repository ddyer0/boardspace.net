package online.game;
import online.game.cell.Geometry;

/* support for hex boards which start as a rhombus of some size, then can be
 * dynamically enlarged one cell at a time.  This is intended to be used for
 * boardless hex games such as Palago and Hive 
 *
 * as a side effect of dynamically enlarging the board, the "col" field of cells
 * can wander out of the normal A-Z range, which would wreak havoc in game records,
 * so the *printed* representation of col will be 3A 2A 1A A B ... Z 1Z 2Z 3Z
 * this requires some minor adjustment in the xxMovespec class for each game.
 * also, because it's encoded as a char, the range is not really infinite, but
 * 0-255 (or maybe more) will have to do.
 * 
 * another side effect of adding cells after the fact is that the printing order 
 * based on the raster is pretty much toast.  Instead, we make a sorted version
 * of the cell array, sorted appropriately for the Itype.xxx iterator request.
 * This is cached, so care has to be taken to uncache it when the cell list 
 * changes, or the perspective/screen reverse causes the presentation to change.
 * 
 * The torus option is still available, and can be used for legacy games made with
 * torroid boards.  Flipping back and forth from torus to expandable is possible,
 * but some care has to be taken.
 * 
 * Adding new cells is not completely automatic, GetCell will create new cells
 * automatically, and you can call createAdjacentCells to expand the margins.
 * 
 * @author ddyer
 *
 */
public abstract class infiniteHexBoard<CELLTYPE extends cell<CELLTYPE>> extends infiniteBoard<CELLTYPE>
{
	public Geometry geometry() { return Geometry.Hex; }
	/*
     * the directions to the adjacent cells, clockwise from the lower-left position
     * in principal any arrangement is equivalent, but games can have subtle dependencies
     * and this is the legacy arrangement.  These directions are single steps in col,row
     * rather than the array dx,dy used in the raster-based game classes based on gBoard
     * 
     */
    static int ihex_dxs[] = {-1, -1,0,1,1,0};
    static int ihex_dys[] = {-1, 0, 1,1,0,-1};
    
    public int[] dxs() { return ihex_dxs; }
    public int[] dys() { return ihex_dys; }
    
     /** this is a magic number for hex boards */
    public double yCellRatio() { return(1.165); }
    /** this is a magic number for hex boards */
    public double yGridRatio() { return(Math.sqrt(3.0)); }
    /** for hexagonal geometry, add this to rotate to the opposite direction */
    public static final int CELL_HALF_TURN = 3;
    /** for hexagonal geometry, this is the modulus of the clock (ie; 6 steps) */
    public static final int CELL_FULL_TURN = 6;
    /** this is the direction for {@link cell#exitTo} to go visually down */
    public static final int CELL_DOWN = 0;
    /** this is the direction for {@link cell#exitTo} to go visually up */
    public static final int CELL_UP = 3;
    /** this is the direction for {@link cell#exitTo} to go visually down and left */
    public static final int CELL_DOWN_LEFT = 1;
    /** this is the direction for {@link cell#exitTo} to go visually up and left */
    public static final int CELL_UP_LEFT = 2;
    /** this is the direction for {@link cell#exitTo} to go visually up and right */
    public static final int CELL_UP_RIGHT = 4;
    /** this is the direction for {@link cell#exitTo} to go visually down and right */
    public static final int CELL_DOWN_RIGHT = 5;
	public static final int CELL_FULL_TURN() { return(CELL_FULL_TURN); }
	public static final int CELL_HALF_TURN() { return(CELL_HALF_TURN); }


	public void sameboard(infiniteHexBoard<CELLTYPE> other)
	{	// sameboard checks the cells using the allCells chain
		super.sameboard(other);
		
	}
 
	/**
	 * create an initial grid of rows and columns
	 * cols start at 'A' and rows start at 1
	 * 
	 * @param nc number of columns
	 * @param nr number of rows
	 */
    public void initBoard(int nc,int nr)
    {	
    	super.initBoard(nc,nr);
    }

    /**
     * find the direction from fc,fr to tc,tr (given that they are on a line)
	 * these directions are appropriate for {@link cell#exitTo}.
	 */
	public int findDirection(char fc,int fr0,char tc,int tr0)
	  {	
		if(fc==tc) { return((fr0<tr0)?  CELL_UP_LEFT : CELL_DOWN_RIGHT ); }
	  	if(fr0==tr0) { return ((fc<tc) ? CELL_UP_RIGHT :  CELL_DOWN_LEFT) ; }
	  	return (fc<tc) ? CELL_UP : CELL_DOWN; 
	}
 
    private int firstRowOffset(int col)
    {
    	return ncols-col ;
    }
 
    /** convert cell col,row to Y coordinate
     * 
     */
    public double cellToY00(char colchar, int arow)
    {
        int col = geo_colnum(colchar);
        int thisrow = geo_rownum(colchar,arow);
        int coffset = firstRowOffset(col);
        double y0 = (thisrow * displayParameters.YCELLSIZE) 
        		+ (coffset * displayParameters.GRIDSIZE)
        		;

        return (y0);
    }

}
