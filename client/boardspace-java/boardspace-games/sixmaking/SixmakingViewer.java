package sixmaking;


import java.awt.*;
import javax.swing.JMenuItem;


/* below here should be the same for codename1 and standard java */
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.DefaultId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.StockArt;

import static sixmaking.SixmakingMovespec.*;

/**
 * This code shows the overall structure appropriate for a game view window.
*/
public class SixmakingViewer extends CCanvas<SixmakingCell,SixmakingBoard> implements SixmakingConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    boolean usePerspective() { return(super.getAltChipset()==0); }
 
    // private state
    private SixmakingBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle reverseViewRect = addRect("reverse");
    private JMenuItem offerDrawAction = null;
    private Rectangle chessRect = addRect("chessRect");
    private Rectangle repRect = addRect("repRect");
    private Rectangle declineDrawRect = addRect("declineDraw");
    private Rectangle acceptDrawRect = addRect("acceptDraw");	
    private double chipScale = 0.75;
    /**
     * preload all the images associated with the game. This is delegated to the chip class.
     */
    public void preloadImages()
    {	
       	SixmakingChip.preloadImages(loader,ImageDir);
       	gameIcon = SixmakingChip.chessIconOn.image;
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
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	InternationalStrings.put(SixmakingStrings);
        	InternationalStrings.put(SixmakingStringPairs);
        }

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new SixmakingBoard(info.getString(OnlineConstants.GAMETYPE, Variation.Sixmaking.name),
        		randomKey,players_in_game,repeatedPositions,getStartingColorMap());
        //useDirectDrawing(); // not tested yet
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
    	int chipW = unitsize*5;
    	int chipH = unitsize*7;
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW, chipH);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	G.SetRect(done, x+chipW+unitsize/2, G.Bottom(box)+unitsize/2, chipW, plannedSeating()?chipW/2:0);
    	pl.displayRotation = rotation;
    	G.union(box, chip,done);
    	return(box);
    }
    
    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
       	FontMetrics fm = G.getFontMetrics(standardPlainFont());
    	int fh = standardFontSize();
    	int margin = fh/2;
    	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// space allocated to the board
    			1,		// aspect ratio for the board
    			fh*2.0,
    			fh*3.5,	// maximum cell size
    			0.5		// preference for the designated layout, if any
    			);
    	int minLogW = fh*18;	
    	int vcrw = fh*16;
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*4/3);
    	
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
       	layout.placeDrawGroup(fm, acceptDrawRect, declineDrawRect);
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);
       
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int stateH = fh*3;
    	int ncols = 5;
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH)/ncols);
    	SQUARESIZE = (int)cs;
   	
     	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(ncols*cs);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-stateH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+stateH+extraH;
    	int boardBottom = boardY+boardH;
	      	
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,viewsetRect,liftRect,reverseViewRect,chessRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
 
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.placeRow(stateX, boardBottom-stateH,boardW,stateH,goalRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white); 	
    }

    //
	// reverse view icon, made by combining the stones for two colors.
    //
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	SixmakingChip king = SixmakingChip.getChip(b.reverseY()?1:0);
    	SixmakingChip reverse = SixmakingChip.getChip(b.reverseY()?0:1);
    	int w = G.Width(r);
    	int cx = G.centerX(r);
    	int t = G.Top(r);
    	reverse.drawChip(gc,this,w,cx,+w/4,null);
    	king.drawChip(gc,this,w,cx,t+w*3/4,null);
    	HitPoint.setHelpText(highlight,r,SixmakingId.ReverseViewButton,s.get(ReverseViewExplanation));
     }  

    //
    // implement a "active when pushed" button to separate stacks into
    // their components.  This is animated gradually by the drawBoardElements method
    //
    private void DrawChessRect(Graphics gc,HitPoint highlight)
    {	
    	if(G.pointInRect(highlight,chessRect))
    	{	
    		highlight.hitCode = SixmakingId.ChessRect;
    		highlight.spriteRect = chessRect;
    		highlight.spriteColor = Color.red;
    	}
    	(showChessTops?SixmakingChip.chessIconOff.image:SixmakingChip.chessIconOn.image).centerImage(gc,
				chessRect); 
		GC.frameRect(gc,Color.black,chessRect);
    }
    
    //
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    // for checkers, this is just a single checker, which also displays the number of pieces 
    // left on the board as a sort of progress metric.
    //
    private void DrawCommonChipPool(Graphics gc, SixmakingBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	SixmakingCell chips[]= gb.rack;
    	commonPlayer cp = getPlayerOrTemp(forPlayer);
        boolean canHit = gb.LegalToHitChips(forPlayer);
        SixmakingCell thisCell = chips[forPlayer];
        SixmakingChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = null;
        int sz = (int)(G.Width(r)*chipScale);
        int xp = G.Left(r)+sz/2;
        int yp = G.Bottom(r)-sz/2;
        if(thisCell.drawStack(gc,this,pt,sz,xp,yp,002,0.04,msg))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = sz/3;
        	highlight.hit_index = 1;
        	highlight.spriteColor = Color.red;
        }
        cp.rotateCurrentCenter(thisCell, xp, yp);
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
    	if(obj>0)
    	{
    		SixmakingCell stack = b.pickedStack;
    		stack.drawStack(g,this,null,(int)(chipScale*SQUARESIZE),xp,yp,0,0.1,null);
    	}
     }

 

    /* draw the deep unchangeable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	boolean reviewBackground = reviewMode()&&!mutable_game_record;
    	setBoardRect(b);
      // erase
     SixmakingChip.backgroundTile.image.tileImage(gc, fullRect);   
      //gc.setColor(Color.black);
      //G.fillRect(gc, boardRect);
      if(reviewBackground)
      {	 
       SixmakingChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
      boolean perspective = usePerspective();
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      (perspective ? SixmakingChip.board.image : SixmakingChip.board_np.image).centerImage(gc, boardRect);
      
      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
   private boolean showChessTops = false;
   private boolean drawChipWithIcon(Graphics gc,HitPoint hitNow,SixmakingCell cell,
		   int size,int xpos,int ypos,int liftsteps,int heights)
   {	
   		SixmakingCell proxy = new SixmakingCell(cell);
   		SixmakingChip top = proxy.topChip();
   		if(showChessTops && (top!=null))
   		{
   		int player = top.playerIndex();
   		int chip = Math.min(5,proxy.height()-1);
   		proxy.addChip(SixmakingChip.caps[player][chip]);
   		}
   		if(levelSelectMode!=null)
   		{
   			if(cell!=levelSelectMode) { hitNow=null; }
   			else { xpos-=size/2; size = size*2; }
   		}
   		boolean hit = proxy.drawStack(gc,this,hitNow,size,xpos,ypos,liftSteps,0.1,null);
   		if(gc!=null) { cell.copyCurrentCenter(proxy); }
   		if(hit) 
   			{ if(enterLevelSelect) 
   					{ if(cell.onBoard) 
   						{ levelSelectMode = cell; repaint(); 
   						}
   					  enterLevelSelect = false;
   					  hitNow.hitCode = DefaultId.HitNoWhere; 
   					}
   			else if(b.movingObjectIndex()<0) 
   			{
   			  int index = hitNow.hit_index;
   			  if(((1<<(cell.height()-index))&heights)!=0)
   			  {
   			  hitNow.hitObject = cell;
   			  index--;
   			  if(index>=0 && index<cell.height()-1)
   			  {	proxy.setChipAtIndex(index,SixmakingChip.flat);
   			    while(--index >=0)
   			    { proxy.setChipAtIndex(index,SixmakingChip.blank);
   			    }
   			    proxy.drawStack(gc,this,null,size,xpos,ypos,liftSteps,0.1,null);
   			    }
   			  }
   			  else { 
   				  // can't hit this height due to the Ko rule.
   				  hitNow.hitCode = DefaultId.HitNoWhere;
   				  hitNow.hitObject = null;
   			  }
   		}}
	   return(hit);   
   }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, SixmakingBoard gb, Rectangle brect, HitPoint highlight)
    {
    	// this logic animates the expansion of stacks when the button is pushed.
     	boolean dolift = doLiftAnimation();
     	
     	// targets are the pieces we can hit right now.
     	Hashtable<SixmakingCell,SixmakingMovespec>targets = gb.getTargets();
     	SixmakingCell dest = gb.getDest();		// also the current dest and source
     	SixmakingCell src = gb.getSource();
     	SixmakingCell last = gb.getPrevDest();	// and the other player's last move
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	int sz = (int)(chipScale*SQUARESIZE);
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
       	Enumeration<SixmakingCell>cells = gb.getIterator(Itype.TBRL);
       	int top = G.Bottom(brect);
       	int left = G.Left(brect);
       		
       	while(cells.hasMoreElements())
       	{
       		SixmakingCell cell = cells.nextElement();

            int ypos = top -gb.cellToY(cell);
            int xpos = left + gb.cellToX(cell);
            HitPoint hitNow = !dolift && gb.legalToHitBoard(cell,targets) ? highlight : null;
            SixmakingMovespec target = targets.get(cell);
            int heights = target==null ? -1 : target.height;
            
            if( drawChipWithIcon(gc,hitNow,cell,sz,xpos,ypos,liftSteps,heights))
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	hitNow.arrow =hasMovingObject(highlight)
      				? StockArt.DownArrow 
      				: cell.topChip()!=null?StockArt.UpArrow:null;
            	hitNow.awidth = sz/3;
            	highlight.spriteColor = Color.red;
            	}
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE/4,xpos,ypos,null);
            if((cell==dest)||(cell==src))
            {
            	StockArt.SmallO.drawChip(gc,this,SQUARESIZE/4,xpos,ypos,null);
            }
           if(cell==last)
            {
            	StockArt.Dot.drawChip(gc,this,SQUARESIZE/4,xpos,ypos,null);
            }
        	}
    }
    public void drawAuxControls(Graphics gc,HitPoint highlight)
    {      	
       drawLiftRect(gc,liftRect,highlight,SixmakingChip.liftIcon.image);
       DrawChessRect(gc,highlight);
       DrawReverseMarker(gc,reverseViewRect,highlight);
    }

    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  SixmakingBoard gb = disB(gc);
       setBoardRect(gb);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      SixmakingState vstate = gb.getState();
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        
        boolean planned = plannedSeating();
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
 		double messageRotation = pl.messageRotation();

 		for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
        {
     	 commonPlayer cpl = getPlayerOrTemp(i);
     	 cpl.setRotatedContext(gc, highlight, false);
         DrawCommonChipPool(gc, gb,i,chipRects[i], gb.whoseTurn,ot);
     	 if(planned && (i==gb.whoseTurn))
     	 {
     		 handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
  					HighlightColor, rackBackGroundColor);
     	 }
     	 cpl.setRotatedContext(gc, highlight, true);
        }

        GC.setFont(gc,standardBoldFont());
        switch(vstate)
        {
        default:
        	if(gb.drawIsLikely())
        	{	// if not making progress, put the draw option on the UI
            	if(GC.handleSquareButton(gc,acceptDrawRect,select,s.get(OFFERDRAW),HighlightColor,rackBackGroundColor))
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
        
		if (vstate != SixmakingState.Puzzle)
        {
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==SixmakingState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);

 		standardGameMessage(gc,messageRotation,
            		vstate==SixmakingState.Gameover
            			?gameOverMessage()
            			:s.get(vstate.getDescription()),
            				vstate!=SixmakingState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
 		gb.rack[gb.whoseTurn].topChip().drawChip(gc, this, iconRect, null);
 		
        goalAndProgressMessage(gc,ourSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
        
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        drawAuxControls(gc,ourSelect);
        drawViewsetMarker(gc,viewsetRect,ourSelect);
        drawVcrGroup(ourSelect, gc);

    }
    public int midGamePoint() { return(12); }	// games are short
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
        startBoardAnimations(replay,b.animationStack,(int)(b.cellSize()*chipScale),MovementStyle.Stack);
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
        return (new SixmakingMovespec(st, player));
    }

    
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
	int startDragX = 0;
	int startDragY = 0;
	boolean enterLevelSelect = false;
	SixmakingCell levelSelectMode = null;
	
	public void MouseDown(HitPoint hp)
	{	super.MouseDown(hp);
		if(isTouchInterface() && (b.movingObjectIndex()<0) && G.pointInRect(hp,boardRect)) 
			{ enterLevelSelect = true;
			  repaint();
			}
	}
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof SixmakingId) // not dragging anything yet, so maybe start
        {
        SixmakingId hitObject = (SixmakingId)hp.hitCode;
		SixmakingCell cell = hitCell(hp);
		SixmakingChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case ChessRect:
             break;
	    case Black_Chip_Pool:
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick "+hitObject.shortName+" "+hp.hit_index);
	    	break;
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(!isTouchInterface())
	    	{
	    	if(cell.height()>0)
	    		{
	    		startDragX = G.Left(hp);
	    		startDragY = G.Top(hp);
	    		int cellh = cell.height();
	    		int h = Math.max(1,Math.min(cellh,(cellh-hp.hit_index)));
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+ " "+h);
	    		}
	    	}
	    	else { // on a tablet without near field feedback
	    		levelSelectMode = cell;
	    		enterLevelSelect = false;
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
    	levelSelectMode = null;
    	enterLevelSelect = false;
    	if(!(id instanceof SixmakingId)) 
    		{ // handle all the actions that aren't ours
    			missedOneClick = performStandardActions(hp,missedOneClick); 
    		}
    	else {
    	missedOneClick = false;
        SixmakingId hitObject = (SixmakingId)id;
		SixmakingState state = b.getState();
		SixmakingCell cell = hitCell(hp);
		SixmakingChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);

        case ReverseViewButton:
       	 b.setReverseY(!b.reverseY());
       	 generalRefresh();
       	 break;
        case ChessRect:
        	showChessTops = !showChessTops;
        	break;
        case BoardLocation:	// we hit the board 

				if(b.movingObjectIndex()>=0)
				{
				  PerformAndTransmit("Dropb "+cell.col+" "+cell.row); 
				}
				else if(chip!=null)
				{
		    	int cellh = cell.height();
		    	int h = Math.max(1,Math.min(cellh,(cellh-hp.hit_index)));
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+h);
				}
				break;
			
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

    private void setBoardRect(SixmakingBoard gb)
    {	if(usePerspective())
    	{
    	gb.SetDisplayParameters(new double[]{0.28,0.27}, 
    			new double[]{0.94,0.325},
    			new double[]{0.17,0.86},
    			new double[]{0.80,0.90});
    	}
    else {
    	gb.SetDisplayParameters(1.05,1,0,-0.1,0);
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
    	return(""+b.gametype+" "+b.randomKey+" "+b.nPlayers()); 
   }
    public String sgfGameType() { return(Sixmaking_SGF); }

    
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
    			&& (b.getState()==SixmakingState.Play))
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
    public SimpleRobotProtocol newRobotPlayer() { return(new SixmakingPlay()); }


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

