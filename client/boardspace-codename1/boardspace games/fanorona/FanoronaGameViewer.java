package fanorona;

import bridge.*;

import com.codename1.ui.geom.Rectangle;

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
import lib.Toggle;


/**
 * 
 * Change History
 *
 * May 2007 initial work in progress. 
 *
 * todo: add the ReverseView feature
 * 
*/
public class FanoronaGameViewer extends CCanvas<FanoronaCell,FanoronaBoard> implements FanoronaConstants, GameLayoutClient
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
    static final String Fanoronar_SGF = "Fanorona"; // sgf game name

    
    // file names for jpeg images and masks
    static final String ImageDir = "/fanorona/images/";
	// sounds
    static final int BOARD_INDEX = 0;
    static final String[] ImageFileNames =  {  "simple-board"  };
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"};
    private boolean testing=false;
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] images = null; // images of black and white gobblets and board
    private static Image[] textures = null;// background textures
    
    // private state
    private FanoronaBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private double boardRotation = 0;
    private Rectangle []chipRects = addRect("chip",2);
    private Rectangle declineDrawRect = addRect("declineDraw");
    private Rectangle acceptDrawRect = addRect("acceptDraw");	
    private Toggle eyeRect = new Toggle(this,"eye",
			StockArt.NoEye,FanId.ToggleEye,NoeyeExplanation,
			StockArt.Eye,FanId.ToggleEye,EyeExplanation
			);
   
    private Rectangle repRect = addRect("repRect");
    private Rectangle reverseRect = addRect("reverse");
    private JMenuItem offerDrawAction = null;

    public synchronized void preloadImages()
    {	FanoronaChip.preloadImages(loader,ImageDir);
    	if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        images = loader.load_images(ImageDir, ImageFileNames, 
        		loader.load_images(ImageDir, ImageFileNames,"-mask")); // load the main images
        textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = images[BOARD_INDEX];
    }


	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);

         
        b = new FanoronaBoard(info.getString(GAMETYPE, "Fanorona"),getStartingColorMap());
        offerDrawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);     
        useDirectDrawing(true);
        doInit(false);
     }

    public boolean handleDeferredEvent(Object target,String cmd)
    {
    	if(target==offerDrawAction)
			{	if(OurMove() 
					&& (b.movingObjectIndex()<=0)
					&& ((b.getState()==FanoronaState.PLAY_STATE) || (b.getState()==FanoronaState.DrawPending)))
				{
				PerformAndTransmit(OFFERDRAW);
				}
				return(true);
			}
    	return super.handleDeferredEvent(target, cmd);
    }
    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype);						// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
    }

    public void setLocalBounds(int x, int y, int width, int height)
    {	
    	setLocalBoundsV(x,y,width,height,aspects);
    }
    private double aspects[] = {1.6,1/1.6};
    public double setLocalBoundsA(int x,int y,int width,int height,double aspect)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int vcrW = fh*15;
    	int minLogW = fh*15;	
       	int minChatW = fh*35;	
       	int margin = fh/2;
        int minLogH = fh*10;	
        int ncols = b.ncols;
        int nrows = b.nrows;

        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			aspect,		// aspect ratio for the board
    			(int)(fh*1.5),// maximum cell size
    			0.25		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
        		
    	CELLSIZE = (int)(fh*2);
    	int C2 = CELLSIZE/2;
    	int buttonW = CELLSIZE*5;
    	layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);	
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
       	layout.placeTheVcr(this,vcrW,vcrW*3/2);
               
    	Rectangle main = layout.getMainRectangle();
        int stateH = CELLSIZE+C2;
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main)-stateH*2;
    	
    	int boardW;
    	int boardH;
    	if(mainW>=mainH)
    	{
        // calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	SQUARESIZE = (int)cs;
    	boardW = (int)(ncols*SQUARESIZE);
    	boardH = (int)(nrows*SQUARESIZE);
    	boardRotation = 0;
    	}
    	else
    	{
    	double cs = Math.min((double)mainW/nrows,(double)mainH/ncols);
    	SQUARESIZE = (int)cs;
    	boardW = (int)(nrows*SQUARESIZE);
    	boardH = (int)(ncols*SQUARESIZE);
    	boardRotation = Math.PI/2;
    	}
    	int extraW = Math.max(0,(mainW-boardW)/2);
    	int extraH = Math.max(0,(mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH+stateH;
    	int boardBottom = boardY+boardH;
       	layout.returnFromMain(extraW,extraH);
       	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW,stateH,iconRect,stateRect,annotationMenu,eyeRect,reverseRect,noChatRect);
        G.SetRect(boardRect,boardX,boardY,boardW,boardH);
        if(boardRotation!=0)
        {
        	G.setRotation(boardRect, boardRotation,boardX+boardW/2,boardY+boardH/2);
                       } 
 
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        return(boardW*boardH);
    }
    public int cellSize() { return b.cellSize()/2; }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle sq = chipRects[player];
    	Rectangle done = doneRects[player];
    	int boxsize = unitsize*3;
    	Rectangle box = pl.createRectangularPictureGroup(x+boxsize,y,unitsize);
    	G.SetRect(sq, x, y, boxsize,boxsize);
    	if(plannedSeating()) 
    	{ G.SetRect(done, G.Right(box), y+unitsize, unitsize*6, unitsize*3); G.union(box, done); } 
    	G.union(box, sq);
    	pl.displayRotation = rotation;
    	return(box);
    }
	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, int forPlayer, Rectangle r, int player,
        HitPoint highlight)
    {	FanoronaCell chips[]= b.fChips;
        boolean canhit = G.pointInRect(highlight, r) && b.LegalToHitChips(forPlayer) ;
        boolean canDrop = hasMovingObject(highlight);
        int cellW = G.Width(r);
        {	FanoronaCell thisCell = chips[b.getColorMap()[forPlayer]];
        	if(thisCell!=null)
        	{	FanoronaChip thisChip = thisCell.topChip();
        		int left = G.Left(r);
	    		int top = G.Top(r);
	       		if(canhit 
	       				&& G.pointInRect(highlight,left,top,cellW,SQUARESIZE)
	       				&& (canDrop ? b.canDropOn(thisCell) : (thisChip!=null))
	       				)
	    		{ highlight.hitObject = thisCell;
	     		  highlight.hitCode = chipPoolIndex[forPlayer];
	     		  highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
	     		  highlight.awidth = SQUARESIZE/3;
	     		  highlight.hit_x = left+cellW/2;
	     		  highlight.hit_y = top+cellW/2;
	     		  highlight.spriteColor = Color.red;
	     		  
	    		}
                  if(gc!=null)
                	{int pl = b.playerIndex(thisChip);
                	 thisCell.drawChip(gc,this,cellW,G.Left(r) + cellW/2,  G.Top(r) + cellW/2,""+b.chips_on_board[pl]);
                	 GC.setFont(gc,standardPlainFont());
                	//G.frameRect(gc,Color.black,left,top,cellW,SQUARESIZE);
                	}
	       		}
        	}
        }
    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	FanoronaChip.getChip(idx).drawChip(g,this,SQUARESIZE, xp, yp,null);

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
    Image scaled = null;
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      int cx = G.centerX(brect);
      int cy = G.centerY(brect);
      
      GC.setRotation(gc, boardRotation, cx,cy);
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
       
      // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid
      scaled = images[BOARD_INDEX].centerScaledImage(gc, brect,scaled);
     	b.SetDisplayParameters(0.9,1.0,  0.15,0.15,  0);
    	b.SetDisplayRectangle(brect);

      b.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
      
      GC.setRotation(gc, -boardRotation, cx, cy);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, FanoronaBoard gb, Rectangle brect, HitPoint highlight)
    {
        
        GC.setRotatedContext(gc,boardRect,highlight,boardRotation);
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	if(G.debug())
    		{gb.ScoreForPlayer(0,false,true);
    		 gb.ScoreForPlayer(1,false,true);
    		}
        FanoronaCell hitCell = null;
        Hashtable<FanoronaCell,String> prevCells = gb.stackOrigins();
        Hashtable<FanoronaCell,FanoronaCell> prevCaps = gb.lastCapturedCells();
        boolean show = eyeRect.isOnNow();
        for (int row = gb.boardRows; row>0; row--)	// back to front
        { //where we draw the grid
        	for (int col = gb.boardColumns-1; col>=0; col--)
        	{
            char thiscol = (char) ('A' + col);
            FanoronaCell cell = gb.getCell(thiscol,row);
            int ypos = G.Bottom(brect) - gb.cellToY(thiscol, row);
            int xpos = G.Left(brect) + gb.cellToX(thiscol, row);
            FanoronaChip chip = cell.topChip();
            String orig = prevCells.get(cell);
            cell.drawChip(gc, this, SQUARESIZE, xpos,ypos,null);
            	if((chip==null) && prevCaps.get(cell)!=null)
            	 {	StockArt.SmallX.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
              	 }
               	 if(orig!=null)
            	 {	// mark the path back
               		StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,orig);
            	 }
               	 else if((chip!=null)&& G.debug())
               	 { // debug mcd
               		int mcs = cell.sweep_distance;
               		int cov = cell.sweep_coverage;
               	 	GC.Text(gc,true,xpos-10,ypos-10,20,20,Color.red,null,""+mcs+" "+cov);
               	 }
              boolean canHit = gb.LegalToHitBoard(cell);
              if(show && canHit)
              {
            	  StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
              }
              if((highlight!=null)
            		  && canHit
              		  && cell.closestPointToCell(highlight, SQUARESIZE,xpos, ypos)
                    )
              { hitCell = cell; 
             }}
             }
        
  
        if(hitCell!=null)
        {  	highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = 3*CELLSIZE/2;
        	highlight.hitCode = FanId.BoardLocation;
        	highlight.spriteColor = Color.red;
        }

        // for testing, draw the connectivity grid
        if(testing && gc!=null)
        	{
        for(FanoronaCell c = b.allCells; c !=null; c=c.next)
        {  int ypos = G.Bottom(brect) - gb.cellToY(c.col, c.row);
           int xpos = G.Left(brect) + gb.cellToX(c.col, c.row);
           for(int direction=0;direction<8;direction++)
        	{	FanoronaCell cp = c.moveTo(direction);
        		if(cp!=null)
        		{
        		int nxpos =  G.Left(brect) + gb.cellToX(cp.col, cp.row);
        		int nypos = G.Bottom(brect) - gb.cellToY(cp.col, cp.row);
        		GC.setColor(gc,((direction&1)==0) ? Color.red : Color.yellow);
        		GC.drawLine(gc,xpos,ypos,nxpos,nypos);
        		}
       	}
        }}
        GC.unsetRotatedContext(gc,highlight);

    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    { 
      FanoronaBoard gb = disB(gc);
      int whoseTurn = gb.whoseTurn;
      boolean planned = plannedSeating(); 
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      FanoronaState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX;i++)
        {	commonPlayer p = getPlayerOrTemp(i);
        	p.setRotatedContext(gc, highlight, false);
        	DrawCommonChipPool(gc,i,chipRects[i], whoseTurn,ot);
        	if(planned && (i==whoseTurn)) 
        		{ handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null),HighlightColor,rackBackGroundColor); 
        		}
        	p.setRotatedContext(gc, highlight, true);
        }
        
        GC.setFont(gc,standardBoldFont());
        commonPlayer pl = getPlayerOrTemp(whoseTurn);
		double messageRotation = pl.messageRotation();
		
        switch(vstate)
        {
        default: break;
        case PLAY_STATE:
        	if(gb.drawIsLikely())
        	{	// if not making progress, put the draw option on the UI
            	if(GC.handleSquareButton(gc,acceptDrawRect,select,s.get(OFFERDRAW),
            			HighlightColor,
            			vstate==FanoronaState.DrawPending ? HighlightColor : rackBackGroundColor))
            	{
            		select.hitCode = GameId.HitOfferDrawButton;
            	}
       		
        	}
        	break;
        case AcceptOrDecline:
        case AcceptPending:
        case DeclinePending:
        	if(GC.handleSquareButton(gc,messageRotation,acceptDrawRect,select,s.get(ACCEPTDRAW),HighlightColor,rackBackGroundColor))
        	{
        		select.hitCode = GameId.HitAcceptDrawButton;
        	}
        	if(GC.handleSquareButton(gc,messageRotation,declineDrawRect,select,s.get(DECLINEDRAW),HighlightColor,rackBackGroundColor))
        	{
        		select.hitCode = GameId.HitDeclineDrawButton;
        	}
       	break;
        }

		if (vstate != FanoronaState.PUZZLE_STATE)
        {
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
        }

		drawPlayerStuff(gc,(vstate==FanoronaState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);

		standardGameMessage(gc,messageRotation,
            		vstate==FanoronaState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
            				vstate!=FanoronaState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
		gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);
         
        DrawRepRect(gc,messageRotation,Color.black,gb.Digest(),repRect);
        DrawReverseMarker(gc,reverseRect,ourSelect,FanId.Reverse);
        eyeRect.activateOnMouse = true;
        eyeRect.draw(gc,highlight);
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
        return (new FanoronaMovespec(st, player));
    }
    

private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_CAPTUREA:
    case MOVE_CAPTUREW:
    case MOVE_BOARD_BOARD:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
   	break;
    case MOVE_REMOVE:
    case MOVE_PICKB:
    case MOVE_PICK:
    	 playASoundClip(light_drop,100);
    	 break;
    case MOVE_DROP:
    case MOVE_DROPB:
      	 playASoundClip(heavy_drop,100);
      	break;
    default: break;
    }
	
}
boolean isDesignation(FanoronaCell cell)
{
   	return((b.getState()==FanoronaState.DESIGNATE_STATE)
   			&& (cell!=b.droppedDest[b.stackIndex]));
	
}
boolean startMotion(FanId hitObject,FanoronaCell cell,FanoronaChip chip)
{
	if(chip!=null)
	{
    switch(hitObject)
    {
    case Black_Chip:
    	PerformAndTransmit("Pick B "+cell.row+" "+b.playerIndex(chip));
    	break;
    case White_Chip:
    	PerformAndTransmit("Pick W "+cell.row+" "+b.playerIndex(chip));
    	break;
    case BoardLocation:
    	if(isDesignation(cell)) {}
    	else  { 	PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+b.playerIndex(chip)); }
    	break;
	default:
		break;
    }}
    return(hasMovingObject(null));
}
 
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof FanId) // not dragging anything yet, so maybe start
        {
        FanId hitObject = (FanId)hp.hitCode;
		FanoronaCell cell = b.getCell(hitCell(hp));
		FanoronaChip chip = (cell==null) ? null : cell.topChip();
		hp.dragging = startMotion(hitObject,cell,chip);
        }
    }

	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof FanId)) { missedOneClick = performStandardActions(hp,missedOneClick); }
     	else {
     	missedOneClick = false;
   		FanId hitObject = (FanId)hp.hitCode;
        FanoronaState state = b.getState();
		boolean movingObject = hasMovingObject(hp);
		FanoronaCell cell = b.getCell((FanoronaCell)hitCell(hp));
		FanoronaChip cup = (cell==null) ? null : cell.topChip();
        if(!movingObject && (cup!=null))	// nothing moving and we hit something movable
        { if(isDesignation(cell)) { PerformAndTransmit("Remove "+cell.col+" "+cell.row+" "+b.playerIndex(cup)); }
          else { startMotion(hitObject,cell,cup); }
        }
        else
        {
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case ToggleEye:
        	eyeRect.toggle();
        	break;
        case BoardLocation:	// we hit the board 
			if(movingObject && (cell!=null)) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
			else { performReset(); }
			break;
        case Reverse:
        	b.setReverseY(!b.reverseY());
        	generalRefresh();
        	break;
        case White_Chip:
        case Black_Chip:
        	if(cell!=null)
        	{
           	switch(state)
           	{
           	default: throw G.Error("can't drop on rack in state %s",state);
          	case PLAY_STATE:
           		PerformAndTransmit(RESET);
           		break;
           	case PUZZLE_STATE:
            	String col = (hitObject==FanId.Black_Chip) ? " B " : " W ";
           		PerformAndTransmit("Drop"+col+cell.row+" "+movingObject);
           		break;
           	}
        	}
           break;
        }
        }
        }
    }


    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Fanoronar_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
         b.doInit(token);
    }

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new FanoronarPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/25/2023
     * 	28398 files visited 0 problems
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
                b.doInit(value);
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

