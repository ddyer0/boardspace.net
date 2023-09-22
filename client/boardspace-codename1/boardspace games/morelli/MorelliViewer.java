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
package morelli;

import bridge.*;
import lib.Graphics;
import lib.Image;
import com.codename1.ui.geom.Rectangle;


import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Text;
import lib.TextChunk;
import static morelli.MorelliMovespec.*;

/**
 * This code shows the overall structure appropriate for a game view window.
*/
public class MorelliViewer extends CCanvas<MorelliCell,MorelliBoard> implements MorelliConstants, GameLayoutClient,PlacementProvider
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(170,175,168);
    private Color boardBackgroundColor = new Color(160,165,155);
    private Color BlackArrowColor = new Color(230,200,255);;
    
    private boolean checkerEffect = true;
    public int getAltChipset() {  return(checkerEffect?1:0); }
    // images
    private static Image[] textures = null;// background textures
    
    // private state
    private MorelliBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("chip",2);
    
    private JCheckBoxMenuItem checkerOption = null;
   
    private Rectangle altSetupRect = addRect("altSetup");
    
    public synchronized void preloadImages()
    {	
       	MorelliChip.preloadImages(loader,ImageDir);
        if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
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
    {	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	// int players_in_game = Math.max(3,info.getInt(exHashtable.PLAYERS_IN_GAME,4));
    	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
    	enableAutoDone = true;
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        
        b = new MorelliBoard(info.getString(OnlineConstants.GAMETYPE, Variations.morelli_13.name),
        		randomKey,players_in_game,getStartingColorMap(),MorelliBoard.REVISION);
        useDirectDrawing(true);
        doInit(false);
        checkerOption = myFrame.addOption(s.get(CheckerEffect),checkerEffect,deferredEvents);

        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        int rev = b.revision;
        b.doInit(b.gametype,b.randomKey,rev,b.setup);			// initialize the board
        if(!preserve_history)
    	{
    	startFirstPlayer();
    	}

    }

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*3;
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW, chipW);
    	int doneW = plannedSeating() ? unitsize*4 : 0;
    	G.SetRect(done, x, y+chipW, doneW, doneW/2);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
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
        int margin = fh/2;
        int buttonW = fh*8;
        int nrows = b.boardRows;  
        int ncols = b.boardColumns;
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	//
    			0.75,	// % of space allocated to the board
    			1.0,	// 1:1 aspect ratio for the board
    			fh*2,	// minimum cell size
    			fh*3,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);

    	layout.placeRectangle(altSetupRect,buttonW,buttonW/2,buttonW,buttonW/2,BoxAlignment.Edge,false);
 
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	int CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE;
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
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);

    }
                
    private MorelliChip playerChip(int forPlayer)
    {
    	return MorelliChip.getChip(b.getColorMap()[forPlayer]);
    }
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, MorelliBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	
        MorelliChip thisChip = playerChip(forPlayer);
        GC.setFont(gc,largeBoldFont());
        thisChip.drawChip(gc,this,r,allowed_to_edit?""+gb.forwardPotential(forPlayer):null);
     }

    //
    // sprites are normally a game piece that is "in the air" being moved
    // around.  This is called when dragging your own pieces, and also when
    // presenting the motion of your opponent's pieces, and also during replay
    // when a piece is picked up and not yet placed.  While "obj" is nominally
    // a game piece, it is really whatever is associated with b.movingObject()
    //
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {  	// draw an object being dragged
    	MorelliChip ch = MorelliChip.getChip(obj);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);
     }

 

    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	boolean reviewBackground = reviewMode()&&!mutable_game_record;
      // erase
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);
	    b.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
	    b.SetDisplayRectangle(boardRect);

      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    private void drawAltBoards(Graphics gc,MorelliBoard gb,HitPoint highlight,Rectangle rect)
    {	Setup setups[] = Setup.values();
    	int sidx = 0;
    	int nBoards = setups.length - 2;	// exclude free and the main one
    	int szw = G.Width(rect)/nBoards;
    	int sz = Math.min(G.Height(rect),szw);
    	int cx = G.Left(rect);
    	int cy = G.Top(rect) + (G.Height(rect)-sz)/2;
    	int squareSize = sz/(gb.boardRows);
    	int cellSize = squareSize/4;
    	Setup mainSetup = gb.setup;
    	MorelliBoard tempB = (MorelliBoard)gb.cloneBoard();
    	for(int idx = 0;idx<nBoards; idx++)
    	{	if(setups[sidx]==mainSetup) { sidx++; }
    		Setup current = setups[sidx];
    		Rectangle newR = new Rectangle(cx-cellSize,cy-cellSize,sz+cellSize*2,sz+cellSize*2);
    		tempB.doInit(tempB.gametype,tempB.randomKey,tempB.revision,current);
    		tempB.SetDisplayRectangle(newR);
    		drawBoardElements(gc,tempB,newR,null,squareSize);
    		if(G.pointInRect(highlight,newR))
    		{
    			highlight.hitCode = current.id;
    			highlight.spriteRect = newR;
        		highlight.spriteColor = Color.red;    		}
    		cx += sz;
    		sidx++;
    	}
    	GC.frameRect(gc,Color.black,rect);
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, MorelliBoard gb, Rectangle brect,
    		HitPoint highlight,int squareSize)
    {	
        Hashtable<MorelliCell,MorelliCell>dests = gb.getDests();
        MorelliCell lastDest = gb.lastDest;
        MorelliCell lastSrc = gb.lastSrc;
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
        MorelliCell hitCell = null;
        numberMenu.clearSequenceNumbers();
        Enumeration<MorelliCell> cells = gb.getIterator(Itype.LRTB);
        while (cells.hasMoreElements())
        {
            MorelliCell cell = cells.nextElement();
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            boolean canHit = gb.LegalToHitBoard(cell);
            boolean isDest = dests.get(cell)!=null;
            numberMenu.saveSequenceNumber(cell,xpos,ypos,cell.lastEmptiedPlayer==0 ? labelColor : BlackArrowColor);

            if( cell.drawStack(gc,this,canHit?highlight:null,squareSize,xpos,ypos,0,0.1,null)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	hitCell = cell;
            	}

            if(isDest || (cell==lastDest) || (cell==lastSrc))
            	{	StockArt.SmallO.drawChip(gc, this, squareSize, xpos,ypos,null);
            	}
        	}
    	if(hitCell!=null)
    	{
        	highlight.arrow =hasMovingObject(highlight)
  				? StockArt.DownArrow 
  				: hitCell.topChip()!=null?StockArt.UpArrow:null;
        	highlight.awidth = squareSize/2;
        	highlight.spriteColor = Color.red;
    	}
    	numberMenu.drawSequenceNumbers(gc,SQUARESIZE,labelFont,labelColor);
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    { 
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  MorelliBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      MorelliState vstate = gb.getState();
      int FontHeight = standardFontSize();
      gameLog.redrawGameLog(gc, ourSelect, logRect, Color.black,Color.white,standardPlainFont(),
    		  	G.getFont("monospaced",G.Style.Bold,FontHeight+5));
    
        drawBoardElements(gc, gb, boardRect, ot,SQUARESIZE);
        if(gb.getState()==MorelliState.FirstPlay)
        {
        	drawAltBoards(gc,gb,ourSelect,altSetupRect);
        }
        boolean planned = plannedSeating();
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
        double messageRotation = pl.messageRotation();       
        
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
        {
        commonPlayer cpl = getPlayerOrTemp(i);
        cpl.setRotatedContext(gc, highlight, false);
        DrawCommonChipPool(gc, gb,i,chipRects[i], gb.whoseTurn,ot);
        if(planned && (gb.whoseTurn==i))
        {
			handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
        
        }
        cpl.setRotatedContext(gc, highlight, true);
        }
        GC.setFont(gc,standardBoldFont());
		if (vstate != MorelliState.Puzzle)
        {
			if(!planned && !autoDoneActive()) 
				{ handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
                }

 		drawPlayerStuff(gc,(vstate==MorelliState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);


        
 		standardGameMessage(gc,messageRotation,
            		vstate==MorelliState.Gameover
            			?gameOverMessage()
            			:s.get(vstate.getDescription()),
            				vstate!=MorelliState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
 		playerChip(gb.whoseTurn).drawChip(gc,this,iconRect,null);
            goalAndProgressMessage(gc,ourSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
         
        drawAuxControls(gc,ourSelect);
        drawVcrGroup(ourSelect, gc);

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
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Simultaneous);
        if(replay!=replayMode.Replay) { playSounds(mm); }
 
        return (true);
    }
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new MorelliMovespec(st, player));
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
    		return(EditHistory(nmove,(nmove.op==MorelliMovespec.CHOOSE_SETUP)));
    	}

//    public commonMove EditHistory(commonMove nmove)
//    {
//    	MorelliMovespec newmove = (MorelliMovespec) nmove;
//    	MorelliMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            MorelliMovespec m = (MorelliMovespec) History.elementAt(idx);
//                if(m.next!=null) { idx = -1; }
//                else 
//               {
//                switch (newmove.op)
//                {
//                case MOVE_RESET:
//                	rval = null;	// reset never appears in the record
//                 case MOVE_RESIGN:
//                	// resign unwind any preliminary motions
//                	switch(m.op)
//                	{
//                  	default:	
//                 		if(state==PUZZLE_STATE) { idx = -1; break; }
//                 	case MOVE_PICK:
//                 	case MOVE_PICKB:
//               		UndoHistoryElement(idx);	// undo back to last done
//                		idx--;
//                		break;
//                	case MOVE_DONE:
//                	case MOVE_START:
//                	case MOVE_EDIT:
//                		idx = -1;	// stop the scan
//                	}
//                	break;
//                	
//             case MOVE_DONE:
//             default:
//            		idx = -1;
//            		break;
//               case MOVE_DROPB:
//                	if(m.op==MOVE_PICKB)
//                	{	if((newmove.to_col==m.from_col)
//                			&&(newmove.to_row==m.from_row))
//                		{ UndoHistoryElement(idx);	// pick/drop back to the same spot
//                		  idx--;
//                		  rval=null;
//                		}
//                	else if(idx>0)
//                	{ MorelliMovespec m2 = (MorelliMovespec)History.elementAt(idx-1);
//                	  if((m2.op==MOVE_DROPB)
//                			  && (m2.to_col==m.from_col)
//                			  && (m2.to_row==m.from_row))
//                	  {	// sequence is pick/drop/pick/drop, edit out the middle pick/drop
//                		UndoHistoryElement(idx);
//                	  	UndoHistoryElement(idx-1);
//                	  	idx = idx-2;
//                	  }
//                	  else { idx = -1; }
//                		
//                	}
//                	else { idx = -1; }
//                	}
//                	else { idx = -1; }
//                	break;
//                	
//            	}
//               }
//            G.Assert(idx!=start_idx,"progress editing history");
//            }
//         return (rval);
//    }
//
    
private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_BOARD_BOARD:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
   	break;

    case MOVE_PICKB:
    	playASoundClip(light_drop,100);
    	break;

    case MOVE_DROPB:
      	 playASoundClip(heavy_drop,100);
      	break;
    default: break;
    }
	
}

 
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof MorelliId)// not dragging anything yet, so maybe start
        {
        MorelliId hitObject = (MorelliId)hp.hitCode;
		MorelliCell cell = hitCell(hp);
		MorelliChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
 	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(cell.chipIndex>0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
	    		}
	    	break;
		default:
			break;
        }
		}
        }
    }

	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging( HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof MorelliId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	MorelliId hitObject = (MorelliId)hp.hitCode;
		MorelliState state = b.getState();
		MorelliCell cell = hitCell(hp);
		MorelliChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case RandomSetup:
        case AdjacentSetup:
        case BlocksSetup:
        case OpposingSetup:
        case FreeSetup:
        	PerformAndTransmit("Choose "+Setup.getSetup(hitObject));
        	break;
        	
         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Confirm:
			case Play:
			case SecondPlay:
			case Puzzle:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
				}
				break;
			}
			break;
			
        }
         }
    }


    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parseable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() 
    { 
    	return(""+b.gametype+" "+b.randomKey+" "+b.revision+" "+b.setup.name()); 
   }
    public String sgfGameType() { return(Morelli_SGF); }

    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be an init spec
    	long rk = G.LongToken(his);
    	int rev = G.IntToken(his);
    	Setup setup = null;
     	// make the random key part of the standard initialization,
    	// even though games like morelli probably don't use it.
    	if(rev>=100) { setup = Setup.getSetup(his.nextToken()); }
        b.doInit(token,rk,rev,setup);
        adjustPlayers(2);

    }

    
 //   public void doShowText()
 //   {
 //       if (debug)
 //       {
 //           super.doShowText();
 //       }
 //       else
 //       {
 //           theChat.postMessage(GAMECHANNEL,KEYWORD_CHAT,
 //               s.get(CensoredGameRecordString));
//        }
//    }

    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
    	if(target==checkerOption)
    	{
    	checkerEffect = checkerOption.getState();
    	generalRefresh();
    	return(true);
    	}
    	else 
    	return(super.handleDeferredEvent(target,command));
     }
/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically
 *  */
    
    //   public void ViewerRun(int wait)
    //   {
    //       super.ViewerRun(wait);
    //   }


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new MorelliPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
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
            {	StringTokenizer st = new StringTokenizer(value);
            	String typ = st.nextToken();
            	long ran = G.LongToken(st);
            	int rev = G.IntToken(st);
            	Setup setup = rev>=100 ? Setup.getSetup(st.nextToken()) : null;
                b.doInit(typ,ran,rev,setup);
                adjustPlayers(b.nPlayers());
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (parseVersionCommand(name,value,2)) {}
            else if (parsePlayerCommand(name,value)) {}
            else 
            {
                replayStandardProps(name,value);
            }

            prop = prop.next;
        }

        if (!"".equals(comments))
        {
            setComment(comments);
        }
    }
    

    Color textColors[] = 
    	{	 new Color(110,54,100),	// center color, not actually used
    		new Color(104,59,158),	// purple
    		new Color(13,109,178),	// blue
    		new Color(26,173,0),	// green
    		new Color(193,177,19),	// yellow
    		new Color(193,102,53),	// orange
    		new Color(146,27,29)	// red
    	};
    // return a text chunk in the same color as the coordinate
    public Text coloredCoordinate(char col,int row)
    {	
    	int center = (b.boardColumns-1)/2;
    	int dis = Math.max(Math.abs(row-1-center),Math.abs(col-('A'+center)));
    	return(TextChunk.create(""+col+row+" ",
    				(dis<textColors.length) ? textColors[dis] : null)); 
    	
    }
    
	public int getLastPlacement(boolean empty) {
		return b.moveNumber;
	}
}

