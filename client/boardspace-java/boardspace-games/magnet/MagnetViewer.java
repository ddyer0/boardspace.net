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
package magnet;




import java.awt.*;

import javax.swing.JCheckBoxMenuItem;

import common.GameInfo;

import static magnet.Magnetmovespec.*;
import online.common.*;
import java.util.*;
import lib.CellId;
import lib.Graphics;
import lib.ChatInterface;
import lib.Drawable;
import lib.ExtendedHashtable;
import lib.Random;
import lib.SimpleSprite;
import lib.StockArt;
import lib.Tokenizer;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.Image;
import lib.LFrameProtocol;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;


/**
 * 
 * This is intended to be maintained as the reference example how to interface to boardspace.
 * <p>
 * The overall structure here is a collection of classes specific to Magnet, which extend
 * or use supporting online.game.* classes shared with the rest of the site.  The top level 
 * class is a Canvas which implements ViewerProtocol, which is created by the game manager.  
 * The game manager has very limited communication with this viewer class, but manages
 * all the error handling, communication, scoring, and general chatter necessary to make
 * the game part of the site.
 * <p>
 * The main classes are:
 * <br>MagnetViewer - this class, a canvas for display and mouse handling
 * <br>MagnetBoard - board representation and implementation of the game logic
 * <br>Magnetmovespec - representation, parsing and printing of move specifiers
 * <br>MagnetPlay - a robot to play the game
 * <br>MagnetConstants - static constants shared by all of the above.  
 *  <p>
 *  The primary purpose of the MagnetViewer class is to do the actual
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
 *  <li> use eclipse refactor to rename the package for "magnet" and for individual files
 *  <li> duplicate the magnet start configuration, making a new one for the new game
 *  <li> launch the new game and get it to start, still identical to the old magnet in all but name.
 *  	this will probably require a few edits to the init code.
 *  <li> do a cvs update on the original magnet hierarchy to get back the original code.
 *  
*/
public class MagnetViewer extends CCanvas<MagnetCell,MagnetBoard> implements MagnetConstants
{		
    static final String Magnet_SGF = "Magnet"; // sgf game number allocated for magnet

    // file names for jpeg images and masks
    static final String ImageDir = "/magnet/images/";

     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.yellow;
    private Color chatBackgroundColor = new Color(230,230,230);
    private Color rackBackGroundColor = new Color(200,200,200);
    private Color boardBackgroundColor = new Color(200,200,200);
    

    // private state
    private MagnetBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //public Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //public Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle chatRect = addRect("chatRect");
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    
    //
    // addZoneRect also sets the rectangle as specifically known to the 
    // mouse tracker.  The zones are considered in the order that they are
    // added, so the smaller ones should be first, then any catchall.
    //
    // zones ought to be mostly irrelevant if there is only one board layout.
    //
    private Rectangle chipRects[] = addZoneRect(",chip",2);
    private Rectangle rackRects[] = addRect(",rack",2);
    private Rectangle randomRects[] = addRect(",random",2);
    
    private JCheckBoxMenuItem reverseOption = null;
    private Rectangle reverseRect = addRect("Reverse");
    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	MagnetChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	gameIcon = MagnetChip.board.image;
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
       	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        MouseColors = new Color[]{Color.red,Color.blue};
        MouseDotColors = new Color[]{Color.white,Color.white};
        // magnet is not suitable for autodone because the primary "done" state
        // is an optional promote-or-done state.
        enableAutoDone = false;
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default

        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	MagnetConstants.putStrings();
        }
         

        String type = info.getString(GameInfo.GAMETYPE, MagnetVariation.magnet.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new MagnetBoard(type,players_in_game,randomKey,getActivePlayer(),MagnetBoard.REVISION);
        reverseOption = myFrame.addOption(s.get(ReverseView),bb.reverseY(),deferredEvents);
        setSimultaneousTurnsAllowed(!isTurnBasedGame()); 
        useDirectDrawing(true); 
        doInit(false);

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
    	startFirstPlayer();
    	}
    }
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle rack = rackRects[player];
    	Rectangle random = randomRects[player];
    	Rectangle done = doneRects[player];
    	int u4 = unitsize/4;
    	G.SetRect(chip,	x+u4,	y,	2*unitsize-u4,	2*unitsize-u4);
    	Rectangle box =  pl.createRectangularPictureGroup(x+2*unitsize,y,2*unitsize/3);
    	G.SetRect(rack, x, y+G.Height(box),unitsize*13,unitsize*4);
    	G.SetRect(random, G.Right(box)+unitsize/2,y+unitsize/2,unitsize*5,unitsize);
    	G.SetRect(done, G.Right(box)+unitsize, y, unitsize*4,unitsize*2);
    	G.union(box,rack,random,chip,done);
      	pl.displayRotation = rotation;
    	return(box);
    }
 
    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect, x, y, width, height);
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
				fh*2.5,
				fh*2.5,	// maximum cell size
				0.7		// preference for the designated layout, if any
				);
		
	    // place the chat and log automatically, preferring to place
		// them together and not encroaching on the main rectangle.
		layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
				minLogW, minLogH, minLogW*3/2, minLogH*3/2);
		layout.placeTheVcr(this,minLogW,minLogW*3/2);
	   	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
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
		boolean rotate = seatingFaceToFaceRotated();
	    int nrows = rotate ? 24 : 15;  // b.boardRows
	    int ncols = rotate ? 15 : 24;	 // b.boardColumns
	    int stateH = fh*5/2;
		
		// calculate a suitable cell size for the board
		double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH*2)/nrows);
		CELLSIZE = (int)cs;
		//G.print("cell "+cs0+" "+cs+" "+bestPercent);
		// center the board in the remaining space
		int boardW = (int)(ncols*CELLSIZE);
		int boardH = (int)(nrows*CELLSIZE);
		int extraW = Math.max(0, (mainW-boardW)/2);
		int extraH = Math.max(0, (mainH-boardH-stateH*2)/2);
		int boardX = mainX+extraW;
		int boardY = mainY+extraH+stateH;
		int boardBottom = boardY+boardH;
	   	layout.returnFromMain(extraW,extraH);
		//
		// state and top ornaments snug to the top of the board.  Depending
		// on the rendering, it can occupy the same area or must be offset upwards
		//
	    int stateY = boardY-stateH;
	    int stateX = boardX;
	    placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,noChatRect);
		G.SetRect(boardRect,boardX,boardY,boardW,boardH);
		
		if(rotate)
		{
			G.setRotation(boardRect, -Math.PI/2);
			contextRotation = -Math.PI/2;
		}
		// goal and bottom ornaments, depending on the rendering can share
		// the rectangle or can be offset downward.  Remember that the grid
		// can intrude too.
		placeRow( boardX, boardBottom,boardW,stateH,goalRect,reverseRect);       
	    setProgressRect(progressRect,goalRect);
	    positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
	
}


    //
	// reverse view icon, made by combining the stones for two colors.
    //
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	MagnetChip king = !bb.reverseY() ? bb.getPlayerChip(0) : bb.getPlayerChip(1);
    	MagnetChip reverse = bb.reverseY() ? bb.getPlayerChip(0) : bb.getPlayerChip(1);
    	reverse.drawChip(gc,this,G.Width(r),G.Left(r)+G.Width(r)/2,G.Top(r)+G.Height(r)/4,null);
    	king.drawChip(gc,this,G.Width(r),G.Left(r)+G.Width(r)/2,G.Top(r)+G.Height(r)-G.Height(r)/4,null);
    	HitPoint.setHelpText(highlight,r,MagnetId.ReverseViewButton,s.get(ReverseViewExplanation));
     }

	// draw a box of spare chips. For magnet it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, int player)
    {	
    	MagnetChip chip = bb.getPlayerChip(player);
    	chip.drawChip(gc, this, r, null);
    }
    public int activePlayer()
    {
    	return (bb.asyncPlay 
		? getActivePlayer().boardIndex
		: bb.whoseTurn);
    }
	// draw a box of spare chips. For magnet it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawRack(Graphics gc, Rectangle r, Rectangle rr, int player, HitPoint highlight,MagnetBoard gb)
    {	
    	MagnetCell row[] = gb.rack[player];
    	MagnetCell cap[] = gb.captures[player];
    	int realw = G.Width(r)/((row.length+1)/2);
    	int w = (int)(0.85*realw);
    	int x0 = G.Left(r)+2*realw/3;
    	int x = x0;
    	int y = G.Top(r)+2*realw/3;
    	commonPlayer pl = getActivePlayer();
    	boolean skipEmpty = pl.isSpectator() || (pl.boardIndex!=player);
    	boolean setupPhase = !gb.setupDone[player];
    	for(int i=0;i<row.length;i++)
    	{
    		MagnetCell cell = cap[i];
    		MagnetCell firstCell = cell;
    		boolean skipThis = cell.isEmpty() && !gb.setupDone[player]; 
    		if(skipThis) { cell = row[i]; }
        	boolean canHit = bb.legalToHitChips(cell,player);
        	boolean skip = skipEmpty && skipThis && cell.isEmpty();
        	if(!skip)
        	{
        	// if drawing the opposing rack, draw strictly left to right
        	// to conceal what's being picked and moved.
    		if(drawStack(gc, gb, cell,canHit?highlight:null,w,x,y))
    		{
    			highlight.arrow = gb.pickedObject!=null ? StockArt.DownArrow : StockArt.UpArrow;
    	    	highlight.awidth = w/2;
    	    	highlight.spriteColor = Color.red;
    		}
    		if(gc!=null) { firstCell.copyCurrentCenter(cell); }
     		x += w;
    		if(x>G.Right(r)) { x = x0; y+= w; }
        	}
    	}
     	if(setupPhase
    			&& !gb.DoneState() 
    			&& (player==activePlayer()))
    	{
    		if(GC.handleRoundButton(gc, rr, highlight,
    				PlaceRandom,
                    HighlightColor, rackBackGroundColor))
    		{
    			highlight.hitCode = MagnetId.RandomRect;    			
    		}
    	}
    }
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
    	MagnetChip chip = MagnetChip.getChip(obj);
    	if(shouldBeConcealed(chip,getActivePlayer())) { chip = chip.getFaceProxy(); }
    	chip.drawChip(g,this,bb.cellSize(), xp, yp, null);
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
       MagnetChip.backgroundTile.image.tileImage(gc, fullRect);   
        drawFixedBoard(gc);
    }
    Image scaled = null;
    Image background = null;
    
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { // erase
      MagnetBoard gb = disB(gc);
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      if(reviewBackground)
      {	 
       MagnetChip.backgroundReviewTile.image.tileImage(gc,brect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      MagnetChip bd = gb.reverseY() ? MagnetChip.board_reversed : MagnetChip.board;
      Image im = bd.getImage(loader);
      if(im!=background) { scaled = null;}
      background = im;
      scaled = im.centerScaledImage(gc, brect,scaled);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
	  setDisplayParameters(gb,brect);
      gb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

     }
    private boolean censorPlayerRects(int x,int y)
    {
    	return(!mutable_game_record
    			&& ((!bb.setupDone[FIRST_PLAYER_INDEX] && G.pointInRect(x, y,getPlayerOrTemp(FIRST_PLAYER_INDEX).playerBox))
    			    || (!bb.setupDone[SECOND_PLAYER_INDEX] && G.pointInRect(x, y,getPlayerOrTemp(SECOND_PLAYER_INDEX).playerBox))));
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
    {	if(censorPlayerRects(x,y))
    	{
    	G.SetLeft(p, -1);
    	G.SetTop(p,-1);
    	return("off");
    	}
    	return(super.encodeScreenZone(x,y,p));
    }
    private MagnetCell temp = new MagnetCell();
    
    // helper function for the GUI, to determine what ought to be hidden
    // from the other player for now.
    private boolean shouldBeConcealed(MagnetCell c,commonPlayer activePlayer)
    {	if(c.isEmpty() || allowed_to_edit) {return(false); }
    	switch(c.rackLocation())
    	{
    	case Blue_Captures:
    	case Red_Captures:
    		return(false);
    	default: break;
    	}
    	return(shouldBeConcealed(c.chipAtIndex(0),activePlayer));
    }
    private boolean shouldBeConcealed(MagnetChip chip,commonPlayer activePlayer)
    {	if(allowed_to_edit) { return(false); }
    	switch(bb.getState())
    	{
    	case Gameover: 
    	case Puzzle: 	return(false); 
    	default:
    		{
    			return(activePlayer.isSpectator() || (chip.playerIndex()!=activePlayer.boardIndex));
    		}
    	}
    }
    private boolean shouldBeConcealed(MagnetCell c)
    {	return(shouldBeConcealed(c,getActivePlayer()));
    }

    private boolean drawStack(Graphics gc,MagnetBoard gb,MagnetCell c,HitPoint highlight,int w,int x,int y)
    {
    	MagnetCell target = c;
    	boolean hit = false;
    	if(shouldBeConcealed(c)) { target = temp; target.concealedCopyFrom(c); }
    	
    	if(target.drawStack(gc, this, highlight, w, x, y,0,0.6,null))
    	{	MagnetState state = gb.getState();
    		highlight.hitObject = c;
    		MagnetChip top = c.topChip();
            boolean picked = (gb.pickedObject!=null);
     	 
        	if((c==gb.selectedCell)
        			||(state==MagnetState.FirstSelect) 
        			|| (state==MagnetState.Select)
        			|| (state==MagnetState.Confirm && gb.resetState()==MagnetState.Select))
        	{
        		highlight.hitCode = MagnetId.UnSelect;
        	}

       		if((top==MagnetChip.magnet) 
    				&& ((highlight.hit_index==1) || (c.height()==1)))
    			{ highlight.hitCode = MagnetId.Magnet;
    			}
            
            if((state==MagnetState.Promote) 
            		&& (highlight.hitCode!=MagnetId.Magnet))
            {
            	highlight.arrow = gb.isNewlyPromoted(c) ? StockArt.Rotate_CCW : StockArt.Rotate_CW;
                highlight.awidth = 3*CELLSIZE/3;
                
            }
            else 
            {
                highlight.awidth = CELLSIZE;
                highlight.arrow = picked ? StockArt.DownArrow : StockArt.UpArrow;
            }
            
    		hit = true;
    	}
    	c.copyCurrentCenter(target);
    	return(hit);
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
    public void drawBoardElements(Graphics gc, MagnetBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  

        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        MagnetCell closestCell = gb.closestCell(highlight,brect);
        boolean hitCell = gb.LegalToHitBoard(closestCell,bb.whoseTurn);
        MagnetCell dest = gb.getDest();
       	Enumeration<MagnetCell>cells = gb.getIterator(Itype.TBLR);
       	numberMenu.clearSequenceNumbers();
       	while(cells.hasMoreElements())
       	{
       		MagnetCell cell = cells.nextElement();

  	        //G.print(cell);
            boolean drawhighlight = 
            	(hitCell && (cell==closestCell));	// is legal for a "pick" operation+
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            numberMenu.saveSequenceNumber(cell,xpos,ypos);
            if (drawhighlight || (cell==dest) || (cell==gb.selectedCell))
             { // checking for pointable position
            	int sz = (cell==gb.selectedCell)
            				?gb.cellSize()*4 
            				: gb.cellSize()*5;
            	 StockArt.SmallO.drawChip(gc,this,sz,xpos,ypos,null);                
             }
            drawStack(gc,gb,cell,drawhighlight?highlight:null,gb.cellSize(),xpos,ypos);

            //StockArt.SmallO.drawChip(gc, this, CELLSIZE, xpos,ypos,null);
            }
       	numberMenu.drawSequenceNumbers(gc,gb.cellSize(),labelFont,labelColor,0.45);
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
    {  MagnetBoard gb = disB(gc);
       MagnetState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
   	   if(gc!=null)
   		{
   		// note this gets called in the game loop as well as in the display loop
   		// and is pretty expensive, so we shouldn't do it in the mouse-only case
      
       setDisplayParameters(gb,boardRect);
   		}
       // 
       // if it is not our move, we can't click on the board or related supplies.
       // we accomplish this by supressing the highlight pointer.
       //
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;
       //
       // even if we can normally select things, if we have already got a piece
       // moving, we don't want to hit some things, such as the vcr group
       //
       HitPoint buttonSelect = moving ? null : ourTurnSelect;
       // hit anytime nothing is being moved, even if not our turn or we are a spectator
       HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;
       
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
       GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect);
       GC.unsetRotatedContext(gc,selectPos);
       GC.setFont(gc,standardBoldFont());
       boolean planned = plannedSeating();
       for(int i=0;i<2;i++)
       {   commonPlayer pl = getPlayerOrTemp(i);
       	   pl.setRotatedContext(gc,selectPos,false);
           DrawRack(gc, rackRects[i],randomRects[i],i, ourTurnSelect,gb);  	   
           DrawChipPool(gc, chipRects[i],i);
           if(planned && (state!=MagnetState.Setup) && gb.whoseTurn==i)
    	   {
    		   handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
       	   pl.setRotatedContext(gc,selectPos,true);
       }
        

		if (state != MagnetState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive() && handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor))
			{
				 buttonSelect.hitCode = simultaneousTurnsAllowed()
 						? MagnetId.EphemeralDone 
 						: GameId.HitDoneButton;
			}
			
			handleEditButton(gc,editRect,buttonSelect,selectPos, HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==MagnetState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        boolean simultaneous = simultaneousTurnsAllowed();
        standardGameMessage(gc,
            		state==MagnetState.Gameover?gameOverMessage(gb):s.get(state.description()),
            				state!=MagnetState.Puzzle && !simultaneous,
            				gb.whoseTurn,
            				stateRect);
        if(!simultaneous) { DrawChipPool(gc, iconRect, gb.whoseTurn); }

        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(MagnetVictoryCondition),progressRect, goalRect);
            //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for magnet
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        drawAuxControls(gc,nonDragSelect);

    }
    public int getLastPlacement(boolean empty)
    {
    	return bb.dropState;
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
        numberMenu.recordSequenceNumber(bb.moveNumber);
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

 public SimpleSprite startAnimation(cell<?> src,cell<?> dest,Drawable top,int siz,double start,double end,int depthm1)
 {	 if(shouldBeConcealed((MagnetCell)dest))
 		{ top = ((MagnetChip)top).getFaceProxy(); 
 		}
	 return(super.startAnimation(src, dest, top, siz, start, end,depthm1));
 }
 void playSounds(commonMove mm)
 {
	 switch(mm.op)
	 {
	 case MOVE_DROPB:
	 case MOVE_PICKB:
	 case MOVE_SELECT:
	 case MOVE_PICK:
	 case EPHEMERAL_PICK:
	 case EPHEMERAL_PICKB:
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
    public commonMove ParseNewMove(String st, int player)
    {
        return (new Magnetmovespec(st, player));
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
 * For reference, the commented out method below does the same thing for "Hex". 
 * You could resort to similar techniques to replace or augment what super.EditHistory
 * does, but your efforts would probably be better spent improving your Digest() method
 * so the commonCanvas method gives the desired result.
 * 
 * Note that it should always be correct to simply return nmove and accept the messy
 * game record.
 * 
 * This may require that move be merged with an existing history move
 * and discarded.  Return null if nothing should be added to the history
 * One should be very cautious about this, only to remove real pairs that
 * result in a null move.  It is vital that the operations performed on
 * the history are identical in effect to the manipulations of the board
 * state performed by "nmove".  This is checked by verifyGameRecord().
 * 
 * in commonEditHistory()
 * 
 */
      public commonMove EditHistory(commonMove nmove)
      {	  // some damaged games ended up with naked "drop", this lets them pass
    	  if(nmove.op==NORMALSTART) { return(nmove); }
    	  commonMove rval = EditHistory(nmove,(nmove.op==EPHEMERAL_DONE)
    			  										||(nmove.op==EPHEMERAL_MOVE));

    	  return(rval);
      }

 
    private void doHitCell(MagnetCell hitCell,boolean stop)
    {
    switch(bb.getState())
    {
    case FirstSelect:
    case Select:
    	break;
    case Promote:
    	if(stop)
    	{
    	if(bb.isNewlyPromoted(hitCell))
    	{
    	PerformAndTransmit("Demote "+hitCell.col+" "+hitCell.row);
    	}
    	else 
    	{
    	PerformAndTransmit("Promote "+hitCell.col+" "+hitCell.row);	
    	}}
    	break;
    default:
    	if(simultaneousTurnsAllowed())
    	{	// ephemeral picks not transmitted
    		PerformAndTransmit("ePickb "+hitCell.col+" "+hitCell.row,false,replayMode.Live);
    	}
    	else
    	{
    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
    	}
    }
    }
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
        if (hp.hitCode instanceof MagnetId)// not dragging anything yet, so maybe start
        {
        MagnetId hitObject =  (MagnetId)hp.hitCode;
        MagnetCell hitCell = bb.getCell(hitCell(hp));
 	    switch(hitObject)
	    {
	    default: break;

 	    case Blue_Chip_Pool:
 	    case Red_Chip_Pool:
 	    	{
 	    	// ephemeral picks not transmitted
	    	PerformAndTransmit((bb.simultaneousTurnsAllowed() ? "epick " : "Pick ") + hitObject.shortName+" "+hitCell.row,
	    			false,replayMode.Live);
 	    	}
	    	break;
 	    case Magnet: 
 	    	PerformAndTransmit((bb.simultaneousTurnsAllowed() ? "epickb " : "Pickb ") + hitCell.col+" "+hitCell.row);
 	    	break;
	    case BoardLocation:
	        { 
	        doHitCell(hitCell,false);
	        }
	    	break;
	    }
        }
    }
    
    public void sendDone()
    {
    	if(simultaneousTurnsAllowed()) { PerformAndTransmit("edone"); }
    	else { PerformAndTransmit("done"); }
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
       	if(!(id instanceof MagnetId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        MagnetId hitCode = (MagnetId)id;
        MagnetCell hitObject = bb.getCell(hitCell(hp));
        switch (hitCode)
        {
        case UnSelect:
        	PerformAndTransmit("Select "+hitObject.col+" "+hitObject.row);
        	break;
        case EphemeralDone: 
        	PerformAndTransmit("edone");
        	break;
        default:
              	throw G.Error("Hit Unknown: %s", hitObject);
        case ReverseViewButton:
        	{boolean v = !bb.reverseY();
        	bb.setReverseY(v);
        	reverseOption.setState(v);
        	generalRefresh();
        	}
          	 break;
        case RandomRect:
        {	int pl = activePlayer();
        	String move = simultaneousTurnsAllowed() ? "emove" : "randommove";
        	MagnetCell rack[] = bb.rack[pl];
        	CellStack from = new CellStack();
        	for(MagnetCell c : rack) { if(!c.isEmpty()) { from.push(c);} }
        	CellStack to = new CellStack();
        	for(MagnetCell c = bb.allCells; c!=null; c=c.next)
        	{
        		if(c.isStartingCell(pl) && c.isEmpty())
        		{
        			to.push(c);
        		}
        	}
        	Random r = new Random();
        	from.shuffle(r);
        	to.shuffle(r);
        	while(from.size()>0)
        	{
        		MagnetCell f = from.pop();
        		MagnetCell t = to.pop();
        		PerformAndTransmit(move+" "+f.rackLocation().shortName+" "+f.col+" "+f.row
        						+" "+t.rackLocation().shortName+" "+t.col+" "+t.row);
        	}
        	}

        	break;
        case Magnet:
        case BoardLocation:	// we hit an occupied part of the board 
        	if(bb.movingObjectIndex()>=0)
        	{
         	   if(simultaneousTurnsAllowed())
         	   {
         		
         		MagnetCell src = bb.getSource();
         		PerformAndTransmit("emove "+ src.rackLocation().shortName+" "+src.col+" "+src.row
                						+" "+hitObject.rackLocation().shortName+" "+hitObject.col+" "+hitObject.row);
       		   
         	   }
         	   else 
         	   {
         		   PerformAndTransmit("dropb "+hitObject.col+" "+hitObject.row);
         	   }
        	}
        	else
        	{
        		doHitCell(hitObject,true);
        	}
 			break;
        case Blue_Captures:
        case Red_Captures:
        case Blue_Chip_Pool:
        case Red_Chip_Pool:
           if(bb.movingObjectIndex()>=0)
			{//if we're dragging a chip around, drop it.
        	   if(simultaneousTurnsAllowed())
        	   {
        		MagnetCell src = bb.getSource();
               	PerformAndTransmit("emove "+ src.rackLocation().shortName+" "+src.col+" "+src.row
               						+" "+hitObject.rackLocation().shortName+" "+hitObject.col+" "+hitObject.row);
       		   
        	   }
        	   else
        	   {
            	PerformAndTransmit("Drop "+ hitCode.shortName+" "+hitObject.row);
        	   }
			}
           break;
 
        }
        }
    }

    public void drawAuxControls(Graphics gc,HitPoint highlight)
   {  
      DrawReverseMarker(gc,reverseRect,highlight);
   }

    private void setDisplayParameters(MagnetBoard gb,Rectangle r)
    {

          // the numbers for the square-on display are slightly ad-hoc, but they look right
          //gb.SetDisplayParameters( 0.85, 0.6, 0,0,20,
          //	  0.1,0.0,0.25); // shrink a little and rotate 30 degrees
      	  //gb.setDisplayRect();
          gb.SetDisplayParameters(1.18, 0.91, 
      		  	-0.44,-0.2,
      		  	-29.5,
      		  	0.148,0.13,
      		  	0.045); // shrink a little and rotate 30 degrees

      	gb.SetDisplayRectangle(r);
    }
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     * <p>
     * if complete is true, we definitely want to start from scratch, otherwise
     * only the known changed elements need to be painted.  Exactly what this means
     * is game specific, but for magnet the underlying empty board is cached as a deep
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
    public String sgfGameType() { return(Magnet_SGF); }	// this is the official SGF number assigned to the game

   
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
    }
     public void doShowSgf()
     {
         if ( mutable_game_record || G.debug())
         {
             super.doShowSgf();
         }
         else
         {
             theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                 s.get(CensoredGameRecordString));
         }
     }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);

       	if(target==reverseOption)
    	{
    	bb.setReverseY(reverseOption.getState());
    	generalRefresh();
    	handled = true;
    	}
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
 *  */
    

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


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new MagnetPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/25/2023
		46 files visited 0 problems
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
    
    /* 
     * 
     * Asynchronous startup handling
     * 
     * The general strategy is that "ephemeral" moves are not part of the 
     * permanent game record, and are retransmitted with each move instead
     * of being incrementally added to the permanent record.  The various 
     * players are allowed (and expected) to see and execute these moves in
     * different order.   At some point, ephemeral movement stops, the
     * ephemeral moves are canonicalized and added to the permanent record,
     * and normal synchronous moves resume.
     * 
     * In Magnet, the opening sequence where both players secretly
     * place their pieces is simultaneous and ephemeral.  when all players
     * have committed to their placement, canonicalization occurs and 
     * normal movement starts. 
     * 
     * 
     */

    
    public commonMove convertToSynchronous(commonMove m)
    {
    	switch(m.op)
		{
		default: throw G.Error("Not expecting move %s",m);
		case EPHEMERAL_PICK: 
		case EPHEMERAL_PICKB:
			return(null);	// remove
		case EPHEMERAL_DONE:
			m.op=MOVE_DONE;
			break;
		case EPHEMERAL_MOVE:
			m.op =MOVE_FROM_TO;	// convert to a non-ephemeral move
		}
    	return(m);
    }
    
    // true if allowed in the active game.  This is used
    // by the standard game method to ok triggering of robot moves.
    public MagnetState activeGameState()
    {
    	return (reviewMode()? (MagnetState)History.pre_review_state : bb.getState());
    }

    public boolean allowRobotsToRun(commonPlayer p)
    {
    	if(simultaneousTurnsAllowed() && bb.setupDone[p.boardIndex]) { return false; }
    	return true;
    }
    public void startSynchronousPlay()
    {
 	   if(!reviewMode() 
 			   && allRobotsIdle()
 			   && (bb.getState()==MagnetState.NormalStart))
 	   {
 		   canonicalizeHistory();
 		   // all the players independently resume normal play
 		   // and must all make the same decisions about what
 		   // the game history is and whose turn it is now.
 		   PerformAndTransmit("NormalStart",false,replayMode.Live);
 	   }
    }
    public boolean allowUndo()
    {	// this prevents a null "undo" at the start of normal play
    	return(super.allowUndo() && (getCurrentMoveOp()!=NORMALSTART));
    }

    public void ViewerRun(int wait)
    {
           super.ViewerRun(wait);
           startSynchronousPlay();		// start synchronous play when all are ready
    }

}

