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
package xiangqi;


import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import common.GameInfo;

import java.awt.*;


import java.util.Hashtable;
import java.util.StringTokenizer;

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
import lib.StockArt;
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

/**
 * 
 * Change History
 *
 * March 2010 Initial work in progress. 
 * 
 *
*/
public class XiangqiViewer extends CCanvas<XiangqiCell,XiangqiBoard> implements XiangqiConstants
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private Color XiangqiMouseColors[] = {Color.red,Color.black};
    private Color XiangqiMouseDotColors[] = { Color.black,Color.white};
	private Color YELLOWDOT = new Color(0.7f,0.7f,0.5f);
	private Color BLUEDOT = new Color(0.4f,0.4f,0.8f);
	private Color GRAYDOT = new Color(0.3f,0.3f,0.3f);
 
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;
    private static StockArt ornaments[] = null;
    // private state
    private XiangqiBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle rackRects[] = addRect("rack",2);

    private Rectangle acceptDrawRect = addRect("acceptDraw");
    private Rectangle declineDrawRect = addRect("declineDraw");
    
    private Rectangle repRect = addRect("repRect");
    private Rectangle altchipRect = addRect("altChip");
    private Rectangle reverseViewRect = addRect("reverse");
    private boolean traditional_chips = false;
    private boolean prerotated = false;
    public int getAltChipset()
    {	int rotate = (!prerotated && seatingFaceToFace())
    					? b.reverseY()?4:2
    					: 0;
    	return (rotate | (traditional_chips?1:0));
    }
    private JCheckBoxMenuItem chipsetOption = null;
    private JCheckBoxMenuItem reverseOption = null;
    
    private JMenuItem drawAction = null;
    public synchronized void preloadImages()
    {	
       	XiangqiChip.preloadImages(loader,ImageDir);
        if (ornaments == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
        ornaments = StockArt.preLoadArt(loader,ImageDir,ExtraImageNames,ExtraImageScale);
    	}
        gameIcon = XiangqiChip.getChip(0).image;
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
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        enableAutoDone = true;
    	super.init(info,frame);
    	MouseDotColors = XiangqiMouseDotColors;
    	MouseColors = XiangqiMouseColors;
    	
        b = new XiangqiBoard(info.getString(GameInfo.GAMETYPE, Xiangqi_INIT),randomKey,
        		repeatedPositions,getStartingColorMap());
        useDirectDrawing(true);
        doInit(false);
        chipsetOption = myFrame.addOption(s.get("Traditional Pieces"),traditional_chips,deferredEvents);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        drawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);						// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
   }

    	
    private double aspect[] = { 0.7,1.0,1.4,-0.7,-1.0,-1.4};
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	setLocalBoundsV(x,y,width,height,aspect);
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double aspect0)
    {	G.SetRect(fullRect, x, y, width, height);
    	double aspect = Math.abs(aspect0);
    	vertical = aspect0>0;
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*16;
    	int vcrW = fh*15;
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
    			aspect,	// 1:1 aspect ratio for the board
    			fh*2,	// maximum cell size
    			0.2		// preference for the designated layout, if any
    			);
    	
        boolean rotate = seatingFaceToFaceRotated();
        int nrows = rotate ? b.boardColumns : b.boardRows;  
        int ncols = rotate ? b.boardRows : b.boardColumns;
       // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
    	layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);
       	layout.placeDoneEdit(buttonW,3*buttonW/2,doneRect,editRect);
       	layout.placeTheVcr(this,vcrW,vcrW*3/2);
       	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
        int stateH = fh*5/2;
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH)/nrows);
    	SQUARESIZE = (int)cs;
     	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(nrows*cs);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH-stateH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH+stateH;
        int boardBottom = boardY + boardH;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH/2;
        int stateX = boardX;

        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,reverseViewRect,noChatRect);
        G.SetRect(boardRect,boardX, boardY, boardW, boardH);         
        G.placeRow(boardX, boardBottom-stateH,boardW,stateH,goalRect,altchipRect);       
        if(rotate)
        {
        	G.setRotation(boardRect, -Math.PI/2);
        	contextRotation = -Math.PI/2;
        }
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        return(boardW*boardH);
    }
    boolean vertical = false;
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unit)
    {	commonPlayer pl0 = getPlayerOrTemp(player);
    	int chipW = unit*3;
    	int rackW = unit*7;
    	int rackH = unit*5;
    	Rectangle chip = chipRects[player];
    	Rectangle rack = rackRects[player];
    	Rectangle done = doneRects[player];
    	Rectangle box = pl0.createRectangularPictureGroup(x+chipW, y, unit);
    	int doneW = plannedSeating()?unit*6:0;
    	G.SetRect(chip, x, y, chipW,chipW);
    	int bb = G.Bottom(box);
    	if(vertical)
    	{
    	G.SetRect(rack, x, bb, rackW,rackH);
    	G.SetRect(done, x+rackW+unit/2, bb+unit/2, doneW , doneW/2);
    	}
    	else
    	{	int boxR = G.Right(box);
        	G.SetRect(done, boxR+unit/2, y+unit/2, doneW, doneW/2); 		
        	G.SetRect(rack, boxR+doneW+doneW/4, y, rackW,rackH);
    	}
    	G.union(box, chip,done,rack);
    	pl0.displayRotation = rotation;
        return(box);
    }
    
	
    private void DrawPlayerMarker(Graphics gc, int forPlayer, Rectangle r)
    {	XiangqiChip king = XiangqiChip.getChip(b.getColorMap()[forPlayer],XiangqiChip.GENERAL_INDEX);
    	king.drawChip(gc,this,r,null);
     }
    private void DrawChipsetMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	XiangqiChip king = XiangqiChip.getChip(0,XiangqiChip.CANNON_INDEX).alt_image;
    	king.drawChip(gc,this,r,null);
    	if(G.pointInRect(highlight,r))
    	{	highlight.spriteRect = r;
    		highlight.spriteColor = Color.red;
    		highlight.hitCode = XId.ChangeChipsetButton;
    	}
     }
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	XiangqiChip king = XiangqiChip.getChip(b.reverseY()?1:0,XiangqiChip.GENERAL_INDEX);
    	XiangqiChip reverse = XiangqiChip.getChip(b.reverseY()?0:1,XiangqiChip.GENERAL_INDEX);
    	int w = 3*G.Width(r)/4;
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	reverse.drawChip(gc,this,w,cx,cy-w/3,null);
    	king.drawChip(gc,this,w,cx,cy+w/3,null);
    	HitPoint.setHelpText(highlight,r, XId.ReverseViewButton,s.get(ReverseViewExplanation));
    	
     }   
  
 	
	
    private void DrawCommonChipPool(Graphics gc, int player, Rectangle r, int forPlayer, HitPoint highlight)
    {	XiangqiCell chips[]= b.rack[player];
        boolean canHit = b.LegalToHitChips(player);
        commonPlayer cp = getPlayerOrTemp(player);
        int xs = G.Width(r)/3;
        int ys = G.Height(r)/2;
        int x = G.Left(r) + xs/2;
        int y = G.Top(r) + ys/2;
        GC.frameRect(gc,Color.black,r);
        for(int i=1; i<chips.length;i++)	// start at 1, 0 is the king
        {
        XiangqiCell thisCell = chips[i];
        XiangqiChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = null;
        thisCell.drawStack(gc,this,pt,ys,x,y,0,0.15,msg);
        cp.rotateCurrentCenter(thisCell,x,y);

        if((highlight!=null) && (highlight.hitObject==thisCell))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth =ys/2;
        }
        x += xs;
        if(x>G.Right(r)) { x = G.Left(r) + xs/2; y += ys; }
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
    	XiangqiChip ch = XiangqiChip.getChip(obj);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);
     }


    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }



    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	
       textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
        drawFixedBoard(gc);
    }
    Image scaled = null;
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { boolean review = reviewMode() && !mutable_game_record;
      XiangqiBoard gb = disB(gc);
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      scaled = images[BOARD_INDEX].centerScaledImage(gc, brect,scaled);
	    gb.SetDisplayParameters(0.91,1.04,  0.16,0.2,  0);
	    gb.SetDisplayRectangle(brect);

      gb.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, XiangqiBoard gb, Rectangle brect, HitPoint highlight)
    {
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	boolean moving = hasMovingObject(highlight);
     	Hashtable<XiangqiCell,XiangqiCell> dests = gb.getDests();
        
       	drawArrow(gc,gb,YELLOWDOT,gb.pickedSource,gb.droppedDest,brect);
       	drawArrow(gc,gb,YELLOWDOT,gb.prevPickedSource,gb.prevDroppedDest,brect);

        // drawing order doesn't matter for these graphics
    	for(XiangqiCell cell = gb.allCells;
    	    cell!=null;
    	    cell =cell.next)
    	{	
            boolean canhit = gb.LegalToHitBoard(cell);
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            if( cell.drawStack(gc,this,canhit?highlight:null,SQUARESIZE,xpos,ypos,0,0.1,null)) 
            	{ highlight.arrow = moving 
      				? StockArt.DownArrow 
      				: cell.height()>0?StockArt.UpArrow:null;
      				highlight.awidth = SQUARESIZE/2;
      				highlight.spriteColor = Color.red;
            	}
            if(moving)
            {
           	int scale = Math.max(2,(int)(0.1 * SQUARESIZE));
             if(dests.get(cell)!=null) 
            	{ GC.cacheAACircle(gc,xpos,ypos-scale/2,scale,BLUEDOT,GRAYDOT,true);
            	}
            if(gb.isSource(cell)) 
            	{ 
            	GC.cacheAACircle(gc,xpos,ypos-scale/2,scale,YELLOWDOT,GRAYDOT,true);
            	}
        	}
    	}    	
    }
    private void drawArrow(Graphics gc,XiangqiBoard gb,Color color,XiangqiCell from,XiangqiCell to,Rectangle brect)
    {
        if((gc!=null) && (from!=null) && (to!=null) && from.onBoard&& to.onBoard)
        {
        int fx = G.Left(brect)+gb.cellToX(from.col,from.row);
        int fy = G.Bottom(brect)-gb.cellToY(from.col,from.row);
        int tx = G.Left(brect)+gb.cellToX(to.col,to.row);
        int ty = G.Bottom(brect)-gb.cellToY(to.col,to.row);
        ornaments[SQUARE_INDEX].drawChip(gc,this,(int)(SQUARESIZE*0.7),fx,fy,null);
        ornaments[SQUARE_INDEX].drawChip(gc,this,(int)(SQUARESIZE*1.2),tx,ty,null);
        //gc.setColor(color);
        //G.DrawArrow(gc, fx, fy, tx, ty, SQUARESIZE / 6);
        }
	
    }


   //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  XiangqiBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
       XiangqiState vstate = gb.getState();
       gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
       	GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
        drawBoardElements(gc, gb, boardRect, ot);
        GC.unsetRotatedContext(gc,highlight);
        
        boolean planned = plannedSeating();
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
          {	commonPlayer pl = getPlayerOrTemp(i);
          	pl.setRotatedContext(gc, highlight, false);
            DrawCommonChipPool(gc, i,rackRects[i], gb.whoseTurn,ot);
            DrawPlayerMarker(gc,i,chipRects[i]);            
          	if(planned && (i==gb.whoseTurn))
        {
          		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
      					HighlightColor, rackBackGroundColor);
    	}       
          	pl.setRotatedContext(gc, highlight, true);
        }
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
        double messageRotation = pl.messageRotation();
        DrawReverseMarker(gc,reverseViewRect,highlight);
        DrawChipsetMarker(gc,altchipRect,highlight);
        
        GC.setFont(gc,standardBoldFont());
		if (vstate != XiangqiState.PUZZLE_STATE)
        {
			if((vstate == XiangqiState.QUERY_DRAW_STATE)
						|| (vstate==XiangqiState.ACCEPT_DRAW_STATE)
						|| (vstate==XiangqiState.DECLINE_DRAW_STATE))
			{
			if(GC.handleRoundButton(gc,messageRotation,acceptDrawRect,select,s.get(ACCEPTDRAW),
					HighlightColor,rackBackGroundColor))
			{ select.hitCode = GameId.HitAcceptDrawButton;
			}
			if(GC.handleRoundButton(gc,messageRotation,declineDrawRect,select,s.get(DECLINEDRAW),
					HighlightColor,rackBackGroundColor))
			{ select.hitCode = GameId.HitDeclineDrawButton;
			}
			}
			else if((vstate==XiangqiState.OFFER_DRAW_STATE) || ( gb.movesSinceProgress()>30)) 
			{	if(GC.handleRoundButton(gc,acceptDrawRect,select,s.get(OFFERDRAW),
					HighlightColor,(XiangqiState.OFFER_DRAW_STATE==vstate)?HighlightColor:rackBackGroundColor))
					{ select.hitCode = GameId.HitOfferDrawButton;
					}
			}
			if(!planned && !autoDoneActive())
			{
			handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
			}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);

 		drawPlayerStuff(gc,(vstate==XiangqiState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);


       	standardGameMessage(gc,messageRotation,
        		vstate==XiangqiState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=XiangqiState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
       	DrawPlayerMarker(gc,gb.whoseTurn,iconRect);   
		goalAndProgressMessage(gc,ourSelect,s.get("Checkmate your opponent's general"),progressRect, goalRect);
		DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	
        }
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
        return (new XiangqiMovespec(st, player));
    }
    

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
    	if(b.getState()==XiangqiState.CHECK_STATE)
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
    	if (hp.hitCode instanceof XId) // not dragging anything yet, so maybe start
        {

        XId hitObject = (XId)hp.hitCode;
		XiangqiCell cell = hitCell(hp);
		XiangqiChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case Black_Chip_Pool:
	    	PerformAndTransmit("Pick B "+cell.row+" "+chip.chipNumber());
	    	break;
	    case Red_Chip_Pool:
	    	PerformAndTransmit("Pick R "+cell.row+" "+chip.chipNumber());
	    	break;
	    case BoardLocation:
	    	if(cell.chipIndex>=0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
	    		}
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
    public void StopDragging( HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof XId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
       missedOneClick = false;
    	XId hitObject = (XId)hp.hitCode;
        XiangqiState state = b.getState();
		XiangqiCell cell = hitCell(hp);
		XiangqiChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
       	
         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
	        case ILLEGAL_MOVE_STATE:
			case PLAY_STATE:
			case CHECK_STATE:
			case PUZZLE_STATE:
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
         case ReverseViewButton:
        	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
        	 generalRefresh();
        	 break;
         case ChangeChipsetButton:
         	chipsetOption.setState(traditional_chips = !traditional_chips);
         	break;
			
        case Red_Chip_Pool:
        case Black_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                	case PLAY_STATE:
            		PerformAndTransmit(RESET);
            		break;

               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop "+col+" "+cell.row+" "+mov);
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
    	return(""+b.gametype+" "+b.randomKey); 
   }
    public String sgfGameType() { return(Xiangqi_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link #gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long rk = G.LongToken(his);
    	// make the random key part of the standard initialization,
    	// even though games like checkers probably don't use it.
        b.doInit(token,rk);
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
    {	if(target==reverseOption)
    	{
    	b.setReverseY(reverseOption.getState());
    	generalRefresh();
    	return(true);
    	}
    	else if(target==chipsetOption)
    	{
    	traditional_chips = chipsetOption.getState();
    	repaint(20);
    	return(true);
    	}
    	else if (target==drawAction)
    	{
    		if(OurMove()) 
    			{ 
        		if(b.canOfferDraw())
    			{
    			PerformAndTransmit(OFFERDRAW);
    			}
        		else { G.infoBox(null,s.get(DrawNotAllowed)); }
        		}
    		else {
                theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                    s.get(CantDraw));
            }
    		return(true);
    	}
        return(super.handleDeferredEvent(target,command));
     }

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new XiangqiPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/23/2023
		2178 files visited 0 problems
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

