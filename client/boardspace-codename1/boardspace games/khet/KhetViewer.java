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
package khet;

import bridge.*;
import common.GameInfo;

import com.codename1.ui.geom.Rectangle;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.SimpleSprite.Movement;
import lib.*;




/**
 * 
 * Change History
 *
 * May 2007 initial work in progress. 
 *
 * 
 */
public class KhetViewer extends CCanvas<KhetCell,KhetBoard> implements KhetConstants
{
     /**
	 */
	static final String Khet_SGF = "Khet"; // sgf game name
    // file names for jpeg images and masks
    static final String ImageDir = "/khet/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"};
    static final int BOARD_INDEX = 0;
    static final int BOARD_FLIPPED_INDEX = 1;
    static final int BOARD_NP_INDEX = 2;
    static final double ImageScales[][] = {{0.5,0.5,1.0},{0.83,0.495,1.19},{0.895,0.502,1.21}};
    static final String ImageNames[] = {"board","board-flipped","board-np"};
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private Color chatBackgroundColor = new Color(0.85f,0.85f,0.85f);
    // images
    private static Image[] textures = null;// background textures
    private static StockArt[] images = null;
    
    // private state
    private KhetBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle rackRects[] = addRect("rack",2);
    
    private Rectangle reverseViewRect = addRect("reverse");
   
    private Rectangle repRect = addRect("repRect");
    
    private boolean usePerspective() { return((getAltChipset()&2)==0); }
    
    public int getAltChipset() 
    { 
    	int n = super.getAltChipset();
    	return( (n<<1) | ((b.reverseXneqReverseY()) ? 1 : 0 )); 
    }
    
    public synchronized void preloadImages()
    {	
       	KhetChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = StockArt.preLoadArt(loader,ImageDir,ImageNames,ImageScales);
    	}
        gameIcon = KhetChip.getChip(3).image;
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
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        int map[] = getStartingColorMap();
        b = new KhetBoard(info.getString(GameInfo.GAMETYPE, Khet_Classic_Init),randomKey,map);
        if(seatingFaceToFace()) { b.autoReverseYNormal(); }
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
    boolean vertical = true;
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*3;
    	int chipH = unitsize*3;
    	int doneW = plannedSeating() ? unitsize*4 : 0;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	Rectangle done = doneRects[player];
    	Rectangle rack = rackRects[player];
    	
    	G.SetRect(chip, x, y+unitsize/2, chipW, chipH);
    	int boxR = G.Right(box);
    	G.SetRect(done, boxR+unitsize/2,y+unitsize/2,doneW,doneW/2);
    	if(vertical)
    	{
    	G.SetRect(rack,x,G.Bottom(box),unitsize*20,unitsize*3);
    	}else
    	{
    	   	G.SetRect(rack,boxR+doneW+doneW/4,y,unitsize*20,unitsize*3);
    	}
    	pl.displayRotation = rotation;
    	
    	G.union(box, chip,done,rack);
    	return(box);
    }
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	setLocalBoundsV(x,y,width,height,new double[] {1,-1});
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double a)
    {	G.SetRect(fullRect, x, y, width, height);
    	vertical = a>0;
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*14;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.55,	// % of space allocated to the board
    			1.2,	// 1.2:1 aspect ratio for the board
    			fh*2.0,	//  cell size
    			fh*2.5,
    			0.1		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);

    	Rectangle main = layout.getMainRectangle();
    	int stateH = fh*5/2;
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	boolean rotated = !usePerspective() && seatingFaceToFaceRotated();
    	int nrows = rotated ? b.ncols : b.nrows;  
        int ncols = rotated ? b.nrows : b.ncols;

     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH/2)/nrows);
    	int CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(nrows*cs);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH-CELLSIZE/2)/2);
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
        placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	if(rotated)
    	{	G.setRotation(boardRect,-Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW,stateH,goalRect,reverseViewRect,viewsetRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,chatBackgroundColor);
 
        return boardW*boardH+G.Height(getPlayerOrTemp(0).playerBox);
   }
    
	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	KhetChip king = KhetChip.getChip(b.reverseY()?1:0,KhetChip.PHAROH_INDEX);
    	KhetChip reverse = KhetChip.getChip(b.reverseY()?0:1,KhetChip.PHAROH_INDEX);
    	int sz = 3*G.Width(r)/4;
    	int cx = G.centerX(r);
    	int cy = G.centerY(r)+sz/6;
    	king.drawChip(gc,this,sz,cx,cy-sz/4,null);
    	reverse.drawChip(gc,this,sz,cx,cy+sz/4,null);
    	HitPoint.setHelpText(highlight,r,KhetId.ReverseViewButton,s.get(ReverseViewExplanation));
 
     }  

    private void DrawPlayerChip(Graphics gc, int forPlayer, Rectangle r)
    {	
        KhetChip thisChip = KhetChip.getChip(b.getColorMap()[forPlayer],KhetChip.PHAROH_INDEX);
        thisChip.drawChip(gc,this,r,null);
    }
	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawPlayerRack(Graphics gc, KhetBoard gb,int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	KhetCell rack[] = gb.rack[forPlayer];
    	int w = G.Height(r);
    	int h = w;
    	int xp = G.Left(r)+w/2;
    	int yp = G.Top(r)+h/2;
    	int ncells = 0;
    	for(int i=0,lim = rack.length; i<lim;i++) { if(rack[i].topChip()!=null){ ncells++; }}
    	int step = Math.min((G.Width(r)-w)/(ncells+1),w);
    	KhetCell hitCell = null;
    	for(int i=0,lim=rack.length; i<lim;i++)
    	{
    		KhetCell thisCell = rack[i];
 
        	boolean canHit = gb.LegalToHitChips(forPlayer,thisCell);
    		if(thisCell.drawStack(gc,this,canHit?highlight:null,w,xp,yp,0,1.0,null))
    		{
    			hitCell = thisCell;
    		}
    		if(thisCell!=null) { xp += step; }
     	}
    	if(hitCell!=null)
    	{
    		highlight.awidth=SQUARESIZE/3;
			highlight.arrow = gb.pickedObject==null ? StockArt.UpArrow : StockArt.DownArrow;
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
    	KhetChip ch = KhetChip.getChip(obj);// Tiles have zero offset
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
    Image background = null;
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      KhetBoard gb = disB(gc);
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
      if(!usePerspective())
      {	  int cx = G.centerX(brect);
      	  int cy = G.centerY(brect);
    	  boolean reverse = !gb.reverseY();
    	  if(reverse) { GC.setRotation(gc, Math.PI,cx,cy); }
    	  Image board = images[BOARD_NP_INDEX].getImage();
    	  if(board!=background) { scaled = null; }
    	  background = board;
    	  scaled = board.centerScaledImage(gc,brect,scaled);
       	  if(reverse) { GC.setRotation(gc, -Math.PI,cx,cy); }
       	  gb.SetDisplayParameters(
       			  new double[] {0.11,0.12},
       			  new double[]{0.99,0.12}, 
       			  new double[] {0.11,0.98},
       			  new double[] {0.99,0.98});
     }
      else
      {
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
    	Image board = images[gb.reverseY()?BOARD_INDEX:BOARD_FLIPPED_INDEX].getImage();
    	if(board!=background) { scaled = null; }
    	background = board;
    	scaled = board.centerScaledImage(gc, brect,scaled);
    	gb.SetDisplayParameters(0.91,0.91, 
	    		0.08,0.20,0.0,  
	    		0.114,0.1,
	    		0);
      }
	  gb.SetDisplayRectangle(brect);

      gb.DrawGrid(gc,brect,use_grid,Color.white,Color.white,Color.blue,Color.white);
    }
    int adjustedSquareSize(int startingSize,int ydistance,int height)
    {
    	return(startingSize-(int)(startingSize*ydistance*0.05/height));
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, KhetBoard gb, Rectangle brect, HitPoint highlight)
    {
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	
    	KhetState state= gb.getState();
        boolean useAnyDest = (gb.getState()==KhetState.PUZZLE_STATE);
        Hashtable<KhetCell,KhetCell>dests = gb.getDests();
        boolean moving = hasMovingObject(highlight);
        KhetCell theSource = moving ? gb.getSource() : null; 
        Hashtable<KhetCell,KhetCell>sources = gb.getSources();
        boolean rotated = (gb.rotatedCell!=null);
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
        {KhetCell lastDest = gb.lastDest;
        if(lastDest!=null && lastDest.onBoard)
        {
        int ydistance = gb.cellToY(lastDest.col, lastDest.row);
      	int ypos = G.Bottom(brect) - ydistance;
        int xpos = G.Left(brect) + gb.cellToX(lastDest.col, lastDest.row);
        StockArt.SmallO.drawChip(gc,this,SQUARESIZE*4,xpos,ypos-SQUARESIZE/6,null);
        }}

        Enumeration<KhetCell> cells = gb.getIterator(Itype.LRTB);
    	// overall drawing is always left to right, top to bottom
        while(cells.hasMoreElements())
        {
        	KhetCell cell = cells.nextElement();
            boolean isDest = dests.get(cell)!=null;
            boolean isSource = rotated ? cell==gb.rotatedCell : (sources.get(cell)!=null);
            int ydistance = gb.cellToY(cell);
            KhetChip top = cell.topChip();
            int ypos = G.Bottom(brect) - ydistance;
            int xpos = G.Left(brect) + gb.cellToX(cell);
            int adjustedSize = adjustedSquareSize(SQUARESIZE,ydistance,G.Height(brect));
            boolean useAnyDestHere = useAnyDest && !gb.isForbiddenSpace(cell,gb.pickedObject);
            boolean pick = cell.drawStack(gc,this,((cell==theSource)||isSource||isDest||useAnyDestHere)?highlight:null,adjustedSize,xpos,ypos,0,0.1,null);
           if(  pick)
            	{ 
                   boolean rotate = rotated 
                   			|| ((top!=null) 
                   					&& !moving
                   					&& !gb.isDest(cell)
                   					&& top.canRotate()
                   					&& ((!top.canMove()&&(state!=KhetState.PUZZLE_STATE))
                   							|| G.distance(G.Left(highlight),G.Top(highlight),xpos,ypos)>(SQUARESIZE/4)));
            	  if(rotate && (top!=null))
            	  {	// must be in the rotation range
            		  KhetChip cw = top.getRotated(RotateCW);
            		  KhetChip ccw = top.getRotated(RotateCCW);
            		  boolean rotateCW = rotated  
            		  	? (gb.rotatedDirection==RotateCCW) 
            		  	: !cw.canRotateToward(cell.exitTo(cw.getRotation())) 
            		  			? false
            		  			: !ccw.canRotateToward(cell.exitTo(ccw.getRotation()))
            		  					? true
            		  					: (G.Left(highlight)>xpos);
            		  highlight.hitCode = rotateCW ? KhetId.Rotate_CW : KhetId.Rotate_CCW;
            		  highlight.arrow = rotateCW ? StockArt.Rotate_CW : StockArt.Rotate_CCW;
            		  highlight.awidth = 2*SQUARESIZE/3;
            		  highlight.spriteColor = Color.blue;
            	  }
            	  else
            	  {
            		  highlight.arrow = moving 
            			? StockArt.DownArrow 
            			: cell.height()>0?StockArt.UpArrow:null;
            		highlight.awidth = SQUARESIZE/2;
            		highlight.spriteColor = Color.red;
            	  }
            	}
            if(isDest) 
            	{ StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null); 
            	}
			//mark center of all squares
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);

        	}
    	}
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  
       DrawReverseMarker(gc,reverseViewRect,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    { KhetBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      KhetState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        boolean planned = plannedSeating();
    
        GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
        drawBoardElements(gc, gb, boardRect, ot);
        GC.unsetRotatedContext(gc,highlight);
        
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
        {	commonPlayer pl = getPlayerOrTemp(i);
        	pl.setRotatedContext(gc, highlight, false);
            DrawPlayerChip(gc, i,chipRects[i]);
            DrawPlayerRack(gc, gb,i,rackRects[i], gb.whoseTurn,ot);
        	if(planned && (i==gb.whoseTurn))
        	{	HitPoint hit = (gb.DoneState() ? select : null);
        		if(GC.handleRoundButton(gc,doneRects[i],hit, 
    					s.get(FireAction),HighlightColor, rackBackGroundColor))
        	{
        			 hit.hitCode = GameId.HitDoneButton;
        			 HitPoint.setHelpText(hit,doneRects[i],s.get(FireExplanation));
        		}
        	}
        	pl.setRotatedContext(gc, highlight, true);
        }	
 
 		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
 		double messageRotation = pl.messageRotation();
 
        GC.setFont(gc,standardBoldFont());
		if (vstate != KhetState.PUZZLE_STATE)
        {	HitPoint hit = (gb.DoneState() ? select : null);
			if(!planned) 
			{ if(GC.handleRoundButton(gc,doneRect,hit, 
					s.get(FireAction),HighlightColor, rackBackGroundColor))
        {
    			 hit.hitCode = GameId.HitDoneButton;
    			 HitPoint.setHelpText(hit,doneRect,s.get(FireExplanation));
    		}
			}
			
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
                }

 		drawPlayerStuff(gc,(vstate==KhetState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);

       standardGameMessage(gc,messageRotation,
        		vstate==KhetState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=KhetState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        goalAndProgressMessage(gc,ourSelect,s.get(VictoryCondition),progressRect, goalRect);
        DrawPlayerChip(gc, gb.whoseTurn,iconRect);
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        drawAuxControls(gc,ourSelect);
        drawVcrGroup(ourSelect, gc);
        drawViewsetMarker(gc,viewsetRect,ourSelect);

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
     
     double animationSpeed = 0.1;
     void startBoardAnimations(replayMode replay)
     {	CellStack animationStack = b.animationStack;
     	try {
        if(replay.animate)
     {	
     		int lim = animationStack.size();
      		if(lim>0)
     		{
      		double maxStart = 0.0;
      		double start = 0.0;
      		int blastSize = SQUARESIZE;
     		for(int i = 0; i<lim; i+=2)
     		{
     		KhetCell src = animationStack.elementAt(i);
     		KhetCell dest = animationStack.elementAt(i+1);
     		int distance = Math.abs(src.row-dest.row) + Math.abs(dest.col-src.col);
     		double speed = masterAnimationSpeed*animationSpeed*distance;
     		KhetChip top = dest.topChip();
     		int itemsize = dest.onBoard  
     						? adjustedSquareSize(SQUARESIZE,G.Bottom(boardRect)-src.centerY(),G.Height(boardRect))
     						: SQUARESIZE;
     		int animsize = itemsize;
     		if((src.rackLocation==KhetId.LaserProxy)||(src.rackLocation==KhetId.EyeProxy))
     		{
     			start = src.row*animationSpeed;
     			animsize = blastSize;
     		}
     		else { start = maxStart; }
     		if(dest.rackLocation==KhetId.EyeProxy) { blastSize = Math.max(SQUARESIZE/5,(int)(blastSize/Math.sqrt(2))); }
     		startAnimation(src,dest,top,start,speed,animsize,animsize);
     		if(!dest.onBoard)
     		{	// it's a capturing move.  Keep a proxy visible until it moves off
     			startAnimation(src,src,top,0.0,maxStart,itemsize,itemsize);
     			startAnimation(src,src,KhetChip.Blast,maxStart-0.1,1.0,itemsize/2,itemsize*2);
     			//G.print("Blast "+maxStart);
     		}
     		else
     		{
     		start += speed;
     		maxStart = Math.max(start,maxStart);
     		}
     		}}
     	}}
        finally {
      		animationStack.clear();
     	}
       
     } 
     //
     // animations on the same cell will be drawn in the reverse of the order they are added.
     // this is needed so transparent crawl cookies are drawn after the solid underlying cookie.
     //
     SimpleSprite startAnimation(KhetCell from,KhetCell to,KhetChip top,double start,double speed,int size,int finalsize)
     {	
    	SimpleSprite sprite = super.startAnimation(from,to,top,size,start,speed);
     	if(sprite!=null)
     	{
     		sprite.finalsize = finalsize;
     		sprite.movement = Movement.Linear;
     		if(from==to && from==b.rotatedCell)
      		{	
     			sprite.start_rotation = Math.PI/2*(b.rotatedDirection==RotateCCW?1:-1);
     			sprite.specifiedDuration = 0.5*masterAnimationSpeed;
      		}
 
     	}
     	return sprite;
     }  
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new KhetMovespec(st, player));
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
//    	CarnacMovespec newmove = (CarnacMovespec) nmove;
//    	CarnacMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            CarnacMovespec m = (CarnacMovespec) History.elementAt(idx);
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
//                	{ CarnacMovespec m2 = (CarnacMovespec)History.elementAt(idx-1);
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
        if (hp.hitCode instanceof KhetId)// not dragging anything yet, so maybe start
        {
        KhetId hitObject = (KhetId)hp.hitCode;
		KhetCell cell = hitCell(hp);
		KhetChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
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
        if(!(id instanceof KhetId)) {  missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
    	missedOneClick = false;
   		KhetId hitObject = (KhetId)hp.hitCode;
        KhetState state = b.getState();
		KhetCell cell = hitCell(hp);
		KhetChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:       	
        	throw G.Error("Hit Unknown: %s", hitObject);
        case Rotate_CW:
        	PerformAndTransmit("Rotate CW "+cell.col+" "+cell.row);
        	break;
        case Rotate_CCW:
        	PerformAndTransmit("Rotate CCW "+cell.col+" "+cell.row);
        	break;
        case ReverseViewButton:
       	 b.setReverseY(!b.reverseY());
       	 generalRefresh();
       	 break;

         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
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
			
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  (hitObject==KhetId.Black_Chip_Pool) ? " B " : " W ";
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                	case PLAY_STATE:
            		PerformAndTransmit(RESET);
            		break;

               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop"+col+cell.row);
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
     * token, which ought to be parsable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() 
    { 
    	return(""+b.gametype+" "+b.randomKey); 
   }
    public String sgfGameType() { return(Khet_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(Tokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long rk = his.longToken();
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
    public SimpleRobotProtocol newRobotPlayer() { return(new KhetPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 12/1/2025
     *  11363 files visited 0 problems
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
            {	Tokenizer st = new Tokenizer(value);
            	String typ = st.nextToken();
            	long ran = st.longToken();
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

