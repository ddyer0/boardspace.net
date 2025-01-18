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
package gametimer;


import common.GameInfo;

import static gametimer.GameTimerMovespec.*;

import com.codename1.ui.geom.Rectangle;

import bridge.Color;

import online.common.*;
import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

/**
 * GameTimer is a special non-game game, which only operates as a timer.
 * This is intended to allow using your phone as a game clock in offline
 * tournaments.
 */
public class GameTimerViewer extends CCanvas<cell<?>,GameTimerBoard> implements GameTimerConstants, PlacementProvider
{		// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
    static final String Gametimer_SGF = "gametimer"; // sgf game name

    // file names for jpeg images and masks
    static final String ImageDir = "/gametimer/images/";

     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(150,150,150);
    private Rectangle pauseRect = addRect("pause");
    private Rectangle endgameRect = addRect("endgame");
    public Rectangle chipRects[] = addRect("chip",6);
     
    // private state
    private GameTimerBoard bb = null; //the board from which we are displaying
 
    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	GameTimerChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = GameTimerChip.Icon.image;
    }

 
	/**
	 * 
	 * this is the real instance initialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	info.putBoolean(NOCHAT,true);
    	info.putBoolean(DONOTSAVE,true);
    	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	int players_in_game = info.getInt(OnlineConstants.PLAYERS_IN_GAME,2);
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default
        
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	GameTimerConstants.putStrings();
        }
         
        
        String type = info.getString(GameInfo.GAMETYPE, "timer");
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new GameTimerBoard(type,players_in_game,randomKey,getStartingColorMap(),GameTimerBoard.REVISION);
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
 	 	  PerformAndTransmit("edit", false,replayMode.Replay);
    	}
    }
    boolean prealloc = false;
    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int margin = fh/2;
        int buttonW = fh*12;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0,	// 60% of space allocated to the board
    			1,	// aspect ratio for the board
    			fh*3,	// minimum cell size
    			fh*20,	// maximum cell size (can be giant!)
    			0.05		// preference for the designated layout, if any
    			);
    	Rectangle peek = layout.peekMainRectangle();
    	int pw = G.Width(peek);
    	int ph = G.Height(peek);
    	int dim = Math.max(pw,ph);
    	prealloc = dim<buttonW*6;
    	// two different strategies.  If the "board" rectangle isn't shrunk too much 
    	// by the giant player areas, allocate just a single line and partition it into
    	// a few boxes.  For some layouts, the board shrinks to nothing, so allocate the
    	// boxes using the standard methods.
    	if(prealloc)
    	{
    		layout.placeRectangle(pauseRect,buttonW,buttonW/2,BoxAlignment.Center);
    		layout.placeRectangle(endgameRect,buttonW,buttonW/2,BoxAlignment.Center);
    	}
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	int goalH = fh*4;
 
    	boolean rotate = mainW<buttonW*5 && mainW<mainH;
    	// note this doesn't respect the layout of the other rectangles
    	G.SetRect(boardRect,mainX,mainY,mainW,mainH);
    	
    	if(rotate)
    	{	// this conspires to rotate the drawing of the board
    		// and contents if the players are sitting opposite
    		// on the short side of the screen.
    		G.setRotation(boardRect,-Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
    	int boardY = G.centerY(boardRect)-goalH/2;    
     	placeRow(G.Left(boardRect)+buttonW/2,boardY,G.Width(boardRect)-buttonW,goalH,goalRect);
     	if(!prealloc)
     	{
       	G.splitLeft(goalRect,pauseRect,buttonW);
       	G.splitRight(goalRect,endgameRect,buttonW);
    	}
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
      	Rectangle box =  pl.createRectangularPictureGroup(x,y,unitsize);
      	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	int doneW = G.Width(box)-unitsize;
    	int doneH = doneW/2;
    	G.copy(chip,pl.picRect);	// make the picrect zero size and use it for a chip rect
    	G.SetHeight(pl.picRect,0);
    	G.SetWidth(pl.picRect,0);
    	G.SetRect(done,x+unitsize/2,G.Bottom(box)+unitsize,doneW,doneH);
    	G.union(box, done);
    	pl.displayRotation = rotation;
    	return(box);
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
    	G.Error("Not expected");
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
     GameTimerChip.backgroundTile.image.tileImage(gc, fullRect);   
      drawFixedBoard(gc);
     }
    
    // land here after rotating the board drawing context if appropriate
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	return;
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
       // if direct drawing is in effect disB gets a copy of the board, which should be
       // used for everything called from here.  Also beware that the board structures
       // seen by the user interface are not the same ones as are seen by the execution engine.
       GameTimerBoard gb = disB(gc);
       GameTimerState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
        // 
       // if it is not our move, we can't click on the board or related supplies.
       // we accomplish this by suppressing the highlight pointer.
       //
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;
       //
       // even if we can normally select things, if we have already got a piece
       // moving, we don't want to hit some things, such as the vcr group
       //
       HitPoint buttonSelect = moving ? null : ourTurnSelect;
       // hit any time nothing is being moved, even if not our turn or we are a spectator

       int whoseTurn = gb.whoseTurn;
       GC.setFont(gc,largeBoldFont());
       for(int player=0;player<gb.players_in_game;player++)
       	{ commonPlayer pl = getPlayerOrTemp(player);
       	  pl.setRotatedContext(gc, selectPos,false);
    	   if((nPlayers()==2) && (whoseTurn==player))
    	   {	// 2 players flip-flop
    		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
    	   else if(nPlayers()>2) 
    	   {
    		  if(GC.handleRoundButton(gc, 0 ,doneRects[player],buttonSelect,s.get("Start"),
    				  HighlightColor, whoseTurn==player ? HighlightColor : bsBlue))
    		  {	  buttonSelect.hitCode = GameTimerId.SetPlayer;
    		  	  buttonSelect.hit_index = player;
    			  HitPoint.setHelpText(buttonSelect,doneRects[player],s.get("ExplainStart"));
    		  }
    	   }
    	   GameInfo info = gameInfo;
    	   if(info!=null)
    	   {   Rectangle chip = chipRects[player];
    	   	   int map[] = bb.getColorMap();
    		   Color c = info.colorMap[map[player]];
    		   int w = G.Width(chip)/4;
    		   int cx = G.centerX(chip);
    		   int cy = G.centerY(chip);
    		   GC.setColor(gc,c);
    		   GC.DrawAACircle(gc,cx,cy,w,c,rackBackGroundColor,true);
    	   }
       	   pl.setRotatedContext(gc, selectPos,true);
       	}
       commonPlayer pl = getPlayerOrTemp(whoseTurn);
       double messageRotation = pl.messageRotation();
       
       
       GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);

       GC.setFont(gc,largeBoldFont());
       String timeMessage = timeControl().timeControlMessage();
       GC.Text(gc,messageRotation,true,goalRect,Color.black,null,timeMessage);
       if(prealloc) { GC.unsetRotatedContext(gc,selectPos); }
       
		String message = null;
		GameTimerId code = null;;
		switch(state)
		{
		case Paused: 
				message = s.get(ResumeMessage);
				code = GameTimerId.Resume;
				if(GC.handleSquareButton(gc,messageRotation,endgameRect,selectPos,s.get(EndGameMessage),
						HighlightColor, rackBackGroundColor)
						)
				{
					selectPos.hitCode = GameTimerId.Endgame;
				}
				break;
		case Running:
				message = s.get(PauseMessage);
				code = GameTimerId.Pause;
				break;
		default: break;
		}
		if(message!=null)
		{
			if(GC.handleSquareButton(gc, messageRotation, pauseRect, selectPos, message,
					HighlightColor, rackBackGroundColor))
			{
				selectPos.hitCode = code;
			}
		}
		if(!prealloc) { GC.unsetRotatedContext(gc,selectPos); }
		
		

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
		drawPlayerStuff(gc,(state==GameTimerState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
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
        
		if(replay.animate) { playSounds(mm); }
       return (true);
    }

 void playSounds(commonMove mm)
 {
	 switch(mm.op)
	 {
	 case MOVE_DONE:
	 case MOVE_PAUSE:
	 case MOVE_SETPLAYER:
	 case MOVE_RESUME:
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
        return (new GameTimerMovespec(st, pl));
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
 
    }


	 public boolean timersActive()
	 {
		 return (super.timersActive() && (bb.getState()!=GameTimerState.Paused));
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
       	if(!(id instanceof GameTimerId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        GameTimerId hitCode = (GameTimerId)id;
        switch (hitCode)
        {
        case Endgame:
        case Pause:
        case Resume:
        	PerformAndTransmit(hitCode.toString());
        	break;
        case SetPlayer:
        	PerformAndTransmit(hitCode.toString()+" P"+hp.hit_index);
        	break;
        default:
        	if (performStandardButtons(hitCode, hp)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " + hitCode);
            }
        	break;
        }
        }
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
    	return(bb.gameType()); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Gametimer_SGF); }	// this is the official SGF number assigned to the game

   
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


    public void ViewerRun(int wait)
      {	   // reduce the cycle time so the timers tick regularly
           super.ViewerRun(Math.min(wait,250));
      }

    
    /** this is used by the stock parts of the canvas machinery to get 
     * access to the default board object.
     */
    public BoardProtocol getBoard()   {    return (bb);   }


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

	public SimpleRobotProtocol newRobotPlayer() {
		return null;
    }

}

