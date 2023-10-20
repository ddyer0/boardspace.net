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
package stac;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import common.GameInfo;
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.Image;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.StockArt;

import static stac.StacMovespec.*;

/**
 * This code shows the overall structure appropriate for a game view window.
*/
public class StacViewer extends CCanvas<StacCell,StacBoard> implements StacConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(164,175,190);
    private Color boardBackgroundColor = new Color(220,165,155);
    private Color chatBackGroundColor = new Color(220,220,250);
    
    // private state
    private StacBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    private double CHIPSCALE = 0.7;
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;
    private JMenuItem offerDrawAction = null;
    private Rectangle repRect = addRect("repRect");
    private Rectangle declineDrawRect = addRect("declineDraw");
    private Rectangle acceptDrawRect = addRect("acceptDraw");
    private Rectangle chipRects[] = addRect("chip",2);
    
 
    public synchronized void preloadImages()
    {	
       	StacChip.preloadImages(loader,ImageDir);
       	gameIcon = StacChip.board.image;
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
        	InternationalStrings.put(StacStrings);
        	InternationalStrings.put(StacStringPairs);
        }

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new StacBoard(info.getString(GameInfo.GAMETYPE, Variation.Stac.name),
        		randomKey,players_in_game,repeatedPositions,getStartingColorMap(),StacBoard.REVISION);
        useDirectDrawing(true);
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        offerDrawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);
        MouseColors = new Color[]{Color.red,Color.blue};
        MouseDotColors = new Color[]{Color.white,Color.white};
        
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
        int nrows = 15;  // b.boardRows
        int ncols = 15;	 // b.boardColumns
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*2,	// minimum cell size
    			fh*3,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
       	layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);
       	layout.placeTheVcr(this,minLogW,minLogW*3/2);
        
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	SQUARESIZE = (int)(cs*3);
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
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,liftRect,reverseViewRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackGroundColor,rackBackGroundColor);
 	
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	G.SetRect(chip,	x,	y,	3*unitsize,	4*unitsize);
    	Rectangle box =  pl.createRectangularPictureGroup(x+3*unitsize,y,unitsize);
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*4 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.union(box, done,chip);
    	pl.displayRotation = rotation;
    	return(box);
    }
 
 	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	StacChip king = StacChip.getChip(b.reverseY()?1:0);
    	StacChip reverse = StacChip.getChip(b.reverseY()?0:1);
    	int w = G.Width(r);
    	int t = G.Top(r);
    	int cx = G.centerX(r);
    	reverse.drawChip(gc,this,w,cx,t+w/4,null);
    	king.drawChip(gc,this,w,cx,t+w/4*3,null);
    	HitPoint.setHelpText(highlight,r,StacId.ReverseViewButton, s.get(ReverseViewExplanation));
     }  

	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, StacBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	StacCell chips[]= gb.rack;
        boolean canHit = gb.LegalToHitChips(forPlayer);
        StacCell thisCell = chips[forPlayer];
        StacChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = ""+thisCell.height();
        int sz = (int)(G.Width(r)*CHIPSCALE);
        if(thisCell.drawStack(gc,this,pt,sz,G.centerX(r),G.centerY(r),0,0.1,msg))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth =sz/3;
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
    	int carry = obj/100;
    	int main = obj%100;
    	if(carry>0) { StacChip.getChipNumber(carry).drawChip(g,this,(int)(SQUARESIZE*CHIPSCALE),xp,yp,null); }
    	StacChip.getChipNumber(main).drawChip(g,this,(int)(SQUARESIZE*CHIPSCALE),xp,yp,null);
     }


    Image scaled = null;
    
    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	boolean reviewBackground = reviewMode()&&!mutable_game_record;
      // erase
      StacBoard gb = disB(gc);
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
     StacChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       StacChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      scaled = StacChip.board.getImage(loader).centerScaledImage(gc, boardRect,scaled);
      /*
	 	    gb.SetDisplayParameters(
	 	    		new double[]{0.20,0.33},
	 	    		new double[]{.18,0.77},
	 	    		new double[]{0.96,0.375},
	 	    		new double[]{.89,.83});
	 	    		*/
      gb.SetDisplayParameters(0.79,0.55,0.0,1.4,3.0,0.09,0,0.05);

      gb.SetDisplayRectangle(boardRect);
      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
     private void drawBoardElements(Graphics gc, StacBoard gb, Rectangle brect, HitPoint highlight)
    {	Hashtable<StacCell,StacMovespec>targets = gb.getTargets();
     	boolean dolift = doLiftAnimation();
   
     	//
        // now draw the contents of the board and anything it is pointing at
        //
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
        double xstep = 0.03;
        double ystep = 0.08;
        Enumeration<StacCell> cells = gb.getIterator(Itype.LRTB);
        while (cells.hasMoreElements())
        {
            StacCell cell = cells.nextElement();
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            HitPoint hitNow = !dolift && gb.legalToHitBoard(cell,targets) ? highlight : null;
            if( cell.drawStack(gc,this,hitNow,(int)(SQUARESIZE*CHIPSCALE),xpos,ypos,liftSteps,xstep,ystep,null)) 
            	{ 
           		// draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
             	boolean moving = hasMovingObject(highlight);
         		int boxSize = CELLSIZE;
                	highlight.arrow =moving 
      				? StockArt.DownArrow 
          				: cell.topChip()!=null?StockArt.UpArrow:null;
                	highlight.awidth = SQUARESIZE/3;
                	highlight.spriteColor = Color.red;  
            	if(!moving)
            	{	// check for a carry move rather than a pawn move
                		StacMovespec m = (StacMovespec)targets.get(cell);
                	int xp = highlight.hit_x;
                	int yp = highlight.hit_y;
            		boolean movingMeeple = (m!=null)
                					? ((m.op!=MOVE_CARRY) || G.pointInside(highlight,xp,yp-CELLSIZE/2,CELLSIZE))
                					: gb.isDest(cell) ? gb.pickedDisk==null : false;
                    if(movingMeeple)
            			{	// close in, leave this as a simple pick and make the box small
            				boxSize = 2*CELLSIZE/3;
             			}
            			else 
            			{
                			highlight.hitCode = StacId.ChipLocation; 
            			}
                	highlight.awidth = boxSize;
                	         	
                	}
        	}
    	}
 
    }
    public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  drawLiftRect(gc,liftRect,highlight,StacChip.liftIcon.image);
       DrawReverseMarker(gc,reverseViewRect,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  StacBoard gb = disB(gc);

      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      StacState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        
        boolean planned = plannedSeating();
        
        for(int player=0;player<2;player++)
        	{ commonPlayer pl = getPlayerOrTemp(player);
        	  pl.setRotatedContext(gc, highlight,false);
        	  DrawCommonChipPool(gc, gb,player,chipRects[player], gb.whoseTurn,ot);
     	   if(planned && gb.whoseTurn==player)
     	   {
     		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? ot : null), 
    					HighlightColor, rackBackGroundColor);
     	   }
        	   pl.setRotatedContext(gc, highlight,true);
        	}
  
        GC.setFont(gc,standardBoldFont());
       
        switch(vstate)
        {
        default:
        	if((b.moveNumber-b.lastStackMove)>(5*b.nSingleChips))
        	{	// if not making progress, put the draw option on the UI
            	if(GC.handleSquareButton(gc,acceptDrawRect,select,s.get(OFFERDRAW),
            			HighlightColor,
            			vstate==StacState.DrawPending?HighlightColor : rackBackGroundColor))
            	{
            		select.hitCode = GameId.HitOfferDrawButton;
            	}
       		
        	}
        	break;
        case AcceptOrDecline:
        case AcceptPending:
        case DeclinePending:
        	if(GC.handleSquareButton(gc,acceptDrawRect,select,s.get(ACCEPTDRAW),HighlightColor,rackBackGroundColor))
        	{
        		select.hitCode = GameId.HitAcceptDrawButton;
        	}
        	if(GC.handleSquareButton(gc,declineDrawRect,select,s.get(DECLINEDRAW),HighlightColor,rackBackGroundColor))
        	{
        		select.hitCode = GameId.HitDeclineDrawButton;
        	}
       	break;
        }
        
 		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
 		double messageRotation = pl.messageRotation();
 
 		if (vstate != StacState.Puzzle && !autoDoneActive())
        {
			handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
			
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==StacState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);


		standardGameMessage(gc,messageRotation,
            		vstate==StacState.Gameover
            			?gameOverMessage()
            			:s.get(vstate.getDescription()),
            				vstate!=StacState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
            goalAndProgressMessage(gc,ourSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
        gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
        
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        drawAuxControls(gc,ourSelect);
        drawVcrGroup(ourSelect, gc);

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
        startBoardAnimations(replay,b.animationStack,(int)(SQUARESIZE*CHIPSCALE),MovementStyle.Stack);
        if(replay!=replayMode.Replay) { playSounds(mm); }
 
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
        return (new StacMovespec(st, player));
    }
    
    
private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_CARRY:
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
        if (hp.hitCode instanceof StacId) // not dragging anything yet, so maybe start
        {
        StacId hitObject = (StacId)hp.hitCode;
		StacCell cell = hitCell(hp);
		StacChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case Blue_Chip_Pool:
	    	PerformAndTransmit("Pick Blue");
	    	break;
	    case Red_Chip_Pool:
	    	PerformAndTransmit("Pick Red");
	    	break;
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(cell.height()>0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row);
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
    	if(!(id instanceof StacId)) 
    		{ // handle all the actions that aren't ours
    			missedOneClick = performStandardActions(hp,missedOneClick); 
    		}
    	else {
    	missedOneClick = false;
        StacId hitObject = (StacId)id;
		StacState state = b.getState();
		StacCell cell = hitCell(hp);
		StacChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);

        case ReverseViewButton:
       	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
       	 generalRefresh();
       	 break;

         case ChipLocation:	// hit the board, looking to carry
         	{
         		if(b.getState()==StacState.Puzzle) { PerformAndTransmit("Pickb "+cell.col+" "+cell.row); }
         		else { PerformAndTransmit( "Pickc "+cell.col+" "+cell.row); }
         	}
         	break;
         case BoardLocation:	// we hit the board 
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row);
				}
				break;
			
        case Red_Chip_Pool:
        case Blue_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                case Play:
            		PerformAndTransmit(RESET);
            		break;

               	case Puzzle:
            		PerformAndTransmit("Drop"+col+cell.row+" "+mov);
            		break;
            	}
			}
         	}
            break;

        }}
    }

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
    public String sgfGameType() { return(Stac_SGF); }

  
    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a stac init spec
    	long rk = G.LongToken(his);
    	int np = G.IntToken(his);
    	int rev = G.IntToken(his);
    	// make the random key part of the standard initialization,
    	// even though games like stac probably don't use it.
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
    {	if(target==offerDrawAction)
    	{
    		if(OurMove() 
    			&& (b.movingObjectIndex()<=0)
    			&& ((b.getState()==StacState.Play)||(b.getState()==StacState.Carry)||(b.getState()==StacState.DrawPending)))
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



    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new StacPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 464 files visited 0 problems
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
}

