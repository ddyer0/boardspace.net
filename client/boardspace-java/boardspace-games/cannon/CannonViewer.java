package cannon;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;

import bridge.Config;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;
import lib.Graphics;
import lib.Image;
import lib.*;


/**
 * 
 * Change History
 *
 * May 2007 initial work in progress. 
 *
*/
public class CannonViewer extends CCanvas<CannonCell,CannonBoard> implements CannonConstants, GameLayoutClient
{
    // file names for jpeg images and masks
    static final String Cannon_SGF = "Cannon"; // sgf game name
    static final String ImageDir = "/cannon/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BOARD_INDEX = 0;
    static final String ImageNames[] =    { "board" };
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"};
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(170,177,200);
    private Color boardBackgroundColor = new Color(220,227,200);
    private Color chatBackgroundColor = new Color(230,230,255);
    static final String cannonSoundName = SOUNDPATH + "cannon" + Config.SoundFormat;

 
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// images with transparency
    
    // private state
    private CannonBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;
   

    public synchronized void preloadImages()
    {	
       	CannonChip.preloadImages(loader,ImageDir);
        if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        images = loader.load_masked_images(ImageDir,ImageNames);
        textures = loader.load_images(ImageDir,TextureNames);
    	}
        gameIcon = CannonChip.WhiteSoldier.image;
    }

    Color CannonMouseColors[] = { Color.white,Color.blue };
    Color CannonMouseDotColors[] = { Color.black,Color.white} ;

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	enableAutoDone = true;
        super.init(info,frame);
        MouseColors = CannonMouseColors;
        MouseDotColors = CannonMouseDotColors;
        b = new CannonBoard(info.getString(GAMETYPE, Cannon_INIT),
        		getStartingColorMap());
        useDirectDrawing(true);
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
        b.doInit(b.gametype,0L);						// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
   }
 
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rot,int unit)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	G.SetRect(chip, x, y, unit*2, unit*4);
    	Rectangle box = pl.createRectangularPictureGroup(x+unit*2+unit/2,y,unit+unit/3);
    	int doneW = plannedSeating()? unit*4 : 0;
    	G.SetRect(done, G.Right(box)+unit/2,y+unit/2,doneW,unit*2);
    	pl.displayRotation = rot;
    	G.union(box,chip,done);
    	return(box);
    }

	/**
	 * this is the main method to do layout of the board and other widgets.  I don't
	 * use swing or any other standard widget kit, or any of the standard layout managers.
	 * they just don't have the flexibility to produce the results I want.  Your mileage
	 * may vary, and of course you're free to use whatever layout and drawing methods you
	 * want to.  However, I do strongly encourage making a UI that is resizable within
	 * reasonable limits, and which has the main "board" object at the left.
	 * 
	 *  The basic layout technique used here is to start with a cell which is about the size
	 *  of a board square, and lay out all the other object relative to the board or to one
	 *  another.  The rectangles don't all have to be on grid points, and don't have to
	 *  be non-overlapping, just so long as the result generally looks good.
	 *  
	 *  When "extraactions" is available, a menu option "show rectangles" works
	 *  with the "addRect" mechanism to help visualize the layout.
	 */ 
    public void setLocalBounds(int x, int y, int width, int height)
    {   G.SetRect(fullRect, x, y, width, height);
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
	    
	    	// this does the layout of the player boxes, and leaves
		// a central hole for the board.
		//double bestPercent = 
		layout.selectLayout(this, nPlayers, width, height,
				margin,	
				0.65,	// 60% of space allocated to the board
				1.0,	// aspect ratio for the board
				fh*2,	// maximum cell size
				fh*3,
				0.3		// preference for the designated layout, if any
				);
		
	    // place the chat and log automatically, preferring to place
		// them together and not encroaching on the main rectangle.
		layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
				minLogW, minLogH, minLogW*3/2, minLogH*3/2);
	   	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
		layout.placeTheVcr(this,minLogW,minLogW*3/2);
	
	
		Rectangle main = layout.getMainRectangle();
		int mainX = G.Left(main);
		int mainY = G.Top(main);
		int mainW = G.Width(main);
		int mainH = G.Height(main);
		boolean rotate = seatingFaceToFaceRotated();
	    int nrows = rotate ? b.boardColumns : b.boardRows;
	    int ncols = rotate ? b.boardRows : b.boardColumns;
		
		// calculate a suitable cell size for the board
		double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
		CELLSIZE = (int)cs;
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
	    int stateH = fh*2;
	    G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,reverseViewRect,noChatRect);
		G.SetRect(boardRect,boardX,boardY,boardW,boardH);
		
		if(rotate)
		{
			contextRotation = -Math.PI/2;
			G.setRotation(boardRect,-Math.PI/2);
		}
		// goal and bottom ornaments, depending on the rendering can share
		// the rectangle or can be offset downward.  Remember that the grid
		// can intrude too.
		G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
	    setProgressRect(progressRect,goalRect);
	    positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);

    }
    
	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	CannonChip reverse = b.reverseY()?CannonChip.WhiteSoldier:CannonChip.BlueSoldier;
    	CannonChip king = b.reverseY()?CannonChip.BlueSoldier:CannonChip.WhiteSoldier;
    	int w = G.Width(r);
    	int t = G.Top(r);
    	int cx = G.centerX(r);
    	reverse.drawChip(gc,this,w,cx,t+w/3,null);
    	king.drawChip(gc,this,w,cx,t+w,null);
    	HitPoint.setHelpText(highlight,r,CannonId.ReverseViewButton,s.get(ReverseViewExplanation));
 
     } 

	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, CannonBoard gb,commonPlayer pl, HitPoint highlight)
    {	int forPlayer = pl.boardIndex;
    	CannonCell chips[]= gb.rack[forPlayer];
    	Rectangle r = chipRects[forPlayer];
        int center = G.Width(r)/2;
        for(int i=0;i<chips.length;i++)
        {
        	CannonCell thisCell = chips[i];	
        	CannonChip thisChip = thisCell.topChip();
        	boolean canHit = gb.LegalToHitChips(thisChip);
        	boolean canDrop = hasMovingObject(highlight);
        	boolean canPick = (thisChip!=null);
            HitPoint pt = (canHit && (canPick||canDrop))? highlight : null;
            int xp = G.Left(r)+center;
            int yp = G.Top(r)+center+i*G.Width(r);
            thisCell.drawStack(gc,this,pt,G.Width(r),xp,yp,0,0,null);
            pl.rotateCurrentCenter(gb.capturedChips[forPlayer], xp,yp);
            if((highlight!=null) && (highlight.hitObject==thisCell))
            {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
            	highlight.awidth = G.Width(r)/2;
            	highlight.spriteColor = Color.red;
            }
        	
        }
        GC.setFont(gc,largeBoldFont());
        GC.Text(gc,true,G.centerX(r),G.Top(r)+G.Height(r)/3,G.Width(r),G.Width(r),Color.black,null,""+(gb.chipsOnBoard(forPlayer)-1));
        
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
    	CannonChip ch = CannonChip.getChip(obj);// Tiles have zero offset
    	ch.drawChip(g,this,CELLSIZE,xp,yp,null);
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
    {	
       textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
        drawFixedBoard(gc);
    }
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    {	boolean backgroundReview = reviewMode() && !mutable_game_record;
    	CannonBoard gb = disB(gc);
		gb.SetDisplayParameters(0.94,0.99,  0.0,0.0,  0);
		gb.SetDisplayRectangle(brect);

      if(backgroundReview)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
	 images[BOARD_INDEX].centerImage(gc,brect);

	  gb.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, CannonBoard gb, Rectangle brect, HitPoint highlight)
    {	
     	//
        // now draw the contents of the board and anything it is pointing at
        //
    	Hashtable<CannonCell,CannonCell> dests = gb.movingObjectDests();
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left back-front order so the
        // solid parts will fall on top of existing shadows
       	Enumeration<CannonCell>cells = gb.getIterator(Itype.TBRL);
       	while(cells.hasMoreElements())
       	{
       		CannonCell cell = cells.nextElement();
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            cell.rotateCurrentCenter(gc,xpos,ypos);
            CannonChip cup = cell.topChip();
            boolean shot = (cell==gb.droppedDest)&&(cup==null);
            if(shot) { cup = gb.captured; } 
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
             if((cell.activeAnimationHeight()==0) && (cup!=null))
                {	cup.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
                }
             if(shot || (dests.get(cell)!=null))
             {	StockArt.SmallX.drawChip(gc,this,CELLSIZE*(shot?2:1),xpos,ypos,null);
             }
              if((highlight!=null)
            		&& gb.LegalToHitBoard(cell)
              		&& cell.closestPointToCell(highlight, CELLSIZE, xpos, ypos))
              	{ 
            	// draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
              	highlight.arrow =hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
              	highlight.awidth = CELLSIZE/2;
              	highlight.spriteColor = Color.red;
              	}
             }

    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  CannonBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ourTurnSelect = ourTurn ? highlight : null;	// hit if our turn
      HitPoint ourNonDragSelect = moving?null:ourTurnSelect;	// hit if our turn and not dragging
      HitPoint vcrSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
       
      CannonState vstate = gb.getState();
      redrawGameLog(gc, vcrSelect, logRect, boardBackgroundColor);
    
      GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
      drawBoardElements(gc, gb, boardRect, ourTurnSelect);
      GC.unsetRotatedContext(gc,highlight);
      
      boolean planned = plannedSeating();
      for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
      {
    	  commonPlayer pl = getPlayerOrTemp(i);
    	  pl.setRotatedContext(gc, highlight,false);
    	  DrawCommonChipPool(gc,gb, pl, ourTurnSelect);
    	  if(planned && gb.whoseTurn==i)
   	   		{
   		   handleDoneButton(gc,doneRects[i],(gb.DoneState() ? ourTurnSelect : null), 
  					HighlightColor, rackBackGroundColor);
   	   		}

    	  pl.setRotatedContext(gc, highlight,true);
      }
      commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
      double messageRotation = pl.messageRotation();
      GC.setFont(gc,standardBoldFont());
		if (vstate != CannonState.PUZZLE_STATE)
        {	if(!planned && !autoDoneActive())
        	{
			handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? ourNonDragSelect : null), 
					HighlightColor, rackBackGroundColor);
        	}
			handleEditButton(gc,messageRotation,editRect,ourNonDragSelect, highlight,HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==CannonState.PUZZLE_STATE),ourNonDragSelect,HighlightColor,rackBackGroundColor);

       
        standardGameMessage(gc,messageRotation,
            		vstate==CannonState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
            				vstate!=CannonState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
        b.playerChip[b.whoseTurn].drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,vcrSelect,s.get(GoalMessage),progressRect, goalRect);
        
        DrawReverseMarker(gc,reverseViewRect,vcrSelect);
        drawVcrGroup(vcrSelect, gc);
 
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
        
        if(replay!=replayMode.Replay) { playSounds(mm); }
        startBoardAnimations(replay,b.animationStack,CELLSIZE,MovementStyle.SequentialFromStart);
        
 
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
        return (new CannonMovespec(st, player));
    }
    

private void playSounds(commonMove mm)
{
	CannonMovespec m = (CannonMovespec) mm;

    // add the sound effects
    switch(m.op)
    {
    case MOVE_RACK_BOARD:
    case MOVE_BOARD_BOARD:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
   	break;
     case MOVE_PICK:
    	 playASoundClip(light_drop,100);
    	 break;
    case MOVE_PICKB:
    	playASoundClip(light_drop,100);
    	break;
    case MOVE_DROP:
    	break;
    case MOVE_DROPB:
    	if((b.board_state!=CannonState.PUZZLE_STATE)
    			&& (m.source==CannonId.BoardLocation)
    			&& (Math.max(Math.abs(m.to_col-m.from_col),Math.abs(m.from_row-m.to_row))>3))
    		{playASoundClip(cannonSoundName,100);
    		}
    	else {
      	 playASoundClip(heavy_drop,100);
    	}
      	break;
    case SHOOT2_BOARD_BOARD:
    case SHOOT3_BOARD_BOARD:
    	playASoundClip(cannonSoundName,100);
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
        if (hp.hitCode instanceof CannonId)// not dragging anything yet, so maybe start
        {
        CannonId hitObject = (CannonId)hp.hitCode;
		CannonCell cell = hitCell(hp);
		CannonChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case Black_Chip_Pool:
	    	PerformAndTransmit("Pick B "+cell.row);
	    	break;
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick W "+cell.row);
	    	break;
	    case BoardLocation:
    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row);
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
    public void StopDragging( HitPoint hp)
    {
       
        CellId id = hp.hitCode;
        if (!(id instanceof CannonId)) {missedOneClick = performStandardActions(hp,missedOneClick); }
    	else
    	{
    	missedOneClick = false;
		CannonState state = b.getState();
		CannonId hitObject = (CannonId)id;
		CannonCell cell = hitCell(hp);
		CannonChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
            throw G.Error("Hit Unknown: %s", hitObject);
        case ReverseViewButton:
        	{boolean v = !b.reverseY();
        	 b.setReverseY(v);
          	 reverseOption.setState(v);
           	 generalRefresh();
        	}
        	break;
         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw  G.Error("Not expecting drop on filled board in state %s",state);
			case PLACE_TOWN_STATE:
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
				}
				break;
			}
			break;
			
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
            	case PLACE_TOWN_STATE:
                	case PLAY_STATE:
            		PerformAndTransmit(RESET);
            		break;

               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop "+col+" "+cell.row+" "+mov);
            		break;
            	}
			}
         	}
            break;
        }}
    }

    public String gameType() 
    { 
   	   // in games which have a randomized start, this method would return
 	   // return(bb.gametype+" "+bb.randomKey); 
    	return(b.gametype); 
   }
    public String sgfGameType() { return(Cannon_SGF); }


    
    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
	   	//
		// in games which have a randomized start, this is the point where
		// the randomization is inserted
	    // int rk = G.IntToken(his);
		// bb.doInit(token,rk);
         b.doInit(token,0L);
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
    	if(target==reverseOption)
    	{
    	b.setReverseY( reverseOption.getState());
    	generalRefresh();
    	return(true);
    	}
    	else 
    	return(super.handleDeferredEvent(target,command));
     }
/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically
 *  */
    
 //   public void ViewerRun(int wait)
 //   {
 //       super.ViewerRun(wait);
 //   }


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new CannonPlay()); }


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
                b.doInit(value,0L);
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

