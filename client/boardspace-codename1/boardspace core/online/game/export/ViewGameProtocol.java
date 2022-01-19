package online.game.export;

import bridge.Color;

import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.Vector;

import lib.HitPoint;
import lib.SimpleObserver;
import online.common.LaunchUser;
import online.common.OnlineConstants;
import online.game.commonMove;
import online.game.commonPlayer;

/**
 * these protocol items are normally of concern only to the Game class. 
 * these are low level services provided to viewers in conjunction with
 * the game.  These methods may be wrapped or overridden by the viewer
 * class, but are not expected or required to be.
 * @author ddyer
 *
 */
public interface ViewGameProtocol 
{
	 /** 
	  *  add a new player to the list of commonPlayers, replace the one specified, which
	  * may be null, meaning assign the next empty slot.  If both are null
	  * clear the player list.  This is used by the Game to keep the Viewer informed
	  * of the identities of the players. 
	 * @param p  the player to add, or null to remove
	 * @param replace the player to replace, or null to assign a new slot.
	  */
	public void changePlayerList(commonPlayer p,commonPlayer replace);
    /**
     * return the number of animation steps that are still pending.  This is used to
     * determine that the viewer is idle and finished all the end of game displays
     */
	public boolean spritesIdle();
	/**
	  *  add a new spectator to the list of commonPlayers, replace the one specified, which
	  * may be null, meaning assign the next empty slot.  If both are null
	  * clear the player list.  This is used by the Game to keep the Viewer informed
	  * of the identities of the Spectators.  In review rooms, there are only spectators.
	 * @param p  the player to add, or null to remove
	 * @param replace the player to replace, or null to assign a new slot.
	 */
	public void changeSpectatorList(commonPlayer p,commonPlayer replace);

/** 
 * Record the latest mouse tracking information about the player (or spectator).  
 * only remembering, no other action, should occur.  By convention, record nothing
 * if player happens to be self.  Also by convention, if inx is >=0 it reflects
 * an object begin moved by the player, and all the other players should be set to
 * not be moving.  By convention, there is at most one moving object in the game
 * at any time.
 * @param player  the player, or spectator.
 * @param zone default "on" "board" or "off", or whatever means something to your viewer
 * @param inx encoded x coordinate
 * @param iny encoded y coordinate
 * @param ino if >=0 represents an object being moved.
 */
    public void doMouseTracking(commonPlayer player,String zone,int inx,int iny,int ino);
	/** viewers are created in a not-yet-visible state, this method tells them
	 * to become visible.  This allows messy startup phases to be invisible.
	 */
	public void setVisible(boolean val); 
    /** return any interesting information for debugging and logging.  This is used
     * to get interesting information into the logs to help debug difficult problems. */
    public String errorContext();
    /**
     * return true if there are no robots actively running
     * @return true is no robots are running
     */
    public boolean allRobotsIdle();
    /**
     * update the player time, normally for the current player only 
     */
    public void updatePlayerTime(long currentT,commonPlayer whoseTurn);
  
    /** stop all robots.  This is done by traversing the list of players.
     * 
     */
    public void stopRobots();
    /** if there is a robot player for this player, start it running to make the next move. */
    public void startRobotTurn(commonPlayer p);
    /** set the viewer into "limbo" state, where no moves can be made.  This is probably due to
    an involuntary interruption of the game, such as a loss of communications */
    public void setLimbo(boolean v);
    /** set a message which will be seen by the user.  These are normally related to game events
    such as "waiting for other players". This should override routine game advice which might
    share the same space. 
    This message is eventually made visible in the GUI by standardGameMessage
    */
    public void setUserMessage(Color c,String m);
    /** parse and execute a message which originated in a getEvents string.
    Return true if the message was successfully parsed in the game main sequence,
    false if some error or if a review variation 
     * @param st  a move string to be parsed
     * @param player the player to parse for, ie the current player
     */
    public boolean ParseMessage(String st, int player);
    
    /** parse a text move specification
     * 
     * @param st a string, the spec for a move in this game
     * @param player board index of the player
     * @return a commonMove resulting from parsing the string
     */
    public commonMove ParseNewMove(String st, int player);

    /** same as ParseMessage, but for strings that are the echo of commands from us
     * 
     * @param str
     * @return true if all ok
     */
    public boolean ParseEcho(String str);
    
    /** get a vector of strings, which are events that should be sent to spectators and other
        players.  These events will be presented to other viewers of the same class to be interpreted
        and executed. These "events" are normally moves or gestures, but not individual mouse
        positions (mouse tracking sprites are handled automatically) */
    public Vector<String> getEvents();
    /** this is used to insert moves by robots into the game "as though" they resulted
     * from a user interface action
     */
    public boolean PerformAndTransmit(String st);
    
    /** first chance to process a message. Normally just ignore.  This is a way for 
    * a game to intercept messages sent outside of the normal mechanism
    * */
   public boolean processMessage(String cmd,StringTokenizer localST,String st);
   /** declare now it is ok to edit the game record.  During normal play, editing
    * the game record is not allowed. After game end, players are free to go back
    * and play variations.  This method will be called after the game is over and
    * scoring activity has completed. */ 
   public void setEditable();	
   /** 
    * set who has control to make editing moves.  This is handled by the game controler.
    * There is no UI, but the viewer should respect hasControlToken()
    */
   public void setControlToken(boolean val,long timestamp);
   /** say if we think we have the control token
    */
   public boolean hasControlTokenOrPending();
   /** called when setup is complete and we're ready to start.  At this point
    * the user interface becomes live and moves may arrive from other players. 
    * */
   public void startPlaying();
   
   /** true if we have the server-defined control token, so might benefit by
    * giving it up.
    * @return a boolean
    */
   public boolean hasExplicitControlToken();
   
   /** print debugging info for the error log.  This service is normally completely 
    * covered by the commonCanvas class
    */
   public void printDebugInfo(PrintStream s);
	/** Load a file, presumably full of game records in SGF format.  This service is
	 * normally completely covered by the commonCanvas class.  This request is 
	 * intended to be services asynchronously, so call isUrlLoaded() to determine
	 * when the operation is complete.
	 */
   public void doLoadUrl(String name,String gamename);
   /**
    * return true (once only) when a previous doLoadUrl has completed
    * this is expected to trigger sending the "url changed" notice 
    * to other players participating in the review
    * @return
    */
   public boolean isUrlLoaded();
   
   public void doSaveUrl(String name);
	
   /** Select and set up to replay a game which has been loaded. 
	 * This service is normally completely covered by the commonCanvas class, which records
	 * the activity for later action by the viewerRun loop
  */
   public boolean selectGame(String selected);
   /** scroll the game to a position, identified by getReviewPosition in another viewer 
	 * This service is normally completely covered by the commonCanvas class, which records
	 * the activity for later action by the viewerRun loop
   */
   public boolean doScrollTo(int val); 
   /** set the requested review position
	 * This service is normally completely covered by the commonCanvas class, which records
	 * the activity for later action by the viewerRun loop
    */
   public void setJointReviewStep(int v);
   /** get the current actual review position
 	 * This service is normally completely covered by the commonCanvas class, which records
	 * the activity for later action by the viewerRun loop
     */
   public int getReviewPosition();
   /** get the current requested review position. -2 indicates don't know 
	 * This service is normally completely covered by the commonCanvas class, which records
	 * the activity for later action by the viewerRun loop
   */
   public int getJointReviewStep();
   
   /** return the true move number of the game (ie: not the review mode move number). 
    *  This is used only for a few non-critacl things such as guessing if the game has 
    *  really started, or if it is still in setup phase, and to determine if the game
    *  has reached midpoint.
    *  */
   public int MoveStep();

   /** return the player whose turn it is.  This is used to switch the game state
   to reflect the current player.  Some actions (for example supplying the current
   state of the game to new spectators) are only performed by the current player. 
   This is also the trigger for "turn change" sounds. 
   */
   public commonPlayer whoseTurn();
   
   /** get the identity of the object being dragged by the mouse, or null.  This is
   an arbitrary integer which matters only to other instances of the same type of viewer. 
 * @param highlight
 * */
   public int getMovingObject(HitPoint highlight);
  
   /** required by the observer protocol 
	 * This service is normally completely covered by the commonCanvas class, which records
	 * the activity for later action by the viewerRun loop
    * */
   public void addObserver(SimpleObserver o);
   /** print the game record as a SGF file.  These are "public" rather than private
   formats, but mainly to be parsed by the reviewer version of the viewer. */
   public void printGameRecord(PrintStream s,String startingTime,String filename);
    


   /** playback the ephemeral data supplied by formEphemeralString.
    * @param mySt
    */
   public void useEphemeraBuffer(StringTokenizer mySt);
   
   /** interpret a history string as created by formHistoryString.  The contract is to bring
   the local game into the same state as the game that created the string.  This is used
   to give spectators the view of the game, and also to reconnect players that have
   been disconnected. */
   public void useStoryBuffer(String tok, StringTokenizer mySt);  
    
   /**
    * called when the game controller detects that the current player is changing
    * to your player.  If this methods returns true, the road runner "beep beep"
    * or "turnchange" sound is played.
    */
   public boolean playerChanging();
   
   public commonPlayer[] getPlayers();
   public String colorMapString();
   
   public OnlineConstants.Bot salvageRobot();		// robot to use when player quits

   /**
    *  add an additional player that will be using this GUI
    * @param u
    */
   public void addLocalPlayer(LaunchUser u);
   /**
    * handle "spare" messages that extend the game messages
    */
   public boolean processSpareMessage(String cmd,String fullmessage);

}
