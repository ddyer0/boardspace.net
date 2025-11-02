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
package trax;

import javax.swing.JCheckBoxMenuItem;

import common.GameInfo;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.StringTokenizer;


import lib.Graphics;
import lib.Image;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.CellId;
import online.game.BoardProtocol;
import online.game.commonCanvas;
import online.game.commonMove;
import online.game.commonPlayer;
import online.game.replayMode;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

/**
 * 
 * Change History
 *
 * Aug 2005  initial work in progress.  
 * 
*/
public class TraxGameViewer extends commonCanvas implements TraxConstants
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    private Color rackBackGroundColor = new Color(140,175,146);
    private Color boardBackgroundColor = new Color(140,175,146);
    private Color highlightColor = new Color(150,195,166);
    

    
    private static Image[] textures = null; // backgrounds
    // private state
    private TraxGameBoard b = null; //the board from which we are displaying
    private int CELLSIZE=1; 	//size of the layout cell

    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle tileRects[] = addRect("tile",2);
    private Rectangle chipPool = addRect("chipPoolRect");

	// private menu items
    // this is a debugging hack to print the robot's evaluation of the
    // current position.  Not important in real play.  Only available via secret options.
    private JCheckBoxMenuItem useClassicItem = null;
    private boolean useClassic=false; 
    private static final double INITIAL_TILE_SCALE = 3.0;
    private int XtoBC(int x,Rectangle brect)
    {	int CELL = boardCellSize();
		int center_x = b.left*CELL-boardCenterX(CELL,brect);
		return((((x-G.centerX(brect))-center_x)*10)/CELL);
    }
    private int YtoBC(int y,Rectangle brect)
    {	int CELL = boardCellSize();
    	int center_y = b.top*CELL-boardCenterY(CELL,brect);
    	int yinbox = (y-G.centerY(brect));
    	int ry = ((yinbox-center_y)*10)/CELL;
     	return( ry );
   }

    public BoardProtocol getBoard()   {    return (b);   }
    
    public synchronized void preloadImages()
    {
        if (textures == null)
        { // note that dfor this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        	TraxChip.preloadImages(loader,ImageDir);
        	
            textures = loader.load_images(ImageDir,TextureNames);
             
        }
        gameIcon = textures[ICON_INDEX];
     }
    Color TraxMouseColors[] = { Color.white,Color.red };
    Color TraxMouseDotColors[] = { Color.black,Color.white} ;
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

        MouseDotColors = TraxMouseDotColors;
        MouseColors = TraxMouseColors;
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),TraxId.ZoomSlider);
        zoomRect.min=1.0;
        zoomRect.max=3.5;
        zoomRect.value=INITIAL_TILE_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = highlightColor;
        
        
        useClassicItem = myFrame.addOption(ClassicTileOption,false,deferredEvents);
        
        b = new TraxGameBoard(info.getString(GameInfo.GAMETYPE, "Trax"),
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
        b.doInit(b.gametype);						// initialize the board
        if(!preserve_history)
    	{   
        startFirstPlayer(); 
    	}
    }

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = tileRects[player];
    	int chipW = unitsize*5;
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

    	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.strictBoardsize = false;
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.8,	// 80% of space allocated to the board
    			1,		// aspect ratio for the board
    			fh*2.0,	// maximum cell size
    			0.5		// preference for the designated layout, if any
    			);
    	int minLogW = fh*12;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
     	CELLSIZE = fh*2;


        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeRectangle(chipPool,minLogW,minLogW/2,minLogW,minLogW/2,BoxAlignment.Edge,true);
    	
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);

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
        placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom,boardW,stateH,goalRect);       
        G.placeRight(goalRect, zoomRect, zoomW);
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white); 	
    }

  
    TraxChip [] imageGroup(int alt_image)
    {	switch(alt_image)
    	{ 
    	default: return(useClassic ? TraxChip.classicChips : TraxChip.modernChips);
    	case FIRST_PLAYER_INDEX: return(useClassic ? TraxChip.classicWhiteChipLines : TraxChip.modernWhiteChipLines);
    	case SECOND_PLAYER_INDEX: return(useClassic ? TraxChip.classicBlackChipLines : TraxChip.modernRedChipLines);
    	}
    }
	// draw a graphic at x,y.  The images are loaded and composisted
    // at init time, so they're always completely ready to draw.
    private void drawChip(Graphics gc, int idx, int x, int y, int boxw, double scale,int alt_image)
    {	TraxChip chip = imageGroup(alt_image)[idx];
    	Image im = chip.image;
    	double []lscale = chip.scale ;
    	drawImage(gc,im,lscale,x,y,boxw*scale);
    }


	// draw a box of spare chips. For trax it's purely for effect.
    private void DrawChipPool(Graphics gc, Rectangle r,  HitPoint highlight)
    {
       int cw = G.Width(r)/3;
       int ch = G.Height(r)/2;
       int unit = ch*2/3;
       boolean inRect = G.pointInRect(highlight, r);
       boolean canhit = inRect && b.LegalToHitChips(OurMove());

        if(gc!=null) { GC.frameRect(gc, Color.black, r); }
    	for(int col=0; col<3;col++)
        {
        	for(int row=0;row<2;row++)
        	{	int rx0 = G.Left(r)+cw*col;
        		int rx = rx0+(cw-unit)/2+unit/2;
        		int ry0 = G.Top(r)+ch*row;
        		int ry = ry0+(ch-unit)/2+unit/2;
        		int ccode = row+col*2;
        		boolean ishit = canhit && G.pointInRect(G.Left(highlight),G.Top(highlight),rx0,ry0,cw,ch);
        		if(ishit)
        		{	highlight.hitCode = MATCHTILES[ccode];
        		}
        		if(gc!=null)
        		{
        		if(ishit)
        		{  
        			GC.fillRect(gc,HighlightColor,rx0,ry0,cw,ch);
        		}
        		drawChip(gc,ccode,rx,ry,unit,1.0,-1);
        		}
        		//gc.setColor(Color.red);
        		//gc.drawLine(r.x+cw*col,r.y,r.x+cw*col,r.y+r.height);
        	}
       		//gc.drawLine(r.x,r.y+ch,r.x+r.width,r.y+ch);
       	 }
    }

    /**
     * translate the mouse coordinate x,y into a size-independant representation
     * presumably based on the cell grid.  This is used to transmit our mouse
     * position to the other players and spectators, so it will be displayed
     * at approximately the same visual spot on their screen.  
     *  */
    public Point getTraxBoardCoords(int x, int y)
    {	// implement a dual coordinate system, for "in board" and "out of board"
    	if(G.pointInRect(x,y,boardRect))
    	{ 	return(new Point(1000+XtoBC(x,boardRect),1000+YtoBC(y,boardRect)));
    	}
    	return (new Point((x * 10) / CELLSIZE, (y * 10) / CELLSIZE));
    }
    private int BCtoX(int x,Rectangle brect)
    {	int CELL = boardCellSize();
		int center_x = b.left*CELL-boardCenterX(CELL,brect);
		return((x*CELL)/10+center_x+G.centerX(brect));
    }
    private int BCtoY(int y,Rectangle brect)
    {	int CELL = boardCellSize();
		int center_y =  b.top*CELL-boardCenterY(CELL,brect);
		int ry = (y*CELL)/10+center_y+G.centerY(brect);
		return(ry);
    }
    public String encodeScreenZone(int x, int y,Point p)
    {	Point pt = getTraxBoardCoords(x,y);
    	G.SetLeft(p, G.Left(pt));
    	G.SetTop(p, G.Top(pt));
    	return("on");
    }
	public Point decodeTraxCellPosition(int x,int y,double cellsize)
	{	
        int xp = (x>800) 
		? BCtoX(x-1000,boardRect)
		:(int) (((x + 0.5) * cellsize) / 10);	// invert the transform of getBoardCoords
		int yp = (y>800)
			? BCtoY(y-1000,boardRect)
					: (int) (((y + 0.5) * cellsize) / 10);
		return(new Point(xp,yp));
	}
	public Point decodeScreenZone(String zone,int x,int y)
	{
		return(decodeTraxCellPosition(x,y,CELLSIZE));
	}

    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
       	int scale = G.pointInRect(xp,yp,boardRect) ? boardTileSize() : CELLSIZE;
        drawChip(g, obj, xp+CELLSIZE/2, yp-CELLSIZE/2, scale, 1.0,-1);

    }

 
    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {boolean review = reviewMode() && !mutable_game_record;
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
         textures[review ? BROWN_FELT_INDEX:GREEN_FELT_INDEX].tileImage(gc,
          		boardRect); 
      GC.frameRect(gc,Color.black,boardRect);
       
     }
    int boardCellSize() { return((int)(CELLSIZE*zoomRect.value)); }
    int boardTileSize() { return((int)(boardCellSize()*0.9)); }
    int boardCenterX(int CELL,Rectangle brect)
    	{ return((int)G.rangeLimit((b.ncols*CELL)/2-(board_center_x*G.Width(brect)),b.ncols*2)); 
    	}
    int boardCenterY(int CELL,Rectangle brect) 
    	{ return((int)G.rangeLimit((b.nrows*CELL)/2-(board_center_y*G.Height(brect)),b.nrows*2)); 
    	}
   
    /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, TraxGameBoard gb, Rectangle brect, HitPoint ourTurnSelect,HitPoint alwaysSelect)
    {	TraxState state = gb.getState();
    	boolean showdots = (state == TraxState.PUZZLE_STATE);
    	int winner = b.winner();
    	boolean showwins = (winner>=0);
    	boolean moving = (gb.movingObjectIndex()>=0);
    	Rectangle sh= GC.combinedClip(gc,G.Left(brect),G.Top(brect),G.Width(brect),G.Height(brect));
        //
        // now draw the contents of the board and anything it is pointing at
        //
    	HitPoint mo = getDragPoint();
    	boolean draggingBoard = (alwaysSelect!=null) && (mo!=null) 
    			&& ((mo.hitCode==TraxId.InvisibleDragBoard)||(mo.hitCode==TraxId.DragBoard));
    	setDraggingBoard(draggingBoard);
    	// trax is an outlier that doesn't use the standard board drag logic
	    if(draggingBoard)
		{	double center_x = (board_center_x*G.Width(brect))+(G.Left(alwaysSelect)-G.Left(mo));
		    double center_y = (board_center_y*G.Height(brect))+(G.Top(alwaysSelect)-G.Top(mo));
		    board_center_x = center_x / G.Width(brect);
		    board_center_y = center_y / G.Height(brect);
		    G.SetTop(mo, G.Top(alwaysSelect));
		    G.SetLeft(mo,G.Left(alwaysSelect));
		    repaint(20);
		}
	    if(gb.tilesOnBoard==0)
    	{	if(ourTurnSelect!=null)
    		{	if(G.pointInRect(ourTurnSelect,brect))
    				{ourTurnSelect.hitCode = TraxId.EmptyBoard;
    				ourTurnSelect.col = '@';
    				ourTurnSelect.row = 0;
    				}
    		}
    	}
    	else
    	{
    	int CELL = boardCellSize();
    	int center_x = boardCenterX(CELL,brect);
    	int center_y = boardCenterY(CELL,brect);
    	int firstcol = gb.left-1;
    	int firstrow = gb.top-1;
    	int CHIPSIZE = boardTileSize();
  
     	//
    	// note we draw from lower-right to upper-left so the shadows will
    	// not be cast over the next piece drawn.
    	//
   		for(int yindex=gb.bottom; yindex>=firstrow; yindex--)
   		{	int row = yindex-firstrow;
			int y = yindex*CELL-center_y + G.centerY(brect);
			for(int xindex=gb.right; xindex>=firstcol; xindex--)
    		{	char col = (char)(xindex-firstcol+'@');
     			int x = xindex*CELL-center_x + G.centerX(brect);
    			char cell = gb.GetBoardPos(col,row);
    			boolean isEmpty = cell==Empty;
    			boolean canhit =  gb.LegalToHitBoard(col,row);
    			boolean bisdest = gb.isDest(col, row);
     			if(use_grid && (gc!=null))
    			{if((xindex==firstcol) && (row>0))
    				{	
    				GC.Text(gc,true,x-CELL/4,y-CELL/4,CELL/2,CELL/2,Color.black,null,""+row);
    				}
    			if((yindex==firstrow))
    				{
    				GC.Text(gc,true,x-CELL/4,y-CELL/4,CELL/2,CELL/2,Color.black,null,""+col);
    				}
    			}
    			if(G.pointInsideSquare(ourTurnSelect,x,y,CELL)
    				&& HitPoint.closestPoint(ourTurnSelect, x,y,TraxId.EmptyBoard))
    			{	if(canhit)
    				{
    				ourTurnSelect.col = col;
					ourTurnSelect.row = row;
    				if(isEmpty 
    					|| moving 
    					|| G.pointInsideSquare(ourTurnSelect, x, y,CELL/4)
    					|| (gb.nColorMatch(col,row)<=1)
    					)
    				{	
 				    ourTurnSelect.arrow = isEmpty ? StockArt.DownArrow : StockArt.UpArrow;
 				    ourTurnSelect.hitCode = isEmpty ? TraxId.EmptyBoard : TraxId.BoardLocation;
 				    ourTurnSelect.awidth = CHIPSIZE/2;
 				    ourTurnSelect.spriteColor = Color.red;
 				    }
    				else 
    				{	
    				// hitting the corner which means flip to the next state
					ourTurnSelect.hitCode = TraxId.TileCorner;
					ourTurnSelect.arrow = StockArt.Rotate_CW;
					ourTurnSelect.awidth = 2*CHIPSIZE/3;
					ourTurnSelect.spriteColor = Color.blue;
    				}
    				}
    			else 
    				{ ourTurnSelect.hitCode = GameId.HitNothing; 
    				  ourTurnSelect.spriteColor = null;
    				  ourTurnSelect.arrow = null;
    				}
				 }
	

				if(gc!=null)
 				{
				if(cell!=Empty)
    			{	int idx = cell-'0';
     				drawChip(gc, idx, x, y, CHIPSIZE, 1.0,-1);
     				if(bisdest) 
     					{ drawChip(gc,idx,x,y,CHIPSIZE,1.0,b.whoseTurn);
     					  StockArt.Dot.drawChip(gc,this,CHIPSIZE,x,y,null);
     					}
     				if(showwins && b.onWinningLine(col,row))
     				{//drawGif(gc,GIF_DESAT_INDEX,x,y,CHIPSIZE,1.0);
     				   drawChip(gc,idx,x,y,CHIPSIZE,1.0,winner);
     				}
       			}
				else if(showdots)
				{ if(gb.nColorMatch(col,row)==1)
					{ StockArt.Dot.drawChip(gc,this,CHIPSIZE,x,y,null);
					}
				}
 				}
    		}
   		}
        if(startBoardDrag(alwaysSelect,brect)) { alwaysSelect.hitCode = TraxId.InvisibleDragBoard; }

    	}
     GC.setClip(gc,sh);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  TraxGameBoard gb = (TraxGameBoard)disB(gc);
       boolean moving = hasMovingObject(selectPos);
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;
       HitPoint buttonSelect = moving?null:ourTurnSelect;
       HitPoint nonDraggingSelect = (moving && !reviewMode()) ? null : selectPos;

       TraxState state = gb.getState();
       gameLog.redrawGameLog(gc, nonDraggingSelect, logRect, highlightColor);
        drawBoardElements(gc, gb, boardRect, ourTurnSelect,nonDraggingSelect);
        zoomRect.draw(gc,nonDraggingSelect);
        DrawChipPool(gc, chipPool, ourTurnSelect);
        GC.setFont(gc,standardBoldFont());
        drawPlayerStuff(gc,(state==TraxState.PUZZLE_STATE),nonDraggingSelect,
	   			HighlightColor, rackBackGroundColor);


		
		boolean planned = plannedSeating();
		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

		for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
		{
	    	 commonPlayer cpl = getPlayerOrTemp(i);
	    	 cpl.setRotatedContext(gc, selectPos, false);
	         int map[]=b.getColorMap();
	         Rectangle r = tileRects[i];
	         int inc = CELLSIZE/5;
	         
	         Rectangle sh = GC.combinedClip(gc,r);
	         drawChip(gc,PlayerIndicators[map[i]],G.Left(r)+inc,G.Top(r)+inc,G.Height(r)*2,0.8,-1);
	         GC.setClip(gc,sh);
	         
	    	 if(planned && (i==gb.whoseTurn))
	    	 {
	    		 handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
	 					HighlightColor, rackBackGroundColor);
	    	 }
	    	 cpl.setRotatedContext(gc, selectPos, true);
	       }
		if (state != TraxState.PUZZLE_STATE)
        {
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);
        }


        if (gc != null)
        {
            standardGameMessage(gc,messageRotation,
            		state==TraxState.GAMEOVER_STATE?gameOverMessage(gb):s.get(state.getDescription()),
            				state!=TraxState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
            Rectangle sh = GC.combinedClip(gc,iconRect);
            int w = G.Width(iconRect);
            drawChip(gc,PlayerIndicators[gb.getColorMap()[gb.whoseTurn]],G.Left(iconRect)+w/8,G.Top(iconRect)+w/8,w,1.8,-1);
            GC.setClip(gc,sh);
            goalAndProgressMessage(gc,nonDraggingSelect,
            		s.get(GoalMessage),progressRect, goalRect);

         }
    
        drawVcrGroup(nonDraggingSelect, gc);
     }

    /**
     * Execute a move by the other player, or as a result of local mouse activity,
     * or retrieved from the move history, or replayed form a stored game. 
     * @param m the parameter is a commonMove so the superclass commonCanvas can
     * request execution of moves in a generic way.
     * @return true if all went well.  Normally G.Error would be called if anything went
     * seriously wrong.
     */
     public boolean Execute(commonMove m,replayMode replay)
    {	
        handleExecute(b,m,replay);						 // let the board do the dirty work
        if(replay.animate) { playSounds(m); }
        return (true);
    }
     void playSounds(commonMove mm)
     {
   	  switch(mm.op)
   	  {
   	  case MOVE_DROPB:
   	  case MOVE_PICKB:
   	  case MOVE_ROTATEB:
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
        return (new Traxmovespec(st, player));
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
    {	switch(nmove.op)
    	{
    	case MOVE_RESET:
    	case MOVE_UNDO:
    	case MOVE_DONTUNDO:
    	case MOVE_ALLOWUNDO: return(null); 
    	default: break;
    	}
        Traxmovespec newmove = (Traxmovespec) nmove;
        Traxmovespec rval = newmove;			// default returned value
        int size = History.size() - 1;
        int idx = size;
        while (idx >= 0)
            {	int start_idx = idx;
                Traxmovespec m = (Traxmovespec) History.elementAt(idx);
                if(m.next!=null) { idx = -1; }
                else {
                switch(newmove.op)
                {
            	case MOVE_DONE:
               	case MOVE_RESIGN:		// resign is your whole move
            	default:
            		idx = -1;
            		break;
            	case MOVE_DROP:
            		if(m.op==MOVE_PICK)
            		{
            		popHistoryElement();
             		rval = null;
            		}
               		idx=-1;
            		break;
            	case MOVE_PICKB:
            		if((m.op==MOVE_DROPB)	// drop but not rotate
            			&& ((m.to_row_after()==newmove.to_row)
                        		&& (m.to_col_after()==newmove.to_col)))
            		{	// drop followed by pick at the same location
            			m.op=MOVE_PICK;
            			m.source = TraxId.find(""+b.pickedObject);
            			rval = null;
            		}
        			idx = -1;
        			break;
            	case MOVE_ROTATEB:
            	case MOVE_DROPB:
            		if(m.op==MOVE_PICK) { popHistoryElement();	}
            		else if (((m.op==MOVE_DROPB)||(m.op==MOVE_ROTATEB))
                			&& ((m.to_row_after()==newmove.to_row)
                            		&& (m.to_col_after()==newmove.to_col)))
            		{
                   	// drop followed by another drop on the same location, convert the first drop to the second type
                	m.source = newmove.source;
                	rval = null;	// remove the new move
              		}
            		idx = -1;
            		break;
                }
            idx--;
            }
            G.Assert(idx!=start_idx,"progress on edit history");
         }
        
 
        return (rval);
    }

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof TraxId)// not dragging anything yet, so maybe start
        {

       	TraxId hitObject =(TraxId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: 
	    case TileCorner:
        case ZoomSlider:
        case DragBoard:
        case InvisibleDragBoard:
        	break;
        case hitTile0:
        case hitTile1:
        case hitTile2:
        case hitTile3:
        case hitTile4:
        case hitTile5:
        	PerformAndTransmit("Pick "+hitObject.shortName);
        	break;
	    case BoardLocation:
	    	PerformAndTransmit("Pickb "+hp.col+" "+hp.row);
	    	break;
        }
        }
    }
	private void doDropChip(String op,char col,int row,char chip)
	{	TraxState state = b.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case CONFIRM_STATE:
		case PLAY_STATE:
		case ILLEGAL_MOVE_STATE:
		case PUZZLE_STATE:
		{
			char st = b.nextColorMatch(col,row,chip); 
			PerformAndTransmit(op+" "+st+" "+col+" "+row);
		}
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
        if(!(id instanceof TraxId)) {   missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
    	missedOneClick = false;
    	TraxId hitCode= (TraxId)hp.hitCode;
		TraxState state = b.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s" , hitCode);
         case ZoomSlider:
        case InvisibleDragBoard:
        case DragBoard: break;
        case TileCorner:
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
	        case ILLEGAL_MOVE_STATE:
			case CONFIRM_STATE:
			case PLAY_STATE:
				if(!b.isDest(hp.col,hp.row))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					throw G.Error("shouldn't hit a chip in state %s",state);
					}
				// fall through and pick up the previously dropped piece
				//$FALL-THROUGH$
			case PUZZLE_STATE:
				char ch = (char)(1+b.GetBoardPos(hp.col,hp.row));
				if(ch=='6') { ch='0'; }
		       	doDropChip("rotateb",hp.col,hp.row,ch);
			}
			break;
       case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
				if(!b.isDest(hp.col,hp.row))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					throw G.Error("shouldn't hit a chip in state %s",state);
					}
				// fall through and pick up the previously dropped piece
				//$FALL-THROUGH$
			case PUZZLE_STATE:
				PerformAndTransmit("Pickb "+hp.col+" "+hp.row);
				break;
			}
			break;
			
        case EmptyBoard:
			switch(state)
			{
				default:
					throw G.Error("Not expecting EmptyBoard in state %s",state);
				case GAMEOVER_STATE:
				case RESIGN_STATE:
					break;
				case ILLEGAL_MOVE_STATE:
				case CONFIRM_STATE:
				case PLAY_STATE:
				case PUZZLE_STATE:
					{
					int mo = b.movingObjectIndex();
					if(mo<0) { mo = b.lastPicked(); }
					if(mo<0) { mo=0; }
					char moc = (char)('0'+mo);
					doDropChip("dropb",hp.col,hp.row,moc);
					}
					break;
			}
			break;
		
        case hitTile0:
        case hitTile1:
        case hitTile2:
        case hitTile3:
        case hitTile4:
        case hitTile5:
        	PerformAndTransmit("Drop "+hitCode.shortName);
            break;


        }
        }
    }

   public String gameType() { return(b.gametype); }
   public String sgfGameType() { return(Trax_SGF); }
   public void performHistoryInitialization(StringTokenizer his)
   {   //the initialization sequence
   	String token = his.nextToken();
    b.doInit(token);
   }


    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
        if (target == useClassicItem)
        {	handled=true;
        	useClassic = useClassicItem.getState();
        }
 
        return (handled);
    }

    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() { return(new TraxPlay()); }
    
    public boolean parsePlayerExecute(commonPlayer p,String first,StringTokenizer tokens)
    {	// handle older games that don't have "done" after every move
    	boolean isDone = "done".equals(first);
    	if(!isDone && b.DoneState() && (p.boardIndex!=b.whoseTurn))
    	{	PerformAndTransmit("Done",false,replayMode.Replay);
    	}
    	String msg = first + " "+ G.restof(tokens);
        return(PerformAndTransmit(msg, false,replayMode.Replay));	
    }
    
    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/23/2023	
     * 	21992 files visited 0 problems
     */
    public void ReplayMove(sgf_node no)
    {
        String comments = "";
        sgf_property prop = no.properties;

        while (prop != null)
        {
            String name = prop.getName();
            String value = (String) prop.getValue();

            //System.out.println("prop " + name + " " + value);
            if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (name.equals(game_property) && value.equalsIgnoreCase("trax"))
            {// the equals sgf_game_type case is handled in replayStandardProps
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
