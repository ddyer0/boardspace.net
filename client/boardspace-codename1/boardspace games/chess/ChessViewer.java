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
package chess;

import com.codename1.ui.geom.Rectangle;
import bridge.Color;
import bridge.JCheckBoxMenuItem;
import bridge.JMenuItem;
import common.GameInfo;
/* below here should be the same for codename1 and standard java */
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import static chess.ChessMovespec.*;


import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Toggle;

/**
 * This code shows the overall structure appropriate for a game view window.
 * 
 * TODO: make castling more intuitive by moving the rook at the "confirm" stage
 * 
*/
public class ChessViewer extends CCanvas<ChessCell,ChessBoard> implements ChessConstants, PlacementProvider
{
	static final String Chess_SGF = "Chess"; // sgf game number allocated for lyngk
    static final String ImageDir = "/chess/images/";
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    boolean prerotated = false;
    // private state
    private ChessBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("chip",2);

    private Rectangle reverseViewRect = addRect("reverse");
    private Toggle eyeRect = new Toggle(this,"eye",
    						StockArt.NoEye,ChessId.ToggleEye,NoeyeExplanation,
    						StockArt.Eye,ChessId.ToggleEye,EyeExplanation
    						);
    private JCheckBoxMenuItem reverseOption = null;
    private JMenuItem offerDrawAction = null;
    
    private Rectangle declineDrawRect = addRect("declineDraw");
    private Rectangle acceptDrawRect = addRect("acceptDraw");	
    private Rectangle bannerRect = addRect("banner");			// the game type, positioned at the top
    private Rectangle capRects[] = addRect("cap",2);
    private Rectangle repRect = addRect("repetition");
    /**
     * preload all the images associated with the game. This is delegated to the chip class.
     */
    public synchronized void preloadImages()
    {	
       	ChessChip.preloadImages(loader,ImageDir);
       	gameIcon = ChessChip.whiteKing.image;
    }
    public int getAltChipset()
    {
    	if(!prerotated && seatingFaceToFace())
    	{
    	// this communicates with ChessChip.getChipRotation();
    	return(b.reverseY()?1:2);
    	}
    	return(0);
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	// int players_in_game = Math.max(3,info.getInt(exHashtable.PLAYERS_IN_GAME,4));
    	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
    	enableAutoDone = true;
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	ChessConstants.putStrings();
        }

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        int map[] = getStartingColorMap();
        b = new ChessBoard(info.getString(GameInfo.GAMETYPE, Variation.Chess.name),randomKey,players_in_game,
        		repeatedPositions,map,ChessBoard.REVISION);
        useDirectDrawing(true);
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        offerDrawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);     
        
    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        int np = b.nPlayers();
        b.doInit(b.gametype,b.randomKey,np,b.revision);			// initialize the board
        if(!preserve_history)
    	{ 
        startFirstPlayer();
    	}

    }
    
public Rectangle createPlayerGroup(int player,int x,int y,double rotate,int unit)
    {	commonPlayer p0 = getPlayerOrTemp(player);
    	Rectangle chipRect = chipRects[player];
    	Rectangle capturedRect = capRects[player];
    	Rectangle done = doneRects[player];
    	int chipW = unit*3;
    	int doneW = plannedSeating() ? unit*5 : 0;
        G.SetRect(chipRect,x,y,chipW,chipW*3/2);
        Rectangle box = p0.createRectangularPictureGroup(x+chipW,y,unit);
        int boxw = G.Width(box);
        G.SetRect(done, G.Right(box)+unit/2, y+unit/3,doneW,doneW/2);
        G.union(box,done,chipRect);
        if(flatten)
        {
        G.SetRect(capturedRect, G.Right(done)+unit/3,y,boxw,unit*3);
        }
        else
        {
        G.SetRect(capturedRect, x,G.Bottom(box),G.Width(box),unit*3);
        }
        G.union(box, capturedRect);
        p0.displayRotation = rotate;
        return(box);
    }	
    
private boolean flatten = false;
public void setLocalBounds(int x, int y, int width, int height)
{
	setLocalBoundsV(x,y,width,height,new double[] {1,-1});
}

public double setLocalBoundsA(int x, int y, int width, int height,double a)
{	flatten = a<0;	
	G.SetRect(fullRect, x, y, width, height);
	GameLayoutManager layout = selectedLayout;
	int nPlayers = nPlayers();
    int chatHeight = selectChatHeight(height);
   	// ground the size of chat and logs in the font, which is already selected
	// to be appropriate to the window size
	int fh = standardFontSize();
	int minLogW = fh*10;
	int vcrW = fh*16;
   	int minChatW = fh*35;	
    int minLogH = fh*10;	
    int buttonW = fh*8;
    int margin = fh/2;
   	// this does the layout of the player boxes, and leaves
	// a central hole for the board.
	//double bestPercent = 
	layout.selectLayout(this, nPlayers, width, height,
			margin,	
			0.70,	// % of space allocated to the board
			1,	// 1:1 aspect ratio for the board
			fh*2,	// maximum cell size
			0.2		// preference for the designated layout, if any
			);
	
    // place the chat and log automatically, preferring to place
	// them together and not encroaching on the main rectangle.
	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
	layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);
   	layout.placeDoneEditRep(buttonW,3*buttonW/2,doneRect,editRect,repRect);
	layout.placeTheVcr(this,vcrW,vcrW*3/2);
	layout.placeRectangle(Purpose.Banner,bannerRect,vcrW,vcrW/4,BoxAlignment.Top);
	Rectangle main = layout.getMainRectangle();
	int mainX = G.Left(main);
	int mainY = G.Top(main);
	int mainW = G.Width(main);
    int stateH = fh*5/2;
	int mainH = G.Height(main);
	boolean rotate = seatingFaceToFaceRotated();
    int nrows = rotate ? b.boardColumns : b.boardRows;  
    int ncols = rotate ? b.boardRows : b.boardColumns;
 	// calculate a suitable cell size for the board
	double cs = Math.min((double)mainW/ncols,(double)(mainH)/nrows);
	SQUARESIZE = (int)cs;
 	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
	// center the board in the remaining space
	int boardW = (int)(ncols*cs);
	int boardH = (int)(nrows*cs);
	int extraW = Math.max(0, (mainW-boardW)/2);
	int extraH = Math.max(0, (mainH-boardH)/2);
	int boardX = mainX+extraW;
	int boardY = mainY+extraH;
    int boardBottom = boardY + boardH;
    layout.returnFromMain(extraW,extraH);		// for use in the optimization phase

	//
	// state and top ornaments snug to the top of the board.  Depending
	// on the rendering, it can occupy the same area or must be offset upwards
	//
    int stateY = boardY;
    int stateX = boardX;

    G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,reverseViewRect,eyeRect,noChatRect);
    G.SetRect(boardRect,boardX, boardY, boardW, boardH);         
    G.placeRow(boardX, boardBottom-stateH,boardW,stateH,goalRect);       
    
	if(rotate)
	{
		G.setRotation(boardRect, -Math.PI/2);
		contextRotation = -Math.PI/2;
	}
    setProgressRect(progressRect,goalRect);
    positionTheChat(chatRect,Color.white,Color.white);
    return boardW*boardH;
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
     * translate the mouse coordinate x,y into a size-independent representation
     * presumably based on the cell grid.  This is used to transmit our mouse
     * position to the other players and spectators, so it will be displayed
     * at approximately the same visual spot on their screen.  
     * The results of this function only have to be interpreted by {@link #decodeScreenZone}
     * Some trickier logic may be needed if the board has several orientations,
     * or if some mouse activity should be censored.
     */
    //public String encodeScreenZone(int x, int y,Point p)
    //{
    //	return(super.encodeScreenZone(x,y,p));
    //}
    /**
     * invert the transformation done by {@link #encodeScreenZone}, returning 
     * an x,y pixel address on the main window.
     * @param z
     * @param x
     * @param y
     * @return a point representing the decoded position
     */
    //public Point decodeScreenZone(String z,int x,int y)
    //{
    //	return(super.decodeScreenZone(z,x,y));
    //}
	/**
	 * 
	 * this is a debugging hack to give you an event based on clicking in the player name
	 * You can take whatever action you like, or no action.
	 */
    //public boolean inPlayRect(int eventX, int eventY)
    //{	return(super.inPlayRect(eventX,eventY));
    // }

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

   
    //
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    // for checkers, this is just a single checker, which also displays the number of pieces 
    // left on the board as a sort of progress metric.
    //
    private void DrawCommonChipPool(Graphics gc, ChessBoard gb, int forPlayer, Rectangle r, int player)
    {	ChessChip chips[]= gb.rack;
    	ChessChip thisChip = chips[forPlayer];
        String msg = null;
        thisChip.drawChip(gc,this,5*G.Width(r)/2,G.Left(r)+G.Width(r)/2,G.Top(r)+G.Height(r)/2,msg);
     }
    private void DrawCaptured(Graphics gc,ChessBoard gb,commonPlayer pl,
    			HitPoint highlight,Hashtable<ChessCell,ChessMovespec>targets)
    {	int forPlayer = pl.boardIndex;
    	ChessCell chips[]= gb.captured;
        ChessCell thisCell = chips[forPlayer];
        ChessChip thisChip = thisCell.topChip();
        Rectangle r = capRects[forPlayer];
        
        boolean canHit = gb.LegalToHitChips(forPlayer,thisCell,targets);
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        int h = G.Height(r);
        //G.frameRect(gc, Color.blue, r);
        prerotated = true;
        int px = G.Left(r)+h/2;
        int py = G.Top(r)+h/2;
        if(thisCell.drawStack(gc,this,pt,h*4/3,px,py,0,0.25,0.0,null))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = G.Height(r)/2;
        	highlight.spriteColor = Color.red;
        }
        // 
        // this is a little complicated.  thisCell.curret_rotation will be used
        // to animate moving captured pieces to the rack.  Other things equal,
        // the pieces heading for the white rack should be upside down if the context 
        // displays white pieces upside down.  Chess (and a few other games) accomplish
        // this by adding a rotation to the white pieces in drawChip.  This all works out
        // for animations within the board, but for animations to the rack, the extra animation
        // will be applied, so we need to pre-rotate to compensate.
        //
        // long story short - this only matters in "face to face" seating, to get
        // captured pieces to fly off the board with the proper orientation.
        prerotated = false;
        double rot = gb.rack[forPlayer].getChipRotation(this);
        thisCell.setCurrentRotation(thisCell.currentRotation()- rot); 
        prerotated = true;
        boolean showMoves = eyeRect.isOnNow();
        
        if(showMoves && targets.get(thisCell)!=null)
        {
        	StockArt.SmallO.drawChip(gc,this,h,
        			G.Left(r)+h/4,G.Top(r)+h/2,null);
    	}
        DrawCommonChipPool(gc, gb,forPlayer,chipRects[forPlayer], gb.whoseTurn);
        prerotated = false;

    }
    
    //
    // sprites are normally a game piece that is "in the air" being moved
    // around.  This is called when dragging your own pieces, and also when
    // presenting the motion of your opponent's pieces, and also during replay
    // when a piece is picked up and not yet placed.  While "obj" is nominally
    // a game piece, it is really whatever is associated with b.movingObject()
    //
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {  	// draw an object being dragged
    	ChessChip ch = ChessChip.getChipNumber(obj);// Tiles have zero offset
     	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);

     }


   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, ChessBoard gb, Rectangle brect, HitPoint highlight,Hashtable<ChessCell,ChessMovespec>targets)
    {
     	// targets are the pieces we can hit right now.
     	ChessCell dest = gb.getDest();		// also the current dest and source
     	ChessCell src = gb.getSource();
     	ChessCell last = gb.getPrevDest();	// and the other player's last move
    	numberMenu.clearSequenceNumbers();
     	//gb.simpleScore(gb.whoseTurn);
     	//int sweep = gb.sweep_counter;
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	boolean showMoves = eyeRect.isOnNow();
     	
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
     	Enumeration<ChessCell>cells = gb.getIterator(Itype.LRTB);
     	while(cells.hasMoreElements())
     	{
            ChessCell cell = cells.nextElement();
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            numberMenu.saveSequenceNumber(cell,xpos,ypos);
            HitPoint hitNow = gb.legalToHitBoard(cell,targets) ? highlight : null;
            if( cell.drawStack(gc,this,hitNow,SQUARESIZE,xpos,ypos,0,0.1,null)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	hitNow.arrow =hasMovingObject(highlight) 
      				? StockArt.DownArrow 
      				: cell.topChip()!=null?StockArt.UpArrow:null;
            	hitNow.awidth = SQUARESIZE/2;
            	hitNow.spriteColor = Color.red;
            	}
            if((cell==dest)||(cell==src))
            {
            	StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            }
            if((cell==last) && (numberMenu.selected()==NumberMenu.NumberingMode.None ))
            {
            	StockArt.Dot.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            }
            if((cell.topChip()==null)
        			&& cell.lastContents!=null 
        			&& cell.lastCaptured>0
        			&& numberMenu.getVisibleNumber(cell.lastCaptured)>0)
                	{
                		cell.lastContents.drawChip(gc,this,SQUARESIZE*2/3,xpos,ypos,null);
                		StockArt.SmallX.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
                	}
            if(showMoves && targets.get(cell)!=null)
            {
            	StockArt.SmallO.drawChip(gc,this,SQUARESIZE/2,xpos,ypos,null);
        	}
            //if(cell.sweep_counter==sweep)
            //	{
            //	StockArt.Dot.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            //	}
        	}
     	numberMenu.drawSequenceNumbers(gc,SQUARESIZE*2/3,labelFont,labelColor);
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  
       ChessChip banner = b.variation.banner;
       banner.getImage(loader).centerImage(gc, bannerRect);
       GC.frameRect(gc,Color.red,bannerRect);
       DrawReverseMarker(gc,reverseViewRect,highlight,ChessId.ReverseViewButton);
       eyeRect.activateOnMouse=true;
       eyeRect.draw(gc,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    { ChessBoard gb = disB(gc);
      int whoseTurn = gb.whoseTurn;
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      ChessState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
       Hashtable<ChessCell,ChessMovespec>targets = gb.getTargets();

      /**
      try { 
    	  targets = gb.getTargets();
      }
      catch (Throwable err)
      {	G.print("err "+err);
    	  targets = new Hashtable<ChessCell,ChessMovespec>();
      }
       */
       GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
       drawBoardElements(gc, gb, boardRect, ot,targets);
       GC.unsetRotatedContext(gc,highlight);
       

       boolean planned = plannedSeating();
       for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX;i++)
         {	commonPlayer pl = getPlayerOrTemp(i);
         	pl.setRotatedContext(gc, highlight, false);
            DrawCaptured(gc, gb,pl,ot,targets);
         	if(planned && (i==whoseTurn))
         	{
         		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? ot : null), 
     					HighlightColor, rackBackGroundColor);
         	}       
         	pl.setRotatedContext(gc, highlight, true);
       }
       commonPlayer pl = getPlayerOrTemp(whoseTurn);

		double messageRotation = pl.messageRotation();
        
        GC.setFont(gc,standardBoldFont());
       
        switch(vstate)
        {
        default:
        	if(gb.drawIsLikely())
        	{	// if not making progress, put the draw option on the UI
            	if(GC.handleSquareButton(gc,acceptDrawRect,select,s.get(OFFERDRAW),
            			HighlightColor,
            			vstate==ChessState.DrawPending ? HighlightColor : rackBackGroundColor))
            	{
            		select.hitCode = GameId.HitOfferDrawButton;
            	}
       		
        	}
        	break;
        case AcceptOrDecline:
        case AcceptPending:
        case DeclinePending:
        	if(GC.handleSquareButton(gc,messageRotation,acceptDrawRect,select,s.get(ACCEPTDRAW),HighlightColor,rackBackGroundColor))
        	{
        		select.hitCode = GameId.HitAcceptDrawButton;
        	}
        	if(GC.handleSquareButton(gc,messageRotation,declineDrawRect,select,s.get(DECLINEDRAW),HighlightColor,rackBackGroundColor))
        	{
        		select.hitCode = GameId.HitDeclineDrawButton;
        	}
       	break;
        }
        
		if (vstate != ChessState.Puzzle)
        {
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
                }

 		drawPlayerStuff(gc,(vstate==ChessState.Puzzle),moving?null:highlight,HighlightColor,rackBackGroundColor);
        if (gc != null)
        {	
            standardGameMessage(gc,messageRotation,
            		vstate==ChessState.Gameover
            			?gameOverMessage(gb)
            			:s.get(vstate.getDescription()),
            				vstate!=ChessState.Puzzle,
            				whoseTurn,
            				stateRect);
            gb.rack[whoseTurn].drawChip(gc,this,iconRect,null,2);
        }
        goalAndProgressMessage(gc,highlight,Color.black,s.get(VictoryCondition),progressRect, goalRect);
   
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);
        drawVcrGroup(ourSelect, gc);
        drawAuxControls(gc,ourSelect);
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
        
		if(b.pickedObject!=null) { lastDropped = b.pickedObject; }

        handleExecute(b,mm,replay);
		numberMenu.recordSequenceNumber(b.moveNumber());
		
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,
        		b.variation==Variation.Atomic
        			? MovementStyle.SimultaneousAfterOne
        			: MovementStyle.Sequential);
        if(replay.animate) { playSounds(mm); }
 
        return (true);
    }

/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new ChessMovespec(st, player));
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
    //{	super.verifyGameRecord();
    //}    
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
    //   		{ commonMove mv = (commonMove)History.elementAt(i);
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
    
private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_BOARD_BOARD:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
       	 break;
     case MOVE_PICK:
    	 playASoundClip(light_drop,100);
    	 break;
    case MOVE_PICKB:
    	playASoundClip(light_drop,100);
    	break;
    case MOVE_DROP:
    	break;
    case MOVE_DROPB:
    	playASoundClip(heavy_drop,100);
      	break;
    case MOVE_DONE:
		 if(b.getState()==ChessState.Check)
	    	{
	    		playASoundClip(light_drop,60);
	    		playASoundClip(light_drop,60);
	    		playASoundClip(light_drop,60);
	    		playASoundClip(light_drop,60);
	    	}
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
        if (hp.hitCode instanceof ChessId) // not dragging anything yet, so maybe start
        {
        ChessId hitObject = (ChessId)hp.hitCode;
		ChessCell cell = hitCell(hp);
		ChessChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(cell.height()>0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.color.shortName);
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
    {	CellId id = hp.hitCode;
    	if(!(id instanceof ChessId)) 
    		{ // handle all the actions that aren't ours
    			missedOneClick = performStandardActions(hp,missedOneClick); 
    		}
    	else {
    	missedOneClick = false;
        ChessId hitObject = (ChessId)id;
		ChessState state = b.getState();
		ChessCell cell = hitCell(hp);
		ChessChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);

        case ToggleEye:
        	eyeRect.toggle();
        	break;
        case ReverseViewButton:
       	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
       	 generalRefresh();
       	 break;
        case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Confirm:
			case Check:
			case Play:
			case Filter:
			case Puzzle:
				if(cell.onBoard)
				{
				if(b.movingObjectIndex()>=0)
				{ 
				  if((chip!=null) 
						  && b.pickedObject.isKing() 
						  && (chip.piece==ChessPiece.Rook)
						  && (chip.color==b.pickedObject.color))
				  {
				  ChessCell src = b.getSource();
				  // special signature, castling looks like the king capturing
				  // its own rook.
				  PerformAndTransmit("Castle "+src.col+" "+src.row+" "+cell.col+" "+cell.row);
				  }
				  else
				  {
				  PerformAndTransmit("Dropb "+cell.col+" "+cell.row); 
				}
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb " +cell.col+" "+cell.row+" "+chip.color.shortName);
				}}
					
				break;
			}
			break;
			
        case White_Captured:
        case Black_Captured:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.

            		PerformAndTransmit("Drop "+col);

			}
            else {
            	PerformAndTransmit("Pick "+((hitObject==ChessId.Black_Captured)?"BC ":"WC "+hp.hit_index));
            }
         	}
            break;

        }}
    }
/**
 * draw the deep unchangeable objects, including those that might be rather expensive
 * to draw.  This background layer is used as a backdrop to the rest of the activity.
 * in our cease, we draw the board and the chips on it. 
 * @param gc
 */
    public void drawFixedElements(Graphics gc)
    {	
       ChessChip.backgroundTile.image.tileImage(gc, fullRect);   
        drawFixedBoard(gc);
    }
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { ChessBoard gb = disB(gc);
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      
      setBoardRect(gb);	// set up the coordinate system for the board
      
      if(reviewBackground)
      {	 
       ChessChip.backgroundReviewTile.image.tileImage(gc,brect);   
      }
       
      gb.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    } 
    
    private void setBoardRect(ChessBoard gb)
    {
	    gb.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
	    gb.SetDisplayRectangle(boardRect);
       
    }
    /**
     * this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     */
    //public void drawCanvas(Graphics offGC, boolean complete,HitPoint hp)
    //{
    // 	drawFixedElements(offGC,complete);
   	
    	// draw the board contents and changing elements.
    //    redrawBoard(offGC,hp);
        //      draw clocks, sprites, and other ephemera
    //    drawClocksAndMice(offGC, null);
    //    DrawArrow(offGC,hp);
    //}

    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parseable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() 
    { 
    	return(""+b.gametype+" "+b.randomKey+" "+b.nPlayers()+" "+b.revision); 
   }
    public String sgfGameType() { return(Chess_SGF); }

    // the format is just what is produced by FormHistoryString
    //
    // this is completely standardized
    //public void performHistoryTokens(StringTokenizer his)
    //{	String command = "";
    //    // now the rest
    //    while (his.hasMoreTokens())
    //    {
    //        String token = his.nextToken();
    //        if (",".equals(token) || ".end.".equals(token))
    //        {
    //            if (!"".equals(command))
    //            {
    //                PerformAndTransmit(command, false,false);
    //                command = "";
    //            }
    //        }
    //       else
    //        {
    //            command += (" " + token);
    //        }
    //    }	
    //}  
    //public void performPlayerInitialization(StringTokenizer his)
    //{	int fp = G.IntToken(his);
    //	BoardProtocol b = getBoard();
    //    if (fp < 0)   {  fp = 0;  }
    //    b.setWhoseTurn(fp);
    //    players[fp].ordinal = 0;
    //    players[(fp == 0) ? 1 : 0].ordinal = 1;
    //	
    //}

    
    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long rk = G.LongToken(his);
    	int np = G.IntToken(his);
    	int rev = G.IntToken(his); 
    		
    	// make the random key part of the standard initialization,
    	// even though games like checkers probably don't use it.
        b.doInit(token,rk,np,rev);
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

    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target,String cmd)
    {
    	if(target==offerDrawAction)
    	{	if(OurMove() 
    			&& (b.movingObjectIndex()<=0)
    			&& ((b.getState()==ChessState.Play) || (b.getState()==ChessState.DrawPending))) 					
    					
    		{
    		if(b.canOfferDraw())
			{
    		PerformAndTransmit(OFFERDRAW);
			}
    		else { G.infoBox(null,s.get(DrawNotAllowed)); }
    		}
    		return(true);
    	}
    	else if(target==reverseOption)
    	{
    	b.setReverseY(reverseOption.getState());
    	generalRefresh();
    	return(true);
    	}
    	else 
    	return(super.handleDeferredEvent(target,cmd));
     }
/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically
 *  */
    
    //   public void ViewerRun(int wait)
    //   {
    //       super.ViewerRun(wait);
    //   }

    /** start the robot.  This is used to invoke a robot player.  Mainly this needs 
     * to know the class for the robot and any initialization it requires.  Normally
     * the the robot player p is also returned.  Raj (and other simultaneous play games)
     * might return a different player, the one actually started.
     *  */
   // public commonPlayer startRobot(commonPlayer p,commonPlayer runner,Bot bot)
   // {	// this is what the standard method does:
    	// int level = sharedInfo.getInt(sharedInfo.ROBOTLEVEL,0);
    	// RobotProtocol rr = newRobotPlayer();
    	// rr.InitRobot(sharedInfo, getBoard(), null, level);
    	// p.startRobot(rr);
    //	return(super.startRobot(p,runner,bot));
    //}

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new ChessPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * 
     * ultima summary:	5/23/2023
		163 files visited 0 problems
	   chess summary:
		315 files visited 0 problems
	   chess960 summary:
		9 files visited 0 problems
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
             	int np = st.hasMoreTokens() ? G.IntToken(st) : 2;
               	int rev = st.hasMoreTokens() ? G.IntToken(st) : 0; 
                b.doInit(typ,ran,np,rev);
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
                replayStandardProps(name,value);
            }

            prop = prop.next;
        }

        if (!"".equals(comments))
        {
            setComment(comments);
        }
    }

    public int getLastPlacement(boolean empty) {
    	return b.lastPlacedIndex;
	}
}

