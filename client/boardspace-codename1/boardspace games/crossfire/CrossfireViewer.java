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
package crossfire;

import bridge.*;
import com.codename1.ui.geom.Rectangle;


import online.common.*;
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
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;




/**
 * 
 * Crossfire viewer, Dec 2010
 * 
*/
public class CrossfireViewer extends CCanvas<CrossfireCell,CrossfireBoard> implements CrossfireConstants, GameLayoutClient
{	
	static final String Crossfire_SGF = "crossfire"; // sgf game name
	static final String ImageDir = "/crossfire/images/";
	static final int HEXTILE_PBOARD_INDEX = 0;
	static final int BOWL_BASE_INDEX = 1;
	static final int CELL_INDEX = 2;
	static final int HEXTILE_NP_INDEX = 3;
	static final double tileScales[][] = {{0.5,0.5,1.0}};
	static final String colorNames[] = {ColorNames.WhiteColor,ColorNames.BlackColor};
	static final double INITIAL_CHIP_SCALE = 0.125;
	static final double MAX_CHIP_SCALE = 0.2;
	static final double MIN_CHIP_SCALE = 0.005;
	static final int INITIAL_CHIPS_IN_PLAY = 18;
	//
	// basic image strategy is to use jpg format because it is compact.
	// .. but since jpg doesn't support transparency, we have to create
	// composite images wiht transparency from two matching images.
	// the masks are used to give images soft edges and shadows
	//
	static final String[] TileFileNames = 
	    {   "p-board","bowl-base","cell","board-np"
	    };
	
	
	static final int BACKGROUND_TILE_INDEX = 0;
	static final int BACKGROUND_REVIEW_INDEX = 1;
	static final int LIFT_ICON_INDEX = 2;
	static final String TextureNames[] = { "background-tile" ,"background-review-tile","lift-icon"};
	
     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridFillColor = new Color(10, 163, 190);
    private Color GridTextColor = Color.black;
    private Color GridColor = Color.black;
    private Color rackBackGroundColor = new Color(180,153,135);
    private Color boardBackgroundColor = new Color(187,165,241);
    
    public boolean usePerspective() { return(super.getAltChipset()==0); }

    // images, shared among all instances of the class so loaded only once
    private static StockArt[] tileImages = null; // tile images
    private static Image[] textures = null;// background textures
    
    // private state
    private CrossfireBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private static double CHIP_SIZE_SCALE = 2.5;	// 2.5 cells
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("chip",2);


    private Rectangle player1Reserve = addRect("player1ReserveRect");
    private Rectangle player2Reserve = addRect("player2ReserveRect");
    private Rectangle prisonerRects[] = addRect("Prisoners",2);
    
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    private Color ZoomHighlightColor = new Color(150,195,166);

    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;

     
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	CrossfireChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	if (tileImages == null)
    	{ 	// note that for this to work correctly, the images and masks must be the same size.  
        	// Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    		
    		// images and textures are static variables, so they're shared by
    		// the entire class and only get loaded once.  Special synchronization
    		// tricks are used to make sure this is true.

    	  // load the background textures as simple images
          textures = loader.load_images(ImageDir,TextureNames);
          tileImages = StockArt.preLoadArt(loader,ImageDir,TileFileNames,tileScales);

    	}
    	gameIcon = textures[LIFT_ICON_INDEX];
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
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default


        zoomRect = addSlider(TileSizeMessage,s.get(StackHeightMessage),CrossId.ZoomSlider);
        zoomRect.min=MIN_CHIP_SCALE;
        zoomRect.max=MAX_CHIP_SCALE;
        zoomRect.value=INITIAL_CHIP_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = ZoomHighlightColor;
        zoomRect.helpText = s.get(AdjustChipSpacing);
        
        bb = new CrossfireBoard(info.getString(OnlineConstants.GAMETYPE, Crossfire_INIT),
        		repeatedPositions,getStartingColorMap());
        useDirectDrawing(true);
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),bb.reverseY(),deferredEvents);

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
    	{ startFirstPlayer();
    	}
    }

    boolean vertical = false;
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {   boolean planned = plannedSeating() ;
		int buttonH = unitsize*3/2;

    	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	Rectangle prisoner = prisonerRects[player];
    	int doneW = planned ? buttonH*2 : 0;
    	int pwidth = unitsize*2;
    	Rectangle box;
    	if(vertical)
    	{
    		box = pl.createRectangularPictureGroup(x+(planned?0:buttonH),y,2*unitsize/3);
        	int boxH = G.Height(box);
    	   	G.SetRect(chip, x, y+(planned ? boxH:0), buttonH,buttonH);
        	G.SetRect(done, x+buttonH+buttonH/4, y+boxH, doneW,doneW/2);
          	G.SetRect(prisoner,G.Right(box),y,pwidth,unitsize*3);
    	}
    	else
    	{
       		box = pl.createRectangularPictureGroup(x+buttonH,y,2*unitsize/3);
        	int boxH = G.Height(box);
        	int boxR = G.Right(box);
    	   	G.SetRect(chip, x, y, buttonH,buttonH);
           	G.SetRect(prisoner,boxR,y,pwidth,boxH);
           	G.SetRect(done, boxR+pwidth+unitsize/2, y+unitsize/2, doneW,doneW/2);
    	}
       	pl.displayRotation = rotation;
       	G.union(box, done,chip,prisoner);
    	return(box);
    }

    private int nrows = 20;
    public void setLocalBounds(int x, int y, int width, int height)
    {	setLocalBoundsV(x,y,width,height,new double[] {-1,1});	
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double a)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	vertical = a>0;
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
        int stateH = fh*3;
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.70,	// 60% of space allocated to the board
    			0.9,	// aspect ratio for the board
    			fh*3.5,
    			fh*4,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/(nrows+3),(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(nrows*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW-CELLSIZE*3)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	int zoomW = CELLSIZE*4;
    	int boardRight = boardX+boardW;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        int ph = CELLSIZE*4;
        
        G.placeStateRow(stateX,stateY,boardW,stateH,iconRect,stateRect,annotationMenu,reverseViewRect,viewsetRect,noChatRect);
  
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    		
            G.SetRect(player1Reserve,
        		boardRight-CELLSIZE,
        		boardY+CELLSIZE*6,
            		ph, ph);
            
            G.AlignXY(player2Reserve,
        		boardRight-CELLSIZE,
        		boardBottom-ph-CELLSIZE*3,
            		player1Reserve);

 
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
        G.placeRow(boardX, boardBottom-stateH,boardW,stateH,goalRect,liftRect);
        G.placeRight(goalRect, zoomRect, zoomW);
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,rackBackGroundColor);
        return boardW*boardH;
    }

	
    private void DrawReverseMarker(Graphics gc, CrossfireBoard gb,Rectangle r,HitPoint highlight)
    {	CrossfireChip king = CrossfireChip.getChip(gb.reverseY()?1:0);
    	CrossfireChip reverse = CrossfireChip.getChip(gb.reverseY()?0:1);
    	GC.frameRect(gc,Color.black,r);
    	int w = G.Width(r);
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	reverse.drawChip(gc,this,w,cx,cy-w/4,null);
    	king.drawChip(gc,this,w,cx,cy+w/4,null);
    	HitPoint.setHelpText(highlight,r,CrossId.ReverseViewButton,s.get(ReverseViewExplanation));
     }  


	// draw a box of spare chips. It's mostly for visual effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,CrossfireBoard gb)
    {


        if (gc != null)
        { // draw a random pile of chips.  It's just for effect
        	int inplay = INITIAL_CHIPS_IN_PLAY - gb.prisoners[player].height();
            GC.frameRect(gc, Color.black, r);
            CrossfireChip chip = CrossfireChip.getChip(gb.getColorMap()[player]);
            GC.setColor(gc,Color.yellow);
            chip.drawChip(gc,this,r,""+inplay);
         }
    }
    

	// draw a box of spare chips. It's mostly for visual effect, but if you
    // wish you can pick up and drop chips.
    private void DrawReserve(Graphics gc, Rectangle r, int player, HitPoint highlight,CrossfireBoard gb)
    {
    	CrossfireCell res = gb.reserve[player];
    	boolean canHit = (gb.isSource(res) || gb.LegalToHitChips(player)) 
    						&& ((gb.pickedObject!=null) || (res.topChip()!=null));
    	int xp = G.centerX(r);
    	int yp = G.Bottom(r)-G.Width(r)/2;
    	tileImages[BOWL_BASE_INDEX].drawChip(gc, this, (int)(CELLSIZE*CHIP_SIZE_SCALE*1.3),xp,yp, null);
         { // draw a random pile of chips.  It's just for effect
            //G.frameRect(gc, Color.black, r);
            if(res.drawStack(gc,this,canHit?highlight:null,(int)(CELLSIZE*CHIP_SIZE_SCALE),xp,yp,0,0.1,null))
            {
            	highlight.hitObject = res;
            	highlight.awidth = CELLSIZE;
            	highlight.arrow = (gb.pickedObject==null) ? StockArt.UpArrow:StockArt.DownArrow;
            	highlight.spriteColor = Color.red;
            	
            }
         }
         HitPoint.setHelpText(highlight, r, s.get(ReserveString,s.get(colorNames[player])));

    }
    
    private void DrawPrisoners(Graphics gc, Rectangle r, int player, HitPoint highlight,CrossfireBoard gb)
    {
    	CrossfireCell res = gb.prisoners[player];
    	boolean canHit = (gb.board_state==CrossfireState.PUZZLE_STATE) && gb.LegalToHitChips(player) && ((gb.pickedObject!=null) || (res.topChip()!=null));
    	int w = G.Width(r);
    	int xp = G.centerX(r);
    	int yp = G.Bottom(r)-w/2;
        { 
            if(res.drawStack(gc,this,canHit?highlight:null,w,xp,yp,0,0.05,null))
            {
            	highlight.hitObject = res;
            	highlight.awidth = CELLSIZE;
            	highlight.arrow = (gb.pickedObject==null) ? StockArt.UpArrow:StockArt.DownArrow;
            	
            }
         }
        HitPoint.setHelpText(highlight, r, s.get(PrisonerString,s.get(colorNames[player])));
        
     	//tileImages[BOWL_INDEX].drawChip(gc, this, r.width,r.x+r.width/2,r.y+r.width/2, null);

    }
    
    // adjust y size as a function of position.  If in the board rect, and not in the reserve rects.
    private double ysizeadj(int xp,int yp)
    {
    	return( (G.pointInRect(xp,yp,boardRect)
    			&& !G.pointInRect(xp,yp,player1Reserve)
    			&& !G.pointInRect(xp,yp,player2Reserve))
    		? Math.sqrt((double)yp/G.Height(boardRect)*2) 
    		: 1.0);
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
     	CrossfireChip.getChip(obj).drawChip(g,this,(int)(CELLSIZE*CHIP_SIZE_SCALE*ysizeadj(xp,yp-G.Top(boardRect))), xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  


    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { // erase
      boolean backgroundReview = reviewMode() && !mutable_game_record;
      GC.setColor(gc,backgroundReview ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(backgroundReview)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
      boolean perspective = usePerspective();
       
      if(perspective)
      {
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     tileImages[HEXTILE_PBOARD_INDEX].getImage(loader).centerImage(gc, boardRect);
      
      bb.SetDisplayParameters( 0.93, 0.825,
      		  0,0.35,
      		  0.0,
      		  0.2, 0.18,0.0); // shrink a little and rotate 30 degrees
      }
      else
      {
    	  // if the board is one large graphic, for which the visual target points
          // are carefully matched with the abstract grid
         tileImages[HEXTILE_NP_INDEX].getImage(loader).centerImage(gc, boardRect);
          
          bb.SetDisplayParameters( 0.97,0.97,0.1,-0.5,0); // shrink a little and rotate 30 degrees

      }

      bb.SetDisplayRectangle(boardRect);
  

      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      bb.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridFillColor, GridTextColor,GridColor);

 
    }
    
    private void drawCellMarker(Graphics gc,CrossfireBoard gb,Rectangle brect,CrossfireCell cell)
    {	if(cell!=null && cell.onBoard)
    	{
    	int celly = gb.cellToY(cell);
   	 	int ypos = G.Bottom(brect) - celly;
   	 	int xpos = G.Left(brect) + gb.cellToX(cell);
   	 	tileImages[CELL_INDEX].drawChip(gc, this, (int)(CELLSIZE*CHIP_SIZE_SCALE*1.7*ysizeadj(xpos,ypos-G.Top(brect))),xpos,ypos, null);
    	}
    }
   /** draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */
    private void drawBoardElements(Graphics gc, CrossfireBoard gb, Rectangle brect, HitPoint highlight)
    {	
    	double yscale = zoomRect.value;
    	boolean dolift = doLiftAnimation();

    	Hashtable<CrossfireCell,CrossfireCell> dests = gb.getDests();
        boolean picked = (gb.pickedObject!=null);

        // mark the to-from squares with a puddle of color
        if((gb.pickedSource!=null) || (gb.droppedDest!=null))
        {
        	drawCellMarker(gc,gb,brect,gb.pickedSource);
            drawCellMarker(gc,gb,brect,gb.droppedDest);
        }
        else {
        	drawCellMarker(gc,gb,brect,gb.prevPickedSource);
            drawCellMarker(gc,gb,brect,gb.prevDroppedDest);
       }
        Enumeration<CrossfireCell>cells = gb.getIterator(Itype.TBRL);
        boolean perspective = usePerspective();
        int top = G.Top(brect);
     	while(cells.hasMoreElements())
        	{	
            CrossfireCell cell = cells.nextElement();
        	int celly = gb.cellToY(cell);
       	 	int ypos = G.Bottom(brect) - celly;
       	 	int xpos = G.Left(brect) + gb.cellToX(cell);
       	 	CrossfireCell moveAsDest = (dests.get(cell));
       	 	boolean isSource = gb.LegalToHitBoard(cell);
            boolean drawhighlight = 
            	(moveAsDest!=null) 	// is a dest
            	|| isSource
				|| gb.isDest(cell) 		// is legal for a "drop" operation
				|| (picked && gb.isSource(cell));	// is legal for a "pick" operation+
            
        	int csize = (int)(CELLSIZE*CHIP_SIZE_SCALE*(perspective ? ysizeadj(xpos,ypos-top) : 1));

            //StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            String msg = "";
            if((cell.chipIndex>1) && (dolift || (yscale<=(MIN_CHIP_SCALE+0.01)))) { msg += cell.height(); }
            if(cell.drawStack(gc,this,drawhighlight?highlight:null,csize,xpos,ypos,liftSteps,perspective?0:yscale,perspective?yscale:0,msg))
            {
                boolean empty = (cell.topChip() == null);
                highlight.hitCode = (empty||picked) ? CrossId.EmptyBoard : CrossId.BoardLocation;
                highlight.arrow = (empty||picked) ? StockArt.DownArrow : StockArt.UpArrow;
                highlight.awidth = CELLSIZE;
                highlight.spriteColor = Color.red;
            }
            if (drawhighlight && picked && gb.pickedSource!=null && gb.pickedSource.onBoard)
             { // checking for pointable position
            	 StockArt.SmallO.drawChip(gc,this,CELLSIZE*3,xpos,ypos,null);                
             }

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
    {  CrossfireBoard gb = disB(gc);
      
       CrossfireState state = gb.getState();
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
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
         {	commonPlayer pl = getPlayerOrTemp(i);
         	pl.setRotatedContext(gc, selectPos,false);
            DrawChipPool(gc, chipRects[i],i, ourTurnSelect,gb);
            DrawPrisoners(gc,prisonerRects[i],i,ourTurnSelect,gb);
             if(planned && (i==gb.whoseTurn))
             {
             	handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
     					HighlightColor, rackBackGroundColor);	
             }
             pl.setRotatedContext(gc, selectPos,true);
         }
         
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       DrawReserve(gc,player1Reserve,FIRST_PLAYER_INDEX,ourTurnSelect,gb);
       DrawReserve(gc,player2Reserve,SECOND_PLAYER_INDEX,ourTurnSelect,gb);
       GC.setFont(gc,standardBoldFont());
       

		if (state != CrossfireState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos, HighlightColor, rackBackGroundColor);
        }

 
        drawPlayerStuff(gc,(state==CrossfireState.PUZZLE_STATE),nonDragSelect,HighlightColor,rackBackGroundColor);
  
 
        // draw the avatars
            standardGameMessage(gc,messageRotation,
            		state==CrossfireState.GAMEOVER_STATE?gameOverMessage():s.get(state.getDescription()),
            				state!=CrossfireState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
            CrossfireChip.getChip(gb.getColorMap()[gb.whoseTurn]).drawChip(gc, this, iconRect, null);
            goalAndProgressMessage(gc,nonDragSelect,s.get(VictoryCondition),progressRect, goalRect);
        
        drawLiftRect(gc,liftRect,nonDragSelect,textures[LIFT_ICON_INDEX]);
        zoomRect.draw(gc,nonDragSelect);
        DrawReverseMarker(gc,gb,reverseViewRect,selectPos);
        drawViewsetMarker(gc,viewsetRect,selectPos);
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
        int sz = (int)(CELLSIZE*CHIP_SIZE_SCALE);
        startBoardAnimations(replay,bb.animationStack,sz,MovementStyle.Stack);
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay!=replayMode.Replay) { playSounds(mm); }
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
        return (new CrossfireMovespec(st, player));
    }
    public commonMove EditHistory(commonMove m)
    {
    	if(m.op==MOVE_REJECT) { return null; }
    	return EditHistory(m,m.op==MOVE_PICK);
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
        if (hp.hitCode instanceof CrossId) // not dragging anything yet, so maybe start
        {
       	CrossId hitObject =  (CrossId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
	    
	    case Black_Chip_Pool:
	    case White_Chip_Pool:
	    case White_Prisoner_Pool:
	    case Black_Prisoner_Pool:
	    	PerformAndTransmit("Pick "+hitObject.shortName);
	    	break;
	    case BoardLocation:
	        CrossfireCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    	break;
        }

        }
    }
	private void doDropChip(CrossfireCell to)
	{	CrossfireState state = bb.getState();
		CrossfireChip mo = bb.pickedObject;
		if(mo==null) { mo=bb.lastPicked; }
		if(mo==null) { mo=CrossfireChip.getChip(bb.whoseTurn); }
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case PUZZLE_STATE:
			{
				PerformAndTransmit("dropb "+mo.colorName+" "+to.col+" "+to.row+" "+0);
			}
		break;
		case CONFIRM_STATE:
		case PLAY_STATE:
			CrossfireCell from = bb.pickedSource;
			int dis = bb.findDistance(from,to);
			PerformAndTransmit("dropb "+mo.colorName	+ " "+to.col+" "+to.row+" "+dis);
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
        if (!(id instanceof CrossId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
   		CrossId hitCode = (CrossId)id;
        CrossfireCell hitObject = hitCell(hp);
        CrossfireState state = bb.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case ReverseViewButton:
        	{boolean v = !bb.reverseY();
        	 bb.setReverseY(v);
          	 reverseOption.setState(v);
          	 generalRefresh();
        }
          	 break;
        case ZoomSlider:
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
				if(!bb.isDest(hitObject))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					G.Error("shouldn't hit a chip in state %s",state);
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
					throw G.Error("Not expecting hit in state %s",state);
				case CONFIRM_STATE:
				case PLAY_STATE:
				case PUZZLE_STATE:
					doDropChip(bb.getCell(hitObject));
					break;
			}
			break;
		
        case Black_Prisoner_Pool:
        case White_Prisoner_Pool:
        case Black_Chip_Pool:
        case White_Chip_Pool:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+hitCode.shortName);
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
    	   // in games which have a randomized start, this method would return
    	   // return(bb.gametype+" "+bb.randomKey); 
    	return(bb.gametype); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Crossfire_SGF); }	// this is the official SGF number assigned to the game

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link #gameType}
     */
     public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // int rk = G.IntToken(his);
    	// bb.doInit(token,rk);
        bb.doInit(token);
    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
    	if(!handled)
    	{if(target==reverseOption)
	    	{
	    	bb.setReverseY(reverseOption.getState());
	    	generalRefresh();
	    	handled=true;
	    	}}

        return (handled);
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


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new CrossfirePlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
	summary: 5/23/2023
		746 files visited 0 problems
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
}

