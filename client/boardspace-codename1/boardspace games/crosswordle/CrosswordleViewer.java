package crosswordle;

import static crosswordle.CrosswordleMovespec.*;


import online.common.*;
import java.util.*;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.FileDialog;
import bridge.FontMetrics;
import bridge.JMenuItem;
import bridge.Platform.Style;
import dictionary.Dictionary;
import dictionary.Entry;
import lib.Graphics;
import lib.BSDate;
import lib.CalculatorButton;
import lib.CellId;
import lib.DateSelector;
import lib.ExtendedHashtable;
import lib.Base64;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.Keyboard;
import lib.KeyboardLayout;
import lib.LFrameProtocol;
import lib.MouseState;
import lib.StringStack;
import lib.TextButton;
import lib.TextContainer;
import lib.SimpleObservable;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.game.sgf.sgf_reader;
import online.search.SimpleRobotProtocol;



/**
 *  Initial work July 2020
 *  
 *   Crosswordle is an original idea based on the "Wordle" craze.  
 *   The basic puzzle is to solve de densely packed crossword grid
*/
public class CrosswordleViewer extends CCanvas<CrosswordleCell,CrosswordleBoard> implements CrosswordleConstants, GameLayoutClient
{	static final long serialVersionUID = 1000;
	static final String Crosswords_SGF = "Crosswordle"; // sgf game name
	boolean useKeyboard = G.isCodename1();
	KeyboardLayout Minimal =new KeyboardLayout(0.085,0.085*3,new String[][]
			{				
		 {"Q","W","E","R","T","Y","U","I","O","P"},
		 {"Halfspace","A","S","D","F","G","H","J","K","L","Ndel"}, 
		 {"Fullspace","Z","X","C","V","B","N","M", "Guess"}});
	
	Keyboard keyboard = null;
	// file names for jpeg images and masks
	static final String ImageDir = "/crosswordle/images/";
     // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color chatBackgroundColor = new Color(235,235,235);
    private Color rackBackGroundColor = new Color(192,192,192);
    private Color boardBackgroundColor = new Color(220,165,155);
    private static long MILLIS_PER_DAY = (1000*24*60);
    private Dictionary dictionary = Dictionary.getInstance();
	private TextContainer inputField = new TextContainer(CrosswordleId.InputField);
	private Rectangle keyboardRect = addRect("keyboard");
	private Rectangle keytextRect = addRect("keytext");
	private Rectangle logoRect = addRect("logo");
	private TextButton hardButton = addButton(
			"hard puzzles",CrosswordleId.ToggleEasy,"use hard puzzles",
    		"easy puzzles",CrosswordleId.ToggleEasy,"use easy puzzles",
    		HighlightColor, rackBackGroundColor,boardBackgroundColor);
	private DateSelector dateRect = 
			(DateSelector)addRect("date",
							new DateSelector(this,s.get(PuzzleFor),new BSDate()));
	
	private String usedLetters = "";
    private CrossKeyboard keys = new CrossKeyboard(this,inputField,keyboardRect,Minimal);
    public Color keyboardTextColor(CalculatorButton button)
    {	char letter = (char)(button.value.ival);
    	int index = usedLetters.indexOf(letter);
    	return (index>=0
    				? Color.red
    				: Color.black);
    }
    
    // private state
    private CrosswordleBoard bb = null; //the board from which we are displaying
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
    private Rectangle guessRect = addRect("guess");
    
    private boolean lockOption = false;
    private Rectangle lockRect = addRect("lock");
    private Rectangle largerBoardRect = addRect(".largerBoard");
    private Rectangle altNoChatRect = addRect("nochat");
    private Rectangle restartRect = addRect("restart");	// not needed for pushfight
    int boardRotation = 0;
    double effectiveBoardRotation = 0.0;
    private Rectangle dupsRect = addRect("duplicates");
    private Rectangle openRect = addRect("openrack");
  
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public void preloadImages()
    {	CrosswordleChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = CrosswordleChip.Icon.image;
    }
    public int ScoreForPlayer(commonPlayer p)
    {
    	return(bb.score[p.boardIndex]);
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
       	int players_in_game = info.getInt(OnlineConstants.PLAYERS_IN_GAME,1);
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        //int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        
        super.init(info,frame);
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	CrosswordleConstants.putStrings();
        }
        
        String type = info.getString(GAMETYPE, CrosswordleVariation.Crosswordle_55.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new CrosswordleBoard(type,players_in_game,dateRect.getTime(),getStartingColorMap(),dictionary,CrosswordleBoard.REVISION);
        //robotGame = sharedInfo.get(exHashtable.ROBOTGAME)!=null;
        // some problems with the animation
        // useDirectDrawing();
        doInit(false);
        adjustPlayers(players_in_game);
        inputField.singleLine = true;
        inputField.addObserver(this);
        if(G.debug()) { saveScreen = myFrame.addAction("Save Board Image",deferredEvents); }

    }
    JMenuItem saveScreen;
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
    /** this is called by the game controller when all players have connected
     * and the first player is about to be allowed to make his first move. This
     * may be a new game, or a game being restored, or a player rejoining a game.
     * You can override or encapsulate this method.
     */


    public double aspects[] = {0.7,1.0,1.4};
    public void setLocalBounds(int x,int y,int w,int h)
    {	setLocalBoundsV(x,y,w,h,aspects);
    }
	/**
	 * this is the main method to do layout of the board and other widgets.  I don't
	 * use swing or any other standard widget kit, or any of the standard layout managers.
	 * they just don't have the flexibility to produce the results I want.  Your mileage
	 * may vary, and of course you're free to use whatever layout and drawing methods you
	 * want to.  However, I do strongly encourage making a UI that is resizable within
	 * reasonable limits, and which has the main "board" object at the left.
	 * <p>
	 *  The basic layout technique used here is to start with a cell which is about the size
	 *  of a board square, and lay out all the other objects relative to the board or to one
	 *  another.  The rectangles don't all have to be on grid points, and don't have to
	 *  be non-overlapping, just so long as the result generally looks good.
	 *  <p>
	 *  When "extraactions" is available, a menu option "show rectangles" works
	 *  with the "addRect" mechanism to help visualize the layout.
	 */ 

    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*12;	
    	int logow = fh*30;
       	int minChatW = Math.min(fh*55,width-fh*2);
       	int vcrw = fh*16;
        int margin = fh/2;
        int buttonW = (G.isCodename1()?8:6)*fh;
        int stateH = fh*2;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			aspect,	// aspect ratio for the board
    			fh*2,	// min cell size
    			fh*2,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChat(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2);
      	layout.placeRectangle(keytextRect,minChatW,minChatW/3+stateH*3,BoxAlignment.Bottom);
    	layout.placeRectangle(logoRect,logow,logow/4,BoxAlignment.Top);
       	int ll = G.Left(logoRect);
    	int lt = G.Top(logoRect);
    	int lw = G.Width(logoRect);	
    	int lh = G.Height(logoRect);
    	G.SetRect(dateRect,ll,lt+lh-stateH*2,lw,stateH*2);
    	G.SetHeight(logoRect,lh-stateH);
    	layout.placeRectangle(logRect,minLogW, minLogW*2, minLogW, minLogW*4,BoxAlignment.Edge,false);
    	//int ll = G.Left(logRect);
    	//int lt = G.Top(logRect);
    	//int lw = G.Width(logRect);	
    	//int lh = G.Height(logRect);
    	//G.SetRect(dateRect,ll,lt,lw,stateH*2);
    	//G.SetRect(logRect,ll,lt+stateH*2,lw,lh-stateH*2);
    	
    	
       	layout.alwaysPlaceDone = false;
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,hardButton,restartRect);
       	layout.alwaysPlaceDone = true;
       	
      	int tl = G.Left(keytextRect);
      	int tt = G.Top(keytextRect);
      	int tw = G.Width(keytextRect);
      	int th = G.Height(keytextRect);
      	G.SetRect(keyboardRect,	tl,tt+stateH*3,tw,th-stateH*3);
      	G.SetRect(guessRect,G.centerX(keytextRect)-stateH*6,tt+stateH/2,stateH*12,stateH*2);    	
      	keys.setBounds(keyboardRect);
       	inputField.setBounds(guessRect);
       	inputField.setFont(G.getFont(largeBoldFont(),stateH*9/5));
       		
       	
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);
       	
       	       	
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main)-stateH*2;
    	
    	// There are two classes of boards that should be rotated. For boards with a strong
    	// "my side" orientation, such as chess, use seatingFaceToFaceRotated() as
    	// the test.  For boards that are noticably rectangular, such as Push Fight,
    	// use mainW<mainH
    	boolean planned = plannedSeating();
    	int nrows =  bb.nrows;
        int ncols =  bb.ncols;
  	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW+(planned?stateH:0);
    	int boardY = mainY+extraH+(planned?stateH:0);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
    	int stripeW = CELLSIZE;
    	G.placeRow(stateX,stateY,boardW ,stateH,stateRect,lockRect,altNoChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	G.SetRect(goalRect, boardX, G.Bottom(boardRect),boardW,stateH);   
    	G.SetRect(largerBoardRect,boardX-stateH,boardY-stateH,boardW+stateH*2,boardH+stateH*2);

    	int stripeLeft = G.Right(largerBoardRect)-stripeW-CELLSIZE/3;
    	int stripeTop = boardY+boardH-9*stripeW;
    	G.SetRect(dupsRect,stripeLeft,stripeTop,stripeW,stripeW);
       	stripeTop += stripeW+stateH/2;
    	G.SetRect(openRect,stripeLeft,stripeTop,stripeW,stripeW);
  

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        labelFont = largeBoldFont();
        return(boardW*boardH);
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
     	Rectangle box =  pl.createRectangularPictureGroup(x,y,unitsize);
    	Rectangle done = doneRects[player];
    	
    	int doneW = plannedSeating()? unitsize*4 : 0;
    	int donel = G.Right(box)+unitsize/2;
    	G.SetRect(done,donel,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.union(box, done);
    	pl.displayRotation = rotation;
    	return(box);
    }


 
    // return the player whose chip rect this HitPoint is in
    // considering the rotation of the player block
    
    public commonPlayer inPlayerBox(HitPoint hp)
    {	
    	for(int lim = nPlayers()-1; lim>=0; lim--)
    	{ 
    	commonPlayer pl =getPlayerOrTemp(lim);
    	if((pl!=null) && pl.inPlayerBox(hp)) { return(pl); }
    	}
    	return(null);
    }

    public int getMovingObject(HitPoint highlight)
    {	// censor the identity of the moving object for other players
        if (OurMove())
        {	int mo = bb.movingObjectIndex();
            if((mo>=0)&&!allowed_to_edit) { mo = CrosswordleChip.Blank.chipNumber(); }
            return(mo);
        }
        return (NothingMoving);
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
       	// hidden windows have x coordinates that are negative, we don't want to rotate tiles
       	// being displayed on hidden windows
       	if(xp>=-100) { GC.setRotation(g, effectiveBoardRotation,xp,yp); }
       	drawPrerotatedSprite(g,obj,xp,yp);
       	if(xp>=-1000) { GC.setRotation(g,effectiveBoardRotation,xp,yp); }
    }
    public void drawPrerotatedSprite(Graphics g,int obj,int xp,int yp)
    { 
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
       	CrosswordleChip ch = CrosswordleChip.getChip(obj);
       	if(ch!=null)
       		{
       		// hidden windows have x coordinates that are negative, we don't want to rotate tiles
       		// being displayed on hidden windows
       		g.setColor(Color.black);
       		ch.drawChip(g,this,CELLSIZE, xp, yp, null);  
       		}
   }
    /**
     * if the sprite is inside one of the player racks, display it with that racks's 
     * orientation.
     */
    public void drawRotatedSprite(Graphics gc,int chip,HitPoint hp,Point pt)
    {
    	commonPlayer pl = inPlayerBox(hp);
    	if(pl!=null)
    	{	int left = G.Left(pt);
   			int top = G.Top(pt);
  			GC.setRotation(gc,pl.displayRotation,left,top);
       		drawPrerotatedSprite(gc,chip,left,top);
       		GC.setRotation(gc,-pl.displayRotation,left,top);
    	}
    	else { super.drawRotatedSprite(gc, chip,  hp, pt); }
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
    { 
     CrosswordleChip.backgroundTile.image.tileImage(gc, fullRect);   
      GC.setRotatedContext(gc,largerBoardRect,null,effectiveBoardRotation);
      drawFixedBoard(gc,boardRect);
      GC.unsetRotatedContext(gc,null);  
     }
    public void drawFixedElements(Graphics gc,boolean complete)
    {	commonPlayer pl = getPlayerOrTemp(bb.whoseTurn);
    	if(!lockOption) { effectiveBoardRotation = (boardRotation*Math.PI/2+pl.displayRotation); }
    	super.drawFixedElements(gc,complete);
    }

    // land here after rotating the board drawing context if appropriate
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	
        boolean reviewBackground = reviewMode()&&!mutable_game_record;
        if(reviewBackground)
        {	 
         CrosswordleChip.backgroundReviewTile.image.tileImage(gc,brect);   
        }
	  	// drawing the empty board requires detailed board coordinate information
	  	// games with less detailed dependency in the fixed background may not need
	  	// this. 
	  	setDisplayParameters(bb,brect);
	      // if the board is one large graphic, for which the visual target points
	      // are carefully matched with the abstract grid
	      //G.centerImage(gc,images[BOARD_INDEX], brect,this);
	  	if(remoteViewer<0)
	  	{
	      // draw a picture of the board. In this version we actually draw just the grid
	      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
	      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
	      // on the board to fine tune the exact positions of the text
	      bb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

	      // draw the tile grid.  The positions are determined by the underlying board
	      // object, and the tile itself if carefully crafted to tile the pushfight board
	      // when drawn this way.  For games with simple graphics, we could use the
	      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
	      // but for more complex graphics with overlapping shadows or stacked
	      // objects, this double loop is useful if you need to control the
	      // order the objects are drawn in.
	      for(CrosswordleCell c = bb.allCells; c!=null; c=c.next)
	      {
	    	  int ypos = G.Bottom(brect) - bb.cellToY(c.col, c.row);
	    	  int xpos = G.Left(brect) + bb.cellToX(c.col, c.row);
	    	  int ind = c.row&1|c.col&2;
	    	  CrosswordleChip.Letter[ind].drawChip(gc,this,CELLSIZE*2/3,xpos,ypos,null);  
	    	  //GC.Text(gc,""+ind,xpos,ypos);
	      }     
	  	}
	//      draw
    }


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
    {
 
    	return(super.encodeScreenZone(x,y,p));
    }
    

	CrosswordleCell definitionCell = null;
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, CrosswordleBoard gb, Rectangle brect, HitPoint highlight,HitPoint all)
    {	
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  

        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
        CrosswordleCell closestCell = gb.closestCell(all,brect);
        definitionCell = null;
        if(closestCell!=null && 
        		((closestCell.col=='A') || closestCell.row==bb.ncols))
        	{ definitionCell = closestCell; 
        	}

        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
        {
        Enumeration<CrosswordleCell>cells = gb.getIterator(Itype.RLTB);
		while(cells.hasMoreElements())
          { CrosswordleCell cell = cells.nextElement();
         	int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            String msg = cell.color.chip.overlay;

            cell.drawStack(gc,this,null,CELLSIZE,xpos,ypos,1,1,msg);
            //if(G.debug() && (gb.endCaps.contains(cell) || gb.startCaps.contains(cell)))
            //	{ StockArt.SmallO.drawChip(gc, this, CELLSIZE,xpos,ypos,null); 
            //	}
            }
        }
        if(definitionCell!=null)
        {
        	drawDefinition(gc,gb,all);
        }
    }
     public void drawDefinition(Graphics gc,CrosswordleBoard gb,HitPoint hp)
    {	
    	CrosswordleCell target = definitionCell;
    	StringStack words = new StringStack();
    	if(target.col=='A') 
    		{ String hword = bb.collectWord(target,CrosswordleBoard.CELL_RIGHT());
    		  if(hword!=null) { words.push(hword); }
    		}
    	if(target.row==bb.nrows) 
    		{ String vword = bb.collectWord(target,CrosswordleBoard.CELL_DOWN());
    		  if(vword!=null) { words.push(vword); } 
    		} 
    	
    	StringBuilder message = new StringBuilder();
     	FontMetrics fm = G.getFontMetrics(standardPlainFont());
    	int targetWidth = G.Width(boardRect)/2;
    	if(target!=null && (words.size()>0) && hp!=null)
    	{	for(int lim=words.size()-1; lim>=0; lim--)
    		{
    		String word = words.elementAt(lim);
    			{
    			Entry e = dictionary.get(word);
    			if(e!=null)
    				{
    				message.append(word);
    				message.append(": ");
    				String def = e.getDefinition();
    				if(def!=null)
    				{
    				String split = s.lineSplit(def,fm,targetWidth);
    				message.append(split);
    				}
    				else { message.append("No definition available"); }
    				message.append("\n\n");
    				}
    			}
    		}
    	String m = message.toString();
    	hp.setHelpText(m);
    	}
    }

    private String bigString = null;
    private int bigX = 0;
    private int bigY = 0;
    

    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  CrosswordleBoard gb = disB(gc);
    	usedLetters = gb.usedLetters().toUpperCase();
       if(remoteViewer>=0)
       {	CELLSIZE = getWidth()/10;
    	 drawHiddenWindow(gc,selectPos,remoteViewer,getBounds());  
       }
       else
       {
       CrosswordleState state = gb.getState();
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
       
       GC.setRotatedContext(gc,logRect,selectPos,effectiveBoardRotation);
       Font f = G.getFont("monospaced",Style.Bold,G.getFontSize(standardBoldFont()));
       redrawGameLog(gc, nonDragSelect, logRect,Color.black, boardBackgroundColor,standardBoldFont(),f);
       GC.unsetRotatedContext(gc,selectPos);
       
       GC.frameRect(gc, Color.black, logRect);
       // this does most of the work, but other functions also use contextRotation to rotate
       // animations and sprites.
       boolean planned = plannedSeating();
     
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       {    
	   GC.setRotatedContext(gc,largerBoardRect,selectPos,effectiveBoardRotation);
       standardGameMessage(gc,stateRect,state);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,selectPos);

       String msg = bb.invalidReason==null ? s.get(CrosswordsVictoryCondition) : s.get(bb.invalidReason);
       String goalmsg = GoalExplanation;
       goalAndProgressMessage(gc,nonDragSelect,Color.black,msg,progressRect, goalRect,goalmsg);
       if(planned) 
       	{ 
       	  CrosswordleChip chip = lockOption ? CrosswordleChip.UnlockRotation : CrosswordleChip.LockRotation;
       	  chip.drawChip(gc, this,lockRect, selectPos, CrosswordleId.Lock,s.get(chip.overlay)); 
       	}
       drawNoChat(gc,altNoChatRect,selectPos);
       GC.unsetRotatedContext(gc,selectPos);
       }
       for(int player=0;player<bb.players_in_game;player++)
       	{ commonPlayer pl1 = getPlayerOrTemp(player);
       	  pl1.setRotatedContext(gc, selectPos,false);
       	   GC.setFont(gc,standardBoldFont());
       	   if(planned && gb.whoseTurn==player)
    	   {
    		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
    	   GC.setFont(gc, largeBoldFont());
       	   pl1.setRotatedContext(gc, selectPos,true);
       	}
       if(GC.handleSquareButton(gc,restartRect,buttonSelect,s.get(RestartMessage),HighlightColor, rackBackGroundColor))
       {
    	   buttonSelect.hitCode = CrosswordleId.Restart;
       }
       if(!planned)
      	{  
    	   //int ap = allowed_to_edit|G.offline() ? gb.whoseTurn : getActivePlayer().boardIndex;
    	   // generally prevent spectators seeing tiles, unless openracks or gameover
    	   //boolean censorSpectator =  !gb.openRack[ap] && getActivePlayer().spectator&&!allowed_to_edit;
      	}
     
       GC.setFont(gc,standardBoldFont());
       

       if (state != CrosswordleState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned)
				{
				String theWord = inputField.getText().trim().toLowerCase();
				boolean done = gb.canBeGuessed(theWord);
				if(gb.DoneState())
				{
					handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
							HighlightColor, rackBackGroundColor); 
				}
				else if(GC.handleRoundButton(gc, messageRotation,doneRect,done ? buttonSelect : null,s.get(ProbeMessage),
						HighlightColor, rackBackGroundColor))
					{
					buttonSelect.hitCode = CrosswordleId.Playword;
					buttonSelect.hitObject = theWord;
					}	
				}
			
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
        }
       

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==CrosswordleState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
  
 
        
        
             //      DrawRepRect(gc,pl.displayRotation,Color.black,b.Digest(),repRect);
        GC.setFont(gc,largeBoldFont());
        dateRect.draw(gc,selectPos);
        hardButton.draw(gc,selectPos);
        inputField.setVisible(true);
        inputField.redrawBoard(gc,selectPos);
        keys.draw(gc,selectPos);
        CrosswordleChip.logo.getImage().centerImage(gc,logoRect);
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        
        if(bigString!=null)
        {	GC.setFont(gc,largeBoldFont());
        	GC.drawBubble(gc,bigX,bigY,bigString,fullRect,messageRotation);
        }
       }
       drawHiddenWindows(gc, selectPos);
    }
    public String gameOverMessage()
    {	commonPlayer pl =getPlayerOrTemp(0);
    	long time = pl.elapsedTime;
    	return s.get(SolvedMessage,""+bb.guesses.size(),G.briefTimeString(time));
    }
    public void standardGameMessage(Graphics gc,Rectangle stateRect,CrosswordleState state)
    {
        standardGameMessage(gc,
   				state==CrosswordleState.Gameover?gameOverMessage():s.get(state.description()),
   				state!=CrosswordleState.Puzzle,
   				bb.whoseTurn,
   				stateRect);

    }
    public boolean allowResetUndo() { return(false); }
    public boolean allowPartialUndo()
    {	if(allowUndo())
    		{
  	  		int op = getCurrentMoveOp();
  	  		return((op!=MOVE_PLAYWORD)
  	  				&& (op!=MOVE_DONE) 
  	  				&& (op!=MOVE_START) 
  	  				&& (op!=MOVE_EDIT));
    		}
    		return(false);
    }
    public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode mode)
    {
  	
    	return(super.PerformAndTransmit(m,transmit,mode));
    }
    private void doRestart(CrosswordleMovespec m)
    {
       	// set the randomkey to the new date
    	bb.setRandomKey(m.to_row*MILLIS_PER_DAY,m.from_row!=0);
    	dateRect.setTime(bb.randomKey);
    	//
    	// reset a grab bag of state variables so we can do a new puzzle and score it.
    	//
    	commonPlayer ap = getActivePlayer();
       	ap.setElapsedTime(0);
       	ap.spectator = false;
       	setScored(false);
       	gameOver = false;
       	allowed_to_edit = false;
       	mutable_game_record = false;
       	doWayBack(replayMode.Live);
    	doTruncate();	
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
    {	if(mm.op==MOVE_PLAYWORD) { mm.setLineBreak(true); }
    	if(mm.op==MOVE_RESTART) 
    		{ doRestart((CrosswordleMovespec)mm);
    		  return true;
    		}
    	if(mm.op==MOVE_SETWORD)
    		{CrosswordleMovespec m = (CrosswordleMovespec)mm;
    		 String w = Base64.decodeString(m.word);
    		 if(!w.equals(inputField.getText())) { inputField.setText(w); }
    		 return true;
    		}
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
		if(replay!=replayMode.Replay) { playSounds((CrosswordleMovespec)mm); }
		return (true);
    }
     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for pushfight, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
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
//     		PushfightCell dest = bb.animationStack.pop();
//     		PushfightCell src = bb.animationStack.pop();
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

 void playSounds(CrosswordleMovespec mm)
 {
	 switch(mm.op)
	 {
	 case MOVE_PLAYWORD:
		 for(int i=0;i<mm.word.length();i++) 
		 	{ playASoundClip(light_drop,150);
		 	}
		 break;		 
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
        return (new CrosswordleMovespec(st, pl));
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
    	  switch(nmove.op)
    	  {
    	  case MOVE_SETWORD:
     	  case MOVE_RESTART:
    		  return(null);
    	  default: break;
    	  }

    	  commonMove rval = EditHistory(nmove,false);
     	     
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
// for reference, here's the standard definition
 //   public void verifyGameRecord()
 //   {	BoardProtocol ourB =  getBoard();
 //   	int ourDig = ourB.Digest();
 //   	BoardProtocol dup = dupBoard = ourB.cloneBoard();
 //   	int dupDig = dup.Digest();
 //   	G.Assert(dupDig==ourDig,"Duplicate Digest Matches");
 //   	dup.doInit();
 //   	int step = History.size();
 //   	int limit = viewStep>=0 ? viewStep : step;
 //   	for(int i=0;i<limit;i++) 
 //   		{ commonMove mv = History.elementAt(i);
 //   		  //G.print(".. "+mv);
 //   		  dup.Execute(mv); 
 //   		}
 //   	int dupRedig = dup.Digest();
 //   	G.Assert(dup.whoseTurn()==ourB.whoseTurn(),"Replay whose turn matches");
 //   	G.Assert(dup.moveNumber()==ourB.moveNumber(),"Replay move number matches");
 //   	if(dupRedig!=ourDig)
 //   	{
 //   	//int d0 = ourB.Digest();
 //   	//int d1 = dup.Digest();
 //   	G.Assert(false,"Replay digest matches");
 //   	}
 //   	// note: can't quite do this because the timing of "SetDrawState" is wrong.  ourB
 //   	// may be a draw where dup is not if ourB is pending a draw.
 //   	//G.Assert(dup.getState()==ourB.getState(),"Replay state matches");
 //   	dupBoard = null;
 //   }
    
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
        if (hp.hitCode instanceof CrosswordleId)// not dragging anything yet, so maybe start
        {
        CrosswordleId hitObject =  (CrosswordleId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: break;

        case BoardLocation:
	        CrosswordleCell hitCell = hitCell(hp);
	    	PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    	break;
        } 
        }
    }
	public HitPoint MouseMotion(int ex, int ey,MouseState upcode)
	{	if(keys!=null && keys.containsPoint(ex,ey))
			{	
			keys.doMouseMove(ex,ey,upcode);
			}
			else { keys.doMouseMove(ex, ey, upcode); }
	
		return(super.MouseMotion(ex, ey, upcode));
	}
	
	public void update(SimpleObservable o, Object eventType, Object arg) 
	{
			if(arg==TextContainer.Op.Send)
			{
			deferredEvents.deferActionEvent(arg);
			}
	}

	private void maybeSendGuess()
	{
		String theWord = inputField.getText().trim().toLowerCase();
		boolean done = bb.canBeGuessed(theWord);
		if(done) 
			{ 
			commonPlayer ap = getActivePlayer();
			if(allowed_to_edit || !ap.spectator) { PerformAndTransmit("Play "+theWord); } 
			inputField.setText("");
			}
	}
	private String lastText = "";
	public void updateInput()
	{	String newText = inputField.getText();
		if(!newText.equals(lastText))
		{
			lastText = newText;
	    	PerformAndTransmit(G.concat("SetWord ",Base64.encodeSimple(newText)));
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
        bigString = null;
        if(id instanceof CalculatorButton.id)
		{	if(id==CalculatorButton.id.Guess)
			{
			maybeSendGuess();
			}
        	if(keys!=null)
			{
				keys.StopDragging(hp); 
			}
		}
        else if(id instanceof DateSelector.DateCode) { dateRect.StopDragging(hp); }
        else if(!(id instanceof CrosswordleId))  
       		{   missedOneClick = performStandardActions(hp,missedOneClick);
       			definitionCell = null;
       		}
        else {
        missedOneClick = false;
        CrosswordleId hitCode = (CrosswordleId)id;
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
        case ToggleEasy:
        	hardButton.toggle();
        	break;
        case Restart:
        	PerformAndTransmit(G.concat("Restart ",dateRect.getTime()/MILLIS_PER_DAY," ",hardButton.isOn()));
        	break;
        case Playword:
        	PerformAndTransmit("Play "+(String)(hp.hitObject));
        	inputField.setText("");
        	break;
        case InputField:
			{
			if(useKeyboard) {
				keyboard = new Keyboard(this,inputField);
			}
			else 
			{	requestFocus(inputField); 
				int flipInterval = 500;
				inputField.setEditable(this,true);
				inputField.setFocus(true,flipInterval);
				repaint(flipInterval);
			}}
			break;
        case Definition:
        	definitionCell = hitCell(hp);
        	break;
        case Lock:
        	lockOption = !lockOption;
        	break;
        case Rotate:
        	effectiveBoardRotation = (effectiveBoardRotation+Math.PI/2);
        	boardRotation = (boardRotation+1)%4;
        	contextRotation = 0;
        	generalRefresh();
        	break;
        case EyeOption:
        	{
         	String op = (String)hp.hitObject;
         	int rm = remoteWindowIndex(hp);
        	PerformAndTransmit((rm<0 ? "show ":"see ")+op);
        	}
        	break;
        case SetOption:
        	{
        	String m = (String)hp.hitObject;
        	PerformAndTransmit(m);
        	}
        	break;
        case Blank:
        	{
        	CrosswordleChip ch = (CrosswordleChip)hp.hitObject;
        	PerformAndTransmit("SetBlank "+ch.letter);
        	}
        	break;
 
         case EmptyBoard:
        	{
        		CrosswordleCell hitObject = hitCell(hp);
        		if(bb.pickedObject==null)
            	{	
            		{
            			PerformAndTransmit(G.concat("Pick ",hitCode.name()," ",hitObject.col," ",hitObject.row));
            		}
            	}
        		else {
        			PerformAndTransmit(G.concat("Dropb ",hitObject.col," ",hitObject.row)); 
        		}
        	}
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
        	{
            CrosswordleCell hitObject = hitCell(hp);
 			PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
        	}
			break;
			
        }
        }
    }


    private boolean setDisplayParameters(CrosswordleBoard gb,Rectangle r)
    {
      	boolean complete = false;
      	// the numbers for the square-on display are slightly ad-hoc, but they look right
      	gb.SetDisplayParameters(1, 1.0, 0,0,0); // shrink a little and rotate 60 degrees
       	gb.SetDisplayRectangle(r);
      	if(complete) { generalRefresh(); }
      	return(complete);
    }

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
    public String sgfGameType() { return(Crosswords_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int np = G.IntToken(his);	// players always 2
    	long rv = G.LongToken(his);
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
        if(target==TextContainer.Op.Send)
        {
        	handled = true;
        	maybeSendGuess();
        }
        if(target==saveScreen)
        {	handled = true;
            String ss = sgf_reader.do_sgf_dialog(FileDialog.SAVE,gameName(), "*.png");
            if(ss!=null)
            {
            //dictionary.saveDefinitionList(ss);
            /*
            int bw = G.Width(boardRect);
            int bh = G.Height(boardRect);
            Image boardImage = G.createImage(this,bh,bw);
            Graphics g = boardImage.getGraphics();
            drawFixedBoard(g,new Rectangle(0,0,bw,bh));
            boardImage.SaveImage(ss);
            */
            }      	
        }
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
    
 public void ViewerRun(int wait)
   {	
        super.ViewerRun(wait);
        if(ourActiveMove()) { updateInput(); }
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
    {  return(new CrosswordlePlay());
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
                dateRect.setTime(bb.randomKey);
                adjustPlayers(bb.nPlayers());
              }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
           else if (parseVersionCommand(name,value,2)) {}
           else if (parsePlayerCommand(name,value)) {}
           else if (name.equalsIgnoreCase(game_property) && value.equalsIgnoreCase("sprint"))
           {	// grandfather an editing error
           }
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
    // first=true causes the bs_uni script to check with the server
    // and record the result. This is a temporary solution so the
    // user can solve multiple puzzles in one session.
    //
    private String firstPuzzle = "true";
    public String getUrlNotes()
    {	long key = bb.randomKey;
    	String v = G.concat(
    			"&puzzleid=",bb.getSolution(key),
    			"&variation=",bb.variation.name(),
    			"&hard=",((key&1)==0?"false":"true"),
    			"&puzzledate=",dateRect.dateString(),
    			"&first=",firstPuzzle
    			);
    	firstPuzzle = "false";
    	return v;
    }
    public void doGameTest()
    {	Builder.getInstance().generateCrosswords(G.Date(),bb.ncols,bb.nrows);
    }
   
}

