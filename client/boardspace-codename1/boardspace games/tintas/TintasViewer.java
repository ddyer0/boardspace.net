package tintas;

import static tintas.Tintasmovespec.*;

import bridge.*;
import com.codename1.ui.geom.Rectangle;

import online.common.*;
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
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;


/**
 * 
 *  
*/
public class TintasViewer extends CCanvas<TintasCell,TintasBoard> implements TintasConstants, GameLayoutClient
{	static final long serialVersionUID = 1000;
     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(245,225,198);
    private Color rackBackGroundColor = new Color(204,172,143);
    private Color rackEmphasisColor = new Color(224,192,163);
    private Color boardBackgroundColor = new Color(220,165,155);
    

     
    // private state
    private TintasBoard bb = null; //the board from which we are displaying
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

    private Rectangle chipRects[] = addZoneRect("chip",2);
    private Toggle eyeRect = new Toggle(this,"eye",
			StockArt.NoEye,TintasId.EyeRect,NoeyeExplanation,
			StockArt.Eye,TintasId.EyeRect,EyeExplanation
			);
	private TextButton swapButton = addButton(SWAP,GameId.HitSwapButton,SwapDescription,
			HighlightColor, rackBackGroundColor);
    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	TintasChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	gameIcon = TintasChip.Icon.image;
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
      
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default

        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	InternationalStrings.put(TintasStrings);
        	InternationalStrings.put(TintasStringPairs);
        }
         
        
        String type = info.getString(GAMETYPE, TintasVariation.tintas.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new TintasBoard(type,players_in_game,randomKey,TintasBoard.REVISION);
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
    	int minLogW = fh*10;
    	int vcrW = fh*15;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        boolean perspective = usePerspective();
        int nrows = perspective ? 12 : 20;  
        int ncols = 20;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.70,	// % of space allocated to the board
    			1,	// 1:1 aspect ratio for the board
    			fh*2,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,3*buttonW/2,doneRect,editRect,swapButton);
    	layout.placeTheVcr(this,vcrW,vcrW*3/2);
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
        int stateH = fh*3;
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH)/nrows);
        CELLSIZE = (int)cs;
     	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(nrows*cs);
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
        int stateY = boardY-stateH/2;
        int stateX = boardX;

        G.placeRow(stateX,stateY,boardW ,stateH,stateRect,viewsetRect,eyeRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY-(perspective?(boardW-boardH)/2:0),
    			boardW,perspective ? boardW : boardH);

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        return boardW*boardH;
    }
 
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unit)
    {	commonPlayer pl0 = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()?unit*6:0;
    	Rectangle box = pl0.createRectangularPictureGroup(x, y, unit);
    	int chipW = unit*18;
    	int chipH = unit*8;
    	if(flatten)
    	{
    	   	 G.SetRect(done,x,G.Bottom(box)+unit/2,doneW,doneW/2);
    	   	 G.SetRect(chip,G.Right(box),y,chipW,chipH);
    	}
    	else
    	{
    	G.SetRect(chip, x, G.Bottom(box), chipW,chipH);
    	G.SetRect(done, G.Right(box)+unit/2, y+unit/2, doneW, doneW/2);
    	}
    	G.union(box, chip,done);
    	pl0.displayRotation = rotation;
        return(box);
    }

    private void drawAuxControls(Graphics gc,HitPoint highlight)
    {	eyeRect.activateOnMouse = true;
    	eyeRect.draw(gc,highlight);
    }

	// draw a box of spare chips. For tintas it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, commonPlayer pl, HitPoint highlight,TintasBoard gb)
    {	
    	TintasCell[] rack = gb.captures[pl.boardIndex];
    	int subs = rack.length;
    	int space = (int)(G.Width(r)/(subs+0.5));
    	int xpos = G.Left(r)+space-space/4;
        int sz = Math.min((int)(space*1.3), gb.cellSize());
    	int ypos = G.Bottom(r)-sz/3;
        boolean showCol = false;
        boolean shortStack = false;
        switch(gb.getState())
        {
        default: showCol = true;
        	break;
        case Play:
        case PlayOrSwap:
        case FirstPlay: shortStack = showCol = (gb.pickedObject==null); 
        }
        GC.fillRect(gc, rackBackGroundColor,r);
    	for(int i=0;i<subs;i++)
    	{
    		TintasCell c = rack[i];
            boolean can = gb.LegalToHitChips(c);
            HitPoint high = can?highlight:null;
    		if(showCol && (c==gb.capturedStack.top()))
    		{
    			GC.fillRect(gc, rackEmphasisColor,(int)(xpos-space*0.4),
    					shortStack ? G.Bottom(r)-space/2 : G.Top(r)+1,
    					(int)(space*0.9),
    					shortStack ? space/2-2 : G.Height(r)-2);
    		}
    		if(c.drawStack(gc,this,high,sz,xpos,ypos,0,.3,null))
    		{
    			highlight.awidth = CELLSIZE;
    			highlight.spriteColor = Color.red;
    		}
    		pl.rotateCurrentCenter(c, xpos,ypos);
    		xpos += space;
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
    	TintasChip.getChip(obj).drawChip(g,this,bb.cellSize(), xp, yp, null);
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
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
      
     TintasChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       TintasChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       

      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     (usePerspective() ? TintasChip.board : TintasChip.board_np ).getImage(loader).centerImage(gc,
    		  	boardRect);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      setDisplayParameters(bb,boardRect);
      bb.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);


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

    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, TintasBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
    	// this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
    	TintasCell pawnLocation = gb.pawnStack.top();
    	boolean perspective = usePerspective();
    	boolean showTargets = eyeRect.isOnNow();
        for(TintasCell cell = gb.allCells; cell!=null; cell=cell.next)
          { boolean isDest = gb.isDest(cell);
            boolean isSource = gb.isASource(cell); 
            boolean hitCell = gb.LegalToHitBoard(cell);
            int yp0 = gb.cellToY(cell);
         	int ypos = G.Bottom(brect) - yp0;
            int xpos = G.Left(brect) + gb.cellToX(cell);
            // yscale is an ad-hoc number to shrink the chips as they move
            // toward the back row.
            double yscale = perspective ? (0.99*yp0/G.Height(brect)-0.33)*CELLSIZE : 1.0;
            if (isSource || isDest || (hitCell && showTargets))
             { // checking for pointable position
           	 	StockArt.SmallO.drawChip(gc,this,(int)(gb.cellSize()*((perspective ? 0 : 1)+(isSource ? 2 : 3))),xpos,ypos,null);                
             }

            //StockArt.SmallO.drawChip(gc,this,gb.cellSize(),xpos,ypos,null);     
            int sz = (int)(gb.cellSize() - yscale);
            if(cell.drawStack(gc,this,hitCell
            						?highlight
            						:null,
            						sz,xpos,ypos,0,.25,null))
            	{
             	highlight.arrow = (cell==pawnLocation) ? StockArt.UpArrow : StockArt.DownArrow;
            	highlight.awidth = CELLSIZE;
            	}
            
            if((cell.col=='F') && (cell.row==1))
            	{
            	int xp = xpos+CELLSIZE*2;
            	int yp = ypos+CELLSIZE;
            	if(gb.pawnHome.drawStack(gc, this, gb.LegalToHitBoard(gb.pawnHome)||isSource||isDest ? highlight:null, sz, 
                		xp,yp,
            			0,0,null))
            	{
                    boolean empty = gb.pawnHome.isEmpty();
                    boolean picked = (gb.pickedObject!=null);
                    highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
                    highlight.awidth = CELLSIZE;           
            	}
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
    {  TintasBoard gb = disB(gc);
       TintasState state = gb.getState();
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
       
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, rackEmphasisColor);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect);
       
 
       
       boolean planned = plannedSeating();
       for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX;i++)
         {	commonPlayer pl = getPlayerOrTemp(i);
         	pl.setRotatedContext(gc, selectPos, false);
            DrawChipPool(gc, chipRects[i],pl, ourTurnSelect,gb);
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
        switch(state)
        {
        case Puzzle: break;
        case PlayOrSwap:
        case ConfirmSwap:
        	swapButton.show(gc, messageRotation,buttonSelect);
			//$FALL-THROUGH$
		default:       	
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
        		if(!planned) 
        			{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
    					HighlightColor, rackBackGroundColor);
        			}
    			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);
                    }
            }


		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==TintasState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        standardGameMessage(gc,messageRotation,
            		state==TintasState.Gameover?gameOverMessage():s.get(state.description()),
            				state!=TintasState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(TintasVictoryCondition),progressRect, goalRect);
            //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for tintas
        
        drawAuxControls(gc,selectPos);
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        drawViewsetMarker(gc,viewsetRect,selectPos);

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
     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for tintas, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
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
//     		TintasCell dest = bb.animationStack.pop();
//     		TintasCell src = bb.animationStack.pop();
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
	 case MOVE_PAWN:
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
        return (new Tintasmovespec(st, player));
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
        if (hp.hitCode instanceof TintasId)// not dragging anything yet, so maybe start
        {
        TintasId hitObject =  (TintasId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    case PawnHome:
	    case RackLocation:
	        TintasCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pick "+hitCell.rackLocation().shortName+" "+hitCell.col+" "+hitCell.row);
	    	break;
	    case BoardLocation:
	    	break;
        }
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
       	if(!(id instanceof TintasId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        TintasId hitCode = (TintasId)id;
		TintasState state = bb.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitCode);
         case EyeRect:	
        	eyeRect.toggle();
        	break;
        case PawnHome:
        case RackLocation:
        	{
            TintasCell hitObject = hitCell(hp);
        	if(bb.pickedObject==null)
			{
			PerformAndTransmit("Pick "+hitObject.rackLocation().shortName+" "+hitObject.col+" "+hitObject.row);
			}
			else 
			{
				PerformAndTransmit("Drop "+hitObject.rackLocation().shortName+" "+hitObject.col+" "+hitObject.row);		
			}
        	}
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Play:
			case FirstPlay:
			case ContinuePlay:
			case PlayOrSwap:
			case Confirm:
			case ConfirmFinal:
			case ConfirmSwap:
			case Puzzle:
			{
		        TintasCell hitObject = bb.getCell(hitCell(hp));

				if((bb.pickedObject==null) && (hitObject.topChip()!=null))
				{
				switch(state)
				{
				case Play:
				case PlayOrSwap:
				case FirstPlay:
				case ContinuePlay:
					if(hitObject==bb.pawnStack.top())
				{
				PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
						
					}
					else {
						PerformAndTransmit("dropb "+hitObject.col+" "+hitObject.row);
						
					}
					break;
				default:
					PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
					break;
				}
				}
				else 
				{
					PerformAndTransmit("Dropb "+hitObject.col+" "+hitObject.row);		
				}
				break;
			}}
			break;
			

        }
        }
    }
    public boolean usePerspective()
    {
        return(getAltChipset()==0);
    }
    private void setDisplayParameters(TintasBoard gb,Rectangle r)
    {
          // the numbers for the square-on display are slightly ad-hoc, but they look right
    	if(usePerspective())
    	{
      	gb.SetDisplayParameters(0.640, 0.89, 1.14, -0.16,27.5,
      			0.41,0.3,
      			0.07); // shrink a little and rotate 60 degrees

    	}
    	else
       	{
    		gb.SetDisplayParameters(0.97,0.965,0.35,-.46,30.5);
    	}
      	
      	gb.SetDisplayRectangle(r);
    }
    
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     * <p>
     * if complete is true, we definitely want to start from scratch, otherwise
     * only the known changed elements need to be painted.  Exactly what this means
     * is game specific, but for tintas the underlying empty board is cached as a deep
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
    public String sgfGameType() 
    { return(TintasBoard.REVISION<=100 ? "11" : Tintas_SGF); }	// this is the official SGF number assigned to the game

   
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
    {  return(new TintasPlay());
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
            else if (name.equals(game_property) && value.equals("11"))
            {	// "11" grandfathers in some early game id mistakes.  Unfortunately it overlaps with Hex
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

