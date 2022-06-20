package yspahan;

import java.awt.*;
import bridge.Config;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import vnc.VNCService;
import vnc.VNCTransmitter;
import yspahan.YspahanBoard.PlayerBoard;

import java.util.*;

import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.DefaultId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.Slider;
import lib.SoundManager;
import lib.StockArt;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;

/**
 * 
 * Change History
 *
 * TODO: allow camel cards to be played when sending own cubes to the caravan
 * 
 * July 2011 Initial work in progress. 
 *
 */
public class YspahanViewer extends CCanvas<YspahanCell,YspahanBoard> implements YspahanConstants
{
     /**
	 * 
	 */
	static final double INITIAL_ANIMATION_SPEED = 0.7;
    static final String Yspahan_SGF = "Yspahan"; // sgf game name

    
    // file names for jpeg images and masks
    static final String ImageDir = "/yspahan/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BOARD_INDEX = 2;
    static final int CARAVAN_INDEX = 3;
    static final int CARAVAN_3_INDEX = 4;
    static final int PLAYER_TEXTURE_INDEX[] = {5,6,7,8 };
    static final int PLAYER_BLANK_INDEX[] = { 9,10,11,12};
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "board",
    	  "caravan",
    	  "caravan-3",
    	  "green-board",
    	  "yellow-board",
    	  "red-board",
    	  "blue-board",
    	  "green-blank",
    	  "yellow-blank",
    	  "red-blank",
    	  "blue-blank"
    	  };
    static final int LID_INDEX = 0;
    static final int VCR_STOP_INDEX = 1;
    static final int VCR_PLAY_INDEX = 2;
    static final String ImageNames[] = 
    	{ "lid" };
   
    static String DiceSound = "/yspahan/images/dice" + Config.SoundFormat;
    static String ClickSound = "/yspahan/images/click" + Config.SoundFormat;
    static String Sounds[] = { DiceSound, ClickSound };
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackgroundColor = new Color(236,229,213);
    private Color rackBackGroundColor = new Color(215,195,148);
    private Color rackActiveColor = new Color(235,215,168);
    
    private Color reallyDoneColor = new Color(0.7f,0.4f,0.4f);
    private Color redHighlightColor = new Color(0.6f, 0.95f, 0.75f);
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    private Color yMouseColors[] = 
    	{ Color.green, 
    	  Color.yellow, 
    	  Color.red, 
    	  Color.blue};
    private Color yMouseDotColors[] = 
    	{ Color.black, 
    		Color.black, 
    		Color.black, 
    		Color.white};
 
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// background images
    // private state
    private YspahanBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    private static int SUBCELL = 4;	// number of cells in a square
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle lidRect = addRect(".lidRect");
    private Rectangle playerRects[] = addRect(".player",4);
    private Rectangle caravanRect = addRect("caravan");
    private Rectangle cardRect = addRect("cards");
    private Rectangle discardRect = addRect("discards");
    private Rectangle payCamelRect = addRect("payCamel");
    private Rectangle sendCubeRect = addRect("sendCube");
    private Rectangle turnRect = addRect("turnRect");
    private Rectangle goldRect = addRect("goldRect");
    private Rectangle camelRect = addRect("camelRect");
    private Slider speedRect = null;
    private boolean showNetworkStats = false;	// debugging overlay
    public int ScoreForPlayer(commonPlayer pl)
    {	
    	return(b.currentScoreForPlayer(pl.boardIndex));
    	
    }
    // get the player index of the player looking at the UI.  
    // In review or pass-and-play this is the play whose turn it is
    // if multiplayer, it's the main player for this UI, no matter whose turn it is.
    public int getUIPlayerIndex()
    {
    	return(allowed_to_edit|isPassAndPlay 
    			? b.whoseTurn
    			: getActivePlayer().boardIndex ); 
    }
    
    public void preloadImages()
    {	
       	YspahanChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        SoundManager.preloadSounds(Sounds);    
   		textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
    	}
        gameIcon = textures[BOARD_INDEX];
    }


	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	int players_in_game = Math.max(3,info.getInt(OnlineConstants.PLAYERS_IN_GAME,4));
    	super.init(info,frame);

    	YspahanMovespec.s = s;
        speedRect = addSlider(TileSizeMessage,s.get(AnimationSpeed),yrack.Zoom_Slider);
        speedRect.min=0.0;
        speedRect.max=INITIAL_ANIMATION_SPEED*2;
        speedRect.value=INITIAL_ANIMATION_SPEED;
        speedRect.barColor=ZoomColor;
        speedRect.highlightColor = HighlightColor;

        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	YspahanConstants.putStrings();
        }
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        MouseColors  = yMouseColors;
        MouseDotColors = yMouseDotColors;
        b = new YspahanBoard(info.getString(OnlineConstants.GAMETYPE, Yspahan_INIT),
        		randomKey,getStartingColorMap(),players_in_game);
        useDirectDrawing(true);
    	adjustPlayers(players_in_game);
     	doInit(false);
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey,b.players_in_game);						// initialize the board
        if(!preserve_history)
        	{ 
        	speedRect.setValue(INITIAL_ANIMATION_SPEED);
        	startFirstPlayer();
       	}
    }

    int PlayerBoardWidth = 30;
    int PlayerBoardHeight = 12;
    void createPlayerGroup(int x,int y, int cellsize,commonPlayer pl0,  Rectangle r,boolean left)
    {	pl0.displayRotation = 0;
    	Rectangle box = pl0.playerBox;
        Rectangle p0time = pl0.timeRect;
        Rectangle xtime = pl0.extraTimeRect;
        Rectangle p0anim = pl0.animRect;
        Rectangle p0name = pl0.nameRect;
        Rectangle p0pic = pl0.picRect;
        int pbw = PlayerBoardWidth*cellsize;
        int pbh = PlayerBoardHeight*cellsize;
        if(left)
        {
        	G.SetRect(r, x-pbw-CELLSIZE/2,y,pbw,pbh);
        }
        else {
        	G.SetRect(r, x, y-pbw-CELLSIZE*2, pbh, pbw);
        }
        //first player name
        G.SetRect(p0name, x, y+CELLSIZE/2, CELLSIZE * 10, CELLSIZE*2);
        // first player portrait
        G.SetRect(p0pic, x, G.Bottom(p0name), CELLSIZE * 8, CELLSIZE * 8);
        // time display for first player
        int timeH = CELLSIZE*2;
        G.SetRect(p0time, G.Right(p0pic),G.Top(p0pic), CELLSIZE * 6, timeH);
        G.AlignLeft(xtime, G.Bottom(p0time),p0time);
        
        // first player "i'm alive" animation ball
        G.SetRect(p0anim,G.Left(xtime),G.Bottom( xtime),timeH,timeH);
        G.SetHeight(box,-1);
        G.union(box, p0name,p0pic,p0time);
     }

   	int cols = 20;	// the board artwork is almost perfect 2:1 aspect ratio
	int rows = 10;
	int snrows;
	int sncols;
	public int setLocalBoundsSize(int width,int height,boolean wideFormat,boolean tallFormat)
    {
	   	int chatHeight = selectChatHeight(height);
	   	boolean noChat = chatHeight==0;
        boolean plannedSeating = plannedSeating();
        if(plannedSeating&&wideFormat) { return(0); }
	   	sncols = (cols*SUBCELL+(tallFormat ? 4 : 54)); // more cells wide to allow for the aux displays
        snrows = (rows+6)*SUBCELL+(tallFormat ? ((noChat|plannedSeating)?40:56) : 4);  
        int cellw = width / sncols;
        int useheight = (height-(wideFormat|plannedSeating?0:chatHeight));
        int cellh = useheight / snrows;
        CELLSIZE = Math.max(1,Math.min(cellw, cellh));
        SQUARESIZE = 4*CELLSIZE;
        return(SQUARESIZE);
    }
    public void setLocalBoundsWT(int x, int y, int width, int height,boolean wideFormat,boolean tallFormat)
    {	
    	int chatHeight = selectChatHeight(height);
    	boolean noChat = chatHeight==0;
    	int nPlayers = b.nPlayers();
    	int C2 = CELLSIZE/2;
        G.SetRect(fullRect,0,0,width,height);
        // game log.  This is generally off to the right, and it's ok if it's not
        // completely visible in all configurations.
        int breakageW = width - sncols*CELLSIZE;
        int breakageH = height- snrows*CELLSIZE;
        int buttonW = CELLSIZE*10;
        int buttonH = 4*CELLSIZE;
        if(breakageW>2*breakageH) { breakageW = breakageH*2; }
        else { breakageH = breakageW/2; }
        int logHeight = wideFormat&!noChat ? (int)(CELLSIZE*12) : chatHeight;

        boolean plannedSeating = plannedSeating();
        int cx2 = CELLSIZE*2;
        G.SetHeight(speedRect,0);
        if(plannedSeating && !wideFormat && !tallFormat)
        {	int BSS = SQUARESIZE+SQUARESIZE/8;
        	int boardWidth = BSS*cols;
        	int boardHeight = BSS*rows;
        	int blockWidth = 31*CELLSIZE;
        	int blockHeight = 11*CELLSIZE;
        	int stateH = CELLSIZE*3;
        	int stateW = BSS*7;
        	boolean seatingAround = seatingAround();
        	int boardX = (width-boardWidth)/2+(seatingAround?0:BSS);
        	int boardY = (height-boardHeight)/2;
        	G.SetRect(boardRect, boardX, boardY, boardWidth, boardHeight);
        	int boardBottom = boardY+boardHeight;
        	int boardRight = boardX+boardWidth;
        	int caravanW = BSS;
        	int caravanX = boardX-caravanW;
        	commonPlayer pl0 = getPlayerOrTemp(0);
            commonPlayer pl1 = getPlayerOrTemp(1);
            commonPlayer pl2 = getPlayerOrTemp(2);
            commonPlayer pl3 = nPlayers>3 ? getPlayerOrTemp(3) : null;
            int px = width/2+blockWidth/2-blockHeight;
            if(seatingAround)
            {	// seated around all around
            	createPlayerGroup(px,height-blockHeight,CELLSIZE,pl0,playerRects[0],true);
             	createPlayerGroup(-CELLSIZE,height/2+blockWidth/2,CELLSIZE,pl1,playerRects[1],false);
            	pl1.displayRotation= Math.PI/2;
            	createPlayerGroup(px,0,CELLSIZE,pl2,playerRects[2],true);
            	pl2.displayRotation = Math.PI;
            	if(nPlayers==4)
            	{	createPlayerGroup(width-blockHeight-CELLSIZE,height/2+blockHeight,CELLSIZE,pl3,playerRects[3],false);
            		pl3.displayRotation = -Math.PI/2;
            	}
    
            	G.placeStateRow(CELLSIZE,0,stateW,stateH,iconRect,stateRect);
                G.SetRect(goalRect,
                		G.Right(pl0.playerBox)+CELLSIZE,
                		boardBottom,
                		CELLSIZE*37,
                		cx2);
                int vcX = CELLSIZE;
                int vcSize = CELLSIZE*20;
                int vcY = height-vcSize/2;
                      SetupVcrRects(
                 		vcX,
                 		vcY,
                 		vcSize,
                 		vcSize/2);
                 int logX = G.Right(vcrZone)+C2;
                 int logY = boardBottom+CELLSIZE ;
                 G.SetRect(logRect, 
                       		logX ,
                       		logY,
                       		G.Left(playerRects[0])-logX-CELLSIZE,
                       		height-logY-C2);

                 G.SetRect(noChatRect,width-stateH, 0, stateH, stateH);
                 {	int chatX = G.Right(pl2.playerBox)+C2;
                    G.SetRect(chatRect,
                     		chatX,
                     		0,
                     		noChat?0:G.Left(noChatRect)-chatX-C2,boardY);
                     }
                 
                 G.SetRect(payCamelRect,
                		caravanX-buttonW-C2,boardY-buttonH,
     	       			buttonW,buttonH);
                 G.AlignLeft(sendCubeRect, 
                		 	G.Bottom(payCamelRect)+CELLSIZE,
                		 	payCamelRect);

               }
            else
            {	// seating on top and bottom
                createPlayerGroup(width-3*blockHeight/2,height-blockHeight,CELLSIZE,pl0,playerRects[0],true);
                createPlayerGroup(blockWidth+CELLSIZE*1,height-blockHeight,CELLSIZE,pl1,playerRects[1],true);
                if(nPlayers==3) 
                	{ createPlayerGroup(px,0,CELLSIZE,pl2,playerRects[2],true);
                	}
                	else {
                	createPlayerGroup(blockWidth+CELLSIZE*1,0,CELLSIZE,pl2,playerRects[2],true);
                	}
                pl2.displayRotation=Math.PI;
                if(nPlayers==4)
                	{ createPlayerGroup(width-3*blockHeight/2,0,CELLSIZE,pl3,playerRects[3],true);
                	  pl3.displayRotation = Math.PI; 
                	}
                int stateX = (nPlayers==3)? CELLSIZE : G.Right(pl2.playerBox)+BSS;
                G.SetRect(stateRect, stateX+stateH,0,stateW-stateH,stateH);
                G.SetRect(iconRect,stateX,0,stateH,stateH);

                G.SetRect(goalRect,
                		boardX+BSS*5,
                		boardBottom,
                		CELLSIZE*40,
                		cx2);
          
               int vcX = CELLSIZE;
               int vcSize = CELLSIZE*20;
               int vcY = G.Top(pl1.playerBox)+CELLSIZE-vcSize/2;
                     SetupVcrRects(
                		vcX,
                		vcY,
                		vcSize,
                		vcSize/2);
                     
               int logY = G.Top(vcrZone)-BSS*4;
               G.SetRect(logRect, 
                     		CELLSIZE ,
                     		logY ,
                     		caravanX-cx2,
                     		BSS*3);

               G.SetRect(noChatRect,((nPlayers==4)?G.Left(playerRects[3]):width)-stateH, boardY-stateH, stateH, stateH);
               {  int chatX = G.Right(pl2.playerBox)+C2;
               	  int chatY = nPlayers==4?G.Bottom(stateRect):0;
                  G.SetRect(chatRect,
                   		chatX,
                   		chatY,
                   		noChat?0:G.Left(noChatRect)-chatX-C2,boardY-chatY);
                   }

               G.SetRect(payCamelRect,
               		caravanX-buttonW-C2,boardY+cx2,
    	       			buttonW,buttonH);
                G.AlignLeft(sendCubeRect, 
               		 	G.Bottom(payCamelRect)+CELLSIZE,
               		 	payCamelRect);
     
            }
            
            setProgressRect(progressRect,goalRect);
            int goldX = boardRight+CELLSIZE;
            G.SetRect(caravanRect,caravanX,boardY,
            		caravanW,boardHeight);
            
            G.SetRect(goldRect, goldX ,boardY+CELLSIZE*3,3*BSS/2,2*BSS/3);
            
            G.AlignLeft(camelRect,boardBottom-6*CELLSIZE, goldRect);
            
            G.SetRect(cardRect, goldX, G.Bottom(goldRect)+CELLSIZE ,BSS, BSS*2);
            
            G.AlignLeft(discardRect, G.Bottom(cardRect)+CELLSIZE,cardRect);
                    
            // "edit" rectangle, available in reviewers to switch to puzzle mode
            G.SetRect(editRect, goldX,G.Bottom(discardRect)+BSS/2, buttonW,buttonH);
          
            // "done" rectangle, should always be visible, but only active when a move is complete.
            G.AlignLeft(doneRect, G.Bottom(editRect)+CELLSIZE,editRect);

            // friendly warning message
            G.AlignLeft(turnRect,G.Bottom(goalRect),goalRect);

         }
        else
        {
        // first player perspective 
        int boardX = CELLSIZE;
        int boardY = (wideFormat?0:chatHeight)+SQUARESIZE+(tallFormat?cx2:0);
        int boardWidth = (SQUARESIZE * cols + breakageW);
        int boardHeight = (SQUARESIZE * rows + breakageH);
        G.SetRect(boardRect,boardX,boardY,
        		boardWidth ,boardHeight);
        int boardRight = boardX+boardWidth;
        int boardBottom = boardY+boardHeight;
         
        G.SetRect(caravanRect,boardX,boardBottom+cx2,
        		SQUARESIZE*3*nPlayers,SQUARESIZE*2);
        
          
        int zoomW = CELLSIZE*15;
        int stateX = boardX + CELLSIZE;
        int stateH =  (tallFormat ? 5 : 3)*CELLSIZE;
        int stateY = boardY-stateH;
        G.SetRect(noChatRect,boardRight-stateH, stateY, stateH, stateH);
        G.SetRect(speedRect, G.Left(noChatRect)-zoomW-C2, stateY, zoomW,   stateH);
        G.SetRect(stateRect, stateX+stateH,stateY,G.Left(speedRect)-stateX-stateH,stateH);
        G.SetRect(iconRect, stateX,stateY,stateH,stateH);
        int ideal_logwidth = (SQUARESIZE * 20);
        int playerX = tallFormat? boardX+cx2 : boardRight+SQUARESIZE-2*CELLSIZE;
        int playerRight = playerX + 53*CELLSIZE;
        
        boolean ultraWideFormat = wideFormat && (width - playerRight)>(((playerRight-boardRight)*0.5));
        
      
        int playerY = tallFormat 
        		? (boardBottom+CELLSIZE*10) 
        		:  wideFormat ? logHeight+C2 : chatHeight+C2;
        int playerY0 = playerY;
        int ystep = (3*SQUARESIZE-CELLSIZE);;
        commonPlayer pl0 = null;
        int pbx = playerX+PlayerBoardWidth*CELLSIZE+C2;
        for(int i=0;i<nPlayers;i++)
        {	
        	createPlayerGroup(pbx,playerY, CELLSIZE, pl0=getPlayerOrTemp(i),playerRects[i],true);
        	playerY += ystep;
        }
        int rpx = G.Right(pl0.playerBox)+CELLSIZE;
        
        G.SetRect(goldRect, boardRight-(SQUARESIZE*8),G.Top( caravanRect),2*SQUARESIZE,2*SQUARESIZE/3);
        
        G.AlignXY(camelRect,G.Left(goldRect), G.Bottom(goldRect)+CELLSIZE*3, goldRect);
                
        G.SetRect(cardRect, G.Right(goldRect)+cx2, G.Top(goldRect) ,SQUARESIZE, SQUARESIZE*2);
         
        G.AlignXY(discardRect, G.Right(cardRect)+CELLSIZE,G.Top(cardRect) ,cardRect);
 
        if(ultraWideFormat)
        {	// ultra wide format
    	   G.SetRect(chatRect, playerRight, playerY0,
    			   width-playerRight-CELLSIZE,
    			   Math.min(chatHeight,height-playerRight-CELLSIZE));
        }
        else
        {
        int chatY = wideFormat|noChat ? playerY+CELLSIZE : y;
        G.SetRect(chatRect,
        		wideFormat ? playerX : x+C2,
        		chatY,
        		wideFormat ? width-playerX-CELLSIZE : Math.max(boardWidth,width-ideal_logwidth-CELLSIZE),
        			wideFormat&!noChat ? height-chatY-CELLSIZE : chatHeight);
        }
    
        // "edit" rectangle, available in reviewers to switch to puzzle mode
        G.SetRect(editRect, G.Right(discardRect)+2*CELLSIZE,G.Top(discardRect), buttonW,buttonH);
      
        // "done" rectangle, should always be visible, but only active when a move is complete.
        G.AlignXY(doneRect,G.Left(editRect) , G.Bottom(editRect)+CELLSIZE,editRect);
               
        G.SetRect(turnRect,
        		G.Right( doneRect) - SQUARESIZE*6,
        		G.Bottom(doneRect)+C2,
        		SQUARESIZE*6, cx2);
             
        G.SetRect(goalRect,
        		boardX,
        		boardBottom,
        		CELLSIZE*40,
        		cx2);
  
        setProgressRect(progressRect,goalRect);

        //this sets up the "vcr cluster" of forward and back controls.
        int logX = tallFormat 
        			? rpx 
        			: wideFormat ? playerX : G.Right(noChat?boardRect:chatRect)+CELLSIZE/3;
       int vcRow = G.Bottom(doneRect)-CELLSIZE;
       int vcSize = CELLSIZE*20;
       int vcX = 
        		tallFormat 
        			? G.Right(cardRect)
        			: wideFormat|noChat ? boardX : playerX;
       int vcY = tallFormat ? G.Bottom(turnRect)+C2 : wideFormat|noChat ? vcRow : G.Top(editRect);
       G.AlignXY(payCamelRect,
        	       			tallFormat ? vcX-buttonW-CELLSIZE : vcX+vcSize+cx2 ,
        	       			wideFormat ? vcRow : vcY,
        	       			doneRect);
       	G.AlignXY(sendCubeRect, 
        	       			G.Left(payCamelRect)  , 
        	       			G.Bottom(payCamelRect)+CELLSIZE,
        	       			payCamelRect);

         SetupVcrRects(
        		vcX,
        		vcY,
        		vcSize,
        		vcSize/2);
 
        int logY = tallFormat 
        		? G.Bottom(vcrRect)+cx2 
        		: wideFormat&!noChat ? y : G.Top(chatRect);
         G.SetRect(logRect, 
        		logX ,
        		logY ,
        		tallFormat ? width-logX-CELLSIZE : Math.min(ideal_logwidth,width-logX-CELLSIZE),
        		tallFormat|noChat ? height-logY-CELLSIZE : logHeight);

       
        }
        //
        // common configuration rectangles
        //
       	positionTheChat(chatRect,chatBackgroundColor,chatBackgroundColor);

       	int lidW = G.Height(boardRect)*2/3;
        G.SetRect(lidRect, 
        		G.Right(boardRect)-lidW+cx2,
        		G.Top(boardRect) ,
        		lidW,
        		G.Height(boardRect));

        generalRefresh();
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
    	boolean hidden = xp<0;
    	int size = hidden 
    			? SQUARESIZE*HiddenViewWidth/G.Width(playerRects[0]) 
    			: SQUARESIZE ;
    	YspahanChip ch = YspahanChip.getChip(obj%100);// Tiles have zero offset
    	int count = obj/100+1;
    	YspahanCell stack = b.tempCardCell;
    	
    	stack.reInit();
    	if(count>1) 
    		{ 
    		  stack.type = yclass.nullSet;
    		  while(count-- >0) { stack.addChip(ch); }
    		  drawPile(g,b,stack,null,null,size,size*2/3,size,xp,yp,false);
    		}
    		else 
    		{
    		if(ch.type==yclass.cards) { ch=ycard.back.chip; }
    	   	ch.drawChip(g,this,size,xp,yp,null);
    		}
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
      YspahanBoard gb = disB(gc);
      GC.setColor(gc,review ? reviewModeBackground : rackBackGroundColor);
      //G.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     if(remoteViewer<0) { textures[BOARD_INDEX].centerImage(gc, boardRect); }
	    gb.SetDisplayRectangle(boardRect);
	    
      //gb.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

    private void drawPile(Graphics gc,YspahanBoard gb,YspahanCell c,HitPoint highlight,HitPoint anyone,
    		int SQ,int w,int h,int xpos,int ypos,boolean showCount)
    {	Random r = new Random(c.col*(c.rackLocation().ordinal()+1));
    	int nc = Math.max(0,c.stackTopLevel());
    	if(w>0 && h>0)
    	{
    	int sw = w;
        int sh = h;
     	YspahanChip top = c.topChip();
    	boolean moving = gb.pickedObject!=null;
		HitPoint myHit = gb.legalToHitCell(c) ? highlight : null;
		int prev_rx = -1;
		int prev_ry = -1;
		int span = sw/3+sh/3;
		String helpText = c.helpText;
		String count = helpText==null ? "" : s.get0or1(helpText,c.height());
		
		if(moving && G.pointInRect(myHit,xpos-SQ/2,ypos-SQ/2,SQ,SQ))
		{
			highlight.awidth = SQ;
			highlight.arrow = StockArt.DownArrow;
			highlight.hitCode = c.rackLocation;
			highlight.hit_x = xpos;
			highlight.hit_y = ypos;
			highlight.hitObject = c;
			highlight.spriteColor = Color.red;
		}
     	while(nc > 0)
    	{	int rx = r.nextInt(sw)-w/2;
    		int ry = r.nextInt(sh)-h/2;
    		// enforce a wider "random" spread
    		if((Math.abs(rx-prev_rx)+Math.abs(prev_ry-ry))>span)
    		{
    		nc--;
    		prev_rx = rx;
    		prev_ry = ry;
    		if(top==null)
    		{	rx = 0;				// enlarge and center the target if there is no chip to draw
    			ry = 0;
    			SQ = Math.min(w,h);
    		}
    		if(c.drawChip(gc,this,top,myHit,SQ,xpos-rx,ypos-ry,null))
    		{	highlight.awidth = SQ/2;
    			highlight.arrow = moving ? StockArt.DownArrow :StockArt.UpArrow;
    			highlight.spriteColor = Color.red;
    			highlight.setHelpText(count); 
    		}}
      	}
   		if((myHit!=null)&&(gb.pickedObject!=null))
   			{ StockArt.LandingPad.drawChip(gc,this,SQ,1.2,xpos,ypos,null); 
   			}
		if(anyone!=null) 
			{	// fatten up the tool tip targets
				int w2 = w*5/4;
				int h2 = h*5/4;
				HitPoint.setHelpText(anyone,xpos,ypos,w2,h2,count); 
			}
		// leave the sprite center at the center, even if nothing drawn
    	if(showCount) { GC.Text(gc,true,xpos-w/2,ypos+h-h/5,w*2,h/5,Color.black,null,count); }
    	}
		c.setCurrentCenter(xpos+w/2,ypos+h/2);
    }
    public boolean allowPartialUndo()
    {
    	return(super.allowPartialUndo() && !b.getPlayerBoard(b.whoseTurn).hasSeenNewCard);
    }
    private boolean drawHelpStack(Graphics gc,YspahanBoard gb,YspahanCell c,HitPoint highlight,HitPoint anyone,int xpos,int ypos,int size,double yscale)
    {	HitPoint myHit = gb.legalToHitCell(c) ? highlight : null;
    	boolean val = c.drawStack(gc,this,myHit,size,xpos,ypos,0, yscale,null); 
    	boolean moving = gb.pickedObject!=null;
    	if(val)
    		{	highlight.awidth = SQUARESIZE/2;
				highlight.arrow = moving ? StockArt.DownArrow :StockArt.UpArrow;
				highlight.spriteColor = Color.red;
    		}
    	if(c.helpText!=null) 
    		{ Text text = TextChunk.split(s.get0or1(c.helpText,c.height()));
    		  text.colorize(s,icons);
    		  HitPoint.setHelpText(anyone,xpos,ypos,size,size,text); 
    		} 
    	return(val);
    }
    
    private void drawPlayerBoard(Graphics gc,double rotation,boolean hidden,YspahanBoard gb,YspahanBoard.PlayerBoard pb, Rectangle br,HitPoint highlight,HitPoint anyone)
    {	
    	HitPoint high = (allowed_to_edit || (gb.board_state==ystate.PUZZLE_STATE)||(gb.board_state==ystate.GAMEOVER_STATE))
    						?anyone
    						:highlight;
    	boolean seecards = !hidden
    						? pb.showCards
    						: pb.showHiddenWindowCards;
     	if ( seecards) { showCards(gc,rotation,gb,pb,br,high); }
    	else { showMainPlayerBoard(gc,rotation,gb,pb,br,high,anyone); }
    }
    
    private void showCards(Graphics gc,double rotation,YspahanBoard gb,YspahanBoard.PlayerBoard pb, Rectangle br0,HitPoint highlight)
    {	YspahanCell c = pb.pmisc[ypmisc.card.index];
    	int cy = G.centerY(br0);
    	int cx = G.centerX(br0);
    	int map[] = gb.getColorMap();
    	GC.setRotation(gc, rotation, cx, cy);
    	G.setRotation(highlight,rotation,cx,cy);
    	pb.hasSeenNewCard = pb.hasNewCard;
    	Rectangle br = G.copy(null,br0);
    	G.setRotation(br, rotation, cx, cy);

		boolean temp = isTemporaryCard(pb.myIndex);
       	int player = pb.myIndex;
       	int height = c.height();
       	int size = G.Height(br)/4;
       	int dx  = (G.Width(br)-size)/Math.max(1,height);
       	int SZ = (int)Math.min(size*1.6,dx*0.9);
       	HitPoint myHit = ((gb.getState()!=ystate.GAMEOVER_STATE) && gb.legalToHitCell(c)) 
       							? highlight
       							: null;
       	int y  = cy;
       	int x = G.Left(br)+SZ;
    textures[PLAYER_BLANK_INDEX[map[player]]].centerImage(gc, br);
    	pb.setDisplayRectangle(br);
    	
    	for(int i=0; i<height; i++)
    	{	YspahanChip card = temp && (i+1==height) 
    								? YspahanChip.cardBack
    								: c.chipAtIndex(i);
    		if(c.drawChip(gc,this,card,myHit,SZ,x,y,null))
    		{	highlight.row = 1000*i;
    			highlight.awidth = SZ;
    			highlight.spriteColor = Color.red;
    			highlight.arrow = StockArt.UpArrow;
    		}
    		HitPoint.setHelpText(highlight,x,y,SZ,SZ*2,s.get(card.helpText));
    		x += dx;
    	}
    	G.setRotation(highlight,-rotation,cx,cy);
    	GC.setRotation(gc,-rotation, cx, cy);
    }
    private boolean isTemporaryCard(int forplayer)
    {
    	return(b.hasTemporaryCard(forplayer));
    }
    private void showMainPlayerBoard(Graphics gc,double rotation,YspahanBoard gb,YspahanBoard.PlayerBoard pb, Rectangle br0, HitPoint highlight,HitPoint anyone)
    {	int player = pb.myIndex;
    	int cx = G.centerX(br0);
    	int cy = G.centerY(br0);
    	int map[]=gb.getColorMap();
    	Rectangle br = G.copy(null,br0);
    	G.setRotation(br, rotation, cx, cy);
    	GC.setRotation(gc,rotation, cx, cy);
    	G.setRotation(anyone, rotation, cx, cy);	// note anyone and highlight are the same object or null
    	
	textures[PLAYER_TEXTURE_INDEX[map[player]]].centerImage(gc, br);
		pb.setDisplayRectangle(br);
		boolean hidden = remoteWindowIndex(anyone)>=0;
    	int size = Math.min(G.Width(br)/3, G.Height(br))/4;

    	if(player == gb.startPlayer) 
    		{ymisc.firstPlayer.chip.drawChip(gc,this,size,G.Left(br)-size/3,G.centerY(br),null);
    		}
    	for(int i=0;i<pb.buildings.length; i++) 
    	{	YspahanCell c = pb.buildings[i];
     		int xpos = pb.getX(c);
    		int ypos = pb.getY(c);
    		//StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,""+c.row);
    		int screenX = G.rotateX(xpos, ypos, rotation, cx, cy);
    		int screenY = G.rotateY(xpos, ypos, rotation, cx, cy);
    		c.setCurrentCenter(screenX,screenY);
    		drawHelpStack(gc,gb,c,highlight,anyone,xpos,ypos,size,1.0); 
    	}
    	for(int i=0;i<pb.pmisc.length;i++)
    	{	YspahanCell c = pb.pmisc[i];
    		int xpos = pb.getX(c);
    		int ypos = pb.getY(c);
			//StockArt.SmallO.drawChip(gc,this,size,xpos,ypos,""+c.row);
			if(i==ypmisc.cubes.index)
			{	if(!hidden) { drawPile(gc,gb,c,highlight,anyone,size,size*2,size,xpos,ypos,false); }
			}
			else if((i==ypmisc.camel.index) || (i==ypmisc.gold.index))
			{	drawPile(gc,gb,c,hidden?null:highlight,anyone,size,size-size/4,size-size/4,xpos,ypos,hidden);
			}
			else if(i==ypmisc.card.index)
			{	Rectangle r = new Rectangle(xpos-size/2,ypos-size,size,size*2);
				boolean picked = gb.pickedObject!=null;
				if(drawCardStack(gc,gb,c,r,highlight,anyone,true,
						!picked 
							&& !getActivePlayer().spectator
							&& (G.offline()||(allowed_to_edit || hidden || (getUIPlayerIndex()==player)))))
				{	// allow looking at cards at any time, but be careful not to make moves
					// for the other player!
					if((gb.whoseTurn!=player) || (gb.pickedObject==null)) { anyone.hitCode = yrack.HitShowCardsButton; }
				}
				int nogold = pb.buildNoGold;
				int nocam = pb.buildNoCamels;
				int S34 = 3*size/4;
				int S2 = size/2;
				if(nogold>0) 
				{ int xp = xpos+S34;
				  int yp = ypos-S2;
				  ymisc.gold.chip.drawChip(gc,this,S34,xp,yp,null);
				  StockArt.SmallX.drawChip(gc,this,S34,xp,yp,""+nogold);
				}
				if(nocam>0) 
				{ int xp = xpos+S34;
				  int yp = ypos+S2;
				  ymisc.camel.chip.drawChip(gc,this,S34,xp,yp,null);
				  StockArt.SmallX.drawChip(gc,this,S34,xp,yp,""+nocam);
				}
	    		if(hidden)
	    		{
	    			GC.Text(gc,true,xpos-SQUARESIZE,ypos+3*SQUARESIZE/4,SQUARESIZE*2,SQUARESIZE/2,Color.black,null,
	    					s.get0or1(cardHelpText, c.height()));
	    		}

			}
			else if(i==ypmisc.points.index)
			{	GC.setFont(gc,largeBoldFont());
				Rectangle r = new Rectangle(xpos-size/2,ypos-size/2,size,size);
				GC.Text(gc, true, r,	Color.white, Color.black, ""+c.height());
			}
			else 
			{ c.drawStack(gc,this,highlight,size,xpos,ypos,0, 0.15,null); 
			drawHelpStack(gc,gb,c,highlight,anyone,xpos,ypos,size,0.15); 
			}
    		// this makes the animation targets right
    		int screenX = G.rotateX(xpos, ypos, rotation, cx, cy);
    		int screenY = G.rotateY(xpos, ypos, rotation, cx, cy);
    		c.setCurrentCenter(screenX,screenY);

		}
    	G.setRotation(anyone, -rotation, cx, cy);
    	GC.setRotation(gc, -rotation, cx, cy);
    }
    /**
     * if the input rectangle is y-major, generate a rotated rectangle
	 * with the same center.
     * @param br
     * @return a new rectangle
     */
    private Rectangle twistRect(Rectangle br)
    {
    	int w = G.Width(br);
    	int h = G.Height(br);
    	int l = G.Left(br);
    	int t = G.Top(br);
    	if(w<h)
    	{	int w2=w/2;
    		int h2=h/2;
    		int cx = l+w2;
    		int cy = t+h2;
    		br = new Rectangle(cx-h2,cy-w2,h,w);
     	}
    	return(br);
    }
    private void drawCaravanBoard(Graphics gc,YspahanBoard gb, Rectangle br0, int whoseTurn,HitPoint highlight,HitPoint anyone)
    {	int nPlayers = gb.nPlayers();
    	int cx = G.centerX(br0);
    	int cy = G.centerY(br0);
    	Rectangle br = twistRect(br0);
    	int neww = G.Width(br)*4/3;
    	if(br!=br0) { GC.setRotation(gc, -Math.PI/2, cx,cy); neww = (int)(neww*0.9); }

    textures[nPlayers==3?CARAVAN_3_INDEX:CARAVAN_INDEX].centerImage(gc, br);
    	YspahanBoard.Caravan pb = gb.caravan;
    	// correct for the 3 player camel track, which is has rows of 3 instead of 4
    	Rectangle r2 = (nPlayers==3) ? new Rectangle(G.Left(br),G.Top(br),
    			neww,G.Height(br)) : br;
    	pb.setDisplayRectangle(r2);
    	boolean moving =  gb.pickedObject==null;
    	YspahanCell caravan[] = pb.caravan;
    	int lim = caravan.length;
    	if(nPlayers==3) { lim = lim/4*3; }
    	for(YspahanCell c : caravan) 
    	{	int xpos = pb.getX(c);
    		int ypos = pb.getY(c);
    		HitPoint myHigh = gb.legalToHitCell(c) ? highlight : null;
    		//StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,""+c.row);
    		if(c.drawStack(gc,this,c.legalToAdd(gb.pickedObject)?myHigh:null,SQUARESIZE,xpos,ypos,0, 1.0,null))
    		{	highlight.awidth = SQUARESIZE/2;
    			highlight.arrow = moving?StockArt.UpArrow:StockArt.DownArrow;
    			highlight.spriteColor = Color.red;
    		}
    	}
    	if(br0!=br) { GC.setRotation(gc,Math.PI/2,cx,cy); }
    }
    private void drawRowOfDice(Graphics gc,YspahanCell c,YspahanBoard gb,int whoseTurn,HitPoint highlight,HitPoint anyone)
    {
    	
    	int height = c.height();
    	if(height>0)
    	{
		int xpos = gb.getX(c);
		int ypos = gb.getY(c);
		int spacing = SQUARESIZE*7/10;
    	int n=0;
		int ix = xpos-2*spacing;
		int initial_x = ix;
		int iy = ypos-(height>5?(height>10?spacing:spacing/2):0);
		int minx = xpos;
		int miny = ypos;
		int maxx = xpos;
		int maxy = ypos;
    	while (height-->0)
    	{
		YspahanChip top = c.chipAtIndex(height);
		c.drawChip(gc,this,top,SQUARESIZE, ix,iy,null);
		minx = Math.min(minx,ix);
		miny = Math.min(miny,iy);
		maxx = Math.max(maxx,ix);
		maxy = Math.max(maxy,iy);
		ix += spacing;
		n++;
		if(n==5) { n=0; iy += spacing; ix = initial_x; }
		//StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,""+c.row);
    	}
    	minx -= SQUARESIZE/2;
    	miny -= SQUARESIZE/2;
    	maxx = maxx-minx+SQUARESIZE;
    	maxy = maxy-miny+SQUARESIZE/2;
		if(gb.legalToHitCell(c) && G.pointInRect(highlight,minx,miny,maxx,maxy))
			{	highlight.spriteRect = new Rectangle(minx,miny,maxx,maxy);
				highlight.spriteColor = Color.red;
				highlight.hitObject = c;
				highlight.hitCode = c.rackLocation;
			}
		if(c == gb.selectedDice)
		{	GC.frameRect(gc,Color.blue,minx-1,miny-1,maxx+2,maxy+2);
		}

    	}	
    }
    private void drawDiceTower(Graphics gc,YspahanBoard gb, int whoseTurn,HitPoint highlight,HitPoint anyone)
    {	// image is part of the main board
    	YspahanCell tower[] = gb.diceTower;
    	for(YspahanCell c : tower)
    	{
    		drawRowOfDice(gc,c,gb,whoseTurn,highlight,anyone);
    	}
    	for(YspahanCell c : gb.diceTowerExtraGold)
    	{	int xpos = gb.getX(c);
			int ypos = gb.getY(c);
			drawPile(gc,gb,c,highlight,anyone,SQUARESIZE,SQUARESIZE,SQUARESIZE*2/3,xpos,ypos,false);
	   		//StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
    	}
    	for(YspahanCell c : gb.diceTowerExtraCard)
    	{	int xpos = gb.getX(c);
			int ypos = gb.getY(c);
			Rectangle r = new Rectangle(xpos-SQUARESIZE/2,ypos-SQUARESIZE/2,SQUARESIZE,SQUARESIZE);
    		drawCardStack(gc,gb,c,r,highlight,anyone,true,false);
    		//StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
    	}
   	
    }
    private void drawDiceBox(Graphics gc,YspahanBoard gb,int whoseTurn,HitPoint highlight,HitPoint anyone)
    {images[LID_INDEX].centerImage(gc, lidRect);	
    	YspahanBoard.DiceTable table = gb.diceTable;
    	table.setDisplayRectangle(lidRect);
    	YspahanCell dice[] = table.dice;
    	YspahanCell extra[] = table.extraDice;
    	int i=0;
    	for(YspahanCell c : dice) 
    		{	
    		int xpos = table.getX(c);
    		int ypos = CELLSIZE*2+table.getY(c);
    		HitPoint myHit = gb.legalToHitCell(c)?highlight:null;
	   		if(myHit!=null) { StockArt.LandingPad.drawChip(gc,this,SQUARESIZE/2,xpos,ypos,null); }
	   		if(c.drawStack(gc,this,(i<N_DICE)?null:myHit,SQUARESIZE,xpos,ypos,0, 1.0,null))
    		{	highlight.spriteColor = Color.red;
    			highlight.awidth = SQUARESIZE;
    		}
    		HitPoint.setHelpText(anyone,xpos,ypos,SQUARESIZE,SQUARESIZE,s.get("This die will be rolled"));
    		i++;
    		}
    	for(YspahanCell d : extra)
    		{   
     		int xpos = table.getX(d);
    		int ypos = CELLSIZE*2+table.getY(d);
			boolean canPick = gb.goldCount(whoseTurn)>0;
	   		HitPoint myHit = gb.legalToHitCell(d)?highlight:null;
	   		if(myHit!=null) { StockArt.LandingPad.drawChip(gc,this,SQUARESIZE/2,xpos,ypos,null); }
			if(d.drawStack(gc,this,canPick?myHit:null,SQUARESIZE,xpos,ypos,0, 1.0,null))
				{ highlight.spriteColor = Color.red;
				  highlight.awidth = SQUARESIZE;
				}
			HitPoint.setHelpText(anyone,xpos,ypos,SQUARESIZE,SQUARESIZE,s.get("This die will not be rolled"));
    		}
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, YspahanBoard gb, Rectangle r,int whoseTurn, HitPoint highlight,HitPoint anyone)
    {
    	//
        // now draw the contents of the board and anything it is pointing at
        //
    	gb.SetDisplayRectangle(boardRect);
    	ystate myState = gb.getState();
    	Hashtable<YspahanCell,YspahanCell>dests = gb.getDests();
         // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left back-front order so the
        // solid parts will fall on top of existing shadows

        for(YspahanCell c = gb.allCells; c!=null; c=c.next)
        {	int xpos = gb.getX(c);
        	int ypos = gb.getY(c);
        	//StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,""+c.row);
         	HitPoint myHit = gb.legalToHitCell(c) ? highlight : null;
        	if(c.drawStack(gc,this,myHit,SQUARESIZE,xpos,ypos,0, 1.0,null))
        	{// draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
      		highlight.arrow =hasMovingObject(anyone) 
      			? StockArt.DownArrow 
      			: c.height()>0?StockArt.UpArrow:null;
      		highlight.awidth = SQUARESIZE/2;
      		highlight.spriteColor = Color.red;
        	}
        	if(dests.get(c)!=null)
        	{	StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
        	}
        	if(c==gb.selectedCube)
        	{	StockArt.SmallX.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
        	}
         }
        if(myState!=ystate.ROLL_STATE) { drawDiceTower(gc,gb,whoseTurn, highlight,anyone); }
        else { drawDiceBox(gc,gb,whoseTurn,highlight,anyone); }
        
              

    }
    /**
     * draw a stack of cards
     * @param gc
     * @param gb
     * @param realCell
     * @param r
     * @param highlight
     * @param showBacks
     */
    public boolean drawCardStack(Graphics gc,YspahanBoard gb,YspahanCell realCell,Rectangle r,HitPoint highlight,HitPoint anyone,boolean showBacks,boolean canAlwaysHit)
    {	YspahanCell c = realCell;
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	if(cx>=0 && cy>=0)	// avoid capturing the coordinates of hidden windows
    	{
    	realCell.setCurrentCenter(cx,cy);
    	}
    	int height = c.stackTopLevel();	// side effect of checking for an animation
    	if(showBacks)
    	{	YspahanCell tempCell = gb.tempCardCell;
    		tempCell.rackLocation = realCell.rackLocation;
    		tempCell.reInit();
    		tempCell.col = c.col;
    		tempCell.row = c.row;
    		tempCell.type = c.type;
    		for(int lim = height; lim>0; lim--) { tempCell.addChip(ycard.back.chip); }
    		c = tempCell;
    	}
    	
     	HitPoint myHit =  gb.legalToHitCell(realCell)
     							? highlight
     							: canAlwaysHit
     									? anyone
     									:null;	// allow gesture to click on a card stack
     	boolean moused=c.drawStack(gc,this,myHit,G.Width(r),G.centerX(r),G.centerY(r),0,
     			0.5/Math.max(c.height(),1),
     			-0.04,null);
     	if(moused)
    	{	myHit.awidth = G.Width(r);
    		myHit.hitObject = realCell;
    		myHit.setHelpText (s.get(cardHelpText,c.height()));
    		myHit.arrow = gb.pickedObject==null?StockArt.UpArrow:StockArt.DownArrow;
    		myHit.spriteColor = Color.red;
    		myHit.spriteRect = r;
    	}
     	if((gb.pickedObject!=null) && gb.isSource(realCell))
     	{	//G.Text(gc,true,r,Color.blue,null,s.get(c.type.name()));
     		StockArt.LandingPad.drawChip(gc,this,r,null);
     		//G.frameRect(gc,Color.blue,r);
     	}
		if(anyone!=null)
			{ int h = height;
			  String help = s.get(realCell.helpText,h);
			  while(!showBacks && (h-->0))
			  {
				  help += "\n" + s.get(realCell.chipAtIndex(h).helpText);
				  if(c==gb.discards) { h=0; }
			  }
			  HitPoint.setHelpText(anyone,r,help); 
			  }
		return(moused);
    }

    public void drawAuxControls(Graphics gc,YspahanBoard gb,HitPoint highlight,HitPoint anyone)
    {  	
    	drawCardStack(gc,gb,gb.discards,discardRect,highlight,anyone,false,false);
    	drawCardStack(gc,gb,gb.cards,cardRect,highlight,anyone,true,false);
       	drawPile(gc,gb,gb.gold,highlight,anyone,SQUARESIZE,G.Width(goldRect),G.Height(goldRect),
       			G.centerX(goldRect),G.centerY(goldRect),false);
       	drawPile(gc,gb,gb.camels,highlight,anyone,SQUARESIZE,G.Width(camelRect),G.Height(camelRect),
       			G.centerX(camelRect),G.centerY(camelRect),false);
    }
    int gameLogScroll = 0;
    YspahanMovespec logState[] = new YspahanMovespec[2];

    private Text icons[] = {
    		TextGlyph.create("Played","x",StockArt.SolidDownArrow,this,new double[]{1.0,2.0,-0.3,-0.6}),
    		TextGlyph.create("Hoist","xx",YspahanChip.plusCube,this,new double[]{1.0,1.7,0,-0.3}),
    		TextGlyph.create("Plus2","xx",YspahanChip.plus2,this,new double[]{1.0,1.2,0,-0.3}),
    		TextGlyph.create("Card","xx",YspahanChip.cardBack,this,new double[]{1.0,0.7,0,-0.3}),
    		TextGlyph.create("Bag","xx",YspahanChip.rows[0],this,new double[]{1.0,1.5,0,-0.3}),
    		TextGlyph.create("Barrel","xx",YspahanChip.rows[1],this,new double[]{1.0,1.5,0,-0.3}),
    		TextGlyph.create("Chest","xxx",YspahanChip.rows[2],this,new double[]{1.0,1.1,0,-0.3}),
    		TextGlyph.create("Vase","xx",YspahanChip.rows[3],this,new double[]{1.0,1.5,0,-0.3}),
    		TextGlyph.create("Supervisor","xx",YspahanChip.getMiscChip(ymisc.supervisor),this,new double[]{1.0,1.3,0,-0.2}),
    		TextGlyph.create("Camels","xx",YspahanChip.getMiscChip(ymisc.camel),this,new double[]{1.0,1.5,0,-0.3}),
    		TextGlyph.create("Gold","xx",YspahanChip.getMiscChip(ymisc.gold),this,new double[]{1.0,1.5,0,-0.3}),
   };
    public Text censoredMoveText(commonMove m,int idx)
    {
    	String str = ((YspahanMovespec)m).censoredMoveString(logState,b);
    	Text chunk = TextChunk.create(str);
    	chunk.colorize(s,icons);
    	return(chunk);
    }

    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    { 
      YspahanBoard gb = disB(gc);
      int nPlayers = gb.nPlayers();
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      ystate myState = gb.getState();
      boolean ds = gb.DoneState();
      redrawGameLog2(gc,ourSelect, logRect, 
    		  Color.black,rackBackGroundColor,
    		  standardBoldFont(),standardBoldFont());
    
       drawBoardElements(gc, gb, boardRect, gb.whoseTurn, ot,highlight);
       drawCaravanBoard(gc,gb,caravanRect,gb.whoseTurn,ot,highlight);
  
	   YspahanCell cube = gb.selectedCube;
       if((myState==ystate.DESIGNATED_CUBE_STATE)
    		   && cube!=null 
    		   && (ot!=null)
    		   && gb.playerWithColor(cube.topChip())!=gb.whoseTurn)
       {
           	   GC.setFont(gc,largeBoldFont());
    		   GC.Text(gc,true,turnRect,Color.red,null, 
        			   s.get(WarnCubeMessage));
       }
       else if((gb.gameDay%7)==6)
       {   
    	   GC.setFont(gc,largeBoldFont());
    	   GC.Text(gc,true,turnRect,Color.blue,null, s.get((gb.gameDay>14)?"Last Turn" : "Last Turn before Scoring"));
       }

       // draw player card racks on hidden boards.  Draw here first so the animations will target
       // the main screen location which is drawn next.
       drawHiddenWindows(gc, highlight);	

       for(int i=0;i<nPlayers; i++)
    	   { Rectangle r = playerRects[i];
    	     PlayerBoard pb = gb.getPlayerBoard(i);
    	   	 if(players[i]!=null && pb!=null) 
    	   	 	{ drawPlayerBoard(gc,players[i].displayRotation,false,gb,pb,r,(i==gb.whoseTurn)?ot:null,highlight); }
    	   	 // debugging
    	   	if(showNetworkStats)
    	   	{ 
    	   	 HiddenGameWindow hidden = getHiddenWindow(i);
    	     if(hidden!=null)
    	    	 { VNCTransmitter trans = hidden.transmitter;
    	    	   if(trans!=null)
    	    		   {String stats = trans.netConn.stateSummary();
    	    		   GC.Text(gc, false, G.Left(r),G.Top(r),G.Width(r),40,
    	    			   Color.black,Color.white,stats);
    	    		   }
    	    	 }
    	   	}
    	   }
       GC.setFont(gc,standardBoldFont());
       if (myState ==ystate.PAY_CAMEL_STATE)
        {if (GC.handleRoundButton(gc, payCamelRect, 
        		select,s.get("Pay Camel"),
                HighlightColor, rackBackGroundColor))
                {
                select.hitCode = yrack.HitPayCamelButton;
                }
        if (GC.handleRoundButton(gc, sendCubeRect, 
        		select,s.get("Send Cube"),
                HighlightColor, rackBackGroundColor))
                {
                select.hitCode = yrack.HitSendCubeButton;
                }
        	
        }
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation(); 
 
       if (myState != ystate.PUZZLE_STATE)
        {	HitPoint sel = (ds? select : null);
        	boolean endturn = ((myState==ystate.CONFIRM_STATE)||(myState==ystate.BUILD_STATE));
             if (GC.handleRoundButton(gc, doneRect, 
            		sel, s.get((myState==ystate.ROLL_STATE)?"Roll":DoneAction),
                    endturn?redHighlightColor:HighlightColor, 
                    		endturn?reallyDoneColor
                    				:ds ?  rackActiveColor : rackBackGroundColor))
            {	// always display the done button, but only make it active in
            	// the appropriate states
                select.hitCode = GameId.HitDoneButton;
            }
 			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
 			
        }

 		drawPlayerStuff(gc,(myState==ystate.PUZZLE_STATE),ourSelect,
 				HighlightColor,
 				rackBackGroundColor);


 		drawStateMessage(gc,myState,gb,stateRect);
 		PlayerBoard pb = gb.getPlayerBoard(gb.whoseTurn);
    	if(pb!=null) { pb.playerChip().drawChip(gc,this,iconRect,null);}
    	 
        goalAndProgressMessage(gc,ourSelect,"",progressRect, goalRect);
        //speedRect.draw(gc,ourSelect);
        drawAuxControls(gc,gb,ot,ourSelect);
        drawVcrGroup(ourSelect, gc);

    }
    public void drawStateMessage(Graphics gc,ystate myState,YspahanBoard gb,Rectangle r)
    {
        if (gc != null)
        {	String rawstate = myState.stateMsg;
        	int secondCount = gb.supervisorCount();
           	String secondCountString = ""+secondCount;
           	int playerToMove = gb.whoseTurn();
           	int extraSupervisor = gb.extraSupervisorCount();
            boolean over = (myState==ystate.GAMEOVER_STATE);
            boolean addname = !over && (myState!=ystate.PUZZLE_STATE);

        	if(extraSupervisor>0) 
        		{	int mins = Math.max(secondCount-extraSupervisor,1);
        			int maxs = secondCount+extraSupervisor;
        			secondCountString = ""+mins+"-"+maxs;
        		}

        	int firstCount = -1;
        	switch(myState)
        	{
        	case CARD_TRADE_CAMELS_GOLD:
        		if(gb.cardTradeCount>=0) { rawstate += "  -  " + s.get("Traded #1 camels become gold", gb.cardTradeCount); }
        		else { rawstate += "  -  " + s.get("Traded: #1 gold become camels",gb.cardTradeCount); }
        		break;
        	case CARD_SCORE_CAMELS:
        		rawstate += "  -  " + s.get("Traded: #1 camels for #2 points",gb.cardTradeCount,gb.cardTradeCount*2);
        		break;
        	case CARD_SCORE_GOLD:
        		rawstate += "  -  " + s.get("Traded: #1 gold for #2 points",gb.cardTradeCount,gb.cardTradeCount);
        		break;
        	case TAKE_CAMEL_STATE:
        		secondCount=-1;
				//$FALL-THROUGH$
			case THREEWAY_TAKE_CAMEL_STATE:
        		firstCount = gb.takeCamelCount();
        		break;
        	case TAKE_GOLD_STATE:
        		secondCount=-1;
				//$FALL-THROUGH$
			case THREEWAY_TAKE_GOLD_STATE:
        		firstCount = gb.takeGoldCount();
        		break;
        	case PLACE_BAG_STATE:
        	case PLACE_CHEST_STATE:
        	case PLACE_BARREL_STATE:
        	case PLACE_VASE_STATE:
        		secondCountString = "";
        		secondCount = -1;
         		//$FALL-THROUGH$
			case THREEWAY_PLACE_BAG_STATE:
        	case THREEWAY_PLACE_CHEST_STATE:
        	case THREEWAY_PLACE_BARREL_STATE:
        	case THREEWAY_PLACE_VASE_STATE:
	        	firstCount = gb.cubePlacementCount();
	        	break;
 	        default: rawstate = s.get(rawstate);
	        		break;
	        }
        	
        	if(firstCount<0) {}
        	else if(secondCount<=0) 
    			{ rawstate = s.get(rawstate,firstCount); 
    			}
        	else
    			{ rawstate = s.get(rawstate,""+firstCount,secondCountString); 
    			}

        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
        double messageRotation = pl.messageRotation();

     	standardGameMessage(gc,messageRotation,over?gameOverMessage():rawstate,addname,playerToMove,r);
 
        }
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
       
        if(replay!=replayMode.Replay) 
        	{ playSounds(mm);
        	  
        	}
 
        return (true);
    }

     void startBoardAnimations(replayMode replay)
     {	try {
        	if(replay!=replayMode.Replay)
     	{	while(b.animationStack.size()>1)
     		{
     		YspahanCell dest = b.animationStack.pop();
     		YspahanCell src = b.animationStack.pop();
     		YspahanChip top = dest.topChip();
    		if(top.isCard()) { top = ycard.back.chip; }
    		startAnimation(src,dest,top,CELLSIZE*5,0,0);
     		}
     	}}
     	finally {
        	b.animationStack.clear();
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
        return (new YspahanMovespec(st, player));
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
    public commonMove EditHistory(commonMove nmove)
    {	YspahanMovespec mm = (YspahanMovespec)nmove;
    	boolean oksame = (mm.source==yrack.Dice_Tower)
    						||(b.board_state==ystate.DESIGNATE_CUBE_STATE)
    						||(b.board_state==ystate.DESIGNATED_CUBE_STATE);
    	return(EditHistory(nmove,oksame));
    }

   /** 
     * this method is called from deep inside PerformAndTransmit, at the point
     * where the move has been executed and the history has been edited.  It's
     * purpose is to verify that the history accurately represents the current
     * undoInfo of the game, and that the fundamental game machinery is in a consistent
     * and reproducible undoInfo.  Basically, it works by creating a duplicate board
     * resetting it and feeding the duplicate the entire history, and then verifying 
     * that the duplicate is the same as the original board.  It's perfectly ok, during
     * debugging and development, to temporarily change this method into a no-op, but
     * be warned if you do this because it is throwing an error, there are other problems
     * that need to be fixed eventually.
     */
    //public void verifyGameRecord()
    //{	//DISABLE_VERIFY = true;
    //	super.verifyGameRecord();
    //}
    
private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_DONE:
    	YspahanMovespec mm = (YspahanMovespec)m;
    	if(mm.moveInfo!=null)
    		{ playASoundClip(DiceSound,200); 
    		}
    	else {playASoundClip(ClickSound,50); }
    	break;
    case MOVE_BOARD_BOARD:
      	 playASoundClip(light_drop,100);
       	 break;
    case MOVE_PICK:
    	 playASoundClip(light_drop,100);
    	break;
    case MOVE_DROP:
      	 playASoundClip(light_drop,100);
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
        if (hp.hitCode instanceof yrack)// not dragging anything yet, so maybe start
        {
		YspahanCell cell = hitCell(hp);
		YspahanChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    performStandardYspahanAction(hp,true);
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
        if(id == DefaultId.HitNoWhere)
        {	
        	boolean some = false;
        	for(PlayerBoard pb : b.playerBoards) 
			{ some |= pb.showCards;
			  pb.showCards = false; 
			  
			}
        	int rm =remoteWindowIndex(hp);
        	if(rm>=0) 
        		{ PlayerBoard pb = b.getPlayerBoard(rm);
        		  if(pb!=null) { pb.showHiddenWindowCards = false; }
        		}
        	if(!some && !missedOneClick) { missedOneClick = true; }
        	else 
        	{	performReset() ;  
        		missedOneClick = false;
        	}
        }
        else if(!(id instanceof yrack)) { missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	yrack hitObject = (yrack)hp.hitCode;
        switch (hitObject)
        {
        default:
             performStandardYspahanAction(hp,false);
        	break;
         case HitPayCamelButton:
        	 PerformAndTransmit("Move Misc "+((char)('A'+b.whoseTurn))+" "+ypmisc.camel.index+" camels @ -1"); // move Misc C 2 5 Discards @ -1
			//$FALL-THROUGH$
		case HitSendCubeButton:
        	 PerformAndTransmit("Done");
        	 break;

        }}
    }
 
    void performStandardYspahanAction(HitPoint hp,boolean fromDrag)
    {
        int remoteWindow = remoteWindowIndex(hp);
        yrack hitObject = (yrack)hp.hitCode;
        YspahanCell cell = hitCell(hp);
        switch(hitObject)
            {
            case Zoom_Slider: break;
            case HitShowCardsButton:
            	// mystery behavior without the "fromDrag" test.  If this is triggered
            	// by the StartDragging, it will be triggered again by the StopDragging
            	// but this change also changes what will be hit at the same location.
            	if(!fromDrag)
            	{
            	int index = cell.col-'A';
        		PlayerBoard pb = b.getPlayerBoard(index);
            	if(pb!=null)
            		{if(remoteWindow>=0) { pb.showHiddenWindowCards = !pb.showHiddenWindowCards; }
            		else { pb.showCards = !pb.showCards; }
            		}
            	}            	
            	break;
            case Bag_Neighborhood:
            case Chest_Neighborhood:
            case Barrel_Neighborhood:
            case Vase_Neighborhood:
            	if((cell.height()==0)&&(b.pickedObject==null))
            	{
            	// special case, we're allowed to hit an empty souk cell
            	PerformAndTransmit("Move Misc "
            					+ ((char)('A'+b.whoseTurn))+" "+ypmisc.cubes.index+" "
            					+ hitObject.name + " " + cell.col +" "+ cell.row);
            	}
            	else
            	{
            	if(fromDrag || ((b.board_state != ystate.DESIGNATE_CUBE_STATE) && (b.board_state != ystate.DESIGNATED_CUBE_STATE)))
            		{
            		PerformAndTransmit((b.pickedObject==null?"Pick ":"Drop ")+hitObject.name + " " + cell.col +" "+ cell.row);
            		}
            	}
            	break;
            case Misc_Track:
            	if(!b.isDest(cell) && (b.pickedObject==null) && (cell.row==ypmisc.card.index))
            	{	int index = cell.col-'A';
            		PlayerBoard pb = b.getPlayerBoard(index);
            		if(pb!=null)
            		{
            		boolean seeCards = remoteWindow<0 ? pb.showCards : pb.showHiddenWindowCards;
            		if(seeCards)
            		{
            		PerformAndTransmit((b.pickedObject==null?"Pick ":"Drop ")+hitObject.name + " " + cell.col +" "+ cell.row+" "+(hp.row/1000));
            		pb.showCards = pb.showHiddenWindowCards = false;
            		}}
            	}
            	else {
            	YspahanCell dest = null;
            	if((b.pickedObject==null))
            	{	
            		// code a "flick" from player camels or gold when in trade states
            		switch(b.board_state)
            		{
            		default: break;
            		case PREBUILD_STATE:
            			{
            			switch(cell.type)
            				{
            				case camels:	dest = b.camels; break;
            				case gold: dest = b.gold; break;
            				case cards: dest = b.cards; break;
						default:
							break; 
            				}
            			}
            			break;
            		case CARD_SCORE_CAMELS: dest = b.camels; break;
             		case CARD_SCORE_GOLD:	dest = b.gold; break;
             		case CARD_TRADE_CAMELS_GOLD:
             		{
             			YspahanBoard.PlayerBoard pb = b.getPlayerBoard(cell.col-'A');
             			if(pb!=null)
             			{
             			switch(cell.type) 
             			{
             			default: throw G.Error("Not expecting ",cell.type);
             			case gold: dest = pb.pmisc[ypmisc.camel.index]; break;
             			case camels: dest = pb.pmisc[ypmisc.gold.index]; break;
             			}}
              		}
             		}
            		if(dest!=null)
            			{
            			if(!fromDrag)
            				{yrack destrack = dest.rackLocation();
            				PerformAndTransmit("Move "+hitObject.name + " " + cell.col +" "+ cell.row
            						+" -1 "+	destrack.name+" "+dest.col+" "+dest.row);
            				}
            			break;
            			}
            	}
            	if(dest==null) { PerformAndTransmit((b.pickedObject==null?"Pick ":"Drop ")+hitObject.name + " " + cell.col +" "+ cell.row); }
            	}
            	break;
            case Card_Stack:
            	if((b.pickedObject==null) && b.board_state.canFlingCard())
            	{
            		if(!fromDrag)
            		{	PlayerBoard pb = b.getPlayerBoard(b.whoseTurn);
            			if(pb!=null)
            			{
                    	YspahanCell dest = pb.pmisc[ypmisc.card.index];
                    	PerformAndTransmit("Move "+hitObject.name+" "+cell.col+" "+cell.row+" 1 "+dest.rackLocation().name+" "+dest.col+" "+dest.row);
            			}
            		}
            		break;
            	}
            	//$FALL-THROUGH$
            case Discard_Stack:
            case Supervisor_Track:
            case Time_Track:
            case Building_Track:
            case Caravan_Track:            	
            case Dice_Table:
            case Dice_Tower:      
            	PerformAndTransmit((b.pickedObject==null?"Pick ":"Drop ")+hitObject.name + " " + cell.col +" "+ cell.row);
            	break;   
            case Camel_Pool:
            	if((b.pickedObject==null) 
            			&& ((b.board_state==ystate.CARD_SCORE_CAMELS)
            				|| (b.board_state==ystate.CONFIRM_CARD_STATE)
            				|| (b.board_state==ystate.THREEWAY_TAKE_CAMEL_STATE)
            				|| (b.board_state==ystate.TAKE_CAMEL_STATE)))
            	{
            	if(!fromDrag)
            	{
            	// move a camel directly from the pile to the player's pile
            	PlayerBoard pb =  b.getPlayerBoard(b.whoseTurn);
            	if(pb!=null)
            	{
            	YspahanCell dest =pb.pmisc[ypmisc.camel.index];
            	PerformAndTransmit("Move "+hitObject.name+" "+cell.col+" "+cell.row+" 1 "+dest.rackLocation().name+" "+dest.col+" "+dest.row);
             	}}
            	}
            	else
            	{
            	PerformAndTransmit((b.pickedObject==null?"Pick ":"Drop ")+hitObject.name + " " + cell.col +" "+ cell.row);
            	}
            	break;
            case Gold_Pool:
            	if((b.pickedObject==null) 
            			&& ((b.board_state==ystate.THREEWAY_TAKE_GOLD_STATE)
            					|| (b.board_state==ystate.CONFIRM_CARD_STATE)
            					|| (b.board_state==ystate.TAKE_GOLD_STATE)
            					|| (b.board_state==ystate.CARD_SCORE_GOLD)))
            	{
            	if(!fromDrag)
            	{	// move a gold directly from the pile to the player's pile
            		PlayerBoard pb = b.getPlayerBoard(b.whoseTurn);
            		if(pb!=null)
            		{
                	YspahanCell dest = pb.pmisc[ypmisc.gold.index];
                	PerformAndTransmit("Move "+hitObject.name+" "+cell.col+" "+cell.row+" 1 "+dest.rackLocation().name+" "+dest.col+" "+dest.row);
                	//SimpleSprite goldSprite = new SimpleSprite(ymisc.gold.chip,CELLSIZE*5,3.25,cell.current_center_x,cell.current_center_y,dest.current_center_x,dest.current_center_y);
                	//addSprite(goldSprite);
            		}
             	}
            	}
            	else
            	{
               	PerformAndTransmit((b.pickedObject==null?"Pick ":"Drop ")+hitObject.name + " " + cell.col +" "+ cell.row);
            	}
               	break;
            default:
            	throw G.Error("Hit Unknown: %s", hitObject);
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
    	return(""+b.gametype+" "+b.randomKey+" "+b.nPlayers()); 
   }
    public String sgfGameType() { return(Yspahan_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	int rk = G.IntToken(his);
       	int np = G.IntToken(his);
        
    	// make the random key part of the standard initialization,
    	// even though games like checkers probably don't use it.
        b.doInit(token,rk,np);
        adjustPlayers(np);

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
    public SimpleRobotProtocol newRobotPlayer() { return(new YspahanPlay()); }

    public boolean replayStandardProps(String name,String value)
    {	nextIntCompatabilityKludge(b,name,value,"Aug 25 2012");
    	return(super.replayStandardProps(name,value));
    }
    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
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
            	int ran = G.IntToken(st);
            	int np = G.IntToken(st);
	            adjustPlayers(np);
                b.doInit(typ,ran,np);
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

    /*
     * support for hidden windows in pass-n-play mode
     * */
    int HiddenViewWidth = 20*PlayerBoardWidth;
    int HiddenViewHeight = 20*PlayerBoardHeight;

    public String nameHiddenWindow()
    {	return ServiceName;
    }
    public void adjustPlayers(int n)
    {
    	super.adjustPlayers(n);
        if(RpcService.isRpcServer() || VNCService.isVNCServer() || G.debug())
        {
        createHiddenWindows(n,HiddenViewWidth,HiddenViewHeight);
        }
    }

    /*
       * @see online.game.commonCanvas#drawHiddenWindow(Graphics, lib.HitPoint, online.game.HiddenGameWindow)
     */
    public void drawHiddenWindow(Graphics gc,HitPoint hp,int index,Rectangle bounds)
    {	
    	PlayerBoard pb = b.getPlayerBoard(index);
    	if(pb!=null)
    	{
    	int w = G.Width(bounds);
    	int h = G.Height(bounds);
    	if(remoteViewer<0) {textures[BACKGROUND_TILE_INDEX].tileImage(gc, bounds); }
    	drawPlayerBoard(gc,0,true,b,pb,bounds,hp,hp);
    	Rectangle stateR =new Rectangle(G.Left(bounds)+CELLSIZE,G.Top(bounds),w-CELLSIZE,h/10);
    	drawStateMessage(gc,b.getState(),b,stateR);
    	if(showNetworkStats)
    	{
    	// debugging overlay with network status
    	GC.Text(gc, true, bounds, Color.black, null,G.timeString(G.Date()));
    	}}
    }
}

