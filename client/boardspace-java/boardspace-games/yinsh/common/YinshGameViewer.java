package yinsh.common;

import java.awt.*;
import lib.Graphics;
import lib.Image;

import online.common.*;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;
import lib.*;

import java.util.Enumeration;
import java.util.StringTokenizer;

/*
Change History

Nov 12 2004  Iniital work in progress, support for Yinsh
July 2005        Lightspeed progress toward a working version
*/
public class YinshGameViewer extends CCanvas<YinshCell,YinshBoard> implements YinshConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;

	private static Image[] backgrounds=null;
    
    // vcr stuff
    static final Color vcrButtonColor = new Color(0.7f, 0.7f, 0.75f);

    // private state
    YinshBoard b = null; //the board from which we are displaying
    private double MASTER_SCALE = 1.0;
    private double MASTER_YSCALE = 0.9;
    private double MASTER_NP_YSCALE = 1.1;
    private double XPERSPECTIVE = 0.2;
    private double YPERSPECTIVE = 0.2;
    private boolean use_perspective() { return(getAltChipset()==0);}
    private  int CELLSIZE; //size of the layout cell
    private  int RINGRADIUS; //ring radius, about CELLSIZE/2
     
    private Rectangle chipRects[] = addRect("ring",2);
    private Rectangle capturedRects[] = addRect("captured",2);
    
    private Rectangle chipPool = addRect("chipPool");
    private final Color reviewModeBackground = new Color(1.0f, 0.90f, 0.90f);
    private final Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private final Color RingFillColor = new Color(10, 163, 190);
    private final Color LineFillColor = new Color(100, 163, 190);
    private final Color GridColor = Color.black;
    private final Color rackBackGroundColor = new Color(153, 217, 228);
    private final Color boardBackgroundColor = new Color(220, 238, 255);
    private final Color chatBackgroundColor = new Color(0.90f,0.95f,1.0f);
    
    public BoardProtocol getBoard()   {    return (b);   }

   

    public void preloadImages()
    {	YinshChip.preloadImages(loader,ImageDir);
	    if (backgrounds == null)
	    {
	         backgrounds = loader.load_images(ImageDir,backgroundNames);
	    }
	    gameIcon = backgrounds[ICON_INDEX];
    }
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
        
        b = new YinshBoard(info.getString(OnlineConstants.GAMETYPE, "Yinsh"),getStartingColorMap());
        doInit(false);
   }

    /* used when loading a new game */
    private boolean hasDones = false;
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);
        b.doInit(b.gametype);
        hasDones=false;		// for use by replayMove
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
     }

    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*12;	
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
    			0.75,	// 60% of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*3,	// maximum cell size
    			0.7		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);
       	//layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// for hex, there is no well defined "my side" and the board is quite rectangular
    	// so it's appropriate to rotate so the board uses the space available better.
    	int ncols = b.ncols+2;
        int nrows = b.nrows+2;
 	
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
        
        RINGRADIUS = CELLSIZE/2; //ball radius to work with
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,viewsetRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY+stateH,boardW,boardH);
    	G.SetRect(chipPool,boardX+boardW-CELLSIZE*2,boardY+boardH-CELLSIZE*2-stateH*2,CELLSIZE*2,CELLSIZE*2);
     	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
 	
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle cap = capturedRects[player];
    	int ringW = unitsize*8;
    	int ringH = unitsize*3;
    	G.SetRect(chip,	x,	y,	ringW,	ringH);
    	G.SetRect(cap, x, y, ringW, ringH);
    	
    	Rectangle box =  pl.createRectangularPictureGroup(x+ringW,y,unitsize);
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*4 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.union(box, done,chip,cap);
    	pl.displayRotation = rotation;
    	return(box);
    }
  
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
        double ring_yscale = use_perspective() ? YinshChip.RING_YSCALE : 1.0;
        drawChip(g, obj, xp, yp, CELLSIZE, 1.0, ring_yscale);

    }

    public void DrawChipPool(Graphics gc, Rectangle r, HitPoint highlight)
    {
        boolean canhit = b.legalToHitRing(b.whoseTurn, YinshId.Black_Chip_Pool) 
        				&& G.pointInRect(highlight, r);
        int C2 = CELLSIZE/2;
        int wi = G.Left(r) + C2;
        int bi = G.Right(r) - C2;
        int nc = b.chips[0];
        b.poolCell[0].rotateCurrentCenter(gc,G.centerX(r),G.centerY(r));
        b.poolCell[1].copyCurrentCenter(b.poolCell[0]);
        if ((gc != null) && (G.Width(r)>CELLSIZE) && (G.Height(r)>CELLSIZE))
        {
            int spacex = G.Width(r) - CELLSIZE;
            int spacey = G.Height(r) - CELLSIZE;
            int rc = YinshChip.BLACK_CHIP_INDEX;
            Random rand = new Random(4321);
            double ring_yscale = use_perspective() ? YinshChip.RING_YSCALE : 1.0;

            if (canhit)
            {
                GC.fillRect(gc, HighlightColor, r);
            }

            GC.frameRect(gc, Color.black, r);

            if (nc-- > 0)
            {
                drawChip(gc, YinshChip.WHITE_CHIP_INDEX, wi, G.Top(r) + C2,
                    CELLSIZE, 1.0, ring_yscale);
            }

            if (nc-- > 0)
            {
                drawChip(gc, YinshChip.BLACK_CHIP_INDEX, bi, G.Top(r) + C2,
                    CELLSIZE, 1.0, ring_yscale);
            }

            while (nc-- > 0)
            {
                int rx = Random.nextInt(rand,spacex);
                int ry = Random.nextInt(rand,spacey);
                rc = (YinshChip.WHITE_CHIP_INDEX == rc) ? YinshChip.BLACK_CHIP_INDEX
                                              : YinshChip.WHITE_CHIP_INDEX;

                if (rx < (spacex / 3))
                {
                    rc = YinshChip.WHITE_CHIP_INDEX;
                }

                if (rx > ((spacex * 2) / 3))
                {
                    rc = YinshChip.BLACK_CHIP_INDEX;
                }

                drawChip(gc, rc, G.Left(r) + C2 + rx,
                    G.Top(r) + C2 + ry, CELLSIZE, 1.0, ring_yscale);
            }

            GC.Text(gc, true, G.Right(r) - 20, G.Bottom(r) - 15, 20,
                15, Color.black, null, "" + b.chips[0]);
        }

        if (canhit)
        {
            switch (b.getState())
            {
            case PUZZLE_STATE:
                highlight.hitCode = (G.Left(highlight) < (G.Left(r) + (G.Width(r) / 2)))
                    ? YinshId.White_Chip_Pool : YinshId.Black_Chip_Pool;

                break;

            default:
                highlight.hitCode = b.chipPoolIndex[b.whoseTurn];
            }
        }
    }

    //
    // draw the ring cache and notice if the mouse is in the current rect.
    // also, if the moving object is a ring from this cache, decrement the count by one
    public void DrawRings(Graphics gc,commonPlayer pl, int nrings, int imageindex, Rectangle r,
        int player, HitPoint highlight, YinshId target)
    {
        int i = 0;
        boolean canhit = G.pointInRect(highlight, r) &&
            b.legalToHitRing(player, target);
        YinshCell rp = b.ringPool[player];
        pl.rotateCurrentCenter(rp,G.centerX(r),G.centerY(r));
       if (gc != null)
        {
            double ring_yscale = use_perspective() ? YinshChip.RING_YSCALE : 1.0;
            int CS = (int)(G.Width(r)*0.29);
            if (canhit)
            {
                GC.fillRect(gc, HighlightColor, r);
            }
            int C2 = CS/2;
            int C3 = 3*CS/4;
            for (i = 0; (i < nrings-rp.activeAnimationHeight()) && (i < 3); i++)
            {	
                int x = G.Left(r) + (CS * i) + C3;
                int y = G.Top(r) + C2;
                drawChip(gc, imageindex, x, y, CS, 1.0, ring_yscale);
            }

            for (i = 0; i < (nrings - 3); i++)
            {
                int x = G.Left(r) + (CS * i) + CS;
                int y = G.Top(r) + C3;
                drawChip(gc, imageindex, x, y, CS, 1.0, ring_yscale);
            }
        }

        if (canhit)
        {
            highlight.hitCode = target;
        }
    }



    /* draw the board and the balls on it. */
    public void DrawBoardElements(Graphics gc, YinshBoard gb,Rectangle brect, HitPoint highlight)
    {
        double ring_yscale = use_perspective() ? YinshChip.RING_YSCALE : 1.0;

        int height = G.Height(brect);
        boolean somehit = false;
        DrawChipPool(gc, chipPool, highlight);

        int top = G.Bottom(brect);
        int left = G.Left(brect);
        for(Enumeration<YinshCell> cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements();)
            { //where we draw the grid
        		YinshCell c = cells.nextElement();
                int ypos = top - gb.cellToY(c);
                int xpos = left + gb.cellToX(c);
                char thiscol = c.col;
                int thisrow = c.row;
                boolean hitpoint = !somehit &&
                G.pointInside(highlight, xpos, ypos, RINGRADIUS) &&
                    gb.legalToHitBoard(thiscol, thisrow);
                somehit |= hitpoint;

                boolean drawhighlight = hitpoint ||
                    gb.isDest(thiscol, thisrow) ||
                    gb.isSource(thiscol, thisrow) ||
                    gb.isRemoved(thiscol, thisrow);
                c.rotateCurrentCenter(gc,xpos,ypos);
                char piece = c.contents;

                // drawing
                double pscale = chipScale(height,ypos);
 
                //StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
                if (hitpoint)
                {
                    highlight.hitCode = (piece == Empty) ? YinshId.EmptyBoard
                                                         : YinshId.BoardLocation;
                    highlight.col = thiscol;
                    highlight.row = thisrow;
                }

                if (drawhighlight)
                { // checking for pointable position

                    if (gc != null)
                    {
                        drawChip(gc, YinshChip.SELECTION_INDEX, xpos, ypos, CELLSIZE,
                            pscale, ring_yscale);
                    }
                }

                if (gc != null && (c.activeAnimationHeight()==0))
                {
                    int pi = YinshChip.pieceIndex(piece);

                    if (pi >= 0)
                    {
                        drawChip(gc, pi, xpos, ypos, CELLSIZE, pscale,
                            ring_yscale);
                    }
                }

                // draw the actual rings and chips
                // for testing, draw a random ring under the lines
                //if(gc!=null) { int ii = (xpos*ypos^(xpos+ypos))%15; if(ii<SELECTION_INDEX) 
                //	{ drawChip(gc,ii,xpos,ypos,CELLSIZE); }}
            }
    }
    
    void drawChip(Graphics gc, int idx, int x, int y, int boxw, double scale,
        double ring_yscale)
    {	YinshChip.getChip(idx).drawChip(gc,this,(int)(boxw*scale),x,y,null);
    }

	public void drawFixedElements(Graphics gc)
    {	
	  	if (use_perspective())
	    {
	    b.SetDisplayParameters(MASTER_SCALE, MASTER_YSCALE, 0,0,0,XPERSPECTIVE, YPERSPECTIVE,0);
	    }
	    else
	    {
	        b.SetDisplayParameters(MASTER_NP_YSCALE, MASTER_NP_YSCALE, 0,0,0);
	    }
		b.SetDisplayRectangle(boardRect);    	
       backgrounds[NORMAL_BACKGROUND_INDEX].tileImage(gc,fullRect);
        boolean review = reviewMode() && !mutable_game_record;
        if(review)
        	{
           backgrounds[REVIEW_BACKGROUND_INDEX].tileImage(gc,boardRect);
        	}
 
           //draw the actual board
        Color bgcolor = review ? reviewModeBackground : boardBackgroundColor;

        b.DrawGrid(gc, boardRect, use_grid, bgcolor, RingFillColor,
                LineFillColor,GridColor);
    	
  

    }
    public void redrawBoard(Graphics gc,HitPoint hi)
    {	redrawBoard(gc,b,hi);
    }
    //
    // draw the board and balls on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, YinshBoard gb,HitPoint selectPos)
    {
        YinshState vstate = gb.getState();
        HitPoint ourTurnSelect = OurMove()?selectPos:null;
        boolean moving = hasMovingObject(selectPos);
        HitPoint buttonSelect = moving ? null : ourTurnSelect;
        HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;
        redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
        
        DrawBoardElements(gc, gb, boardRect, ourTurnSelect);
        
        boolean planned = plannedSeating();
        for(int player = FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
        {
        commonPlayer pl = getPlayerOrTemp(player);
        pl.setRotatedContext(gc,selectPos,false);
        
        DrawRings(gc,pl, gb.rings[player], b.ringImageIndex[player],
            chipRects[player], player, selectPos, b.ringCacheIndex[player]);
        
          
        DrawRings(gc,pl, gb.captured_rings[player], b.ringImageIndex[player],
            capturedRects[player], player, selectPos, b.ringCapturedIndex[player]);
        
 	   if(planned && gb.whoseTurn==player)
 	   {
 		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
 	   }
        
        pl.setRotatedContext(gc,selectPos,true);
        }
        
        GC.setFont(gc,standardBoldFont());
        
        drawPlayerStuff(gc,(vstate==YinshState.PUZZLE_STATE),nonDragSelect,
 	   			HighlightColor, rackBackGroundColor);

 
		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

		// draw the board control buttons 
        if (vstate != YinshState.PUZZLE_STATE)
        {
            GC.setFont(gc,standardBoldFont());
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos,HighlightColor, rackBackGroundColor);
		}


        standardGameMessage(gc,messageRotation,
        		vstate==YinshState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=YinshState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        drawChip(gc,gb.whoseTurn,G.centerX(iconRect),G.centerY(iconRect),G.Width(iconRect),1,1);
        		
        goalAndProgressMessage(gc,nonDragSelect,
        					b.blitz 
												? s.get("make 5 in a row to capture a ring and win")
												: s.get("make 5 in a row to capture a ring, 3 rings win"),progressRect, goalRect);

        drawVcrGroup(nonDragSelect, gc);
        drawViewsetMarker(gc,viewsetRect,nonDragSelect); 
    }

    private double chipScale(int height,int ypos)
    {
    	double pscale = use_perspective()
                ? (1.0 - ((YPERSPECTIVE * (height - ypos)) / height)) : 1.0;
        return(pscale);
    }
    public boolean Execute(commonMove m,replayMode replay)
    {   //System.out.println("e "+m);
    	handleExecute(b,m,replay);
        int size = CELLSIZE;
        int stackSize = b.animationStack.size();
    	if(stackSize>=2)
    	{
    		YinshCell c = b.animationStack.elementAt(1);
    		int pos = c.centerY() - G.Top(boardRect);
    		if(c.onBoard) { size = (int)(size*chipScale(G.Height(boardRect),pos)); }
    	}
    	MovementStyle style = (m.op==MOVE_REMOVE) ? MovementStyle.Simultaneous : MovementStyle.SequentialFromStart; 

    	startBoardAnimations(replay,b.animationStack,size,style);
    	if(replay!=replayMode.Replay) { playSounds(m); }
        return (true);
    }
    
    // this is a modified version of the standard startBoardAnimations, which knows about
    // the specific actions of drop-slide-flip that are peculiar to yinsh
    public void startBoardAnimations(replayMode replay,CellStack animationStack,int size,MovementStyle groupMovement)
    {	try {
    	boolean sequential = (groupMovement==MovementStyle.SequentialFromStart);
    	boolean fromStart = (groupMovement==MovementStyle.SequentialFromStart);
    	if(replay!=replayMode.Replay)
    	{	double start = 0.0;
    		cell <?> lastDest = null;
    		int depth = 0;
    		double lastTime = 0;
    		double firstStop = 0;
    		
    		for(int idx = 0,lim=animationStack.size()&~1; idx<lim;idx+=2)
    		{
			YinshCell from = animationStack.elementAt(idx);		// source and destination for the move
 			YinshCell to = animationStack.elementAt(idx+1);
 			if(sequential || ((fromStart) && ((to!=lastDest) || ((lastDest!=null) && lastDest.forceSerialAnimation()))))
 					{
 					// step to the next starting time if we change dests
 					start += lastTime;
 					}
  
 			// if serially, the moving object is the chip on top of the last
 			// destination, and it moves from the first source to last in 
 			// multiple steps.
    		Drawable moving =  to.animationChip(depth);
    		if(moving!=null)
				{
    			boolean flipper = fromStart && from.onBoard && ((moving==YinshChip.BlackChip)||(moving==YinshChip.WhiteChip));
    			if(flipper)
    			{ moving = (moving==YinshChip.BlackChip)?YinshChip.WhiteChip : YinshChip.BlackChip;
    			}
     			SimpleSprite anim = startAnimation(from,to,moving,size,
    					Math.min(firstStop,start) ,0,-1);	// start it
				lastTime = anim.getDuration();
				if(flipper)
    			{
    				anim.setFromEqualTo();
    				if(firstStop>0) { startAnimation(to,to,moving,size,0,firstStop,-1); }
    			}
    			else { firstStop = lastTime; }
				if(!from.onBoard && fromStart && (start==0) )
				{	// preserve the ring for the initial chip placement
					// and eliminate the final ring for the first segment
					YinshCell last = animationStack.elementAt(animationStack.size()-1);
					if(last!=to) 
					{ startAnimation(to,to,last.animationChip(-1),size,0,lastTime,-1); 
					  startAnimation(last,last,StockArt.SmallX,1,0,lastTime,-1);
					}
				}
				}
    		lastDest = to;
   
    		}
    	}}
    	finally {
       	animationStack.clear();
    	}
    }
    
    void playSounds(commonMove mm)
    {
  	  switch(mm.op)
  	  {
  	  case MOVE_REMOVE:
  		  for(int i=0;i<5;i++) { playASoundClip(scrape,50);  }
  	  	  break;
  	  case MOVE_MOVE:
  	  case MOVE_PICK:
  	  case MOVE_DROP:
  	  case MOVE_PLACE:
  		  playASoundClip(light_drop,100);
  		  break;
  	  default: break;
  	  }
    }
    public commonMove ParseNewMove(String st, int player)
    {
        return (new Yinshmovespec(st, player));
    }



    void doPlaceChip(char col, int row)
    {
        char c = b.GetBoardPos(col, row);
        int who = b.whoseTurn;

        if (b.ringCharIndex[who] == c)
        {
            PerformAndTransmit(b.placeCommand[who] + col + " " + row);
        }
        else
        {
        	throw G.Error("No command encoded for hit " + c);
        }
    }

    public void StartDragging(HitPoint hp)
    {
        //System.out.println("Start mo="+mo+" hit "+hitObject+" state "+state);
        if (hp.hitCode instanceof YinshId)
        {
        	YinshId hitObject = (YinshId)hp.hitCode;
            YinshState state = b.getState();

            switch (hitObject)
            {
            case EmptyBoard:default:
                break;

            case BoardLocation:
               switch (state)
                {
                default:
                	throw G.Error("Can't hit object in state " + state);
                case SELECT_EARLY_REMOVE_CHIP_DONE_STATE:
                case SELECT_EARLY_REMOVE_RING_DONE_STATE:
                case SELECT_LATE_REMOVE_CHIP_DONE_STATE:
                case SELECT_LATE_REMOVE_RING_DONE_STATE:
                case SELECT_EARLY_REMOVE_RING_STATE:
                case SELECT_LATE_REMOVE_RING_STATE:
                case SELECT_EARLY_REMOVE_CHIP_STATE:
                case SELECT_LATE_REMOVE_CHIP_STATE:

                    break;

                case PUZZLE_STATE:
                case PLACE_DONE_STATE:
                case MOVE_DONE_STATE:
                    PerformAndTransmit("Pick Board " + " " + hp.col + " " +hp.row);

                    break;

                case PICK_RING_STATE:
                    doPlaceChip(hp.col, hp.row);

                    break;
                }

                break;

            case Black_Chip_Pool:
            case White_Chip_Pool:
            case Black_Ring_Cache:
            case White_Ring_Cache:
            case White_Ring_Captured:
            case Black_Ring_Captured:
                PerformAndTransmit("Pick " + hitObject.shortName);

                break;
            }
        }
    }


    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof YinshId)) { missedOneClick = performStandardActions(hp,missedOneClick);}
    	else 
    	{
    	missedOneClick = false;
        YinshId hitCode = (YinshId)hp.hitCode;
        YinshState state = b.getState();

        switch (hitCode)
        {
        default:
        	throw  G.Error("Hit Unknown: %s in state %s", hitCode,state);
 
        case BoardLocation:

            switch (state)
            {
            default:
            	throw G.Error("Should't drop in state " + state);
            
            case SELECT_EARLY_REMOVE_RING_STATE:
            case SELECT_LATE_REMOVE_RING_STATE:
                PerformAndTransmit(b.removeString[b.whoseTurn] + hp.col +
                    " " + hp.row);
 
                break;
           
            case SELECT_EARLY_REMOVE_CHIP_STATE:
            case SELECT_LATE_REMOVE_CHIP_STATE:
               // let the board figure it out.
               PerformAndTransmit(b.moveForRemove(hp.col, hp.row,true),true,replayMode.Live);
               // hp.dragging = true;

                break;
            case DROP_RING_STATE:
            case PUZZLE_STATE:

                if (hasMovingObject(hp))
                {
                    PerformAndTransmit("Drop Board " + hp.col + " " + hp.row);
                }

                break;
            
            case PICK_RING_STATE:
                doPlaceChip(hp.col, hp.row);
            }

            break;

        case EmptyBoard:

            switch (state)
            {
            default:
                break; // this is a key bounce
            case DROP_RING_STATE:
            case PUZZLE_STATE:

                if (hasMovingObject(hp))
                {
                    PerformAndTransmit("Drop Board " + hp.col + " " + hp.row);
                }

                break;
            case PLACE_DONE_STATE: // changed his mind about the placement
            case PLACE_RING_STATE:

                if (hitCode == YinshId.EmptyBoard)
                {
                    PerformAndTransmit(b.placeRingCommand[b.whoseTurn] + hp.col + " " + hp.row);
                }
  
            break;
            }
            break;
        case Black_Chip_Pool:
        case White_Chip_Pool:
        case Black_Ring_Cache:
        case White_Ring_Cache:
        case White_Ring_Captured:
        case Black_Ring_Captured:
            PerformAndTransmit("Drop " + hitCode.shortName);

            break;
        }
    	}
     }

    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Yinsh_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        b.doInit(token);
    }


    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);

        return (handled);
    }



    public String gameProgressString()
    {
        return ((allowed_to_edit ? Reviewing
                          : ("" + History.viewMove + " W:" +
        b.captured_rings[FIRST_PLAYER_INDEX] + " B:" +
        b.captured_rings[SECOND_PLAYER_INDEX])));
    }


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() { return(new YinshPlay()); }
    public String sgfGameVersion() { return(SGF_GAME_VERSION); }

    public boolean parsePlayerInfo(commonPlayer p,String first,StringTokenizer tokens)
    {
    	if(exHashtable.TIME.equals(first) && b.DoneState())
    	{
    		PerformAndTransmit("Done",false,replayMode.Replay);
    	}
    	return(super.parsePlayerInfo(p, first, tokens));
    }
   public void ReplayMove(sgf_node no)
    {
        String comments = "";
        sgf_property prop = no.properties;
        int player = -1;
        while (prop != null)
        {
            String name = prop.getName();
            String value = (String) prop.getValue();

            //System.out.println("prop " + name + " " + value);
            if (setup_property.equals(name))
            {
                b.doInit(value);
            }
            if(date_property.equals(name))
            {
            	BSDate dd = new BSDate(value);
            	BSDate de = new BSDate("May 31 2006");
            	hasDones = !dd.before(de);
            }
            if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (name.equals(game_property) && value.equalsIgnoreCase("yinsh") )
            {	// the equals sgf_game_type case is handled in replayStandardProps
            }
            else if (name.equals(version_property))
            {   int val = G.IntToken(value);
	        	switch(val)
	        	{
	        	default: throw G.Error("Game version %s not handled",value);
	        	case 3: hasDones = true;
					break;
	        	case 2:
				case 1: break;
	        	}

            }
            else if ((player = playerNumberToken(name))>=0)
            {
                 commonPlayer p = players[player];

                if (p == null)
                {
                    p = players[player] = new commonPlayer(player);
                }
                
                StringTokenizer tokens = new StringTokenizer(value);
                String num="";
                String first = tokens.nextToken().toLowerCase();
                if(Character.isDigit(first.charAt(0))) { num=first+" "; first=tokens.nextToken().toLowerCase(); }
                if (true)
                {
                    if(parsePlayerInfo(p,first,tokens)) {}
                    else
                    {
                        String msg = first + " " + G.restof(tokens);
                        boolean isDone = msg.equals("Done ") ;
                        if(isDone)
                        	{ hasDones=true; }
                        if(!hasDones 
                        		&& !isDone 
                        		&& b.DoneState() 
                        		&& ((p.boardIndex!=b.whoseTurn) 
                        				|| (!msg.startsWith("done")&&!msg.startsWith("pick"))))
                        {
                        	//perform the done "just in time" so if there are
                            //editing/undo commands in the stream, they work ok
                        	// originally the dones were edited out.  Later, it became
                        	// apparent that it was better to include them.  Even later
                        	// the definite "has dones" is indicated by version 2
                        	//
                        	// the current logic is a fixup to try to make all the old game records work.
                            PerformAndTransmit("Done", false,replayMode.Replay);
                        }
                        PerformAndTransmit(hasDones ? num+msg : msg, false,replayMode.Replay);
                    }
                }
            }
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