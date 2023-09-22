/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package mutton;

import java.util.*;

import lib.AR;
import lib.G;
import online.game.*;

/**
 * MuttonGameBoard knows all about the game of Mutton, which is played
 * on a hexagonal board.
 *
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 *
 * The principle interface with the game viewer is the "Execute" method
 * which processes moves.
 *
 * In general, the state of the game is represented by the contents of the board,
 * whose turn it is, and an explicit state variable.  All the transitions specified
 * by moves are mediated by the state.  In general, my philosophy is to be extremely
 * restrictive about what to allow in each state, and have a lot of tripwires to
 * catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 * will be made and it's good to have the maximum opportunity to catch the unexpected.
 * 
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author rwalter42
 *
 */

class MuttonGameBoard extends BaseBoard implements BoardProtocol, MuttonConstants
{
	private MuttonState unresign;
 	private MuttonState board_state;
	public MuttonState getState() {return(board_state); }
	public void setState(MuttonState st) 
	{ 	unresign = (st==MuttonState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	// Indicates if this game is being played with the shotgun or not.
	private boolean shotgunEnabled = false;

	// Which player is the farmer during this phase of the game.
	private int farmerId = 0;

	// The number of wolves currently on the board.  This is used during the
	// initial setup to ensure that the wolf player hides 4 wolves on the
	// board.
	private int wolfCountOnBoard = 0;

	// The vector of sheep that are currently targeted.
	// Sheep can be targeted by the wolf for eating or by the farmer for shooting.
	private Vector<MuttonCell> targetedSheep = new Vector<MuttonCell> ();

	// A flag that indicates if the current relocation of suspects done after
	// the wolf has eaten a victim is a valid configuration or not.
	// If it is valid, then the wolf player may end his turn.
	private boolean relocationValid = true;

	// The data kept in the "History" panel that indicates which sheep have been
	// eaten and are suspects for each turn.  This is a vector of MuttonHistoryElements.
	private Vector<MuttonHistoryElement> eatenSheepHistory = new Vector<MuttonHistoryElement> ();

	public static Vector<MuttonHistoryElement> cloneHistory(Vector<MuttonHistoryElement>vector)
	{	int size = vector.size();
		Vector<MuttonHistoryElement>dup = new Vector<MuttonHistoryElement>(size);
		for(int i=0;i<size;i++) { dup.addElement(vector.elementAt(i)); }
		return(dup);
	}

	// An array of the overall sheep status.
	private int [] sheepStatus = new int [26];

	// The array of the board.  Each element of the board is a sheep.
	// Note that even the empty spaces on the board have sheep objects on them.
	private MuttonCell [][] board = new MuttonCell [BOARD_COLS][BOARD_ROWS];

	private int [][] initialSheepId = {
		{CELL_NOT_EXIST, CELL_NOT_EXIST, 7, 11, 15, CELL_NOT_EXIST, CELL_NOT_EXIST},
		{0, 4, 8, CELL_EMPTY, 16, 19, 22},
		{1, CELL_EMPTY, CELL_EMPTY, 12, CELL_EMPTY, CELL_EMPTY, 23},
		{2, 5, CELL_EMPTY, CELL_EMPTY, CELL_EMPTY, 20, 24},
		{3, CELL_EMPTY, 9, 13, 17, CELL_EMPTY, 25},
		{CELL_NOT_EXIST, 6, 10, CELL_EMPTY, 18, 21, CELL_NOT_EXIST},
		{CELL_NOT_EXIST, CELL_NOT_EXIST, CELL_NOT_EXIST, 14, CELL_NOT_EXIST, CELL_NOT_EXIST, CELL_NOT_EXIST}
	};

	// For each cell on the board, this is a list of the neighboring cells
	private Vector<MuttonCell> [][] neighbors;

	// Indication of whether or not a valid meal exists for the wolf player.
	private boolean validMealExists;

	// The sheep that was most recently eaten.
	private MuttonCell mostRecentVictim;

	// A list of the suspects that were next to the most recent victim.
	private Vector<MuttonCell> mostRecentSuspects = new Vector<MuttonCell>();

	// Information about the sheep that is currently being dragged around by
	// the mouse.
	private int movingSheepId = -1;
	private MuttonCell movingSheepSourceCell;

	// The number of dead sheep that the wolf needs in order to win the game.
	private int wolfWinTarget = DEFAULT_DEAD_SHEEP_NEEDED_FOR_WOLF_WIN;

	// Remember if the wolf has passed or not.
	// This is needed by the robot players.
	private boolean wolfHasPassed = false;


	/**
	 * Constructor for a new Mutton game board.
	 *
	 * @param gameTypeString   The initialization string that indicates the
	 *                         type of game that will be played.
	 * @param randomSeed       The seed to use for randomization on the game.
	 *                         Mutton uses this to randomly rotate the animals
	 *                         in the field.
	 */
	@SuppressWarnings("unchecked")
	public MuttonGameBoard (String gameTypeString, long randomSeed) {
		// Create things that only need to be done once.
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				board[c][r] = new MuttonCell(c, r, initialSheepId[c][r]);
			}
		}

		// Reset the board
		doInit(gameTypeString, randomSeed);

		// Initialize the array of vectors that hold the neighbors for
		// each cell.
		neighbors = new Vector[BOARD_COLS][BOARD_ROWS];
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				neighbors[c][r] = new Vector<MuttonCell>();
				addNeighbor(neighbors[c][r], c, r-1);
				addNeighbor(neighbors[c][r], c, r+1);
				addNeighbor(neighbors[c][r], c-1, r);
				addNeighbor(neighbors[c][r], c+1, r);
				if ((r & 0x01) == 0) {
					// Even row
					addNeighbor(neighbors[c][r], c+1, r-1);
					addNeighbor(neighbors[c][r], c+1, r+1);
				} else {
					// Odd row
					addNeighbor(neighbors[c][r], c-1, r-1);
					addNeighbor(neighbors[c][r], c-1, r+1);
				}
			}
		}
	}

	/**
	 * Clone this board and return the clone.
	 */
	public MuttonGameBoard cloneBoard() {
		MuttonGameBoard dup = new MuttonGameBoard (gametype, randomKey);
		dup.copyFrom(this);
		return (dup);
	}
    public void copyFrom(BoardProtocol b) { copyFrom((MuttonGameBoard)b); }

	/**
	 * Initialize the board back to the initial state.
	 *
	 * @param gameTypeString   The initialization string that indicates the
	 *                         type of game that will be played.
	 * @param randomSeed       The seed for the random number generator that
	 *                         creates the rotations of the animals.
	 */
	public void doInit (String gameTypeString, long randomSeed) {
		// Do general initialization required by BaseBoard
		gametype = gameTypeString;
		moveNumber = 1;

		// Set the random seed for the animal rotations
		randomKey = randomSeed;

		// Do Mutton game specific initialization
		MuttonInit(randomSeed);

		// The farmer player starts the game in board configure state.
		setFarmerTurn();
		setState(MuttonState.FARMER_CONFIGURING_BOARD);
	}

	/*
	 * Specific initialization for game of Mutton.
	 * 
	 * @param randomSeed       The seed for the random number generator that
	 *                         creates the rotations of the animals.
	 */
	private void MuttonInit (long randomSeed) {
		// Set game variant parameters.
		shotgunEnabled = Mutton_SHOTGUN_INIT.equalsIgnoreCase(gametype);

		// Misc. initialization
		farmerId = 0;
		wolfCountOnBoard = 0;
		wolfWinTarget = DEFAULT_DEAD_SHEEP_NEEDED_FOR_WOLF_WIN;
		targetedSheep.setSize(0);
		movingSheepId = -1;
		movingSheepSourceCell = null;
		Random randRotationGen = new Random(randomSeed);
		validMealExists = false;
		relocationValid = true;
		wolfHasPassed = false;

		// Set the mostRecentVictim to one that is off the board.
		// It just can't be null.
		mostRecentVictim = board[0][0]; 

		// Clear the history & status panel.
		for (int i=0; i<sheepStatus.length; i++) {
			sheepStatus[i] = SHEEP_STATUS_ALIVE;
		}
		eatenSheepHistory.setSize(0);
		

		// Clear the board and set the initial sheep on it
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				board[c][r].init(initialSheepId[c][r]);
				board[c][r].setDisplayRotation(Math.abs(randRotationGen.nextInt() % 6));
			}
		}
	}

	/*
	 * Add the cell on space <col, row> to the currList vector, if the target
	 * space actually exists on the board.
	 */
	private void addNeighbor (Vector<MuttonCell> currList, int col, int row) {
		if ((col >= 0) && (col < BOARD_COLS) &&
		    (row >= 0) && (row < BOARD_ROWS)) {
			MuttonCell neighbor = board[col][row];
			if (neighbor.isOnBoard()) {
				currList.addElement(neighbor);
			}
		}
	}

	/**
	 * Return the Cell that is on the board at the given location.
	 *
	 * @param col    The column to return the cell from
	 * @param row    The row to return the cell from
	 * @return the cell at (col, row) of the board.
	 */
	public MuttonCell getCell (int col, int row) {
		if ((col >= 0) && (col < BOARD_COLS) &&
		    (row >= 0) && (row < BOARD_ROWS)) {
			return board[col][row];
		}
		// Cell 0,0 is known to be off the board, so it is returned for
		// requests that lie off the board.
		return board[0][0];
	}

	/**
	 * Return the MuttonHistoryElement for the given game turn.
	 *
	 * @param gameTurn   The game turn for which history element is desired.
	 *                   This is 0-based, so the first turn of the game is turn 0.
	 * @return the history element for the requested game turn.
	 */
	public MuttonHistoryElement getHistoryForTurn (int gameTurn) {
		return eatenSheepHistory.elementAt(gameTurn);
	}

	/**
	 * Return the number of entries that are in the eatenSheepHistory.
	 *
	 * @return the number of entries that are in the eatenSheepHistory.
	 */
	public int getHistorySize () {
		return eatenSheepHistory.size();
	}

	/**
	 * Return if this game is playing with the shotgun variant enabled.
	 * 
	 * @return if this game is playing with the shotgun variant enabled.
	 */
	public boolean isShotgunEnabled () {
		return shotgunEnabled;
	}

	/**
	 * Return a string that contains the numbers of the sheep that are the
	 * wolves.
	 */
	public String getWolfIdsString () {
		String result = "";

		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				if (board[c][r].isWolf()) {
					result += (board[c][r].getSheepId() + " ");
				}
			}
		}

		return result;
	}

	/**
	 * Return a string the contains the numbers of the sheep that are currently
	 * targeted.
	 */
	public String getTargetedSheepIds () {
		String result = "";
		for (int i = 0; i < targetedSheep.size(); i++) {
			MuttonCell c = targetedSheep.elementAt(i);
			result += c.getSheepId() + " ";
		}

		return result;
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
	 * @return the digest for the current board state.
	 */
	private boolean verboseDigest = false;
	public long Digest () {
		// The basic technique used here is to continually add new information
		// to an accumulator.  For each new item added, the old value of the
		// accumulator is multiplied by 17 and then the new value is added.
		// Once this goes into production, it can't be changed without invalidating
		// all of the existing digests.
		long val = 0;

		if (verboseDigest) {System.out.println("Digest for " + this);}
		val = digestAdd(val, shotgunEnabled ? 1 : 0);
		val = digestAdd(val, farmerId);
		val = digestAdd(val, wolfCountOnBoard);
		val = digestAdd(val, wolfWinTarget);

		if (verboseDigest) {System.out.println("  p1: " + val);}
		for (int i=0; i<eatenSheepHistory.size(); i++) {
			MuttonHistoryElement e = eatenSheepHistory.elementAt(i);
			val = digestAdd(val, e.getDigestValue());
		}

		if (verboseDigest) {System.out.println("  p2: " + val);}
		for (int i=0; i<sheepStatus.length; i++) {
			val = digestAdd(val, sheepStatus[i]);
		}

		if (verboseDigest) {System.out.println("  p3: " + val);}
		for (int c=0; c<BOARD_COLS; c++) {
			for (int r=0; r<BOARD_ROWS; r++) {
				MuttonCell cell = board[c][r];
				if (cell.isOnBoard() && !cell.isEmpty()) {
					val = digestAdd(val, cell.getSheepId());
					val = digestAdd(val, cell.isAlive() ? 1 : 0);
					val = digestAdd(val, cell.getHighlightCode(PRE_ANIMAL_HIGHLIGHT));
					val = digestAdd(val, cell.getHighlightCode(POST_ANIMAL_HIGHLIGHT));
				}
			}
		}

		if (verboseDigest) {System.out.println("  p4: " + val);}
		val = digestAdd(val, mostRecentVictim.getSheepId());

		val = digestAdd(val, whoseTurn);
		val = digestAdd(val, moveNumber);
		val = digestAdd(val, win[0] ? 1 : 0);
		val = digestAdd(val, win[1] ? 1 : 0);
		val = digestAdd(val, board_state.ordinal());
		if (verboseDigest) {System.out.println("  p5: " + val);}
		return (val);
	}

	/*
	 * A helper function for digest.  This adds the value of a newAddition to
	 * the given accumulator value and returns the new value.
	 */
	private long digestAdd (long oldVal, long newAddition) {
		return (oldVal * 17) + newAddition;
	}

	/**
	 * Copy the state of the given board into myself.
	 * This is used by the robot to get a copy of the board for it to
	 * manipulate and analyze without affecting the board that is being
	 * displayed.
	 *
	 * @param srcBoard    The board that I should become a copy of.
	 */
	public void copyFrom (MuttonGameBoard srcBoard) 
	{	super.copyFrom(srcBoard);
		board_state = srcBoard.board_state;
		unresign = srcBoard.unresign;
		// Copy Mutton specific stuff over from the srcBoard into myself.

		farmerId = srcBoard.farmerId;
		wolfCountOnBoard = srcBoard.wolfCountOnBoard;
		wolfWinTarget = srcBoard.wolfWinTarget;
		// targetedSheep - Doesn't need to be cloned.
		// relocationValid - Doesn't need to be cloned.
		if (srcBoard.mostRecentVictim == null) {
			mostRecentVictim = null;
		} else {
			mostRecentVictim = getCell(srcBoard.mostRecentVictim.getCol(),
			                           srcBoard.mostRecentVictim.getRow());
		}
		movingSheepId = srcBoard.movingSheepId;
		if (srcBoard.movingSheepSourceCell == null) {
			movingSheepSourceCell = null;
		} else {
			movingSheepSourceCell = getCell(srcBoard.movingSheepSourceCell.getCol(),
			                                srcBoard.movingSheepSourceCell.getRow());
		}

		// movingSheepId - Doesn't need to be cloned.
		// movingSheepSourceCell - Doesn't need to be cloned.
		validMealExists = srcBoard.validMealExists;
		wolfHasPassed = srcBoard.wolfHasPassed;

		// Copy the list of most recent suspects
		mostRecentSuspects.setSize(0);
		for (int i = 0; i < srcBoard.mostRecentSuspects.size(); i++) {
			MuttonCell suspect = srcBoard.mostRecentSuspects.elementAt(i);
			mostRecentSuspects.addElement(getCell(suspect.getCol(), suspect.getRow()));
		}

		// Copy the sheep history table.
		// Since history elements are immutable, a clone of the source vector
		//  is good enough
		eatenSheepHistory = cloneHistory(srcBoard.eatenSheepHistory);

		// Copy the sheep status
		for (int i = 0; i < sheepStatus.length; i++) {
			sheepStatus[i] = srcBoard.sheepStatus[i];
		}

		// Copy the board.
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				board[c][r].copyFrom(srcBoard.board[c][r]);
			}
		}

		// If debug is enabled, then check to make sure that we really are a
		// clone.
		sameboard(srcBoard);
	}
    public void sameboard(BoardProtocol f) { sameboard((MuttonGameBoard)f); }
	/**
	 * Robots use this to verify a copy of a board.  If the clone() method is
	 * implemented correctly, there should never be a problem.  This is mainly
	 * a bug trap to see if BOTH the clone() and sameboard() methods agree.
	 *
	 * @param srcBoard   The board to compare ourselves to.
	 */
	public void sameboard (MuttonGameBoard srcBoard) 
	{	super.sameboard(srcBoard);

		// Verify other Mutton specific board state
		G.Assert(shotgunEnabled == srcBoard.shotgunEnabled, "shotgunEnabled doesn't match");
		G.Assert(farmerId == srcBoard.farmerId, "farmerId doesn't match");
		G.Assert(wolfWinTarget == srcBoard.wolfWinTarget, "wolfWinTarget doesn't match");
		G.Assert(wolfCountOnBoard == srcBoard.wolfCountOnBoard, "wolfCountOnBoard doesn't match");
		if (mostRecentVictim == null) {
			G.Assert(srcBoard.mostRecentVictim == null, "mostRecentVictim is (unexpectedly) not null");
		} else {
			G.Assert(mostRecentVictim.sameCell(srcBoard.mostRecentVictim), "mostRecentVictim doesn't match");
		}
		G.Assert(validMealExists == srcBoard.validMealExists, "validMealExists doesn't match");
		G.Assert(wolfHasPassed == srcBoard.wolfHasPassed, "wolfHasPassed doesn't match");

		// Verify sheep status matches
		for (int i = 0; i < sheepStatus.length; i++) {
			G.Assert(sheepStatus[i] == srcBoard.sheepStatus[i], "sheepStatus doesn't match");
		}

		G.Assert((movingSheepId == srcBoard.movingSheepId), "movingSheepId doesn't match");
		if (movingSheepSourceCell == null) {
			G.Assert((srcBoard.movingSheepSourceCell == null), "movingSheepSourceCell doesn't match");
		} else {
			G.Assert(movingSheepSourceCell.sameCell(srcBoard.movingSheepSourceCell),
			         "movingSheepSourceCell doesn't match");
		}

		// Verify most recent suspects are the same
		// Note: This assumes that the list of suspects is in the same order.
		//       Technically, order of this list isn't required to be the same.
		//       (ie: "A B" is the same as "B A", but I'm being lazy (again!).
		G.Assert(mostRecentSuspects.size() == srcBoard.mostRecentSuspects.size(), "mostRecentSuspects are different lengths");
		for (int i = 0; i < mostRecentSuspects.size(); i++) {
			MuttonCell a = mostRecentSuspects.elementAt(i);
			MuttonCell b = srcBoard.mostRecentSuspects.elementAt(i);
			G.Assert(a.sameCell(b), "mostRecentSuspects don't match");
		}

		// Verify that the eatenSheepHistory are the same.
		G.Assert(eatenSheepHistory.size() == srcBoard.eatenSheepHistory.size(), "eatenSheepHistory are different lengths");
		for (int i = 0; i < eatenSheepHistory.size(); i++) {
			MuttonHistoryElement a =eatenSheepHistory.elementAt(i);
			MuttonHistoryElement b =srcBoard.eatenSheepHistory.elementAt(i);
			G.Assert(a.sameHistory(b), "eatenSheepHistory don't match");
		}
		
		// Verify the board layout matches.
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				G.Assert(board[c][r].sameCell(srcBoard.board[c][r]), "boards don't match");
			}
		}
		// This is a good overall check that all the copy/check/digest methods
		// are in sync, although if this does fail you'll no doubt be at a loss
		// to explain why.
		G.Assert(Digest() == srcBoard.Digest(), "Digest doesn't match");
	}

	/**
	 * Return the current status of a sheep.
	 * The status is one of Alive, Dead_Sheep, or Dead_Wolf.
	 */
	public int getSheepStatus (int sheepId) {
		return sheepStatus[sheepId];
	}

	/**
	 * Return the seat Id of which player is the farmer.
	 */
	public int getFarmerId () {
		return farmerId;
	}

	/**
	 * Return if the requested seat number is the wolf player or not.
	 */
	public boolean isWolfPlayer (int seatNum) {
		return (seatNum == (1 - farmerId));
	}

	/**
	 * Return the current number of sheep that are targeted.
	 */
	public int getNumberOfSheepTargeted () {
		return targetedSheep.size();
	}

	/**
	 * Return if the wolf player has at least one valid meal on the board.
	 * @return if the wolf player has at least one valid meal on the board.
	 */
	public boolean wolfHasValidMeal () {
		return validMealExists;
	}

	/**
	 * Return the number of sheep that the wolf player needs to kill to win.
	 */
	public int getWolfWinTarget () {
		return wolfWinTarget;
	}

	/**
	 * Return the number of live wolves still on the board
	 */
	public int getWolfCount () {
		return wolfCountOnBoard;
	}

	/**
	 * Set the wolf player to be the current player
	 */
	private void setWolfTurn () {
		whoseTurn = 1 - farmerId;
	}

	/**
	 * Set the farmer player to be the current player
	 */
	private void setFarmerTurn () {
		whoseTurn = farmerId;
	}


	/** This is used to determine if the "Done" button in the UI is live.
	 *
	 * @return if the Done button should be active or not.
	 */
	public boolean DoneState () {
		switch (board_state) {
			case WOLF_HIDING_STATE :
				return (wolfCountOnBoard == 4);
			case WOLF_CHOOSING_MEAL_STATE :
				return (targetedSheep.size() == 1) || !validMealExists;
			case WOLF_MOVING_SHEEP_STATE :
			case FARMER_MOVING_SHEEP_STATE :
				return relocationValid;
			case FARMER_CHOOSING_TARGETS_STATE :
			case FARMER_CONFIGURING_BOARD :
			case RESIGN_STATE:
				return true;
		default:
			break;
		}

		return false;
	}

	/**
	 * Toggle the targeted state of the given cell.
	 * 
	 *    // RAW: perhaps rather than keep a vector of targeted cells, just
	 *    //      add a flag to the MuttonCell for targeted status and keep
	 *    //      a count of cells marked.  (Similar to how the wolfCount works
	 *    //      during initial wolf selection.)
	 */
	public void toggleCellTargeted (MuttonCell targetCell, boolean allowMultiple) {
		if (targetCell.getHighlightCode(PRE_ANIMAL_HIGHLIGHT) == HIGHLIGHT_AS_TARGETED) {
			// This cell is highlighted, so remove it from the list.
			targetCell.setHighlightCode(PRE_ANIMAL_HIGHLIGHT, HIGHLIGHT_NONE);
			targetedSheep.removeElement(targetCell);
		} else {
			// This cell is not highlighted, so add it to the list.
			targetCell.setHighlightCode(PRE_ANIMAL_HIGHLIGHT, HIGHLIGHT_AS_TARGETED);
			targetedSheep.addElement(targetCell);

			// If we are not to allow multiple targets, then untarget all but the
			// last one (which is the one we just added)
			if (!allowMultiple) {
				untargetAllBut(1);
			}
		}
	}

	/*
	 * Untarget animals starting from the oldest until the number of targeted
	 * animals gets below the given limit.
	 */
	public void untargetAllBut (int limit) {
		while (targetedSheep.size() > limit) {
			MuttonCell c = targetedSheep.elementAt(0);
			targetedSheep.removeElementAt(0);
			c.setHighlightCode(PRE_ANIMAL_HIGHLIGHT, HIGHLIGHT_NONE);
		}
	}

	/**
	 * Return the vector of targeted cells.
	 */
	public Vector<MuttonCell> getTargetedCells () {
		return targetedSheep;
	}

	/**
	 * Return the Id of the sheep that is currently being dragged around.
	 */
	public int getMovingSheepId () {
		return movingSheepId;
	}

	/**
	 * Return if it is legal to begin moving the animal at the given location
	 */
	public boolean canMoveAnimalFrom (MuttonCell sourceCell) {
		return (((board_state == MuttonState.WOLF_MOVING_SHEEP_STATE) &&
		        (sourceCell.getHighlightCode(POST_ANIMAL_HIGHLIGHT) == HIGHLIGHT_AS_SUSPECT)) ||
		       ((board_state == MuttonState.FARMER_MOVING_SHEEP_STATE) &&
		        (sourceCell.getHighlightCode(POST_ANIMAL_HIGHLIGHT) == HIGHLIGHT_AS_SCARED)) ||
		       (board_state == MuttonState.FARMER_CONFIGURING_BOARD));
	}

	/**
	 * Attempt to start moving the sheep that is sitting on the given cell.
	 */
	public void startMovingSheep (MuttonCell sourceCell) {
		if (canMoveAnimalFrom (sourceCell)) {
			// The source cell is a valid animal to drag around.
			movingSheepId = sourceCell.getSheepId();
			movingSheepSourceCell = sourceCell;
			sourceCell.setSheepId(CELL_EMPTY);
		}
	}

	/**
	 * Return the moving sheep back to it's starting place.
	 */
	public void returnMovingSheep () {
		movingSheepSourceCell.setSheepId(movingSheepId);
		movingSheepId = -1;
		movingSheepSourceCell = null;
	}

	// Get the index in the image array corresponding to movingObjectChar 
	// or HitNoWhere if no moving object.  This is used to determine what
	// to draw when tracking the mouse.
	public int movingObjectIndex () {
		return (movingSheepId >= 0) ? movingSheepId : NothingMoving;
	}

	/**
	 * Indicate if the moving animal is a wolf or not.
	 */
	public boolean isMovingAnimalWolf () {
		return (movingSheepSourceCell != null) ?
		            movingSheepSourceCell.isWolf() :
		            false;
	}

	/**
	 * Indicate the rotation value to use for the moving animal. 
	 * @return the rotation value to use for the moving animal.
	 */
	public int getMovingAnimalDisplayRotation () 
	{
		return (movingSheepSourceCell != null) ? 
		            movingSheepSourceCell.getDisplayRotation() :
		            -1;
	}

	/**
	 * Determine if the given cell is a valid target for the mouse in the GUI.
	 * If this returns true, then the cell will be highlighted in the GUI.
	 * If this returns false, then the cell will not be highlighted in the GUI.
	 */
	public boolean isValidMouseTarget (MuttonCell tgtCell) {
		switch (board_state) {
		case WOLF_HIDING_STATE :
		case FARMER_CHOOSING_TARGETS_STATE :
			return tgtCell.isAlive();

		case WOLF_CHOOSING_MEAL_STATE :
			return tgtCell.isValidMeal();

		case WOLF_MOVING_SHEEP_STATE :
		case FARMER_MOVING_SHEEP_STATE :
			if (movingSheepSourceCell == null) {
				// Nothing is currently dragging, so valid targets are those
				// that can be picked up.
				int highlightCode = tgtCell.getHighlightCode(POST_ANIMAL_HIGHLIGHT);
				return ((highlightCode == HIGHLIGHT_AS_SUSPECT) ||
				        (highlightCode == HIGHLIGHT_AS_SCARED));
			} else {
				// An animal is being dragged, so valid spaces are those that
				// an animal can be put down on.
				return isValidMoveDestination(tgtCell);
			}

		case FARMER_CONFIGURING_BOARD :
			if (movingSheepSourceCell == null) {
				// Nothing is currently dragging, so valid targets are those
				// that can be picked up.
				return tgtCell.isAlive();
			} else {
				// An animal is being dragged, so valid spaces are those that
				// an animal can be put down on.
				return isValidMoveDestination(tgtCell);
			}

		default :
			return false;
		}
	}

	/**
	 * Determine if the given cell is a valid location to move an animal to.
	 * A cell to be moved to must be empty and must not be next to the
	 * most recent victim.
	 * 
	 * @param tgtCell    The cell to test for validity
	 * @return if the target cell is a valid destination.
	 */
	public boolean isValidMoveDestination (MuttonCell tgtCell) {
		// Must move to an empty cell.
		if (!tgtCell.isEmpty()) {
			return false;
		}

		// If the target cell is a neighbor of the most recent victim,
		// then it isn't a valid move.
		return !neighbors[tgtCell.getCol()][tgtCell.getRow()].contains(mostRecentVictim);
	}

	// This is the default, so we don't need it explicitly here.
	// but games with complex "rearrange" states might want to be
	// more selective.  This determines if the current board digest is added
	// to the repetition detection machinery.
	// For Mutton, we don't want to record the moves made when either the wolf
	// or farmer player is re-arranging the animals on the board.
	public boolean DigestState() {
		
		if ((board_state == MuttonState.WOLF_MOVING_SHEEP_STATE) ||
		    (board_state == MuttonState.FARMER_MOVING_SHEEP_STATE) ||
		    (board_state == MuttonState.FARMER_CONFIGURING_BOARD)) {
			return false;
		}

		return (DoneState());
	}

	/**
	 * Return the number of dead sheep so far in the game.
	 * @return the number of dead sheep so far in the game.
	 */
	public int getDeadSheepCount () {
		int deadSheepCount = 0;
		for (int i=0; i<sheepStatus.length; i++) {
			if (sheepStatus[i] == SHEEP_STATUS_DEAD_SHEEP) {
				deadSheepCount += 1;
			}
		}
		return deadSheepCount;
	}

	/**
	 * Determine if a player has won.
	 * If the number of live wolves on the board is reduced to 0, then the farmer wins.
	 * If there are more dead sheep than the number required to win, then the wolf
	 * wins.
	 */
	private void checkForWin () {
		// The farmer wins if he kills all of the wolves on the board.
		if ((wolfCountOnBoard == 0) && (moveNumber > 0)) {
			win[farmerId] = true;
			return;
		}

		// The wolf wins if there are more than the required number of dead sheep.
		if (getDeadSheepCount() >= wolfWinTarget) {
			win[1-farmerId] = true;
		}
	}

	/*
	 * Given a sheepId, return the cell that that sheep is sitting on.
	 * If the sheepId isn't found on the board, then an off-board cell
	 * is returned.
	 */
	public MuttonCell findCellWithSheep (int sheepId) {
		if (sheepId >= 0) {
			for (int c = 0; c < BOARD_COLS; c++) {
				for (int r = 0; r < BOARD_ROWS; r++) {
					if (board[c][r].getSheepId() == sheepId)
						return board[c][r];
				}
			}
		}

		// Cell [0][0] isn't on the board, so returning this is guaranteed
		// to be an off-board cell.
		return board[0][0];
	}

	/*
	 * Change the wolf hiding status of the sheep with the given Id.
	 */
	public void doSheepWolfMutate (int sheepId) {
		MuttonCell tgtCell = findCellWithSheep(sheepId);
		if ((board_state == MuttonState.WOLF_HIDING_STATE) &&
		     tgtCell.isOnBoard() &&
		    !tgtCell.isEmpty() &&
		     tgtCell.isAlive()) {
			if (tgtCell.isWolf()) {
				tgtCell.setWolf(false);
				wolfCountOnBoard -= 1;
			} else if (wolfCountOnBoard < 4) {
				tgtCell.setWolf(true);
				wolfCountOnBoard += 1;
			}
		}
	}

	/*
	 * Set all of the animals on the board to sheep, except for the ones in
	 * the given list which will be made to be wolves.
	 */
	private void hideWolves (int [] wolfIds) {
		// Mark all animals as sheep.
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				board[c][r].setWolf(false);
			}
		}

		// Go through the wolf id and set each of them to wolves.
		for (int i = 0; i < wolfIds.length; i++) {
			MuttonCell cell = findCellWithSheep(wolfIds[i]);
			cell.setWolf(true);
		}

		wolfCountOnBoard = wolfIds.length;
	}

	/*
	 * Get the game board ready for the wolf player to choose his victim.
	 */
	private void getReadyForWolfToEat () {
		if ((board_state == MuttonState.WOLF_HIDING_STATE) ||
		    (board_state == MuttonState.FARMER_CHOOSING_TARGETS_STATE) ||
		    (board_state == MuttonState.FARMER_MOVING_SHEEP_STATE)) {
			// If the farmer is using the shotgun, then remove the dead bodies
			// from the board before getting ready for the wolf to eat.
			if (shotgunEnabled) {
				for (int c = 0; c < BOARD_COLS; c++) {
					for (int r = 0; r < BOARD_ROWS; r++) {
						MuttonCell cell = board[c][r];
						if (!cell.isEmpty() && !cell.isAlive() && cell.isOnBoard()) {
							cell.clear();
						}
					}
				}
			}

			// Mark all of the sheep that are next to a wolf as potential
			// meals.  This allows us to do this scan only once to determine
			// which sheep are valid targets rather than having to do it as the
			// cursor moves around the board.
			// This also checks to make sure that there is at least one valid
			// meal on the board.  If there isn't then the wolf player is
			// forced to pass.
			rescanValidMeals();

			setWolfTurn();
			setState(MuttonState.WOLF_CHOOSING_MEAL_STATE);
		}
	}

	/**
	 * Scan the board, setting the value of the valid meal flag on all
	 * spaces to the appropriate value for the current board configuration.
	 */
	public void rescanValidMeals() {
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				board[c][r].setValidMealFlag(false);
			}
		}

		validMealExists = false;
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				MuttonCell cell = board[c][r];
				if (cell.isWolf() && cell.isAlive()) {
					markCellsAsMeals(neighbors[c][r]);
				}
			}
		}
	}

	/*
	 * Mark all cells in the given list as valid meals.
	 */
	private void markCellsAsMeals (Vector<MuttonCell> lneighbors) {
		for (int i=0; i<lneighbors.size(); i++) {
			MuttonCell cell = lneighbors.elementAt(i);
			if (!cell.isEmpty() && cell.isAlive()) {
				cell.setValidMealFlag(true);
				validMealExists = true;
			}
		}
	}

	/*
	 * Do all the work required for the wolf player to eat the victim
	 * with the given id.
	 *
	 * @param victimId   The id of the animal to be eaten.
	 */
	private void doWolfEat (int victimId) {
		MuttonCell victim = findCellWithSheep(victimId);
		if ((board_state == MuttonState.WOLF_CHOOSING_MEAL_STATE) &&
		    (victim.isValidMeal())) {
			// Clear any most-recent-victim highlights that are left on the
			// board from the farmer's last shooting.  Note: mostRecentVictim
			// holds the id of the last animal shot by the farmer.  In the case
			// that the farmer went on a rampage and shot multiple animals, then
			// only the last one is saved.  However, in that case the game will
			// end and the wolf player won't have a chance to eat anymore, and
			// so we'll never get here anyway...
			mostRecentVictim.setHighlightCode(PRE_ANIMAL_HIGHLIGHT, HIGHLIGHT_NONE);
			setHighlightCode(mostRecentSuspects, POST_ANIMAL_HIGHLIGHT, HIGHLIGHT_NONE);

			// The new victim is now the mostRecent one.
			mostRecentVictim = victim;
			boolean wasWolf = mostRecentVictim.isWolf();

			// Kill the animal in the cell
			mostRecentVictim.setAlive(false);
			sheepStatus[victimId] = wasWolf ?
			                            SHEEP_STATUS_DEAD_WOLF :
			                            SHEEP_STATUS_DEAD_SHEEP;

			// Get a list of the suspects (the neighbors of the victim).
			getAliveNeighbors(mostRecentVictim, mostRecentSuspects);

			// Mark the victim & suspects so that they are drawn correctly in the GUI
			mostRecentVictim.setHighlightCode(PRE_ANIMAL_HIGHLIGHT, HIGHLIGHT_AS_LAST_VICTIM);
			setHighlightCode(mostRecentSuspects, POST_ANIMAL_HIGHLIGHT, HIGHLIGHT_AS_SUSPECT);

			// Remove the victim from the targeted list
			targetedSheep.setSize(0);

			// Create a history element indicating that the victim was eaten
			// and who the suspects are.
			eatenSheepHistory.addElement(
			      new MuttonHistoryElement(victimId,
			                               wasWolf,
			                               createListOf(mostRecentSuspects)));

			// If a wolf has been eaten, then reduce the count of live wolves.
			if (wasWolf) {
				wolfCountOnBoard -= 1;
			}

			// Suspect animals must be moved.
			relocationValid = false;

			// Check to see if the game is over, which can happen if the wolf
			// has eaten enough sheep to go over the limit.
			checkForWin();

			// If the game is over, then move to gameOver state, otherwise the
			// wolf needs to move the suspect sheep around.
			if (win[1-farmerId]) {
				setState(MuttonState.GAMEOVER_STATE);
			} else {
				setState(MuttonState.WOLF_MOVING_SHEEP_STATE);
			}
		}
	}

	/*
	 * If the wolf player has no valid meals (none of the wolves that are
	 * still alive have living neighbors to eat), then the wolf player must
	 * pass.  In this case, we still want to clean up the state of the
	 * board and move on to the farmer's turn to shoot.
	 */
	private void doWolfMustPassEat () {
		if ((board_state == MuttonState.WOLF_CHOOSING_MEAL_STATE) && !validMealExists) {
			// Clear any most-recent-victim highlights that are left on the
			// board from the farmer's last shooting.  Note: mostRecentVictim
			// holds the id of the last animal shot by the farmer.  In the case
			// that the farmer went on a rampage and shot multiple animals, then
			// only the last one is saved.  However, in that case the game will
			// end and the wolf player won't have a chance to eat anymore, and
			// so we'll never get here anyway...
			mostRecentVictim.setHighlightCode(PRE_ANIMAL_HIGHLIGHT, HIGHLIGHT_NONE);
			setHighlightCode(mostRecentSuspects, POST_ANIMAL_HIGHLIGHT, HIGHLIGHT_NONE);

			// There is no mostRecent victim, so set it to off the board.
			mostRecentVictim = board[0][0];

			// Clear the list of suspects.
			mostRecentSuspects.setSize(0);

			// Clear the targeted sheep.
			targetedSheep.setSize(0);

			// The game cannot be won or lost here, since no animal died.
			// So, we go directly to the farmer shooting phase.
			setState(MuttonState.FARMER_CHOOSING_TARGETS_STATE);
			moveNumber += 1;
			setFarmerTurn();

			// Remember that the wolf has passed
			wolfHasPassed = true;
		}
	}

	/*
	 * Given a cell, this will populate the neighborList with all of the cells
	 * that are neighbors of the given cell that actually have alive animals
	 * on them.
	 *
	 * @param centerCell    The cell whose neighbors are desired.
	 * @param neighborList  The vector to fill with the neighbors.
	 */
	public void getAliveNeighbors (MuttonCell centerCell, Vector<MuttonCell> neighborList) {
		neighborList.setSize(0);

		Vector<MuttonCell> n = neighbors[centerCell.getCol()][centerCell.getRow()];
		for (int i = 0; i < n.size(); i++) {
			MuttonCell c = n.elementAt(i);
			if (c.isAlive() && !c.isEmpty()) {
				neighborList.addElement(c);
			}
		}
	}

	/*
	 * Given a list of cells and a highlight code, set the highlight code
	 * on all of the cells.
	 */
	private void setHighlightCode (Vector<MuttonCell> cells, int whichCode, int newCode) {
		for (int i=0; i<cells.size(); i++) {
			MuttonCell c = cells.elementAt(i);
			c.setHighlightCode(whichCode, newCode);
		}
	}

	/*
	 * Handle moving a suspect sheep after the wolf has eaten a victim.
	 * Or handle moving a scared sheep after the farmer has shot a suspect with
	 * the shotgun.
	 *
	 * @param theSheepId     The id of the animal that is moving
	 * @param destination    The coordinates of where the animal is moving to
	 */
	private void doRelocateAnimal (int theSheepId, int [] destination) {
		if ((board_state == MuttonState.WOLF_MOVING_SHEEP_STATE) ||
		    (board_state == MuttonState.FARMER_MOVING_SHEEP_STATE)) {
			// Find the source & destination cells.
			MuttonCell fromCell;
			if (movingSheepSourceCell == null) {
				// This relocate move did not have a "Pick" move preceding it,
				// so we need to find the cell that has the movingSheepId on it.
				fromCell = findCellWithSheep(theSheepId);
			} else {
				// This relocate move did have a "Pick" move preceding it,
				// so movingSheepSourceCell is the source cell, but we need to
				// put the animal back on it before we can move it.
				fromCell = movingSheepSourceCell;
				fromCell.setSheepId(movingSheepId);
			}
			MuttonCell toCell = getCell(destination[0], destination[1]);

			// Verify that this move is valid.
			if (fromCell.isOnBoard() && toCell.isOnBoard() && toCell.isEmpty() &&
			    mostRecentSuspects.contains(fromCell)) {
				// Move the animal.
				toCell.moveAnimalFrom(fromCell);
				mostRecentSuspects.removeElement(fromCell);
				mostRecentSuspects.addElement(toCell);

				// Determine if all of the victims have been moved to valid spaces
				relocationValid = checkRelocationValid();
			}

			// Forget that we were dragging around anything.
			movingSheepId = -1;
			movingSheepSourceCell = null;
		}
	}

	/*
	 * Determine if the current configuration on the board is valid for the
	 * player to end his turn during relocation of the suspects.
	 * The wolf player must simply make sure that there are no animals
	 * next to the most recent victim.
	 * The farmer player has the additional restriction that every scared
	 * animal must be next to another live one.
	 */
	private boolean checkRelocationValid () {
		// The victim cannot have any neighbors.
		Vector<MuttonCell> tempVector = new Vector<MuttonCell>();
		getAliveNeighbors(mostRecentVictim, tempVector);
		if (tempVector.size() != 0) {
			return false;
		}

		// For the farmer, we need to verify that all of the suspects
		// are next to at least one live animal.
		if (board_state == MuttonState.FARMER_MOVING_SHEEP_STATE) {
			for (int i=0; i < mostRecentSuspects.size(); i++) {
				MuttonCell suspect = mostRecentSuspects.elementAt(i);
				getAliveNeighbors(suspect, tempVector);
				if (tempVector.size() == 0) {
					return false;
				}
			}
		}

		return true;
	}

	/*
	 * Advance the game state after relocation of animals is complete.
	 */
	private void doPostRelocateAction () {
		if (relocationValid) {
			if (board_state == MuttonState.WOLF_MOVING_SHEEP_STATE) {
				setFarmerTurn();
				setState(MuttonState.FARMER_CHOOSING_TARGETS_STATE);
			} else if (board_state == MuttonState.FARMER_MOVING_SHEEP_STATE) {
				getReadyForWolfToEat();
			} else if (board_state == MuttonState.FARMER_CONFIGURING_BOARD) {
				setWolfTurn();
				setState(MuttonState.WOLF_HIDING_STATE);
			}
			moveNumber += 1;
		}
	}

	/*
	 * Have the farmer shoot a number of animals.
	 * @param victims   A list of victim Id's.
	 * return if the shooting was made or not.
	 */
	private void doFarmerShoot (int [] victims) {
		if (board_state == MuttonState.FARMER_CHOOSING_TARGETS_STATE) {
			// Remove the highlighting from the most recent victim & suspects
			// (These were the animals eaten/moved by the wolf player on his last turn.
			mostRecentVictim.setHighlightCode(PRE_ANIMAL_HIGHLIGHT, HIGHLIGHT_NONE);
			setHighlightCode(mostRecentSuspects, POST_ANIMAL_HIGHLIGHT, HIGHLIGHT_NONE);
			mostRecentSuspects.setSize(0);
			relocationValid = false;

			// Go through the list of shooting victims and kill each one.
			// Note that mostRecentVictim is updated for each animal killed.
			// If the farmer only shoots one, then this is set to that victim
			// so that the highlighting can be removed when the wolf eats his
			// victim.
			// If the farmer goes on a rampage and shoot multiple targets,
			// then mostRecentVictim only keeps the last one.  However, in this
			// case the game will be over and we don't care about not keeping
			// track of all of the victims.
			for (int i = 0; i < victims.length; i++) {
				mostRecentVictim = findCellWithSheep(victims[i]);
				int victimId = mostRecentVictim.getSheepId();
				if (mostRecentVictim.isAlive() && (victimId >= 0)) {
					boolean wasWolf = mostRecentVictim.isWolf();

					// Kill the animal in the cell
					mostRecentVictim.setAlive(false);
					sheepStatus[mostRecentVictim.getSheepId()] = wasWolf ?
					                            SHEEP_STATUS_DEAD_WOLF :
					                            SHEEP_STATUS_DEAD_SHEEP;

					// Mark the victim so that they are drawn correctly in the GUI
					mostRecentVictim.setHighlightCode(PRE_ANIMAL_HIGHLIGHT, HIGHLIGHT_AS_LAST_VICTIM);

					// If a wolf has been shot, then reduce the count of live wolves.
					if (wasWolf) {
						wolfCountOnBoard -= 1;
					}
				}
			}

			// Remove the victims from the targeted list
			targetedSheep.setSize(0);

			// Check to see if the game is over.
			checkForWin();

			// If the farmer shot more than one animal and didn't win, then
			// that means that he loses.
			if ((victims.length > 1) && !win[farmerId]) {
				win[1-farmerId] = true;
			}

			// If the game is over, then move to the GAMEOVER_STATE.
			// If the game is not over, then either get ready for the wolf to
			// eat or for the farmer to move the scared sheep.
			if (win[0] || win[1]) {
				setState(MuttonState.GAMEOVER_STATE);
			} else {
				// If the shotgun is enabled, then find the scared sheep that
				// will need to be relocated by the farmer. (otherwise, the
				// mostRecenetSuspects list will remain empty.)
				if (shotgunEnabled) {
					getAliveNeighbors(mostRecentVictim, mostRecentSuspects);
				}

				// If there are scared sheep to move, then the farmer needs
				// to relocate them, otherwise the wolf is ready to eat again.
				if (mostRecentSuspects.size() != 0) {
					// Mark the suspects so that they are drawn correctly in the GUI
					setHighlightCode(mostRecentSuspects, POST_ANIMAL_HIGHLIGHT, HIGHLIGHT_AS_SCARED);

					// Move on to the farmer moving sheep state.
					setState(MuttonState.FARMER_MOVING_SHEEP_STATE);
				} else {
					getReadyForWolfToEat();
					moveNumber += 1;
				}
			}
		}
	}

	/*
	 * Given a vector of cells on the board, create and return an array of
	 * ints that are the sheep Id's in the cells.
	 */
	public int [] createListOf (Vector<MuttonCell> cells) {
		// Create the array.
		int size = cells.size();
		int [] list = new int [size];

		// Copy the Id's
		for (int i=0; i<size; i++) {
			MuttonCell c = cells.elementAt(i);
			list[i] = c.getSheepId();
		}

		// Return the list.
		return list;
	}

	/**
	 * Execute a move for Mutton.
	 */
	public boolean Execute (commonMove mm,replayMode replay) {
		MuttonMoveSpec m = (MuttonMoveSpec) mm;

		//G.print("E "+m+" for "+whoseTurn+" "+board_state);
		switch (m.op) {
			case MOVE_EDIT:
				board_state=MuttonState.PUZZLE_STATE;
				break;
			case MOVE_BOARD_STATE :
				if (board_state == MuttonState.FARMER_CONFIGURING_BOARD) {
					setBoardLayout(m.boardStateString);

					// Forget that any sheep are moving.
					movingSheepId = -1;
					movingSheepSourceCell = null;
				}
				break;

			case MOVE_SWAP :
				if ((board_state == MuttonState.WOLF_HIDING_STATE) &&
				    (farmerId == 0)) {
					farmerId = 1;
					moveNumber += 1;
					setWolfTurn();
				}
				break;

			case MOVE_FINISHED_HIDING :
				hideWolves(m.sheepIds);
				getReadyForWolfToEat();
				break;

			case MOVE_PICKUP :
				startMovingSheep(findCellWithSheep(m.sheepIds[0]));
				break;

			case MOVE_DROP :
				returnMovingSheep();
				break;

			case MOVE_EAT :
				if (m.sheepIds.length == 0) {
					doWolfMustPassEat();
				} else {
					doWolfEat(m.sheepIds[0]);
				}
				break;

			case MOVE_RELOCATE :
				doRelocateAnimal(m.sheepIds[0], m.destination);
				break;

			case MOVE_DONE_RELOCATING :
				doPostRelocateAction();
				break;

			case MOVE_SHOOT :
				doFarmerShoot(m.sheepIds);
				break;

			case MOVE_START:
				setWhoseTurn(m.player);
				break;

			case MOVE_RESIGN:
				setState(unresign==null?MuttonState.RESIGN_STATE:unresign);
				break;
			case MOVE_DONE:
				// used only in resign state
				G.Assert(board_state==MuttonState.RESIGN_STATE,"resign state expected, but is "+board_state);
				win[nextPlayer[whoseTurn]] = true;
				setState(MuttonState.GAMEOVER_STATE);
				break;
	        case MOVE_GAMEOVERONTIME:
	        	win[whoseTurn] = true;
	        	setState(MuttonState.GAMEOVER_STATE);
		     	break;
			default:
				cantExecute(m);
		}

		//System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);

		return (true);
	}

	/**
	 * Return a string that indicates the current board layout after moving
	 * the given sheep to the given cell.
	 *
	 * @param lmovingSheepId   The sheep that will move.
	 * @param tgtCell         The cell that the sheep is moving to.
	 * @return the string that represents the new board state after moving.
	 */
	public String getNewBoardLayoutString (int lmovingSheepId, MuttonCell tgtCell) {
		// Only move the animal if it's not already on the tgtCell.
		if (tgtCell.getSheepId() != lmovingSheepId) {
			// We want to keep the rotation of the target cell rather than the
			// rotation of the source cell.  Normally, during the game, when moving
			// an animal, the rotation setting moves with the animal.  This keeps all
			// animals facing in a constant direction during the game, reducing
			// potential confusion of players.  However, during board setup by the
			// farmer, as animals are moved around, the command that is transmitted
			// among the clients is the state of which animals are on which spaces
			// of the board, and it does *not* include rotation info.  Instead, each
			// space on the board is assigned a random rotation (during game board
			// creation) that the initial animal on that space inherits.  Therefore,
			// if we didn't keep the rotation of the target cell here, then the farmer
			// player would have the rotation from the source cell applied to the
			// animal (since tgtCell.moveAnimalFrom moves the rotation), while all
			// other clients would have the rotation from the target cell applied
			// (since they would just unpack the state string that only has locations).
			// In order to keep all clients in sync with the rotation of the animals,
			// we save the old rotation value in the target cell prior to moving,
			// and then restore it after the move.  This ensures that all clients
			// think that all animals have the same rotations at all times.
			int tgtCellRotation = tgtCell.getDisplayRotation();
			tgtCell.moveAnimalFrom(findCellWithSheep(lmovingSheepId));
			tgtCell.setDisplayRotation(tgtCellRotation);
		}
		return getBoardLayoutString();
	}

	/**
	 * Return a string that indicates the board layout after changing the wolf
	 * player's target by the given amount.
	 *
	 * @param wolfWinDelta   the amount to change the wolf's win target.
	 * @return the string that represents the new board state.
	 */
	public String getNewBoardLayoutString (int wolfWinDelta) {
		wolfWinTarget += wolfWinDelta;
		return getBoardLayoutString();
	}

	/*
	 * Return a string that indicates the current board layout.
	 * The board layout string's format is:
	 *   - Will be 38 characters in length.
	 *   - The first character indicates the number of sheep deaths needed
	 *     for the wolf player to win.  A = 1, B = 2, C = 3, etc...
	 *   - The next 37 characters are the 37 spaces on the board (in col/row
	 *     order)  Each character indicates which animal is on the space (A, B,
	 *     C, ...) or is the character @ to indicate an empty space.
	 */
	public String getBoardLayoutString () {
		char [] chars = new char [38];
		chars [0] = (char) ('@' + wolfWinTarget);

		int index = 1;
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				if (board[c][r].isOnBoard()) {
					chars [index] = (char) ('A' + board[c][r].getSheepId());
					index += 1;
				}
			}
		}
		// note the String.valueOf(chars) form doesn't work in codename1
		String str = new String(chars);
		return str;
	}

	/*
	 * Given a string representation of the board layout, put the board into
	 * that state.  The board layout string's format is:
	 *   - Must be 38 characters in length.
	 *   - The first character indicates the number of sheep deaths needed
	 *     for the wolf player to win.  A = 1, B = 2, C = 3, etc...
	 *   - The next 37 characters are the 37 spaces on the board (in col/row
	 *     order)  Each character indicates which animal is on the space (A, B,
	 *     C, ...) or is the character @ to indicate an empty space.
	 */
	private void setBoardLayout (String stateString) {
		char [] chars = stateString.toCharArray();

		// The string must have 38 characters to be valid.
		G.Assert(chars.length == 38,"setBoardLayout string length is %s",chars.length);

		// Ensure that each of the 26 unique sheep exists in the string
		// exactly once.  (The animalSeen array is sized 27, because an empty
		// space is sent as animal -1.)
		int [] animalCount = new int [27];
		for (int i = 0; i < animalCount.length; i++) {
			animalCount[i] = 0;
		}
		// We start at character 1 to skip over the winTarget number.
		for (int i = 1; i < chars.length; i++) {
			animalCount[chars[i] - '@'] += 1;
		}
		// We start the check at animalCount 1 to to skip over the count of
		// empty spaces (which will be larger than 1.)
		for (int i = 1; i < animalCount.length; i++) {
			if (animalCount[i] != 1) {
				System.out.println("Error: setBoardLayout animal " + i + " count = " + animalCount[i]);
				System.out.println("       stateString = " + stateString);
				System.out.println("       animalCount = " + animalCount.length);
				return;
			}
		}

		// Pull out the wolf win target value.
		wolfWinTarget = chars[0] - '@';

		// Set all of the sheep on the board in their designated locations.
		int index = 1;
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				if (board[c][r].isOnBoard()) {
					// Put the next animal from the string on the board.
					int sheepId = chars[index] - 'A';
					board[c][r].setSheepId(sheepId);
					board[c][r].setAlive(sheepId >= 0);
					index += 1;
				}
			}
		}

		// The change of positions of the sheep is valid.
		relocationValid = true;
	}


	/**
	 * This method is called when a draw by repetition is detected by other
	 * parts of the game infrastructure.  The game should enter a "draw
	 * pending" state and await confirmation from the user (by clicking on
	 * done) that a draw has happened.
	 * If this mechanism is triggered unexpectedly, it is probably because move
	 * editing in xxxGameViewer.editHistory() is not correctly removing
	 * tentative moves by the user, or the Digest() method is not returning
	 * unique results.
	 * The Viewer also ought to have a "repRect" and call DrawRepRect to warn
	 * the user that repetitions have been seen.
	 *
	 * Mutton cannot have repetitions of moves (since at least one sheep is
	 * killed each turn), and therefore doesn't need this method.
	 */
	public void SetDrawState() { throw G.Error("SetDrawState() not expected"); }

	
/**************************************************************************
  The following methods are used by the robot players.
*/

	/**
	 * Return a vector that includes all of the animals that are currently
	 * valid meals for the wolves.
	 * @param areWolves   If true, then the list only includes animals that
	 *                    are wolves.
	 *                    If false, then the list only includes animals that
	 *                    are sheep.
	 * @return a vector of cells on the board that are meals and are the
	 *         type of animals requested.
	 */
	public Vector<MuttonCell> getValidMeals (boolean areWolves) {
		Vector<MuttonCell> v = new Vector<MuttonCell>();
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				MuttonCell cell = board[c][r];
				if (cell.isValidMeal() && (cell.isWolf() == areWolves)) {
					v.addElement(cell);
				}
			}
		}

		return v;
	}

	/**
	 * @return one of the suspects that is to be moved.
	 * First, check around the most recent victim.  If there is an alive
	 * animal there, then return one of them.  If there are no suspects left
	 * next to the most recent victim, then check to see if any of the
	 * suspects are left with no alive neighbors; these suspects will have to
	 * move.  (They could have moved into that position by moving to an empty
	 * space that was only next to other suspects and those other suspects
	 * subsequently moved away themselves, leaving the first one with no alive
	 * neighbors.)
	 * If none of these exist, then nobody need to move and null is returned.
	 */
	public MuttonCell getASuspectToMove () {
		// Check around the most recent victim.
		Vector<MuttonCell> neighborList = neighbors[mostRecentVictim.getCol()][mostRecentVictim.getRow()];
		for (int i = 0; i < neighborList.size(); i++) {
			MuttonCell cell = neighborList.elementAt(i);
			if (!cell.isEmpty() && cell.isAlive()) {
				return cell;
			}
		}

		// Check all suspects to see if there is one that is lonely.
		Vector<MuttonCell> aliveNeighbors = new Vector<MuttonCell>();
		for (int i = 0; i < mostRecentSuspects.size(); i++) {
			MuttonCell cell = mostRecentSuspects.elementAt(i);
			getAliveNeighbors(cell, aliveNeighbors);
			if (aliveNeighbors.size() == 0) {
				return cell;
			}
		}

		return null;
	}

	/**
	 * Return a vector of all suspects that still need to move.
	 */
	public Vector<MuttonCell> getSuspectsToMove () {
		Vector<MuttonCell> suspects = new Vector<MuttonCell> ();

		// Check around the most recent victim.
		Vector<MuttonCell> neighborList = neighbors[mostRecentVictim.getCol()][mostRecentVictim.getRow()];
		for (int i = 0; i < neighborList.size(); i++) {
			MuttonCell cell = neighborList.elementAt(i);
			if (!cell.isEmpty() && cell.isAlive()) {
				suspects.addElement(cell);
			}
		}

		if (suspects.size() == 0) {
			// Check all suspects to see if there is one that is lonely.
			Vector<MuttonCell> aliveNeighbors = new Vector<MuttonCell>();
			for (int i = 0; i < mostRecentSuspects.size(); i++) {
				MuttonCell cell = mostRecentSuspects.elementAt(i);
				getAliveNeighbors(cell, aliveNeighbors);
				if (aliveNeighbors.size() == 0) {
					suspects.addElement(cell);
				}
			}
		}

		return suspects;
	}

	/**
	 * @return a list of all spaces on the board that are empty and are not
	 * next to the mostRecentVictim.
	 */
	public Vector<MuttonCell> getValidEmptyCells () {
		Vector<MuttonCell> emptyCells = new Vector<MuttonCell>();

		// Add all empty cells that are next to a living animal
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				MuttonCell cell = board[c][r];
				if (cell.isOnBoard() && cell.isEmpty() && hasLivingNeighbor(cell)) {
					emptyCells.addElement(cell);
				}
			}
		}

		// Remove the cells next to the mostRecentVictim
		Vector<MuttonCell> neighborList = neighbors[mostRecentVictim.getCol()][mostRecentVictim.getRow()];
		for (int i = 0; i < neighborList.size(); i++) {
			emptyCells.removeElement(neighborList.elementAt(i));
		}

		return emptyCells;
	}

	/*
	 * Determine if the given cell has a living neighbor
	 */
	public boolean hasLivingNeighbor (MuttonCell c) {
		Vector<MuttonCell> neighborList = neighbors[c.getCol()][c.getRow()];
		for (int i=0; i < neighborList.size(); i++) {
			MuttonCell neighbor = neighborList.elementAt(i);
			if (!neighbor.isEmpty() && neighbor.isAlive()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if the given animal has a living neighbor or not. 
	 */
	public boolean hasLivingNeighbor (int animalId) {
		return hasLivingNeighbor(findCellWithSheep(animalId));
	}

	/**
	 * Return the value of the wolfHasPassed flag.
	 * If a robot player is the farmer, and the wolf has passed, then
	 * the farmer must shoot something.
	 */
	public boolean getWolfHasPassed () {
		return wolfHasPassed;
	}

	/**
	 * Return the vector of the most recent suspects.
	 * @return the vector of the most recent suspects.
	 */
	public Vector<MuttonCell> getMostRecentSuspects () {
		return mostRecentSuspects;
	}

	/**
	 * Return the MuttonHistory vector
	 */
	public Vector<MuttonHistoryElement> getHistory () {
		return eatenSheepHistory;
	}

	/**
	 * Return the current status of all sheep.
	 */
	public int [] getSheepStatus () {
		return sheepStatus;
	}

	/**
	 * @return a vector of all animals on the board who don't have any live neighbors.
	 */
	public Vector<MuttonCell> getLonelySheep () {
		Vector<MuttonCell> v = new Vector<MuttonCell>();

		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				MuttonCell cell = board[c][r];
				if (!cell.isEmpty() && cell.isAlive() && cell.isOnBoard() && !hasLivingNeighbor(cell)) {
					v.addElement(cell);
				}
			}
		}

		return v;
	}

	/**
	 * Fill in the given array with the id's of the living wolves.
	 * This used to be used by the V2 robot player when playing as the wolf.
	 * The V2 player no longer uses it, but I'm leaving the code here in case
	 * a future robot player may want to use it.
	 */
	public int [] getLivingWolfIds () {
		int [] ids = new int [wolfCountOnBoard];

		int index = 0;
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				MuttonCell cell = board[c][r];
				if (cell.isAlive() && cell.isOnBoard() && cell.isWolf()) {
					ids[index] = cell.getSheepId();
					index += 1;
				}
			}
		}

		return ids;
	}

}