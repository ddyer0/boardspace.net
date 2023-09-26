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
package gounki;

import java.awt.*;

import online.common.*;
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
 * Sept 2011 Initial work in progress. 
 *
*/
public class GounkiViewer extends CCanvas<GounkiCell,GounkiBoard> implements GounkiConstants, GameLayoutClient
{	
     /**
	 * 
	 */
	 
	static final String Gounki_SGF = "Gounki"; // sgf game name
	static final String ImageDir = "/gounki/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BOARD_INDEX = 0;
    static final int BOARD_TWIST_INDEX = 1;
    static final int BOARD_NP_INDEX = 2;
    static final int ICON_INDEX = 2;
    static final String ImageNames[] = { "board","board-twist","board-np"};
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "gounki-icon-nomask",
    	  };
    // colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private Color chatBackGroundColor = new Color(222,194,164);
    private boolean usePerspective() { return(getAltChipset()==0); }
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// images
    // private undoInfo
    private GounkiBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("chip",2);
    
    private Rectangle altViewRect = addRect("altView");
    private Rectangle reverseRect = addRect("reversetRect");
    private Rectangle rackRects[] = addRect("rack",2);
    
    boolean twist = false;
    
    public synchronized void preloadImages()
    {	
       	GounkiChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
    	}
        gameIcon = textures[ICON_INDEX];
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
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//

    	super.init(info,frame);
        int randomKey = sharedInfo.getInt(OnlineConstants.RANDOMSEED,-1);
        int map[] = getStartingColorMap();
        b = new GounkiBoard(info.getString(GAMETYPE, Gounki_INIT),
        		randomKey,map);
        if(seatingFaceToFace()) { b.autoReverseYNormal(); }
        useDirectDrawing(true);
        doInit(false);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);			// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
    }
    boolean vertical = false;
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	int chipW = unitsize*2;
    	int chipH = unitsize*2;
    	int doneW = plannedSeating() ? unitsize*4 : 0;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	Rectangle done = doneRects[player];
    	Rectangle rack = rackRects[player];
    	
    	G.SetRect(chip, x, y, chipW, chipH);
       	G.SetRect(rack,G.Right(box),y,unitsize*4,unitsize*4);
    
    	if(vertical)
    		{ G.SetRect(done, x+chipW,G.Bottom(box),doneW,doneW/2);
    		}
    	else {
    		G.SetRect(done, G.Right(box)+unitsize/2,y+unitsize/2,doneW,doneW/2);
    	}
    	
    	pl.displayRotation = rotation;
    	
    	G.union(box, chip,done,rack);
    	return(box);
    }
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	setLocalBoundsV(x,y,width,height,new double[] {-1,1});
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double a)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	vertical = a>0;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*12;
    	int vcrW = fh*15;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
       	boolean perspective = usePerspective();
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.70,	// % of space allocated to the board
    			0.85,	// aspect ratio for the board
    			fh*2.5,	// minimum cell size
    			fh*3,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEdit(buttonW,3*buttonW/2,doneRect,editRect);
    	layout.placeTheVcr(this,vcrW,vcrW*3/2);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
        int stateH = fh*3;
    	int mainH = G.Height(main);
        boolean rotate = !perspective && seatingFaceToFaceRotated();
        int prows = (perspective ? 15 : 27);
        int nrows = rotate ? 24 : prows;  
        int ncols = rotate ? prows : 24;
        int bufferCellsW = rotate ? 2 : 0;
        int bufferCellsH = rotate ? 0 : 2;
        
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/(ncols+bufferCellsW*2),(double)(mainH-stateH)/(nrows+bufferCellsH*2));
    	int CELLSIZE = (int)cs;
  
        SQUARESIZE = CELLSIZE*3;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW-bufferCellsW*2*CELLSIZE)/2);
    	int extraH = Math.max(0, (mainH-boardH-bufferCellsH*2*CELLSIZE)/2);
    	int boardX = mainX+extraW+bufferCellsW*CELLSIZE;
    	int boardY = mainY+extraH+bufferCellsH*CELLSIZE;
    	int boardBottom = boardY+boardH;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-bufferCellsH*CELLSIZE;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,reverseRect,altViewRect,viewsetRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	if(rotate)
    	{
    		G.setRotation(boardRect,-Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackGroundColor,chatBackGroundColor);
        return boardW*boardH;
    }

    private void DrawAltViewRect(Graphics gc, Rectangle r,HitPoint highlight)
    {	      // if the board is one large graphic, for which the visual target points
        // are carefully matched with the abstract grid
       images[!twist?BOARD_TWIST_INDEX:BOARD_INDEX].centerImage(gc, r);
    	//G.frameRect(gc,Color.black,r);
    	//drawBoardElements(gc,b, r, null);
    	if(G.pointInRect(highlight,r))
    	{	highlight.spriteRect = r;
			highlight.spriteColor = Color.red;
    		highlight.hitCode = GounkiId.TwistViewButton;
    	}
     }  
	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCapturedChipPool(Graphics gc, int player, Rectangle r, int forPlayer, HitPoint highlight)
    {	GounkiCell chips[]= b.rack[player];
        boolean canHit = b.LegalToHitChips(player);
        boolean moving = b.pickedObject!=null;
        int step = (int)(G.Height(r)*0.5);
        int start = step/2;
        for(int i=0;i<chips.length; i++)
        {	GounkiCell thisCell = chips[i];
            HitPoint pt = canHit? highlight : null; 
            if(thisCell.drawStack(gc,this,pt,step,G.Left(r)+start,G.Bottom(r)-start-step*i,0,0.15,0,null))
            {
            	highlight.arrow = moving ? StockArt.DownArrow : StockArt.UpArrow;
            	highlight.awidth = step/2;
            	highlight.spriteColor = Color.red;
            }
        }
      }
	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawSampleChipPool(Graphics gc, int forPlayer, Rectangle r)
    {	
        GounkiChip thisChip = GounkiChip.getChip(b.getColorMap()[forPlayer],0);
        thisChip.drawChip(gc,this,G.Width(r),G.centerX(r),G.centerY(r),null);
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
    	GounkiChip ch = GounkiChip.getChip(obj);
    	boolean inrect = G.pointInRect(xp,yp,boardRect);
    	double startScale = inrect 
							? 1.1*yScale( G.Bottom(boardRect) - yp,G.Height(boardRect)) 
							: 0.8;
    	int sz = (int)(SQUARESIZE*startScale);
    	ch.drawChip(g,this,sz,xp,yp,null);
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
    Image scaled = null;
    Image background = null;
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      // erase
     if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     Image board = images[usePerspective() 
                              	? twist?BOARD_TWIST_INDEX:BOARD_INDEX
                              	: BOARD_NP_INDEX];
     if(board!=background) { scaled = null; }
     background = board;
     scaled = board.centerScaledImage(gc,	brect, scaled);

      setBoardParameters(b,brect);
      
      b.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

    private double yScale(int y,int h)
    {	return(1.0-(0.2*y/h));	// scale sizes as y moves toward the top
    }
    private void setBoardParameters(GounkiBoard gb,Rectangle r)
    {	
    	if(usePerspective())
    	{
    	if(twist)
       	{gb.SetDisplayParameters(0.97,0.71,  		// xy scale
    		1.02,-1.25,							// xy offset
    		16.9,			// rot
    		0.185, 			// xper
    		0.16,			// yper
    		-0.0);				// skew
    	} else
    	{gb.SetDisplayParameters(1.12,0.70,  		// xy scale
	    		-0.01,-0.95,							// xy offset
	    		0.0,			// rot
	    		0.16, 			// xper
	    		0.13,			// yper
	    		0);				// skew
    	}}
    	else 
    	{
    		gb.SetDisplayParameters(1.21,1,-0.05,0.05,0);
    	}
    gb.SetDisplayRectangle(r);
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, GounkiBoard gb, Rectangle brect, HitPoint highlight)
    {	
     	//
        // now draw the contents of the board and anything it is pointing at
        //
    	Hashtable<GounkiCell,GounkiMovespec>dests = gb.getDests();
    	CellStack current_cells = gb.cellsForCurrentMove;
    	CellStack prev_cells = gb.cellsForPrevMove;
    	GounkiCell current_source = current_cells.size()>0 ? current_cells.elementAt(0) : null;
    	GounkiCell prev_source = prev_cells.size()>0 ? prev_cells.elementAt(0) : null;

    	boolean perspective = usePerspective();
    	double ystep = perspective ? 0.2 : 0.03;
    	double xstep = perspective ? 0 : 0.1;
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left back-front order so the
        // solid parts will fall on top of existing shadows
    	Enumeration<GounkiCell> cells = gb.getIterator(perspective ? Itype.LRTB : Itype.RLTB ); 
    	while(cells.hasMoreElements())
    	{
            GounkiCell cell = cells.nextElement();
            int rawy = gb.cellToY(cell);
            int ypos = G.Bottom(brect) - rawy;
            int xpos = G.Left(brect) + gb.cellToX(cell);
            boolean legal = gb.LegalToHitBoard(cell,dests);
            double yscl = perspective 
            				? (twist?0.9:1.0)*yScale(rawy-G.Top(brect),G.Height(brect))
            				: 1.0;
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE/2,xpos,ypos,null);
            if(prev_cells.contains(cell))
            {	double siz = SQUARESIZE*((cell==prev_source)?0.5:0.75)*yscl;
            	StockArt.LandingPad.drawChip(gc,this,(int)siz,xpos,ypos,null);
            }
            if(current_cells.contains(cell))
            {	double siz = SQUARESIZE*((cell==current_source)?0.5:0.75)*yscl;
            	StockArt.LandingPad.drawChip(gc,this,(int)siz,xpos,ypos,null);
            }
            if( cell.drawStack(gc,this,legal?highlight:null,(int)(SQUARESIZE*yscl),xpos,ypos,0,xstep,ystep,null)) 
            	{ 
            	  highlight.awidth = SQUARESIZE/3;
            	  highlight.arrow = gb.pickedObject!=null ? StockArt.DownArrow:StockArt.UpArrow; 
              	  highlight.spriteColor = Color.red;
            	}
            if(dests.get(cell)!=null)
            {
            	StockArt.SmallO.drawChip(gc,this,SQUARESIZE/2,xpos,ypos,null);
        	}
    	}
  
      }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  DrawReverseMarker(gc,reverseRect,highlight,GounkiId.ReverseViewButton);
       if(usePerspective()) { DrawAltViewRect(gc,altViewRect,highlight); }
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  GounkiBoard gb = disB(gc);

      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      GounkiState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
      
      GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
      drawBoardElements(gc, gb, boardRect, ot);
      GC.unsetRotatedContext(gc,highlight);
      
      boolean planned = plannedSeating();
      for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
        {	commonPlayer pl = getPlayerOrTemp(i);
        	pl.setRotatedContext(gc, highlight, false);
        	DrawSampleChipPool(gc, i,chipRects[i]);
        	DrawCapturedChipPool(gc,i,rackRects[i],gb.whoseTurn,highlight);
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
		if (vstate != GounkiState.PUZZLE_STATE)
        {
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==GounkiState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);

        standardGameMessage(gc,messageRotation,
        		vstate==GounkiState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        		vstate!=GounkiState.PUZZLE_STATE,
        		gb.whoseTurn,stateRect);
        DrawSampleChipPool(gc,gb.whoseTurn,iconRect);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);

        drawAuxControls(gc,ourSelect);
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
        
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Simultaneous);

        if(replay!=replayMode.Replay) { playSounds(mm); }
 
        return (true);
    }
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current undoInfo of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new GounkiMovespec(st, player));
    }
    
    
private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_DEPLOY:
    case MOVE_BOARD_BOARD:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
       	 break;
    case MOVE_PICKB:
    	playASoundClip(light_drop,100);
    	break;
    case MOVE_DROPB:
      	 playASoundClip(heavy_drop,100);
      	break;
    default: break;
    }
	
}
public boolean PerformAndTransmit(String str) 
{	//G.print("P "+str);
	return(super.PerformAndTransmit(str));
}

//{	
//	return(super.handleExecute(command));
//}

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
	public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof GounkiId)// not dragging anything yet, so maybe start
        {
        GounkiId hitObject = (GounkiId)hp.hitCode;
		GounkiCell cell = hitCell(hp);
		GounkiChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case ReverseViewButton:
	    	break;
 	    case Black_Chip_Pool:
	    	PerformAndTransmit("Pick B "+cell.row);
	    	break;
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick W "+cell.row);
	    	break;
	    case BoardLocation:
	    	if((b.getState()==GounkiState.PLAY_STATE)||(b.getState()==GounkiState.PUZZLE_STATE))
	    	{
	    		PerformAndTransmit("Pickb "+cell.col+ " "+(cell.row-2));
	    	}
	    	break;
		case TwistViewButton:
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
        if(!(id instanceof GounkiId)) { missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
   		missedOneClick = false;
   		GounkiId hitObject =(GounkiId)hp.hitCode;
        GounkiState state = b.getState();
		GounkiCell cell = b.getCell(hitCell(hp));
		GounkiChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case TwistViewButton:
        	twist = !twist;
        	generalRefresh();
        	break;
        case ReverseViewButton:
	       	 b.setReverseY(!b.reverseY());
	       	 generalRefresh();
        	break;
         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case DEPLOY2_STATE:
			case DEPLOY_ONLY_STATE:
			case DEPLOY_STATE:
	    		{
	    		GounkiCell dest = b.getDest();
	    		if(cell==dest) { PerformAndTransmit("Pickb "+cell.col+" "+(cell.row-2)); }
	    		else
	    		{
	    		CommonMoveStack possible = b.deployMovesContaining(dest,cell);
	    		GounkiMovespec m = (GounkiMovespec)possible.pop();
	    		GounkiCell intermediate = b.getCell(m.to_col2,m.to_row2);
	    		if(!b.isDest(intermediate) && (cell!=intermediate))
	    		{
	    			PerformAndTransmit("DSTEP "+m.to_col2+" "+(m.to_row2-2));
	    		}
	    		PerformAndTransmit("DSTEP "+cell.col+" "+(cell.row-2));
	    		
	    		}}
				break;
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+(cell.row-2)); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+(cell.row-2)+" "+chip.chipNumber());
				}
				break;
			}
			break;
			
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  (hitObject==GounkiId.Black_Chip_Pool) ? " B " : " W ";
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in undoInfo %s",state);
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
    public String sgfGameType() { return(Gounki_SGF); }

    
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

/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically
 *  */
    

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new GounkiPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
	 * summary: 5/26/2023
	 *  2061 files visited 0 problems
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
                resetBounds();
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

