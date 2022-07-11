package online.game.export;

import com.codename1.ui.geom.Point;

import lib.ExtendedHashtable;
import lib.LFrameProtocol;
import lib.TimeControl;
import online.game.BoardProtocol;
import online.game.CommonMoveStack;
import online.game.commonPlayer;
import online.game.sgf.sgf_game;
import online.search.TreeViewerProtocol;

import static online.common.OnlineConstants.Bot;

/** Technically speaking, an add-on game for boardspace only needs
 * to implement ViewerProtocol.  Everything else is optional.  
 * <p>
 * Generally speaking, the game window will create some class that implements this protocol
 * and call doInit.  The game controller will collect the players and spectators and maintain
 * communications with the server and among the players.  There are just a few "incoming"
 * type events that the client class will need to handle, and only a few "outgoing" pieces of
 * information that the client will need to provide.
 * <p>
 * Generally speaking, the client class will generate a series of simple messages that describe
 * the events in the game.  All the clients in the game (both spectators and players)
 * work from this stream and maintain their state from it.
 * <p>
 * Disconnections, reconnection, new spectators, and resumptions of suspended games
 * are handled by requiring the clients to produce a "replay" string, and to be
 * able to parse and interpret similar strings.  Generally, these strings are in a format
 * very similar to the routine messages in the game, so essentially new clients are synchronized 
 * by feeding them the game history.
 * <p>
 * Another important point is that error logging is automatic, so information about problems 
 * with the client will be available in the debugging environment and on the server.
 * 
 * @author ddyer
 *
 */
public interface ViewerProtocol extends ViewGameProtocol
{  
	 /** initialize from scratch.  H contains a lot of shared state available to the viewer, including
		the specs for the game being created and environment variables such as "s", the language translations,
		and "root" the applet root, and "frame" the frame containing menus.
	  */
	public void init(ExtendedHashtable h, LFrameProtocol frame);
    /** initialize back to the "start" position.  This is used when a reviewer
        is about to load a new game, or when the "WayBack" button is pressed.
        In particular, this implies that the players list (and maybe also the game subtype)
        may change.  As invoked by the game controller, "preserve_history" will always
        be false, indicating that history should be discarded.
     */
   public void doInit(boolean preserve_history);
    /** the number of moves at which midgame is reached, this is just a guess, not a 
     * contract.  When this point in the game is reached, a midpoint digest is extracted
     * for later use in fraud detection.  midGamePoint() only relevant the first time
     * MoveStep() is greater, so a variety of strategies for determining and declaring
     * midpoint could be used. 
     * */
    public int midGamePoint();
    /** moves made in this game, used to determine if we should discard a game just launched */
    public boolean discardable();
    
    /** this is an entertainment string passed to the lobby.  The first element is the current
    move number, the rest are an ordered list of short summary strings, one for each player. 
    There are a couple of undocumented special formats that help the lobby display things
    prettily 
    */ 
    public String gameProgressString();

    /** return an integer representing a hashed digest (Zorbrist hash) of the state of the game.  This is 
    used to detect duplicate game fraud, to detect game end by repetition, and to detect internal
    errors where game state "should" have been preserved 
    */
    public long Digest();

    /**
     * if true, the game should keep a fixed baseline of moves.  This should
     * be true when simultaneous moves will actually occur.  It is used to
     * inhibit triggering robot moves, and incremental updates to the shared
     * game state.
     * 
     * @return a boolean
     */
    public boolean fixed_move_baseline();
  
    /** set up a robot for this player as p.robotPlayer.  Don't actually make any moves yet. 
     * this is where you instantiate the class that will implement RobotProtocol for the game.
     * @return  the player controlling the robot
     */
    public commonPlayer startRobot(commonPlayer p,commonPlayer runner,Bot robo);
    

    /** return a string naming the "zone" of the window, whatever that means, and the normalized
     * x,y coordinates of that zone in p.  For the simplest layouts with no variability, this can
     * be just an x/y based on the board grid and "on" as the zone.  
     * <p>The most common extension
     * to the basic x,y is if the board has a reverse view, so the coordinate system has to be 
     * flipped when the mouse is over the board, presumably to represent the part of the board
     * the mouse is pointing to.
     * <p>
     * Yet more complex "zones" might include a censored area, as for plateau, or if the board has
     * major alternative layouts.
     * <p>
     * The main thing is that whatever getBoardZone returns, it only has to be sensible to {@link online.game.commonCanvas#decodeScreenZone}
     * 
     * @param x the raw x coordinate
     * @param y the ray y coordinate
     * @param p a point, which is modified to be the encoded x,y
     * @return the zone name
     */
    public String encodeScreenZone(int x,int y,Point p);
    
    
      /** return true if the game is over.  If false, ongoing activity
       * such as updating player clocks and running robot moves can happen.
       * If true, endgame activity such as recording the score will occur once.
       *  */
    public boolean GameOver();
     
    
    /** return true if player p has won.  This is used in 2 player games to transmit the winner
     * to the score keeper if the game is a draw or inconclusive, all players will return false.
     * It's an error for multiple players to return true 
     * 
     */
    public boolean WinForPlayer(commonPlayer p);
    /** return the final score for this player in multi-player games.
     * If the game is win/loss, return a higher value for the winning player,
     * otherwise, if the game has a score for each player return the score.
     */
    public int ScoreForPlayer(commonPlayer p);
    public int ScoreForAutoma();
    public boolean UsingAutoma();
    
    /**
     * retrieves an auxiliary window being used to study a robot search in progress
     * 
     * @return a tree viewer, or null if none exists
     */
    public TreeViewerProtocol getTreeViewer();
    /**
     * get the shared info which was used to initialize this window.  This is 
     * used when initializing robots associated with this window.
     * @return the hashtable full of shared data
     */
    public ExtendedHashtable getSharedInfo(); 
    
    /**
     * return true if the game is currently in "all play" mode.  This is used internally
     * for things such as marking the "current" player, and in communication with robots.    
     */
    public boolean simultaneous_turns_allowed();
    
    /**
     * this is called when the window will be closing, and should trigger 
     * cleanup and shutdown of all subsidiary windows and processes.
     */
    public void shutdownWindows();
    /**
     * this is used by robots to add completed games to the list available for review
     * @param game
     * @return the game added
     */
    public sgf_game addGame(sgf_game game);
    /**
     * this is used by robots to add completed games to the list available for review
     * @param moves
     * @param name
     * @return the game added
     */
    public sgf_game addGame(CommonMoveStack moves,String name);
	public void setHasGameFocus(boolean on);
	public BoardProtocol getBoard();
	public String mouseMessage();
	public void testSwitch();
	public boolean mouseDownEvent(boolean b);
	public TimeControl timeControl();
	public int timeRemaining(int pl);
	/**
	 * return true if robots should be allowed to run.  Generally the
	 * result should be true, but if simultaneous actions are allowed
	 * there can be complications.  This can be altered to return true
	 * if its' good enough that the robot will run synchronously when
	 * it is its turn.  Otherwise some special code has to start the
	 * robot when it's not its turn.
	 * @return
	 */
	public boolean allowRobotsToRun();
	public String fixedServerRecordString(String string, boolean includePlayerNames);
	public String fixedServerRecordMessage(String fixedHist);

}