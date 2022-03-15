package chess;
/**
 * TODO: needs to be more willing to accept draws
 * TODO: ultima needs to be better at checkmate
 */
import online.game.*;

import static chess.ChessMovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
/**
 * ChessBoard knows all about the game of Chess.
 * It gets a lot of logistic support from game.rectBoard, 
 * which knows about the coordinate system.  
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

class ChessBoard extends rectBoard<ChessCell> implements BoardProtocol,ChessConstants
{	static int REVISION = 101;			// revision numbers start at 100
	static final int White_Chip_Index = 0;
	static final int Black_Chip_Index = 1;
	static final ChessId RackLocation[] = { ChessId.White_Chip_Pool,ChessId.Black_Chip_Pool};
	static final ChessId CaptureLocation[] = { ChessId.White_Captured, ChessId.Black_Captured};
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

	public int getMaxRevisionLevel() { return(REVISION); }
	
	static int ForwardDirection[] = { CELL_UP, CELL_DOWN};
	private int forwardDirection(int who) { return(ForwardDirection[getColorMap()[who]]); }
	
	static int Starting_Row[] = {1,8};
	private int startingRow(int who) { return(Starting_Row[getColorMap()[who]]); }
	
	static int Pawn_Starting_Row[] = { 2,7 };
	private int pawnStartingRow(int who) { return(Pawn_Starting_Row[getColorMap()[who]]); }
	
	static int rookDirections[] = { CELL_UP, CELL_LEFT, CELL_RIGHT, CELL_DOWN };
	static int bishopDirections[] = { CELL_UP_LEFT, CELL_UP_RIGHT, CELL_DOWN_LEFT, CELL_DOWN_RIGHT };
	static int queenDirections[] = { CELL_UP, CELL_LEFT,CELL_RIGHT,CELL_DOWN, 
					CELL_UP_LEFT,CELL_UP_RIGHT,CELL_DOWN_LEFT,CELL_DOWN_RIGHT};
    public int boardColumns;	// size of the board
    public int boardRows;
    public void SetDrawState() { setState(ChessState.Draw); }
    
    public ChessChip rack[] = null;	// the pool of chips for each player.  
    public CellStack animationStack = new CellStack();
    //
    // private variables
    //
    private RepeatedPositions repeatedPositions = null;		// shared with the viewer
    private ChessState board_state = ChessState.Play;	// the current board state
    private ChessState unresign = null;					// remembers the previous state when "resign"
    Variation variation = Variation.Chess;
    public ChessState getState() { return(board_state); } 
	public void setState(ChessState st) 
	{ 	unresign = (st==ChessState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
    private ChessId playerColor[]={ChessId.White_Chip_Pool,ChessId.Black_Chip_Pool};
 	public ChessId getPlayerColor(int p) { return(playerColor[p]); }
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public ChessChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    private StateStack dropState = new StateStack();
    private CellStack captureStack = new CellStack();
    private IStack capeeStack = new IStack();
    public ChessCell lastDest[] = {null,null};				// last spot the opponent dropped, for the UI and en passe captures
    public ChessCell lastSrc[] = {null,null};				// last source for UI and en passe captures
    
    public ChessCell kingLocation[] = {null,null};
    public ChessCell captured[] = new ChessCell[2];
    
    int lastProgressMove = 0;		// last move where a pawn was advanced
    int lastDrawMove = 0;			// last move where a draw was offered
    int robotDepth = 0;		// current depth of robot search.  This is used to make faster wins look better
    						// than slower wins.  It's part of the board so multiple threads have independent values.
  	private StateStack robotState = new StateStack();
  	private CellStack robotLast = new CellStack();
  	private IStack robotCapture = new IStack();
  	public boolean robotBoard = false;
  	
    CellStack occupiedCells[] = new CellStack[2];	// cells occupied, per color
    private boolean kingHasMoved[] = new boolean[2];
    private boolean kingRookHasMoved[] = new boolean[2];
    private boolean queenRookHasMoved[] = new boolean[2];
    private ChessMovespec castleMove = null;
	// factory method
	public ChessCell newcell(char c,int r)
	{	return(new ChessCell(c,r));
	}
    public ChessBoard(String init,long rv,int np,RepeatedPositions rep,int map[],int rev) // default constructor
    {   repeatedPositions = rep;
    	displayParameters.reverse_y = true;			// put white on top
        setColorMap(map);
        doInit(init,rv,np,rev); // do the initialization 
        autoReverseYNormal();		// reverse_y based on the color map
     }


	public void sameboard(BoardProtocol f) { sameboard((ChessBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ChessBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(sameCells(kingLocation, from_b.kingLocation),"kinglocation mismatch");
       	G.Assert(AR.sameArrayContents(playerColor,from_b.playerColor),"playerColor contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
       	G.Assert(sameCells(captured, from_b.captured), "captured mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(dropState.sameContents(from_b.dropState),"dropState mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
        G.Assert(occupiedCells[FIRST_PLAYER_INDEX].size()==from_b.occupiedCells[FIRST_PLAYER_INDEX].size(),"occupiedCells mismatch");
        G.Assert(occupiedCells[SECOND_PLAYER_INDEX].size()==from_b.occupiedCells[SECOND_PLAYER_INDEX].size(),"occupiedCells mismatch");
        G.Assert(AR.sameArrayContents(kingHasMoved,from_b.kingHasMoved),"king moved mismatch");
        G.Assert(AR.sameArrayContents(kingRookHasMoved,from_b.kingRookHasMoved),"king rook moved mismatch");
        G.Assert(AR.sameArrayContents(queenRookHasMoved,from_b.queenRookHasMoved),"queen rook moved mismatch");
        
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");

    }
    
    @SuppressWarnings("unused")
	private void checkOccupied()
    {	/*for(int j=0;j<2;j++)
    	{
    	CellStack cs = occupiedCells[j];
    	ChessId targetColor = playerColor[j];
    	for(int i=0;i<cs.size();i++)
    		{
    			ChessCell c = cs.elementAt(i);
    			ChessChip top = c.topChip();
    			G.Assert((top!=null) && (top.color==targetColor),"incorrectly occupied");
    		}
    	}*/
    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are germane to the ordinary operation
     * of the game, others are for system record keeping use; so it is important that the
     * game Digest be consistent both within a game and between games over a long period
     * of time which have the same moves. 
     * (1) Digest is used by the default implementation of EditHistory to remove moves
     * that have returned the game to a previous state; ie when you undo a move or
     * hit the reset button.  
     * (2) Digest is used after EditHistory to verify that replaying the history results
     * in the same game as the user is looking at.  This catches errors in implementing
     * undo, reset, and EditHistory
	 * (3) Digest is used by standard robot search to verify that move/unmove 
	 * returns to the same board state, also that move/move/unmove/unmove etc.
	 * (4) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (5) games where repetition is forbidden (like xiangqi/arimaa) can also use this
     * information to detect forbidden loops.
	 * (6) Digest is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     * looks for duplicate digests.  
     * (7) digests are also used in live play to detect "parroting" by running two games
     * simultaneously and playing one against the other.
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

		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,captureStack);
		v ^= Digest(r,capeeStack);
		v ^= Digest(r,captured);
		v ^= Digest(r,kingLocation);
		v ^= Digest(r,kingHasMoved);
		v ^= Digest(r,kingRookHasMoved);
		v ^= Digest(r,queenRookHasMoved);
		v ^= Digest(r,occupiedCells[SECOND_PLAYER_INDEX].size());	// not completely specific because the stack can be shuffled
		v ^= Digest(r,occupiedCells[FIRST_PLAYER_INDEX].size());
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public ChessBoard cloneBoard() 
	{ ChessBoard copy = new ChessBoard(gametype,randomKey,players_in_game,repeatedPositions,getColorMap(),revision);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((ChessBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ChessBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(kingLocation,from_b.kingLocation);
        copyFrom(captured,from_b.captured);
        dropState.copyFrom(from_b.dropState);
        getCell(captureStack,from_b.captureStack);
        capeeStack.copyFrom(from_b.capeeStack);
        getCell(occupiedCells,from_b.occupiedCells);
        AR.copy(kingHasMoved,from_b.kingHasMoved);
        AR.copy(kingRookHasMoved,from_b.kingRookHasMoved);
        AR.copy(queenRookHasMoved,from_b.queenRookHasMoved);
        
        AR.copy(playerColor,from_b.playerColor);
        board_state = from_b.board_state;
        lastProgressMove = from_b.lastProgressMove;
        lastDrawMove = from_b.lastDrawMove;
        castleMove = from_b.castleMove;
        getCell(lastDest,from_b.lastDest);
        getCell(lastSrc,from_b.lastSrc);
        unresign = from_b.unresign;
        repeatedPositions = from_b.repeatedPositions;
        robotBoard = from_b.robotBoard;
        sameboard(from_b);
    }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game,revision);
    }
    private int playerIndex(ChessChip top)
    {
    	return((playerColor[FIRST_PLAYER_INDEX]==top.color) ? FIRST_PLAYER_INDEX : SECOND_PLAYER_INDEX);
    }
    private void addChip(ChessCell c,ChessChip top)
    {	G.Assert(c.onBoard,"must be on the board");
    	G.Assert(c.topChip()==null,"must be empty");
    	c.addChip(top);
    	int ci = playerIndex(top);
    	if(top.isKing())
    	{	kingLocation[ci] = c;
    	}
    	G.Assert(top.color==playerColor[ci],"matching color");
    	occupiedCells[ci].push(c);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int np,int rev)
    {  	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
		adjustRevision(rev);
    	Grid_Style = GRIDSTYLE; //coordinates left and bottom
    	randomKey = rv;
    	players_in_game = np;
		rack = new ChessChip[2];
    	Random r = new Random(67246765);
     	for(int i=0,pl=FIRST_PLAYER_INDEX;i<2; i++,pl=nextPlayer[pl])
    	{
     	kingLocation[i] = null;
     	occupiedCells[i] = new CellStack();
     	AR.setValue(kingHasMoved, false);
     	AR.setValue(queenRookHasMoved, false);
     	AR.setValue(kingRookHasMoved, false);
     	captured[i] = new ChessCell(CaptureLocation[i],i);
     	}    
    	int colorMap[] = getColorMap();
    	int second = colorMap[SECOND_PLAYER_INDEX];
    	int first = colorMap[FIRST_PLAYER_INDEX];
     	variation = Variation.findVariation(gtype);
     	switch(variation)
     	{
     	default:  throw G.Error(WrongInitError,gtype);
     	case Chess:
     	case Chess960:
    	case Ultima:
     		boardColumns = variation.size;
     		boardRows = variation.size;
     		initBoard(boardColumns,boardRows);
 
      		gametype = gtype;
     		break;
      	}

        allCells.setDigestChain(r);
	    setState(ChessState.Puzzle);
 		playerColor[first]=ChessId.White_Chip_Pool;
		playerColor[second]=ChessId.Black_Chip_Pool;

	    // fill the board with the background tiles
	    for(ChessCell c = allCells; c!=null; c=c.next)
	    {  int i = (c.row+c.col)%2;
	       c.addChip(ChessChip.getTile(i^1));
	    }
	    switch(variation)
	    {
	    case Chess:
	    {
	    	{int col = 0;
	    	for(ChessChip ch : ChessChip.blackInit)
	    	{	addChip(getCell((char)('A'+col),Starting_Row[SECOND_PLAYER_INDEX]),ch);
	    		addChip(getCell((char)('A'+col),Pawn_Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackPawn);
	    	    col++;
	    	}}
	    	{int col = 0;
	    	for(ChessChip ch : ChessChip.whiteInit)
	    	{	addChip(getCell((char)('A'+col),Starting_Row[FIRST_PLAYER_INDEX]),ch);
	    	    addChip(getCell((char)('A'+col),Pawn_Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whitePawn);
	    	    col++;
	    	}}
	    	rack[first] = ChessChip.whitePawn;
	    	rack[second] = ChessChip.blackPawn;
	    	}
	    	break;
	    case Ultima:
	    	{int col = 0;
	    	for(ChessChip ch : revision>100 
	    			? ChessChip.blackUltimaInit1 
	    			: ChessChip.blackUltimaInit0)
	    	{	addChip(getCell((char)('A'+col),Starting_Row[SECOND_PLAYER_INDEX]),ch);
	    	    addChip(getCell((char)('A'+col),Pawn_Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackCustodialPawn);
	    	    col++;
	    	}}
	    	{int col = 0;
	    	for(ChessChip ch : ChessChip.whiteUltimaInit)
	    	{	addChip(getCell((char)('A'+col),Starting_Row[FIRST_PLAYER_INDEX]),ch);
	    	    addChip(getCell((char)('A'+col),Pawn_Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whiteCustodialPawn);
	    	    col++;
	    	}}
	    	rack[first] = ChessChip.whiteImmobilizer;
	    	rack[second] = ChessChip.blackImmobilizer;
	    	break;
	    case Chess960:
	    {	IStack all = new IStack();
	    	Random random = new Random(rv);
	    	for(int i=0;i<ncols; i++) { all.push(i); }
	    	int kingCol = 1+random.nextInt(ncols-2);
	    	// rooks on opposite sides of the king
	    	int lrookCol = random.nextInt(kingCol);
	    	int rrookCol = 1+kingCol+random.nextInt(ncols-kingCol-1);
	    	all.removeValue(kingCol,true);
	    	all.removeValue(lrookCol,true);
	    	all.removeValue(rrookCol,true);
	    	// bishops on opposite colors
	    	int lbishopCol = all.elementAt(random.nextInt(all.size()));
	    	all.removeValue(lbishopCol,true);
	    	int rbishopCol = lbishopCol;
	    	while(((rbishopCol^lbishopCol)&1)==0) { rbishopCol = all.elementAt(random.nextInt(all.size())); }    	
	    	all.removeValue(rbishopCol,true);
	    	int queenCol = all.elementAt(random.nextInt(all.size()));
	    	all.removeValue(queenCol,true);
	    	int lknightCol = all.pop();
	    	int rknightCol = all.pop();
	    	
	    	addChip(getCell((char)('A'+kingCol),Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whiteKing);
	    	addChip(getCell((char)('A'+kingCol),Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackKing);
	    	addChip(getCell((char)('A'+queenCol),Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whiteQueen);
	    	addChip(getCell((char)('A'+queenCol),Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackQueen);
	    	
	    	addChip(getCell((char)('A'+lbishopCol),Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whiteBishop);
	    	addChip(getCell((char)('A'+lbishopCol),Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackBishop);
	    	addChip(getCell((char)('A'+rbishopCol),Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whiteBishop);
	    	addChip(getCell((char)('A'+rbishopCol),Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackBishop);
	    	
	    	addChip(getCell((char)('A'+lknightCol),Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whiteKnight);
	    	addChip(getCell((char)('A'+lknightCol),Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackKnight);
	    	addChip(getCell((char)('A'+rknightCol),Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whiteKnight);
	    	addChip(getCell((char)('A'+rknightCol),Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackKnight);
	    	
	    	addChip(getCell((char)('A'+lrookCol),Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whiteRook);
	    	addChip(getCell((char)('A'+lrookCol),Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackRook);
	    	addChip(getCell((char)('A'+rrookCol),Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whiteRook);
	    	addChip(getCell((char)('A'+rrookCol),Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackRook);
	    	
	    	for(int col = 0; col<ncols;col++)
	    	{		    	
	    		addChip(getCell((char)('A'+col),Pawn_Starting_Row[SECOND_PLAYER_INDEX]),ChessChip.blackPawn);
	    		addChip(getCell((char)('A'+col),Pawn_Starting_Row[FIRST_PLAYER_INDEX]),ChessChip.whitePawn);
	    	}
	    	rack[first] = ChessChip.whitePawn;
	    	rack[second] = ChessChip.blackPawn;
	    	}
	    	break;
	    	
	    default: break;

	    }

	    lastProgressMove = 0;
	    lastDrawMove = 0;
	    castleMove = null;
	    robotDepth = 0;
	    robotState.clear();
	    robotLast.clear();
	    robotCapture.clear();
	    whoseTurn = FIRST_PLAYER_INDEX;
		acceptPlacement();
        AR.setValue(win,false);
        moveNumber = 1;
        // note that firstPlayer is NOT initialized here
    }
    
    private double pawnStructure(ChessCell c,int player)
    {	double val = 0;
    	int forward = forwardDirection(player);
    	for(int dir=0;dir<c.geometry.n;dir++)
    	{	
    		ChessCell adj = c.exitTo(dir);
    		if(adj!=null)
    		{
    		if((adj.row-c.row)==forward)
    		{	ChessChip atop = adj.topChip();
    			if((atop!=null)&&(playerIndex(atop)==player))
    			{if(adj.col==c.col)
    			{
    			// stacked pawns
    			val -= ChessPiece.Pawn.value/100.0;
    			}
    			else
    			{
    			// protecting pawns	
    			val += ChessPiece.Pawn.value/100.0;
    			}}
    		}
    		}

    	}   	
    	return(val);
    }
    public double scoreStartingValue(int who)
    {	double v = 0;
    	CellStack s = occupiedCells[who];
    	for(int lim=s.size()-1; lim>=0; lim--)
    	{	ChessCell cell = s.elementAt(lim);
    		ChessChip top = cell.topChip();
    		ChessPiece piece = top.piece;
    		v += piece.value;
    		switch(piece)
    		{
    		default: break;
    		case Pawn:
    			v += pawnStructure(cell,who);
    			break;
    		case Immobilizer:
    			v += scoreImmobilizer(cell,who,false);
    			break;
    		case Chamelion:
    			v += scoreImmobilizer(cell,who,true);
    			break;
    		}
    	}
    	return(v);
    }
    //
    // intended for endgame, add a bonus for the
    // squares currently acessible to the king.
    // this is used to incentivize squeezing the king
    // into ever smaller areas, as in king vs rook endings
    //
    private double scoreSmotherKing(int who)
    {	double val = 0;
    	int ncap = captured[who].height();
    	if(ncap>ncols)
    	{	// more than half captured
    	double range = sweepKingRange(who);
    	double quant = ((double)(ncols*2-ncap))/(ncols*nrows);
    	// value starts low, gets greater as the number captured increases
    	val += range*quant;
    	// make the other pieces avoid the enemy king.  This 
    	// influence has to be weaker than the desire to enclose
    	ChessCell k = kingLocation[whoseTurn];
    	if(k!=null)
    	{
    	CellStack occ = occupiedCells[nextPlayer[whoseTurn]];
    	for(int lim=occ.size()-1; lim>=0; lim--)
    	{	ChessCell op = occ.elementAt(lim);
    		double dis = G.distance(k.row, k.col, op.row, op.col);
    		val += quant/(dis+1);
    	}}
    	}	
    	return(val);
    }
    // score bonus for immobilizing things
    private double scoreImmobilizer(ChessCell c,int who,boolean chamelion)
    {	double val = 0;
    	for(int direction=c.geometry.n; direction>0; direction--)
    	{
    		ChessCell adj = c.exitTo(direction);
    		if(adj!=null)
    		{
    			ChessChip top = adj.topChip();
    			if( (top!=null) 
    				&& (playerIndex(top)!=who)
    				// if testing for a chamelion acting as an immobilizer,
    				// only benefit if the victim is a real immobilizer
    				&& (!chamelion || (top.piece==ChessPiece.Immobilizer))
    				)
    			{
    				// opponent piece is immobilized, downgrade it.
    				val += top.piece.value/2.0;
    			}
    		}
    	}
    	return(val);
    }
    
    public double simpleScore(int who)
    {	
    	double val=scoreStartingValue(who); 	
    	val += scoreSmotherKing(who);
    	return(val);
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
        case Puzzle:
            break;
        case Confirm:
        case Draw:
        case AcceptPending:
        case DeclinePending:
        case DrawPending:
        case Filter:
        case Resign:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(nextPlayer[whoseTurn]);
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
        {case Resign:
         case Confirm:
         case Draw:
         case DrawPending:
         case AcceptPending:
         case DeclinePending:
            return (true);

        default:
            return (false);
        }
    }
    
    //
    // this is used by the UI to decide when to display the OFFERDRAW box
    //
    public boolean drawIsLikely()
    {	switch(board_state)
    	{
    	case Play:
    		return((moveNumber - lastProgressMove)>10);
    	default: return(false);
    	}
    	
    }
    //
    // declare the game over, and the winner and loser
    //
    void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=false; } // simultaneous win is a win for player2
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(ChessState.Gameover);
    }
    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	// we maintain the wins in doDone so no logic is needed here.
    	if(board_state.GameOver()) { return(win[player]); }
    	return(false);
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print)
    {  	double finalv=simpleScore(player);
    	if(print) { G.print("Eval "+player+" = "+finalv);}
    	return(finalv);
    }


    //
    // finalize all the state changes for this move.
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        castleMove = null;
        droppedDestStack.clear();
        dropState.clear();
        captureStack.clear();
        capeeStack.clear();
        pickedSourceStack.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	ChessCell dr = droppedDestStack.pop();
    	setState(dropState.pop());
    	switch(dr.rackLocation())
	    	{
	   		default: throw G.Error("Not expecting rackLocation %s",dr.rackLocation);
			case BoardLocation: 
				ChessChip ch = pickedObject = dr.removeTop();
				int ci = playerIndex(ch);
				G.Assert(ch.color==playerColor[ci],"matching color");
				occupiedCells[ci].remove(dr,false); 
				if(pickedObject.isKing())
				{
					kingLocation[ci] = getSource();
				}
				undoCaptures(); 
				break;
			case White_Chip_Pool:	// treat the pools as infinite sources and sinks
			case Black_Chip_Pool:	
			case White_Captured:
			case Black_Captured:
				pickedObject = dr.removeTop();
				break;	// don't add back to the pool
	    	
	    	}
	    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	ChessChip po = pickedObject;
    	if(po!=null)
    	{
    		ChessCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case BoardLocation: 
    				ps.addChip(po);
    				int ci = playerIndex(po);
    				G.Assert(po.color==playerColor[ci],"matching color");
    				occupiedCells[ci].push(ps);
    				break;
    		case White_Chip_Pool:
    		case Black_Chip_Pool:	break;	// don't add back to the pool
    		}
    		pickedObject = null;
     	}
     }
    
    // return the cell where an en pass capture would be legal
    private ChessCell canCaptureEnpass(int who)
    {	int prev = nextPlayer[who];
    	ChessCell dest = lastDest[prev];
    	if(dest!=null)
    	{
    	ChessChip top = dest.topChip();
    	if((top!=null)&&(top.piece==ChessPiece.Pawn))
    	{
    		ChessCell src = lastSrc[prev];
    		if(Math.abs(src.row-dest.row)==2) { return(dest);}
    	}}
    	return(null);
    }
    
    private ChessCell canCaptureEnPass(ChessCell from, ChessCell to,int who)
    {	ChessCell victim = canCaptureEnpass(who);
    	if((victim!=null) && (from.row==victim.row) && (to.col==victim.col)) { return(victim); }
    	return(null);
    }
    private void doIndirectCaptures(ChessCell from,ChessCell to,ChessPiece pieceType,int who,replayMode replay,boolean chamelion)
    {	if(to.onBoard)	// ignore suicide moves
    	{
		switch(pieceType)
		{
		case Pawn:
			// normal chess pawn can capture en passe
			ChessCell victim = canCaptureEnPass(from,to,who);
			if(victim!=null)
				{
				doCapture(victim,replay);
				}
			break;
		case Withdrawer:
			{ int dir = findDirection(to,from);
			  ChessCell adj = from.exitTo(dir);
			  if(adj!=null)
			  {
				  ChessChip top = adj.topChip();
				  if((top!=null) && ((playerIndex(top)!=who)) && (!chamelion||(top.piece==pieceType)))
				  {	doCapture(adj,replay);
				  }
			  }
			}
			break;
		case Coordinator:
			{
			ChessCell king = kingLocation[who];
			if(king!=null)
			{
			ChessCell c1 = getCell(king.col,to.row);
			ChessChip c1top = c1.topChip();
			if((c1top!=null) && (playerIndex(c1top)!=who) && (!chamelion||(c1top.piece==pieceType)))
				{ doCapture(c1,replay); 
				}
			ChessCell c2 = getCell(to.col,king.row);
			ChessChip c2top = c2.topChip();
			if((c2top!=null) && (playerIndex(c2top)!=who) && (!chamelion||(c2top.piece==pieceType))) 
				{ doCapture(c2,replay); 
				}
			}}
			break;
		case LongLeaper:
			{	int dir = findDirection(from,to);
				ChessCell next = from;
				while( (next=next.exitTo(dir))!=to)
				{	ChessChip top = next.topChip();
					if((top!=null) && (playerIndex(top)!=who) && (!chamelion||(top.piece==pieceType))) 
					{ doCapture(next,replay); 
					}
				}
			}
			break;
		case CustodialPawn:
			{
				for(int direction : rookDirections)
				{	ChessCell adj = to.exitTo(direction);
					if(adj!=null)
					{	ChessChip top = adj.topChip();
						if((top!=null) && (playerIndex(top)!=who))
						{	ChessCell next = adj.exitTo(direction);
							if(next!=null)
							{	ChessChip nextTop = next.topChip();
								if((nextTop!=null) && (playerIndex(nextTop)==who)  && (!chamelion||(top.piece==pieceType)))
								{	doCapture(adj,replay);
								}
							}
						}
					}
				}
			}
			break;
		case Chamelion:
			doIndirectCaptures(from,to,ChessPiece.Withdrawer,who,replay,true);
			doIndirectCaptures(from,to,ChessPiece.Coordinator,who,replay,true);
			doIndirectCaptures(from,to,ChessPiece.LongLeaper,who,replay,true);
			if((from.col==to.col)||(from.row==to.row))
				{ // capture pawns only if moving like a pawn
				  doIndirectCaptures(from,to,ChessPiece.CustodialPawn,who,replay,true);
				}
			break;
		default: break;
		}
    	}
    }
    // 
    // drop the floating object.
    //
    private void dropObject(ChessCell c,replayMode replay)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation:
    		if(c.topChip()!=null)
    			{	doCapture(c,replay);
    			}
			c.addChip(pickedObject);
			int ci = playerIndex(pickedObject);
			G.Assert(pickedObject.color==playerColor[ci],"matching color");
			occupiedCells[ci].push(c);
			if(pickedObject.isKing())
			{	kingLocation[ci] = c;
			}
			break;
		case Black_Captured:
		case White_Captured:
			c.addChip(pickedObject);
			break;
		case White_Chip_Pool:
		case Black_Chip_Pool:	break;	// don't add back to the pool
		}
    	dropState.push(board_state);
       	droppedDestStack.push(c);
       	pickedObject = null;
    }
    
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(ChessCell c,int lvl)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	G.Assert(!c.isEmpty(),"should have a chip");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case BoardLocation: 
			
			ChessChip ch = pickedObject = c.removeTop();
			int ci = playerIndex(ch);
			G.Assert(ch.color==playerColor[ci],"matching color");
			occupiedCells[ci].remove(c,false);
			break;
		case White_Captured:
		case Black_Captured:
		case White_Chip_Pool:
		case Black_Chip_Pool:	
			pickedObject = lvl<0 ? c.topChip() : c.removeChipAtIndex(lvl);
			break;	// don't add back to the pool
    	
    	}
    	pickedSourceStack.push(c);
   }

    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ChessCell cell)
    {	return(droppedDestStack.top()==cell);
    }
    //
    // get the last dropped dest cell
    //
    public ChessCell getDest() 
    { return(droppedDestStack.top()); 
    }
    
    public ChessCell getPrevDest()
    {
    	return(lastDest[nextPlayer[whoseTurn]]);
    }
    public ChessCell getPrevSource()
    {	return(lastSrc[nextPlayer[whoseTurn]]);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  Returns +100 if a king is the moving object.
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	ChessChip ch = pickedObject;
    	if(ch!=null)
    		{ int nn = ch.chipNumber();
    		  return(nn);
    		}
        return (NothingMoving);
    }
   // get a cell from a partucular source
    public ChessCell getCell(ChessId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(getCell(col,row));
        case White_Captured:
        	return(captured[White_Chip_Index]);
        case Black_Captured:
        	return(captured[Black_Chip_Index]);
       }
    }
    //
    // get the local cell which is the same role as c, which might be on
    // another instance of the board
    public ChessCell getCell(ChessCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case Confirm:
        case Draw:
        	setNextStateAfterDone(!robotBoard); 
        	break;
        case Check:
        case Play:
        	if(!robotBoard && attackingKing(whoseTurn))
        	{
        	setState(ChessState.Check);
        	}
        	else
        	{
  			setState(ChessState.Confirm);
        	}
			break;
        case Filter:
        	break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(ChessCell c)
    {	return(getSource()==c);
    }
    public ChessCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }
    
    // the second source is the previous stage of a multiple jump
    public boolean isSecondSource(ChessCell cell)
    {	int h =pickedSourceStack.size();
    	return((h>=2) && (cell==pickedSourceStack.elementAt(h-2)));
    }

    //
    // we don't need any special state changes for picks.
    //
    private void setNextStateAfterPick()
    {
    }
    
    private void setNextStateAfterDone(boolean strict)
    {
       	switch(board_state)
    	{

    	default: throw G.Error("Not expecting state %s",board_state);
    	case Gameover: 
    		break;
        case DrawPending:
        	lastDrawMove = moveNumber;
        	setState(ChessState.AcceptOrDecline);
        	break;
    	case AcceptPending:
        case Draw:
        	setGameOver(false,false);
        	break;
        case Filter:	// filter moves for checkmate test
        	break;
    	case Confirm:
       	case DeclinePending:
       	case Puzzle:
    	case Play:
    		if(kingLocation[whoseTurn]==null) 
    			{ setGameOver(false,true); 
    			}
    		else if(kingLocation[nextPlayer[whoseTurn]]==null)
    			{ setGameOver(true,false); 
    			}
    		else if(hasSimpleMoves())
    			{
    			  if(!strict) 
    			  { // the robot plays a slightly different game, where
    				// checks don't exist and the king is actually captured
    				// to win the game. This results in the same attacking
    				// and winning moves, without the cost of checking for
    				// check and checkmate.
    				setState(ChessState.Play); 
    			  }
    			  else 
    			  {
    			  setState(ChessState.Filter);
    			  if(attackingKing(whoseTurn))
    				{
    				 if(hasEscapeCheckMoves())
    				 {
    					 setState(ChessState.Check);
    				 }
    				 else { setGameOver(false,true); }
    				}
    				else if(hasEscapeCheckMoves())
    				{setState(ChessState.Play); 
    				}
    				else { setGameOver(false,false); }	// stalemate
    			  }
    			}
    		else { setGameOver(false,false); } // no moves, stalemate
     		break;
    	}

    }

    private void doDone(replayMode replay,boolean strict)
    {	ChessCell dest = getDest();
    	ChessCell src = getSource();
    	lastDest[whoseTurn] = dest;
    	lastSrc[whoseTurn] = src;
    	lastProgressMove = moveNumber; 
      	if(dest!=null)
      	{
    	ChessChip lastMoved = dest.topChip();
    	if(lastMoved!=null)
    	{
    	switch(lastMoved.piece)
    	{
    	default: break;
    	case King: 	
	    	{	int dcol = dest.col-src.col;
	    		kingHasMoved[whoseTurn]=true; 
	    		switch(variation)
	    		{
	    		case Chess960: break;
	    		default:
	    		case Chess:
	    		if(dcol==2)
	    		{	// castle king side
	    			ChessCell rd = dest.exitTo(CELL_LEFT);
	    			ChessCell rf = dest.exitTo(CELL_RIGHT);
	    			rd.addChip(rf.removeTop());
	    			occupiedCells[whoseTurn].remove(rf,false);
	    			occupiedCells[whoseTurn].push(rd);
	    			if(replay!=replayMode.Replay)
	    			{
	    				animationStack.push(rf);
	    				animationStack.push(rd);
	    			}
	    		}
	    		else if(dcol==-2)
	    		{	// castle queen side
	    			ChessCell rd = dest.exitTo(CELL_RIGHT);
	    			ChessCell rf = dest.exitTo(CELL_LEFT).exitTo(CELL_LEFT);
	    			rd.addChip(rf.removeTop());
	    			occupiedCells[whoseTurn].remove(rf,false);
	    			occupiedCells[whoseTurn].push(rd);
	    			if(replay!=replayMode.Replay)
	    			{
	    				animationStack.push(rf);
	    				animationStack.push(rd);
	    			}
	    		}
	    	}}
    				break;
    	case Rook:	if(src.row==startingRow(whoseTurn))
    				{	if(src.col==ChessChip.kingRookCol)
    					{
    					kingRookHasMoved[whoseTurn] = true;
    					}
    					else 
    					{ queenRookHasMoved[whoseTurn] = true;
    					}
    				}
    				break;
    	case Pawn:	// possible promotion
    		if(dest.row==startingRow(nextPlayer[whoseTurn]))
    		{ 	// handle chess pawn promotion
        		dest.removeTop();
        		int colorIndex = getColorMap()[whoseTurn];
        		dest.addChip(ChessChip.PromotedPawnChip[colorIndex]);
        		doCapture(dest,replay);
        		dest.addChip(ChessChip.QueenChip[colorIndex]);
        		occupiedCells[whoseTurn].push(dest);
        		checkOccupied();
        		if(replay!=replayMode.Replay)
        		{	
        		    animationStack.push(captured[whoseTurn]);
        		    animationStack.push(dest);
        		}
        		}
    	}}}
		doRobotCapture();
      	acceptPlacement();

        if (board_state==ChessState.Resign)
        {	setGameOver(false,true);
        }
        else
        {	setNextPlayer(); 
        	setNextStateAfterDone(strict); 
        }
    }

    private void doCapture(ChessCell mid,replayMode replay)
    {	
    	ChessChip ch = mid.removeTop();
    	int capee = playerIndex(ch);
    	//G.print("Cap "+mid+" "+ch+" "+captured[capee].height());
		occupiedCells[capee].remove(mid,false);
		captureStack.push(mid);
		capeeStack.push(capee);
		captured[capee].addChip(ch);
		if(ch.isKing())
		{
			kingLocation[capee]=null;
		}
		if(replay!=replayMode.Replay)
		{	animationStack.push(mid);
			animationStack.push(captured[capee]);
		}
    }
    private void undoCaptures()
    {	while(captureStack.size()>0)
    	{
    	ChessCell cap = captureStack.pop();
    	int capee = capeeStack.pop();
		ChessChip chip = captured[capee].removeTop();
		cap.addChip(chip);
		//G.print("Uncap "+cap+" "+captured[capee].height());
		if(chip.isKing())
		{
			kingLocation[capee] = cap;
		}
		occupiedCells[capee].push(cap);
    	}
    }
    private void doRobotCapture()
    {
		if(robotBoard)
		{	int cs = captureStack.size();
			if(cs>0)
			{
        	for(int i=0;i<cs;i++)
        		{ robotLast.push(captureStack.pop()); 
        		  robotCapture.push(capeeStack.pop());
        		}
			}
			robotCapture.push(cs);
			
		}
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	ChessMovespec m = (ChessMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
       // System.out.println("E "+m+" for "+whoseTurn);
        checkOccupied();
        switch (m.op)
        {
        case MOVE_STALEMATE:
        	setState(ChessState.AcceptPending);
        	break;
        case MOVE_DONE:

         	doDone(replay,!robotBoard);

            break;
        case MOVE_SUICIDE:
        	{
			ChessCell src = getCell(m.source, m.from_col, m.from_row);
			doCapture(src,replay);
			setNextStateAfterDrop();
        	}
        	break;

        case MOVE_CASTLE:
        	{
           	// castles are coded as though the king were going to capture its own rook.
        	// chess960 castles such that the king and rook end in the same positions
        	// as castling in normal chess, regardless how that translates into moves
        	// of the king and rook.
        	if(pickedObject!=null) 
        		{ G.Assert(pickedObject.isKing(),"not the king");
        		  unPickObject();
        		}
        	ChessCell king = getCell(m.from_col,m.from_row);
        	ChessCell rook = getCell(m.to_col,m.to_row);
        	ChessChip r = rook.topChip();
        	ChessChip k = king.topChip();
        	G.Assert(r.piece==ChessPiece.Rook && k.piece==ChessPiece.King,"incorrect pieces");
        	rook.removeTop();
        	pickObject(king,-1);
        	
        	ChessCell rdest =null;
        	ChessCell kdest =null;
        	if(rook.col<king.col)
        	{
        		// king side castle
        		rdest = getCell('D',rook.row);
        		kdest = getCell('C',rook.row);
        	}
        	else 
        	{	// queen side castle
        		rdest = getCell('F',rook.row);
        		kdest = getCell('G',rook.row);
        	}
    		rdest.addChip(r);
    		dropObject(kdest,replay);

    		occupiedCells[whoseTurn].remove(rook,false);
    		occupiedCells[whoseTurn].push(rdest);
    		
    		if(replay!=replayMode.Replay)
    		{
    			animationStack.push(rook);
    			animationStack.push(rdest);
        	}
        	kingHasMoved[whoseTurn] = true;		// sometimes it makes no apparent move, but it has
        	castleMove = m;
        	setState(ChessState.Confirm);
        	}
        	break;

        case MOVE_BOARD_BOARD:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Check:
        		case Filter:
        		case Play:
        			G.Assert(pickedObject==null,"something is moving");
        			ChessCell src = getCell(m.source, m.from_col, m.from_row);
        			ChessCell dest = getCell(m.dest,m.to_col,m.to_row);
           			if(replay!=replayMode.Replay)
        			{
        				animationStack.push(src);
        				animationStack.push(dest);
        			}
        			pickObject(src,-1);
        			m.chip = pickedObject;
        			doIndirectCaptures(src,dest,pickedObject.piece,whoseTurn,replay,false);
        			dropObject(dest,replay); 
        			
 				    setNextStateAfterDrop();
        			break;
        	}
        	break;
        case MOVE_DROPB:
			{
			ChessCell dest = getCell(ChessId.BoardLocation, m.to_col, m.to_row);
			ChessCell src = getSource();
        	G.Assert(pickedObject!=null,"something is moving");
			if(dest==src) 
            	{ 
            	  unPickObject(); 

            	} 
            	else
            		{
        			if(board_state!=ChessState.Puzzle) 
        				{doIndirectCaptures(src,dest,pickedObject.piece,whoseTurn,replay,false);
        				}
            			
            		dropObject(dest,replay);
            		
            		setNextStateAfterDrop();
            		if(replay==replayMode.Single)
            			{
            			animationStack.push(getSource());
            			animationStack.push(dest);
            			}
            		}
			}
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	ChessCell dest = getCell(m.from_col,m.from_row);
        	if(isDest(dest))
        		{ 
        		if(castleMove!=null)
        		{
        			// very special case
        			
                	ChessCell king = getCell(castleMove.from_col,castleMove.from_row);
                	ChessCell rook = getCell(castleMove.to_col,castleMove.to_row);
                	ChessCell rdest =null;
                	ChessCell kdest =null;
                	if(rook.col<king.col)
                	{
                		// king side castle
                		rdest = getCell('D',rook.row);
                		kdest = getCell('C',rook.row);
                	}
                	else 
                	{	// queen side castle
                		rdest = getCell('F',rook.row);
                		kdest = getCell('G',rook.row);
                	}
                	unDropObject();
                	rook.addChip(rdest.removeTop());
                	occupiedCells[whoseTurn].remove(rdest,false);
                	occupiedCells[whoseTurn].push(rook);
                	castleMove = null;
                	kingHasMoved[whoseTurn]=false;
       			
        		}
        		else {
        		unDropObject(); 
        		}}
        	else 
        		{ pickObject(getCell(ChessId.BoardLocation, m.from_col, m.from_row),-1);
        		  m.chip = pickedObject;
         		}
 
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	ChessCell c = getCell(m.source, m.to_col, m.to_row);
            dropObject(c,replay);
            setNextStateAfterDrop();
            if(replay==replayMode.Single)
			{
			animationStack.push(getSource());
			animationStack.push(c);
			}}
            break;


        case MOVE_PICK:
        	{
        	ChessCell c = getCell(m.source, m.from_col, m.from_row);
        	if(c==getDest()) { unDropObject(); }
        	else {pickObject(c,m.from_row);
            	  setNextStateAfterPick();
        		}
        	}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(ChessState.Puzzle);
            {	boolean win1 = winForPlayerNow(whoseTurn);
            	boolean win2 = winForPlayerNow(nextPlayer[whoseTurn]);
            	if(win1 || win2) { setGameOver(win1,win2); }
            	else
            	{  setNextStateAfterDone(true); 
            	}
            }
            break;
        case MOVE_OFFER_DRAW:
        	if(board_state==ChessState.DrawPending) { setState(dropState.pop()); }
        	else { dropState.push(board_state);
        			setState(ChessState.DrawPending);
        		}
        	break;
        case MOVE_ACCEPT_DRAW:
           	switch(board_state)
        	{	
        	case AcceptPending: 	// cancel accept and revert to neutral
        		setState(ChessState.AcceptOrDecline); 
        		break;
           	case AcceptOrDecline:
           	case DeclinePending:	// accept pending
           		setState(ChessState.AcceptPending); 
           		break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
           	break;
        case MOVE_DECLINE_DRAW:
        	switch(board_state)
        	{	
        	case DeclinePending:	// cancel decline and revert to neutral
        		setState(ChessState.AcceptOrDecline); 
        		break;
        	case AcceptOrDecline:
        	case AcceptPending: setState(ChessState.DeclinePending); break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
        	break;
        case MOVE_RESIGN:
        	setState(unresign==null?ChessState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            // standardize "gameover" is not true
            setState(ChessState.Puzzle);
 
            break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;

        default:
        	cantExecute(m);
        }

        checkOccupied();

        return (true);
    }

    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player,ChessCell cell,Hashtable<ChessCell,ChessMovespec>targets)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Confirm:
        	return(cell==getDest());
        case Draw:
        case Play: 
        case Filter:
		case Gameover:
		case Resign:
		case AcceptOrDecline:
		case DeclinePending:
		case DrawPending:
		case AcceptPending:
		case Check:
			return(targets.get(cell)!=null);
        case Puzzle:
        	return((pickedObject==null)?true:(player==playerIndex(pickedObject)));
        }
    }
  

    public boolean legalToHitBoard(ChessCell cell,Hashtable<ChessCell,ChessMovespec>targets)
    {	
        switch (board_state)
        {
        case Filter:
        case Check:
 		case Play:
 			return(isSource(cell)||targets.get(cell)!=null);
 		case Resign:
		case Gameover:
		case AcceptOrDecline:
		case DeclinePending:
		case AcceptPending:
		case DrawPending:
			return(false);
		case Confirm:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return(pickedObject==null?!cell.isEmpty():true);
        }
    }
  public boolean canDropOn(ChessCell cell)
  {		ChessCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
 

 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(ChessMovespec m,boolean strict)
    {
        robotState.push(board_state);
        robotLast.push(lastDest[whoseTurn]);
        robotLast.push(lastSrc[whoseTurn]);
        robotCapture.push(kingHasMoved[whoseTurn]?1:0);
        robotCapture.push(kingRookHasMoved[whoseTurn]?1:0);
        robotCapture.push(queenRookHasMoved[whoseTurn]?1:0);
        robotDepth++;
        //G.print("R "+m +" "+robotDepth+" "+robotCapture.size());
      
        if (Execute(m,replayMode.Replay))
        {	
        	
            if (m.op == MOVE_DONE)
            {
            }
            else if ((board_state==ChessState.Filter) || DoneState())
            {	if((robotDepth<=6) && (repeatedPositions.numberOfRepeatedPositions(Digest())>1))
            		{
            		// this check makes game end by repetition explicitly visible to the robot
            		setGameOver(false,false);
            		robotCapture.push(0);
            		}
            else { doDone(replayMode.Replay,strict); }
            }
        }
    }
 

   //
   // un-execute a move.  The move should only be un-executed
   // in proper sequence, and if it was executed by the robot in the first place.
   // If you use monte carlo bots with the "blitz" option this will never be called.
   //
    public void UnExecute(ChessMovespec m)
    {
        // G.print("U "+m+" for "+robotDepth+" "+robotCapture.size()); 
    	checkOccupied();
    	robotDepth--;
        ChessCell dest=null;
        ChessCell src = null;
        if(whoseTurn!=m.player)
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_CASTLE:
   	    	{
   	       	ChessCell king = getCell(m.from_col,m.from_row);
        	ChessCell rook = getCell(m.to_col,m.to_row);        	
        	ChessCell rdest =null;
        	ChessCell kdest =null;
        	if(rook.col<king.col)
        	{
        		// king side castle
        		rdest = getCell('D',rook.row);
        		kdest = getCell('C',rook.row);
        	}
        	else 
        	{	// queen side castle
        		rdest = getCell('F',rook.row);
        		kdest = getCell('G',rook.row);
        	}
    		pickObject(kdest,-1);
    		rook.addChip(rdest.removeTop());
    		dropObject(king,replayMode.Replay);
    		occupiedCells[whoseTurn].remove(rdest,false);
    		occupiedCells[whoseTurn].push(rook);
    		acceptPlacement();
   	    	}
    
   	    	break;
        case MOVE_STALEMATE:
        case MOVE_DONE:
           break;
        case MOVE_SUICIDE:
        	// unusual case, suicide moves capture our own pieces
        	break;
        case MOVE_BOARD_BOARD:
       			{
    			G.Assert(pickedObject==null,"something is moving");
    			dest = getCell(m.dest, m.to_col, m.to_row);
    			pickObject(dest,-1);
    			ChessChip moved = pickedObject;
    			src = getCell(m.source, m.from_col,m.from_row);
   			    dropObject(src,replayMode.Replay); 
   			    acceptPlacement();
   			    
   			    switch(variation)
   			    {
   			    case Chess960: break;
   			    default:
   			    case Chess:
	   			    if(moved.piece==ChessPiece.King)
	   			    {
	   			    	int dcol = dest.col-src.col;
	   		    		if(dcol==2)
	   		    		{	// castle king side
	   		    			ChessCell rf = dest.exitTo(CELL_LEFT);
	   		    			ChessCell rd = dest.exitTo(CELL_RIGHT);
	   		    			rd.addChip(rf.removeTop());
	   		    			occupiedCells[whoseTurn].remove(rf,false);
	   		    			occupiedCells[whoseTurn].push(rd);
	   		    		}
	   		    		else if(dcol==-2)
	   		    		{	// castle queen side
	   		    			ChessCell rf = dest.exitTo(CELL_RIGHT);
	   		    			ChessCell rd = dest.exitTo(CELL_LEFT).exitTo(CELL_LEFT);
	   		    			rd.addChip(rf.removeTop());
	   		    			occupiedCells[whoseTurn].remove(rf,false);
	   		    			occupiedCells[whoseTurn].push(rd);
	   		    		}
	   			    }
   			    	break;
   			    
     			}}

        	break;
 
        case MOVE_RESIGN:
        case MOVE_ACCEPT_DRAW:
        case MOVE_DECLINE_DRAW:
        case MOVE_OFFER_DRAW:
            break;
        }
        
        setState(robotState.pop());
        
        int cap = robotCapture.pop();
        if(cap>0)
        {	
        	while(cap-- > 0)
        	{	captureStack.push(robotLast.pop());
        		capeeStack.push(robotCapture.pop());
        	}
    		undoCaptures();
    		
    		if(dest!=null)
            {	// if the promotion move is also a capture, there are two chips
    			// stacked, the original occupant and the capturing pawn.
    			ChessChip newtop = dest.chipAtIndex(1);
    			int colorIndex = getColorMap()[whoseTurn];
            	if(newtop==ChessChip.PromotedPawnChip[colorIndex])
            	{
            		dest.removeChipAtIndex(1);
            		occupiedCells[playerIndex(newtop)].remove(dest, false);
            		src.removeTop();
            		src.addChip(ChessChip.PawnChip[colorIndex]);
            	}           	
            }
        }
        lastSrc[whoseTurn] = robotLast.pop();
        lastDest[whoseTurn] = robotLast.pop();
        
        queenRookHasMoved[whoseTurn] = robotCapture.pop()!=0;
        kingRookHasMoved[whoseTurn] = robotCapture.pop()!=0;
        kingHasMoved[whoseTurn] = robotCapture.pop()!=0;
    	checkOccupied();
  }

private void loadHash(CommonMoveStack all,Hashtable<ChessCell,ChessMovespec>hash,boolean from)
{
	for(int lim=all.size()-1; lim>=0; lim--)
	{
		ChessMovespec m = (ChessMovespec)all.elementAt(lim);
		switch(m.op)
		{
		default: break;
		case MOVE_CASTLE:
		case MOVE_SUICIDE:
		case MOVE_BOARD_BOARD:
			if(from) { hash.put(getCell(m.source,m.from_col,m.from_row),m); }
			else { hash.put(getCell(m.dest,m.to_col,m.to_row),m); }
		}
		}
}
/**
 * getTargets() is called from the user interface to get a hashtable of 
 * cells which the mouse can legally hit.
 * 
 * Chess uses the move generator for most of the logic of where it's legal
 * for the mouse to pick up or drop something.  We start with the list of legal
 * moves, and select either the legal "from" spaces, or the legal "to" spaces.
 * 
 * The advantage of this approach is that the logic for "legal moves" whatever it
 * may be, is needed anyway to drive the robot, and by reusing the move list we
 * avoid having to duplicate that logic.
 * 
 * @return
 */
public Hashtable<ChessCell,ChessMovespec>getTargets()
{
	Hashtable<ChessCell,ChessMovespec>hash = new Hashtable<ChessCell,ChessMovespec>();
	CommonMoveStack all = new CommonMoveStack();

		switch(board_state)
		{
		default: break;
		case Check:
		case Play:
		case Filter:
			{	addMoves(all,whoseTurn);
				filterCheckMoves(all,whoseTurn);
				loadHash(all,hash,pickedObject==null);
			}
			break;
		}
	return(hash);
}

private void filterCheckMovesInternal(CommonMoveStack all,int who,ChessBoard from)
{	long dig = Digest();
	for(int lim=all.size()-1; lim>=0; lim--)
	{
		ChessMovespec m = (ChessMovespec)all.elementAt(lim);
		RobotExecute(m,false);
		if(attackingKing(who))
		{	all.remove(lim,false);
		}
		UnExecute(m);
		long newdig = Digest();
		if(newdig!=dig)
		{
		sameboard(from);
		G.Assert(newdig==dig,"digest changed");
		}
	}
}
public void filterCheckMoves(CommonMoveStack all,int who)
{
	ChessBoard cp = cloneBoard();
	cp.robotBoard = true;
	cp.unPickObject();
	cp.acceptPlacement();
	ChessBoard fr = cp.cloneBoard();
	cp.filterCheckMovesInternal(all,who,fr);
}
private void filterStalemateMovesInternal(CommonMoveStack all,int who,ChessBoard from)
{	long dig = Digest();
	for(int lim=all.size()-1; lim>=0; lim--)
	{
		ChessMovespec m = (ChessMovespec)all.elementAt(lim);
		RobotExecute(m,true);
		if(gameOverNow())
		{
			if(!winForPlayerNow(who))
				{ all.remove(lim, false); 
				}
		}
		UnExecute(m);
		long newdig = Digest();
		//sameboard(from);
		G.Assert(newdig==dig,"digest changed");
	}
}
public void filterStalemateMoves(CommonMoveStack all)
{
	ChessBoard cp = cloneBoard();
	cp.robotBoard = true;
	cp.unPickObject();
	cp.acceptPlacement();
	cp.filterStalemateMovesInternal(all,whoseTurn,this);
}
public boolean hasSimpleMoves()
{
	return(addSimpleMoves(null,whoseTurn));
}
public boolean hasEscapeCheckMoves()
{	CommonMoveStack all = new CommonMoveStack();
	addMoves(all,whoseTurn);
	filterCheckMoves(all,whoseTurn); 
	return(all.size()>0);
}
private boolean addSuicideMove(CommonMoveStack all,ChessCell cell,int who)
{	if(all!=null)
	{
	all.push(new ChessMovespec(MOVE_SUICIDE,cell,captured[who],who));	// suicide move
	}
	return(true);
}
 // add normal Chess or Ultima moves
 // "all" can be null
 // return true if there are any.
 // this should ignore the complications of check and uncovered check.  In robot games,
 // a move the result in check will be evaluated as a loss.  In human games, a post
 // filter will remove the illegal gestures.
 //
 public boolean addSimpleMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	CellStack pieces = occupiedCells[who];
 	for(int lim=pieces.size()-1; lim>=0; lim--)
 	{	
 		ChessCell cell = pieces.elementAt(lim);
 		ChessChip top = cell.topChip();
 		if(isImmobilized(cell,top,who))
 		{ if(!top.isKing())
 			{ some |= addSuicideMove(all,cell,who);
 			}
 		}
 		else
 		{
 		some |= addSimpleMoves(all, cell,top.piece,who);
 		if(some && (all==null))  { return(true); }
 		}
 	}
 	return(some);
 }
 
 private boolean addPawnCaptureMove(CommonMoveStack all,ChessCell from,int direction,int who)
 {
		ChessCell capleft = from.exitTo(direction);
		if(capleft!=null)
			{
			ChessChip top = capleft.topChip();
			if(((top!=null)&&(playerIndex(top)!=who)) || (canCaptureEnPass(from,capleft,who)!=null))
				{
				nonKingMoves++;
				if(all!=null) { all.push(new ChessMovespec(from,capleft,who)); }
				return(true);
				}
			}
		return(false);
 }
 private boolean addPawnForwardMove(CommonMoveStack all,ChessCell from, int direction,int who)
 {
	 ChessCell forward = from.exitTo(direction);	// forward can't be null because we promote
	 if(forward!=null && forward.topChip()==null)
			{
		 	nonKingMoves++;
			if(all==null) { return(true); }
			all.push(new ChessMovespec(from,forward,who));
			if(from.row==pawnStartingRow(who))
				{
				ChessCell forward2 = forward.exitTo(direction);
				if(forward2.topChip()==null)
				{
				nonKingMoves++;
				all.push(new ChessMovespec(from,forward2,who));
				}
				}
			return(true);
			}
		return(false);
 }
 private boolean addNoncapturingMoves(CommonMoveStack all, ChessCell from, int direction, int who)
 {	ChessCell to = from;
 	boolean some = false;
 	while( ((to=to.exitTo(direction))!=null)
 			&& (to.topChip()==null))
 	{	if(all==null) { return(true); }
 		nonKingMoves++;
 		all.push(new ChessMovespec(from,to,who)); 
 		some = true;
 	}
 	return(some);
 }
 private void sweepNoncapturingMoves(ChessCell from, int direction, int who)
 {	ChessCell to = from;
 	while( ((to=to.exitTo(direction))!=null)
 			&& (to.topChip()==null))
 	{	to.sweep_counter = sweep_counter;
 	}
 }
 private boolean addMoves(CommonMoveStack all, ChessCell from, int direction, int who)
 {	ChessCell to = from;
 	ChessChip top = null;
 	boolean some = false;
 	while( (top==null) && ((to=to.exitTo(direction))!=null))
 	{	top = to.topChip();
 		if((top==null) || (playerIndex(top) != who))
 		{
 			if(all==null) { return(true); }
 			nonKingMoves++;
 			all.push(new ChessMovespec(from,to,who)); 
 			some = true;
 		}
 	}
 	return(some);
 }
 

 private boolean addNoncapturingRookMoves(CommonMoveStack all, ChessCell from, int who)
 {	boolean some = false;
 	for(int direction : rookDirections)
 	{
	some |= addNoncapturingMoves(all,from, direction, who);
	if(some && (all==null)) break;
 	}
 	return(some);
 }
 private void sweepRookMoves(ChessCell from, int who)
 {	for(int direction : rookDirections)
 	{
	sweepNoncapturingMoves(from, direction, who);
 	}
 }
 private void sweepCustodialCapture(ChessCell from,int direction,int who)
 {
	 ChessCell side = from.exitTo(direction);
	 if(side!=null)
	 {
		 ChessChip top = side.topChip();
		 if((top!=null)&&(playerIndex(top)!=who))
		 {
			 ChessCell side2 = side.exitTo(direction);
			 if(side2!=null)
			 {
				 ChessChip top2 = side2.topChip();
				 if((top2!=null) && (playerIndex(top2)==who)) { side.sweep_counter = sweep_counter; }
			 }
			 
		 }
	 }
 }
 private void sweepCustodialPawnMoves(ChessCell from,int direction,int who)
 {
	 while( ((from=from.exitTo(direction))!=null)
			 && (from.topChip()==null))
	 {	sweepCustodialCapture(from,direction+CELL_QUARTER_TURN,who);
	 	sweepCustodialCapture(from,direction-CELL_QUARTER_TURN,who);
	 }
 }
 
 private void sweepCustodialPawnMoves(ChessCell from, int who)
 {	for(int direction : rookDirections)
 	{
	sweepCustodialPawnMoves(from, direction, who);
 	}
 }
 private boolean addMoves(CommonMoveStack all, ChessCell from, int[]directions, int who)
 {	boolean some = false;
 	for(int direction : directions)
 	{
	some |= addMoves(all,from, direction, who);
	if(some && (all==null)) break;
 	}
 	return(some);
 }

 private boolean addSingleMoves(CommonMoveStack all, ChessCell from, int[]directions, int who,boolean chamelion)
 {	boolean some = false;
 	for(int direction : directions)
 	{
 	ChessCell to = from.exitTo(direction);
 	if(to!=null)
 	{
 	ChessChip top = to.topChip();
 	if((chamelion ? ((top!=null) && top.isKing()) : true)
 		&& ((top==null) || (playerIndex(top)!=who)))
 	{
 		if(all==null) { return(true); }
 		some = true;
 		// move by a king
 		all.push(new ChessMovespec(from,to,who));
 	}}}
 	return(some);
 }
 private void sweepSingleMoves(ChessCell from, int[]directions, int who,boolean chamelion)
 {	
 	for(int direction : directions)
 	{
 	ChessCell to = from.exitTo(direction);
 	if(to!=null)
 	{
 	if(chamelion)
	 	{
	 		ChessChip top = to.topChip();
	 		if((top!=null) && top.isKing() && (playerIndex(top)!=who))
	 		{
	 			to.sweep_counter = sweep_counter;
	 		}
	 	}
 	else
	 	{
	 	to.sweep_counter = sweep_counter;
	 	}
 	}
 	}
 }
 private boolean addRookMoves(CommonMoveStack all,ChessCell from,int who)
 {
	 return(addMoves(all,from,rookDirections,who));
 }

 private boolean addBishopMoves(CommonMoveStack all,ChessCell from,int who)
 {	return(addMoves(all,from,bishopDirections,who));
 }
 private void sweepBishopMoves(ChessCell from,int who)
 {	for(int dir : bishopDirections) { sweepNoncapturingMoves(from,dir,who); }
 }
 private boolean addQueenMoves(CommonMoveStack all,ChessCell from,int who)
 {	return(addMoves(all,from,queenDirections,who));
 }
 private void sweepQueenMoves(ChessCell from,int who)
 {	for(int dir : queenDirections) { sweepNoncapturingMoves(from,dir,who);}
 }
 private boolean addKingMoves(CommonMoveStack all,ChessCell from,int who,boolean chamelion)
 {	return(addSingleMoves(all,from,queenDirections,who,chamelion));
 }
 private void sweepKingMoves(ChessCell from,int who,boolean chamelion)
 {	sweepSingleMoves(from,queenDirections,who,chamelion);
 }
 
 private boolean add960CastlingMoves(CommonMoveStack all,ChessCell from,int who)
 {	boolean some = false;
	 if(!kingHasMoved[who] && (board_state!=ChessState.Check))
	 {	ChessCell start = kingLocation[who];
	 	int other = nextPlayer[who];
		if(!kingRookHasMoved[who])
		{	
			int dir = CELL_RIGHT;
			ChessCell next = start.exitTo(dir);
			ChessCell rdest = getCell('F',start.row);	// where the rook will end up
			if((rdest==start) || (rdest.topChip()==null))
			{
			// the eventual destination of the rook has to be vacant or currently occupied by the king
			while(next!=null)
			{
				ChessChip top = next.topChip();
				if(top!=null)
				{
				if(top.piece==ChessPiece.Rook 
						&& (playerIndex(top)==who))
						// special case where the king won't move, the rook hops over
				{	// reached the rook
					if(all==null) { return(true); }
					some = true;
					all.push(new ChessMovespec(MOVE_CASTLE,start,next,who));
				}
				next = null; 
				}
				else if(attackingSquare(next,other)) { next = null; }	// can't move through an attack
				else { 
					next = next.exitTo(dir); 
					}
			}}
		}
		if(!queenRookHasMoved[who])
		{
			int dir = CELL_LEFT;
			ChessCell next = start.exitTo(dir);
			ChessCell rdest = getCell('D',start.row);	// where the rook will end up
			if((rdest==start) || (rdest.topChip()==null))
			{
			while(next!=null)
			{
				ChessChip top = next.topChip();
				if(top!=null)
				{
				if(top.piece==ChessPiece.Rook 
						&& (playerIndex(top)==who))
				{	// reached the rook
					if(all==null) { return true; }
					some = true;
					all.push(new ChessMovespec(MOVE_CASTLE,start,next,who));
				}
				next = null;
				}
				else if(attackingSquare(next,other)) { next = null; }	// can't move through an attack
				else { 
					next = next.exitTo(dir); 
					}
			}}
		}
	 }
	 return(some);
 }
 private boolean addCastlingMoves(CommonMoveStack all,ChessCell from,int who)
 {
	 switch(variation)
	 {
	 case Chess960:	return add960CastlingMoves(all,from,who);
	 case Chess: return(addStandardCastlingMoves(all,from,who));
	 default: throw G.Error("not expecting %s",variation);
	 }
 }
 private boolean addStandardCastlingMoves(CommonMoveStack all,ChessCell from,int who)
 {
	 if(!kingHasMoved[who] && (board_state!=ChessState.Check))
	 {	ChessCell start = kingLocation[who];
	 	int other = nextPlayer[who];
		if(!kingRookHasMoved[who])
		{	
			int dir = CELL_RIGHT;
			ChessCell next = start.exitTo(dir);
			if(next.topChip()==null && !attackingSquare(next,other))
			{	ChessCell next2 = next.exitTo(dir);
				if((next2!=null) && (next2.topChip()==null) 
						&& !attackingSquare(next2,other)
						)
				{	ChessChip rook = next2.exitTo(dir).topChip();
					// hasMoved doesn't track captures, so we need to check
					// that the rook is still there.
					if((rook!=null)
							&& (rook.piece==ChessPiece.Rook)
							&& (playerIndex(rook)==who))
					{
					if(all==null) { return(true); }
					all.push(new ChessMovespec(start,next2,who));
					}
				}
			}
		}
		if(!queenRookHasMoved[who])
		{
			int dir = CELL_LEFT;
			ChessCell next = start.exitTo(dir);
			if((next.topChip()==null) && !attackingSquare(next,other))
			{	ChessCell next2 = next.exitTo(dir);
				if((next2.topChip()==null)&&!attackingSquare(next2,other))
				{	ChessCell next3 = next2.exitTo(dir);
					if(next3.topChip()==null)
					{
					ChessCell qr = next3.exitTo(dir);
					ChessChip rook = (qr!=null) ? qr.topChip() : null;
					// hasMoved doesn't track captures, so we need to check
					// that the rook is still there.
					if((rook!=null)
							&&(rook.piece==ChessPiece.Rook)
							&&(playerIndex(rook)==who))
					{
					if(all==null) { return(true); }
					all.push(new ChessMovespec(start,next2,who));
					}}
				}
			}
		
		}
	 }
	 return(false);
 }
 private boolean addKnightStepMoves(CommonMoveStack all,ChessCell from,ChessCell step, int direction,int who)
 {	if(step!=null)
 	{	ChessCell to = step.exitTo(direction);
 		if(to!=null)
 		{	ChessChip top = to.topChip();
 			if((top==null) || (playerIndex(top)!=who))
 			{
 				if(all!=null) { all.push(new ChessMovespec(from,to,who)); }
 				return(true);
 			}
 		}
 	}
 	return(false);
 }
 private void sweepKnightStepMoves(ChessCell from,ChessCell step, int direction,int who)
 {	if(step!=null)
 	{	ChessCell to = step.exitTo(direction);
 		if(to!=null)
 		{	to.sweep_counter = sweep_counter;
 		}
 	}
 }
 private boolean addKnightMoves(CommonMoveStack all,ChessCell from,int who)
 {	boolean some = false;
 	for(int direction : rookDirections)
 	{
 		ChessCell step = from.exitTo(direction);
 		if(step!=null)
 		{	some |= addKnightStepMoves(all,from,step,direction+1,who);
 			some |= addKnightStepMoves(all,from,step,direction-1,who);
 		}
 	}
 	return(some);
 }
 private void sweepKnightMoves(ChessCell from,int who)
 {	
 	for(int direction : rookDirections)
 	{
 		ChessCell step = from.exitTo(direction);
 		if(step!=null)
 		{	sweepKnightStepMoves(from,step,direction+1,who);
 			sweepKnightStepMoves(from,step,direction-1,who);
 		}
 	}
 }
 private boolean addNoncapturingMoves(CommonMoveStack all,ChessCell from,int who)
 {	boolean some = false;
 	for(int direction : queenDirections)
 	{	some |= addNoncapturingMoves(all,from,direction,who);
 		if(some && (all==null)) { return(true); } 
 	}
 	return(some);
 }
 private void sweepNoncapturingMoves(ChessCell from,int who)
 {	
 	for(int direction : queenDirections)
 	{	sweepNoncapturingMoves(from,direction,who);
 	}
 }
 // who is the color potentially being immobilized 
 private boolean isImmobilized(ChessCell from,ChessChip fromTop,int who)
 {	boolean lockByChamelion = (fromTop.piece==ChessPiece.Immobilizer);
 	for(int direction : queenDirections)
 	{
	 ChessCell adj = from.exitTo(direction);
	 if(adj!=null)
	 {	ChessChip top = adj.topChip();
	 	if((top!=null)
	 			&& ((top.piece==ChessPiece.Immobilizer)
	 					|| ((top.piece==ChessPiece.Chamelion)
	 						&& (fromTop!=null)
	 						&& (fromTop.piece==ChessPiece.Immobilizer))
	 					|| (lockByChamelion && (top.piece==ChessPiece.Immobilizer))) 
	 			&& (playerIndex(top)!=who))
	 	{	return(true);
	 	}
	 }
 	}
 	return(false);
 }
 
 // leap over zero, one or several enemy pieces.  If chamelion is true, only leap over long leapers.
 private boolean addLongLeaperMoves(CommonMoveStack all, ChessCell from, int direction, int who,boolean chamelion)
 {	ChessCell to = from;
 	boolean some = false;
 	while( (to=to.exitTo(direction))!=null)
 	{	ChessChip top = to.topChip();
 	    if(top==null)
 			{ if(chamelion ? some : true)
 				{if(all==null) { return(true); } 
 				 all.push(new ChessMovespec(from,to,who)); some = true;
 				}
 			}// another landing spot
 		else if(playerIndex(top)==who) { return(some); }					  // blocked by our piece
 		else if(!chamelion || (top.piece==ChessPiece.LongLeaper))
 		{	// found an enemy piece we can leap
 			ChessCell land = to.exitTo(direction);
 			if((land==null) || (land.topChip()!=null)) { return(some); }	// no place to land
 			if(all==null) { return(true); }
 			all.push(new ChessMovespec(from,land,who));	// valid landing space
 			to = land;
 			some = true;			
 		}
 	}
 	return(some);
 }
 // leap over zero, one or several enemy pieces.  If chamelion is true, only leap over long leapers.
 private void sweepLongLeaperMoves(ChessCell from, int direction, int who)
 {	ChessCell to = from;
 	ChessCell prev = null;
 	ChessChip ptop = null;
 	// mark places where if a piece were placed, a long leaper could leap
 	while( (to=to.exitTo(direction))!=null)
 	{	ChessChip top = to.topChip();
 		if(top!=null)
 		{
 			if(playerIndex(top)==who) { break; }	// our own piece
 		}
 		else
 		if( (prev!=null) && (ptop==null))
 		{
 			prev.sweep_counter = sweep_counter;
 		}
 		ptop = top;
 		prev = to;
 	}
 }
 private boolean addLongLeaperMoves(CommonMoveStack all,ChessCell from,int who,boolean chamelion)
 {	boolean some = false;
 	for(int direction : queenDirections)
 	{	some |= addLongLeaperMoves(all,from,direction,who,chamelion);
 		if(some && (all==null)) { return(true); } 
 	}
 	return(some);
 }
 
 private void sweepLongLeaperMoves(ChessCell from,int who)
 {	
 	for(int direction : queenDirections)
 	{	sweepLongLeaperMoves(from,direction,who);
 	}
 }
 
 
 // add normal chess moves from a particular cell
 // "all" can be null
 // return true if there are any.
 public boolean addSimpleMoves(CommonMoveStack all,ChessCell from,ChessPiece moving,int who)
 {	boolean some = false;
 	
 	switch(moving)
 	{
	case Pawn:
		{
		int direction = forwardDirection(who);
		some |= addPawnForwardMove(all,from,direction,who);
		some |= addPawnCaptureMove(all,from,direction-1,who);
		some |= addPawnCaptureMove(all,from,direction+1,who);
		break;
		}
	case Rook:
		some |= addRookMoves(all, from, who);
		break;
	case Bishop:
		some |= addBishopMoves(all,from, who);
		break;
	case Queen: 
		some |= addQueenMoves(all,from,who);
		break;
	case King:
		some |= addKingMoves(all,from,who,false);
		some |= addCastlingMoves(all,from,who);
		break;
	case Knight:
		some |= addKnightMoves(all,from,who);
		break;
	//
	// ultima pieces
	//
	case CustodialPawn:
		some |= addNoncapturingRookMoves(all,from,who);
		break;
	case UltimaKing:
		some |= addKingMoves(all,from,who,false);
		break;
	case Withdrawer:
	case Immobilizer:
	case Coordinator:
		some |= addNoncapturingMoves(all,from,who);
		break;
	case Chamelion:
		some |= addNoncapturingMoves(all,from,who);
		some |= addLongLeaperMoves(all,from,who,true);
		some |= addKingMoves(all,from,who,true);
		break;
	case LongLeaper:
		some |= addLongLeaperMoves(all,from,who,false);
		break;
	default: G.Error("Not expecting %s",moving);
 	}
	 return(some);
 }
 
 public boolean reachesKing(ChessCell from,ChessCell king,int direction)
 {	while( (from=from.exitTo(direction))!=null)
 	{ 	if(from==king) { return(true); }
 		if(from.topChip()!=null) { return(false); }	// reaches something else
 	}
 	return(false);
 }
 public boolean leapsKing(ChessCell from,ChessCell king,int direction,int who)
 {	while( (from=from.exitTo(direction))!=null)
 	{ 	if(from==king) 
 			{ 	// needs a landing spot
 				from=from.exitTo(direction);
 				return((from!=null)&&(from.topChip()==null));
 			}
 		ChessChip top = from.topChip();
 		if(top!=null)
 		{
 		if(playerIndex(top)==who) { return(false); }
 		from=from.exitTo(direction);
 		if((from==null)||(from.topChip()!=null)) { return(false); }
 		}
 	}
 	return(false);
 }
 private boolean exitPinch(ChessCell from, int direction,int who)
 {
	 ChessCell adj = from.exitTo(direction);
	 if(adj!=null)
	 {
		 ChessChip top = adj.topChip();
		 return((top!=null)&&(playerIndex(top)==who));
	 }
	 return(false);
 }
 private boolean moveToReach(ChessCell from, int direction,char col,int row)
 {	while(( (from = from.exitTo(direction))!=null)
		  && (from.topChip()==null))
 		{	if((col==from.col)&&(row==from.row)) 
 				{ return(true); }
 		}
 		return(false);
 }
 private boolean moveToReachTarget(ChessCell from, int direction,char col,int row)
 {	while(( (from = from.exitTo(direction))!=null)
		  && !((col==from.col)&&(row==from.row)))
 		{	if((from.topChip())!=null) { return(false);}
 		}
 		return(from!=null);
 } 
 public boolean attackingSquare(ChessCell target,ChessCell from,ChessPiece type,int bywho)
 {	 if(target!=null)
	 {
	 switch(type)
	 {
	 default: break;
	 case Withdrawer:
	 	{
	 		if((Math.abs(from.col-target.col)<=1)
	 				&& (Math.abs(from.row-target.row)<=1))
	 		{	int direction = findDirection(target.col,target.row,from.col,from.row);
	 			ChessCell adj = from.exitTo(direction);
	 			return((adj!=null) && (adj.topChip()==null));
	 		}
	 	}
	 	break;
	 case Chamelion:
	 	{
	 		ChessChip top = target.topChip();
	 		if(top!=null && top.isKing())
	 		{
	 			for(int direction:queenDirections) 
	 				{ if(from.exitTo(direction)==target) 
	 					{ return(true); 
	 					}
	 				}
	 		}
	 	}
	 	break;
	 case Pawn:
	 	{
	 		int direction = forwardDirection(bywho);
	 		return( (from.exitTo(direction+1)==target) || (from.exitTo(direction-1)==target));
	 	}
	 	
	 case Rook:
		 if(from.col==target.col) { return(reachesKing(from,target,target.row>from.row?CELL_UP:CELL_DOWN)); }
		 else if(from.row==target.row) { return(reachesKing(from,target,target.col>from.col ? CELL_RIGHT : CELL_LEFT)); }
		 break;
	 	
	 case Bishop:
		 if(from.col<target.col) { return(reachesKing(from,target,target.row>from.row ? CELL_UP_RIGHT : CELL_DOWN_RIGHT)); }
		 else if(from.col>target.col) { return(reachesKing(from,target,target.row>from.row ? CELL_UP_LEFT : CELL_DOWN_LEFT)); }
		 break;
	 	
	 case Queen:
		 return(attackingSquare(target,from,ChessPiece.Rook,bywho) || attackingSquare(target,from,ChessPiece.Bishop,bywho));
	 	
	 case King: 
	 case UltimaKing:	// ultima kings can close the gap if the other king isn't immobilized
		 for(int direction:queenDirections) { if(from.exitTo(direction)==target) { return(true); }}
		 break;
	  
	 case Knight:
		 for(int direction : rookDirections)
		 {	ChessCell step = from.exitTo(direction);
		 	if(step!=null)
		 	{	if(step.exitTo(direction+1)==target) { return(true); }
		 		if(step.exitTo(direction-1)==target) { return(true); }
		 	}}
		 break;
		 
	 case LongLeaper:
	 	{
		 if(from.col==target.col) { return(leapsKing(from,target,target.row>from.row?CELL_UP:CELL_DOWN,bywho)); }
		 else if(from.row==target.row) { return(leapsKing(from,target,target.col>from.col ? CELL_RIGHT : CELL_LEFT,bywho)); }
		 
		 if(from.col<target.col) { return(leapsKing(from,target,target.row>from.row ? CELL_UP_RIGHT : CELL_DOWN_RIGHT,bywho)); }
		 else if(from.col>target.col) { return(leapsKing(from,target,target.row>from.row ? CELL_UP_LEFT : CELL_DOWN_LEFT,bywho)); }
		 }
		 break;
	 case CustodialPawn:
		 if(from.row!=target.row)	// possible horizontal
		 {	int colDif = from.col-target.col;
		 	if(colDif==0)
		 	{	// same column
				 int direction = findDirection(from.col,from.row,from.col,target.row);
				 return(exitPinch(target,direction,bywho)
						 && moveToReachTarget(from,direction,from.col,target.row));
		 	}
		 	else if(Math.abs(colDif)==1)
		 	{	// adjacent column
			 int direction = findDirection(from.col,from.row,from.col,target.row);
			 int pinchDirection = findDirection(from.col,target.row,target.col,target.row);
			 return(exitPinch(target,pinchDirection,bywho)
					 && moveToReach(from,direction,from.col,target.row));
		 	}
		 }
		 if(from.col!=target.col)	// possible vertical
		 {	int rowDif = from.row-target.row;
		 	if(rowDif==0)
		 	{	// same row
				 int direction = findDirection(from.col,from.row,target.col,from.row);
				 return(exitPinch(target,direction,bywho)
						 && moveToReachTarget(from,direction,target.col,from.row));
		 	}
		 	else if(Math.abs(rowDif)==1)
		 	{	// adjacent row
			 int direction = findDirection(from.col,from.row,target.col,from.row);
			 int pinchDirection = findDirection(target.col,from.row,target.col,target.row);
			 return(exitPinch(target,pinchDirection,bywho)
					 && moveToReach(from,direction,target.col,from.row));
		 	}
		 }
		 break;
	 case Coordinator:
	 	{
		ChessCell my = kingLocation[bywho];
		if(my!=null)
		{
			if(target.col==my.col)
			{	// kings in same column
				if((from.row==target.row)&&(from.col!=my.col)) 
					{ return(true); }	// already there
				
				int primeDirection = findDirection(from.col,from.row,from.col,target.row);
				// primeDirection moves orthogonally to the capture column
				// +- 1 move diagonally to the capture column
				for(int direction=primeDirection-1; direction<=primeDirection+1; direction++)
				{
				ChessCell d = from;
				ChessChip top = null;
				while(((d=d.exitTo(direction))!=null)
						&& ((top=d.topChip())==null)
						&& (d.row!=target.row) )
					{					
					}
				if((top==null) && (d!=null) && (d.row==target.row))
					{ return(true); }
				}
				return(false);
			}
			else if(target.row==my.row)
			{	// kings in same row
				if((from.col==target.col)&&(from.row!=my.row))
					{ return(true); }	// already there
				int primeDirection = findDirection(from.col,from.row,target.col,from.row);
				for(int direction = primeDirection-1; direction<=primeDirection+1; direction++)
				{
				ChessCell d = from;
				ChessChip top = null;
				while(((d=d.exitTo(direction))!=null) 
						&& ((top=d.topChip())==null)
						&& (d.col!=target.col) )
					{					
					}
				if((top==null) && (d!=null) && (d.col==target.col)) 
					{ return(true); }
				}
				return(false);
			}
			}
		}
	 }}
	 return(false);
 }

 private boolean attackingKing(int who)
 {	int next = nextPlayer[who];
 	return(attackingSquare(kingLocation[who],next));
 }
 private boolean attackingSquare(ChessCell dest,int byWho)
 {
 	CellStack pieces = occupiedCells[byWho];
 	for(int lim=pieces.size()-1; lim>=0; lim--)
 	{	ChessCell c = pieces.elementAt(lim);
 		ChessChip top = c.topChip();
 		if(!isImmobilized(c,top,byWho))
 		{	if(attackingSquare(dest,c,top.piece,byWho)) 
 				{ return(true); }
 		}
 	}
 	return(false);
 }
 int sweep_counter=0;
 
 // add normal chess moves from a particular cell
 // "all" can be null
 // return true if there are any.
 public void sweepCoverage(ChessCell from,int who)
 {	
 	from.sweep_counter = sweep_counter;
 	ChessChip moving = from.topChip();
 	switch(moving.piece)
 	{
	case Pawn:
		{
		int direction = forwardDirection(who);
		ChessCell n1 = from.exitTo(direction+1);
		if(n1!=null) { n1.sweep_counter=sweep_counter; }
		ChessCell n2 = from.exitTo(direction-1);
		if(n2!=null) { n2.sweep_counter = sweep_counter; }
		}
		break;
	case Rook:
		sweepRookMoves(from, who);
		break;
	case Bishop:
		sweepBishopMoves(from, who);
		break;
	case Queen: 
		sweepQueenMoves(from,who);
		break;
	case King:
		sweepKingMoves(from,who,false);
		break;
	case Knight:
		sweepKnightMoves(from,who);
		break;
	//
	// ultima pieces
	//
	case CustodialPawn:
		sweepCustodialPawnMoves(from,who);
		break;
	case UltimaKing:
		sweepKingMoves(from,who,false);
		break;
	case Withdrawer:
	case Immobilizer:
	case Coordinator:
		sweepNoncapturingMoves(from,who);
		break;
	case Chamelion:
		sweepKingMoves(from,who,true);
		break;
	case LongLeaper:
		sweepLongLeaperMoves(from,who);
		break;
	default: G.Error("Not expecting %s",moving);
 	}
 }
 // mark cells that the opposing king can't occupy.
 // Who is the attacking player.
 public void sweepCoverage(int who)
 {	sweep_counter++;
 	switch(variation)
	 {
	 default: throw G.Error("Not expecting %s",variation); 
	 case Ultima:
	 case Chess960:
	 case Chess:
 		 // captures are mandatory
		 CellStack cells =  occupiedCells[who];
		 for(int lim = cells.size()-1; lim>=0; lim--)
		 {
			 sweepCoverage(cells.elementAt(lim),who);
		 }
		 break;
	 }
 }
 int sweepKingRange(ChessCell from,int bound)
 {	int count = 0;
 	from.sweep_counter = sweep_counter;
 	for(int direction = from.geometry.n-1; direction>=0; direction--)
 	{
	ChessCell ex = from.exitTo(direction);
	if((ex!=null) && (ex.sweep_counter<bound) && (ex.topChip()==null))
		{
		count++;
		ex.sweep_counter=sweep_counter;
		count += sweepKingRange(ex,bound);
		}
 	}
 	return(count);
 }
 int sweepKingRange(int who)
 {	sweepCoverage(nextPlayer[who]);
 	int boundary = sweep_counter;
 	sweep_counter++;
 	ChessCell loc = kingLocation[who];
 	int range = (loc==null)?0:sweepKingRange(loc,boundary);
 	return(range);
 }
 public boolean addMoves(CommonMoveStack all,int who)
 {	boolean some = false;
 	switch(variation)
	 {
	 default: throw G.Error("Not expecting %s",variation); 
	 case Ultima:
	 case Chess960:
	 case Chess:
 		 // captures are mandatory
 		 switch(board_state)
 		 {
 		 default: throw G.Error("Not expecting state %s",board_state);
 		 case AcceptOrDecline:
 			 all.push(new ChessMovespec(MOVE_ACCEPT_DRAW,whoseTurn));
 			 all.push(new ChessMovespec(MOVE_DECLINE_DRAW,whoseTurn));
 			 break;
 		 case Check:
 		 case Play:
 		 case Filter:
 			 if(pickedObject==null)
 			 {
 			 some = addSimpleMoves(all,whoseTurn); 
 			 }
 			 else
 			 {	// something is already moving
 			 ChessCell source = getSource();
 			 ChessPiece piece = pickedObject.piece;
 			 if(isImmobilized(source,pickedObject,who))
 			 {
 			 some = addSuicideMove(all,source,who);
 			 }
 			 else
 			 {
 			 some = addSimpleMoves(all,source,piece,who); 
 			 }}
 			if( ((moveNumber-lastProgressMove)>8)
 					 && ((moveNumber-lastDrawMove)>4))
 			 {
 				 all.push(new ChessMovespec(MOVE_OFFER_DRAW,whoseTurn));
 			 }
 			 break;

 		 }
	 }
 	return(some);
 }
 private int nonKingMoves = 0;
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
  	addMoves(all,whoseTurn);
  	// special logic to poision stalemates.  If we have only king
  	// moves, filter them for check moves, and if nothing is left
  	// emit a special "claimvictory" move.  This will cause the 
  	// higher levels of the search to avoid this branch.  If it
  	// actually is impossible to avoid, it will be a stalemate.
  	if((board_state==ChessState.Play) 
  			&& (nonKingMoves==0)
  			&& (!attackingKing(whoseTurn))
  			)
  	{
  		filterCheckMoves(all,whoseTurn);
  		if(all.size()==0)
  		{
  			// nothing left
  			all.addElement(new ChessMovespec(MOVE_STALEMATE,whoseTurn));
  			
  		}
  	}
 	return(all);
 }
 
 

}
