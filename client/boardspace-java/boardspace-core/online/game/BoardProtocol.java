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
package online.game;

import java.awt.Point;

import lib.Digestable;
import online.game.BaseBoard.BoardState;

/**
 * this is the visible contract between "board" objects and the general
 * parts of the game system.  Anything not in this protocol is really
 * a private matter between your board class and your viewer and robot
 * classes.
 * @author ddyer
 *
 */
public interface BoardProtocol extends Digestable
{	// this an abstract class to allow all viewers to share an alternate
	/**
	 * return true if the board should allow a "done" action. 
	 * This is used internally as a matter of convention, but externally
	 * it's used to trigger "tick tick" sounds to remind the user he's still
	 * expected to act. 
	 */
	public boolean DoneState();		//true if the board is waiting for confirmation of a complete move
	/**
	 * execute a move.  This should be the only way the internals of
	 * board objects are modified by the viewer class.   Boards may do
	 * reasonable consistency checking, and should call {@link lib.G#Error} if something
	 * is wrong with the move.   By convention, the robot should only attempt
	 * legal moves, and the viewer should only allow legal moves to be made.
	 * 
	 * @param m a {@link commonMove}
	 * @return true if the move was successfully executed.
	 */
	public boolean Execute(commonMove m,replayMode mode);		//execute a move
	/**
	 * return a 32 Bit digest of the board state.  Any significant board 
	 * state change should result in a different digest.
	 * <p>A cloned board should result in the same digest.  Reinitializing
	 * and replaying a series of moves should result in the same digest.
	 * A duplicate endgame position should result in the same digest.  When a robot
	 * makes a trial move, and unmakes it, the same digest should result, and so on.
	 * <p>
	 * Digests are also used in real time to detect repeated positions, to detect "follower fraud" and over long periods
	 * of time to detect deliberately replaying the same game to inflate rankings.
	 * @return an integer
	 */
	public long Digest();		// digest the board to an integer for fraud and repetition detection
	/** 
	 * called when a three-times-repeated position is detected.  Depending on the
	 * game, this may indicate a draw, or it may indicate a bug in the bookkeeping.
	 * <p>Unexpected calls to this method frequently indicate that a viewer's {@link online.game.commonCanvas#EditHistory}
	 * has not edited out repeated do-undo sequences by an indecisive player.
	 */
	public void SetDrawState();	// set the board into the "it's a tie game" state
	/**
	 * return true if the game is at an end-of-game state.  Normally, this is 
	 * implemented as game_state==GAMEOVER_STATE.  This is used to trigger
	 * a phase transition from honest play to after-game review.
	 * @return true if the game is at an end
	 */
	public boolean GameOver();	// gameover has been seen at some time in the real game sequence
	/**
	 * return true if the player identified by idx has won the game. This is used
	 * when scoring 2 player games.  It's an error for there to be more
	 * than one winner, but it's ok if there is no winner.
	 * @param idx
	 * @return true if the player has won
	 */
	public boolean WinForPlayer(int idx);
	/**
	 * return the current move number.  Changes in move numbers are used to 
	 * trigger new columns in the visible game log.
	 * @return the current move number
	 */
	public int moveNumber();
	/**
	 * return the index of the current player.  This index doesn't necessarily
	 * correspond to the position of commonPlayer objects or the seat position.
	 * It's purely an internal board variable that the board will change from 
	 * time to time.
	 * @return an integer representing the current player
	 */	
	public int whoseTurn();
	/**
	 * compare two boards and throw an error if they are not the same.  This is 
	 * used to verify copies of the board for the robot, and also to verify that
	 * replaying the current move history results in the same board position.
	 */
	public void sameboard(BoardProtocol other);

	/**
	 * this is used when initializing games to set the starting player or the next
	 * player.  It's sometimes convenient to breakpoint this method.
	 * @param pn
	 */
	public void setWhoseTurn(int pn);
	/**
	 * return true if the board's internal state should be digested
	 * and saved to detect repetitions.
	 * @return true if this board state should be digested
	 */
	public boolean DigestState();	// if this state should be digested.  Normally the same as DoneState()

	/**
	 * encode the position of some point on the board in a board-specific
	 * way.  Typically this becomes the closest cell.  The purpose of this
	 * encoding is so the visible mouse position will hover near the same
	 * object, even if the viewer is using a different view of the board.
	 * @param x
	 * @param y
	 * @param cellsize
	 * @return a Point
	 * @see #decodeCellPosition
	 */
    public Point encodeCellPosition(int x,int y,double cellsize);
    /**
     * reverse the encoding done by encodeCellPosition
     * @param x
     * @param y
     * @param cellsize
     * @return a Point
     * @see #encodeCellPosition
     */
    public Point decodeCellPosition(int x,int y,double cellsize);
    /**
     * 
     * @return the approximate size, in pixels, of a board square.
     */
    public int cellSize();		// this is used only in rough calculations
    /**
     * get an index that encodes the object that has been picked up
     * and is tracking with the mouse.  This is use to tell the clients
     * which object to depict.  
     * @return an index representing the currently moving object.
     */
    public int movingObjectIndex();
    
    /**
     * create a copy of the current board.  This is used to initialize
     * the board used by the robot, and also to verify the integrity
     * and reproducibility of the game record.
     * @return a new board
     */
    public BoardProtocol cloneBoard();

    /**
     * make this board a copy of "from", which is another board of the current real type.
     * 
     * @param from
     */
    public void copyFrom(BoardProtocol from);

    /**
     * re-initialize the board back to the state before any moves
     * were made.  Any generic setup or state, such as the number
     * of players or the random seed for the game should be preserved.
     * 
     */
	public void doInit();

	/**
	 * @return the number of players in the current game
	 */
	public int nPlayers(); 
	
	/** allow illegal moves (permissive replay)
	 */
	public void setPermissiveReplay(boolean f);
	/** get the current color map used by the game
	 * 
	 * @return
	 */
	public int[] getColorMap();
	/**
	 * set the color map for the game, or null to initialize it to the appropriate default
	 * @param map
	 * @param players
	 */
	public void setColorMap(int[]map, int players);
	/**
	 * this supplies the maximum game revision for distribution to the other clients.
	 * @return
	 */
	public int getMaxRevisionLevel();
	public int getActiveRevisionLevel();
	/**
	 * this supplies revision information from another client.
	 * @param n
	 */
	public void setClientRevisionLevel(int n);
	/**
	 * call this before starting a game, after all the clients have registered.
	 * return true if the revision changed
	 */
	public boolean checkClientRevision();
	/**
	 * forget any previous game revision information.  Use this when starting replay of a new game.
	 */
	public void resetClientRevision();

	/** for debugging - copies of board are given informative names which indicate
	 * who is using it.
	 * @return
	 */
	public String getName();
	public void setName(String s);
	public BoardState getState();
	public boolean canResign();
	
	/**
	 * true if the board has a reverse view
	 */
	public boolean reverseView();
	}
