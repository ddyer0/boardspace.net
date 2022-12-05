package jumbulaya;

import static jumbulaya.Jumbulayamovespec.*;

import online.common.*;
import java.util.*;

import com.codename1.ui.Font;
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
import lib.Slider;
import lib.StockArt;
import lib.TextButton;
import lib.Random;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.game.sgf.sgf_reader;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import vnc.VNCService;



/**
 *  Initial work Sept 2020 
*/
public class JumbulayaViewer extends CCanvas<JumbulayaCell,JumbulayaBoard> implements JumbulayaConstants, GameLayoutClient
{	static final long serialVersionUID = 1000;

	
	static final String Jumbulaya_SGF = "jumbulaya"; // sgf game name

	// file names for jpeg images and masks
	static final String ImageDir = "/jumbulaya/images/";

	boolean DRAWBACKGROUNDTILES = true;
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(235,235,235);
    private Color rackBackGroundColor = new Color(192,192,192);
    private Color boardBackgroundColor = new Color(220,165,155);
    private Color newLetterColor = new Color(0.25f,0.25f,1.0f);
    private Color tempLetterColor = new Color(0.1f,0.5f,0.1f);
    private Dictionary dictionary = Dictionary.getInstance();
    
    // private state
    private JumbulayaBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addZoneRect("chip",4);    
    private Rectangle startJrects[] = addZoneRect("jrect",4);
    private Rectangle scoreRects[] = addZoneRect("score",4);
    private Rectangle eyeRects[] = addZoneRect("eye",4);
    private Rectangle drawPileRect = addRect("drawPileRect");
    private Rectangle timeRect = addRect("timecontrol");
    private Rectangle rotateRect = addRect("rotate");
    private boolean lockOption = false;
    private Rectangle lockRect = addRect("lock");
    private Rectangle largerBoardRect = addRect(".largerBoard");
    private Rectangle altNoChatRect = addRect("nochat");
    private TextButton checkWordsButton = addButton(JustWordsMessage,JumbulayaId.CheckWords,
    		JustWordsHelp,
						HighlightColor, rackBackGroundColor);
    private Slider vocabularyRect = new Slider(VocabularyMessage,JumbulayaId.Vocabulary,0,1,
    		JumbulayaPlay.vocabularyPart(online.search.RobotProtocol.DUMBOT_LEVEL));
    private TextButton checkJumbulayaButton = addButton(JumbulayasMessage,JumbulayaId.CheckJumbulayas,
    		CheckJumbulayaHelp,
			HighlightColor, rackBackGroundColor);
    private TextButton startJRect = addButton(StartJumbulaya,JumbulayaId.StartJumbulaya,
    		EndExplanation,
    		HighlightColor, rackBackGroundColor);
    private TextButton endJRect = addButton(EndJumbulaya,JumbulayaId.EndJumbulaya,
    		AbandonExplanation,
    		HighlightColor, rackBackGroundColor);
    int boardRotation = 0;
    double effectiveBoardRotation = 0.0;
    private Rectangle bigRack = addZoneRect("bigrack");
    private Rectangle iconRect = addRect("icon");
    private TextButton passButton = addButton(PASS,GameId.HitPassButton,ExplainPass,
			HighlightColor, rackBackGroundColor);
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	JumbulayaChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = JumbulayaChip.Icon.image;
    }
    public int ScoreForPlayer(commonPlayer p)
    {
    	return(bb.score[p.boardIndex]);
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
        	JumbulayaConstants.putStrings();
        }
        MouseColors = new Color[]{Color.red,Color.green,Color.blue,Color.yellow};
        MouseDotColors = new Color[]{ Color.white,Color.white,Color.white,Color.black};
     
        String type = info.getString(OnlineConstants.GAMETYPE, JumbulayaVariation.Jumbulaya.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new JumbulayaBoard(type,players_in_game,randomKey,getStartingColorMap(),dictionary,JumbulayaBoard.REVISION);
        // some problems with the animation
        // useDirectDrawing();
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
    /** this is called by the game controller when all players have connected
     * and the first player is about to be allowed to make his first move. This
     * may be a new game, or a game being restored, or a player rejoining a game.
     * You can override or encapsulate this method.
     */


    public void setLocalBounds(int x,int y,int w,int h)
    {	rackSize = plannedSeating()?3:1;
    	int fh = standardFontSize();
    	do {
        	setLocalBoundsV(x,y,w,h,new double[] {0.7,-0.7});
    	
    	G.print("Racksize "+rackSize+" scale "+selectedLayout.selectedCellSize()/fh," ",fh+" "+selectedLayout.selectedSeating());
        	int width = G.Width(boardRect);
            if(selectedLayout.selectedCellSize()>=fh*2)
            {
    		int height = G.Height(boardRect);
    		int dim = Math.min(width,height);
    		int adim = Math.min(w,h);
    		if(dim>adim*0.75) { break; }
            }
    		rackSize -= 0.25;
    	} while(rackSize>=1.2);
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
    	int minLogW = fh*22;	
       	int minChatW = fh*35;	
       	int vcrw = fh*16;
        int margin = fh/2;
        int buttonW = 8*fh;
        int stateH = fh*4;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			aspect,	// aspect ratio for the board
    			fh*2,	// min cell size
    			fh*3,	// maximum cell size
    			0.2		// preference for the designated layout, if any
    			);
       	boolean planned = plannedSeating();
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChat(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2);
    	layout.placeRectangle(logRect,minLogW, minLogW, minLogW*3/2, minLogW*3/2,BoxAlignment.Edge,true);
    	layout.alwaysPlaceDone = false;
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,planned?null:startJRect);
       	G.copy(endJRect,startJRect);
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);
       	int doneW = G.Width(editRect);
       	layout.placeRectangle(passButton, doneW,doneW/2,BoxAlignment.Center);
    	layout.alwaysPlaceDone = G.debug();
       	layout.placeDoneEditRep(doneW*3/2,doneW*3/2,checkWordsButton, checkJumbulayaButton,vocabularyRect);
       	commonPlayer pl = getPlayerOrTemp(0);
       	int spare = G.Height(pl.playerBox)/2;
       	layout.placeRectangle(drawPileRect,spare,spare,BoxAlignment.Center);
       	       	
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// There are two classes of boards that should be rotated. For boards with a strong
    	// "my side" orientation, such as chess, use seatingFaceToFaceRotated() as
    	// the test.  For boards that are noticably rectangular, such as Push Fight,
    	// use mainW<mainH
    	int brows = bb.nrows;
    	int bcols = bb.ncols;
        int ncols =  bcols;
    	int nrows =  planned ? brows : brows+2;
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH*2)/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int largeW = CELLSIZE*ncols;
    	int largeH = CELLSIZE*nrows+stateH;
    	int boardW = (int)(bcols*CELLSIZE);
    	int boardH = (int)(brows*CELLSIZE+stateH);
    	int extraW = Math.max(0, (mainW-largeW)/2);
    	int extraH = Math.max(0, (mainH-largeH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH/2;
        int stateX = boardX;
    	G.placeStateRow(stateX,stateY,boardW ,stateH/2,iconRect,stateRect,rotateRect,lockRect,altNoChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	G.SetRect(goalRect, boardX, G.Bottom(boardRect)-stateH/4,boardW,stateH/2);   
    	G.SetRect(timeRect, boardX, G.Bottom(goalRect),boardW,timeControl().timeControlMessage()==null ? 0 : stateH/2);   
    	G.SetRect(bigRack, boardX+CELLSIZE/2, G.Bottom(timeRect), boardW-CELLSIZE, planned?0:CELLSIZE*3/2);
    	G.SetRect(largerBoardRect,mainX+extraW,mainY+extraH,largeW,largeH);

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        labelFont = largeBoldFont();
        return(boardW*boardH+rackSize*layout.selectedCellSize()*boardW);
    }
    boolean vertical = false;
    double rackSize = 1;
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle score = scoreRects[player];
    	Rectangle eye = eyeRects[player];
    	Rectangle jrect = startJrects[player];
    	int scoreW = unitsize*3;
    	int scoreH = unitsize*3;
    	G.SetRect(score,x,y+unitsize*2,scoreW,scoreH);
    	Rectangle box =  pl.createRectangularPictureGroup(x+scoreW-unitsize,y,unitsize);
    	Rectangle done = doneRects[player];
     	boolean planned = plannedSeating();
    	int doneW = planned ? unitsize*4 : 0;
    	int donel = G.Right(box);
    	int top =  G.Top(box);
    	int dtop = top+unitsize/4;
    	int donex = donel+unitsize/2;
    	G.SetRect(done,donex,dtop,doneW,doneW/2);
    	G.SetRect(jrect,donex,dtop+doneW*2/3,doneW*2,doneW*2/3);
    	int dright = donex+doneW*2;
    	G.SetRect(eye, dright-doneW/2,dtop,doneW/2,doneW/2);
    	G.union(box, done,score,eye,jrect);
    	int boxW = G.Width(box);
    	int chipH = unitsize*3/2+(planned ? unitsize*2 : 0);
    	
       	if(vertical) { G.SetRect(chip,	x,	G.Bottom(box),	(int)(boxW*rackSize),(int)(chipH*rackSize)); }
       	else { 
       		G.SetRect(chip,G.Right(box)+doneW/4,y+unitsize/2,(int)(chipH*rackSize*5),(int)(chipH*rackSize));
       	}
        G.union(box, chip);
    	pl.displayRotation = rotation;
    	return(box);
    }
    private void drawDrawPile(Graphics gc,HitPoint highlight,JumbulayaBoard gb)
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
      		highlight.hitCode=JumbulayaId.DrawPile;
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
    	JumbulayaCell last = gb.lastDropped();
    	for(int i=0;i<tilesLeft;i++)
    	{
    		int dx = l+rand.nextInt(w);
    		int dy = t+rand.nextInt(h);
    		boolean hide = !((i==tilesLeft-1) && (last==gb.drawPile));
    		if(gb.drawPile.chipAtIndex(i).drawChip(gc, this, canHit?highlight:null, JumbulayaId.DrawPile,cs,dx,dy,
    				hide ? JumbulayaChip.BACK : null))
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
    private void DrawChipPool(Graphics gc, Rectangle r,Rectangle er, commonPlayer pl, HitPoint highlight,HitPoint highlightAll,JumbulayaBoard gb)
    {	int pidx = pl.boardIndex;
    	Rectangle rack = chipRects[pidx];
    	commonPlayer ap = getActivePlayer();
       	 
    	if(G.offline() || (pl==ap))
    	{
    		drawEye(gc,gb,er,gb.openRack[pidx],highlightAll,pl.boardIndex);
    	}
    	JumbulayaCell prack[] = gb.getPlayerRack(pidx);
    	if(prack!=null)
    	{
       	if(allowed_to_edit || ap==pl) { for(JumbulayaCell c : prack) { c.seeFlyingTiles=true; }}
       	boolean open = gb.openRack[pidx];
       	boolean showTiles = open || allowed_to_edit || bb.openRacks; 
       	boolean anyRack = G.offline() || allowed_to_edit || (ap==pl);
       	drawRack(gc,gb,rack,prack,gb.getPlayerMappedRack(pidx),
       			gb.getRackMap(pidx),gb.getMapPick(pidx),!showTiles,anyRack ? highlightAll : null,!anyRack);  	
    	}}
    
    private void drawEye(Graphics gc,JumbulayaBoard gb,Rectangle er,boolean showing,HitPoint highlightAll,int who)
    {
   		StockArt chip = showing ? StockArt.NoEye : StockArt.Eye;
   		String help = s.get(chip==StockArt.Eye ? ShowTilesMessage : HideTilesMessage);
		if(chip.drawChip(gc, this, er, highlightAll, JumbulayaId.EyeOption,help))
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
     * @param mappedRack 
     * @param cells
     * @param map
     * @param picked
     * @param censor
     * @param highlight
     * @param rackOnly
     */
    private void drawRack(Graphics gc,JumbulayaBoard gb, Rectangle rack,
    		JumbulayaCell[]cells,
    		JumbulayaCell[]mappedCells,
    		int map[],int picked,boolean censor,HitPoint highlight,boolean rackOnly)
    {
    	int h = G.Height(rack);
    	int w = G.Width(rack);
    	int cy = G.centerY(rack);
    	int nsteps = map.length;
    	int xstep = (int)Math.min(w/(nsteps+0.5),h*3/4); 
    	int tileSize = (int)(xstep*1.1);
    	int cx = G.Left(rack)+(w-xstep*nsteps)/2+xstep/2;
       	GC.frameRect(gc, Color.black, rack);

       //.print("");
       	// for remote viewers, always use this size
       	if(remoteViewer>=0) { CELLSIZE = tileSize; }
       	/* this was useful while debugging rack manipulation
       	if(false) // G.debug())
       	{
       	int cx0 = cx;
     	for(int idx = 0;idx<nsteps;idx++)
   		{
    	JumbulayaCell d = cells[idx];
       	if(d!=null) 
       	{
      	setLetterColor(gc,gb,d);
      	//print("Draw "+idx+" "+c+" "+top+" @ "+cx);
      	JumbulayaChip dtop = d.topChip();
      	if(dtop!=null) { d.drawChip(gc, this, dtop, tileSize*2/3, cx0, cy+tileSize*2/3, null);}
      	cx0 += xstep;
       	}}}
	*/
    	for(int idx = 0;idx<nsteps;idx++)
		{
    	int mapValue = map[idx];
    	JumbulayaCell c = mapValue>=0 ? cells[mapValue] : null;
    	JumbulayaCell mc = mappedCells[idx];
    	JumbulayaChip top = c==null ? null : c.topChip();
    	mc.reInit();
    	if(top!=null) { mc.addChip(top); mc.fromRack = c.fromRack; }
    	
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
    	mc.drawChip(gc, this, top, tileSize, cx, cy, censor ? JumbulayaChip.BACK : null);
       	if(c!=null) { c.copyCurrentCenter(mc);	}// set the center so animations look right
       	}

    	if(canDrop && top==null)
    	{
    		StockArt.SmallO.drawChip(gc,this,tileSize,cx,cy,null);
    		
    	}    	
     	if((canPick||canDrop) && G.pointInRect(highlight, cx-tileSize/2,cy-tileSize/2,tileSize,tileSize))
    	{
   			highlight.hit_x = cx;
    		highlight.hit_y = cy;
			highlight.spriteColor = Color.red;
			highlight.awidth = tileSize;
    		if(remoteDrop)
    		{	c = null;
    			// it's very important that the choice of the destination cell
    			// is always the same, and unrelated to the UI cell the user
    			// touched.  When side screens are in use, the tile can be dropped
    			// on both screens if the user was attempting a 2-handed gesture,
    			// dropping the tile on the main board and picking up the next tile
    			// on the side screen.
    		    for(int i=0;c==null && i<cells.length;i++) 
    				{ if(cells[i].topChip()==null) { c = cells[i]; }
    				}
    		    if(c!=null)
    		{
    		    if(gb.pickedFromRack)
    		    {
    		    	JumbulayaCell d = gb.pickedSourceStack.top();
    		    	if(d!=null && !d.onBoard && d.topChip()==null)
    		    	{
    		    		c = d;
    		    	}
    		    }
    			highlight.hitObject = G.concat("droponrack ",c.col," ",c.row," ", idx);	
    			highlight.hitCode = JumbulayaId.Rack ;
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
    			highlight.hitCode = JumbulayaId.RemoteRack ;
    		}
    		else if(localDrop )
    		{	
    			highlight.hitObject = G.concat("replace Rack ",myCol," ",idx);
    			highlight.hitCode = JumbulayaId.LocalRack;
    			
    		}
    		else
    		{   highlight.hitObject = G.concat("lift Rack ",myCol," ",idx," ",map[idx]);
    			highlight.hitCode = JumbulayaId.LocalRack;
    		}
			
    	}
		cx += xstep;
		}

    }

    private void DrawScore(Graphics gc, Rectangle r, commonPlayer pl, HitPoint highlight,JumbulayaBoard gb)
    {	int pidx = pl.boardIndex;
    	//int val = gb.score[pidx];
    	JumbulayaCell c = gb.getPlayerCell(pidx);
    	int score = gb.score[pidx];
    	
    	if(c.drawStack(gc, this, r,(gb.getState()==JumbulayaState.Puzzle) ? highlight:null,1,1,null))
    	{
    		highlight.spriteColor = Color.red;
    		highlight.spriteRect = r;
    	}
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,true,r,c.topChip()==JumbulayaChip.Yellow?Color.black:Color.white,null,""+score);
    }
    /**
     * return the dynamically adjusted size during an animation.  This allows
     * compensation for things like the zoom level of the board changing after
     * the animation is started.
     */
   // public int activeAnimationSize(Drawable chip,int thissize) 	{ 	 return(thissize);	}
    private JumbulayaCell getMovingTile(int ap)
    {
    	int picked = bb.getMapTarget(ap);
    	if(picked>=0) { 
    		JumbulayaCell[] prack = bb.getPlayerRack(ap);
    		if(prack==null) { return null;}
    		if(picked>=0 && picked<prack.length)
    		{
    			return prack[picked];
    		}
    	}
    	return(null);
    }
    private JumbulayaCell getPickedRackCell(HitPoint highlight)
    {
    	int rm = remoteWindowIndex(highlight);
    	if(rm>=0)
    	{	JumbulayaCell c = getMovingTile(rm);
    		if(c!=null) { return(c); }
    	}
    	else
    	{
    	{int ap = allowed_to_edit||G.offline() ? bb.whoseTurn : getActivePlayer().boardIndex;
    	 JumbulayaCell c = getMovingTile(ap);
    	 if(c!=null) { return(c); }
    	}
 
    	if(allowed_to_edit || G.offline())
    	{	commonPlayer pl = inPlayerBox(highlight);
    		if(pl!=null)
    			{JumbulayaCell c = getMovingTile(pl.boardIndex);
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
    {	JumbulayaCell picked = getPickedRackCell(highlight);
    	if(picked!=null)
    	{
    		JumbulayaChip top = picked.topChip();
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
       	JumbulayaChip ch = JumbulayaChip.getChip(obj);
       	if(ch!=null)
       		{
       		// hidden windows have x coordinates that are negative, we don't want to rotate tiles
       		// being displayed on hidden windows
       		g.setColor(Color.black);
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
     JumbulayaChip.backgroundTile.image.tileImage(gc, fullRect);   
      GC.setRotatedContext(gc,largerBoardRect,null,effectiveBoardRotation);
      drawFixedBoard(gc,boardRect);
      GC.unsetRotatedContext(gc,null);  
      if(remoteViewer<0 && DRAWBACKGROUNDTILES)
      {		GC.setRotatedContext(gc,drawPileRect,null,effectiveBoardRotation);
    		drawDrawPile(gc,null,bb);
    		GC.unsetRotatedContext(gc,null);  
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
         JumbulayaChip.backgroundReviewTile.image.tileImage(gc,brect);   
        }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(bb,brect);
	      // if the board is one large graphic, for which the visual target points
	      // are carefully matched with the abstract grid
	  	if(remoteViewer<0)
	  	{
	  	  JumbulayaChip.Board.getImage().centerImage(gc, brect);

	      // draw a picture of the board. In this version we actually draw just the grid
	      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
	      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
	      // on the board to fine tune the exact positions of the text
	      bb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

	      if(DRAWBACKGROUNDTILES) 
	    	  { drawFixedTiles(gc,brect,bb);
	    	  }
	  	}
	//      draw
    }
    
    static long FIXEDRANDOM = 25661463;
    //
    // in this game, most of the letters on the board and all of the drawpile
    // will change very slowly if at all.  We mark appropriate cells as "fixed"
    // and draw those with the background.  On PCs this is hardly noticable, 
    // but on mobiles if makes a big difference.
    // This digest determines when the background has changed, and needs to be redrawn.
    public long digestFixedTiles()
    {	Random r = new Random(FIXEDRANDOM);
    	long v = 0;
    	for(JumbulayaCell cell = bb.allCells; cell!=null; cell=cell.next)
        {	if(cell.fromRack)
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
    public void drawFixedTiles(Graphics gc,Rectangle brect,JumbulayaBoard gb)
    {	Random r = new Random(FIXEDRANDOM);
    	long v = 0;
    	pendingFullRefresh = !spritesIdle();
    	Enumeration<JumbulayaCell>cells = gb.getIterator(Itype.RLTB);
    	while(cells.hasMoreElements())
        {	JumbulayaCell cell = cells.nextElement();
        if(cell.fromRack)
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
    
public void setLetterColor(Graphics gc,JumbulayaBoard gb,JumbulayaCell cell)
{	if(gc!=null)
	{
    JumbulayaChip ch = cell.topChip();
    if(ch!=null)
    {
    Color col = cell.getSelected() 
    			? Color.green 
    			: (cell.onBoard == cell.fromRack)  
    				?  tempLetterColor
    				: (cell.onBoard && cell.row == gb.previousRow)
    					? newLetterColor 
    					: Color.black ;
	labelColor = col;
	GC.setColor(gc,col);
    }
    }
}
	JumbulayaCell definitionCell = null;
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, JumbulayaBoard gb, Rectangle brect, HitPoint highlight,HitPoint all)
    {	
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
        // draw the claimed row markers
        JumbulayaCell claimed[] = gb.claimed;
        boolean hitClaim = false;
        for(JumbulayaCell cell : claimed)
        {
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            //StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            if(cell.drawStack(gc,this,all,CELLSIZE,xpos,ypos,1,1,null))
            {
            	hitClaim = true;
            	StringBuilder wordlist = new StringBuilder();
            	Word cw = gb.getCurrentWord(cell.row);
            	int ntiles = cw.nTiles;
            	int nw = 0;
            	while(cw!=null && cw.points>0) 
            	{	nw++;
            		wordlist.append(cw.name);
            		wordlist.append("\n");
            		if(cw.nTiles<ntiles) { cw = null; }
            		else { cw = cw.predecessor; }
            	}   
           		if(nw>0) { all.setHelpText(wordlist.toString()); }
           	 
            }
            if(cell.row==gb.activeRow)
            {	JumbulayaChip chip = gb.getPlayerChip(gb.whoseTurn);
            	chip.drawChip(gc,this,CELLSIZE*4/5,xpos-CELLSIZE/3,ypos,null);
            }
        }


        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        JumbulayaCell closestCell = hitClaim ? null : gb.closestCell(all,brect);
        boolean moving = getOurMovingObject(highlight)>=0;
        boolean hitCell = (highlight!=null) && gb.LegalToHitBoard(closestCell,moving);
        if(hitCell)
        { // note what we hit, row, col, and cell
          boolean empty = closestCell.isEmpty();
          boolean picked = (gb.pickedObject!=null);
          highlight.hitCode = (empty||picked) ? JumbulayaId.EmptyBoard : JumbulayaId.BoardLocation;
          highlight.hitObject = closestCell;
          highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
          highlight.awidth = CELLSIZE;
        }
        if(closestCell!=null && all!=null && !gb.isDest(closestCell))
        {	for(int lim=gb.words.size()-1; lim>=0; lim--)
        	{
        	Word word = gb.words.elementAt(lim);
        	if(word.seed==closestCell)
        	{	all.hitCode = JumbulayaId.Definition;
        		all.hitObject = closestCell;
        		all.setHelpText(s.get(GetDefinitionMessage,word.name));
        	}
        	}
        	if(definitionCell!=closestCell) { definitionCell = null; }
        }
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        {
        Enumeration<JumbulayaCell>cells = gb.getIterator(Itype.RLTB);
		while(cells.hasMoreElements())
          { JumbulayaCell cell = cells.nextElement();
            boolean drawhighlight = (hitCell && (cell==closestCell));
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            
            if (drawhighlight)
             { // checking for pointable position
            	 StockArt.SmallO.drawChip(gc,this,gb.cellSize()*5,xpos,ypos,null);                
             }
            if(!cell.fromRack || !DRAWBACKGROUNDTILES)
            {
            setLetterColor(gc,gb,cell);
            cell.drawStack(gc,this,null,CELLSIZE,xpos,ypos,1,1,null);
            }
            // StockArt.SmallO.drawChip(gc, this, CELLSIZE,xpos,ypos,null); 
            //if(G.debug() && (gb.endCaps.contains(cell) || gb.startCaps.contains(cell)))
            //	{ StockArt.SmallO.drawChip(gc, this, CELLSIZE,xpos,ypos,null); 
            //	}
            }
        }
        
        if(definitionCell!=null)
        {
        	drawDefinition(gc,gb,all);
        }
    }
    
    private String definitionString()
    {	boolean prev = bb.activeRow<0;
    	Word e = prev ? bb.previousWord : bb.pendingWord;
    	if(e!=null)
    	{	Entry ee = Dictionary.getInstance().get(e.name);
    		if(ee!=null)
    		{
    		return(s.get(prev 
    					? e.isJumbulaya() ? JumbulayaMessage : PrevWordMessage 
    					: PendingWordMessage ,ee.word,ee.getDefinition()));
    	}
    	}
    	return(s.get(JumbulayaVictoryCondition));
    }
    
    public void drawDefinition(Graphics gc,JumbulayaBoard gb,HitPoint hp)
    {
    	JumbulayaCell target = definitionCell;
    	StringBuilder message = new StringBuilder();
    	WordStack words = gb.words;
    	FontMetrics fm = G.getFontMetrics(standardPlainFont());
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

    public boolean canStartJumbulaya(JumbulayaBoard gb,int forPlayer)
    {	JumbulayaState state = gb.getState();
    	return (((state==JumbulayaState.Play)		// anybody when idle
					&& (gb.activeRow<0)
					&& gb.canCallJumbulaya(false))
				|| ((gb.whoseTurn==forPlayer)	// only us when in progress
					 && (state==JumbulayaState.CanJumbulaya)));
    }

    private String bigString = null;
    private int bigX = 0;
    private int bigY = 0;
    
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  
       JumbulayaBoard gb = disB(gc);
       JumbulayaState state = gb.getState();
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
       gameLog.redrawGameLog2(gc, nonDragSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),standardBoldFont());
       GC.unsetRotatedContext(gc,selectPos);
       
       GC.frameRect(gc, Color.black, logRect);
       // this does most of the work, but other functions also use contextRotation to rotate
       // animations and sprites.
       boolean planned = plannedSeating();
     
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       {    
	   GC.setRotatedContext(gc,largerBoardRect,selectPos,effectiveBoardRotation);
	   JumbulayaChip ch = gb.getPlayerChip(gb.whoseTurn);
	   ch.drawChip(gc,this,iconRect,null);
       standardGameMessage(gc,stateRect,state);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,selectPos);
       String msg = 
    		   bb.invalidReason==null 
    		   	? definitionString()
    		   	: s.get(bb.invalidReason);
       String goalmsg = bb.invalidReason==null ? GoalExplanationOnly : InvalidExplanation;
       goalAndProgressMessage(gc,nonDragSelect,Color.black,msg,progressRect, goalRect,goalmsg);
       String timeControlMessage = timeControl().timeControlMessage();
       if(timeControlMessage!=null)
       { GC.Text(gc,true,timeRect,Color.black,null,timeControlMessage);
       }
       if(planned) 
       	{ StockArt.Rotate180.drawChip(gc, this,rotateRect, selectPos, JumbulayaId.Rotate,s.get(RotateMessage)); 
       	  JumbulayaChip chip = lockOption ? JumbulayaChip.UnlockRotation : JumbulayaChip.LockRotation;
       	  chip.drawChip(gc, this,lockRect, selectPos, JumbulayaId.Lock,s.get(chip.tip)); 
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
    	   if(planned && canStartJumbulaya(gb,player))
    	   {   // start a jumbulaya, even if not our turn
    		   if(startJRect.show(gc, startJrects[player], 0, selectPos))
    		   {
    			   selectPos.hit_index = player;
    		   }
    	   }
    	   else if(planned && (state==JumbulayaState.Jumbulaya) && (gb.whoseTurn==player))
    	   {   // end a jumbulaya in progress
    		   endJRect.show(gc, startJrects[player], 0, selectPos);
    	   }
       	   pl1.setRotatedContext(gc, selectPos,true);
       	}
       if(!planned)
      	{  
    	   int ap = allowed_to_edit|G.offline() ? gb.whoseTurn : getActivePlayer().boardIndex;
    	   // generally prevent spectators seeing tiles, unless openracks or gameover
    	   boolean censorSpectator = !gb.openRacks && !gb.openRack[ap] && getActivePlayer().spectator&&!allowed_to_edit;
    	   drawRack(gc,gb,bigRack,gb.getPlayerRack(ap),gb.getPlayerMappedRack(ap),
    			   gb.getRackMap(ap),gb.getMapPick(ap),
    			   	censorSpectator,
    			   	censorSpectator ? null : selectPos,
    			   	ourTurnSelect==null); 
      	}
     
       GC.setFont(gc,standardBoldFont());
       

       if (state != JumbulayaState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{
				boolean done = gb.DoneState();
				handleDoneButton(gc,messageRotation,doneRect,(done ? buttonSelect : null),HighlightColor, rackBackGroundColor);		
				}
			int apindex = allowed_to_edit ? gb.whoseTurn : getActivePlayer().boardIndex;
			if(canStartJumbulaya(gb,apindex))
			{
				if(startJRect.show(gc,messageRotation,selectPos))
			{
					selectPos.hit_index = apindex;
				}
			}
			else if((state==JumbulayaState.Jumbulaya) && (gb.whoseTurn==apindex))
			{	// during a jumbulaya, abandon hope and lose a turn
				endJRect.show(gc, messageRotation,buttonSelect);
			}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
			if(state==JumbulayaState.Play 
					&& (buttonSelect!=null)
					&& gb.notStarted())
					{
					passButton.show(gc,messageRotation,buttonSelect);
					}
			
			
        }
		if(G.debug()||allowed_to_edit)
		{ 	checkWordsButton.show(gc,messageRotation,selectPos);
			checkJumbulayaButton.show(gc,messageRotation,selectPos);
			vocabularyRect.draw(gc, selectPos);
		}

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==JumbulayaState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        
        
             //      DrawRepRect(gc,pl.displayRotation,Color.black,b.Digest(),repRect);
        
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        
        drawHiddenWindows(gc, selectPos);	

        if(bigString!=null)
        {	GC.setFont(gc,largeBoldFont());
        	GC.drawBubble(gc,bigX,bigY,bigString,fullRect,messageRotation);
        }
    }
    public void standardGameMessage(Graphics gc,Rectangle stateRect,JumbulayaState state)
    {
        standardGameMessage(gc,
   				state==JumbulayaState.Gameover?gameOverMessage():s.get(state.description()),
   				state!=JumbulayaState.Puzzle,
   				bb.whoseTurn,
   				stateRect);

    }
    

    public boolean allowUndo() 
    {
    	if(bb.getState()==JumbulayaState.Jumbulaya) { return(false); }
    	return super.allowUndo();
    }
    public boolean allowResetUndo() { return(false); }
    public boolean canSendAnyTime(commonMove m)
    {
    	return super.canSendAnyTime(m)
    			|| (m.op==MOVE_SHOW);
    }
    public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode mode)
    {
	   	 if(m.op==MOVE_DROPONRACK)
	   	 {
	   		 // this is the point where a remote RPC screen thinks a tile
	   		 // is being dropped from the main screen.  Unfortunately it
	   		 // may have already been dropped by a bounce or other action
	   		 // on the main screen.
	   		 if((remoteViewer<0) && (bb.pickedObject!=null))
	   		 {	transmit = true;
	   		 	m.op=MOVE_DROPFROMBOARD;
	   		 }
	   	 }
  	
    	return(super.PerformAndTransmit(m,transmit,mode));
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
    	 // record some state so the game log will look pretty
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
		if(replay!=replayMode.Replay) { playSounds((Jumbulayamovespec)mm); }
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

 void playSounds(Jumbulayamovespec mm)
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
        return (new Jumbulayamovespec(st, pl));
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
    	  case MOVE_DROPONRACK:
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
public void verifyGameRecord()
{	//DISABLE_VERIFY = true;
	super.verifyGameRecord();
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
        if (hp.hitCode instanceof JumbulayaId)// not dragging anything yet, so maybe start
        {
        JumbulayaId hitObject =  (JumbulayaId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
        case Rack:
        case LocalRack:
        case RemoteRack:
    		{
    		// drawing the rack prepares the move
            String msg = (String)hp.hitObject;
            // transmit only drop from the board, not shuffling of the rack
            boolean transmit = (hitObject==JumbulayaId.Rack) 
            		||  ((bb.whoseTurn==remoteViewer)&&(hitObject==JumbulayaId.RemoteRack));
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
	    {
	    	JumbulayaState state = bb.getState();
            if((state==JumbulayaState.Jumbulaya)||(state==JumbulayaState.ConfirmJumbulaya))
            {}
            else {
	        JumbulayaCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
            }
	    }
	    	break;
        }} 
    }

    private void showWords(WordStack ws,HitPoint hp,String msg)
    {
    	StringBuilder words = new StringBuilder();
    	G.append(words,msg,"\n");
    	for(int lim=ws.size()-1;lim>=0; lim--)
    		{
    		Word w = ws.elementAt(lim);
    		if(w.isJumbulaya())
    		{
    		G.append(words, w.name,"\n");	
    		}
    		else { 
    		Word pre = bb.getCurrentWord(w.seed.row);
    		G.append(words, pre.name," -> ",w.name,"\n");       		
    		}
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
       	if(!(id instanceof JumbulayaId))  
       		{   missedOneClick = performStandardActions(hp,missedOneClick);
       			definitionCell = null;
       		}
        else {
        missedOneClick = false;
        JumbulayaId hitCode = (JumbulayaId)id;
        switch (hitCode)
        {
        default:
        	if (performStandardButtons(hitCode)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " + hp);
            }
        	break;
        case EndJumbulaya:
        	PerformAndTransmit("stopjumbulaya");
        	break;
        case StartJumbulaya:
        	{
        	JumbulayaChip chip = bb.getPlayerChip(hp.hit_index)	;
        	PerformAndTransmit("StartJumbulaya "+chip.tip);
        	}
        	break;
        case Vocabulary:
        	bb.setVocabulary(vocabularyRect.value);
        	break;
        case LocalRack:
        case RemoteRack:
        	// local rack never has a real moving object]
        	break;
        case Definition:
        	definitionCell = hitCell(hp);
        	break;
        case CheckJumbulayas:
        	// check unrestricted by winning, but use the vocabulary limit
        	bb.setVocabulary(vocabularyRect.value);
        	showWords(bb.checkLikelyJumbulayas(),hp,s.get(PossibleMessage));
        	break;
        case CheckWords:
        	{
        	bb.setVocabulary(vocabularyRect.value);
        	showWords(bb.checkLikelyWords(),hp,s.get(WordsMessage));
        	}       	
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
        	JumbulayaChip ch = (JumbulayaChip)hp.hitObject;
        	PerformAndTransmit("SetBlank "+ch.letter);
        	}
        	break;
        case Rack:
    		{
    		// drawing the rack prepares the move
            String msg = (String)hp.hitObject;
            // transmit only drop from the board, not shuffling of the rack
            PerformAndTransmit(msg,hitCode==JumbulayaId.Rack,replayMode.Live);
        	}
        	
        	break;
        case Claimed:
        case PlayerCell:
	        {	if(bb.getState()==JumbulayaState.Puzzle)
	        {
	        	JumbulayaCell hitObject = hitCell(hp);
	      		if(bb.pickedObject==null)
	      		{
	        			PerformAndTransmit(G.concat("Pick ",hitCode.name()," ",hitObject.col," ",hitObject.row));
	        	}
	    		else {
	    			PerformAndTransmit(G.concat("Drop ",hitCode.name()," ",hitObject.col," ",hitObject.row)); 
	    		}
	    	}
	    	}
	        break;
        	
       case DrawPile:
       case EmptyBoard:
        	{
        		JumbulayaCell hitObject = hitCell(hp);
        		if(bb.pickedObject==null)
            	{	JumbulayaCell c = getPickedRackCell(hp);
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
        		else if(hitCode==JumbulayaId.DrawPile)
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
            JumbulayaCell hitObject = hitCell(hp);
            JumbulayaState state = bb.getState();
            if((state==JumbulayaState.Jumbulaya)||(state==JumbulayaState.ConfirmJumbulaya))
            {
            	PerformAndTransmit("Select "+hitObject.col+" "+hitObject.row);
            }
            else {
 			PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
        	}}
			break;
			
        }
        }
    }


    private boolean setDisplayParameters(JumbulayaBoard gb,Rectangle r)
    {
      	boolean complete = false;
      	// the numbers for the square-on display are slightly ad-hoc, but they look right
      	gb.SetDisplayParameters(0.896, 1.12, 0.39,-0.10,0); // shrink a little and rotate 60 degrees
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
    public String sgfGameType() { return(Jumbulaya_SGF); }	// this is the official SGF number assigned to the game

   
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
    {  return(new JumbulayaPlay());
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

    /*
     * support for hidden windows in pass-n-play mode
     * */
 
    public String nameHiddenWindow()
    {	return ServiceName;
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
    	
    	JumbulayaCell rack [] = bb.getPlayerRack(index);
    	if(rack!=null)
    	{
    	int hiddenRack[] = bb.getRackMap(index);
    	int hiddenMapPick = bb.getMapPick(index);
    	Rectangle rackRect = new Rectangle(l,t+h/2,w,(int)(step*3.25));
    	Rectangle eyeRect = new Rectangle(l,t+step*2,step,step);
    	Rectangle whoRect = new Rectangle(l+step*2,t+step*2,w-step*4,step*2);
    	Rectangle stateRect = new Rectangle(l+step,t,w,step);
    	Rectangle icon = new Rectangle(l,t,step,step);
    	Rectangle turnNotice =new Rectangle(l,t+step,w,step);
    	if (remoteViewer<0)
    		{ StockArt.Scrim.image.stretchImage(gc, bounds);
    	}
    	Font myFont = G.getFont(largeBoldFont(), step/2);
    	GC.setFont(gc, myFont);
    	JumbulayaChip chip = bb.getPlayerChip(bb.whoseTurn);
    	chip.drawChip(gc,this, icon,null);
    	GC.Text(gc, true, whoRect, Color.black, null, s.get(ServiceName,prettyName(index)));
    	JumbulayaState state = bb.getState();
    	
    	standardGameMessage(gc,stateRect,state);
        
    	boolean hide = bb.hiddenVisible[index];
    	if(rack!=null)
    	{
    	drawRack(gc,bb,rackRect,rack,bb.getPlayerMappedRack(index),
    			hiddenRack,hiddenMapPick,hide,hp,bb.whoseTurn!=index);
    	}
    	drawEye(gc,bb,eyeRect,hide,hp,index);
    	GC.setFont(gc, myFont);
		if(bb.whoseTurn==index)
			{
			 GC.Text(gc, true, turnNotice,
			Color.red,null,YourTurnMessage);
			}
    	}
    }
    /*
    public void startRobotTurn(commonPlayer p)
    {	if(bb.pickedSourceStack.size()>10) 
    		{	
    		PerformAndTransmit("Undo");
    		//G.waitAWhile(this, 1000);
    		}
    else { 
        if (canStartRobotTurn(p))
        {	//G.print("Start turn for "+p+" by "+my);
            p.startRobotTurn();
        }}
    }
    public boolean allowBackwardStep()
    {	return(false);
    }
    */

}

