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
package imagine;


import static imagine.Imaginemovespec.*;

import java.awt.*;
import online.common.*;
import java.util.*;

import bridge.Config;
import common.GameInfo;
import lib.Base64;
import lib.Graphics;
import lib.CalculatorButton;
import lib.CellId;
import lib.DrawableImage;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.Keyboard;
import lib.LFrameProtocol;
import lib.MouseState;
import lib.StockArt;
import lib.TextContainer;
import online.game.*;
import online.game.BaseBoard.BoardState;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;


/**
 * 
 * Imagine is Inspired by the games Dixit and Mysterium, and the movie Inception
 * initial work March 2021
 *  
 *  
*/

public class ImagineViewer extends CCanvas<ImagineCell,ImagineBoard> implements  ImagineConstants
{	static final long serialVersionUID = 1000;

	static final String Imagine_SGF = "imagine"; // sgf game name

	// file names for jpeg images and masks
	static final String ImageDir = "/imagine/images/";
	static final String Deck1Dir = 
		G.isCodename1() 
			? "/appdata/imagine-deck1/images/"
			: ImageDir;

static String SWOOSH = ImageDir + "swoosh"+ Config.SoundFormat;

     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackgroundColor = new Color(255,230,230);
    private Color rackBackGroundColor = new Color(225,225,225);
    private Color boardBackgroundColor = new Color(225,225,225);

	private Color ImagineMouseColors[] = {
			 Color.blue, Color.green, bsOrange, bsPurple, Color.black, Color.yellow ,Color.black,Color.white

	};
	private Color ImagineMouseDotColors[] = {
			Color.white,Color.white,Color.white,
			Color.white,Color.white,Color.black,
			Color.white,Color.black
	};
  
    // private state
    private ImagineBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addZoneRect("chip",MAX_PLAYERS);
    private Rectangle infoRects[] = addZoneRect("info",MAX_PLAYERS);

    private Rectangle skipRect = addRect("skipRect");	// used for "skip a turn" button
    private Rectangle getnewRect = addRect("getnewRect");	// used for "skip a turn" button
    private Rectangle myCardsRect = addRect("mycards");
    private Rectangle modeRect = addRect("mode");
    private Rectangle storyRect = addRect("story");
    private Rectangle stakeRect = addRect("stake");
    private Rectangle storyLegend = addRect("storyLegend");
    private TextContainer story = new TextContainer(ImagineId.Story);
    private Keyboard keyboard = null;
	private boolean useKeyboard = G.isCodename1();

/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	ImagineChip.preloadImages(loader,ImageDir,Deck1Dir);	// load the images used by stones
		gameIcon = ImagineChip.Icon.image;
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
        // use_grid=reviewer;// use this to turn the grid letters off by default
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	ImagineConstants.putStrings();
        }
        MouseColors  = ImagineMouseColors;
        MouseDotColors = ImagineMouseDotColors;
         
        String type = info.getString(GameInfo.GAMETYPE, ImagineVariation.Imagine.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new ImagineBoard(type,players_in_game,randomKey,getStartingColorMap(),ImagineBoard.REVISION);
        doInit(false);
        adjustPlayers(players_in_game);
    }

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
    //public void startPlaying()
    //{	super.startPlaying();
    //}
    

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
    {
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*22;	
       	int minChatW = fh*35;	
        int minLogH = fh*14;	
        int margin = fh/2;
        int buttonW = fh*8;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*3,	// minimum cell size
    			fh*4,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,skipRect,getnewRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	//layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// There are two classes of boards that should be rotated. For boards with a strong
    	// "my side" orientation, such as chess, use seatingFaceToFaceRotated() as
    	// the test.  For boards that are noticably rectangular, such as Push Fight,
    	// use mainW<mainH
        int nrows = 6;  				// b.boardRows
        int ncols = CARDS_PER_PLAYER; 	// b.boardColumns
  	
        
        
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
       	layout.returnFromMain(extraW,extraH);
   	
        G.copy(boardRect,main);		// board is the whole extra area

    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateH = fh*5/2;
        G.placeStateRow(boardX,stateY,boardW ,stateH,iconRect,stateRect,noChatRect);
        int storyLeft = boardX+CELLSIZE;
        int storyTop = stateY+stateH;
        int storyW = boardW-CELLSIZE*2;
        G.SetRect(storyLegend, storyLeft, storyTop,storyW,stateH);
        storyTop += stateH;
        G.SetRect(storyRect,storyLeft,storyTop,storyW,stateH*2);
        storyTop += stateH*2+stateH/2;
        int stakeLeft = storyLeft+CELLSIZE;
        int stakeHH = stateH*3/2;
        G.SetRect(stakeRect, stakeLeft, storyTop,storyW-2*CELLSIZE, stateH*3/2);
        G.SetRect(modeRect,boardX,storyTop,stakeLeft-boardX,stakeHH);
        storyTop += stateH*2;
        story.setBounds(storyRect);
        story.setEditable(this,true);
        story.setScrollable(false);
        story.setVisible(true);
    	
        G.SetRect(myCardsRect,boardX,storyTop,boardW,boardBottom-storyTop-stateH);

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        if(keyboard!=null) { keyboard.resizeAndReposition(); }
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle info = infoRects[player];
    	G.SetRect(chip,	x,	y,	unitsize*2,	unitsize);
    	G.SetRect(info, x, y+unitsize,unitsize*2,unitsize);
    	Rectangle box =  pl.createRectangularPictureGroup(x+9*unitsize/4,y,2*unitsize/3);
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*3 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.union(box, done,chip,info);
    	pl.displayRotation = rotation;
    	return(box);
    }

	// draw a box of spare chips. For pushfight it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, Rectangle info,commonPlayer pl, HitPoint highlight,ImagineBoard gb)
    {	int player = pl.boardIndex;
        boolean canhit = gb.legalToHitChips(player) && G.pointInRect(highlight, r);
        PlayerBoard pb = gb.getPlayerBoard(player);
        pb.color.chip.drawChip(gc, this, r, ""+pb.getScore());
        int xp = G.centerX(r);
        int yp = G.centerY(r);
        
        //set the location of the player cards, which are never seen directly 
        for(ImagineCell c : pb.cards) { c.setCurrentCenter(xp, yp); } 
        
        if(gb.isStoryTeller(player) && (gb.getState()!=ImagineState.Appreciate))
        {
        	GC.Text(gc,true, info, Color.black,null,s.get(StoryTellerMessage));
        }
        else if(pb.isReady())
        {
        	GC.Text(gc, true, info, Color.black,null,s.get(ReadyMessage));
        }
        	
        if (canhit)
        {
            highlight.hitCode = ImagineId.HitPlayerChip;
            highlight.arrow = (gb.pickedObject!=null)?StockArt.DownArrow:StockArt.UpArrow;
            highlight.awidth = CELLSIZE;
        }

        if (gc != null)
        { // draw a random pile of chips.  It's just for effect

            if (canhit)
            {	// draw a highlight background if appropriate
                GC.fillRect(gc, HighlightColor, r);
            }

            GC.frameRect(gc, Color.black, r);
            
            }
     }
    /**
     * return the dynamically adjusted size during an animation.  This allows
     * compensation for things like the zoom level of the board changing after
     * the animation is started.
     */
   // public int activeAnimationSize(Drawable chip,int thissize) 	{  		return(thissize); 	}

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
    	ImagineChip.getChip(obj).drawChip(g,this,bb.cellSize(), xp, yp, null);
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
     ImagineChip.backgroundTile.image.tileImage(gc, fullRect);   
      drawFixedBoard(gc);
     }
    
    // land here after rotating the board drawing context if appropriate
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {
        boolean reviewBackground = reviewMode()&&!mutable_game_record;
        ImagineBoard gb = disB(gc);
        if(reviewBackground)
        {	 
         ImagineChip.backgroundReviewTile.image.tileImage(gc,brect);   
        }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(gb,brect);
	      // if the board is one large graphic, for which the visual target points
	      // are carefully matched with the abstract grid
	      //G.centerImage(gc,images[BOARD_INDEX], brect,this);

	      // draw a picture of the board. In this version we actually draw just the grid
	      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
	      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
	      // on the board to fine tune the exact positions of the text

	      // draw the tile grid.  The positions are determined by the underlying board
	      // object, and the tile itself if carefully crafted to tile the pushfight board
	      // when drawn this way.  For games with simple graphics, we could use the
	      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
	      // but for more complex graphics with overlapping shadows or stacked
	      // objects, this double loop is useful if you need to control the
	      // order the objects are drawn in.
 
      	
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
    	switch(bb.getState())
    	{
    	case Story:
    	case Play:
    	case Vote:
    		if (boardRect.contains(x, y)&&!mutable_game_record)
    		{
    		G.SetLeft(p, -1);
    		G.SetTop(p,-1);
    		return("off");
            }
    		break;
    	default: break;
    	}
        return (super.encodeScreenZone(x,y,p));
    }
 
    private void drawStoryBox(Graphics gc,ImagineBoard gb,int who,Rectangle r,Rectangle legend,HitPoint highlight0)
    {	ImagineState state = gb.getState();
    	commonPlayer ap = guiPlayer(gb);
    	boolean spec = isSpectator();
    	HitPoint highlight = spec ? null : highlight0;
    	switch(state)
    	{
    	case Getnew:
    	case Skip:
    	case Story:
    	{
    		int storyTeller = gb.architect();
    		if((gb.whoseTurn==ap.boardIndex) && !spec)
    			{
    			{
    				HitPoint kp = highlight;
    				if(keyboard!=null && keyboard.containsPoint(kp)) { ap=null; }
    				story.redrawBoard(gc, kp);
    			}
    			GC.setFont(gc, largePlainFont());
    			if(GC.handleSquareButton(gc,skipRect,highlight,SkipStory,
    					HighlightColor, 
    					state==ImagineState.Skip ? HighlightColor : rackBackGroundColor))
    				{
    				highlight.hitCode = ImagineId.Skip;
    				}
       			if(GC.handleSquareButton(gc,getnewRect,highlight,GetnewStory,
       					HighlightColor,
       					state==ImagineState.Getnew ? HighlightColor : rackBackGroundColor))
       				{
       				highlight.hitCode = ImagineId.GetNew;
       				}
    			}
    		else 
    			{
        		GC.setFont(gc, largeBoldFont());
        		GC.Text(gc, true, r, Color.white,Color.black,s.get(WaitForStory,prettyName(storyTeller)));
        		GC.frameRect(gc, Color.white, r);
    			}
    		break;
    	}
       	case Appreciate:
    		if(gb.isStoryTeller(who)) {  story.clear(); }
			//$FALL-THROUGH$
		default:
    		GC.setFont(gc, largeBoldFont());
    		GC.Text(gc, true, r, Color.white,Color.black,gb.getSelectedStory());
    		GC.frameRect(gc, Color.white, r);
    		break;
    	}
    }
    public void drawStakeBox(Graphics gc,ImagineBoard gb,int who,Rectangle r,HitPoint highlight)
    {	ImagineChip buttons[] = ImagineChip.Buttons;
    	int w = G.Width(r);
    	int h = G.Height(r);
    	int left = G.Left(r);
    	int top = G.Top(r);
    	int step = w/buttons.length;
    	boolean spectator = isSpectator();
    	int cy = top+h/2;
    	int margin = step/6;
    	int sz = step-margin;
       	PlayerBoard pb = gb.getPlayerBoard(who);
       	boolean vote = gb.getState()==ImagineState.Vote;
    	boolean ready = pb.isReady() || (vote && gb.isStoryTeller(who));
    	int stake = vote ? pb.getBet() : pb.getStake();
     	for(int i=0;i<buttons.length;i++)
    		{	ImagineChip chip = buttons[i];
    			int cx = left+step*i+sz/2;
    		    if(chip.drawChip(gc, this, sz,cx,
    		    		cy,ready ? null : highlight,chip.id,null)) 
    		    	{ highlight.hit_index = who*100+i; 
    		    	}
    		    if(!spectator && i==stake) 
    		    	{ pb.color.checkMark.drawChip(gc,this,sz/3,cx-sz/3,cy+sz/3,null); }
    		}
    }
    public void drawLegend(Graphics gc,ImagineBoard gb)
    {	int h = G.Height(storyLegend)*3/2;
    	int yp = G.centerY(storyLegend);
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc, true, storyLegend,Color.black,null,
    			gb.GameOver() 
    					? s.get(BoardState.GameOverStateDescription) 
    					: s.get(RoundMarker,gb.currentRound+1,gb.numberOfRounds));
    	// use an explicit backs deck rather than CardBacks, so the game doesn't 
    	// pull the entire hires deck all at once.
    	gb.backs.drawStack(gc, this,null,h,G.Left(storyLegend)+h/2,yp,0,0.007,0,null);
    	gb.deck.copyCurrentCenter(gb.backs);
    	gb.discards.drawStack(gc, this,null,h,G.Right(storyLegend)-h/2,yp,0,-0.015,0,null);

    }
    ImagineCell bigChip = null;
    public void drawBigChip(Graphics gc,HitPoint highlight,Rectangle cards,boolean appreciate)
    {	int w = G.Width(cards);
    	int step = w/40;
    	Rectangle inset = G.copy(null,cards);
    	
    	G.insetRect(inset,step);
     StockArt.Scrim.getImage().stretchImage(gc, cards);
    	ImagineChip bigTop = bigChip.topChip();
    	if(bigTop.drawChip(gc, this, inset,appreciate ? null : highlight, ImagineId.Card,(String)null,1))
    	{
    		highlight.hitObject = bigChip; 
    	}
    	if(StockArt.FancyCloseBox.drawChip(gc, this, step*2, G.Right(inset),G.Top(inset),highlight,ImagineId.Eye,null,1.0,1.33)
    			|| (appreciate && G.pointInRect(highlight,inset))
    			)
    	{
    		highlight.hitObject = bigChip;
    		highlight.hitCode = ImagineId.Eye;
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
    public void drawBoardElements(Graphics gc, ImagineBoard gb, int apIndex, Rectangle cards, HitPoint highlightAll)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	HitPoint highlight = (bigChip==null) ? highlightAll : null;
    	// something which might allow an action.  
    	drawLegend(gc,gb);
    	drawStoryBox(gc,gb,apIndex,storyRect,storyLegend,highlight);
    	HitPoint activeHighlight = bigChip==null ? highlight : null;
    	boolean appreciate = false;
    	boolean storyTeller = gb.isStoryTeller(apIndex);
    	boolean ready = gb.isReady(apIndex);
    	String msg = "";
    	switch(gb.getState())
    	{
    	case Vote:
        	drawStakeBox(gc,gb,apIndex,stakeRect,highlight);
        	if(!storyTeller)
    		{
        	if(!ready) 
        		{ msg = s.get(VoteBestMatch);
        		  // prevent voting for self by clicking on the magnified chip
        		  appreciate |= ((bigChip!=null) && (gb.getSelectedChip(apIndex)==bigChip.topChip())); 
        		}
    		}
        	else { 
        	msg = s.get(OthersVoting);
        	}
       		drawPresentationCards(gc,gb,apIndex,cards,activeHighlight,highlight);
    		
       		break;
		case Gameover:
			if(gb.revision<102) 
				{ 
				drawStakeBox(gc,gb,apIndex,stakeRect,highlight);
	    		drawPlayerCards(gc,gb,apIndex,cards,activeHighlight,highlight);
	    		break; 
				}
			//$FALL-THROUGH$
		case Appreciate:
			appreciate =true;
    		drawPresentationCards(gc,gb,apIndex,cards,activeHighlight,highlight);
        	msg = s.get(ViewResult);
    		break;
		case Skip:
		case Getnew:
		case Story:
    		if(apIndex==gb.architect()) { drawStakeBox(gc,gb,apIndex,stakeRect,highlight); }
    		drawPlayerCards(gc,gb,apIndex,cards,storyTeller ? activeHighlight : null,highlight);
    		if(!storyTeller)
    		{
        	msg = s.get(WaitForArchitect);
    		}
    		else {
    		msg = s.get(YouMustSelect);
    		}
    		break;
		case Play:
			if(ready)
			{	msg = s.get(OthersAreSelecting);
				drawPreviewCards(gc,gb,apIndex,cards,activeHighlight,highlight);
				break;
			}
			
			msg = s.get(SelectYourImage); 
    		//$FALL-THROUGH$
		default:
			{
    		drawStakeBox(gc,gb,apIndex,stakeRect,highlight);
    		drawPlayerCards(gc,gb,apIndex,cards,activeHighlight,highlight);
    		break;
			}
    	}
    
    	GC.setFont(gc, largeBoldFont());
    	GC.Text(gc, true, modeRect, Color.black, null, msg);

    	if(bigChip!=null)
    	{	 
    		drawBigChip(gc,highlightAll,cards,appreciate);
    	}
    }

    public void drawPlayerCards(Graphics gc, ImagineBoard gb, int apIndex, Rectangle cards, HitPoint highlight,HitPoint anyHighlight)
    {
    	int pw = G.Width(cards);
    	int ph = G.Height(cards);
    	int stepx = pw/3;
    	int stepy = ph/2;
    	int left = G.Left(cards);
    	int right = G.Right(cards);
    	int left0 = left;
    	int margin = stepx/40;
    	int top = G.Top(cards);
    	boolean spectator = isSpectator();
    	boolean debug = G.debug();
    	PlayerBoard pb = gb.getPlayerBoard(apIndex);
    	ImagineCell pics[] = pb.getCards();
    	boolean ready = pb.isReady();
    	int sstepx = stepx-margin*2;
    	int sstepy = stepy-margin*2;
    	int checksize = stepx/7;
     	for(ImagineCell pic : pics)
    	{	int xp = left+margin;
    		int yp = top+margin;
    		pic.setCurrentCenter(xp, yp);
    		ImagineChip topchip = pic.topChip();   
    		boolean hitmain = false;
    		if(topchip!=null)
    		{
    		Rectangle r = topchip.getSnugRectangle(this,xp,yp,sstepx,sstepy);
    		
    		if( topchip.drawChip(gc, this,r,ready ? null : highlight,pic.rackLocation(),debug ? ""+topchip : null,1.0))
    		{	hitmain = true;
    		}
    		if(!spectator && pb.isSelected(pic)) {
    			pb.color.checkMark.drawChip(gc, this, checksize, G.Left(r)+checksize/2,G.Bottom(r)-checksize/2,null);
    			}
    		if(StockArt.Magnifier.drawChip(gc, this, checksize, G.Right(r)-checksize/2,G.Bottom(r), anyHighlight, ImagineId.Eye,null,1.0,1.33))
    			{ 
    			anyHighlight.hitObject = pic;
    			anyHighlight.spriteRect = null;
    			}
    			else if(hitmain) 
    			{ // dont set highlighting until the magnifier is drawn and missed
    			  highlight.hitObject = pic;
    			  highlight.spriteColor =Color.red;			  
    			}
    		}
    		left += stepx;
    		if(left+stepx/2>right) { left = left0; top += stepy; }
    	}
    	
    } 
    
    public void drawPresentationCards(Graphics gc, ImagineBoard gb, int apIndex,Rectangle cards, HitPoint highlight,HitPoint anyHighlight)
    {
     	ImagineCell pics[] = gb.selectedPresentation;
     	PlayerBoardStack votes[] = gb.presentationVotes;
    	PlayerBoard pb = gb.getPlayerBoard(apIndex);
    	ImagineChip myCard = gb.getSelectedChip(apIndex);
    	boolean debug = G.debug();
    	ImagineState state = gb.getState();
    	boolean isArchitect = gb.isArchitect(apIndex);
      	boolean appreciate = isArchitect || (state==ImagineState.Appreciate) || (state==ImagineState.Gameover);
     	int pw = G.Width(cards);
    	int ph = G.Height(cards);
     ImagineChip.presentationTile.getImage().stretchImage(gc, cards);
    	GC.frameRect(gc, Color.black, cards);
    	int stepx = pw/3;
    	int stepy = ph/2;

    	int left = G.Left(cards);
    	int right = G.Right(cards);
    	int left0 = left;
    	int margin = stepx/40;
    	int top = G.Top(cards);
    	int selectedPres = pb.getSelectedPresentation();
    	int sstepx = stepx-margin*2;
    	int sstepy = stepy-margin*2;
    	int checksize = stepx/7;
     	for(int picn=0;picn<pics.length;picn++)
    	{	ImagineCell pic = pics[picn];
    		PlayerBoardStack vote = votes[picn];
    		int xp = left+margin;
    		int yp = top+margin;
    		pic.setCurrentCenter(xp,yp);
    		ImagineChip topchip = pic.topChip();
    		PlayerBoard owner = gb.getOwner(topchip);
    		if(topchip!=null)
    		{
    		Rectangle r = topchip.getSnugRectangle(this,xp,yp,sstepx,sstepy);
    		int lr = G.Left(r);
    		int tr = G.Top(r);
    		int br = G.Bottom(r);
    		HitPoint notMine = (gb.isStoryTeller(apIndex) || appreciate || gb.isReady(apIndex) || (topchip==myCard)) ? null : highlight;

    		boolean hitmain = false;
    		if(topchip.drawChip(gc, this,r,notMine,pic.rackLocation(),debug ? ""+topchip : null, 1.0))
    		{	hitmain = true;
    			notMine.spriteColor =Color.red;
    		}
    		if(appreciate && owner!=null && gb.isStoryTeller(owner.boardIndex))
			{ GC.frameRect(gc, Color.red, r); 
			  Rectangle r1 = G.copy(null, r);
			  G.insetRect(r1, -1);
			  GC.frameRect(gc, Color.black, r1); 
			  G.insetRect(r1, -1);
			  GC.frameRect(gc, Color.red, r1); 
			} 
    		if(!isArchitect && !appreciate && (picn==selectedPres)) {
    			pb.color.checkMark.drawChip(gc, this, checksize, lr+checksize/2,br-checksize/2,null);
    		}
    		if(StockArt.Magnifier.drawChip(gc, this, checksize, G.Right(r)-checksize/2,G.Bottom(r), anyHighlight, ImagineId.Eye,null,1.0,1.33))
			{ 
    		anyHighlight.hitObject = pic;
			anyHighlight.spriteRect = null;
			}
			else if(hitmain) 
			{ // dont set highlighting until the magnifier is drawn and missed
			  highlight.hitObject = pic;
			  highlight.spriteColor =Color.red;			  
			}

    		if(appreciate)
    		{
    			
    			DrawableImage<?> chip = (owner == null) ? StockArt.SmallX : owner.color.chip;
    			String stake = owner==null ? null : ""+owner.getStake();
    			{
    			int ll =lr+margin*3;
    			int sz = stepx/5;
    			chip.drawChip(gc, this, sz,ll, tr, stake);
    			
      			for(int voten=0;voten<vote.size();voten++)
    				{
    				PlayerBoard voter = vote.elementAt(voten);
    				voter.color.checkMark.drawChip(gc,this,stepx/6,lr+margin*3+(voten+1)*stepx/6,tr,""+voter.getBet());
    				}
    			}}}
    		
    		left += stepx;
    		if(left+stepx/2>right) { left = left0; top += stepy; }
    	}
   	
    }
    
    public void drawPreviewCards(Graphics gc, ImagineBoard gb, int apIndex,Rectangle cards, HitPoint highlight,HitPoint anyHighlight)
    {
    	boolean debug = G.debug();
    	boolean isArchitect = gb.isArchitect(apIndex);
       	int pw = G.Width(cards);
    	int ph = G.Height(cards);
     ImagineChip.presentationTile.getImage().stretchImage(gc, cards);
    	GC.frameRect(gc, Color.black, cards);
    	int stepx = pw/3;
    	int stepy = ph/2;

    	int left = G.Left(cards);
    	int right = G.Right(cards);
    	int left0 = left;
    	int margin = stepx/40;
    	int top = G.Top(cards);
    	int sstepx = stepx-margin*2;
    	int sstepy = stepy-margin*2;
		int xp = left+margin;
		int yp = top+margin;
    	int checksize = stepx/7;
    	for(PlayerBoard pb : gb.pbs)
    	{
    		if(pb.isReady())
    		{	ImagineCell card = pb.getSelectedCard();
    			ImagineChip topchip = isArchitect||allowed_to_edit ? card.topChip() : ImagineChip.cardBack;
    			Rectangle r = topchip.getSnugRectangle(this,xp,yp,sstepx,sstepy);
    	   		int lr = G.Left(r);
    	   		int tr = G.Top(r);
         		topchip.drawChip(gc, this,r, debug ? ""+topchip : null);
        		left += stepx;
        		if(left+stepx/2>right) { left = left0; top += stepy; }
        		
        		DrawableImage<?> chip = pb.color.chip;
     			int ll =lr+margin*3;
    			int sz = stepx/5;
    			chip.drawChip(gc, this, sz,ll, tr, null);    	
    			
        		if(isArchitect && StockArt.Magnifier.drawChip(gc, this, checksize, G.Right(r)-checksize/2,G.Bottom(r), anyHighlight, ImagineId.Eye,null,1.0,1.33))
    			{ 
        		anyHighlight.hitObject = card;
    			anyHighlight.spriteRect = null;
    			}
        		xp += stepx;
        		if(xp+stepx/2>right) { xp = left0; yp += stepy; }
     		}
    	}

    }
    private String standardGameMessage(PlayerBoard pb,ImagineState state,ImagineBoard gb)
    {	if(gb.simultaneousTurnsAllowed(state) && pb.isReady()) 
    		{ String msg = state.readyDescription;
    		  if(msg!=null) { return s.get(msg); } 
    		}
    	return(s.get(state.description()));
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
    public void redrawBoard(Graphics gc, HitPoint selectPos0)
    {  HitPoint selectPos = selectPos0;
       Keyboard kb = getKeyboard();
       if(kb!=null )
        {  selectPos = null;
        }
       
       ImagineBoard gb = disB(gc);
       ImagineState state = gb.getState();
       
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

       drawGameLog(gc, nonDragSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),standardBoldFont());

       // this does most of the work, but other functions also use contextRotation to rotate
       // animations and sprites.
       commonPlayer ap = guiPlayer(gb);
       int apIndex = ap.boardIndex;
       GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);
       drawBoardElements(gc, gb, apIndex, myCardsRect, selectPos);
       GC.unsetRotatedContext(gc,selectPos);
       
       boolean planned = plannedSeating();
       boolean spectator = isSpectator();
       
       for(int player=0;player<bb.players_in_game;player++)
       	{ commonPlayer pl = getPlayerOrTemp(player);
       	  pl.setRotatedContext(gc, selectPos,false);
    	   DrawChipPool(gc, chipRects[player],infoRects[player],pl, ourTurnSelect,gb);
    	   if(!spectator && planned && gb.whoseTurn==player)
    	   {
    		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
       	   pl.setRotatedContext(gc, selectPos,true);
       	}
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       
       GC.setFont(gc,standardBoldFont());
       
       if (state != ImagineState.Puzzle && !spectator)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{
				CellId button = GameId.HitDoneButton;
				switch(state)
				{
				case Story:
					if(buttonSelect!=null) 
						{ bb.pbs[gb.whoseTurn].setStory(story.getText());
						  button = ImagineId.SetStory;
						}
					break;
				case Appreciate:
					button = ImagineId.SetReady;
					break;
				case Play:
					button = ImagineId.SetCandidate;
					break;
				case Vote:
					button = ImagineId.SetChoice;
					break;
				case Skip:
				default: ;
				}
				boolean done = gb.DoneState(apIndex);
				if(handleDoneButton(gc,messageRotation,doneRect,done ? buttonSelect : null,HighlightColor, rackBackGroundColor))
					{
					buttonSelect.hitCode = button;
					}
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==ImagineState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        PlayerBoard apb = gb.getPlayerBoard(ap.boardIndex);
        standardGameMessage(gc,messageRotation,
            				state==ImagineState.Gameover?gameOverMessage(gb):standardGameMessage(apb,state,gb),
            				state!=ImagineState.Puzzle && !gb.simultaneousTurnsAllowed(state),
            				gb.whoseTurn,
            				stateRect);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(ImagineVictoryCondition),progressRect, goalRect);
            //      DrawRepRect(gc,pl.displayRotation,Color.black,b.Digest(),skipRect);
        
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);

		if(kb!=null)
		{
			kb.draw(gc, selectPos0);
		}
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
        if(mm.op==MOVE_COMMIT || mm.op==MOVE_SCORE)
        	{
        	canonicalizeHistory();
        	}
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
		if(replay.animate) { playSounds(mm); }
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

 void playSounds(commonMove mm)
 {
	 switch(mm.op)
	 {
	 case SET_STORY:
	 case MOVE_COMMIT:
	 case MOVE_SCORE:
	 	playASoundClip(SWOOSH,200);
	 	break;
	 case MOVE_PICK:
	 case MOVE_DROP:
	 case MOVE_SELECT:
	 case SET_STAKE:
	 case EPHEMERAL_SET_CANDIDATE:
	 case EPHEMERAL_SET_STAKE:
	 case EPHEMERAL_MOVE_SELECT:
	 case EPHEMERAL_SET_CHOICE:
	 case EPHEMERAL_SET_READY:
		 if(mm.player==guiPlayer(bb).boardIndex) 
		 	{ playASoundClip(light_drop,100); }
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
        return (new Imaginemovespec(st, pl));
    }

      /**
       *  the default behavior is if there is a picked piece, unpick it
       *  but if no picked piece, undo all the way back to the done.
       */
      //public void performUndo()
      //{
      //	if(allowBackwardStep()) { doUndoStep(); }
      //	else if(allowUndo()) { doUndo(); }
      //
      //}


    
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
    public commonPlayer nextActivePlayer(ImagineBoard gb)
    {
    	int index = History.viewStep-1;
    	if(gb.simultaneousTurnsAllowed() && (index>=0) && (index<History.size()))
    	{
    		commonMove m = History.elementAt(index);
    		return(getPlayerOrTemp(m.player));
    	}
    	return(getPlayerOrTemp(gb.whoseTurn));
    }
    // this controls who gets the highlighted indication in the 
    // player info area.
    public boolean playerIsActive(commonPlayer pl)
    {	if(reviewOrOffline()) { return nextActivePlayer(bb)==pl; }
    	if(bb.simultaneousTurnsAllowed())
    	{
       		PlayerBoard pb = bb.getPlayerBoard(pl.boardIndex);
       		return(!pb.isReady());
    	}
    	return (pl.boardIndex==bb.whoseTurn);
    	
    	

    }
    public boolean reviewOrOffline()
    {
    	return (allPlayersLocal() || (allowed_to_edit && (reviewMode()||reviewOnly)) || isSpectator());
    }
	public commonPlayer guiPlayer(ImagineBoard gb)
	{
		return reviewOrOffline() 
				 ? nextActivePlayer(gb)
				 : getActivePlayer();
	}


	public void MouseDown(HitPoint p)
	{	
		if(keyboard!=null) 
			{ keyboard.MouseDown(p);
			  //Plog.log.addLog("Down "+p+" and repaint");
			  repaint();
			}	
		super.MouseDown(p);
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
        if(id instanceof CalculatorButton.id) { }
        else if(!(id instanceof ImagineId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        ImagineId hitCode = (ImagineId)id;
		commonPlayer ap = guiPlayer(bb);
		PlayerBoard pb = bb.getPlayerBoard(ap.boardIndex);
		switch(bb.getState())
		{	case Story: 
				pb.setStory(story.getText());
				break;
			default: break;
		}
        switch (hitCode)
        {
        default:
        	if (performStandardButtons(hitCode, hp)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " + hitCode);
            }
        	break;
        case Eye:
        	{
        	ImagineCell chip = hitCell(hp);
        	if(bigChip==chip) { bigChip = null; }
        	else { bigChip = chip; }
        	}
        	break;
        case GetNew:
        	PerformAndTransmit("GetNew");
        	break;
        case Skip:
        	PerformAndTransmit("Skip");
        	break;
        case Stake_0:
        case Stake_1:
        case Stake_2:
        	int who = hp.hit_index/100;
        	int ind = hp.hit_index%100;
        	String op = simultaneousTurnsAllowed() ? "eSetStake " : "SetStake ";
        	PerformAndTransmit(op+(char)(who+'A') + " "+ind);
        	break;

        case SetChoice:
    	{
    		int sel = bb.getSelectedPresentation(ap.boardIndex);
        	ImagineChip card = bb.selectedPresentation[sel].topChip();
         	int stake = bb.getBet(ap.boardIndex);
         	String opc = simultaneousTurnsAllowed() ? "eSetChoice " : "SetChoice ";
        	PerformAndTransmit(G.concat(opc,
        			(char)('A'+ap.boardIndex)," ",
        			stake," ",
        			Base64.encodeSimple(card.deck)," ",
        			Base64.encodeSimple(card.name)));
 	
    	}
    	break;

        case SetReady:
        	{
    		commonPlayer pl = guiPlayer(bb);
    		String opc = simultaneousTurnsAllowed() ? "eSetReady " : "SetReady ";
    		PerformAndTransmit(G.concat(opc,
    				(char)('A'+pl.boardIndex)
    				));
        	}
        	break;
        case SetCandidate:
        	{
        		commonPlayer pl = guiPlayer(bb);
            	ImagineChip card = bb.getSelectedChip(pl.boardIndex);
            	if(card!=null)
            	{
             	int stake = bb.getStake(pl.boardIndex);
             	String opc = simultaneousTurnsAllowed() ? "eSetCandidate " : "SetCandidate ";
            	PerformAndTransmit(G.concat(opc,
            			(char)('A'+pl.boardIndex)," ",
            			stake," ",
            			Base64.encodeSimple(card.deck)," ",
            			Base64.encodeSimple(card.name)));
            	}
        	}
        	break;
        case SetStory:
        	{
        	commonPlayer pl = guiPlayer(bb);
        	ImagineChip card = bb.getSelectedChip(pl.boardIndex);
        	if(card!=null)
        	{
        	String story = bb.getStory(pl.boardIndex);
        	int stake = bb.getStake(pl.boardIndex);
        	PerformAndTransmit(G.concat("SetStory ",
        			stake," ",
        			Base64.encodeSimple(card.deck)," ",
        			Base64.encodeSimple(card.name)," ",
        			Base64.encodeSimple(story)));
        	}}
        	break;
        	
        case Presentation:
        case Card:
        	{
        	bigChip = null;
        	ImagineCell hitObject = hitCell(hp);
        	ImagineState state = bb.getState();
        	if(!pb.isReady()
        			&& (state!=ImagineState.Gameover)
        			&& ((state!=ImagineState.Story)||bb.isArchitect(ap.boardIndex)))
        	{
        	// use the active player rather than the actual cell row, so the
        	// same logic works for selecting player cards and selecting from
        	// the voter presentation. 
        	// do not allow selection when we are already ready
        	String ops = simultaneousTurnsAllowed() ? "eSelect " : "select ";
        	PerformAndTransmit(G.concat(ops,
        								hitObject.rackLocation().shortName(),
        								" ",
        								(char)(ap.boardIndex+'A'),
        								" ",
        								hitObject.row));
        	}}
        	break;
        case Story:
        	{
        	story.setFocus(true);
    		if(useKeyboard) {
    			keyboard = new Keyboard(this,story);
    		}
    		else 
    		{	requestFocus(story); 
    			repaint(story.flipInterval);
    		}}
    		break;
        case HitPlayerChip:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+bb.pickedObject.id.shortName);
			}
           break;
 
        }
        }
    }



    private boolean setDisplayParameters(ImagineBoard gb,Rectangle r)
    {
      	boolean complete = false;
      	gb.SetDisplayRectangle(r);
      	if(complete) { generalRefresh(); }
      	return(complete);
    }
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     * <p>
     * if complete is true, we definitely want to start from scratch, otherwise
     * only the known changed elements need to be painted.  Exactly what this means
     * is game specific, but for pushfight the underlying empty board is cached as a deep
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
        //redrawBoard(gc,hp));
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
    	return(bb.gameType()); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Imagine_SGF); }	// this is the official SGF number assigned to the game

   
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
    {  throw G.Error("No robots");//(new ImaginePlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/26/2023
     * 	18 files visited 0 problems
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
    
    /** support for simultaneous turns **/
    

	//if true, run the UI with simultaneous plays. Otherwise, run the moves serially
	//like most of the other games.  Changing to false is intended for debugging only.
	static boolean SIMULTANEOUS_PLAY = true;
	public boolean gameHasEphemeralMoves() { return(SIMULTANEOUS_PLAY); }
	
 	   //
    // this is called both when starting a new game, and when restoring a game from history.
    // we need to inform our board which index is "us"
    //
    public void startPlaying()
    {	super.startPlaying();
    }
    
    public commonMove convertToSynchronous(commonMove m)
    {
    	switch(m.op) {
    	default: throw G.Error("not expecting ", m);
    	case EPHEMERAL_MOVE_SELECT:
    	case EPHEMERAL_SET_STAKE:
    		return(null);
    	case EPHEMERAL_SET_CANDIDATE:
    		m.op = SET_CANDIDATE;
    		break;
    	case EPHEMERAL_SET_CHOICE:
    		m.op = SET_CHOICE;
    		break;
    	case EPHEMERAL_SET_READY:
    		m.op = SET_READY;
    		break;
     	}
    	return(m);
    }
    
    public int startOfCensorship = 0;
    public void drawGameLog(Graphics gc, HitPoint highlight, Rectangle r,  Color textColor,Color highlightColor,Font bold,Font normal)
    {	startOfCensorship = History.size()-1;
    	String mn =null;
    	while(startOfCensorship>=0)
    		{ commonMove mv = History.elementAt(startOfCensorship);
    		  String mnum = mv.getSliderNumString();
    		  if((mv.op==MOVE_SCORE) || (mn!=null && !mn.equals(mnum))) { break; }
    		  startOfCensorship--;
    		  mn = mnum;
    		}
    	gameLog.redrawGameLog2(gc, highlight, r, textColor, highlightColor, bold, normal);
    }
     
    public void ViewerRun(int delay)
    {
    	super.ViewerRun(delay);
    	commonPlayer pl = getActivePlayer();
    	int myPlayer = pl.boardIndex;
    	
    	 if(!pl.isSpectator() && initialized && !reviewMode() && (bb.whoseTurn==myPlayer))
         {   if(bb.readyToScore()) { PerformAndTransmit("Score"); }
         	 else if(bb.readyToProceed()) {	 PerformAndTransmit("Commit"); }
         }
    }
    
	public HitPoint MouseMotion(int ex, int ey,MouseState upcode)
	{	if(keyboard!=null && keyboard.containsPoint(ex,ey))
			{	
			keyboard.doMouseMove(ex,ey,upcode);
			}
			else { story.doMouseMove(ex, ey, upcode); }
	
		return(super.MouseMotion(ex, ey, upcode));
	}
    public void closeKeyboard()
    {
    	Keyboard kb = keyboard;
    	if(kb!=null) { kb.setClosed(); }
    }
    private void loseFocus()
    {
    	if(useKeyboard)
    	{
    		story.setFocus(false);
    	}
    }
    public Keyboard getKeyboard() 
    { Keyboard k = keyboard;
      if(k!=null && k.closed) 
      	{ k = keyboard = null; 
      	  loseFocus();
      	}
      return(k); 
    }
    public void createKeyboard()
    {	if(useKeyboard)
    	{
    	keyboard = story.makeKeyboardIfNeeded(this,keyboard);
    	}
    }
    public void performReset()
    {	if(keyboard!=null) { closeKeyboard(); }
    	super.performReset();
    }
    public void updatePlayerTime(long inc,commonPlayer p)
    {	if(simultaneousTurnsAllowed())
    	{
    		for(commonPlayer pl : players)
    		{	
    			if(!bb.isReady(pl.boardIndex))
    			{
    				super.updatePlayerTime(inc,pl); 	// keep on ticking
    			}
    		}
    	}
    	else if(p!=null)
    	{ super.updatePlayerTime(inc,p); 
    	}
    }
    public boolean playerChanging()
    {
    	if(bb.simultaneousTurnsAllowed()) { return(false); }	
    	return(super.playerChanging());
    	
    }

}

