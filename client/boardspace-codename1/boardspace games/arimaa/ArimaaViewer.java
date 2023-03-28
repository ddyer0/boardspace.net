package arimaa;
/*
 * TODO: for playtable, do a no-compromise inverted piece set.
 * TODO: in face to face modes, the silver tiles are a little off center
 */
import com.codename1.ui.geom.Rectangle;
import bridge.Color;
import bridge.JCheckBoxMenuItem;

import bridge.Config;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import lib.Graphics;
import lib.Image;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.SoundManager;
import lib.StockArt;
import lib.TextButton;
import lib.CellId;
import online.common.OnlineConstants;
import online.game.BoardProtocol;
import online.game.CCanvas;
import online.game.GameLayoutClient;
import online.game.GameLayoutManager;
import online.game.NumberMenu;
import online.game.PlacementProvider;
import online.game.commonMove;
import online.game.commonPlayer;
import online.game.replayMode;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;
/**
 * 
 * Change History
 *
 * May 2007 Initial work in progress. 
 *
 * evaluator development notes
 * 
 * having fixed bonuses for rabbit advancement led to rabbits advancing for no good reason with no support.
 * removing that bonus completely led to rabbits lingering with no apparent motive to advance.
 * replaced by a sweep that follows open lanes from the goal squares, and is blocked by opposing pieces
 *  
*/
@SuppressWarnings("serial")
public class ArimaaViewer extends CCanvas<ArimaaCell,ArimaaBoard> implements ArimaaConstants, GameLayoutClient, PlacementProvider
{
    static final String Arimaa_SGF = "Arimaa"; // sgf game name

    
    // file names for jpeg images and masks
    static final String ImageDir = "/arimaa/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile"};
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color chatBackgroundColor = new Color(236,231,223);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    private Color blackArrow = new Color(0.1f,0.1f,0.0f);
    
    // sounds
    static final String sucking_sound = "/arimaa/images/Suck Up" + Config.SoundFormat;
    // images
    private static Image[] textures = null;// background textures
    
    // private state
    private ArimaaBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle firstPlayerChipRect = addRect("firstPlayerChipRect");
    private Rectangle secondPlayerChipRect = addRect("secondPlayerChipRect");
    private Rectangle chipRects[] = { firstPlayerChipRect, secondPlayerChipRect };
    private NumberMenu numberMenu = null;
    private Rectangle reverseViewRect = addZoneRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;

    private TextButton passButton = addButton(PASS,GameId.HitPassButton,ExplainPass,
			HighlightColor, rackBackGroundColor);
    private Rectangle whiteStack = addRect("whiteStack");
    private Rectangle blackStack = addRect("blackStack"); 
    private Rectangle stackRects[] = { whiteStack,blackStack };
    private Rectangle repRect = addRect("repRect");
    private boolean normalChipset = false;
    public int getAltChipset()
    {	if(normalChipset) { return(0); }
    	int n = super.getAltChipset();
    	if(b.playerColor[0]!=0)
    	{
    	switch(n)
    	{	
    	default:
    	case 0:	break;
    	case 1: n=3; break;
    	case 2: n=4; break;
    	}
    	}
    	return(n);
    }
  
    public synchronized void preloadImages()
    {	
       	ArimaaChip.preloadImages(loader,ImageDir);
        if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        SoundManager.preloadSounds(sucking_sound);
        textures = loader.load_images(ImageDir,TextureNames);
    	}
        gameIcon = ArimaaChip.getChip(0,ArimaaChip.ELEPHANT_INDEX).getImage();
    }


	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	// int players_in_game = Math.max(3,info.getInt(exHashtable.PLAYERS_IN_GAME,4));
       	// 
    	super.init(info,frame);
    	numberMenu = new NumberMenu(this,ArimaaChip.getChip(FIRST_PLAYER_INDEX),ArimaaId.ShowNumbers);
    	if(G.debug()) { ArimaaConstants.putStrings(); }

    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//
        int randomKey = sharedInfo.getInt(OnlineConstants.RANDOMSEED,-1);
        
        
        b = new ArimaaBoard(info.getString(GAMETYPE, Arimaa_Init),randomKey,
        		repeatedPositions, getStartingColorMap());
    	useDirectDrawing(true);
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        if(!seatingFaceToFace()) { currentViewset = ViewSet.Normal; }
       
     }
    /*
    private boolean setRepeatedPosition(commonMove m)
    { if(m.op==MOVE_RESIGN) { return(false); }
  	  long prev = b.recentDigest;
	  long recent = b.SubDigest();
	  if(prev!=recent)
	  	{ 
		  b.nextRecentDigest = recent;
	  	}
	  	return(recent==prev);
    }
*/
    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);						// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
   }
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int chipw = unitsize*2;
    	int rackw = unitsize*12;
    	Rectangle done = doneRects[player];
    	Rectangle chipRect = chipRects[player];
    	Rectangle rackRect = stackRects[player];
        G.SetRect(chipRect,	x,	y,	chipw,	chipw+chipw/2);       
        Rectangle box = pl.createRectangularPictureGroup(x+chipw,y,2*unitsize/3);
    	int doneW = plannedSeating()? unitsize*3 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.SetRect(rackRect,x,G.Bottom(box),rackw,unitsize*3);

    	G.union(box, done,chipRect,rackRect);
    	pl.displayRotation = rotation;
    	return(box);
    }
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*25;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int vcrW = fh*16;
        int margin = fh/2;
        int buttonW = fh*9;
        int nRows = 8;  // b.boardRows
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			getGlobalZoom(),margin,	
    			0.6,	// 60% of space allocated to the board
    			1,	// aspect ratio for the board
    			fh*2,fh*2.5,	// maximum cell size
    			0.3		// preference for the designated layout, if any
    			);
    	
       	boolean rotate = seatingFaceToFaceRotated();
       // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
    	layout.placeTheVcr(this,vcrW,vcrW*3/2);
       	int doneW = G.Width(editRect);
       	layout.placeRectangle(passButton, doneW,doneW/2,BoxAlignment.Center);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int stateH = fh*5/2;
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/nRows,(double)(mainH)/nRows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(nRows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardW)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardW;
    	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,viewsetRect,annotationMenu,numberMenu,reverseViewRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardW);
    	if(rotate)
    	{
    		contextRotation = -Math.PI/2;
    	}
        
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        }

    public boolean reversed()
    {
    	return(b.reverseY());
    }
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	ArimaaChip king = ArimaaChip.getChip(reversed()?1:0,ArimaaChip.ELEPHANT_INDEX);
    	ArimaaChip reverse = ArimaaChip.getChip(reversed()?0:1,ArimaaChip.ELEPHANT_INDEX);
    	int w = G.Width(r);
    	int cx = G.centerX(r);
    	int t = G.Top(r);
    	reverse.drawChip(gc,this,w,cx,t+w/3,null);
    	king.drawChip(gc,this,w,cx,t+2*w/3,null);
    	HitPoint.setHelpText(highlight,r,ArimaaId.ReverseViewButton,s.get(ReverseViewExplanation));
     }

    private void DrawSampleChip(Graphics gc, ArimaaBoard gb,int forPlayer, Rectangle r)
    {	ArimaaCell chips[]= gb.rack[forPlayer];
        ArimaaCell thisCell = chips[0];
       thisCell.drawStack(gc,this,null,G.Width(r),G.centerX(r),G.centerY(r),0,0,null);
     }


   private void DrawChipRack(Graphics gc, int forPlayer,ArimaaBoard gb,Rectangle r, int player, HitPoint highlight)
    {	
    	normalChipset = true;
    	ArimaaCell chips[]= gb.rack[forPlayer];
    	int div = chips.length-1;
        boolean canHit = gb.LegalToHitChips(forPlayer);
        boolean canDrop = hasMovingObject(highlight);
        boolean puzzle = gb.getState()==ArimaaState.PUZZLE_STATE;
        String msg = "";
        boolean hasNonRabbits = false;
        boolean hasRabbits = false;
        int xstep = G.Width(r)/div;
        int top = G.centerY(r);
        ArimaaCell srcCell = gb.getSource();
        for(int i=1,last=chips.length; i<last;i++)
        {
        ArimaaCell thisCell = chips[i];
        ArimaaChip thisChip = thisCell.topChip();
        boolean canPick = !canDrop && (thisChip!=null) ;
        HitPoint pt = ((canHit && canPick)||(canDrop && ((thisCell==srcCell)||puzzle)))? highlight : null; 
        if(thisChip!=null)
        {
        	if(thisChip.chipType()==ArimaaChip.RABBIT_INDEX) 
        		{ hasRabbits=true; }
        		else { hasNonRabbits=true; }
         }
        int left = G.Left(r)-xstep/2+i*xstep;
        if(thisCell.drawStack(gc,this,pt,xstep,left,top,0, 0.1,msg))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = xstep;
        	highlight.spriteColor = Color.red;
        }
        }
        if(hasRabbits && !hasNonRabbits && (gb.pickedObject==null) && (gb.getState()==ArimaaState.INITIAL_SETUP_STATE))
        	{
        	Rectangle rr = new Rectangle(G.Left(r)+G.Width(r)/3,G.Top(r)+G.Height(r)/3,G.Width(r)/3,G.Height(r)/3);
            if (GC.handleRoundButton(gc,rr, 
            		highlight, s.get(PlaceRabbits),
            		HighlightColor, rackBackGroundColor))
            {	// always display the done button, but only make it active in
            	// the appropriate states
            	highlight.hitCode = ArimaaId.HitPlaceRabbitsButton;
            }
        	}
        normalChipset = false;
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
    	ArimaaChip ch = ArimaaChip.getChip(obj);// Tiles have zero offset
    	ch.drawChip(g,this,CELLSIZE,xp,yp,null);
     }

    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean backgroundReview = reviewMode() && !mutable_game_record;
      // erase
      ArimaaBoard gb = disB(gc);
      setDisplayParameters(gb,boardRect);
      GC.setColor(gc,backgroundReview ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(backgroundReview)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
      GC.setRotatedContext(gc,boardRect,null,contextRotation);
      for(ArimaaCell c = gb.allCells;  c!=null; c=c.next)
      {
    	  int ypos = G.Bottom(boardRect) - gb.cellToY(c);
          int xpos = G.Left(boardRect) + gb.cellToX(c);
          ArimaaChip base = c.chipAtIndex(0);
          c.drawChip(gc,this,base,null,CELLSIZE,xpos,ypos,null  );
      }
      GC.unsetRotatedContext(gc,null);
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);

      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
 
    private void setDisplayParameters(ArimaaBoard gb,Rectangle r)
    {
	    gb.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
	    gb.SetDisplayRectangle(r);
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, ArimaaBoard gb, Rectangle brect, HitPoint highlight)
    {
     	//
        // now draw the contents of the board and anything it is pointing at
        //
        //setDisplayParameters(gb,boardRect);
    	numberMenu.clearSequenceNumbers();
     	
        boolean moving = gb.pickedObject!=null;
        Hashtable<ArimaaCell,ArimaaCell> targets = moving?gb.getDests():gb.getSources();
        Hashtable<ArimaaCell,ArimaaCell> steps = gb.getSteps();
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left back-front order so the
        // solid parts will fall on top of existing shadows
        gb.scoreTraps();
        for(ArimaaCell c=gb.allCells; c!=null; c=c.next) { c.runwayScore=-1; }
        gb.markRunways(gb.whoseTurn);
        double tmap[][] = gb.currentTrapMap[gb.whoseTurn];
        
       	Enumeration<ArimaaCell>cells = gb.getIterator(Itype.TBRL);
       	while(cells.hasMoreElements())
       	{
       		ArimaaCell cell = cells.nextElement();
       		int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            Color co = cell.lastEmptiedPlayer==0 ? labelColor : blackArrow;
            numberMenu.saveSequenceNumber(cell,xpos,ypos,co);
            if(cell.auxDisplay.lastEmptyMoveNumber==cell.lastEmptyMoveNumber)
            {
            	numberMenu.saveSequenceNumber(cell.auxDisplay,xpos,ypos,co);
            }
            boolean can = gb.legalToHitBoard(cell) && ((targets==null)||(targets.get(cell)!=null));
            boolean isAStep = (steps.get(cell)!=null);
            ArimaaChip top = cell.topChip();
            String msg = null;
            if(tmap!=null)
            {
            	//msg = ""+(int)(tmap[cell.col-'A'][cell.row-1]*100);
            	//msg = ""+cell.runwayScore;
            }
            if(top==null)
            {	if(isAStep) { StockArt.Dot.drawChip(gc,this,2*CELLSIZE/3,xpos,ypos,null); }
            }
            else 
            {	if(isAStep) 
            	{ ArimaaChip.getTile(ArimaaChip.GLOW_TILE_INDEX).drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            	}
            	if(gb.isFrozen(cell)) 
            		{
            		ArimaaChip.getTile(ArimaaChip.BLUE_GLOW_INDEX).drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            		}
            	
            	
                //cell.drawStack(gc,this,top,can?highlight:null,SQUARESIZE,xpos,ypos, msg);
             }
            if(cell.activeAnimationHeight()==0)
            	{
            		cell.drawChip(gc,this,top,can?highlight:null,CELLSIZE,xpos,ypos, msg);
            	}
            boolean dead = gb.isDead(cell);
            ArimaaChip ch = gb.getPieceCaptured(cell);
            if(ch!=null) 
            	{ ch.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            	  dead = true;
            	}
            if(dead) { StockArt.SmallX.drawChip(gc,this,2*CELLSIZE/3,xpos,ypos,null); }
        	}
    	
       	numberMenu.drawSequenceNumbers(gc,CELLSIZE,labelFont,labelColor);
  
        if((highlight!=null)&&(highlight.hitObject!=null))
        {	// draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
      		highlight.awidth = CELLSIZE/2;
      		highlight.arrow = hasMovingObject(highlight) 
      			? StockArt.DownArrow 
      			: StockArt.UpArrow;
      		highlight.spriteColor = Color.red;
        }

    }
    public void drawPlayerStuff(Graphics gc,ArimaaBoard gb,int idx,boolean drawAsButton,
    		HitPoint nonDragSelect,HitPoint dragSelect)
    {	commonPlayer player = getPlayerOrTemp(idx);
    	drawPlayerStuff(gc,player,drawAsButton,nonDragSelect,HighlightColor,rackBackGroundColor);
    	player.setRotatedContext(gc, dragSelect,false);
    	Rectangle done = doneRects[idx];
    	Rectangle chip = chipRects[idx];
    	Rectangle rack = stackRects[idx];
        DrawSampleChip(gc,gb, idx,chip);
        DrawChipRack(gc,  idx,gb,rack,gb.whoseTurn,dragSelect);
        if(plannedSeating() && (gb.whoseTurn==player.boardIndex)) { handleDoneButton(gc,done,(gb.DoneState()? nonDragSelect : null),HighlightColor, rackBackGroundColor); }
       	player.setRotatedContext(gc, dragSelect,true);

    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    { ArimaaBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      boolean facingMode = seatingFaceToFace();
      HitPoint ourTurnSelect = ourTurn ? highlight : null;	// hit if our turn
      HitPoint ourNonDragSelect = moving?null:ourTurnSelect;	// hit if our turn and not dragging
      HitPoint vcrSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
       ArimaaState vstate = gb.getState();
       gameLog.redrawGameLog2(gc, vcrSelect, logRect, Color.black, boardBackgroundColor,
    		  standardBoldFont(),standardPlainFont());
    
      GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
      drawBoardElements(gc, gb, boardRect, ourTurnSelect);
      GC.unsetRotatedContext(gc,highlight);

      DrawReverseMarker(gc,reverseViewRect,vcrSelect);
      if(facingMode||G.debug()) { drawViewsetMarker(gc,viewsetRect,vcrSelect); }
      numberMenu.draw(gc,vcrSelect);
      boolean planned = plannedSeating();
	  for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX;i++)
		  {drawPlayerStuff(gc,gb,i,(vstate==ArimaaState.PUZZLE_STATE),
				  	ourNonDragSelect,ourTurnSelect);
		  }
      
        GC.setFont(gc,standardBoldFont());
      commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
      double messageRotation = pl.messageRotation();
		if (vstate != ArimaaState.PUZZLE_STATE)
        {	
        	if(!planned) { handleDoneButton(gc,doneRect,(gb.DoneState()? ourNonDragSelect : null),HighlightColor, rackBackGroundColor); }
        	handleEditButton(gc,messageRotation,editRect,ourNonDragSelect,highlight,HighlightColor, rackBackGroundColor);
            if(gb.started 
            		&& ((vstate==ArimaaState.PLAY_STATE)||(vstate==ArimaaState.CONFIRM_STATE)) 
            		&& (gb.playStep<4) && (gb.playStep>0))
            {
            	passButton.show(gc,messageRotation,ourNonDragSelect);         
            }
                }

        	String pmsg = vstate.getDescription();
        	String msg = ((vstate==ArimaaState.PLAY_STATE)||(vstate==ArimaaState.PUSH_STATE)||(vstate==ArimaaState.PULL_STATE)||(vstate==ArimaaState.PUSHPULL_STATE))
        			?s.get(pmsg,""+(gb.playStep+1))
        			:s.get(pmsg);
    	String stateMessage = vstate==ArimaaState.GAMEOVER_STATE?gameOverMessage():msg;
        standardGameMessage(gc,messageRotation,
        		stateMessage,
            		vstate!=ArimaaState.PUZZLE_STATE,
            		gb.whoseTurn,
            		stateRect);
        DrawSampleChip(gc,gb,gb.whoseTurn,iconRect);
        goalAndProgressMessage(gc,vcrSelect,s.get(ArimaaGoal),progressRect, goalRect);
        
        DrawRepRect(gc,pl.displayRotation,Color.black,gb.Digest(),repRect);
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
    {	numberMenu.recordSequenceNumber(b.moveNumber);
        handleExecute(b,mm,replay);
        startBoardAnimations(replay);
        if(replay!=replayMode.Replay) { playSounds(mm); }
 
        return (true);
    }
     
     private void startBoardAnimations(replayMode replay)
     {	try {
        if(replay!=replayMode.Replay)
     	{	
     		CellStack anim = b.animationStack;
     		boolean place = getCurrentMoveOp()==MOVE_PLACE_RABBITS;
     		double now = 0.0;
     		int sz = anim.size();
     		for(int i=0;i<sz;i+=2)
     		{
     		ArimaaCell dest = anim.elementAt(i+1);
     		ArimaaCell src = anim.elementAt(i);
     		double speed = masterAnimationSpeed*(src.onBoard|place ? 0.5 : 0.2);
       		ArimaaChip top = dest.topChip();
       		if((top==null)&&(sz>=4) && dest.isTrap)		// something being captured
       		{	ArimaaCell fin = b.animationStack.elementAt(sz-1);
       			top = fin.topChip();
       		}
     		startAnimation(src,dest,top,CELLSIZE,now,speed);
     		if((sz>=4) && (i==0) && (src.topChip()!=null))
     		{	ArimaaCell stand = anim.elementAt(i+2);	// push or pull move
     			// for a push/pull animation a pause of the origin chip
     			startAnimation(stand,stand,src.topChip(),CELLSIZE,now,speed);
     		}
     		if(!place) { now += speed; }
     		}
     	}}
     	finally {
        	b.animationStack.clear();
     	}
     }


/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new ArimaaMovespec(st, player));
    }
    

private void playSounds(commonMove mm)
{
	ArimaaMovespec m = (ArimaaMovespec) mm;

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
    case MOVE_DONE:
    case MOVE_DROP:
    	break;
    case MOVE_DROPB:
      	 playASoundClip(heavy_drop,100);
      	break;
    default: break;
    }
   	if(m.captures) { playASoundClip(sucking_sound,50); }

}

public boolean allowResetUndo()
{
	 return super.allowResetUndo() || !b.started;
}
 public boolean allowUndo()
 {	if(!b.started)
 		{
	 		return somethingIsMoving();
 		}
 	else return super.allowUndo();
 }
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof ArimaaId)// not dragging anything yet, so maybe start
        {
        ArimaaId hitObject = (ArimaaId)hp.hitCode;
		ArimaaCell cell = hitCell(hp);
		ArimaaChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{

	    switch(hitObject)
	    {
 	    case B:
	    	PerformAndTransmit("Pick B "+cell.row+" "+chip.chipNumber());
	    	break;
	    case W:
	    	PerformAndTransmit("Pick W "+cell.row+" "+chip.chipNumber());
	    	break;
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(cell.chipIndex>0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
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
    public void StopDragging( HitPoint hp)
    {
        CellId id = hp.hitCode;
       	if(!(id instanceof ArimaaId)) { missedOneClick = performStandardActions(hp,missedOneClick);}
    	else
    	{
    	missedOneClick = false;
		ArimaaState state = b.getState();
		ArimaaCell cell = hitCell(hp);
		ArimaaId hitObject = (ArimaaId)hp.hitCode;
		ArimaaChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
         case ShowNumbers:
        	numberMenu.showMenu();
        	break;
         case HitPlaceRabbitsButton:
        	 PerformAndTransmit("PlaceRabbits");
         	break;
         case ReverseViewButton:
         {
          boolean v = !b.reverseY();
          b.setReverseY(v);
       	  reverseOption.setState(v);
       	  generalRefresh("reverseView");
         }
       	  break;

         case BoardLocation:	// we hit the board 

			if(b.movingObjectIndex()>=0)
			{ if(cell!=null) { PerformAndTransmit(G.concat("Dropb ",cell.col," ",cell.row)); }
			}
			else if(chip!=null)
			{
			PerformAndTransmit(G.concat("Pickb ",cell.col," "+cell.row," ",chip.chipNumber()));
			}
			break;

			
        case W:
        case B:
        	{
        	boolean mov = b.movingObjectIndex()>=0;
        	String col =  hitObject.name();
            if(mov) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                case CONFIRM_STATE:
                case INITIAL_SETUP_STATE:
               	case PUZZLE_STATE:
            		PerformAndTransmit(G.concat("Drop ",col," ",cell.row," ",mov));
            		break;
            	}
			}
         	}
            break;

        }
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
    	return(""+b.gametype+" "+b.randomKey); 
   }
    public String sgfGameType() { return(Arimaa_SGF); }

    
    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link #gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long rk = G.LongToken(his);
    	// make the random key part of the standard initialization,
    	// even though games like checkers probably don't use it.
        b.doInit(token,rk);
    }

    

    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {	if(target==reverseOption)
    	{
    	b.setReverseY(reverseOption.getState());
    	generalRefresh("reversed");
    	return(true);
    	}
    	else if(numberMenu.selectMenu(target,this)) { return true; }
    	else 
        return(super.handleDeferredEvent(target,command));
     }

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new ArimaaPlay()); }


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
            	long ran = G.LongToken(st);
                b.doInit(typ,ran);
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

	public int getLastPlacement(boolean empty) {
		return b.placementIndex;
	}

}

