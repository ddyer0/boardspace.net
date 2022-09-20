package majorities;

import bridge.*;

import com.codename1.ui.geom.Rectangle;
import online.common.*;
import java.util.StringTokenizer;
import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.Random;
import lib.StockArt;
import lib.LFrameProtocol;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

import static majorities.MajoritiesMovespec.*;


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
 * <br>MajoritiesViewer - this class, a canvas for display and mouse handling
 * <br>MajoritiesBoard - board representation and implementation of the game logic
 * <br>MajoritiesMovespec - representation, parsing and printing of move specifiers
 * <br>MajoritiesPlay - a robot to play the game
 * <br>MajoritiesConstants - static constants shared by all of the above.  
 *  <p>
 *  The primary purpose of the MajoritiesViewer class is to do the actual
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
 *  paintCanvas is called in the event loop.
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
 *  <li> use eclipse refactor to rename the package for "majorities" and for individual files
 *  <li> duplicate the majorities start configuration, making a new one for the new game
 *  <li> launch the new game and get it to start, still identical to the old majorities in all but name.
 *  	this will probably require a few edits to the init code.
 *  <li> do a cvs update on the original majorities hierarchy to get back the original code.
 *  
*/
public class MajoritiesViewer extends CCanvas<MajoritiesCell,MajoritiesBoard> implements MajoritiesConstants, GameLayoutClient
{	
    static final String Majorities_SGF = "Majorities"; 				// sgf game type allocated for majorities

    // file names for jpeg images and masks
    static final String ImageDir = "/majorities/images/";

     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(230,240,255);
    private Color rackBackGroundColor = new Color(185,192,205);
    private Color boardBackgroundColor = new Color(220,165,155);
    

    // private state
    private MajoritiesBoard bb = null; //the board from which we are displaying
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

    private Rectangle chipRects[] = addRect("chip",2);

    private Rectangle tinyBoard0 = addRect("tinyBoard0");
    private Rectangle tinyBoard1 = addRect("tinyBoard1");
    private Rectangle tinyBoard2 = addRect("tinyBoard2");
    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	MajoritiesChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	gameIcon = MajoritiesChip.Icon.image;
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
        	MajoritiesConstants.putStrings();
        }
        
        String type = info.getString(OnlineConstants.GAMETYPE, MajoritiesVariation.majorities_3.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new MajoritiesBoard(type,players_in_game,randomKey,getStartingColorMap(),MajoritiesBoard.REVISION);
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
    	int chipW = unitsize*5;
    	int chipH = unitsize*5;
    	int doneW = unitsize*5;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	Rectangle done = doneRects[player];
    	
    	G.SetRect(chip, x, y, chipW, chipH);
    	G.SetRect(done, x+chipW,G.Bottom(box),doneW,plannedSeating()?doneW/2:0);
    	
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
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        int ncols = 24;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// 1:1 aspect ratio for the board
    			fh*1.5,	// minximum cell size
    			fh*2,
    			0.7		// preference for the designated layout, if any
    			);
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
    	
 
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
    	
       	Rectangle full = layout.peekMainRectangle();
       	int fuw = G.Width(full);
       	int fuh = G.Height(full);
       	if(fuw>fuh)
       	{	int boxW = fuh/5;
       		layout.placeRectangle(tinyBoard0,boxW,boxW*3+margin*2,BoxAlignment.Center);   
       		G.SetHeight(tinyBoard0,boxW);
       		G.AlignLeft(tinyBoard1, G.Bottom(tinyBoard0)+margin,tinyBoard0);
       		G.AlignLeft(tinyBoard2, G.Bottom(tinyBoard1)+margin,tinyBoard1);
    }
       	else
    {   
       		int boxH = fuw/5;
       		layout.placeRectangle(tinyBoard0,boxH*3+margin*2,boxH,BoxAlignment.Center);   
       		G.SetWidth(tinyBoard0,boxH);
       		G.AlignTop(tinyBoard1, G.Right(tinyBoard0)+margin,tinyBoard0);
       		G.AlignTop(tinyBoard2, G.Right(tinyBoard1)+margin,tinyBoard1);
       	}
 
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/ncols);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(ncols*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateH = CELLSIZE;
        int stateY = boardY;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,chatBackgroundColor);
   
    }


	// draw a box of spare chips. For majorities it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,MajoritiesBoard gb)
    {	int unit = gb.cellSize();
        boolean canhit = gb.LegalToHitChips(player) && G.pointInRect(highlight, r);
        if (canhit)
        {
            highlight.hitCode = gb.getPlayerColor(player);
            highlight.arrow = (gb.pickedObject!=null)?StockArt.DownArrow:StockArt.UpArrow;
            highlight.awidth = unit;
            highlight.hit_x = G.centerX(r);
            highlight.hit_y = G.centerY(r);
            highlight.spriteColor = Color.red;
        }

        if (gc != null)
        { // draw a random pile of chips.  It's just for effect

            int spacex = G.Width(r) - unit;
            int spacey = G.Height(r) - unit;
            Random rand = new Random(4321 + player); // consistant randoms, different for black and white 
            MajoritiesCell c = gb.getPlayerCell(player);
             MajoritiesChip chip = gb.getPlayerChip(player);
            int nc = 8;							 // draw 10 chips
            while (nc-- > 0)
            {	int rx = Random.nextInt(rand, 100)*spacex/100;	// slightly obscure algorithm keeps the positions the same regargless of scale
                int ry = Random.nextInt(rand, 100)*spacey/100;
                c.drawChip(gc,this,chip.getAltDisplayChip(rx+ry),unit,G.Left(r)+unit/2+rx,G.Top(r)+unit/2+ry,null);
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
    	MajoritiesChip.getChip(obj).drawChip(g,this,bb.cellSize(), xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  

	private void drawBoardCells(Graphics gc, MajoritiesBoard gb,Rectangle brect)
    {
        // draw the tile grid.  The positions are determined by the underlying board
        // object, and the tile itself if carefully crafted to tile the majorities board
        // when drawn this way.  For games with simple graphics, we could use the
        // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
        // but for more complex graphics with overlapping shadows or stacked
        // objects, this double loop is useful if you need to control the
        // order the objects are drawn in.
         for(cell<MajoritiesCell> cc : gb.getCellArray())
             { //where we draw tgb.he grid
        	 	MajoritiesCell c = (MajoritiesCell)cc;
                int ypos = G.Bottom(brect)- gb.cellToY(c);
                int xpos = G.Left(brect) + gb.cellToX(c);
                MajoritiesChip tile = c.removed?MajoritiesChip.hexTileDark:MajoritiesChip.hexTile;
                int xsize = gb.cellSize();
                tile.getAltDisplayChip(c).drawChip(gc,this,xsize,xpos,ypos,null);
                /*
                 * test the 3 coloring algorithm
       			Color colors[] = new Color[] { Color.red,Color.green,Color.blue};
    			int color = bb.triColor(c.col,c.row);
    			GC.setColor(gc, colors[color]);
    			GC.fillOval(gc, xpos, ypos, 10, 10);
    			*/
                //equivalent lower level draw image
                // drawImage(gc,tileImages[hidx].image,tileImages[hidx].getScale(), xpos,ypos,gb.CELLSIZE,1.0);
                //
            }
    }

    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { // erase
    	boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //G.fillRect(gc, fullRect);
     MajoritiesChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       MajoritiesChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);
      setDisplayParameters(bb,boardRect);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      bb.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

      drawBoardCells(gc,bb,boardRect);
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

   /** draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */
    private void drawBoardElements(Graphics gc, MajoritiesBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  Either gc or highlight might be
    	// null, but not both.
        //

        // using closestCell is preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        MajoritiesCell closestCell = gb.closestCell(highlight,brect);
        boolean hitCell = gb.LegalToHitBoard(closestCell);
        if(hitCell)
        { // note what we hit, row, col, and cell
          boolean empty = (closestCell.chip == null);
          boolean picked = (gb.pickedObject!=null);
          highlight.hitCode = (empty||picked) ? MajoritiesId.EmptyBoard : MajoritiesId.BoardLocation;
          highlight.hitObject = closestCell;
          highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
          highlight.awidth = CELLSIZE;
        }
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        if (gc != null)
        {
        for(MajoritiesCell cell = gb.allCells; cell!=null; cell=cell.next)
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
    {  MajoritiesBoard gb = disB(gc);
       MajoritiesState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
       
    	if(gc!=null)
 		{
 		drawTinyBoard(gc,gb,0,tinyBoard0); 
 		drawTinyBoard(gc,gb,1,tinyBoard1); 
 		drawTinyBoard(gc,gb,2,tinyBoard2);
 		}

       setDisplayParameters(gb,boardRect);
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
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, Color.black,boardBackgroundColor,null,standardBoldFont());
       drawBoardElements(gc, gb, boardRect, ourTurnSelect);
       
       boolean planned = plannedSeating();
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
         {	commonPlayer pl = getPlayerOrTemp(i);
         	pl.setRotatedContext(gc, selectPos, false);
         	DrawChipPool(gc, chipRects[i], i, ourTurnSelect,gb);
          	if(planned && (i==gb.whoseTurn))
         	{
         		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? ourTurnSelect : null), 
     					HighlightColor, rackBackGroundColor);
         	}
         	pl.setRotatedContext(gc, selectPos, true);
         }	
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       
       GC.setFont(gc,standardBoldFont());


		if (state != MajoritiesState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos, HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==MajoritiesState.Puzzle),nonDragSelect,HighlightColor,rackBackGroundColor);
  
 
       	// draw the avatars
            standardGameMessage(gc,messageRotation,
            		state==MajoritiesState.Gameover?gameOverMessage():s.get(state.description()),
            				state!=MajoritiesState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
            gb.getPlayerChip(gb.whoseTurn).drawChip(gc, this, iconRect,null);
            goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
            //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for majorities
        
        // draw the vcr controls
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
        return (new MajoritiesMovespec(st, player));
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
        if (hp.hitCode instanceof MajoritiesId)// not dragging anything yet, so maybe start
        {
        MajoritiesId hitObject =  (MajoritiesId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    
 	    case Black_Chip_Pool:
 	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick " + hitObject.shortName);
	    	break;
	    case BoardLocation:
	        MajoritiesCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    	break;
        }
        }
    }
	private void doDropChip(char col,int row)
	{	MajoritiesState state = bb.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case Puzzle:
			{
			MajoritiesChip mo = bb.pickedObject;
			if(mo==null) { mo=bb.lastPicked; }
			if(mo==null) { mo=bb.getPlayerChip(bb.whoseTurn); }

			PerformAndTransmit("dropb "+mo.id.shortName+" "+col+" "+row);
			}
			break;
		case Confirm:
		case Play1:
		case Play2:
		case Play:
			MajoritiesChip mo=bb.getPlayerChip(bb.whoseTurn);	
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
       	if(!(id instanceof MajoritiesId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        MajoritiesId hitCode = (MajoritiesId)id;
        MajoritiesCell hitObject = hitCell(hp);
		MajoritiesState state = bb.getState();
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


    private void setDisplayParameters(MajoritiesBoard gb,Rectangle r)
    {
    	gb.SetDisplayParameters(0.95, 1.0, 0,0,60); // shrink a little and rotate 60 degrees
      	gb.SetDisplayRectangle(r);
    }
    //
    // draw a board in a tiny rectangle, with stones of the winning color occupying the
    // entire row.  There are three of these little boards, one for each of the major
    // directions. They provide a visual cue for progress toward dominating each direction.
    //
    private void drawTinyBoard(Graphics gc,MajoritiesBoard gb,int direction,Rectangle r)
    {	GC.frameRect(gc,Color.black,r);
    	setDisplayParameters(gb,r);
    	drawBoardCells(gc,gb,r); 
    	int map[] = bb.getColorMap();
    	int white = 0;
    	int black = 0;
     	for(MajoritiesCell c : gb.lineRoots[direction])
    	{
    		int winner = c.lineOwner[direction];
    		if(winner>=0)
    		{	MajoritiesChip ch = gb.getPlayerChip(map[winner]);
    			if(winner==0) { white++; } else { black++; }
    			while(c!=null)
    			{
    			//if(!c.removed)
    				{
        			int xp0 = G.Left(r)+gb.cellToX(c);
        			int yp0 = G.Bottom(r)-gb.cellToY(c);
        			ch.drawChip(gc,this,gb.cellSize(),xp0,yp0,null);
    				}
    				c = c.exitTo(MajoritiesBoard.lineDirections[direction]);
    			}
      		}
    	}
       	int quarter = G.Height(r)/8;
    	MajoritiesChip.White.drawChip(gc,this,quarter,G.Left(r)+quarter,G.Top(r)+G.Height(r)-quarter,"  "+white);
       	MajoritiesChip.Black.drawChip(gc,this,quarter,G.Right(r)-quarter,G.Top(r)+G.Height(r)-quarter,"  "+black);
    }
  
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
    public String sgfGameType() { return(Majorities_SGF); }	// this is the official SGF number assigned to the game

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
    {  
    	return new MajoritiesPlay();
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

