package exxit;

import bridge.*;
import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;

import online.common.*;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
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
import lib.Toggle;


/**
 * 
 * Change History
 *
 * December 2006  Initial version, derived from Hive  

*/
public class ExxitGameViewer extends CCanvas<ExxitCell,ExxitGameBoard> implements ExxitConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color logrectHighlightColor = new Color(0.9f,0.9f,0.3f);
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    private Color rackBackGroundColor = new Color(165,155,155);
    private Color boardBackgroundColor = new Color(165,155,155);
    
 
    private Font gameLogBoldFont=null;
    private Font gameLogFont = null;
    // images
    private boolean redblack_tiles = true;
    private JCheckBoxMenuItem useWoodenTiles = null;

    private static Image[] textures = null;// background textures
    // private state
    private ExxitGameBoard b = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private final double INITIAL_TILE_SCALE = 3.0;
    private double BOARD_TILE_SCALE = 4.0;
    private double SPRITE_TILE_SCALE = 4.0;
    private double RACK_TILE_SCALE = 2.0;
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle repRect = addRect("repRect");
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle scoreRects [] = addRect("score",2);
    private Toggle eyeRect = new Toggle(this,"eye",
			StockArt.NoEye,ExxitId.ToggleEye,NoeyeExplanation,
			StockArt.Eye,ExxitId.ToggleEye,EyeExplanation
			);
    
    private Rectangle tilePoolRect = addRect("tilePoolRect");
    
	// whem moving, these remember the object we're dragging around
    //private HitPoint movingObject = null; // 

    public synchronized void preloadImages()
    {	
	    if (textures == null)
	    { // note that dfor this to work correctly, the images and masks must be the same size.  
	      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
	      // this doesn't use the "-mask" suffix form of load images because some masks are
	      // shared.
	    	ExxitPiece.preloadImages(loader,ImageDir);
	        textures = loader.load_images(ImageDir,TextureNames);
	    }
	    gameIcon = textures[LIFT_ICON_INDEX];
    }
    Color StandardMouseColors[] = MouseColors;
    Color StandardMouseDotColors[] = MouseDotColors;
    Color ExxitMouseColors[] = {Color.red,Color.black};
    Color ExxitMouseDotColors[] = {Color.black,Color.white};
    void setMouseColors()
    {	if(redblack_tiles)
    	{
        MouseColors = ExxitMouseColors;
        MouseDotColors = ExxitMouseDotColors;
    	}
    	else
    	{
    	MouseColors = StandardMouseColors;
    	MouseDotColors = StandardMouseDotColors;
    	}
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

        setMouseColors();
        int FontHeight = standardFontSize();
        gameLogBoldFont = G.getFont(standardPlainFont(), G.Style.Bold, FontHeight+2);
        gameLogFont = G.getFont(standardPlainFont(),G.Style.Plain,FontHeight);
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),ExxitId.ZoomSlider);
        zoomRect.min=1.0;
        zoomRect.max=5.0;
        zoomRect.value=2.0;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = HighlightColor;

        useWoodenTiles = myFrame.addOption(s.get(WoodenTilesMessage),false,deferredEvents);
       
        b = new ExxitGameBoard(info.getString(OnlineConstants.GAMETYPE, "Exxit"),getStartingColorMap());
        //not suitable for direct drawing for reasons not quite clear,
        //but probably something to do with the pieces not being immutable
        //useDirectDrawing();
        doInit(false);
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
        	{zoomRect.setValue(INITIAL_TILE_SCALE);
        	 board_center_x = board_center_y = 0.0;
        	 startFirstPlayer();        	
        	 }
   } 
    
    public int midGamePoint()
    {	return(40);		// exxit games are long
    }

    public Rectangle createPlayerGroup(int player,int x,int y,double displayRotation,int unit)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int chipW = unit*5;
     	Rectangle box = pl.createRectangularPictureGroup(x+chipW+unit/4,y,unit);
     	Rectangle chip = chipRects[player];
     	Rectangle score = scoreRects[player];
     	Rectangle done = doneRects[player];
       	pl.displayRotation = displayRotation;
       	int boxr = G.Right(box);
       	G.SetRect(chip, x, y, chipW, chipW/2);
        G.SetRect(score,boxr,y,unit*2,unit*2);
        G.SetRect(done,x,y+chipW/2+unit/4,chipW,plannedSeating()?chipW/2:0);
        G.union(box, score,done,chip);
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
    	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.6,	// 80% of space allocated to the board
    			1,		// aspect ratio for the board
    			fh*2.5,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
    	int vcrH = fh*8;
    	int minLogW = fh*15;
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
     	CELLSIZE = fh*2;
 
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);

       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
       	layout.placeTheVcr(this, vcrH*2, vcrH*3);
    	layout.placeRectangle(tilePoolRect,
    				buttonW*2,buttonW,buttonW*2,buttonW,
    				buttonW,buttonW*2,buttonW,buttonW*2,
    				BoxAlignment.Center,false);

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
        G.placeStateRow(stateX,stateY,boardW,stateH,iconRect,stateRect,eyeRect,liftRect,noChatRect);
        G.placeRight(stateRect,zoomRect,zoomW);
        
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
    }

    
	// draw a box of spare chips. It's purely for visual effect.
    private boolean fliptiles = false;
    private void DrawTilePool(Graphics gc, Rectangle r, HitPoint highlight)
    {
      boolean canhit = b.LegalToHitTiles() && G.pointInRect(highlight, r);
      ExxitCell c = b.tiles;
      if(gc!=null) { GC.frameRect(gc, Color.black, r); }
      Random rand = new Random(4321); // consistant randoms, different for black and white 
      boolean canDrop = canhit && (b.movingObjectIndex()>=0);
      boolean hitPiece = false;
      int height = c.height();
     // if(r.height>CELLSIZE*4)	// r can be not laid out during initialization
      {for(int i=0;i<height;i++)
        {
        	ExxitPiece p = c.pieceAtIndex(i);
            int cs = CELLSIZE*7;
            int spacex = Math.max(10, G.Width(r) - CELLSIZE*2);
            int spacey = Math.max(10, G.Height(r)- CELLSIZE*2);
            int rx = G.Left(r)+ Random.nextInt(rand,spacex);
            int ry = G.Top(r)+ Random.nextInt(rand,spacey);
            c.setCurrentCenter(rx,ry);
            int index = p.imageIndex(fliptiles)
	  			+ (redblack_tiles ? REDBLACK_OFFSET : STANDARD_OFFSET);
	  			 
            if(gc!=null) 
        	{drawImage(gc, ExxitPiece.images[index], SCALES[index], rx + CELLSIZE, ry +CELLSIZE, cs, 1.0,0.0,null,false);
        	}
        	 if(canhit && !canDrop && G.pointInRect(highlight,rx,ry,CELLSIZE*2,CELLSIZE*2))
	        		{ highlight.hitObject = b.tiles;
	        		  highlight.arrow = StockArt.UpArrow;
	        		  highlight.awidth = CELLSIZE;
	        		  highlight.spriteColor = Color.red;
	        		  highlight.hit_x = rx;
	        		  highlight.hit_y = ry;
	        		  hitPiece = true;
	         		  highlight.hitCode = (p.colorIndex==(fliptiles?SECOND_PLAYER_INDEX:FIRST_PLAYER_INDEX))?ExxitId.White_Tile_Pool:ExxitId.Black_Tile_Pool;
	        		}

        }
       if(gc!=null)
    	   {GC.Text(gc,true,G.Right(r)-CELLSIZE,G.Bottom(r)-CELLSIZE,CELLSIZE,CELLSIZE,Color.black,null,
    		   	""+height);
    	   }
       if(canhit && canDrop) 
       	{ highlight.hitObject = b.tiles;
       	  highlight.hitCode = ExxitId.White_Tile_Pool;
       	  highlight.arrow = StockArt.DownArrow;
       	  highlight.awidth = CELLSIZE;
       	  highlight.spriteColor = Color.red;

       	  hitPiece = true;
       	}
       else if (canhit && !canDrop)
       {	if(!hitPiece) { highlight.hitCode = ExxitId.Flip_Tiles; }
       }
      }
    }
    private void DrawScore(Graphics gc,Rectangle r,int player)
    {
    	GC.setFont(gc,gameLogBoldFont);
    	GC.Text(gc,true,r,0,Color.black,rackBackGroundColor,""+b.scoreForPlayer(player,true));
    	GC.frameRect(gc,Color.black,r);
    }
	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawChipPool(Graphics gc, ExxitState state,Rectangle r, int player,HitPoint highlight)
    {	ExxitCell thisCell= b.rack[player];
        boolean canhit = b.LegalToHitChips(player) && G.pointInRect(highlight, r);
        boolean canDrop = canhit && (b.movingObjectIndex() == player) && G.pointInRect(highlight,r);
        boolean doesHit = canDrop;
        int cellW = G.Width(r)/2;
        if(gc!=null) { GC.frameRect(gc, Color.black, r); }
        {	
        	if(thisCell!=null)
        	{
	    		int left = G.Left(r)+cellW/2;
	    		int height=thisCell.height();
    		
        		for(int bug=height; bug>0;bug--)
        		{
        		ExxitPiece topCup = thisCell.pieceAtIndex(height-bug);
        		if(topCup!=null)
        		{ int rx = left+((bug<=height/2)?cellW:0);
        		  int ry = G.Top(r)+5*G.Height(r)/8+2-CELLSIZE/3*((bug<=height/2)?height/2-bug:height-bug);
        		  int index = topCup.imageIndex(false)
        		  			+ (redblack_tiles ? REDBLACK_OFFSET : STANDARD_OFFSET);
        		  thisCell.setCurrentCenter(rx, ry);
	           	  if(canhit 
	           			&& G.pointInRect(highlight,rx-cellW/2,ry-CELLSIZE/2,cellW,CELLSIZE*2)
	           			&& (height>0)
	           			)
	        		{ doesHit = true;
	        		  highlight.hit_x = rx;
	      			  highlight.hit_y = ry;
 	        		}

        		  if(gc!=null)
                	{drawImage(gc, ExxitPiece.images[index], SCALES[index],rx,  ry,
                			CELLSIZE*7, 1.0,0.0,null,false);
                	//G.frameRect(gc,Color.black,left,top,cellW,SQUARESIZE);
                	}
       		}}
        		
      		if(doesHit)
      			{highlight.hitObject = thisCell;
      			highlight.hitCode = thisCell.rackLocation();
      			highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
      			highlight.awidth = CELLSIZE;
      			highlight.spriteColor = Color.red;
      			}

        		//if((gc!=null)&&(hitCell==thisCell)) { G.frameRect(gc,Color.red,left-cellW/2,top-CELLSIZE/2,cellW,2*CELLSIZE); }
           	if((gc!=null) && ((thisCell==b.pickedSource)||(thisCell==b.droppedDest)))
        	{ 
            GC.cacheAACircle(gc,left,G.Top(r)+CELLSIZE/2,2,Color.green,Color.yellow,true);
            }
        	}
 

        }
    }

    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
    	int idx = obj + (redblack_tiles ? REDBLACK_OFFSET : STANDARD_OFFSET);
      	 ;
       	int cellS = chipSize();
       	double scale = boardRect.contains(xp,yp)?SPRITE_TILE_SCALE:RACK_TILE_SCALE;
           drawImage(g, ExxitPiece.images[idx],SCALES[idx], xp, yp,cellS*scale);
	
    }


    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);   
         textures[reviewBackground 
                                 ? BROWN_FELT_INDEX
                                 : (redblack_tiles ? YELLOW_FELT_INDEX : OLD_YELLOW_FELT_INDEX)].tileImage(gc,
          		boardRect); 
      GC.frameRect(gc,Color.black,boardRect);
 
      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method
      //gb.DrawGrid(gc, tbRect, use_grid, boardBackgroundColor, Color.blue, Color.blue,Color.black);

     }

    private int chipSize()
    {
        double cs = (b.cellToX('B',2)-b.cellToX('A',2))/zoomRect.value;
        int cellSize =  (int)(cs*zoomRect.value);
        return(cellSize);	
    }

    /* draw the board and the chips on it. */
     private void drawBoardElements(Graphics gc, ExxitGameBoard gb, Rectangle tbRect,
    		 HitPoint ourTurnSelect,HitPoint anySelect)
     {	
     	int liftdiv = 40;
     	int cellSize = chipSize();
     	boolean dolift = doLiftAnimation();
        Rectangle oldClip = GC.combinedClip(gc,tbRect);
        boolean show = eyeRect.isOnNow();
      	boolean draggingBoard = draggingBoard();
 
     	//
       	// now draw the contents of the board and anything it is pointing at
        //
         ExxitCell hitCell = null;
         Hashtable<ExxitCell,ExxitCell> dests = gb.movingObjectDests();
         ExxitCell sourceCell = gb.pickedSource; 
         ExxitCell destCell = gb.droppedDest;
        // calculate the spanning size of the board.
         
         int liftYval =  cellSize/6+(dolift?(liftSteps*cellSize)/liftdiv : 0);
         int liftXval = dolift?(liftSteps*cellSize)/(2*liftdiv) : 0;
         double actCellSize = cellSize*BOARD_TILE_SCALE;
         //System.out.println("cs "+cs/CELLSIZE+ " "+cs+" "+CELLSIZE);
         int left = G.Left(tbRect);
         int top = G.Bottom(tbRect);
         for(Enumeration<ExxitCell> cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements(); )
             { //where we draw the grid
        	 ExxitCell cell = cells.nextElement();
        	 int xpos = left + gb.cellToX(cell);
        	 int ypos = top - gb.cellToY(cell);
                 cell.setCurrentCenter(xpos,ypos);
                 boolean isADest = dests.get(cell)!=null;
                 boolean isASource = (cell==sourceCell)||(cell==destCell);
                 ExxitPiece piece = cell.topPiece();
             boolean canHit = gb.LegalToHitBoard(cell);
                 boolean hitpoint = !draggingBoard
                		 && canHit
                		 && cell.closestPointToCell(ourTurnSelect,cellSize, xpos, ypos);
                 if(hitpoint) 
                 { hitCell = cell;
                 }

                 boolean drawhighlight = hitpoint ||
                     gb.isDest(cell) ||
                     gb.isSource(cell);
  

                 if (gc != null)
                 {
                 //G.DrawAACircle(gc,xpos,ypos,1,tiled?Color.green:Color.blue,Color.yellow,true);
                 if(piece!=null)
                 {	for(int hgt=cell.height()-1-cell.activeAnimationHeight(),lvl=0; lvl<=hgt; lvl++)
                 	{
                 	ExxitPiece drawPiece = cell.pieceAtIndex(lvl);
                     int pi = drawPiece.imageIndex(false) 
                     			+ (redblack_tiles ? REDBLACK_OFFSET : STANDARD_OFFSET);  
                     if (drawhighlight)
                     { // checking for pointable position
                       //  drawChip(gc, SELECTION_INDEX, xpos, ypos, actcellSize, 1.0,0.0);
                     }

                     if (pi >= 0)
                     {	
                     	String id=null;
                     	
                        if(use_grid && (lvl==hgt)) 
                        { id= cell.cellName;
                        }
 
                     	// adjustScales(pscale,piece);
      
                     	drawImage(gc, ExxitPiece.images[pi], SCALES[pi], xpos+liftXval*lvl,ypos-liftYval*lvl, actCellSize, 1.0,0,id,false);

                     }
                 	}
                }
                 if((show && canHit) || isASource)
                 {GC.cacheAACircle(gc,xpos,ypos,2,Color.green,Color.yellow,true);
                 } else
                 if(isADest)
                 {GC.cacheAACircle(gc,xpos,ypos,2,Color.red,Color.yellow,true);
                 }
             }
         }
         
         // drawing
         if (hitCell!=null)
         {	 //if(gc!=null) { gc.drawOval(xpos-cellSize/2,ypos-cellSize/2,cellSize,cellSize); }
             ourTurnSelect.hitCode = ExxitId.BoardLocation;
             ourTurnSelect.arrow = hasMovingObject(ourTurnSelect) ? StockArt.DownArrow:StockArt.UpArrow;
             ourTurnSelect.awidth = CELLSIZE;
             ourTurnSelect.spriteColor = Color.red;
         }
       	doBoardDrag(tbRect,anySelect,cellSize,ExxitId.InvisibleDragBoard);

  		GC.setClip(gc,oldClip);
     }


    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  ExxitGameBoard gb = disB(gc);
       boolean moving = hasMovingObject(selectPos);
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;
       HitPoint buttonSelect = moving?null:ourTurnSelect;
       HitPoint nonDraggingSelect = (moving && !reviewMode()) ? null : selectPos;
  
   
       ExxitState state = gb.getState();
       gameLog.redrawGameLog(gc, nonDraggingSelect, logRect, Color.black, logrectHighlightColor,gameLogBoldFont,gameLogFont);
        drawBoardElements(gc, gb, boardRect, ourTurnSelect,nonDraggingSelect);
        boolean planned = plannedSeating();
        
        for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX;i++)
        {
        commonPlayer pl = getPlayerOrTemp(i);
        pl.setRotatedContext(gc, selectPos, false);
        DrawScore(gc,scoreRects[i],i);
        DrawChipPool(gc, state, chipRects[i], i, ourTurnSelect);
		if(planned && (b.whoseTurn==i))
			{handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
				HighlightColor, rackBackGroundColor);
			}
        pl.setRotatedContext(gc, selectPos, true);
        }

        commonPlayer pl = getPlayerOrTemp(b.whoseTurn);
        
        DrawTilePool(gc, tilePoolRect,ourTurnSelect);
        zoomRect.draw(gc,nonDraggingSelect);
        drawLiftRect(gc,liftRect,nonDraggingSelect,textures[LIFT_ICON_INDEX]);
        double messageRotation = pl.messageRotation();
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        //System.out.println("dig "+b.Digest());
        
        GC.setFont(gc,standardBoldFont());
		if (state != ExxitState.PUZZLE_STATE)
        {
			if(!planned  && !autoDoneActive())
				{handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos, HighlightColor, rackBackGroundColor);
        }

		drawPlayerStuff(gc,(state==ExxitState.PUZZLE_STATE),nonDraggingSelect,HighlightColor,rackBackGroundColor);


            standardGameMessage(gc,messageRotation,
            		state==ExxitState.GAMEOVER_STATE?gameOverMessage():s.get(state.getDescription()),
            				state!=ExxitState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
            ExxitPiece chip = gb.playerChip[gb.whoseTurn];
            int idx = chip.imageIndex(false);
            drawImage(gc, ExxitPiece.images[idx],SCALES[idx],G.centerX(iconRect),G.centerY(iconRect),G.Width(iconRect)*4);
            goalAndProgressMessage(gc,nonDraggingSelect,s.get(GoalMessage),progressRect, goalRect);
        eyeRect.activateOnMouse = true;
        eyeRect.draw(gc,selectPos);
        drawVcrGroup(nonDraggingSelect, gc);

    }
    public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode mode)
    {	// the super method in commonCanvas is where the history is actually recorded
       	if(((m.op==MOVE_DONE) 
       			&& (b.getState()==ExxitState.PASS_STATE)
       			&& OurMove() 
       			&& (mode==replayMode.Live)
       			))
       		{	// insert a "pass" before "done"
       		PerformAndTransmit(PASS); 
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
    {	Exxitmovespec m = (Exxitmovespec)mm;
  
        handleExecute(b,m,replay);
        b.labelCells();
        switch(m.op)
        {
        case MOVE_MOVE:
        	m.shortMoveString = b.distributionLabel();
        	break;
        case MOVE_EXCHANGE:
        	m.shortMoveString = b.exchangeLabel();
        	break;
        case MOVE_DROPB:
        case MOVE_PICKB:
        	{
        	ExxitCell c = b.GetExxitCell(m.from_col,m.from_row);
        	m.shortMoveString = c.cellName;
        	}
        	break;
		default:
			break;
        }
        startBoardAnimations(replay,b.animationStack,(int)(chipSize()*BOARD_TILE_SCALE*0.25),
        		MovementStyle.Simultaneous);

        if(replay!=replayMode.Replay) { playSounds(m); }

         return (true);
    }
     
 void playSounds(Exxitmovespec mm)
 {
	switch(mm.op)
	{
	case MOVE_EXCHANGE:
	case MOVE_MOVE:
		int n = mm.undoDistributionInfo;
		while(n-- >0) { playASoundClip(light_drop,100); }
		//playASoundClip(heavy_drop,50);
		break;
	case MOVE_PICK:
	case MOVE_PICKB:
	case MOVE_DROPB:
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
        return (new Exxitmovespec(st, player));
    }
/**
 * prepare to add nmove to the history list, but also edit the history
 * to remove redundant elements, so that indecisiveness by the user doesn't
 * result in a messy replay.
 * This may require that move be merged with an existing history move
 * and discarded.  Return null if nothing should be added to the history
 * One should be very cautious about this, only to remove real pairs that
 * result in a null move.
 * 
 */
    public commonMove EditHistory(commonMove nmove)
    {	// allow forced passes that are in the game record for appearance sake
    	return(EditHistory(nmove,(nmove.op==MOVE_PASS)));
    }


/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof ExxitId) // not dragging anything yet, so maybe start
        {
       	ExxitId hitObject = (ExxitId)hp.hitCode;
		ExxitCell cell = hitCell(hp);
		ExxitPiece bug = (cell==null) ? null : cell.topPiece();
		ExxitState state = b.getState();
      		
  		switch(hitObject)
	    {
	    default: break;
	    case Black_Tile_Pool:
	    case White_Tile_Pool:
            {
            String col = hitObject.shortName;
            if(state==ExxitState.DROPTILE_STATE) { col = TILE_NAMES[b.getColorMap()[b.whoseTurn]]; }
            PerformAndTransmit("Pick "+col);
            }
            break;
	    case Flip_Tiles:
	    	//fliptiles = !fliptiles;	// local action only
	    	break;
        case InvisibleDragBoard:
        	break;
        case ZoomSlider:
        	break;
	    case Black_Chip_Pool:
	    	PerformAndTransmit("Pick B "+cell.row+" "+CHIP_NAMES[bug.colorIndex]);
	    	break;
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick W "+cell.row+" "+CHIP_NAMES[bug.colorIndex]);
	    	break;
	    case BoardLocation:
	    	switch(state)
	    	{
	    	case DROPTILE_STATE:
	    		PerformAndTransmit("Dropb "+TILE_NAMES[b.getColorMap()[b.whoseTurn]]+" "+cell.col+" "+cell.row);
	    		break;
	    	case DROP_STATE:
	    	case EXCHANGE_STATE:
	    	case DROP_OR_EXCHANGE_STATE:
	    		if(cell.canExchange())
	    				{
	    				PerformAndTransmit("Exchange " + cell.col+" "+cell.row+" "
	    							+CHIP_NAMES[b.getColorMap()[b.whoseTurn]]);
	    				}
	    		else 
	    			{PerformAndTransmit("Dropb "+CHIP_NAMES[b.getColorMap()[b.whoseTurn]]+" "+cell.col+" "+cell.row);
	    			}
	    		break;
	    	case CONFIRM_STATE:
	    	case PUZZLE_STATE:
	    	case DISTRIBUTE_STATE:
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+CHIP_NAMES[bug.colorIndex]);
	    		break;
	    	case CONFIRM_DISTRIBUTE_STATE:
	    	case CONFIRM_EXCHANGE_STATE:
	    		PerformAndTransmit(RESET);
	    		break;
	    	default: throw G.Error("Not expecting state %s",state);
	    	}
	    	break;
        }

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
        if(!(id instanceof ExxitId)) {  missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
    	missedOneClick = false;
    	ExxitId hitObject = (ExxitId)hp.hitCode;
        ExxitState state = b.getState();
		ExxitCell cell = b.getCell(hitCell(hp));
		ExxitPiece bug = (cell==null) ? null : cell.topPiece();
       	
		switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case ToggleEye:
        	eyeRect.toggle();
        	break;
        case ZoomSlider:
        case InvisibleDragBoard:
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case CONFIRM_DISTRIBUTE_STATE:
			case CONFIRM_EXCHANGE_STATE:
				break;
				
			case DISTRIBUTE_STATE:
			case DROP_STATE:
			case DROP_OR_EXCHANGE_STATE:
			case EXCHANGE_STATE:
			case PUZZLE_STATE:
			case DROPTILE_STATE:
			{
				String name = b.movingObjectName();
				if(name!=null)
				{ ExxitCell source = b.pickedSource;
				  if((state==ExxitState.DISTRIBUTE_STATE) && (cell != source))
					{ int dir = b.findDirection(source.col,source.row,cell.col,cell.row);
					  PerformAndTransmit("Move "+name+" "+source.col+" "+source.row+" "+dir);
					}
					else
					{ PerformAndTransmit("Dropb "+name+" "+cell.col+" "+cell.row); 
					}
				}
				else if((bug!=null) && (bug.typecode==TILE_TYPE))
				{
				PerformAndTransmit( "pickb "+TILE_NAMES[bug.colorIndex]+" "+cell.col+" "+cell.row);
				}
				break;
			}}
			break;
        case Black_Tile_Pool:
        case White_Tile_Pool:
    	{
            String name = b.movingObjectName();
            if(name!=null) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on pool in state %s",state);
            	case DROPTILE_STATE:
               	case PUZZLE_STATE:
                	PerformAndTransmit("Drop "+name);
            		break;
            	}
			}
        	}
    	break;
        case Black_Chip_Pool:
        case White_Chip_Pool:
    	{
        	String name = b.movingObjectName();
            if(name!=null) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
            	case DROP_OR_EXCHANGE_STATE:
               	case DROP_STATE:
               	case DISTRIBUTE_STATE:
               	case EXCHANGE_STATE:
               		performReset();
            		break;

               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop "+name);
            		break;
            	}
			}
        	}
            break;

        case Flip_Tiles: break; // no action here
        }
    	}
//        movingObject = null;
    }



    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     */
    public void drawCanvas(Graphics offGC, boolean complete,HitPoint hp)
    {
    	b.SetDisplayParameters(zoomRect.value,1.0,board_center_x,board_center_y,30.0); // shrink a little and rotate 30 degrees
    	b.SetDisplayRectangle(boardRect);
		super.drawCanvas(offGC,complete,hp);
 
    }  
    // return what will be the init type for the game
    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Exxit_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        b.doInit(token);
    }



    /** return the player whose turn it really is.  This is used by the game controller
     * to key sounds and other per player turn actions.
     */
    public BoardProtocol getBoard()
    {
        return (b);
    }


    /** this is used by the game controller to supply entertainment strings to the lobby */
    public String gameProgressString()
    {	   return ((mutable_game_record ? Reviewing : ("" + History.viewMove)) + " " +
            b.scoreForPlayer(FIRST_PLAYER_INDEX,true) + " " +
            b.scoreForPlayer(SECOND_PLAYER_INDEX,false));

    }

    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
        if (target == useWoodenTiles)
        {	handled=true;
        	redblack_tiles = !useWoodenTiles.getState();
        	setMouseColors();
        	generalRefresh();
        }
        return (handled);
    }
    public SimpleRobotProtocol newRobotPlayer() { return(new ExxitPlay()); }


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
                b.doInit(value);
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (name.equals(game_property) && value.equalsIgnoreCase("exxit"))
            {
                // the equals sgf_game_type case is handled in replayStandardProps
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
