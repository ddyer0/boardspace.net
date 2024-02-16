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
package gobblet;

import bridge.*;

import com.codename1.ui.geom.Rectangle;


import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import bridge.Config;
import common.GameInfo;
import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ChatInterface;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.SoundManager;
import lib.StockArt;


/**
 * 
 * Change History
 *
 * Feb 2006 initial work in progress. 
 * May 2006 Complete version released
 *
*/
public class GobGameViewer extends CCanvas<GobCell,GobGameBoard> implements GobConstants
{
     /**
	 * 
	 */
    static final String Gobblet_SGF = "26"; // sgf game name

    
    // file names for jpeg images and masks
    static final String GobImageDir = "/gobblet/images/";
	// sounds
	static final String NESTED_PICK_SOUND = SOUNDPATH+ "pick-3" + Config.SoundFormat;
	
    static final int BOARD_INDEX = 0;
    static final String[] ImageFileNames = 
        {   "board-overhead",
            "board-oblique"
        };
    
     static final double[][]BOARDSCALES = {
	 {0.5,0.5,1.0}, // board
     };

    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int OVERHEAD_BOARD_ICON = 2;
    static final int OBLIQUE_BOARD_ICON = 3;
    static final int LIFT_ICON_INDEX = 4;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    		"background-review-tile",
    		"overhead-board-icon",
    		"oblique-board-icon",
    		"lift-icon"};	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] images = null; // images of black and white gobblets and board
    private static Image[] textures = null;// background textures
    
    // private state
    private GobGameBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle boardIconRect = addRect("boardIcon");	// the icon to switch perspective
    private Rectangle repRect = addRect("repRect");
    private Rectangle chipRects[] = addRect("chip",2);

    private boolean use_perspective() { return(getAltChipset()==0); };
    

    public synchronized void preloadImages()
    {	GobCup.preloadImages(loader,GobImageDir);
    	if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    	SoundManager.preloadSounds(NESTED_PICK_SOUND);
        images = loader.load_images(GobImageDir, ImageFileNames, 
        		loader.load_images(GobImageDir, ImageFileNames,"-mask")); // load the main images
        textures = loader.load_images(GobImageDir,TextureNames);
    	}
    	gameIcon = images[1];
    }
    public boolean allowBackwardStep()
    {
    	return(super.allowBackwardStep() && b.canUndoPick());
    }

    public boolean allowPartialUndo()
    {
    	return(super.allowPartialUndo() && b.canUndoPick());
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	enableAutoDone = true;
        super.init(info,frame);
        b = new GobGameBoard(info.getString(GameInfo.GAMETYPE, "Gobblet"),getStartingColorMap());   
        useDirectDrawing(true);
        doInit(false);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype);						// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
   }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*14;
    	int chipH = unitsize*6;
    	boolean planned = plannedSeating();
    	int doneW = planned ? unitsize*5 : 0;
    	Rectangle box = pl.createRectangularPictureGroup(x,y,unitsize);
    	Rectangle done = doneRects[player];
    	G.SetRect(done, G.Right(box)+unitsize/2,y+unitsize/2,doneW,doneW/2);
    	if(flatten)
    	{
    	G.SetRect(chip, G.Right(done)+unitsize/2, y, chipW, chipH);
    	}
    	else
    	{
    	G.SetRect(chip, x, G.Bottom(box), chipW, chipH);
    	}
    	pl.displayRotation = rotation;
    	
    	G.union(box, chip,done);
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
    	int minLogW = fh*20;	
    	int vcrW = fh*15;
       	int minChatW = fh*30;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        int nrows = b.ncols;
        int ncols = b.ncols;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// 1:1 aspect ratio for the board
    			fh*2.2,	// maximum cell size
    			0.4	// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,vcrW,3*vcrW/2 );

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	int CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE;
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
        
        G.placeStateRow( stateX,stateY,boardW ,stateH, iconRect,stateRect,annotationMenu,liftRect,boardIconRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        return boardW*boardH;
          }


	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawChipPool(Graphics gc, Rectangle r, int player,
        HitPoint highlight)
    {	GobCell cells[]= b.rack[player];
        boolean canhit = b.LegalToHitChips(player) && G.pointInRect(highlight, r);
        boolean canDrop = hasMovingObject(highlight);
		commonPlayer pl = getPlayerOrTemp(player);
        int nCells = cells.length;
        int cellW = G.Width(r)/nCells;
        GC.frameRect(gc, Color.black, r);
        int rl = G.Left(r);
        int top = G.centerY(r)+G.Height(r)/8;
        int size = G.Height(r);
      	for(int i=0,cellX = cellW/3;i<nCells;i++,cellX+=cellW)
        {	GobCell thisCell = cells[i];
        	if(thisCell!=null)
        	{	GobCup topCup = thisCell.topChip();
	    		int left = rl+cellX;
	       		if(canhit 
	       				&& (canDrop ? b.canDropOn(thisCell) : (topCup!=null))
	       				&& thisCell.closestPointToCell(highlight,size,left,top)
	       				)
	    		{ highlight.arrow = canDrop ? StockArt.DownArrow:StockArt.UpArrow;
	    		  highlight.awidth = size/3;
	      		  highlight.spriteColor = Color.red;
	    		}
        		if(topCup!=null)
        		{
        		int cx = G.Left(r)+cellX;
        		int cy = top;
        		pl.rotateCurrentCenter(thisCell,cx,cy);
        		topCup.drawChip(gc,this,size,cx,cy,null);
  	       		}
        	}
        }
    }
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {	GobCup sprite = GobCup.getCup(obj);
    	sprite.drawChip(g,this,SQUARESIZE,xp,yp,null);
    }
 
    Image scaled = null;
    Image background = null;

    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      // erase
      GobGameBoard gb = disB(gc);
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);   
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid
      Image board = images[BOARD_INDEX+(use_perspective()?1:0)];
      if(board!=background) { scaled = null; }
      background = board;
      scaled = board.centerScaledImage(gc, boardRect,scaled);
      if (use_perspective())
	    {	// this are purely emphirical magic numbers to match the grid to the actual artwork.
	        gb.SetDisplayParameters(0.72, 1.0,   0.20,0.3, 0, 0.22,0.30,0);
	    }
	    else
	    {
	    	gb.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
	    }
      gb.SetDisplayRectangle(boardRect);
      
      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
     textures[use_perspective()?OVERHEAD_BOARD_ICON:OBLIQUE_BOARD_ICON].centerImage(gc,
    		  boardIconRect);
    }
 
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, GobGameBoard gb, Rectangle brect, HitPoint highlight)
    {
     	int liftdiv = 40;
     	GobCell hitCell = null;
     	boolean dolift = doLiftAnimation();
     	
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	int left = G.Left(brect);
     	int top = G.Bottom(brect);
     	for(Enumeration<GobCell> cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements();)
     	{	GobCell cell = cells.nextElement();
     		int ypos = top - gb.cellToY(cell);
            int xpos = left + gb.cellToX(cell);
                int topindex = cell.chipIndex;
                cell.rotateCurrentCenter(gc,xpos,ypos);
                for(int cindex = dolift?0:topindex; cindex<=topindex; cindex++)
                {
                GobCup cup = cell.chipAtIndex(cindex - cell.activeAnimationHeight());
                if(cup!=null)
                {	int liftYval = (!use_perspective()&&dolift)?((liftSteps*SQUARESIZE)/(2*liftdiv))*cindex : 0;
                	int liftXval = (dolift&&use_perspective())?((liftSteps*SQUARESIZE)/(2*liftdiv))*cindex : 0;
                	cup.drawChip(gc,this,SQUARESIZE,xpos+liftXval,ypos+liftYval,null);
                  }}
              boolean hitpoint = (highlight!=null)
            		  			&& gb.LegalToHitBoard(cell)
	  			&& cell.closestPointToCell(highlight,SQUARESIZE, xpos, ypos)
            		  			;
              if(hitpoint) 
              	{ hitCell = cell;
              	}
             }

    	if(hitCell!=null)
    	{ 
    	highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
      	highlight.awidth = SQUARESIZE/3;
      	highlight.spriteColor = Color.red;
        }
    }
    boolean peeking() { return(allowed_to_edit || !b.MEMORY || mutable_game_record); }
    
    public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  if(peeking()) { drawLiftRect(gc,liftRect,highlight,textures[LIFT_ICON_INDEX]); }
       if(G.pointInRect(highlight,boardIconRect)) 
       	{ highlight.hitCode = use_perspective() ? GameId.FacingView : GameId.NormalView; 
       	  highlight.spriteRect = boardIconRect;
       	  highlight.spriteColor = Color.red;
    		}
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  GobGameBoard gb = disB(gc);

      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      boolean review = reviewMode();
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !review) ? null : highlight;	// hit if not dragging
      GobbletState vstate = gb.getState();
       if(peeking())
        	{ gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
        	}
    
        boolean planned = plannedSeating();
        drawBoardElements(gc, gb, boardRect, ot);
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
        {	commonPlayer pl = getPlayerOrTemp(i);
        	pl.setRotatedContext(gc, highlight, false);
        	DrawChipPool(gc, chipRects[i],i, ot);
        	if(planned && (i==gb.whoseTurn))
        	{
        		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
    					HighlightColor, rackBackGroundColor);
        	}
        	pl.setRotatedContext(gc, highlight, true);
        }
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();
		 
		GC.setFont(gc,standardBoldFont());
        drawPlayerStuff(gc,(vstate==GobbletState.PUZZLE_STATE),ourSelect,
 	   			HighlightColor, rackBackGroundColor);

		if (vstate != GobbletState.PUZZLE_STATE)
        {	
			if(!planned && !autoDoneActive()) 
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
     
            }
 
       standardGameMessage(gc,messageRotation,
        		vstate==GobbletState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=GobbletState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);

        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        drawAuxControls(gc,ourSelect);
        if(peeking())
    	{ drawVcrGroup(ourSelect, gc);
    	}
        else { drawNoChat(gc,noChatRect,ourSelect); }

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
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Simultaneous);
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
        return (new GobMovespec(st, player));
    }
    
   
private void playPickSound(GobCell c,int delay)
{  	GobCup cup = c.topChip();
	if(cup!=null) { playASoundClip(NESTED_PICK_SOUND,delay); }
}
private void playDropSound(GobCell c,int delay)
{
	playASoundClip((c.chipIndex>0) ? heavy_drop : light_drop,delay); 	
}
private void playSounds(commonMove mm)
{
	GobMovespec m = (GobMovespec) mm;

    // add the sound effects
    switch(m.op)
    {
    case MOVE_RACK_BOARD:
    	playPickSound(b.GetRackCell(m.player,m.from_row),750);
    	playDropSound(b.getCell(m.to_col,m.to_row),500);
    	break;
    case MOVE_BOARD_BOARD:
    	playPickSound(b.getCell(m.from_col,m.from_row),750);
    	playDropSound(b.getCell(m.to_col,m.to_row),500);
    	break;
    case MOVE_PICK:
    	playPickSound(b.GetRackCell(m.player,m.from_row),500);
    	break;
    case MOVE_PICKB:
    	playPickSound(b.getCell(m.from_col,m.from_row),500);
    	break;
    case MOVE_DROP:
    	playDropSound(b.GetRackCell(m.player,m.to_row),500);
    	break;
    case MOVE_DROPB:
      	playDropSound(b.getCell(m.to_col,m.to_row),500);
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
        if (hp.hitCode instanceof GobbletId)// not dragging anything yet, so maybe start
        {
        GobbletId hitObject = (GobbletId)hp.hitCode;
		GobCell cell = hitCell(hp);
		GobCup cup = (cell==null) ? null : cell.topChip();
		if(cup!=null)
		{
	    switch(hitObject)
	    {
 	    case Black_Chip_Pool:
	    	PerformAndTransmit("Pick B "+cell.row+" "+cup.size);
	    	break;
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick W "+cell.row+" "+cup.size);
	    	break;
	    case BoardLocation:
	    	PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+cup.size);
	    	break;
		default:
			break;
        }
		}
        }
    }

	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof GobbletId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	GobbletId hitObject = (GobbletId)hp.hitCode;
        GobbletState state = b.getState();
		GobCell cell = hitCell(hp);
		GobCup cup = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PICKED_STATE:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(cup!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+cup.size);
				}
				break;
			}
			break;
			
			
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col = (hitObject==GobbletId.Black_Chip_Pool) ? " B " : " W ";
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                	case PLAY_STATE:
            		performReset();
            		break;

               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop"+col+cell.row+" "+mov);
            		break;
            	}
			}
         	}
            break;
        }}
    }

    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Gobblet_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        b.doInit(token);
     }
    
    public void doShowSgf()
    {
        if (peeking() || G.debug())
        {
            super.doShowSgf();
        }
        else
        {
            theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                s.get(CensoredGameRecordString));
        }
    }

    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
        return (handled);
    }

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new GobPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
	 * summary: 5/26/2023
	 * 	27532 files visited 0 problems
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
                b.doInit(value);
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

