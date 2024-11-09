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
package cookie;

import java.awt.*;
import java.awt.Rectangle;
import online.common.*;

import java.util.*;

import common.GameInfo;
import lib.Graphics;
import lib.Image;
import lib.*;
import lib.Random;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

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
 * Change History
 */
public class CookieViewer extends CCanvas<CookieCell,CookieBoard> implements CookieConstants
{	
	static final String ImageDir = "/cookie/images/";
    static final String Cookie_SGF = "Cookie-Disco"; // sgf game number allocated for Cookie Disco
	   
        
        static final int BACKGROUND_TILE_INDEX = 0;
        static final int BACKGROUND_REVIEW_INDEX = 1;
        static final int BACKGROUND_TABLE_INDEX = 2;
        static final String TextureNames[] = { 
        			"background-tile" ,
        			"background-review-tile",
        			"Marble-tile" /* "green-felt-tile" */
        		};
     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(176,185,225);
    private Color rackActiveColor = new Color(196,205,245);
    

   
    // private state
    private CookieBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private final double INITIAL_TILE_SCALE = 3.0;

    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle sampleRects[] = addRect("logo",2);
    
	private TextButton swapButton = addButton(SWAP,GameId.HitSwapButton,SwapDescription,
			HighlightColor, rackBackGroundColor);
	private Rectangle chipRect = addRect("firstPlayerChipRect");
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);

	private static Image textures[] = null;

    public synchronized void preloadImages()
    {	
    	if(textures==null)
    	{	CookieChip.preloadImages(loader,ImageDir);
            textures =loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = CookieChip.getChip(0).image;
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

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),CookieId.ZoomSlider);
        zoomRect.min=1.0;
        zoomRect.max=5.0;
        zoomRect.value=INITIAL_TILE_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = HighlightColor;   
        labelColor = Color.red;
        labelFont = largeBoldFont();
        
        bb = new CookieBoard(info.getString(GameInfo.GAMETYPE, "cookie-disco"),
        		getStartingColorMap());
        useDirectDrawing(true);
        Random r = new Random(randomKey);
        int pat = Random.nextInt(r,startPatterns.length);
        doInit(false,randomKey,pat);
    }
    public void doInit(boolean preserve)
    {	doInit(preserve,bb.randomKey,bb.startingPattern);
    }
    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history,long rand,int pat)
    {	
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        
        bb.doInit(bb.gametype,rand,pat);						// initialize the board
      
   	 	if(!preserve_history) 
   	 		{ zoomRect.setValue(INITIAL_TILE_SCALE);
   	 		  board_center_x = board_center_y = 0.0; 
   	 		  startFirstPlayer();
   	 		}
 
     }

	public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit) 
	{	commonPlayer pl = getPlayerOrTemp(player);
		int u2 = unit/2;
		Rectangle box = pl.createRectangularPictureGroup(x+4*unit,y,2*unit/3);
		Rectangle sampleRect = sampleRects[player];
		Rectangle doneRect = doneRects[player];
		G.SetRect(sampleRect,x+u2,y, unit+u2,unit+u2);
		G.SetRect(doneRect, x, y+unit+3*unit/4, unit*4, plannedSeating()?unit*2:0);
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
    	int minLogW = fh*20;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int CELLSIZE=(int)(fh*2.5);
        int C2 = CELLSIZE/2;
        int margin = fh/2;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	// margins for allocated boxes
    			0.6,	// 60% of space allocated to the board
    			1.0,	// 1.0:1 aspect ratio for the board
    			fh*3,	// maximum cell size
    			0.5		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	int chipW = CELLSIZE*3;
       	int chipH = CELLSIZE*12;
       	layout.placeRectangle(chipRect, chipW, chipH,chipW,chipH,
       			chipH,chipW, chipH,chipW,BoxAlignment.Center,true);
   	
    	layout.placeDoneEdit(CELLSIZE*3, CELLSIZE*3, doneRect, editRect);
    	layout.placeRectangle(swapButton, CELLSIZE*3,CELLSIZE+C2,CELLSIZE*3,CELLSIZE+C2,BoxAlignment.Center,true);
      	
       	
  	
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

      	placeStateRow(boardX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,noChatRect);

    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	SetupVcrRects(boardX+C2/2,boardBottom-C2/2-minLogW/2,minLogW,minLogW/2);
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow(boardX, boardBottom,boardW,stateH,goalRect);       
      	G.placeRight(goalRect,zoomRect,zoomW);
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
 
    }

    private void DrawLogo(Graphics gc,HitPoint highlight,Rectangle r,int player)
    {	
    	CookieCell c = bb.playerChip[player];
    	int width = G.Width(r);
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	if(c.drawStack(gc,bb.LegalToHitChips(c)?highlight:null,
    			cx,cy,
    			this,0,(int)(G.Width(r)*0.8),1.0,""))
    	{
            if(highlight.hitObject!=null)
            {	
            	highlight.awidth =width/3;
            	highlight.spriteColor = Color.red;
            }
    	}
    }

	// draw a box of spare chips. It's purely for visual effect.
    private void DrawChipPool(Graphics gc, Rectangle r, HitPoint highlight,CookieBoard gb)
    {	
    	int w = G.Width(r);
    	int h = G.Height(r);
    	boolean tall = w<h;
        int ystep = tall ? h/3 : 0;
        int xstep = tall ? 0 : w/3;
        int sz = tall ? w : h;
        for(int i=0,y=G.Top(r)+ (tall ? ystep/2 : h/2 ),
        			x=G.Left(r)+(tall ? w/2 : xstep/2 );
        	i<CookieChip.nChips;
        	i++,y+=ystep,x+=xstep)
        {	CookieCell c = gb.chipPool[i];
        	boolean canhit = gb.LegalToHitChips(c);
        	CookieChip chip = c.topChip();
        	labelColor = Color.white;
        	labelFont = G.getFont(largeBoldFont(),sz/3);
        	if(c.drawStack(gc,canhit?highlight:null,
        			x,y,
        			this,0,tall?(int)(w*0.9):h,1.0,""+chip.value))
        	{
        	highlight.arrow = (gb.pickedObject==null)?StockArt.UpArrow:StockArt.DownArrow;
        	highlight.awidth = sz/3;
        	highlight.spriteColor = Color.red;
         	}
        	HitPoint.setHelpText(highlight,sz,x,y,s.get(chip.description)); 
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
   		int cellS = inboard? bb.cellSize() :
   			Math.min(G.Height(chipRect), G.Width(chipRect)) ;
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
   		CookieChip chip = CookieChip.getChip(obj%100);
   		int bottom = G.Bottom(boardRect);
   		int left = G.Left(boardRect);
     	if(obj>100)
     		{ CookieChip second = CookieChip.getChip(obj/100);
     		  int yp1 = bottom-yp;
     		  CookieCell close = bb.closestCell(xp,yp1);
     		  if(close!=null && (close.topChip()!=null))
     		  {	CookieCell src = bb.getSource();
     		  	if(src!=null)
     		  	{	
     		  		int xp2 = left+bb.cellToX(src);
     		  		int yp2 = bottom-bb.cellToY(src);
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
     GC.setColor(gc,review ? reviewModeBackground : rackBackGroundColor);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
     textures[review ? BACKGROUND_REVIEW_INDEX:BACKGROUND_TABLE_INDEX].tileImage(gc,
          		boardRect); 
      GC.frameRect(gc,Color.black,boardRect); 
        
    
    }
    public void ShowDirections(Graphics gc,CookieBoard gb,int xpos,int ypos,CookieCell c)
    {
    	if(gc!=null)
    	{
    		int xp0 = gb.cellToX(c.col,c.row);
    		int yp0 = gb.cellToY(c.col,c.row);
		   	for(int i =0;i<CookieBoard.CELL_FULL_TURN();i++)
		   	{
		   		CookieCell nc = c.exitTo(i);
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

    private void drawBoardElements(Graphics gc, CookieBoard gb, Rectangle tbRect,
    		HitPoint ourTurnSelect,HitPoint anySelect)
    {	
       Rectangle oldClip = GC.combinedClip(gc,tbRect);
       int cellSize = gb.cellSize();
   	   boolean someHit = draggingBoard();
    	//
        // now draw the contents of the board and anything it is pointing at
        //
       double scale = G.getDisplayScale();
       Hashtable<CookieCell,CookieMovespec> dests = gb.movingObjectDests();
       CookieCell sourceCell = gb.getSource();
       CookieCell destCell = gb.getDest();
       int left = G.Left(tbRect);
       int top = G.Bottom(tbRect);
       for(Enumeration<CookieCell> cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements();)
        { //where we draw the grid
           	CookieCell ccell = cells.nextElement();
           	int xpos = left + gb.cellToX(ccell);
           	int ypos = top - gb.cellToY(ccell);
                                  
           	boolean isADest = dests.get(ccell)!=null;
           	boolean isASource = (ccell==sourceCell)||(ccell==destCell);
           	boolean canHitThis = !someHit && gb.LegalToHitBoard(ccell);
 
           	if(ccell.drawStack(gc,canHitThis?ourTurnSelect:null,
            			xpos,ypos,
            			this,0,cellSize,1.0,use_grid?(""+ccell.col+ccell.row):null ))
                {	 //if(gc!=null) { gc.drawOval(xpos-cellSize/2,ypos-cellSize/2,cellSize,cellSize); }
                   	ourTurnSelect.hitCode = CookieId.BoardLocation;
                    ourTurnSelect.awidth = cellSize/2;
                    ourTurnSelect.spriteColor = Color.red;
                    someHit = true;
                }
                
                if(gb.cherryCell==null) {}
                else if(gb.crawlOption)
            	{
                boolean cherryDrawn = false;
            	if((ccell==gb.crawlCell) 
            			&& !gb.activeCrawlAnimations()
            			&& (gb.getSource()!=gb.crawlCell) 
            			&& (gb.pickedObject!=CookieChip.Crawl))
            	{	
            		if((ccell!=gb.cherryCell) && (ccell.topChip()==gb.cherryChip)) // single cherry on the locked color
            		{	// if the cookie under the crawl cookie is locked, draw the cherry under
                	CookieChip.Cherry.drawChip(gc,this,cellSize/2,xpos,ypos,null);
                	cherryDrawn = true;
                	}
            		CookieChip.Crawl.drawChip(gc,this,cellSize,xpos,ypos,null);
            	}
            	if((ccell==gb.cherryCell) && (ccell.activeAnimationHeight()==0))
            		{	// double cherry on the chip moved
            		CookieChip.Cherry.drawChip(gc,this,cellSize/2,xpos-CELLSIZE/2,ypos-CELLSIZE/2,null); 
      			  	CookieChip.Cherry.drawChip(gc,this,cellSize/2,xpos+CELLSIZE/2,ypos+CELLSIZE/4,null); 
            		}
            		else if(!cherryDrawn && (ccell.topChip()==gb.cherryChip) && (ccell.activeAnimationHeight()==0))
            		{
            			CookieChip.Cherry.drawChip(gc,this,cellSize/2,xpos,ypos,null);
            		}
            	}
                else if((ccell==gb.cherryCell) && (ccell.activeAnimationHeight()==0)) // single cherry on the locked color
            	{
            	CookieChip.Cherry.drawChip(gc,this,cellSize/2,xpos,ypos,null);
            	}
                
                if (gc != null)
                {
                //G.DrawAACircle(gc,xpos,ypos,1,tiled?Color.green:Color.blue,Color.yellow,true);
                //if(cell.topChip()!=null) { ShowDirections(gc,gb,xpos,ypos,cell); }
                if(isASource)
                	{GC.cacheAACircle(gc,xpos,ypos,(int)(2*scale),Color.green,Color.yellow,true);
                	} else
                if(isADest)
                	{GC.cacheAACircle(gc,xpos,ypos,(int)(3*scale),Color.red,Color.yellow,true);
                	}
                //if(ccell.topChip()==null) { G.Text(gc,true,xpos-CELLSIZE*2,ypos-CELLSIZE*2,CELLSIZE*4,CELLSIZE*4,Color.white,null,""+ccell.col+ccell.row); }
  
                }

        }
        if(gb.crawlOption && (gb.crawlCell==null))
    	{	// draw the crawl cookie in the corner of the board if not elsewhere
        	int xp = G.Right(tbRect)-cellSize;
        	int yp = G.Top(tbRect)+cellSize;
        	CookieChip.Crawl.drawChip(gc,this,cellSize,xp,yp,null);
    	}
        
        doBoardDrag(tbRect,anySelect,cellSize,CookieId.InvisibleDragBoard); 

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
    {  CookieBoard gb = disB(gc);
	if(gc!=null)
	{
	// note this gets called in the game loop as well as in the display loop
	// and is pretty expensive, so we shouldn't do it in the mouse-only case

    	gb.SetDisplayParameters(zoomRect.value,1.0,board_center_x,board_center_y,30.0); // shrink a little and rotate 30 degrees
    	gb.SetDisplayRectangle(boardRect);
	}
       CookieState state = gb.getState();
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
       
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, rackBackGroundColor);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,nonDragSelect);
       DrawChipPool(gc, chipRect, ourTurnSelect,gb);
       boolean ds = gb.DoneState();
       boolean planned = plannedSeating();
       for(int i=0;i<2;i++)
       {
    	   commonPlayer pl = getPlayerOrTemp(i);
    	   pl.setRotatedContext(gc, selectPos,false);
    	   DrawLogo(gc,ourTurnSelect,sampleRects[i],i);
    	   if(planned && (bb.whoseTurn==i))
           { 
             handleDoneButton(gc,doneRects[i],(ds ? buttonSelect : null), 
           		HighlightColor, rackBackGroundColor);
           }
    	   pl.setRotatedContext(gc, selectPos, true);
       }
       zoomRect.draw(gc,nonDragSelect);
       
       GC.setFont(gc,standardBoldFont());
     
       drawPlayerStuff(gc,(state==CookieState.PUZZLE_STATE),nonDragSelect,
	   			HighlightColor, rackBackGroundColor);

       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
   	   double messageRotation = pl.messageRotation();
   		if (state != CookieState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned) { handleDoneButton(gc,doneRect,(ds ? buttonSelect : null), 
					HighlightColor, ds ? rackActiveColor : rackBackGroundColor);}
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos,HighlightColor, rackBackGroundColor);
			boolean conf = (state==CookieState.CONFIRM_SWAP_STATE);
            if(conf ||(state==CookieState.PLACE_OR_SWAP_STATE))
            {	swapButton.highlightWhenIsOn = true;
            	swapButton.setIsOn(conf);
            	swapButton.show(gc, messageRotation, buttonSelect);
            }
          }


           standardGameMessage(gc,messageRotation,
            		state==CookieState.GAMEOVER_STATE?gameOverMessage(gb):s.get(state.getDescription()),
            				state!=CookieState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
            gb.playerChip[gb.whoseTurn].topChip().drawChip(gc,this,iconRect,null);
            if(!DrawRepRect(gc,messageRotation,Color.black,gb.Digest(),goalRect))
            {
            goalAndProgressMessage(gc,nonDragSelect,s.get(GoalMessage),progressRect, goalRect);
            }
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
        
        startBoardAnimations(replay);
      
		lastDropped = bb.lastDropped;	// this is for the image adjustment logic
		if(replay.animate) { playSounds(mm); }
       return (true);
    }
     
    void startBoardAnimations(replayMode replay)
     {	CellStack animationStack = bb.animationStack;
     	try {
        if(replay.animate)
     	{	
     		int lim = animationStack.size();
      		if(lim>0)
     		{
    		CookieCell target = animationStack.top();
       		boolean movingCrawlCookie = (bb.crawlCell==target);
     		CookieChip chip = target.topChip();
     		if(chip!=null)
     		{
     		double speed = masterAnimationSpeed*1.0/lim;
     		double start = 0.0;
     		for(int i = 0; i<lim; i+=2)
     		{
     		CookieCell src = animationStack.elementAt(i);
     		CookieCell dest = animationStack.elementAt(i+1);
     		if(movingCrawlCookie) 
     			{
     			// start the crawl animation first, which will be displayed last.
     			startAnimation(src,dest,CookieChip.Crawl,bb.cellSize(),start,speed);
     			}
     		startAnimation(src,dest,chip,bb.cellSize(),start,speed);
     		start += speed;
     		}}}}}
        finally {
   		 animationStack.clear();
        }
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
        return (new CookieMovespec(st, player));
    }
 

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof CookieId) // not dragging anything yet, so maybe start
        {

        CookieId hitObject =  (CookieId)hp.hitCode;
        CookieCell c = hitCell(hp);
 	    switch(hitObject)
	    {
	    default: break;
	    
        case InvisibleDragBoard:
			break;
		case ChipPool:
	    	if(c!=null)
	    		{ CookieChip top = c.topChip();
	    		  if(top!=null) { PerformAndTransmit("Pick "+CookieMovespec.D.findUnique(CHIP_OFFSET+top.index)); }
	    		}
	    	break;
	    case BoardLocation:
	    	if((c!=null) && c.topChip()!=null)
	    	{
	    		if((c==bb.crawlCell) && !bb.hasLegalMoves(c,bb.whoseTurn,false))
	    		{	// can only move the crawl cookie
	    			PerformAndTransmit("Pick Crawl");
	    		}
	    		else
	    		{
	    		PerformAndTransmit("Pickb "+c.col+" "+c.row);
	    		}
    		}
	    	break;
        }
         }
    }
	private void doDropChip(char col,int row,CookieChip ch)
	{	
		PerformAndTransmit("dropb "+col+" "+row+" "+ch.index);
	}
	private void doDropChip(char col,int row)
	{
		CookieChip mo = bb.pickedObject;
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
        if(!(id instanceof CookieId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	CookieId hitCode = (CookieId)hp.hitCode;
        CookieCell hitObject = hitCell(hp);
        CookieState state = bb.getState();
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
			default: throw G.Error("Not expecting drop on board in state %s",state);
			
			case PLACE_COOKIE_STATE:
			case PLACE_OR_SWAP_STATE:
					if(bb.pickedObject==null)
					{	CookieCell from = bb.playerChip[bb.whoseTurn];
						CookieChip top = from.topChip();
						PerformAndTransmit("add "+top.chipName()+" "+hitObject.col+" "+hitObject.row);
						return;
					}
				//$FALL-THROUGH$
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
            	PerformAndTransmit("Drop "+CookieMovespec.D.findUnique(hitObject.row));
			}
           break;
 
         }
    	}
    }

    // return what will be the init type for the game
    public String gameType() { return(bb.gametype+" "+bb.randomKey+" "+bb.startingPattern); }	// this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Cookie_SGF); }	// this is the official SGF number assigned to the game

    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int rand = G.IntToken(his);
    	int pat = G.IntToken(his);
        bb.doInit(token,rand,pat);
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
    	return(new CookiePlay());
     }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/24/2023
     * 	1995 files visited 0 problems
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
            	int rv = G.IntToken(tok);
            	int pat = G.IntToken(tok);
                bb.doInit(init,rv,pat);
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
