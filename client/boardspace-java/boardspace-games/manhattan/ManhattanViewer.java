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
package manhattan;

import java.awt.*;
import online.common.*;
import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.DefaultId;
import lib.DrawableImage;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.StockArt;
import lib.Text;
import lib.TextButton;
import lib.TextChunk;
import lib.TextGlyph;
import lib.TextStack;
import lib.Toggle;
import lib.Image;
import lib.ImageStack;
import lib.LFrameProtocol;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;
import common.GameInfo;
import static manhattan.ManhattanMovespec.*;


/**
 * 
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
public class ManhattanViewer extends CCanvas<ManhattanCell,ManhattanBoard> implements ManhattanConstants,  PlacementProvider
{		// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	// TODO: display some indication of the university's owner for "india" selection.
	

    static final String Manhattan_SGF = "manhattan"; // sgf game name

    // file names for jpeg images and masks
    static final String ImageDir =  
    		G.isCodename1() ? "/appdata/manhattan/images/" : "/manhattan/images/";
    
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackIdleColor = new Color(155,155,155);
    
    private Color chatBackgroundColor = new Color(235,235,235);
    private Color rackBackGroundColor = new Color(192,192,192);
    private Color boardBackgroundColor = new Color(220,165,155);

    private Rectangle visualBoardRect = new Rectangle();
    private Rectangle visualBoardRectRotated = new Rectangle();
    
    // private state
    private ManhattanBoard bb = null; //the board from which we are displaying
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
    private Toggle eyeRect = new Toggle(this,"eye",
 			StockArt.NoEye,ManhattanId.ToggleEye,NoeyeExplanation,
 			StockArt.Eye,ManhattanId.ToggleEye,EyeExplanation
 			);
 	private TextButton doneButton = addButton(DoneAction,GameId.HitDoneButton,ExplainDone,
			HighlightColor, rackBackGroundColor,rackIdleColor);
 	private TextButton retrieveButton = addButton(RetrieveAction,ManhattanId.HitRetrieve,ExplainRetrieve,
 			HighlightColor, rackBackGroundColor,rackIdleColor);
 	
	// private menu items
    
    private Rectangle playerBoards[] = addRect("pbs",5);
    private Rectangle playerBombDesigns[] = addRect("design",5);
    private Rectangle playerPersonality[] = addRect("personality",5);
    private Rectangle playerCard[] = addRect("playercard",5);
    private Rectangle playerChip[] = addRect("score",5);
    private Toggle playerEyes[] = 
    	{
    			new Toggle(this,"eye",
    					StockArt.NoEye,ManhattanId.SeeBombs,HideBombsExplanation,
    					StockArt.Eye,ManhattanId.SeeBombs,SeeBombsExplanation
    					),
    			new Toggle(this,"eye",
    					StockArt.NoEye,ManhattanId.SeeBombs,HideBombsExplanation,
    					StockArt.Eye,ManhattanId.SeeBombs,SeeBombsExplanation
    					),
    			new Toggle(this,"eye",
    					StockArt.NoEye,ManhattanId.SeeBombs,HideBombsExplanation,
    					StockArt.Eye,ManhattanId.SeeBombs,SeeBombsExplanation
    					),
    			new Toggle(this,"eye",
    					StockArt.NoEye,ManhattanId.SeeBombs,HideBombsExplanation,
    					StockArt.Eye,ManhattanId.SeeBombs,SeeBombsExplanation
    					),
    			new Toggle(this,"eye",
    					StockArt.NoEye,ManhattanId.SeeBombs,HideBombsExplanation,
    					StockArt.Eye,ManhattanId.SeeBombs,SeeBombsExplanation
    					)
    			
    	};
    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	ManhattanChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = ManhattanChip.icon.getImage();
    }

    /**
     * this is the hook for substituting alternate tile sets.  This is called at a low level
     * from drawChip, and the result is passed to the chip's getAltChip method to substitute
     * a different chip.
     */
    public int getAltChipset() { return(0); }
    
 
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	int players_in_game = info.getInt(OnlineConstants.PLAYERS_IN_GAME,5);
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        //
        // not suitable for games with any optional "done" states.  If true, autodone
        // is controlled by an option menu option.  Also conditionalize showing the
        // "done" button with autoDoneActive()
        enableAutoDone = false;
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default
        
        addZoneRect("done",doneButton);	// this makes the "done" button a zone of its own
        
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	ManhattanConstants.putStrings();
        }
         
        String type = info.getString(GameInfo.GAMETYPE, ManhattanVariation.manhattan.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new ManhattanBoard(type,players_in_game,randomKey,getStartingColorMap(),ManhattanBoard.REVISION);
        //
        // this gets the best results on android, but requires some extra care in
        // the user interface and in the board's copyBoard operation.
        // in the user interface.
        useDirectDrawing(true);
        doInit(false);
        adjustPlayers(players_in_game);
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
    //public void startPlaying()
    //{	super.startPlaying();
    //}
    
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
    	setLocalBoundsV(x,y,width,height,new double[] {0.5,1,2});
    	zoomer.reCenter();
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double v)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*15;	
       	int minChatW = fh*35;	
        int minLogH = fh*15;	
        int margin = fh/2;
        int buttonW = fh*7;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.7,	// 60% of space allocated to the board
    			2.0,	// aspect ratio for the board
    			fh*5,	// minimum cell size
    			fh*6,	// maximum cell size
    			0.6		// preference for the designated layout, if any
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
    	// to face the player.
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneButton,editRect,retrieveButton);
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
    	boolean rotate = mainW<mainH;	
        int nrows = rotate ? 21 : 15;  // b.boardRows
        int ncols = rotate ? 15 : 21;	 // b.boardColumns
  	
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
    	G.SetRect(visualBoardRect,boardX,boardY+stateH,boardW,boardH-stateH*2);
    	G.copy(visualBoardRectRotated,visualBoardRect);
    	G.setRotation(visualBoardRectRotated,Math.PI/2);
    	
    	if(rotate)
    	{	// this conspires to rotate the drawing of the board
    		// and contents if the players are sitting opposite
    		// on the short side of the screen.
    		G.setRotation(boardRect,-Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW-CELLSIZE,stateH,goalRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        return boardW*boardH+G.Width(playerBoards[0]);
 	
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
    	Rectangle box =  pl.createRectangularPictureGroup(x,y,2*unitsize/3);
    	Rectangle done = doneRects[player];
    	Rectangle chip = playerChip[player];
    	Rectangle eye = playerEyes[player];
    	int doneW = plannedSeating()? unitsize*2 : 0;
    	int t = G.Bottom(box);
       	G.SetRect(chip,x-unitsize/4,t-unitsize*4/3,unitsize*5/4,unitsize*5/4);
       	int bigboxH = unitsize*5;
       	int overallBottom = y+bigboxH;
 
       	G.SetRect(done,x,t,doneW,doneW/2);
    	G.union(box, done);
    	
    	G.SetRect(eye,G.Right(box),t-unitsize,unitsize,unitsize);
    	
    	int l = x;
    	if(bb.testOption(Options.Personalities))
    	{
    	Rectangle pp = playerPersonality[player];
    	int bb = G.Bottom(box);
    	int pwid = Math.max(doneW,unitsize*3/2);
    	G.SetRect(pp,l,bb,pwid,overallBottom-bb);
    	G.union(box,pp);
    	l += pwid;
    	}
    	
    	{
    	Rectangle or = playerBombDesigns[player];
    	int pw = unitsize*2+(doneW==0?unitsize:unitsize/2);
    	G.SetRect(or,l,t,pw,overallBottom-t);
    	G.union(box,or);
    	l += pw;
    	}

    	{
    	Rectangle pp = playerCard[player];
    	G.SetRect(pp,l,t,unitsize*2,overallBottom-t);
    	G.union(box,pp);
    	l += unitsize*3/2;
    	}
   	
    	Rectangle pb = playerBoards[player];
       	G.SetRect(pb,G.Right(box),y,unitsize*6+unitsize/4,bigboxH);
    	G.union(box,pb);

    	pl.displayRotation = rotation;
    	return(box);
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
    	ManhattanChip ch = bb.pickedObject;
    	ManhattanCell top = bb.pickedSourceStack.top();
    	if(ch!=null) { ch.drawChip(g,this,top.lastSize(), xp, yp, null); }
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
     ManhattanChip.backgroundTile.image.tileImage(gc, fullRect);   
      drawFixedBoard(gc);
     }
    
    Image scaledBoard = null;
    // land here after rotating the board drawing context if appropriate
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	// note, drawing using disB is very important for boards which have different sizes
    	// and which are using copyies of the real board for drawing.
    	// Not so important for boards which are always the same dimensions, but it's always safe.
    	ManhattanBoard gb = disB(gc);
        boolean reviewBackground = reviewMode()&&!mutable_game_record;
        if(reviewBackground)
        {	 
         ManhattanChip.backgroundReviewTile.image.tileImage(gc,brect);   
        }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(gb,brect);
	      // if the board is one large graphic, for which the visual target points
	      // are carefully matched with the abstract grid
	  	scaledBoard = ManhattanChip.board.getImage().centerScaledImage(gc, brect,scaledBoard);


	      // draw the tile grid.  The positions are determined by the underlying board
	      // object, and the tile itself if carefully crafted to tile the pushfight board
	      // when drawn this way.  For games with simple graphics, we could use the
	      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
	      // but for more complex graphics with overlapping shadows or stacked
	      // objects, this double loop is useful if you need to control the
	      // order the objects are drawn in.
 
    }
    
    /**
     * translate the mouse coordinate x,y into a size-independent representation
     * presumably based on the cell grid.  This is used to transmit our mouse
     * position to the other players and spectators, so it will be displayed
     * at approximately the same visual spot on their screen.  
     * 
     * Some trickier logic may be needed if the board has several orientations,
     * or if some mouse activity should be censored.
     */
    public String encodeScreenZone(int x, int y,Point p)
    {
    	return(super.encodeScreenZone(x,y,p));
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
    public void drawBoardElements(Graphics gc, ManhattanBoard gb, Rectangle brect, HitPoint highlight,Hashtable<ManhattanCell,ManhattanMovespec>targets,HitPoint hitAny)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
    	ManhattanState state = gb.getState();
 
    	gb.setRectangle(brect);
    	//if(G.debug()) { gb.positionCells(); }
    	int cellSize = G.Width(brect)/18;
    	
       	drawBoardCell(gc,highlight,gb,targets,gb.seeBribe, cellSize*2, 0.1, 0.0, null,hitAny);
       	if(state==ManhattanState.Puzzle)
       	{
       	drawBoardCell(gc,highlight,gb,targets,gb.seeBank, cellSize*2, 0.2, 0.1, null,hitAny);
       	drawBoardCell(gc,highlight,gb,targets,gb.seeYellowcake, cellSize, 0.2, 0.1, null,hitAny);
       	}
       	drawBoardCell(gc,highlight,gb,targets,gb.seeDamage,cellSize*2/3,0,0,null,hitAny);
       	double space = (G.Width(brect)*0.75)/(0.7*cellSize*gb.availableWorkers.height());
       	double spacing = Math.min(space,0.5);
       	// draw them across the board between the buildings and the player spaces
       	drawBoardCell(gc,highlight,gb,targets,gb.availableWorkers, cellSize*8/10, spacing, 0.0, null,hitAny);
       	
       	drawBoardCell(gc,highlight,gb,targets,gb.seeBuildings, cellSize*2, 0.002, 0.003, ManhattanChip.BACK,hitAny);
       	drawBoardCell(gc,highlight,gb,targets,gb.seeDiscardedBombs, cellSize*3/4, 0.01, 0.01, ManhattanChip.BACK,hitAny);
       	drawBoardCell(gc,highlight,gb,targets,gb.seeBombtests, cellSize*3/4, 0.02, 0.03, null,hitAny);
     	drawBoardCell(gc,highlight,gb,targets,gb.seeBombs, cellSize*3/4, 0.01, 0.01, ManhattanChip.BACK,hitAny);
      	drawBoardCell(gc,hitAny,gb,targets,gb.seePersonalities, cellSize*3/4, 0.02, 0.025, null,hitAny);
       	drawBoardCell(gc,highlight,gb,targets,gb.seeNations, cellSize*3/4, 0.01, 0.02, ManhattanChip.BACK,hitAny);
       	
       	drawBoardCell(gc,highlight,gb,targets,gb.playDesignBomb, cellSize, 0.00, 0.03, null,hitAny);
       	drawBoardCell(gc,hitAny,gb,targets,gb.bombtestHelp,cellSize*2/3,0,0,null,hitAny);
       	boolean censor = gb.pendingBenefit==Benefit.BombDesign;
       	drawBoardCell(gc,hitAny,gb,targets,gb.seeCurrentDesigns, cellSize, 0.26, 0.22, censor?ManhattanChip.BACK:null,hitAny);
      	drawBoardCell(gc,highlight,gb,targets,gb.playMakePlutonium, cellSize*2, 0.00, 0.00, null,hitAny);
     	drawBoardCell(gc,highlight,gb,targets,gb.playMakeUranium, cellSize*2, 0.00, 0.00, null,hitAny);
       	drawBoardCell(gc,highlight,gb,targets,gb.playEspionage, cellSize*2, 0.00, 0.00, null,hitAny);
       	drawBoardCell(gc,highlight,gb,targets,gb.playRepair, cellSize*2, 0.00, 0.00, null,hitAny);
       	drawBoardCell(gc,highlight,gb,targets,gb.playMakeFighter, cellSize*2, 0.00, 0.00, null,hitAny);
       	drawBoardCell(gc,highlight,gb,targets,gb.playMakeBomber, cellSize*2, 0.00, 0.00, null,hitAny);
       	drawBoardCell(gc,highlight,gb,targets,gb.playBuyBuilding, cellSize*3/2, 0.2, 0.4, null,hitAny);
       	drawBoardCell(gc,highlight,gb,targets,gb.playBuyBuilding2, cellSize*3/2, 0.2, 0.4, null,hitAny);

    	drawBoardArray(gc,highlight,gb,targets,gb.playAirStrike, cellSize*2, 0.00, 0.00, null,hitAny);
    	drawBoardArray(gc,highlight,gb,targets,gb.seeEspionage, cellSize, 0.15, 0.000, null,hitAny);
    	drawBoardArray(gc,highlight,gb,targets,gb.seeBuilding, cellSize*2, 0.00, 0.00, null,hitAny);
    	drawBoardArray(gc,highlight,gb,targets,gb.seeUranium, cellSize, 0.2, 0.00, null,hitAny);
    	drawBoardArray(gc,highlight,gb,targets,gb.seePlutonium, cellSize, -0.2, 0.00, null,hitAny);
    	drawBoardArray(gc,highlight,gb,targets,gb.playMakeMoney, cellSize*2, 0.00, 0.00, null,hitAny);

    	drawBoardArray(gc,highlight,gb,targets,gb.playMine, cellSize*2, 0.00, 0.00, null,hitAny);
    	drawBoardArray(gc,highlight,gb,targets,gb.playUniversity, cellSize*2, 0.00, 0.00, null,hitAny);
    	
    	zoomer.drawMagnifier(gc,hitAny,brect,0.04,0.98,0.97,0);
    }
    public void drawPlayerArray(Graphics gc,HitPoint hp,ManhattanBoard gb,PlayerBoard pb,Hashtable<ManhattanCell, ManhattanMovespec> targets,
    		CellStack cells, ManhattanCell exclude, int cellSize, double xstep, 
    		double ystep,String hint, HitPoint hitAny)
    {
      	for(int i=0,siz=cells.size();i<siz;i++)
    	{	ManhattanCell c = cells.elementAt(i);
       		drawPlayerCell(gc,hp,gb,pb,targets,c,cellSize,xstep,ystep, hint,hitAny);
    		if(c==exclude) { drawGrayOverlay(gc, pb, c,cellSize); }
    	}
    }
    public void drawGrayOverlay(Graphics gc, PlayerBoard pb,ManhattanCell c,int cellSize)
    {	int xpos =pb.cellToX(c);
    	int ypos =pb.cellToY(c);
    	ManhattanChip.BlankCard.drawChip(gc,this,cellSize,xpos,ypos, null);
    }
    public void drawPlayerArray(Graphics gc,HitPoint hp,ManhattanBoard gb,PlayerBoard pb,Hashtable<ManhattanCell, ManhattanMovespec> targets,ManhattanCell cells[], int cellSize, double xstep, double ystep, 
    		String hint,HitPoint hitAny)
    {
      	for(ManhattanCell c : cells) 
    	{	
      		drawPlayerCell(gc,hp,gb,pb,targets,c,cellSize,xstep,ystep, hint,hitAny);
    	}
    }
    
    public boolean drawDefaultCell(Graphics gc,HitPoint hp,boolean selected,ManhattanCell c,int cellSize,int xpos,int ypos,double xstep,double ystep,String hint, HitPoint hitAny)
    {
    	boolean see = c.rackLocation()==ManhattanId.Stockpile && c.height()>=2;
    	numberMenu.saveSequenceNumber(c,xpos,ypos);
    	boolean hit = c.drawStack(gc,this,hp, cellSize,xpos,ypos,0,xstep,ystep,see ? null : hint);
		if(hit)
		{	
			hp.spriteColor = Color.red;
			hp.awidth = cellSize/2;
		}
		
		if(selected) 
			{ 
			if(bb.getState()==ManhattanState.ConfirmNichols)
			{
			StockArt.Exmark.drawChip(gc,this,cellSize/2,xpos,ypos,null);
			}
			else 
			{ StockArt.Checkmark.drawChip(gc,this,cellSize/2,xpos,ypos,null); 
			}
			}
		//StockArt.SmallO.drawChip(gc,this,cellSize,xpos,ypos,null);
		return hit;
    	
    }
    /**
     * scan a cell for possible cards to highlight using the "bigchip" mechanism.
     * nothing is actually drawn, but the contents of the cell is scanned for cards.
     * 
     * @param c
     * @param hitAny
     * @param cellSize
     * @param xpos
     * @param ypos
     * @param xstep
     * @param ystep
     * @param hint
     * @return
     */
    public boolean findBigChip(ManhattanCell c,boolean wholeCell,HitPoint hitAny,int cellSize,int xpos,int ypos,double xstep,double ystep,String hint)
    {
	int hi = c.height();
	boolean hit = false;
	if(!ManhattanChip.BACK.equals(hint) && (hi>=1))
	{
		for(int i=0;i<hi;i++)
		{
		ManhattanChip base = c.chipAtIndex(i);
		switch(base.type)
		{
		default: break;
		case Building:
		case Personalities:
		case Nations:
		case Bomb:
			int xp = (int)(xpos + cellSize*i*xstep);
			int yp = (int)(ypos - cellSize*i*ystep);	
			int top = wholeCell ? yp-cellSize*4/6 : yp+cellSize/3;
			int h = wholeCell ? cellSize*4/3 : cellSize/3;
			int left = xp-cellSize*2/5;
			int w = cellSize*4/5;
			//GC.frameRect(gc,Color.blue,r);
			if(G.pointInRect(hitAny,left,top,w,h))
				{Rectangle r = new Rectangle(left,top,w,h);
				hitAny.spriteRect = r;
				hitAny.spriteColor = Color.red;
				hitAny.hitCode = ManhattanId.ShowChip;
				hitAny.hitData = base;
				hitAny.hit_index = 0;
				hitAny.hitObject = c;
				hit = true;
			}
		}}}
		return hit;
	}
    private ManhattanCell temp = new ManhattanCell();
    public boolean drawBuildingCell(Graphics gc,HitPoint hp,boolean selected,ManhattanMovespec move,ManhattanCell c,int cellSize,int xpos,int ypos,double xstep,double ystep,
    		String hint,HitPoint hitAny)
    {	
    	ManhattanCell t = temp;
    	t.copyAllFrom(c);
    	int n = 0;
    	ManhattanChip top=null;
    	// count the number of workers and remove them
    	while ( (top = t.topChip())!=null 
    			&& top!=null
    			&& (top.type==Type.Worker || top.type==Type.Damage ||top.type==Type.Bomber || top.type==Type.Bombtest))
    	{	n++;
    		t.removeTop();
    	}
    	// draw the naked cell (and maybe also the card)
    	boolean hit = drawDefaultCell(gc,hp,false,t,cellSize,xpos,ypos,xstep,ystep,hint, hitAny);
    	if(hit)
    	{	// in case it used T the temp cell
    		hp.hitObject = c;
    	}
    	n -= c.activeAnimationHeight();
    	if(n>0)
    	{
    	// draw the workers
    	int step = cellSize/(2*n);
    	int offset = xpos-(int)(step*(n/2.0-0.5));
    	int ind = c.height()-n;
    	while(n-- > 0)
    	{
    		ManhattanChip ch = c.chipAtIndex(ind);
    		int size = cellSize;
    		switch(ch.type) 
    		{
    		case Bomber: size=cellSize/2;
    			break;
    		default: size = cellSize*2/3;
    			break;
    		}
    		if(ch.drawChip(gc,this,size,offset,ypos,hp,c.rackLocation(),null))
    		{	if(move!=null && move.op==MOVE_SELECT)
    			{
    			move.to_index = move.from_index = ind;
    			hp.spriteRect = new Rectangle(offset-size/2,ypos-size,size,size*2);
    			}
    		}
    		offset += step;
    		ind++;
    	}}
    	
		if(selected)
			{ StockArt.Checkmark.drawChip(gc,this,cellSize/2,xpos,ypos,null); 
			}
		// damage the temp cell in case it got incorporated into any useful cells
		temp.reInit();
		return hit;
    	
    }
    public void drawPlayerCell(Graphics gc,HitPoint hp,ManhattanBoard gb,PlayerBoard pb,Hashtable<ManhattanCell, 
    		ManhattanMovespec> targets,ManhattanCell c,int cellSize, double xstep, double ystep, String hint,HitPoint hitAny)
    {
    	int xpos = pb.cellToX(c);
		int ypos = pb.cellToY(c);
		ManhattanMovespec move = targets.get(c);
		HitPoint ahp = move!=null ? hp : null;
		ManhattanId rack = c.rackLocation();
		boolean bombing =  (c.topChip()==null) && ((rack==ManhattanId.Fighters) || (rack==ManhattanId.Bombers));
		boolean see = eyeRect.isOnNow() || bombing;

		switch(c.type)
		{
		case Building:
		case Bomb:
			if(drawBuildingCell(gc,ahp,gb.isSelected(c),move,c,cellSize,xpos,ypos,xstep,ystep,hint,hitAny))
			{	
				hitAny.hitData = move;
			}
			break;
		case Help:
			if(gb.pickedObject==null) { ahp =  hitAny; }
			//$FALL-THROUGH$
		case Worker:
		default:
			if(drawDefaultCell(gc,ahp,gb.isSelected(c),c,cellSize,xpos,ypos,xstep,ystep,hint, hitAny))
			{
				ahp.hitData = move;
				if(c.type==Type.Worker)
				{
					move.from_index = ahp.hit_index;
				}
			}

		}
		boolean selectable = (ahp==null)
								&&(gb.pickedObject==null)
								&&((move==null || move.op!=MOVE_SELECT));
    	findBigChip(c,selectable,hitAny,cellSize,xpos,ypos,xstep,ystep,hint);
		if(c.benefit!=Benefit.Inspect 
				&& see
				&& !gb.getState().Puzzle()
				&& targets.get(c)!=null)
		{	
			StockArt.SmallO.drawChip(gc,this,cellSize*3,xpos,ypos,null);
			if(bombing)
			{
				StockArt.RedLight.drawChip(gc,this,cellSize/2,xpos,ypos,null);
			}
		}
		//StockArt.SmallO.drawChip(gc,this,cellSize,xpos,ypos,null);
    }
    public void drawBoardArray(Graphics gc,HitPoint hp,ManhattanBoard gb,Hashtable<ManhattanCell, ManhattanMovespec> targets,ManhattanCell cells[], int cellSize, double xstep, double ystep, 
    		String hint,HitPoint hitAny)
    {
      	for(ManhattanCell c : cells) 
    	{	drawBoardCell(gc,hp,gb,targets,c, cellSize,xstep, ystep, null,hitAny);
    	}
    }
    public void drawBoardCell(Graphics gc,HitPoint hp,ManhattanBoard gb,Hashtable<ManhattanCell, ManhattanMovespec> targets,ManhattanCell c,
    		int cellSize, double xstep, double ystep, String hint,HitPoint hitAny)
    {
      		int xpos = gb.cellToX(c);
    		int ypos = gb.cellToY(c);
    		drawBoardCell(gc,hp,gb,targets,c,cellSize,xpos,ypos,xstep,ystep,hint,hitAny);
    }
    
    public boolean drawBoardCell(Graphics gc,HitPoint hp,ManhattanBoard gb,Hashtable<ManhattanCell, ManhattanMovespec> targets,ManhattanCell c, 
    		int cellSize, int xpos,int ypos,double xstep, double ystep, String hint,HitPoint hitAny)
    {
    		ManhattanMovespec move = targets.get(c);
    		HitPoint ahp = move!=null ? hp : null;
    		boolean hit = false;
    		switch(c.type)
    		{
    		case Worker:
    			if(drawBuildingCell(gc,ahp,gb.isSelected(c),move,c,cellSize,xpos,ypos,xstep,ystep,hint,hitAny))
    			{
    				hp.hitData = move;
    				hit = true;
    			}
    			break;
    		case Help:
    			if(gb.pickedObject==null) { ahp =  hitAny; }
    			//$FALL-THROUGH$
    		default:
    			if(drawDefaultCell(gc,ahp,gb.isSelected(c),c,cellSize,xpos,ypos,xstep,ystep,hint, hitAny))
    			{	boolean top = (xstep==0 && ystep==0) || c.height()==0 || c.rackLocation()==ManhattanId.Bombtest;
    				if(move!=null)
    				{
    				move.from_index = top ? -1 : hitAny.hit_index;
        			move.to_index = top ? -1 : hitAny.hit_index+1;
        			ahp.hitData = move;
    				}
        			hit = true;
    			}
    		}
    		boolean selectable = (ahp==null)
					&&(gb.pickedObject==null)
					&&((move==null || move.op!=MOVE_SELECT));
			findBigChip(c,selectable,hitAny,cellSize,xpos,ypos,xstep,ystep,hint);
			if(c.benefit!=Benefit.Inspect 
				&& eyeRect.isOnNow() 
				&& !gb.getState().Puzzle()
				&& targets.get(c)!=null)
			{	
				StockArt.SmallO.drawChip(gc,this,cellSize*3,xpos,ypos,null);
			}
    		return hit;
    }
    
    public boolean drawBoardChip(Graphics gc,HitPoint hp,ManhattanChip c,ManhattanId code,int cellSize,int xpos,int ypos,String hint)
    {
    		if(c.drawChip(gc,this,cellSize,xpos,ypos, hp, code, hint,1,1))
    		{	
    			hp.spriteColor = Color.red;
    			hp.awidth = cellSize/2;
    			return true;
    		}
    		return false;
    }
    public boolean censor(PlayerBoard pb,HitPoint hp)
    {
    	return (hp==null);
    }

    public void drawPlayerBoard(Graphics gc,  HitPoint hp,ManhattanBoard gb,PlayerBoard pb,commonPlayer pl, Rectangle r, 
    			Hashtable<ManhattanCell,ManhattanMovespec>targets,HitPoint hitAny)
    {	
    	ManhattanChip back = pb.background;
    	pb.setRectangle(r);
    	int w = G.Width(r);
    	int cellSize = w/15;
    	Rectangle box = playerPersonality[pb.boardIndex];
       	Rectangle bombBox = playerBombDesigns[pb.boardIndex];

    	int bombsize = pb.setPosition(pb.stockpile,bombBox);
    	pb.setPosition(pb.personality,playerPersonality[pb.boardIndex],0,0);
    	int score = pb.calculateScore();
    	pb.chip.drawChip(gc,this,playerChip[pb.boardIndex],""+score);
    	if(G.debug()) { pb.positionCells(); }
    	
    	back.getImage().centerImage(gc,r);
 
    	drawPlayerCell(gc,hp,gb,pb,targets,pb.personality,(int)(cellSize*3.5),0,0,null,hitAny);
    	int step = G.Width(box)/3;
		int ystep = G.Height(box)/3;
		int left = G.Left(box)+step*1/3;
		int top =G.Top(box)+step/2;
    	if(pb.hasPersonality(ManhattanChip.Lemay))
    	{	
    		if(!pb.testOption(TurnOption.LemayFighter))
    		{	pb.fighter.drawChip(gc,this,step,left,top+ystep*2,null);
    		}
    		if(!pb.testOption(TurnOption.LemayBomber))
    		{   pb.bomber.drawChip(gc,this,step,left,top+step,null);
    		}
    		if(!pb.testOption(TurnOption.LemayAirstrike))
    		{   ManhattanChip.Damage.drawChip(gc,this,step,left,top,null);
    		}
 		
    	 }
    	else if(pb.hasPersonality(ManhattanChip.Nichols) 
    			&& !pb.testOption(TurnOption.NicholsShuffle))
    	{	
    		
    		ManhattanCell building = gb.seeBuilding[0];
    		if(building.height()>0)
    		{
    		ManhattanChip ch = building.chipAtIndex(0);
    		ch.drawChip(gc,this,step,left,top,null);
    		StockArt.SmallX.drawChip(gc,this,step,left,top,null);
    		}
    	}
    	else if(pb.hasPersonality(ManhattanChip.Fuchs))
    	{   if(!pb.testOption(TurnOption.FuchsEspionage))
    		{
    		
			ManhattanChip.Spy.drawChip(gc,this,step,left,top,hp,null,FuchsExplanation);
    		}
    	}
    	else if(pb.hasPersonality(ManhattanChip.Groves))
    	{   if(!pb.testOption(TurnOption.GrovesWorker))
    		{
    		
			pb.engineer.drawChip(gc,this,step,left,top,hp,null,GrovesWorkerExplanation);
    		}
    	}
    	else if(pb.hasPersonality(ManhattanChip.Oppenheimer))
    	{   
    		if(!pb.testOption(TurnOption.OppenheimerWorker))
			{
			
			pb.scientist.drawChip(gc,this,step,left,top,hp,null,OppenheimerWorkerExplanation);
			}
    	}
    	if(G.debug() || G.offline())
    	{
    		Toggle eyerect = playerEyes[pl.boardIndex];
    		if(eyerect.draw(gc,hitAny))
    		{
    			hitAny.hitData = eyerect;
    		}
    	}
    	drawPlayerCell(gc,hp,gb,pb,targets,pb.workers,(int)(cellSize*1.3), 0.4, 0.0, null,hitAny);
    	drawPlayerCell(gc,hp,gb,pb,targets,pb.scientists,(int)(cellSize*1.3), 0.4, 0.0, null,hitAny);
    	drawPlayerCell(gc,hp,gb,pb,targets,pb.engineers,(int)(cellSize*1.3), 0.4, 0.0, null,hitAny);
    	int nyellow = pb.yellowcakeDisplay.height();
    	double cakeStep = Math.max(-0.1,-1.2/nyellow);
    	drawPlayerCell(gc,hp,gb,pb,targets,pb.yellowcakeDisplay,cellSize*3/2, cakeStep,0, ""+(nyellow-pb.yellowcakeDisplay.activeAnimationHeight()),hitAny);	
    	drawPlayerArray(gc,hp,gb,pb,targets,pb.fighters, cellSize*3/2, 0.5, 0.5, null,hitAny);
    	drawPlayerArray(gc,hp,gb,pb,targets,pb.bombers, cellSize*3/2, 0.5, 0.5, null,hitAny);
    	drawPlayerArray(gc,hp,gb,pb,targets,pb.buildings, null, cellSize*3, 0.0, 0.0,null, hitAny);
    	double cashstep = Math.min(0.4,1.5/pb.cashDisplay.height());
    	drawPlayerCell(gc,hp,gb,pb,targets,pb.cashDisplay,cellSize*5/2,0.0,cashstep,null,hitAny);
    	HitPoint.setHelpText(hitAny,cellSize*3,pb.cellToX(pb.cashDisplay),pb.cellToY(pb.cashDisplay),
    			""+pb.cashDisplay.cash+"$");
 
    	Toggle seeBombs = playerEyes[pb.boardIndex];
    	String bombBacks = (pl!=getActivePlayer() && !mutable_game_record && !seeBombs.isOn()) 
    							? ManhattanChip.BACK 
    							: null;
    	int bsize = Math.min(bombsize,w/pb.stockpile.size());
    	
    	drawPlayerArray(gc,hp,gb,pb,targets,pb.stockpile,pb.selectedBomb,bsize,0.2,
    			0.2,bombBacks, hitAny);
    	
    	ManhattanCell selected = pb.selectedBomb;
    	if(selected!=null)
    	{	
    		ManhattanMovespec bomb = targets.get(selected);
    		boolean can = bomb!=null;
    		HitPoint ahp = can ? hp : null;
    		Rectangle cr = playerCard[pb.boardIndex];
    		int cansize = G.Width(cr);
    		int xp = G.centerX(cr);
    		int yp = G.centerY(cr);
    		if(drawBuildingCell(gc,ahp,false,null, selected,cansize,
    				xp,yp,0.2,0.2,
    				selected.type==Type.Bomb ? bombBacks : null,
    				hitAny))
    		{
    			hp.hitObject = selected;
    			hp.hitData = bomb;
    		}
    		boolean selectable = (ahp==null)
					&&(gb.pickedObject==null);
        	findBigChip(selected,selectable,hitAny,cansize,xp,yp,0,0,null);
    		GC.frameRect(gc,Color.black,cr);
    	}
    	drawPlayerCell(gc,hp,gb,pb,targets,pb.airstrikeHelp,cellSize,0.0,0.4, null,hitAny);
    	
    	zoomer.drawMagnifier(gc,hitAny,pl.playerBox,0.04,0.98,0.97,G.rotationQuarterTurns(pl.displayRotation));

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
       ManhattanBoard gb = disB(gc);
       ManhattanState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
       if(gb.whoseTurn!=lastSeenPlayer)
       {	// set the orientation for popups when the player changes.
    	   lastSeenPlayer = gb.whoseTurn;
    	   scaleOverlays = false;
    	   overlayOrientation = G.rotationQuarterTurns(getPlayerOrTemp(gb.whoseTurn).displayRotation);
       }
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

       gameLog.redrawGameLog2(gc, nonDragSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),standardBoldFont());

       if(ourTurnSelect!=null)
       {
    	   switch(state)
    	   {
    	   case Airstrike:	
    		   if(!helpOff.contains(ManhattanChip.AirstrikeHelp))
    		   {
    		   bigChip = ManhattanChip.AirstrikeHelp;
    		   }
    		   break; 
    	   case PlayLocal:
    		   if(!helpOff.contains(ManhattanChip.BombtestHelp)
    				   && bb.scoreForPlayer(bb.whoseTurn)>0)
    		   {   // first time they score
    			   bigChip = ManhattanChip.BombtestHelp;
    		   }
    		   break;
    		default:
    	   		   if(bigChip!=null && bigChip.type==Type.Help && !helpOff.contains(bigChip))
        		   {
        			   helpOff.push(bigChip);
        			   bigChip = null;
        		   }
    	   		   break;
    	   }
       }
       // this does most of the work, but other functions also use contextRotation to rotate
       // animations and sprites.
       GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);
       
       ManhattanCell deckOverlay = overlayDeck;
       boolean deckSelect = false;
       boolean choiceOverlay = false;
       boolean koreaOverlay = false;
       
       CellStack choices = gb.displayCells;
       switch(state)
       {
       case North_Korea_Dialog:
    	   koreaOverlay = true;
    	   break;
       case DiscardBombs:
       case ConfirmDiscard:
       case ResolvePayment:
       case ConfirmPayment:
       case CollectBenefit:
       case ConfirmBenefit:
       case ResolveChoice:
       case ConfirmChoice:
       case DiscardOneBomb:
       case SelectPersonality:
    	   	if(bb.displayCells.size()>0) { choiceOverlay = true; }
       		break;
       default: break;
       }
       boolean anyOverlay = overlayDeck!=null || choiceOverlay || koreaOverlay || bigChip!=null;
       
       if(gb.displayCell!=null) { deckOverlay = gb.displayCell; deckSelect=true; choiceOverlay = false; }
       HitPoint boardSelect = anyOverlay ? null :ourTurnSelect;

       Hashtable<ManhattanCell,ManhattanMovespec> targets = gb.getTargets();

       numberMenu.clearSequenceNumbers();

       drawBoardElements(gc, gb, boardRect, boardSelect,targets,anyOverlay ? null : selectPos);

       GC.unsetRotatedContext(gc,selectPos);
       
       Rectangle overlayRect = ((overlayOrientation&1)==0) 
   		   						? visualBoardRect
   		   						: visualBoardRectRotated;
       GC.setRotatedContext(gc,
    		   overlayRect,selectPos,Math.PI/2*overlayOrientation);
       scalableOverlay = false;
       if(choiceOverlay)
       	{  
    	   scalableOverlay = true;
    	   if(scaleOverlays)
    	   {
    		   drawChoiceOverlay(gc,gb,
    				   new Rectangle(G.Left(overlayRect),G.Top(overlayRect),G.Width(overlayRect)/5,G.Height(overlayRect)/5),
    				   targets,
       			   	choices,
       			   	null,selectPos
       			   );  
    	   }
    	   else
    	   {
    	   drawChoiceOverlay(gc,gb,overlayRect,targets,
    			   	choices,
    			   	ourTurnSelect,selectPos
    			   );
    	   }
       	}
       else if(koreaOverlay)
       {
       	   scalableOverlay = true;
       	   if(scaleOverlays)
    	   {drawKoreaOverlay(gc,gb,
				   new Rectangle(G.Left(overlayRect),G.Top(overlayRect),G.Width(overlayRect)/5,G.Height(overlayRect)/5),
				   targets,
   			   	null,selectPos
   			   ); 
       	}
       else 
		   {
    	   drawKoreaOverlay(gc,gb,overlayRect,targets,ourTurnSelect,selectPos);
		   }
       }
       else if(deckOverlay!=null)
       	{
    	drawDeckOverlay(gc,gb,overlayRect,deckOverlay,state.Puzzle()?ourTurnSelect:null,deckSelect,selectPos);   
       	}
       
       if(bigChip!=null)
       {
    	   drawBigChip(gc,gb,overlayRect,bigChip,selectPos);
       }
       
       GC.unsetRotatedContext(gc,selectPos);
       
       boolean planned = plannedSeating();
       int whoseTurn = gb.whoseTurn;
       for(int player=0;player<gb.players_in_game;player++)
       	{ commonPlayer pl = getPlayerOrTemp(player);
       	  PlayerBoard pb = gb.getPlayerBoard(player);
       	  pl.setRotatedContext(gc, selectPos,false);
       	  drawPlayerBoard(gc,ourTurnSelect,gb,pb,pl,playerBoards[player],  targets,selectPos);
    	   if(planned && whoseTurn==player)
    	   {
    		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
       	   pl.setRotatedContext(gc, selectPos,true);
       	}
       commonPlayer pl = getPlayerOrTemp(whoseTurn);
       double messageRotation = pl.messageRotation();
       
       if(bigChip==null && deckOverlay==null && !koreaOverlay && !choiceOverlay)
    	   { numberMenu.drawSequenceNumbers(gc,CELLSIZE*2,labelFont,labelColor);    	
    	   }
       
       GC.setFont(gc,standardBoldFont());
       
 
		if (state != ManhattanState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive())
				{
				doneButton.setOffText(bb.endTurnState()?s.get(EndTurnAction):s.get(DoneAction));
				doneButton.show(gc,messageRotation,gb.DoneState() ? buttonSelect : null);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
        }
		switch (state)
		{	
			case Puzzle:
			case Retrieve:
			case PlayOrRetrieve:
			case ConfirmRetrieve:
				if(buttonSelect!=null) 
					{ retrieveButton.highlightWhenIsOn = true;
					  retrieveButton.setIsOn(state==ManhattanState.ConfirmRetrieve);
					  retrieveButton.show(gc,buttonSelect); 
					}
				break;
			default:
		}

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==ManhattanState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        standardGameMessage(gc,messageRotation,
        					// note that gameOverMessage() is also put into the game record
            				state.GameOver()?gameOverMessage(gb):gb.getStateDescription(state),
            				!state.Puzzle(),
            				gb.whoseTurn,
            				stateRect);
        gb.getPlayerChip(gb.whoseTurn).drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(VictoryCondition,bb.winningScore()),progressRect, goalRect);
            //      DrawRepRect(gc,pl.displayRotation,Color.black,b.Digest(),repRect);
        eyeRect.activateOnMouse = true;
        eyeRect.draw(gc,selectPos);
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        
    }

    /**
     * normally, no moves should be transmitted during in-game review.  This
     * allows an override for particular moves. Presumably moves that only
     * affect the global state, not the particular board position.  
     */
    public boolean canSendAnyTime(commonMove m)
    {
    	return super.canSendAnyTime(m);
    			//|| (m.op==MOVE_SHOW);
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
    {	boolean sim = simultaneousTurnsAllowed();
    	boolean val = super.PerformAndTransmit(m,transmit,replay);
    	if(sim && !simultaneousTurnsAllowed())
    	{
    		canonicalizeHistory();
    	}
    	return val;
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
    {	
        handleExecute(bb,mm,replay);
        // movenumber() vs activeMoveNumber().  ActiveMoveNumber causes the 
        // move arrows to disappear when the next move has started.  For games with from-to or more
        // complicated moves, the persistence of the arrows is annoying.  for games like Go
        // and Hex, where the stones are placed and not moved, it's more innocuous
        numberMenu.recordSequenceNumber(bb.moveNumber());
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
	 case MOVE_DROP:
		 playASoundClip(light_drop,100);
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
        return (new ManhattanMovespec(st, pl));
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
    	  boolean oknone = (nmove.op==MOVE_DROP) || nmove.isEphemeral() || nmove.op==MOVE_CONTRIBUTE || nmove.op==MOVE_APPROVE;
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
     * this method is called from deep inside PerformAndTransmit, at the point
     * where the move has been executed and the history has been edited.  It's
     * purpose is to verify that the history accurately represents the current
     * state of the game, and that the fundamental game machinery is in a consistent
     * and reproducible state.  Basically, it works by creating a duplicate board
     * resetting it and feeding the duplicate the entire history, and then verifying 
     * that the duplicate is the same as the original board.  It's perfectly ok, during
     * debugging and development, to temporarily change this method into a no-op, but
     * be warned if you do this because it is throwing an error, there are other problems
     * that need to be fixed eventually.
     */
    public void verifyGameRecord()
    {	//DISABLE_VERIFY=true;
    	super.verifyGameRecord();
    }
 // for reference, here's the standard definition
 //   public void verifyGameRecord()
 //   {	BoardProtocol ourB =  getBoard();
 //   	int ourDig = ourB.Digest();
 //   	BoardProtocol dup = dupBoard = ourB.cloneBoard();
 //   	int dupDig = dup.Digest();
 //   	G.Assert(dupDig==ourDig,"Duplicate Digest Matches");
 //   	dup.doInit();
 //   	int step = History.size();
 //   	int limit = viewStep>=0 ? viewStep : step;
 //   	for(int i=0;i<limit;i++) 
 //   		{ commonMove mv = History.elementAt(i);
 //   		  //G.print(".. "+mv);
 //   		  dup.Execute(mv); 
 //   		}
 //   	int dupRedig = dup.Digest();
 //   	G.Assert(dup.whoseTurn()==ourB.whoseTurn(),"Replay whose turn matches");
 //   	G.Assert(dup.moveNumber()==ourB.moveNumber(),"Replay move number matches");
 //   	if(dupRedig!=ourDig)
 //   	{
 //   	//int d0 = ourB.Digest();
 //   	//int d1 = dup.Digest();
 //   	G.Assert(false,"Replay digest matches");
 //   	}
 //   	// note: can't quite do this because the timing of "SetDrawState" is wrong.  ourB
 //   	// may be a draw where dup is not if ourB is pending a draw.
 //   	//G.Assert(dup.getState()==ourB.getState(),"Replay state matches");
 //   	dupBoard = null;
 //   }
    
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
        if (hp.hitCode instanceof ManhattanId)// not dragging anything yet, so maybe start
        {
        ManhattanId hitObject =  (ManhattanId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    

        } 
        }
    }
	  public boolean allowOpponentUndoNow() 
	  {
		  return super.allowOpponentUndoNow();
	  }
	  public boolean allowOpponentUndo() 
	  {
		  return super.allowOpponentUndo();
	  }
	  
	  public boolean allowUndo()
	  {		return super.allowUndo();
	  }
	/**
	 * this is the key to limiting "runaway undo" in situations where the player
	 * might have made a lot of moves, and undo should limit the damage.  One
	 * example of this is in perliminary setup such as arimaa or iro
	 */
	public boolean allowPartialUndo()
	{
		return super.allowPartialUndo();
	}
	 /**
	  * this is called when the user clicks with no effect a few times, and is intended to 
	  * put him into an un-confused state.  Normally this is equivalient to an undo, but
	  * in games with complex setups, something else might be appropriate
	  */
	 public void performReset()
	    {
	    	super.performReset();
	    }
	 //
	 // reduce columns by enough to cause an additional row
	 //
	 private int columnDecrement(int count,int nrows,int ncols)
	 {
		 int leftover = ncols-count%ncols;
		 int v = Math.max(1,leftover/(nrows+1));
		 return v;
	 }
	 private int nrows(int count, int cols)
	 {
		 return (count/cols+(count%cols==0 ? 0 : 1));
	 }
	 public void drawChoiceOverlay(Graphics gc,ManhattanBoard gb,Rectangle rect,
			 		Hashtable<ManhattanCell,ManhattanMovespec>targets,
			 		CellStack choices,
			 		HitPoint hp,HitPoint hitAny)
	 {	int left = G.Left(rect);
		int top = G.Top(rect);
		int w = G.Width(rect);
		int h = G.Height(rect);
		int ah = h-h/5;

		int ncells = choices.size();
	 	Benefit benefit = gb.pendingBenefit;
	 	boolean trade =  benefit== Benefit.Trade;
	 	//ManhattanState state = gb.getState();
	 	boolean censor = ((benefit==Benefit.DiscardOneBomb) 
	 						|| (benefit==Benefit.DiscardBombs)
	 						|| (benefit==Benefit.BombDesign)) && hp==null;
		int nrows = trade ? 2 : 1;
	 	int ncols = trade 
	 			? 4 
	 			: Math.max(2,ncells);
		 
		int cellSize = (int)( w/(ncols+0.5));
		int ystep = cellSize*4/3;
		
		if(!trade)
		{
			while(ncols>2 && ystep*(nrows+1)<ah)
			{
				ncols--;
				nrows = nrows(ncells,ncols);
				cellSize =(int)( w/(ncols+0.5));
				ystep =cellSize*4/3;
			}
			if(ystep*(nrows)>ah)
			{
				ncols++;
				nrows = nrows(ncells,ncols);
				cellSize = (int)( w/(ncols+0.5));
				ystep =cellSize*4/3;
			}
			// keep the cell size constant, but equalize the length of the rows
			while(ncols>2 && nrows(ncells,ncols-1)==nrows)
				{ ncols--; }
		}
		
		double off = (ncols-1)/2.0;
		int x00 = left+w/2 - (int)(cellSize*off);
		int x0 = x00;
		int y0 = top+h/2 - (int)(((nrows/2.0)-0.5)*ystep);
		int scrimW = (int)(cellSize*(ncols+0.2));
		int scrimH = (int)(ystep*(nrows+0.2));
		int scrimLeft = x0-cellSize*2/3;
		int scrimTop =  y0+(int)(ystep*-0.6);
		StockArt.Scrim.getImage().drawImage(gc,scrimLeft,scrimTop,scrimW,scrimH);
	
		for(int i=0;i<ncells;i++)
		 {	ManhattanCell cell = choices.elementAt(i);
		 	double xs = 0.3/cell.height();
		 	double ys = 0.3/cell.height();
		 	int cs = cell.height()>0 && cell.chipAtIndex(0).type.isACard() ?  cellSize : cellSize*2/3 ;
			drawBoardCell(gc,hp,gb,targets,cell,cs,x0,y0,xs,ys,
					censor ? ManhattanChip.BACK:null,hitAny);

			x0 += cellSize; 
			
			if(trade && i+1<ncells)
			{	// look for a change to the second row
				ManhattanCell next = choices.elementAt(i+1);
				if(next.rackLocation()!=cell.rackLocation())
				{
					x0 = x00;
					y0 += ystep;
				}
			}
			else if((i+1)%ncols==0)
			{
				x0 = x00;
				y0 += ystep;
			}
		 }
		if(trade)
			{	// trade
				String give = s.get(TradeAwayMessage);
				String take = s.get(TradeGetMessage);
				GC.setFont(gc,largeBoldFont());
				GC.Text(gc,true,scrimLeft,scrimTop+(int)(scrimH*0.3),scrimW,scrimH/4,Color.black,null,give);
				GC.Text(gc,true,scrimLeft,scrimTop+(int)(scrimH*0.715),scrimW,scrimH/3,Color.black,null,take);
				
			}
		 drawRotators(gc,scrimLeft,scrimTop,scrimW,scrimH,hitAny);

		 GC.frameRect(gc,Color.black,scrimLeft,scrimTop,scrimW,scrimH);
	 }
 
	 public void drawKoreaOverlay(Graphics gc,ManhattanBoard gb,Rectangle rect,
		 		Hashtable<ManhattanCell,ManhattanMovespec>targets,
		 		HitPoint hp0,HitPoint hitAny)
{	
	int excludedPlayer = gb.northKoreanPlayer;
	boolean simultaneous = simultaneousTurnsAllowed();
	int approveOp = simultaneous ? EPHEMERAL_APPROVE : MOVE_APPROVE;
	int contributeOp = simultaneous ? EPHEMERAL_CONTRIBUTE : MOVE_CONTRIBUTE;
	boolean iAmExcluded = (getActivePlayer().boardIndex==excludedPlayer);
	HitPoint hp = iAmExcluded && simultaneousTurnsAllowed() ? null : hp0;
	int nrows = bb.players_in_game-1;
	int ncols = 6;
	int left = G.Left(rect);
	int top = G.Top(rect);
	int w = G.Width(rect);
	int h = G.Height(rect);
	 
	double off = (ncols-1)/2.0;
	int cellSize = (int)Math.min(h/(nrows+1.5),w/(ncols+1));
	int yspace = 4;
	int ystep = cellSize-yspace;
	int x00 = left+w/2 - (int)(cellSize*off);
	int y0 = top+h/2 - (int)((nrows/2.0)*ystep);
	int scrimW = (int)(cellSize*(ncols+0.3));
	int scrimH = (int)(ystep*(nrows+0.3)+ystep/2);
	int scrimLeft = x00-cellSize*2/3;
	int scrimTop =  y0-ystep*2/3;
	// if everyone else has approved, present the final checkbox to the korean player
	boolean allApproved = bb.allApprovedExcept();
	StockArt.Scrim.getImage().drawImage(gc,scrimLeft,scrimTop,scrimW,scrimH);
	int total = 0;
	commonPlayer us = getActivePlayer();
	for(int player=0;player<bb.players_in_game;player++)
	{	
		if(player!=excludedPlayer)
		{	HitPoint hitUs = mutable_game_record || player==us.boardIndex ? hp : null;
			PlayerBoard pb = gb.getPlayerBoard(player);
			int available = Math.min(3,pb.cashDisplay.cash);
			int x0 = x00;
			if(pb.chip.drawChip(gc,this,cellSize,x0,y0,hitUs,ManhattanId.Select,null))
				{
				hp.hitData = new ManhattanMovespec(approveOp,pb.color,pb.boardIndex);
				}
			if(pb.approvedNorthKorea)
			{
				StockArt.Checkmark.drawChip(gc,this,cellSize/2,x0,y0,null);
			}
			for(int i=0;i<=available;i++)
			{	int xpos = x0+(i+1)*cellSize;
				ManhattanChip c1 = null;
				DrawableImage<?> c2 = null;
				switch(i)
				{
				case 0:	c2 = StockArt.SmallX;
					//$FALL-THROUGH$
				case 1:	c1 = ManhattanChip.coin_1;
						break;
				case 3: c2 = ManhattanChip.coin_1;
					//$FALL-THROUGH$
				case 2: c1 = ManhattanChip.coin_2;
						break;
				default: break;
				}
				if(c1.drawChip(gc,this,cellSize,xpos,y0,i==pb.koreanContribution?null:hitUs,ManhattanId.Select,null))
					{
					hp.hitData = new ManhattanMovespec(contributeOp,pb.color,i,pb.boardIndex);
					}
				if(c2!=null)
				{
					c2.drawChip(gc,this,cellSize,xpos+cellSize/5,y0-cellSize/6,null);
				}
				if(i==pb.koreanContribution)
				{	total += i;
					StockArt.Checkmark.drawChip(gc,this,cellSize/2,xpos,y0,null);
				}
			}
			y0 += ystep;
		}
		drawRotators(gc,scrimLeft,scrimTop,scrimW,scrimH,hitAny);
	}

	GC.setFont(gc,largeBoldFont());
	GC.Text(gc,true,left,y0-ystep/2,w,ystep/2,Color.black,null,s.get("Total Contribution is $#1",total));
	
	if (allApproved)
	{	PlayerBoard pb = bb.getPlayerBoard(excludedPlayer);
		// when everyone else has approved, the korean's chip appears and they can click on it.
		y0 -= ystep/3;
		int x0 = x00+cellSize/2;
		if(pb.chip.drawChip(gc,this,cellSize,x0,y0,mutable_game_record || iAmExcluded ? hp0 : null,ManhattanId.Select,null))
		{
		hp0.hitData = new ManhattanMovespec(approveOp,pb.color,pb.boardIndex);
		}
	   if(pb.approvedNorthKorea)
	   {
		StockArt.Checkmark.drawChip(gc,this,cellSize/2,x0,y0,null);
	   }
	}
	 int dotSize = w/30;
	 GC.frameRect(gc,Color.black,scrimLeft,scrimTop,scrimW,scrimH);
	 StockArt.FancyCloseBox.drawChip(gc, this, dotSize*3/4, scrimLeft+scrimW-dotSize/2, scrimTop+dotSize/2, hp,ManhattanId.CloseOverlay,null);
}

	 private ChipStack helpOff = new ChipStack();
	 
	 ManhattanChip bigChip = null;
	 int overlayOrientation = 0;
	 int lastSeenPlayer = -1; 
	 boolean scaleOverlays = false;
	 boolean scalableOverlay = false;
	 public void drawBigChip(Graphics gc,ManhattanBoard gb,Rectangle rect,ManhattanChip ch,	HitPoint hp)
	 {	int w = G.Width(rect);
	 	int h = G.Height(rect);
	 	int cx = G.centerX(rect);
	 	int cy = G.centerY(rect);
	 	int sz = ch.type==Type.Help ? Math.min(w,h) : Math.min(w,h)*2/3;
	 	Image im = ch.getImage();
	 	double imw = im.getWidth();
	 	double imh = im.getHeight();
	 	int scrimW = (int)(sz*1.02);
	 	int scrimH = (int)(scrimW*imh/imw);
	 	int scrimLeft = cx-scrimW/2;
	 	int scrimTop = cy-scrimH/2;
	 	GC.fillRect(gc,Color.gray,scrimLeft,scrimTop,scrimW,scrimH);
	 	GC.frameRect(gc,Color.black,scrimLeft,scrimTop,scrimW,scrimH);
	 	if(ch.drawChip(gc,this,sz,cx,cy,hp,ManhattanId.ShowChip,null,1,1))
	 	{
	 		hp.hitData = ch;
	 	}
	 	drawRotators(gc,scrimLeft,scrimTop,scrimW,scrimH,hp);
	 }
	 
	 ManhattanCell overlayDeck = null;
	
	 public void drawDeckOverlay(Graphics gc,ManhattanBoard gb,Rectangle rect,ManhattanCell cell,
			 	HitPoint hp,boolean select,HitPoint hitAny)
	 {
		int left = G.Left(rect);
		int top = G.Top(rect);
		int w = G.Width(rect);
		int h = G.Height(rect);
		int ncells = cell.height();
	 	int ncols = Math.max(3,ncells);
		int nrows = 1;
		boolean personalities = cell.rackLocation()==ManhattanId.SeePersonalityPile;
		double aspect = 1.4;
		 int cellSize = (int)(w/(ncols+0.5));
		 int ystep = (int)(cellSize*aspect);
		 // increase the number of rows until the new number of rows
		 // exceeds the vertical space
		 while(ncols>1 && ystep*(nrows+1)<h)
		 {
			 ncols -= personalities ? 2 : columnDecrement(ncells,nrows,ncols);
			 nrows = nrows(ncells,ncols);
			 cellSize =  w/(ncols+1);
			 ystep = (int)(cellSize*aspect);
		 }
		 // we oversteped so step back by 1
		 if((nrows+0.1)*ystep>h)
		 {	// overshot
			 ncols += personalities ? 2 : 1;
			 nrows = nrows(ncells,ncols-(personalities ? 2 : 1));
			 cellSize =  w/(ncols+1);
			 ystep = (int)(cellSize*aspect);
		 }
		 
		 // keep the cell size constant, but equalize the length of the rows
		 while(ncols>2 && nrows(ncells,ncols-(personalities ? 2 : 1))==nrows)
				{ ncols -= personalities ? 2 : 1; }

		 double off = (ncols-1)/2.0;
		 
		 int fh = standardFontSize();
		 int x00 = left+w/2 - (int)(cellSize*off);
		 int x0 = x00;
		 int y0 = top+h/2 - (int)((nrows/2.0-0.5)*ystep);
		 int scrimW = (int)(cellSize*(ncols+0.3));
		 int scrimH = (int)(ystep*(nrows+0.3));
		 int scrimLeft = x0-cellSize*2/3;
		 int scrimTop =  y0-ystep*2/3;
		 boolean picked = gb.pickedObject!=null;
		 StockArt.Scrim.getImage().drawImage(gc,scrimLeft,scrimTop,scrimW,scrimH);
		 
		 for(int i=0;i<ncells;i++)
		 {	 ManhattanChip chip = cell.chipAtIndex(i);
			 if(drawBoardChip(gc,hp,chip,ManhattanId.HitCard,cellSize,x0,y0,null))
			 {	hp.hitObject = cell;
				hp.hit_index = i;
				hp.hitData = new ManhattanMovespec(select ? MOVE_SELECT : picked ? MOVE_DROP : MOVE_PICK,cell,i,gb.whoseTurn);
			 }
			 if(gb.isSelected(chip))
			 {
				 StockArt.Exmark.drawChip(gc,this,cellSize/2,x0,y0,null);
			 }
			 if(personalities && (chip.type==Type.Personalities))
			 {	boolean has = false;
			 	for(PlayerBoard pb : gb.pbs) { has |= pb.hasPersonality(chip); }
			 	if(!has)
			 	{	GC.setFont(gc,standardBoldFont());
			 	GC.Text(gc,true,x0-cellSize/2,y0-(int)(cellSize*0.73),cellSize,fh*3/2,Color.black,null,s.get(AvailableMessage));
			 	//GC.frameRect(gc,Color.black,x0-cellSize/2,y0-(int)(cellSize*0.73),cellSize,fh*3/2);
			 	}
			 }
			 // select an enlarged card if pointing at the bottom of the card
			 int xp = x0-cellSize*2/5;
			 int yp = y0+cellSize/3;
			 Rectangle r = new Rectangle(xp,yp,cellSize*4/5,cellSize/3);
				//GC.frameRect(gc,Color.blue,r);
				if(G.pointInRect(hitAny,r))
				{
					hitAny.spriteRect = r;
					hitAny.spriteColor = Color.red;
					hitAny.hitCode = ManhattanId.ShowChip;
					hitAny.hitObject = null;
					hitAny.hitData = chip;
				}
			 if((i+1)%ncols==0) { x0 = x00; y0 += ystep; }
			 else { x0 += cellSize; }
		 }
		 
		 drawRotators(gc,scrimLeft,scrimTop,scrimW,scrimH,hitAny);

		 GC.frameRect(gc,Color.black,scrimLeft,scrimTop,scrimW,scrimH);	 
		 

		 if(hitAny!=null && hitAny.hitCode==DefaultId.HitNoWhere )
		 {	hitAny.hitObject = cell;
		 	hitAny.hitCode = ManhattanId.CloseOverlay;
		 }
	 }
	 
	 // draw rotation arrows and close box for displays.
	 // this also triggers re-expansion of non-closable overlays that were scaled to get them out of the way.
	 //
	 private void drawRotators(Graphics gc,int scrimLeft,int scrimTop,int scrimW,int scrimH,HitPoint hitAny)
	 {	if(scaleOverlays)
	 	{
		 if(HitPoint.setHelpText(hitAny,scrimLeft,scrimTop,scrimW,scrimH,
				 ReexpandMessage))
		 	{
			 hitAny.hitCode = ManhattanId.CloseOverlay;
		 	}
	 	}
	 else
	 {
	 	int sz = Math.max(scrimH,scrimW)/20;
	 	if(plannedSeating())
		 {	
			 
			 ManhattanChip.RotateCCW.drawChip(gc,this,new Rectangle(scrimLeft,scrimTop,sz,sz),
					 hitAny,ManhattanId.RotateCW,RotateCCWMessage,1.2);
			 ManhattanChip.RotateCW.drawChip(gc,this,new Rectangle(scrimLeft+sz,scrimTop,sz,sz),
					 hitAny,ManhattanId.RotateCW,RotateCWMessage,1.2);
		 }
		 StockArt.FancyCloseBox.drawChip(gc, this, sz, scrimLeft+scrimW-sz/2, scrimTop+sz/2, hitAny,ManhattanId.CloseOverlay,null);
	 }
	 }

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
       	if(!(id instanceof ManhattanId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
       	else if(missedOneClick && id==DefaultId.HitNoWhere) { performReset(); }
        else {
        missedOneClick = false;
        ManhattanId hitCode = (ManhattanId)id;
        
        // if direct drawing, hp.hitObject is a cell from a copy of the board
        ManhattanCell hitObject = bb.getCell(hitCell(hp));
        switch (hitCode)
        {
        case CloseOverlay:
        	bigChip = null;
        	overlayDeck = null;
        	if(scalableOverlay) { scaleOverlays = !scaleOverlays; }
        	break;
        case ShowChip:
        	{
        	ManhattanChip ch =  (ManhattanChip)hp.hitData;
         	if(bigChip!=null) 
        		{	if(bigChip.type==Type.Help) { helpOff.pushNew(bigChip); }
        		}
        	bigChip = bigChip==ch ? null : ch;
        	if(hitObject!=null)
        	{
        		PlayerBoard pb = bb.getPlayerBoard(hitObject.color);
        		if(pb!=null)
        		{
        			pb.selectedBomb = hitObject;
        			saveDisplayBoard();
        		}
        	}
        	}
        	break;
        case HitRetrieve:
        	PerformAndTransmit("Retrieve "+bb.getCurrentPlayerBoard().color);
        	break;
        case BombHelp:
        	helpOff.pushNew(ManhattanChip.BombtestHelp);
        	bigChip = ManhattanChip.BombtestHelp;
        	break;
        case AirstrikeHelp:
        	helpOff.pushNew(ManhattanChip.AirstrikeHelp);
        	bigChip = ManhattanChip.AirstrikeHelp;
        	break;
        case RotateCW:
        	overlayOrientation = (overlayOrientation+1)&3;
        	break;
        case RotateCCW:
           	overlayOrientation = (overlayOrientation+3)&3;      	
        	break;
        case SeeBombs:
        	{
        		Toggle o = (Toggle)hp.hitData;
        		o.toggle();
        	}
        	break;
        case SeeBuildingPile:
        case CurrentDesigns:
        case SeePersonalityPile:
        case SeeNationsPile:
        case SeeBombPile:
        	if((overlayDeck==null) && (bb.pickedObject==null))
        	{
        		overlayDeck = hitObject;
        		break;
        	}		
			//$FALL-THROUGH$
		default:
        	if (performStandardButtons(hitCode, hp)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
         	else if(hp.hitData!=null)
        	{	ManhattanMovespec m = (ManhattanMovespec)hp.hitData;
        		//if(m.op==MOVE_SELECT) { m.to_index = m.from_index = hp.hit_index; }
        		PerformAndTransmit(m.longMoveString());
        	}
        	else if(hitObject==null) { G.Error("should have hit something"); }
            else if(hitObject.onBoard)
            {	G.Error("should have generated a move %s", hitCode);
               /*
            	if(bb.pickedObject==null)
            	{	if(hitObject.topChip()!=null)
            		{
            		PerformAndTransmit(G.concat("pickb ",hitCode," ",hitObject.row," ",hp.hit_index));
            		}
            	}
            	else
            	{
            	PerformAndTransmit(G.concat("dropb ",hitCode," ",hitObject.row," ",hp.hit_index));
            	}
            }
            else {
            	if(bb.pickedObject==null)
            	{
            		PerformAndTransmit(G.concat("pick ",hitCode," ",hitObject.color," ",hitObject.row," ",hp.hit_index));
            	}
            	else
            	{
            	PerformAndTransmit(G.concat("drop ",hitCode," ",hitObject.color," ",hitObject.row," ",hp.hit_index));
            	}
            	*/
            }
        	break;

        case ToggleEye:
        	eyeRect.toggle();
        	break;

 
        }
        }
    }



    private boolean setDisplayParameters(ManhattanBoard gb,Rectangle r)
    {
      	boolean complete = false;
      	gb.SetDisplayRectangle(r);
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
    public String sgfGameType() { return(Manhattan_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int np = G.IntToken(his);	// players always 2
    	long rv = G.IntToken(his);
    	int rev = G.IntToken(his);	// rev does't get used either
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // int rk = G.IntToken(his);
    	// bb.doInit(token,rk);
        bb.doInit(token,rv,np,rev);
        adjustPlayers(np);
        bb.parseOptions(his);

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
/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically.
 * <p>
 * This is a good place to make notes about threads.  Threads in Java are
 * very dangerous and tend to lead to all kinds of undesirable and/or flakey
 * behavior.  The fundamental problem is that there are three or four sources
 * of events from different system-provided threads, and unless you are very
 * careful, these threads will all try to use and modify the same data
 * structures at the same time.   Java "synchronized" declarations are
 * hard to get right, resulting in synchronization locks, or lack of
 * synchronization where it is really needed.
 * <p>
 * This toolkit addresses this problem by adopting the "one thread" model,
 * and this is where it is.  Any other threads should do as little as possible,
 * mainly leave breadcrumbs that will be picked up by this thread.
 * <p>
 * In particular:
 * GUI events do not respond in the native thread.  Mouse movement and button
 * events are noted for later.  Requests to repaint the canvas are recorded but
 * not acted upon.
 * Network I/O events, merely queue the data for delivery later.
 * 
 * Another thing ViewerRun typically does is provide "auto-move" for when
 * conditions are right - typically at the end of a simultaneous movement
 * phase.  Be careful, this interacts with "undo".  Typically solved by
 * extending allowBackwardStep() and allowPartialUndo() to go back a step further
 *  */
    commonPlayer asyncRobot = null;
    public commonPlayer currentRobotPlayer()
    {	if(asyncRobot!=null) 
    		{ if(simultaneousTurnsAllowed()) { return asyncRobot; }
    		  asyncRobot = null; 
    		}
    	return super.currentRobotPlayer();
    }
    public void ViewerRun(int wait)
       {   if(simultaneousTurnsAllowed())
    	   {
    	   if(allRobotsIdle()) { asyncRobot = null; }
    	   if(asyncRobot!=null)
    	   {
    		   PlayerBoard pb = bb.getPlayerBoard(asyncRobot.boardIndex);
    		   if(pb.approvedNorthKorea) { asyncRobot = null; }
    	   }
    	   if(asyncRobot==null)
    		{ 	// as of now, this is only in the korean dialog
    		   for(commonPlayer pp : players)
    		   	{ 	PlayerBoard pb = bb.getPlayerBoard(pp.boardIndex);
    		   		if(!pb.approvedNorthKorea)
    		   		{	if((pb.boardIndex!=bb.northKoreanPlayer) || bb.allApprovedExcept())
    		   				{ asyncRobot = pp;
    		   				  startRobotTurn(pp);
    		   				}
    		   		}
    		   	}
    		}}
           super.ViewerRun(wait);
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
    {  return(new ManhattanPlay());
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
     * the most common solutions are embodied in cost for move specs, states, and
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
    boolean SIMULTANEOUS_PLAY = true;		
    /** true if this game ever has simultaneous moves.  If true, formEphemeralHistoryString
     * will do a second pass to collect the ephemeral moves, and {@link #useEphemeraBuffer} will
     * expect there to be one.
     */
    public boolean gameHasEphemeralMoves() { return(SIMULTANEOUS_PLAY); }
    // these related methods can be wrapped or overridden to customize the behavior of the ephemeral part game records.
    //
    // public String formEphemeralHistoryString()
    // public void useEphemeraBuffer(StringTokenizer h)
    // public String formEphemeralMoveString() {} 
    // public void useEphemeralMoves(StringTokenizer his) {}
    // -- the top level --
    // public void useStoryBuffer(String tok,StringTokenizer his) {}
    // public void formHistoryString(PrintStream os,boolean includeTimes) {}

    /**
     * call this at appropriate times to convert ephemeral moves to their
     * non ephemeral equivalents.  Usually, only the {@link #convertToSynchronous }
     */
    public void canonicalizeHistory()
    {
    	super.canonicalizeHistory();
    }
    /**
     * sort the ephemeral moves into their final order.  Normally is is
     * just ordering the moves so all of each players moves are together.
     * 
     * @param ephemera
     */
    public void ephemeralSort(CommonMoveStack ephemera)
    {	int korean = bb.northKoreanPlayer;
    	for(int i=0,siz=ephemera.size();i<siz;i++)
    	{
    	commonMove m = ephemera.elementAt(i);
    	int player = m.player;
    	// there ought to be just one "approve" move from the north korean player
    	// make sure this final approval is last.
    	m.setEvaluation(((player==korean ? 100:player)*1000+m.index()));	// set evaluation for the sort
    	}
    	ephemera.sort(false);
    }
    /**
     * convert an ephemeral move to it's no-ephemeral equivalent.  It's also
     * ok to return null meaning the move should be deleted.  Normally, all
     * this will do is change the m.op field, but it needs to agree with 
     * the behavior of movespec {@link commonMove#isEphemeral} method.
     */
    public commonMove convertToSynchronous(commonMove m)
    {	switch(m.op)
    	{
    	case EPHEMERAL_APPROVE: m.op=MOVE_APPROVE; break;
    	case EPHEMERAL_CONTRIBUTE: m.op=MOVE_CONTRIBUTE; break;
    	default: G.Error("Not expecting %s",m);
    	}
    	return m;
    }
    
    // used by the UI to alter the behavior of clocks and prompts
    public boolean simultaneousTurnsAllowed()
    {	return super.simultaneousTurnsAllowed();
    }
  // public RecordingStrategy gameRecordingMode()
  //  {	return(super.gameRecordingMode());
  //  }
    private String vprogressString()
    {	return super.gameProgressString()+" score score";
    }
    public String gameProgressString()
    {	// this is what the standard method does
    	 return(mutable_game_record 
    			? Reviewing
    			: vprogressString());
    }
    // if there are simultaneous turns, robot start/stop can be tricky
    // by default, not allowed in simultaneous phases.  Return true 
    // to let them run "in their normal turn", but this will not allow
    // the robots to start at the beginning of the async phase.
    public boolean allowRobotsToRun() {
    	return super.allowRobotsToRun();
    }
    //
    // support for the last move "numberMenu" logic
    //
	public int getLastPlacement(boolean empty) {
		return (bb.moveNumber);
	}
	
	  public double imageSize(ImageStack im)
	  {
		  return(super.imageSize(im) + ManhattanChip.imageSize(im));
	  }
	  
	   private Text[] gameMoveText = null;
	   private Text[] gameMoveText()
	    {  
	    	if(gameMoveText==null)
	    	{
	    	double[] escale = {1,1.5,.3,-0.4};
	    	double[] wscale = {1,1.5,.3,-0.4};
	    	double[] sscale = {1,1.5,.3,-0.4};
	    	double[] xscale = {1,2,0.0,-0.4};
	    	double[] cscale = {1,2,0.0,-0.4};
	    	double[] c5scale = {1,1.8,0.0,-0.4};
	    	TextStack texts = new TextStack();
	    	for(ManhattanChip chip : ManhattanChip.playerChips)
	    	{
	    		texts.push(TextGlyph.create(""+chip.color+"-chip","xx",chip,this,escale));
	    	}
	    	for(ManhattanChip worker : ManhattanChip.engineers)
	    	{
	    		texts.push(TextGlyph.create(""+worker.color+"-"+worker.workerType,"xx",worker,this,escale));
	    		
	    	}
	    	for(ManhattanChip worker : ManhattanChip.laborers)
	    	{
	    		texts.push(TextGlyph.create(""+worker.color+"-"+worker.workerType,"xx",worker,this,wscale));
	    		
	    	}
	    	for(ManhattanChip worker : ManhattanChip.scientists)
	    	{
	    		texts.push(TextGlyph.create(""+worker.color+"-"+worker.workerType,"xx",worker,this,sscale));	
	    	}
	    	for(ManhattanChip ch : ManhattanChip.playerFighters)
	    	{
	    		texts.push(TextGlyph.create("fighter-"+ch.color,"xx",ch,this,sscale));
	    	}
	    	for(ManhattanChip ch : ManhattanChip.playerBombers)
	    	{
	    		texts.push(TextGlyph.create("bomber-"+ch.color,"xx",ch,this,cscale));
	    	}

	    	texts.push(TextGlyph.create("1$","xx",ManhattanChip.coin_1,this,cscale));
	    	texts.push(TextGlyph.create("2$","xx",ManhattanChip.coin_2,this,cscale));
	    	texts.push(TextGlyph.create("5$","xx",ManhattanChip.coin_5,this,c5scale));
	    	texts.push(TextGlyph.create("uranium","xx",ManhattanChip.Uranium,this,xscale));
	    	texts.push(TextGlyph.create("plutonium","xx",ManhattanChip.Plutonium,this,xscale));
	    	texts.push(TextGlyph.create("yellowcake","xx",ManhattanChip.Yellowcake,this,xscale));
	    	
	    	
	    	gameMoveText = texts.toArray();
	    	}
	    	return gameMoveText;
	    }
	    public Text colorize(String str)
	    {
	    	return TextChunk.colorize(str,null,gameMoveText());
	    }
	    public void testSwitch()
	    {	Graphics.preferStd = !Graphics.preferStd;
	    }
	    public boolean mandatoryDoneState()
	    {
	    	return bb.DoneState() && !bb.optionalDoneState();
	    }
	    public boolean autoDoneActive()
	    {
	    	return super.autoDoneActive() && !bb.optionalDoneState();
	    }
}

