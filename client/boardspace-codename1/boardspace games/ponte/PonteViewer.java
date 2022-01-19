package ponte;

import bridge.*;
import com.codename1.ui.geom.Rectangle;

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
import lib.TextButton;

import static ponte.PonteMovespec.*;

/**
 * This code shows the overall structure appropriate for a game view window.
*/
public class PonteViewer extends CCanvas<PonteCell,PonteBoard> implements PonteConstants, GameLayoutClient
{
	static String VictoryCondition = "bridge islands of exactly 4 squares to score points";
    static final String Ponte_SGF = "PonteDelDiavolo"; // sgf game number allocated for ponte

    // file names for jpeg images and masks
    static final String ImageDir = "/ponte/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "ponte-icon-nomask"};
    static final int BOARD_INDEX = 0;
    static final String ImageNames[] = {"board"};
    
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(165,165,155);
    private Color boardBackgroundColor = new Color(165,165,155);
    
 
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;
    // private state
    private PonteBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] =  addRect(",chip",2);
	private TextButton swapButton = addButton(SWAP,GameId.HitSwapButton,SwapDescription,
			HighlightColor, rackBackGroundColor);
    private Rectangle scoreRects[] =addRect(",score",2);
    private Rectangle bridgeRect = addRect("bridgeRect");
    private Rectangle playerChip[] = addRect(",chip",2);

    public void preloadImages()
    {	
       	PonteChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
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
    	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new PonteBoard(info.getString(OnlineConstants.GAMETYPE, Ponte_INIT),randomKey,
        		players_in_game,getStartingColorMap());
        doInit(false);

        
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
    	Rectangle score = scoreRects[player];
    	Rectangle chip = playerChip[player];
    	int doneW = unitsize*4;
    	int scoreW = 3*unitsize/2;
    	Rectangle box = pl.createRectangularPictureGroup(x+scoreW,y,unitsize);
    	Rectangle done = doneRects[player];
    	
    	//G.SetRect(chip, x, y, chipW, chipH);
    	G.SetRect(score, x,y,scoreW,scoreW);
    	G.SetRect(chip, x, y+scoreW, scoreW, scoreW);
    	G.SetRect(done, G.Right(box)+unitsize/4,y+unitsize,doneW,plannedSeating()?doneW/2:0);
    	
    	pl.displayRotation = rotation;
    	
    	G.union(box,done,score,chip);
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
    	int minLogW = fh*18;	
    	int vcrW = fh*16;
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
    			0.75,	// 60% of space allocated to the board
    			1.1,	// 1:1 aspect ratio for the board
    			fh*2.5,	// maximum cell size
    			fh*4.0,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,swapButton);
    	layout.placeTheVcr(this,vcrW,vcrW*3/2);
        

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/(ncols+3),(double)mainH/ncols);
    	int CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(ncols*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW-SQUARESIZE*2)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	int boardRight = boardX+boardW;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        int stateH = CELLSIZE/2;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	int chipW = SQUARESIZE;
    	G.SetRect(chipRects[0],boardRight,boardY+SQUARESIZE*2,chipW,chipW);
    	G.SetRect(chipRects[1],boardRight,boardBottom-SQUARESIZE*2,chipW,chipW);
    	G.SetRect(bridgeRect,boardRight+SQUARESIZE,boardY+boardH/2-SQUARESIZE,chipW,chipW);
 /*   	
        G.SetRect(bridgeRect, 
        		G.Left( firstPlayerChipRect)+(tallMode ?SQUARESIZE*5 : CELLSIZE*4)-SQUARESIZE,
        		tallMode ? G.Top(firstPlayerChipRect)+SQUARESIZE : G.centerY( boardRect)-CELLSIZE*2,
        		SQUARESIZE*2,SQUARESIZE);
*/
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
      
    }

	// draw the unused components
    private void DrawCommonChipPool(Graphics gc, PonteBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight,double vstep)
    {	
        PonteCell thisCell = gb.getPlayerChips(forPlayer);
        PonteChip thisChip = thisCell.topChip();
        boolean canHit = gb.LegalToHitChips(forPlayer<=1?forPlayer:gb.whoseTurn,thisCell);
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = null;
        int step = G.Height(r);
        boolean isRect = G.pointInRect(pt, r);
        if(thisCell.drawStack(gc,this,pt,step,G.Right(r)-step/2,G.centerY(r),0,vstep,msg)
        	|| isRect)
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = step/2;
        	if(isRect) 
        		{ highlight.spriteRect = r;
        		  highlight.hitObject = thisCell;
        		  highlight.hitCode = thisCell.rackLocation();
        		  }
        	highlight.spriteColor = Color.red;
        }
     }

	// draw the unused components
    private void DrawPlayerScore(Graphics gc, PonteBoard gb, int forPlayer, Rectangle r)
    {	GC.fillRect(gc,boardBackgroundColor,r);
    	GC.frameRect(gc,Color.black,r);
    	int score = gb.scoreForPlayer(forPlayer);
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,true,r,Color.black,null,""+score);
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
    	PonteChip ch = PonteChip.getPiece(obj);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);
     }



  /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {
      // erase
      GC.setColor(gc,!animating && reviewMode() ? reviewModeBackground : boardBackgroundColor);
      //G.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(!animating && reviewMode())
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     images[BOARD_INDEX].centerImage(gc, boardRect);

  	// and is pretty expensive, so we shouldn't do it in the mouse-only case
      b.SetDisplayRectangle(boardRect);
  	  b.SetDisplayParameters(0.844,1.0, 0.25,0.3,0);
      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, PonteBoard gb, Rectangle brect, HitPoint highlight)
    {
    	Hashtable<PonteCell,PonteMovespec>dests =  gb.getDests();
    	Hashtable<PonteCell,PonteMovespec>alternateDests = gb.getAlternateBridgeEnds();
    	boolean showDots = dests.size()< ((gb.pickedObject==null) ? 4 : (gb.boardColumns*gb.boardRows)-4);
      	gb.blobsValid = false;
     	gb.findBlobs();

     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	PonteCell hitCell = null;
        for(int layer = 0;layer<=1;layer++)
        {
        	Enumeration<PonteCell> cells = gb.getIterator(Itype.LRTB);
            while (cells.hasMoreElements())
            {
            PonteCell cell = cells.nextElement();
            // because we draw layer-by-layer instead of using drawstack, we have
            // to include activeAnimationHeight so pieces don't appear until they land.
            PonteChip chip = ((cell.height()-cell.activeAnimationHeight())>layer) ? cell.chipAtIndex(layer) : null;  
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            HitPoint hit = ((highlight!=null) && gb.legalToHitBoard(cell,dests)) ? highlight : null;
            if( cell.drawChip(gc,this,chip,hit,SQUARESIZE,xpos,ypos,null)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	hitCell = cell;
             	}
          	//StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            //if(cell.underBridge)
            //	{
            //	StockArt.SmallX.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            //	}
            if(!animating)
            {
            if(layer==0)
            {
            	if(alternateDests.get(cell)!=null)
            	{
            	if(StockArt.SmallO.drawChip(gc,this,highlight,PonteId.BridgeEnd,SQUARESIZE,xpos,ypos,null))
            	{
            		hitCell = cell;
            	highlight.hitObject = cell;
            	
            	}
            	}
            	else 
            	{	if( showDots && dests.get(cell)!=null)
            		{
            		StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            		}
            		if(chip!=null && ((cell==gb.prev_tile_1)
            						|| (cell==gb.prev_tile_2)
            						|| (cell==gb.getDest())
            						|| (cell==gb.getPrevDest())))
            				{ StockArt.SmallX.drawChip(gc,this,2*SQUARESIZE/3,xpos,ypos,null); 
            				}
            	}
            }}
        	}
    	}
        if(hitCell!=null)
        {
        	highlight.arrow =hasMovingObject(highlight)
      				? StockArt.DownArrow 
      				: hitCell.topChip()!=null?StockArt.UpArrow:null;
        	
        	if(highlight.hitCode==PonteId.BridgeEnd)
        	{
        		highlight.arrow = StockArt.Rotate_CW;
        	}
        	highlight.awidth = SQUARESIZE/2;
            highlight.spriteColor = Color.red;
         }
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  //DrawLiftRect(gc,highlight);
       //DrawReverseMarker(gc,reverseViewRect,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  PonteBoard gb = disB(gc);

	if(gc!=null)
	{
	// note this gets called in the game loop as well as in the display loop
	// and is pretty expensive, so we shouldn't do it in the mouse-only case
  	gb.SetDisplayRectangle(boardRect);
   	gb.SetDisplayParameters(0.844,1.0, 0.25,0.3,0);
	}
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      PonteState vstate = gb.getState();
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        boolean planned = plannedSeating();
        
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
        {	commonPlayer pl = getPlayerOrTemp(i);
        	DrawCommonChipPool(gc, gb,i,chipRects[i], gb.whoseTurn,ot,0.05);
        	pl.setRotatedContext(gc, highlight, false);
            DrawPlayerScore(gc,gb,i,scoreRects[i]);
            gb.getPlayerChip(i).drawChip(gc,this,playerChip[i],null);
        	if(planned && (i==gb.whoseTurn))
        	{
        		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
    					HighlightColor, rackBackGroundColor);
        	}
        	pl.setRotatedContext(gc, highlight, true);
        }	

 
        DrawCommonChipPool(gc, gb, Bridge_Index, bridgeRect,gb.whoseTurn,ot,0.1);
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
        double messageRotation = pl.messageRotation();
        
        GC.setFont(gc,standardBoldFont());
		if (vstate != PonteState.Puzzle)
        {
			if(!planned) 
				{handleDoneButton(gc,messageRotation, doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
        }
		if((vstate==PonteState.PlayOrSwap)||(vstate==PonteState.ConfirmSwap))
		{
			swapButton.show(gc,messageRotation, select);
		}

 		drawPlayerStuff(gc,(vstate==PonteState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);



        standardGameMessage(gc,messageRotation,
        		vstate==PonteState.Gameover
        			?gameOverMessage()
        			:s.get(vstate.getDescription()),
        				vstate!=PonteState.Puzzle,
        				gb.whoseTurn,
        				stateRect);
        gb.getPlayerChip(gb.whoseTurn).drawChip(gc,this,iconRect,null); 
        goalAndProgressMessage(gc,ourSelect,s.get(VictoryCondition),progressRect, goalRect);
         
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
        return (new PonteMovespec(st, player));
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
public commonMove EditHistory(commonMove m)
{
	return(EditHistory(m,m.op==PonteMovespec.MOVE_BRIDGE_END));
}
    
 
private void playSounds(commonMove m)
{

    // add the sound effects
    switch(m.op)
    {

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
    	if (hp.hitCode instanceof PonteId)// not dragging anything yet, so maybe start        	
        {
        PonteId hitObject = (PonteId)hp.hitCode;
		PonteCell cell = hitCell(hp);
		PonteChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {case BridgeEnd: break;
	     case Bridge_30:
	     case Bridge_150:
	     case Bridge_45:
         case Bridge_60:
         case Bridge_90:
         case Bridge_120: 
         case Bridge_135:
         case Bridge_180:       	 
         case Red_Chip_Pool:
         case White_Chip_Pool:
	    	PerformAndTransmit("Pick "+hitObject.shortName);
	    	break;
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	{
	    	if(cell.chipIndex>0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
	    		}
	    	break;
	    	}
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
       	if(!(id instanceof PonteId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;		
        PonteId hitObject = (PonteId)id;
		PonteCell cell = hitCell(hp);
		PonteChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case BridgeEnd:
	    	PerformAndTransmit("BridgeEnd "+cell.col+" "+cell.row);
	    	break;
         case BoardLocation:	// we hit the board 
         	{	PonteChip picked = b.pickedObject;
				if(picked!=null)
				{ 	if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
				}
				break;
         	}
         case Bridge_30:
         case Bridge_150:
         case Bridge_45:
         case Bridge_60:
         case Bridge_90:
         case Bridge_120: 
         case Bridge_135:
         case Bridge_180:       	 
         case White_Chip_Pool:
         case Red_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
            	{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+col);
            	}
         	}
            break;

        }}
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
    public String sgfGameType() { return(Ponte_SGF); }

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


/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically
 *  */
    
    //   public void ViewerRun(int wait)
    //   {
    //       super.ViewerRun(wait);
    //   }

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new PontePlay()); }


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

