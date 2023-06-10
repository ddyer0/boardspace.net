package pushfight;

import static pushfight.Pushfightmovespec.*;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;

import online.common.*;
import online.common.SeatingChart.DefinedSeating;

import java.util.*;
import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.Image;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.StockArt;
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
 * <br>PushfightViewer - this class, a canvas for display and mouse handling
 * <br>PushfightBoard - board representation and implementation of the game logic
 * <br>Pushfightmovespec - representation, parsing and printing of move specifiers
 * <br>PushfightPlay - a robot to play the game
 * <br>PushfightConstants - static constants shared by all of the above.  
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
 *  
*/
public class PushfightViewer extends CCanvas<PushfightCell,PushfightBoard> implements PushfightConstants, GameLayoutClient
{	static final long serialVersionUID = 1000;
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(214,201,189);
    private Color rackBackGroundColor = new Color(214,201,189);
    private Color boardBackgroundColor = new Color(214,201,189);
    private Color BlackArrowColor = new Color(230,200,255);


     
    // private state
    private PushfightBoard bb = null; //the board from which we are displaying
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
    private Rectangle chipRect[] = addZoneRect("chip",2);
    private Rectangle repRect = addRect("repRect");	
    private Rectangle reverseRect = addRect("reverseRect");
    private JCheckBoxMenuItem reverseOption = null;

/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	PushfightChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = PushfightChip.Icon.image;
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
       	int players_in_game = Math.max(chipRect.length,info.getInt(OnlineConstants.PLAYERS_IN_GAME,chipRect.length));
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	InternationalStrings.put(PushfightStrings);
        	InternationalStrings.put(PushfightStringPairs);
        }
         

        String type = info.getString(GAMETYPE, PushFightVariation.pushfight.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new PushfightBoard(type,players_in_game,randomKey,getStartingColorMap(),PushfightBoard.REVISION);
        useDirectDrawing(true); 
        doInit(false);
        bb.setReverseY(preferredRotation());
        reverseOption = myFrame.addOption(s.get(ReverseView),bb.reverseY(),deferredEvents);
        adjustPlayers(players_in_game);
    }
    public boolean preferredRotation()
    {
        boolean rev = false;
        DefinedSeating defined = seatingChart();
        switch(defined)
        {
        default: 
        	rev = false;
        	break;
        case FaceToFacePortrait: 
        case FaceToFaceLandscapeSide:
        case SideBySide:
        case RightCorner:
        	rev = true;
        }
        return(rev);
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


    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle box = createPlayerGroup(pl,x,y,chipRect[player],unitsize);
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*3 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.union(box, done);
    	pl.displayRotation = rotation;
    	return(box);
    }
    private Rectangle createPlayerGroup(commonPlayer pl,int x,int y,Rectangle chipRect,int unitsize)
    {
		// a pool of chips for the first player at the top
        G.SetRect(chipRect,	x,	y,	2*unitsize,	3*unitsize);
        Rectangle box = pl.createRectangularPictureGroup(x+2*unitsize,y,2*unitsize/3);
        G.union(box, chipRect);
        return(box);
    }

    public void setLocalBounds(int x,int y,int w,int h)
    {
    	setLocalBoundsV(x,y,w,h,aspect);
    }
    double aspect[] = {1.0, 1.5, 2.0};
    
    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*14;	
    	int vcrw = fh*16;
       	int minChatW = fh*35;	
        int minLogH = fh*15;	
        int margin = fh/2;
        int buttonW = fh*8;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			aspect,	// aspect ratio for the board
    			fh*3,	// min cell size
    			fh*4,	// maximum cell size
    			0.2		// preference for the designated layout, if any
    			);
    	// place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*4/3);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);


    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	boolean rotate = mainW<mainH;	// rotate if it will fit better
        int nrows = rotate ? 24 : 15;  // b.boardRows
        int ncols = rotate ? 15 : 24;	 // b.boardColumns
   	
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
        int stateH = fh*3;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,reverseRect,viewsetRect,noChatRect);
        G.SetRect(iconRect,stateX,stateY,stateH,stateH);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	if(rotate)
    	{	double rd = -Math.PI/2;
    		DefinedSeating seat = seatingChart();
    		switch(seat)
    			{
    			default: break;
    			case LeftCorner: 
    			case FaceToFacePortrait:
    				rd = Math.PI/2;
    				break;
    			}
    		G.setRotation(boardRect,rd);
    		contextRotation = rd;
    	}
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        Rectangle pl = getPlayerOrTemp(0).playerBox;
        int boxSize = G.Width(pl)*G.Height(pl)/10;
        return(boardW*boardH+boxSize/10);
    }
	// draw a box of spare chips. For pushfight it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, commonPlayer pl, HitPoint highlight,PushfightBoard gb)
    {   if (gc != null)
        { 	// draw a random pile of chips.  It's just for effect
        	PushfightChip chip = gb.getPlayerChip(pl.boardIndex);
        	chip.drawChip(gc, this,r,null,0.7);
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
    	PushfightChip.getChip(obj).drawChip(g,this,bb.cellSize(), xp, yp, null);
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
     PushfightChip.backgroundTile.image.tileImage(gc, fullRect);
      drawFixedBoard(gc);
    }
    Image scaled = null;
    PushfightChip background = null;
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {
    	boolean reviewBackground = reviewMode()&&!mutable_game_record;	
    	if(reviewBackground)
    	{	 
       PushfightChip.backgroundReviewTile.image.tileImage(gc,brect);   
    	}
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(bb,brect);
      
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      PushfightChip board = usePerspective() ? PushfightChip.board : PushfightChip.boardO;
      if(board!=background) { scaled = null; }
      background = board;
      scaled = board.getImage().centerScaledImage(gc, brect,scaled);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      bb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

 
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
    public void drawBoardElements(Graphics gc, PushfightBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
    	boolean perspective = usePerspective();
     	Enumeration<PushfightCell>cells = gb.getIterator(Itype.LRTB);
     	Hashtable<PushfightCell,Pushfightmovespec>targets = gb.getTargets();
        numberMenu.clearSequenceNumbers();
	
     	while(cells.hasMoreElements())
           {PushfightCell cell = cells.nextElement();
          	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            double dx = perspective ? 0 : 0;
            double dy = perspective ? 0.5 : 0;
            boolean canHit = gb.legalToHitBoard(cell,targets);
            numberMenu.saveSequenceNumber(cell,xpos,ypos,cell.lastEmptiedPlayer==0 ? labelColor : BlackArrowColor);
         
            if(cell.drawStack(gc,this,canHit ? highlight : null,gb.cellSize(),xpos,ypos,0,dx,dy,null))
            {
            	highlight.spriteColor = Color.red;
            	highlight.awidth = CELLSIZE;
            }
           if(targets.get(cell)!=null)
        	   {StockArt.SmallO.drawChip(gc,this,CELLSIZE, xpos, ypos , null);
        	   }
            }
        numberMenu.drawSequenceNumbers(gc,CELLSIZE,labelFont,labelColor);

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
    {  PushfightBoard gb = disB(gc);
       PushfightState state = gb.getState();
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
       
       boolean planned = plannedSeating();
       
       for(int player=FIRST_PLAYER_INDEX;player<=SECOND_PLAYER_INDEX;player++)
       	{ commonPlayer pl = getPlayerOrTemp(player);
       	  pl.setRotatedContext(gc, selectPos,false);
    	   DrawChipPool(gc, chipRect[player],pl, ourTurnSelect,gb);
    	   if(planned && gb.whoseTurn==player)
    	   {
    		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
       	   pl.setRotatedContext(gc, selectPos,true);
       	}
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
    
       GC.setFont(gc,standardBoldFont());
       
       if (state != PushfightState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? buttonSelect : null),HighlightColor, rackBackGroundColor);		
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==PushfightState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        standardGameMessage(gc,messageRotation,
            				state==PushfightState.Gameover?gameOverMessage():s.get(state.description()),
            				state!=PushfightState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        gb.getPlayerChip(gb.whoseTurn).drawChip(gc,this,iconRect,null,0.9);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(PushVictoryCondition),progressRect, goalRect);
        DrawRepRect(gc,pl.displayRotation,Color.black,gb.Digest(),repRect);
        
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        drawViewsetMarker(gc,viewsetRect,selectPos); 
        DrawReverseMarker(gc,reverseRect,selectPos,PushfightId.ReverseViewButton);
        
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
     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for pushfight, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
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
	 case MOVE_DROPB:
	 case MOVE_PICKB:
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
        return (new Pushfightmovespec(st, pl));
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
    //  public commonMove EditHistory(commonMove nmove)
    //  {	  // some damaged games ended up with naked "drop", this lets them pass 
    	  //G.print("D "+bb.Digest());
    //	  commonMove rval = super.EditHistory(nmove);
     	     
    //	  return(rval);
    //  }

    
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
      //{	//DISABLE_VERIFY = true;
      //	super.verifyGameRecord();
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
        if (hp.hitCode instanceof PushfightId)// not dragging anything yet, so maybe start
        {
        PushfightId hitObject =  (PushfightId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    
	    case BoardLocation:
	        PushfightCell hitCell = hitCell(hp);
	        if(bb.getState()==PushfightState.Confirm)
	        {
	        	hitCell = bb.getSource();
	        	PerformAndTransmit(UNDO_ALLOW);	// use undo to do the complicated un-push
	        }
	        PerformAndTransmit("Pickb "+(char)(hitCell.col-1)+" "+hitCell.row);
	    	break;
        } 
        }
    }
	private void doDropChip(char col,int row)
	{	PushfightState state = bb.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state "+state);
		case Puzzle:
		{
		PushfightChip mo = bb.pickedObject;
		if(mo==null) { mo=bb.lastPicked; }
		if(mo==null) { mo=bb.getPlayerChip(bb.whoseTurn); }
		PerformAndTransmit("dropb "+mo.id.shortName+" "+(char)(col-1)+" "+row);
		}
		break;
		case Confirm:
			PushfightChip mo=bb.getPlayerChip(bb.whoseTurn);	
			PerformAndTransmit("dropb "+mo.id.shortName	+ " "+(char)(col-1)+" "+row);
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
       	if(!(id instanceof PushfightId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        PushfightId hitCode = (PushfightId)id;
        PushfightCell hitObject = bb.getCell(hitCell(hp));
		PushfightState state = bb.getState();
        switch (hitCode)
        {
        default:
        	if (performStandardButtons(hitCode, hp)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " + hitCode);
            }
        	break;

        case ReverseViewButton:
        {boolean v = !bb.reverseY();
    	bb.setReverseY(v);
      	 reverseOption.setState(v);
      	 generalRefresh();
    	}
         	 break;

        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state "+state);
			case Confirm:
				if(!bb.isDest(hitObject))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					throw G.Error("shouldn't hit a chip in state "+state);
					}
				// fall through and pick up the previously dropped piece
				//$FALL-THROUGH$
			case Puzzle:
			case PushMove2:
			case PushMove1:
			case Push:
			case InitialPosition:
				PerformAndTransmit((bb.pickedObject==null ? "Pickb " : "Dropb ")+(char)(hitObject.col-1)+" "+hitObject.row);
				break;
			}
			break;
			
        case EmptyBoard:
			doDropChip(hitObject.col,hitObject.row);
			break;
			
 
        }
        }
    }

    private boolean usePerspective() { return(getAltChipset()==0); }


    private boolean setDisplayParameters(PushfightBoard gb,Rectangle r)
    {
    	// 0.95 and 1.0 are more or less magic numbers to match the board to the artwork
      	gb.SetDisplayRectangle(r);

      	if(usePerspective())
    	{
    	double UL[] = new double[]{0.16,0.98};
    	double UR[] = new double[]{1.08,.80};
    	double LL[] = new double[]{0.026,0.43};
    	double LR[] = new double[]{1.0,.19};
    	gb.SetDisplayParameters(LL,  LR, UL, UR);      	
    	}
    	else
    	{
    	gb.SetDisplayParameters( 0.99,1.06,0.1,0,0);
    	}
       	return(true);
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
    public String sgfGameType() { return(Pushfight_SGF); }	// this is the official SGF number assigned to the game

   
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
        if(target==reverseOption)
    	{
    	bb.setReverseY(reverseOption.getState());
    	generalRefresh();
    	return(true);
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


     /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new PushfightPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 797 files visited 0 problems
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
    public int getLastPlacement(boolean em)
    {
    	return bb.lastPlacedIndex;
    }
}

