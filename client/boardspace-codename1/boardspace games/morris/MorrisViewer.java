package morris;

import bridge.*;
import com.codename1.ui.geom.Rectangle;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.StockArt;

import static morris.MorrisMovespec.*;

/**
 * This code shows the overall structure appropriate for a game view window.
*/
public class MorrisViewer extends CCanvas<MorrisCell,MorrisBoard> implements MorrisConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // private state
    private MorrisBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private JMenuItem offerDrawAction = null;
    
    private Rectangle repRect = addRect("repRect");
    private Rectangle declineDrawRect = addRect("declineDraw");
    private Rectangle acceptDrawRect = addRect("acceptDraw");	
    private Rectangle bannerRect = addRect("banner");			// the game type, positioned at the top

    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle poolRects[] = addRect("pool",2);
    
    /**
     * preload all the images associated with the game. This is delegated to the chip class.
     */
    public void preloadImages()
    {	
       	MorrisChip.preloadImages(loader,ImageDir);
       	gameIcon = MorrisChip.board_9.image;
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
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	InternationalStrings.put(MorrisStrings);
        	InternationalStrings.put(MorrisStringPairs);
        }

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new MorrisBoard(info.getString(OnlineConstants.GAMETYPE, Variation.Morris_9.name),randomKey,players_in_game,repeatedPositions,
        		getStartingColorMap());
        useDirectDrawing(true); // not tested yet
        doInit(false);
        offerDrawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);       
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        int np = b.nPlayers();
        b.doInit(b.gametype,b.randomKey,np);			// initialize the board
        if(!preserve_history)
    	{
    	startFirstPlayer();
    	}

    }

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*3;
    	int chipH = unitsize*2;
    	int doneW = unitsize*4;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	Rectangle done = doneRects[player];
    	Rectangle rack = poolRects[player];
    	
    	G.SetRect(chip, x, y, chipW, chipH);
    	G.SetRect(done, G.Right(box)+unitsize/2,y+unitsize/2,doneW,plannedSeating()?doneW/2:0);
    	G.SetRect(rack,x,G.Bottom(box),unitsize*8,unitsize*3);
    	
    	pl.displayRotation = rotation;
    	
    	G.union(box, chip,done,rack);
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
    			fh*2.5,	// minimum cell size
    			fh*3.5,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);

        layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);
        layout.placeRectangle(bannerRect,buttonW*3,buttonW/2,buttonW*5,5*buttonW/2,BoxAlignment.Top,false);
    	Rectangle main = layout.getMainRectangle();
        int stateH = fh*3;
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH/(ncols+1)));
    	int CELLSIZE = (int)cs;
    	
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(ncols*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-stateH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	SQUARESIZE = boardW/12;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH/4;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH/2,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);

    }
    

    //
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    // for checkers, this is just a single checker, which also displays the number of pieces 
    // left on the board as a sort of progress metric.
    //
    private void DrawCommonChipPool(Graphics gc, MorrisBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	MorrisCell chips[]= gb.rack;
        MorrisCell thisCell = chips[forPlayer];
        int sz = (int)(G.Height(r)*0.7);
        int xp = G.Left(r)+sz;
        int yp = G.Bottom(r)-2*sz/3;
        drawChipStack(gc,false,gb,sz,xp,yp,thisCell,forPlayer,highlight,null);
    }
    private void DrawSamplePool(Graphics gc, MorrisBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	MorrisCell chips[]= gb.sample;
        MorrisCell thisCell = chips[forPlayer];
        int sz = (int)(G.Width(r)*0.7);
        int xp = G.centerX(r);
        int yp = G.Top(r)+G.Height(r);
        thisCell.drawStack(gc,this,null,sz,xp,yp,0,0.14,0,""+gb.sample[player].height());
    }
    private void DrawCapturedPool(Graphics gc, MorrisBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	MorrisCell chips[]= gb.captured;
        MorrisCell thisCell = chips[forPlayer];
        int sz = (int)(G.Height(r)*0.7);
        int xp = G.Right(r)-sz-sz/2;
        int yp = G.Bottom(r)-2*sz/3;
        drawChipStack(gc,true,gb,sz,xp,yp,thisCell,forPlayer,highlight,""+thisCell.height());
    }
    
    private boolean drawChipStack(Graphics gc,boolean reverse,MorrisBoard gb,int sz,int x,int y,MorrisCell thisCell,int forPlayer,HitPoint highlight,String msg)
    {
        MorrisChip thisChip = thisCell.topChip();
        boolean canHit = gb.LegalToHitChips(thisCell,forPlayer);
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        boolean hit = thisCell.drawStack(gc,this,pt,sz,x,y,0,reverse?-0.14 : 0.14,0,msg);

        if(hit)
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = sz/2;
        	highlight.spriteColor = Color.red;
        }
        if(pt!=null && canDrop) 
    	{ // draw a visible marker for the drop zone
    	StockArt.SmallO.drawChip(gc, this, SQUARESIZE,x,y, "");
    	}
        return(hit);
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
     	MorrisChip ch = MorrisChip.getChipNumber(obj);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);

     }


    /* draw the deep unchangeable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	boolean reviewBackground = reviewMode()&&!mutable_game_record;
    	setBoardRect(b);
      // erase
     MorrisChip.backgroundTile.image.tileImage(gc, fullRect);   
      //gc.setColor(Color.black);
      //G.fillRect(gc, boardRect);
      if(reviewBackground)
      {	 
       MorrisChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      MorrisChip.board_9.drawChip(gc,this,boardRect,null);
      
      //G.centerImage(gc,MorrisChip.board_9.image, brect,this);
      Variation v = b.variation;
      if(v.banner!=null) {v.banner.image.centerImage(gc,bannerRect); }
      
      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, MorrisBoard gb, Rectangle brect, HitPoint highlight)
    {
     	
      	// targets are the pieces we can hit right now.
     	Hashtable<MorrisCell,MorrisMovespec>targets = gb.getTargets();
     	MorrisCell dest = gb.getDest();		// also the current dest and source
     	MorrisCell src = gb.getSource();
     	MorrisCell last = gb.getPrevDest();	// and the other player's last move
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
     	MorrisCell hitCell = null;
    	for(MorrisCell cell = gb.allCells; cell!=null; cell = cell.next)
    	{
        	{ 
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            HitPoint hitNow = gb.legalToHitBoard(cell,targets) ? highlight : null;
            if( cell.drawStack(gc,this,hitNow,SQUARESIZE,xpos,ypos,0,0.1,null)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	hitCell = cell;
            	}
            if((cell==dest)||(cell==src))
            {
            	StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            }
            if(cell==last)
            {
            	StockArt.Dot.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            }
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
        	}
    	}
    	if(hitCell!=null)
    	{
    		highlight.arrow =hasMovingObject(highlight)
      				? StockArt.DownArrow 
      				: hitCell.topChip()!=null?StockArt.UpArrow:null;
            highlight.awidth = SQUARESIZE/2;
            highlight.spriteColor = Color.red;
     	}
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  String var = b.variation.rules;
       HitPoint.setHelpText(highlight,bannerRect,s.get(var));
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  MorrisBoard gb = disB(gc);
       setBoardRect(gb);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      MorrisState vstate = gb.getState();
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        
        boolean planned = plannedSeating();
        
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
          {	commonPlayer pl = getPlayerOrTemp(i);
          	pl.setRotatedContext(gc, highlight, false);

            DrawCapturedPool(gc, gb,i,poolRects[i], gb.whoseTurn,ot);
            DrawCommonChipPool(gc, gb,i,poolRects[i], gb.whoseTurn,ot);
            DrawSamplePool(gc, gb,i,chipRects[i], gb.whoseTurn,null);        	
          	if(planned && (i==gb.whoseTurn))
          	{
          		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
      					HighlightColor, rackBackGroundColor);
          	}
          	pl.setRotatedContext(gc, highlight, true);
          }	

        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
        double messageRotation = pl.messageRotation();

        
        GC.setFont(gc,standardBoldFont());
        
        switch(vstate)
        {
        default:
        	if(gb.drawIsLikely())
        	{	// if not making progress, put the draw option on the UI
            	if(GC.handleSquareButton(gc,messageRotation,acceptDrawRect,select,s.get(OFFERDRAW),HighlightColor,rackBackGroundColor))
            	{
            		select.hitCode = GameId.HitOfferDrawButton;
            	}
       		
        	}
        	break;
        case AcceptOrDecline:
        case AcceptPending:
        case DeclinePending:
        	if(GC.handleSquareButton(gc,messageRotation,
        			acceptDrawRect,select,s.get(ACCEPTDRAW),HighlightColor,rackBackGroundColor))
        	{
        		select.hitCode = GameId.HitAcceptDrawButton;
        	}
        	if(GC.handleSquareButton(gc,messageRotation,
        			declineDrawRect,select,s.get(DECLINEDRAW),HighlightColor,rackBackGroundColor))
        	{
        		select.hitCode = GameId.HitDeclineDrawButton;
        	}
       	break;
        }
        
		if (vstate != MorrisState.Puzzle)
        {
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
                }

 		drawPlayerStuff(gc,(vstate==MorrisState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);


            standardGameMessage(gc,messageRotation,
            		vstate==MorrisState.Gameover
            			?gameOverMessage()
            			:s.get(vstate.getDescription()),
            				vstate!=MorrisState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
 		gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null,0.7);
            goalAndProgressMessage(gc,ourSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
         
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
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
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Chained);
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
        return (new MorrisMovespec(st, player));
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
//    	MorrisMovespec newmove = (MorrisMovespec) nmove;
//    	MorrisMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            MorrisMovespec m = (MorrisMovespec) History.elementAt(idx);
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
//                	{ MorrisMovespec m2 = (MorrisMovespec)History.elementAt(idx-1);
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
        if (hp.hitCode instanceof MorrisId) // not dragging anything yet, so maybe start
        {
        MorrisId hitObject = (MorrisId)hp.hitCode;
		MorrisCell cell = hitCell(hp);
		MorrisChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case Black_Chip_Pool:
	    	PerformAndTransmit("Pick B "+cell.row+" "+chip.id.shortName);
	    	break;
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick W "+cell.row+" "+chip.id.shortName);
	    	break;
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(cell.height()>0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.id.shortName);
	    		}
	    	break;
		default:
			break;
        }
        }}
    }

 	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging( HitPoint hp)
    {	CellId id = hp.hitCode;
    	if(!(id instanceof MorrisId)) 
    		{ // handle all the actions that aren't ours
    			missedOneClick = performStandardActions(hp,missedOneClick); 
    		}
    	else {
    	missedOneClick = false;
    	MorrisId hitObject = (MorrisId)id;
		MorrisState state = b.getState();
		MorrisCell cell = hitCell(hp);
		MorrisChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);


         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Confirm:
			case Play:
			case Capture:
			case Place:
			case Puzzle:
				if(b.movingObjectIndex()>=0)
				{ 
				  PerformAndTransmit("Dropb "+cell.col+" "+cell.row); 
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.id.shortName);
				}
				break;
			}
			break;
        case White_Captured:
        case Black_Captured:
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{
            	int mov = b.movingObjectIndex();
                if(mov>=0) 
                	{
                	PerformAndTransmit("Drop "+hitObject.shortName);
                	}
                else 
                {
                	PerformAndTransmit("Pick "+hitObject.shortName);
                }
        	}
        	break;
  
        }}
    }

    private void setBoardRect(MorrisBoard gb)
    {
	    gb.SetDisplayParameters(0.98,0.99,  0.02,-0.1,  0);
	    gb.SetDisplayRectangle(boardRect);
       
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
    	return(""+b.gametype+" "+b.randomKey+" "+b.nPlayers()); 
   }
    public String sgfGameType() { return(Morris_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long rk = G.LongToken(his);
    	int np = G.IntToken(his);
    	// make the random key part of the standard initialization,
    	// even though games like checkers probably don't use it.
        b.doInit(token,rk);
        adjustPlayers(np);

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
    	if(target==offerDrawAction)
    	{	if(OurMove() 
    			&& (b.movingObjectIndex()<=0)
    			&& (b.getState()==MorrisState.Play))
    		{
    		PerformAndTransmit(OFFERDRAW);
    		}
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
    public SimpleRobotProtocol newRobotPlayer() { return(new MorrisPlay()); }


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
}

