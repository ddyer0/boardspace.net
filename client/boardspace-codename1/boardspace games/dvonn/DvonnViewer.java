package dvonn;

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
import lib.SimpleSprite;
import lib.Slider;
import lib.StockArt;




/**
 * 
 * Change History
 *
 * Feb 2008 initial work. 
 * Jan 2013 Changed moving object to a stack.  Added animation to robot and replay
 *
 * This code is derived from the "HexGameViewer" and other viewer classes.  Refer to the
 * documentation there for overall structure notes.
 * 
 */
public class DvonnViewer extends CCanvas<DvonnCell,DvonnBoard> implements DvonnConstants, GameLayoutClient
{	
    /**
	 * 
	 */
    static final double INITIAL_CHIP_SCALE = 0.125;
    static final double MAX_CHIP_SCALE = 0.2;
    static final double MIN_CHIP_SCALE = 0.05;
    static final String Dvonn_SGF = "Dvonn"; // sgf game number allocated for hex
    // file names for jpeg images and masks
    static final String ImageDir = "/dvonn/images/";

    static final int BOARD_INDEX = 0;
    static final int BOARD_FLAT_INDEX = 1;
    static final String[] ImageFileNames = 
        {
    	"board","board-flat"
        };
    
               
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int LIFT_ICON_INDEX = 2;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "lift-icon"};
    
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    private Color chatBackgroundColor = new Color(250,240,230);
    // images
    private static Image[] images = null; // images of black and white gobblets and board
    private static Image[] textures = null;// background textures
    // private state
    private DvonnBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle innerBoardRect = addRect(".innerBoard");
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle scoreRects[] = addRect("score",2);
    private Rectangle  rackRects[] = addRect(".rack",2);

    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    private Color ZoomHighlightColor = new Color(150,195,166);
    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;

    boolean usePerspective() { return(getAltChipset()==0); }
    public void preloadImages()
    {	
    	if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
    	  // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    	DvonnChip.preloadImages(loader,ImageDir);
    	images = loader.load_images(ImageDir, ImageFileNames, loader.load_images(ImageDir, ImageFileNames,"-mask")); // load the main images
        textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = textures[LIFT_ICON_INDEX];
    }
    public Slider sizeRect = null;


	/**
	 * 
	 * this is the real instance initialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
        int randomKey = sharedInfo.getInt(OnlineConstants.RANDOMSEED,-1);
        labelFont = largeBoldFont();
        sizeRect = addSlider(TileSizeMessage,s.get(StackHeightMessage),DvonnId.ZoomSlider);
        sizeRect.min=-MAX_CHIP_SCALE;
        sizeRect.max=MAX_CHIP_SCALE;
        sizeRect.value=INITIAL_CHIP_SCALE;
        sizeRect.barColor=ZoomColor;
        sizeRect.highlightColor = ZoomHighlightColor;
 
        b = new DvonnBoard(randomKey,info.getString(OnlineConstants.GAMETYPE, "dvonn"),
        		getStartingColorMap());
        useDirectDrawing(true);
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);			// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
    }
    
public void setLocalBounds(int x, int y, int width, int height)
{	G.SetRect(fullRect, x, y, width, height);
	GameLayoutManager layout = selectedLayout;
	int nPlayers = nPlayers();
   	int chatHeight = selectChatHeight(height);
   	// ground the size of chat and logs in the font, which is already selected
	// to be appropriate to the window size
	int fh = G.defaultFontSize;
	int minLogW = fh*16;	
	int vcrW = fh*16;
   	int minChatW = fh*40;	
    int minLogH = fh*10;	
    int margin = fh/2;
    int buttonW = fh*8;
    int stateH = fh*3;
    // this does the layout of the player boxes, and leaves
	// a central hole for the board.
	// in this mode, with zoom, width/zoom and height/zoom are
    // the actual size of the window.
	layout.selectLayout(this, nPlayers, width, height,
			getGlobalZoom(),margin,	
			0.75,	// 60% of space allocated to the board
			1.75,	// aspect ratio for the board
			fh*2,	// maximum cell size
			fh*2.5,
			0.4		// preference for the designated layout, if any
			);
    // place the chat and log automatically, preferring to place
	// them together and not encroaching on the main rectangle.
	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
   	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
	layout.placeTheVcr(this,vcrW,vcrW*3/2);
   	//layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);

	Rectangle main = layout.getMainRectangle();
	int mainX = G.Left(main);
	int mainY = G.Top(main);
	int mainW = G.Width(main);
	int mainH = G.Height(main)-stateH*2;
	// There are two classes of boards that should be rotated. For boards with a strong
	// "my side" orientation, such as chess, use seatingFaceToFaceRotated() as
	// the test.  For boards that are noticably rectangular, such as Push Fight,
	// use mainW<mainH
	boolean rotate = mainW<(mainH*3/4);	
  	int lastcol = b.ncols;
   	int lastrow = 6;
    int ncols = rotate ? lastrow : lastcol;
    int nrows = rotate ? lastcol : lastrow;

	// calculate a suitable cell size for the board
	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
        CELLSIZE = (int)cs;
	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
	// center the board in the remaining space
	int boardW = (int)(ncols*CELLSIZE);
	int boardH = (int)(nrows*CELLSIZE);
	int extraW = Math.max(0, (mainW-boardW)/2);
	int extraH = Math.max(0, (mainH-boardH)/2);
	int boardX = mainX+extraW;
	int boardY = mainY+extraH;
        int boardBottom = boardY+boardH;
	//
	// state and top ornaments snug to the top of the board.  Depending
	// on the rendering, it can occupy the same area or must be offset upwards
	//
    int stateY = boardY;
    int stateX = boardX;
    G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,liftRect,reverseViewRect,viewsetRect,noChatRect);
	G.SetRect(boardRect,boardX,boardY+(rotate?CELLSIZE/2:CELLSIZE),boardW,boardH);
	if(rotate)
	{	// this conspires to rotate the drawing of the board
		// and contents if the players are sitting opposite
		// on the short side of the screen.
		G.setRotation(boardRect,-Math.PI/2);
		contextRotation = -Math.PI/2;
	}
	G.copy(innerBoardRect,boardRect);
    G.SetHeight(innerBoardRect,G.Height(innerBoardRect)-CELLSIZE);
    int bx = G.Left(boardRect);
    int by = G.Top(boardRect);
    int bh = G.Height(boardRect);
    int bw = G.Width(boardRect);
	G.SetRect(rackRects[0],bx,by,CELLSIZE,bh-CELLSIZE);
	G.SetRect(rackRects[1],bx+bw-CELLSIZE,by,CELLSIZE,bh-CELLSIZE);
 
	// goal and bottom ornaments, depending on the rendering can share
	// the rectangle or can be offset downward.  Remember that the grid
	// can intrude too.
	int goalY = boardBottom+(rotate?CELLSIZE/2:0);
	G.SetRect(goalRect, boardX, goalY,boardW,stateH);
    int sizeW = stateH*5;
    G.SetWidth(goalRect, G.Width(goalRect)-sizeW);
    G.SetRect(sizeRect, G.Right(goalRect),goalY,sizeW,stateH);

    setProgressRect(progressRect,goalRect);
    positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        }
public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
{	commonPlayer pl = getPlayerOrTemp(player);
	Rectangle chip = chipRects[player];
	Rectangle score = scoreRects[player];
	Rectangle done = doneRects[player];
	
	G.SetRect(chip,	x,	y,	2*unitsize,	2*unitsize);
	G.SetRect(score, x, y+unitsize*2, unitsize*2, unitsize*2);
	
	Rectangle box =  pl.createRectangularPictureGroup(x+2*unitsize,y,unitsize);
	int doneW = plannedSeating()? unitsize*4 : 0;
	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
	G.union(box, done,chip,score);
	pl.displayRotation = rotation;
	return(box);
    }
    

    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	if(idx==DvonnId.PickedStack.ordinal())
    	{
    	b.pickedStack.drawStack(g,this,null,CELLSIZE,xp,yp,0,sizeRect.value,null);
    	}
     	//DvonnChip.getChip(idx).drawChip(g,this,SQUARESIZE,xp,yp,0,1.0,null);
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
    { 	boolean reviewBackground = reviewMode() && !mutable_game_record;
    	boolean perspective = usePerspective();
    	{	// good for board-skew : gb.SetDisplayParameters(0.7,0.8,  0.0,0.00,  2.3, .15, 0.25);
    	// good for board-skew2 gb.SetDisplayParameters(0.67,0.72,  0.0,0.00,  14.5, .22, 0.25);
    	// good for board-skew3 gb.SetDisplayParameters(0.54,0.80,  0.0,-0.30,  7.0, .250, 0.32);
       	if(perspective)
    	{
    	b.SetDisplayParameters(.68,0.8, 0.85,-0.4, 30.0,0.2, 0.10,0.0);
    	}
    	else 
    	{
    	   	b.SetDisplayParameters(.78,1.03, 0.1,0.33, 30,0,0,0.0);  		
    	}
 	    b.SetDisplayRectangle(brect);
    	}

      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      if(perspective)
      {
     images[BOARD_INDEX].centerImage(gc,brect);
      }
      else 
      {	
         images[BOARD_FLAT_INDEX].centerImage(gc,innerBoardRect);
      }
      b.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.yellow);

    }
    private void DrawScore(Graphics gc,Rectangle r,Rectangle chipR,int player)
    {	if(gc!=null)
    	{
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,true,r,0,Color.black,rackBackGroundColor,""+b.scoreForPlayer(player));
    	GC.frameRect(gc,Color.black,r);
    	DvonnChip.getChip(b.getColorMap()[player]).drawChip(gc, this, chipR,null);
    	}
    }
    private boolean drawZoomStack(Graphics gc,DvonnCell cell,int xpos,int ypos,int steps,HitPoint canhit,HitPoint draw,boolean perspective)
	{	double zoom = sizeRect.value;
        int height = cell.chipIndex+1;
        boolean lifting = (draw!=null) && (draw.hitCode==DvonnId.LiftRect);
        String msg = ((height>1) && (lifting || (Math.abs(zoom)<=(MIN_CHIP_SCALE+0.01))))
            				? (""+height)
            				: null;
        boolean hit = cell.drawStack(gc,this,canhit,CELLSIZE,xpos,ypos,steps,perspective?0:zoom,perspective?zoom:0,msg);
        if(hit) 
        {
        	DvonnCell ch = hitCell(canhit);
        	canhit.arrow = ((ch.topChip()==null)||hasMovingObject(canhit))
        			? StockArt.DownArrow 
        			: StockArt.UpArrow;
        	canhit.awidth = CELLSIZE/3;
        	canhit.spriteColor = Color.red;
        }
        return(hit);

 	}
    
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, DvonnBoard rb, Rectangle brect, HitPoint highlight,HitPoint any)
    {	
    	Hashtable<DvonnCell,DvonnCell> dests = rb.getMoveDests();
    	Hashtable<DvonnCell,DvonnCell> removed = rb.getRemoved();
    	boolean dolift = doLiftAnimation();
     	
      	int dotsize = Math.max(2,CELLSIZE/15);
      	boolean canHit =  !dolift && (highlight!=null);
      	
      	boolean perspective = usePerspective();
     	//
        // now draw the contents of the board and anything it is pointing at
        //
       	Enumeration<DvonnCell>cells = rb.getIterator(sizeRect.value>=0 ? Itype.TBRL : Itype.TBLR);
       	while(cells.hasMoreElements())
        	{
       		DvonnCell c = cells.nextElement();
            boolean canHitCell = canHit && rb.LegalToHitBoard(c);
            int ypos = G.Bottom(brect) - rb.cellToY(c);
            int xpos = G.Left(brect) + rb.cellToX(c);
            drawZoomStack(gc,c,xpos,ypos,liftSteps,(canHitCell?highlight:null),any,perspective);
            // mark the cells for debugging
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,""+c.col+c.row);
            if(rb.isSource(c))
            {	GC.cacheAACircle(gc,xpos+dotsize,ypos,dotsize,Color.blue,Color.gray,true);
            }
            if(dests.get(c)!=null)
	        {
	        	GC.cacheAACircle(gc,xpos+dotsize,ypos,dotsize,Color.red,Color.gray,true);
	        }
            if(removed.get(c)!=null)
            {	
           	 StockArt.SmallX.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            }
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
         }
        boolean allPlaced = !rb.placingRings();   
       	for(int player = FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
       	{
	       	commonPlayer pl = getPlayerOrTemp(player);
	       	DvonnCell rack = allPlaced ? rb.captures[player] : rb.rack[player];
	       	drawRack(gc,pl,rb.LegalToHitChips(player)?highlight:null,highlight,rack,rackRects[player]);
       	}

        }
    
    public void drawRack(Graphics gc,commonPlayer pl,HitPoint highlight,HitPoint any,DvonnCell rack,Rectangle r)
    {
       	int zoomoff = (int)((sizeRect.value*0.5/MAX_CHIP_SCALE)*G.Height(r));
           	
        drawZoomStack(gc,rack,G.centerX(r),G.centerY(r)+zoomoff,0,highlight,any,true);

    }
    
    public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  	drawLiftRect(gc,liftRect,highlight,textures[LIFT_ICON_INDEX]); 
    	DrawReverseMarker(gc,reverseViewRect,highlight,DvonnId.ReverseViewButton);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  DvonnBoard gb = disB(gc);

      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      DvonnState vstate = gb.getState();
      
      GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
      HitPoint bot = G.pointInRect(ot, boardRect) ? ot : null;
      drawBoardElements(gc, gb, boardRect, bot, highlight);
      GC.unsetRotatedContext(gc,highlight);
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
      boolean planned = plannedSeating();
      for(int player = FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
      {
       commonPlayer pl = getPlayerOrTemp(player);
   	   pl.setRotatedContext(gc, highlight, false);
  	   DrawScore(gc,scoreRects[player],chipRects[player],player);  	   
	   if(planned && gb.whoseTurn==player)
       {
		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
	   }

   	   pl.setRotatedContext(gc, highlight, true);
       }

       GC.setFont(gc,standardBoldFont());
       drawPlayerStuff(gc,(vstate==DvonnState.PUZZLE_STATE),ourSelect,
	   			HighlightColor, rackBackGroundColor);

		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();
       if (vstate != DvonnState.PUZZLE_STATE)
        {
			if(!planned) 
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
			
        }
 
        
            standardGameMessage(gc,messageRotation,
            		vstate==DvonnState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
            				vstate!=DvonnState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
            DvonnChip.getChip(b.getColorMap()[gb.whoseTurn]).drawChip(gc,this,iconRect,null);
            
            drawAuxControls(gc,highlight);
     	    sizeRect.draw(gc,highlight);
            goalAndProgressMessage(gc,highlight,s.get(GoalMessage),progressRect, goalRect);
            drawViewsetMarker(gc,viewsetRect,highlight);

           // no repetitions are possible in dvonn
        // DrawRepRect(gc,b.Digest(),repRect);
         drawVcrGroup(ourSelect, gc);

    }
    public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode mode)
    {	// the super method in commonCanvas is where the history is actually recorded
       	if((b.getState()==DvonnState.PASS_STATE) && (m.op==MOVE_DONE) && OurMove() && (mode==replayMode.Live))
    	{
         PerformAndTransmit(PASS,true,mode); 
    	}
       	boolean val =  super.PerformAndTransmit(m,transmit,mode);
        return(val);
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
        
        if(replay!=replayMode.Replay) { playSounds((DvonnMovespec)mm); }
        lastDropped = b.lastDropped;
        return (true);
    }
     
     void startBoardAnimations(replayMode replay)
     {	try {
        if(replay!=replayMode.Replay)
     	{	double start = 0.0;
     		double speed = masterAnimationSpeed*0.5;
     		boolean later = false;
     		//
     		// if there is only one pair on the stack it's an ordinary move, and we
     		// migrate it cleanly.  If there are multiple pairs, the later pairs are
     		// captured chips which we want to send off in a staggered manner
     		
     		for(int idx = 0,lim=b.animationStack.size(); idx<lim; idx+=2)
     		{
     		DvonnCell dest = b.animationStack.elementAt(idx+1);
     		DvonnCell src = b.animationStack.elementAt(idx);
     		if(later)
     		{	// this extra animation displays a static image of a stack waiting
     			// to start moving. The cell is a throw away copy that is source, dest, and 
     			// the stack to be painted all in one.
     			SimpleSprite ss = startAnimation(src,src,src,CELLSIZE,0,start);		// stand the stacks while waiting to start moving
     			ss.overlapped = true;	// this keeps the stack visible despite an animation in progress.
     		}
     		SimpleSprite s = startAnimation(src,dest,src,CELLSIZE,start,speed);
     		s.isAlwaysActive = true;	// this makes the destination stack grow monotonically
     		start += masterAnimationSpeed*0.2;
     		speed = masterAnimationSpeed*1.0;
     		later = true;
     		}
     	}}
     	finally {
        	b.animationStack.clear();
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
        return (new DvonnMovespec(st, player));
    }
    

     

private void playSounds(DvonnMovespec m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_DROPB:
    case MOVE_BOARD_BOARD:
    	{
    	  int h = (m.undoInfo==null) ? 1 : (1+(m.undoInfo.length()/3));	// proportional to the number of chips removed
    	  while(h-- > 0) { playASoundClip(light_drop,50); }
    	}
    	break;
    case MOVE_DROP:
    case MOVE_PICKB:
    case MOVE_PICK:
    	 playASoundClip(light_drop,50);
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
        if (hp.hitCode instanceof DvonnId) // not dragging anything yet, so maybe start
        {
        DvonnId hitObject = (DvonnId)hp.hitCode;
		DvonnCell cell = hitCell(hp);
		DvonnChip chip = (cell==null) ? null : cell.topChip();
		
        if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case LiftRect:
        case First_Player_Rack:
        case Second_Player_Rack:
        case First_Player_Captures:
        case Second_Player_Captures:
        	PerformAndTransmit("Pick "+hitObject.shortName+" "+chip.pieceNumber());
        	break;
	    case BoardLocation:
	    	if(cell.chipIndex>=0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.pieceNumber());
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
    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof DvonnId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
   		DvonnId hitObject = (DvonnId)hp.hitCode;
		DvonnCell cell = hitCell(hp);
		DvonnChip cup = (cell==null) ? null : cell.topChip();
		DvonnState state = b.getState();	// state without resignation
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case ReverseViewButton:
          	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
          	 generalRefresh();
          	 break;

        case LiftRect:
        case ZoomSlider: break;
        case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case PLACE_RING_STATE:
			case CONFIRM_PLACE_STATE:
				if(cup==null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				else { PerformAndTransmit("Pickb "+cell.col+" "+cell.row); }
				break;
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(cup!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+cup.pieceNumber());
				}
				break;
			}
			break;
			
        case First_Player_Captures:
        case Second_Player_Captures:
        case First_Player_Rack:
        case Second_Player_Rack:
        	{
        	int mov = b.movingObjectIndex();
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                	case PLAY_STATE:
            		PerformAndTransmit(RESET);
            		break;
               	case PLACE_RING_STATE:
               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop "+hitObject.shortName+" "+mov);
            		break;
            	}
			}
         	}
            break;
        }
        }
     }

        
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
    public String gameType() { return(b.gametype+" "+b.randomKey); }
    public String sgfGameType() { return(Dvonn_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
	    long rk = G.LongToken(his);
	    b.doInit(token,rk);
	}


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new DvonnPlay()); }


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
            {	StringTokenizer tok = new StringTokenizer(value);
        		String gametype = tok.nextToken();
        		long rk = G.LongToken(tok);
                b.doInit(gametype,rk);
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

