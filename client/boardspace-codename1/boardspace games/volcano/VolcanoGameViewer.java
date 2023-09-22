/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package volcano;



import com.codename1.ui.geom.Rectangle;
import bridge.*;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
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
 * Feb 2008 initial work. 
 * 
 * 
*/
public class VolcanoGameViewer extends CCanvas<VolcanoCell,VolcanoBoard> implements VolcanoConstants, GameLayoutClient
{	
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(174,155,150);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    // images
    private static Image[] images = null; // images of black and white gobblets and board
    private static Image[] textures = null;// background textures
    // private state
    private VolcanoBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    // private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle scoreRects[] = addRect("score",2);
    private Rectangle repRect = addRect("repRect");


    public synchronized void preloadImages()
    {	
    	if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    	Pyramid.preloadImages(loader,ImageDir);
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

        b = new VolcanoBoard(randomKey,info.getString(GAMETYPE, "volcano"));
        useDirectDrawing(true); 
        doInit(false);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	boolean hexB = b.hexBoard;
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);				// initialize the board
        if(b.hexBoard!=hexB) { resetBounds(); }
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
   }

    private boolean usePerspective() 
    { 	// based on the current view, so we can toggle to the non-perspective view
    	return(super.getAltChipset()==0); 
    }
    boolean fixedChipSet = false;	// flag to use the upright chipset
    public int getAltChipset()
    { 	// if the original seating chart wasn't one of the ftf charts, return 0 so the pyramids remain upright
    	return ((!seatingFaceToFace() || fixedChipSet) ? 0 : super.getAltChipset()); 
    }
 
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
		int scoreW = unitsize*2;
		int doneW = plannedSeating() ?  unitsize*5 : 0;
    	Rectangle box = pl.createRectangularPictureGroup(x+scoreW+unitsize,y,unitsize);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	Rectangle score = scoreRects[player];
    	int boxW = G.Width(box);
    	G.SetRect(score, x+unitsize/2, y, scoreW,scoreW);
    	if(flatten)
    	{
        	G.SetRect(done, x, G.Bottom(box)+unitsize/3, doneW,doneW/2);
        	G.SetRect(chip, G.Right(box),y,boxW,unitsize*4);
        	G.union(box, chip,done);
    		
    	}
    	else
    	{
    	G.SetRect(done, G.Right(box), y+unitsize/2, doneW,doneW/2);
    	G.union(box, done,score);
    	G.SetRect(chip, x, G.Bottom(box),boxW,unitsize*4);
    	G.union(box, chip);
    	}
    	pl.displayRotation = rotation;
    	return(box);
    }
    double aspects[] = { 0.7,1.0,1.4,-0.7,-1.0,-1.4};
    private boolean flatten = false;
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	setLocalBoundsV(x,y,width,height,new double[] {1,-1});
    }

    public double setLocalBoundsA(int x, int y, int width, int height,double a)
    {	flatten = a<0;	
    	double aspect = Math.abs(a);
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	boolean perspective = usePerspective();
    	int minLogW = fh*15;	
       	int minChatW = fh*30;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        int nrows = 6;
        int ncols = perspective ? 8 : 6;
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			aspect,	// 1:1 aspect ratio for the board
    			fh*3,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int stateH = fh*5/2;
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH)/nrows);
    	SQUARESIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(nrows*cs);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-stateH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH+stateH/2;
    	int boardBottom = boardY+boardH+stateH;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH/2;
        int stateX = boardX;
        
        boolean rotate = seatingFaceToFaceRotated();
        if(rotate)
        {
        	contextRotation = -Math.PI/2;
        }
        G.placeRow(stateX,stateY,boardW,stateH,stateRect,annotationMenu,liftRect,viewsetRect,noChatRect);
        
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH-stateH/2,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,rackBackGroundColor);
        return(boardW*boardH);
    }
    public int cellSize() { return b.cellSize()*2; }
	// draw a box of spare pyramids. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, VolcanoBoard bd,int forPlayer, Rectangle r, Rectangle sr,int player,  HitPoint highlight)
    {	VolcanoCell captures = bd.captures[forPlayer];
    	if((highlight!=null)&&(highlight.hitObject!=null)) { highlight=null; }
        boolean canhit = G.pointInRect(highlight, r) && bd.LegalToHitChips(forPlayer) ;
        boolean canDrop = hasMovingObject(highlight);
        //set up cells as a rack of pyramids sorted by color and size
        VolcanoCell cells[] = new VolcanoCell[Pyramid.nColors];
        for(int i=0;i<cells.length;i++) { cells[i]=new VolcanoCell(null,i,captures.rackLocation()); }
        // sort the captives into the cells
        for(int i=0;i<=captures.chipIndex;i++)
        {	Pyramid p = captures.chipAtIndex(i);
        	cells[p.colorIndex].addPyramidInPosition(p);
        }
        int cellW = (int)(G.Width(r)/(bd.colorsToWin+0.25));
        int e_y = G.centerY(r)+cellW/5;
        fixedChipSet = true;
        if(gc!=null) {  GC.frameRect(gc,Color.black,r);  GC.frameRect(gc,Color.black,sr);}
        for(int cx = 0,e_x = G.Left(r)+cellW/2;
        	 cx<cells.length;
        	 cx++)
        {	VolcanoCell c = cells[cx];
        	if(c.drawStack(gc,canhit?highlight:null,e_x,e_y,this,liftSteps,SQUARESIZE,yscale,null))
        	{
        		highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        		highlight.awidth = SQUARESIZE/4;
        		highlight.spriteColor = Color.red;
        	}
        	if(c.chipIndex>=0)
        		{e_x = e_x + cellW;
        		}
        }
        fixedChipSet = false;

        int score = bd.scoreForPlayer(forPlayer);
        if(gc!=null)
        {	GC.setFont(gc,standardPlainFont());
        	GC.Text(gc,true,G.Left(r),G.Bottom(r)-20,G.Width(r),20,Color.blue,null,
        		"Solid Nests: "+bd.capture_size[0]+" Mixed Nests: "+bd.capture_size[1]+" Other: "+bd.capture_size[2]);
        	GC.setFont(gc,largeBoldFont());
        	GC.Text(gc,true,sr,0,Color.blue,null,""+score);
        	GC.setFont(gc,standardPlainFont());
        }
        
     }
    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	Pyramid.getPyramid(idx).drawChip(g,this,SQUARESIZE,xp,yp,null);
    }

    void prepareBoardGeometry(VolcanoBoard b)
    {
    	if(usePerspective())
    	{
    	double lr = 0;
	    	if(b.hexBoard)
	    		{ b.getRealBoard().SetDisplayParameters(0.78,0.73,  
	    				.45,0.0,
	    				19+lr,
	    				.25, 0.14,0.0);
	    		}
	    	else
	    	{ // square geometry board
	    	b.getRealBoard().SetDisplayParameters(0.63,0.86,
	    				0.35,-0.13,
	    				17.5+lr, .205, 0.1950,0.0);
	    	}}
    	else {
    		if(b.hexBoard)
    		{
    			b.getRealBoard().SetDisplayParameters(1.0,1.13,0.82,-0.1,90);
    		}
    		else
    		{
    			b.getRealBoard().SetDisplayParameters(0.81,1,0,0.15,0);    			
	    	}
	    	}
	  b.getRealBoard().SetDisplayRectangle(boardRect);

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
      boolean perspective = usePerspective();
       
      // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid
     images[perspective
                              ? b.hexBoard?HBOARD_INDEX:BOARD_INDEX
                              : b.hexBoard?HBOARD_NP_INDEX : RBOARD_NP_INDEX
    		  ].centerImage(gc, boardRect);
      prepareBoardGeometry(b);
      b.getRealBoard().DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.yellow);
    }
 	double yscale = 1/15.0;

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, VolcanoBoard gb, Rectangle brect, HitPoint highlight)
    {	gBoard<VolcanoCell> rb = gb.getRealBoard();
    	prepareBoardGeometry(gb);
      	Hashtable<VolcanoCell,VolcanoCell> dests = gb.getMoveDests();
     	Hashtable<VolcanoCell,VolcanoCell> caps = gb.getCaptures();
     	int dotsize = Math.max(2,SQUARESIZE/15);
     	boolean doLift = doLiftAnimation();
     	//
        // now draw the contents of the board and anything it is pointing at
        //
      	boolean perspective = usePerspective();
      	Enumeration<VolcanoCell>cells = rb.getIterator(perspective ? Itype.TBRL : Itype.BTRL );
      	int off = SQUARESIZE/15;
      	int yoff = SQUARESIZE/25;
       	while(cells.hasMoreElements())
       	{
       		VolcanoCell cell = cells.nextElement();
     	
            boolean isADest = dests.get(cell)!=null;
            boolean canHit = !doLift && gb.LegalToHitBoard(cell);
            boolean offset = perspective ? false : (((b.hexBoard ? cell.row : cell.col)&1)==0);
            int ypos = G.Bottom(brect) - rb.cellToY(cell)+(offset?yoff:-yoff);
            int xpos = G.Left(brect) + rb.cellToX(cell)+(offset?off:-off);
            if(cell.drawStack(gc,canHit?highlight:null,xpos,ypos,this,liftSteps,SQUARESIZE,yscale,null))
            {
            highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
            highlight.awidth = SQUARESIZE/3;
            highlight.spriteColor = Color.red;
            }
 
            if(gc!=null)
            	{
            	if(isADest)
	        	{
	        		GC.cacheAACircle(gc,xpos+dotsize,ypos,dotsize,Color.red,Color.gray,true);
	        	}
	            if(caps.get(cell)!=null)
	            { 	
	           	drawImage(gc,images[X_INDEX],SCALES[X_INDEX],xpos,ypos,SQUARESIZE,1.0,0.0,null,true);
	            }
	        	}
          }
  	        }

    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  VolcanoBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
       VolcanoState vstate = gb.getState();
       gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
       
       GC.setRotatedContext(gc,boardRect,highlight,contextRotation);     
       drawBoardElements(gc, gb, boardRect, ot);
       GC.unsetRotatedContext(gc,highlight);
       
       boolean planned = plannedSeating();
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
       {
       commonPlayer cpl = getPlayerOrTemp(i);
       cpl.setRotatedContext(gc, highlight, false);
       DrawCommonChipPool(gc,gb, i,chipRects[i], scoreRects[i],gb.whoseTurn,ot);
       if(planned && (gb.whoseTurn==i))
       {
			handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
      	
       }
       cpl.setRotatedContext(gc, highlight, true);
       }
       GC.setFont(gc,standardBoldFont());
       drawPlayerStuff(gc,(vstate==VolcanoState.PUZZLE_STATE),ourSelect,
	   			HighlightColor, rackBackGroundColor);

		double messageRotation = pl.messageRotation();

		if (vstate != VolcanoState.PUZZLE_STATE)
        {
			if(!planned && !autoDoneActive()) {
			handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
			}

			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
         }
		standardGameMessage(gc,messageRotation,
            		vstate==VolcanoState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
            				vstate!=VolcanoState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
         goalAndProgressMessage(gc,ourSelect,
            		s.get(CaptureBestMessage),progressRect,
             		goalRect);
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        drawLiftRect(gc,liftRect,highlight,textures[LIFT_ICON_INDEX]); 
        drawViewsetMarker(gc,viewsetRect,ourSelect);
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
        
        if(replay!=replayMode.Replay) { playSounds((VolcanoMovespec)mm); }
 
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
        return (new VolcanoMovespec(st, player));
    }
    

private void playSounds(VolcanoMovespec m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_DROPB:
    case MOVE_BOARD_BOARD:
    	{ 
    	  int h = m.undoInfo%10+1;	// number of pyramids moved
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
        if (hp.hitCode instanceof VolcanoId) // not dragging anything yet, so maybe start
        {

        VolcanoId hitObject = (VolcanoId)hp.hitCode;
		VolcanoCell cell = hitCell(hp);
		Pyramid chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
        case First_Player_Captures:
        case Second_Player_Captures:
        	PerformAndTransmit("Pick "+hitObject.shortName+" "+chip.colorIndex+" "+chip.sizeIndex);
        	break;
	    case BoardLocation:
	    	if(cell.chipIndex>0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.colorIndex+" "+chip.sizeIndex);
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
        if(!(id instanceof VolcanoId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    		missedOneClick = false;
    		VolcanoId hitObject = (VolcanoId)hp.hitCode;
        VolcanoState state = b.getState();
		VolcanoCell cell = hitCell(hp);
		Pyramid cup = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
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
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+cup.colorIndex+" "+cup.sizeIndex);
				}
				break;
			}
			break;
			
        case First_Player_Captures:
        case Second_Player_Captures:
        	{
            int mov = b.movingObjectIndex();
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                	case PLAY_STATE:
            		performReset();
            		break;

               	case PUZZLE_STATE:
                	String col = hitObject.shortName;
               		PerformAndTransmit("Drop "+col+" "+mov);
            		break;
            	}
			}
         	}
            break;
        }
        }
    }


    public String gameType() { return(b.gametype+" "+b.randomKey); }
    public String sgfGameType() { return(Volcano_SGF); }

    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	long rk = G.LongToken(his);
        b.doInit(token,rk);
    }
    

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new VolcanoPlay()); }

    public boolean replayStandardProps(String name,String value)
    {	nextIntCompatabilityKludge(b,name,value,"Oct 25 2013");
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

