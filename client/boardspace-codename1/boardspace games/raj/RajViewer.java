package raj;
/**
 * The unusual feature of Raj is simultaneous play, and no "done" moves.
 */
import com.codename1.ui.Font;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import bridge.Color;

import bridge.Utf8OutputStream;
import bridge.Utf8Printer;

import online.common.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;


import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.DefaultId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Text;
import lib.TextChunk;

import online.game.*;
import online.game.BaseBoard.BoardState;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;
import raj.RajBoard.PlayerBoard;
import rpc.RpcService;
import vnc.VNCService;

/**
 * Raj is the first Boardspace game which has truly simultaneous play.  A number of
 * adaptions to the overall kit were required, and the internal structure of the board
 * and communications is significantly different from standard.
 * 
 * In "SIMULTANEOUS_PLAY" mode, there are no "Done" buttons.  All the players are free to
 * play their card, and retract that play, until all player have played at the same time.
 * In this phase, the board state is either "play" or "wait"
 * When the nominal "current player" notices that all are ready, he enters "commit" phase
 * and commits his move, which bumps the current player to the next.  This guarantees that
 * he stays committed permanently.   The next player will usually see all players reads
 * and also commit, but if he or one of the remaining players has retracted a move he simply
 * waits for the "all ready".
 * 
 *  
*/

public class RajViewer extends CCanvas<RajCell,RajBoard> implements RajConstants, GameLayoutClient
{	static final long serialVersionUID = 1000;

	//if true, run the UI with simultaneous plays. Otherwise, run the moves serially
	//like most of the other games.  Changing to false is intended for debugging only.
	static boolean SIMULTANEOUS_PLAY = true;

     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackgroundColor = new Color(248,252,238);
    private Color rackBackGroundColor = new Color(178,212,168);
    private Color boardBackgroundColor = new Color(178,212,168);
    
    Font cardDeckFont = G.getFont("Dialog", G.Style.Bold, 25);
    // images, shared among all instances of the class so loaded only once
    private static Image[] textures = null;// background textures
    
    // private state
    private RajBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //
    private Rectangle prizeRect = addZoneRect(".prizeRect");
    private Rectangle playerCardRects[] = addRect("card",6);
    
    public Color rajMouseColors[] = 
    {	RajColor.Red.color,
    	RajColor.Green.color,
    	RajColor.Blue.color,
    	RajColor.Brown.color,
    	RajColor.Purple.color
    };
    public Color rajDotColors[] = 
    {	Color.white,Color.white,Color.white,Color.white,Color.black
    };
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	RajChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	if (textures == null)
    	{ 	// note that for this to work correctly, the images and masks must be the same size.  
        	// Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    		
    		// images and textures are static variables, so they're shared by
    		// the entire class and only get loaded once.  Special synchronization
    		// tricks are used to make sure this is true.
    		
    	  // load the tiles used to construct the board as stock art

          textures = loader.load_images(ImageDir,TextureNames);

    	}
    	gameIcon = RajChip.getPrize(10).image;
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	//
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default

        MouseColors = rajMouseColors;
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        int np = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME));
         
        bb = new RajBoard(info.getString(OnlineConstants.GAMETYPE, Raj_INIT),randomKey,np,getStartingColorMap());
        useDirectDrawing(true); // not tested yet
        doInit(false);

    }
    public void doUndo()
    {	super.doUndo();
    	doScrollTo(BACKWARD_PLAYER);
    	truncateHistory();
    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        bb.doInit();						// initialize the board
        bb.setMyIndex(getActivePlayer().boardIndex,SIMULTANEOUS_PLAY);
        if(!preserve_history)
    		{ startFirstPlayer();
    		}
        initialized = true;
    }
    boolean initialized = false;

	public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit)
	    {
		commonPlayer pl = getPlayerOrTemp(player);
		Rectangle cards = playerCardRects[player];
		Rectangle box = pl.createSquarePictureGroup(x,y,unit);
		G.SetRect(cards, G.Right(box), G.Top(box)+unit*1, unit*7,unit*8);
		G.union(box, cards);
		pl.displayRotation = rotation;
		return box;
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
        int nrows = 15;  
        int ncols = 24;
        layout.strictBoardsize = false;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.2 ,	// 50% of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*2,	// maximum cell size
    			0.25		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);      
    	layout.placeRectangle(editRect,buttonW,buttonW/2,buttonW*2,buttonW,BoxAlignment.Bottom,true);
    	Rectangle main = layout.getMainRectangle();
    	int boardX = G.Left(main);
    	int boardY = G.Top(main);
    	int boardW = G.Width(main);
    	int boardH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)boardW/ncols,(double)boardH/nrows);
    	CELLSIZE = (int)cs;
        int C2 = CELLSIZE/2;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        int stateH = fh*3;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,noChatRect);

    	G.SetRect(boardRect,boardX,boardY+stateH,boardW,boardH-stateH);
		// a pool of chips for the first player at the top
        G.SetRect(prizeRect, (G.centerX(boardRect)-5*C2),
        		G.Top(boardRect)-C2,
        		5*CELLSIZE,4*CELLSIZE);
        
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);

             			}
 
    //
    // draw the numbered side of a card.
    //
    private void drawCardFace(Graphics gc,Rectangle r,RajChip ch)
    {	int xp = G.centerX(r);
		int yp = G.centerY(r);
		int hscale = G.Width(r);
		ch.drawChipFront(gc,this,hscale,xp,yp);
    }
    private boolean drawStack(Graphics gc,RajCell c,HitPoint hp,int w,int x,int y,double xstep,double ystep,boolean showFront)
    {	RajCell proxy = c;
    	if(showFront)
    	{
    		proxy = new RajCell(c.rackLocation());
    		for(int lim=c.height(),i=0; i<lim;i++)
    		{
    			RajChip ch = c.chipAtIndex(i);
    			proxy.addChip(ch.getCardFront());
    		}
    	}
    	boolean val = proxy.drawStack(gc, this, hp, w, x, y,0,xstep,ystep, null);
        if(val && c!=proxy)
        {
        	hp.hitObject = c;
        	if(remoteViewer>=0)
        	{
        		hp.spriteColor = Color.red;
        		hp.spriteRect = new Rectangle(
        				(int)(x+xstep*hp.hit_index*w-w/4),
        				(int)(y-hp.hit_index*ystep*w-w/2),w*2/3,w);
        	}
        }
        return(val);
    }
    public Text censoredMoveText(commonMove sp,int index)
     {
    	commonMove last = History.top();
    	String mv = last.getSliderNumString();
    	String spnum = sp.getSliderNumString();
    	boolean censor = mv.equals(spnum) && (sp.op!=MOVE_AWARD);
    	Rajmovespec spc = (Rajmovespec)sp;
    	return(TextChunk.create(spc.shortMoveString(censor)));
     }

    private int movingObjectIndex()
    { 	return(bb.movingObjectIndex()); 
    }
    private void drawPlayerBoard(Graphics gc,int pl,HitPoint highlight,RajBoard gb,HitPoint anyHit)
    {	RajBoard.PlayerBoard pb = gb.getPlayerBoard(pl);
    	if(pb!=null)
    	{
    	Rectangle r = playerCardRects[pl];
    	int height = G.Height(r);
    	int width = G.Width(r)*7/10;
    	int left = G.Left(r);
    	int top = G.Top(r);
    	int right = left+width;
    	int unit = width/4;
    	Rectangle playerPrizeRect = new Rectangle(right-width/2,top+unit/3,width/2,height/3);
    	int mid = top+height/2;
    	int step = (-width/5);
    	int cx = (left-step);
    	RajCell played = pb.playedCards;
    	RajCell unplayed = pb.cards;
        drawPrizePool(gc, playerPrizeRect, anyHit,gb,pb.wonPrizes,true,unit);
        boolean canHitPlayedCards = gb.LegalToHitCards(played);    
        //StockArt.SmallO.drawChip(gc,this,CELLSIZE*2,r.x,mid-step,null);
    	if(played.drawHStack(gc,this,
    				canHitPlayedCards?highlight:null,unit*2,
    				left+(unit*3),mid-step,0,0.07,null))
    	{	highlight.setHelpText(s.get("Used Cards"));
    		highlight.hit_index = -1;
    		highlight.arrow = (movingObjectIndex()<0)?StockArt.UpArrow:StockArt.DownArrow;
    		highlight.awidth = unit;
   		
    	}

     	{	
     		int cy = mid+unit+unit/3;
     		int hscale = unit*2;
     		int stackHeight = unplayed.height();
     		double vscale = 1.0/15+0.5/Math.max(8,stackHeight);
     		double vsize = hscale+(stackHeight*vscale)*unit+unit*2;
     		double vstep = vsize/(stackHeight+1);
     		int boxtop = cy - (int)(vsize)+unit*2;
            boolean canHitCards = (allowed_to_edit||(!getActivePlayer().spectator && (G.offline()||(getActivePlayer().boardIndex==pl)))) 
            			? gb.LegalToHitCards(unplayed):false;    
            // a little assist for the robot
            Rectangle boxRect = new Rectangle(cx-unit,boxtop,unit*2,(int)(vstep*stackHeight));

            unplayed.drawStack(gc,this,null,unit*2,cx,cy,0,vscale,null);
            // note we use anyHit because we can pick up cards out of turn sequence.
            // we're doing our own mouse sensitivity because the 2:1 aspect ratio is unexpected,
            // and we need to point to a particular card.
            //G.frameRect(gc,Color.red,boxRect);
        	if(G.pointInRect(canHitCards?anyHit:null,boxRect))
        	{	//showingLid = pl;
         		int ind = Math.max(0,Math.min(stackHeight-1,(int)((G.Top(anyHit)-boxtop)/vstep)));
         		anyHit.hitCode = unplayed.rackLocation;
         		anyHit.hitObject = unplayed;
        		RajChip ch = unplayed.chipAtIndex(ind);
        		if(ch!=null)
        		{   boolean moving = (movingObjectIndex()<0);
        			anyHit.hit_index = ind;
        			anyHit.setHelpText(s.get(ClickToSelect));
        			anyHit.arrow = moving?StockArt.DownArrow:StockArt.UpArrow;
        			anyHit.awidth = unit;
        			if(moving) { drawCardFace(gc,new Rectangle(G.Left(anyHit)-unit*3,G.Top(anyHit)-5*unit/3,unit*2,unit*3),ch); }
        		}
         	}
            //G.frameRect(gc,Color.yellow,boxRect);

     	}}
    }
	// draw a box of spare chips. For raj it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void drawPrizePool(Graphics gc, Rectangle r, HitPoint highlight,RajBoard gb,RajCell pool,boolean up,int unit)
    {
        boolean canhit = gb.LegalToHitPrizes(pool) && G.pointInRect(highlight, r);
        int nPrizes = pool.height();
        int tvalue = 0;
        //RajChip.getTray().drawChip(gc,this,r,null);
        for(int lim=nPrizes-1; lim>=0; lim--) { tvalue += pool.chipAtIndex(lim).prizeValue(); }
        
        HitPoint.setHelpText(highlight,r,s.get(PrizeCountString,nPrizes,tvalue));
        if (canhit)
        {   highlight.hitCode = pool.rackLocation;
            highlight.hitObject = pool;
            highlight.arrow = (movingObjectIndex()>=0)?StockArt.DownArrow:StockArt.UpArrow;
            highlight.awidth = unit;
         }

        if (gc != null)
        { int yloc = G.centerY(r);
          int xloc = G.Left(r);
          double space = 0.08;
          int total = pool.totalPrizeValue();
          pool.drawHStack(gc,this,highlight,unit*2,xloc,yloc,0,space,null);
          if(pool.height()>0)
          {
          GC.setFont(gc,largeBoldFont());
          GC.Text(gc,true,xloc+unit+(int)(nPrizes*space*unit*2),
        		  yloc-unit,unit,unit,
        		  Color.black,null,s.get(TotalString)+"\n"+total);
          }
        }
     }
    /**
    * sprites are normally a game piece that is "in the air" being moved
    * around.  This is called when dragging your own pieces, and also when
    * presenting the motion of your opponent's pieces, and also during replay
    * when a piece is picked up and not yet placed.  While "obj" is nominally
    * a game piece, it is really whatever is associated with b.movingObject()
    
      */
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
    	if(xp>0 && yp>0)
    	{
    	RajChip ch = RajChip.getChip(obj);
    	if(ch.isCard() &&  (G.offline() || (reviewOnly || (!getActivePlayer().spectator && (bb.playerOwning(ch.cardColor())==getActivePlayer().boardIndex)))))
    		{ drawCardFace(g,new Rectangle(xp-CELLSIZE,yp-5*CELLSIZE/3,CELLSIZE*2,CELLSIZE*3),ch);
    		}
    	else { ch.drawChip(g,this,CELLSIZE*2, xp, yp, null); }
    }
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  


    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean review = reviewMode() && !mutable_game_record;
      // erase
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //G.fillRect(gc, fullRect);
     textures[review?BACKGROUND_REVIEW_INDEX:BACKGROUND_INDEX].tileImage(gc, fullRect);  
     
      if(remoteViewer<0)
    	  {textures[BACKGROUND_TILE_INDEX].tileImage(gc,boardRect);   
      GC.frameRect(gc,Color.black,boardRect);
    	  }
      bb.SetDisplayParameters( 1.0, 1.0, 0, 0, 0); // shrink a little and rotate 30 degrees
      bb.SetDisplayRectangle(boardRect);
      
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      //gb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

 
    }
    
    /**
     * translate the mouse coordinate x,y into a size-independent representation
     * presumably based on the cell grid.  This is used to transmit our mouse
     * position to the other players and spectators, so it will be displayed
     * at approximately the same visual spot on their screen.  
     * 
     * Some trickier logic may be needed if the board has several orientations,
     * or if some mouse activity should be censored.
     */
    static final String HIDDEN = "H";
    public String encodeScreenZone(int x, int y,Point p)
    {	// censor the player card rectangles
    	for(commonPlayer pl : players)
    	{	if(pl!=null)
    		{	Rectangle pr = playerCardRects[pl.boardIndex];
    			if(G.pointInRect(x,y,pr)) 
    			{
    				return(HIDDEN);
    			}
    		}
    	}
    	return(super.encodeScreenZone(x,y,p));
    }
    
	/**
	 * Overriding this method so that we can "animate" a wolf while the
	 * wolf player is hiding & choosing a meal.
	 */
	public Point decodeScreenZone(String z,int x, int y) {
		if (HIDDEN.equals(z)) {
			// This is indication that the wolf player is moving within
			// the area with a hidden wolf, so use the Y coordinate as
			// the animation updated value.
			return new Point (0, 0);
		}

		return super.decodeScreenZone(z,x, y);
	}

   /** draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */
    private void drawBoardElements(Graphics gc, RajBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  Either gc or highlight might be
    	// null, but not both.
        //
        // using closestCell is preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        RajCell closestCell = gb.closestCell(highlight,brect);
        RajState state = gb.getState();
        boolean anyhit = G.offline() 
        				|| (reviewOnly && ((state==RajState.CONFIRM_CARD_STATE) || (state==RajState.PLAY_STATE)));
        boolean hitCell = !getActivePlayer().spectator
        					&& !isQuietTime(state)
        					&& gb.LegalToHitBoard(closestCell,anyhit);
        boolean moving = (movingObjectIndex()>=0);
        if(hitCell)
        { // note what we hit, row, col, and cell
          highlight.hitObject = closestCell;
          highlight.awidth = CELLSIZE;
          highlight.hitCode = moving ? RajId.EmptyBoard : RajId.BoardLocation;
          highlight.arrow = moving ? StockArt.DownArrow:StockArt.UpArrow;
        }
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        if (gc != null)
        {
        for(RajCell cell = gb.allCells; cell!=null; cell=cell.next)
          {
            boolean drawhighlight = (hitCell && (cell==closestCell)) 
   				|| gb.isDestCell(cell) 		// is legal for a "drop" operation
   				|| gb.isSource(cell);	// is legal for a "pick" operation+
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            RajChip top = cell.topChip();
            if (drawhighlight)
             { // checking for pointable position
            	 StockArt.SmallO.drawChip(gc,this,gb.cellSize()*5,xpos,ypos,null);                
             }
            boolean show = (state==RajState.AWARD_PRIZE_STATE) && (top!=null) && (top.isCard());
            double skip = Math.min(0.7,(5*0.7)/cell.height());
            cell.drawHStack(gc,this,null,CELLSIZE*2,xpos,ypos,0,skip,show?"#":null);
            }
        }
    }
    
    // this is non-standard, but for Raj the moving obects are per-player
    // this allows simultaneous card moves to display properly
    public int getMovingObject(HitPoint highlight)
    {	return(getActivePlayer().spectator?NothingMoving:movingObjectIndex()); 
    }

    /**
     * draw the main window and things on it.  
     * If gc!=null then actually draw, 
     * If selectPos is not null, then as you draw (or pretend to draw) notice if
     * you are drawing under the current position of the mouse, and if so if you could
     * click there to do something.  Care must be taken to consider if a click really
     * ought to be allowed, considering spectator status, use of the scroll controls,
     * if some board token is already actively moving, and if the game is active or over.
     * <p>
     * This dual purpose (draw, and notice mouse sensitive areas) tends to make the
     * code a little complicated, but it is the most reliable way to make sure the
     * mouse logic is in sync with the drawing logic.
     * <p>
    General GUI checklist
<p>
<li>vcr scroll section always tracks, scroll bar drags
<li>lift rect always works
<li>zoom rect always works
<li>drag board always works
<li>pieces can be picked or dragged
<li>moving pieces always track
<li>stray buttons are insensitive when dragging a piece
<li>stray buttons and pick/drop are inactive when not on turn
*/
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  

       RajBoard gb = disB(gc);
       RajState state = gb.getState();
       if((state==RajState.PLAY_STATE) && gb.hasDroppedCard(getActivePlayer().boardIndex)) { state = RajState.WAIT_STATE; }
       boolean moving = (getMovingObject(selectPos)>=0);
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
       
       drawHiddenWindows(gc, selectPos);		// draw before the main screen draw, so the animations will see the main

       gameLog.redrawGameLog2(gc, nonDragSelect, logRect, Color.black,boardBackgroundColor,standardBoldFont(),standardPlainFont());
       drawBoardElements(gc, gb, boardRect, ((state==RajState.PLAY_STATE)||(state==RajState.CONFIRM_CARD_STATE))?selectPos:ourTurnSelect);
       drawPrizePool(gc, prizeRect, selectPos,gb,gb.prizes,false,CELLSIZE);
       int nPlayers = gb.nPlayers();
       for(int i=0;i<nPlayers;i++)
    	   {
    	    commonPlayer pl = getPlayerOrTemp(i);
    	    pl.setRotatedContext(gc, selectPos,false);
    	   	drawPlayerBoard(gc,i,(i==gb.whoseTurn)?ourTurnSelect:null,gb,selectPos);
    	   	pl.setRotatedContext(gc, selectPos, true);
    	   }
       GC.setFont(gc,standardBoldFont());
       // draw the board control buttons 
   	commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
   	double messageRotation = pl.messageRotation();

		if (state != RajState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
            if ((!SIMULTANEOUS_PLAY || (state==RajState.RESIGN_STATE)) 
            		&& GC.handleRoundButton(gc, doneRect, 
            		(gb.DoneState() ? buttonSelect : null), s.get(DoneAction),
                    HighlightColor, rackBackGroundColor))
            {	// always display the done button, but only make it active in
            	// the appropriate states
                buttonSelect.hitCode = GameId.HitDoneButton;
            }
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);
        }

 
        drawPlayerStuff(gc,(state==RajState.PUZZLE_STATE),nonDragSelect,
        			HighlightColor,
        			rackBackGroundColor);
  
 
        // draw the avatars
    	int activePl = (simultaneous_turns_allowed())
    								? getActivePlayer().boardIndex
    								: gb.whoseTurn;
    								

        standardGameMessage(gc,messageRotation,
        		state==RajState.GAMEOVER_STATE?gameOverMessage():s.get(state.getDescription()),
        				state!=RajState.PUZZLE_STATE,
        				activePl,
        				stateRect);
        PlayerBoard pb = gb.getPlayerBoard(gb.whoseTurn);
       	if(pb!=null) { pb.cardBack.drawChip(gc,this,iconRect,null,0.75); }

        goalAndProgressMessage(gc,nonDragSelect,s.get(GoalString),progressRect, goalRect);
        //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for raj
    
        // draw the vcr controls
        drawVcrGroup(nonDragSelect, gc);

    }
    
    //
    // in simultaneous mode, all players clocks advance
    public void updatePlayerTime(long inc,commonPlayer p)
    {	if(SIMULTANEOUS_PLAY)
    	{
    	BoardState st = reviewMode()?History.pre_review_state:bb.getState();
    	if(st.simultaneousTurnsAllowed())
    	{
    		for(commonPlayer pl : players)
    		{
    			if(pl!=null)
    			{
    				if(!bb.hasDroppedCard(pl.boardIndex))
    				{
    					super.updatePlayerTime(inc,pl);
    				}
    			}
    		}
    		return;
    	}}
    	// default is to do the usual
   		super.updatePlayerTime(inc,p);
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
        if(!reviewMode())
        {
        switch(bb.getState())
    	{
    	default: 
    		startIdleTime = 0;
    		break;
    	case CONFIRM_CARD_STATE:
    		if(startIdleTime==0) 
    			{ startIdleTime = G.Date(); 
    			}
    	}}
        startBoardAnimations(replay,bb.animationStack,CELLSIZE*2,MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
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
        return (new Rajmovespec(st, player));
    }

    
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 * <p>
 * Note on debugging: If you get here mysteriously with hitOjbect and hitCode
 * set to default values, instead of the values you expect, you're probably
 * not setting the values when the gc is null.
 */
    public void StartDragging(HitPoint hp)
    {	RajCell hitCell = hitCell(hp);
    	int remoteIndex = remoteWindowIndex(hp);
        if (hp.hitCode instanceof RajId) // not dragging anything yet, so maybe start
        {
        RajId hitObject =  (RajId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    case ShowCards:
	    	if(remoteIndex>=0)
	    	{	PlayerBoard pl = bb.getPlayerBoard(remoteIndex);
	    		if(pl!=null) { pl.hiddenShowCards = !pl.hiddenShowCards; }
	    	}
	    	break;
	    case PlayerPrizes:
       case PlayerCards:
       case PlayerDiscards:
    	{
	    	boolean simultaneous = simultaneous_turns_allowed();
	    	if(remoteIndex>=0)
	    	{	
	    		int cx = hp.hit_index;
	    		PlayerBoard pb = bb.getPlayerBoard(remoteIndex);
	    		if(pb!=null)
	    		{
	    		RajCell from = pb.cards;
	    		RajCell location = bb.goodLocation(pb.color,from.centerX(),from.centerY());
	    		PerformAndTransmit("emove "+remoteIndex+" "+from.col+" "+cx+" "+location.col+" "+location.row+" "+remoteIndex);
	    	}
	    	}
	    	else if(simultaneous)
	    	{	if(!isTouchInterface()) 
	    		{
	    		int ord = hitCell.col-'A';
	    		int cx = hp.hit_index;
	    			bb.setMyIndex(ord,true);
	    		PerformAndTransmit("epick "+ord+" "+hitObject.shortName+" "+hitCell.col+" "+cx); 
	    		}
	    	}
	    	else
	    	{	
	    		int ord = hitCell.col-'A';
	    		int cx = hp.hit_index;
	    		bb.setMyIndex(ord,true);
	    		PerformAndTransmit("pick "+hitObject.shortName+" "+hitCell.col+" "+cx);	
    		}}
    		break;
	    case PrizePool:
	    	PerformAndTransmit("Pick P @ -1");
	    	break;
	    case BoardLocation:
	    	{	RajChip obj = hitCell.topChip();
	    	if(obj.isCard() && (G.offline() || (simultaneous_turns_allowed())))
	    	{	int ord = bb.playerOwning(obj.cardColor());
	    		bb.setMyIndex(ord,true);  
	    		if(remoteIndex>=0)
	    		{
	    		PerformAndTransmit("Eunmove "+ord+" "+hitCell.col+" "+hitCell.row);
	    		}
	    		else
	    		{
	    		PerformAndTransmit("Epickb "+ord+" "+hitCell.col+" "+hitCell.row);
	    		}
	    	}
	    	else {
	    		PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    	}}
	    	break;
        }
        }
    }

	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
 * <p>
 * Note on debugging: If you get here mysteriously with hitOjbect and hitCode
 * set to default values, instead of the values you expect, you're probably
 * not setting the values when the gc is null.
	 */
    public void StopDragging(HitPoint hp)
    {
    CellId id = hp.hitCode;
    int remoteIndex = remoteWindowIndex(hp);
    if(!(id instanceof RajId))
    	{	if(remoteIndex>=0) {}
    		else if((id==DefaultId.HitNoWhere)
    			&& !getActivePlayer().spectator 
    			&& !G.offline() 
    			&& simultaneous_turns_allowed())
    		{
    			String dn = bb.unDropMove();
    			if(dn!=null) { PerformAndTransmit(dn); }
    			String up = bb.unPickMove();
    			if(up!=null) { PerformAndTransmit(up); }
    			missedOneClick = false;
    		}
    		else { missedOneClick = performStandardActions(hp,false); }
    		}
	else {
		RajId hitCode = (RajId)hp.hitCode;
		int mo = movingObjectIndex();
    	if(mo>=0)
    	{
		RajCell hitObject = hitCell(hp);
		RajState state = bb.getState();
		RajChip movingObject = RajChip.getChip(mo);
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case BoardLocation:	// we hit an occupied part of the board 
        case EmptyBoard:
			switch(state)
			{	case CONFIRM_CARD_STATE:
				default:
					throw G.Error("Not expecting hit in state %s",state);
				case CONFIRM_STATE:
				case PLAY_STATE:
				case PUZZLE_STATE:
					if(movingObject.isCard() && simultaneous_turns_allowed() )
					{
						PerformAndTransmit("edropb "+ bb.playerOwning(movingObject.cardColor())+" "+hitObject.col+" "+hitObject.row);
					}
					else
					{
						PerformAndTransmit("dropb "+hitObject.col+" "+hitObject.row);
					}
					break;
			}
			break;
        case PlayerPrizes:
        case PlayerDiscards:
        case PlayerCards:
        	{
        	if(remoteIndex>=0) {}
        	else
        	{
        	int cx = hp.hit_index;
        	String op = (simultaneous_turns_allowed()) ? ("edrop "+(hitObject.col-'A')+" ") : "Drop "; 
        	PerformAndTransmit(op+ hitCode.shortName+" "+hitObject.col+" "+cx);
        	}}
        	break;
        	
        case PrizePool:
           if(movingObjectIndex()>=0) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop P @ -1");
			}
           break;
 
         }
    	}
    	else {
    		switch(hitCode)
    		{
    		default: break;
            case PlayerPrizes:
            case PlayerDiscards:
            case PlayerCards:
        	if(remoteIndex>=0) {}
        	else
        	{	RajCell hitObject = hitCell(hp);
        		boolean sim = simultaneous_turns_allowed();
        		int ord = hitObject.col-'A';
        		bb.setMyIndex(ord,true);
        		if(sim)
        		{	
        			PerformAndTransmit("epick "+ord+" "+hitCode.shortName+" "+hitObject.col+" "+hp.hit_index); 
        	}
        		else
        		{
        		
        		PerformAndTransmit("pick "+hitCode.shortName+" "+hitObject.col+" "+hp.hit_index); }
    	}
    		}}}
	
	}

    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parseable by {@link online.game.commonCanvas#performHistoryInitialization}
     * @return return what will be the init type for the game
     */
     public String gameType() 
    	{
    	   // in games which have a randomized start, this method would return
    	   // return(bb.gametype+" "+bb.randomKey); 
    	return(bb.gametype+" "+bb.randomKey+" "+bb.nPlayers()); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Raj_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int key = G.IntToken(his);
    	int np = G.IntToken(his);
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // int rk = G.IntToken(his);
    	// bb.doInit(token,rk);
        adjustPlayers(np);
        bb.doInit(token,key,np,getActivePlayer().boardIndex);
    }


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


    public int ScoreForPlayer(commonPlayer p)
    {	return(bb.ScoreForPlayerNow(p.boardIndex));
    }


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return new RajPlay();
    }
    public boolean replayStandardProps(String name,String value)
    {	nextIntCompatabilityKludge(bb,name,value,"Aug 25 2012");
    	return(super.replayStandardProps(name,value));
    }
    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
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
            	String game = tok.nextToken();
            	int key = G.IntToken(tok);
            	int np = G.IntToken(tok);
                bb.doInit(game,key,np,0);
                adjustPlayers(np);
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
    
    // ***************************************************************************************
    // ******************                                                                 ****
    // ****************** special things we do because of the simultaneous movement phase ****
    // ******************                                                                 ****
    // ***************************************************************************************
    
    /** handle the run loop, and any special actions we need to take.
     * The mouse handling and canvas painting will be called automatically.
     * <p>
     * This is a good place to make notes about threads.  Threads in Java are
     * very dangerous and tend to lead to all kinds of undesirable and/or flakey
     * behavior.  The fundamental problem is that there are three or four sources
     * of events from different system-provided threads, and unless you are very
     * careful, these threads will all try to use and modify the same data
     * structures at the same time.   Java "synchronized" declarations are
     * hard to get right, resulting in synchronization locks, or lack of
     * synchronization where it is really needed.
     * <p>
     * This toolkit addresses this problem by adopting the "one thread" model,
     * and this is where it is.  Any other threads should do as little as possible,
     * mainly leave breadcrumbs that will be picked up by this thread.
     * <p>
     * In particular:
     * GUI events do not respond in the native thread.  Mouse movement and button
     * events are noted for later.  Requests to repaint the canvas are recorded but
     * not acted upon.
     * Network I/O events, merely queue the data for delivery later.
     *  */
        long award_prize_end_time = 0;
        static final int AWARD_PRIZE_DELAY = 2000;		// 2 seconds
        static final int FINISH_AUCTION_DELAY = 3000;	// 3 seconds
        static final int FINISH_AUCTION_UNDO = 2000;	// 2 seconds
        static long startIdleTime = 0;
        //
        // return true if we're locked into the auction end
        //
        public boolean isQuietTime(RajState state)
        {
        	switch(state)
        	{
        	default: return(false);
        	case CONFIRM_CARD_STATE:
        		boolean yes = (G.Date()-startIdleTime)>FINISH_AUCTION_UNDO;
        		return(yes);
        	}
        }
        public void ViewerRun(int wait)
        {
            super.ViewerRun(wait);
            runAsyncRobots();
            RajState state = bb.getState();
            
            if(initialized && !reviewMode())
            { 
            
            if(remoteViewer<0 && (started || reviewOnly) && OurMove() )
            {	commonPlayer p = players[bb.whoseTurn];
            	if(p!=null && p.robotPlayer==null)
            	{
            	switch(state)
            	{
            	case AWARD_PRIZE_STATE:
            		{
            		// in award_prize_state, we show the cards played and wait a short time
            		// for the players to see it.  Then the player whose turn it is auto-generates
            		// the award prize move.
            		if(bb.award_prize_timer==0)
            		{	// just starting, set up the delay
            			bb.award_prize_timer++;
            			award_prize_end_time = G.Date() + AWARD_PRIZE_DELAY; 
            		}
            		else if(G.Date()>award_prize_end_time)
            		{if(bb.award_prize_timer>0)
            			{bb.award_prize_timer = -1;
            			 PerformAndTransmit("Award");
            			}
            		}
            	}
            		break;
            	case CONFIRM_CARD_STATE:
            		if((G.Date()-startIdleTime)>FINISH_AUCTION_DELAY)
            	{	// our turn, and we think everyone is ready.
            		// the theory is that if anyone becomes NOT ready, they will
            		// revert to "play" state until they also see all-ready.
            		// meantime, we're already committed but oh-well - we were ready, right?
            		String mov = bb.cardMove();
            		if(mov!=null)
            		{
            		// we got the move.  If we didn't it's because we're the one who
            		// sneaked in a mind-change when the others weren't looking.
            		PerformAndTransmit(mov);
            		}
            	}
            		break;
            	case SELECT_PRIZE_STATE:
            	{	// time to select a new prize.
            		PerformAndTransmit("Select -1");
            	}
            		break;
            	default: break;
            	}
            	}}}
        }
    //
    // this is called both when starting a new game, and when restoring a game from history.
    // we need to inform our board which index is "us"
    //
    public void startPlaying()
    {	super.startPlaying();
    	bb.setMyIndex(getActivePlayer().boardIndex,SIMULTANEOUS_PLAY);
    }

    //
    // if we're in a simultaneous move state, start the robot immediately rather 
    // than the normal wait for his turn.
    //
    public void startRobotTurn(commonPlayer p)
    {
       	switch(bb.getState())
    	{
    	case PLAY_STATE:
	    	if(!bb.hasDroppedCard(p.boardIndex))
					{ super.startRobotTurn(p); 
					}
	    	break;
	    default: 
	    	super.startRobotTurn(p);
    	}

    }
    //
    // get the player who may be running a robot. This is used
    // for board and progress displays.
    //
    public commonPlayer currentRobotPlayer()
    {	
    	switch(bb.getState())
    	{
    	case PLAY_STATE:
    	
    	if(SIMULTANEOUS_PLAY)
    	{
   			for(commonPlayer pp : players) 
    				{ if((pp!=null) 
    						&& pp.robotStarted() 
    						&& !bb.hasDroppedCard(pp.boardIndex))
    					{
    						return(pp);
    					}
    				}
    		}
			break;
		default:
			break;
    	}
    	return(super.currentRobotPlayer());
    }
    //
    // start robots if this is an asynchronous phase
    // if it's a normal, synchronous phase, the main game
    // controller will do it.
    //
    public void runAsyncRobots()
    {	
       	switch(bb.getState())
    	{
    	case PLAY_STATE:
    	if( !GameOver() && allRobotsIdle())
    	{ 	for(commonPlayer pp : players)
			{	if((pp!=null) && !bb.hasDroppedCard(pp.boardIndex))
					{ startRobotTurn(pp); 
					}
			}}
			break;
		default: break;
    	}
    }
    	
    /**
     * the trickiest bit of simultaneous play is the inherent uncertainty that
     * the players will all see the same sequence of moves.  With up to 5 players
     * picking up cards and placing them, they will inevitably disagree about who
     * was first.  The server establishes a canonical order, but it's difficult
     * to use because the protocol is to play your move immediately rather than
     * wait for a round trip to the server.
     * 
     * if two players come to disagree about the sequence of moves, every time
     * they add a move to the server it causes consternation because it seems to
     * be contradicting the previous player.
     * 
     * After a lot of thought, the solution implemented here is to remove ALL ephemeral
     * moves from the game record, and tack on the unconfirmed, changeable, ephemeral moves
     * as a separate list.
     */

    
    /**
	 * this is a filter used to present only the ephemeral moves
	 * at the end of the history for use by the game filter.
	 * @author ddyer
	 *
     */
	class EphemeralFilter implements online.game.commonMove.MoveFilter 
    {
		int seen = 0;		// a bitmask of the player indexes that have committed their ephemeral cards
		public EphemeralFilter(int see) { seen = see; } 
		public boolean reNumber() { return(true); }
		public boolean included(commonMove m) 
		{	return(m.isEphemeral() && ((seen & (1<<m.player))==0));
		}
    }
    
    
    /**
     * this appends the ephemeral moves to the normal "ephemeral" history string.
     */
    public String formEphemeralMoveString()
    {	
    	ByteArrayOutputStream b = new Utf8OutputStream();
    	PrintStream os = Utf8Printer.getPrinter(b);
    	os.print(" ");
    	int idx = History.size();
    	int hasSeen = 0;	// keeps track of the players we've seen make permanant moves.
    	boolean done = false;
    	while(!done && (--idx >=0))
    	{
    		commonMove m = History.elementAt(idx);
    		if(m.op==MOVE_CMOVE)
    		{	hasSeen |= (1<<m.player);	// note we've seen this player make a permanant move, so 
    			// we should filter out his ephemeral moves.
    		}
    		else if(m.op==MOVE_SELECT)
    		{	// found the start of a move
    			done = true;
    			// we backed up to the beginning of the current move.
    			m.formHistoryTree(os,new EphemeralFilter(hasSeen),m.index()+10,false);
    	    	os.print(" ");
    		}
    	}
    	os.print(KEYWORD_END_HISTORY);
    	os.flush();
    	return(b.toString());
    }
    

    /**
     * support for hidden windows
     * 
     */
     
    // create the appropriate set of hidden windows for the players when
    // the number of players changes.
    
    public void adjustPlayers(int np)
    {
        int HiddenViewWidth = 600;
        int HiddenViewHeight = 250;
    	super.adjustPlayers(np);
        if(RpcService.isRpcServer() || VNCService.isVNCServer() || G.debug())
        {
        createHiddenWindows(np,HiddenViewWidth,HiddenViewHeight);
        }
    }
    
    public String nameHiddenWindow()
    {	return ServiceName;
    }


    public HiddenGameWindow makeHiddenWindow(String name,int index,int width,int height)
	   	{
	   		return new HiddenGameWindow(name,index,this,width,height);
	   	}
    
    public void drawHiddenWindow(Graphics gc,HitPoint hp,int index,Rectangle r)
    {
    	String name = prettyName(index);
    	int margin = G.minimumFeatureSize()/2;
    	int h = G.Height(r)-margin*2;
    	int w = G.Width(r)-margin*2;
    	int l = G.Left(r)+margin;
    	int t = G.Top(r)+margin;
    	int topPart = h/10;
    	Font myfont = G.getFont(largeBoldFont(), topPart/2);
    	Rectangle stateRect = new Rectangle(l,t,w/2,topPart);
    	PlayerBoard pl = bb.getPlayerBoard(index);
    	if (pl!=null)
    	{
    	RajState vstate = bb.getState();
    	GC.setFont(gc,myfont);
    	if (remoteViewer<0)
    		{GC.fillRect(gc,rackBackGroundColor,r);
    		}
    	standardGameMessage(gc,
        		vstate==RajState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=RajState.PUZZLE_STATE,
        				bb.whoseTurn,
        				stateRect);
 
       	RajChip ch = RajChip.getCardBack(pl.color);
       	int xp = l+w-topPart;
       	int yp = t+3*topPart/2;
    	ch.drawChip(gc,this, topPart*2, xp,yp,null);
    	
    	StockArt icon = pl.hiddenShowCards ? StockArt.NoEye : StockArt.Eye;
    	icon.drawChip(gc,this,hp,RajId.ShowCards,topPart,xp,yp+topPart*2,null);
    	GC.setFont(gc, myfont);
    	GC.Text(gc,true,l+w/2,t,w/2,topPart,Color.black,null,s.get(ServiceName,name));
    	RajState state = bb.getState();
    	RajCell playedCard = pl.droppedCard;
    	boolean canHit = (state==RajState.PLAY_STATE) && (playedCard==null);
    	int sz = (int)(topPart*2.5);
    	RajCell cards = pl.cards;
    	if(h>w)
    	{	int ypos = h/3;
    		double step = Math.min(0.64, (double)(h-ypos)/(sz*(cards.height()+3)));
        	drawStack(gc,cards,canHit?hp:null,sz,l+topPart, t+ypos,0,-step, pl.hiddenShowCards);
   		
    	}
    	else
    	{
    	double step = Math.min(0.64, (double)w/(sz*(cards.height()+3)));
    	drawStack(gc,cards,canHit?hp:null,sz,l+topPart, t+2*h/3,step,0, pl.hiddenShowCards);
    	}
    	if(playedCard!=null)
    	{	drawStack(gc,playedCard,hp,(int)(topPart*3),l+w/2, t+h/4,
    			0 ,0, pl.hiddenShowCards);
    	}
    	//drawPlayerBoard(gc,hp,hp,b,pl,new Rectangle(l,t+topPart,w,h-topPart),true);
    	if(bb.LegalToHitCards(pl.cards))
    	{
    		GC.setFont(gc,myfont);
    		GC.Text(gc, true, l, t+topPart,w/5,topPart,
    				Color.red,null,YourTurnMessage);
    	}
    }}
}

