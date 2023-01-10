package barca;

import static barca.Barcamovespec.*;

import java.awt.*;

import online.common.*;
import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Toggle;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;
// TODO think about "consistent" and "3 player" variants
// TODO annotations with perspective are placed a little oddly
public class BarcaViewer extends CCanvas<BarcaCell,BarcaBoard> implements BarcaConstants, GameLayoutClient,PlacementProvider
{	 	
    static final String Barca_SGF = "Barca"; // sgf game number allocated for barca

    // file names for jpeg images and masks
    static final String ImageDir = "/barca/images/";

     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(235,245,255);
    private Color rackBackGroundColor = new Color(235,245,255);
    private Color boardBackgroundColor = new Color(220,220,255);
    private Color BlackArrowColor = new Color(230,200,255);;
    
    // adjust the cell size for annotations.
    public int cellSize() { return (bb.cellSize()/(usePerspective() ? 2 : 1)); }
    
    public boolean usePerspective() { return(super.getAltChipset()==0);}
    
    public int getAltChipset()
    {	int alt = super.getAltChipset();
    	int n = seatingFaceToFace() ? alt : alt*5;
    	if(bb.playerColor[0]!=0)
    	{
    	switch(n)
    	{	
    	default:
    	case 0:	break;
    	case 1: n=3; break;
    	case 2: n=4; break;
    	}
    	}
    	return(n);
    }
    // private state
    private BarcaBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private int SQUARESIZE;
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addZoneRect("chip",2);
    private NumberMenu numberMenu = new NumberMenu(this,BarcaChip.White_Mouse,BarcaId.ShowNumbers) ;

    private Toggle eyeRect = new Toggle(this,"eye",
			StockArt.NoEye,BarcaId.ToggleEye,NoeyeExplanation,
			StockArt.Eye,BarcaId.ToggleEye,EyeExplanation
			);
    private Rectangle reverseRect = addRect("reverse");
    private Rectangle repRect = addRect("repRect");	

    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	BarcaChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	gameIcon = BarcaChip.White_Elephant_Icon.image;
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
        	BarcaConstants.putStrings();
        }   
        
        String type = info.getString(GAMETYPE, BarcaVariation.barca.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new BarcaBoard(type,players_in_game,randomKey,getStartingColorMap(),BarcaBoard.REVISION);
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
        boolean perspective = usePerspective();
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,margin,
    			0.75,	// % of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*2,	// minimum cell size
    			fh*4,	// max cell size
    			0.3		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*3/2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*2);
    	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect, editRect, repRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int nrows = 15;  
        int ncols = 15;
        boolean rotate = seatingFaceToFaceRotated();
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/(nrows+1));
    	CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE*2;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(nrows*cs);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateH = 3*CELLSIZE/4;
        int stateY = boardY-stateH/2;
        int stateX = boardX;

        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,reverseRect,annotationMenu,numberMenu,viewsetRect,eyeRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
       	
    	if(perspective)
    		{
    		// this is an ad-hoc retrofit to the original layout.  We stretch
    		// the board rectangle to put the perspective board in the center
    		int w = G.Width(boardRect);
    		int h = G.Height(boardRect);
    		int cx = G.centerX(boardRect);
    		int cy = G.centerY(boardRect);
    		double scl = 0.59;
    		G.SetRect(boardRect,(int)(cx-w*scl-w*0.045), (int)(cy-h*scl), (int)(w*scl*2),(int)(h*scl*2));
    		}
    	if(rotate)
    	{	G.setRotation(boardRect, -Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-2*stateH/3,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
 	
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int ux4 = unitsize*4;
    	int ux2 = unitsize*2;
    	int doneW = plannedSeating() ? ux4 : 0;
    	Rectangle box = pl.createRectangularPictureGroup(x+ux4,y,unitsize);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x,y, ux4 ,ux2);
    	G.SetRect(done, x,y+ux2, doneW, doneW/2);
    	pl.displayRotation = rotation;
    	G.union(box, chip, done);
    	return(box);
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
    	BarcaChip.getChip(obj).drawChip(g,this,(int)(SQUARESIZE*sizeScale(xp,yp,boardRect)), xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  

    public void drawCommonChipPool(Graphics gc,int pl,Rectangle r)
    {	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	int size = G.Height(r)*3/4;
    	BarcaChip.mice[bb.playerColor[pl]].drawRotatedChip(gc,this,0,size,1,cx+size/2,cy,null);
    	BarcaChip.lions[bb.playerColor[pl]].drawRotatedChip(gc,this,0,size,1,cx,cy+size/8,null);
    	BarcaChip.elephants[bb.playerColor[pl]].drawRotatedChip(gc,this,0,size,1.0,cx-size/2,cy+size/4,null);    	
    }
    /** draw the deep unchangeable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { // erase
       BarcaChip.backgroundTile.image.tileImage(gc, fullRect);   
        drawFixedBoard(gc);
    }
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      if(reviewBackground)
      {	 
       BarcaChip.backgroundReviewTile.image.tileImage(gc,brect);   
      }
      boolean perspective = usePerspective();
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     (perspective ? BarcaChip.board.image : BarcaChip.board_np.image).centerImage(gc, brect);
      

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
	  setDisplayParameters(bb,brect);	// make sure the coordinate system is in sync
      bb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

     }
    
    // this is an ad-hoc scale factor to shrink the pieces as they move to the top of the board
    private double sizeScale(int cx,int cy,Rectangle r)
    {	if(usePerspective() && G.pointInRect(cx, cy,r))
    		{         	
    		double cy2 = G.Bottom(r)-cy;
            double scale = 1-cy2/G.Height(r)*0.9+0.5;
            return(scale);
    		}
    	return(1.0);
    }
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, BarcaBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  

        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        boolean picked = gb.pickedObject!=null;
        int whoseTurn = gb.whoseTurn;
        Hashtable<BarcaCell,commonMove>targets = gb.getTargets();
        Enumeration<BarcaCell>cells = gb.getIterator(Itype.LRTB);
        boolean perspective = usePerspective();
        boolean showMoves = eyeRect.isOnNow();
        numberMenu.clearSequenceNumbers();
    	while(cells.hasMoreElements())
    	{
            BarcaCell cell = cells.nextElement();
            BarcaChip top = cell.topChip();
            boolean canHit = gb.LegalToHitBoard(cell,targets);
            boolean frightened = (!picked 
            						&& (top!=null) 
            						&& (top.colorIndex()==bb.playerColor[whoseTurn]) 
            						&& (gb.isAfraid(cell,top.id)));
            boolean drawhighlight = (showMoves||picked) && (canHit
   				|| gb.isDest(cell) 		// is legal for a "drop" operation
   				|| gb.isSource(cell)
            		);	// is legal for a "pick" operation+
            int cy = gb.cellToY(cell,perspective);
            int cx = gb.cellToX(cell,perspective);
         	int ypos = G.Bottom(brect) - cy;
            int xpos = G.Left(brect) + cx;
            double scale = sizeScale(xpos,ypos,brect);
            numberMenu.saveSequenceNumber(cell,xpos,ypos,cell.lastEmptiedPlayer==0 ? labelColor : BlackArrowColor);
            int sz = (int)(SQUARESIZE*scale);
            //StockArt.SmallO.drawChip(gc,this,gb.cellSize(),xpos,ypos,null);                
            if(cell.drawChip(gc,this,canHit ? highlight : null,sz,xpos,ypos,null))
            {
            	highlight.awidth = sz*2/3;
            	highlight.spriteColor = Color.red;
            }
            if(frightened)
            { StockArt.Dot.drawChip(gc,this,sz+2,xpos+sz/15,ypos,null); 
            }
            if (drawhighlight)
            { // checking for pointable position
           	 StockArt.SmallO.drawChip(gc,this,sz,xpos,ypos,null);                
            }
           //if(cell.isOdd()){ StockArt.SmallO.drawChip(gc,this,(int)(gb.cellSize()*1),xpos,ypos,null); }                
            }
        numberMenu.drawSequenceNumbers(gc,SQUARESIZE,labelFont,labelColor);

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
    {  BarcaBoard gb = disB(gc);
       BarcaState state = gb.getState();
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
       
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
       
       GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect);
       GC.unsetRotatedContext(gc,selectPos);
       GC.setFont(gc,standardBoldFont());
       boolean planned = plannedSeating();
       double messageRotation = pl.messageRotation();
              
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
       {
       commonPlayer cpl = getPlayerOrTemp(i);
       cpl.setRotatedContext(gc, selectPos, false);
       drawCommonChipPool(gc,i,chipRects[i]);
       if(planned && (gb.whoseTurn==i))
       {
    	   handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
       }
       cpl.setRotatedContext(gc, selectPos,true);
       }
       if (state != BarcaState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive()) 
				{ handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
						HighlightColor, rackBackGroundColor); 
				}
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==BarcaState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        standardGameMessage(gc,messageRotation,
            		state==BarcaState.Gameover?gameOverMessage():s.get(state.description()),
            				state!=BarcaState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        BarcaChip.mice[gb.playerColor[gb.whoseTurn]].drawChip(gc,this,iconRect,null);
        
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(BarcaVictoryCondition),progressRect, goalRect);
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        drawAuxControls(gc,selectPos);
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
        startBoardAnimations(replay,bb.animationStack,CELLSIZE*2,MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay!=replayMode.Replay) { playSounds(mm); }
       return (true);
    }
     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for barca, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
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
//     		BarcaCell dest = bb.animationStack.pop();
//     		BarcaCell src = bb.animationStack.pop();
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

 private void playSounds(commonMove mm)
 {
	 switch(mm.op)
	 {
	 case MOVE_FROM_TO:
		 playASoundClip(light_drop,50);
		//$FALL-THROUGH$
	case MOVE_DROPB:
		 playASoundClip(heavy_drop,100);
		 break;
	 case MOVE_PICKB:
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
        return (new Barcamovespec(st, player));
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
        if (hp.hitCode instanceof BarcaId)// not dragging anything yet, so maybe start
        {
        BarcaId hitObject =  (BarcaId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    case BoardLocation:
	        BarcaCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
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
       	if(!(id instanceof BarcaId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        boolean missed = missedOneClick;
        missedOneClick = false;
        BarcaId hitCode = (BarcaId)id;
        switch (hitCode)
        {
        default:
        	if (performStandardActions(hp,missed)) {}
            else
            {
            	throw G.Error("Hit Unknown %s", hitCode);
            }
        	break;
        case ShowNumbers:
        	numberMenu.showMenu();
        	break;
        case ToggleEye:
        	eyeRect.toggle();
        	break;
        case Reverse:
        	bb.setReverseY(!bb.reverseY());
          	generalRefresh("reverse");
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
            BarcaCell hitObject = hitCell(hp);
			PerformAndTransmit((bb.pickedObject==null)?"Pickb ":"Dropb "+hitObject.col+" "+hitObject.row);

			break;
			
 
        }
        }
    }



    private void setDisplayParameters(BarcaBoard gb,Rectangle r)
    {	if(usePerspective())
    	{
      	double LL[] = {0.2,0.43};
       	double LR[] = {0.87,0.29};
       	double UL[] = {0.46,0.77};
       	double UR[] = {0.99,0.72};
       	gb.SetDisplayParameters( LL,LR,UL,UR);
       	int[][]coords = {
       			{ 'J', 10, 912, 684 },
       			{ 'J', 9, 900, 650 },
       			{ 'J', 8, 892, 618 },
       			{ 'J', 7, 882, 579 },
       			{ 'J', 6, 870, 541 },
       			{ 'J', 5, 859, 499 },
       			{ 'J', 4, 849, 452 },
       			{ 'J', 3, 830, 405 },
       			{ 'J', 2, 819, 349 },
       			{ 'J', 1, 805, 292 },
       			
       			{ 'I', 10, 848, 688 },
       			{ 'I', 9, 840, 657 },
       			{ 'I', 8, 828, 625 },
       			{ 'I', 7, 814, 591 },
       			{ 'I', 6, 800, 550 },
       			{ 'I', 5, 791, 511 },
       			{ 'I', 4, 774, 465 },
       			{ 'I', 3, 760, 420 },
       			{ 'I', 2, 743, 366 },
       			{ 'I', 1, 730, 311 },
       			
       			{ 'H', 10, 790, 694 },
       			{ 'H', 9, 778, 664 },
       			{ 'H', 8, 767, 632 },
       			{ 'H', 7, 754, 599 },
       			{ 'H', 6, 740, 561 },
       			{ 'H', 5, 722, 519 },
       			{ 'H', 4, 708, 477 },
       			{ 'H', 3, 690, 431 },
       			{ 'H', 2, 668, 380 },
       			{ 'H', 1, 648, 331 },
       			
       			{ 'G', 10, 740, 700 },
       			{ 'G', 9, 723, 671 },
       			{ 'G', 8, 708, 641 },
       			{ 'G', 7, 691, 605 },
       			{ 'G', 6, 680, 569 },
       			{ 'G', 5, 661, 532 },
       			{ 'G', 4, 643, 487 },
       			{ 'G', 3, 627, 444 },
       			{ 'G', 2, 600, 394 },
       			{ 'G', 1, 581, 345 },
       			
       			{ 'F', 10, 685, 705 },
       			{ 'F', 9, 670, 677 },
       			{ 'F', 8, 655, 648 },
       			{ 'F', 7, 638, 615 },
       			{ 'F', 6, 621, 579 },
       			{ 'F', 5, 599, 539 },
       			{ 'F', 4, 580, 498 },
       			{ 'F', 3, 555, 455 },
       			{ 'F', 2, 532, 411 },
       			{ 'F', 1, 512, 360 },
       			
       			
       			{ 'E', 10, 635, 711 },
       			{ 'E', 9, 618, 684 },
       			{ 'E', 8, 601, 653 },
       			{ 'E', 7, 580, 621 },
       			{ 'E', 6, 560, 588 },
       			{ 'E', 5, 540, 549 },
       			{ 'E', 4, 518, 512 },
       			{ 'E', 3, 490, 471 },
       			{ 'E', 2, 467, 421 },
       			{ 'E', 1, 445, 372 },
       			
       			{ 'D', 10, 582, 717 },
       			{ 'D', 9, 564, 690 },
       			{ 'D', 8, 548, 659 },
       			{ 'D', 7, 527, 627 },
       			{ 'D', 6, 507, 596 },
       			{ 'D', 5, 484, 562 },
       			{ 'D', 4, 454, 520 },
       			{ 'D', 3, 434, 480 },
       			{ 'D', 2, 403, 435 },
       			{ 'D', 1, 380, 387 },
       			
       			{ 'C', 10, 530, 722 },
       			{ 'C', 9, 515, 697 },
       			{ 'C', 8, 495, 668 },
       			{ 'C', 7, 472, 635 },
       			{ 'C', 6, 450, 603 },
       			{ 'C', 5, 426, 569 },
       			{ 'C', 4, 402, 529 },
       			{ 'C', 3, 372, 492 },
       			{ 'C', 2, 348, 443 },
       			{ 'C', 1, 319, 401 },
       			
       			{ 'B', 10, 487, 730 },
       			{ 'B', 9, 465, 701 },
       			{ 'B', 8, 446, 673 },
       			{ 'B', 7, 424, 643 },
       			{ 'B', 6, 400, 610 },
       			{ 'B', 5, 374, 576 },
       			{ 'B', 4, 350, 540 },
       			{ 'B', 3, 317, 500 },
       			{ 'B', 2, 291, 460 },
       			{ 'B', 1, 260, 415 },
       			
       			{ 'A', 10, 442, 735 },
       			{ 'A', 9, 417, 709 },
       			{ 'A', 8, 394, 680 },
       			{ 'A', 7, 375, 650 },
       			{ 'A', 6, 347, 618 },
       			{ 'A', 5, 319, 585 },
       			{ 'A', 4, 291, 551 },
       			{ 'A', 3, 265, 515 },
       			{ 'A', 2, 230, 472 },
       			{ 'A', 1, 200, 430 },
       		};
       	// the barca board is sufficiently distorted that the standard
       	// scaling algorithms didn't cut it. So every cell is located.
		for(int loc[] : coords)
		{
			BarcaCell c = gb.getCell((char)loc[0],loc[1]);
			c.xloc = loc[2]/1000.0;
			c.yloc = loc[3]/1000.0;
		}
    	}
    	else 
    	{
    	gb.SetDisplayParameters(0.91,0.98,
    			0.03,0.15,
    			0);
    	}
    	gb.SetDisplayRectangle(r);
    }
  
    //
	// reverse view icon, made by combining the stones for two colors.
    //
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	BarcaChip king = bb.reverseY() ? BarcaChip.White_Elephant : BarcaChip.Black_Elephant;
    	BarcaChip reverse = !bb.reverseY() ? BarcaChip.White_Elephant : BarcaChip.Black_Elephant;
    	int w = G.Width(r);
    	int ww = usePerspective() ? (int)(w*0.8) : (int)(w*1.2);
    	int t = G.centerY(r);
    	int l = G.centerX(r);
    	reverse.drawChip(gc,this,ww,l,t-w/4,null);
    	king.drawChip(gc,this,ww,l,t+w/3,null);
    	HitPoint.setHelpText(highlight,r, BarcaId.Reverse,s.get(ReverseViewExplanation));
    	
     }
   private void drawAuxControls(Graphics gc,HitPoint highlight)
   {  	eyeRect.activateOnMouse=true;
    	eyeRect.draw(gc,highlight);
    	DrawReverseMarker(gc,reverseRect,highlight);
    	numberMenu.draw(gc,highlight);
   }
    
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     * <p>
     * if complete is true, we definitely want to start from scratch, otherwise
     * only the known changed elements need to be painted.  Exactly what this means
     * is game specific, but for barca the underlying empty board is cached as a deep
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
    public String sgfGameType() { return(Barca_SGF); }	// this is the official SGF number assigned to the game


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

    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new BarcaPlay());
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

    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
        if(!handled)
        {
        	if(numberMenu.selectMenu(target,this)) {  handled = true;}
        }
        return (handled);
    }

	public int getLastPlacement(boolean empty) {
		return bb.moveNumber+(bb.DoneState()?1:0);
	}
}

