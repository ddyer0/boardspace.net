package kuba;
import bridge.*;
import com.codename1.ui.geom.Rectangle;

import lib.Graphics;
import lib.Image;

import online.common.*;

import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;

/**
 * 
 * Change History
 *
 * May 2007 initial work in progress. 
 *
 * This code is derived from the "HexGameViewer" class.  Refer to the
 * documentation there for overall structure notes.
*/
public class KubaViewer extends CCanvas<KubaCell,KubaBoard> implements KubaConstants, GameLayoutClient
{
     /**
	 * 
	 */
	 
    static final String Kuba_SGF = "Kuba"; // sgf game number allocated for hex

    // file names for jpeg images and masks
    static final String ImageDir = "/kuba/images/";
	// sounds
    static final int BOARD_INDEX = 0;
    static final String[] ImageFileNames = 
        {  "board"
        };

    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "kuba-icon-nomask",
    	  };
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] images = null; // images of black and white gobblets and board
    private static Image[] textures = null;// background textures
    
    // private state
    private KubaBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle repRect = addRect("repRect");
    

    public void preloadImages()
    {	
    	if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    	KubaChip.preloadImages(loader,ImageDir);
        images = loader.load_images(ImageDir, ImageFileNames, 
        		loader.load_images(ImageDir, ImageFileNames,"-mask")); // load the main images
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
    {
        super.init(info,frame);
        
        b = new KubaBoard(info.getString(OnlineConstants.GAMETYPE, "Traboulet"),
        		getStartingColorMap());
        useDirectDrawing(true); 	// not tested yet
        doInit(false);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype);						// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
   }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*3;
    	int chipH = unitsize*2;
    	int doneW = unitsize*4;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	Rectangle done = doneRects[player];
    	
    	G.SetRect(chip, x, y, chipW, chipH);
    	G.SetRect(done, x+chipW,G.Bottom(box),doneW,plannedSeating()?doneW/2:0);
    	
    	pl.displayRotation = rotation;
    	
    	G.union(box, chip,done);
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
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        int nrows = b.boardRows;
        int ncols = b.boardColumns;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// 1:1 aspect ratio for the board
    			fh*2.5,	// minimum cell size
    			fh*3.5,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	int CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        int stateH = CELLSIZE/3;
        G.placeStateRow(stateX,stateY,boardW,stateH,iconRect,stateRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
   
    }
    

	// draw a color indicator ball
    private void DrawCommonChipPool(Graphics gc, int forPlayer, Rectangle r)
    {	if(gc!=null)
    	{
    	KubaChip chip = KubaChip.getChip(b.getColorMap()[forPlayer]);
        chip.drawChip(gc,this,r,null);
    	}
      }

    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	KubaChip chip = KubaChip.getChip(idx);
    	chip.drawChip(g,this,SQUARESIZE,xp,yp,null);
    }



    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      // erase
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //G.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);   
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid
     images[BOARD_INDEX].centerImage(gc, boardRect);
  	b.SetDisplayParameters(0.58,1.0,  0.1,0.0,  0);
    b.SetDisplayRectangle(boardRect);

      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    private void drawBall(Graphics gc,KubaCell cell,int xpos,int ypos)
    {
         cell.drawChip(gc, this, SQUARESIZE, xpos, ypos, null);
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, KubaBoard gb, Rectangle brect, HitPoint highlight)
    {
     	//
        // now draw the contents of the board and anything it is pointing at
        //
        KubaCell hitCell = null;
        Hashtable<KubaCell,String> prevCells = gb.stackOrigins();
        double cellw = gb.cellSize();
        double cellOff = (gb.cellSize()/4);
        int top = G.Bottom(brect);
        int left =  G.Left(brect);
        char firstCol = 'A';
        char lastCol = (char)('A'+gb.ncols);
        int lastRow = gb.nrows;
        for(Enumeration<KubaCell> cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements();)
       		{ //where we draw the grid
            KubaCell cell = cells.nextElement();
            int ypos = top - gb.cellToY(cell);
            int xpos =  left + gb.cellToX(cell);
            int thiscol = cell.col;
            int row = cell.row;
            int col = thiscol-'A';
            boolean canHitGutters = gb.canHitGutters();
            String orig = prevCells.get(cell);
            drawBall(gc,cell,xpos,ypos);
        	 if((gc!=null) && (orig!=null))
        	 {	// mark the path back
        		 StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,orig);
        	 }
            if(gb.LegalToHitBoard(cell)
            		&& cell.closestPointToCell(highlight, SQUARESIZE,xpos, ypos)
                    )
              { hitCell = cell; 
               }
            if((thiscol==firstCol))
              {	int x1 = xpos-(int)(cellw+cellOff);
              	int x2 = xpos-(int)(cellw*2+cellOff);
              	KubaCell gcell = gb.gutters[LeftIndex][row-1];
              	KubaCell tcell = gb.trays[LeftIndex][row-1];
              	drawBall(gc,gcell,x1,ypos);
              	drawBall(gc,tcell,x2,ypos);
              	if(canHitGutters)
              	{
              	if(gb.LegalToHitCell(gcell) && gcell.closestPointToCell(highlight,SQUARESIZE,x1,ypos)) 
              		{ hitCell = gcell;
               		}
              	if( gb.LegalToHitCell(tcell) && tcell.closestPointToCell(highlight,SQUARESIZE,x2,ypos)) 
              		{ hitCell = tcell;  }
              	}
              }
              if((thiscol==lastCol))
              {	int x1 = xpos+(int)(cellw+cellOff);
              	int x2 = xpos+(int)(cellw*2+cellOff);
              	KubaCell gcell = gb.gutters[RightIndex][row-1];
              	KubaCell tcell = gb.trays[RightIndex][row-1];
             	drawBall(gc,gcell,x1,ypos);
              	drawBall(gc,tcell,x2,ypos);
              	if(canHitGutters)
              	{
             	if(gb.LegalToHitCell(gcell) && gcell.closestPointToCell(highlight,SQUARESIZE,x1,ypos)) 
              		{ hitCell = gcell; 
              		}
              	if(gb.LegalToHitCell(tcell) && tcell.closestPointToCell(highlight,SQUARESIZE,x2,ypos)) 
              		{ hitCell = tcell;
              		}
              	}
              }
              if((row==1))
              {	int y1 = ypos+(int)(cellw+cellOff);
              	int y2 = ypos+(int)(cellw*2+cellOff);
              	KubaCell gcell = gb.gutters[BottomIndex][col];
              	KubaCell tcell = gb.trays[BottomIndex][col];
            	drawBall(gc,gcell,xpos,y1);
              	drawBall(gc,tcell,xpos,y2);
              	if(canHitGutters)
              	{
             	if(gb.LegalToHitCell(gcell) && gcell.closestPointToCell(highlight,SQUARESIZE,xpos,y1)) 
              		{ hitCell = gcell;
             		}
              	if(gb.LegalToHitCell(tcell) && tcell.closestPointToCell(highlight,SQUARESIZE,xpos,y2)) 
              		{ hitCell = tcell;
              		}
              	}
              }
              if((row==lastRow))
              {	int y1 = ypos-(int)(cellw+cellOff);
              	int y2 = ypos-(int)(cellw*2+cellOff);
            	KubaCell gcell = gb.gutters[TopIndex][col];
            	KubaCell tcell = gb.trays[TopIndex][col];
            	drawBall(gc,gcell,xpos,y1);
              	drawBall(gc,tcell,xpos,y2);
              	if(canHitGutters)
              	{
             	if(gb.LegalToHitCell(gcell) && gcell.closestPointToCell(highlight,SQUARESIZE,xpos,y1))
              	{ hitCell = gcell;
              	}
              	if(gb.LegalToHitCell(tcell) && tcell.closestPointToCell(highlight,SQUARESIZE,xpos,y2)) 
              	{ hitCell = tcell;
             	  }
              	}
       		  }
        
        }
        if(hitCell!=null)
        {	
        	highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = SQUARESIZE/3;
        	highlight.spriteColor = Color.red;
        }

    }
   //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  KubaBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      KubaState vstate = gb.getState();
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        
        boolean planned = plannedSeating();
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
          {	commonPlayer pl = getPlayerOrTemp(i);
          	pl.setRotatedContext(gc, highlight, false);
            DrawCommonChipPool(gc, i, chipRects[i]);
          	if(planned && (i==gb.whoseTurn))
          	{
          		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
      					HighlightColor, rackBackGroundColor);
          	}
          	pl.setRotatedContext(gc, highlight, true);
          }	
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

        GC.setFont(gc,standardBoldFont());
        drawPlayerStuff(gc,(vstate==KubaState.PUZZLE_STATE),ourSelect,
 	   			HighlightColor, rackBackGroundColor);
       
		if (vstate != KubaState.PUZZLE_STATE)
        {
			if(!planned) { handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
			}
			handleEditButton(gc,messageRotation,editRect,select, highlight,HighlightColor, rackBackGroundColor);
        }

 
        standardGameMessage(gc,messageRotation,
        		vstate==KubaState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=KubaState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        DrawCommonChipPool(gc, gb.whoseTurn, iconRect);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);

        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
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

        if(replay!=replayMode.Replay) { playSounds(mm); }
 
        return (true);
    }
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new KubaMovespec(st, player));
    }
    
     

private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
     case MOVE_BOARD_BOARD:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
   	break;
    case MOVE_PICKB:
    case MOVE_PICKG:
    	 playASoundClip(light_drop,100);
    	 break;
    case MOVE_DROPG:
    case MOVE_DROPB:
      	 playASoundClip(heavy_drop,100);
      	break;
    default: break;
    }
	
}

boolean startMotion(KubaId hitObject,KubaCell cell,KubaChip chip)
{	// nothing is moving, maybe start something.  This might be 
	// either from a click or a click/drag
	if(chip!=null)
	{
	switch(hitObject)
	{
    case Gutter0:
    case Gutter1:
    case Gutter2:
    case Gutter3:
    	PerformAndTransmit( "Pickg "+hitObject.shortName+" "+cell.row+" "+chip.pieceNumber());
    	break;
    case Tray0:
	case Tray1:
	case Tray2:
	case Tray3:
		PerformAndTransmit( "Pickt "+hitObject.shortName+" "+cell.row+" "+chip.pieceNumber());
		break;
	case BoardLocation:
		PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.pieceNumber());
		break;
	default:
		break;
	}}
	return(hasMovingObject(null));
}
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
		if (hp.hitCode instanceof KubaId) // not dragging anything yet, so maybe start
        {

			KubaId hitObject = (KubaId)hp.hitCode;
			KubaCell cell = hitCell(hp);
			KubaChip chip = (cell==null) ? null : cell.topChip();
			hp.dragging = startMotion(hitObject,cell,chip);
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
        if(!(id instanceof KubaId)) {  missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
    	missedOneClick = false;
    	KubaId hitObject = (KubaId)hp.hitCode;
		KubaCell cell = hitCell(hp);
		KubaChip cup = (cell==null) ? null : cell.topChip();
		int movingObject = b.movingObjectIndex();
		if(cell!=null)
		{
        if(movingObject<0)
		{ 	startMotion(hitObject,cell,cup);
		}
 		else 
		{
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case Tray0:
        case Tray1:
        case Tray2:
        case Tray3:
			PerformAndTransmit("Dropt "+hitObject.shortName+" "+cell.row); 
        	break;
        case Gutter0:
        case Gutter1:
        case Gutter2:
        case Gutter3:
			PerformAndTransmit("Dropg "+hitObject.shortName+" "+cell.row); 
       		break;
        case BoardLocation:	// we hit the board 
			PerformAndTransmit("Dropb "+cell.col+" "+cell.row); 
			break;
        }}}
		}
    }

    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Kuba_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        b.doInit(token);
    }


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new KubaPlay()); }


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
            {
                b.doInit(value);
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

