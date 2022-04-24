package truchet;


import java.awt.*;
import online.common.*;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

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
public class TruGameViewer extends CCanvas<TruCell,TruGameBoard> implements TruConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
     
 
    // images
    private static Image[] textures = null;// background textures
    
    // private state
    private TruGameBoard b = null; 	// the board from which we are displaying
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
    private Rectangle captureRects[] = addRect("Captures",2);
    

    public void preloadImages()
    {	TruChip.preloadImages(loader,ImageDir);
    	if (textures == null)
    	{ 
        textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = textures[ICON_INDEX];
    }

    Color CheMouseColors[] = { new Color(0.7f,0.7f,1.0f),new Color(0.3f,0.3f,1.0f)};
    Color CheMouseDotColors[] = { Color.black,Color.white};

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
        MouseColors = CheMouseColors;
        MouseDotColors = CheMouseDotColors;
        b = new TruGameBoard(info.getString(OnlineConstants.GAMETYPE, "Truchet"),
        		info.getString(exHashtable.GAMEUID,
        		G.getString(exHashtable.GAMEUID,"testgame")),
        		getStartingColorMap());
        doInit(false); 
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);						// initialize the board
        if(!preserve_history)
    	{
        	startFirstPlayer();
        }
   }
    
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*4;
    	int doneW = unitsize*6;
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW, chipW);
    	G.SetRect(done, x, y+chipW, doneW, plannedSeating()?doneW/2:0);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	Rectangle captures = captureRects[player];
    	G.SetRect(captures, G.Right(box),y,unitsize*2,unitsize*5);	// a little taller than necessary so overall layout is better

    	pl.displayRotation = rotation;
    	G.union(box, chip,done,captures);
    	return(box);
    }
    
    public boolean usePerspective() { return(super.getAltChipset()==0); }
    
    private double aspect[] = { 0.7,1.0,1.4};
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	setLocalBoundsV(x,y,width,height,aspect);
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*25;
    	int vcrW = fh*16;
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        int nrows = b.boardSize;  
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	//
    			0.75,	// 60% of space allocated to the board
    			1.0,	// 1.0:1 aspect ratio for the board
    			fh*1.8,
    			fh*2.5,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        boolean rotate = seatingFaceToFaceRotated();
      // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
       	layout.placeTheVcr(this,vcrW,vcrW*3/2);
        
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int stateH = fh*3;
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/nrows,(double)mainH/nrows);
    	SQUARESIZE = (int)cs;
     	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(nrows*SQUARESIZE);
    	int boardH = (int)(nrows*SQUARESIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-stateH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,liftRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	if(rotate)
    	{
    		// board is square, no need to rotate the rectangle
    		contextRotation = -Math.PI/2;
    	}
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        return(boardW*boardH);
    }

      
    private void DrawCaptures(Graphics gc, int forPlayer, Rectangle r,
            HitPoint highlight)
    {
    	TruCell c = b.captures[forPlayer];
    	boolean canHit = b.getState()==TruchetState.PUZZLE_STATE
    						&& ((b.movingObjectIndex()>=0) 
    								? true
    								: (c.topChip()!=null));
    	if(c.drawStack(gc, this, canHit?highlight:null,G.Width(r),
    			G.Left(r),G.Bottom(r)-SQUARESIZE/3,0,0.1,null))
    		{	highlight.awidth=SQUARESIZE/2;
    			highlight.hit_x += SQUARESIZE/3;
    			highlight.arrow = b.movingObjectIndex()>0 ? StockArt.DownArrow : StockArt.UpArrow;
    			highlight.spriteColor = Color.red;
    			
    		};

    }

	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, int forPlayer, Rectangle r,  HitPoint highlight,boolean score)
    {	
        boolean canhit =  G.pointInRect(highlight, r) && b.LegalToHitChips(forPlayer);
        boolean canDrop = hasMovingObject(highlight);
        int cellW = G.Width(r);
        {	TruCell thisCell = b.playerPool[forPlayer];
        	if(thisCell!=null)
        	{	TruChip thisChip = thisCell.topChip();
        		int left = G.Left(r);
	    		int top = G.Top(r);
	    		int cx = G.centerX(r);
	    		int cy = G.centerY(r); 
	       		if(canhit 
	       				&& G.pointInRect(highlight,left,top,cellW,cellW)
	       				&& (canDrop ? b.canDropOn(thisCell) : (thisChip!=null))
	       				)
	    		{ highlight.hitObject = thisCell;
	     		  highlight.hitCode = thisCell.rackLocation();
	     		  //(thisChip.colorIndex()==FIRST_PLAYER_INDEX) ? TruId.White_Chip_Pool : TruId.Black_Chip_Pool;
	     	      highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
	     	      highlight.awidth = G.Width(r)/2;
	    		}
        		if(thisChip!=null )
        		{
                  if(gc!=null)
                	{
                	 GC.setFont(gc,largeBoldFont());
                 	 thisChip.drawChip(gc,this,cellW,cx-cellW/3 ,cy+cellW/3,
                			 score ? ""+b.enemyBasesOccupied(forPlayer)+"-"+b.chips_on_board[forPlayer]
                					: "");
                 	GC.setFont(gc,standardPlainFont());
 
                	}
	       		}
         	}
        }
    }

    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	TruChip chip = TruChip.getChip(idx);
    	chip.drawChip(g,this,SQUARESIZE,xp-SQUARESIZE/3,yp+SQUARESIZE/3,null);
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
    { boolean review = reviewMode() && !mutable_game_record;
     if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
      b.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
      b.SetDisplayRectangle(brect);

        
      // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], 
      //	  brect.x,brect.y,brect.width,brect.height,this);

      b.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, TruGameBoard gb, Rectangle brect, HitPoint highlight)
    {
     	int liftdiv = 40;
     	Hashtable<TruCell,TruCell> riverDests = gb.riverMoveDests();		// can be a river move cell
     	Hashtable<TruCell,TruCell> splitDests = gb.splitMoveDests();		// can be a split cell
     	Hashtable<TruCell,TruCell> mergeDests = gb.mergeMoveDests();		// can be a merge cell
     	Hashtable<TruCell,TruCell> mergeSources = gb.mergeMoveSources();	// can be the source for other partners in a merge
     	TruCell flipped = gb.flippedCell;
     	boolean dolift = doLiftAnimation();
 
     	//
        // now draw the contents of the board and anything it is pointing at
        //
    	TruCell hitCell = null;
     	int CSQ = SQUARESIZE/3;
     	int cx = G.centerX(boardRect);
     	int cy = G.centerY(boardRect);
     	int top = G.Bottom(brect);
     	int left = G.Left(brect);
        for(Enumeration<TruCell> cells= gb.getIterator(Itype.TBRL); cells.hasMoreElements();)
        	{
            TruCell cell = cells.nextElement();
            boolean isARiverDest = riverDests.get(cell)!=null;
            boolean isASplitDest = splitDests.get(cell)!=null; 
            boolean isAMergeDest = mergeDests.get(cell)!=null;
            boolean isAMergeSource = mergeSources.get(cell)!=null; 
            int ypos0 = top - gb.cellToY(cell);
            int xpos0 = left + gb.cellToX(cell);
            int topindex = cell.stackTopLevel()-1;
            cell.rotateCurrentCenter(contextRotation, xpos0, ypos0,cx,cy);
            if(topindex>=0)
                {
                for(int cindex = 0; cindex<=topindex; cindex++)
                {
                TruChip cup = cell.chipAtIndex(cindex);
                int liftYval = ((cindex-1)*SQUARESIZE)/10
					+(dolift?((liftSteps*SQUARESIZE)/(2*liftdiv))*cindex : 0);
                int liftXval =  ((cindex-1)*SQUARESIZE)/40;
                int xpos = xpos0 + liftXval;
                int ypos = ypos0 - liftYval;
                if(cup!=null)
                {	
                	if(gc!=null) 
                		{
                		cup.drawChip(gc,this,SQUARESIZE,xpos,ypos, null);
                		//this puts the region size in each cell of each region
                		//String msg = (cell!=null) ? ""+cell.region_size : null;
                		//G.Text(gc,false,xpos+SQUARESIZE/3,ypos-SQUARESIZE,SQUARESIZE,SQUARESIZE,Color.yellow,null,msg);
 
                		if(cindex==0) 
                			{ 
                			  if(cell==flipped)
                				{
                				GC.frameRect(gc,Color.red,xpos-CSQ,ypos-CSQ,CSQ*2-1,CSQ*2-1);
                				}
                			  if(cell.isBase)
                			  {GC.frameRect(gc,Color.white,xpos+(int)(SQUARESIZE*0.25),ypos-(int)(SQUARESIZE*0.6),CSQ,CSQ);
                			  }
                			}
                		}
                }
                
                
                if((cindex==topindex) && (gc!=null))
                {
                if(isARiverDest)
                	{ GC.cacheAACircle(gc,xpos+(int)(DOT_X_SCALE*SQUARESIZE),ypos-(int)(DOT_Y_SCALE*SQUARESIZE),Math.max(2,SQUARESIZE/10),Color.red,Color.red,true);
                	}
                if(isASplitDest)
            	{ GC.cacheAACircle(gc,xpos+(int)(DOT_X_SCALE*SQUARESIZE),ypos-(int)(DOT_Y_SCALE*SQUARESIZE),Math.max(2,SQUARESIZE/10),Color.yellow,Color.yellow,true);
            	}
                if(isAMergeDest)
            	{ GC.cacheAACircle(gc,xpos+(int)(DOT_X_SCALE*SQUARESIZE),ypos-(int)(DOT_Y_SCALE*SQUARESIZE),Math.max(2,SQUARESIZE/15),Color.green,Color.green,true);
            	}
                if(isAMergeSource)
            	{ GC.cacheAACircle(gc,xpos+(int)(DOT_X_SCALE*SQUARESIZE),ypos-(int)(DOT_Y_SCALE*SQUARESIZE),Math.max(2,SQUARESIZE/15),Color.blue,Color.blue,true);
            	}

                }
                
              if(highlight!=null)
              {
              boolean hitpoint = 
            		  gb.LegalToHitBoard(cell)
            		  && cell.closestPointToCell(highlight,SQUARESIZE/2, xpos, ypos)
                   			;

              if(cindex==topindex)
              {
            	if(hitpoint) 
              	{	hitCell = cell;
              	}
              else
              {	  int corner = (int)(0.4*SQUARESIZE);
              	  if(gb.LegalToHitIntersection(cell)
              			  && cell.closestPointToCell(highlight, SQUARESIZE/2,xpos+corner, ypos-corner)
            		  	)
            		  	{
              		    hitCell = cell;
             		  	} 	
              }
              }
              }
              }
             }
        }
        
 
        if(hitCell!=null)
        {	highlight.hitCode = (hitCell.chipIndex>0) ? TruId.BoardLocation : TruId.EmptyBoard;
       		highlight.arrow =hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
      		highlight.awidth = SQUARESIZE/2;
      		highlight.spriteColor = Color.red;
         }

    }
    

    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  TruGameBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      TruchetState vstate = gb.getState();
      redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
      GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
      drawBoardElements(gc, gb, boardRect, ot);
      GC.unsetRotatedContext(gc,highlight);
      
      
      boolean planned = plannedSeating();
      commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
      
      for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
      {
      commonPlayer cpl = getPlayerOrTemp(i);
      cpl.setRotatedContext(gc, highlight, false);
      DrawCommonChipPool(gc, i,chipRects[i],ot,true);
      DrawCaptures(gc,i,captureRects[i],highlight);
      if(planned && (gb.whoseTurn==i))
      {
			handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
     	
      }
      cpl.setRotatedContext(gc, highlight, true);
      }

       GC.setFont(gc,standardBoldFont());
       drawPlayerStuff(gc,(vstate==TruchetState.PUZZLE_STATE),ourSelect,
	   			HighlightColor, rackBackGroundColor);

       double messageRotation = pl.messageRotation();

		if (vstate != TruchetState.PUZZLE_STATE)
        {
			if(!planned) 
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
         }

        String msg = s.get(vstate.getDescription());
        if(G.debug()) { msg = "("+b.sm_step+")"+msg;}
        standardGameMessage(gc,messageRotation,
        		vstate==TruchetState.GAMEOVER_STATE?gameOverMessage():msg,
        				vstate!=TruchetState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        DrawCommonChipPool(gc, gb.whoseTurn,iconRect,null,false);
        goalAndProgressMessage(gc,ourSelect,s.get("Occupy 3 enemy bases"),progressRect, goalRect);
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        drawLiftRect(gc,liftRect,ourSelect,textures[LIFT_ICON_INDEX]); 
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
        if(replay!=replayMode.Replay) { playSounds((TruMovespec)mm); }
 
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
        return (new TruMovespec(st, player));
    }
    
public commonMove EditHistory(commonMove m)
{	// allow "no change" moves, which occur when you pick or drop to the pool.
	if((m.op==MOVE_PICKB)||(m.op==MOVE_PICK)) { return(m); }
	int sz = History.size()-1;
	if(sz>=1)
		{	
		TruMovespec prev = (TruMovespec)History.elementAt(sz);
		TruMovespec prev2 = (TruMovespec)History.elementAt(sz-1);
		if(((m.op==MOVE_DROP))&&(prev.op==MOVE_PICK)) 
			{ popHistoryElement();
			  return(null);
			} 
		else
		if((b.getState()==TruchetState.PUZZLE_STATE)
				&& (m.op==MOVE_DROPB) 
				&& (prev.op==MOVE_PICKB)
				&& (prev2.op==MOVE_DROPB)
				&& (prev.from_col==prev2.to_col)
				&& (prev.from_row==prev2.to_row))
			{
			popHistoryElement();
			popHistoryElement();
			}
		}
	return(EditHistory(m,true));
}
private void playSounds(TruMovespec m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_FLIP:
    	playASoundClip(scrape,50);
    	break;
    case MOVE_BOARD_BOARD:
       	playASoundClip(light_drop,50);
    	playASoundClip(heavy_drop,50);
    	break;
    case MOVE_SPLIT:
    case MOVE_MERGE:
    	{
	    	int nn = m.splitInfo.length();
	    	while(nn-- > 0)
	    	{playASoundClip(light_drop,100);
	    	}
    	}
    	break;
    case MOVE_PICK:
    case MOVE_DROP:
    case MOVE_PICKB:
    	playASoundClip(light_drop,50);
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
        if (hp.hitCode instanceof TruId) // not dragging anything yet, so maybe start
        {

        TruId hitObject = (TruId)hp.hitCode;
		TruCell cell = hitCell(hp);
		TruChip cup = (cell==null) ? null : cell.topChip();
		if(cup!=null)
		{
	    switch(hitObject)
	    {
        case Black_Captures:
        case White_Captures:
	    case Black_Chip_Pool:
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick "+hitObject.shortName);
	    	break;

	    case EmptyBoard:
	    	PerformAndTransmit("Flip "+ cell.col+" "+cell.row);
	    	break;
	    case BoardLocation:
	    	PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+cup.chipNumber);
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
    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof TruId)) {  missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
    	TruId hitObject = (TruId)hp.hitCode;
        TruchetState state = b.getState();
		TruCell cell = hitCell(hp);
		TruChip cup = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case EmptyBoard:
        case BoardLocation:	// we hit the board 
		 switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case M_STATE:
			case M_DONE_STATE:
			case S_STATE:
			case MSM_STATE: 
			case CONFIRM_STATE:
			case STARTM_STATE:
			case STARTS_STATE:
			case STARTSM_STATE:
			case SORM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if((cup!=null)&&(hitObject!=TruId.EmptyBoard))
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+cup.chipNumber);
				}
				break;
			}
			break;
        case Black_Captures:
        case White_Captures:
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col = hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default:throw  G.Error("can't drop on rack in state %s",state);
                	case PLAY_STATE:
            		performReset();
            		break;

               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop "+col+" "+cell.row+" "+mov);
            		break;
            	}
			}
         	}
            break;

        }
        }
    }


    public String gameType() { return(""+b.gametype+" "+b.randomKey); }
    public String sgfGameType() { return(Truchet_SGF); }

    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        long rk = G.LongToken(his);
        b.doInit(token,rk);
     }
    
   
//    public void doShowText()
//    {
//        if ( debug)
//        {
//            super.doShowText();
//        }
//        else
//        {
//            theChat.postMessage(GAMECHANNEL,KEYWORD_CHAT,
//                s.get(CensoredGameRecordString));
//        }
//    }


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new TruPlay()); }

    public boolean replayStandardProps(String name,String value)
    {	nextIntCompatabilityKludge(b,name,value,"Dec 6 2009");
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
            {	StringTokenizer tok = new StringTokenizer(value);
            	String gametype = tok.nextToken();
            	long rk = G.LongToken(tok);
                b.doInit(gametype,rk);
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if(parsePlayerCommand(name,value)) {}
            else if (parseVersionCommand(name,value,2)) {}
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

