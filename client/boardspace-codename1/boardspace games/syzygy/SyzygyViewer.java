package syzygy;

import com.codename1.ui.geom.Rectangle;
import bridge.Color;
import online.common.*;
import java.util.*;
import lib.*;
import online.game.*;
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
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

/**
 * 
 * Change History
 */
public class SyzygyViewer extends CCanvas<SyzygyCell,SyzygyBoard> implements SyzygyConstants, GameLayoutClient
{	
    static final String Syzygo_SGF = "Syzygy"; // sgf game number allocated for Cookie Disco

    
    // file names for jpeg images and masks
    static final String ImageDir = "/syzygy/images/";
	
    
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BACKGROUND_INDEX = 2;
    static final int ICON_INDEX = 3;
    static final String TextureNames[] = { 
    			"starfield" ,
    			"starfield-red",
    			"starfield-gray",
    			"syzygy-icon-nomask",
    		};


     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(176,185,225);
    private Color boardBackgroundColor = new Color(176,185,225);
    
    private Color veryLightGray  = new Color(0.8f,0.8f,0.8f);
   
    // private state
    private SyzygyBoard bb = null; //the board from which we are displaying
    private final double INITIAL_TILE_SCALE = 3.0;

    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle repRect = addRect("repRect");	// Not needed for games with no possible repetition
    private Rectangle chipRect = addRect("samplechips");

    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);

	private Image textures[] = null;

    public synchronized void preloadImages()
    {	SyzygyChip.preloadImages(loader,ImageDir);
    	if(textures==null)
    	{
            textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = textures[ICON_INDEX];
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	enableAutoDone = true;
        super.init(info,frame);
        use_grid = false;
        gridOption.setState(false);

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),SyzId.ZoomSlider);
        zoomRect.min=1.0;
        zoomRect.max=5.0;
        zoomRect.value=INITIAL_TILE_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = HighlightColor;   
        labelColor = Color.red;
        labelFont = largeBoldFont();
        
        bb = new SyzygyBoard(info.getString(OnlineConstants.GAMETYPE, "syzygy"));
        useDirectDrawing(true);
        doInit(false,randomKey);
    }
    public void doInit(boolean preserve)
    {	doInit(preserve,bb.randomKey);
    }
    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history,long rand)
    {	
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        
        bb.doInit(bb.gametype,rand);						// initialize the board
      
   	 	if(!preserve_history) 
   	 		{ zoomRect.setValue(INITIAL_TILE_SCALE);
   	 		  board_center_x =  0.0; 
   	 		  board_center_y = -1.0;
   	 		  startFirstPlayer();
   	 		}
 
     }
	public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit) 
	{	commonPlayer pl = getPlayerOrTemp(player);
		Rectangle box = pl.createRectangularPictureGroup(x,y,2*unit/3);
		Rectangle doneRect = doneRects[player];
		G.SetRect(doneRect, G.Right(box)+unit/4, y, unit*3, plannedSeating()?unit*3/2:0);
	    G.union(box, doneRect);
	    pl.displayRotation = rotation;
	    return(box);
    }
    	
    public void setLocalBounds(int x, int y, int width, int height)
    {	double vs[] = { -1, 1};
    	setLocalBoundsV(x,y,width,height,vs);
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double v)
    {   G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
    	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*15;	
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int CELLSIZE=(int)(fh*2.5);
    	int C2 = CELLSIZE/2;
        int margin = fh/2;
        boolean vertical = v>0;
 
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	// margins for allocated boxes
    			0.6,	// 60% of space allocated to the board
    			1.0,	// 1.0:1 aspect ratio for the board
    			fh*4,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
   	
      	
       	layout.placeRectangle(chipRect,
       							vertical ? CELLSIZE*3 : CELLSIZE*12,
       							vertical ? CELLSIZE*12 : CELLSIZE*3,BoxAlignment.Center);
    	layout.placeDoneEditRep(CELLSIZE*3, CELLSIZE*3, doneRect, editRect, repRect);
  	
         //  SetupVcrRects(0,0,10,5);        	
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
        int zoomW = CELLSIZE*7;

        G.placeRow( boardX,stateY,boardW ,stateH,stateRect,noChatRect);
        G.placeRight(stateRect, zoomRect, zoomW);

    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	SetupVcrRects(boardX+C2/2,boardBottom-C2/2-minLogW/2,minLogW,minLogW/2);
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);

        return boardW*boardH;
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
   		SyzygyChip chip = SyzygyChip.getChip(obj%100);
   		int left = G.Left(boardRect);
   		int top = G.Bottom(boardRect);
     	if(obj>100)
     		{ SyzygyChip second = SyzygyChip.getChip(obj/100);
     		  int yp1 = top-yp;
     		  SyzygyCell close = bb.closestCell(xp,yp1);
     		  if(close!=null && (close.topChip()!=null))
     		  {	SyzygyCell src = bb.getSource();
     		  	if(src!=null)
     		  	{	
     		  		int xp2 = left + bb.cellToX(src);
     		  		int yp2 = top - bb.cellToY(src);
     		  		chip.drawChip(g,this,cellS, xp2,yp2, null);
     		  		GC.cacheAACircle(g,xp2,yp2,2,Color.green,Color.yellow,true);
     		  	}
     			 
     		  }
     		  else
     		  {
     		  	chip.drawChip(g,this,cellS, xp, yp, null);
     		  }
     	      
        	  second.drawChip(g,this,cellS, xp, yp, null);
     		}
     		else
     		{ chip.drawChip(g,this,cellS, xp, yp, null);
     		}

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
     textures[BACKGROUND_INDEX].tileImage(gc, fullRect);   
         textures[review ? BACKGROUND_REVIEW_INDEX:BACKGROUND_TILE_INDEX].tileImage(gc,
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
    public void ShowDirections(Graphics gc,SyzygyBoard gb,int xpos,int ypos,SyzygyCell c)
    {
    	if(gc!=null)
    	{
    		int xp0 = gb.cellToX(c.col,c.row);
    		int yp0 = gb.cellToY(c.col,c.row);
		   	for(int i =0;i<SyzygyBoard.CELL_FULL_TURN();i++)
		   	{
		   		SyzygyCell nc = c.exitTo(i);
		   		if(nc!=null)
		   		{
		   		int xp = gb.cellToX(nc.col,nc.row);
		   		int yp = gb.cellToY(nc.col,nc.row);
		   		GC.Text(gc,false,xpos+xp-xp0,ypos+yp-yp0,10,10,Color.black,null,""+i);
		   		}
		   	}}
    }

   /* draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */

    private void drawBoardElements(Graphics gc, SyzygyBoard gb, Rectangle tbRect,
    		HitPoint ourTurnSelect,HitPoint anySelect)
    {	
       Rectangle oldClip = GC.combinedClip(gc,tbRect);
       int cellSize = gb.cellSize();
       boolean draggingBoard = draggingBoard();
  	   boolean canHit = !draggingBoard && G.pointInRect(ourTurnSelect,tbRect);
    	//
        // now draw the contents of the board and anything it is pointing at
        //
        Hashtable<SyzygyCell,SyzygyMovespec> dests = gb.movingObjectDests();
        SyzygyCell sourceCell = gb.getSource();
        SyzygyCell destCell = gb.getDest();
        if(ourTurnSelect!=null) {  ourTurnSelect.awidth = cellSize; }
        int left = G.Left(tbRect);
        int top = G.Bottom(tbRect);
        for(Enumeration<SyzygyCell>cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements();)
        { 
        	SyzygyCell ccell = cells.nextElement();
            int xpos = left + gb.cellToX(ccell);
            int ypos = top - gb.cellToY(ccell);
                                
                boolean isADest = dests.get(ccell)!=null;
                boolean isASource = (ccell==sourceCell)||(ccell==destCell);
                boolean canHitThis = canHit && gb.LegalToHitBoard(ccell);
                //String labl = "";//+ccell.col+ccell.row;
                if(ccell.drawStack(gc,canHitThis?ourTurnSelect:null,
            			xpos,ypos,
            			this,0,cellSize,1.0,use_grid?(""+ccell.col+ccell.row):null ))
                {	 //if(gc!=null) { gc.drawOval(xpos-cellSize/2,ypos-cellSize/2,cellSize,cellSize); }
                    ourTurnSelect.hitCode = SyzId.BoardLocation;
                    ourTurnSelect.awidth = cellSize/2;
                    ourTurnSelect.spriteColor = Color.red;
                     
                //	for(int i=0;i<6;i++) 
            	//	{ 
            	//	CookieCell c = ccell.exitTo(i);
                //   int ypos1 = gb.cellToY(c.col, c.row);
                //    int xpos1 = gb.cellToX(c.col, c.row);
                //    ypos1 = (tbRect.y + tbRect.height) - ypos1;
                //    xpos1 += tbRect.x;
                //    G.Text(gc,true,xpos1-30,ypos1-10,60,20,Color.black,null,""+c.col+c.row+"("+i+")");
            	//	}
                   canHit = false;
                }
                
                if(gb.cherryCell==null) {}
                else if((ccell==gb.cherryCell) && (ccell.activeAnimationHeight()==0)) // single cherry on the locked color
            	{
            	SyzygyChip.Asteroid.drawChip(gc,this,cellSize/2,xpos,ypos,null);
            	}
                
                if (gc != null)
                {
                //G.DrawAACircle(gc,xpos,ypos,1,tiled?Color.green:Color.blue,Color.yellow,true);
                //if(cell.topChip()!=null) { ShowDirections(gc,gb,xpos,ypos,cell); }
                if(isASource)
                	{GC.cacheAACircle(gc,xpos,ypos,2,Color.green,Color.yellow,true);
                	} else
                if(isADest)
                	{GC.cacheAACircle(gc,xpos,ypos,3,Color.red,Color.yellow,true);
                	}
                //if(ccell.topChip()==null) { G.Text(gc,true,xpos-CELLSIZE*2,ypos-CELLSIZE*2,CELLSIZE*4,CELLSIZE*4,Color.white,null,""+ccell.col+ccell.row); }
  
                }
        }
        doBoardDrag(tbRect,anySelect,cellSize,SyzId.InvisibleDragBoard);        
 		GC.setClip(gc,oldClip);
    }

	// draw a box of spare chips. It's purely for visual effect.
    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,SyzygyBoard gb)
    {	int w = G.Width(r);
    	int h = G.Height(r);    	
        boolean vertical = h>w;
        int ystep = vertical ? h/4 : 0;
        int xstep = vertical ? 0 : w/4 ;
        int xp = vertical ? G.centerX(r) : G.Left(r)+xstep/2;
        int yp = vertical ? G.Top(r)+ystep/2 : G.centerY(r);
        int sz = (vertical?w:h)*8/10;
        for(int i=0,y=yp,x=xp;
        	i<SyzygyChip.nChips;
        	i++,y+=ystep,x+=xstep)
        {	SyzygyChip chip = SyzygyChip.getChip(i);
         	labelColor = Color.white;
         	GC.setFont(gc,largeBoldFont());
        	chip.drawChip(gc,this,sz,x,y,""+chip.value);
        }
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
    {  SyzygyBoard gb = disB(gc);
	if(gc!=null)
	{
	// note this gets called in the game loop as well as in the display loop
	// and is pretty expensive, so we shouldn't do it in the mouse-only case
	gb.SetDisplayParameters(zoomRect.value,1.0,board_center_x,board_center_y,30.0); // shrink a little and rotate 30 degrees
    gb.SetDisplayRectangle(boardRect);
	}
       SyzygyState state = gb.getState();
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
       
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, veryLightGray,boardBackgroundColor,standardBoldFont(),standardPlainFont());
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,nonDragSelect);
       
       
       DrawChipPool(gc, chipRect, FIRST_PLAYER_INDEX, ourTurnSelect,gb);
       GC.fillRect(gc,Color.gray,zoomRect);
       zoomRect.draw(gc,nonDragSelect);
       
       boolean planned = plannedSeating();
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
       {
    	 commonPlayer cpl = getPlayerOrTemp(i);
    	 cpl.setRotatedContext(gc, selectPos, false);
    	 if(planned && (i==gb.whoseTurn))
    	 {
    		 handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
 					HighlightColor, rackBackGroundColor);
    	 }
    	 cpl.setRotatedContext(gc, selectPos, true);
       }
   
       GC.setFont(gc,standardBoldFont());
     
       drawPlayerStuff(gc,(state==SyzygyState.PUZZLE_STATE),nonDragSelect,
	   			HighlightColor, rackBackGroundColor);

		double messageRotation = pl.messageRotation();

		if (state != SyzygyState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive())
				{ handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
			}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos, HighlightColor, rackBackGroundColor);
            }

        // draw the avatars
        	GC.setColor(gc,Color.white);
            standardGameMessage(gc,messageRotation,
            		Color.yellow,
            		state==SyzygyState.GAMEOVER_STATE?gameOverMessage():s.get(state.getDescription()),
            				state!=SyzygyState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
            goalAndProgressMessage(gc,nonDragSelect,Color.yellow,s.get(GoalString),progressRect, goalRect);
            DrawRepRect(gc,messageRotation,veryLightGray,gb.Digest(),repRect);	// Not needed for games with no possible repetition
       
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
        bb.animationStack.clear();
        
        handleExecute(bb,mm,replay);
        int siz = bb.cellSize();
        startBoardAnimations(replay,bb.animationStack,siz,MovementStyle.Chained);
      
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
        return (new SyzygyMovespec(st, player));
    }

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof SyzId)// not dragging anything yet, so maybe start
        {

       	SyzId hitObject =  (SyzId)hp.hitCode;
        SyzygyCell c = hitCell(hp);
 	    switch(hitObject)
	    {
	    default: break;
	    
        case InvisibleDragBoard:
        	
			break;
		case ChipPool:
	    	if(c!=null)
	    		{ SyzygyChip top = c.topChip();
	    		  if(top!=null) { PerformAndTransmit("Pick "+SyzygyMovespec.D.findUnique(SyzId.CHIP_OFFSET.ordinal()+top.index)); }
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
	private void doDropChip(char col,int row,SyzygyChip ch)
	{	
		PerformAndTransmit("dropb "+col+" "+row+" "+ch.index);
	}
	private void doDropChip(char col,int row)
	{
		SyzygyChip mo = bb.pickedObject;
		if(mo!=null) { doDropChip(col,row,mo); }
	}
	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof SyzId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    		missedOneClick = false;
    		SyzId hitCode = (SyzId)hp.hitCode;
        SyzygyCell hitObject = hitCell(hp);
        SyzygyState state = bb.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case InvisibleDragBoard:
        case ZoomSlider:
        	break;

        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
				if(bb.pickedObject!=null)
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
			
        case ChipPool:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+SyzygyMovespec.D.findUnique(hitObject.row));
			}
           break;
        }
         }

     }

    // return what will be the init type for the game
    public String gameType() { return(bb.gametype+" "+bb.randomKey); }	// this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Syzygo_SGF); }	// this is the official SGF number assigned to the game

    
    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	long rand = G.LongToken(his);
        bb.doInit(token,rand);
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
    	return(new SyzygyPlay());
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
            {	StringTokenizer tok = new StringTokenizer(value);
            	String init = tok.nextToken();
            	long rv = G.LongToken(tok);
                bb.doInit(init,rv);
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
