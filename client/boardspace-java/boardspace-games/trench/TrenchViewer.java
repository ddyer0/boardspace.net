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
package trench;



import static trench.Trenchmovespec.*;

import online.common.*;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;

import bridge.JMenuItem;
import common.GameInfo;
import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.StockArt;
import lib.TextButton;
import lib.Toggle;
import lib.LFrameProtocol;
import lib.Image;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;


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
public class TrenchViewer extends CCanvas<TrenchCell,TrenchBoard> implements TrenchConstants, PlacementProvider
{		// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
    static final String Trench_SGF = "trench"; // sgf game name

    // file names for jpeg images and masks
    static final String ImageDir = "/trench/images/";

     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(255,230,230);
    private Color rackBackGroundColor = new Color(225,192,182);
    private Color rackIdleColor = new Color(205,172,162);
    private Color boardBackgroundColor = new Color(220,165,155);
    

     
    // private state
    private TrenchBoard bb = null; //the board from which we are displaying
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
    private JMenuItem offerDrawAction = null;

    private Toggle eyeRect = new Toggle(this,"eye",
 			StockArt.NoEye,TrenchId.ToggleEye,NoeyeExplanation,
 			StockArt.Eye,TrenchId.ToggleEye,EyeExplanation
 			);
    
    private Toggle arrowRect = new Toggle(this,"arrow",
 			TrenchChip.noarrow,TrenchId.ToggleArrow,NoArrowExplanation,
 			TrenchChip.arrow,TrenchId.ToggleArrow,ArrowExplanation
 			);

    
    private Rectangle chipRects[] = addZoneRect("chip",2);
    private Rectangle capRects[] = addZoneRect("capture",2);
    
 	private TextButton doneButton = addButton(DoneAction,GameId.HitDoneButton,ExplainDone,
			HighlightColor, rackBackGroundColor,rackIdleColor);
 	private Rectangle reverseViewRect = addRect("reverse");
 	private Rectangle repRect = addRect("repetitions");
    private Rectangle declineDrawRect = addRect("declineDraw");
    private Rectangle acceptDrawRect = addRect("acceptDraw");	

/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	TrenchChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = TrenchChip.Icon.image;
    }

    /**
     * this is the hook for substituting alternate tile sets.  This is called at a low level
     * from drawChip, and the result is passed to the chip's getAltChip method to substitute
     * a different chip.
     */
    public int getAltChipset() { return(usePerspective()?0:1); }
    
 
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	int players_in_game = info.getInt(OnlineConstants.PLAYERS_IN_GAME,chipRects.length);
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
        
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	TrenchConstants.putStrings();
        }
         
        
        String type = info.getString(GameInfo.GAMETYPE, TrenchVariation.trench.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new TrenchBoard(type,players_in_game,randomKey,getStartingColorMap(),TrenchBoard.REVISION);
        //
        // this gets the best results on android, but requires some extra care in
        // the user interface and in the board's copyBoard operation.
        // in the user interface.
        useDirectDrawing(true);
        doInit(false);
        offerDrawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);     
        
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
    boolean traditionalLayout = false;
    public void setLocalBounds(int x, int y, int width, int height)
    {	
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*15;	
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
    			fh*4,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);

    	layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);

    	// games which have a private "done" button for each player don't need a public
    	// done button, and also we can make the edit/undo button square so it can rotate
    	// to face the player.
     	layout.placeTheVcr(this,minLogW,minLogW*3/2);
      	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneButton,editRect,repRect);
      	//layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	int nrows = 15;  // b.boardRows
        int ncols = 15;	 // b.boardColumns
  	
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
        int stateH = fh*5/2;
        int stateY = boardY+stateH/3;
        int stateX = boardX;
        placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,eyeRect,arrowRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
      	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW,stateH,goalRect,reverseViewRect,viewsetRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
 	
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle cap = capRects[player];
    	int chipw = unitsize*3/2;
    	G.SetRect(chip,	x,	y,	chipw,	chipw);
    	Rectangle box =  pl.createRectangularPictureGroup(x+chipw,y,2*unitsize/3);
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*3 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.SetRect(cap,x,G.Bottom(box),G.Width(box)+chipw,(int)(unitsize*1.8));
    	G.union(box, done,chip,cap);
    	pl.displayRotation = rotation;
    	return(box);
    }

	// draw a box of spare chips. For pushfight it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, Rectangle cr, commonPlayer pl, HitPoint highlight,TrenchBoard gb)
    {	int player = pl.boardIndex;
    	int other = nextPlayer[player];
    	gb.getPlayerChip(player).drawChip(gc,this,G.Width(r)/2,G.centerX(r),G.centerY(r),null);
        TrenchCell cap = gb.captured(other);
        double cells = (double)G.Width(cr)/(CELLSIZE*3/4);
        boolean canHit = gb.legalToHitChips(other);
        int size = CELLSIZE*2/3;
        boolean hit = false;
        double space = Math.min(cells/cap.height(),1.7);
        if(cap.drawStack(gc,this,canHit?highlight:null,
        		size,G.Left(cr)+CELLSIZE/2,G.Top(cr)+(int)(G.Height(cr)*0.45),
        		0,space,0,null))
        {
        	highlight.spriteColor = Color.red;
        	int index = Math.max(0,highlight.hit_index);
         	highlight.spriteRect = new Rectangle(G.Left(cr)+(int)(index*space*size)+size/4,
        										 G.centerY(cr)-CELLSIZE/2,
        										 CELLSIZE,CELLSIZE);
         	hit = true;
        }
        if(canHit && !hit && gb.pickedObject!=null && G.pointInRect(highlight,cr))
        {
        	highlight.hitCode = cap.rackLocation();
        	highlight.hit_index = -1;
        	
        }
        int hh = G.Height(cr)/3;
        GC.setFont(gc,largeBoldFont());
        GC.Text(gc,false,G.Left(cr)+hh,G.Bottom(cr)-hh,G.Width(cr),hh,
        		Color.black,null,
        		s.get(PointsMessage,gb.totalCaptured(nextPlayer[player])));
        GC.frameRect(gc,Color.black,cr);
       // gb.captured[player];
  
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
    	int size = G.pointInRect(xp,yp,boardRect)
    				? scaledCellSize(boardRect,G.Bottom(boardRect)-yp)
    				: CELLSIZE;
     	TrenchChip.getChip(obj).drawChip(g,this,size, xp, yp, null);
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
     TrenchChip.backgroundTile.image.tileImage(gc, fullRect);   
      drawFixedBoard(gc);
     }
    
    Image background = null;
    Image prevBoard = null;
    // land here after rotating the board drawing context if appropriate
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	TrenchBoard gb = disB(gc);
        boolean reviewBackground = reviewMode()&&!mutable_game_record;
        if(reviewBackground)
        {	 
         TrenchChip.backgroundReviewTile.image.tileImage(gc,brect);   
        }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(gb,brect);
	      // if the board is one large graphic, for which the visual target points
	      // are carefully matched with the abstract grid
	  	  Image board =  (usePerspective() 
	  			  			? reverseView() 
	  			  					? TrenchChip.board_reverse.getImage() 
	  			  					: TrenchChip.board.getImage() 
	  			  			: reverseView() 
	  			  				? TrenchChip.pboard_reverse.getImage()
	  			  				: TrenchChip.pboard.image);
	  	  if(board!=prevBoard) { background = null; }
	  	  prevBoard = board;
	  	  background = board.centerScaledImage(gc,brect,background);

	      // draw a picture of the board. In this version we actually draw just the grid
	      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
	      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
	      // on the board to fine tune the exact positions of the text
	      gb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

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
    private int scaledCellSize(Rectangle brect,int ypos0)
    {
     	boolean perspective = !usePerspective();
     	double hscale = G.Height(brect)*7;
        double scale = perspective
				? 1.05-(ypos0/hscale) 
				: 1;
        int size = (int)(CELLSIZE*scale);
        return size;
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
    public void drawBoardElements(Graphics gc, TrenchBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
    	Hashtable<TrenchCell,Trenchmovespec> targets = gb.getTargets();
     	numberMenu.clearSequenceNumbers();
     	Enumeration<TrenchCell> cells = gb.getIterator(Itype.TBRL);
     	boolean show = eyeRect.isOnNow();
     	boolean showArrows = arrowRect.isOnNow();
     	//int seq = 0;
    	while(cells.hasMoreElements())
          {	TrenchCell cell = cells.nextElement();
          	int ypos0 =  gb.cellToY(cell);
         	int ypos = G.Bottom(brect) - ypos0;
            int xpos = G.Left(brect) + gb.cellToX(cell);
            numberMenu.saveSequenceNumber(cell,xpos,ypos);
            boolean canHit = gb.legalToHitBoard(cell,targets);
            //seq++;
            String msg = null;//""+seq;
            int size = scaledCellSize(brect,ypos0);
            if(cell.drawStack(gc,this,canHit?highlight:null,size,xpos,ypos,0,1,1,msg))
            		{
            		highlight.spriteColor = Color.red;
                	highlight.awidth = size;
            		}
            if(showArrows)
            {
            	TrenchChip top = cell.topChip();
            	if(top!=null)
            	{	boolean rev = gb.reverseY();
            		int chipset = getAltChipset();
            		TrenchChip arrow = top.getAltChip(2+chipset);
            		boolean perspective = chipset==1;
            		if(rev)
            		{
                		switch(top.type)
            			{
            			case m4:
	        				{
	        				// ad hoc rotation and reposioning of the arrows so the reversed positions
	        				// work correctly
	        				boolean white = top.color==TrenchId.White;
	        				int dx = perspective ? -size/3 : white ? -(int)(size*0.15) : (int)(size*0.1);
	        				int dy = perspective ? size/2 : white ? -(int)(size*0.2) : 0;
	        				GC.translate(gc,xpos,ypos);
	        				GC.setRotation(gc,Math.PI);
	        				arrow.drawChip(gc,this,size,dx,dy,null);
	        				GC.setRotation(gc,-Math.PI);
	        				GC.translate(gc,-xpos,-ypos);
	        				}
	        				break;
            			case m2:
            				{
            				// ad hoc rotation and reposioning of the arrows so the reversed positions
    	        			// work correctly
             				int dx = (int)(perspective ? -(0.05*size) : -0.2*size);
            				int dy = (int)(perspective ? (0.6*size) : -0.1*size);
            				GC.translate(gc,xpos,ypos);
            				GC.setRotation(gc,Math.PI);
            				arrow.drawChip(gc,this,size,dx,dy,null);
            				GC.setRotation(gc,-Math.PI);
            				GC.translate(gc,-xpos,-ypos);
            				}
            				break;
            			default:
            				arrow.drawChip(gc,this,size,xpos,ypos,null);
            			}
            		}
            		else
            		{
                		arrow.drawChip(gc,this,size,xpos,ypos,null);
            		}
            	}
            }
            if(canHit && show) { StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null); }
            if((cell.topChip()==null)
        			&& cell.lastContents!=null 
        			&& cell.lastCaptured>0
        			&& numberMenu.getVisibleNumber(cell.lastCaptured)>0)
                	{
                		cell.lastContents.drawChip(gc,this,size*2/3,xpos,ypos,null);
                		StockArt.SmallX.drawChip(gc,this,size,xpos,ypos,null);
                	}
           // if(cell.visibleFromTrench>0)
           // {
           // 	StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,""+cell.visibleFromTrench);
           // }
           //visualize the grid
           //StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
        }
    	numberMenu.drawSequenceNumbers(gc,CELLSIZE*2/3,labelFont,labelColor);
    }
    public boolean usePerspective() { return(super.getAltChipset()!=0);}

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
       TrenchBoard gb = disB(gc);
       TrenchState state = gb.getState();
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

       // for a multiplayer game, this would likely be redrawGameLog2
       gameLog.redrawGameLog(gc, nonDragSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),standardBoldFont());

       // this does most of the work, but other functions also use contextRotation to rotate
       // animations and sprites.
       GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect);
       GC.unsetRotatedContext(gc,selectPos);
       
       boolean planned = plannedSeating();
       int whoseTurn = gb.whoseTurn;
       for(int player=0;player<gb.players_in_game;player++)
       	{ commonPlayer pl = getPlayerOrTemp(player);
       	  pl.setRotatedContext(gc, selectPos,false);
    	   DrawChipPool(gc, chipRects[player],capRects[player],pl, ourTurnSelect,gb);
    	   if(planned && whoseTurn==player)
    	   {
    		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
       	   pl.setRotatedContext(gc, selectPos,true);
       	}
       commonPlayer pl = getPlayerOrTemp(whoseTurn);
       double messageRotation = pl.messageRotation();
       
       GC.setFont(gc,standardBoldFont());
       handleDrawUi(gc,messageRotation,state.getRole(),gb.drawIsLikely(),buttonSelect,
       		  acceptDrawRect,declineDrawRect,HighlightColor,rackBackGroundColor);

       if (state != TrenchState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive())
				{
				doneButton.show(gc,messageRotation,gb.DoneState() ? buttonSelect : null);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==TrenchState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        standardGameMessage(gc,messageRotation,
        					// note that gameOverMessage() is also put into the game record
            				state==TrenchState.Gameover?gameOverMessage(gb):s.get(state.description()),
            				state!=TrenchState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        gb.getPlayerChip(gb.whoseTurn).drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
        DrawRepRect(gc,pl.displayRotation,Color.black,gb.Digest(),repRect);
        eyeRect.activateOnMouse = true;
        eyeRect.draw(gc,selectPos);
        arrowRect.activateOnMouse = true;
        arrowRect.draw(gc,selectPos);
        drawViewsetMarker(gc,viewsetRect,selectPos);
        DrawReverseMarker(gc,reverseViewRect,selectPos,TrenchId.Reverse);
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);

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
        numberMenu.recordSequenceNumber(bb.activeMoveNumber());
        /**
         * animations are handled by a simple protocol between the board and viewer.
         * when stones are moved around on the board, it pushes the source and destination
         * cells onto the animationStck.  startBoardAnimations converts those points into
         * animation sprites.  drawBoardElements arranges for the destination stones, which
         * are already in place, to disappear until the animation finishes.  The actual drawing
         * is done by drawSprites at the end of redrawBoard
         */
        startBoardAnimations(replay,bb.animationStack,CELLSIZE,MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay.animate) { playSounds(mm); }
       return (true);
    }


 void playSounds(commonMove mm)
 {
	 switch(mm.op)
	 {
	 case MOVE_DROPB:
	 case MOVE_PICKB:
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
        return (new Trenchmovespec(st, pl));
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
    	  boolean oknone = (nmove.op==MOVE_DROP);
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
        if (hp.hitCode instanceof TrenchId)// not dragging anything yet, so maybe start
        {
        TrenchId hitObject =  (TrenchId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    
 	    case Black:
 	    case White:
	    	PerformAndTransmit(G.concat("Pick " , hitObject.name()," ",hp.hit_index));
	    	break;
	    case BoardLocation:
	        TrenchCell hitCell = hitCell(hp);
	        // this enables starting a move by dragging 
	    	if((hitCell.topChip()!=null) && (bb.movingObjectIndex()<0))
	    		{ PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    		
	    		}
	    	break;
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
       	if(!(id instanceof TrenchId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        TrenchId hitCode = (TrenchId)id;
        
        // if direct drawing, hp.hitObject is a cell from a copy of the board
        TrenchCell hitObject = bb.getCell(hitCell(hp));
		TrenchState state = bb.getState();
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
        case Reverse:
        	bb.setReverseY(!bb.reverseY());
        	generalRefresh();
        	break;
        case ToggleArrow:
        	arrowRect.toggle();
        	break;
        case ToggleEye:
        	eyeRect.toggle();
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state "+state);
			case Confirm:
			case Play:
				// fall through and pick up the previously dropped piece
				//$FALL-THROUGH$
			case Puzzle:
				PerformAndTransmit((bb.pickedObject==null ? "Pickb ":"Dropb ")+hitObject.col+" "+hitObject.row);
				break;
			}
			break;
			
        case Black:
        case White:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit(G.concat("Drop ",hitCode.name()," ",hp.hit_index));
			}
           break;
 
        }
        }
    }



    private boolean setDisplayParameters(TrenchBoard gb,Rectangle r)
    {
      	boolean complete = false;
      	boolean perspective = usePerspective();
      	if(perspective)
      	{
      		gb.SetDisplayParameters(0.82, 1.0, 0.26,0,42); // shrink a little and rotate 45 degrees
      	}
      	else
      	{	if(reverseView())
      			{
    		gb.SetDisplayParameters(
      				new double[] {0.48,0.24},	// bottom
      				new double[] {0.95,0.57}, // right
      				new double[] {0.08,0.57}, // left
      				new double[] {0.54,0.824}  // top
      				);
     		
      			}
      	else
      		{
      		gb.SetDisplayParameters(
      				new double[] {0.487,0.24},	// bottom
      				new double[] {0.96,0.58}, // right
      				new double[] {0.08,0.58}, // left
      				new double[] {0.55,0.84}  // top
      				);
      		}
      		/*
     		gb.SetDisplayParameters(0.8, 0.75,
     				0.25,	// xoffset
     				.85,		// yoffset
     				40,		// rotation
     				0.0,	// xperspective
     				0.0,	// yperspective
     				0.13); 
     		      		*/
      	}
      	

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
    public String sgfGameType() { return(Trench_SGF); }	// this is the official SGF number assigned to the game

   
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

    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        if(target==offerDrawAction)
     	{	if(OurMove() 
     			&& bb.canOfferDraw()
     			&& (bb.movingObjectIndex()<0)
     			&& ((bb.getState()==TrenchState.Play) || (bb.getState()==TrenchState.DrawPending))) 							
     		{
			PerformAndTransmit(OFFERDRAW);
			}
    		else { G.infoBox(null,s.get(DrawNotAllowed)); }
     		return(true);
     	}
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
 * mainly leave bread crumbs that will be picked up by this thread.
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
    
    //   public void ViewerRun(int wait)
    //   {
    //       super.ViewerRun(wait);
    //   }
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
    {  return(new TrenchPlay());
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
     * convert an ephemeral move to it's no-ephemeral equivalent.  It's also
     * ok to return null meaning the move should be deleted.  Normally, all
     * this will do is change the m.op field, but it needs to agree with 
     * the behavior of movespec {@link commonMove#isEphemeral} method.
     */
    public commonMove convertToSynchronous(commonMove m)
    {	throw G.Error("Not implemented");
    }
    
    // used by the UI to alter the behavior of clocks and prompts
    public boolean simultaneousTurnsAllowed()
    {	return super.simultaneousTurnsAllowed();
    }
  // public RecordingStrategy gameRecordingMode()
  //  {	return(super.gameRecordingMode());
  //  }
    
    // if there are simultaneous turns, robot start/stop can be tricky
    // by default, not allowed in simultaneous phases.  Return true 
    // to let them run "in their normal turn", but this will not allow
    // the robots to start at the beginning of the async phase.
    public boolean allowRobotsToRun() {
    	return super.allowRobotsToRun();
    }

	public int getLastPlacement(boolean empty) {
		return (bb.moveNumber);
	}
	
    private String vprogressString()
    {	return super.gameProgressString()
     		+" " +bb.totalCaptured(SECOND_PLAYER_INDEX)
    		+" " +bb.totalCaptured(FIRST_PLAYER_INDEX);
    }
    public String gameProgressString()
    {	// this is what the standard method does
    	 return(mutable_game_record 
    			? Reviewing
    			: vprogressString());
    }
    
    public boolean drawTileSprite(Graphics gc,HitPoint hp)
    {	int px = hp.hit_x;
    	int py = hp.hit_y;
    	if(G.pointInRect(px,py,boardRect))
    	{	int w = hp.awidth;
    		TrenchChip.Marker.drawChip(gc,this,w,px-w/4,py+w/4,null);
    		return true;
    	}
    	
    	return super.drawTileSprite(gc,hp);
    }
}

