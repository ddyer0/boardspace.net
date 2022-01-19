package oneday;

import static com.codename1.util.MathUtil.atan2;
import bridge.Color;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Image;
import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.SimpleSprite;
import lib.StockArt;
import lib.SimpleSprite.Movement;
import oneday.OnedayBoard.OnedayLocation;
import oneday.OnedayBoard.PlayerBoard;

import static oneday.OnedayMovespec.*;

/**
 * The viewer for One Day in London is pretty standard except for the game setup
 * phase, where all the players get to select and place their cards at the same time.
 * Consequently, there can be 4 moving cards and the precise ordering of the game record 
 * is indeterminate until sequential play begins.  A number of careful "dance moves"
 * are needed to support this.
 * 
 * During placement:
 * (1) picks and drops are ephemeral, and not transmitted to the other players.
 *     The moving card is still known to the other player because the
 *     mouse movement messages convey it.
 * (2) completed pick/drop sequences that place a card are replaced by a "place" atomic
 *     operation, which doesn't disturb the local board state machine.  That is, they
 *     don't use the customary "pickObject" and "dropObject" subroutines.
 * (3) formHistoryString and formEphemeralHistoryString are customized so the ephemeral
 *     placement moves are placed in the ephemeral part.  useEphemeralBuffer is customized
 *     to replay the ephemeral moves.  the simultaneous_moves_allowed method prevents
 *     the game manager from consolidating the current move list until the ephemeral
 *     phase ends.
 * (4) at the end of the placement phase, the ephemeral moves are put into canonical
 *     order and replaced by non-ephemeral placement moves. 
 * (5) some special logic is needed to start robots in the asynchronous phase.  @see runAsyncRobots();
 * (6) player clocks need to run concurrently when in simultaneous play mode @see updatePlayerTime();
 *  
 *  
 *  TODO: OnedayInLondon could use an upgrade to the game record
*/
public class OnedayViewer extends CCanvas<OnedayCell,OnedayBoard> implements OnedayConstants
{

    public commonMove convertToSynchronous(commonMove m)
    {	switch(m.op)
    				{
    				default: throw G.Error("Not expecting move %s",m);
    				case EPHEMERAL_PICK: 
    				case EPHEMERAL_DROP:
    						break;	// remove
    				case EPHEMERAL_TO_RACK:
    					m.op = MOVE_TO_RACK;	// convert to a non-ephemeral move
			break;			
    			}
    	return(m);
    }
    
           
    public boolean allowBackwardStep()
    {
    	return(super.allowBackwardStep() && allowUndo());
     }
    public boolean allowUndo()
    {
    	if(super.allowUndo())
    		{ OnedayMovespec m = (OnedayMovespec)getCurrentMove();
    		  return( (m!=null)
    				  && (m.op!=NORMALSTART)
    				  && !((m.op==MOVE_PICK) && (m.source==OneDayId.DrawPile)));
    		}
    	return(false);
    }
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color boardBackgroundColor = new Color(185,164,189);
    private Color rackBackGroundColor = new Color(155,164,189);
 
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// images
    // private state
    private OnedayBoard b = null; 	// the board from which we are displaying
    private OnedayChip featuredCard = null;
    private int CELLSIZE; 			// size of the layout cell.  
    private int SQUARESIZE;			// size of a board square
    private int CARDSIZE;
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle bigRackRect = addRect("bigRackRect");
    
    private Rectangle playerChipRect[] = addRect("chip",4);
    private Rectangle discardPile = addRect("discardPiles");
    private Rectangle drawPile = addRect("drawPile");
    private Rectangle startingCardRect = addRect("startingCards");
    private Rectangle localViewRect = addRect("localViewRect");
    private Rectangle platformViewRect = addRect("platformViewRect");
    private Rectangle playerStateRect = addRect("playerState");
    private Rectangle trainActionRect = addRect("trainAction");
   public void preloadImages()
    {	
       	Station.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
    	}
        gameIcon = images[BOARD_INDEX];
    }


	/**
	 * 
	 * this is the real instance initialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	// int players_in_game = Math.max(3,info.getInt(exHashtable.PLAYERS_IN_GAME,4));
    	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
    	super.init(info,frame);
    	G.print(G.getSystemProperties());
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new OnedayBoard(info.getString(OnlineConstants.GAMETYPE, OnedayVariation.Standard.name),randomKey,players_in_game);
        doInit(false);

        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	InternationalStrings.put(OnedayStrings);
        	InternationalStrings.put(OnedayStringPairs);
        }
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        int np = b.nPlayers();
        b.doInit(b.gametype,b.randomKey,np);			// initialize the board
        if(!preserve_history)
    	{   
    		startFirstPlayer();
    	}

    }

    /**
     * translate the mouse coordinate x,y into a size-independent representation
     * presumably based on the cell grid.  This is used to transmit our mouse
     * position to the other players and spectators, so it will be displayed
     * at approximately the same visual spot on their screen.  
     * The results of this function only have to be interpreted by {@link #decodeScreenZone}
     * Some trickier logic may be needed if the board has several orientations,
     * or if some mouse activity should be censored.
     */
    public String encodeScreenZone(int x, int y,Point p)
    {	int w = G.Width(fullRect);
    	int h = G.Height(fullRect);
    	if(!reviewMode() && !mutable_game_record && w>0 && h>0)
    	{
    	//
    	// when sumultaneous moves are going on, encode the movements so they can be 
    	// decoded as heading toward the small player racks in stead of toward the
    	// big rack at the bottom.
    	//
    	G.SetLeft(p, ((x-G.Left(startingCardRect))*100)/w);
    	G.SetTop(p, ((y-G.Top(startingCardRect))*100)/h);
    	return("SETUP"+getActivePlayer().boardIndex);
    	}
    	else
    	{
    	return(super.encodeScreenZone(x,y,p));
    	}
    }
    /**
     * invert the transformation done by {@link #encodeScreenZone}, returning 
     * an x,y pixel address on the main window.
     * @param z
     * @param x
     * @param y
     * @return a point representing the decoded position
     */
    public Point decodeScreenZone(String z,int x,int y)
    {	if(z!=null && z.startsWith("SETUP"))
    	{	int pl = (z.charAt(5)-'0');
    		if((pl>=0)&&(pl<=playerChipRect.length))
    		{	Rectangle chip = playerChipRect[pl];
    			int xx = G.Left(chip) + x*G.Width(chip)/100;
    			int yy = G.Top(chip);
    			return(new Point(xx,yy));
    			//int v = ((x+y)/2)*fullRect.
    			
    		}
    		return(new Point(G.Left(startingCardRect)+(x*G.Width(fullRect))/100,G.Top(startingCardRect)+y*G.Height(fullRect)/100));
    	}
    	else { return(super.decodeScreenZone(z,x,y));
    	}
    }

    /**
     * update the players clocks.  The normal thing is to tick the clocks
     * only for the player whose turn it is.  Games with a simultaneous action
     * phase need to do something more complicated.
     * @param inc the increment (in milliseconds) to add
     * @param p the current player, normally the player to update.
     */
    public void updatePlayerTime(long inc,commonPlayer p)
    {	if(b.getState()==OnedayState.Place)
    	{
    		for(int i=0,lim=nPlayers(); i<lim; i++)
    		{	commonPlayer pl = getPlayerOrTemp(i);
    			if(!b.rackFull(pl.boardIndex))		// this player has not finished
    			{
    				super.updatePlayerTime(inc,pl); 	// keep on ticking
    			}
    		}
    	}
    	else 
    	{ super.updatePlayerTime(inc,p); 
    	}
    }
    
    private void createPlayerGroup(commonPlayer pl0,Rectangle playerChipRect,int inx,int iny,int xsize,int ysize)
    {
        Rectangle timeRect = pl0.timeRect;
        Rectangle altTimeRect = pl0.extraTimeRect;
        Rectangle animRect = pl0.animRect;
        Rectangle playerRect = pl0.nameRect;
        Rectangle picRect = pl0.picRect;
        Rectangle box = pl0.playerBox;
        int CS = (xsize-ysize)/13;
        G.SetRect(box, inx, iny, xsize, ysize);
        // first player portrait
        G.SetRect(picRect, inx, iny, ysize-CS,ysize-CS);
        
        //first player name
        G.SetRect(playerRect, G.Right(picRect), iny, CS * 10, 3*CS/2);

       	
        // time display for first player
        G.SetRect(timeRect, G.Right(playerRect),G.Top(playerRect),CS * 4,CS);
        G.SetRect(altTimeRect,G.Left( timeRect),G.Bottom(timeRect), G.Width(timeRect),G.Height( timeRect));
        
        // first player "i'm alive" animation ball
        G.SetRect(animRect, G.Right(timeRect),G.Top( timeRect),G.Height(timeRect),G.Height(timeRect));
        
       	G.SetRect(playerChipRect, G.Right(picRect), G.Bottom(altTimeRect)+CS/8,CS*14,CS*2);
  
     }

    static private final int ncols = 10;
    static private final int nrows = 7;
    static private final int SUBCELL = 4;	// number of cells in a square

    private boolean playersInColumns(int nPlayers,boolean wideMode,boolean tallMode)
    {
    	return(tallMode);
    }
    public int setLocalBoundsSize(int width,int height,boolean wideMode,boolean tallMode)
    {	if(wideMode) { return(0); }
    	OnedayVariation variation = b.variation;
    	boolean safari = (variation==OnedayVariation.Safari);
    	int nPlayers = b.nPlayers();
    	boolean columns = playersInColumns(nPlayers,wideMode,tallMode);
        int chatHeight = selectChatHeight(height);
        boolean noChat = chatHeight==0;
        double playerVSpace = (columns 
        							? ((nPlayers+1)/2)*2
        							: nPlayers*2);
        double playerHSpace = 4 + (tallMode ? 1 :  (columns ? 10:5));
        double sncols = (ncols+playerHSpace); // more cells wide to allow for the aux displays
        double snrows = (safari? nrows+2 : 6)
        				+ (noChat ? 5 : 0)
        				+ (tallMode 
        						? nrows+playerVSpace+1 
        						: Math.max(nrows,playerVSpace));  
        int cellw = (int)(width / sncols);
        int cellh = (int)((height-(wideMode ? 0 : chatHeight)) / snrows);
        SQUARESIZE = Math.max(SUBCELL,Math.min(cellw, cellh)); //cell size appropriate for the aspect ration of the canvas
 
        return(SQUARESIZE);
    }

    public void setLocalBoundsWT(int x, int y, int width, int height,boolean wideMode,boolean tallMode)
    {   
     	OnedayVariation variation = b.variation;
    	@SuppressWarnings("unused")
		boolean safari = (variation==OnedayVariation.Safari);
    	int nPlayers = b.nPlayers();
        int chatHeight = selectChatHeight(height);
        boolean noChat = chatHeight==0;
        CELLSIZE = SQUARESIZE/SUBCELL;
        int C2 = CELLSIZE/2;
        int ideal_logwidth = CELLSIZE*8*2;
        // game log.  This is generally off to the right, and it's ok if it's not
        // completely visible in all configurations.
        int boardW = SQUARESIZE * ncols;
        
        int boardX = C2;
        
        CARDSIZE =  Math.min((int)(boardW*0.35),(width-boardX-C2)/10);

        G.SetRect(fullRect,x,y,width, height);

        int stateY = (wideMode ? 0 : chatHeight) +CELLSIZE/3;
        int stateH = CELLSIZE*2;
        G.placeRow(boardX + CELLSIZE, stateY,	boardW, stateH,stateRect,noChatRect);
        
        G.SetRect(boardRect, boardX,G.Bottom(stateRect), boardW , SQUARESIZE * nrows);
        boolean columns = playersInColumns(nPlayers,wideMode,tallMode);
        int playerx = 0;
        boolean rightPlacement = false;
        switch(variation)
        {
        case Standard:
	        int bigW = CARDSIZE*10;
	        G.SetRect(bigRackRect,G.Left( boardRect), G.Bottom(boardRect)+C2,
	        		bigW,
	        		Math.min((bigW/10)*2,height-(noChat?12*CELLSIZE:0)-G.Bottom(boardRect)-C2));

	        if(G.Height(bigRackRect)*6.5<G.Width(bigRackRect))
	        {
	        	G.SetWidth(bigRackRect, (int)(G.Height(bigRackRect)*6.5));
	        }
	        int cardW = G.Width(bigRackRect)/10;
	        int cardH = (int)(cardW*1.7);
	        
	        int pileX = G.Right(boardRect)+CELLSIZE;
	        int discardY = G.Bottom( boardRect)-cardH;
	        G.SetRect(drawPile,pileX+CELLSIZE*2,
	        		G.Top(boardRect),
	        		cardW,cardH);
	        rightPlacement = (discardY<G.Bottom(drawPile));
	        G.SetRect(discardPile, 
	        		rightPlacement ? G.Right(drawPile)+CELLSIZE : pileX ,
	        		discardY,
	        		cardW*3,
	        		cardH);
	        
	        
	       
	        G.AlignTop(startingCardRect,
	        		G.Left(drawPile),
	        		drawPile);
	        playerx =  tallMode ? G.Left(boardRect) : G.Right(discardPile)+C2;        
	        break;
	        
        case Safari:
        	G.SetRect(localViewRect,G.Left(boardRect),G.Bottom(boardRect)+C2,
        				G.Height(boardRect),G.Height(boardRect));
        	G.SetRect(playerStateRect,G.Right(localViewRect)+C2,G.Top(localViewRect),
        			G.Width(localViewRect),CELLSIZE*3);
        	G.SetRect(platformViewRect, G.Left(playerStateRect),G.Bottom(playerStateRect),
        			G.Width(playerStateRect),G.Height(localViewRect)-G.Height(playerStateRect));
        	G.SetRect(trainActionRect, G.Right(playerStateRect)+C2,G.Top(playerStateRect),CELLSIZE*14,CELLSIZE*3);
        	playerx = G.Right(drawPile)+C2;
			break;
		default:
			break;
	    }
        G.SetRect(goalRect,G.Left( boardRect),		// really just a general message
        		G.Bottom(boardRect)-2*CELLSIZE,
        		G.Width( boardRect),CELLSIZE*2);
        
        setProgressRect(progressRect,goalRect);
      
        int playery = tallMode 
        			? G.Bottom(bigRackRect)+CELLSIZE 
        			: chatHeight+CELLSIZE;
        int xsize = (width-playerx-C2);
        if(columns) { xsize=xsize/2; }
        int ysize = (int)(xsize/(20/7.0));
        if(!columns && (ysize*nPlayers>G.Height(boardRect)))
        {
        	ysize = G.Height(boardRect)/nPlayers;
        	xsize = ysize*20/7;
        }
        for(int i=0;i<nPlayers;i++)
        {	createPlayerGroup(getPlayerOrTemp(i),playerChipRect[i],
        		playerx+(columns?((i&1)*xsize):0),playery,xsize,ysize);

        	playery += (columns?(i&1)*ysize:ysize);
        }
        if(columns && (nPlayers==3)) { playery += ysize; }
 
        int chatX = wideMode ? playerx : 0;
        int chatY = wideMode ? playery : 0;
        int chatW = wideMode ? width-chatX-C2 : width-ideal_logwidth;
        int chatH = wideMode ? Math.min(chatHeight,G.Bottom(boardRect)-chatY) : chatHeight;
        int logX =  noChat ? C2 : wideMode ?  playerx : chatX+chatW+C2;
        int logY = noChat ? (tallMode ? playery:G.Bottom(bigRackRect))+C2 : (wideMode ?G.Top(boardRect):0)+C2;
        G.SetRect(logRect,logX,
        			logY,
        			ideal_logwidth,
        			noChat ? height-logY-C2 : wideMode ? CELLSIZE*7 : chatHeight-C2);

        G.SetRect(chatRect,chatX,chatY,	chatW,	chatH);
        

        // "edit" rectangle, available in reviewers to switch to puzzle mode
        G.SetRect(doneRect,
        		G.Right(drawPile)+CELLSIZE,
        		G.Top(boardRect)+C2/2,
        		CELLSIZE*8,4*CELLSIZE);

        
        // "done" rectangle, should always be visible, but only active when a move is complete.
        G.AlignXY(editRect,
        		rightPlacement ? G.Right(doneRect)+CELLSIZE : G.Left(doneRect),
        		rightPlacement ? G.Top(doneRect) : G.Bottom( doneRect)+CELLSIZE/2,
        		doneRect);
  
        //this sets up the "vcr cluster" of forward and back controls.
        SetupVcrRects(G.Right(boardRect)-CELLSIZE*11,G.Bottom(boardRect)-7*CELLSIZE,
            CELLSIZE * 10,
            5 * CELLSIZE);
 
        positionTheChat(chatRect,Color.white,Color.white);
        generalRefresh();
    }

	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void drawSingleStack(Graphics gc, OnedayBoard gb,OnedayCell c, boolean showBacks,
    			int forPlayer, Rectangle r,HitPoint highlight,HitPoint any,double xstep,double ystep)
    {	boolean canHit = gb.LegalToHitChips(forPlayer,c);
    	boolean canPick = gb.pickedObject==null;
    	HitPoint pt = canHit? highlight : null;
    	int width = G.Width(r);
    	int height = G.Height(r);
    	double aspect = 1.45;
    	OnedayCell target = c;
    	if(width*aspect>height) { width = (int)(height/aspect); }
    	//G.frameRect(gc, Color.red, r);
    	if(c.height()==0)
    	{
    		target = gb.blankCard;
    	}
    	else if(showBacks)
    	{	target = gb.tempCell;
    		target.rackLocation = c.rackLocation;
    		target.reInit();
    		while(target.height()<c.height()) { target.addChip(c.exposed?Station.back2:Station.back); }
    	}
    	int cx = G.Left(r)+((width>height)?height/2:width/2);
    	int cy = G.Top(r)+height/2;
    	if(c.rackLocation==OneDayId.StartingPile)
    	{	// the other players starting piles are never shown, so give them the
    		// current location
    		for(OnedayCell d : gb.startingPile)
    		{
            	d.setCurrentCenter(cx,cy);	
    		}
    	}
    	else
    	{
        	c.setCurrentCenter( cx, cy);	

    	}
    	String label = "";
    	if(b.isDest(c)||b.isAnySource(c)) { label = Station.HOTFRAME; } 
    	
    	if(target.drawStack(gc,this,pt,width,cx,cy,0,xstep,ystep,label))
    	{	highlight.hitCode = c.rackLocation;
    		highlight.hitObject = c;
    		highlight.arrow = canPick ? StockArt.UpArrow : StockArt.DownArrow;
        	highlight.awidth = G.Width(r)/2;
        	highlight.spriteColor = Color.red;
    	}
    	if(any!=null)
    	{
    		HitPoint.setHelpObject(any,r,c);
    	}
      }
    private void drawPrizeRack(Graphics gc,OnedayBoard gb,int forPlayer,OnedayCell c,
			Rectangle r,
			HitPoint p)
    {
    	drawSingleStack(gc, gb, c, true, forPlayer,r,p,p,1.1,0.0);
    }
    private void drawCardRack(Graphics gc,OnedayBoard gb,OnedayCell rack[],
    							boolean showBack,int forPlayer,Rectangle r,
    							HitPoint p,HitPoint any,boolean showBars,String msg)
    {
    	int w = G.Width(r);
    	int h = G.Height(r);
    	boolean horizontal = w>h;
    	int ncards = rack.length;
    	int xstep = horizontal ? w/ncards : w;
    	int ystep = horizontal ? h : h/ncards;
    	int ix = G.Left(r) + (horizontal ?  (w-ncards*xstep)/2 : 0);
    	int iy = G.Top(r) + (horizontal ? 0 : (h-ncards*ystep)/2);
    	int iix = ix+xstep/2;
    	int iiy = iy+ystep-h/20;
    	for(OnedayCell c : rack)
    	{	
    		drawSingleStack(gc,gb,c,showBack,forPlayer,new Rectangle(ix,iy,xstep,ystep),p,any,-0.005,0);
    		if(horizontal) { ix += xstep; } else  { iy += ystep; }
    	}
    	if(showBars)
    	{
    	int start = 0;
    	int nCells = rack.length;
    	while(start<nCells)
    	{
    		int len = OnedayCell.lengthOfChain(rack,start);
    		if(len>1)
    		{
    		Rectangle bar = new Rectangle(
    				iix+start*(horizontal?xstep:0),
    				iiy+start*(horizontal?0:ystep/2),
    				xstep*(len-1),
    				Math.max(2,h/30+(horizontal?0:ystep)*(len-1)));
        	GC.fillRect(gc,Color.black,bar);
        	}
       		start += Math.max(1,len);
       	 
    	}
    	}
    	if(msg!=null)
    	{
    		GC.Text(gc,false,G.Left(r),G.Top(r),G.Width(r),G.Height(r)/20,Color.black,null,msg);
    	}
    }

    //
    // sprites are normally a game piece that is "in the air" being moved
    // around.  This is called when dragging your own pieces, and also when
    // presenting the motion of your opponent's pieces, and also during replay
    // when a piece is picked up and not yet placed.  While "obj" is nominally
    // a game piece, it is really whatever is associated with b.movingObject()
    //
    public void drawSprite(Graphics g,int objcode,int xp,int yp)
    {  	// draw an object being dragged
    	int moving = getMovingObject(null)%1000;
       	int obj = objcode%1000;
    	boolean ours = OurMove() && (obj==moving);
     	CellId from = OneDayId.find(objcode/1000);
    	Station ch = (!ours && ((from==OneDayId.DrawPile)||(from==OneDayId.StartingPile)||(from==OneDayId.RackLocation))) 
    					? Station.back 			// conceal the card
    					: Station.getCard(obj);	// Tiles have zero offset
    	boolean hot = b.getState()==OnedayState.Discard;
    	if(hot) { xp-=CARDSIZE/6; yp-=CARDSIZE/6; }
    	ch.drawChip(g,this,CARDSIZE,xp,yp,hot?Station.HOTFRAME:null);
     }


    /** this is used by the game controller to supply entertainment strings to the lobby */
    public String gameProgressString()
    {	// this is what the standard method does
    	// return ((reviewer ? s.get(ReviewAction) : ("" + viewMove)));
    	return(super.gameProgressString());
    }



    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	boolean reviewBackground = reviewMode()&&!mutable_game_record;
      // erase
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //G.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      drawFixedBoard(gc,boardRect);
	  b.SetDisplayRectangle(boardRect);

    }
    public void drawFixedBoard(Graphics gc,Rectangle r)
    {
     images[BOARD_INDEX].centerImage(gc, r);
	  Line.drawAllLines(gc,r,0.5);
      Station.drawAllStops(gc,this,r,0.5);
    }
    public void drawPrizeBoard(Graphics gc,OnedayBoard gb,Rectangle r)
    { gb.SetDisplayRectangle(r);
     images[BOARD_INDEX].centerImage(gc, r);
	  Line.drawAllLines(gc,r,0.5);
      Station.drawAllStops(gc,this,r,0.5);
      double scl = (double)G.Width(r)/G.Width(boardRect);
      for(OnedayCell cell = gb.allCells; cell!=null; cell=cell.next)
      { 
    	  int ypos = G.Bottom(r) - gb.cellToY(cell);
    	  int xpos = G.Left(r) + gb.cellToX(cell);

	      if(cell.hasPrize)
	      {	// safari prizes
	    	  Station.back.drawChip(gc, this, (int)(CELLSIZE*scl), xpos,ypos,null);
	      }
	  }

    }
    private void drawTrains(Graphics gc,OnedayBoard gb,Rectangle brect)
    {	TrainStack trains = gb.trains;
    	for(int lim=trains.size()-1; lim>=0; lim--)
    	{
    		Train t = trains.elementAt(lim);
    		Line l = t.line;
    		int xp = G.Left(brect)+t.positionOnMap_x(brect);
    		int yp = G.Top(brect)+t.positionOnMap_y(brect);
    		StockArt dot = l.getLineDot();
    		int scaledW = G.Width(brect)/10;
    		dot.drawChip(gc,this,scaledW,xp,yp,null);

    		for(int forPlayer=0,np=nPlayers(); forPlayer<np; forPlayer++)
    		{
    			PlayerBoard pb = gb.playerBoard[forPlayer];
    			if(pb.getLocation().getTrain()==t)
    			{
    				//drawPlayerAvatar(gc,forPlayer,xp,yp,scaledW/4);
    			}
    		}
    }

    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, OnedayBoard gb, Rectangle brect, HitPoint highlight)
    {
     	//
        // now draw the contents of the board and anything it is pointing at
        //
    	gb.SetDisplayRectangle(brect);
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
        if((gc!=null) && (featuredCard!=null) && featuredCard.isStation())
        {	Station featuredStation = (Station)featuredCard;
        	featuredStation.drawLines(gc,brect,1.0);
        	featuredStation.drawStops(gc,this,brect);
        }
        OnedayCell hitCell = null;
    	for(OnedayCell cell = gb.allCells; cell!=null; cell=cell.next)
        	{ 
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            
           // Station station = cell.topChip();
           // String name = station.station;
           // G.Text(gc,false,xpos+CELLSIZE/2,ypos-CELLSIZE/4,CELLSIZE*4,CELLSIZE/2,Color.white,null,name);
            if( cell.closestPointToCell(highlight,CELLSIZE,xpos,ypos)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            		hitCell = cell;
            	}
        	}
    	if(hitCell!=null)
    	{
        	Station top = (Station)hitCell.topChip();
            	highlight.arrow =(getMovingObject(highlight)>=0) 
      				? StockArt.DownArrow 
      				: top!=null?StockArt.UpArrow:null;
            	highlight.awidth = SQUARESIZE/2;
            	if(top!=null)
            	{ if(gc!=null)
            	  {String activity = s.lineSplit(top.activity,GC.getFontMetrics(gc),300);
            	   highlight.setHelpText(top.station
            		+ "\n"+ s.get(PronouncedDescription,top.pronounciation)
            		+ "\n \n"+ activity);
            	  }
            	}
        	highlight.spriteColor = Color.red;
            	}
    	switch(gb.variation)
    	{
    	default: throw G.Error("not expecting %s",gb.variation);
    	case Safari:

        	drawPrizeBoard(gc,gb,brect);
    		drawTrains(gc,gb,brect);
    		drawPlayerAvatars(gc,gb,brect);
			break;
		case Standard:
    		break;
    	}
     }
    // this is non-standard
    // this allows simultaneous card moves to display properly
    public int getMovingObject(HitPoint highlight)
    {	if (OurMove()||(b.getState()==OnedayState.Place))
    	{
    	// encode the source of the card as well as the card, so the 
    	// client will conceal the card identity if appropriate
        return(b.publicMovingObjectIndex());
    	}
    	return(NothingMoving);
    }
    public void drawArrow(Graphics gc,int cx,int cy,double angle,int w)
    {
   	 Image arrow = StockArt.SolidDownArrow.image;
   	 int iw = arrow.getWidth();
   	 Image im = Image.createTransparentImage(iw,iw);
   	 Graphics ig = im.getGraphics();
   	arrow.drawImage(ig,0,0,iw,iw);
   	 im = im.rotate( angle, 0x0);
   	 im.centerImage(gc, cx-w/2,cy-w/2,w,w);
    }
    public void drawPlatform(Graphics gc,OnedayLocation myLoc,Platform station,Rectangle r,HitPoint p,PlayerBoard bd)
    {	Station next = station.nextStation;
    	boolean left = "L".equals(station.uid);
    	if(next!=null)
    	{
    	int h = G.Height(r);
    	String target = next.getStation().getName();
    	//
    	// draw a dot in the station color, with an arrow 
    	// indicating the direction of travel
    	{Line myLine = station.getLine();
    	 StockArt dot = myLine.getLineDot();
    	 double angle = atan2(next.getX()-station.getX(),next.getY()-station.getY());     	 
    	 double sina = Math.sin(angle);
    	 double cosa = Math.cos(angle);
    	 int cx = left ? G.Left(r)+h/3: G.Right(r)-h/3;
    	 int cy = G.Top(r)+h/3;
    	 int rh = (int)(h*0.3);
    	 int ex = (int)(cx + sina*h*0.25);
    	 int ey = (int)(cy + cosa*h*0.25);
    	 dot.drawChip(gc, this, h*2,cx,cy,null);
    	 int l = (cx+ex)/2;
    	 int top =(cy+ey)/2;
    	 drawArrow(gc,l,top,angle,3*rh/2);
    	}
    	GC.Text(gc,true,G.Left(r)+h/4,G.Top(r)+h/3,G.Width(r)-h/4,h-h/3,Color.black,null,s.get(TowardString,target));
    	HitPoint.setHelpText(p,r,s.get(WalkTo,target));
    	}
    	//
    	// if there's a prize on the platform, draw a card
    	if(station.prize!=null)
		{	int w = G.Width(r);
			int h = G.Height(r);
			int cx = left ? G.Left(r)+w/8 : G.Right(r)-w/8;
			int cy = G.Top(r)+h/2;
			Station.back.drawChip(gc,this,w/4,cx,cy,null);
		}

    	if(G.pointInRect(p, r) && bd.canTransitionToWalking())
    	{
    		p.hitCode = OneDayId.Platform;
    		p.hitObject = station;
    		p.spriteColor = Color.red;
    		p.spriteRect = r;
    	}
    	station.setVisLocation(r);
    }
    public void drawPlatformInfo(Graphics gc,Station station,OnedayLocation loc,HitPoint p,PlayerBoard bd)
    {	
    	Rectangle r = platformViewRect;
    	int ystep = G.Height(r)/5;		// max of 4 stations
    	int w = G.Width(r);
    	int x = G.Left(r);
    	int y = G.Top(r);
    	Line myLine = loc.getLine();
    	GC.fillRect(gc, Color.gray,r);
    	if(station!=null)
    	{
    	Stop stops[] = station.stops;
    	if(stops!=null)
    	{
    		for (Stop stop : stops)
    		{	Line line = stop.getLine();
    			Interchange interchange = station.findInterchange(myLine, line);
    			Color fillColor = line.getDesatColor();
    			Color mainColor = line.getColor();
    			String name = line.name;
    			if(interchange!=null) 
    				{ name += "\n"+s.get(WalkMinutes,""+interchange.timeInMinutes); 
    				}
    			Rectangle stopRect = new Rectangle(x+2,y+2,w-4,ystep-4);
    			Rectangle leftRect = new Rectangle(x+3,y+3,w/3,ystep-6);
    			int rx = w*2/3;
    			Rectangle rightRect = new Rectangle(x+rx,y+3,w-(rx+3),ystep-6);
    			GC.fillRect(gc, fillColor,stopRect);
    			GC.frameRect(gc, mainColor, stopRect);
    			GC.setFont(gc,standardBoldFont());
    			GC.Text(gc,true,stopRect,Color.black,null,name);
    			y += ystep;
    			Platform prevStop = stop.getPrevPlatform();
    			
    			Platform nextStop = stop.getNextPlatform();
    			if(prevStop!=null)
    			{
    				drawPlatform(gc,loc,prevStop,leftRect,p,bd);
    			}
    			if(nextStop!=null)
    			{
    				drawPlatform(gc,loc,nextStop,rightRect,p,bd);
    			}
    			
    		}
    	}
    	}

    	GC.frameRect(gc, Color.black, r);
    	
    }
    public void drawPlayerSummary(Graphics gc,OnedayBoard.PlayerBoard pb,HitPoint hp)
    {	SafariState state = pb.getState();
    	GC.fillRect(gc,Color.white,playerStateRect);
    	GC.frameRect(gc, Color.black, playerStateRect);
    	switch(state)
    	{
    	default: throw G.Error("not expected");
    	case WalkAndEnterTrain:
    	case WaitAndEnterTrain:
    		{
    			OnedayLocation loc = pb.getLocation();
    			Platform plat = loc.getPlatform();
    			Station sta = plat.nextStation;
    			@SuppressWarnings("unused")
				String dest = (sta==null) 
    					? s.get(EndLineMessage)  
    					: s.get(TowardString,sta.getName());
    			GC.Text(gc, true, playerStateRect,Color.black,Color.white,
    					s.get(BoardingMessage,
    							loc.getStation().getName()
    							)
    					);
    		}
    	break;
    	case Waiting:
			{
			OnedayLocation loc = pb.getLocation();
			Platform plat = loc.getPlatform();
			Station sta = plat.nextStation;
			@SuppressWarnings("unused")
			String dest = (sta==null) 
					? s.get(EndLineMessage)  
					: s.get(TowardString,sta.getName());
	    	GC.Text(gc, true,  playerStateRect, Color.black,Color.white,
	    			s.get(WaitingMessage,loc.getStation().getName())
	    			);
			if(GC.handleSquareButton(gc, trainActionRect, hp,s.get(BoardTrain), HighlightColor, rackBackGroundColor))
			{
				hp.hitCode = OneDayId.BoardTrain;
			}
			}
			break;

    	case Walking:
    		{
    		OnedayLocation loc = pb.getLocation();
    		OnedayLocation next = pb.getNextLocation();
    		Platform nextPlat = next.getPlatform();
    		//Station sta = nextPlat.nextStation;
        	GC.Text(gc, true,  playerStateRect, Color.black,Color.white,
        			s.get(AtWalkingMessage,
        					loc.getStation().getName(),
        					nextPlat.getLine().getName()));
    		if(GC.handleSquareButton(gc, trainActionRect, hp,s.get(BoardTrain), HighlightColor, rackBackGroundColor))
    		{
    			hp.hitCode = OneDayId.BoardTrain;
    		}
    		}
    		break;
    	case ExitingTrain:
    	case OnTrain:
    		{
    	   	OnedayLocation loc = pb.getLocation();
           	GC.Text(gc, true,  playerStateRect, Color.black,Color.white,
        			s.get(OnWalkingMessage,
        					loc.getLine().getName(),
        					loc.getStation().getName()));
    		if(GC.handleSquareButton(gc, trainActionRect, hp, s.get(ExitTrain),HighlightColor,rackBackGroundColor))
    		{
    			hp.hitCode = OneDayId.ExitTrain;
    		}
    		}
    		break;
    	}
    }
    public void drawPlayerAvatars(Graphics gc,OnedayBoard gb,Rectangle r)
    {	int w = G.Width(r);
    	int h = G.Height(r);
    	int l = G.Left(r);
    	int t = G.Top(r);
    	int adjW = w/30;
    	for(int i=0,lim=nPlayers();i<lim;i++)
    	{	PlayerBoard pb = gb.playerBoard[i];
			OnedayLocation loc = pb.getLocation();
			double cx = loc.getX();
			double cy = loc.getY();
			int x = l+(int)(w*cx/100);
			int y = t+(int)(h*cy/100);
			drawPlayerAvatar(gc,i,x,y,adjW);
    	}
    }
    public void drawPlayerAvatar(Graphics gc,int forPlayer,int cx,int cy,int w)
    {	commonPlayer pl = getPlayerOrTemp(forPlayer);
    	Image avatar = pl.getPlayerImage();
    	double angle = 2*Math.PI/6*forPlayer;
    	double sina = Math.sin(angle);
    	double cosa = Math.cos(angle);
     avatar.centerImage(gc, (int)(cx-w/2+sina*w/2), (int)(cy-w/2+cosa*w/2),w,w);
    	}
    public void drawSafariInfo(Graphics gc,HitPoint hp,OnedayBoard gb,int forPlayer)
    {	OnedayBoard.PlayerBoard pb = gb.playerBoard[forPlayer];   	
    	drawPlayerSummary(gc,pb,hp);
    	OnedayLocation loc = pb.getLocation();
    	double xpos = loc.getX()/100.0;					// our location x % of the map
    	double ypos = loc.getY()/100.0;					// our location y % of the map
    	
    	double scale = pb.displayScale;					// scale factor for the local rect
    	int width = (int)(G.Width(boardRect)*scale);	// virtual width of the local view
    	int height = (int)(G.Height(boardRect)*scale);	// virtual height of the local view
    	int localX = (int)(width*xpos);					// our location x absolute
    	int localY = (int)(height*ypos);				// our location x absolute
    	
    	// show the local view's location on the big map
    	int rWidth = G.Width(localViewRect);
    	int rHeight = G.Height(localViewRect);
    	int mWidth = (int)(rWidth/scale);
    	int mHeight = (int)(rHeight/scale);
    	Rectangle bdisp = new Rectangle((int)(G.Left(boardRect)+xpos*G.Width(boardRect)-mWidth/2),
    									(int)(G.Top(boardRect)+ypos*G.Height(boardRect)-mHeight/2),
    									mWidth,
    									mHeight);
    	GC.frameRect(gc, Color.yellow, bdisp);
    	
    	// show the local view, offset and clipped to appear in localrect
    	Rectangle playerRect =
    			new Rectangle(G.Left(localViewRect)+rWidth/2-localX,
    						G.Top(localViewRect)+rHeight/2-localY,
    						width,
    						height);
    	Rectangle oldclip = GC.combinedClip(gc,localViewRect);
    	GC.fillRect(gc, Color.gray,localViewRect);
    	

    	drawBoardElements(gc,gb,playerRect,null);
    	GC.setClip(gc,oldclip);
    	
    	GC.frameRect(gc, Color.yellow, localViewRect);
    	// show the current or next platform
    	Station next = loc.getStation();
    	
    	// draw the platforms and general platform state
    	drawPlatformInfo(gc,next,loc,hp,pb);
   
       	{	// frame the center to disambiguate it
        	int w = G.Width(localViewRect);
        	int h = G.Height(localViewRect);
        	GC.frameRect(gc, Color.yellow,
        			(int)(G.Left(localViewRect)+w*0.4),
        			(int)(G.Top(localViewRect)+h*0.4),
        			(int)(w*0.2),
        			(int)(h*0.2));
    	}
 
    	// draw the player avatar on the platform area
    	//if(myLoc==station) { StockArt.SmallO.drawChip(gc, this, r, null); }
    	SafariState state = pb.getState();
    	switch(state)
    	{
    	default: break;
    	case Walking:
    	case WalkAndEnterTrain:
    	{
    		OnedayLocation nextLoc = pb.getNextLocation();
    		Platform npl = nextLoc.getPlatform();
    		Platform pl = loc.getPlatform();
    		int plx = npl.getScreenX();
    		int ply = npl.getScreenY();
    		int frx = pl.getScreenX();
    		int fry = pl.getScreenY();
    		Rectangle r = pl.getVisLocation();
    		int w = G.Width(r);
    		GC.setColor(gc, Color.blue);
    		GC.drawFatLine(gc,frx,fry,plx,ply,Math.max(1,w/10));
    		GC.setColor(gc, Color.black);
    		double angle = atan2(plx-frx,ply-fry);
    		drawArrow(gc,(frx+plx)/2,(fry+ply)/2,angle,w/2);
    		int locx = G.interpolate(pb.position,frx,plx);
    		int locy = G.interpolate(pb.position, fry, ply);
    		drawPlayerAvatar(gc,forPlayer,locx,locy,w/4);
    	}
    		break;
    	case Waiting:
    	case WaitAndEnterTrain:
    		{	Platform pl = loc.getPlatform();
    			Rectangle r = pl.getVisLocation();
    			drawPlayerAvatar(gc,forPlayer,pl.getScreenX(),pl.getScreenY(),G.Width(r)/4); 
    		}
    		break;
    	}

    }

    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  	OnedayBoard gb = disB(gc);
    	OnedayVariation variation = b.variation;
 
      boolean ourTurn = allowed_to_edit?true:OurMove();
      boolean moving = getMovingObject(highlight)>=0;
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      OnedayState vstate = gb.getState();
      redrawGameLog2(gc, ourSelect, logRect, Color.black,boardBackgroundColor,standardBoldFont(),standardPlainFont());
    
        drawBoardElements(gc, gb, boardRect, highlight);
        GC.setFont(gc,standardBoldFont());
		if (vstate != OnedayState.Puzzle)
        {
			handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
			
			handleEditButton(gc,editRect,select,highlight, HighlightColor, rackBackGroundColor);
                }
		drawPlayerStuff(gc,(vstate==OnedayState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);
 		
 		switch(variation)
 		{
 		default: throw G.Error("Not expecting %s",variation);
 		case Standard:
 		if((vstate!=OnedayState.Place)&&(vstate!=OnedayState.SynchronousPlace)) 
	 			{ drawSingleStack(gc,gb,gb.drawPile,true,gb.whoseTurn(),drawPile,ot,null,-0.005,0);
 	 		  drawCardRack(gc,gb,gb.discardPile,false,gb.whoseTurn(),discardPile,ot,highlight,false,null);
 			}
 		if((vstate==OnedayState.Puzzle)||(vstate==OnedayState.Place)||(vstate==OnedayState.SynchronousPlace))
 			{
 			OnedayCell sta = gb.startingPile[allowed_to_edit?gb.whoseTurn:getActivePlayer().boardIndex];
	 			drawSingleStack(gc,gb,sta,true,allowed_to_edit?gb.whoseTurn:getActivePlayer().boardIndex,startingCardRect,ot,null,
	 					-0.005,0);
 			}
			if(!getActivePlayer().spectator || mutable_game_record)
	 		{
			int who = allowed_to_edit?gb.whoseTurn:getActivePlayer().boardIndex;
			String msg = s.get(CardsForDescription,prettyName(who));
			drawCardRack(gc,gb,gb.playerBoard[who].rack,false,who,bigRackRect,ot,highlight,true,msg);
	 		}
			{
			int nPlayers = gb.nPlayers();
	 		for(int i=0;i<nPlayers;i++)
	 		{
	 			drawCardRack(gc,gb,gb.playerBoard[i].rack,
	 					true,	// always show backs in the small rack
	 					i,playerChipRect[i],ot,
	 					(mutable_game_record||((i==getActivePlayer().boardIndex)&&!getActivePlayer().spectator))?highlight:null,
	 					allowed_to_edit,null);
	 		}}
	 		
	 		break;
 		case Safari:
 			{
 			drawSafariInfo(gc,ot,gb,getActivePlayer().boardIndex);
 			
			int nPlayers = gb.nPlayers();
	 		for(int i=0;i<nPlayers;i++)
	 		{	PlayerBoard pb = gb.playerBoard[i];
	 			drawPrizeRack(gc,gb,i,pb.winnings,
	 					playerChipRect[i],ot);

	 		}}

 			break;
 		}

 		
        if (gc != null)
        {	featuredCard = null;
        	if(highlight!=null)
        	{	OnedayCell c = (OnedayCell)highlight.helpObject;
        		if(c!=null)
        		{switch(c.rackLocation())
        		{
        		default: break;
        		case RackLocation:
        		case DiscardPile:
            		featuredCard = (Station)c.topChip(); 
        		}}
        		if(ourTurn && (gb.pickedObject!=null) && (featuredCard==null))
        		{
        			featuredCard = (Station)gb.pickedObject;
        		}
        		
        	}}
            standardGameMessage(gc,
            		vstate==OnedayState.Gameover
            			?simpleGameOverMessage()
            			:s.get(vstate.getDescription()),
            				vstate!=OnedayState.Puzzle && !vstate.simultaneousTurnsAllowed(),
            				gb.whoseTurn,
            				stateRect);
             goalAndProgressMessage(gc,ourSelect,Color.white,
            		 s.get(variation.victoryCondition),progressRect, goalRect);
         
        drawVcrGroup(ourSelect, gc);
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
    {	OnedayState state = b.getState();
        switch(state)
        {	
        case Puzzle:
		case Place:
        case SynchronousPlace:
        	switch(mm.op)
        	{
        	case MOVE_TO_RACK:
        	case EPHEMERAL_TO_RACK:
        	
        		// set the correct player for ephemeral moves
        		if(mm.player<0)
        			{ OnedayMovespec m = (OnedayMovespec)mm;
        			  m.player = (m.to_col-'A');
        			}
        		mm.setLineBreak(true);
        		break;
        	default: break;
        	}
			break;
		default:
			break;}
        if(mm.op==NORMALSTART) { mm.setLineBreak(true); }
         handleExecute(b,mm,replay);
        startBoardAnimations(replay);
        if(replay!=replayMode.Replay) { playSounds(mm); }
 
        return (true);
    }
     
     void startBoardAnimations(replayMode replay)
     {	
        if(replay!=replayMode.Replay)
     	{	double starttime = 0.0;
     		while(b.animationStack.size()>1)
     		{
     		OnedayCell dest = b.animationStack.pop();
     		OnedayCell src = b.animationStack.pop();
    		//
    		// in cases where multiple chips are flying, topChip() may not be the right thing.
    		//
     		starttime += startAnimation(src,dest,dest.exposed?dest.topChip():Station.back,starttime);
     		}
     	}
        	b.animationStack.clear();
     } 
     double startAnimation(OnedayCell from,OnedayCell to,OnedayChip top,double starttime)
     {	if((from!=null) && (to!=null) && (top!=null))
     	{	
      		if(G.debug())
     		{	from.assertCenterSet();
     			to.assertCenterSet();
     		}
     		
     		// make time vary as a function of distance to partially equalize the runtim of
     		// animations for long verses short moves.
      		double speed = masterAnimationSpeed*1.0;
      		double dist = from.distanceTo(to);
     		double full = G.distance(0,0,G.Width(boardRect),G.Height(boardRect));
     		double endtime = starttime+speed*Math.sqrt(dist/full);
     		double rot = to.activeAnimationRotation();
     		SimpleSprite newSprite = new SimpleSprite(true,top,
     				SQUARESIZE,	// use the same cell size as drawSprite would
     				starttime,
     				endtime,
             		from.centerX(),from.centerY(),
             		to.centerX(),to.centerY(),rot);
     		newSprite.movement = Movement.SlowIn;
            to.addActiveAnimation(newSprite);
   			addSprite(newSprite);
   			return(endtime);
   			}
     	return(starttime);
     }
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new OnedayMovespec(st, player));
    }
    


    /**
     * prepare to add nmove to the history list, but also edit the history
     * to remove redundant elements, so that indecisiveness by the user doesn't
     * result in a messy game log.  
     * 
     * For all ordinary cases, this is now handled by the standard implementation
     * in commonCanvas, which uses the board's Digest() method to distinguish new
     * states and reversions to past states.
     * 
     * For reference, the commented out method below does the same thing for "Hex". 
     * You could resort to similar techniques to replace or augment what super.EditHistory
     * does, but your efforts would probably be better spent improving your Digest() method
     * so the commonCanvas method gives the desired result.
     * 
     * Note that it should always be correct to simply return nmove and accept the messy
     * game record.
     * 
     * This may require that move be merged with an existing history move
     * and discarded.  Return null if nothing should be added to the history
     * One should be very cautious about this, only to remove real pairs that
     * result in a null move.  It is vital that the operations performed on
     * the history are identical in effect to the manipulations of the board
     * state performed by "nmove".  This is checked by verifyGameRecord().
     * 
     * in commonEditHistory()
     * 
     */
    public commonMove EditHistory(commonMove nmove)
    {
    	return(EditHistory(nmove,(nmove.op==NORMALSTART) || (nmove.op==MOVE_RUN)));
    }

    public boolean AddToHistory(commonMove originalnewmove)
    {	// make "RUN" moves just disappear if they have no effect
    	commonMove ed = (originalnewmove.op==MOVE_RUN) ? EditHistory(originalnewmove) : originalnewmove;
    	if(ed!=null)
    	{
    		return super.AddToHistory(ed);
    	}
    	return(false);
    }
   /** 
     * this method is called from deep inside PerformAndTransmit, at the point
     * where the move has been executed and the history has been edited.  It's
     * purpose is to verify that the history accurately represents the current
     * state of the game, and that the fundamental game machinery is in a consistent
     * and reproducible state.  Basically, it works by creating a duplicate board
     * resetting it and feeding the duplicate the entire history, and then verifying 
     * that the duplicate is the same as the original board.  It's perfectly ok, during
     * debugging and development, to temporarily change this method into a no-op, but
     * be warned if you do this because it is throwing an error, there are other problems
     * that need to be fixed eventually.
     */
    //public void verifyGameRecord()
    //{	//DISABLE_VERIFY = true;
    //	super.verifyGameRecord();
    //}
    
private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_TO_DISCARD:
    case MOVE_TO_RACK_AND_DISCARD:
    case MOVE_TO_RACK:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
   	break;
     case MOVE_PICK:
    	 playASoundClip(light_drop,100);
    	 break;
    case MOVE_DROP:
    	break;
    default: break;
    }
	
}

 
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if(hp.hitCode instanceof OneDayId)
        {
		OnedayCell cell = hitCell(hp);
        OneDayId hitObject = (OneDayId)hp.hitCode;
		OnedayState state = b.getState();
		Station chip = (cell==null) ? null : (Station)cell.topChip();
		if(chip!=null)
		{
		switch(hitObject)
		{
		case BoardTrain:
		case ExitTrain:
		case Platform:
			break;
        case DrawPile:
        case StartingPile:
        case DiscardPile:
        case RackLocation:
       	 if((state==OnedayState.Place)||(state==OnedayState.SynchronousPlace))
       	{	 // this ought to be unnecessary, but somehow another player's cell is sometimes the value of "cell"
       		 OnedayCell sta = b.startingPile[allowed_to_edit?b.whoseTurn:getActivePlayer().boardIndex];
       		 PerformAndTransmit("Epick "+hitObject.shortName+" "+sta.col+" "+sta.row,false,replayMode.Single);
       	 }
       	 else
       	 {
       		 PerformAndTransmit("Pick "+hitObject.shortName+" "+cell.col+" "+cell.row);
       	 }
       	break;
		default:
			break;
		}

        }}
    }

	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging( HitPoint hp)
    {
    	CellId id = hp.hitCode;
    	if(!(id instanceof OneDayId)) { missedOneClick = performStandardActions(hp,missedOneClick); }
    	else
    	{
    	missedOneClick = false;
        OneDayId hitObject = (OneDayId)hp.hitCode;
		OnedayState state = b.getState();
		OnedayCell cell = hitCell(hp);
		
        switch (hitObject)
        {
         default: throw G.Error("Hit Unknown: %s", hitObject);
         case BoardLocation: break;
         case BoardTrain:
        	 PerformAndTransmit("Board "+b.currentSimulatedTime());
        	 break;
         case ExitTrain:
        	 PerformAndTransmit("Exit "+b.currentSimulatedTime());
        	 break;
         case Platform:
         	{Platform plat = (Platform)hp.hitObject;
         	 Line line = plat.getLine();
         	 Station sta =plat.getStation();
        	 PerformAndTransmit("Walk "+b.currentSimulatedTime()+" "+sta.getUid()+" "+line.getUid()+" "+plat.getUid());
         	}
         	break;
         case DrawPile:
         case StartingPile:
         case DiscardPile:
         case RackLocation:
        	 if((state==OnedayState.Place)||(state==OnedayState.SynchronousPlace))
        	 {
        		 if(b.pickedObject==null)
            	 {	// epicks are not to be transmitted
            		PerformAndTransmit("Epick "+hitObject.shortName+" "+cell.col+" "+cell.row,false,replayMode.Live);
            	 } 
        		 else
        		 {	// drop to undo the pick
             		PerformAndTransmit("Edrop "+OneDayId.StartingPile.shortName+" "+cell.col+" "+cell.row,false,replayMode.Live); 
            		if(hitObject==OneDayId.RackLocation)
            		{ 	// do the move as an atomic.  transmit it.  replayMode.Replay suppresses the local animation
            			PerformAndTransmit("ERack "+cell.col+" "+cell.row,true,replayMode.Replay);
            			// this sets the time for the ephemeral move.  Recording the time was inhibited
            			// by the use of .Replay
            			History.top().setElapsedTime((int)getActivePlayer().elapsedTime);
            		}
        		 }
        	 }
        	 else
        	 {
        	 if(b.pickedObject==null)
        	 {	
        		 PerformAndTransmit("Pick "+hitObject.shortName+" "+cell.col+" "+cell.row);
        	 }
        	 else 
        	 {
        		 PerformAndTransmit("Drop "+hitObject.shortName+" "+cell.col+" "+cell.row);
        	 }}
        	break;
        }
         }
    }
    
    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parseable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() 
    { 
    	return(""+b.gametype+" "+b.randomKey+" "+b.nPlayers()); 
   }
    public String sgfGameType() { return(Oneday_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a onday init spec
    	long rk = G.LongToken(his);
    	int np = G.IntToken(his);
    	// make the random key part of the standard initialization,
        b.doInit(token,rk,np);
        adjustPlayers(np);

    }

    
 //   public void doShowText()
 //   {
 //       if (debug)
 //       {
 //           super.doShowText();
 //       }
 //       else
 //       {
 //           theChat.postMessage(GAMECHANNEL,KEYWORD_CHAT,
 //               s.get(CensoredGameRecordString));
//        }
//    }

  
    long lastGameOverAnimationTime = 0;
    public void gameOverAnimation()
    {
    	if(b.GameOver())
    	{	if(G.Date()>lastGameOverAnimationTime)
    		{
    		TrainSimulator sim = new TrainSimulator(b.playerBoard[b.whoseTurn].rack);
    		sim.runSimulation(this,boardRect);
    		lastGameOverAnimationTime = G.Date()+10000;
    		}
		}
    	else { lastGameOverAnimationTime = 0; }
    }
    
    long lastSimulatedTime = 0;
    long stepQuantum = 10;
    public void runTrains()
    {	long now = G.Date();
    	if(lastSimulatedTime==0) { lastSimulatedTime = now; }
    	long step = (now-lastSimulatedTime);
		long tstep = (long)(stepQuantum*b.simulationRate);
    	if(b.running && (step>stepQuantum))
    	{	if(step<100000)
    		{
    		long timeStep = (b.simulatedTime+tstep);
    		while(step > 100)
    			{ PerformAndTransmit("Run "+timeStep);
    			  step -=stepQuantum;
    			  timeStep += tstep;
    			}
    		repaint();
    		lastSimulatedTime = now-step;
    		}
    	else { lastSimulatedTime = now; }
    	
    	}
    }
/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically
 *  */
    
   public void ViewerRun(int wait)
   {
       super.ViewerRun(wait);
       switch(b.variation)
       {
       default: throw G.Error("not expecting %s",b.variation);
       case Standard:
           runAsyncRobots();
           startSynchronousPlay();
           gameOverAnimation();
           break;
       case Safari:
    	   runTrains();
       }
       
   }
   
   public void startSynchronousPlay()
   {
	   if(!reviewMode() 
			   && allRobotsIdle()
			   && (b.getState()==OnedayState.NormalStart))
	   {
		   canonicalizeHistory();
		   PerformAndTransmit("NormalStart",false,replayMode.Live);
	   }
   }
   public void startRobotTurn(commonPlayer pp)
   {  OnedayState state = b.getState();
	   if((state==OnedayState.SynchronousPlace)||(state==OnedayState.Place))
	   {
		   if((pp!=null)&&!b.rackFull(pp.boardIndex)) { super.startRobotTurn(pp); }
	   }
	   else { super.startRobotTurn(pp); }
   }

    //
    // start robots if this is an asynchronous phase
    // if it's a normal, synchronous phase, the main game
    // controller will do it.
    //
    public void runAsyncRobots()
    {	
       	switch(b.getState())
    	{
    	case Place:
    		{
    		for(int i=0,lim=nPlayers(); i<lim; i++) {  startRobotTurn(getPlayerOrTemp(i)); }
    		}
			break;
		default: break;
    	}
    }
    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new OnedayPlay()); }

    /**
     * return a score for the player in a multiplayer game. 
     */
    public int ScoreForPlayer(commonPlayer p)
    {	return(b.scoreForPlayer(p.boardIndex));
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
            {	StringTokenizer st = new StringTokenizer(value);
            	String typ = st.nextToken();
            	long ran = G.LongToken(st);
            	int np = G.IntToken(st);
                b.doInit(typ,ran,np);
                adjustPlayers(b.nPlayers());
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (parseVersionCommand(name,value,2)) {}
            else if (parsePlayerCommand(name,value)) {}
            else 
            {
                if(replayStandardProps(name,value))
                {
                	if(date_property.equals(name)) { b.setCompatibility(value); }
                }
            }

            prop = prop.next;
        }

        if (!"".equals(comments))
        {
            setComment(comments);
        }
    }
}

