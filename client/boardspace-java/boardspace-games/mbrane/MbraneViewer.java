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
package mbrane;


import static mbrane.Mbranemovespec.*;

import java.awt.*;

import online.common.*;
import common.GameInfo;
import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.FontManager;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.TextButton;
import lib.Tokenizer;
import lib.Image;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;


/**
 * 
 * TODO: needs rotation options like crosswords
 *  
*/
public class MbraneViewer extends CCanvas<MbraneCell,MbraneBoard> implements MbraneConstants
{		
    static final String Mbrane_SGF = "MBrane"; // sgf game number allocated for Mbrane

    // file names for jpeg images and masks
    static final String ImageDir = "/mbrane/images/";
    

     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(255,230,230);
    private Color rackBackGroundColor = new Color(225,192,182);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private MbraneColor altChipset = null;
    private boolean drawingBoard = false;
    public int getAltChipset() 
    {	// the alternate chipset flips red and black letters, encoded as chipset&1
    	// also encodes the rotation of the letters, encoded as (chipset>>1)&3
    	// 
    	// when the board is drawn in landscape format, the numbers need to be rotated
    	// drawing the reserve rack, we dynamically change the color of the letters 
    	// depending on whose turn it is.
    	//
    	int chipset = ( (altChipset!=null) ? altChipset.ordinal() : 0);
    	int chipTextRotation = twistBoard&drawingBoard&!seatingFaceToFace() ? 1 : 0;
    	return( chipset	+ (chipTextRotation<<1));
    }
     
    // private state
    private MbraneBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private double CELLSCALE = 0.8;
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //public Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //public Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle chatRect = addRect("chatRect");
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
   private Rectangle activeBoardRect = new Rectangle();
   private boolean twistBoard = false;
    //
    // addZoneRect also sets the rectangle as specifically known to the 
    // mouse tracker.  The zones are considered in the order that they are
    // added, so the smaller ones should be first, then any catchall.
    //
    // zones ought to be mostly irrelevant if there is only one board layout.
    //
     private Rectangle chipRects[] = addZoneRect("chip",2);
	private TextButton swapButton = addButton(SWAP,GameId.HitSwapButton,SwapDescription,
			HighlightColor, rackBackGroundColor);
	private Rectangle reserveRect = addRect(".reserve");
	private Rectangle scoreRect = addRect("scorerect");
    private Rectangle okendgame = addRect("okendgame");
    private Rectangle noendgame = addRect("noendgame");

/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	MbraneChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = MbraneChip.Icon.image;
    }


	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	int players_in_game = 2;
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        enableAutoDone = true;
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default

        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	MbraneConstants.putStrings();
        }
        
        String type = info.getString(GameInfo.GAMETYPE, MbraneVariation.Mbrane.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new MbraneBoard(type,players_in_game,randomKey,getStartingColorMap(),MbraneBoard.REVISION);
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
        bb.doInit(bb.gametype);						// initialize the board
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


    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	FontMetrics fm = FontManager.getFontMetrics(standardBoldFont());
    	int minLogW = fh*12;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        int scoreW = fh*10;
        String startres = s.get(StartResolution);
        String okres = s.get(OkResolution);
        String nores = s.get(NoResolution);
        int resSize = fh*2+Math.max(fm.stringWidth(startres),Math.max(fm.stringWidth(okres),fm.stringWidth(nores)));
        int nrows = 13;  // b.boardRows
        int ncols = 10;	 // b.boardColumns
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			(double)ncols/nrows,	// aspect ratio for the board
    			fh*3,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEdit(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	layout.placeRectangle(scoreRect, scoreW, scoreW, scoreW*3/2,scoreW*3/2,BoxAlignment.Edge,true);
       	layout.placeRectangle(okendgame, resSize,fh*5,BoxAlignment.Center);
       	G.copy(swapButton,okendgame);
       	G.splitBottom(okendgame,noendgame,fh*5/2);
       	
    	Rectangle main = layout.getMainRectangle();
        int stateH = fh*5/2;
        int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	twistBoard = mainW>mainH;
    	// calculate a suitable cell size for the board
    	double cs = twistBoard 
    					? Math.min((double)mainH/ncols,(double)mainW/nrows)
    					: Math.min((double)mainW/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)((twistBoard?nrows:ncols)*cs);
    	int boardH = (int)((twistBoard?ncols:nrows)*cs);
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
        placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	G.copy(activeBoardRect,boardRect);
    	if(twistBoard) { G.setRotation(activeBoardRect,Math.PI/2,boardX+boardW/2,boardY+boardH/2); }
    	G.SetRect(reserveRect,(int)(G.Left(activeBoardRect)+0.1*G.Width(activeBoardRect)),
    			(int)(G.Top(activeBoardRect)+0.7*G.Height(activeBoardRect)),
    			(int)(G.Width(activeBoardRect)*0.8),(int)(G.Height(activeBoardRect)*0.20));
     	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW,stateH,goalRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        labelFont = FontManager.getFont(largeBoldFont(),CELLSIZE/3);
 	
    }
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
       	Rectangle done = doneRects[player];
    	int chipW = unitsize*2;
    	int chipH = unitsize*2;
    	int doneW = plannedSeating()?unitsize*3 : 0;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW+unitsize/2,y,unitsize);
    	G.SetRect(chip, x, y, chipW, chipH);
    	G.SetRect(done, x,y+chipH+unitsize/2,doneW,doneW/2);
    	
    	pl.displayRotation = rotation;
    	
    	G.union(box, chip,done);
    	return(box);
    }
 
	// draw a box of spare chips. For Mbrane it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, HitPoint hit,int player,MbraneBoard gb)
    {
    	GC.frameRect(gc, Color.black, r);
        MbraneColor color = gb.getPlayerColor(player);
        MbraneId id = color.id;
        GC.setFont(gc,labelFont);
        boolean canHit = (gb.getState()==MbraneState.Puzzle) && (gb.reserveColor!=color);
        gb.getPlayerChip(player).draw(gc,this,r,canHit ? hit : null,id,(String)null);
    }
    
    private void drawScore(Graphics gc,Rectangle r)
    {	// permute the location where the region totals are displayed
    	// corresponding to their appearance in a rotated board.
    	int twistMap[] = { 2,5,8,  1,4,7, 0,3,6, };
    	
     MbraneChip.smallBoard.image.centerImage(gc, r);
    	int w = G.Width(r);
    	int step = (int)(w*0.31);
    	int left = (int)(G.Left(r)+w*0.04);
    	int top = (int)(G.Top(r)+w*0.01);
   		int small = (int)(w*0.02);
   		boolean simple = bb.variation == MbraneVariation.MbraneSimple;
   		for(int lim=bb.zoneCenters.length-1; lim>=0; lim--)
    	{	int mapidx = twistBoard ? twistMap[lim] : lim;
    		double v = bb.scoreZone(lim);	// don't change the tiles
    		int coords[] = bb.zoneCenters[mapidx];
    		int coln = coords[0]-'A';
    		int rown = coords[1]-1;
    		Color co = v>0 ? MbraneColor.Red.textColor : MbraneColor.Black.textColor;
    		GC.setFont(gc, labelFont);
    		int xpos = left+((coln+1)/3)*step;
    		int ypos = top+(2-rown/3)*step;
    		if(bb.resolved[lim]) 
    		{	StockArt.FilledCheckbox.draw(gc, this, step/3,xpos+step/8,ypos+step/4,null);
    		}
    		GC.Text(gc, true, xpos,ypos+small,step,step,co,null,
    				(v==0) ? "" 
    				: simple ? ""+(int)(Math.abs(v)) : ""+Math.abs(v));
    	}

    }
    
    // this is drawn in the possibly rotated frame of reference of the board
    private void drawReserve(Graphics gc,MbraneBoard gb,Rectangle r,HitPoint p,int validMask)
    {	MbraneCell reserve[] = gb.reserve;
    	altChipset = gb.reserveColor();
    	double step = (double)G.Width(r)/reserve.length;
    	int cx = (int)(G.Left(r)+step/2);
    	int cy = (int)(G.Bottom(r)-step/4);
    	boolean usuallyLegal =  gb.legalToHitChips();
    	int moving = gb.pickedObject==null ? 0 : (1<<gb.pickedObject.visibleNumber());
	   	GC.setFont(gc, labelFont);
    	for(int i=0,maskBit=0x1;i<reserve.length;i++,maskBit=maskBit<<1) 
    		{ int xpos = cx+(int)(step*i);
    		  MbraneCell c = reserve[i];
    		  boolean canhit = (maskBit&validMask)==0;
    		  boolean candrop = usuallyLegal && ((moving!=0) ? ((maskBit&moving)!=0) : canhit);
    		  if(c.drawStack(gc,this,candrop?p:null,(int)step,xpos,cy,0,0.2,null))
    			{	p.awidth = (int)step;
    				p.spriteColor = Color.red;
    			}
              
    		  if( !canhit )
    		  {
    			  StockArt.SmallX.draw(gc, this, (int)step,xpos,cy,null);
    		  }
    		}
    	altChipset =null;
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
        GC.setFont(g,labelFont);
        MbraneChip.getChip(obj).draw(g,this,(int)(bb.cellSize()*CELLSCALE), xp, yp, MbraneChip.SPRITE);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  

    Image scaled = null;
    
    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { // erase
      MbraneBoard gb = disB(gc);
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     MbraneChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       MbraneChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(gb,activeBoardRect);
      if(twistBoard)
      {
    	  GC.setRotation(gc,-Math.PI/2,G.centerX(boardRect),G.centerY(boardRect));
      }
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      scaled = MbraneChip.board.image.centerScaledImage(gc, activeBoardRect,scaled);

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      gb.DrawGrid(gc, activeBoardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);
      if(twistBoard)
      {
    	  GC.setRotation(gc,Math.PI/2,G.centerX(boardRect),G.centerY(boardRect));
      }

    }    
 
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, MbraneBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  

        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        if(twistBoard)
        { drawingBoard = true;
          GC.setRotatedContext(gc,brect,highlight,-Math.PI/2);
        }
        numberMenu.clearSequenceNumbers();
        MbraneCell closestCell = gb.closestCell(highlight,brect);
        gb.markValidSudoku();
        int validMask = gb.invalidPlacementMask();
        boolean hitCell = gb.LegalToHitBoard(closestCell);
        MbraneChip picked = gb.pickedObject;
        int pickedBit = (picked==null) ? 0 : 1<<picked.visibleNumber();
        if(hitCell )
        { // note what we hit, row, col, and cell
          boolean empty = closestCell.isEmpty();
          highlight.hitCode = (empty||(picked!=null)) ? MbraneId.EmptyBoard : MbraneId.BoardLocation;
          highlight.hitObject = closestCell;
          highlight.arrow = (empty||(picked!=null)) ? StockArt.DownArrow : StockArt.UpArrow;
          highlight.awidth = CELLSIZE;
        }
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        if (gc != null)
        {
        int size = (int)(gb.cellSize()*0.75);
        GC.setFont(gc,labelFont);
        for(MbraneCell cell = gb.allCells; cell!=null; cell=cell.next)
          {
            boolean drawhighlight = (hitCell && (cell==closestCell)) 
   				|| gb.isDest(cell) 		// is legal for a "drop" operation
   				|| gb.isSource(cell);	// is legal for a "pick" operation+
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
           numberMenu.saveSequenceNumber(cell,xpos,ypos); 
           // StockArt.SmallO.drawChip(gc,this,(int)(gb.CELLSIZE/2),xpos,ypos,null);  
            if (drawhighlight)
             { // checking for pointable position
            	 StockArt.SmallO.draw(gc,this,gb.cellSize(),xpos,ypos,null);                
             }
            int cellMask = cell.invalidPlacementMask(); 
            if(cellMask == 0x1ff) 
            {	// no placement is possible
            	StockArt.SmallX.draw(gc, this, size/2,xpos,ypos,null);
            }
            if(pickedBit!=0)
            {
            if((pickedBit & cellMask)==0)
            {
            	StockArt.SmallO.draw(gc,this,size/2,xpos,ypos,null);
            }}
            cell.drawStack(gc,this,highlight,size,xpos,ypos,xpos, 0,0, null);
            }

        }
        numberMenu.drawSequenceNumbers(gc,CELLSIZE,largePlainFont(),labelColor); 
        drawReserve(gc,gb,reserveRect,highlight,validMask);
        
        if(twistBoard)
        { drawingBoard = false;
        	GC.unsetRotatedContext(gc,highlight);
        }
    }
    // override for the standard numberMenu drawNumber
    public void drawNumber(Graphics gc,PlacementProvider source,PlacementProvider dest,int cellSize,int x,int y,Font font,Color color, String str)
    {	
     	  GC.setFont(gc,font);
     	  if(twistBoard)
     	  {
     		  GC.setRotation(gc,Math.PI/2,x+cellSize/2,y+cellSize/2);
     		  super.drawNumber(gc,source,dest,cellSize,x-cellSize/5,y+cellSize/5,font,color, str);
     		  GC.setRotation(gc,-Math.PI/2,x+cellSize/2,y+cellSize/2);
     	  }
     	  else
     	  {
     	  super.drawNumber(gc,source,dest,cellSize,x-cellSize/5,y+cellSize/5,font,color, str);
     	  }

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
    {  MbraneBoard gb = disB(gc);
       MbraneState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
   	   if(gc!=null)
   		{
   		// note this gets called in the game loop as well as in the display loop
   		// and is pretty expensive, so we shouldn't do it in the mouse-only case
      
       setDisplayParameters(gb,activeBoardRect);
   		}
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
       drawBoardElements(gc, gb, activeBoardRect, ourTurnSelect);
       drawScore(gc,scoreRect);
       boolean planned = plannedSeating();
       
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
       	{  commonPlayer pl = getPlayerOrTemp(i);
       	   pl.setRotatedContext(gc, selectPos,false);
    	   DrawChipPool(gc, chipRects[i],buttonSelect,i, gb);
    	   if(planned && gb.whoseTurn==i)
    	   {
    		   handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
       	   pl.setRotatedContext(gc, selectPos,true);
       	}
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       
       GC.setFont(gc,standardBoldFont());
       
       switch(state)
       {
       case ConfirmSwap:
       case PlayOrSwap:
    	   swapButton.highlightWhenIsOn = true;
       	   swapButton.setIsOn(state==MbraneState.ConfirmSwap);
    	   swapButton.show(gc,messageRotation, buttonSelect);
    	   break;
       case ConfirmPlay:
       case ConfirmScore:
       case ProposeResolution:
    	   if(GC.handleSquareButton(gc, messageRotation, noendgame, buttonSelect,
    			   s.get(OkResolution),HighlightColor,rackBackGroundColor))
    	   		{
    		   	buttonSelect.hitCode = MbraneId.OkResolution;
    	   		}
    	   if(GC.handleSquareButton(gc, messageRotation, okendgame, buttonSelect,
    			   s.get(NoResolution),HighlightColor,rackBackGroundColor))
    	   		{
    		   	buttonSelect.hitCode = MbraneId.NoResolution;
    	   		}
    	   break;
       default: break;
       }
       // second set of buttons
       switch(state)
       {
       case Gameover:
       case Puzzle: break;
       default:
       		{
       		// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);
			
			if(gb.emptyReserve()>=3)
			{
			switch(state)
			{
			case ProposeResolution:
			case Score:
			case Confirm:
			case ConfirmPlay:
			case ConfirmScore:
			case PlayNoResolve:
			case ConfirmSwap:
				break;
			default:
	    	   if(GC.handleSquareButton(gc, messageRotation, okendgame, buttonSelect,
	    			   s.get(StartResolution),HighlightColor,rackBackGroundColor))
	    	   		{
	    		   	buttonSelect.hitCode = MbraneId.StartResolution;
	    	   		}	
			}
       		}
       }}

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==MbraneState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
        standardGameMessage(gc,messageRotation,
            		state==MbraneState.Gameover?gameOverMessage(gb):s.get(state.description()),
            				state!=MbraneState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        gb.getPlayerChip(gb.whoseTurn).drawChip(gc,this, iconRect,null);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(MbraneVictoryCondition),progressRect, goalRect);
            //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for Mbrane
        
        // draw the vcr controls, last so the pop-up version will be above everything else
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
    	 numberMenu.recordSequenceNumber(bb.moveNumber);
        /**
         * animations are handled by a simple protocol between the board and viewer.
         * when stones are moved around on the board, it pushes the source and destination
         * cells onto the animationStck.  startBoardAnimations converts those points into
         * animation sprites.  drawBoardElements arranges for the destination stones, which
         * are already in place, to disappear until the animation finishes.  The actual drawing
         * is done by drawSprites at the end of redrawBoard
         */
        startBoardAnimations(replay,bb.animationStack,(int)(bb.cellSize()*CELLSCALE),MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay.animate) { playSounds(mm); }
       return (true);
    }
     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for Mbrane, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
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
//     		MbraneCell dest = bb.animationStack.pop();
//     		MbraneCell src = bb.animationStack.pop();
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
        return (new Mbranemovespec(st, pl));
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
      public commonMove EditHistory(commonMove nmove)
      {	  // some damaged games ended up with naked "drop", this lets them pass 
    	  boolean oknone = (nmove.op==MOVE_DROP);
    	  commonMove rval = EditHistory(nmove,oknone);
     	     
    	  return(rval);
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
        if (hp.hitCode instanceof MbraneId)// not dragging anything yet, so maybe start
        {
        MbraneId hitObject =  (MbraneId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    
 	    case Reserve_Pool:
 	    	{
 	    	MbraneCell hitCell = hitCell(hp);
 	    	PerformAndTransmit("Pick " + hitCell.row);
 	    	}
	    	break;
	    case BoardLocation:
	    	{
	        MbraneCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    	}
	    	break;
        } 
        }
    }
	private void doDropChip(char col,int row)
	{	MbraneState state = bb.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case Puzzle:
		{
		MbraneChip mo = bb.pickedObject;
		if(mo==null) { mo=bb.lastPicked; }
		if(mo==null) { mo=bb.getPlayerChip(bb.whoseTurn); }
		PerformAndTransmit("dropb "+col+" "+row);
		}
		break;
		case Confirm:
		case Play:
		case PlayNoResolve:
		case PlayOrSwap:
			PerformAndTransmit("dropb "+col+" "+row);
			break;
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
       	if(!(id instanceof MbraneId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        MbraneId hitCode = (MbraneId)id;
        MbraneCell hitObject = hitCell(hp);
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitCode);
        case StartResolution:
        case OkResolution:
        case NoResolution:
        	PerformAndTransmit(hitCode.name());
        	break;
        case PlaceRed:
        	PerformAndTransmit("PlaceRed");
        	break;
        case PlaceBlack:
        	PerformAndTransmit("PlaceBlack");
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
        case EmptyBoard:
			doDropChip(hitObject.col,hitObject.row);
			break;
			
        case Reserve_Pool:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+hitObject.row);
			}
           break;
 
        }
        }
    }



    private boolean setDisplayParameters(MbraneBoard gb,Rectangle r)
    {
      	boolean complete = false;
      	gb.SetDisplayParameters(
      			new double[]{0.16,0.362},
      			new double[]{0.918,0.362},
      			new double[]{0.16,0.94},
      			new double[]{0.918,0.94});
      	gb.SetDisplayRectangle(r);
      	if(complete) { generalRefresh(); }
      	return(complete);
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
    public String sgfGameType() { return(Mbrane_SGF); }	// this is the official SGF number assigned to the game


   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int np = his.intToken();	// players always 2
    	long rv = his.intToken();
    	int rev = his.intToken();	// rev does't get used either
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // long rk = his.longToken();
    	// bb.doInit(token,rk);
        bb.doInit(token,rv,np,rev);
        adjustPlayers(np);

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

    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new MbranePlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 12/2/2025
     *  50 files visited 0 problems
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
    public int getLastPlacement() { return bb.placementIndex; }
}

