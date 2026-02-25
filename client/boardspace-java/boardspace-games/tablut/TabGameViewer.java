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
package tablut;

import java.awt.*;


import java.util.Hashtable;

import common.GameInfo;
import lib.Graphics;
import lib.Image;
import lib.BSDate;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.FontManager;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.TextButton;
import lib.Tokenizer;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

/**
 * TODO: revamp options screens, use viticulture as a template
 * Change History

 * The main classes are:
 *  TabGameViewer - this class, a canvas for display and mouse handling
 *  TabGameBoard - board representation and implementation of the game logic
 *  Tabmovespec - representation, parsing and printing of move specifiers
 *  TabPlay - a robot to play the game
 *  TabConstants - static constants shared by all of the above.  
 *  
 *  The primary purpose of the TabGameViewer class is to do the actual
 *  drawing and to mediate the mouse gestures.   However, all the actual
 *  work is done in an event loop, rather than in direct reposonse to mouse or
 *  window events, so there is only one process involved.  With a single 
 *  process, there are no worries about synchronization among processes
 *  of lack of synchronization - both major causes of flakey user interfaces.
 *  
 *  The actual mouse handling is done by the commonCanvas class, which simply 
 *  records the recent mouse activity, and triggers "MouseMotion" to be called
 *  while the main loop is executing.
 *  
 *  Similarly, the actual "update" and "paint" methods for the canvas are handled
 *  by commonCanvas, which merely notes that a paint is needed and returns immediately.
 *  paintCanvas is called in the event loop.
 *  
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
 *  Steps to clone this hierarchy to start the next game
 *  1) copy the hierarchy to a brother directory
 *  2) open eclipse, then select the root and "refresh".  This should result
 *     in just a few complaints about package mismatches for the clones.
 *  3) fix the package names in the clones
 *  4) rename each of the classes in the clones, using refactor/rename
*/
public class TabGameViewer extends CCanvas<TabCell,TabGameBoard> implements TabConstants
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color rackBackGroundColor = new Color(225,225,255);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images, shared among all instances of the class so loaded only once
    private static Image[] textures = null;// background textures
    
    // private state
    private TabGameBoard b = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private int BOARDCELLSIZE;
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle shipRects[] = addRect("ship",2);
    
    private Rectangle goldFlagRect = addRect("goldFlag");
	private TextButton swapButton = addButton(SWAP,GameId.HitSwapButton,SwapDescription,
			HighlightColor, rackBackGroundColor);

    public Rectangle flagshipWinRect = addRect(".flagWinRect");		// flagship wins only in the corner
    public Rectangle flagOwnRect = addRect(".flagOwnRect");			// only king can occupy the center
    public Rectangle flagCaptureRect = addRect(".flagCaptureRect");	// flagship can capture
    public Rectangle flagFoursideRect = addRect(".flagFoursideRect");// flagship captured on 4 sides only


    public synchronized void preloadImages()
    {	TabChip.preloadImages(loader,ImageDir);
    	if (textures == null)
    	{ 	// note that for this to work correctly, the images and masks must be the same size.  
        	// Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    		
    		// images and textures are static variables, so they're shared by
    		// the entire class and only get loaded once.  Special synchronization
    		// tricks are used to make sure.
          textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = textures[ICON_INDEX];
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
       
        b = new TabGameBoard(info.getString(GameInfo.GAMETYPE, Default_Tablut_Game),getStartingColorMap());
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
        b.doInit(b.gameType());						// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
    }

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = shipRects[player];
    	int chipW = unitsize*4;
    	int chipH = unitsize*2;
    	int doneW = plannedSeating()?unitsize*4:0;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	Rectangle done = doneRects[player];
    	
    	G.SetRect(chip, x, y, chipW, chipH);
    	G.SetRect(done, x,y+chipH+unitsize/4,doneW,doneW/2);
    	
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
       	int ruleW = (int)(optionWidth(FontManager.getFontMetrics(standardPlainFont()))*1.05);
       	int ruleH = fh*4;
       	
    	int minLogW = fh*15;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        int nrows = b.nrows;  
        int ncols = b.ncols;
        
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// 1:1 aspect ratio for the board
    			fh*2.5,	// minimum cell size
    			fh*3.0,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,swapButton);
      	
       	layout.placeRectangle(flagCaptureRect, ruleW,ruleH*4,BoxAlignment.Edge);
       	int ry = G.Top(flagCaptureRect);
       	G.SetHeight(flagCaptureRect, ruleH);
        G.AlignLeft(flagFoursideRect,ry+ruleH,flagCaptureRect);
        G.AlignLeft(flagOwnRect,ry+ruleH*2,flagCaptureRect);
        G.AlignLeft(flagshipWinRect,ry+ruleH*3,flagCaptureRect);
 
 
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/(nrows+0.5));
    	BOARDCELLSIZE = CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
        int stateH = fh*5/2;
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH-stateH*3/2)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH+stateH;
    	int boardBottom = boardY+boardH;
    	int boardRight = boardX+boardW;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
         int stateY = boardY-stateH;
        int stateX = boardX;
        placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
  
        
        G.SetRect(goldFlagRect,boardRight-CELLSIZE,boardBottom-CELLSIZE,CELLSIZE,CELLSIZE);
      

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH/2,boardW,stateH,goalRect);      
 
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
    }

 

   public void drawSprite(Graphics g,int obj,int xp,int yp)
   {	TabChip.getChip(obj).draw(g,this,BOARDCELLSIZE,xp,yp,null);
  
   }
   // also related to sprites,
   // default position to display static sprites, typically the "moving object" in replay mode
   public Point spriteDisplayPoint()
	{   return(new Point(G.Right(boardRect)-(BOARDCELLSIZE/4),G.Bottom(boardRect)-(BOARDCELLSIZE/4)));
	}


    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {boolean review = reviewMode() && !mutable_game_record;
      // erase
      TabGameBoard gb = disB(gc);
      gb.SetDisplayRectangle(boardRect); 	   // this is necessary to inform disb of the board geometry

      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method
      gb.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

      // draw the tile grid.  The positions are determined by the underlying board
      // object, and the tile itself if carefully crafted to tile the board
      // when drawn this way.  For games with simple graphics, we could use the
      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
      // but for more complex graphics with overlapping shadows or stacked
      // objects, this double loop is useful if you need to control the
      // order the objects are drawn in.
      int lastincol = gb.ncols;
       for (int col = lastincol-1; col >= 0; col--)
       {
           char thiscol = (char) ('A' + col);
           for (int thisrow = lastincol-1; thisrow >= 0; thisrow--) // start at row 1 (0 is the grid) 
           { //where we draw the grid
        	  TabCell cell = gb.getCell(thiscol,thisrow+1);
              int ypos = G.Bottom(boardRect) - gb.cellToY(thiscol, thisrow+1);
              int xpos = G.Left(boardRect) + gb.cellToX(thiscol, thisrow);
              TabChip tile = TabChip.HexTile;
              if(cell.flagArea) { tile = TabChip.HexTile_Gold2; }
              else if(cell.centerArea) { tile = TabChip.HexTile_Gold; }
              tile.draw(gc,this,(int)(BOARDCELLSIZE*0.74),xpos,ypos,null);
           }
       }
 
    }
    boolean sloppyDigest = false;
    public commonMove EditHistory(commonMove m)
    {	return(EditHistory(m,sloppyDigest||(m.op==MOVE_SETOPTION)));
    }
    
   /* draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */
    private void drawBoardElements(Graphics gc, TabGameBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  Either gc or highlight might be
    	// null, but not both.
        //
     	numberMenu.clearSequenceNumbers();

        // using closestCell is preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        TabCell closestCell = gb.closestCell(highlight,brect);
        boolean hitCell = gb.LegalToHitBoard(closestCell);
        if(hitCell)
        { // note what we hit, row, col, and cell
          highlight.hitCode = (closestCell.chip == null) ? TabId.EmptyBoardLocation : TabId.BoardLocation;
          highlight.hitObject = closestCell;
          highlight.arrow = (closestCell.chip == null)?StockArt.DownArrow:StockArt.UpArrow;
          highlight.awidth = CELLSIZE;
        }
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        {
        Hashtable<TabCell,TabCell> captures = gb.droppedObjectCaptures();
        for(TabCell cell = gb.allCells; cell!=null; cell=cell.next)
          {
            boolean drawhighlight = (hitCell && (cell==closestCell)) 
   				|| gb.isDest(cell) 		// is legal for a "drop" operation
   				|| gb.isSource(cell);	// is legal for a "pick" operation+
         	 int ypos = G.Bottom(brect) - gb.cellToY(cell);
             int xpos = G.Left(brect) + gb.cellToX(cell);
  
             if (drawhighlight)
             { // checking for pointable position
            	TabChip.Selection.draw(gc,this,BOARDCELLSIZE,xpos,ypos,null);
             }
             numberMenu.saveSequenceNumber(cell,xpos,ypos);
             cell.draw(gc, this, BOARDCELLSIZE, xpos,ypos,null);
             if(captures.get(cell)!=null)
              {
              StockArt.SmallX.draw(gc,this,BOARDCELLSIZE*2,xpos,ypos,null);
              }
 
            }
        }
        numberMenu.includeNumbers = false;
        numberMenu.drawSequenceNumbers(gc,CELLSIZE*2/3,labelFont,labelColor);
    }
	// draw a box of spare chips. It's purely for visual effect.
    private void drawShipRect(Graphics gc, commonPlayer pl,TabGameBoard gb,Rectangle r, TabCell pool, HitPoint highlight)
    {	
        boolean canhit = b.LegalToHitChips(pool) && G.pointInRect(highlight, r);
        if (canhit)
        {
            highlight.hitCode = pool.rackLocation;
        }
      	int cx = G.centerX(r);
      	int h = G.Height(r);
    	int cy = G.centerY(r)+h/4;

        if (gc != null)
        { 
          	int w = G.Width(r)/2;
           if(canhit)
           {
            TabChip.Selection.draw(gc,this,w,cx,cy,null);
           }
           pool.draw(gc,this,w,cx,cy,null);
        }
        pl.rotateCurrentCenter(pool,cx,cy);
    }
    private int optionWidth(FontMetrics fm)
    {
    	int sz = 0;
    	for(TabId op : TabId.values())
    	{
    		if(op.trueName!=null) { sz = Math.max(sz, fm.stringWidth(s.get(op.trueName))); }
    		if(op.falseName!=null) { sz = Math.max(sz, fm.stringWidth(s.get(op.falseName))); }
    	}
    	return(sz);
    }
    private boolean drawOptionRect(Graphics gc,HitPoint hp,TabGameBoard gb,Rectangle r,TabId op)
    {   boolean hit = G.pointInRect(hp,r);
		if(hit) { hp.hitCode = op; }
    	if(gc!=null)
    	{
    	int half = G.Height(r)/2;
    	int x = G.Left(r);
    	int y = G.Top(r);
    	int w = G.Width(r);
    	boolean val = gb.getOptionValue(op);
    	Color selectColor = hit?Color.red:Color.gray;
    	String trueVal = "  "+s.get(op.trueName);
    	String falseVal = "  "+s.get(op.falseName);
    	GC.Text(gc,false,x,y,w,half,val?Color.black:selectColor,val?Color.white:null,trueVal);
       	GC.Text(gc,false,x,y+half,w,G.Height(r)/2,val?selectColor:Color.black,val?null:Color.white,falseVal);
    	GC.frameRect(gc,Color.black,r);
    	}
    	return(false);
    }
    
    /*
     * draw the main window and things on it.  
     * If gc!=null then actually draw, 
     * If selectPos is not null, then as you draw (or pretend to draw) notice if
     * you are drawing under the current position of the mouse, and if so if you could
     * click there to do something.  Care must be taken to consider if a click really
     * ought to be allowed, considering spectator status, use of the scroll controls,
     * if some board token is already actively moving, and if the game is active or over.
     * 
     * This dual purpose (draw, and notice mouse sensitive areas) tends to make the
     * code a little complicated, but it is the most reliable way to make sure the
     * mouse logic is in sync with the drawing logic.
     * 
    General GUI checklist

    vcr scroll section always tracks, scroll bar drags
    lift rect always works
    zoom rect always works
    drag board always works
    pieces can be picked or dragged
    moving pieces always track
    stray buttons are insensitive when dragging a piece
    stray buttons and pick/drop are inactive when not on turn
*/
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  TabGameBoard gb = disB(gc);

       TablutState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
       if(gc!=null)
	   	{
	   	// note this gets called in the game loop as well as in the display loop
	   	// and is pretty expensive, so we shouldn't do it in the mouse-only case
	       gb.SetDisplayRectangle(boardRect); 	   // this is necessary to inform disb of the board geometry
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
            drawShipRect(gc,pl,gb,shipRects[i],b.playerChipPool(i),selectPos);
         	if(planned && (i==gb.whoseTurn))
         	{
         		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? selectPos : null), 
     					HighlightColor, rackBackGroundColor);
         	}
         	pl.setRotatedContext(gc, selectPos, true);
         }	
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
      

       {
       HitPoint sp = ourTurnSelect;
       switch(state)
       {	
       case PUZZLE_STATE:
    	   if(gb.moveNumber>1) { sp=null; }
    	   break;
       case REARRANGE_GOLD_STATE:
       case REARRANGE_SILVER_STATE: 
    	   break;
       default: sp=null;
       }
       drawOptionRect(gc,sp,gb,flagshipWinRect,TabId.CornerWin);
       drawOptionRect(gc,sp,gb,flagOwnRect,TabId.ExclusiveCenter);
       drawOptionRect(gc,sp,gb,flagCaptureRect,TabId.FlagShipCaptures);
       drawOptionRect(gc,sp,gb,flagFoursideRect,TabId.FourSideCaptures);
       }
 
       if((state==TablutState.PUZZLE_STATE) || (state==TablutState.GAMEOVER_STATE))
       	{ 
           drawShipRect(gc,pl,gb,goldFlagRect,b.goldFlagPool,selectPos);
           if(b.goldFlagPool.topChip()!=null) { StockArt.SmallX.drawChip(gc,this,goldFlagRect,null); }
      	}
       GC.setFont(gc,standardBoldFont());
       drawPlayerStuff(gc,(state==TablutState.PUZZLE_STATE),nonDragSelect,
	   			HighlightColor, rackBackGroundColor);

       double messageRotation = pl.messageRotation();
       // draw the board control buttons 
       boolean conf = (state==TablutState.CONFIRM_SWAP_STATE) ;
		if(conf
			|| (state==TablutState.PLAY_OR_SWAP_STATE) 
			|| (state==TablutState.PUZZLE_STATE))
		{ // make the "swap" button appear if we're in the co;rrect state
			swapButton.highlightWhenIsOn = true;
        	swapButton.setIsOn(conf);
        	swapButton.show(gc,messageRotation, buttonSelect);
		}

		if (state != TablutState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);  
            }


 
		// draw the avatars
        standardGameMessage(gc,messageRotation,
        		state==TablutState.GAMEOVER_STATE?gameOverMessage(gb):s.get(state.getDescription()),
        				state!=TablutState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,nonDragSelect,s.get(GoalString),progressRect, goalRect);
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),swapButton);	
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

        handleExecute(b,mm,replay);
        
        numberMenu.recordSequenceNumber(b.moveNumber());
        startBoardAnimations(replay,b.animationStack,BOARDCELLSIZE,MovementStyle.Sequential);

        if(replay.animate) { playSounds(mm); }
		lastDropped = b.lastDroppedDest;	// this is for the image adjustment logic
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
        return (new Tabmovespec(st, player));
    }

    

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof TabId)// not dragging anything yet, so maybe start
        {

        TabId hitObject =  (TabId)hp.hitCode;
        TabCell hitCell = hitCell(hp);
 	    switch(hitObject)
	    {
	    default: break;
	    
        case GoldFlagLocation:
	    case SilverShipLocation:
	    case GoldShipLocation:
	    	PerformAndTransmit("Pick "+hitObject.shortName);
	    	break;
	    case BoardLocation:
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    	break;
        }
         }
    }
	private void doDropChip(char col,int row)
	{	TablutState state = b.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case PUZZLE_STATE:
			{
			TabChip mo = b.lastPicked();
			if(mo!=null) { PerformAndTransmit("dropb "+mo.chipName+" "+col+" "+row); }
			}
			break;
		case CONFIRM_STATE:
		case CONFIRM_NOSWAP_STATE:
		case PLAY_STATE:
		case REARRANGE_GOLD_STATE:
		case REARRANGE_SILVER_STATE:
		case PLAY_OR_SWAP_STATE:
			PerformAndTransmit("dropb "+b.getPlayerChip(b.whoseTurn).chipName	+ " "+col+" "+row);
			break;
					                 
		
		}
	}

	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof TabId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	TabId hitCode = (TabId)hp.hitCode;
        TabCell hitObject = hitCell(hp);
        TablutState state = b.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case BoardLocation:	// we hit an occupied part of the board 
        case EmptyBoardLocation:
			switch(state)
			{
				default:
					throw G.Error("Not expecting %s in state %s",hitCode,state);
				case CONFIRM_STATE:
				case PLAY_STATE:
				case CONFIRM_NOSWAP_STATE:
				case PLAY_OR_SWAP_STATE:
				case PUZZLE_STATE:
				case REARRANGE_GOLD_STATE:
				case REARRANGE_SILVER_STATE:
					doDropChip(hitObject.col,hitObject.row);
					break;
			}
			break;
        case SilverShipLocation:
        case GoldShipLocation:
        case GoldFlagLocation:
        	PerformAndTransmit("Drop "+b.pickedObject.chipName);
             break;
        case CornerWin:
        case FlagShipCaptures:
        case ExclusiveCenter:
        case FourSideCaptures:
         	{
        	boolean newval = !b.getOptionValue(hitCode);
        	PerformAndTransmit("SetOption "+hitCode.shortName+" "+newval);
        	}
        	break;
        }
        }
    }

    // return what will be the init type for the game
    public String gameType() // this is the subgame "setup" within the master type.
    	{ return(b.gameType()); 
    	}	
    public String sgfGameType() { return(Tablut_SGF); }	// this is the official SGF number assigned to the game
    public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	String initToken = token;
    	do { String nt = his.nextToken();
    		 if(ENDOPTIONS.equals(nt)) { break; }
    		 initToken += " " + nt;
    		} while(true);

        b.doInit(initToken);
       // G.Error("Check this");
     }

/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically.
 * 
 * This is a good place to make notes about threads.  Threads in Java are
 * very dangerous and tend to lead to all kinds of undesirable and/or flakey
 * behavior.  The fundamental problem is that there are three or four sources
 * of events from different system-provided threads, and unless you are very
 * careful, these threads will all try to use and modify the same data
 * structures at the same time.   Java "synchronized" declarations are
 * hard to get right, resulting in synchronization locks, or lack of
 * synchronization where it is really needed.
 * 
 * This toolkit addresses this problem by adopting the "one thread" model,
 * and this is where it is.  Any other threads should do as little as possible,
 * mainly leave breadcrumbs that will be picked up by this thread.
 * 
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
    public BoardProtocol getBoard()   {    return (b);   }


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new TabPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/23/2023
		10667 files visited 0 problems
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
                b.doInit(value);
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
            	if(replayStandardProps(name,value))
            	{
            		if(date_property.equals(name))
            		{	BSDate dd = new BSDate(value);
            			BSDate ta = new BSDate("Jan 1 2012");
            			sloppyDigest = dd.before(ta);
            			b.setCaptureCompatibility(value);
            		}
            	}
            }

            prop = prop.next;
        }

        if (!"".equals(comments))
        {
            setComment(comments);
        }
    }
    
    //
    // support for the last move "numberMenu" logic
    //
	public int getLastPlacement() {
		return (b.moveNumber);
	}

}
