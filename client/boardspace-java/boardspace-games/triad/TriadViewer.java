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
package triad;

import java.awt.*;

import java.util.Hashtable;
import common.GameInfo;
import online.common.*;
import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Toggle;
import lib.Tokenizer;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;


public class TriadViewer extends CCanvas<TriadCell,TriadBoard> implements TriadConstants
{	static final long serialVersionUID = 1000;
     // colors
    private Color reviewModeBackground = new Color(0.7f,0.7f,0.7f);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color rackBackGroundColor = new Color(0.7f,0.7f,0.7f);
    private Color boardBackgroundColor = new Color(220,165,155);
    

    // images, shared among all instances of the class so loaded only once
    private static StockArt[] tileImages = null; // tile images
    private static StockArt[] borders = null;// border tweaks for the images
    private static Image[] textures = null;// background textures
    
    // private state
    private TriadBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 			//size of the layout cell
    private double xscale = 0.6;	//this is a fudge to adjust the size of tiles
    		// and chips to the current size of the board.
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRect[] = addRect("chip",3);
    
    private Rectangle repRect = addRect("repRect");
    private Toggle eyeRect = new Toggle(this,"eye",
			StockArt.NoEye,TriadId.ToggleEye,NoeyeExplanation,
			StockArt.Eye,TriadId.ToggleEye,EyeExplanation
			);


    public synchronized void preloadImages()
    {	TriadChip.preloadImages(loader,ImageDir);
    	if (borders == null)
    	{ 	// note that for this to work correctly, the images and masks must be the same size.  
        	// Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    		
    		// images and textures are static variables, so they're shared by
    		// the entire class and only get loaded once.  Special synchronization
    		// tricks are used to make sure.
    	  tileImages = StockArt.preLoadArt(loader,ImageDir,TileFileNames,TILESCALES);
          textures = loader.load_images(ImageDir,TextureNames);
          borders = StockArt.preLoadArt(loader,StonesDir,BorderFileNames,BORDERSCALES);
    	}
    	gameIcon = textures[ICON_INDEX];
    }
    Color TriadMouseColors[] = { Color.red,Color.green,Color.blue};
    Color TriadMouseDotColors[] = { Color.white,Color.white,Color.white};
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	enableAutoDone = true;
        super.init(info,frame);
        MouseColors = TriadMouseColors;
        MouseDotColors = TriadMouseDotColors; 
        
        // use_grid=reviewer;// use this to turn the grid letters off by default      
        bb = new TriadBoard(info.getString(GameInfo.GAMETYPE, Triad_INIT),
        		getStartingColorMap());
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
    	{startFirstPlayer();
    	}
    } 
    
    public void setLocalBounds(int x,int y,int width,int height)
    {
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();		// always 3 for triad
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*30;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;

        int nrows = 15;  
        int ncols = 15;

        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// % of space allocated to the board
    			1.0,	// 1.0:1 aspect ratio for the board
    			fh*2.5,	// maximum cell size based on font size
    			0.5		// preference for the designated layout, if any
    			);
    	
    	// place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	// however, if that doesn't work out the main rectangle will shrink.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	int buttonW = fh*8;
    	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
        Rectangle main = layout.getMainRectangle();
         
        int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	int C2 = CELLSIZE/2;
    	// center the board in the remaining space
        int stateH = fh*5/2;
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)((nrows-1)*CELLSIZE);
    	int extraW = (mainW-boardW)/2;
    	int extraH = (mainH-stateH-boardH)/2;
    	int boardX = mainX+extraW;
    	int boardY = mainY+stateH+extraH;
    	int boardBottom = boardY+boardH;
    	int stateY = boardY-stateH;
       	layout.returnFromMain(extraW,extraH);
    	placeStateRow(boardX,stateY,boardW,stateH,iconRect,stateRect,annotationMenu,numberMenu,eyeRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	placeRow( boardX, boardBottom-stateH, boardW, stateH,goalRect);
    	setProgressRect(progressRect,goalRect);
    	
        int vcrW = (int)(C2*7.5);
        int vcrH = vcrW/2;
        SetupVcrRects(boardX,boardBottom-vcrH-stateH,vcrW,vcrH);
        

         positionTheChat(chatRect,Color.white,Color.white);
  
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int cell)
    {
        int chipW = cell*2;
        commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,cell);
    	Rectangle chip = chipRect[player];
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW, chipW);
    	G.union(box, chip);
    	G.SetRect(done, G.Right(box)+cell/2, y+cell/2, cell*4, cell*2);
    	G.union(box, done);
    	pl.displayRotation = rotation;
    	return(box);
    }

	// draw a box of spare chips. It's purely for visual effect.
    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,TriadBoard gb)
    {
        boolean canhit = gb.LegalToHitChips(player) && G.pointInRect(highlight, r);
        if (canhit)
        {
            highlight.hitCode = gb.getPlayerColor(player);
            highlight.arrow = (gb.pickedObject!=null)?StockArt.DownArrow:StockArt.UpArrow;
            highlight.awidth = CELLSIZE;
            highlight.hit_x = G.centerX(r);
            highlight.hit_y = G.centerY(r);
            highlight.spriteColor = Color.red;
        }

        if (gc != null)
        { 	TriadCell c = gb.getPlayerCell(player);
        	int siz = G.Width(r)*2/3;
        	int cx = G.centerX(r);
        	int cy = G.centerY(r);
        	c.drawChip(gc,this,siz,cx,cy,null);
        	if(player==gb.candidate_player)
        	{	tileImages[CANDIDATE_INDEX].drawChip(gc,this,siz,cx,cy,null);
        	}
           	if(player==gb.bunny_player)
        	{	tileImages[BUNNY_INDEX].drawChip(gc,this,siz,cx,cy,null);
        	}

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
    {
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
        int cellSize = (int)(bb.cellSize()*xscale);
    	TriadChip.getChip(obj).drawChip(g,this,cellSize, xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  


    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean review = reviewMode() && !mutable_game_record;
      TriadBoard gb = disB(gc);
      // erase
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
      // the numbers for the square-on display are slightly ad-hoc, but they look right
      gb.SetDisplayParameters( 1.0,1.0, 0.3,0,30.0, 0, 0,0); // shrink a little and rotate 30 degrees
      gb.SetDisplayRectangle(boardRect);
      
      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      gb.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

      // draw the tile grid.  The positions are determined by the underlying board
      // object, and the tile itself if carefully crafted to tile the board
      // when drawn this way.  For games with simple graphics, we could use the
      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
      // but for more complex graphics with overlapping shadows or stacked
      // objects, this double loop is useful if you need to control the
      // order the objects are drawn in.
      int cell = (int)(gb.cellSize()*xscale);
      int top = G.Bottom(boardRect);
      int left = G.Left(boardRect);
      for(TriadCell c = gb.allCells; c!=null ; c=c.next)
      { //where we draw the grid
    	  int ypos = top - gb.cellToY(c);
    	  int xpos = left + gb.cellToX(c);
    	  int bindex = HEXTILE_BORDER_NR_INDEX;
    	  int hidx = c.color.ordinal()+HEXTILE_NR_INDEX; 
    	  // to fine tune the board rendering.
    	  tileImages[hidx].drawChip(gc,this,cell,xpos,ypos,null);
              
              // decorate the borders with darker and lighter colors.  The border status
              // of cells is precomputed, so each cell has a mask of which borders it needs.
              // in order to make the artwork as simple as possible to maintain, the border
              // pictures are derived directly from the cell masters, so they need the
              // same scale and offset factors as the main cell.
    	  	  int border = c.borderMask();
              if(border!=0)
              {
              for(int dir=0; dir<6;dir++)
              {	  //turn on rectangles and sliders to adjust
            	  lastDropped = borders[bindex+5];
            	  adjustScales(borders[bindex+5].getScale(),lastDropped);
            	  if((border&(1<<dir))!=0)
            	  {	borders[bindex+dir].drawChip(gc,this,cell,xpos,ypos,null);
            	  }
              }
              }
       }
 
    }

   /* draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */
    private void drawBoardElements(Graphics gc, TriadBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  Either gc or highlight might be
    	// null, but not both.
        //
        int cellSize = (int)(gb.cellSize()*xscale);
        Hashtable<TriadCell,TriadCell> dests = gb.getDests();
        Hashtable<TriadCell,TriadCell> sources = gb.getSources();
        Hashtable<TriadCell,TriadCell> captures = gb.getCaptures();
        boolean show = eyeRect.isOnNow();
        numberMenu.clearSequenceNumbers();
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        for(TriadCell cell = gb.allCells; cell!=null; cell=cell.next)
          {
         	 int ypos = G.Bottom(brect) - gb.cellToY(cell);
             int xpos = G.Left(brect) + gb.cellToX(cell);
             boolean canHit = gb.LegalToHitBoard(cell);
             numberMenu.saveSequenceNumber(cell,xpos,ypos);
               if(cell.drawChip(gc, this,canHit?highlight:null, cellSize,xpos,ypos,null))
             {
                 boolean empty = (cell.chip == null);
                 boolean picked = (gb.pickedObject!=null);
                 highlight.hitCode = (empty||picked) ? TriadId.EmptyBoard : TriadId.BoardLocation;
                 highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
                 highlight.awidth = CELLSIZE;              
            	 highlight.spriteColor = Color.red;
             }
           if(show && sources.get(cell)!=null)
           {StockArt.SmallO.drawChip(gc,this,cellSize,xpos,ypos,null);
           }
           if(show && dests.get(cell)!=null)
             {	StockArt.SmallO.drawChip(gc,this,cellSize,xpos,ypos,null);
             }
           if(show && captures.get(cell)!=null)
             	{StockArt.SmallX.drawChip(gc,this,cellSize,xpos,ypos,null);
             	}

            }
        numberMenu.drawSequenceNumbers(gc,CELLSIZE,labelFont,labelColor);
    }

    /*
     * draw the main window and things on it.  
     * If gc!=null then actually draw, 
     * If selectPos is not null, then as you draw (or pretend to draw) notice if
     * you are drawing under the current position of the mouse, and if so if you could
     * click there to do something.  Care must be taken to consider if a click really
     * ought to be allowed, considering spectator status, use of the scroll controls,
     * if some board token is already actively moving, and if the game is active or over.
     * 
     * This dual purpose (draw, and notice mouse sensitive areas) tends to make the
     * code a little complicated, but it is the most reliable way to make sure the
     * mouse logic is in sync with the drawing logic.
     * 
    General GUI checklist

    vcr scroll section always tracks, scroll bar drags
    lift rect always works
    zoom rect always works
    drag board always works
    pieces can be picked or dragged
    moving pieces always track
    stray buttons are insensitive when dragging a piece
    stray buttons and pick/drop are inactive when not on turn
*/
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  TriadBoard gb = disB(gc);
       TriadState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
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
       drawBoardElements(gc, gb, boardRect, ourTurnSelect);

       boolean planned = plannedSeating();
       
       for(int i=0;i<=2;i++)
       {	commonPlayer pl = getPlayerOrTemp(i);
       		pl.setRotatedContext(gc, selectPos, false);
       		DrawChipPool(gc, chipRect[i], i, ourTurnSelect,gb);
		   if(planned && gb.whoseTurn==i)
		   	{
     		   handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
    					HighlightColor, rackBackGroundColor);
		   	}
       		pl.setRotatedContext(gc,selectPos,true);
       }
       
       GC.setFont(gc,standardBoldFont());
       int whoseTurn = gb.whoseTurn;
       commonPlayer pl = getPlayerOrTemp(whoseTurn);
       double messageRotation = pl.messageRotation();

		if (state != TriadState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);
        }

        drawPlayerStuff(gc,(state==TriadState.PUZZLE_STATE),nonDragSelect,HighlightColor,rackBackGroundColor);
  
 
        if (gc != null)
        {	// draw the avatars
   
             standardGameMessage(gc,messageRotation,
            		state==TriadState.GAMEOVER_STATE?gameOverMessage(gb):s.get(state.getDescription()),
            				state!=TriadState.PUZZLE_STATE,
            				whoseTurn,
            				stateRect);
             TriadChip cc = gb.getPlayerChip(whoseTurn);
             cc.drawChip(gc,this,iconRect,null,0.7);
            goalAndProgressMessage(gc,nonDragSelect,s.get(GoalMessage),progressRect, goalRect);
            DrawRepRect(gc,0,Color.black, gb.Digest(),repRect);	
         }
        eyeRect.activateOnMouse = true;
        eyeRect.draw(gc,selectPos);
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
        numberMenu.recordSequenceNumber(bb.moveNumber());
        int cellSize = (int)(bb.cellSize()*xscale);
        startBoardAnimations(replay,bb.animationStack,cellSize,MovementStyle.SequentialFromStart);
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
        return (new TriadMovespec(st, player));
    }
   
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof TriadId) // not dragging anything yet, so maybe start
        {
       	TriadId hitObject =  (TriadId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    
        case Blue_Chip_Pool:
        case Red_Chip_Pool:
        case Green_Chip_Pool:
        	PerformAndTransmit(TriadMovespec.D.findUnique(MOVE_PICK)+" "+hitObject.shortName);
        	break;
	    case BoardLocation:
	        TriadCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    	break;
        }

        }
    }
	private void doDropChip(char col,int row)
	{	TriadState state = bb.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case PUZZLE_STATE:
		{
		TriadChip mo = bb.pickedObject;
		if(mo==null) { mo=bb.lastPicked; }
		if(mo==null) { mo=bb.getPlayerChip(bb.whoseTurn); }
		PerformAndTransmit("dropb "+mo.colorName+" "+col+" "+row);
		}
		break;
		case CONFIRM_STATE:
		case CONFIRM_END_STATE:
		case DRAW_STATE:
		case PLAY_STATE:
			TriadChip mo=bb.getPlayerChip(bb.whoseTurn);	
			PerformAndTransmit("dropb "+mo.colorName	+ " "+col+" "+row);
			break;
					                 
		
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
        if(!(id instanceof TriadId)) {   missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
   		TriadId hitCode = (TriadId)hp.hitCode;
        TriadCell hitObject = hitCell(hp);
        TriadState state = bb.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case ToggleEye:
        	eyeRect.toggle();
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case CONFIRM_END_STATE:
			case DRAW_STATE:
			case PLAY_STATE:
				if(!bb.isDest(hitObject))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					throw G.Error("shouldn't hit a chip in state %s",state);
					}
				// fall through and pick up the previously dropped piece
				//$FALL-THROUGH$
			case PUZZLE_STATE:
				PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
				break;
			}
			break;
			
        case EmptyBoard:
			switch(state)
			{
				default:
					throw G.Error("Not expecting EmptyBoard in state %s",state);
				case DROP_STATE:
					PerformAndTransmit("Dropb "+bb.getPlayerChip(bb.bunny_player).colorName+" "+hitObject.col+" "+hitObject.row);
					break;
				case CONFIRM_STATE:
				case PLAY_STATE:
				case PUZZLE_STATE:
					doDropChip(hitObject.col,hitObject.row);
					break;
			}
			break;
			
        case Red_Chip_Pool:
        case Blue_Chip_Pool:
        case Green_Chip_Pool:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+bb.pickedObject.colorName);
			}
           break;
        }
        }
    }

    // return what will be the init type for the game
     public String gameType() 
    	{
    	   // in games which have a randomized start, this method would return
    	   // return(bb.gametype+" "+bb.randomKey); 
    	return(bb.gametype); 
    	}	// this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Triad_SGF); }	// this is the official SGF number assigned to the game

    
    // interact with the board to initialize a game
    public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // int rk = G.IntToken(his);
    	// bb.doInit(token,rk);
        bb.doInit(token);
    }



/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically.
 * 
 * This is a good place to make notes about threads.  Threads in Java are
 * very dangerous and tend to lead to all kinds of undesirable and/or flakey
 * behavior.  The fundamental problem is that there are three or four sources
 * of events from different system-provided threads, and unless you are very
 * careful, these threads will all try to use and modify the same data
 * structures at the same time.   Java "synchronized" declarations are
 * hard to get right, resulting in synchronization locks, or lack of
 * synchronization where it is really needed.
 * 
 * This toolkit addresses this problem by adopting the "one thread" model,
 * and this is where it is.  Any other threads should do as little as possible,
 * mainly leave breadcrumbs that will be picked up by this thread.
 * 
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



    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new TriadPlay());
     
    }
    public boolean parsePlayerInfo(commonPlayer p,String first,Tokenizer tokens)
    {
    	if(OnlineConstants.TIME.equals(first) && bb.DoneState())
    	{
    		PerformAndTransmit("Done",false,replayMode.Replay);
    	}
    	return(super.parsePlayerInfo(p, first, tokens));
    }
    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/23/2023
     *  163 files visited 0 problems
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
    public int getLastPlacement()
    {
    	return bb.placementIndex;
    }
}

