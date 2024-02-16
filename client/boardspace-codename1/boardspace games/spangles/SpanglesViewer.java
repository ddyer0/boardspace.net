/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package spangles;

import com.codename1.ui.geom.Rectangle;
import bridge.*;
import common.GameInfo;
import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import online.game.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

/**
 * Change History
 */
public class SpanglesViewer extends CCanvas<SpanglesCell,SpanglesBoard> implements SpanglesConstants
{	static final long serialVersionUID = 1000;
     // colors
    private Color reviewModeBackground = new Color(120,175,147);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(120,175,147);
    private Color boardBackgroundColor = new Color(120,175,147);
    
 
    
    // private state
    private SpanglesBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private final double INITIAL_TILE_SCALE = 3.0;

    private Rectangle chipRects[] = addRect("chjp",2);

    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);

	private static Image textures[] = null;
	public commonMove EditHistory(commonMove nmove)
	{
		boolean oksame = nmove.op==MOVE_DROP;	// some damaged games have naked drops
		return EditHistory(nmove,oksame);
	}
    public synchronized void preloadImages()
    {	
    	if(textures==null)
    	{
            SpanglesChip.preloadImages(loader,ImageDir);
            textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = textures[ICON_INDEX];
    }
    Color SpanglesMouseColors[] = { Color.blue,Color.yellow };
    Color SpanglesMouseDotColors[] = { Color.white,Color.black};
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

        // use_grid=reviewer;// use this to turn the grid letters off by default
        MouseColors = SpanglesMouseColors;
        MouseDotColors = SpanglesMouseDotColors;
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),SpanglesId.ZoomSlider);
        zoomRect.min=2.0;
        zoomRect.max=5.0;
        zoomRect.value=INITIAL_TILE_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = HighlightColor;       
        bb = new SpanglesBoard(info.getString(GameInfo.GAMETYPE, Spangles_INIT),
        		getStartingColorMap());
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
       	board_center_x = board_center_y = 0.0; 
       	startFirstPlayer();
   	}
   }
   

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*7;
    	int chipH = unitsize*4;
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW, chipH);
    	G.SetRect(done, x, y+chipH, chipW, plannedSeating()?chipW/2:0);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize+unitsize/2);
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
    	int margin = fh/2;
    	layout.strictBoardsize = false;
    	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.6,	// 80% of space allocated to the board
    			1,		// aspect ratio for the board
    			fh*2,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
     	int minLogW = fh*15;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
     	CELLSIZE = fh*2;
        int C2 = CELLSIZE/2;
        
        

        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	
       	layout.placeDoneEdit(buttonW,buttonW*3/2,doneRect,editRect);

    	Rectangle main = layout.getMainRectangle();
        int stateH = fh*5/2;

    	int boardX = G.Left(main);
    	int boardY = G.Top(main)+stateH;
    	int boardW = G.Width(main);
    	int boardH = G.Height(main)-stateH*2;
         	
      	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH;
        int stateX = boardX;
        int zoomW = CELLSIZE*5;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,noChatRect);
        G.placeRight(stateRect, zoomRect, zoomW);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	SetupVcrRects(boardX+C2/2,boardBottom-minLogW/2-C2/2,minLogW,minLogW/2);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
    }


	// draw a box of spare chips. It's purely for visual effect.
    private void DrawChipPool(Graphics gc, Rectangle r, HitPoint highlight,SpanglesBoard gb,int player)
    {	boolean canhit = G.pointInRect(highlight, r);
    	int h = G.Height(r);
    	int w = G.Width(r);
    	int t = G.Top(r);
    	int l = G.Left(r);
        for(int i=0,xstep=w/3,y=t+h/2,x=l+xstep;
        	i<2;
        	i++,x+=xstep)
        {  
        	SpanglesCell c = gb.rack[player][i];
        	if(c.drawChip(gc,this,(canhit && gb.LegalToHitChips(player))?highlight:null,
        			h/2,x,y+h/8+(((i&1)==1)?-h/4:0),
        			null))
        	{
        		highlight.arrow = (gb.pickedObject==null)?StockArt.UpArrow:StockArt.DownArrow;
            	highlight.awidth = h/3;
            	highlight.spriteColor = Color.red;	
        }
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
   		int cellS = inboard? bb.cellSize() : CELLSIZE*2 ;
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
    	SpanglesChip.getChip(obj).drawChip(g,this,cellS, xp, yp, null);
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

    
   /* draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */
    private void drawBoardElements(Graphics gc, SpanglesBoard gb, Rectangle tbRect,
    		HitPoint ourTurnSelect,HitPoint anySelect)
    {	
       Rectangle oldClip = GC.combinedClip(gc,tbRect);
       int cellSize = gb.cellSize();
       boolean draggingBoard = draggingBoard();

    	//
        // now draw the contents of the board and anything it is pointing at
        //
        Hashtable<SpanglesCell,SpanglesCell> dests = gb.movingObjectDests();
        SpanglesCell sourceCell = gb.pickedSource; 
        SpanglesCell destCell = gb.droppedDest;

   	 	int left = G.Left(tbRect);
   	 	int top = G.Bottom(tbRect);
   	 	for(Enumeration<SpanglesCell>cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements();)
            { //where we draw the grid
   	 		 SpanglesCell cell = cells.nextElement();
   	 		 int xpos = left + gb.cellToX(cell);
   	 		 int ypos = top - gb.cellToY(cell);
                boolean isADest = dests.get(cell)!=null;
                boolean isASource = (cell==sourceCell)||(cell==destCell);
                SpanglesChip piece = cell.topChip();
                boolean isSource = gb.isSource(cell);
                boolean hitpoint = !draggingBoard
                	&& gb.LegalToHitBoard(cell)
                	&& cell.closestPointToCell(ourTurnSelect,cellSize*2, xpos, ypos) 
                	;
                
                 boolean drawhighlight = hitpoint || isSource || gb.isDest(cell) ;
 
                // drawing
                if (hitpoint)
                	{
                  	 //if(gc!=null) { gc.drawOval(xpos-cellSize/2,ypos-cellSize/2,cellSize,cellSize); }
                	boolean isEmpty = (cell.topChip()==null);
                    ourTurnSelect.hitCode = isEmpty?SpanglesId.EmptyBoard:SpanglesId.BoardLocation;
                    ourTurnSelect.arrow = isEmpty?StockArt.DownArrow:StockArt.UpArrow;
                    ourTurnSelect.awidth = cellSize/2;
                    ourTurnSelect.spriteColor = Color.red;
                }

                if (gc != null)
                {
                if(piece!=null)
                {	//SpanglesCell et = (SpanglesCell)cell.exitToward(2);
                	//String ets = (et==null) ? "" : ("->"+et.col+et.row);
                	// ""+cell.col+cell.row+ets
                	String gridmsg = use_grid ? G.printCol(cell.col)+cell.row : null;
                	GC.setColor(gc,Color.black);
                	piece.drawChip(gc,this,cellSize,xpos,ypos,gridmsg); 
                    if (drawhighlight)
                    { // checking for pointable position
                      //  drawChip(gc, SELECTION_INDEX, xpos, ypos, cellSize, 1.0,0.0);
                    }

                }
                if(isASource)
                {GC.cacheAACircle(gc,xpos,ypos,2,Color.green,Color.yellow,true);
                } else
                if(isADest)
                {GC.cacheAACircle(gc,xpos,ypos,2,Color.red,Color.yellow,true);
 
                }
        //if(G.debug() && (cell.topChip()==null))
        //{	// draw a grid of other cells
        //	GC.Text(gc,true,xpos-CELLSIZE/2,ypos-CELLSIZE/2,CELLSIZE,CELLSIZE,null,null,""+G.printCol(cell.col)+cell.row);
        //}
        // the structure of the board is unusual.  This adds an arrow
        // that shows the pointer to adjacent to help debugging
        //SpanglesCell d = cell.exitTo(2);
        //if(cell.topChip()!=null) { GC.drawArrow(gc,xpos,ypos,left+gb.cellToX(d),top-gb.cellToY(d),3,3);}

                                }}
   	 	doBoardDrag(tbRect,anySelect,cellSize,SpanglesId.InvisibleDragBoard);
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
    {  SpanglesBoard gb = disB(gc);
	if(gc!=null)
	{
	// note this gets called in the game loop as well as in the display loop
	// and is pretty expensive, so we shouldn't do it in the mouse-only case
    	gb.SetDisplayParameters(zoomRect.value,1.0,board_center_x,board_center_y,0.0); // shrink a little and rotate 30 degrees
    	gb.SetDisplayRectangle(boardRect);
	}
       SpanglesState state = gb.getState();
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
       
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,nonDragSelect);
       
       boolean planned = plannedSeating();
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
       {
    	 commonPlayer cpl = getPlayerOrTemp(i);
    	 cpl.setRotatedContext(gc, selectPos, false);
    	 DrawChipPool(gc,chipRects[i],ourTurnSelect,gb,i);
    	 if(planned && (i==gb.whoseTurn))
    	 {
    		 handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
 					HighlightColor, rackBackGroundColor);
    	 }
    	 cpl.setRotatedContext(gc, selectPos, true);
       }
      
      zoomRect.draw(gc,nonDragSelect);
      GC.setFont(gc,standardBoldFont());

       drawPlayerStuff(gc,(state==SpanglesState.PUZZLE_STATE),nonDragSelect,
	   			HighlightColor, rackBackGroundColor);

     

		if (state != SpanglesState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
     
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);
            }



 
    	// draw the avatars
        standardGameMessage(gc,messageRotation,
        		state==SpanglesState.GAMEOVER_STATE?gameOverMessage(gb):s.get(state.getDescription()),
        				state!=SpanglesState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        SpanglesCell c = gb.rack[gb.whoseTurn][0];
        c.topChip().drawChip(gc, this, iconRect, null,0.5);
        
        goalAndProgressMessage(gc,nonDragSelect,s.get("form a larger triangle with your color at the tips"),progressRect, goalRect);
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
        return (new Spanglesmovespec(st, player));
    }
    

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof SpanglesId) // not dragging anything yet, so maybe start
        {

       	SpanglesId hitObject =  (SpanglesId)hp.hitCode;
        SpanglesCell c = hitCell(hp);
 	    switch(hitObject)
	    {
	    default: break;
        case InvisibleDragBoard:
        	break;
		case ChipPool:
	    	if(c!=null)
	    		{ SpanglesChip top = c.topChip();
	    		  PerformAndTransmit("Pick "+top.index);
	    		}
	    	break;
	    case BoardLocation:
	    	if(c.topChip()!=null) { PerformAndTransmit("Pickb "+G.printCol(c.col)+" "+c.row); }
	    	break;
        }
        }
    }
	private void doDropChip(char col,int row,int ob)
	{	SpanglesState state = bb.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case PUZZLE_STATE:
		{
		PerformAndTransmit("dropb "+G.printCol(col)+" "+row+" "+ob);
		}
		break;
		case CONFIRM_STATE:
		case PLAY_STATE:
			PerformAndTransmit("dropb "+G.printCol(col)+" "+row+" "+ob);
			break;
					                 
		
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
        if(!(id instanceof SpanglesId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    		missedOneClick = false;
    		SpanglesId hitCode = (SpanglesId)hp.hitCode;
        SpanglesCell hitObject = bb.getCell(hitCell(hp));
		SpanglesState state = bb.getState();
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
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
				if(!bb.isDest(hitObject))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					PerformAndTransmit("Dropb "+G.printCol(hitObject.col)+" "+hitObject.row+" "+bb.whoseTurn);
					break;
					}
				// fall through and pick up the previously dropped piece
				//$FALL-THROUGH$
			case PUZZLE_STATE:
				PerformAndTransmit("Pickb "+G.printCol(hitObject.col)+" "+hitObject.row);
				break;
			}
			break;
			
        case EmptyBoard:
			switch(state)
			{
				default:
					throw G.Error("Not expecting EmptyBoard in state %s",state);
				case PLAY_STATE:
					if(bb.pickedObject==null)
						{ PerformAndTransmit("Dropb "+G.printCol(hitObject.col)+" "+hitObject.row+" "+bb.whoseTurn);
						  break;
						}

					//$FALL-THROUGH$
				case CONFIRM_STATE:
				case PUZZLE_STATE:
					if(bb.pickedObject!=null)
						{ doDropChip(hitObject.col,hitObject.row,bb.playerIndex(bb.pickedObject));
						}
					break;
			}
			break;
			
        case ChipPool:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop");
			}
           break;
        }
        }

     }

    // return what will be the init type for the game
    public String gameType() { return(bb.gameType()); }	// this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Spangles_SGF); }	// this is the official SGF number assigned to the game

    
    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	if(token.equals("Spangles")) 
    	{
    		// old school, where there was just the key Spangles
            bb.doInitOld(token);
    }
    	else
    	{	// new game, with the revision protocol, flag their presence with lower case.
    		// this is only a problem for games being restarted in the live context, or by spectators.
    		int np = G.IntToken(his);
    		long rv = G.LongToken(his);
    		int rev = G.IntToken(his);
    		bb.doInit(token,np,rv,rev);
    	}
 
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
    	return(new SpanglesPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 10939 files visited 0 problems
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
                bb.reInit(value);
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

