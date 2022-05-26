package octiles;

import java.awt.Color;
import java.awt.Rectangle;

import online.game.*;
import java.util.*;
import lib.*;
import lib.Random;

import static octiles.OctilesMovespec.*;

/**
 * OctilesBoard knows all about the game of octiles, which is played
 * on a wonderful 5x5 board that's a grid of octagons and squares.
 * The octagons host octagonal tiles, the squares host pawns, which are
 * called "runners". 
 * 
 * The board representation is based on "rectBoard", but pretty radically
 * modified.  The "rectboard" supplies cells for the runners.  We add to
 * that an additional 5x5 board for the octagonal tiles.  The two boards
 * are unlinked and interlinked to match the connectivity of the physical
 * board, with the runner cells interspersed between the tile cells.  In 
 * addition to this general connectivity scheme, the tile cells have extra
 * assymetric links added in 8 places, and the outer ring of runner cells
 * are connected to exactly one tile cell instead of the normal 4.
 * 
 * "allCells" remains the list of all runner cells.
 * "allTiles" remains the list of all tile cells.
 * 
 * the tile cells are lower cased, so 'a1' is a tile cell, while 'A1' is a runner cell
 * 
 * 2DO: record the deck shuffle in the game records
 * this will disentangle game records from the live random number generator

 * @author ddyer
 *
 */

class OctilesBoard extends rectBoard<OctilesCell>implements BoardProtocol,OctilesConstants
{
    static final String[] OctilesGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	static final int DEFAULT_COLUMNS = 10;	// 8x6 board
	static final int DEFAULT_ROWS = 10;
	private OctilesState unresign;
	private OctilesState board_state;
	public CellStack animationStack = new CellStack(); 
	public OctilesState getState() {return(board_state); }
	public void setState(OctilesState st) 
	{ 	unresign = (st==OctilesState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

	int runners_in_game = 0;
	int search_clock = 0;
    public void SetDrawState() { setState(OctilesState.DRAW_STATE); }
    private static int MAX_PLAYERS = 4;
    
    // the squares where the runners sit are the standard "cells".  
    // the octagons where the tiles sit are called "tiles"
    public OctilesCell tiles[][] = new OctilesCell[5][5];	// places to put the tiles
    public OctilesCell allTileCells = null;
    public OctilesCell tilePool = null;
    private OctilesCell homePads[][] = new OctilesCell[MAX_PLAYERS][5];	// always 4 even in 2 player games
    public OctilesCell goalPads[][] = new OctilesCell[MAX_PLAYERS][5];	// always 4 even in 2 player games
    OctilesChip playerRunner[] = new OctilesChip[MAX_PLAYERS];
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public OctilesChip pickedObject = null;
    public int pickedObjectRotation = 0;
    public OctilesCell pickedSource = null;
    private OctilesCell droppedDest = null;
    
    private OctilesCell droppedTileSource = null;
    private OctilesCell droppedTileDest = null;
    private OctilesChip droppedTile = null;
    private int droppedTileSavedRotation = 0;
   
   // get the player index associated with a runner type
   public int getPlayerIndex(OctilesChip forRunner)
   {	for(int i=0;i<players_in_game;i++) { if(playerRunner[i]==forRunner) { return(i); }}
   		throw G.Error("Runner doesn't have a player");
   }

    // tile cells are 'a' instead of 'A' and 'a,1 is located between 'A',1 and 'B',2
   public OctilesCell getTile(char col,int row)
    {	if(!((col>='a')&&(col<='e') && (row>=1) && (row<=5)))
    		{return(null);
    		}
    	return(tiles[col-'a'][row-1]);
    }
   public int numberHome(int pl)
   {   int count = 0;
	   OctilesCell goals[] = goalPads[pl];
	   for(int i=0,lim=goals.length; i<lim;i++)
	   {	OctilesCell c = goals[i];
	   		if(c.topChip()==c.goalForColor) { count++; }	
	   }
	   return(count);
   }

    /**
     * the octiles coordinate system is more complex than normal.  There
     * are two overlapping grids, one for the the runners (called "post cells")
     * and one for the tiles.  The runners are addressed as A-Z 1-N, the tiles
     * are addressed as a-z 1-n.  Additionally, the colored launch/destination
     * spaces are positioned off the regular grid at arbitrary positions.
     */
    public boolean visualParameters=true;
    public void SetDisplayRectangle(Rectangle r)
    {
	    visualParameters = false;	// inhibit the nonstandard locations
	    super.SetDisplayRectangle(r);	// calculate grid parameters
	    visualParameters = true;	// restore normal operation
    }

	
	public int cellToY(char colchar, int thisrow)
	{	//adjustPadCoordinates_normal();
		if(visualParameters)
		{OctilesCell c = getCell(colchar,thisrow);
		if((c!=null) && ((c.xpos!=0.0) || (c.ypos!=0.0)))
		{	// specially positioned post cell
			// extra scale and offset factors of not quite understood origin
			// but likely related to the autoscaling process
			return((int)(displayParameters.HEIGHT-c.ypos*displayParameters.HEIGHT));
		}}
		if(colchar<'Z') 
			{ 	
			return(super.cellToY(colchar,thisrow)); 
			}	// post cell
	
		int y0 = super.cellToY((char)(colchar-'a'+'A'),thisrow);
		int y1 = super.cellToY((char)(colchar-'a'+'A'+1),thisrow+1);
		return((y0+y1)/2);
	}
    // tile cells are 'a' instead of 'A' and 'a,1 is located between 'A',1 and 'B',2
	public int cellToX(char colchar, int thisrow)
	{	//adjustPadCoordinates_normal();
		if(visualParameters)
		{OctilesCell c = getCell(colchar,thisrow);
		if( (c!=null) && ((c.xpos!=0.0) || (c.ypos!=0.0)))
		{	// extra scale and offset factors of not quite understood origin
			// but likely related to the autoscaling process
			// specially positioned post cell
			return((int)(c.xpos*displayParameters.WIDTH));
		}}
		if(colchar<'Z') 
			{	
			return(super.cellToX(colchar,thisrow)); 
			}	// post cell
		int x0 = super.cellToX((char)(colchar-'a'+'A'),thisrow);
		int x1 = super.cellToX((char)(colchar-'a'+'A'+1),thisrow+1);
		return((x0+x1)/2);
	}
   //
   // private variables
   //
     
    public OctilesCell getCell(char col,int row)
    {	if(col=='@'&&(row==-1)) { return(tilePool); }
    	OctilesCell  c= getTile(col,row);
    	if(c!=null) { return(c); }
    	return(super.getCell(col,row));
    }

	// factory method
	public OctilesCell newcell(char c,int r)
	{	return(new OctilesCell(c,r));
	}
    public OctilesBoard(String init,int nplay,long ran,int map[]) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = OctilesGRIDSTYLE; //coordinates left and bottom
    	setColorMap(map);
    	construct();
        doInit(init,ran,nplay); // do the initialization 
     }

    public int nextValidRotation(OctilesCell cell,int incr)
    {	if((board_state==OctilesState.PUZZLE_STATE)||(cell!=droppedTileDest))
    		{ return((cell.rotation+incr+8)%8); }
    	// a temp tile
    	for(int i=1;i<=8;i++)
    	{
    	CommonMoveStack v = new CommonMoveStack();
    	int nextrot = ((droppedTileDest.rotation+incr*i)+8)%8;
    	getListOfRunnerMoves(playerRunner[whoseTurn],(board_state==OctilesState.MOVE_RUNNER_HOME_STATE),v,droppedTileDest,droppedTileDest.topChip(),nextrot);
    	if(v.size()>0) { return(nextrot); }
    	}
    	return(droppedTileDest.rotation);
    }
    
    public Hashtable<OctilesCell,OctilesCell> getSources()
    {	Hashtable<OctilesCell,OctilesCell> h = new Hashtable<OctilesCell,OctilesCell>();
    	switch(board_state)
    	{
    	case MOVE_RUNNER_STATE:
    	case MOVE_RUNNER_HOME_STATE:
       		G.Assert(droppedTileDest!=null,"tile was dropped");
       		CommonMoveStack v = new CommonMoveStack();
       		getListOfRunnerMoves(playerRunner[whoseTurn],(board_state==OctilesState.MOVE_RUNNER_HOME_STATE),v,droppedTileDest,droppedTile,droppedTileDest.rotation);
       		for(int i=0;i<v.size();i++)
    		{	OctilesMovespec m = (OctilesMovespec)v.elementAt(i);
    			OctilesCell c = getCell(m.from_col,m.from_row);
    			if(c!=null)
    			{ h.put(c,c);
    			//G.print("s "+m);
    			}
    		}
       		break;
		default:
			break;
		
    	}
    	return(h);
    }
    public int getValidRotation(OctilesCell cell,int preferred)
    {	boolean homeflag=false;
    	switch(board_state)
    	{
    	case MOVE_RUNNER_HOME_STATE:
    	case MOVE_RUNNER_STATE:
    	case PUZZLE_STATE:
    	case CONFIRM_STATE: 
    	case CONFIRM_PASS_STATE:
    		return(preferred);
    	case PLAY_TILE_HOME_STATE:
    		homeflag = true;
			//$FALL-THROUGH$
		case PLAY_TILE_STATE:
    		CommonMoveStack  v = new CommonMoveStack();
    		OctilesChip who = playerRunner[whoseTurn];
    		getListOfRunnerMoves(who,homeflag,v,cell,pickedObject,preferred); 
    		if(v.size()>0) { return(preferred); }
    		getListOfRunnerMoves(who,homeflag,v,cell,pickedObject,-1);
    		if(v.size()>0)
    			{ OctilesMovespec m = (OctilesMovespec)v.elementAt(0);
    			  return(m.rotation);
    			}
    		break;
		default:
			break;
    	}
    	return(preferred);
    }
    public Hashtable<OctilesCell,OctilesMovespec> getDests(boolean any_rotation)
    {	Hashtable<OctilesCell,OctilesMovespec> h = new Hashtable<OctilesCell,OctilesMovespec>();
    	if((pickedSource!=null) && (pickedObject!=null))
    	{
    	boolean homeFlag = false;
    	OctilesChip who = playerRunner[whoseTurn];
    	switch(board_state)
    	{
    	case MOVE_RUNNER_HOME_STATE:
    		homeFlag=true;
			//$FALL-THROUGH$
		case MOVE_RUNNER_STATE:
    		{
    		G.Assert(droppedTileDest!=null,"tile was dropped");
    		CommonMoveStack  v = new CommonMoveStack();
    		
    		if(pickedObject!=null)
    			{
       			getListOfRunnerMoves(who,v,
       					pickedSource,pickedObject,
       					droppedTileDest,droppedTile,droppedTileDest.rotation);
    			}
    		else {
    			getListOfRunnerMoves(who,homeFlag,v,droppedTileDest,droppedTile,droppedTileDest.rotation);
    		}
       		for(int i=0;i<v.size();i++)
    		{	OctilesMovespec m = (OctilesMovespec)v.elementAt(i);
    			OctilesCell c = getCell(m.to_col,m.to_row);
    			if(c!=null) 
    				{ h.put(c,m);
  				}
    		}
    		}
    		break;
    	case PLAY_TILE_HOME_STATE:
    		homeFlag = true;
			//$FALL-THROUGH$
		case PLAY_TILE_STATE:
    		{
   			CommonMoveStack  v = new CommonMoveStack();
    		getListOfTileMoves(who,homeFlag,v,pickedObject);
    		for(int i=0;i<v.size();i++)
    		{	OctilesMovespec m = (OctilesMovespec)v.elementAt(i);
    			OctilesCell c = getCell(m.drop_col,m.drop_row);
    			if(c!=null) 
    			{ h.put(c,m);
    			}
    		}
    		}
			break;
		default:
			break;
    	}}
    	return(h);
    }

    private void relink(OctilesCell from,int from_dir, OctilesCell to,int to_dir)
    {	if(from!=null) 
    		{ from.addLinkU(from_dir,to); 
    		  from.setEntryDirection(from_dir,to_dir);
    		}
    	to.addLinkU(to_dir,from);
    	to.setEntryDirection(to_dir,from_dir);
    }
    
    // return true if this is one of the cell positions for tiles
    // that are empty
    private boolean missingTile(char col,int row)
    {	if((col=='a')&&((row==1)||(row==4)||(row==5))) { return(true); }
    	if((col=='e')&&((row==1)||(row==2)||(row==5))) { return(true); }
    	if((col=='b')&&(row==1)) { return(true); }
    	if((col=='d')&&(row==5)) { return(true); }
    	return(false);
    }
    private void construct()
    {	Random r = new Random(6278843);
        initBoard(6,6); //this sets up the board and cross links for the runners
        allCells.setDigestChain(r);
        tilePool = new OctilesCell(r,OctilesId.TilePoolRect);
        for(OctilesCell c = allCells; c!=null; c=c.next) 
        	{ c.rackLocation = OctilesId.PostLocation;
        	  c.isPostCell=true; 
        	  c.isTileCell=false;
        	  c.unCrossLink();  
        	}
        // create the tile cells, except the 8 missing tiles
        allTileCells = null;
        for(int colNum=0;colNum<tiles.length;colNum++)
        {	char col = (char)('a'+colNum);
        	OctilesCell row[] = tiles[colNum];
        	for(int rv=1;rv<=row.length;rv++)
        	{	if(!missingTile(col,rv)) 
        		{OctilesCell c = newcell(col,rv);
        		c.next = allTileCells;
        		allTileCells = c;
        		c.rackLocation = OctilesId.TileLocation;
        		row[rv-1]=c;
        		c.isTileCell =true;
        	}}}
        tilePool.next = allTileCells;
        allTileCells.setDigestChain(r);
        // link the tile cells inbetween the runner cells
        for(int colNum=0; colNum<5; colNum++)
        { 
        char col=(char)('a'+colNum);
        for(int row=1;row<=5; row++)
        {	
        if(!missingTile(col,row))
        {	OctilesCell blueCell = getCell((char)('A'+colNum),row);
        	OctilesCell redCell = getCell((char)('A'+colNum+1),row);
        	OctilesCell yellowCell = getCell((char)('A'+colNum),row+1);
        	OctilesCell greenCell = getCell((char)('A'+colNum+1),row+1);
         	OctilesCell tile = getTile(col,row);
        	relink(blueCell,CELL_UP_RIGHT,tile,CELL_DOWN_LEFT);
           	relink(redCell,CELL_UP_LEFT,tile,CELL_DOWN_RIGHT);
           	relink(yellowCell,CELL_DOWN_RIGHT,tile,CELL_UP_LEFT);
           	relink(greenCell,CELL_DOWN_LEFT,tile,CELL_UP_RIGHT);
         
           	relink(getTile((char)(col-1),row),CELL_RIGHT,tile,CELL_LEFT);
          	relink(getTile((char)(col+1),row),CELL_LEFT,tile,CELL_RIGHT);
          	relink(getTile(col,row-1),CELL_UP,tile,CELL_DOWN);
         	relink(getTile(col,row+1),CELL_DOWN,tile,CELL_UP);
        }
        }}
        
        for(int i=1; i<6;i++)
        {
        OctilesCell firstPlayerCell = getCell('A',i);
        firstPlayerCell.unCrossLink();
        OctilesCell thirdPlayerCell = getCell('F',7-i);
        thirdPlayerCell.unCrossLink();
        OctilesCell greenCell = getCell((char)('A'+i),1);
       	greenCell.unCrossLink();
        OctilesCell redCell = getCell((char)('A'+i-1),6);
        redCell.unCrossLink();
        }
        
        adjustPadCoordinates_perspective();
        
        // finally, ad-hoc link the orphaned post cells to the remaining tile cells
        //relink(getCell('A',1),Direction_Diagonal_Up_Right
        {
        int links[][] = {
        		// blue homes
        		{'A',1,CELL_UP,'a',2,CELL_DOWN},
        		{'A',2,CELL_UP_RIGHT,'a',2,CELL_DOWN_LEFT},
        		{'A',3,CELL_RIGHT,'a',2,CELL_LEFT},
        		{'A',4,CELL_RIGHT,'a',3,CELL_LEFT},
        		{'A',5,CELL_DOWN_RIGHT,'a',3,CELL_UP_LEFT},
        		// red homes
        		{'A',6,CELL_RIGHT,'b',5,CELL_LEFT},
        		{'B',6,CELL_DOWN_RIGHT,'b',5,CELL_UP_LEFT},
        		{'C',6,CELL_DOWN,'b',5,CELL_UP},
        		{'D',6,CELL_DOWN,'c',5,CELL_UP},
        		{'E',6,CELL_DOWN_LEFT,'c',5,CELL_UP_RIGHT},
        		// yellow homes
        		{'F',6,CELL_DOWN,'e',4,CELL_UP},
        		{'F',5,CELL_DOWN_LEFT,'e',4,CELL_UP_RIGHT},
        		{'F',4,CELL_LEFT,'e',4,CELL_RIGHT},
        		{'F',3,CELL_LEFT,'e',3,CELL_RIGHT},
        		{'F',2,CELL_UP_LEFT,'e',3,CELL_DOWN_RIGHT},
        		// green homes
        		{'F',1,CELL_LEFT,'d',1,CELL_RIGHT},
        		{'E',1,CELL_UP_LEFT,'d',1,CELL_DOWN_RIGHT},
        		{'D',1,CELL_UP,'d',1,CELL_DOWN},
        		{'C',1,CELL_UP,'c',1,CELL_DOWN},
        		{'B',1,CELL_UP_RIGHT,'c',1,CELL_DOWN_LEFT},
        		// extra links drawn on the board.  Note that these are not
        		// symmetric inverse linkages.
        		{'a',3,CELL_UP,'b',4,CELL_LEFT},
        		{'c',5,CELL_RIGHT,'d',4,CELL_UP},
        		{'e',3,CELL_DOWN,'d',2,CELL_RIGHT},
        		{'c',1,CELL_LEFT,'b',2,CELL_DOWN},
        		{'a',2,CELL_UP_LEFT,'a',3,CELL_DOWN_LEFT},
        		{'b',5,CELL_UP_RIGHT,'c',5,CELL_UP_LEFT},
        		{'e',4,CELL_DOWN_RIGHT,'e',3,CELL_UP_RIGHT},
        		{'d',1,CELL_DOWN_LEFT,'c',1,CELL_DOWN_RIGHT}
        		
        };
        for(int row=0;row<links.length;row++)
        { int rr[] = links[row];
          OctilesCell from = (rr[0]>='a')?getTile((char)rr[0],rr[1]):getCell((char)rr[0],rr[1]);
          OctilesCell to = getTile((char)rr[3],rr[4]);
          relink(from,rr[2],to,rr[5]);
        }
        }

    }
    
    private int getUnusedColor(int map[])
    {	int usedColors = 0;
    	for(int i=0;i<map.length;i++) { usedColors |= 1<<map[i]; }
    	usedColors = ~usedColors;		// unused colors
    	int unusedColor = usedColors & -usedColors;	// least 1 bit
    	int n = G.numberOfTrailingZeros(unusedColor);
    	return(Math.min(MAX_PLAYERS-1,n));
    }
    // get a color map which uses all 4 colors
    private int[] getExtendedMap()
    {
    	int map[] = getColorMap();
    	int extended[] = new int[MAX_PLAYERS];
    	AR.setValue(extended, -1);
    	for(int i=0,len=map.length;i<MAX_PLAYERS;i++)
    	{	if(i<len) { extended[i]=map[i]; }
    		else { extended[i] = getUnusedColor(extended); }
    	}
    	return(extended);
    }
    private void setHomes()
    {
        // 
        // unlink the runner home cells. (we'll relink later)
        // set their real coordinates
        int map[]=getExtendedMap();
        // in a 3 or 4 player game, players and colors rotate clockwise
        // in a 2 player game, skip the second position and pair 1 with 3
        int secondPlayerIndex = (players_in_game==2) ? 2 : 1;
        int thirdPlayerIndex = (players_in_game==2) ? 1 : 2;
        for(int i=0;i<4;i++) { playerRunner[i]=OctilesChip.getRunner(map[i]); }
        
        for(int i=1; i<6;i++)
        { OctilesCell firstPlayerCell = getCell('A',i);
        	firstPlayerCell.homeForColor = playerRunner[0];
           	firstPlayerCell.goalForColor = playerRunner[thirdPlayerIndex];
        	homePads[0][i-1] = firstPlayerCell;
        	goalPads[thirdPlayerIndex][i-1] = firstPlayerCell;
 
         OctilesCell thirdPlayerCell = getCell('F',7-i);
        	thirdPlayerCell.homeForColor = playerRunner[thirdPlayerIndex];
        	thirdPlayerCell.goalForColor = playerRunner[0];
        	homePads[thirdPlayerIndex][i-1] = thirdPlayerCell;
        	goalPads[0][i-1] = thirdPlayerCell;
        	
         OctilesCell greenCell = getCell((char)('A'+i),1);
         	greenCell.homeForColor = playerRunner[3];
        	greenCell.goalForColor = playerRunner[secondPlayerIndex];
        	homePads[3][i-1] = greenCell;
        	goalPads[secondPlayerIndex][i-1] = greenCell;
      	
         OctilesCell redCell = getCell((char)('A'+i-1),6);
         	redCell.homeForColor = playerRunner[secondPlayerIndex];
         	redCell.goalForColor = playerRunner[3];
           	homePads[secondPlayerIndex][i-1] = redCell;
        	goalPads[3][i-1] = redCell;

        }
       	for(int pl=0;pl<4;pl++) 
       	{ 
       	G.Assert(homePads[pl][0].homeForColor==playerRunner[pl],"home mismatch");
      	G.Assert(goalPads[pl][0].goalForColor==playerRunner[pl],"home mismatch");      	
       	}
    }
    
    void adjustPadCoordinates_perspective()
    {	tilePool.xpos = 0.868;
    	tilePool.ypos = 0.72;
    	double blue_pads[][] =	// left side bottom up
    	{{0.22, 0.656}, 
    	 {0.14, 0.63},
    	 {0.12, 0.57}, 
    	 {0.126, 0.46}, 
    	 {0.16, 0.40}};
    	double red_pads[][] =  // top side left to right
    	{{0.275, 0.263}, 
    	 {0.31, 0.20}, 
    	 {0.383, 0.18}, 
    	 {0.514, 0.18}, 
    	 {0.586, 0.20}};
    	double yellow_pads[][] = // right side top down
    	{{0.777, 0.27},
         {0.852, 0.3}, 
         {0.885, 0.351}, 
         {0.89, 0.455},
         {0.86, 0.515}};
    	double green_pads[][] = // bottom side right to left
    	{{0.754, 0.667}, 
    	 {0.72, 0.73}, 
    	 {0.643, 0.76}, 
    	 {0.5, 0.755}, 
    	 {0.42, 0.73}};
  	
        // 
        // set their real coordinates
        for(int i=1; i<6;i++)
        { OctilesCell blueCell = getCell('A',i);
          blueCell.xpos = blue_pads[i-1][0];
          blueCell.ypos = blue_pads[i-1][1];
  
         OctilesCell yellowCell = getCell('F',7-i);
         	yellowCell.xpos = yellow_pads[i-1][0];
        	yellowCell.ypos = yellow_pads[i-1][1];
        	
         OctilesCell greenCell = getCell((char)('A'+i),1);
          	greenCell.xpos = green_pads[5-i][0];
        	greenCell.ypos = green_pads[5-i][1];
      	
         OctilesCell redCell = getCell((char)('A'+i-1),6);
         	redCell.xpos = red_pads[i-1][0];
         	redCell.ypos = red_pads[i-1][1];
        }    	
    }
    void adjustPadCoordinates_normal()
    {	tilePool.xpos = 0.85;
		tilePool.ypos = 0.81;
    	double blue_pads[][] =	// left side bottom up
    	{{0.255, 0.730}, //{0.287, 0.692}, 
    	 {0.18, 0.694},
    	 {0.150, 0.62}, 
    	 {0.150, 0.49}, 
    	 {0.178, 0.415}};
    	double red_pads[][] =  // top side left to right
    	{{0.28, 0.225}, 
    	 {0.315, 0.15}, 
    	 {0.39, 0.12}, 
    	 {0.523, 0.12}, 
    	 {0.595, 0.15}};
    	double yellow_pads[][] = // right side top down
    	{{0.787, 0.252},
         {0.86, 0.284}, 
         {0.892, 0.355}, 
         {0.89, 0.49},
         {0.858, 0.561}};
    	double green_pads[][] = // bottom side right to left
    	{{0.753, 0.755}, 
    	 {0.722, 0.827}, 
    	 {0.65, 0.856}, 
    	 {0.52, 0.854}, 
    	 {0.443, 0.824}};
  	
        // 
        // set their real coordinates
        for(int i=1; i<6;i++)
        { OctilesCell blueCell = getCell('A',i);
          blueCell.xpos = blue_pads[i-1][0];
          blueCell.ypos = blue_pads[i-1][1];
  
         OctilesCell yellowCell = getCell('F',7-i);
         	yellowCell.xpos = yellow_pads[i-1][0];
        	yellowCell.ypos = yellow_pads[i-1][1];
        	
         OctilesCell greenCell = getCell((char)('A'+i),1);
          	greenCell.xpos = green_pads[5-i][0];
        	greenCell.ypos = green_pads[5-i][1];
      	
         OctilesCell redCell = getCell((char)('A'+i-1),6);
         	redCell.xpos = red_pads[i-1][0];
         	redCell.ypos = red_pads[i-1][1];
        }    	
    }
    private void Init_Standard(String game,int play,long key)
    { 	if(Octiles_INIT.equalsIgnoreCase(game)) 
    		{ 
    		}
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        setState(OctilesState.PUZZLE_STATE);
        players_in_game = play;
        win = new boolean[play];
        switch(players_in_game)
        {	case 4:
        		runners_in_game = 4;
        		break;
        	default: 
        		runners_in_game=5;
        		break;
        }
        randomKey = key;
        tilePool.reInit();
        for(OctilesCell c=allCells; c!=null; c=c.next) { c.reInit(); }
        for(OctilesCell c=allTileCells; c!=null; c=c.next) { c.reInit(); }
        
        droppedTileSource = null;
        droppedTileDest = null;
        droppedTile = null;
        droppedTileSavedRotation = 0;
        pickedObject = null;
        
        setHomes();
        
        // place the tiles in the rack. 
        for(int i=0;i<OctilesChip.NTILES;i++) { tilePool.addChip(OctilesChip.getTile(i)); }
        {
        Random randomSetup = new Random(randomKey);
        tilePool.shuffle(randomSetup);
        if(players_in_game==3) 
    	{ addChip(getCell('c',1),tilePool.removeTop(),Random.nextInt(randomSetup,8));
    	  addChip(getCell('d',1),tilePool.removeTop(),Random.nextInt(randomSetup,8));
    	}
      
        }
        for(int pl=0;pl<players_in_game;pl++)
        {
        OctilesCell home[] = homePads[pl];
        OctilesChip runner = playerRunner[pl];
        for(int i=0;i<5;i++)
        	{
        	if((runners_in_game==4)&&(i==2)) {}
        	else 
        		{	home[i].addChip(runner);
        		}
        	}
        }
        whoseTurn = FIRST_PLAYER_INDEX;

    }

    public void sameboard(BoardProtocol f) { sameboard((OctilesBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(OctilesBoard from_b)
	{	super.sameboard(from_b);

        for(OctilesCell c = allTileCells,d=from_b.allTileCells;
        	c!=null;
        	c=c.next,d=d.next)
        {	G.Assert(c.sameCell(d),"cells match");
        }
        G.Assert(tilePool.sameCell(from_b.tilePool),"tile pool matches");
        
        G.Assert((droppedTileSource==from_b.droppedTileSource)
        		|| ((droppedTileSource!=null)&& droppedTileSource.sameCell(from_b.droppedTileSource)),
        		"same dropped tile source");
        G.Assert((droppedTileDest==from_b.droppedTileDest)
        		|| ((droppedTileDest!=null)&& droppedTileDest.sameCell(from_b.droppedTileDest)),
        		"same dropped tile dest");
        G.Assert((droppedTile==from_b.droppedTile)
        		 && (droppedTileSavedRotation==from_b.droppedTileSavedRotation),"same dropped tile");
            
        G.Assert(pickedObject==from_b.pickedObject,"same picked object");
        G.Assert(OctilesCell.sameCell(pickedSource,from_b.pickedSource),"same pickedSource");
        G.Assert(OctilesCell.sameCell(droppedDest,from_b.droppedDest),"same droppedDest");
        
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

    }

    /** 
     * Digest produces a 32 bit hash of the game state.  This is used 3 different
     * ways in the system.
     * (1) This is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     *  looks for duplicate digests.  
     * (2) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (3) Digests are used by the search machinery as a check on the robot's winding/unwinding
     * of the board position, this is mainly a debug/development function, but a very useful one.
     * @return
     */
   public long Digest()
    {

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest();
      
		for(OctilesCell c = allTileCells;c!=null; c=c.next)
		{	v ^= c.Digest();
		}
		v ^= tilePool.Digest(r);
        // for most games, we should also digest whose turn it is
		for(int i=0;i<players_in_game;i++)
		{	long v0 = r.nextLong();
			if(i==whoseTurn) { v ^= v0; }
		}
		v ^= chip.Digest(r,pickedObject);
		v ^= cell.Digest(r,pickedSource);
		v ^= r.nextLong()*(board_state.ordinal()*10);
        return (v);
    }
   public OctilesBoard cloneBoard() 
	{ OctilesBoard copy = new OctilesBoard(gametype,players_in_game,randomKey,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((OctilesBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(OctilesBoard from_b)
    {	
    	super.copyFrom(from_b);
         
 		for(OctilesCell dest=allTileCells,src=from_b.allTileCells;
			dest!=null;
			dest=dest.next,src=src.next)
		{	dest.copyFrom(src);
		}
		tilePool.copyFrom(from_b.tilePool);
	    droppedTileSource = getCell(from_b.droppedTileSource);
	    droppedTileDest = getCell(from_b.droppedTileDest);
	    droppedTile = from_b.droppedTile;
	    droppedTileSavedRotation = from_b.droppedTileSavedRotation;
	    pickedObject = from_b.pickedObject;
	    pickedSource = getCell(from_b.pickedSource);
	    droppedDest = getCell(from_b.droppedDest);
	    pickedObjectRotation = from_b.pickedObjectRotation;
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        sameboard(from_b);
    }

    /* initialize a board back to initial empty state */
    public void doInit() { doInit(gametype,randomKey,players_in_game); }
    public void doInit(String type,long key) { doInit(type,key,players_in_game); } 
    public void doInit(String gtype,long seed,int nplay)
    {
 
       Init_Standard(gtype,nplay,seed);
       moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }


    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player");
        case PUZZLE_STATE:
            break;
        case CONFIRM_STATE:
        case CONFIRM_PASS_STATE:
        case PASS_STATE:
        case PLAY_TILE_STATE:
        case PLAY_TILE_HOME_STATE:
        case DRAW_STATE:
        case RESIGN_STATE:
            moveNumber++; //the move is complete in these states
			setWhoseTurn((whoseTurn+1)%players_in_game);
            return;
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	
        switch (board_state)
        {case RESIGN_STATE:
         case CONFIRM_STATE:
         case CONFIRM_PASS_STATE:
         case PASS_STATE:
         case DRAW_STATE:
            return (true);

        default:
            return (false);
        }
    }


    void setGameOver(boolean currentwin)
    {	for(int i=0;i<win.length;i++) { win[i] = (i==whoseTurn)?currentwin:false; }
    	setState(OctilesState.GAMEOVER_STATE);
    }
    
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	int ndone = 0;
    	if(board_state==OctilesState.GAMEOVER_STATE) { return(win[player]); }
    	OctilesCell goal[]=goalPads[player];
    	for(int i=0,lim=goal.length;i<lim;i++) 
    		{	OctilesCell c = goal[i];
    			// require all the goal posts be filled.  This will need to 
    			// be adjusted if we add a 4-runner fast game.
    			if(c.topChip()==c.goalForColor) { ndone++; }
    		}
    	return(ndone==runners_in_game);
    }
    
    // return true if all the home pads are full, so the player
    // must leave if possible.
    public boolean mustLeaveHome(int pl)
    {	OctilesCell homes[] = homePads[pl];
    	for(int i=0,lim=homes.length; i<lim; i++)
    	{
    		if(homes[i].topChip()==null) { return(false); }
    	}
    	return(true);
    }
    // look for a win for player. 
    public double ScoreForPlayer(int player,boolean print,double cup_weight,double ml_weight,boolean dumbot)
    {  	double finalv=1000.0;
    	OctilesCell goals[] = goalPads[player];
    	int goalIndex = 0;
    	OctilesChip myRunner = playerRunner[player];
    	for(OctilesCell c = allCells;
    	    c!=null;
    	    c=c.next)
    	{
    	OctilesChip top = c.topChip();
    	if(top==myRunner)
    	{
    	// found one of our runners
    	if(c.goalForColor==top) { finalv += 1; }	// already home
    	else {
    		while((goalIndex<goals.length) 
    				&& (goals[goalIndex].topChip()==myRunner)) 
    			{ goalIndex++; }
    		G.Assert(goalIndex<goals.length,"ran out of goal cells");
    		OctilesCell goal = goals[goalIndex];
    		double dist = G.distance(c.col,c.row,goal.col,goal.row);
    		finalv-=dist;
    	}
    	}
    	
    	}
     	return(finalv);
    }

    public void acceptPlacement()
    {	if(droppedTileDest!=null)
    	{
    	while(droppedTileDest.height()>1)
    		{
    		OctilesChip b = droppedTileDest.removeChipAtIndex(0);
    		tilePool.insertChipAtIndex(0,b);
    		}
    	}
        pickedObject = null;
        droppedDest = null;
        pickedSource = null;
        droppedTileSource = null;
        droppedTileDest = null;
        droppedTile = null;
        droppedTileSavedRotation = 0;
        pickedObjectRotation = 0;
     }
    private OctilesChip removeChip(OctilesCell c)
    {
       	pickedObject = c.removeTop();
       	pickedObjectRotation = c.rotation;
       	if(c==droppedDest) { droppedDest=null; }
       	if(c.isTileCell)
       	{	droppedTileDest = null;
       		c.rotation = droppedTileSavedRotation;
       	}
       	return(pickedObject);
     }
    private void addChip(OctilesCell c,OctilesChip po,int rot)
    {  
     	if(c.isTileCell)
    	{
     		droppedTileSavedRotation = c.rotation;
    		if(c.onBoard)
	    	{	droppedTileDest = c;
	    		droppedTileSource = pickedSource;
	    		droppedTile = pickedObject;
	    	}
	    	else
	    	{	c.rotation=0;
	    		droppedTileDest = null;
	    		droppedTileSource = null;
	    		droppedTile = null;
	    	}
        	c.rotation = rot;
    	}
     	c.addChip(po);
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    OctilesCell dr = droppedDest;
    if(dr!=null)
    	{
    	droppedDest = null;
    	removeChip(dr);
     	}
     }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	OctilesChip po = pickedObject;
    	if(po!=null)
    	{
    	OctilesCell ps = pickedSource;
    	pickedSource=null;
    	pickedObject = null;
    	addChip(ps,po,pickedObjectRotation);
    	if(droppedTileDest!=null)
    	{	pickedSource = droppedTileSource;
    		droppedDest = droppedTileDest;
    	}
    	}
     }

    // 
    // drop the floating object.
    //
    private OctilesCell dropObject(OctilesId dest, char col, int row,int rot)
    {
       G.Assert((pickedObject!=null),"ready to drop");
       OctilesCell loc = getCell(dest,col,row);
       addChip(loc,pickedObject,rot);
       switch (dest)
        {
        case TilePoolRect:
        	pickedObject = null;
        	droppedDest = null;
        	pickedSource=null;
        	break;
        case TileLocation: // an already filled board slot.
        	pickedObject = null;
        	droppedDest = loc;
         	break;
	default:
		break;
        }
       return(loc);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(OctilesCell cell)
    {	switch(board_state)
    	{	case CONFIRM_STATE:
    			return(droppedDest==cell);
    		case MOVE_RUNNER_HOME_STATE:
    		case MOVE_RUNNER_STATE:
    			return(droppedTileDest==cell);
    		default: 
    	}
    	return(false);
    }
    public boolean isSource(OctilesCell ccell)
    {	return((ccell!=null) && (pickedSource==ccell));
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	OctilesChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber()+pickedObjectRotation*1000);
    		}
        return (NothingMoving);
    }
    public int movingObjectRotation()
    {	return(((pickedObject!=null)&&(pickedSource!=null))?pickedObjectRotation:0);
    }    
    
    public OctilesCell getCell(OctilesCell c) { return((c==null)?null:getCell(c.rackLocation(),c.col,c.row)); }
    private OctilesCell getCell(OctilesId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case PostLocation:
        case TileLocation:
        	return(getCell(col,row));
        case TilePoolRect:
        	return(tilePool);

        }
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private OctilesCell pickObject(OctilesId source, char col, int row)
    {	G.Assert((pickedObject==null),"ready to pick");
    
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case TileLocation:
         	{
        	OctilesCell c = pickedSource = getCell(col,row);
        	removeChip(c);
        	if(c.topChip()==null) { c.rotation=0; }
        	droppedDest = null;
        	return(c);
         	}
        case TilePoolRect:
        	{
       		OctilesCell c = pickedSource = tilePool;
       		removeChip(c);
       		c.rotation=0;
       		droppedDest = null;
        	return(c);
        	}
        }
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(OctilesCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        	if(c!=null) { setNextStateAfterDone(); }
        	break;
        	
        case MOVE_RUNNER_STATE:
        case MOVE_RUNNER_HOME_STATE:
        	setState(OctilesState.CONFIRM_STATE);
        	break;
        	
        case PLAY_TILE_HOME_STATE:
        	if(c!=tilePool) { setState(OctilesState.MOVE_RUNNER_HOME_STATE); }
        	break;
        case PLAY_TILE_STATE:
			if(c!=tilePool) { setState(OctilesState.MOVE_RUNNER_STATE); }
			break;

        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(char col, int row)
    {
        return ((pickedSource!=null) 
        		&& (pickedSource.col == col) 
        		&& (pickedSource.row == row));
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterPick()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting pick in state %s", board_state);
        case CONFIRM_STATE:
        case DRAW_STATE:
        	setState(mustLeaveHome(whoseTurn)?OctilesState.PLAY_TILE_HOME_STATE:OctilesState.PLAY_TILE_STATE);
        	break;
        case PLAY_TILE_STATE:
        case PLAY_TILE_HOME_STATE:
			break;
        case PUZZLE_STATE:
            break;
        }
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
     	case GAMEOVER_STATE: 
    		break;

        case DRAW_STATE:
        	setGameOver(false);
        	break;
    	case CONFIRM_STATE:
    	case CONFIRM_PASS_STATE:
    	case PUZZLE_STATE:
    	case PASS_STATE:
    		setState(OctilesState.PLAY_TILE_STATE);
			//$FALL-THROUGH$
		case PLAY_TILE_HOME_STATE:
    	case PLAY_TILE_STATE:
    		setState(mustLeaveHome(whoseTurn)
    					?OctilesState.PLAY_TILE_HOME_STATE
    					: hasLegalMoves()
    						? OctilesState.PLAY_TILE_STATE
    						: OctilesState.PASS_STATE);
    		if(board_state==OctilesState.PASS_STATE)
    		{
    			//p1("pass here");
    		}
    		break;
    	}
    }
    OctilesPlay robot = null;
	public boolean p1(String msg)
	{
		if(G.p1(msg) && (robot!=null))
		{	String dir = "g:/share/projects/boardspace-html/htdocs/octiles/octilesgames/";
			robot.saveCurrentVariation(dir+msg+".sgf");
			return(true);
		}
		return(false);
	}

    
    private void doDone(boolean next)
    {	
        acceptPlacement();

        if (board_state==OctilesState.RESIGN_STATE)
        {	setGameOver(false);
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	if(win1)  { setGameOver(true); }
        	else {if(next) { setNextPlayer(); } setNextStateAfterDone(); }
        }
    }
    
    // this is used to mark the path associated with a move for the user interface.
    // if called wiht mark=false, it just tests that a particular move is legal
    boolean markValidMove(boolean mark,OctilesCell c,OctilesCell d,OctilesCell through)
    {	boolean val = false;
    	for(int i=0,lim=CELL_FULL_TURN; i<lim; i++)
    	{
    	OctilesCell next = c.exitTo(i);
    	if(next!=null)
    		{
    		val |= markPathFrom(mark,next,c.exitToEntry(i),d,through);	
    		}
    	}
    	return(val);
    }
    boolean isValidMove()
    {	return((droppedTileDest!=null)
    			&& (pickedSource!=null)
    			&& (droppedDest!=null)
    			&& markValidMove(false,pickedSource,droppedDest,droppedTileDest));
    }
    
    boolean isLoopMove()
    {	return((pickedSource==droppedDest) && pickedSource.isPostCell && isValidMove());
    }
    // doDone sets restrictive states where home runners have to move
    // but sometimes this isn't possible, so the state has to be relaxed.
    // sometimes no moves are available.
    void doExtendedDone()
    {	boolean hasMoves = hasLegalMoves();
    	if(!hasMoves)
     	{
    		switch(board_state)
    		{
    		case GAMEOVER_STATE: hasMoves = true; break;
    		case MOVE_RUNNER_HOME_STATE:
    			setState(OctilesState.MOVE_RUNNER_STATE);
    			hasMoves = hasLegalMoves();
    			break;
    		case PLAY_TILE_HOME_STATE:
    			setState(OctilesState.PLAY_TILE_STATE);
    			hasMoves = hasLegalMoves();
    			break;
			default:
				break;
    		}
    	if(!hasMoves) { setState(OctilesState.PASS_STATE); }
     	}
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	OctilesMovespec m = (OctilesMovespec)mm;

        //System.out.println("E "+m+" for "+whoseTurn);
        switch (m.op)
        {
        case MOVE_DONE:
        	// done, from the user interface
         	doDone(true);
         	doExtendedDone();
            break;
        case MOVE_ROTATE:
        	{
        	OctilesCell c = getCell(m.to_col,m.to_row);
        	c.rotation = m.rotation;
        	}
        	break;
        case MOVE_DROPB:
        	G.Assert(pickedObject!=null,"something is moving");
            {
            OctilesCell c = dropObject(OctilesId.TileLocation, m.to_col, m.to_row,m.rotation);
            if(replay==replayMode.Single)
            {
            	animationStack.push(pickedSource);
            	animationStack.push(c);
            }
            if((board_state!=OctilesState.PUZZLE_STATE) && (pickedSource==droppedDest) && !isLoopMove())
            	{ unDropObject(); 
            	  unPickObject(); 

            	} 
            	else
            		{
            		setNextStateAfterDrop(c);
            		}
            }
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  switch(board_state)
        		  {
        		  default: throw G.Error("Not expecting state %s",board_state);
        		  case PUZZLE_STATE: break;
        		  case CONFIRM_STATE:
        			  setState(mustLeaveHome(whoseTurn)?OctilesState.MOVE_RUNNER_HOME_STATE:OctilesState.MOVE_RUNNER_STATE);
        			  break;
        		  case MOVE_RUNNER_HOME_STATE:
        			  setState(OctilesState.PLAY_TILE_HOME_STATE);
        			  break;
        		  case MOVE_RUNNER_STATE: 
        			  setState(OctilesState.PLAY_TILE_STATE); 
        			  break;
        		  }
        		}
        	else 
        		{ pickObject(OctilesId.TileLocation, m.from_col, m.from_row);
          		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
            {
            OctilesCell c = dropObject(m.source, m.to_col, m.to_row,m.rotation);
            setNextStateAfterDrop(c);
            }

            break;
        case MOVE_PASS:
        	setState(board_state==OctilesState.CONFIRM_PASS_STATE 
        		? OctilesState.PLAY_TILE_STATE
        		: OctilesState.CONFIRM_PASS_STATE);
        	break;
        	
        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickObject(m.source, m.from_col, m.from_row);
            setNextStateAfterPick();
            break;

        case DROP_AND_MOVE:
        	// complete move, used by the robot
        	unDropObject();
        	unPickObject();
        	unDropObject();
        	unPickObject();
        	{
        	OctilesCell from = pickObject(OctilesId.TilePoolRect,'@',0);
        	// record the original tile, if any, and the original rotation for undo
        	OctilesCell loc = dropObject(OctilesId.TileLocation,m.drop_col,m.drop_row,m.rotation);
        	int rot = droppedTileSavedRotation;
        	if(loc.height()>1) { rot += loc.chipAtIndex(0).chipNumber()*100; }
        	m.saved_rotation = rot;
        	OctilesCell mfrom = pickObject(OctilesId.TileLocation, m.from_col, m.from_row);
        	OctilesCell mto = dropObject(OctilesId.TileLocation,m.to_col,m.to_row,0);
        	if(replay!=replayMode.Replay) 
        	{
        		animationStack.push(from);
        		animationStack.push(loc);
        		animationStack.push(mfrom);
        		animationStack.push(mto);
        	}
        	setState(OctilesState.CONFIRM_STATE);
        	}
        	
        	break;
        case MOVE_START:
            setWhoseTurn(m.player);
            setState(OctilesState.PUZZLE_STATE);
            doDone(false);
            doExtendedDone();
            break;

        case MOVE_RESIGN:
            setState(unresign==null?OctilesState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(OctilesState.PUZZLE_STATE);

            break;
        case MOVE_GAMEOVERONTIME:
      	   win[whoseTurn] = true;
      	   setState(OctilesState.GAMEOVER_STATE);
      	   break;

        default:
        	cantExecute(m);
        }


        return (true);
    }

    public boolean pickMatchesCell(OctilesChip picked,OctilesCell dest)
    {	switch(dest.rackLocation())
    	{
    	default: throw G.Error("not expecting rackLocation %s",dest.rackLocation);
    	case TileLocation:
    	case EmptyTileLocation:
    		if(dest.chipIndex>=0) { return(false); }	// can't stack onto the board
    	//$FALL-THROUGH$
    	case TilePoolRect:
    		return(picked.isTile);
    	case PostLocation:
    	case EmptyPostLocation:
    		return((dest.chipIndex<0) && picked.isRunner);
    	}
    }
    //
    // this version uses an extreme instance of depending on the move
    // generator.  Almost all the legality testing is based on filtering
    // the list of legal moves.
    //
    public boolean LegalToHitBoard(OctilesCell cell)
    {	
        switch (board_state)
        {
        case MOVE_RUNNER_STATE:
        case MOVE_RUNNER_HOME_STATE:
        	if(pickedObject==null)
        	{
        	if(isDest(cell)) { return(true); }
        	Hashtable<OctilesCell,OctilesCell> h = getSources();
        	return(h.get(cell)!=null);
        	}
        	else // something moving
        	{
        	if(isSource(cell)) { return(true); }
        	Hashtable<OctilesCell,OctilesMovespec> h = getDests(false);
        	return(h.get(cell)!=null);
        	}
 		case PLAY_TILE_STATE:
 		case PLAY_TILE_HOME_STATE:
 			if(cell==tilePool) { return(true); }
 			if(pickedObject!=null)
 			{
 			Hashtable<OctilesCell,OctilesMovespec> h  = getDests(true);
 		    return(isDest(cell) || (h.get(cell)!=null));
 			}
			//$FALL-THROUGH$
		case GAMEOVER_STATE:
		case RESIGN_STATE:
		case PASS_STATE:
		case CONFIRM_PASS_STATE:
			return(false);
		case CONFIRM_STATE:
		case DRAW_STATE:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
        	return(pickedObject==null
        			?(cell.chipIndex>=0)
        			:pickMatchesCell(pickedObject,cell));
        }
    }

 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(OctilesMovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone(true);
            }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(OctilesMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
        case MOVE_PASS:
            break;
        case DROP_AND_MOVE:
        	{	acceptPlacement();
        		pickObject(OctilesId.TileLocation, m.to_col, m.to_row);
       			dropObject(OctilesId.TileLocation, m.from_col,m.from_row,0);
       			pickObject(OctilesId.TileLocation,m.drop_col,m.drop_row);
      			dropObject(OctilesId.TilePoolRect,'@',0,0);
       			{	int tile = m.saved_rotation/100;
       				int rot = m.saved_rotation%100;
       				OctilesCell target = getCell(m.drop_col,m.drop_row);
       				if(tile>0)
       				{	OctilesChip ch = tilePool.removeChipAtIndex(0);
       					G.Assert(ch.chipNumber()==tile,"expected chip "+tile);
       					acceptPlacement();
       					addChip(target,ch,rot);
       				}
       				else { target.rotation = rot; }
       			}
        		acceptPlacement();
        	}
        	break;
        case MOVE_RESIGN:
            break;
        }
        setState(m.state);
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 /**
  * mark the strokes on a path starting at "from" entering from "fromDir".  This is 
  * called to flag cells for fancy display, so it only needs to be callable when the
  * test tile is actually in place on the board
  * 
  * @param from
  * @param fromDir
  */
 boolean markPathFrom(boolean mark,OctilesCell from,int fromDir,OctilesCell toCell,OctilesCell through)
 {
	 	OctilesChip nexttop = from.topChip();
		if(nexttop==null) { return(false); }
		int nextRot = from.rotation;
		int nextDir = nexttop.exitDirection(nextRot,fromDir);
		OctilesCell nextInPath = from.exitTo(nextDir);
		int nextEntry = from.exitToEntry(nextDir);	// special logic for next entry direction
		if(nextInPath==null) { return(false); } 	// fell off the board
		if(from==through) { through=null; }
		boolean val = (nextInPath.isPostCell)
				? ((nextInPath==toCell)&&(through==null))
				: markPathFrom(mark,nextInPath,nextEntry,toCell,through);
		
		if(val && mark)
			{
			from.markStroke(fromDir);
			from.markStroke(nextDir);
			}
	
	 	return(val);
 }
 // clear the path markings which cause them to be drawn in red
 void clearMarkedPaths()
 {	for(OctilesCell c = allTileCells; c!=null; c=c.next)
 	{	c.markStroke(-1);
 	}
 }
 
 // small ad-hoc adjustment to the grid positions
 public void DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
 {   if(Character.isDigit(txt.charAt(0)))
	 	{ xpos+=cellsize/2;
	 	  if("1".equals(txt)) { ypos+= cellsize/4; }
	 	}
 		else { ypos += cellsize/5; }
 	GC.Text(gc, false, xpos, ypos, -1, 0,clt, null, txt);
 }

 
 // return a post cell if the path from here eventually passes through a specified
 // cell.  Note that it may pass through more than once, but is guaranteed not to loop
 // except possibly back to the original post cell.
 OctilesCell validPathFrom(int depth,OctilesChip runner,OctilesCell from,int fromdir,
		 		OctilesCell through,boolean satisfied,OctilesChip top,int rot)
 {	//if(depth>=100) { return(null); }
 	if(depth>=100)
 		{
 		addChip(through,top,rot);	// help the visualization
 		G.Assert(depth<100,"nonrecursive");
 		}
 	OctilesChip nexttop = (from==through) ? top : from.topChip();
	if(nexttop==null) { return(null); }
	int nextRot = (from==through) ? rot : from.rotation;
	int nextDir = nexttop.exitDirection(nextRot,fromdir);
	OctilesCell nextInPath = from.exitTo(nextDir);
	int nextEntry = from.exitToEntry(nextDir);	// special logic for next entry direction
	satisfied |= (from==through);
	if(nextInPath==null) { return(null);} 	// fell off the board
	if(nextInPath.isPostCell)
		{ // got to a post cell, if we've satisfied the "through" condition we're done.
		  // otherwise we fail.
		if(!satisfied)
			{ return(null); }					// must be through the new tile
		if(nextInPath.homeForColor==runner)
			{ return(null); }	// can't be our home cell
		if((nextInPath.goalForColor!=null)&&(nextInPath.goalForColor!=runner)) 
			{ return(null); }	// someone else goal
		return(nextInPath); 
		}
 	return(validPathFrom(depth+1,runner,nextInPath,nextEntry,through,satisfied,top,rot));
 }
 boolean getListOfRunnerMoves(
		 OctilesChip forRunner,	// for this player
		 CommonMoveStack  all,			// vector or results
		 OctilesCell first,		// runner in this cell
		 OctilesChip runner,	// with this runner
		 OctilesCell through,	// pass through this cell
		 OctilesChip top,		// with this cell as the top
		 int whichRotation)			// and this rotation
 {		G.Assert(first.isPostCell
		 			&& runner.isRunner
		 			&& through.isTileCell 
		 			&& top.isTile,"expected actors");
 		boolean some = false;
 		for(int dir=0;dir<CELL_FULL_TURN;dir++)
 		{
 		OctilesCell from = first.exitTo(dir);
 		if(from!=null)
 			{
 			OctilesCell dest = validPathFrom(1,runner,from,(dir+4)%8,through,false,top,whichRotation);
 			if((dest!=null) && ((dest==first)||(dest.topChip()==null)))
 			{	
 				if(all==null) { return(true); }
 				some = true;
 				all.addElement(new OctilesMovespec(through.col,through.row,top.chipNumber(),whichRotation,
 							first.col,first.row,dest.col,dest.row,getPlayerIndex(forRunner)));
 				
 			}}
 		}
 		return(some);
 }
 /**
  * get a list of runner moves from a specific cell
  * 
  * @param chipIndex
  * @param homeflag true if restricted to moves that vacate a home cell
  * @param all
  * @param r a runner cell
  * @param c a tile cell
  * @param place the placed tile
  * @param whichRotation the placed rotation
  */
 boolean getListOfRunnerMoves(OctilesChip forRunner,boolean homeFlag,CommonMoveStack  all,
		 OctilesCell r,OctilesCell c,OctilesChip place,int whichRotation)
 {	OctilesChip runner = r.topChip();
 	boolean some = false;
 	if((runner==forRunner)
 			&& (!homeFlag || r.homeForColor==forRunner) 
 			&& (r.goalForColor!=runner))
 	{
	 if(whichRotation>=0) 
	 {  some |=getListOfRunnerMoves(forRunner,all,r,runner,c,place,whichRotation);
	    if(all==null && some) { return(true); }
	 }
	 else
	 {
	 for(int rot=0;rot<8;rot++)
	 {	
		 some |= getListOfRunnerMoves(forRunner,all,r,runner,c,place,rot);
		 if(all==null && some) { return(true); }
	 }}}
 	return(some);
 }
 /**
  *  get a list of moves with tile "place" on cell "c" and any rotation
  * @param forRunner
  * @param homeFlag true if restricted to moves that vacate a home square 
  * @param all
  * @param c
  * @param place
  * @param rot rotation, or -1 for any
  */
 boolean getListOfRunnerMoves(OctilesChip forRunner,boolean homeFlag,CommonMoveStack  all,
		 OctilesCell c,OctilesChip place,int rot)
 {	boolean some = false;
 	for(OctilesCell r=allCells; r!=null; r=r.next)
		 {	some |=getListOfRunnerMoves(forRunner,homeFlag,all,r,c,place,rot);
		 	if(some && (all==null)) { return(true); }
		 }
 	return(some);
 }
 /**
  * get a list of moves that would put chip "place" anyplace
  * @param forRunner the runner type to move
  * @param homeFlag if restricted to moves that vacate a home square
  * @param all the vector of results
  * @param place the tile to place
  */
 boolean getListOfTileMoves(OctilesChip forRunner,boolean homeFlag,CommonMoveStack  all,OctilesChip place)
 {	return getListOfTileMoves(forRunner,homeFlag,all,place,-1);
 }
 
 boolean getListOfTileMoves(OctilesChip forRunner,boolean homeFlag,CommonMoveStack  all,OctilesChip place,int rot)
 {	boolean some = false;
	 for(OctilesCell c = allTileCells; c!=null; c=c.next)
	 {	some |= getListOfRunnerMoves(forRunner,homeFlag,all,c,place,rot);
	 	if(some && (all==null)) { return(true); }
	 }
	 return(some);
 }
 //
 // return true if there are any legal moves.  This is mainly for
 // the benefit of the user interace, so the user get a nice "pass"
 // state.  It's not intended to be particularly effecient.
 //
 public boolean hasLegalMoves()
 {	
	 return getListOfMoves(null,whoseTurn);
 }
 
 // get moves for the robot
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
    int who = whoseTurn;
    getListOfMoves(all,who);
    if(all.size()==0) { all.addElement(new OctilesMovespec(PASS,who)); }
    return(all);
 }
 private boolean getListOfMoves(CommonMoveStack all, int who)
 {
    OctilesChip forRunner = playerRunner[who];
    boolean homeFlag = false;
    boolean some = false;
    switch(board_state)
    {
    default: throw G.Error("Not expecting state %s",board_state);
    case PUZZLE_STATE:
    case GAMEOVER_STATE:
    	break;
    case CONFIRM_PASS_STATE:
    	if(all!=null) { all.push(new OctilesMovespec(MOVE_PASS,who)); }
		//$FALL-THROUGH$
	case CONFIRM_STATE:
    case DRAW_STATE:
    	some = true;
    	if(all!=null) { all.push(new OctilesMovespec(MOVE_DONE,who)); }
    	break;
    case PLAY_TILE_HOME_STATE:
    	homeFlag = true;
		//$FALL-THROUGH$
	case PLAY_TILE_STATE:
    	some |= getListOfTileMoves(forRunner,homeFlag,all,tilePool.topChip());
    	break;
    case MOVE_RUNNER_HOME_STATE:
    	homeFlag = true;
		//$FALL-THROUGH$
	case MOVE_RUNNER_STATE:
    	some |= getListOfRunnerMoves(forRunner,homeFlag,all,
    			droppedTileDest,droppedTile,droppedTileDest.rotation);
		break;
	case PASS_STATE:
    	;
    }
    return(some);
 	}
}
