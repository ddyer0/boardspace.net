package palago;

import bridge.*;
import com.codename1.ui.geom.Rectangle;

import online.common.*;
import java.util.*;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;
import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
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
 * class "commonCanvas" and by several board-like base classes for hexagonal and square geometry boards.   
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
public class PalagoViewer extends CCanvas<PalagoCell,PalagoBoard> implements PalagoConstants, GameLayoutClient,ColorNames
{	   // file names for jpeg images and masks
    static final String ImageDir = "/palago/images/";
    static final String Palago_SGF = "Palago"; // sgf game name
	static final String TileColorMessage = "select tile color";
    
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BACKGROUND_TABLE_INDEX = 2;
    static final String TextureNames[] = { "background-tile" ,"background-review-tile", "green-felt-tile"};


     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(176,185,225);
    private Color boardBackgroundColor = new Color(176,185,225);
    
    private JMenu colorOption = null;
    private int tileColorSet = 2;
    public int getAltChipset() { return(tileColorSet); }
    private JMenuItem aquaOption=null;
    private JMenuItem redOption=null;
    private JMenuItem blueOption=null;
    static final Color PalagoMouseColors[][] = {
    		
    		{Color.white,new Color(0.4f,0.4f,1.0f)},
    		{new Color(1.0f,1.0f,0.6f),Color.red},
    		{Color.yellow,Color.blue}
    };
    static final Color PalagoMouseDotColors[][] = {
    		{Color.black,Color.white},
    		{Color.black,Color.black},
    		{Color.black,Color.white}
    };
    	
    void setMouseDotColors()
    {  	MouseColors = PalagoMouseColors[tileColorSet];
    	MouseDotColors = PalagoMouseDotColors[tileColorSet];
    }    
    // private state
    private PalagoBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private final double INITIAL_TILE_SCALE = 3.0;

    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle logoRects[] = addRect("logo",2);
    private Rectangle chipRect = addRect("firstPlayerChipRect");
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);

	private Image textures[] = null;

    public void preloadImages()
    {	PalagoChip.preloadImages(loader,ImageDir);
    	if(textures==null)
    	{
            textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = PalagoChip.getChip(6).image;
    }

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

        setMouseDotColors();
        // use_grid=reviewer;// use this to turn the grid letters off by default
        colorOption = myFrame.addChoiceMenu(s.get(TileColorMessage),deferredEvents);
        aquaOption = new JMenuItem(s.get(AquaColor));
        aquaOption.addActionListener(deferredEvents);
        redOption = new JMenuItem(s.get(RedColor));
        redOption.addActionListener(deferredEvents);
        blueOption = new JMenuItem(s.get(BlueColor));
        blueOption.addActionListener(deferredEvents);
        
        colorOption.add(aquaOption);
        colorOption.add(redOption);
        colorOption.add(blueOption);
       
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),PalagoId.ZoomSlider);
        zoomRect.min=1.0;
        zoomRect.max=5.0;
        zoomRect.value=INITIAL_TILE_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = HighlightColor;   
        labelColor = Color.red;
        labelFont = largeBoldFont();
        bb = new PalagoBoard(info.getString(OnlineConstants.GAMETYPE, "Palago"),getStartingColorMap());
        useDirectDrawing(true); // not tested yet
        doInit(false);
    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        
        
   	 	if(!preserve_history) 
   	 		{ zoomRect.setValue(INITIAL_TILE_SCALE);
   	 		  board_center_x =  board_center_y = 0; 
   	 		}
 
        bb.doInit(bb.gametype);						// initialize the board
        if(!preserve_history)
    	{ 
        	startFirstPlayer();
    	}

    }
    

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = logoRects[player];
    	int chipW = unitsize*4;
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW, chipW);
    	G.SetRect(done, x, y+chipW, chipW, plannedSeating()?chipW/2:0);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
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
    	int fh = standardFontSize();
    	int minLogW = fh*15;	
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
     	CELLSIZE = fh*2;
        
    	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.7,	// 80% of space allocated to the board
    			1,		// aspect ratio for the board
    			fh*3,	// maximum cell size
    			0.5		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);

       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
   	
    	{
    	int chipW = CELLSIZE*3;
    	int chipH = CELLSIZE*12;
    	// place either horizontally or vertically
    	layout.placeRectangle(chipRect,
    			chipW,chipH,chipW,chipH,
    			chipH,chipW,chipH,chipW,
    			BoxAlignment.Center,true);
    	}

    	Rectangle main = layout.getMainRectangle();
    	int boardX = G.Left(main);
    	int boardY = G.Top(main)+CELLSIZE;
    	int boardW = G.Width(main);
    	int boardH = G.Height(main)-CELLSIZE*2;
        
      	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-CELLSIZE;
        int stateX = boardX;
        int stateH = CELLSIZE;
        int zoomW = CELLSIZE*5;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,noChatRect);
        G.placeRight(stateRect, zoomRect, zoomW);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white); 	
         }
 
         
    private void DrawLogo(Graphics gc,Rectangle r,int player)
    {	if(gc!=null)
    	{
    	Color back = PalagoChip.ChipColor[getAltChipset()][bb.getColorMap()[player]];
    	int width = G.Width(r);
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	int w = (int)(width*0.8);
    	GC.setColor(gc,back);
    	GC.fillOval(gc,cx-w/2,cy-w/2,w,w);
    	PalagoChip.logo.drawChip(gc,this,w,cx,cy,null);
    	}
    }

	// draw a box of spare chips. It's purely for visual effect.
    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,PalagoBoard gb)
    {
        boolean canhit = gb.LegalToHitChips() && G.pointInRect(highlight, r);
        int h = G.Height(r);
        int w = G.Width(r);
        boolean vertical = h>w;
        int xstep = vertical ? 0 : w/3;
        int ystep = vertical ? h/3 : 0;
        int size = (int)(Math.min(h,w)*0.8);
        for(int i=0,
        		y=G.Top(r)+(vertical ? ystep/2 : h/2),
        		x=G.Left(r)+(vertical? w/2 : xstep/2);
        	i<PalagoChip.nChips;
        	i++,y+=ystep,x+=xstep)
        {	PalagoCell c = gb.chipPool[i];
        	c.drawChip(gc,this,canhit?highlight:null,size,x,y,null);
        }
        if(canhit && (highlight.hitObject!=null))
        {	highlight.arrow = (gb.pickedObject==null)?StockArt.UpArrow:StockArt.DownArrow;
        	highlight.awidth = size/2;
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
    {	boolean inboard = boardRect.contains(xp,yp);
   		int cellS = inboard? bb.cellSize() : Math.min(G.Height(chipRect),G.Width(chipRect));
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
    	PalagoChip.getChip(obj).drawChip(g,this,cellS, xp, yp, null);
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
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
         textures[review ? BACKGROUND_REVIEW_INDEX:BACKGROUND_TABLE_INDEX].tileImage(gc,
          		boardRect); 
      GC.frameRect(gc,Color.black,boardRect); 
   	
       
      
       // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      //gb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

      // draw the tile grid.  The positions are determined by the underlying board
      // object, and the tile itself if carefully crafted to tile the board
      // when drawn this way.  For games with simple graphics, we could use the
      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
      // but for more complex graphics with overlapping shadows or stacked
      // objects, this double loop is useful if you need to control the
      // order the objects are drawn in.
 
    }
    public void ShowDirections(Graphics gc,PalagoBoard gb,int xpos,int ypos,PalagoCell c)
    {
    	if(gc!=null)
    	{
    		int xp0 = gb.cellToX(c);
    		int yp0 = gb.cellToY(c);
		   	for(int i =0;i<PalagoBoard.CELL_FULL_TURN();i++)
		   	{
		   		PalagoCell nc = c.exitTo(i);
		   		if(nc!=null)
		   		{
		   		int xp = gb.cellToX(nc);
		   		int yp = gb.cellToY(nc);
		   		GC.Text(gc,false,xpos+xp-xp0,ypos+yp-yp0,10,10,Color.black,null,""+i);
		   		}
		   	}}
    }

   /* draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */

    private void drawBoardElements(Graphics gc, PalagoBoard gb, Rectangle tbRect,
    		HitPoint ourTurnSelect,HitPoint anySelect)
    {	
       Rectangle oldClip = GC.combinedClip(gc,tbRect);
       int cellSize = gb.cellSize();
       boolean draggingBoard = draggingBoard(); 
 	   boolean canHit = !draggingBoard && G.pointInRect(ourTurnSelect,tbRect);
    	
    	//
        // now draw the contents of the board and anything it is pointing at
        //
        Hashtable<PalagoCell,PalagoCell> dests = gb.movingObjectDests();
        PalagoCell sourceCell = gb.getSource();
        PalagoCell destCell = gb.getDest();
        int left = G.Left(tbRect);
        int top = G.Bottom(tbRect);
        for(Enumeration<PalagoCell> cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements(); )
            { //where we draw the grid
        	PalagoCell ccell = cells.nextElement();
            int xpos = left + gb.cellToX(ccell);
            int ypos = top - gb.cellToY(ccell);     
                                
                boolean isADest = dests.get(ccell)!=null;
                boolean isASource = (ccell==sourceCell)||(ccell==destCell);
                boolean canHitThis = canHit && gb.LegalToHitBoard(ccell);
                String labl = use_grid ? ccell.cellName : "";//+ccell.col+ccell.row;
                if(G.debug() && !use_grid) { labl+=""+ccell.col+ccell.row; }
                if(ccell.drawChip(gc,this,canHitThis?ourTurnSelect:null,cellSize,xpos,ypos,labl))
                {
                boolean isEmpty = ccell.topChip()==null;
                if(!isEmpty && !G.pointInsideSquare(ourTurnSelect, ourTurnSelect.hit_x,ourTurnSelect.hit_y, cellSize/3))
                {	ourTurnSelect.hitCode = PalagoId.RotateTile;
                	ourTurnSelect.arrow = StockArt.Rotate_CW;
                	ourTurnSelect.spriteColor = Color.blue;
                	ourTurnSelect.awidth = 2*cellSize/3;
                }
                else
                {
                ourTurnSelect.hitCode = isEmpty?PalagoId.EmptyBoard:PalagoId.BoardLocation;
                ourTurnSelect.arrow = isEmpty?StockArt.DownArrow:StockArt.UpArrow;
                ourTurnSelect.awidth = cellSize/2; 
                ourTurnSelect.spriteColor = Color.red;
                }
               }
                if (gc != null)
                {
                if(ccell==destCell) 
                {
                PalagoChip.blank.drawChip(gc,this,cellSize,xpos,ypos,null);
                }
                if(isASource)
                	{GC.cacheAACircle(gc,xpos,ypos,2,Color.green,Color.yellow,true);
                	} else
                if(isADest)
                	{GC.cacheAACircle(gc,xpos,ypos,2,Color.red,Color.yellow,true);
                	}
            }
        }
        doBoardDrag(tbRect,anySelect,cellSize,PalagoId.InvisibleDragBoard);
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
    {  PalagoBoard gb = disB(gc);
       PalagoState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
      	if(gc!=null)
       	{
       	// note this gets called in the game loop as well as in the display loop
       	// and is pretty expensive, so we shouldn't do it in the mouse-only case
           gb.SetDisplayParameters(zoomRect.value,1.0,board_center_x,board_center_y,30.0); // shrink a little and rotate 30 degrees
           gb.SetDisplayRectangle(boardRect);
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
       
       redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,nonDragSelect);
       DrawChipPool(gc, chipRect, FIRST_PLAYER_INDEX, ourTurnSelect,gb);
       boolean planned = plannedSeating();
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
       {
    	 commonPlayer cpl = getPlayerOrTemp(i);
    	 cpl.setRotatedContext(gc, selectPos, false);
    	 DrawLogo(gc,logoRects[i],i);
    	 if(planned && (i==gb.whoseTurn))
    	 {
    		 handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
 					HighlightColor, rackBackGroundColor);
    	 }
    	 cpl.setRotatedContext(gc, selectPos, true);
       }
       
       zoomRect.draw(gc,nonDragSelect);
       
       GC.setFont(gc,standardBoldFont());
       
       drawPlayerStuff(gc,(state==PalagoState.PUZZLE_STATE),nonDragSelect,
	   			HighlightColor, rackBackGroundColor);

		if (state != PalagoState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}	
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos, HighlightColor, rackBackGroundColor);
     
            }


		// draw the avatars
        standardGameMessage(gc,messageRotation,
        		state==PalagoState.GAMEOVER_STATE?gameOverMessage():s.get(state.getDescription()),
        				state!=PalagoState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        DrawLogo(gc,iconRect,gb.whoseTurn);
        goalAndProgressMessage(gc,nonDragSelect,s.get("Form a closed shape in your color"),progressRect, goalRect);
        //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for games with no possible repetition
        
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
		lastDropped = bb.lastDroppedDest;	// this is for the image adjustment logic
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
        return (new Palagomovespec(st, player));
    }
/**
 * prepare to add nmove to the history list, but also edit the history
 * to remove redundant elements, so that indecisiveness by the user doesn't
 * result in a messy game log.
 * This may require that move be merged with an existing history move
 * and discarded.  Return null if nothing should be added to the history
 * One should be very cautious about this, only to remove real pairs that
 * result in a null move.
 * 
 */
    public commonMove EditHistory(commonMove nmove)
    {	
    	int sz = History.size()-1;
    	boolean same = false;
    	if((nmove.op==MOVE_DROPB)
    			&& (sz>=0))
    		{
    			Palagomovespec prev = (Palagomovespec)History.elementAt(sz);
    			Palagomovespec newmove = (Palagomovespec)nmove;
    			if((prev.op==MOVE_DROPB)
    				&& (prev.to_col==newmove.to_col)
    				&& (prev.to_row==newmove.to_row))
    			{	// rotations are accomplished by dropping a new tile in the same position
    				popHistoryElement();
    				same=true;
    			}
    		}
    	if(nmove.op==MOVE_DROP) { same = true; }	// some damaged game records have a naked drop in them
    	return(EditHistory(nmove,same));
    }
  

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof PalagoId) // not dragging anything yet, so maybe start
        {

        PalagoId hitObject = (PalagoId) hp.hitCode;
        PalagoCell c = hitCell(hp);
 	    switch(hitObject)
	    {
	    default: break;
	    
        case InvisibleDragBoard:
        	    
			break;
		case ChipPool:
	    	if(c!=null)
	    		{ PalagoChip top = c.topChip();
	    		  if(top!=null) { PerformAndTransmit("Pick "+top.index); }
	    		}
	    	break;
	    case BoardLocation:
	    	if((c!=null) && c.topChip()!=null)
	    	{
	    	PerformAndTransmit("Pickb "+c.col+" "+c.row);
	    	}
	    	break;
        }
         }
    }
	private void doDropChip(char col,int row,PalagoChip ch)
	{	PalagoState state = bb.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case PUZZLE_STATE:
		{
		PerformAndTransmit("dropb "+col+" "+row+" "+ch.index);
		}
		break;
		case CONFIRM_STATE:
        case CONFIRM2_STATE:
		case PLAY2_STATE:
		case PLAY_STATE:
				PerformAndTransmit("dropb "+col+" "+row+" "+ch.index);
			break;
 		}
	}
	private void doDropChip(char col,int row)
	{
		PalagoChip mo = bb.pickedObject;
		if(mo==null) { mo=bb.lastPicked; }
		if(mo==null) { mo=PalagoChip.getChip(0); }
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
        if(!(id instanceof PalagoId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
   		PalagoId hitCode = (PalagoId)hp.hitCode;
        PalagoCell hitObject = bb.getCell(hitCell(hp));
        PalagoState state = bb.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
         case InvisibleDragBoard:
        case ZoomSlider:
        	break;
        case RotateTile:
			switch(state)
			{
			default: throw G.Error("Not expecting rotate in state %s",state);
			case CONFIRM_STATE:
	        case CONFIRM2_STATE:
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
				//$FALL-THROUGH$
			case PUZZLE_STATE:
				PalagoCell ce = hitObject;
				PalagoChip ch = ce.topChip();
				doDropChip(hitObject.col,hitObject.row,
						PalagoChip.getChip((ch.index+1)%PalagoChip.nChips));
			}
			break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PLAY2_STATE:
			case PUZZLE_STATE:
				if(!bb.isDest(hitObject))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					doDropChip(hitObject.col,hitObject.row);
					break;
					}
				// fall through and pick up the previously dropped piece
				PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
				break;
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
				case PUZZLE_STATE:
					doDropChip(hitObject.col,hitObject.row);
					break;
			}
			break;
			
        case ChipPool:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop");
			}
           break;
        }}

    }


    // return what will be the init type for the game
    public String gameType() { return(bb.gametype); }	// this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Palago_SGF); }	// this is the official SGF number assigned to the game

    
    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        bb.doInit(token);
    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
        if(handled) {}
        else if(target==colorOption) {	handled=true;    }
        else if(target==aquaOption) { handled=true;tileColorSet = 0;  }
        else if(target==redOption) { handled=true;tileColorSet = 1; }
        else if(target==blueOption) { handled=true;tileColorSet = 2; }
        setMouseDotColors();
 
        return (handled);
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
    {  
    	return(new PalagoPlay());
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
}
