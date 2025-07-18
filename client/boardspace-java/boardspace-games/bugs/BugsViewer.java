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
    
    todo: bonus cards for diet and flying types?
    todo: bonus cards to clear cells "killer bonus"
    todo: allow predators to eat predators under some conditions - maybe when cell is full.
    todo: discard market cards if they stick around the bottom too long.
    todo: preview value of bonus cards
 */
package bugs;

import bridge.JMenuItem;
import bugs.data.MasterSpecies;
import bugs.data.Profile;
import bugs.data.Taxonomy;
import common.GameInfo;

import static bugs.BugsMovespec.*;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import online.common.*;
import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.ImageStack;
import lib.StockArt;
import lib.TextButton;
import lib.Toggle;
import lib.LFrameProtocol;
import lib.Sort;
import lib.Image;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;



/**
 * 
 * This is intended to be maintained as the reference example how to interface to boardspace.
 * <p>
 * The overall structure here is a collection of classes specific to Hex, which extend
 * or use supporting online.game.* classes shared with the rest of the site.  The top level 
 * class is a Canvas which implements ViewerProtocol, which is created by the game manager.  
 * The game manager has very limited communication with this viewer class, but manages
 * all the error handling, communication, scoring, and general chatter necessary to make
 * the game part of the site.
 * <p>
 * The main classes are:
 * <br>GameViewer - this class, a canvas for display and mouse handling
 * <br>GameBoard - board representation and implementation of the game logic
 * <br>GameMovespec - representation, parsing and printing of move specifiers
 * <br>GamePlay - a robot to play the game
 * <br>GameConstants - static constants shared by all of the above.  
 *  <p>
 *  The primary purpose of the PushfightViewer class is to do the actual
 *  drawing and to mediate the mouse gestures.  All the actual work is 
 *  done in an event loop, rather than in direct response to mouse or
 *  window events, so there is only one process involved.  With a single 
 *  process, there are no worries about synchronization among processes
 *  of lack of synchronization - both major causes of flakey user interfaces.
 *  <p>
 *  The actual mouse handling is done by the commonCanvas class, which simply 
 *  records the recent mouse activity, and triggers "MouseMotion" to be called
 *  while the main loop is executing.
 *  <p>
 *  Similarly, the actual "update" and "paint" methods for the canvas are handled
 *  by commonCanvas, which merely notes that a paint is needed and returns immediately.
 *  drawCanvas is called in the event loop.
 *  <p>
 *  The drawing methods here combine mouse handling and drawing in a slightly
 *  nonstandard way.  Most of the drawing routines also accept a "HitPoint" object
 *  which contains the coordinates of the mouse.   As objects are drawn, we notice
 *  if the current object contains the mouse point, and if so deposit a code for 
 *  the current object in the HitPoint.  the Graphics object for drawing can be null,
 *  in which case no drawing is actually done, but the mouse sensitivity is checked
 *  anyway.  This method of combining drawing with mouse sensitivity helps keep the
 *  mouse sensitivity accurate, because it is always in agreement with what is being
 *  drawn.
 *  <p>
 *  Steps to clone this hierarchy to start the next game
 *  <li> use eclipse refactor to rename the package and individual files
 *  <li> duplicate the game start configuration, making a new one for the new game
 *  <li> launch the new game and get it to start, still identical to the old pushfight in all but name.
 *  	this will probably require a few edits to the init code.
 *  <li> do a cvs update on the original pushfight hierarchy to get back the original code.
 *  
*/
public class BugsViewer extends CCanvas<BugsCell,BugsBoard> implements BugsConstants, PlacementProvider
{		// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
    static final String Prototype_SGF = "bugs"; // sgf game name

    // file names for jpeg images and masks
    static final String ImageDir = "/bugs/images/";
    static final String DataDir = "/bugs/data/";
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(240,240,240);
    private Color rackBackGroundColor = new Color(225,225,225);
    private Color rackIdleColor = new Color(205,172,162);
    private Color boardBackgroundColor = new Color(200,200,200);
    
    
     
    // private state
    private BugsBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
 
    private Toggle eyeRect = new Toggle(this,"eye",
 			StockArt.NoEye,BugsId.ToggleEye,NoeyeExplanation,
 			StockArt.Eye,BugsId.ToggleEye,EyeExplanation
 			);
    private Rectangle [] playerBugRects = addRect("playerbug",4);
    private Rectangle [] playerGoalRects = addRect("playerGoal",4);
    private Rectangle [] playerScoreRects = addRect("score",4);
    private Rectangle roundRect = addRect("round");
    private TextButton readyButton = addButton(ReadyButton,BugsId.Ready,ExplainReady,
			HighlightColor, rackBackGroundColor,rackIdleColor);
    private Rectangle marketCardRect = addRect("market");
    private Rectangle goalCardRect = addRect("goal");
    private Rectangle marketRect = addRect("both");
    private Rectangle chipRects[] = addZoneRect("chip",MAX_PLAYERS);
    private TextButton passButton = addButton(PASS,GameId.HitPassButton,ExplainPass,
			HighlightColor, rackBackGroundColor);
    
	private TextButton doneButton = addButton(DoneAction,BugsId.DoneButton,ExplainDone,
			HighlightColor, rackBackGroundColor,rackIdleColor);
	// private menu items
    private JMenuItem makeDeck = null;		// rotate the board view
    private Rectangle deckRect = addRect("deck");
    private Rectangle goalDeckRect = addRect("goal");

 /**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	BugsChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = BugsChip.Icon.image;
		// first, because it collects the set sizes
		Taxonomy.load(DataDir+"taxonomy_export.tsv");
		MasterSpecies.load(DataDir+"master_species_export.tsv");
		// profiles last, it constructs the bug cards
		Profile.load(DataDir+"profile_export.tsv",
				ImageDir+"categorypix/",
				ImageDir+"bugpix-1/",
				ImageDir+"bugpix/");
		G.print("Profiles ",Profile.profiles.size(),
				" Taxonomy ",Taxonomy.taxonomies.size(),
				" Species ",MasterSpecies.species.size(),
				" Bug Cards ",BugCard.bugCount());
    }

    /**
     * this is the hook for substituting alternate tile sets.  This is called at a low level
     * from drawChip, and the result is passed to the chip's getAltChip method to substitute
     * a different chip.
     */
    public int getAltChipset() { return(0); }
    

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
        //
        // not suitable for games with any optional "done" states.  If true, autodone
        // is controlled by an option menu option.  Also conditionalize showing the
        // "done" button with autoDoneActive()
        enableAutoDone = true;
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default
        
        addZoneRect("done",doneButton);	// this makes the "done" button a zone of its own
        
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	BugsConstants.putStrings();
        	makeDeck = myFrame.addAction("make deck",deferredEvents);
        }
        
        String type = info.getString(GameInfo.GAMETYPE, BugsVariation.bugspiel_parallel.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new BugsBoard(type,players_in_game,randomKey,getStartingColorMap(),BugsBoard.REVISION);
        //
        // this gets the best results on android, but requires some extra care in
        // the user interface and in the board's copyBoard operation.
        // in the user interface.
        useDirectDrawing(true);
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
	 * 
	 * this is a debugging hack to give you an event based on clicking in the player name
	 * You can take whatever action you like, or no action.
	 */
    //public boolean inPlayRect(int eventX, int eventY)
    //{	return(super.inPlayRect(eventX,eventY));
    //}

    /**
     * update the players clocks.  The normal thing is to tick the clocks
     * only for the player whose turn it is.  Games with a simtaneous action
     * phase need to do something more complicated.
     * @param inc the increment (in milliseconds) to add
     * @param p the current player, normally the player to update.
     */
    //public void updatePlayerTime(long inc,commonPlayer p)
    //{
    //	super.updatePlayerTime(inc,p);
    //}
	/**
	 * 
	 * this is a debugging hack to give you an event based on clicking in the time
	 * clock.  You can take whatever action you like, or no action.
	 * */
    //public boolean inTimeRect(int eventX, int eventY)
    //{
    //    boolean val = super.inTimeRect(eventX, eventY);
    //    return (val);
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
    	int minLogW = fh*15;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*2,	// minimum cell size
    			fh*3,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	// its generally recommended for the vcr to be no wider than the game log,
       	// this helps the vcr to tuck into the spare space allocated for the log
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	// games which have a private "done" button for each player don't need a public
    	// done button, and also we can make the edit/undo button square so it can rotate
    	// to face the player.  Placing the done/edit after the vcr sometimes gives better
    	// results, because it can be placed horizontally or vertically
    	Rectangle pass = null;
    	switch(bb.variation)
    	{
    	case bugspiel_sequential:
    	case bugspiel_sequential_large:
    		pass = passButton;
    		break;
    	case bugspiel_parallel:
    	case bugspiel_parallel_large:
    		break;
    	default: G.Error("Not expecting state %s",bb.variation);
    	}
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneButton,editRect,pass);
       	//layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);
       	layout.placeRectangle(deckRect,buttonW,buttonW/2,BoxAlignment.Center);
       	layout.placeRectangle(goalDeckRect,buttonW,buttonW/2,BoxAlignment.Center);
       	
       	{	
          	Rectangle peek = layout.peekMainRectangle();
          	// layout the largest possible
           	int pw = G.Width(peek)-margin*2;
           	int ph = G.Height(peek)-margin*2;
           	int divw = ph/N_MARKETS*3;
           	int divh = pw/N_MARKETS*3;
       		// place either horizontally or vertically
        	layout.placeRectangle(marketRect,
        			divw,ph,divw,ph,
        			divh,pw,divh,pw,
        			BoxAlignment.Right,true);
        	G.copy(goalCardRect,marketRect);
        	int l = G.Left(goalCardRect);
        	int t = G.Top(goalCardRect);
        	int w = G.Width(goalCardRect);
        	int h = G.Height(goalCardRect);
        	G.SetWidth(goalCardRect,(int)(w*0.35));
        	G.SetRect(marketCardRect,l+(int)(w*0.35),t,(int)(w*0.65),h);
       	}

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	int nrows = 15;  // b.boardRows
        int ncols = 15;	 // b.boardColumns
  	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	

    	int extraW = 0;
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
        int stateH = fh*5/2;
        placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,eyeRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY+stateH,boardW,boardH-stateH);
      	G.SetRect(readyButton,boardX,boardY+stateH*2,buttonW*3/2,buttonW/2);
     	G.SetRect(roundRect,boardX,boardY+boardH-buttonW/2-stateH*2,buttonW*3/2,buttonW/2);
   	
     	G.SetRect(marketRect,boardX+boardW,G.Top(marketRect),G.Right(marketRect)-boardX-boardW,G.Height(marketRect));
     	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW,stateH,goalRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
 	
    }
    /**
     * create all per-player boxes.  Nothing is required, but the standard methods
     * create a player name, clocks, and a box for an avatar.  Standard practice
     * is to include a private "done" box if using a planned seating chart.
     * 
     * The layout manager tries many values for "unitsize" so effectively the rest of
     * the boxes should use it as a standard unit.
     */
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	G.SetRect(chip,	x,	y,	1*unitsize,	1*unitsize);
    	Rectangle bugs = playerBugRects[player];
    	Rectangle goals = playerGoalRects[player];
    	Rectangle score = playerScoreRects[player];
    	Rectangle box =  pl.createRectangularPictureGroup(x+2*unitsize,y,2*unitsize/3);
    	G.SetRect(score,x,y+unitsize,unitsize*2,unitsize);
    	int t = G.Bottom(box);
    	G.SetRect(bugs,x,t,unitsize*5,unitsize*3);
    	G.SetRect(goals,x+unitsize*5,t,unitsize*3,unitsize*3);
    	G.union(box, bugs,goals,score,chip);
    	pl.displayRotation = rotation;
    	return(box);
    }
    

	// draw a box of spare chips. For pushfight it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void drawPlayerBoard(Graphics gc, commonPlayer pl, HitPoint highlight,Hashtable<BugsCell,BugsMovespec> targets,BugsBoard gb)
    {	int player = pl.boardIndex;
    	PlayerBoard pb = gb.getPlayerBoard(pl.boardIndex);
    	
       	if(gb.simultaneousTurnsAllowed()
    			&& HitPoint.setHelpText(highlight,pl.nameRect,"use UI for this player"))
    	{
    		highlight.hitCode = BugsId.Select;
    		highlight.hit_index = player;
    		highlight.hitData = pl;
    		highlight.spriteRect = pl.nameRect;
    		highlight.spriteColor = Color.red;
    	}
       	GC.fillRect(gc,pb.backgroundColor,pl.playerBox);
       	GC.frameRect(gc,Color.black,pl.playerBox);
       	
    	Rectangle r = chipRects[player];
    	Rectangle goal = playerGoalRects[player];
    	Rectangle bug = playerBugRects[player];
    	Rectangle score = playerScoreRects[player];
    	pb.cell.drawChip(gc,this,G.Width(r),G.centerX(r),G.centerY(r),null);
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,true,score, Color.black, null, ""+pb.getScore());
    	drawCellInBox(gc,gb,highlight,targets,pb.bugs,bug,0.5);
    	drawCellInBox(gc,gb,highlight,targets,pb.goals,goal,0.85); 
     }
    public void drawCellInBox(Graphics gc,BugsBoard gb,HitPoint highlight,Hashtable<BugsCell,BugsMovespec> targets,BugsCell bugs,Rectangle r,double aspect)
    {	int w = G.Width(r);
    	int h = G.Height(r);
    	int bugw = w*3/4;
    	int bugh = (int)(bugw*aspect);
    	int n = Math.max(1,bugs.height()-1);
    	double bugXstep = (double)(w-bugw)/(n*bugw);
    	double bugYstep = (double)(h-bugh)/(n*bugw);
    	if(drawSingleCell(gc,gb,highlight,targets.get(bugs),0,bugXstep,bugYstep,bugs,bugw,G.Left(r)+bugw/2,G.Bottom(r)-(int)(bugh*0.50)))
    	{
    		//GC.frameRect(gc,Color.yellow,r);
    	}
    	if(bugs.height()>1)
    	{	int eyeSize = h/5;
    		if(StockArt.Eye.drawChip(gc,this,eyeSize,G.Left(r)+eyeSize/2,G.Top(r)+eyeSize/2,highlight,BugsId.HitCell,"expand"))
    		{
    			highlight.hitData = bugs;
    		}
    	}
    }
    /**
     * return the dynamically adjusted size during an animation.  This allows
     * compensation for things like the zoom level of the board changing after
     * the animation is started.
     */
    //public int activeAnimationSize(Drawable chip,int thissize) 	{ 	 	return(thissize); 	}

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
    	BugsChip.getChip(obj).drawChip(g,this,bb.cellSize(), xp, yp, null);
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
     BugsChip.backgroundTile.image.tileImage(gc, fullRect);   
      drawFixedBoard(gc);
     }
    
    // land here after rotating the board drawing context if appropriate
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	// note, drawing using disB is very important for boards which have different sizes
    	// and which are using copyies of the real board for drawing.
    	// Not so important for boards which are always the same dimensions, but it's always safe.
    	BugsBoard gb = disB(gc);
        boolean reviewBackground = reviewMode()&&!mutable_game_record;
        if(reviewBackground)
        {	 
         BugsChip.backgroundReviewTile.image.tileImage(gc,brect);   
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
	      gb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

	      // draw the tile grid.  The positions are determined by the underlying board
	      // object, and the tile itself if carefully crafted to tile the pushfight board
	      // when drawn this way.  For games with simple graphics, we could use the
	      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
	      // but for more complex graphics with overlapping shadows or stacked
	      // objects, this double loop is useful if you need to control the
	      // order the objects are drawn in.
          int left = G.Left(brect);
          int top = G.Bottom(brect);
          int xsize = gb.cellSize();//((lastRotation?0.80:0.8)*);
          for(Enumeration<BugsCell>cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements(); )
          { //where we draw the grid
        	  BugsCell cell = cells.nextElement();
              BugsChip tile = cell.background;
              int ypos = top - gb.cellToY(cell);
        	  int xpos = left + gb.cellToX(cell);
        	  int thiscol = cell.col;
        	  int thisrow = cell.row;
        	  // double scale[] = TILESCALES[hidx];
        	  //adjustScales(scale,null);		// adjust the tile size/position.  This is used only in development
        	  // to fine tune the board rendering.
        	  //G.print("cell "+CELLSIZE+" "+xsize);
        	  tile.getAltDisplayChip(thiscol*thisrow^thisrow).drawChip(gc,this,xsize,xpos,ypos,null);
        	  //equivalent lower level draw image
        	  // drawImage(gc,tileImages[hidx].image,tileImages[hidx].getScale(), xpos,ypos,gb.CELLSIZE,1.0);
        	  //
	               
	       }       	
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
    	return(super.encodeScreenZone(x,y,p));
    }
    public boolean drawSingleCell(Graphics gc,BugsBoard gb,HitPoint highlight,BugsMovespec m,
    		int step,double dx,double dy,
    		BugsCell cell,
    		int cellSize,int xpos,int ypos)
    {	
    	CellId startHC = highlight==null ? null : highlight.hitCode;
    	boolean val = cell.drawStack(gc,this,highlight,cellSize,xpos,ypos,step,dx,dy,
    					m==null ? BugsChip.NOHIT : BugsChip.NORMAL );
     	if(val)
		{
    	highlight.hitMove = m;
       	int index = Math.max(0,highlight.hit_index);
    	BugsChip chip = cell.chipAtIndex(index);
    	highlight.hitData = chip;
    	highlight.hit_index = index;
    	if(m!=null && highlight.hitCode == cell.rackLocation())
    	{
    	PlayerBoard pb = bb.getPlayerBoard(m.forPlayer);
    	if(chip==null || pb.canPlayCard(cell,chip))
    		{
    		m.chip = chip;
    		highlight.spriteColor = Color.red;
    		int xx = (int)(index*cellSize*dx);
    		int yy = (int)(index*cellSize*dy);
    		highlight.spriteRect = new Rectangle(xpos-cellSize/2+xx,ypos-cellSize/4-yy,cellSize,cellSize/2);
    		}
    	else 
    		{
    		highlight.hitCode = startHC;
    		highlight.spriteColor = null;
    		}
    	}
    	
		}
     	if(eyeRect.isOnNow() && m!=null)
     	{
     		StockArt.SmallO.drawChip(gc,this,cellSize,xpos,ypos,null);
     	}
     	
       	if(cell.height()>0 && cell.player!=null)
    	{
    		cell.player.drawChip(gc,this,cellSize/4,xpos-cellSize/2,ypos-cellSize/10,null);
    		if(cell.rackLocation()==BugsId.BoardLocation && cell.placedInRound==gb.roundNumber)
    		{
    			GC.Text(gc,true,xpos-cellSize/2,ypos-cellSize/3,cellSize/3,cellSize/3, Color.black,null,"+");
    		}
    	}

    	return val;
    }
    
    public boolean drawSingleCell(Graphics gc,HitPoint highlight,BugsMovespec m,
    			BugsCell cell,Rectangle r,
    			int lift,double xstep,double ystep,
    			String msg)
    {
    	if(cell.drawStack(gc,this,r,highlight,lift,xstep,ystep,
    			msg!=null ? msg : m==null ? BugsChip.NOHIT : BugsChip.NORMAL))
    	{	int index = Math.max(0,highlight.hit_index);
    		BugsChip chip = cell.chipAtIndex(index);
    		highlight.hitData = chip;
    		if(m!=null)
    		{
       		m.chip = chip;
    		highlight.hitMove = m;
    		}
    		return true;
    	}
    	return false;
    }
    public boolean drawCellWithRotators(Graphics gc,BugsBoard gb,BugsCell mainCell,BugsCell cell,BugsMovespec m,PlayerBoard pb,HitPoint highlight,int cellSize,int xpos,int ypos)
    {
    	int xleft = (int)(xpos-cellSize*0.1);
    	int xp = cell.height()>=2 ? xleft:xpos;
    	boolean hit = drawSingleCell(gc,gb,highlight,m,0,0.2,0.0,cell,cellSize,xp,ypos);
    	if(cell.topChip()!=null)
    	{	
    		boolean canHit = false;
    		switch(gb.getState())
    		{
    		default: break;
    		case Play:
    		case SequentialPlay:
    			canHit = pb.uiState==UIState.Confirm 
    						&& pb.droppedDest==cell 
    						&& pb.droppedObject!=null
    						&& pb.droppedObject.isBugCard();
    			break;
    		case Puzzle:
    			canHit=true;
    			break;
    		}
    		if(canHit
    				&& (StockArt.SwingCW.drawChip(gc,this,cellSize/3,(int)(xpos+cellSize*0.7),ypos,canHit?highlight:null,BugsId.RotateCCW,null)
    					| StockArt.SwingCCW.drawChip(gc,this,cellSize/3,(int)(xpos-cellSize*0.7),ypos,canHit?highlight:null,BugsId.RotateCW,null)))
    		{
    			highlight.hitObject = mainCell;
    			highlight.hit_index = pb.boardIndex;
     		}
    	}
    	return hit;
    }
    
    public boolean drawBoardCell(Graphics gc,BugsBoard gb,PlayerBoard pb,Rectangle brect,HitPoint highlight,Hashtable<BugsCell,BugsMovespec> targets,BugsCell cell)
    {	boolean some = false;
	    int ypos = G.Bottom(brect) - gb.cellToY(cell);
	    int xpos = G.Left(brect) + gb.cellToX(cell);
	    numberMenu.saveSequenceNumber(cell,xpos,ypos);
    	BugsCell top = cell.above;
    	BugsCell bottom = cell.below;
    	int cellSize = (int)(CELLSIZE*(gb.variation==BugsVariation.bugspiel_parallel ? 2.3 : 1.3));
    	double rotation =Math.PI*cell.rotation/3;
    	Rectangle sr = null;
    	if(highlight!=null) { sr = highlight.spriteRect; highlight.spriteRect = null; }
    	GC.setRotatedContext(gc,xpos,ypos,highlight,rotation);
    	int ya = (int)(cellSize*0.57);
    	try {   	
    	some |= drawCellWithRotators(gc,gb,cell,cell,targets.get(cell),pb,highlight,cellSize,xpos,ypos);
    	some |= drawCellWithRotators(gc,gb,cell,top,targets.get(top),pb,highlight,cellSize,xpos,ypos-ya);
		some |= drawCellWithRotators(gc,gb,cell,bottom,targets.get(bottom),pb,highlight,cellSize,xpos,ypos+ya);
    	}
    	finally {
    	if(highlight!=null)
    	{
    		if(sr==null && highlight.spriteRect!=null)
    		{ Rectangle s2 = G.copy(null,highlight.spriteRect);
    		  G.insetRect(s2,1,1);
    		  GC.frameRect(gc,Color.gray,s2);
    		  G.insetRect(s2,-2,-2);
    		  GC.frameRect(gc,Color.gray,s2);
    		  GC.frameRect(gc,Color.red,highlight.spriteRect);
    		  highlight.spriteRect = sr = null;
    		}	
    	}
  	    GC.unsetRotatedContext(gc,highlight);
  	    if(highlight!=null) { highlight.spriteRect = sr; }
    	}

    	return some;
    }
    
    public BugsChip bigChip = null;
    public BugsCell bigCell = null;
    
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, BugsBoard gb, PlayerBoard pb,Rectangle brect, HitPoint highlight,Hashtable<BugsCell, BugsMovespec> targets)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
     	numberMenu.clearSequenceNumbers();
     	for(BugsCell cell = gb.allCells; cell!=null; cell=cell.next)
          { drawBoardCell(gc,gb,pb,brect,highlight,targets,cell);
          }
     	HitPoint hp = reviewOnly?highlight:null;
     	
      	drawSingleCell(gc,hp,targets.get(gb.bugDiscards),gb.bugDiscards,deckRect,0,0.002,0.000,null); 
      	if(drawSingleCell(gc,hp,targets.get(gb.activeDeck),gb.activeDeck,deckRect,0,0.002,0.001,BugsChip.BACK))
    	{
    		highlight.hitCode = BugsId.HitCell;
    		highlight.hitData = gb.activeDeck;
    		highlight.setHelpText(s.get(BugCardMessage,gb.activeDeck.height()));
    	}
       	
    	drawSingleCell(gc,hp,targets.get(gb.goalDiscards),gb.goalDiscards,goalDeckRect,0,0.001,0.00,null);
       	if(drawSingleCell(gc,hp,targets.get(gb.goalDeck),gb.goalDeck,goalDeckRect,0,0.002,0.001,BugsChip.BACK))
       	{
    		highlight.hitCode = BugsId.HitCell;
    		highlight.hitData = gb.goalDeck;
    		highlight.setHelpText(s.get(BonusCardMessage,gb.goalDeck.height()));
       	}

    	drawMarket(gc,marketCardRect,gb,pb,highlight,targets);
    	drawGoals(gc,goalCardRect,gb,pb,highlight,targets);
    	drawCosts(gc,gb,pb,highlight);
    	switch(gb.variation)
    	{
    	case bugspiel_parallel:
    	case bugspiel_parallel_large:
    		drawReady(gc,readyButton,gb,pb,highlight);
    		if(gb.lastRound())
        	{
        	GC.Text(gc,true,roundRect,Color.black,null,s.get(FinalMessage,gb.roundNumber));	
        	}
        	else
        	{
        	GC.Text(gc,true,roundRect,Color.blue,null,s.get(RoundMessage,gb.roundNumber));
        	}
    		break;
    	case bugspiel_sequential:
    	case bugspiel_sequential_large:
    		break;
    	default: G.Error("Not expecting variation %s",gb.variation);
    	}
    	
    	numberMenu.drawSequenceNumbers(gc,CELLSIZE*2/3,labelFont,labelColor);
    }
    public void drawReady(Graphics gc,Rectangle mRect,BugsBoard gb,PlayerBoard pb,HitPoint highlight)
    {	
    	String msg = null;
    	switch(gb.board_state)
    	{	case Purchase:
    			msg = EndPurchaseMessage;
    			break;
    		case Bonus:
    			msg = EndBonusMessage;
    			break;
    		case Play:
    			msg = EndPlacingMessage;
    			break;
     		default: break;
    	}
    	if(msg!=null)
    	{
   			int pl = getActivePlayerIndex(gb);
			readyButton.setOffText(s.get(msg));
			if(readyButton.show(gc,gb.DoneState(pl) ? null : highlight))
			{
				highlight.hit_index = pl;
			}
			int hs = G.Height(readyButton)/2;
			int xp = G.Left(readyButton)+hs;
			int yp = G.Top(readyButton);
			for(PlayerBoard player : gb.pbs)
			{
				if(player.isReady()) {
					player.cell.drawChip(gc,this,hs,xp,yp,null);
					xp += hs;
					
				}
			}
    	}
    }
    public void drawCosts(Graphics gc,BugsBoard gb,PlayerBoard pb,HitPoint highlight)
    {
    	int w = G.Width(goalCardRect);
    	int h = G.Height(goalCardRect);
       	int nsteps = gb.market.length;
    	int step = h/nsteps;
    	int x = G.Left(goalCardRect);
    	int aw = w+G.Width(marketCardRect);
    	int top = G.Top(goalCardRect);
    	int y = top+(h-step*nsteps)/2;
    	Font f = largeBoldFont();
     	for(int i=0;i<nsteps;i++)
    		{	Rectangle r = new Rectangle(x,y+step*i+step/9,w,step/9);
    			GC.setFont(gc,f);
    			BugsCell c = gb.market[i];
    			GC.Text(gc,true,r,Color.black,null,s.get(CostMessage,c.cost));
    			GC.fillRect(gc,Color.black,x,y+step*i-(int)(step*0.01),aw,(int)(step*0.02));
    		}
    }
    public void drawGoals(Graphics gc,Rectangle mRect,BugsBoard gb,PlayerBoard pb,HitPoint highlight,Hashtable<BugsCell, BugsMovespec> targets)
    {
    	int w = G.Width(mRect);
    	int h = G.Height(mRect);
     	BugsCell cells[] = gb.goals;
       	int nsteps = cells.length;
    	int step = h/nsteps;
    	int x = G.Left(mRect);
    	int y = G.Top(mRect)+(h-step*nsteps)/2;
    	for(int i=0;i<nsteps;i++)
    		{	int yp=y+step*i;
    			int cardH = (int)(w*0.85);
    			int cardOff = (int)(w*0.86);
    			Rectangle r = new Rectangle(x,yp+step-cardOff,(int)(w*0.99),cardH);
    			BugsCell c = cells[i];
    			drawSingleCell(gc,highlight,targets.get(c),c,
    					r,0,0.002,0.001,null);
    			if(isSelected(gb,pb,c))
    			{
    				StockArt.Checkmark.drawChip(gc,this,w/4,x+w/2,yp+step/2,null);
    			}

    		}
    }
    public void drawMarket(Graphics gc,Rectangle mRect,BugsBoard gb,PlayerBoard pb,HitPoint highlight,Hashtable<BugsCell, BugsMovespec> targets)
    {
    	int w = G.Width(mRect);
    	int h = G.Height(mRect);
     	BugsCell cells[] = gb.market;
       	int nsteps = cells.length;
    	int step = h/nsteps;
    	int x = G.Left(mRect);
    	int y = G.Top(mRect)+(h-step*nsteps)/2;
    	for(int i=0;i<nsteps;i++)
    		{	int yp = y+step*i;
    			Rectangle r = new Rectangle(x,yp,w,step);
    			BugsCell c = cells[i];
    			drawSingleCell(gc,highlight,targets.get(c),c,
    					r,0,0.002,0.001,null);
    			if(isSelected(gb,pb,c))
    			{
    				StockArt.Checkmark.drawChip(gc,this,w/4,x+w/2,yp+step/2,null);
     			}
       		}
    }
    public void drawBigCell(Graphics gc,BugsBoard gb,HitPoint hp,Hashtable<BugsCell,BugsMovespec>targets)
    {
    	BugsCell cell = gb.getCell(bigCell);
    	Rectangle r = G.copy(null,marketRect);
    	int w = G.Width(r);
    	int h = G.Height(r);
    	StockArt.Scrim.getImage().drawImage(gc,G.Left(r),G.Top(r),w,h);
    	GC.frameRect(gc,Color.black,r);
    	
    	G.insetRect(r,w/20);	
    	w = G.Width(r);
    	h = G.Height(r);
    	
    	boolean goalCard = cell.topChip().isGoalCard();
    	double aspect = goalCard? 0.65 : 0.5;
    	int nchips = cell.height();
       	double bestFill = 0;
        int bestRows = 1;
        int bestWidth = 0;
        int bestHeight = 0;
        int bestCols = 0;
        int nrows = 1;
        CellId startHC = hp==null? null : hp.hitCode;
    	while(nrows<=nchips)
    	{
    		int ncols = (nchips+nrows-1)/nrows;
    		double width = w/ncols;
    		double height = h/nrows;
    		if(width*aspect<height) { height = width*aspect; } else { width = height/aspect; }
    		if(width*height*nchips>bestFill) 
    			{ bestFill = width*height*nchips; 
    			  bestRows = nrows; 
    			  bestCols = ncols;
    			  bestWidth = (int)width;
    			  bestHeight = (int)height;
    			}
    		nrows++;
    	}
    	BugsMovespec m = targets.get(cell);
    	int spareX = G.Left(r)+(w-bestCols*bestWidth)/2;
    	int Y = G.Top(r)+(h-bestRows*bestHeight)/2;
    	int X = spareX;
    	int col = 0;
    	StockArt.Scrim.getImage().drawImage(gc,X-5,Y-5,bestWidth*bestCols+10,bestHeight*bestRows+10);
    	for(int i=0;i<nchips;i++)
    	{
    		BugsChip ch = cell.chipAtIndex(i);
    		Rectangle r1 = new Rectangle(X,Y-(goalCard?bestWidth/10:0),bestWidth,bestHeight);
    		if(ch.drawChip(gc,this,r1,hp,cell.rackLocation(),m!=null ? BugsChip.NORMAL : BugsChip.NOHIT))
    		{
    			hp.hit_index= i;
    			if(m!=null)
    			{
    			m.chip = ch; 
    	   		PlayerBoard pb = gb.getPlayerBoard(m.forPlayer);
    			if(pb.canPlayCard(cell,ch))
    			{
        			hp.hitMove = m;
    			}
    			else
    			{
    				hp.hitCode = startHC;
    	    		hp.spriteColor = null;
    			}
    			}    			
    		}
    		//GC.frameRect(gc,Color.blue,r1);
    		col++;
    		X+= bestWidth;
    		if(col>=bestCols) { col=0; Y+= bestHeight; X=spareX; }
    	}
      }
    public void drawBigChip(Graphics gc, HitPoint hp)
    {
    	if(bigChip!=null)
    	{	
    		if(bigChip.drawChip(gc,this,boardRect,hp,BugsId.HitChip))
    		{
    			hp.hitCode = BugsId.HitChip;
    			hp.hitData = bigChip;
    			hp.spriteColor = null;
    		}
    	}
    }
    public boolean isSelected(BugsBoard gb,PlayerBoard pb,BugsCell c)
    {
    	return gb.isSelected(pb,c);
    }
    public int getActivePlayerIndex(BugsBoard gb)
    {
    	return (gb.board_state==BugsState.Puzzle)
   		? guiPlayer
   		: reviewOnly ? gb.whoseTurn : getActivePlayer().boardIndex;
    }
    /**
     * draw the main window and things on it.  
     * If gc!=null then actually draw, 
     * If selectPos is not null, then as you draw (or pretend to draw) notice if
     * you are drawing under the current position of the mouse, and if so if you could
     * click there to do something.  Care must be taken to consider if a click really
     * dropoought to be allowed, considering spectator status, use of the scroll controls,
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
       // if direct drawing is in effect disB gets a copy of the board, which should be
       // used for everything called from here.  Also beware that the board structures
       // seen by the user interface are not the same ones as are seen by the execution engine.
       BugsBoard gb = disB(gc);
       //if(gb!=bb) { bb.sameboard(gb); }
       int ap = getActivePlayerIndex(gb);
       BugsState state = gb.board_state;
       boolean moving = hasMovingObject(selectPos);
   	   if(gc!=null)
   		{
   		// note this gets called in the game loop as well as in the display loop
   		// and is pretty expensive, so we shouldn't do it in the mouse-only case
      
       setDisplayParameters(gb,boardRect);
   		}
       // 
       // if it is not our move, we can't click on the board or related supplies.
       // we accomplish this by suppressing the highlight pointer.
       //
   	   boolean ourmove = OurMove();
       HitPoint ourTurnSelect = ourmove ? selectPos : null;
       //
       // even if we can normally select things, if we have already got a piece
       // moving, we don't want to hit some things, such as the vcr group
       //
       HitPoint buttonSelect = moving ? null : ourTurnSelect;
       // hit any time nothing is being moved, even if not our turn or we are a spectator
       HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;

       // for a multiplayer game, this would likely be redrawGameLog2
       gameLog.redrawGameLog2(gc, nonDragSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),standardBoldFont());

       // this does most of the work, but other functions also use contextRotation to rotate
       // animations and sprites.
       
       boolean overlay = bigChip!=null || bigCell!=null;
       
       Hashtable<BugsCell, BugsMovespec> targets = 
    		   (overlay||!ourmove)
    		   			? new Hashtable<BugsCell,BugsMovespec>() 
    		   			: gb.getTargets(ap);
       PlayerBoard pb = gb.getPlayerBoard(ap);
       drawBoardElements(gc, gb, pb,boardRect, selectPos,targets);
       
       boolean planned = plannedSeating();
       for(int player=0;player<gb.players_in_game;player++)
       	{ commonPlayer pl = getPlayerOrTemp(player);
       	  pl.setRotatedContext(gc, selectPos,false);
    	   drawPlayerBoard(gc, pl, selectPos,targets,gb);
    	   pl.setRotatedContext(gc, selectPos,true);
       	}
        
       if(bigCell!=null)
       {
    	   drawBigCell(gc,gb,selectPos,gb.getTargets(ap));
       }
       if(bigChip!=null)
       {
    	   drawBigChip(gc,selectPos);
       }
       GC.setFont(gc,standardBoldFont());
       boolean doneState = gb.DoneState(getActivePlayerIndex(gb));
       if (state != BugsState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive())
				{
				if(doneButton.show(gc,doneState ? buttonSelect : null))
				{
					buttonSelect.hit_index = ap;
				}
				}
			handleEditButton(gc,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==BugsState.Puzzle),selectPos,HighlightColor,rackBackGroundColor);
 
        // draw the avatars
        standardGameMessage(gc,
        					// note that gameOverMessage() is also put into the game record
            				state==BugsState.Gameover
            						? gameOverMessage(gb)
            						: doneState 
            							? s.get(ConfirmDescription) 
            							: s.get(state.description()),
            				state!=BugsState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        gb.getPlayerChip(gb.whoseTurn).drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
        if(state==BugsState.SequentialPlay)
        {
        	passButton.draw(gc,selectPos);
        }
            //      DrawRepRect(gc,pl.displayRotation,Color.black,b.Digest(),repRect);
        eyeRect.activateOnMouse = true;
        eyeRect.draw(gc,selectPos);
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);

    }

    /**
     * normally, no moves should be transmitted during in-game review.  This
     * allows an override for particular moves. Presumably moves that only
     * affect the global state, not the particular board position.  
     */
    public boolean canSendAnyTime(commonMove m)
    {
    	return super.canSendAnyTime(m);
    			//|| (m.op==MOVE_SHOW);
    }
    
    /**
     * perform and optionally transmit a move, return true if ok.  Note, it's tempting
     * to do any "auto move" that is needed in the continuation of this method, but don't.
     * Doing so may separate the "perform" from the "transmit" which will lead to other
     * players seeing the events out of order.  Instead, use the continuation of ViewerRun
     * @param m
     * @param transmit
	 * @param replay replay mode
     * @return true if successful
     */
    public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode replay)
    {
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
   		 if(mm.op==MOVE_RESET && simultaneousTurnsAllowed())
    	 {
    		BugsMovespec m = (BugsMovespec)mm;
    		PlayerBoard pb = bb.getPlayerBoard(m.forPlayer);
    		if(pb.pickedObject!=null)
    		{	BugsMovespec m2 = new BugsMovespec(MOVE_DROP,pb.pickedSource,pb.pickedObject,pb.pickedSource,m.forPlayer);
    			PerformAndTransmit(m2,true,replayMode.Live);
    		}
    		return true;
    	 }

        handleExecute(bb,mm,replay);
        
        
        // movenumber() vs activeMoveNumber().  ActiveMoveNumber causes the 
        // move arrows to disappear when the next move has started.  For games with from-to or more
        // complicated moves, the persistence of the arrows is annoying.  for games like Go
        // and Hex, where the stones are placed and not moved, it's more innocuous
        numberMenu.recordSequenceNumber(bb.moveNumber());
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
        return (new BugsMovespec(st, pl));
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
 */
      public commonMove EditHistory(commonMove nmove)
      {	  // some damaged games ended up with naked "drop", this lets them pass 
    	  boolean oknone = (nmove.op==MOVE_DROP);
    	  commonMove rval = EditHistory(nmove,oknone);
     	     
    	  return(rval);
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
    {	//DISABLE_VERIFY=false;
    	super.verifyGameRecord();
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
        if (hp.hitCode instanceof BugsId)// not dragging anything yet, so maybe start
        {
        BugsId hitCode =  (BugsId)hp.hitCode;
 	    switch(hitCode)
	    {
	    default: break;
	    case BugMarket:
	    case ActiveDeck:
 	    case GoalMarket:
 	    case BugCard:
 	    case PlayerBugs:
 	    case PlayerGoals:
 	    case BoardLocation:
 	    case BoardTopLocation:
 	    case BoardBottomLocation:
 	    	{
 	    	bigChip = null;
 	        bigCell = null;

 	    	BugsMovespec m = (BugsMovespec)hp.hitMove;
 	    	PerformAndTransmit(m.moveString());
 	    	PlayerBoard pb = bb.getPlayerBoard(m.forPlayer);
 	    	hp.dragging = pb.pickedObject!=null;
 	    	}
	    	break;
        } 
        }
    }
	  public boolean allowOpponentUndoNow() 
	  {
		  return super.allowOpponentUndoNow();
	  }
	  public boolean allowOpponentUndo() 
	  {
		  return super.allowOpponentUndo();
	  }
	  
	  public boolean allowUndo()
	  {		return super.allowUndo();
	  }
	/**
	 * this is the key to limiting "runaway undo" in situations where the player
	 * might have made a lot of moves, and undo should limit the damage.  One
	 * example of this is in perliminary setup such as arimaa or iro
	 */
	public boolean allowPartialUndo()
	{
		return super.allowPartialUndo();
	}
	 /**
	  * this is called when the user clicks with no effect a few times, and is intended to 
	  * put him into an un-confused state.  Normally this is equivalient to an undo, but
	  * in games with complex setups, something else might be appropriate
	  */
	 public void performReset()
	    {
	    	super.performReset();
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
       	if(!(id instanceof BugsId))  
       		{   missedOneClick = performStandardActions(hp,missedOneClick);  
       		    bigCell = null;
       		    bigChip = null;
       		}
        else {
        missedOneClick = false;
        BugsId hitCode = (BugsId)id;
        
        // if direct drawing, hp.hitObject is a cell from a copy of the board
        BugsCell hitObject = bb.getCell(hitCell(hp));
        switch (hitCode)
        {
        default:
        	if (performStandardButtons(hitCode, hp)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " + hitObject);
            }
        	break;
        case HitCell:
        	bigChip = null;
        	bigCell = bigCell==null ? (BugsCell)hp.hitData : null;
        	break;
        case HitChip:
        	{
        	BugsChip ch = (BugsChip)hp.hitData;
        	if(bigChip==ch) { bigChip=null; } else { bigChip = ch; }
        	}
        	break;
        case DoneButton:
        	PerformAndTransmit("Done P"+hp.hit_index);
        	break;
        case Ready:
        	PerformAndTransmit("Ready P"+hp.hit_index);
        	break;
        case Select:
   			PerformAndTransmit("SetActive P"+hp.hit_index);
   			guiPlayer = hp.hit_index;
   			break;
   			
        case RotateCW:
        	PerformAndTransmit(G.concat("RotateCW P",hp.hit_index," ",hitObject.col," ",hitObject.row));
        	break;
        case RotateCCW:
        	PerformAndTransmit(G.concat("RotateCCW P",hp.hit_index," ",hitObject.col," ",hitObject.row));
        	break;
        case ToggleEye:
        	eyeRect.toggle();
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
        case BoardTopLocation:
        case BoardBottomLocation:
        case BugCard:
        case PlayerGoals:
        case PlayerBugs:
        case BugMarket:
        case GoalMarket:
        case ActiveDeck:
        case GoalDeck:
        	{
        	BugsMovespec m = (BugsMovespec)hp.hitMove;
        	guiPlayer = m.forPlayer;
        	bigChip = null;
        	bigCell = null;
        	if(m.op!=MOVE_SELECT) { PerformAndTransmit(m.moveString()); }
        	}
           break;
 
        }
        }
    }

    private double cellMultiplier(BugsBoard gb)
    {
    	switch(gb.variation)
    	{
    	case bugspiel_parallel: return 1.2;
    	default: return 1.0;
    	}
    }

    private boolean setDisplayParameters(BugsBoard gb,Rectangle r)
    {
      	boolean complete = false;
        gb.SetDisplayParameters(cellMultiplier(gb), 1.0, 0.3,-0.2,60); // shrink a little and rotate 60 degrees
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
    	return(bb.gameType()); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Prototype_SGF); }	// this is the official SGF number assigned to the game

   
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

     public static String bugdeck = "g:/share/projects/boardspace-html/htdocs/deck/";

    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
        if(target==makeDeck)
        {	handled=true;
        	String html = bugdeck+"deck.html";
        	try {
        	PrintStream out = new PrintStream(new FileOutputStream(html));
        	out.print("<h1><cener>Prototype BugSpiel Card Deck</center></h1>\n");
        	out.print("<nl><center>send corrections and suggestions to bugspiel@boardspace.net</center><nl>\n");
        	int nBugs = BugCard.bugCount();
        	BugsChip[] ar = new BugsChip[nBugs];
        	for(int i=0;i<nBugs;i++) { ar[i] = BugCard.getCard(i); }
        	Taxonomy group = null;
        	Sort.sort(ar);
        	for(int i=0;i<ar.length;i++)
        	{
        		BugCard ch = (BugCard)ar[i];
        		Image im = ch.makeCardImage(this,800,400);
        		Image im2 = ch.makeCardImage(this,200,100);
        		File f = new File(im.getName());
        		Profile profile = ch.getProfile();
        		Taxonomy newgroup = profile.getCategory();
        		if(newgroup!=null && newgroup!=group)
        		{	group = newgroup;
        			out.print("<center><h3>Group "+newgroup.getCommonName()+" ("+newgroup.getScientificName()+")</h3></center><nl>\n");
        		}
        		String baseName = f.getName()+".png";
        		String smallName = f.getName()+"-small.png";
				String imname = bugdeck+baseName;
				String imnameSmall = bugdeck+smallName;
				String alt = "\""+ch.getCommonName()+" ("+ch.getScientificName()+")\"";
				out.print("<a href=\""+baseName+"\"><image src=\""+smallName+"\" alt="+alt+"title="+alt+">\n</a>");
				if(i%5==4) { out.println("<nl><nl>"); }
				im.saveImage(imname);
				im2.saveImage(imnameSmall);
				im.discard();
				if(i%100==0) { Image.unloadRegisteredImages(); }
        	}
        	out.close();
        	}
        	catch(IOException e)
        	{
        		G.Error("making deck failed "+e);
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
 * 
 * Another thing ViewerRun typically does is provide "auto-move" for when
 * conditions are right - typically at the end of a simultaneous movement
 * phase.  Be careful, this interacts with "undo".  Typically solved by
 * extending allowBackwardStep() and allowPartialUndo() to go back a step further
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


/** this is used by the scorekeeper to determine who won. Draws are indicated
 * by both players returning false.  Be careful not to let both players return true!
 */
   // public boolean WinForPlayer(commonPlayer p)
   // { // this is what the standard method does
      // return(getBoard().WinForPlayer(p.index));
   //   return (super.WinForPlayer(p));
   // }

    /** start the robot.  This is used to invoke a robot player.  Mainly this needs 
     * to know the class for the robot and any initialization it requires.  The return
     * value is the player actually started, which is normally the same as requested,
     * but might be different in some games, notably simultaneous play games like Raj
     *  */
    public commonPlayer startRobot(commonPlayer p,commonPlayer runner,Bot bot)
    {
    	return super.startRobot(p,runner,bot);
    }
   // public commonPlayer startRobot(commonPlayer p,commonPlayer runner,Bot bot)
   // {	// this is what the standard method does:
    	// int level = sharedInfo.getInt(sharedInfo.ROBOTLEVEL,0);
    	// RobotProtocol rr = newRobotPlayer();
    	// rr.InitRobot(sharedInfo, getBoard(), null, level);
    	// p.startRobot(rr);
    //	return(super.startRobot(p,runner,bot));
    //}
    // this is conventionally used as the game state message above the board,
    // but it is also inserted into the game record as the RE property
    //
    //public String gameOverMessage()
    //{
    //	return super.gameOverMessage();
    //}
    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new BugsPlay());
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
    /**
     *****		 Simultaneous moves support *****
     * 
     * Games where all the players move at the same time cause a lot of problems, but
     * it's the right thing to do in some cases.  The current games that use at least
     * some simultaneous moves are:
     *  Euphoria, Magnet, Raj, One Day In London, Q.E., Breaking Away, Tammany Hall, and Imagine
     * 
     * The root of the problem is that players WILL disagree about the order of arrival of 
     * asynchronous moves. This causes their game records to be different from one another,
     * and causes the server's incremental bookkeeping of the game state to be seen as
     * inconsistent.
     * 
     * The overall strategy for dealing with his problem is to segregate the simultaneous
     * moves to the end of the game record, and anchor the server's record of the game at
     * the beginning of the ephemeral block.   At appropriate points, the ephemeral moves
     * are replaced in the game record by equivalent non-ephemeral moves in canonical order.
     * 
     * 
     * Another problem is time accounting - while in simultaneous mode, the clocks for all 
     * the players who have not completed their move should tick.
     * 
     * Tendrils to support this are all over the place, which made support difficult; but
     * the most common solutions are embodied in requirements for move specs, states, and
     * in overridable methods of commonCanvas 
     * 
     * the xxMovespec class will use pairs of ephemeral/normal moves, and implement
     * and board states which allow asynchronous moves will be distinct from the states
     * that do not.
     *  
     * Reviews of games which originally had simultaneous moves will have all synchronous
     * moves, so the game engine ought to function perfectly with any ephemeral moves.
     * 
     *  
     */
    /* for debugging and initial development, leave this false */
    boolean SIMULTANEOUS_PLAY = false;		
    /** true if this game ever has simultaneous moves.  If true, formEphemeralHistoryString
     * will do a second pass to collect the ephemeral moves, and {@link #useEphemeraBuffer} will
     * expect there to be one.
     */
    public boolean gameHasEphemeralMoves() { return(SIMULTANEOUS_PLAY); }
    // these related methods can be wrapped or overridden to customize the behavior of the ephemeral part game records.
    //
    // public String formEphemeralHistoryString()
    // public void useEphemeraBuffer(StringTokenizer h)
    // public String formEphemeralMoveString() {} 
    // public void useEphemeralMoves(StringTokenizer his) {}
    // -- the top level --
    // public void useStoryBuffer(String tok,StringTokenizer his) {}
    // public void formHistoryString(PrintStream os,boolean includeTimes) {}

    /**
     * call this at appropriate times to convert ephemeral moves to their
     * non ephemeral equivalents.  Usually, only the {@link #convertToSynchronous }
     */
    public void canonicalizeHistory()
    {
    	super.canonicalizeHistory();
    }
    
    /**
     * sort the ephemeral moves into their final order.  Normally is is
     * just ordering the moves so all of each players moves are together.
     * 
     * @param ephemera
     */
    public void ephemeralSort(CommonMoveStack ephemera)
    {
    	super.ephemeralSort(ephemera);
    }
    /**
     * convert an ephemeral move to it's no-ephemeral equivalent.  It's also
     * ok to return null meaning the move should be deleted.  Normally, all
     * this will do is change the m.op field, but it needs to agree with 
     * the behavior of movespec {@link commonMove#isEphemeral} method.
     */
    public commonMove convertToSynchronous(commonMove m)
    {	throw G.Error("Not implemented");
    }
    
    // used by the UI to alter the behavior of clocks and prompts
    public boolean simultaneousTurnsAllowed()
    {	return super.simultaneousTurnsAllowed();
    }
  // public RecordingStrategy gameRecordingMode()
  //  {	return(super.gameRecordingMode());
  //  }
    private String vprogressString()
    {	return super.gameProgressString()+" score score";
    }
    public String gameProgressString()
    {	// this is what the standard method does
    	 return(mutable_game_record 
    			? Reviewing
    			: vprogressString());
    }

    //
    // support for the last move "numberMenu" logic
    //
	public int getLastPlacement(boolean empty) {
		return (bb.moveNumber);
	}
	 
	/** this is a debugging interface to provide information about memory
	 * consumption by images.
	 */
	public double imageSize(ImageStack im)
	  {
		  return(super.imageSize(im) + BugsChip.imageSize(im));
	  }
	
    int guiPlayer = 0;
    public UIState getUIState(BugsBoard gb)
    {	boolean sim = gb.getState().simultaneousTurnsAllowed();
    	PlayerBoard pb = gb.getPlayerBoard(sim ? guiPlayer : gb.whoseTurn);
    	return pb.uiState;
    }
    public void adjustPlayers(int n)
    {
    	super.adjustPlayers(n);
    	guiPlayer = Math.min(n-1,guiPlayer);
    }
	  public boolean allowResetUndo()
	  {
		  if(simultaneousTurnsAllowed())
		  {
		    	int who = reviewOnly 
	    				? simultaneousTurnsAllowed()?guiPlayer:bb.whoseTurn 
	    				: getActivePlayer().boardIndex;
		    	PlayerBoard pb = bb.getPlayerBoard(who);
		    	if(pb.pickedObject!=null) 
		    		{ return true; }
		  }
		  return super.allowResetUndo();
	  }
	  public int getOurMovingObject(HitPoint highlight)
	    {
	        if (OurMove())
	        {
	            return (simultaneousTurnsAllowed() 
	            		? bb.movingObjectIndex(reviewOnly ? guiPlayer : getActivePlayer().boardIndex) 
	            		: bb.movingObjectIndex());
	        }
	       
	        return (NothingMoving);
	    }  
	  
	    public void startPlaying()
	    {	super.startPlaying();
	    	guiPlayer = getActivePlayer().boardIndex;
			setGameRecorder();
	    }
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
	    public void setLimbo(boolean v)
	    {
	    	boolean oldLimbo = inLimbo;
	    	super.setLimbo(v);
	    	if(oldLimbo!=inLimbo)
	    	{
	    		setGameRecorder();
	    	}
	    }
	    public boolean iAmTheLeadPlayer()
	    {
	    	return getActivePlayer().boardIndex==gameRecorder;
	    }
	    public RecordingStrategy gameRecordingMode()
	    {
	    	return iAmTheLeadPlayer() ? RecordingStrategy.Single : RecordingStrategy.None;
	    }
	    public boolean allowRobotsToRun(commonPlayer pp)
	    {
	    	if(simultaneousTurnsAllowed())
	    	{
	    		PlayerBoard pb = bb.getPlayerBoard(pp.boardIndex);
	    		switch(pb.uiState)
	    		{
	    			case Ready:
	    				return false;
	    			default: break;
	    		}
	    	}
	    	return true;
	    }

    /**
     * colorize a string and return a Text with the result.  This is used
     * to substitute icons for words, or translate the words, in the string;
     * or to otherwise change the presentation of the string to something other than a plain 
     * fonted string.  See {@link lib.Text#colorize}
     * This is used by redrawGameLog to transform raw strings from game events.  In conjunction
     * with iconification done by "shortMoveString", this is responsible for making pretty
     * pictures in the game log.
     * @param str
     * @return the new Text object
     */
    //public Text colorize(String str)
    //{	return(TextChunk.create(str));
    //}
    
}

