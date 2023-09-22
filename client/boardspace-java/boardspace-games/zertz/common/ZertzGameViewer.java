/* copyright notice */package zertz.common;

import java.awt.*;
import java.util.Enumeration;
import java.util.StringTokenizer;

import lib.Graphics;
import lib.CellId;
import lib.DefaultId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.PopupManager;
import lib.StockArt;
import lib.TextButton;
import online.common.*;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;


/*
Change History

Feb 29 2004  Iniital work in progress, support for Zertz
Oct 2004 Added simple variations branching and scrolling
  Split some sharable elements into commonCanvas
*/
public class ZertzGameViewer extends CCanvas<zCell,GameBoard> implements GameConstants, GameLayoutClient,PlacementProvider
{
    /**
	 * 
	 */
    static final String ImageDir = "/zertz/images/";
	// things you can point at.  >=0 is a ball.
	static final double JITTER = 0.03;
    static final Color zMouseColors[]= { Color.green, Color.yellow };
    static final Color zMouseDotColors[] =  { Color.black, Color.black };
    static final Color reviewModeBackground = new Color(1.0f, 0.90f, 0.90f);
    static final Color ButtonColor = new Color(0.5f, 0.5f, 1.0f);
    static final Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    static final Color MovableRingColor = new Color(0.5f, 0.6f, 0.9f);
    static final Color RingOutlineColor = new Color(136, 216, 240);//128, 183, 195);
    static final Color RingTextColor = Color.black;
    static final Color rackBackGroundColor = new Color(153, 217, 228);
    static final Color boardBackgroundColor = new Color(210, 238, 255);
    static final Color chatColor = new Color(243, 248, 255);
    
    static final Color[] BallFillColors = { Color.white, Color.gray, Color.black };
    static final Color[] BallOutlineColors = 
        {
            rackBackGroundColor, rackBackGroundColor, rackBackGroundColor
        };
    static final Color[] BallTextColors = { Color.black, Color.white, Color.white };

    // vcr stuff
    static final Color vcrButtonColor = new Color(0.7f, 0.7f, 0.75f);
    // private state
    GameBoard b = null; //the board from which we are displaying
    public int CELLSIZE; //size of the layout cell
    public int RINGRADIUS; //ring radius, about CELLSIZE/2
    public int BALLRADIUS; //ball raduis, about RINGRADIUS-2;
    //private Rectangle boardRect = addRect("boardRect");
    private Rectangle reserveRect = addRect("reserveRect");
    private Rectangle variationRect = addRect("variationRect");
    public Rectangle rackRects[] = addRect("rack",2);
    
    private TextButton swapButton = addButton(SwapFirst,GameId.HitSwapButton,SwapFirst,
			HighlightColor, rackBackGroundColor);
    private Rectangle repRect = addRect("repRect");
    private static int HitNoWhere = -101;
    // stuff for tracking ball clicking and dragging
    private char highlightBoardCol;
    private int highlightBoardRow;
    private ZertzId highlightRackIndex;

    // whem moving, these remember the object we're dragging around
    private int movingObject = HitNoWhere; // >=0 iff a ball is moving
    private ZertzId movingFromRackIndex; //>=0 iff moving from a rack
    private char movingFromBoardCol; // x and y if moving from a board
    private int movingFromBoardRow;


    public BoardProtocol getBoard()
    {
        return (b);
    }


    public int getMovingObject(HitPoint highlight)
    {
        return (movingObject
        		+ ((movingObject>=0)
        		   ? (movingFromBoardCol*10
        			  +movingFromBoardRow*1000
        		      +(movingFromRackIndex.rackIndex())*100000)
       		      : 0)); // this is transmitted to players
    }
    
    public synchronized void preloadImages()
    {	zChip.preloadImages(loader,ImageDir);
    	gameIcon = zChip.Icon.image;
    }
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
        MouseColors = zMouseColors;
        MouseDotColors = zMouseDotColors;

        b = new GameBoard(info.getString(GAMETYPE, "Zertz"));
        useDirectDrawing(true);
        doInit(false);
   }

    /* used when loading a new game */
    private boolean hasDones=false;
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);
        b.doInit(b.gametype);
        hasDones=false;		// for use by replayMove
        if(!preserve_history)
    	{ if(b.variation!=Zvariation.Zertz_h) { startFirstPlayer(); }
    		else 
    		{ PerformAndTransmit(EDIT,false,replayMode.Replay); 
    		}
    	}
   }
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	setLocalBoundsV(x,y,width,height,aspects);
    }
    double aspects[] = {0.7,1.2,1.6};
    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
        int stateH = fh*3;
    	int minLogW = fh*18;	
    	int vcrw = fh*16;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// % of space allocated to the board
    			aspect,	// aspect ratio for the board
    			fh*2.5,	// minimum cell size
    			fh*3.5,
    			0.2		// preference for the designated layout, if any
    			);
    	
//        layout.placeRectangle(reserveRect, 

        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
       	Rectangle rack = rackRects[1];
       	G.copy(swapButton,rack);
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);
    	if(reviewOnly || (b.variation==Zvariation.Zertz_h))
    		{ layout.placeRectangle(variationRect, vcrw, vcrw/4,BoxAlignment.Edge);
    		}
    	else { G.SetHeight(variationRect,0); }
    	
      	//layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// There are two classes of boards that should be rotated. For boards with a strong
    	// "my side" orientation, such as chess, use seatingFaceToFaceRotated() as
    	// the test.  For boards that are noticeably rectangular, such as Push Fight,
    	// use mainW<mainH
        int nrows = b.nrows + 2;
        int ncols = b.ncols + 5;
        
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
        RINGRADIUS = CELLSIZE / 2; //ball radius to work with
        BALLRADIUS = (int) (RINGRADIUS * 0.7);
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)((ncols-3)*CELLSIZE);
    	int rackW = 3*CELLSIZE;
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW-rackW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY+CELLSIZE/2,boardW,boardH);
    	
    	G.SetRect(reserveRect,boardX+boardW,boardY+CELLSIZE,3*CELLSIZE,boardH-2*CELLSIZE);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatColor,rackBackGroundColor);
        return((boardW+rackW)*boardH);
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = rackRects[player];
    	Rectangle box =  pl.createRectangularPictureGroup(x,y,unitsize);
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*4 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	int chipw = unitsize*12;
    	if(doneW>0)
    		{  chipw = Math.max(chipw,G.Right(done)-x); 
    		}
    	G.SetRect(chip,x,G.Bottom(box),chipw,unitsize*3/2);
    	G.union(box, done,chip);
    	pl.displayRotation = rotation;
    	return(box);
    }
 

    public void drawTile(Graphics g, int X, int Y, int radius, String contents,
        Color fillColor, Color bgcolor, Color textColor)
    {
        drawBlankTile(g, X, Y, radius, fillColor, bgcolor);

        if (contents != null)
        {
            GC.Text(g, true, X - radius, Y - radius, radius * 2, radius * 2,
                textColor, null, contents);
        }
    }


    //
    // draw balls in a rack.  If gc!=null actually draw them,
    // otherwise just notice of the highlight point is nearby
    //
    void DrawBalls(Graphics gc, GameBoard bd, Rectangle R0, int rackindex,
        HitPoint highlight)
    { // draw a row of balls, player balls or reserve balls
    	Rectangle R = R0;
    	int w = G.Width(R);
    	int h = G.Height(R);
    	int size = Math.min(h, CELLSIZE);
    	boolean horizontal = w>h;
    	boolean isReserve = rackindex==RESERVE_INDEX;
    	int l = G.Left(R);
    	int at = G.Top(R);
    	int t = horizontal ?at :  G.Bottom(R) ;
    	double xscale = 0.79;
        int[] balls = bd.balls[rackindex];
        
    	if(!isReserve)
    	{
    		h = size;
    		w = Math.min(size*10, w);
    		int tot = balls[0]+balls[1]+balls[2]; 
    		if(tot>0)
        	{	// squeeze if necessary
        		xscale = Math.min(xscale, (double)w/((tot+0.75)*size));
        	}
  
    	}
       	GC.frameRect(gc,Color.gray,l,at,w,h); 

       	ZertzId moving = movingFromRackIndex;
    	int mo = movingObject;
    	if(mo<0)
    	{
            for(commonPlayer p : players)
            {	if(p!=null)
            	{
            	int mouse = p.mouseObj;
            	if(mouse>=0)
            	{
            		int rack = mouse/100000;
            		if(rack>=0) { moving = ZertzId.find(rack); mo=mouse%10; }
            	}}  
            }
    	}
    	else if((bd.getState()==ZertzState.PUZZLE_STATE) && G.pointInRect(highlight, R))
    		{
    		// allow moving balls in puzzle mode
    		GC.frameRect(gc, Color.red, R);
            highlightRackIndex = ZertzId.find(rackindex);
            highlightBoardCol = (char)0;
            highlightBoardRow = 0;
            highlight.hitCode = ZertzId.find(rackindex);
    		}
        zCell[] ballCells = bd.rack[rackindex];
        int row = 0;
        int total = 0;
        for(int i=0;i<3;i++) { total+=balls[i]-ballCells[i].activeAnimationHeight(); }
        for(int index=2;index>=0;index--)
        {	
        	zCell bCell = ballCells[index];
        	int anim = bCell.activeAnimationHeight();
        	int subt = balls[index]-anim;
        	if(rackindex==RESERVE_INDEX) { total=subt; }

        	if((mo==index) 
        			&& (moving==ZertzId.find(rackindex))) 
        		{ subt--;
        		  total--; 
        		}
        	bCell.rotateCurrentCenter(gc,G.Left(R)+size/2, 2+G.Top(R)+size/2);
        	int empty = (int)(h-subt*size*xscale-size/4)/2;
        	while(subt>0)
        	{
        	total--;
        	subt--;
        	int xoff = size/2+(int)(size*total*xscale);
        	int yoff = 2+size/2+size*row;
        	int xp = l+(horizontal 
        				? xoff
        				: yoff);
        	int yp = t+ (horizontal 
        					? yoff
        					: -(xoff+empty));
        	boolean drawhighlight = G.pointInside(highlight,xp,yp,RINGRADIUS)
        				&& bd.AllowSelectRack(rackindex, index, movingObject);
        				
        	bCell.rotateCurrentCenter(gc,xp,yp);
        	if(gc!=null)
            	{
            	if (drawhighlight)
		            {
		                int bigradius = RINGRADIUS - 1;
		                drawTile(gc, xp, yp, bigradius, null ,
		                    HighlightColor, rackBackGroundColor, BallTextColors[index]);
		            }
            	zChip.getChip(index).drawChip(gc,this,size,xp,yp,null);
             	}
            else if (drawhighlight)
            {
                highlightRackIndex = ZertzId.find(rackindex);
                highlightBoardCol = (char)0;
                highlightBoardRow = 0;
                highlight.hitCode = ZertzId.find(index);
            }
        	}
        	if(isReserve) { row++; }
         }
    }
    
     public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
    	zChip.getChip(obj%10).drawChip(g,this,CELLSIZE,xp-CELLSIZE/4,yp-CELLSIZE/4,null);
    	
    }

    /* draw the board and the balls on it. */
    public void DrawBoardElements(Graphics gc, GameBoard bd, Rectangle brect,  HitPoint highlight)
    {
        int bigradius = RINGRADIUS - 1;
        int fromRow = movingFromBoardRow;
        int fromCol = movingFromBoardCol;
        int moving = movingObject;
        boolean review = reviewMode() && !allowed_to_edit;
        numberMenu.clearSequenceNumbers();
        Color background = review ? reviewModeBackground : boardBackgroundColor;
        if(movingObject < 0)
        {
        for(commonPlayer p : players)
        {	if(p!=null)
        	{
        	int mouse = p.mouseObj;
        	if(mouse>=0)
        	{
        			int rem = mouse%100000;
        			fromRow = (rem/1000);
        			fromCol = (char)((rem%1000)/10);
        			moving = rem%10;
        	}}
        }}
        // draw rings and balls
        if(gc!=null) { bd.DrawGrid(gc, brect, use_grid, Color.black, background, Color.black,Color.black); }
      	Enumeration<zCell>cells = bd.getIterator(Itype.TBLR);
       	while(cells.hasMoreElements())
       	{
       		zCell cel = cells.nextElement();
    	   int xpos = G.Left(brect)+bd.cellToX(cel);
    	   int ypos = G.Bottom(brect)-bd.cellToY(cel);
           boolean hitpoint = G.pointInside(highlight, xpos, ypos,RINGRADIUS);
           char piece = cel.contents;
           int color = zChip.BallColorIndex(piece);
           boolean movable = (color >= 0) && (movingObject < 0) && bd.BallCanMove(cel.col, cel.row);
           boolean canChange = bd.RingCanChange(cel);
           boolean allowSelect = ((color >= 0)
                    ? movable // pointing at a ball
                    : ((movingObject >= 0) // pointing at space
                    		? bd.CaptureOccurs(bd.getCell(movingFromBoardCol,movingFromBoardRow), null,cel, true) //  & dragging a ball
                            : canChange)); // & not dragging a ball
           boolean drawhighlight = allowSelect && hitpoint;

           numberMenu.saveSequenceNumber(cel,xpos,ypos);
           
           if ((piece != NoSpace))
                {
                    if (gc != null)
                    {
                      zChip.getChip(zChip.RING_INDEX).drawChip(gc,this,CELLSIZE,xpos,ypos,null);
                      cel.rotateCurrentCenter(gc,xpos,ypos);
                      if(drawhighlight)
                	  {	
                    	 drawTile(gc, xpos, ypos, bigradius, null , HighlightColor, Color.black, Color.black);
                	  }
                       if (color >= 0) 
                        { // ball resting on a ring
                           // drawTile(gc, xpos, ypos, BALLRADIUS, "",
                           //     BallFillColors[color],
                           //     drawhighlight ? HighlightColor : ringColor,
                           //     BallTextColors[color]);
                          // to fine tune the board rendering.
                          if(!((moving>=0)
                        	  && (movingFromRackIndex==ZertzId.EmptyBoard)
                        	  && (fromCol==cel.col)
                        	  && (fromRow==cel.row)))
                            {
                        	  if(cel.activeAnimationHeight()==0)
                        	  {
                        		  zChip.getChip(color).drawChip(gc,this,CELLSIZE,xpos,ypos,null);
                        	  }
                        	 }
                            if (bd.CapturedColorIndex(piece) >= 0)
                            {
                                StockArt.SmallX.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
                            }
                        }
                       else {

                    	   showCaptured(gc,cel,xpos,ypos);
                    	   
                       }
                    }
                    else if (drawhighlight)
                    { // checking for pointable position
                        highlightRackIndex = ZertzId.EmptyBoard;
                        highlightBoardCol = cel.col;
                        highlightBoardRow = cel.row;
                        highlight.hitCode = color>=0?ZertzId.find(color):ZertzId.EmptyBoard;   
                    }
                }
                else /* empty space */
                {
                    if (gc != null)
                    {
                       drawTile(gc, xpos, ypos, RINGRADIUS-3, "",
                            drawhighlight ? HighlightColor : background,
                            review ? reviewModeBackground : boardBackgroundColor, null);

                       if (canChange)
                        {
                            drawTile(gc, xpos, ypos, RINGRADIUS / 2, "",
                            		background,
                                drawhighlight ? HighlightColor
                                              : RingOutlineColor,
                                RingTextColor);
                        }
                       showCaptured(gc,cel,xpos,ypos);
                    }
                    else if (drawhighlight)
                    {
                        // a ring can be restored
                        highlightBoardCol = cel.col;
                        highlightBoardRow = cel.row;
                        highlight.hitCode = ZertzId.RemovedRing;
                    }
                }
        }
       	numberMenu.drawSequenceNumbers(gc,CELLSIZE,labelFont,labelColor);
    }
    private void showCaptured(Graphics gc,zCell cel,int xpos,int ypos)
    {
    	int nn = numberMenu.getVisibleNumber(cel.lastCaptured);
 	   	if(nn>0)
 	   	{   int cc = zChip.BallColorIndex(cel.lastContents);
 	   	   if(cc>=0)
 	   	   {
 	   	   zChip.getChip(cc).drawChip(gc,this,CELLSIZE,xpos,ypos,null);
 		   StockArt.SmallX.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
 	   	   }
 	   }
    }
    public boolean DrawTileSprite(Graphics gc,HitPoint hp)
    { //draw the ball being dragged
    	int obj = movingObject;
        if ((obj >= 0)&&(hp!=null))
        { // draw a ball in transit
             zChip.getChip(obj).drawChip(gc,this,CELLSIZE,G.Left(hp),G.Top( hp),null);
             return(true);
        }
        else return super.DrawTileSprite(gc,hp);
    }

    private Color tableColor()
    {
        return (reviewMode()&&!allowed_to_edit ? reviewModeBackground : boardBackgroundColor);
    }
    
    PopupManager changeBoard = new PopupManager();
    private void changeBoardMenu(HitPoint hp)
    {
    	changeBoard.newPopupMenu(this, deferredEvents);
    	for(Zvariation v : Zvariation.values())
    	{
    		if(v.boardSetup==v) 
    		{
    			changeBoard.addMenuItem(v.shortName,v);
    		}
    	}
    	changeBoard.show(G.Left(hp),G.Top(hp));
    }
    public boolean handleDeferredEvent(Object target,String command)
    {
    	if(changeBoard.selectMenuTarget(target))
    			{	Zvariation v = (Zvariation)changeBoard.rawValue;
    				PerformAndTransmit("SetBoard "+v.shortName);
     				return(true);
    			}
    	else return(super.handleDeferredEvent(target,command));
    }
    
    private void drawVariation(Graphics gc,HitPoint highlight)
    {	if((b.getState()==ZertzState.PUZZLE_STATE) && G.Height(variationRect)>0)
    	{
    		if(GC.handleSquareButton(gc, variationRect, highlight, s.get(BoardSetup,b.boardSetup.shortName),HighlightColor,rackBackGroundColor))
    		{
    			highlight.hitCode = ZertzId.HitChangeBoard;
    		}
    	}
    }
    //
    // draw the board and balls on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {
    	GC.fillRect(gc, tableColor(),fullRect);
    	
    	GameBoard bd = disB(gc);
    	if(gc!=null)
    	{
    	// note this gets called in the game loop as well as in the display loop
    	// and is pretty expensive, so we shouldn't do it in the mouse-only case
    	bd.SetDisplayRectangle(boardRect);
    	}
        ZertzState vstate = bd.getState();
        boolean moving = (movingObject>=0);
        HitPoint ourTurnSelect = OurMove() ? selectPos : null;
        HitPoint buttonSelect = moving?null:ourTurnSelect;
        HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;
 
        gameLog.redrawGameLog(gc, nonDragSelect, logRect, rackBackGroundColor);
        DrawBoardElements(gc, bd, boardRect, ourTurnSelect);
        //DrawRings(gc, bd, boardRect, ourTurnSelect);
        DrawBalls(gc, bd, reserveRect, RESERVE_INDEX, ourTurnSelect);
        boolean planned = plannedSeating();
        
        for(int player = FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
        {
        	commonPlayer pl = getPlayerOrTemp(player);
        	pl.setRotatedContext(gc,selectPos,false);
            DrawBalls(gc, bd, rackRects[player], player, ourTurnSelect);
            if(planned && bd.whoseTurn==player)
	     	   {
	     		   handleDoneButton(gc,doneRects[player],(bd.DoneState() ? buttonSelect : null), 
	    					HighlightColor, rackBackGroundColor);
	     	   }
	
        	pl.setRotatedContext(gc, selectPos, true);
        }
 
        GC.setFont(gc,standardBoldFont());
        drawPlayerStuff(gc,(vstate==ZertzState.PUZZLE_STATE),nonDragSelect,
 	   			HighlightColor, rackBackGroundColor);
        int whoseTurn = bd.whoseTurn;
        commonPlayer pl = getPlayerOrTemp(whoseTurn);
        double messageRotation = pl.messageRotation();
        // draw the board control buttons 
        if (vstate != ZertzState.PUZZLE_STATE)
        {
			if(!planned)
				{handleDoneButton(gc,messageRotation, doneRect,(bd.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos, HighlightColor, rackBackGroundColor);

			if((vstate==ZertzState.MOVE_OR_SWAP_STATE)||(vstate==ZertzState.SWAP_CONFIRM_STATE))
            { 
				swapButton.show(gc, messageRotation, buttonSelect);
            }
            
        }
        standardGameMessage(gc,messageRotation,
        		vstate==ZertzState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=ZertzState.PUZZLE_STATE,
        				whoseTurn,
        				stateRect);
        goalAndProgressMessage(gc,nonDragSelect,
				s.get(GoalMessage),progressRect, goalRect);

        DrawRepRect(gc,messageRotation,Color.black, bd.Digest(),repRect);	// Not needed for barca
        drawVcrGroup(nonDragSelect, gc);
        drawVariation(gc,ourTurnSelect);
    }


    public boolean Execute(commonMove m,replayMode replay)
    {	//System.out.println("e "+m);
    	if((replay==replayMode.Live) && !OurMove())
    	{ // zertz is unusual because human and robot moves both present
    	  // the same gestures.  This makes the robot's moves generate
    	  // animations anyway.
    	  replay = replayMode.Single; 
    	}
    	

   	handleExecute(b,m,replay);
    // record where the boundaries in move numbers lie
    numberMenu.recordSequenceNumber(b.moveNumber());
    
  	if(m.op==MOVE_SETBOARD) { resetBounds(); }
   	// in capture moves, SequentialFromStart shows the captured ball in its original place
   	// until the jumping ball has landed
   	startBoardAnimations(replay,b.animationStack,CELLSIZE,MovementStyle.SequentialFromStart);
   	
   	if(replay!=replayMode.Replay) { playSounds(m); }
   	return (true);
    }
   void playSounds(commonMove m)
   {
	 switch(m.op)
	  {	case MOVE_BtoB:
	  case MOVE_BtoR:
	  case MOVE_RtoB:
	  case MOVE_RtoR:
   		  playASoundClip(light_drop,100);
   		  break;
	  case MOVE_R_PLUS:
	  case MOVE_R_MINUS:
   		  playASoundClip(scrape,100);
	   	  break;
	  default: break;
	   	  }
    }
    public commonMove ParseNewMove(String st, int player)
    {
        return (new movespec(st, player));
    }

    //
    // prepare to add nmove to the history list, but also edit the history
    // to remove redundant elements, so that indecisiveness by the user doesn't
    // result in a messy replay.
    // This may require that nmove be merged with an existining history move
    // and discarded.  Return null if nothing should be added to the history
    //
    public commonMove EditHistory(commonMove nmove)
    {	movespec newmove = (movespec)nmove;
        int size = History.size() - 1;
        int idx = size;
        boolean first = true;
        if((newmove.op==MOVE_RtoR)
        		&& (newmove.from_rack == newmove.to_rack))
        {	//G.print("Null mov "+newmove);
            return(null);
        }
        if((newmove.op==MOVE_BtoB)
        		&& (newmove.from_col==newmove.to_col)
        		&& (newmove.from_row==newmove.to_row))
        {
        	return(null);
        }
        while (idx >= 0)
        {
            movespec m = (movespec) History.elementAt(idx);
            if(m.nVariations()>1) { idx = -1; }
            else if(first && (m.next!=null)) { idx = -1; }
            else
            {
            first=false;
            switch (m.op)
            {
            case MOVE_START:
            case MOVE_EDIT:
            case MOVE_DONE:
              idx = -1;
                break;

            case MOVE_R_PLUS:
                // adding a ring - look for the same ring being removed
                // r+ uses to_col r- uses from_col
                if (
                		(newmove.op == MOVE_R_MINUS) 
                		&& (newmove.from_row == m.to_row) 
                		&& (newmove.from_col == m.to_col))
                { // if there are variations, back over and start a new one
                  // if there are no variations, remove the r- entirely
                    popHistoryElement(idx);

                    return (null);
                }

                break;

            case MOVE_R_MINUS:

                // removing a ring, look for the same ring being added
                // r+ uses to_col r- uses from_col
                if ( (newmove.op == MOVE_R_PLUS) 
                		&& (newmove.to_col == m.from_col) 
                		&& (newmove.to_row == m.from_row))
                {	
                	popHistoryElement(idx);
                	return(null);
                }

                break;

            case MOVE_RtoR:
                // transfer rack to rack, eliminate the middle man
                if ((newmove.op == MOVE_RtoR) &&
                        (newmove.color == m.color) &&
                        (m.to_rack == newmove.from_rack))
                {
                    if (m.from_rack == newmove.to_rack)
                    {
                    	popHistoryElement(idx);

                        return (null);
                    }

                    m.to_rack = newmove.to_rack;

                    return (null);
                }

                break;

            case MOVE_BtoB:
            // moving board to board, look for the matching
             {
                if ((newmove.op == MOVE_BtoB) &&
                        (m.from_col == newmove.to_col) &&
                        (m.from_row == newmove.to_row) &&
                        (m.to_row == newmove.from_row) &&
                        (m.to_col == newmove.from_col) &&
                        (m.color == newmove.color))
                {	// straight over and back
                    popHistoryElement();
                    return (null);
                }
                idx = -1;	// only in the immediately reverse direction
            }

            break;

            case MOVE_BtoR:
            {
                // moving board to rack, look for the corresponding
                // rack to board transfer
                if ((newmove.op == MOVE_RtoB)
                     && (newmove.to_col == m.from_col)
                     && (newmove.to_row == m.from_row)
                     && (newmove.color == m.to_rack)
                     && (newmove.movedAndCaptured == m.movedAndCaptured)	// can be different if was captured ball
                     )
                {
                    if(idx==size) { popHistoryElement(); return(null); }
                    else if(idx+1==size) { commonMove rem = popHistoryElement(); popHistoryElement(); return(rem); }
                    idx = -1;
                }
            }

            break;

            case MOVE_RtoB:
            // moving rack to to board, if there's a board to board,
            // we're just repositioning a ball.  If there is a 
            // previous board to board, we're adjusting a capture
            {
                if ((newmove.op == MOVE_BtoB) &&
                        (m.to_col == newmove.from_col) &&
                        (m.to_row == newmove.from_row))
                { //if there are variations, back up and place a new ball
                  //if there are no variations, just set a new destination
                	popHistoryElement(idx);
                    newmove.op = MOVE_RtoB;
                    newmove.from_col = m.from_col;
                    newmove.from_row = m.from_row;
                    newmove.from_rack = m.from_rack;
                    newmove.color = m.color;
                    idx = -1; //break out

                    //m.to_col=newmove.to_col;
                    //m.to_row=newmove.to_row;
                    //return(null);
                }

                if ((newmove.op == MOVE_BtoR) &&
                        (newmove.to_rack == m.from_rack) &&
                        (newmove.to_row == m.from_row) &&
                        (newmove.from_col == m.to_col) &&
                        (newmove.from_row == m.to_row))
                { //undoing a capture step.  
                	popHistoryElement(idx);
                	return (null);
                }
            }

            break;
			default:
				break;
            }

            idx--; //keep rolling back
        }}
        if(newmove!=null)
        {
        	newmove = (movespec)super.EditHistory(newmove);
        }
        return (newmove);
    }

    public void StartDragging(HitPoint hp)
    {	
        CellId id = hp.hitCode;
        if((movingObject<0) && (id instanceof ZertzId))
        {
        	ZertzId hitObject = (ZertzId)id;
        	if(hitObject.isBall)
        	{
        	movingFromRackIndex = highlightRackIndex;
            movingFromBoardRow = highlightBoardRow;
            movingFromBoardCol = highlightBoardCol;
            movingObject = hitObject.ordinal();            
            hp.dragging = true;
        	}
        	else if(hitObject==ZertzId.HitChangeBoard)
        	{
        		changeBoardMenu(hp);
        	}
    	}
    }

    public void StopDragging(HitPoint hp)
    {
        //System.out.println("Stop "+hp+" m "+movingObject);

        CellId id = hp.hitCode;
        if(!(id instanceof ZertzId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}	
    	else {
    	missedOneClick = false;
        ZertzId hitObject = (ZertzId)hp.hitCode;


        if (movingObject >= 0)
        { //moving a ball
        	boolean dropped = false;
            if (movingFromRackIndex==ZertzId.EmptyBoard)
            { //from board

                if (highlightRackIndex == ZertzId.EmptyBoard)
                {	dropped = true;
                	if((movingFromBoardCol!=highlightBoardCol)
                    		||(movingFromBoardRow!=highlightBoardRow))
                    {PerformAndTransmit("BtoB " + movingFromBoardCol + " " +
                        movingFromBoardRow + " " + highlightBoardCol + " " +
                        highlightBoardRow);
                    }
                }
                else 
                {	dropped = true;
                    PerformAndTransmit("BtoR " + movingFromBoardCol + " " +
                        movingFromBoardRow + " " + highlightRackIndex.shortName);
                }
            }
            else
            { //from rack

                if (highlightRackIndex == ZertzId.EmptyBoard)
                {	dropped = true;
                    PerformAndTransmit("RtoB " + movingFromRackIndex.shortName + " " +
                        movingObject + " " + highlightBoardCol + " " +
                        highlightBoardRow);
                }
                else 
                {	dropped = true;
                    PerformAndTransmit("RtoR " + movingFromRackIndex.shortName + " " +
                        movingObject + " " + highlightRackIndex.shortName);
                }
            }
            if(dropped) { hp.hitCode = DefaultId.HitNoWhere; movingObject = HitNoWhere; }
            else 
            { //System.out.println("not dropped");
            }

        }
        else if (hitObject == ZertzId.EmptyBoard)
        {	
            PerformAndTransmit("R- " + highlightBoardCol + " " +
                highlightBoardRow);
        }
        else if (hitObject == ZertzId.RemovedRing)
        {	
            PerformAndTransmit("R+ " + highlightBoardCol + " " +
                highlightBoardRow);
        }
        else if(hitObject instanceof ZertzId) {}
        else {	
        	throw G.Error("Hit Unknown: %s", hitObject);
        }}
       movingObject=HitNoWhere;
       generalRefresh();
    }


    public void drawBlankTile(Graphics g, int X, int Y, int radius,
        Color color, Color bgcolor)
    { 
    	GC.cacheAACircle(g,X,Y,radius,color,bgcolor,true);
    }


    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Zertz_SGF); }


    public void performHistoryInitialization(StringTokenizer his)
    {  
        //the initialization sequence
    	String token = his.nextToken();
 
        b.doInit(token);
     }
    

    public String gameProgressString()
    {
        return ((allowed_to_edit ? Reviewing : ("" + History.viewMove)) + " " +
        b.ballSummary(FIRST_PLAYER_INDEX) + " " +
        b.ballSummary(SECOND_PLAYER_INDEX));
    }

    public SimpleRobotProtocol newRobotPlayer() { return(new autoPlay2()); }

    public String sgfGameVersion() { return(SGF_GAME_VERSION); }
    
    public boolean parsePlayerInfo(commonPlayer p,String first,StringTokenizer tokens)
    {
    	if(exHashtable.TIME.equals(first) && b.DoneState())
    	{
    		PerformAndTransmit("Done",false,replayMode.Replay);
    	}
    	return(super.parsePlayerInfo(p, first, tokens));
    }

    private int ppn = -1;
    /* 5/23/2023 
    summary:
    	3975: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\zertz\games\games\archive-2004\games-Oct-31-2004.zip Z-ddyer-Qarl-2004-10-29-2141.sgf lib.ErrorX: Destination cell not empty!
    	4051: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\zertz\games\games\archive-2004\games-Oct-31-2004.zip Z-Qarl-ddyer-2004-10-29-2155.sgf lib.ErrorX: Destination is not empty

    	74252 files visited 2 problems
    	*/
    public void ReplayMove(sgf_node no)
    {
        String comments = "";
        sgf_property prop = no.properties;
        while (prop != null)
        {
            String name = prop.getName();
            String value = (String) prop.getValue();
            int pn=-1;
            //System.out.println("prop " + name + " " + value);
           if (setup_property.equals(name))
            {
                b.doInit(value);
                b.setWhoseTurn(0);
            }

            if (name.equals(comment_property))
            {
                comments += value;
            }
            // note, don't replace these with the standardized versions
            else if (name.equals(version_property))
            {	
            	int val = G.IntToken(value);
            	switch(val)
            	{
            	default: throw G.Error("Game version %s not handled",value);
            	case 2: 
					break;
				case 1: break;
            	}
            }
            else if (((pn = commonMove.playerNumberToken(name))>=0)
            		&& (pn<players.length))
            {
                commonPlayer p = players[pn];

                if (p == null)
                {
                    p = players[pn] = new commonPlayer(pn);
                }

                StringTokenizer tokens = new StringTokenizer(value);
                String num = "";
                String first = tokens.nextToken();
                if(Character.isDigit(first.charAt(0))) 
                { num=first+ " "; first=tokens.nextToken(); }
                   	if(parsePlayerInfo(p,first,tokens)) {}
                    else
                    {
                        String msg = first + " " + G.restof(tokens);
                        boolean isDone = msg.equals("Done ") ;
                        if(isDone) 
                        	{ hasDones=true; }
                        if(!hasDones && !isDone && b.DoneState() && (pn!=ppn))
                        {
                        	//perform the done "just in time" so if there are
                            //editing/undo commands in the stream, they work ok
                        	// originally the dones were edited out.  Later, it became
                        	// apparent that it was better to include them.  Even later
                        	// the definite "has dones" is indicated by version 2
                        	//
                        	// the current logic is a fixup to try to make all the old game records work.
                            PerformAndTransmit("Done", false,replayMode.Replay);
                        }
                        ppn = pn;

                        //b.setWhoseTurn(player);
                        if(b.needStart)
                        {	// repair for some old records that don't have a "start" or "edit"
                        	PerformAndTransmit("Start "+pn,false,replayMode.Replay);
                        }
                        if(b.GameOver()&& msg.toLowerCase().startsWith("edit")) {}
                        else {
                        	if("reset".equalsIgnoreCase(msg.trim()))
                        	{	// some damaged games have "reset" embedded in them
                        	}
                        	else
                        	{
                        	PerformAndTransmit(num+msg, false,replayMode.Replay);
                        	}
                        }
                    }
            }
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


	public int getLastPlacement(boolean empty) {
			return b.placementIndex;
	}



}
