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
package viticulture;

//TODO: side screens need to display the context sensitive cards for planting and building
// TODO: add "blended wines" as a new option - use 2 or more cheap wines to make a better
//       add "3 special workers" (use only 2) as an option
//		 add "no seasons" as an option
// TODO: preselect the font size for grapes and wines value
// TODO: draft orange cards at the start
// TODO: market for wine orders, extended wine orders require more valuable wines.

import static viticulture.Viticulturemovespec.*;

import java.awt.*;
import online.common.*;
import java.util.*;

import common.GameInfo;
import lib.Image;
import lib.Graphics;
import lib.CellId;
import lib.DefaultId;
import lib.Drawable;
import lib.ExtendedHashtable;
import lib.FontManager;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.IStack;
import lib.ImageStack;
import lib.LFrameProtocol;
import lib.LateDrawing;
import lib.LateDrawingStack;
import lib.MouseState;
import lib.Plog;
import lib.StockArt;
import lib.Text;
import lib.TextButton;
import lib.TextChunk;
import lib.TextContainer;
import lib.TextGlyph;
import lib.TextStack;
import lib.Base64;
import lib.Toggle;
import lib.Tokenizer;
import lib.Random;
import online.game.*;
import online.game.sgf.sgf_game;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import viticulture.PlayerBoard.ScoreEvent;
import viticulture.PlayerBoard.ScoreStack;
import vnc.VNCService;

public class ViticultureViewer extends CCanvas<ViticultureCell,ViticultureBoard> implements ViticultureConstants
{	static final long serialVersionUID = 1000;
     // colors
	int BX_CELLS = 28;
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackgroundColor = new Color(230,250,240);
    private Color rackBackGroundColor = new Color(130,192,130);
    private Color dullBackgroundColor =  new Color(130,170,130);
    private Color boardBackgroundColor = new Color(140,165,140);
    LateDrawingStack lateTips = new LateDrawingStack();
    // this optimization pushes more elements into the background bitmap, which
    // has to be reconstructed frequently.  Sometime around Dec 2022 I started
    // noticing "flash" frames on desktops, which turned out to be frames missing the board part
    // of the background.  Attempts to fix the problem by not using the regenerated
    // backgrounds for a delay failed, so instead I've turned it off.  It's still on
    // for mobiles, where it doesn't seem to cause a problem.
    static boolean BACKGROUND_OPTIMIZATION = G.isCodename1();
    boolean showBuildings = false;
    boolean showOptions = false;
    
    public void testSwitch()
    {	super.testSwitch();
    	//BACKGROUND_OPTIMIZATION = !BACKGROUND_OPTIMIZATION;
    	//G.print("Now "+BACKGROUND_OPTIMIZATION);
    	//generalRefresh();
    	
    }
    // this is used mainly to position the check boxes on cards
    // that offer choices
    static class Loc
    {	static Hashtable<Loc,Loc> bank=new Hashtable<Loc,Loc>();
    	ChipType type;
    	ViticultureId id;
    	int index;
    	double x=0,y=0,xscale=0.2,yscale=0.2;
    	public int getX(Rectangle r)
    		{ 
    		// the 0.038 is an ad hoc adjustment after the stockart was recentered
    		return (int)((G.centerX(r)+G.Width(r)*(x+0.038)));
    		}
    	public int getY(Rectangle r)
    		{
    		// the 0.019 is an ad hoc adjustment after the stockart was recentered
    		return ((int)((G.centerY(r)+G.Height(r)*(y+0.019))));
    		}
    	public int getW(Rectangle r)
    		{
    		return((int)(G.Width(r)*xscale));
    		}
      	public int getH(Rectangle r)
		{
      		return((int)(G.Height(r)*yscale));
		}
    	public Rectangle getRect(Rectangle r)
    	{	return new Rectangle(getX(r),getY(r),getW(r),getH(r));
    	}
    	
    	public boolean equals(Object other)
    	{	if(other instanceof Loc)
    		{ Loc ot = (Loc)other;
    		  return(type==ot.type && index==ot.index && id==ot.id);
    		}
    		return(false);
    	}
    	public int hashCode()
    	{
    		return(id.ordinal()+type.ordinal()*1000+index*1000000);
    	}
    	public Loc(ChipType t,int i,ViticultureId vid)
    	{
    		type = t;
    		index = i;
    		id = vid;
    	}
    	static Loc make(ChipType type,int index,ViticultureId id,double x,double y,double scale)
    	{	Loc l = new Loc(type,index,id);
    		l.x = x;
    		l.y = y;
    		l.yscale=l.xscale = scale;
    		bank.put(l,l);
    		return(l);
    	}
    	static Loc make(ChipType type,int index,ViticultureId id,double x,double y,double xscale,double yscale)
    	{	Loc l = new Loc(type,index,id);
    		l.x = x;
    		l.y = y;
    		l.yscale=yscale;
    		l.xscale = xscale;
    		bank.put(l,l);
    		return(l);
    	}
    	
    
    	
    	public static Loc find(ChipType type,int index,ViticultureId id)
    	{
    		Loc key = new Loc(type,index,id);
    		Loc found = bank.get(key);
    		if(found==null) 
    			{	Loc key2 = new Loc(type,0,id);
    			    Loc f = bank.get(key2); 
    			    if(f==null) { bank.put(key2,key2); f=key2; }
    				key.x = f.x;
    				key.y = f.y;
    				key.xscale = f.xscale;
    				key.yscale = f.yscale;
     				bank.put(key,key); 
     				found = key;
    			}
    		return(found);
    	}
    }

    
    // private state
    private ViticultureBoard mainBoard = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private int STANDARD_CELLSIZE;
    
    //
    // addZoneRect also sets the rectangle as specifically known to the 
    // mouse tracker.  The zones are considered in the order that they are
    // added, so the smaller ones should be first, then any catchall.
    //
    // zones ought to be mostly irrelevant if there is only one board layout.
    //
    private Rectangle chipRects[] = addZoneRect(".chip",6);
    private Rectangle playerBoardRects[] = addRect("pb",6);
    private Rectangle playerSideRects[] = addRect(".ps",6);
    private Rectangle rotatedStateRect = addRect(".rotatedState");
    private Rectangle wakeupRect = addRect(".wakeup");
    private Rectangle starRect = addRect(".stars");
    private Rectangle passRect = addRect(".passrect");
    private Rectangle passWarnRect = addRect(".passwarn");
    private Toggle hintRect = new Toggle(this,"hints",StockArt.Tooltips,ViticultureId.ShowHints,true,s.get(ShowAllHintsMessage));
    private Toggle scoreRect = new Toggle(this,"score",ViticultureChip.ScoreSheet,ViticultureId.ShowScores,true,s.get(ShowScoresMessage));
    public boolean isNextSeason()
    {	int step = History.viewStep;
    	if(step<=0) { step = History.size(); }
    	commonMove m = History.elementAt(step-1);
    	return(m.op==MOVE_NEXTSEASON || m.op==MOVE_SELECTWAKEUP);
    }
    public boolean undoNextSeason()
    {	if(UsingAutoma())
    	{int step = History.viewStep-1;
    	int size = History.size()-2;
    	if(step<=0 && size>=0) { step = size; }
    	if(step>=0)
    	{
    	commonMove m = History.elementAt(step);
    	return(m.op==MOVE_NEXTSEASON || m.op==MOVE_SELECTWAKEUP);
    	}}
    	return(false);
    }
    
    public boolean UsingAutoma()
    {
    	return(mainBoard.automa);
    }
    public int ScoreForAutoma()
    {
    	return(mainBoard.automaScore);
    }
    public boolean WinForAutoma()
    {
    	return(ScoreForAutoma()>mainBoard.pbs[0].score);
    }
    /*
     * special logic for single player mode, the current player
     * never changes, so we treat "change of season" as an honorary
     * change of player.  Undo within the current season is allowed.
     * 
     */
    public boolean allowUndo()
    {
    	return(super.allowUndo()
    			&& !undoNextSeason());
    }
    public boolean allowPartialUndo()
    {
    	return(super.allowPartialUndo()
    			&& (getCurrentMoveOp()!=MOVE_MAKEWINE)
    			&& !undoNextSeason());
    }
    public boolean allowOpponentUndo()
    {
    	return(super.allowOpponentUndo()
    			&& !undoNextSeason());
    }

    public void doUndo()
    {	
    	super.doUndo();
    	while( getCurrentMoveOp() == MOVE_MAKEWINE)
    	{
    		super.doUndoStep();
    	}	
    }
    public void doUndoStep()
    {
    	super.doUndoStep();
    	while( getCurrentMoveOp() == MOVE_MAKEWINE)
    	{
    		super.doUndoStep();
    	}	
    }
    public boolean doBack(int n)
    {
    	boolean rval = super.doBack(n);
    	while( getCurrentMoveOp() == MOVE_MAKEWINE)
    	{
    		rval = doBack(1);
    	}
    	return(rval);
    }
    public boolean doBackwardPlayer()
    {
		BoardProtocol bd = mainBoard;
    	int who =bd.whoseTurn();
    	int steps = 0;
		while((bd.whoseTurn()==who) && doScrollTo(BACKWARD_ONE))
			{
			if(isNextSeason() && steps>1) { doForward(replayMode.Single); return(true); }
			steps++;
			};
		while((bd.whoseTurn()!=who) && doScrollTo(BACKWARD_ONE)) {};
		return(doForward(replayMode.Replay));
    }
    public void doForwardPlayer()
    {
    	BoardProtocol bd =  mainBoard;
		int who =bd.whoseTurn();
		while((bd.whoseTurn()==who) && doForward(replayMode.Single))
		{ if(isNextSeason()) { doForward(replayMode.Single); break; }
		}; 
    }


/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	Plog.log.addLog("preload");
    	ViticultureChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	Plog.log.addLog("preload finished");
		gameIcon = ViticultureChip.Icon.image;
		
    }

	public int PrettyScoreForPlayer(BoardProtocol gb, commonPlayer p)
	{	ViticultureBoard b = (ViticultureBoard)gb;
		if(UsingAutoma() && (p.boardIndex>0)) { return(b.automaScore); }
		else {
			PlayerBoard pb = b.pbs[p.boardIndex];
			return(pb.score);
		}
	}
	
	private Color ViticultureMouseColors[] = {
			 Color.blue, Color.green, bsOrange, bsPurple, Color.white, Color.yellow 

	};
	private Color ViticultureMouseDotColors[] = {
			Color.white,Color.white,Color.white,
			Color.white,Color.black,Color.black
	};
	
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	Plog.log.addLog("init started");
    	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	int players_in_game = info.getInt(OnlineConstants.PLAYERS_IN_GAME,chipRects.length);
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	ViticultureState.putStrings();
        }
         
        MouseColors  = ViticultureMouseColors;
        MouseDotColors = ViticultureMouseDotColors;
 
        String type = info.getString(GameInfo.GAMETYPE, ViticultureVariation.viticulture.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        mainBoard = new ViticultureBoard(type,players_in_game,randomKey,getStartingColorMap(),ViticultureBoard.REVISION,isTurnBasedGame());
        backgroundCells = new ViticultureCell[] {mainBoard.greenDiscards,mainBoard.yellowDiscards,
    		mainBoard.purpleDiscards,mainBoard.blueDiscards,
    		mainBoard.structureDiscards};
        deckCells = new ViticultureCell[] {mainBoard.greenCards,mainBoard.yellowCards,mainBoard.purpleCards,
        			mainBoard.blueCards,mainBoard.structureCards};
        
        scoreRect.setValue(true);	// turn it on
        //believed to work, but the display on android is too glitchy
        //useDirectDrawing(!G.isAndroid());
        //if(G.debug()) { useDirectDrawing(true); }
        doInit(false);
        if(reviewOnly || isTurnBasedGame()) { setSimultaneousTurnsAllowed(false); }
        adjustPlayers(players_in_game);
        ready = true;
        Plog.log.addLog("init finished");
    }
    private boolean ready = false;

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        mainBoard.doInit(mainBoard.gametype);						// initialize the board
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

    public void truncateHistory()
    {
    	super.truncateHistory();
    	mainBoard.flashChip = null;	// inhibit the flashed cards when backing up
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
    public void setLocalBounds(int x, int y, int width, int height)
    {	// try different aspect ratios
       	double aspects[] = new double[]{ 1.2 , 1.5, 2.2, 2.5, 1.8};
       	setLocalBoundsV(x,y,width,height,aspects);
       	zoomer.reCenter();
    }

    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
    {	G.SetRect(fullRect, x, y, width, height);
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh0 = standardFontSize();
    	int margin = fh0/2;
    	//double bestPercent = 
    	selectedLayout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.8-0.05*Math.max(3, nPlayers),	// % of space allocated to the board
    			aspect,	// aspect ratio for the board
    			// the player box is a lot bigger than the text size, so allow it
    			// to be relatively smaller.
    			fh0*1.4,fh0*2.0,		// maximum cell size
    			0.2	// preference for the designated layout, if any
    			);
    	int fh = standardFontSize();
    	FontMetrics fm = getFontMetrics(standardPlainFont());
    	int minLogW = fh*15;	
    	int vcrW = minLogW;
       	int minChatW = fh*30;	
         int buttonW = fh*8;
        int buttonH = buttonW*2/3;
        int stateH = fh*5/2;
        Rectangle main1 = selectedLayout.peekMainRectangle();
        int minLogH = Math.min(fh*15,G.Height(main1)-margin*2);	
        
        selectedLayout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*3/2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3);
        selectedLayout.placeRectangle(doneRect,buttonW,buttonH,BoxAlignment.Center);
        selectedLayout.placeTheVcr(this,vcrW,vcrW*3/2);
     	int passw = 0;
       	for(String season : NextSeasons)  
       		{ passw = Math.max(passw,fm.stringWidth(s.get(NextSeasonMessage,s.get(season))));
       		}
       	
    	Rectangle main = selectedLayout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	boolean around = seatingAround();
    	boolean rotateBoard = mainW<mainH;
    	int nrows = rotateBoard ? BX_CELLS :  15;  	// b.boardRows
        int ncols = rotateBoard ? 15 : BX_CELLS;	//b.boardColumns

    	// calculate a suitable cell size for the board. If "around" seating is in effect,
    	// we need a second "state" rectangle down the left side of the board.
    	double cs = Math.min((double)(mainW-(around?stateH:0))/ncols,(double)(mainH-stateH)/nrows);
    	STANDARD_CELLSIZE = CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	{
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(nrows*cs);
    	int extraW = Math.max(0, (mainW-boardW-(around?stateH:0))/2);
    	int extraH = Math.max(0, (mainH-stateH-boardH)/2);
    	int boardX = mainX+extraW+(around ? stateH : 0);
    	int boardY = mainY+stateH+extraH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH;
        int stateX = boardX+stateH*2;
        placeRow(stateX,stateY,boardW-stateH*2 ,stateH,stateRect,annotationMenu,scoreRect,hintRect,noChatRect);
        G.SetRect(rotatedStateRect,stateX-boardH/2-stateH,stateY+boardH/2,boardH-stateH*2,stateH);
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW-stateH/3,stateH,goalRect);       
        setProgressRect(progressRect,goalRect);

    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	selectedLayout.returnFromMain(extraW,extraH);

    	if(rotateBoard)
    	{
    		G.setRotation(boardRect,-Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}}
    	int boardX = G.Left(boardRect);
    	int boardY = G.Top(boardRect);
    	int boardH = G.Height(boardRect);
    	int boardW = G.Width(boardRect);
       	int third = (int)(boardW*0.2);
    	int mid = boardX+(int)(boardW*0.12);
  	
    	G.SetRect(wakeupRect, mid-third,boardY+(int)(boardH*0.22),third*2,(int)(boardH*0.45));
    	G.SetRect(starRect, mid-third,boardY+(int)(boardH*0.6),third*2,(int)(boardH*0.45));
 
		// set a default position so replay animations will be quasi reasonable
    	{int cx = G.centerX(boardRect);
		int cy = G.centerY(boardRect);
		for(ViticultureCell c = mainBoard.uiCells; c!=null; c=c.next) 	
			{	c.rotateCurrentCenter(0,cx,cy,cx,cy);
			}}

    	int passX = (int)(boardX+0.2*boardW);
    	int passY = (int)(boardY+0.25*boardH);
    	G.SetRect(passRect, passX,passY,passw*3/2,fh*3);
    	G.SetRect(passWarnRect, passX,passY+fh*3,passw*2,fh*3);
    	G.SetRect(editRect,boardX+boardW-buttonW-fh*3,G.Top(passRect),buttonW,fh*3);
    	
        
        //selectedLayout.optimize();
        positionTheChat(chatRect, chatBackgroundColor,rackBackGroundColor);
        //return the board size as a score
        labelFont =standardPlainFont();
        
        return(boardW*boardH);
    }

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	// place the player info group vertically, which will lie to the left of the player board.
    	// this leaves maximum vertical space for the main board.
    	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chipRect = chipRects[player];
    	Rectangle doneRect = doneRects[player];
    	Rectangle board = playerBoardRects[player];
    	Rectangle sideBoard = playerSideRects[player];
    	int doneW = plannedSeating()?unitsize*3:0;
    	int doneH = unitsize*6;
	    G.SetRect(doneRect,x,y+unitsize,doneW,doneH);
	    int doneR = x+doneW+unitsize/2;
	    Rectangle box = pl.createVerticalPictureGroup(doneR,y,unitsize+unitsize/3);
	    
	    
	    G.SetRect(chipRect,	G.Right(box)-unitsize*2,y+2*unitsize,	unitsize*2,	unitsize*2);
	    int sideW = unitsize*5;
	    G.union(box, doneRect);
	    int boardH = unitsize*10;
	    G.SetRect(board, G.Right(box)+sideW-unitsize/2,y+unitsize,unitsize*15,boardH);
	    G.SetRect(sideBoard, G.Right(box),y+unitsize, sideW,boardH);
	    G.union(box, sideBoard,board,new Rectangle(x,y,unitsize,unitsize));
	    pl.displayRotation = rotation;
	    return(box);
    }
    private boolean drawArray(Graphics gc,boolean positionKludge,ViticultureState state,commonPlayer pl,ViticultureBoard gb,Rectangle br,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets,
    		double[][]fieldsLoc,
    		double xstep,double ystep,String label,ViticultureCell... fields )
    {
    	return(drawArray(gc,positionKludge,state,pl,gb,0,br,highlight,highlightAll,targets,fieldsLoc,
    			xstep,ystep,label,fields));
    }
        
    private boolean drawArray(Graphics gc,boolean positionKludge,ViticultureState state,commonPlayer pl,ViticultureBoard gb,double rotation,Rectangle br,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets,
    		double[][]fieldsLoc,
    		double xstep,double ystep,String label,ViticultureCell... fields )
    {
        int w = G.Width(br);
        int h = G.Height(br);
        int x = G.Left(br);
        int y = G.Top(br);
        if(positionKludge && (BX_CELLS!=30))
        {
        int spare = w/BX_CELLS;	// this is an adjustment because the cell coordinates were derived
        x -= spare*(30-BX_CELLS)/2;				// based on a board that was 30 cells wide
        w = (int)(w*30*0.993/BX_CELLS);
        }
        
        boolean some = false;
        boolean showLabels = ViticultureChip.INDEX.equals(label);
        for(int i=0;i<fields.length;i++)
        {
        	double loc[] = fieldsLoc[i];
        	int xpos = (int)(x+loc[0]*w);
        	int ypos = (int)(y+loc[1]*h);
        	int sz = (int)(loc[2]*w);
        	if(sz>0)
        	{
        	if(rotation!=0)
        	{
        		GC.setRotation(gc, rotation, xpos, ypos);        		
        	}
        	ViticultureCell c = fields[i];
        	boolean selectable = c.isSelectable() && gb.legalToHit(c,targets);
        	String thisLabel = showLabels ? ""+(i+(9-fields.length+1)) : label;
        	ViticultureChip top = c.topChip();
        	boolean actPrevious = (gb.resetState==ViticultureState.TakeActionPrevious);
        	boolean isWorkerPlace = selectable && ((c.contentType==ChipType.Worker) 
        											|| (c.contentType==ChipType.Field)
        											|| ((c.rackLocation!=ViticultureId.StructureCards)
        													&& (c.rackLocation!=ViticultureId.StructureDiscards)
        													&& (c.contentType==ChipType.StructureCard)));
        	boolean isWorker = top!=null && top.type.isWorker() && !actPrevious ;
			if(isWorkerPlace && isWorker)
			{ // if it is a worker, draw the landing pad first
			  StockArt.LandingPad.drawChip(gc, this, sz*2/3,xpos,ypos,null);
			  int ss = sz*1/4;
			  GC.frameRect(gc, Color.red, xpos-ss,ypos-ss,ss*2,ss*2);
			}
			HitPoint drawHit = selectable ? highlight : null;
        	if(drawStack(gc,state,pl,c,drawHit,highlightAll,sz,xpos,ypos,0,xstep,ystep,thisLabel))
        	{	some = true;
        		int ind = drawHit.hit_index;
        		if(c.rackLocation==ViticultureId.Workers
        				&& ind>=0 
        				&& ind<c.height()
        				&& (gb.movingObjectIndex()<0))
        		{	// make the meeples pop to help show what you are picking
        			ViticultureChip chip = c.chipAtIndex(ind);
        			if(chip!=null)
        			{
        			boolean isTouch = isTouchInterface();
        			if(!isTouch || drawHit.down)	// on touch screens, pop only while pressed
        			{
        			int touch = isTouch ? ypos-sz : ypos;
        			int xp = (int)(xpos+xstep*sz*ind);
        			int yp = (int)(touch+ind*sz*ystep);
        			// adjust the highlight box to the popped position
        			int dx = xp-highlight.hit_x;
        			int dy = yp-highlight.hit_y;
        			int newxp = highlight.hit_x += dx;
        			int newyp = highlight.hit_y += dy;
        			int neww = highlight.hit_width += highlight.hit_width/3;
        			int newh = highlight.hit_height += highlight.hit_height/3;        			
        			if(highlight.spriteRect!=null)
        				{
        				G.SetRect(highlight.spriteRect,newxp-neww/2,newyp-newh/2,neww,newh);
        				}
       				chip.drawChip(gc, this, sz+sz/3, xp,yp,null);
       				
        			}}
        		}
    			if(gb.pickedObject==null)
    			{switch(gb.resetState)
        		{
        		case Move1Star:
        		case Move2Star:
        		case Retrieve1Current:
        		case Retrieve2Workers:
        			// make sure we're hitting something the right color
        			ViticultureChip chip = c.chipAtIndex(highlight.hit_index);
        			ViticultureColor targetColor = gb.getCurrentPlayerBoard().color;
        			if((chip==null) || (chip.color!=targetColor))
        				{ 
        				for(int lim=c.height()-1; lim>=0; lim--)
        					{
        					if(c.chipAtIndex(lim).color==targetColor) 
        						{ highlight.hit_index = lim;
        						  break; 
        						}
        					}
        				}
        			break;
        		default: break;
        		}}
        	}
        	else { Rectangle chipR = new Rectangle(xpos-sz/2,ypos-3*sz/4,sz,sz*3/2);
        		if(!c.onBoard && G.pointInRect(highlightAll,chipR))
        		{
        		int targetIndex = 0;
        		switch(c.contentType)
        		{
        		default: break;
        		case Field: 
        			targetIndex = 1;
					//$FALL-THROUGH$
				case GreenCard:
        		case StructureCard:
        		{	ViticultureChip chip = (c.height()>targetIndex) ? c.chipAtIndex(targetIndex) : null;
        			if(chip!=null)
        			{ if(chip.type==ChipType.StructureCard)
   						{
        				highlightAll.hitObject = chip;
   						highlightAll.hitCode=ViticultureId.ShowBigChip;
   						highlightAll.arrow = StockArt.Eye;
   						highlightAll.awidth = G.Width(chipR)*2/3;
   						}
        			else if(chip.type==ChipType.GreenCard)
        			{
        				highlightAll.hitObject = c;
   						highlightAll.hitCode=ViticultureId.ShowBigStack;
   						highlightAll.arrow = StockArt.Eye;
   						highlightAll.awidth = G.Width(chipR)*2/3;        				
        			}
        		}}}
        	}}
        	
			if(isWorkerPlace && !isWorker)
			{ StockArt.LandingPad.drawChip(gc, this, sz*2/3,xpos,ypos,null);
			  int ss = sz*1/4;
			  GC.frameRect(gc, Color.red, xpos-ss,ypos-ss,ss*2,ss*2);
			}

      	
          	if(rotation!=0)
        	{
        		GC.setRotation(gc, -rotation, xpos, ypos);        		
        	}
  
        	}
        }
        return(some);
    }
    private ViticultureCell proxy = new ViticultureCell(ViticultureId.Cards);
    
    // drawing card stacks forces their images to be loaded, even though you can't see them.
    // this method replaces cards you won't see with card backs.
	public boolean drawStack(Graphics gc,ViticultureCell c,HitPoint highlight,int sz,int xpos,int ypos, int steps, double xscale,double yscale,String label)
	{
		boolean backs = ViticultureChip.BACK.equals(label);
		boolean tops = ViticultureChip.TOP.equals(label);
		int size = c.height();
		if(tops) { label = null; }
		if((backs|tops) && (size>1))
		{	
			proxy.reInit();
			proxy.rackLocation = c.rackLocation;
			proxy.contentType = c.contentType;
			proxy.copyCurrentCenter(c); 
			for(int i=0,lim=size-1;i<=lim;i++)
			{	ViticultureChip target = c.chipAtIndex(i);
			    //if(target==null) { G.print("no target for ",c, " @ ",i);   }
			    //else
			    {
				ViticultureChip back = target.cardBack;
				ViticultureChip alt = back!=null ? back : target;
				if(backs || (tops&&(i<lim))) { proxy.addChip(alt); }
				else proxy.addChip(target);
			}
			}
			boolean v = proxy.drawStack(gc,this,highlight,sz,xpos,ypos, 0, xscale,yscale, null);
			if(v) { highlight.hitObject = c; }
			c.copyCurrentCenter(proxy); 
			// don't leave some random center on the proxy
			proxy.copyCurrentCenter(null);
			return(v);
		}
		else 
		{	boolean v = c.drawStack(gc,this,highlight,sz,xpos,ypos, 0, xscale,yscale, label);
			return(v);
		}
	}
    private boolean drawStack(Graphics gc,ViticultureState state,commonPlayer pl,ViticultureCell c,HitPoint highlight,HitPoint highlightAll,int sz,int xpos,int ypos,int liftSteps,double xstep,double ystep,String label)
    {
     	boolean selected = drawStack(gc,c,highlight,sz,xpos,ypos, 0, xstep,ystep, label);
     	if(selected)
    	{	
    		highlight.awidth = highlight.hit_width;
    		int aw = highlight.hit_width;
    		int ah = highlight.hit_height;
    		if(aw!=ah)
    		{
    			highlight.spriteRect = new Rectangle(highlight.hit_x-aw/2,highlight.hit_y-ah/2,aw,ah);
    		}
    		else {
    			highlight.spriteRect = null;
    		}
    		highlight.spriteColor = Color.red;
    	}
     	if(hintRect.isOnNow()
     			&& (pl!=null)
     			&& (gc!=null)
     			&& (c.toolTip!=null)
     			&& (c.contentType.showStaticToolTips())
     			&& (c.row==(((G.rotationQuarterTurns(pl.displayRotation)&1)!=0) ? c.tipOffset : 0))
     			) 
     		{  GC.setFont(gc, standardPlainFont());
     		   final int xp = c.centerX();
     		   final int yp = c.centerY();
     		   final double rot = pl.displayRotation;
     		   final String msg = s.get(c.toolTip);
     		   final Rectangle r = fullRect;
     		   lateTips.addElement(new LateDrawing() 
     		   						{
     			   					public void draw(Graphics gc)
     			   					{ 	    		   						
     			   						GC.drawBubble(gc, xp,yp,msg,r,rot);
     			   					}});
     		}
     	else if(c.toolTip!=null) 
     	{ 	
     		HitPoint.setHelpTextNear(highlightAll,xpos,ypos,sz*2,sz*2,s.get0or1(c.toolTip,c.height()));
      	} 
     	
     	if(c.isSelected())
     	{	boolean isDiscard = state.discardCards()>=0;
     		(isDiscard?StockArt.Exmark:StockArt.Checkmark).drawChip(gc, this, sz/2, xpos, ypos,null);
     	}
     	return(selected);
    }
    
    // load the display-only cell for coins.  
    private void loadCoins(ViticultureCell c,int cash)
    {
    	c.reInit();
     	while(cash>=5) { c.addChip(ViticultureChip.Coin_5); cash-=5; }
        while(cash>=2) { c.addChip(ViticultureChip.Coin_2); cash-=2; }
        while(cash>=1) { c.addChip(ViticultureChip.Coin_1); cash-=1; }
    }

    public double getPreferredRotation()
    {	double sup = super.getPreferredRotation();
    	commonPlayer pl = getPlayerOrTemp(mainBoard.whoseTurn);
    	return(sup+pl.displayRotation);
    }

    private void setBoardMagnifiers(Rectangle box0)
    {	Rectangle box = box0;   
    	if(currentZoomZone!=null && box!=null && currentZoomZone.equals(box)) 
    		{ box = null; 
    		}
    	currentZoomZone = box;
    	autoZoom = false;
    }
     
    public void setGlobalUnZoomButton()
    {	
		setBoardMagnifiers(null);
		super.setGlobalUnZoomButton();
    	
    }

private double redGrapeLoc[][] = {
	{0.0875,0.685,0.07},
	{0.14,0.685,0.07},
	{0.19,0.685,0.07},
	
	{0.13,0.775,0.07},
	{0.18,0.775,0.07},
	{0.23,0.775,0.07},
	
	{0.051,0.865,0.07},
	{0.10,0.865,0.07},
	{0.15,0.865,0.07}};
	
private double whiteGrapeLoc[][] = {
	{0.368,0.685,0.07},
	{0.42,0.685,0.07},
	{0.47,0.685,0.07},
	
	{0.33,0.775,0.07},
	{0.38,0.775,0.07},
	{0.43,0.775,0.07},
	
	{0.35,0.865,0.07},
	{0.40,0.865,0.07},
	{0.45,0.865,0.07}};
	
private double redWineLoc[][] = {
	{0.54,0.645,0.07},
	{0.585,0.645,0.07},
	{0.63,0.645,0.07},
	
	{0.69,0.645,0.07},
	{0.735,0.645,0.07},
	{0.785,0.645,0.07},
	
	{0.844,0.645,0.07},
	{0.89,0.645,0.07},
	{0.936,0.645,0.07}};
	
private double whiteWineLoc[][] = {
	{0.54,0.715,0.07},
	{0.585,0.715,0.07},
	{0.63,0.715,0.07},
	
	{0.69,0.715,0.07},
	{0.735,0.715,0.07},
	{0.785,0.715,0.07},
	
	{0.844,0.715,0.07},
	{0.89,0.715,0.07},
	{0.936,0.715,0.07}};
private double roseWineLoc[][] = {
	   		
	{0.69,0.79,0.07},
	{0.735,0.79,0.07},
	{0.785,0.79,0.07},
	
	{0.844,0.79,0.07},
	{0.89,0.79,0.07},
	{0.936,0.79,0.07}};
	
private double champaignLoc[][] = {
	
	{0.844,0.862,0.07},
	{0.89,0.862,0.07},
	{0.936,0.862,0.07}};
	
double buildingsLoc[][] = {
		{0.045,0.128,0.2},		// trellis
		{0.865,0.25,0.2},		// water tower
		{0.13,0.56,0.2},		// windmill
		{0.765,0.346,0.2},		// tasting room
		{0.48,0.367,0.2},		// yoke
		{0.336,0.481,0.2},		// cottage
		{0.74,0.95,0.15},		// medium cellar
   		{0.89,0.95,0.15},		// large cellar
		};

private double workersLoc[][] = {
	{0.92,0.5,0.18},		// workers
	{0.6,0.045,0.18},	// pending workers
	};

private double structuresLoc[][] = {
 		{0.5,0.25,0.55},
 		{0.5,0.75,0.55},
 };
private double destroyLoc[][] = {
		{0.5,0.48,0.4}	
};

PlayerBoard touchPlayer = null;
private PlayerBoard showPlayerCards()
{	return(touchPlayer); 
}


private boolean censor(PlayerBoard pb,HitPoint hp)
{	//if(allowed_to_edit) { return(false); }
	if(allPlayersLocal())
		{ return((remoteWindowIndex(hp)<0) 
			? (pb.showCards 
					? false 
					: pb.publicCensoring) 
			: pb.hiddenCensoring); 
		}
	commonPlayer player = getActivePlayer();
	PlayerBoard target = mainBoard.pbs[player.boardIndex];
	return(player.isSpectator() ? !reviewOnly : target.color!=pb.color);
}

private double residualLocs[][] = {
   			{0.05,0.1,0.05},
			{0.11,0.11,0.05},
			{0.145,0.11,0.05},
			{0.15,0.17,0.05},
			{0.13,0.21,0.05},
			{0.1,0.18,0.05}
	};

double cardsLoc[][] = {
   		{0.06,0.4,0.12},		// cards	
};
double cashLoc[][] = {
		{0.05,0.58,0.1},	// cash
};
double [][] workerLocs = {
		{0.425,0.34,0.2},		// yoke

};
double starsTrackLoc[][] = {
			{0.02,0.98,0.08} // stars
			};

private void drawVines(Graphics gc,ViticultureBoard gb,Rectangle br,commonPlayer pl,PlayerBoard pb,ViticultureState state,
				HitPoint highlight,HitPoint highlightBoard,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
{
    double vinesLoc[][] = { {0.252,0.171,0.15},	// must remain here because it's modified below.
    		{0.49,0.17,0.15},
    		{ 0.73,0.169,0.15}};
    for(int i=0;i<vinesLoc.length; i++)
    {	double loc[] = vinesLoc[i];
    	double adj = -0.045*pb.vines[i].height();

    	loc[1] += adj;
    }
    // makes sure can't click on the wines and grapes that are on the player board
    drawArray(gc,true,state,pl,gb,Math.PI,br,highlightBoard,highlightAll,targets,vinesLoc,  	0.0, 0.3, null,pb.vines);

}

private void drawPlayerBoard(Graphics gc, 
    			Rectangle br /* main rectangle */,
    			ViticultureBoard gb,
    			Rectangle sr /* structures rectangle */,
    			Rectangle r , int player, 
    			HitPoint highlight,HitPoint highlightAll,
    			Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {	
        PlayerBoard pb = gb.getPlayerBoard(player);
        commonPlayer pl = getPlayerOrTemp(player);
        boolean showAll = (remoteWindowIndex(highlightAll)>=0) || !BACKGROUND_OPTIMIZATION;
        if(showAll)
        {
        ViticultureChip.playermat.image.centerImage(gc, br);
        if(sr!=null) { ViticultureChip.structuresidemat.image.centerImage(gc, sr); }
        }
        int w = G.Width(br);
        int h = G.Height(br);
        int x = G.Left(br);
        int y = G.Top(br);
        HitPoint highlightBoard = pb.showCards ? null : highlight;
        ViticultureState state = gb.resetState;
        // never let him hit the card display on the player board
        drawArray(gc,false,state,pl,gb,br,state==ViticultureState.Puzzle ? highlight : null,highlightAll,targets,cardsLoc, 0.1, -0.02, ViticultureChip.BACK,pb.cards);
        drawArray(gc,false,state,pl,gb,br,highlight,highlightAll,targets,cashLoc, 0.05, 0.0, ""+pb.cash,pb.cashDisplay);

        
        if(r!=null)
        	{ViticultureChip ch = pb.getScoreMarker();
        	 ch.drawChip(gc,this,r,null);
        	}
        {
        // always draw for mouse sensitivity, but if optimizing backgrounds, not for actual drawing
        Graphics bgc = BACKGROUND_OPTIMIZATION ? null : gc;
        drawVines(bgc,gb,br,pl,pb,state,highlight,highlightBoard,highlightAll,targets);
        HitPoint wineHighlight = (state==ViticultureState.Puzzle)? highlightBoard : null;
         		
        drawArray(bgc,false,state,pl,gb,br,wineHighlight,highlightAll,targets,redGrapeLoc,	0.0, 0.0,  ViticultureChip.INDEX,pb.redGrape);
        drawArray(bgc,false,state,pl,gb,br,wineHighlight,highlightAll,targets,whiteGrapeLoc,0.0, 0.0, ViticultureChip.INDEX,pb.whiteGrape);
        drawArray(bgc,false,state,pl,gb,br,wineHighlight,highlightAll,targets,redWineLoc,   0.0, 0.0, ViticultureChip.INDEX,pb.redWine);
        drawArray(bgc,false,state,pl,gb,br,wineHighlight,highlightAll,targets,whiteWineLoc, 0.0, 0.0, ViticultureChip.INDEX,pb.whiteWine);
        drawArray(bgc,false,state,pl,gb,br,wineHighlight,highlightAll,targets,roseWineLoc,  0.0, 0.0, ViticultureChip.INDEX,pb.roseWine);
        drawArray(bgc,false,state,pl,gb,br,wineHighlight,highlightAll,targets,champaignLoc, 0.0, 0.0, ViticultureChip.INDEX,pb.champagne);
        
        drawArray(bgc,false,state,pl,gb,br,wineHighlight,highlightAll,targets,buildingsLoc, 0.1, 0.0,null,pb.buildable);
        drawArray(bgc,false,state,pl,gb,br,wineHighlight,highlightAll,targets,starsTrackLoc, 0.5, 0.0, null,pb.stars);
       }
     //    
     // Coordinates for drawing player boards
     //
        double fieldsLoc[][] = 
        		{ {0.252,0.171,0.242},
        		  {0.49,0.17,0.242},
     		      { 0.73,0.169,0.242}};
        for(int i=0;i<fieldsLoc.length;i++)
        {	ViticultureCell c = pb.fields[i];
        	ViticultureChip top = c.height()>1 ? c.chipAtIndex(1) : null;
        	if((top!=null && top.type==ChipType.StructureCard))
        	{
        		fieldsLoc[i][2] = 0.175; 
        		fieldsLoc[i][1] = 0.11;
        	}
        }
        drawArray(gc,false,state,pl,gb,br,highlight,highlightAll,targets,fieldsLoc,  	0.0, 0.0, null,pb.fields);    
    
                 
       if(UsingAutoma())
    	  {double [][] bonusLocs = {
        		{0.95,0.4,0.12}          
    	  };
    	  drawArray(gc,false,state,pl,gb,br,highlight,highlightAll,targets,bonusLocs, -0.5, 0.0,null,pb.bonusActions);
    	  }
       {
       // allow picking up the workers even if in showCards mode
       boolean can = gb.legalToHit(pb.workers,targets);
       drawArray(gc,true,state,pl,gb,br,can ? highlight : null,highlightAll,targets,workersLoc, -0.45, 0.0, null,pb.workers,pb.pendingWorker);
       }


        if(sr!=null)
        {
        drawArray(gc,false,state,pl,gb,sr,highlight,highlightAll,targets,structuresLoc, 0.0, 0.0, null,pb.structures);
        drawArray(gc,false,state,pl,gb,sr,highlight,highlightAll,targets,destroyLoc, 0.0, 0.0, null,pb.destroyStructureWorker);
        }
        // prepare cash
        loadCoins(pb.cashDisplay,pb.cash);

        // allow picking up the workers even if in showCards mode
        drawArray(gc,false,state,pl,gb,br,highlight,highlightAll,targets,workerLocs, 0.1, 0.0,null,pb.yokeWorker);
        
        if(sr!=null)	// proxy for we're from the main board, not the side board
        {
        int sz = w/8;
        StockArt art = pb.showCards ? StockArt.NoEye : StockArt.Eye;

        {
            if(art.drawChip(gc, this, sz, (int)(x+cardsLoc[0][0]*w-sz/2),
            		(int)(y+cardsLoc[0][1]*h-sz),
            		highlightAll, 
            		ViticultureId.Eye,
            		null,1.3,1.3))
            {
            	highlightAll.hitObject = pb.cards;
            }
  
        }
 
        }
        else if(G.pointInRect(highlightAll,br))
        {	highlightAll.hitCode = ViticultureId.ShowPlayerBoard;
        }
        zoomer.drawMagnifier(gc,highlightAll,pl.playerBox,0.08,0.99,0.98,pl.displayRotation);
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
    	GC.setFont(g,largeBoldFont());
    	ViticultureChip chip = ViticultureChip.getChip(obj);
    	if(chip!=null)
    		{ ViticultureBoard bb = getActiveBoard();
    		  if(bb!=null)
    			  {chip.drawChip(g,this,CELLSIZE*2, xp, yp,
    					  (bb.getState()!=ViticultureState.Puzzle)?ViticultureChip.BACK:null);
    			  }
    		}
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}    

    Image scaled = null;
    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc,ViticultureBoard gb,Rectangle brect)
    { // erase
   
      
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     // G.tileImage(gc,ViticultureChip.backgroundTile.image, fullRect, this);   
     ViticultureChip.backgroundTile.getImage().stretchImage(gc, fullRect);  
     GC.setRotatedContext(gc,boardRect,null,contextRotation);

     if(reviewBackground)
      {	 
       ViticultureChip.backgroundReviewTile.image.tileImage(gc,brect);   
      }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
      setDisplayParameters(gb,brect);
      
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      if(remoteViewer<0)
      {
      if(brect.equals(boardRect))
      {
      scaled = ViticultureChip.board.image.centerScaledImage(gc,brect,scaled);
      }
      else
      {
      // note that this uses centerImage rather than centerScaledImage, so we 
      // don't randomize scaled images during zooms
      ViticultureChip.board.image.centerImage(gc,brect);
      }
      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      //bb.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);
      
      if(BACKGROUND_OPTIMIZATION) 
      { drawBackgroundElements(gc,null,gb,brect,null,null,dummyTargets); 
      backgroundDigest = backgroundDigest(gb);
    }}
      GC.unsetRotatedContext(gc,null);
      
    }
    private Hashtable<ViticultureCell,Viticulturemovespec> dummyTargets = new Hashtable<ViticultureCell,Viticulturemovespec>();
	ViticultureCell backgroundCells[];
	ViticultureCell deckCells[];
	
	private long backgroundDigest = 0;
	static long FIXEDRANDOM = 22684745;
    public long backgroundDigest(ViticultureBoard gb)
    {	Random r = new Random(FIXEDRANDOM);
    	long v = 0;
    	// include the animation height in the digest so the digest will change when an animation
    	// starts or finishes.
    	for(ViticultureCell c : backgroundCells) { v ^= DigestA(r,c); }
    	for(ViticultureCell c : deckCells) { v ^= DigestA(r,c);  }
    	
    	for(int i=0;i<gb.nPlayers();i++)
    	{
    		PlayerBoard pb = gb.pbs[i];
    		v ^= DigestA(r,pb.vines);
    		
            v ^= DigestA(r,pb.redGrape);
            v ^= DigestA(r,pb.whiteGrape);
            v ^= DigestA(r,pb.redWine);
            v ^= DigestA(r,pb.whiteWine);
            v ^= DigestA(r,pb.roseWine);
            v ^= DigestA(r,pb.champagne);
            v ^= DigestA(r,pb.buildable);
            v ^= DigestA(r,pb.stars);

    	}
    	return(v);
    }
    private long DigestA(Random r,ViticultureCell ...c)
    {	long val = 0;
    	for(ViticultureCell c1 : c)
    	{	val ^= c1.Digest(r)+123*c1.activeAnimationHeight();
    	}
    	return(val);
    }
    
    private boolean pendingFullRepaint = false;
    public void drawBackgroundElements(Graphics gc,commonPlayer pl,ViticultureBoard gb,Rectangle brect,HitPoint hitBoard,HitPoint highlightAll,
    						Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {	
     	// we have some elements as "background" that are animated when placed.
    	// we need to inhibit the animation drawing bit while we draw the background
    	// or else the background will be permanantly frozen with missing elements.
    	pendingFullRepaint = !spritesIdle();
    	drawBoardBackgroundElements(gc,pl,gb,brect,hitBoard,highlightAll,targets);
    	GC.unsetRotatedContext(gc,hitBoard);
        for(int i=0;i<gb.nPlayers();i++)
       	{  commonPlayer pl0 = getPlayerOrTemp(i);
       	   pl0.setRotatedContext(gc, null,false);
       	   drawPlayerBackgroundElements(gc,gb,pl0,gb.pbs[i],playerBoardRects[i],playerSideRects[i],targets);
           
       	   pl0.setRotatedContext(gc, null,true);
        }
        GC.setRotatedContext(gc,boardRect,hitBoard,-Math.PI/2);
    	
    }
    public void drawBoardBackgroundElements(Graphics gc,commonPlayer pl,ViticultureBoard gb,Rectangle brect,
    		HitPoint hitBoard,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
    	drawArray(gc,true,ViticultureState.Puzzle,pl,gb,brect,highlightAll,highlightAll,targets,decksLoc, 	0.004, 0.007, 
    			ViticultureChip.TOP,
        		backgroundCells);
    	
    	drawArray(gc,true,ViticultureState.Puzzle,pl,gb,brect,hitBoard,highlightAll,targets,yokeLocRest,0.2,0.0,null,
		gb.yokeRedGrape,gb.yokeWhiteGrape,
   		gb.yokeRedWine,gb.yokeWhiteWine,
 		gb.yokeRoseWine,gb.yokeChampaign);
 		

        
    	// these must correspond to deckCells
        drawArray(gc,true,ViticultureState.Puzzle,pl,gb,brect,hitBoard,highlightAll,targets,greenLoc,0.004,0.007,ViticultureChip.BACK,gb.greenCards);
        drawArray(gc,true,ViticultureState.Puzzle,pl,gb,brect,hitBoard,highlightAll,targets,yellowLoc,0.004,0.007,ViticultureChip.BACK,gb.yellowCards);
        drawArray(gc,true,ViticultureState.Puzzle,pl,gb,brect,hitBoard,highlightAll,targets,purpleLoc,0.004,0.007,ViticultureChip.BACK,gb.purpleCards);
        drawArray(gc,true,ViticultureState.Puzzle,pl,gb,brect,hitBoard,highlightAll,targets,blueLoc,0.004,0.007,ViticultureChip.BACK,gb.blueCards);
        drawArray(gc,true,ViticultureState.Puzzle,pl,gb,brect,hitBoard,highlightAll,targets,structureLoc,0.004,0.007,ViticultureChip.BACK,gb.structureCards);
    }
    public void drawPlayerBackgroundElements(Graphics gc,ViticultureBoard gb,commonPlayer pl,PlayerBoard pb,Rectangle br,Rectangle sr,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
     ViticultureChip.playermat.image.centerImage(gc, br);
    	if(sr!=null) { ViticultureChip.structuresidemat.image.centerImage(gc, sr); }
        drawVines(gc,gb,br,pl,pb,ViticultureState.Puzzle,null,null,null,targets);
        drawArray(gc,false,ViticultureState.Puzzle,pl,gb,br,null,null,targets,redGrapeLoc,	0.0, 0.0,  ViticultureChip.INDEX,pb.redGrape);
        drawArray(gc,false,ViticultureState.Puzzle,pl,gb,br,null,null,targets,whiteGrapeLoc,0.0, 0.0, ViticultureChip.INDEX,pb.whiteGrape);
        drawArray(gc,false,ViticultureState.Puzzle,pl,gb,br,null,null,targets,redWineLoc,   0.0, 0.0, ViticultureChip.INDEX,pb.redWine);
        drawArray(gc,false,ViticultureState.Puzzle,pl,gb,br,null,null,targets,whiteWineLoc, 0.0, 0.0, ViticultureChip.INDEX,pb.whiteWine);
        drawArray(gc,false,ViticultureState.Puzzle,pl,gb,br,null,null,targets,roseWineLoc,  0.0, 0.0, ViticultureChip.INDEX,pb.roseWine);
        drawArray(gc,false,ViticultureState.Puzzle,pl,gb,br,null,null,targets,champaignLoc, 0.0, 0.0, ViticultureChip.INDEX,pb.champagne);
        drawArray(gc,false,ViticultureState.Puzzle,pl,gb,br,null,null,targets,buildingsLoc, 0.1, 0.0,null,pb.buildable);
        drawArray(gc,false,ViticultureState.Puzzle,pl,gb,br,null,null,targets,starsTrackLoc, 0.5, 0.0, null,pb.stars);

    }
    
    public void drawFixedElements(Graphics gc)
    {
    	drawFixedElements(gc,disB(gc),boardRect);
    }
    

    private double workerLoc[][] = {
    		{0,0,0},	
    		{0,0,0},	
    		{0,0,0},	
    		{0,0,0},	
    };
    public void drawWorkerArray(Graphics gc,commonPlayer pl,ViticultureBoard gb,Rectangle brect,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets,
    		double pos[],ViticultureCell workers[])
    {
    	double step = 0.0235;
    	for(int i=0,last=workerLoc.length-1;i<=last;i++) 
    		{ workerLoc[i][0] = pos[0]+(i*step)+((i==last)?-0.0035:0);
    		  workerLoc[i][1]= pos[1];
    		  workerLoc[i][2]= pos[2];
    		}
    	drawArray(gc,true,ViticultureState.Play,pl,gb,brect,highlight,highlightAll,targets,workerLoc,0.2,0.0,null,workers);
    }
    public void drawResidualTrack(Graphics gc,commonPlayer pl,ViticultureBoard gb,Rectangle brect,
    		HitPoint highlight,Hashtable<ViticultureCell,Viticulturemovespec>targets,
    		ViticultureCell residual[])
    {
    	drawArray(gc,true,ViticultureState.Play,pl,gb,brect,highlight,highlight,targets,residualLocs,0.1,0.15,null,residual);
    }
    
    //
    // display the scoring track.  The actual score is kept
    // as integers in PlayerBoard
    //
    public void drawScoringTrack(Graphics gc,ViticultureBoard gb,Rectangle brect,
    		HitPoint highlight,Hashtable<ViticultureCell,Viticulturemovespec>targets,
    		ViticultureCell score[])
    {
    	double ll[] = {0.222 ,0.935};
    	double lr[] = {0.956,0.935 };
    	int w = G.Width(brect);
    	int h = G.Height(brect);
    	int x = G.Left(brect);
    	int y = G.Top(brect);
    	if(BX_CELLS!=30)
        {
        int spare = w/BX_CELLS;	// this is an adjustment because the cell coordinates were derived
        x -= spare*(30-BX_CELLS)/2;				// based on a board that was 30 cells wide
        w = (int)(w*30*0.993/BX_CELLS);
        }

    	int max = score.length;
    	int scale = (int)(0.03*w);
    	gb.reInit(score);
    	if(UsingAutoma()) {
    		score[Math.min(score.length-1, gb.automaScore-MIN_SCORE)].addChip(ViticultureChip.getChip(ChipType.Post,gb.automaColor)); 
    	}
       	for(PlayerBoard pb : gb.pbs) {  score[pb.score-MIN_SCORE].addChip(pb.getScoreMarker()); } 
       	for(int i=0;i<max;i++)
    	{
    		int xp = (int)(x+w*G.interpolateD((double)i/max,ll[0],lr[0]));
    		int yp = (int)(y+h*G.interpolateD((double)i/max,ll[1],lr[1]));
    		boolean canHit = gb.legalToHit(score[i],targets);
			if(canHit)
					{ StockArt.SmallO.drawChip(gc,this,scale,xp,yp,null);
					}
			score[i].drawStack(gc,this,canHit ? highlight : null,scale,
					xp,
					yp,
					0,0.0,0.2 ,null);
    		
    	}
    }
    //
    // draw the startup track.  This is the actual data, rooster objects
    // mark the players current position in the year
    //
    private void drawRoosterArray(Graphics gc,ViticultureBoard gb,Rectangle brect,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets,
    		ViticultureCell workers[][])
    {	double msgLoc[] = {0.05,0.24};
    	double ul[] = {0.08,0.33};
    	double ur[] = {0.2,0.32};
    	double ll[] = {0.09,0.6};
    	double lr[] = {0.21,0.59};
    	PlayerBoard pb = gb.getCurrentPlayerBoard();
    	commonPlayer pl = getPlayerOrTemp(pb.boardIndex);
    	int w = G.Width(brect);
    	int h = G.Height(brect);
    	int x = G.Left(brect);
    	int y = G.Top(brect);

    	if(BX_CELLS!=30)
        {
        int spare = w/BX_CELLS;	// this is an adjustment because the cell coordinates were derived
        x -= spare*(30-BX_CELLS)/2;				// based on a board that was 30 cells wide
        w += spare*(30-BX_CELLS);
        }

    	int scale = (int)(0.03*w);
    	int nrows = workers.length;
    	
    	if(gb.year>0) 
    		{ String season = gb.testOption(Option.ContinuousPlay)
    								? s.get(TimeYearDescription,gb.year)
    								: s.get(TimeLineDescription,s.get(seasons[gb.season]),""+gb.year);
    		  GC.setFont(gc,largeBoldFont());
    		  GC.setColor(gc,Color.black);
    		  int xp = (int)(x+msgLoc[0]*w);
    		  int yp = (int)(y+msgLoc[1]*h);
    		  GC.setRotation(gc, -0.057,xp,yp);
    		  GC.Text(gc,true, xp,yp,(int)(w*0.13),(int)(w*0.02),Color.black,null,season);
    		  GC.setRotation(gc, 0.057,xp,yp);
    		}
    	boolean select = (gb.resetState==ViticultureState.SelectWakeup);
    	for(int row = nrows-1; row>=0; row--)
    	{	ViticultureCell rowCell[] = workers[row];
    		int ncols = rowCell.length;
    		double fr = (double)row/nrows;
    		int llx = (int)(x+w*G.interpolateD(fr, ul[0],ll[0]));
    		int lly = (int)(y+h*G.interpolateD(fr, ul[1],ll[1]));
    		int rrx = (int)(x+w*G.interpolateD(fr, ur[0],lr[0]));
    		int rry = (int)(y+h*G.interpolateD(fr, ur[1],lr[1]));
    		for(int col=ncols-1; col>=0; col--)
    		{
    			ViticultureCell c = rowCell[col];
        		boolean canSelect = gb.legalToHit(c,targets);
        		// special tweak for selecting new rooster positions, 
        		// slide the old off the the right a little so the 
        		// bonuses can be seen. 
        		double fry = ((double)col+(select && (col==3) && (row<6) ? 0.5: 0))/ncols;
    			int xp = G.interpolate(fry, llx,rrx);
    			int yp = G.interpolate(fry, lly, rry);
    			boolean canSelectThis = canSelect;
    			int thisScale = scale;
    			ViticultureChip extra = null;
    			for(PlayerBoard p : gb.pbs)
    			{	
    				ViticultureCell ac = p.activeWakeupPosition;
    				ViticultureCell ap = p.wakeupPosition;
    				if(ac!=ap)
    				{	if(ac==c) { extra = p.getRooster();thisScale = scale*2/3; }
    					if(c==ap)
    					{
    					thisScale = scale*3/4;
    					}
    				}
    			}
    			if(canSelectThis)
    				{ StockArt.SmallO.drawChip(gc,this,scale,xp,yp,null);
    				}
    			
    			boolean hit = drawStack(gc,gb.resetState,pl,c,canSelectThis ? highlight : null,highlightAll,thisScale,
    					xp,
    					yp,
    					0,0,0,null);
    			ViticultureChip top = c.topChip();
    			if((top!=null) && (top.color==pb.color))
    			{
    				GC.frameRect(gc,Color.blue,xp-scale/2,yp-scale/2,scale,scale);
    			}
    			if(extra!=null)
    				{
    				
    					extra.drawChip(gc,this,scale*2/3,xp+scale/2, yp,null);
    				}

    			if((hit || c.isSelected())&&(highlight!=null)) 
    			{
    				highlight.awidth = scale;
    				highlight.spriteColor = Color.red;
    			}
    		}
    	}
    	
    	
    }
    
    static {
     	//
    	// locate the checkboxes on cards.  This static data is normally used,
    	// but you can include Loc.make in actual display functions when tuning
    	//
    	Loc.make(ChipType.YellowCard, 0, ViticultureId.Choice_0,0.6,-0.1,1,0.15);	// default boxes
       	Loc.make(ChipType.YellowCard, 0, ViticultureId.Choice_A,0.6,0.0,1,0.15);
       	Loc.make(ChipType.YellowCard, 0, ViticultureId.Choice_B,0.6,-0.15,1,0.15);
 
    	Loc.make(ChipType.YellowCard, 0, ViticultureId.Choice_Done,0.6,0.15,1,0.15);
    	
    	Loc.make(ChipType.YellowCard, 0, ViticultureId.Choice_A,-0.43,0.2,0.15);
       	Loc.make(ChipType.YellowCard, 0, ViticultureId.Choice_B,-0.43,0.25,0.15);
       	Loc.make(ChipType.YellowCard, 0, ViticultureId.Choice_C,-0.43,0.3,0.15);
     
       	// surveyor
    	Loc.make(ChipType.YellowCard, 1, ViticultureId.Choice_A,-0.48,0.19,0.15);
       	Loc.make(ChipType.YellowCard, 1, ViticultureId.Choice_B,-0.48,0.30,0.15);

       	// broker
    	Loc.make(ChipType.YellowCard, 2, ViticultureId.Choice_A,-0.38,0.236,0.15);
       	Loc.make(ChipType.YellowCard, 2, ViticultureId.Choice_B,-0.38,0.305,0.15);
       	
       	// wine critic
    	Loc.make(ChipType.YellowCard, 3, ViticultureId.Choice_A,-0.49,0.236,0.15);
       	Loc.make(ChipType.YellowCard, 3, ViticultureId.Choice_B,-0.49,0.31,0.15);

       	// blacksmith
    	Loc.make(ChipType.YellowCard, 4, ViticultureId.Choice_A,-0.49,0.236,0.15);

    	// contractor
    	Loc.make(ChipType.YellowCard, 5, ViticultureId.Choice_A,-0.39,0.205,0.15);
       	Loc.make(ChipType.YellowCard, 5, ViticultureId.Choice_B,-0.39,0.265,0.15);
       	Loc.make(ChipType.YellowCard, 5, ViticultureId.Choice_C,-0.39,0.32,0.15);
    	
      	// tour guide critic
    	Loc.make(ChipType.YellowCard, 6, ViticultureId.Choice_A,-0.33,0.245,0.15);
       	Loc.make(ChipType.YellowCard, 6, ViticultureId.Choice_B,-0.33,0.315,0.15);

      	// novice guide
    	Loc.make(ChipType.YellowCard, 7, ViticultureId.Choice_A,-0.33,0.245,0.15);
       	Loc.make(ChipType.YellowCard, 7, ViticultureId.Choice_B,-0.33,0.315,0.15);

      	// uncertified broker
    	Loc.make(ChipType.YellowCard, 8, ViticultureId.Choice_A,-0.4,0.24,0.15);
       	Loc.make(ChipType.YellowCard, 8, ViticultureId.Choice_B,-0.4,0.315,0.15);

      	// planter
    	Loc.make(ChipType.YellowCard, 9, ViticultureId.Choice_A,-0.53,0.18,0.15);
       	Loc.make(ChipType.YellowCard, 9, ViticultureId.Choice_B,-0.40,0.28,0.15);

      	// buyer
    	Loc.make(ChipType.YellowCard, 10, ViticultureId.Choice_A,-0.41,0.18,0.15);
       	Loc.make(ChipType.YellowCard, 10, ViticultureId.Choice_B,-0.41,0.29,0.15);

      	// landscaper
    	Loc.make(ChipType.YellowCard, 11, ViticultureId.Choice_A,-0.5,0.24,0.15);
       	Loc.make(ChipType.YellowCard, 11, ViticultureId.Choice_B,-0.5,0.315,0.15);
       	
      	// architect
    	Loc.make(ChipType.YellowCard, 12, ViticultureId.Choice_A,-0.43,0.17,0.15);
       	Loc.make(ChipType.YellowCard, 12, ViticultureId.Choice_B,-0.43,0.27,0.15);

      	// uncertified architect
    	Loc.make(ChipType.YellowCard, 13, ViticultureId.Choice_A,-0.48,0.19,0.15);
       	Loc.make(ChipType.YellowCard, 13, ViticultureId.Choice_B,-0.48,0.28,0.15);

       	// patron
    	Loc.make(ChipType.YellowCard, 14, ViticultureId.Choice_A,-0.38,0.245,0.15);
       	Loc.make(ChipType.YellowCard, 14, ViticultureId.Choice_B,-0.38,0.313,0.15);
   	
       	// auctioneer
    	Loc.make(ChipType.YellowCard, 15, ViticultureId.Choice_A,-0.45,0.245,0.15);
       	Loc.make(ChipType.YellowCard, 15, ViticultureId.Choice_B,-0.45,0.313,0.15);
   	
       	// entertainer
       	Loc.make(ChipType.YellowCard, 16, ViticultureId.Choice_A,-0.45,0.22,0.15);
       	Loc.make(ChipType.YellowCard, 16, ViticultureId.Choice_B,-0.45,0.31,0.15);

       	// vendor
    	Loc.make(ChipType.YellowCard, 17, ViticultureId.Choice_A,-0.43,0.26,0.15);

      	// handyman
    	Loc.make(ChipType.YellowCard, 18, ViticultureId.Choice_A,-0.48,0.23,0.15);

       	// horticulturist
       	Loc.make(ChipType.YellowCard, 19, ViticultureId.Choice_A,-0.45,0.19,0.15);
     	Loc.make(ChipType.YellowCard, 19, ViticultureId.Choice_B,-0.52,0.32, 0.15);

      	// peddler
    	Loc.make(ChipType.YellowCard, 20, ViticultureId.Choice_A,-0.45,0.26,0.15);
    	
      	// banker
    	Loc.make(ChipType.YellowCard, 21, ViticultureId.Choice_A,-0.47,0.26,0.15);

      	// overseer
    	Loc.make(ChipType.YellowCard, 22, ViticultureId.Choice_A,-0.455,0.325,0.15);

     	// importer
    	Loc.make(ChipType.YellowCard, 23, ViticultureId.Choice_A,-0.455,0.325,0.15);

       	// sharecropper
       	Loc.make(ChipType.YellowCard, 24, ViticultureId.Choice_A,-0.45,0.2,0.15);
       	Loc.make(ChipType.YellowCard, 24, ViticultureId.Choice_B,-0.45,0.3,0.15);

     	// grower
    	Loc.make(ChipType.YellowCard, 25, ViticultureId.Choice_A,-0.455,0.325,0.15);

       	// negotiator
       	Loc.make(ChipType.YellowCard, 26, ViticultureId.Choice_A,-0.43,0.245,0.15);
       	Loc.make(ChipType.YellowCard, 26, ViticultureId.Choice_B,-0.43,0.31,0.15);

       	// cultivator
       	Loc.make(ChipType.YellowCard, 27, ViticultureId.Choice_A,-0.52,0.245,0.15);

      	// homesteader
       	Loc.make(ChipType.YellowCard, 28, ViticultureId.Choice_A,-0.49,0.175,0.15);
       	Loc.make(ChipType.YellowCard, 28, ViticultureId.Choice_B,-0.49,0.26,0.15);

       	// planner
       	Loc.make(ChipType.YellowCard, 29, ViticultureId.Choice_A,-0.5,0.32,0.15);

       	// agriculturist
       	Loc.make(ChipType.YellowCard, 30, ViticultureId.Choice_A,-0.48,0.24,0.15);

       	// swindler
       	Loc.make(ChipType.YellowCard, 31, ViticultureId.Choice_A,-0.45,0.325,0.15);

       	// producer
       	Loc.make(ChipType.YellowCard, 32, ViticultureId.Choice_A,-0.45,0.325,0.15);

       	// organizer
       	Loc.make(ChipType.YellowCard, 33, ViticultureId.Choice_A,-0.49,0.325,0.15);

      	// sponsor
       	Loc.make(ChipType.YellowCard, 34, ViticultureId.Choice_A,-0.33,0.175,0.15);
       	Loc.make(ChipType.YellowCard, 34, ViticultureId.Choice_B,-0.33,0.25,0.15);

      	// artisan
    	Loc.make(ChipType.YellowCard, 35, ViticultureId.Choice_A,-0.48,0.16,0.15);
       	Loc.make(ChipType.YellowCard, 35, ViticultureId.Choice_B,-0.48,0.235,0.15);
       	Loc.make(ChipType.YellowCard, 35, ViticultureId.Choice_C,-0.48,0.32,0.15);
 
      	// stonemason
    	Loc.make(ChipType.YellowCard, 36, ViticultureId.Choice_A,-0.51,0.26,0.15);
 
      	// volunteer crew
    	Loc.make(ChipType.YellowCard, 37, ViticultureId.Choice_A,-0.43,0.285,0.15);

      	// wedding party
    	Loc.make(ChipType.YellowCard, 38, ViticultureId.Choice_A,-0.43,0.32,0.15);

      	// merchant
      	Loc.make(ChipType.BlueCard, 1, ViticultureId.Choice_A,-0.49,0.21,0.15);
       	Loc.make(ChipType.BlueCard, 1, ViticultureId.Choice_B,-0.49,0.31,0.15);

     	// crusher
       	Loc.make(ChipType.BlueCard, 2, ViticultureId.Choice_A,-0.44,0.21,0.15);
       	Loc.make(ChipType.BlueCard, 2, ViticultureId.Choice_B,-0.44,0.29,0.15);

     	// judge
       	Loc.make(ChipType.BlueCard, 3, ViticultureId.Choice_A,-0.44,0.21,0.15);
       	Loc.make(ChipType.BlueCard, 3, ViticultureId.Choice_B,-0.44,0.29,0.15);

       	// oenologist
       	Loc.make(ChipType.BlueCard, 4, ViticultureId.Choice_A,-0.47,0.21,0.15);
       	Loc.make(ChipType.BlueCard, 4, ViticultureId.Choice_B,-0.47,0.29,0.15);

       	// marketer
       	Loc.make(ChipType.BlueCard, 5, ViticultureId.Choice_A,-0.45,0.22,0.15);
       	Loc.make(ChipType.BlueCard, 5, ViticultureId.Choice_B,-0.45,0.31,0.15);

       	// crush expert
       	Loc.make(ChipType.BlueCard, 6, ViticultureId.Choice_A,-0.45,0.22,0.15);
       	Loc.make(ChipType.BlueCard, 6, ViticultureId.Choice_B,-0.45,0.31,0.15);

       	// uncertified teacher
       	Loc.make(ChipType.BlueCard, 7, ViticultureId.Choice_A,-0.48,0.21,0.15);
       	Loc.make(ChipType.BlueCard, 7, ViticultureId.Choice_B,-0.48,0.30,0.15);
       	
       	// teacher
       	Loc.make(ChipType.BlueCard, 8, ViticultureId.Choice_A,-0.45,0.23,0.15);
       	Loc.make(ChipType.BlueCard, 8, ViticultureId.Choice_B,-0.45,0.32,0.15);

       	// benefactor
       	Loc.make(ChipType.BlueCard, 9, ViticultureId.Choice_A,-0.42,0.21,0.15);
       	Loc.make(ChipType.BlueCard, 9, ViticultureId.Choice_B,-0.42,0.30,0.15);

       	// assessor
       	Loc.make(ChipType.BlueCard, 10, ViticultureId.Choice_A,-0.5,0.21,0.15);
       	Loc.make(ChipType.BlueCard, 10, ViticultureId.Choice_B,-0.5,0.30,0.15);

       	// queen
       	Loc.make(ChipType.BlueCard, 11, ViticultureId.Choice_A,-0.44,0.32,0.15);

      	// harvestor
       	Loc.make(ChipType.BlueCard, 12, ViticultureId.Choice_A,-0.28,0.245,0.15);
       	Loc.make(ChipType.BlueCard, 12, ViticultureId.Choice_B,-0.28,0.32,0.15);

       	// professor
		Loc.make(ChipType.BlueCard, 13, ViticultureId.Choice_A,-0.4,0.21,0.15);
     	Loc.make(ChipType.BlueCard, 13, ViticultureId.Choice_B,-0.4,0.327,0.15);

      	// master vintner
       	Loc.make(ChipType.BlueCard, 14, ViticultureId.Choice_A,-0.44,0.22,0.15);
       	Loc.make(ChipType.BlueCard, 14, ViticultureId.Choice_B,-0.44,0.33,0.15);

      	// uncertified oenologist
       	Loc.make(ChipType.BlueCard, 15, ViticultureId.Choice_A,-0.49,0.20,0.15);
       	Loc.make(ChipType.BlueCard, 15, ViticultureId.Choice_B,-0.49,0.31,0.15);

       	// promoter
       	Loc.make(ChipType.BlueCard, 16, ViticultureId.Choice_A,-0.43,0.28,0.15);
       	
       	// promoter
       	Loc.make(ChipType.BlueCard, 17, ViticultureId.Choice_A,-0.45,0.33,0.15);

      	// uncertified oenologist
       	Loc.make(ChipType.BlueCard, 18, ViticultureId.Choice_A,-0.43,0.21,0.15);
       	Loc.make(ChipType.BlueCard, 18, ViticultureId.Choice_B,-0.43,0.31,0.15);
       	
       	// innkeeper
       	Loc.make(ChipType.BlueCard, 19, ViticultureId.Choice_A,-0.45,0.235,0.15);

     	// jack of all trades
       	Loc.make(ChipType.BlueCard, 20, ViticultureId.Choice_A,-0.39,0.245,0.15);
       	Loc.make(ChipType.BlueCard, 20, ViticultureId.Choice_B,-0.39,0.31,0.15);
      	Loc.make(ChipType.BlueCard, 20, ViticultureId.Choice_C,-0.24,0.35,0.15);
       	Loc.make(ChipType.BlueCard, 20, ViticultureId.Choice_HarvestFirst,1.39,0.245,0.15);
       	Loc.make(ChipType.BlueCard, 20, ViticultureId.Choice_MakeWineFirst,1.39,0.31,0.15);
      	Loc.make(ChipType.BlueCard, 20, ViticultureId.Choice_FillWineFirst,1.39,0.35,0.15);

      	// politician
       	Loc.make(ChipType.BlueCard, 21, ViticultureId.Choice_A,-0.45,0.32,0.15);

      	// supervisor
       	Loc.make(ChipType.BlueCard, 22, ViticultureId.Choice_A,-0.45,0.32,0.15);

    	// uncertified oenologist
       	Loc.make(ChipType.BlueCard, 23, ViticultureId.Choice_A,-0.39,0.20,0.15);
       	Loc.make(ChipType.BlueCard, 23, ViticultureId.Choice_B,-0.39,0.27,0.15);

      	// reaper
       	Loc.make(ChipType.BlueCard, 24, ViticultureId.Choice_A,-0.44,0.255,0.15);

      	// motivator
       	Loc.make(ChipType.BlueCard, 25, ViticultureId.Choice_A,-0.49,0.28,0.15);

     	// bottler
       	Loc.make(ChipType.BlueCard, 26, ViticultureId.Choice_A,-0.48,0.335,0.15);

     	// craftsman
       	Loc.make(ChipType.BlueCard, 27, ViticultureId.Choice_A,-0.25,0.209,0.13);
       	Loc.make(ChipType.BlueCard, 27, ViticultureId.Choice_B,-0.25,0.275,0.13);
      	Loc.make(ChipType.BlueCard, 27, ViticultureId.Choice_C,-0.25,0.34,0.13);

     	// homesteader
       	Loc.make(ChipType.BlueCard, 28, ViticultureId.Choice_A,-0.43,0.17,0.15);
       	Loc.make(ChipType.BlueCard, 28, ViticultureId.Choice_B,-0.43,0.24,0.15);
      	Loc.make(ChipType.BlueCard, 28, ViticultureId.Choice_C,-0.43,0.315,0.15);

     	// laborer
       	Loc.make(ChipType.BlueCard, 29, ViticultureId.Choice_A,-0.40,0.195,0.15);
       	Loc.make(ChipType.BlueCard, 29, ViticultureId.Choice_B,-0.40,0.26,0.15);

    	// designer
       	Loc.make(ChipType.BlueCard, 30, ViticultureId.Choice_A,-0.42,0.32,0.15);

     	// governess
       	Loc.make(ChipType.BlueCard, 31, ViticultureId.Choice_A,-0.48,0.225,0.15);
       	Loc.make(ChipType.BlueCard, 31, ViticultureId.Choice_B,-0.48,0.315,0.15);

     	// manager
       	Loc.make(ChipType.BlueCard, 32, ViticultureId.Choice_A,-0.43,0.325,0.15);

     	// zymologist
       	Loc.make(ChipType.BlueCard, 33, ViticultureId.Choice_A,-0.45,0.33,0.15);

     	// noble
       	Loc.make(ChipType.BlueCard, 34, ViticultureId.Choice_A,-0.42,0.245,0.15);
       	Loc.make(ChipType.BlueCard, 34, ViticultureId.Choice_B,-0.42,0.315,0.15);

      	// governor
       	Loc.make(ChipType.BlueCard, 35, ViticultureId.Choice_A,-0.49,0.34,0.15);
       	
      	// taster
       	Loc.make(ChipType.BlueCard, 36, ViticultureId.Choice_A,-0.49,0.27,0.15);
       	
     	// caravan
       	Loc.make(ChipType.BlueCard, 37, ViticultureId.Choice_A,-0.45,0.235,0.15);

      	// guest speaker
       	Loc.make(ChipType.BlueCard, 38, ViticultureId.Choice_A,-0.46,0.33,0.15);
       	      	
      	Loc.make(ChipType.ChoiceCard, 1, ViticultureId.Choice_A,-0.28,0.237, 0.15);	// default dollar
      	Loc.make(ChipType.ChoiceCard, 1, ViticultureId.Choice_B,-0.28,0.31, 0.15);
      	
      	Loc.make(ChipType.ChoiceCard, 2, ViticultureId.Choice_A,-0.33,0.23, 0.15);	// swindler
      	Loc.make(ChipType.ChoiceCard, 2, ViticultureId.Choice_B,-0.33,0.31, 0.15);
      	
		// 
		Loc.make(ChipType.ChoiceCard, 3, ViticultureId.Choice_A,-0.40,0.225, 0.15);	// banker
      	Loc.make(ChipType.ChoiceCard, 3, ViticultureId.Choice_B,-0.40,0.305, 0.15);
       	
		Loc.make(ChipType.ChoiceCard, 4, ViticultureId.Choice_A,-0.43,0.225, 0.15);	// buyer buy grape
      	Loc.make(ChipType.ChoiceCard, 4, ViticultureId.Choice_B,-0.43,0.305, 0.15);

		Loc.make(ChipType.ChoiceCard, 5, ViticultureId.Choice_A,-0.47,0.225, 0.15);	// buyer sell grape
      	Loc.make(ChipType.ChoiceCard, 5, ViticultureId.Choice_B,-0.47,0.305, 0.15);

      	
		Loc.make(ChipType.ChoiceCard, 6, ViticultureId.Choice_A,-0.44,0.225, 0.15);	// 
      	Loc.make(ChipType.ChoiceCard, 6, ViticultureId.Choice_B,-0.44,0.305, 0.15);

		Loc.make(ChipType.ChoiceCard, 7, ViticultureId.Choice_A,-0.09,0.23, 0.15);	// negotiator
      	Loc.make(ChipType.ChoiceCard, 7, ViticultureId.Choice_B, 0.13,0.23, 0.15);
		Loc.make(ChipType.ChoiceCard, 7, ViticultureId.Choice_C,-0.32,0.33, 0.15);	// 
      	Loc.make(ChipType.ChoiceCard, 7, ViticultureId.Choice_D,-0.12,0.33, 0.15);

		Loc.make(ChipType.ChoiceCard, 8, ViticultureId.Choice_A,-0.4,0.225, 0.15);	// lose a yellow
		Loc.make(ChipType.ChoiceCard, 8, ViticultureId.Choice_B,-0.4,0.30, 0.15);	// give a vp

		Loc.make(ChipType.ChoiceCard, 9, ViticultureId.Choice_A,-0.38,0.225, 0.15);	// negotiator sell something
		Loc.make(ChipType.ChoiceCard, 9, ViticultureId.Choice_B,-0.38,0.30, 0.15);	// give a vp
		Loc.make(ChipType.ChoiceCard, 10, ViticultureId.Choice_A,-0.42,0.225, 0.15);	// retrieve grande
      	Loc.make(ChipType.ChoiceCard, 10, ViticultureId.Choice_B,-0.42,0.31, 0.15);

		Loc.make(ChipType.ChoiceCard, 11, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico payoff
      	Loc.make(ChipType.ChoiceCard, 11, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 12, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico green
      	Loc.make(ChipType.ChoiceCard, 12, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 13, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico structure
      	Loc.make(ChipType.ChoiceCard, 13, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 14, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico yellow
      	Loc.make(ChipType.ChoiceCard, 14, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 15, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico purple
      	Loc.make(ChipType.ChoiceCard, 15, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 16, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico blue
      	Loc.make(ChipType.ChoiceCard, 16, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 17, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico wine
      	Loc.make(ChipType.ChoiceCard, 17, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 18, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico star
      	Loc.make(ChipType.ChoiceCard, 18, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 19, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico plant
      	Loc.make(ChipType.ChoiceCard, 19, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 20, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico plant
      	Loc.make(ChipType.ChoiceCard, 20, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 21, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico harvest
      	Loc.make(ChipType.ChoiceCard, 21, ViticultureId.Choice_B,-0.39,0.305, 0.15);

		Loc.make(ChipType.ChoiceCard, 22, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// farmer card or dollar for blue
      	Loc.make(ChipType.ChoiceCard, 22, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 23, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// farmer card or dollar for yellow
      	Loc.make(ChipType.ChoiceCard, 23, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 24, ViticultureId.Choice_A,-0.45,0.225, 0.15);	// farmer harvest 2 or dollar
      	Loc.make(ChipType.ChoiceCard, 24, ViticultureId.Choice_B,-0.45,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 25, ViticultureId.Choice_A,-0.41,0.225, 0.15);	// farmer card or vp
      	Loc.make(ChipType.ChoiceCard, 25, ViticultureId.Choice_B,-0.41,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 26, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// farmer card or star
      	Loc.make(ChipType.ChoiceCard, 26, ViticultureId.Choice_B,-0.39,0.305, 0.15);

		Loc.make(ChipType.ChoiceCard, 27, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico blue extra
      	Loc.make(ChipType.ChoiceCard, 27, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 28, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico yellow extra
      	Loc.make(ChipType.ChoiceCard, 28, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 29, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico star extra
      	Loc.make(ChipType.ChoiceCard, 29, ViticultureId.Choice_B,-0.39,0.305, 0.15);
		Loc.make(ChipType.ChoiceCard, 30, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico trade extra
     	Loc.make(ChipType.ChoiceCard, 30, ViticultureId.Choice_B,-0.39,0.305, 0.15);	// politico trade extra
		Loc.make(ChipType.ChoiceCard, 31, ViticultureId.Choice_A,-0.39,0.225, 0.15);	// politico plant extra
     	Loc.make(ChipType.ChoiceCard, 31, ViticultureId.Choice_B,-0.39,0.305, 0.15);	// politico plant extra

   		Loc.make(ChipType.PapaCard,0,ViticultureId.Choice_0,0.19,0.1,0.15);
   		Loc.make(ChipType.PapaCard,0,ViticultureId.Choice_A,0.17,0.24,0.05);
		Loc.make(ChipType.PapaCard,0,ViticultureId.Choice_B,0.33,0.24,0.05);


    }
    
    // pick one of the available worker types
    private void showWorkers(Graphics gc, ViticultureBoard gb, Rectangle br,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
    	PlayerBoard pb = gb.getCurrentPlayerBoard();
    	int nMeeples = 0;
    	ViticultureState state = gb.resetState;
    	int w = G.Width(br);
    	int step = Math.min((int)(G.Height(br)*0.3),w*3/10);
    	w = step*4;
    	int totalW = step*3;
    	int left = G.centerX(br)-step;
    	int top = G.centerY(br)-step;
    	int totalH = step*2+step/3;
    	
 
    	for(Enumeration<Viticulturemovespec> e = targets.elements(); e.hasMoreElements();)
    	{	
    		Viticulturemovespec m = e.nextElement();
    		while(m!=null) { nMeeples++; m=(Viticulturemovespec)m.next; }
    	}
    	int mstep = w/Math.max(6,nMeeples+5);
  	
    	Rectangle workerRect = new Rectangle(left,top,totalW,totalH);
   
    	ViticultureChip.Scrim.image.stretchImage(gc, workerRect);  
    	
 
       	if(G.pointInRect(highlightAll, workerRect)) { highlightAll.neutralize(); }

      	int csize = totalW/20;

      	if(gb.triggerCard!=null)
       	{
       		gb.triggerCard.drawChip(gc, this, w/10, left+totalW-csize*2, top+totalH-csize*5/2,highlightAll,ViticultureId.ShowBigChip,null);
       	}

    	GC.frameRect(gc, Color.black, workerRect);
    	GC.setFont(gc, largeBoldFont());
    	GC.Text(gc, true, left, top, totalW, step/5,Color.black,null,SelectWorkerMessage);
    	
    	int xleft = G.centerX(workerRect)-nMeeples*mstep/2;
    	int ytop = top+step/2;
   
    	
    	for(Enumeration<Viticulturemovespec> e = targets.elements(); e.hasMoreElements();)
    	{	
    		Viticulturemovespec m = e.nextElement();
    		while(m!=null) 
    		{ 	ViticultureChip chip = ViticultureChip.getChip(ChipType.values()[m.from_index],pb.color);
    			if(chip.drawChip(gc,this,mstep,xleft,ytop,highlight,ViticultureId.WineSelection,null))
    			{
    				highlight.hitObject = m;
    			}
    		   	int cost = gb.costOfWorker(pb,chip,state);
    			if(gb.movestackContains(m,gb.pendingMoves))
    			{
    				StockArt.Checkmark.drawChip(gc, this, step/5,xleft+step/5,ytop,null);
        			if(chip.type!=ChipType.Worker)
        				{
        				
        				ViticultureChip card = ViticultureChip.getChip(ChipType.WorkerCard,chip.type.name());
        				card.drawChip(gc, this,step*2,G.centerX(workerRect),ytop+step+step/10,null);
        				}
         			}
    			GC.Text(gc, true, xleft-mstep/2, ytop+step/8, mstep, step/4,Color.black,null,chip.type.name());
    			
    			loadCoins(pb.cashDisplay,cost);
    			if(cost>0)
				{
    				drawStack(gc,state,null, pb.cashDisplay,null,highlightAll, step*4/10,xleft,ytop+step/2, 0,0.3,0.00,null);
				}
    			else { 
 				GC.Text(gc,true,xleft-step/2,ytop+step*3/8,step,step/4,Color.black,null,s.get(FreeMessage));
				}

    			
  			xleft += mstep;
      			m = (Viticulturemovespec)m.next;
       		}
    	}

    	StockArt.FancyCloseBox.drawChip(gc, this, csize, left+totalW-csize,top+csize, highlightAll,ViticultureId.CloseOverlay,null);

    }
    //
    // pick other players.  this is used by a few cards that demand you
    // choose a few players to pick on.
    //
    private void showPlayers(Graphics gc, ViticultureBoard gb, Rectangle br,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
    	int nPlayers = 0;
    	for(PlayerBoard other : gb.pbs) { if(targets.get(other.roosterDisplay)!=null) { nPlayers++; }} 
    	int w = G.Width(br);
    	int step = w/7;	// max of 5 other players
    	int totalW = Math.max(3, (nPlayers+1))*step;
    	int totalH = step*3/2;
    	int left = G.centerX(br)-totalW/2;
    	int top = G.centerY(br)-step;
    	ViticultureState state = gb.resetState;
    	Rectangle playersRect = new Rectangle(left,top,totalW,totalH);
   
    	//G.tileImage(gc,ViticultureChip.Scrim.image,playersRect, this);   
    ViticultureChip.Scrim.image.stretchImage(gc, playersRect);  
    	
 
       	if(G.pointInRect(highlightAll, playersRect)) { highlightAll.neutralize(); }

      	int csize = step/5;
      	if(gb.triggerCard!=null)
       	{
       		gb.triggerCard.drawChip(gc, this, w/15, left+totalW-csize, top+totalH-csize*3/2,highlightAll,ViticultureId.ShowBigChip,null);
       	}

       	GC.frameRect(gc, Color.black, playersRect);
    	GC.setFont(gc, largeBoldFont());
    	boolean stealMode = state==ViticultureState.StealVisitorCard;
    	boolean stealColor = stealMode && gb.revision>=121;
    	String msg = stealColor ? Select1Card : stealMode ? Select1Player : Select3Players;

    	GC.Text(gc, true, left, top+csize/2, totalW-csize, step/5,Color.black,null,s.get(msg));
    	
    	int xleft = left+step;
    	int ytop = top+step/2;
    	for(PlayerBoard other : gb.pbs)
    	{	Viticulturemovespec canhit = targets.get(other.roosterDisplay);
    		if(canhit!=null) 
    		{	
        		Viticulturemovespec canhitNext = (Viticulturemovespec)canhit.next;
        		Viticulturemovespec canhitYellow = stealColor ? canhit.from_index==1 ? canhit : canhitNext : null;
        		Viticulturemovespec canhitBlue = stealColor ? canhit.from_index==2 ? canhit : canhitNext : null;
    			if(other.getRooster().drawChip(gc,this,step/3,xleft,ytop,stealColor ? null : highlight,ViticultureId.WineSelection,null,1.1,1.3))
    			{	
    				highlight.hitObject = canhit;
    			}
    			ViticultureCell cards = other.cards;
    			ViticultureCell cards2 = null;
 				IStack cardsIndex = new IStack();
				IStack cards2Index = new IStack();
				if(stealMode)
    			{	ViticultureCell selectedYellow = gb.cardDisplay;
    				ViticultureCell selectedBlue = gb.cardDisplay1;
    				selectedYellow.reInit();
    				selectedBlue.reInit();
    				for(int lim=cards.height(),i=0;i<lim;i++)
    				{
    					ViticultureChip card = cards.chipAtIndex(i);
    					if(card.type==ChipType.YellowCard) { selectedYellow.addChip(card); cardsIndex.push(lim);}
    					else if(card.type==ChipType.BlueCard) { selectedBlue.addChip(card); cards2Index.push(lim);} 
    					
    				}
    				cards = selectedYellow;
    				cards2 = selectedBlue;
    			}
    			
    			commonPlayer pl = getPlayerOrTemp(other.boardIndex);
       			drawStack(gc,gb.resetState,pl, other.cashDisplay,null,highlightAll,step/3,xleft+step/4,ytop,0,0.1,0.05,""+other.cash);
       			boolean twostacks = ((cards2!=null) && (cards.height()>0) && (cards2.height()>0));
       			double cardStep = twostacks ? 0.1 : 0.2;
     			if(cards.height()>0)
       			{	int xxleft = twostacks ? xleft-step/6 : xleft;
       				if(drawStack(gc,gb.resetState,pl, cards,stealColor ? highlight : null,highlightAll,step/3,
       						xxleft,ytop+step/2,
       						0,cardStep,0.05,ViticultureChip.BACK))
       				{	
       					highlight.hitObject = canhitYellow;
       					highlight.hit_index = cardsIndex.elementAt(highlight.hit_index);
       				}
       			if(stealColor && gb.movestackContains(canhitYellow,gb.pendingMoves))
       			{
       				StockArt.Checkmark.drawChip(gc, this, step/5,xxleft,ytop+step/2,null);
       			}
       			}
       			if(cards2!=null && cards2.height()>0)
       			{	int xxleft = twostacks?xleft+step/6 : xleft;
           			if(drawStack(gc,gb.resetState,pl, cards2,stealColor ? highlight : null,highlightAll,step/3,
           					xxleft,ytop+step/2,
           					0,cardStep,0.05,ViticultureChip.BACK))
         			{
         				highlight.hitObject = canhitBlue;
         				highlight.hit_index = cards2Index.elementAt(highlight.hit_index);
         			}
          			if(stealColor && gb.movestackContains(canhitBlue,gb.pendingMoves))
           			{
           				StockArt.Checkmark.drawChip(gc, this, step/5,xxleft,ytop+step/2,null);
           			}
            	}
       			
       		    if(!stealColor && gb.movestackContains(canhit,gb.pendingMoves))
       		    {
       		    	StockArt.Checkmark.drawChip(gc, this, step/5,xleft-step/5,ytop,null);
       		    }
    			xleft += step;
    		}
    	}
    	if(stealMode)
    	{	String smsg = (gb.getState()==ViticultureState.Confirm)
    						? DoStealMessage
    						: DontStealMessage;
           	if(GC.handleRoundButton(gc,new Rectangle(left+(int)(0.72*totalW),top+(int)(totalH*0.8),totalW/4,totalW/15),
    				highlight,s.get(smsg),rackBackGroundColor,null))
           		{
           			highlight.hitCode = GameId.HitDoneButton;
           		}
    	}
       	StockArt.FancyCloseBox.drawChip(gc, this, csize, left+totalW-csize,top+csize, highlightAll,ViticultureId.CloseOverlay,null);

    }
    private boolean doneButton(Graphics gc,Rectangle r,HitPoint hgh)
    {
    	Color bg = hgh==null ? dullBackgroundColor : rackBackGroundColor;
		return handleDoneButton(gc,r,hgh, HighlightColor, bg);
    }
    //
    // display a single card with choices to be resolved.
    //
    private void showCard(Graphics gc, ViticultureBoard gb, Rectangle brect,
    		HitPoint highlight,HitPoint highlightAll,
    		Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {	
       	ViticultureChip target = gb.cardBeingResolved;
       	PlayerBoard pb = gb.getCurrentPlayerBoard();
       	int cx = G.centerX(brect);
       	int cy = G.centerY(brect);
       	int w = G.Width(brect);
       	ViticultureState state = gb.resetState;
      	int szw = (int)(w*0.45);
       	int szh = (int)Math.min(G.Height(brect)*0.8,szw*3/2);
       	szw = szh*2/3;
       	int x = cx-szw;
       	int y = cy-szh/2;
       	Rectangle cardRect = new Rectangle(x,y,szw*2,szh);
       ViticultureChip.Scrim.image.stretchImage(gc, cardRect);  
       	
 
       	if(G.pointInRect(highlightAll, cardRect)) { highlightAll.neutralize(); }
       	
      	int csize = szw*2/20;

       	GC.frameRect(gc,Color.black,cardRect);
     	boolean showOrder = false;
      	switch(target.type)
    		{
    		case BlueCard:
    			// special UI used for only Laborer and Jack of All Trades, which
    			// allows the user to specify which of 2 actions to perform first
    			showOrder = gb.revision>=136
    						&& ((target==ViticultureChip.JackOfAllTradesCard)
    								||(target==ViticultureChip.LaborerCard));
    		//$FALL-THROUGH$
    		case ChoiceCard:
    		case YellowCard:
    			{
    			Loc doneLoc = Loc.find(ChipType.YellowCard,0,ViticultureId.Choice_Done);
       	       	Rectangle dr = new Rectangle(cx-(int)(szw*0.95),cy-(int)(szh*0.46),szw*9/10,szh);
    			
       			// draw the card
       			target.drawChip(gc, this, szw*8/9, cx-szw/2, cy, null);
    			
     			Rectangle extradone = doneLoc.getRect(dr);
     			
    			
     			switch(state)
     			{
     			case Sell1VPfor3:
     			default:
     				{
        			Loc doNothingLoc = Loc.find(ChipType.YellowCard,0,ViticultureId.Choice_0);
        			Rectangle donothing = doNothingLoc.getRect(dr);
        			if(target.type!=ChipType.ChoiceCard)
        				{if(GC.handleRoundButton(gc, donothing,highlight, DiscardCardMessage,
        					HighlightColor, gb.choice0.selected ? HighlightColor : rackBackGroundColor))
        			{
        				highlight.hitCode = ViticultureId.Choice_0;
        				highlight.hitObject = gb.choice0;
        			}}}
     
     			}
     			HitPoint hgh = (gb.DoneState() ? highlight : null);
    			doneButton(gc,extradone,hgh);

    	    	// craftsman
    	       	//Loc.make(ChipType.BlueCard, 27, ViticultureId.Choice_A,-0.25,0.209,0.13);
    	       	//Loc.make(ChipType.BlueCard, 27, ViticultureId.Choice_B,-0.25,0.275,0.13);
    	      	//Loc.make(ChipType.BlueCard, 27, ViticultureId.Choice_C,-0.25,0.34,0.13);
   			if(targets.get(gb.choiceA)!=null)
    				{
    				Loc ca = Loc.find(target.type,target.order,ViticultureId.Choice_A);
    				StockArt art = gb.choiceA.selected ? StockArt.FilledCheckbox : StockArt.EmptyCheckbox;
    				art.drawChip(gc, this, ca.getW(dr), ca.getX(dr), 
    						ca.getY(dr), highlight,ViticultureId.Choice_A, null,1.1,1.3);
    				}
    			if(targets.get(gb.choiceB)!=null)
    				{
    				Loc ca = Loc.find(target.type,target.order,ViticultureId.Choice_B);
       				StockArt art = gb.choiceB.selected ? StockArt.FilledCheckbox : StockArt.EmptyCheckbox;
    				art.drawChip(gc, this, ca.getW(dr), ca.getX(dr),
    						ca.getY(dr), highlight,ViticultureId.Choice_B, null,1.1,1.3);  					
    				}
    			if(targets.get(gb.choiceC)!=null)
    				{
    				Loc ca = Loc.find(target.type,target.order,ViticultureId.Choice_C);
    				StockArt art = gb.choiceC.selected ? StockArt.FilledCheckbox : StockArt.EmptyCheckbox;
        			art.drawChip(gc, this, ca.getW(dr), ca.getX(dr),
        					ca.getY(dr), highlight,ViticultureId.Choice_C, null,1.1,1.3);  					
    				}
       			if(targets.get(gb.choiceD)!=null)
				{
				Loc ca = Loc.find(target.type,target.order,ViticultureId.Choice_D);
				StockArt art = gb.choiceD.selected ? StockArt.FilledCheckbox : StockArt.EmptyCheckbox;
    			art.drawChip(gc, this, ca.getW(dr), ca.getX(dr),
    					ca.getY(dr), highlight,ViticultureId.Choice_D, null,1.1,1.3);  					
				}
       			if(showOrder)
       				{
       				// special logic for the jack of all trades and laborer cards, which
       				// allow harvesting winemaking and filling orders, and the order
       				// of those actions may matter 
       		       	Loc.make(ChipType.BlueCard, 20, ViticultureId.Choice_HarvestFirst,0.6,-0.445,0.15);
       		       	Loc.make(ChipType.BlueCard, 20, ViticultureId.Choice_MakeWineFirst,0.6,-0.35,0.15);
       		      	Loc.make(ChipType.BlueCard, 20, ViticultureId.Choice_FillWineFirst,0.6,-0.25,0.15);
       		       	{
       		       	Loc ca = Loc.find(ChipType.BlueCard,20,ViticultureId.Choice_HarvestFirst);
    				StockArt art = gb.choice_HarvestFirst.selected ? StockArt.FilledCheckbox : StockArt.EmptyCheckbox;
    				int ax = ca.getX(dr);
    				int ay = ca.getY(dr);
    				int aw = ca.getW(dr);
    	   			art.drawChip(gc, this, aw, ax,
        					ay, highlight ,ViticultureId.Choice_HarvestFirst, null,1.1,1.3);
    	   			GC.Text(gc, false, ax+aw, ay-aw/3, szw/2,aw,Color.black,null,HarvestFirstMessage);
       		       	}
       		       	{
       		       	Loc ca = Loc.find(ChipType.BlueCard,20,ViticultureId.Choice_MakeWineFirst);
    				StockArt art = gb.choice_MakeWineFirst.selected ? StockArt.FilledCheckbox : StockArt.EmptyCheckbox;
    				int ax = ca.getX(dr);
    				int ay = ca.getY(dr);
    				int aw = ca.getW(dr);
    				art.drawChip(gc, this, aw, ax,
        					ay, highlight,ViticultureId.Choice_MakeWineFirst,null,1.1,1.3);  					
    	   			GC.Text(gc, false, ax+aw, ay-aw/3, szw/2,aw,Color.black,null,MakeWineFirstMessage);
      		       	}
       		       	if(target==ViticultureChip.JackOfAllTradesCard)
       		       		{
           		       	Loc ca = Loc.find(ChipType.BlueCard,20,ViticultureId.Choice_FillWineFirst);
        				StockArt art = gb.choice_FillWineFirst.selected ? StockArt.FilledCheckbox : StockArt.EmptyCheckbox;
        				int ax = ca.getX(dr);
        				int ay = ca.getY(dr);
        				int aw = ca.getW(dr);
        				art.drawChip(gc, this, aw, ax,
            					ay, highlight,ViticultureId.Choice_FillWineFirst,null,1.1,1.3);  					
        	   			GC.Text(gc, false, ax+aw, ay-aw/3, szw*2/3,aw,Color.black,null,FillWineFirstMessage);
       		       		}
       				}
    			}
    			if((gb.triggerCard!=null)&&(gb.triggerCard!=target))
    			{	
    				gb.triggerCard.drawChip(gc, this, w/15, cx+w/4, y+w/5,highlightAll,ViticultureId.ShowBigChip,null);
    			}
    			break;
    		case PapaCard:
    			resolvePapa(gc,pb,gb,highlight,target,cardRect,true);
    			
         		break;
    			
    		default: G.Error("Not expecting %s",target);
    		}

       	StockArt.FancyCloseBox.drawChip(gc, this, csize, x+szw*2-csize,y+csize, highlightAll,ViticultureId.CloseOverlay,null);


    }
    
    public boolean resolvePapa(Graphics gc,PlayerBoard pb,ViticultureBoard gb,HitPoint highlight,ViticultureChip target,Rectangle papaRect,boolean showcards)
    {
    		int yp = G.centerY(papaRect);
    		int cx = G.centerX(papaRect);
    		int sz = G.Height(papaRect);
			ViticultureChip.PapasBack.drawChip(gc, this, (int)(sz*1.1),(int)(cx+sz*0.04),yp,null);
    		target.drawChip(gc, this, sz, (int)(cx+sz*0.04), yp, null);
    		gb.choiceA.reInit();
    		if(showcards)
    		{
    		// in the standard variant, player has already been awarded cash and/or cards by the
    		// mama card.  In the draft variant not
      		boolean censor = !reviewOnly && censor(pb,highlight);
     		if(pb.cards.drawStack(gc, this, censor?null:highlight, sz/6, cx+sz/10, yp-sz/5, 0, 1, 0, censor?ViticultureChip.BACK:null))
    		{	
    			highlight.hitCode = ViticultureId.Magnify;
    			highlight.hitObject = pb.cards.chipAtIndex(highlight.hit_index);
    			highlight.awidth = sz/15;
    			highlight.arrow = StockArt.Eye;
    		}
    		if(pb.cash>0)
    		{
    			loadCoins(gb.choice0,pb.cash);
    			// a few mamas give coins instead of cards
    			drawStack(gc,gb.resetState,null,gb.choice0,null,null,
    					sz/5,cx+sz*4/10,yp-sz/5,
    					0,0.3,0,null);
  			
    		}}

    		int cash = 0;
    		int coins = 0;
    		switch(target.order)
    		{
    		case 1: //andrew
    			gb.choiceA.addChip(pb.getTrellis());
    			cash = 4;
    			coins = 2;
    			break;
    		case 2: //christian
    			gb.choiceA.addChip(pb.getWatertower());
    			cash = 3;
    			coins = 3;
    			break;
    		case 3: //jay
    			gb.choiceA.addChip(pb.getYoke());
    			cash = 5;
    			coins = 2;
    			break;
    		case 4: //josh
    			gb.choiceA.addChip(pb.getMediumCellar());
    			cash = 3;
    			coins = 4;
    			break;
			case 5: //kozi
				gb.choiceA.addChip(pb.getCottage());
				cash = 2;
				coins = 4;
				break;
			case 6: //matthew
				gb.choiceA.addChip(pb.getWindmill());
				cash = 1;
				coins = 5;
				break;
			case 7: //matt
				gb.choiceA.addChip(pb.getTastingroom());
				cash = 0;
				coins = 6;
				break;
			case 8: //paul
				gb.choiceA.addChip(pb.getTrellis());
				cash = 5;
				coins = 1;
				break;
			case 9: //stephan
				gb.choiceA.addChip(pb.getWatertower());
				cash = 4;
				coins = 2;
				break;
			case 10: //steven
				gb.choiceA.addChip(pb.getYoke());
				cash = 6;
				coins = 1;
				break;
			case 11: //joel
				gb.choiceA.addChip(pb.getMediumCellar());
				cash = 4;
				coins = 3;
				break;
			case 12: //raymond
				gb.choiceA.addChip(pb.getCottage());
				cash = 3;
				coins = 3;
				break;
			case 13: //jerry
				gb.choiceA.addChip(pb.getWindmill());
				cash = 2;
				coins =4;
				break;
			case 14: //trevor
				gb.choiceA.addChip(pb.getTastingroom());
				cash = 1;
				coins = 5;
				break;
			case 15: //rafael
				gb.choiceA.addChip(pb.getWorker());
				cash = 2;
				coins = 4;
				break;
			case 16: // gary
				gb.choiceA.addChip(pb.getWorker());
				cash = 3;
				coins = 3;
				break;
			case 17: //morten
				gb.choiceA.addChip(ViticultureChip.VictoryPoint_1);
				cash = 4;
				coins = 3;
				break;
			case 18: // alan
				gb.choiceA.addChip(ViticultureChip.VictoryPoint_1);
				cash = 5;
				coins = 2;
				break;
				
    		default: G.Error("Not expecting ",target);
    		}
    		loadCoins(gb.choice0,cash);
    		loadCoins(gb.choiceB,coins);
    		Loc PapaCoins = Loc.find(ChipType.PapaCard,0,ViticultureId.Choice_0);
			Loc PapaChoiceA = Loc.find(ChipType.PapaCard,0,ViticultureId.Choice_A);
			Loc PapaChoiceB = Loc.find(ChipType.PapaCard,0,ViticultureId.Choice_B);
			ViticultureState state = gb.resetState;
			int scale = (int)(PapaCoins.getW(papaRect)*0.95);
			drawStack(gc,state,null,gb.choice0,null,null,
					scale,(int)(PapaCoins.getX(papaRect)+sz*0.03),PapaCoins.getY(papaRect),
					0,-0.3,0,null);
			drawStack(gc,state,null,gb.choiceA,highlight,null,
					scale,PapaChoiceA.getX(papaRect),PapaChoiceA.getY(papaRect),
					0,0.3,0,null);
			drawStack(gc,state,null,gb.choiceB,highlight,null,
					scale,PapaChoiceB.getX(papaRect),PapaChoiceB.getY(papaRect),
					0,0.3,0,null);

			gb.choiceA.reInit();
			gb.choiceB.reInit();
			return G.pointInRect(highlight,papaRect);

    }
    // find the wine this would make in the list of targets
    public Viticulturemovespec getWineMove(ViticultureBoard gb,PlayerBoard pb,ViticultureCell c,ViticultureChip addedGrape,
    			Hashtable<ViticultureCell,Viticulturemovespec>targets,
    			boolean makeChampaign)
    {	
    	int totalRed = 0;
    	int totalWhite = 0;
    	int nreds = 0;
    	int nwhites  = 0;
    	int leastRed = 0;
    	
    	if(addedGrape!=null)
    	{
    		switch(addedGrape.type)
    		{
    		case RedGrape:	leastRed = totalRed = addedGrape.order; nreds++; break;
    		case WhiteGrape: totalWhite = addedGrape.order; nwhites++; break;
    		default: G.Error("Not expecting %s",addedGrape);
    		}
    	}
    	
    	for(int i=0;i<c.height();i++) {
    		ViticultureChip ch = c.chipAtIndex(i);
    		switch(ch.type)
    		{
    		case RedGrape:
    			totalRed += ch.order;
    			nreds++;
    			if(ch.order<leastRed || leastRed==0) { leastRed = ch.order; }
    			break;
    		case WhiteGrape:
    			nwhites++;
    			totalWhite += ch.order;
    			break;
    		case Art: break;
    		default: G.Error("Not expecting "+ch);
    		}
    	}
    	Viticulturemovespec m = null;
    	if((makeChampaign && (nwhites==1))
    			|| (nreds == 2 && nwhites==1)) 	// champagne
    		{ 
    			int secondRed = nreds==2 ? Math.max(0, totalRed-leastRed-1) : -1;
            	// from_index==-1 is a cheap champagne made with the charmat and only 1 red + 1 white
    			m = new Viticulturemovespec(MOVE_MAKEWINE,
    				ViticultureId.RedGrape,pb.colCode,leastRed-1,secondRed,totalWhite-1,
    				ViticultureId.Champaign,pb.boardIndex);
    		}
    	else if ((nreds==1) && (nwhites<=1))// red or rose
    		{
    		  m = new Viticulturemovespec(MOVE_MAKEWINE,ViticultureId.RedGrape,pb.colCode,leastRed-1,Math.max(0, totalWhite-1),0,
    				  							nwhites==1 ? ViticultureId.RoseWine : ViticultureId.RedWine,
    				  							pb.boardIndex);
    		}
    	else if (nwhites==1) // must be white
    		{
     		  m = new Viticulturemovespec(MOVE_MAKEWINE,ViticultureId.WhiteGrape,pb.colCode,totalWhite-1,0,0,ViticultureId.WhiteWine,pb.boardIndex);		
    		}
    	else { return(null); }
    	for(Enumeration<Viticulturemovespec> e = targets.elements(); e.hasMoreElements();)
    	{
    		Viticulturemovespec match = e.nextElement();
    		if(match.sameWine(m,gb.revision>=115))
    		{	
    		 	return(m);
    		}
    	}
    	// also consider the moves that have already been played. In live play
    	// this is too fast to notice, but in replay it's a distinct step
    	for(int lim=gb.pendingMoves.size()-1; lim>=0; lim--)
    	{	Viticulturemovespec match = (Viticulturemovespec)gb.pendingMoves.elementAt(lim);
    		if(match.sameWine(m,gb.revision>=115)) { return(m); }
    	}
    	
		return(null);
    }
    public void showWineOrders(Graphics gc,ViticultureBoard gb,PlayerBoard pb,HitPoint highlight,
    		Rectangle wrect,boolean censor)
    {
     	gb.cardDisplay.reInit();
     	for(int lim = pb.cards.height()-1; lim>=0; lim--)
     	{
     		ViticultureChip chip = pb.cards.chipAtIndex(lim);
     		if(chip.type==ChipType.PurpleCard) { gb.cardDisplay.addChip(chip);  } 
     	}
     	int ncards = gb.cardDisplay.height();
     	int cx = G.centerX(wrect);
     	int cy = G.centerY(wrect);
     	int cstep = G.Height(wrect)/2;
     	if(gb.cardDisplay.drawStack(gc, this, censor ? null : highlight, cstep, cx-cstep*ncards/2,cy,0, 1,0,censor ? ViticultureChip.BACK : null))
     	{	highlight.arrow = StockArt.Eye;
     		highlight.awidth = cstep/4;
     		highlight.hitObject = gb.cardDisplay.chipAtIndex(highlight.hit_index);
     		highlight.hitCode = ViticultureId.Magnify;
     	}
    }
    private int lastWleft=0;
    // show potential wines
    public void showWines(Graphics gc,ViticultureBoard gb,Rectangle br,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
     	PlayerBoard pb = gb.getCurrentPlayerBoard();
     	int cx = G.centerX(br);
     	int cy = G.centerY(br);
     	int w = (int)(G.Width(br)*0.95);
     	int h = w/2;
     	int x = cx-w/2;
     	int y = cy-h/2;
     	ViticultureState resetState = gb.resetState;
     	Rectangle wineRect = new Rectangle(x,y,w,h);
     	int maxWineValue = (pb.hasBothCellars()||(resetState == ViticultureState.Make2WinesNoCellar))
     							? 9 
     							: pb.hasMediumCellar() ? 6 : 3;
     	boolean mixedOnly = (resetState==ViticultureState.MakeMixedWinesForVP);
     	boolean minValue = (resetState==ViticultureState.Make2WinesNoCellar);
       ViticultureChip.Scrim.image.stretchImage(gc, wineRect);  
       	

       	if(G.pointInRect(highlightAll, wineRect)) { highlightAll.neutralize(); }

      	int csize = w/20;

      	if(gb.triggerCard!=null)
       	{
       		gb.triggerCard.drawChip(gc, this, w/15, x+w-csize, y+h-csize*3/2,highlightAll,ViticultureId.ShowBigChip,null);
       	}

       	GC.frameRect(gc,Color.black,wineRect);
       	ViticultureCell grapeDisplay[] = gb.grapeDisplay;
       	ViticultureCell wineDisplay[] = gb.wineDisplay;
       	int nGrapes = gb.grapeDisplayCount;
       	int maxWines = gb.wineDisplayCount;

    	int step = w/15;
    	int boxw = w/6;
    	int totalw = boxw*maxWines+step*(maxWines-1);
    	int xleft = cx-totalw/2;
    	int xright = xleft+totalw;
    	int grapew = boxw/3;
    	int grapex = cx-(nGrapes*grapew)/2;
    	int grapey = y+step*2/3+boxw;
    	int boxx = cx-totalw/2;
    	int nWhiteGrapes = 0;
    	ViticultureChip pickedObject = gb.pickedObject;
    	boolean hasInvalidWines = pickedObject!=null;
    	// display the grapes
     	for(int i=0;i<nGrapes;i++)
    	{	ViticultureCell c = grapeDisplay[i];
    		ViticultureChip top = c.topChip();
    		boolean canhit = (pickedObject==null) != (top==null); 
    		StockArt.SmallO.drawChip(gc, this, grapew,grapex,grapey,null);
    		drawStack(gc,gb.resetState,null, c,canhit ? highlight : null, highlightAll,grapew,grapex,grapey, 0,0.0,0.1,null);
    		if(top!=null) { if(top.type==ChipType.WhiteGrape) { nWhiteGrapes++; }} 
    		grapex += grapew;
    	}
     	boolean censor = !reviewOnly && censor(pb,highlightAll);
     	boolean charmat = pb.hasCharmat();
     	boolean useMixer = (resetState==ViticultureState.MakeMixedWinesForVP);
     	int nWines = 0;
     	Viticulturemovespec prev = null;
     	int cstep = step*5/4;
     	Rectangle wrect = new Rectangle(x,y+step*5-cstep,w,cstep*2);
     	showWineOrders(gc,gb,pb,highlight,wrect,censor);
     	
       	if(minValue) { GC.Text(gc, true, x,y+step*5+cstep,w,step, Color.black, null, MinValueMessage); }
       	if(mixedOnly) { GC.Text(gc, true, x,y+step*5+cstep,w,step, Color.black, null, BlushOnlyMessage); }
 
     	// display the wines
		int nChampagne = 0;		// for mixer
		int nBlush = 0;
		for(int i=0; i<maxWines;i++)
    	{	ViticultureCell c = wineDisplay[i];
    		int xpos = boxx+i*(boxw+step);
    		int xpos0 = xpos;
    		int ypos = y+step;
    		int frameX = xpos;
    		int frameY = ypos;
     		int nreds = 0;
    		int nwhites = 0;
    		int totalvalue = 0;
    		for(int lim=c.height()-1; lim>=0; lim--)
    		{	ViticultureChip ch = c.chipAtIndex(lim);
    			switch(ch.type)
    			{
    			case WhiteGrape: nwhites++; totalvalue+= ch.order; break;
    			case RedGrape: nreds++; totalvalue+= ch.order; break;
    			default: G.Error("Not expecting %s",ch);
    			}
    		}
    		boolean useCharmat = charmat && nreds==1 && nwhites==1;
    		int boxh = useCharmat ? boxw*2/3 : boxw/2;
       		GC.frameRect(gc, Color.black, xpos,ypos,boxw,boxh);
       		ypos += step/2;
       		ViticultureChip actualArt = null;	// for mixer
    		// display a suitable wine bottle as a reminder of the state of the box
    		ViticultureChip art = ViticultureChip.GenericWine;
    		if(nreds==2 ) { art = ViticultureChip.Champagne; }
    		else if(nwhites==1) { art = nreds==1 ? ViticultureChip.RoseWine : ViticultureChip.WhiteWine; }
    		else if(nreds==1) { art = ViticultureChip.RedWine; }
    		boolean makeChampaign = false;
    		actualArt = art;
    		if(useCharmat)
    		{	boolean valid = false;
    			if(art.drawChip(gc, this, grapew, xpos+step/4, ypos, highlight, ViticultureId.Choice_A,null,1.1,1.3))
    			{	highlight.setHelpText(MakeRoseWine);
    				highlight.hitObject = c;
    			}
    			if(c.cost == ViticultureId.Choice_A.ordinal()) 
    			{	valid = true;
    				StockArt.Checkmark.drawChip(gc, this, grapew/2, xpos+grapew/4, ypos+grapew/4,null);
    			}
    			xpos+=step/2;
    			if(ViticultureChip.Champagne.drawChip(gc, this, grapew, xpos+step/7, ypos, highlight, ViticultureId.Choice_B,null))
    			{	makeChampaign = false;
    				highlight.setHelpText(MakeChampagne);
    				highlight.hitObject = c;
    			}
    			if(c.cost == ViticultureId.Choice_B.ordinal()) 
    			{	valid = true;
    				makeChampaign = true;
    				actualArt = ViticultureChip.Champagne;
    				StockArt.Checkmark.drawChip(gc, this, grapew/2, xpos+grapew/4, ypos+grapew/4,null);
    			}
    			hasInvalidWines |= !valid;
    			// always display the choice
    			Text msg = TextChunk.create(SelectWineType);
    			msg.colorize(s,gameMoveText());
    			int h0 = step/2;
    			int h1 = step*2/3;
    			int ypos0 = ypos+h0;
    			// this is very messy and ad-hoc.  A cleaner way would be a general set
    			// of Text methods that includes a HitPoint.  Additional complication
    			// is that when gc==null, w1 = 0;
    			int w0 = lastWleft;
    			int w1 = GC.Text(gc,true,xpos0,ypos0,boxw,step/2,Color.black,null,msg);
    			if(w1>0) { lastWleft = w0 = w1;}
    			int xo = (boxw-w0)/2;
    			
    			int h1w = (int)(w0*0.3);
    			int x1 = xpos0+xo+w0-h1w;
    			Rectangle r0 = new Rectangle(x1+h1w/4,ypos0,h1w,h1);
    			Rectangle r1 = new Rectangle(x1-h1w,ypos0,h1w,h1);
    			
				if(HitPoint.setHelpText(highlight,r0,MakeChampagne))
    				{ highlight.spriteRect = r0;
    				  highlight.spriteColor = Color.blue;
    				  highlight.hitObject = c;
    				  valid = true;
    				  makeChampaign = true;
    				  highlight.hitCode = ViticultureId.Choice_B;
    				}
    			
    			if(HitPoint.setHelpText(highlight,r1,MakeRoseWine))
    				{ highlight.spriteRect = r1;
    				  highlight.spriteColor = Color.red;
    				  highlight.hitObject = c;
    				  valid = true;
    				  makeChampaign = false;
    				  highlight.hitCode = ViticultureId.Choice_A;
    				}
    				
    		}
    		else
    		{
    		c.cost = 0;
       		art.drawChip(gc, this, grapew, xpos+step/4, ypos,null);
    		}
    		
    		// for mixer
    		if(actualArt==ViticultureChip.Champagne) { nChampagne++; }
    		if(actualArt==ViticultureChip.RoseWine) { nBlush++; }
    		
    		Viticulturemovespec wineMove = getWineMove(gb,pb,c,pickedObject,targets,makeChampaign);	// gets a move if adding the picked wine makes a valid wine
    		boolean mixedOk = mixedOnly && (c.height()==(pickedObject==null ? 1 : 0));
    		boolean makingChampaign = (nWhiteGrapes>0) && (nreds==1 && pickedObject!=null && pickedObject.type==ChipType.RedGrape);
    		if(totalvalue>0 && wineMove==null)
    		{	hasInvalidWines = true;
    			if(!mixedOk)
    			{GC.Text(gc,true,xpos,ypos+step/8,boxw,step/2,Color.yellow,null,InvalidWine);
    			}
    		}
    		else if((pickedObject==null) && (wineMove!=null))
    		{ nWines++; 
    		  wineMove.next = prev;
    		  prev = wineMove;
    		}
   		
    		StockArt.SmallO.drawChip(gc, this, grapew, xpos+step*2/3, ypos,null);
    		HitPoint canHit = (pickedObject!=null) 
    				|| ((pickedObject==null&&totalvalue>0) 
    				|| wineMove!=null
    				|| mixedOk
    				|| makingChampaign) ? highlight : null;
    		drawStack(gc,resetState,null,c,
    				canHit,
    				highlightAll,grapew,xpos+step*2/3,ypos,0,0.7,0,null);
    		if((canHit!=null) && (pickedObject!=null) && G.pointInRect(canHit, frameX,frameY,boxw,boxw/2))
    		{	// make the whole box mouse sensitive when dropping a grape on a wine
    			c.registerChipHit(canHit,frameX+boxw/2,frameY+boxw/4,boxw,boxw/2);
    			canHit.spriteRect = new Rectangle(frameX,frameY,boxw,boxw/2);
    			canHit.spriteColor = Color.red;
    		}

    	}
    	
		if(useMixer && (nChampagne>1 || nBlush>1))
		{
			hasInvalidWines = true;
		}
    	GC.setFont(gc, largeBoldFont());
    	GC.Text(gc, true, xleft, y, xright-xleft,step,
    			Color.black,null,s.get(MaxWineValueMessage,""+maxWineValue));
    	
    	int xstep = step*3/2;
		showOtherWines(gc,gb,pb,null,highlightAll,resetState,
				new Rectangle(x+xstep,y+h-xstep,xleft-x-xstep,xstep),targets,StockArt.Checkmark);

		Rectangle makeRect = new Rectangle((int)(x+0.65*w),(int)(y+0.85*h),(int)(0.25*w),(int)(0.08*h));
    	
	
    	if(!hasInvalidWines 
    			&& GC.handleRoundButton(gc,makeRect,highlight,s.get(MakeTheseWines,nWines),HighlightColor,rackBackGroundColor))
    	{
    		highlight.hitCode = ViticultureId.MakeWine;
    		highlight.hitObject = prev;
    	}
       	StockArt.FancyCloseBox.drawChip(gc, this, csize, x+w-csize,y+csize, highlightAll,ViticultureId.CloseOverlay,null);

    }
    
    // used to display the special worker cards "really big" so they can be read.
    public void showBigStack(Graphics gc,ViticultureBoard gb,Rectangle br,
    		HitPoint highlight,ViticultureCell see)
    {	int n = see.height();
    	ViticultureChip chip = see.topChip();
    	int w = G.Width(br);
    	int h = G.Height(br);
    	int xp = (int)(w*0.1);
    	int yp = (int)(h*0.03);
    	int ytop = G.Top(br)+yp;
    	Image im = chip==null ? null : chip.getImage();
    	int size = (im==null) || im.getWidth()>im.getHeight() ? (int)(br.getWidth()*0.6) : (int)(br.getHeight()*0.6);
    	int actualW = w-2*xp;
    	int left = G.Left(br);
    	int actualH = h-2*yp;
    	size = Math.min(size, (int)(actualW*0.8));
    	Rectangle r = new Rectangle(left+xp,ytop,actualW,actualH);
    ViticultureChip.Scrim.image.stretchImage(gc, r);

       	int centerX = G.centerX(br);
    	if(n>1)
    	{
    		size = Math.min(size, actualW/(n+1));
    		centerX -= (size*n)/4;
    	}
    	if(G.pointInRect(highlight, r)) { highlight.neutralize(); }

    	see.drawStack(gc, this, highlight, 
    			size, centerX, G.centerY(br),0,
    			1,0,null);
    
    	StockArt.FancyCloseBox.drawChip(gc, this, w/20,
     				 left+xp+actualW-w/20,ytop+w/20,highlight,ViticultureId.CancelBigChip,null);
 
    }
    // show potential sales, also potential victims to be aged prematurely.
    public void showWinesSale(Graphics gc,ViticultureBoard gb,Rectangle br,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
    	
    	PlayerBoard pb = gb.getCurrentPlayerBoard();

    	
    	ViticultureState state = gb.resetState;

    	int cx = G.centerX(br);
     	int cy = G.centerY(br);
     	int w = (int)(G.Width(br)*0.5);
     	int h = w/2;
     	int x = cx-w/2;
     	int y = cy-h/2;
     	StockArt mark = StockArt.Checkmark;
     	Rectangle wineSaleRect = new Rectangle(x,y,w,h);

       ViticultureChip.Scrim.image.stretchImage(gc, wineSaleRect); 
       	if(G.pointInRect(highlightAll, wineSaleRect)) { highlightAll.neutralize(); }

       	int csize = w/20;
      	if(gb.triggerCard!=null)
       	{
       		gb.triggerCard.drawChip(gc, this, w/15, x+w-csize*4/5, y+h-csize*5/4,highlightAll,ViticultureId.ShowBigChip,null);
       	}

       	GC.frameRect(gc, Color.black, wineSaleRect);
       	ViticultureCell wineSelect[] = gb.wineSelect;
 
      	CommonMoveStack wines = new CommonMoveStack();
     	
     	Hashtable<Viticulturemovespec,ViticultureCell> reverse = new Hashtable<Viticulturemovespec,ViticultureCell>();
     	int nwines = 0;
    	for(Enumeration<ViticultureCell> e = targets.keys(); e.hasMoreElements();)
    	{	ViticultureCell c = e.nextElement();
    	    Viticulturemovespec m = targets.get(c);
    	    reverse.put(m,c);
    		int wineValue = m.from_row+ViticultureChip.minimumWineValue(m.source);
    		// knuth forgive me, but do an insertion sort 
    		int position = wines.size();
    		while(position>0)
    		{	
    			Viticulturemovespec pm = (Viticulturemovespec)wines.elementAt(position-1);
    			int pmv = pm.from_row+ViticultureChip.minimumWineValue(pm.source);
    			if(wineValue<pmv) break;   			
    			position--;
    		}
    		wines.insertElementAt(m,position);
    		nwines++;
    	}
    	int ystep = w/6;
       	int step = w/Math.max(6,nwines);
    	int boxStep = w/Math.max(7,nwines+1);
    	int totalw = boxStep*wines.size()+step*2;
    	int boxx = cx-totalw/2;
    	int nWines = wines.size();
    	for(int wineIndex = wines.size()-1; wineIndex>=0; wineIndex--)
    	{	
    		Viticulturemovespec m = (Viticulturemovespec)wines.elementAt(wineIndex);
    		ViticultureCell disp = wineSelect[wineIndex];
    		disp.reInit();
    		disp.addChip(ViticultureChip.getChip(m.source,m.from_row+ViticultureChip.minimumWineValue(m.source)));
 
    		int xp = boxx+step+step/2;
    		int yp = y+ystep;
    		boolean hitBelow = false;
        	switch(state)
        	{
         	default: break;	
        	case Sell1Wine:
               	GC.setFont(gc, largeBoldFont());
               	gb.choice0.reInit();
            	int value = ViticultureChip.wineSaleValue(m.source,gb.revision);
            	gb.choice0.addChip(ViticultureChip.VictoryPoints[value]);
            	int yp2 = y+(int)(step*1.75);
            	drawStack(gc,state,null,gb.choice0,null,highlightAll,step*9/10,xp,yp2,0,0,0,null);
            	if(G.pointInRect(highlight,xp-step/2,yp2-step/2,step,step))
            	{ hitBelow = true;
            	}
         	}
        	
    		if(drawStack(gc,state,null,disp,highlight,highlightAll,step,xp,yp,0,0,0,null) || hitBelow)
    			{
    			 highlight.hitObject = m;
    			 highlight.spriteColor = Color.red;
    			 highlight.hitCode = disp.rackLocation();
    			 highlight.spriteRect = new Rectangle(xp-step/2,yp-step/2,step,step);
    			}
    		if(pb.selectedCells.contains(reverse.get(m)))
    		{
    			mark.drawChip(gc,this,step/2,boxx+step+step/4,y+step+step/4,null);
    		}
       	
 
    		boxx += boxStep;
    	}
    	String message = null;
    	String noMessage = null; 
    	boolean selectable = true;
    	switch(state)
    	{
    	case DiscardGrapeAndWine:
    		message = Discard1And1Message;
    		noMessage = NoDiscardMessage;
    		break;
    	case Age2Once:
    		message = Age2OnceMessage;
    		break;
    	case Age1Twice:
    		message = Age1TwiceMessage;
    		break;
    	case Age1AndFill:
    		message = SelectWineAge;
    		break;
    	case Discard2GrapesFor3VP:
    		selectable = false;
    		message = DiscardTwoMessage;
    		noMessage = NoDiscardMessage;
    		break;
    		
    	case DiscardGrapeFor2VP:
    	case DiscardGrapeFor3And1VP:
    		selectable = false;
			//$FALL-THROUGH$
		case DiscardWineFor3VP:
    	case DiscardGrapeOrWine:    	
    	case DiscardWineFor4VP:
    	case DiscardWineFor2VP:
    	case DiscardWineForCashAndVP:
    		message = DiscardOneMessage;
    		noMessage = NoDiscardMessage;
    		break;
    	case Sell1WineOptional:
    		noMessage = NoSellMessage;
			//$FALL-THROUGH$
		case Sell1Wine:
    		message = SellOneWineMessage;
    		break;
    	default: G.Error("Not expecting %s",state); 
    	}
    	GC.setFont(gc, largeBoldFont());
    	
    	if(message!=null) 
		{	
			GC.Text(gc, true, x, y, w,step/2,Color.black,null,s.get(message)); 
		}
		if((noMessage!=null)
				&& (pb.selectedCells.size()==0)
    			&& GC.handleRoundButton(gc,new Rectangle(x+step/4,y+h-step,step*3/2,step/3),highlight,noMessage,
					HighlightColor,rackBackGroundColor ))
    		{
    			highlight.hitCode = GameId.HitDoneButton;
    		}
		if(nWines==0)
		{
			GC.Text(gc, true, x, y+step/2, w,step*3/2,Color.black,null,HaveNoneMessage);
		}
		Rectangle wrect = new Rectangle(x+step,y+h-step,w-step,step);
		int wx = showOtherWines(gc,gb,pb,selectable ? highlight : null,highlightAll,state,wrect,targets,mark);
		G.SetLeft(wrect,wx);
		G.SetWidth(wrect, w-(wx-x));
		showWineOrders(gc,gb,pb,highlight,wrect,highlight==null);
       	StockArt.FancyCloseBox.drawChip(gc, this, csize, x+w-csize,y+csize, highlightAll,ViticultureId.CloseOverlay,null);
		//G.frameRect(gc, Color.blue, wrect);
    }
    
    private void accumulate(ViticultureCell cells[],CellStack otherwines,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
    	for(ViticultureCell p : cells)
    	{
    		if((p.topChip()!=null) && (targets.get(p)==null)) { otherwines.push(p); }
    	}
    }
    
   // show the wines not in "targets" in the given rectangle
    private int showOtherWines(Graphics gc,ViticultureBoard gb,PlayerBoard pb,HitPoint highlight,HitPoint highlightAll,
    		ViticultureState state,Rectangle r,Hashtable<ViticultureCell,Viticulturemovespec>targets,StockArt mark)
    {
    	CellStack otherwines = new CellStack();
    	accumulate(pb.whiteWine,otherwines,targets);
    	accumulate(pb.redWine,otherwines,targets);
    	accumulate(pb.roseWine,otherwines,targets);
    	accumulate(pb.champagne,otherwines,targets);
    	int left = G.Left(r);
    	int step = G.Height(r);
    	int y = G.Top(r);
		for(int i=0;i<otherwines.size(); i++,left+=step/3)
			{
			ViticultureCell cell = otherwines.elementAt(i);
			if(drawStack(gc,state,null,cell,highlight,highlightAll,step*3/4,left,y+step*2/3,0,0,0,""+(cell.row+ViticultureChip.minimumWineValue(cell.rackLocation()))))
				{
				commonMove m = new Viticulturemovespec(MOVE_SELLWINE,cell,gb.whoseTurn);
				highlight.hitObject = m;
				}
    		if(pb.selectedCells.contains(cell) && (mark!=null))
    		{
    			mark.drawChip(gc,this,step/4,left,y+4*step/5,null);
    		}
			}
		return(left);
    }
    
    public void showClosedOverlay(Graphics gc,Rectangle br,HitPoint highlight)
    {	int w = G.Width(br);
    	int h = G.Height(br);
    	Rectangle r = new Rectangle(G.Left(br),G.Top(br)+h/20,w/10,h/10);
    ViticultureChip.Scrim.image.stretchImage(gc, r); 
    	GC.frameRect(gc, Color.black, r);
    	StockArt.Checkmark.drawChip(gc, this, w/20,G.centerX(r),G.centerY(r),highlight,ViticultureId.CloseOverlay,null);
    }
    private TextContainer scoreSummary = new TextContainer(ViticultureId.ScoreSummary);
    private boolean scoreSummaryPrepared = false;
    private void buildScoreSummary(ViticultureBoard gb)
    {	
		scoreSummary.clear();
		for(PlayerBoard pb : gb.pbs)
			{scoreSummary.append("\n");
			scoreSummary.append(s.get(ScoringFor,prettyName(pb.boardIndex)));
			scoreSummary.append("\n");
			pb.buildStatString();
			scoreSummary.append(pb.scoreString.toString());			
			
			}
		scoreSummaryPrepared = true;
    }
    public HitPoint MouseMotion(int ex, int ey,MouseState upcode)
    {
    	HitPoint hp = super.MouseMotion(ex, ey, upcode);
    	if(scoreSummaryPrepared)
    	{
    		scoreSummary.doMouseMove(ex,ey,upcode);
    	}
    	return(hp);
    }
    public boolean GameOver()
    {
    	boolean over = super.GameOver();
    	if(!over)
    	{
    		scoreSummaryPrepared = false;
    	}
    	else
    	{
    		if(!scoreSummaryPrepared) { buildScoreSummary(mainBoard); }
    	}
    	return(over);
    }
    private void showScoreGraph(Graphics gc,HitPoint hp,ViticultureBoard gb,Rectangle br)
    {	GC.fillRect(gc,Color.gray,br);
    	GC.frameRect(gc,Color.black,br);
    	int width = G.Width(br);
    	int xscale = width/40;
    	int yscale = G.Height(br)/7;
    	int top = G.Top(br);
    	int left = G.Left(br);
    	int y = top+yscale;
    	int x0 = left+xscale*2;
    	GC.setFont(gc,standardPlainFont());
    	int tleft = left+yscale;
    	for(ScoreType e : ScoreType.values()) 
    	{
    		Color c = e.color;
    		if(c!=null)
    		{	
    			int dx = GC.Text(gc,false,tleft,top,width,yscale,c,null,""+e);
    			tleft += dx+yscale/2;
    		}
    		
    	}
	

				
    	for(PlayerBoard pb : gb.pbs)
    	{	int x = x0;
			int highx = x;
    		int y0 = y;
    	    pb.getRooster().drawChip(gc,this,yscale*3/2,left+xscale,y+yscale/2,null);
   		for(ScoreType e : ScoreType.values()) 
    		{
    			Color c = e.color;
    			int plusy = y;
    			if(c!=null)
    			{	int points = pb.statSummary[e.ordinal()];
    				int w = points*xscale;
    				int plusw = w;
    				int ys = yscale;
    				if(w<0) { x += w; plusw = 0; w = -w; ys=ys/3; plusy = y+yscale*2/3; }
    				else { plusy = y0; ys = y0+yscale-y; }
    				GC.fillRect(gc,c,x,y,w,ys);
    				GC.frameRect(gc,Color.gray,x,y,w,ys);
      				HitPoint.setHelpText(hp,x,y,w,ys,
    						s.get(PointsFrom,""+points,""+e));
    				if(y!=y0)
    				{
    					if(x<highx)
    					{
    						int dif = highx-x;
    						w -=dif;
    						GC.fillRect(gc,c,highx,y0,w,yscale);
    						GC.frameRect(gc,Color.gray,highx,y0,w,yscale);
    						HitPoint.setHelpText(hp,highx,y0,w,yscale,
    	    						s.get(PointsFrom,""+points,""+e));
    					}
    				}
     				
     				x += plusw;
    				highx = Math.max(highx,x);
    				y = plusy;
    			}
    		}
    		GC.frameRect(gc,Color.black,x0,y,x-x0,yscale);
    		y0 += yscale;
    		y = y0;
    	}
    }
    // show scoring information at the endgame
    public void showScoreSheet(Graphics gc,ViticultureBoard gb,Rectangle br,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {	int boardW = G.Width(br);
    	int boardH = G.Height(br);
    	int w = (int)(boardW*0.9);
    	int h = (int)(boardH*0.9);
    	int x = G.Left(br)+(int)(boardW*0.05);
    	int y = G.Top(br)+(int)(boardH*0.05);
    	int nyears = Math.max(7, gb.year);
     	Rectangle scoreRect = new Rectangle(x,y,w,h);
     ViticultureChip.Scrim.image.stretchImage(gc, boardRect);  
     	GC.fillRect(gc,Color.lightGray,scoreRect);
     	GC.frameRect(gc, Color.black, scoreRect);
     	
     	int xstep = w/(4*(nyears-1));
     	int ystep = h/(MAX_SCORE-MIN_SCORE);

      	GC.setFont(gc,largeBoldFont());
     	GC.Text(gc,true,x+w/2-xstep*10,y,xstep*20,ystep*6,Color.black,null,ScoreSummaryMessage);
    	
     	GC.setFont(gc, standardPlainFont());
		GC.setColor(gc,Color.darkGray);
		
		{			
		int ypos = y+h - (WINNING_SCORE-MIN_SCORE)*ystep;
		GC.drawLine(gc, x,ypos,x+w,ypos);
 		for(int i=1;i<nyears;i++)
 		{	int xpos = x+xstep*(i*4);
 			
 			GC.drawLine(gc, xpos, ypos,xpos, y+h);
 			Text msg = TextChunk.create(s.get(YearDescription,i));
 			msg.drawRight(gc,xpos-xstep*3,y+h,xstep*3-xstep/4,ystep,Color.black,null);
 		}}
		{	int xpos = x+w;
			for(int xp = 0;xp<=MAX_SCORE;xp++)
			{	
				int ypos =  y + h - (xp-MIN_SCORE)*ystep;
				GC.drawLine(gc, xpos, ypos, xpos+xstep/5, ypos);
				if((xp%5)==0) { GC.Text(gc,""+xp,xpos+xstep/10, ypos); }
			}
		}
 		for(int pass=1; pass<=2;pass++)
 		{
     	for(PlayerBoard pb : gb.pbs)
     	{	
     		//if(pb.color==ViticultureColor.Purple)
     		{
     		ScoreStack scores = pb.scoreEvents;
       		int pxpos = x;
         	int pypos = y+h+MIN_SCORE*ystep-pb.colorIndex*2;
         	
     		for(int i=0,lim=scores.size(); i<lim; i++)
     		{	ScoreEvent event = scores.elementAt(i);
     			int ypos = y+h-(-MIN_SCORE + event.net)*ystep-pb.colorIndex*ystep/20;
     			int xpos = x+(Math.max(0,(event.year-1))*4+event.season)*xstep-pb.colorIndex*xstep/30;
     			if(pass==1)
     			{
     			GC.setColor(gc, MouseColors[pb.colorIndex]);
     			GC.drawLine(gc, pxpos, pypos, xpos, pypos);
     			GC.drawLine(gc,xpos, pypos, xpos, ypos);
     			GC.fillOval(gc, xpos-xstep/6, ypos-xstep/6, xstep/3, xstep/3);
     			}
       			if(pass==2)
      				{
       				
       				double vscale [] = {1.0,1.2,0.3,-0.2};
       				double wscale[] = {1.0,1.7,0.1,-0.4};
       				double zscale[] = {1.0,1.0,0.1,0};
       				String message = event.message;
       				ViticultureChip chip = event.visual;
       				Text msg = null;
       				if(HitPoint.setHelpText(highlightAll, xstep/2,xpos,ypos, ""+pb.color+":"+message))
       				{
       					if(chip!=null)
       					{	highlightAll.hitObject = chip;
       						highlightAll.hitCode=ViticultureId.ShowBigChip;
       						highlightAll.awidth = xstep/2;
       						highlightAll.hit_x = xpos;
       						highlightAll.hit_y = ypos;
       						highlightAll.spriteColor = Color.red;
       					}
       				}
       				if(chip!=null)
       				{	ChipType type = chip.type;
       					double [] scale = type.isWine() ? wscale : zscale;
       					switch(type)
       					{
       					case Star:	scale = wscale;
       						break;
       					case VP: 	scale = vscale;
       						break;
       					case TastingRoom: 
       						scale = wscale;
       						break;
       					default: break;
       					}
       					
       					msg = TextGlyph.create("xxx","xx",chip,this,scale);
       					     					
        			}
       				else { msg = TextChunk.create(message); } 
      				GC.setColor(gc,Color.black);    				
      				msg.drawRight(gc,x,ypos-ystep/2,xpos-xstep/4-x, ystep,Color.black,null);
      				}
     			pxpos = xpos;
     			pypos = ypos;
     			}
     			if(pass==1) { GC.drawLine(gc, pxpos, pypos,x+w, pypos); }
     		}
     		}
     	}
 		showScoreGraph(gc,highlightAll,gb,new Rectangle(x+w/6+xstep,y+ystep*5,w/2,2*ystep*7));
 		
    	scoreSummary.setBounds(x+xstep/3,y+h/10,xstep*4,2*h/3);
     	scoreSummary.setVisible(true);
    	scoreSummary.redrawBoard(gc,null);
    }
    private void viewCard(HitPoint highlight,Rectangle sr,ViticultureChip chip)
    {
    	if(HitPoint.setHelpText(highlight,sr,ViticultureId.ShowBigChip,ViewCard))
    	{
    		highlight.hitObject = chip;
    	}
    }
    
    // show potential trades
    public void showTrades(Graphics gc,ViticultureBoard gb,Rectangle br,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
    	PlayerBoard pb = gb.getCurrentPlayerBoard();
    	ViticultureState state = gb.resetState;
    	boolean fromCards = pb.cards.height()>=2;
    	boolean fromCash = pb.cash>=3;
    	boolean fromVP =  pb.score>MIN_SCORE;
    	boolean censor = !reviewOnly && censor(pb,highlightAll);
    	int fromRedGrapes = pb.leastRedGrapeValue();
    	int fromWhiteGrapes = pb.leastWhiteGrapeValue();
     	int cx = G.centerX(br);
     	int cy = G.centerY(br);
     	int w = (int)(G.Width(br)*0.9);
     	int h = Math.min((int)(G.Height(br)*0.9), w*2/3);
     	w = h*3/2;
     	int x = cx-w/2;
     	int y = cy-h/2;
     	Rectangle tradeRect = new Rectangle(x,y,w,h);
   	
    	int step = w/10;
    	int xleft = x+step;
    	int xright = x+w-step*2;
    	int yleft = y+step*2;
    	int yright = y+step*2;
    	
     
    	//G.tileImage(gc,ViticultureChip.Scrim.image,tradeRect, this);   
    ViticultureChip.Scrim.image.stretchImage(gc, tradeRect);  
    	

       	if(G.pointInRect(highlightAll, tradeRect)) { highlightAll.neutralize(); }
       	
       	int csize = w/20;
      	if(gb.triggerCard!=null)
       	{
       		gb.triggerCard.drawChip(gc, this, w/15, x+w-csize, y+h-csize*3/2,highlightAll,ViticultureId.ShowBigChip,null);
       	}

    	GC.frameRect(gc, Color.black, tradeRect);
    	GC.setFont(gc, largeBoldFont());
       	GC.Text(gc,true,xleft,y,5*step,step,Color.black,null,TradeSell);
    	GC.Text(gc,true,xright-3*step,y,3*step,step,Color.black,null,TradeReceive);
    	
    	if(fromCards) 
    		{ String cardLabel = censor ? ViticultureChip.BACK : null;
    		  drawStack(gc,state,null, pb.cards,highlight,highlightAll, step,xleft,yleft, 0,0.05,0.05,cardLabel); 
    		  if(pb.cards.selected)
    		  {	  ViticultureCell cardDisplay = gb.cardDisplay;
    		  	  IStack cardIndex = new IStack();
    		  	  cardDisplay.copyFrom(pb.cards);
    		  	  cardDisplay.selected=false;
    		  	  for(int i=0;i<cardDisplay.height();i++) { cardIndex.push(i); }
    		  	  
       		  	  int cardLimit = Math.max(7, (cardDisplay.height()+1)/2);
       		  	  int cardStep = Math.min(step*4/3,(int)(w/(cardLimit+0.5)));
    		  	  if(cardDisplay.height()>cardLimit)
    		  	  { ViticultureCell cardDisplay1 = gb.cardDisplay1;
    		  	    IStack card1Index = new IStack();
    		  	    cardDisplay1.reInit();
    		  	    yleft-= step*2/3;
    		  	    while(cardDisplay.height()>cardLimit)
    		  	    	{ cardDisplay1.addChip(cardDisplay.removeTop());
    		  	    	  card1Index.push(cardIndex.pop());
    		  	    	}
    		  	    if(drawStack(gc,state,null,cardDisplay1,highlight,highlightAll,cardStep,xleft,yleft+step*5 ,0,1,0,cardLabel))
    		  	    {	// pretend we hit the main stack
    		  	    	highlight.hit_index = card1Index.elementAt(highlight.hit_index);
    		  	    }
    		  	    for(int i=0;i<cardDisplay1.height();i++)
    		  	    { ViticultureChip chip = cardDisplay1.chipAtIndex(i);
    		  	      int xp = xleft+cardStep*i;
    		  	      int yp = yleft+step*4+cardStep/2;
    				  if(pb.selectedCards.contains(pb.cards.rackLocation(),chip,card1Index.elementAt(i)))
    				  {
    					  StockArt.Checkmark.drawChip(gc, this, step/2,xp,yp,null);
    				  }
    				  Rectangle sr = new Rectangle(xp-step/2,yp+step,step,step/3);
    				  viewCard(highlight,sr,chip);
    		  	    }
    		  	  }
    		  	  
    			  if(drawStack(gc,state,null,cardDisplay,highlight,highlightAll,cardStep,xleft,yleft+step*3,0,1,0,cardLabel))
    			  {
    				  highlight.hit_index = cardIndex.elementAt(highlight.hit_index);
    			  }
    			  for(int i=0;i<cardDisplay.height();i++)
    			  {
  				  	int xp = xleft+cardStep*i;
  				  	int yp = yleft+step*2+cardStep/2;
  				  	ViticultureChip chip = cardDisplay.chipAtIndex(i);
  				  	if(pb.selectedCards.contains(pb.cards.rackLocation(),chip,cardIndex.elementAt(i)))
    				  {	boolean isDiscard = state.discardCards()>=0;
    		     		(isDiscard?StockArt.Exmark:StockArt.Checkmark).drawChip(gc, this, step/2,xp,yp,null);
    				  }
  				  	  Rectangle sr = new Rectangle(xp-step/2,yp+step,step,step/3);
  				  	  viewCard(highlight,sr,chip);
     			  }
    		  }
    		  xleft+=step; 
    		}
    	if(fromCash) 
    		{  loadCoins(pb.cashDisplay,3);
    		   drawStack(gc,state,null,pb.cashDisplay,highlight,highlightAll, step,xleft,yleft, 0,0.15,0.05,null);
    		   xleft += step;
    		}
    	if(fromVP)
    		{	drawStack(gc,state, null,pb.vpDisplay,highlight,highlightAll, step,xleft,yleft, 0,0.05,0.05,null);
    			xleft += step;
    		}
    	if(fromRedGrapes>0)
			{
    		drawStack(gc,state,null, pb.redGrapeDisplay,highlight,highlightAll, step,xleft,yleft, 0,0.05,0.05,null);
			xleft += step;
			}
       	if(fromWhiteGrapes>0)
       		{
      		drawStack(gc,state, null,pb.whiteGrapeDisplay,highlight,highlightAll, step,xleft,yleft, 0,0.05,0.05,null);
      		xleft += step;
       		}
       	
       	// receive 
 		{ 
		  yright -= step/3;
		  drawStack(gc,state,null, gb.tradeCards,highlight,highlightAll, step,xright,yright, 0,0.25,0.25,null); 
		  yright += step;

		  drawStack(gc,state,null,gb.tradeCoins,highlight,highlightAll, step ,xright,yright, 0,0.15,0.05,null);
		  yright += step;
		   
		  drawStack(gc,state,null, gb.tradeVP,highlight,highlightAll, step ,xright-step,yright-step*2, 0,0.05,0.05,null);

		  yright -= step+step/4;
		  xright += step;
		  drawStack(gc,state, null,gb.tradeWhiteGrape,highlight,highlightAll, step ,xright,yright, 0,0.05,0.05,null);
		  yright -= step;
		  drawStack(gc, state,null,gb.tradeRedGrape,highlight,highlightAll, step ,xright,yright, 0,0.05,0.05,null);

		}
       	StockArt.FancyCloseBox.drawChip(gc, this, csize, x+w-csize,y+csize, highlightAll,ViticultureId.CloseOverlay,null);
       	

    }
    
    private void addCardMoves(ViticultureCell cardDisplay,CardPointerStack in,ViticultureCell cards,
    		Hashtable<ViticultureCell,Viticulturemovespec>targets,boolean vines,boolean backs,ViticultureBoard gb)
    {
    	Viticulturemovespec cardMoves = targets.get(cards);	
    	ViticultureId rack = cards.rackLocation();
		while(cardMoves!=null) 
			{ int index = cardMoves.from_index;
			  ViticultureChip ch = backs ?  ViticultureChip.cardBack(rack) : cards.chipAtIndex(index);
			  if(ch!=null)
			  {
			  if((!vines || (ch.type==ChipType.GreenCard)) 
					  && !in.contains(rack,ch,index))
			  	{ 
				cardDisplay.addChip(ch); 
			  	in.push(rack,ch,index);
			  	}}
			  cardMoves = (Viticulturemovespec)cardMoves.next;
			}
    }
    private boolean addToWineSelection(ViticultureBoard gb,PlayerBoard pb,ViticultureCell row[],int wineIndex,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
    	int leastRed = pb.leastFilledWineValue(row);
		int min = ViticultureChip.minimumWineValue(row[0].rackLocation());
		if(leastRed>=0)
			{
			ViticultureCell c = row[leastRed-min];
			if(targets.get(c)!=null) 
				{ ViticultureChip chip = ViticultureChip.getChip(c.rackLocation(),c.row+ViticultureChip.minimumWineValue(c.rackLocation()));
				ViticultureCell disp = gb.wineSelect[wineIndex];
				disp.reInit();
				disp.addChip(chip);
				}
			return(true);
			}
		return(false);
    }

    // show cards and/or fields
    boolean uprootMode = false;
    public void showCards(Graphics gc, ViticultureBoard gb, Rectangle br,
    		HitPoint highlight0,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets,UI uimode)
    {	HitPoint highlight = highlight0;
        boolean uprootOnlyMode = false;
        boolean plantMode = false;
        boolean optionalPlant = false;
        boolean harvestOrUprootMode = false;
        boolean showUproots = uprootMode;
        boolean cardBacks = false;
        boolean apCards = false;
        boolean showFields = true;
     	ViticultureState state = gb.resetState;
     	boolean swapMode = false;
        int nToDiscard = state.discardCards();
        //int nToKeep = state.keepCards();
        StockArt mark = (nToDiscard>=0?StockArt.Exmark:StockArt.Checkmark);
        boolean quickExit = false;
        PlayerBoard pb = gb.getCurrentPlayerBoard();
       	ViticultureCell cards = pb.cards;
		ViticultureCell cardDisplay = gb.cardDisplay;
		CardPointerStack cardIndex = new CardPointerStack();
		ViticultureCell cardDisplay1 = null;
		CardPointerStack card1Index = null;
		ViticultureCell unusedCards = gb.unusedCardDisplay;
		CardPointerStack unusedIndex = null;
		unusedCards.reInit();
		cardDisplay.reInit();
		int hi = remoteWindowIndex(highlightAll);
		PlayerBoard activePlayer = (hi>=0) ? gb.pbs[hi] : showPlayerCards();
		if(activePlayer==null) { activePlayer = pb; }
		String apname = prettyName(activePlayer.boardIndex);
		
       	switch(uimode)
        {
		case ShowUproots:
        	plantMode = true;
        	uprootOnlyMode = showUproots = true;
        	break;
			
		case ShowHarvestsAndUproots:
			harvestOrUprootMode = true;
			break;
		case ShowSwitches:
			uprootMode = true;
			swapMode = true;
			plantMode = false;
			break;
        case ShowPlants:
        	uprootMode = false;
        	plantMode = true;
        	break;
		case ShowCardBacks:
			cardBacks = true;
			//$FALL-THROUGH$
		case ShowCards:
			uprootMode = showUproots = false;
        	break;
		case ShowAPCards:
			apCards = true;
			showFields = false;
			uprootMode = showUproots = false;
			highlight = null;
			cardDisplay.copyFrom(activePlayer.cards);
			cardDisplay.selected = false;
			for(int i=0;i<cardDisplay.height();i++) { cardIndex.push(activePlayer.cards.rackLocation(),cardDisplay.chipAtIndex(i),i); }
			break;
		default: G.Error("Not expecting %s",uimode);
        }
     	boolean cardAndWineMode = false;
     	boolean destroyMode = false;
     	boolean uncensored = false;
     	boolean oracleMode = false;
        boolean showWines = false;
        boolean selectWines = false;
        boolean harvesting  = false;
        if(!apCards)
        {
        switch(state.activity) {

        case DiscardWines: 
        	selectWines = true;
			//$FALL-THROUGH$
		case FillWine:
        	showWines = true;
        	break;
        case Harvesting: 
        	harvesting = true; 
        	break;
        default: break;
        }
      	switch(state)
     	{
     	
        case DiscardCards: 
        	nToDiscard = pb.cards.height()-7;
        	break;
        case Plant1AndGive2:
     		optionalPlant =  true;
     		break;
    	case SelectCardColor:
     		oracleMode = true;
     		break;
     	case Discard3CardsAnd1WineFor3VP:
     		cardAndWineMode = true;
       		showWines = selectWines = true;
     		break;
     	case DestroyStructure:
     	case DestroyStructureOptional:
     		destroyMode = true;
     		break;
     	case Pick2TopCards:
		case Pick2Discards:
     		nToDiscard = 2;
     		uncensored = true;
     		break;
     	default: break;
     	}}
        int wineIndex = 0;
    	ViticultureCell fields[] = pb.fields;
    	ViticultureCell vines[] = pb.vines;
    	CardPointerStack selected = pb.selectedCards;
		CellStack fieldDisplay = new CellStack();
		CellStack vineDisplay = new CellStack();
		CellStack vineSelect = pb.selectedCells;
		boolean extraHeight = false;
		ViticultureChip selectedVine = null;
		boolean limitedVines = (state!=ViticultureState.Plant1VineNoLimit);
		
		// in case this is a planting move, note the vine that has been selected
		for(int lim=selected.size()-1; lim>=0; lim--)
		{	ViticultureChip top =  selected.elementAt(lim).card;
			if(top!=null)
			{
				if(top.type==ChipType.GreenCard) { selectedVine = top; }
			}
		}
		
      	boolean censor = !reviewOnly 
					&& (apCards
						? censor(activePlayer,highlightAll) 
						: (!uncensored && (highlight==null || censor(pb,highlightAll))));
		if(!apCards)
		{
		cardDisplay.reInit();
		cardIndex.clear();
		addCardMoves(cardDisplay,cardIndex,cards,targets,uprootMode|plantMode,cardBacks,gb);
		if(plantMode)
		{	// if we're censoring the cards, keep them all in one row
			// to enhance the censorship
			ViticultureCell addTo = cardBacks||censor ? cardDisplay : unusedCards;
			CardPointerStack indexTo = cardBacks||censor ? cardIndex : (unusedIndex = new CardPointerStack());
			ViticultureCell addFrom = pb.cards;
			ViticultureId rack = addFrom.rackLocation();
			// add the green cards we're not using to either the main list or an auxiliary
			for(int lim = addFrom.height()-1; lim>=0; lim--)
			{	ViticultureChip card = addFrom.chipAtIndex(lim);
				if((card.type==ChipType.GreenCard) && !cardIndex.contains(rack,card,lim)) 
					{ addTo.addChip(card);
					  indexTo.push(rack,card,lim); 
					  extraHeight = true;
					}
			}
		}
		for(ViticultureCell stack : gb.cardsAndDiscards) 
			{ addCardMoves(cardDisplay,cardIndex,stack,targets,false,cardBacks,gb); }
		for(ViticultureCell stack : pb.structures)
			{
			addCardMoves(cardDisplay,cardIndex,stack,targets,false,cardBacks,gb); 
			}
		if(destroyMode)
			{
			for(ViticultureCell stack : pb.fields)
			{
			addCardMoves(cardDisplay,cardIndex,stack,targets,false,false,gb); 
			}}
		uprootMode |= ((state!=ViticultureState.GiveYellow) && (gb.pendingMoves.size()>0));
		}
		
		
		{
		// split into two stacks if there are a lot of cards
		int nCards = cardDisplay.height();
		if(nCards>6)
		{	cardDisplay1 = gb.cardDisplay1;
			cardDisplay1.reInit();
			card1Index = new CardPointerStack();
			for(int lim = nCards/2; lim>0;lim--)
			{
				cardDisplay1.addChip(cardDisplay.removeTop());
				card1Index.push(cardIndex.pop());
			}
		extraHeight = true;
		unusedCards.reInit();	// if the field is too crowded, skip this
		}
		}
		
		
		if(showFields && !destroyMode)
		{
		for(int i=0;i<fields.length;i++)
			{	ViticultureCell f = fields[i];
				if(targets.get(f)!=null)
				{ if(!fieldDisplay.contains(f) 
						&& (uprootMode 
								? ((vines[i].height()>0) && ((f.topChip().type==ChipType.Bead)||(f.topChip().type==ChipType.Field)))
								: harvestOrUprootMode 
									? (vines[i].height()>0 && f.topChip().type!=ChipType.Bead) 
									: true))
					{ fieldDisplay.push(f);
					  vineDisplay.push(vines[i]);
					  wineIndex++;
					}
				}
			}
		}
		if(cardAndWineMode)
		{	
			if(addToWineSelection(gb,pb,pb.redWine,wineIndex,targets)) { wineIndex++; }
			if(addToWineSelection(gb,pb,pb.whiteWine,wineIndex,targets)) { wineIndex++; }
			if(addToWineSelection(gb,pb,pb.roseWine,wineIndex,targets)) { wineIndex++; }
			if(addToWineSelection(gb,pb,pb.champagne,wineIndex,targets)) { wineIndex++; }
		}	
    	
		int w = G.Width(br);
		int h = G.Height(br);
		int nCards = cardDisplay.height();
		int nCardSlots = (nCards==1 ? 2 : nCards);
		int nFieldSlots = (wineIndex==0) ? 0 : (wineIndex+1);
		int step = Math.min(3*h/10,w/Math.max(5, 1+(nCardSlots+nFieldSlots)));
		
		int fstep = cardAndWineMode ? step*2/3 : step*6/5;
      	String cardLabel = censor ? ViticultureChip.BACK : null;
		int fieldW = nFieldSlots*fstep;
		int cardW = Math.max(0, Math.min(w-fieldW,step*nCardSlots));
		int totalW = Math.max(step*5+step/3,cardW+fieldW);
		if(cardW==0) { fieldW = totalW; }
		if(fieldW==0) { cardW = totalW; }
		
		int centerX = G.centerX(br);
		int centerY = G.centerY(br);
		int xp = (int)(centerX-totalW/2);
		int cardH = Math.min(h,step*3+(extraHeight ? step*3/4 : 0));
		step = Math.min(step,(int)(cardH*0.27));
		int yp = (int)(centerY-cardH/2);
		Rectangle fieldRect = new Rectangle(xp,yp,totalW,cardH);
    ViticultureChip.Scrim.image.stretchImage(gc, fieldRect);  
    	

       	if(G.pointInRect(highlightAll, fieldRect)) 
       		{ highlightAll.neutralize(); 
			}

		{
		int mleft = xp+step/8;
		int top  = yp+cardH-step*2/3+(extraHeight?step/4:0);
		//
		if(harvestOrUprootMode)
		{
			if(GC.handleRoundButton(gc,new Rectangle(mleft,top,step,step/3),highlight,HarvestMode,
					HighlightColor,uprootMode ? rackBackGroundColor :HighlightColor ))
			{	highlight.hitCode = ViticultureId.HarvestMode;
			}
			top -= step/2;
			if(GC.handleRoundButton(gc,new Rectangle(mleft,top,step,step/3),
					uprootOnlyMode ? null :  highlight,		s.get(UprootMode),	HighlightColor,uprootMode ?HighlightColor:rackBackGroundColor))
				{	highlight.hitCode = ViticultureId.UprootMode;
				}
		}
		else if(!apCards)
		{
			{String msg = state.activity.getName();
			String tmsg = oracleMode ? s.get(OracleCardsMessage2)
							: s.get0or1(msg,Math.abs(nToDiscard));
			GC.Text(gc,false, mleft,top,step,step/3,Color.blue,null,tmsg);
			}
       	if(showWines)
       	{	
       		int lleft = mleft+step+step/3;
       		Rectangle wr = new Rectangle(lleft,top-step/3,totalW-lleft,step/2+step/3);
    		showOtherWines(gc,gb,pb,selectWines ? highlight: null,highlightAll,state,wr,targets,mark);

       	}
		
		if(swapMode)
		{	String msg = s.get(gb.invalidFieldSwap(pb) ? FieldLimitsExceeded : FieldLimitsApply);
			GC.Text(gc,false, mleft,top+step/5,step*3/2,step/3,Color.blue,null,msg);
		}
		
       	if(allPlayersLocal() && !apCards && !cardBacks)
       	{	
       		(censor ? StockArt.Eye : StockArt.NoEye).drawChip(gc, this, totalW/20, xp+totalW/20,
       					yp+totalW/20,highlightAll,ViticultureId.Eye,null);
       	}}
		
		}
      	if(gb.triggerCard!=null && !apCards)
       	{
       		gb.triggerCard.drawChip(gc, this, w/15, xp+totalW-w/25, yp+cardH-w/18,highlightAll,ViticultureId.ShowBigChip,null);
       	}

		if(nCards==0)
		{
			switch(state)
			{
			case FillWineOptional:
				GC.Text(gc, true, xp, yp, totalW,step/2,Color.black,null,s.get(NoWineOrderMessage));
				break;
			default:
				if(!uprootOnlyMode && plantMode)
				{
				quickExit = true;
				GC.Text(gc, true, xp, yp, totalW,step/2,Color.black,null,s.get(CantPlant));
				}
				break;
			}
		}
		
		if(!uprootMode && (cardDisplay.height()>0))
		{
		String cardMessage = oracleMode ? OracleCardsMessage
					: (( apCards && censor) || reviewOnly)
						? s.get(OtherCardsMessage,apname)
						: s.get(apCards ?  YourCardsMessage : AvailableCardsMessage);
		GC.Text(gc, true, xp+totalW/20, yp, cardW,step/2,Color.black,null,cardMessage);
		int cardStep = Math.min(cardH/((cardDisplay1==null)?3:4),(int)( cardW/Math.max(3, (nCards+1))));
		int secondCardStep = cardStep;
		int cardY = yp+step*3/2;
		if(unusedCards.height()>0) 
			{
			secondCardStep = secondCardStep*3/4; 
			cardDisplay1 = unusedCards; 
			card1Index = unusedIndex;
			} 
		if(cardDisplay1!=null)
		{
			cardY = yp+step*7/6;
			int cardy2 = cardY+step*3/2;
			HitPoint hp = (censor&!cardBacks)
					? null 
					: apCards
							? highlightAll 
							: highlight;
			if(drawStack(gc,state,null, cardDisplay1,
								hp,
							highlightAll,secondCardStep,xp+secondCardStep,cardy2, 0,1,0.0,
							cardLabel))
			{	
				if(apCards)
				{
					hp.hitCode = ViticultureId.Magnify;
					hp.hitObject = cardDisplay1.chipAtIndex(hp.hit_index);
					hp.arrow = StockArt.Eye;
					hp.awidth = step/4;
				}
				else 
					{ hp.hitObject = cardDisplay1.chipAtIndex(hp.hit_index);
					}
				hp.hit_index = card1Index.elementAt(hp.hit_index).index;
			}
			// mark the currently selected cards
			for(int i=0;i<cardDisplay1.height();i++)
			{
				ViticultureChip ch = cardDisplay1.chipAtIndex(i);
				int xpos = xp+cardStep+cardStep*i;
				CardPointer p = card1Index.elementAt(i);
				G.Assert(ch==p.card,"must match");
				if(selected.contains(p.source,p.card,p.index)) {
					
		     		mark.drawChip(gc,this,cardStep/2,xpos,cardy2,null);
				}
				Rectangle sr = new Rectangle(xpos-cardStep/2,cardy2+cardStep/2,cardStep,cardStep/3);
				viewCard(highlight,sr,ch);

			}
		}
		HitPoint hl = (censor&&!cardBacks) ? null 
				: apCards
					? highlightAll 
					: highlight;
		if(drawStack(gc,state,null, cardDisplay,
					hl,
					highlightAll,cardStep,xp+cardStep,cardY, 0,1,0.0, cardLabel))
		{	if(apCards)
			{
			hl.hitCode = ViticultureId.Magnify;
			hl.arrow = StockArt.Eye;
			hl.hitObject = cardDisplay.chipAtIndex(hl.hit_index);
			hl.awidth = step/4;
			}
			else
			{
			ViticultureChip card = cardDisplay.chipAtIndex(hl.hit_index);
			
			// in case this is a planting move, note the field that has been selected
			ViticultureCell selectedField = null;
			for(int lim = vineSelect.size()-1; lim>=0; lim--)
			{
				ViticultureCell field = vineSelect.elementAt(lim);
				ViticultureChip top = field.topChip();
				if((top!=null) && (top.type==ChipType.Field)) { selectedField = field; } 
			}

			if(plantMode 
					&& (selectedField!=null) 
					&& (limitedVines && !gb.canPlantValue(card,selectedField))
					)
			{
				hl.hitCode = ViticultureId.OverValue;
			}
			else {
				hl.hitObject = card;
			}
			hl.hit_index = cardIndex.elementAt(hl.hit_index).index;
			}
		}
		// mark the currently selected cards
		for(int i=0;i<cardDisplay.height();i++)
		{
			ViticultureChip ch = cardDisplay.chipAtIndex(i);
			int xpos = xp+cardStep+cardStep*i;
			CardPointer p = cardIndex.elementAt(i);
			if(selected.contains(p.source,p.card,p.index)) {
	     		mark.drawChip(gc,this,cardStep/2,xpos,cardY,null);
			}
			Rectangle sr = new Rectangle(xpos-cardStep/2,cardY+cardStep/2,cardStep,cardStep/3);
			viewCard(highlight,sr,ch);
		}
		}
		if(cardAndWineMode)
		{
			int fieldX = xp + cardW;
			int fieldY = yp + step*3/2;
			for(int i=0;i<wineIndex;i++)
			{	int fxp = fieldX+fstep*i+fstep/4;
				ViticultureCell field = gb.wineSelect[i];
				ViticultureChip top = field.topChip();
				ViticultureId wineId = top.type.getWineId();
				int min = ViticultureChip.minimumWineValue(wineId);
				ViticultureCell matchingCell = gb.getCell(wineId,pb.colCode,top.order-min);
				if(drawStack(gc,state,null,field,highlight,highlightAll, step,fxp,fieldY,0,0,0.2,null))
				{
					highlight.hitCode = ViticultureId.WineDisplay;
				}
				if(pb.selectedCells.contains(matchingCell))
				{	
	     			mark.drawChip(gc, this, fstep/3, fxp, fieldY+fstep/4,null);
				}
			}
			GC.Text(gc, true, fieldX-step/4, yp, fieldW,step/2,Color.black,null,s.get(AvailableWinesMessage));
		}
		else
		{
		int fds = fieldDisplay.size();
		if(fds>0)
		{
		
		int fieldX = xp + cardW+ (cardW==0?step/3:-step/3);
		int fieldY = yp + step*3/2;

		GC.Text(gc, true, fieldX, yp, fieldW,step/2,Color.black,null,s.get(AvailableFieldsMessage));
		if(harvesting  && !uprootMode && pb.hasHarvestMachine())
		{
			ViticultureChip.HarvestMachine.drawChip(gc,this,step,xp+step/2+step/8,yp+step*7/8,null);
		}
		//G.frameRect(gc, Color.red, fieldX,yp,fieldW,step/2);
		fieldX += (fieldW-(fds+1)*fstep)/2;	// center (fieldw is 1+fsd
		for(int i=0;i<fds;i++)
			{
			int fxp = fieldX+fstep*(i+1);
			ViticultureCell field = fieldDisplay.elementAt(i);
			ViticultureCell vine = vineDisplay.elementAt(i);
			int fieldOffset = Math.max(0, vine.height()-3)*(int)(0.4*fstep);
			boolean thisHasVines = vine.height()>0;
			int ypos = fieldY+(int)(vine.height()*0.2*fstep);
			double ystepsize = 0.4;
			HitPoint uproothighlight = showUproots ? highlight : null;
			if(drawStack(gc,state,null,vine,uproothighlight,highlightAll,fstep,fxp,ypos,0,0,ystepsize,null))
			{	//highlight.spriteRect = new Rectangle(fxp-fstep/2,ypos-fstep*2/3-(int)(highlight.hit_index*fstep*0.4),fstep,fstep*3/2); 
				uproothighlight.spriteColor = Color.blue;
			}
			if((thisHasVines || !uprootMode) 
					&& drawStack(gc,state,null,field,showUproots ? null : highlight,highlightAll,fstep,fxp,fieldY-fieldOffset,0,0,0.2,null))
				{
				if((selectedVine!=null)  
						&& (limitedVines && !gb.canPlantValue(selectedVine,field))
						)
					{ highlight.hitCode = ViticultureId.OverValue;
					}
					else 
					{	highlight.hitCode = ViticultureId.Choice_1;					
					}
				}
			if(vineSelect.contains(vine) || vineSelect.contains(field))
				{
				StockArt.Checkmark.drawChip(gc, this, fstep/3, fxp+fstep/4, fieldY+fstep/5, null);
				}
			}
		if(uprootMode|uprootOnlyMode)
		{
			for(int lim = gb.pendingMoves.size()-1; lim>=0; lim--)
			{	// mark the vines selected to be uprooted
				Viticulturemovespec m = (Viticulturemovespec)gb.pendingMoves.elementAt(lim);
				int position = m.from_index;
				ViticultureCell field = pb.fields[m.from_row];
				int ind = fieldDisplay.indexOf(field);
				int fxp = fieldX+fstep*(ind);
				ViticultureCell vine = vines[m.from_row];
				int ypos = fieldY+(int)(vine.height()*0.2*fstep);
				StockArt.Checkmark.drawChip(gc, this, fstep/3, fxp+step,ypos+fstep/2-(int)(position*fstep*0.4),null);
			}
		}
		

		if(apCards && (hi>=0))
		{	int sz = cardH/4;
			if(StockArt.NoEye.drawChip(gc, this, sz*2/3, (int)(xp+sz/2),
		        		(int)(yp+sz/2),
		        		highlightAll, 
		        		ViticultureId.Eye,
		        		null,1.1,1.3))
		        	{
		        	highlightAll.hitObject = activePlayer.cards;
		        	}


		}}}
       	if(apCards && G.pointInRect(highlightAll, fieldRect) && (highlightAll.hitCode==DefaultId.HitNoWhere))
   		{ 	// make the "my cards" display easier to get rid of
       		highlightAll.hitCode = ViticultureId.Eye;
       		highlightAll.hitObject = pb.cards;
		}
       	if(quickExit)
       	{	int dw = w/20;
       		Rectangle r = new Rectangle(centerX-dw,centerY-dw/2,dw*2,dw);
       		doneButton(gc,r,(gb.DoneState() ? highlight : null));
        	}
       	else
       	{
       	if((showFields && gb.underHarvest()) || optionalPlant)
       		{
       		int sw = w/40;
       		int left = optionalPlant ? xp+w/2 : xp+sw/2;
       		Rectangle r = new Rectangle(left,yp+cardH-sw-sw/4,sw*8,sw);
       		if(GC.handleRoundButton(gc,r,highlight,s.get0or1(plantMode ? NoPlantMessage : uprootMode ? UnderUprootMessage : UnderHarvestMessage,pb.selectedCells.size()),HighlightColor,rackBackGroundColor))
       			{
       			highlight.hitCode = GameId.HitDoneButton;
       			}
       		}
       	GC.frameRect(gc,Color.black,fieldRect);
		GC.setFont(gc, largeBoldFont());
       	}
		if(apCards)
		{
		if(StockArt.FancyCloseBox.drawChip(gc, this, totalW/20,
				 xp+totalW-totalW/20,yp+totalW/20,highlightAll,ViticultureId.Eye,null))
			{
			highlightAll.hitObject = activePlayer.cards;
			}

		}
		else
		{
       	int csize = totalW/20;
       	StockArt.FancyCloseBox.drawChip(gc, this, csize, xp+totalW-csize,yp+csize, highlightAll,ViticultureId.CloseOverlay,null);
		}
		switch(state)
		{
		default: break;
		case PlaySecondBlue:
		case PlaySecondYellow:
		if((gb.getState()!=ViticultureState.Confirm)
			&& uimode==UI.ShowCards
			&& GC.handleRoundButton(gc,new Rectangle(xp+totalW-totalW/3,h-h/10,totalW/3-totalW/15,h/15),
       					highlight,NoCardMessage,HighlightColor,rackBackGroundColor))
       	{
       		highlight.hitCode = GameId.HitDoneButton;
       	}}
    
    }
    // show cards and/or fields
    public void showMarket(Graphics gc, ViticultureBoard gb, Rectangle br,
    		HitPoint highlight0,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {	HitPoint highlight = highlight0;
        boolean cardBacks = false;
     	ViticultureState state = gb.resetState;
        StockArt mark = StockArt.Checkmark;
        PlayerBoard pb = gb.getCurrentPlayerBoard();
       	ViticultureCell cards = pb.oracleCards;
		ViticultureCell cardDisplay = gb.cardDisplay;
		CardPointerStack cardIndex = new CardPointerStack();
		cardDisplay.reInit();
		
		int hi = remoteWindowIndex(highlightAll);
		PlayerBoard activePlayer = (hi>=0) ? gb.pbs[hi] : showPlayerCards();
		if(activePlayer==null) { activePlayer = pb; }
		
    	CardPointerStack selected = pb.selectedCards;
		boolean extraHeight = false;

      	boolean censor = !reviewOnly &&  ((highlight==null || censor(pb,highlightAll)));
      	
		addCardMoves(cardDisplay,cardIndex,cards,targets,false,cardBacks,gb);

		int w = G.Width(br);
		int h = G.Height(br);
		int nFree = state.nFree();				// number of free cards in the market
		int nToTake = state.nToTake();
		
		int nCards = cardDisplay.height();
		int nCardSlots = (nCards==1 ? 2 : nCards);
		int step = Math.min(3*G.Height(br)/10,w/Math.max(5, 1+nCardSlots));
		
     	String cardLabel = censor ? ViticultureChip.BACK : null;
		int cardW = Math.max(0,step*nCardSlots);
		int totalW = cardW = Math.max(step*5+step/3,cardW);
		
		int centerX = G.centerX(br);
		int centerY = G.centerY(br);
		int xp = (int)(centerX-totalW/2);
		int yp = (int)(centerY-step*3/2-(extraHeight ? step/3 : 0));
		int cardH = step*3+(extraHeight ? step*3/4 : 0);
		int cost = pb.committedCost();
		
		Rectangle fieldRect = new Rectangle(xp,yp,totalW,cardH);
		ViticultureChip.Scrim.image.stretchImage(gc, fieldRect);  
    	

       	if(G.pointInRect(highlightAll, fieldRect)) 
       		{ highlightAll.neutralize(); 
			}

		{
		int mleft = xp+step/8;
		int doneWidth = w/10;
		int doneRowCenter = centerY+h/2-doneWidth;
		Rectangle r = new Rectangle(xp+totalW-doneWidth*3/2,doneRowCenter,doneWidth,doneWidth/2);
		boolean done = gb.DoneState();
		doneButton(gc,r,(done ? highlight : null));

		String msg = state.activity.getName();
		String tmsg = s.get(msg,nToTake);
		int bot = centerY+h/2-step/2;
		GC.Text(gc,false, mleft,bot,step,step/3,Color.blue,null,tmsg);
	
		// display the player's current green cards for reference
		int left = mleft+step*3/2;
		ViticultureCell c = pb.cards;
		for(int i=pb.cards.height()-1; i>=0;i--)
		{
			ViticultureChip ch = c.chipAtIndex(i);
			if(ch.type==ChipType.GreenCard)
			{
				ch.drawChip(gc,this,step/3,left,bot+step/20,censor ? null : highlightAll,ViticultureId.ShowBigChip,cardLabel);
				left += step/2;
			}
		}
		
		if(cost>pb.cash)
		{
			GC.Text(gc,true, mleft+step/4,doneRowCenter,totalW-mleft-doneWidth*2,step/3,Color.yellow,null,s.get(TooExpensive,pb.cash));
		}
		
       	if(allPlayersLocal()  && !cardBacks)
       		{	
       		(censor ? StockArt.Eye : StockArt.NoEye).drawChip(gc, this, totalW/20, xp+totalW/20,
       					yp+totalW/20,highlightAll,ViticultureId.Eye,null);
       		}	
		}
 
		String cardMessage = AvailableCardsMessage;
		GC.Text(gc, true, xp+totalW/20, yp, cardW,step/2,Color.black,null,cardMessage);
		int cardStep = Math.min(cardH/3,(int)( cardW/Math.max(3, (nCards+1))));
		int cardY = yp+(int)(step*1.2);

		
		if(drawStack(gc,state,null, cardDisplay,
					(censor&&!cardBacks) ? null 
						: highlight,
					highlightAll,cardStep,xp+cardStep,cardY, 0,1,0.0, cardLabel))
		{	
			ViticultureChip card = cardDisplay.chipAtIndex(highlight.hit_index);
			highlight.hitObject = card;
			highlight.hit_index = cardIndex.elementAt(highlight.hit_index).index;
		}
		
		// mark the currently selected cards and the prices for the cards
		for(int i=0;i<cardDisplay.height();i++)
			{
			ViticultureChip ch = cardDisplay.chipAtIndex(i);
			int xpos = xp+cardStep+cardStep*i;
			CardPointer p = cardIndex.elementAt(i);
			if(selected.contains(p.source,p.card,p.index)) {
	     		mark.drawChip(gc,this,cardStep/2,xpos,cardY,null);
			}
			Rectangle sr = new Rectangle(xpos-cardStep/2,cardY+cardStep/2,cardStep,cardStep/3);
			viewCard(highlight,sr,ch);
			
			Rectangle price = new Rectangle(xpos-cardStep/3,cardY+cardStep*2/3,cardStep*2/3,cardStep/6);
			priceDisplay(gc, pb, state, highlightAll,price,null,i<nFree?0:1);
			}

  
		
		int csize = totalW/20;
       	StockArt.FancyCloseBox.drawChip(gc, this, csize, xp+totalW-csize,yp+csize, highlightAll,ViticultureId.CloseOverlay,null);
       	GC.frameRect(gc,Color.black,fieldRect);
    }
    // show cards and/or fields
    public void showOptions(Graphics gc, ViticultureBoard gb, Rectangle br,
    		HitPoint highlight0,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {	
    	HitPoint highlight = highlight0;
        Option options[] = Option.values();
		Rectangle fieldRect = br;
		int w = G.Width(fieldRect);
		int h = G.Height(fieldRect);
		int left = G.Left(fieldRect);
		int top = G.Top(fieldRect);
		ViticultureChip.Scrim.image.stretchImage(gc, fieldRect);  
      	if(G.pointInRect(highlightAll, fieldRect)) 
   		{ highlightAll.neutralize(); 
		}
    	
		int fontsize = standardFontSize();
		int step = Math.min(fontsize * 4,h/Math.max(7,(options.length+1)));
		int x =left + w/6;
		int y = (h-step*options.length)/2+top;
		int fullW = w*2/3;
		if(highlight!=null)
		{
		GC.setFont(gc,standardBoldFont());
		GC.Text(gc,true,x-step,y-step/2,step,step/2,Color.black,null,s.get(AcceptMessage));
		GC.Text(gc,true,x+fullW,y-step/2,step,step/2,Color.black,null,s.get(VetoMessage));
		}
		GC.setFont(gc,largeBoldFont());
		int nPlayers = gb.nPlayers();
		for(Option op : options)
		{	// the limit option is only relevant if 5-6 players
			if(nPlayers>4 || (op!=Option.LimitPoints))
			{
			TextButton toggle = new TextButton(op.message,ViticultureId.SetOption,op.message,Color.lightGray,null,null); 
			boolean on = gb.testOption(op);
			toggle.setValue(on);
			toggle.textColor = on ? Color.yellow : Color.black;
			toggle.setBounds(x,y,w*2/3,step);
			int l = x-step/2;
			int r = x+fullW+step/2;
			int cy = y+step/2;
			for(PlayerBoard pb : gb.pbs) 
			{
				if(pb.selectedOptions.test(op)) 
					{ pb.getScoreMarker().drawChip(gc,this,step,l,cy,null); 
					  StockArt.Checkmark.drawChip(gc,this,step/2,l+step/4,cy,null);
					  l -= step/2; 
					}
				if(pb.unSelectedOptions.test(op)) 
					{ pb.getScoreMarker().drawChip(gc,this,step,r,cy,null);
					  StockArt.SmallX.drawChip(gc,this,step,r+step/4,cy,null);
					  r += step/2; 
					}
			}
			if(toggle.show(gc,highlight))
			{
				highlight.hitObject = op;
				highlight.hit_index = on ? 0 : 1;	// toggle
			}
			
			y += step;
			}
		}
		if(highlight!=null)
		{
		int xp = left+step*2/3;
		int ystep = step*3/2;
		y = top+h/2-((ystep*mainBoard.nPlayers())/2);
		for(PlayerBoard p : gb.pbs)
		{
			ViticultureChip ch = p.getRooster();
			boolean ready = p.isReady;
			boolean me = allPlayersLocal() || ( p.boardIndex==getActivePlayer().boardIndex);
			if(ch.drawChip(gc,this,step*2,xp,y,me ? highlight:null,ViticultureId.SetReady,null))
			{
				highlight.hitObject = p;
				highlight.hit_index = ready ? 0 : 1;
				highlight.setHelpText(s.get(AcceptOptionsMessage));
			}
			if(ready)
			{
				StockArt.Checkmark.drawChip(gc,this,step,xp,y,null);
			}
			y+= step*3/2;
		}}
		
		int csize = w/20;
       	StockArt.FancyCloseBox.drawChip(gc, this, csize, left+w-csize,top+csize, highlightAll,ViticultureId.CloseOverlay,null);
       	GC.frameRect(gc,Color.black,fieldRect);
    }
    

    // show cards and/or fields
    public void showPandM(Graphics gc, ViticultureBoard gb, Rectangle br,
    		HitPoint highlight0,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {	HitPoint highlight = highlight0;
        StockArt mark = StockArt.Checkmark;
        PlayerBoard pb = gb.getCurrentPlayerBoard();
       	ViticultureCell cards = pb.cards;
		
		int hi = remoteWindowIndex(highlightAll);
		PlayerBoard activePlayer = (hi>=0) ? gb.pbs[hi] : showPlayerCards();
		if(activePlayer==null) { activePlayer = pb; }
		
    	CardPointerStack selected = pb.selectedCards;
		boolean extraHeight = false;

		// move mamas to a new stack, prepare for a 2x2 display
		ViticultureChip m1 = cards.chipAtIndex(3);
		ViticultureChip m2 = cards.chipAtIndex(2);
		ViticultureChip p1 = cards.chipAtIndex(1);
		ViticultureChip p2 = cards.chipAtIndex(0);

		int w = G.Width(br);
		int h = G.Height(br);
		
		int nCardSlots =2;
		int step = Math.min(3*G.Height(br)/10,w/2);
		
 		int cardW = Math.max(0,step*nCardSlots);
		int totalW = cardW = Math.max(step*5+step/3,cardW);
		
		int centerX = G.centerX(br);
		int centerY = G.centerY(br);
		int xp = (int)(centerX-totalW/2);
		int yp = (int)(centerY-step*3/2-(extraHeight ? step/3 : 0));
		int cardH = step*3+(extraHeight ? step*3/4 : 0);
		
		Rectangle fieldRect = new Rectangle(xp,yp,totalW,cardH);
		ViticultureChip.Scrim.image.stretchImage(gc, fieldRect);  
    	

       	if(G.pointInRect(highlightAll, fieldRect)) 
       		{ highlightAll.neutralize(); 
			}

		int doneWidth = w/10;
		int doneRowCenter = h-doneWidth/2;
		Rectangle r = new Rectangle(xp+totalW-doneWidth*3/2,doneRowCenter,doneWidth,doneWidth/2);
		boolean done = gb.DoneState();
		doneButton(gc,r,(done ? highlight : null));

 		int cardStep = (int) Math.min(cardH*0.6, cardW*0.6);
 		int cardV = (int)(cardStep*0.72*1.1);
		int cardY = (int)(yp+step*0.78);
		int cardW1 = (int)(cardStep*1.1);
		int cardX = xp+cardStep*2/3;
		Rectangle m1Rect = new Rectangle(cardX-cardW1/2,cardY-cardV/2,cardW1,cardV);
		if(m1.drawChip(gc,this,m1Rect,highlight,ViticultureId.MamaCards,(Text)null))
		{	highlight.hitObject = pb.cards;
			highlight.hit_index = 3;
			highlight.spriteRect = m1Rect;
			highlight.spriteColor = Color.red;
		}
		if(selected.contains(m1)) {  mark.drawChip(gc,this,cardStep/4,cardX,cardY,null);  }

		int m2Y = (int)(cardY+cardStep*0.8);
		Rectangle m2Rect = new Rectangle(cardX-cardW1/2,m2Y-cardV/2,cardW1,cardV);
		if(m2.drawChip(gc,this,m2Rect,highlight,ViticultureId.MamaCards,(Text)null))
		{	highlight.hitObject = pb.cards;
			highlight.hit_index = 2;
			highlight.spriteRect = m2Rect;
			highlight.spriteColor = Color.red;
		}
		if(selected.contains(m2)) {  mark.drawChip(gc,this,cardStep/4,cardX,m2Y,null);  }
		int cardW2 = (int)(cardStep*1.36);
		int py = cardY-cardStep/2;
		int px = cardX+cardW2/3;
		boolean ca = gb.choiceA.isSelected();
		boolean cb = gb.choiceB.isSelected();
		boolean selectp1 = selected.contains(p1);
		Rectangle cr1 = new Rectangle(px,py,cardW2,cardStep);
		gb.choiceA.selected = selectp1 && ca;
		gb.choiceB.selected = selectp1 && cb;
		if(resolvePapa(gc, pb, gb, highlight, p1,cr1,false))
		{
			highlight.hit_index = 1;
			if(highlight.hitCode==DefaultId.HitNoWhere)
			{	ViticultureCell choice = ca ? gb.choiceB : gb.choiceA;
				highlight.hitObject = choice;
				highlight.hitCode = choice.rackLocation();
				highlight.spriteColor=Color.red;
				Rectangle sp = new Rectangle((int)(px+cardStep*0.17),
								(int)(py+cardStep*0.11),
								(int)(cardW2*0.8),
								(int)(cardStep*0.78));
				highlight.spriteRect = sp;
			}
		}
		if(selectp1) { mark.drawChip(gc,this,cardStep/4,px+cardW2/2,py+cardStep/2,null); }
		
		cardY += (int)(cardStep*0.8);
		
		boolean selectp2 = selected.contains(p2);
		gb.choiceA.selected = selectp2 && ca;
		gb.choiceB.selected = selectp2 && cb;
		py = cardY-cardStep/2;
		Rectangle cr2 = new Rectangle(px,py,cardW2,cardStep);
		if(resolvePapa(gc, pb, gb, highlight,p2,cr2,false))
		{
			highlight.hit_index = 0;
			if(highlight.hitCode==DefaultId.HitNoWhere)
			{	ViticultureCell choice = ca ? gb.choiceB : gb.choiceA;
				highlight.hitObject = choice;
				highlight.hitCode = choice.rackLocation();
				highlight.spriteColor=Color.red;
				Rectangle sp = new Rectangle((int)(px+cardStep*0.17),
								(int)(py+cardStep*0.11),
								(int)(cardW2*0.8),
								(int)(cardStep*0.78));
				highlight.spriteRect = sp;
			}

		}
		if(selected.contains(p2)) { mark.drawChip(gc,this,cardStep/4,px+cardW2/2,cardY,null); }

		
		gb.choiceA.selected = ca;
		gb.choiceB.selected = cb;
		
		int csize = totalW/20;
       	StockArt.FancyCloseBox.drawChip(gc, this, csize, xp+totalW-csize,yp+csize, highlightAll,ViticultureId.CloseOverlay,null);
       	GC.frameRect(gc,Color.black,fieldRect);
    }
    

    public void priceDisplay(Graphics gc,PlayerBoard pb,ViticultureState state,HitPoint hp,Rectangle r,String name,int price)
    {
    	GC.setFont(gc, standardBoldFont());
		if(name!=null) { GC.Text(gc, true,r,Color.black,null,name); }
		loadCoins(pb.coinDisplay,price);
		pb.coinDisplay.selected = false;
		if(price>0)
			{
			int coinw = G.Width(r)*2/3;
			drawStack(gc,state,null, pb.coinDisplay,null,hp, coinw,G.centerX(r)-coinw/8,G.Bottom(r)+coinw/2, 0,0.5,0.00,null);
			}
		else {  
				GC.Text(gc,true,G.Left(r),G.Bottom(r),G.Width(r),G.Height(r)*2,Color.black,null,s.get(FreeMessage));
			}
    }
    public void showBuildable(Graphics gc, ViticultureBoard gb, Rectangle br,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {	PlayerBoard pb = (showBuildings && !allowed_to_edit)
    						? gb.getPlayerBoard(getActivePlayer().boardIndex)
    						: gb.getCurrentPlayerBoard();
    	boolean quickExit = false;
    	int w = G.Width(br);
    	int cx = G.centerX(br);
    	int cy = G.centerY(br);
    	int nBuilds = 0;
    	int tourBuilds = 0;
    	int discount = 0;
    	HitPoint mainHighlight = showBuildings ? null : highlight;
       	ViticultureState state = gb.resetState;
       	boolean censor = !reviewOnly && censor(pb,highlightAll);
       	switch(state)
    	{
       	case BuildStructure23Free:
       	case BuildStructureFree:
       	case Build2StructureFree:
       		discount = 999;
       		break;
       	case BuildStructureDiscount3:
       		discount = 3;
       		break;
       	case BuildStructureBonus:
       		discount = 1;
       		break;
       	case BuildAtDiscount2:
       	case BuildAtDiscount2forVP:
       		discount = 2;
       		break;
    	case BuildTourBonus:
    		discount = 1;
			//$FALL-THROUGH$
		case BuildTour:
  			tourBuilds = 2;
  			break;
  		default: break;
    	}
       	if(pb.hasWorkshop()) { discount+=1; }
       	
       	if(showBuildings) { nBuilds = pb.buildable.length;}
       	else {
    	for(int lim=pb.buildable.length-1; lim>=0; lim--)
    	{
    		ViticultureCell c = pb.unBuilt[lim];
    		if(gb.legalToHit(c,targets))
    		{
    			nBuilds++;
    		}
    	}}
    	ViticultureCell cardDisplay = gb.cardDisplay;
    	CardPointerStack cardIndex = new CardPointerStack();
    	cardDisplay.reInit();
    	if(showBuildings) 
    	{	
    		ViticultureCell cards = pb.cards;
    		for(int lim=cards.height()-1; lim>=0; lim--)
    		{
    			ViticultureChip ch = cards.chipAtIndex(lim);
    			if(ch.type==ChipType.StructureCard) 
    				{ nBuilds++; 
    				cardDisplay.addChip(ch);
    				cardIndex.push(pb.cards.rackLocation(),ch,lim);
    				}
    		}
    	}
    	else
    	{
    	if(gb.legalToHit(pb.cards,targets) || censor)
    	{	
    		if(censor)
    		{	// add all the cards for the other players, so they can't tell which ones
    			// you might be able to build
    			ViticultureCell cards = pb.cards;
    			for(int i=0;i<cards.height();i++)
    			{
    				ViticultureChip chip = cards.chipAtIndex(i);
    				if(chip.type==ChipType.StructureCard)
    				{	nBuilds++;
    					cardDisplay.addChip(chip);
    					cardIndex.push(pb.cards.rackLocation(),chip,i);
    				}
    			}
    		}
    		else
    		{
    		Viticulturemovespec m = targets.get(pb.cards);
    		while(m!=null) 
    			{ nBuilds++;
    			ViticultureChip ch = pb.cards.chipAtIndex(m.from_index);
    			cardDisplay.addChip(ch);
    			cardIndex.push(pb.cards.rackLocation(),ch,m.from_index);
    			m=(Viticulturemovespec)m.next; 
    			}}
    	}}

    	int step = (int)(w/Math.max(6,(nBuilds+tourBuilds+2)));
    	 
    	int buildW = step*((nBuilds==1)?2:nBuilds);
    	int tourW = tourBuilds*step;
    	int frameW = Math.max(5*step, buildW+tourW+step);
    	
    	int frameH = step*3;
    	int xp = (cx-frameW/2);
    	int yp = (cy-frameH/2);
    	int yp0 = yp;
    	int xp0 = xp;
    	Rectangle frame = new Rectangle(xp,yp,frameW,frameH);
    	yp += step;
    	ViticultureChip.Scrim.image.stretchImage(gc, frame);  
    	

       	if(G.pointInRect(highlightAll, frame)) { highlightAll.neutralize(); }


       	GC.frameRect(gc,Color.black,frame);
    	GC.setFont(gc, largeBoldFont());

    	if((nBuilds+tourBuilds)==0) 
    		{
    		quickExit = true;
    		GC.Text(gc, true, xp,yp,frameW,frameH/10, Color.black, null,CantBuildMessage);
    		}
    	if(buildW>0) 
    		{ GC.Text(gc, true, xp, yp-step, buildW,step,Color.black,null,
    				s.get(showBuildings ? AllBuildingsMessage : AvailableBuildingsMessage));
    		}
    	xp+=step;
    	for(int lim=pb.buildable.length-1; lim>=0; lim--)
    	{
    		ViticultureCell c = pb.unBuilt[lim];
    		if(showBuildings || gb.legalToHit(c,targets))
    		{
    			ViticultureCell d = pb.unBuilt[lim];
    			ViticultureCell built = pb.buildable[lim];
    			ViticultureChip chip = pb.getContent(c);
    			d.reInit();
    			d.addChip(chip);
    			if(drawStack(gc,state,null, d,mainHighlight,highlightAll, step,xp,yp+step/3, 0,0.0,0.0,null))
    				{	highlight.setHelpText(s.get(built.toolTip));
    				}
    			if(showBuildings && built.topChip()!=null)
    			{
    				StockArt.Checkmark.drawChip(gc,this,step/4,xp,yp,null);
    			}
    			
    			/** add prices */
    			Rectangle price = new Rectangle((int)(xp-step*0.4),yp+step*2/3,(int)(step*0.8),step/4);
      			int netCost = Math.max(0,pb.buildable[lim].cost-discount);
      			priceDisplay(gc,pb,state,highlightAll,price,chip.type.prettyName(),netCost);
    			/*
    			GC.setFont(gc, standardBoldFont());
    			GC.Text(gc, true,(int)(xp-step*0.4),yp+step*2/3,(int)(step*0.8),step/4,Color.black,null,chip.type.prettyName());
    			loadCoins(pb.coinDisplay,netCost);
    			pb.coinDisplay.selected = false;
    			if(netCost>0)
    				{drawStack(gc,state,null, pb.coinDisplay,null,highlightAll, step/2,xp,yp+step*6/5, 0,0.5,0.00,null);
    				}
    			else { 
     				GC.Text(gc,true,xp-step/2,yp+step,step,step/4,Color.black,null,s.get(FreeMessage));
    				}
    				*/
    			xp += (int)(0.9*step);
    		}
    	}
    	if(cardDisplay.height()>0)
    	{
    		if(drawStack(gc,state,null,cardDisplay,mainHighlight,highlightAll,step,xp+step/4,yp+step*1/3,0,1,0,censor ? ViticultureChip.BACK : null))
    		{
    			highlight.hitObject = cardDisplay.chipAtIndex(highlight.hit_index);
    			highlight.hit_index = cardIndex.elementAt(highlight.hit_index).index;
    		}
        	CardPointerStack discards = pb.selectedCards;
    		// mark the currently selected cards
    		for(int i=0;i<cardDisplay.height();i++)
    		{	int xpos = xp+step*i;
    			int ypos = yp-step+step*3/2;
    			ViticultureChip ch = cardDisplay.chipAtIndex(i);
    			int ci = cardIndex.elementAt(i).index;
    			if(discards.contains(pb.cards.rackLocation(),ch,ci)) 
    			{
    				boolean isDiscard = state.discardCards()>=0;
    	     		(isDiscard?StockArt.Exmark:StockArt.Checkmark).drawChip(gc,this,step/2,xpos,ypos,null);
    			}
    			int netCost = Math.max(0,ch.costToBuild()-discount);
    			loadCoins(pb.coinDisplay,netCost);
    			if(!censor || discount>100)	// show "cost" for free things
    			{
    			if(netCost>0)
    			{
    				drawStack(gc,state,null, pb.coinDisplay,null,highlightAll, step/2,xpos+step/6,ypos+step*7/8, 0,0.5,0.00,null);
    			}
    			else 
    			{
    				GC.Text(gc,true,xpos-step/4,ypos+step*2/3,step,step/4,Color.black,null,s.get(FreeMessage));
    			}}
    		}
    		
    	}
    	
    	//
    	// optionally add the option to give a tour
    	//
     	int bonus = 2;
    	switch(state)
    	{
    	case BuildTourBonus:
    		bonus++;
			//$FALL-THROUGH$
		case BuildTour:
        	xp = xp0+buildW;
        	// note that coding this as "choice0" is a terrible mistake, see the long comment
        	// in editHistory.  It should have been coded as another build choice, ie "build money"
    		loadCoins(gb.choice0,bonus);
    		drawStack(gc,state,null, gb.choice0,highlight,highlightAll, step,xp+tourW/2,yp+step, 0,0.5,0.0,null);
    		GC.setFont(gc, standardBoldFont());
    		GC.Text(gc, true, xp,yp+step+step/3,tourW,step/2,Color.black,null,GiveATourMessage);
    		break;
    	default: ;
    	}

    	if(quickExit)
    	{
    		int dw = w/20;
       		Rectangle r = new Rectangle(cx-dw,cy-dw/2,dw*2,dw);
       		doneButton(gc,r,(gb.DoneState() ? highlight : null));	
    	}
    	else
    	{
           	int csize = frameW/20;
           	if(!showBuildings)
           	{
           	if(gb.triggerCard!=null)
           	{
           		gb.triggerCard.drawChip(gc, this, w/15, xp0+frameW-csize, yp0+frameH-csize*3/2,highlightAll,ViticultureId.ShowBigChip,null);
           	}
           	if(gb.DoneState() 
           			&& (gb.getState()!=ViticultureState.Confirm) 
           			&& GC.handleRoundButton(gc,new Rectangle(xp0+(int)(0.7*frameW),yp0+csize/2,frameW/6,frameW/18),
           					highlight,NoBuildMessage,HighlightColor,rackBackGroundColor))
           	{
           		highlight.hitCode = GameId.HitDoneButton;
           	}}
           	StockArt.FancyCloseBox.drawChip(gc, this, csize, 
           			xp0+frameW-csize,yp0+csize, highlightAll,showBuildings ? ViticultureId.CancelBigChip: ViticultureId.CloseOverlay,null);
    	}
    }
    double decksLoc[][] = {
			// green
	{0.305,0.125,0.06},		// greendiscards
		{0.45,0.125,0.06},		// yellowdiscards
	{0.595,0.125,0.06},		// purplediscards
	{0.745,0.125,0.06},		// bluediscards
		{0.89,0.125,0.06},		// structurediscards
    };
    double greenLoc[][] = {{0.237,0.125,0.06}};			// green
    double yellowLoc[][] = {{0.384,0.125,0.06}};		// yellow
    double purpleLoc[][] = {{0.53,0.125,0.06}};			// purple
    double blueLoc[][] =   {{0.68,0.125,0.06}};			// blue
    double structureLoc[][] = {{0.822,0.125,0.06}};		// structure
    double yokeLoc[][] = {
    		{0.925,0.74,0.04},
    		{0.92,0.67,0.04},};
    double yokeLocRest[][] = {
    		{0.64,0.48,0.02},
    		{0.66,0.48,0.02},
    		{0.812,0.59,0.015},
    		{0.796,0.59,0.015},
    		{0.80,0.61,0.015},
    		{0.80,0.625,0.015},
    		};		// structure

    double workerPlacementLocs[][] = {
    		{0.295,0.39,0.04},
    		{0.29,0.487,0.04},
    		{0.2939,0.584,0.04},
    		{0.292,0.772,0.04},

       		{0.4444,0.428,0.04},
    		{0.4455,0.52,0.04},
    		{0.444,0.695,0.04},
    		{0.446,0.788,0.04},

       		{0.618,0.396,0.04},
    		{0.63,0.508,0.04},
    		{0.6367,0.618,0.04},
    		{0.6476,0.79,0.04},

       		{0.854,0.405,0.04},
    		{0.866,0.5,0.04},
    		{0.851,0.598,0.04},
    		{0.802,0.733,0.04},

    };
    double specialWorkerLocs[][] = {
    		{0.96,0.56,0.03},
    };

    double starsLoc[][] = {
    		{0.09,0.7,0.04},	// luca
    		{0.099,0.8,0.04},	// pisa
    		{0.147,0.791,0.04},	// firenze
    		{0.08,0.88,0.04}, // livorno
    		{0.145,0.875,0.04},		// siena
    		{0.19,0.81,0.04},	// arrezo
    		{0.12,0.975,0.04},	// grosseto
    };
    
  
    private boolean showBigStack = false;
    private ViticultureCell showBigStackFrom = null;
    private ViticultureCell bigStack = new ViticultureCell(ViticultureId.CancelBigChip);
    private ViticultureCell flashStack = new ViticultureCell(ViticultureId.CancelBigChip);
    
    private void changeBigStack(ViticultureChip newchip)
    {
    	if(newchip==null || (showBigStack && (bigStack.topChip()==newchip) && (showBigStackFrom==null))) 
    		{ bigStack.reInit();
    		  showBigStack = false; 
    		}
    	else {
    	bigStack.reInit();
    	bigStack.addChip(newchip);
    	showBigStack = true;
    	}
    	showBigStackFrom = null;
    }
    private void changeBigStack(ViticultureCell from)
    {
    	if(from==showBigStackFrom) { showBigStackFrom = null; showBigStack = false; }
    	else {
    	bigStack.copyFrom(from);
    	bigStack.selected=false;
    	showBigStackFrom = from;
    	showBigStack = true;
    	}
    }
    private long flashChipTimer = 0;
    private UI previousUI=null;
    private boolean overlayClosed = false;
    private int overlayClosedFor = -1;
    
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, ViticultureBoard gb, Rectangle brect, 
    			HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets,UI ui)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  

        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        HitPoint hitBoard = highlight;
        ViticultureState resetState = gb.resetState;
        ViticultureState state = gb.getState();
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn());
        boolean tempOff = currentZoomZone!=null;
        
        ViticultureChip.NeutralBuilding.drawChip(gc,this,gb.pToS(0.04),
        			gb.pToX(0.21),gb.pToY(0.570),
        			highlightAll,ViticultureId.ShowBuildings,s.get(ShowBuildingInfo));
        if(mainBoard.variation==ViticultureVariation.viticulturep)
        {
        	ViticultureChip.GenericWine.drawChip(gc,this,gb.pToS(0.03),
        			gb.pToX(0.212),gb.pToY(0.50),
        			highlightAll,ViticultureId.ShowOptions,s.get(ShowOptionInfo));
        	
        }
        
        if(showBigStack) { hitBoard = null; tempOff = true; }
        HitPoint tipHighlight = (ui==UI.Main)||overlayClosed ? highlightAll : null;
        if(showBuildings) { ui = UI.ShowBuildable; overlayClosed = false; }
        if(showOptions) { ui = UI.ShowOptions; overlayClosed = false; highlight = null; }
        switch(ui)
        {
		case ShowWakeup:
        case ShowStars:
        	break;
        case Main: 
            if(previousUI!=ui) 
        		{ 	setBoardMagnifiers(null); 
        		}
        	autoZoom = true; 
        	
        	if(gc!=null) { loadCoins(gb.yokeCash,8); }
        	
        	for(PlayerBoard pb : gb.pbs) { pb.publicCensoring = true; } 
        	
        	if(!showBigStack && (currentZoomZone==null))
        	{
            GC.setFont(gc,standardBoldFont());
            boolean fullPass = state==ViticultureState.FullPass;
            if((resetState==ViticultureState.Play)||fullPass)
            {boolean canHit = ((state==ViticultureState.Play && gb.pickedObject==null) || fullPass);
             PlayerBoard pb = gb.getCurrentPlayerBoard();
             if(GC.handleRoundButton(gc,passRect,canHit ? highlight : null,
         		   s.get(NextSeasonMessage,s.get(NextSeasons[gb.season(pb)])),
         		   HighlightColor,fullPass ? HighlightColor : rackBackGroundColor))
             	{
            	highlight.hitCode = GameId.HitPassButton;
             	}
             if((gb.season(pb)==3) && fullPass && (pb.workers.topChip()!=null))
             	{
            	GC.Text(gc, true, passWarnRect,Color.red,Color.black,UnplacedWorkerWarning);
            	GC.frameRect(gc,Color.red,passWarnRect);
             	}
            }}


        	break;
        default: 
        	if(previousUI!=ui) 
    			{
        		setBoardMagnifiers(null); 
    			}
        	autoZoom = true;
        	
        	hitBoard = null;	// prevent hitting any targets on the main board
          	if(!overlayClosed) { tempOff = true; }
        }
        hintRect.setTemporarilyOff(tempOff);
    	previousUI = ui;
    	
       	if(currentZoomZone==null || currentZoomZone==starRect)
    	{
    	Rectangle scale = zoomScale(starRect);
        if(zoomer.drawMagnifier(gc,highlightAll,scale,currentZoomZone!=null,0.06,0.65,0.8,0))
        {	highlightAll.hitData = starRect;
        	highlightAll.hitCode = ViticultureId.Magnifier;
        }}
    	if(currentZoomZone==null || currentZoomZone==wakeupRect)
    	{
    	Rectangle scale2 = zoomScale(wakeupRect);
    	if(zoomer.drawMagnifier(gc,highlightAll,scale2,currentZoomZone!=null,0.06,0.65,0.78,0))
        {	highlightAll.hitData = wakeupRect;
        	highlightAll.hitCode = ViticultureId.Magnifier;
        }}
    	
    	drawBoardBackgroundElements(BACKGROUND_OPTIMIZATION?null:gc,pl,gb,brect,hitBoard,tipHighlight,targets); 
    	
        drawArray(gc,true,state,pl,gb,brect,hitBoard,tipHighlight,targets,yokeLoc,0.2,0.0,null,
        		gb.dollarWorker,gb.yokeCash);
        
       //drawArray(gc,pl,gb,brect,hitBoard,highlightAll,targets,yokeLoc,0.2,0.0,null,gb.blank);

  
        
         for(int i=0;i<workerPlacementLocs.length;i++)
        {
        	drawWorkerArray(gc,pl,gb,brect,hitBoard,tipHighlight,targets,workerPlacementLocs[i],gb.mainBoardWorkerPlacements[i]);
        }
        if(drawArray(gc,true,state,pl,gb,brect,highlightAll,tipHighlight,targets,specialWorkerLocs,0,2,null,gb.specialWorkerCards))
        {
        	highlightAll.arrow = StockArt.Eye;
        	highlightAll.awidth = CELLSIZE/2;
        }
        
        drawRoosterArray(gc,gb,brect,hitBoard,tipHighlight,targets,gb.roosterTrack);
        
        drawArray(gc,true,state,pl,gb,brect,hitBoard,tipHighlight,targets,starsLoc, 	0.04, 0.1, null,
        		gb.starTrack);
        drawScoringTrack(gc,gb,brect,hitBoard,targets,gb.scoringTrack);
        drawResidualTrack(gc,pl,gb,brect,hitBoard,targets,gb.residualTrack);
                
        int cx = G.centerX(brect);
        int cy = G.centerY(brect);
        for(PlayerBoard pb : gb.pbs)
		{
			for(ViticultureCell c = pb.uiCells; c!=null; c=c.next)
				{ c.rotateCurrentCenter(gc,cx,cy); 
				}
		}
        
        GC.setRotation(gc,pl.displayRotation,cx,cy);
        HitPoint.setRotation(highlightAll, pl.displayRotation,cx, cy);
        switch(resetState)
        {
        case TakeActionPrevious:
    		{
    			ViticultureCell c = gb.getCurrentPlayerBoard().selectedCells.top();
    			if(c!=null) { StockArt.Checkmark.drawChip(gc, this, CELLSIZE,c.centerX(),c.centerY(),null); }
    		}
    		break;
    	default: break;
        }
        Rectangle rotatedBoard = G.copy(null,brect);
        G.setRotation(rotatedBoard, pl.displayRotation, G.centerX(brect),G.centerY(brect));
        
        ViticultureChip flash = gb.flashChip;
    	gb.flashChip = null;
    	if(flash==null)
    	{	int act = allowed_to_edit ? gb.whoseTurn : getActivePlayer().boardIndex;
    		PlayerBoard pb = gb.pbs[act];
    		flash = pb.flashChip;
    		pb.flashChip = null;
    	}
        if(flash!=null)
        {
        	flashStack.reInit();
        	flashStack.addChip(flash);
        	showBigStack = true;
        	flashChipTimer = G.Date()+2000;	// 2 seconds
        }
        if((G.Date()>flashChipTimer) && (flashChipTimer>0)) 
        	{ flashChipTimer = 0;
        	  flashStack.reInit();
        	  showBigStack = (bigStack.topChip()!=null); 
        	}
        ViticultureCell bc = flashChipTimer>0 ? flashStack : bigStack;
        showOverlay(ui,gc,rotatedBoard,
        		gb,showBigStack,bc,highlight,highlightAll,targets);
 
        zoomer.drawMagnifier(gc,highlightAll,brect,0.03,0.93,0.99,0);
        GC.setRotation(gc,-pl.displayRotation,cx,cy);
        HitPoint.setRotation(highlightAll, -pl.displayRotation,cx, cy);
        
    }
    private void showOverlay(UI ui,Graphics gc,Rectangle brect,ViticultureBoard gb,boolean showBig,ViticultureCell bigStack,
    		HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets)
    {
    boolean closed = overlayClosed;
    
    switch(ui)
    {
    case ShowAPCards:
    	// ShowAPCards is different because the user caused it, not the game.
    	closed = false;
    	break;
    default: break;
    }
    if(closed)
    	{ int who = remoteViewer>=0 ? remoteViewer : getActivePlayer().boardIndex;
    	  if(who!=overlayClosedFor && (gb.whoseTurn==who)) 
    	  	{	closed = overlayClosed = false;
    	  		overlayClosedFor = -1;
    	  	}
    	}
    if(showBig && bigStack!=null)
    {
    	showBigStack(gc,gb,brect,highlightAll,bigStack);
    }
    else if(closed && ui!=UI.Main) { showClosedOverlay(gc,brect,highlightAll); }
    else
    switch(ui)
    {
    default: 
    	G.p1("No ui for "+ui);
    	break;
    case ShowOptions:
    	{
    	showOptions(gc,gb,brect,highlight,highlightAll,targets);
    	}
    	break;
    case ShowPandM:
    	showPandM(gc,gb,brect,highlight,highlightAll,targets);
    	break;
    case ShowMarket:
    	showMarket(gc,gb,brect,highlight,highlightAll,targets);
    	break;
    case Main: 
    	uprootMode = false;
    	overlayClosed = false;
    	overlayClosedFor = -1;
    	break;	// done if we want the main UI
    case ScoreSheet:
    	showScoreSheet(gc,mainBoard,brect,highlight,highlightAll,targets);
    	break;
    case ShowWinesSale:
    	showWinesSale(gc,gb,brect,highlight,highlightAll,targets);
    	break;
    	
    case ShowWines:
    	showWines(gc,gb,brect,highlight,highlightAll,targets);
    	break;
    	
    case ShowTrades:
    	showTrades(gc,gb,brect,highlight,highlightAll,targets);
    	break;
    
    case ShowBuildable:
    	showBuildable(gc,gb,brect,highlight,highlightAll,targets);
    	break;
    	
    case ShowCard:	// resolve a card with options
    	showCard(gc,gb,brect,highlight,highlightAll,targets);
    	break;
    	
    case ShowWorkers:
    	showWorkers(gc,gb,brect,highlight,highlightAll,targets);
    	break;
    case ShowPlayers:
    	showPlayers(gc,gb,brect,highlight,highlightAll,targets);
    	break;

    case ShowAPCards:
    case ShowCards:
    case ShowCardBacks:
    case ShowPlants:
    case ShowUproots:
    case ShowHarvestsAndUproots:
    case ShowSwitches:
    	showCards(gc,gb,brect,highlight,highlightAll,targets,ui);
    	break;
    	
    case ShowStars:
    	if(autoZoom) { setBoardMagnifiers(starRect); }
    	break;
    case ShowWakeup:
    	if(autoZoom) { setBoardMagnifiers(wakeupRect); }
    	break;
    }}
   
   boolean autoZoom = true;
   Rectangle currentZoomZone = null;
   double zoomscale;
   int zoomleft;
   int zoomtop;
   
   Rectangle zoomScale(Rectangle r)
   {	if(currentZoomZone==null) { return r; }
   		else {
		double neww = (G.Width(r)*zoomscale);
		double newh = (G.Height(r)*zoomscale);
		Rectangle newbr = new Rectangle(
				(int)((G.Left(r)-zoomleft)),
				(int)((G.Top(r)-zoomtop)),
				(int)neww,
				(int)newh);
		return newbr;
   		}
   }
    // draw the board zoomed to bounds[]
//    private void drawZoomedBoardElements(Graphics gc, EuphoriaBoard gb, Rectangle brect, HitPoint highlight,HitPoint highlightAll,
//    		Hashtable<EuphoriaCell,EuphoriaMovespec>sources,Hashtable<EuphoriaCell,EuphoriaMovespec>dests,double bounds[],HitPoint tip)
   public void drawZoomedBoardElements(Graphics gc, ViticultureBoard gb, Rectangle brect, 
			HitPoint highlight,HitPoint highlightAll,Hashtable<ViticultureCell,Viticulturemovespec>targets,
			Rectangle zoom)
    {	//top= 0.0; bottom=1.0; left=0.5 ;right=1.0;
       UI ui = gb.resetState.ui;
       PlayerBoard showcards = showPlayerCards();
       if(showcards!=null) { ui=UI.ShowAPCards; }
       if(ui==UI.ScoreSheet && !scoreRect.isOnNow()) { ui=UI.Main; }
       if((showcards!=null) || showBigStack || (zoom==null)) 
       	{ drawBoardElements(gc,gb,brect,highlight,highlightAll,targets,ui); 
       	}
       else
    	{
    	int boardX = G.Left(boardRect);
    	int boardY = G.Top(boardRect);
    	int boardW = G.Width(boardRect);
    	int boardH = G.Height(boardRect);
    	zoomleft = G.Left(zoom)-boardX;
    	zoomtop = G.Top(zoom)-boardY;
    	double zleft = (double)(zoomleft)/boardW;
    	double ztop = (double)(zoomtop)/boardH;
    	double right = (double)(G.Right(zoom)-boardX)/boardW;
    	double bottom = (double)(G.Bottom(zoom)-boardY)/boardH;
		double boardRatio = (double)boardW/boardH;
		int viewW = G.Width(brect);
		int viewH = G.Height(brect);
		double viewRatio = (double)viewW/viewH;
		int viewL = G.Left(brect);
		int viewT = G.Top(brect);
		Rectangle dispR;
		// 
		// this is extra tricky because the board aspect ratio is sacrosanct.  All the objects
		// on the board are relative to the full board size, and the shape of the display rectangle
		// has to match the shape of the board image.  The zoom views are not constrained to this
		// so the first determination is if the zoom area will be limited by the height or the width
		if(viewRatio > boardRatio)
		{	// dominated by view height
			int vieww = (int)(viewH*boardRatio);
			dispR = new Rectangle(viewL+(viewW-vieww)/2 , viewT, vieww, viewH);
			zoomscale = (viewH/(boardH*(bottom-ztop)));
		}
		else
		{	// dominated by view width
			int viewh = (int)(G.Width(brect)/boardRatio);
			dispR = new Rectangle(viewL, viewT+(viewH-viewh)/2 , viewW, viewh);
			zoomscale = viewW/(boardW*(right-zleft));
		}
		double neww = (boardW*zoomscale);
		double newh = (boardH*zoomscale);
    	Rectangle newBR = new Rectangle(boardX-(int)(zleft*neww),boardY-(int)(ztop*newh),(int)neww,(int)newh);
     	if(gc!=null)
    	{	Rectangle oldClip = GC.combinedClip(gc,brect);
    		GC.unsetRotatedContext(gc,highlightAll);
    		int xdis = G.Left(dispR)-boardX;
    		int ydis = G.Top(dispR)-boardY;
     		GC.translate(gc,xdis,ydis);
    		drawFixedElements(gc,gb,newBR);
    		GC.translate(gc,-xdis,-ydis);
    		GC.setRotatedContext(gc,boardRect,highlightAll,contextRotation);
    		GC.setClip(gc,oldClip);
    	}
    	int hx = G.Left(highlightAll);
    	int hy = G.Top(highlightAll);
     	try {
    		CELLSIZE = (int)(STANDARD_CELLSIZE*zoomscale);
    		gb.SetDisplayRectangle(newBR);
    	    int w = G.Width(newBR);
    	    int h = G.Height(newBR);
    	    int x = G.Left(newBR);
    	    int y = G.Top(newBR);

        	double xpos = zleft+(double)(hx-G.Left(brect))/w;
        	double ypos = ztop+(double)(hy-G.Top(brect))/h;
        	
        	
        	G.SetLeft(highlightAll,(int)(x+xpos*w));		// adjust the mouse coordinates
    		G.SetTop(highlightAll,(int)(y+ypos*h));
    		Rectangle oldClip = GC.combinedClip(gc,brect);
     	   	drawBoardElements(gc,gb,newBR,highlight,highlightAll,targets,ui);
     	   	GC.setClip(gc,oldClip);
     	   	//GC.frameRect(gc,Color.red,brect);
       	   	//G.frameRect(gc,Color.red,brect);
      	   	//G.frameRect(gc,Color.green,dispR);
      	}
    	finally 
    		{ CELLSIZE = STANDARD_CELLSIZE;
    	  	  gb.SetDisplayRectangle(boardRect);
    	  	  G.SetLeft(highlightAll,hx);
    	  	  G.SetTop(highlightAll, hy);
       		}
    	}
    }
   private String targetPlayerId(ViticultureBoard gb)
   {	int target = gb.targetPlayer;
	   return(gb.getPlayerBoard(target).getRooster().colorPlusName()+" "+prettyName(target));
   }
    private String gameStateMessage(ViticultureBoard gb,ViticultureState state)
    {	
   		PlayerBoard pb = gb.getCurrentPlayerBoard();
   	  	if((gb.resetState==ViticultureState.ResolveCard) && (gb.cardBeingResolved==ViticultureChip.GovernersChoice))
    	{
    		// very specific logic for governor
    		String message = pb.hasCard(ChipType.YellowCard)
    					? GovernorYellowDesciption
    					: GovernorLongDescription;
    		// add rooster name
    		return(s.get(message,targetPlayerId(gb)));   	
    	}
    	String message = state.description();
    	switch(state)
    	{
    	case BuildAtDiscount2forVP:
    		if(gb.targetPlayer==gb.whoseTurn) { message =  YouMayBuildMinus2; }
    		break;
    	case Plant1AndGive2:
    		if(gb.targetPlayer==gb.whoseTurn) { message = YouMayPlantOne; }
    		break;
    	case TrainWorkerDiscount3:
    	case ResolveCard:
 
    		{ ViticultureChip card =  gb.cardBeingResolved;
    		  boolean confirm = (gb.getState()==ViticultureState.Confirm);
    		  String des = card.description;
    		  if(confirm)
    		  {	if(gb.choiceA.isSelected() || (state==ViticultureState.TrainWorkerDiscount3))
    		  	{
    			  if(card==ViticultureChip.GrandeMotivation)
      		  		{
    				  des = (gb.whoseTurn==gb.targetPlayer) ? RetrieveGrandeSelfDescription : RetrieveGrandeYesDescription;
      		  		}
    			  if(card==ViticultureChip.TrainWorker)
    			  {
    				  des =  (gb.whoseTurn==gb.targetPlayer) ? TrainWorkerSelfDescription : TrainWorkerYesDescription;
    			  }
    		  	}
    		  else {
    			  des = s.get(DeclineChoiceDescription); 
    		  }}
    		  else
    		  if(gb.whoseTurn==gb.targetPlayer)
    		  {
    		  if(card==ViticultureChip.GrandeMotivation)
    		  	{
    			 des = RetrieveGrandeSelfDescription;
    		  	}
    		  if(card==ViticultureChip.TrainWorker)
    		  {
    			  des = TrainWorkerSelfDescription;
    		  }}
     		  if(des!=null && !des.equals(card.cardName)) { message = des; }
    		}
    		break;
    	case FullPass:
    		return(s.get(NextSeasonMessage,s.get(NextSeasons[gb.season(pb)])));
    	case Confirm:
    		return(s.get(state.description,gameStateMessage(gb,gb.resetState)));
    	default: break;
    	}
    	
    	if(pb.hasHarvestMachine() && state.activity==Activity.Harvesting)
    		{
    			if(state.altDescription!=null)
    			{
    				message = state.altDescription;
    			}
    		}

    	if(message.indexOf("#1")>=0)
    	{
    	switch(state)
    	{
    	case ResolveCard:
    	case GiveYellow:
    	case Give2orVP:
    	case Make2Draw2:
    	case Plant1AndGive2:
    	case TrainWorkerDiscount3:
    	case BuildAtDiscount2forVP:
    		return(s.get(message,targetPlayerId(gb)));
    	default: 
    		if(G.debug()) {G.Error("State message %s not handled",state); }
    		return(message);
    	}
    	}
    	else {
    	return s.get(message);
    	}
    }

    public String simpleGameOverMessage(BoardProtocol gb)
    {	if(UsingAutoma())
    	{
    	boolean win = WinForPlayer(players[0]);
    	boolean awin = WinForAutoma();
    	if(win) { return(s.get(WonOutcome,prettyName(0))); }
    	if(awin) { return(s.get(WonOutcome,Bot.Automa.name)); }
    	return(s.get(DrawOutcome));
    	}
    	else
    	{ return (super.simpleGameOverMessage(gb));
    	}
    }
    public void drawFixedElements(Graphics gc,boolean complete)
    {
    	complete |= (backgroundDigest(disB(gc))!=backgroundDigest)
    				|| pendingFullRepaint;
    	super.drawFixedElements(gc,complete);
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
    {  if(!ready) { return; }
       ViticultureBoard gb = disB(gc);
       ViticultureState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
       Hashtable <ViticultureCell,Viticulturemovespec>targets = gb.getTargets();
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
       
       gameLog.playerIcons = true;
       gameLog.redrawGameLog2(gc, nonDragSelect, logRect, Color.black,
    		   boardBackgroundColor, standardBoldFont(),standardBoldFont());
       
       GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);

       drawZoomedBoardElements(gc, gb, boardRect, ourTurnSelect,selectPos,targets,currentZoomZone);
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       if((state != ViticultureState.Puzzle) && !showBigStack && (state!=ViticultureState.Gameover))
		{
		handleEditButton(gc,messageRotation,editRect, buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
		}
  
       GC.unsetRotatedContext(gc,selectPos);

       boolean planned = plannedSeating();
       for(int i=0;i<gb.nPlayers();i++)
       	{  commonPlayer pla = getPlayerOrTemp(i);
       	   pla.setRotatedContext(gc, selectPos,false);
    	   drawPlayerBoard(gc, playerBoardRects[i],gb,playerSideRects[i],chipRects[i],i, ourTurnSelect,selectPos,targets);
    	   if(planned && gb.whoseTurn==i)
    	   {
    		   doneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null));
    	   }
       	   pla.setRotatedContext(gc, selectPos,true);
       	}

       // draw the board control buttons 

		if (state != ViticultureState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			boolean emergency = targets.size()==0 && (state!=ViticultureState.Gameover);
			if(!planned)
				{doneButton(gc,doneRect,(emergency || (gb.DoneState()) ? buttonSelect : null));		
				}
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==ViticultureState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        Rectangle rect = stateRect;
        switch(G.rotationQuarterTurns(pl.displayRotation))
        {
        	case 1:
        	case 3: rect = rotatedStateRect;
        		break;
        	default: break;
        }
        hintRect.draw(gc,selectPos);
        if(state==ViticultureState.Gameover) { scoreRect.draw(gc,selectPos); }
        
        // draw player card racks on hidden boards.  Draw here first so the animations will target
        // the main screen location which is drawn next.
        drawHiddenWindows(gc, selectPos);	
        
      	String stateMessage = state==ViticultureState.Gameover
				?gameOverMessage(gb)
				:gameStateMessage(gb,state);
      	Text stateText = TextChunk.colorize(stateMessage, null, gameMoveText());

        standardGameMessage(gc,pl.displayRotation,Color.black,
            				stateText,
            				state!=ViticultureState.Puzzle,
            				gb.whoseTurn,
            				rect);
        if(state.ui==UI.Main)
        {
        goalAndProgressMessage(gc,selectPos,Color.black,s.get(ViticultureVictoryCondition),progressRect, goalRect);
        }
        int stateH = G.Height(stateRect);
        gb.getCurrentPlayerBoard().getRooster().drawChip(gc, this, stateH*2,G.Left(stateRect)-stateH,G.centerY(stateRect),null);
       
        // draw the vcr controls, last so the pop-up version will be above everything else
        redrawChat(gc,selectPos);
        drawVcrGroup(nonDragSelect, gc);
        GC.setFont(gc, standardPlainFont());
        lateTips.draw(gc);
       // if(hintRect.isOnNow())
       // {
       // 	ViticultureChip.showGrid(gc,this,boardRect);
       // }
    }
    
    double textIconScale[] = new double[] {1,1.3,-0.2,-0.2};
    // used to draw the player icons in the game log
    public Drawable getPlayerIcon(int n)
    {	playerTextIconScale = textIconScale;
    	return mainBoard.getPlayerBoard(n).getScoreMarker();
    }
	public ViticultureBoard getActiveBoard()
	{
		ViticultureBoard b = disB();
		if(b==null) { return(mainBoard); }
		else { return(b); }
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
    {	ViticultureBoard gb = getActiveBoard();
    	 // record some state so the game log will look pretty
    
        handleExecute(gb,mm,replay);
        
         /**
         * animations are handled by a simple protocol between the board and viewer.
         * when stones are moved around on the board, it pushes the source and destination
         * cells onto the animationStck.  startBoardAnimations converts those points into
         * animation sprites.  drawBoardElements arranges for the destination stones, which
         * are already in place, to disappear until the animation finishes.  The actual drawing
         * is done by drawSprites at the end of redrawBoard
         */
        startBoardAnimations(replay,gb.animationStack,CELLSIZE,MovementStyle.Simultaneous);
        
		lastDropped = gb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay.animate) { playSounds((Viticulturemovespec)mm); }
       return (true);
    }
     
 	// return the dynamically correct size.  this can change when the zoom level changes 
 	public int activeAnimationSize(Drawable chip,int thissize) 
 	{ 	 
 		if(currentZoomZone!=null)
 		{	if((chip instanceof ViticultureChip) && ((ViticultureChip)chip).type.isCard()) { return(thissize); }
 			return ((int)(thissize*((double)G.Width(boardRect)/G.Width(currentZoomZone))));
 		}
 		return(thissize);
 	}

 public void drawAnimation(Drawable im,int siz,int w,int h)
 {
	 
 }
 void playSounds(Viticulturemovespec mm)
 {
	 boolean flush = false;
	 switch(mm.op)
	 {
	 case MOVE_SELECT:
		 mm.setLineBreak(true);	// this helps the formatting of the game log
		 break;
	 case MOVE_DROPB:
	 case MOVE_PICKB:
	 case MOVE_PICK:
	 case MOVE_DROP:
		 playASoundClip(light_drop,100);
		 break;
	 case MOVE_MAKEWINE:
	 	{	if(mm.gameEvents!=null)
	 		{
	 		// the game events associated with makewine are "discard" which is translatable,
	 		// and +vp or +$  which are not, so process by elimination
	 		for(String e : mm.gameEvents)
	 		{
	 		if((e.indexOf("Penthouse")<0)
	 			&& (e.indexOf(" $1")<0))// patio logs +money for making wine
	 		{
	 			flush = true;
	 		}
	 		}}
	 	}
	 	break;
	 case MOVE_DONE:
		 if(mm.gameEvents!=null)
		 {
			 for(String e : mm.gameEvents)
			 {	
				 flush |= e.indexOf("GrapeDiscard")>=0;
			 }
		 }

		 break;
	 default: break;
	 }
	 if(flush)
	 {
		 playASoundClip(drainSound,500);
	 }
	 if(mainBoard.reshuffled())
	 {	G.print("Play "+CARD_SHUFFLE);
		 playASoundClip(CARD_SHUFFLE,500);
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
        return (new Viticulturemovespec(st, pl));
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
    	  boolean oknone = true;
    	  switch(nmove.op)
    	  {
    	  case MOVE_SELECT:
	    	  	{
	    	  	// ddyer 4/4/2020
	    	  	// this is special for the "build or give tour" UI.  The problem is that the
	    	  	// give tour action is coded as a "select" were all the other choices are
	    	  	// coded as "build".  If you selected a build choice and switched to the
	    	  	// select choice, or vice versa, no item got popped from the history here, 
	    	  	// and later in the edit the current action got discarded instead.  The
	    	  	// net result was you saw what you intended going forward, but if you 
	    	  	// look back over the game or replay a completed game, the actions taken
	    	  	// are different.
	      	  	int idx = History.size()-1;
	      		commonMove pm = History.elementAt(idx);
	      		int pmop = pm.op;
	      		if(pmop==MOVE_BUILD) { popHistoryElement(); }
	      	  	}
    		  break;
    	  case MOVE_TRAIN:
    	  	{
	      	  	int idx = History.size()-1;
	      		commonMove pm = History.elementAt(idx);
	      		int pmop = pm.op;
	      		if(pmop==MOVE_TRAIN) { popHistoryElement(); }

    	  	}
    	  	break;
    	  case MOVE_BUILDCARD:
    	  case MOVE_BUILD:
    	  case MOVE_SELECTWAKEUP:
    	  case MOVE_NEWWAKEUP:
    	  case MOVE_PLACE_STAR:
     	  	{
    	  	int idx = History.size()-1;
    		commonMove pm = History.elementAt(idx);
    		if((pm.op==nmove.op)
    			|| ((pm.op==MOVE_SELECT)&&(nmove.op==MOVE_BUILD)))
    			{ popHistoryElement(); }
    	  	}
    	  	break;
    	  default: break;
    	  }
    	  commonMove rval = EditHistory(nmove,oknone);
     	     
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
//  public void verifyGameRecord()
//{	//DISABLE_VERIFY = true;
//   	super.verifyGameRecord();
// }
    
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
    }
    public void setOverlayClosed(ViticultureBoard gb,boolean v)
    {
    	overlayClosed = v;
    	if(overlayClosed) { overlayClosedFor = gb.whoseTurn; }
    	else { overlayClosedFor = -1; }
    }
    private void doPickDrop(HitPoint hp,ViticultureBoard gb)
    {	ViticultureId hitCode = (ViticultureId)hp.hitCode;
    	ViticultureCell hitObject = hitCell(hp);
    	ViticultureState state = gb.getState();
    	if (state == ViticultureState.Puzzle)
    	{
    		if(gb.pickedObject==null)
       		{
       			if(hitObject.topChip()!=null)  { PerformAndTransmit("Pick "+hitCode.name()+" "+hitObject.col+" "+hitObject.row+" "+hp.hit_index); }
       		}
       		else
       		{
       			PerformAndTransmit("Drop "+hitCode.name()+" "+hitObject.col+" "+hitObject.row);
       		}
  
    	}
    	else G.Error("not expecting hit %s in state %s",hitCode,state);
    }
    // you might get here when confused and clicking randomly trying
    // to figure out the state.  So do various things to undo special
    // viewing states and return the interface to "standard" mode.
    public void leaveLockedReviewMode()
    {
    	super.leaveLockedReviewMode();	// this undoes review
    	
    	setOverlayClosed(getActiveBoard(),false);	// re-open closed overlays (green checkbox)
    	
    	if(touchPlayer!=null)	// undo "show cards"
    		{ touchPlayer.showCards = false; touchPlayer = null; 
    		}
    	// undo special blown up card or cards
    	changeBigStack((ViticultureChip)null);
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
        ViticultureBoard gb = getActiveBoard();
     	PlayerBoard pb = gb.getCurrentPlayerBoard();
        int hi = remoteWindowIndex(hp);
        PlayerBoard hiddenPb = hi<0 ? null : gb.pbs[hi];
        if(id==GameId.HitDoneButton)
       	{
        	switch(gb.resetState)
        	{
        	case SwitchVines:
        	{
        		Viticulturemovespec swap = gb.findSwitchMove(pb);
        		if(swap!=null)
    			{
        			PerformAndTransmit(swap.moveString());
    			}
    		}
        	break;
        	default: 
        		if(gb.resetState.ui==UI.ShowWines)
        		{	// turn done into makewine
        			id = ViticultureId.MakeWine;
        		}
        		break;
       	}}
        
       	if(!(id instanceof ViticultureId)) 
       	{   // only do standard actions (like reset) if it is our turn
       		if(hiddenPlayerOrMainMove(findHiddenWindow(hp)))
       			{ 
       			missedOneClick = performStandardActions(hp,missedOneClick);   
       			showBuildings = false;     
       			showOptions = false;
       			} 
       	}
        else {
        missedOneClick = false;
        ViticultureId hitCode = (ViticultureId)id;
        ViticultureChip hitChip = (hp.hitObject instanceof ViticultureChip) ? (ViticultureChip)hp.hitObject : null;
        ViticultureCell hitObject = hitCell(hp);
        ViticultureState state = gb.getState();
        ViticultureState resetState = gb.resetState;
       	
        switch (hitCode)
        {
        default:
        	doPickDrop(hp,gb);
         	break;
        case SetReady:
        	{
        	PlayerBoard ap = (PlayerBoard)hp.hitObject;
        	PerformAndTransmit((simultaneousTurnsAllowed() ? "Ready " : "Ok ")+(char)('A'+ap.boardIndex)+" "+((hp.hit_index==0)?false:true));
        	}
        	break;
        case SetOption:
        	{
        	Option op = (Option)hp.hitObject;
         	commonPlayer ap = getActivePlayer();
        	PerformAndTransmit((simultaneousTurnsAllowed() ? "Option " : "SetOption ")+(char)('A'+ap.boardIndex)+" "+op.name()+" "+((hp.hit_index==0)?false:true));
        	}
        	break;
        case ShowOptions:
        	showOptions = !showOptions;
        	break;
        case ShowBuildings:
        	showBuildings = !showBuildings;
        	break;
        case CloseOverlay:
        	showOptions = false;
        	setOverlayClosed(gb,!overlayClosed);
        	break;
        case ShowScores: scoreRect.toggle();
        	break;
        case ShowHints: hintRect.toggle();    
        	break;
        case ShowPlayerBoard:
        	if(hiddenPb!=null)
        	{
        	hiddenPb.showPlayerBoard = !hiddenPb.showPlayerBoard;
        	if(hiddenPb.showPlayerBoard) { hiddenPb.hiddenCardsOnTop = false; }
        	}
        	break;
        case ShowHidden:
        	if(hiddenPb!=null)
        	{
        	hiddenPb.hiddenCardsOnTop = !hiddenPb.hiddenCardsOnTop; 
        	if(hiddenPb.hiddenCardsOnTop) { hiddenPb.showPlayerBoard = false; } 
        	hiddenPb.showHiddenBigStack = false;
        	}
        	break;
        case OverValue:
        	{
        	G.infoBox("",CantPlantMessage);
        	}
        	break;
        case ShowBigStack:
        	{
        	ViticultureCell from = hitCell(hp);
        	changeBigStack(from);
        	}
        	break;
        case ShowBigChip:
        	{
        	ViticultureChip newchip = (ViticultureChip)hp.hitObject;
        	changeBigStack(newchip);
        	break;
        	}
        case Magnify:
        	{
        	ViticultureChip newChip = (ViticultureChip)hp.hitObject;
        	if(hiddenPb!=null) {
        		if(hiddenPb.showHiddenBigStack) { hiddenPb.showHiddenBigStack = false; }
        		else { 	hiddenPb.showHiddenBigStack = true; 
        				hiddenPb.hiddenBigStack.reInit();
        				hiddenPb.hiddenBigStack.addChip(newChip);   		
        			}
        		}       	
        		else 
        		{	changeBigStack(newChip);
        	}}
        	break;
        case SpecialWorkerCards:
        	{
        	ViticultureChip newChip = hitObject.chipAtIndex(hp.hit_index);
        	if(hiddenPb!=null) {
        		if(pb.showHiddenBigStack) { pb.showHiddenBigStack = false; }
        		else { 
        			pb.showHiddenBigStack = true;
        			pb.hiddenBigStack.reInit();
        			pb.hiddenBigStack.addChip(newChip);
        		}
        	}
        	else 
        	{ changeBigStack(newChip);
        	}}
        	break;
        case CancelBigChip:
        	showBuildings = false;
        	if(hiddenPb!=null) { hiddenPb.showHiddenBigStack = false; }
        	else { changeBigStack((ViticultureChip)null);
        	}
        	break;
        case HarvestMode:
        	// switching back to harvest
         	uprootMode = false;
         	PerformAndTransmit("Unselect");
        	break;
        case PlantMode:	
        	uprootMode = false;
        	break;
        case UprootMode: 
        	uprootMode = true; 
        	PerformAndTransmit("Unselect");
        	break;
        case MakeWine:
        	{
        		commonMove m = (commonMove)hp.hitObject;
        		while(m!=null)
        		{	
        			PerformAndTransmit(m.moveString());
        			
        			m = m.next;
        		}
        		PerformAndTransmit("Done");
        	}
        	break;
        case WhiteGrape:	/// targets in showTrades
        case WhiteGrapeDisplay:
        case RedGrape:
        case RedGrapeDisplay:
        case RoosterDisplay:
        case VP:	// targets in showTrades
        case Cards:
        case Cash:
         	if(state==ViticultureState.Puzzle) { doPickDrop(hp,gb); } 
        	else { PerformAndTransmit("Select "+hitCode.name()+" "+hitObject.col+" "+hitObject.row+" "+hp.hit_index); }
        	break;
        case WineDisplay:
        	ViticultureChip top = hitObject.topChip();
        	PerformAndTransmit("Discard "+top.type.name()+" "+pb.colCode+" "+(top.order-1)+" 0");
        	break;
        case Eye:
        	{
        	if(hitObject==null)
        	{
        	if(hiddenPb!=null) { hiddenPb.hiddenCensoring = !hiddenPb.hiddenCensoring; }
        	else { pb.publicCensoring = !pb.publicCensoring; }
        	}
        	else if(touchPlayer!=null)
        	{	touchPlayer.showCards = false;
        		touchPlayer = null;
        	}
        	else
        	{
        	PlayerBoard eye = gb.getPlayerBoard(hitObject.col-'A');
        	eye.publicCensoring = true;
        	eye.showCards = !eye.showCards;
           	touchPlayer = eye.showCards ? eye : null;
           	changeBigStack((ViticultureChip)null);
        	}}
        	break;
        case Choice_0:
        case Choice_1:
        case MamaCards:
        	// hit the fields
        	if(hitObject.rackLocation()==ViticultureId.Field)
        	{
        	PerformAndTransmit("Select "+hitObject.rackLocation().name()+" "+hitObject.col+" "+hitObject.row+" "+hp.hit_index);
        	}
        	else
        	{
       		PerformAndTransmit("Select "+hitObject.rackLocation().name()+" @ 0 "+hp.hit_index);
        	}
        	break;
        case CardDisplay:	// target in showTrades

       // case Choice_0:
        	// hit cards in the card selector UI
    		{
    		
        	ViticultureState dstate = resetState;
        	if(dstate.isBuilding()) { dstate = ViticultureState.BuildStructure; }	// consolidate
        	switch(dstate)
        	{
        	case Trade1:
        	case Trade2:
        	case TradeSecond:
        		PerformAndTransmit("Select "+hitCode.name()+" "+hitObject.col+" "+hitObject.row+" "+hp.hit_index);
        		break;
        	case BuildStructure:	// consolidated from all possible building states
        	case PlaySecondBlue:
        	case Play2Blue:
        	case Play1Blue:
        	case PlayBlueDollar:
        	case Play1Yellow:
        	case PlaySecondYellow:
        	case Play2Yellow:
        	case PlayYellowDollar:
        		
        	case Plant1Vine:
        	case Plant2Vines:
        	case PlantSecondVine:
        	case Plant2VinesOptional:
        	case Plant1For2VPVolume:
        	case Plant1For2VPDiversity:
        	case Plant1VineOptional:
        	case Plant1AndGive2:
        	case Plant1VineNoLimit:
        	case Plant1VineNoStructures:
        	case PlantVine4ForVP:
        		
        	case FillWineBonus:
        	case FillWineBonusOptional:
        	case FillWineFor2VPMore:
        	case FillWine:
        	case FillMercado:
        	case FillWineOptional:
        	case GiveYellow:
        		{
         		PerformAndTransmit("Select "+ViticultureId.Cards.name()+" "+pb.colCode+" 0 "+hp.hit_index);
        		}	
        		break;
           	case DestroyStructure:
           	case DestroyStructureOptional:
           		{
           		boolean some = false;
           		for(ViticultureCell c : pb.buildStructureCells) 
           		{	int index = c.height()-1;
           			if(index>=0)
           				{ViticultureChip ch = c.chipAtIndex(index);
           				 // if the top is a worker, dig under it for the struture
           				 if(ch.type.isWorker()) { index--; ch = c.chipAtIndex(index); }
           				 if(ch==hitChip)
           				 {	some = true;
           				 PerformAndTransmit("discard "+c.rackLocation()+" "+c.col+" "+c.row+" "+index);
           				 break;
           				 }}
           			}
           			G.Assert(some, "structure to destroy not found");
           		}
           		break;
 
        	case DiscardCards:
        	case Discard1ForOracle:
        	case DiscardGreen:
        	case Discard2Green:
        	case Discard4CardsFor3:
        	case Discard2CardsFor2VP:
        	case Discard3CardsAnd1WineFor3VP:
        	case Discard2CardsFor1VP:
        	case Discard2CardsForAll:
        	case Discard2CardsFor4:
        		{
         		PerformAndTransmit("discard "+ViticultureId.Cards.name()+" "+pb.colCode+" 0 "+hp.hit_index);
        		}
        		break;
        	case SelectPandM:
       			{
        		PerformAndTransmit("Select "+pb.cards.rackLocation().name()+" @ 0 "+hp.hit_index);     		
       			}
       			break;
       		
           	case Keep1ForOracle:
        	case Keep2ForOracle:
       			{
        		PerformAndTransmit("Select "+pb.cards.rackLocation().name()+" @ 0 "+hp.hit_index);     		
       			}
       			break;
          	case Select1Of1FromMarket:
        	case Select1Of2FromMarket:
        	case Select2Of2FromMarket:
        	case Select2Of3FromMarket:
        		{
            		PerformAndTransmit("Select "+pb.oracleCards.rackLocation().name()+" @ 0 "+hp.hit_index);     		
        		}
        		break;
        	case Pick2TopCards:
        	case Pick2Discards:
          		{
        		ViticultureCell from = gb.getStack(hitChip.type) ;
        		if(resetState==ViticultureState.Pick2Discards) { from = gb.getDiscards(from); }
         		PerformAndTransmit("Select "+from.rackLocation().name()+" @ 0 "+hp.hit_index);
        		}
        		break;
           	case TakeYellowOrBlue:
           	case TakeYellowOrGreen:
        	case Take2Cards:
        	case TakeCard:
        	case SelectCardColor:
     		{
    		ViticultureCell from = gb.getStack(hitChip.type) ;
    		PerformAndTransmit("Select "+from.rackLocation().name()+" @ 0 "+(from.height()-1));
    		}
    		break;

         	case StealVisitorCard:
         		PerformAndTransmit(((Viticulturemovespec)hp.hitObject).moveString());
         		break;
         	case ResolveCard:
         	case ResolveCard_2of3:
         	case ResolveCard_AorBorBoth:
         		/* hack for placements, make the discard button advance to the next card */
         		//bb.cardBeingResolved = ViticultureChip.WinterDeck
         		//	[bb.cardBeingResolved.order];
         		//G.print("Now "+bb.cardBeingResolved.order);
         		PerformAndTransmit("Select "+hitCode.name()+" @ 0 0");
         		break;
         		
        	default: doPickDrop(hp,gb);
        	}}
        	break;
        case Magnifier:
        	{
        	setBoardMagnifiers((Rectangle)hp.hitData);
        	}
        	break;
        case Field:
        	{
        	ViticultureChip ftop = hitObject.topChip();
        	if(state==ViticultureState.Puzzle) { doPickDrop(hp,gb); }
        	else if(!(ftop.type.isWorker()||(ftop.type==ChipType.StructureCard)))
        	{	PerformAndTransmit("Select "+hitCode.name()+" "+hitObject.col+" "+hitObject.row+" "+hp.hit_index);
            	break;
        	}}
			//$FALL-THROUGH$
		case PlayerStructureCard:
        case DestroyStructureWorker:
        case PlayerYokeWorker:	// summer and winter on the player board
        	if(gb.pickedObject==null)
           		{
           			PerformAndTransmit("Pick "+hitCode.name()+" "+hitObject.col+" "+hitObject.row+" "+hp.hit_index);
           		}
           		else
           		{
           			PerformAndTransmit("Drop "+hitCode.name()+" "+hitObject.col+" "+hitObject.row);
           		}
        	break;
        case DollarOrCardWorker:		// anytime
        case PlayBlueWorker:	// winter
        case RecruitWorker:
        case SellWineWorker:
        case FillWineWorker:
        	
        case DrawPurpleWorker:	// fall
        case HarvestWorker:
        case MakeWineWorker:
        case BuildTourWorker:

        case PlayYellowWorker:	// summer
        case PlantWorker:
        case TradeWorker:
        case FlipWorker:
        	
        case GiveTourWorker:	// spring
        case BuildStructureWorker:
        case StarPlacementWorker:
        case DrawGreenWorker:
        	
        if(resetState==ViticultureState.TakeActionPrevious)
        	{
        		PerformAndTransmit("TakeAction "+hitCode.name());
        	}
        else if((resetState==ViticultureState.PlayBonus) 
        		&& gb.hasBonusMoves(hitObject)
        		)
        	{
        		PerformAndTransmit("PlaceBonus "+hitCode.name()+" "+hitObject.row);
        	}
        	else 
        	{	if(gb.pickedObject==null)
	       		{	// this is a rare case, when picking up a piece from the grande meeple
        			// spot, such as from a retrieve worker action
        			String ind = hp.hit_index==0 ? "" : " "+hp.hit_index;
	       			PerformAndTransmit("Pickb "+hitCode.name()+" "+hitObject.row+ind);
	       		}
	       		else
	       		{
	       			PerformAndTransmit("Dropb "+hitCode.name()+" "+hitObject.row);
	       		}
	       	}
        	break;
        // places on the player board
            // places on the player board
        case UnbuiltYoke:
        case UnbuiltTrellis:
        case UnbuiltWaterTower:
        case UnbuiltCottage:
        case UnbuiltMediumCellar:
        case UnbuiltLargeCellar:
        case UnbuiltTastingRoom:
        case UnbuiltWindmill:
        	// building buildings
        	PerformAndTransmit("Build "+hitCode.name().substring(7)+" "+hitObject.col);
        	break;
        
        case WineSelection:	// also player selection
        case WhiteWine:
        case RedWine:
        case RoseWine:
        case Champaign:
        	if(hp.hitObject instanceof Viticulturemovespec)
        	{
        	PerformAndTransmit(((Viticulturemovespec)hp.hitObject).moveString());
        	break;
        	}
			//$FALL-THROUGH$
		case WineBin:	// display bin for wines being made
        case StartPlayer:		// display grape in winemaking mode
 	    	if(gb.pickedObject==null)
	    		{
	    		PerformAndTransmit("Pick "+hitCode.name()+" "
	    				+hitObject.col+" "+hitObject.row+" "+hp.hit_index);
	    		}
	    	else {
	    		PerformAndTransmit("Drop "+hitCode.name()+" "
	    				+hitObject.col+" "+hitObject.row+" "+hp.hit_index);    		
	        	}
    		break;
    	case Vine:	// hit a vine in uproot mode
    		switch(resetState)
    		{
    		case SwitchVines:
    			PerformAndTransmit("Select "+hitObject.rackLocation()+" "+hitObject.col+" "+hitObject.row+" "+hp.hit_index);
    			break;
    		case Puzzle:
       			PerformAndTransmit("Drop "+hitObject.rackLocation()+" "+hitObject.col+" "+hitObject.row+" "+hp.hit_index);
       		    break;		
    		default: 
    		case HarvestOrUproot:
    			PerformAndTransmit("Uproot "+hitObject.rackLocation()+" "+hitObject.col+" "+hitObject.row+" "+hp.hit_index);
    			uprootMode = false;
    			break;
    		}
    		break;
        case Workers:
        	{
        	if(gb.pickedObject==null)
        		{
        		PerformAndTransmit("Pick "+hitCode.name()+" "
        				+hitObject.col+" 0 "+hp.hit_index);
        		}
        	else {
        		PerformAndTransmit("Drop "+hitCode.name()+" "+hitObject.col+" "+hitObject.row);    		
	        	}
        	}
        	break;
        case StarTrack:
        	switch(resetState)
        	{
        	case Move1Star:
        	case Move2Star:
        	case Puzzle:
        		if(gb.pickedObject==null) 
        		{ PerformAndTransmit("Pick "+hitCode.name()+" "+hitObject.col+" "+hitObject.row+" "+hp.hit_index);
        		}
        		else { 
        			PerformAndTransmit("Drop "+hitCode.name()+" "+hitObject.col+" "+hitObject.row);
        		}
        		break;
        	case Place1Star:
        	case Place2Star:
        		if(gb.pickedObject==null)
        		{
        		PerformAndTransmit("PlaceStar "+hitObject.row);
        		}
        		break;
        	default: G.Error("Not expecting %s", resetState);
        	}
        	break;
        case RoosterTrack:
        	switch(resetState)
        	{
        	case PickNewRow:
        		PerformAndTransmit("NewWakeup "+hitObject.col+" "+hitObject.row);
        		break;
        	case SelectWakeup:
        		PerformAndTransmit("SelectWakeup "+hitObject.row);
        		break;
        	default: doPickDrop(hp,gb);
        	}
        	
        	break;
        case BlueDiscards:
        case StructureDiscards:
        case PurpleDiscards:
        case YellowDiscards:
        case GreenDiscards:
        	if(resetState!=ViticultureState.Puzzle) 
        		{ 
        		ViticultureCell c = hitCell(hp);
        		changeBigStack(c.topChip());
        		break;
        		}; 
			//$FALL-THROUGH$
		case YellowCards:
        case GreenCards:
        case BlueCards:
        case StructureCards:
        case PurpleCards:
        case Choice_A:
        case Choice_B:
        case Choice_C:
        case Choice_D:
        case Choice_HarvestFirst:
        case Choice_FillWineFirst:
        case Choice_MakeWineFirst:
        	switch(resetState)
        	{
        	case Sell1VPfor3:
        	case ResolveCard_AorBorBoth:
        	case ResolveCard_2of3:
        	case ResolveCard:
        	case TakeCard:
        	case Give2orVP:
        	case Take2Cards:
        	case TakeYellowOrBlue:
        	case SelectPandM:
        		PerformAndTransmit("Select "+hitCode.name()+" @ 0 "+hp.hit_index);
        		break;
        		
        	default: 
        		if(resetState.isWinemaking()) 
        		{	if(mainBoard.revision>=126)
        			{PerformAndTransmit("select "+hitObject.rackLocation()+" "+hitObject.col+" "+hitObject.row+" "+hitCode.ordinal());
        			}
        			hitObject.cost = hitCode.ordinal();
        		}
        		else { doPickDrop(hp,gb); }
        	}
        	break;
 
        }
        }
    }



    private boolean setDisplayParameters(ViticultureBoard gb,Rectangle r)
    {	
     	gb.SetDisplayRectangle(r);
      	return(false);
    }
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     * <p>
     * if complete is true, we definitely want to start from scratch, otherwise
     * only the known changed elements need to be painted.  Exactly what this means
     * is game specific, but for Kulami the underlying empty board is cached as a deep
     * background, but the chips are painted fresh every time.
     * <p>
     * this used to be very important to optimize, but with faster machines it's
     * less important now.  The main strategy we employ is to paint EVERYTHING
     * into a background bitmap, then display that bitmap to the real screen
     * in one swell foop at the end.
     * 
     * @param gc the graphics object.  If gc is null, don't actually draw but do check for mouse location anyay
     * @param complete if true, always redraw everything
     * @param hp the mouse location.  This should be annotated to indicate what the mouse points to.
     */
  //  public void drawCanvas(Graphics gc, boolean complete,HitPoint hp)
  //  {	
       	//drawFixedElements(gc,complete);	// draw the board into the deep background
   	
    	// draw the board contents and changing elements.
        //redrawBoard(gc,hp);
        //      draw clocks, sprites, and other ephemera
        //drawClocksAndMice(gc, null);
        //DrawArrow(gc,hp);
 //    }
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
    	return(mainBoard.gameType()); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Viticulture_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int np = his.intToken();
    	long rv = his.longToken();
    	int rev = his.intToken();	
    	boolean turn = his.boolToken();
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // long rk = his.longToken();
    	// bb.doInit(token,rk);
        mainBoard.doInit(token,rv,np,rev,turn);
        adjustPlayers(np);

    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);


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
    
   public void ViewerRun(int wait)
      {
          super.ViewerRun(wait);

          if(!reviewOnly 
        	 && !reviewMode() 
        	 && simultaneousTurnsAllowed()
        	 && (mainBoard.getState()==ViticultureState.ChooseOptions)
        	 && (mainBoard.allPlayersReady())
        	 && (allPlayersLocal() ||(mainBoard.whoseTurn == getActivePlayer().boardIndex)))
          {	  
        	  PerformAndTransmit( "ECommence " +mainBoard.options.memberString(Option.values()));
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
    public BoardProtocol getBoard()   {    return (mainBoard);   }

    //** this is used by the game controller to supply entertainment strings to the lobby */
     public String gameProgressString()
     {	// this is what the standard method does
     	 return(mutable_game_record 
     			? Reviewing
     			: vprogressString());
     }
     private String vprogressString()
     {	StringBuilder str = new StringBuilder("x64 ");
     	PlayerBoard pb = mainBoard.getCurrentPlayerBoard();
     	str.append(Base64.encode(s.get(TimeLineDescription,s.get(seasons[mainBoard.season(pb)]),""+mainBoard.year)));
     	for(PlayerBoard p : mainBoard.pbs) {str.append(" "); str.append(Base64.encode(""+p.score)); } 
     	return(str.toString());
     }

    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new ViticulturePlay());
    }
    
    
    private boolean collectingStats = false;
    
    public void doReplayCollection()
    {
    	collectingStats = true;
    	ViticultureStats.allStats.clear();
    	try {
    		super.doReplayCollection();
    	}
    	finally 
    		{ collectingStats = false; 
    		  ViticultureStats.saveStats();
    		}
    }
    public void doReplayFolder()
    {
    	collectingStats = true;
    	ViticultureStats.allStats.clear();
    	try {
    		super.doReplayFolder();
    	}
    	finally 
    		{ collectingStats = false; 
    		  ViticultureStats.saveStats();
    		}
    }
    public void ReplayGame(sgf_game ga)
    {
    	super.ReplayGame(ga);
    	if(collectingStats)
    	{
    		ViticultureStats.collectStats(ga,mainBoard,players);
    	}
    }

     /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/23/2023
		225: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\viticulture\viticulturegames\viticulturegames\archive-2020\games-Apr-25-2020.zip U!VI-mgahagen-epatterson-lmarkus001-Lgahagen-2020-04-12-2352.sgf lib.ErrorX: chip exists
		267: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\viticulture\viticulturegames\viticulturegames\archive-2020\games-Apr-25-2020.zip VI-ddyer-Wilson17-sven2-lfedel-mfeber-idyer-2020-04-19-1907.sgf lib.ErrorX: must be a blue card
		284: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\viticulture\viticulturegames\viticulturegames\archive-2020\games-Apr-25-2020.zip VI-mfeber-ddyer-idyer-sven2-2020-04-12-1916.sgf lib.ErrorX: must be a blue card
		286: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\viticulture\viticulturegames\viticulturegames\archive-2020\games-Apr-25-2020.zip VI-mfeber-Runcible-lfedel-idyer-ddyer-sven2-2020-04-11-0218.sgf lib.ErrorX: must be a blue card
		298: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\viticulture\viticulturegames\viticulturegames\archive-2020\games-Apr-25-2020.zip VI-wilson17-ddyer-sven2-2020-04-15-1937.sgf lib.ErrorX: Not expecting state Play
		1971 files visited 5 problems
		12/2/2025
		2391 files visited 5 problems
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
            {	scoreSummaryPrepared = false;
                mainBoard.doInit(value);
                adjustPlayers(mainBoard.nPlayers());
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

    Text gameMoveText[] = null;  
    private Text[] gameMoveText()
    {  
    	if(gameMoveText==null)
    	{
    	double[] escale = {1,1,0,-0.3};
    	double[] bscale = {2,2,0,-0.2};
    	double[] bbscale = {1,2,0,-0.2};
    	double[] cscale = {1.5,2,0,-0.2};
    	double[] vscale = {1.5,1.4,0,-0.4}; 
    	double[] xscale = {1.5,1.7,0.0,-0.4}; 
    	double[] gscale = {1.5,1.0,0,-0.4}; 
    	TextStack texts = new TextStack();
    	icons(texts,ViticultureChip.Roosters,cscale);
    	icons(texts,ViticultureChip.CardBacks,escale);
    	icons(texts,ViticultureChip.Buildings,bbscale);
    	icons(texts,ViticultureChip.SpecialWorkers,bscale);
    	icons(texts,ViticultureChip.GrapesAndWines,bscale);
    	icons(texts,ViticultureChip.Stars,bscale);
    	texts.push(TextGlyph.create("$5","xx",ViticultureChip.Coin_5,this,xscale));
    	texts.push(TextGlyph.create("$2","xx",ViticultureChip.Coin_2,this,xscale));
    	texts.push(TextGlyph.create("$1","xx",ViticultureChip.Coin_1,this,xscale));
    	texts.push(TextGlyph.create("RedGrape","xxx",ViticultureChip.RedGrape,this,gscale));   	
    	texts.push(TextGlyph.create("WhiteGrape","xxx",ViticultureChip.WhiteGrape,this,gscale));   	
    	texts.push(TextGlyph.create("RedGrapeDiscard","xxx",ViticultureChip.RedGrape,this,gscale));   	
    	texts.push(TextGlyph.create("WhiteGrapeDiscard","xxx",ViticultureChip.WhiteGrape,this,gscale));   	
    	texts.push(TextGlyph.create("1VP","xx",ViticultureChip.VictoryPoint_1,this,vscale));
    	texts.push(TextGlyph.create("2VP","xx",ViticultureChip.VictoryPoint_2,this,vscale));
    	texts.push(TextGlyph.create("3VP","xx",ViticultureChip.VictoryPoint_3,this,vscale));
    	texts.push(TextGlyph.create("Rose Wine","xx",ViticultureChip.RoseWine,this,cscale));
    	texts.push(TextGlyph.create("Champaign", "xx", ViticultureChip.Champagne, this,cscale));
    	texts.push(TextGlyph.create("Wine Order", "xx", ViticultureChip.OrderBack, this,escale));
    	texts.push(TextGlyph.create("Blue card","xx", ViticultureChip.WinterBack, this,escale));
    	texts.push(TextGlyph.create("a blue card,","xx", ViticultureChip.WinterBack, this,escale));
    	texts.push(TextGlyph.create("PurpleCards", "xx", ViticultureChip.OrderBack, this,escale));
    	texts.push(TextGlyph.create("YellowCards", "xx", ViticultureChip.SummerBack, this,escale));
    	texts.push(TextGlyph.create("a Yellow card", "xx", ViticultureChip.SummerBack, this,escale));
    	texts.push(TextGlyph.create("a Yellow", "xx", ViticultureChip.SummerBack, this,escale));
    	texts.push(TextGlyph.create("Green card", "xx", ViticultureChip.VineBack, this,escale));   	
    	texts.push(TextGlyph.create("GreenCards", "xx", ViticultureChip.VineBack, this,escale));
    	texts.push(TextGlyph.create("BlueCards", "xx", ViticultureChip.WinterBack, this,escale));
    	texts.push(TextGlyph.create("StructureCards", "xx", ViticultureChip.StructureBack, this,escale));
    	texts.push(TextGlyph.create("Gray-Worker", "xx", ViticultureChip.GrayMeeple, this,escale));

    	gameMoveText = texts.toArray();
    	}
    	return(gameMoveText);
    }
    private void icons(TextStack stack,ViticultureChip[][]cs,double[]bscale)
    {
    	for(ViticultureChip ch[] : cs) { icons(stack,ch,bscale); }
    }
    
    private void icons(TextStack stack,ViticultureChip[]c,double[]bscale)
    {
       	for(ViticultureChip chip :c)
		{
  		stack.push(TextGlyph.create(chip.colorPlusName(), "xx", chip, this,bscale));
		}

    }
    public Text censoredMoveText(SequenceElement m,int idx,Font f)
    {
    	Text str = ((Viticulturemovespec)m).censoredMoveText(this,mainBoard,f);
    	str.colorize(null,gameMoveText());
    	return(str);
    }
    public Text colorize(String str)
    {
    	return TextChunk.colorize(str,null,gameMoveText());
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
        int HiddenViewHeight = 400;
    	super.adjustPlayers(n);
        if(RpcService.isRpcServer() || VNCService.isVNCServer() || G.debug())
        {
        createHiddenWindows(n,HiddenViewWidth,HiddenViewHeight);
        }
    }
    public void drawEye(Graphics gc,Rectangle eyeR,HitPoint hp,boolean censor,int index)
    {    	
      	StockArt icon = censor ? StockArt.Eye : StockArt.NoEye;
       	if(icon.drawChip(gc, this, eyeR, hp, ViticultureId.ShowHidden))
       	{
       		hp.hit_index = index;
       	}
    }
    /*
     * @see online.game.commonCanvas#drawHiddenWindow(Graphics, lib.HitPoint, online.game.HiddenGameWindow)
   */
  public void drawHiddenWindow(Graphics gc,HitPoint hp,int index,Rectangle bounds)
  { 
    PlayerBoard pb = mainBoard.getPlayerBoard(index);
    if(pb!=null)
    {
  	int left = G.Left(bounds);
  	int top = G.Top(bounds);
  	int width = G.Width(bounds);
  	int height = G.Height(bounds);
  	int fh = FontManager.getFontSize(largeBoldFont());
  	boolean censor = !pb.hiddenCardsOnTop;
  	
  	int stateH = 100;
  	if(remoteViewer<0) { ViticultureChip.backgroundTile.image.stretchImage(gc, bounds);  }
  	
  	int stateX = left+CELLSIZE+stateH;
     	
  	int calcW = width/5;
  	int calcX = left+width-calcW;
 	int lineH = stateH/2;
  	ViticultureState uistate = mainBoard.resetState;
  	Rectangle stateR =new Rectangle(stateX,top,calcX-stateX,lineH); 				// game state   
  	Rectangle nameR = new Rectangle(left,top,width/3,fh);
  	Rectangle eyeR = new Rectangle(left,top+lineH,lineH,lineH);	// see hidden
	Rectangle roosterR = new Rectangle(left,top+fh/2,lineH,lineH);
	Rectangle doneR = new Rectangle(left,top+height-lineH,lineH*2,lineH);
	int coinX = left+lineH/2;
	int coinY = top+lineH*3;
	int boardY = coinY+lineH*3/2;
	int boardX = left+lineH*3/4;
  	Hashtable <ViticultureCell,Viticulturemovespec>targets = mainBoard.getTargets();
  	boolean ourTurn =  index == mainBoard.whoseTurn;
  	UI ui = uistate.ui;
  	boolean overlayUI = false;
  	switch(ui)
  	{	case ShowCard:
  		case Main:
  		case ShowWakeup:
  		case ShowStars: 
  		default: 
 			break;
  		//	overlayUI=true;
  		//	break;
  	}
  	boolean overlay = (ourTurn && overlayUI && censor) || pb.showHiddenBigStack;
 	if(pb.showPlayerBoard)
 	{	Rectangle br = playerBoardRects[index];
 		double ratio = (double)G.Width(br)/G.Height(br);
 		int aw = width-lineH*2;
 		int ah = (int)(aw/ratio);
 		if(ah>height-lineH) { ah = height-lineH; aw = (int)(ah*ratio); }
 		Rectangle br2 = new Rectangle(left+lineH*2,top+lineH,aw,ah);
 		drawPlayerBoard(gc,br2,mainBoard,null,null,index,null,hp,targets);
 	}
 	else if(overlay)
  	{
    showOverlay(ui,gc,bounds,
    		mainBoard,true,pb.hiddenBigStack,hp,hp,targets);
	if( ourTurn && mainBoard.DoneState())
		{
		GC.setFont(gc, largeBoldFont());
		doneButton(gc,doneR, hp);
		}
  	}
  	else
  	{
  		ViticultureCell cards = pb.hiddenCardDisplay1;
  		cards.copyFrom(pb.cards);
  		cards.selected = false;
  		int nCards = cards.height();
  		int size = width/Math.max(4,(nCards+1));
  		int ypos = top+height/2;
  		String cardId = censor ? ViticultureChip.BACK : null;
  		if(nCards>6)
  		{
  			ViticultureCell card2 = pb.hiddenCardDisplay2;
  			int lim = (nCards+1)/2;
  			card2.reInit();
  			for(int i=lim;i<nCards;i++)
  				{ ViticultureChip card = cards.removeChipAtIndex(lim);
  				  card2.insertChipAtIndex(0,card); 
  				}
  			size = Math.min((int)(height*0.25),width/(lim+1));
  			ypos -= size/2;
  			if(card2.drawStack(gc,this,hp,size,left+size*5/4,ypos+(int)(size*1.6),0,1,0,cardId))
  			{
  				hp.hitCode = ViticultureId.Magnify;
  				hp.hitObject = card2.chipAtIndex(hp.hit_index);
  				hp.arrow = StockArt.Eye;
  				hp.awidth = size/4;
  			}
  		}
  		if(cards.drawStack(gc, this, hp, size, left+size*5/4, ypos, 0, 1, 0, cardId))
  		{
			hp.hitCode = ViticultureId.Magnify;
			hp.hitObject = cards.chipAtIndex(hp.hit_index);
			hp.arrow = StockArt.Eye;
			hp.awidth = size/4;			
  		}
  	}
      // draw the game state
  	drawEye(gc,eyeR,hp,censor,index);
  	pb.getRooster().drawChip(gc, this, roosterR,null);
  	loadCoins(pb.cashDisplay,pb.cash);
  	labelFont = largeBoldFont();
  	pb.cashDisplay.drawStack(gc,this,null,lineH*3/2,coinX,coinY,0,0.3,0,""+pb.cash);
  	ViticultureChip.playermat.drawChip(gc, this,lineH*4/3, boardX, boardY, hp,ViticultureId.ShowPlayerBoard,null);
  	String stateMessage = (uistate==ViticultureState.Gameover)?gameOverMessage(mainBoard):gameStateMessage(mainBoard,uistate);
  	Text stateText = TextChunk.colorize(stateMessage, s, gameMoveText());
 	standardGameMessage(gc,0,Color.black,
 			stateText,
  					(uistate!=ViticultureState.Puzzle)&&!simultaneousTurnsAllowed(),
  					mainBoard.whoseTurn,
  					stateR);
  	String name = prettyName(index);
  	GC.setFont(gc,largeBoldFont());
  	GC.Text(gc, false, nameR, Color.black,null,name);
    }
  }
  private int lastMoveNumber = -1;
  public boolean playerChanging()
  {
  	commonPlayer who = currentGuiPlayer();
  	commonMove top = History.top();
  	int currentMove = lastMoveNumber;
  	lastMoveNumber = getBoard().moveNumber() ;
  	return((mainBoard.players_in_game>1) 
  			&& (allPlayersLocal()|| (who==getActivePlayer()))
  			&& (mainBoard.turnChangeSamePlayer ||
  					((top.op==MOVE_DONE)	
  					  || (who==null)
  					  || (currentMove!=lastMoveNumber)
  					  || (who.boardIndex!=top.player))));
  }

  public double imageSize(ImageStack im)
  {
	  return(super.imageSize(im) + ViticultureChip.imageSize(im));
  }
  public Bot salvageRobot()
  {
	  Bot sp = super.salvageRobot();
	  return ((sp==null) ? Bot.RandomBot : sp);
  }
  public int getAltChipset()
  {	int v = 0;
  	if(mainBoard.testOption(Option.LimitPoints)) { v |= 1; }	// use alternate chipset bit 1 to indicate the limit points overlay
  	return v;
  }
  public boolean PerformAndTransmit(commonMove m,boolean transmit,replayMode replay)
  {
	  
  	// if stray ephemeral moves arrive after we have finalized the setup, just
  	// flush them.  This can occur if there is a realtime race between confirming
  	// the setup and the other player changing his mind about the setup.
  	//
  	if(mainBoard.simultaneousTurnsEverAllowed()		// if they are ever allowed
  		&& !mainBoard.simultaneousTurnsAllowed()	// but not right now
  		&& m.isEphemeral()) 						// and this is any ephemeral move
  		{ return true;
  		}
  

  	boolean v = super.PerformAndTransmit(m,transmit,replay);
  	if(v && m.op==EPHEMERAL_COMMENCE)
  	{
  		canonicalizeHistory();
  	}
  	return v;
  }

  public commonMove convertToSynchronous(commonMove m)
  {	  if(m.op==EPHEMERAL_COMMENCE) { m.op = MOVE_COMMENCE; return m; }
  		return null;
  }

  public void updatePlayerTime(long inc,commonPlayer p)
  {	if(!reviewMode() && simultaneousTurnsAllowed()) {}
  	else { super.updatePlayerTime(inc,p); }
  }
}

