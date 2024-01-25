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
package volo;

import java.awt.*;


import lib.Graphics;
import lib.Image;
import online.common.*;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.Random;
import lib.StockArt;
import lib.TextButton;
import online.game.*;
import online.game.RBoard.DrawingStyle;

import java.util.Hashtable;
import java.util.StringTokenizer;

import common.GameInfo;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;


/**
 *  Volo 
 */
public class VoloViewer extends CCanvas<VoloCell,VoloBoard> implements VoloConstants, GameLayoutClient
{	static final long serialVersionUID = 1000;
     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(210,240,240);
    private Color rackBackGroundColor = new Color(180,225,225);
    private Color boardBackgroundColor = new Color(100,100,155);

    // images, shared among all instances of the class so loaded only once
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// board image etc.
    
    // private state
    private VoloBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
     
    //
    // addZoneRect also sets the rectangle as specifically known to the 
    // mouse tracker.  The zones are considered in the order that they are
    // added, so the smaller ones should be first, then any catchall.
    //
    // zones ought to be mostly irrelevant if there is only one board layout.
    //
    private Rectangle chipRects[] = addRect("chip",2);
    private TextButton passButton = addButton(PASS,GameId.HitPassButton,ExplainPass,
 			HighlightColor, rackBackGroundColor);

    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	VoloChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	if (images == null)
    	{ 	// note that for this to work correctly, the images and masks must be the same size.  
        	// Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    		
    		// images and textures are static variables, so they're shared by
    		// the entire class and only get loaded once.  Special synchronization
    		// tricks are used to make sure this is true.
    		
    	  // load the background textures as simple images
          textures = loader.load_images(ImageDir,TextureNames);
          // load the black and white borders as stock art.
          images = loader.load_masked_images(ImageDir,ImageNames);
    	}
    	gameIcon = textures[ICON_INDEX];
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	// int players_in_game = Math.max(3,info.getInt(exHashtable.PLAYERS_IN_GAME,4));
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//
    	enableAutoDone = true;
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default

         
        
        bb = new VoloBoard(info.getString(GameInfo.GAMETYPE, Volo_Init),
        		getStartingColorMap());
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
    	{ startFirstPlayer();
    	}
    }
    
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*3;
    	int chipH = unitsize*3;
    	int doneW = unitsize*4;
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW, chipH);
    	G.SetRect(done, x, y+chipH+unitsize/2, doneW, plannedSeating()?doneW/2:0);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW+unitsize,y,unitsize);
    	pl.displayRotation = rotation;
    	G.union(box, chip,done);
    	return(box);
    }
    
    private int boardRows = 18;

    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect, x, y, width, height);
	  	GameLayoutManager layout = selectedLayout;
		int nPlayers = nPlayers();
	   	int chatHeight = selectChatHeight(height);
	   	// ground the size of chat and logs in the font, which is already selected
		// to be appropriate to the window size
		int fh = standardFontSize();
		int minLogW = fh*20;	
	   	int minChatW = fh*35;	
	    int minLogH = fh*10;	
	    int vcrW = fh*15;
	    int margin = fh/2;
	    int buttonW = fh*8;
	    int nrows = boardRows;
	    int ncols = boardRows;
	    	// this does the layout of the player boxes, and leaves
		// a central hole for the board.
		//double bestPercent = 
		layout.selectLayout(this, nPlayers, width, height,
				margin,	
				0.75,	// 60% of space allocated to the board
				1,	// aspect ratio for the board
				fh*2.5,	// minimum cell size
				fh*3.5,	// maximum cell size
				0.5		// preference for the designated layout, if any
				);
		
	    // place the chat and log automatically, preferring to place
		// them together and not encroaching on the main rectangle.
		layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,3*minChatW/2,3*chatHeight/2,logRect,
				minLogW, minLogH, minLogW*3/2, minLogH*3/2);
	   	layout.placeDoneEdit(buttonW,buttonW,doneRect,editRect);
		layout.placeTheVcr(this,vcrW,vcrW*3/2);
	

		Rectangle main = layout.getMainRectangle();
		int mainX = G.Left(main);
		int mainY = G.Top(main);
		int mainW = G.Width(main);
		int mainH = G.Height(main);
		
		// calculate a suitable cell size for the board
		double cs = Math.min((double)mainW/(ncols+1),(double)mainH/(nrows+1.5));
		CELLSIZE = (int)cs;
		//G.print("cell "+cs0+" "+cs+" "+bestPercent);
		// center the board in the remaining space
	    int stateH = (int)(fh*2.5);
		int boardW = (int)((ncols+1)*CELLSIZE);
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
	    G.placeStateRow( stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,noChatRect);
		G.SetRect(boardRect,boardX,boardY,boardW,boardH);
        G.SetRect(passButton,boardX+ CELLSIZE,boardY + CELLSIZE,CELLSIZE*3,CELLSIZE*3/2);
		
	//	G.SetRect(shooterChipRect,boardRight,boardY+boardH/2-SQUARESIZE/2,SQUARESIZE,SQUARESIZE);
		// goal and bottom ornaments, depending on the rendering can share
		// the rectangle or can be offset downward.  Remember that the grid
		// can intrude too.
		G.SetRect(goalRect, boardX, boardBottom,boardW,stateH);       
	    setProgressRect(progressRect,goalRect);
	    positionTheChat(chatRect,chatBackgroundColor,chatBackgroundColor);

    }

	// draw a box of spare chips. For volo it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,VoloBoard gb)
    {	VoloCell c = gb.playerCells[player];
    
        boolean canhit = gb.LegalToHitChips(player) && G.pointInRect(highlight, r);
        if (canhit)
        {
            highlight.hitCode = gb.playerColor[player];
            highlight.arrow = (gb.pickedObject!=null)?StockArt.DownArrow:StockArt.UpArrow;
            highlight.awidth = CELLSIZE;
            highlight.hit_x = G.centerX(r);
            highlight.hit_y = G.centerY(r);
            highlight.spriteColor = Color.red;
        }

        if (gc != null)
        { // draw a random pile of chips.  It's just for effect

            int spacex = G.Width(r) - CELLSIZE;
            int spacey = G.Height(r) - CELLSIZE;
            Random rand = new Random(4321 + player); // consistant randoms, different for black and white 

            if (canhit)
            {	// draw a highlight background if appropriate
                GC.fillRect(gc, HighlightColor, r);
            }

            GC.frameRect(gc, Color.black, r);
            VoloChip chip = gb.playerChip[player];
            int nc = 20;	
            if(spacex>0)// draw 20 chips
            {
            while (nc-- > 0)
            {	int rx =Random.nextInt(rand,spacex);
                int ry = Random.nextInt(rand,spacey);
                c.drawChip(gc,this,chip,CELLSIZE,G.Left(r)+CELLSIZE/2+rx,G.Top(r)+CELLSIZE/2+ry,null);
            }}
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
    	VoloChip.getChip(obj).drawChip(g,this,bb.cellSize(), xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  

    Image background = null;
    Image scaled = null;
    
    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { 
      setDisplayParameters();
      VoloBoard gb = disB(gc);
      boolean review = reviewMode() && !mutable_game_record;
      // erase
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      Image board = images[gb.smallBoard?BOARD84_INDEX:BOARD_INDEX];
      if(board!=background) { scaled = null; }
      background = board;
      scaled = board.centerScaledImage(gc, boardRect,scaled);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      gb.drawing_style = DrawingStyle.STYLE_NOTHING;
      gb.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);


 
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
    //public String encodeScreenZone(int x, int y,Point p)
    //{
    //	return(super.encodeScreenZone(x,y,p));
    //}

   /** draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */
    private void drawBoardElements(Graphics gc, VoloBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  Either gc or highlight might be
    	// null, but not both.
        //
        G.Assert(gb.chipsOnBoard()>3,"no chips");
    	VoloCell startLine = gb.startOfLine;
    	VoloCell endLine = gb.endOfLine;
    	Hashtable<VoloCell,VoloMovespec> slideDests = gb.getSlideDests();
    	Hashtable<VoloCell,VoloMovespec> selectDests = gb.getSelectDests();
    	VoloBlob orangeZone = gb.getReserveSet(0);
    	VoloBlob blueZone = gb.getReserveSet(1);

        // using closestCell is preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.

        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.

        for(VoloCell cell = gb.allCells; cell!=null; cell=cell.next)
          {
            boolean drawhighlight = gb.isDest(cell) 		// is legal for a "drop" operation
   				|| gb.isSource(cell);	// is legal for a "pick" operation+
        	 int ypos = G.Bottom(brect) - gb.cellToY(cell);
             int xpos = G.Left(brect) + gb.cellToX(cell);
             if (drawhighlight || (cell==startLine && cell.chip!=null))
             { // checking for pointable position
            	 StockArt.SmallO.drawChip(gc,this,gb.cellSize()*5,xpos,ypos,null);                
             }

            boolean hitCell = gb.LegalToHitBoard(cell);

            if(cell.drawChip(gc,this,hitCell ? highlight : null,gb.cellSize(),xpos,ypos,null))
            {
                boolean empty = (cell.chip == null);
                boolean picked = (gb.pickedObject!=null);
                highlight.hitCode = (empty||picked) ? VoloId.EmptyBoard : VoloId.BoardLocation;
                highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
                highlight.awidth = 2*CELLSIZE/3;
                highlight.spriteColor = Color.red;
            }
            
            if((cell==startLine) || (selectDests.get(cell)!=null)||(cell==endLine) || (slideDests.get(cell)!=null))
            {
            	StockArt.SmallO.drawChip(gc,this,CELLSIZE*2,xpos,ypos,null);
            }
            if((orangeZone!=null) && (orangeZone.contains(cell)))
        	{  VoloChip.BlueO.drawChip(gc,this,CELLSIZE/3,xpos,ypos,null);
        	}
            if((blueZone!=null) && (blueZone.contains(cell)))
          	{  VoloChip.OrangeO.drawChip(gc,this,CELLSIZE/3,xpos,ypos,null);
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
    {  VoloBoard gb = disB(gc);
       VoloState state = gb.getState();
              
       boolean moving = hasMovingObject(selectPos);
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
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
       {
    	 commonPlayer cpl = getPlayerOrTemp(i);
    	 cpl.setRotatedContext(gc, selectPos, false);
    	 DrawChipPool(gc, chipRects[i], i, ourTurnSelect,gb);
    	 if(planned && (i==gb.whoseTurn))
    	 	{
    		 handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
 					HighlightColor, rackBackGroundColor);
    	 	}
    	 cpl.setRotatedContext(gc, selectPos, true);
       }

       GC.setFont(gc,standardBoldFont());
	    
        if(state==VoloState.PLAY_STATE)
        {	passButton.show(gc, messageRotation,buttonSelect);
        }

		if (state != VoloState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive()) 
				{handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos,HighlightColor, rackBackGroundColor);
        }

 
        drawPlayerStuff(gc,state==VoloState.PUZZLE_STATE,nonDragSelect,HighlightColor,rackBackGroundColor);
  
 
	// draw the avatars
        standardGameMessage(gc,messageRotation,
        		state==VoloState.GAMEOVER_STATE?gameOverMessage(gb):s.get(state.getDescription()),
        				state!=VoloState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
        
        goalAndProgressMessage(gc,nonDragSelect,s.get("connect all your birds into one flock"),progressRect, goalRect);
        //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for volo
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
        return (new VoloMovespec(st, player));
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
      {
    	  commonMove rval = super.EditHistory(nmove);
    	  if((rval!=null)
    		 && (bb.getState()==VoloState.CONFIRM_STATE)
    	     && (rval.op==MOVE_DROPB))
    	  {
    		  // a peculiarity of the volo engine, if we have dropped a stone,
    		  // we can change our mind by simply dropping another stone elsewhere,
    		  // or dropping it back in the pool, or picking up a new stone from the pool.
    		  // Most games do not need this extra logic.
    		  int idx = History.size()-1;
    		  while(idx>=0)
    		  {	VoloMovespec oldMove = (VoloMovespec)History.elementAt(idx);
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
 //   public commonMove EditHistory(commonMove nmove)
 //   {
 //       VoloMovespec newmove = (VoloMovespec) nmove;
 //       VoloMovespec rval = newmove;			// default returned value
 //       int state = bb.board_state;
 //       int idx = History.size() - 1;
 //       
 //      while(idx>=0)
 //       {	VoloMovespec m = (VoloMovespec) History.elementAt(idx);
 //       	int start_idx = idx;
 //           if(m.next!=null) 
 //           	{ // editing history, don't edit directly
 //           		switch(newmove.op)
 //           		{	case MOVE_PICK:
 //           			case MOVE_DROP:
 //           				rval = null;
 //           			default: break;
 //           		}
 //           		idx = -1; 
 //           	}
 //            else {
 //           switch(newmove.op)
 //       	{
 //           case MOVE_SWAP:
 //           	if(m.op==MOVE_SWAP) 
 //           		{ UndoHistoryElement(idx);
 //            		  rval = null;
 //           		  }
 //      		  idx=-1;
 //             break;
 //       	case MOVE_DONE:
 //       	default:
 //       		idx = -1;
 //       		break;
 //       	case MOVE_PICK:	
 //       		if((state!=PUZZLE_STATE) && (m.op==MOVE_DROPB))
 //       			{ UndoHistoryElement(idx); idx=-1; }
 //          	case MOVE_DROP:
 //          		if((idx>1) && (m.op==MOVE_PICKB) && (state!=PUZZLE_STATE))
 //          		{
 //          			VoloMovespec m2 = (VoloMovespec) History.elementAt(idx-1);
 //          			if((m2.op==MOVE_DROPB)
 //          				&& (m2.to_row==m.to_row)
 //          				&& (m2.to_col==m.to_col))
 //          				{
 //         				UndoHistoryElement(idx);
 //          				UndoHistoryElement(idx-1);
 //          				rval = null;
 //          				idx = -1;
 //          				}
 //          				
 //          		}
 //          		else if(m.op!=MOVE_PICKB)
 //          		{
 //       		rval = null;		// picks don't appear in the history.  This is probably peculiar to volo
 //          		}
 //       		idx = -1;
 //       		break;
 //       	case MOVE_RESET:
 //       		// reset is a special move that gets you back to the start of the
 //       		// current player's turn, effectively unwinding all actions taken
 //       		// so far.
 //       		rval = null;		// reset is never recorded in the history
 //       		// fall through
 //       	case MOVE_RESIGN:		// resign is your whole move, so acts like reset.
 //       		switch(m.op)
 //       		{
 //               default:
 //            		if(state==PUZZLE_STATE) { idx = -1; break; }
 //               case MOVE_PICKB:
 //               case MOVE_PICK:
 //                   UndoHistoryElement(idx);
 //               	idx--;
 //                   break;
 //               case MOVE_DONE: // these stop the scan 
 //               case MOVE_EDIT:
 //               case MOVE_START:
 //                   idx = -1;
 //               }
 //       		break;
 //       	case MOVE_DROPB:
 //       		if(state!=PUZZLE_STATE)
 //       		{	if(m.op==MOVE_DROPB) 
 //       			{ UndoHistoryElement(idx); 
 //       			}
 //       		}
 //       		else
 //       		{
 //       		if((idx>2)&& (m.op==MOVE_PICKB))
 //       		{
 //       		VoloMovespec m2 = (VoloMovespec) History.elementAt(idx-1);	
 //      		VoloMovespec m3 = (VoloMovespec) History.elementAt(idx-2);	
 //       		if((m3.op==MOVE_PICKB)
 //       				&& (m2.source==EmptyBoard)
 //       				&& (m2.op==MOVE_DROPB)
 //       				&& (m2.to_col==m.to_col)
 //       				&& (m2.to_row==m.to_row))
 //       			{	UndoHistoryElement(idx);
 //       				UndoHistoryElement(idx-1);
 //       			}
 //       		}
 //       		}
 //       		idx = -1;
 //       		break;
 //       	case MOVE_PICKB:
 //       		if(state!=PUZZLE_STATE)
 //       		{
 //       		switch(m.op)
 //       		{
 //       		case MOVE_PICKB:
 //       		case MOVE_DROPB:
 //       			if( (newmove.to_col==m.to_col)
 //           				&& (newmove.to_row==m.to_row))
 //           			{
 //           				UndoHistoryElement(idx);
 //           				if(newmove.op!=m.op) // protect against bounces
 //           					{ rval = null; }
 //           			}
 //       		}}
 //  				idx = -1;
 //  			 	break;
 //       		}	// end of switch on new move
 //           }
 //           G.Assert(idx!=start_idx,"progress editing history");
 //           }
 //
 //       return (rval);
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
        if (hp.hitCode instanceof VoloId)// not dragging anything yet, so maybe start
        {
       	VoloId hitObject = (VoloId) hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    
	    case Orange_Chip_Pool:
	    case Blue_Chip_Pool:
	    	PerformAndTransmit("Pick "+hitObject.shortName);
	    	break;
       }
        }
    }
	private void doDropChip(char col,int row)
	{	VoloState state = bb.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case PUZZLE_STATE:
		{
		VoloChip mo = bb.pickedObject;
		if(mo==null) { mo=bb.lastPicked; }
		if(mo==null) { mo=bb.playerChip[bb.whoseTurn]; }
		PerformAndTransmit("dropb "+col+" "+row);
		}
		break;
		case CONFIRM_STATE:
		case PLAY_STATE:
		case PLAY_OR_SLIDE_STATE:
			PerformAndTransmit("dropb "+col+" "+row);
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
        if(!(id instanceof VoloId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	VoloId hitCode = (VoloId)hp.hitCode;
        VoloCell hitObject = hitCell(hp);
        VoloState state = bb.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case DESIGNATE_ZONE_STATE:
			case PLAY_OR_SLIDE_STATE:
			case SECOND_SLIDE_STATE:
				PerformAndTransmit("Select "+hitObject.col+" "+hitObject.row); 
				break;
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
				PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
				break;

			}
			break;
			
        case EmptyBoard:
			switch(state)
			{
				default:
					throw G.Error("Not expecting EmptyBoard in state %s",state);
				case LAND_FLOCK_STATE:
					{	Hashtable<VoloCell,VoloMovespec> slideDests = bb.getSlideDests();
						VoloMovespec spec = slideDests.get(bb.getCell(hitObject));
						PerformAndTransmit(spec.moveString());
					}
					break;
				case DESIGNATE_ZONE_STATE:
					PerformAndTransmit("Select "+hitObject.col+" "+hitObject.row); 
					break;
				case CONFIRM_STATE:
				case PLAY_STATE:
				case PUZZLE_STATE:
				case PLAY_OR_SLIDE_STATE:
					doDropChip(hitObject.col,hitObject.row);
					break;
			}
			break;
			
        case Blue_Chip_Pool:
        case Orange_Chip_Pool:
           if(bb.pickedObject!=null) 
			{//if we're dragging a chip around, drop it.
            	PerformAndTransmit("Drop "+hitCode.shortName);
			}
           break;
        }
       }
    }

    public void setDisplayParameters()
    {
      	  // 0.95 and 1.0 are more or less magic numbers to match the board to the artwork
          if(bb.smallBoard)
        	  {
        	  bb.SetDisplayParameters(0.99, 0.99, .02,-0.4, 0); // 
        	  }
          else 
          	{ bb.SetDisplayParameters(1.112, 0.99, .02,-0.6, 0); // 
          	}
     
      	bb.SetDisplayRectangle(boardRect);

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
    	   // in games which have a randomized start, this method would return
    	   // return(bb.gametype+" "+bb.randomKey); 
    	return(bb.gametype); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Volo_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // int rk = G.IntToken(his);
    	// bb.doInit(token,rk);
        bb.doInit(token);
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
    { 	return new VoloPlay();
    }
    public boolean parsePlayerInfo(commonPlayer p,String first,StringTokenizer tokens)
    {
    	if(OnlineConstants.TIME.equals(first) && bb.DoneState())
    	{
    		PerformAndTransmit("Done",false,replayMode.Replay);
    	}
    	return(super.parsePlayerInfo(p, first, tokens));
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/23/2023
     * 2180 files visited 0 problems
     * 
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
            else if(game_property.equals(name) && "11".equals(value))	// oops
           {}
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

