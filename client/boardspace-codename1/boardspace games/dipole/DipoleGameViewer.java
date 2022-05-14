package dipole;

import bridge.*;
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
public class DipoleGameViewer extends CCanvas<DipoleCell,DipoleBoard> implements DipoleConstants, GameLayoutClient
{
     /**
	 * 
	 */
	   static final String Dipole_SGF = "Dipole"; // sgf game number allocated for hex
	   
	   
	    // file names for jpeg images and masks
	    static final String ImageDir = "/dipole/images/";
	    static final int LOGO_INDEX = 0;
	    static final String[] ImageFileNames = 
	        {
	        "logo",
	        
		};
	    static final int BACKGROUND_TILE_INDEX = 0;
	    static final int BACKGROUND_REVIEW_INDEX = 1;
	    static final int LIFT_ICON_INDEX = 2;
	    static final String TextureNames[] = 
	    	{ "background-tile" ,
	    	  "background-review-tile",
	    	  "lift-icon-nomask"
	    	  };
	    // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] images = null; // images of black and white gobblets and board
    private static Image[] textures = null;// background textures
    
    // private state
    private DipoleBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle wasteRect = addRect("wasteRect");
    private Rectangle logoRect = addRect("logoRect");
    

    public void preloadImages()
    {	
    	DipoleChip.preloadImages(loader,ImageDir);
    	if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        images = loader.load_masked_images(ImageDir, ImageFileNames); // load the main images
        textures = loader.load_images(StonesDir,TextureNames);
    	}
    	gameIcon = images[LOGO_INDEX];
    }


	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
        
        b = new DipoleBoard(info.getString(OnlineConstants.GAMETYPE, "Dipole"),
        		getStartingColorMap());
        useDirectDrawing(true);
        doInit(false);
        b.setReverseY(!b.reverseY());
        
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
    	{ 	startFirstPlayer();
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
        int nrows = b.boardSize;  
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			0.9,	// aspect ratio for the board
    			fh*2.5,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	layout.placeRectangle(wasteRect, buttonW, buttonW,BoxAlignment.Center);
       	layout.placeRectangle(logoRect,buttonW*2,buttonW,buttonW*6,buttonW*3,BoxAlignment.Top,true);
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/nrows,(double)mainH/nrows);
    	SQUARESIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(nrows*cs);
    	int boardH = (int)(nrows*cs);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	boolean rotate = seatingFaceToFaceRotated();
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateH = SQUARESIZE/3;
        int stateY = boardY;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,liftRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	if(rotate)
    	{	// board is square, no need to rotate
    		contextRotation = -Math.PI/2;
    	}
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        
        }


    private void DrawLiftRect(Graphics gc,HitPoint highlight)
    {	
    	drawLiftRect(gc,liftRect,highlight,textures[LIFT_ICON_INDEX]); 
 		images[LOGO_INDEX].centerImage(gc,logoRect);
    }
    private void DrawWasteRect(Graphics gc,HitPoint highlight)
    {	
   
		  GC.frameRect(gc,Color.black,wasteRect);
		  DipoleCell waste = b.Waste_Cell;
		  boolean canhit = G.pointInRect(highlight,wasteRect) &&  b.legalToHitWaste();
		    int xpos = G.centerX(wasteRect);
		  int ypos = G.centerY(wasteRect);
		  if(waste.drawStack(gc, this, canhit?highlight:null, SQUARESIZE, xpos, ypos, 0, 0.1, null))
		  {		highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
		  		highlight.awidth = SQUARESIZE/3;
    		highlight.spriteRect = wasteRect;
    		highlight.spriteColor = Color.red;
    	}
    }
	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, int forPlayer, Rectangle r, int player,
        HitPoint highlight)
    {	
    	DipoleCell rack[] = b.rack;
        boolean canhit = G.pointInRect(highlight, r) && b.LegalToHitChips(forPlayer) ;
        boolean canDrop = hasMovingObject(highlight);
        	DipoleCell thisCell = rack[forPlayer];
        int xpos = G.centerX(r);
        int ypos = G.centerY(r);
        String msg = "     "+b.chips_on_board[player]+" - "+b.sum_of_rows[forPlayer];
        GC.setFont(gc,standardBoldFont());
        if(thisCell.drawStack(gc,this,canhit?highlight:null,SQUARESIZE,xpos,ypos,0,0.1,msg))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = SQUARESIZE/3;
	     		  highlight.spriteColor = Color.red;
        	highlight.spriteRect = r;
        }
    }

    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {     DipoleChip chip = DipoleChip.getTile(idx);	// tiles have the zero offset
    	  chip.drawChip(g,this,SQUARESIZE, xp, yp, null);	
    }
 


    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	
       textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
        drawFixedBoard(gc);
    }
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      //G.fillRect(gc, fullRect);
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }

  		b.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
	    b.SetDisplayRectangle(brect);     
	    
      b.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, DipoleBoard gb, Rectangle brect, HitPoint highlight)
    {
     	boolean dolift = doLiftAnimation();
     	boolean showSize = dolift || G.pointInRect(highlight, liftRect);
     	Hashtable<DipoleCell,DipoleCell> dests = gb.getMoveDests();
     	
     	boolean planned = plannedSeating();
     	//
        // now draw the contents of the board and anything it is pointing at
        //
    	Enumeration<DipoleCell>cells = gb.getIterator(Itype.RLTB);
     	while(cells.hasMoreElements())
     	{	DipoleCell cell = cells.nextElement();
            char thiscol = cell.col;
            int row = cell.row;
            int ypos = G.Bottom(brect) - gb.cellToY(thiscol, row);
            int xpos = G.Left(brect) + gb.cellToX(thiscol, row);
            int topindex = cell.chipIndex;
            boolean isADest = dests.get(cell)!=null;
            boolean canHit = gb.LegalToHitBoard(cell);
            String msg = showSize && cell.height()>1 ? ""+topindex : null;
              			 GC.setFont(gc,largeBoldFont());
            DipoleChip chip = cell.topChip();
            GC.setColor(gc,chip==DipoleChip.white ? Color.black :Color.yellow);
            if(cell.drawStack(gc, this, canHit ? highlight:null, SQUARESIZE, xpos, ypos,liftSteps,
            		planned ? 0.05 : 0,
            		planned ? 0 : 0.05,
            		msg))
              { 
            highlight.spriteColor = Color.red;
      		highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
          		highlight.awidth = SQUARESIZE/3;
             }

            if(isADest)
            {	StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,msg);
            //G.DrawAACircle(gc,e_xpos+dotsize,e_ypos,dotsize,Color.red,Color.gray,true);
        }
        	}
        
        DrawWasteRect(gc,highlight);

    }
     
    public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  DrawLiftRect(gc,highlight); 
    
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  DipoleBoard gb = disB(gc);
       
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      DipoleState vstate = gb.getState();
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
     
       GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
         drawBoardElements(gc, gb, boardRect, ot);
         GC.unsetRotatedContext(gc,highlight);
         
        //DrawChipPool(gc, secondPlayerChipRect, SECOND_PLAYER_INDEX, ot);
        //DrawChipPool(gc, firstPlayerChipRect, FIRST_PLAYER_INDEX, ot);
         
         boolean planned = plannedSeating();
         for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
           {	commonPlayer pl = getPlayerOrTemp(i);
           	pl.setRotatedContext(gc, highlight, false);
           	DrawCommonChipPool(gc, i,chipRects[i], gb.whoseTurn,ot);
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
        drawPlayerStuff(gc,(vstate==DipoleState.PUZZLE_STATE),ourSelect,
    	   			HighlightColor, rackBackGroundColor);

		if (vstate != DipoleState.PUZZLE_STATE)
        {
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
 
         }
 


 
        
            standardGameMessage(gc,messageRotation,
            		vstate==DipoleState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
            				vstate!=DipoleState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
            gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
            goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);
         
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
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Stack);
        
        if(replay!=replayMode.Replay) { playSounds((DipoleMovespec)mm); }
 
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
        return (new DipoleMovespec(st, player));
    }
    

    

private void playSounds(DipoleMovespec m)
{

    // add the sound effects
    switch(m.op)
    {
    case MOVE_BOARD_WASTE:
    case MOVE_DROP:
		{ int h = m.to_row;
		  while(h-- > 0) { playASoundClip(scrape,100); }
		}
      	 break;
    case MOVE_PICK:
    case MOVE_PICKB:
    	playASoundClip(light_drop,300); 
    	break;
    case MOVE_BOARD_BOARD:
    	//playPickSound(b.getCell(m.from_col,m.from_row),750);
    	playASoundClip(light_drop,300); 
		//$FALL-THROUGH$
	case MOVE_DROPB:
    	{
    	DipoleCell c = b.pickedSource;
    	DipoleCell d = b.droppedDest;
    	int h = 1;
    	if((b.getState()!=DipoleState.PUZZLE_STATE)
    			&& (c!=null) 
    			&& (d!=null) 
    			&& (c.onBoard) 
    			&& (d.onBoard))
    		{
    		h = Math.max(Math.abs(c.row-d.row),Math.abs(c.col-d.col));
    		}
    	while(h-- > 0) { playASoundClip(light_drop,100); }
    	}
      	break;
    default: break;
    }
	
}

 
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp )
    {
        if (hp.hitCode instanceof DipoleId) // not dragging anything yet, so maybe start
        {
       	DipoleId hitObject = (DipoleId)hp.hitCode;
		DipoleCell cell = hitCell(hp);
	    switch(hitObject)
	    {
        case Common_Pool:
        	PerformAndTransmit("Pick C "+cell.row+" "+cell.row);
        	break;
	    case BoardLocation:
			DipoleChip chip = (cell==null) ? null : cell.topChip();
			if(chip!=null)
			{
	    	if(cell.chipIndex>0)
	    		{ PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber);
	    		}
			}
	    	break;
		default:
			break;
        }
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
        if(!(id instanceof DipoleId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	DipoleId hitObject = (DipoleId)hp.hitCode;
        DipoleState state = b.getState();
		DipoleCell cell = hitCell(hp);
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case Waste_Rect:
        	if(b.movingObjectIndex()>=0) { PerformAndTransmit("Drop X "+b.distanceToEdge());}
        	else { performReset(); }
        	break;
        case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ 
				  if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
			}
			break;
			
        case Common_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col = " C " ;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                	case PLAY_STATE:
            		PerformAndTransmit(RESET);
            		break;

               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop"+col+cell.row+" "+mov);
            		break;
            	}
			}
         	}
            break;

         }}
    }

    	
    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Dipole_SGF); }

    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
        b.doInit(token);
    }

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new DipolePlay()); }


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

