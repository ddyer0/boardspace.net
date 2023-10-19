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
package container;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Point;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import vnc.VNCService;

import java.util.*;

import lib.*;
import lib.SimpleSprite.Movement;

/** 
 * hidden container windows, show the private information for players.
 * and intermediate the bidding process.
 * 
 * We share the run loop with the main game.  The virtual windows are 
 * positioned to the left of the real window, so mouse activity on the
 * real screen can't affect them.
 * 
 * @author Ddyer
 *
 */

/**
 * 
 * Change History
 *
 * April 2010 added "reverse y" option
 *
*/
public class ContainerViewer extends CCanvas<ContainerCell,ContainerBoard> implements ContainerConstants, PlayConstants
{
    static final String Container_SGF = "Container"; // sgf game name
    static final String ImageDir = "/container/images/";
  
    // version 2 is after a serious state machine bug is fixed.  Special logic
    // applies so version 1 game records can still be read.
    static final int SGF_GAME_VERSION = 2;		
    // file names for jpeg images and masks
 	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int PANELMASK_INDEX = 2;
    static final int ISLAND_INDEX = 0;
    static final int P_PERSONAL_MAT_CW_INDEX = 1;
    static final int P_PERSONAL_MAT_CCW_INDEX = 2;
    static final int HAND_DOWN_INDEX = 3;
    static final int HAND_UP_INDEX = 4;
    static final int PERSONAL_MAT_CW_INDEX = 5;
    static final int PERSONAL_MAT_CCW_INDEX = 6;
	static final double INITIAL_ANIMATION_SPEED = 0.7;
  
    static final String ImageNames[] = {"p-island", "p-person-mat-cw","p-person-mat-ccw","hand-down","hand-up",
    		"person-mat-cw","person-mat-ccw"};
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "panelmask"
    	  };
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(200,200,230);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    
    // version 2 is after a serious state machine bug is fixed.  Version 1 game records can still be read
    public String sgfGameVersion() { return(""+SGF_GAME_VERSION); }
    
    public int ScoreForPlayer(commonPlayer p)
    {  	
    	return(b.currentScoreForPlayer(p.boardIndex));
    }
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// masked images
    // private state
    ContainerBoard b = null; 		// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    private static int SUBCELL = 2;	// number of cells in a square
    private int SQUARESIZE;			// size of a board square
    private int badClick = 0;
    
    
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle loanRect = addRect("loanRect");
    private Rectangle fundLoanRect = addRect("fundLoan");
    private Rectangle declineLoanRect = addRect("declineLoan");
    private Rectangle bidAmountRect = addRect(".bidAmount");
    private Rectangle bidCalculatorRect = addRect(".bidCalculator");
    private PopupManager bidPop = new PopupManager();
         
    private Rectangle playerMats[] = addRect(".playerMat",5);
    private Rectangle playerHands[] = addRect("playerHand",5);
    private Rectangle shipRects[] = addRect("playerShip",5);
    private Rectangle statRects[] = addRect(".playerStat",5); 
    
    private Rectangle islandRect = addRect(".islandRect");
    private Rectangle atSeaRect = addRect(".atSeaRect");
    private Rectangle containerStorageRect = addRect("containerStorageRect");
    private Rectangle machineStorageRect = addRect("machineStorageRect");
    private Rectangle warehouseStorageRect = addRect("warehouseStorageRect");
         
    private TextButton passButton = addButton(PASS,GameId.HitPassButton,ExplainPass,
			HighlightColor, rackBackGroundColor);
    private boolean menuIsUp = false;
    private Point lastMouseLocation = new Point(-1,-1);
    private String lastMouseZone = "on";
    private int selectedPerspective = -1;
    
    private boolean showDevelopInfo() { return(false); }
    

    /**
     * translate the mouse coordinate x,y into a size-independent representation
     * presumably based on the cell grid.  This is used to transmit our mouse
     * position to the other players and spectators, so it will be displayed
     * at approximately the same visual spot on their screen.  
     * 
     * Some trickier logic may be needed if the board has several orientations,
     * or if some mouse activity should be censored.
     */
    public String encodeScreenZone(int x, int y,Point p)
    {	if(menuIsUp) 
    		{ G.SetLeft(p,G.Left( lastMouseLocation));
    		  G.SetTop(p,G.Top(lastMouseLocation));
    		  return(lastMouseZone); 
    		}
    	lastMouseZone = super.encodeScreenZone(x,y, p);
    	G.SetLeft(lastMouseLocation, G.Left(p));
    	G.SetTop(lastMouseLocation, G.Top(p));
    	return(lastMouseZone);
    }
   
    public synchronized void preloadImages()
    {	
       	ContainerChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
    	}
        gameIcon = ContainerChip.getShip(0).image;
    }

    ContainerBoard.playerBoard getCurrentPlayerBoard(ContainerBoard gb)
    {	int ind = allowed_to_edit|isPassAndPlay ? b.whoseTurn : getActivePlayer().boardIndex;
    	return(gb.getPlayer(ind)); 
    }

    ContainerBoard.playerBoard getPlayerBoard(ContainerBoard gb,int n)
    {	return(gb.getPlayer(n));
    }
    Color ContainerMouseColors[] = 
    	{ new Color(150,150,100),
    	  new Color(105,135,135),
    	  Color.black,
    	  new Color(122,89,110),
    	  new Color(125,135,145)};
    Color ContaqinerMouseDotColors[] = { Color.white,Color.white,Color.white,Color.white,Color.white};
    public Slider speedRect = null;

    public void stopRobots()
    {
    	super.stopRobots();
    }
    int HiddenViewWidth = 600;
    int HiddenViewHeight = 450;
    int HiddenButtonWidth = 150;
    int HiddenButtonHeight = 60;
    private Rectangle hiddenViewRect = new Rectangle(0,0,HiddenViewWidth,HiddenViewHeight);;
    private Rectangle hiddenNameRect = new Rectangle(0,0,HiddenViewWidth,50);
    
    private Rectangle hiddenCashRect = new Rectangle(10,100,HiddenButtonWidth,HiddenButtonHeight);
    
    private Rectangle hiddenDeclineLoanRect = new Rectangle(HiddenViewWidth/4,HiddenViewHeight/4,(int)(HiddenViewWidth*0.7),HiddenViewHeight/5);
    private Rectangle hiddenFundLoanRect = new Rectangle(HiddenViewWidth/4,HiddenViewHeight/2,(int)(HiddenViewWidth*0.7),HiddenViewHeight/5);

    private Rectangle hiddenNetworthRect =  new Rectangle(10,200,HiddenButtonWidth,HiddenButtonHeight);
    private Rectangle hiddenGoalRect = new Rectangle(HiddenViewWidth/4,(int)(HiddenViewHeight*0.45),HiddenViewWidth/2,HiddenViewHeight/3);
    private Rectangle hiddenBidRect = new Rectangle(HiddenViewWidth-HiddenButtonWidth-10,100,HiddenButtonWidth,HiddenButtonHeight);
    private Rectangle hiddenCalculatorRect = new Rectangle(HiddenViewWidth/4,HiddenViewHeight/10,HiddenViewWidth/2,2*HiddenViewWidth/3);;
    private Rectangle hiddenDoneRect = new Rectangle(HiddenViewWidth-HiddenButtonWidth-10,HiddenViewHeight-HiddenButtonHeight-10,
			HiddenButtonWidth,HiddenButtonHeight);
    private Rectangle hiddenEyeRect = new Rectangle(10,10,
        		HiddenButtonHeight,HiddenButtonHeight);
    private void configureHiddenView()
    {
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	int players_in_game = Math.max(3,info.getInt(OnlineConstants.PLAYERS_IN_GAME,4));
    	int ran = info.getInt(OnlineConstants.RANDOMSEED,0);
        super.init(info,frame);
        MouseColors = ContainerMouseColors;
        MouseDotColors = ContaqinerMouseDotColors;
        speedRect = addSlider("speedRect",s.get(AnimationSpeed),ContainerId.Zoom_Slider);
        speedRect.min=0.0;
        speedRect.max=INITIAL_ANIMATION_SPEED*2;
        speedRect.value=INITIAL_ANIMATION_SPEED;
        speedRect.barColor=ZoomColor;
        speedRect.highlightColor = HighlightColor;
 
        b = new ContainerBoard(info.getString(GAMETYPE, Container_INIT),
        		players_in_game,ran,getStartingColorMap());
        if(G.debug()) {
        	ContainerConstants.putStrings();
        }
        //useDirectDrawing();	// not checked yet
        doInit(false);

        
     }
    
    public void adjustPlayers(int np)
    {
    	super.adjustPlayers(np);
        if(RpcService.isRpcServer() || VNCService.isVNCServer() || G.debug())
        {
        createHiddenWindows(np,HiddenViewWidth,HiddenViewHeight);
        }
    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey,(b.nextIntCompatibility?100:0)+b.nPlayers());	// initialize the board
        if(!preserve_history)
        	{
        	selectedPerspective = -1; 
        	speedRect.setValue(INITIAL_ANIMATION_SPEED);
        	startFirstPlayer();
        	}
    }


    public Rectangle createPlayerGroup(commonPlayer pl0,int fullwidth,int x,int y,int width,int height,
    			Rectangle matRect,Rectangle handRect,
    			Rectangle shipRect,
    			Rectangle statRect,
    			double scale,
    			boolean left,
    			int unitsize)
    {   
    	int pgsize = unitsize*5;
    	boolean perspective = usePerspective();
    	Rectangle box = pl0.playerBox;
    	Rectangle firstPlayerRect = pl0.nameRect;
        Rectangle p0anim = pl0.animRect;
        Rectangle p0time = pl0.timeRect;
        Rectangle p0aux = pl0.extraTimeRect;
        Rectangle p0pic = pl0.picRect;
        int playerHeight = unitsize*2;
        
        {
        int plex = left ? x : (x+width)-pgsize;
        G.SetRect(p0pic, plex,y+playerHeight+unitsize, unitsize*4, unitsize*4);
        G.SetRect(firstPlayerRect, plex,y,unitsize*4,unitsize*1);
        }
        int statWidth = unitsize*7;
        G.SetRect(statRect,
        			left ? Math.max(unitsize/8,G.Left(p0pic)-statWidth-unitsize/8)
        				 : Math.min(fullwidth-statWidth,G.Right(p0pic)),
        			G.Top(p0pic),
        			statWidth,
        			unitsize*5);
        
 
        // time display for first player
        int tleft = G.Left( firstPlayerRect);
        int ttop = G.Bottom(firstPlayerRect);
        G.SetRect(p0time,tleft,ttop, unitsize * 3, unitsize);
        G.SetRect(shipRect,left?G.Right(firstPlayerRect):G.Left(firstPlayerRect)-unitsize*4,y-unitsize/2,
        		unitsize*4, unitsize*2);
        G.AlignLeft(p0aux,ttop+unitsize,p0time);
     
        // first player "i'm alive" animation ball
        G.SetRect(p0anim,G.Right(p0time),G.Top(p0time),unitsize,G.Height( p0time));
        int matW = width-pgsize-(int)(2*scale*width);
        G.SetRect(matRect,
        		(left 
        			? x+pgsize-unitsize-(perspective?(int)(scale*width):0) 
        			: x+unitsize)+(int)(scale*width),
        		y+(perspective?(int)(scale*height):0),
        		matW,
        		(int)(matW*0.5+(perspective?0:0.9*scale*height)));
        
        G.SetRect(handRect,G.centerX(matRect)-4*unitsize-unitsize/2,
        		G.centerY( matRect),
        		unitsize*9, 3*unitsize/2);
        G.SetHeight(box, -1);
        G.union(box,matRect,statRect,p0pic);
        return(box);
        
     }


    public void setLocalBounds(int x, int y, int width, int height)
    {   
    	configureHiddenView();
        int chatHeight = selectChatHeight(height);
        int numplayers = b.nPlayers();
    	int sncols = DEFAULT_COLUMNS*SUBCELL; // more cells wide to allow for the aux displays
        int snrows = (DEFAULT_ROWS+2)*SUBCELL+2;  
        int cellw = width / sncols;
        int cellh = height / snrows;
        CELLSIZE = Math.max(1,Math.min(cellw, cellh)); //cell size appropriate for the aspect ration of the canvas
        SQUARESIZE = CELLSIZE*SUBCELL;

        boolean perspective = usePerspective();
        boolean righthandChat = ((chatHeight>0)&&(numplayers==5));
        int vdiv = numplayers>3 ? 3 : 2;
        G.SetRect(fullRect,0,0,width,height);
        int ideal_logwidth = 16 * CELLSIZE;
        int logH = CELLSIZE*8;
        // game log.  This is generally off to the right, and it's ok if it's not
        // completely visible in all configurations.
        double playerAspect = ((numplayers==3)?2.5:1.75);
        int C2 = CELLSIZE/2;
        int cx2 = CELLSIZE*2;
        int vcw = CELLSIZE * 8;
        int vch = vcw/2;
        int boardY = C2;//(wideMode ? 0 : logH)+CELLSIZE*2;
        int boardHeight = height-boardY-C2;
        int boardWidth = Math.min((int)(boardHeight*playerAspect),width);
        int boardX = (width-boardWidth)/2;
        G.SetRect(boardRect, 
        		boardX,
        		boardY,boardWidth, boardHeight);
         int boardBottom = boardY+boardHeight;
         int boardRight = boardX+boardWidth;
        int buttonW = CELLSIZE*3;
        int buttonH = buttonW/2;
        int playerWidth = (int)(0.4*boardWidth);
        int playerHeight = (int)((boardHeight*0.8-2*CELLSIZE)/vdiv);
        Rectangle lastbox = null;
        boolean left = false;
        int pstep = numplayers>3?3:2;
        double scale = 0.05;
        int nstep = pstep-1;
        int islandX = G.centerX( boardRect)-playerWidth/2;
        int islandWidth = playerWidth;
        int atSeaX = islandX+islandWidth/4;
       
        G.SetRect(warehouseStorageRect,G.Left(boardRect)+CELLSIZE,G.Top(boardRect)+CELLSIZE, 4*CELLSIZE, 5*CELLSIZE);
        G.SetRect(machineStorageRect,G.Left( boardRect)+CELLSIZE, G.Bottom(warehouseStorageRect) + C2,4*CELLSIZE, CELLSIZE*6+CELLSIZE/2);
        G.SetRect(containerStorageRect, G.Right(machineStorageRect)+CELLSIZE,G.Top( boardRect)+CELLSIZE,(CONTAINER_COLORS-2)*2*CELLSIZE, CELLSIZE*12);
         
        G.SetRect(loanRect, G.Right(containerStorageRect)+CELLSIZE, G.Bottom(containerStorageRect)-5*CELLSIZE, 2*CELLSIZE,4*CELLSIZE);
        
        G.SetRect(declineLoanRect, atSeaX,G.Bottom(loanRect), CELLSIZE*12, CELLSIZE+C2);
        G.SetRect(fundLoanRect,atSeaX,G.Bottom(declineLoanRect)+C2,G.Width( declineLoanRect),G.Height(declineLoanRect));

        int px = boardRight-playerWidth;
        int py = boardBottom-CELLSIZE-pstep*playerHeight;
        Rectangle pl0box = createPlayerGroup(getPlayerOrTemp(0),width,px,py,
        			playerWidth,playerHeight,
        			playerMats[0],playerHands[0],shipRects[0],statRects[0],(perspective?(pstep-1):nstep)*scale,left,
        			CELLSIZE);
        py += playerHeight; 
        pstep--;
        createPlayerGroup(getPlayerOrTemp(1),width,px,py,
        			playerWidth,playerHeight,
        			playerMats[1],playerHands[1],shipRects[1],statRects[1],(perspective?(pstep-1):nstep)*scale,left,
        			CELLSIZE);
        if(numplayers<=3) 
        	{ px = G.Left(boardRect)+CELLSIZE;
        	  pstep--;
        	  left = true;
         	}
        else { py += playerHeight; pstep=0; }
        
        Rectangle pl2box = createPlayerGroup(getPlayerOrTemp(2),width,px,py,
    			playerWidth,playerHeight,
    			playerMats[2],playerHands[2],shipRects[2],statRects[2],(perspective?pstep:nstep)*scale,left,
    			CELLSIZE);
        lastbox = pl2box;
        if(numplayers>3)
        {	left = true;
        	py = G.Bottom(boardRect)-CELLSIZE*2; 
        	px = G.Left(boardRect)+CELLSIZE; 
        	pstep = 0;
        }
        Rectangle pl3box = null;
        Rectangle pl4box = null;
        if(numplayers>=4)
        {
            py -= playerHeight;
            pl3box = createPlayerGroup(getPlayerOrTemp(3),width,px,py,
        			playerWidth,playerHeight,
        			playerMats[3],playerHands[3],shipRects[3],statRects[3],(perspective?pstep:nstep)*scale,left,
        			CELLSIZE);
            pstep++;
            lastbox = pl3box;
            if(numplayers>=5)
        	{
                py -= playerHeight;
                pl4box = createPlayerGroup(getPlayerOrTemp(4),width,px,py,
            			playerWidth,playerHeight,
            			playerMats[4],playerHands[4],shipRects[4],statRects[4],(perspective?pstep:nstep)*scale,left,
            			CELLSIZE);
                lastbox = pl4box;
     		
        	}
        	
        }
        G.SetRect(islandRect,islandX,G.Top( boardRect),	islandWidth,3*playerHeight/2-CELLSIZE);
         
        int bidX =  G.Right(islandRect)-CELLSIZE*12;
        int bidY = G.Bottom(islandRect)-CELLSIZE*2;
        G.SetRect(bidAmountRect,bidX, bidY, CELLSIZE*7,CELLSIZE+C2);
        G.SetRect(bidCalculatorRect,bidX+C2,G.Bottom(bidAmountRect),CELLSIZE*10,CELLSIZE*14);
        G.SetRect(atSeaRect, atSeaX, G.Bottom(islandRect), islandWidth/3,(CELLSIZE+C2)*numplayers);
        int rightX = G.Right(islandRect)+CELLSIZE*3+C2;
        if(righthandChat)
        {
        int chatX = rightX;
        int chatY = C2;
        int availableH = G.Top(pl0box)-C2;
        int logY = G.Bottom(passButton)+C2;
        int logX = G.Right(pl3box)+C2;
        int logW = G.Left(pl2box)-logX-C2;
        logH = G.Top(goalRect)-logY-C2;
        G.SetRect(logRect,logX,logY,logW,logH);
        G.SetRect(chatRect, chatX,chatY,width-chatX-C2,availableH-CELLSIZE);
        }
        else
        {
        int logX =  Math.max(rightX,width-ideal_logwidth-C2);
        int logW = width-logX-C2;
        int chatX = C2;      
        int chatY = Math.max(G.Bottom(machineStorageRect),G.Bottom(islandRect)-chatHeight-C2);
        G.SetRect(chatRect, 
        		chatX,
        		chatY,
        		atSeaX-C2-chatX, 
        		Math.min(chatHeight,G.Top(lastbox)-chatY-CELLSIZE/2));

  
        G.SetRect(logRect, 
        		logX,C2,
        		logW,
        		Math.min(logH,G.Top(pl0box)-CELLSIZE*2));
        }
        
        // "edit" rectangle, available in reviewers to switch to puzzle mode
        G.SetRect(editRect, atSeaX+CELLSIZE, G.Bottom(atSeaRect)+CELLSIZE, buttonW,buttonH);

        G.SetRect(goalRect, 
        		G.Left(boardRect)+G.Width(boardRect)/3,
        		G.Bottom(boardRect)-CELLSIZE-C2,
        		G.Width(boardRect)/3,CELLSIZE);
        
        setProgressRect(progressRect,goalRect);
 
        // "done" rectangle, should always be visible, but only active when a move is complete.
        G.AlignTop(doneRect, G.Right(editRect)+C2,editRect);

        // "done" rectangle, should always be visible, but only active when a move is complete.
        G.AlignLeft(passButton, G.Bottom(doneRect)+C2,doneRect);
        
        //this sets up the "vcr cluster" of forward and back controls.
         SetupVcrRects(boardX,boardBottom-vch-C2,vcw,vch);
                
        {
            int stateH = CELLSIZE+C2;
            int stateX = G.Left( boardRect) + C2;
            int stateY = 0;
            G.placeStateRow(stateX, stateY,G.Right(islandRect)-stateX,stateH,
            		iconRect,stateRect,noChatRect);
        }
        int vsy = boardBottom-cx2;
        G.SetRect(viewsetRect,boardRight-cx2,vsy,cx2,cx2);
        G.SetRect(speedRect, G.Left(viewsetRect)-vcw,vsy+CELLSIZE,vcw,CELLSIZE);
        positionTheChat(chatRect,Color.white,rackBackGroundColor);
        generalRefresh();
    }


    private boolean usePerspective()
    {
    	return(getAltChipset()==0);
    }
    private void DrawPlayerMat(Graphics gc, HitPoint highlight, int forPlayer, Rectangle r)
    {	int imageidx = G.Left(r)>G.centerX(boardRect) 
    			?  (usePerspective()?P_PERSONAL_MAT_CCW_INDEX:PERSONAL_MAT_CCW_INDEX)
    			: (usePerspective()?P_PERSONAL_MAT_CW_INDEX:PERSONAL_MAT_CW_INDEX);

    images[imageidx].centerImage(gc,  r  );
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
    	ContainerChip ch = ContainerChip.getChip(obj);// Tiles have zero offset
    	drawSprite(g,ch,xp,yp);
    }
    public void drawSprite(Graphics g,ContainerChip ch,int xp,int yp)
    {
    	int sz = SQUARESIZE;
    	
    	if(ch.isShip())
    	{
    		sz = G.Width(atSeaRect);
    		ContainerChip alt = ch;
    		boolean right = xp>G.Left(atSeaRect)+2*sz/3;
    		if(right) { alt = ch.getAltShip(); }
    		alt.drawChip(g,this,sz,xp,yp,null);
    		drawShipContents(g,b,ch,!right,false,null,null,null,sz,xp,yp);
    	}
    	else
    	{
    	ch.drawChip(g,this,sz,xp,yp,null);
    	}
     }

    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    public Point spriteDisplayPoint()
	{   return(new Point(G.Right(boardRect)-SQUARESIZE*2,G.Bottom(boardRect)-SQUARESIZE));
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
    {	boolean backgroundReview = reviewMode() && !mutable_game_record;
      // erase
      GC.setColor(gc,backgroundReview ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(backgroundReview)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     if(remoteViewer<0) { images[ISLAND_INDEX].centerImage(gc, islandRect); }

    }
    private ContainerBoard.ContainerGoalSet selectedGoalSet()
    {
    	if(showDevelopInfo() && (selectedPerspective>=0)) 
    		{ return(b.getPlayer(selectedPerspective).possibleGoalSets[0]); }
    	return(b.masterGoalSet);
    }
    
    private void drawLandingPad(Graphics gc,int w,double xscl,int x, int y)
    {
    	StockArt.LandingPad.drawChip(gc,this,w,xscl,x,y,null);
    	int xstep = (int)((w*xscl)/3);
    	int ystep = w/3;
    	GC.frameRect(gc, Color.red, x-xstep,y-ystep,xstep*2,ystep*2);
    }
  
    private void drawContainerStorage(Graphics gc,ContainerBoard gb,Rectangle r,HitPoint highlight,Hashtable<ContainerCell,ContainerCell> allowed,HitPoint any,boolean moving,ContainerCell store[],int across,boolean showValue)
    {	boolean showGoodValue = !showValue && (showDevelopInfo() || allowed_to_edit);
    	if(showGoodValue)
    	{
    	gb.setupIslandGoodsValues(gb.whoseTurn,selectedGoalSet());
    	}
       	// draw the container storage
    	//G.frameRect(gc,Color.white,r);
       	for(int i=0,
       			lim=gb.first_shipment?store.length-1:store.length,
       			wid = G.Width(r)/2,
       			awid = wid-wid/4,
       			ahgt = wid/Math.max(across-1,1),
       			ystep = (G.Height(r)-CELLSIZE)/3,
       			y = G.Top(r)+ystep,
       			x = G.Left(r);
       		i<lim;
       		i++)
       	{	ContainerCell c = store[i];
       		HitPoint alloww = isAllowed(allowed,highlight,c);
       		String msg = showValue ? "$ "+c.value: null;
       		String helpMsg = s.get(ContainerCountMessage,""+c.height(),s.get(c.getColor()));
     		if(showGoodValue)
       		{	String val = "$ "+gb.estimatedIslandGoodValue_v5(selectedGoalSet(),ContainerChip.getContainer(i),selectedPerspective);
       			GC.setFont(gc,largeBoldFont());
       			GC.Text(gc,false,x,y-2*ystep/3,awid,ahgt/4,Color.black,null,val);
       			helpMsg += "; "+s.get(SpotPrice,val);
       		}
       		GC.setFont(gc,standardPlainFont());
       		c.drawBrick(gc,alloww,true,x,y,this,awid,ahgt,across,false,2.0,1.0,msg);
       		HitPoint.setHelpText(any,x+awid/2,y-ystep/2,awid,ystep,helpMsg);
       		if(moving && isAllowed(allowed,any,c)!=null)
   			{	
       		  drawLandingPad(gc,awid,1.0,x+awid/2,y-ahgt);
   			}

         		if((i%2)!=0) { x=G.Left(r); y+=ystep; }
       		else { x += wid; }

       	}	
    }
    private Rectangle subRectangle(Rectangle master,double sr[])
    {	int ih = G.Height(master);
    	int iw = G.Width(master);
    	return(new Rectangle((int)(G.Left(master)+sr[0]*iw),(int)(G.Top(master)+ih*sr[1]),
					(int)(iw*sr[2]),(int)(ih*sr[3])));	
    }
    public void drawShipContents(Graphics gc,ContainerBoard gb,ContainerChip ship,boolean left,boolean moving,HitPoint highlight,Hashtable<ContainerCell,ContainerCell> allowed,HitPoint any,int cellw,int xpos,int ypos)
    {
       	// draw the warehouse storage
    	int wid = cellw/2;
 		ContainerBoard.playerBoard master = gb.getShipOwner(ship);
  		Object hitObj = (highlight!=null) ? highlight.hitObject : null;

  		if(hitObj!=null)
  		{	highlight.hitObject = null; 
  		}
  		HitPoint allows = isAllowed(allowed,highlight,master.shipGoods);
  		int awid = wid-wid/5;
  		int axpos = xpos-wid/3;
  		int bxpos = (int)(xpos+(left?-1:1)*wid*0.65);
       	boolean hit = master.shipGoods.drawBrick(gc,allows,b.getState()==ContainerState.PUZZLE_STATE,axpos,ypos,
       		this,awid,wid/3,MAX_SHIP_GOODS,!left,2.0,1.0,null);
       	if(master.current_ship_cost!=0) 
       		{ String msg = ""+master.current_ship_cost;
       		  if(showDevelopInfo())
       		  {	  
       		  	  //if(island<0) 
       		  	  master.current_ship_island_value = gb.valueAtAuction_v5(master.player,gb.game_stage_v5(),selectedGoalSet(),false); 
       		  	  msg += "/"+master.current_ship_island_value;
       		  }
       		  GC.setFont(gc, standardPlainFont());
       		  GC.Text(gc,false,bxpos,ypos-cellw/12,cellw/6,20,new Color(0.85f,0.85f,0.85f),null,msg);
       		}
       	if(moving && isAllowed(allowed,any,master.shipGoods)!=null)
       	{	drawLandingPad(gc,awid/2,3.0,axpos+awid/2,ypos);
       	}
       	if((hitObj!=null) && !hit)
       	{	highlight.hitObject = hitObj;
       	}
	
    }
    public void drawShipAndContents(Graphics gc,ContainerBoard gb,ContainerCell c,boolean moving,HitPoint highlight,Hashtable<ContainerCell,ContainerCell> allowed,HitPoint any,int cellw,int xpos,int ypos,String msg)
    {	ContainerChip ship = c.stackTopLevel()==0 ? null : c.topChip();
    	ContainerChip alt = ship;
    	boolean right = (xpos>G.Left(atSeaRect)+2*G.Width(atSeaRect)/3);
    	if((alt!=null)&&right) { alt = alt.getAltShip(); }
       	ContainerChip sample = (alt==null) ? ContainerChip.getShip(0) : alt;
       	double aspect = sample.getAspectRatio(this);
       	int aheight = (int)(cellw/aspect);
       	HitPoint hit =  isAllowed(allowed,highlight,c);
   		c.drawChip(gc, this ,alt,hit, cellw,aheight, xpos, ypos, msg);
 		if(alt!=null) 
 			{ HitPoint.setHelpText(any,xpos,ypos,cellw,aheight/2,s.get(ShipMessage,s.get(ship.getColor()))); 
 			} 
   		
		if(ship!=null)
		{
		drawShipContents(gc,gb,ship,!right,moving,highlight,allowed,any,cellw,xpos,ypos);
		}
		else if(isAllowed(allowed,any,c)!=null)
		{ 
		  drawLandingPad(gc,cellw/6,5.0,xpos,ypos);
		  //G.frameRect(gc,frameColor,xpos-cellw/3,ypos-cellh/2,3*cellw/5,cellh); 
		}
		}
    // this cell is an allowed target for drop
    private HitPoint isAllowed(Hashtable<ContainerCell,ContainerCell> dests,HitPoint highlight,ContainerCell c)
    {	return( (dests!=null) 
    				?  ((dests.get(c)!=null) ? highlight : null)
    				: c.topChip()!=null ? highlight : null);
    }
    
    private void showHands(Graphics gc,HitPoint highlight,boolean bidReady,ContainerBoard gb,ContainerBoard.playerBoard ob,Rectangle master,ContainerId hitCode)
    {	int playeridx = ob.player;
    	ContainerBoard.GuiBoard gui = gb.guiBoards[ob.player];
    	if(ob.showHandUp && !ob.cannotRebid)
			{	String msg = ob.showHandMsg;
			images[HAND_UP_INDEX].centerImage(gc,master);
				GC.setFont(gc,largeBoldFont()); 
				GC.Text(gc,true,G.Left(master)+(int)(G.Width(master)*0.4),G.Top(master)+(int)(G.Height(master)*0.16),G.Width(master)/3,G.Height(master)/3,Color.black,null,msg);

	   		if(bidReady)
 			  {	// show a button for "ACCEPT"
 				int highbid = gb.getHighBid();
 				if(ob.bidAmount()==highbid)
 				{
 				String amsg = s.get(AcceptMessage,""+highbid,prettyName(playeridx));
 		        	if(GC.handleRoundButton(gc, playerHands[playeridx], 
 		    			highlight,
 		        		amsg,
 		                HighlightColor, 
 		                ob.selectedAsFunder?HighlightColor:rackBackGroundColor))
 		                {
 		    			highlight.hitCode = hitCode;
 		    			highlight.row = playeridx;
 		                }
 				}
 			  }
			
			}
			else if((gui.ephemeral_loan_showHandDown || gui.ephemeral_auction_showHandDown || ob.showHandDown) && !ob.cannotRebid)
			{  images[HAND_DOWN_INDEX].centerImage(gc,master);
			}	
    }
    
    //
    // draw the buttons relating to bids and loans that are appropriate for the 
    // players responding to the auction or loan request
    // unlike almost all other buttons, these can be hit "out of turn"
    //
    private void drawBidChoiceButtons(Graphics gc,ContainerBoard gb,ContainerBoard.playerBoard bd,ContainerState vstate,HitPoint anyHighlight,
    		Rectangle declineRect,Rectangle fundRect,Rectangle amountRect,Rectangle done)
    {
       	ContainerBoard.GuiBoard gui = gb.guiBoards[bd.player];
       	//if(!bd.requestingLoan)
       	{
        if((vstate==ContainerState.FUND_LOAN_STATE) 
        		&& !bd.requestingLoan
        		&& !bd.bidReceived
        		&& !gui.pendingLoanDoneDone )
        {	GC.setFont(gc,largeBoldFont());
        	if(GC.handleRoundButton(gc, declineRect, 
        			anyHighlight,
            		s.get(DeclineLoan),
                    HighlightColor, 
                    (gui.pendingLoanDoneCode==ContainerId.HitDeclineLoan)?HighlightColor:rackBackGroundColor))
                    {
        			anyHighlight.hitCode = ContainerId.HitDeclineLoan;
                    }
        	int available_cash = bd.cash;
        	if(gui.pendingAuctionDoneDone) { available_cash -= bd.bidAmount(); }
        	else { available_cash -= bd.loanBidAmount(); }
        	if(available_cash>=STANDARD_LOAN_AMOUNT)
        		{if(GC.handleRoundButton(gc, fundRect, 
        			anyHighlight,
            		s.get(MakeLoan),
                    HighlightColor, 
                    (gui.pendingLoanDoneCode==ContainerId.HitFundLoan)?HighlightColor:rackBackGroundColor))
                    {
        			anyHighlight.hitCode = ContainerId.HitFundLoan;
                    }}

         	if((done!=null) 
         			&& (gui.pendingLoanDone||gui.pendingAuctionDone)
            		&& GC.handleRoundButton(gc, done, anyHighlight, s.get(DoneAction), HighlightColor, rackBackGroundColor))
            	{
         		anyHighlight.hitCode = ContainerId.HitPendingLoanDone;
            	}
        }	
        drawAuctionButtons(gc,vstate,anyHighlight,gb,bd,
        		amountRect,done);
       
       	}
    }
    private void drawAuctionButtons(Graphics gc,ContainerState vstate,HitPoint hp,ContainerBoard gb,ContainerBoard.playerBoard bd,
    		Rectangle bidAmountRect,Rectangle done)
    {	ContainerBoard.GuiBoard gui = gb.guiBoards[bd.player];
        boolean requestingAuction = ((vstate==ContainerState.AUCTION_STATE)||(vstate==ContainerState.REBID_STATE)) &&   !gui.pendingAuctionDoneDone && !bd.cannotRebid;
        boolean requestingLoan = (vstate==ContainerState.FINANCEER_STATE) && !gui.pendingLoanDoneDone && bd.willFundLoan;
        if((requestingAuction || requestingLoan)
        		&& !bd.requestingBid
        		&& !bd.bidReceived)
        {	
            String msg = ((vstate==ContainerState.FINANCEER_STATE)
            				?(gui.pendingLoanMove==null)
            				:(gui.pendingAuctionMove==null))
        			?s.get(SetBid)
        			:s.get(SetBidAmount,(vstate==ContainerState.FINANCEER_STATE) 
        									? gui.pendingLoanBid
        									: gui.pendingAuctionBid);

            GC.setFont(gc,largeBoldFont());

        	if(GC.handleRoundButton(gc, bidAmountRect, 
    			hp,
        		msg,
                HighlightColor, 
                (gui.pendingAuctionDoneCode==ContainerId.HitBidLocation)?HighlightColor:rackBackGroundColor))
                {
    			hp.hitCode = ContainerId.HitBidLocation;
                }
          	if((done!=null) 
          			&& (gui.pendingAuctionDone||gui.pendingLoanDone)
            		&& GC.handleRoundButton(gc, done, hp, s.get(DoneAction), HighlightColor, rackBackGroundColor))
            	{
          		hp.hitCode = requestingAuction 
          						? ContainerId.HitPendingAuctionDone
          						: ContainerId.HitPendingLoanDone;
            	}
        }
    }
    private void drawAtSeaBoats(Graphics gc,ContainerBoard gb,HitPoint highlight,Hashtable<ContainerCell,ContainerCell> allowed,HitPoint any)
    {
       	int nplayers = gb.atSea.length;
       	int map[] = gb.getColorMap();
     	// draw the at sea boats
       	for(int playerIndex=0,
      			cellw = G.Width(atSeaRect),
      			cellh = G.Height(atSeaRect)/nplayers,
     			xpos = G.Left(atSeaRect) + cellw/2,
     			ypos = G.Top(atSeaRect) + cellh/2; 
       		playerIndex<nplayers;
       		playerIndex++,ypos+=cellh)
     	{	int colorIndex = map[playerIndex];
     		ContainerCell c = gb.atSea[playerIndex];
     		drawShipAndContents(gc,gb,c,false,highlight,allowed,any,cellw,xpos,ypos,null);
     		ContainerChip ship = ContainerChip.getShip(colorIndex);
     		Rectangle r = shipRects[playerIndex];
     		if(G.Left(r)<xpos) { ship = ship.getAltShip(); }
     	ship.getAltChip(getAltChipset()).getImage(loader).centerImage(gc,r);
        		
     	}	
    }
    private void drawUnsoldMachines(Graphics gc,ContainerBoard gb,HitPoint highlight,Hashtable<ContainerCell,ContainerCell> allowed,HitPoint any)
    {
       	// draw the unused machine storage
       	for(int i=0,
       			lim=gb.machineStorage.length,
       			wid = G.Width(machineStorageRect)/2,
       			sw = wid-wid/4,
       			ystep = (G.Height(machineStorageRect)-CELLSIZE)/3,
       			y = G.Top(machineStorageRect)+ystep/2,
       			x = G.Left(machineStorageRect);
       		i<lim;
       		i++)
       	{	ContainerCell c = gb.machineStorage[i];
       		c.drawBrick(gc,isAllowed(allowed,highlight,c),true,x,y,this,sw,sw,2,true,2.0,-1.2,null);
       		HitPoint.setHelpText(any,sw,x+sw/2,y-sw/2,s.get(MachineMessage,c.getColor()));
       		if((i%2)==0) { x=G.Left(machineStorageRect); y+=ystep; }
       		else { x += wid; }
       	}	
    }
    private void drawUnsoldWarehouses(Graphics gc,ContainerBoard gb,HitPoint highlight,Hashtable<ContainerCell,ContainerCell> allowed,HitPoint any)
    {       	// draw the warehouse storage
   		int wid =  G.Width(warehouseStorageRect)-G.Width(warehouseStorageRect)/5;
   		int xp = G.Left(warehouseStorageRect);
   		int yp = G.Bottom(warehouseStorageRect)-CELLSIZE;
       	gb.warehouseStorage.drawBrick(gc,isAllowed(allowed,highlight,gb.warehouseStorage),true,xp,yp,
       		this,wid,wid/3,4,true,2.0,1.0,null);
       	HitPoint.setHelpText(any,warehouseStorageRect,s.get(UnsoldWarehouse));

    }
    //
    // this is the public stack of cards representing possible loans.
    //
    private void drawLoanCards(Graphics gc,ContainerBoard gb,HitPoint highlight,Hashtable<ContainerCell,ContainerCell> allowed,HitPoint any)
    {
       	
       	// draw the loan cards
    	int w = G.Width(loanRect);
    	int xp = G.Left(loanRect)+w/2;
    	int yp = G.centerY(loanRect);
        gb.loanCards.drawStack(gc,this,
       			isAllowed(allowed,highlight,gb.loanCards), G.Width(loanRect), xp,yp,0,0.05,null);
        HitPoint.setHelpText(any,loanRect,s.get(LoanCard));
    }
    private void drawPlayerCard(Graphics gc,Rectangle r,ContainerChip goal)
    {	int cx = G.centerX(r);
		goal.drawChip(gc,this,G.Width(r),cx,G.centerY(r),null);
		if(b.first_shipment)
		{
			// blank out the gold
			int sz = (int)(G.Width(r)*0.38);
			int yp = (int)(G.Height(r)*0.6);
			int xp = (int)(G.Width(r)*0.0);
		textures[PANELMASK_INDEX].centerImage(gc, cx-xp, G.Top(r)+yp,
						sz,sz);
		}

    }
    // interpolate boxes from a given position relative to the reference box
    // to the given destination rectangle
    private Rectangle interpolateBox(Rectangle referenceBox,Rectangle movingBox,Rectangle dest)
    {	int refw = G.Width(referenceBox);
    	int refh = G.Height(referenceBox);
    	double w = G.Width(dest);
    	double h = G.Height(dest);
    	int dx = G.Left(dest);
    	int dy = G.Top(dest);
    	double xscale = w/refw;
    	double yscale = h/refh;
    	double scale = Math.min(xscale, yscale);
    	int xoff = (int)(w-refw*scale)/2+dx;
    	int yoff = (int)(h-refh*scale)/2+dy;
    	int newx = (int)(xoff+(G.Left(movingBox)-G.Left(referenceBox))*scale);
    	int newy = (int)(yoff+(G.Top(movingBox)-G.Top(referenceBox))*scale);
    	int neww = (int)(G.Width(movingBox)*scale);
    	int newh = (int)(G.Height(movingBox)*scale);
    	return(new Rectangle(newx,newy,neww,newh));
    }
      
    
    //
    // draw the player's slice of island storage, his ship in island parking, and any
    // debugging information we are displaying on the island.
    //
    private void drawPlayerIsland(Graphics gc,ContainerBoard gb,int playeridx,HitPoint highlight,Hashtable<ContainerCell,ContainerCell> allowed,HitPoint any)
    {	ContainerBoard.playerBoard ob = gb.getPlayer(playeridx);
    	double islandSubRect[][] = {
   			{0.11,0.19,0.13,0.5},	// these are ad-hoc sub rectangles of the island rectangle
   			{0.28,0.17,0.13,0.5},
   			{0.43,0.16,0.13,0.5},
   			{0.58,0.15,0.13,0.5},
   			{0.73,0.14,0.13,0.5}};
    	int map[]=gb.getColorMap();
    	double sr[] = islandSubRect[map[playeridx]];
		Rectangle r = subRectangle(islandRect,sr);
		// island storage	
		boolean show = showDevelopInfo() || allowed_to_edit;
		// get the current values set before drawing
		CC.islandGoodsValue(selectedGoalSet().goals[playeridx],ob.islandGoods);
		drawContainerStorage(gc,gb,r,highlight,allowed,any,false,ob.islandGoods,2,show);
		if(showDevelopInfo() || allowed_to_edit)
		{	GC.setFont(gc,largeBoldFont());
			GC.Text(gc,true,G.Left(r),G.Top(r)-CELLSIZE,30,30,Color.black,null,"$ "+ob.islandGoodsValue(selectedGoalSet()));
		}
		// island parking space
		{
		ContainerCell c = gb.islandParking[playeridx]; 
 		int rpos = G.Right(islandRect);
  		int hdiv = G.Height(islandRect)/9;
  		int hpos = G.Top(islandRect)+hdiv*(playeridx+1); 		
		drawShipAndContents(gc,gb,c,false,highlight,allowed,any,(int)(G.Width(atSeaRect)*0.9),rpos,hpos,null);
		}

    }
    
    private void drawPlayerMachinesAndGoods(Graphics gc,ContainerBoard gb,HitPoint highlight,Hashtable<ContainerCell,ContainerCell> allowed,boolean allowGold,
    				HitPoint any,
    				boolean moving,ContainerCell source,Rectangle master,ContainerBoard.playerBoard ob)
    {
		//
		// player's machines and machine goods
		//
    	boolean perspective = usePerspective();
		boolean left = (G.Left(master)<G.Width(boardRect)/2);
		double macpos_l_p [][] ={{0.2,0.17}, {0.19,0.27},{0.19,0.38},{0.18,0.53}};	// top to bottom on the left side
		double macpos_l [][] ={{0.21,0.12}, {0.21,0.27},{0.21,0.40},{0.21,0.53}};	// top to bottom on the left side
		double macpos_r_p [][] = {{0.80,0.48}, {0.82,0.60},{0.82,0.72},{0.83,0.84}};// top to bottom on the right side
		double macpos_r [][] = {{0.74,0.49}, {0.74,0.63},{0.74,0.77},{0.74,0.90}};  // top to bottom on the right side
		double macpos[][] = perspective 
							? left ? macpos_l_p : macpos_r_p
							: left ? macpos_l : macpos_r;
		
		for(int idx = 0,nmachines=ob.machines.length;  idx<nmachines; idx++)
		{	int machine_index = left ? idx : nmachines-idx-1;
			int goods_index = nmachines-machine_index-1;
			double off[] = macpos[idx];
		 	ContainerCell machine = ob.machines[machine_index];
			ContainerCell mgood = ob.factoryGoods[goods_index];
			int mw = G.Width(master);
			int mh = G.Height(master);
			int xp = G.Left(master)+(int)(off[0]*mw);
			int yp = G.Top(master)+(int)(off[1]*mh);
			int wid = (int)(mw*0.18);
			int offz = (int)(mw*(left ? 0.06 : -0.24));
			int w = mw/10;
			ContainerChip chip = machine.topChip();
			HitPoint allowm = isAllowed(allowed,highlight,machine);
			machine.drawStack(gc, this, allowm, w, xp, yp, 0, 1.0, null);
			if(chip!=null) { HitPoint.setHelpText(any,w,xp,yp,s.get(MachineMessage,chip.getColor())); }
			if(moving && (isAllowed(allowed,any,machine)!=null))
				{
				drawLandingPad(gc,w,1.0,xp,yp);
				}
			
			{
   			HitPoint alloww = isAllowed(allowed,highlight,mgood);
   			HitPoint allowa = isAllowed(allowed,any,mgood);
   			ContainerChip forbidden = 
   					(gb.board_state==ContainerState.TRADE_CONTAINER_STATE)
   						?gb.produceLuxuryFirstColor
   						:null;
   			boolean hitAny = false;
   			switch(b.board_state)
   			{
   			case REPRICE_FACTORY_STATE:
   			case PUZZLE_STATE:
   				hitAny = true;
				//$FALL-THROUGH$
			default: if(!b.isDest(mgood)) { hitAny = true; }
   			}
   			mgood.drawBrick(gc,alloww,hitAny,allowGold,forbidden,xp+offz,yp,this,(int)(wid*1.2),wid/3,5,left,2.0,1.0,null);
   			if(moving && (allowa!=null))
   			{ 
   			  drawLandingPad(gc,wid/3,3.0,xp+offz+wid/2,yp);
   			}
  			if(moving 
  					&& (source.rackLocation==ContainerId.ContainerLocation) 
  					&& (ob.player==gb.whoseTurn)
  					&& (allowed!=null) 
  					&& (allowa==null))
   			{	StockArt.SmallX.drawChip(gc,this,3*wid/4,xp+offz/2,yp,null);
   			}
   			}       			
		}
    }
    private void drawPlayerWarehousesAndGoods(Graphics gc,ContainerBoard gb,HitPoint highlight,Hashtable<ContainerCell,ContainerCell> allowed,boolean allowGold,
    		HitPoint any,
			boolean moving,ContainerCell source,Rectangle master,ContainerBoard.playerBoard ob)
    {
    	boolean left = (G.Left(master)<G.Width(boardRect)/2);
    	boolean perspective = usePerspective();
       		//
       		// players's warehouses and warehouse goods
      		//
       		double macpos_l_p [][] = {{0.51,0.20}, {0.51,0.33},{0.51,0.46},{0.51,0.61},{0.52,0.77}};	// top to bottom left
      		double macpos_r_p [][] = {{0.47,0.25},{0.47,0.38},{0.48,0.50},{0.48,0.65},{0.48,0.82}};	// top to bottom right
      		double macpos_l [][] = {{0.51,0.12}, {0.51,0.29},{0.51,0.46},{0.51,0.64},{0.52,0.81}};	// top to bottom left
      		double macpos_r [][] = {{0.47,0.16},{0.47,0.36},{0.48,0.55},{0.48,0.73},{0.48,0.88}};	// top to bottom right
     		double macpos[][] = perspective 
     							? left ? macpos_l_p : macpos_r_p
     							: left ? macpos_l : macpos_r;
      		
      		for(int idx = 0,nware=ob.warehouses.length;  idx<nware; idx++)
       		{	double off[] = macpos[idx];
       			int machine_index = left ? idx : nware-idx-1;
       			int goods_index = nware-machine_index-1;
       			ContainerCell warehouse = ob.warehouses[machine_index];
       			ContainerCell wgood =  ob.warehouseGoods[goods_index];
        		int mw = G.Width(master);
       			int mh = G.Height(master);
       			int xp = G.Left(master)+(int)(off[0]*mw);
       			int yp = G.Top(master)+(int)(off[1]*mh);
       			int wid = (int)(mw*0.18);
       			int offz = (int)(mw*(left ? 0.08 : -0.28));
       			int w = mw/8;
       			ContainerChip chip = warehouse.topChip();
       			HitPoint allowm = isAllowed(allowed,highlight,warehouse);
       			warehouse.drawStack(gc,this,allowm,w,xp,yp,0,1.0,null);
       			if(moving && (isAllowed(allowed,any,warehouse)!=null))
       			{
       				drawLandingPad(gc,w,1.0,xp,yp);
       			}
       			if(chip!=null) { HitPoint.setHelpText(any,w,xp,yp,s.get(WarehouseMessage)); }
       			{
       			HitPoint alloww = isAllowed(allowed,highlight,wgood);
       			HitPoint allowa = isAllowed(allowed,any,wgood);
       			ContainerChip forbidden = 
   					(gb.board_state==ContainerState.TRADE_CONTAINER_STATE)
   						?gb.produceLuxuryFirstColor
   						:null;
       			boolean hitAny = false;
       			switch(b.board_state)
       			{
       			case REPRICE_WAREHOUSE_STATE:
       			case PUZZLE_STATE:
       				hitAny = true;
					//$FALL-THROUGH$
				default: if(!b.isDest(wgood)) { hitAny = true; }
       			}
       			wgood.drawBrick(gc,alloww,hitAny,allowGold,forbidden,xp+offz,yp,this,(int)(wid*1.2),wid/3,5,left,2.0,1.0,null);
       			if(moving && allowa!=null)
       			{
       				drawLandingPad(gc,wid/3,3.0,xp+offz+wid/2,yp);
       			}
       			if(moving 
       					&& ((source.rackLocation==ContainerId.WarehouseGoodsLocation) && (ob.warehouseGoods[0].col==source.col)	//owned by us
       							|| (source.rackLocation==ContainerId.FactoryGoodsLocation))
       					&& (ob.player==gb.whoseTurn) 
       					&& (allowed!=null) 
       					&& (allowa==null))
       			{	StockArt.SmallX.drawChip(gc,this,3*wid/4,xp+offz/2,yp,null);
       			}
       			}
       			
       		}  	
    }
    private void drawPlayerDocks(Graphics gc,ContainerBoard gb,HitPoint highlight,boolean moving,Hashtable<ContainerCell,ContainerCell> allowed,HitPoint any,
    		Rectangle master,ContainerBoard.playerBoard ob)
    {
    	boolean left = (G.Left(master)<G.Width(boardRect)/2);
    	boolean perspective = usePerspective();
   		//
   		// players's docks
  		//
   		double macpos_l_p [][] = {{0.97,0.17}, {0.98,0.37},{0.99,0.55},{1.0,0.77}};
  		double macpos_r_p [][] = { {0.01,0.84},{0.02,0.62},{0.01,0.41},{0.00,0.22}};
   		double macpos_l [][] = {{0.99,0.13}, {0.99,0.33},{0.99,0.57},{0.99,0.8}};
  		double macpos_r [][] = { {-0.04,0.89},{-0.06,0.67},{-0.05,0.43},{-0.05,0.18}};
 		double macpos[][] = 
 				perspective
 					? left ? macpos_l_p : macpos_r_p
 					: left ? macpos_l : macpos_r;
  		
  		for(int ndocks=ob.docks.length, shipn = 0;  shipn<ndocks; shipn++)
   		{	int idx = left ? shipn : ndocks-shipn-1;
   			ContainerCell dock = ob.docks[idx];
   			int mw = G.Width(master);
   			int mh = G.Height(master);
   			double off[] = macpos[idx];
   			int xp = G.Left(master)+(int)(off[0]*mw);
   			int yp = G.Top(master)+(int)(off[1]*mh);
    		drawShipAndContents(gc,gb,dock,moving,highlight,allowed,any,G.Width(atSeaRect),xp,yp,null);
   			//dock.drawChip(gc,dock.topChip(),this,highlight,mw/10,xp,yp,null);
   			
   		}      	
    }
    
    //
    // draw the individual loan cards representing outstanding loans
    //
    private void drawPlayerGoalAndLoans(Graphics gc,ContainerBoard gb,HitPoint highlight,
    		Hashtable<ContainerCell,ContainerCell> allowed,
    		HitPoint any,
    		Rectangle master,ContainerBoard.playerBoard ob)
    {
		boolean left = (G.Left(master)<G.Width(boardRect)/2);
			//
			// players's goal and loans
			//
			//boolean moving = gb.movingObjectIndex()>0;
			double macpos_l [][] = {{0.2,0.82}, {0.30,.78},{0.4,0.79}};
			double macpos_r [][] = { {0.8,0.24},{0.7,0.26},{0.6,0.23}};
			double macpos[][] = left ? macpos_l : macpos_r;
				
			for(int nloans=ob.loans.length,idx=nloans;  idx>=0; idx--)
			{	
				ContainerCell loan = (idx==nloans)?ob.goalCell:ob.loans[idx];
				boolean active = ob.loanIsActive(loan);
				String msg = (idx==nloans) ? "" : active?""+ob.loanAmount(loan): "";
				int mw = G.Width(master);
				int mh = G.Height(master);
				double off[] = macpos[idx];
				int xp = G.Left(master)+(int)(off[0]*mw);
				int yp = G.Top(master)+(int)(off[1]*mh);
				labelColor = Color.black;
				labelFont = largeBoldFont();
				
				 {Color cc = labelColor;
				  HitPoint hit = (idx==nloans)?null:isAllowed(allowed,highlight,loan);
				  HitPoint anyHit = (idx==nloans)?null:isAllowed(allowed,any,loan);
				  int siz = mw/8;
				  if(idx!=nloans) { labelColor=Color.red; }
				  if(!active && (anyHit!=null) && (loan.topChip()==null)) 
				  	{ //G.frameRect(gc,frameColor,xp-siz/2,yp-siz/2,2*siz/3,siz); 
				  	  drawLandingPad(gc,2*siz/3,1.3,xp,yp);
				  	}
				  
				  if(idx==nloans)
				  {	// drawing the goal card
					  ContainerChip card = selectedGoalSet().goals[ob.player]; 
					  boolean spectator = getActivePlayer().spectator;
					  boolean inrect = G.pointInRect(any,xp-siz/2,yp-siz/2,siz,siz);
					  boolean itsMe = allowed_to_edit || GameOver() || (!spectator && !isPassAndPlay && (ob==getCurrentPlayerBoard(gb)));
					  boolean canShow = itsMe || G.offline() ;
					  boolean showBig = inrect && canShow && any.down;
					  boolean hideDetails = isPassAndPlay && !showBig;
					  int size = showBig ? siz*3 : siz;
					  if(inrect) { setDraggingBoard(showBig); }
					  if(inrect && !showBig) { any.setHelpText(s.get(SeeCashMessage,prettyName(ob.player))); }
					  int cardLeft = xp-size/2;
					  int cardRight = xp+size/2;
					  int cardTop =  yp-size/2 - (showBig ? size/6:0);
					  if(hideDetails) { card = ContainerChip.BLANK_CARD; }
						  //loan.drawChip(gc,this,card,hit,siz*3,xp,yp-siz,null);
					  if(inrect)
					  	{ any.hitCode = ContainerId.SeeCash; 
					  	}
					  if(canShow || showBig)
					  {
						  Rectangle r = new Rectangle(cardLeft,cardTop,size,size);
						  drawPlayerCard(gc,r,card);
						  if(!hideDetails)
						  {
						  int cash = ob.cash;
						  int space = (int)(0.9*size);
						  GC.setFont(gc, largeBoldFont());
						  Rectangle price = new Rectangle( cardRight, cardTop-size/3, space,space/2);
						  GC.Text(gc, true, price,Color.black,Color.white,"$ "+cash);
						  GC.frameRect(gc, Color.black,price);
						  int score = gb.currentScoreForPlayer(ob.player);
						  if(showBig)
						  {
						  G.SetTop(price,G.Bottom(price)+size/8);
						  GC.Text(gc, true, price, Color.black,Color.white,"= "+score);
						  GC.frameRect(gc,Color.black, price);
						  }
						  }}
				  }

				  	else 
				  	  {loan.drawChip(gc,this,loan.topChip(),hit,siz,xp,yp,null);
					  }
				 
				  GC.setFont(gc,largeBoldFont());
				  GC.Text(gc,true,xp-mw/50,yp-mh/5,mw/10,mh/5,Color.black,null,msg);
				  
				  if(active) 
				  	{	int funder = ob.loanFunder(loan);
				  		HitPoint.setHelpText(any,siz,xp,yp,
				  				(funder<0)? s.get(LoanFromBank,msg)
				  						: s.get(LoanForMessage,msg,prettyName(funder)));
				  	}
				  labelColor=cc;
				 }
			
			}	
    }
    
    void drawLoanClosing(Graphics gc,ContainerBoard gb,HitPoint highlight,ContainerBoard.playerBoard ob,Rectangle master)
	{	ContainerState vstate = gb.getState();
		boolean bidReady  = (vstate==ContainerState.ACCEPT_LOAN_STATE)||(vstate==ContainerState.CONFIRM_AUCTION_STATE)||(vstate==ContainerState.CONFIRM_STATE);
		
		// draw loan funding info
		if((vstate==ContainerState.ACCEPT_LOAN_STATE)
				||(vstate==ContainerState.FUND_LOAN_STATE)
				||(vstate==ContainerState.FINANCEER_STATE))	// accept/decline of loan or bidding on a loan
		{
		Rectangle mr =playerHands[ob.player];
		if((vstate==ContainerState.FUND_LOAN_STATE)||(vstate==ContainerState.FINANCEER_STATE))
		{
		String msg = ob.requestingLoan ? s.get(RequestingLoan)
						 : (ob.ephemeral_willFundLoan ? s.get(IWillFund)
							: (ob.ephemeral_wontFundLoan? s.get(BankWillFund)
								: "?"));
		GC.setFont(gc,largeBoldFont());
		GC.Text(gc,true,mr,Color.black,rackBackGroundColor,msg);
		GC.frameRect(gc,Color.black,mr);
		}}
		
		if(ob.requestingLoan && bidReady)
		{
		int playeridx = ob.player;
		int bidders = 0;
		for(int i=0,limit=nPlayers(); i<limit; i++) 
			{ ContainerBoard.playerBoard bbd = gb.getPlayer(i);
			  if((bbd!=ob)&&bbd.willFundLoan) { bidders++; }
			}
		GC.setFont(gc,largeBoldFont());
		if(bidders==0)
		{
		String amsg = s.get(AcceptFromBank);
    	if(GC.handleRoundButton(gc, playerHands[playeridx], 
			highlight,
    		amsg,
            HighlightColor, 
            ob.willFundLoan?HighlightColor:rackBackGroundColor))
            {
    		highlight.hitCode = ContainerId.HitAcceptLoanLocation;
    		highlight.row = -1;
            }}
		String bmsg = s.get(DeclineFromBank);
		Rectangle r1 = G.clone(playerHands[playeridx]);
		G.SetTop(r1, G.Top(r1)+ G.Height(r1)+CELLSIZE/2);
		if(!gb.mustPayLoan)
		{
		if(GC.handleRoundButton(gc, r1, 
    		highlight,
    		bmsg,
            HighlightColor, 
            ob.declinedLoan?HighlightColor:rackBackGroundColor))
            {
    	highlight.hitCode = ContainerId.HitDeclineLoanLocation;
            }
		}
		}
		else
		{showHands(gc,getCurrentPlayerBoard(gb).requestingLoan?highlight:null,true,gb,ob,master,ContainerId.HitAcceptLoanLocation);
		}
		

	}
    
    private void drawBidClosing(Graphics gc,ContainerBoard gb,HitPoint highlight,ContainerBoard.playerBoard ob,Rectangle master)
    {	int playeridx = ob.player;
    	ContainerState vstate = gb.getState();
		boolean bidReady = (vstate==ContainerState.ACCEPT_BID_STATE)||(vstate==ContainerState.CONFIRM_AUCTION_STATE);
		if(ob.requestingBid)
		{GC.setFont(gc,largeBoldFont());
		 GC.Text(gc,true,playerHands[playeridx],Color.black,rackBackGroundColor,s.get(RequestingBids));
		 GC.frameRect(gc,Color.black,playerHands[playeridx]);
		}
		if(ob.requestingBid && bidReady)
		{	// we're runnang the auction, and the bid is ready.  We have the 
			// option to buy it ourselves if we have the cash
			int highbid = gb.getHighBid();
			boolean wefund = ob.selectedAsFunder;
			//
			// a special complication here if we are low on cash, we can only do the
			// "buy it" option if we have enough.  The cash balance has already been
			// adjusted if we are in confirm state, so we might think we already have
			// the cash we would get if we sold the goods normally.
			//
			int available_cash = (vstate==ContainerState.ACCEPT_BID_STATE) 
					? ob.cash 	// not decided yet
					: wefund 
						? ob.cash+highbid 		// cash was deducted
						: ob.cash-highbid*2;	// cash was added
			if(available_cash >= highbid)
			{
				String amsg = s.get(BuyMessage,highbid);
	        	if(GC.handleRoundButton(gc, playerHands[playeridx], 
	        			highlight,
	        		amsg,
	                HighlightColor, 
	                wefund ? HighlightColor:rackBackGroundColor))
	                {
	        		highlight.hitCode = ContainerId.HitBuyLocation;
	        		highlight.row = highbid;
	                }
				
			}
		}
		else 
			{ showHands(gc,getCurrentPlayerBoard(gb).requestingBid?highlight:null,bidReady,gb,ob,master,ContainerId.HitAcceptLocation);
			}	
    }
    
    private void drawCashStats(Graphics gc,ContainerBoard.playerBoard ob,HitPoint highlight,Rectangle r)
    {	boolean isLeft = G.Left(r)<G.Width(boardRect)/2;
    	int cellH = (G.Height(r)-4)/6;
    	int cellW = (G.Width(r)-4)/6;
    	int ix = G.Left(r)+2;
    	int title = ix + (isLeft?0:cellW*3);
    	int titlew = cellW*3;
    	int number1 = isLeft?title+titlew:ix;
    	int number2 = number1+cellW;
    	int number3 = number2+cellW;
    	int rowNumber = G.pointInRect(highlight,r) ? (G.Top(highlight)-G.Top(r))/cellH : -1;
    	String help = null;
    	int iy = G.Top(r)+2;
    	ContainerBoard.ContainerGoalSet goalset = selectedGoalSet();
    	Color bgcolor = (ob.player==selectedPerspective)? new Color(0.8f,0.8f,1.0f) : Color.white;
		if(showDevelopInfo() && G.pointInRect(highlight,r))
		{
			highlight.hitCode = ContainerId.HitSelectPerspective;
			highlight.row = ob.player;
		}
    	GC.setColor(gc,bgcolor);

    	{
    	GC.fillRect(gc, r);
    	GC.setFont(gc,standardPlainFont());
    	int cash = ob.cash;
    	int score = +b.currentScoreForPlayer(ob.player,goalset);
    	GC.Text(gc,false,title,      iy,titlew,cellH,Color.black,null,s.get(CashScore));
    	GC.Text(gc,true, number1,iy,cellW,cellH,Color.black,null,""+cash);
    	GC.Text(gc,true, number2,iy,cellW,cellH,Color.blue,null,""+score);
    	if(rowNumber==0)
    	{
    		help = s.get(CashOnHand,cash);
    		help += "\n"+ s.get(FinalScore,score);
    	}
    	if(showDevelopInfo()) 
    		{String vcash = " "+(ob.virtual_cash/100); 
    		GC.Text(gc,true, number3,iy,cellW,cellH,Color.blue,null,vcash);
    		}
    	}
    	{
     	iy += cellH;
     	int cashout = +ob.machine_cash_out;
     	int cashin = ob.machine_cash_in;
    	GC.Text(gc,false,title,iy,titlew,cellH,Color.black,null,s.get(MachinesMessage));
       	GC.Text(gc,true, number1,iy,cellW,cellH,Color.black,null,"-"+cashout);
    	GC.Text(gc,true, number2,iy,cellW,cellH,Color.blue,null,""+cashin);
    	if(rowNumber==1)
    	{
    		help = s.get(CashForMachines,cashout);
    	    help += "\n" + s.get(CashForSelling,cashin);
    	}
    	if(ob.machine_turns>0)
    	{	int eff = ((ob.machine_cash_in-ob.machine_cash_out)*10)/ob.machine_turns;
    		String effStr = ""+(eff/10)+"."+(Math.abs(eff)%10);
    	    GC.Text(gc,true, number3,iy,cellW,cellH,Color.blue,null,effStr);
    	    if(rowNumber==1)
    	    {
    	    	help += "\n"+s.get(CashPerProduction,effStr);
    	    }
    	}
    	
    	}
    	{
    	iy += cellH;
    	int cashout = ob.warehouse_cash_out;
    	int cashin = ob.warehouse_cash_in;
    	GC.Text(gc,false,title,iy,titlew,cellH,Color.black,null,s.get(WarehousesMessage));
    	
       	GC.Text(gc,true, number1,iy,cellW,cellH,Color.black,null,"-"+cashout);
    	GC.Text(gc,true, number2,iy,cellW,cellH,Color.blue,null,""+cashin);
    	if(rowNumber==2)
    	{
    		help = s.get(CashForGoods,cashout);
    		help += "\n" + s.get(CashForSelling,cashin);
    	}
    	if(ob.warehouse_turns>0)
    		{
    		int eff = ((ob.warehouse_cash_in-ob.warehouse_cash_out)*10)/ob.warehouse_turns;
    		String effstr = ((eff<0)?"-":"")+(eff/10)+"."+(Math.abs(eff)%10);
    		GC.Text(gc,true, number3,iy,cellW,cellH,Color.blue,null,effstr);
    		if(rowNumber==2)
    		{
    			help +=  "\n" + s.get(CashPerWarehouse,effstr);
    		}
    		}
    	}
    	{
    	iy += cellH;
    	int cashout = ob.ship_cash_out;
    	int cashin = ob.ship_cash_in;
    	GC.Text(gc,false,title,iy,number1,cellH,Color.black,null,s.get(ShippingMessage));
       	GC.Text(gc,true, number1,iy,cellW,cellH,Color.black,null,"-"+cashout);
    	GC.Text(gc,true, number2,iy,cellW,cellH,Color.blue,null,""+cashin);
    	if(rowNumber==3)
    	{
    		help = s.get(CashForShipping,cashout);
    		help += "\n"+ s.get(CashFromAuctions,cashin);
    	}
    	if(ob.ship_turns>0)
    	{
    	int eff = ((ob.ship_cash_in-ob.ship_cash_out)*10)/ob.ship_turns;
    	String effstr = ((eff<0)?"-":"")+(eff/10)+"."+(Math.abs(eff)%10);
    	GC.Text(gc,true, number3,iy,cellW,cellH,Color.blue,null,effstr);
    	if(rowNumber==3)
    	{
    		help += "\n"+s.get(CashPerAction,effstr);
    	}
    	}}
    	{
       	iy += cellH;
       	int cashout = ob.island_cash_out;
    	GC.Text(gc,false,title,iy,titlew,cellH,Color.black,null,s.get(IslandMessage));
       	GC.Text(gc,true, number1,iy,cellW,cellH,Color.black,null,"-"+cashout);
       	{int value = ob.islandGoodsValue(goalset);
    	GC.Text(gc,true, number2,iy,cellW,cellH,Color.blue,null,""+value);
       	if(rowNumber==4)
       	{
       		help = s.get(CashSpent,cashout);
       		help += "\n"+s.get(IslandValue,value);
       	}
    	if(ob.island_cash_out>0)
       	{
       		int eff = ((value-ob.island_cash_out)*10)/ob.island_cash_out;
       		int aeff = Math.abs(eff);
       		String effstr = ((eff<0)?"-":"")+(aeff/10)+"."+(aeff%10);
       		GC.Text(gc,true, number3,iy,cellW,cellH,Color.blue,null,effstr);
       		if(rowNumber==4)
       		{
       			help += "\n"+s.get(IslandEffeciency,effstr);
       		}
       		
       	}}}
    	{
    	iy += cellH;
    	int cashout = +ob.interest_out;
    	int cashin = ob.interest_in;
    	GC.Text(gc,false,title,iy,titlew,cellH,Color.black,null,s.get(LoansMessage));
       	GC.Text(gc,true, number1,iy,cellW,cellH,Color.black,null,"-"+cashout);
    	GC.Text(gc,true, number2,iy,cellW,cellH,Color.blue,null,""+cashin);
    	if(rowNumber==5)
    	{
    		help = s.get(InterestPaid,cashout);
    		help += "\n"+s.get(PaymentsReceived,cashin);
    	}
    	}
    	GC.frameRect(gc,Color.black,r);
    	if(help!=null) 
    		{ highlight.setHelpText(help); }
    }
    //
    // draw all the things on the player mat.
    //
    private void drawPlayerMat(Graphics gc,ContainerBoard gb,int playeridx,HitPoint highlight,
    		Hashtable<ContainerCell,ContainerCell> allowed,boolean allowFactoryGold,HitPoint any,boolean moving,
    		Rectangle statRect)
    {	ContainerBoard.playerBoard ob = gb.getPlayer(playeridx);
 		ContainerCell source = moving ? gb.getSource() : null;
 		Rectangle master = playerMats[playeridx];
 
  		drawPlayerMachinesAndGoods( gc, gb, highlight, allowed,allowFactoryGold,any,
				 moving, source, master,ob)	;
  		drawPlayerWarehousesAndGoods( gc, gb, highlight, allowed,true,any,
				 moving, source, master,ob)	;
  		drawPlayerDocks( gc, gb, highlight, moving,allowed,any,
				 master,ob)	;
	
  		drawPlayerGoalAndLoans( gc, gb, highlight, allowed,any,
				 master,ob)	;

		if(gb.someoneRequestingLoan())
		{	
			drawLoanClosing( gc, gb,highlight,ob, master);
		}
		else if(gb.playerRequestingBid()>=0)
		{ // draw the hand for bids
			drawBidClosing(gc,gb,highlight,ob, master);
		}
		if(allowed_to_edit || GameOver() /*||showDevelopInfo()*/) { drawCashStats(gc,ob,highlight,statRect); };

	}
    
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, ContainerBoard gb, Rectangle brect, HitPoint highlight,HitPoint anyHighlight)
    {   ContainerState vstate = gb.getState();
     	boolean moving = gb.pickedObject!=null;
     	HitPoint playerHighlight = anyHighlight;
     	if(showDevelopInfo())
     	{	double turns = gb.estimated_turns_remaining_v5();
     		double stage = gb.game_stage_v5(turns);
     		int turnint = (int)(turns*10);
     		int stageint = (int)(stage*100);
     		GC.setFont(gc, standardPlainFont());
     		GC.Text(gc,false,G.Left(brect),G.Top(brect),G.Width(brect),20,Color.black,null,"Game turns "+(turnint/10)+"."+(turnint%10)+" "+" stage "+stageint+"%");
     	}
     	//
     	// make sure sources, dests, and allowed are null if it's not our turn
     	//
     	Hashtable<ContainerCell,ContainerCell> dests = moving ? gb.getDests() : null;
     	Hashtable<ContainerCell,ContainerCell> sources =  moving ? null : gb.getSources();
     	Hashtable<ContainerCell,ContainerCell> allowed = sources!=null ? sources : dests;
		int nplayers = gb.nPlayers();
    	
		if(moving && (allowed!=null)) 
		{ 	ContainerCell src =  gb.getSource();
			allowed.put(src,src); 
		}
		if(highlight!=null) 
		{ highlight.spriteColor = Color.yellow; 
		}
		
		drawAtSeaBoats(gc,gb,highlight,allowed,anyHighlight);			// boats at sea
       	drawUnsoldMachines(gc,gb,highlight,allowed,anyHighlight);		// unsold machines
       	drawUnsoldWarehouses(gc,gb,highlight,allowed,anyHighlight);		// unsold warehouses
        drawLoanCards(gc,gb,highlight,allowed,anyHighlight);				// pool of loan cards
      	drawContainerStorage(gc,gb,containerStorageRect,highlight,allowed,anyHighlight,moving,gb.containerStorage,5,false);		// unsold containers
       	ContainerBoard.playerBoard bd = getCurrentPlayerBoard(gb);
		drawBidChoiceButtons(gc,gb,bd,vstate,playerHighlight,
				declineLoanRect,fundLoanRect,bidAmountRect,null);	// auction/loan responder buttons

      	// draw the auction block
   		drawShipAndContents(gc,gb,gb.auctionBlock,moving,highlight,allowed,anyHighlight,G.Width(atSeaRect),
   				G.Right(islandRect)-G.Width(islandRect)/2,G.Bottom(islandRect)-G.Height(islandRect)/4,s.get(AuctionMessage));


      	//
       	// draw each player's board and goods
       	//
   		boolean allowFactoryGold = gb.canBuyFactoryGold();
       	for(int playeridx=0;     	playeridx<nplayers;       	playeridx++)
       		{	
       			drawPlayerIsland(gc,gb,playeridx,highlight,allowed,anyHighlight);
       			drawPlayerMat(gc,gb,playeridx,highlight,allowed,(playeridx!=gb.whoseTurn)?allowFactoryGold:true,anyHighlight,moving,statRects[playeridx]);
       		}
       	
       	
       	if((highlight!=null) && (highlight.hitCode!=DefaultId.HitNoWhere))
       	{	highlight.arrow = (gb.pickedObject!=null)?StockArt.DownArrow:StockArt.UpArrow;
       		highlight.awidth = CELLSIZE;
        	}
       	
    }
    public boolean hasCompletedAuction(int idx)
    {	
    	for(int lim=History.size(),i=idx; i<lim;i++)
    	{
    		commonMove m = History.elementAt(i);
    		if(m!=null && ((m.op==MOVE_BUY) || (m.op==MOVE_ACCEPT))) { return(true); }
    	}
    	return(false);
    }
    public Text censoredMoveText(commonMove sp,int idx)
    {	
    	if(sp.op==MOVE_BID)
    	{
    	if(!allowed_to_edit && !hasCompletedAuction(idx))
    		{
    		return(TextChunk.create("Bid ??"));
    		}
    	}
    	return(sp.shortMoveText(this));   	
    }

    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {
      ContainerBoard gb = disB(gc);
      int nPlayers = gb.nPlayers();
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      ContainerState vstate = gb.getState();
      gameLog.redrawGameLog2(gc,ourSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),standardBoldFont());

        
       drawPlayerStuff(gc,(vstate==ContainerState.PUZZLE_STATE),ourSelect,HighlightColor, rackBackGroundColor);

       //drawRack(gc,ot,0,URPlayerPieceRect,gb); 
       //drawRack(gc,ot,1,LRPlayerPieceRect,gb); 
       //drawRack(gc,ot,2,LLPlayerPieceRect,gb); 
       //if(nPlayers>3) { drawRack(gc,ot,3,ULPlayerPieceRect,gb); }


       
       if(gc!=null)
       {
       
       GC.setFont(gc,largeBoldFont());
       GC.setColor(gc,Color.black);
       for(int i=0;i<nPlayers;i++)
       	{
    	   DrawPlayerMat(gc,ot, i,playerMats[i]);
       	}
       }
        
       speedRect.draw(gc,ourSelect);
       drawViewsetMarker(gc,viewsetRect,ourSelect);
       drawBoardElements(gc, gb, boardRect, ot, highlight);

		if (vstate != ContainerState.PUZZLE_STATE)
        {
			GC.setFont(gc,standardBoldFont());
			ContainerBoard.playerBoard bd = getCurrentPlayerBoard(gb);
			ContainerBoard.GuiBoard gui = gb.guiBoards[bd.player];
			boolean isAuction = (vstate==ContainerState.AUCTION_STATE)||(vstate==ContainerState.REBID_STATE);
            if (GC.handleRoundButton(gc, doneRect, 
            		(isAuction&&gui.pendingAuctionDone)||gui.pendingLoanDone?highlight:(b.DoneState() ? select : null), s.get(DoneAction),
                    HighlightColor, rackBackGroundColor))
            {	// always display the done button, but only make it active in
            	// the appropriate states
            	highlight.hitCode = 
            			gui.pendingLoanDone
            			? ContainerId.HitPendingLoanDone 
            			: ((isAuction&&gui.pendingAuctionDone) ? ContainerId.HitPendingAuctionDone : GameId.HitDoneButton);
            }
        }
		if((vstate==ContainerState.PLAY1_STATE)||(vstate==ContainerState.PLAY2_STATE))
		{ 
			passButton.show(gc, select);
		}
		if (vstate != ContainerState.PUZZLE_STATE)
		{
			handleEditButton(gc,editRect,select,highlight, HighlightColor, rackBackGroundColor);
		}
 
        if (gc != null)
        {	String msg = s.get(vstate.getDescription());
			ContainerBoard.playerBoard bd = getCurrentPlayerBoard(gb);
			ContainerBoard.GuiBoard gui = gb.guiBoards[bd.player];
        	boolean isAuction = (vstate==ContainerState.AUCTION_STATE) || (vstate==ContainerState.REBID_STATE);
            if( (isAuction && gui.pendingAuctionDoneDone) ||  gui.pendingLoanDoneDone)
            	{ msg = s.get(WaitForPlayers); 
            	}
            else if(gui.pendingAuctionDone || gui.pendingLoanDone) { msg = ContainerState.CONFIRM_STATE.getDescription(); }
            standardGameMessage(gc,
            		vstate==ContainerState.GAMEOVER_STATE?gameOverMessage():msg,
            				vstate!=ContainerState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
            int map[] = gb.getColorMap();
            int colorIndex = map[gb.whoseTurn];
     		ContainerChip ship = ContainerChip.getShip(colorIndex);
     		ship.drawChip(gc, this, iconRect, null);
            goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);
         }
        
        drawVcrGroup(ourSelect, gc);
    	drawHiddenWindows(gc,highlight);
 		drawCalculator(gc,highlight,bidCalculatorRect,bidCalculator);

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
		lastDropped = b.lastDroppedObject;
		if(replay!=replayMode.Replay) { playSounds(mm); }
 
        return (true);
    }
     void startBoardAnimations(replayMode replay)
     {
        if(replay!=replayMode.Replay)
     	{	while(b.animationStack.size()>1)
     		{
     		ContainerCell dest = b.animationStack.pop();
     		ContainerCell src = b.animationStack.pop();
     		startAnimation(src,dest,dest.topChip());
     		}
     	}
        	b.animationStack.clear();
     } 
     void startAnimation(ContainerCell from,ContainerCell to,ContainerChip top)
     {	if((from!=null) && (to!=null) && (top!=null))
     	{	double speed = masterAnimationSpeed*speedRect.value;
     		if(speed>0.05)
     		{
      		if(showDevelopInfo())
     		{	from.assertCenterSet();
     			to.assertCenterSet();
     		}
     		
     		// make time vary as a function of distance to partially equalize the runtim of
     		// animations for long verses short moves.
     		double dist = G.distance(from.centerX(), from.centerY(), to.centerX(),  to.centerY());
     		double full = G.distance(0,0,G.Width(boardRect),G.Height(boardRect));
     		double endtime = speed*Math.sqrt(dist/full);
         	int sz = top.isShip() ? G.Width(atSeaRect) : 3*SQUARESIZE/4;
         	Drawable spr = top.isShip() ? to : top;
         	double rot = to.activeAnimationRotation();
     		SimpleSprite newSprite = new SimpleSprite(true,spr,
     				sz,	// use the same cell size as drawSprite would
     				endtime,
             		from.centerX(),from.centerY(),
             		to.centerX(),to.centerY(),rot);
     		newSprite.movement = Movement.SlowIn;
             to.addActiveAnimation(newSprite);
   			addSprite(newSprite);
   			}}
     }
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new ContainerMovespec(st, player));
    }
    


/**
 * prepare to add nmove to the history list, but also edit the history
 * to remove redundant elements, so that indecisiveness by the user doesn't
 * result in a messy replay.
 * This may require that move be merged with an existing history move
 * and discarded.  Return null if nothing should be added to the history
 * One should be very cautious about this, only to remove real pairs that
 * result in a null move.
 * 
 */
    public commonMove EditHistory(commonMove nmove)
    {	// For container, some "moves" do not affect the game record
    	// because they are only used for asynchronous entertainmnet
    	int sz = History.size()-1;
    	if(sz>=0)
    		{ commonMove prev = History.elementAt(sz);
    		  if((((nmove.op==MOVE_ACCEPT) || (nmove.op==MOVE_BUY))
    			  && ((prev.op==MOVE_ACCEPT)||(prev.op==MOVE_BUY)))
    			  ||
    			 (((nmove.op==MOVE_ACCEPT_LOAN) || (nmove.op==MOVE_DECLINE_LOAN))
    	    			  && ((prev.op==MOVE_ACCEPT_LOAN)||(prev.op==MOVE_DECLINE_LOAN)))
    		  	)
    		  {
    			  popHistoryElement();
    		  }
    		}
        switch (nmove.op)
        {
        case MOVE_EPHEMERAL_LOAN_BID:
        case MOVE_EPHEMERAL_AUCTION_BID:
        case MOVE_EPHEMERAL_DECLINE:
        case MOVE_EPHEMERAL_FUND:
        	return(null);
        default: return(super.EditHistory(nmove));
        }
    }

private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_FROM_TO:
    	playASoundClip(light_drop,100);
    	playASoundClip(heavy_drop,100);
    	break;
     case MOVE_PICK:
     case MOVE_PICKC:
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
    	if (hp.hitCode instanceof ContainerId) // not dragging anything yet, so maybe start
        {
       	ContainerId hitObject = (ContainerId)hp.hitCode;
		ContainerCell cell = hitCell(hp);
		ContainerChip chip = (cell==null) ? null : cell.topChip();
		if(chip==null)
		{
	    switch(hitObject)
	    {
	    case Zoom_Slider: break;
        case ShipGoodsLocation:	
        case IslandGoodsLocation:		
        case FactoryGoodsLocation:
        case WarehouseGoodsLocation:
        case ContainerLocation:
          	  if(cell.chipIndex>=0)
        	  { ContainerChip ch = cell.chipAtHitIndex();
        	  	if(ch!=null)
        	  		{String color = ContainerMovespec.D.findUnique(ch.getContainerIndex()+CONTAINER_BLACK);
        	  		PerformAndTransmit("PickC "+ContainerMovespec.D.findUnique(hitObject.IID())+" "+cell.col+" "+cell.row+" "+color);
        	  		}
        	  }
          	  break;
        case MachineLocation:
        case LoanLocation:
        case AtDockLocation:
        case WarehouseLocation:
        case AuctionLocation: 
        case AtSeaLocation:	// we hit the board 
        case AtIslandParkingLocation:		// parking lot at the island
           
         	  if(cell.chipIndex>=0)
         	  { PerformAndTransmit("Pick "+ContainerMovespec.D.findUnique(hitObject.IID())+" "+cell.col+" "+cell.row);
         	  }
         	  break;
         	
	    default:

	    	break;
        }
		}}
    }

    Calculator bidCalculator=null;
 
    public void doBidCalculator(HiddenGameWindow hidden,ContainerBoard.playerBoard bd)
    {	G.Assert(!bd.cannotRebid,"allowed");
    	int cash = bd.cash;
    	Calculator cc =  new Calculator(this,Calculator.StandardButtons);
    	String msg = "";
    	switch(b.board_state)
    	{
    	case FUND_LOAN_STATE:
    		msg = s.get(LoanOffer,STANDARD_LOAN_AMOUNT,cash);
    		break;
    	case REBID_STATE:
    		msg = s.get(RebidOffer,bd.bidAmount(),cash);
    		break;
    	default:
    		msg = s.get(MaxPrice,cash);
    	}
       	cc.setMessage(CalculatorButton.id.Text1,msg);
       	
       	if(hidden!=null)
	    	{
	    	hidden.bidCalculator =cc;	
	    	hidden.repaint(0,"Bid calcularor");
	    	}
    	else {
    		bidCalculator = cc;
    		repaint();
    		}
       	repaint();
    }
    public void drawCalculator(Graphics gc,HitPoint hp,Rectangle r,Calculator calculator)
    {
    	if(calculator!=null)
    	{
    		calculator.draw(gc, r, hp);
    	}
    }
    public boolean handleCalculator(HitPoint hp)
    {	HiddenGameWindow hidden = findHiddenWindow(hp);
		int ind = remoteWindowIndex(hp);
    	Calculator calc = hidden==null ? bidCalculator : hidden.bidCalculator;
    	if(calc!=null && calc.processButton(hp))
    	{
      	if(calc.done)
    		{	boolean cancel = calc.cancel;
    			int bid = (int)calc.value;
    			ContainerBoard.playerBoard bd = null;
    		   	if(ind<0) 
    		   	{ bidCalculator=null; 
    		   	  bd = getCurrentPlayerBoard(b);
    		   	} 
    		   	else 
    		   	{ if(hidden!=null) { hidden.bidCalculator=null;  }
    		   			else { bidCalculator = null; }
    		   	  bd = getPlayerBoard(b,ind);
    		   	}
     		   	
    			G.Assert(!bd.cannotRebid,"allowed");
    			
    	    	int cash = bd.cash;
    	    	if(bid>cash)  	{ cancel = true; } 	 
    	    	switch(b.board_state)
    	    	{
    	    	case FINANCEER_STATE:
    	    		if(bid<STANDARD_LOAN_AMOUNT) { cancel = true; };
    	    		break;
    	    	case REBID_STATE:
    	    		if(bid<bd.bidAmount()) { cancel = true; }
    	    		break;
    	    	default: break;
    	    	}
    	    	if(!cancel)
    	    	{
   	       		resolveBid(bd,bid); 		
    	    	}
    		}
    	repaint();
    	return(true);
    	}
    	return(false);
    }
    
	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging( HitPoint hp)
    {
        int localBadClick = badClick;
        int remoteIndex = remoteWindowIndex(hp);
        //
        // pb is either the current players' board, or the board
        // associated with some player's hidden window.
        //
        ContainerBoard.playerBoard pb = remoteIndex>=0 
        			? getPlayerBoard(b,remoteIndex)
        			: getCurrentPlayerBoard(b);
        ContainerBoard.GuiBoard gui = b.guiBoards[pb.player];
        CellId id = hp.hitCode;
        if(handleCalculator(hp)) {}      
        else if(!(id instanceof ContainerId))
        {
        // this does not use the standard "missed click" logic
        if (id == DefaultId.HitNoWhere)
    		{ 	
      	     localBadClick ++;
    	     switch(badClick++)
    	     {
    	     case 0:	// first time, do nothing
    	    	 break;
    	     case 1:	// second time, just unplace if something is moving
    	    	 if(!reviewMode() && OurMove() && (b.pickedObject!=null))
    	    	 {
    	    	  ContainerCell c = b.getSource();
    	    	  PerformAndTransmit("drop "+ContainerMovespec.D.findUnique(c.rackLocation().IID())+" "+c.col+" "+c.row);
    	    	 }
   	    	  	break;
			default:
				if(remoteIndex>=0){ gui.reset(); }
				else { 
					gui.reset();
					performReset();
					localBadClick=0;
				}
    	     }
    		}
    	else { performStandardActions(hp,false); }
        }
    	else {
        ContainerId hitObject = (ContainerId)id;
		//int state = b.getState();
		ContainerCell cell = hitCell(hp);
		//ContainerChip chip = (cell==null) ? null : cell.topChip();
		ContainerChip picked = b.pickedObject;
		menuIsUp = false;
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", id); 
        case HideInfo:
        	pb.hidden = !pb.hidden;
        	break;
        case SeeCash:
        case Zoom_Slider: break;
        case HitSelectPerspective:
        	selectedPerspective = (selectedPerspective==-1)?hp.row : -1;
        	break;

        case HitDeclineLoanLocation:
        	{
        		PerformAndTransmit("DeclineLoan");
        	}
        	break;
        case HitAcceptLoanLocation:
        	{
        		PerformAndTransmit("AcceptLoan "+hp.row);
        	}
        	break;
        case HitBuyLocation:
        	{	PerformAndTransmit("Buy "+hp.row);
        	}
        	break;
        case HitAcceptLocation:
        	{	PerformAndTransmit("Accept "+hp.row);
        	}
        	break;
        case HitBidLocation:
        	doBidCalculator(findHiddenWindow(hp),pb);
        	break;
        case HitPendingAuctionDone:
        	gui.pendingAuctionDoneDone = true;
        	gui.pendingAuctionDone = false;
        	PerformAndTransmit(gui.ephemeralAuctionMove);
        		break;
        		
        case HitPendingLoanDone:
        	gui.pendingLoanDoneDone = true;
        	gui.pendingLoanDone = false;
    		PerformAndTransmit(gui.ephemeralLoanMove);
    		break;
    		
        case HitFundLoan:
        	gui.pendingLoanMove = "Fund";
        	gui.ephemeralLoanMove = "EFund "+ContainerMovespec.D.findUnique(pb.player);
        	gui.pendingLoanDoneCode = ContainerId.HitFundLoan;
        	gui.pendingLoanDone = true;
        	break;
        case HitDeclineLoan:
        	gui.pendingLoanMove = "Decline";
        	gui.ephemeralLoanMove = "EDecline "+ContainerMovespec.D.findUnique(pb.player);
        	gui.pendingLoanDoneCode = ContainerId.HitDeclineLoan;
        	gui.pendingLoanDone = true;
        	break;
        	
        case ShipGoodsLocation:	
        case IslandGoodsLocation:		
        case FactoryGoodsLocation:
        case WarehouseGoodsLocation:
        case ContainerLocation:
        	  gui.clearPendingAuctionMoves();
        	  gui.clearPendingLoanMoves();
           	  if(picked!=null)
        	  {	PerformAndTransmit("Drop "+ContainerMovespec.D.findUnique(hitObject.IID())+" "+cell.col+" "+cell.row);
        	  }
        	  else
        	  { ContainerChip ch = cell.chipAtHitIndex();
        	    if(ch!=null)
        	    {
        	  	String color = ContainerMovespec.D.findUnique(ch.getContainerIndex()+CONTAINER_BLACK);
        	    PerformAndTransmit("PickC "+ContainerMovespec.D.findUnique(hitObject.IID())+" "+cell.col+" "+cell.row+" "+color);
        	    }
        	  }

        		break;
       case MachineLocation:
       case LoanLocation:
       case AtDockLocation:
       case WarehouseLocation:
       case AuctionLocation: 
       case AtSeaLocation:	// we hit the board 
       case AtIslandParkingLocation:		// parking lot at the island
    	   	  gui.clearPendingAuctionMoves();
    	   	  gui.clearPendingLoanMoves();
        	  if(picked!=null)
        	  {	PerformAndTransmit("Drop "+ContainerMovespec.D.findUnique(hitObject.IID())+" "+cell.col+" "+cell.row);
        	  }
        	  else
        	  { PerformAndTransmit("Pick "+ContainerMovespec.D.findUnique(hitObject.IID())+" "+cell.col+" "+cell.row);
        	  }
        	  break;
        	
        }}
        badClick = localBadClick;
    }

    public String gameType() { return(b.gameType()); }
    public String sgfGameType() { return(Container_SGF); }


    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	int np = G.IntToken(his);			// should be the number of players in the game
    	int ran = G.IntToken(his);			// random key for the game
        b.doInit(token,ran,np);
        adjustPlayers(np%100);	// players is overloaded with a compatability flag
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
    	if(bidPop.selectMenuTarget(target))
    	{	
    		resolveBid(getCurrentPlayerBoard(b),bidPop.value); 		
    		return(true);
    	}
        return(super.handleDeferredEvent(target,command));
     }
    
  void resolveBid(ContainerBoard.playerBoard bd,int amount)
  {	ContainerState vstate = b.getState();
  	ContainerBoard.GuiBoard gui = b.guiBoards[bd.player];
	if(vstate==ContainerState.FINANCEER_STATE) 	// financing a loan
	{ gui.pendingLoanMove = "Bid "+amount; 
	  gui.ephemeralLoanMove = "ELoanBid "+amount+" "+ContainerMovespec.D.findUnique(bd.player);
	  gui.pendingLoanDone = true;
	  gui.pendingLoanBid = amount;
	  gui.ephemeral_loan_showHandDown = true;
	}
	else
	{ gui.pendingAuctionMove = "Bid "+amount; 
	  gui.ephemeralAuctionMove = "EBid "+amount+" "+ContainerMovespec.D.findUnique(bd.player);
	  gui.pendingAuctionDone = true;
	  gui.pendingAuctionBid = amount;
	  gui.ephemeral_auction_showHandDown = true;
	}
  }
  
  //
  // this covers the case where ephemeral activity such as placing bids
  // is undone.  The sponsoring case was that a player took out two loans,
  // prepared a bid using those loans, then undid the load.  This resulted
  // in a bid with more than available cash.
  //
  public void truncateHistory()
  {	  ContainerBoard.playerBoard pb = getCurrentPlayerBoard(b);
	  ContainerBoard.GuiBoard gui = b.guiBoards[pb.player];
	  gui.reset();
	  super.truncateHistory();
  }

/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically
 *  */
    
 public void ViewerRun(int wait)
  {
	 if(OurMove())
		 {
		 ContainerState vstate = b.getState();
		 ContainerBoard.playerBoard pb = getCurrentPlayerBoard(b);
		 ContainerBoard.GuiBoard gui = b.guiBoards[pb.player];
		 if(gui.pendingLoanDoneDone)
		 	{	// priority to loans, which are preemptive
			 	gui.pendingLoanDoneDone = false;
				G.Assert(gui.pendingLoanMove!=null,"move is ready");
				String mm = gui.pendingLoanMove;
				gui.clearPendingLoanMoves();
				PerformAndTransmit(mm);
		 	}
		 else if(((vstate==ContainerState.AUCTION_STATE)||(vstate==ContainerState.REBID_STATE)) 
				 && gui.pendingAuctionDoneDone)
		 	{	// our turn, and an auction is immediately pending
			 	gui.pendingAuctionDoneDone = false;
			 	G.Assert(gui.pendingAuctionMove!=null,"move is ready");
				String mm = gui.pendingAuctionMove;
				gui.clearPendingAuctionMoves();
				PerformAndTransmit(mm);
			 
		 	}
		 }
	 super.ViewerRun(wait);
  }


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new ContainerPlay()); }

    int versionValue = SGF_GAME_VERSION;
    String dateString = "";
    public void ReplayGame(sgf_game ga) 
    	{ versionValue = SGF_GAME_VERSION; 
    	  b.complaints.clear();
    	  super.ReplayGame(ga);
    	  for(int i=0;i<b.complaints.size(); i++)
    	  {
    		  theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,b.complaints.elementAt(i)); 
    	  }
    	}
    
    public boolean replayStandardProps(String name,String value)
    {	nextIntCompatabilityKludge(b,name,value,"Aug 25 2012");
    	return(super.replayStandardProps(name,value));
    }
    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/24/2023
		330 files visited 0 problems
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
            {   b.doInitSpec(value);
            	b.setVersion(versionValue,dateString);
                adjustPlayers(b.nPlayers());
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (parseVersionCommand(name,value,SGF_GAME_VERSION)) 
            	{
            	versionValue = G.IntToken(value);
            	b.setVersion(versionValue,dateString);
            	}
            else if (parsePlayerCommand(name,value)) {}
            else if(replayStandardProps(name,value))
            {
            	if(name.equals(date_property)) 
            		{ dateString = value; 
            		  datePlayed = value;
            		  b.setVersion(versionValue,dateString);
            		}
            }
            else {};
            prop = prop.next;
        }

        if (!"".equals(comments))
        {
            setComment(comments);
        }
    }
    
    public String nameHiddenWindow()
    {	
    	return ServiceName;
    }
    /*
     * support for hidden windows, callback from the remote connection
     * TODO: add "your turn" and current state indicator
     */

    public void drawHiddenWindow(Graphics gc,HitPoint hp,int myIndex,Rectangle bounds)
    {	

    	ContainerState vstate = b.getState();
		ContainerBoard.playerBoard bd = getPlayerBoard(b,myIndex);
		if(bd!=null)
		{
		// reposition and rescale the rectangles we need

		if(remoteViewer<0)
			{
			GC.setColor(gc,Color.lightGray);
			GC.fillRect(gc, bounds);
			}

		Rectangle nameRect = interpolateBox(hiddenViewRect,hiddenNameRect,bounds);
		Font myfont = G.getFont(largeBoldFont(),G.Height(nameRect)/2);
		GC.setFont(gc,myfont);
		{	// player name
			String name = prettyName(myIndex); 
			GC.Text(gc, true, nameRect,Color.black,null,s.get(SeeingCashMessage,name)); 
			ContainerChip ship = ContainerChip.getShip(b.getColorMap()[bd.player]);
			int w = G.Width(nameRect);
			ship.drawChip(gc, this,w/4,G.Left(nameRect)+w-w/8,G.centerY(nameRect),null);	
		}
		
		boolean hidden = bd.hidden;

		// eye icon
		{
		Rectangle eyeRect = interpolateBox(hiddenViewRect,hiddenEyeRect,bounds);
		StockArt icon = hidden ? StockArt.Eye : StockArt.NoEye;
		icon.drawChip(gc,this,eyeRect,hp,ContainerId.HideInfo);
		}
		
		{	// cash
		Rectangle cashRect = interpolateBox(hiddenViewRect,hiddenCashRect,bounds);
		String cash = "$"+bd.cash;
		if(showDevelopInfo()) { cash += " "+(bd.virtual_cash/100); }
		if(!hidden) { GC.Text(gc,true,cashRect,Color.black,null,cash); }
		GC.frameRect(gc,Color.black,cashRect);
		}
		
		{	// net worth
		Rectangle worthRect = interpolateBox(hiddenViewRect,hiddenNetworthRect,bounds);
		if(!hidden) { GC.Text(gc,true,worthRect,Color.blue,null,"="+b.currentScoreForPlayer(bd.player)); }
		GC.frameRect(gc,Color.blue,worthRect);			
		}
		
		{	// secret goal
			Rectangle goalRect = interpolateBox(hiddenViewRect,hiddenGoalRect,bounds);
			ContainerChip goal = hidden 
									? ContainerChip.BLANK_CARD 
									: selectedGoalSet().goals[bd.player];
			drawPlayerCard(gc,goalRect,goal);
		}
		HiddenGameWindow hiddenWindow = findHiddenWindow(hp);
		Calculator calc = hiddenWindow==null ? bidCalculator : hiddenWindow.bidCalculator;
		if(calc!=null)
		{
			Rectangle bidCalcRect = interpolateBox(hiddenViewRect,hiddenCalculatorRect,bounds);
			drawCalculator(gc,hp,bidCalcRect,calc);
		}
		{
		Rectangle bidRect = interpolateBox(hiddenViewRect,hiddenBidRect,bounds);
		Rectangle bidDoneRect = interpolateBox(hiddenViewRect,hiddenDoneRect,bounds);
		Rectangle fundRect = interpolateBox(hiddenViewRect,hiddenFundLoanRect,bounds);
		Rectangle declineRect = interpolateBox(hiddenViewRect,hiddenDeclineLoanRect,bounds);
		drawBidChoiceButtons(gc,b,bd,vstate,hp,
				declineRect,fundRect,bidRect,bidDoneRect);
		}}
    }



    

}
