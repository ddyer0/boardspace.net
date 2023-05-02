package online.game;

import lib.G;
import online.game.cell.Geometry;

/**
 * common support for square geometry boards with connections in 2 4 or 8 directions.  
 * @see squareBoard
 * @see rectBoard
 * @see trackBoard 
 * @author ddyer
 *
 */
public abstract class infiniteSquareBoard<CELLTYPE extends cell<CELLTYPE>> extends infiniteBoard<CELLTYPE>
{	
	private static int sq_dxs[] = { -1, 0, 1, 0};
	private static int sq_dys[] = {  0, 1, 0, -1};
	public int[] dxs() { return sq_dxs; }
	public int[] dys() { return sq_dys; }
	protected static int CELL_FULL_TURN = 4;
	protected static int CELL_HALF_TURN = 2;
	protected static int CELL_QUARTER_TURN = 1;
	protected static int CELL_LEFT = 0;
	protected static int CELL_UP = 1;
	protected static int CELL_RIGHT = 2;
	protected static int CELL_DOWN = 3;
	
	public Geometry geometry() { return Geometry.Square; }
	
	public void setRotation(int rotation)
	{
		switch(rotation%4)
    	{
    	default:
    	case 0: 	
    		displayParameters.reverse_y = false;
    		displayParameters.reverse_x = false;
    		displayParameters.swapXY = false;
    		break;
    	case 2:		
    		displayParameters.reverse_y = true;
    		displayParameters.reverse_x = false;
    		displayParameters.swapXY = false;
    		break;
    	case 3:	
    		displayParameters.reverse_y = false;
    		displayParameters.reverse_x = true;
    		displayParameters.swapXY = true;
    		break;
    	case 1:	
    		displayParameters.reverse_y = true;
    		displayParameters.reverse_x = true;
    		displayParameters.swapXY = true;
    		break;
    	}
	}

    /**
     * find the direction from fc,fr to tc,tr (given that they are on a line)
     * these directions are appropriate for cell.exitTo.
     */
    public int findDirection(char fc,int fr0,char tc,int tr0)
    {	int dx = G.signum(tc-fc);
    	int dy = G.signum(tr0-fr0);
    	int dxs[] = dxs();
    	int dys[] = dys();
    	for(int i=0;i<dxs.length;i++) { if((dxs[i]==dx)&&(dys[i]==dy)) { return(i); }}
    	throw G.Error("No direction found");
    }
 
}
