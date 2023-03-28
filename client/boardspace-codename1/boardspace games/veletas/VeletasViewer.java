package veletas;

import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.JCheckBoxMenuItem;
/* below here should be the same for codename1 and standard java */
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
import lib.TextButton;
import lib.Toggle;

import static veletas.VeletasMovespec.*;

/**
 * This code shows the overall structure appropriate for a game view window.
*/
public class VeletasViewer extends CCanvas<VeletasCell,VeletasBoard> implements VeletasConstants, GameLayoutClient, PlacementProvider
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
    private VeletasBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("chip",2);
    
    private Rectangle shooterChipRect = addRect("shooters");
    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;
 	private TextButton swapButton = addButton(SWAP,GameId.HitSwapButton,SwitchMessage,
			HighlightColor, rackBackGroundColor);

	private Toggle eyeRect = new Toggle(this,"eye",
				StockArt.NoEye,VeletasId.ToggleEye,NoeyeExplanation,
				StockArt.Eye,VeletasId.ToggleEye,EyeExplanation
				);

    private NumberMenu numberMenu = new NumberMenu(this,VeletasChip.shooter,VeletasId.ShowNumbers);
    
    /**
     * preload all the images associated with the game. This is delegated to the chip class.
     */
    public synchronized void preloadImages()
    {	
       	VeletasChip.preloadImages(loader,ImageDir);
       	gameIcon = VeletasChip.Icon.image;
    }


	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info, LFrameProtocol frame)
    {	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	// int players_in_game = Math.max(3,info.getInt(exHashtable.PLAYERS_IN_GAME,4));
    	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
    	enableAutoDone = true;
    	super.init(info, frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	InternationalStrings.put(VeletasStrings);
        	InternationalStrings.put(VeletasStringPairs);
        }

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new VeletasBoard(info.getString(GAMETYPE, Variation.Veletas_10.name),
        		randomKey,players_in_game,getStartingColorMap(),VeletasBoard.REVISION);
        doInit(false);
    	useDirectDrawing(true);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        
    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        int np = b.nPlayers();
        b.doInit(b.gametype,b.randomKey,np,b.revision);			// initialize the board
        if(!preserve_history)
    	{ 
            startFirstPlayer();
    	}

    }

	
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*4;
    	int chipH = unitsize*3;
    	int doneW = plannedSeating() ? unitsize*5 : 0;
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW, chipH);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	if(flatten)
    	{
    	G.SetRect(done,G.Right(box)+unitsize/3,y+unitsize/3,doneW,doneW/2);
    	}
    	else
    	{
    	G.SetRect(done, x, y+chipH+unitsize, doneW, doneW/2);
    	}
    	pl.displayRotation = rotation;
    	G.union(box, chip,done);
    	return(box);
    }
    
    
    private boolean flatten = false;
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	setLocalBoundsV(x,y,width,height,new double[] {1,-1});
    }

    public double setLocalBoundsA(int x, int y, int width, int height,double a)
    {	flatten = a<0;	
    	G.SetRect(fullRect, x, y, width, height);
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
				getGlobalZoom(),margin,
				0.75,	// % of space allocated to the board
				1.0,	// aspect ratio for the board
				fh*2.0,fh*2.5,	// minimum, maximum cell size
				0.4		// preference for the designated layout, if any
				);
		
	    // place the chat and log automatically, preferring to place
		// them together and not encroaching on the main rectangle.
		layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
				minLogW, minLogH, minLogW*3/2, minLogH*3/2);
	   	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,swapButton);
		layout.placeTheVcr(this,minLogW,minLogW*3/2);
	
	
		Rectangle main = layout.getMainRectangle();
		int mainX = G.Left(main);
		int mainY = G.Top(main);
		int mainW = G.Width(main);
		int mainH = G.Height(main);
		
		// calculate a suitable cell size for the board
		double cs = Math.min((double)mainW/(ncols+1),(double)mainH/nrows);
		SQUARESIZE = (int)cs;
		//G.print("cell "+cs0+" "+cs+" "+bestPercent);
		// center the board in the remaining space
		int boardW = (int)((ncols+1)*SQUARESIZE);
		int boardH = (int)(nrows*SQUARESIZE);
		int extraW = Math.max(0, (mainW-boardW)/2);
		int extraH = Math.max(0, (mainH-boardH)/2);
		int boardX = mainX+extraW;
		int boardY = mainY+extraH;
		int boardBottom = boardY+boardH;
		int boardRight = boardX+boardH;
	   	layout.returnFromMain(extraW,extraH);
		//
		// state and top ornaments snug to the top of the board.  Depending
		// on the rendering, it can occupy the same area or must be offset upwards
		//
	    int stateY = boardY;
	    int stateX = boardX;
	    int stateH = fh*3;
	    G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect, stateRect,annotationMenu,numberMenu,eyeRect,noChatRect);
        G.SetRect(boardRect,boardX,boardY,boardW,boardH);
        
		G.SetRect(shooterChipRect,boardRight,boardY+boardH/2-SQUARESIZE/2,SQUARESIZE,SQUARESIZE);
		// goal and bottom ornaments, depending on the rendering can share
		// the rectangle or can be offset downward.  Remember that the grid
		// can intrude too.
		G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
	    positionTheChat(chatRect,Color.white,rackBackGroundColor);
	    return boardW*boardH;
    }
    

    //
	// reverse view icon, made by combining the stones for two colors.
    //
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	VeletasChip king = VeletasChip.getChip(b.reverseY()?1:0);
    	VeletasChip reverse = VeletasChip.getChip(b.reverseY()?0:1);
    	reverse.drawChip(gc,this,G.Width(r),G.centerX(r),G.Top(r)+G.Height(r)/4,null);
    	king.drawChip(gc,this,G.Width(r),G.centerX(r),G.Top(r)+G.Height(r)-G.Height(r)/4,null);
    	HitPoint.setHelpText(highlight,r,VeletasId.ReverseViewButton,s.get(ReverseViewExplanation));

     }  

    //
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    // for checkers, this is just a single checker, which also displays the number of pieces 
    // left on the board as a sort of progress metric.
    //
    private void DrawCommonChipPool(Graphics gc, VeletasBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	VeletasCell chips[]= gb.rack;
    	VeletasCell thisCell = chips[forPlayer];
    	commonPlayer cp = getPlayerOrTemp(forPlayer);
    	int xp = G.centerX(r);
    	int yp = G.centerY(r);
    	cp.rotateCurrentCenter(thisCell, xp, yp);
    	boolean canHit = gb.LegalToHitChips(thisCell);
        VeletasChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = ""+gb.ownedShooters[forPlayer];
        int w = G.Width(r);
        thisCell.drawStack(gc,this,pt,w,xp,yp,0,0,msg);

        if((highlight!=null) && (highlight.hitObject==thisCell))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth =w/3;
        	highlight.spriteColor=Color.red;
        }
     }
    private void DrawShooterPool(Graphics gc, VeletasBoard gb, int forPlayer, Rectangle r, HitPoint highlight)
    {	
        VeletasCell thisCell = gb.shooters;
        boolean canHit = gb.LegalToHitChips(thisCell);
        VeletasChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = ""+thisCell.height();
        int w = G.Width(r);
        thisCell.drawStack(gc,this,pt,w,G.centerX(r),G.centerY(r),0,0.1,msg);

        if((highlight!=null) && (highlight.hitObject==thisCell))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = w/3;
        	highlight.spriteColor = Color.red;
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
    	int obj1 = obj%100;
    	VeletasChip ch = VeletasChip.getChipNumber(obj1);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);

     }


   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, VeletasBoard gb, Rectangle brect, HitPoint highlight)
    {
    	// this logic animates the expansion of stacks when the button is pushed.
     	boolean moving = gb.movingObjectIndex()>=0;
    	boolean show = eyeRect.isOnNow();
     	// targets are the pieces we can hit right now.
     	Hashtable<VeletasCell,VeletasMovespec>targets = gb.getTargets();
     	VeletasCell dest = gb.getDest();		// also the current dest and source
     	VeletasCell src = gb.getSource();
     	VeletasCell last = gb.getPrevDest();	// and the other player's last move
     	numberMenu.clearSequenceNumbers();

     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
     	Enumeration<VeletasCell>cells = gb.getIterator(Itype.LRTB);
     	while(cells.hasMoreElements())
       	{	
     		VeletasCell cell = cells.nextElement();
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            numberMenu.saveSequenceNumber(cell,xpos,ypos);
            HitPoint hitNow = gb.legalToHitBoard(cell,targets) ? highlight : null;
            if( cell.drawStack(gc,this,hitNow,SQUARESIZE,xpos,ypos,liftSteps,0.1,null)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	hitNow.arrow =moving 
      				? StockArt.DownArrow 
      				: cell.topChip()!=null?StockArt.UpArrow:null;
            	hitNow.awidth = SQUARESIZE/3;
            	hitNow.spriteColor = Color.red;
            	}
            if(((cell==dest)&&!moving)||(cell==src))
            {
            	StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            }
            if(cell==last)
            {
            	StockArt.Dot.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            }
            if((hitNow!=null)&&show)
            	{
            	StockArt.SmallO.drawChip(gc,this,SQUARESIZE/2,xpos,ypos,null);
            	}
        	}
     	numberMenu.drawSequenceNumbers(gc,SQUARESIZE*2/3,labelFont,labelColor);
    	}
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  	eyeRect.activateOnMouse = true;
    	eyeRect.draw(gc,highlight);
        DrawReverseMarker(gc,reverseViewRect,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    { VeletasBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      VeletasState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        
        boolean planned = plannedSeating();
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
        {
     	 commonPlayer cpl = getPlayerOrTemp(i);
     	 cpl.setRotatedContext(gc, highlight, false);
     	 DrawCommonChipPool(gc, gb,i,chipRects[i], gb.whoseTurn,ot);
     	 if(planned && (i==gb.whoseTurn))
     	 	{
     		 handleDoneButton(gc,doneRects[i],(gb.DoneState() ? ot : null), 
  					HighlightColor, rackBackGroundColor);
     	 	}
     	 cpl.setRotatedContext(gc, highlight, true);
        }

        DrawShooterPool(gc, gb, gb.whoseTurn, shooterChipRect,ot);

        GC.setFont(gc,standardBoldFont());
 
 		double messageRotation = pl.messageRotation();
       
		if (vstate != VeletasState.Puzzle)
        {
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
                }

 		drawPlayerStuff(gc,(vstate==VeletasState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);

 		switch(vstate)
 		{
 			case PlayOrSwap:
 			case PlaceOrSwap:
 			case ConfirmSwap:
 				swapButton.show(gc,messageRotation, select);
 					break;
 			default: break;
 		}
        if (gc != null)
        {	
            standardGameMessage(gc,messageRotation,
            		vstate==VeletasState.Gameover
            			?gameOverMessage()
            			:s.get(vstate.getDescription()),
            				vstate!=VeletasState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
            gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
        }
        goalAndProgressMessage(gc,ourSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
   
        drawAuxControls(gc,ourSelect);
        numberMenu.draw(gc,highlight);
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
    	numberMenu.recordSequenceNumber(b.moveNumber);
        handleExecute(b,mm,replay);
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Simultaneous);
		lastDropped = b.lastDroppedObject;	// this is for the image adjustment logic

        if(replay!=replayMode.Replay) { playSounds(mm); }
 
        return (true);
    }
   		
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st,int player)
    {
        return (new VeletasMovespec(st, player));
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
//    	VeletasMovespec newmove = (VeletasMovespec) nmove;
//    	VeletasMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            VeletasMovespec m = (VeletasMovespec) History.elementAt(idx);
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
//                	{ VeletasMovespec m2 = (VeletasMovespec)History.elementAt(idx-1);
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
    case MOVE_RACK_BOARD:
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
        if (hp.hitCode instanceof VeletasId) // not dragging anything yet, so maybe start
        {
        VeletasId hitObject = (VeletasId)hp.hitCode;
		VeletasCell cell = hitCell(hp);
		VeletasChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case Shooter_Chip_Pool:
	    	PerformAndTransmit("Pick S "+cell.row+" "+chip.id.shortName);
	    	break;

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
    	if(!(id instanceof VeletasId)) 
    		{ // handle all the actions that aren't ours
    			missedOneClick = performStandardActions(hp,missedOneClick); 
    		}
    	else {
    	missedOneClick = false;
        VeletasId hitObject = (VeletasId)id;
		VeletasState state = b.getState();
		VeletasCell cell = hitCell(hp);
		VeletasChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case ShowNumbers:
        	numberMenu.showMenu();
        	break;
        case ToggleEye:
        	eyeRect.toggle();
        	break;
        case ReverseViewButton:
       	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
       	 generalRefresh();
       	 break;

         case BoardLocation:	// we hit the board 

				if(b.movingObjectIndex()>=0)
				{ 
				  PerformAndTransmit("Dropb "+cell.col+" "+cell.row); 
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.id.shortName);
				}
				else { 
					switch(state)
					{
					default: throw G.Error("not expecting %s",state);
					case PlaceOrSwap:
					case PlaceShooters:
						PerformAndTransmit("add s "+cell.col+" "+cell.row);
						break;
					case Play:
					case PlaceSingleStone:
					case PlayStone:
						PerformAndTransmit("add "+b.getPlayerColor(b.whoseTurn).shortName+" "+cell.col+" "+cell.row);
					}
					
				}

			break;
        case Shooter_Chip_Pool:
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                case Play:
                case PlayStone:
                case PlaceShooters:
                case PlaceSingleStone:
               	case Puzzle:
            		PerformAndTransmit("Drop "+col);
            		break;
            	}
			}
         	}
            break;

        }}
    }
/**
 * draw the deep unchangeable objects, including those that might be rather expensive
 * to draw.  This background layer is used as a backdrop to the rest of the activity.
 * in our cease, we draw the board and the chips on it. 
 * @param gc
 */
    public void drawFixedElements(Graphics gc)
    {	
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      
      setBoardRect(b);	// set up the coordinate system for the board
      
      // erase
     VeletasChip.backgroundTile.image.tileImage(gc, fullRect);   
      //gc.setColor(Color.black);
      //GC.fillRect(gc, boardRect);
      if(reviewBackground)
      {	 
       VeletasChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);
      
      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    } 
    
    private void setBoardRect(VeletasBoard gb)
    {
	    gb.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
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
    	return(""+b.gametype+" "+b.randomKey+" "+b.nPlayers()+" "+b.revision); 
   }
    public String sgfGameType() { return(Veletas_SGF); }

    
    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long rk = G.LongToken(his);
    	int np = G.IntToken(his);
    	int rev = G.IntToken(his);
    	// make the random key part of the standard initialization,
    	// even though games like checkers probably don't use it.
        b.doInit(token,rk,np,rev);
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
    	if(numberMenu.selectMenu(target,this)) { return(true); }
    	else if(target==reverseOption)
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
    public SimpleRobotProtocol newRobotPlayer() { return(new VeletasPlay()); }


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
             	int np = st.hasMoreTokens() ? G.IntToken(st) : 2;
               	int rev = st.hasMoreTokens() ? G.IntToken(st) : 0; 
                b.doInit(typ,ran,np,rev);
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

	public int getLastPlacement(boolean empty) {
		return b.placementCount+(b.DoneState()?1:0);
	}
}

