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
package palabra;
/**
 * The unusual feature of Raj is simultaneous play, and no "done" moves.
 */

import java.awt.*;

import online.common.*;

import javax.swing.JMenuItem;

import common.GameInfo;
import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.DefaultId;
import lib.ExtendedHashtable;
import lib.FontManager;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Text;
import lib.TextChunk;
import lib.Tokenizer;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

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

public class PalabraViewer extends CCanvas<PalabraCell,PalabraBoard> implements PalabraConstants
{	static final long serialVersionUID = 1000;


	//if true, run the UI with simultaneous plays. Otherwise, run the moves serially
	//like most of the other games.  Changing to false is intended for debugging only.
	static boolean SIMULTANEOUS_PLAY = true;
	private Dictionary dictionary = new Dictionary();
     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackgroundColor = new Color(248,252,238);
    private Color rackBackGroundColor = new Color(178,212,168);
    private Color boardBackgroundColor = new Color(178,212,168);
    
    Font cardDeckFont = FontManager.getFont("Dialog", FontManager.Style.Bold, 25);
    // images, shared among all instances of the class so loaded only once
    private static Image[] textures = null;// background textures
    JMenuItem printButton = null;
    // private state
    private PalabraBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //
      Rectangle prizeRect = addZoneRect("prizeRect");
 
    private Rectangle playerRects[] = addRect("P0",6);
    public Color rajMouseColors[] = 
    {	PalabraColor.Red.color,
    	PalabraColor.Green.color,
    	PalabraColor.Blue.color,
    	PalabraColor.Brown.color,
    	PalabraColor.Purple.color
    };
    public Color rajDotColors[] = 
    {	Color.white,Color.white,Color.white,Color.white,Color.black
    };
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	PalabraChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	if (textures == null)
    	{ 	// note that for this to work correctly, the images and masks must be the same size.  
        	// Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    		
    		// images and textures are static variables, so they're shared by
    		// the entire class and only get loaded once.  Special synchronization
    		// tricks are used to make sure this is true.
    		
    	  // load the tiles used to construct the board as stock art

          textures = loader.load_images(ImageDir,TextureNames);
          dictionary.load();

    	}
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
        int np = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
         
        bb = new PalabraBoard(info.getString(GameInfo.GAMETYPE, Palabra_init),randomKey,np);
        
        printButton = myFrame.addAction("Print deck",deferredEvents);
        useDirectDrawing(true); // not tested yet
        doInit(false);

    }
    public boolean handleDeferredEvent(Object target, String command)
    {
    	if(target==printButton)
    	{	PalabraChip.printDeck(this,"g:/temp/gamecraftercards/",PalabraChip.gamecrafterDims);
       		PalabraChip.printDeck(this,"g:/temp/makeplayingcards/",PalabraChip.makeplayingcardsDims);
       		PalabraChip.printMasterDeck(this,"g:/temp/printplaycards/",true,
       				PalabraChip.gamecrafterDims,
       				PalabraChip.printplaygamesDims,PalabraChip.printplaygamesInsideMargins);
       		PalabraChip.printMasterDeck(this,"g:/temp/superiorpodcards/",true,
       				PalabraChip.gamecrafterDims,
       				PalabraChip.superiorpodDims,PalabraChip.superiorpodInsideMargins);
       	    		
    		return(true);
    	}
    	else {
    		return(super.handleDeferredEvent(target,command));
    	}
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
    			fh*3,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
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
    	boolean rotate = mainW<mainH;	
        int nrows = rotate ? 24 : 15;  // b.boardRows
        int ncols = rotate ? 15 : 24;	 // b.boardColumns
  	
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
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        int stateH = fh*5/2;
        placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	if(rotate)
    	{	// this conspires to rotate the drawing of the board
    		// and contents if the players are sitting opposite
    		// on the short side of the screen.
    		G.setRotation(boardRect,-Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW,stateH,goalRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
 	
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = playerRects[player];
    	G.SetRect(chip,	x,	y,	2*unitsize,	3*unitsize);
    	Rectangle box =  pl.createRectangularPictureGroup(x+2*unitsize,y,2*unitsize/3);
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*3 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.union(box, done,chip);
    	pl.displayRotation = rotation;
    	return(box);
    }

    //
    // draw the numbered side of a card.
    //
    private void drawCardFace(Graphics gc,Rectangle r,PalabraChip ch)
    {	int xp = G.centerX(r);
		int yp = G.centerY(r);
		int hscale = G.Width(r);
		ch.drawChip(gc,this,hscale,xp,yp,null);
    }

    public Text censoredMoveText(SequenceElement sp,int index,Font f)
     {
    	commonMove last = History.top();
    	String mv = last.getSliderNumString();
    	String spnum = sp.getSliderNumString();
    	boolean censor = mv.equals(spnum) && (((commonMove)sp).op!=MOVE_AWARD);
    	Palabramovespec spc = (Palabramovespec)sp;
    	return(TextChunk.create(spc.shortMoveString(censor)));
     }

    private int movingObjectIndex()
    { 	return(bb.movingObjectIndex()); 
    }
    private void drawPlayerBoard(Graphics gc,int pl,HitPoint highlight,PalabraBoard gb,HitPoint anyHit)
    {	PalabraBoard.PlayerBoard pb = gb.playerBoards[pl];
    	Rectangle r = playerRects[pl];
    	boolean left = G.Left(r) < 2*G.Width(fullRect)/3;
    	int mid = G.centerY(r);
    	int step = (-G.Width(r)/5);
    	int cx = (left ? G.Right(r)+step: G.Left(r)-step);
    	PalabraCell played = pb.playedCards;
    	PalabraCell unplayed = pb.cards;
        boolean canHitPlayedCards = gb.LegalToHitCards(played);    
        //StockArt.SmallO.drawChip(gc,this,CELLSIZE*2,r.x,mid-step,null);
    	if(played.drawHStack(gc,this,
    				canHitPlayedCards?highlight:null,CELLSIZE*2,
    				G.Left(r)+(left?CELLSIZE:CELLSIZE*3),mid-step,0,0.07,null))
    	{	highlight.setHelpText(s.get(UsedCardsMessage));
    		highlight.hit_y = -1;
    		highlight.arrow = (movingObjectIndex()<0)?StockArt.UpArrow:StockArt.DownArrow;
    		highlight.awidth = CELLSIZE;
   		
    	}

     	{	
     		int cy = mid+CELLSIZE+CELLSIZE/3;
     		int hscale = CELLSIZE*2;
     		int stackHeight = unplayed.height();
     		double vscale = 1.0/15+0.5/Math.max(8,stackHeight);
     		double vsize = hscale+(stackHeight*vscale)*CELLSIZE+CELLSIZE*2;
     		double vstep = vsize/(stackHeight+1);
     		int boxtop = cy - (int)(vsize)+CELLSIZE*2;
            boolean canHitCards = (allowed_to_edit||(!isSpectator() && (getActivePlayer().boardIndex==pl))) ? gb.LegalToHitCards(unplayed):false;    
            // a little assist for the robot
            Rectangle boxRect = new Rectangle(cx-CELLSIZE,boxtop,CELLSIZE*2,(int)(vstep*stackHeight));

            unplayed.drawStack(gc,this,null,CELLSIZE*2,cx,cy,0,vscale,null);
            // note we use anyHit because we can pick up cards out of turn sequence.
            // we're doing our own mouse sensitivity because the 2:1 aspect ratio is unexpected,
            // and we need to point to a particular card.
            //G.frameRect(gc,Color.red,boxRect);
        	if(G.pointInRect(canHitCards?anyHit:null,boxRect))
        	{	//showingLid = pl;
         		int ind = Math.max(0,Math.min(stackHeight-1,(int)((G.Top(anyHit)-boxtop)/vstep)));
         		anyHit.hitCode = unplayed.rackLocation;
         		anyHit.hitObject = unplayed;
        		PalabraChip ch = unplayed.chipAtIndex(ind);
        		if(ch!=null)
        		{
        			anyHit.hit_y = ind;
        			anyHit.setHelpText(s.get(ClickToSelect));
        			anyHit.arrow = (movingObjectIndex()<0)?StockArt.UpArrow:StockArt.DownArrow;
        			anyHit.awidth = CELLSIZE;
        			drawCardFace(gc,new Rectangle(G.Left(anyHit)-CELLSIZE,G.Top(anyHit)-5*CELLSIZE/3,CELLSIZE*2,CELLSIZE*3),ch);
        		}
         	}
            //G.frameRect(gc,Color.yellow,boxRect);

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
    	PalabraChip ch = PalabraChip.getChip(obj);
    	ch.drawChip(g,this,CELLSIZE*2, xp, yp, null); 
    }


    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean review = reviewMode() && !mutable_game_record;
      // erase
     PalabraBoard gb = disB(gc);
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[review?BACKGROUND_REVIEW_INDEX:BACKGROUND_INDEX].tileImage(gc, fullRect);  
     textures[BACKGROUND_TILE_INDEX].tileImage(gc,boardRect);   
      GC.frameRect(gc,Color.black,boardRect);
      gb.SetDisplayParameters( 1.0, 1.0, 0, 0, 0); // shrink a little and rotate 30 degrees
      gb.SetDisplayRectangle(boardRect);
      
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
    		{	Rectangle pr = playerRects[pl.boardIndex];
    			if(G.pointInRect(x,y,pr)) 
    			{
    				return(HIDDEN);
    			}
    		}
    	}
    	return(super.encodeScreenZone(x,y,p));
    }
    
   /** draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */
    private void drawBoardElements(Graphics gc, PalabraBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  Either gc or highlight might be
    	// null, but not both.
        //
        // using closestCell is preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.

        ChipStack deck = PalabraChip.redDeck;
        int w = G.Width(brect);
        int step = w/20;
        int dx = step/2;
        int dy = step;
        int l = G.Left(brect);
        int t = G.Top(brect);
        for(int lim = deck.size()-1; lim>=0; lim--)
        {
        	PalabraChip ch = deck.elementAt(lim);
        	ch.drawCard(gc, this, step, 1.0, l+dx,t+dy, null);
        	dx += step;
        	if(dx>w) { dx = step/2; dy+= step*2; }
        }
    }
    
    // this is non-standard, but for Raj the moving obects are per-player
    // this allows simultaneous card moves to display properly
    public int getMovingObject(HitPoint highlight)
    {	return(isSpectator()?NothingMoving:movingObjectIndex()); 
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
    {  PalabraBoard gb = disB(gc);
       PalabraState state = gb.getState();
       if((state==PalabraState.PLAY_STATE) && gb.hasDroppedCard(getActivePlayer().boardIndex)) { state = PalabraState.WAIT_STATE; }
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
       
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
       drawBoardElements(gc, gb, boardRect, (gb.getState()==PalabraState.PLAY_STATE)?selectPos:ourTurnSelect);
       int nPlayers = gb.nPlayers();
       for(int i=0;i<nPlayers;i++)
    	   { drawPlayerBoard(gc,i,(i==gb.whoseTurn)?ourTurnSelect:null,gb,selectPos);
    	   }
       GC.setFont(gc,standardBoldFont());
       // draw the board control buttons 

		if (state != PalabraState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
            if ((!SIMULTANEOUS_PLAY || (state==PalabraState.RESIGN_STATE)) && GC.handleRoundButton(gc, doneRect, 
            		(gb.DoneState() ? buttonSelect : null), s.get(DoneAction),
                    HighlightColor, rackBackGroundColor))
            {	// always display the done button, but only make it active in
            	// the appropriate states
                buttonSelect.hitCode = GameId.HitDoneButton;
            }
			handleEditButton(gc,editRect,buttonSelect, selectPos,HighlightColor, rackBackGroundColor);
        }

 
        drawPlayerStuff(gc,(state==PalabraState.PUZZLE_STATE),nonDragSelect,
        			HighlightColor,
        			rackBackGroundColor);
  
 
        // draw the avatars
    	int activePl = (simultaneousTurnsAllowed())
    								? getActivePlayer().boardIndex
    								: gb.whoseTurn;
    							
    	commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
    	double messageRotation = pl.messageRotation();

        standardGameMessage(gc,messageRotation,
        		state==PalabraState.GAMEOVER_STATE?gameOverMessage(gb):s.get(state.getDescription()),
        				state!=PalabraState.PUZZLE_STATE,
        				activePl,
        				stateRect);
        goalAndProgressMessage(gc,nonDragSelect,s.get(GoalString),progressRect, goalRect);
        //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for raj
    
        // draw the vcr controls
        drawVcrGroup(nonDragSelect, gc);

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
        
        startBoardAnimations(replay,bb.animationStack,CELLSIZE*2,MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay.animate) { playSounds(mm); }
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
        return (new Palabramovespec(st, player));
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
    {	PalabraCell hitCell = hitCell(hp);
        if (hp.hitCode instanceof PalabraId) // not dragging anything yet, so maybe start
        {
        PalabraId hitObject =  (PalabraId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    
	    case PlayerPrizes:
       case PlayerCards:
       case PlayerDiscards:
    	{
	    	int cx = hp.hit_y;
	    	boolean simultaneous = simultaneousTurnsAllowed();
	    	if(simultaneous)
	    	{	int ord = hitCell.col-'A';
	    		bb.setMyIndex(ord,true);
	    		PerformAndTransmit("epick "+ord+" "+hitObject.shortName+" "+hitCell.col+" "+cx);
	    	}
	    	else
	    	{	PerformAndTransmit("pick "+hitObject.shortName+" "+hitCell.col+" "+cx);	
	    	}
    		}
    		break;
	    case PrizePool:
	    	PerformAndTransmit("Pick P @ -1");
	    	break;
	    case BoardLocation:
	    	{	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
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
    if(!(id instanceof PalabraId))
    	{	if((id==DefaultId.HitNoWhere) && !isSpectator() && simultaneousTurnsAllowed())
    		{
    			String dn = bb.unDropMove();
    			if(dn!=null) { PerformAndTransmit(dn); }
    			String up = bb.unPickMove();
    			if(up!=null) { PerformAndTransmit(up); }
    			missedOneClick = false;
    		}
    		else { missedOneClick = performStandardActions(hp,missedOneClick); }
    		}
	else {
		PalabraId hitCode = (PalabraId)hp.hitCode;
		int mo = movingObjectIndex();
    	if(mo>=0)
    	{
		PalabraCell hitObject = hitCell(hp);
		PalabraState state = bb.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case BoardLocation:	// we hit an occupied part of the board 
        case EmptyBoard:
			switch(state)
			{	case CONFIRM_CARD_STATE:
				default:
					throw G.Error("Not %s hit in state %s",hitCode,state);
				case CONFIRM_STATE:
				case PLAY_STATE:
				case PUZZLE_STATE:
					PerformAndTransmit("dropb "+hitObject.col+" "+hitObject.row);
					break;
			}
			break;
        case PlayerPrizes:
        case PlayerDiscards:
        case PlayerCards:
        	{
        	int cx = hp.hit_y;
        	String op = (simultaneousTurnsAllowed()) ? ("edrop "+(hitObject.col-'A')+" ") : "Drop "; 
        	PerformAndTransmit(op+ hitCode.shortName+" "+hitObject.col+" "+cx);
        	}
        	break;
        	
        case PrizePool:
           if(movingObjectIndex()>=0) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop P @ -1");
			}
           break;
 
         }
    	}}
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
    public String sgfGameType() { return(Palabra_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int key = his.intToken();
    	int np = his.intToken();
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // long rk = his.longToken();
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



    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return new PalabraPlay();
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
            {	Tokenizer tok = new Tokenizer(value);
            	String game = tok.nextToken();
            	long key = tok.longToken();
            	int np = tok.intToken();
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
        public void ViewerRun(int wait)
        {
            super.ViewerRun(wait);
            runAsyncRobots();
            
            if((started || reviewOnly) && (OurMove() && !reviewMode()))
            {	commonPlayer p = players[bb.whoseTurn];
            	if(p!=null && p.robotPlayer==null)
            	{
            	if(bb.getState()==PalabraState.AWARD_PRIZE_STATE)
            	{	// in award_prize_state, we show the cards played and wait a short time
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
            	else if(bb.getState()==PalabraState.CONFIRM_CARD_STATE)
            	{	// our turn, and we think everyone is ready.
            		// the theory is that if anyone becomes NOT ready, they will
            		// revert to "play" state until they also see all-ready.
            		// meantime, we're already comitted but oh-well - we were ready, right?
            		String mov = bb.cardMove();
            		if(mov!=null)
            		{
            		// we got the move.  If we didn't it's because we're the one who
            		// sneaked in a mind-change when the others weren't looking.
            		PerformAndTransmit(mov);
            		}
            	}
            	else if(bb.getState()==PalabraState.SELECT_PRIZE_STATE)
            	{	// time to select a new prize.
            		PerformAndTransmit("Select -1");
            	}
            	}
            }
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


}

