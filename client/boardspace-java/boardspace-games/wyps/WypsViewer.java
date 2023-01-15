package wyps;


import java.awt.*;

import static wyps.Wypsmovespec.*;
import online.common.*;
import java.util.*;

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
import lib.Image;
import lib.Random;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.game.sgf.sgf_reader;
import online.search.SimpleRobotProtocol;


/**
 *  Initial work Sept 2020 
*/
public class WypsViewer extends CCanvas<WypsCell,WypsBoard> implements WypsConstants, GameLayoutClient
{	static final long serialVersionUID = 1000;
	boolean DRAWBACKGROUNDTILES = true;
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(235,235,235);
    private Color rackBackGroundColor = new Color(192,192,192);
    private Color boardBackgroundColor = new Color(220,165,155);
    private Color newDarkLetterColor = new Color(239,66,75);
    private Color newLightLetterColor = new Color(212,5,25);
    private Dictionary dictionary = Dictionary.getInstance();
    private boolean robotGame = false;
    private int rackSize = 2;
    
    // private state
    private WypsBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //public Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //public Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle chatRect = addRect("chatRect");
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
   
    //
    // addZoneRect also sets the rectangle as specifically known to the 
    // mouse tracker.  The zones are considered in the order that they are
    // added, so the smaller ones should be first, then any catchall.
    //
    // zones ought to be mostly irrelevant if there is only one board layout.
    //
    private Rectangle chipRects[] = addZoneRect("chip",2);
    private Rectangle drawPileRect = addRect("drawPileRect");
    private Rectangle timeRect = addRect("time");
    private Rectangle rotateRect = addRect("rotate");
    private boolean lockOption = false;
    private Rectangle lockRect = addRect("lock");
    private Rectangle largerBoardRect = addRect("largerBoard");
    private Rectangle altNoChatRect = addRect("nochat");
    private Rectangle checkWordsRect = addRect("checkwordsrect");
    //private Rectangle repRect = addRect("repRect");	// not needed for pushfight
    int boardRotation = 0;
    double effectiveBoardRotation = 0.0;
    private Rectangle bigRack = addZoneRect("bigrack");
    TextButton passButton = addButton(PASS,GameId.HitPassButton,ExplainPass,
			HighlightColor, rackBackGroundColor);
	TextButton swapButton = addButton(SWAP,GameId.HitSwapButton,SwapTiles,
			HighlightColor, rackBackGroundColor);
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	WypsChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = WypsChip.Icon.image;
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
        	WypsConstants.putStrings();
        }
        
        String type = info.getString(GAMETYPE, WypsVariation.Wyps.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new WypsBoard(type,players_in_game,randomKey,getStartingColorMap(),dictionary,WypsBoard.REVISION);
        robotGame = sharedInfo.get(exHashtable.ROBOTGAME)!=null;
        // flickers background, keep off 5/13/2022
        // useDirectDrawing(G.isIOS());
        doInit(false);
        adjustPlayers(players_in_game);
        
        if(G.debug()) { saveScreen = myFrame.addAction("Save Board Image",deferredEvents); }

    }
    JMenuItem saveScreen;
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
        	// the color determines the first player
        	startFirstPlayer();
        	//
        	// alternative where the first player is just chosen
        	//startFirstPlayer();
 	 		//PerformAndTransmit(reviewOnly?"Edit":"Start P"+first, false,replayMode.Replay);

    	}
    }


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
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*22;	
    	int vcrw = fh*16;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = (G.isCodename1()?8:6)*fh;
        int stateH = fh*3;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			aspect,	// aspect ratio for the board
    			fh*2,	// min cell size
    			fh*3,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);
       	int doneW = G.Width(editRect);
       	layout.placeRectangle(passButton, doneW,doneW/2,BoxAlignment.Center);
       	if(G.debug()) 
       		{ layout.placeRectangle(checkWordsRect, G.Width(editRect),G.Height(editRect),BoxAlignment.Center);
       		}
       	commonPlayer pl = getPlayerOrTemp(0);
       	int spare =  G.Height(pl.playerBox)*2/3;
       	layout.placeRectangle(drawPileRect,spare,spare,BoxAlignment.Center);
       	       	
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main)-stateH*2;
    	
    	// There are two classes of boards that should be rotated. For boards with a strong
    	// "my side" orientation, such as chess, use seatingFaceToFaceRotated() as
    	// the test.  For boards that are noticably rectangular, such as Push Fight,
    	// use mainW<mainH
    	boolean planned = plannedSeating();
    	int nrows =  bb.nrows+(planned?0:2);
        int ncols =  bb.ncols+(planned?0:2);
  	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW+(planned?stateH:0);
    	int boardY = mainY+extraH+(planned?stateH:0);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        G.placeRow(stateX,stateY,boardW ,stateH,stateRect,rotateRect,lockRect,altNoChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH-(planned?0:2*CELLSIZE));
    	G.SetRect(goalRect, boardX, G.Bottom(boardRect),boardW,stateH);   
    	G.SetRect(timeRect,  boardX, G.Bottom(goalRect),boardW,timeControl().timeControlMessage()==null?0:stateH); 
    	int goalBottom = G.Bottom(timeRect);
    	G.SetRect(bigRack, boardX, goalBottom, boardW, planned?0:CELLSIZE*6/3);
    	G.SetRect(largerBoardRect,boardX-stateH,boardY-stateH,boardW+stateH*2,boardH+stateH*2);
    	int sz = Math.min(boardW/4,CELLSIZE*3);
    	G.SetRect(swapButton,G.Right(boardRect)+CELLSIZE/2-sz,boardY+CELLSIZE*2,sz,sz/3);


    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(progressRect, boardX+boardW/3,G.Bottom(bigRack)-stateH/3,boardW/3,stateH/3);
    	
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        labelFont = largeBoldFont();
        return(boardW*boardH);
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle box =  pl.createRectangularPictureGroup(x,y,unitsize);
    	Rectangle done = doneRects[player];
    	
    	int doneW = plannedSeating()? unitsize*4 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.union(box, done);
    	int unith = (rackSize-2)*unitsize;
       	G.SetRect(chip,	x,	G.Bottom(box),	unith*3*7/4,unith);
        G.union(box, chip);
    	pl.displayRotation = rotation;
    	return(box);
    }
    @SuppressWarnings("unused")
	private void drawOptions(Graphics gc,HitPoint highlight,HitPoint highlightAll,WypsBoard gb)
    {
   	
  
    }
    @SuppressWarnings("unused")
	private void drawOptionIcon(Graphics gc,Option op,Rectangle r,HitPoint highlight,HitPoint highlightAll)
    {	
     	boolean value = bb.getOptionValue(op);
    	WypsChip chip = value ? op.onIcon : op.offIcon;
    	if(chip.drawChip(gc,this,r,!robotGame || op.allowedForRobot ? highlight : null,WypsId.SetOption))
    		{
    		highlight.hitObject = "SetOption "+op.name()+" "+!value;
    		}
    	HitPoint.setHelpText(highlightAll, r, s.get(op.message));
    	GC.frameRect(gc,Color.black,r);
     }
    private void drawDrawPile(Graphics gc,HitPoint highlight,WypsBoard gb)
    {	Rectangle r = drawPileRect;
    	int boxw = G.Width(r);
    	int cs = Math.min(gb.cellSize(),boxw/4);
    	int w = boxw-cs;
    	int h = G.Height(r)-cs-cs/2;
    	int l = G.Left(r)+cs/2;
    	int t = G.Top(r)+cs/2;
    	boolean inside = G.pointInRect(highlight, r);
    	
    	boolean canHit = inside && gb.LegalToHitPool(getOurMovingObject(highlight)>=0);
		gb.drawPile.setLastSize(cs);
    	GC.frameRect(gc,Color.black,r);
      	if(canHit && highlight!=null)
      		{
      		highlight.hitObject = gb.drawPile;
      		highlight.hitCode=WypsId.DrawPile;
      		highlight.spriteColor = Color.red;
      		highlight.spriteRect = r;
      		}
      	if(inside)
      	{
      		HitPoint.setHelpText(highlight, r,s.get(DumpRackMessage));
      	}
    	if(w>0 && h>0)
    	{
    	Random rand = new Random(2324);
    	int tilesLeft = gb.drawPile.height();
    	WypsCell last = gb.lastDropped();
    	for(int i=0;i<tilesLeft;i++)
    	{
    		int dx = l+rand.nextInt(w);
    		int dy = t+rand.nextInt(h);
    		boolean hide = !((i==tilesLeft-1) && (last==gb.drawPile));
    		WypsChip chip = gb.drawPile.chipAtIndex(i);
    		if(rand.nextInt(2)==1) { chip = chip.getAltChip(); }
    		if(chip.drawChip(gc, this, canHit?highlight:null, WypsId.DrawPile,cs,dx,dy,
    				hide ? WypsChip.BACK : null))
        		{
        			highlight.hitObject = gb.drawPile;
        		}
    		gb.drawPile.rotateCurrentCenter(gc,dx,dy);
    	}
    	GC.setFont(gc, standardBoldFont());
    	GC.Text(gc, true, l,G.Bottom(r)-cs/2,w,cs/2,Color.black,null,s.get(TilesLeft,tilesLeft));
    	}
     }
	// draw a box of spare chips. For pushfight it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r,commonPlayer pl, HitPoint highlight,HitPoint highlightAll,WypsBoard gb)
    {	int pidx = pl.boardIndex;
    	Rectangle rack = chipRects[pidx];
    	commonPlayer ap = getActivePlayer();
       	 

       	if(allowed_to_edit || ap==pl) { for(WypsCell c : gb.getPlayerRack(pidx)) { c.seeFlyingTiles=true; }}
       	boolean open = gb.openRack[pidx];
      	boolean showTiles = open || allowed_to_edit || bb.openRacks; 
       	boolean anyRack = G.offline() || allowed_to_edit || (ap==pl);
       	drawRack(gc,gb,rack,gb.getPlayerRack(pidx),gb.getPlayerMappedRack(pidx),gb.rackMap[pidx],gb.getMapPick(pidx),!showTiles ,anyRack ? highlightAll : null,!anyRack);  	
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
     * @param cells
     * @param map
     * @param picked
     * @param censor
     * @param highlight
     * @param rackOnly
     * @param pl
     */
    private void drawRack(Graphics gc,WypsBoard gb, Rectangle rack,
    		WypsCell[]cells,
    		WypsCell[]mappedCells,
    		int map[],int picked,boolean censor,HitPoint highlight,boolean rackOnly)
    {
    	int h = G.Height(rack);
    	int w = G.Width(rack);
    	int cy = G.centerY(rack);
    	int nsteps = map.length;
    	int xstep = Math.min(w/(nsteps+1),h*3/4); 
    	int tileSize = (int)(xstep*1);
    	int cx = G.Left(rack)+(w-xstep*nsteps)/2+xstep/2;
       	GC.frameRect(gc, Color.black, rack);
       	//.print("");
       	// for remote viewers, always use this size
       	/*
       	if(G.debug())
       	{
       	int cx0 = cx;
     	for(int idx = 0;idx<cells.length;idx++)
   		{
     		WypsCell d = cells[idx];
       	if(d!=null) 
       	{
      	setLetterColor(gc,gb,d);
      	//print("Draw "+idx+" "+c+" "+top+" @ "+cx);
      	WypsChip dtop = d.topChip();
      	if(dtop!=null) { d.drawChip(gc, this, dtop, tileSize*2/3, cx0, cy+tileSize*2/3, null);}
      	cx0 += xstep;
       	}}}
       	 */
    	for(int idx = 0;idx<nsteps;idx++)
		{
    	int mapValue = map[idx];
    	WypsCell c = mapValue>=0 ? cells[mapValue] : null;
    	WypsCell mc = mappedCells[idx];
    	WypsChip top = c==null ? null : c.topChip();
    	mc.reInit();
    	if(top!=null) { mc.addChip(top); }
    	
       	boolean legalPick = gb.LegalToHitChips(mc);
    	if(picked==idx) { top = null; }
     	boolean localDrop = picked>=0;
     	boolean moving = gb.pickedObject!=null;
    	boolean ourDrop = !rackOnly && moving;	// we can drop from the board, and something is moving
    	boolean remoteDrop = ourDrop && gb.LegalToHitChips(mc);
    	char myCol = cells[0].col;
    	boolean canDrop = (localDrop | remoteDrop);
    	boolean canPick = legalPick && 
    				(!(localDrop || remoteDrop)
    						&& (top!=null) 
    						&& !canDrop 
    						&& !ourDrop);

    	{
       	if(mc.activeAnimationHeight()>0) { top = null; }
      	setLetterColor(gc,gb,mc);
      	//print("Draw "+idx+" "+c+" "+top+" @ "+cx);
    	mc.drawChip(gc, this, top, tileSize, cx, cy, censor ? WypsChip.BACK : null);
       	if(c!=null) { c.copyCurrentCenter(mc);	}// set the center so animations look right

       	}

    	if(canDrop && top==null)
    	{
    		StockArt.SmallO.drawChip(gc,this,tileSize,cx,cy,null);
    		
    	}  
    	boolean ptin =  G.pointInRect(highlight, cx-tileSize/2,cy-tileSize/2,tileSize,h);
     	if((canPick||canDrop) && ptin)
    	{
   			highlight.hit_x = cx;
    		highlight.hit_y = cy;
			highlight.spriteColor = Color.red;
			highlight.awidth = tileSize;
    		if(remoteDrop)
    		{	c = null;
    		    for(int i=0;c==null && i<cells.length;i++) 
    				{ if(cells[i].topChip()==null) { c = cells[i]; }
    				}
    		    if(c!=null)
    		    {
    			highlight.hitObject = G.concat("Drop Rack ",c.col," ",c.row," ", idx);	
    		    }
    		    else { highlight.spriteColor = null; }
    			highlight.hitCode = WypsId.Rack;
    		}
    		else if(localDrop )
    		{
    			highlight.hitObject = G.concat("Replace Rack ",myCol," ",idx);
    			highlight.hitCode = WypsId.LocalRack;
	
        			}
    		else
    		{   highlight.hitObject = G.concat("Lift Rack ",myCol," ",idx," ",map[idx]);
    			highlight.hitCode = WypsId.LocalRack;
    		}
    	}
		cx += xstep;
		}

    }
 
    private WypsCell getMovingTile(int ap)
    {
    	int picked = bb.getMapTarget(ap);
    	if(picked>=0) { 
    		WypsCell[] prack = bb.getPlayerRack(ap);
    		if(prack==null) { return null;}
    		if(picked>=0 && picked<prack.length)
    		{
    			return prack[picked];
    		}
    	}
    	return(null);
    }
    
    private WypsCell getPickedRackCell(HitPoint highlight)
    {
    	{int ap = allowed_to_edit||G.offline() ? bb.whoseTurn : getActivePlayer().boardIndex;
    	 WypsCell c = getMovingTile(ap);
    	 if(c!=null) { return(c); }
    	}
       	HiddenGameWindow hidden = findHiddenWindow(highlight);
    	if(hidden!=null)
    	{	WypsCell c = getMovingTile(hidden.getIndex());
    		if(c!=null) { return(c); }
    	}
    	if(allowed_to_edit || G.offline())
    	{	commonPlayer pl = inPlayerBox(highlight);
    		if(pl!=null)
    			{WypsCell c = getMovingTile(pl.boardIndex);
    			return(c);
    			}
    	}
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
    {	WypsCell picked = getPickedRackCell(highlight);
    	if(picked!=null)
    	{
    		WypsChip top = picked.topChip();
    		if(top!=null) { return(top.chipNumber()); }
    	}
        if (OurMove())
        {
            return (getBoard().movingObjectIndex());
        }
        return (NothingMoving);
    }

    public int getMovingObject(HitPoint highlight)
    {	// censor the identity of the moving object for other players
        if (OurMove())
        {	int mo = bb.movingObjectIndex();
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
       	commonPlayer pl = getPlayerOrTemp(bb.whoseTurn);
       	// hidden windows have x coordinates that are negative, we don't want to rotate tiles
       	// being displayed on hidden windows
       	if(xp>=-100) { GC.setRotation(g, pl.displayRotation,xp,yp); }
       	drawPrerotatedSprite(g,obj,xp,yp);
       	if(xp>=-1000) { GC.setRotation(g, -pl.displayRotation,xp,yp); }
    }
    public void drawPrerotatedSprite(Graphics g,int obj,int xp,int yp)
    { 
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
       	WypsChip ch = WypsChip.getChip(obj);
       	if(ch!=null)
       		{
       		// hidden windows have x coordinates that are negative, we don't want to rotate tiles
       		// being displayed on hidden windows
       		GC.setColor(g,Color.black);
       		ch.drawChip(g,this,CELLSIZE, xp, yp, null);  
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
     WypsChip.backgroundTile.image.tileImage(gc, fullRect);   
      GC.setRotatedContext(gc,largerBoardRect,null,effectiveBoardRotation);
      drawFixedBoard(gc,boardRect);
      GC.unsetRotatedContext(gc,null);  
      if(DRAWBACKGROUNDTILES)
      {
    		drawDrawPile(gc,null,bb);
      }
     }
    public void drawFixedElements(Graphics gc,boolean complete)
    {	commonPlayer pl = getPlayerOrTemp(bb.whoseTurn);
    	if(!lockOption) { effectiveBoardRotation = (boardRotation*Math.PI/2+pl.displayRotation); }
    	complete |= (DRAWBACKGROUNDTILES && (digestFixedTiles()!=fixedTileDigest))
    				|| pendingFullRefresh;
    	super.drawFixedElements(gc,complete);
    }

    // land here after rotating the board drawing context if appropriate
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	
        boolean reviewBackground = reviewMode()&&!mutable_game_record;
        if(reviewBackground)
        {	 
         WypsChip.backgroundReviewTile.image.tileImage(gc,brect);   
        }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(bb,brect);
	      // if the board is one large graphic, for which the visual target points
	      // are carefully matched with the abstract grid
	      //G.centerImage(gc,images[BOARD_INDEX], brect,this);

	      // draw a picture of the board. In this version we actually draw just the grid
	      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
	      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
	      // on the board to fine tune the exact positions of the text
	      bb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

	      // draw the tile grid.  The positions are determined by the underlying board
	      // object, and the tile itself if carefully crafted to tile the pushfight board
	      // when drawn this way.  For games with simple graphics, we could use the
	      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
	      // but for more complex graphics with overlapping shadows or stacked
	      // objects, this double loop is useful if you need to control the
	      // order the objects are drawn in.
    	  int xsize = bb.cellSize();
	      for(WypsCell c = bb.allCells; c!=null; c=c.next)
	      {
	    	  int ypos = G.Bottom(brect) - bb.cellToY(c.col, c.row);
	    	  int xpos = G.Left(brect) + bb.cellToX(c.col, c.row);
	    	  WypsChip.Tile.getAltDisplayChip(c).drawChip(gc,this,xsize,xpos,ypos,null);              
	      }     
       	
	      if(DRAWBACKGROUNDTILES) 
	    	  { drawFixedTiles(gc,brect,bb);
	    	  }
	//      draw
    }
    private void setDisplayParameters(WypsBoard gb,Rectangle r)
    {
    	// the numbers for the square-on display are slightly ad-hoc, but they look right
    	gb.SetDisplayParameters( 0.95, 0.94, 0.5,-1,30); // shrink a little and rotate 30 degrees     	
      	gb.SetDisplayRectangle(r);

    }
    static long FIXEDRANDOM = 346546235;
    //
    // in this game, most of the letters on the board and all of the drawpile
    // will change very slowly if at all.  We mark appropriate cells as "fixed"
    // and draw those with the background.  On PCs this is hardly noticable, 
    // but on mobiles if makes a big difference.
    // This digest determines when the background has changed, and needs to be redrawn.
    public long digestFixedTiles()
    {	Random r = new Random(FIXEDRANDOM);
    	long v = 0;
    	for(WypsCell cell = bb.allCells; cell!=null; cell=cell.next)
        {	if(cell.isFixed)
        	{
        	v ^= cell.Digest(r);
        	}
        }
    	int tilesLeft = bb.drawPile.height();
    	v ^= (tilesLeft*r.nextLong());
    	v ^= G.rotationQuarterTurns(effectiveBoardRotation)*r.nextLong();
    	return(v);
    }
    long fixedTileDigest = 0;
    private boolean pendingFullRefresh = false;
    public void drawFixedTiles(Graphics gc,Rectangle brect,WypsBoard gb)
    {	Random r = new Random(FIXEDRANDOM);
    	long v = 0;
   		pendingFullRefresh = !spritesIdle();
   		Enumeration<WypsCell>cells = gb.getIterator(Itype.TBRL);
   		while(cells.hasMoreElements())
        {	WypsCell cell = cells.nextElement();
        if(cell.isFixed)
        	{
        	int ypos = G.Bottom(brect) - gb.cellToY(cell);
        	int xpos = G.Left(brect) + gb.cellToX(cell);
        	v ^= cell.Digest(r);
        	setLetterColor(gc,gb,cell);
        	cell.drawStack(gc,this,null,CELLSIZE,xpos,ypos,1,1,null);
        	}
        }
    	int tilesLeft = gb.drawPile.height();
    	v ^= (tilesLeft*r.nextLong());
    	v ^= G.rotationQuarterTurns(effectiveBoardRotation)*r.nextLong();
        fixedTileDigest=v;
    }
    
public void setLetterColor(Graphics gc,WypsBoard gb,WypsCell cell)
{	if(gc!=null)
	{
    WypsChip ch = cell.topChip();
    if(ch!=null)
    {
    boolean temp = gb.isADest(cell);
    boolean last = gb.lastLetters.contains(cell);
    Color col = last 
    				? (cell.topChip().getColor()==WypsColor.Light) ? newLightLetterColor : newDarkLetterColor  
    				: Color.black
    					;
    cell.isFixed = !(temp||last);
	labelColor = col;
	GC.setColor(gc, col);
    }
    }
}

    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, WypsBoard gb, Rectangle brect, HitPoint highlight,HitPoint all)
    {	
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  

        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        WypsCell closestCell = gb.closestCell(highlight,brect);
        boolean moving = getOurMovingObject(highlight)>=0;
        boolean hitCell = gb.LegalToHitBoard(closestCell,moving);
        if(hitCell)
        { // note what we hit, row, col, and cell
          boolean dest = gb.isADest(closestCell);
          boolean empty = closestCell.isEmpty();
          boolean picked = (gb.pickedObject!=null);
          highlight.hitCode = (empty||picked) ? WypsId.EmptyBoard : WypsId.BoardLocation; 
          highlight.hitObject = closestCell;
          highlight.arrow = dest ? (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow : null;
          highlight.awidth = CELLSIZE;
        }
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        {
        Enumeration<WypsCell>cells = gb.getIterator(Itype.TBRL);
		while(cells.hasMoreElements())
          { WypsCell cell = cells.nextElement();
            boolean drawhighlight = (hitCell && (cell==closestCell));
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
             if (drawhighlight)
             { // checking for pointable position
            	 StockArt.SmallO.drawChip(gc,this,gb.cellSize()*5,xpos,ypos,null);                
             }
            boolean selected = (gb.isSelected(cell) || gb.isADest(cell));
            if(selected || (!cell.isFixed || !DRAWBACKGROUNDTILES))
            {
            setLetterColor(gc,gb,cell);
            String msg = selected ? WypsChip.SELECT : null; 
            cell.drawStack(gc,this,null,CELLSIZE,xpos,ypos,1,1,msg);
            }
            // show the edge mask
            // G.Text(gc,""+cell.edgeMask,xpos,ypos);
            //if(G.debug() && (gb.endCaps.contains(cell) || gb.startCaps.contains(cell)))
            //	{ StockArt.SmallO.drawChip(gc, this, CELLSIZE,xpos,ypos,null); 
            //	}
            }
        }
    }

    private String definitionString(Entry e)
    {
    	if(e!=null)
    	{
    		return(s.get(PrevWordMessage,e.word,e.getDefinition()));
    	}
    	return("");
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
    {  WypsBoard gb = disB(gc);
       WypsState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
       
   	   if(gc!=null)
   		{
   		// note this gets called in the game loop as well as in the display loop
   		// and is pretty expensive, so we shouldn't do it in the mouse-only case
      
       setDisplayParameters(gb,boardRect);
   		}
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
       
       gameLog.redrawGameLog2(gc, nonDragSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),standardBoldFont());
       GC.frameRect(gc, Color.black, logRect);
       // this does most of the work, but other functions also use contextRotation to rotate
       // animations and sprites.
       boolean planned = plannedSeating();
     
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       {
       
       GC.setRotatedContext(gc,largerBoardRect,selectPos,effectiveBoardRotation);
       standardGameMessage(gc,stateRect,state);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,selectPos);
       /*
       drawOptions(gc,((state==WypsState.Puzzle)
    		   			||(state==WypsState.FirstPlay)
    		   			||((state==WypsState.Play) && robotGame && bb.moveNumber<=2)
    		   			) ? ourTurnSelect :null,selectPos,gb);
        */
       String msg = bb.invalidReason==null 
    		   ? bb.previousWord==null 
    		   		? s.get(WypsVictoryCondition)
    		   		: definitionString(bb.previousWord)
    		   : s.get(bb.invalidReason);
       String goalmsg = bb.invalidReason==null 
    		? GoalExplanationOnly
    		: InvalidExplanation;
       goalAndProgressMessage(gc,nonDragSelect,Color.black,msg,progressRect, goalRect,goalmsg);
       String timeControl = timeControl().timeControlMessage();
       if(timeControl!=null)
       {
    	   GC.Text(gc,true,timeRect,Color.black,null,timeControl);
       }
       if(planned) 
       	{ StockArt.Rotate180.drawChip(gc, this,rotateRect, selectPos, WypsId.Rotate,s.get(RotateMessage)); 
       	  WypsChip chip = lockOption ? WypsChip.UnlockRotation : WypsChip.LockRotation;
       	    chip.drawChip(gc, this,lockRect,
       			  selectPos, WypsId.Lock,s.get(chip.tip)); 
       	}
       drawNoChat(gc,altNoChatRect,selectPos);
       
		switch(state)
		{
		case FirstPlay:
		case ConfirmFirstPlay:
			swapButton.show(gc,messageRotation, buttonSelect);
			break;
		case Atari:
			GC.setFont(gc, largeBoldFont());
			// use the rectangle for a message
			GC.Text(gc, true, swapButton,newLightLetterColor,null,s.get(ConnectedWarning));

			break;
		default: break;
		}
		GC.unsetRotatedContext(gc,selectPos);
       }
       drawDrawPile(DRAWBACKGROUNDTILES?null:gc,ourTurnSelect,gb);
       for(int player=0;player<bb.players_in_game;player++)
       	{ commonPlayer pl1 = getPlayerOrTemp(player);
       	  pl1.setRotatedContext(gc, selectPos,false);
       	   GC.setFont(gc,standardBoldFont());
    	   DrawChipPool(gc, chipRects[player],pl1, ourTurnSelect,selectPos,gb);
    	   if(planned && gb.whoseTurn==player)
    	   {
    		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
       	   pl1.setRotatedContext(gc, selectPos,true);
       	}
       if(!planned)
      	{  
    	   int ap = allowed_to_edit|G.offline() ? gb.whoseTurn : getActivePlayer().boardIndex;
    	   drawRack(gc,gb,bigRack,gb.getPlayerRack(ap),gb.getPlayerMappedRack(ap),gb.rackMap[ap],gb.getMapPick(ap),false ,selectPos,ourTurnSelect==null); 
      	}
     
       GC.setFont(gc,standardBoldFont());
       

       if (state != WypsState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? buttonSelect : null),HighlightColor, rackBackGroundColor);		
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
			switch(state)
			{
			case Atari:
			case Play:

				if((buttonSelect!=null)
					&& gb.notStarted())
					{
					passButton.show(gc,buttonSelect);
					}
				if(G.debug() && GC.handleRoundButton(gc, checkWordsRect, buttonSelect,"Check",HighlightColor, rackBackGroundColor))
				{
					buttonSelect.hitCode = WypsId.CheckWords;
				}
				break;
			default: break;
			}

        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==WypsState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        
        
             //      DrawRepRect(gc,pl.displayRotation,Color.black,b.Digest(),repRect);
        
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        


    }
    public void standardGameMessage(Graphics gc,Rectangle stateRect,WypsState state)
    {
        standardGameMessage(gc,
   				state==WypsState.Gameover?gameOverMessage():s.get(state.description()),
   				state!=WypsState.Puzzle,
   				bb.whoseTurn,
   				stateRect);

    }
    public boolean allowResetUndo() { return(false); }
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
        startBoardAnimations(replay,bb.animationStack,bb.cellSize(),MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay!=replayMode.Replay) { playSounds((Wypsmovespec)mm); }
       return (true);
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

 void playSounds(Wypsmovespec mm)
 {
	 switch(mm.op)
	 {
	 case MOVE_PLAYWORD:
		 for(int i=0;i<mm.word.length();i++) 
		 	{ playASoundClip(light_drop,150);
		 	}
		 break;		 
	 case MOVE_MOVETILE:
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
    public commonMove ParseNewMove(String st,int pl)
    {
        return (new Wypsmovespec(st, pl));
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
    	  case MOVE_SHOW:
    	  case MOVE_SEE:
    	  case MOVE_LIFT:
    	  case MOVE_REMOTELIFT:
    	  case MOVE_REMOTEDROP:
    	  case MOVE_REPLACE:
    	  case MOVE_CANCELLED:
    		  return(null);
    	  default: break;
    	  }
    	  commonMove rval = EditHistory( nmove, false);
     	     
    	  return(rval);
      }

    

 // for reference, here's the standard definition
 //   public void verifyGameRecord()
 //   {	BoardProtocol ourB =  getBoard();
 //   	int ourDig = ourB.Digest();
 //   	BoardProtocol dup = dupBoard = ourB.cloneBoard();
 //   	int dupDig = dup.Digest();
 //   	G.Assert(dupDig==ourDig,"Duplicate Digest Matches");
 //   	dup.doInit();
 //   	int step = History.size();
 //   	int limit = viewStep>=0 ? viewStep : step;
 //   	for(int i=0;i<limit;i++) 
 //   		{ commonMove mv = History.elementAt(i);
 //   		  //G.print(".. "+mv);
 //   		  dup.Execute(mv); 
 //   		}
 //   	int dupRedig = dup.Digest();
 //   	G.Assert(dup.whoseTurn()==ourB.whoseTurn(),"Replay whose turn matches");
 //   	G.Assert(dup.moveNumber()==ourB.moveNumber(),"Replay move number matches");
 //   	if(dupRedig!=ourDig)
 //   	{
 //   	//int d0 = ourB.Digest();
 //   	//int d1 = dup.Digest();
 //   	G.Assert(false,"Replay digest matches");
 //   	}
 //   	// note: can't quite do this because the timing of "SetDrawState" is wrong.  ourB
 //   	// may be a draw where dup is not if ourB is pending a draw.
 //   	//G.Assert(dup.getState()==ourB.getState(),"Replay state matches");
 //   	dupBoard = null;
 //   }
    
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
        if (hp.hitCode instanceof WypsId)// not dragging anything yet, so maybe start
        {
        WypsId hitObject =  (WypsId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	       case Rack:
	       case LocalRack:
	    		{
	    		// drawing the rack prepares the move
	            String msg = (String)hp.hitObject;
	            // transmit only drop from the board, not shuffling of the rack
	            boolean transmit = (hitObject==WypsId.Rack);
	            if(msg.startsWith("remotedrop "))
	        	{
	        		PerformAndTransmit(G.replace(msg,"remotedrop","replace"),false,replayMode.Live);
	        	}
	            PerformAndTransmit(msg,transmit,replayMode.Live);
	           
	            if(msg.startsWith("rlift "))
	            	{
	            		PerformAndTransmit(msg.substring(1),false,replayMode.Live);
	            	}
	        	}
	    		break;
   
	    case BoardLocation:
	        WypsCell hitCell = hitCell(hp);
	    	if(bb.isADest(hitCell))
	    		{ PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    		}
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
       	if(!(id instanceof WypsId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        WypsId hitCode = (WypsId)id;
        switch (hitCode)
        {
        default:
        	if (performStandardButtons(hitCode, hp)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " , hp);
            }
        	break;
        case Swap:
        	PerformAndTransmit("swap");
        	break;
        case CheckWords:
        	bb.checkWords();
        	break;
        case Lock:
        	lockOption = !lockOption;
        	break;
        case Rotate:
        	effectiveBoardRotation = (effectiveBoardRotation+Math.PI/2);
        	boardRotation = (boardRotation+1)%4;
        	contextRotation = (boardRotation*Math.PI)/2;
        	generalRefresh();
        	break;
        case EyeOption:
        	{
         	String op = (String)hp.hitObject;
         	HiddenGameWindow w = findHiddenWindow(hp);
        	PerformAndTransmit(((w==null&&remoteViewer<0) ? "show ":"see ")+op);
        	}
        	break;
        case SetOption:
        	{
        	String m = (String)hp.hitObject;
        	PerformAndTransmit(m);
        	}
        	break;
        case Blank:
        	{
        	WypsChip ch = (WypsChip)hp.hitObject;
        	PerformAndTransmit("SetBlank "+ch.letter);
        	}
        	break;
        case LocalRack:
        	break;
        case Rack:
			{
			// drawing the rack prepares the move
	        String msg = (String)hp.hitObject;
	        // transmit only drop from the board, not shuffling of the rack
	        PerformAndTransmit(msg,true,replayMode.Live);
	    	}
         	break;
        case DrawPile:
        case EmptyBoard:
        	{
        		WypsCell hitObject = hitCell(hp);
        		if(bb.pickedObject==null)
            	{	WypsCell c = getPickedRackCell(hp);
            		if(c!=null)
            		{
            			PerformAndTransmit(G.concat("move Rack ",c.col," ",c.row," ",
            					hitCode.name()," ",hitObject.col," ",hitObject.row));
            		}
            		else 
            		{
            			PerformAndTransmit(G.concat("Pick ",hitCode.name()," ",hitObject.col," ",hitObject.row));
            		}
            	}
        		else if(hitCode==WypsId.DrawPile)
        		{
        			PerformAndTransmit(G.concat("Drop ",hitCode.name()," ",hitObject.col," ",hitObject.row)); 
        		}
        		else {
        			PerformAndTransmit(G.concat("Dropb ",hitObject.col," ",hitObject.row)); 
        		}
        	}
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
        	{
            WypsCell hitObject = hitCell(hp);
            boolean dest = bb.isADest(hitObject);
            WypsState state = bb.resetState;
            switch(state)
            {
            default: throw G.Error("Not expecting hit board in state "+state);
            case FlipTiles:
            	PerformAndTransmit("Flip "+hitObject.col+" "+hitObject.row);
            	break;
            case Play:
            case Atari:
            	PerformAndTransmit((dest ? "Pickb " : "Select ")+hitObject.col+" "+hitObject.row);
            	break;
            case Puzzle:
            	PerformAndTransmit("Pickb " +hitObject.col+" "+hitObject.row);
            	break;
        	}}
			break;
        }
        }
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
    public String sgfGameType() { return(Wyps_SGF); }	// this is the official SGF number assigned to the game

   
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
            int bw = G.Width(boardRect);
            int bh = G.Height(boardRect);
            Image boardImage = Image.createImage(bh,bw);
            Graphics g = boardImage.getGraphics();
            drawFixedBoard(g,new Rectangle(0,0,bw,bh));
            boardImage.SaveImage(ss);
            
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

    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }


     /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new WypsPlay());
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


}

