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
package crosswords;

import static crosswords.Crosswordsmovespec.*;

import online.common.*;
import java.util.*;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.FileDialog;
import bridge.FontMetrics;
import bridge.JMenuItem;
import bridge.SystemFont;
import common.GameInfo;
import dictionary.Dictionary;
import dictionary.Entry;
import lib.Graphics;
import lib.CellId;
import lib.Drawable;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.Slider;
import lib.StockArt;
import lib.TextButton;
import lib.Toggle;
import lib.Tokenizer;
import lib.Random;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.game.sgf.sgf_reader;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import vnc.VNCService;

//
// TODO: add a button to show the game log on side screens
// TODO: make word definitions pop on companion apps also
// TODO: pop grand total for the current play
// TODO: position letters selected by the side screen off-center if on the board
// TODO: rethink on-board rotation, maybe use only the main rotater
// TODO: maybe rotate the game log to face the player
// TODO: add a personal "rotate to me" button in planned mode
//
/**
 *  Initial work Sept 2020 
*/
public class CrosswordsViewer extends CCanvas<CrosswordsCell,CrosswordsBoard> implements CrosswordsConstants
{	static final long serialVersionUID = 1000;
	static final String Crosswords_SGF = "crosswords"; // sgf game name

	// file names for jpeg images and masks
	static final String ImageDir = "/crosswords/images/";
	boolean DRAWBACKGROUNDTILES = true;
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(235,235,235);
    private Color rackBackGroundColor = new Color(192,192,192);
    private Color boardBackgroundColor = new Color(220,165,155);
    private Color newLetterColor = new Color(0.20f,0.00f,0.9f);
    private Color tempLetterColor = new Color(0.0f,0.5f,0.1f);
	private Color middleGray = new Color(0x64,0x64,0x64);
    private Dictionary dictionary = Dictionary.getInstance();
    private boolean robotGame = false;
    private int rackSize = 2;
    
    // private state
    private CrosswordsBoard bb = null; //the board from which we are displaying
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
    private Toggle bigTypeToggle = new Toggle(this,"bigfont",
    		CrosswordsChip.BigFontOn,CrosswordsId.BigfontOff,UseSmallFont,
    		CrosswordsChip.BigFontOff,CrosswordsId.BigfontOn,UseBigFont);
    
    private Rectangle chipRects[] = addZoneRect("chips",4);
    private Rectangle scoreRects[] = addZoneRect("playerScore",4);
    private Rectangle eyeRects[] = addZoneRect("PlayerEye",4);
    private Rectangle noticeRects[] = addRect("notice",4);
    private Rectangle drawPileRect = addRect("drawPileRect");
    private TextButton passButton = addButton(PASS,GameId.HitPassButton,ExplainPass,
			HighlightColor, rackBackGroundColor);
    private Rectangle rotateRect = addRect("rotate");
    private boolean lockOption = false;
    private Rectangle lockRect = addRect("lock");
    private Rectangle largerBoardRect = addRect(".largerBoard");
    private Rectangle altNoChatRect = addRect("nochat");
    //private Rectangle repRect = addRect("repRect");	// not needed for pushfight
    int boardRotation = 0;
    double effectiveBoardRotation = 0.0;
    private Rectangle bigRack = addZoneRect("bigrack");
    private Rectangle backwardsRect = addRect("backwards");
    private Rectangle diagonalsRect = addRect("diagonals");
    private Rectangle connectedRect = addRect("connected");
    private Rectangle dupsRect = addRect("duplicates");
    private Rectangle openRect = addRect("openrack");
    private Rectangle noticeRect = addRect("notice");
    private TextButton checkWordsButton = addButton(JustWordsMessage,CrosswordsId.CheckWords,
    		JustWordsHelp,
						HighlightColor, rackBackGroundColor);
    private Slider vocabularyRect = new Slider(VocabularyMessage,CrosswordsId.Vocabulary,0,1,0);

/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	CrosswordsChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = CrosswordsChip.Icon.image;
    }

    public int getAltChipset()
    {
    	return bigTypeToggle.isOnNow() ? 1 : 0;
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
        painter.useBackgroundBitmap = false;
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        
        super.init(info,frame);
        
        // the prevents racking back and forth when picking a tile from the
        // rack in review mode.  See comments in commonCanvas
        resynchronizeOnUnbranch = false;
 
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	CrosswordsConstants.putStrings();
        }
        MouseColors = mouseColors;
        MouseDotColors = dotColors;
        
        String type = info.getString(GameInfo.GAMETYPE, CrosswordsVariation.Crosswords.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new CrosswordsBoard(type,players_in_game,randomKey,getStartingColorMap(),dictionary,CrosswordsBoard.REVISION);
        robotGame = sharedInfo.get(OnlineConstants.ROBOTGAME)!=null;
        useDirectDrawing(true);
        doInit(false);
        adjustPlayers(players_in_game);
        
        // defer this as long as possible to give the dictionary time to load
        vocabularyRect.setValue(CrosswordsPlay.vocabularyPart(online.search.RobotProtocol.DUMBOT_LEVEL));
        
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
    /** this is called by the game controller when all players have connected
     * and the first player is about to be allowed to make his first move. This
     * may be a new game, or a game being restored, or a player rejoining a game.
     * You can override or encapsulate this method.
     */


    public double aspects[] = {0.9,1.0,1.1};
    public void setLocalBounds(int x,int y,int w,int h)
    {	if(plannedSeating())
    	{
    	int dim = Math.min(w, h);
    	rackSize = 4;
    	do {
    		setLocalBoundsV(x,y,w,h,aspects);
    		int boardw = G.Width(boardRect);
			if(boardw>dim*0.65) { break; }
			rackSize-=0.25;
    		}
    		while ((G.Width(boardRect)<(dim*0.75)) && (rackSize>3.0));
    	}
    else {
    	rackSize =2;
    	setLocalBoundsV(x,y,w,h,aspects);
    }

//    	int sz = standardFontSize();
//   		G.print("Racksize "+rackSize+" "+selectedLayout);
//   		G.print("cell size ",(int)(selectedLayout.selectedCellSize()/sz)," ",sz);

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

    public double setLocalBoundsA(int x, int y, int width, int height,double aspect0)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	double aspect = Math.abs(aspect0);
    	vertical = aspect0>0;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = boardMax ? 0 : fh*16;	
       	int minChatW = fh*35;	
       	int vcrw = fh*16;
        int margin = fh/2;
        int buttonW = 8*fh;
        int stateH = fh*5/2;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.6,	// 60% of space allocated to the board
    			aspect,	// aspect ratio for the board
    			fh*(plannedSeating()?3:2),	// min cell size
    			fh*(plannedSeating()?4:2.5),	// maximum cell size
    			0.2		// preference for the designated layout, if any
    			);
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);
    	layout.placeTheChat(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2);
    	layout.placeRectangle(logRect,minLogW, plannedSeating() ? minLogW : minLogW*2/3, minLogW*3/2, minLogW*3/2,BoxAlignment.Edge,true);
       	layout.alwaysPlaceDone = false;
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,noticeRect);
       	int doneW = G.Width(editRect);
       	layout.alwaysPlaceDone = true;
       	layout.placeDoneEditRep(doneW,doneW,passButton,checkWordsButton,vocabularyRect);
      	 
       	commonPlayer pl = getPlayerOrTemp(0);
       	int spare = Math.min(G.Height(pl.playerBox),fh*7);
       	layout.placeRectangle(drawPileRect,spare,spare,BoxAlignment.Edge);
       	       	
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main)-stateH*2;
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
    	int boardX = mainX+extraW+stateH;
    	int boardY = mainY+extraH+stateH;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH/2;
        int stateX = boardX;
    	int stripeW = CELLSIZE;
    	placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,altNoChatRect);
    	G.SetRect(boardRect,boardX-(planned?CELLSIZE:0),boardY,boardW,boardH-(planned?0:2*CELLSIZE));
    	if(plannedSeating())
    	{
    		placeRow(boardX, G.Bottom(boardRect),boardW,stateH,goalRect,bigTypeToggle,rotateRect,lockRect);   
    	}
    	else
    	{
    		placeRow(boardX, G.Bottom(boardRect),boardW,stateH,goalRect,bigTypeToggle);   
    	}
    	int bigRackH = planned?0:CELLSIZE*2;
    	G.SetRect(bigRack, boardX+CELLSIZE/2, G.Bottom(goalRect), boardW-CELLSIZE-stripeW, bigRackH);
    	G.SetRect(largerBoardRect,boardX-stateH,boardY-stateH,boardW+stateH*2,boardH+stateH*2);

    	int stripeLeft = G.Right(largerBoardRect)-stripeW-CELLSIZE/3;
    	int stripeTop = boardY+boardH-9*stripeW;
    	G.SetRect(backwardsRect,stripeLeft,stripeTop,stripeW,stripeW);
    	stripeTop += stripeW+stateH/2;
    	G.SetRect(diagonalsRect,stripeLeft,stripeTop,stripeW,stripeW);
    	stripeTop += stripeW+stateH/2;
    	G.SetRect(connectedRect,stripeLeft,stripeTop,stripeW,stripeW);
       	stripeTop += stripeW+stateH/2;
    	G.SetRect(dupsRect,stripeLeft,stripeTop,stripeW,stripeW);
       	stripeTop += stripeW+stateH/2;
    	G.SetRect(openRect,stripeLeft,stripeTop,stripeW,stripeW);
  

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        labelFont = largeBoldFont();
        return(boardW*boardH+bigRackH*boardW);
    }
    boolean vertical = false;
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	boolean planned = plannedSeating();
    	boolean showRacks = allPlayersLocal() || !boardMax;
    	Rectangle chip = chipRects[player];
    	Rectangle score = scoreRects[player];
    	Rectangle eye = eyeRects[player];
    	int scoreW = unitsize*5/2;
    	int scoreH = unitsize*3/2;
    	G.SetRect(score,x,y,scoreW-2,scoreH);
    	int eyesize = showRacks ? unitsize*2 : 0;
    	G.SetRect(eye, x, y+scoreH, eyesize,eyesize);
     	Rectangle box =  pl.createRectangularPictureGroup(x+scoreW,y,unitsize);
    	Rectangle done = doneRects[player];
    	Rectangle notice = noticeRects[player];
    	
    	int doneW = planned ? unitsize*4 : 0;
    	int donel = G.Right(box)+unitsize/2;
    	int noticeH = unitsize;
    	G.SetRect(done,donel,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.union(box, done,score,eye);
    	double unith = showRacks ? rackSize*unitsize : 0;
       	if(vertical)
       		{ 
       		G.SetRect(chip,	x,	G.Bottom(box),	(int)(unith*5),(int)(unith*7/8)); 
       		G.SetRect(notice, donel , G.Bottom(done),doneW*2,noticeH);
       		}
       	else
       		{ 
       		int boxH = G.Height(box);
       		int boxR = G.Right(box)+unitsize/4;
       		int mbox = (int)Math.max(unith*7/8,boxH-noticeH);
       		G.SetRect(chip,boxR,y,(int)(unith*5),mbox); 
        	G.SetRect(notice, boxR ,y+mbox,doneW*2,noticeH);
       		}
        G.union(box, chip,notice);
    	pl.displayRotation = rotation;
    	return(box);
    }
    private void drawOptions(Graphics gc,HitPoint highlight,HitPoint highlightAll,CrosswordsBoard gb)
    {
   	
    	drawOptionIcon(gc,Option.Backwards,backwardsRect,highlight,highlightAll);
    	drawOptionIcon(gc,Option.Diagonals,diagonalsRect,highlight,highlightAll);
    	drawOptionIcon(gc,Option.Connected,connectedRect,highlight,highlightAll);
    	drawOptionIcon(gc,Option.NoDuplicate,dupsRect,highlight,highlightAll);
    	drawOptionIcon(gc,Option.OpenRacks,openRect,highlight,highlightAll);
  
    }
    private void drawOptionIcon(Graphics gc,Option op,Rectangle r,HitPoint highlight,HitPoint highlightAll)
    {	
     	boolean value = bb.getOptionValue(op);
    	CrosswordsChip chip = value ? op.onIcon : op.offIcon;
    	if(chip.drawChip(gc,this,r,!robotGame || op.allowedForRobot ? highlight : null,CrosswordsId.SetOption,(String)null))
    		{
    		highlight.hitObject = "SetOption "+op.name()+" "+!value;
    		}
    	HitPoint.setHelpText(highlightAll, r, s.get(op.message));
    	GC.frameRect(gc,Color.black,r);
     }
    private void drawDrawPile(Graphics gc,HitPoint highlight,CrosswordsBoard gb)
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
      		highlight.hitCode=CrosswordsId.DrawPile;
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
    	CrosswordsCell last = gb.lastDropped();
    	for(int i=0;i<tilesLeft;i++)
    	{
    		int dx = l+rand.nextInt(w);
    		int dy = t+rand.nextInt(h);
    		boolean hide = !((i==tilesLeft-1) && (last==gb.drawPile));
    		if(gb.drawPile.chipAtIndex(i).drawChip(gc, this, cs, dx,dy,canHit?highlight:null,CrosswordsId.DrawPile,
    				hide ? CrosswordsChip.BACK : null))
    		{
    			highlight.hitObject = gb.drawPile;
    		}
    		gb.drawPile.rotateCurrentCenter(gc,dx,dy);
    	}
    	GC.setFont(gc, largeBoldFont());
    	GC.Text(gc, true, l,G.Bottom(r)-cs/2,w,cs/2,Color.black,null,s.get(TilesLeft,tilesLeft));
    	}
     }
    public Drawable getPlayerIcon(int n)
    {
    	return bb.getPlayerChip(n);
    }
	// draw a box of spare chips. For pushfight it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r,Rectangle er, commonPlayer pl, HitPoint highlight,HitPoint highlightAll,CrosswordsBoard gb)
    {	int pidx = pl.boardIndex;
    	Rectangle rack = chipRects[pidx];
    	commonPlayer ap = getActivePlayer();
       	 
   	if(!mutable_game_record 
   			&& !isSpectator()
   			&& (isTableGame() 
   								|| (isPassAndPlay() 
   										? pl==currentGuiPlayer()
   										: pl==getActivePlayer())))
   	{	// always for yourself, and also if this is an around-the-table game
    		drawEye(gc,gb,er,gb.openRack[pidx],highlightAll,pl.boardIndex);
    	}
    	CrosswordsCell prack[] = gb.getPlayerRack(pidx);
    	if(prack!=null)
    	{
      	boolean showTiles = explicitlyVisible(gb,pl.boardIndex); 
      	for(CrosswordsCell c : prack) { c.seeFlyingTiles=showTiles; } 
      	boolean anyRack = showTiles && (isTableGame() || allowed_to_edit || (ap==pl));
      	drawRack(gc,gb,rack,prack,gb.getPlayerMappedRack(pidx),
      			gb.getRackMap(pidx),gb.getMapPick(pidx),!showTiles,anyRack ? highlight : null,!anyRack);  	

   		}
   	}
    
    private void drawEye(Graphics gc,CrosswordsBoard gb,Rectangle er,boolean showing,HitPoint highlightAll,int who)
    {
   		StockArt chip = showing ? StockArt.NoEye : StockArt.Eye;
   		String help = s.get(chip==StockArt.Eye ? ShowTilesMessage : HideTilesMessage);
		if(chip.drawChip(gc, this, er, highlightAll, CrosswordsId.EyeOption,help))
		{	
	   		boolean newv = !showing;
			highlightAll.hitObject = (char)('A'+who)+(newv ?" true":" false");
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
    private void drawRack(Graphics gc,CrosswordsBoard gb, Rectangle rack,
    		CrosswordsCell[]cells,
    		CrosswordsCell[]mappedCells,
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
       	if(remoteViewer>=0) { CELLSIZE = tileSize; }
       	/* this was useful when debugging rack manipulation code 
       	if(false)//G.debug())
       	{
       	int cx0 = cx;
     	for(int idx = 0;idx<cells.length;idx++)
   		{
     	CrosswordsCell d = cells[idx];
       	if(d!=null) 
       	{
      	setLetterColor(gc,gb,d);
      	//print("Draw "+idx+" "+c+" "+top+" @ "+cx);
      	CrosswordsChip dtop = d.topChip();
      	if(dtop!=null) { d.drawChip(gc, this, dtop, tileSize*2/3, cx0, cy+tileSize*2/3, null);}
      	cx0 += xstep;
       	}}}
       	 */
    	for(int idx = 0;idx<nsteps;idx++)
		{
    	int mapValue = map[idx];
    	CrosswordsCell c = mapValue>=0 ? cells[mapValue] : null;
    	CrosswordsCell mc = mappedCells[idx];
    	CrosswordsChip top = c==null ? null : c.topChip();
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
    	mc.drawChip(gc, this, top, tileSize, cx, cy, censor ? CrosswordsChip.BACK : null);
    	if(c!=null) { bb.getCell(c).copyCurrentCenter(mc);	}// set the center so animations look right
       	}

    	if(canDrop && top==null)
    	{
    		StockArt.SmallO.drawChip(gc,this,CELLSIZE,cx,cy,null);
    		
    	}  
     	if((canPick||canDrop) && G.pointInRect(highlight, cx-tileSize/2,cy-tileSize/2,tileSize,tileSize))
    	{
   			highlight.hit_x = cx;
    		highlight.hit_y = cy;
			highlight.spriteColor = Color.red;
			highlight.awidth = tileSize;
    		if(remoteDrop)
    		{	c = null;
    			// it's important that the actual destination cell in the rack
    			// is chosen consistently and unrelated to the UI cell the 
    			// user actually touches.  If remote screens are in use, the
    			// tile can be dropped separately on both screens.
    		    for(int i=0;c==null && i<cells.length;i++) 
    				{ if(cells[i].topChip()==null) { c = cells[i]; }
    				}
 
    		    if(c!=null)
    		    {
    		    highlight.hitObject = G.concat("droponrack Rack ",c.col," ",c.row," ", idx);	
    			highlight.hitCode = CrosswordsId.Rack ;
    		    }
    		    else { highlight.spriteColor = null; }
    		}
    		else if(remoteViewer>=0)
    		{	
    			if(localDrop)
    			{
    				highlight.hitObject = G.concat("remotedrop Rack ",myCol," ",idx);
    			}
    			else
    			{
    				// pick by a remote viewer
    				highlight.hitObject = G.concat("rlift Rack ",myCol," ",idx," ",map[idx]);
    			}
    			highlight.hitCode = CrosswordsId.RemoteRack ;
    		}
    		else if(localDrop )
    		{	
    			highlight.hitObject = G.concat("replace Rack ",myCol," ",idx);
    			highlight.hitCode = CrosswordsId.LocalRack;
    			
    		}
    		else
    		{   highlight.hitObject = G.concat("lift Rack ",myCol," ",idx," ",map[idx]);
    			highlight.hitCode = CrosswordsId.LocalRack;
    			highlight.spriteColor=Color.blue;
    		}
			
    	}
		cx += xstep;
		}

    }


    private void DrawScore(Graphics gc, Rectangle r, commonPlayer pl, HitPoint highlight,CrosswordsBoard gb)
    {	int pidx = pl.boardIndex;
    	int val = gb.score[pidx];
    	if(G.Height(r)>0)
    	{
       	Drawable chip = getPlayerIcon(pidx);
       	chip.drawChip(gc,this,r,null);
    	GC.setFont(gc,SystemFont.getFont(largeBoldFont(),G.Height(r)*3/5));
    	GC.Text(gc, true,r,Color.white,null,""+val);
    	}
    }
    /**
     * return the dynamically adjusted size during an animation.  This allows
     * compensation for things like the zoom level of the board changing after
     * the animation is started.
     */
    //public int activeAnimationSize(Drawable chip,int thissize) { 	 return(thissize); 	} 
    
    private CrosswordsCell getMovingTile(int ap)
    {
    	int picked = bb.getMapTarget(ap);
    	if(picked>=0) { 
    		CrosswordsCell[] prack = bb.getPlayerRack(ap);
    		if(prack==null) { return null;}
    		if(picked>=0 && picked<prack.length)
    		{
    			return prack[picked];
    		}
    	}
    	return(null);
    }
    private CrosswordsCell getPickedRackCell(HitPoint highlight)
    {
       	int rm = remoteWindowIndex(highlight);
    	if(rm>=0)
    	{	CrosswordsCell c = getMovingTile(rm);
    		if(c!=null) { return(c); }
    	}
    	else {
    	{int ap = allowed_to_edit||allPlayersLocal() ? bb.whoseTurn : getActivePlayer().boardIndex;
    	 CrosswordsCell c = getMovingTile(ap);
    	 if(c!=null) { return(c); }
    	}
 
     	if(allowed_to_edit || allPlayersLocal())
    	{	commonPlayer pl = inPlayerBox(highlight);
    		if(pl!=null)
    			{CrosswordsCell c = getMovingTile(pl.boardIndex);
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
    {	CrosswordsCell picked = getPickedRackCell(highlight);
    	if(picked!=null)
    	{
    		CrosswordsChip top = picked.topChip();
    		if(top!=null) { return(top.chipNumber()); }
    	}
        if (OurMove())
        {
            return (bb.movingObjectIndex());
        }
        return (NothingMoving);
    }

    public int getMovingObject(HitPoint highlight)
    {	// censor the identity of the moving object for other players
        if (OurMove())
        {	int mo = bb.movingObjectIndex();
            if((mo>=0)&&!allowed_to_edit) { mo = CrosswordsChip.Blank.chipNumber(); }
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
       	if(xp>=-100) { GC.setRotation(g, effectiveBoardRotation,xp,yp); }
       	drawPrerotatedSprite(g,obj,xp,yp);
       	if(xp>=-1000) { GC.setRotation(g,effectiveBoardRotation,xp,yp); }
    }
    public void drawPrerotatedSprite(Graphics g,int obj,int xp,int yp)
    { 
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
       	CrosswordsChip ch = CrosswordsChip.getChip(obj);
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
     CrosswordsChip.backgroundTile.image.tileImage(gc, fullRect);   
      GC.setRotatedContext(gc,largerBoardRect,null,effectiveBoardRotation);
      drawFixedBoard(gc,boardRect);
      GC.unsetRotatedContext(gc,null);  
      if((remoteViewer<0) && DRAWBACKGROUNDTILES)
      {		GC.setRotatedContext(gc,drawPileRect,null,effectiveBoardRotation);
    		drawDrawPile(gc,null,disB(gc));
    		GC.unsetRotatedContext(gc,null);  
      }
     }
    public void drawFixedElements(Graphics gc,boolean complete)
    {	commonPlayer pl = getPlayerOrTemp(bb.whoseTurn);
    	if(!lockOption) { effectiveBoardRotation = (boardRotation*Math.PI/2+pl.displayRotation); }
    	complete |= (DRAWBACKGROUNDTILES && (digestFixedTiles(disB(gc))!=fixedTileDigest))
    			     || pendingFullRefresh;
    	super.drawFixedElements(gc,complete);
    }

    // land here after rotating the board drawing context if appropriate
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	CrosswordsBoard gb = disB(gc);
        boolean reviewBackground = reviewMode()&&!mutable_game_record;
        if(reviewBackground)
        {	 
         CrosswordsChip.backgroundReviewTile.image.tileImage(gc,brect);   
        }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(gb,brect);
	      // if the board is one large graphic, for which the visual target points
	      // are carefully matched with the abstract grid
	      //G.centerImage(gc,images[BOARD_INDEX], brect,this);
	  	if(remoteViewer<0)
	  	{
	      // draw a picture of the board. In this version we actually draw just the grid
	      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
	      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
	      // on the board to fine tune the exact positions of the text
	      gb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

	      // draw the tile grid.  The positions are determined by the underlying board
	      // object, and the tile itself if carefully crafted to tile the pushfight board
	      // when drawn this way.  For games with simple graphics, we could use the
	      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
	      // but for more complex graphics with overlapping shadows or stacked
	      // objects, this double loop is useful if you need to control the
	      // order the objects are drawn in.
    	  int xsize = gb.cellSize();
	      for(CrosswordsCell c = gb.allCells; c!=null; c=c.next)
	      {
	    	  int ypos = G.Bottom(brect) - gb.cellToY(c.col, c.row);
	    	  int xpos = G.Left(brect) + gb.cellToX(c.col, c.row);
	    	  CrosswordsChip.Tile.drawChip(gc,this,xsize,xpos,ypos,null);              
	      }     
	      int ysize = getAltChipset()==0 ? xsize : (int)(xsize*1.3);
	      for(CrosswordsCell c = gb.allPostCells; c!=null; c=c.next)
	      {
	    	  int ypos = G.Bottom(brect) - gb.cellToY(c.col, c.row) - gb.cellSize()/2;
	    	  int xpos = G.Left(brect) + gb.cellToX(c.col, c.row) + gb.cellSize()/2;
	    	  CrosswordsChip tile = c.topChip();
	    	  //tile.scale = new double[] { 0.42,0.32,0.78};
	    	  if(tile!=null)
	    	  	{ tile.drawChip(gc,this,tile==CrosswordsChip.Post ? xsize : ysize,xpos,ypos,null); 
	    	  	}	               
	       }       	
	      if(DRAWBACKGROUNDTILES) 
	    	  { drawFixedTiles(gc,brect,gb);
	    	  }
	  	}
	//      draw
    }
    
    static long FIXEDRANDOM =6435724;
    //
    // in this game, most of the letters on the board and all of the drawpile
    // will change very slowly if at all.  We mark appropriate cells as "fixed"
    // and draw those with the background.  On PCs this is hardly noticable, 
    // but on mobiles if makes a big difference.
    // This digest determines when the background has changed, and needs to be redrawn.
    public long digestFixedTiles(CrosswordsBoard gb)
    {	Random r = new Random(FIXEDRANDOM);
    	long v = 0;
    	for(CrosswordsCell cell = gb.allCells; cell!=null; cell=cell.next)
        {	if(cell.isFixed)
        	{
        	v ^= cell.Digest(r);
        	}
        }
    	int tilesLeft = gb.drawPile.height();
    	v ^= (tilesLeft*r.nextLong());
    	v ^= G.rotationQuarterTurns(effectiveBoardRotation)*r.nextLong();
    	return(v);
    }
    long fixedTileDigest = 0;
    private boolean pendingFullRefresh = false;
    public void drawFixedTiles(Graphics gc,Rectangle brect,CrosswordsBoard gb)
    {	long v = 0;
    	Random r = new Random(FIXEDRANDOM);
    	pendingFullRefresh = !spritesIdle();
    	Enumeration<CrosswordsCell>cells = gb.getIterator(Itype.RLTB);
    	while(cells.hasMoreElements())
        {	CrosswordsCell cell = cells.nextElement();
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
    
public void setLetterColor(Graphics gc,CrosswordsBoard gb,CrosswordsCell cell)
{	if(gc!=null)
	{
    CrosswordsChip ch = cell.topChip();
    if(ch!=null)
    {
    boolean blank = ch.isBlank();
    boolean isDest = false;
    Color col = blank
    			? middleGray 
    				: gb.lastLetters.contains(cell) 
    					? newLetterColor 
    					: (isDest = gb.isADest(cell)) ? tempLetterColor : Color.black
    					;
    cell.isFixed = isDest;
	labelColor = col;
	GC.setColor(gc,col);
    }
    }
}
	CrosswordsCell definitionCell = null;
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, CrosswordsBoard gb, Rectangle brect, HitPoint highlight,HitPoint all)
    {	
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  

        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        CrosswordsCell closestCell = gb.closestCell(all,brect);
        CrosswordsState state = gb.getState();
        boolean resolve = state==CrosswordsState.ResolveBlank;
        boolean moving = getOurMovingObject(highlight)>=0;
        boolean hitCell = !resolve && (highlight!=null) && gb.LegalToHitBoard(closestCell,moving);
        if(hitCell)
        { // note what we hit, row, col, and cell
          boolean empty = closestCell.isEmpty();
          boolean picked = (gb.pickedObject!=null);
          highlight.hitCode = (empty||picked) ? CrosswordsId.EmptyBoard : CrosswordsId.BoardLocation;
          highlight.hitObject = closestCell;
          highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
          highlight.awidth = CELLSIZE;
        }
        definitionCell = null;
        if(closestCell!=null && all!=null && !gb.isADest(closestCell))
        {	CrosswordsCell cc = bb.getCell(closestCell);
        	for(int lim=gb.words.size()-1; lim>=0; lim--)
        	{
        	Word word = gb.words.elementAt(lim);
        	if(!resolve && (word.seed==cc))
        	{	all.hitCode = CrosswordsId.Definition;
        		all.hitObject = cc;
        		all.setHelpText(s.get(GetDefinitionMessage,word.name));
        		definitionCell = cc;
        	}
        	}
         }
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        {
        Enumeration<CrosswordsCell>cells = gb.getIterator(Itype.RLTB);
		while(cells.hasMoreElements())
          { CrosswordsCell cell = cells.nextElement();
            boolean drawhighlight = (hitCell && (cell==closestCell));
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            if (drawhighlight)
             { // checking for pointable position
            	 StockArt.SmallO.drawChip(gc,this,gb.cellSize()*5,xpos,ypos,null);                
             }
            if(!cell.isFixed || !DRAWBACKGROUNDTILES)
            {
            setLetterColor(gc,gb,cell);
            cell.drawStack(gc,this,null,CELLSIZE,xpos,ypos,1,1,null);
            }
            if(!resolve && G.pointInRect(all, xpos,ypos,CELLSIZE,CELLSIZE))
            	{
            	CrosswordsCell cc = gb.getPostCell(cell.col,cell.row-1);
            	if(cc!=null) {
            		CrosswordsChip top = cc.topChip();
            		if(top!=null && top.tip!=null)
            		{
            			all.setHelpText(s.get(top.tip));
            		}
            	}
            	}
            //if(G.debug() && (gb.endCaps.contains(cell) || gb.startCaps.contains(cell)))
            //	{ StockArt.SmallO.drawChip(gc, this, CELLSIZE,xpos,ypos,null); 
            //	}
            }
        }
       if(resolve)
        {
        	drawResolveBlank(gc,gb,brect,highlight);
        }
        if(definitionCell!=null)
        {
        	drawDefinition(gc,gb,all);
        }
    }
    public void drawDefinition(Graphics gc,CrosswordsBoard gb,HitPoint hp)
    {
    	CrosswordsCell target = definitionCell;
    	StringBuilder message = new StringBuilder();
    	WordStack words = gb.words;
    	FontMetrics fm = lib.FontManager.getFontMetrics(standardPlainFont());
    	int targetWidth = G.Width(boardRect)/2;
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
    public void drawResolveBlank(Graphics gc, CrosswordsBoard gb, Rectangle brect, HitPoint highlight)
    {
    	int w = CELLSIZE*7;
    	int h = CELLSIZE*6;
    	CrosswordsCell focus = gb.lastDropped();
    	if(focus!=null)
    	{	// focus has to be non-null, unless there's a bug somewhere else.
    		// guess why this test was added. :)
    	boolean horizontal = gb.dropIsHorizontal();
    	int left = G.Left(brect);
    	int top = G.Top(brect);
    	int bottom = G.Bottom(brect);
    	int right = G.Right(brect);
    	int xp = left+gb.cellToX(focus);
    	int yp = bottom-gb.cellToY(focus);
    	int q = CELLSIZE/5;
    	if(horizontal)
    	{	// place above or below
    		if(focus.row*2>gb.nrows) { yp += CELLSIZE; } 
    		else { yp -= (h+CELLSIZE); }
    		xp = Math.max(Math.min(right-w-q,xp-w/2),left+q);
    		}
    	else {
    		// place left or right
    		if((focus.col-'A')*2<gb.ncols) { xp += CELLSIZE; }
    		else { xp -= (w+CELLSIZE); }
    		yp = Math.max(top+q,Math.min(yp-h/2,bottom-h-q));
    	}
    	Rectangle r = new Rectangle(xp,yp,w,h);
    StockArt.Scrim.image.stretchImage(gc,r);
    	GC.frameRect(gc, Color.black, r);
    	GC.Text(gc,true,xp,yp+CELLSIZE/5,w,CELLSIZE/2,Color.black,null,s.get(SelectBlankMessage));
    	
    	int cx = xp+CELLSIZE;
    	int cy = yp+CELLSIZE+CELLSIZE/7;
    	for(CrosswordsChip ch : CrosswordsChip.assignedBlanks)
    	{
    		ch.drawChip(gc,this, CELLSIZE, cx, cy, highlight, CrosswordsId.Blank,null,1.3,1.3);
    		cx += CELLSIZE;
    		if(cx>=xp+w) { cx = xp+CELLSIZE; cy += CELLSIZE; }
    	}}
    }
    private void drawNotice(Graphics gc,Rectangle r,CrosswordsBoard gb)
    {	if((G.Width(r)>0) 
    		&& !gb.GameOver()
    		&& (gb.drawPile.height()==0)
    		)
    	{	
		 	String msg = (gb.someRackIsEmpty()) ? LastTurnMessage : s.get(TilesLeft,0);
			GC.Text(gc, true, r,Color.blue,null, msg);
    	}
    }
    private String bigString = null;
    private int bigX = 0;
    private int bigY = 0;
    
    public void drawAnnotationMenu(Graphics g,HitPoint p)
    {	// this makes the annotation menu follow the board rotation, which is independent of the window rotation
    	GC.setRotatedContext(g,largerBoardRect,p,effectiveBoardRotation);
    	super.drawAnnotationMenu(g,p);
    	GC.unsetRotatedContext(g,p);
    }
 
    public void showAnnotationMenu()
    {	// TODO: fix sub-rotated annotation menu on crosswordle 
    	// this doesn't have the desired effect.  When you've used the local rotater on the menu,
    	// the actual popup appears in full screen rotation
    	//int oldro = getCanvasRotation() ;
    	//int newro = oldro + boardRotation;
    	//setCanvasRotation(newro);
    	super.showAnnotationMenu();
    	//setCanvasRotation(oldro);
    }

    private boolean explicitlyVisible(CrosswordsBoard gb,int who)
    {
       	return (mutable_game_record 	// endgame review or reviewonly
       			|| gb.openRacks 		// always visible
       			|| gb.openRack[who]);	// this player always visible;
    }
    private int mainRackVisibleFor = -1;
    /**
     * for pass-and-play games, the intended behavior is to conceal the main rack
     * after every turn change.  So you make your move, pass the device to the next
     * player, then they hit the eye button to see their tiles. If you use the VCR
     * to look back, you can see your own current tiles when appropriate.
     * @param gb
     * @return if the main rack should be visible
     */
    private boolean rackCensored(CrosswordsBoard gb,int rack)
    {	if(explicitlyVisible(gb,rack)) { return false; }
    	
    	if(isSpectator()) { return true; }  	
    	if(isTurnBasedGame())
    		{
    		commonPlayer ap = getActivePlayer();
    		return (ap.boardIndex!=rack);
    		}
    	if(!isOfflineGame())
    		{ commonPlayer gui = getActivePlayer();
    		  return gui.boardIndex!=rack;
    		}
    	if(!isPassAndPlay()) { return false; }
    	if(rack==mainRackVisibleFor) { return false; }
    	if(!reviewMode()) { mainRackVisibleFor = -1; }
    	return true;
    }
    
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  CrosswordsBoard gb = disB(gc);
    
       if(remoteViewer>=0)
       {	CELLSIZE = getWidth()/10;
    	 drawHiddenWindow(gc,selectPos,remoteViewer,getBounds());  
       }
       else
       {
       CrosswordsState state = gb.getState();
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
       
       GC.setRotatedContext(gc,logRect,selectPos,effectiveBoardRotation);
       boolean big = bigTypeToggle.isOn();
       if(big) { bigTypeToggle.setValue(false); }
       gameLog.playerIcons = true;
       gameLog.redrawGameLog2(gc, nonDragSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),standardBoldFont());
       if(big) { bigTypeToggle.setValue(true); }
       GC.unsetRotatedContext(gc,selectPos);
       
       GC.frameRect(gc, Color.black, logRect);
       // this does most of the work, but other functions also use contextRotation to rotate
       // animations and sprites.
       boolean planned = plannedSeating();
     
       int who = gb.whoseTurn;
       commonPlayer pl = getPlayerOrTemp(who);
       double messageRotation = pl.messageRotation();
       {    
	   GC.setRotatedContext(gc,largerBoardRect,selectPos,effectiveBoardRotation);
       standardGameMessage(gc,gb,stateRect,state);
       getPlayerIcon(who).drawChip(gc,this,iconRect,null);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,selectPos);
       redrawChat(gc,selectPos);
       drawOptions(gc,((state==CrosswordsState.Puzzle)
    		   			||(state==CrosswordsState.ConfirmFirstPlay)
    		   			||(state==CrosswordsState.FirstPlay)
    		   			||((state==CrosswordsState.Play) && robotGame && bb.moveNumber<=2)
    		   			) ? ourTurnSelect :null,selectPos,gb);

       String msg = bb.invalidReason==null ? s.get(CrosswordsVictoryCondition) : s.get(bb.invalidReason);
       String goalmsg = bb.invalidReason==null ? GoalExplanation : InvalidExplanation;
       goalAndProgressMessage(gc,nonDragSelect,Color.black,msg,progressRect, goalRect,goalmsg);
       if(planned) 
       	{ StockArt.Rotate180.drawChip(gc, this,rotateRect, selectPos, CrosswordsId.Rotate,s.get(RotateMessage)); 
       	  CrosswordsChip chip = lockOption ? CrosswordsChip.UnlockRotation : CrosswordsChip.LockRotation;
       	  chip.drawChip(gc, this,lockRect, selectPos, CrosswordsId.Lock,s.get(chip.tip)); 
       	}
       drawNoChat(gc,altNoChatRect,selectPos);
       GC.unsetRotatedContext(gc,selectPos);
       }
       drawDrawPile(DRAWBACKGROUNDTILES?null:gc,ourTurnSelect,gb);
       for(int player=0;player<bb.players_in_game;player++)
       	{ commonPlayer pl1 = getPlayerOrTemp(player);
       	  pl1.setRotatedContext(gc, selectPos,false);
       	   GC.setFont(gc,standardBoldFont());
    	   DrawChipPool(gc, chipRects[player],eyeRects[player],pl1, ourTurnSelect,selectPos,gb);
    	   DrawScore(gc,scoreRects[player],pl1,ourTurnSelect,gb);
    	   if(planned && gb.whoseTurn==player)
    	   {
    		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
    	   GC.setFont(gc, largeBoldFont());
    	   drawNotice(gc,noticeRects[player],gb);
       	   pl1.setRotatedContext(gc, selectPos,true);
       	}
       if(!planned)
      	{  
    	   // generally prevent spectators seeing tiles, unless openracks or gameover
    	   int whop = isPassAndPlay() ? gb.whoseTurn : getActivePlayer().boardIndex;
    	   boolean censorRack = rackCensored(gb,whop);
    	   drawRack(gc,gb,bigRack,gb.getPlayerRack(whop),gb.getPlayerMappedRack(whop),
    			   gb.getRackMap(whop),gb.getMapPick(whop),
    			   	censorRack,
    			   	censorRack ? null : selectPos,	// always allow rearranging the primary rack
    			   	ourTurnSelect==null); 
    	   if(isPassAndPlay() && !explicitlyVisible(gb,whop) && (currentGuiPlayer().boardIndex==whop))
    	   {   Rectangle rackEye = new Rectangle(G.Left(bigRack),G.Top(bigRack),G.Height(bigRack)/3,G.Height(bigRack)/3);
    		   StockArt.Eye.drawChip(gc,this,rackEye,selectPos,CrosswordsId.RevealRack,SeeYourTilesMessage);
    	   }

      	}
     
       GC.setFont(gc,standardBoldFont());
       

       if (state != CrosswordsState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{
				boolean done = gb.DoneState();
				handleDoneButton(gc,messageRotation,doneRect,(done ? buttonSelect : null),HighlightColor, rackBackGroundColor);		
				}
			drawNotice(gc,noticeRect,gb);
			
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
			if(state==CrosswordsState.Play 
					&& (buttonSelect!=null)
					&& gb.notStarted())
					{
					passButton.show(gc, messageRotation, buttonSelect);
					}
        }
       
		if(G.debug()||allowed_to_edit)
		{ 	checkWordsButton.show(gc,messageRotation,selectPos);
			vocabularyRect.draw(gc, selectPos);
		}

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==CrosswordsState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        
        
             //      DrawRepRect(gc,pl.displayRotation,Color.black,b.Digest(),repRect);
        
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        
        if(bigString!=null)
        {	GC.setFont(gc,largeBoldFont());
        	GC.drawBubble(gc,bigX,bigY,bigString,fullRect,messageRotation);
        }
       }
       bigTypeToggle.draw(gc,selectPos);
       drawHiddenWindows(gc, selectPos);
    }
    public void standardGameMessage(Graphics gc,CrosswordsBoard gb,Rectangle stateRect,CrosswordsState state)
    {
        standardGameMessage(gc,
   				state==CrosswordsState.Gameover?gameOverMessage(gb):s.get(state.description()),
   				state!=CrosswordsState.Puzzle,
   				gb.whoseTurn,
   				stateRect);

    }
    public boolean allowResetUndo() { return(false); }
    public boolean canSendAnyTime(commonMove m)
    {
    	return super.canSendAnyTime(m)
    			|| (m.op==MOVE_SHOW);
    }
    public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode replay)
    {
	   	 if(m.op==MOVE_DROPONRACK)
	   	 {
	   		 // this is the point where a remote RPC screen thinks a tile
	   		 // is being dropped from the main screen.  Unfortunately it
	   		 // may have already been dropped by a bounce or other action
	   		 // on the main screen.
	   		 if((remoteViewer<0) && (bb.pickedObject!=null))
	   		 {	transmit = true;
	   		 	m.op=MOVE_DROP;
	   		 }
	   	 }
  	
    	return(super.PerformAndTransmit(m,transmit,replay));
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
        startBoardAnimations(replay,bb.animationStack,CELLSIZE,MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay.animate) { playSounds((Crosswordsmovespec)mm); }
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
//        if(replay.animate)
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

 void playSounds(Crosswordsmovespec mm)
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
        return (new Crosswordsmovespec(st, pl));
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
    	  case MOVE_REPLACE:
    	  case MOVE_REMOTELIFT:
    	  case MOVE_DROPONRACK:
    	  case MOVE_REMOTEDROP:
    	  case MOVE_CANCELLED:
    		  return(null);
    	  default: break;
    	  }

    	  commonMove rval = EditHistory(nmove,false);
     	     
    	  return(rval);
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
      // public void verifyGameRecord()
      // {	super.verifyGameRecord();
      // }
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
        if (hp.hitCode instanceof CrosswordsId)// not dragging anything yet, so maybe start
        {
        CrosswordsId hitObject =  (CrosswordsId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
        case Rack:
        case LocalRack:
        case RemoteRack:
    		{
    			sendRack(hp);
        	}
    		break;
        case BoardLocation:
	        CrosswordsCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    	break;
        } 
        }
    }
    private void sendRack(HitPoint hp)
    {	CrosswordsId hitObject =  (CrosswordsId)hp.hitCode;
    		// drawing the rack prepares the move
            String msg = (String)hp.hitObject;
            // transmit only drop from the board, not shuffling of the rack
            boolean transmit = (hitObject==CrosswordsId.Rack) 
					|| ((bb.whoseTurn==remoteViewer) && (hitObject==CrosswordsId.RemoteRack));
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
    
    private void showWords(WordStack ws,HitPoint hp,String msg)
    {
    	StringBuilder words = new StringBuilder();
    	G.append(words,msg,"\n");
    	for(int lim=ws.size(),i=0;i<lim;i++)
    		{
    		Word w = ws.elementAt(i);
    		CrosswordsCell seed = w.seed;
    		G.append(words, w.name," @",seed.col,seed.row," ",w.points," points\n");
    		}
    	bigX = G.Left(hp);
    	bigY = G.Top(hp);
    	bigString = words.toString();
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

       	if(!(id instanceof CrosswordsId))  
       		{   missedOneClick = performStandardActions(hp,missedOneClick);
       			definitionCell = null;
       		}
        else {
        missedOneClick = false;
        CrosswordsId hitCode = (CrosswordsId)id;
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
        case LocalRack:
        case RemoteRack:
        	sendRack(hp);
        	break;
        case BigfontOn:
        	bigTypeToggle.setValue(true);
        	break;
        case BigfontOff:
        	bigTypeToggle.setValue(false);
        	break;
        case Vocabulary:
        	bb.setVocabulary(vocabularyRect.value);
        	break;
        case Definition:
        	// getCell was already done
        	definitionCell = hitCell(hp);
        	break;
        case CheckWords:
        	bb.setVocabulary(vocabularyRect.value);
        	showWords(bb.checkLikelyWords(),hp,s.get(WordsMessage));
        	break;
        case Lock:
        	lockOption = !lockOption;
        	break;
        case Rotate:
        	effectiveBoardRotation = (effectiveBoardRotation+Math.PI/2);
        	boardRotation = (boardRotation+1)%4;
        	contextRotation = 0;
        	generalRefresh();
        	break;
        case RevealRack:
    		mainRackVisibleFor = mainRackVisibleFor<0 ? bb.whoseTurn : -1;
    		break;
        case EyeOption:
        	{
         	String op = (String)hp.hitObject;
         	int rm = remoteWindowIndex(hp);
        	PerformAndTransmit((rm<0 ? "show ":"see ")+op);
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
        	CrosswordsChip ch = (CrosswordsChip)hp.hitObject;
        	PerformAndTransmit("SetBlank "+ch.letter);
        	}
        	break;
        case Rack:
    		{
    		// drawing the rack prepares the move
            String msg = (String)hp.hitObject;
            // transmit only drop from the board, not shuffling of the rack
            PerformAndTransmit(msg,hitCode==CrosswordsId.Rack,replayMode.Live);
        	}
        	
        	break;
        case DrawPile:
        case EmptyBoard:
        	{
        		CrosswordsCell hitObject = hitCell(hp);
        		if(bb.pickedObject==null)
            	{	CrosswordsCell c = getPickedRackCell(hp);
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
        		else if(hitCode==CrosswordsId.DrawPile)
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
            CrosswordsCell hitObject = hitCell(hp);
 			PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
        	}
			break;
			
        }
        }
    }


    private boolean setDisplayParameters(CrosswordsBoard gb,Rectangle r)
    {
      	boolean complete = false;
      	// the numbers for the square-on display are slightly ad-hoc, but they look right
      	gb.SetDisplayParameters(1, 1.0, 0,0,0); // shrink a little and rotate 60 degrees
       	gb.SetDisplayRectangle(r);
      	if(complete) { generalRefresh(); }
      	return(complete);
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
    public String sgfGameType() { return(Crosswords_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int np = his.intToken();	// players always 2
    	long rv = his.intToken();
    	int rev = his.intToken();	// rev does't get used either
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
    {  return(new CrosswordsPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary 2025/07/28
     * 626 files visited 0 problems after pruning about 30 early games
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

    /*
     * support for hidden windows in pass-n-play mode
     * */
    public String nameHiddenWindow()
    {	
    		return(ServiceName);
    }
    public void adjustPlayers(int n)
    {
        int HiddenViewWidth = 800;
        int HiddenViewHeight = 300;

    	super.adjustPlayers(n);
        if(RpcService.isRpcServer() || VNCService.isVNCServer() || G.debug())
        {
        createHiddenWindows(n,HiddenViewWidth,HiddenViewHeight);
        }
    }

    public void drawHiddenWindow(Graphics gc,HitPoint hp,int index,Rectangle bounds)
    {	
    	int margin = G.minimumFeatureSize()/4;
    	int w = G.Width(bounds)-margin*2;
    	int h = G.Height(bounds)-margin*2;
    	int l = G.Left(bounds)+margin;
    	int t = G.Top(bounds)+margin;
    	int step = h/8;
    	CrosswordsCell rack [] = bb.getPlayerRack(index);
    	if(rack!=null)
    	{
    	CrosswordsCell prack[] =bb.getPlayerMappedRack(index);
    	int hiddenRack[] = bb.getRackMap(index);
    	int hiddenMapPick = bb.getMapPick(index);
    	Rectangle rackRect = new Rectangle(l,t+h/2,w,(int)(step*3.25));
    	Rectangle eyeRect = new Rectangle(l,t+step*2,step,step);
    	Rectangle whoRect = new Rectangle(l+step*2,t+step*2,w-step*4,step*2);
    	Rectangle stateRect = new Rectangle(l,t,w,step);
    	Rectangle turnnotice = new Rectangle(l,t+step,w,step);
    	Rectangle notice = new Rectangle(l,t+step,w/4,step);
    	if (remoteViewer<0)
    		{ StockArt.Scrim.image.stretchImage(gc, bounds);
    		}
    	Font myfont = SystemFont.getFont(largeBoldFont(), step/2);
    	GC.setFont(gc, myfont);
    	GC.Text(gc, true, whoRect, Color.black, null, s.get(ServiceName,prettyName(index)));
    	CrosswordsState state = bb.getState();
    	GC.setFont(gc, myfont);
    	standardGameMessage(gc,bb,stateRect,state);
    	boolean hide = bb.hiddenVisible[index];
    	drawRack(gc,bb,rackRect,rack,prack,hiddenRack,hiddenMapPick,hide,hp,bb.whoseTurn!=index);
    	GC.setFont(gc, myfont);
    	drawNotice(gc,notice,bb);
    	drawEye(gc,bb,eyeRect,hide,hp,index);
    	
    	GC.setFont(gc, myfont);
		if(bb.whoseTurn==index)
			{
			 GC.Text(gc, true, turnnotice,
			Color.red,null,s.get(YourTurnMessage));
			}
    	}
    }

}

