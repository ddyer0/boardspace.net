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
package gyges;

import bridge.*;
import common.GameInfo;

import com.codename1.ui.geom.Rectangle;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.CommonDriver;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Graphics;
import lib.Image;
import lib.*;
import static java.lang.Math.*;

/**
 * 
 * Change History
 *
 * Dec 2012 initial work in progress. 
 *
*/
public class GygesViewer extends CCanvas<GygesCell,GygesBoard> implements GygesConstants
{
     /**
	 * 
	 */
	   // file names for jpeg images and masks
    static final String ImageDir = "/gyges/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"
    	};
    static final int BOARD_INDEX = 0;
    static final String ImageNames[] = { "board" };
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;
    private static StockArt[] stockArt = null;
    // private undoInfo
    private GygesBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("rack",2);
    
    private Rectangle repRect = addRect("repRect");
    private Rectangle reverseViewRect = addRect("reverse");
    private Toggle eyeRect = new Toggle(this,"eye",
			StockArt.NoEye,GygesId.ToggleEye,NoeyeExplanation,
			StockArt.Eye,GygesId.ToggleEye,EyeExplanation
			);
    
    static final double ArrowScales[][] = {{0.6,0.5,1.0},{0.6,0.5,1.0},{0.6,0.5,1.0},{0.6,0.5,1.0}};
    static final String ArrowNames[] = { "arrow-left","arrow-up","arrow-right","arrow-down" };
    static final int ArrowOffsets[] = new int[4];
    static {
    	ArrowOffsets[GygesBoard.CELL_LEFT()] = 0;
    	ArrowOffsets[GygesBoard.CELL_UP()] = 1;
    	ArrowOffsets[GygesBoard.CELL_RIGHT()] = 2;
    	ArrowOffsets[GygesBoard.CELL_DOWN()] = 3;
    }
    public synchronized void preloadImages()
    {	
       	GygesChip.preloadImages(loader,ImageDir);
        if (stockArt == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
        stockArt = StockArt.preLoadArt(loader,ImageDir,ArrowNames,true,ArrowScales);
    	}
        gameIcon = images[BOARD_INDEX];
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
    	enableAutoDone = true;
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new GygesBoard(info.getString(GameInfo.GAMETYPE, Gyges_INIT_beginner),randomKey);
        useDirectDrawing(true);
        doInit(false);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);			// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}

    }
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int doneW = plannedSeating()?unitsize*4:0;
    	Rectangle box = pl.createRectangularPictureGroup(x+doneW+unitsize/4,y,unitsize);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	G.SetRect(done, x, y, doneW,doneW/2);
    	G.SetRect(chip,x+unitsize/4,y+unitsize*3/2,unitsize,unitsize);
    	pl.displayRotation = rotation;
    	G.union(box, done);
    	return(box);
    }
    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*30;	
    	int minVcrW = fh*16;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// % of space allocated to the board
    			0.9,	// aspect ratio for the board
    			fh*2,
    			fh*2.5,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
        boolean rotated = seatingFaceToFaceRotated();
        int nrows = rotated ? b.boardColumns : b.boardRows;
        int ncols = rotated ? b.boardRows : b.boardColumns;
       // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeTheVcr(this,minVcrW,minVcrW*3/2);
    	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);

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
        int stateH = (int)(fh*2.5);
        placeRow(stateX,stateY,boardW,stateH,stateRect,annotationMenu,eyeRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	if(rotated)
    	{
    		G.setRotation(boardRect, -Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
        
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW,stateH,goalRect,reverseViewRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,rackBackGroundColor);

    }
    public void drawPlayerIcon(Graphics gc,int forPlayer,int cx,int cy,int size)
    {
    	int dir = ((forPlayer!=0 == b.reverseY()) ? 1 : -1) * size/3;
    	GC.setColor(gc,MouseColors[forPlayer]);
    	//Color edgeColor = MouseDotColors[forPlayer];
    	GC.drawArrow(gc, cx,cy-dir,cx,cy+dir,size/3,size/7);
    }
    double iconScale[] = new double[] {1,0.8,0,-0.2};
    public Drawable getPlayerIcon(int p)
    {	playerTextIconScale = iconScale;
    	return( new DrawnIcon(100,100,p)
    		{ public void drawChip(Graphics gc, exCanvas c, int size, int posx, int posy, String msg)
    			{
    			drawPlayerIcon(gc,(int)parameter,posx,posy,size);
    			}
    		});
    }
	// draw the rack of unplaced pieces.
    private void DrawCommonChipPool(Graphics gc, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	GygesCell chips[]= b.rack[forPlayer];
        boolean canHit = b.LegalToHitChips(forPlayer);
        int x = G.Left(r);
        int y = G.Top(r);
        int w = G.Width(r);
        int thisx = x+SQUARESIZE;
        int thisy = y+SQUARESIZE/2;
        drawPlayerIcon(gc,forPlayer,x+w/2,y+w/2,w*2/3);
        boolean picked = (b.pickedObject!=null);
        boolean show = eyeRect.isOnNow();
        for(GygesCell thisCell : chips)
        {
        GygesChip thisChip = thisCell.topChip();
        boolean canPick = !picked && (thisChip!=null);
        boolean canDrop = picked && (thisChip==null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        if(thisCell.drawStack(gc,this,pt,SQUARESIZE,thisx,thisy,0,0,null))
        	{	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        		highlight.awidth = G.Width(r)/4;
        		highlight.spriteColor = Color.red;
        	}
        if(canHit && canPick && show) 
        {
        	StockArt.SmallO.drawChip(gc,this,SQUARESIZE/2,thisx,thisy,null);
        }
        thisx += SQUARESIZE-SQUARESIZE/4;
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
    	GygesChip ch = GygesChip.getChip(obj);// Tiles have zero offset
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
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      GygesBoard gb = disB(gc);
      // erase
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      scaled = images[BOARD_INDEX].centerScaledImage(gc, brect,scaled);
	    gb.SetDisplayParameters(0.63,0.78,  -0.0,0.55,  0, 00,0.0,0);
	    gb.SetDisplayRectangle(brect);

      gb.DrawGrid(gc,brect,use_grid,Color.white,Color.white,Color.blue,Color.yellow);
    }

    private boolean handleCell(Graphics gc,GygesBoard gb,Rectangle brect,HitPoint highlight,
    		GygesCell cell,Hashtable<GygesCell,GygesCell> dests ,Hashtable<GygesCell,GygesCell> sources,GygesCell src)
    {
        HitPoint hit = gb.legalToHitBoard(cell,sources,dests) ? highlight : null;
        int ydistance =  gb.cellToY(cell);
        int ypos = G.Bottom(brect) - ydistance;
        int xpos = G.Left(brect) + gb.cellToX(cell);
        int adjustedSize = adjustedSquareSize(SQUARESIZE,ydistance,G.Height(brect));
        //StockArt.SmallO.drawChip(gc,this,adjustedSize,xpos,ypos,null);
        if(cell==src) { StockArt.SmallO.drawChip(gc,this,adjustedSize/2,xpos,ypos,null); }
        boolean hitCell = cell.drawStack(gc,this,hit,adjustedSize,xpos,ypos,0,0.1,null);
    	boolean show = eyeRect.isOnNow();

        if(show && sources.get(cell)!=null)
        {
        	StockArt.SmallO.drawChip(gc,this,SQUARESIZE/2,xpos,ypos,null);
    	}
        if(dests.get(cell)!=null)
        {
        	StockArt.SmallO.drawChip(gc,this,SQUARESIZE/2,xpos,ypos,null);
    	}
        return(hitCell);
    }
    void decoratePathStep(Graphics gc,GygesBoard gb,Rectangle brect,GygesCell from,GygesCell to,CellStack bounces,GygesChip top,GygesCell empty,int who)
	{	if(from!=null && top!=null)
		{
		CellStack steps = gb.getMovePath(from,to,top,bounces,empty,who);
		if(steps!=null)
		{	GygesCell terminal = (to!=null) || (bounces==null) ? to : bounces.top();
			GygesCell prev = from;
			int reverse = gb.reverseY() ? 2 : 0;
			for(int idx = 0; idx<steps.size(); idx++)
			{
				GygesCell current = steps.elementAt(idx);
				if(current!=prev)
				{
			        if(current.col=='G')
			        {
			        	decorateDropStep(gc,gb,brect,prev,current);
			        }
			        else
			        {
			        int ypos0 = G.Bottom(brect) - gb.cellToY(prev);
			        int xpos0 = G.Left(brect) + gb.cellToX(prev);
			        int ypos1 = G.Bottom(brect) - gb.cellToY(current);
			        int xpos1 = G.Left(brect) + gb.cellToX(current);
			        int direction = b.findDirection(prev.col,prev.row,current.col,current.row);
			        
			        stockArt[direction^reverse].drawChip(gc,this,SQUARESIZE/3,(xpos0+xpos1)/2,(ypos1+ypos0)/2,null);
			        }
				}
				prev  = current;
				if((prev!=empty) && (prev==terminal))
					{ break; }
			}
		}}

	}
    private void decorateDropStep(Graphics gc,GygesBoard gb,Rectangle brect,GygesCell from,GygesCell to)
    {
    	int x1 = G.Left(brect) + gb.cellToX(from);
		 int y1 = G.Bottom(brect) - gb.cellToY(from);
		 int x2 = G.Left(brect) + gb.cellToX(to);
		 int y2 = G.Bottom(brect) - gb.cellToY(to);
		 double dis = G.distance(x1,y1,x2,y2);
		 int steps = Math.max(2,(int)(dis/(SQUARESIZE/4)));
		 for(int idx=1; idx<=steps;idx++)
		 {	double frac = idx/(double)(steps+1);
			 int xp = G.interpolate(frac, x1,x2);
			 int yp = G.interpolate(frac, y1,y2);
			 StockArt.SmallO.drawChip(gc,this,SQUARESIZE/2,xp,yp,null);
		 }
    }
    private void decorateMovePath(Graphics gc,GygesBoard gb,Rectangle brect)
    {	
    	switch(gb.getState())
    	{
    	case Gameover:
    	case PlayTop:
    	case PlayBottom:
    		// decorate a completed move based on the move history.
    		{
    			int idx = History.viewStep>=0 ? History.viewStep-1 : History.size()-1;
    			boolean first = true;
    			while(idx>=0)
    			{
    				GygesMovespec m = (GygesMovespec)History.elementAt(idx);
    				idx--;
    				switch(m.op)
    				{
    				case MOVE_DONE:	if(!first) { idx = -1;  }
    					break;
    				case MOVE_BOARD_BOARD:
    	    			GygesCell from = gb.getCell(m.from_col,m.from_row);
    	    			GygesCell to = gb.getCell(m.to_col,m.to_row);
    	    			GygesChip top = to.topChip();
    	    			if(top==null) { top=from.topChip(); }
    	    			decoratePathStep(gc,gb,brect,from,to,null,top,to,m.player);
    	    			idx = -1;
    					break;
    				case MOVE_DROPB:
    				case MOVE_DROPB_R:
    					idx = -1;
    					break;
					default:
						break;
    				}
    				first = false;
    			}
    			
    		}
    		break;
    	case Puzzle:
    		break;
    	default:
    	case Confirm:
    	case Continue:
    	case DropTop:
    	case DropBottom:
    		{
    		// decorate based on a move in progress, using the state of the board.
    		int dd = gb.droppedDestStack.size();
    		int ss = gb.pickedSourceStack.size();
    		int sz = min(ss,dd);
   			boolean dropping = gb.dropping;
    		if(sz>0)
    		{
   			
    			{GygesCell from = gb.pickedSourceStack.elementAt(0);
	   			GygesCell to = (gb.pickedObject!=null) ? gb.pickedSourceStack.top() : gb.droppedDestStack.top();
	   			GygesChip top = gb.pickedObject;
	   			GygesCell empty = to;
	   			CellStack localPath = gb.pickedSourceStack;
	   			if(dropping)
	   			{
	   				localPath = new CellStack();
	   				localPath.copyFrom(gb.pickedSourceStack);
	   				localPath.pop();
	   				to = gb.dropSource;
	   				top = to.topChip(); 
	   				empty = null;
	   			}
		   		if(top==null) 
		   			{ top = to.topChip(); 
		   			}
	    		decoratePathStep(gc,gb,brect,from,(dropping || (gb.pickedObject==null))?to:null,localPath,top,empty,gb.whoseTurn);
    			}

     		if(gb.dropping && (gb.getState()==GygesState.Confirm)) 
 			{int dsz = gb.droppedDestStack.size();
 			 GygesCell from = gb.droppedDestStack.elementAt(dsz-2);
 			 GygesCell to = gb.droppedDestStack.elementAt(dsz-1);
 			 decorateDropStep(gc,gb,brect,from,to);
 			}}}
    	}
 		
       }
    
    int adjustedSquareSize(int startingSize,int ydistance,int height)
    {
    	return(startingSize-(int)(startingSize*ydistance*0.05/height));
    }
    
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, GygesBoard gb, Rectangle brect, HitPoint highlight)
    {	
    	for(int i=0;i<2;i++)
    		{ DrawCommonChipPool(gc, i,chipRects[i], gb.whoseTurn,highlight);
    		}
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	Hashtable<GygesCell,GygesCell> dests = gb.getDests();
     	Hashtable<GygesCell,GygesCell> sources = gb.getSources();
     	GygesCell src = gb.getSource();
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
     	GygesCell hitCell = null;
     	Enumeration<GygesCell> cells = gb.getIterator(Itype.LRTB);
     	while( cells.hasMoreElements())
     	{
            GygesCell cell = cells.nextElement();;
            if(handleCell(gc,gb,brect,highlight,cell,dests,sources,src)) { hitCell = cell; }
        	}
    	for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
    	{
    		if(handleCell(gc,gb,brect,highlight,gb.goalCell[i],dests,sources,src)) { hitCell = gb.goalCell[i]; }
    	}
    	if(hitCell!=null)
    	{
    		highlight.arrow =hasMovingObject(highlight) 
    	  			? StockArt.DownArrow 
    	  			: hitCell.height()>0?StockArt.UpArrow:null;
    	  	highlight.awidth = SQUARESIZE/4;
    	  	highlight.spriteColor = Color.red;
    	}
    	decorateMovePath(gc,gb,brect);
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  	DrawReverseMarker(gc,reverseViewRect,highlight,GygesId.ReverseViewButton);
    	eyeRect.activateOnMouse = true;
    	eyeRect.draw(gc,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  GygesBoard gb = disB(gc);
       boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      GygesState vstate = gb.getState();
      gameLog.playerIcons = true;
      gameLog.redrawGameLog2(gc, ourSelect, logRect, Color.black,boardBackgroundColor,
    		   standardBoldFont(),standardPlainFont());
    
      GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
        drawBoardElements(gc, gb, boardRect, ot);
      GC.unsetRotatedContext(gc,highlight);
        
        boolean planned = plannedSeating();
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
          {	commonPlayer pl = getPlayerOrTemp(i);

          	pl.setRotatedContext(gc, highlight,false);
          	if(planned && (i==gb.whoseTurn))
              {
              	handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
      					HighlightColor, rackBackGroundColor);	
              }
              pl.setRotatedContext(gc, highlight,true);
          }
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
 		double messageRotation = pl.messageRotation();

 		GC.setFont(gc,standardBoldFont());
		if (vstate != GygesState.Puzzle)
        {
			if(!planned && !autoDoneActive()) 
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
                }

 		drawPlayerStuff(gc,(vstate==GygesState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);

        standardGameMessage(gc,messageRotation,
            		vstate==GygesState.Gameover?gameOverMessage(gb):s.get(vstate.getDescription(gb.reverseY())),
            				vstate!=GygesState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        goalAndProgressMessage(gc,ourSelect,s.get(VictoryCondition),progressRect, goalRect);

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
        startBoardAnimations(replay);
        
        if(replay.animate) { playSounds(mm); }
 
        return (true);
    }
     
     void startBoardAnimations(replayMode replay)
     {	try {
     	double start = 0.0;
        if(replay.animate)
     	{	GygesCell endpoint = b.animationStack.top();
     		if(endpoint!=null)
     		{
     		GygesChip top = b.pickedObject;
     		if(top==null) { top = endpoint.topChip(); }
     		if(top!=null)
     		{
     		for(int i=0,lim=b.animationStack.size(); i<lim; i+=2)
     		{
     		GygesCell src = b.animationStack.elementAt(i);
     		GygesCell dest = b.animationStack.elementAt(i+1);
  			double speed = masterAnimationSpeed*((src.onBoard && (lim<=2)) ? 1.0 : 0.5);
      		startAnimation(src,dest,top,start,speed,((i+2)<lim) || (b.getState()==GygesState.Continue));
     		start += speed;
     		}}
     		if(b.dropping && (b.dropSource!=null)&&(b.pickedObject!=null))
     			{
     			startAnimation(b.dropSource,b.dropSource,b.pickedObject,0,start,false);
     			}
     		}
     	}}
     	finally { 
        	b.animationStack.clear();
     	}
     } 

     void startAnimation(GygesCell from,GygesCell to,GygesChip top,double start,double speed,boolean overlap)
     {	SimpleSprite newSprite = super.startAnimation(from,to,top,SQUARESIZE,start,speed);
     	if(newSprite!=null)
     	{	
     		newSprite.overlapped = overlap;
   		}
     }
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current undoInfo of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new GygesMovespec(st, player));
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
     * undoInfo performed by "nmove".  This is checked by verifyGameRecord().
     * 
     * in commonEditHistory()
     * 
     */

//    public commonMove EditHistory(commonMove nmove)
//    {
//    	GygesMovespec newmove = (GygesMovespec) nmove;
//    	GygesMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int undoInfo = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            GygesMovespec m = (GygesMovespec) History.elementAt(idx);
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
//                 		if(undoInfo==PUZZLE_STATE) { idx = -1; break; }
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
//                	{ GygesMovespec m2 = (GygesMovespec)History.elementAt(idx-1);
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
    case MOVE_RACK_BOARD:
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
    case MOVE_DROPB_R:
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
        if (hp.hitCode instanceof GygesId)// not dragging anything yet, so maybe start
        {

        GygesId hitObject = (GygesId)hp.hitCode;
		GygesCell cell = hitCell(hp);
		GygesChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case First_Player_Pool:
	    	PerformAndTransmit("Pick F "+cell.row);
	    	break;
	    case Second_Player_Pool:
	    	PerformAndTransmit("Pick S "+cell.row);
	    	break;
	    case First_Player_Goal:
	    	PerformAndTransmit("Pick FG "+cell.row);
	    	break;
	    case Second_Player_Goal:
	    	PerformAndTransmit("Pick SG "+cell.row);
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
        if(!(id instanceof GygesId)) {  missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
    	missedOneClick =false;
    	GygesId hitObject = (GygesId)hp.hitCode;
		GygesState state = b.getState();
		GygesCell cell = hitCell(hp);
		GygesChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case ToggleEye:
        	eyeRect.toggle();
        	break;
        case ReverseViewButton:
        	boolean v = !b.reverseY(); b.setReverseY(v);
        	break;
         case BoardLocation:	// we hit the board 

				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
				}
				break;

         case First_Player_Pool:
         case Second_Player_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in undoInfo %s",state);
            	case PlaceTop:
        		case PlaceBottom:
               	case Puzzle:
            		PerformAndTransmit("Drop "+col+" "+cell.row);
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
    public String sgfGameType() { return(Gyges_SGF); }

    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
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


/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically
 *  */
    
    //   public void ViewerRun(int wait)
    //   {
    //       super.ViewerRun(wait);
    //   }



    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new GygesPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/26/2023
     *  2717 files visited 0 problems
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



int max_legal_moves = 100;

//
// record this solution if it appears to be unique.
//
public void recordSolution(CommonDriver search_state,int complexity)
{		if(complexity>max_legal_moves)
	{	max_legal_moves = complexity;
		commonMove var = search_state.getCurrentVariation();
		//var.showPV("Solution "+number_of_solutions+" ");
		CommonMoveStack hist = new CommonMoveStack();
		
		{
			CommonMoveStack vhist = History;
			for(int i=0,lim=vhist.size();i<lim;i++)
			{	// copy the game before starting
				commonMove elem = vhist.elementAt(i);
				hist.push(elem.Copy(null));
			}
		}
		commonMove prev = null;
		while(var!=null)
		{	// add the moves in this solution
			hist.push(var);
			//UniverseMovespec svar = (UniverseMovespec)var;
			//if(svar.declined!=null) 
			//{ exclude += " "+svar.declined; }
			prev = var;
			var = var.best_move();
			if((var==null) || (var.player!=prev.player))
			{
				hist.push(new GygesMovespec(MOVE_DONE,prev.player)); 
			}
		}
		addGame(hist,"Solution #"+complexity);
	}
}

}
