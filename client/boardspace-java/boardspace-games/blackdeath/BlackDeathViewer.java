package blackdeath;

import static blackdeath.BlackDeathMovespec.*;

import java.awt.*;
import online.common.*;
import java.util.*;

import lib.Random;
import lib.StockArt;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.TextStack;
import lib.Graphics;
import lib.CellId;
import lib.DrawableImage;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

/**
 * 
 * This is intended to be maintained as the reference example how to interface to boardspace.
 * <p>
 * The overall structure here is a collection of classes specific to Hex, which extend
 * or use supporting online.game.* classes shared with the rest of the site.  The top level 
 * class is a Canvas which implements ViewerProtocol, which is created by the game manager.  
 * The game manager has very limited communication with this viewer class, but manages
 * all the error handling, communication, scoring, and general chatter necessary to make
 * the game part of the site.
 * <p>
 * The main classes are:
 * <br>BlackDeathViewer - this class, a canvas for display and mouse handling
 * <br>BlackDeathBoard - board representation and implementation of the game logic
 * <br>BlackDeathMovespec - representation, parsing and printing of move specifiers
 * <br>BlackDeathPlay - a robot to play the game
 * <br>BlackDeathConstants - static constants shared by all of the above.  
 *  <p>
 *  The primary purpose of the BlackDeathViewer class is to do the actual
 *  drawing and to mediate the mouse gestures.  All the actual work is 
 *  done in an event loop, rather than in direct response to mouse or
 *  window events, so there is only one process involved.  With a single 
 *  process, there are no worries about synchronization among processes
 *  of lack of synchronization - both major causes of flakey user interfaces.
 *  <p>
 *  The actual mouse handling is done by the commonCanvas class, which simply 
 *  records the recent mouse activity, and triggers "MouseMotion" to be called
 *  while the main loop is executing.
 *  <p>
 *  Similarly, the actual "update" and "paint" methods for the canvas are handled
 *  by commonCanvas, which merely notes that a paint is needed and returns immediately.
 *  drawCanvas is called in the event loop.
 *  <p>
 *  The drawing methods here combine mouse handling and drawing in a slightly
 *  nonstandard way.  Most of the drawing routines also accept a "HitPoint" object
 *  which contains the coordinates of the mouse.   As objects are drawn, we notice
 *  if the current object contains the mouse point, and if so deposit a code for 
 *  the current object in the HitPoint.  the Graphics object for drawing can be null,
 *  in which case no drawing is actually done, but the mouse sensitivity is checked
 *  anyway.  This method of combining drawing with mouse sensitivity helps keep the
 *  mouse sensitivity accurate, because it is always in agreement with what is being
 *  drawn.
 *  <p>
 *  Steps to clone this hierarchy to start the next game
 *  <li> use eclipse refactor to rename the package for "blackdeath" and for individual files
 *  <li> duplicate the blackdeath start configuration, making a new one for the new game
 *  <li> launch the new game and get it to start, still identical to the old blackdeath in all but name.
 *  	this will probably require a few edits to the init code.
 *  <li> do a cvs update on the original blackdeath hierarchy to get back the original code.
 *  
*/
public class BlackDeathViewer extends CCanvas<BlackDeathCell,BlackDeathBoard> implements BlackDeathConstants, GameLayoutClient
{	static final long serialVersionUID = 1000;
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackgroundColor = new Color(255,230,230);
    private Color rackBackGroundColor = new Color(225,192,182);
    private Color boardBackgroundColor = new Color(220,165,155);
    private String DiceRoll = DICEPATH + "dice-roll-2" + SoundFormat;
    private String DiceShake = DICEPATH + "dice-rattle-2" + SoundFormat;
     
    // private state
    private BlackDeathBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //public Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //public Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle chatRect = addRect("chatRect");
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
   
    //
    // addZoneRect also sets the rectangle as specifically known to the 
    // mouse tracker.  The zones are considered in the order that they are
    // added, so the smaller ones should be first, then any catchall.
    //
    // zones ought to be mostly irrelevant if there is only one board layout.
    //
    private Rectangle chipRect[] = addZoneRect("chip",6);
    private Rectangle cardRects[] = addRect("card",6);
    private Rectangle idRect = addRect("idrect");
    private Rectangle mortalityRect = addRect("mortalityRect");
    private Rectangle rotatedMortalityRect = new Rectangle();
    private Rectangle infoRect = addRect("infoRect");
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	BlackDeathChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = BlackDeathChip.SkullIcon.image;
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
       	int players_in_game = info.getInt(OnlineConstants.PLAYERS_IN_GAME,chipRect.length);
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default
        if(G.debug())
        {	BlackDeathConstants.putStrings();
        }
        
        String type = info.getString(GAMETYPE, BlackDeathVariation.blackdeath.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new BlackDeathBoard(type,players_in_game,randomKey,getStartingColorMap(),BlackDeathBoard.REVISION);
        useDirectDrawing(true);
        doInit(false);
        adjustPlayers(players_in_game);
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
        	// the color determines the first player
        	startFirstPlayer();
        	//
        	// alternative where the first player is just chosen
        	//startFirstPlayer();
 	 		//PerformAndTransmit(reviewOnly?"Edit":"Start P"+first, false,replayMode.Replay);

    	}
    }
 

    double aspects[]= {1.2,1.8,1.5};
    public void setLocalBounds(int x, int y, int width, int height)
    {	setLocalBoundsV(x,y,width,height,aspects);
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
    {
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
        int stateH = fh*2;
    	int minLogW = fh*25;	
    	int vcrw = fh*16;
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// % of space allocated to the board
    			aspect,	// aspect ratio for the board
    			fh*3,	// minimum cell size
    			fh*5,	// maximum cell size
    			0.25	// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);


    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
        boolean rotate = mainW<mainH;
        int nrows = rotate ? 20 : 15;  // b.boardRows
        int ncols = rotate ? 15 : 20;	 // b.boardColumns

        // calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH)/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+stateH+extraH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH;
        int stateX = boardX;
        G.placeRow(stateX+stateH,stateY,boardW-stateH ,stateH,stateRect,noChatRect);
        G.SetRect(idRect, stateX, stateY, stateH, stateH);
        
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	G.SetRect(mortalityRect,
    			(int)(boardX+0.018*boardW),
    			(int)(boardY+0.2*boardH),
    			(int)(boardW*0.14),
    			(int)(boardH*0.35));     
    	G.copy(rotatedMortalityRect,mortalityRect);
    	G.SetRect(infoRect, boardX+(int)(0.4*boardW),boardY+(int)(0.89*boardH),
    			(int)(boardW*0.33),(int)(boardH*0.07));
    	if(rotate)
    	{	
    		contextRotation = -Math.PI/2;
    		G.setRotation(boardRect, -Math.PI/2);
    		G.SetRect(mortalityRect,       			
        			(int)(G.Left(boardRect)+0.018*boardH),
        			(int)(G.Top(boardRect)+0.2*boardW),
        			(int)(boardH*0.14),
        			(int)(boardW*0.35));     
    		G.copy(rotatedMortalityRect,mortalityRect);
    		G.setRotation(rotatedMortalityRect, contextRotation,boardX+boardW/2,boardY+boardH/2);
    	}
    	
  
     	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	int off = (int)(boardW*0.31);
    	G.SetRect(goalRect, boardX+off, boardBottom-(int)(boardH*0.05),boardW-off-stateH,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        centerOnBox();
        return(boardW*boardH);
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
		int chipw = 10*unitsize;
		Rectangle chipR = chipRect[player];
        Rectangle cards = cardRects[pl.boardIndex];
		Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*3 : 0;
        Rectangle box = pl.createRectangularPictureGroup(x+chipw,y,2*unitsize/3);
        int h = G.Height(box);
        G.SetRect(chipR,	x,	y,	chipw,2*unitsize);
        G.SetRect(cards,x,y+2*unitsize,chipw,h);        
        G.SetRect(done,x+chipw+unitsize/2,y+h+unitsize/2,doneW,doneW/2);
    	G.union(box, done,  cards,chipR);
    	pl.displayRotation = rotation;
    	return(box);
    }
 
    // draw a box of spare chips. For blackdeath it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void DrawChipPool(Graphics gc, Rectangle r, commonPlayer pl, HitPoint highlight,HitPoint highlightAll,
    							BlackDeathBoard gb,Hashtable<BlackDeathCell,BlackDeathMovespec>targets)
    {	int player = pl.boardIndex;
        int left = G.Left(r);
        int top = G.Top(r);
        int wid = G.Width(r);
        int hgt = G.Height(r);
        PlayerBoard pb = gb.getPlayerBoard(pl.boardIndex);
        
 
        { // draw the player disease chart
        	
        	
            GC.frameRect(gc, Color.black, r);
            BlackDeathChip chip = gb.getPlayerChip(player);
            BlackDeathChip modChip = gb.getPlayerDisease(player);
       
            BlackDeathChip.PlayerFrame.drawChip(gc, this, r,null);
            Rectangle chipp = new Rectangle(left,top,hgt,hgt);
            Rectangle track = new Rectangle((int)(left+wid*0.205),top,(int)(0.5*wid),hgt-hgt/20);
            Rectangle mod = new Rectangle((int)(left+0.71*wid),top,(int)(0.28*wid),hgt);
            pl.rotateCurrentCenter(pb.chipCell,left+hgt/2, top+hgt/2);
            chip.drawChip(gc,this,chipp,null); 
            BlackDeathChip.Track.drawChip(gc,this,track,null);
            modChip.drawChip(gc, this, mod,highlightAll, BlackDeathId.Eye, (String)null);
            
            // draw various ornaments on the player board
            int chipw = (int)(0.06*wid);
            GC.setFont(gc, largeBoldFont());
            GC.drawOutlinedText(gc, true,
            			(int)(left+0.75*hgt),
            			(int)(top+0.75*hgt),
            			chipw,chipw,Color.yellow,Color.black,""+pb.chipCount);
            double v0x = left+0.208*wid;
            int v0y = (int)(top + 0.034*wid);
            double boxw = (int)(0.07178*wid);
            int boxi = (int)(boxw);
            for(int i=0;i<=6;i++)
            {
            
            { BlackDeathCell cell = pb.virulenceCells[i];          
            Rectangle vr = new Rectangle((int)(v0x+i*boxw),v0y,boxi ,boxi);
            boolean can = targets.get(cell)!=null;

            HitPoint.setHelpText(highlightAll,vr,s.get("Virulence #1",i));
            if(cell.drawStack(gc,this,can ? highlight:null,(int)boxw,G.centerX(vr),G.centerY(vr),0,0,0,null))
            	{
            		highlight.spriteRect = vr;
            		highlight.spriteColor = Color.red;
            	}
           
            }
            {
            BlackDeathCell cell = pb.mortalityCells[i];
            Rectangle mr = new Rectangle((int)(v0x+i*boxw),v0y+boxi,boxi ,boxi);
            boolean can = targets.get(cell)!=null;
            HitPoint.setHelpText(highlightAll,mr,s.get("Mortality #1",i));
            if(cell.drawStack(gc,this,can ? highlight:null,(int)boxw,G.centerX(mr),G.centerY(mr),0,0,0,null))
        	{
        		highlight.spriteRect = mr;
        		highlight.spriteColor = Color.red;
        	}
            
            }}
            
            // draw the cards
            Rectangle cards = cardRects[player];
            int h = G.Height(cards);
            int l = G.Left(cards);
            int t = G.Top(cards);
            int cw = h*2;
            int cx = G.centerX(cards);
            int cy = G.centerY(cards)+cw/30;
            boolean active = pb.temporaryCards.height()>0;
            BlackDeathCell source = gb.getSource();
            boolean can = targets.get(pb.cards)!=null || ((gb.pickedObject!=null) && (pb.cards==source));
            double xstep = 0.4;
            int xp = cx+(active?h:0);
            BlackDeathCell ccards = pb.cards;
            if(ccards.drawStack(gc,this,can||(ccards.height()>0)?highlightAll:null,cw,xp,cy,0,xstep,0,null))
            {	highlightAll.spriteColor = Color.red;
            	highlightAll.spriteRect = new Rectangle((int)(xp-cw/2+highlightAll.hit_index*cw*xstep),cy-cw/4,cw,cw/2);
            	if(!can) 
            		{ highlightAll.hitCode = BlackDeathId.Eye;
            		  highlightAll.hitObject = ccards.chipAtIndex(highlightAll.hit_index);
            		}
            }
            int cardL = l+h+cw/8;
            BlackDeathCell dest = gb.getDest();
            boolean cantemp = targets.get(pb.temporaryCards)!=null || (pb.temporaryCards==dest);
            BlackDeathCell tcards = pb.temporaryCards;
            if(tcards.drawStack(gc, this, cantemp||(tcards.height()>0) ? highlightAll : null, cw,cardL,cy,0,0.4,0,null))
            {
            	highlightAll.spriteColor = Color.red;
            	highlightAll.spriteRect = new Rectangle((int)(cardL-cw/2+highlightAll.hit_index*cw*xstep),cy-cw/4,cw,cw/2);
              	if(!cantemp) 
        		{ highlightAll.hitCode = BlackDeathId.Eye;
        		  highlightAll.hitObject = tcards.chipAtIndex(highlightAll.hit_index);
        		}
            }
            if(targets.get(pb.temporaryCards)!=null)
            {
            	StockArt.SmallO.drawChip(gc, this, cw, cardL-cw/3, cy, null);
            }
            if(active)
            	{
            	StockArt.SolidRightArrow.drawChip(gc, this,cw/4,l,cy,null);
            	}
            // draw a magnifier for the player stuff
            int cs = CELLSIZE/2;
            if(StockArt.Magnifier.drawChip(gc, this, highlightAll, BlackDeathId.Magnify, cs,
            		l, t+cs/2,null))
            	{
            		highlightAll.hitObject = r;
            	}
        	
        }
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
    	BlackDeathChip.getChip(obj).drawChip(g,this,(int)(CELLSIZE*getGlobalZoom()), xp, yp, null);
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
       BlackDeathChip.backgroundTile.image.tileImage(gc, fullRect);   
        drawFixedBoard(gc);
    }
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      if(reviewBackground)
      {	 
       BlackDeathChip.backgroundReviewTile.image.tileImage(gc,brect);   
      }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(bb,brect);
      
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     BlackDeathChip.board.getImage().centerImage(gc,brect);

     }
    public int ScoreForPlayer(commonPlayer p)
    {	return(bb.scoreForPlayer(p.boardIndex));
    }

    public void drawLink(Graphics gc,BlackDeathBoard gb,HitPoint hp,BlackDeathLink link,DrawableImage<?> icon,BlackDeathMovespec m)
    {
    	int fx = gb.cellToX(link.from);
    	int fy = gb.cellToY(link.from);
    	int tx = gb.cellToX(link.to);
    	int ty = gb.cellToY(link.to);
    	GC.setColor(gc, Color.red);
    	GC.drawLine(gc,fx+2,fy+2,tx-2,ty-2);
    	StockArt.LandingPad.drawChip(gc,this,CELLSIZE/2,(fx+tx)/2,(fy+ty)/2,""+link.cost);
    	
    	if(icon!=null)
    	{
    		if(icon.drawChip(gc, this,hp,BlackDeathId.Link, CELLSIZE*3/4,(fx+tx)/2,(fy+ty)/2,null))
    		{
    			hp.hitObject = m;
    		}
    	}
    }
    
    private BlackDeathChip rollDie = null;
    private long rollDieTime = 0;
    private int rollDieX = 0;
    private int rollDieY = 0;
    private BlackDeathChip rollDie2 = null;
    private long rollDie2Time = 0;
    private int rollDieX2 = 0;
    private int rollDieY2 = 0;
    
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, BlackDeathBoard gb, Rectangle brect, HitPoint highlight0,HitPoint highlightAll,Hashtable<BlackDeathCell,BlackDeathMovespec> targets)
    {	int wid = G.Width(brect);
    	int bleft = G.Left(brect);
    	int btop = G.Top(brect);
    	int height = G.Height(brect);
    	BlackDeathCell dest = gb.getDest();
    	BlackDeathState state = gb.getState();
    	BlackDeathState resetState = gb.resetState;
    	boolean linkMode = resetState==BlackDeathState.CloseLinks;
        BlackDeathChip bc = noBigChip 
        		? null 
        		: ((gb.pickedObject!=null) && gb.pickedObject.isCard()) 
    			 	? gb.pickedObject 
    			 	: bigChip;
        // inhibit mouse if showing an overlay card
        HitPoint highlight = bc==null ? highlight0 : null;

        gb.discardPile.drawStack(gc, this, null, (int)(CELLSIZE*1.5),(int)(bleft+0.04*wid),(int)(btop+0.12*wid),
    			0,0.01, 0.01,null);
    	gb.drawPile.drawStack(gc, this, null, (int)(CELLSIZE*1.5),(int)(bleft+0.05*wid),(int)(btop+0.12*wid),
    			0,0.01, 0.01,BlackDeathChip.BACK);
    	
    	for(BlackDeathCell cell = gb.allCells; cell!=null; cell=cell.next)
    	{
         	if(gb.regionIsClosed(cell,BlackDeathState.Puzzle))	// never closed in CloseLinkLink state is what we're looking for
          	{
            	int xpos = gb.cellToX(cell);
              	int ypos = gb.cellToY(cell);
          		BlackDeathChip.Quaranteen.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
          	}
         	if(gb.regionIsPogrom(cell, BlackDeathState.Puzzle))
         	{
            	int xpos = gb.cellToX(cell);
              	int ypos = gb.cellToY(cell);
          		BlackDeathChip.Pogrom.drawChip(gc,this,CELLSIZE*3/4,xpos,ypos,null);        		
         	}
    	}

    	// draw the links explicitly when closed links exist or linkmode
    	gb.sweep_counter++;
    	Hashtable <BlackDeathLink,BlackDeathMovespec>linkTargets = null;
    	if(linkMode)
    		{ linkTargets = new Hashtable<BlackDeathLink,BlackDeathMovespec>();
    		  for( Enumeration<BlackDeathMovespec> k = targets.elements(); k.hasMoreElements();)
    		  {
    			  BlackDeathMovespec m = k.nextElement();
    			  while(m!=null)
    			  {	BlackDeathCell from = gb.getCell(m.source,m.from_name);
    			  	BlackDeathCell to = gb.getCell(m.dest,m.to_name);
    				BlackDeathLink l = gb.getLink(from,to);
    				linkTargets.put(l, m);
    				m = (BlackDeathMovespec)m.next;
    			  }
    		  }
    		}
    	for(BlackDeathCell cell = gb.allCells; cell!=null; cell=cell.next)
    	{	BlackDeathCell parent = cell.parentCity;
    		if(parent.sweep_counter!=gb.sweep_counter)
    		{
    		parent.sweep_counter = gb.sweep_counter;
    		LinkStack links = parent.links;
         	for(int lim=links.size()-1; lim>=0; lim--)
         	{	BlackDeathLink link = links.elementAt(lim);
         		if(linkMode) 
         			{ BlackDeathMovespec m = linkTargets.get(link);
         			  if(m!=null) { drawLink(gc,gb,highlight,link,StockArt.SmallO,m); }
         			}
         		 if(gb.linkIsClosed(link)) {	drawLink(gc,gb,null,link,BlackDeathChip.War,null); }

         	}}
    	}
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
        for(BlackDeathCell cell = gb.allCells; cell!=null; cell=cell.next)
          {	
        	int xpos = gb.cellToX(cell);
          	int ypos = gb.cellToY(cell);
          	// draw contents of cells
        	boolean can = gb.LegalToHitBoard(cell,targets);
        	String msg = null;
           	if(cell.drawStack(gc,this,can ? highlight : null,CELLSIZE/2,xpos,ypos,0,0.3,0.3,msg))
          	{
          		highlight.spriteColor = Color.red;
          		highlight.awidth = CELLSIZE/2;
          	
          	}
          	if(!linkMode && (cell.parentCity.links.size()>0) && targets.get(cell)!=null)
          	{
          		StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
          	}
          	if((cell.topChip()!=null) && ((cell==gb.initialDice1)||(cell==gb.initialDice2)))
          	{	String label = cell.label;
          		if(label!=null)
          		{	GC.setColor(gc, Color.black);
          			GC.setFont(gc, largeBoldFont());
          			GC.Text(gc, s.get(label), xpos+CELLSIZE/3, ypos+CELLSIZE/10);
          		}
          	}
          	switch(state)
          	{
          	default: break;
          	case Roll2:	// roll for virulence and mortality
          		if(cell == gb.initialDice1)
          			{ doDieRoll(gc,xpos,ypos,false); 
          			}
          		else if(cell == gb.initialDice2)
          			{ doDieRoll2(gc,xpos,ypos); 
          			}
          		break;
          	case Mortality:
          		if(cell==gb.mortalityTable) { doDieRoll(gc,xpos,ypos,false); }
          		break;
          	case Roll:
          		if(cell==dest)
          			{ boolean perfect = gb.perfectlyRolling;
          			  doDieRoll(gc,xpos+CELLSIZE/2,ypos+CELLSIZE/2,perfect); 
          			  PlayerBoard pb =gb.getPlayer(gb.whoseTurn);
          			  if(pb.canWinAutomatically(gb.getSource()))
          			  {	 Rectangle r = new Rectangle(xpos+CELLSIZE/2, ypos-CELLSIZE/2, CELLSIZE*2, CELLSIZE/2);
          				 if(GC.handleRoundButton(gc, r,highlight,
          						 s.get(PerfectRollAction),HighlightColor,
          						 perfect?HighlightColor:rackBackGroundColor)
           						 )
          				 {
          					 highlight.hitCode = BlackDeathId.PerfectRoll;
          				 }
          				HitPoint.setHelpText(highlightAll,r,s.get(PerfectRollExplanation,pb.automaticWinLimit()));
          			  }
          			}
          	}
         	/*  	
         	 // show links
         	LinkStack links = cell.links;
         	for(int lim=links.size()-1; lim>=0; lim--)
         	{
         		BlackDeathLink link = links.elementAt(lim);
         		drawLink(gc,gb,link);
         	}
         	// show cells
          	String cost = cell.cost==0 ? "" : " "+cell.cost;
          	StockArt.SmallO.drawChip(gc, this, CELLSIZE*2, xpos, ypos,null);
          	G.drawOutlinedText(gc, true,xpos-CELLSIZE/2, ypos-CELLSIZE/2,
          			CELLSIZE,CELLSIZE/2,Color.yellow,Color.black,
          			cell.name+cost);
 
           	 */
       	
          }
        
        {// draw the body count track
       	int left = bleft+(int)(0.895*wid);
    	int top = btop+(int)(0.332*wid);
    	double boxw = wid*0.0282;
     	int boxi = (int)(wid*0.024);
     	// draw a box for the winning position
     	{
     	int count = BlackDeathBoard.winningBodyCount[gb.players_in_game-1];
    	int col = count>10 ? (count>20 ? 2 : 1) : 0;
    	int row = count>10 ? ((count-1)%10+1) : count;
     	GC.frameRect(gc,Color.yellow,(int)(left+col*boxw-boxw/2),(int)( top-row*boxw-boxw/2),(int)boxw,(int)boxw);
     	}
     	for(PlayerBoard pb : gb.pbs)
        {	
        	int count = pb.bodyCount;     	
        	int col = count>10 ? (count>20 ? 2 : 1) : 0;
        	int row = count>10 ? ((count-1)%10+1) : count;
        	int dx =(int)(0.1*(pb.index/3)*boxi);
        	int dy = (int)(0.1*(pb.index%3)*boxi);
        	pb.chip.drawChip(gc, this, boxi,(int)(left+col*boxw+dx),(int)( top-row*boxw+dy),null);       	
        }

        }
        // draw the turn order track
        {	int turnOrder[] = gb.turnOrder;
    		double boxw = wid*0.0294;
           	int boxi = (int)(wid*0.024);
           	int left = bleft+(int)(0.213*wid);
        	int top = btop+(int)(0.709*wid);
        	int who = gb.whoseTurn;
        	for(int i=0;i<turnOrder.length;i++)
        	{	int thisPlayer = turnOrder[i];
        		PlayerBoard pb = gb.getPlayer(thisPlayer);
        		pb.chip.drawChip(gc,this,thisPlayer==who?boxi*3/2:boxi,left+(int)(i*boxw),top,null);
        	}
        }
        int sh = G.Height(stateRect);
        if(StockArt.Magnifier.drawChip(gc, this, highlightAll, BlackDeathId.MagnifyBoard,
        		sh,G.Right(mortalityRect)-sh/2,G.Bottom(mortalityRect)-sh/2,null))
        	{
        		highlightAll.hitObject = rotatedMortalityRect;
        	}
        if(StockArt.Magnifier.drawChip(gc, this, highlightAll, BlackDeathId.MagnifyBoard,
        		sh,G.Right(boardRect)-sh/2,G.Bottom(boardRect)-sh/2,null))
        	{
        		highlightAll.hitObject = boardRect;
        	}
        GC.frameRect(gc, Color.red, mortalityRect);
        if(bc!=null)
        {	int margin = wid/10;
        	int scrimW = wid-margin*2;
        	Rectangle marginRect = new Rectangle(bleft+margin,btop+margin,scrimW,height-margin*2);
        StockArt.Scrim.image.stretchImage(gc, marginRect);
        	if(bc.drawChip(gc, this, highlightAll, BlackDeathId.Eye, (int)(wid*0.7),G.centerX(brect), G.centerY(brect),null,1,1))
        	{
        		highlightAll.hitObject = null;
        	}

        	if(StockArt.FancyCloseBox.drawChip(gc, this, highlightAll, BlackDeathId.Eye,
        			margin/2,bleft+scrimW+margin-margin/2,btop+margin*3/2,null))
        	{
        		highlightAll.hitObject = null;
        	}


        }
    }
    private BlackDeathChip bigChip = null;
    private boolean noBigChip = false;
    
    public void doDieRoll(Graphics gc,int xpos,int ypos,boolean perfect)
    {
    	long now = G.Date();
  		if(CELLSIZE>0 && now>rollDieTime)
  			{
  			Random r = new Random();
  			int v = 1+ (perfect?0:r.nextInt(6));
 			int dx = perfect?0:(r.nextInt(CELLSIZE/8)-CELLSIZE/16);
  			int dy = perfect?0:(r.nextInt(CELLSIZE/8)-CELLSIZE/16);
  			rollDieX = xpos+dx;
  			rollDieY = ypos+dy;
   			rollDieTime = now+50;
  			rollDie = BlackDeathChip.getDie(v);
  			}
			repaintSprites((int)(rollDieTime-now),"DieRoll");
  		if(rollDie!=null) { rollDie.drawChip(gc, this, CELLSIZE/2,rollDieX,rollDieY,null); }
    } 
    public void doDieRoll2(Graphics gc,int xpos,int ypos)
    {
    	long now = G.Date();
  		if(CELLSIZE>0 && now>rollDie2Time)
  			{
  			Random r = new Random();
  			int v = 1+r.nextInt(6);
 			int dx = r.nextInt(CELLSIZE/8)-CELLSIZE/16;
  			int dy = r.nextInt(CELLSIZE/8)-CELLSIZE/16;
  			rollDieX2 = xpos+dx;
  			rollDieY2 = ypos+dy;
   			rollDie2Time = now+50;
  			rollDie2 = BlackDeathChip.getDie(v);
 			}
			repaintSprites((int)(rollDieTime-now),"DieRoll2");
 		if(rollDie2!=null) { 		rollDie2.drawChip(gc, this, CELLSIZE/2,rollDieX2,rollDieY2,null); }
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
    {  BlackDeathBoard gb = disB(gc);
       BlackDeathState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
   	   if(gc!=null)
   		{
   		// note this gets called in the game loop as well as in the display loop
   		// and is pretty expensive, so we shouldn't do it in the mouse-only case
      
       setDisplayParameters(gb,boardRect);
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
   		Hashtable<BlackDeathCell,BlackDeathMovespec> targets = gb.getTargets();

   		gameLog.redrawGameLog2(gc, nonDragSelect, logRect, Color.blue,boardBackgroundColor,standardBoldFont(),standardPlainFont());

       GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,selectPos,targets);
       GC.unsetRotatedContext(gc,selectPos);
       
       GC.frameRect(gc, Color.blue,rotatedMortalityRect);
    
       boolean planned = plannedSeating();
       
       for(int player=0;player<players.length;player++)
       	{ commonPlayer pl = getPlayerOrTemp(player);
       	  pl.setRotatedContext(gc, selectPos,false);
    	   DrawChipPool(gc, chipRect[player],pl, ourTurnSelect,selectPos,gb,targets);
    	   if(planned && gb.whoseTurn==player)
    	   {
    		   doneButton(gc,gb,0,doneRects[player],buttonSelect);
    	   }
       	   pl.setRotatedContext(gc, selectPos,true);
       	}
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       
       GC.setFont(gc,standardBoldFont());
       
       // draw the board control buttons 

       if (state != BlackDeathState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{doneButton(gc,gb,messageRotation,doneRect,buttonSelect);		
				}
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==BlackDeathState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
        if(gb.escapeState){
        	{	GC.Text(gc,true,infoRect,Color.red,Color.white,s.get(NoKillMessage));
        		GC.frameRect(gc, Color.black, infoRect);
        	}
        }
        // draw the avatars
        standardGameMessage(gc,messageRotation,
            				state==BlackDeathState.Gameover
            					?gameOverMessage()
            					:activeGameMessage(state,gb),
            				state!=BlackDeathState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
        BlackDeathChip id = gb.getPlayerChip(pl.boardIndex);
        id.drawChip(gc, this, idRect,null);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(BlackDeathVictoryCondition),progressRect, goalRect);
            //      DrawRepRect(gc,pl.displayRotation,Color.black,b.Digest(),repRect);
        
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);

    }
    private void doneButton(Graphics gc,BlackDeathBoard gb,double rotation,Rectangle r,HitPoint select)
    {	
    	boolean roll = gb.rollState();
    	if(roll)
    	{
    		if(GC.handleRoundButton(gc, rotation,r,select,s.get(RollAction),HighlightColor, rackBackGroundColor))
    		{
    		 select.hitCode = BlackDeathId.Roll;
    		}
    	}
    	else
    	{
    	if(handleDoneButton(gc,r,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor))
    		{
    		if(gb.escapeState)
    		{select.hitCode = BlackDeathId.Escape;
    		}
    		}
    	}}
    
    private String activeGameMessage(BlackDeathState state,BlackDeathBoard gb)
    {	String msg = state.description;
    	switch(state)
    	{
    	default: 
    		msg = s.get(msg);
    		break;
    	case CloseLinks:
    		msg = s.get(msg,gb.closeLinkPoints);
    		break;
    	case Infection:
    	case FirstInfect:
    	case TradersPlus2:
    		msg = s.get(msg,gb.getInfectionPoints());
    		break;
    	case FirstMovement:
    	case Movement:
    		msg = s.get(msg,gb.getMovementPoints());
    		break;
    	case Kill:
    	case Cure:
    	case CatastrophicKill:
    		msg = s.get(msg,gb.killPoints);
    		break;
    	}
    	return(msg);    	
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
        
        /**
         * animations are handled by a simple protocol between the board and viewer.
         * when stones are moved around on the board, it pushes the source and destination
         * cells onto the animationStck.  startBoardAnimations converts those points into
         * animation sprites.  drawBoardElements arranges for the destination stones, which
         * are already in place, to disappear until the animation finishes.  The actual drawing
         * is done by drawSprites at the end of redrawBoard
         */
        startBoardAnimations(replay,bb.animationStack,CELLSIZE,MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay!=replayMode.Replay) { playSounds(mm); }
       return (true);
    }
     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for blackdeath, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
      * destination chip disappear until the animation is finished.
      * @param replay
      */
//     void startBoardAnimations(replayMode replay)
//     {
//        if(replay!=replayMode.Replay)
//     	{
//     		double full = G.distance(0,0,G.Width(boardRect),G.Height(boardRect));
//        	while(bb.animationStack.size()>1)
//     		{
//     		BlackDeathCell dest = bb.animationStack.pop();
//     		BlackDeathCell src = bb.animationStack.pop();
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
	 case MOVE_ROLL:
	 case MOVE_ROLL2:
		 playASoundClip(DiceRoll,100);
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
        return (new BlackDeathMovespec(st, pl));
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
    	if (hp.hitCode instanceof BlackDeathId)// not dragging anything yet, so maybe start
        {
        BlackDeathId hitObject =  (BlackDeathId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;
        }}
    }
	private void doDropChip(char col,int row)
	{	BlackDeathState state = bb.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state "+state);
		case Puzzle:
		{
		BlackDeathChip mo = bb.pickedObject;
		if(mo==null) { mo=bb.lastPicked; }
		PerformAndTransmit("dropb "+col+" "+row);
		}
		break;
		case Confirm:
		case SelectInfection:
			PerformAndTransmit("dropb "+col+" "+row);
			break;
					                 
		
		}
	}
    private Rectangle centerOnBox = null;
    private void centerOnBox()
    {	Rectangle box = centerOnBox;
    	if(box!=null)
    	{
    	centerOnBox = null;
    	int fullW = getWidth();
    	int fullH = getHeight();
		int x = (int)(G.centerX(box)-fullW/2);
		int y = (int)(G.centerY(box)-fullH/2);
  		setSX(x);
		setSY(y);
    	}
    }
    private Rectangle globalMag = null;
    private void setGlobalMagnifier(Rectangle box,double rotation)
    {	if(getGlobalZoom()<=1) { globalMag = null; }
    	if(box==globalMag) { setGlobalZoom(1,0); globalMag=null; }
    	else if(box!=null)
    	{
    	globalMag = box;
		int qt = G.rotationQuarterTurns(rotation);
		boolean swap = (qt&1)!=0;
		int w = G.Width(box);
		int h = G.Height(box);
		
		int fullh = G.Height(fullRect);
		int fullw = G.Width(fullRect);
		double expand = 0.9;
		double hscale = fullh/((swap?w:h));
		double wscale = fullw/((swap?h:w));
		double ratio = expand*Math.min(wscale, hscale);
		setGlobalZoom(ratio,rotation);
		centerOnBox = box;
		resetBounds();
    	}
    }
    public boolean allowPartialUndo()
    {	if(super.allowPartialUndo())
    	{	
	  	  	int op = getCurrentMoveOp();
	  	  	switch(op)
	  	  	{
	  	  	default: return(true);
	  	  	case MOVE_ROLL:
	  	  	case MOVE_ROLL2:
	  	  		return(false);
	  	  	}
    	}
    	return(false);
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
    	Hashtable<BlackDeathCell,BlackDeathMovespec> targets = bb.getTargets();
       	if(!(id instanceof BlackDeathId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        BlackDeathId hitCode = (BlackDeathId)id;
		BlackDeathState state = bb.getState();
        switch (hitCode)
        {
        default:
        	if (performStandardButtons(hitCode)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " + hitCode);
            }
        	break;
        case Escape:
        	PerformAndTransmit("Escape");
        	break;
        case Eye:
        	if(bb.pickedObject==null)
        	{	noBigChip = false;
        		BlackDeathChip hit = (BlackDeathChip)hp.hitObject;
        		bigChip = (hit==bigChip) ? null : hit; 
        	}
        	else { noBigChip = true; }
        	break;
        case PlayerMortality:
        case PlayerVirulence:
        	{
        	BlackDeathCell hitObject = hitCell(hp);
        	BlackDeathMovespec move = targets.get(bb.getCell(hitObject));
        	PerformAndTransmit(move.moveString());
        	}
        	break;
        case PerfectRoll:
        	PerformAndTransmit("PerfectNext");
        	break;
        case MagnifyBoard:
	    	{
	    	Rectangle r = (Rectangle) hp.hitObject;
	    	setGlobalMagnifier(r,0);
	    	}
        	break;
        case Magnify:
        	{
        	Rectangle r = (Rectangle)hp.hitObject;
        	// rotation perspective for the current player
        	double rotation = getPlayerOrTemp(bb.whoseTurn).displayRotation;
        	setGlobalMagnifier(r,rotation);
        	}
        	break;
        case Roll:
        	{
        	PlayerBoard pb = bb.getCurrentPlayer();
         	boolean perfect = bb.perfectlyRolling;
        	// we do the actual randomization in the UI here.
        	if(perfect)
    		{
    			PerformAndTransmit("perfectroll "+pb.color);
    		}
    		else 
    			{
               	Random ran =  bb.currentRandomSelector();
            	int rv = 1+ran.nextInt(6);
            	if(state==BlackDeathState.Roll2) 			
        		{
        		int rv2 = 1 + ran.nextInt(6);
        		PerformAndTransmit("roll2 "+pb.color+" "+rv+" "+rv2);
        		}
        		else {
        		PerformAndTransmit("roll "+pb.color+" "+rv);
        		}
    			}
        	}
        	break;
        case Cards:
        case TemporaryCards:
        	{
        	BlackDeathCell hitObject = hitCell(hp);
        	if(bb.movingObjectIndex()>0)
        	{
        		PerformAndTransmit("Drop "+hitObject.color+" "+hitCode);
        	}
        	else
        	{	noBigChip = false;
        		PerformAndTransmit("Pick "+hitObject.color+" "+hitCode+" "+hp.hit_index);
        	}}
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
        	{
        	BlackDeathCell hitObject = hitCell(hp);
        	// note that the bb.getCell is needed because the cell
			// may be a cell from a copy of the main board.
        	BlackDeathMovespec move = targets.get(bb.getCell(hitObject));
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state "+state);
			case FirstMovement:
			case EasternMovement:
			case WesternMovement:
			case Movement:
				{
				if(bb.movingObjectIndex()>0)
				{
				PerformAndTransmit(move.moveString());
				}
				else
				{
				noBigChip = false;
				PerformAndTransmit("Pickb "+move.from_name);
				}}
				break;
			case Infection:
			case Cure:
			case Kill:
			case TradersPlus1:
			case TradersPlus2:
			case FirstInfect:
			case InitialPlacement:
			case SelectInfection:
			case CatastrophicKill:
			case Pogrom:
			case CloseRegion:
			case AnyCatastrophicKill:
				if(move!=null) { PerformAndTransmit(move.moveString()); }		
				else {BlackDeathMovespec mm = bb.getDestMove(hitObject);
				if(mm!=null) 
					{ undoToMove(mm);
					}}
				break;
			case Roll:
				{
				BlackDeathMovespec mm = bb.getDestMove(hitObject);
				if(mm!=null)
				{
					undoToMove(mm);
				}
				}
				break;
			case Confirm:
				if(!bb.isDest(hitObject))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					throw G.Error("shouldn't hit a chip in state "+state);
					}
				else {
					BlackDeathMovespec mm = bb.getDestMove(hitObject);
					if(mm!=null) 
						{ undoToMove(mm);
						}
				}
				break;
			case Puzzle:
				noBigChip = false;
				PerformAndTransmit("Pickb "+hitObject.name+" "+hitObject.row);
				break;
			}}
			break;
        case Link:
        	// link mode
        	{
        	BlackDeathMovespec m = (BlackDeathMovespec)hp.hitObject;
        	PerformAndTransmit(m.moveString());
        	}
        	break;
        case EmptyBoard:
        	{
           	BlackDeathCell hitObject = hitCell(hp);
			doDropChip(hitObject.col,hitObject.row);
        	}
			break;
			
  
        }
        }
    }



    private boolean setDisplayParameters(BlackDeathBoard gb,Rectangle r)
    {
      	boolean complete = false;
      	gb.SetDisplayRectangle(r);
      	if(complete) { generalRefresh(); }
      	return(complete);
    }
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     * <p>
     * if complete is true, we definitely want to start from scratch, otherwise
     * only the known changed elements need to be painted.  Exactly what this means
     * is game specific, but for blackdeath the underlying empty board is cached as a deep
     * background, but the chips are painted fresh every time.
     * <p>
     * this used to be very important to optimize, but with faster machines it's
     * less important now.  The main strategy we employ is to paint EVERYTHING
     * into a background bitmap, then display that bitmap to the real screen
     * in one swell foop at the end.
     * 
     * @param gc the graphics object.  If gc is null, don't actually draw but do check for mouse location anyay
     * @param complete if true, always redraw everything
     * @param hp the mouse location.  This should be annotated to indicate what the mouse points to.
     */
  //  public void drawCanvas(Graphics gc, boolean complete,HitPoint hp)
  //  {	
       	//drawFixedElements(gc,complete);	// draw the board into the deep background
   	
    	// draw the board contents and changing elements.
        //redrawBoard(gc,hp);
        //      draw clocks, sprites, and other ephemera
        //drawClocksAndMice(gc, null);
        //DrawArrow(gc,hp);
 //    }
    /**
     * draw any last-minute items, directly on the visible canvas. These
     * items may appear to flash on and off, if so they probably ought to 
     * be drawn in {@link #drawCanvas}
     * @param offGC the gc to draw
     * @param hp the mouse {@link HitPoint} 
     */
  // public void drawCanvasSprites(Graphics offGC,HitPoint hp)
  //  {
  //     DrawTileSprite(offGC,hp); //draw the floating tile we are dragging, if present
       //
       // draw any animations that are in progress
       //
  //     drawSprites(offGC);       
  //  }
    
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
    public String sgfGameType() { return(BlackDeath_SGF); }	// this is the official SGF number assigned to the game

    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int np = G.IntToken(his);	// players always 2
    	long rv = G.IntToken(his);
    	int rev = G.IntToken(his);	// rev does't get used either
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // int rk = G.IntToken(his);
    	// bb.doInit(token,rk);
        bb.doInit(token,rv,np,rev);
        adjustPlayers(np);

    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);

 
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
    
     long diceTime = 0;
     public void ViewerRun(int wait)
       {   switch(bb.getState())
    	 	{
       		default: break;
    	 	case Roll2:
    	 		{
    	 		long now = G.Date();
    	 		if((now-diceTime)>1000)
    	 			{
    	 			diceTime = now;
    	 			playASoundClip(DiceShake,1000);
    	 			}
    	 		}
    	 		break;
    	 	}
           super.ViewerRun(wait);
       }
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
    {  return(new BlackDeathPlay());
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
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
    
    Text gameMoveText[] = null;  
    private Text[] gameMoveText()
    {  
    	//if(gameMoveText==null)
    	{
    	double[] cscale = {1.5,0.7,0,-0.1};
    	double[] dscale = {1.5,0.8,-0.0,-0.15};
    	
    	TextStack texts = new TextStack();
    	for(BlackDeathColor color : BlackDeathColor.values())
    	{
    		texts.push(TextGlyph.create(color.name(),color.chip,this,cscale));
    	}
    	for(int i=1;i<=6;i++)
    	{	BlackDeathChip chip = BlackDeathChip.getDie(i);
    		texts.push(TextGlyph.create("Roll-"+i,chip,this,dscale));
    	}
    	texts.push(TextGlyph.create("catastrophic",BlackDeathChip.SkullIcon,this,cscale));
    	gameMoveText = texts.toArray();
    	}
    	return(gameMoveText);
    }
    public Text censoredMoveText(commonMove m,int idx)
    {
    	Text str = ((BlackDeathMovespec)m).censoredMoveText(this,bb);
    	str.colorize(null,gameMoveText());
    	return(str);
    }
    public Text colorize(String str)
    {
    	return TextChunk.colorize(str,null,gameMoveText());
    }

}

