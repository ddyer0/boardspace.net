package takojudo;

import bridge.*;
import com.codename1.ui.geom.Rectangle;

import lib.*;
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.Enumeration;
import java.util.StringTokenizer;
import static takojudo.TakojudoMovespec.*;
/**
 * 
 * Change History
 *
 * Oct 2012 Initial release.
*/
public class TakojudoViewer extends CCanvas<TakojudoCell,TakojudoBoard> implements TakojudoConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;// background textures
   
    // private state
    private TakojudoBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;
   
    private Rectangle acceptDrawRect = addRect("acceptDraw");
    private Rectangle declineDrawRect = addRect("declineDraw");
    private Rectangle logoRect = addRect("logoRect");
    private Rectangle repRect = addRect("repRect");
     

    public void preloadImages()
    {	
       	TakojudoChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
    	}
        gameIcon = images[LOGO_INDEX];
    }


	/**
	 * {@inheritDoc}
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * @parm info initializatino values for the class
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	// int players_in_game = Math.max(3,info.getInt(exHashtable.PLAYERS_IN_GAME,4));
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new TakojudoBoard(info.getString(OnlineConstants.GAMETYPE, Tacojudo_INIT),
        		randomKey,repeatedPositions,getStartingColorMap());
        useDirectDrawing(true); // not tested yet
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);

        
     }

    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);			// initialize the board
        if(!preserve_history)
    	{startFirstPlayer();
    	}

    }
    boolean horizontal = false;

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*4;
    	int doneW = plannedSeating() ? unitsize*6 : 0;
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW, chipW);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
       	G.SetRect(done, horizontal ? G.Right(box)+unitsize/4 : x, y+(horizontal ? unitsize/2 : chipW), doneW, doneW/2);
    	pl.displayRotation = rotation;
    	G.union(box, chip,done);
    	return(box);
    }
    
    public boolean usePerspective() { return(super.getAltChipset()==0); }
    
    public void setLocalBounds(int x, int y, int width, int height)
    {	
    	setLocalBoundsV(x,y,width,height,new double[] {1,-1});
    }
    public double setLocalBoundsA(int x,int y,int width,int height,double a)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	horizontal = a<0;
    	int nPlayers = nPlayers();
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*15;	
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	//
    			0.75,	// 60% of space allocated to the board
    			1.0,	// 1.0:1 aspect ratio for the board
    			fh*3,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        boolean rotate = seatingFaceToFaceRotated();
        int nrows = rotate ? b.boardColumns : b.boardRows;  
        int ncols = rotate ? b.boardRows : b.boardColumns;
       // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
        layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
        layout.placeRectangle(logoRect, fh*10,fh*10, BoxAlignment.Edge);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int stateH = fh*3;
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH*2)/nrows);
    	SQUARESIZE = (int)cs;
     	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*SQUARESIZE);
    	int boardH = (int)(nrows*SQUARESIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-stateH*2-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH+stateH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH, iconRect,stateRect,viewsetRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	if(rotate)
    	{
    		G.setRotation(boardRect, -Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        return boardW*boardH;
    }
	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	TakojudoChip king = TakojudoChip.getChip(b.reverseY()?1:0,TakojudoChip.TENTACLE_INDEX);
    	TakojudoChip reverse = TakojudoChip.getChip(b.reverseY()?0:1,TakojudoChip.TENTACLE_INDEX);
    	reverse.drawChip(gc,this,G.Width(r),G.centerX(r),G.Top(r)+G.Width(r)/2,null);
    	king.drawChip(gc,this,G.Width(r),G.centerX(r),G.Top(r)+G.Width(r)+G.Width(r)/2,null);
    	HitPoint.setHelpText(highlight,r, TacoId.ReverseViewButton,s.get(ReverseViewExplanation));
     }  
    private void DrawLogoMarker(Graphics gc,Rectangle r)
    {	
		if(gc!=null) 
		{images[LOGO_INDEX].centerImage(gc,r); 
		  GC.frameRect(gc,Color.black,r);
		}
    }
    private TakojudoChip playerChip(int forPlayer)
    {
    	return TakojudoChip.getChip(b.getColorMap()[forPlayer],TakojudoChip.HEAD_INDEX);
    }
	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, TakojudoBoard gb,int forPlayer, Rectangle r)
    {  
        TakojudoChip thisChip = playerChip(forPlayer);
        int width = G.Width(r);
        int h2 = width/2;
        int h4 = h2/2;
        int left = G.Left(r);
        int bottom = G.Bottom(r);
        thisChip.drawChip(gc,this,h2,left+h4,bottom-h4,null);
        GC.setFont(gc,largeBoldFont());
        GC.Text(gc,true,left,(int)(bottom-width*0.4),width,h4, Color.black, null, gb.pieceSummary(forPlayer));
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
    	TakojudoChip ch = TakojudoChip.getChip(obj);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);
     }



    /**{@inheritDoc}
     *  this is used by the game controller to supply entertainment strings to the lobby
     */
    public String gameProgressString()
    {	// this is what the standard method does
    	// return ((reviewer ? s.get(ReviewAcion) : ("" + viewMove)));
    	return(super.gameProgressString() + " " + b.pieceSummary(0)+ " "+b.pieceSummary(1));
    }



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
    {boolean review = reviewMode() && !mutable_game_record;
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      boolean perspective = usePerspective();
     images[perspective ? BOARD_INDEX : BOARD_NP_INDEX].centerImage(gc, brect);
      if(perspective)
      {
	    b.SetDisplayParameters(0.95, 0.9,  		0.0,0.0, 0.0,0.2,0.1,0.0);
      }
      else
      {
  	    b.SetDisplayParameters(1, 1,  		0.0,0.0, 0.0 );
    	  
      }
	    b.SetDisplayRectangle(brect);

      b.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, TakojudoBoard gb, Rectangle brect, HitPoint highlight)
    {
     	//
        // now draw the contents of the board and anything it is pointing at
        //
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
    	boolean reverse = gb.reverseXneqReverseY();
    	TakojudoChip picked = gb.pickedObject;
    	Enumeration<TakojudoCell> cells = gb.getIterator(Itype.LRTB);
    	int cx = G.centerX(brect);
    	int cy = G.centerY(brect);
        while (cells.hasMoreElements())
        {
        	TakojudoCell cell = cells.nextElement();
            TakojudoChip ch = cell.topChip();
            TakojudoCell draw = (reverse && !cell.dontDraw && (ch!=null) && ch.isHead())
            						? cell.exitTo(TakojudoBoard.CELL_UP_RIGHT()) 
            						: cell;
            int ypos = G.Bottom(brect) - gb.cellToY(draw);
            int xpos = G.Left(brect) + gb.cellToX(draw);
            // need to set location for the animation, even if not drawing
            cell.rotateCurrentCenter(contextRotation, xpos, ypos, cx, cy);
            if( !cell.dontDraw && ch!=null)
            {
            // only the head needs to be compensated in reverse view.
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            draw.drawStack(gc,null,xpos,ypos,this,0,SQUARESIZE,0.1,null);
            	
        	}
    	}
    	TakojudoCell hitCell = gb.closestCell(highlight,brect);
    	if((hitCell!=null) && reverse && (picked!=null) && picked.isHead())
			{	// compensate for the reversed view, where the head is still drawn from the visual lower-left
    			hitCell = hitCell.exitTo(TakojudoBoard.CELL_DOWN_LEFT());
			}
        if((hitCell!=null) && gb.LegalToHitBoard(hitCell))
        {	
        	TakojudoChip ch = hitCell.topChip();
        	if((ch!=null) && ch.isHead())
        			{	// change the head to the canonical head location
        			hitCell = gb.headLocation(ch);
        			}
            highlight.hit_y = G.Bottom(brect) - gb.cellToY(hitCell);
            highlight.hit_x = G.Left(brect) + gb.cellToX(hitCell);
            highlight.spriteColor = Color.red;
        	highlight.hitObject = hitCell;
        	highlight.hitCode = TacoId.BoardLocation;
      		highlight.arrow =hasMovingObject(highlight) 
      			? StockArt.DownArrow 
      			: hitCell.topChip()!=null?StockArt.UpArrow:null;
      		highlight.awidth = SQUARESIZE/2;
        }

    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  DrawLogoMarker(gc,logoRect);
       DrawReverseMarker(gc,reverseViewRect,highlight);
    }
     
     public int currentRepetitionCount(int forPlayer)
     {  	for(int lim = History.size()-1; lim>=0; lim--)
     	{	commonMove m = History.elementAt(lim);
     		if(m.op==MOVE_DECLINE_DRAW) { return(0); }
     		if((m.player==forPlayer) && (m.op==MOVE_BOARD_BOARD))
     		{int nreps = repeatedPositions.numberOfRepeatedPositions(m.digest);
     		switch(nreps)
     		{
     		case 1: return(nreps);
     		default:
     			return(nreps);
     		case 0:
     			break;
     		}}
     	}
     	return(0);
     }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  TakojudoBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
       TakojudoState vstate = gb.getState();
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
        drawBoardElements(gc, gb, boardRect, ot);
        GC.unsetRotatedContext(gc,highlight);
        
        boolean planned = plannedSeating();
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();
       
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
        {
        commonPlayer cpl = getPlayerOrTemp(i);
        cpl.setRotatedContext(gc, highlight, false);
        DrawCommonChipPool(gc, gb,i,chipRects[i]);
        if(planned && (gb.whoseTurn==i))
        {
			handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
       	
        }
        cpl.setRotatedContext(gc, highlight, true);
        }

        GC.setFont(gc,standardBoldFont());
		if (vstate != TakojudoState.PUZZLE_STATE)
        {
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}


		handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);

    	if((vstate == TakojudoState.QUERY_DRAW_STATE)
    				|| (vstate==TakojudoState.ACCEPT_DRAW_STATE)
    				|| (vstate==TakojudoState.DECLINE_DRAW_STATE))
    	{
    	if(GC.handleRoundButton(gc,messageRotation,acceptDrawRect,select,s.get(ACCEPTDRAW),
    			HighlightColor,rackBackGroundColor))
    	{ select.hitCode = TacoId.AcceptDraw;
    	}
    	if(GC.handleRoundButton(gc,messageRotation,declineDrawRect,select,s.get(DECLINEDRAW),
    			HighlightColor,rackBackGroundColor))
    	{ select.hitCode = TacoId.DeclineDraw;
    	}
    	}
    	else if(((vstate==TakojudoState.PLAY_STATE)&&(currentRepetitionCount(gb.whoseTurn)==2))||(vstate==TakojudoState.OFFER_DRAW_STATE)) 
    	{	if(GC.handleRoundButton(gc,messageRotation,acceptDrawRect,select,s.get(OFFERDRAW),
    			HighlightColor,(TakojudoState.OFFER_DRAW_STATE==vstate)?HighlightColor:rackBackGroundColor))
    			{ select.hitCode = TacoId.OfferDraw;
    			}
    	}

 		drawPlayerStuff(gc,(vstate==TakojudoState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);

 
        standardGameMessage(gc,messageRotation,
        		vstate==TakojudoState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=TakojudoState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        int w = G.Width(iconRect)/2;
        playerChip(gb.whoseTurn).drawChip(gc, this,w,G.centerX(iconRect),G.centerY(iconRect)+w/2,null);
        goalAndProgressMessage(gc,ourSelect,s.get("Immobilize your opponent"),progressRect, goalRect);
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        }

        drawAuxControls(gc,ourSelect);
        drawVcrGroup(ourSelect, gc);
        drawViewsetMarker(gc,viewsetRect,ourSelect);
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
        startBoardAnimations(replay,b.animationStack,b.cellSize(),MovementStyle.Simultaneous);
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
        return (new TakojudoMovespec(st, player));
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
//    	TakojudoMovespec newmove = (TakojudoMovespec) nmove;
//    	TakojudoMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            TakojudoMovespec m = (TakojudoMovespec) History.elementAt(idx);
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
//                	{ TakojudoMovespec m2 = (TakojudoMovespec)History.elementAt(idx-1);
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
        if (hp.hitCode instanceof TacoId)// not dragging anything yet, so maybe start
        {

        TacoId hitObject = (TacoId)hp.hitCode;
		TakojudoCell cell = hitCell(hp);
		TakojudoChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(cell.chip!=null)
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
        if(!(id instanceof TacoId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	TacoId hitObject = (TacoId)hp.hitCode;
        TakojudoState state = b.getState();
		TakojudoCell cell = hitCell(hp);
		TakojudoChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case OfferDraw:
        	PerformAndTransmit("Offer");
        	break;
        case DeclineDraw:
        	PerformAndTransmit("Decline");
        	break;
        case AcceptDraw:
        	PerformAndTransmit("Accept");
        	break;
        case ReverseViewButton:
       	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
       	 generalRefresh();
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
     * token, which ought to be parseable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() 
    { 
    	return(""+b.gametype+" "+b.randomKey); 
   }
    public String sgfGameType() { return(Takojudo_SGF); }

    
    
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
    public SimpleRobotProtocol newRobotPlayer()
    	{ return(new TakoJudoPlay(this)); }


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

