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
package octiles;

import java.awt.*;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import common.GameInfo;
import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.Drawable;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Text;
import lib.TextButton;
import lib.Toggle;
import lib.Tokenizer;

import static octiles.OctilesMovespec.*;

/**
 * 
 * Change History
 *
 * May 2007 Initial work in progress. 
 *
*/

public class OctilesViewer extends CCanvas<OctilesCell,OctilesBoard> implements OctilesConstants
{
	 
    // file names for jpeg images and masks
    static final String Octiles_SGF = "Octiles"; // sgf game name
    static final String ImageDir = "/octiles/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BOARD_INDEX = 0;
    static final int BOARD_FLAT_INDEX = 1;
    static final int POST_INDEX = 2;
    static final String ImageNames[] = { "board","board-flat"};
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "posts-mask"
    	  };
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    

    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// foreground images
    // private state
    private OctilesBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
     private Rectangle chipRects[] = addRect("chip",4);
     private Toggle eyeRect = new Toggle(this,"eye",
 			StockArt.NoEye,OctilesId.ToggleEye,NoeyeExplanation,
 			StockArt.Eye,OctilesId.ToggleEye,EyeExplanation
 			);
    private Rectangle repRect = addRect("repRect");
    private TextButton passButton = addButton(PASS,GameId.HitPassButton,ExplainPass,
			HighlightColor, rackBackGroundColor);
    public Color[] OctilesMouseColors = 
	{ Color.blue, Color.red ,
	  Color.yellow, Color.green
	  };
    
    
    /** default for center dots of mouse sprites */
    public Color[] OctilesMouseDotColors = 
	{ Color.white, Color.black, 
		Color.black, Color.white
    };

    private static boolean imagesLoaded=false;
    public synchronized void preloadImages()
    {	
       	OctilesChip.preloadImages(loader,ImageDir);
        if (!imagesLoaded )
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
        textures[POST_INDEX] = images[BOARD_INDEX].compositeSelf(textures[POST_INDEX]);
        imagesLoaded = true;
    	}
        gameIcon = images[BOARD_INDEX];
    }

    public commonMove EditHistory(commonMove m) 
    { 	// this ia a simple fix to allow rotate moves to do nothing.
    	return(EditHistory(m,m.op==MOVE_ROTATE)); 
    }
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
       	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,4));
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        super.init(info,frame);
        
        MouseColors = OctilesMouseColors;
        MouseDotColors = OctilesMouseDotColors;
        int map[]=getStartingColorMap();
        b = new OctilesBoard(info.getString(GameInfo.GAMETYPE, Octiles_INIT),players_in_game,
        		randomKey,map);
        useDirectDrawing(true);
        doInit(false);

         
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey,b.nPlayers());						// initialize the board
        if(!preserve_history)
    	{         
        startFirstPlayer();
    	}
   }
    

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unit)
    {	
    	commonPlayer pl0 = getPlayerOrTemp(player);
    	int u2 = unit/2;
		int sz = unit*2;
		Rectangle chipRect = chipRects[player];
		Rectangle done = doneRects[player];
	    G.SetRect(chipRect, x, y, sz, sz);
		Rectangle box = pl0.createRectangularPictureGroup(x+sz,y,unit);
		G.SetRect(done, G.Right(box)+u2, y+u2, plannedSeating()?unit*5:0, unit*5/2);
		G.union(box,chipRect,done);
		pl0.displayRotation = rotation;
		return(box);
    }


    public void setLocalBounds(int x,int y,int width,int height)
    {   
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();		// always 3 for triad
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*15;	
       	int minChatW = fh*35;	
        int minLogH = fh*14;	
        int margin = fh/2;
        int ncols = 20;
        int buttonW = fh*8;

        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// % of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*2.0,	// minimum cell size based on font size
    			fh*3.0,	// maximum cell size based on font size
    			0.3		// preference for the designated layout, if any
    			);
    	
    	// place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	// however, if that doesn't work out the main rectangle will shrink.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
     	
        layout.placeTheVcr(this,minLogW,minLogW*3/2);
        layout.placeDoneEditRep(buttonW, buttonW*2, doneRect,editRect,repRect);
        layout.placeRectangle(passButton, G.Width(editRect), G.Height(editRect), BoxAlignment.Edge);
        Rectangle main = layout.getMainRectangle();
        int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/ncols);
    	CELLSIZE = (int)cs;
        SQUARESIZE = (int)(cs*(usePerspective()?1.7:2.1));
    	// center the board in the remaining space
        int stateH = fh*5/2;
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(ncols*cs);
    	
    	int extraW = (mainW-CELLSIZE*2-boardW)/2;
    	int extraH = (mainH-boardH)/2;
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	int stateY = boardY;
       	layout.returnFromMain(extraW,extraH);
    	placeStateRow( boardX+CELLSIZE,stateY,boardW-CELLSIZE,stateH,iconRect,stateRect,annotationMenu,eyeRect,noChatRect);
     	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
        
    	placeRow( boardX, boardBottom-stateH*2, boardW, stateH,goalRect,viewsetRect);
        setProgressRect(progressRect,goalRect);

        positionTheChat(chatRect,Color.white,Color.white);
 
    }
    
 

    //
    // sprites are normally a game piece that is "in the air" being moved
    // around.  This is called when dragging your own pieces, and also when
    // presenting the motion of your opponent's pieces, and also during replay
    // when a piece is picked up and not yet placed.  While "obj" is nominally
    // a game piece, it is really whatever is associated with b.movingObject()
    //
    public void drawSprite(Graphics g,int obj0,int xp,int yp)
    {  	// draw an object being dragged
    	int rot = obj0/1000;
    	int obj = obj0%1000;
    	OctilesChip ch = OctilesChip.getChip(obj);// Tiles have 1 as the first offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null,rot,0);
     }


    private void setDisplayRectangle(Rectangle r,OctilesBoard gb)
    {
		if(usePerspective())
		{
		gb.adjustPadCoordinates_perspective();
		gb.SetDisplayParameters(0.85,0.85,  0.05,0.45, 
				-0.4,	// rotation
				.09, 0.1,0.0);
		gb.SetDisplayRectangle(r);
		
		}
		  else 
		  {	gb.adjustPadCoordinates_normal();
		  	gb.SetDisplayRectangle(r);
		  	double[] ll = new double[] {0.19,0.175} ;// {0.24,0.23};
		  	double[] lr = new double[] {0.985,0.185};
		  	double[] ul = new double[] {0.185,0.98}; //{0.24,0.90};
		  	double[] ur = new double[] {0.98,0.98};
		  	gb.SetDisplayParameters(ll, lr, ul, ur);
			
		  }

    }

    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }

    public boolean usePerspective()
    {
    	return(getAltChipset()==0);
    }
    Image background = null;
    Image scaled = null;
    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean review = reviewMode() && !mutable_game_record;
      OctilesBoard gb = disB(gc);
      // erase
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      boolean perspective = usePerspective();
      
      Image board = images[perspective?BOARD_INDEX:BOARD_FLAT_INDEX];
      if(board!=background) { scaled = null; }
      background = board;
      scaled = board.centerScaledImage(gc, boardRect,scaled);
      
      setDisplayRectangle(boardRect,gb);
      int left = G.Left(boardRect);
      int top = G.Top(boardRect);
      int h = G.Height(boardRect);
      int low = (int)(0.4*CELLSIZE);
      int high = (int)(0.5*CELLSIZE);
      int xoff = (int)(CELLSIZE*0.1);
      int yoff = (int)(CELLSIZE*0.06);
      for(OctilesCell rack[] : gb.goalPads)
      {	  for(OctilesCell c : rack)
    	  {
    	  int cx = left+gb.cellToX(c);
    	  int cy0 = gb.cellToY(c);
    	  int cy = top+h-cy0;
    	  OctilesChip dest = c.goalForColor;
    	  OctilesChip disk = dest.disk;
    	  if(perspective)
    	  {
    	  int CS = G.interpolate(cy0/(double)h, high,low);
    	  disk.drawChip(gc,this,CS,1.4,cx-xoff,cy-yoff,null,0,0);
    	  }
    	  else
    	  {	// artwork is tuned to the flat version
    	   	  disk.drawChip(gc,this,(int)(CELLSIZE*0.58),cx,cy,null);	   	   		  
    	  }
    	  }
      }
      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    
 // draw the runner cells, (not the tile cells)
 private OctilesCell drawRunnerCells(Graphics gc,OctilesBoard gb,Rectangle brect,HitPoint highlight,int row)
 {	OctilesCell hitCell = null;
 	boolean show = eyeRect.isOnNow();
    for (char thiscol = (char)('A'+gb.ncols-1),firstcol='A';
		 thiscol>=firstcol;
		 thiscol--)
	{
    OctilesCell cell = gb.getCell(thiscol,row);
    int ypos = G.Bottom(brect) - gb.cellToY(thiscol, row);
    int xpos = G.Left(brect) + gb.cellToX(thiscol, row);
        //for debugging, draw the posts
    GC.setColor(gc,Color.yellow);
   //  StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,""+cell.col+cell.row);
   	boolean canhit = gb.LegalToHitBoard(cell);
       if((cell.activeAnimationHeight()==0)
    		   && cell.drawChip(gc,this,canhit?highlight:null,SQUARESIZE,xpos,ypos,null)) 
       	{ hitCell = cell; 
		highlight.awidth=SQUARESIZE/2;
		highlight.spriteColor = Color.red;
        }
       if(canhit && show) { StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null); }
       cell.rotateCurrentCenter(gc,xpos,ypos);
	}
 	return(hitCell);
    }


   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, OctilesBoard gb, Rectangle brect, HitPoint highlight)
    {  	//
        // now draw the contents of the board and anything it is pointing at
        //
    	OctilesState board_state = gb.getState();
    	boolean show = eyeRect.isOnNow();
        boolean mov = (gb.pickedObject!=null);
        boolean perspective = usePerspective();
        Hashtable<OctilesCell,OctilesMovespec> dests = gb.getDests(false);
        // draw the back row of runners first, so they can be partially occluded by the tiles
        OctilesCell hitCell = drawRunnerCells(gc,gb,brect,highlight,gb.nrows);
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left back-front order so the
        // solid parts will fall on top of existing shadows
        gb.clearMarkedPaths();
        if( ((gb.getState()==OctilesState.MOVE_RUNNER_HOME_STATE) || (gb.getState()==OctilesState.MOVE_RUNNER_STATE))
        		&&mov)
        {
        // mark the valid runner paths
        OctilesCell c = gb.pickedSource;
        for(Enumeration<OctilesMovespec> de = dests.elements(); de.hasMoreElements(); )
        	{
        	OctilesMovespec m = de.nextElement();
        	OctilesCell d = gb.getCell(m.to_col,m.to_row);
        	OctilesCell through = gb.getCell(m.drop_col,m.drop_row);
        	gb.markValidMove(true,c,d,through);
         	}
        }
        for(OctilesCell c = gb.tilePool; c!=null; c=c.next)
        {	
        	int xpos = G.Left(brect) + gb.cellToX(c);
        	int ypos = G.Bottom(brect) - gb.cellToY(c);
        	double adj = 1.0-0.14*((G.Height(brect)-ypos)/(double)G.Height(brect));
        	int ss = (int)(SQUARESIZE*adj);
        	boolean canhit = gb.LegalToHitBoard(c);
        	OctilesChip top = c.topChip();
        double step = 0.15*(1.0-(c.height()/39.0));
        boolean rotateZone = (highlight!=null)
            	&& (top!=null)
            	&& !mov
            	&& canhit
            	&& ((board_state==OctilesState.MOVE_RUNNER_STATE)
             			||(board_state==OctilesState.PUZZLE_STATE)
             			||(board_state==OctilesState.MOVE_RUNNER_HOME_STATE))
            	&& G.pointInsideSquare(highlight,xpos,ypos,gb.isDest(c)?2*SQUARESIZE/3:SQUARESIZE/2)
            	&& !G.pointInsideSquare(highlight,xpos,ypos,SQUARESIZE/3);
        if(rotateZone)
        {
        	boolean left = (xpos<G.Left(highlight));
    		highlight.awidth=ss;
    		highlight.hitObject = c;
    		//highlight.spriteColor = Color.blue;
    		highlight.hit_x = G.Left(highlight);
    		highlight.hit_y = G.Top(highlight);
    		highlight.hitCode = left?OctilesId.RotateLeft:OctilesId.RotateRight;
    		highlight.arrow = left?StockArt.Rotate_CCW:StockArt.Rotate_CW;
        }
        HitPoint hitStack = (canhit&&!rotateZone)?highlight:null;
        if(c.drawStack(gc,this,hitStack,SQUARESIZE,xpos,ypos,
        		0,(perspective?0:step*.2),(perspective?step:0), null))
        	{ 	
        		// inner part or just dropping in
	               	highlight.awidth=ss/2;
	        		highlight.spriteColor = Color.red;
	         		highlight.arrow =mov ? StockArt.DownArrow : StockArt.UpArrow;
	         		highlight.spriteColor = Color.red;
        	}
        if(show ?  hitStack!=null : (dests.get(c)!=null))
        	{ StockArt.SmallO.drawChip(gc,this,SQUARESIZE,
        			xpos-(int)(perspective?0:step*0.2*c.height()*SQUARESIZE),
        			ypos-(int)(perspective?step*c.height()*SQUARESIZE:0),null); 
        	}
                 
        }
        if(perspective)
        {
        // redraw the posts over the tile cells
       textures[POST_INDEX].centerImage(gc, brect);
        gb.clearMarkedPaths();
        }
      	for (int row = 1;
        		 row<gb.nrows;
        		 row++)		
        	{ 
       		OctilesCell hc = drawRunnerCells(gc,gb,brect,highlight,row);
       		if(hitCell==null) { hitCell = hc; }
        	}
        
    }
    public Text censoredMoveText(SequenceElement sp,int idx)
    {	OctilesMovespec next = ((idx+1<History.size())
    									? (OctilesMovespec)History.elementAt(idx+1) 
    									: null);
    	return(((OctilesMovespec)sp).censoredMoveText(this,next));
    }
    private double[] textIconScale = new double[] { 1,1.5,-0.1,-0.2};
    public Drawable getPlayerIcon(int p)
    {	playerTextIconScale = textIconScale;
    	return b.playerRunner[p];
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  OctilesBoard gb = disB(gc);
    setDisplayRectangle(boardRect,gb);

      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      OctilesState vstate = gb.getState();
      gameLog.playerIcons = true;
      gameLog.redrawGameLog2(gc, ourSelect, logRect, Color.black,Color.black,standardBoldFont(),standardPlainFont());
      drawBoardElements(gc, gb, boardRect, ot);
//        DrawTilePool(gc, FIRST_PLAYER_INDEX,firstPlayerChipRect, gb.whoseTurn,ot);
      boolean planned = plannedSeating();
	  double showRotation = seatingAround() ? 0 : getPlayerOrTemp(gb.whoseTurn).displayRotation;
		GC.setFont(gc,standardBoldFont());
		if (vstate != OctilesState.PUZZLE_STATE)
        {	if(!planned) { handleDoneButton(gc,showRotation,doneRect,(b.DoneState()? select : null),HighlightColor, rackBackGroundColor); }
        	boolean conf = vstate==OctilesState.CONFIRM_PASS_STATE;
        	if(vstate==OctilesState.PLAY_TILE_STATE || conf)
        	{	
        		passButton.highlightWhenIsOn = true;
            	passButton.setIsOn(conf);
        		passButton.show(gc,showRotation,select);
        	}
        	handleEditButton(gc,showRotation,editRect,select,highlight,HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==OctilesState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);
 		for(int i=0;i<gb.nPlayers();i++)
 			{
 			Rectangle r = chipRects[i];
 	    	OctilesChip chip = b.playerRunner[i];
 	    	commonPlayer pl = getPlayerOrTemp(i);
 	    	pl.setRotatedContext(gc, highlight, false);
 	    	chip.drawChip(gc,this,r,null);
 	    	if(planned && (i==gb.whoseTurn)) { handleDoneButton(gc,doneRects[i],(b.DoneState()? select : null),HighlightColor, rackBackGroundColor); }
 	    	pl.setRotatedContext(gc, highlight, true);
 			}
 		
        standardGameMessage(gc,
        		vstate==OctilesState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=OctilesState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        OctilesChip chip = b.playerRunner[gb.whoseTurn];
        chip.drawChip(gc, this, iconRect,null);
        goalAndProgressMessage(gc,ourSelect,
        		s.get(GoalMessage),progressRect, goalRect);
        eyeRect.activateOnMouse=true;
        eyeRect.draw(gc,highlight);
        DrawRepRect(gc,showRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
        drawVcrGroup(ourSelect, gc);
        drawViewsetMarker(gc,viewsetRect,ourSelect); 

    }
    public boolean replayStandardProps(String name,String value)
    {	nextIntCompatabilityKludge(b,name,value,"Aug 25 2012");
    	return(super.replayStandardProps(name,value));
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
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.SequentialFromStart);
        if(replay.animate) { playSounds(mm); }
 
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
        return (new OctilesMovespec(st, player));
    }
    

   
private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_PICK:
    	 playASoundClip(light_drop,100);
    	 break;
    case MOVE_PICKB:
    	playASoundClip(light_drop,100);
    	break;
    case MOVE_DROP:
    	break;
    case MOVE_DROPB:
      	 playASoundClip(heavy_drop,100);
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
        if(hp.hitCode instanceof OctilesId)
        {
       	OctilesId hitObject = (OctilesId)hp.hitCode;
		OctilesCell cell = hitCell(hp);
		OctilesChip chip = (cell==null) ? null : cell.topChip();

		if(chip!=null)
		{
	    switch(hitObject)
	    {
 	    case TilePoolRect:
	    	PerformAndTransmit("Pick "+chip.chipNumber());
	    	break;
	    case PostLocation:
	    	if(cell.chipIndex>=0)
    		{
    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
    		}
	    	break;
	    case TileLocation:
	    	if(cell.chipIndex>=0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
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
    public void StopDragging( HitPoint hp)
    {	CellId id = hp.hitCode;
    	if(!(id instanceof OctilesId)) {missedOneClick = performStandardActions(hp,missedOneClick); }
    	else
    	{
    	missedOneClick = false;
        OctilesId hitObject = (OctilesId)hp.hitCode;
		OctilesCell cell = b.getCell(hitCell(hp));
		OctilesChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s" , hitObject);
        case ToggleEye:
        	eyeRect.toggle();
        	break;
        case RotateRight:
        	{
        	int rot = b.nextValidRotation(cell,1);
        	PerformAndTransmit("Rotate "+cell.col+" "+cell.row+" "+chip.chipNumber()+" "+rot);
        	}
        	break;
        case RotateLeft:
        	{
        	int rot = b.nextValidRotation(cell,-1);
        	PerformAndTransmit("Rotate "+cell.col+" "+cell.row+" "+chip.chipNumber()+" "+rot);
        	}
        	break;
         case PostLocation:
         case TileLocation:	// we hit the board 
			{	
				if(b.pickedObject!=null)
				{
				if(cell!=null) 
					{ 
			        int rot = (b.getState()!=OctilesState.PUZZLE_STATE) 
			        			? b.getValidRotation(cell,b.pickedObjectRotation)
			        			: b.pickedObjectRotation;
			        int obj = b.pickedObject.chipNumber();

			        PerformAndTransmit("Dropb "+cell.col+" "+cell.row+" "+obj+" "+rot); 
					}
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
				}
			}
			break;
			
         case TilePoolRect:
        	{
        	int mov = b.movingObjectIndex();
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	PerformAndTransmit("Drop "+(mov%1000));
			}
         	}
            break;

        }
    	}
    }


    public String gameType() 
    { 
     	return(b.gametype+" "+b.nPlayers()+" "+b.randomKey); 
    }
    public String sgfGameType() { return(Octiles_SGF); }

    
    // interact with the board to initialize a game
    public void performHistoryInitialization(Tokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	int pla = his.intToken();
    	int key = his.intToken();
	   	//
		// in games which have a randomized start, this is the point where
		// the randomization is inserted
	    // int rk = G.IntToken(his);
		// bb.doInit(token,rk);
         b.doInit(token,key,pla);
         adjustPlayers(pla);

    }


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new OctilesPlay()); }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 2/27/2023
     * 2334 files visited 0 problems
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
            {	Tokenizer st = new Tokenizer(value);
            	String typ = st.nextToken();
            	int np = st.intToken();
            	long ran = st.longToken();
                b.doInit(typ,ran,np);
                adjustPlayers(np);
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

