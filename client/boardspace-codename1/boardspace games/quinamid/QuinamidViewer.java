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
package quinamid;

import bridge.*;
import common.GameInfo;

import com.codename1.ui.geom.Rectangle;

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
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.SoundManager;
import lib.StockArt;
import lib.TextButton;


/**
 * 
 * Change History
 *
 * May 2007 initial work in progress. 
 *
*/
public class QuinamidViewer extends CCanvas<QuinamidCell,QuinamidBoard> implements QuinamidConstants
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private static String rotates[] = {
    		"/quinamid/images/rotate-1" + Config.SoundFormat,
    		"/quinamid/images/rotate-2" + Config.SoundFormat,    				
    		"/quinamid/images/rotate-3" + Config.SoundFormat,
    		"/quinamid/images/rotate-4" + Config.SoundFormat    			
    };
    private static String shifts[] = {
    		"/quinamid/images/scrape-1" + Config.SoundFormat,
    		"/quinamid/images/scrape-2" + Config.SoundFormat,    				
    		"/quinamid/images/scrape-3" + Config.SoundFormat,
    		"/quinamid/images/scrape-4" + Config.SoundFormat  			
    };
    // images
    private static Image[] textures = null;// background textures
    // private undoInfo
    private QuinamidBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //public Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //public Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle chatRect = addRect("chatRect"); // the chat window
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
   
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle repRect = addRect("repRect");
	private TextButton swapButton = addButton(SWAP,GameId.HitSwapButton,SwapDescription,
			HighlightColor, rackBackGroundColor);
    private Rectangle helpRect = addRect("helpRect");
    private boolean showingHelp = false;

    public synchronized void preloadImages()
    {	
       	QuinamidChip.preloadImages(loader,ImageDir);
        if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        SoundManager.preloadSounds(rotates);
        SoundManager.preloadSounds(shifts);
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
    	enableAutoDone = true;
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        Random r = new Random(randomKey);
        String key = "A1";
        char col='A';
        int row = 1;
        for(int i=1;i<NLEVELS;i++) 
        {	col += r.nextInt()&1;
        	row += r.nextInt()&1;
        	key += col+""+row;
        }
        b = new QuinamidBoard(info.getString(GameInfo.GAMETYPE, Quinamid_INIT),
        		key,getStartingColorMap());
        useDirectDrawing(true);
        doInit(false);

        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.initialPosition);						// initialize the board
        new_board = true;
       if(!preserve_history)
    	{ 
    	   startFirstPlayer();
    	}
    }

    public void resetBounds() { new_board = true; super.resetBounds(); }


    /**
     * calculate a metric for one of three layouts, "normal" "wide" or "tall",
     * which should normally correspond to the area devoted to the actual board.
     * these don't have to be different, but devices with very rectangular
     * aspect ratios make "wide" and "tall" important.  
     * @param width
     * @param height
     * @param wideMode
     * @param tallMode
     * @return a metric corresponding to board size
     */
     
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int chipW = unitsize*4;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,2*unitsize/3);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW,chipW);
    	G.SetRect(done, x+chipW+unitsize, G.Bottom(box)+unitsize, unitsize*4,plannedSeating()?unitsize*2:0);
    	pl.displayRotation = rotation;
    	G.union(box, done,chip);
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
    	int minLogW = fh*15;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        int nrows = DEFAULT_COLUMNS;
        int ncols = DEFAULT_COLUMNS;
        
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// 1.5:1 aspect ratio for the board
    			fh*3,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeRectangle(helpRect, buttonW,buttonW,buttonW*2,buttonW*2,BoxAlignment.Bottom,true);
        

    	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
    	layout.placeRectangle(swapButton,buttonW*2,buttonW/2,buttonW*3,buttonW,BoxAlignment.Center,true);
        
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
        G.placeStateRow(stateX,stateY,boardW,stateH,iconRect,stateRect,annotationMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH*2,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,rackBackGroundColor);
   
    }
 

	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, int player, Rectangle r, HitPoint highlight)
    {	QuinamidCell chips[]= b.rack;
        boolean canHit = b.LegalToHitChips(player);
        QuinamidCell thisCell = chips[b.playerColors[player]];
        QuinamidChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        Random rv = new Random(1234+player);
        int size = G.Width(r)/2;
        int xspan = Math.max(2, G.Width(r)-size);
        int yspan = Math.max(2, G.Height(r)-size);
        for(int i=0,lim=(36 - b.chips_on_board[player] - ((b.movingObjectIndex()==player)?1:0)) ; i<lim; i++)
        {
        int rx = size/2+rv.nextInt(xspan);
        int ry = size/2+rv.nextInt(yspan);
        thisCell.drawStack(gc,pt,G.Left(r)+rx,G.Top(r)+ry,this,0,size,0,null);
        }
        if((highlight!=null) && (highlight.hitObject==thisCell))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = size/2;
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
    	QuinamidChip ch = QuinamidChip.getChip(obj);// Tiles have zero offset
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
    { boolean review = reviewMode() && !mutable_game_record;
      // erase
      QuinamidBoard gb = disB(gc);
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
	    gb.SetDisplayParameters(MASTER_SCALE,1.0,  0.16,0.045,  0);
	    gb.SetDisplayRectangle(boardRect);
      
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);

      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private long animStart = 0;
    private int prev_board_x[] = new int[NLEVELS];
    private int prev_board_y[] = new int[NLEVELS];
    private int prev_board_rot[]= new int[NLEVELS];
    private double animx[] = new double[NLEVELS];
    private double animy[] = new double[NLEVELS];
    private boolean new_board = true;
    private Image rotationImage = null;
    private void drawBoardElements(Graphics gc, QuinamidBoard gb, Rectangle brect, HitPoint highlight)
    {	Rectangle []edgeBoards = new Rectangle[NLEVELS];
    	Rectangle []edgeCenters = new Rectangle[NLEVELS];
  		QuinamidCell hitCell = null;
    	Graphics activeGC = gc;
 		boolean rotating = false;
 		Graphics rotationGC = null;
    	long now = G.Date();
    	double animAmount = 0.0;
    	boolean rotationActive = false;
    	boolean end_anim = false;
    	if(animStart>0)
    	{	animAmount = 1.0-(now-animStart)/600.0;
    		if(animAmount<0) 
    			{ animStart = 0; animAmount = 0; new_board=true; end_anim=true;}
    			else { repaint(100); }
    	}
    	int flaggedBoard = -1;
    	QuinamidChip picked = b.pickedObject;
     	{
     	int xo = 0;
     	int yo = 0;
     	double  ss = SQUARESIZE*MASTER_SCALE;
     	for(int level = 0; level<b.stackedBoards.length;level++)
     	{	QuinamidBoard.EdgeBoard lboard = b.stackedBoards[level];
     		double xa[] = {1.0, 0.89, 0.87, 0.855, 0.855};
     		double ya[] = {1.0, 0.90, 0.865, 0.855, 0.861};	// ad hox x adjust so the rect is visually snug on the board
     		double snugx[] = {0.41,0.40,0.38,0.33,0.26};	// ad hoc y adjust so the rect is visially snug on the board
     		double snugy[] = {0.45,0.46,0.47,0.50,0.56};
       		xo += lboard.col_offset;
     		yo += lboard.row_offset;
     		if(new_board || end_anim) 
     		{
     			prev_board_x[level]=xo;
     			prev_board_y[level]=yo;
     			prev_board_rot[level]=lboard.rot;
     		}
    		if((animStart==0) 
     				&& ((prev_board_x[level]!=xo) 
     						|| (prev_board_y[level]!=yo) 
     						|| prev_board_rot[level]!=lboard.rot)) 
     			{ animStart = now; 
     			  rotationImage = null;
      			}
     		rotating |= (prev_board_rot[level]!=lboard.rot);// note the rotation
     		//
     		// the board has already been configured for the new position.  We draw a retro version of it
     		// and interpolate toward the final state.
     		// if we are rotating, allocate a temporary bitmap and draw the new board into it, then rotate the drawn board backwards
     		// by the desired amount toward the old position.  If we are shifting, interpolate the x or y positions of the edge boards
     		// back toward their previous position.
     		//
     		// animx and y offset the boards and all the stones drawn on them
     		animx[level] = (rotating || (animStart==0)) ? 0 : ((prev_board_x[level]-xo)*animAmount)*ss*xa[level];
     		animy[level] = (rotating || (animStart==0)) ? 0 : -((prev_board_y[level]-yo)*animAmount)*ss*ya[level];
     		int bsize = (int)(ss*lboard.size);
     		int left = (int)(G.Left(brect)+bsize/2 +     xo*ss*xa[level] + animx[level]);
     		int top = (int)(G.Top(brect)+6*ss-bsize/2 - yo*ss*ya[level] + animy[level]);
     		edgeCenters[level] = new Rectangle(left,top,bsize,bsize);
     		edgeBoards[level] = new Rectangle((int)(left-bsize*snugx[level]),(int)(top-bsize*snugy[level]),(int)(bsize*0.87),(int)(bsize*0.85));
     	}}
     	new_board = false;

     	if(picked==null)
         {	// process sensitive areas on the board when there is no other activity
         	for(int rlevel = NLEVELS-1; rlevel>=0; rlevel--)
         	{	// find the uppermost level above zero that containes the mouse
          		Rectangle r = edgeBoards[rlevel];
         		if(G.pointInRect(highlight,r))
         		{
        			MovementZone zone = movementZone(highlight,edgeBoards[rlevel],rlevel);
        			int dx = 0;
        			int dy = 0;
        			switch (zone)
        			{
            		case Rotate_UpRight:	// rotate upper-right corner
     				dx = G.Top(highlight) -G.Top(r);
            			dy = G.Right(r)-G.Left(highlight);
            			break;
            		case Rotate_UpLeft:
            			dy = G.Top(highlight)-G.Top(r);
            			dx = G.Left(highlight)-G.Left(r);
            			break;
            		case Rotate_DownLeft:
            			dx = G.Bottom(r)-G.Top(highlight);
            			dy = G.Left(highlight)-G.Left(r);
            			break;
            		case Rotate_DownRight:
            			dy = G.Bottom(r)-G.Top(highlight);
            			dx = G.Right(r)-G.Left(highlight);
            			break;
            		default: break;
         			};
        			
         		switch (zone)
         		{
         		case Move_Right:	// move right
          		case Move_Up: 		// move up
         		case Move_Left: 	// move left;
         		case Move_Down:		// move down
         			{
         			if(rlevel==(NLEVELS-1)) break;		// topmost can't move the one above it.
         			int mlevel = rlevel+1;
         			QuinamidBoard.EdgeBoard movee = b.stackedBoards[mlevel];
         			if(movee.canShiftWithArrow(zone))
         			{
         			highlight.arrow = zone.arrow;
         			highlight.hitCode = zone.opcode;
         			highlight.awidth = SQUARESIZE;
         			highlight.spriteRect = edgeBoards[mlevel];
         			highlight.spriteColor = Color.red;
         			highlight.col = (char)('A'+mlevel);
         			highlight.distanceToPoint = SQUARESIZE;
         			flaggedBoard = mlevel;
         			}}
         			break;	
         		case Rotate_UpRight:	// rotate upper-right corner
            	case Rotate_UpLeft: // rotate upped-left corner
         		case Rotate_DownLeft: // rotate lower-left cornet
         		case Rotate_DownRight: // rotate lower-right corner
         			{
         			QIds direction = (dx>dy)? zone.opcode : zone.reverseOpcode;
         			QuinamidBoard.EdgeBoard rotatee = b.stackedBoards[rlevel];
         			if(rotatee.canRotateWithArrow(direction))
     				{
     				highlight.arrow = (dx>dy) ? zone.arrow : zone.reverseArrow;
     				highlight.hitCode = direction;
     				highlight.awidth = SQUARESIZE;
     				highlight.col = (char)('A'+rlevel);   
     				highlight.spriteRect = r;
     				highlight.spriteColor = Color.red;
     				highlight.distanceToPoint = SQUARESIZE;
     				flaggedBoard = rlevel;
     				}}
         			break;	

         		default: break;
         		}
         			
         		break;	
         		}
         	}
         } 
     	int rotation_x_offset = 0;
 		int rotation_y_offset = 0;
 		int rotation_center_x = 0;
 		int rotation_center_y = 0;
 		int rotateDirection = 0;
 		int rwidth = 0;
     	//
        // now draw the contents of the board and anything it is pointing at
        //

     	for(int level = 0; level<b.stackedBoards.length;level++)
     	{	
     		{
     		Rectangle center = edgeCenters[level];
     		
     		if((animStart!=0) && !rotationActive)
 			{	int rotdir = (prev_board_rot[level]-b.stackedBoards[level].rot+4)%4;// start the rotation
 				if(rotdir!=0)	// is this the level where we start the rotation?
 				{
 				rotationActive = true;
     			rwidth = (int)(G.Width(center)*1.42);
     			if(rotdir==3) { rotdir=-1; }
     			rotateDirection = -rotdir;
     			if(rotationImage==null) 
     				{ rotationImage = Image.createTransparentImage(rwidth,rwidth);
     				}
     			else { rotationImage = Image.clearTransparentImage(rotationImage); }
    			rotationGC = rotationImage.getGraphics();
 			
    			// Make all filled pixels transparent
       			//rotationGC.setColor(new Color(0xffffff,true));
     			//rotationGC.fillRect(0,0,rwidth,rwidth);
       			activeGC = rotationGC;
       			// correct for the x,y,scale associated with the board images, so
       			// the board image is drawn visually centered in rotationimage
       			QuinamidChip bpic = QuinamidChip.getBoard(level);
       			int xoff = bpic.local_x_offset(G.Width(center));
       			int yoff = bpic.local_y_offset(G.Height(center));
       			
     			rotation_x_offset = G.Left(center)-rwidth/2-xoff;
     			rotation_y_offset = G.Top(center)-rwidth/2-yoff;
     			rotation_center_x = G.Left(center)-xoff;
     			rotation_center_y = G.Top(center)-yoff;
 				}
 			}
          	QuinamidChip.getBoard(level).drawChip(activeGC,this,G.Width(center),
      						G.Left(center)-rotation_x_offset,
      						G.Top(center)-rotation_y_offset,
      						null);
      		}
      		if((flaggedBoard>=0) && (flaggedBoard<=level) && !rotationActive)
      		{	Rectangle snug = edgeBoards[level];
      			QuinamidChip.getLandingPad().drawChip(activeGC,this,G.Width(snug),G.centerX(snug),
      					G.centerY(snug),null);
      			//G.frameRect(gc,Color.red,snug);
      		}
      		//G.frameRect(gc,Color.red,edgeBoards[level]);
      		for(QuinamidCell c = b.allCells; c!=null; c=c.next)
     		{	QuinamidCell up = c.upLink;
     			HitPoint canhit = b.LegalToHitBoard(c) ? highlight : null;
     			if((up!=null) && ((up.col-'a')==level))
     			{
     	            int ypos = G.Bottom(brect) - gb.cellToY(c.col, c.row);
     	            int xpos =G.Left( brect) + gb.cellToX(c.col, c.row);
     	            int ax = xpos+(int)(animx[level]-rotation_x_offset);
     	            int ay = ypos+(int)(animy[level]-rotation_y_offset);
     	            if( c.drawStack(activeGC,canhit,
     	            			ax,
     	            			ay,
     	            			this,0,SQUARESIZE,0.1,null)) 
     	            	{ 
     	            	hitCell = c;
      	            	}
     			}
     		}
     	}
     	if(hitCell!=null)
     	{
     	            	highlight.arrow =(picked!=null)
     	      				? StockArt.DownArrow 
   				: hitCell.chip!=null?StockArt.UpArrow:null;
     	      			highlight.awidth = SQUARESIZE/2;
     	      			highlight.hitCode = QIds.BoardLocation;
   			highlight.spriteRect = null;
   			highlight.spriteColor = Color.red;
     		}
        
        if(rotationActive)
        {	if(gc!=null)
        	{
        	Image r2 = rotationImage.rotate(rotateDirection*Math.PI/2*animAmount,0x0);
        	int xp = rotation_center_x-rwidth/2;
        	int yp = rotation_center_y-rwidth/2;
        r2.drawImage(gc,xp,yp);
        	}
        	repaint(20);
       }
       
    }
    public MovementZone movementZone(HitPoint highlight,Rectangle r,int level)
    {  	int leftCell = G.Width(r)/(6-level);	// top board is still divided into 3
    	int rightCell = G.Width(r)-leftCell; 
    	int xp = G.Left(highlight)-G.Left(r);
    	int yp = G.Top(highlight)-G.Top(r);
    	// we know the board is square 
    	if(yp<leftCell)
    	{	// top part
    		if(xp<leftCell) {return(MovementZone.Rotate_UpLeft);}
    		if(xp>rightCell) { return(MovementZone.Rotate_UpRight); }
    		return(MovementZone.Move_Up);
    	}
    	else if(yp>rightCell)	//bottom part
    	{	if(xp<leftCell) {return(MovementZone.Rotate_DownLeft);}
    		if(xp>rightCell) { return(MovementZone.Rotate_DownRight); }
    		return(MovementZone.Move_Down);
    	}
    	// middle stripe
    	else if(xp<leftCell)
    	{	//left part
    		return(MovementZone.Move_Left);
    	}
    	else if(xp>rightCell)
    	{	// right part
    		return(MovementZone.Move_Right);
    	}
    	else 
     	return(MovementZone.Move_None);	// none
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    { 	QuinamidChip.HelpPanel.drawChip(gc, this, showingHelp ? boardRect : helpRect,highlight,QIds.ShowHelp);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  QuinamidBoard gb = disB(gc);
      int whoseTurn = gb.whoseTurn;
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = showingHelp ? null : ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      QuinamidState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        boolean planned = plannedSeating();
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
          {	commonPlayer pl = getPlayerOrTemp(i);
          	pl.setRotatedContext(gc, highlight,false);
          	DrawCommonChipPool(gc, i,chipRects[i],ot);
              if(planned && (i==whoseTurn))
              {
              	handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
      					HighlightColor, rackBackGroundColor);	
              }
              pl.setRotatedContext(gc, highlight,true);
          }
        
        commonPlayer pl = getPlayerOrTemp(whoseTurn);
        double messageRotation = pl.messageRotation();
        boolean conf = (vstate==QuinamidState.CONFIRM_STATE && gb.swapPending);
        if(conf
        	|| ((vstate==QuinamidState.PLAY_OR_SWAP_STATE) && !moving))
        {
        swapButton.highlightWhenIsOn = true;
    	swapButton.setIsOn(conf);
    	swapButton.show(gc, messageRotation,highlight);       	
        }
        GC.setFont(gc,standardBoldFont());
		if (vstate != QuinamidState.PUZZLE_STATE)
        {
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
                }

 		drawPlayerStuff(gc,(vstate==QuinamidState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);

        standardGameMessage(gc,messageRotation,
        		vstate==QuinamidState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=QuinamidState.PUZZLE_STATE,
        				whoseTurn,
        				stateRect);
        gb.playerChip[whoseTurn].drawChip(gc, this, iconRect,null);
        
        goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);
     
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
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Simultaneous);
        if(replay.animate) { playSounds(mm); }
 
        return (true);
    }
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current undoInfo of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new QuinamidMovespec(st, player));
    }
    



    
private void playSounds(commonMove mm)
{
	QuinamidMovespec m = (QuinamidMovespec) mm;

    // add the sound effects
    switch(m.op)
    {
    case MOVE_RACK_BOARD:
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
    case MOVE_SHIFT:
    	 playASoundClip(shifts[m.to_row-1],100);
    	 break;
    case MOVE_ROTATE:
    	playASoundClip(rotates[m.to_row-1],100);
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
        if (hp.hitCode instanceof QIds)// not dragging anything yet, so maybe start
        {
        
       	QIds hitObject = (QIds)hp.hitCode;
		QuinamidCell cell = hitCell(hp);
		QuinamidChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case Red_Chip_Pool:
	    case Blue_Chip_Pool:
	    	PerformAndTransmit("Pick "+hitObject.shortName);
	    	break;
	    case BoardLocation:
	    	if(cell.chip!=null)
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
        if(!(id instanceof QIds)) {  missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
    	missedOneClick = false;
    	QIds hitObject = (QIds)hp.hitCode;
		QuinamidCell cell = hitCell(hp);
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case ShowHelp:
        	showingHelp = !showingHelp;
        	break;
        case MoveUp:
        case MoveDown:
        case MoveLeft:
        case MoveRight:
        	PerformAndTransmit("Shift "+hp.col+" "+hitObject.shortName);
        	break;
        case RotateCW:
        case RotateCCW:
        	PerformAndTransmit("Rotate "+hp.col+" "+hitObject.shortName);
        	break;

         case BoardLocation:	// we hit the board 
			if(cell!=null)
				{
				if (b.movingObjectIndex()>=0)
				{ PerformAndTransmit("Dropb "+cell.col+" "+cell.row); 
				}
				else if(cell.chip!=null)
				{
					PerformAndTransmit( "Pickb "+cell.col+" "+cell.row);
				}}
			break;
			
        case Red_Chip_Pool:
        case Blue_Chip_Pool:
         	PerformAndTransmit("Drop "+hitObject.shortName);
            break;
        }
         }
    }

    private static final double MASTER_SCALE=1.0;
    
    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parsable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() 
    { 
    	return(""+b.gametype+" "+b.initialPosition); 
   }
    public String sgfGameType() { return(Quinamid_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	String rk = his.nextToken();
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

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new QuinamidPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 1949 files visited 0 problems
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
            	String ran = st.nextToken();
                b.doInit(typ,ran);
                resetBounds();
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

