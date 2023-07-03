package sprint;

import static sprint.Sprintmovespec.*;



import online.common.*;
import java.util.*;

import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.FileDialog;
import bridge.FontMetrics;
import bridge.JMenuItem;
import dictionary.Dictionary;
import dictionary.Entry;
import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.TextButton;
import lib.Random;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.game.sgf.sgf_reader;
import online.search.SimpleRobotProtocol;



/**
 *  Initial work May 2023
*/
public class SprintViewer extends CCanvas<SprintCell,SprintBoard> implements SprintConstants, GameLayoutClient
{	static final long serialVersionUID = 1000;
	static final String Sprint_SGF = "sprint"; // sgf game name

	// file names for jpeg images and masks
	static final String ImageDir = "/sprint/images/";
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    //private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(235,235,235);
    private Color rackBackGroundColor = new Color(192,192,192);
    private Color boardBackgroundColor = new Color(220,165,155);
    @SuppressWarnings("unused")
	private Color newLetterColor = new Color(0.25f,0.25f,1.0f);
    private Color tempLetterColor = new Color(0.1f,0.5f,0.1f);
    private Dictionary dictionary = Dictionary.getInstance();
    private int rackSize = 2;
    private int plannedRackSize = 4;
    public String deskBellSoundName = SOUNDPATH + "rdkbell" + SoundFormat;
    
    // private state
    private SprintBoard bb = null; //the board from which we are displaying
    
    //
    // addZoneRect also sets the rectangle as specifically known to the 
    // mouse tracker.  The zones are considered in the order that they are
    // added, so the smaller ones should be first, then any catchall.
    //
    // zones ought to be mostly irrelevant if there is only one board layout.
    //
    private Rectangle chipRects[] = addZoneRect("chips",MAX_PLAYERS);
    private Rectangle scoreRects[] = addZoneRect("playerScore",MAX_PLAYERS);
    private Rectangle eyeRects[] = addZoneRect("PlayerEye",MAX_PLAYERS);
    private Rectangle noticeRects[] = addRect("notice",MAX_PLAYERS);
    private Rectangle drawPileRect = addRect("drawPileRect");
    private Rectangle pullTimer = addRect("pull");
    private Rectangle bigRack = addZoneRect("bigrack");
    private Rectangle noticeRect = addRect("notice");
    private Rectangle upperBoardRect = addRect("upperboard");
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);

	private TextButton pullButton = addButton(PullAction,SprintId.PullAction,ExplainPull,
			HighlightColor, rackBackGroundColor,boardBackgroundColor);
	private TextButton endgameButton = addButton(EndGameAction,SprintId.EndGame,EndGameDescription,
			HighlightColor, rackBackGroundColor,boardBackgroundColor);

  
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	SprintChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = SprintChip.Icon.image;
    }
    public int ScoreForPlayer(commonPlayer p)
    {
    	return(bb.getPlayerBoard(p.boardIndex).highScore());
    }
  
    public boolean WinForPlayer(commonPlayer p)
    {	return bb.winForPlayer(p.boardIndex);
    }
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	int players_in_game = info.getInt(OnlineConstants.PLAYERS_IN_GAME,chipRects.length);
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        
        super.init(info,frame);
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	SprintConstants.putStrings();
        }
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),SprintId.ZoomSlider);
        zoomRect.min=0.5;
        zoomRect.max=2;
        zoomRect.value=INITIAL_TILE_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = HighlightColor;   

        String type = info.getString(GAMETYPE, SprintVariation.Sprint.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new SprintBoard(type,players_in_game,randomKey,getStartingColorMap(),dictionary,SprintBoard.REVISION);
        // some problems with the animation
        // useDirectDrawing();
        doInit(false);
        adjustPlayers(players_in_game);
        
        if(G.debug()) { saveScreen = myFrame.addAction("Save Board Image",deferredEvents); }

    }
    JMenuItem saveScreen;
    double INITIAL_TILE_SCALE = 1.0;
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
        	if(!preserve_history) 
   	 		{ zoomRect.setValue(INITIAL_TILE_SCALE);
   	 		  board_center_x =  board_center_y = 0; 
   	 		}
        	// the color determines the first player
        	startFirstPlayer();
        	//
        	// alternative where the first player is just chosen
        	//startFirstPlayer();
 	 		//PerformAndTransmit(reviewOnly?"Edit":"Start P"+first, false,replayMode.Replay);

    	}
    }
    /** this is called by the game controller when all players have connected
     * and the first player is about to be allowed to make his first move. This
     * may be a new game, or a game being restored, or a player rejoining a game.
     * You can override or encapsulate this method.
     */


    public double aspects[] = {0.7,1.0,1.4};
    public void setLocalBounds(int x,int y,int w,int h)
    {	rackSize = plannedSeating()?5:2;
    	do {
    		setLocalBoundsV(x,y,w,h,aspects);
    		int boardw = G.Width(boardRect);
    		int dim = Math.min(w, h);
    		if(boardw>dim*0.75) { break; }
    		rackSize--;
    	} while(rackSize>=3);
    }
	/**
	 * this is the main method to do layout of the board and other widgets.  I don't
	 * use swing or any other standard widget kit, or any of the standard layout managers.
	 * they just don't have the flexibility to produce the results I want.  Your mileage
	 * may vary, and of course you're free to use whatever layout and drawing methods you
	 * want to.  However, I do strongly encourage making a UI that is resizable within
	 * reasonable limits, and which has the main "board" object at the left.
	 * <p>
	 *  The basic layout technique used here is to start with a cell which is about the size
	 *  of a board square, and lay out all the other objects relative to the board or to one
	 *  another.  The rectangles don't all have to be on grid points, and don't have to
	 *  be non-overlapping, just so long as the result generally looks good.
	 *  <p>
	 *  When "extraactions" is available, a menu option "show rectangles" works
	 *  with the "addRect" mechanism to help visualize the layout.
	 */ 

    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
       	boolean planned = plannedSeating();
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	//int minLogW = fh*22;	
       	int minChatW = fh*35;	
       	int vcrw = fh*12;
        int margin = fh/2;
        int buttonW = (G.isCodename1()?8:6)*fh;
        int stateH = fh*5/2;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			planned ? 0 : 0.75,	// 60% of space allocated to the board
    			aspect,	// aspect ratio for the board
    			fh*2,	// min cell size
    			fh*2,	// maximum cell size
    			planned ? 0 : 0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChat(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2);
    	//layout.placeRectangle(logRect,minLogW, minLogW, minLogW*3/2, minLogW*3/2,BoxAlignment.Edge,true);
       	layout.alwaysPlaceDone = false;
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,endgameButton,editRect,noticeRect);
       	int doneW = G.Width(editRect);
       	layout.alwaysPlaceDone = true;
       	layout.placeDoneEditRep(doneW,doneW,pullButton,null);
      	 
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);
       	
       //	commonPlayer pl = getPlayerOrTemp(0);
       //	int spare = G.Height(pl.playerBox)/3;
       // layout.placeRectangle(drawPileRect,spare,spare,BoxAlignment.Center);
       	       	
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main)-stateH*2;
    	int mainH = G.Height(main)-stateH*2;
    	
    	// There are two classes of boards that should be rotated. For boards with a strong
    	// "my side" orientation, such as chess, use seatingFaceToFaceRotated() as
    	// the test.  For boards that are noticably rectangular, such as Push Fight,
    	// use mainW<mainH
     	int nrows =  SprintGridSpan;
        int ncols =  nrows;
  	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH)/nrows);
    	int CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = mainW;
    	int boardH = mainH-stateH;
    	int boardX = mainX;
    	int boardY = mainY+stateH;
       	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH;
        int stateX = boardX;
    	int stripeW = CELLSIZE*2*(rackSize+3);
    	int zoomW = stateH*4;
    	G.placeRow(stateX,stateY,boardW ,stateH,stateRect,annotationMenu,noChatRect);
    	G.placeRight(stateRect, zoomRect, zoomW);
    	if(plannedSeating())
    	{ G.SetRect(boardRect,0,0,0,0);
    	  G.SetRect(upperBoardRect,0,0,0,0);
    	  G.SetRect(bigRack,0,0,0,0);
    	  G.SetRect(goalRect,0,0,0,0);
    	}
    	else 
    	{	int sp = (boardW-stripeW)/2;
    		int c2 = CELLSIZE*2;
  
        	G.SetRect(boardRect,boardX,boardY,boardW,boardH-c2);
           	G.SetRect(upperBoardRect,boardX,boardY,boardW,c2);
        	G.SetRect(goalRect, boardX, G.Bottom(boardRect),boardW,stateH);   
        	int goalB = G.Bottom(goalRect);
        	G.SetRect(bigRack, boardX+sp, goalB, stripeW, planned?0:c2);
        	G.SetRect(drawPileRect,boardX,goalB, c2,c2);
        	G.SetRect(pullTimer,boardX+boardW-c2,goalB,c2,c2);
    	}

  

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        labelFont = largeBoldFont();
        return(boardW*boardH);
    }
    
    public void updatePlayerTime(long inc,commonPlayer p)
    {	if(simultaneous_turns_allowed())
    	{	// update all players so the timestamps will reflect reality
    		if(!reviewMode()&&!reviewOnly)
    	    {for(commonPlayer pl : players)
    		{
    			if(pl!=null)		// this player has not finished
    			{
    				super.updatePlayerTime(inc,pl); 	// keep on ticking
    			}
    		}}
    	}
    	else if(p!=null)
    	{ super.updatePlayerTime(inc,p); 
    	}
    }
     
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	boolean planned = plannedSeating();
    	
    	Rectangle chip = chipRects[player];
    	Rectangle score = scoreRects[player];
    	Rectangle eye = eyeRects[player];
    	int scoreW = unitsize*3;
    	int scoreH = unitsize*2;
    	G.SetRect(score,x,y,scoreW,scoreH);
    	G.SetRect(eye, x, y+scoreH, unitsize*2, unitsize*2);
    	Rectangle box =  pl.createRectangularPictureGroup(x+scoreW,y,unitsize);
    	Rectangle done = doneRects[player];
    	Rectangle notice = noticeRects[player];
    	
    	int doneW = planned ? unitsize*4 : 0;
    	int donel = G.Right(box)+unitsize/2;
    	G.SetRect(done,donel,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.SetRect(notice, donel , G.Bottom(done),doneW*2,doneW/4);
    	G.union(box, done,score,eye,notice);
    	int unitw = (planned ? (plannedRackSize*2) : rackSize)*unitsize;
    	int unith = (planned ? plannedRackSize : rackSize)*unitsize;
       	G.SetRect(chip,	x,	G.Bottom(box),	unitw*3, unith*3);
        G.union(box, chip);
    	pl.displayRotation = rotation;
    	return(box);
    }
    private void drawPullTimer(Graphics gc,HitPoint highliht,SprintBoard gb)
    {
    	Rectangle r = pullTimer;
    	int w = G.Width(r);
    	int h = G.Height(r);
    	int l = G.Left(r);
    	int t = G.Top(r);
    	SprintState state = currentPlayerBoard(gb).getState();
    	GC.fillRect(gc,Color.lightGray,r);
    	if(state!=SprintState.Gameover)
    	{
    	GC.setFont(gc,standardBoldFont());
    	String mess = (state==SprintState.Endgame)||(gb.tilesLeft()==0) ? EndGameMessage : NextDrawMessage;
    	GC.Text(gc,true,l,t,w,w/4,Color.black,null,s.get(mess));
    	
    	long now = G.Date();
    	long lim = gb.drawTimer;
    	long rem = lim - now;
    	String tim = lim<0 ? "" : G.briefTimeString(Math.abs(rem));
    	if(rem<0) { tim = "-"+tim; }
    	GC.Text(gc,true,l,t+w/4,w,h-w/4,Color.blue,null,tim);
    	}
    	GC.frameRect(gc,Color.black,r);
    }
    private void drawDrawPile(Graphics gc,HitPoint highlight,SprintBoard gb)
    {	Rectangle r = drawPileRect;
    	int boxw = G.Width(r);
    	int cs = Math.min(gb.cellSize(),boxw/4);
    	int w = boxw-cs;
    	int h = G.Height(r)-cs-cs/2;
    	int l = G.Left(r)+cs/2;
    	int t = G.Top(r)+cs/2;
    	boolean canHit = false;
		gb.drawPile.setLastSize(cs);
    	GC.frameRect(gc,Color.black,r);
      	if(canHit && highlight!=null)
      		{
      		highlight.hitObject = gb.drawPile;
      		highlight.hitCode=SprintId.DrawPile;
      		highlight.spriteColor = Color.red;
      		highlight.spriteRect = r;
      		}
    	if(w>0 && h>0)
    	{
    	Random rand = new Random(2324);
    	int tilesLeft = gb.drawPile.height();
    	for(int i=0;i<tilesLeft;i++)
    	{
    		int dx = l+rand.nextInt(w);
    		int dy = t+rand.nextInt(h);
    		boolean hide = true;
    		if(gb.drawPile.chipAtIndex(i).drawChip(gc, this, canHit?highlight:null, SprintId.DrawPile,cs,dx,dy,
    				hide ? SprintChip.BACK : null))
    		{
    			highlight.hitObject = gb.drawPile;
    		}
    		gb.drawPile.rotateCurrentCenter(gc,dx,dy);
    	}
    	GC.setFont(gc, largeBoldFont());
    	GC.Text(gc, true, l,G.Bottom(r)-cs/2,w,cs/2,Color.black,null,s.get(TilesLeft,tilesLeft));
    	}
     }
	// draw a box of spare chips. For pushfight it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r,Rectangle er, commonPlayer pl, HitPoint highlight,HitPoint highlightAll,SingleBoard pboard)
    {	
    	if(gc!=null)
    	{
    		setDisplayParameters(pboard,r,true);
    		drawBoardElements(gc,pboard,r,null,null);
    	}
    }
    
     /**
     * major pain taken to allow racks to be rearranged any time.  There is an intermediate
     * map, used only by the user interface, which rearranges the location of the actual
     * cells containing the player's letters.  When the user picks up one of their letters,
     * nothing changes in the actual rack, and nothing is transmitted to the opponents.
     * When the used drops the letter back onto the rack, the map is reordered but again,
     * nothing changes from the public perspective.  If a tile is dropped on the board,
     * which can only happen when it is the player's turn, an actual "move tile" is sent.
     * 
     * Additional complications take care of displaying the sprite with the correct
     * orientation, both during this local rearrangement and global play on the board,
     * which is being rotated to match the player's seat.
     * @param gc
     * @param gb
     * @param rack
     * @param rackmap
     * @param cells
     * @param map
     * @param picked
     * @param censor
     * @param highlight
     * @param rackOnly
     * @param pl
     */
    private void drawRack(Graphics gc,SingleBoard gb, Rectangle rack,
    		SprintCell[]cells,
    		SprintCell[]mappedCells,
    		int map[],int picked,boolean censor,HitPoint highlight,boolean rackOnly)
    {	int forPlayer = gb.boardIndex;
    	int h = G.Height(rack);
    	int w = G.Width(rack);
    	int cy = G.centerY(rack)-h/10;
    	int nsteps = map.length;
    	int xstep = Math.min(w/(nsteps+1),h*3/4); 
    	int tileSize = (int)(xstep*1);
    	int cx = G.Left(rack)+(w-xstep*nsteps)/2+xstep/2;
    	boolean full = gb.rackIsFull();
    	GC.fillRect(gc,Color.lightGray,rack);
       	GC.frameRect(gc, Color.black, rack);


    	for(int idx = 0;idx<nsteps;idx++)
		{
    	int mapValue = map[idx];
    	SprintCell c = mapValue>=0 ? cells[mapValue] : null;
    	SprintCell mc = mappedCells[idx];
    	SprintChip top = c==null ? null : c.topChip();
    	mc.reInit();
    	if(top!=null) { mc.addChip(top); }
       	boolean legalPick = gb.LegalToHitChips(mc);
    	if(picked==idx) { top = null; }
     	boolean localDrop = !full && (picked>=0 || gb.pickedObject!=null);
     	boolean moving = gb.pickedObject!=null;
    	boolean ourDrop = !rackOnly && moving;	// we can drop from the board, and something is moving
    	char myCol = cells[0].col;
    	boolean canPick = legalPick && 
    				(!localDrop
    						&& (top!=null)
    						&& !ourDrop);

    	{
       	if(mc.activeAnimationHeight()>0) { top = null; }
      	setLetterColor(gc,gb,mc);
      	//print("Draw "+idx+" "+c+" "+top+" @ "+cx);
    	mc.drawChip(gc, this, top, tileSize, cx, cy, censor ? SprintChip.BACK : null);
    	if(c!=null) { c.copyCurrentCenter(mc);	}// set the center so animations look right
       	}

    	if(localDrop && top==null)
    	{
    		StockArt.SmallO.drawChip(gc,this,tileSize,cx,cy,null);
    		
    	}  
     	if((canPick||localDrop) 
     			&& G.pointInRect(highlight, cx-tileSize/2,cy-tileSize/2,tileSize,tileSize))
    	{	boolean hit = false;
    		if(localDrop )
    		{	if(moving)
    			{
    				if(top==null)
    				{
    	    			highlight.hitObject = G.concat( "drop " ,forPlayer," Rack ",myCol," ",idx);
    	    			hit = true;
   					
    				}
    			}
    			else {
    			highlight.hitObject = G.concat("replace ",forPlayer," Rack ",myCol," ",idx);
				hit = true;
    			}
    			
    		}
    		else
    		{   highlight.hitObject = G.concat("lift ",forPlayer," Rack ",myCol," ",idx," ",map[idx]);
				hit = true;
    		}
    		if(hit)
    		{
  			highlight.hit_x = cx;
    		highlight.hit_y = cy;
			highlight.hitCode = SprintId.LocalRack;
			highlight.spriteColor = Color.red;
			highlight.awidth = tileSize;
    		}
 
			
    	}
		cx += xstep;
		}

    }


    private void DrawScore(Graphics gc, Rectangle r, commonPlayer pl, HitPoint highlight,SingleBoard pboard)
    {	
    	int val = pboard.score();
    	int high = pboard.highScore();
    	GC.frameRect(gc,Color.black,r);
    	String msg = ""+val;
    	if(high>val) { msg += "\nHigh: "+high; }
    	GC.Text(gc, true,r,Color.black,null,msg);
    }
    /**
     * return the dynamically adjusted size during an animation.  This allows
     * compensation for things like the zoom level of the board changing after
     * the animation is started.
     */
    //public int activeAnimationSize(Drawable chip,int thissize) { 	 return(thissize); 	} 
    
    private SprintCell getMovingTile(int ap)
    {	SingleBoard pboard = currentPlayerBoard(bb);
    	int picked = pboard.getMapTarget();
    	if(picked>=0) { 
    		SprintCell[] prack = pboard.getPlayerRack();
    		if(prack==null) { return null;}
    		if(picked>=0 && picked<prack.length)
    		{
    			return prack[picked];
    		}
    	}
    	return(null);
    }
    private SprintCell getPickedRackCell(HitPoint highlight)
    {
       	int rm = remoteWindowIndex(highlight);
    	if(rm>=0)
    	{	SprintCell c = getMovingTile(rm);
    		if(c!=null) { return(c); }
    	}
    	else {
    	{int ap = getViewPlayer();
    	 SprintCell c = getMovingTile(ap);
    	 if(c!=null) { return(c); }
    	}
 
     	if(allowed_to_edit || G.offline())
    	{	commonPlayer pl = inPlayerBox(highlight);
    		if(pl!=null)
    			{SprintCell c = getMovingTile(pl.boardIndex);
    			return(c);
    			}
    	}}
    	return(null);
    }
    // return the player whose chip rect this HitPoint is in
    // considering the rotation of the player block
    
    public commonPlayer inPlayerBox(HitPoint hp)
    {	
    	for(int lim = nPlayers()-1; lim>=0; lim--)
    	{ 
    	commonPlayer pl =getPlayerOrTemp(lim);
    	if((pl!=null) && pl.inPlayerBox(hp)) { return(pl); }
    	}
    	return(null);
    }
    public int getOurMovingObject(HitPoint highlight)
    {	SprintCell picked = getPickedRackCell(highlight);
    	if(picked!=null)
    	{
    		SprintChip top = picked.topChip();
    		if(top!=null) { return(top.chipNumber()); }
    	}
        if (OurMove())
        {	
            return (currentPlayerBoard(bb).movingObjectIndex());
        }
        return (NothingMoving);
    }

    public int getMovingObject(HitPoint highlight)
    {	// censor the identity of the moving object for other players
        if (OurMove())
        {	int mo = currentPlayerBoard(bb).movingObjectIndex();
            return(mo);
        }
        return (NothingMoving);
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
       	// hidden windows have x coordinates that are negative, we don't want to rotate tiles
       	// being displayed on hidden windows
       	drawPrerotatedSprite(g,obj,xp,yp);
    }
    public void drawMice(Graphics gc)
    {
    	if(reviewOnly || mutable_game_record)
    	{	// only draw opposing mice if we're in a review situation
    		super.drawMice(gc);	
    	}
    }
    	
    public void drawPrerotatedSprite(Graphics g,int obj,int xp,int yp)
    { 
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
       	SprintChip ch = SprintChip.getChip(obj);
       	if(ch!=null)
       		{
       		// hidden windows have x coordinates that are negative, we don't want to rotate tiles
       		// being displayed on hidden windows
       		GC.setColor(g,Color.black);
       		SingleBoard gb = currentPlayerBoard(bb);
       		ch.drawChip(g,this,gb.cellSize(), xp, yp, null);  
       		}
   }
    /**
     * if the sprite is inside one of the player racks, display it with that racks's 
     * orientation.
     */
    public void drawRotatedSprite(Graphics gc,int chip,HitPoint hp,Point pt)
    {
    	commonPlayer pl = inPlayerBox(hp);
    	if(pl!=null)
    	{	int left = G.Left(pt);
   			int top = G.Top(pt);
  			GC.setRotation(gc,pl.displayRotation,left,top);
       		drawPrerotatedSprite(gc,chip,left,top);
       		GC.setRotation(gc,-pl.displayRotation,left,top);
    	}
    	else { super.drawRotatedSprite(gc, chip,  hp, pt); }
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
    { 
     SprintChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(!plannedSeating())
      {
      drawFixedBoard(gc,boardRect);
      }
     }
    public void drawFixedElements(Graphics gc,boolean complete)
    {	
    	complete |= pendingFullRefresh;
    	super.drawFixedElements(gc,complete);
    }

    // land here after rotating the board drawing context if appropriate
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	
        boolean reviewBackground = reviewMode()&&!mutable_game_record;
        if(reviewBackground)
        {	 
         SprintChip.backgroundReviewTile.image.tileImage(gc,brect);   
        }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	//setDisplayParameters(bb,brect);
        setDisplayParameters(currentPlayerBoard(bb),brect,false);
	      // if the board is one large graphic, for which the visual target points
	      // are carefully matched with the abstract grid
	      //G.centerImage(gc,images[BOARD_INDEX], brect,this);

    }

    static long FIXEDRANDOM = 346572435;
    long fixedTileDigest = 0;
    private boolean pendingFullRefresh = false;
    public void drawFixedTiles(Graphics gc,Rectangle brect,SprintBoard gb)
    {	Random r = new Random(FIXEDRANDOM);
    	long v = 0;
    	pendingFullRefresh = !spritesIdle();

    	int tilesLeft = gb.tilesLeft();
    	v ^= (tilesLeft*r.nextLong());
        fixedTileDigest=v;
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
    public String encodeScreenZone(int x, int y,Point p)
    {
    	if (bigRack.contains(x, y)&&!mutable_game_record)
    	{	G.SetLeft(p, -1);
    		G.SetTop(p,-1);
    		return("off");
    	}
    	return(super.encodeScreenZone(x,y,p));
    }
    
public void setLetterColor(Graphics gc,SingleBoard gb,SprintCell cell)
{	if(gc!=null)
	{
    SprintChip ch = cell.topChip();
    if(ch!=null)
	    {
	    Color col = (cell.rackLocation==SprintId.Unplaced) || cell.nonWord 
	    				? Color.black
	    				: tempLetterColor
	    					;
	 	labelColor = col;
		GC.setColor(gc,col);
	    }
    }
}
	SprintCell definitionCell = null;
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, SingleBoard gb, Rectangle brect, HitPoint highlight,HitPoint all)
    {	 
 		boolean censor = all==null;
    	boolean draggingBoard = draggingBoard(); 
    	boolean canHit = !draggingBoard && G.pointInRect(highlight,brect);
    	int cs = Math.max(5,(int)gb.cellSize());
    	Rectangle oldClip = GC.combinedClip(gc,brect);
    	//GC.fillRect(gc,new Color(0.8f,0.8f,0.85f),brect);
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  

        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        SprintCell closestCell = gb.closestCell(all,brect);
        boolean moving = getOurMovingObject(highlight)>=0;
        boolean hitCell = canHit && gb.LegalToHitBoard(closestCell,moving);
        if(hitCell)
        { // note what we hit, row, col, and cell
          boolean empty = closestCell.isEmpty();
          boolean picked = (gb.pickedObject!=null);
          if(picked && closestCell.isEdgeCell())
          {
        	  gb.createExitCells(closestCell);
          }
          highlight.hitCode = (empty||picked) ? SprintId.EmptyBoard : SprintId.BoardLocation;
          highlight.hitObject = closestCell;
          highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
          highlight.awidth = cs;
        }
        definitionCell = null;
        if(closestCell!=null && all!=null && !all.isUp)
        {	for(int lim=gb.words.size()-1; lim>=0; lim--)
        	{
        	Word word = gb.words.elementAt(lim);
        	if(word.seed==closestCell)
        	{	all.hitCode = SprintId.Definition;
        		all.hitObject = closestCell;
        		all.setHelpText(s.get(GetDefinitionMessage,word.name));
        		definitionCell = closestCell;
        	}
        	}
        }
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        {
        Enumeration<SprintCell>cells = gb.getIterator(Itype.RLTB);
        
		while(cells.hasMoreElements())
          { SprintCell cell = cells.nextElement();
          	G.Assert(cell.parent==gb,"not mine");
            boolean drawhighlight = (hitCell && (cell==closestCell));
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            
            if (drawhighlight)
             { // checking for pointable position
            	 StockArt.SmallO.drawChip(gc,this,gb.cellSize()*5,xpos,ypos,null);                
             }
 
            setLetterColor(gc,gb,cell);
            cell.drawStack(gc,this,null,cs,xpos,ypos,1,1,censor ? SprintChip.BACK : null);
	        
	        //if(!censor && G.debug() && cell.topChip()==null)
	        //{	// draw a grid of other cells
	        //	GC.setFont(gc,standardPlainFont());
	        //	GC.Text(gc,true,xpos-cs/2,ypos-cs/2,cs,cs,Color.black,null,""+G.printCol(cell.col)+cell.row);
	        //}
          }
        }
        if(definitionCell!=null)
        {
        	drawDefinition(gc,gb,all,brect);
        }
        doBoardDrag(brect,all,cs,SprintId.InvisibleDragBoard);
       // GC.Text(gc,false,G.Left(brect),G.Top(brect),300,100,Color.blue,null,""+gb);
        GC.setClip(gc,oldClip);
		GC.frameRect(gc,Color.black,brect);

     }
    
    //
    // unplaced tiles are in a 1D row, which we want to display across the top of the 
    // board in as few rows as possible while maintaining the spacial relationships.
    // this version makes tiles appear to roll up to fill empty slots, so eventually
    // the second and third rows vanish.
    //
    public void drawUnplacedTiles(Graphics gc, SingleBoard gb, Rectangle brect, HitPoint highlight,HitPoint all)
    {	int forPlayer = gb.boardIndex;
    	int left = G.Left(brect);
    	int top = G.Top(brect);
    	int width = G.Width(brect);
    	SprintCell cells[] = gb.unplacedTiles;
    	int ncells = cells.length;
    	int cols = SprintGridSpan;
    	int cs = width/cols;
    	int spareX = (width-cols*cs)/2;
    	int rows = 0;
    	SprintCell source = gb.getSource();
    	boolean running = gb.getState()!=SprintState.Gameover;
    	// first pass, count the number of rows we need
    	for(int i=0;i<cols;i++)
    	{	int idx = i;
    		int row = 0;
    		int blankRow = -1;
    		while(idx<ncells)
    		{	SprintCell c = cells[idx];
    			
    			if(c.topChip()!=null) 
    				{ c.displayRow = row;
    				  row++; 
    				  
    				}
    			else if(c==source) 
    				{ 
    				  c.displayRow = row;
    				  row++; 
    				}
    			else { c.displayRow = blankRow; blankRow--; }
    			idx += cols;
    		}
    		rows = Math.max(row,rows);
    	}
    	if(gb.unplacedCount+4>rows*cols) { rows++; }	// make sure there are some blanks
    	
       	GC.fillRect(gc,Color.lightGray,left,top,width,cs*rows);
    	GC.frameRect(gc,Color.black,left,top,width,cs*rows);
    	
    	int xpos = left+spareX+cs/2;
    	int ypos = top + cs/2;
        
    	if(G.pointInRect(highlight,left,top,width,cs*rows))
    	{
    		highlight.neutralize();
    	}
    	boolean moving = (gb.pickedObject!=null) || (getOurMovingObject(highlight)>=0);
    	// second pass, actually draw the number of rows we need
    	for(int i=0;i<cols;i++)
    	{	int idx = i;
			int xp = xpos+i*cs;
    		while(idx<ncells)
    		{	SprintCell c = cells[idx];
    			setLetterColor(gc,gb,c);
    			int displayRow = c.displayRow;
    			if(displayRow<0) { displayRow = rows+displayRow; }
    			if(displayRow<rows)
    			{
    			int yp = ypos+displayRow*cs;
    			SprintChip topchip = c.topChip();
    			boolean canHit = running && ( moving == (topchip==null));
    			if(c.drawStack(gc,this,canHit ? highlight:null,cs,xp,yp,1,1, null))
    				{	// checking for pointable position
    					highlight.spriteRect = new Rectangle(xp-cs/2,yp-cs/2,cs,cs);
    					highlight.spriteColor = Color.red;
    		   		    highlight.hitObject = G.concat((moving ? "drop ":"pick "),forPlayer," Unplaced ",c.col," ",c.row);	
    				}
    			}
					   				     			
    			idx += cols;
    		}
    	}
    }
    
    public void drawDefinition(Graphics gc,SingleBoard gb,HitPoint hp,Rectangle brect)
    {	
    	SprintCell target = definitionCell;
    	StringBuilder message = new StringBuilder();
    	WordStack words = gb.words;
    	FontMetrics fm = G.getFontMetrics(standardPlainFont());
    	int targetWidth = G.Width(brect)/2;
    	if(target!=null && words!=null && hp!=null)
    	{	for(int lim=words.size()-1; lim>=0; lim--)
    		{
    		Word word = words.elementAt(lim);
    		if(word.seed==target)
    			{
    			Entry e = dictionary.get(word.name);
    			if(e!=null)
    				{
    				message.append(word.name);
    				message.append(": ");
    				String def = e.getDefinition();
    				if(def!=null)
    				{
    				String split = s.lineSplit(def,fm,targetWidth);
    				message.append(split);
    				}
    				else { message.append("No definition available"); }
    				message.append("\n\n");
    				}
    			}
    		}
    	String m = message.toString();
    	hp.setHelpText(m);
    	}
    }

    private void drawNotice(Graphics gc,Rectangle r,SprintBoard gb)
    {
    	if(!gb.GameOver() && (gb.tilesLeft()==0))
		{	
		 	String msg =  LastTurnMessage ;
			GC.Text(gc, true, r,Color.blue,null, msg);
		}
    }
    private String bigString = null;
    private int bigX = 0;
    private int bigY = 0;
    
    private boolean iAmSpectator() { return getActivePlayer().spectator; }

    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  SprintBoard gb = disB(gc);
       SingleBoard pboard = currentPlayerBoard(gb);
       SprintState state = pboard.getState();
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
       
       //gameLog.redrawGameLog2(gc, nonDragSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),standardBoldFont());
       
       GC.frameRect(gc, Color.black, logRect);
       // this does most of the work, but other functions also use contextRotation to rotate
       // animations and sprites.
       boolean planned = plannedSeating();
     
       commonPlayer pl = getPlayerOrTemp(pboard.whoseTurn);
       double messageRotation = pl.messageRotation();
       {    
       standardGameMessage(gc,stateRect,state);
      	
       
       String msg = pboard.invalidReason==null ? s.get(SprintVictoryCondition) : s.get(pboard.invalidReason);
       String goalmsg = pboard.invalidReason==null ? GoalExplanation : InvalidExplanation;
       goalAndProgressMessage(gc,nonDragSelect,Color.black,msg,progressRect, goalRect,goalmsg);
       }
       drawDrawPile(gc,ourTurnSelect,gb);
       drawPullTimer(gc,ourTurnSelect,gb);
       for(int player=0;player<bb.players_in_game;player++)
       	{ commonPlayer pl1 = getPlayerOrTemp(player);
       	  SingleBoard pb = gb.getPlayerBoard(player);
       	  pl1.setRotatedContext(gc, selectPos,false);
       	   GC.setFont(gc,standardBoldFont());
    	   DrawChipPool(gc, chipRects[player],eyeRects[player],pl1, ourTurnSelect,selectPos,pb);
    	   DrawScore(gc,scoreRects[player],pl1,ourTurnSelect,pb);
    	   if(planned && pboard.whoseTurn==player)
    	   {
    		   handleDoneButton(gc,doneRects[player],(pboard.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
    	   GC.setFont(gc, largeBoldFont());
    	   drawNotice(gc,noticeRects[player],gb);
    	   if((mutable_game_record || iAmSpectator()) && G.pointInRect(selectPos,pl1.playerBox))
    	   {
    		   selectPos.hitCode = SprintId.Switch;
    		   selectPos.hit_index = player;
    		   selectPos.spriteColor = Color.red;
    		   selectPos.spriteRect = pl1.playerBox;
    		   selectPos.setHelpText(SwitchExplanation);
    	   }
       	   pl1.setRotatedContext(gc, selectPos,true);
       	   
       	}
       
     	if(gc!=null)
  		{
  		// note this gets called in the game loop as well as in the display loop
  		// and is pretty expensive, so we shouldn't do it in the mouse-only case  
  		   setDisplayParameters(pboard,boardRect,false);
  		}
       if(!planned)
       {
       drawBoardElements(gc, pboard, boardRect, ourTurnSelect,selectPos);
       drawUnplacedTiles(gc, pboard, boardRect, ourTurnSelect,selectPos);
       }
       if(!planned)
      	{  
    	   // generally prevent spectators seeing tiles, unless openracks or gameover
    	   boolean censorSpectator =  iAmSpectator()&&!allowed_to_edit;
    	   drawRack(gc,pboard,bigRack,
    			    pboard.getPlayerRack(),pboard.getPlayerMappedRack(),
    			    pboard.getRackMap(),pboard.getMapPick(),

    			   	censorSpectator,
    			   	censorSpectator ? null : selectPos,
    			   	ourTurnSelect==null); 
      	}
       GC.setFont(gc,standardBoldFont());
       zoomRect.draw(gc,nonDragSelect);
     
       GC.setFont(gc,standardBoldFont());
       

       if (state != SprintState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{
				boolean done = pboard.DoneState();
				if(done)
					{
					endgameButton.draw(gc,buttonSelect);
					}
				}
			drawNotice(gc,noticeRect,gb);
			
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
			if(state==SprintState.Confirm 
					|| mutable_game_record
					)
					{
					pullButton.show(gc, messageRotation, buttonSelect);
					}
        }
       

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==SprintState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        
        
             //      DrawRepRect(gc,pl.displayRotation,Color.black,b.Digest(),repRect);
        
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        
        if(bigString!=null)
        {	GC.setFont(gc,largeBoldFont());
        	GC.drawBubble(gc,bigX,bigY,bigString,fullRect,messageRotation);
        }
 
    }
    public void standardGameMessage(Graphics gc,Rectangle stateRect,SprintState state)
    {
        standardGameMessage(gc,
   				state==SprintState.Gameover?gameOverMessage():s.get(state.description()),
   				(state!=SprintState.Puzzle) && !state.simultaneousTurnsAllowed(),
   				bb.whoseTurn,
   				stateRect);

    }
    public boolean allowResetUndo() { return(false); }
    
    private commonPlayer viewPlayer = null;
    public void setViewPlayer(commonPlayer p)
    {
    	viewPlayer = p;
    }
    public int getViewPlayer()
    {
    	if(viewPlayer!=null) { return viewPlayer.boardIndex; }
    	return getActivePlayer().boardIndex;
    }
    public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode mode)
    {
    	boolean v = super.PerformAndTransmit(m,transmit,mode);
    	
        if(m.op==MOVE_SWITCH)
        {
        	setViewPlayer(getPlayerOrTemp(m.player));
        }
        else if(m.op==MOVE_PULLSTART)
        {	if((mode==replayMode.Live) && !iAmSpectator())
        	{
        	int pl = getActivePlayer().boardIndex;
        	// synchronously pull new tiles
         	int count = bb.pullCount();
        	while(count>0)
        	{
        		String pc = bb.getPullMove(pl);
        		if(pc!=null) { PerformAndTransmit(pc); }
        		count--;
        	}
        	}
        }          

    	return v;
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
     
        /**
         * animations are handled by a simple protocol between the board and viewer.
         * when stones are moved around on the board, it pushes the source and destination
         * cells onto the animationStck.  startBoardAnimations converts those points into
         * animation sprites.  drawBoardElements arranges for the destination stones, which
         * are already in place, to disappear until the animation finishes.  The actual drawing
         * is done by drawSprites at the end of redrawBoard
         */
        SingleBoard gb = currentPlayerBoard(bb);
        startBoardAnimations(replay,gb.animationStack,gb.cellSize(),MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay!=replayMode.Replay) { playSounds((Sprintmovespec)mm); }
		return (true);
    }
    

     SingleBoard currentPlayerBoard(SprintBoard gb)
     {
    	 return gb.getPlayerBoard(getViewPlayer());
     }
     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for pushfight, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
      * destination chip disappear until the animation is finished.
      * @param replay
      */
//     void startBoardAnimations(replayMode replay)
//     {
//        if(replay!=replayMode.Replay)
//     	{
//     		double full = G.distance(0,0,G.Width(boardRect),G.Height(boardRect));
//        	while(bb.animationStack.size()>1)
//     		{
//     		PushfightCell dest = bb.animationStack.pop();
//     		PushfightCell src = bb.animationStack.pop();
//    		double dist = G.distance(src.current_center_x, src.current_center_y, dest.current_center_x,  dest.current_center_y);
//    		double endTime = masterAnimationSpeed*0.5*Math.sqrt(dist/full);
    		//
    		// in cases where multiple chips are flying, topChip() may not be the right thing.
    		//
//     		startAnimation(src,dest,dest.topChip(),bb.cellSize(),0,endTime);
//     		}
//     	}
//        	bb.animationStack.clear();
//     } 

 void playSounds(Sprintmovespec mm)
 {
	 switch(mm.op)
	 {
	 case MOVE_PULL:
	 case MOVE_PULLSTART:
		 playASoundClip(deskBellSoundName,200);
		 break;
	 case MOVE_DROPB:
		 if(mm.player!=getActivePlayer().boardIndex) { break; }
		//$FALL-THROUGH$
	 case MOVE_MOVETILE:
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
    public commonMove ParseNewMove(String st,int pl)
    {
        return (new Sprintmovespec(st));
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
      {	  // some damaged games ended up with naked "drop", this lets them pass 
    	  switch(nmove.op)
    	  {
    	  case MOVE_SEE:
    	  case MOVE_LIFT:
    	  case MOVE_REPLACE:
    		  return(null);
    	  default: break;
    	  }

    	  commonMove rval = EditHistory(nmove,false);
     	     
    	  return(rval);
      }
      public long setDigest(commonMove m)
      {
    	  long dig = bb.getPlayerBoard(m.player).Digest();
    	  m.digest = dig;
    	  return dig;
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
  public void verifyGameRecord()
   {	super.verifyGameRecord();
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
    {
        if (hp.hitCode instanceof SprintId)// not dragging anything yet, so maybe start
        {
        SprintId hitObject =  (SprintId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    case ZoomSlider:
        case InvisibleDragBoard:
			break;
        case Rack:
        case Unplaced:
        case LocalRack:
        	{
        	String msg = (String)hp.hitObject;
            // transmit only drop from the board, not shuffling of the rack
            PerformAndTransmit(msg);
        	}
    		break;
        case BoardLocation:
	        SprintCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+getViewPlayer()+" "+hitCell.col+" "+hitCell.row);
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
        bigString = null;

       	if(!(id instanceof SprintId))  
       		{   missedOneClick = performStandardActions(hp,missedOneClick);
       			definitionCell = null;
       		}
        else {
        missedOneClick = false;
        SprintId hitCode = (SprintId)id;

        switch (hitCode)
        {
        default:
        	if (performStandardButtons(hitCode, hp)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " + hp);
            }
        	break;
        case Switch:
        	PerformAndTransmit("switch "+hp.hit_index,mutable_game_record,replayMode.Live);
        	break;
        case EndGame:
        	PerformAndTransmit("EndGame "+getViewPlayer());
        	break;
        case ZoomSlider:
        case InvisibleDragBoard:
			break;
        case PullAction:
        	PerformAndTransmit("Pull "+getViewPlayer()+" "+bb.nextTileCount());
        	break;
        case LocalRack:
        	{
        	String msg = (String)hp.hitObject;
            // transmit only drop from the board, not shuffling of the rack
            PerformAndTransmit(msg);
        	}
        	break;
        case Definition:
        	definitionCell = hitCell(hp);
        	break;
        case EyeOption:
        	{
         	String op = (String)hp.hitObject;
         	int rm = remoteWindowIndex(hp);
        	PerformAndTransmit((rm<0 ? "show ":"see ")+getViewPlayer()+" "+op);
        	}
        	break;
        case Blank:
        	{
        	SprintChip ch = (SprintChip)hp.hitObject;
        	PerformAndTransmit("SetBlank "+getViewPlayer()+" "+ch.letter);
        	}
        	break;
        case Unplaced:
        case Rack:
    		{
    		// drawing the rack prepares the move
            String msg = (String)hp.hitObject;
            // transmit only drop from the board, not shuffling of the rack
            PerformAndTransmit(msg);
        	}
        	
        	break;
        case DrawPile:
        case EmptyBoard:
        	{
        		SprintCell hitObject = hitCell(hp);
        		SingleBoard pb = currentPlayerBoard(bb);
        		if(pb.pickedObject==null)
            	{	SprintCell c = getPickedRackCell(hp);
            		if(c!=null)
            		{
            			PerformAndTransmit(G.concat("move ",getViewPlayer()," Rack ",G.printCol(c.col)," ",c.row," ",
            					hitCode.name()," ",G.printCol(hitObject.col)," ",hitObject.row));
            		}
            		else 
            		{
            			PerformAndTransmit(G.concat("Pick ",getViewPlayer()," ",hitCode.name()," ",G.printCol(hitObject.col)," ",hitObject.row));
            		}
            	}
        		else if(hitCode==SprintId.DrawPile)
        		{
        			PerformAndTransmit(G.concat("Drop ",getViewPlayer()," ",hitCode.name()," ",G.printCol(hitObject.col)," ",hitObject.row)); 
        		}
        		else {
        			PerformAndTransmit(G.concat("Dropb ",getViewPlayer()," ",G.printCol(hitObject.col)," ",hitObject.row)); 
        		}
        	}
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
        	{
            SprintCell hitObject = hitCell(hp);
 			PerformAndTransmit(G.concat("Pickb ",getViewPlayer()," ",G.printCol(hitObject.col)," ",hitObject.row));
        	}
			break;
			
        }
        }
    }


    private void setDisplayParameters(SingleBoard pboard,Rectangle r,boolean fixed)
    {
      	// the numbers for the square-on display are slightly ad-hoc, but they look right
      	if(fixed) { 
      		pboard.SetDisplayParameters(1,1.0,0,0,0.0); 
      	}
      	else
      	{
      		pboard.SetDisplayParameters(zoomRect.value,1.0,board_center_x,board_center_y,0.0); // shrink a little and rotate 30 degrees
      	}
      	pboard.SetDisplayRectangle(r);
    }

    /**
     * draw any last-minute items, directly on the visible canvas. These
     * items may appear to flash on and off, if so they probably ought to 
     * be drawn in {@link #drawCanvas}
     * @param offGC the gc to draw
     * @param hp the mouse {@link HitPoint} 
     */
  // public void drawCanvasSprites(Graphics offGC,HitPoint hp)
  //  {
  //     DrawTileSprite(offGC,hp); //draw the floating tile we are dragging, if present
       //
       // draw any animations that are in progress
       //
  //     drawSprites(offGC);       
  //  }
    
    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parseable by {@link online.game.commonCanvas#performHistoryInitialization}
     * @return return what will be the init type for the game
     */
     public String gameType() 
    	{
    	return(bb.gameType()); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Sprint_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int np = G.IntToken(his);	// players always 2
    	long rv = G.IntToken(his);
    	int rev = G.IntToken(his);	// rev does't get used either
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // int rk = G.IntToken(his);
    	// bb.doInit(token,rk);
        bb.doInit(token,rv,np,rev);
        adjustPlayers(np);

    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);

        if(target==saveScreen)
        {	handled = true;
            String ss = sgf_reader.do_sgf_dialog(FileDialog.SAVE,gameName(), "*.png");
            if(ss!=null)
            {
            //dictionary.saveDefinitionList(ss);
            /*
            int bw = G.Width(boardRect);
            int bh = G.Height(boardRect);
            Image boardImage = G.createImage(this,bh,bw);
            Graphics g = boardImage.getGraphics();
            drawFixedBoard(g,new Rectangle(0,0,bw,bh));
            boardImage.SaveImage(ss);
            */
            }      	
        }
        return (handled);
    }
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
    public boolean pullTimeExpired(SprintBoard bb)
    {
    	long lim = bb.drawTimer;
    	return ((lim>=0) && lim<G.Date());
    }
    public void ViewerRun(int wait)
    {	
        super.ViewerRun(wait);
        if(!reviewOnly 
        	 && !GameOver()
        	 && !iAmSpectator()
           	 && !reviewMode())
        {	SingleBoard pb = currentPlayerBoard(bb);
        	SprintState state = pb.getState();
         	switch(state)
        	{
        	default:
        	case Puzzle:
        	case Gameover:
        		break;
        	case EndingGame:
        		PerformAndTransmit("Ended "+getViewPlayer()); 
        		break;
          	case Play: 
          		if(gameRecordingMode()==RecordingStrategy.None) { break; }
        		if(pullTimeExpired(bb))
        		{
	        		if(bb.tilesLeft()==0)
	        		{	     		
        			PerformAndTransmit("EndGame "+getViewPlayer());
        			break;
	        		}
        		}
        		else { break; }
				//$FALL-THROUGH$
			case Confirm: 
        		if(bb.revision<102)
        		{
        		int nt = bb.nextTileCount();
        		PerformAndTransmit(G.concat("Pull ",getViewPlayer()," ",nt));
        		}
        		else if(bb.tilesLeft()>0)
        		{
        			PerformAndTransmit(G.concat("PullStart ",getViewPlayer()));
         		}
        		break;

          	case Endgame:
          		if(gameRecordingMode()==RecordingStrategy.None) { break; }
        		if(pullTimeExpired(bb))
        		{
        			PerformAndTransmit("EndGame "+getViewPlayer());
        		}
        	}
        }
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
    public BoardProtocol getBoard()  
    {    return (bb);   
    }

    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }



      /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new SprintPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 12 files visited 0 problems
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
                adjustPlayers(bb.nPlayers());
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
    public void setLimbo(boolean v)
    {
    	boolean oldLimbo = inLimbo;
    	super.setLimbo(v);
    	if(oldLimbo!=inLimbo)
    	{
    		setGameRecorder();
    	}
    }
    
    /**
     * Sprint strategy for recording games is different from all others.  One player
     * is designated as the recorder, and that client does all the recording of game
     * state on the server.   This avoids the Rashomon effect where each client has
     * a unique view of the order of events.   If the recording player loses connection,
     * some other player is designated as the recorder, and their view of the game
     * history replaces the previous recorder.
     * 
     * The primary organizing method for this is the setLimbo method.  The supervising
     * game object coordinates the new/old record strings around the limbo state.
     * 
     * The worrisome case is where the recording client loses connection, but the 
     * other clients have continued transmitting for a while, so they have more moves
     * than the recording client has recorded.  In that case, the new recorder will
     * record the advanced state, which the old recorder will retrieve when they
     * reconnect. 
     */
    private int gameRecorder = 0;
    private void setGameRecorder()
    {	// select the lowest boardIndex of a real player as the recorder
    	int least = -1;
    	for(int i=0;i<players.length;i++)
    	{
    		commonPlayer p = players[i];
    		// isActivePlayer excludes robots and players who have disconnected.
    		if(p!=null && p.isActivePlayer())
    		{
    			if(least<0 || least>p.boardIndex) { least = p.boardIndex; } 
    		}
    	}
    	gameRecorder = least;	
    }
    
    public RecordingStrategy gameRecordingMode()
    {
    	return getActivePlayer().boardIndex==gameRecorder ? RecordingStrategy.Single : RecordingStrategy.None;
    }
    public void startPlaying()
    {
    	super.startPlaying();
    	setGameRecorder();
    }
    /** 
     * @return true if the game is in a GameOverState state right now.
    */
   public boolean GameOverNow()
   {
   	SingleBoard b = currentPlayerBoard(bb);
   	boolean now = b.GameOver();
   	gameOver |= now;
   	return(now);	// game over right now?
   }
   public boolean gameHasEphemeralMoves()
   {
	   return true;
   }
   static String KEYWORD_TIMER = "Timer";
   static String KEYWORD_END = "ENDEPHEMERAL";
   
   public String formEphemeralMoveString()
   {
	   return " " + KEYWORD_TIMER +" "+ bb.drawTimer + " "+KEYWORD_END;
   }
   public void useEphemeralMoves(StringTokenizer c)
   {
	   String command = "";
	   while( !KEYWORD_END.equals(command=c.nextToken()))
	   {
		   if(KEYWORD_TIMER.equals(command))
		   {
			   long g = G.LongToken(c);
			   bb.drawTimer = g;
			   
		   }
	   }
   }
}

