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
package lyngk;

import javax.swing.JCheckBoxMenuItem;

import common.GameInfo;

import java.awt.*;

import static lyngk.LyngkMovespec.*;
import online.common.*;
import java.util.*;

import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Toggle;
import online.game.*;
import online.game.NumberMenu.NumberingMode;
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
 * <br>LyngkViewer - this class, a canvas for display and mouse handling
 * <br>LyngkBoard - board representation and implementation of the game logic
 * <br>LyngkMovespec - representation, parsing and printing of move specifiers
 * <br>LyngkPlay - a robot to play the game
 * <br>LyngkConstants - static constants shared by all of the above.  
 *  <p>
 *  The primary purpose of the LyngkViewer class is to do the actual
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
 *  <li> use eclipse refactor to rename the package for "lyngk" and for individual files
 *  <li> duplicate the lyngk start configuration, making a new one for the new game
 *  <li> launch the new game and get it to start, still identical to the old lyngk in all but name.
 *  	this will probably require a few edits to the init code.
 *  <li> do a cvs update on the original lyngk hierarchy to get back the original code.
 *  
*/
public class LyngkViewer extends CCanvas<LyngkCell,LyngkBoard> implements LyngkConstants
{		
	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
    static final String Lyngk_SGF = "lyngk"; // sgf game number allocated for lyngk

    // file names for jpeg images and masks
    static final String ImageDir = "/lyngk/images/";

     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private double CELLSPACING = 0.15;	// vertical spacing for stacked cells
    private Color chatBackgroundColor = new Color(240,235,240);
    private Color rackBackGroundColor = new Color(220,215,220);
    private Color boardBackgroundColor = new Color(220,165,155);
    

     
    // private state
    private LyngkBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private int SQUARESIZE;	// size of the cell for the board only
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addZoneRect("chip",2);
    private Rectangle captureRects[] = addZoneRect("Captures",2);
    private Rectangle tiebreakRects[] = addZoneRect("TieBreak",2);
    private Rectangle unclaimedChipRect = addRect("unclaimed chips");
    private Rectangle rotateRect = addRect("rotateRect");
    private Toggle eyeRect = new Toggle(this,"eye",
			StockArt.NoEye,LyngkId.ShowMoves,NoeyeExplanation,
			StockArt.Eye,LyngkId.ShowMoves,EyeExplanation
			);

	// private menu items
    private JCheckBoxMenuItem rotationOption = null;		// rotate the board view
    private boolean doRotation=false;					// current state
    private boolean lastRotation=!doRotation;			// user to trigger background redraw
    private boolean lastPerspective = false;
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	LyngkChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	gameIcon = LyngkChip.lift.image;
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
        enableAutoDone = true;
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default

        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	LyngkConstants.putStrings();
        }
         
        rotationOption = myFrame.addOption("rotate board",false,deferredEvents);
        
        String type = info.getString(GameInfo.GAMETYPE, LyngkVariation.lyngk.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new LyngkBoard(type,players_in_game,randomKey,LyngkBoard.REVISION);
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

   
    public Rectangle createPlayerGroup(int player,int x,int y,double rot,int unit)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int chipW = unit*5;
    	int doneW = chipW;
    	int c2 = unit/2;
    	Rectangle done = doneRects[player];
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unit);
    	Rectangle chipRect = chipRects[player];
    	Rectangle captures = captureRects[player];
    	Rectangle tieBreak = tiebreakRects[player];
		// a pool of chips for the first player at the top
        G.SetRect(chipRect,x,y,chipW,unit*3);
        int captureW = unit*11;
        int captureY = G.Bottom(box);
        int captureH = unit*5;
        G.SetRect(done,G.Right(box)+c2,y+c2,plannedSeating()?doneW:0,doneW/2); 
        G.SetRect(captures,x+chipW,captureY,captureW,captureH);
        G.SetRect(tieBreak,x+chipW+captureW,captureY,unit*2,captureH);
        pl.displayRotation = rot;
        G.union(box, chipRect,captures,tieBreak,done);
    	return(box);
    }

    public void setLocalBounds(int x, int y, int width, int height)
    {	
    	setLocalBoundsV(x,y,width,height,new double[] {1,-1});
    }
    boolean positionBelow = false;
    public double setLocalBoundsA(int x,int y,int width,int height,double v)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	positionBelow = v<0;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*20;	
    	int vcrW = fh*16;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int nrows = 20;  
        int margin = fh/2;
 
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 70% of space allocated to the board
    			1.2,	// aspect ratio for the board
    			(int)(fh*2.0),	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
        
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW, minLogH);
    	layout.placeTheVcr(this,vcrW,vcrW*3/2);
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)(mainW)/(nrows+(positionBelow?1:3)),(double)mainH/(nrows+(positionBelow?3:1)));
    	SQUARESIZE = (int)cs;
    	CELLSIZE = SQUARESIZE/2;
    	int C2 = CELLSIZE/2;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
        int stateH = fh*5/2; 
        int boardW = (int)(nrows*cs);
    	int boardH = (int)(nrows*cs);
    	int unclaimedW = positionBelow ? SQUARESIZE*10 : SQUARESIZE*2;
    	int unclaimedH = positionBelow ? SQUARESIZE*2 : SQUARESIZE*10;
    	int extraW = Math.max(0, (mainW-boardW-(positionBelow ? 0 : unclaimedW))/2);
    	int extraH = Math.max(0, (mainH-boardH-stateH-(positionBelow ? unclaimedH : 0))/2);
    	int boardY = extraH+mainY+stateH;
    	int boardX = mainX+extraW+(positionBelow ? 0 : unclaimedW);
    	if(positionBelow)
    	{
    	int unclaimedY = boardY+boardH;
    	G.SetRect(unclaimedChipRect,(boardX+boardW)/2-unclaimedW/2,unclaimedY,unclaimedW,unclaimedH);
    	}
    	else
    	{
    	int unclaimedX = mainX+extraW+SQUARESIZE/4;
    	G.SetRect(unclaimedChipRect,unclaimedX,boardY+(boardH-unclaimedH)/2,unclaimedW,unclaimedH);
    	}
       	layout.returnFromMain(extraW,extraH);

    	int boardBottom = boardY+boardH;
    	int boardRight = boardX + boardW;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
    	placeRow(boardX,boardY-stateH, boardW, stateH,stateRect,annotationMenu,eyeRect,numberMenu,noChatRect);

    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW,stateH,goalRect,liftRect,rotateRect,viewsetRect);       
        setProgressRect(progressRect,goalRect);
  
            // "done" rectangle, should always be visible, but only active when a move is complete.
            int bw = SQUARESIZE*3;
            int bh = bw/2;
        int doneX = boardRight-bw-C2;
        int doneY = boardBottom - bh*2-CELLSIZE;
            G.SetRect(doneRect, 
        		doneX,
        		doneY,
            		bw,
            		bh);
            G.AlignXY(editRect, 
        		boardX,
        		doneY,
            		doneRect);
 
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        return boardW*boardH;
    }
    private boolean drawStack(boolean perspective,Graphics gc,HitPoint highlight,LyngkBoard gb,LyngkCell c,int x,int y,
    		double ystep,String msg)
    {	boolean hit = false;
    	int sz = (int)(gb.cellSize() * (usePerspective()? 1.0 : 1.08));
    	boolean canDrop = gb.movingObjectIndex()>=0;
    	boolean show = eyeRect.isOnNow();
    	if(c.drawStack(gc, this, highlight, sz, x,y,  0, perspective?0:-ystep,perspective?ystep:0,msg))
    	{
		highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
    	highlight.awidth = sz/2;
    	highlight.spriteColor = Color.red;
    	highlight.hitCode = c.rackLocation();
		hit=true;
    	}
		if(show && highlight!=null)
		{
			StockArt.SmallO.drawChip(gc, this, CELLSIZE*2,x,y,null);
		}
    	return(hit);
    }
    private void DrawUnclaimedPool(Graphics gc, Rectangle r, HitPoint highlight,LyngkBoard gb,Hashtable<LyngkCell,LyngkMovespec> targets)
    {	
    	int w = G.Width(r);
    	int h = G.Height(r);
    	int top = G.Top(r);
    	boolean vertical = w<h;
    	int left = G.Left(r);
   
     	for(LyngkCell c : gb.unclaimedColors)
    	{	boolean canHit = gb.LegalToHitChips(c,gb.whoseTurn,targets);
    		drawStack(true,gc, canHit ? highlight : null, gb,c,
    				left+(vertical? w/2:h/2),
    				top+(vertical ? w/2 :  h/2),CELLSPACING,null);
      		if(vertical) { top += w; } else { left += h; }
    	}
    }


	// draw a box of spare chips. For lyngk it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,LyngkBoard gb,Hashtable<LyngkCell,LyngkMovespec> targets)
    {
        LyngkCell c = gb.playerColors[player];
        boolean legalToHit = gb.LegalToHitChips(c,gb.whoseTurn,targets);
		if(legalToHit)
		{
			StockArt.SmallO.drawChip(gc, this, CELLSIZE*4,G.centerX(r),G.centerY(r), null);
		}
		int cx = G.centerX(r);
		int cy = G.Top(r)+G.Width(r)/2;
		drawStack(true,gc, legalToHit ? highlight : null, gb,c, cx,cy,-0.6,null);
    }
    

	// draw a box of spare chips. For lyngk it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawCapturePool(Graphics gc, Rectangle r, Rectangle ar,int player, HitPoint highlight,LyngkBoard gb,Hashtable<LyngkCell,LyngkMovespec> targets)
    {
        LyngkCell c = gb.captures[player];
        int unit = G.Width(r)/7;
        LyngkCell spare = new LyngkCell(c.rackLocation());
        int lim = c.height()-c.activeAnimationHeight();
        int xp = G.Left(r)+unit;
        int yp = G.Bottom(r)-unit;
        if(lim>0)
        {
        int dx = Math.min(unit*2, (G.Width(r)*5)/c.height());
        boolean canHit = gb.LegalToHitChips(c,gb.whoseTurn,targets) && G.pointInRect(highlight, r);
        for(int idx = 0; idx<lim; idx+=5)
        {
        spare.reInit();
        for(int i=0;i<5;i++)
        {
        	if((i+idx)<lim) { spare.addChip(c.chipAtIndex(i+idx));}
        }
        spare.drawStack(gc,this, canHit ? highlight : null,unit*2, xp,yp,0,CELLSPACING,null);
        xp += dx;
        }}
       // set the cell location for animations
        commonPlayer pl = getPlayerOrTemp(player);
        pl.rotateCurrentCenter(c,xp,yp);
		long score = gb.adjustedScoreForPlayer(player);
		int step = G.Height(ar)/5;
		int ypos = G.Bottom(ar)-step;
		int xpos= G.Left(ar);
		int w = G.Width(ar);
		for(int i=1;i<=5;i++)
		{	int rem = (int)(score%100);
			score = score/100;
			GC.Text(gc, false, xpos, ypos, w,step,Color.black,null,""+i+"/"+rem);
			ypos -= step;
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
    	bb.pickedStack.drawStack(g,this,null,bb.cellSize(), xp, yp,0,CELLSPACING,null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  

    public boolean usePerspective() { return(getAltChipset()==0); }
    Image scaled = null;
    Image background = null;
    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { // erase
      LyngkBoard gb = disB(gc);
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     LyngkChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       LyngkChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      boolean perspective = usePerspective();
      Image image = (perspective? LyngkChip.board : LyngkChip.boardFlat).getImage(loader);
      if(image!=background) { scaled = null;}
      background = image;
      scaled = image.centerScaledImage(gc, boardRect,scaled);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      //  DrawGridCoord(gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
	  setDisplayParameters(gb,boardRect);
      gb.DrawGrid(gc, boardRect, use_grid, Color.black,  Color.black,  Color.black,Color.black);

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
    public void drawBoardElements(Graphics gc, LyngkBoard gb, Rectangle brect, HitPoint highlight,Hashtable<LyngkCell,LyngkMovespec> targets)
    {	
    	boolean dolift = doLiftAnimation();
     	numberMenu.clearSequenceNumbers();
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
    	LyngkCell last = gb.lastMove[nextPlayer[gb.whoseTurn]];
    	boolean perspective = usePerspective();
        Enumeration<LyngkCell>cells = gb.getIterator(Itype.TBRL);
        boolean show = eyeRect.isOnNow();
        boolean numbers = numberMenu.selected()!=NumberingMode.None;
    	while(cells.hasMoreElements())
          { LyngkCell cell = cells.nextElement();
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            numberMenu.saveSequenceNumber(cell,xpos,ypos);
            
            boolean canHit = !dolift && gb.LegalToHitBoard(cell,targets);
            drawStack(perspective,gc, canHit ? highlight : null, gb,cell, xpos,ypos,CELLSPACING+0.01*liftSteps,!numbers&&(cell==last)?".":null);
      		if(show && (targets.get(cell)!=null))
    		{
    			StockArt.SmallO.drawChip(gc, this, CELLSIZE*2,xpos,ypos,null);
    		}
    		if((cell.height()==CaptureHeight)
    			&& gb.variation.removeStacksOf5()
    			&& gb.isOwnedBy(cell.topChip(),gb.whoseTurn))
    			{ StockArt.SmallX.drawChip(gc,this,CELLSIZE*2,xpos,(int)(ypos-CELLSIZE*CELLSPACING*CaptureHeight),null);	
    			}  
    		//StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
          }
    	numberMenu.drawSequenceNumbers(gc,CELLSIZE*3,labelFont,labelColor);
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
    {  LyngkBoard gb = disB(gc);
       LyngkState state = gb.getState();
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
       // hit anytime nothing is being moved, even if not our turn or we are a spectator
       HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;
       
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
       Hashtable<LyngkCell,LyngkMovespec> targets = gb.getTargets();
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,targets);
       boolean planned = plannedSeating();
      
       for(int i=FIRST_PLAYER_INDEX ; i<=SECOND_PLAYER_INDEX; i++)
       {
       commonPlayer pl = getPlayerOrTemp(i);
       pl.setRotatedContext(gc, selectPos,false);
       DrawChipPool(gc, chipRects[i], i, ourTurnSelect,gb,targets);
       DrawCapturePool(gc, captureRects[i], tiebreakRects[i],i, ourTurnSelect,gb,targets);
       if(planned && (i==gb.whoseTurn))
       {
		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
				HighlightColor, rackBackGroundColor);
       }
       pl.setRotatedContext(gc, selectPos,true);

       }
       
       DrawUnclaimedPool(gc,unclaimedChipRect,ourTurnSelect,gb,targets);
       GC.setFont(gc,standardBoldFont());
       
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       
       if (state != LyngkState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive())
			{
			handleDoneButton(gc,0,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
			}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos, HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==LyngkState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        standardGameMessage(gc,
        			messageRotation,
            		state==LyngkState.Gameover?gameOverMessage(gb):s.get(state.description()),
            				state!=LyngkState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(LyngkVictoryCondition),progressRect, goalRect);
            //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for lyngk
        
        drawAuxControls(gc,selectPos);
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        drawViewsetMarker(gc,viewsetRect,nonDragSelect);
    }
    
    //
    // support for the last move "numberMenu" logic
    //
	public int getLastPlacement(boolean empty) {
		return (bb.dropStep);
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
         * 
         * Lyngk uses a few slightly nonstandard tricks to animate the multiple bounces
         * implied by "lyngk" moves and by the need to animate whole stacks moving.  The
         * intermediate cells that are bounced over are presented by copies of the actual
         * cell, which inherit the locations but leave the underlying cell with no active
         * animations.  If they had active animations they would disappear!   Moving the
         * stack instead of a single is accomplished by having multiple from-to pairs for
         * each step.
         */
        startBoardAnimations(replay,bb.animationStack,bb.cellSize(),MovementStyle.Chained);
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
    public commonMove ParseNewMove(String st, int player)
    {
        return (new LyngkMovespec(st, player));
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
        if (hp.hitCode instanceof LyngkId)// not dragging anything yet, so maybe start
        {
        LyngkId hitObject =  (LyngkId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
        case Blue_Chip:
        case Red_Chip:
        case Black_Chip:
        case Ivory_Chip:
        case Green_Chip:
        case White_Chip:	    
 	    case FirstPlayer:
 	    case SecondPlayer:
	    	PerformAndTransmit("Pick " + hitObject.shortName);
	    	break;
	    case BoardLocation:
	        LyngkCell hitCell = hitCell(hp);
	        int index = (bb.getState()==LyngkState.Puzzle) ? Math.max(0, hp.hit_index) : 0;
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row+" "+index);
	    	break;
        }
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
       	if(!(id instanceof LyngkId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        LyngkId hitCode = (LyngkId)id;
        LyngkCell hitObject = hitCell(hp);
		LyngkState state = bb.getState();
		boolean moving = bb.movingObjectIndex()>=0;
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case ShowMoves:	eyeRect.toggle(); break;
        case RotateRect:	doRotation = !doRotation; repaint(); break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Confirm:
			case PlayOrClaim:
			case Play:
			case Puzzle:
				if(moving)
				{
					PerformAndTransmit("dropb "+hitObject.col+" "+hitObject.row);
				}
				else
				{
				// in puzzle mode, allow picking partial stacks
				int index = (state==LyngkState.Puzzle)
								? hp.hit_index 
								: 0;
				PerformAndTransmit("pickb "+hitObject.col+" "+hitObject.row+" " + index);
				}
				
				break;
			}
			break;

			
        case Blue_Chip:
        case Red_Chip:
        case Black_Chip:
        case Ivory_Chip:
        case Green_Chip:
        case White_Chip:
        case FirstPlayer:
        case SecondPlayer:
        case FirstCaptures:
        case SecondCaptures:
           if(bb.pickedStack.height()>0) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+hitCode.shortName);
			}
           break;
 
        }
        }
    }

    private void drawAuxControls(Graphics gc,HitPoint highlight)
    {	eyeRect.activateOnMouse = true;
    	eyeRect.draw(gc,highlight);
    
    	LyngkChip swing = doRotation ?  LyngkChip.swing_cw : LyngkChip.swing_ccw;
    	swing.drawChip(gc, this,rotateRect, highlight, swing.id,LyngkSwingBoard );
    	
    	drawLiftRect(gc,liftRect,highlight,LyngkChip.lift.image);
    	
    }

    private boolean setDisplayParameters(LyngkBoard gb,Rectangle r)
    {
      	boolean complete = false;
      	boolean perspective = usePerspective();
      	if((perspective!=lastPerspective) || (doRotation!=lastRotation))		//if changing the whole orientation of the screen, unusual steps have to be taken
      	{ complete=true;					// for sure, paint everything
      	  lastRotation=doRotation;			// and only do this once
      	  lastPerspective = perspective;
      	  if(doRotation)
      	  {
      		  if(perspective)
      		  {
      		  // the numbers for the square-on display are slightly ad-hoc, but they look right
              gb.SetDisplayParameters(1.25, .86, 
            		  	0.82,-0.3,
            		  	66,
            		  	0.03,0.03,
            		  	-0.07); // shrink a little and rotate 30 degrees
      		  }
      		  else
      		  {
                  gb.SetDisplayParameters(1.16, .95, 
              		  	0.72,0.1,
              		  	90,
              		  	0.00,0.00,
              		  	0); // shrink a little and rotate 30 degrees
     			  
      		  }
     
     	  }
      	  else
      	  {
          // the numbers for the square-on display are slightly ad-hoc, but they look right
      	  if(perspective)
      	  {
          gb.SetDisplayParameters(1.02, .89, 
        		  	0.35,-0.4,
        		  	6,
        		  	0.1,0.12,
        		  	0); // shrink a little and rotate 30 degrees
     	  }
      	  else
      	  {
              gb.SetDisplayParameters(1.15, 0.95, 
          		  	0.35,-0.4,
          		  	30,
          		  	0.0,0.0,
          		  	0); // shrink a little and rotate 30 degrees
     		  
      	  }
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
     * is game specific, but for lyngk the underlying empty board is cached as a deep
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
    public String sgfGameType() { return(Lyngk_SGF); }	// this is the official SGF number assigned to the game

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


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new LyngkPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 527/2023
     *  3691 files visited 0 problems
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

}

