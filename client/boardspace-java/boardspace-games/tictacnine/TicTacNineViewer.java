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
package tictacnine;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.awt.*;
import java.util.*;

import common.GameInfo;
import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Tokenizer;



/**
 * 
 * Change History
 *
 * May 2007 initial work in progress. 
 *

 
*/
public class TicTacNineViewer extends CCanvas<TicTacNineCell,TicTacNineBoard> implements TicTacNineConstants
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// large images
    
    // private state
    private TicTacNineBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    private static int SUBCELL = 4;	// number of cells in a square
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle firstPlayerChipRect = addRect("firstPlayerChipRect");
    private Rectangle secondPlayerChipRect = addRect("secondPlayerChipRect");
    private Rectangle commonChipRect = addRect("commonChipRect");

   
    private Rectangle repRect = addRect("repRect");
    

    public synchronized void preloadImages()
    {	
       	TicTacNineChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
    	}
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
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new TicTacNineBoard(info.getString(GameInfo.GAMETYPE, TicTacNine_INIT),randomKey);
        doInit(false);
 
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);						// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
    }


	/**
	 * this is the main method to do layout of the board and other widgets.  I don't
	 * use swing or any other standard widget kit, or any of the standard layout managers.
	 * they just don't have the flexibility to produce the results I want.  Your mileage
	 * may vary, and of course you're free to use whatever layout and drawing methods you
	 * want to.  However, I do strongly encourage making a UI that is resizable within
	 * reasonable limits, and which has the main "board" object at the left.
	 * 
	 *  The basic layout technique used here is to start with a cell which is about the size
	 *  of a board square, and lay out all the other object relative to the board or to one
	 *  another.  The rectangles don't all have to be on grid points, and don't have to
	 *  be non-overlapping, just so long as the result generally looks good.
	 *  
	 *  When "extraactions" is available, a menu option "show rectangles" works
	 *  with the "addRect" mechanism to help visualize the layout.
	 */ 
    public void setLocalBounds(int x, int y, int width, int height)
    {   int separation=2;
        int sncols = (b.boardColumns*SUBCELL+20); // more cells wide to allow for the aux displays
        int snrows = (b.boardRows+2)*SUBCELL;  
        int cellw = width / sncols;
        int chatHeight = selectChatHeight(height);
        int cellh = (height-chatHeight) / snrows;
        CELLSIZE = Math.max(1,Math.min(cellw, cellh)); //cell size appropriate for the aspect ration of the canvas
        int ideal_logwidth = CELLSIZE * 12;
        SQUARESIZE = CELLSIZE*SUBCELL;
        fullRect.x = 0;			// the whole canvas
        fullRect.y = 0;
        fullRect.width = width;
        fullRect.height = height;

        // game log.  This is generally off to the right, and it's ok if it's not
        // completely visible in all configurations.
        
        boardRect.x = 0;
        boardRect.y = chatHeight+SQUARESIZE-CELLSIZE;
        boardRect.width = SQUARESIZE * b.boardColumns ;
        boardRect.height = SQUARESIZE * b.boardRows;

        int stateX = boardRect.x + CELLSIZE;
        int stateY = chatHeight+CELLSIZE/3;
        int stateH = CELLSIZE;
        G.SetRect(noChatRect, G.Right(boardRect)-stateH, stateY,stateH, stateH);
        stateRect.x = stateX;
        stateRect.y = stateY;
        stateRect.width = G.Left(noChatRect)-stateX;
        stateRect.height = stateH;

        firstPlayerChipRect.x = G.Right(boardRect)-CELLSIZE/2;
        firstPlayerChipRect.y = chatHeight+CELLSIZE;
        firstPlayerChipRect.width = SQUARESIZE;
        firstPlayerChipRect.height = SQUARESIZE;
 
        secondPlayerChipRect.x = firstPlayerChipRect.x;
        secondPlayerChipRect.y = G.Bottom(fullRect)-CELLSIZE-SQUARESIZE;
        secondPlayerChipRect.width = SQUARESIZE;
        secondPlayerChipRect.height = SQUARESIZE;

        chatRect.x = fullRect.x;
        chatRect.y = fullRect.y;
        chatRect.width = Math.max(boardRect.width,fullRect.width-ideal_logwidth-CELLSIZE);
        chatRect.height = chatHeight;

        logRect.x = chatRect.x + chatRect.width+CELLSIZE/3 ;
        logRect.y = chatRect.y ;
        logRect.width = Math.min(ideal_logwidth,fullRect.width-logRect.x);
        logRect.height = chatRect.height;

        
        // "edit" rectangle, available in reviewers to switch to puzzle mode
        editRect.x = G.Right(boardRect)+CELLSIZE*separation;
        editRect.y = G.Bottom(firstPlayerChipRect)+CELLSIZE*2;
        editRect.width = CELLSIZE*6;
        editRect.height = 2*CELLSIZE;

        goalRect.x = boardRect.x;		// really just a general message
        goalRect.y = G.Bottom(boardRect)-CELLSIZE;
        goalRect.height = CELLSIZE*2;
        goalRect.width = boardRect.width;
        
        progressRect.x = goalRect.x+goalRect.width/6;	// a simple progress bar when the robot is running.
        progressRect.width = goalRect.width/2;
        progressRect.y = goalRect.y;
        progressRect.height = CELLSIZE/2;

        {
            commonPlayer pl0 = getPlayerOrTemp(0);
            commonPlayer pl1 = getPlayerOrTemp(1);
            Rectangle p0time = pl0.timeRect;
            Rectangle p1time = pl1.timeRect;
            Rectangle p0anim = pl0.animRect;
            Rectangle p1anim = pl1.animRect;
            Rectangle firstPlayerRect = pl0.nameRect;
            Rectangle secondPlayerRect = pl1.nameRect;
            Rectangle firstPlayerPicRect = pl0.picRect;
            Rectangle secondPlayerPicRect = pl1.picRect;
            
            //first player name
            firstPlayerRect.x = G.Right(firstPlayerChipRect)+CELLSIZE;
            firstPlayerRect.y = firstPlayerChipRect.y;
            firstPlayerRect.width = CELLSIZE * 10;
            firstPlayerRect.height = CELLSIZE*2;
            //second player name
            secondPlayerRect.x = firstPlayerRect.x;
            secondPlayerRect.y = G.Bottom(boardRect) - firstPlayerRect.height;
            secondPlayerRect.width = firstPlayerRect.width;
            secondPlayerRect.height = firstPlayerRect.height;


            // first player portrait
            firstPlayerPicRect.x = G.Right(editRect)+CELLSIZE;
            firstPlayerPicRect.y = G.Bottom(firstPlayerRect);
            firstPlayerPicRect.width = CELLSIZE * 8;
            firstPlayerPicRect.height = CELLSIZE * 8;
            
     
            // player 2 portrait
            secondPlayerPicRect.x = firstPlayerPicRect.x;
            secondPlayerPicRect.height = firstPlayerPicRect.height;
            secondPlayerPicRect.y = secondPlayerRect.y - secondPlayerPicRect.height;
            secondPlayerPicRect.width = firstPlayerPicRect.width;
           	
            // time display for first player
            p0time.x = G.Right(firstPlayerRect);
            p0time.y = firstPlayerRect.y;
            p0time.width = CELLSIZE * 3;
            p0time.height = CELLSIZE;
            // first player "i'm alive" animation ball
            p0anim.x = G.Right(p0time);
            p0anim.y = p0time.y;
            p0anim.width = p0time.height;
            p0anim.height = p0time.height;
            // time display for second player
            p1time.x = G.Right(secondPlayerRect);
            p1time.y = secondPlayerRect.y;
            p1time.width = p0time.width;
            p1time.height = p0time.height;
            p1anim.x = p1time.x+p1time.width;
            p1anim.y = p1time.y;
            p1anim.width = p1time.height;
            p1anim.height = p1time.height;


                  }  
        
        // "done" rectangle, should always be visible, but only active when a move is complete.

        doneRect.x = editRect.x;
        doneRect.y = G.Bottom(editRect)+CELLSIZE*2;
        doneRect.width = editRect.width;
        doneRect.height = editRect.height;

        commonChipRect.x = G.Right(boardRect);
        commonChipRect.y = G.Bottom(doneRect)+CELLSIZE*2;
        commonChipRect.width = CELLSIZE*20;
        commonChipRect.height = CELLSIZE*7;

        repRect.x = goalRect.x+CELLSIZE;
        repRect.y = goalRect.y-CELLSIZE;
        repRect.height = CELLSIZE;
        repRect.width = goalRect.width-CELLSIZE;
        

 
        //this sets up the "vcr cluster" of forward and back controls.
        SetupVcrRects(boardRect.x+CELLSIZE/2,G.Bottom(boardRect)-2*CELLSIZE,
            CELLSIZE * 10,
            5 * CELLSIZE);
 
        positionTheChat(chatRect,Color.white,Color.white);
        generalRefresh();
    }

	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, int forPlayer, Rectangle r,  HitPoint highlight)
    {	TicTacNineCell chips[]= b.rack;
        boolean canHit = b.LegalToHitChips(forPlayer);
        TicTacNineCell thisCell = chips[forPlayer];
        TicTacNineChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = null;
        thisCell.drawStack(gc,this,pt,r.width,r.x+r.width/2,r.y+r.height/2,0,0,msg);

        if((highlight!=null) && (highlight.hitObject==thisCell))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = r.width/2;
        }
     }

    private void drawSpareChipPool(Graphics gc,Rectangle r, HitPoint highlight)
    {	TicTacNineCell balls[] = b.rack;
    	int n = balls.length;
    	int cellsize = r.width/(n+1);
    	int initial_x = r.x+cellsize;
    	int initial_y = r.y + (r.height-cellsize)/2;
    	
    	for(int i=0;i<n;i++)
    	{	DrawCommonChipPool(gc,i,new Rectangle(initial_x,initial_y,cellsize,cellsize),highlight);
    		initial_x += cellsize;
    	}
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
    	TicTacNineChip ch = TicTacNineChip.getChip(obj);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);
     }


    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }


    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {boolean review = reviewMode() && !mutable_game_record;
      // erase
      TicTacNineBoard gb = disB(gc);
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     images[BOARD_INDEX].centerImage(gc, boardRect);
	    gb.SetDisplayParameters(0.748,1.0,  -0.1,0.086,  0, 0, 0,0);
	    gb.SetDisplayRectangle(boardRect);

      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, TicTacNineBoard gb, Rectangle brect, HitPoint highlight)
    {
     	boolean dolift = doLiftAnimation();
 
     	//
        // now draw the contents of the board and anything it is pointing at
        //
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
     	Enumeration<TicTacNineCell>cells = gb.getIterator(Itype.LRTB);
     	int top = G.Bottom(brect);
     	int left = G.Left(brect);
     	while(cells.hasMoreElements())
       	{	
     		TicTacNineCell cell = cells.nextElement();
            int ypos = top - gb.cellToY(cell);
            int xpos = left + gb.cellToX(cell);
            //StockArt.SmallO.drawChip(gc,this,CELLSIZE*2,xpos,ypos,null);
            if( cell.drawStack(gc,this,dolift ? null : highlight,SQUARESIZE,xpos,ypos,liftSteps,0.1,null)) 
            	{// draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
          		highlight.arrow =hasMovingObject(highlight) 
              			? StockArt.DownArrow 
              			: cell.height()>0?StockArt.UpArrow:null;
              		highlight.awidth = SQUARESIZE/2;
            		highlight.spriteColor = Color.red;
            	}
    	}

    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    { 
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  TicTacNineBoard gb = disB(gc);
       boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      TictacnineState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        DrawCommonChipPool(gc, FIRST_PLAYER_INDEX,firstPlayerChipRect, ot);
        DrawCommonChipPool(gc, SECOND_PLAYER_INDEX, secondPlayerChipRect,ot);
        drawSpareChipPool(gc,commonChipRect,ot);
        GC.setFont(gc,standardBoldFont());
		if (vstate != TictacnineState.PUZZLE_STATE)
        {
			handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
			
			handleEditButton(gc,editRect,select, highlight, HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc, (vstate==TictacnineState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);


        if (gc != null)
        {
            standardGameMessage(gc,
            		vstate==TictacnineState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
            				vstate!=TictacnineState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
            goalAndProgressMessage(gc,ourSelect,s.get("do what it takes to win"),progressRect, goalRect);
         }
        DrawRepRect(gc,b.Digest(),repRect);
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
        
        if(replay.animate) { playSounds(mm); }
 
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
        return (new TicTacNineMovespec(st, player));
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

//    public commonMove EditHistory(commonMove nmove)
//    {
//    	TicTacNineMovespec newmove = (TicTacNineMovespec) nmove;
//    	TicTacNineMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            TicTacNineMovespec m = (TicTacNineMovespec) History.elementAt(idx);
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
//                	{ TicTacNineMovespec m2 = (TicTacNineMovespec)History.elementAt(idx-1);
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
    
private void playSounds(commonMove m)
{

    // add the sound effects
    switch(m.op)
    {
    case MOVE_RACK_BOARD:
    case MOVE_BOARD_BOARD:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
   	break;
     case MOVE_PICK:
    	 playASoundClip(light_drop,100);
    	 break;
    case MOVE_PICKB:
    	playASoundClip(light_drop,100);
    	break;
    case MOVE_DROP:
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
        if (hp.hitCode instanceof TicId) // not dragging anything yet, so maybe start
        {

        TicId hitObject = (TicId)hp.hitCode;
		TicTacNineCell cell = hitCell(hp);
		TicTacNineChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case LiftRect:
 	    case Chip_Rack:
	    	PerformAndTransmit("Pick "+cell.row);
	    	break;
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
        if(!(id instanceof TicId)) { missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	TicId hitObject = (TicId)hp.hitCode;
        TictacnineState state = b.getState();
		TicTacNineCell cell = hitCell(hp);
		TicTacNineChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case LiftRect:
        	break;
         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
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
			
        case Chip_Rack:
         	{
        	int mov = b.movingObjectIndex();
             if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                	case PLAY_STATE:
            		PerformAndTransmit(RESET);
            		break;

               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop "+cell.row);
            		break;
            	}
			}
         	}
            break;
        }
        }
        repaint(20);
    }

    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parsable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() 
    { 
    	return(""+b.gametype+" "+b.randomKey); 
   }
    public String sgfGameType() { return(TicTacNine_SGF); }

    
    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(Tokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long rk = his.longToken();
    	// make the random key part of the standard initialization,
    	// even though games like checkers probably don't use it.
        b.doInit(token,rk);
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

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new TicTacNinerPlay()); }


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
            {	Tokenizer st = new Tokenizer(value);
            	String typ = st.nextToken();
            	long ran = st.longToken();
                b.doInit(typ,ran);
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

	public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit) {
		throw G.Error("Not needed with manual layout");
	}

}

