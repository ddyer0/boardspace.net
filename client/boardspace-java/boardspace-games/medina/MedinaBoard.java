package medina;

import online.game.*;
import online.game.cell.Geometry;

import java.util.*;

import lib.*;
/**
 * todo: check drop error
 * todo: fix lobby translations
 */
import lib.Random;
import medina.MedinaChip.DomeColor;
import medina.MedinaChip.PalaceColor;

/** MedinaBoard knows all about the game of Medina.
 * It gets a lot of logistic support from 
 * common.rectBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
 *  
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of trip wires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

class MedinaBoard extends rectBoard<MedinaCell> implements BoardProtocol,MedinaConstants
{	static final int REVISION = 104;			// revision 100 implements the "all domes placed" rule
												// revision 101 implements v2 rules and actually removing the surplus palaces
												// revision 102 implements the correct restriction on staring a new merchant line
												// revision 103 adds points for meeples adjacent to stables
												// revision 104 fixes selection of unused color for neutral domes
	static final int MAX_PLAYERS = 4;		// game is for 3-4
	static final int DEFAULT_COLUMNS = 18;	// standard medina board
	static final int DEFAULT_ROWS = 13;		// standard medina board
	static final int P2_COLUMNS = 16;
	static final int P2_ROWS = 12;
	static final int TEACARDCOUNT = 6;
    // setup parameters
    static final int MAX_INITIAL_PALACES = 9;
	static final int nInitialPalaces[]={8,6,5};
	static final int nInitialStables[]={4,4,3};
	static final int nInitialMeeples[]={12,8,6};
	static final int nInitialWalls[]={15,10,8};
	static final int nTowerMeeples[] = { 3,2,1,0};
	static final int nInitialWalls_v2[] = {15,12,9};
    static final String[] MEDINAGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers



	public int getMaxRevisionLevel() { return(REVISION); }
	public Variation variation = Variation.Medina_V1;
    public int boardColumns = DEFAULT_COLUMNS;	// size of the board
    public int boardRows = DEFAULT_ROWS;
    public void SetDrawState() { throw G.Error("shouldn't be possible");  }
	private MedinaState unresign;
	private boolean resigned = false;
 	private MedinaState board_state;
 	private int robotUndo = 0;
 	private DomeColor playerColor[] = new DomeColor[4];
 	private int playerOwning(DomeColor co)
 	{
 		for(int i=0;i<playerColor.length;i++) { if(playerColor[i]==co) { return(i); }}
 		// this is legitimate in the 2 player game, where an imaginary third
 		// player can "own" a cluster.
 		return(-1);
 	}
 	private IStack robotStack=new IStack();
 	private StateStack robotState = new StateStack();
 	
	public MedinaState getState() {return(board_state); }
	public CellStack animationStack = new CellStack();
	
	public void setState(MedinaState st) 
	{ 	unresign = (st==MedinaState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			  resigned = false;
			}
	}
    public MedinaCell getCell(MedinaCell c)
    {	if(c==null) { return(null); }
    	if(c.onBoard) { return(getCell(c.col,c.row)); }
    	return(getCell(c.rackLocation(),c.col,c.row));
    }
    public MedinaCell unownedPalaces;
    public MedinaCell unownedTowers;
    //
    // per player arrays.  These are all size by players_in_game
    public MedinaCell palaces[][] = null;
    public MedinaCell domes[] = null;
    public MedinaCell meeples[] = null;
    public MedinaCell walls[] = null;
    public MedinaCell stables[] = null;
    public MedinaCell cards[] = null;		// cards owned
    public MedinaCell teaCards[] = null;
    public MedinaCell neutralDomes[] = null;
    public MedinaCell towerMerchants[] = null;    
    public int score[] = null;

    // true if all the palaces of this type have been placed.
    public boolean allDomesPlaced(MedinaChip type,Cluster willBeClaimed,MedinaChip claimedBy)
    {	int claimed = 0;
    	if(revision<100) { return(false); }
    	buildClusters();
    	for(Cluster cl = palace_clusters; cl!=null; cl = cl.next)
    	{
    		if( ((cl==willBeClaimed) && (playerOwning(claimedBy.domeColor())>=0))
    				|| (((cl.type==type)&&cl.claimed) &&playerOwning(cl.owner())>=0))
    		{
    			claimed++;
    		}
    	}
    	return(claimed==players_in_game);
    }
    public String gameType() { return(gametype+" "+revision+" "+players_in_game+" "+randomKey); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public MedinaChip pickedObject = null;
    public MedinaCell lastDroppedDest = null;	// this is used in the size and positioning UI for chips
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    private IStack undoStack = new IStack();	// undo information for tea tiles and palaces during the incomplete move
    
    private MedinaChip[] towerOwner = new MedinaChip[4];			// tower tile owners
    private int piecesOnBoard = 0;
    private Cluster palace_clusters = null;
    private Cluster wall_clusters = null;
    private Cluster meeple_clusters = null;
    private boolean clustersValid = false;
    private int sweep_counter = 0;
    private char well_col;
    private int well_row;
    private Cluster bigPalace[] = new Cluster[4];	// largest palace of each color (not per player)
    private MedinaCell[] towerClaimer = new MedinaCell[4];	// cell which last claimed this tower
    private int tileScore[] = new int[4];		// value of currently claimed tiles
    private int edgeScore[] = new int[4];		// number of adjacent edge spaces
    private int claimedPalaces[] = new int[4];	// total of claimed palaces of each color
    private int placedPalaces[] = new int[4];	// placed but unclaimed palaces of each color
    private int nClaimedPalaces[] = new int[4];	// number of claimed palaces of each color
    private int owned_palaces[] = new int[4];	// mask of palaces claimed per player
    
    public MedinaCell teaPool = null;
    public MedinaCell teaDiscards = null;
    public MedinaCell trash = null;
    public boolean meeplesOnBoard()
    {	
    	for(MedinaCell c=allCells; c!=null; c=c.next)
    	{	MedinaChip top = c.topChip();
    		if((top!=null) && top.isMeeple()) { return(true); } 
    	}
    	return(false);
    }
	// factory method
	public MedinaCell newcell(char c,int r)
	{	return(new MedinaCell(c,r));
	}
    public MedinaBoard(String init,long randomv,int players,int[]colors,int rev) // default constructor
    {   drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = MEDINAGRIDSTYLE; //coordinates left and bottom
    	setColorMap(colors, players);
        doInit(init,randomv,players,rev); // do the initialization 
     }

    public MedinaBoard cloneBoard() 
	{ MedinaBoard dup = new MedinaBoard(gametype,randomKey,players_in_game,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((MedinaBoard)b); }
    
    // true if this col,row is adjacent to the well
    public boolean adjacentToWell(char meeple_col,int meeple_row)
    {
    	return(((Math.abs(meeple_col-well_col)<=1) && (Math.abs(meeple_row-well_row)<=1)));
    }
    private int getUnusedColor(int map[])
    {	int usedColors = 0;
    	for(int i=0,lim=(revision>=104?players_in_game:map.length);i<lim;i++) { usedColors |= 1<<map[i]; }
    	usedColors = ~usedColors;		// unused colors
    	int unusedColor = usedColors & -usedColors;	// least 1 bit
    	int n = G.numberOfTrailingZeros(unusedColor);
    	return(Math.min(MAX_PLAYERS-1,n));
    }
    // standard initialization for Medina. 
    private void Init_Standard(String game,int num)
    { 	Variation v = Variation.find(game);
    	animationStack.clear();
    	if(v!=null)
    	{
    		variation = v;
    	   	gametype = game;
       	}
    	else {  throw G.Error(WrongInitError,game);
    	}
    	
		boolean v2 = variation==Variation.Medina_V2;
		if(!v2) { num = Math.max(num,3); }	// at least 3 players for v1

        players_in_game = num;
        robotUndo = 0;
        robotStack.clear();
    	boardColumns=(players_in_game==2) ? P2_COLUMNS : DEFAULT_COLUMNS; 
    	boardRows = (players_in_game==2) ? P2_ROWS : DEFAULT_ROWS;
 
        win = new boolean[num];
        palaces = new MedinaCell[num][4];
        domes = new MedinaCell[num];
        meeples = new MedinaCell[num];
        walls = new MedinaCell[num];
        stables = new MedinaCell[num];
        cards = new MedinaCell[num];
        score = new int[num];
        teaCards = new MedinaCell[num];
        neutralDomes = new MedinaCell[num];
        towerMerchants = new MedinaCell[4];
        Random r = new Random(2346467);
        teaPool = new MedinaCell(r , MedinaId.TeaPoolLocation,0,0,MedinaChip.TEA_BACK);
        teaDiscards = new MedinaCell(r , MedinaId.TeaDiscardLocation,0,0,MedinaChip.TEA);
        trash = new MedinaCell(r,MedinaId.Trash,0,0,null);
        if(v2) { for(int i=0;i<TEACARDCOUNT;i++) { teaPool.addChip(MedinaChip.TEA_BACK); } }
        setState(MedinaState.PUZZLE_STATE);
        reInitBoard(boardColumns,boardRows); //this sets up the board and cross links
	    invalidateClusters();
	    pickedSourceStack.clear();
	    droppedDestStack.clear();
	    stateStack.clear();
	    undoStack.clear();
	    pickedObject=null;
         
        {
        char last = (char)('A'+ncols-1);
        getCell('A',1).addChip(MedinaChip.TOWER);
        getCell(last,1).addChip(MedinaChip.TOWER);
        getCell(last,lastRowInColumn(last)).addChip(MedinaChip.TOWER);
        getCell('A',lastRowInColumn('A')).addChip(MedinaChip.TOWER);
        piecesOnBoard = 4;
        }
        int[] map = getColorMap();
        AR.setValue(playerColor, null);
        for(int playerIndex=0;playerIndex<players_in_game;playerIndex++)
        {
        for(int j=0;j<4;j++)
        { 
            MedinaCell tm = towerMerchants[j] = new MedinaCell(r,MedinaId.TowerMerchantLocation,j,j,MedinaChip.MEEPLE);
            if(variation==Variation.Medina_V2)
        	{ for(int i=nTowerMeeples[j];i>0;i--) { tm.addChip(MedinaChip.MEEPLE); }
        	}
          
          // 5 or 6 palaces in each of 4 colors
          int initialPalaces = nInitialPalaces[players_in_game-2];
          MedinaChip sto = MedinaChip.getPalace(j);
          MedinaCell d = new MedinaCell(r,MedinaId.PalaceLocation,playerIndex,j,sto);
          palaces[playerIndex][j] = d;
           for(int n=0;n<initialPalaces;n++)
        	   {d.addChip(sto);
        	   }
        }
        // one dome in each of 4 colors
        {
        MedinaChip sto = MedinaChip.getDome(map[playerIndex]);
        playerColor[playerIndex] = sto.domeColor();
        MedinaChip stt = MedinaChip.TEA;
        MedinaCell c = new MedinaCell(r,MedinaId.DomeLocation,playerIndex,0,sto);
        domes[playerIndex] = c;
        for(int n=0;n<4;n++) { c.addChip(sto);}
        
        // add neutral domes for 3 player game
        MedinaChip std = MedinaChip.getDome(getUnusedColor(map));
        MedinaCell d = neutralDomes[playerIndex] = new MedinaCell(r,MedinaId.NeutralDomeLocation,playerIndex,0,std);
        teaCards[playerIndex] = new MedinaCell(r,MedinaId.TeaCardLocation,playerIndex,0,stt);
        if(v2)
        	{
        	if(players_in_game<=3)
        		{
        		d.addChip(std);
        		if(players_in_game==2) { d.addChip(std); }
        		}
        	}
        }
        // 3 or 4 stables
        MedinaCell e = new MedinaCell(r,MedinaId.StableLocation,playerIndex,0,MedinaChip.STABLE);
        stables[playerIndex] = e;
        int nStables = nInitialStables[players_in_game-2];
        for(int n=0;n<nStables;n++)
        	{ e.addChip(MedinaChip.STABLE);
        	}
        
        // empty card rack
        MedinaCell cc = new MedinaCell(r,MedinaId.CardLocation,playerIndex,0,null);
        cards[playerIndex] = cc;
         
        // initial walls
        MedinaCell f = new MedinaCell(r,MedinaId.WallLocation,playerIndex,0,MedinaChip.H_WALL);
        walls[playerIndex] = f;
        int nWalls = v2 ? nInitialWalls_v2[players_in_game-2] : nInitialWalls[players_in_game-2];
        for(int n=0;n<nWalls; n++)
        	{ f.addChip(MedinaChip.H_WALL);
        	}
        // initial meeples
        MedinaCell g = new MedinaCell(r,MedinaId.MeepleLocation,playerIndex,0,MedinaChip.MEEPLE);
        meeples[playerIndex] = g;
        int nMeeples =nInitialMeeples[players_in_game-2];
        for(int n=0;n<nMeeples;n++)
        	{	g.addChip(MedinaChip.MEEPLE);
        	}
        // one extra meeple for the first player in v1
        if(!v2 && (playerIndex==0)) { g.addChip(MedinaChip.MEEPLE); }
        }
        switch(variation)
        {
        default: throw G.Error("Not expecting %s",variation);
        case Medina_V1: 
        	well_col=(char)0;
        	well_row=0;
        	break;
        case Medina_V2:
        	{
        	Random R = new Random(randomKey);
        	// randomly place the well
        	well_col = (char)('C'+Random.nextInt(R,boardColumns-4));
        	well_row = 3+Random.nextInt(R,boardRows-4);
        	piecesOnBoard++;
        	getCell(well_col,well_row).addChip(MedinaChip.WELL);
        	// randomly place the first meeple
        	char meeple_col;
        	int meeple_row;
        	do
        		{
        		meeple_col = (char)('C'+Random.nextInt(R,boardColumns-4));
        		meeple_row = 3+Random.nextInt(R,boardRows-4);
        		}
        		while((meeple_col==well_col) && (meeple_row==well_row));
        	piecesOnBoard++;
        	getCell(meeple_col,meeple_row).addChip(MedinaChip.MEEPLE);
        	}
        }
        unownedPalaces = new MedinaCell(r);
        unownedTowers = new MedinaCell(r);
        whoseTurn = FIRST_PLAYER_INDEX;
        AR.setValue(win,false);
        moveNumber = 1;

    }

    public void sameboard(BoardProtocol f) { sameboard((MedinaBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(MedinaBoard from_b)
    {
    	super.sameboard(from_b);
       	G.Assert(sameCells(domes,from_b.domes),"domes mismatch");
       	G.Assert(sameCells(stables,from_b.stables),"stables mismatch");
    	G.Assert(sameCells(meeples,from_b.meeples),"meeples mismatch");
    	G.Assert(sameCells(palaces,from_b.palaces),"palaces mismatch");
    	// cards may not match, depending on state of clustersValid
    	// G.Assert(sameCells(cards,from_b.cards),"cards mismatch");
      	G.Assert(sameCells(teaCards,from_b.teaCards),"tea cards mismatch");
    	G.Assert(sameCells(walls,from_b.walls),"meeples mismatch");
       	G.Assert(sameCells(towerMerchants,from_b.towerMerchants),"tower merchant mismatch");
       	G.Assert(sameCells(neutralDomes,from_b.neutralDomes),"neutral domes mismatch");
       	G.Assert(sameCells(towerMerchants,from_b.towerMerchants),"tower merchants mismatch");
       	G.Assert(sameCells(walls,from_b.walls),"walls mismatch");
        G.Assert(variation==from_b.variation,"variation mismatch");
        G.Assert((piecesOnBoard==from_b.piecesOnBoard), "piecesOnBoard not the same");
        G.Assert(well_col==from_b.well_col,"well col mismatch");
        G.Assert(well_row==from_b.well_row,"well row mismatch");
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
    {	buildClusters();
    	Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);
        v ^= Digest(r,domes);
        v ^= Digest(r,stables);
        v ^= Digest(r,meeples);
        v ^= Digest(r,palaces);
       //v ^= Digest(cards);
        v ^= Digest(r,teaCards);
        v ^= Digest(r,teaPool);
        v ^= Digest(r,teaDiscards);
        v ^= Digest(r,neutralDomes);
        v ^= Digest(r,towerMerchants);
        v ^= Digest(r,walls);
        v ^= Digest(r,(variation.ordinal()+1));
		MedinaChip po = pickedObject;
		if(po==MedinaChip.V_WALL) { po = MedinaChip.H_WALL; }
		v ^= chip.Digest(r,po);
		//v ^= cell.Digest(r,getSource());
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
         return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(MedinaBoard from_b)
    {	
    	super.copyFrom(from_b);
    	revision = from_b.revision;
        piecesOnBoard = from_b.piecesOnBoard;
        copyFrom(domes,from_b.domes);
        copyFrom(stables,from_b.stables);
        copyFrom(meeples,from_b.meeples);
        copyFrom(palaces,from_b.palaces);
        //copyFrom(cards,from_b.cards);
        copyFrom(teaCards,from_b.teaCards);
        copyFrom(teaPool,from_b.teaPool);
        copyFrom(teaDiscards,from_b.teaDiscards);
        copyFrom(neutralDomes,from_b.neutralDomes);
        copyFrom(towerMerchants,from_b.towerMerchants);   
        copyFrom(walls,from_b.walls);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        stateStack.copyFrom(from_b.stateStack);
        copyFrom(undoStack,from_b.undoStack);
        variation = from_b.variation;
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        resigned = from_b.resigned;
        well_col = from_b.well_col;
        well_row = from_b.well_row;
        pickedObject = from_b.pickedObject;
        sameboard(from_b);
        G.Assert(Digest()==from_b.Digest(),"digest mismatch");
    }


    // scan to find the extent of meeples
    private void addAdjacentMeeple(Cluster cluster,MedinaCell c)
    {	if(c.sweep_counter==sweep_counter) { return; }
    	if(c.isEdgeCell()) { return; }
    	MedinaChip chip = c.chipAtIndex(0);
    	if(chip==null) 	{ 	return;	}
    	else if(chip.isMeeple())
    	{	c.sweep_counter = sweep_counter;
        	c.cluster = cluster;
        	cluster.size++;
        	
			for(int i=0,dir=CELL_RIGHT;i<4;i++,dir+=2)
			{	addAdjacentMeeple(cluster,c.exitTo(dir));
			}
   	}
   }
    private void expandAdjacentMeeple(Cluster cluster,MedinaCell c)
    {	if(c.sweep_counter==sweep_counter) { return; }
    	if(c.cluster!=cluster) { return; }
     	int nadj = 0;
   		c.sweep_counter = sweep_counter;
		for(int i=0,dir=CELL_RIGHT;i<4;i++,dir+=2)
			{	MedinaCell adj = c.exitTo(dir);
				MedinaChip top = adj.chipAtIndex(0);
				expandAdjacentMeeple(cluster,adj);
				if((top!=null) && top.isMeeple()) { nadj++; }
			}
		if(nadj<=1) 
			{
				for(int i=0,dir=CELL_RIGHT;i<4;i++,dir+=2)
				{	MedinaCell adj = c.exitTo(dir);
					if((adj!=null) && !adj.isEdgeCell() && (adj.sweep_counter!=sweep_counter) && (adj.chipAtIndex(0)==null))
					{	adj.sweep_counter=sweep_counter;
						int nadj2 = 0;
						for(int j=0,dir2=CELL_RIGHT;j<4;j++,dir2+=2)
						{	MedinaCell adj2 = adj.exitTo(dir2);
							MedinaChip top2 = (adj2==null) ? null : adj2.chipAtIndex(0);
							if((top2!=null) && top2.isMeeple()) { nadj2++; }
						}
						if(nadj2<=1)
						{
						cluster.addExpansionCell(adj);
						}
					}
				}
				
			}
    }

    private void buildMeepleCluster(MedinaCell c,MedinaChip base)
    {	Cluster meeple = new Cluster(c,base);
    	addAdjacentMeeple(meeple,c);		// dir won't be used
    	meeple.next = meeple_clusters;
    	meeple_clusters = meeple; 
    }
    
    private void addAdjacentPalace(Cluster cluster,MedinaCell c,boolean expand)
    {	if(c.sweep_counter==sweep_counter) { return; }
		MedinaChip chip = c.chipAtIndex(0);
    	if(c.isEdgeCell()) 
    	{ if(expand)
    		{
    		if((chip!=null)&&chip.isWall())
    		{ cluster.wall_size++;
    		  cluster.addWallCluster(c);
    		}
    		else { cluster.edge_size++; }
    		}
    	}
    	else if(chip==null) 
    		{ 
    		if(expand)
    			{	boolean fail = adjacentToWell(c.col,c.row);
    				c.sweep_counter = sweep_counter;

    				for(int dir=0;!fail && (dir<8);dir++) 
    				{
    				MedinaCell adj = c.exitTo(dir);
    				if((adj!=null) 
    						&& !adj.isEdgeCell() 
    						&& (adj.cluster!=cluster))
    						{
    						MedinaChip top = adj.chipAtIndex(0);
    						if((top!=null) && top.isPalaceComponent()) { fail=true; }
    						}
    				}
    				if(!fail)
    				{ G.Assert(!c.isEdgeCell(),"away from edge");
    				  cluster.addExpansionCell(c); 
    				}
    			}
    		}
    	else if(chip.isMeeple()) { if(expand) { cluster.addMeepleCell(c); }}
    	else if((chip==cluster.type) || chip.isStable())
    	{	MedinaChip top = c.topChip();
    		if(top.isDome()) 
    			{ cluster.claimed = true; 
    			  cluster.clusterOwner = top; 
    			  cluster.seed = c;		// make it the seed too
    			}
        	c.sweep_counter = sweep_counter;
        	c.cluster = cluster;
	    	if(expand)
	    		{	// expansion pass, note the wall claimants
	    		int wall = c.moveWalled;
	    		if(wall>0)
	    			{	for(Cluster wallCluster = wall_clusters; wallCluster!=null; wallCluster=wallCluster.next)
	    				{	MedinaCell tower = wallCluster.tower;
	    					int num = (1<<tower.towerNumber())&wall;
	    					if(((num&wall)!=0) 
	    						&& ( (wallCluster.wallClaimer==null) || (wall>wallCluster.wallClaimer.moveWalled)))
	    					{	wallCluster.wallClaimer = c;
	    					}
	    				}
	    			}
	    		}
	    	else { // first pass, count the cluster
	    			cluster.size++; 
	            	cluster.moveExpanded = Math.max(cluster.moveExpanded,c.movePlaced);

	    			if(chip==cluster.type)
	    				{ cluster.palace_size++; }
	    				else
	    				{ cluster.stable_size++; 
	    				}
	    		}
	    	if(chip==cluster.type)
	    	{
			for(int i=0,dir=CELL_RIGHT;i<4;i++,dir+=2)
			{	MedinaCell adj = c.exitTo(dir);
				G.Assert(adj!=null,"on board");
				addAdjacentPalace(cluster,adj,expand);
			}}
	    	else if(expand)
	    	{	// expansion pass for stables, not walls adjacent to stables
	    		for(int i=0,dir=CELL_RIGHT;i<4;i++,dir+=2)
				{	
	    		MedinaCell adj = c.exitTo(dir);
	    		if(revision>=103) { 
	    			MedinaChip ch = adj.topChip();
	    			if((ch!=null) && ch.isMeeple()) { cluster.addMeepleCell(adj) ;}
	    		}
	        	if(adj.isEdgeCell()) 
	        	{ 	MedinaChip adjtop = adj.chipAtIndex(0);
	        		if((adjtop!=null)&&adjtop.isWall())
	        		{ cluster.wall_size++;
	        		  cluster.addWallCluster(adj);
	        		}
	        	
	        	}
				}
	
	    	}
    	}
    }
    private void buildPalaceCluster(MedinaCell c,MedinaChip base)
    {
    	Cluster palace = new Cluster(c,base);
    	addAdjacentPalace(palace,c,false);		// dir won't be used
    	palace.next = palace_clusters;
    	palace_clusters = palace; 
    	//G.print(""+palace);
    }
    private void buildWallClusters(MedinaCell c)
    {
    	for(int nd=0,dir=CELL_UP; nd<4; nd++,dir+=2)
    	{	MedinaCell adj = c.exitTo(dir);
    		c.sweep_counter = sweep_counter;
    		if(adj!=null)
    		{
    		Cluster cl = new Cluster(adj,MedinaChip.H_WALL);
    		cl.tower = c;
    		cl.next = wall_clusters;
    		wall_clusters = cl;
    		while(adj!=null)
    		{
	    		MedinaChip top = adj.topChip();
	    		MedinaCell adj2= adj.exitTo(dir);
	   			if(top==null)
	   			{ if(adj2.topChip()==null) { cl.addExpansionCell(adj); }
	   			  adj2=null;  
	   			}
	    		else
	    		{	adj.cluster = cl;
	    			adj.sweep_counter = sweep_counter;
	    			int walled = adj.moveWalled;
	    			if(walled>0 && ((cl.wallClaimer==null)||(cl.wallClaimer.moveWalled<walled)))
	    			{	// this wall cell was placed and adjacent to a palace, which therefore
	    				// claims the wall card.  
	    				cl.wallClaimer = adj;
	    			}
	    			cl.size++;
	    		}
	    		adj = adj2;
	     		}
    		}
    	}
    }

    private void invalidateClusters()
    {
    	clustersValid=false;
    	palace_clusters=null;
    	meeple_clusters = null;
    	wall_clusters = null;
    }
    public void buildClusters()
    {
    	if(!clustersValid)
    	{
    	int pob = 0;
     	sweep_counter++;
     	for(int i=0;i<bigPalace.length;i++) 
     		{ bigPalace[i]=null; 
       		  towerClaimer[i]=null;
       		  towerOwner[i]=null;
       		  claimedPalaces[i]=0;
       		  nClaimedPalaces[i]=0;
       		  placedPalaces[i]=0;
      		} 

     	reInit(cards);
     	AR.setValue(score,0);
     	AR.setValue(owned_palaces,0);
     	AR.setValue(tileScore,0);
     	AR.setValue(edgeScore,0);
     	//
    	// the first pass builds a cluster for each palace, wall, and meeple
    	//
    	for(MedinaCell c=allCells; c!=null; c=c.next)
    	{	pob+= c.chipIndex+1;		// count pieces on board	
    		if(c.sweep_counter!=sweep_counter)
    		{	MedinaChip top = c.chipAtIndex(0);	// bottom chip, not top chip
    			c.cluster = null;
    			if(top==null) { }	// empty cell
    			else if(top.isWall()) {  ;  } 	// walls need no explicit maintenance
    			else if(top.isTower()) { buildWallClusters(c); } 	// nor towers
    			else if(top.isStable()) { }	// nor stables
    			else if(top.isMeeple()) { buildMeepleCluster(c,top); }
    			else if(top.isPalace()) { buildPalaceCluster(c,top); }
    			else if(top.isWell()) { }
    			else 
    			{// domes should not be at level 0 of the board
    				throw G.Error("shouldn't get here");
    			}
     		}
    	}
     	G.Assert(pob==piecesOnBoard,"pieces on board "+piecesOnBoard+" matches actual count "+pob);
    	// 
    	// find expansion space for the palace clusters, this also
    	// locates the walls and finds the biggest palace of each color
    	//

    	sweep_counter++;  
    	for(Cluster cl = palace_clusters; cl!=null; cl=cl.next)
    	{	addAdjacentPalace(cl,cl.seed,true);
    		if(cl.claimed) 
    		{	// maintain the biggest cluster bonus
    			PalaceColor palaceColor = cl.palaceColor();
    			int colorIndex = palaceColor.ordinal();
    			G.Assert(cl.seed.movePlaced>0,"someone claimed this cluster");
    			if(colorIndex>=0)
    			{
    			if((bigPalace[colorIndex]==null)
    				|| (bigPalace[colorIndex].size<cl.size) 
    				|| ((bigPalace[colorIndex].size==cl.size) 
    						&& (bigPalace[colorIndex].moveExpanded>cl.moveExpanded)))
    			{	bigPalace[colorIndex]=cl;
    			}}
    				
    		}
    	}
    	sweep_counter++;
    	// find the expansion spaces for meeples
    	for(Cluster cl = meeple_clusters; cl!=null; cl=cl.next)
    	{	expandAdjacentMeeple(cl,cl.seed);
    	}
    	
     	unownedPalaces.reInit();
    	// score the big palaces and award the palace cards
    	for(int i=0;i<bigPalace.length;i++)
    	{	// add the palace cards to appropriate players
    		Cluster cl = bigPalace[i];
    		MedinaChip palace = MedinaChip.getPalaceCard(i);
    		boolean owned = false;
    		if(cl!=null)
    		{	int own = playerOwning(cl.owner());
    			if(own>=0) 
    			{	// 3 player v2 games place 4'th domes as "neutral"
    			owned = true;
    			cards[own].addChip(palace);
    			score[own] += i+1;
    			tileScore[own] += i+1;	// remember tile contribution
    			}
    		}
    		if(!owned) { unownedPalaces.addChip(palace); }
    	}
    	// assign the final owners of each tower
    	for(Cluster cl = wall_clusters; cl!=null; cl=cl.next)
    	{	MedinaCell owner = cl.wallClaimer;
    		
    		if(owner!=null) 
     		{Cluster ownCluster = owner.cluster;
    		 int tower = cl.tower.towerNumber()-1;
    		 // there can be 2 clusters for each tower, and the last claim should dominate
    		 if((towerClaimer[tower]==null) || (towerClaimer[tower].moveWalled<owner.moveWalled))
    		 {
    		 towerClaimer[tower]=owner;
    		 if(owner.isEdgeCell())
    				{ for(int nd=0,dir=CELL_RIGHT; nd<4; nd++,dir+=2)
    					{ MedinaCell adj = owner.exitTo(dir);
    					  if((adj!=null)&&!adj.isEdgeCell())
    					  {	ownCluster = adj.cluster;
    					  }
    					}
    				}
    		G.Assert((ownCluster!=null)
    				&& (ownCluster.isPalaceCluster) 
    				&& (ownCluster.clusterOwner!=null),"got a good owner");
    		towerOwner[tower] = ownCluster.clusterOwner;
    		 }
    		}
    	}
     	unownedTowers.reInit();
    	for(int i=0;i<towerOwner.length;i++)
    	{	MedinaChip owner = towerOwner[i];
    		MedinaChip towerCard = MedinaChip.getTowerCard(i);
    		boolean owned = false;
    		if(owner!=null)
    		{	int ownindex = playerOwning(owner.domeColor());
    			// in V2, unused colors are neutral domes
    			if(ownindex>=0)
    			{
    			// tower 0 is worth 1 point, etc.
    			score[ownindex] += 1+i;	
    			tileScore[ownindex] += 1+i;		// remember the tiles contribution
    			// and assign the card
    			cards[ownindex].addChip(towerCard);
    			owned = true;
    			}
    		}
    		if(!owned) { unownedTowers.addChip(towerCard); }
    	}
    	
    	// score the palaces for size, wall and wall count
    	for(Cluster cl = palace_clusters; cl!=null; cl=cl.next)
    	{   PalaceColor color = cl.palaceColor();
    		int colorIndex = color.ordinal();
    		if(cl.claimed)
    		{	
				int owner = playerOwning(cl.owner());
				if(owner>=0)
				{
				nClaimedPalaces[colorIndex]++;				// number of palaces of this color that are claimed by players
				claimedPalaces[colorIndex]+=cl.palace_size;
				// 3 player v2 games place 4'th domes as "neutral"
     			score[owner]+=cl.size+cl.meeple_size+cl.wall_size;
    			// count the number and size of claimed palaces for the robot 
    			owned_palaces[owner] |= (1<<colorIndex);
    			edgeScore[owner] += cl.edge_size;
				}
    		}
    		else
    		{	placedPalaces[colorIndex]+=cl.palace_size;
    		}
    	}
    	// score the well
    	if(variation==Variation.Medina_V2)
    	{
    		MedinaCell well = getCell(well_col,well_row);
    		for(int nd=0,dir=CELL_RIGHT; nd<4; nd++,dir+=2)
    		{
    			MedinaCell ex1 = well.exitTo(dir);
    			MedinaCell ex2 = ex1.exitTo(dir);
    			Cluster cl = ex2.cluster;
    			if(cl!=null)
    			{	MedinaChip owner = cl.clusterOwner;
    				if(owner!=null)
    				{
    				int own = playerOwning(owner.domeColor());
    				if(own>=0) { score[own]+=4; }
    				}
    			}
    		}
    	}
    	clustersValid=true;
    	}
     }
    
    // find the incomplete meeple or palace cluster
    Cluster findIncompleCluster(MedinaChip type)
    {	buildClusters();
    	Cluster base_cluster = type.isPalace() 
    							? palace_clusters
    							: type.isMeeple() 
    								? meeple_clusters
    								: null;	
    	for(Cluster cl = base_cluster; cl!=null; cl=cl.next)
    	{
    		if((cl.type==type)&&!cl.complete && !cl.claimed) { return(cl); }
    	}
    	return(null);
    }
    /* initialize a board back to initial empty state */
    public void doInit() { doInit(gametype,randomKey,players_in_game,revision); }
    public void doInit(String gtype,long randomv,int num,int rev)
    {	randomKey = randomv;
   		adjustRevision(rev);
    	Init_Standard(gtype,num);
    }
    public void doInit(String game,long randomv)
    {	StringTokenizer tok = new StringTokenizer(game);
    	String nam = tok.nextToken();
    	int rev = revision;
    	int num = tok.hasMoreTokens()?G.IntToken(tok):players_in_game;
    	long newkey = randomv;
    	if(tok.hasMoreTokens())
    		{
    			rev = num;
    			num = G.IntToken(tok);
    			if(tok.hasMoreTokens()) { newkey = G.IntToken(tok); }
    		}
    		else
    		{ rev = 100;	// old game records do not have a revision 
    		}
         doInit(nam,newkey,num,rev);
        // note that firstPlayer is NOT initialized here
    }

    public int getNextPlayer() { return((whoseTurn+1)%players_in_game); }
    public int getNextPlayer(int pl)  { return((pl+1)%players_in_game); }
    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player");
        case GAMEOVER_STATE:
        case PUZZLE_STATE:
            break;
        case PASS_STATE:
        case CONFIRM_STATE:
        case CONFIRM2_STATE:
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
        {
         case PASS_STATE:
         case CONFIRM_STATE:
         case CONFIRM2_STATE:
         case RESIGN_STATE:
            return (true);

        default:
            return (false);
        }
    }


   public boolean uniformRow(int player,MedinaCell cell,int dir)
    {	for(MedinaCell c=cell.exitTo(dir); c!=null; c=c.exitTo(dir))
    	{    MedinaChip cup = c.topChip();
    	     if(cup==null) { return(false); }	// empty cell
    	     if(cup.chipNumber()!=player) { return(false); }	// cell covered by the other player
    	}
    	return(true);
    }
    void setGameOver()
    {	setState(MedinaState.GAMEOVER_STATE);
    }

    public boolean WinForPlayer(int player)
    {	if(board_state==MedinaState.GAMEOVER_STATE)
    	{	if(resigned) { return(win[player]);}
    		int best = currentScoreForPlayer(player);
    		int second = -1;
    		for(int i=0;i<score.length;i++)
    		{	if(i!=player) 
    			{  int sc = currentScoreForPlayer(i);
    			   if(sc>=second) { second=sc; }
    			}
    		}
    		return(best>second);
    	}
    	return(false);
    }
    int currentScoreForPlayer(int pl)
    {
    	buildClusters();
    	return(score[pl]);
	
    }
    
    void getScores(MedinaMovespec move)
    {	for(int i=0;i<players_in_game;i++) { move.playerScores[i] = score[i]; }
    }
    // look for a win for player.  This algorithm should work for Gobblet Jr too.


    // look for a win for player.  
    public double ScoreForPlayer(int player,boolean print)
    {  	double finalv=0.0;
    	double unused_dome_value = 0.0;
       	double stageOfGame = (1.0-piecesOnBoard/150.0);
        
    	buildClusters();
    	// score the palaces we haven't claimed at some approcimation of their eventual value
    	int myPalaces = owned_palaces[player];
    	int enemyNeutrals = 0;
    	for(int i=0;i<players_in_game;i++) { if(i!=player) { enemyNeutrals += neutralDomes[i].height(); }}
    	if(print) { System.out.println(""+player+" Owned: "+myPalaces);  }
    	for(int i=0;i<4;i++)
    	{	if((myPalaces&(1<<i))==0)	// we don't own one
    		{	double number_to_place = (players_in_game+enemyNeutrals-nClaimedPalaces[i]);
    			if(number_to_place>0)
    			{
    			int palaces = nInitialPalaces[players_in_game-2];
    			double numberUnplaced =  palaces*players_in_game-claimedPalaces[i]-placedPalaces[i];
    			// the factor of i represents the likelyhood that someone else
    			// will claim the palace bonus (worth i+1) for this color
    			double val = ((numberUnplaced*stageOfGame)/number_to_place)+i+1;
    			unused_dome_value += val;
    			if(print) { System.out.println(""+player+" "+i+" n="+number_to_place+" nun="+numberUnplaced+":"+val); }
    			}
    			
    		}
    	}
    	// downgrade the value of the tower and palaces bonuses proportional
    	// to the stage of the game.
    	double tile_value_adj = -tileScore[player]*stageOfGame;
    	finalv += edgeScore[player]*0.6;	// fractional point for being on the edge
    	finalv += score[player];
    	finalv += unused_dome_value;		// points for each unused dome
    	finalv += tile_value_adj;			// downgrade tiles near the beginning.
    	finalv += meeples[player].height()*0.55;	// half a point for each unused meeple
    	finalv += walls[player].height()*0.6;		// hald a point for each unused wall
    	finalv += stables[player].height()*1.3;		// points for unused stables
    	if(print) { System.out.println("Final for "+player+" ="+finalv+" un="+unused_dome_value); }
    	return(finalv);
    }

    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        pickedSourceStack.clear();
        droppedDestStack.clear();
        undoStack.clear();
        stateStack.clear();
        
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    MedinaCell dr = droppedDestStack.pop();
    int undo = undoStack.pop();
	undoExtras(undo);
	setState(stateStack.pop());
	robotUndo = 0;
    if(dr!=null)
    	{
     	pickedObject = dr.removeTop();
    	if(dr.onBoard) 
    		{ invalidateClusters(); 
    		  piecesOnBoard--;
    		  lastDroppedDest = null;
    		  if(pickedObject.isDome()||pickedObject.isStable()) { dr.movePlaced = 0; }
    		  dr.moveWalled = 0;
    		}
    	}
    }
    
    // claim the tower meeples in a v2 game.  In a v1 game, 
    // there won't be any meeples there 
    void claimMeeples(int towerNumber,DomeColor ownerColor,replayMode replay)
    {	MedinaCell merchants = towerMerchants[towerNumber-1];
    	if(merchants.height()>0)
    	{	// encode the claim in 0xFF of the undo info, 0xF0 is the owner, 0xF is the tower.  Note
    		// that we expect one owner but possibly multiple towers.
    		int owner = playerOwning(ownerColor);
    		// owner = -1 is claim by a neutral dome
     		robotUndo |= ((1<<(towerNumber-1)) | ((owner+1)<<4));
			while(merchants.height()>0)
			{
				// claim the merchants on the tower
				MedinaChip m = merchants.removeTop();
				// if the claimer is a neutral tower, the meeples go back in the box
				if(owner>=0)
				{				
				meeples[owner].addChip(m);
				if(replayMode.Replay!=replay)
					{
					animationStack.push(merchants);
					animationStack.push(meeples[owner]);
					}
				}
				else
				{	trash.addChip(m);
					animationStack.push(merchants);
					animationStack.push(trash);
				}
			}
    	}
    }
    // undo the claim meeples action
    void unclaimMeeples(int robotUndo)
    {	int undoTowerBits = robotUndo&0xf;
    	if(undoTowerBits!=0)
    	{	int undoOwner= ((robotUndo&0xf0)>>4)-1;   		
    		while(undoTowerBits!=0)
    		{
    			int towerNumber = G.numberOfTrailingZeros(undoTowerBits);
    			undoTowerBits &= ~(1<<towerNumber);		// remove the current bit
    			int count = 3-towerNumber;				// how many meeples were there
    			while(count-- > 0 )
    			{	towerMerchants[towerNumber].addChip(MedinaChip.MEEPLE);
    				if((undoOwner>=0) && (undoOwner<players_in_game))
    				{
    					// take them away from the beneficiary
    					meeples[undoOwner].removeTop();
    				}
    			}
    		}
    	}
    }
    // award tea tiles for claiming a purple palace
    void claimTeaCards(MedinaChip owner,replayMode replay)
    {
    	int ownerNumber = playerOwning(owner.domeColor());
    	int number = (teaPool.height()+1)/2;				// start with 6, give away 3, 2 1
    	robotUndo |= (1<<(ownerNumber+8))|(number<<12);		// encode the beneficiary in 0xf00, number of cards in 0xf000
    	while(number-- > 0)
    	{	teaPool.removeTop();
    		if(ownerNumber>=0)
    		{ teaCards[ownerNumber].addChip(MedinaChip.TEA);
    		  if(replay!=replayMode.Replay)
    		  {
    			  animationStack.push(teaPool);
    			  animationStack.push(teaCards[ownerNumber]);
    		  }
    		}
    		else { 
    			teaDiscards.addChip(MedinaChip.TEA);
    			if (replay!=replayMode.Replay)
    			{	// throw them down the well
    				animationStack.push(teaPool);
    				animationStack.push(teaDiscards);
    			}
    		}
    	}
    }
    
    void discardPalaces(MedinaChip type,replayMode replay)
    {	if(revision>=101)
    	{
    	int chinese = 0;
    	for(int i = 0;i<players_in_game;i++)
    	{
    		MedinaCell p = palaces[i][type.palaceColor().ordinal()];
    		int count = p.height();
    		chinese = chinese*MAX_INITIAL_PALACES+count;
    		while(count-- > 0)
    		{	MedinaChip ch = p.removeTop();
    			trash.addChip(ch);
    			if(replayMode.Replay!=replay)
    			{	
    				animationStack.push(p);
    				animationStack.push(trash);
    			}
    			
    		}
    	}
    	chinese = chinese*4 + type.palaceColor().ordinal();
    	robotUndo |= chinese<<16;	// encode the undo as 0xffff0000;
    	}
    }
    void recoverPalaces(int undo)
    {	int rec = undo>>16;
    	if(rec>0)
    	{	int palaceIndex = rec%4;
    		MedinaChip palace = MedinaChip.getPalace(palaceIndex);
    		rec = rec/4;
    		for(int np = players_in_game-1; np>=0; np--)
    		{
    			MedinaCell p = palaces[np][palaceIndex];
    			int rem = rec%MAX_INITIAL_PALACES;
    			rec = rec/MAX_INITIAL_PALACES;
    			while(rem-- > 0)
    			{
    				p.addChip(palace);
    			}
    		}
    	}
    }
    void unclaimTeaCards(int undo)
    {	int ownerBit = undo&0xF00;
    	if(ownerBit!=0)
    	{	int owner = G.numberOfTrailingZeros(ownerBit)-8;
    		int number = (undo>>12) & 0xf;
    		while(number-- > 0)
    		{
    			teaPool.addChip(MedinaChip.TEA_BACK);
    			if(owner<players_in_game)
    			{
    				teaCards[owner].removeTop();
    			}
    			else { teaDiscards.removeTop(); }
    		}
    	}
    	
    }
    //
    // chip "top" is not yet placed on "c", but is about to be.
    // if top is a dome, then all adjacent walls are claimed.
    // otherwise, if a wall is in play and not yet claimed, it is claimed.
    //
    void claimWalls(MedinaCell c,MedinaChip top,replayMode replay)
    {	MedinaCell palaceSeed = null;		// the existing palace we're joining
    	MedinaCell wallSeed = null;			// the wall we're adjacent to
    	MedinaCell towerSeed = null;		// the tower we're connected to
    	boolean newClaim = false;			// true if this is a dome placement
     	
    	if(top.isWall())
    	{	// we're extending a wall, and may be hitting a palace
       		// if we didn't hit a palace, there's nothing to do for sure
    		for(int nd=0,dir=CELL_RIGHT; nd<4; nd++,dir+=2)
    		{	MedinaCell adj = c.exitTo(dir);
    			if(adj!=null)
    			{	MedinaChip adjtop = adj.chipAtIndex(0);
    				if(adjtop!=null)
    				{	if(adjtop.isWall()) { wallSeed = adj; }
    					else if(adjtop.isPalaceComponent()) { palaceSeed = adj; }
    					else if(adjtop.isTower()) { towerSeed = adj; }
    				}
    			}
    		}
     	}
    	else if(top.isDome()) 
    		{ palaceSeed = c; 
    		  newClaim = true; 
    		}
    	else if(top.isPalace() || top.isStable()) 
    			{ 
    			// placing a palace piece not adjacent to a wall can't change anything
    			// if we're placing a new palace not adjacent to an old palace, it's
    			// unclaimed for sure and therefore not interesting
    			boolean hasWall = false;
    			MedinaCell seed = null;
    			for(int nd=0,dir=CELL_RIGHT; nd<4; nd++,dir+=2)
    			{	MedinaCell adj = c.exitTo(dir);
    				if(adj!=null)
    				{	MedinaChip adjtop = adj.chipAtIndex(0);
    					if(adjtop!=null)
    						{ if(adjtop.isWall()) { hasWall=true; wallSeed = adj; }
    						  // if there is no palace adjacent to the new palace (or stable)
    						  // then there is no owned cluster and can be no change of owner
    						  if(adjtop.isPalace()) { seed = adj; }
    						}
    				}
    				}
    				if(hasWall) { palaceSeed = seed; } 
   			 
    			}
    	else if(top.isMeeple()) {}
    	else if(top.isWell()) {}
    	else {throw G.Error("shouldn't get here");}
	
    	if(newClaim)
    	{	buildClusters();
    		// if we got here from an about to be placed dome, then we claim
    		// all the walls the palace is already adjacent to.
    		//
    		Cluster home_cluster = palaceSeed.cluster;
    		ClusterStack touched = home_cluster.touchedWalls;
    		if(touched!=null)
    		{	int sz = touched.size();
    			int towers = piecesOnBoard<<8;
    			for(int i=0; i<sz; i++)
    			{
    			Cluster cl = touched.elementAt(i);
    			MedinaCell tower = cl.tower;
    			int towerNumber = tower.towerNumber();
    			claimMeeples(towerNumber,top.domeColor(),replay);
    			towers |= (1<<towerNumber);	
    			}    
    		c.moveWalled = towers;	// this claims them all
    		//G.print("C "+c+" " +c.moveWalled);
    		}
    		if(home_cluster.type==MedinaChip.PURPLE_PALACE)
    		{
    			claimTeaCards(top,replay);
    		}
    		if(allDomesPlaced(home_cluster.type,home_cluster,top))
    		{
    			discardPalaces(home_cluster.type,replay);
    		}
    	}
    	else if(palaceSeed!=null)
    	{	buildClusters();
    		// if we got here from placing a wall, palace, or stable, then we claim only
    		// the adjacent wall, and only if it had not already been adjacent.
    		Cluster cl = palaceSeed.cluster;
     		if(cl.claimed)
    		{
    		ClusterStack touchedWalls =  cl.touchedWalls;
    		Cluster wallCluster = null;
    		// if we got here from a newly placed wall adjacent to a claimed palace, either
    		// towerSeed or wallCluster must have been found.  If from a newly placed palace,
    		// then wallCluster must found.
    		if(towerSeed==null) { wallCluster = wallSeed.cluster; towerSeed = wallCluster.tower; }
     		if((touchedWalls==null) || (wallCluster==null) || !touchedWalls.contains(wallCluster))
    			{	// we'll claim the walls we touch
        		int towerNumber = towerSeed.towerNumber();
           		claimMeeples(towerNumber,cl.owner(),replay);
   				c.moveWalled = (piecesOnBoard<<8) + (1<<towerNumber);
     	    		//G.print("C "+c+" " +c.moveWalled);
    			}
    		}
     	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject(replayMode replay)
    {	MedinaChip po = pickedObject;
    	if(po!=null)
    	{
    	MedinaCell ps = pickedSourceStack.pop();
    	pickedObject = null;
    	if(ps.onBoard) { lastDroppedDest = null; claimWalls(ps,po,replay); } 
    	else { if(po==MedinaChip.V_WALL){ po=MedinaChip.H_WALL; }} 
    	ps.addChip(po);
    	if(ps.onBoard) 
    		{ invalidateClusters(); 
    		  piecesOnBoard++;
    		  if(po.isDome() || po.isStable()) { ps.movePlaced = piecesOnBoard; }
    		}
    	}
     }
    // 
    // drop the floating object.
    //
    private MedinaCell getCell(MedinaId type,char col,int row)
    {
    	switch(type)
    	{
    	default: throw G.Error("Not expecting dest %s",type);
    	case BoardLocation:
    		return(getCell(col,row));
    	case TeaPoolLocation:
    		return(teaPool);
    	case TeaDiscardLocation:
    		return(teaDiscards);
    	case TeaCardLocation:
    		return(teaCards[col-'A']);
    	case NeutralDomeLocation:
    		return(neutralDomes[col-'A']);
    	case TowerMerchantLocation:
    		return(towerMerchants[col-'A']);
    	case DomeLocation:
    		return(domes[col-'A']);
    	case PalaceLocation:
    		return(palaces[col-'A'][row]);
    	case MeepleLocation:
    		return(meeples[col-'A']);
     	case WallLocation:
    		return(walls[col-'A']);
    	case StableLocation:
    		return(stables[col-'A']);
    	}
    	
    }
    private void dropObject(MedinaCell dr,replayMode replay)
    {
       G.Assert(pickedObject!=null,"ready to drop");
       droppedDestStack.push(dr);
       robotUndo = 0;
       if(pickedObject.isWall())
       {
    	pickedObject = (dr.onBoard
    						&& ((dr.exitTo(CELL_LEFT)==null)||(dr.exitTo(CELL_RIGHT)==null)))
    					? MedinaChip.V_WALL
    					: MedinaChip.H_WALL;
    	
       }
       
       if(dr.onBoard) { lastDroppedDest = dr; claimWalls(dr,pickedObject,replay); }
       
       dr.addChip(pickedObject);
   	   if(dr.onBoard) 
   	   {   int hh=dr.height();
   	   	   piecesOnBoard++;
   		   if(pickedObject.isDome())
   		   { dr.movePlaced = piecesOnBoard; 
   		     G.Assert(hh==2,"dome on top of 1");
   		   }
   		   else
   		   { if(pickedObject.isStable()) { dr.movePlaced = piecesOnBoard; }
   		     G.Assert(hh==1,"placed on board directly");
   		   }
   		   invalidateClusters();
   	   }
   	   undoStack.push(robotUndo);
   	   stateStack.push(board_state);
       pickedObject = null;
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(MedinaCell cell)
    {	return(cell==droppedDestStack.top());
    }
    public boolean isSource(MedinaCell cell)
    {
    	return(cell==pickedSourceStack.pop());
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	MedinaChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    public int boardImageIndex(boolean use_perspective)
    {	return(
    		variation.index 
    		+ (((variation==Variation.Medina_V2) && (nPlayers()==2)) 
    				? 2 : 0)
    		+ (use_perspective?0:1));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private MedinaChip pickObject(MedinaCell sr)
    {	
    	G.Assert(pickedObject==null,"ready to pick");
    	pickedSourceStack.push(sr);
    	pickedObject = sr.removeTop();
    	if(sr.onBoard) 
    		{ invalidateClusters();
    		  piecesOnBoard--;
    		  if(pickedObject.isDome()||pickedObject.isStable()) { sr.movePlaced = 0; }
    		  sr.moveWalled = 0;
    		}
    	return(pickedObject);
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(MedinaChip pic)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case PLAY_MEEPLE_STATE:
        	setState(MedinaState.PLAY_STATE);
        	break;
        case PLAY_STATE:
        	buildClusters();
        	if((variation==Variation.Medina_V2)
        		&& (piecesOnBoard<=((players_in_game==2)?7:8)))
        	{	// opening moves of a game
        		setState(MedinaState.CONFIRM_STATE);
        	}
        	else
        	{
    		setState(MedinaState.PLAY2_STATE);
    		if(!mustHaveLegalMoves(whoseTurn) && !hasLegalMoves(whoseTurn)) 
    		{ setState(MedinaState.CONFIRM2_STATE);
    		}
    		else if(((pic!=null) && pic.isDome())
    					|| someoneElseHasLegalMoves(whoseTurn)) { }
    		else { setState(MedinaState.DOME_STATE); }	// must place a dome
        	}
			break;
        case PLAY2_STATE:
        case CONFIRM2_STATE:
        case DOME_STATE:
			setState(MedinaState.CONFIRM_STATE);
			break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    public MedinaCell getSource() { return(pickedSourceStack.top()); }


    private void setEndgameStates()
    {
		if(!mustHaveLegalMoves(whoseTurn) && !hasLegalMoves(whoseTurn)) 
		{ setState(noMoreMoves()?MedinaState.GAMEOVER_STATE:MedinaState.PASS_STATE);
		}
	
    }
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case RESIGN_STATE:
    		setGameOver();
    		win[whoseTurn]=true;
    		resigned = true;
    		break;
    	case GAMEOVER_STATE: 
    		break;

    	case PASS_STATE:
     	case CONFIRM_STATE:
     	case CONFIRM2_STATE:
    	case PUZZLE_STATE:
    		buildClusters();
    		setState(MedinaState.PLAY_STATE);
    		setEndgameStates();
    		break;
    	}

    }
   
    // this is used as a test to enter DOME_STATE
    private boolean someoneElseHasLegalMoves(int who)
    {	if(domes[who].height()==0) { return(true); }	// we don't care if we don't have domes
    	// quick check
    	for(int i=0;i<players_in_game;i++)
    	{	if(i!=who)	// for everyone else
    		{
    		if(mustHaveLegalMoves(i)) { return(true); }
    		}
    	}
    	// slow check
    	for(int i=0;i<players_in_game;i++)
    	{	if(i!=who)	// for everyone else
    		{
    		if(getMoves(null,i)>0) { return(true); }
    		}
    	}
    	// in rare circumstances, we may have domes left but be unable to play it.
    	// if we get this far, we'll never be able to play it.
    	if(generateDomeMoves(null,domes[who],who)==0) { return(true); }
    	
    	return(false);	// no one has legal moves, so we need to place a dome
    }
    
    // quick check that somebody must have moves
    private boolean mustHaveMoreMoves()
    {	int who = whoseTurn;
   		do 
   		{	if(mustHaveLegalMoves(who)) { return(true); }
   			who = getNextPlayer(who);
   		}
   		while(who!=whoseTurn);
   		
    	return(false);
    }
    private boolean noMoreMoves()
    {	if(mustHaveMoreMoves()) { return(false); }
    	int who = whoseTurn;
    	do 
    	{	if(hasLegalMoves(who)) { return(false); }
    		who = getNextPlayer(who);
    	} while(who!=whoseTurn);
    	return(true);
     }


    private void doDone()
    {	
        acceptPlacement();
        setNextPlayer();
        setNextStateAfterDone(); 
        trash.reInit();
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	MedinaMovespec m = (MedinaMovespec)mm;

        //G.print("E "+m+" for "+whoseTurn+" "+board_state); 
        switch (m.op)
        {
        case MOVE_DONE:

         	doDone();

            break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case PLAY_STATE:
        		case PLAY_MEEPLE_STATE:
        		case DOME_STATE:
        		case PLAY2_STATE:
        			acceptPlacement();
        			{
        			MedinaCell src = getCell(m.source, m.from_col, m.from_row);
        			MedinaCell dest = (m.source==MedinaId.TeaCardLocation) 
        									? teaDiscards			// special case, discards is the only choice
        									: getCell(MedinaId.BoardLocation,m.to_col,m.to_row);
                    MedinaChip pik = pickObject(src);
                    if(replay!=replayMode.Replay)
                    {
                    	animationStack.push(src);
                    	animationStack.push(dest);
                    }
                    m.chip = pickedObject;	// for game log
                    dropObject(dest,replay); 
                    setNextStateAfterDrop(pik);
        			}
                    break;
        	}
        	break;
         case MOVE_DROPB:
        	{
        	MedinaChip pic = pickedObject;
        	G.Assert(pic!=null,"something is moving");
        	MedinaCell dest = getCell(MedinaId.BoardLocation, m.to_col, m.to_row);
        	if(replay==replayMode.Single)
        	{
        	MedinaCell src = pickedSourceStack.top();
        	animationStack.push(src);
        	animationStack.push(dest);
        	}
            dropObject(dest,replay);
            setNextStateAfterDrop(pic);
        	}
            break;
         case MOVE_PASS:
        	 setNextStateAfterDrop(null);
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	MedinaCell src = getCell(m.from_col,m.from_row);
        	if(isDest(src))
        		{ 
        		  unDropObject(); 
        		}
        	else 
        		{ pickObject(getCell(MedinaId.BoardLocation, m.from_col, m.from_row));
        			// if you pick up a gobblet and expose a row of 4, you lose immediately
         		}
        	}
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
            MedinaCell dest = getCell(m.source, m.to_col, m.to_row);
            if(dest==teaDiscards)
            {
            	dropObject(dest,replay);
            	if(board_state!=MedinaState.PUZZLE_STATE) { setState(MedinaState.CONFIRM_STATE); }
            }
            else if(!dest.onBoard && (board_state!=MedinaState.PUZZLE_STATE))
            	{ unPickObject(replay); 
            	}
            else
            	{
            	dropObject(dest,replay);
            	if(board_state==MedinaState.PUZZLE_STATE) { acceptPlacement(); }
            	}
        	}
        	break;

        case MOVE_PICK:
        	{
            MedinaCell src = getCell(m.source, m.from_col, m.from_row);
            if(src==teaDiscards)
        	{
            unDropObject();
        	if(board_state==MedinaState.CONFIRM_STATE)
       			{
        		setState(MedinaState.PLAY2_STATE);
       			}
        	}
            else
            {
            MedinaCell dest = droppedDestStack.top();
            if((dest!=null)&& !dest.onBoard) 
            	{ // this prevents accidental rearrangement of the rack 
            	 unDropObject(); 
            	 unPickObject(replay);  
            	}
            pickObject(src);
            }
            m.chip = pickedObject;	// for game log
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject(replay);
            setState(meeplesOnBoard()?MedinaState.PLAY_STATE:MedinaState.PLAY_MEEPLE_STATE); 
    		setEndgameStates();
            
            break;

        case MOVE_EDIT:
    		acceptPlacement();
            setState(MedinaState.PUZZLE_STATE);

            break;
        case MOVE_RESIGN:
        	setState(unresign==null?MedinaState.RESIGN_STATE:unresign);
        	break;

        case MOVE_GAMEOVERONTIME:
     	   win[whoseTurn] = true;
     	   setState(MedinaState.GAMEOVER_STATE);
     	   break;
        default:
        	cantExecute(m);
        }


        return (true);
    }

    private boolean canLegallyPickFromRack(int player,MedinaCell c)
    {
    	CommonMoveStack  v = GetListOfMoves(player);
    	int nv = v.size();
    	for(int i=0;i<nv; i++)
    	{	MedinaMovespec mm = (MedinaMovespec)v.elementAt(i);
    		if((mm.op==MOVE_RACK_BOARD)
    			&& ((c.rackLocation!=MedinaId.PalaceLocation) || (mm.from_row == c.row))
    				&& (mm.source==c.rackLocation))
    		{
    			return(true);
    		}
    	}
    	return(false);
    }
    public boolean LegalToHitChips(int player ,MedinaCell c)
    {	
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
         case CONFIRM_STATE:
         case CONFIRM2_STATE:
        	 if(pickedObject==null) { return(false); }
        	 return(whoseTurn==player);
         case DOME_STATE:
        	 if(c.rackLocation!=MedinaId.DomeLocation) { return(false); }
			//$FALL-THROUGH$
		case PLAY2_STATE:        	 
         case PLAY_MEEPLE_STATE:
         case PLAY_STATE: 
        	    if(player!=whoseTurn) { return(false); }
       	    if(pickedObject==null)
       	    {
       	    	return(canLegallyPickFromRack(player,c));
       	    }
       	    // something already picked
       	    MedinaCell top = pickedSourceStack.top();
       	    return((top.onBoard==false)
    					&& (c.rackLocation==(top.rackLocation))
    					);
        case PUZZLE_STATE:
        	if(pickedObject==null) { return(true); }
        	return(pickedObject==c.storageFor); 
        case PASS_STATE:
        case RESIGN_STATE:
		case GAMEOVER_STATE:
			return(false);
        }
    }
  
    // true if it's legal to drop gobblet  originating from fromCell on toCell
    public boolean legalToPlaceDome(MedinaCell cell,MedinaChip type)
    {	
    	MedinaChip top = cell.topChip();
    	if(top==null) { return(false); }
    	if(type==null) { return(false); }
    	if(!top.isPalace()) { return(false); }
    	if(cell.cluster==null) { return(false); }
    	if(cell.cluster.claimed) { return(false); }
    	// domes must be on unclaimed palace of a unique color for the player
    	return(legalToPlaceNewDome(type,top));
    }
    public boolean legalToPlaceNewDome(MedinaChip domeType,MedinaChip palaceType)
    {
    	if((variation==Variation.Medina_V2)
    		&& (players_in_game<=3)
    		&& (domeType==MedinaChip.getDome(getUnusedColor(getColorMap()))))
    	{	// neutral domes can be placed anywhere
    		return(true);
    	}
    	else
    	{
    	for(Cluster cl = palace_clusters; cl!=null; cl=cl.next)
    	{
    	if(cl.isPalaceCluster 
    			&& cl.claimed 
    			&& (cl.type==palaceType) 
    			&& (cl.clusterOwner==domeType)) 
    		{ return(false); 
    		}
    	}
    	return(true);
    	}
    }
    //
    // true if it's legal to drop a wall.  This is believed to be complete and correct
    //
    public boolean legalToPlaceWall(MedinaCell cell)
    {	{ MedinaChip top = cell.topChip();
    	  if(top!=null) { return(false); }
    	}
    	if(!cell.isEdgeCell()) { return(false); }
    	// wall must be adjacent to 1 wall or tower
    	
    	int adjacent_piece = 0;
    	
    	for(int id=0,dir = CELL_UP; id<4; id++,dir+=2 )
    	{	MedinaCell adj = cell.exitTo(dir);
    		if(adj!=null) 
    			{ 	MedinaChip top = adj.topChip();
    			  	if((top!=null)&&(top.isWall()||top.isTower()))
    			  	{	adjacent_piece++;
    			  	}
    			}
    	}
    	return(adjacent_piece==1);
    }
    // true if it's legal to drop a palace,  originating from fromCell on toCell
    public boolean legalToPlacePalace(MedinaCell cell,MedinaChip type)
    {	Cluster palace = findIncompleCluster(type);
       	if(palace!=null)
       	{	if (palace.claimed || !(palace.isExpansionCell(cell)))
       		{ //can't add to a claimed palace except with stables
       			return(false); 
       		} 
       		// in version 1, the well if out of the way
       		return(!adjacentToWell(cell.col,cell.row));
       	}
       	return(legalToPlaceNewPalace(cell));
    }
    public boolean legalToPlaceNewPalace(MedinaCell cell)
    {	MedinaChip top = cell.topChip();
		if(top!=null) { return(false); }
		if(cell.isEdgeCell() || (cell.geometry==Geometry.Standalone)) { return(false); }
		// in V1, the well is out of the y
		if(adjacentToWell(cell.col,cell.row)) { return(false); }
       	// new palaces must not be adjacent to any old palace
       	for(int dir=0;dir<CELL_FULL_TURN;dir++)
       	{	MedinaCell c = cell.exitTo(dir);
       		MedinaChip ctop = c.chipAtIndex(0);
       		if((ctop!=null)&&(ctop.isPalaceComponent())) { return(false);}
       	}
    	return(true);
    } 
    
    // true if it's legal to drop gobblet  originating from fromCell on toCell
    public boolean legalToPlaceMeeple(MedinaCell cell)
    {	MedinaChip top = cell.topChip();
    	if(top!=null) { return(false); }
    	if(cell.isEdgeCell()) { return(false); }
     	if((board_state==MedinaState.PLAY_MEEPLE_STATE)&&!isInteriorCell(cell)) { return(false); }
    	// meeples must be adjacent to endpoint meeples
    	Cluster meeple = findIncompleCluster(MedinaChip.MEEPLE);
    	if((meeple!=null)&& !meeple.isExpansionCell(cell)) { return(false); }
    	return(singleMeepleCondition(cell));
    } 
    
    // true if it's legal to drop gobblet  originating from fromCell on toCell
    public boolean legalToPlaceStable(MedinaCell cell)
    {	MedinaChip top = cell.topChip();
    	if(top!=null) { return(false); }
    	if(cell.isEdgeCell() || (cell.geometry==Geometry.Standalone)) { return(false); }
    	if(adjacentToWell(cell.col,cell.row)) { return(false); }
    	// stables have to be adjacent to a palace
    	for(int nd=0,dir=CELL_RIGHT; nd<4; nd++,dir+=2)
    	{	MedinaCell c = cell.exitTo(dir);
    		MedinaChip ctop = c.chipAtIndex(0);
    		if((ctop!=null)&&(ctop.isPalace()))
    		{
    		buildClusters();
    		Cluster cl = c.cluster;
    		G.Assert(cl!=null,"there must be a cluseter for each palace");
    		if(cl.isExpansionCell(cell)) { return(true); }
    		}
    	}
    	return(false);
    } 
    public boolean legalToPlaceWell(MedinaCell c)
    {
    	return(isInteriorCell(c));
    }
    public boolean legalToPlaceTea(MedinaCell c)
    {
    	return(c==teaDiscards);
    }
	public boolean isInteriorCell(MedinaCell c) 
	{	if(!c.onBoard || c.isEdgeCell()) { return(false); }
		for(int i=0,dir=CELL_LEFT;  i<4; i++,dir+=2)
			{
			MedinaCell adj = c.exitTo(dir);
			if(adj.isEdgeCell()) { return(false); }
			}
		return(true);
	}
	public boolean legalToDropOn(MedinaChip po,MedinaCell cell)
	{
		if(po.isDome()) { return(legalToPlaceDome(cell,po)); }
		if(po.isTower()) { throw G.Error("Not expecting a tower"); }
		if(po.isWall()) { return(legalToPlaceWall(cell)); }
		if(po.isPalace()) { return(legalToPlacePalace(cell,po)); }
		if(po.isMeeple()) { return(legalToPlaceMeeple(cell)); }
		if(po.isStable()) { return(legalToPlaceStable(cell)); }
		if(po.isWell()) { return(legalToPlaceWell(cell)); }
		if(po.isTea()) { return(legalToPlaceTea(cell)); }
		throw G.Error("should not get here");	
	}
	public Hashtable<MedinaCell,MedinaCell> getDests()
	{	Hashtable<MedinaCell,MedinaCell> hh = new Hashtable<MedinaCell,MedinaCell>();
		if(pickedObject!=null)
			{for(MedinaCell c=allCells; c!=null; c=c.next)
				{		if(legalToDropOn(pickedObject,c)) { hh.put(c,c); }
				}
				if(legalToDropOn(pickedObject,teaDiscards)) { hh.put(teaDiscards,teaDiscards); }
			}
		return(hh);
	}
	
    public boolean LegalToHitBoard(MedinaCell cell)
    {	
        switch (board_state)
        {
        case PLAY_MEEPLE_STATE:
        	if((pickedObject!=null) && (pickedObject.isMeeple()))
        		{
        		return(isInteriorCell(cell));
        		}
        	return(false);
		case PLAY2_STATE:
 		case PLAY_STATE:
 		case PUZZLE_STATE:
 		case DOME_STATE:
 			if(pickedObject!=null)
 			{
 			return(legalToDropOn(pickedObject,cell));
 			}
 			if(board_state==MedinaState.PUZZLE_STATE)
 			{	// need to be more selective about what can be picked up?
 				MedinaChip top = cell.topChip();
 				return((top!=null)&&!top.isTower());
 			}
 			return(isDest(cell));
 			
        default:
        	throw G.Error("Not expecting state %s", board_state);
		case GAMEOVER_STATE:
		case PASS_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		case CONFIRM2_STATE:
			return(isDest(cell));
 
         }
    }
  public boolean canDropOn(MedinaCell cell)
  {	MedinaCell top = pickedSourceStack.top();
  	return((pickedObject!=null)				// something moving
  			&&(top.onBoard == (cell!=top)));// dropping in the rack, must be to the same cell
  			
  }
 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(MedinaMovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        robotUndo = 0;
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("R "+m);
        if (Execute(m,replayMode.Replay))
        {	switch(m.op)
        	{
        	case MOVE_DONE:
        	case MOVE_RACK_BOARD:
        	case MOVE_PASS:
        		break;
        	default:
        		if (DoneState())
        		{
                doDone();
        		}
        		else
        		{
        			throw G.Error("Robot move should be in a done state");
        		}

        	}
        }
        robotStack.push(robotUndo);
    }
    void undoExtras(int undo)
    {
       	unclaimMeeples(undo);
    	unclaimTeaCards(undo);
    	recoverPalaces(undo);

    }

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(MedinaMovespec m)
    {	int undo =robotStack.pop();
        //G.print("U "+m+" for "+whoseTurn);
    	undoExtras(undo);
    	switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;

        case MOVE_DONE:
        case MOVE_PASS:
            break;
        case MOVE_RACK_BOARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case GAMEOVER_STATE:
        		case PLAY2_STATE:
        		case DOME_STATE:
        		case PLAY_MEEPLE_STATE:
        		case PLAY_STATE:
        		case CONFIRM_STATE:
        		case CONFIRM2_STATE:
        			MedinaCell src = getCell(m.source,m.from_col, m.from_row);
        			MedinaCell dest = (m.source==MedinaId.TeaCardLocation)
        								? teaDiscards	// special case for the discards 
        								: getCell(MedinaId.BoardLocation,m.to_col,m.to_row);
        			pickObject(dest);
        			dropObject(src,replayMode.Replay);
       			    acceptPlacement();
                    break;
        	}
        	break;
        }
        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
        invalidateClusters();
 }
 int generateWallMoves(CommonMoveStack  all,MedinaCell wall,int who)
 {	int nmoves = 0;
 	if(wall.topChip()!=null)
	 {for(Cluster cl = wall_clusters; cl!=null; cl=cl.next)
 	{
	 CellStack exp = cl.expansionCells;
	 if(exp!=null)
	 {	MedinaCell c = exp.top();
	 	if(all!=null) 
	 		{all.addElement(new MedinaMovespec(MedinaId.WallLocation,wall.col,wall.row,c.col,c.row,who));
	 		}
	 	nmoves++;
	 }
 	}
	 }	
 	return(nmoves);

 	}
 int generateStableMoves(CommonMoveStack  all,MedinaCell pieces,int who)
 {	int nmoves = 0;
 	MedinaChip type =pieces.topChip();
 	// stables can be placed in the expansion cells of any palace cluster
 	if(type!=null)
 		{
 		buildClusters();
	 	for(Cluster cl = palace_clusters; cl!=null; cl=cl.next)
	 	{ CellStack expansion = cl.expansionCells;
	 	  if((cl.isPalaceCluster && expansion!=null))
	 	  {	int siz = expansion.size();
	 	  	for(int i=0;i<siz;i++)
	 	  	{
	 	  	MedinaCell c = expansion.elementAt(i);
	 	  	if(all!=null)
	 	  		{all.addElement(new MedinaMovespec(MedinaId.StableLocation,pieces.col,pieces.row,c.col,c.row,who));
	 	  		}
	 	  	nmoves++;
	 	  	}
	 	  }
	 	}
 		}
 	return(nmoves);
 }
 int generateTeaMoves(CommonMoveStack all,MedinaCell pieces,int who)
 {
	 if(pieces.height()>0)
			{ if(all!=null) 
				{all.addElement(new MedinaMovespec(MedinaId.TeaCardLocation,pieces.col,pieces.row,teaDiscards.col,teaDiscards.col,who));}
			  return(1);
			}
	 return(0);
 }
 int generateDomeMoves(CommonMoveStack  all,MedinaCell pieces,int who)
 {	int nmoves = 0;
	 MedinaChip type =pieces.topChip();
	 // domes can be placed on any unclaimed palace, but only one per color
	
	 if(type!=null)
	 {	 G.Assert(type.isDome(),"dome expected");
	 	for(Cluster cl=palace_clusters; cl!=null; cl=cl.next)
	 	{
		 if(cl.isPalaceCluster && !cl.claimed && legalToPlaceNewDome(type,cl.type))
				 {
			 	MedinaCell c = cl.seed;
			 	if(all!=null)
			 		{all.addElement(new MedinaMovespec(pieces.rackLocation(),pieces.col,pieces.row,c.col,c.row,who));
			 		}
			 	nmoves++;
				 }
	 	}	
	 }
	 return(nmoves);
 }
 boolean singleMeepleCondition(MedinaCell c)
 {	if(c.isEdgeCell() || (c.geometry==Geometry.Standalone)) { return(false); }
 	if(revision<102) { return(true); }
 	
 	int meeples = 0;
	 for(int i=0,dir=CELL_RIGHT;i<4;i++,dir+=2)
		{	MedinaCell d = c.exitTo(dir);
			MedinaChip top = d.topChip();
			if((top!=null) && top.isMeeple()) { meeples++; }
		}
	 return(meeples<=1);
 }
 int generateMovesFor(CommonMoveStack  all,MedinaCell pieces,int who)
 {	int nmoves = 0;
 	MedinaChip type = pieces.topChip();
	if(type!=null)
	{
	Cluster cluster =  findIncompleCluster(type);
	if(cluster!=null)
	{	// if there's an incomplete cluster, we have to add to it
		CellStack expansion = cluster.expansionCells;
		G.Assert(expansion!=null,"must be an expansion");
		int siz = expansion.size();
		// note that an incomplete cluster can exist for a palace color with all domes placed.
		// in that case, in rev 101 and better, the pieces have been removed.
		for(int i=0;i<siz;i++) 
			{
			MedinaCell c = expansion.elementAt(i);
			G.Assert(c.topChip()==null,"empty cell");
			if(all!=null) 
			{ all.addElement(new MedinaMovespec(cluster.isPalaceCluster?MedinaId.PalaceLocation:MedinaId.MeepleLocation,pieces.col,pieces.row,c.col,c.row,who));
			}
			nmoves++;
			}
	}
	else if(type.isMeeple())
	{	// meeples can go to any unoccupied point
		for(MedinaCell c = allCells; c!=null; c=c.next)
		{
		if((c.topChip()==null) && singleMeepleCondition(c))
			{
			if(all!=null)
			{all.addElement(new MedinaMovespec(MedinaId.MeepleLocation,pieces.col,pieces.row,c.col,c.row,who));
			}
			nmoves++;
			}
		}
	}
	else if(type.isPalace())
	{	
		if(!allDomesPlaced(type,null,null) || revision<101) 
		{
		for(MedinaCell c = allCells; c!=null; c=c.next)
		{
		if(legalToPlaceNewPalace(c))
			{
			if(all!=null)
				{all.addElement(new MedinaMovespec(MedinaId.PalaceLocation,pieces.col,pieces.row,c.col,c.row,who));
				}
			nmoves++;
			}
		}}
	}
	else { throw G.Error("Not expecing "+type); }
	}
	return(nmoves);
 }
 // this is for the robot, and it insists that there be actual
 // moves.
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  v = GetListOfMoves(whoseTurn);
	G.Assert( v.size()>0,"moves generated");
	return(v);
 }
 CommonMoveStack  GetListOfMoves(int who)
 {	CommonMoveStack v = new CommonMoveStack();
 	getMoves(v,who);
 	return(v);
 }
 
 // this is a quick but not definitive test.  If true there
 // definitely are moves.  If false there may or may not be
 private boolean mustHaveLegalMoves(int who)
 {	return((walls[who].height()>0) 
 			|| (meeples[who].height()>0)); 
 
 }
 
 // this is the definitive test.  True only if the player
 // has legal moves other than passing.
 boolean hasLegalMoves(int who)
 {
	return(mustHaveLegalMoves(who) || getMoves(null,who)>0); 
 }
 
 
 int getMoves(CommonMoveStack  all,int who)
 {	int nmoves = 0;
 	buildClusters();	// just to be sure.  progressive search managed to get here without..
 	switch(board_state)
 	{
	case CONFIRM2_STATE:
 	case PASS_STATE:
 		if(all!=null) { all.addElement(new MedinaMovespec("Done",who)); }
 		// don't cound when asking about moves
 		break;
 	case CONFIRM_STATE: 
  		if(all!=null) { all.addElement(new MedinaMovespec("Done",who)); }
 		nmoves++;
 		break;
 	case PLAY_MEEPLE_STATE:
 		{
 		MedinaCell meeple = meeples[who];
 		for(MedinaCell c=allCells; c!=null; c=c.next)
 		{	if((c.topChip()==null) && isInteriorCell(c))
 			{	if(all!=null) 
 				{ all.addElement(new MedinaMovespec(MedinaId.MeepleLocation,meeple.col,meeple.row,c.col,c.row,who));
 				}
 				nmoves++;
 			}
 		}
 		// pick one at random
 		if(all!=null)
 			{int ri = Random.nextInt(new Random(randomKey),nmoves);
 			all.setElementAt(all.elementAt(ri),0);
 			all.setSize(1);
 			}
 		nmoves = 1; 		
 		}
 		break;
 	case DOME_STATE:
 		nmoves += generateDomeMoves(all,domes[who],who);
 		break;
	case PLAY2_STATE:
		// in play2 state, we can place tea tiles
		nmoves += generateTeaMoves(all,teaCards[who],who);
		//$FALL-THROUGH$
	case PLAY_STATE:
 		nmoves += generateMovesFor(all,meeples[who],who);
 		for(int i=0;i<4;i++) { nmoves+=generateMovesFor(all,palaces[who][i],who); }
 		nmoves += generateStableMoves(all,stables[who],who);
 		nmoves += generateDomeMoves(all,domes[who],who);
 		nmoves += generateDomeMoves(all,neutralDomes[who],who);
 		nmoves += generateWallMoves(all,walls[who],who);
 		if(nmoves==0) { if(all!=null) { all.addElement(new MedinaMovespec(PASS,who)); }}
 		break;
	default:
		break;
 	}
   	return(nmoves);
 }
 
}