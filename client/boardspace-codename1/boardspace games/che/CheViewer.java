package che;

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
import lib.Random;
import lib.StockArt;

/**
 * 
 * Overall Architecture
 * 
 * The site provides the lobby, choice game and opponents, communication between the players, information 
 * for spectators,  rankings, and a host of other services.  Each game has to be concerned only with 
 * the game itself.   An individual game (say, Hex) is launched and each client independently initializes
 * itself to a common starting state.   Thereafter each player specifies messages to be broadcast to the
 * other participants, and receives messages being broadcast by the other participants, which keep everyone
 * informed about the state of the game.  There is no common "true" state of the game - all the participants
 * keep in step by virtue of seeing the same stream of messages.    Messages are mostly simple "pick up a stone"
 * "place a stone on space x" and so on.
 * 
 * The things a game must implement are specified by the class "ViewerProtocol", and a game could just
 * start there and be implemented completely from scratch, but in practice there is another huge pile
 * of things that every game has to do; dealing with graphics, mouse events, saving and restoring the
 * game state from static records, replaying and reviewing games and so on.   These are implemented in the 
 * class "commonCanvas" and by several board-like base classes for Hex and Square geometry boards.   
 * All the existing games for boardspace use these classes to provide graphics and basic board representation.
 * 
 * For games with robot players, there is another huge pile of things that a robot has to do, generating
 * moves, evaluating and choosing the best, and implementing a lookahead several moves deep.   There's a
 * standard framework for this using the "RobotProtocol" class and the "SearchDriver" class. 
 */

/**
 * 
 * Change History
 */
public class CheViewer extends CCanvas<CheCell,CheBoard> implements CheConstants, GameLayoutClient
{	static final String Che_SGF = "che"; // sgf game number allocated for hex
		// file names for jpeg images and masks
	static final String ImageDir = "/che/images/";

     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(165,191,146);
    private Color boardBackgroundColor = new Color(165,191,146);
    
 
    
    // private state
    private CheBoard bb = null; //the board from which we are displaying
    private final double INITIAL_TILE_SCALE = 3.0;

    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
   
    private Rectangle sampleRects[] =  addRect("sample",2);
    private Rectangle chipRect = addRect("firstPlayerChipRect");
    private Rectangle poolRect = addRect("poolRect");
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);

	private Image textures[] = null;

    public void preloadImages()
    {	CheChip.preloadImages(loader,ImageDir);
    	if(textures==null)
    	{
            textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = CheChip.getChip(0).image;
    }
    Color CheMouseColors[] = { new Color(0.7f,0.7f,1.0f),new Color(0.3f,0.3f,1.0f)};
    Color CheMouseDotColors[] = { Color.black,Color.white};
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
        use_grid = false;
        gridOption.setState(false);

        // use_grid=reviewer;// use this to turn the grid letters off by default
        MouseColors = CheMouseColors;
        MouseDotColors = CheMouseDotColors;
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),CheId.ZoomSlider);
        zoomRect.min=1.0;
        zoomRect.max=5.0;
        zoomRect.value=INITIAL_TILE_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = HighlightColor;       
        bb = new CheBoard(info.getString(OnlineConstants.GAMETYPE, Che_INIT),
        		getStartingColorMap());
        useDirectDrawing(true);
        doInit(false);
        labelColor = Color.red;
        labelFont = largeBoldFont();
    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
  
        bb.doInit(bb.gametype,0L);						// initialize the board
	 	if(!preserve_history) 
	 		{ board_center_x = board_center_y = 0.0; 
	 		zoomRect.setValue(INITIAL_TILE_SCALE);
	 		startFirstPlayer();
	        }
    }
    
	public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit) 
	{	commonPlayer pl = getPlayerOrTemp(player);
		int u2 = unit/2;
		boolean planned = plannedSeating();
		int donew = planned ? unit*4 : 0;
		Rectangle box = pl.createRectangularPictureGroup(x+(planned?donew:2*unit),y,2*unit/3);
		Rectangle sampleRect = sampleRects[player];
		Rectangle doneRect = doneRects[player];
		G.SetRect(sampleRect,x+(planned?u2:0),y, unit+u2,unit+u2);
		G.SetRect(doneRect, x, y+unit+3*unit/4,donew,donew/2);
	    G.union(box, sampleRect,doneRect);
	    pl.displayRotation = rotation;
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
    	int minLogW = fh*14;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int vcrW = fh*18;
        int CELLSIZE=(int)(fh*2.5);
        int C4 = CELLSIZE/4;
        int margin = fh/2;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	// 60% of space allocated to the board
    			0.6,	// board share
    			1.0,	// aspect ratio for the board
    			fh*2,	// minimum cell size
    			fh*3,	// maximum cell size
    			0.5		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	
    	
       	int chipW = CELLSIZE*2;
       	int chipH = CELLSIZE*10;
       	
        layout.placeRectangle(poolRect,
        		chipW, chipH,chipW,chipH,
        		chipH,chipW,chipH,chipW,
        		BoxAlignment.Center,true);
        layout.placeRectangle(chipRect,
        		chipW, chipH,chipW,chipH,
        		chipH,chipW,chipH,chipW,
        		BoxAlignment.Center,true);
   
       	 layout.placeDoneEdit(chipW*2,chipW*2, doneRect,editRect); 
  	
    	Rectangle main = layout.getMainRectangle();
    	int boardX = G.Left(main);
    	int stateY = G.Top(main);
        int stateH = CELLSIZE;
        int boardY = stateY+stateH;
    	int boardW = G.Width(main);
    	int boardH = G.Height(main)-stateH*2;
        
      	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int zoomW = CELLSIZE*5;

        G.placeStateRow(boardX,stateY,boardW ,stateH,iconRect,stateRect,noChatRect);
        G.placeRight(stateRect, zoomRect, zoomW);

    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	SetupVcrRects(boardX+C4,boardBottom-vcrW/2-C4,vcrW,vcrW/2);
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
 
    }


	// draw a box of spare chips. For hex it's purely for effect.
    private void DrawChipPool(Graphics gc, Rectangle r,HitPoint highlight,CheBoard gb)
    {	int w = G.Width(r);
    	int h = G.Height(r);
    	int l = G.Left(r);
    	int t = G.Top(r);
    	boolean xmajor = w>h;
    	int ystep = xmajor ? 0 : h/4;
    	int xstep = xmajor ? w/4 : 0;
    	int step = xstep+ystep;
    	int size = (int)(0.9*(xmajor ? h : w));
        boolean canhit = gb.LegalToHitChips() && G.pointInRect(highlight, r);
        if (canhit)
        {  highlight.awidth = size/2;
           highlight.arrow = (gb.pickedObject!=null) ? StockArt.DownArrow : StockArt.UpArrow;
           highlight.spriteColor = Color.red;
        }
        for(int i=0,y=t+step/2,x=l+step/2;
        	i<CheChip.nChips;
        	i++,y+=ystep,x+=xstep)
        {	CheCell c = gb.chipPool[i];
        	c.drawChip(gc,this,canhit?highlight:null,size,x,y,null);
        }

    }

	// draw a box of spare chips. Gameover when you run out.
    private void DrawChipStack(Graphics gc, Rectangle r0,CheBoard gb)
    {	int w = G.Width(r0);
    	int h = G.Height(r0);
    	boolean rot = w>h;
    	Rectangle r = r0;
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	if(rot) 
    	{ GC.setRotation(gc, Math.PI/2, cx, cy);
    	  r = G.copy(null,r);
    	  G.setRotation(r,Math.PI/2,cx,cy);
    	  int t = w;
    	  w = h;
    	  h = t;
    	}
    	int chipsleft = CHIPS_IN_GAME-gb.chips_on_board;
    	int quarter = (CHIPS_IN_GAME/2);
    	double ystep = w/10.0;
    	int bot = G.Bottom(r)-w;
    	int x = G.Left(r) +w/2;
    	Random rr = new Random(123);
    	for(int i=0;i<chipsleft;i++)
    	{	StockArt sample = CheChip.OBLIQUE_CHIPS[i/(quarter/2)];
    		int off = (w*(i/quarter))/5;
    		int rx = Random.nextInt(rr,(int)(ystep/2));	// a little randomness in x position
    		sample.drawChip(gc,this,
    				2*w/3,
    				x-off-rx,
    				bot-(int)((i%quarter)*ystep)+off,null);
    	}
    	if(rot) { GC.setRotation(gc, -Math.PI/2,cx,cy); }
    }
    //
    // sprites are normally a game piece that is "in the air" being moved
    // around.  This is called when dragging your own pieces, and also when
    // presenting the motion of your opponent's pieces, and also during replay
    // when a piece is picked up and not yet placed.  While "obj" is nominally
    // a game piece, it is really whatever is associated with b.movingObject()
    //
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {	boolean inboard = boardRect.contains(xp,yp);
   		int cellS = inboard? bb.cellSize() : G.Width(chipRect) ;
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
    	CheChip.getChip(obj).drawChip(g,this,cellS, xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  


    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { // erase
      boolean review = reviewMode() && !mutable_game_record;
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);   
         textures[review ? BACKGROUND_REVIEW_INDEX:BACKGROUND_TABLE_INDEX].tileImage(gc,
          		boardRect); 
      GC.frameRect(gc,Color.black,boardRect); 

    }
    

   /* draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */

    private void drawBoardElements(Graphics gc, CheBoard gb, Rectangle tbRect,
    		HitPoint ourTurnSelect,HitPoint anySelect)
    {	
       Rectangle oldClip = GC.combinedClip(gc,tbRect);
       int cellSize = gb.cellSize();
    	//
        // now draw the contents of the board and anything it is pointing at
        //
        boolean somehit = draggingBoard();
        Hashtable<CheCell,CheCell> dests = gb.movingObjectDests();
        //System.out.println("cs "+cs/CELLSIZE+ " "+cs+" "+CELLSIZE);
        int left = G.Left(tbRect);
        int top = G.Bottom(tbRect);
        for(Enumeration<CheCell> cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements();)
        {
                CheCell cell = cells.nextElement();
                int xpos = left+gb.cellToX(cell);
                int ypos = top - gb.cellToY(cell);
                boolean canhit = gb.LegalToHitBoard(cell);
                boolean isADest = dests.get(cell)!=null;
                boolean isASource = gb.isSource(cell) || gb.isDest(cell);
                CheChip piece = cell.topChip();
                boolean hitpoint = !somehit 
                	&& canhit
                	&& G.pointInRect(xpos,ypos,tbRect)
                	&& G.pointInside(ourTurnSelect, xpos, ypos, cellSize/2);
                String labl = use_grid ? cell.cellName : "";//+ccell.col+ccell.row;              
                if(canhit 
                	&& !hitpoint
                	&& (piece!=null) 
                	&& G.pointInsideSquare(ourTurnSelect, xpos, ypos, cellSize/2)
                	)
                {
                	ourTurnSelect.hitCode = CheId.RotateTile;
                	ourTurnSelect.hitObject = cell;
                	ourTurnSelect.arrow = StockArt.Rotate_CW;
                	ourTurnSelect.awidth = 2*cellSize/3;
                	ourTurnSelect.hit_x = xpos;
                	ourTurnSelect.hit_y = ypos;
                	ourTurnSelect.spriteColor = Color.blue;
               }
                // drawing
                if (hitpoint)
                {	 //if(gc!=null) { gc.drawOval(xpos-cellSize/2,ypos-cellSize/2,cellSize,cellSize); }
                    ourTurnSelect.hitCode = (piece==null)?CheId.EmptyBoard:CheId.BoardLocation;
                    ourTurnSelect.arrow = (piece==null)?StockArt.DownArrow:StockArt.UpArrow;
                    ourTurnSelect.awidth = cellSize/2;
                    ourTurnSelect.hitObject = cell;
                	ourTurnSelect.hit_x = xpos;
                	ourTurnSelect.hit_y = ypos;
                    ourTurnSelect.spriteColor = Color.red;
                    somehit = true;
                }
                //G.DrawAACircle(gc,xpos,ypos,1,tiled?Color.green:Color.blue,Color.yellow,true);
                if(piece!=null)
                {	cell.drawChip(gc,this,piece,null,cellSize,xpos,ypos,labl); 
                	//StockArt.SmallO.drawChip(gc,this,cellSize,xpos,ypos,null);
                }
                if(isASource)
                {GC.cacheAACircle(gc,xpos,ypos,2,Color.green,Color.yellow,true);
                } else
                if(isADest)
                {GC.cacheAACircle(gc,xpos,ypos,2,Color.red,Color.yellow,true);
                }
                }
        doBoardDrag(tbRect,anySelect,cellSize,CheId.InvisibleDragBoard); 
 
 		GC.setClip(gc,oldClip);
    }


    /*
     * draw the main window and things on it.  
     * If gc!=null then actually draw, 
     * If selectPos is not null, then as you draw (or pretend to draw) notice if
     * you are drawing under the current position of the mouse, and if so if you could
     * click there to do something.  Care must be taken to consider if a click really
     * ought to be allowed, considering spectator status, use of the scroll controls,
     * if some board token is already actively moving, and if the game is active or over.
     * 
     * This dual purpose (draw, and notice mouse sensitive areas) tends to make the
     * code a little complicated, but it is the most reliable way to make sure the
     * mouse logic is in sync with the drawing logic.
     * 
    General GUI checklist

    vcr scroll section always tracks, scroll bar drags
    lift rect always works
    zoom rect always works
    drag board always works
    pieces can be picked or dragged
    moving pieces always track
    stray buttons are insensitive when dragging a piece
    stray buttons and pick/drop are inactive when not on turn
*/
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  CheBoard gb = disB(gc);
       CheState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
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
       
       redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,nonDragSelect);
       DrawChipPool(gc, chipRect, ourTurnSelect,gb);
       DrawChipStack(gc, poolRect,gb);
       boolean planned = plannedSeating();
       zoomRect.draw(gc,nonDragSelect);
   	   int map[]=gb.getColorMap();
       
       {   gb.findBlobs(0);
       	   gb.findBlobs(1);
       	   GC.setFont(gc,standardBoldFont());
       	   for(int i=0;i<2;i++)
       	   {
       		commonPlayer pl = getPlayerOrTemp(i);
       		Rectangle r = sampleRects[i];
       		pl.setRotatedContext(gc,selectPos,false);     	   
       		
           textures[PlayerSamples[map[i]]].centerImage(gc,r);
            GC.Text(gc,true,
            		G.Left(r),G.Top(r),
            		G.Width(r)/3,G.Height(r)/3,
            		Color.white,null,
        		   	""+gb.max_blob_size[i]);
            if(planned && (bb.whoseTurn==i))
            { handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
            		HighlightColor, rackBackGroundColor);
            }
        	pl.setRotatedContext(gc, selectPos, true);	   	
       	   }     		   	
     }
       
   	   drawPlayerStuff(gc,(state==CheState.PUZZLE_STATE),buttonSelect,
	   			HighlightColor, rackBackGroundColor);

		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

		if (state != CheState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned) {handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor); }
                
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos, HighlightColor, rackBackGroundColor);
        }
        // draw the avatars
        standardGameMessage(gc,messageRotation,
            		state==CheState.GAMEOVER_STATE?gameOverMessage():s.get(state.getDescription()),
            		state!=CheState.PUZZLE_STATE,
            		gb.whoseTurn,
            		stateRect);
       textures[PlayerSamples[map[gb.whoseTurn]]].centerImage(gc,iconRect);
        goalAndProgressMessage(gc,nonDragSelect,s.get(GoalMessage),progressRect, goalRect);
            //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for hex
        
        // draw the vcr controls
        drawVcrGroup(nonDragSelect, gc);

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
		lastDropped = bb.lastDropped;	// this is for the image adjustment logic
		if(replay!=replayMode.Replay) { playSounds(mm); }
       return (true);
    }
 void playSounds(commonMove mm)
 {
	 switch(mm.op)
	 {
	 case MOVE_DROPB:
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
        return (new Chemovespec(st, player));
    }

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof CheId) // not dragging anything yet, so maybe start
        {

        CheId hitObject =  (CheId)hp.hitCode;
        CheCell c = hitCell(hp);
 	    switch(hitObject)
	    {
	    default: break;
	    
        case InvisibleDragBoard:
        	break;
 	    case ChipPool0:
	    case ChipPool1:
	    case ChipPool2:
	    case ChipPool3:
	   		  PerformAndTransmit("Pick "+(hitObject.ordinal()-CheId.ChipPool0.ordinal()));
	    	break;
	    case BoardLocation:
	    	PerformAndTransmit("Pickb "+c.col+" "+c.row);
	    	break;
        }
         }
    }
	private void doDropChip(char col,int row,CheChip ch)
	{	CheState state = bb.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case PUZZLE_STATE:
			PerformAndTransmit("dropb "+col+" "+row+" "+ch.index);
			break;
		case CONFIRM_STATE:
		case FIRST_PLAY_STATE:
		case PLAY_STATE:
		case PLAY2_STATE:
			PerformAndTransmit("dropb "+col+" "+row+" "+ch.index);
			break;
					                 
		
		}
	}
	private void doDropChip(char col,int row)
	{
		CheChip mo = bb.pickedObject;
		if(mo==null) { mo=bb.lastPicked; }
		if(mo==null) { mo=CheChip.getChip(0); }
		doDropChip(col,row,mo);
	}
	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging(HitPoint hp)
    {
    	CellId id = hp.hitCode;
       	if(!(id instanceof CheId)) { missedOneClick = performStandardActions(hp,missedOneClick);}
    	else
    	{
    	missedOneClick = false;
        CheId hitCode = (CheId)id;
        CheCell hitObject = hitCell(hp);
        CheState state = bb.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", id);
        case InvisibleDragBoard:
        case ZoomSlider:
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PLAY2_STATE:
				if(!bb.isDest(hitObject))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					throw G.Error("shouldn't hit a chip in state %s",state);
					}
				// fall through and pick up the previously dropped piece
				/*$FALL-THROUGH$*/
			case PUZZLE_STATE:
				PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
				break;
			}
			break;
        case RotateTile:
        	if(bb.pickedObject==null)
        	{
        		int index = hitObject.topChip().index;
        		PerformAndTransmit("rotate "+hitObject.col+" "+hitObject.row+" " + (index^2));
        	}
        	break;
        case EmptyBoard:
			switch(state)
			{
				default:
					throw G.Error("Not expecting hit in state %s",state);
				case CONFIRM_STATE:
				case PLAY_STATE:
				case PLAY2_STATE:
				case FIRST_PLAY_STATE:
				case PUZZLE_STATE:
					doDropChip(hitObject.col,hitObject.row);
					break;
			}
			break;
			
        case ChipPool0:
        case ChipPool1:
        case ChipPool2:
        case ChipPool3:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop");
			}
           break;
 
        }
        }

    }

    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     * 
     * if complete is true, we definitely want to start from scratch, otherwise
     * only the known changed elements need to be painted.  Exactly what this means
     * is game specific, but for hex the underlying empty board is cached as a deep
     * background, but the chips are painted fresh every time.
     * 
     * this used to be very important to optimize, but with faster machines it's
     * less important now.  The main strategy we employ is to paint EVERYTHING
     * into a background bitmap, then display that bitmap to the real screen
     * in one swell foop at the end.
     */
    public void drawCanvas(Graphics offGC, boolean complete,HitPoint hp)
    {	
        bb.SetDisplayParameters(zoomRect.value,1.0,board_center_x,board_center_y,0.0); // shrink a little and rotate 30 degrees
     	bb.SetDisplayRectangle(boardRect);
		super.drawCanvas(offGC,complete,hp);
    }
    // return what will be the init type for the game
    public String gameType() { return(bb.gametype); }	// this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Che_SGF); }	// this is the official SGF number assigned to the game

   
    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        bb.doInit(token,0L);
    }



/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically.
 * 
 * This is a good place to make notes about threads.  Threads in Java are
 * very dangerous and tend to lead to all kinds of undesirable and/or flakey
 * behavior.  The fundamental problem is that there are three or four sources
 * of events from different system-provided threads, and unless you are very
 * careful, these threads will all try to use and modify the same data
 * structures at the same time.   Java "synchronized" declarations are
 * hard to get right, resulting in synchronization locks, or lack of
 * synchronization where it is really needed.
 * 
 * This toolkit addresses this problem by adopting the "one thread" model,
 * and this is where it is.  Any other threads should do as little as possible,
 * mainly leave breadcrumbs that will be picked up by this thread.
 * 
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
    {  return(new ChePlay());
    }

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
            {
                bb.doInit(value,0L);
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


}

