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
package blooms;

import bridge.*;
import com.codename1.ui.geom.Rectangle;

import static blooms.Bloomsmovespec.*;

import online.common.*;
import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.TextButton;
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
 * <br>BloomsViewer - this class, a canvas for display and mouse handling
 * <br>BloomsBoard - board representation and implementation of the game logic
 * <br>Bloomsmovespec - representation, parsing and printing of move specifiers
 * <br>BloomsPlay - a robot to play the game
 * <br>KulamiConstants - static constants shared by all of the above.  
 *  <p>
 *  The primary purpose of the KulamiViewer class is to do the actual
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
 *  
*/
public class BloomsViewer extends CCanvas<BloomsCell,BloomsBoard> implements BloomsConstants, GameLayoutClient
{	
	static final String Blooms_SGF = "blooms"; // sgf game number

	// file names for jpeg images and masks
	static final String ImageDir = "/blooms/images/";

     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(252,255,255);
    private Color rackBackGroundColor = new Color(178,174,167);
    private Color boardBackgroundColor = new Color(220,165,155);
    

     
    // private state
    private BloomsBoard bb = null; //the board from which we are displaying
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
    private Rectangle chipRects[] = addZoneRect("chip",2);
    private Rectangle[] scoreBox = addRect("score",2);
    
    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	BloomsChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = BloomsChip.Icon.image;
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
       	int players_in_game = Math.max(chipRects.length,info.getInt(OnlineConstants.PLAYERS_IN_GAME,chipRects.length));
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default         
        
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	BloomsConstants.putStrings();
        }
         
        
        String type = info.getString(GAMETYPE, BloomsVariation.blooms_4.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new BloomsBoard(type,players_in_game,randomKey,getStartingColorMap(),BloomsBoard.REVISION);
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
    

    int SQUARESIZE;
    public void setLocalBounds(int x, int y, int width, int height)
    {	
    	setLocalBoundsV(x,y,width,height,aspects);
    }
    private double aspects[] = {0.8,1.3,1.6};
    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*16;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int nrows = 20;  
        int ncols = 24;
        int buttonW = fh*8;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.65,	// 60% of space allocated to the board
    			aspect,	// aspect ratio for the board
    			fh*2,	// minimum cell size
    			fh*3,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);


    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/(ncols),(double)mainH/nrows);
        CELLSIZE = (int)cs;
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
        int stateH = CELLSIZE;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        return(boardW*boardH);
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
       	Rectangle done = doneRects[player];
       	Rectangle score = scoreBox[player];
    	int doneW = plannedSeating()? unitsize*4 : 0;
    	int chipw =unitsize*3/2;
    	int u3 = unitsize*3;
    	G.SetRect(chip, x, y, chipw, chipw*2);
    	G.SetRect(score, x+chipw, y, u3, unitsize*3);
       	Rectangle box =  pl.createRectangularPictureGroup(x+chipw+u3+unitsize/3,y,unitsize);
    	G.SetRect(done,x,G.Bottom(score)+unitsize/3,doneW,doneW/2);
    	G.union(box, done,chip,score);
    	pl.displayRotation = rotation;
    	return(box);
    }



	// draw a box of spare chips. For Blooms it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,BloomsBoard gb)
    {
    	
            int spacey = G.Height(r)/2;
            int xp = G.centerX(r);
            int yp = G.Top(r)+spacey/2;
            int w = G.Width(r)*7/8;
            for(BloomsCell c : gb.playerCells[player])
            {	boolean canHit = gb.LegalToHitChips(c.topChip());
            	if(c.drawChip(gc, this, canHit?highlight:null,w,xp,yp,null))
            	{
            		highlight.spriteColor = Color.RED;
            		highlight.awidth = CELLSIZE;
            	}
            	yp += spacey;
            }
     }
    private void drawScore(Graphics gc,Rectangle r,int n,int cap)
    {
    	if(bb.chips_on_board>4)
    	{
    		GC.setFont(gc, largeBoldFont());
    		GC.Text(gc, true, r, Color.black, null, cap+" / "+n);
    	}
		GC.frameRect(gc, Color.black, r);
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
    	BloomsChip.getChip(obj).drawChip(g,this,bb.cellSize(), xp, yp, null);
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
      setDisplayParameters(bb,boardRect);
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     BloomsChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       BloomsChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      bb.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

      // draw the tile grid.  The positions are determined by the underlying board
      // object, and the tile itself if carefully crafted to tile the Blooms board
      // when drawn this way.  For games with simple graphics, we could use the
      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
      // but for more complex graphics with overlapping shadows or stacked
      // objects, this double loop is useful if you need to control the
      // order the objects are drawn in.
      int left = G.Left(boardRect);
      int top = G.Bottom(boardRect);
              BloomsChip tile = BloomsChip.hexTileNR;
              int xsize = bb.cellSize();//((lastRotation?0.80:0.8)*);
               
      for(Enumeration<BloomsCell>cells = bb.getIterator(Itype.TBRL); cells.hasMoreElements();)
      { //where we draw the grid
    	  BloomsCell cell = cells.nextElement();
    	  int ypos = top - bb.cellToY(cell);
    	  int xpos = left + bb.cellToX(cell);
    	  tile.getAltDisplayChip(cell.col*cell.row^cell.row).drawChip(gc,this,xsize,xpos,ypos,null);              
       }

     }
    private String goalMessage(EndgameCondition m)
    {
    	if(m.ncaptured==0) { return BloomsVictoryCondition; }
    	else
    	{
    		return s.get(CaptureWinMessage,m.ncaptured);
    	}
    }
    private void showEndgameOptions(Graphics gc,BloomsBoard gb,Rectangle brect,HitPoint highlight)
    {
    	Rectangle r = G.copy(null,brect);
    	G.insetRect(r,G.Width(r)/5);
    	StockArt.Scrim.image.stretchImage(gc, r);  
    	GC.frameRect(gc,Color.black,r);
    	int step = G.Height(stateRect);
    	int left = G.Left(r);
    	int top = G.Top(r);
    	int width = G.Width(r);
    	int centerX = G.centerX(r);
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,true,left,top,width,step, Color.black,null,s.get(SelectGoalMessage));
    	EndgameCondition current = gb.endgameCondition;
    	FontMetrics fm = G.getFontMetrics(largeBoldFont());
    	int xstep = fm.stringWidth(s.get(ShortGoalMessage,20));
    	int optionY = top+step*3;
    	int optionX = centerX-xstep*2;
    	int optionX0 = optionX;
    	int stepn = 0;
    	EndgameCondition options[] = EndgameCondition.values();
    	
    	for(EndgameCondition option : options)
    	{	boolean selected = option==current;
    		TextButton b = null;
    		if(option==EndgameCondition.Territory)
    		{
    			b = new TextButton(s.get(ShortTerritory),
    								BloomsId.Select,
    								goalMessage(option),
    								Color.white,null,null); 
    

    		}
    		else
    		{
    			b = new TextButton(s.get(ShortGoalMessage,option.ncaptured),
    								BloomsId.Select,
    								goalMessage(option),
    								Color.white,null,null);
    		}
    		G.SetRect(b,optionX,optionY,xstep,step);
			optionX += xstep;
			stepn++;
			if(stepn%4==0) { optionX = optionX0; optionY+=step;}
    		b.textColor = selected ? Color.yellow : Color.lightGray;
    		if(b.draw(gc,highlight))
    		{
    			highlight.hitObject = option;
    		}   		
    	}
    	int approveY = optionY+step*2;
    	int approveX = centerX-xstep*2+xstep/4;
    	commonPlayer ap = getActivePlayer();
    	for(int i=0;i<2;i++)
    	{	boolean approved = gb.endgameApproved[i];
    		TextButton b = new TextButton(prettyName(i),
    							BloomsId.Approve,
    							s.get(ApproveMessage),
    							HighlightColor,boardBackgroundColor,boardBackgroundColor);
    		b.textColor = Color.black;
    		G.SetRect(b,approveX,approveY,xstep*3/2,step);
    		if(b.draw(gc,i==ap.boardIndex||G.offline() ? highlight : null))
    		{	
    			highlight.hit_index = i;
    		}
    		if(approved)
    		{
    			StockArt.Checkmark.drawChip(gc,this,step,approveX+step/2,approveY+step/2,null);
    		} 	
    	approveX += xstep*2;
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
    public void drawBoardElements(Graphics gc, BloomsBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
    	BloomsState state = gb.getState();
        numberMenu.clearSequenceNumbers();
        
    	if(state==BloomsState.SelectEnd)
    	{
    		showEndgameOptions(gc,gb,brect,highlight);
    	}
    	else {
        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        BloomsCell closestCell = gb.closestCell(highlight,brect);
        boolean hitCell = gb.LegalToHitBoard(closestCell);
        if(hitCell)
        { // note what we hit, row, col, and cell
          boolean empty = closestCell.isEmpty();
          boolean picked = (gb.pickedObject!=null);
          highlight.hitCode = (empty||picked) ? BloomsId.EmptyBoard : BloomsId.BoardLocation;
          highlight.hitObject = closestCell;
          highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
          highlight.awidth = CELLSIZE;
        }
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        if (gc != null)
        {
        for(BloomsCell cell = gb.allCells; cell!=null; cell=cell.next)
          {
            boolean drawhighlight = (hitCell && (cell==closestCell)) 
   				|| gb.isDest(cell) 		// is legal for a "drop" operation
   				|| gb.isSource(cell);	// is legal for a "pick" operation+
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            numberMenu.saveSequenceNumber(cell,xpos,ypos);
  
            if (drawhighlight)
             { // checking for pointable position
            	 StockArt.SmallO.drawChip(gc,this,(gb.cellSize()*5),xpos,ypos,null);                
             }
            cell.drawChip(gc,this,highlight,gb.cellSize(),xpos,ypos,null);
            
            }
        }}
        numberMenu.drawSequenceNumbers(gc,gb.cellSize(),labelFont,labelColor);

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
    {  BloomsBoard gb = disB(gc);
       BloomsState state = gb.getState();
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
       drawBoardElements(gc, gb, boardRect, ourTurnSelect);
       boolean planned = plannedSeating();
       for(int i=0;i<chipRects.length;i++)
       	{  commonPlayer pl = getPlayerOrTemp(i);
       	   pl.setRotatedContext(gc, selectPos, false);
     	   DrawChipPool(gc, chipRects[i],i, ourTurnSelect,gb);
           drawScore(gc,scoreBox[i],gb.scoreForPlayer(i),gb.capturesForPlayer(i^1));
           if(planned && (bb.whoseTurn==i))
           { handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
           		HighlightColor, rackBackGroundColor);
           }      
           pl.setRotatedContext(gc, selectPos, true);
       	}
       GC.setFont(gc,standardBoldFont());
       

		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

		if (state != BloomsState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned) 
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);

        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==BloomsState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        standardGameMessage(gc,
        			messageRotation,
            		state==BloomsState.Gameover?gameOverMessage():s.get(state.description()),
            				state!=BloomsState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        BloomsChip colors[] = gb.playerColors[gb.whoseTurn];
        colors[0].drawChip(gc,this,iconRect,null);
        colors[1].drawChip(gc, this, G.Width(iconRect),G.centerX(iconRect),G.Bottom(iconRect),null);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,goalMessage(gb.endgameCondition),progressRect, goalRect);
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
		if(replay!=replayMode.Replay) { playSounds(mm); }
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
        return (new Bloomsmovespec(st, pl));
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
    	  return(rval);
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
        if (hp.hitCode instanceof BloomsId)// not dragging anything yet, so maybe start
        {
        BloomsId hitObject =  (BloomsId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    
 	    case Red_Chip_Pool:
 	    case Green_Chip_Pool:
 	    case Blue_Chip_Pool:
 	    case Orange_Chip_Pool:
	    	PerformAndTransmit("Pick " + hitObject.shortName);
	    	break;
	    case BoardLocation:
	        BloomsCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    	break;
        } 
        }
    }
	private void doDropChip(char col,int row)
	{	BloomsState state = bb.getState();
		BloomsChip mo=bb.pickedObject;	
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case Puzzle:
			{
				if(mo==null) { mo=bb.lastPicked; }
			}
			break;
		case Play1:
		case Play1Capture:
			if(mo==null) { mo=bb.otherColor(); }
			break;
		case Confirm:
		case Play:
		case PlayFirst:
		case PlayLast:
			break;
		}
		if(mo!=null)
		{
		PerformAndTransmit("dropb "+mo.id.shortName	+ " "+col+" "+row);
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
       	if(!(id instanceof BloomsId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        BloomsId hitCode = (BloomsId)id;
        BloomsCell hitObject = hitCell(hp);
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case Select:
        	{
        	commonPlayer ap = getActivePlayer();
        	PerformAndTransmit("Eselect "+bb.playerColor(ap.boardIndex).id.shortName()+" "+hp.hitObject);
        	}
        	break;
        case Approve:
        	{
            	PerformAndTransmit("EApprove "+bb.playerColor(hp.hit_index).id.shortName());
        	}
        	break;
        case BoardLocation:	// we hit an occupied part of the board 		
        case EmptyBoard:
			doDropChip(hitObject.col,hitObject.row);
			break;
			
        case Red_Chip_Pool:
        case Blue_Chip_Pool:
        case Green_Chip_Pool:
        case Orange_Chip_Pool:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+bb.pickedObject.id.shortName);
			}
           break;
 
        }
        }
    }



    private boolean setDisplayParameters(BloomsBoard gb,Rectangle r)
    {
      	boolean complete = false;

          // the numbers for the square-on display are slightly ad-hoc, but they look right
      	gb.SetDisplayParameters( 1, 0.94, 0,-0.4,28.2); // shrink a little and rotate 30 degrees
      	gb.SetDisplayRectangle(r);
      	if(complete) { generalRefresh(); }
      	return(complete);
    }
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     * <p>
     * if complete is true, we definitely want to start from scratch, otherwise
     * only the known changed elements need to be painted.  Exactly what this means
     * is game specific, but for Blooms the underlying empty board is cached as a deep
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
    public String sgfGameType() { return(Blooms_SGF); }	// this is the official SGF number assigned to the game

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



  
    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new BloomsPlay());
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

    //
    // some magic in service of the preliminary setup phase.
    //
    public boolean PerformAndTransmit(commonMove m,boolean transmit,replayMode replay)
    {	//
    	// if stray ephemeral moves arrive after we have finalized the setup, just
    	// flush them.  This can occur if there is a realtime race between confirming
    	// the setup and the other player changing his mind about the setup.
    	//
    	if(!bb.getState().simultaneousTurnsAllowed()
    		&& m.isEphemeral()) 
    		{ return true;
    		}

    	//
    	// select sets the start of the real game.   canonicalizeHistory will
    	// discard all the ephemeral moves and replaces them with the final
    	// selection.
    	//
    	boolean v = super.PerformAndTransmit(m,transmit,replay);
    	if(v && m.op==SELECT)
    	{	m.setLineBreak(true);
    		canonicalizeHistory();
    	}
    	return v;
    }

    public commonMove convertToSynchronous(commonMove m)
    {	
  	  return null;
    }

     public void ViewerRun(int n)
        {
        	super.ViewerRun(n);
        	BloomsState state = bb.getState();
        	if(state.simultaneousTurnsAllowed())
        	{
            if(!reviewOnly 
          	 && !reviewMode() 
          	 && (bb.allApproved())
          	 && (G.offline()||(bb.whoseTurn == getActivePlayer().boardIndex)))
            	{	  
          	  	PerformAndTransmit("Select R "+bb.endgameCondition);
          	  	// test rejection of surplus ephemeral moves
          	  	// PerformAndTransmit("ESelect R Capture5");
            	}
            else {
            // run async robots. Normally robots only run when it is the robot's turn
            // and not at all when simultaneous turns are in effect.   For blooms, the
            // robot will only accept whatever endgame option the user wants, so all it
            // will do is accept what the user selects.
            //
            for(commonPlayer pp : players)
            { if( canStartRobotTurn(pp))
            	{
            	CommonMoveStack all = bb.GetListOfLegalMoves(pp.boardIndex);
            	if(all.size()>0)
            		{	// robot generates moves only if it hasn't accepted the
            			// or needs to confirm the final selection.
            			startRobotTurn(pp);
            		}
            	}
            }}
  
        	}}
     
 	public int getLastPlacement(boolean empty) {
		return bb.lastPlacement;
	}
    	}

