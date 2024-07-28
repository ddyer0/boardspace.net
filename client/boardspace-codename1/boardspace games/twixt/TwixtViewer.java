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
package twixt;

import static twixt.Twixtmovespec.*;

import com.codename1.ui.geom.Rectangle;
import bridge.Color;
import bridge.JCheckBoxMenuItem;
import bridge.JMenuItem;
import common.GameInfo;
import online.common.*;
import java.util.*;
import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.Image;
import lib.StockArt;
import lib.TextButton;
import online.game.*;
/**
 * 
 * Overall Architecture
 * 
 * The site provides the lobby, choice game and opponents, communication between the players, information 
 * for spectators,  rankings, and a host of other services.  Each game has to be concerned only with 
 * the game itself.   An individual game (say, Hex) is launched and each client independently initializes
 * itself to a common starting state.   Thereafter each player specifies messages to be broadcast to the
 * other participants, and receives messages being broadcast by the other participants, which keep everyone
 * informed about the state of the game.  There is no common "true" state of the game - all the participants
 * keep in step by virtue of seeing the same stream of messages.    Messages are mostly simple "pick up a stone"
 * "place a stone on space x" and so on.
 * 
 * The things a game must implement are specified by the class "ViewerProtocol", and a game could just
 * start there and be implemented completely from scratch, but in practice there is another huge pile
 * of things that every game has to do; dealing with graphics, mouse events, saving and restoring the
 * game state from static records, replaying and reviewing games and so on.   These are implemented in the 
 * class "commonCanvas" and by several board-like base classes for hexagonal and square geometry boards.   
 * All the existing games for boardspace use these classes to provide graphics and basic board representation.
 * 
 * For games with robot players, there is another huge pile of things that a robot has to do, generating
 * moves, evaluating and choosing the best, and implementing a lookahead several moves deep.   There's a
 * standard framework for this using the "RobotProtocol" class and the "SearchDriver" class. 
 */
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
 * <br>TwixtViewer - this class, a canvas for display and mouse handling
 * <br>TwixtBoard - board representation and implementation of the game logic
 * <br>Twixtmovespec - representation, parsing and printing of move specifiers
 * <br>TwixtPlay - a robot to play the game
 * <br>TwixtConstants - static constants shared by all of the above.  
 *  <p>
 *  The primary purpose of the TwixtViewer class is to do the actual
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
 *  <li> use eclipse refactor to rename the package for "twixt" and for individual files
 *  <li> duplicate the twixt start configuration, making a new one for the new game
 *  <li> launch the new game and get it to start, still identical to the old twixt in all but name.
 *  	this will probably require a few edits to the init code.
 *  <li> do a cvs update on the original twixt hierarchy to get back the original code.
 *  
*/
public class TwixtViewer extends CCanvas<TwixtCell,TwixtBoard> implements TwixtConstants,PlacementProvider
{	static final long serialVersionUID = 1000;
     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(245,245,250);
    private Color rackBackGroundColor = new Color(245,245,250);
    private Color boardBackgroundColor = new Color(220,165,155);
    private JMenuItem offerDrawAction = null;
    
    public boolean usePerspective() { return(super.getAltChipset()==0); }
    
    // private state
    TwixtBoard bb = null; //the board from which we are displaying
 
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
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle rotateRect = addRect("Reverse");
    private Rectangle flatRect = addRect("flat");

	private TextButton swapButton = addButton(SWAP,GameId.HitSwapButton,SwapDescription,
			HighlightColor, rackBackGroundColor);
    private Rectangle acceptDrawRect = addRect("acceptDraw");
    private Rectangle declineDrawRect = addRect("declineDraw");
	private JCheckBoxMenuItem showDistancesItem;
	private JCheckBoxMenuItem showPreferredItem;
	private Rectangle guidelinesRect = addRect("guidelines");
	private boolean guidelinesOn = false;
	private double lineStrokeWidth =  1;
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	TwixtChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	gameIcon = TwixtChip.Icon.image;
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
        	InternationalStrings.put(TwixtStrings);
        	InternationalStrings.put(TwixtStringPairs);
        	
        	showDistancesItem = myFrame.addOption("Show edge distance",false,deferredEvents);
        	showPreferredItem = myFrame.addOption("Show preferred moves",false,deferredEvents);
        }
         
        
        String type = info.getString(GameInfo.GAMETYPE, TwixtVariation.twixt.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new TwixtBoard(type,players_in_game,randomKey,getStartingColorMap(),TwixtBoard.REVISION);
        useDirectDrawing(true); 
        offerDrawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);     
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
    

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*4;
    	int chipH = unitsize*6;
    	int doneW = unitsize*6;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	Rectangle done = doneRects[player];
    	
    	G.SetRect(chip, x, y+unitsize/3, chipW, chipH);
    	G.SetRect(done, x+chipW+unitsize/2,G.Bottom(box),doneW,plannedSeating()?doneW/2:0);
    	
    	pl.displayRotation = rotation;
    	
    	G.union(box, chip,done);
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
        int buttonW = fh*8;
        int margin = fh/2;
       	boolean perspective = usePerspective();
       	double ncols = bb.boardSize;
       	double nrows = perspective ? (bb.boardSize*0.7) : ncols;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			ncols/nrows,	// aspect ratio for the board
    			fh*2.5,	// maximum cell size
    			0.4	// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
    	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
       	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	layout.placeRectangle(swapButton,buttonW, buttonW/2,BoxAlignment.Center);
        layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int stateH = fh*5/2;
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH)/nrows);
    	SQUARESIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(nrows*cs);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-stateH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH+stateH/2;
    	int boardBottom = boardY+boardH;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH/2;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,rotateRect,flatRect,viewsetRect,guidelinesRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	lineStrokeWidth = boardW/600.0;
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH/2,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
 	
    }

    int SQUARESIZE =0;
    double bratio = 1.2;
 
    //
	// reverse view icon, made by combining the stones for two colors.
    //
    private void drawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	TwixtChip image = ((rotation&1)==0) ? TwixtChip.rotate_cw_1 : TwixtChip.rotate_cw_2;
    	image.drawChip(gc,this,r,null);
    	HitPoint.setHelpText(highlight,r,TwixtId.ReverseViewButton,s.get(RotateExplanation));
     }
    private void drawGuidelineIcon(Graphics gc,Rectangle r,HitPoint highlight)
    {
    	TwixtChip c = guidelinesOn ? TwixtChip.GuidelinesOff : TwixtChip.GuidelinesOn;
    	c.drawChip(gc,this,r,highlight,TwixtId.GuidelineButton,s.get(GuidelinesExplanation));
    }
    private void drawFlatMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	TwixtChip image = flat ? TwixtChip.black_peg : TwixtChip.black_peg_flat;
    	TwixtChip image2 = flat ? TwixtChip.red_peg : TwixtChip.red_peg_flat;
    	int w = G.Width(r);
    	GC.frameRect(gc, Color.black, r);
    	image.drawChip(gc,this,2*w/3,G.Left(r)+w/3,G.centerY(r),null);
    	image2.drawChip(gc, this, 2*w/3,G.Left(r)+2*w/3,G.centerY(r),null);
    	if(HitPoint.setHelpText(highlight,r,TwixtId.FlatViewButton,s.get(FlattenExplanation)))
    	{	
    		showFlat = flatLock ? flat : !flat; 
    	}
    	else { showFlat = flat; flatLock = false; }
     }
    private void drawAuxControls(Graphics gc,HitPoint highlight)
    {
        drawReverseMarker(gc,rotateRect,highlight);
        drawFlatMarker(gc,flatRect,highlight);
        drawGuidelineIcon(gc,guidelinesRect,highlight);

    }
	// draw a box of spare chips. For twixt it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,TwixtBoard gb)
    {
    	commonPlayer cp = getPlayerOrTemp(player);
        int vspace = G.Height(r)/4;
        int hspace = G.Width(r)/2;
        int xlim = G.Left(r)+hspace*2;
        int xp = G.Left(r)+hspace/2;
        int yp = G.Top(r)+vspace;
        TwixtCell rack[] = gb.getRack(player);
        boolean toDrop = (gb.pickedObject!=null)  
        					&& G.pointInRect(highlight, r) 
        					&& (gb.pickedObject.color()==gb.getPlayerColor(player));
        boolean hit = toDrop;
        boolean first = true;
        int unit = SQUARESIZE;
        for(TwixtCell c : rack)
        {
            boolean canhit = gb.legalToHitChips(c,player);
            
        	if(c.drawStack(gc, this, (canhit&&!toDrop)?highlight:null,first?unit:unit/2, xp, yp, 0, 0.6,null))
        	{	hit = true;
        	}
        	cp.rotateCurrentCenter(c, xp, yp);
        	xp += hspace;
        	if(first||xp>xlim)
        	{
        		xp = G.Left(r)+hspace/2;
        		yp += vspace;
        		first=false;
        	}
        	
        }
        if(hit | toDrop)
        {
            highlight.arrow = toDrop?StockArt.DownArrow:StockArt.UpArrow;
            highlight.awidth = unit;
            highlight.hit_width = unit;
            highlight.hit_height = unit;
            highlight.spriteColor = Color.red;
            if(toDrop)
            { 	TwixtCell d = gb.getCell(gb.pickedObject);
            	highlight.hitCode = d.rackLocation();
            	highlight.hitObject = d;
            	highlight.spriteRect = r;
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
    	TwixtChip.getChip(obj).drawChip(g,this,bb.cellSize(), xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  

    StockArt arrows[] = {StockArt.SolidUpArrow,StockArt.SolidRightArrow,StockArt.SolidDownArrow,StockArt.SolidLeftArrow};
    private void drawArrow(Graphics gc,char col,int row)
    {
		  int xp1 = G.Left(boardRect)+bb.cellToX(col,row);
	  	  int yp1 = G.Bottom(boardRect)-bb.cellToY(col,row);
	  	  StockArt chip = arrows[rotation&3];
	  	  chip.drawChip(gc, this, 2*SQUARESIZE/3, xp1,yp1,null);

    }
    Image scaled = null;
    TwixtChip background = null;
    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { // erase
      TwixtBoard gb = disB(gc);
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     TwixtChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       TwixtChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      boolean perspective = usePerspective();
      if(perspective)
      {
      if(background!=TwixtChip.board) { scaled = null; }
      background = TwixtChip.board;
      scaled = TwixtChip.board.image.centerScaledImage(gc, boardRect,scaled);
      if((rotation&1)!=0)
      {
      int l = G.Left(boardRect);
      int t = G.Top(boardRect);
      int w = G.Width(boardRect);
      int h = G.Height(boardRect);
      // draw an overlay to make the stripes on the edges correct
      Rectangle leftRect = new Rectangle((int)(l+0.1*w),(int)(t+0.117*h),(int)(0.22*h),(int)(0.74*h));
      TwixtChip.left.image.centerImage(gc,leftRect);
      Rectangle rightRect = new Rectangle((int)(l+0.741*w),(int)(t+0.1080*h),(int)(0.22*h),(int)(0.74*h));
      TwixtChip.right.image.centerImage(gc,rightRect);
      Rectangle topRect = new Rectangle((int)(l+0.211*w),(int)(t+0.0025*h),(int)(0.83*h),(int)(0.2*h));
      TwixtChip.top.image.centerImage(gc,topRect);
      Rectangle botRect = new Rectangle((int)(l+0.16*w),(int)(t+0.757*h),(int)(0.983*h),(int)(0.2*h));
      TwixtChip.bottom.image.centerImage(gc,botRect);
     }}
      else
      {	  int cx = G.centerX(boardRect);
      	  int cy = G.centerY(boardRect);
      	  boolean rotate = (rotation&1)!=0;
      	  if(rotate) { GC.setRotation(gc,Math.PI/2,cx,cy);}
      	  if(background!=TwixtChip.board_np) { scaled = null; }
      	  background = TwixtChip.board_np;
      	  scaled = TwixtChip.board_np.image .centerScaledImage(gc, boardRect,scaled);
          if(rotate) { GC.setRotation(gc,-Math.PI/2, cx, cy); }
     }
      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      setDisplayParameters(gb,boardRect);
      gb.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);
      if(use_grid)
    	  {
    	  drawArrow(gc,'A',1);
    	  drawArrow(gc,(char)('A'+gb.ncols-1),1);
    	  }
      // draw the tile grid.  The positions are determined by the underlying board
      // object, and the tile itself if carefully crafted to tile the twixt board
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
   // public String encodeScreenZone(int x, int y,Point p)
   // {
    //	return(super.encodeScreenZone(x,y,p));
   // }
    boolean inBoardDraw = false;
    int rotation = 0;
    boolean showFlat = false;
    boolean flat = false;
    boolean flatLock = false;
    
    public int getAltChipset()
    {	
    	return( /* temp inBoardDraw*/
    			+(inBoardDraw?((showFlat?100:0)+4):0)
    			+(bb.reverseY()?(bb.reverseX() ? 3: 1 ):(bb.reverseX()?2:0))
    			);
    }
     
    // redraw a bridge if it is present
    double ycellscale = 0.6;
	private boolean showDistances;
	private boolean showPreferred;
    private boolean redrawBridge(Graphics gc,TwixtCell cell,TwixtChip bridge,int sz,int xpos,int ypos)
    {
    	if((cell!=null) && cell.containsChip(bridge))
		{ bridge.drawChip(gc,this,sz,xpos,(int)(ypos-ycellscale*sz),null);
		  return(true);
		}
    	return(false);
    }
    // redraw a bridge if it is present
    private boolean redrawBridge(Graphics gc,TwixtBoard gb,TwixtCell cell,TwixtChip bridge,Rectangle brect)
    {	if((cell!=null) && cell.containsChip(bridge))
    	{
    	int cy = gb.cellToY(cell);
    	int cx = gb.cellToX(cell);
    	int h = G.Height(brect);
    	double scl = 1.0-(usePerspective()?(double)(cy-h/6)/(h*6.25):0);
    	int ypos = G.Bottom(brect) - cy;
    	int xpos = G.Left(brect) + cx;
        int sz = (int)(gb.cellSize()*scl);
        bridge.drawChip(gc,this,sz,xpos,(int)(ypos-(ycellscale*sz)),null);
        return(true);
    	}
    	return(false);
    }
    private boolean redrawPost(Graphics gc,TwixtBoard gb,TwixtCell cell,Rectangle brect)
    {	if((cell!=null) && !cell.isEmpty())
    	{
    	int cy = gb.cellToY(cell);
    	int cx = gb.cellToX(cell);
    	int h = G.Height(brect);
    	double scl = 1.0-(usePerspective()?(double)(cy-h/6)/(h*6.25):0);
    	int ypos = G.Bottom(brect) - cy;
    	int xpos = G.Left(brect) + cx;
        int sz = (int)(gb.cellSize()*scl);
        cell.chipAtIndex(0).drawChip(gc,this,sz,xpos,ypos,null);
        return(true);
    	}
    	return(false);
    }
    private void drawStack(Graphics gc, TwixtBoard gb,TwixtCell cell,HitPoint highlight,Rectangle brect)
    {  	int cy = gb.cellToY(cell);
    	int cx = gb.cellToX(cell);
    	int h = G.Height(brect);
    	boolean perspective = usePerspective();
    	double scl = perspective ? 1.0-(double)(cy-h/6)/(h*4.25) : 1.0 ;
    	int bot = G.Bottom(brect);
    	int left = G.Left(brect);
    	int ypos = bot - cy;
    	int xpos = left + cx;
        int sz = (int)(gb.cellSize()*scl);
        TwixtChip po = gb.pickedObject;
        int who = gb.whoseTurn;
        TwixtState state = gb.getState();
        boolean picked = (po!=null);
        boolean canhit = gb.legalToHitBoard(cell);
        boolean hitCell = (picked ? po.isPeg() : true) &&  canhit;
        //StockArt.SmallO.drawChip(gc, this, sz, xpos,ypos+sz/6,null);
        TwixtCell last = gb.getLastDest();
        if(cell==last)
    	{	TwixtChip lastChip = gb.getLastDestChip();
    		if(lastChip!=null && lastChip.isBridge())
    		{
    		 TwixtCell lastD = last.bridgeTo(lastChip);
    		 if(lastD!=null)
    			 {
    			 int xp = left+gb.cellToX(lastD);
    			 int yp = bot-gb.cellToY(lastD);
    			 StockArt.SmallO.drawChip(gc, this, sz*4, (xpos+xp)/2,(int)((ypos+yp)/2-(showFlat?0:sz*0.5)),null);
    			 }
    		 
    		}
    		else {
    		StockArt.SmallO.drawChip(gc, this, sz*4, xpos,ypos+sz/6,null);
    		}
    	}
    	numberMenu.saveSequenceNumber(cell,xpos,ypos);

    	if(cell.drawStack(gc,this,hitCell?highlight:null,sz,xpos,ypos,0,showFlat?0:ycellscale,null))
    	{	boolean empty = cell.isEmpty();
        	
            highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
            highlight.awidth = SQUARESIZE/2;
            highlight.spriteColor = Color.red;
    	}
    	
    	if((showDistances|showPreferred) && cell.samples>=0 && cell.samples<100)
    	{
    	int q = showPreferred ? cell.samples : cell.distanceToEdge();
        String dis = ""+q;
        GC.Text(gc, true, xpos-SQUARESIZE/2, ypos-SQUARESIZE/2, SQUARESIZE, SQUARESIZE,Color.yellow,null,dis);
    	}
     	if(picked)
    	{
    		if(po.isBridge())
    		{
    			// place this bridge with current cell as the source
    			if(gb.legalToPlaceBridge(cell,po))
    			{	TwixtCell to = cell.bridgeTo(po);
    		   		int yp1 = bot - gb.cellToY(to);
    	    		int xp1 = left+gb.cellToX(to);
    	    		int cy1 = (int)((yp1+ypos)/2-(showFlat?0:(ycellscale*sz)*0.8));
    	    		int cx1 = (xp1+xpos)/2;
    	    		if(cell.findChipHighlight(highlight,po,sz,sz,cx1,cy1))
    	    		{
    	                highlight.arrow = StockArt.DownArrow;
    	                highlight.awidth = SQUARESIZE;
    	                highlight.spriteColor = Color.red;
    	    		}
   				
    			}
    			// place this bridge with current cell as the destination
    			TwixtCell from = cell.bridgeFrom(po);
    			if((from!=null) && gb.legalToPlaceBridge(from,po))
    			{
    		   		int yp1 = bot - gb.cellToY(from);
    	    		int xp1 = left+gb.cellToX(from);
    	    		int cy1 = (int)((yp1+ypos)/2-(showFlat?0:(ycellscale*sz)*0.8));
    	    		int cx1 = (xp1+xpos)/2;
    	    		if(from.findChipHighlight(highlight,po,sz,sz,cx1,cy1))
    	    		{
    	                highlight.arrow = StockArt.DownArrow;
    	                highlight.awidth = SQUARESIZE/2;
    	                highlight.spriteColor = Color.red;
    	    		}
    				
    			}
    	}
    	}
    	else if(gb.legalToPickBridge())
    	{
    	for(int lim=cell.height()-1; lim>0; lim--)
    	{	// look for hit on a bridge
    		TwixtChip bridge = cell.chipAtIndex(lim);
    		if((bridge.color()==gb.getPlayerColor(who)) || (state==TwixtState.Puzzle))
    		{
    		TwixtCell conn = cell.bridgeTo(bridge);
    		G.Assert(conn!=null,"must connect");
    		int yp1 = bot - gb.cellToY(conn);
    		int xp1 = left+gb.cellToX(conn);
    		int cy1 = (int)((yp1+ypos)/2-(showFlat?0:(ycellscale*sz)*0.8));
    		int cx1 = (xp1+xpos)/2;
    		if(cell.findChipHighlight(highlight,bridge,sz,sz,cx1,cy1))
    		{
    			highlight.hitCode = bridge.id;
                highlight.arrow = StockArt.UpArrow;
                highlight.awidth = SQUARESIZE/2;
                highlight.spriteColor = Color.red;
    		}}
    	}}
     	if(!showFlat)
     	{
    	// fixups for the bridges which are always drawn "up" in the natural board coordinates, but
    	// when rotated can point in any direction.  The overall board drawing order is left-right top-bottom
    	switch(rotation)
    	{
    	default: throw G.Error("Not expecting %s",rotation);
    	case 0:
    		// rotation 0, bridges are being draw "up"
    		if(cell.containsChip(TwixtChip.red_bridge_60)||cell.containsChip(TwixtChip.black_bridge_60))
    		{	redrawPost(gc,gb,cell.exitTo(TwixtBoard.CELL_UP_LEFT),brect);
    			redrawBridge(gc,cell,TwixtChip.red_bridge_30,sz,xpos,ypos);
    			redrawBridge(gc,cell,TwixtChip.black_bridge_30,sz,xpos,ypos);
    			redrawPost(gc,gb,cell.exitTo(TwixtBoard.CELL_LEFT),brect);
    		}
    		if(cell.containsChip(TwixtChip.red_bridge_120)||cell.containsChip(TwixtChip.black_bridge_120))
    		{	redrawPost(gc,gb,cell.exitTo(TwixtBoard.CELL_UP_RIGHT),brect);
				redrawBridge(gc,cell,TwixtChip.red_bridge_150,sz,xpos,ypos);
				redrawBridge(gc,cell,TwixtChip.black_bridge_150,sz,xpos,ypos);
    		}
    		if(cell.containsChip(TwixtChip.red_bridge_30)||cell.containsChip(TwixtChip.black_bridge_30))
    		{	
    			redrawPost(gc,gb,cell.exitTo(TwixtBoard.CELL_LEFT),brect);
    		}
    		
    		break;
    	case 1:
    		// rotation 1, bridges are being drawn "right"
    		{
    		TwixtCell dcell = cell.exitTo(TwixtBoard.CELL_DOWN);	// drawn to the left of us
    		redrawBridge(gc,gb,dcell,TwixtChip.red_bridge_120,brect);
    		redrawBridge(gc,gb,dcell,TwixtChip.black_bridge_120,brect);
    		}
    		{
    			TwixtCell dcell = cell.exitTo(TwixtBoard.CELL_DOWN_LEFT);	// drawn to the left of us
    			redrawBridge(gc,gb,dcell,TwixtChip.red_bridge_150,brect);
        		redrawBridge(gc,gb,dcell,TwixtChip.black_bridge_150,brect);
    		}
    		break;
    	case 2:
    		// rotation 2, bridges are being drawn "down"
    		{
    			TwixtCell dcell = cell.exitTo(TwixtBoard.CELL_RIGHT);	// drawn to the left of us
    			redrawBridge(gc,gb,dcell,TwixtChip.red_bridge_30,brect);
        		redrawBridge(gc,gb,dcell,TwixtChip.black_bridge_30,brect);
        		if(dcell!=null)
        		{
        			dcell = dcell.exitTo(TwixtBoard.CELL_RIGHT);
        			redrawBridge(gc,gb,dcell,TwixtChip.red_bridge_30,brect);
            		redrawBridge(gc,gb,dcell,TwixtChip.black_bridge_30,brect);       			
        		}
    		}
    		{
    			TwixtCell dcell = cell.exitTo(TwixtBoard.CELL_DOWN_LEFT);	// drawn to the left of us
    			if(dcell!=null)
    			{
    			redrawBridge(gc,gb,dcell,TwixtChip.red_bridge_120,brect);
        		redrawBridge(gc,gb,dcell,TwixtChip.black_bridge_120,brect);
    			}
    		}
       		{
    			TwixtCell dcell = cell.exitTo(TwixtBoard.CELL_DOWN_RIGHT);	// drawn to the left of us
    			if(dcell!=null)
    			{
    			redrawBridge(gc,gb,dcell,TwixtChip.red_bridge_60,brect);
        		redrawBridge(gc,gb,dcell,TwixtChip.black_bridge_60,brect);
        		TwixtCell d2cell = dcell.exitTo(TwixtBoard.CELL_DOWN);
        		if((d2cell!=null) 
        				&& (d2cell.containsChip(TwixtChip.red_bridge_60)
        						||d2cell.containsChip(TwixtChip.black_bridge_60)))
        		{
        			redrawPost(gc,gb,dcell,brect);
        			TwixtCell d4cell = dcell.exitTo(TwixtBoard.CELL_UP);
        			if(d4cell!=null)
        			{
        			if(redrawPost(gc,gb,d4cell,brect))
        				{
        				TwixtCell d5cell = d4cell.exitTo(TwixtBoard.CELL_RIGHT);      				
        				redrawBridge(gc,gb,d5cell,TwixtChip.red_bridge_30,brect);
        				redrawBridge(gc,gb,d5cell,TwixtChip.black_bridge_30,brect);
        				}
        			}
        			TwixtCell d3cell = dcell.exitTo(TwixtBoard.CELL_RIGHT);
        			redrawBridge(gc,gb,d3cell,TwixtChip.red_bridge_30,brect);
        			redrawBridge(gc,gb,d3cell,TwixtChip.black_bridge_30,brect);
        		}
    			}

    		}

 
    		break;
    	case 3:
    		// rotation 3, bridges are being drawn "left"
			{
			if(cell.containsChip(TwixtChip.red_bridge_120)||cell.containsChip(TwixtChip.black_bridge_120))
				{
				if(redrawPost(gc,gb,cell.exitTo(TwixtBoard.CELL_UP),brect))
				{	redrawBridge(gc,gb,cell,TwixtChip.red_bridge_60,brect);
					redrawBridge(gc,gb,cell,TwixtChip.black_bridge_60,brect);
				}}
			}
			{
				TwixtCell dcell = cell.exitTo(TwixtBoard.CELL_DOWN_RIGHT);
				redrawBridge(gc,gb,dcell,TwixtChip.red_bridge_30,brect);
				redrawBridge(gc,gb,dcell,TwixtChip.black_bridge_30,brect);
			}

    		break;
    	}}
        if(gb.illegalBridge(cell))
    	{
    	StockArt.SmallX.drawChip(gc, this, SQUARESIZE, xpos,ypos,null);
    	}
    }
    public void drawNextLine(Graphics gc,TwixtBoard gb,Rectangle br,TwixtCell start,int dx,int dy,int rep)
    {	TwixtCell from = start;
    	GC.setColor(gc, Color.gray);
    	int x = G.Left(br);
    	int y = G.Bottom(br);
    	for(int e=0;e<rep;e++)
    		{ TwixtCell to = gb.getCell((char)(from.col+dx),from.row+dy);
    		  int fx = gb.cellToX(from);
    		  int fy = gb.cellToY(from);
    		  int tx = gb.cellToX(to);
    		  int ty = gb.cellToY(to);
    		  //for(int i=-1;i<=1;i++) { for(int j=-1;j<=1;j++) { G.drawLine(gc, i+x+fx, j+y-fy, i+x+tx, j+y-ty); }}
    		  GC.drawFatLine(gc, x+fx, y-fy, x+tx, y-ty,Math.max(1, lineStrokeWidth));
    		  from = to;
    		  
    		}    	
    }
    public void drawGuidelines(Graphics gc,TwixtBoard gb,Rectangle br)
    {	int seeds[][] = {
    		{'B',23,1,-2},
    		{'B',23,2,-1},
    		{'W',23,-2,-1},
    		{'W',23,-1,-2},
    		
    		 {'B',2,1,2},
     		{'B',2,2,1},
     		{'W',2,-2,1},
     		{'W',2,-1,2},
    		};
    	for(int seed[] : seeds)
    	{
    	drawNextLine(gc,gb,br,gb.getCell((char)seed[0],seed[1]),seed[2],seed[3],7);
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
    @SuppressWarnings("unused")
	public void drawBoardElements(Graphics gc, TwixtBoard gb, Rectangle brect, HitPoint highlight)
    {	if(guidelinesOn) { drawGuidelines(gc,gb,brect); }
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  

        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
    	inBoardDraw = true;      
        // this is the active part of the "Start Evaluator" feature
        commonPlayer activePlayer = getActivePlayer(); //viewerWhoseTurn()
    	TwixtPlay robot = (TwixtPlay)(extraactions ? (activePlayer.robotPlayer) : null);

        numberMenu.clearSequenceNumbers();

        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
    	Enumeration<TwixtCell> allCells = gb.getIterator(Itype.LRTB);
    	if(showPreferred) { gb.buildPlausibleMoveSet(gb.whoseTurn); }
    	else if(showDistances) { gb.Static_Evaluate_Position2(gb.whoseTurn,false); }
    	// overall drawing is always left to right, top to bottom
        while(allCells.hasMoreElements())
        {
        	TwixtCell cell = allCells.nextElement();
        	// the four corner cells are removed, but still exist in the grid
        	if(cell.onBoard)
        		{ 
        		drawStack(gc,gb,cell,highlight,brect); 
        		} 

            if(false && 
            		cell!=null 
            		&& cell.topChip()==null 
            		&& robot!=null
            		)
            	{
            	int cy = gb.cellToY(cell);
            	int cx = gb.cellToX(cell);
            	int bot = G.Bottom(brect);
            	int left = G.Left(brect);
            	int ypos = bot - cy;
            	int xpos = left + cx;  
            	double ev =robot.getNeuroEval(cell);
            	//if((cell.row==11) && cell.col>='F') { G.print("c "+cell+" "+ev); }
            	//if(ev>10) {G.print(""+cell+" "+ev); }
            	StockArt.SmallO.drawChip(gc,this,(int)(gb.cellSize()*ev*2),xpos,ypos,null);
            	repaint(1000);
            	}
        }
        numberMenu.drawSequenceNumbers(gc,gb.cellSize(),labelFont,labelColor);
        inBoardDraw = false;
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
    {  TwixtBoard gb = disB(gc);
       TwixtState state = gb.getState();
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
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
         {	commonPlayer pl = getPlayerOrTemp(i);
         	pl.setRotatedContext(gc, selectPos, false);
         	
         	DrawChipPool(gc,chipRects[i],i, ourTurnSelect,gb);
         	if(planned && (i==gb.whoseTurn))
         	{
         		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
     					HighlightColor, rackBackGroundColor);
         	}
         	pl.setRotatedContext(gc, selectPos, true);
         }	
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);

       GC.setFont(gc,standardBoldFont());
       double messageRotation = pl.messageRotation();
       
       // draw the board control buttons 
		if((state==TwixtState.ConfirmSwap) 
			|| (state==TwixtState.PlayOrSwap))
		{ // make the "swap" button appear if we're in the correct state
			swapButton.show(gc,messageRotation,buttonSelect);
		}

		handleDrawUi(gc,messageRotation,state.getRole(),gb.drawIsLikely(),buttonSelect,
	      		  acceptDrawRect,declineDrawRect,HighlightColor,rackBackGroundColor);
	 

		if (state != TwixtState.Puzzle)
        {	
    	    // if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos,HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==TwixtState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        standardGameMessage(gc,messageRotation,
            		state==TwixtState.Gameover?gameOverMessage(gb):s.get(state.description()),
            				state!=TwixtState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null,0.5);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,
        		s.get(TwixtVictoryCondition),progressRect, goalRect);
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        drawViewsetMarker(gc,viewsetRect,nonDragSelect);
        drawAuxControls(gc,nonDragSelect);
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
      * for twixt, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
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
//     		TwixtCell dest = bb.animationStack.pop();
//     		TwixtCell src = bb.animationStack.pop();
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
        return (new Twixtmovespec(st, player));
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
        if (hp.hitCode instanceof TwixtId)// not dragging anything yet, so maybe start
        {
        TwixtId hitObject =  (TwixtId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
 	    case Black_Chip_Pool:
 	    case Red_Chip_Pool:
 	    	{
 	    	TwixtCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pick " + hitObject.shortName+" "+hitCell.row);
 	    	}
	    	break;
	    case BoardLocation:
	    	{
	        TwixtCell hitCell = hitCell(hp);
	    	if(!hitCell.isEmptyOrGhost()) { PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row); }
	    	}
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
       	if(!(id instanceof TwixtId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        TwixtId hitCode = (TwixtId)id;
        TwixtCell hitObject = hitCell(hp);
		TwixtState state = bb.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);

        case GuidelineButton:
        	guidelinesOn = !guidelinesOn;
        	break;
        case FlatViewButton:
        	flat = !flat;
        	showFlat = flat;
        	flatLock = true;
        	break;
	    case ReverseViewButton:
	    	rotation = (rotation+1)&3;
	    	bb.setRotation(rotation);
         	generalRefresh();
         	break;
	    case Red_Peg:
	    case Black_Peg:
		case Red_Bridge_30:
		case Red_Bridge_60:
		case Red_Bridge_120:
		case Red_Bridge_150:
		case Black_Bridge_30:
		case Black_Bridge_60:
		case Black_Bridge_120:
		case Black_Bridge_150:
			if(bb.pickedObject==null)
			{	if(hitObject.onBoard)
			{
				PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row+" "+hitCode.shortName);
			}
			else
			{
				PerformAndTransmit("Pick "+hitCode.shortName);
				}
			}
			else
			{
				PerformAndTransmit("Drop "+hitCode.shortName);
			}
			break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Confirm:
				if(bb.pickedObject==null) { PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row); }
				else { PerformAndTransmit("dropb "+hitObject.col+" "+hitObject.row); }
				break;
			case Gameover:
			case Play:
			case PlayOrSwap:
				if(bb.pickedObject==null)
				{	TwixtCell rack[]=bb.getRack(bb.whoseTurn);
					PerformAndTransmit("pick "+rack[0].rackLocation().shortName+" "+0);
				}
				PerformAndTransmit("dropb "+hitObject.col+" "+hitObject.row);
				break;
			case Puzzle:
				if(bb.pickedObject!=null) { PerformAndTransmit("dropb "+hitObject.col+" "+hitObject.row); }
				else { PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row); }
				break;
			}
			break;

        case Black_Chip_Pool:
        case Red_Chip_Pool:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+hitCode.shortName);
			}
           break;
 
        }
        }
    }



    private boolean setDisplayParameters(TwixtBoard gb,Rectangle r)
    {	boolean perspective = usePerspective();
    	boolean complete = false;
    	if(perspective)
    {
    	ycellscale = 0.6;

      	/*
      	double LL[] = {0.153,0.128};
       	double LR[] = {0.882,0.13};
       	double UL[] = {0.21,0.949};
       	double UR[] = {0.81,0.949};
       	gb.SetDisplayParameters( LL,LR,UL,UR);
       	*/
      	double xscale = 0.832;
    	double yscale = 0.936;
    	double xoff = 0.04;
    	double yoff = 0.25;
    	double rot = 0.5;
    	double xpers = 0.165;
    	double ypers = 0.15;
    	double skew = -0.005;
    	gb.SetDisplayParameters(xscale,yscale,xoff,yoff,rot,xpers,ypers,skew);
    	}
    	else
    	{
    	ycellscale = 0.6;

    	if((rotation&1)==0)
    	{
    		gb.SetDisplayParameters(
    				new double[] {0.07,0.075},
    				new double[] {0.976, 0.075},
    				new double[] {0.07,0.98},
    				new double[] {0.975, 0.98}
    		);
    	}
    	else {
    		gb.SetDisplayParameters(
    				new double[] {0.066,0.0725},
    				new double[] {0.975, 0.0725},
    				new double[] {0.066,0.976},
    				new double[] {0.975, 0.976});
    		
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
     * is game specific, but for twixt the underlying empty board is cached as a deep
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
    public String sgfGameType() { return(Twixt_SGF); }	// this is the official SGF number assigned to the game

    // the format is just what is produced by FormHistoryString
    //
    // this is completely standardized
    //public void performHistoryTokens(StringTokenizer his)
    //{	String command = "";
    //    // now the rest
    //    while (his.hasMoreTokens())
    //    {
    //        String token = his.nextToken();
    //        if (",".equals(token) || ".end.".equals(token))
    //        {
    //            if (!"".equals(command))
    //            {
    //                PerformAndTransmit(command, false,false);
    //                command = "";
    //            }
    //        }
    //       else
    //        {
    //            command += (" " + token);
    //        }
    //    }	
    //} 
    //public void performPlayerInitialization(StringTokenizer his)
    //{	int fp = G.IntToken(his);
    //	BoardProtocol b = getBoard();
    //    if (fp < 0)   {  fp = 0;  }
    //    b.setWhoseTurn(fp);
    //    players[fp].ordinal = 0;
    //    players[(fp == 0) ? 1 : 0].ordinal = 1;
    //	
    //}
   
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
        if(target==offerDrawAction)
     	{	if(OurMove() 
     			&& bb.canOfferDraw()
     			&& (bb.movingObjectIndex()<0)
     			&& ((bb.getState()==TwixtState.Play) || (bb.getState()==TwixtState.QueryDraw))
     			) 							
			{
			PerformAndTransmit(OFFERDRAW);
			}
    		else { G.infoBox(null,s.get(DrawNotAllowed)); }
     		return(true);
     	}
    	if(target==showDistancesItem)
    	{
    		showDistances = showDistancesItem.getState();
    		return(true);
    	}
    	else if(target==showPreferredItem)
    	{	showPreferred = showPreferredItem.getState();
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
    {  return(new TwixtPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/23/2023
     * 	642 files visited 0 problems
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

	public int getLastPlacement(boolean empty) {
		return bb.moveNumber;
	}
}

