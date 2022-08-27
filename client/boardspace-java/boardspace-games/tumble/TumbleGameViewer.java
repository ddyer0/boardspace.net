package tumble;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;

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
 * April 2010 added ReverseView option
*/
public class TumbleGameViewer extends CCanvas<TumbleCell,TumbleBoard> implements TumbleConstants, GameLayoutClient
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
    
 
    // images
    private static Image[] images = null; // images of black and white gobblets and board
    private static Image[] textures = null;// background textures
    
    // private state
    private TumbleBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle reverseRect = addRect("reverse");
    private Rectangle chipRects[] = addRect("chip",2);
    
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    private Color ZoomHighlightColor = new Color(150,195,166);
    private Rectangle repRect = addRect("repRect");
    private Rectangle logoRect = addRect("logoRect");
    private JCheckBoxMenuItem reverseOption = null;

    

    public synchronized void preloadImages()
    {	TumbleChip.preloadImages(loader,ImageDir);
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
    {	enableAutoDone = true;
        super.init(info,frame);

        zoomRect = addSlider(TileSizeMessage,s.get(StackHeightMessage),TumbleId.ZoomSlider);
        zoomRect.min=MIN_CHIP_SCALE;
        zoomRect.max=MAX_CHIP_SCALE;
        zoomRect.value=INITIAL_CHIP_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = ZoomHighlightColor;
       
        b = new TumbleBoard(info.getString(GAMETYPE, "Tumblingdown"),
        		getStartingColorMap());
        useDirectDrawing(true); // not tested yet
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        
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
    	int chipW = unitsize*4;
    	int doneW = unitsize*6;
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, chipW, chipW);
    	G.SetRect(done, x, y+chipW, doneW, plannedSeating()?doneW/2:0);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
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
        int margin = fh/2;
        int buttonW = fh*8;
        int nrows = b.boardSize;
         	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	//
    			0.75,	// 50% of space allocated to the board
    			1.0,	// 1:1 aspect ratio for the board
    			fh*2.5,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
        boolean rotate = seatingFaceToFaceRotated();
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);
       	layout.placeRectangle(logoRect, minLogH, minLogH,BoxAlignment.Edge);
 
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int stateH = fh*3;
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/nrows,(double)mainH/(nrows+0.5));
    	int CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(nrows*CELLSIZE);
    	int boardH = boardW;
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-stateH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH+stateH/2;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH/2;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,liftRect,reverseRect,noChatRect);
        
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	if(rotate)
    	{
    		contextRotation = -Math.PI/2;
    	}
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH/2,boardW,stateH);    
    	G.placeRight(goalRect,zoomRect,CELLSIZE*2);
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
 	
    }

    private void DrawLiftRect(Graphics gc,HitPoint highlight)
    {  	
    	drawLiftRect(gc,liftRect,highlight,textures[LIFT_ICON_INDEX]); 
		GC.frameRect(gc,Color.black,liftRect);
    }
	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, int forPlayer, Rectangle r, int player,
        HitPoint highlight)
    {	
        boolean canhit = G.pointInRect(highlight, r) && b.LegalToHitChips(forPlayer) ;
        boolean canDrop = hasMovingObject(highlight);
        int cellW = G.Width(r);
        {
        TumbleCell hitCell = b.playerChipPool[forPlayer];
        TumbleChip thisChip = hitCell.topChip();
        	{	
        		int left = G.Left(r);
	    		int top = G.Top(r);
	       		if(canhit 
	       				&& G.pointInRect(highlight,left,top,cellW,SQUARESIZE)
	       				&& (canDrop ? b.canDropOn(hitCell) : (thisChip!=null))
	       				)
	    		{ highlight.hitObject = hitCell;
	     		  highlight.hitCode = hitCell.rackLocation();
	     		  highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
	     		  highlight.awidth = SQUARESIZE;
	    		}
        		if(thisChip!=null)
        		{
        		thisChip.drawChip(gc,this,r,null);
 	       		}
        	}
        }

     }

    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	TumbleChip.getChip(idx).drawChip(g,this,SQUARESIZE,xp,yp,null);
    }



    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean review = reviewMode() && !mutable_game_record;
      // erase
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //G.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      b.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
      b.SetDisplayRectangle(boardRect);
        
       // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], 
      //	  brect.x,brect.y,brect.width,brect.height,this);

      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, TumbleBoard gb, Rectangle brect, HitPoint highlight)
    {
       	Hashtable<TumbleCell,TumbleCell> dests = gb.getMoveDests();
       	double yscale = zoomRect.value;
     	boolean dolift = doLiftAnimation();
     	int dotsize = Math.max(2,SQUARESIZE/15);
  
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	boolean ftf = seatingFaceToFace();
     	Enumeration<TumbleCell>cells = gb.getIterator(ftf?Itype.BTRL : Itype.RLTB);
     	int top =  G.Bottom(brect);
     	int left = G.Left(brect);
     	while(cells.hasMoreElements())
       	{	
     		TumbleCell cell = cells.nextElement();
            boolean isADest = dests.get(cell)!=null;
            int ypos = top - gb.cellToY(cell);
            int xpos = left + gb.cellToX(cell);
            String kk = cell.isAKing ? "K " : ""; 
            if((cell.chipIndex>1) && (dolift || (yscale<=(MIN_CHIP_SCALE+0.01))))
            {	kk += cell.chipIndex;
            }
            if(cell.drawStack(gc,this, !dolift && gb.LegalToHitBoard(cell)?highlight:null, SQUARESIZE,xpos,ypos,liftSteps,
            		ftf ? yscale : 0,
            		ftf ? 0 : yscale,
            		kk))
            		{
            		highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
            		highlight.awidth = SQUARESIZE/2;
            		highlight.spriteColor = Color.red;
            		}
	            if(isADest) 
	            {GC.cacheAACircle(gc,xpos+dotsize,ypos,dotsize,Color.red,Color.gray,true);
	            }
        	}

    }
    
	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	TumbleChip king = TumbleChip.getChip(b.reverseY()?1:0);
    	TumbleChip reverse = TumbleChip.getChip(b.reverseY()?0:1);
    	GC.frameRect(gc,Color.black,r);
    	int w = (int)(0.8*G.Width(r));
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	reverse.drawChip(gc,this,w,cx,cy-w/4,null);
    	king.drawChip(gc,this,w,cx,cy+w/4 ,null);
    	HitPoint.setHelpText(highlight,r,TumbleId.ReverseViewButton, s.get(ReverseViewExplanation));
     }  
    public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  DrawLiftRect(gc,highlight);
       DrawReverseMarker(gc,reverseRect,highlight);
    
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  TumbleBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      TumbleState vstate = gb.getState();
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
       DrawCommonChipPool(gc, i,chipRects[i], gb.whoseTurn,ot);
       if(planned && (gb.whoseTurn==i))
       {
			handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
      	
       }
       cpl.setRotatedContext(gc, highlight, true);
       }

       GC.setFont(gc,standardBoldFont());
       drawPlayerStuff(gc,(vstate==TumbleState.PUZZLE_STATE),ourSelect,
	   			HighlightColor, rackBackGroundColor);


		double messageRotation = pl.messageRotation();
		if (vstate != TumbleState.PUZZLE_STATE)
        {
			if(!planned && !autoDoneActive()) 
			{ handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
			}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
         }

		standardGameMessage(gc,messageRotation,
        		vstate==TumbleState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=TumbleState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
		gb.playerChip[gb.whoseTurn].drawChip(gc, this, iconRect, null);
        goalAndProgressMessage(gc,ourSelect,s.get("Capture your opponent's tallest king stack"),progressRect, goalRect);
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        drawAuxControls(gc,ourSelect);
        zoomRect.draw(gc,ourSelect);
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

        if(replay!=replayMode.Replay) { playSounds((TumbleMovespec)mm); }
 
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
        return (new TumbleMovespec(st, player));
    }
    



private void playSounds(TumbleMovespec m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_DROPB:
    case MOVE_BOARD_BOARD:
    	{ 
    	  int h = m.originalHeight;
    	  while(h-- > 0) { playASoundClip(light_drop,50); }
    	}
    	break;
    case MOVE_DROP:
    case MOVE_PICKB:
    case MOVE_PICK:
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
        if (hp.hitCode instanceof TumbleId)// not dragging anything yet, so maybe start
        {

       	TumbleId hitObject = (TumbleId)hp.hitCode;
		TumbleCell cell = hitCell(hp);
		TumbleChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
        case Black_Chip_Pool:
        	PerformAndTransmit("Pick B "+cell.row+" "+chip.chipNumber);
        	break;
        case White_Chip_Pool:
        	PerformAndTransmit("Pick W "+cell.row+" "+chip.chipNumber);
        	break;
	    case BoardLocation:
	    	if(cell.chipIndex>0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber);
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
    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof TumbleId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
   		TumbleId hitObject =  (TumbleId)hp.hitCode;
		TumbleState state = b.getState();
		TumbleCell cell = hitCell(hp);
		TumbleChip cup = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        	
        case ReverseViewButton:
       	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
       	 generalRefresh();
       	 break;

       case ZoomSlider:
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
				else if(cup!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+cup.chipNumber);
				}
				break;
			}
			break;
			
        case Black_Chip_Pool:
        case White_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col = hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
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

    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Tumble_SGF); }

    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        b.doInit(token);
    }



    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new TumblePlay()); }


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
    
    public boolean handleDeferredEvent(Object target, String command)
    {	if(target==reverseOption)
    	{
    	b.setReverseY(reverseOption.getState());
    	generalRefresh();
    	return(true);
    	}
        return(super.handleDeferredEvent(target,command));
     }
}

