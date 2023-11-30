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
package colorito;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;

import common.GameInfo;
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Toggle;

import static colorito.ColoritoMovespec.*;

/**
 * This code shows the overall structure appropriate for a game view window.
*/
public class ColoritoViewer extends CCanvas<ColoritoCell,ColoritoBoard> implements ColoritoConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final String Colorito_SGF = "Colorito"; // sgf game name
	 
    // file names for jpeg images and masks
    static final String ImageDir = "/colorito/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "colorito-icon-nomask",
    	  };
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] textures = null;// background textures
    
    // private state
    private ColoritoBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("chip",2);

    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;
   
    private Rectangle repRect = addRect("repRect");
    private Toggle eyeRect = new Toggle(this,"eye",
			StockArt.NoEye,ColoritoId.ToggleEye,NoeyeExplanation,
			StockArt.Eye,ColoritoId.ToggleEye,EyeExplanation
			);


    public synchronized void preloadImages()
    {	
       	ColoritoChip.preloadImages(loader,ImageDir);
        if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
    	}
        gameIcon = textures[ICON_INDEX];
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

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new ColoritoBoard(info.getString(GameInfo.GAMETYPE, Variation.Colorito_10.name),randomKey,
        		players_in_game,repeatedPositions,getStartingColorMap());
        useDirectDrawing(true);
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        int np = b.nPlayers();
        b.doInit(b.gametype,b.randomKey,np);			// initialize the board
        if(!preserve_history)
    	{ 
         	startFirstPlayer();
    	}

    }
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle box = pl.createRectangularPictureGroup(x+3*unitsize,y,2*unitsize/3);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, unitsize*2, unitsize*2);
    	int doneW = plannedSeating() ? unitsize*3 : 0;
    	if(flatten)
    	{
    		G.SetRect(done, G.Right(box)+unitsize/3,y, doneW,doneW/2);
    	}
    	else
    	{
    	G.SetRect(done, x, y+unitsize*2, doneW,doneW/2);
    	}
    	pl.displayRotation = rotation;
    	G.union(box, done,chip);
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
    	int minLogW = fh*15;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        int nrows = b.boardRows;
        int ncols = b.boardColumns;
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.6,	// 60% of space allocated to the board
    			0.9,	// aspect ratio for the board
    			fh*3,
    			fh*3.5,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        boolean rotate = seatingFaceToFaceRotated();
       // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	SQUARESIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*SQUARESIZE);
    	int boardH = (int)(nrows*SQUARESIZE);
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
        
        G.placeStateRow(stateX,stateY,boardW,stateH,iconRect,stateRect,annotationMenu,reverseViewRect,eyeRect,noChatRect);
        
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	if(rotate)
    	{
    		G.setRotation(boardRect,-Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,rackBackGroundColor);
        return boardW*boardH;
    }

    
	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	ColoritoChip king = ColoritoChip.getChip(b.reverseY()?0:2);
    	ColoritoChip reverse = ColoritoChip.getChip(b.reverseY()?2:0);
    	int h = G.Height(r);
    	int w = G.Width(r);
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	reverse.drawChip(gc,this,w,cx,cy+h/4,null);
    	king.drawChip(gc,this,w,cx,cy-h/4,null);
    	HitPoint.setHelpText(highlight,r,ColoritoId.ReverseViewButton,s.get(ReverseViewExplanation));
     }  

	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, ColoritoBoard gb, int forPlayer, Rectangle r)
    {	ColoritoCell chips[]= gb.rack;
        ColoritoCell thisCell = chips[forPlayer];
        thisCell.drawStack(gc,this,null,G.Width(r),G.centerX(r),G.centerY(r),0,0.2,null);
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
    	ColoritoChip ch = b.pickedObject;// Tiles have zero offset
    	if(ch!=null) { ch.drawChip(g,this,SQUARESIZE,xp,yp,null); }
     }


    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	
       textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
        drawFixedBoard(gc);
    }
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	ColoritoBoard gb = disB(gc);
    	boolean reviewBackground = reviewMode()&&!mutable_game_record;
    	if(reviewBackground)
    	{	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
    	}
	    gb.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
	    gb.SetDisplayRectangle(brect);
     
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);

      gb.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, ColoritoBoard gb, Rectangle brect, HitPoint highlight)
    {
    	Hashtable<ColoritoCell,ColoritoCell> dests = gb.getDests();
    	Hashtable<ColoritoCell,ColoritoCell> sources = gb.getSources();
    	ColoritoCell src = gb.getSource();
    	boolean show = eyeRect.isOnNow();
     	//
        // now draw the contents of the board and anything it is pointing at
        //
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
    	Enumeration<ColoritoCell> cells = gb.getIterator(Itype.LRTB);
    	while( cells.hasMoreElements())
    	{
            ColoritoCell cell = cells.nextElement();
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            String number = cell.number>0 ? ""+cell.number : null;
            boolean canHit = gb.LegalToHitBoard(cell,sources,dests);
            if( cell.drawStack(gc,this,canHit? highlight : null,SQUARESIZE,xpos,ypos,0,0.1,number)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	highlight.arrow =hasMovingObject(highlight) 
          				? StockArt.DownArrow 
          				: cell.topChip()!=null?StockArt.UpArrow:null;
            	highlight.awidth = SQUARESIZE/3;
            	highlight.spriteColor = Color.red;            	
             	}
            if((canHit && show) || (cell==src))
            {
            StockArt.Dot.drawChip(gc, this, SQUARESIZE, xpos,ypos,null);        	
            }
           	if(dests.get(cell)!=null) 
    		{
    		StockArt.SmallO.drawChip(gc, this, SQUARESIZE, xpos,ypos,null);
    		}
    	}

    }
    public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  
       DrawReverseMarker(gc,reverseViewRect,highlight);
       eyeRect.activateOnMouse = true;
       eyeRect.draw(gc,highlight);
    }
    public void setDisplayParameters(ColoritoBoard gb,Rectangle r)
    {
    	gb.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
		gb.SetDisplayRectangle(r);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  ColoritoBoard gb = disB(gc);
      int whoseTurn = gb.whoseTurn;
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      ColoritoState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
      GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
      drawBoardElements(gc, gb, boardRect, ot);
      GC.unsetRotatedContext(gc,highlight);
      boolean planned = plannedSeating();
      for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
        {	commonPlayer pl = getPlayerOrTemp(i);
        	pl.setRotatedContext(gc, highlight,false);
            DrawCommonChipPool(gc, gb,i,chipRects[i]);
            if(planned && (i==whoseTurn))
            {
            	handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
    					HighlightColor, rackBackGroundColor);	
            }
            pl.setRotatedContext(gc, highlight,true);
        }
        
      commonPlayer pl = getPlayerOrTemp(whoseTurn);
      double messageRotation = pl.messageRotation();
       
        GC.setFont(gc,standardBoldFont());
		if (vstate != ColoritoState.Puzzle)
        {
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==ColoritoState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);

        standardGameMessage(gc,messageRotation,
            		vstate==ColoritoState.Gameover
            			?gameOverMessage(gb)
            			:s.get(vstate.getDescription()),
            				vstate!=ColoritoState.Puzzle,
            				whoseTurn,
            				stateRect);
        DrawCommonChipPool(gc, gb,whoseTurn,iconRect);
        goalAndProgressMessage(gc,ourSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
        DrawRepRect(gc,messageRotation,Color.black,gb.Digest(),repRect);
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
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Chained);
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
        return (new ColoritoMovespec(st, player));
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
//    	ColoritoMovespec newmove = (ColoritoMovespec) nmove;
//    	ColoritoMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            ColoritoMovespec m = (ColoritoMovespec) History.elementAt(idx);
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
//                	{ ColoritoMovespec m2 = (ColoritoMovespec)History.elementAt(idx-1);
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
 
private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_BOARD_BOARD:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
   	break;
    case MOVE_PICKB:
    	playASoundClip(light_drop,100);
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
    	if(hp.hitCode instanceof ColoritoId)
        {
        ColoritoId hitObject = (ColoritoId)hp.hitCode;
		ColoritoCell cell = hitCell(hp);
		ColoritoChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case LiftRect:
 	    case Black_Chip_Pool:
	    	PerformAndTransmit("Pick B "+cell.row+" "+chip.chipNumber());
	    	break;
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick W "+cell.row+" "+chip.chipNumber());
	    	break;
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(cell.chipIndex>0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
	    		}
	    	break;
		case ReverseViewButton:
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
    {
       
        CellId id = hp.hitCode;
        if(!(id instanceof ColoritoId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	ColoritoId hitObject = (ColoritoId)id;
		ColoritoState state = b.getState();
		ColoritoCell cell = hitCell(hp);
		ColoritoChip chip = (cell==null) ? null : cell.topChip();
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

        case LiftRect:
        	break;
         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Confirm:
			case Play:
			case Puzzle:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
				}
				break;
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
        }
        }
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
    	return(""+b.gametype+" "+b.randomKey+" "+b.nPlayers()); 
   }
    public String sgfGameType() { return(Colorito_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long rk = G.LongToken(his);
    	int np = G.IntToken(his);
    	// make the random key part of the standard initialization,
    	// even though games like colorito probably don't use it.
        b.doInit(token,rk);
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
    	if(target==reverseOption)
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
    public SimpleRobotProtocol newRobotPlayer() { return(new ColoritoPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary:
		27: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\colorito\coloritogames\coloritogames\archive-2017\games-Jun-23-2017.zip CL-Dumbot-guest-2016-05-05-2011.sgf lib.ErrorX: Not expecting drop in state Gameover
		32: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\colorito\coloritogames\coloritogames\archive-2017\games-Jun-23-2017.zip CL-Dumbot-mosquito-2014-06-12-1652.sgf lib.ErrorX: Not expecting drop in state Gameover
		54: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\colorito\coloritogames\coloritogames\archive-2017\games-Jun-23-2017.zip U!CL-barmaley-Dumbot-2016-03-27-0841.sgf lib.ErrorX: Not expecting drop in state Gameover
		64: play Problem in zip file:G:\share\projects\boardspace-html\htdocs\colorito\coloritogames\coloritogames\archive-2017\games-Jun-23-2017.zip U!CL-guest-Dumbot-2014-05-05-1123.sgf lib.ErrorX: Not expecting robot in state Gameover
		253: play Problem in file:G:\share\projects\boardspace-html\htdocs\colorito\coloritogames\coloritogames\games-Jun-23-2017\CL-Dumbot-guest-2016-05-05-2011.sgf lib.ErrorX: Not expecting drop in state Gameover
		258: play Problem in file:G:\share\projects\boardspace-html\htdocs\colorito\coloritogames\coloritogames\games-Jun-23-2017\CL-Dumbot-mosquito-2014-06-12-1652.sgf lib.ErrorX: Not expecting drop in state Gameover
		280: play Problem in file:G:\share\projects\boardspace-html\htdocs\colorito\coloritogames\coloritogames\games-Jun-23-2017\U!CL-barmaley-Dumbot-2016-03-27-0841.sgf lib.ErrorX: Not expecting drop in state Gameover
		290: play Problem in file:G:\share\projects\boardspace-html\htdocs\colorito\coloritogames\coloritogames\games-Jun-23-2017\U!CL-guest-Dumbot-2014-05-05-1123.sgf lib.ErrorX: Not expecting robot in state Gameover
		
		304 files visited 8 problems
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
                b.doInit(typ,ran);
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

