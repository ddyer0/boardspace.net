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
package entrapment;

import javax.swing.JCheckBoxMenuItem;
import java.awt.Rectangle;
import common.GameInfo;

import static java.lang.Math.pow;
import java.awt.*;
/* below here should be the same for codename1 and standard java */
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;
import lib.Graphics;
import lib.Image;
import lib.*;



/**
 * 
 * Change History
 *
 * May 2007 initial work in progress. 
 *
*/

public class EntrapmentViewer extends CCanvas<EntrapmentCell,EntrapmentBoard> implements EntrapmentConstants
{	
     /**
	 * 
	 */
    static final String Entrapment_SGF = "Entrapment"; // sgf game name
    
    
    // file names for jpeg images and masks
    static final String ImageDir = "/entrapment/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
   // static final int LIFT_ICON_INDEX = 2;
    static final int BOARD_INDEX = 0;
    static final int STICK_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final int BOARD_NP_INDEX = 2;
    static final int STICK_NP_INDEX = 3;
    static final String ImageNames[] = { "board","stick","board-np","stick-np"};
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "entrapment-icon-nomask",
    	  //,"lift-icon"
    	  };

	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackGroundColor = new Color(222,184,144);
    private Color rackBackGroundColor = new Color(202,162,122);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] textures = null; // background square textures
    private static Image[] images = null;	// main masked images
    // private state
    private EntrapmentBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle barrierRects[] = addRect(",barrier",2);
    private Rectangle roamerRects[] = addRect(",roamer",2);
    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;
    private Rectangle chipRects[]= addRect(",chip",2);
    private Rectangle repRect = addRect("repRect");
    public synchronized void preloadImages()
    {	
       	EntrapmentChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
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
    	enableAutoDone = true;
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new EntrapmentBoard(info.getString(GameInfo.GAMETYPE, Entrapment_INIT),
        		randomKey,getStartingColorMap(),EntrapmentBoard.REVISION);
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
        b.doInit(b.gametype,b.randomKey,b.revision);			// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
    }

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle barrier = barrierRects[player];
    	Rectangle roamer = roamerRects[player];
    	int chipW = unitsize*3;
    	int doneW = plannedSeating() ? unitsize*4 : 0;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x,y,chipW,chipW);
    	G.SetRect(done,x,y+chipW,doneW,doneW/2);
    	
    	G.SetRect(barrier,x+doneW+unitsize/3, G.Bottom(box),G.Width(box)-doneW-unitsize/3,unitsize*5/2);
    	G.union(box, done,chip,barrier);
    	G.SetRect(roamer, G.Right(box), y, unitsize*2,G.Height(box));
    	G.union(box, roamer);
    	pl.displayRotation = rotation;
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
    	int minLogW = fh*20;	
    	int vcrw = fh*16;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int ncols = 14;
        int buttonW = fh*8;
        double minSize = fh*2;
        double maxSize = fh*2.5;
        
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,		// margin for allocated boxes
    			0.75,	// 70% of space allocated to the board
    			0.9,	// aspect ratio for the board
    			minSize,// minimum cell size
    			maxSize,// maximum cell size
    			0.2		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,4*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*2);
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);
    	layout.placeDoneEditRep(buttonW, buttonW*3/2,doneRect, editRect, repRect);
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);

    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/ncols);
    	int CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE*2;
    	int C2 = CELLSIZE/2;

    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(ncols*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = (mainH-boardH)/2;
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
       	layout.returnFromMain(extraW,extraH);
  	

    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY+C2/2;
        int stateX = boardX;
        int stateH = fh*5/2;

        placeStateRow(stateX,stateY,boardW,stateH,iconRect,stateRect,annotationMenu,numberMenu,noChatRect);
        
        
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH-C2/2,boardW,stateH,goalRect,reverseViewRect,viewsetRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackGroundColor,chatBackGroundColor);
 	
    }    
    public int cellSize() { return b.cellSize()/2; }
	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	EntrapmentChip king = EntrapmentChip.getChip(b.reverseY()?1:0);
    	EntrapmentChip reverse = EntrapmentChip.getChip(b.reverseY()?0:1);
    	int w = G.Width(r);
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	reverse.drawChip(gc,this,w,cx,cy-w/4,null);
    	king.drawChip(gc,this,w,cx,cy+w/4,null);
    	HitPoint.setHelpText(highlight,r,EntrapmentId.ReverseViewButton,s.get(ReverseViewExplanation));
    	
     }  

	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawBarrierPool(Graphics gc, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	EntrapmentCell chips[]= b.unplacedBarriers;
        boolean canHit = b.LegalToHitBarriers(forPlayer);
        EntrapmentCell thisCell = chips[forPlayer];
        EntrapmentChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        int h = G.Height(r);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null;
        double step = 0.1;
        if(thisCell.drawStack(gc,this,pt,h,(int)(G.Left(r)+step*h),G.centerY(r),0,step,0,null))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = h/3;
        	highlight.spriteColor = Color.red;
        }
     }

	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawRoamerPool(Graphics gc, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	EntrapmentCell chips[]= b.initialSetupPhase ? b.unplacedRoamers : b.deadRoamers;
        boolean canHit = b.LegalToHitRoamers(forPlayer);
        EntrapmentCell thisCell = chips[forPlayer];
        EntrapmentChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        int w = G.Width(r);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 

        int xp = G.Left(r)+w/2;
        int yp = G.Top(r)+G.Height(r)/2-w/2;
        if(canHit && canDrop) 
        	{ StockArt.SmallO.drawChip(gc,this,w*3,xp,yp,null); 
        	}
        int wp = G.Width(r);
        thisCell.drawStack(gc,this,pt,(int)(wp*1.3),xp+wp/8,yp,0,-0.6,null);

        if((highlight!=null) && (highlight.hitObject==thisCell))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = w/3;
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
    	EntrapmentChip ch = EntrapmentChip.getChip(obj);// Tiles have zero offset
    	int ss = G.pointInRect(xp,yp,boardRect) ? yScaleSize(yp,boardRect,SQUARESIZE) : SQUARESIZE;
    	ch.drawChip(g,this,ss,xp,yp,null);
     }



    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }

    public boolean usePerspective() { return(getAltChipset()==0); }

    Image background = null;
    Image scaled = null;
    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      EntrapmentBoard gb = disB(gc);
      // erase
      Rectangle nrect = G.clone(boardRect);
      double coords[] = { 0.8, 0.7,0.6,0.5,0.3,0.177 }; 
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
      boolean perspective = usePerspective();
      
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      int w = G.Width(boardRect);
      int left = G.Left(boardRect);
      int top = G.Top(boardRect);
      Rectangle r = boardRect;
      Image board = images[perspective ? BOARD_INDEX : BOARD_NP_INDEX];
      if(board!=background) { scaled = null; }
      background = board;
      scaled = board.centerScaledImage(gc, r,scaled);
      if(gb.is6x7)
      	{ 
    	  Rectangle srect =
    			  perspective 
    			  	? new Rectangle(
    			  			left+(int)(w*0.079),
    			  			top+(int)(w*0.128),
    			  			(int)(w*0.73),
    			  			w/4)
    			  	: new Rectangle(
    			  			left+(int)(w*0.06),
    			  			top+(int)(w*0.08),
    			  			(int)(w*0.85),
    			  			w/5);
      	 images[perspective ? STICK_INDEX : STICK_NP_INDEX].centerImage(gc,srect);
      	}
      
      setDisplayRectangles();
	    
      for(int i=0;i<coords.length;i++) 
      {	int off = (int)(w*coords[i]);
        G.SetTop(nrect, off);
       // G.centerImage(gc,images[STRIP_0_INDEX+i], nrect, this);
      }
      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    void setDisplayRectangles()
    {  	boolean is6x7 = b.is6x7;
    	if(usePerspective())
     	{
     	if(is6x7)
     	{
     		b.SetDisplayParameters(
  	    		0.88,0.90,	// xscale, yscale
  	    		0.25,0.09,  	// xoff yoff
  	    		9.6,		// rotation 
  	    		0.14, 0.17,		// xperspective, yperspective
  	    		-0.08);	
     	}
     	else {b.SetDisplayParameters(
	    		0.88,0.90,	// xscale, yscale
	    		0.23,0.37,  	// xoff yoff
	    		9.6,		// rotation 
	    		0.14, 0.17,		// xperspective, yperspective
	    		-0.08);			// skew
     	}
     	}
     	else
     	{
         	if(is6x7)
         	{
         		b.SetDisplayParameters(
        	    		0.89,1.0,		// xscale, yscale
        	    		0.0,-0.45,  	// xoff yoff
        	    		0,			// rotation 
        	    		0.0, 0.0,		// xperspective, yperspective
        	    		-0.0);			// skew
         	}
         	else 
         	{
      		b.SetDisplayParameters(
    	    		1.01,1.0,		// xscale, yscale
    	    		0.0,0.05,  	// xoff yoff
    	    		0,			// rotation 
    	    		0.0, 0.0,		// xperspective, yperspective
    	    		-0.0);			// skew
         	}
     	}
	    b.SetDisplayRectangle(boardRect);

    }
    private int yScaleSize(int ypos,Rectangle brect,int sz)
    {  	double scl = usePerspective()
    					? pow(((ypos-G.Top(brect))/(double)G.Height(brect)),0.1)
    					: 1.0;
		int ss = (int)(sz*scl);
		return(ss);
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, EntrapmentBoard gb, Rectangle brect, HitPoint highlight)
    {	//setDisplayRectangles();
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	EntrapmentChip moving = gb.pickedObject;
    	Hashtable<EntrapmentCell,EntrapmentChip> dead = gb.getDead();
    	Hashtable<EntrapmentCell,EntrapmentCell> hittable = gb.getDests();
    	numberMenu.clearSequenceNumbers();
     	if(hittable==null) { hittable = gb.getSources(); }
     	boolean isBarrier = (moving!=null) && moving.isBarrier();
     	boolean canHitBarrier = (moving==null) || isBarrier;
     	boolean canHitCell = (moving==null) || !isBarrier;
        EntrapmentCell hitCell = null;
       	Enumeration<EntrapmentCell>cells = gb.getIterator(Itype.TBRL);
       	while(cells.hasMoreElements())
       	{
       		EntrapmentCell cell = cells.nextElement();

        		
            	if(gb.reverseY()){
                    int ypos = G.Bottom(brect) - gb.cellToY(cell);
                    int xpos = G.Left(brect) + gb.cellToX(cell);
                    numberMenu.saveSequenceNumber(cell,xpos,ypos);
                   	double scl = pow(((ypos-G.Top(brect))/(double)G.Height(brect)),0.1);
                	int ss = (int)(SQUARESIZE*scl);
                	boolean isDest =  (hittable!=null) && (hittable.get(cell)!=null);
                	boolean canHit = canHitCell && gb.LegalToHitBoard(cell) && ((hittable==null) || isDest);
                	//StockArt.SmallO.drawChip(gc, this,CELLSIZE, xpos, ypos, null);
                	if( cell.drawStack(gc,this,canHit?highlight:null,ss,xpos,ypos,0,0.1,null)) 
                    	{ hitCell = cell; 
                    	  StockArt.SmallO.drawChip(gc,this,ss,xpos,ypos,null);
                    	}
                    if (dead!=null)
                    	{ EntrapmentChip ch = dead.get(cell);
                    		if(ch!=null)
                    			{ StockArt.SmallX.drawChip(gc,this,SQUARESIZE,xpos,ypos,null); 
                    			}
                    	}
                    if (isDest) 
                    	{ //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null); 
                    	}
                	}
 
        	{
           	EntrapmentCell vbar = gb.getVBarrier(cell);
            if(vbar!=null)
            	{
            	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            	int xpos = G.Left(brect) + (gb.cellToX(cell)+gb.cellToX((char)(cell.col+1),cell.row))/2;
               	int ss = yScaleSize(ypos,brect,SQUARESIZE);
            	boolean isDest =  (hittable!=null) && (hittable.get(vbar)!=null);
               	boolean canHit = canHitBarrier && gb.LegalToHitBoard(vbar) && ((hittable==null) || isDest);
                numberMenu.saveSequenceNumber(vbar,xpos,ypos);
                if( vbar.drawStack(gc,this,canHit?highlight:null,ss,xpos,ypos,0,0.1,null)) 
            		{ hitCell = vbar; 
            		  gb.switchPickedObject(hitCell);
            		  //StockArt.SmallO.drawChip(gc,this,ss,xpos,ypos,null);
            		}
            	if(isDest) 
            	{ //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,"v"+thisCol+thisRow); 
            	}
            		
            	}
           	}
         	{
        	EntrapmentCell hbar = gb.getHBarrier(cell);
            if(hbar!=null)
            	{
            	int ypos = G.Bottom(brect) - (gb.cellToY(cell)+gb.cellToY(cell.col,cell.row+1))/2;
            	int xpos = G.Left(brect) + gb.cellToX(cell);
            	int ss = yScaleSize(ypos,brect,SQUARESIZE);
            	boolean isDest =  (hittable!=null) && (hittable.get(hbar)!=null);
             	boolean canHit = canHitBarrier && gb.LegalToHitBoard(hbar) && ((hittable==null) || isDest);
                numberMenu.saveSequenceNumber(hbar,xpos,ypos);
            	if( hbar.drawStack(gc,this,canHit?highlight:null,ss,xpos,ypos,0,0.1,null)) 
            		{ hitCell = hbar; 
            		  gb.switchPickedObject(hitCell);
            		  //StockArt.SmallO.drawChip(gc,this,ss,xpos,ypos,""+thisCol+thisRow);
            		}
            	if(isDest) 
            	{ //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,"h"+thisCol+thisRow); 
            	}
            	}
        	}
        	if( !gb.reverseY()){
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            numberMenu.saveSequenceNumber(cell,xpos,ypos);
          // StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
           	double scl = pow(((ypos-G.Top(brect))/(double)G.Height(brect)),0.1);
        	int ss = (int)(SQUARESIZE*scl);
        	boolean isDest =  (hittable!=null) && (hittable.get(cell)!=null);
        	boolean canHit = canHitCell && gb.LegalToHitBoard(cell) && ((hittable==null) || isDest);
        	if( cell.drawStack(gc,this,canHit?highlight:null,ss,xpos,ypos,0,0.1,null)) 
            	{ hitCell = cell; 
            	  //StockArt.SmallO.drawChip(gc,this,ss,xpos,ypos,null);
            	}
            if (dead!=null)
        	{ EntrapmentChip ch = dead.get(cell);
        		if(ch!=null)
        			{ StockArt.SmallX.drawChip(gc,this,SQUARESIZE,xpos,ypos,null); 
        			}
        	}
            if (isDest) 
            	{ //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null); 
            	}
        	}
        	}
    	
  
        if(hitCell!=null)
        {	// draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
      		highlight.arrow =hasMovingObject(highlight) 
      			? StockArt.DownArrow 
      			: hitCell.height()>0?StockArt.UpArrow:null;
      		highlight.awidth = SQUARESIZE/3;
      		highlight.spriteColor = Color.red;
        }
        numberMenu.drawSequenceNumbers(gc,SQUARESIZE*2/3,labelFont,labelColor); 
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
    {  EntrapmentBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      EntrapmentState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        GC.setFont(gc,standardBoldFont());
        
        boolean planned = plannedSeating();
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
        {
        	commonPlayer pl = getPlayerOrTemp(i);
        	pl.setRotatedContext(gc, highlight,false);
        	if(planned && (i==gb.whoseTurn))
        	{
        		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
    					HighlightColor, rackBackGroundColor);
        	}
            DrawBarrierPool(gc, i,barrierRects[i], gb.whoseTurn,ot);
            DrawRoamerPool(gc, i,roamerRects[i], gb.whoseTurn,ot);
            Rectangle cr = chipRects[i];
        	EntrapmentChip.getRoamer(gb.getColorMap()[i]).drawChip(gc,this,cr,null,1);
        	pl.setRotatedContext(gc, highlight,true);
        }
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
        double messageRotation = pl.messageRotation();
		if (vstate != EntrapmentState.PUZZLE_STATE)
        {	
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==EntrapmentState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);


 		standardGameMessage(gc,messageRotation,
            		vstate==EntrapmentState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
            				vstate!=EntrapmentState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
 		EntrapmentChip.getRoamer(gb.getColorMap()[gb.whoseTurn]).drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);
         
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        drawAuxControls(gc,ourSelect);
        drawVcrGroup(ourSelect, gc);
        drawViewsetMarker(gc,viewsetRect,highlight);

    }
    public int getLastPlacement()
    {
    	return b.dropState;
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
        numberMenu.recordSequenceNumber(b.activeMoveNumber());
        startBoardAnimations(replay,b.animationStack,b.cellSize(),MovementStyle.Simultaneous);
        if(replay.animate) { playSounds(mm); }
 
        return (true);
    }
     
     // always undo all the way back to "done",  this is slightly nonstandard,
     // but necessary for the replay of a few old games, including ET-SmartBot-smatt-2011-05-20-1833.sgf
     public void performUndo()
     {	if(b.revision<101)
     	{
     	if(allowBackwardStep()) { doUndoStep(); }
     	if(allowUndo()) { doUndo(); }
     	}
     	else
     	{
    	super.performUndo() ;
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
        return (new EntrapmentMovespec(st, player));
    }
    

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
    // public void verifyGameRecord()
    // {	super.verifyGameRecord();
    // }

    
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
    case MOVE_DROP:
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
        if (hp.hitCode instanceof EntrapmentId) // not dragging anything yet, so maybe start
        {

        EntrapmentId hitObject = (EntrapmentId)hp.hitCode;
		EntrapmentCell cell = hitCell(hp);
		EntrapmentChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case White_Barriers:
	    case Black_Barriers:
	    case White_Roamers:
	    case Black_Roamers:
	    case DeadWhiteRoamers:
	    case DeadBlackRoamers:
	    	PerformAndTransmit("Pick "+hitObject.shortName);
	    	break;
	    case HBarriers:
	    case VBarriers:
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(cell.chipIndex>=0)
	    		{
	    		PerformAndTransmit("Pickb "+hitObject.shortName+" "+cell.col+" "+cell.row);
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
    {	CellId id = hp.hitCode;
        if(!(id instanceof EntrapmentId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	EntrapmentId hitObject = (EntrapmentId)hp.hitCode;
		EntrapmentCell cell = hitCell(hp);
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", id);
        case White_Roamers:
        case DeadWhiteRoamers:
        case DeadBlackRoamers:
        case Black_Roamers:
        case White_Barriers:
        case Black_Barriers:
        	if(b.movingObjectIndex()>=0)
        		{
        		PerformAndTransmit("Drop "+hitObject.shortName);
        		}
        		break;
        case ReverseViewButton:
       	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
       	 generalRefresh();
       	 break;

        case HBarriers:
        case VBarriers:
        case BoardLocation:	// we hit the board 
        	if(b.movingObjectIndex()>=0)
				{ if(cell!=null) 
					{ PerformAndTransmit("Dropb "+hitObject.shortName+" "+cell.col+" "+cell.row); 
					}
				}
			break;

          }}
    }

    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parsable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() 
    { 	// this arrangement with a leading 0 is unique to entrapment
    	// which doesn't need a randomkey
    	return(G.concat(b.gametype," 0",b.revision)); 
    }
    public String sgfGameType() { return(Entrapment_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link #gameType}
     */
    public void performHistoryInitialization(Tokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	String next = his.nextToken();
    	int rev = 100;
    	long rk = 0;
    	if(next.charAt(0)=='0') { rev = Math.max(100,Math.min(EntrapmentBoard.REVISION+10,G.IntToken(next))); }
    	else { rk = G.LongToken(next); }
    	// make the random key part of the standard initialization,
    	// even though games like checkers probably don't use it.
        b.doInit(token,rk,rev);
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
    


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new EntrapmentPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/25/2023
     * 	6210 files visited 0 problems
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
            	String n = st.nextToken();
            	int rev = 100;
            	if(n.charAt(0)=='0') { rev = Math.max(100,Math.min(EntrapmentBoard.REVISION+10,G.IntToken(n))); }
            	b.doInit(typ,0,rev);
            }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (parseVersionCommand(name,value,2)) {}
            else if (parsePlayerCommand(name,value))
            	{
            	if(value.startsWith("time"))
            	{	// this is a kludge to fix a large class of damaged games,
            		// where the robot won the game without an explicit "done"
                		if(b.DoneState())
                		{	PerformAndTransmit("done", false,replayMode.Replay);
                 		}

            	}
            	
            	}
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

