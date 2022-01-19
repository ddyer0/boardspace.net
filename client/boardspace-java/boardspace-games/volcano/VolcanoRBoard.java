package volcano;


import lib.G;
import online.game.*;

/*
 * This is a minimal, game specific extension to rectBoard, structured
 * so the real board can use either a rectBoard or hexBoard extension
 * both the main "board" class and the main "viewer" class use this
 * object to locate board cells, and to translate board cells to geometry
 * 
 */
public class VolcanoRBoard extends rectBoard<VolcanoCell> implements VolcanoConstants
{ 	// factory method
	public VolcanoCell newcell(char c,int r)
	{	return(new VolcanoCell(c,r,cell.Geometry.Oct));
	}

	public BoardState getState() { throw G.Error("shouldn't be called"); }
    public void doInit(String var,long rv) { G.Error("shouldn't be called"); }

    public VolcanoRBoard(int boardsize) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = VOLCANOGRIDSTYLE; //coordinates left and bottom
    	Rect_Init(boardsize);
     }

    // init for Rectangular board.  Presumably there could be different
    // initializations for variation games.
    public void Rect_Init(int boardSize)
    {   // init the board with pyramids board code 
    	initBoard(boardSize,boardSize); //this sets up the board and cross links
    
        for(int col=0;col<ncols;col++)
        {	int colors[] = starting_colors[ncols-col-1];
        	for(int row=0;row<nrows;row++)
        	{	VolcanoCell cell = getCell((char)('A'+col),row+1);
        		for(int idx=0;idx<Pyramid.nSizes;idx++) 
        			{ cell.addChip(Pyramid.getPyramid(colors[row],Pyramid.nSizes-idx-1));	// make nests
        			}
        		}
        	}
		//  add the caps
		Pyramid p = Pyramid.getPyramid(Pyramid.BLACK_PYRAMID,Pyramid.SMALL_PYRAMID);
		for(int i=0;i<nrows;i++) { getCell((char)('A'+i),i+1).addChip(p); }


    }


}
