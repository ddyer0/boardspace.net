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
package ordo;


import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.JCheckBoxMenuItem;
import bridge.JMenuItem;
import common.GameInfo;

import static ordo.OrdoMovespec.*;


/* below here should be the same for codename1 and standard java */
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.Drawable;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Toggle;

/**
 * This code shows the overall structure appropriate for a game view window.
*/
public class OrdoViewer extends CCanvas<OrdoCell,OrdoBoard> implements OrdoConstants, GameLayoutClient
{
	static final String Ordo_SGF = "Ordo"; // sgf game name
	static final String ImageDir = "/ordo/images/";
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // private state
    private OrdoBoard b = null; 	// the board from which we are displaying
    private int CHIPSIZE;
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle playerChipRect[] = addRect("chip",2);
 

    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;
    private JMenuItem offerDrawAction = null;
    
    private Rectangle repRect = addRect("repRect");
    private Rectangle declineDrawRect = addRect("declineDraw");
    private Rectangle acceptDrawRect = addRect("acceptDraw");	
    private Toggle eyeRect = new Toggle(this,"eye",
			StockArt.NoEye,OrdoId.ToggleEye,NoeyeExplanation,
			StockArt.Eye,OrdoId.ToggleEye,EyeExplanation
			);
  /**
     * preload all the images associated with the game. This is delegated to the chip class.
     */
    public synchronized void preloadImages()
    {	
       	OrdoChip.preloadImages(loader,ImageDir);
       	gameIcon = OrdoChip.OrdoIcon.image;
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
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	OrdoConstants.putStrings();
        }

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new OrdoBoard(info.getString(GameInfo.GAMETYPE, Variation.Ordo.name),randomKey,players_in_game,
        			repeatedPositions,getStartingColorMap(),OrdoBoard.REVISION);
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
	/**
	 * this is the main method to do layout of the board and other widgets.  I don't
	 * use swing or any other standard widget kit, or any of the standard layout managers.
	 * they just don't have the flexibility to produce the results I want.  Your mileage
	 * may vary, and of course you're free to use whatever layout and drawing methods you
	 * want to.  However, I do strongly encourage making a UI that is resizable within
	 * reasonable limits, and which has the main "board" object at the left.
	 * 
	 *  The basic layout technique used here is to start with a cell which is about the size
	 *  of a board square, and lay out all the other object relative to the board or to one
	 *  another.  The rectangles don't all have to be on grid points, and don't have to
	 *  be non-overlapping, just so long as the result generally looks good.
	 *  
	 *  When "extraactions" is available, a menu option "show rectangles" works
	 *  with the "addRect" mechanism to help visualize the layout.
	 */ 
    public void setLocalBounds(int x,int y,int width,int height)
    {
    	setLocalBoundsV(x,y,width,height,aspects);
    }
    double aspects[] = {0.7,1.0,1.3};
    public double setLocalBoundsA(int x,int y,int width,int height,double aspect)
    {
        G.SetRect(fullRect,x,y,width,height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
     	int fh = standardFontSize();
     	int margin = fh/2;
     	int minChatW = fh*35;
    	int ideal_logwidth = fh*15;
     	
    	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	    
       	layout.selectLayout(this, nPlayers, width,height,
       			margin,	// size of margins for boxes	
       			0.7, 	// at least 70% for the board
       			aspect,	// preferred aspect ratio for the board 
       			fh*3, 	//maximum cell size, based on font size for the window
       			0.3);	// preference for sticking with the designated layout

    	int chatHeight = selectChatHeight(height);
        int logH =  fh*15 ;
        int vcrW = fh*16;
        int vcrMW = fh*20;
        int stateH = fh*5/2;
        int buttonW = fh*8;
      	layout.placeTheVcr(this,vcrW,vcrMW);
       	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight, minChatW*2, 3*chatHeight/2, logRect,
    			ideal_logwidth,logH,ideal_logwidth*2,logH*2);
 
      	layout.placeDoneEditRep(buttonW, buttonW*3/2, doneRect, editRect, repRect);
    	layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);

    	Rectangle main = layout.getMainRectangle();
      
        
    	boolean rotate = seatingFaceToFaceRotated();	// divine is we want to rotate the board
    	int w = G.Width(main);
    	int h = G.Height(main);
    	int SQUARESIZE = Math.min(w/b.ncols,(h-stateH)/(b.nrows));
    	int boardW = SQUARESIZE * (rotate ? b.nrows : b.ncols);
        int boardH = SQUARESIZE * (rotate ? b.ncols : b.nrows);
        int extraW = (w-boardW)/2;
        int extraH = (h-boardH-stateH*2)/2;
        int boardX = G.Left(main)+extraW;
        int boardY = G.Top(main)+extraH+stateH;
       	layout.returnFromMain(extraW,extraH);
       	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
       
        if(rotate)
        {	// this will trigger rotation of the board and it's coordinate system
        	// when it's drawn.
        	G.setRotation(boardRect, -Math.PI/2);
        	contextRotation = -Math.PI/2;
        	CHIPSIZE = SQUARESIZE*9/10;
        }
        else {
        	CHIPSIZE=SQUARESIZE;
        }
        int boardBottom = boardY+boardH;

         {
        int stateY = boardY-stateH/2;
        int stateX = boardX;
        G.placeStateRow(	stateX,
        			stateY,
        			boardW,stateH,iconRect,stateRect,annotationMenu,numberMenu,eyeRect,noChatRect);
        
        G.placeRow(stateX, boardBottom-stateH/2, boardW, stateH, goalRect,reverseViewRect);
        
 
        setProgressRect(progressRect,goalRect);
                        
        }
        
         positionTheChat(chatRect,Color.white,Color.white);
         return(boardW*boardH);
    }

    /** create a compact group of boxes with the player components and an identifier icon
     * normally we use a piece from the board as the icon.
     * @param player the index of the player.  use {@link #getPlayerOrTemp(int)}to get the associated commonPlayer
     * @param x
     * @param y
     * @param rot for the player information
     * @param unit pixel size, usually based on the font size
     * @return the bounding rectangle for the newly created group
     */
    public Rectangle createPlayerGroup(int player,int x,int y,double rot,int unit)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int chipW = unit*3;
    	int doneW = unit*4;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unit);
    	Rectangle chip = playerChipRect[player];
        Rectangle done = doneRects[player];
        G.SetRect(chip,x,y,chipW,chipW);
        G.SetRect(done, G.Right(box)+unit/2,y+unit/2,plannedSeating()?doneW:0,doneW/2);
    	pl.displayRotation = rot;
    	G.union(box, chip,done);
    	return(box);
    }

    //
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    // for checkers, this is just a single checker, which also displays the number of pieces 
    // left on the board as a sort of progress metric.
    //
    private void DrawCommonChipPool(Graphics gc, OrdoBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	OrdoCell chips[]= gb.rack;
        boolean canHit = gb.LegalToHitChips(forPlayer);
        OrdoCell thisCell = chips[forPlayer];
        OrdoChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
         HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = ""+gb.occupiedCells[forPlayer].size();
        thisCell.drawStack(gc,this,pt,G.Width(r),G.centerX(r),G.centerY(r),0,0,msg);
        if((highlight!=null) && (highlight.hitObject==thisCell))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = G.Width(r)/2;
        	highlight.spriteColor = Color.red;
        }
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
    	int obj2 = obj/100;
    	int obj1 = obj%100;
    	OrdoChip ch = OrdoChip.getChipNumber(obj1);// Tiles have zero offset
    	Drawable chd = obj2!=0 ? ch.getKing() : ch;
    	chd.drawChip(g,this,CHIPSIZE,xp,yp,null);

     }

    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    // public Point spriteDisplayPoint()
	//{  return super.spriteDisplayPoint();
	//}


    /** this is used by the game controller to supply entertainment strings to the lobby */
    //public String gameProgressString()
    //{	// this is what the standard method does
    	// return ((reviewer ? s.get(ReviewAction) : ("" + viewMove)));
    //	return(super.gameProgressString());
    //}





   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, OrdoBoard gb, Rectangle brect, HitPoint highlight)
    {
    	// this logic animates the expansion of stacks when the button is pushed.
    	boolean show = eyeRect.isOnNow();
     	// targets are the pieces we can hit right now.
     	Hashtable<OrdoCell,OrdoMovespec>targets = gb.getTargets();
     	OrdoCell dest = gb.getDest();		// also the current dest and source
     	OrdoCell last = gb.getPrevDest();	// and the other player's last move
     	OrdoCell selectedStart = gb.selectedStart;
     	OrdoCell selectedEnd = gb.selectedEnd;
     	
    	numberMenu.clearSequenceNumbers();
    	 
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	
     	Enumeration<OrdoCell>cells = gb.getIterator(Itype.LRTB);
     	while(cells.hasMoreElements())
     	{
            OrdoCell cell = cells.nextElement();
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            HitPoint hitNow = gb.legalToHitBoard(cell,targets) ? highlight : null;
            
            numberMenu.saveSequenceNumber(cell,xpos,ypos);

            if( cell.drawStack(gc,this,hitNow,CHIPSIZE,xpos,ypos,liftSteps,0.1,null)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	highlight.arrow =hasMovingObject(highlight) 
          				? StockArt.DownArrow 
          				: cell.topChip()!=null?StockArt.UpArrow:null;
                highlight.awidth = CHIPSIZE/3;
            	highlight.spriteColor = Color.red;
            	}
            if(show)
            {
            if((cell==dest)||(cell==selectedStart)||(hitNow!=null))
            
            {
            	StockArt.SmallO.drawChip(gc,this,CHIPSIZE,xpos,ypos,null);
            }}
            if(cell==last)
            {
            	StockArt.Dot.drawChip(gc,this,CHIPSIZE,xpos,ypos,null);
            }
            if(cell==selectedStart)
            {
            	StockArt.Dot.drawChip(gc,this,CHIPSIZE*3/2,xpos,ypos,null);
            }
            if(cell==selectedEnd)
            {
            	StockArt.Dot.drawChip(gc,this,CHIPSIZE*3/2,xpos,ypos,null);
            }
            if((cell.topChip()==null)
        			&& cell.lastContents!=null 
        			&& cell.lastCaptured>0
        			&& numberMenu.getVisibleNumber(cell.lastCaptured)>0)
                	{	
                		cell.lastContents.drawChip(gc,this,CHIPSIZE*2/3,xpos,ypos,null);
                		StockArt.SmallX.drawChip(gc,this,CHIPSIZE,xpos,ypos,null);
                	}
    	}
     	numberMenu.drawSequenceNumbers(gc,CHIPSIZE*2/3,labelFont,labelColor);

    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {      	
       DrawReverseMarker(gc,reverseViewRect,highlight,OrdoId.ReverseViewButton);
       eyeRect.activateOnMouse=true;
       eyeRect.draw(gc,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    { OrdoBoard gb = disB(gc);
      int whoseTurn = gb.whoseTurn;
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ourTurnSelect = ourTurn ? highlight : null;	// hit if our turn
      HitPoint ourButtonSelect = moving?null:ourTurnSelect;	// hit if our turn and not dragging
      HitPoint vcrSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      OrdoState vstate = gb.getState();
      gameLog.redrawGameLog(gc, vcrSelect, logRect, boardBackgroundColor);
      GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
      drawBoardElements(gc, gb, boardRect, ourTurnSelect);
      GC.unsetRotatedContext(gc,highlight);

      boolean planned = plannedSeating();
      for(int i=0;i<nPlayers();i++)
        {
    	commonPlayer pl = getPlayerOrTemp(i);
    	pl.setRotatedContext(gc, highlight,false);
        DrawCommonChipPool(gc, gb,i,playerChipRect[i], whoseTurn,ourTurnSelect);
        if(planned && (i==whoseTurn)) 
        	{handleDoneButton(gc,doneRects[i],(b.DoneState()? ourButtonSelect : null),HighlightColor, rackBackGroundColor);
        	}
    	pl.setRotatedContext(gc, highlight,true);
        }

      GC.setFont(gc,standardBoldFont());
      commonPlayer pl = getPlayerOrTemp(whoseTurn);
      double messageRotation = pl.messageRotation();
      switch(vstate)
        {
        default:
        	if(gb.drawIsLikely())
        	{	// if not making progress, put the draw option on the UI
            	if(GC.handleSquareButton(gc,messageRotation,acceptDrawRect,ourButtonSelect,s.get(OFFERDRAW),
            			HighlightColor,
            			vstate==OrdoState.DrawPending ? HighlightColor : rackBackGroundColor))
            	{
            		ourButtonSelect.hitCode = GameId.HitOfferDrawButton;
            	}
       		
        	}
        	break;
        case AcceptOrDecline:
        case AcceptPending:
        case DeclinePending:
        	if(GC.handleSquareButton(gc,messageRotation,acceptDrawRect,ourButtonSelect,s.get(ACCEPTDRAW),HighlightColor,rackBackGroundColor))
        	{
        		ourButtonSelect.hitCode = GameId.HitAcceptDrawButton;
        	}
        	if(GC.handleSquareButton(gc,messageRotation,declineDrawRect,ourButtonSelect,s.get(DECLINEDRAW),HighlightColor,rackBackGroundColor))
        	{
        		ourButtonSelect.hitCode = GameId.HitDeclineDrawButton;
        	}
       	break;
        }
        
		if (vstate != OrdoState.Puzzle)
        {
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(b.DoneState()? ourButtonSelect : null),HighlightColor, rackBackGroundColor);
				}
        	handleEditButton(gc,messageRotation,editRect,ourButtonSelect,highlight,HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==OrdoState.Puzzle),ourButtonSelect,HighlightColor,rackBackGroundColor);

        if (gc != null)
        {	
            standardGameMessage(gc,messageRotation,
            		vstate==OrdoState.Gameover
            			?gameOverMessage(gb)
            			:s.get(vstate.getDescription()),
            				vstate!=OrdoState.Puzzle,
            				whoseTurn,
            				stateRect);
            gb.playerChip(whoseTurn).drawChip(gc, this, iconRect,null);
        }
        goalAndProgressMessage(gc,vcrSelect,Color.black,
        			s.get(gb.variation==Variation.OrdoX?VictoryConditionX:VictoryCondition),progressRect, goalRect);
   
        DrawRepRect(gc,messageRotation,Color.black,gb.Digest(),repRect);
        drawAuxControls(gc,vcrSelect);
        // draw last so pop-up version is over everything else
        drawVcrGroup(vcrSelect, gc);
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
        handleExecute(b,mm,replay);
        numberMenu.recordSequenceNumber(b.moveNumber());

        lastDropped = b.lastDropped;
        
        startBoardAnimations(replay,b.animationStack,CHIPSIZE,MovementStyle.Simultaneous);        	
        if(replay!=replayMode.Replay) { playSounds(mm); }
 
        return (true);
    }
 	/**
      * return the dynamically adjusted size during an animation.  This allows
      * compensation for things like the zoom level of the board changing after
      * the animation is started.
      */
 	//public int activeAnimationSize(Drawable chip,int thissize) { return(thissize); }

     /**
      * in general, the animation stack is a specialized communication between
      * the board and the viewer.  Executing moves conventionally pushes pairs
      * of source+destination on the stack.  These moves have already occurred,
      * so if the viewer was undisturbed, it would display the final state.
      * 
      * the invisible trick is that the cells that are the targets of animations
      * will reduce stack heights by the number of animations still in progress.
      * This magically makes them seem shorter until the animation completes.
      * 
      * the rest of exactly how the animations are timed and structured is
      * Idiosyncratic to a particular game.
      * 
      * @param replay
      */

/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new OrdoMovespec(st, player));
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

//    public commonMove EditHistory(commonMove nmove)
//    {
//    	OrdoMovespec newmove = (OrdoMovespec) nmove;
//    	OrdoMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            OrdoMovespec m = (OrdoMovespec) History.elementAt(idx);
//                if(m.next!=null) { idx = -1; }
//                else 
//               {
//                switch (newmove.op)
//                {
//                case MOVE_RESET:
//                	rval = null;	// reset never appears in the record
//                 case MOVE_RESIGN:
//                	// resign unwind any preliminary motions
//                	switch(m.op)
//                	{
//                  	default:	
//                 		if(state==PUZZLE_STATE) { idx = -1; break; }
//                 	case MOVE_PICK:
//                 	case MOVE_PICKB:
//               		UndoHistoryElement(idx);	// undo back to last done
//                		idx--;
//                		break;
//                	case MOVE_DONE:
//                	case MOVE_START:
//                	case MOVE_EDIT:
//                		idx = -1;	// stop the scan
//                	}
//                	break;
//                	
//             case MOVE_DONE:
//             default:
//            		idx = -1;
//            		break;
//               case MOVE_DROPB:
//                	if(m.op==MOVE_PICKB)
//                	{	if((newmove.to_col==m.from_col)
//                			&&(newmove.to_row==m.from_row))
//                		{ UndoHistoryElement(idx);	// pick/drop back to the same spot
//                		  idx--;
//                		  rval=null;
//                		}
//                	else if(idx>0)
//                	{ OrdoMovespec m2 = (OrdoMovespec)History.elementAt(idx-1);
//                	  if((m2.op==MOVE_DROPB)
//                			  && (m2.to_col==m.from_col)
//                			  && (m2.to_row==m.from_row))
//                	  {	// sequence is pick/drop/pick/drop, edit out the middle pick/drop
//                		UndoHistoryElement(idx);
//                	  	UndoHistoryElement(idx-1);
//                	  	idx = idx-2;
//                	  }
//                	  else { idx = -1; }
//                		
//                	}
//                	else { idx = -1; }
//                	}
//                	else { idx = -1; }
//                	break;
//                	
//            	}
//               }
//            G.Assert(idx!=start_idx,"progress editing history");
//            }
//         return (rval);
//    }
//
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
    case MOVE_CAPTURE:
    case MOVE_ORDO:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
       	 break;
     case MOVE_PICK:
    	 playASoundClip(light_drop,100);
    	 break;
    case MOVE_SELECT:
    case MOVE_RETAIN:
    case MOVE_PICKB:
    	playASoundClip(light_drop,100);
    	break;
    case MOVE_DROP:
    	break;
    case MOVE_DROPB:
      	 playASoundClip(heavy_drop,100);
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
    	if (hp.hitCode instanceof OrdoId) // not dragging anything yet, so maybe start
        {
        OrdoId hitObject = (OrdoId)hp.hitCode;
		OrdoCell cell = hitCell(hp);
		OrdoChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case Black_Chip_Pool:
	    	PerformAndTransmit("Pick B "+cell.row+" "+chip.id.shortName);
	    	break;
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick W "+cell.row+" "+chip.id.shortName);
	    	break;
	    case BoardLocation:
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
    	if(!(id instanceof OrdoId)) 
    		{ // handle all the actions that aren't ours
    			missedOneClick = performStandardActions(hp,missedOneClick); 
    		}
    	else {
    	missedOneClick = false;
        OrdoId hitObject = (OrdoId)id;
		OrdoState state = b.getState();
		OrdoCell cell = hitCell(hp);
		OrdoChip chip = (cell==null) ? null : cell.topChip();
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
        	{
        	switch(state)
        	{
        	default: throw G.Error("Not expecting drop on filled board in state %s",state);
        	case Puzzle:
        		if(b.movingObjectIndex()>=0)
				{
        			PerformAndTransmit("Dropb "+cell.col+" "+cell.row); 
				}
				else if(chip!=null)
				{
					PerformAndTransmit("pickb "+cell.col+" "+cell.row+" "+chip.id.shortName);
				}
        		break;
			case OrdoRetain:
				PerformAndTransmit("Retain "+cell.col+" "+cell.row+" "+chip.id.shortName);
				break;
        	case OrdoPlay:
			case Reconnect:
			case OrdoPlay2:
        	case Confirm:
        		if(chip==b.playerChip(b.whoseTurn))
        		{
        			PerformAndTransmit("Select "+cell.col+" "+cell.row+" "+chip.id.shortName);
        		}
        		else
        		{ Hashtable<OrdoCell,OrdoMovespec>moves = b.getTargets();
				  OrdoMovespec m = moves.get(b.getCell(cell));
				  PerformAndTransmit(m.moveString());
				}
        		break;
        		}
        	}
        	
			break;
			
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                	case Puzzle:
            		PerformAndTransmit("Drop "+col);
            		break;
            	}
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
      
      
      // erase
     OrdoChip.backgroundTile.image.tileImage(gc, fullRect);   
      //gc.setColor(Color.black);
      //GC.fillRect(gc, boardRect);
      drawFixedBoard(gc);
    }
    public void drawFixedBoard(Graphics gc,Rectangle rect)
    { OrdoBoard gb = disB(gc);
    	setBoardRect(gb);	// set up the coordinate system for the board
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      if(reviewBackground)
      {	 
       OrdoChip.backgroundReviewTile.image.tileImage(gc,rect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);
      // in this implementation, the checker squares of the board are actually
      // items in the cells, so they are drawn in the drawBoardElements method
      //
      
      gb.DrawGrid(gc,rect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    } 
    
    private void setBoardRect(OrdoBoard gb)
    {
	    gb.SetDisplayParameters(contextRotation==0 ? 1 : 0.9,1.0,  0.12,0.1,  0);
	    gb.SetDisplayRectangle(boardRect);
       
    }
    /**
     * this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     */
//    public void drawCanvas(Graphics offGC, boolean complete,HitPoint hp)
//   {
//     	drawFixedElements(offGC,complete);
//   	
//    	// draw the board contents and changing elements.
//        redrawBoard(offGC,hp);
//        //      draw clocks, sprites, and other ephemera
//        drawClocksAndMice(offGC, null);
//
//        DrawArrow(offGC,hp);
// 
//
//    }

    /** these are drawn separately, directly on the canvas.  They
    might seem to flash on and off.
    */
 //   public void drawCanvasSprites(Graphics offGC, HitPoint hp)
 //   {
 //       DrawTileSprite(offGC,hp); //draw the floating tile, if present
 //       drawSprites(offGC);
 //   }
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
    public String sgfGameType() { return(Ordo_SGF); }

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
    	// make the number of players, random key, and revision part of the standard initialization,
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
    public boolean handleDeferredEvent(Object target, String command)
    {
    	if(target==offerDrawAction)
    	{	if(OurMove() 
    			&& (b.movingObjectIndex()<=0)
    			&& ((b.getState()==OrdoState.OrdoPlay)||(b.getState()==OrdoState.DrawPending)))
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
    	return(super.handleDeferredEvent(target,command));
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
    //public commonPlayer startRobot(commonPlayer p,commonPlayer runner,Bot bot)
    //{	// this is what the standard method does:
    	// int level = sharedInfo.getInt(sharedInfo.ROBOTLEVEL,0);
    	// RobotProtocol rr = newRobotPlayer();
    	// rr.InitRobot(sharedInfo, getBoard(), null, level);
    	// p.startRobot(rr);
    //	return(super.startRobot(p,runner,bot));
    //}

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new OrdoPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 182 files visited 0 problems
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
                adjustPlayers(np);
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (name.equals(game_property) && value.equalsIgnoreCase("checker"))
            {	// the equals sgfGameType() case is handled in replayStandardProps
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

