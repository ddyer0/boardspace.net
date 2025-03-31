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

import bridge.*;
import common.GameInfo;

import jzlib.ZipEntry;
import jzlib.ZipInputStream;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;

import lib.*;
import lib.RepaintManager.RepaintStrategy;
import lib.SeatingChart.DefinedSeating;
import lib.SimpleSprite.Movement;
import lib.TimeControl.TimeId;
import online.common.LaunchUser;
import online.common.LaunchUserStack;
import online.common.OnlineConstants;
import online.common.Session;
import online.common.TurnBasedViewer;
import online.game.BaseBoard.BoardState;
import online.game.BaseBoard.StateRole;
import online.game.export.ViewerProtocol;
import online.game.sgf.sgf_game;
import online.game.sgf.sgf_gamestack;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.game.sgf.sgf_reader;
import online.game.sgf.export.sgf_names;
import online.search.TreeViewer;
import online.search.UCTTreeViewer;
import rpc.RpcInterface;
import rpc.RpcRemoteServer;
import rpc.RpcService;
import vnc.VNCService;
import vnc.VNCTransmitter;
import vnc.VncEventInterface;
import vnc.VncServiceProvider;
import vnc.VncRemote;
import online.search.AlphaTreeViewer;
import online.search.SimpleRobotProtocol;
/*
Change History

Oct 2004 Migrated some common stuff from zertz, to be used by all

Feb 2005 switched the PAINT and MOUSE functions to synchronize through
  the run thread.  This avoids all manner of random nastiness which could
  occur as complex data structures are manipulated by three independant
  processes.

July 2006 added repeatedPositions related functions

 TODO: when multiple viewers are reviewing, only the primary sees animations during playback
 
*/
/**
 * This class extends exCanvas and provides a bunch of services for the
 * individual game's Viewer class.   Many of the requirements of {@link ViewerProtocol}
 * are implemented here, and will not needs to be done by the game's subclass.
 * <br>
 * The main services are
 * <li>a history of moves in the game
 * <li>a "VCR cluster" of forward-back controls to view the game history.
 * <li>mouse handling
 * <li>repaint management
 * <li>some standard option menus
 * <li>debugging aids for screen layout and performance
 * <li>a run loop
 * @see exCanvas
 */
// TODO: add some visual distraction when the tick-tock sound plays and the sound is off
// TODO: add some additional cue when "living in the past"
// TODO: the concept of "peeking" and its interaction with "undo" needs more thought.  Need to be able to review without losing track of the fact that we peeked
// TODO: game records can acquire inconsistent times when editing with more than one player, which causes a flood of logged errors
//
public abstract class commonCanvas extends exCanvas 
	implements OnlineConstants,PlayConstants,ViewerProtocol,CanvasProtocol,sgf_names,ActionListener,Opcodes,PlacementProvider,
		VncEventInterface,GameLayoutClient
{ // state shared with parent frame
    // aux sliders
    public static final String LiftExplanation = "spread stacks for easy viewing";
    public  static final String AnimationSpeed = "Animation Speed";
    public static String DisabledForSpectators = "Disabled for Spectators";
    private  static final String CantResign = "You can only resign when it is your move";
    public  static final String ColorBlindOption = "Colorblind";
    public  static String NoeyeExplanation = "Stop showing available moves";
    public  static String EyeExplanation = "Show available moves";	
    public  static final String TileSizeMessage = "Tile Size";
    public  static final String SwapDescription = "Swap Colors with your opponent";
    private  static String NoResignMessage = "You cannot resign in a multiplayer game";
    public  static String ReverseView = "Reverse View";
    public  static String ReverseViewExplanation = "Rotate the board 180 degrees";
    public  static String YourTurnMessage = "Your Turn";
    public static String DoneAction = "Done";
    public static String EndTurnAction = "End Turn";
    public static String UndoAction = "Undo";
    public  static String ExplainDone = "Commit to the actions taken so far";
    public  static String ExplainPass = "take no action this turn";
    public  static String GoalExplanation = "GoalExplanation2";
    public  static String GoalExplanationOnly = "GoalExplanation";
    public  static String CensoredGameRecordString = "The game record is not available during the game";
    public  static String Reviewing = "reviewing";
    public  static String DrawOutcome = "The game is a draw";
    public  static String DrawNotAllowed = "You can't offer a draw at this time";
    public  static String WonOutcome = "Game won by #1";
    public  static String StackHeightMessage = "Stack Height";
    private  static String GoalExplanationOnlyDetail = "The goal of the game is:\n#1";
    public static final String diceSoundName = DICEPATH + "dice" + Config.SoundFormat;
    public static final String light_drop = SOUNDPATH + "drop-1" + Config.SoundFormat;
    public static final String heavy_drop = SOUNDPATH + "drop-3" + Config.SoundFormat;
    public static final String scrape = SOUNDPATH + "scrape" + Config.SoundFormat;
    public static final String swish = SOUNDPATH + "swish" + Config.SoundFormat;
    public static final String clockSound = SOUNDPATH + "ticktock" + Config.SoundFormat;
    public static final String beepBeepSoundName = SOUNDPATH + "meepmeep"  + SoundFormat;
    public static final String doorBell = SOUNDPATH + "Doorbl" + SoundFormat;
	public static final String CARD_SHUFFLE = SOUNDPATH + "CardShuffle"+ Config.SoundFormat;
	public static final String TimeExpiredMessage = "Time has expired for #1";
	public static final String TimeEndGameMessage = "end the game with a loss for #1";
	public static final String ChangeLimitsMessage = "change the time limits";
	private String RANKING = "ranking";

	/**
	 * board cell iterator types, used to iterate over all cells of a game board.
	 * @author Ddyer
	 *
	 */
	public enum Itype
    {LRTB,	// left-right fast, top-bottom slow
     RLTB,	// right-left fast, top-bottom slow
     TBRL,	// top-bottom fast, right-left slow (normal for hexagonal grids)
     TBLR,	// top-bottom fast, left-right slow
     LRBT,	// left-right fast, bottom-top slow
     RLBT,  // right-left fast, bottom-top slow
     BTLR,	// bottom-top fast, left-right slow
     BTRL,	// bottom-top fast, right-left slow
     ANY,	// no preference
     ;
    }
	
	enum DMType { Move, Command }
    class DeferredMessage
    { 
	     
	  int player; 
      String str;
      DMType type;
      RpcInterface.Keyword key;
      SimpleObserver notify;
 
      DeferredMessage(int pl,String s)
      {	  type = DMType.Move;
      	  notify = null;
      	  key = null;
    	  player = pl;
    	  str = s;
      }
      DeferredMessage(RpcInterface.Keyword t,SimpleObserver not,int pl,String s)
      {	  type = DMType.Command;
    	  key = t;
    	  notify = not;
    	  player = pl;
    	  str = s;
      }
    }
	class Layout {
	    // play rapidly through the entire collection as a test
		private int fileVisits = 0;
		private int fileProblems = 0;

	    private JCheckBoxMenuItem tickTockOption = null;	// play ticktok and hurry sounds
	    private boolean playTickTockSounds = true;
	    private int currentMoveTime ;

	    private Vector<DeferredMessage> deferredMessages = new Vector<DeferredMessage>();
	    // handle stored messages, either immediately or when exiting review mode.
	    private boolean handleDeferredMessages()
	    {	boolean playing = !mutable_game_record;
	    	boolean some = false;
	    	if (playing && reviewMode()) { return(true); }
	     	while(hasDeferredMessages())
	    	{	DeferredMessage st = deferredMessages.elementAt(0);
	    		deferredMessages.removeElementAt(0);
	    		if(filterMessage(st))
	    		{
	    		switch(st.type) {
	    		case Move:
	    			// a game move, nominally from another client,
	    			// which could be from a RPC screen performing an ordinary game operation
	    			some |= performMessage(st.str,st.player);
	    			break;
	    		case Command:
	    			// a request for a service from a RPC screen,
	    			some |= performMessage(st.key,st.notify,st.str,st.player);
	    			break;
	    		default: G.Error("deferred message type %s not expected", st.type);
	    		}
	    		
	            if(GameOverNow()) { stopRobots(); }
	    		}
	    	}
	    	return(some);	//  
	    }
		/**
		 * this is a {@link lib.Slider} active when the "use aux sliders" option is selected.
		 * Setting its min and max values may be useful adjusting the size of artwork. 
		 * Used by {@link #adjustScales}
		 */
		Slider auxXRect = addSlider(".auxXRect","X=",VcrId.sliderXButton);
		/**
		 * this is a {@link lib.Slider} active when the "use aux sliders" option is selected.
		 * Setting its min and max values may be useful adjusting the size of artwork.
		 * Used by {@link #adjustScales}
		 */
		Slider auxYRect = addSlider(".auxYRect","Y=",VcrId.sliderYButton);
		/**
		 * this is a {@link lib.Slider} active when the "use aux sliders" option is selected.
		 * Setting its min and max values may be useful adjusting the size of artwork.
		 * Used by {@link #adjustScales}
		 */
		Slider auxSRect = addSlider(".auxSRect","S=",VcrId.sliderSButton);
		/**
		 * this is a {@link lib.Slider} active when the "use aux sliders" option is selected.
		 * Setting its min and max values may be useful adjusting the size of artwork.
		 * Used by {@link #adjustScales2}
		 */
		Slider auxX2Rect = addSlider(".auxXRect","X2=",VcrId.sliderX2Button);
		/**
		* this is a {@link lib.Slider} active when the "use aux sliders" option is selected.
		* Setting its min and max values may be useful adjusting the size of artwork.
		 * Used by {@link #adjustScales2}
		*/
		Slider auxY2Rect = addSlider(".auxYRect","Y2=",VcrId.sliderY2Button);
		/**
		 * this is a {@link lib.Slider} active when the "use aux sliders" option is selected.
		 * Setting its min and max values may be useful adjusting the size of artwork.
		 * Used by {@link #adjustScales2}
		*/
		Slider auxS2Rect = addSlider(".auxSRect","S2=",VcrId.sliderS2Button);
		// support for 1 hidden window per player
		private HiddenGameWindow[] hiddenWindows = null;
		private RpcInterface[] sideScreens = null;
		
		private boolean drawingGameOverlay = false;
	    private boolean deferWarningSent = false;
	    private boolean tournamentMode = false;
	    
	    private long lastMouseTime = 0;
	    private String mouseMessage = null;
	    private int mouseObject = -1;
	    private JMenuItem zoomButton = null;
	    private JMenuItem unzoomButton = null;
	    private IconMenu paperclipMenu = null;
	    private IconMenu paperclipRestoreMenu = null;
	    private commonPlayer my = null;
	    private JCheckBoxMenuItem auxSliders = null;
	    private JCheckBoxMenuItem mouseCheckbox = null;
	    private JMenuItem layoutAction = null;

		void adjustScales(double pscale[],DrawableImage<?> last)
		{
			if(useAuxSliders
	    			&& (lastDropped!=null) 
	    			&& ((lastDropped.getAltChip(getAltChipset())==last))
	    			)
		    {	// use this to tune piece position
	    		if(lastDropped!=hidden.lastLastDropped)
		    	{
		    		auxSRect.value=pscale[2];
		    		auxXRect.value=pscale[0];
		    		auxYRect.value=pscale[1];
		    		hidden.lastLastDropped = lastDropped;
		        	auxSRect.centerValue();
		        	auxXRect.centerValue();
		        	auxYRect.centerValue();		
		    	}
	    		pscale[0] = auxXRect.value;
	    		pscale[1] = auxYRect.value;
	    		pscale[2] = auxSRect.value;
	    		}
		}
	    void adjustScales2(double pscale[],DrawableImage<?> last)
	    {
	    if(useAuxSliders && (lastDropped!=null) && (lastDropped.getAltChip(getAltChipset())==last))
		    {	// use this to tune piece position
	    		if(last!=hidden.lastLastDropped2)
	    			{
		    		auxS2Rect.value=pscale[2];
		    		auxX2Rect.value=pscale[0];
		    		auxY2Rect.value=pscale[1];
		    		hidden.lastLastDropped2=last;
		    		}
	    		pscale[0] =auxX2Rect.value;
	    		pscale[1] =auxY2Rect.value;
	    		pscale[2] =auxS2Rect.value;
	    		}
	    }
	    private void drawAuxSliders(Graphics inG,HitPoint p)
	    {
	    	auxXRect.draw(inG,p);
    	    auxYRect.draw(inG,p);
    	    auxSRect.draw(inG,p);
    	    auxS2Rect.draw(inG,p);
    	    auxX2Rect.draw(inG,p);
    	    auxY2Rect.draw(inG,p);
	    }
	    
	    
	    // cleanup, call from stopRobots
	    public void disposeHiddenWindows()
	    {	HiddenGameWindow[] hidden = hiddenWindows;
	    	hiddenWindows = null;
	    	if(hidden!=null)
	    	{
	    		for(HiddenGameWindow w : hidden)
	    		{	if(w!=null)
	    		{	
	    			VNCService.unregisterService(w);
	    			w.stopService("game closing");
	    		}
	    	}
	    	}
	    	RpcInterface []ss = sideScreens;
	    	sideScreens = null;
	    	if(ss!=null) { for(RpcInterface si : ss) 
	    		{ if(si!=null) 
	    			{
	    			  RpcService.unregisterService(si);
	    			}
	    		}}
	    	
	    }
	    public void runHiddenWindows()
	    {	
	    	if(hiddenWindows!=null)
	    	{
	    		for(int i=0;i<hiddenWindows.length;i++)
	    		{	HiddenGameWindow w = hiddenWindows[i];
	    			if(w!=null) { w.performMouse(); }
	    		}
	    	}
	    }
	    public HiddenGameWindow getHiddenWindow(int n)
	    {  	if(hiddenWindows!=null && n>=0 && n<hiddenWindows.length)
	    	{
	    		return(hiddenWindows[n]);
	    	}
	    	return(null);
	    }
	    // call from game setup
	    public void createHiddenWindows(int np,int width,int height)
	    {      
	    	disposeHiddenWindows();		// both types
	    	if(REMOTEVNC)
	    	{
	    	HiddenGameWindow w[] = new HiddenGameWindow[np];
	    	for(int i=0;i<np;i++)
	    	{	// position the virtual windows left of the real window
	    		HiddenGameWindow pi = makeHiddenWindow("hidden window "+i,i,width,height);
	    		w[i] = pi;
	    		VNCService.registerService(pi);
	    		
	    	}
	    	hiddenWindows = w;
	    	}
	    	if(REMOTERPC)
	    	{
	    	RpcInterface ss[] = new RpcInterface[np];
	    	for(int i=0;i<np;i++)
	    	{	// position the virtual windows left of the real window
	    		RpcInterface si = new RpcRemoteServer("hidden window "+i,commonCanvas.this,i);
	    		ss[i]=si;
	    		RpcService.registerService(si);
	    		
	    	}
	    	sideScreens = ss;	    	
	    	}

	    }
	    // find the board that corresponds to hp, or null if its the main board
	    public HiddenGameWindow findHiddenWindow(HitPoint hp)
	    {	if(hp!=null)
	    	{
	    	MouseClient cl = hp.parentWindow;
	    	if(cl!=null && cl instanceof HiddenGameWindow)  { return((HiddenGameWindow)cl); }
	    	}
	    	return(null);
	    }
	    private LaunchUserStack localPlayers = new LaunchUserStack();
	    private boolean gameOverlayEnabled = false;
	    private Thread displayThread = null;
	    private String lastParsed = "";
		private SimpleRobotProtocol extraBot;
	    private StringStack panAndZoom  = new StringStack();
	    /** if true, use a copy of the real board for screen drawing.
	     *  this requires some attention to how the animations look.
	     */
	    private BoardProtocol backupBoard = null;	// maybe in the process of being copied
	    private BoardProtocol displayBoard = null;	// safe to display
	    private boolean copyBoardSwapNeeded = false;
	    private String zipArchive = null;
	    private String loadUrlName = null;
	    private String loadUrlGame = null;
	    private boolean urlLoaded = false;
	    private long parsedTime = -1;
	    private String parsedResult = null;
	    private StackIterator<MoveAnnotation>parsedAnnotation = null;
		private commonMove changeMove = null;
	    private double boardZoomStartValue = 0.0;

	}
    // use cautiously, it will not be present in offline games launched directly
    // for debugging or other direct launch configurations.
    public GameInfo gameInfo = null;
    
	public Layout l = new Layout();
	// scrolling directives
	private static final int FORWARD_TO_END = 99999;
	private static final int FORWARD_PLAYER = -7;
	public static final int BACKWARD_PLAYER = -8;
	private static final int BACKWARD_DONE = -9;
	public static final int BACKWARD_ONE = -6;
	public static final int FORWARD_ONE = -5;
	private static final int FORWARD_NEXT_BRANCH = -4;
	private static final int BACKWARD_NEXT_BRANCH = -3;
	private static final int BACKWARD_TO_START = 0;

	private static final String MovesComingIn = "Moves are coming in";
	private static final String ShowSGFMessage = "Show SGF Output";
	private static final String EmailSGFMessage = "Email Game Record";
	private static final String ShowChatMessage = "Show the chat window";
	private static final String HideChatMessage = "Hide the chat window";
    private static String ShowGridAction = "show grid";
    private static String TickTockAction = "Tick Tock sound";
	private static String ExplainUndo = "Undo the last \"Done\" action";
	private static String GameOverMessage = "Game Over!  Final scores ";
	private static String ExplainEdit = "Stop playing, make changes without rules";
	private static String ChoiceString = "#1{ Choices, Choice}";
    private static String StartPlayer = "Start #1";
    private static String RepetitionsMessage = "#1 repetitions";
    private static final String hurrySound = SOUNDPATH + "cowbell" + Config.SoundFormat;
    private static final String OptimizeForFacingString = "Optimize the view for Face to Face players";
    private static final String OptimizeForSideString = "Optimize the view for Side by Side players";
	private static String ResumePlayText = "Start play with #1 as the current player";
	private static String ExplainPleaseUndo = "Ask your opponent to let you undo your last action";
	private static String UploadHelp = "Upload your avatar at #1";
	private static String EditAction = "Edit";
	private static String PlayedOnDate = "Played on #1";
	private static final String TrackMice = "Track Mice";
	private static final String SaveSingleGame = "Save Single Game";
	private static final String RecordOfGame = "Record of Game #1";
	private static final String SaveGameCollection = "Save Game Collection";
	private static final String LoadGameFile = "Load Game File";
	private static final String ReplayGameCollection = "Replay Game Collection";
	private static final String ReplayGameFolder = "Replay games in folder";
	private static final String RemoteFor = "Remote for #1";
	static public String[] CanvasStrings = {
			SaveSingleGame,
			//ReplayGameFolder, // debug only
			//ReplayGameCollection,
			RemoteFor,
			LoadGameFile,
			SaveGameCollection,
			TrackMice,
	        TileSizeMessage,
	        DrawOutcome,
			WonOutcome,
			StackHeightMessage,
			UndoAction,
			SwapDescription,
			YourTurnMessage,
			ExplainDone,
			ExplainPass,
			DrawNotAllowed,
			Reviewing,
			ReverseViewExplanation,
			CensoredGameRecordString,
			ReverseView,
			DoneAction,
			EndTurnAction,
			NoResignMessage,
			NoeyeExplanation,
			EyeExplanation,
			AnimationSpeed,
			DisabledForSpectators,
			ColorBlindOption,
			// game lobby
			CantResign,
			LiftExplanation,
			TimeEndGameMessage,
			ChangeLimitsMessage,
			TimeExpiredMessage,

			ResumePlayText,
			PlayedOnDate,
			EditAction,
			UploadHelp,
			RecordOfGame,
			ExplainPleaseUndo,
			OptimizeForSideString,
			OptimizeForFacingString,
			RepetitionsMessage,
			GameOverMessage,
			StartPlayer,
			ChoiceString,
			ExplainEdit,
			ShowChatMessage,
    		TickTockAction,
    		ExplainUndo,
    		HideChatMessage,
        	ShowSGFMessage,
    		ShowGridAction,
    		EmailSGFMessage,
			MovesComingIn,
			SelectAGameMessage,
			"perspective",
			RESIGN,			// both a move opcode and a menu item
			PASS,
	       	EDIT,
	    	DONE,
	};

	public static final String[][] commonStringPairs = {
			{GoalExplanation, GoalExplanationOnlyDetail},
	    	{GoalExplanationOnly,GoalExplanationOnlyDetail},
	        {SWAP,"Swap colors"}, 
	    	{OFFERDRAW,"Offer a Draw"},
			{ACCEPTDRAW,"Accept a Draw"},
			{DECLINEDRAW,"Decline a Draw"},
	};
    public Bot robot = null; 
    /**
     * Many ways to play, with subtly different expectations in the GUI
     * 
     * The default mode is online/live play.  Here you have a private screen so displaying
     * private information is always OK, and displaying the other player's private information
     * is never ok.  Editing the game record (except by undo) is not allowed.
     * 
     * Turn based games are offline but otherwise very similar to live online games.  The
     * main distinction is that no simultaneous moves are possible, so any phase of games
     * that involves simultaneous moves has to be serialized.
     * 
     * Offline games are subdivided into two major categories "pass and play" games where
     * the players will be sharing a screen, but not simultaneously.  So displaying private
     * information shoudln't be automatic.  And "table games" where all players are seated
     * around a table device, always sharing the screen, and possibly using tablets/phones
     * as a secondary screen.
     * 
     * Cross product these with "Review" mode, where editing is possible and private
     * information should be public, or at least always available.
     * 
     * Cross product with "spectator" mode where the spectators shouldn't be able to see 
     * anything that any player shoudn't see.
     * 
     * also! mutable_game_record is true if either reviewOnly after game review
     * allowed_to_edit is true if mutable_game_record and not a spectator in after game review
     * 
     */
    private TurnBasedViewer.AsyncGameInfo turnBasedGame = null;
    public boolean isTurnBasedGame() { return turnBasedGame!=null; }
    private boolean offlineGame = true;
    public boolean isOfflineGame() 
    { return offlineGame; }
    /** 
     * should be the same as isOfflineGame() && !isTurnBasedGame()
     * @return true if this is a local game (all players present)
     */
    public boolean allPlayersLocal() { return (offlineGame && (turnBasedGame==null)); }
    /**
     * 
     * @return true if is an offline game, and not played on a table device 
     */
    public boolean isPassAndPlay() { return(isOfflineGame() && !isTurnBasedGame() && !G.isTable());}

    public boolean isTableGame() { return G.isTable(); }
    
    class ScoreItem implements CompareTo<ScoreItem>
    {
    	int prettyScore;
    	int score;
    	String name;
    	ScoreItem(String n,int sc,int ps)
    	{
    		name=n;
    		score =sc ;
    		prettyScore = ps;
    	}
    	public int compareTo(ScoreItem o) {
    		return(G.signum(o.score-score));
    	}
    	public int altCompareTo(ScoreItem o) {
    		return(-compareTo(o));
    	}
    }
    

    
    public void performClientMessage(String st0,boolean perform,boolean transmit)
    {	
    	if(perform) { performMessage(st0,-1); }
    	if(transmit) { addEvent(st0); }
    }
    
    public boolean processSpareMessage(String cmdStr,String fullMessage)
    {
    	if(KEYWORD_SPARE.equals(cmdStr))
    	{
    		// this is sent to indicate handling timecontrols
    		return(true);
    	}
    	if(annotationMenu.processSpareMessage(cmdStr,fullMessage))
		{
		 return true;
		}
    	return(false);
    }
    /**
     * called on an incoming move with the raw move string, to perform any preprocessing
     * needed, which allows the game canvas to do things outside of the formal moves.
     * This is currently used to supply player elapsed times, and to introduce out of 
     * band actions to resolve time expired.
     * In general, the current default looks for initial commands that begin with +, with
     * appropriate override and encapsulation, arbitrary things could be done.
     *   
     * This returns a new string (or the same one) that will be passed to @link ParseNewMove for
     * parsing as a move.
     * 
     * @param st the command string
     * @param player the player the move is from 
     * @return a (possibly different or null) string to continue with
     */
    public String preprocessMessage(String st)
    {
    	l.currentMoveTime = -1;
    	while(st!=null && st.charAt(0)=='+')
    	{	// accept a leading token of +T nnn
    		// and for future compatibility, accept +xx xxx 
    		String op;
    		int space1 = st.indexOf(' ');
    		if(space1>0) 
    			{ op = st.substring(0,space1); 
    			  st = st.substring(space1+1); 
    			}
    		else 
    			{ op = st; 
    			  st = "";
    			}
    		
    		if("+T".equals(op)) 
    			{ int space2 = st.indexOf(' ');
    			  String payload = st.substring(0,space2);
    			  st = st.substring(space2+1);
    			  l.currentMoveTime = G.IntToken(payload); 
    			}
    		else if(performMessage(futureTimeControl,op,st)) { st = null ; }
    		else { G.print("Unexpected message ",op); }
    		
    		if("".equals(st)) { st = null; }
    	}
    	return(st);
    }

    /**
     * called after parsing an incoming move, 
     * @param m		if not null, the move that was performed
     * @param success if true, the move was successfully parsed and executed
     */
    public void postProcessMessage(commonMove m,boolean success)
    {
       	if(success && m!=null && l.currentMoveTime>=0) 
		{
		// this assures that the elapsed times agree for all players
		m.setElapsedTime(l.currentMoveTime);
		l.currentMoveTime = -1;
		}	
    }
    /** default filter for incoming Viewer messages
     *  this method is currently unused, but might come in handy
     * 
     * */
    public boolean filterMessage(DeferredMessage st)
    {
    	return true;
    }
    
    // replay one incoming move.  This is the entry point for network moves
    // and also for rpc local mirror moves.
    public boolean performMessage(String st0,int player)
    {	boolean playing = !mutable_game_record;
    	//Plog.log.appendNewLog("performMessage ");
    	//Plog.log.appendLog(player<0 ? whoseTurn() : player);
    	//Plog.log.appendLog(' ');
    	//Plog.log.appendLog(st0);
    	//Plog.log.finishEvent();
    	String st = preprocessMessage(st0);
    	
    	boolean success=false;
    	commonMove m = null;
    	if(st!=null)
    	{
        m = ParseNewMove(st, player);
        // clear the move index so moves don't become variations if they would otherwise
        // just supersede the previous move
        m.setIndex(-1);
        l.lastParsed = "Replay "+m;
        // note, capturing the value here is crucial.  If this is a game ending move
        // the the value of "mutable" will be changed by perform and transmit.
        // we depend on the caller seeing true here to correctly run the "finishUp" process.     
        if(m!=null) { success = PerformAndTransmit(m, false,replayMode.Live); }
    	}
        postProcessMessage(m,success);
        // note that when gameover occurs, playing would now be false if we re evaluated it.
    	return (playing);
    }
    // perform a rpc request
    public boolean performMessage(RpcInterface.Keyword cmd,SimpleObserver who,String st0,int player)
    {	
    	switch(cmd)
    	{
    	case Complete:
    		{
    		String str = serverRecordString();
    		who.update(getObservers(), cmd, str);
    		return true;
    		}
    	default: 
    		throw G.Error("Rpc command %s not expected", cmd);
    	}

    }

    class Hidden
	{
		private int chatSizeAdj = 0;
		private commonPlayer lastPlayer=null;
	    private long startTurn = 0;
	    private long doneAtTime=0;
		private long lastAnimTime = 0;
	    private Object lastLastDropped = null;
	    private Object lastLastDropped2 = null;
	    private long timePerTurn = (2 * 60 * 1000);	// tick him after this much time
	    private long timePerDone = 15*1000;		
	    private CellId vcr6ButtonCodes[] = 
	    	{ VcrId.WayBackButton, VcrId.BackPlayerButton, VcrId.BackStepButton,
	    	VcrId.ForeStepButton, VcrId.ForePlayerButton, VcrId.ForeMostButton};
	    

	    private Rectangle vcrFrontRect = addRect(".vcrFrontRect");
	    private Rectangle vcrButtonRect = addRect(".vcrButtonRect");
	    private Rectangle vcrVarRect = addRect(".vcrVarRect");
	    private Rectangle vcrBackRect = addRect(".vcrBackRect");
	    private Rectangle normalVcrRect = new Rectangle();	// copy of vcrZone until expanded
	    private Rectangle outsideVcrRect = new Rectangle();
	    private boolean vcrExpanded = false;
	    // common support for reviewer windows
	    private sgf_gamestack Games = null;
	    private sgf_game selectedGame = null;
	    private JMenu gamesMenu = null;
	    private PopupManager gamePopup =  new PopupManager();
	    private PopupManager threadPopup = new PopupManager();
	    private PopupManager robotPopup = new PopupManager();
	    private SimpleRobotProtocol threadPopupRobot = null;
	    private JMenuItem saveGame = null;
	    private JMenuItem loadGame = null;
	    private JMenuItem saveCollection = null;
	    private JMenuItem replayCollection = null;
	    private JMenuItem replayFolder = null;
	    private JMenuItem gameTest = null;
	    
	    private JMenuItem resignAction = null;
	    private JMenuItem offerDrawAction = null;
	    private JMenuItem editMove = null;
	    private JMenuItem saveAndCompare = null;
	    private boolean separateChat = false;		// chat in a separate window
	    private boolean hiddenChat = false;			// chat in current window, but hidden
	    private JMenuItem showText = null;
	    private JMenuItem showSgf = null;
	    private JMenuItem emailSgf = null;
	    private PopupManager panzoomPopup = new PopupManager();

	    private JMenuItem treeViewerMenu = null; //for testing, 
	    private JMenuItem alphaViewerMenu = null; //for testing, 
	    private JCheckBoxMenuItem alternateBoard = null;
	    private JMenuItem truncateGame = null; // truncate the game here
	    private JMenuItem evalRobot = null; //start the robot static evaluator
	    private JMenuItem testRobot = null; //ask the robot for a test move
	    private JMenuItem startRobot = null; //start the robot level 0 as a running process
	    private JMenuItem robotLevel0 = null;
	    private JMenuItem stopRobot = null; //stop the robot from playing
	    private JMenuItem pauseRobot = null; //stop the robot from playing
	    private JMenuItem resumeRobot = null;	// resume the robots
	    private JMenuItem selectRobotThread = null;	// select which thread to view
	    private JMenuItem saveVariation = null;	// save the robot's variation (presumably at an error)
	    private JMenuItem runGameDumbot = null;	// run a test game
	    private JMenuItem runGameSelf = null;	// run a test game
	    private JMenuItem train = null;		// run training
	    private JMenuItem stopTrain = null;	// request stop
	    private JMenuItem startShell = null; 	// start a bean shell
	    /**
	     * this is a {@link lib.Slider} this is a rectangle embedded in the VCR control cluster
	     */
	    private Slider animationSpeedRect = addSlider(".animationSpeed",s.get(AnimationSpeed),VcrId.sliderAnimSpeedButton,0.0,2.0,0.6);
	    
	    private PopupManager vcrVarPopup = new PopupManager();
	
	    private boolean controlToken=false;
	    private long controlTimeStamp = 0;
	    private Thread cnThread = null;
	    private BoardProtocol savedBoard = null;
	    private TreeViewer treeViewer = null; 
	
	    private boolean pendingControlExpired()
	    {	Thread myThread = Thread.currentThread();
	    	if(cnThread==null) { cnThread = myThread; }
	    	//G.Assert(myThread==cnThread,"thread mismatch, is %s should be %s",myThread,cnThread);
	    	// 

	    	return(controlToken);
	    }

	    /**
	     * switch to the other player for the next move.
	     */
	    private void doEditMove()
	    {	// not usually used in the game, but available in debugging situations to just toggle the player.
	    	PerformAndTransmit(EDIT);
	    }
	 
	    //
	    // this console, based on "beanshell" 
	    //
	       private void startShell()
	       {	
	       	ShellProtocol shell = (ShellProtocol)G.MakeInstance("lib.ShellBridge");
	       	shell.startShell("viewer",this,"board",getBoard(),"out",System.out);
	       	G.setPrinter(shell);
	       }
	       private void selectRobot(int x,int y)
	       {	robotPopup.newPopupMenu(commonCanvas.this,deferredEvents);
	       	for(Bot b : Bot.values())
	       	{	if(b.idx>=0) { hidden.robotPopup.addMenuItem(b.name,b); }   		
	       	}
	       	robotPopup.show(x,y);
	       }
	       private void selectRobotThread(int x,int y)
	       {	
	       	for (int n = 0; n < players.length; n++)
	           {
	              commonPlayer player = players[n];
	               if (player != null) 
	               {
	               	SimpleRobotProtocol r = player.robotBeingMonitored();
	               	if(r!=null)
	               	{	Thread threads[] = r.getThreads();
	               		if(threads!=null)
	               		{	
	               			threadPopup.newPopupMenu(commonCanvas.this,deferredEvents);
	               			threadPopupRobot = r;
	               			for(Thread t : threads) { threadPopup.addMenuItem(t.toString(),t); }
	               			threadPopup.show(x,y);
	               		}
	               	}
	               }
	           }
	       }
	       private void runRobotGameDumbot()
	       {	SimpleRobotProtocol rr = l.extraBot = newRobotPlayer();
	       	rr.runRobotGameDumbot(commonCanvas.this,getBoard(),newRobotPlayer());
	       }
	       private void runRobotGameSelf()
	       {	SimpleRobotProtocol rr = l.extraBot = newRobotPlayer();
	       	rr.runRobotGameSelf(commonCanvas.this,getBoard(),newRobotPlayer());
	       }
	       private void runRobotTraining()
	       {
	       	SimpleRobotProtocol rr = l.extraBot = newRobotPlayer();
	       	rr.runRobotTraining(commonCanvas.this,getBoard(),newRobotPlayer());
	       }
	       private void stopTraining()
	       {
	    	   if(l.extraBot!=null) { l.extraBot.stopTraining(); }
	       }
	       private void saveRobotVariation()
	       {
	           for (int n = 0; n < players.length; n++)
	           {
	              commonPlayer player = players[n];
	               if (player != null) 
	               {
	               	SimpleRobotProtocol r = player.robotRunning();
	               	if(r!=null)
	               	{	commonMove var = r.getCurrentVariation();
	               		commonMove lastMove = null;
	               		if(var!=null)
	               		{
	               			//var.showPV("Solution "+number_of_solutions+" ");
	               			CommonMoveStack hist = new CommonMoveStack();
	   				
	   						{
	   							CommonMoveStack vhist = History;
	   							for(int i=0,lim=vhist.size();i<lim;i++)
	   							{	// copy the game before starting
	   								commonMove elem = vhist.elementAt(i).Copy(null);
	   								if(lastMove!=null) { lastMove.next = elem; }
	   								hist.push(elem);
	   								lastMove = elem;
	   							}
	   						}
	   						while(var!=null)
	   						{	// add the moves in this solution
	   							if((lastMove.player!=var.player)
	   									&& (lastMove.op!=MOVE_DONE))
	   							{	// insert a "done" before a turn change
	   								commonMove don = lastMove.Copy(null);
	   								don.op = MOVE_DONE;
	   								don.setIndex(lastMove.index()+1);
	   								lastMove.next = don;
	   								hist.push(don);
	   								lastMove = don;
	   							}
	   							var.setIndex(lastMove.index()+1);
	   							lastMove.next = var;
	   							lastMove = var;
	   							hist.push(var);
	   							var = var.best_move();
	   						}
	   						doSaveGame(save_game(hist));

	               		}

	               	}
	               }
	           }
	           getActivePlayer().stopRobot();
	       }
	       /** 
	        * show the unedited game history
	        */
	       private void doShowRawText()
	       {
	    	   TextDisplayFrame w = new TextDisplayFrame("Text of Game");        
	    	   w.setText(gameRecordString());
	       }
	       
	    private boolean handleDeferredEvent(Object target)
	    {	if(target==l.zoomButton)
	    	{
	    	setGlobalZoomButton();
	    	}
	    	else if(target==l.unzoomButton)
	    	{
	    	setGlobalUnZoomButton();
	    	}
	    	else if(target==saveAndCompare)
	        {	
	        	if(savedBoard!=null) { savedBoard.sameboard(getBoard()); G.print("Ok"); savedBoard=null; }
	        	else { savedBoard = getBoard().cloneBoard(); G.print("Saved "+savedBoard); }
	        }
	        else if (target == showText)
	        {
	            doShowRawText();
	        }
	        else if (target == emailSgf)
	        {
	        	doEmailSgf();
	        }
	        else if (target == showSgf)
	        {
	            doShowSgf();
	        }
	        else if(target==resignAction)
	        {  doResign();
	        }
	        else if(target==offerDrawAction)
	        {
	        	doOfferDraw();
	        }
	        else if (target == editMove)
	        {
	        	doEditMove();
	        }
	        else if (target == robotLevel0) 
	        	{   selectRobot(robotLevel0.getX(),robotLevel0.getY());
	        	}
	        else if (target == startShell) {  startShell(); }
	        else if (target == stopRobot)   {   stopRobots();  }
	        else if (target == pauseRobot)   {   pauseRobots();  }
	        else if (target == resumeRobot)   {   resumeRobots();  }
	        else if (target == selectRobotThread) { selectRobotThread(selectRobotThread.getX(),selectRobotThread.getY()); }
	        else if (target == saveVariation) { saveRobotVariation(); }
	        else if (target == runGameDumbot) { runRobotGameDumbot(); }
	        else if (target == runGameSelf) { runRobotGameSelf(); }
	        else if (target == train) { runRobotTraining(); }
	        else if (target == stopTrain) { stopTraining(); }
	        else if (target == loadGame)
	        {
	        	doLoadGame();
	        }
	        else if (target == saveGame)
	        {
	            doSaveGame();
	        }
	        else if (target == saveCollection)
	        {	doSaveCollection();
	        }
	        else if (target== replayCollection)
	        {
	        	doReplayCollection();
	        }
	        else if (target== replayFolder)
	        {
	           	doReplayFolder();
	        }
	        else if (target== gameTest)
	        {
	        	doGameTest();
	        }
	        else if (target==treeViewerMenu)
	        {
	        	doTreeViewer(true);
	        }
	        else if (target==alphaViewerMenu)
	        {
	        	doTreeViewer(false);
	        }
	        else if(robotPopup.selectMenuTarget(target))
	        {
	        	robot = (Bot)robotPopup.rawValue;
	        }
	        else if(threadPopup.selectMenuTarget(target))
	        {	SimpleRobotProtocol r = threadPopupRobot;
	        	if(r!=null)
	        	{	Thread val = (Thread)threadPopup.rawValue;
	        		if(val!=null) { r.selectThread(val); }
	        	}
	        }
	        else if(gamePopup.selectMenuTarget(target))
	        {
	        	selectGame((sgf_game)gamePopup.rawValue);
	        }
	        else if (panzoomPopup.selectMenuTarget(target))
	        {
	        	doRestorePanZoom((String)panzoomPopup.rawValue);
	        }
	        else if (target==gamesMenu) 
	        	{ if(!gamePopup.isShowing())
	        		{ selectGameMenu(gamesMenu.getX(),gamesMenu.getY());
	        		}
	        	}
	        else if (target == startRobot)
	        {  	commonPlayer who = currentGuiPlayer();
	        	commonPlayer ap = getActivePlayer();
	            commonPlayer started = startRobot(who,ap,robot);
	            if(started!=null) { started.runRobot(true); }
	        }   
	        else if (target == alternateBoard)
	        {
	            useAlternateBoard = alternateBoard.getState();
	        }
	        else if (target == truncateGame)
	        {
	            doTruncate();
	        }
	        else if (target == evalRobot)
	        {
	          // this is a debugging hack to print the robot's evaluation of the
	          // current position.  Not important in real play
	        	startRobot(getActivePlayer(),getActivePlayer(),robot);
	        	
	        }
	        else if (target == testRobot)
	        {   
	        	commonPlayer who = viewerWhoseTurn();
	        	commonPlayer act = getActivePlayer();
	        	commonPlayer started = startRobot(who,act,robot);
	            if(started!=null) 
	            	{ 
	            	  startRobotTurn(started);
	            	}
	        }
	        else { return(false); }

	    	return(true); 
	    }
	    /** 
	     * draw the vcr control buttons
	     * @param p
	     * @param inG
	     * @param HighlightColor
	     * @param vcrButtonColor
	     * @return the hit code for the sub-button hit
	     */
	    private CellId drawVcrButtons(HitPoint p, Graphics inG)
	    {
	        CellId rval = null;
	        CellId codes[] = vcr6ButtonCodes;
	        int nButtons = StockArt.vcrButtons.length;
	        Rectangle r = vcrButtonRect;
	        int vcrWidth = G.Width(r) / nButtons;
	        boolean iny0 = (p != null) && (G.Top(p) >= G.Top(r)) && (G.Top(p) < G.Bottom(r));
	        boolean iny = iny0 && !p.dragging;

	        for (int i = 0; i < nButtons; i++)
	        {
	            int currentX = G.Left(r) + (i * vcrWidth);
	            boolean inbox = iny && (G.Left(p) >= currentX) &&
	                (G.Left(p) <= (currentX + vcrWidth));

	            if (inG != null)
	            {
	                GC.setFont(inG,standardBoldFont());
	                StockArt.vcrButtons[i].drawChip(inG,commonCanvas.this,vcrWidth+(inbox?vcrWidth/3:0),currentX+vcrWidth/2,
	                		G.Top(r)+G.Height(r)/2,null);
	            }
	            if (inbox)
	            {
	                    p.hitCode = rval = codes[i];
	            }
	        }
	        if((rval==null) && G.pointInRect(p,vcrZone))
	        {	// hit the background not occupied by a button
	        	rval = VcrId.noVcrButton;
	        }
	        return (rval);
	    }
	    private boolean enterReviewMode()
	    { 	int size = History.size();
	        if ((History.viewStep == -1) && (size > 0))
	        {
	            // entering review mode. Remember whose turn it really is
	            // this is so the outside world can be kept in the dark and incoming
	            // moves can be handled properly.
	        	BoardProtocol bb = getBoard();
	        	History.pre_review_state = bb.getState();
	        	History.viewMoveNumber = bb.moveNumber();
	            History.viewTurn = currentGuiPlayer();
	            History.viewStep = size;
	            commentedMove = History.currentHistoryMove();
	            commentedMoveSeen = null;
	           	generalRefresh(); 
	            return(true);
	        }
	        return(false);
	    }
	    
	    //
	    // this construction for doBack with N as an argument
	    // allows us to do UndoStep just once instead of once per step
	    //
	    private boolean doBack(int n)
	    {
	        hidden.enterReviewMode();

	        int v0 = History.viewStep;

	        if (History.viewStep > 0)
	        {	showComments();	// save as a side effect
	            UndoStep( v0-n);
	        }

	        showComments(); 
	        return ((History.viewStep!=-1) && (History.viewStep < v0));
	    }
	    private void RedoStep(replayMode replay,commonMove m)
	    {
	        Execute(m,replay);
	        if(m.digest==0) { m.digest = getBoard().Digest(); }
	        repeatedPositions.checkForRepetition(getBoard(),m);
	        if(History.viewStep==-1) {  generalRefresh(); }
	    }
	    private commonMove removeHistoryElementRecurse(int idx)
	    {	int siz = History.size();
			if(idx>=siz) { return(null); }
			if(idx+1==siz) { return(popHistoryElement()); }
	    	commonMove rem = popHistoryElement();
	  	  	commonMove val = removeHistoryElementRecurse(idx);
	  	  	if (History.size() > 0)
	  	  	{	
	            commonMove m = History.top();
	            rem = m.addVariation(rem);
	        }
	  	  rem.digest = 0;		// digest will be inaccurate when undoing out of order
	  	  rem.addToHistoryAndExtend(History);
	  	  return(val);
	    }
	    
	    private void doTreeViewer(boolean uct)
	    {   
	    	commonPanel panel = new commonPanel();
	    	String name = uct ?  "UCT search tree viewer"
	    				: "AlphaBeta search tree viewer";
	    	
	    	XFrame frame = new XFrame(name);
	    	if(uct)
	    		{
	    		treeViewer = (UCTTreeViewer)G.MakeInstance("online.search.UCTTreeViewer");
	    		}
	    	else {
		    	treeViewer = (AlphaTreeViewer)G.MakeInstance("online.search.AlphaTreeViewer");
	    	}
	    	treeViewer.init(sharedInfo,frame);
	    	treeViewer.setCanvas(commonCanvas.this);
	    	panel.setCanvas(treeViewer);
	    	frame.setContentPane(panel);
	    	treeViewer.setVisible(true);
	    	
	    	frame.setInitialBounds(100,100,(int)(SCALE*800),(int)(SCALE*600));
	    	frame.setVisible(true);
	    	panel.start();
	    }
	
	    private void gameSubMenu(MenuInterface sub,int start,int to)
	    {	
	    	if((to-start)<=40)	// terminal nodes
	    	{
	    		while(start<to) 
		    	{
		    		sgf_game game = hidden.Games.elementAt(start);
		    		String name = game.short_name();
		    		start++;
		    		if(name==null) { name = "Game #"+start; }
		    		gamePopup.addMenuItem(sub,name,game);
		    	}
	    	}
	    	else	// multiple levels
	    	{	int span = ((to-start)<(25*40)) ? 25 : ((to-start)/25);
	    		while(start<to)
	    		{int end = Math.min(to,(start+span));
	    		 MenuInterface m = gamePopup.newSubMenu(""+(start+1)+"..."+end);
				 gameSubMenu(m,start,end);
				 gamePopup.addMenuItem(sub,m);
				 start += span;
	    		}
	    	}

	    }
	  
	    private void selectGameMenu(int x,int y)
	    {	gamePopup.newPopupMenu(commonCanvas.this,deferredEvents);
	    	if(hidden.Games!=null)
	    	{
	    	 gameSubMenu(null,0,hidden.Games.size());
	    	}
	    	gamePopup.show(x,y);
	    }
	    

	    private boolean hasConnection(commonPlayer pl)
	    {
	    	return((G.Date()-pl.lastInputTime)<3*60*1000);
	    }

		
	    /**
	     * draw the player name, clock, and network activity spinner.  If hp
	     * is not null, draw the player name as a "start" button.
	     * 
	     * @param gc	  the gc
	     * @param pl	  a commonPlayer 
	     * @param hp	  the mouse location
	     * @param code	  the hitCode to use if the button is hit, or null to present as a label
	     * @param highlightColor
	     * @param backgroundColor
	     */
	    private void drawPlayerStuff(Graphics gc,commonPlayer pl,
	    		HitPoint hp,CellId code,
	    		Color highlightColor,Color backgroundColor)
	    {	pl.setRotatedContext(gc, hp, false);
	        String p0Name1 = prettyName(pl.boardIndex);
	        Font nameFont = largeBoldFont();
	        GC.setFont(gc,nameFont);
	        boolean active = playerIsActive(pl);
	        if(code!=null)
	        {
	        String p0Name2 = s.get(StartPlayer,p0Name1);
	        if(GC.handleSquareButton(gc, pl.nameRect, hp, p0Name2,	highlightColor,
	        		active
	        			?highlightColor
	        			:backgroundColor))
	        	{	hp.hitCode = code;
	        		hp.setHelpText(s.get(ResumePlayText,p0Name1));
	        	}
	        }
	        else
	        {
	        	GC.handleRoundButton(gc, pl.nameRect, null, p0Name1,	highlightColor,
	            		active
	            			?highlightColor
	            			:backgroundColor);
	        }
	     	if(gc!=null) 
	     		{ pl.drawPlayerImage(gc, pl.picRect); 
	     		  HitPoint.setHelpText(hp, pl.picRect,s.get(UploadHelp,bridge.Config.uploadPicture));
	     		  
	     		}

	     	boolean review = reviewMode();
	
	    	Rectangle ar = pl.animRect;
	    	long ptime = review ? pl.reviewTime : pl.elapsedTime;
	    	if(ar!=null && hasConnection(pl))
		    	{
	    		GC.draw_anim(gc,ar,G.Width(ar)/2,pl.lastInputTime,pl.progress);
		    	}
    
	    	String timeString = G.timeString(ptime);
	    	int hgt = G.Height(pl.timeRect);
	    	if(hgt>0)
	    	{
	    	Font timeFont = G.getFont(nameFont,hgt);
	    	GC.printTimeC(gc, pl.timeRect,timeString, 
	    				   review?Color.blue:Color.black,null, timeFont);
	    	if(G.Height(pl.extraTimeRect)>0)
	    	{	Font timeFont2 = G.getFont(nameFont,hgt*2/3);
    			printExtraTime(gc,pl,pl.extraTimeRect,timeControl,timeFont2);
    	    }}
    		pl.setRotatedContext(gc, hp, true);
	    }
	    private void printExtraTime(Graphics gc,commonPlayer pl,Rectangle r,TimeControl time,Font timeFont)
	    {	TimeControl.Kind kind = time==null ? TimeControl.Kind.None : time.kind;
 			boolean review = reviewMode();
	    	long ptime = review ? pl.reviewTime : pl.elapsedTime;
 	    	long ftime = time==null ? 0 : time.fixedTime;
	    	int warntime = 30*1000;
	    	Font font = timeFont;
	    	Color col = Color.blue;
	    	StringBuilder b = new StringBuilder();
	    	int dtime = 0;
	    	switch(kind)
	    	{
	    	default: G.Error("Not expecting kind %s",kind);
	    	case None:
	    	case Differential:
	    		{
	       		long mintime = ptime;
	     		for(commonPlayer p : players)
		    	{
		    		if(p!=null) { mintime = Math.min(mintime,review?p.reviewTime : p.elapsedTime); }
		    	}
	     		if(ptime<=mintime) { return; };	// don't print anything

	     		dtime = (int)(ptime-mintime);
	    		
	    		if(kind==TimeControl.Kind.Differential)
	    		{	
	    			col = ! ((ptime>time.fixedTime) &&	(dtime>time.differentialTime))
								? ((ptime+warntime<time.fixedTime)||(dtime+warntime<time.differentialTime))
										? Color.blue 
												: Color.yellow
												: Color.red;
	    		}
	    		else if(gameInfo==GameInfo.GameTimer)
	    			{ col = Color.gray; font=largeBoldFont(); 
	    			}
	    		b.append("+ ");
	    		}
	    		break;
	    	case Incremental:
	    		{
	    		int otime = numberOfOvertimeMoves(pl);
	    		ftime += otime*time.moveIncrement;
	    		{
	    		dtime = (int)(ftime -ptime);
	    		col = (dtime>0) 
	    					? dtime<warntime 
	    							? Color.yellow
	    							: otime>0 ? new Color(0.3f,0.4f,1.0f) : Color.blue 
	    					: Color.red;
	    		if(dtime<0) { b.append("-"); }
	    		
	    		}}
	    		break;
	    	case PlusTime:
	    		ftime += numberOfCompletedMoves(pl)*time.moveIncrement;
	    	case Fixed:
	    		{
	    		dtime = (int)(ftime -ptime);
	    		col = (dtime>0) 
	    					? dtime<warntime ? Color.yellow : Color.blue 
	    					: Color.red;
	    		if(dtime<0) { b.append("-"); }
	    		
	    		}
	    	}
    	
	    	b.append(G.briefTimeString(Math.abs(dtime)));
	    	if(col==Color.yellow) 
	    		{
	    			b.append(".");
	    			b.append((dtime%1000)/100);
	    			repaint(100);	// don't miss any ticks
	    		}
	    	GC.printTimeC(gc, pl.extraTimeRect,
	    			b.toString(),	// round up
	    			col,null,font);
	
	    }
	    private int numberOfOvertimeMoves(commonPlayer pl)
	    {	// this might seem be more complicated than strictly necessary,
	    	// but with multiple actions per move (as in zertz) it's not simple.
	    	int timeex = pl.principleTimeExpired;
	    	if(timeex<=0) { return 0; }
	    	int completed = numberOfCompletedMoves(pl);
	    	int over = completed-timeex+1;
	    	return over;
	    }
	    
	    private int numberOfCompletedMoves(commonPlayer pl)
	    {	// this might seem be more complicated than strictly necessary,
	    	// but with multiple actions per move (as in zertz) it's not simple.
	    	int count = 0;	
	    	int targetPlayer = pl.boardIndex;
	    	int last = reviewMode() ? History.viewStep : History.size()-1;
	    	int prevPlayer = -1;
	    	if(pl==currentGuiPlayer()) 
	    		{ // this awards the "per move" time at the beginning of the player's turn
	    		  // instead of at the end.  The reason for this is that when the overtime
	    		  // event occurs, and the player moves anyway, he would go from slightly
	    		  // overtime to slightly under time
	    		count++; 
	    		}
	    	for(int num = 1; num<=last; num++)
	    	{
	    		commonMove m = History.elementAt(num);
	    		if(m!=null)
	    		{ if(m.player!=targetPlayer && prevPlayer==targetPlayer)
		    		{
		    			count++;
		    		}
	    		prevPlayer = m.player;
	    		}
	    	}
	    	if(prevPlayer==targetPlayer)
				{ BoardProtocol b = getBoard();
				  if(b!=null && (b.whoseTurn()!=targetPlayer)) { count++; }
				}
	    	return(count);
	    }
	    // this is overridable but do not call it directly, call doScrollTo(FORWARD_ONE);
	    private boolean doForward(replayMode replay)
	    {
	    	return doForwardStep(replay);
	    }


	
	} 	// end of hidded class definition
    
	Hidden hidden = new Hidden();
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	/**
	 * animating is true while we are animating replay of a whole game, triggered by the |> button on the vcr panel.
	 */
	public boolean animating = false;
	/**
	 * this is the value associated with the slider that appears during "movie" replays.  When establishing
	 * individual animation steps, multiply time by masterAnimationSpeed 
	 */
	public double masterAnimationSpeed = 1.0;
	public boolean started = false;
	public boolean startedOnce = false;
	public boolean stopped = false;
	/**
	 * return a guesstimate of what move number constitutes "midgame"
	 * this is used as a marking point to save the digest for "repeated 
	 * game" fraud detection.
	 */
	public int midGamePoint() { return(20); }
	
	public boolean discardable()
	{	int step = reviewMode()? History.viewMoveNumber : getBoard().moveNumber();
		return (step<=2);
	}
    /**
     * this is a standard rectangle of all viewers - the rectangle that 
     * contains the board.  Your {@link #setLocalBounds} method still has
     * to position this rectangles.
     */
    public Rectangle boardRect = addRect(".boardRect"); //the actual board, normally at the left edge
    /**
     * this is a standard rectangle of all viewers - the rectangle that explains
     * the goal of the game.  It's usually placed at the bottom of the board.
     * In timed games, this will display the time constraints for the game.
     */
    public Rectangle goalRect = addRect("goalRect");	// standard rectangle for game goal
    
    /** rectangle where the link to rules will be displayed
     * 
     */
    public Rectangle rulesRect = addRect("rules");
    /**
     * this is a standard rectnagle of all viewers, normally displays the word "done"
     */
    public Rectangle doneRect = addZoneRect("doneRect");
    /**
     * this is a standard rectangle shared by all viewers, normally
     * positioned paired with doneRect
     */
    public Rectangle editRect = addRect("editRect");
    /**
     * standard rectangles of all viewers, showing the private "done" rectangle
     * for the player in offline mode.
     */
    public Rectangle doneRects[] = addZoneRect("zone",Session.MAXPLAYERSPERGAME);
    
    /** standard rectangle of all viewers used to display the log of game's moves */
    public Rectangle logRect = addZoneRect("logRect"); //the game log, normally off the the right

    public AnnotationMenu annotationMenu = new AnnotationMenu(this,GameId.ShowAnnotations);
    public NumberMenu numberMenu =  new NumberMenu(this,StockArt.EmptyCheckbox,GameId.ShowNumbers);
   
    /**
     * this is a standard rectangle shared by all viewers which displays the progress
     * made by the robot.  It's normally positioned inside goalRect
     */
    public Rectangle progressRect = addRect("progressRect");	// standard rectangle for robot progress
    
    /**
     * override this method to do just the drawing of the board background and grid,
     * sepatated from whatever else the drawFixedElements method does.  Then call
     * drawFixedBoard(gc) to tap into the optional rotation of the board that may
     * or may not be in effect.
     * @param gc
     * @param r
     */
    public void drawFixedBoard(Graphics gc,Rectangle r)
    {
    	G.Error("override this method");
    }
    /**
     * provide the rotation context, if needed, for the board.  Define drawFixedBoard(gc,rect)
     * to do the work, after the optional board rotating transformation is applied.
     * @param gc
     */
    public void drawFixedBoard(Graphics gc)
    {
        GC.setRotatedContext(gc,boardRect,null,contextRotation);
        drawFixedBoard(gc,boardRect);
        GC.unsetRotatedContext(gc,null);      
    }

     /**
     * this is the standard rectangle for the current game state/action to take
     * which is usually placed on top of the board rectangle
     */
    public Rectangle stateRect = addRect("stateRect");
    /**
     * this is the standard rectangle to display some sort of player
     * icon next to the player name
     */
    public Rectangle iconRect = addRect("iconRect");
    /**
     * this is a standard rectangle of all viewers - the rectangle that 
     * contains the hide chat icon.  Your {@link #setLocalBounds} method still has
     * to position this rectangles.
     */
    public Rectangle noChatRect = addRect("nochat");
   
    /**
     * common rectangle to hold the view selection rectangle
     */
    public Rectangle viewsetRect = addRect("viewset");
    
	
    /** for games with a scaleable board, this is the conventional name for
     * the slider that controls the scale
     */
    public Slider zoomRect = null;
    /** for games with a boardless board, center of the visible portion
     */
    public double board_center_x = 0.0;
    /** for games with a boardless board, center of the visible portion
     */
    public double board_center_y = 0.0;


    /**
     * this is a standard rectangle of all viewers - the rectangle that 
     * contains the board.  Your {@link #setLocalBounds} method still has
     * to position this rectangles.
     */
    public Rectangle chatRect = addZoneRect("chatRect"); // the chat window
    
    public boolean avoidChat(int x,int y)
    {
    	return((chatRect!=null) 
    			&& (theChat!=null)
    			&& theChat.isWindow()	// old style chat window 
    			&& G.pointInRect(x,y,chatRect));
    }
    /** default for main colors of mouse sprites, index by player boardIndex,
     * for up to 6 players.  These are the default colors of the opponent's mice.
     * */
    private Color SpectatorColor = new Color(0.5f,0.5f,0.5f);
    /**
     * the default center color for spectator's mouse sprites.
     */
    private Color SpectatorDotColor = new Color(0.9f,0.9f,0.9f);
    /**
     * MouseColors are the default array of 6 colors for the mouse tracking sprites that
     * appear in live games.  You can replace this array with your own array containing game-appropriate colors 
     */
    public Color[] MouseColors = 
    	{ Color.white, Color.black ,
    	  Color.green, Color.yellow,
    	  Color.blue,Color.red,
    	  Color.orange,Color.cyan,
    	  Color.darkGray,Color.lightGray,
    	  Color.pink,Color.magenta};
    /** default for center dots of mouse sprites */
    public Color[] MouseDotColors = 
    	{ Color.black, Color.white, 
    		Color.yellow, Color.green,
    		Color.red,Color.blue,
    		Color.cyan,Color.orange,
    		Color.lightGray,Color.darkGray,
    		Color.magenta,Color.pink
    		};

    /**
     * draw a mouse sprite and possibly some other object associated
     * with the mouse.  The moving object is encoded by {@link #getMovingObject}
     * <p>
     * note that this is not a real-time process, the mouse position and
     * object being dragged may differ from the internal state of the board.
     * @param g
     * @param idx
     * @param x
     * @param y
     */
    public abstract void drawSprite(Graphics g,int idx, int x,int y);
    /**
     * draw the annotation sprite if it is active
     * @param g
     * @param obj
     * @param xp
     * @param yp
     * @param mode
     */
    public void drawAnnotationSprite(Graphics g,int obj,int xp,int yp,String mode)
    {
    	annotationMenu.drawAnimationSprite(g,obj,xp,yp,mode,cellSize());
    }

    
    /**
     * draw a representation of someone else's mouse position.  The default is a circle with
     * a center dot in a contrasting color.   The colors are determined by {@link #MouseColors MouseColors[]} and
     * {@link #MouseDotColors MouseDotColors[]} which can be replaced. Player can be -1 to indicate a spectator.
     * 
     * @param g
     * @param player a player index or -1
     * @param xp
     * @param yp
     */
    public void drawMouseSprite(Graphics g,int player,int xp,int yp)
    {	Color main = SpectatorColor;
    	Color dot = SpectatorDotColor;
    	if(g!=null)
    	{
    	if(player>=0) 
    		{	player = player%MouseColors.length;
     			main =  getMouseColor(player);
    			dot = getMouseDotColor(player);
    		}
    	  g.drawLargeSpot(xp, yp,main, dot,(int)(16*SCALE));
        }
    }
    private int getPlayerColorIndex(int player)
    {
    	int map[] = getBoard().getColorMap();
		if(player>=0 && player<map.length)
			{
				return(map[player]);
			}
		return(0);
    }
	public int nPlayers() {
		return(getBoard().nPlayers());
	}
	public DefinedSeating seatingChart() 
	{
		return seatingChart.id;
	}
    /**
     * get the mouse color to use for a player, considering
     * the default colors for the game and the current color map
     * @param player
     * @return a color
     */
    public Color getMouseColor(int player)
    {
    	return(MouseColors[getPlayerColorIndex(player)]);
    }
    /**
     * get the contrast color to use for a player, considering
     * the default colors for the game and the current color map
     * @param player
     * @return
     */
    private Color getMouseDotColor(int player)
    {
    	return(MouseDotColors[getPlayerColorIndex(player)]);
    }

 
    /**
     * send a note to the server log file.   This is intended as an adjunct to
     * bug reporting or other useful information collection.
     * @param ev
     */
    public void SendNote(String ev)
    {  	ConnectionManager myNetConn = (ConnectionManager)sharedInfo.get(NETCONN);
        if(myNetConn!=null) 
        	{ myNetConn.na.getLock();
        	  String seq = "";
        	  if(myNetConn.hasSequence) { seq = "x"+myNetConn.na.seq++ + " "; }
        	  myNetConn.count(1);
        	  myNetConn.sendMessage(seq+NetConn.SEND_NOTE + ev); 
        	  myNetConn.na.Unlock();
        	}
    }
    /**
     * add a string to the sequential communications log.  This will appear in sequence
     * if the communications log is dumped as part of an error report.  The intention of
     * this is to note events of interest correlated to the sequence of communications.
     * @param ev
     */
    private void LogMessage(String ev)
    { ConnectionManager myNetConn = (ConnectionManager)sharedInfo.get(NETCONN);
      if(myNetConn!=null) { myNetConn.LogMessage(ev); }
    }
    
    private void LogMessage(String ev,Object ...od)
    { ConnectionManager myNetConn = (ConnectionManager)sharedInfo.get(NETCONN);
      if(myNetConn!=null) { myNetConn.LogMessage(ev,od); }
    }
    /** draw a mouse sprite for a particular player, and if there is a moving
     * object, draw a representation of the object for the player in control.
     * @param g
     * @param p
     */
    private void drawMouseSprite(Graphics g,commonPlayer p)
    {	//
    	// our own mouse will have mouseX and mouseY = -1 so they won't be tracked here.
    	//
    	if ((p != null) && l.mouseCheckbox.getState())
    	{ int mx = p.mouseX;
    	  int my = p.mouseY;
    	  long now = G.Date();
    	Point mp = decodeScreenZone(p.mouseZone,p.mouseX,p.mouseY);
        int xp = G.Left(mp);
        int yp = G.Top(mp);
        
        if (fullRect.contains(xp, yp))
        {
            int col = p.boardIndex; //assign colors to players
            int obj = p.mouseObj;
            String mode = p.mouseType; 
            if(mode!=null)
            {
            	drawAnnotationSprite(g,obj,xp,yp,mode);
            }
            else if ((obj >= 0) && !reviewMode())
                { // draw an object being dragged
                  drawSprite(g,obj,xp,yp);
                }
            
      	  // only draw the mouse sprite if it has moved or for 1/2 second after
          // this prevents one user accidentally (or deliberately) blocking
          // the view by hovering his mouse over something.
      	  if((mx>=0) 
      		&& (my>=0)
      		&& ((mx!=p.drawnMouseX)
      			||(my!=p.drawnMouseY)
      			|| ((now-p.drawnMouseTime)<500)))
      	  {
              p.drawnMouseX = mx;		// remember when and where
              p.drawnMouseY = my;
              p.drawnMouseTime = now;

            drawMouseSprite(g,col,xp,yp);
        }
    	}}
	}
    
    /** return the position to display "moving" objects in review mode, when
     * they are not actively being moved.  The default is near the lower right
     * corner of the board.
     */
    public Point spriteDisplayPoint()
    {	BoardProtocol b = getBoard();
    	int celloff = b.cellSize();
    	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    }
    /**
     * select the actual chat window height, given the height of the window.  This
     * method can be overridden to alter the choices within reasonable bounds.
     * 
     * @param height
     * @return the height to assign to the chat window
     */
    public int selectChatHeight(int height)
    {	if(chatFramed) { return(0); }
    	if(hidden.separateChat || hidden.hiddenChat) { return(0); }
    	int nominal = hidden.chatSizeAdj+(height*chatPercent)/100;
    	int fs = G.getFontSize(standardPlainFont());
    	int fh = fs*14;		// minimum lines, about 7
    	int fm = fs*20;		// maximum lines, about 10
    	int maxh = height*2/3;
        return(Math.min(Math.min(fm, maxh),
        				Math.max(Math.max(MINCHATHEIGHT,fh),
        						nominal)));
    }
    
    public void drawRotatedSprite(Graphics gc,int chip,HitPoint hp,Point pt)
    {	
    	HiddenGameWindow hidden = findHiddenWindow(hp);
    	int left = G.Left(pt);
    	int top = G.Top(pt);
    	if((hidden==null)&&(remoteViewer<0))
    	{
    	// rotate the sprite to face the current player
    	boolean drawn = false;
    	for(commonPlayer p : players)
    	{
    		if(p!=null && p.inPlayerBox(hp))
    		{
    			GC.setRotatedContext(gc,hp,p.displayRotation);
    			drawSprite(gc,chip,left,top);
    			GC.unsetRotatedContext(gc,hp);
    			drawn = true;
    		}
    	}
    	if(!drawn)
    	{
    	GC.setRotatedContext(gc,hp,contextRotation);
    	drawSprite(gc,chip,left,top);
    	GC.unsetRotatedContext(gc,hp);
    	}}
    	else {
    		// if this is a hidden window, there should be no rotation
    		drawSprite(gc,chip,left,top);
    	}
    }
    /**
     * return a reference size, nominally the size of a cell on the board
     * 
     * @return
     */
    public int cellSize() { return getBoard().cellSize(); }
    /**
     * draw any object being dragged by the mouse.  In review mode, where the mouse
     * is actually moving the arrow controls, display "moving" objects at "spriteDisplayPoint"
     * 
     * @param gc
     * @param hp
     */
    public boolean DrawTileSprite(Graphics gc,HitPoint hp)
    { 	//draw the ball being dragged
    	// draw a moving tile
    	boolean seen = false;
    	
     	if(mouseTrackingAvailable(hp)) {  drawTileSprite(gc,hp); }
    	int chip = getOurMovingObject(hp);
        if(reviewMode()) // in review mode we normally display moving pieces beside the vcr controls
          {	
          	if(!animating && chip>=0)
          	{
          	Point pt = spriteDisplayPoint();
          	drawRotatedSprite(gc,chip,hp,pt);
          	seen = true;
            }
          }
          else if( (hp!=null) && hasControlToken())
          { // using getMovingObject() gets the published value of the moving object,
        	// which is >0 only if we're the active player dragging the object.
            if (chip >= 0)
            {	
                drawRotatedSprite(gc,chip,hp,hp);
                seen = true;
            }
        }
        
        // handle annotation menu
    	commonMove m = getCurrentMove();   	
        if(m!=null) { annotationMenu.drawSavedAnimations(gc,m,cellSize()); }

        AnnotationMenu.Annotation selected = annotationMenu.getSelected();
    	if(selected!=null)
    	{

        if(seen) { annotationMenu.setSelected(null); }
    		else {
    		 annotationMenu.drawAnnotation(gc,selected,cellSize(),G.Left(hp),G.Top(hp));
    		 hp.hitCode = GameId.PlaceAnnotation;
    		 seen = true;
     		}
    	}
      
    	return(seen);	// didn't draw any
    }

    /**
     * return the current real move number.  This is used by the
     * game controller to tell when to ask for the mid game digest (at 20 moves)
     * and when to consider a game a game to be recorded (after 3 moves).
     */
    public int MoveStep()
    {
        return (History.viewMove);
    }
    
	/**
	 * this provides the number seen in the VCR slider.  
	 */ 
    private String CurrentMoveString()
    {	BoardProtocol b = getBoard();
        return ("" + (b==null ? 0 : b.moveNumber()));
    }
    /** this provides the game state, digested as a unique integer, for
     * the game controller for fraud detection (the fraud being to play the
     * same game over and over to inflate your score).  It's used at midgame
     * as well as endgame, to uniquely identify this game.
     */
    public long Digest()
    {	BoardProtocol b = getBoard();
        return (b==null ? 0 : b.Digest());
    }

    /** this is used by the game controller to supply entertainment/progress strings to the lobby.  The usual format is the move number, 
     * optionally followed by 1 non-spaced string per player representing the score.
     * A more general format is also available, where the first token is x64 and the remaining tokens are encoded with
     * lib.Base64.encode(str) to allow arbitrary spaced and formatting.
     * */
    public String gameProgressString()
    {
        return ((mutable_game_record ? Reviewing : ("" + History.viewMove)));
    }
    public void addLocalPlayer(LaunchUser u)
    {
    	l.localPlayers.push(u);
    }
    /**
     * return true if the player to move now is one of the players
     * using this gui
     * @return true if this player is using the same gui
     */
    private boolean isALocalPlayer(commonPlayer lp)
    { 	if(lp!=null && (remoteViewer<0) && !isTurnBasedGame())
    	{
    	if(isOfflineGame() || lp.isProxyPlayer) { return(true); } 
    	}
    	return(false);
    }
    
    /**
     * return true if the currently active player is a spectator in a live game
     * 
     * @return
     */
    boolean canUseDone = false;
    public boolean canUseDone() 
    {	if(canUseDone || reviewOnly) { return true; }
     	return !isSpectator();
    }
    public boolean isSpectator()
    {
    	commonPlayer p = getActivePlayer();
    	return p!=null && p.isSpectator(); 
    }
    /**
     * true if we're not in review mode and we're the active player
     * @return true if it is our move as a player, including simultaneous moves by players
     */
    public boolean ourActiveMove()
    {	commonPlayer ap = getActivePlayer();
    	commonPlayer who = currentGuiPlayer();
    	return(reviewOnly
    			|| (!reviewMode()
    			&& (ap!=null)
    			&& !ap.isSpectator() 
    			&& (who!=null)
    			&& (simultaneousTurnsAllowed()
    					|| isALocalPlayer(who)
    					|| (who==ap)
    					|| (who.boardIndex == ap.boardIndex))));
    }
    
     /** return true of we are allowed to make move-type gestures
     * that may affect the state of the game. This includes consideration
     * if if a game is in progress, if we are a player, and if the game
     * is active, temporarily in review, or permanently in review.
     * @return true if we can make moves
     */
    public boolean OurMove()
    {	if(!reviewOnly && !started) { return(false); }
        return (allowed_to_edit
        		? hasControlToken()
        		: ourActiveMove());
    }
    /**
     *  return true if w is not a hidden player
     *  or w is a hidden player and it is their move
     * @param w
     * @return
     */ 
    public boolean hiddenPlayerOrMainMove(HiddenGameWindow w)
    {
     	return((w==null) || (w.getIndex()==currentGuiPlayer().boardIndex));
    }
    /**
     * @return true of we are allowed to scroll the game record.
     */
    private boolean allowedToScroll()
    {	return(allowed_to_edit ? hasControlToken() : true);
    }

    /**
     * this is used by the game class, not locally in the UI,
     * to get a token to tack onto the mouse coordinates.  By
     * convention, positive integers indicate trackable objects
     * 
     */ 
    public int getOurMovingObject(HitPoint highlight)
    {
        if (OurMove())
        {
            return (getBoard().movingObjectIndex());
        }
        return (NothingMoving);
    }
    
    /**
     * this is used by the game class, not locally in the UI,
     * to get a token to tack onto the mouse coordinates.  By
     * convention, positive integers indicate trackable objects
     * this retrieves the public identity of the moving object
     * in cases where censorship might be in effect
     */ 
    public int getMovingObject(HitPoint hp)
    {
    	return(getOurMovingObject(hp));
    }
    
    public boolean hasMovingObject(HitPoint hp)
    {	
    	return(getOurMovingObject(hp)>=0);
    }
    /** get the board object associated with the game.
     * 
     * @return the main game board
     */
    public abstract BoardProtocol getBoard();
    /**
     * redraw the board now.  This is where repainting actually
     * happens, also where the mouse is tracked and we decide
     * what object it points to.
     * @param gc
     * @param p
     */
    public abstract void redrawBoard(Graphics gc,HitPoint p);

    // 
    public void redrawClientBoard(Graphics gc,HitPoint p)
    {	// redrawboard may as a side effect set usingCopyBoard
    	// when it calls disB.  This prevents the new board
    	// from being swapped in until usingCopyBoard is null
    	Thread current = Thread.currentThread();
    	Thread prev = l.displayThread;
    	//recursion is normal if in touch magnifier
    	G.Assert(gc==null || ( prev==null)
    				, "multi threaded! %s and now %s",prev,current);
    		
    	G.Assert(!G.debug() || G.isEdt(), "not edt!");
    	try {
    		if(gc!=null) { l.displayThread = current; }
    		
    		if(remoteViewer>=0)
    			{ 
    			// if we are a remote viewer, skip the redrawBoard and go
    			// directly to the hidden window drawing
    			drawHiddenWindows(gc, p); 
    			}
    		else
    		{
    			redrawBoard(gc,p);
    			doFlashAnimation(gc);
    		}
    	if(l.gameOverlayEnabled) { drawGameOverlay(gc,p); }
    }
    	finally {
    		Thread later = l.displayThread;
    		if(gc!=null)
    		{
    		G.Assert(later==current,"changed from "+current+" to "+later);
    		l.displayThread = null;
    		}
    	}
   }
    private commonPlayer otherPlayerTimeExpired(commonPlayer my)
    {	my.setTimeIsInactive(false);
    	for(int i = 0;i<nPlayers();i++)
    	{	if(allPlayersLocal() ? (i!=currentGuiPlayer().boardIndex) : (i!=my.boardIndex))
    		{
    		commonPlayer p = getPlayerOrTemp(i);
    		if(p!=null)
    		{
    		// note that this time is not actively changing
    		p.setTimeIsInactive(true);
    		if(timeExpired(p)) { return(p); }
    		}}
    	}
    	return(null);
    }
    
    
    // true if this player's time has expired
    private boolean timeExpired(commonPlayer pl)
    {	return(timeRemaining(pl)<0);
    }
    public int timeRemaining(int pl)
    {
    	return(timeRemaining(getPlayerOrTemp(pl)));
    }
    private boolean principleTimeExpired(commonPlayer pl)
    {
    	boolean review = reviewMode();
    	long ptime = review ? pl.reviewTime : pl.elapsedTime;
    	return timeControl.timeRemaining(ptime,-1)<0;
    }
    /**
     * return this player's time remaining
     * @param pl
     * @return
     */
    private int timeRemaining(commonPlayer pl)
    {	boolean review = reviewMode();
    	long ptime = review ? pl.reviewTime : pl.elapsedTime;
    	switch(timeControl.kind)
    	{
    	default:	G.Error("Case not handled");
    	case None:		
    	case Fixed:
    		return(timeControl.timeRemaining(ptime,0));
    		
    	case Incremental:
    		{
       		int nmoves = hidden.numberOfOvertimeMoves(pl);
    		int rem = timeControl.timeRemaining(ptime,timeControl.moveIncrement*nmoves);
    		return rem;
    		}
    	case Differential:
    		{
    		long mintime = ptime;
     		for(commonPlayer p : players)
	    	{
	    		if(p!=null) 
	    			{ long reviewTime = review
							? p.reviewTime 
							: p.timeWhenLastInactive();
	    			  mintime = Math.min(mintime,reviewTime); 
	    			}
	    	}

    		return(timeControl.timeRemaining(ptime,ptime-mintime));
    		}
    		
    	case PlusTime:
    		{
    		int nmoves = hidden.numberOfCompletedMoves(pl);
    		return(timeControl.timeRemaining(ptime,timeControl.moveIncrement*nmoves));
    		}
    	}
    }
    /**
     * return true if this is a multiplayer timed game, and our time has expired.
     * if true, the next step will be to call autoPass();
     * @return true if time has expired and we should autoPass next
     */
    private boolean ourTimeIsExpired()
    {
    	return (ourActiveMove()
    			&& (nPlayers()>2)
    			&& timeExpired(getActivePlayer()));
    }
    /**
     * undo any partial moves, then pass and invoke done.
     * this is normally used to pass when time has expired in timed games.
     */
    public void autoPass()
    {
    	BoardProtocol b = getBoard();
    	if(b.DoneState())
    		{
    			PerformAndTransmit(DONE);
    		}
    	else {
    		if(allowUndo())
    		{
    			PerformAndTransmit(UNDO); 
    		}
    	PerformAndTransmit(PASS);
    	PerformAndTransmit(DONE);
    	}
    }

	public boolean performStandardActions(TimeControl time,HitPoint hp,commonCanvas canvas)
	{	CellId id = hp.hitCode;
		if(id instanceof TimeId)
		{
		TimeId tid = (TimeId)id;
		int left = G.Left(hp);
		int top = G.Top(hp);
		if(time.handleMenus(tid,left,top,this,this)) { return true; }
		else
		switch(tid)
		{	case ChangeTimeControl:
			case GameOverOnTime:
	  			canvas.performClientMessage("+" + tid.name(),true,true);
	  			return(true);
	
			default: G.Error("not handled %s", tid);
		}}
		return(false);
	}

	/**
	 * this is used within a 2-player game when the time has expired.  It allows
	 * the game to be ended on the current terms, or for the time controls to 
	 * be changed.  This is called for the *winning* player, so no cooperation
	 * with the losing player is required.
	 * 
	 * @param gc
	 * @param hitPoint
	 * @param expired
	 * @param canvas
	 * @param boardRect
	 */
    public void drawGameOverlay(Graphics gc,TimeControl time,HitPoint hitPoint0,commonPlayer expired,commonCanvas canvas,Rectangle boardRect)
    {	InternationalStrings s = G.getTranslations();
    	commonPlayer who = canvas.currentGuiPlayer();
    	boolean robo = (who.robotRunner==expired);
    	HitPoint hitPoint = robo||canvas.ourActiveMove() ? hitPoint0 : null;
    	if(hitPoint!=null) { hitPoint.neutralize(); }
    	int left = G.Left(boardRect);
    	int top = G.Top(boardRect);
    	int width = G.Width(boardRect);
    	int inside = (int)(width*0.05);
       	int h = inside*7;
    	int l =left+inside;
    	int t = top+(G.Height(boardRect)-h)/2;
    	int w = width-inside*2;
     	Rectangle ir = new Rectangle(l,t,w,h);
    	StockArt.Scrim.getImage().stretchImage(gc, ir);  
    	GC.setFont(gc,canvas.largeBoldFont());
    	String pname = expired.prettyName("");
    	String banner = s.get(TimeExpiredMessage,pname);
    	String endgame = s.get(TimeEndGameMessage,pname);
    	String changetime = s.get(ChangeLimitsMessage);
    	GC.Text(gc,true,l,t,w,inside*2,Color.black,null,banner);
    	
    	if(GC.handleSquareButton(gc, new Rectangle(l+inside*2,t+inside*2,w-inside*4,inside), hitPoint, endgame,
    			bsBlue,Color.lightGray))
    	{
    		hitPoint.hitCode = TimeId.GameOverOnTime;
    	}
    	if(GC.handleSquareButton(gc, new Rectangle(l+inside*2,t+(int)(inside*3.5),w-inside*4,inside), hitPoint, changetime,
    			bsBlue,Color.lightGray))
    	{
    		hitPoint.hitCode = TimeId.ChangeTimeControl;
    	}
    	Rectangle timeControlRect = new Rectangle(l+w/6,t+inside*5,4*w/6,inside);
    	time.drawTimeControl(gc,canvas,hitPoint!=null,hitPoint,timeControlRect);
    	GC.frameRect(gc,Color.black,ir);
    }

    /**
     * in 2 player games with time controls, this will draw an overlay
     * on the board if the opponents time has expired.  The overlay lets
     * you claim the win or change the time controls for the game.  
     * 
     * @param gc
     * @param p
     */
    public void drawGameOverlay(Graphics gc,HitPoint p)
    {	boolean drawing = false;
    	BoardProtocol b = getBoard();
    	if(b!=null 
    			&& (b.nPlayers()==2) 
    			&& !b.GameOver() 
    			&& !reviewMode()
    			&& !mutable_game_record)
    	{	commonPlayer my = currentGuiPlayer();
    		if(my!=null)
    		{ 	
    			commonPlayer expired = otherPlayerTimeExpired(my);
    			if(expired!=null)
    			  { // we don't want the clock to tick while the player considers endgame
    				my.setElapsedTimeFrozen(true);
    				drawing = true;
    				drawGameOverlay(gc,futureTimeControl,p,expired,this,boardRect);
    			  }
    			else
    			{ my.setElapsedTimeFrozen(false);
    			  
    			}
    		}
    	}
    	if(!drawing) { if(ourTimeIsExpired()) { autoPass(); }}
    	l.drawingGameOverlay = drawing;
    }

    /** return the player whose turn it really is.  This is used by the game controller
     * to key sounds and other per player turn actions.  This does not change if the 
     * player goes into review mode.
     */
    public commonPlayer currentGuiPlayer()
    {	commonPlayer p = reviewMode() ? History.viewTurn : null;
    	if(p==null) { p = viewerWhoseTurn(); }
    	if(p==null) { p = getPlayerOrTemp(0); }	// during game setup and other rare circumstances
    	return(p);
    }
    public commonPlayer viewerWhoseTurn()
    {
    	return(commonPlayer.findPlayerByIndex(players, getBoard().whoseTurn()));
    }
    // award control anyway after 2 seconds

    /**
     * true if we have the control token.  This will claim the token
     * if it is expected but delayed by 2 seconds.
     * @return true of we are in control and can click on the board
     */
    private boolean hasControlToken()
    {	// say we have control if we're not allowed to edit.
    	return(allowed_to_edit?(isOfflineGame()||hidden.controlToken||hidden.pendingControlExpired()||inLimbo):true);
    }
    /**
     * we have the control token or are expecting it. This is used by the game
     * controller to decide if it should request the token.
     */
    public boolean hasControlTokenOrPending()
    {	boolean has = hasControlToken() || (hidden.controlTimeStamp>0);
    	return(has);
    }
    /**
     * true if we have the control token.  This should be used during
     * normal play, when operations are strictly synchronous.
     */
    public boolean hasExplicitControlToken()
    {
    	return(hidden.controlToken);
    }
    /** set ownership of the control token, with the timestamp if it's pending
     * 
     * false,0 means we are relinquishing control
     * false,nnn means we have requested control
     * true,nnn means we have received acknowlegement of our claim of control
     * 
     * if nnn isn't the same as controlToken, then there is a control fight
     * going on and we shouldn't claim control even if the val is true
     * 
     * The original communications to support this used client level message, either
     * CONTROL or NOCONTROL to signal who was permitted to make moves.  This proved
     * to be unreliable because of real-time lags.
     * 
     * The new protocol uses a server level "lock" request, which depends on
     * the clients relinquishing the lock in a timely manner.
     */
    public void setControlToken(boolean val,long timeStamp)
    {	//G.print("Set "+val+" "+getActivePlayer());
    	//G.print(G.getStackTrace());
     	if(val && (timeStamp>0))
    	{
     		hidden.controlToken=val; 
     		hidden.controlTimeStamp=timeStamp; 
    	}
    	else 
    	{
    	 hidden.controlTimeStamp = timeStamp;
    	 if(hidden.controlTimeStamp==0)
    	 	{ 
    	 	hidden.controlToken=val; 
     	 	}
    	}
    }
 /**
  * this is called by the game controller when the game is over
  * and scoring activity has completed.  It's now safe to let the
  * players edit and rehash the game.   
  * boolean andDone if true, can use the "done" button too
  */
    public void setEditable(boolean always)
    {	// a bit of a mess, turn based games that are viewed after completion
    	// should be editable like they were immediately after play
    	if(always) { canUseDone = true; }
    	if(always || canUseDone()) { allowed_to_edit = true; }
    	mutable_game_record = true;
    }
    /**
     * return true if the game has ended.
     */
    public boolean GameOver()
    { // if we are in gameover state, become a reviewer of the finished game
        return(gameOver);
    }
    /** 
      * @return true if the game is in a GameOverState state right now.
     */
    public boolean GameOverNow()
    {	BoardProtocol b = getBoard();
    	boolean now = (b==null) ? false : b.GameOver();
    	gameOver |= now;
    	return(now);	// game over right now?
    }
    private boolean isScored = false;
    public void setScored(boolean v) { isScored = v; }
    public boolean isScored() { return isScored; }
    /**
     * return true if the player is a winner of this game.  This is used in scoring,
     * there should be at most one winner in a 2 player game.
     */
    public boolean WinForPlayer(commonPlayer p)
    {	if(p==null) { return(false); }
    	BoardProtocol b = getBoard();
    	if(b==null) { return(false); }
    	if(p.boardIndex>=b.nPlayers()) { return(false); }
    	return(b.WinForPlayer(p.boardIndex));
    }
    /**
     * return a score for the player in a multiplayer game.  This ought to 
     * include any tie breaker differentiation, so may not be the score
     * the user expects to see.  The extra info will be packed by some
     * kind of Chinese remaindering. 
     */
    public int ScoreForPlayer(commonPlayer p)
    {	return getBoard().scoreForPlayer(p.boardIndex);
    }
   
    //
    // a less precise but suitable for viewing score. This
    // may not include tie break information
    public int PrettyScoreForPlayer(BoardProtocol gb, commonPlayer p)
    {
    	return(gb.scoreForPlayer(p.boardIndex));
    }
    
    public boolean UsingAutoma() { return(false); }
    public int ScoreForAutoma() { return(-1); }
    public String getUrlNotes() { return ""; }
    /** 
     * override this method to create an appropriate class of robot that implements RobotProtocol
     * @return a class instance of a new robot player for this game.
     */
    public abstract SimpleRobotProtocol newRobotPlayer();
    /** start the robot.  This is used to invoke a robot player.  Mainly this needs 
     * to know the class for the robot and any initialization it requires.
     *  */
    public commonPlayer startRobot(commonPlayer p,commonPlayer runner,Bot robo)
    {
        gameOver = false;
        if(robo!=null)
        {
        SimpleRobotProtocol rr = newRobotPlayer();
        if(rr!=null)
        {
        rr.InitRobot(this, sharedInfo, getBoard(), null, robo.idx);

        p.startRobot(rr,runner);
        if(G.debug()) { p.setPlayerName(robo.name+p.boardIndex,true,this); }
        return(p);
        }}
        G.infoBox("No Robot","Robot not specified yet"); 
        return(null);
        
    }
    /**
     * this is true when the "use aux sliders" menu item is in effect.
     */
    public boolean useAuxSliders = false;
    public DrawableImage<?> lastDropped=null;
    /**
     * this is called to adjust the x,y and scale associated with 
     * an object, using the auxiliary sliders.
     */
    public void adjustScales(double pscale[],DrawableImage<?> last)
    {	
    	l.adjustScales(pscale,last);
    }
    /**
     * this is available to adjust the scale parameters of a second object during developent.
     * an object, using the second auxiliary sliders.
     */
    public void adjustScales2(double pscale[],DrawableImage<?> last)
    {	l.adjustScales2(pscale,last);
    }
     
   
     /**
     * the vcr slider cluster uses these Ids.  Normally these should be ignored
     * by individual game UIs.
     * @author Ddyer
     *
     */
    private enum VcrId implements CellId {
        WayBackButton, // also -103 -102 -101
        BackPlayerButton,
        BackStepButton,
        ForeStepButton,
        ForePlayerButton,
        ForeMostButton,
        
        Slider, // the vcr slide bar
        BackButton, //the back variation button
        FrontButton, // the forward variation button
        VarButton, // select variation
        AnimateButton,	// animate button
        
    	sliderXButton,
    	sliderYButton,
    	sliderSButton,
    	sliderAnimSpeedButton,
    	sliderX2Button,
    	sliderY2Button,
    	sliderS2Button,

    	expandButton,
    	shrinkButton,
    	noVcrButton,
        ;    
    }
    public boolean use_grid = false; //maintained by this class
    public JCheckBoxMenuItem gridOption = null;
    private boolean useAlternateBoard = false;
    
    
    /**
     * place a sound and delay milliseconds until the next
     * @param name
     * @param delay
     */
    public void playASoundClip(String name,int delay)
    {	if(myFrame.doSound())
    	{ SoundManager.playASoundClip(name,delay);
    	}
    }
    
    private commonPlayer spectators[] = {};		// a null array, not null
    /** add a new spectator to the game, replacing the specified spectator.
     * this list is important mainly in review rooms, where all the participants
     * are spectators.
     * 
     */
    public void changeSpectatorList(commonPlayer p,commonPlayer replace)
    {	//G.print(""+my+" add "+p+" "+replace);
    	spectators = commonPlayer.changePlayerList(spectators,p,replace,true);
    	if(p!=null) { p.addObserver(this); }
    	if(reviewOnly && (theChat!=null))
    		{
    		int nc = numberOfConnections();
    		boolean hide = nc<=1;
    		theChat.setHideInputField(hide);
    		} 
     }

    public int numberOfConnections() 
    { 	int ns = 0;
    	for(commonPlayer p : spectators) { if(p!=null) { ns++; }}
    	return(ns+(reviewOnly?0:players.length)+((getActivePlayer().channel>0)?1:0)); }
    
    // list of player objects, list of time clock rectangles  
    public commonPlayer[] players = {null}; //shared with player
    private commonPlayer[] tempPlayers = new commonPlayer[Session.MAXPLAYERSPERGAME]; 
     /**
     * add a new player to the list of players in the game, replacing the
     * specified player.
     */
    public void changePlayerList(commonPlayer p,commonPlayer replace)
    {	commonPlayer newpl[] = commonPlayer.changePlayerList(players,p,replace,false);
    	if(newpl==null) 
    		{ throw G.Error("No empty slot for player %s players %s",p,AR.toString(players)); }
    	if(p!=null)
    	{	changeSpectatorList(null,p);	// remove from spectators list
    		p.addObserver(this);
    	}
    	commonPlayer.reorderPlayers(players);			// make sure the players are properly ordered before

    	resetBounds();
    }
/**
 * the player object which is "us", even if we are not a player but
 * a spectator.
 */
	public commonPlayer getActivePlayer() {
		return l.my;
	}
	public void setActivePlayer(commonPlayer mypl) {
		l.my = mypl;
	}
    private String leftBarMessage = null; // message dictated by the overall frame
    private Color leftBarMessageColor = null;
    public boolean inLimbo = false;	   // true if we've been disconnected
    /**
     * normally, the game record is considered immutable during a game, but
     * becomes mutable by the players after the game is over.  What starts
     * as a regular game is never mutable by spectators.   What starts as
     * a game review is mutable by everyone.
    */
    public boolean reviewOnly = false; 		// if everyone is permanantly a reviewer
    /**
     * true if we're allowed to make changes in the game record
     */
    public boolean allowed_to_edit = false; 
    /**
     * gameover seen at some time
     */
    public boolean gameOver = false;
    /**
     * true if the game record is generally mutable by someone.  The distinction between
     * this and allowed_to_edit is that spectators after a game are not allowed to edit.
     */
    public boolean mutable_game_record = false;	
    /**
     * true if a game record has actually been branched
     */
    public boolean mutated_game_record = false;	
    /**
     * this is the official move history, as edited by editHistory.
     * This is used to display the game log, to replay the game,
     * and to produce .sgf game records.
     */
    public CommonMoveStack  History = new CommonMoveStack();	 
    
    public GameLog gameLog = new GameLog(this);
    
    /** this is the unedited, annotated history, which is kept to
     * help debug errors in the editing process.
     */
    private CommonMoveStack  rawHistory = new CommonMoveStack(); // unedited history
    /**
     * this is a collection of digests for the game
     * at various points, looking for 3 repetitions.
     */
    public RepeatedPositions repeatedPositions = new RepeatedPositions();
    
    /**
     * current index, always points to the piece that
     */
    public int jointReviewStep = -2; //the last transmitted review position
/*
     * a standard rectangle for the vcr control
     */
    public Rectangle vcrZone = addZoneRect("vcrZone");
    public Rectangle vcrRect = addRect(".vcrRect");
    private commonMove commentedMove = null;
    private commonMove commentedMoveSeen = null;


    /**
     * locate a player which may be an active robot.  This is used in the user interface
     * to attach auxiliary display boards and also to find candidates to start, stop, or return moves.
     * THe default will always return the current player, but in games with simultaneous actions, 
     * this method can be overridden to return a non-current player which is a robot.
     * @return a player (which might or might not be a robot)
     */
    public commonPlayer currentRobotPlayer()
    {
    	commonPlayer who = currentGuiPlayer();
    	commonPlayer act = getActivePlayer();
    	if(who==null) { who=act; }
    	return(who);
    }
    static int NoRemoteViewer = -999;
    public int remoteViewer = NoRemoteViewer;
    public void setRemoteViewer(int n) 
    { remoteViewer = n; 
      if(n>=0) 
      	{ commonPlayer m = getPlayerOrTemp(n);
     	  setActivePlayer(m);
      	}
    }
    
    public void addEvent(String str)
    {
    	if(isOfflineGame()) 
		{
			setChanged(str);
  		}
    	else  {
    		super.addEvent(str);
    	}  	
    }

/** this is called by the game controller when all players have connected
 * and the first player is about to be allowed to make his first move. This
 * may be a new game, or a game being restored, or a player rejoining a game.
 * You can override or encapsulate this method.
 */
    public void startPlaying()
    {	setUserMessage(Color.black,null);
    	if(getBoard().checkClientRevision())
    	{	G.print(G.uniqueName()+" back and forth");
    		doWayBack(replayMode.Replay);
    		doScrollTo(FORWARD_TO_END);
    	}
    	for(commonPlayer p : players)
    	{
    		if(p!=null) { p.setTimeIsInactive(true); }
    	}
    	saveDisplayBoard();
        if((hidden.resignAction==null) && canUseDone()) 
        { hidden.resignAction = myFrame.addAction(s.get(RESIGN),deferredEvents);   
          hidden.offerDrawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);     
 	   	}
    	resetBounds();
    	wake();
    	startedOnce = started = true;
    }

    private void doMouseTrackingInternal(commonPlayer player,commonPlayer[] plist,String zone,int inx,int iny,int ino,String intype)
    {   for(int pl = 0;pl<plist.length;pl++)
        {
        // make only one player the tracking player
        commonPlayer p = plist[pl];
        if(p==player) 
        { if(player!=getActivePlayer())
        	{ player.mouseTracking(zone,inx, iny, ino,intype);
        	}
        }
        else if((p!=null)&&(ino!=NothingMoving)) 
        	{ p.mouseTrackingObject(NothingMoving); 
        	}
        }
    }
    
    public void doMouseTracking(StringTokenizer myST,commonPlayer player)
    {	
        String zone = myST.nextToken();
        int inx = G.IntToken(myST);
        int iny = G.IntToken(myST);
        int ino = G.IntToken(myST);
        String typ = myST.hasMoreTokens() ? myST.nextToken() : null;
        doMouseTracking(player,zone,inx,iny,ino,typ);
     }

    private void doMouseTracking(commonPlayer player,String zone,int inx,int iny,int ino,String intype)
    {
    	doMouseTrackingInternal(player,players,zone,inx,iny,ino,intype);
    	doMouseTrackingInternal(player,spectators,zone,inx,iny,ino,intype);
     }
    /**
     * draw the players time clocks, using the standard black for the main clock
     * and blue for differential time.
     * @param gc
     */
    public void drawMice(Graphics gc)
    {
        for(commonPlayer p : players)
        {
            if(p!=null)
            {
            drawMouseSprite(gc, p);
            }
        }
       if(reviewOnly || mutable_game_record)
       {
    	 for(commonPlayer pl : spectators)
    	 {	
    	 	if(pl!=null)
    	 	{
   	 		drawMouseSprite(gc, pl);
    	 	}
    	 }
       }
    }

/** change or remove the user message
 * 
 */
    public void setUserMessage(Color color, String message)
    {
        if (((message != null) && !message.equals(leftBarMessage)) ||
                ((message == null) && (leftBarMessage != null)))
        {
            leftBarMessage = message;
            leftBarMessageColor = color;
            repaint(20);
        }
    }
/** set to true if the game controller is disconnected
 * 
 */
    public void setLimbo(boolean v)
    {
        inLimbo = v;
        if(v)
        	{ started = false; 
        	}
    }

    /** move the view of the game history back to a particular step.  The default inplementation
     * does this the simplest and dumbest way, by calling "doWayBack()" and then stepping
     * forward. For most circumstances this should be fine.  You can override this method
     * with a fancier or smarter UNDO if needed.
     */
    public void UndoStep(int target)
    {
        doWayBack(replayMode.Replay);
        while ((History.viewStep!=-1) 
        		&& (History.viewStep < target)
        		&& doForwardStep(replayMode.Replay1))
        {
        }
        //showComments();
    }

    /** move the view of the game history all the way back to the beginning.
      * This could be implemented cleverly, but the simple and reliable method 
      * is to just reinialize the board.
     */
     public void doWayBack(replayMode replay)
    {	int sz = History.size();
        if (sz > 0)
        {   hidden.enterReviewMode();
        	if(replay!=replayMode.Replay)
        		{ showComments(); 	// save modified comments as a side effect
                commentedMove = commentedMoveSeen = null;
                if(reviewOnly && theChat!=null) { theChat.clearMessages(true); }
        		}
            doInit(true);	// reinitialize
            // showComments();
            History.viewStep = 0;	// position 0
            hidden.doForward(replay);	// the history is seeded with a start command
        }
    }
     /**
      * scroll backward to where the current player has a choice 
      */
    private void doBackwardBranch()
    {
    	int ind = History.viewStep-1;
       	if(ind<0) { ind = History.size(); }
       	while(--ind>0)
       	{
       		commonMove m = History.elementAt(ind);
       		if(m.nVariations()>1) 
       			{ break; 
       			}
       	}
       	doScrollTo(ind+1);
    }
    
    private void doForwardBranch()
    {
    	// scroll forward to the next branch
        while (hidden.doForward(replayMode.Replay1))
        {
            int pos = getReviewPosition();

            if (pos <= 0)
            {
                break;
            }

            commonMove m = History.elementAt(pos - 1);

            if (m.nVariations() > 1)
            {
                break;
            }
        }
    }
/**
 * set up the VCR cluster.  This rectangle should have a 2:1 aspect ratio.
 * the rectangle vcrZone will be set to this rectangle, and various internal
 * rectangles will be defined.
 * 
 * @param x
 * @param y
 * @param w
 * @param h
 */
    public void SetupVcrRects(int x,int y,int w,int h)
    {	if(boardMax) { setupVcrRects(0,0,0,0,false); }
    	else { setupVcrRects(x,y,w,h,false); }
    }
    private void setupVcrRects(int x,int y,int w,int h,boolean expanded)
    {
    	hidden.vcrExpanded = expanded;
    	int border = w/10;
    	int b2 = border/2;
        int h1 = (h - border) / 3;
        int btop = (h-3*h1-b2);
        // set up aux sliders too
        G.SetRect(vcrZone,x,y,w,h);
        if(!expanded) { G.SetRect(hidden.normalVcrRect,x,y,w,h); }
        G.SetRect(hidden.animationSpeedRect,G.Left(vcrZone)+border,G.Top(vcrZone)+border,w-border*2,h/2-border);
        G.SetRect(l.auxXRect,x+b2,y+btop,w-border,(int)(h1*0.8));
        G.SetRect(l.auxYRect,x+b2,y+btop+h1,w-border,(int)(h1*0.8));
        G.SetRect(l.auxSRect,x+b2,y+btop+h1*2,w-border,(int)(h1*0.8));

        G.SetRect(l.auxX2Rect,x+w+b2,y+btop,w-border,(int)(h1*0.8));
        G.SetRect(l.auxY2Rect,x+w+b2,y+btop+h1,w-border,(int)(h1*0.8));
        G.SetRect(l.auxS2Rect,x+w+b2,y+btop+h1*2,w-border,(int)(h1*0.8));
        int vtop =y+btop+2 + (h1 * 2);
        G.SetRect(vcrRect,x+b2, vtop,w-border,Math.min(h1,h-(y+h-vtop)));
        G.SetRect(hidden.vcrButtonRect,G.Left(vcrRect),G.Top(vcrRect) - G.Height(vcrRect),G.Width(vcrRect),G.Height(vcrRect));
        G.SetRect(hidden.vcrBackRect,G.Left(hidden.vcrButtonRect),
        			G.Top(hidden.vcrButtonRect) - G.Height(hidden.vcrButtonRect) - G.Height(hidden.vcrButtonRect)/6,
        			G.Width(hidden.vcrButtonRect) / 5,
        			G.Height(hidden.vcrButtonRect));
        G.SetRect(hidden.vcrVarRect,G.Right(hidden.vcrBackRect),G.Top(hidden.vcrBackRect),G.Width(vcrRect) - (2 * G.Width(hidden.vcrBackRect)),h1);
   
        G.SetRect(hidden.vcrFrontRect,G.Right(hidden.vcrVarRect),G.Top(hidden.vcrBackRect),
        		G.Width(hidden.vcrBackRect),G.Height(hidden.vcrBackRect));
        		
    }


/** draw the slider button in the standard VCR group, and/or notice if the
 * mouse is in the slider.
 * 
 * @param p					the mouse position or null
 * @param inG				the graphics or null
 * @return the hit object code
 * 
 */
  public CellId drawVcrSlider(HitPoint p, Graphics inG)
    {	//G.Assert(Thread.currentThread()==runThread,"running in the wrong thread");
        Rectangle r = vcrRect;
        int sliderWidth = G.Width(r)-G.Width(r)/6;
        CellId retval = null;
        HitPoint dp = getDragPoint();
        boolean inDrag = (dp!=null) && (dp.hitCode==VcrId.Slider);
        boolean inSlider = (p == null) 
        				? false 
        				: (p.dragging 
        						? (inDrag || (((p.hitCode==DefaultId.HitNoWhere) ||(p.hitCode==VcrId.Slider)) && G.pointInRect(p,G.Left(r),G.Top(r),sliderWidth,G.Height(r))))
        					    : ( ((p.hitCode==DefaultId.HitNoWhere) ||(p.hitCode==VcrId.Slider)) && 
        					   		G.pointInRect(p,G.Left(r),G.Top(r),sliderWidth,G.Height(r))));
        int barHeight = G.Height(r);
        int buttonWidth = barHeight+barHeight/3;
        int hsize = History.size();
        int size = Math.max(hsize + 10, 50);
        double step = (double)(sliderWidth-G.Width(r)/12)/ size;

        if (inG != null)
        {
            int sliderPos =  History.sliderPosition>=0 
            					? History.sliderPosition
            					: (History.viewStep < 0) ? hsize : History.viewStep;

            //System.out.println("Size "+hsize+" Step "+step);
            if (inSlider && p.dragging && (p.hitCode==VcrId.Slider))
            {
                sliderPos = (int) Math.max(0,
                        Math.min(hsize-1, (G.Left(p) - G.Left(r)) / step));
            }
            commonMove m = (sliderPos >= hsize) ? null
                                                : (commonMove) History.elementAt(sliderPos);
            String str = (m == null) ? CurrentMoveString() : m.getSliderNumString();
            int sliderVal = (int) ( (step * sliderPos));
            {
            int w = sliderWidth;
            int h = G.Height(r);
            int xp = G.Left(r)+w/2;
            int yp = G.Top(r)+h/2;
            StockArt.VCRBar.drawChip(inG,this,w,xp,yp,null);
            }
            {
            int w = buttonWidth;
            int h = G.Height(r);
            int xp = G.Left(r) + sliderVal+w/2;
            int yp = G.Top(r) + h/2;
            StockArt.VCRTick.drawChip(inG,this,w+(inSlider?w/3:0),xp,yp,null);
            GC.drawOutlinedText(inG,true,xp-w/2,yp-w/2,w,w,Color.white,Color.black,str);
            }
         }
        if (inSlider)
        {
            p.hitCode = retval = VcrId.Slider;
            p.dragging = p.down;
            if(p.down)
            	{ History.sliderPosition = nextSliderPosition(p); 
            	}
        }
        if(G.isCodename1()
        		&& !hidden.vcrExpanded
        		&& (buttonWidth<G.minimumFeatureSize())
        		&& G.pointInRect(p,vcrZone))
        {
        	retval = VcrId.expandButton;
        }
        else if(hidden.vcrExpanded
        		&& !G.pointInRect(p,vcrZone)
        		&& G.pointInRect(p,hidden.outsideVcrRect))
        {
        	retval = VcrId.shrinkButton;
        }
        else {
        int w = G.Width(r)/6;
        int h = G.Height(r);
        int xp = G.Left(r)+sliderWidth+w*2/3;
        int yp = G.Top(r) + h/2;
        boolean inrect = G.pointInRect(p,xp-w/2,yp-h/2,w,h);
        if(inrect) { retval = VcrId.AnimateButton; }
        (animating?StockArt.VCRStop:StockArt.VCRPlay).drawChip(inG,this,w+(inrect?w/3:0),xp,yp,null);
        }
        return (retval);
    }
  
  public boolean missedOneClick = false;
  /**
   * call this to do the system standard actions that may be associated
   * with HitPoint.  These include scrolling actions generated by the
   * vcr controls, and "edit" and "done" buttons which use the default ids
   * and so on.  If "miss" is true, and the action is "HitNoWhere" then
   * performReset is called.
   * 
   * The "miss" functionality provides a "miss one click, do nothing, miss two clicks, reset" paradigm
   * @param hp
   * @return true if the reset action was not armed, but should be armed next time
   */
  public boolean performStandardActions
  	(HitPoint hp,	// @parm the HitPoint associated with the action
  	boolean miss)	// @parm if true, reset is enabled.
  {		CellId id = hp.hitCode;
  		boolean armed = false;
	  	if(performStandardButtons(id, hp))  { }
	  	else if(id == DefaultId.HitNoWhere) { if(miss) { performReset(); } else { armed = true;  }} 
	  	else if(performVcrButton(id, hp)) {  }
	  	else if(id ==GameId.HitNothing) { }
	  	else if(performStandardActions(futureTimeControl,hp,this)) { }
	  	else 
	  	{	// this should only occur if there's a bug in the
	  		// standard widgets used in the UI
	  		throw G.Error("Hit unknown object %s",hp);
	    }
	  	return(armed);
  }
	
	public boolean performMessage(TimeControl time,String op,String rest)
	   {
		   TimeId v = TimeId.find(op);
		   if(v!=null)
		   {
		   switch(v)
		   {
		   case ChangeFutureTimeControl:
			   TimeControl tc = TimeControl.parse(rest);
			   time.copyFrom(tc);
			   return(true);
		   case GameOverOnTime:
			   doEndGameOnTime();
			   return(true);
		   case ChangeTimeControl:
			   adoptNewTimeControl();
			   return(true);
		   default: G.Error("Can't handle %s",v);
		   }}
		   return(false);
	   }
  /**
   * draw a standard "done" button
   * @param gc
   * @param r
   * @param hit
   * @param highlightColor
   * @param backgroundColor
   * @return true if the button is hit
   */
  public boolean handleDoneButton(Graphics gc,Rectangle r,HitPoint hit,Color highlightColor,Color backgroundColor)
  {		return handleDoneButton(gc,0,r,hit,highlightColor,backgroundColor);
  }
  /**
   * draw a standard "done" button
   * 
   * @param gc
   * @param rotation
   * @param r
   * @param hit
   * @param highlightColor
   * @param backgroundColor
   * @return true if the button is hit
   */
  public boolean handleDoneButton(Graphics gc,double rotation,Rectangle r,HitPoint hit,Color highlightColor,Color backgroundColor)
  {
	  //StockArt icon = p==null?StockArt.OffLight:StockArt.GreenLight;
	  //return(icon.drawChip(gc,this,r,p,GameId.HitDoneButton,s.get(ExplainDone)));
	  if(GC.handleRoundButton(gc, rotation,r,hit,s.get(DoneAction),highlightColor, backgroundColor))
	  {	  hit.hitCode = GameId.HitDoneButton;
		  HitPoint.setHelpText(hit,r,s.get(ExplainDone));
		  return(true);
	  }
	  return(false);
  }
  /**
   * draw a standard "undo" button
   * 
   * @param gc
   * @param r
   * @param hit
   * @param highlightColor
   * @param backgroundColor
   * @return true if the button is hit
   */
  public boolean handleUndoButton(Graphics gc,Rectangle r,HitPoint hit,Color highlightColor,Color backgroundColor)
  {		if(allowUndo())
	  	{return handleUndoButton(gc,0,r,hit,highlightColor,backgroundColor);
	  	}
  		return(false);
  }
  /**
   * draw a standard "undo" button
   * 
   * @param gc
   * @param rotation
   * @param r
   * @param hit
   * @param highlightColor
   * @param backgroundColor
   * @return true if the button is hit
   */
  public boolean handleUndoButton(Graphics gc,double rotation,Rectangle r,HitPoint hit,Color highlightColor,Color backgroundColor)
  {
	  //StockArt icon = p==null?StockArt.OffLight:StockArt.GreenLight;
	  //return(icon.drawChip(gc,this,r,p,GameId.HitDoneButton,s.get(ExplainDone)));
	  if(GC.handleRoundButton(gc, rotation,r,hit,s.get(UndoAction),highlightColor, backgroundColor))
	  {	  hit.hitCode = GameId.HitUndoButton;
		  HitPoint.setHelpText(hit,r,s.get(ExplainUndo));
		  return(true);
	  }
	  return(false);
  }
  /**
   * draw a "please undo" button
   * 
   * @param gc
   * @param rotation
   * @param r
   * @param hit
   * @param highlightColor
   * @param backgroundColor
   * @return true if the button is hit
   */
  public boolean handlePleaseUndoButton(Graphics gc,double rotation,Rectangle r,HitPoint hit,Color highlightColor,Color backgroundColor)
  {
	  //StockArt icon = p==null?StockArt.OffLight:StockArt.GreenLight;
	  //return(icon.drawChip(gc,this,r,p,GameId.HitDoneButton,s.get(ExplainDone)));
	  if(GC.handleRoundButton(gc, rotation,r,hit,s.get(UndoAction),highlightColor, backgroundColor))
	  {	  hit.hitCode = GameId.HitPleaseUndoButton;
		  HitPoint.setHelpText(hit,r,s.get(ExplainPleaseUndo));
		  return(true);
	  }
	  return(false);
  }
/**
 * draw a standard "edit" button
 * @param gc
 * @param r
 * @param hit
 * @param hitAny
 * @param highlightColor
 * @param backgroundColor
 * @return true if the button is hit
 */
  public boolean handleEditButton(Graphics gc,Rectangle r,HitPoint hit,HitPoint hitAny,Color highlightColor,Color backgroundColor)
  {
	  return handleEditButton(gc,0,r,hit,hitAny,highlightColor,backgroundColor);
  }
  /**
   * @return true if it is ok to undo a current "pick" operation.
   */
  public boolean allowBackwardStep()
  {	return somethingIsMoving();
  }
  /**
   * return true of it is ok to undo the most recent moves, and stop
   * at the most recent "done".
   * @return true if ok
   */
  public boolean allowPartialUndo()
  {		
	  	if(isPuzzleState() ) { return somethingIsMoving(); }

	    if(allowUndo())
  		{
	  	int op = getCurrentMoveOp();
	  	return((op!=MOVE_DONE) && (op!=MOVE_START) && (op!=MOVE_EDIT));
  		}
  		return(false);
  }
  private commonPlayer playerBeforeRobot()
  {
	  int idx = History.size()-1;
	  commonMove cm = getCurrentMove();
	  if(cm!=null)
	  {
	  commonPlayer robot = players[cm.player];
	  if(robot!=null && robot.isRobot)
	  {
	  while(--idx>0)
	  	{
		  commonMove m = History.elementAt(idx);
		  commonPlayer p = players[m.player];
		  if(p!=robot && !p.isRobot)
		  {
			  return(p);
		  }
	  	}
	  }}
	  return(null);
  }
  /**
   * This is normally determined by matching
   * the player at the top of the game record with the current player.  This
   * is only ok during normal play, not after endgame or while reviewing games. 
   * @return true if it is ok to undo the most recent moves back to where the turn changes to the another player
   */
  public boolean allowUndo() 
  {	if( !reviewMode()
		  && !simultaneousTurnsAllowed())
  	{	commonMove m = getCurrentMove();
  		BoardProtocol b = getBoard();
  		return ((m!=null) 
  				&& (b!=null)
  				&& ((m.op!=MOVE_START) && (m.op!=MOVE_EDIT))
  				// note that this test is to prevent undoing past a variation.  In live games
  				// this is not a problem. In replays, it prevents using "undo" to remove a 
  				// temporary variation with the side effect of losing the main line.
  				&& (m.next==null)
  				&& (m.nVariations()<=1)
  				&& (allPlayersLocal() || (m.player==b.whoseTurn())));
  	}
  	return(false);
  }
  /**
   * decide if its' ok to undo a robot move.  By default it's
   * allowed in non-tournament unranked games.
   * @return
   */
  public boolean allowRobotUndo()
  {	if((gameMode==Session.Mode.Unranked_Mode) && !l.tournamentMode) { return(true); }
	BoardProtocol b = getBoard();
  	int nplayers = b!=null ? b.nPlayers() : 0;
  	return(nplayers>2);
  }
  /**
   *  decide if we will actually give turn back to the opponent
   * @return true if it is ok
   */
  public boolean allowOpponentUndoNow() 
  { return( !((gameMode==Session.Mode.Master_Mode)|l.tournamentMode));
  }
  
  /**
   * decide if to present the "undo" button when it's not
   * our turn. Normally this means we accidentally hit the 
   * final "done", and the other player hasn't started his
   * move yet.  By default, don't allow it in tournament
   * or master games, and if the robot is thinking about
   * a move, wait for it to finish.
   * @return
   */
   public boolean allowOpponentUndo()
  {
	  if( !reviewMode()
			  && allowOpponentUndoNow()
			  && !simultaneousTurnsAllowed()
			  )
	  	{	commonMove m = getCurrentMove();
	  		// we will allow undo of the robot move if we're the player
	  		// before the robot, and various other conditions apply
	  		BoardProtocol b = getBoard();
	  		boolean canUndo = (m!=null) 
	  				&& !isRobotTurn() 
	  				&& (b!=null)
	  				&& ((allowRobotUndo() && (playerBeforeRobot()==l.my))	// previous move before robot was mine
	  					|| (m.player==l.my.boardIndex))	// previous move was mine
	  				&& (m.player!=b.whoseTurn())				// some other player's turn
	  				&& (m.op!=MOVE_START);
	  		return (canUndo);
	  	}
	  	return(false);
  }
  //
  // in turn based games, we don't want to allow a full undo
  // when it's the player's regular move.  This requires that
  // some move of his own be on top of the stack.
  //
  public boolean allowTurnBasedUndo()
  {
	  commonMove m = getCurrentMove();
	  BoardProtocol b = getBoard();
	  return m.player()==b.whoseTurn();
  }
  public boolean handleEditButton(Graphics gc,double rotation,Rectangle r,HitPoint p,HitPoint any,Color highlightColor,Color backgroundColor)
  {
	  //StockArt icon = p==null?StockArt.OffLight:StockArt.RedLight;
	  //return(allowed_to_edit && icon.drawChip(gc,this,r,p,GameId.HitEditButton,s.get(ExplainEdit)));
	  if((p!=null) && allowUndo())
	  {	  
	  	  commonPlayer pl = currentGuiPlayer();
	  	  if(!isTurnBasedGame() || allowTurnBasedUndo())
	  	  {
		  return handleUndoButton(gc,
				  	G.isApproximatelySquare(r)
				  		? pl.displayRotation
				  		:rotation,
				  	r,any,highlightColor,backgroundColor);
	  	  }
	  }
	  else if((any!=null) && allowOpponentUndo())
	  {
	  	  commonPlayer pl = l.my;
	  	  if(isTurnBasedGame())
	  	  { // in turn based games, undos are handled locally with no "please" from the other player.
	  		handleUndoButton(gc,G.isApproximatelySquare(r) ? pl.displayRotation:rotation,r,any,highlightColor,backgroundColor);  
	  	  }
	  	  else
	  	  {
		  return handlePleaseUndoButton(gc,G.isApproximatelySquare(r) ? pl.displayRotation:rotation,r,any,highlightColor,backgroundColor);		
	  	  }
	  }
	  else if(allowed_to_edit || G.debug())
	  {if (GC.handleRoundButton(gc, r, p,s.get(EditAction),highlightColor, backgroundColor))
		 	{
			 p.hitCode = GameId.HitEditButton;
			 HitPoint.setHelpText(p,r,s.get(ExplainEdit));
			 return(true);
		 	}
		 }
	  return(false);
  }

 /**
  * call this from your {@link #StopDragging} method to interpret
  * the standard buttons, such as "done" "edit" "start" "swap"
 * @param hitObject
  * @return true if the hitCode was handled
  */
    public boolean performStandardButtons(CellId hitObject, HitPoint hitPoint)
    {  	String msg = null;
    	if(super.performStandardButtons(hitObject, hitPoint)) { return(true); }
		if(hitObject instanceof GameId)
    	{
    	GameId ho = (GameId)hitObject;
    	switch(ho)
    	{
    	default: break;
    	case PlaceAnnotation:
    		annotationMenu.saveCurrentAnnotation(hitPoint,getCurrentMove());
        	return true;
    	case ShowNumbers:
    		showNumberMenu();
    		return true;
        case ShowAnnotations:
        	showAnnotationMenu(); 
        	return true;
    	case HitLiftButton: return(true); 	// maintained "lifted" variable
    	case FacingView: 
    		{ currentViewset = ViewSet.Reverse; resetBounds(); generalRefresh(); return(true); 
    		}
    	case NormalView:
    		{ currentViewset = ViewSet.Normal; resetBounds(); generalRefresh(); return(true); 
    		}
    	case TwistedView: 
    		{ currentViewset = ViewSet.Twist; resetBounds(); generalRefresh(); return(true); 
    		}
    	case ShowChat:
    		{
    			setSeeChat(true);
    		}
    		return(true);
    	case HideChat:
    		{
    			setSeeChat(false);
    		}
    		return(true);
    	case HitRulesButton:
    		{	String rules = rulesUrl();
    			if(rules!=null)
    				{
    				URL u = G.getUrl(rules,true);
    				G.showDocument(u);
    				}
    		}
    		return(true);
       	case HitPleaseUndoButton:
      	case HitUndoButton:
       	case HitResignButton:
    	case HitEditButton:
     	case HitStartP1Button:
    	case HitStartP2Button:
    	case HitStartP3Button:
     	case HitStartP4Button:
    	case HitStartP5Button:
    	case HitStartP6Button:
    	case HitStartP7Button:
    	case HitStartP8Button:
    	case HitStartP9Button:
     	case HitStartP10Button:
    	case HitStartP11Button:
    	case HitStartP12Button:

     	case HitPassButton:
     	case HitSwapButton:
       	case HitDoneButton:
       	case HitOfferDrawButton:
       	case HitDeclineDrawButton:
       	case HitAcceptDrawButton:
    		msg = ho.shortName;
    		break;

    	}}
    	if(msg!=null) 
    	{ PerformAndTransmit(msg); return(true); }
    	return(false);
    }
    
	  /**
     *  call this from your StopDragging method to handle the VCR control.
     * 
     * @param hitCode
     * @param hp
     * @return true if the hitObject was handled.
     */
    public boolean performVcrButton(CellId hitCode, HitPoint hp)
    {	if((hitCode==GameId.HitGameRecord)
    		&& (hp.hitObject instanceof commonMove))
    	{	commonMove m = (commonMove)hp.hitObject;
    		doScrollTo(m.index());   			 
    		return(true);
    	}
    	else {
        boolean rval = (hitCode instanceof VcrId);
        if(rval && hasControlToken())	// only actually do the scrolling if we have control
        {
        VcrId ho = (VcrId)hitCode;
        switch(ho)
        {
        case sliderAnimSpeedButton:
        	{	double newspeed = hidden.animationSpeedRect.value;
        		masterAnimationSpeed = Math.max(0,(2.0-newspeed));
        	}
			//$FALL-THROUGH$
		case sliderX2Button: { l.auxX2Rect.setValue(hp); } break;
        case sliderY2Button: { l.auxY2Rect.setValue(hp); } break;
        case sliderS2Button: { l.auxS2Rect.setValue(hp); } break;
        case sliderXButton:  { l.auxXRect.setValue(hp);  } break;
        case sliderSButton:  { l.auxSRect.setValue(hp);  } break;
        case sliderYButton:  { l.auxYRect.setValue(hp);  } break;
        case BackButton:
        	{
        	doScrollTo(BACKWARD_NEXT_BRANCH);
            setJointReviewStep(GET_CURRENT_POSITION);
        	}
        	break;
        case VarButton:
        	{
            doVcrVar();
            setJointReviewStep(GET_CURRENT_POSITION);
        	}
        	break;
        case FrontButton:
        	{
            doScrollTo(FORWARD_NEXT_BRANCH);
            setJointReviewStep(GET_CURRENT_POSITION);
        	}
        	break;
        case WayBackButton:
        	{
            doScrollTo(BACKWARD_TO_START);
            setJointReviewStep(GET_CURRENT_POSITION);
        	}
        	break;
       case BackStepButton:
        	{
            doScrollTo(BACKWARD_ONE);
            setJointReviewStep(GET_CURRENT_POSITION);
        	}
        	break;
        case ForeStepButton:
        	{
             doScrollTo(FORWARD_ONE);
             setJointReviewStep(GET_CURRENT_POSITION);
        	}
        	break;
        case ForeMostButton:
        	scrollFarForward();
        	setJointReviewStep(GET_CURRENT_POSITION);
        	break;
        case ForePlayerButton:
        	{
         	doScrollTo(FORWARD_PLAYER);
         	setJointReviewStep(GET_CURRENT_POSITION);
        	}
        	break;
        case BackPlayerButton:
        	{
        	doScrollTo(BACKWARD_PLAYER);
        	setJointReviewStep(GET_CURRENT_POSITION);
        	}
        	break;
        case Slider:
        	{
            doVcrSlider(hp);
            setJointReviewStep(GET_CURRENT_POSITION);
        	}
        	break;
        case AnimateButton:
        	{
        	if(!reviewMode()) 
        		{ doWayBack(replayMode.Replay); 
            	  hidden.lastAnimTime = G.Date()+250;	// make the animation wait for a refresh
        		}
        	animating = !animating;
        	repaint();
        	}
        	break;
        case expandButton:
        	Rectangle old = hidden.normalVcrRect;
        	int fr = G.Right(fullRect);
        	int fb = G.Bottom(fullRect);
        	int oldH = G.Height(old);
        	int oldW = G.Width(old);  		  	
  		  	int rw = oldW*2;
  		  	int rh = oldH*2;
 		  	int rl = Math.min(fr-rw,Math.max(0, G.Left(old)-oldH/2));
  		  	int rt = Math.min(fb-rh,Math.max(0, G.Top(old)-oldW/2));     	
        	setupVcrRects(rl,rt,rw,rh,true);   
  		  	G.SetRect(hidden.outsideVcrRect,rl-oldW/2,rt-oldH/2, rw+oldW, rh+oldH);
        	break;
        case shrinkButton:
        	SetupVcrRects(G.Left(hidden.normalVcrRect),G.Top(hidden.normalVcrRect),
        			G.Width(hidden.normalVcrRect),G.Height(hidden.normalVcrRect));
        	break;
        case noVcrButton:
        	break;
        default:
        	throw G.Error("Hit unknown vcr token %s",hitCode);
        	}
        }
        saveDisplayBoard();
        repaint();
        return (rval);
    	}
    
    }
    public void scrollFarForward()
    {
    	LogMessage("scroll far forward");
        doScrollTo(FORWARD_TO_END);
        setJointReviewStep(GET_CURRENT_POSITION);
    }
    public void leaveLockedReviewMode()
    { if( reviewMode()			// and may not even realize it.
    	  && !mutable_game_record
    	  && canUseDone()) 
    		{ scrollFarForward(); }
    }
    /**
     * this is where the annotation menu is actually drawn.
     * Wrap this method if you need to make any adjustments
     * 
     * @param g
     * @param p
     */
    public void drawAnnotationMenu(Graphics g,HitPoint p)
    {
    	if(G.Height(annotationMenu)>0)
    	{
    		annotationMenu.draw(g,disableForSpectators(p));
    	}
    }
    public boolean disableForSpectators()
    {
    	return (mutable_game_record && !allowed_to_edit);
    }
    public HitPoint disableForSpectators(HitPoint p)
    {
    	if(p!=null && mutable_game_record && !allowed_to_edit)
    	{
    		p.setHelpText(s.get(DisabledForSpectators));
    		return null;
    	}
    	return p;
    }
    /**
     * this is where the number menu is actually drawn.
     * Wrap this method if you need to make any adjustments.
     * 
     * @param g
     * @param p
     */
    public void drawNumberMenu(Graphics g,HitPoint p)
    {
    	if(G.Height(numberMenu)>0)
    	{
    		numberMenu.draw(g,p);
    	}
    }
    /**
     * this is where the annotation menu is actually popped up.
     * wrap this method if you need to make any adjustments
     * 
     */
    public void showAnnotationMenu()
    {
    	annotationMenu.showMenu();
    }
    /**
     * this is where the number menu is actually popped up.
     * wrap this method if you need to make any adjustments
     * 
     */
    public void showNumberMenu()
    {
    	numberMenu.showMenu();
    }
  /**
 * call this from your {@link #redrawBoard} method to draw the
 * vcr control. It also draws annotationMenu and numberMenu if
 * they are active.
 * 
 * @param p
 * @param inG
 * @return the hit code for the subbutton hit
 */
    public CellId drawVcrGroup(HitPoint p0, Graphics inG)
    {  	
    	if(G.Height(noChatRect)>0) { drawNoChat(inG,noChatRect,p0); }	// magically draw the chat/nochat button
       	drawNumberMenu(inG,p0);
       	HitPoint p = disableForSpectators(p0);
    	drawAnnotationMenu(inG,p);
     	int artHeight = G.Height(vcrZone);
     	CellId rval = DefaultId.HitNoWhere;
    	if(artHeight>0)
    	{
    	int artX = G.Left(vcrZone);
    	int artWidth = G.Width(vcrZone);
    	int artY = G.Top(vcrZone);
    	StockArt artObj = StockArt.VCRFrame;
    	if(!mutable_game_record && !animating)
    	{   if(reviewMode()) 
    		{
    		GC.setFont(inG,largeBoldFont());
    		GC.Text(inG, true, artX,artY,artWidth,artHeight/3,Color.blue,null,Reviewing);
    		}
    		else 
    		{artY = artY+artHeight/3;
    		artHeight = artHeight - artHeight/3;
    		artObj = StockArt.VCRFrameShort;
    		}
     	}
    		artObj.drawChip(inG,this,G.Width(vcrZone),
    			G.centerX(vcrZone),
    			artY+artHeight/2,
    			null);

    	
        if(useAuxSliders)
    	{	l.drawAuxSliders(inG,p);
    	}
    	else {

        if(animating)
        { rval =  DefaultId.HitNoWhere;
          hidden.animationSpeedRect.draw(inG,p,s);
          if(p!=null) { rval = p.hitCode; }
          
         }
        else 
        	{ rval = hidden.drawVcrButtons(p, inG); // button bar
        	}
        CellId tval = drawVcrSlider(p,  inG);
        if (tval != null)
        {
            rval = tval;
        }
        if(!animating)
        {
        if (mutable_game_record ) 
        {	boolean isin = G.pointInRect(p, hidden.vcrBackRect);
        	int w = G.Width(hidden.vcrBackRect);
        	int h = G.Height(hidden.vcrBackRect);
        	int xp = G.Left(hidden.vcrBackRect) + w/2;
        	int yp = G.Top(hidden.vcrBackRect) + h/2;
        	StockArt.VCRBackBranch.drawChip(inG,this,w+(isin?w/3:0),xp,yp,null);
        	if(isin)
        		{
        		rval = VcrId.BackButton;
        		}
        }
 
        if (reviewMode() && mutable_game_record)
        {
            int vv = History.viewStep;
            commonMove m = ((vv<History.size())&&(vv > 0)) 
            						? (commonMove) History.elementAt(vv - 1)
                                    : null;
            int n = (m == null) ? 1 : m.nVariations();
            String txt = s.get(ChoiceString,n);
            {
            boolean isin = G.pointInRect(p, hidden.vcrVarRect);
            int w = G.Width(hidden.vcrVarRect);
            int h = G.Height(hidden.vcrVarRect);
            int xp = G.Left(hidden.vcrVarRect) + w/2;
            int yp = G.Top(hidden.vcrVarRect) + h/2;
            GC.setColor(inG,isin?Color.white:Color.black);
            StockArt.VCRButton.drawChip(inG,this,w,xp,yp,null);
            GC.drawOutlinedText(inG,true,xp-w/2,yp-h/2,w,h,Color.white,Color.black,txt);
            if(isin)
            {
                rval = VcrId.VarButton;
            }}
            {
            boolean isin = G.pointInRect(p,hidden.vcrFrontRect);
            int w = G.Width(hidden.vcrFrontRect);
            int h = G.Height(hidden.vcrFrontRect);
            int xp = G.Left(hidden.vcrFrontRect) + w/2;
            int yp = G.Top(hidden.vcrFrontRect) + h/2;
            StockArt.VCRForwardBranch.drawChip(inG,this,w+(isin?w/3:0),xp,yp,null);
            if (isin)
            {
                rval = VcrId.FrontButton;
            }}}

        }

        if ((p!=null) && (rval != null))
        {   p.hitCode = rval;
            if(p.down && (rval==VcrId.Slider)) { p.dragging=true; }
        }
    	}
        if(!allowedToScroll()) 
        	{	// if we get here unexpectedly, it's because the
        		// control token has been passed and we should be silent
        		rval = DefaultId.HitNoWhere; 
        	}
    	}
        return (rval);
    }

   /**
     * This is called to present a menu of alternatives when the "n choices" button is pressed.
     */
    private void doVcrVar()
    {
        int sliderPos = (History.viewStep < 0) ? (-1) : (History.viewStep - 1);

        if ((sliderPos<History.size()) && (sliderPos >= 0))
        {	LogMessage("select variation");
            commonMove m1 =History.elementAt(sliderPos);
            m1.variationsMenu(hidden.vcrVarPopup,this,deferredEvents);
            //this.add(vcrVarMenu);
            //vcrVarMenu.addActionListener(deferredEvents);
            hidden.vcrVarPopup.show( G.Left(hidden.vcrFrontRect), G.Top(hidden.vcrFrontRect));
        }
    }
    public boolean boardMax = false;
    private JCheckBoxMenuItem boardMaxCheckbox = null;
    
    public GameLayoutManager selectedLayout = new GameLayoutManager(boardMax);
    /**
     * this is called to display the rectangle grid while debugging layouts.
     */
    public void showRectangles(Graphics gc,HitPoint pt, int cellsize)
    {	if(show_rectangles)
    	{
	    	for(int i=0;i<players.length;i++) 
	    	{ commonPlayer pl = players[i];
	    	  if(pl!=null) { pl.addRectangles(this,i); }
	    	}
    	if(selectedLayout!=null)
    	{
    	//	G.fillRect(gc,Color.darkGray,fullRect);
    		Rectangle main = selectedLayout.peekMainRectangle();
    		if(main!=null) { GC.fillRect(gc, Color.lightGray,main); }
    		RectangleStack rects = selectedLayout.rects.spareRects;
    		for(int i=0;i<rects.size();i++) 
    			{ Rectangle r = rects.elementAt(i);
    			  GC.fillRect(gc, new Color(0xff&(i*10),0xff&(i*20),0xff), r);
    			  GC.frameRect(gc, Color.green,G.Left(r),G.Top(r),G.Width(r),G.Height(r));
    			  HitPoint.setHelpText(pt,r,G.concat(G.Left(r),",",G.Top(r)," - ",G.Right(r),",",G.Bottom(r)));
    			}
 
       	}}
       	super.showRectangles(gc,pt,cellsize);
   	    if(G.pointInRect(pt,goalRect))
	    {
	    	pt.setHelpText(""+selectedLayout.selectedSeating());
	    }
    }
    /**
     * This is called to display the current move's associated comments, in the chat window.
     */
    public void showComments()
    {
        //System.out.println("Save for "+commentedMove+" show for "+m);
    	//
    	// this behavior is intended for generating commented files, but in multi-player
    	// contexts it creates a lot of confusion testing the number of connections tweaks
    	// the behavior to be the "right thing" 7/2015
    	//
        if (reviewOnly && (numberOfConnections()<=1) && theChat!=null)	
        {
            commonMove m = History.currentHistoryMove();
            if(m!=null)
            {
            String newMessage = m.getComment();	
            String currentMessage = theChat.getMessages();
            
            if(commentedMove!=null)
            {	String currentComment = commentedMove.getComment();
            	boolean same = currentMessage.equals(currentComment);
            	if(!same && (commentedMoveSeen!=null))
            		{
            		commentedMove = commentedMoveSeen;
            		}
            	if(!same)
            		{ commentedMove.setComment(currentMessage); 
            		}
            }
            else if ((commentedMoveSeen!=null) && (currentMessage!=null) && !"".equals(currentMessage))
            {
            	commentedMoveSeen.setComment(currentMessage);
            	commentedMove = commentedMoveSeen;
            }
            if(!"".equals(newMessage) && (newMessage!=null))
            {
            commentedMove = m;
            theChat.clearMessages(true);
            commentedMoveSeen = m;
            theChat.setMessage(newMessage);
            }
            else 
            { if(commentedMoveSeen==null) { theChat.clearMessages(true); }
              commentedMoveSeen = m;
            }
            }
        }
    }


    private void chooseVcrVar(commonMove command) // select a succesor for the current move
    { // command will be a selection from a menu, which are the descriptions of the successor moves
        int sliderPos = (History.viewStep < 0) ? (-1) : (History.viewStep - 1);

        if ((sliderPos<History.size()) && (sliderPos >= 0)) // only if we're still in review mode
        {
            commonMove m1 = History.elementAt(sliderPos);
            commonMove m = m1.findVariation(command);
            LogMessage("selected variation is ",command);
            if (m != null)
            {	if(m1.next == m) 
            		{ hidden.doForward(replayMode.Replay1); }
            		else {  PerformAndTransmit(m.moveString(), true,replayMode.Replay1); }
            }
        }
    }
    private void doVcrSlider(Point hitPoint)
    {
        if (hitPoint != null)
        {
        	int next = nextSliderPosition(hitPoint);
        	LogMessage("scroll to ",next);
            doScrollTo(next);
        }
    }
    private int nextSliderPosition(Point hitPoint)
    {
            int width = G.Width(vcrRect)-G.Width(vcrRect)/6-G.Width(vcrRect)/12-6;
            int x = G.Left(hitPoint) - 3 - G.Left(vcrRect);
            int size = Math.max(History.size() + 10, 50);
            double step = (double) width / size;
        int newpos = Math.max(0, Math.min(size,(int) (x / step)));
        return(newpos);
    }
    
  
    private boolean isRobotTurn()
    {
    	commonPlayer p = currentGuiPlayer();
    	return( p!=null && p.isRobot); 
    }

    /**
     * Undo all of the current player's actions.
     * don't call this directly from game code - your game history will get out of sync.
     * instead use PerformAndTransmit("Undo");  You may superceed or wrap it to change
     * the behavior
     */
   public void doUndo()
    {  	
    	do {
    		doScrollTo(BACKWARD_ONE) ;
    		truncateHistory(); 
    	} while(allowPartialUndo());
    }
    /**
     * Undo 1 game step.
     * don't call this directly from game code - your game history will get out of sync.
     * instead use PerformAndTransmit(UNDO_ALLOW);  You may supersede or wrap it to 
     * change behavior.
     */
    public void doUndoStep()
    {	do {
    	doScrollTo(BACKWARD_ONE);
		truncateHistory();
    	} while (allowBackwardStep());
    }
    public boolean doBackwardPlayer()
    {
		BoardProtocol bd =  getBoard();
    	if(bd!=null)
    	{
    	int who =bd.whoseTurn();
		while((bd.whoseTurn()==who) && doScrollTo(BACKWARD_ONE)) {};
		while((bd.whoseTurn()!=who) && doScrollTo(BACKWARD_ONE)) {};
		return(hidden.doForward(replayMode.Replay1));
    	}
    	return(false);
    }
    public void doForwardPlayer()
    {
    	BoardProtocol bd =  getBoard();
    	if(bd!=null)
    	{
		int who =bd.whoseTurn();
		while((bd.whoseTurn()==who) && hidden.doForward(replayMode.Single)) {};
    	}
    }
    public boolean doForward(replayMode replay)
    {
    	return(hidden.doForward(replay));
    }
    
   // scroll by action by this player.  we should have control
   public boolean doScrollTo(int whence)
   {	
	   return doScroll(whence,"local");
   }
   // scroll by another player.  We should NOT have control here
   public boolean doRemoteScrollTo(int whence)
   {	
	   boolean v = doScroll(whence,"remote");
	   saveDisplayBoard();
	   repaint();
	   return v;
	   
   }

   public boolean doScroll(int whence,String remote)
   {	boolean val = false;
   		if(whence!=History.viewStep)
    	{BoardState pre_state = getBoard().getState();
    	int pre_view = History.viewStep;
    	val = doScrollTo_internal(whence,remote);
    	if((pre_view==-1) && (History.viewStep!=-1))
    		{	History.pre_review_state = pre_state;
    		}}
   	return(val);
   }
   public boolean doBack(int n)
   {
	   return(hidden.doBack(n));
   }
    /**
     * scroll to a particular move number, or to one of the
     * special positions indicated by negative codes.
     */
    private boolean doScrollTo_internal(int val,String remote)
    {	boolean rval = false;
        int vs = History.viewStep;
        History.sliderPosition = -1;
        rawHistory.addElement(new dummyMove("vcr:@"+vs+" doScrollTo "+val+" control "+hasControlToken()+" "+remote));
        switch (val)
        {
        case BACKWARD_ONE:
           	LogMessage("scroll back 1");
            rval = doBack(1);
            break;

        case FORWARD_ONE:
           	LogMessage("scroll forward 1");
            rval = hidden.doForward(replayMode.Single);
            break;
        case BACKWARD_DONE:
        	LogMessage("scroll backward player");
    		{
    		BoardProtocol bd =  getBoard();
    		commonMove m = History.currentHistoryMove();
    		if((m!=null) && (m.op==MOVE_DONE)) { doScrollTo(BACKWARD_ONE); }
    		if(bd!=null)
    		{
        	int who =bd.whoseTurn();
        	while( ((m=History.currentHistoryMove())!=null)
        			&& (m.op!=MOVE_DONE) 
        			&& (bd.whoseTurn()==who) && doScrollTo(BACKWARD_ONE))
        		{  }
    		}}
    		break;
        case BACKWARD_PLAYER:
        	LogMessage("scroll backward player");
        	rval = doBackwardPlayer();
        	break;
        case FORWARD_PLAYER:
           	LogMessage("scroll froward player");
        	doForwardPlayer();
        	break;
        case FORWARD_NEXT_BRANCH:

        	LogMessage("scroll 	forward-branch");
        	doForwardBranch();

            break;

        case BACKWARD_NEXT_BRANCH:
           	LogMessage("scroll backward-branch");

           	doBackwardBranch();

            break;

        case BACKWARD_TO_START:
        	LogMessage("scroll wayback");

            // all the way back
            doWayBack(replayMode.Replay);
            showComments();
            break;

        default:

        	showComments();	// save modified comments as a side effect
        	int initialPosition = getReviewPosition();
            int hsize = History.size();
            if (initialPosition == -1)
            {	initialPosition = hsize;
                if(val<initialPosition) { hidden.enterReviewMode(); }
            } 
            if (val == -1)
            {	val = hsize;
            } //prepare to leave review mode
            val = Math.min(val, hsize);

            
			// scroll to a position (if back)
            {
            if(initialPosition!=val)
            {
            int pos;
			while (( (pos=getReviewPosition()) > val) && doBack(pos-val))
            { // doBack does it all
            }
			//	scroll to a position (if forward)
            while ((getReviewPosition() < val) && hidden.doForward(replayMode.Replay1))
            { // doforward does it all
            	commentedMoveSeen = commentedMove = null;
            	showComments();
            }}}

            break;
        }
        
        if(val==History.size()) { History.viewStep = -1; val=-1; }
               
        if( ((vs==-1) && (History.viewStep!=-1))
        		|| ((History.viewStep==-1) && (vs!=-1)))
        	{ generalRefresh(); 
        	}
        	else if(rval) { repaint(20); }
        
       return (rval);
    }

    /**
     *  reviewMode is true only if we are NOT at the end
       of the current variation.
    */
    public boolean reviewMode()
    {
        return (History.viewStep >= 0);
    }
    /**
     * get the current review position.  This is used to synchronize multiple clients.
     */
    public int getReviewPosition()
    {
        return (History.viewStep);
    }

    /** set the desired common review position
     * 
     */
    public void setJointReviewStep(int v)
    {	if(v!=jointReviewStep)
    	{
        jointReviewStep = v;
    	}
    }
/**
 * get the desired joint review position
 */
    public int getJointReviewStep()
    {
        return (jointReviewStep);
    }
       

    public void RedoStep(replayMode mode)
    {
        int vs = History.viewStep;
        commonMove m = History.elementAt(vs++);
        if (vs >= History.size())
        {
            vs = -1;
        }
        History.viewStep = vs;
        hidden.RedoStep(mode,m);
    }
    
    
    public boolean doForwardStep(replayMode replay)
    {
        int size = History.size();
        int start = History.viewStep;
        if ((start >= 0) && (start < size))
        {
            RedoStep(replay);
            boolean notAtEnd = (History.viewStep >= 0);
            if(!notAtEnd || (replay!=replayMode.Replay)) { showComments(); }
            if(notAtEnd) { return notAtEnd; }
        }

        History.viewStep = -1;
        generalRefresh();
        return (false);
    }

    /**
     * movement style for an animation.
     * <li>{@link #Chained} chained movement of a piece or stack from one dest to the next</li>
     * <li>{@link #Simultaneous} everything moves at the same time</li>
     * <li>{@link #Stack} everything moves at the same time, as a stack</li>
     * <li>{@link #StackFromStart} sequential stacks start one at a time, but launch sequentially</li>
     * <li>{@link #Sequential} pieces move separately, one at a time.</li>
     * <li>{@link #SequentialFromStart} pieces start at the same time but launch sequentially
     * @author Ddyer
     *
     */
    public enum MovementStyle 
    	{ 
    	  Chained,		/** chained movement of a single piece or a stack of pieces */
    	  Simultaneous, /** everything moves at the same time */
    	  SimultaneousAfterOne,	/** first move is alone, them all together */
    	  Stack,		/** everything moves at the same time, as a stack */
    	  StackFromStart, /** sequential stacks start one at a time, but launch sequentially */
    	  Sequential,	/** pieces move separately, one at a time. */
    	  SequentialFromStart,	/** pieces start at the beginning but launch sequentially.  This is useful for standing targets. */
    	}
    /**
     * start animations from animationStack.  In this version, if there
     * are multiple animations, depending on the value of "serially", they
     * are all started at once, or they are chained from first to last.
     * If serially, the last cell is the final hop where the chip lives
     * and the animation hops from the first to last. 
     * @param replay
     * @param animationStack
     * @param size
     * @param groupMovement
     */
    public void startBoardAnimations(replayMode replay,OStack<?>animationStack,int size,MovementStyle groupMovement)
    {	int sz =  animationStack.size();
        if(sz>0)
        {
    	try {
    	boolean chained = (groupMovement==MovementStyle.Chained);
    	boolean stack = (groupMovement==MovementStyle.Stack)||(groupMovement==MovementStyle.StackFromStart);
    	boolean sequential = (groupMovement==MovementStyle.Sequential)||(groupMovement==MovementStyle.SequentialFromStart);
    	boolean fromStart = (groupMovement==MovementStyle.SequentialFromStart)||(groupMovement==MovementStyle.StackFromStart);
    	if(replay.animate)
    	{	double start = 0.0;
    		cell <?> lastDest = null;
    		int depth = 0;
    		double lastTime = 0;
    		for(int idx = 0,lim=sz&~1; idx<lim;idx+=2)
    		{
			cell<?> from = (cell<?>)animationStack.elementAt(idx);		// source and destination for the move
 			cell<?> to = (cell<?>)animationStack.elementAt(idx+1);
 			if(!G.Advise(from!=null,"animation stack item shouldn't be null"))  { break; }
 			if(!G.Advise(to!=null ,"animation stack item shouldn't be null")) { break; }
 			if(sequential || ((chained||fromStart) && ((to!=lastDest) || ((lastDest!=null) && lastDest.forceSerialAnimation()))))
 					{
 					// step to the next starting time if we change dests
 					start += lastTime;
 					}
 			depth = (to==lastDest) ? depth+1 : 0; 
 			// if serially, the moving object is the chip on top of the last
 			// destination, and it moves from the first source to last in 
 			// multiple steps.
 			cell<?>from0 = (chained ? (cell<?>)animationStack.top() : to);
 			if(!G.Advise(from0!=null,"animation stack item shouldn't be null")) { break; }
    		Drawable moving = from0==null ? null : from0.animationChip(depth);
    		if(moving!=null)
				{
        		int animationSize = from0.animationSize(size);
        		if(G.Advise(animationSize>2,"animation size too small %s",from0))
        		{
    			int d = (chained|stack)?depth:-1;
    			if(fromStart && (start>0.0))
    			{
    			// the subsequent steps start by standing on the origin until their
    			// turn.  This works for zertz captures for example, where the jumping
    			// ball jumps, then the captured ball moves off.
    			SimpleSprite anim = startAnimation(from,from,moving,animationSize,0,start,d);
    			if(chained) { anim.isAlwaysActive=true; }
    			}
    			SimpleSprite anim = startAnimation(from,to,moving,animationSize,start,0,d);	// start it
    			if(anim!=null)	// be defensive
    			{
    			if(chained) { anim.isAlwaysActive = true; }
				lastTime = anim.getDuration();
				}}}
    		lastDest = to;  
    		}
    	}
    	}
    	finally {
       	animationStack.clear();
       	// make sure we have the latest copy, ortherwise the active copy won't have 
       	// the active animation stacks
        saveDisplayBoard();	
    	}
        }
    }    
/**
 * start an animation step in the simplest way.  
 * @param from	the source cell
 * @param to	the destination cell
 * @param top	the chip to draw
 * @param size	the size of the chip (ie; squaresize)
 * @param start the start time in seconds relative to the start of this animated sequence (0=start of this animation sequence)
 * @param duration in seconds of this animation.  If 0, scale to the distance moved
 * @return the sprite returned
 */
    public SimpleSprite startAnimation(cell<?> from,cell<?> to,Drawable top,int size,double start,double duration)
    {
    	return startAnimation(from,to,top,size,start,duration,-1);
    }
    /**
     * adjust the size of this particular drawable for animation ending at some cell
     * @param dest
     * @param chip
     * @param sz
     * @return
     */
    public int adjustAnimationSize(cell<?>dest,Drawable chip,int sz)
    {
    	return(sz);
    }
    /**
     * 
 * start an animation step in the simplest way.  
 * @param from	the source cell
 * @param to	the destination cell
 * @param top	the chip to draw
 * @param size	the size of the chip (ie; squaresize)
 * @param start the start time in seconds relative to the start of this animated sequence (0=start of this animation sequence)
 * @param duration in seconds of this animation.  If 0, scale to the distance moved
 * @return the sprite returned
     */
    public SimpleSprite startAnimation(cell<?> from,cell<?> to,Drawable top,int size0,double start,double duration,int depthm1)
    {	if((from!=null) && (to!=null) && (top!=null))
    	{	// if debugging on a main screen, check that the cell has it's center set.
    		// a lot of cells for side screens never get touched, so don't check.
    		if(remoteViewer<0 && G.debug())
    		{	from.assertCenterSet();
				to.assertCenterSet();		
    		}
    		int depth = Math.max(0, depthm1);
    		int size = adjustAnimationSize(to,top,size0);
    		boolean randomize = depthm1<0;
    		int tox = to.animationChipXPosition(depth);
    		int fromx = from.animationChipXPosition(depth);
    		int toy = to.animationChipYPosition(depth);
    		int fromy = from.animationChipYPosition(depth); 
    		double rot =to.activeAnimationRotation();
    		double end = duration;
    		// make time vary as a function of distance to partially equalize the runtime of
    		// animations for long verses short moves.
     		if(end==0)
     		{	double speed = masterAnimationSpeed;
     	   		double dist = G.distance(fromx, fromy, tox,  toy);
        		double full = G.distance(0,0,G.Width(boardRect),G.Height(boardRect));
        		end = Math.sqrt(2*speed*dist/full);
     		}
    		SimpleSprite newSprite = new SimpleSprite(randomize,top,
    				size,		// use the same cell size as drawSprite would
    				start,end,
            		fromx,fromy,
            		tox,toy,rot);
    		newSprite.movement = Movement.SlowIn;
            to.addActiveAnimation(newSprite);
  			addSprite(newSprite);
  			return(newSprite);
  			}
    	return(null);
    }
    
    // set comments on moves as they are being replayed from
    // a sgf file.
    public void setComment(String str)
    {	int siz = History.size();
    	commonMove m =   siz>0 ?       History.elementAt(siz - 1) : null;
    	if (m != null)
    	{
    		m.setComment(str);
        }
    }
    /**
     * get the current move at the end of the move history.
     * this may return NULL under rare circumstances
     * @return
     */
    public commonMove getCurrentMove()
    {
    	return(History.currentHistoryMove());
    }
    /**
     * get the opcode of the current move.  This will return MOVE_START if
     * the move history is empty
     * 
     * @return
     */
    public int getCurrentMoveOp()
    {
    	commonMove m = getCurrentMove();
    	if(m!=null) { return(m.op); }
    	return(MOVE_START);		// nothing there yet, claim the start
    }
    /**
     * This is how events such as
     * mouse clicks are transmitted to the local board.
     * If "playsounds" is true (and other sound-related flags
     * permit) then game sounds are also played.
     * 
     * @param m
     * @param replay true if we are from a single step forward
     * @return true if successful
     */
     public abstract boolean Execute(commonMove m,replayMode replay);
    
     /**
      * parse a string toto a move spec.  As a debugging aid, the parsed
      * string is printed and reparsed.
      * @param str	a move string
      * @param player the player to parse for (ie, the current player)
      * @return a parsed move
      */
    public commonMove ParseMove(String str, int player)
    {	l.lastParsed = str;
       	commonMove m = ParseNewMove(str, player);
    	if(G.debug())
    		{ String ms = m.moveString();
    		  commonMove m1 = ParseNewMove(ms, player);
    		  // attempt to print/reparse the result, so we have some assurance
    		  // that live use will match offline use.
    		  String m1s = m1.moveString();
    		  if(!m1s.equals(ms))
    		  {	System.out.println("Probable parse/print error in moveString(); "+m1+" becomes "+m1s); 
    		  }
    		}
    	return(m);
    }

    /**
     * parse and execute the string.  This seemingly inefficient
     * process assures that what we send over the web will be interpreted
     * identically to what we do locally.  Also, if ParseMessage trips
     * any illegal-move type errors, which shouldn't occur but "shit happens"
     * we will never get to adding to the even queue
      * @param str
     * @param transmit
     * @param replay  replay mode, which advises auxiliary behavior such as sound and animation
     * @return true if successful
     */
 public boolean PerformAndTransmit(String str, boolean transmit,replayMode replay)
 {
     // used in different contexts; parsing live moves from the GUI
     // also parsing replayed moves when resuming a game.  If not live, 
	 // defer setting the player until later - this handles cases where
	 // joining a game which has variations and it will be necessary
	 // to roll back the game before executing.
     int player =
    		 (replay==replayMode.Replay) 
    		 ? -1 
    		 : (simultaneousTurnsAllowed()
   		  		? getActivePlayer().boardIndex
   		  		: getBoard().whoseTurn());
   	return(PerformAndTransmit(str,player,transmit,replay));

 }
/*
 * 
 */
 public boolean PerformAndTransmit(String str, int player,boolean transmit,replayMode replay)
 { 
	  l.lastParsed = str;
      commonMove m = ParseMove(str,player);
      if(m!=null)
        {	
	        if (!PerformAndTransmit(m, transmit,replay))
	        {
	        	throw G.Error("can't perform %s" , str);
	        }
        }
        else
        { throw G.Error("Can't parse %s",str);
        }
        l.lastParsed=null;
        //G.startLog("Move "+str);
        repaint();
        return (true);
    }
    
    public String errorContext()
    {	String sup = super.errorContext();
    	if(l.lastParsed!=null) { sup += "{LastParsed: "+l.lastParsed+"}"; }
    	return(sup);
    }
    public void undoToMove(commonMove m)
    {
    	if(m.index()>0)
    		{ rewindHistory(m.index());
    		  truncateHistory();
    		}
    }
    /**
     * normally, no moves should be transmitted during in-game review.  Or
     * when it is not your turn to play. This allows an override for particular
     * moves. Presumably moves that only affect the global state, not the
     * particular board position.  
     */
    public boolean canSendAnyTime(commonMove m)
    {
    	return (m.op==MOVE_PLEASEUNDO);
    }
    
    /**
     * perform and optionally transmit a move, return true if ok.  Note, it's tempting
     * to do any "auto move" that is needed in the continuation of this method, but don't.
     * Doing so may separate the "perform" from the "transmit" which will lead to other
     * players seeing the events out of order.  Instead, use the continuation of ViewerRun
     * @param m
     * @param transmit
	 * @param replay replay mode
     * @return true if successful
     */

    public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode replay)
    {	//G.print("e "+my+" "+m);
    	if(transmit && !canSendAnyTime(m) && G.debug())
    	{ G.Assert(reviewOnly || mutable_game_record || !reviewMode(),
    			"can't send moves during in-game review: %s",m); 
    	}
       if(m.index()>=0)
       {   // something delicate here.  m.index=-1 for newly input moves
    	   // moves being replayed may have an index, in which case their position 
    	   // in the history is fixed. The replayers can add optional "done" moves
    	   // which are immediately followed by real done moves which supercede them.
    	   rewindHistory(m.index());
       }
       else if (History.viewStep>0)  // maybe backed up
       	{   
            rewindHistory(History.viewStep);
       	} 
       		// collect the spec before executing or adding to history, so that
       		// edits made to m by execute or editHistory will not be transmitted
  //      if(m.op==HitGameOverOnTime(GAMEOVERONTIME,GameOverOnTime),
//	HitIgnoreTime(GAMEIGNORETIME,GameIgnoreTime),
//		HitAddTime(GAMEADDTIME,GameAddTime),
//		ResolveTimeOver(RESOLVETIMEOVER,TimeOverResolution),)
        
      	if(m.op==MOVE_PLEASEUNDO)
       	{	// this when we hit the undo button, and we need to 
       		// pass the request to the opponent.
       		commonPlayer p = currentGuiPlayer();
       		if(p!=l.my)
       			{ if(transmit)
       				{ addEvent(m.moveString()); // out of turn undo
       				}
       			}	
       		else if(allowOpponentUndoNow()) 
       			{ PerformAndTransmit(UNDO_ALLOW); 
       			}
       		return(true);	// we did it
       	}
       	else if (Execute(m,replay))
        {
        	repaint(20);					 // states will have changed.
        	// str may be altered by AddToHistory, too, so get the string that must be transmitted first.
        	String str = m.moveString();	// note that "moveString" may be altered by the "execute"
            boolean added = AddToHistory(m);
            // this is the active part of the "Start Evaluator" feature
            if (extraactions && (getActivePlayer().robotPlayer != null))
            {	// get the robot to static eval this position
                getActivePlayer().robotPlayer.StaticEval();
            }
 
            if(added)
            	{ // this is later than originally placed, so the repetition check
            	  // takes place only after the current move is sure to be added.
            		BoardProtocol bd = getBoard();
                 	m.setDigest(bd.Digest());
                 	m.setGameover(false);
                 	if(repeatedPositions.checkForRepetition(bd,m))
                 		{
                 		// we changed state, the display may need to be updated
                 		saveDisplayBoard();
                 		}
            		long newdig = bd.Digest();
            		if(newdig!=m.digest())
            			{ m.setGameover(true); 
            			  // use the new digest (hive, game over by repetition)
            			  m.setDigest(newdig);
            			} 
            	}
            if(replay==replayMode.Live) { verifyGameRecord(); }
            if (transmit && (allowed_to_edit || canUseDone()))
            {	if(m.elapsedTime()<=0)
            	{
            	// this may not have been done yet if the game is messing with "replaymode" 
            	// for whatever reason.
            	commonPlayer p = getPlayerOrTemp(m.player);
            	m.setElapsedTime((int)p.elapsedTime);
            	}
            	str = "+T "+m.elapsedTime()+" "+str; 	// add the move time stamp
                addEvent(str);
            }
            if((replay==replayMode.Live) && playerChanging()) { playTurnChangeSounds(); }
            return (true);
        }
        return (false);
    }

    /**
    * handle Execute in the standard way, which includes 
    * recording potential repetitions and checking for
    * draws by repetition and checking for game over.
    *
     * @param b
     * @param m
     */
    public void handleExecute(BoardProtocol b,commonMove m,replayMode replay)
    {	boolean review = reviewMode();
    	if(b.getState().Puzzle())
    	{
    		m.setSliderNumString("--");
    	}
    	if(G.debug())
    	{
    	// this ought to be called ONLY with the real board, not from robot
    	// boards, and not from display copies.  It also ought to be called
    	// ONLY in the real game thread.
    	G.Assert(b==getBoard(), "not my board");
    	G.Assert(runThread==null || (Thread.currentThread()==runThread),
    			"wrong thread");
    	}
        int step = b.moveNumber();
        if(!review) { History.viewMove = Math.max(step, History.viewMove);} // maintain the max move for the parents
        
        // note this is a deliberate EQ comparison of a string.
        if(commonMove.isDefaultNumString(m.getSliderNumString()))
        	{ m.setSliderNumString("" + step); 
        	}
        if (m.player < 0)
        {
            m.player = b.whoseTurn();
        }
        if(m.op==MOVE_DONTUNDO) {}	// ignore this
        else if(m.op==MOVE_ALLOWUNDO) 
        {	// unconditional undo, after approval of a request
        	do { doUndoStep(); } while(isRobotTurn()); 
        	if(autoDoneActive() ) { skipAutoDone = true; }
        }
        else if((m.op==MOVE_RESET)||(m.op==MOVE_UNDO))
        {	performUndo();
        }
        else
        { 	commonPlayer pl = getPlayerOrTemp(m.player);
        	switch(replay)
        	{	default: G.Error("Not expecting mode %s", replay);
        		case Live: 
        			{ 	// in live play, load the move from the player time
        				m.setElapsedTime((int)pl.elapsedTime); 
        			}
        			break;
        		case Replay:
        		case Replay1:
        		case Single:
        			{	// in review, load the player from the move time
        			pl.setReviewTime(m.elapsedTime());
        			}
        	}
        	b.Execute(m,replay);						 // let the board do the dirty work
        }
        if(m.op==MOVE_EDIT) 
        	{ stopRobots(); 
        	}
        // this is where checkForRepetition used to be.  It's now slightly later, after the history
        // has been updated.  The problem with checjing in this position is that the move beng added
        // may alter the history, for example causing itself to be eliminated, so a third repetition
        // will not occur.
        // checkForRepetition(b,m);
        if(!review) 
        	{ //let the supervisor know if we're just waiting for a "done"
         	setDoneState(b.DoneState()); 
        	}
        GameOverNow();	// notice if the game is over now
        // this saves a copy of the board for use in redisplay
        // if it is necessary to do so - ie on codename1
        if(replay!=replayMode.Replay) { saveDisplayBoard(); }
    }
    /**
     *  the default behavior is if there is a picked piece, unpick it
     *  but if no picked piece, undo all the way back to the done.
     */
    public void performUndo()
    {	BoardProtocol b = getBoard();
    	if(b!=null) 
    	{
    		if(isPuzzleState()) 
    			{ doUndoStep(); 
    			}
    		else if(allowResetUndo()) { doUndoStep(); }
    		else if(allowUndo()) { doUndo(); }
  		}

    }

/**
 * the contract of editHistory is to remove unnecessary elements
 * from the game history, resulting in a tidy record that when 
 * replayed, results in a duplicate of the current state.  This default
 * method works by taking a game digest after every move, and assuming 
 * that if the game digest reverts to a previous value, it must be an undo
 * back to that move.   Scanning back stops with a MOVE_DONE 
 * @param m
 * @return a move to be added to the history, or null
 */
    public commonMove EditHistory(commonMove m)
    {	return(EditHistory(m,false));
    }
    
    public long setDigest(commonMove m)
    {
    	long dig = getBoard().Digest();
    	m.digest = dig;  
    	return dig;
    }
    public commonMove EditHistory(commonMove m,boolean okSame)
    {	long dig = setDigest(m);
    	int size = History.size() - 1;
    	int fullsize = size;
    	switch(m.op) {
    	case MOVE_RESET:
    	case MOVE_UNDO:
    	case MOVE_PLEASEUNDO:
    	case MOVE_DONTUNDO:
    	case MOVE_ALLOWUNDO:
    		// these always get filtered out
    		m = null;
    		size = -1;
    		break;
    	default: break;
    	}
    	while(size>=0)
    	{
    		commonMove hmove = History.elementAt(size);
    		
   			if(hmove.digest() == dig)
    			{	if(size!=fullsize)
    				{
    				// this is the normal "undo" case, where we undo some number of moves
    				// to get back to the same state
    				while(fullsize>size)  
    					{ popHistoryElement();
    					  fullsize--;
    					}
    				m = null;
    				size = -1;
    				}
     				else if(m.op==MOVE_PASS) 
    				{	// special treatment for pass moves. passes may or may not change the state
    					// and if they don't, then two passed in a row are ok, just remove one.
    					// this accommodates games like hive and exxit where you can't pass voluntarily, but
    					// you can be forced to pass.
    					if(hmove.op==MOVE_PASS) { popHistoryElement(); size=-1;  }
    					else if(okSame) {/* pro forma pass for effect in the log */ size=-1; }
    					else { throw G.Error("Single pass did not change the Digest"); }
    				}
    				else if((m.op==MOVE_EDIT) || (m.op==MOVE_START)) 
    				{ 	size = -1;
     				}
    				else if(okSame || (m.op==MOVE_OFFER_DRAW)) { m=null; size=-1; }
 				    else
    				{
				    	throw G.Error( "Move %s didn't change digest",m);
    				}
    			} 
    			else { size--; }		// keep scanning
    		if(hmove.op==MOVE_DONE) 
    			{ size=-1; }
    		}
    	return(m);
     }

    /**
     * this is the usual point where gestures made with the mouse
     * are transformed into public actions.
     */
    public boolean PerformAndTransmit(String str)
    {	return(PerformAndTransmit(str,true,replayMode.Live));
    }

    private void setHistorySize(int sz)
    {	int vstep = sz;
    	int hsize = History.size();
    	// coordinate with the repeatedPositions cache
    	if(History.viewStep<hsize) 
    	{	while(--hsize>vstep)
			{	repeatedPositions.removeFromRepeatedPositions(History.elementAt(hsize));
			}
    	}
    	History.setSize(sz);
    }
    /**
     * called to set the current move as the end of the move history.  This can be used
     * in conjunction with {@link #rewindHistory} to rewind a game to a given position.
     * and permanently undo the intervening moves.
     */
    public void truncateHistory()
    {	hidden.enterReviewMode();
    	setHistorySize(History.viewStep);
       	if(History.viewStep>0)
        	{
        	commonMove m = History.elementAt(History.viewStep-1);
        	commonMove next = m.next;
        	m.removeVariation(next);
        	}
       	doScrollTo(FORWARD_TO_END);
       	l.changeMove = getCurrentMove();	// inhibit turn change sound
    }
/**
 * rewind the game to a given position, usually preliminary to calling {@link #truncateHistory}
 * @param size
 */
    public void rewindHistory(int size)
    {
        if ((size >= 0) && (size < History.size()))
        {
            //System.out.println("Truncating history at step "+size);
        	//this is called both live and as replay from an opponent
        	// so use doScroll which doesn't check control 
            doScroll(size,"rewind");
            mutated_game_record = true;
            setHistorySize(size);
            doScroll(FORWARD_TO_END,"new variation");
         }
    }
    
/**
 * call this from your {@link #EditHistory} method to remove
 * a superfluous element at idx.  Note that this maintains both
 * the move tree in the commonMove elements, and the History list
 * which contains the current linear path to the current move in the tree.
 * 
 * Usually idx is the current last move in the history, and usually it has no successors.
 * This method works to remove elements that are not leaves of the tree, but that is
 * not usually something you want to do.  Routine use by EditHistory should always
 * remove leaves. 
 */
    public commonMove popHistoryElement()
    {
        //int size = History.size()-1;
        //commonMove msize = (commonMove)History.elementAt(size);
        //boolean review = msize.nVariations()>0;
    	int idx = History.size()-1;
    	commonMove midx = History.top();
    	
    	
        { String msg = "Pop "+midx;
          LogMessage(msg);
          rawHistory.addElement(new dummyMove(msg));
          if(G.debug() && (midx.op!=MOVE_EDIT)) { G.print(msg); }
        }
        
        // if next is not null, this is a move in the middle of a chain and we can't really remove
        // it without destroying the semantics of the continuation.  If it was at the end of a chain
        // then make it really disappear.
        if((idx>0) && (midx.next==null))
        {	commonMove pmidx = History.elementAt(idx-1);
        	pmidx.removeVariation(midx);
         }
        repeatedPositions.removeFromRepeatedPositions(midx);
        // note that this remove will leave the history with the last
        // element having a non-null "next" if we just removed the end of
        // a variation.  We get here from "edithistory" which should exit
        // and then the caller of edithistory will extend the history and set viewStep
        return(History.remove(idx,false));
    
     }
    /**
     * remove a history element without attempting to remove the effects
     * from the digest.  This is used by some games (ie; zertz) to remove
     * moves out of order which are actually independent and can be done
     * in either order.
     * @param idx
     * @return a commonMove
     */
    public commonMove popHistoryElement(int idx)
    {	int siz = History.size();
    	if(idx>=siz) { return(null); }
    	if(idx+1==siz) { return(popHistoryElement()); }
        else 
        	{ commonMove rem = popHistoryElement();
        	  commonMove val = popHistoryElement(idx);
        	  rem.digest = 0;		// when this is put back, the digest won't be correct.
        	  if (History.size() > 0)
              {	
                  commonMove m = History.top();
                  rem = m.addVariation(rem);
              }
        	  rem.addToHistoryAndExtend(History);
        	  return(val);
        	}
    }

    /**
     * remove a history element at an arbitrary index.  Unlike popHistoryElement(),
     * this also removes the effect from the board by unwinding and rewinding the board.
     * @param idx
     * @return the removed element
     */
    public commonMove removeHistoryElement(int idx)
    {	int step = History.viewStep;
     	commonMove val = hidden.removeHistoryElementRecurse(idx);
     	if(idx>0)
     	{	// we removed the element from somewhere back on the path, 
     		// but doing so left the digests incorrect
     		doScrollTo(idx-1);
     		if(step>idx) { step--; }
     		doScrollTo(step);
     	}
     	return(val);
    }
 
    /** draw the standard repeated positions warning message 
     * 
     * @param gc
     * @param digest
     * @param r
     */
    public boolean DrawRepRect(Graphics gc,long digest,Rectangle r)
    {
    	 	return DrawRepRect(gc,0,Color.black,digest,r);
    }
   public boolean DrawRepRect(Graphics gc,double rotation,Color textColor,long digest,Rectangle r)
   {
    	if(gc!=null)
    	{	
    		int nreps = repeatedPositions.numberOfRepeatedPositions(digest);
    		GC.setFont(gc,largeBoldFont());
    		if(nreps>1) 
    		{ GC.Text(gc,rotation,true,r,textColor,null,s.get(RepetitionsMessage,nreps)); 
    		  return true;
    		}
    	}
    	return false;
    }  

    /** handle the boilerplate editing options involving punting on a resign,
     * multiple starts or edits.
     * @param newmove
     * @return a move to add to the history, or null
     */
    public commonMove commonEditHistory(commonMove newmove)
    {	int idx = History.size() - 1;
    	commonMove rval = newmove;
    	if(idx>=0) 
    	{
    	commonMove m = History.elementAt(idx);
    	switch(newmove.op)
    	{
    	// MOVE_EDIT isn't included here, so if you have start+edit you continue to see the 
    	// edit until you hit start again.  This prevents "whoseturn mismatch".
    	case MOVE_START:
    		while(idx>=0)
    		{
    		// in a start, remove any number of previous start/edit/resign
    		switch(History.elementAt(idx).op)
    		{
    		default:
    			idx=-1; 
    			break;
    		case MOVE_START:
    		case MOVE_EDIT:
    		case MOVE_RESIGN:
    			popHistoryElement();
    			idx--;
    			}}
    		break;
    	case MOVE_DONE:
    		break;
    	case MOVE_RESIGN:
    		// multiple resigns, remove both
    		if(m.op==MOVE_RESIGN) { rval = null; }
    		// fall through
			//$FALL-THROUGH$
		default:
     		// anything other than "done" after a resign, remove the resign
    		if(m.op==MOVE_RESIGN) { popHistoryElement(); }
    		break;
    	}}
    	return(rval);
    }
    /** add to the history, but also edit the current move in progress
    so we don't clutter the history with extra steps.  This doesn't
    hurt anything, but it looks bad and makes the move slider slide.
    At the point we get there, the move has already been fully
    performed, so only the history vector itself is affected.
    @return true if the current move was added to history
    */
    public boolean AddToHistory(commonMove originalnewmove)
    { // add to history, but also edit history so redundant moves are eliminated
    	commonMove newmove = originalnewmove;
    	rawHistory.addElement(newmove);
        if(History.viewStep==-1) 
        	{ newmove = commonEditHistory(newmove);
        	  if(newmove!=null) { newmove = EditHistory(newmove); } 
         	}
        if(newmove==null) 
        { // in some games (ie: plateau) will have a "done" state that is optional, in which case
          // we can pass through a done state and then undo back to it.  In this case the done move
          // may have already been recorded as a repetition
          	{ String str = "edit: "+originalnewmove+" deleted";
          	  LogMessage(str);
          	  //if(G.debug()) { System.out.println(str); } 
          	  rawHistory.addElement(new dummyMove(str));
          	}
          repeatedPositions.removeFromRepeatedPositions(originalnewmove); 
       	  int sz = History.size();
    	  if(sz>0)
    	  {	  // removing elements from the history can result in backing out of a variation,
    		  // which extends the sequence instead of reducing it.  Consequently we can
    		  // go from non-review/end-of-variation to review/middle-of-variation by
    		  // deleting some moves in EditHistory
    		  commonMove oldm = History.elementAt(sz-1);
    		  if(oldm.next!=null)
    		  {	History.viewStep = sz;		// viewstep should point to the next item to be executed, which will be 
    		  						// the first item added.
    		  	mutated_game_record=true;
    		    oldm.extendHistory(History);
    		    // if we have removed a branch and re-extended the history,
    		    // the state of the real board may be different from what we
    		    // expect, especially if the move that caused the merge was a 
    		    // reset.  The simplest way to be sure it's all ok is to replay
    		    // the game to this point.  However, this also has consequences
    		    // for games like Jumbulaya, where some GUI actions don't change
    		    // the game record, but only prime the gui for the next action.
    		    if(resynchronizeOnUnbranch)
    		    {
    		    	boolean changed = resynchronizeBoard(originalnewmove);
    		    	if(changed && G.debug())
    		    	{
    		    		G.print("resynchronizeOnUnbranch did something good");
    		    	}    		    
    		    }
    		  }
    	  }     
         if (History.viewStep >= History.size())
          {
        	 History.viewStep = -1;
        	 
          }
        }
        else //no editing needed, annotate the move with its position in the history and add it.
        {
            rewindHistory(newmove.index());

            int hsize = History.size();
            newmove.setIndex(hsize);

            if (hsize > 0)
            {	
                commonMove m = History.top();
                newmove = m.addVariation(newmove);

                //System.out.println("V "+m+" -> "+newmove+" -> " +m.next);
            }
            {
             int hs = newmove.addToHistoryAndExtend(History);
             if (hs > 0)
             	{
                History.viewStep = hs;
             	}}
             // be careful to preserve the elapsedtime that might have
             // originally been recorded on a variaiton we are re-entering
             if(newmove!=originalnewmove && newmove.elapsedTime()<=0)
            	{ newmove.setElapsedTime(originalnewmove.elapsedTime()); }
        }
        return(newmove!=null);
    }
    /**
     * this is a conservative hack.  Resynchronizing "ought to" be unnecessary, but
     * it was historically in place for a long time.  It can be turned off in cases
     * where the resync causes harm, as is the case for Jumbulaya.
     */
    public boolean resynchronizeOnUnbranch = true;
    /**
     * rack back to the beggining and back to the current location.
     * this can be called when the state of the board might be confused
     * @param deletedMove
     * @return true if the new digest differs, so we probably did some good.
     */
    public boolean resynchronizeBoard(commonMove deletedMove)
    {
    	int step = History.viewStep;
    	long dig1 = getBoard().Digest();
    	rawHistory.addElement(new dummyMove("vcr:@"+step+" edit rescroll"));
    	doWayBack(replayMode.Replay);
    	doScrollTo(step);
    	long dig2 = getBoard().Digest();
	    generalRefresh();
	    return dig2!=dig1;
    }
    
    /* hack to verify that the entire tree has times associated
    private void verify(commonMove m,String w)
    {
    	G.Assert(m.op==MOVE_START || m.elapsedTime()>0,"no "+w);
    	if(m.next!=null) { verify(m.next,w); }
    	int n = m.nVariations();
    	for(int i=0;i<n;i++)
    	{
    		verify(m.getVariation(i),w);
    	}
    }
    */
    public boolean singlePlayer() 
    {	int nPlayers = sharedInfo.getInt(PLAYERS_IN_GAME,0);
    	return ( (nPlayers<2) || 
    				( sharedInfo.get(ROBOTGAME)!=null)
    					&& nPlayers==2);	
    }
    
    private VncServiceProvider regService = null;
    private RpcInterface rpcService = null;
    
    private SeatingChart seatingChart = null;
    /**
     * @return true if the seating chart provides seats on at least 3 sides
     */
    public boolean seatingAround() { return(seatingChart.seatingAround()); }
    /**
     * @return true of the selected seating has players across from one another
     */
    public boolean seatingFaceToFace() { return(seatingChart.seatingFaceToFace()&&!seatingChart.seatingAround()); };
    
    public boolean seatingFaceToFaceRotated() { 
    	if(seatingFaceToFace())
    	{	commonPlayer pl = getPlayerOrTemp(0);
    		int qt = G.rotationQuarterTurns(pl.displayRotation);
    		return((qt&1)!=0);
    	}
    	return(false);
    }
    /**
     * @return true if the seating chart specifies player positions
     */
    public boolean plannedSeating() { return(seatingChart.plannedSeating()); }
    /**
     * get the color map specified at launch, before possible player color swaps.
     * the default map is [0,1,2 ...]
     * @return an array of integers, which remap the colors 
     */
    public int[]getStartingColorMap() 
    { 	Object map = sharedInfo.get(KEYWORD_COLORMAP);
    	if(map instanceof String) { map = G.parseColorMap((String)map); }
    	return((int[])map);
    }
  
    public void startFirstPlayer()
    {
    	int first = -1;
    	if(gameInfo!=null)
    	{
    	if(gameInfo.variableColorMap)
    		{ first = sharedInfo.getInt(FIRSTPLAYER,0); }
       	if(first<0 && (gameInfo.colorMap!=null)) 
		{ // if the first player is not specified explicitly, find the player playing color 0
       	  BoardProtocol b = getBoard();
		  int map[] = b.getColorMap();
		  if(map!=null) 
		  	{ for(int lim=b.nPlayers(),i=0;i<lim;i++)
		  		{ if(map[i]==0) { first = i; }
		  			}
		  		}
		  	}
    	}
    	if(first<0) { first=0; }
    	PerformAndTransmit(reviewOnly?EDIT:"Start P"+first, false,replayMode.Replay);
    }
    
    
    public boolean canTrackMouse()
    {	return( reviewOnly 
    			|| ( started
    				&& ( l.my.isSpectator() 
    					? mutable_game_record		// inhibit spectator tracking during live games
    					: true
    					)));
    }
    
	public String mouseMessage() 
    { 	String mm = annotationMenu.mouseMessage();
    	if(mm==null) 
    		{ mm=l.mouseMessage; 
    		  l.mouseMessage = null; 
    		  return(mm);
    		}
    	return mm;
    }
    
    /**
     * this is called from mouse activity to register the current whereabouts of the mouse.
     * it is responsible for rate-limiting the tracking of opponents' mice
     */
    public void trackMouse(int x, int y)
    {	
    	if(annotationMenu.trackMouse(x,y)) { }
    	else
    	{
    	long now = G.Date();
        int mouseObj = getMovingObject(null);
        boolean newState = mouseObj!=l.mouseObject;
        l.mouseObject = mouseObj;
        String oldzone = l.my.mouseZone!=null ? l.my.mouseZone : "";
        if (started
        		&& (newState||((now - l.lastMouseTime) > 250)) // rate limit to 1 per 1/4 second
                 && canTrackMouse())
        {	Point pt = new Point(0,0);
            String zone = encodeScreenZone(x, y,pt); // not raw coordinates but normalized
            if (zone != null)
            {   int closestX = G.Left(pt);
                int closestY = G.Top(pt);                

                if ((closestX != l.my.mouseX) || (closestY != l.my.mouseY) || !oldzone.equals(zone))
                {
                	l.lastMouseTime = now;
                	l.mouseMessage = G.concat(NetConn.SEND_GROUP+KEYWORD_TRACKMOUSE," ",zone," "
                    		, closestX , " " ,closestY, " " , mouseObj);
                }
            }
        }}
    }
    public enum ViewSet { Normal(0),Reverse(1),Twist(2);
    	int value;
    	ViewSet(int v) { value = v; }
    }
    public ViewSet currentViewset = ViewSet.Normal;
 

 
    public int getAltChipset() { return(currentViewset.value); }


    private TimeControl timeControl = new TimeControl(TimeControl.Kind.None);
    private TimeControl futureTimeControl = new TimeControl(TimeControl.Kind.None);
    public TimeControl timeControl() { return(timeControl); }
    public String datePlayed = "";
    Session.Mode gameMode = Session.Mode.Game_Mode;
    // general initialization
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
        
        SeatingChart chart = (SeatingChart)sharedInfo.get(SEATINGCHART);
        turnBasedGame = (TurnBasedViewer.AsyncGameInfo)info.get(TURNBASEDGAME);
        offlineGame = info.getBoolean(OFFLINEGAME,true);
        if(chart!=null)
        {
        	if(chart.seatingFaceToFace()) { currentViewset = ViewSet.Reverse; }
        }
        else if(G.isTable()) { currentViewset = ViewSet.Reverse; }
 
        TimeControl tc = (TimeControl)info.get(TIMECONTROL);
        if(tc==null) { tc = new TimeControl(TimeControl.Kind.None);}
        timeControl = tc;
        futureTimeControl = tc.copy();
        datePlayed = "";
        setCanvasRotation(info.getInt(ROTATION,0));
        if(info.getBoolean(REVIEWONLY,false)) { gameMode = Session.Mode.Review_Mode; }
        gameMode =(Session.Mode)info.getObj(MODE,gameMode);
        l.tournamentMode = info.getBoolean(TOURNAMENTMODE,false);
        
        Random.setNextIntCompatibility(false);
        gameInfo = info.getGameInfo();
        if(G.isSimulator() || !G.isTouchInterface()) 
    	{ 
        	l.zoomButton = myFrame.addAction("Zoom Up",deferredEvents);
        	l.unzoomButton = myFrame.addAction("Un Zoom",deferredEvents);
    	}
        commonPlayer myPlayer = (commonPlayer)info.get(MYPLAYER);
        setActivePlayer(myPlayer);
        G.Assert(getActivePlayer()!=null,"my player not supplied");
        if(gameInfo!=null)
        {  String name = gameInfo.gameName;
           if(G.debug()) { name += " "+myPlayer.trueName(); }
           myFrame.setTitle(name);
        }
        // gameicon is initialized as part of the contract of preloadImages(), even
        // if the images were already loaded.
        myFrame.setIconAsImage(gameIcon);
        
        hidden.controlToken = mutable_game_record = allowed_to_edit = reviewOnly = 
        		
        		(gameMode==Session.Mode.Review_Mode);
        		
        use_grid = reviewOnly || Config.Default.getBoolean(Config.Default.showgrid);
        
        mutated_game_record = false;
        l.playTickTockSounds = Config.Default.getBoolean(Config.Default.ticktock);
        gridOption = myFrame.addOption(s.get(ShowGridAction), use_grid,deferredEvents);
        gridOption.setForeground(Color.blue);
        l.tickTockOption = myFrame.addOption(s.get(TickTockAction),l.playTickTockSounds,deferredEvents);
        l.tickTockOption.setForeground(Color.blue);
        
        boardMax = reviewOnly  ? false : Default.getBoolean(Default.boardMax);
        boardMaxCheckbox = myFrame.addOption(s.get(BoardMaxEverywhere),boardMax,deferredEvents);
        boardMaxCheckbox.setForeground(Color.blue);
        selectedLayout.boardMax = boardMax;
        
        l.mouseCheckbox = myFrame.addOption(s.get(TrackMice),true,null);
        if(enableAutoDone())
        {	autoDone = Default.getBoolean(Default.autodone);
        	autoDoneCheckbox = myFrame.addOption(s.get(AutoDoneEverywhere),autoDone,deferredEvents);
        }

        boolean viewer = reviewOnly;


       if(viewer || singlePlayer() || isOfflineGame()) { setSeeChat(false); }
       if (viewer)
        {
        	if(G.isCodename1())
        		{ myFrame.setCanSavePanZoom(deferredEvents); 
        	}
           	hidden.saveGame = myFrame.addAction(SaveSingleGame,deferredEvents); 
           	hidden.saveCollection = myFrame.addAction(SaveGameCollection,deferredEvents);
        	hidden.loadGame = myFrame.addAction(LoadGameFile,deferredEvents);
            	hidden.gamesMenu = new XJMenu(SelectAGameMessage,true);
                myFrame.addToMenuBar(hidden.gamesMenu);
                hidden.gamesMenu.addItemListener(deferredEvents);
                
            if(extraactions || G.debug())
            {
        	hidden.showText = myFrame.addAction(s.get("Show Raw Text"),deferredEvents);
        	hidden.replayCollection = myFrame.addAction(ReplayGameCollection,deferredEvents); 
            hidden.replayFolder = myFrame.addAction(ReplayGameFolder,deferredEvents);
            }
        	}

       if (extraactions)
        {
           setNetConsole = myFrame.addAction("Set Net Logger",deferredEvents);
    		hidden.gameTest = myFrame.addAction("In game test",deferredEvents);
        	l.layoutAction = myFrame.addAction("check layouts",deferredEvents);
        	{
        	JMenu robotMenu = new XJMenu("Robot Actions",true);
        	myFrame.addToMenuBar(robotMenu);
        	
        	hidden.treeViewerMenu = myFrame.addAction(robotMenu,"view UCT search tree",deferredEvents);
        	hidden.alphaViewerMenu = myFrame.addAction(robotMenu,"view Alpha-Beta search tree",deferredEvents);
        	hidden.truncateGame = myFrame.addAction(robotMenu,"truncate game",deferredEvents);
        	hidden.evalRobot = myFrame.addAction(robotMenu,"start evaluator",deferredEvents);
        	hidden.testRobot = myFrame.addAction(robotMenu,"test one robot move",deferredEvents);
        	hidden.startRobot = myFrame.addAction(robotMenu,"start a robot",deferredEvents);
        	hidden.robotLevel0 = myFrame.addAction(robotMenu,"select robot",deferredEvents);
        	hidden.selectRobotThread = myFrame.addAction(robotMenu,"select thread...",deferredEvents);
        	hidden.stopRobot = myFrame.addAction(robotMenu,"stop all robots",deferredEvents);
        	hidden.pauseRobot = myFrame.addAction(robotMenu,"pause all robots",deferredEvents);
        	hidden.resumeRobot = myFrame.addAction(robotMenu,"resume all robots",deferredEvents);
        	hidden.saveVariation = myFrame.addAction(robotMenu,"save current variation",deferredEvents);
        	hidden.runGameDumbot = myFrame.addAction(robotMenu,"play against dumbot",deferredEvents);
        	hidden.runGameSelf = myFrame.addAction(robotMenu,"play against self",deferredEvents);
        	hidden.train = myFrame.addAction(robotMenu, "run training",deferredEvents);
        	hidden.stopTrain = myFrame.addAction(robotMenu,"stop training",deferredEvents);
        	}
        	hidden.startShell = myFrame.addAction("start shell",deferredEvents);
            hidden.editMove = myFrame.addAction("edit this game",deferredEvents);
            l.auxSliders = myFrame.addOption("Use aux sliders",false,deferredEvents);
           	hidden.alternateBoard = myFrame.addOption("Show Alternate Board", false,deferredEvents);
            hidden.saveAndCompare = myFrame.addAction("Save/Compare Position",deferredEvents);
            if(allPlayersLocal())
            {
            if(REMOTEVNC) { 
            regService = new VncRemote(s.get(RemoteFor,gameName()),painter,this);
            	VNCService.registerService(regService);
            }
            if(REMOTERPC)
            {
            rpcService = new RpcRemoteServer(s.get(RemoteFor,gameName()),this,-1);
            RpcService.registerService(rpcService);
            }
            }
            
        }
        
        hidden.showSgf = myFrame.addAction(ShowSGFMessage,deferredEvents);
        if(!G.isCheerpj())
        {
        hidden.emailSgf = myFrame.addAction(EmailSGFMessage,deferredEvents);
        }

        if (canUseDone())
        {
            hidden.resignAction = myFrame.addAction(s.get(RESIGN),deferredEvents);
            hidden.offerDrawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);     
        }
        if(!G.useTabInterface())
        {

        l.paperclipMenu = new IconMenu(StockArt.PaperClip.image);
        myFrame.addToMenuBar(l.paperclipMenu,deferredEvents);
        l.paperclipMenu.setVisible(true);
        
        l.paperclipRestoreMenu = new IconMenu(StockArt.PaperClipSide.image);
        myFrame.addToMenuBar(l.paperclipRestoreMenu,deferredEvents);
        l.paperclipRestoreMenu.setVisible(false);

        
        }
        seatingChart = (SeatingChart)info.get(SEATINGCHART);
        if(seatingChart==null) 
        	{ seatingChart = SeatingChart.defaultSeatingChart(info.getInt(PLAYERS_IN_GAME,2)); 
        	}
        SoundManager.loadASoundClip(turnChangeSoundName);
        SoundManager.loadASoundClip(beepBeepSoundName);

     }

    /**
     * reset just the game history, leaving the player list and board state unchanged.
     */
    public void resetHistory()
    {
    	History.viewStep = -1;
    	History.viewTurn = null;
    	History.viewMoveNumber = -1;
        History.clear();
        rawHistory.clear();
 
    }

    /* used when loading a new game */
    public void doInit(boolean preserve_history)
    {      	
    	if(!preserve_history)
    	{
    	resetHistory();
    	BoardProtocol b = getBoard();
        if(b!=null) { adjustPlayers(b.nPlayers()); }
        commonPlayer.initPlayers(players,reviewOnly);
    	}
    for(int i=0;i<players.length;i++)
    {
    	commonPlayer pl = players[i];
    	if(pl!=null) { pl.setReviewTime(-1); }
    }
    l.gameOverlayEnabled = true;
    repeatedPositions.clear();
    }
    public void stopRobots()
    {
        for (int n = 0; n < players.length; n++)
        {
           commonPlayer player = players[n];
            if (player != null) 
            {
                player.stopRobot();
            }
        }
        getActivePlayer().stopRobot();
        if(l.extraBot!=null) { l.extraBot.StopRobot(); }
    }
    public void pauseRobots()
    {
        for (commonPlayer player : players)
        {
            if (player != null) 
            {
                player.pauseRobot();
            }
        }
        getActivePlayer().pauseRobot();
        if(l.extraBot!=null) { l.extraBot.Pause(); }
    }    
    
    public void resumeRobots()
    {
        for (commonPlayer player : players)
        {
           if (player != null) 
            {
                player.resumeRobot();
            }
        }
        getActivePlayer().resumeRobot();
        if(l.extraBot!=null) { l.extraBot.Resume(); }
    }

    public TreeViewer getTreeViewer() { return(hidden.treeViewer); }
    
    
    public void doEndGameOnTime()
    {   
    	PerformAndTransmit(GAMEOVERONTIME,false,replayMode.Replay);
    }
   
    public void adoptNewTimeControl()
    {
    	timeControl.copyFrom(futureTimeControl);
    }
    /** this is called from the run loop to actually
     * perform actions that were deferred by a deferredEventHandler
     * <p>
     * this method handles the standard events added in the standard
     * action and options menu.  It should be wrapped as necessary to
     * handle things added by your viewer subclass.
     * @param target will be a menu item 
     * @return true if the target event was handled.
     */
     public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command)
        					|| hidden.handleDeferredEvent(target);
        if(!handled)
        {
        if(futureTimeControl.handleDeferredEvent(target,command)) 
        {	//update the other client
        	performClientMessage("+" + TimeId.ChangeFutureTimeControl.name()+" "+futureTimeControl.print(),false,true);
        	handled = true;
        }
        else if(numberMenu.selectMenu(target,this)) { return true; }
    	else if(annotationMenu.selectMenu(target)) { return true; }
        else if(hidden.vcrVarPopup.selectMenuTarget(target))
    	{	chooseVcrVar((commonMove)(hidden.vcrVarPopup.rawValue));
    		handled = true;
        }
    	else if (target == autoDoneCheckbox)
    	{
    		autoDone = autoDoneCheckbox.getState();
    	}
    	else if (target == boardMaxCheckbox)
    	{
    		boardMax = boardMaxCheckbox.getState();
    		selectedLayout.boardMax = boardMax;
    		Config.Default.setBoolean(Config.Default.boardMax,boardMax);
    		resetBounds();
    	}
        else if (target == l.tickTockOption)
        {
        	l.playTickTockSounds = l.tickTockOption.getState();
        	Config.Default.setBoolean(Config.Default.ticktock,l.playTickTockSounds);
        	handled = true;
        }
        else if(target == l.layoutAction)
        {
        	checkLayouts();
        	handled = true;
        }
        else if("savepanzoom".equals(command) || (target == l.paperclipMenu))
        {	
        	doSavePanZoom("");
        	if(l.paperclipRestoreMenu!=null) { l.paperclipRestoreMenu.setVisible(true); }
        	myFrame.setHasSavePanZoom(true);
         	handled = true;
        }
        else if("restorepanzoom".equals(command) || (target == l.paperclipRestoreMenu))
        {	// use the component x,y because we lost the event xy
        	doRestorePanZoom(((SizeProvider)target).getX(),((SizeProvider)target).getY());
        	handled = true;
        }
        else if (target == gridOption)
        {	use_grid = gridOption.getState();
            Config.Default.setBoolean(Config.Default.showgrid,use_grid);
           generalRefresh();
           handled = true;
        }

        else if (target == l.auxSliders)
        {	
        	useAuxSliders = l.auxSliders.getState();
        	handled = true;
        }
        else if(target==setNetConsole)
        {   ConnectionManager myNetConn = (ConnectionManager)sharedInfo.get(NETCONN);
        	   boolean wasOn = G.getPrinter()!=null;
        	   if(wasOn) { G.print("Net logger off"); setNetConsole.setText("Start Net logger"); G.setPrinter(null); }
        	   else 
        	   { ShellProtocol m = (myNetConn==null) ? null : myNetConn.getPrintStream();
        	     if(m==null)
        	     { G.print("Not connected, can't net log"); 
        	     }
        	     else { G.setPrinter(m); G.print("Net Logger on"); 
        	     setNetConsole.setText("Stop Net logger"); }
        	     }
        	   return(true);
        }
        }
        return (handled);
    }


    private String gameRecordString()
    {
    	Utf8OutputStream lbs = new Utf8OutputStream();
        PrintStream os = Utf8Printer.getPrinter(lbs);
        printGameRecord(os, datePlayed,"");
        os.flush();
        return(lbs.toString());
    }
    
    
    public void doSavePanZoom(String specs)
    {	int sx = getSX();
    	int sy = getSY();
    	double z = getGlobalZoom();
    	double r = getRotation();
    	String pz = "globalzoomrot "+z+" "+r+" globalx "+sx+" globaly "+sy;
    	if(zoomRect!=null)
    	{
    		pz += " tilesize "+zoomRect.value
        			+" boardx "+board_center_x
        			+" boardy "+board_center_y;
    	}
    	l.panAndZoom.pushNew(pz + specs);
    	if(l.paperclipRestoreMenu!=null) { l.paperclipRestoreMenu.setText(""+l.panAndZoom.size()); }
    	myFrame.setHasSavePanZoom(true);
    	playASoundClip(Keyboard.clickSound,50); 
    }

    public void doRestorePanZoom(String specs)
    {	StringTokenizer tok = new StringTokenizer(specs);
    	while(tok.hasMoreTokens())
    	{
    		String key = tok.nextToken();
    		String val = tok.nextToken();
    		if("globalx".equals(key)) { setSX(G.IntToken(val)); }
    		else if("globaly".equals(key)) { setSY(G.IntToken(val)); }
    		else if("globalzoomrot".equals(key)) { changeZoom(G.DoubleToken(val),G.DoubleToken(tok.nextToken())); }
    		else if("boardx".equals(key)) { board_center_x = G.DoubleToken(val); }
    		else if("boardy".equals(key)) { board_center_y = G.DoubleToken(val); }
    		else if("tilesize".equals(key)) { zoomRect.setValue(G.DoubleToken(val)); }

    	}
    	if(zoomMenu!=null)
    	{
    	boolean zoomed = getGlobalZoom()>=MINIMUM_ZOOM;
    	zoomMenu.changeIcon(zoomed ? StockArt.UnMagnifier.image : StockArt.Magnifier.image,!zoomed);
    	sliderMenu.setVisible(zoomed);
    	}
    	resetBounds();
  	  	resetLocalBoundsIfNeeded();
    	generalRefresh();
    }
    private String summarize(String e)
    {	String res = "";
    	StringTokenizer tok = new StringTokenizer(e," ");
    	while(tok.hasMoreTokens())
    	{
    		String m = tok.nextToken();
    		int ind = m.indexOf('.');
    		if(ind>=0)
    		{
    			if(ind+3<m.length())
    			{
    				m = m.substring(0,ind+3);
    			}
    		}
    		res += m+" ";
    	}
    	return(res);
    }
    public void doRestorePanZoom(int x,int y)
    {	PopupManager pop = hidden.panzoomPopup;
    	pop.newPopupMenu(this,deferredEvents);
    	for(int i=0;i<l.panAndZoom.size();i++)
    	{	String e = l.panAndZoom.elementAt(i);
    		pop.addMenuItem((i+1)+": "+summarize(e),e);
    	}
    	pop.useSimpleMenu = false;
    	pop.show(x+30,y+10);
    }
    /** 
     * this becomes sequence of tokens that is used to identify
     * and initialize the game when being restarted or spectated on.
     * It will be parsed by your {@link #performHistoryInitialization}
     * method.
     * @return a string representing the game type
     */
    public abstract String gameType();
    
    public abstract String sgfGameType();
    
    public String sgfGameVersion() { return("1"); }

    /**
     * form the canonical version of the game record for use during the game.  This is used to
     * bring spectators up to date, to restart interrupted games, and to re synchronize players
     * who are rejoining a game in progress.  Normally this will contain a stripped down
     * version of what will eventually be the sgf record of the game, but importantly,
     * the server doesn't care what it looks like, as long as all the clients agree and
     * like it.  This is paired with {@link #useStoryBuffer} which must consume what
     * this produces
     */
    public void formHistoryString(PrintStream os,boolean includeTimes,boolean startAtZero)
    {
        os.print(gameType() + " " + 0 );

        if (History.size() > 0)
        {
            commonMove mv = History.elementAt(0);
            mv.formHistoryTree(os,mv.finalFilter(),startAtZero ?-1 : 0,includeTimes);
        }
    } 
    /**
     * form the canonical version of the game record for use during the game.  This is used to
     * bring spectators up to date, to restart interrupted games, and to resynchronize players
     * who are rejoining a game in progress.
     * 
     * includeTimes is a compatibility bridge to old versions of the server
     * which do not have special logic to filter out per-move times. If the
     * server doesn't know about +T nn times, don't send them.
     */
    private String formStoryString(boolean includeTimes, boolean startAtZero)
    {
    	Utf8OutputStream b = new Utf8OutputStream();
        PrintStream os = Utf8Printer.getPrinter(b);
        formHistoryString(os,includeTimes,startAtZero);
        os.flush();
        String str = b.toString();
        return (str);
    }
    
    public void setSimultaneousTurnsAllowed(boolean v)
    {
    	getBoard().setSimultaneousTurnsAllowed(v);
    }

    /**
     * return true if simultaneous game actions are allowed right now.  This is
     * used by the user interface to control prompts and actions the player is
     * looking at, which may be in review mode.
     *
     * @return true if simultaneous game actions are allowed right now
     */
    public boolean simultaneousTurnsAllowed()
    {	
    	return(getBoard().simultaneousTurnsAllowed());
 
    }
    /**
     * this should return true when out-of-sequence moves will
     * actually occur.  The effect of "true" is that communications with
     * the server are held to a baseline and the players are allowed to overwrite each
     * other's view of the most recent move.  Note that this should have the
     * same value when the user interface is scrolled back during a game.
     */
    public RecordingStrategy gameRecordingMode()
    {	BoardState state = reviewMode() ? History.pre_review_state : getBoard().getState();
    	// state can be null if in intialization
    	return(state==null
    			? RecordingStrategy.All 
    			: getBoard().simultaneousTurnsAllowed(state)
    				? RecordingStrategy.Fixed 
    				: RecordingStrategy.All );
    }
    
    /**
     * for games which allow simultaneous moves, there is an uncertainty in the order
     * of events as seen by various players, which would make the game records different
     * for different players.  We combat this by segregating all the uncommitted moves
     * to a section at the very end of the game record, and exclude it from recording
     * in the permanent game record.
     * 
     * gameHasEphemeralMoves() indicates that the game needs this mechanism, and 
     * causes the default implementations of {@link formEphemeralHistoryString}
     * and {@link #useEphemeraBuffer} to add ephemeral moves as a second block
     * of moves in game records.
     * 
     * @return
     */
    public boolean gameHasEphemeralMoves() { return(false); }
    
    /** describe the ephemeral (ie; rapidly changing) state of the game.  This method
     * usually returns player time information, but can include other things if they
     * are needed to reconstruct a game.  Normally this will include at least the
     * player times, and the current scroll position.   if {@link #gameHasEphemeralMoves}
     * is true, it will also create a second block corresponding to the ephemeral moves
     * to be added to the end.
     * The consumer of this string is the  {@link #useEphemeraBuffer} method. If you
     * override or wrap this method, do the same to useEphemeraBuffer
     */
    private String formEphemeralHistoryString()
    {
    	String eph = formEphemeralPlayerHistoryString();
    	if(gameHasEphemeralMoves())
    	{
    		eph += formEphemeralMoveString();
    	}
    	return(eph);
    }
    /**
     * form just the history tree for ephemeral moves.  This is called
     * from {@link #formEphemeralHistoryString}. This can be wrapped
     * or overridden in cooperation with {@link useEphemeralMoves} to alter
     * the standard behavior
     * 
     * @return
     */
    public String formEphemeralMoveString()
    {
    	Utf8OutputStream b = new Utf8OutputStream();
    	PrintStream os = Utf8Printer.getPrinter(b);
        if (History.size() > 0)
        {	
            commonMove mv = History.elementAt(0);
            // the filter removes ephemeral moves and renumbers the moves
            mv.formHistoryTree(os,mv.ephemeralFilter(),0,false);
           
        }
    	os.print(" "+KEYWORD_END_HISTORY);
        os.flush();
        return(b.toString());
    }
    /**
     * form the ephemeral player info and position part of the game record 
     * this normally consists of time times for each player + "scroll" + position
     * @return
     */
    public String formEphemeralPlayerHistoryString()
    {	StringBuilder time = new StringBuilder();
    	String space = "";
    	for(int i=0;i<players.length;i++)
    	{	commonPlayer pl = players[i];
    		time.append(space);
    		time.append((pl==null) ? 0 : pl.elapsedTime);
    		space = " ";
    	}
    	// 3/2013 add the current scroll position to the sting, so spectators joining
    	// in review mode are auto scrolled to the current view position.  This prevents
    	// them becoming confused of the next transmitted action is a real move rather
    	// than another repositioning.  Don't make this optional, the number of tokens
    	// we produce/consume should always be a constant
    	time.append(" ");
    	time.append(KEYWORD_SCROLL);
    	time.append(" ");
    	time.append(getReviewPosition());
    	return(time.toString());
    }
    
    /**
     * 
     * accept a message, presumably incoming from the other player.
     * return true if it's one we recognized and handled.  Moves
     * may arrive while we are reviewing and not prepared to act
     * on them immediately.
     *
     * @param st
     * @return a parsed move
     */
    public abstract commonMove ParseNewMove(String st, int player);

       
    /** true if there are unprocessed messages, usually as a result 
     *  of messages arriving while in temporary review mode
     * @return true of there are deferred Messages waiting to be processed
     */
    public boolean hasDeferredMessages()
    {	return(l.deferredMessages.size()>0);
    }
    /**
     * incoming messages from a remote client are added to the message queue
     * to be replayed locally.  This is the same process that is used to update
     * the game in normal networked play.
     * @param str
     * @param player
     */
    public void deferMessage(String str,int player)
    {
    	if(str!=null) 
    	{ 
    		l.deferredMessages.addElement(new DeferredMessage(player,str)); 
    		wake();
    	}
    }
    /** incoming messages from RPC screens are added to the message queue
     * 
     * @param op
     * @param not
     * @param str
     * @param player
     */
    public void deferMessage(RpcInterface.Keyword op,SimpleObserver not,String str, int player)
    {
    	l.deferredMessages.addElement(new DeferredMessage(op,not,player,str)); 
    }
    /**
     * when incoming messages arrive from one remote client, it is replayed
     * locally here on the main screen, then it needs to be transmitted to
     * any other siblings that are also watching the main screen.
     * @param from
     * @param str
     */
    public void notifySiblings(RpcRemoteServer from,String str)
    {
    	setChanged(from,str);
    }
    private String initialization = null;
    /**
     * called from RPC clients, this uses the initialization
     * provided by the remote client.  It's done this way to
     * make it happen in the normal game process.
     * 
     * @param str
     */
    public void saveInitialization(String str)
    {
    	initialization = str;
    }
    /**
     * do a full initialization from a saved story buffer
     */
    public void useInitialization()
    {
    	String in = initialization;
    	if(in!=null)
    		{ initialization = null;
    		  StringTokenizer tok = new StringTokenizer(in);
    		  String cmd = tok.nextToken();
    		  if(RpcInterface.Keyword.Complete.name().equals(cmd))
    		  {
    			  useRemoteStoryBuffer(null,tok);
    			  startPlaying();
    		  }
    		}
    }
    public void runRemoteViewer()
    {
    	if((remoteViewer!=NoRemoteViewer) && started)
    	{	
    		ParseMessage(null,remoteViewer);
    	}
    }
    
    /**
     * parse an incoming "viewer" message from another player.
     */
    public boolean ParseMessage(String str, int player)
    {	// this is a little convoluted because we want to defer execution
    	// if the user has scrolled back during a game.  The old logic parsed
    	// the message then added it to the history, so the replay at the
    	// end of reviewmode would execute it.  The flaw with this is that
    	// the history replay doesn't do editing, so actions like a reset
    	// did their actions but didn't edit the history.  This left the
    	// history harmlessly confused, except for incremental recording
    	// got a little confused.
    	//
    	// the new logic defers ALL incoming moves, then replays all deferred
    	// moves, including the current one.  If in review mode, it stays deferred
    	// but is replayed at the end of review mode.
    	//
    	// can be called with st==null to only parse existing deferred
    	// messages.
    	//
    	if(str!=null)
    		{l.deferredMessages.addElement(new DeferredMessage(player,str));		// add to the list
    		}
    	boolean some = l.handleDeferredMessages(); // do the list if we're live, or do nothing if we're in review
    	
    	boolean has = hasDeferredMessages();
    	if((str!=null) && has)	
        {	if(!l.deferWarningSent)
        	{
        	if(theChat!=null) { theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_LOBBY_CHAT,
                s.get(MovesComingIn));}
        	l.deferWarningSent = true;
        	}
         }
    	else { l.deferWarningSent = false; }
    	
        return(some);
    }
    public boolean ParseEcho(String str)
    {
    	return(true);
    }

    /**
     * return a censored version of shortMoveText.
     * This is a method of the viewer, so the viewer can apply knowledge from the
     * current state of the game in progress.
     * The default version of this method calls {@link online.game.commonMove#shortMoveText}
    *  @param sp the move to be stringified
     * @param index the current index into the move history
     * @return a Text object
     */
    public Text censoredMoveText(SequenceElement sp,int index)
    {
    	return(shortMoveText(sp));
    }
    public Text shortMoveText(SequenceElement sp) {
		return sp.shortMoveText(this);
	}
    /**
     * colorize a string and return a Text with the result.  This is used
     * to substitute icons for words, or translate the words, in the string;
     * or to otherwise change the presentation of the string to something other than a plain 
     * fonted string.  See {@link lib.Text#colorize}
     * @param str
     * @return the new Text object
     */
    public Text colorize(String str)
    {	return(TextChunk.create(str));
    }
    // use this to skip major computation when zoomed up
    public boolean rectangleIsVisible(Rectangle r)
    {	int xp = getSX();
    	int yp = getSY();
    	if(	(G.Bottom(r)<yp)
    		|| (G.Right(r)<xp)
    		|| G.Left(r)>=(getWidth()+xp)
    		|| (G.Top(r)>=(getHeight()+yp))
    			)
    	{ 
    	  return(false); 
    	}
    	return(true);
    		
    }

  

    public void shutDown()
    {	super.shutDown();
    	stopped = true;
    	History.setSize(0);
        rawHistory.setSize(0);
        l.disposeHiddenWindows();
        VNCService.unregisterService(regService);    
        RpcService.unregisterService(rpcService);
   }

    public boolean canStartRobotTurn(commonPlayer p)
    {	return((p != null)
    	&& (p.robotPlayer!=null)
    	&& (p.robotRunner == getActivePlayer())
    	&& !reviewMode() 
    	&& !GameOverNow() 
    	&& (p.robotRunning()==null)
    	&& !l.drawingGameOverlay			// not in the endgame on time dialog
    	&& !hasDeferredMessages());
    }
    //
    // there's a rare glitch where the robot can be triggered twice, one example
    // is  U!HV-Dumbot-wls-2022-06-30-2206 move 31
    // TODO: fix the rare "double robot" glitch
    //
    public void startRobotTurn(commonPlayer p)
    {
        if (canStartRobotTurn(p))
        {	//G.print("Start turn for "+p+" by "+my);
            p.startRobotTurn();
        }
    }
    
    public boolean allRobotsIdle()
    {
        for (commonPlayer p : players)
        {
            if ((p != null) 
            		&& (p.robotPlayer != null) 
            		&& (p.robotRunning()!=null)) //waiting or waiting for color
            {
                return (false);
            }
        }

        return (true);
    }
    /**
     * return true if the player's clocks ought to be ticking.
     * 
     * @return
     */
    public boolean timersActive()
    {
       	BoardProtocol bb = getBoard();
    	if(bb!=null)
    	{
    	BaseBoard.BoardState state = bb.getState();
    	return(! ( state.Puzzle() || state.GameOver()));
    	}
    	return false;
    }
    
    public void updatePlayerTime(long increment,commonPlayer p)
    {	if((p!=null) && timersActive())
    	{
     	long newtime = (p.elapsedTime + increment);
        p.setElapsedTime(newtime);
        if(principleTimeExpired(p)) 
        	{
        	if(p.principleTimeExpired<0)
        		{ p.principleTimeExpired = hidden.numberOfCompletedMoves(p); 
        		}
        	}
        else { p.principleTimeExpired = -1; }
        if(!p.isSpectator()) { doTime(p,myFrame.doSound()); }
    	}
    }
    public void doTruncate()
    {
        if (reviewMode())
        {
            truncateHistory();
        }
    }

    /** 
     * get the alternate display board from a robot that is currently running.
     * Note that the robot's board may not be happy if used for display when
     * the robot is not stopped at a breakpoint.
     * @return an alternate board
     */
    public BoardProtocol disB()
    {
        if (useAlternateBoard)
        {	if(dupBoard!=null) { return(dupBoard); }	// duplicate for editHistory
            commonPlayer p = currentRobotPlayer();
            if (p != null)
            {
                SimpleRobotProtocol rob = p.robotBeingMonitored();

                if (rob != null)
                {
                    return (rob.disB());
                }
            }
            if(l.extraBot!=null)
            {	return l.extraBot.disB();
            }
        }

        return (null);
    }
        
    /** get a display board, and indicate a duplicate is in used with
     * a black border.  This will not generate a copy board if gc is null
     * @param gc
     * @return a BoardProtocol (ie a board)
     */
    public BoardProtocol disB(Graphics gc)
    {	
    	if(gc!=null)
    	{  //possibly select an alternate board
	   	  BoardProtocol disb = disB();
	   	  // disb is a board from the robot search, only during
	      if(disb!=null) 
	      { 
	    	if(boardRect!=null) { GC.frameRect(gc,Color.black,boardRect); }; // give a visual indicator
	    	return(disb);
	      }
    	}
    	return(getSafeDisplayBoard());
	}
    
    /**
     * use direct drawing instead of double buffering, and 
     * draw on a copy of the real board rather than the board itself.
     * Since you're drawing based on a copy, it's stable and the drawing
     * can be done directly in any process.
     * 
     * this requires some care, especially when board-level objects
     * are visible to the user interface, such as cells or player sub boards.
     * 
     * Common problems when enabling this feature 
     * 
     * animations where the animation origin or destination is not set, 
     * usually on cells used only in the UI
     * 
     * Missed UI events because some cells on the board, which are used only in the UI
     * are not copied to the copy boards.
     * 
     * Missed UI events where the display code uses a cell from the display board, and the activation code
     * in stopdragging uses the actual board.  Fix this by basing the actions on cell.col and cell.row
     * or by using <board>.getCell(x) to get the board's original cell.
     * 
     */
    public void useDirectDrawing(boolean unbuffered)
    {	
	     painter.drawLockRequired = false;	// we can draw any time
	     saveDisplayBoard(getBoard());		// make sure there is one
	     swapCopyBoardIfNeeded();
	     saveDisplayBoard(getBoard());		// make sure there are two
	     useCopyBoard = true;
	     if(G.isCodename1() && unbuffered) 
	 		{	 
	    	 painter.setRepaintStrategy(RepaintStrategy.Direct_Unbuffered); 
	 		}
    }

	private boolean saveDisplayBoardNeeded = false;
	//callback from the mouse manager, which may have done something
    //that affects the display state
	public void repaintForMouse(int n,String s) {
		saveDisplayBoardNeeded = true;
		super.repaint(n,s);
	}
	
    /**
     * save a copy of the real board to be used for screen drawing
     * this requires 2 copies that ping-pong so no lock is needed
     * and the display can happen at any time. 
     * @return the copy of the board that's safe to display from
     */
    public void saveDisplayBoard()
    {
    	if(useCopyBoard) 
    		{
    		saveDisplayBoard(getBoard());   	// do the actual save
    		}
    }
    
    private void saveDisplayBoard(BoardProtocol b)
    {
    		// if the viewer enters after this point, it will not swap in the board
    		// we're about to copy to, even if it was already in need of swapping.
    		l.copyBoardSwapNeeded = false;
    		
    		if(l.backupBoard == null) 
    			{ l.backupBoard = b.cloneBoard();    			  	
    			  l.backupBoard.setName(l.displayBoard==null ? "display 1" : "display 0");
    	   		  G.print("Create "+l.backupBoard);
    			}
    			else 
    			{ String n = l.backupBoard.getName();
    			  l.backupBoard.copyFrom(b); 
    			  l.backupBoard.setName(n);
    			}
 			 
    		// the new board is ready to be seen
    		l.copyBoardSwapNeeded = true;
    }
    
    private void swapCopyBoardIfNeeded()
    {    
    	if(l.copyBoardSwapNeeded)
    	{
    		synchronized (this) { 
			// don't flip the boards when the display board is in use.
    			BoardProtocol temp = l.backupBoard;
    			l.backupBoard = l.displayBoard;
    			l.displayBoard = temp;
    			l.copyBoardSwapNeeded = false;
    		}
    		}
    	}
    /**
     * if drawing to a copy of the board is in effect, 
     * this returns a copy of the real board that can be drawn on
     * otherwise it returns the real board
     * @return
     */
    public BoardProtocol getSafeDisplayBoard()
    {	if(useCopyBoard)
    	{
     	swapCopyBoardIfNeeded(); 
     	G.Assert(l.displayBoard!=null,"there must already be a display board");
     	return(l.displayBoard);
    	}
    	return(getBoard());
    }



    public String gameName()
    {
    	return sharedInfo.getString(GameInfo.GAMENAME,"gamename");
    }
    public void doSaveCollection()
    {
        doSaveCollection(sgf_reader.do_sgf_dialog(FileDialog.SAVE, gameName(),"*.sgf"));
    }

    // play rapidly through the entire collection as a test
    public void doReplayCollection()
    {	if(hidden.Games!=null)
    	{for(int i=0;i<hidden.Games.size(); i++)
    	{
    	sgf_game game = hidden.Games.elementAt(i);
    	String sn = game.short_name();
    	if(sn==null || "".equals(sn)) { game.set_short_name("Game #"+i); }
    	ReplayGame(game);   	
    	repaint();
    	G.doDelay(10);
    	}}
    }
    public void doReplayFolder()
    {	JFileChooser fd = new JFileChooser("G:\\share\\projects\\boardspace-html\\htdocs\\");
    	fd.setDialogTitle("Select a sgf directory or zip");
    	fd.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    	fd.showOpenDialog(null);
    	File f = fd.getSelectedFile();
    	l.fileVisits = 0;
    	l.fileProblems = 0;
    	StringBuilder problems = new StringBuilder();
    	if(f!=null)
    	{
        G.print("Selected "+f);
    	FileMapper map = new FileMapper();
    	map.visit(f, new FileVisitor() {
    	String phase = "";
    	    private void doReReplay()
    	    {	BoardProtocol b = getBoard();
    	    	long startingDigest = b.Digest(); 
    	    	//boolean gameOver = b.GameOver();
				phase = "replay";
				doWayBack(replayMode.Replay);
				doScrollTo(FORWARD_TO_END);
				long endingDigest = b.Digest();
				G.Assert(startingDigest==endingDigest,"digest after replay is different");
				//G.Assert(gameOver,"not game over after initial play");
				//G.Assert(hidden.Games.size()==1,"file %s contains %s games",hidden.Games.elementAt(0),hidden.Games.size());
    	    }
			public void visit(File u) {
				//G.print(u);
				phase = "";
				try {
				l.fileVisits++;
				phase = "play";
				doLoadGameFile(u.getPath());
				doReReplay();
				// replayed successfully
				//String name = u.getAbsolutePath();
				//String newname = name.replaceFirst("testgames","testgames-tofix-4");
				//u.renameTo(new File(newname));
				}
				catch(Throwable err)
				{	repaint();
					l.fileProblems++;
					String msg = ""+l.fileVisits+": "+phase+" Problem in file:"+u+" "+err;
					G.print(msg);
					problems.append(msg);
					problems.append("\n");
				}
			}

			public void visitZip(File zip,ZipEntry e,ZipInputStream s)
			{	//G.print(e);
				phase="";
				try {
					l.fileVisits++;
				phase = "read";
				sgf_game[] gg = sgf_reader.parse_sgf_file(zip.toString(),e.getName(),s,System.out);
				if(gg!=null)
				{hidden.Games = new sgf_gamestack();
           	     hidden.Games.pushArray(gg);
           	     phase = "play";
                 if((gg.length>0)) 
                 { 
     			   //G.print("game "+e);
                   selectGame(gg[0]);
                   doReReplay();
                 /* repaintCanvas();*/}
				}}
				catch(Throwable err)
				{	repaint();
				l.fileProblems++;
					String msg = ""+l.fileVisits+": "+phase+" Problem in zip file:"+zip+" "+e+" "+err;
					G.print(msg);
					problems.append(msg);
					problems.append("\n");
				}
			}
			public boolean filter(String name, boolean isDirectory, File parent) {
				return(isDirectory || name.endsWith(".sgf"));
				} 
			} );
    	G.print("summary:\n",problems.toString());
    	G.print(l.fileVisits," files visited ",l.fileProblems, " problems");

    	}

    }
    
    private void doSaveGame()
    {	doSaveGame(save_game());
    }

    private void doSaveGame(sgf_game game)
    {
        String ss = sgf_reader.do_sgf_dialog(FileDialog.SAVE,gameName(), "*.sgf");
        if (ss != null)
        {
            int lastDot = ss.lastIndexOf('.');
            int lastSlash = ss.lastIndexOf('/');
            if((lastDot<0) || (lastSlash>lastDot)) 
            	{ ss += ".sgf"; 
            	}
            
            sgf_reader.sgf_save(ss, game);
        }
    }
    public void doSaveUrl(String file)
    {	if(l.zipArchive!=null)
    	{
    	String f = unfile(file);
    	int ind = l.zipArchive.lastIndexOf('/');
    	if(ind>=0) { file = l.zipArchive.substring(0,ind+1)+f; }
    	}
    	int dot = file.lastIndexOf('.');
    	int slash = file.lastIndexOf('/');
    	if(dot<0 || slash>dot) 
    		{ file += ".sgf"; 
    		}
    	sgf_reader.sgf_save(G.getUrl(file),save_game());
    }
    
    public void doSaveCollection(String ss)
    {
        if (ss != null)
        {	if(hidden.Games==null)
        		{ hidden.Games = new sgf_gamestack();
        		  addGame(History,"unnamed");
        		}

        if (hidden.selectedGame!=null)
            {
            	hidden.Games.replace(hidden.selectedGame,save_game());
            }
            int lastDot = ss.lastIndexOf('.');
            int lastSlash = ss.lastIndexOf('/');
            if((lastDot<0) || (lastSlash>lastDot)) { ss += ".sgf"; }

            sgf_reader.sgf_save(ss, hidden.Games.toArray());
        }
    }
    public commonPlayer[] getPlayers() { return(players); }
    public commonPlayer getPlayer(int n)
    {
    	if(n>=0 && n<players.length) { return(players[n]); }
    	return(null);
    }
    /**
     * this gets player n's commonPlayer object, or a temporary new commonPlayer
     * if the player hasn't arrived yet.  Use this in setLocalBounds so the layout
     * can always be done based on real player objects.
     * @param n
     * @return get a player or a new temporary player table
     */
    public synchronized commonPlayer getPlayerOrTemp(int n)
    {	
     	extendPlayers(n);
    	commonPlayer p = getPlayer(n);
    	if(p ==null) { 
    		p=tempPlayers[n];
    		if(p==null) { tempPlayers[n]= p = new commonPlayer(n); }
    	}
    	return(p);
    }
    private void extendPlayers(int idx)
    {
    	G.Assert((idx>=0)&&(idx<=Session.MAXPLAYERSPERGAME), "%s is not a plausible number of players",idx);
    	if(idx>=players.length) 
    		{ adjustPlayers(idx+1);
    		}
    }
    public void adjustPlayers(int n)
    {	if(n!=players.length)
    	{
    	G.Assert((n>=0)&&(n<=Session.MAXPLAYERSPERGAME),"too many players");
    	commonPlayer newp[] = new commonPlayer[n];
    	int minp = Math.min(n,players.length);
    	for(int i=0;i<minp;i++) { newp[i]=players[i]; }
    	//for(int i=minp;i<n;i++) { newp[i] = new commonPlayer(i); }
    	players = newp;
    	resetBounds();
 
    	}
    }
    private boolean selectGame(sgf_game game)
    {
    	ReplayGame(game);
    	hidden.selectedGame = game;
        l.urlLoaded = true;
    	return(true);
    	
    }
    public boolean selectGame(String selected)
    {
        if (hidden.Games != null)
        {
            for (int i = 0; i < hidden.Games.size(); i++)
            {
                sgf_game game = hidden.Games.elementAt(i);

                if (game != null)
                {
                    String name = game.short_name();

                    if (name == null)
                    {
                        name = "Game " + game.sequence;
                    }

                    if (name.equals(selected))
                    {
                        return(selectGame(game));
                    }
                }
            }
        }

        return (false);
    }

    private void doLoadGameFile(String ss)
    {
        if (ss != null)
        {
            if(theChat!=null)
            	{ theChat.sendAndPostMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_LOBBY_CHAT, "Loading " + ss);
            	}
            hidden.selectedGame = null;

            PrintStream pw = Utf8Printer.getPrinter(System.out);
            
            sgf_game gg[] = sgf_reader.parse_sgf_file(ss, pw);
           	if(gg!=null)
            	{hidden.Games = new sgf_gamestack();
            	 hidden.Games.pushArray(gg);
                 if((gg.length>0)) { selectGame(gg[0]); }
            	}
            pw.flush();
        }
    }

    public sgf_game addGame(CommonMoveStack moves,String name)
    {	for(int i=1,lim=moves.size();i<lim;i++) { moves.elementAt(i-1).next = moves.elementAt(i);}
		sgf_game game = save_game(moves);
		game.getRoot().set_property(sgf_names.gamename_property,name);
		return(addGame(game));
    }
    public sgf_game addGame(sgf_game game)
    {
		if(hidden.Games==null) { hidden.Games=new sgf_gamestack(); }
		hidden.Games.push(game);
		game.sequence = hidden.Games.size();
		return(game);
    }

    private void doLoadGame()
    {
        String ss = sgf_reader.do_sgf_dialog(FileDialog.LOAD,gameName(), ("*.sgf"));
        doLoadGameFile(ss);
    }
    private String unfile(String name)
    {
    	int ind = name.indexOf(':');
    	return(ind>=0? name.substring(ind+1) : name);
    }
    
    public boolean isUrlLoaded()
    {	boolean ul = l.urlLoaded;
    	l.urlLoaded = false;
    	return(ul);
    }
    // this can be invoked by the higher level "game" event loop, at a point where the
    // user interface has never been seen.  In some cases, (on IOS, direct drawing) this
    // can result in a race between the display and rebuilding the game.  To fix this
    // we defer the action until the user interface has been initialized
    public void doLoadUrl(String name, String gamename)
    {	l.loadUrlName = name;
    	l.loadUrlGame = gamename;
    }
    // load the game now, after the UI is fully initialized.
    private void loadUrlNow()
    {	String name = l.loadUrlName;
    	String gamename = l.loadUrlGame;
    	l.loadUrlName = null;
    	l.loadUrlGame = null;
    	if(name!=null)
    {	       		
     	if (name.endsWith(".zip"))
        {
     		l.zipArchive = name;
            return;
        }
    	else if(name.endsWith(".zzz"))
    	{
    		l.zipArchive = name;
    		gamename = null;
    		name = null;
    	}
    	else if (name.endsWith("/"))
        {
    		l.zipArchive = null;
            return;
        }
     	
 

        {
          	Plog.log.appendNewLog("reading ");
          	Plog.log.appendLog(name);
          	Plog.log.appendLog(' ');
          	Plog.log.appendLog(gamename);
          	Plog.log.finishEvent();
          	PrintStream printer = Utf8Printer.getPrinter(System.out);
            //System.out.println("Geturl "+name);
            sgf_game gg[] = (l.zipArchive == null)
                ? sgf_reader.parse_sgf_file(G.getUrl(name, true), printer)
                : sgf_reader.parse_sgf_file(G.getUrl(l.zipArchive, true), unfile(name), printer);
            if(gg!=null)
            {
            	hidden.Games = new sgf_gamestack();
            	hidden.Games.pushArray(gg);
            if((gg.length>0) && "".equals(gamename)) { selectGame(gg[0]); }
            	else { selectGame(gamename); }
            }
            printer.flush();
        }
       	if(!startedOnce)
       		{ //normally this is done in "startPlaying()" but that can be bypassed
       		  //if a game is loaded before the server responds to the history request
       		  startedOnce = started = true; 
       		}
    	}
    }
    

    /** email a game record */
	private void emailGame(String to,String subject,String body)
	{	
		String msg = "mailto:"+to
						+"?subject="+Http.escape(subject)
						+"&body="+Http.escape(body);
		G.showDocument(msg);
	}
	
    
    /**
     * this is the method called to produce a game record.  For display in a window.
     */
    private void doEmailSgf()
    {
        sgf_game game = save_game();
        Utf8OutputStream lbs = new Utf8OutputStream();
        PrintStream os = Utf8Printer.getPrinter(lbs);
        sgf_reader.sgf_save(os, new sgf_game[] {game});
        os.close();
        String msg = lbs.toString();
        emailGame("",s.get(RecordOfGame,game.short_name()),msg);
     }
    /**
     * this is the method called to produce a game record.  For display in a window.
     */
    public void doShowSgf()
    {
        sgf_game game = save_game();
        Utf8OutputStream lbs = new Utf8OutputStream();
        PrintStream os = Utf8Printer.getPrinter(lbs);
        sgf_reader.sgf_save(os, new sgf_game[] { game});
        os.close();
        String msg = lbs.toString();
        TextDisplayFrame f = new TextDisplayFrame(s.get(RecordOfGame,game.short_name()));
		f.setText(msg);
    }


    public abstract void ReplayMove(sgf_node no);
    
    public boolean parsePlayerCommand(String name,String value)
    {	int player = commonMove.playerNumberToken(name);
    	if ((player>=0)&&(player<players.length))
         {	commonPlayer p = players[player];
             if (p == null)
             {
                 p = players[player] = new commonPlayer(player);
             }
              
             StringTokenizer tokens = new StringTokenizer(value);
             String first = tokens.nextToken();


                 if(parsePlayerInfo(p,first,tokens)) {}
                 else
                 { parsePlayerExecute(p,first,tokens);
                 }
         return(true);
         }	
    	 return(false);
    }
    public boolean parseVersionCommand(String name,String value,int max)
    { 	if (name.equals(version_property))
        {	int vv = G.IntToken(value);
        	if(vv>0 && vv<=max) { return(true); }
            throw G.Error("Game version %s not handled",value);
        }	
    	return(false);
    }
    
    public boolean parsePlayerExecute(commonPlayer p,String first,StringTokenizer tokens)
    {	
    	String next = tokens.hasMoreTokens() ? tokens.nextToken() : "";
    	// note; this breaks replays of games from the logs that include edits and errors after edits.
    	// normally the games won't include edit anyway
    	// if("edit".equals(next) && getBoard().GameOver()) { return(true); }
       	String rest = "".equals(next) ? next : next+" "+G.restof(tokens);
       	String msg = first + " "+ rest;
       	// replay1 instead of replay includes "heavier" move bookkeeping such as
       	// reconstructing move paths in Colorito or Iro
        return(PerformAndTransmit(msg, p.boardIndex,false,replayMode.Replay1));	
    }
    
    public boolean parsePlayerInfo(commonPlayer p,String first,StringTokenizer tokens)
    {	if(KEYWORD_ID.equals(first))
    	{
    	String name = tokens.nextToken();
    	while(tokens.hasMoreTokens() 
    			&& (name.charAt(0)=='"') 
    			&& (name.charAt(name.length()-1)!='"')) 
    		{ name = name + " " +tokens.nextToken(); // accumulate the full quoted name  
    		}	
		p.setPlayerName(G.trimQuotes(G.decodeAlphaNumeric(name)),false,this);
		return(true);
    	}
    	else if(TIME.equals(first)
   				|| RANKING.equals(first) 
   				|| COUNTRY.equals(first))
    	{
    		p.setPlayerInfo(first,G.restof(tokens)); 
    		return(true);
 	   	}
        return(false);
    }
    
    private Error replayNode(sgf_node root)
    { 
    	Error theError = null;
   		try	{
   		String rootResult = null;
   		
   		while((root!=null) && (theError==null))
   		{	l.parsedTime = -1;
   			l.parsedAnnotation = null;
   			l.parsedResult=null;
  			ReplayMove(root);
  			commonMove top = History.top();
  			if(l.parsedResult!=null) { rootResult = l.parsedResult; }
  			if(top!=null)
  				{
  				if(l.parsedTime>=0) { top.setElapsedTime(l.parsedTime); }
  				if(l.parsedAnnotation!=null) { top.setAnnotations(l.parsedAnnotation); }
  				}
   			int nSuccessors = root.nElements();
   			if(nSuccessors <= 1) { root = root.firstElement(); }	// iterate for simple nodes
   			else { 	
   					int n = History.size();
   				    for(int nth = 0;
   						(theError==null) && (nth<nSuccessors);
   	    				nth++)
   					{
   				     sgf_node elem = root.nThElement(nth);
   					 theError =  replayNode(elem);
   					 doScrollTo(n);
   					}
   				    
   					root = null;
   			}
   		}
   		if(rootResult!=null && History.size()>0)
   			{ commonMove top = History.elementAt(0);
   			  if(top!=null)
   			  {
   				  top.setProperty(result_property,rootResult);
   			  }
   			}
   		}
   		catch (Error err) 
   			{ theError = err; }
   		
   		return(theError);
    }
    //
    // select the principle variation.  The "natural" replay of a complex tree leaves
    // the last variation rather than the first as the final state.
    //
    private void selectPrincipleVariation()
    {	
    	rewindHistory(1);		// this truncates the history
   		commonMove m = History.elementAt(0);
   		commonMove n;
   		// m.firstVariation() follows the selected path
   		// m.getVariation(0) selects the actual first
   		while((n=m.getVariation(0))!=null)
   		{	History.addElement(n);	// add the intended element to the history
   			if(m.nVariations()>1)	// make sure next is correct
   			{	m.next = n;
   			}
   			hidden.RedoStep(replayMode.Replay1,n);	// replay it
   			m = n;
    	};
    }
    /** replayGame replays a SGF record structure.  This is the normal
     * route for game reviewers get games.  
     */
    public void ReplayGame(sgf_game ga)
        {	//G.print("Replay "+ga);
            boolean sound = myFrame.doSound();
            Random.setNextIntCompatibility(false);
            mutated_game_record = false;
            if (isOfflineGame())
            {
                if(theChat!=null) { theChat.clearMessages(true); }
                commentedMove = commentedMoveSeen = null;
            }
            if(theChat!=null)
            {
            theChat.setShortNameField("");
            theChat.setNameField("");
            }
            Error theError = null;
            // this is to enhance replayability of archived games, 
            // where the rules may have evolved or old archives may
            // be damaged.  Setting the flag doesn't intrinsically
            // do anything, but is information available to the game.
            // 
            // this flag needs to remain set forever, which is ok
            // since we can't switch from replay back to live games.
            //
            try
            {
            	BoardProtocol b = getBoard();
                b.setPermissiveReplay(true);
                b.setColorMap(null, -1);		// standardize the color map
                b.resetClientRevision();	// forget revision from the previous game 
                if (sound)
                {	// temporarily turn sound off
                    myFrame.setDoSound(false);
                }
                timeControl = new TimeControl(TimeControl.Kind.None);
                futureTimeControl = timeControl.copy();
                datePlayed = "";
                gameOver = false;
                doInit(false);
            	//
            	// history ought to be completely empty at this point, but typically isn't
            	// because it is initialized with a start or edit.  Start an edit ought to
            	// be idempotent, but in some cases (such as ManhattanProject, 4-5 players) are not.
            	// this makes damn sure the history is empty when we start replaying a game, where
            	// the first move ought to be a "start"
                //
                resetHistory();
                if(theChat!=null)
                {
                theChat.setShortNameField("Game " + ga.sequence);
                theChat.clearMessages(false);
                }
                //b.board_state=PUZZLE_STATE;			//temp
                sgf_node root = ga.getRoot();
                //
                // this is nasty.  the color map has to be in effect when the setup property is seen, so put it first here.
                //
                sgf_property rootProperties = root.properties;
                sgf_property colormap = sgf_property.get_sgf_property(rootProperties,colormap_property);
                if(colormap!=null)
                {
                	sgf_property setup = sgf_property.get_sgf_property(rootProperties,setup_property);
                	if(setup!=null)
                	{	rootProperties = sgf_property.remove_sgf_property(rootProperties,setup);
                		setup.next = colormap.next;
                		colormap.next = setup;
                		root.properties = rootProperties;
                	}
                }
                theError = replayNode(root);
                
            }
            finally
            {
           		if(theError!=null) { throw theError; }
                if (sound)
                {
                    myFrame.setDoSound(true);
                }
                if(mutated_game_record)
                {
                	selectPrincipleVariation();
                }
            }
            resetBounds();
            saveDisplayBoard();
            generalRefresh();


            if (theChat!=null && "".equals(theChat.nameField()))
            {
                String sf = ga.source_file;
                int idx = (sf==null)? 0 : sf.lastIndexOf("/");

                if (idx > 0)
                {
                    sf = sf.substring(idx + 1);
                }

                idx = (sf==null)?0:sf.lastIndexOf(".");

                if (idx > 0)
                {
                    sf = sf.substring(0, idx);
                }

                theChat.setNameField(sf);
                {	sgf_node root = ga.getRoot();
                	if("".equals(root.get_property(gamename_property))) 
                		{ root.set_property(gamename_property,sf);
                   		}
                }
            }
        }
    
    private void saveNode(sgf_node curr,commonMove m)
    {	while(m!=null)
    	{
        String ms = m.longMoveString();
        if((ms!=null) && !("".equals(ms)))
           {
            sgf_node nn = new sgf_node();
            curr.addElement(nn, Where.atEnd);
            curr = nn;
            curr.set_property(m.playerString(), ms);
            String comm = m.getComment();
            if (comm != null)
            {
                curr.set_property(comment_property, comm);
            }
            
            curr.set_property(annotation_property,MoveAnnotation.toReadableString(m.getAnnotations()));
             
            long tm = m.elapsedTime();
            if(tm>=0) 
            {
            	curr.set_property(time_property, ""+tm);
            }
            
        int nvar = m.nVariations();
        if(nvar>1) 
        	{
        	commonMove next = m.next;
        	if(next!=null) { saveNode(nn,next); }	// use the selected variation first
        	for(int i=0;i<nvar;i++) 
        		{ commonMove var =  m.getVariation(i);
        		  if(var!=next) { saveNode(nn,var); }
        		}
        		m = null; 
        	}
        else { m = m.getVariation(0); curr = nn; }
    	}}
   }
    /**
     * override this method to add properties to the sgf root when saving a game
     * @param root
     */
    public void setRootProperties(sgf_node root)
    {	
    	History.elementAt(0).copyPropertiesTo(root);
    }
    
    /** save the game as a sgf record structure, which will be printed
     * to a file.  This path is actually not much used, since we don't
     * usually output files directly.
     */
    public sgf_game save_game(CommonMoveStack  moves)
    {
        sgf_game game = new sgf_game();
        sgf_node root = game.getRoot();
        sgf_node curr = root;
  
        for (int i = players.length - 1; i >= 0; i--)	// iterate backwards, last player first
        {
            commonPlayer p = players[i];

            if (p != null)
            { //don't translate the color here

                {
                    String rank = p.getPlayerInfo(RANKING);

                    if ((rank != null) && !("".equals(rank)))
                    {
                        root.set_property(p.playerString(), RANKING+" " + rank);
                    }
                }

                {
                    String ct = p.getPlayerInfo(COUNTRY);

                    if ((ct != null) && !("".equals(ct)))
                    {
                        root.set_property(p.playerString(), COUNTRY+" " + ct);
                    }
                }
                // properties are being built backwards, so do the name last, which will be first.
                root.set_property(p.playerString(), " "+KEYWORD_ID+" \"" + p.prettyName("p"+i)+"\"");

            }
        }
        if(theChat!=null)
        {
        root.set_property(gamename_property, theChat.shortNameField());
        root.set_property(gametitle_property, theChat.nameField());
        }
        if(isTurnBasedGame())
     		{	
         	root.set_property(gamespeed_property , turnBasedGame.speed.menuItem()); 
     		}
        root.set_property(setup_property, gameType());
        // only emit color map information if it's active in this environment
        root.set_property(colormap_property,colorMapString());
       	if(timeControl!=null)
    	{
       		root.set_property(timecontrol_property,timeControl.print()); 
    	}
       	if(!"".equals(datePlayed))
       	{
       		root.set_property(date_property,datePlayed);
       	}
        root.set_property(game_property, "" + sgfGameType());
        setRootProperties(root);
        saveNode(curr,moves.elementAt(0));
        

        for (int i = players.length - 1; i >= 0; i--)
        {
            commonPlayer p = players[i];

            if (p != null)
            {
                String timestring = p.getPlayerInfo(TIME);

                if (!"".equals(timestring))
                {
                    timestring = " "+TIME+" " + timestring;
                    curr.set_property(p.playerString(), timestring);
                }
            }
        }

        return (game);
    }
    /** save the game as a sgf record structure, which will be printed
     * to a file.  This path is actually not much used, since we don't
     * usually output files directly.
     */
    private sgf_game save_game()
    {	return(save_game(History));
    }
    

    public String colorMapString()
    {
        int colormap[] = getBoard().getColorMap();
        String colorMapString = "";
        if(colormap!=null) 
        	{ String comma = "";
        	  for(int d : colormap) { colorMapString += comma+d; comma=","; }
        	}
        else { colorMapString = "0,1"; }
        return(colorMapString);
    }
    /** print the game record directly from the history.  This
     * is the way games are recorded by the server: printed by 
     * this PrintStream into a string, which is transmitted to the
     * server over the net connection, and the server writes the file.
     * The net effect should be very similar to the SGF output.
     */
    public void printGameRecord(PrintStream ps, String startingTime,CommonMoveStack  his,String filename)
    {
        String nameString = filename;
        ps.println("(;");
        ps.println(game_property+ "[" + sgfGameType() + "]" + version_property +"["+ sgfGameVersion()+"]");
        // colormap immediately after the game id, so it will be active when the setup happens

        String colormap = colorMapString();
        if(colormap!=null) {  	ps.println(colormap_property+"["+colormap+"]"); }
        if(timeControl!=null)
        {
        	ps.println(timecontrol_property+"["+timeControl.print()+"]"); 
        }
        ps.println(setup_property+"[" + gameType() + "]");
        ps.println(date_property+ "[" + startingTime + "]");
        ps.println(gamename_property + "[" + nameString + "]");
        if(isTurnBasedGame())
        	{ ps.println(gamespeed_property + "[" + turnBasedGame.speed.menuItem() + "]"); 
        	}
        if(History.size()>0)
        {
        History.elementAt(0).printProperties(ps);
        }
        if(GameOver())
        {
        	ps.println(result_property+ "["+gameOverMessage(getBoard())+"]");
        }
        for (commonPlayer p = commonPlayer.firstPlayer(players); p != null;
                p = commonPlayer.nextPlayer(players, p))
        {
            if (!nameString.equals(""))
            {
                nameString += "-";
            }

            nameString += p.trueName;
        }

 
        // version 1 original implementation
        // version 2 after initial fiascos.  Player sequence stabalized
 
        for (commonPlayer p = commonPlayer.firstPlayer(players); p != null;
                p = commonPlayer.nextPlayer(players, p))
        { //don't translate color here
            ps.println(p.playerString() + "[id \"" + p.trueName + "\"]");

            {
                String rank = p.getPlayerInfo(RANKING);

                if ((rank != null) && !("".equals(rank)))
                {
                    ps.println(p.playerString() + "["+RANKING+" " + rank + "]");
                }
            }

            {
                String ct = p.getPlayerInfo(COUNTRY);

                if ((ct != null) && !("".equals(ct)))
                {
                    ps.println(p.playerString() + "["+COUNTRY+" " + ct + "]");
                }
            }
        }

        // this is the standard method since forever.  Since it works perfectly,
        // be careful about featurizing it.  replace this clause with printNode(..);
        // would be the next step.
        for (int i = 0; i < his.size(); i++)
        {
        	commonMove m = his.elementAt(i);
        	String str = m.longMoveString();
        	if(str!=null && !"".equals(str))
        	{  ps.print("; ");
        	   ps.print(m.playerString());
        	   ps.print("[");
        	   ps.print(sgf_property.bracketedString(str));
        	   ps.print("]");
        	   MoveAnnotation.printAnnotations(ps,m);
        	   long tm = m.elapsedTime();
        	   if(tm>=0)
        	   { ps.print(time_property);
        	     ps.print("[");
        	     ps.print(tm);
        	     ps.print("]");
        	   }
        	   ps.println();
         	}
        }

        ps.println(";");

        for (commonPlayer p = commonPlayer.firstPlayer(players); p != null;
                p = commonPlayer.nextPlayer(players, p))
        {
            int col = p.colourIndex();
            if ((col >= 0))
            {
                ps.println(p.playerString() + "["+TIME+" " +
                    G.timeString(p.elapsedTime) + " ]");
            }
        }

        ps.println(")");
    }
    public void printDebugInfo(PrintStream ps)
    {	String er = errorContext();
    	if(er!=null) { ps.print(er); }
    	printGameRecord(ps,"Game Record",History,"");
        if (rawHistory.size() > 0)
        {
            printGameRecord(ps, "Raw History", rawHistory, "");
        }
    }
    
    /**
     * override this method to censor the game history before printing.
     * this isn't intended for real censorship, just to remove unnecessary
     * elements.
     * 
     * @param hist
     * @return
     */
    
    public CommonMoveStack censoredHistory(CommonMoveStack s)
    {
    	return s;
    }
    public void printGameRecord(PrintStream ps, String startingTime,String filename)
    {
        printGameRecord(ps, startingTime, censoredHistory(History), filename);

        if ( G.debug() )
        {	
            printGameRecord(ps, startingTime, rawHistory, filename);
            
        }
        }  

    public void ViewerRun(int waitTime)
    {	useInitialization();
    	if(initialized)
    	{	
    	runRemoteViewer();
    	loadUrlNow();
    	l.runHiddenWindows();	// feed mouse events from hidden windows
    	if(gameLog.doRepeat()) { repaint(20); }
    	for (int i = 0; i < players.length; i++)
        {   commonPlayer p = players[i];
            if(p!=null)
            	{
            	if(p.updated)
            		{ repaint(250,"player changed"); 
            		  p.updated=false; 
            		}
            	}
         }

        boolean spritesIdle =  spritesIdle();
        boolean idle = spritesIdle && SoundManager.soundIdle();
       
        if(!spritesIdle) 
        	{ repaintSprites(); 
        	}
       
        if(( ((reviewOnly || mutable_game_record) && extraactions) || isOfflineGame())
        		&& !reviewMode() && idle)
        {	
            commonPlayer who = currentRobotPlayer();
	        if (who != null)
	        {if(allowRobotsToRun(who)
	        	&& who.robotStarted())
	        	{
	        	startRobotTurn(who);
	        	}
	       String m = who.getRobotMove();
	       // this is where offline robot moves are made
	       if(m!=null) 
	       		{  
	    	    try { PerformAndTransmit(m); }
	    	    catch (Error e)
	    	    {
	    	    	stopRobots();
	    	    	throw e;
	    	    }
	    	    waitTime = Math.min(waitTime,20);
	       	    if(GameOverNow()) { stopRobots(); }
	       		}
	        }
        }
        if(animating && spritesIdle) 
    	{ 	// it is important to stop animating if we no longer have control
        	if(reviewMode() && hasControlToken()) 
    		{long now = G.Date();
    		 int interval = (int)(250*masterAnimationSpeed);
    		 long pause = now-hidden.lastAnimTime;
    		 if(pause<0) { waitTime = Math.min(-(int)pause, waitTime); }	// got here too soon
    		 else {
    			 doForwardStep(replayMode.Single);
    			 repaint(0,"forward step");

    			 long next = hidden.lastAnimTime = Math.max(now, hidden.lastAnimTime+interval);
    			 waitTime = (int)(next-now);
    		 } 			    		 
    		}
    		else 
    		{ animating = false; 
    		}
    	}
        boolean ds = mandatoryDoneState();
        if(autoDoneActive() && ds)
		  {
			  sendDone();
		  }
        if(!ds) 
        	{ // no longer in an "autodone" state, so turn off the skip
        	  // flag to resume normal behavior.
        	  skipAutoDone = false; 
        	}
        
    	}
    	if(saveDisplayBoardNeeded)
		{
			saveDisplayBoardNeeded = false;
			saveDisplayBoard();
		}
        // not completely satisfactory to do this here,
        // because "whoseTurn" may change and get out of 
        // synch with the Game object.  Needed for events fromRPC screens to benoticed.
		//
        if(remoteViewer>=0) 
        	{ // handle incoming messages from the main screen
        		l.handleDeferredMessages();	
        	}
        if((timeControl!=null) && (timeControl.kind!=TimeControl.Kind.None))
        {
        	waitTime = Math.min(waitTime,250);
        }
        super.ViewerRun(waitTime);
        
    }

    /**
     * return true if in principle "resign" actions are allowed.
     * this defaults to true for 1-2 player games.
     * 
     * @return
     */
    public boolean canResign()
    {
    	return(getBoard().nPlayers()<=2);
    }
    
    public void doResign()
    {   
    	if(!canResign()) 
    	{if(theChat!=null) 
    	{theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
            s.get(NoResignMessage));
    	}
    	}
    	else
    	{ 
    	if (ourActiveMove())
        {
            PerformAndTransmit(RESIGN);
        }
        else if(theChat!=null)
        {
            theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                s.get(CantResign));
        }
    	}
    }    
    public boolean doOfferDraw()
    {	
    	if(OurMove()
    			&& canOfferDraw()) 										
    		{
			PerformAndTransmit(OFFERDRAW);
			}
    		else { G.infoBox(null,s.get(DrawNotAllowed)); }
     		return(true);
    	
    }
    public void setDoneState(boolean isDone)
    {	long now = G.Date();
    	hidden.doneAtTime = isDone ? now : 0;
    	hidden.startTurn=now; 
    }
    
    static int HURRYTIME = 1000*60;
    private boolean hurryState = false;
    private long flashAnimation = 0;
    private int flashState = 0;
    private void notifyOfSuspense(boolean doSound)
    {	if(doSound && l.playTickTockSounds)
    	{
    	SoundManager.playASoundClip(clockSound,5000);
    	flashAnimation = 0;
    	}
    	else
    	{
    	flashAnimation = G.Date()+3000;	// 3 seconds
    	repaint();
    	}
    }
    /** this does the attention getting animation when the ticktock sound is off
     * 
     * @param gc
     */
    public void doFlashAnimation(Graphics gc)
    {
    	if(gc!=null  && G.Date()<flashAnimation )
    	{	commonPlayer pl = getActivePlayer();
    		Rectangle flashTimeRect =pl.timeRect;
    		int left = G.Left(flashTimeRect);
    		int top = G.Top(flashTimeRect);
    		int w = G.Width(flashTimeRect);
    		int h = G.Height(flashTimeRect);
    		int mod = Math.min(w,h)/2;
    		if(mod>0)
    			{
    			int step = (int)(flashState%mod);
    			flashState++;
    			GC.frameRect(gc,((step&1)==0)?Color.blue:Color.red,left-step,top-step,w+step*2,h+step*2);
    			}
     		repaint(20);
    	}
    }
    // do time related things for the board.
    private void doTime(commonPlayer whoseTurn,boolean doSound)
    { boolean turnChange = whoseTurn!=hidden.lastPlayer;
      long currentT = G.Date();
      if(turnChange)
      {	hidden.lastPlayer = whoseTurn;
      	hidden.startTurn = currentT;
      }
      commonPlayer active = getActivePlayer();
      if(whoseTurn==active )
      	{	
    	long elapsed = (currentT - hidden.startTurn); 
        if ((hidden.startTurn > 0) 
        		&& (((hidden.doneAtTime>0)&& (elapsed>hidden.timePerDone)) 
        		    || (elapsed > hidden.timePerTurn))
        		&& !isTurnBasedGame())
           {
             notifyOfSuspense(doSound);
             hidden.startTurn = currentT;	// restart the timer
             if(hidden.doneAtTime>0) { hidden.doneAtTime=currentT; }
    	  }
      }
      
      	// if timers are in use, sound a bell at 1 minute
        int remTime = timeRemaining(whoseTurn) - HURRYTIME;		// negative when less than a minute
        if(remTime>0) { hurryState = false; }		// reset
        else if(!hurryState)
        	{
        	hurryState = true;
        	if(doSound) { SoundManager.playASoundClip(hurrySound,1000); }
        }
    }


    /**
     * draw an image
     * @param gc
     * @param im
     * @param boxw
     * @param scale
     * @param x
     * @param y
     */
    public void drawImage(Graphics gc, Image im, double scale[],int x, int y,double boxw) 
    {
    	drawImage(gc,im,scale,x,y,boxw,1.0,0.0,null,false);
    }
  /** this is a horroble kludge to keep old game records
   * for a few games valid.  See the comments near G.nextInt
   * for more detail.  Affected games are container and yspahan 
   * for before 8/25/2012
   * @param b
   * @param name
   * @param value
   */
 public void nextIntCompatabilityKludge(BoardProtocol b,String name,String value,String ref)
 {
 	if(name.equals(date_property))
	{
 	BSDate result = BSDate.parseDate(value);
	BSDate refdate = BSDate.parseDate(ref);
	boolean newformat = result.getTime()<refdate.getTime();
		
	if(newformat!=Random.OLD_NEXTINT_COMPATABILITY) 
		{	// note that games that depend on this (ie; container) need to
			// note this in their history initialization so spectators will
			// also get the note.
			Random.setNextIntCompatibility(newformat);
			b.doInit();
 		}
	}
 }

/**
 * when reloading a game from a sgf game record, handle stock
 * properties such as game name, comments, etc.
 * @param name
 * @param value
 * @return true if this property was handled
 */
public boolean replayStandardProps(String name,String value)
{	
    //System.out.println("prop " + name + " " + value);
    if (name.equalsIgnoreCase(gamename_property))
    {   if(theChat!=null) { theChat.setShortNameField(value); }
        return(true);
    }
    else if (name.equalsIgnoreCase(game_property))
    {	G.Assert(value.equalsIgnoreCase(sgfGameType()),WrongInitError,value);
    	return true;
    }
    else if (name.equalsIgnoreCase(gametitle_property))
    {
    	if(theChat!=null) { theChat.setNameField(value); }
        return(true);
    }
    else if (name.equalsIgnoreCase(date_property))
    {
    	if(theChat!=null) { theChat.sendAndPostMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_LOBBY_CHAT,
            s.get(PlayedOnDate,value));}
        datePlayed = value;
        return(true);
    }
    else if(name.equalsIgnoreCase(colormap_property))
    {
    	
    	getBoard().setColorMap(G.parseColorMap(value), -1); 
    	return(true);
    }
    else if(name.equalsIgnoreCase(timecontrol_property))
    {
    	timeControl = TimeControl.parse(value);
    	futureTimeControl.copyFrom(timeControl);
    }
    else if(name.equalsIgnoreCase(annotation_property))
    {
    	StackIterator<MoveAnnotation> m = MoveAnnotation.fromReadableString(value);
    	l.parsedAnnotation = m;
    }
    else if(name.equalsIgnoreCase(time_property))
    {
    	l.parsedTime = G.LongToken(value);
    	return(true);
    }
    else if(result_property.equals(name))
    {
    	l.parsedResult = value;
    	// fall through to print it into the chat
    }
    
    if(theChat!=null) { theChat.addAMessage("prop " + name + " = " +  value); }
    return(false);
	}
//
// this is the default method used by all except Loa.  It's logic is very
// carefully balanced, so don't disturb it without careful testing
//

/**
 * handle mouse motion.  This is called in the run loop, not synchronous
 * with the mouse events.  The principle effect is to call your {@link #redrawBoard}
 * method with gc=null, for the purpose of locating what the mouse must
 * be pointing to.
 */
public HitPoint MouseMotion(int eventX, int eventY, MouseState upcode)
{	//if(upcode==MouseState.LAST_IS_UP) { painter.setRecord(false); }
	//if(upcode==MouseState.LAST_IS_PINCH||upcode==MouseState.LAST_IS_DOWN) { painter.setRecord(true); } 
	HitPoint p =  new HitPoint(eventX, eventY,upcode);
	HitPoint drag = getDragPoint();
    boolean newDrag = (drag!=null && drag.dragging) && (upcode!=MouseState.LAST_IS_UP);
    if(newDrag)
    {
	p.dragging =  true;			//p.dragging indicates if something is being dragged
	p.inStandard = drag.inStandard;
    }
     
    if(newDrag && (p.hitCode==VcrId.Slider))				// special treatment of the vcr slider
    	{   			  
    	drawVcrSlider(p,null);
    	}
    	else
    	{
    	p.hitCode = DefaultId.HitNoWhere;
    	//
    	// with on the fly rotations of the pointer for playtable, the absolute
    	// x,y tends to drift.  Combat this and other accidents by saving the
    	// location over the interaction with the client code.
    	//
     	int hx = G.Left(p);
     	int hy = G.Top(p);
        if(startedOnce) 
        	{ redrawBoard(p); 
        	  DrawTileSprite(null,p);
        	}
        G.SetLeft(p, hx);
        G.SetTop(p,hy);



    	// this doesn't integrate well with all the new layout logic
    	//if(adjustChatSize(p))
    	//	{
    	//	if(upcode==MouseState.LAST_IS_DOWN)
    	//		{ hidden.dragSizeAdj=0; 
    	//		  p.dragging=true; 
    	//		}
    	//	}
    	}
	HitPoint sp = setHighlightPoint(p);
	switch(upcode)
	{
	case LAST_IS_DOWN:
	case LAST_IS_UP:
		setHasGameFocus(true);
		repaint();
		break;
	case LAST_IS_IDLE: 
	case LAST_IS_DRAG:
	case LAST_IS_PINCH:
		repaintSprites(20,"mouse motion");
		break;
	default: break;
	}
	
    return(sp);
}
/**
 * return a name )string appropriate for player at index n
 * @param n
 * @return a String
 */
public String prettyName(int n)
{	String def = s.get(PlayerNumber,(n+1));
	commonPlayer pl = getPlayer(n);
	if(pl!=null) { def = pl.prettyName(def); }
	return(def); 
}

/**
 * draw standard player stuff as a start button, or just as a player name rectangle
 * 
 * @param gc	the gc
 * @param button if true, draw as a start button
 * @param hit the hit point
 * @param fg the foreground color
 * @param bg the background color
 */
public void drawPlayerStuff(Graphics gc,boolean button,	HitPoint hit,Color fg,Color bg)
{	for(int i=0;i<players.length;i++)
	{
	drawPlayerStuff(gc,getPlayerOrTemp(i),button,hit,fg,bg);
	}
}
/**
 * draw standard player stuff for a single player as a start button, or just as a player name rectangle
 * 
 * @param gc	the gc
 * @param pl the player
 * @param button if true, draw as a start button
 * @param hit the hit point
 * @param fg the foreground color
 * @param bg the background color
 */
public void drawPlayerStuff(Graphics gc,commonPlayer pl,boolean button,	HitPoint hit,Color fg,Color bg)
{	if(pl!=null) 
	{ hidden.drawPlayerStuff(gc,pl,hit,button?GameId.HitStartP[pl.boardIndex]:null,fg,bg); 
	}
}

/**
 * this allows games to indivially decide which players get highlighted
* in drawPlayerStuff
 * @param pl
 * @return
 */
public boolean playerIsActive(commonPlayer pl) 
{ return ((simultaneousTurnsAllowed()&&!reviewOnly)||pl.boardIndex==getBoard().whoseTurn()); 
} 


/** display the standard game state message, or any override message,
 * in the "what to do" rectangle.
 * 
 * @param gc
 * @param defaultMsg
 * @param addPlayer
 * @param who
 * @param stateRect
 */
public void standardGameMessage(Graphics gc,String defaultMsg,boolean addPlayer,int who,Rectangle stateRect)
{	standardGameMessage(gc,0,Color.black,TextChunk.create(defaultMsg),addPlayer,who,stateRect);
}
public void standardGameMessage(Graphics gc,double rotation,String defaultMsg,boolean addPlayer,int who,Rectangle stateRect)
{	
	standardGameMessage(gc,rotation,Color.black,TextChunk.create(defaultMsg),addPlayer,who,stateRect);
}

public void standardGameMessage(Graphics gc,double rotation,Color cc,String defaultMsg,boolean addPlayer,int who,Rectangle stateRect)
{
	standardGameMessage(gc,rotation,cc,TextChunk.create(defaultMsg),addPlayer,who,stateRect);
}
public void standardGameMessage(Graphics gc,double rotation,Color cc,Text defaultMsg,boolean addPlayer,int who,Rectangle stateRect)
{	Text msg = null;
    if (!allowed_to_edit && !reviewMode())
    {
        if (inLimbo)
        {
            msg = TextChunk.create(s.get(LimboMessage));
        }

        if (leftBarMessage != null)
        {
            msg = TextChunk.create(leftBarMessage);
            cc = leftBarMessageColor;
        }
    }
    if(msg==null)
    {	msg = defaultMsg;
    	if(addPlayer) { msg = TextChunk.join(TextChunk.create(currentPlayerPrettyName(who)), msg);	}
    }
	GC.setColor(gc,cc);
	GC.setFont(gc,G.getFont(standardBoldFont(),G.Height(stateRect)/2));
    if(rotation==0) { msg.draw(gc,0, false, stateRect, cc, null); }
    else {
    // rotate and right justify.  This puts the game message at the left of the 
    // state rectangle for both rotations.
	int cx = G.centerX(stateRect);
	int cy = G.centerY(stateRect);
	GC.setRotation(gc, rotation, cx, cy);
	msg.drawRight(gc,stateRect, cc, null);
	GC.setRotation(gc, -rotation, cx, cy);
    }
}
/**
 * general a "gameover" message appropriate for games where someone wins
 * but there is no score per se.
 * @param gb the game board
 * @return a gameOver message
 */
public String simpleGameOverMessage(BoardProtocol gb)
{	
	for(int i=0;i<gb.nPlayers();i++) 
	{ 
	commonPlayer pl = getPlayerOrTemp(i);
	if((pl!=null) && WinForPlayer(pl))
		{ String msg = s.get(WonOutcome,prettyName(i));
		return(msg); 
		}
	}
	return(s.get(DrawOutcome));
}


/**
 * generate a "gameover" message appropriate for games where every player
 * has a final score.
 * @param gb the game board
 * @return a gameOver message
 */
private String scoredGameOverMessage(BoardProtocol gb)
{
	// multi player game
	String msg = s.get(GameOverMessage);
	int np = gb.nPlayers(); 
	ScoreItem scores[] = new ScoreItem[np];
	
	for(int i=0;i<np; i++) 
	{ commonPlayer p = getPlayerOrTemp(i);
	  scores[i] = new ScoreItem(prettyName(i),gb.scoreForPlayer(p.boardIndex),PrettyScoreForPlayer(gb, p));
	}
		
	Sort.sort(scores);	// show the scores high to low
		
	for(ScoreItem score : scores)
		{	msg += " "+score.name+":"+score.prettyScore;
		}
	return(msg);	
}
/**
 * construct a "game won by" message, with scores for multiplayer games
 * @param gb the game board
 * @return a String
 */
public String gameOverMessage(BoardProtocol gb)
{	if(gb.nPlayers()<=2)
	{ return(simpleGameOverMessage(gb));
	}
	else
	{ return(scoredGameOverMessage(gb));
	}
}

/**
 * display the standard "goal of the game" message, or if there is a 
 * robot running, display the robot's progress slider.
 * @param gc
 * @param highlight 
 * @param message
 * @param progressRect
 * @param goalRect
 */
public void goalAndProgressMessage(Graphics gc,HitPoint highlight,String message,Rectangle progressRect, Rectangle goalRect)
{
	goalAndProgressMessage(gc,highlight,Color.black,message,progressRect, goalRect);
	
}
/**
 * display the standard "goal of the game" message, or if there is a 
 * robot running, display the robot's progress slider.
 * @param gc
 * @param highlight 
 * @param color
 * @param message
 * @param progressRect
 * @param goalRect
 */
public void goalAndProgressMessage(Graphics gc,HitPoint highlight,Color color,String message,
		Rectangle progressRect, Rectangle goalRect)
{
	goalAndProgressMessage(gc,highlight,color,message,progressRect,goalRect,GoalExplanation);
}
public String rulesUrl()
{
	String ruledefault = gameInfo!=null ? gameInfo.rules : G.debug() ? "rules" : null;
	return sharedInfo.getString(RULES,ruledefault);
}
/**
 * display the standard "goal of the game" message, or if there is a 
 * robot running, display the robot's progress slider. Tooltip is a message
 * used to wrap the main message in tooltip - it's important when the "goal"
 * message isn't the goal!
 * @param gc
 * @param highlight 
 * @param color
 * @param message
 * @param progressRect
 * @param goalRect
 * @param tooltip	
 */
public void goalAndProgressMessage(Graphics gc,HitPoint highlight,Color color,String message,
		Rectangle progressRect, Rectangle goalRect,String ToolTip)

{	boolean someProgress = false;
	GC.setFont(gc,largePlainFont());
	String timeMessage = timeControl.timeControlMessage();
	if(timeMessage!=null && GoalExplanation.equals(ToolTip))
	{
		ToolTip = TimeControl.TimeControlToolTip;
		message = timeMessage;
	}
	//
	// this version displays the progress from any single robot that is running.
	// normally, the "current player" is also the running robot, but in simultaneous
	// play games (ie Raj), any player's robot can be running.
	//
	for(commonPlayer p : players)
	{
		if((p!=null) && p.drawProgressBar(gc,progressRect)) 
			{ someProgress = true; 
			  break; 	// if there are more running robots they're not displayed.
			}
	}
    if ((goalRect!=null) && (!someProgress ||  !progressRect.intersects(goalRect)))
    {
       	String rules = rulesUrl();
    	int h = G.Height(goalRect);
    	GC.Text(gc, true, G.Left(goalRect), G.Top(goalRect), 
    			G.Width(goalRect)-(rules!=null?h:0),
    			h, color, null,
    			message);
       	if(ToolTip!=null) { HitPoint.setHelpText(highlight,goalRect,s.get(ToolTip,message)); }
     	if(rules!=null)
    		{
    		if(StockArt.Rules.drawChip(gc,this,rulesRect,highlight,GameId.HitRulesButton,ShowRulesMessage))
    			{
    			highlight.spriteRect = rulesRect; 
    			highlight.spriteColor = Color.red;
    			}
    		}
    	// this is a ui hack to provide visible control over the repaint delays
        	painter.positionSliders(gc,highlight,goalRect);
    }
 
}
/**
 * draws a pop up bubble with incoming chat lines
 * 
 * @param gc
 */
private void drawUnseenChat(Graphics gc)
{
	if(unseenPop!=null)
    {	// draw the unseen content as a tooltip, display for 4 seconds
 		GC.setFont(gc,largeBoldFont());
    	GC.drawBubble(gc,unseenPopX,unseenPopY,unseenPop,fullRect,0);
    	if((G.Date()-unseenPopTime)>4000)
    	{
	   		unseenPop = null;
		}
    }
}

/**
 * return a String representing the current player name
 * @param who
 * @return a String
 */
public String currentPlayerPrettyName(int who)
{	// careful, who can be -1
	return(prettyName(who>=0?who:0)+" : ");
}

/**
 * translate the mouse coordinate x,y into a size-independent representation
 * presumably based on the cell grid.  This is used to transmit our mouse
 * position to the other players and spectators, so it will be displayed
 * at approximately the same visual spot on their screen.  
 *  */
public String encodeScreenZone(int x, int y,Point p)
{	
	if(G.pointInRect(x,y,boardRect))
	{ 	int screenXQuant = Math.max(2,G.Width(boardRect)/20);
		BoardProtocol b = getBoard();
		// this inited test is to combat an occasional nullpointerexception
		// when the mouse gets ahead of the game setup
		Point pp = (b!=null 
					 ? b.encodeCellPosition(x-G.Left(boardRect),G.Bottom(boardRect)-y,screenXQuant)
					 : null);

		if(pp!=null)
			{
			G.SetLeft(p,G.Left(pp));
			G.SetTop(p,G.Top(pp));
			return("boardEnc");
			}
	}
	
	// if in one of the named zones
	for(int z=0,lim=mouseZones.size(); z<lim; z++)
	{
		Rectangle zone = mouseZones.elementAt(z);
		if(G.pointInRect(x,y,zone))
		{	G.SetLeft(p,((x-G.Left(zone))*100)/G.Width(zone));
			G.SetTop(p,((y-G.Top(zone))*100)/G.Height(zone));
			String zn = getRectName(zone);
			//if(!myFrame.getSoundState()) 
			//	{ G.print("Encode "+zn+" "+p.x+" "+p.y); }
			return(zn);
		}
	}
	//if in the board zone
	Rectangle ref = fullRect;
	String name = "on";


	// full screen is encoded as arbitrary units from the upper left
	double quant = Math.max(2,G.Width(ref)/100.0);
	G.SetLeft(p,(int)((x-G.Left(ref))/quant));
	G.SetTop(p,(int)((y-G.Top(ref))/quant));
	return(name);
}
/**
 * decode a screen zone x,y
 * @param zone
 * @param x
 * @param y
 * @return a point representing the decoded position
 */
public Point decodeScreenZone(String zone,int x,int y)
{	//if(myFrame.getSoundState()) { G.print("Decode "+zone+" "+x+" "+y); }
	if("boardEnc".equals(zone))
		{ int screenXQuant = Math.max(2,G.Width(boardRect)/20);
		  BoardProtocol b = getBoard();
		// this inited test is to combat an occasional nullpointerexception
		// when the mouse gets ahead of the game setup
		  if(b!=null)
		  {
		  Point mp = b.decodeCellPosition(x,y,screenXQuant);
		  if(G.Advise(mp!=null,"%s %s,%s failed to decode",zone,x,y))
		  {
		  G.SetLeft(mp,G.Left(mp) + G.Left(boardRect));
		  G.SetTop(mp, G.Bottom(boardRect)-G.Top(mp));
		  //G.print("Decode board as "+mp.x+" "+mp.y);
		  return(mp);
		  }}
		}
	{
	Rectangle zr = allRects.get(zone);
	if(zr!=null)
		{// named zones are encoded as a percentage of box size
		Point mp = new Point(G.Left(zr) + (int)(((x+0.5)/100)*G.Width(zr)), G.Top(zr) + (int)(((y+0.5)/100)*G.Height(zr)));
		//if(myFrame.getSoundState()) 
		//{G.print("Decode zone as "+mp.x+" "+mp.y+" from "+zr.y+" "+zr.height);
		//}
		return(mp);
		}
		 
	}
	// full screen is encoded as a arbitrary units from the upper left
	double quant = Math.max(2,G.Width(fullRect)/100.0);
	Point mp =new Point(G.Left(fullRect)+(int)((x+0.5)*quant),G.Top(fullRect)+(int)((y+0.5)*quant));
	//if(myFrame.getSoundState()) { G.print("Decode default as "+mp.x+" "+mp.y); }
	return(mp);
}

/**
 * absorb the first few tokens from a game history string.  These
 * will the produced by your {@link #gameType} method.   Note that originally,
 * the contents would be just a single token "Hive" for example.  The current
 * standard is to include the game identifier, number of players, random initialization, and revision level
 * for example:  Hive 2 0 101
 * 
 * the "identifier" is not necessarily the same as the game name, it might be a selected setup for example
 * include the number of players even for games where it's not expected to change
 * include the random initialization even for games with no randomness
 * include the revision even for games where you know you've done it perfectly the first time.
 * 
 * transitioning old games to the new standard can be tricky, as the expanded format
 * has extra tokens that need to be interpreted.  It's mostly an issue for games on 
 * the server that are incomplete and might be restarted, or spectating in games
 * that are using slightly out of date clients.
 * 
 * @param his
 */
public abstract void performHistoryInitialization(StringTokenizer his);


public void performPlayerInitialization(StringTokenizer his)
{	int fp = G.IntToken(his);
	BoardProtocol b = getBoard();
    if (fp < 0)   {  fp = 0;  }
    b.setWhoseTurn(fp);
    // in restoring poisoned games, the players may not be set up.
    // things are not going well, but don't get null pointer exceptions...
    if((players!=null) && players.length>fp)
    {
    commonPlayer p0 = players[fp];
    if(p0!=null)
    {
    p0.setOrder(0);
    if(players.length>1)
    	{
    	commonPlayer p1 = players[fp^1];
    	if(p1!=null) { p1.setOrder(1); }
    	}
    }
    }
	
}

public void performHistoryTokens(StringTokenizer his)
{	StringBuilder command = new StringBuilder();
    // now the rest
	boolean ended = false;
	boolean first = true;
	int time = -1;
	while (his.hasMoreTokens() && !ended)
    {
        String token = his.nextToken();
        ended = KEYWORD_END_HISTORY.equals(token);
        if (",".equals(token) || ended)
        {
            if (!first)
            {
                PerformAndTransmit(command.toString(), false,replayMode.Replay1);
                if(time>=0) {
                	History.top().setElapsedTime(time);
                }
                command.setLength(0);
                first = true;
                time = -1;
            }
        }
       else if(first && (token.charAt(0)=='+'))
       {	// reserve all first tokens starting with + for future expansion
    	   if("+T".equals(token))
    	   {
    		   time = G.IntToken(his);   		   
    	   }
       }
       else
        {	first = false;
            command.append(" ");
            command.append(token);
        }
    }	
}
/**
 * true if we allow undoing all the way back to the done
 * as a result of a few missed clicks
 * @return
 */
public boolean allowResetUndo() { return(allowPartialUndo()); }
public boolean isPuzzleState() 
{ 	BoardProtocol b = getBoard();
	return b!=null && b.getState().Puzzle();
}
public boolean somethingIsMoving()
{
	BoardProtocol b = getBoard();
	return b!=null && b.movingObjectIndex()>=0;
}
/**
 * reset the state of the game back to the previous "done" state, if that is allowed,
 * or back to the previous "pick" operation, if that is allowed.  This is done by truncating
 * the game record, and replaying back to the new end.  Normally this is triggered by
 * two missed clicks in a row, taken to indicate the user is confused about the state
 * of the game.
 * @see #allowPartialUndo()
 * @see #allowBackwardStep() 
 * 
 */
public void performReset()
{	if(remoteViewer>=0) { return; }
	if(disableForSpectators()) { return; }
	if(!allowed_to_edit && reviewMode()) { scrollFarForward(); }
    else if(ourActiveMove() && (allowResetUndo() || allowBackwardStep() || isPuzzleState())) 
    	{ PerformAndTransmit(RESET); 
    	}
	else  { leaveLockedReviewMode(); }
	if(theChat!=null) { theChat.closeKeyboard(); }
}

private void restorePlayerInfo2(String uid,int ord)
{	
	boolean found = false;
	int ordinal = ord%1000;
	for(commonPlayer pl : players) { if((pl!=null) && uid.equals(pl.uid) && (pl.getOrder()==ordinal)) { found=true; }}
	if(!found)
	{
	boolean change = false;
	for(commonPlayer pl : players)
	{	
		if(pl!=null)
		{
		if(!pl.restored && uid.equals(pl.uid) && (pl.getOrder()!=ordinal))
			{ pl.setOrder(ordinal); 
			  change = true; 
			  pl.restored = true;
			}
		}
	}
	if(change) {commonPlayer.reorderPlayers(players); }
	}
}

private void restorePlayerInfo(int seatposition,int ordinal)
{	boolean change = false;
	for(int i=0;i<players.length;i++)
	{	commonPlayer pl = players[i];
		if(pl!=null)
		{
		if(pl.getPosition()==seatposition) 
			{ if(pl.getOrder()!=ordinal)
				{ pl.setOrder(ordinal); change = true; 
				}
			}
		}
	}
	if(change) {commonPlayer.reorderPlayers(players); }
}


/* replay history as incoming commands.
Used by spectators, multiple reviewers, and players recovering their connection
this consumes the strings produced by {@link #formHistoryString }
*/
public void useStoryBuffer(String tok,StringTokenizer his)
{	
	
	if(tok==null) { tok = his.nextToken(); }
	if(his.hasMoreTokens())
	{	

		while(KEYWORD_PLAYER.equals(tok))
		{	int indx = G.IntToken(his);
			String name = G.decodeAlphaNumeric(his.nextToken());
			extendPlayers(indx);
			commonPlayer p = players[indx];
			if(p==null) { p = players[indx] = new commonPlayer(indx); }
			p.setPlayerName(name,false,this);
			tok = his.hasMoreTokens()? his.nextToken() : KEYWORD_START_STORY;
		}
		while(KEYWORD_PINFO2.equals(tok))
		{
			String uid = his.nextToken();
			int ord = G.IntToken(his);
			restorePlayerInfo2(uid,ord);
			tok = his.hasMoreTokens()? his.nextToken() : KEYWORD_START_STORY;
		}
		while(KEYWORD_PINFO.equals(tok))
		{	int seat = G.IntToken(his);
			int ord = G.IntToken(his);
			restorePlayerInfo(seat,ord);
			tok = his.hasMoreTokens()? his.nextToken() : "";
			if("true".equals(tok)||("false".equals(tok)))
			{	// eat the extra word for backward compatibility.
				// this can be eliminated about 11/1/2009
				tok = his.hasMoreTokens()? his.nextToken() : KEYWORD_START_STORY;
			}
		}
		while(!KEYWORD_START_STORY.equals(tok))
		{
			if (KEYWORD_COLORMAP.equals(tok))
			{
				String cm = his.nextToken();
				getBoard().setColorMap(G.parseColorMap(cm), -1);
			}
			else if(sgf_names.timecontrol_property.equalsIgnoreCase(tok))
			{
				timeControl = TimeControl.parse(his);
				futureTimeControl = timeControl.copy();
			}
			else if("S1".equalsIgnoreCase(tok))
			{	// for future expansion
				his.nextToken();	// skip 1 arg
			}
			else {
				G.print("Unexpected init keyword ",tok);
			}
			tok = his.hasMoreTokens() ? his.nextToken() : KEYWORD_START_STORY;
		}
		// here we found the start token
		performHistoryInitialization(his);
		performPlayerInitialization(his);
	  	//
		// history ought to be completely empty at this point, but typically isn't
		// because it is initialized with a start or edit.  Start an edit ought to
		// be idempotent, but in some cases (such as ManhattanProject, 4-5 players) are not.
		// this makes damn sure the history is empty when we start replaying a game, where
		// the first move ought to be a "start"
	    //
		resetHistory();

		performHistoryTokens(his);
		
		doScrollTo(FORWARD_TO_END);
		
		saveDisplayBoard();
 


	}
}

public void useRemoteStoryBuffer(String tok,StringTokenizer his)
{	
	if((tok!=null) && tok.startsWith(KEYWORD_SPARE))
	{
		// trap door to make future expansion easier
		// valid from v7.50 on
		tok = his.nextToken();
		tok = his.nextToken();
	}
	if(KEYWORD_ROBOTMASTER.equalsIgnoreCase(tok))
	{ // this keyword is used in normal games, we need to ignore it for remote viewers
	tok = his.nextToken(); 
	tok = his.nextToken(); 	
	}

	useStoryBuffer(tok,his);
    if(his.hasMoreTokens())
    {
    //String playerToMove = 
    G.IntToken(his);	// skip player to move
    G.IntToken(his);			// my ordinal
    useEphemeraBuffer(his);
    }
}
/**
 * this replays the moves in the ephemeral part of a game record, which were
 * generated by {@link #formEphemeralHistoryString} In all games, this should
 * include the player times and the current scroll position.   
 * If {@link #gameHasEphemeralMoves} is true, it will also look to a second
 * block of move specifiers corresponding to the ephemeral moves.
 * 
 * its possible to override or wrap both this and {@link #formEphemeralHistoryString}
 */
public void useEphemeraBuffer(StringTokenizer his)
{	for(int i=0; i<players.length && his.hasMoreElements(); i++)
	{	//commonPlayer pli = players[i];
		commonPlayer plp = commonPlayer.findPlayerByPosition(players,i);
		long tim = G.LongToken(his);
		if(plp!=null) 
		{ plp.setElapsedTime(tim); 
		//G.print("Restore "+plp+" "+tim+ ((pli==plp)?"":" not "+pli));
		}
	}	
	if(his.hasMoreElements())
	{
		String tok = his.nextToken();
		if(KEYWORD_SCROLL.equals(tok))
		{	// if a scroll position is specified, scroll there.
			int pos = G.IntToken(his);
			if(pos>=0) { doScrollTo(pos); }
		}
	}
	if(gameHasEphemeralMoves())
	{
		useEphemeralMoves(his);
	}
	resetBounds();
}
/**
 * sort the ephemeral moves into their final order.  Normally is is
 * just ordering the moves so all of each players moves are together.
 * 
 * @param ephemera
 */
public void ephemeralSort(CommonMoveStack ephemera)
{	for(int i=0,siz=ephemera.size();i<siz;i++)
	{
	commonMove m = ephemera.elementAt(i);
	m.setEvaluation((m.player*1000+m.index()));	// set evaluation for the sort
	}
	ephemera.sort(false);
}
/**
 * convert the ephemeral moves into non-ephemeral equivalents, and
 * sort them into canonical order.  This is called at the end of
 * the placement phase to bake-in the players placements. In most
 * cases, this method can be left as-is except to implement the
 * helper function {@link convertToSynchronous}
*/
public void canonicalizeHistory()
{	
	CommonMoveStack  h = History;
	CommonMoveStack ephemera = new CommonMoveStack();
	CommonMoveStack permanent = new CommonMoveStack();
	while(h.size()>0) 
		{ commonMove m = h.pop();
		  if(m.isEphemeral())
		  { 
		    ephemera.push(m); 
		  }
		  else
		  { permanent.push(m); 
		  }
		}
	
	ephemeralSort(ephemera);
	
	commonMove prev = null;
	while(permanent.size()>0)
	{	commonMove m = permanent.pop();
		if(prev!=null) { prev.next = m; }
		prev = m;
		m.next = null;
		m.setIndex(h.size());
		h.push(m);
	}
	// copy the ephemeral
	while(ephemera.size()>0)
	{	// convert to synchronous changes the opcode and makes any other necessary 
		// changes.  It can return null to remove the move completely.
		commonMove m = convertToSynchronous(ephemera.pop());
		if(m!=null)
		{
		prev.next = m;
		prev = m;
		m.next = null;
		// this is a subtle point.  The now-non-epheral moves have been reordered,
		// so the digests are invalid.  The invalid digests can trigger invalid removals
		// due to "digest not changed" if there are undo/redo steps after.
		m.digest = 0;
		m.setIndex(h.size());
		h.push(m);
		}
	}
	//G.print("Canonicalize to "+formStoryString());
	
}
/**
 * convert an ephemeral move to it's no-ephemeral equivalent.  It's also
 * ok to return null meaning the move should be deleted.  Normally, all
 * this will do is change the m.op field, but it needs to agree with 
 * the behavior of {@link commonMove#isEphemeral} method.
 * @param m
 * @return a changed move, or null
 */
public commonMove convertToSynchronous(commonMove m)
{	throw G.Error("Not implemented");
}


/** replay the just the ephemeral moves of the move history
 *  this is called from {@link #useEphemeraBuffer} and can be wrapped or
 *  overridden, in cooperation with {@link formEphemeralMoveString } to change the behavior
 * @param his
 */
public void useEphemeralMoves(StringTokenizer his)
{
   	performHistoryTokens(his);		// extra tokens for ephemeral moves after the end of the standard ephemeral stuff
}
/**
 * this is left poining to the duplicate board if an error is thrown from verifyGameRecord,
 *  so you can use the "show alternate board" to see that the bad duplicate looks like.
 */
private BoardProtocol dupBoard=null;	
public boolean DISABLE_VERIFY = false;
/**
 verify the game record is still correct by cloning the board
 and replaying it.  This is intended to catch "editing errors"
 by editHistory, but may also catch cases where Digest() is
 not producing the same result.  In the process this exercises
 a lot of the game machinery, so other latent bugs are likely
 to show up right away as errors thrown from verifyGameRecord
 <br>To assist with certain phases of debugging, where all this replaying
 can be confusing, set DISABLE_VERIFY=true;
 * 
 */
public void verifyGameRecord()
{	// this thrashes the garbage collector, and on IOS thrashing the GC
	// induces a lockup between sound and point handling.
	// https://github.com/codenameone/CodenameOne/issues/2860
	if(!G.isCodename1() && G.debug())
	{if(DISABLE_VERIFY) {  G.print("verifygamerecord is off"); }
	else {
	BoardProtocol ourB =  getBoard();
	long ourDig = ourB.Digest();
	BoardProtocol dup = dupBoard = ourB.cloneBoard();
	dup.setName("verify "+ourB.getName());
	long dupDig = dup.Digest();
	// first get a duplicate of the board, it had better be good
	if(dupDig!=ourDig)
	{
	// hope that sameboard will produce a better error message
	ourB.sameboard(dup);
	G.Advise(dupDig!=ourDig,"Bad duplicate board - Digest Doesn't Match");
	}
	//
	// now reinitialize the copy and replay all the moves.
	//
	dup.doInit();
	
	G.Advise(ourB.Digest()==ourDig,"our digest changed unexpectedly, probably shared structure");
		
	
	int step = History.size();
	int limit = History.viewStep>=0 ? History.viewStep : step;
	for(int i=0;i<limit;i++) 
		{ commonMove mv = History.elementAt(i);
		  //G.print(".. "+mv);
		  dup.Execute(mv,replayMode.Replay);
		  // checkForRepetition maintains the gameover flag when it calls SetDrawState.
		  if(mv.gameover()) { dup.SetDrawState(); }
		}
	
	long dupRedig = dup.Digest();
	G.Advise(dup.whoseTurn()==ourB.whoseTurn(),"Whose turn mismatch, live %d vs replay %d",ourB.whoseTurn(),dup.whoseTurn());
	G.Advise(dup.moveNumber()==ourB.moveNumber(),"Move number mismatch, live %d vs replay %d",ourB.moveNumber(),dup.moveNumber());
	if(dupRedig!=ourDig)
	{
	// if the digest doesn't match, call sameboard in hope of getting a better error message
	ourB.sameboard(dup);
	// if sameboard passed, all we can say is the the digest doesn't match.  Tough to debug from here.
	//int d0 = ourB.Digest();
	//int d1 = dup.Digest();
	G.Advise(false,"Digest after replay doesn't match, but sameboard(dup) is ok");
	}
	// note: can't quite do this because the timing of "SetDrawState" is wrong.  ourB
	// may be a draw where dup is not if ourB is pending a draw.
	//G.Assert(dup.getState()==ourB.getState(),"Replay state matches");
	dupBoard = null;
	}}
	}

    public boolean playerChanging()
    {
    	commonPlayer who = currentGuiPlayer();
    	commonMove top = History.top();
    	boolean changed = (isOfflineGame() || (who==l.my)) && ((who==null) || (who.boardIndex!=top.player));
    	if(changed && (top==l.changeMove)) { return(false); }
    	else if(changed) { l.changeMove = top; }
    	return(changed);
    }
    private void playTurnChangeSounds()
    {
        if (myFrame.doSound())
        {	playASoundClip(isOfflineGame()||hasGameFocus()
				? turnChangeSoundName 
				: beepBeepSoundName,500);
        }
    }
    private boolean hasGameFocus=false;
    public boolean hasGameFocus() { return(hasGameFocus); }
    public void setHasGameFocus(boolean v) 
    	{ hasGameFocus = v; 
    	}
    
    public void paintSprites(Graphics g,HitPoint hp)
    {
    	super.paintSprites(g, hp);
    	drawUnseenChat(g);		// draw a popup bubble with incoming chat
    	
    }

    public boolean globalPinchInProgress()
    {
    	return(super.globalPinchInProgress()
    			|| ( mouse.mouse_pinch && !inBoardZoom)
    			);
    }
    
    public void stopPinch()
    {	Log.finishLog();
    	l.boardZoomStartValue = 0.0;
    	super.stopPinch();
    }
    public boolean panInProgress() { return(super.panInProgress() || draggingBoard);}

/**
 * return true if it is possible to start a touch zoom "right now"
 */
    public boolean touchZoomEnabled() 
    {
    	if(panInProgress()) { return(false); }	// if already panning, don't touch zoom
    	if(draggingBoard) { return(false); }
    	int x = mouse.getX();
		int y = mouse.getY();
		if(avoidChat(x,y)) { return(false); }
		return(true); 
    }
    private boolean draggingBoard = false;
    /**
     * note if a "drag board" operation is in progress.  This is used to 
     * lock out touch zoom for the duration of the pan
     * 
     * @param val
     */
    public void setDraggingBoard(boolean val)
    {	draggingBoard=val;
    }
    public boolean draggingBoard() { return(draggingBoard); }
    
    /**
     * determine that a "board drag" operation should be started.  This means
     * nothing has been hit by the mouse, which has actually moved, in the
     * game board rectangle, and not in the vcr zoom area, and touch zoom is
     * not in progress.
     * @param anySelect
     * @param tbRect
     * @return true if the drag should be started
     */
    public boolean startBoardDrag(HitPoint anySelect,Rectangle tbRect)
    {
	   	if((anySelect!=null)
	   		&&(anySelect.hitCode==DefaultId.HitNoWhere)	// nothing is hit 
	   		&& G.pointInRect(anySelect,tbRect)			// in the board rectangle
	   		&& !G.pointInRect(anySelect,vcrRect) 		// not in the vcr rectangle
	   		&& !touchZoomInProgress()					// not already doing a touch zoom
	   		&& (annotationMenu.getSelected()==null)		// not dragging an annotation
	   		&& (getDragPoint()==null))					// not already dragging
	   	{ //let him drag anywhere, just don't annoy with the hand icon.
 		anySelect.dragging = mouse.isDragging();		// mouse is actually moving
 		return(true);
	   	}
	   	return(false);
	}
    /* handle board dragging for boardless games.
     * 
     */
    public boolean doBoardDrag(Rectangle tbRect,HitPoint anySelect,int cs,CellId id)
    {
    	HitPoint mo = getDragPoint();
    	boolean in = G.pointInRect(anySelect,tbRect);
    	boolean nowDragging = in && (mo!=null) && anySelect.down;
    	if(nowDragging)
	      	{	if(cs>0)
	      		{int w = G.Width(tbRect);
	      		int h = G.Height(tbRect);
	      		double across = w/cs;
	      		double down = h/cs;
	      		double center_x = (G.Left(anySelect)-G.Left(mo))*across;
	      	    double center_y = (G.Top(anySelect)-G.Top(mo))*down;
	      	    double z = zoomRect==null ? 1 : zoomRect.value/2.5;
	      	    // wrap the board center so the virtual board is 2x real board.  This allows
	      	    // things to scroll all the way off before they start appearing on the opposite edge.
	      	    board_center_x = G.rangeLimit(board_center_x+(center_x / w),across*z);
	      	    board_center_y = G.rangeLimit(board_center_y-(center_y / h),down*z);
	      	    G.SetTop(mo,G.Top(anySelect));
	      	    G.SetLeft(mo,G.Left(anySelect));
	      	    repaint(20);
	      		}
	      	}
         if(startBoardDrag(anySelect,tbRect)||nowDragging)
      		{ anySelect.hitCode = id; 
      		  // if this is a "second cycle" where drag is being done, make sure the
      		  // current drag point retains the drag id.  It was cleared in the
      		  // drawCanvas method
      		  if(mo!=null) { mo.hitCode = id; }
      		}
     	 setDraggingBoard(nowDragging);

    	 return(nowDragging);
    }
    
    /**
     * if true, a "lifted" view is in effect
     */
    private boolean lifted = false;
    public Rectangle liftRect = addRect("lift");
    public int liftSteps = 0;
    /**
     * this activates refreshes with progressively more, then less, steps in liftSteps
     * it also inhibits touch zoom, and returns true if lift animation is in progress
     * @return
     */
    public boolean doLiftAnimation()
    {
    	if(lifted)
    	{
    		if(liftSteps<12) { repaint(20); liftSteps++; }
    	}
    	else 
    	{
    		if(liftSteps>0) { repaint(20); liftSteps--; }
    	}
	 	boolean dolift = (liftSteps>0);
	 	setDraggingBoard(dolift);	// inhibit touch zoom while in contact
	 	return(dolift);
    }
    
    /**
     * draw a "lift" icon with standard behavior, and maintain the variable
     * "lifted".  on press, an animated lift process should start, and continue
     * until release, even if the contact point drifts off the original location
     * 
     * @param gc
     * @param liftRect
     * @param highlight
     * @param image
     * @return
     */
    public boolean drawLiftRect(Graphics gc,Rectangle liftRect,HitPoint highlight,Image image)
    {
    	image.centerImage(gc,liftRect); 
    	HitPoint.setHelpText(highlight, liftRect, s.get(LiftExplanation));
		GC.frameRect(gc,Color.black,liftRect);
		if(highlight!=null)
		{
	    	if(G.pointInRect(highlight,liftRect))
	    	{	highlight.dragging = lifted = highlight.down;
	    		highlight.hitCode = GameId.HitLiftButton;
	    		highlight.spriteRect = liftRect;
	    		highlight.spriteColor = Color.red;
	    	}
	    	else 
	    	{
	    		if(!highlight.down) { lifted = false; }
	    	}
		}
		return(lifted);
    }
    
    public void Pinch(int realx,int realy,double val,double twist)
    {	
		int cx = getSX();
		int cy = getSY();
		//if(startingPinch) { Log.restartLog(); }
		//Log.addLog("X "+realx+","+realy+" "+val+" "+cx+","+cy);

		int x = cx+realx;
		int y = cy+realy;
		
    	if(doBoardPinch(x,y,val)) {
    		//Plog.log.addLog("board pinch ",x,",",y," ",val);
    	}
    	else 
    	{   
    		super.Pinch(realx,realy,val,twist);
    	}
    }

    private int layoutErrors = 0;
    private void checkLayouts()
    {
    	layoutErrors = 0;
    	int minw = getWidth()/2;
    	int minh = getHeight()/2;
    	int w = getWidth();
    	int h = getHeight();
    	Random r = new Random();
    	int neww=0;
    	int newh=0;
    	for(int i=0;(i<10000)&&(layoutErrors==0);i++)
    	{
    		neww = r.nextInt(w-minw)+minw;
    		newh = r.nextInt(h-minh)+minh;
    		double fac = G.adjustWindowFontSize(neww,newh);
      	  	adjustStandardFonts(fac*G.defaultFontSize);
      	  	setLocalBoundsSync(0,0,neww,newh);
    		if(layoutErrors==0) { layoutErrors += checkLayoutBounds(); }
    	}
    	if(layoutErrors==0) 
    		{ 
    		  double fac = G.adjustWindowFontSize(w,h);
      	  	  adjustStandardFonts(fac*G.defaultFontSize);

      	  	  setLocalBoundsSync(0,0,w,h); 
    		  G.print("ok"); 
    		}
    	else
    	{  
    	   G.print("New size "+neww+",",newh); 
    	}
    	generalRefresh();  	
    }
    /**
     * default implementation for games, which selects wide tall or normal layout
     */
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	setLocalBoundsWT(x,y,width,height);
    }

    public double setLocalBoundsA(int x, int y, int width, int height,double aspects)
    {
    	throw G.Error("Override this method if you are using setLocalBoundsV");
    }
    /**
     * set local bounds, varying one parameter.  Normally, this is the "preferred aspect ratio"
     * 
     * the weakness of the layout manager is that it tries to optimize the size of the 
     * hole available for the board and "everything else", without knowing anything about
     * what the everything else is.  This sometimes does less well when the hole is 
     * the perfect size, then the "everything else" chips away at it.
     * 
     * running a few "wider" and "narrower" values helps these cases choose a better result.
     * in general, this algorithm is conservative in the sense that it will never choose a
     * worse result than we used to get anyway.
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     * @param aspects a small array of doubles
     */
    public void setLocalBoundsV(int x, int y, int width, int height,double aspects[])
    {	
    	double best = 0;
    	double last = 0;
    	double bestsize = 0;
    	for(double as : aspects)
    	{	last = as;
    		contextRotation = 0;
    		// bs is a score, nominally the number of pixels in the board.
    		double bs = setLocalBoundsA(x,y,width,height,as);
    		if(bs>=bestsize 
    				// avoid selecting layouts with failed allocaions
    				&& (selectedLayout.fails==0)
    				)
    		{
    			best = as;
    			bestsize = bs;
    		}
    		//if(G.debug()) { System.out.println("aspect "+as+" s "+bs+" "+selectedLayout); }
    	}

    	if(best!=last)
    	{	contextRotation = 0;
    		setLocalBoundsA(x,y,width,height,best);
    	}
		//if(G.debug()) { System.out.println("Best "+best+" "+width+"x"+height); }
    }
    /**
     * this implements the standard "game" setLocalBounds which yields 
     * different layouts for "wide" and "tall" formats, whatever that 
     * means for a particular game.  
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public void setLocalBoundsWT(int x,int y,int width,int height)
    {
    	int wide = setLocalBoundsSize(width,height,true,false);			// get the "wide" size metric
    	int tall = setLocalBoundsSize(width,height,false,true);			// get the "tall" size metric
    	int normal = setLocalBoundsSize(width,height,false,false);		// get the "normal" size metric
    	boolean useWide = wide>normal && wide>tall;
    	boolean useTall = tall>normal && tall>=wide;
    	if(useWide||useTall) { setLocalBoundsSize(width,height,useWide,useTall); }	// recall with the chosen metric
    	setLocalBoundsWT(x,y,width,height,useWide,useTall);			// do the actual layout
    }
    
    public Rectangle rotateForPlayer(String key)
    {	Rectangle r = allRects.get(key);
    	{
		for(int i=0,np=nPlayers(); i<np;i++)
		{
			commonPlayer pl = getPlayerOrTemp(i);
			if((pl.displayRotation!=0) && pl.playerBox.contains(r)) 
				{ Rectangle cop = G.copy(null,r);
				  G.setRotation(cop, pl.displayRotation,G.centerX(pl.playerBox),G.centerY(pl.playerBox));
				  return(cop);
				}
		}
    	}
    	return(r);
    }
    private int checkLayoutBounds()
    {	StringStack keys = new StringStack();
    	Hashtable<String,Rectangle> cop = new Hashtable<String,Rectangle>();
    	int errors = 0;
    	for(Enumeration<String> k = allRects.keys(); k.hasMoreElements();) 
		{	String key = k.nextElement();
			cop.put(key,G.copy(null,allRects.get(key)));
			keys.push(key);
		}
    	setLocalBoundsSync(G.Left(fullRect),G.Top(fullRect),G.Width(fullRect),G.Height(fullRect));
    	for(int sz=keys.size(),i=0;i<sz;i++)
    		{	String key = keys.elementAt(i);
    			Rectangle r1 = allRects.get(key);
    			Rectangle r0 = cop.get(key);
    			if(!r0.equals(r1))
    			{	errors++;
    				G.print("\nunstable "+" "+key+"\n"+r0+"\n"+r1);
    			}
    		}
      	
    	for(int lim = keys.size()-1; lim>=0; lim--)
    	{	String key = keys.elementAt(lim);
    		if(key.charAt(0)!='.')
    		{
			Rectangle r1 = rotateForPlayer(key);
			if( G.Height(r1)!=0 && G.Width(r1)>0 && !r1.intersects(fullRect) && !fullRect.contains(r1)) 
			{	
				errors++; 
				G.print("out of bounds "+key+"\n"+r1+"\n"+fullRect); 			
			}
			else {
				
				for(int lim2 = lim-1; lim2>=0; lim2--)
				{ String key2 = keys.elementAt(lim2);
				  if(key2.charAt(0)!='.')
				  {
				  Rectangle r3 = rotateForPlayer(key2);
				  if(r1.contains(r3)) {}
				    else if(r3.contains(r1)) {}
					else if(r1.intersects(r3)) 
						{ 
						errors++;
						//G.print("\noverlap "+key+" "+key2+"\n"+r1+"\n"+r3); 
						}
				}}
				
			}}
		}
    	return(errors);
    }
    
    private String unseenPop = null;
    private int unseenPopX = 0;
    private int unseenPopY = 0;
    private long unseenPopTime = 0;
    
    public void drawNoChat(Graphics gc,Rectangle r,HitPoint highlight)
    {	boolean cansee = seeChat();
    	CellId hitcode = cansee 
    						? GameId.HideChat 
    						: GameId.ShowChat;
    	String msg = !cansee
				? s.get(ShowChatMessage)
				: s.get(HideChatMessage);
    	StockArt img = cansee ? StockArt.Nochat : StockArt.Chat;
    	
    	img.drawChip(gc, this, r, highlight,hitcode, msg);
    	
    	long now = G.Date();
    	
    	// maintain the unsee chat popup.  It's drawn last rather than right here.
    	if(!cansee && (theChat!=null) && theChat.hasUnseenContent())
    		{ 
    		if(((now/1000)&1)==0) { GC.frameRect(gc, Color.blue,r); }
    		repaint(1000);
    		if(unseenPop==null)
    		{
    		// if the chat is idle for a second, and there is unseen content
    		// show it as a tooltip.
    		long idle = theChat.getIdleTime();
    		if(idle>1000 )
    		{	unseenPopX = G.centerX(r);
    			unseenPopY = G.centerY(r);
    			unseenPop = theChat.getUnseenContent();
    			unseenPopTime = now;
    		}}
    	   	
    		}
    }
    /**
     * return a metric for the cell size for a particular layout. Nominally,
     * this corresponds to the square size of the board.  The intention is
     * that setLocalBoundsWT will select the layout option that yields the 
     * biggest board.
     * @param w
     * @param h
     * @param wide
     * @param tall
     * @return the "square size" for this size window, using the normal, wide, or tall template.
     */
    public int setLocalBoundsSize(int w,int h,boolean wide,boolean tall)
    	{ throw G.Error("you must override this method"); 
    	}
    /**
     * do setLocalBounds with wide, tall, or normal formatting
     * @param x
     * @param y
     * @param w
     * @param h
     * @param wide
     * @param tall
     */
    public void setLocalBoundsWT(int x,int y,int w,int h,boolean wide,boolean tall)
    	{ G.Error("you must override this method"); 
    	}
    /**
     * set a standard progress rect, inset in the goal rect
     * @param progress
     * @param goal
     */
    public void setProgressRect(Rectangle progress,Rectangle goal)
    {	int w = G.Width(goal);
    	int h = G.Height(goal);
    	G.SetRect(progress, G.Left(goal)+w/3,G.Top(goal)+h/4,w/3,h/3);
    }
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     */
    public void drawCanvas(Graphics offGC, boolean complete,HitPoint hp)
    {	//G.addLog("draw canvas");
    	if(startedOnce)
    	{
    	Keyboard k = theChat!=null ? theChat.getKeyboard() : null;
    	drawFixedElements(offGC,complete);
    	// draw the board contents and changing elements.
     	int hx = G.Left(hp);
     	int hy = G.Top(hp);
     	if(offGC!=null) { chatHasRun = false; }
     	else { nullChatHasRun = false; }
 
     	redrawClientBoard(offGC,k==null ? hp : null);		// no mouse activity if keyboard is up      
        if(!((offGC==null) ? nullChatHasRun : chatHasRun))
        	{ // this draws the chat window if it wasn't explicitly drawn during the redraw
        	redrawChat(offGC,hp); 
        	}
        drawKeyboard(offGC,hp);


        G.SetLeft(hp, hx);
        G.SetTop(hp,hy);
        //      draw clocks, sprites, and other ephemera
        drawMice(offGC);
       
        HiddenGameWindow hidden = findHiddenWindow(hp);
		if(hidden==null)
			{ GC.setRotatedContext(offGC,hp,currentPlayerRotation());
			  DrawArrow(offGC,hp);
			  GC.unsetRotatedContext(offGC,hp);
			}
		else { 
			DrawArrow(offGC,hp);
		}
       
        drawUnmagnifier(offGC,hp);
    	}
    }
 
    /** these are drawn separately, directly on the canvas.  They
    might seem to flash on and off.
    */
    public void drawCanvasSprites(Graphics offGC, HitPoint hp)
    {	
        DrawTileSprite(offGC,hp); //draw the floating tile, if present
        drawSprites(offGC);
    }
    public void setTheChat(ChatInterface chat,boolean framed)
    {
    	super.setTheChat(chat,framed);
    	hidden.separateChat = framed;
    	
    }
	public void setSeeChat(boolean v) 
	{	
		if(hidden.hiddenChat==v)
		{   hidden.hiddenChat = !v;
			if(hidden.separateChat)
				{
				if(v && theChat!=null) { theChat.moveToFront(); }
				}
				else 
				{ 
				  resetBounds();
				}
		}
	}
	private boolean seeChat() { return(!hidden.hiddenChat); }
	
	public void drawHiddenWindow(Graphics gc, HitPoint hp, int index,Rectangle bounds) 
	   {
		G.print("Draw hidden window ",index);
	   }
    public void drawHiddenWindowRemote(Graphics gc, HitPoint hp, int index,Rectangle bounds) 
    {
    	if(startedOnce) { drawHiddenWindow(gc,hp,index,bounds); }
    	int width = G.Width(bounds);
    	int height = G.Height(bounds);
    	int size = G.minimumFeatureSize()/2;// Math.min(width, height)/20;
    	GC.draw_anim(gc, new Rectangle(width-size,height-size,size,size),size*3/4, lastInputTime, progress);
    }
	public String nameHiddenWindow()
	   {
		   return("");
	   }

	//
	// call from main window drawing, or from redrawClientBoard if this is a remote viewer.
	// the argument "highlight" is not used if were are drawing on an offscreen window
	// and it must be used if we are drawing to a remote window.
	//
	public void drawHiddenWindows(Graphics useAgc, HitPoint highlight)
	    {	// the GC is not used, but if it is not null, we get one and really draw
			// otherwise this is a scan for mouse effect only
	    	if(remoteViewer>=0)
	    	{	double z =getGlobalZoom();
	    		int w = (int)(getWidth()*z);
	    		int h = (int)(getHeight()*z);
	    		drawHiddenWindowRemote(useAgc,highlight,remoteViewer,new Rectangle(0,0,w,h));
	    	}
	    	else
	    	{
	    	if(l.hiddenWindows!=null)
	    	{	// we only need to do this when we're really drawing
	    		// the mouse movement phase is done as a callback from
	    		// the remote window.
	    		for(HiddenGameWindow window : l.hiddenWindows)
	    		{	if(window!=null)
	    		{	{window.setName(s.get(nameHiddenWindow(),prettyName(window.getIndex())));
	    			if(window.hasConnection())
	    			{
	    			Graphics gc =  useAgc!=null ? window.getGraphics() : null;
	    			window.redraw(gc);	
	    				}}
	    		}}
	    	}
	    	// set the names of the remote screens 
	    	RpcInterface ss[] = l.sideScreens;
	    	if(ss!=null)
	    		{
	    		for(int lim = ss.length-1; lim>=0; lim--)
	    		{
	    		RpcInterface s1 = ss[lim];
	    		if(s1!=null) 
	    			{ s1.setName(s.get(nameHiddenWindow(),prettyName(lim))); 
	    			}
	    		}
	    	}}
	    }
	    // factory method to override
	   	public HiddenGameWindow makeHiddenWindow(String name,int index,int width,int height)
	   	{
	   		return new HiddenGameWindow(name,index,this,width,height);
	   	}
	    // call from game setup
	    public void createHiddenWindows(int np,int width,int height)
	    {   l.createHiddenWindows(np, width, height); 
	    }
	    // find the board that corresponds to hp, or null if its the main board
	    public HiddenGameWindow findHiddenWindow(HitPoint hp)
	    {	return l.findHiddenWindow(hp);
	    }
	    public int remoteWindowIndex(HitPoint hp)
	    {
	    	if(remoteViewer>=0) { return(remoteViewer);}
	    	HiddenGameWindow hidden = findHiddenWindow(hp);
	    	if(hidden!=null) { return(hidden.getIndex()); }
	    	return(-1);	    	
	    }
	    
	    public HiddenGameWindow getHiddenWindow(int n)
	    {	return(remoteViewer>=0?null:l.getHiddenWindow(n));
	    }
	    // hidden game window is active
	    public void touchPlayer(int index)
	    {	commonPlayer p = getPlayer(index);
	    	if(p!=null)
	    	{
	    		p.UpdateLastInputTime();
	    	}
	    }
	    public void notifyFinished(int index)
	    {	commonPlayer p = getPlayer(index);
	    	if(p!=null)
	    	{
	    		p.ClearLastInputTime();
	    	}
	    }
	    
	    /**
	     * get the rotation for help text, based on the current player's orientation
	     */
	    public double getHelpTextRotation()
	    {
	    	commonPlayer p = players[getBoard().whoseTurn()];
	    	double rotation = p!=null ? p.displayRotation : 0;
	    	return(rotation);
	    }

	    public String getErrorReport()
	    {	return(gameRecordString());
	    }
	    public Bot salvageRobot()
	    {
	    	return ((Bot)sharedInfo.get(WEAKROBOT));
	    }
	    private double currentPlayerRotation()
		{	commonPlayer wt = currentGuiPlayer();
			return wt==null ? 0 : wt.displayRotation;
		}
		boolean inBoardZoom = false;
		private boolean inBoardZoom(int x,int y)
		{	inBoardZoom = (getGlobalZoom()<=MINIMUM_ZOOM)	// global zoom not in effect
					&& (globalZoomRect!=null) 
					&& zoomRect!=null
							&& G.pointInRect(x,y,boardRect);
			return(inBoardZoom);
		}
		
		public boolean doBoardPinch(int x,int y,double amount)
		{
			if(inBoardZoom(x,y))
			{
				if(amount<0) { l.boardZoomStartValue = zoomRect.value; }
				else { zoomRect.setValue(l.boardZoomStartValue*amount); }
		   		MouseMotion(x, y, MouseState.LAST_IS_DRAG);	
		   		repaint();
				return(true);
			}
			return(false);
		}
		public boolean doBoardZoom(int x,int y,double amount)
		{
			if(inBoardZoom(x,y))
			{
				l.boardZoomStartValue = zoomRect.value;
		   		zoomRect.setValue(l.boardZoomStartValue*amount);
		   		repaint();
		   		//this call to MouseMotion violates recursion 
		   		//MouseMotion(x, y, MouseState.LAST_IS_DRAG);	
		   		return(true);
			}
			return(false);
		}
		
		public void Wheel(int x,int y,int mod,double amount)
		{
			boolean moved = (mod==0) 
						&& (gameLog.doMouseWheel(x,y,amount)
								|| doBoardZoom(x,y,(amount>0 ? 1.1 : 0.91)));
			if(!moved)
			{			
				super.Wheel(x,y,mod,amount);
			}
		}
		/**
		 * position the chat window, and also as a side effect set the colors
		 * for the chat window and the chat scroll bar, and the game log scroll bar.
		 */
		public void positionTheChat(Rectangle specRect,Color chatBackgroundColor,Color buttons)
		{	if(chatBackgroundColor!=null) { gameLog.backgroundColor = chatBackgroundColor; }
			if(buttons!=null) { gameLog.foregroundColor = buttons; }
			super.positionTheChat(specRect,chatBackgroundColor,buttons);
		}
		public void actionPerformed(ActionEvent e)
		{
			deferredEvents.actionPerformed(e);
		}
		public boolean allowRobotsToRun(commonPlayer pl)
		{	// don't allow robots during simultaneous moves
			RecordingStrategy rm = gameRecordingMode();
			return((rm==RecordingStrategy.All) 
					|| (rm==RecordingStrategy.None)
					|| (rm==RecordingStrategy.Single));
		}
	    public void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight,CellId id)
	    {	StockArt.Rotate180.drawChip(gc, this, r,highlight, id, s.get(ReverseViewExplanation));
	    }

	    public void drawViewsetMarker(Graphics gc,Rectangle r,HitPoint highlight)
	    {	StockArt icon = null;
	   		GameId button = null;
	    	String help = null;
	    	switch(currentViewset)
	    	{
	    	default:
	    	case Normal: 
	    		icon = StockArt.FaceToFace;
	    		button = GameId.FacingView;
	    		help = OptimizeForFacingString;
	    		break;
	    	case Reverse:
	    	case Twist:
	    		icon = StockArt.SideBySide;
	    		button = GameId.NormalView;
	    		help = OptimizeForSideString;
	    	}
	    	if(icon!=null)
	    	{
	    	if(icon.drawChip(gc,this,r,highlight,button,help))
	    		{
	    		highlight.spriteRect = r;
	    		}
	    	}
	    }
	    
	    //
	    // support for recording active games
	    //
	    public String fixedServerRecordMessage(String fixedHist)
	    {
	    	// note that the exact text of this string has to be the same as generated
	    	// by all players, otherwise mismatches will occur in appended game state
	        String ephemera = formEphemeralHistoryString();
	        commonPlayer pl = currentGuiPlayer();
	        int order = (pl==null)?0:pl.getOrder();
	        String msg = 
	        	G.concat(
	        	fixedHist 
	        	// add the ephemeral part
	        	, " " , KEYWORD_END_HISTORY
	        	, " " , order 			// player to move, ignored but informative
	        	, " " , getActivePlayer().getOrder() 	// my player
	        	, " " , ephemera);
	        	// even the "fixed" history may not be fixed, because it reflects
	        // the user's random mouse gestures and undos
	        return(msg);
	    }

	    //
	    // one player must be agreed to be "robotmaster" and run
	    // the robot.  There are an annoying number of special 
	    // cases.
	    // 1) from a clean start, the player with the start button is the robot master
	    // 2) from a complete restart, the player with the start button is the robot master
	    // 3) when the robot master has quit, and is replaced by a robot
	    // 4) when the robot master has quit, and rejoins 
	    // 5) when the robot master rejoins as a spectator
	    // 6) when some other player rejoins, allowing the game to resume.
	    //
	    // in a clean start, the robot master is always the player
	    // with the start button, who started the game.  We track
	    // that player by the starting order he has been assigned.
	    // -- previous to 2.72, we tracked him by the seat position chosen.
	    
	    public String fixedServerRecordString(String orderInit,boolean includePlayerNames)
	    {	
	    	// note that the exact text of this string has to be the same as generated
	    	// by all players, otherwise mismatches will occur in appended game state
	    	
	    	//
	    	// history is based on player indexes - p0 moves first, p1 second
	    	// note well: case (and everything else) of these strings must match among players
	    	//
	    	
	    	// we can always send move times to the server if it is new enough, 
	    	// old clients will never see them because they will be filtered out
	        String hist = formStoryString(true, true);
	        StringBuilder playerinfo = new StringBuilder();
	        StringBuilder orderinfo = new StringBuilder(orderInit);
	        {
	        
	        commonPlayer players[] = getPlayers();

	
	        for(int i=0;i<players.length;i++)
	        	{	commonPlayer p = players[i];
	        		if(p!=null) 
	        		{   if(includePlayerNames)
	        	        {	
	        			G.append(playerinfo, KEYWORD_PLAYER , " ",i," ",G.encodeAlphaNumeric(p.trueName), " ");
	        	        }
	         			G.append(orderinfo, KEYWORD_PINFO2 , " ",p.uid , " ",(p.getOrder()+1000)," ") ;
	         		}
	        	}}
	        playerinfo.append(orderinfo.toString());

	        String map = colorMapString();
	        
	        if (map!=null) { G.append(playerinfo, KEYWORD_COLORMAP," ",map," " ); }
	        
	        TimeControl time = timeControl();
	        
	        if(time!=null)
	        {
	        	G.append(playerinfo,sgf_names.timecontrol_property , " ",time.print()," ");
	        }

	        G.append(playerinfo, KEYWORD_START_STORY , " ",hist);
	        
	        String val = playerinfo.toString();
	       return(val);
	    }

	    //
	    // this version is used only for the remote RPC viewers
	    //
	    public String serverRecordString()
	    {
	    	String fixedHist = fixedServerRecordString("",true);	// include the player names
	    	String msg = fixedServerRecordMessage(fixedHist);
	    	return(msg);
	    }
	    public void testSwitch()
	    {
	    	autoOptimize = !autoOptimize;
	    	resetBounds();
	    }
	    public boolean autoDone = false;
	    public boolean enableAutoDone = false;
	    // this flag tweaks the interaction of "undo" and "autodone".  When undoing,
	    // if autodone would immediately redo, done do it.  This makes undo with autodone
	    // work the same as regular undo, and avoids problems where the clients can disagree
	    // about the state of the board.
	    public boolean skipAutoDone = false;
		public boolean enableAutoDone() { return (enableAutoDone); }
		private JCheckBoxMenuItem autoDoneCheckbox = null;
		public boolean mandatoryDoneState() { return OurMove() && getBoard().DoneState(); }
		public void sendDone() { PerformAndTransmit("Done"); }
		public boolean autoDoneActive() { return autoDone && !skipAutoDone && !reviewMode(); }
		public void doGameTest() { }
		public boolean reverseView() { BoardProtocol b = getBoard(); return (b==null ? false : b.reverseView()); }
		static public boolean autoOptimize = true;
		public void setLocalBoundsSync(int x,int y,int w,int h)
		{	super.setLocalBoundsSync(x,y,w,h);
			if(autoOptimize) { selectedLayout.optimize(); }
		}
		public int getLastPlacement(boolean empty) {
			throw G.Error("Must be overriden");
		}
		public void handleError(String msg,String context,Throwable err)
		{	Utf8OutputStream bs = new Utf8OutputStream();
			PrintStream os = Utf8Printer.getPrinter(bs);
			if(context!=null)
			{
				os.print(" cxt: ");
				os.print(context);
			}
			printDebugInfo(os);
			os.flush();
			logError(msg,bs.toString(), err);
		}
		VNCTransmitter transmitter = null;
		private JMenuItem setNetConsole = null;
		public void setTransmitter(VNCTransmitter m) { transmitter = m; }

		/**
		 * Handle the "offer draw" "accept draw" "decline draw" buttons in a uniform way
		 *  
		 * @param gc
		 * @param rotation 	 	rotation to present the button at (can flip for 2p seating across)
		 * @param role		 	the role of the current state, ie, getState().getRole()
		 * @param drawLikely	true if a draw is "likely" so should be offered in the UI
		 * @param select		the hit point
		 * @param acceptRect	rectangle for accept/offer
		 * @param declineRect	rectangle for decline
		 * @param highlightColor button foreground color
		 * @param backgroundColor button background color
		 */
	    public void handleDrawUi(Graphics gc,double rotation,StateRole role,
	    		boolean drawLikely,HitPoint select,
	    		Rectangle acceptRect,Rectangle declineRect,
	    		Color highlightColor,Color backgroundColor)
	    {	handleDrawUi(gc,rotation,role,drawLikely,select,
	    		acceptRect,acceptRect,declineRect,highlightColor,backgroundColor);
	    }
	    
		/**
		 * Handle the "offer draw" "accept draw" "decline draw" buttons in a uniform way
		 *  
		 * @param gc
		 * @param rotation 	 	rotation to present the button at (can flip for 2p seating across)
		 * @param role		 	the role of the current state, ie, getState().getRole()
		 * @param drawLikely	true if a draw is "likely" so should be offered in the UI
		 * @param select		the hit point
		 * @oaram offerRect		rectangle for offer draw
		 * @param acceptRect	rectangle for accept
		 * @param declineRect	rectangle for decline
		 * @param highlightColor button foreground color
		 * @param backgroundColor button background color
		 */
	    public void handleDrawUi(Graphics gc,double rotation,StateRole role,
	    		boolean drawLikely,HitPoint select,
	    		Rectangle offerDrawRect,Rectangle acceptRect,Rectangle declineRect,
	    		Color highlightColor,Color backgroundColor)
	    {
	        switch(role)
	        {
	        default:
	        case Other: 
	        	break;
	        case Play:
	        	if(!drawLikely) { break; }
	        case DrawPending:
	        	{	// if not making progress, put the draw option on the UI
	            	if(GC.handleSquareButton(gc,rotation,offerDrawRect,select,s.get(OFFERDRAW),
	            			highlightColor,
	            			role==StateRole.DrawPending?highlightColor : backgroundColor))
	            	{
	            		select.hitCode = GameId.HitOfferDrawButton;
	            	}

	        	}
	        	break;
	        case AcceptOrDecline:
	        case AcceptPending:
	        case DeclinePending:
	        	if(GC.handleSquareButton(gc,rotation,acceptRect,select,s.get(ACCEPTDRAW),highlightColor,role==StateRole.AcceptPending?highlightColor:backgroundColor))
	        	{
	        		select.hitCode = GameId.HitAcceptDrawButton;
	        	}
	        	if(GC.handleSquareButton(gc,rotation,declineRect,select,s.get(DECLINEDRAW),highlightColor,role==StateRole.DeclinePending?highlightColor:backgroundColor))
	        	{
	        		select.hitCode = GameId.HitDeclineDrawButton;
	        	}
	       	break;
	        }
	    }
		/**
		 * place a row of rectangles
		 * 
		 * @param stateX
		 * @param stateY
		 * @param stateW
		 * @param stateH
		 * @param rects
		 */
		public void placeRow(int stateX,int stateY,int stateW,int stateH,Rectangle... rects)
		{
			if(rects[0]==goalRect)
			{	int xw = stateH+stateH/4;
				stateW -= xw;
				G.SetRect(rulesRect,stateX+stateW,stateY,stateH,stateH);
			}
			int x = stateX+stateW-stateH/4;
			for(int lim=rects.length-1; lim>0; lim--)
			{
				x -= stateH+stateH/2;
				G.SetRect(rects[lim],x,stateY,stateH,stateH);
			}
			G.SetRect(rects[0], stateX, stateY, x-stateX-stateH/2,stateH);
		}
		/**
		 * place a "stateRect" row with an icon at space at the left and a list of other rectangles
		 * chipped off at the right.
		 * @param l
		 * @param t
		 * @param w
		 * @param h
		 * @param icon
		 * @param r
		 */
		public void placeStateRow(int l,int t,int w,int h,Rectangle icon,Rectangle... r)
		{
			G.SetRect(icon, l, t, h, h);
			placeRow(l+h+h/2,t,w-h-h/2,h,r);
		}
		public boolean passIsPossible() { return false; }
		/** return true of the rest of the logic supports offerdraw/declinedraw */
		public boolean drawIsPossible() { return false; }
		public boolean canOfferDraw() { return false; }

}