package modx;

import com.codename1.ui.geom.Rectangle;
import bridge.Color;

/* below here should be the same for codename1 and standard java */

import static modx.ModxMovespec.*;
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.Drawable;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.SimpleSprite;
import lib.StockArt;
import lib.SimpleSprite.Movement;

/**
 * This code shows the overall structure appropriate for a game view window.
*/
public class ModxViewer extends CCanvas<ModxCell,ModxBoard> implements ModxConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(155,155,155);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    public boolean usePerspective() { return(super.getAltChipset()==0); }
 
    // private state
    private ModxBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle[] playerChipRect = addRect("chip",4);
    private Rectangle[] playerFlatRect = addRect("flat",4);
    private Rectangle[] playerScoreRect = addRect("score",4);
    private Rectangle jokerRect = addRect("jokerRect");
     
    /**
     * preload all the images associated with the game. This is delegated to the chip class.
     */
    public synchronized void preloadImages()
    {	
       	ModxChip.preloadImages(loader,ImageDir);
       	gameIcon = ModxChip.Icon.image;
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
        	InternationalStrings.put(ModxStrings);
        	InternationalStrings.put(ModxStringPairs);
        }

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new ModxBoard(info.getString(GAMETYPE, Variation.Modx.name),
        		randomKey,players_in_game,repeatedPositions,getStartingColorMap(),ModxBoard.REVISION);
        useDirectDrawing(true);
        doInit(false);
        
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
    private boolean flatten = false;
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = playerChipRect[player];
    	Rectangle flat = playerFlatRect[player];
    	Rectangle score = playerScoreRect[player];
       	Rectangle done = doneRects[player];
    	int chipW = unitsize*3;
    	int chipH = unitsize*3;
    	int doneW = unitsize*4;
    	int scoreW = unitsize*2;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW*2+scoreW+unitsize/2,y,unitsize);
    	G.SetRect(chip, x, y, chipW, chipH);
    	G.SetRect(flat,x+chipW,y,chipW,chipH);
    	G.SetRect(score, x+chipW*2+unitsize/2, y,scoreW,scoreW);
    	if(flatten)
    	{
    	G.SetRect(done, G.Right(box)+unitsize/4,G.Top(box),doneW,plannedSeating()?doneW/2:0);    		
    	}
    	else
    	{
    	G.SetRect(done, x+unitsize/2,y+chipH+unitsize/2,doneW,plannedSeating()?doneW/2:0);
    	}
    	pl.displayRotation = rotation;
    	
    	G.union(box, chip,done,flat,score);
    	return(box);
    }
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
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        int ncols = b.boardColumns;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.65,	// 60% of space allocated to the board
    			1.0,	// 1:1 aspect ratio for the board
    			fh*2.5,	// minimum cell size
    			fh*3.5,	// maximum cell size
    			0.5		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
        layout.placeRectangle(jokerRect, G.Width(playerFlatRect[0]),G.Height(playerFlatRect[0]),BoxAlignment.Center);
   
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/ncols);
    	SQUARESIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*SQUARESIZE);
    	int boardH = (int)(ncols*SQUARESIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
        int stateH = SQUARESIZE/3;
        int boardY = mainY+extraH+stateH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH/2;
        int stateX = boardX;
        G.placeRow(stateX+stateH,stateY,boardW-stateH ,stateH,stateRect,liftRect,viewsetRect,noChatRect);
        G.SetRect(boardRect,boardX,boardY,boardW,boardH);
 
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH*2,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        return boardW*boardH;
    }

    
    private void drawChipStack(Graphics gc,ModxCell thisCell,boolean canHit,HitPoint highlight,Rectangle r)
    {
    	boolean canDrop = hasMovingObject(highlight);
    	ModxChip thisChip = thisCell.topChip();
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        boolean didHit = thisCell.drawStack(gc,this,pt,G.Width(r),G.Left(r)+G.Width(r)/2,G.Top(r)+G.Height(r)/2,0,0.02,0.0,""+thisCell.height());
        if(didHit)
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = G.Width(r)/2;
        	highlight.spriteColor = Color.red;
        }
    }
    //
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    // for checkers, this is just a single checker, which also displays the number of pieces 
    // left on the board as a sort of progress metric.
    //
    private void DrawCommonChipPool(Graphics gc, ModxBoard gb, int forPlayer, Rectangle r, Rectangle f, Rectangle score,int player, HitPoint highlight)
    {	
    	drawChipStack(gc,gb.rack[forPlayer],gb.legalToHitChips(gb.rack[forPlayer]),highlight,r);
    	drawChipStack(gc,gb.flat[forPlayer],gb.legalToHitChips(gb.flat[forPlayer]), highlight,f);
    	String pscore = ""+gb.score[forPlayer];
    	GC.setFont(gc, largeBoldFont());
    	GC.Text(gc, true, score, Color.black, null, pscore);
    	GC.frameRect(gc, Color.black, score);  	
    }
    private void DrawJokerPool(Graphics gc,ModxBoard gb,Rectangle r,HitPoint highlight)
    {	drawChipStack(gc,gb.joker,gb.legalToHitChips(gb.joker),highlight,r);
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
     	int obj1 = obj;
    	ModxChip ch = ModxChip.getChipNumber(obj1);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);

     }


   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, ModxBoard gb, Rectangle brect, HitPoint highlight)
    {
     	boolean dolift = doLiftAnimation();

     	// targets are the pieces we can hit right now.
     	Hashtable<ModxCell,ModxMovespec>targets = gb.getTargets();
     	ModxCell dest = gb.getDest();		// also the current dest and source
     	ModxCell src = gb.getSource();
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
    	Enumeration<ModxCell> cells = gb.getIterator(Itype.LRTB);
    	int top =  G.Bottom(brect);
    	int left = G.Left(brect);
    	while(cells.hasMoreElements())
    	{	ModxCell cell = cells.nextElement();
            int ypos = top - gb.cellToY(cell);
            int xpos = left + gb.cellToX(cell);
            HitPoint hitNow = !dolift && gb.legalToHitBoard(cell,targets) ? highlight : null;
            if( cell.drawStack(gc,this,hitNow,SQUARESIZE,xpos,ypos,liftSteps,0.02,null)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	hitNow.arrow =hasMovingObject(highlight) 
      				? StockArt.DownArrow 
      				: cell.topChip()!=null?StockArt.UpArrow:null;
            	hitNow.awidth = SQUARESIZE/2;
            	hitNow.spriteColor = Color.red;
            	}
            //StockArt.Dot.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            if((cell==dest)||(cell==src))
            {
            	StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            }
    	}
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  
       drawLiftRect(gc,liftRect,highlight,ModxChip.liftIcon.image);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    { ModxBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      ModxState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        
        boolean planned = plannedSeating();
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
          {	commonPlayer pl = getPlayerOrTemp(i);
          	pl.setRotatedContext(gc, highlight, false);
            DrawCommonChipPool(gc, gb,i,playerChipRect[i], playerFlatRect[i],playerScoreRect[i],gb.whoseTurn,ot);
          	if(planned && (i==gb.whoseTurn))
          	{
          		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
      					HighlightColor, rackBackGroundColor);
          	}
          	pl.setRotatedContext(gc, highlight, true);
          }	
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
        double messageRotation = pl.messageRotation();
        
        DrawJokerPool(gc,gb,jokerRect,ot);
        GC.setFont(gc,standardBoldFont());
       
        
		if (vstate != ModxState.Puzzle)
        {
			if(!planned && !autoDoneActive()) 
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
                }

 		drawPlayerStuff(gc,(vstate==ModxState.Puzzle),moving?null:highlight,HighlightColor,rackBackGroundColor);


        if (gc != null)
        {	
        	ModxChip chip = gb.getPlayerChip(gb.whoseTurn);
        	int h = G.Height(stateRect);
        	chip.drawChip(gc, this, h ,G.Left(stateRect)-h/2,G.centerY(stateRect),null);
        	
            standardGameMessage(gc,messageRotation,
            		vstate==ModxState.Gameover
            			?gameOverMessage()
            			:s.get(vstate.getDescription()),
            				vstate!=ModxState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        }
        goalAndProgressMessage(gc,highlight,Color.black,s.get(VictoryCondition),progressRect, goalRect);
   
        drawAuxControls(gc,ourSelect);
        drawViewsetMarker(gc,viewsetRect,ourSelect);
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
        startBoardAnimations(replay);
        if(replay!=replayMode.Replay) { playSounds(mm); }
 
        return (true);
    }
     /**
      * in general, the animation stack is a specialized communication between
      * the board and the viewer.  Executing moves conventionally pushes pairs
      * of source+destination on the stack.  These moves have already occurred,
      * so if the viewer was undisturbed, it would display the final state.
      * 
      * the invisible trick is that the cells that are the targets of animations
      * will reduce stack heights by the number of animations still in progress.
      * This magically makes them seem shorter until the animation completes.
      * 
      * the rest of exactly how the animations are timed and structured is
      * Idiosyncratic to a particular game.
      * 
      * @param replay
      */
     void startBoardAnimations(replayMode replay)
     {
        if(replay!=replayMode.Replay)
     	{	double now = 0;
     		while(b.animationStack.size()>1)
     		{
     		ModxCell to = b.animationStack.pop();		// source and destination for the move
     		ModxCell from = b.animationStack.pop();
     		double start = now;
     		Drawable glyph = to.topChip();
     		now = startAnimation(from,to,glyph,start,0);	// start it
     		if(b.animationStack.size()>0)
     			{	// if there are two pairs, the second pair is the captured chip
        		ModxCell rack = b.animationStack.pop();
         		ModxCell cap = new ModxCell(b.animationStack.pop());	// a fake cell
         		cap.addChip(rack.topChip());	// and put a chip on it
    			startAnimation(cap,cap,rack.topChip(),start,now);	// animate the captured stones standing in place
    			startAnimation(cap,rack,rack.topChip(),now,0);		// then move on
     			}
     		}
     	}
        	b.animationStack.clear();
     } 
     //
     // schedule an animation.  If end is 0 calculate an end time based on the
     // distance flown.
     //
     double startAnimation(ModxCell from,ModxCell to,Drawable top,double start,double end)
     {	if((from!=null) && (to!=null) && (top!=null))
     	{	int fromx = from.animationChipXPosition( 0);
     		int fromy = from.animationChipYPosition(0);
     		int tox = to.animationChipXPosition( 0);
     		int toy = to.animationChipYPosition(0);
      		if(G.debug())
     		{
     			G.Assert((fromx|fromy)!=0,"From Cell %s center is not set",from);
     			G.Assert((tox|toy)!=0,"To %s center is not set",to);
     		}
     		double speed = masterAnimationSpeed*0.5;
   		
    		// make time vary as a function of distance to partially equalize the runtim of
     		// animations for long verses short moves.
     		double dist = G.distance(fromx,fromy, tox,toy);
     		double full = G.distance(0,0,G.Width(boardRect),G.Height(boardRect));
     		double endtime = end>0 ? end : speed*Math.sqrt(dist/full);
     		double rot = to.activeAnimationRotation();
     		SimpleSprite newSprite = new SimpleSprite(true,top,
     				SQUARESIZE,	// use the same cell size as drawSprite would
     				start,endtime,
             		fromx,fromy,
             		tox,toy,rot);
     		newSprite.movement = Movement.SlowIn;
             to.addActiveAnimation(newSprite);
   			addSprite(newSprite);
   	     	return(endtime);
   			}
     	return(start);
     }
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new ModxMovespec(st, player));
    }
    
    
    
private void playSounds(commonMove m)
{

    // add the sound effects
    switch(m.op)
    {
    case MOVE_RACK_BOARD:
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
    	if (hp.hitCode instanceof ModxId) // not dragging anything yet, so maybe start
        {
        ModxId hitObject = (ModxId)hp.hitCode;
		ModxCell cell = b.getCell(hitCell(hp));
		ModxChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
        case Joker_Pool:
        case Red_Flat_Pool:
        case Black_Flat_Pool:
        case Yellow_Flat_Pool:
        case Orange_Flat_Pool:
        case Red_Chip_Pool:
        case Black_Chip_Pool:
        case Yellow_Chip_Pool:
        case Orange_Chip_Pool:
        	
	    	PerformAndTransmit("Pick "+chip.id.shortName);
	    	break;

	    case BoardLocation:
	        if(b.isDest(cell))
	        {
	    	PerformAndTransmit("Pickb "+cell.col+" "+cell.row);
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
    	if(!(id instanceof ModxId)) 
    		{ // handle all the actions that aren't ours
    			missedOneClick = performStandardActions(hp,missedOneClick); 
    		}
    	else {
    	missedOneClick = false;
        ModxId hitObject = (ModxId)id;
		ModxState state = b.getState();
		ModxCell cell = hitCell(hp);
		ModxChip chip = (cell==null) ? null : cell.topChip();
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
			case Puzzle:
			case ReplaceJoker:
			case PlaceInitialJoker:
				if(b.movingObjectIndex()>=0)
				{ 
				  PerformAndTransmit("Dropb "+cell.col+" "+cell.row); 
				}
				else if((state==ModxState.Puzzle) && (chip!=null))
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.id.shortName);
				}
				else 
				{ Hashtable<ModxCell,ModxMovespec>targets = b.getTargets();
				  // note that the b.getCell is needed because the cell
				  // may be a cell from a copy of the main board.
				  ModxMovespec m = targets.get(b.getCell(cell));
				  if(m!=null)
				  {
					  PerformAndTransmit(m.moveString());
				  }
				}
			}
			break;
        case Joker_Pool:
        	G.Assert(b.pickedObject==ModxChip.Joker,"should be a joker");
        	PerformAndTransmit("drop j 0");
        	break;
        case Red_Flat_Pool:
        case Black_Flat_Pool:
        case Yellow_Flat_Pool:
        case Orange_Flat_Pool:
        case Red_Chip_Pool:
        case Black_Chip_Pool:
        case Yellow_Chip_Pool:
        case Orange_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                case Play:
            		PerformAndTransmit(RESET);
            		break;

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
      boolean perspective = usePerspective();
      // erase
     ModxChip.backgroundTile.image.tileImage(gc, fullRect);   
      //gc.setColor(Color.black);
      //GC.fillRect(gc, boardRect);
      if(reviewBackground)
      {	 
       ModxChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
      
      (perspective ? ModxChip.board.image : ModxChip.board_np.image).centerImage(gc,boardRect);
      
      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    } 
    
    private void setBoardRect(ModxBoard gb)
    {
    	if(usePerspective())
    	{
	    gb.SetDisplayParameters(
    			new double[]{0.12,0.19},
    			new double[]{0.97,0.19},
    			new double[]{0.14,0.96},
    			new double[]{0.95,0.96}
    			);			// skew
    	}
    	else 
    	{
    	    gb.SetDisplayParameters(
        			new double[]{0.1,0.185},
        			new double[]{0.985,0.185},
        			new double[]{0.1,0.98},
        			new double[]{0.985,0.98});
    		
    	}
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
    public String sgfGameType() { return(Modx_SGF); }

    
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

    	return(super.handleDeferredEvent(target,command));
     }

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new ModxPlay()); }


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
}

