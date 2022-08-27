package kamisado;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;



/**
 * 
 * Change History
 *
 * May 2007 initial work in progress. 
 *
*/
public class KamisadoViewer extends CCanvas<KamisadoCell,KamisadoBoard> implements  KamisadoConstants
{
     /**
	 * 
	 */
    // file names for jpeg images and masks
	static final String Kamisado_SGF = "Kamisado" ; // sgf game name
	static final String ImageDir = "/kamisado/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"};
    static final int BOARD_INDEX = 0;
    static final String ImageNames[] = { "board2"};
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private Color CenterColor[] = {new Color(0.6f,0.7f,0.9f), new Color(0.2f,0.3f,0.6f) };
    private Color EdgeColor[] = { Color.black,Color.black} ;
    private Color chatColor = new Color(0.95f,0.95f,0.95f);

    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;
    // private state
    private KamisadoBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    private static int SUBCELL = 4;	// number of cells in a square
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //public Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //public Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle chatRect = addRect("chatRect"); // the chat window
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle firstPlayerChipRect = addRect("firstPlayerChipRect");
    private Rectangle secondPlayerChipRect = addRect("secondPlayerChipRect");
    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;
   
    private double lineStrokeWidth = 1;

    public synchronized void preloadImages()
    {	
       	KamisadoChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
    	}
        gameIcon = KamisadoChip.getChip(11).image;
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
    	enableAutoDone = true;
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new KamisadoBoard(info.getString(GAMETYPE, Kamisado_INIT),randomKey);
        //useDirectDrawing();	// not tested yet
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);

        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);			// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}

    }


    	
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
        int chatHeight = selectChatHeight(height);
        int sncols = (b.boardColumns*SUBCELL+(tallMode ? 6 : (wideMode ? 20 : 14))); // more cells wide to allow for the aux displays
        int snrows = (b.boardRows+1)*SUBCELL+(tallMode ? 10 : 0);  
        int cellw = width / sncols;
        int cellh = (height-(wideMode ? 0 : chatHeight)) / snrows;
        CELLSIZE = Math.max(1,Math.min(cellw, cellh)); //cell size appropriate for the aspect ration of the canvas
        if(wideMode && (chatHeight==0)) { CELLSIZE=0; }
        SQUARESIZE = CELLSIZE*SUBCELL;
        return(SQUARESIZE);
    }

    public void setLocalBoundsWT(int x, int y, int width, int height,boolean wideMode,boolean tallMode)
    {   
        int chatHeight = selectChatHeight(height);
        boolean noChat = (chatHeight==0);
        int vcells = height/CELLSIZE;
        boolean logBottom = tallMode & vcells>52;
        int ideal_logwidth = CELLSIZE * 8;
        int C2 = CELLSIZE/2;
        int logH = SQUARESIZE*2;
        G.SetRect(fullRect,x,y, width, height);

        // game log.  This is generally off to the right, and it's ok if it's not
        // completely visible in all configurations.
        
        int boardWidth = SQUARESIZE * b.boardColumns;
        G.SetRect(boardRect,x,wideMode ? CELLSIZE : chatHeight+CELLSIZE, boardWidth ,SQUARESIZE * b.boardRows);
        lineStrokeWidth = boardWidth/400.0;
        {
        int stateY = G.Top( boardRect);
        int stateH = CELLSIZE;
        int stateX = G.Left(boardRect);
        G.SetRect(noChatRect,G.Right(boardRect)-CELLSIZE*3-stateH,stateH,stateH,stateH);
        G.SetRect(stateRect, stateX,stateY,G.Left(noChatRect)-stateY, stateH);
        }
        G.SetRect(firstPlayerChipRect, 
        		tallMode ? CELLSIZE : G.Right(boardRect)-CELLSIZE, 
        		tallMode
        			? G.Bottom(boardRect)+CELLSIZE*3 
        			: wideMode ? SQUARESIZE : G.Top(boardRect)+SQUARESIZE*2,
        		SQUARESIZE,SQUARESIZE);
 
        G.AlignXY(secondPlayerChipRect,
        		tallMode 
        				? G.Left(firstPlayerChipRect)+CELLSIZE*16 
        				: G.Left(firstPlayerChipRect),
        		G.Top( firstPlayerChipRect)+(tallMode ? 0 : 3*SQUARESIZE),
        		firstPlayerChipRect);
        
        // "edit" rectangle, available in reviewers to switch to puzzle mode
        int doneW  = CELLSIZE*4;
        G.SetRect(doneRect,G.Right(boardRect)-doneW-CELLSIZE ,G.Bottom(boardRect)-SQUARESIZE/2 , doneW, 2*CELLSIZE);
 
        // "done" rectangle, should always be visible, but only active when a move is complete.
        G.SetRect(editRect,G.Left( doneRect), G.Bottom(doneRect)+C2,G.Width( doneRect),G.Height(doneRect));

        {
            commonPlayer pl0 =getPlayerOrTemp(0);
            commonPlayer pl1 = getPlayerOrTemp(1);
            Rectangle p0time = pl0.timeRect;
            Rectangle p1time = pl1.timeRect;
            Rectangle p0xtime = pl0.extraTimeRect;
            Rectangle p1xtime = pl1.extraTimeRect;
            Rectangle p0anim = pl0.animRect;
            Rectangle p1anim = pl1.animRect;
            Rectangle firstPlayerRect = pl0.nameRect;
            Rectangle secondPlayerRect = pl1.nameRect;
            Rectangle firstPlayerPicRect = pl0.picRect;
            Rectangle secondPlayerPicRect = pl1.picRect;
            
            //first player name
            G.SetRect(firstPlayerRect, 
            		G.Right(firstPlayerChipRect)+CELLSIZE,
            		G.Top( firstPlayerChipRect),
            		CELLSIZE *6, CELLSIZE*2);
            //second player name
            G.AlignXY(secondPlayerRect,
            		G.Right(secondPlayerChipRect)+CELLSIZE,
            		G.Top( secondPlayerChipRect),
            		firstPlayerRect);


            // first player portrait
            G.SetRect(firstPlayerPicRect,G.Left( firstPlayerRect),G.Bottom(firstPlayerRect),
            		CELLSIZE * 8,CELLSIZE * 8);
            
     
            // player 2 portrait
            G.AlignXY(secondPlayerPicRect,
            		G.Left( secondPlayerRect),G.Bottom(secondPlayerRect),firstPlayerPicRect);
           	
            // time display for first player
            G.SetRect(p0time, G.Right(firstPlayerRect)+C2,G.Top( firstPlayerRect),CELLSIZE * 3, CELLSIZE);
            // first player "i'm alive" animation ball
            G.SetRect(p0anim, G.Right(firstPlayerPicRect),G.Top( firstPlayerPicRect),CELLSIZE,CELLSIZE);
            // time display for second player
            G.SetRect(p1time, G.Right(secondPlayerRect),G.Top( secondPlayerRect),G.Width( p0time),G.Height(p0time));
            G.AlignXY(p1anim,G.Right( secondPlayerPicRect),G.Top(secondPlayerPicRect),p0anim);

            G.AlignLeft(p0xtime, G.Bottom(p0time),p0time);
            G.AlignLeft(p1xtime, G.Bottom(p1time),p0xtime);
 
            int chatX = wideMode ? G.Right(boardRect) : x;
            int chatY = wideMode ?(G.Bottom(secondPlayerPicRect)+CELLSIZE) : 0;
            G.SetRect(chatRect,
            		chatX,
            		chatY,
            		wideMode ? width-chatX-CELLSIZE : Math.min(G.Width(boardRect)-chatX,width-ideal_logwidth-CELLSIZE),
            		Math.min(chatHeight,height-chatY-CELLSIZE));

            int logX = wideMode ? G.Right(p0anim)+C2 : G.Right(chatRect)+C2;
            int logY = logBottom ? G.Bottom(firstPlayerPicRect)+C2 : CELLSIZE;
            G.SetRect(logRect,logX, 
            		logY,
            		Math.min(ideal_logwidth,width-logX),
            		logBottom ? height-logY-C2:wideMode||noChat ? logH : G.Height(chatRect));
            }
        
         
        //this sets up the "vcr cluster" of forward and back controls.
        SetupVcrRects(G.Left(boardRect)+CELLSIZE,G.Bottom(boardRect)-CELLSIZE*2,
            CELLSIZE * 8,
            4 * CELLSIZE);
 
        G.SetRect(reverseViewRect, G.Right(vcrZone), G.Top(vcrZone),CELLSIZE*2, CELLSIZE*4);
        
        G.SetRect(goalRect, G.Right(reverseViewRect), G.Bottom(boardRect),SQUARESIZE*4, CELLSIZE*2);
        
        setProgressRect(progressRect,goalRect);

        positionTheChat(chatRect,chatColor,chatColor);
        generalRefresh();
    }
    
	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	KamisadoChip king = KamisadoChip.getChip(b.reverseY()?1:0,KamisadoChip.FIRST_CHIP_INDEX);
    	KamisadoChip reverse = KamisadoChip.getChip(b.reverseY()?0:1,KamisadoChip.FIRST_CHIP_INDEX);
    	reverse.drawChip(gc,this,G.Width(r),G.centerX(r),G.Top(r)+G.Width(r)/2,null);
    	king.drawChip(gc,this,G.Width(r),G.centerX(r),G.Top(r)+G.Width(r)+G.Width(r)/2,null);
    	HitPoint.setHelpText(highlight,r, KamisadoId.ReverseViewButton,s.get(ReverseViewExplanation));
    }

	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	
        KamisadoChip thisChip = KamisadoChip.getChip(forPlayer,KamisadoChip.FIRST_CHIP_INDEX);
        thisChip.drawChip(gc,this, G.Width(r),G.centerX(r), G.centerY(r),null);
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
    	KamisadoChip ch = KamisadoChip.getChip(obj);// Tiles have zero offset
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
    {	
       textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
        drawFixedBoard(gc);
    }
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     images[BOARD_INDEX].centerImage(gc, brect);
	    b.SetDisplayParameters(0.83,0.90,  
	    		0.0,0.18, 
	    		0.0, 
	    		0.1, 0.05,0);
	    b.SetDisplayRectangle(brect);
       
 
      b.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

    int adjustedSquareSize(int startingSize,int ydistance,int height)
    {
    	return(startingSize-(int)(startingSize*ydistance*0.05/height));
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, KamisadoBoard gb, Rectangle brect, HitPoint highlight)
    {
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	Hashtable<KamisadoCell,KamisadoCell>dests = gb.getDests();
     	Hashtable<KamisadoCell,KamisadoCell>sources = gb.getSources();
     	KamisadoCell lastDest = b.lastDestStack.top();
     	KamisadoCell lastSource = b.lastSourceStack.top();
        KamisadoCell hitCell = null;
        KamisadoState state = gb.getState();
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.

        Enumeration<KamisadoCell> cells = gb.getIterator(Itype.LRTB);
        while( cells.hasMoreElements())
        {
            KamisadoCell cell = cells.nextElement();
            boolean isDest = dests.get(cell)!=null;
            boolean isSource = sources.get(cell)!=null;
            int ydistance = gb.cellToY(cell);
            int ypos = G.Bottom(brect) - ydistance;
            int xpos = G.Left(brect) + gb.cellToX(cell);
            int adjustedSize = adjustedSquareSize(SQUARESIZE,ydistance,G.Height(brect));
            boolean canHit = gb.legalToHitBoard();
            if( cell.drawStack(gc,this,(canHit && (isSource || isDest)) ? highlight : null,adjustedSize,xpos,ypos,0,0.1,null)) 
            	{ hitCell = cell; }
            
            if(state!=KamisadoState.PUZZLE_STATE)
            	{
            	int sz = Math.max(SQUARESIZE/15,3);
            	if(isSource)
        		{	int who = gb.whoseTurn;
            		GC.cacheAACircle(gc,xpos,ypos-SQUARESIZE/10,sz,CenterColor[who],EdgeColor[who],true);
        		}
        		else if(isDest)
        		{GC.cacheAACircle(gc,xpos,ypos,sz,Color.red,Color.yellow,true);
        		}}
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE/2,xpos,ypos,null);
    	}
        if((lastSource!=null) && (lastDest!=null))
        {
            int ypos0 = G.Bottom(brect) - gb.cellToY(lastSource.col,lastSource.row);;
            int xpos0 = G.Left(brect) + gb.cellToX(lastSource.col,lastSource.row);
            int ypos1 = G.Bottom(brect) - gb.cellToY(lastDest.col,lastDest.row);
            int xpos1 = G.Left(brect) + gb.cellToX(lastDest.col,lastDest.row);
            GC.setColor(gc,Color.gray);
            GC.drawArrow(gc,xpos0,ypos0,xpos1,ypos1,SQUARESIZE/8,lineStrokeWidth);
        }
        if(hitCell!=null)
        {	// draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
      		highlight.arrow =hasMovingObject(highlight) 
      			? StockArt.DownArrow 
      			: hitCell.height()>0?StockArt.UpArrow:null;
      		highlight.awidth = SQUARESIZE/3;
      		highlight.spriteColor = Color.red;
        	//G.frameRect(gc,Color.red,hitX-CELLSIZE,hitY-CELLSIZE-((hitCell.topChip()==null)?0:perspective_offset),CELLSIZE*2,CELLSIZE*2);
        }
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  //DrawLiftRect(gc,highlight);
       DrawReverseMarker(gc,reverseViewRect,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  KamisadoBoard gb = disB(gc);
       boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
       KamisadoState vstate = gb.getState();
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

        drawBoardElements(gc, gb, boardRect, ot);
        DrawCommonChipPool(gc, FIRST_PLAYER_INDEX,firstPlayerChipRect, gb.whoseTurn,ot);
        DrawCommonChipPool(gc, SECOND_PLAYER_INDEX, secondPlayerChipRect,gb.whoseTurn,ot);
        GC.setFont(gc,standardBoldFont());
		if (vstate != KamisadoState.PUZZLE_STATE)
        {
			if(!plannedSeating() && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==KamisadoState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);


 		String msg = s.get(vstate.getDescription());
    	if(vstate==KamisadoState.PLAY_STATE)
    	{	KColor co = gb.colorToMove();
    		if(co!=null)
    		{
    			String cstring = s.get(co.name());
    			msg = s.get(MoveMessage,cstring);
    		}
    	}
        standardGameMessage(gc,
        		messageRotation,
        		vstate==KamisadoState.GAMEOVER_STATE?gameOverMessage():msg,
        				vstate!=KamisadoState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);

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
        b.animationStack.clear();
        
        handleExecute(b,mm,replay);
        
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
        return (new KamisadoMovespec(st, player));
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
//    	CarnacMovespec newmove = (CarnacMovespec) nmove;
//    	CarnacMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            CarnacMovespec m = (CarnacMovespec) History.elementAt(idx);
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
//                	{ CarnacMovespec m2 = (CarnacMovespec)History.elementAt(idx-1);
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

    
private void playSounds(commonMove mm)
{
	KamisadoMovespec m = (KamisadoMovespec) mm;

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
        if (hp.hitCode instanceof KamisadoId) // not dragging anything yet, so maybe start
        {
       	KamisadoId hitObject = (KamisadoId)hp.hitCode;
		KamisadoCell cell = hitCell(hp);
		KamisadoChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case LiftRect:
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
        if(!(id instanceof KamisadoId)) {  missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
    	missedOneClick = false;
    	KamisadoId hitObject = (KamisadoId)hp.hitCode;
        KamisadoState state = b.getState();
		KamisadoCell cell = hitCell(hp);
		KamisadoChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
            	throw G.Error("Hit Unknown: %s", hitObject);
        case ReverseViewButton:
       	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
       	 generalRefresh();
       	 break;

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
			

        }
         }
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
    public String sgfGameType() { return(Kamisado_SGF); }

  
    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long rk = G.LongToken(his);
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

    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
    	if(target==reverseOption)
    	{
    	b.setReverseY(reverseOption.getState());
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
    public SimpleRobotProtocol newRobotPlayer() { return(new KamisadoPlay()); }


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
}

