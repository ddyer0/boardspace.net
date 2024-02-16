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
package xeh;

import javax.swing.JCheckBoxMenuItem;

import common.GameInfo;

import static xeh.XehMovespec.*;

import java.awt.*;

import online.common.*;
import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.Random;
import lib.StockArt;
import lib.InternationalStrings;
import lib.LFrameProtocol;
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
 * <br>HavannahViewer - this class, a canvas for display and mouse handling
 * <br>HavannahBoard - board representation and implementation of the game logic
 * <br>HavannahMovespec - representation, parsing and printing of move specifiers
 * <br>HavannahPlay - a robot to play the game
 * <br>HavannahConstants - static constants shared by all of the above.  
 *  <p>
 *  The primary purpose of the HavannahViewer class is to do the actual
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
 *  <li> use eclipse refactor to rename the package for "hex" and for individual files
 *  <li> duplicate the hex start configuration, making a new one for the new game
 *  <li> launch the new game and get it to start, still identical to the old hex in all but name.
 *  	this will probably require a few edits to the init code.
 *  <li> do a cvs update on the original hex hierarchy to get back the original code.
 *  
*/
public class XehViewer extends CCanvas<XehCell,XehBoard> implements XehConstants
{	static final long serialVersionUID = 1000;
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(255,230,230);
    private Color rackBackGroundColor = new Color(225,192,182);
    private Color boardBackgroundColor = new Color(220,165,155);
    

     
    // private state
    private XehBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle secondPlayerChipRect = addZoneRect("secondPlayerChipRect");
    private Rectangle firstPlayerChipRect = addZoneRect("firstPlayerChipRect");
	private Rectangle swapRect=addRect("swapRect");
	// private menu items
    private JCheckBoxMenuItem rotationOption = null;		// rotate the board view
    private boolean doRotation=true;					// current state
    private boolean lastRotation=!doRotation;			// user to trigger background redraw
    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	XehChip.preloadImages(loader,ImageDir);	// load the images used by stones
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
        
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default

        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	InternationalStrings.put(XehStrings);
        	InternationalStrings.put(XehStringPairs);
        }
         
        rotationOption = myFrame.addOption("rotate board",true,deferredEvents);
        
        String type = info.getString(GameInfo.GAMETYPE, XehVariation.xeh.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new XehBoard(type,players_in_game,randomKey,XehBoard.REVISION);
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
        //if (val && extraactions)
        //{
        //    System.out.println(formHistoryString());
        //}
     //   return (val);
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
 //   public void setLocalBounds(int x, int y, int width, int height)
 //   {   
 //   	int wide = setLocalBoundsSize(width,height,true,false);
 //   	int tall = setLocalBoundsSize(width,height,false,true);
 //   	int normal = setLocalBoundsSize(width,height,false,false);
 //   	boolean useWide = wide>normal && wide>tall;
 //   	boolean useTall = tall>normal && tall>=wide;
 //   	if(useWide|useTall) { setLocalBoundsSize(width,height,useWide,useTall); }
 //   	setLocalBoundsWT(x,y,width,height,useWide,useTall);
 //   }
    /**
     * calculate a metric for one of three layouts, "normal" "wide" or "tall",
     * which should normally correspond to the area devoted to the actual board.
     * these don't have to be different, but devices with very rectangular
     * aspect ratios make "wide" and "tall" important.  
     * @param width
     * @param height
     * @param wideMode
     * @param tallMode
     * @return a metric corresponding to board size
     */
    public int setLocalBoundsSize(int width,int height,boolean wideMode,boolean tallMode)
    {	
        int ncols = tallMode ? 29 : wideMode ? 42 : 37; // more cells wide to allow for the aux displays
        int nrows = tallMode ? 27 : 20;  
        int cellw = width / ncols;
        int chatHeight = selectChatHeight(height);
        int cellh = (height-(wideMode?0:chatHeight)) / nrows;
        
        CELLSIZE = ((chatHeight==0)&&wideMode) 
        				? 0	// no chat, never select wide mode 
        				: Math.max(2,Math.min(cellw, cellh)); //cell size appropriate for the aspect ratio of the canvas
        return(CELLSIZE);
    }

    public void setLocalBoundsWT(int x, int y, int width, int height,boolean wideMode,boolean tallMode)
    {   lastRotation = !doRotation;	// force new background
        int nrows = 20;  
        int chatHeight = selectChatHeight(height);
        boolean noChat = (chatHeight==0);
        int logHeight = wideMode||noChat||tallMode ? CELLSIZE*5 : chatHeight;
        int C2 = CELLSIZE/2;
        G.SetRect(fullRect,0, 0,width, height);

        G.SetRect(boardRect, 0, wideMode ? 0 : chatHeight+C2,
        		CELLSIZE * (int)(nrows*1.5), CELLSIZE * (nrows ));
        
        int stateX = C2;
        int stateY = G.Top( boardRect);
        int stateH = CELLSIZE;
        G.SetRect(noChatRect,G.Right(boardRect)-stateH , stateY, stateH,stateH);
        G.SetRect(stateRect, stateX,stateY, G.Left(noChatRect), stateH);

 		G.SetRect(swapRect,G.Left( boardRect) + CELLSIZE, G.Top(boardRect)+(doRotation?2:12)*CELLSIZE,
 				 CELLSIZE * 5, 3*CELLSIZE/2 );

		// a pool of chips for the first player at the top
        G.SetRect(firstPlayerChipRect,
        		tallMode ? G.Left(boardRect)+CELLSIZE : G.Right(boardRect) - (wideMode? 0 : 8*CELLSIZE),
        		tallMode ? G.Bottom(boardRect)+CELLSIZE : wideMode? CELLSIZE: chatHeight+CELLSIZE,
        		2*CELLSIZE,
        		3*CELLSIZE);
        
        
        // and for the second player at the bottom
		G.AlignXY(secondPlayerChipRect, 
				tallMode ? G.Left(firstPlayerChipRect)+CELLSIZE*12 : G.Left(firstPlayerChipRect) + (wideMode?0:4*CELLSIZE),	
				tallMode ? G.Top(firstPlayerChipRect) : G.Top(firstPlayerChipRect)+6*CELLSIZE+((doRotation&&!wideMode)?6*CELLSIZE:0),
				firstPlayerChipRect);

		//this sets up the "vcr cluster" of forward and back controls.
        SetupVcrRects(C2,
            G.Bottom(boardRect) - (5 * CELLSIZE),
            CELLSIZE * 6,
            CELLSIZE*3);
        
        G.SetRect(goalRect, CELLSIZE * 5, G.Bottom(boardRect)-CELLSIZE,G.Width(boardRect)-16*CELLSIZE , CELLSIZE);
        
        setProgressRect(progressRect,goalRect);
  
        {
            commonPlayer pl0 = getPlayerOrTemp(0);
            commonPlayer pl1 = getPlayerOrTemp(1);
            Rectangle p0time = pl0.timeRect;
            Rectangle p1time = pl1.timeRect;
            Rectangle p0anim = pl0.animRect;
            Rectangle p1anim = pl1.animRect;
            Rectangle p0aux = pl0.extraTimeRect;
            Rectangle p1aux = pl1.extraTimeRect;
            Rectangle firstPlayerRect = pl0.nameRect;
            Rectangle secondPlayerRect = pl1.nameRect;
            Rectangle firstPlayerPicRect = pl0.picRect;
            Rectangle secondPlayerPicRect = pl1.picRect;
            
            //first player name
            G.SetRect(firstPlayerRect, G.Right(firstPlayerChipRect)+CELLSIZE,G.Top( firstPlayerChipRect),
            		CELLSIZE * 4, CELLSIZE);
            
            // first player portrait
            G.SetRect(firstPlayerPicRect, G.Left(firstPlayerRect),G.Bottom(firstPlayerRect),
            		CELLSIZE * 4,CELLSIZE * 4);
            // "edit" rectangle, available in reviewers to switch to puzzle mode
            G.SetRect(editRect, 
            		G.Right(boardRect)-CELLSIZE*5,G.Bottom(boardRect)-5*CELLSIZE/2,
            		CELLSIZE*3,3*CELLSIZE/2);
           
     
            //second player name
            G.AlignXY(secondPlayerRect, G.Right(secondPlayerChipRect)+CELLSIZE,G.Top(secondPlayerChipRect),
            		firstPlayerRect);


            // player 2 portrait
            G.AlignXY(secondPlayerPicRect,G.Left(secondPlayerRect),G.Bottom(secondPlayerRect),
            		firstPlayerPicRect);
            
            // time display for first player
            G.SetRect(p0time, G.Right(firstPlayerRect),G.Top(firstPlayerRect), 2*CELLSIZE, CELLSIZE);
            G.AlignLeft(p0aux, G.Bottom(p0time), p0time);
            // first player "i'm alive" animation ball
            G.SetRect(p0anim, G.Right(p0time) ,G.Top( p0time),CELLSIZE, CELLSIZE);
            // time display for second player
            G.AlignXY(p1time, G.Right(secondPlayerRect),G.Top(secondPlayerRect),p0time);
            G.AlignLeft(p1aux, G.Bottom(p1time), p1time);
            
            G.AlignXY(p1anim, G.Right(p1time),G.Top(p1time),p0anim);
         
            int chatX = wideMode ? G.Right(boardRect):G.Left(fullRect);
            int chatY = wideMode ? G.Top(secondPlayerChipRect)+6*CELLSIZE : G.Top(fullRect);
            int lowest = G.Bottom(firstPlayerPicRect);
            boolean logBottom = tallMode & (height-lowest>CELLSIZE*6);
            int logW = CELLSIZE * (logBottom ? 8 : 6);
            int logX = wideMode 
            			? G.Right(boardRect)-logW-CELLSIZE*2 
            			: noChat&&!tallMode ? G.Right(boardRect) : width-logW-C2;

            G.SetRect(chatRect, 
            		chatX,		// the chat area
            		chatY,
            		width-(tallMode ? C2 : (wideMode?chatX:logW)+CELLSIZE),
            		wideMode?height-chatY-C2:chatHeight);
            int logY = logBottom 
            			? lowest+C2 
            			: noChat
            			  ? (tallMode ? 0 : G.Bottom(doRotation ? firstPlayerPicRect : secondPlayerPicRect))+CELLSIZE
            			  : tallMode ? G.Bottom(chatRect)+CELLSIZE : y ;
            G.SetRect(logRect,logX,    		logY,logW,
            		logBottom ? height-lowest-CELLSIZE : logHeight);

          
            // "done" rectangle, should always be visible, but only active when a move is complete.
            G.AlignXY(doneRect, G.Left(editRect)-4*CELLSIZE,
            		G.Top(editRect),
            		editRect);
           }
 

        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        generalRefresh();
    }



	// draw a box of spare chips. It's mostly for visual effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,XehBoard gb)
    {
        boolean canhit = gb.LegalToHitChips(player) && G.pointInRect(highlight, r);
        if (canhit)
        {
            highlight.hitCode = gb.getPlayerColor(player);
            highlight.arrow = (gb.pickedObject!=null)?StockArt.DownArrow:StockArt.UpArrow;
            highlight.awidth = CELLSIZE;
        }

        if (gc != null)
        { // draw a random pile of chips.  It's just for effect
        	XehCell c = bb.getPlayerCell(player);
            int spacex = G.Width(r) - CELLSIZE;
            int spacey = G.Height(r) - CELLSIZE;
            Random rand = new Random(4321 + player); // consistant randoms, different for black and white 

            if (canhit)
            {	// draw a highlight background if appropriate
                GC.fillRect(gc, HighlightColor, r);
            }

            GC.frameRect(gc, Color.black, r);
            XehChip chip = gb.getPlayerChip(player);
            int nc = 20;							 // draw 20 chips
            while (nc-- > 0)
            {	int rx = Random.nextInt(rand, spacex);
                int ry = Random.nextInt(rand, spacey);
                c.drawChip(gc,this,chip,bb.cellSize(),G.Left(r)+CELLSIZE/2+rx,G.Top(r)+CELLSIZE/2+ry,null);
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
    	XehChip.getChip(obj).drawChip(g,this,bb.cellSize(), xp, yp, null);
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
    { // erase
      XehBoard gb = disB(gc);
      setDisplayParameters(gb,boardRect);
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
       //GC.fillRect(gc, fullRect);
     XehChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       XehChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      gb.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);
	  // draw the tile grid.  The positions are determined by the underlying board
      // object, and the tile itself if carefully crafted to tile the board
      // when drawn this way.  For games with simple graphics, we could use the
      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
      // but for more complex graphics with overlapping shadows or stacked
      // objects, this double loop is useful if you need to control the
      // order the objects are drawn in.
      int xsize = gb.cellSize();
      int left = G.Left(boardRect);
      int top = G.Bottom(boardRect);
      for(Enumeration<XehCell>cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements(); )
      { 
    	XehCell c = cells.nextElement();
      	int ypos = top - gb.cellToY(c);
      	int xpos = left + gb.cellToX(c);
      	XehChip tile = lastRotation?XehChip.hexTile:XehChip.hexTileNR;
      	XehChip btiles[] = lastRotation ? XehChip.border : XehChip.borderNR;
             // double scale[] = TILESCALES[hidx];
             //adjustScales(scale,null);		// adjust the tile size/position.  This is used only in development
             // to fine tune the board rendering.
             //G.print("cell "+CELLSIZE+" "+xsize);
              tile.drawChip(gc,this,xsize,xpos,ypos,null);
              //equivalent lower level draw image
              // drawImage(gc,tileImages[hidx].image,tileImages[hidx].getScale(), xpos,ypos,gb.CELLSIZE,1.0);
              //
               
              // decorate the borders with darker and lighter colors.  The border status
              // of cells is precomputed, so each cell has a mask of which borders it needs.
              // in order to make the artwork as simple as possible to maintain, the border
              // pictures are derived directly from the hex cell masters, so they need the
              // same scale and offset factors as the main cell.
         if(c.borders!=0)
              {
              for(int dir=0; dir<4;dir++)
              {	// precalculated border cell properties
            	  if((c.borders&(1<<dir))!=0)
            	  {	  btiles[dir].drawChip(gc,this,xsize,xpos,ypos,null);
            	  }
              }}
          }
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
   // public String encodeScreenZone(int x, int y,Point p)
   // {
   // 	return(super.encodeScreenZone(x,y,p));
   // }

    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, XehBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
        // this is the active part of the "Start Evaluator" feature
        commonPlayer activePlayer = getActivePlayer(); //viewerWhoseTurn()
    	XehPlay robot = (XehPlay)(extraactions ? (activePlayer.robotPlayer) : null);
    	if(robot==null && extraactions) {
    		robot = (XehPlay)(players[gb.whoseTurn].robotPlayer);
    	}
    	if(robot!=null)
    	{
    	commonMove cm = getCurrentMove();
    	if(cm!=null) { cm = cm.next; }
    	if(cm!=null)
    		{
    		robot.setTrainingData(cm);
    		}
    	}
        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        XehCell closestCell = gb.closestCell(highlight,brect);
        boolean hitCell = gb.LegalToHitBoard(closestCell);
        if(hitCell)
        { // note what we hit, row, col, and cell
          boolean empty = (closestCell.topChip() == null);
          boolean picked = (gb.pickedObject!=null);
          highlight.hitCode = (empty||picked) ? XehId.EmptyBoard : XehId.BoardLocation;
          highlight.hitObject = closestCell;
          highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
          highlight.awidth = CELLSIZE;
        }
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        if (gc != null)
        {
        for(XehCell cell = gb.allCells; cell!=null; cell=cell.next)
          {
            boolean drawhighlight = (hitCell && (cell==closestCell)) 
   				|| gb.isDest(cell) 		// is legal for a "drop" operation
   				|| gb.isSource(cell);	// is legal for a "pick" operation+
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
  
            if (drawhighlight)
             { // checking for pointable position
            	 StockArt.SmallO.drawChip(gc,this,gb.cellSize()*5,xpos,ypos,null);                
             }
            cell.drawChip(gc,this,highlight,gb.cellSize(),xpos,ypos,null);
 /*           if(cell.isEmpty() && cell.isPossibleBridge(XehChip.White))
            {
            	XehChip.White.drawChip(gc,this,(int)(gb.CELLSIZE/2),xpos,ypos,null);
           	
            }
            if(cell.isEmpty() && cell.isPossibleBridge(XehChip.Black))
            {
            	XehChip.Black.drawChip(gc,this,(int)(gb.CELLSIZE/3),xpos,ypos,null);
           	
            }
            */
            if(cell!=null 
            		&& cell.topChip()==null 
            		&& robot!=null
            		)
            	{
            	double ev =robot.getNeuroEval(cell);
            	//if((cell.row==11) && cell.col>='F') { G.print("c "+cell+" "+ev); }
            	//if(ev>10) {G.print(""+cell+" "+ev); }
            	StockArt.SmallO.drawChip(gc,this,(int)(gb.cellSize()*ev*2),xpos,ypos,null);
            	repaint(1000);
            	}
            }
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
    {  XehBoard gb = disB(gc);
       XehState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);

		   if(theChat!=null) { theChat.redrawBoard(gc, selectPos); }

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
       drawBoardElements(gc, gb, boardRect, ourTurnSelect);
       DrawChipPool(gc, secondPlayerChipRect, SECOND_PLAYER_INDEX, ourTurnSelect,gb);
       DrawChipPool(gc, firstPlayerChipRect, FIRST_PLAYER_INDEX, ourTurnSelect,gb);
       GC.setFont(gc,standardBoldFont());
       
       // draw the board control buttons 
		if((state==XehState.ConfirmSwap) 
			|| (state==XehState.PlayOrSwap) 
			|| (state==XehState.Puzzle))
		{ // make the "swap" button appear if we're in the correct state
			if(GC.handleRoundButton(gc, swapRect, buttonSelect, s.get(SWAP),
                HighlightColor, rackBackGroundColor))
			{ buttonSelect.hitCode = GameId.HitSwapButton;
			}
			// this is an example of how to set a tooltip
			HitPoint.setHelpText(selectPos,swapRect,s.get(SwitchMessage));
		}

		if (state != XehState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
			
			handleEditButton(gc,editRect,buttonSelect, selectPos,HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==XehState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        standardGameMessage(gc,
            		state==XehState.Gameover?gameOverMessage(gb):s.get(state.description()),
            				state!=XehState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(XehVictoryCondition),progressRect, goalRect);
            //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for games with no possible repetition
        
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
		if(replay!=replayMode.Replay) { playSounds(mm); }
       return (true);
    }
     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for hex, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
      * destination chip disappear until the animation is finished.
      * @param replay
      */
//     void startBoardAnimations(replayMode replay)
//     {
//        if(replay!=replayMode.Replay)
//     	{
//     		double full = G.distance(0,0,G.Width(boardRect),G.Height(boardRect));
//        	while(bb.animationStack.size()>1)
//     		{
//     		HavannahCell dest = bb.animationStack.pop();
//     		HavannahCell src = bb.animationStack.pop();
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
    public commonMove ParseNewMove(String st, int player)
    {
        return (new XehMovespec(st, player));
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
    	  boolean oknone = (nmove.op==MOVE_DROP);
    	  commonMove rval = EditHistory(nmove,oknone);
    	  XehState state = bb.getState();
    	  if((rval!=null)
    		 && (state==XehState.Confirm)
    	     && (rval.op==MOVE_DROPB))
    	  {
    		  // a peculiarity of the hex engine, if we have dropped a stone,
    		  // we can change our mind by simply dropping another stone elsewhere,
    		  // or dropping it back in the pool, or picking up a new stone from the pool.
    		  // Most games do not need this extra logic.
    		  int idx = History.size()-1;
    		  while(idx>=0)
    		  {	XehMovespec oldMove = (XehMovespec)History.elementAt(idx);
    		  	switch(oldMove.op)
    		  	{
    		  	case MOVE_DONE:
    		  	case MOVE_START:
    		  	case MOVE_EDIT: idx = -1;
    		  		break;
    		  	default: 
    		  		if(oldMove.nVariations()>0) { idx=-1; }
    		  			else 
    		  			{ 
    		  				popHistoryElement(); 
    		  				idx--;
    		  			}
    		  		break;
    		  	}
    		  }
    	  }
    	     
    	  return(rval);
      }

    
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
    //public void verifyGameRecord()
    //{	super.verifyGameRecord();
    //}
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
        if (hp.hitCode instanceof XehId)// not dragging anything yet, so maybe start
        {
        XehId hitObject =  (XehId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    
 	    case Black_Chip_Pool:
 	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick " + hitObject.shortName);
	    	break;
	    case BoardLocation:
	        XehCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    	break;
        }
        }
    }
	private void doDropChip(char col,int row)
	{	XehState state = bb.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case Puzzle:
		{
		XehChip mo = bb.pickedObject;
		if(mo==null) { mo=bb.lastPicked; }
		if(mo==null) { mo=bb.getPlayerChip(bb.whoseTurn); }
		PerformAndTransmit("dropb "+mo.id.shortName+" "+col+" "+row);
		}
		break;
		case Confirm:
		case Play:
		case PlayOrSwap:
			XehChip mo=bb.getPlayerChip(bb.whoseTurn);	
			PerformAndTransmit("dropb "+mo.id.shortName	+ " "+col+" "+row);
			break;
					                 
		
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
       	if(!(id instanceof XehId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        XehId hitCode = (XehId)id;
        XehCell hitObject = hitCell(hp);
		XehState state = bb.getState();
        switch (hitCode)
        {
        default:
             	throw G.Error("Hit Unknown: %s", hitObject);
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Confirm:
			case Play:
			case PlayOrSwap:
				if(!bb.isDest(hitObject))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					throw G.Error("shouldn't hit a chip in state %s",state);
					}
				// fall through and pick up the previously dropped piece
				//$FALL-THROUGH$
			case Puzzle:
				PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
				break;
			}
			break;
			
        case EmptyBoard:
			doDropChip(hitObject.col,hitObject.row);
			break;
			
        case Black_Chip_Pool:
        case White_Chip_Pool:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+bb.pickedObject.id.shortName);
			}
           break;
 
        }
        }
    }



    private boolean setDisplayParameters(XehBoard gb,Rectangle r)
    {	boolean complete = false;
      	if(doRotation!=lastRotation)		//if changing the whole orientation of the screen, unusual steps have to be taken
      	{ complete=true;					// for sure, paint everything
      	  lastRotation=doRotation;			// and only do this once
      	  if(doRotation)
      	  {
      	  // 0.95 and 1.0 are more or less magic numbers to match the board to the artwork
          gb.SetDisplayParameters(0.95, 1.0, 0,0,60); // shrink a little and rotate 60 degrees
     	  }
      	  else
      	  {
          // the numbers for the square-on display are slightly ad-hoc, but they look right
          gb.SetDisplayParameters( 0.825, 0.94, 0,0,28.2); // shrink a little and rotate 30 degrees
      	  }
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
     * is game specific, but for most games the underlying empty board is cached as a
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
    public String sgfGameType() { return(Xeh_SGF); }	// this is the official SGF number assigned to the game

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
    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);

        if(target==rotationOption)
        {	handled=true;
        	doRotation = rotationOption.getState();
        	resetBounds();
        	repaint(20);
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

    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }


/** this is used by the scorekeeper to determine who won. Draws are indicated
 * by both players returning false.  Be careful not to let both players return true!
 */
    //public boolean WinForPlayer(commonPlayer p)
    //{ // this is what the standard method does
      // return(getBoard().WinForPlayer(p.index));
    //  return (super.WinForPlayer(p));
    //}

    /** start the robot.  This is used to invoke a robot player.  Mainly this needs 
     * to know the class for the robot and any initialization it requires.  The return
     * value is the player actually started, which is normally the same as requested,
     * but might be different in some games, notably simultaneous play games like Raj
     *  */
   // public commonPlayer startRobot(commonPlayer p,commonPlayer runner,Bot bot)
    //{	// this is what the standard method does:
    	// int level = sharedInfo.getInt(sharedInfo.ROBOTLEVEL,0);
    	// RobotProtocol rr = newRobotPlayer();
    	// rr.InitRobot(sharedInfo, getBoard(), null, level);
    	// p.startRobot(rr);
    //	return(super.startRobot(p,runner,bot));
    //}
    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new XehPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     */
    public void ReplayMove(sgf_node no)
    {
        String comments = "";
        sgf_property prop = no.properties;
        String trainingData = null;
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
            else if (name.equals(game_property) && value.equalsIgnoreCase("hex"))
            {
            	// the equals sgf_game_type case is handled in replayStandardProps
            }
           else if (parseVersionCommand(name,value,2)) {}
           else if (parsePlayerCommand(name,value)) {}
           else if(XehPlay.TrainingData.equals(name)) 
           {	trainingData = value;
           }
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
        if(trainingData!=null)
        {
        	commonMove m = getCurrentMove();
    	    if(m!=null) 
    	    	{ m.setProperty(XehPlay.TrainingData,trainingData); 
    	    	}
        }
    }

	public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit) {
		throw G.Error("Not needed with manual layout");
	}

}

