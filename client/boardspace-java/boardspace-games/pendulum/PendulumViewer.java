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
package pendulum;

import common.GameInfo;

import static pendulum.PendulumMovespec.*;

import java.awt.*;

import online.common.*;
import java.util.*;
import bridge.Config;
import lib.Graphics;
import lib.AR;
import lib.Random;
import lib.CellId;
import lib.Drawable;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.ImageStack;
import lib.StockArt;
import lib.TextButton;
import lib.Toggle;
import lib.Tokenizer;
import lib.Image;
import lib.LFrameProtocol;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

/**
 * TODO: sounds for start of council phase, etc.
 * TODO: add a "no money dummy" to the meeple tracks
 * This is intended to be maintained as the reference example how to interface to boardspace.
 * <p>
 * The overall structure here is a collection of classes specific to Hex, which extend
 * or use supporting online.game.* classes shared with the rest of the site.  The top level 
 * class is a Canvas which implements ViewerProtocol, which is created by the game manager.  
 * The game manager has very limited communication with this viewer class, but manages
 * all the error handling, communication, scoring, and general chatter necessary to make
 * the game part of the site.
 * <p>
 * The main classes are:
 * <br>GameViewer - this class, a canvas for display and mouse handling
 * <br>GameBoard - board representation and implementation of the game logic
 * <br>GameMovespec - representation, parsing and printing of move specifiers
 * <br>GamePlay - a robot to play the game
 * <br>GameConstants - static constants shared by all of the above.  
 *  <p>
 *  The primary purpose of the PushfightViewer class is to do the actual
 *  drawing and to mediate the mouse gestures.  All the actual work is 
 *  done in an event loop, rather than in direct response to mouse or
 *  window events, so there is only one process involved.  With a single 
 *  process, there are no worries about synchronization among processes
 *  of lack of synchronization - both major causes of flakey user interfaces.
 *  <p>
 *  The actual mouse handling is done by the commonCanvas class, which simply 
 *  records the recent mouse activity, and triggers "MouseMotion" to be called
 *  while the main loop is executing.
 *  <p>
 *  Similarly, the actual "update" and "paint" methods for the canvas are handled
 *  by commonCanvas, which merely notes that a paint is needed and returns immediately.
 *  drawCanvas is called in the event loop.
 *  <p>
 *  The drawing methods here combine mouse handling and drawing in a slightly
 *  nonstandard way.  Most of the drawing routines also accept a "HitPoint" object
 *  which contains the coordinates of the mouse.   As objects are drawn, we notice
 *  if the current object contains the mouse point, and if so deposit a code for 
 *  the current object in the HitPoint.  the Graphics object for drawing can be null,
 *  in which case no drawing is actually done, but the mouse sensitivity is checked
 *  anyway.  This method of combining drawing with mouse sensitivity helps keep the
 *  mouse sensitivity accurate, because it is always in agreement with what is being
 *  drawn.
 *  <p>
 *  Steps to clone this hierarchy to start the next game
 *  <li> use eclipse refactor to rename the package and individual files
 *  <li> duplicate the game start configuration, making a new one for the new game
 *  <li> launch the new game and get it to start, still identical to the old pushfight in all but name.
 *  	this will probably require a few edits to the init code.
 *  <li> do a cvs update on the original pushfight hierarchy to get back the original code.
 *  
*/
public class PendulumViewer extends CCanvas<PendulumCell,PendulumBoard> implements PendulumConstants
{		// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
	
    static final String Pendulum_SGF = "pendulum"; // sgf game name
   // file names for jpeg images and masks
    static final String ImageDir = G.isCodename1()
    		? "/appdata/pendulum/images/" 
    		: "/pendulum/images/";

	static final String flipSound = ImageDir + "flip" + Config.SoundFormat;
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackgroundColor = new Color(240,240,240);
    private Color rackBackGroundColor = Color.lightGray;
    private Color rackIdleColor = Color.lightGray;
    private Color boardBackgroundColor = Color.lightGray;
         
    // private state
    private PendulumBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //public Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //public Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle chatRect = addRect("chatRect");
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    // also doneRect goalRect progressRect editRect doneRects logRect
    //
    // addZoneRect also sets the rectangle as specifically known to the 
    // mouse tracker.  The zones are considered in the order that they are
    // added, so the smaller ones should be first, then any catchall.
    //
    // zones ought to be mostly irrelevant if there is only one board layout.
    //
    static {
    	if(G.debug()) { PendulumConstants.putStrings(); }
    }
    private Toggle eyeRect = new Toggle(this,"eye",
 			StockArt.NoEye,PendulumId.ToggleEye,NoeyeExplanation,
 			StockArt.Eye,PendulumId.ToggleEye,EyeExplanation
 			);
    private Rectangle chipRects[] = addZoneRect("chip",MAX_PLAYERS);
    private Rectangle belowBoardRects[] = addZoneRect("below",MAX_PLAYERS);
    
    private Rectangle boardRects[] = addZoneRect("playerboard",MAX_PLAYERS);
    
 	private TextButton doneButton = addButton(DoneAction,GameId.HitDoneButton,ExplainDone,
			HighlightColor, rackBackGroundColor,rackIdleColor);
 	private TextButton councilButton = addButton(
 			UnrestPlayMessage,PendulumId.Rest,ExplainUnRestMessage,
 			StartCouncilMessage,PendulumId.StartCouncil,ExplainStartCouncilMessage,
			HighlightColor, rackBackGroundColor,rackIdleColor);
	private TextButton playButton = addButton(StartPlayMessage,PendulumId.StartPlay,ExplainStartCouncilMessage,
			HighlightColor, rackBackGroundColor,rackIdleColor);
	private TextButton restButton = addButton(
			UnrestPlayMessage,PendulumId.Rest,ExplainUnRestMessage,
			RestPlayMessage,PendulumId.Rest,ExplainRestMessage,
			HighlightColor, rackBackGroundColor,rackIdleColor);
	private TextButton pauseButton = addButton(
			ResumePlayMessage,PendulumId.Pause,ExplainResumeMessage,
			PausePlayMessage,PendulumId.Pause,ExplainPauseMessage,
			HighlightColor, rackBackGroundColor,rackIdleColor);

	private TextButton legendaryButton = addButton(TakeLegendaryMessage,PendulumId.TakeLegendary,PendulumId.TakeLegendary.description,
			HighlightColor, rackBackGroundColor,rackIdleColor);
	private TextButton standardButton = addButton(TakeStandardMessage,PendulumId.TakeStandard,PendulumId.TakeStandard.description,
			HighlightColor, rackBackGroundColor,rackIdleColor);
	private TextButton testButton = addButton(ResumeMovesMessage,PendulumId.TestPrivilege,"",
			PauseMovesMessage,PendulumId.TestPrivilege,PendulumId.TestPrivilege.description,
			HighlightColor, rackBackGroundColor,rackIdleColor);

	private Rectangle councilRect = addRect("council");
 	private Rectangle timerRect = addRect("timer");
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	PendulumChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = PendulumChip.Icon.image;
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	int players_in_game = info.getInt(OnlineConstants.PLAYERS_IN_GAME,MAX_PLAYERS);
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        //
        // not suitable for games with any optional "done" states.  If true, autodone
        // is controlled by an option menu option.  Also conditionalize showing the
        // "done" button with autoDoneActive()
        enableAutoDone = true;
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default
        
        addZoneRect("done",doneButton);	// this makes the "done" button a zone of its own
 
        String type = info.getString(GameInfo.GAMETYPE, PendulumVariation.pendulum.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new PendulumBoard(type,players_in_game,randomKey,getStartingColorMap(),PendulumBoard.REVISION);
        //
        // this gets the best results on android, but requires some extra care in
        // the user interface and in the board's copyBoard operation.
        // in the user interface.
        useDirectDrawing(true);
        doInit(false);
        adjustPlayers(players_in_game);
    }
    public void adjustPlayers(int n)
    {
    	super.adjustPlayers(n);
    	guiPlayer = Math.min(n-1,guiPlayer);
    }
    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        bb.doInit(bb.gametype);						// initialize the board
        if(!preserve_history)
    	{ 
        	// the color determines the first player
        	startFirstPlayer();
        	//
        	// alternative where the first player is just chosen
        	//startFirstPlayer();
 	 		//PerformAndTransmit(reviewOnly?"Edit":"Start P"+first, false,replayMode.Replay);

    	}
    }
    /** this is called by the game controller when all players have connected
     * and the first player is about to be allowed to make his first move. This
     * may be a new game, or a game being restored, or a player rejoining a game.
     * You can override or encapsulate this method.
     */
    public void startPlaying()
    {	super.startPlaying();
    	guiPlayer = getActivePlayer().boardIndex;
		setGameRecorder();
    }
    
	/**
	 * 
	 * this is a debugging hack to give you an event based on clicking in the player name
	 * You can take whatever action you like, or no action.
	 */
    //public boolean inPlayRect(int eventX, int eventY)
    //{	return(super.inPlayRect(eventX,eventY));
    //}

    /**
     * update the players clocks.  The normal thing is to tick the clocks
     * only for the player whose turn it is.  Games with a simtaneous action
     * phase need to do something more complicated.
     * @param inc the increment (in milliseconds) to add
     * @param p the current player, normally the player to update.
     */
    //public void updatePlayerTime(long inc,commonPlayer p)
    //{
    //	super.updatePlayerTime(inc,p);
    //}
	/**
	 * 
	 * this is a debugging hack to give you an event based on clicking in the time
	 * clock.  You can take whatever action you like, or no action.
	 * */
    //public boolean inTimeRect(int eventX, int eventY)
    //{
    //    boolean val = super.inTimeRect(eventX, eventY);
    //    return (val);
    //}
    public int getOurMovingObject(HitPoint highlight)
    {
        if (OurMove())
        {
            return (simultaneousTurnsAllowed() 
            		? bb.movingObjectIndex(reviewOnly ? guiPlayer : getActivePlayer().boardIndex) 
            		: bb.movingObjectIndex());
        }
       
        return (NothingMoving);
    }
  
	/**
	 * this is the main method to do layout of the board and other widgets.  I don't
	 * use swing or any other standard widget kit, or any of the standard layout managers.
	 * they just don't have the flexibility to produce the results I want.  Your mileage
	 * may vary, and of course you're free to use whatever layout and drawing methods you
	 * want to.  However, I do strongly encourage making a UI that is resizable within
	 * reasonable limits, and which has the main "board" object at the left.
	 * <p>
	 *  The basic layout technique used here is to start with a cell which is about the size
	 *  of a board square, and lay out all the other objects relative to the board or to one
	 *  another.  The rectangles don't all have to be on grid points, and don't have to
	 *  be non-overlapping, just so long as the result generally looks good.
	 *  <p>
	 *  When "extraactions" is available, a menu option "show rectangles" works
	 *  with the "addRect" mechanism to help visualize the layout.
	 */ 


    public void setLocalBounds(int x, int y, int width, int height)
    {	
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*10;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*3,	// minimum cell size
    			fh*5,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	// its generally recommended for the vcr to be no wider than the game log,
       	// this helps the vcr to tuck into the spare space allocated for the log
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	// games which have a private "done" button for each player don't need a public
    	// done button, and also we can make the edit/undo button square so it can rotate
    	// to face the player.  Placing the done/edit after the vcr sometimes gives better
    	// results, because it can be placed horizontally or vertically
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneButton,editRect);
       	layout.placeRectangle(testButton,buttonW,buttonW/3,BoxAlignment.Edge);
       	G.copy(councilButton,doneButton);
       	G.copy(playButton,doneButton);
       	//layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// There are two classes of boards that should be rotated. For boards with a strong
    	// "my side" orientation, such as chess, use seatingFaceToFaceRotated() as
    	// the test.  For boards that are noticably rectangular, such as Push Fight,
    	// use mainW<mainH
        int nrows = 24;  // b.boardRows
        int ncols = 24;	 // b.boardColumns
  	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        int stateH = fh*5/2;
        placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,eyeRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	G.SetRect(councilRect,(int)(boardX+0.19*boardW),(int)(boardY+0.73*boardH),(int)(0.30*boardW),(int)(0.22*boardH));
    	G.SetRect(timerRect,(int)(boardX+0.37*boardW),(int)(boardY+0.43*boardH),(int)(0.13*boardW),(int)(0.3*boardH));
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW,stateH,goalRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        
        bb.setLocations();
        bb.SetDisplayRectangle(boardRect);
       	int rx = bb.cellToX(bb.restButton);
       	int ry = bb.cellToY(bb.restButton);
       	int bw = buttonW*6/5;
       	G.SetRect(restButton,rx-bw/2,ry-bw/15,bw,bw/4);
       	G.copy(pauseButton,restButton);
       	G.SetTop(pauseButton,(int)(ry-boardH*0.23));
       	G.copy(councilButton,restButton);
       	G.copy(playButton,restButton);
       
       	zoomer.reCenter();

    }
    /**
     * create all per-player boxes.  Nothing is required, but the standard methods
     * create a player name, clocks, and a box for an avatar.  Standard practice
     * is to include a private "done" box if using a planned seating chart.
     * 
     * The layout manager tries many values for "unitsize" so effectively the rest of
     * the boxes should use it as a standard unit.
     */
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle board = boardRects[player];
    	Rectangle below = belowBoardRects[player];
    	G.SetRect(chip,	x,	y,	2*unitsize,	2*unitsize);
    	Rectangle box =  pl.createRectangularPictureGroup(x+2*unitsize,y,2*unitsize/3);
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*3 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.SetRect(board,x,G.Bottom(box),unitsize*11,unitsize*7);
    	G.SetRect(below,x,G.Bottom(board),unitsize*11,unitsize*2);
    	G.union(box, done,chip,board,below);
    	pl.displayRotation = rotation;
    	return(box);
    }
    private double[] textIconScale = new double[] { 1,1,-0.2,-0.2};
    public Drawable getPlayerIcon(int p)
    {	playerTextIconScale = textIconScale;
    	return p>=0 ? bb.getPlayerChip(p) : StockArt.Dot;
    }
	// draw a box of spare chips. For pushfight it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void drawPlayerBoard(Graphics gc, Rectangle r, Rectangle br,commonPlayer pl,
    		HitPoint highlight,PendulumBoard gb,Hashtable<PendulumCell,PendulumMovespec>targets,HitPoint any)
    {	int player = pl.boardIndex;
    
    	if(gb.simultaneousTurnsAllowed()
    			&& HitPoint.setHelpText(highlight,pl.nameRect,"use UI for this player"))
    	{
    		highlight.hitCode = PendulumId.Select;
    		highlight.hit_index = player;
    		highlight.hitData = pl;
    		highlight.spriteRect = pl.nameRect;
    		highlight.spriteColor = Color.red;
    	}
   	
    	PlayerBoard pb = gb.getPlayerBoard(player);
    	pb.setDisplayRectangle(br);
    	//pb.setLocations();
    	PendulumCell tucked[] = pb.tucked;
    	for(PendulumCell c : tucked)
    	{
    		 drawStack(gc,gb,c,highlight,targets,any);
    	}
    	gb.getPlayerChip(player).drawChip(gc,this,r,null);
 
    	pb.mat.drawChip(gc,this,br,null);
        
        for(PendulumCell c = pb.allCells; c!=null; c=c.next)
        {	if(AR.indexOf(tucked,c)<0)
        	{
            drawStack(gc,gb,c,highlight,targets,any);
        	}
        }
        zoomer.drawMagnifier(gc,any,pl.playerBox,0.07,0.98,0.78,G.rotationQuarterTurns(pl.displayRotation));
     }
    /**
     * return the dynamically adjusted size during an animation.  This allows
     * compensation for things like the zoom level of the board changing after
     * the animation is started.
     */
    //public int activeAnimationSize(Drawable chip,int thissize) 	{ 	 	return(thissize); 	}

    /**
    * sprites are normally a game piece that is "in the air" being moved
    * around.  This is called when dragging your own pieces, and also when
    * presenting the motion of your opponent's pieces, and also during replay
    * when a piece is picked up and not yet placed.  While "obj" is nominally
    * a game piece, it is really whatever is associated with b.movingObject()
    
      */
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
    	PendulumChip chip = PendulumChip.getChip(obj);
    	chip.drawChip(g,this,CELLSIZE*5/2, xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  


    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { 
     PendulumChip.backgroundTile.image.tileImage(gc, fullRect);   
      drawFixedBoard(gc);
     }
    
    Image scaledBoard = null;
    
    // land here after rotating the board drawing context if appropriate
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	// note, drawing using disB is very important for boards which have different sizes
    	// and which are using copyies of the real board for drawing.
    	// Not so important for boards which are always the same dimensions, but it's always safe.
    	PendulumBoard gb = disB(gc);
        boolean reviewBackground = reviewMode()&&!mutable_game_record;
        if(reviewBackground)
        {	 
         PendulumChip.backgroundReviewTile.image.tileImage(gc,brect);   
        }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(gb,brect);

	  	PendulumChip board = nPlayers()<=3 ? PendulumChip.Board3 : PendulumChip.Board5;
	  	
	  	// if the board is one large graphic, for which the visual target points
	  	// are carefully matched with the abstract grid
	  	scaledBoard = board.getImage().centerScaledImage(gc, brect,scaledBoard);
	  	
	  	PendulumChip.councilBoard.getImage().centerImage(gc,councilRect);
	  	if(!gb.variation.timers)
	  	{
	  	PendulumChip.timerTrack.getImage().centerImage(gc,timerRect);
	  	}
     	
    }
    
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, PendulumBoard gb, Rectangle brect, HitPoint highlight,Hashtable<PendulumCell,PendulumMovespec> targets,HitPoint any)
    {	
  
    	//gb.setLocations();
    	UIState uis = getUIState(gb);
    	UIState uioverlay = uis;
    	switch(uis)
    	{
    	// reduce uis to those that need an overlay
    	case AchievementOrLegandary: break;
    	default: uioverlay = UIState.Normal;
    	}
    	HitPoint normalHighlight = highlight;
    	
    	if(bigChip!=null || uioverlay!=UIState.Normal) { normalHighlight=null; }

    	for(PendulumCell cell = gb.allCells; cell!=null; cell=cell.next)
          {
            drawStack(gc,gb,cell,normalHighlight,targets,any);

        }
    	PendulumCell cc = gb.councilCards[0];
    	int cx = gb.cellToX(cc);
    	int cy = gb.cellToY(cc);
    	int cs = gb.cellSize(cc);
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,false,cx-cs,(int)(cy-cs*1.4),cs*2,cs/3,Color.black,null,
    			s.get(RoundMessage,gb.round()));
    	
     	
    	switch(uioverlay)
    	{
    	default: throw G.Error("Not expecting %s",uioverlay);
    	case Normal: break;
    	case AchievementOrLegandary:
    		showAchievementUI(gc,gb,brect,highlight);
    		break;
    	}
    	if(bigChip!=null)
    	{	int siz = Math.min(300,G.Width(brect)/2);
    		int xcx = G.centerX(brect);
    		int xcy = G.centerY(brect);
    		String tip = getTranslatedToolTip(bigChip);
    		bigChip.drawChip(gc,this,siz,xcx,xcy,any,PendulumId.ShowCard,tip,1,1);
    		StockArt.FancyCloseBox.drawChip(gc,this,siz/6,(int)(xcx+siz*0.33),(int)(xcy-siz*0.55),any,PendulumId.ShowCard,null);
    	}
    }
    private void showAchievementUI(Graphics gc, PendulumBoard gb, Rectangle brect, HitPoint highlight)
    {
    	int xpos = gb.cellToX(gb.currentAchievement);
    	int ypos = gb.cellToY(gb.currentAchievement);
    	int sz = gb.cellSize(gb.currentAchievement);
    	int xp = xpos-sz*2;
    	int yp = ypos+sz/2;
    	StockArt.Scrim.getImage().drawImage(gc,xp,yp,sz*4,sz);
    	GC.frameRect(gc,Color.black,xp,yp,sz*4,sz);
    	PendulumChip.legendary.drawChip(gc,this,sz*2,xpos-sz,ypos+sz,null);
    	gb.currentAchievement.chipAtIndex(0).drawChip(gc,this,sz*2/3,xpos+sz,ypos+sz,null);
    	GC.setFont(gc,largeBoldFont());
       	G.SetRect(legendaryButton,xp+sz/8,yp+sz/4,sz*2,sz/3);
       	G.SetRect(standardButton,xp+sz/4+sz*2,yp+sz/4,sz*3/2,sz/3);
       	legendaryButton.show(gc,highlight);
       	standardButton.show(gc,highlight);
    }
    private String timerText(PendulumBoard gb,PendulumCell c,Timer t)
    {
    	PendulumChip top = c.topChip();
    	if(top!=null && top!=PendulumChip.purpleGlass)
    	{
    		String msg = t.getText(gb.board_state);
    		return msg;
    	}
    	return null;
    }
    
    public String getTranslatedToolTip(PendulumChip top)
    {
    	if(top!=null)
		{
		PC cost = top.pc;
		String costStr = s.get(cost.description);
		PB benes[] = top.pb;
		if(benes!=null)
		{	
			if(benes.length==4)
			{	
				for(int i=0;i<4;i++)
				{
					costStr += " \n" + s.get(ColonyColors[i]) + " : " + s.get(benes[i].description);
				}
			}
			else
			{
				costStr += "\n" + s.get(benes[0].description);
			}
		}
		return costStr;
		}
    	return null;
    }
    public String getToolTip(PendulumCell c)
    {	PendulumId rack = c.rackLocation();
    	String msg = rack.description;
    	int ind = c.height();
    	switch(rack)
    	{
        case RewardCard:
    	case AchievementCard:
    		{
    		PendulumChip top = c.chipAtIndex(0);
    		return getTranslatedToolTip(top);
    		}
    	case PlayerPopularityVP:
    	case PlayerLegendary:
    	case PlayerMax3Cards:
    	case PlayerPrestigeVP:
    	case PlayerPowerVP: ind = c.row; break;
    	case Privilege: ind = c.row+1; break;
    	default: break;
    	}
    	return s.get0or1(msg,ind);
    }
     
    public boolean drawStack(Graphics gc,PendulumBoard gb,PendulumCell cell,HitPoint highlight,Hashtable<PendulumCell,PendulumMovespec> targets,HitPoint any)
    {    
        String back = null;
        String cost = cell.cost.description;
        String bene = cell.benefit.description;
        String tip = (cell.cost==BC.None && cell.benefit==BB.None)
        				? getToolTip(cell)
        				: "".equals(cost) ? s.get(bene) : "".equals(bene) ? s.get(cost) : s.get(cost)+"\n"+s.get(bene);
        double xstep = 0.005;
        double ystep = 0.005;
    	int ypos0 = gb.cellToY(cell);
    	int xpos0 = gb.cellToX(cell);
    	int siz0 = gb.cellSize(cell);
    	int xpos = xpos0;
    	int ypos = ypos0;
    	int siz = siz0;
    	boolean hitTop = false;
    	boolean canHit = gb.legalToHitBoard(gb.getCell(cell),targets);
    	PendulumState state = gb.getState();
        labelFont = largeBoldFont();
        boolean isSelected = gb.isSelected(cell);
        boolean helpPerCard = false;
        boolean colorMatch = false;
        boolean benefitsPosition = false;
        boolean selectPerCard = false;
        PendulumId rack = cell.rackLocation();
        switch(rack)
        {
        case PlayerRefill:
        	break;
        case TimerTrack:
        	if(gb.variation.timers) { return false; }
        	break;
        case PlayerPlayedStratCard:
        	xstep = 0.2;
         	helpPerCard = true;
        	break;
        case PlayerStratCard:
        	xstep = 0.7;
        	ystep = 0;
        	selectPerCard = true;	// not all cards are playable
        	helpPerCard = true;
        	break;
        case Trash:
        	xstep = 0.3;
        	ystep = 0;
        	break;
        case PlayerBrownBenefits:
        case PlayerYellowBenefits:
        case PlayerRedBenefits:
        case PlayerBlueBenefits:
        	ystep = 0.25;
        	xstep = 0.03;
        	ypos += (int)(siz*ystep*(cell.height())); 
        	benefitsPosition = true;
        	hitTop = true;
        	break;
        case AchievementCard:
        	// note that there's a special case in PendulumCell stackXAdjust to modify this
         	xstep = 0.25;
        	ystep = 0.0;
        	helpPerCard = true;
        	break;
        case BlackActionA:
        case BlackActionB:
        case BlackMeepleA:
        case BlackMeepleB:
        case GreenActionA:
        case GreenActionB:
        case GreenMeepleA:
        case GreenMeepleB:
        case PurpleActionA:
        case PurpleActionB:
        case PurpleMeepleA:
        case PurpleMeepleB:
        	colorMatch = state!=PendulumState.Puzzle;
           	xstep = Math.min(0.33,1.0/(cell.height()+1));
        	ystep = 0.0;
        	xpos -= ((cell.height()/2)*siz)*xstep;
        	break;
		case Rest:
        	xstep = 0.33;
        	ystep = 0.0;
        	xpos -= ((cell.height()/2)*siz)*xstep;
        	break;
	
        case RewardDeck:
        	tip = s.get(CardCountMessage,cell.height()); 
			//$FALL-THROUGH$
		case ProvinceCardStack:
        case AchievementCardStack:
	        hitTop = true;
        	back = PendulumChip.BACK;
        	break;
        case PlayerGrandeReserves:
        case PlayerMeepleReserves:
        case PlayerGrandes:
        case PlayerMeeples:
        	xstep = 0.3;
        	ystep = 0;
        	break;
        case Privilege:
        	{	
        	if(cell.topChip()!=null)
        	{
        	PendulumState res = gb.resetState;
        	if(res!=null)
        	{
        	PlayerBoard pb = gb.getPlayerBoard(cell.topChip());
        	switch(res)
	        	{
	        	case CouncilRewards: 
	        		back = ""+ (pb==null ? 3 : pb.councilVotes.height());
	        		labelFont = standardPlainFont();
	        		break;
	        	case Play: 
	        	case CouncilPlay:
	        		back = ""+(pb==null ? 3 : pb.votes.height()); 
	        		labelFont = standardPlainFont();
	        		break;
	        	default: break;
	        	}
        	}}}
        	break;
        case GreenTimer:
        	// note this is using the real board timer deliberately
        	back = timerText(gb,cell,bb.greenTimer);
        	break;
        case PurpleTimer:
          	// note this is using the real board timer deliberately
       	   	back = timerText(gb,cell,bb.purpleTimer);
        	break;
        case PlayerVotes:
        	xstep = 0.2;
        	ystep = 0.0;
        	xpos -= (cell.height()/2.0*siz)*xstep;
        	break;
       	
        case PlayerMilitaryReserves:
        case PlayerCultureReserves:
        case PlayerCashReserves:
        case PlayerVotesReserves:
        	xstep= 0.08;
        	ystep = 0.0;
        	break;
        case PlayerMilitary:
        case PlayerCulture:
        case PlayerCash:
        	xstep = 0.1;
        	ystep = 0.1;
        	xpos -= (cell.height()/2.0*siz)*xstep;
        	ypos += (cell.height()/2.0*siz)*ystep;
        	break;
        case RewardCard:
        case Province:
        	helpPerCard = true;
        	break;
        case BlackTimer:
        	// note this is using the real board timer deliberately
        	back = timerText(gb,cell,bb.blackTimer);
        	break;
        default: break;
        }
        CellId preCode = (any!=null ? any.hitCode : null);
        Object preobj = (any!=null ? any.hitObject : null);
        int preind = any.hit_index;
        int prewid = any.hit_width;
        int prehei = any.hit_height;
        int prex = any.hit_x;
        int prey = any.hit_y;
        // this uses a slightly nonstandard selection method to get the selection for
        // any item, even if it can't be selected according to the "canhit" and "hitpoint"
        // logic.  If canhit+hitpoint wouldn't have allowed selection, it is unselected.
        // the benefit of this is we have a selection to use for tooltips and other logic,
        // in this case, tooltips for individual cards and a separate selection to view
        // the cards enlarged.
    	numberMenu.saveSequenceNumber(cell,xpos,ypos);
        boolean hit = cell.drawStack(gc,this,any,siz,xpos,ypos,0,xstep,ystep,back);
        if(hit)
        		{
        		boolean misMatch = highlight==null || !canHit;	// not really selectable
        		
        		int hitIndex = hitTop ? -1 : any.hit_index;
        		int hx =any.hit_x;
        		int hy =any.hit_y;
        		if(benefitsPosition)
        		{
        			hy += siz/4;
        		}
        		//int hw =any.hit_width;
        		//int hh =any.hit_height;
        		if(colorMatch)
        		{	// another case of un-selectable is picking up a meeple from a stack
        			// that isn't one of your meeples.
        			PlayerBoard pb = gb.getPlayerBoard(reviewOnly ? guiPlayer : getActivePlayer().boardIndex);
        			PendulumChip ch = hitIndex>=0 ? cell.chipAtIndex(hitIndex) : cell.topChip();
        			PendulumMovespec m = targets.get(cell);
        			misMatch |= ch!=null && m!=null && m.op==MOVE_PICK && ch.color!=pb.color;
        		}
        		if(selectPerCard)
        		{	// strat cards are not all selectable
        			PlayerBoard pb = gb.getPlayerBoard(cell);
        			PendulumChip ch = cell.chipAtIndex(hitIndex);
        			if(ch!=null && !pb.canPlayStratCard(ch)) { misMatch = true; }
        			
        		}
        		if(misMatch)
	        		{
        			hit = false;
	        		any.hitCode = preCode;	// undo the selection/hit on this cell
	        		any.hitObject = preobj;
	        		any.hit_x = prex;
	        		any.hit_y = prey;
	        		any.hit_index = preind;
	        		any.hit_width = prewid;
	        		any.hit_height = prehei;
	        		}
	        		else
	        		{	if(hitTop) { any.hit_index = cell.height()-1; }
	        			any.spriteColor = Color.red;
	        			any.spriteRect = new Rectangle(hx-any.hit_width/2,hy-any.hit_height/2,any.hit_width,any.hit_height);
	        			any.hitData = targets.get(gb.getCell(cell));
	        		}
        		
        		if(helpPerCard)
        		{
        		// generate tool tips to describe particular cards
         		int index = (cell.rackLocation()==PendulumId.AchievementCard) ? 0 : hitIndex;
        		int size = siz;
        		int h = size*4/3;
        		PendulumChip ch = cell.chipAtIndex(index);
        		if(ch!=null)
        		{
        		tip = getTranslatedToolTip(ch);	
        		Rectangle r = new Rectangle(hx-size/2,hy-size*2/3,size,h);
        		//GC.frameRect(gc,Color.green,r);
        		HitPoint.setHelpText(any,r,tip);
        		G.SetHeight(r,h/4);
        		boolean province = ch.id==PendulumId.ProvinceCard;     		
        		G.SetTop(r,G.Top(r)+(province? h*2/3 : h*3/4));
        		// hit the bottom 1/4 of cards to pop up an enlarged version
           		if(G.pointInRect(any,r))
        			{ any.spriteRect = r;
        			  any.spriteColor = Color.red;
        			  any.hitCode = PendulumId.ShowCard;
        			  any.hitData = ch;
        			}
        		tip = null;
        		}
        		}
        		}

        if(tip!=null && !"".equals(tip))
        {
        	HitPoint.setHelpText(any,siz/2,xpos,ypos,tip);
        }
        
        // decorate some cells with the stack height
        int xp = xpos0;
        int yp = ypos0;
        int sz = siz0;
        boolean draw = true;
        switch(rack)
        {
        case PlayerVotes:
        	xp += sz/3;
        	yp += sz/4;
        	break;
        case PlayerMilitary:
        case PlayerCulture:
        case PlayerCash:
        	xp += sz/5;
        	yp += (int)(sz*0.4);
        	break;
        	
        default: draw=false;
        }
        if(draw)
        	{	int h = cell.height();
        		//if(h>0)
        		{
        		GC.setFont(gc,largeBoldFont());
        		GC.setColor(gc,Color.darkGray);
        		GC.Text(gc,""+h,xp,yp);
        		}
        	}

        //StockArt.SmallO.drawChip(gc,this,siz,xpos,ypos,null);
        if(eyeRect.isOnNow() && targets.get(cell)!=null)
        {	boolean mouse = eyeRect.isMouseOver();
        	StockArt.SmallO.drawChip(gc,this,siz*(mouse?2:1),xpos,ypos,null);
        }
        if(isSelected)
        {
        	StockArt.Checkmark.drawChip(gc,this,siz*2/3,xpos,ypos,null);
        }
        return hit;
    }
    //
    // this determines if the UI is allowed maximum flexibility to move any player's pieces
    //
    public boolean allQuiet(PendulumBoard gb)
    {
    	return (reviewOnly
    			&& gb.simultaneousTurnsAllowed()
    			&& gb.allQuiet());
    }
    private String effectiveStateMessage(PendulumBoard gb,PendulumState state,int ap)
    {
    	boolean sim = state.simultaneousTurnsAllowed();
    	PlayerBoard pb = gb.getPlayerBoard(sim ? ap : gb.whoseTurn);
    	int count = 0;
    	switch(state)
    	{
    	case CouncilTrim: 
    		count = pb.provinceCardLimit();
    		break;
    	default: break;
    	}
    	String msg = s.get0or1(state.description(),count);
    	UIState ui = pb.uiState;
    	if(ui!=UIState.Normal && state!=PendulumState.Confirm) {
    			msg = s.get0or1(ui.description,pb.uiCount);
    		}
    
    	return msg;
    }
    
    int guiPlayer = 0;
    public UIState getUIState(PendulumBoard gb)
    {	boolean sim = gb.getState().simultaneousTurnsAllowed();
    	PlayerBoard pb = gb.getPlayerBoard(sim ? guiPlayer : gb.whoseTurn);
    	return pb.uiState;
    }
    public void drawPlayerStuff(Graphics gc,PendulumBoard gb,
    		HitPoint selectPos,HitPoint ourTurnSelect,HitPoint buttonSelect,
    		Hashtable<PendulumCell,PendulumMovespec> targets)
    {	int whoseTurn = gb.whoseTurn;
    	boolean planned = plannedSeating();
        for(int player=0;player<gb.players_in_game;player++)
       	{ commonPlayer pl = getPlayerOrTemp(player);
       	  pl.setRotatedContext(gc, selectPos,false);
    	   drawPlayerBoard(gc, chipRects[player],boardRects[player],pl, ourTurnSelect,gb,targets,selectPos);
    	   if(planned && whoseTurn==player)
    	   {
    		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
       	   pl.setRotatedContext(gc, selectPos,true);
       	}
    }
    /**
     * draw the main window and things on it.  
     * If gc!=null then actually draw, 
     * If selectPos is not null, then as you draw (or pretend to draw) notice if
     * you are drawing under the current position of the mouse, and if so if you could
     * click there to do something.  Care must be taken to consider if a click really
     * ought to be allowed, considering spectator status, use of the scroll controls,
     * if some board token is already actively moving, and if the game is active or over.
     * <p>
     * This dual purpose (draw, and notice mouse sensitive areas) tends to make the
     * code a little complicated, but it is the most reliable way to make sure the
     * mouse logic is in sync with the drawing logic.
     * <p>
    General GUI checklist
<p>
<li>vcr scroll section always tracks, scroll bar drags
<li>lift rect always works
<li>zoom rect always works
<li>drag board always works
<li>pieces can be picked or dragged
<li>moving pieces always track
<li>stray buttons are insensitive when dragging a piece
<li>stray buttons and pick/drop are inactive when not on turn
*/
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  
       // if direct drawing is in effect disB gets a copy of the board, which should be
       // used for everything called from here.  Also beware that the board structures
       // seen by the user interface are not the same ones as are seen by the execution engine.
       PendulumBoard gb = disB(gc);
       PendulumState state = gb.getState();
       int ap = (state==PendulumState.Puzzle)
    		   		? guiPlayer
    		   		: reviewOnly ? gb.whoseTurn : getActivePlayer().boardIndex;
       boolean moving = hasMovingObject(selectPos);
   	   if(gc!=null)
   		{
   		// note this gets called in the game loop as well as in the display loop
   		// and is pretty expensive, so we shouldn't do it in the mouse-only case
      
       setDisplayParameters(gb,boardRect);
   		}
       // 
       // if it is not our move, we can't click on the board or related supplies.
       // we accomplish this by suppressing the highlight pointer.
       //
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;
       //
       // even if we can normally select things, if we have already got a piece
       // moving, we don't want to hit some things, such as the vcr group
       //
       HitPoint buttonSelect = moving ? null : ourTurnSelect;
       // hit any time nothing is being moved, even if not our turn or we are a spectator
       HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;
       gameLog.playerIcons = true;
       gameLog.redrawGameLog2(gc, nonDragSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),standardBoldFont());

       // this does most of the work, but other functions also use contextRotation to rotate
       // animations and sprites.
       GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);
       Hashtable<PendulumCell,PendulumMovespec> targets = allQuiet(gb) 
    		   	? gb.getAllTargets() 
    		   	: gb.getTargets(ap);
       
       
       int whoseTurn = gb.whoseTurn;
       commonPlayer cpl = getPlayerOrTemp(whoseTurn);
       double messageRotation = cpl.messageRotation();
       boolean planned = plannedSeating();

       if (state != PendulumState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
    	    if(state.simultaneousTurnsAllowed())
    	    {	switch(state)
    	    	{
    	    	case PendingPlay:
    	    		playButton.show(gc,buttonSelect);
    	    		break;
    	    	case CouncilPlay:
    	    		councilButton.setIsOn(bb.getPlayerBoard(ap).uiState!=UIState.Normal);
    	    		councilButton.show(gc,buttonSelect); 
    	    		if(gb.variation.timers) { pauseButton.show(gc,buttonSelect); }
    	    		break;
    	    	case Play:
    	    		{
    	    		GC.setFont(gc,largeBoldFont());
    	    		if(gb.variation.timers)
	    			{
	    			pauseButton.show(gc,buttonSelect);
	    			}
    	    		if(gb.allResting())
    	    		{	
    	    			councilButton.show(gc,buttonSelect); 
    	    		}
    	    		else if(!gb.variation.timers)
    	    			{
    	    			restButton.setIsOn(bb.getPlayerBoard(ap).uiState!=UIState.Normal);
    	    			restButton.show(gc,buttonSelect); 
    	    			}
    	    		}
	    	    	break;
    	    	default:
    	    		break;
    	    	}
    	    
    	    }
    	    else if(!planned && !autoDoneActive())
				{
				doneButton.show(gc,messageRotation,gb.DoneState() ? buttonSelect : null);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
        }
       
       numberMenu.clearSequenceNumbers();
 
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,targets,selectPos);
       GC.unsetRotatedContext(gc,selectPos);
       
       drawPlayerStuff(gc,gb,
    		   selectPos,ourTurnSelect,buttonSelect,
    		   targets);
       
       numberMenu.drawSequenceNumbers(gc,CELLSIZE,labelFont,labelColor);
       
       GC.setFont(gc,standardBoldFont());
       

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,
        			(state==PendulumState.Puzzle),
        			buttonSelect,HighlightColor,rackBackGroundColor);
  
        // draw the avatars
        boolean addName = state!=PendulumState.Puzzle && (gb.simultaneousTurnsAllowed() ? false : true);
        standardGameMessage(gc,messageRotation,
        					// note that gameOverMessage() is also put into the game record
            				state==PendulumState.Gameover
            					?gameOverMessage(gb)
            					:effectiveStateMessage(gb,state,ap),
            				addName,
            				gb.whoseTurn,
            				stateRect);
        if(addName) { gb.getPlayerChip(gb.whoseTurn).drawChip(gc,this,iconRect,null); }
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
            //      DrawRepRect(gc,pl.displayRotation,Color.black,b.Digest(),repRect);
        eyeRect.activateOnMouse = true;
        eyeRect.draw(gc,selectPos);
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        if(G.debug())
        {	testButton.setOnText("Resume "+pendingMoves.size()); 
        	testButton.draw(gc,selectPos);
        }
        zoomer.drawMagnifier(gc,selectPos,boardRect,0.05,0.96,0.93,0);
        zoomer.drawMagnifier(gc,selectPos,councilRect,0.1,0.94,0.94,0);
    }

 
    public commonPlayer currentRobotPlayer()
    {
    	if(simultaneousTurnsAllowed())
    	{
    		Random r = new Random(G.Date());
    		int n = r.nextInt(bb.nPlayers());
    		return getPlayer(n);
    	}
    	return super.currentRobotPlayer();
    }
    CommonMoveStack pendingMoves = new CommonMoveStack();
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
    {	switch(m.op) {
    		case MOVE_PAUSE_COMMUNICATION:
    			testButton.setValue(true);
    			return true;
    		case MOVE_RESUME_COMMUNICATION:
    			testButton.setValue(false);
        		while(pendingMoves.size()>0)
        		{
        			commonMove rep = pendingMoves.remove(0,true);
        			Boolean tr = (Boolean)rep.getProperty("transmit");
        			replayMode rm = (replayMode)rep.getProperty("replaymode");
        			PerformAndTransmit(rep,tr,rm);
        		}
        		return true;
    		case MOVE_SETACTIVE:
            	{	PendulumMovespec mm = (PendulumMovespec)m;
            		guiPlayer = mm.forPlayer;
            	}
            	break;
    		default: break;
    		}
 
    	if(testButton.isOn())
    	{
    		m.setProperty("delayed",true);
    		m.setProperty("transmit",transmit);
    		m.setProperty("replaymode",replay);
    		pendingMoves.push(m);
    		return true;
    	}
    	else
    	{  	
    	return(super.PerformAndTransmit(m,transmit,replay));
    	}
    }

    /**
     * Execute a move by the other player, or as a result of local mouse activity,
     * or retrieved from the move history, or replayed form a stored game. 
     * @param mm the parameter is a commonMove so the superclass commonCanvas can
     * request execution of moves in a generic way.
     * @return true if all went well.  Normally G.Error would be called if anything went
     * seriously wrong.
     */
    public boolean Execute(commonMove mm,replayMode replay)
    {	//PendulumMovespec m = (PendulumMovespec)mm;
    	//G.print("before "+m+" "+m.purpleTimer+" "+replay+" "+bb.purpleTimer.timeToRun);
    	 switch(mm.op) 
    	 {
    	 case MOVE_PAUSE:	
    		 pauseButton.setValue(true);
    		 return true;
    	 case MOVE_RESUME:
    		 pauseButton.setValue(false);
    		 return true;
    	 case MOVE_RESET:
    		 if(simultaneousTurnsAllowed())
	    	 {
	    		PendulumMovespec m = (PendulumMovespec)mm;
	    		PlayerBoard pb = bb.getPlayerBoard(m.forPlayer);
	    		if(pb.pickedObject!=null)
	    		{	PendulumMovespec m2 = new PendulumMovespec(MOVE_DROP,pb.pickedSource,pb.pickedObject,m.forPlayer);
	    			PerformAndTransmit(m2,true,replayMode.Live);
	    		}
	    		return true;
	    	 }
    		 break;
    	 default: break; 
    	 }
    	 
    	 handleExecute(bb,mm,replay);
    	 numberMenu.recordSequenceNumber(bb.moveNumber);
        //G.print("after "+m+" "+m.purpleTimer+" "+replay+" "+bb.purpleTimer.timeToRun);
        /**
         * animations are handled by a simple protocol between the board and viewer.
         * when stones are moved around on the board, it pushes the source and destination
         * cells onto the animationStck.  startBoardAnimations converts those points into
         * animation sprites.  drawBoardElements arranges for the destination stones, which
         * are already in place, to disappear until the animation finishes.  The actual drawing
         * is done by drawSprites at the end of redrawBoard
         */
        startBoardAnimations(replay,bb.animationStack,bb.cellSize(),MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay.animate) { playSounds(mm); }
		return (true);
    }
     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for pushfight, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
      * destination chip disappear until the animation is finished.
      * @param replay
      */
//     void startBoardAnimations(replayMode replay)
//     {
//        if(replay.animate)
//     	{
//     		double full = G.distance(0,0,G.Width(boardRect),G.Height(boardRect));
//        	while(bb.animationStack.size()>1)
//     		{
//     		PushfightCell dest = bb.animationStack.pop();
//     		PushfightCell src = bb.animationStack.pop();
//    		double dist = G.distance(src.current_center_x, src.current_center_y, dest.current_center_x,  dest.current_center_y);
//    		double endTime = masterAnimationSpeed*0.5*Math.sqrt(dist/full);
    		//
    		// in cases where multiple chips are flying, topChip() may not be the right thing.
    		//
//     		startAnimation(src,dest,dest.topChip(),bb.cellSize(),0,endTime);
//     		}
//     	}
//        	bb.animationStack.clear();
//     } 

 void playSounds(commonMove mm)
 {
	 switch(mm.op)
	 {
	 case MOVE_PICK:
	 case MOVE_SELECT:
	 case MOVE_DROP:
		 playASoundClip(light_drop,100);
		 break;
	 case MOVE_STARTPLAY:
	 case MOVE_FLIP:
	 case MOVE_AUTOFLIP:
		 playASoundClip(flipSound,100);
		 break;
	 default: break;
	 }
 }
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st,int pl)
    {
        return (new PendulumMovespec(st, pl));
    }
/**
 * prepare to add nmove to the history list, but also edit the history
 * to remove redundant elements, so that indecisiveness by the user doesn't
 * result in a messy game log.  
 * 
 * For all ordinary cases, this is now handled by the standard implementation
 * in commonCanvas, which uses the board's Digest() method to distinguish new
 * states and reversions to past states.
 * 
 */
      public commonMove EditHistory(commonMove nmove)
      {	  // some damaged games ended up with naked "drop", this lets them pass 
    	  boolean oknone = false;
    	  switch(nmove.op) {
    	  	case MOVE_DROP: 
    	  	case MOVE_SETACTIVE:
    	  	case MOVE_STARTCOUNCIL:
    	  	case MOVE_AUTOFLIP:
    	  	case MOVE_STARTPLAY:
    	  	case MOVE_REST:
    	  	case MOVE_READY:
    	  	case MOVE_PAUSE:
    	  	case MOVE_RESUME:
    	  		oknone = true;
    	  		break;
    	  	default: break;
    	  }
    	  commonMove rval = EditHistory(nmove,oknone);
     	     
    	  return(rval);
      }
      /**
       *  the default behavior is if there is a picked piece, unpick it
       *  but if no picked piece, undo all the way back to the done.
       */
      //public void performUndo()
      //{
      //	if(allowBackwardStep()) { doUndoStep(); }
      //	else if(allowUndo()) { doUndo(); }
      //
      //}

     
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 * <p>
 * Note on debugging: If you get here mysteriously with hitOjbect and hitCode
 * set to default values, instead of the values you expect, you're probably
 * not setting the values when the gc is null.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof PendulumId)// not dragging anything yet, so maybe start
        {
        PendulumId hitObject =  (PendulumId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;

        } 
        }
    }

	  public boolean allowResetUndo()
	  {
		  if(simultaneousTurnsAllowed())
		  {
		    	int who = reviewOnly 
	    				? simultaneousTurnsAllowed()?guiPlayer:bb.whoseTurn 
	    				: getActivePlayer().boardIndex;
		    	PlayerBoard pb = bb.getPlayerBoard(who);
		    	if(pb.pickedObject!=null) 
		    		{ return true; }
		  }
		  return super.allowResetUndo();
	  }

  private PendulumChip bigChip = null;
	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
 * <p>
 * Note on debugging: If you get here mysteriously with hitOjbect and hitCode
 * set to default values, instead of the values you expect, you're probably
 * not setting the values when the gc is null.
	 */
    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
    	int who = reviewOnly 
    				? simultaneousTurnsAllowed()?guiPlayer:bb.whoseTurn 
    				: getActivePlayer().boardIndex;
        if(id==null) { bigChip = null; }
        else if(!(id instanceof PendulumId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        PendulumId hitCode = (PendulumId)id;
        
        // if direct drawing, hp.hitObject is a cell from a copy of the board
        PendulumCell hitObject = bb.getCell(hitCell(hp));
        switch (hitCode)
        {
        default:
        	if (performStandardButtons(hitCode, hp)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " + hitObject);
            }
        	break;
        case Pause:
        	if(pauseButton.isOn()) { PerformAndTransmit("Resume P"+who); }
        	else { PerformAndTransmit("Pause P"+who); }
        	break;
        case TestPrivilege:
        	{
        	boolean newval = !testButton.isOn();
        	testButton.setValue(newval);
        	if(newval) { PerformAndTransmit("PauseCommunication P"+who); }
        	else { PerformAndTransmit("ResumeCommunication P"+who); }
        	}
        	break;
        case ShowCard:
        	PendulumChip card = (PendulumChip)hp.hitData;
        	bigChip = (bigChip==card) ? null : card;
        	break;
        case TakeLegendary:
        	PerformAndTransmit("LegendaryAchievement P"+who);
        	break;
        case TakeStandard:
        	PerformAndTransmit("StandardAchievement P"+who);
        	break;
        case Rest:
        	PerformAndTransmit("Rest P"+who);
        	break;
        case StartPlay:
        	PerformAndTransmit("Ready P"+who);
        	break;
        case StartCouncil:
        	PerformAndTransmit("Rest P"+who);
        	break;
        case Trash:
        case RewardCard:
        case PlayerVotes:
        case PlayerVotesReserves:
        case PurpleTimer:
        case BlackTimer:
        case GreenTimer:
        case PlayerCulture:
        case PlayerStratCard:
        case PlayerPlayedStratCard:
        case PlayerCash:
        case PlayerMilitary:
        case PlayerCashReserves:
        case PlayerMilitaryReserves:
        case PlayerCultureReserves:
        case AchievementCard:
        case Privilege:
        case Achievement:
        case AchievementCardStack:
        case Province:
        case PlayerPrestigeVP:
        case PlayerPowerVP:
        case PlayerPopularityVP:
        case PlayerLegendary:
        case PlayerMax3Cards:
        case PlayerBlueBenefits:
        case PlayerYellowBenefits:
        case PlayerBrownBenefits:
        case RewardDeck:
        case PlayerRedBenefits:
        case ProvinceCardStack:
        case GreenActionA:
        case GreenActionB:
        case BlackActionA:
        case BlackActionB:
        case PurpleActionA:
        case PurpleActionB:
        case BlackMeepleA:
        case BlackMeepleB:
        case GreenMeepleA:
        case GreenMeepleB:
        case PurpleMeepleA:
        case PurpleMeepleB:
        case PlayerMeeples:
        case PlayerGrandeReserves:
        case PlayerMeepleReserves:
        case PlayerGrandes:
         	{
        	PendulumMovespec m = (PendulumMovespec)hp.hitData;
        	if(reviewOnly && hitObject.col>='A')
        	{	PlayerBoard pb = bb.getPlayerBoard(hitObject.col);
        		int boardPlayer =pb.boardIndex;
        		if(boardPlayer!=bb.whoseTurn)
        		{
        			PerformAndTransmit("SetActive P"+boardPlayer);
        			who = boardPlayer;
        		}
        	}
        	PlayerBoard pb = bb.getPlayerBoard(who);
        	if(m.op==MOVE_SELECT)
        	{
        		PerformAndTransmit(G.concat("Select P",who," ",hitObject.rackLocation(),
        				" ",hitObject.col," ",hitObject.row," ",m.chip.idString()));
        	}
        	else if(m.op==MOVE_FLIP)
        			{
        			PerformAndTransmit(G.concat("Flip P",who," ",hitObject.rackLocation(),
        				" ",hitObject.col," ",hitObject.row));
        			}
        	else if(pb.pickedObject==null)
        		{  	
        			PendulumChip top = hitObject.chipAtIndex(hp.hit_index);
        			PColor co = top.color;
        			if(co!=null && co!=pb.color)
        				{
        				// picking up a colored piece from the board
        				pb = bb.getPlayerBoard(top);
        				if(pb!=null)
        				{// null if picking up a neutral piece
        				PerformAndTransmit("SetActive P"+pb.boardIndex);
        				who = pb.boardIndex;
        				}
        				}
         		  PerformAndTransmit(G.concat("Pick P",who," ",hitObject.rackLocation(),
          				" ",hitObject.col," ",hitObject.row," ",
          				top.idString()));
        		}
        		else 
        		{
           		PerformAndTransmit(G.concat("Drop P",who," ",hitObject.rackLocation(),
        				" ",hitObject.col," ",hitObject.row));
      		}
        	}
        	break;
        case PlayerRefill:
        	PerformAndTransmit("PlayerRefill P"+who);
        	break;
        case Select:
   			PerformAndTransmit("SetActive P"+hp.hit_index,false,replayMode.Live);
        	
        	break;
        case ToggleEye:
        	eyeRect.toggle();
        	break;


        }
        }
    }



    private boolean setDisplayParameters(PendulumBoard gb,Rectangle r)
    {
      	boolean complete = false;
      	gb.SetDisplayRectangle(r);
    	gb.setCouncilRectangle(councilRect);
    	gb.setTimerRectangle(timerRect);
       	if(complete) { generalRefresh(); }
      	return(complete);
    }
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     * <p>
     * if complete is true, we definitely want to start from scratch, otherwise
     * only the known changed elements need to be painted.  Exactly what this means
     * is game specific, but for pushfight the underlying empty board is cached as a deep
     * background, but the chips are painted fresh every time.
     * <p>
     * this used to be very important to optimize, but with faster machines it's
     * less important now.  The main strategy we employ is to paint EVERYTHING
     * into a background bitmap, then display that bitmap to the real screen
     * in one swell foop at the end.
     * 
     * @param gc the graphics object.  If gc is null, don't actually draw but do check for mouse location anyay
     * @param complete if true, always redraw everything
     * @param hp the mouse location.  This should be annotated to indicate what the mouse points to.
     */
  //  public void drawCanvas(Graphics gc, boolean complete,HitPoint hp)
  //  {	
       	//drawFixedElements(gc,complete);	// draw the board into the deep background
   	
    	// draw the board contents and changing elements.
        //redrawBoard(gc,hp);
        //      draw clocks, sprites, and other ephemera
        //drawClocksAndMice(gc, null);
        //DrawArrow(gc,hp);
 //    }
    /**
     * draw any last-minute items, directly on the visible canvas. These
     * items may appear to flash on and off, if so they probably ought to 
     * be drawn in {@link #drawCanvas}
     * @param offGC the gc to draw
     * @param hp the mouse {@link HitPoint} 
     */
  // public void drawCanvasSprites(Graphics offGC,HitPoint hp)
  //  {
  //     DrawTileSprite(offGC,hp); //draw the floating tile we are dragging, if present
       //
       // draw any animations that are in progress
       //
  //     drawSprites(offGC);       
  //  }
    
    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parseable by {@link online.game.commonCanvas#performHistoryInitialization}
     * @return return what will be the init type for the game
     */
     public String gameType() 
    	{
    	return(bb.gameType()); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Pendulum_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int np = his.intToken();	// players always 2
    	long rv = his.longToken();
    	int rev = his.intToken();	// rev does't get used either
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // int rk = G.IntToken(his);
    	// bb.doInit(token,rk);
        bb.doInit(token,rv,np,rev);
        adjustPlayers(np);

    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
        return (handled);
    }
    
    private int gameRecorder = 0;
    
    private void setGameRecorder()
    {	// select the lowest boardIndex of a real player as the recorder
    	int least = -1;
    	for(int i=0;i<players.length;i++)
    	{
    		commonPlayer p = players[i];
    		// isActivePlayer excludes robots and players who have disconnected.
    		if(p!=null && p.isActivePlayer())
    		{
    			if(least<0 || least>p.boardIndex) { least = p.boardIndex; } 
    		}
    	}
    	gameRecorder = least;	
    }
    
    public void setLimbo(boolean v)
    {
    	boolean oldLimbo = inLimbo;
    	super.setLimbo(v);
    	if(oldLimbo!=inLimbo)
    	{
    		setGameRecorder();
    	}
    }
    public boolean iAmTheLeadPlayer()
    {
    	return getActivePlayer().boardIndex==gameRecorder;
    }
    public RecordingStrategy gameRecordingMode()
    {
    	return iAmTheLeadPlayer() ? RecordingStrategy.Single : RecordingStrategy.None;
    }

    public boolean allowRobotsToRun(commonPlayer pp)
    {
    	if(simultaneousTurnsAllowed())
    	{
    		PlayerBoard pb = bb.getPlayerBoard(pp.boardIndex);
    		switch(pb.uiState)
    		{
    			case Rest:
    			case Ready:
    				return false;
    			default: break;
    		}
    	}
    	return true;
    }

    private long lastTimerTime = 0;
    public void ViewerRun(int wait)
    {
        super.ViewerRun(Math.min(400,wait));
        long now = G.Date();
        if(!GameOver()
        		&& !reviewMode()
        		&& !inLimbo
        		&& !pauseButton.isOnNow()) 
        	{ 
        		if(lastTimerTime>0) { bb.doTimers(now-lastTimerTime); } 
        	}
        lastTimerTime = now;
        
        if( !inLimbo
        	 && (G.offline() || iAmTheLeadPlayer())
           	 && !reviewMode())
        {	PendulumState state = bb.getState();
     		switch(state)
    		{
     		case Flip:
     			PerformAndTransmit("AutoFlip");
     			break;
    		case StartPlay:
    			PerformAndTransmit("Play");
    			break;
    		case StartCouncil:
    			PerformAndTransmit("Council");
    			break;
    		default: break;
        	}
        }
    }
    /**
     * returns true if the game is over "right now", but also maintains 
     * the gameOverSeen instance variable and turns on the reviewer variable
     * for non-spectators.
     */
    //public boolean GameOver()
    //{	// the standard method calls b.GameOver() and maintains
    	// two variables.  
    	// "reviewer=true" means we were a player and the end of game has been reached.
    	// "gameOverSeen=true" means we have seen a game over state 
    //	return(super.GameOver());
    //}
    
    /** this is used by the stock parts of the canvas machinery to get 
     * access to the default board object.
     */
    public BoardProtocol getBoard()   {    return (bb);   }


/** this is used by the scorekeeper to determine who won. Draws are indicated
 * by both players returning false.  Be careful not to let both players return true!
 */
   // public boolean WinForPlayer(commonPlayer p)
   // { // this is what the standard method does
      // return(getBoard().WinForPlayer(p.index));
   //   return (super.WinForPlayer(p));
   // }

    /** start the robot.  This is used to invoke a robot player.  Mainly this needs 
     * to know the class for the robot and any initialization it requires.  The return
     * value is the player actually started, which is normally the same as requested,
     * but might be different in some games, notably simultaneous play games like Raj
     *  */
   // public commonPlayer startRobot(commonPlayer p,commonPlayer runner,Bot bot)
   // {	// this is what the standard method does:
    	// int level = sharedInfo.getInt(sharedInfo.ROBOTLEVEL,0);
    	// RobotProtocol rr = newRobotPlayer();
    	// rr.InitRobot(sharedInfo, getBoard(), null, level);
    	// p.startRobot(rr);
    //	return(super.startRobot(p,runner,bot));
    //}
    // this is conventionally used as the game state message above the board,
    // but it is also inserted into the game record as the RE property
    //
    //public String gameOverMessage()
    //{
    //	return super.gameOverMessage();
    //}
    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new PendulumPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     */
    public void ReplayMove(sgf_node no)
    {
        String comments = "";
        sgf_property prop = no.properties;

        while (prop != null)
        {
            String name = prop.getName();
            String value = (String) prop.getValue();
            
            if (setup_property.equals(name))
            {
                bb.doInit(value);
                adjustPlayers(bb.nPlayers());
              }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
           else if (parseVersionCommand(name,value,2)) {}
           else if (parsePlayerCommand(name,value)) {}
            else
            {	// handle standard game properties, and also publish any
            	// unexpected names in the chat area
            	replayStandardProps(name,value);
            }

            prop = prop.next;
        }

        if (!"".equals(comments))
        {
            setComment(comments);
        }
    }
    /**
     *****		 Simultaneous moves support *****
     * 
     * Games where all the players move at the same time cause a lot of problems, but
     * it's the right thing to do in some cases.  The current games that use at least
     * some simultaneous moves are:
     *  Euphoria, Magnet, Raj, One Day In London, Q.E., Breaking Away, Tammany Hall, and Imagine
     * 
     * The root of the problem is that players WILL disagree about the order of arrival of 
     * asynchronous moves. This causes their game records to be different from one another,
     * and causes the server's incremental bookkeeping of the game state to be seen as
     * inconsistent.
     * 
     * The overall strategy for dealing with his problem is to segregate the simultaneous
     * moves to the end of the game record, and anchor the server's record of the game at
     * the beginning of the ephemeral block.   At appropriate points, the ephemeral moves
     * are replaced in the game record by equivalent non-ephemeral moves in canonical order.
     * 
     * 
     * Another problem is time accounting - while in simultaneous mode, the clocks for all 
     * the players who have not completed their move should tick.
     * 
     * Tendrils to support this are all over the place, which made support difficult; but
     * the most common solutions are embodied in requirements for move specs, states, and
     * in overridable methods of commonCanvas 
     * 
     * the xxMovespec class will use pairs of ephemeral/normal moves, and implement
     * and board states which allow asynchronous moves will be distinct from the states
     * that do not.
     *  
     * Reviews of games which originally had simultaneous moves will have all synchronous
     * moves, so the game engine ought to function perfectly with any ephemeral moves.
     * 
     *  
     */
    /* for debugging and initial development, leave this false */
    boolean SIMULTANEOUS_PLAY = false;		
    /** true if this game ever has simultaneous moves.  If true, formEphemeralHistoryString
     * will do a second pass to collect the ephemeral moves, and {@link #useEphemeraBuffer} will
     * expect there to be one.
     */
    public boolean gameHasEphemeralMoves() { return(SIMULTANEOUS_PLAY); }

    private String vprogressString()
    {	return super.gameProgressString()+" score score";
    }
    public String gameProgressString()
    {	// this is what the standard method does
    	 return(mutable_game_record 
    			? Reviewing
    			: vprogressString());
    }

	/** this is a debugging interface to provide information about memory
	 * consumption by images.
	 */
	public double imageSize(ImageStack im)
	  {
		  return(super.imageSize(im) + PendulumChip.imageSize(im));
	  }
    
    /**
     * colorize a string and return a Text with the result.  This is used
     * to substitute icons for words, or translate the words, in the string;
     * or to otherwise change the presentation of the string to something other than a plain 
     * fonted string.  See {@link lib.Text#colorize}
     * This is used by redrawGameLog to transform raw strings from game events.  In conjunction
     * with iconification done by "shortMoveString", this is responsible for making pretty
     * pictures in the game log.
     * @param str
     * @return the new Text object
     */
    //public Text colorize(String str)
    //{	return(TextChunk.create(str));
    //}
	
    public int getLastPlacement() { return bb.placementIndex; }
}

