package frogs;

import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;

/**
 * FrogBoard knows all about the game Army of Frogs, which is played on a
 * hexagonal board. It gets a lot of logistic support from common.hexBoard,
 * which knows about the coordinate system.
 * 
 * 2DO: record the deck shuffle in the game records
 * this will disentangle game records from the live random number generator
 *
 * @author ddyer
 * 
 */

class FrogBoard extends hexBoard<FrogCell> implements BoardProtocol, FrogConstants {
	
	static final int FROGS_PER_PLAYER = 10;
	static final int NCOLORS = 4;
	   /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
    where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
    calculating adjacency and connectivity. */
	static int[] FrogCols = { 25,24,23,22,21,20,19,18, 17, 16, 15, 14,13,12,11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 }; // these are indexes into the first ball in a column, ie B1 has index 2
	static int[] FrogNInCol = { 26,26,26,26,26,
 								26,26,26,26,26,
 								26,26,26,26,26,
 								26,26,26,26,26,
 								26,26,26,26,26,26 }; // depth of columns, ie A has 4, B 5 etc.

 	private FrogState unresign;
 	private FrogState board_state;
	public FrogState getState() {return(board_state); }
	public CellStack animationStack = new CellStack();
	public void setState(FrogState st) 
	{ 	unresign = (st==FrogState.RESIGN_STATE)?board_state:null;
		board_state = st;
		cached_sources = null;
		cached_dests = null;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	int sweep_counter = 0;
	int sweep_connected = 0;
	public FrogCell hand[][] = new FrogCell[NCOLORS][2]; // player hand of 2 frogs
	public FrogCell bag = null;

	public CellStack occupiedCells = new CellStack();

	public int tilesOnBoard() {
		return (occupiedCells.size());
	}


    public void getScores(FrogMovespec m)
    {	for(int i=0;i<players_in_game; i++) { m.playerScores[i] = win[i]?1.0:0.0; }
    }
	public int[] frogsOnBoard = new int[NCOLORS];

	// intermediate states in the process of an unconfirmed move should
	// be represented explicitly, so unwinding is easy and reliable.
	public FrogPiece pickedObject = null;
	private FrogCell pickedSource = null;
	private FrogCell droppedDest = null;
	private FrogCell pickedMoveSource = null;
	private FrogCell droppedMoveDest = null;

	public FrogCell sourceCell() {
		return ((pickedSource!=null)?pickedSource:pickedMoveSource);
	}

	public FrogCell destCell() {
		return ((droppedDest!=null)?droppedDest:droppedMoveDest);
	}

	// factory method to construct a cell on the board
	public FrogCell newcell(char c, int r) {
		return (new FrogCell(c, r, FrogId.BoardLocation));
	}

	public FrogBoard(String init, long randomv, int npl,int map[]) // default constructor
	{
		drawing_style = DrawingStyle.STYLE_CELL;// STYLE_NOTHING; // don't draw the cells.
									// STYLE_CELL to draw them
		Grid_Style = FROGGRIDSTYLE;
		isTorus = true;
		initBoard(FrogCols, FrogNInCol, null); // this sets up a hexagonal board
		setColorMap(map);
		doInit(init, randomv, npl); // do the initialization
	}

	public void doInit() {
		doInit(gametype, randomKey, players_in_game);
	}
	public void doInit(String init,long randomv)
	{
		doInit(init,randomv,players_in_game);
	}
	public void doInit(String init, long randomv, int npl) 
	{	
		players_in_game = npl;
		randomKey = randomv;
		gametype = init;
		Random r = new Random(1245606);
		win = new boolean[npl];
		hand = new FrogCell[players_in_game][2];
		frogsOnBoard = new int[NCOLORS];	// always 4, because indexed by color
		occupiedCells.clear();
		bag = new FrogCell(r,FrogId.Frog_Bag);
		allCells.setDigestChain(r);
		int map[] = getColorMap();
		for (int i = 0; i < players_in_game; i++) {
			hand[i][0] = new FrogCell(r,Frog_Hands[i], 0);
			hand[i][1] = new FrogCell(r,Frog_Hands[i], 1);
			FrogPiece frog = FrogPiece.getChip(map[i]);
			for (int j = 0; j < FROGS_PER_PLAYER; j++) {
				bag.addChip(frog);
			}
		}

		bag.shuffle(new Random(randomKey));

		for (int i = 0; i < players_in_game; i++) {
			hand[i][0].addChip(bag.removeTop());
			hand[i][1].addChip(bag.removeTop());
		}
		for (FrogCell c = allCells; c != null; c = c.next) {
			c.reInit();
		}
		setState(FrogState.PUZZLE_STATE);

		whoseTurn = FIRST_PLAYER_INDEX;
		droppedDest = null;
		pickedSource = null;
		pickedObject = null;
		pickedMoveSource = null;
		droppedMoveDest = null;
		pickedObject = null;
		moveNumber = 1;
	}

	public FrogBoard cloneBoard() {
		FrogBoard dup = new FrogBoard(gametype, randomKey, players_in_game,getColorMap());
		dup.copyFrom(this);
		return (dup);
	}
    public void copyFrom(BoardProtocol b) { copyFrom((FrogBoard)b); }

	public void SetDrawState() {
		setState(FrogState.DRAW_STATE);
	}
    public void sameboard(BoardProtocol f) { sameboard((FrogBoard)f); }

	/**
	 * Robots use this to verify a copy of a board. If the copy method is
	 * implemented correctly, there should never be a problem. This is mainly a
	 * bug trap to see if BOTH the copy and sameboard methods agree.
	 * 
	 * @param from_b
	 */
	public void sameboard(FrogBoard from_b) 
	{
		super.sameboard(from_b); // hexboard compares the boards
		G.Assert(AR.sameArrayContents(frogsOnBoard,from_b.frogsOnBoard), "frogsOnBoard mismatch");

		int occ = occupiedCells.size();
		CellStack from_c = from_b.occupiedCells;
		int from_occ = from_c.size();
		G.Assert(occ == from_occ, "occupied count matches");
		G.Assert(bag.sameCell(from_b.bag), "bag matches");
		for (int i = 0; i < players_in_game; i++) {
			for (int j = 0; j < 2; j++) {
				G.Assert(hand[i][j].sameCell(from_b.hand[i][j]), "hands match");
			}
		}
		for (int idx = 0; idx < occ; idx++) {
			FrogCell tocell = occupiedCells.elementAt(idx);
			boolean match=false;
			for(int fromidx = 0; fromidx<occ && !match; fromidx++)
			{ 	FrogCell fromcell = from_c.elementAt(fromidx);
				match |= tocell.sameCell(fromcell);
			}
			G.Assert(match, "same cells");
		}
		G.Assert(pickedObject==from_b.pickedObject, "pickedObject matches");
		G.Assert(FrogCell.sameCell(pickedSource,from_b.pickedSource), "pickedSource matches");
		G.Assert(Digest()==from_b.Digest(),"digest matches");
	}

	/**
	 * this is used in fraud detection to see if the same game is being played
	 * over and over. Each game in the database contains a digest of the final
	 * state of the game. Other site machinery looks for duplicate digests.
	 * 
	 * @return
	 */
	public long Digest() {
		Random r = new Random(122058943);
		long v = super.Digest(r);

		v ^= bag.Digest(r);

		for (int i = 0; i < players_in_game; i++) {
			for (int j = 0; j < 2; j++) {
				v ^= hand[i][j].Digest(r);
			}
		}
		v ^= cell.Digest(r,pickedMoveSource);
		v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= r.nextLong()*(board_state.ordinal()*10+(whoseTurn+1));
		
		return (v);
	}

	/*
	 * make a copy of a board. This is used by the robot to get a copy of the
	 * board for it to manupulate and analyze without affecting the board that
	 * is being displayed.
	 */
	public void copyFrom(FrogBoard from_b)
	{	super.copyFrom(from_b);
		pickedObject = from_b.pickedObject;
		pickedSource = getCell(from_b.pickedSource);
		pickedMoveSource = getCell(from_b.pickedMoveSource);
		occupiedCells.clear();

		for (int idx = 0, lim = from_b.occupiedCells.size(); idx < lim; idx++) {
			FrogCell c = from_b.occupiedCells.elementAt(idx);
			occupiedCells.push(getCell(c.col, c.row));
		}
		AR.copy(frogsOnBoard,from_b.frogsOnBoard);
		bag.copyFrom(from_b.bag);
		for (int i = 0; i < players_in_game; i++) {
			for (int j = 0; j < 2; j++) {
				hand[i][j].copyFrom(from_b.hand[i][j]);
			}
		}
        board_state = from_b.board_state;
        unresign = from_b.unresign;

		sameboard(from_b);
	
	}

	public void setWhoseTurn(int who) {
		whoseTurn = (who < 0) ? FIRST_PLAYER_INDEX : who;
	}

	//
	// change whose turn it is, increment the current move number
	//
	public void setNextPlayer(replayMode replay) {
		switch (board_state) {
		default:
			throw G.Error("Move not complete, can't change the current player, state %s",board_state);
		case PUZZLE_STATE:
			break;
		case MOVE_FROG_STATE:
		case PLAY_STATE:
			if(replay==replayMode.Live)
			{	// some damaged games need this
				throw G.Error("Move not complete, can't change the current player, state %s",board_state);
			}
			//$FALL-THROUGH$
		case CONFIRM_STATE:
		case PASS_STATE:
		case DRAW_STATE:
		case RESIGN_STATE:
			moveNumber++; // the move is complete in these states
			setWhoseTurn((whoseTurn + 1) % players_in_game);
			return;
		}
	}

	/**
	 * this is used to determine if the "Done" button in the UI is live
	 * 
	 * @return
	 */
	public boolean DoneState() {
		
		switch (board_state) {
		case RESIGN_STATE:
		case CONFIRM_STATE:
		case PASS_STATE:
		case DRAW_STATE:
			return (true);

		default:
			return (false);
		}
	}

	/**
	 * In our implementation, the letter side(a-k) is black and the number side
	 * (1-11) is white. Either player can be playing either color.
	 * 
	 * @param ind
	 * @return
	 */

	public void setGameOver(boolean draw) {
		for (int i = 0; i < players_in_game; i++) {
			win[i] = !draw && (whoseTurn == i);
		}
		setState(FrogState.GAMEOVER_STATE);
	}

	// this method is also called by the robot to get the blobs as a side effect
	public boolean WinForPlayerNow(int player) {
		if (board_state == FrogState.GAMEOVER_STATE) {
			return (win[player]);
		}
		if (frogsOnBoard[getColorMap()[player]] < 7) {
			return (false);
		} else {
			return (allFrogsConnected(player));
		}
	}

	// sweep from seed C , mark and count cells whose top is target
	int sweepTarget(FrogCell c, FrogPiece target) {
		if (c.sweep_counter == sweep_counter) {
			return (0);
		}
		if (c.topChip() == target) {
			c.sweep_counter = sweep_counter;
			int count = 1;
			for (int dir = 0; dir < 6; dir++) {
				count += sweepTarget(c.exitTo(dir), target);
			}
			return (count);
		}
		return (0);
	}

	boolean allFrogsConnected(int who) {
		int map[] = getColorMap();
		FrogPiece target = FrogPiece.getChip(map[who]);
		for (int idx = 0, lim = occupiedCells.size(); idx < lim; idx++) { // find
																			// some
																			// frog
																			// with
																			// the
																			// appropriate
																			// color
			FrogCell c = occupiedCells.elementAt(idx);
			FrogPiece top = c.topChip();
			if (top == target) {
				sweep_counter++;
				// sweep all the connected cells, count if they add up to the
				// known total
				int count = sweepTarget(c, top);
				return (count == frogsOnBoard[map[who]]);

			}
		}
		return (false);
	}
	// count the number of groups of a particular player
	public int numberOfGroups(int player)
	{	int n=0;
		FrogPiece target = FrogPiece.getChip(getColorMap()[player]);
		sweep_counter++;
		for(int idx=0,lim=occupiedCells.size();  idx<lim; idx++)
		{	FrogCell c =  occupiedCells.elementAt(idx);
			FrogPiece top = c.topChip();
			if(top==target)
			{	
			if(c.sweep_counter!=sweep_counter)
			{
			n++;
			sweepTarget(c, top);
			}
			}
		}
		return(n);
	}
	//
	// return true if balls[rack][ball] should be selectable, meaning
	// we can pick up a ball or drop a ball there. movingBallColor is
	// the ball we would drop, or -1 if we want to pick up
	//
	public void acceptPlacement() {
		pickedObject = null;
		droppedDest = null;
		pickedSource = null;
		pickedMoveSource = null;
		droppedMoveDest = null;
	}

	//
	// undo the drop, restore the moving object to moving status.
	//
	private void unDropObject() {
		FrogCell dr = null;
		if (droppedDest != null) {
			dr = droppedDest;
			droppedDest = null;
		} else if (droppedMoveDest != null) {
			dr = droppedMoveDest;
			droppedMoveDest = null;
		}
		if (dr != null) {
			pickedObject = removeChip(dr);
		}
	}

	public FrogPiece removeChip(FrogCell c) {
		FrogPiece top = c.removeTop();
		cached_sources = null;
		cached_dests = null;
		if (c.onBoard) {
			frogsOnBoard[top.colorIndex]--;
			occupiedCells.remove(c,false);
		}
		return (top);
	}

	public void addChip(FrogCell c, FrogPiece top) {
		G.Assert((c == bag) || (c.topChip() == null),
				"can only stack on the bag");
		c.addChip(top);
		if (c.onBoard) {
			frogsOnBoard[top.colorIndex]++;
			occupiedCells.push(c);
		}
	}

	// 
	// undo the pick, getting back to base state for the move
	//
	private void unPickObject() {
		FrogPiece po = pickedObject;
		if (po != null) {
			FrogCell ps = null;
			if (pickedSource != null) {
				ps = pickedSource;
				pickedSource = null;
			} else if (pickedMoveSource != null) {
				ps = pickedMoveSource;
				pickedMoveSource = null;
			}
			pickedObject = null;
			cached_sources = null;
			cached_dests = null;
			
			addChip(ps, po);

		}
	}

	public FrogCell getCell(FrogCell c)
	{
		return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
	}
	public FrogCell getCell(FrogId dest,char col,int row)
	{
		switch (dest) {
		case Frog_Bag:
			return(bag);
		case Frog_Hand0:
		case Frog_Hand1:
		case Frog_Hand2:
		case Frog_Hand3: 
			return(hand[dest.handNum()][row]);
		case BoardLocation:
			return(getCell(col, row));
		default:
			throw G.Error("not expecting dest %s", dest);
		}
	
	}
	// 
	// drop the floating object.
	//
	private void dropObject(FrogCell dest,replayMode replay) {
		G.Assert((pickedObject != null) && (droppedDest == null),
				"ready to drop");
		cached_sources = null;
		cached_dests = null;

		switch (dest.rackLocation())
		{
		case Frog_Bag:
			droppedDest = dest;
			dest.addChip(pickedObject);
			pickedObject = null;
			break;
		case Frog_Hand0:
		case Frog_Hand1:
		case Frog_Hand2:
		case Frog_Hand3: {
			G.Assert(dest.topChip() == null, "empty cell");
			droppedDest = dest;
			dest.addChip(pickedObject);
			pickedObject = null;
		}
			break;
		case BoardLocation:
			{
			G.Assert(dest.topChip() == null, "empty cell");
			switch (board_state) {
			default:
				throw G.Error("Not expecting drop in state %s",board_state);
				
			case GAMEOVER_STATE:
				if(replay==replayMode.Live)
				{
					throw G.Error("Not expecting drop in state %s",board_state);
				}

				//$FALL-THROUGH$
			case PLAY_STATE:
				droppedDest = dest;
				;
				break;
			case CONFIRM_STATE:
				if(replay==replayMode.Live)
				{	// some damaged games need this to replay
					throw G.Error("Not expecting dropb in state %s", board_state);
				}
				//$FALL-THROUGH$
			case MOVE_FROG_STATE:
			case PUZZLE_STATE:
				droppedMoveDest = dest;
				break;
			}
			addChip(dest, pickedObject);
			pickedObject = null;

		}
			break;
		default:
			throw G.Error("not expecting dest %s", dest);
		}
	}

	//
	// true if col,row is the place where something was dropped and not yet
	// confirmed.
	// this is used to mark the one square where you can pick up a marker.
	//
	public boolean isDest(FrogCell cell) {
		return ((droppedMoveDest == cell) || (droppedDest == cell));
	}

	// get the index in the image array corresponding to movingObjectChar
	// or HitNoWhere if no moving object. This is used to determine what
	// to draw when tracking the mouse.
	public int movingObjectIndex() {
		FrogPiece ch = pickedObject;
		if (ch != null) {
			return (ch.chipIndex());
		}
		return (NothingMoving);
	}

	// pick something up. Note that when the something is the board,
	// the board location really becomes empty, and we depend on unPickObject
	// to replace the original contents if the pick is cancelled.
	private void pickObject(FrogCell c)
	{
		switch (c.rackLocation())
		{
		case Frog_Bag:
			pickedSource = c;
			pickedObject = c.removeTop();
			break;
		case BoardLocation:
		{
			pickedMoveSource = c;
			pickedObject = removeChip(c);
		}
			break;

		case Frog_Hand0:
		case Frog_Hand1:
		case Frog_Hand2:
		case Frog_Hand3: {
			pickedSource = c;
			pickedObject = removeChip(c);
		}
			break;
		default:
			throw G.Error("not expecting source %s", c);
		}
	}

	private void setNextStateAfterDone(replayMode replay) {
		switch (board_state) {
		default:
			throw G.Error("Not expecting Done in state %s", board_state);
		case DRAW_STATE:
			setGameOver(true);
			break;
		case MOVE_FROG_STATE:
		case PLAY_STATE:
			if(replay==replayMode.Live)
			{	// some damaged games need this
				throw G.Error("Not expecting Done in state %s", board_state);
			}
			//$FALL-THROUGH$
		case CONFIRM_STATE:
		case PASS_STATE:
			setState(nextPlayState(whoseTurn));
			break;
		case PUZZLE_STATE:
			break;
		}

	}

	void drawChipFromBag(replayMode replay) {
		FrogCell h[] = hand[whoseTurn];
		if (bag.topChip() != null) {
			for (int i = 0; i < 2; i++) {
				FrogCell hc = h[i];
				if (hc.topChip() == null) {
					FrogPiece p = bag.removeTop();
					if (p != null) {
						hc.addChip(p);
						if(replay!=replayMode.Replay)
						{
							animationStack.push(bag);
							animationStack.push(hc);
						}
						return;
					}
				}
			}
		}
	}

	private void doDone(replayMode replay) {
		acceptPlacement();
		drawChipFromBag(replay);
		if (board_state==FrogState.RESIGN_STATE) {
			G.Assert(players_in_game == 2, "only in 2 player games");
			setGameOver(true); // draw with no winner
			win[(whoseTurn + 1) % 2] = true;
		} else {
			boolean win1 = WinForPlayerNow(whoseTurn);
			if (win1) {
				setGameOver(false);
			} else {
				setNextPlayer(replay);
				setNextStateAfterDone(replay);
			}
		}
	}

	// return TRUE is this cell position adjacent to one of the cells that s is
	// adjacent to
	public boolean adjacentCell(FrogCell which, FrogCell s) {
		if ((which != null) && (which.onBoard)) {
			int len = which.nAdjacentCells();
			for (int i = 0; i < len; i++) {
				FrogCell adjto = which.exitTo(i);
				if ((adjto.height() > 0) && s.isAdjacentTo(adjto)) {
					return (true);
				}
			}
		}
		return (false);
	}

	private FrogState nextPlayState(int who) {
		return (hasFrogMoves(whoseTurn) ? FrogState.MOVE_FROG_STATE
				: hasDropMoves(whoseTurn) ? FrogState.PLAY_STATE : FrogState.PASS_STATE);
	}

	public boolean Execute(commonMove mm,replayMode replay) {
		FrogMovespec m = (FrogMovespec) mm;
		switch (m.op) {
		case MOVE_PASS:
			setState(FrogState.PASS_STATE);
			break;
		case MOVE_DONE:

			doDone(replay);

			break;
		case MOVE_ONBOARD: {
			FrogCell c = hand[m.player][m.from_row];
			FrogCell d = getCell(m.to_col, m.to_row);
			if(replay!=replayMode.Replay)
			{
				animationStack.push(c);
				animationStack.push(d);
			}
			FrogPiece chip = removeChip(c);
			m.object = chip;
			addChip(d, chip);
			setState(FrogState.CONFIRM_STATE);
		}
			break;
		case MOVE_MOVE: {
			FrogCell c = getCell(m.from_col, m.from_row);
			FrogCell d = getCell(m.to_col, m.to_row);
			if(replay!=replayMode.Replay)
			{
				animationStack.push(c);
				animationStack.push(d);
			}
			FrogPiece chip = removeChip(c);
			m.object = chip;
			addChip(d, chip);
			setState(hasDropMoves(whoseTurn) ? FrogState.PLAY_STATE : FrogState.CONFIRM_STATE);
		}
			break;
		case MOVE_DROPB: {
			FrogCell c = getCell(m.to_col, m.to_row);
			if(replay==replayMode.Single)
				{ FrogCell source = (pickedSource!=null) ? pickedSource : pickedMoveSource;
				if(source!=null)
				{
				animationStack.push(source);
				animationStack.push(c);
				}}

			switch (board_state) {
			default:
				throw G.Error("Not expecting dropb in state %s", board_state);
			case CONFIRM_STATE:
				if(replay==replayMode.Live)
				{	// some damaged games need this to replay
					throw G.Error("Not expecting dropb in state %s", board_state);
				}
				//$FALL-THROUGH$
			case GAMEOVER_STATE:
				if(replay==replayMode.Live)
				{
					throw G.Error("Not expecting pickb in state %s", board_state);
				}

				//$FALL-THROUGH$
			case MOVE_FROG_STATE:
				if (c == pickedMoveSource) {
					unPickObject();
				} else {
					dropObject(c,replay);
					setState((hasDropMoves(whoseTurn)&&(!WinForPlayerNow(whoseTurn))) 
										? FrogState.PLAY_STATE
										: FrogState.CONFIRM_STATE);
				}
				break;

			case PLAY_STATE:
				if (c == pickedSource) {
					unPickObject();
				} else {
					dropObject(c,replay);
					setState(FrogState.CONFIRM_STATE);
				}
				break;
			case PUZZLE_STATE:
				dropObject(c,replay);
				acceptPlacement();
				break;
			}
		}
			break;

		case MOVE_PICKB:
			// come here only where there's something to pick, which must
			switch (board_state) {
			default:
				throw G.Error("Not expecting pickb in state %s", board_state);
			case DRAW_STATE:
			case CONFIRM_STATE:
				if (isDest(getCell(m.from_col, m.from_row))) {
					unDropObject();
					if(pickedSource!=null) { setState(FrogState.PLAY_STATE); }
					else if(pickedMoveSource!=null) { setState(FrogState.MOVE_FROG_STATE); }
					else { throw G.Error("not expecting state"); }
				} else {
					throw G.Error("Can't pick something else");
				}
				break;
			case GAMEOVER_STATE:
				if(replay==replayMode.Live)
				{
					throw G.Error("Not expecting pickb in state %s", board_state);
				}

				//$FALL-THROUGH$
			case MOVE_FROG_STATE:
			case PUZZLE_STATE:
				pickObject(getCell(m.from_col, m.from_row));
				m.object = pickedObject;
				break;
			case PLAY_STATE:
				if(isDest(getCell(m.from_col,m.from_row)))
					{
					unDropObject();
					setState(FrogState.MOVE_FROG_STATE);
					}
				else { throw G.Error("Not expecting pickb"); }
				break;
			}
			break;
			
		case MOVE_DROP: // drop on chip pool;
			{
			FrogCell c = getCell(m.source,m.to_col, m.to_row);
			if(c==pickedSource) { unPickObject(); }
			else { dropObject(c,replay); }
			if(board_state==FrogState.PUZZLE_STATE) { acceptPlacement(); }
			}
			break;

		case MOVE_PICK:
			pickObject(getCell(m.source, m.from_col, m.from_row));
			m.object = pickedObject;
			break;

		case MOVE_START:
			setWhoseTurn(m.player);
			acceptPlacement();
			unPickObject();
			setState(FrogState.PUZZLE_STATE);
			{
				boolean win1 = WinForPlayerNow(whoseTurn);
				setState(FrogState.CONFIRM_STATE);
				if (win1) {
					setGameOver(false);
				} else {
					setState(nextPlayState(whoseTurn));
				}
			}
			break;

		case MOVE_RESIGN:
			setState(unresign==null?FrogState.RESIGN_STATE:unresign);
			break;
		case MOVE_EDIT:
			acceptPlacement();
			setWhoseTurn(FIRST_PLAYER_INDEX);
			setState(FrogState.PUZZLE_STATE);

			break;

        case MOVE_GAMEOVERONTIME:
        	win[whoseTurn] = true;
        	setState(FrogState.GAMEOVER_STATE);
        	break;
		default:
			cantExecute(m);
		}

		// System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);
		// System.out.println("Digest "+Digest());
		return (true);
	}

	public boolean LegalToHitBag() {
		switch (board_state) {
		default:
			return (false);
		case PUZZLE_STATE:
			return (pickedObject == null) ? (bag.topChip() != null) : true;
		}
	}

	public boolean LegalToHitChips(FrogCell c, int player) {
		switch (board_state) {
		default:
			throw G.Error("Not expecting state %s", board_state);

		case PLAY_STATE:
			return ((player == whoseTurn) && ((pickedSource == null) 
					? hasMoves(c) 
					: (c.topChip() == null)));
		case PASS_STATE:
		case CONFIRM_STATE:
		case DRAW_STATE:
		case GAMEOVER_STATE:
		case MOVE_FROG_STATE:
		case RESIGN_STATE:
			return (false);
		case PUZZLE_STATE:
			return (pickedObject == null) ? (c.topChip() != null) : (c.topChip() == null);
		}
	}
	// the move generator is intended for the robot, so it "optimizes" by not
	// generating placement moves for the second position if the first position
	// is the same color.  Rather than mess with the move generator at this 
	// late date, we check for this case.
	private boolean hasMoves(FrogCell c)
	{	Hashtable<FrogCell,FrogMovespec> sources = getSources();
		if(sources.get(c)!=null) { return(true); }
		FrogCell other = getCell(c.rackLocation(),c.col,c.row^1);
		if((other.topChip()==c.topChip()) && (sources.get(other)!=null)) { return(true); }
		return(false);
	}
	public boolean LegalToHitBoard(FrogCell cell) {
		switch (board_state) {
		case PLAY_STATE:
			return (((pickedObject == null)
							? ((cell==droppedMoveDest) || (getSources().get(cell) != null))
							: (getDests().get(cell) != null)));

		case DRAW_STATE:
		case CONFIRM_STATE:
		case MOVE_FROG_STATE:
			return ((cell == droppedDest) 
					|| ((pickedObject == null) 
							? (getSources().get(cell) != null)
							: (getDests().get(cell) != null)));
		case PASS_STATE:
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return (false);

		default:
			throw G.Error("Not expecting state %s", board_state);

		case PUZZLE_STATE:
			return ((pickedObject == null) ? ((cell != null) && (cell.height() > 0)) // something
																						// available
																						// to
																						// pick
																						// up
					: ((cell.height()) == 0));
		}
	}
	private StateStack robotState = new StateStack();
	
	/**
	 * assistance for the robot. In addition to executing a move, the robot
	 * requires that you be able to undo the execution. The simplest way to do
	 * this is to record whatever other information is needed before you execute
	 * the move. It's also convenient to automatically supply the "done"
	 * confirmation for any moves that are not completely self executing.
	 */
	public void RobotExecute(FrogMovespec m) {
		robotState.push(board_state); // record the starting state. The most reliable
		// to undo state transistions is to simple put the original state back.
		// G.print("E "+m);
		G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

		if (Execute(m,replayMode.Replay)) {
			if (DoneState()) {
				doDone(replayMode.Replay);
			} else if ((m.op == MOVE_MOVE) || (m.op == MOVE_DONE)
					|| (m.op == MOVE_PASS)) {
			} else {
				throw G.Error("Robot move should be in a done state");
			}
		}
	}

	//
	// un-execute a move. The move should only be unexecuted
	// in proper sequence. This only needs to handle the moves
	// that the robot might actually make.
	//
	public void UnExecute(FrogMovespec m) {
		// System.out.println("U "+m+" for "+whoseTurn);
		// G.print("U "+m);

		switch (m.op) {
		default:
			cantUnExecute(m);
        	break;
		case MOVE_ONBOARD: {
			FrogCell c = getCell(m.to_col, m.to_row);
			FrogCell d = hand[m.player][m.from_row];
			if (d.topChip() != null) {
				bag.addChip(d.removeTop()); // undo the pick from bag
			}
			addChip(d, removeChip(c));
		}
			break;
		case MOVE_MOVE: {
			FrogCell c = getCell(m.to_col, m.to_row);
			FrogCell d = getCell(m.from_col, m.from_row);
			addChip(d, removeChip(c));
		}
			break;
		case MOVE_PASS:
			break;
		case MOVE_DONE:
			break;
		case MOVE_RESIGN:
			break;
		}
		droppedDest = null;
		pickedSource = null;
		pickedMoveSource = null;
		droppedMoveDest = null;
		pickedObject = null;
		setState(robotState.pop());
		if (whoseTurn != m.player) {
			moveNumber--;
			setWhoseTurn(m.player);
		}
	}

	double nextEvaluation(int pl, boolean print) { // simple evaluation based on
													// piece mobility and
													// importance
		return (simpleEvaluation(pl, print));
	}

	double simpleEvaluation(int pl, boolean print) 
	{ // simple evaluation based
	  // on piece mobility and
	  // importance
		int groups = numberOfGroups(pl);
		double val = groups;
		double avex = 0.0;
		double avey = 0.0;
		FrogPiece target = FrogPiece.getChip(getColorMap()[pl]);
		int n = 0;
		// calculate the centroid x and y locations
		for (int idx = 0, lim = occupiedCells.size(); idx < lim; idx++) {
			FrogCell c = occupiedCells.elementAt(idx);
			FrogPiece p = c.topChip();
			if (p == target) {
				avex += c.col;
				avey += c.row;
				n++;
			}
		}
		if (n > 0) {
			avex /= n;
			avey /= n;
			for (int idx = 0, lim = occupiedCells.size(); idx < lim; idx++) {
				FrogCell c = occupiedCells.elementAt(idx);
				FrogPiece p = c.topChip();
				if (p == target) { // add the sum of squares of distance to the
									// center
					double dx = avex - c.col;
					double dy = avey - c.row;
					val += dx * dx + dy * dy;
				}
			}
		}
		if(print) { System.out.println("G "+groups+" V "+(1000-val)); }
		return (1000.0 - val); // we want low scores
	}

	// true if player can play frog on cell
	boolean canPlaceFrog(FrogPiece target, FrogPiece frog, FrogCell c) {
		if ((frog != null) && (c.topChip() == null)) {
			for (int dir = 0; dir < 6; dir++) {
				FrogCell adj = c.exitTo(dir);
				FrogPiece p = adj.topChip();
				if ((p != null) && (p == target)) {
					return (false);
				} // adjacent to own color
			}
			return (frogStringLength(c, null, null, c) <= 2);
		}
		return (false); // not empty
	}

	double maxEvaluation(int pl, boolean print) { // simple evaluation based on
													// piece mobility and
													// importance
		return (simpleEvaluation(pl, print));
	}

	// return true if there are drop moves.
	// if all!=null, it's a list of moves.
	private boolean getFrogPlaceMoves(CommonMoveStack  all, int who, FrogCell from,
			FrogPiece frog) {
		boolean val = false;
		sweep_counter++;
		int map[] = getColorMap();
		if (frog != null) {
			int lim = occupiedCells.size();

			if (lim == 0) {
				val = true;
				if (all == null) {
					return (val);
				}
				all.addElement(new FrogMovespec(who, MOVE_ONBOARD, Frog_Hands[who], from.row, (char) ('A' + ncols / 2), nrows / 4));
			} else {
				FrogPiece target = FrogPiece.getChip(map[who]);
				for (int idx = 0; idx < lim; idx++) {
					FrogCell c = occupiedCells.elementAt(idx);
					if ((frog!=target) || (c.topChip() != target))
					{
						for (int dir = 0; dir < 6; dir++) {
							FrogCell ac = c.exitTo(dir);
							if (ac.sweep_counter < sweep_counter) {
								ac.sweep_counter = sweep_counter;
								if (canPlaceFrog((frog == target) ? target
										: null, frog, ac)) {
									val = true;
									if (all == null) {
										return (val);
									}
									all.addElement(new FrogMovespec(who,
											MOVE_ONBOARD, Frog_Hands[who],
											from.row, ac.col, ac.row));
								}
							}
						}
					}
				}
			}
		}
		return (val);
	}

	// return true if player has drop moves
	boolean hasDropMoves(int who) {
		for (int i = 0; i < 2; i++) {
			FrogCell c = hand[who][i];
			if (getFrogPlaceMoves(null, who, c, c.topChip())) {
				return (true);
			}
		}
		return (false);
	}

	Hashtable<FrogCell,FrogMovespec> cached_sources = null;

	Hashtable<FrogCell,FrogMovespec> getSources() 
	{ 
		if(cached_sources!=null) { return(cached_sources); }
		Hashtable<FrogCell,FrogMovespec> h = new Hashtable<FrogCell,FrogMovespec>();
		if (pickedObject == null) {
			switch (board_state) {
			case PASS_STATE:
			case GAMEOVER_STATE:
				break;
			case MOVE_FROG_STATE: {
				CommonMoveStack v = new CommonMoveStack();
				getFrogMoveMoves(v, whoseTurn);
				for (int idx = 0, lim = v.size(); idx < lim; idx++) {
					FrogMovespec m = (FrogMovespec)v.elementAt(idx);
					FrogCell c = getCell(m.from_col, m.from_row);
					h.put(c, m);
				}
			}
				break;
			case PLAY_STATE: {
				CommonMoveStack v = new CommonMoveStack();
				getFrogPlaceMoves(v, whoseTurn);
				for (int idx = 0, lim = v.size(); idx < lim; idx++) {
					FrogMovespec m = (FrogMovespec) v.elementAt(idx);
					FrogCell c = hand[m.source.handNum()][m.from_row];
					h.put(c, m);
				}
			}

				break;
			default:
				break;
			}
		}
		cached_sources = h;
		return (h);
	}

	Hashtable<FrogCell,FrogCell> cached_dests = null;

	Hashtable<FrogCell,FrogCell> getDests() {
		if (cached_dests != null) {
			return (cached_dests);
		}
		Hashtable<FrogCell,FrogCell> h = new Hashtable<FrogCell,FrogCell>();
		switch (board_state) {
		case PASS_STATE:
		case GAMEOVER_STATE:
			break;
		case MOVE_FROG_STATE:
			if (pickedObject != null) {
				CommonMoveStack v = new CommonMoveStack();
				if(pickedMoveSource!=null)	// should not be null, but some replay games with damage have it.
				{
				getFrogMoveMoves(v, whoseTurn, pickedMoveSource,
						stillConnected(pickedMoveSource));
				for (int idx = 0, lim = v.size(); idx < lim; idx++) {
					FrogMovespec m = (FrogMovespec)v.elementAt(idx);
					FrogCell c = getCell(m.to_col, m.to_row);
					h.put(c, c);
				}}
			} else {
			}
			break;
		case PLAY_STATE:
			if (pickedObject != null) {
				CommonMoveStack v = new CommonMoveStack();
				getFrogPlaceMoves(v, whoseTurn, pickedSource, pickedObject);
				for (int idx = 0, lim = v.size(); idx < lim; idx++) {
					FrogMovespec m = (FrogMovespec) v.elementAt(idx);
					FrogCell c = getCell(m.to_col, m.to_row);
					h.put(c, c);
				}
			} 
			break;
		default:
			break;
		}
		cached_dests = h;
		return (h);
	}

	// mark all the cells starting from some one, considering one cell
	// to be artificailly empty and another to be artificially filled
	void sweepConnected(FrogCell c, FrogCell empty, FrogCell filled) {
		if (c.sweep_connected != sweep_connected) {
			c.sweep_connected = sweep_connected;
			for (int dir = 0; dir < 6; dir++) {
				FrogCell adj = c.exitTo(dir);
				FrogPiece top = adj.topChip();
				if ((adj != empty) // not the cell we will empty
						&& ((adj == filled) || (top != null))) {
					sweepConnected(adj, empty, filled);
				}
			}
		}
	}

	// true if the hive is all connected
	boolean allConnected(FrogCell empty, FrogCell filled) {
		int lim = occupiedCells.size();
		if (lim <= 2) {
			return (true);
		}
		if (!empty.canChangeConnectivity()) {
			return (true);
		}
		// if(empty==getCell('L',12))
		// {G.print("n");
		// }
		sweep_connected++;
		FrogCell seed = occupiedCells.elementAt(0);
		if (seed == empty) {
			seed = occupiedCells.elementAt(1);
		}
		sweepConnected(seed, empty, filled);
		for (int idx = 0; idx < lim; idx++) {
			FrogCell nxt = occupiedCells.elementAt(idx);
			if (nxt == empty) {
				nxt = filled;
			}
			if ((nxt != null) && (nxt.sweep_connected != sweep_connected)) {
				return (false);
			}
		}
		return (true);
	}

	boolean stillConnected(FrogCell empty) {
		return (allConnected(empty, null));
	}

	int frogStringLength(FrogCell c, FrogCell prev, FrogCell empty,	FrogCell filled) {
		FrogCell cont = null;
		int na = 0;
		int len = (c==empty)?0:1;
		for (int dir = 0; dir < 6; dir++) {
			FrogCell adj = c.exitTo(dir);
			if ((adj != empty) && (adj != prev)
					&& ((adj == filled) || (adj.topChip() != null))) {
				cont = adj;
				na++;
			}
		}
		return ((na != 1) ? 0 : len + frogStringLength(cont, c, empty, filled));
	}

	boolean isValidHive(FrogCell from, FrogCell to, boolean stillConnected) {
		if (!stillConnected) {
			if (!allConnected(from, to)) {
				return (false);
			}
		}
		if (frogStringLength(to, null, from, to) >= 3) {
			return (false);
		} // making long string junk

		for (int idx = 0, lim = occupiedCells.size(); idx < lim; idx++) {
			FrogCell c = occupiedCells.elementAt(idx);
			int len = frogStringLength(c, null, from, to);
			if (len >= 3) {
				return (false);
			}
		}
		return (true);
	}

	//
	// return true if there are moves.
	// if all!=null, add moves to it.
	//
	private boolean getFrogMoveMovesSweep(CommonMoveStack  all, int who, FrogCell origin,
			FrogCell from, boolean stillConnected) {
		boolean some = false;
		from.sweep_counter = sweep_counter;
		for (int dir = 0; dir < 6; dir++) {
			FrogCell start = from.exitTo(dir);
			if (start.topChip() != null) {
				FrogCell to = from.exitLine(dir, origin);
				if (to.sweep_counter < sweep_counter) {
					to.sweep_counter = sweep_counter;
					if (isValidHive(origin, to, stillConnected)) {
						some = true;
						if (all == null) {
							break;
						}
						all.addElement(new FrogMovespec(who, MOVE_MOVE,
								origin.col, origin.row, to.col, to.row));
					}
					getFrogMoveMovesSweep(all, who, origin, to, stillConnected);
				}
			}
		}
		return (some);
	}

	// get the frog moves from a single cell
	boolean getFrogMoveMoves(CommonMoveStack  all, int who, FrogCell from,
			boolean stillConnected) {
		sweep_counter++;
		return (getFrogMoveMovesSweep(all, who, from, from, stillConnected));
	}

	// get all the from-to moves for a player
	void getFrogMoveMoves(CommonMoveStack  all, int who) {
		int map[]=getColorMap();
		for (int idx = 0, lim = occupiedCells.size(); idx < lim; idx++) {
			FrogCell c = occupiedCells.elementAt(idx);
			FrogPiece top = c.topChip();
			if ((top != null) && (top.colorIndex == map[who])) {
				boolean conn = stillConnected(c);
				getFrogMoveMoves(all, who, c, conn);
				// if(movesteps>0) { G.print("c "+movesteps+" "+conn); }
			}

		}
	}

	// return true if the player has moves for his frogs
	boolean hasFrogMoves(int who) {
		FrogPiece target = FrogPiece.getChip(getColorMap()[who]);
		for (int idx = 0, lim = occupiedCells.size(); idx < lim; idx++) {
			FrogCell c = occupiedCells.elementAt(idx);
			FrogPiece top = c.topChip();
			if (top == target) {
				if (getFrogMoveMoves(null, who, c, stillConnected(c))) {
					return (true);
				}
			}
		}
		return (false);
	}

	void getFrogPlaceMoves(CommonMoveStack  all, int who) {
		FrogPiece prev = null;
		for (int i = 0; i < 2; i++) {
			FrogCell from = hand[who][i];
			FrogPiece frog = from.topChip();
			if (frog != prev) {
				getFrogPlaceMoves(all, who, from, frog);
			}
			prev = frog;
		}

	}

	CommonMoveStack  GetListOfMoves0(int who) {
		CommonMoveStack all = new CommonMoveStack();
		switch (board_state) {
		case MOVE_FROG_STATE:
			getFrogMoveMoves(all, who);
			break;
		case PLAY_STATE:
			// sweep the board for cell adjacent to occupied cells
			// where a placement is possible
			getFrogPlaceMoves(all, who);
			break;
		case PASS_STATE:
			all.addElement(new FrogMovespec(who, MOVE_PASS));
			break;
		default:
			throw G.Error("Not implemented");
		}

		return (all);
	}

	public CommonMoveStack  GetListOfMoves() {
		CommonMoveStack  all = GetListOfMoves0(whoseTurn);
		if (all.size() == 0) {
			all.addElement(new FrogMovespec(whoseTurn, MOVE_PASS));
		}
		return (all);
	}

}