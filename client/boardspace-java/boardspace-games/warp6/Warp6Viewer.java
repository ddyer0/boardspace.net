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
package warp6;

import java.awt.*;
import java.util.Hashtable;
import java.util.StringTokenizer;

import common.GameInfo;
import lib.Image;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.Graphics;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.SimpleSprite;
import lib.SoundManager;
import lib.StockArt;
import online.common.*;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

/**
 * 
 * Change History
 *
 * Feb 2008 initial work. 
 * 
 * 
*/
public class Warp6Viewer extends CCanvas<Warp6Cell,Warp6Board> implements Warp6Constants
{	// colors

	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	private final boolean SHOW_DEBUG_GRID = false;
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(164,184,180);
    private Color boardBackgroundColor = new Color(202,215,212);
    
    private Color chatBackgroundColor = new Color(230,230,230);
    // images
    private static StockArt[] images = null; // images of black and white gobblets and board
    private static Image[] textures = null;// background textures
    // private state
    private Warp6Board b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("rack",2);
    private Rectangle scoreRects[] = addRect("score",2);
    
    private Warp6Cell roll_anim_cell = null;
    private long roll_anim_stop = 0;

    public synchronized void preloadImages()
    {	
    	if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    	Warp6Chip.preloadImages(loader,ImageDir);
    	SoundManager.preloadSounds(sucking_sound);
        images = StockArt.preLoadArt(loader,ImageDir, ImageFileNames,SCALES); // load the main images
        textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = images[BOARD_INDEX].image;
    }

    Color Warp6MouseColors[] = { Color.white,Color.yellow };
    Color Warp6MouseDotColors[] = { Color.black,Color.black}; 
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	enableAutoDone = true;
        super.init(info,frame);
        MouseColors = Warp6MouseColors;
        MouseDotColors = Warp6MouseDotColors;
        int randomKey = sharedInfo.getInt(OnlineConstants.RANDOMSEED,-1);

        b = new Warp6Board(randomKey,info.getString(GameInfo.GAMETYPE, Warp6_Standard_Init),
        		getStartingColorMap());
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
        b.doInit(b.gametype,b.randomKey);				// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
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
    	int minLogW = fh*14;	
    	int vcrw = minLogW;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        int stateH = fh*5/2;
       // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*2.5,
    			fh*3,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
       	//layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main)+stateH;
    	int mainW = G.Width(main);
    	int mainH = G.Height(main)-stateH*2;
    	
    	// There are two classes of boards that should be rotated. For boards with a strong
    	// "my side" orientation, such as chess, use seatingFaceToFaceRotated() as
    	// the test.  For boards that are noticably rectangular, such as Push Fight,
    	// use mainW<mainH
        int nrows = 15;  // b.boardRows
  	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/nrows,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(nrows*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH;
        int stateX = boardX;
        placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom,boardW,stateH,goalRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle score = scoreRects[player];
    	G.SetRect(score,	x,	y,	unitsize*2,	unitsize*2);
    	Rectangle box =  pl.createRectangularPictureGroup(x+2*unitsize,y,unitsize);
    	G.union(box, score);
    	G.SetRect(chip,x,G.Bottom(box),unitsize*18,unitsize*3);
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*4 : 0;
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	G.union(box, done,chip,score);
    	pl.displayRotation = rotation;
    	return(box);
    }
 

    public int scaleCell(int cellsize,int x,int y,Rectangle r)
    {
    	if(G.pointInRect(x,y,r))
    	{
    		double scl = ((0.2*(y-G.Top(r)))/G.Height(r))+0.9;
    		return((int)(scl*cellsize));
    	}
    	return(cellsize);
    }
    // override so CELLSIZE has a resonable value
    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	Warp6Chip ic = Warp6Chip.getChip(idx);
    	ic.drawChip(g,this,scaleCell(CELLSIZE,xp,yp,boardRect),xp,yp,null);
    }


    /** this is used by the game controller to supply entertainment strings to the lobby */
    public String gameProgressString()
    {	// this is what the standard method does
    	// return ((reviewer ? s.get(Reviewing) : ("" + viewMove)));
    	return(super.gameProgressString()
    			+" "+(5-b.chipsInWarp[0])
    			+" "+(5-b.chipsInWarp[1]));
    }

    Image scaled = null;
    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean review = reviewMode() && !mutable_game_record;
      // erase
      Warp6Board gb = disB(gc);
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
      
      // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid
      scaled = images[BOARD_INDEX].getImage(loader).centerScaledImage(gc, boardRect,scaled);
	    {	// good for board-skew : gb.SetDisplayParameters(0.7,0.8,  0.0,0.00,  2.3, .15, 0.25);
	    	// good for board-skew2 gb.SetDisplayParameters(0.67,0.72,  0.0,0.00,  14.5, .22, 0.25);
	    	// good for board-skew3 gb.SetDisplayParameters(0.54,0.80,  0.0,-0.30,  7.0, .250, 0.32);
	    	gb.SetDisplayParameters(
	    		 1.0, //0.93,	// scale 
	    		 1.0,	// yscale
	    		 0.0,	// xoff
	    		 0.0,//-0.1,	// yoff
	    		 0.0	// rot
	    		 );
    	}
        gb.SetDisplayRectangle(boardRect);
     
      //gb.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
      if( SHOW_DEBUG_GRID)
      {
      for(Warp6Cell c = gb.allCells;  c!=null;  c=c.next)
      { int xx = G.Left(boardRect) + gb.cellToX(c.col,c.row);
      	int yy = G.Bottom(boardRect) - gb.cellToY(c.col,c.row);
      	Warp6Cell next = c.exitTo(Warp6Board.CELL_UP());
      	if(next!=null)
      	{ int x2 =  G.Left(boardRect) + gb.cellToX(next.col,next.row);
      	  int y2 = G.Bottom(boardRect) - gb.cellToY(next.col,next.row);
      	  GC.setColor(gc,Color.red);
      	  GC.drawLine(gc,xx,yy,x2,y2);
      	}
      	Warp6Cell up = c.exitTo(Warp6Board.CELL_RIGHT());
      	if(up!=null)
      	{int x2 =  G.Left(boardRect) + gb.cellToX(up.col,up.row);
      	 int y2 = G.Bottom(boardRect) -gb.cellToY(up.col,up.row);
      	 GC.setColor(gc,Color.yellow);
      	 GC.drawLine(gc,xx,yy,x2,y2);
      	}
      }
      }
    }
    private void DrawScore(Graphics gc,Rectangle r,int player,double scale)
    {	if(gc!=null)
    	{
    	int idx = 6-b.chipsInWarp[player];
    	Warp6Chip cc = (idx>0) ? Warp6Chip.getChip(b.playerColor[player],6,idx) : null;
    	if(cc!=null) { cc.drawChip(gc,this,(int)(G.Width(r)*scale),G.centerX(r),G.centerY(r),null); }
    	}
    }
    
    private void drawRack(Graphics gc,HitPoint highlight,Warp6Board gb,int pla,Rectangle r)
    {	boolean useWarp = (gb.moveNumber>50)||(gb.chipsInWarp[pla]>0);
    	Warp6Cell rack[] = useWarp?gb.warp[pla]:gb.rack[pla];
    	Warp6Cell warp[] = useWarp?gb.rack[pla]:gb.warp[pla];	// get the other set in for animation 
    	int space = G.Width(r)/rack.length;
    	Warp6State state = gb.getState();
    	int x = G.Left(r)+space/2;
    	int y = G.centerY(r);
    	int mo = gb.movingObjectIndex();
    	Warp6Cell hitCell = null;
    	for(int i=0;i<rack.length;i++)
    	{	Warp6Cell ch = rack[i];
    		Warp6Cell och = warp[i];	// other set for animations
    		boolean canHit = gb.LegalToHitChips(pla,ch);
    		if(ch.drawStack(gc,canHit?highlight:null,x,y,this,0,space,1.0,null))
    		{
    			hitCell = ch;
    		}
    		och.copyCurrentCenter(ch);
 
    		switch(state)
    		{
    		case PLACE_STATE:
    		case CONFIRM_STATE:
    			if(canHit)
    				{ StockArt.SmallO.drawChip(gc,this,space*2,x,y+space/2,null);
    				}
    			break;
    		case PUZZLE_STATE:
    			if(mo<0)
    			{ handleRollGesture(ch,highlight,x,y);
    			}
    			break;
    		default: ;
    		}
    		x += space;
    	}
    	if(hitCell!=null)
    	{
    		highlight.spriteColor = Color.red;
    		highlight.awidth = CELLSIZE/2;
    	}
    }
    private void handleRollGesture(Warp6Cell cell,HitPoint highlight,int xpos,int ypos)
    {	Warp6Chip chip = cell.topChip();
        if((chip!=null)&&(highlight!=null)&&(cell==highlight.hitObject))
        {
        boolean isCenter = (G.Top(highlight)<ypos) 
        						|| G.pointInside(highlight, xpos, ypos, (int)(0.3*CELLSIZE));
        WarpId hitCode = null;
        if(b.getState()==Warp6State.CONFIRM_STATE)
        {
        if(b.lastMove!=null)
        {
        switch(b.lastMove.op)
        {
        case MOVE_ROLLUP:	hitCode = WarpId.RollDown; break;
        case MOVE_ROLLDOWN: hitCode = WarpId.RollUp; break;
		default:
			break;
        }}
        } else  if(!isCenter)         
        {	// mouse adjacent to a chip in puzzle mode
        	{	boolean left = (xpos>G.Left(highlight));
        		if(left?chip.canSub():chip.canAdd())
        		{
        		hitCode = left?WarpId.RollDown:WarpId.RollUp;
        		}
        	}
        }
        
        if(hitCode!=null)
        {
       	int off = (hitCode==WarpId.RollUp) ? CELLSIZE/2 : -CELLSIZE/2;
       	highlight.hitCode = hitCode;
   		highlight.arrow = images[(hitCode==WarpId.RollDown)? MINUS1_INDEX :PLUS1_INDEX];
   		highlight.awidth = 3*CELLSIZE/2;
   		highlight.hitObject = cell;
   		highlight.a_x = G.Left(highlight)-off;
   		highlight.a_y = G.Top(highlight)-off;
	
        }}
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, Warp6Board rb, Rectangle brect, HitPoint highlight)
    {	Hashtable<Warp6Cell,Warp6Cell> dests = rb.getMoveDests();
    	Warp6State state = rb.getState();
     	int dotsize = Math.max(2,CELLSIZE/7);
     	int mov = rb.movingObjectIndex();
     	long now = G.Date();
     	if(roll_anim_cell!=null) 
     		{ 
     		if(now > roll_anim_stop) { roll_anim_cell=null; } else { repaint(20); }
     		} 
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	

         { //where we draw the grid
  	        for (int last=rb.lastRowInColumn('A'),row = last,frow=rb.firstRowInColumn('A');
  	        	row>=frow; 
  	        	row--)	// back to front
        	{
            Warp6Cell cell = rb.getCell(row);
            //if(cell!=rb.center)
            {
            boolean isADest = dests.get(cell)!=null;
            boolean isSource = rb.isSource(cell);
            boolean canHit = rb.LegalToHitBoard(cell);
            int ypos = G.Bottom(brect) - rb.cellToY('A', row);
            int xpos =  G.Left(brect) + rb.cellToX('A', row);
       		int scl = scaleCell(CELLSIZE,xpos,ypos,boardRect);
       		//StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
       		Warp6Chip chip = cell.topChip();
       		Warp6Cell lastDropped = rb.lastDropped;
            if(cell==roll_anim_cell)
            {	
            	if(chip!=null)
            	{	int show = (int)(now%chip.numSides)+1;
             		Warp6Chip alt = Warp6Chip.getChip(chip.color,chip.numSides,show);
            		alt.drawChip(gc,this,scl,xpos,ypos,null);
            		
            	}
            }
            else
            	{
            	if(use_grid && (gc!=null) && (chip!=null))
            	{
            	GC.setFont(gc,largeBoldFont());
            	GC.Text(gc,false,xpos+scl/2,ypos-scl/2,scl,scl/2,Color.white,null,""+cell.row);
            	}
            	if(cell==lastDropped)
            	{
            		StockArt.SmallO.drawChip(gc,this,scl*4,xpos,ypos,null);
            	}
            	cell.drawStack(gc,canHit?highlight:null,xpos,ypos,this,0,scl,1.0,null);
            	
            	}
            
           if(((state==Warp6State.PUZZLE_STATE)||(state==Warp6State.PLAY_STATE)||(state==Warp6State.CONFIRM_STATE))
        		&& canHit
        		&& (mov<0))
            	{handleRollGesture(cell,highlight,xpos,ypos);
            	}

        		
         	// temp for grid setup
        	//G.DrawAACircle(gc,xpos+dotsize,ypos,dotsize,Color.red,Color.gray,true);
        	if(isSource)
        	{	GC.cacheAACircle(gc,xpos+dotsize,ypos,dotsize,Color.blue,Color.gray,true);
        	}
        	if(isADest)
        	{
        		GC.cacheAACircle(gc,xpos+dotsize,ypos,dotsize,Color.red,Color.gray,true);
        	}
         	if(cell==rb.rollCell)
         	{	StockArt.Rotate_CW.drawChip(gc,this,3*CELLSIZE/2,xpos,ypos,null);
         	}
        }}}
       	
        if((highlight!=null)&& (highlight.hitObject!=null) && (highlight.hitCode instanceof WarpId))
        {	WarpId hitCode = (WarpId)highlight.hitCode;
        		switch(hitCode)
        		{
        		case RollUp:
        		case RollDown:
        			break;
        		default:
       			highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
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
    {  Warp6Board gb = disB(gc);
       boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      Warp6State vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);

       drawBoardElements(gc, gb, boardRect, ot);
       
       boolean planned = plannedSeating();
       for(int player = FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
       {
	       commonPlayer pl = getPlayerOrTemp(player);
	       pl.setRotatedContext(gc, highlight,false);
	       DrawScore(gc,scoreRects[player],player,0.7);
	       drawRack(gc,ot, gb,player,chipRects[player]);
	       
	   	   if(planned && gb.whoseTurn==player)
		   {
			   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? select : null), 
						HighlightColor, rackBackGroundColor);
		   }
	
	       pl.setRotatedContext(gc, highlight, true);      
       }
 
       GC.setFont(gc,standardBoldFont());
 	   drawPlayerStuff(gc,(vstate==Warp6State.PUZZLE_STATE),ourSelect,
  	   			HighlightColor, rackBackGroundColor);
  	 
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation(); 
       if (vstate != Warp6State.PUZZLE_STATE)
        {
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);

        }
        standardGameMessage(gc,messageRotation,
        		vstate==Warp6State.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=Warp6State.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        DrawScore(gc,iconRect,gb.whoseTurn,0.5);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);
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
        if((mm.op==MOVE_DONE) && (b.rollCell!=null))
        {	roll_anim_stop = G.Date()+600;
        	roll_anim_cell = b.rollCell;
        }
        else { roll_anim_cell = null; }
        
        handleExecute(b,mm,replay);
        
        startBoardAnimations(replay);
        
        if(replay.animate) { playSounds((Warp6Movespec)mm); }
 
        return (true);
    }
     
     // animations are serial
     void startBoardAnimations(replayMode replay)
     {	try {
    	 double time = 0.0;
         if(replay.animate)
     	{	int lim = b.animationStack.size();
     		if(lim>0)
     		{
     		Warp6Cell finalDest = b.animationStack.top();
     		Warp6Chip chip = finalDest.topChip();
     		for(int idx = 0; idx<lim; idx+=2)
     		{
     		Warp6Cell dest = b.animationStack.elementAt(idx+1);
     		Warp6Cell src = b.animationStack.elementAt(idx);
     		if(idx==0 && dest.onBoard &&src.onBoard)
     		{while(src.row+1<dest.row)
     			{	// first is a slide along the rim
     			Warp6Cell d2 = b.getCell(src.col,src.row+1);
     			SimpleSprite sp = startAnimation(src,d2,chip,CELLSIZE,time,0);
     			time += sp.getDuration();
     			src = d2;
     			}
     		}
     		SimpleSprite sp = startAnimation(src,dest,chip,CELLSIZE,time,0);
     		time += sp.getDuration();
     		}}
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
        return (new Warp6Movespec(st, player));
    }
    


private void playSounds(Warp6Movespec m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_DONE:
    	if(roll_anim_cell!=null)
    	{	playASoundClip(diceSoundName,50);
    	}
    	break;
    case MOVE_DROPB:
    case MOVE_BOARD_BOARD:
    case MOVE_ONBOARD:
    	WarpId finalRack = allIds[m.undoInfo/10000];
    	if((finalRack==WarpId.FirstPlayerWarp) || (finalRack==WarpId.SecondPlayerWarp) || (m.to_row>=NUMPOINTS))
    	{
    	  playASoundClip(sucking_sound,50);
    	}
    	else
    	{ playASoundClip(light_drop,50);
    	}
    	break;
    case MOVE_ROLLUP:
    case MOVE_ROLLDOWN:
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
    	if (hp.hitCode instanceof WarpId)// not dragging anything yet, so maybe start
        {

        WarpId hitObject = (WarpId)hp.hitCode;
		Warp6Cell cell = hitCell(hp);
		Warp6Chip chip = (cell==null) ? null : cell.topChip();
		
        if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case FirstPlayerRack:
        case SecondPlayerRack:
        case FirstPlayerWarp:
        case SecondPlayerWarp:
        	if(b.movingObjectIndex()<0)
        	{	PerformAndTransmit("pick "+hitObject.shortName+" "+cell.row);
        	}
        	break;
	    case BoardLocation:
	    	if(cell.chip!=null)
	    		{
	    		PerformAndTransmit("Pickb "+cell.row+" "+chip.pieceNumber());
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
        if(!(id instanceof WarpId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	WarpId hitObject = (WarpId)hp.hitCode;
		Warp6Cell cell = hitCell(hp);
		Warp6Chip cup = (cell==null) ? null : cell.topChip();
		Warp6State state = b.getState();	// state without resignation
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
	    case RollUp:	
        case RollDown:
      	  	  PerformAndTransmit(hitObject.shortName+" "+cell.rackLocation().shortName+" "+cell.row);
      	  	  break;              
        case FirstPlayerRack:
        case SecondPlayerRack:
        case FirstPlayerWarp:
        case SecondPlayerWarp:
        	if(b.movingObjectIndex()>=0)
        	{	PerformAndTransmit("drop "+hitObject.shortName+" "+cell.row);
        	}
        	else
        	{	PerformAndTransmit("pick "+hitObject.shortName+" "+cell.row);
        	}
        	break;
        case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PLACE_STATE:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) 
					{ 
					  PerformAndTransmit("dropb "+cell.row + " "+b.nextRandom()); 
					}
				}
				else if(cup!=null)
				{
				PerformAndTransmit( "Pickb "+cell.row+" "+cup.pieceNumber());
				}
				break;
			}
			break;
        }	
        }
    }

    public String gameType() { return(b.gametype+" "+b.randomKey); }
    public String sgfGameType() { return(warp6_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long ran = G.LongToken(his);
	    b.doInit(token,ran);
	}


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new Warp6Play()); }

     /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/23/2023
		4213 files visited 0 problems
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
        		long ran = G.LongToken(tok);
                b.doInit(gametype,ran);
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

