package tzaar;

import bridge.*;
import com.codename1.ui.geom.Rectangle;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import lib.Graphics;
import lib.Image;
import lib.*;
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;
import tzaar.TzaarChip.ChipColor;

/**
 * 
 * Change History
 *
 * Feb 2008 initial work. 
 *
 * 
 */
public class TzaarViewer extends CCanvas<TzaarCell,TzaarBoard> implements TzaarConstants, GameLayoutClient
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
    private TzaarBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    
    private TextButton passButton = addButton(PASS,GameId.HitPassButton,ExplainPass,
 			HighlightColor, rackBackGroundColor);
    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;

    private Rectangle scoreRect[] = addRect(".score",2);
    private Rectangle playerRack[] = addRect("rack",2);
	private Rectangle playerChip[]=  addRect("chip",2);
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    private Color ZoomHighlightColor = new Color(150,195,166);
 
    

    public synchronized void preloadImages()
    {	
    	if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    	TzaarChip.preloadImages(loader,ImageDir);
    	images = loader.load_images(ImageDir, ImageFileNames, loader.load_images(ImageDir, ImageFileNames,"-mask")); // load the main images
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
    {	enableAutoDone = true;
        super.init(info,frame);
        int randomKey = sharedInfo.getInt(OnlineConstants.RANDOMSEED,-1);
        
        zoomRect = addSlider(TileSizeMessage,s.get(StackHeightMessage),TzaarId.ZoomSlider);
        zoomRect.min=MIN_CHIP_SCALE;
        zoomRect.max=MAX_CHIP_SCALE;
        zoomRect.value=INITIAL_CHIP_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = ZoomHighlightColor;
  
        b = new TzaarBoard(randomKey,info.getString(OnlineConstants.GAMETYPE, Tzaar_Standard_Init),
        		getStartingColorMap());
        useDirectDrawing(true);
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        if(G.debug()) {
        	InternationalStrings.put(TzaarStrings);
        	InternationalStrings.put(TzaarStringPairs);
        }
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);				// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
   }

    boolean horizontal = false;
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle done = doneRects[player];
    	Rectangle score = scoreRect[player];
    	Rectangle rack = playerRack[player];
    	Rectangle chip = playerChip[player];
    	int chipW = unitsize*2;
    	int C2 = unitsize/2;
    	int scoreW = 0;//G.debug() ? unitsize*2 : 0;
    	int rackW = chipW*2;
    	int doneW = plannedSeating() ? unitsize*4 : 0;
    	int rackH = Math.max(doneW/2, unitsize*4);
    	G.SetRect(rack, x,y,rackW,rackH);
    	G.SetRect(chip, x+rackW,y,chipW,chipW);
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW+rackW,y,unitsize);
    	G.SetRect(score, G.Right(box), y, scoreW,scoreW);
    	int boxBottom = G.Bottom(box);
    	G.SetRect(done,horizontal ? G.Right(box)+scoreW+unitsize/4:x+rackW+C2,
    				horizontal ? y+unitsize/2 : boxBottom+C2,doneW,doneW/2);
    	G.union(box, done,score,rack, chip);
    	pl.displayRotation = rotation;
        return(box);
    }
    
    public void setLocalBounds(int x, int y, int width, int height)
    {	
    	setLocalBoundsV(x,y,width,height,new double[] {1,-1});
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
    {	G.SetRect(fullRect, x, y, width, height);
    	horizontal = aspect<0;
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
        int fh = standardFontSize();
    	int minLogW = fh*20;	
    	int vcrW = fh*16;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        int stateH = fh*3;
		int ncols = b.ncols;
		// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			0.8,	// aspect ratio for the board
    			fh*2,	// maximum cell size
    			fh*3,	// maximum cell size
    			0.5		// preference for the designated layout, if any
    			);
        
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW,doneRect,editRect);
       	layout.placeRectangle(passButton, buttonW, buttonW/2,BoxAlignment.Center);
    	layout.placeTheVcr(this,vcrW,vcrW*3/2);
       	
  
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);

    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/(ncols+1),(double)(mainH-2*stateH)/(ncols-1));
    	CELLSIZE = (int)cs;
    	int boardW = (ncols+1)*CELLSIZE;
    	int boardH = (ncols-1)*CELLSIZE;
    	
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
        int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH;
        int stateX = boardX;
        int zoomW = stateH*5;
        G.placeRow(stateX, stateY,boardW, stateH,stateRect,viewsetRect,liftRect,reverseViewRect,noChatRect);
        G.placeRight(stateRect, zoomRect, zoomW);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
        
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        return boardW*boardH;
       }
 
        
	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	TzaarChip king = TzaarChip.getChip(b.reverseY()?1:0,FIRST_PLAYER_INDEX);
    	TzaarChip reverse = TzaarChip.getChip(b.reverseY()?0:1,SECOND_PLAYER_INDEX);
    	GC.frameRect(gc,Color.black,r);
    	int w = G.Width(r);
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	reverse.drawChip(gc,this,w,cx,cy-w/4,null);
    	king.drawChip(gc,this,w,cx,cy+w/4,null);
    	HitPoint.setHelpText(highlight,r, TzaarId.ReverseViewButton, s.get(ReverseViewExplanation));
 
     } 
    
    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	TzaarChip ic = TzaarChip.getChip(idx);
    	int scl = scaleCellSize(CELLSIZE,xp,yp,boardRect);
    	ic.drawChip(g,this,scl,xp,yp,null);
    }




    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {boolean review = reviewMode() && !mutable_game_record;
      // erase
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid
      boolean perspective = getAltChipset()==0;
     images[perspective?BOARD_INDEX:BOARD_FLAT_INDEX].centerImage(gc, boardRect);
      
      if(perspective)
    	{
    	double lr = 0;
	    	b.SetDisplayParameters(
    		 1.13, //0.93,	// scale 
    		 0.83,	// yscale
	    		 0.0,	// xoff
	    		 -0.7,//-0.1,	// yoff
	    		 -2.0+lr,	// rot
	    		 0.14,	// xperspective
	    		 0.11,	// yperspective
	    		 0.0
	    		 );
    	}else
    	{
    	   	b.SetDisplayParameters(
    	    		 1.1, // scale 
    	    		 0.89,	// yscale
    	    		 -0.1,	// xoff
    	    		 -0.4,//-0.1,	// yoff
    	    		 0,	// rot
    	    		 0,	// xperspective
    	    		 0,	// yperspective
    	    		 0
    	    		 );

    	}
    	
	    	b.SetDisplayRectangle(boardRect);
  
      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    private void DrawScore(Graphics gc,Rectangle r,int player)
    {	
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,true,r,0,Color.black,rackBackGroundColor,""+b.ScoreForPlayer(player,false,false));
    	GC.frameRect(gc,Color.black,r);
    }
    
    private void drawRack(TzaarBoard rb,HitPoint highlight,Graphics gc,Rectangle r,Rectangle chipr,int forplayer)
    {	TzaarCell row[] = rb.placing ? rb.rack[forplayer] : rb.captures[nextPlayer[forplayer]];
     	boolean canhit = rb.LegalToHitChips(forplayer);
     	boolean perspective = getAltChipset()==0;
     	TzaarChip chip = TzaarChip.getChip(0,rb.getColorMap()[forplayer]);
     	chip.drawChip(gc, this, chipr,null);
    	int div = row.length;
    	int w = G.Width(r)/div;
    	int moving = rb.movingObjectType();
    	ChipColor movingPlayer = rb.movingObjectColor();
    	int SQUARESIZE = G.Width(r)/3;
		commonPlayer pl = getPlayerOrTemp(forplayer);
    	//G.frameRect(gc,Color.black,r);
    	for(int idx=div-1;idx>=0;idx--)
    	{	TzaarCell c = row[idx];
        	double zoom = zoomRect.value;
        	int height = c.chipIndex+1;
        	String msg = ((height>1) && (lifting || (zoom<=(MIN_CHIP_SCALE+0.01))))
        				? (""+height)
        				: null;
   		boolean canhitthis = ((moving>=0) 
    								? (canhit 
    										&& (movingPlayer==b.playerColor[forplayer]) 
    										&& (idx==moving)) 
    								: canhit); 
   		int xp = G.Left(r)+idx*w+SQUARESIZE/2;
   		int yp = G.Top(r)+G.Height(r)-SQUARESIZE/2;
   		pl.rotateCurrentCenter(c,xp,yp);
   		if(canhitthis && (moving>=0) && (c.height()==0))
   		{	StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xp,yp,null);
   		}
    
    	if(c.drawStack(gc,this,canhitthis?highlight:null,SQUARESIZE,
    				xp,yp,liftSteps,
    					perspective?0:zoomRect.value*0.3,perspective?zoomRect.value*0.6:0,msg))
    		{
    		highlight.arrow = (b.pickedObject==null)?StockArt.UpArrow:StockArt.DownArrow;
    		highlight.awidth = SQUARESIZE/2;
    		highlight.spriteColor = Color.red;
    		}
              		
    	}
//    	gc,highlight,secondPlayerRack.x+secondPlayerRack.width/2,secondPlayerRack.y,
//			this,rb.LegalToHitChips(SECOND_PLAYER_INDEX),0,SQUARESIZE,INITIAL_CHIP_SCALE)	
    }
    
    private int scaleCellSize(int cellsize,int x,int y,Rectangle r)
    {	if(G.pointInRect(x,y,r))
    	{ double scl = (((y-G.Top(r))*0.2)/G.Height(r))+0.9;
    	  int cs = (int)(cellsize*scl);
    	  return(cs);
    	}
      return(cellsize);
     }
   /* draw the board and the chips on it. */
    private boolean lifting = false;
    private void drawBoardElements(Graphics gc, TzaarBoard rb, Rectangle brect, HitPoint highlight)
    {	Hashtable<TzaarCell,TzaarCell> dests = rb.getMoveDests();
     	lifting = doLiftAnimation();
     	int dotsize = Math.max(2,CELLSIZE/15);
     	
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	boolean perspective = getAltChipset()==0;
       	Enumeration<TzaarCell>cells = rb.getIterator(Itype.TBRL);
       	while(cells.hasMoreElements())
        	{
       		TzaarCell cell = cells.nextElement();

            //if(cell!=rb.center)
            {
            boolean isADest = dests.get(cell)!=null;
            boolean isSource = rb.isSource(cell);
            boolean canHit = !lifting && rb.LegalToHitBoard(cell);
            int ypos = G.Bottom(brect) - rb.cellToY(cell);
            int xpos = G.Left(brect) + rb.cellToX(cell);
            double zoom = zoomRect.value;
            int height = cell.chipIndex+1;
            String msg = ((height>1) && (lifting || (zoom<=(MIN_CHIP_SCALE+0.01))))
            				? (""+height)
            				: null;
            int scl = scaleCellSize(CELLSIZE,xpos,ypos,boardRect);
            if(cell.drawStack(gc,this,canHit?highlight:null,scl,xpos,ypos,liftSteps,
            		perspective?0:zoom*0.5,
            		perspective?zoom:0,msg))
            {
            	highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;;
            	int box = 3*CELLSIZE/7;
            	highlight.spriteRect = new Rectangle(xpos-(int)(box*0.4),ypos-box/2,box,box);
            	highlight.awidth = box;
            	highlight.spriteColor = Color.red;
            }
            if(gc!=null)
            	{
            	// temp for grid setup
            	//G.DrawAACircle(gc,xpos+dotsize,ypos,dotsize,Color.red,Color.gray,true);
            	if(isSource)
            	{	GC.cacheAACircle(gc,xpos+dotsize,ypos,dotsize,Color.blue,Color.gray,true);
            	}
            	if(isADest)
	        	{
	        		GC.cacheAACircle(gc,xpos+dotsize,ypos,dotsize,Color.red,Color.gray,true);
	        	}
             	
	        	}
        }}
    }
    
    public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  drawLiftRect(gc,liftRect,highlight,textures[LIFT_ICON_INDEX]); 
       DrawReverseMarker(gc,reverseViewRect,highlight);

     }


    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  TzaarBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      TzaarState vstate = gb.getState();

      gameLog.redrawGameLog(gc, ourSelect, logRect,Color.black, boardBackgroundColor,largeBoldFont(),largeBoldFont());
    
  
       drawBoardElements(gc, gb, boardRect, ot);
       GC.setFont(gc,standardBoldFont());
       drawPlayerStuff(gc,(vstate==TzaarState.PUZZLE_STATE),ourSelect,
	   			HighlightColor, rackBackGroundColor);
       boolean planned = plannedSeating();
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();

       for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX;i++)
      	{
      	commonPlayer pl0 = getPlayerOrTemp(i);
   		pl0.setRotatedContext(gc, highlight, false);
      	drawRack(gb,ot,gc,playerRack[i],playerChip[i],i);
      	if(G.debug()) { DrawScore(gc,scoreRect[i],i); }
      	if(planned && i==gb.whoseTurn) { handleDoneButton(gc,doneRects[i],(b.DoneState()? select : null),HighlightColor, rackBackGroundColor); }
   		pl0.setRotatedContext(gc, highlight, true);
      	}
       

		if (vstate != TzaarState.PUZZLE_STATE)
        {	if(!planned && !autoDoneActive())
        		{ handleDoneButton(gc,messageRotation,doneRect,(b.DoneState()? select : null),HighlightColor, rackBackGroundColor); 
        		}
        	handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
        	
            boolean ispassed = ((vstate==TzaarState.CONFIRM_STATE)&&gb.isPassed());
            if ((vstate==TzaarState.PLAY_STATE) || ispassed)
            {	passButton.show(gc,messageRotation,select);
            }
     
         }

        standardGameMessage(gc,messageRotation,
        		vstate==TzaarState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=TzaarState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);

        // no repetitions are possible in tzaar
        // DrawRepRect(gc,b.Digest(),repRect);
        drawAuxControls(gc,ourSelect);
    	zoomRect.draw(gc,ourSelect);
        drawVcrGroup(ourSelect, gc);
        drawViewsetMarker(gc,viewsetRect,ourSelect);
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
  
        startBoardAnimations(replay,b.animationStack,
        					CELLSIZE,MovementStyle.Stack);
               
        if(replay!=replayMode.Replay) { playSounds((TzaarMovespec)mm); }
 
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
        return (new TzaarMovespec(st, player));
    }
    

private void playSounds(TzaarMovespec m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_DROPB:
    case MOVE_DROPCAP:
    case CAPTURE_BOARD_BOARD:
    case MOVE_BOARD_BOARD:
    	{ int h = m.undoInfo/100;
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
        if (hp.hitCode instanceof TzaarId) // not dragging anything yet, so maybe start
        {

        TzaarId hitObject = (TzaarId)hp.hitCode;
		TzaarCell cell = hitCell(hp);
		TzaarChip chip = (cell==null) ? null : cell.topChip();
		
        if(chip!=null)
		{
	    switch(hitObject)
	    {
        case ZoomSlider:
        	break;
        case First_Player_Rack:
        case Second_Player_Rack:
        	PerformAndTransmit("Pick "+hitObject.shortName+" "+cell.row);
        	break;
	    case BoardLocation:
	    	if(cell.chipIndex>=0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.typeIndex);
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
    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof TzaarId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	TzaarId hitObject = (TzaarId)hp.hitCode;
		TzaarCell cell = hitCell(hp);
		TzaarChip cup = (cell==null) ? null : cell.topChip();
		TzaarState state = b.getState();	// state without resignation
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
			case PLACE_STATE:
			case PUZZLE_STATE:
			case CAPTURE_STATE:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) 
					{ TzaarChip top = cell.topChip();
					  String opname = (top!=null)&&(top.color!=b.movingObjectColor())
					  					? "Dropcap " : "Dropb ";
					  PerformAndTransmit(opname+cell.col+" "+cell.row); 
					}
				}
				else if(cup!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+cup.pieceNumber());
				}
				break;
			}
			break;
			
        case First_Player_Rack:
        case Second_Player_Rack:
        	{
        	int mov = b.movingObjectIndex();
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                	case PLAY_STATE:
                case PLACE_STATE:
            		performReset();
            		break;
               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop "+hitObject.shortName+" "+cell.row);
            		break;
            	}
			}
         	}
            break;
        }
        }
    }

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
    public String gameType() { return(b.gametype+" "+b.randomKey); }
    public String sgfGameType() { return(Tzaar_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
	    long rk = G.LongToken(his);
	    b.doInit(token,rk);
	}


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new TzaarPlay()); }

    public boolean replayStandardProps(String name,String value)
    {	nextIntCompatabilityKludge(b,name,value,"Oct 25 2013");
    	return(super.replayStandardProps(name,value));
    }
    public boolean parsePlayerInfo(commonPlayer p,String first,StringTokenizer tokens)
    {
    	if(exHashtable.TIME.equals(first) && b.DoneState())
    	{
    		PerformAndTransmit("Done",false,replayMode.Replay);
    	}
    	return(super.parsePlayerInfo(p, first, tokens));
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

