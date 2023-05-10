package online.game;

import online.game.cell.Geometry;

/**
 * Implement a one dimensional board with a race track around the edges, like a monopoly board. 
 * This handles only the simplest version of such board where all the edge cells are the same size. 
 * This geometry is used by Mogul. 
 * @author ddyer
 * @see rectBoard
 * @see squareBoard
 * @param <CELLTYPE>
 */
public abstract class trackBoard<CELLTYPE extends cell<CELLTYPE>> extends square_geo_board<CELLTYPE> 
{	private static int track_dys[] = { -1, 1};
	private static int track_dxs[] = {  0, 0};
	public Geometry geometry() { return Geometry.Line; }
	public int[] dxs() { return track_dxs; }
	public int[] dys() { return track_dys; }
	
	public static String DIRECTION_NAMES[]  = {"Forward","Backward"};
	public static final int CELL_LEFT = 0;
	public static final int CELL_RIGHT = 1;
	public static final int CELL_FULL_TURN = 2;
	//
	// separate boardColumns and boardRows is not a mistake
	//
	private int boardColumns;
	private int boardRows;
	
	/**
	 * construct a linear geometry board with a racetrack of square cells, like a monopoly board.
	 * this is actually a 1 x n board, where cells are references by getCell('A',n);  
	 * 
	 */
	public void  initBoard(int ncol,int nrow)
	{	
		boardColumns = ncol;
		boardRows = nrow;
		super.initBoard(1,nrow*2+ncol*2);	// represent as a 1d array of cells
	}
	// cut the x and y coordinates into 4 segments around the edges
	public double cellToX00(char colchar, int thisRow)
	{	int xpos = 0;
		if(thisRow<=boardColumns) { xpos = thisRow; }
		else if(thisRow<=(boardColumns+boardRows)) { xpos =  boardColumns; }
		else if(thisRow<=(boardColumns+boardColumns+boardRows)) { xpos = boardColumns-(thisRow-(boardColumns+boardRows)); }
		else { xpos = 0; }
		return(xpos*displayParameters.CELLSIZE);
	}
	// cut the x and y coordinates into 4 segments around the edges
	public double cellToY00(char colchar, int thisRow)
	{	int ypos = 0;
		if(thisRow<=boardColumns) { ypos = 1; }
		else if(thisRow<=(boardColumns+boardRows)) { ypos = thisRow-boardColumns +1; }
		else if(thisRow<=(boardColumns+boardColumns+boardRows)) { ypos = boardRows+1; }
		else { ypos = boardRows - (thisRow-(boardColumns+boardColumns+boardRows+1)); }
		return(ypos*displayParameters.YCELLSIZE);
	}
	
}
