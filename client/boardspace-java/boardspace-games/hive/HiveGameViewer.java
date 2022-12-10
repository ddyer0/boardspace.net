package hive;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

/* below here should be the same for codename1 and standard java */
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
 * June 2006  initial work in progress.  

*/
public class HiveGameViewer extends CCanvas<HiveCell,HiveGameBoard> implements HiveConstants, GameLayoutClient,PlacementProvider
{       
    // file names for jpeg images and masks
    static final String ImageDir = "/hive/images/";


    static final int BACKGROUND_TILE_INDEX=0;
    static final int YELLOW_FELT_INDEX = 1;
    static final int BROWN_FELT_INDEX = 2;
    static final int LIFT_ICON_INDEX = 3;
    static final String TextureNames[] = { "background-tile" ,"yellow-felt-tile","brown-felt-tile","lifticon"};

	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color logrectHighlightColor = new Color(0.9f,0.9f,0.3f);
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    private Color rackBackGroundColor = new Color(215,197,157);
    private Color rackActiveColor = new Color(225,207,127);		// done button when active
     
    private Color chatBackgroundColor = new Color(240,230,210);
    private Font gameLogBoldFont=null;
    private Font gameLogFont = null;
    // images
    private static Image[] textures = null;// background textures
    // private state
    private HiveGameBoard b = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private int RACKSCALE;
    private final double INITIAL_TILE_SCALE = 3.0;
    private double BOARD_TILE_SCALE = 4.0;
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
     private Rectangle repRect = addRect("repRect");
    private NumberMenu numberMenu = new NumberMenu(this,HivePiece.BugIcon,HiveId.ShowNumbers);
    ;
    private Rectangle idRects[] = addRect("id",2);
    private Rectangle[]chipRects = addRect("chip",2);
    private Rectangle[]setupRects = addRect("setup",2);
    private Rectangle tilesetRect = addRect("tilesetRect");
    private Rectangle reverseRect = addRect("reverseRect");
    private Toggle seeMobile = new Toggle(this,"eye",StockArt.Eye,HiveId.SeeMovable,true,"See Movable Pieces");
 
    private int tileColorSet = 0;
    public int getAltChipset() 
    	{ return(tileColorSet); }
    private JCheckBoxMenuItem textNotation = null;
    public boolean useTextNotation = false;
    private JMenuItem offerDrawAction = null;

    public boolean reverse_y() { return(b.reverseY());}
    
    public boolean WinForPlayer(commonPlayer p)
    {
    	return((p==null)?false:(b.swappedWinForPlayer(p.boardIndex)));
    }
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
    	if(target==offerDrawAction)
    	{	if(OurMove() 
    			&& (b.movingObjectIndex()<=0)
    			&& ((b.getState()==HiveState.PLAY_STATE)||(b.getState()==HiveState.DrawPending)))
    		{
    		if(b.canOfferDraw())
    			{
    			PerformAndTransmit(OFFERDRAW);
    			}
    		else { G.infoBox(null,s.get(DrawNotAllowed)); }
    		}
    	return(true);
    	}
    	if(numberMenu.selectMenu(target,this)) { return(true); }
    	else if(target==textNotation)
        {
        	handled = true;
           	useTextNotation = textNotation.getState();
        	generalRefresh();
        }


        return (handled);
    } 
    public synchronized void preloadImages()
    {	HivePiece.preloadImages(loader,ImageDir);
	    if (textures == null)
	        { // note that dfor this to work correctly, the images and masks must be the same size.  
	          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
	            textures = loader.load_images(ImageDir,TextureNames);
	        }
        gameIcon = HivePiece.gameIcon;
    }
    private Font pieceLabelFont = largeBoldFont();
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
        int FontHeight = standardFontSize();
        gameLogBoldFont = G.getFont(standardPlainFont(), G.Style.Bold, FontHeight+4);
        gameLogFont = G.getFont(standardPlainFont(),G.Style.Plain,FontHeight+2);
        pieceLabelFont = G.getFont(largeBoldFont(),FontHeight*2);
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),HiveId.ZoomSlider);
        zoomRect.min=1.5;
        zoomRect.max=5.0;
        zoomRect.value=INITIAL_TILE_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = HighlightColor;
 
        b = new HiveGameBoard(info.getString(GAMETYPE, "Hive"),getStartingColorMap());
        useDirectDrawing(true);
        textNotation = myFrame.addOption(TextLogMessage,false,deferredEvents);
        offerDrawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);    
        if(G.debug()) {
        	HiveConstants.putStrings();
        }
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
        	 zoomRect.setValue(INITIAL_TILE_SCALE);
        	 board_center_x = board_center_y = 0.0;
             startFirstPlayer();
       	}
   }
 
    int minLogRows = 10;
    private int rackCells() { return ((b.numActivePieceTypes()+1)*2); }
    private boolean flatten = false;
    public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit) 
    {	commonPlayer pl0 = getPlayerOrTemp(player);
    	pl0.displayRotation = rotation;
    	int rackWidth = rackCells()*unit;
    	int C2 = unit/2;
    	int rackH = 3*unit-C2;
    	int chipW = unit*3;
    	int setupRectHeight = (b.gamevariation==variation.hive_u) ? rackH-unit : 0;
    	Rectangle idRect = idRects[player];
    	Rectangle chipRect = chipRects[player];
    	Rectangle setupRect = setupRects[player];
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()?unit*5:0;
    	boolean chipsRight = flatten;
    	boolean chipsBelow = !flatten;
    	
    	Rectangle box = pl0.createRectangularPictureGroup(x+chipW,y,unit);
    	
    	G.SetRect(done, G.Right(box)+C2, y, doneW,doneW/2);
    	G.SetRect(idRect, x, y, chipW, chipW);
    	
    	int chipX = chipsRight ? G.Right(done)+unit/4 : x;
    	int chipY = chipsRight 
    			? y 
    			: chipsBelow ? (C2+G.Bottom(box)) : y-rackH-C2;

    	// a pool of chips for the first player at the top
    	G.SetRect(chipRect,
        		chipX,
        		chipY, 
        		rackWidth,rackH);

    	G.SetRect(setupRect,chipX,chipY+(chipsBelow?rackH:-setupRectHeight),
        		rackWidth,setupRectHeight);
       
    	G.union(box,chipRect,done,idRect,setupRect);
    	return(box);
    	
    }
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	setLocalBoundsV(x,y,width,height,aspects);
    }
    static double aspects[] = { 0.8,1.4,1,-0.8,-1.4,-1};	// negative values encode "flattened" player group
    
    public double setLocalBoundsA(int x, int y, int width, int height,double aspect0)
    {	flatten = aspect0<0;
    	double aspect = Math.abs(aspect0);
    	G.SetRect(fullRect, x, y, width, height);
		int fh = standardFontSize();
    	double zoom = getGlobalZoom();
   
    	gameLogBoldFont = G.getFont(standardPlainFont(), G.Style.Bold, (int)(zoom*(fh)));
    	gameLogFont = G.getFont(standardPlainFont(),G.Style.Plain,(int)(zoom*(fh)));
    	GameLayoutManager layout = selectedLayout;
    	int maxDim = (int)(Math.min(height,width)/(zoom*G.getDisplayScale()));
    	// the intended effect of this is to allow the tiles in the tile rack
    	// to shrink as the board gets small
    	double position = Math.min(1,Math.max(0, maxDim-600)/(double)(1024-600));
    	double tileScale = G.interpolateD(position,1.5,3);
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	//int fh = standardFontSize();
    	int minLogW = fh*15;	
       	int minChatW = fh*40;	
        int minLogH = fh*14;	
        int margin = fh/2;
        int buttonW = fh*8;
        layout.strictBoardsize = false;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent =
    	layout.selectLayout(this, nPlayers, width, height,
    			zoom,
    			margin,	
    			0.8,	// 60% of space allocated to the board
    			aspect,	// aspect ratio for the board
    			fh*tileScale,	// minimum cell size
    			fh*tileScale*4/3,	// maximum cell size
    			0.2		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
    	//layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);


    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	
    	// calculate a suitable cell size for the board
    	CELLSIZE = fh*3;;
        int zoomW = CELLSIZE*5;
        int C4 = CELLSIZE/4;
        RACKSCALE = CELLSIZE*4;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
        //
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = mainY;
        int stateX = mainX;
        int stateH = CELLSIZE;
        int boardY = stateY+stateH+C4;
        int boardBottom = G.Bottom(main)-stateH-C4;
        int boardH = boardBottom-boardY;
        G.placeRow(stateX+stateH,stateY,mainW-stateH,stateH,stateRect,reverseRect,liftRect,seeMobile,noChatRect);
        G.placeRow(stateX,boardBottom+C4,mainW,stateH,goalRect,numberMenu,tilesetRect);
        
        G.placeRight(stateRect, zoomRect, zoomW);

    	G.SetRect(boardRect,mainX,boardY,mainW,boardH);
    	
    	int vcrW = CELLSIZE*6;
    	int vcrH = vcrW/2;
    	
    	SetupVcrRects(mainX+C4/2,boardBottom- vcrH, vcrW,vcrH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        return(mainW*boardH);
    } 

    private void DrawTilesetRect(Graphics gc,HitPoint highlight)
    {	
    	if(G.pointInRect(highlight,tilesetRect))
    	{	
    		highlight.hitCode = HiveId.TilesetRect;
    		highlight.spriteRect = tilesetRect;
    		highlight.spriteColor = Color.red;
   		
    	}
		if(gc!=null) 
		{ HivePiece ch = HivePiece.getCanonicalChip(HiveId.White_Bug_Pool,PieceType.QUEEN).getAltChip(1);
		  ch.drawChip(gc,this,G.Width(tilesetRect)*3,G.centerX(tilesetRect),G.centerY(tilesetRect),null);
		  GC.frameRect(gc,Color.black,tilesetRect);
		}
    }
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void DrawSetupPool(Graphics gc, HiveGameBoard gb, HiveState state,Rectangle r, int player,HitPoint highlight,HiveCell cells[])
    {
    	commonPlayer pl = getPlayerOrTemp(player);
    	pl.setRotatedContext(gc, highlight, false);
        int nCells = gb.numActivePieceTypes();	// omit some if not mosquito variant
        int cellW = G.Width(r)/nCells;
        if(state==HiveState.Swap)
        {	// in swap state, it is always the black player's turn.  White has
        	// made the piece selections, now black decides to play black or white
        	//
        	commonPlayer blackPlayer = getPlayerOrTemp(0);
        	boolean whiteTurn = player!=gb.swappedPlayer(0);
        	double rot = player==gb.whoseTurn ? 0 : blackPlayer.displayRotation; 
        	if(whiteTurn)
        	{
        	if (GC.handleRoundButton(gc,rot,r, 
        			highlight, s.get(PlayBlackAction),
                    HighlightColor, gb.colorsInverted()?rackBackGroundColor:HighlightColor))
            {	// always display the done button, but only make it active in
            	// the appropriate states
            	highlight.hitCode = HiveId.HitPlayBlackAction;
            }}
        	else
        	{
           	if (GC.handleRoundButton(gc, rot,r, 
           			highlight,s.get(PlayWhiteAction),
                    HighlightColor, gb.colorsInverted()?HighlightColor:rackBackGroundColor))
            {	// always display the done button, but only make it active in
            	// the appropriate states
           		highlight.hitCode = HiveId.HitPlayWhiteAction;
            }}
        }
        else	// display the chips
        {
        boolean canhit = gb.LegalToHitChips(player) && G.pointInRect(highlight, r);
        boolean canDrop = canhit && (gb.movingObjectIndex()>=0);
        int cellH = G.Height(r);
        int midr = G.centerY(r);
		int pieceSize = RACKSCALE = (int)(cellW*3.5);
       int cellIndex = 0;
       for(PieceType pt : PieceType.values())
        {	if(gb.pieceTypeIncluded.test(pt))
        	{
         	int cellX = (G.Width(r)-(cellW*nCells))/2+cellW*cellIndex;
        	HiveCell thisCell = cells[pt.ordinal()];
           	cellIndex++;

        	if(thisCell!=null) 
        	{
	    		int left = G.Left(r)+cellX+cellW/2;
	    		int height=thisCell.height();
         		if(canhit 
           				&& (canDrop ? (pt==gb.movingObjectType()): (height>0))
           				&& thisCell.pointInsideCell(highlight,left,midr,cellW,cellH)
           				)
        		{ 
        		highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        		highlight.awidth = CELLSIZE;
        		highlight.spriteColor = Color.red;
        		highlight.spriteRect = new Rectangle(left-cellW/2,midr-cellH/2,cellW,cellH);
        		}
    		
        		for(int bug=height; bug>0;bug--)
        		{
        		HivePiece topCup = thisCell.chipAtIndex(height-bug);
        		if(topCup!=null)
        			{ 
        			pl.rotateCurrentCenter(thisCell, left,midr);
        			topCup.drawChip(gc,this,pieceSize,left, midr,null);
        			}
        		}
        		//if((gc!=null)&&(hitCell==thisCell)) { G.frameRect(gc,Color.red,left-cellW/2,top-CELLSIZE/2,cellW,2*CELLSIZE); }
           	if(((thisCell==gb.pickedSource)||(thisCell==gb.droppedDest)))
        	{ 
            GC.cacheAACircle(gc,left,G.Top(r)+CELLSIZE/2,2,Color.green,Color.yellow,true);
            }
        	}
        	}
        }
        }
        
        pl.setRotatedContext(gc, highlight, true);
        
    }
    
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void DrawChipPool(Graphics gc, HiveGameBoard gb,HiveState state,Rectangle id,Rectangle r,Rectangle done, int player,HitPoint highlight,HiveCell cells[])
    {	
    	commonPlayer pl = getPlayerOrTemp(player);
    	pl.setRotatedContext(gc, highlight, false);
    	HivePiece idbug = HivePiece.getCanonicalChip(gb.playerColor(player),PieceType.QUEEN);
    	idbug.drawChip(gc,this,G.Width(id)*2,G.centerX(id),G.centerY(id),null);
        int nCells = gb.numActivePieceTypes();	// omit some if not mosquito variant
        GC.frameRect(gc, Color.black, r); 
        boolean canhit = gb.LegalToHitChips(player) && G.pointInRect(highlight, r);
        boolean canDrop = canhit && (gb.movingObjectIndex()>=0);
        int cellW = G.Width(r)/nCells;
        int cellH = G.Height(r);
        int midr = G.centerY(r);
        int pieceSize = RACKSCALE = (int)(cellW*3.5);
        int cellIndex = 0;
		int baseY = G.Bottom(r)-(int)(CELLSIZE*0.6);
		for(PieceType pt : PieceType.values())
        {	if(gb.pieceTypeIncluded.test(pt))
        	{
         	int cellX = (G.Width(r)-(cellW*nCells))/2+cellW*cellIndex;
        	HiveCell thisCell = cells[pt.ordinal()];
           	cellIndex++;

        	if(thisCell!=null) 
        	{
	    		int left = G.Left(r)+cellX+cellW/2;
	    		int height=thisCell.height();
         		if(canhit 
           				&& ((state==HiveState.QUEEN_PLAY_STATE) ? (pt==PieceType.QUEEN) : true)
           				&& (gb.canPlayQueen(player) ? true : (pt!=PieceType.QUEEN))
           				&& (canDrop ? (pt==gb.movingObjectType()): (height>0))
           				&& thisCell.pointInsideCell(highlight,left,midr,cellW,cellH)
           				)
        		{
        		highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        		highlight.awidth = CELLSIZE;
        		highlight.spriteColor = Color.red;
        		highlight.spriteRect = new Rectangle(left-cellW/2,midr-cellH/2,cellW,cellH);
        		}
        		for(int bug=height; bug>0;bug--)
        		{
        		HivePiece topCup = thisCell.chipAtIndex(height-bug);
        		if(topCup!=null)
	    			{ 
	    			int pivot = baseY-CELLSIZE/6*(height-bug);
	    			pl.rotateCurrentCenter(thisCell, left,pivot);
	    			topCup.drawChip(gc,this,pieceSize,left, pivot,null);
	    			}
       		}
        		//if((gc!=null)&&(hitCell==thisCell)) { G.frameRect(gc,Color.red,left-cellW/2,top-CELLSIZE/2,cellW,2*CELLSIZE); }
           	if(((thisCell==gb.pickedSource)||(thisCell==gb.droppedDest)))
        	{ 
            GC.cacheAACircle(gc,left,G.Top(r)+CELLSIZE/2,2,Color.green,Color.yellow,true);
            }
        	}
        	}
        }
       if(plannedSeating() && gb.whoseTurn()==player)
       {	
    	   HitPoint ds = (gb.DoneState() ? highlight : null);
    	   handleDoneButton(gc,done,ds,HighlightColor, rackBackGroundColor);   
       }
   	   pl.setRotatedContext(gc, highlight, true);
    }

    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	boolean inboard = boardRect.contains(xp,yp);
       	int cellS = inboard? (int)(b.cellSize()*1.15*BOARD_TILE_SCALE) :RACKSCALE ;
       	HivePiece p = b.pickedObject;
       	if(p!=null) 
       		{ p.drawChip(g,this,cellS,xp,yp,null); 
       		}
    }
    

    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : rackBackGroundColor);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);   
     textures[reviewBackground ? BROWN_FELT_INDEX:YELLOW_FELT_INDEX].tileImage(gc,
          		boardRect); 
      GC.frameRect(gc,Color.black,boardRect);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method
      //gb.DrawGrid(gc, tbRect, use_grid, boardBackgroundColor, Color.blue, Color.blue,Color.black);

     }

    /* draw the board and the chips on it. */
 
     private void drawBoardElements(Graphics gc, HiveGameBoard gb, Rectangle tbRect,HitPoint ourTurnSelect,HitPoint anySelect)
     {	
     	Rectangle oldClip = GC.combinedClip(gc,boardRect);
     	int csize = gb.cellSize();
     	boolean dolift = doLiftAnimation();
     	boolean see = seeMobile.isOnNow();
     	//
     	// now draw the contents of the board and anything it is pointing at
     	//
     	boolean somehit = draggingBoard();
         
         Hashtable<HiveCell,HiveCell> dests = gb.movingObjectDests();
         HiveCell sourceCell = gb.pickedSource; 
         HiveCell destCell = gb.droppedDest;
         boolean inside = G.pointInsideRectangle(ourTurnSelect,tbRect);
         int cellSize =  (int)(csize*1.15);
         double actCellSize = cellSize * BOARD_TILE_SCALE;
         int liftYval =  cellSize/5+(dolift?(liftSteps*cellSize)/20 : 0);
         int liftXval = dolift?(liftSteps*cellSize)/(2*20) : 0;
         int xo = (int)(cellSize*-0.03);
         int yo = (int)(cellSize*0.15);
         int left = G.Left(tbRect);
         int top = G.Bottom(tbRect);
         numberMenu.clearSequenceNumbers();
         for(Enumeration<HiveCell>cells = gb.getIterator(Itype.TBRL);  cells.hasMoreElements();)
         {
        	 HiveCell cell = cells.nextElement();
        	 int xpos = left+gb.cellToX(cell);
        	 int ypos = top-gb.cellToY(cell);
 
        	 boolean isADest = dests.get(cell)!=null;
        	 boolean isASource = (cell==sourceCell)||(cell==destCell);
        	 HivePiece piece = cell.topChip();
        	 int aaheight = cell.activeAnimationHeight();
        	 int cheight = cell.height()-aaheight;
        	 boolean canHit = gb.LegalToHitBoard(cell);
        	 boolean hitpoint = !somehit
                	&& canHit
                 	&& inside ;
        	 cell.rotateCurrentCenter(gc,xpos,ypos);
        	 
        	 numberMenu.saveSequenceNumber(cell,xpos-xo,ypos-yo);
 
        	 //G.DrawAACircle(gc,xpos,ypos,1,tiled?Color.green:Color.blue,Color.yellow,true);
             if(piece!=null)
             {	
                for(int lvl=0; lvl<cheight; lvl++)
             	{
             	HivePiece drawpiece = cell.chipAtIndex(lvl);
             	String id = use_grid 
             			? drawpiece.exactBugName()//+" "+cell.overland_gradient+"+"+cell.slither_gradient 
             			: G.debug() ? null /*(""+cell.col+cell.row) */: null;
             	int xp = xpos+liftXval*lvl;
             	int yp = ypos-liftYval*lvl;

             	if(hitpoint && cell.closestPointToCell(ourTurnSelect, cellSize,
            				xp,
            				yp))
             		{
             		somehit = true;
             		boolean moving = (gb.movingObjectIndex()>=0);
             		ourTurnSelect.hitCode = HiveId.BoardLocation;
             		ourTurnSelect.hitObject = cell;
             		ourTurnSelect.arrow = moving?StockArt.DownArrow:StockArt.UpArrow;
             		ourTurnSelect.awidth = cellSize/2;
             		ourTurnSelect.spriteColor = Color.red;
             		}
             	
               	drawpiece.drawChip(gc,this,(int)actCellSize, xp, yp,null);
               	if(lvl==cheight-1)
               	{	numberMenu.saveSequenceNumber(cell,xp-xo,yp-yo);
               	}
               	if(id!=null)
             	{
               	GC.setFont(gc,actCellSize>240?largeBoldFont():standardBoldFont());
               	GC.setColor(gc,Color.yellow);
             	StockArt.SmallO.drawChip(gc,this,(int)(actCellSize*0.5),xp,yp,id);
             	}
             	}
             }
             else {
              	if(hitpoint && cell.closestPointToCell(ourTurnSelect, cellSize,
        				xpos,
        				ypos))
         		{
         		somehit = true;
         		boolean moving = (gb.movingObjectIndex()>=0);
         		ourTurnSelect.hitCode = HiveId.BoardLocation;
         		ourTurnSelect.hitObject = cell;
         		ourTurnSelect.arrow = moving?StockArt.DownArrow:StockArt.UpArrow;
         		ourTurnSelect.awidth = cellSize/2;
         		ourTurnSelect.spriteColor = Color.red;
         		
         		}
              	/*
              	{
            	String label = numberMenu.getSequenceString(cell,true);		
             	if(label!=null && cell.lastContents!=null)
             		{
                 	GC.setFont(gc,pieceLabelFont);
             		cell.lastContents.drawChip(gc,this,(int)actCellSize/2, xpos, ypos,label);
             		}
              	}*/
 
             }
             if(isASource || (see && canHit))
             {GC.cacheAACircle(gc,xpos,ypos,2,Color.green,Color.yellow,true);
             } else
             if(isADest)
             {GC.cacheAACircle(gc,xpos,ypos,2,Color.red,Color.yellow,true);
             }
         }
         numberMenu.drawSequenceNumbers(gc,cellSize,pieceLabelFont,labelColor);
         

        
       	doBoardDrag(tbRect,anySelect,csize,HiveId.InvisibleDragBoard);

        GC.setClip(gc,oldClip);
        
    }
 
    public boolean canOfferDraw(HiveGameBoard gb)
    {
        int hsize = History.size();
        long hdig = hsize>1 ? History.elementAt(hsize-2).digest : 0;
        return((repeatedPositions.numberOfRepeatedPositions(hdig)>=2) 
				&& gb.canOfferDraw());
    }

    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  

       HiveGameBoard gb = disB(gc);
   	   if(gc!=null)
   		{
   		// note this gets called in the game loop as well as in the display loop
   		// and is pretty expensive, so we shouldn't do it in the mouse-only case

       gb.SetDisplayParameters(zoomRect.value,1.0,board_center_x,board_center_y,30.0); // shrink a little and rotate 30 degrees
       gb.SetDisplayRectangle(boardRect);
   		}
   	   int whoseTurn = gb.whoseTurn();
       boolean moving = hasMovingObject(selectPos);
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;
       HitPoint buttonSelect = moving?null:ourTurnSelect;
       HitPoint nonDraggingSelect = (moving && !reviewMode()) ? null : selectPos;
       HiveState state = gb.getState();
       gameLog.redrawGameLog(gc, nonDraggingSelect, logRect, Color.black,logrectHighlightColor,gameLogBoldFont,gameLogFont);
       commonPlayer pl = getPlayerOrTemp(whoseTurn);
       double messageRotation = pl.messageRotation(); 		// this is correct because we currently only do face to face layout
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,nonDraggingSelect);

       boolean setup = (gb.gamevariation==variation.hive_u) 
        		&& ((state==HiveState.Setup)||(state==HiveState.Swap)||(state==HiveState.PUZZLE_STATE));
       for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX;i++)
        {
       	DrawChipPool(gc, gb, state, idRects[i],chipRects[i],doneRects[i], i,
   			ourTurnSelect,gb.rackForPlayer(i));
    	if(setup)
        {   DrawSetupPool(gc, gb, state, setupRects[i], i, 
        		ourTurnSelect,gb.setupRackForPlayer(i));
        }
        }
 
        zoomRect.draw(gc,nonDraggingSelect);
        drawLiftRect(gc,liftRect,nonDraggingSelect,textures[LIFT_ICON_INDEX]);
        DrawTilesetRect(gc,nonDraggingSelect);
 
        DrawRepRect(gc,messageRotation,Color.black,gb.Digest(),repRect);
        DrawReverseMarker(gc,reverseRect,nonDraggingSelect,HiveId.ReverseRect);
        numberMenu.draw(gc,nonDraggingSelect);
        seeMobile.draw(gc,nonDraggingSelect);
        switch(state)
        {     
        default:
	    	if(canOfferDraw(gb))
	    	{	// if not making progress, put the draw option on the UI
	        	boolean drawPending = (state==HiveState.DrawPending);
	    		String offer = s.get(OFFERDRAW);
	    		Rectangle acceptDrawRect = chipRects[nextPlayer[whoseTurn]];
	        	if(GC.handleSquareButton(gc,messageRotation,acceptDrawRect,ourTurnSelect,offer,
	        				HighlightColor,
	        				drawPending ? HighlightColor : rackBackGroundColor))
	        	{
	        		ourTurnSelect.hitCode = GameId.HitOfferDrawButton;
	        	}	
	    	}
	    	break;
	    case AcceptOrDecline:
	    case AcceptPending:
	    case DeclinePending:
	    {	String accept = s.get(ACCEPTDRAW);
	    	String decline = s.get(DECLINEDRAW);
	    	Color hitAccept = (state==HiveState.AcceptPending)?HighlightColor:rackBackGroundColor;
	    	Color hitDecline = (state==HiveState.DeclinePending) ? HighlightColor : rackBackGroundColor; 
	    	Rectangle acceptDrawRect = chipRects[whoseTurn];
	    	Rectangle declineDrawRect = chipRects[nextPlayer[whoseTurn]];
	    	if(GC.handleSquareButton(gc,messageRotation,acceptDrawRect,ourTurnSelect,accept,HighlightColor,hitAccept))
	    	{
	    		ourTurnSelect.hitCode = GameId.HitAcceptDrawButton;
	    	}
	    	if(GC.handleSquareButton(gc,messageRotation,declineDrawRect,ourTurnSelect,decline,HighlightColor,hitDecline))
	    	{
	    		ourTurnSelect.hitCode = GameId.HitDeclineDrawButton;
	    	}
        }}
        GC.setFont(gc,standardBoldFont());
		if (state != HiveState.PUZZLE_STATE)
        {	HitPoint ds = (gb.DoneState() ? buttonSelect : null);
        	if(!autoDoneActive() && !plannedSeating())
        		{ handleDoneButton(gc,messageRotation,doneRect,ds,HighlightColor, 
        				ds==null ? rackBackGroundColor : rackActiveColor); //215,197,157
        		}
            // passing an explicit rotation is correct because we only seat face to face
            handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor,rackBackGroundColor);
        }

		drawPlayerStuff(gc,(state==HiveState.PUZZLE_STATE),nonDraggingSelect,
				HighlightColor, rackBackGroundColor);
		
		
        standardGameMessage(gc,messageRotation,
            		state==HiveState.GAMEOVER_STATE?gameOverMessage():s.get(state.getDescription()),
            				state!=HiveState.PUZZLE_STATE,
            				whoseTurn,
            				stateRect);
    	HivePiece idbug = HivePiece.getCanonicalChip(gb.playerColor(whoseTurn),PieceType.QUEEN);
    	int h = G.Height(stateRect);
    	idbug.drawChip(gc, this, h*3, G.Left(stateRect)-h/2, G.centerY(stateRect),null);
        goalAndProgressMessage(gc,nonDraggingSelect,s.get(HiveGoal),progressRect, goalRect);
        
        drawVcrGroup(nonDraggingSelect, gc);

    }

    public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode mode)
    {	// the super method in commonCanvas is where the history is actually recorded
       	if((b.getState()==HiveState.PASS_STATE) 
       			&& (m.op==MOVE_DONE) 
       			&& OurMove()
       			&& (mode==replayMode.Live))
    	{
         PerformAndTransmit(PASS,true,mode); 
    	}
       	if(m.op==MOVE_RESET && mode==replayMode.Replay) 
       		{ // this shouldn't occur, but there are a few damaged games
       		  return(true); 
       		} 
       	boolean val =  super.PerformAndTransmit(m,transmit,mode);
        return(val);
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
        handleExecute(b,m,replay);
        lastDropped = b.lastDroppedObject;
        int cellSize =  (int)((b.cellSize()*1.15)*BOARD_TILE_SCALE);
        startBoardAnimations(replay,b.animationStack,cellSize,MovementStyle.Chained);
        
        if(replay!=replayMode.Replay) { playSounds(m); }
         return (true);
    }

     void playSounds(commonMove mm)
     {
    	 switch(mm.op)
    	 {
    	 case MOVE_PMOVE:
    		 playASoundClip(swish,100);
    		 break;
    	 case MOVE_MOVE:
    		 playASoundClip(light_drop,100);
    		 //$FALL-THROUGH$
    	 case MOVE_DROPB:
    	 case MOVE_PDROPB:
    	 case MOVE_PICKB:
    	 case MOVE_PICK:
    	 case MOVE_DROP:
    		 playASoundClip(light_drop,200);
    		 break;
    	 default: break;
    	 }
     }
     
     // this method is needed for hive to maintain compatibility with old game records
     public boolean parsePlayerExecute(commonPlayer p,String first,StringTokenizer tokens)
     {
     	String msg = first + " "+ G.restof(tokens);
     	Hivemovespec m = new Hivemovespec(msg,p.boardIndex);
         return(PerformAndTransmit(m, false,replayMode.Replay));	
     }

/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new Hivemovespec(st, player));
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
    {	
    	if((nmove.op==MOVE_PICK)||(nmove.op==MOVE_PICKB))
    	{
    	Hivemovespec prev = (Hivemovespec)History.top();
    	// this shouldn't happen, but it is a repair for damaged games
    	if((prev!=null) && nmove.Same_Move_P(prev))
    		{ return(null); }
    	}
    	return(EditHistory(nmove,(nmove.op==MOVE_PASS)||(nmove.op==MOVE_PLAYWHITE)||(nmove.op==MOVE_PLAYBLACK))); 
    }

    

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof HiveId) // not dragging anything yet, so maybe start
        {
        HiveId hitObject = (HiveId)hp.hitCode;
		HiveCell cell = hitCell(hp);
		HivePiece bug = (cell==null) ? null : cell.topChip();
		switch(hitObject)
	    {
	    default: break;
        case ReverseRect:
          	 b.setReverseY(!b.reverseY());
          	 generalRefresh();
          	 break;
        case InvisibleDragBoard:
        	break;
		case ZoomSlider:
 	    case Black_Bug_Pool:
	    case Black_Setup_Pool:
	    case White_Setup_Pool:
	    case White_Bug_Pool:
	    	if(bug!=null)
	    	{
	    	PerformAndTransmit("Pick "+cell.rackLocation().shortName+" "+cell.row+" "+bug.exactBugName());
	    	}
	    	break;
	    case BoardLocation:
	    	if(bug!=null)
	    	{
	    	String name = bug.exactBugName();
	       	PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+name);
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
        if(!(id instanceof HiveId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	HiveId hitObject = (HiveId)hp.hitCode;
        HiveState state = b.getState();
		HiveCell cell = hitCell(hp);
		HivePiece bug = (cell==null) ? null : cell.topChip();
       	
		
		switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case ShowNumbers:
        	numberMenu.showMenu();
        	break;
        case SeeMovable:
        	seeMobile.toggle();
        	break;
        case HitPlayBlackAction:
        	PerformAndTransmit("PlayBlack");
        	break;
        case HitPlayWhiteAction:
        	PerformAndTransmit("PlayWhite");
        	break;
        case TilesetRect:
        	tileColorSet = tileColorSet^1;
        	break;
        case ReverseRect:	
        case ZoomSlider:
        case InvisibleDragBoard:
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case FIRST_PLAY_STATE:
			case QUEEN_PLAY_STATE:
			case Setup:
			case PUZZLE_STATE:
			{	if(cell!=null)
				{
				HivePiece obj = b.pickedObject;
				if(obj!=null)
				{ 	// there's an interaction with the robot "evaluator" option if pdropb is used indescriminately
					String drop = ((state==HiveState.PUZZLE_STATE)
										||(obj.color!=b.playerColor(b.whoseTurn)))?"PDropb " : "Dropb ";
					
					PerformAndTransmit(drop+obj.exactBugName()+" "+cell.col+" "+cell.row+" "+Hivemovespec.attachment(cell,null));

				}
				else if(bug!=null)
				{
					PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+bug.exactBugName());					
				}}
				break;
			}}
			break;
			
        case Black_Bug_Pool:
        case White_Bug_Pool:
        case Black_Setup_Pool:
        case White_Setup_Pool:
    	{
        	HivePiece mov = b.pickedObject;
            if(mov!=null) 
			{//if we're dragging a chip around, drop it.

            		if(cell!=null)
            			{
            			String col = hitObject.shortName;
            			PerformAndTransmit("Drop "+col+" "+cell.row+" "+mov.exactBugName()+" Rack");
            			}
 			}
        	}
            break;
        }}
    }

    // return what will be the init type for the game
    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Hive_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        b.doInit(token);
        //PerformAndTransmit(reviewOnly?"Edit":"Start P0", false,true);
     }



    public BoardProtocol getBoard()   {    return (b);   }
    

    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() { return(new HivePlay()); }

    public void ReplayGame(sgf_game ga)
    {
    	super.ReplayGame(ga);
    	resetBounds();
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     */
    public void ReplayMove(sgf_node no)
    {
        String comments = "";
        int trueval = 0;
        sgf_property prop = no.properties;

        while (prop != null)
        {
            String name = prop.getName();
            String value = (String) prop.getValue();

            //System.out.println("prop " + name + " " + value);
            if (setup_property.equals(name))
            {
                b.doInit(value);
             }
            else if (name.equals(comment_property))
            {
                if(value.startsWith("Visits: "))
                {	// saved tree move value
                	trueval = G.IntToken(value.substring(8));
                }
                else { comments += value; }
            }
            else if (name.equals(game_property) && value.equalsIgnoreCase("hive"))
            {	// the equals sgf_game_type case is handled in replayStandardProps
               
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
        if(trueval>0)
        {
        	commonMove m = History.top();
        	m.setEvaluation(trueval);
        }
    }

	public int getLastPlacement(boolean empty) {
		return b.lastPlacement;
	}


}

