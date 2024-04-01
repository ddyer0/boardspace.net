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
package santorini;

import java.awt.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import common.GameInfo;
import lib.Graphics;
import lib.Image;
import lib.AR;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.StockArt;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;
import online.common.*;

/**
 * 
 * Change History
 *
 * May 2007 initial work in progress. 
 *

 
 *
*/
public class SantoriniViewer extends CCanvas<SantoriniCell,SantoriniBoard> implements SantoriniConstants
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(144,165,199);
    private Color boardBackgroundColor = new Color(200,200,200);
    
 
    // images
    private static Image[] textures = null;// background textures
    
    // private state
    private SantoriniBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle manRects[] = addRect("man",2);
    private Rectangle godRects[] = addRect("god",2);
    private Rectangle squareRect = addRect("squareRect");
    private Rectangle rightView = addRect("rightView");
    private boolean useLeftView = true;
    private boolean drawingAltView = false;
    private boolean newView = false;
    public boolean usePerspective() 
    {
    	return(getAltChipset()<=1);
    }
    public int getAltChipset() 
    { return( (currentViewset==ViewSet.Reverse) ? 2 : useLeftView^drawingAltView?0:1); 
    }
   
    private SantorId bigGod = null;
    private Rectangle bigGodRect = null;
    public synchronized void preloadImages()
    {	
       	SantoriniChip.preloadImages(loader,ImageDir);
        if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
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
    {
        super.init(info,frame);
  	  	labelFont = largeBoldFont();
  	    MouseColors[0] = Color.red;
  	    MouseColors[1] = new Color(0.1f,0.9f,0.1f);
        
  	    // santorini is an odd duck, it's two "colors" are cube and cylinder
  	    int first = info.getInt(OnlineConstants.FIRSTPLAYER,0);
        int map[]=AR.intArray(2);
        if(first==1) { map[0] = 1; map[1]=0; }
        b = new SantoriniBoard(info.getString(GameInfo.GAMETYPE, Santorini_INIT),map);
        useDirectDrawing(true);
        doInit(false);
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	InternationalStrings.put(SantoriniStrings);
        	InternationalStrings.put(SantoriniStringPairs);
        }
       
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
    	{
        	startFirstPlayer();
    	}
   }
    boolean horizontal = false;
    public Rectangle createPlayerGroup(int player,int x,int y,double rot,int unit)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int manW = unit*4;
    	int doneW = plannedSeating() ? unit*5 : 0;
    	Rectangle box = pl.createRectangularPictureGroup(x+manW,y,unit);   
    	Rectangle man = manRects[player];
    	Rectangle god = godRects[player];
    	Rectangle done = doneRects[player];
    	boolean gods = (b.variation!=Variation.santorini); 	
    	int godw = gods ? unit*4 : 0;
    	G.SetRect(man, x, y, manW, manW);
    	G.SetRect(god,G.Right(box),y,godw,godw*3/2);
    	if(horizontal && doneW>0 && godw==0)
    	{
    		G.SetRect(done, G.Right(box)+unit/4, G.Top(box)+unit/2,doneW,doneW/2);
    	}
    	else
    	{
       	G.SetRect(done, x, y+manW,doneW,doneW/2);
    	}
       	pl.displayRotation = rot;
    	G.union(box, man,god,done);
    	return(box);
    }
    
    public void setLocalBounds(int x, int y, int width, int height)
    {	setLocalBoundsV( x,  y, width,  height,new double[] {1,-1});
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double a)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	horizontal = a<0;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*20;	
    	int vcrW = fh*16;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        boolean perspective = usePerspective();
        int ncols = 15;
        int nrows = perspective ? 12 : 15;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.2,	// 1.2:1 aspect ratio for the board
    			fh*3,	// maximum cell size
    			0.2		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW, minLogH*3/2);
    	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
       	layout.placeTheVcr(this,vcrW,vcrW*3/2);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	boolean vertical = mainH>mainW;
    	int cols = vertical ? ncols : ncols+3;
    	int rows = vertical ? nrows+3 : nrows;
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/cols,(double)mainH/rows);
    	int CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE*2;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (ncols*CELLSIZE);
    	int boardWF = (cols*CELLSIZE);
    	int boardH =  (nrows*CELLSIZE);
    	int boardHF = (rows*CELLSIZE);
    	int squareW = CELLSIZE*2;
    	int squareH = CELLSIZE*4;
    	int extraW = Math.max(0, (mainW-boardWF)/2);
    	int extraH = Math.max(0, (mainH-boardHF)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	int boardRight = boardX+boardW;
       	layout.returnFromMain(extraW,extraH);
    	if(vertical)
    		{
    		G.SetRect(squareRect,boardX+(boardW-squareH)/2,boardBottom+CELLSIZE,squareH,squareW); 		
    		}
    		else
    		{
    		G.SetRect(squareRect,boardRight,boardY+(boardH-squareH)/2,squareW,squareH);
    		}

    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        int stateH = fh*5/2;
        
        G.placeStateRow(stateX,stateY,boardW, stateH,iconRect,stateRect,annotationMenu,rightView,liftRect,viewsetRect,noChatRect);

    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        return boardW*boardH;
    }

 
    private void drawLiftRect(Graphics gc,SantoriniBoard gb,HitPoint highlight)
    {	
		drawLiftRect(gc,liftRect,highlight,textures[LIFT_ICON_INDEX]); 
		//G.centerImage(gc,textures[useLeftView?LEFT_VIEW_INDEX:RIGHT_VIEW_INDEX],rightView,this);
		if(usePerspective())
		  {
		  SantoriniChip view = useLeftView ? SantoriniChip.RightView : SantoriniChip.LeftView;
		  if(view.drawChip(gc, this, rightView, highlight, SantorId.RightView))
		  	{
			  highlight.spriteRect = rightView;
			  highlight.spriteColor = Color.red;
			  GC.frameRect(gc,Color.black,rightView);
		  	}
		  }
    }
	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, int forPlayer, Rectangle r,
    		SantoriniCell []chips,HitPoint highlight,int lim)
    {	boolean canHit = b.LegalToHitChips(forPlayer);
    	int w = G.Width(r);
    	int h = G.Height(r);
    	boolean vertical = w<h;
    	int step = vertical ? w : h;
    	int xp = G.Left(r)+step/2;
    	int yp = G.Top(r)+step/2;
    	SantoriniCell hitCell = null;
        boolean canDrop = hasMovingObject(highlight);
        for(int i=0;i<lim;i++)
    	{
        SantoriniCell thisCell = chips[i];
        SantoriniChip thisChip = thisCell.topChip();
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = "";
        if(thisCell.drawStack(gc,this,pt,step,xp,yp,step/10,0,msg))
        	{
        	hitCell = thisCell;
        	}
        if(vertical) { yp+=step; }
        	else 
        	{ // normally this would be just "step", but the artwork for the blue dome is offsert
        	  xp+=step+step/3; 
        	  yp -= step/5; 
        	}
        }
    	if(hitCell!=null)
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = step/3;
        	highlight.spriteColor = Color.red;
        }
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
    	SantoriniChip ch = SantoriniChip.getChip(obj);// Tiles have zero offset
    	int sz = scaleCell(b.displayParameters.CELLSIZE,xp,yp,boardRect);
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
    {boolean review = reviewMode() && !mutable_game_record;
     SantoriniBoard gb = disB(gc);
      // erase
      //gc.setColor(reviewMode() ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], 
      //	  brect.x,brect.y,brect.width,brect.height,this);
	  sizeToRectangle(gb,useLeftView,boardRect);	// resize back to the real rectangle

      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    public int scaleCell(double cellsize,int x,int y,Rectangle r)
    {
    	if(usePerspective() && G.pointInRect(x,y,r))
    	{
    		double scl = ((0.2*(y-G.Top(r)))/G.Height(r))+0.8;
    		return((int)(scl*cellsize));
    	}
    	return((int)cellsize);
    }
    SantoriniChip lastGodSelected = null;
    private void displayGods(Graphics gc, SantoriniBoard gb, Rectangle brect, HitPoint highlight,int cols,int rows)
    {	commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
    	pl.setRotation(brect,gc, highlight, false);
		// select two gods
		{
		int w = G.Width(brect);
		int h = G.Height(brect);
		int xstep = w/cols;
		int ystep = h/(rows+1);
		int xp = xstep/2;
		int xw = xstep-xstep/3;
		int xw2 = xstep-xstep/7;
		int yp = rows==1 ? ystep/2+ystep/4 : ystep/2;
		int yh = rows==1 ? ystep+ystep/2 : ystep;
		int l = G.Left(brect);
		int t = G.Top(brect)+ystep/2;
		int nstep = gb.gods.height();
		int lastGodSelectedX = -1;
		for(int step=0;step<nstep;step++)
			{
				SantoriniChip god = gb.gods.chipAtIndex(step);
				boolean selected = gb.godSelections()[step];
				if(selected && (god==lastGodSelected)) { lastGodSelectedX = xp; }
				if(god.drawChip(gc, this, xw,l+xp,t+yp,highlight,SantorId.GodsId,null,1.5,1.0))
				{
				}
				if(selected)
				{
					GC.frameRect(gc,Color.red, l+xp-xw2/2,t+yp-yh/2,xw2,yh);
				}
				xp += xstep;
				if(xp>w) { xp = xstep/2; yp+=ystep; }
				}
			if(lastGodSelectedX>=0 && rows>1)
			{
			 int lx = lastGodSelectedX>w/2 ? lastGodSelectedX-w/4 : lastGodSelectedX+w/4;
			 lastGodSelected.drawChip(gc, this, w/3,
					 l+lx,t+h/3,highlight,SantorId.Godless,null,2.5,1.0);
			 GC.frameRect(gc,Color.black, l+lx-w/6,(int)(t+h*0.04),w/3,(int)(h*0.595));
			}
		}
    	pl.setRotation(brect,gc, highlight, true);

		return;
    }
    private void drawBigGod(Graphics gc,Rectangle r,HitPoint hp,commonPlayer pl)
    {	if(bigGod!=null)
    	{
    	int w = G.Width(r);
    	int h = G.Height(r);
    	int sz = 3*w; 
    	int xp = Math.min(Math.max(w+w/2, G.centerX(r)-w),G.Width(fullRect)-w-w/2);
    	int yp = Math.min(Math.max(h+h/2, G.centerY(r)-h),G.Height(fullRect)-h-h/2);
    	GC.setRotation(gc,pl.displayRotation,xp,yp);
    	SantoriniChip.findGod(bigGod).drawChip(gc, this,sz,xp,yp,hp,bigGod,null,0.66,1.0);
    	GC.setRotation(gc,-pl.displayRotation,xp,yp);
    	}
    }
    private void drawGod(Graphics gc,HitPoint hp,SantorId id,Rectangle r)
    {	SantoriniChip god = SantoriniChip.findGod(id);
    	if(god!=null)
    	{
     			if(god.drawChip(gc, this,G.Width(r),G.centerX(r), G.centerY(r),hp,god.id,null,0.66,1.0))
					{
    				hp.spriteColor = Color.red;
    				hp.spriteRect = bigGodRect = r;
					}
    	}
    }
    
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, SantoriniBoard gb, Rectangle brect, HitPoint highlight,HitPoint anyHighlight,boolean leftView)
    {
    	Hashtable<SantoriniCell,SantoriniMovespec>targets = gb.getTargets();
    
    	switch(gb.getState())
    	{
    	case GodSelect:
    	case GodSelectConfirm:
    		displayGods(gc,gb,brect,highlight,5,3);
    		return;
    	case GodChoose:
    	case GodChooseConfirm:
    		// select a single god
    		displayGods(gc,gb,brect,highlight,2,1);
    		return;
    	default: 
    		lastGodSelected = null;
    		break;
    	}
    	sizeToRectangle(gb,leftView,brect);
    	drawingAltView = leftView!=useLeftView;
     	boolean dolift = doLiftAnimation();
       	//
        // now draw the contents of the board and anything it is pointing at
        //
     	
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left back-front order so the
        // solid parts will fall on top of existing shadows
        SantoriniCell sourceCell = gb.sourceCell();
        SantoriniCell destCell = gb.destCell();
    	boolean candrop = hasMovingObject(anyHighlight);
       	boolean perspective  = usePerspective();
       	Enumeration<SantoriniCell>cells = gb.getIterator(perspective?(leftView?Itype.TBLR:Itype.TBRL) : Itype.TBRL);
       	double xs = perspective ? 0 : 0.075;
       	double ys = perspective ? 0.14 : 0.075;
       	
       	while(cells.hasMoreElements())
       	{
       		SantoriniCell ccell = cells.nextElement();
            boolean isADest = candrop && (targets!=null) && (targets.get(ccell)!=null);
            boolean isASource = !candrop && ( (ccell==sourceCell)||(ccell==destCell));
            int ypos = G.Bottom(brect) - gb.cellToY(ccell);
            int xpos = G.Left(brect) + gb.cellToX(ccell);
       		int scl = scaleCell(gb.displayParameters.CELLSIZE,xpos,ypos,boardRect);
      		boolean canHit = gb.LegalToHitBoard(ccell,targets);
       		int hgh = ccell.height();
       		
       		String msg = 
       			(dolift && (hgh>1))
       				? "" + (hgh-((ccell.topChip()==SantoriniChip.MainTile)?1:2))
       				: null;
            if(ccell.drawStack(gc,this,canHit?highlight:null,scl,xpos,ypos,liftSteps,xs,ys,msg))
            {	highlight.awidth = scl/3;
    			highlight.arrow = (candrop||(ccell.isEmpty()))
					? StockArt.DownArrow 
					: StockArt.UpArrow;
    			highlight.spriteColor = Color.red;
            }
            if(isASource)
            {GC.cacheAACircle(gc,xpos,ypos,2,Color.green,Color.yellow,true);
        	} else
        	if(isADest)
        	{GC.cacheAACircle(gc,xpos,ypos,2,Color.red,Color.yellow,true);
        	}
       	}
    	drawingAltView=false;
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  SantoriniBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = ((bigGod!=null) || (moving && !reviewMode())) ? null : highlight;	// hit if not dragging
      boolean planned = plannedSeating();
       SantoriniState vstate = gb.getState();
        GC.fillRect(gc,rackBackGroundColor,logRect);
        gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
        drawLiftRect(gc,gb,ourSelect);
   
        drawBoardElements(gc, gb, boardRect, ot,ourSelect,useLeftView);
        GC.setFont(gc,standardBoldFont());
        drawPlayerStuff(gc,(vstate==SantoriniState.PUZZLE_STATE),ourSelect,
 	   			HighlightColor, rackBackGroundColor);

        if(gc!=null) 
        	{
        	b.cube_rack[1].copyCurrentCenter(b.cube_rack[0]);
          b.cylinder_rack[1].copyCurrentCenter(b.cylinder_rack[0]);
        }
        DrawCommonChipPool(gc, -1, squareRect, b.reserve,ot,2);
        
		commonPlayer cpl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = cpl.messageRotation();
		
		if (vstate != SantoriniState.PUZZLE_STATE)
        {
	        GC.setFont(gc,standardBoldFont());
			if(!planned)
				{handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);    
        }

		
        standardGameMessage(gc,messageRotation,
        		vstate==SantoriniState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=SantoriniState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
		gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalString),progressRect, goalRect);
        drawVcrGroup(ourSelect, gc);
        drawViewsetMarker(gc,viewsetRect,ourSelect);

        for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX; i++)
        {
        commonPlayer pl = getPlayerOrTemp(i);
        pl.setRotatedContext(gc, highlight,false);
        DrawCommonChipPool(gc, i,manRects[i], b.playerRack[i],ot,1); 
        if(bigGod==null) { drawGod(gc,ourSelect,gb.playerGodI[i],godRects[i]); }
        if(planned && (i==gb.whoseTurn))
        	{
 	       	GC.setFont(gc,standardBoldFont());
 			handleDoneButton(gc,doneRects[i],(gb.DoneState() ? ourSelect : null), 
 					HighlightColor, rackBackGroundColor);
       	
        	}
        pl.setRotatedContext(gc, highlight,true);
        }
        drawBigGod(gc,bigGodRect,select,cpl);
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
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Chained);

        if(replay!=replayMode.Replay) { playSounds(mm); }
 
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
        return (new SantoriniMovespec(st, player));
    }
    

    

private void playSounds(commonMove m)
{

    // add the sound effects
    switch(m.op)
    {
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
    case MOVE_DOME:
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
    	if (hp.hitCode instanceof SantorId) // not dragging anything yet, so maybe start
        {

        SantorId hitObject = (SantorId)hp.hitCode;
		SantoriniCell cell = b.getCell(hitCell(hp));
		SantoriniChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
 	    case Cube_Rack:
	    	PerformAndTransmit("Pick W "+cell.row);
	    	break;
	    case Cylinder_Rack:
	    	PerformAndTransmit("Pick B "+cell.row);
	    	break;
	    case Reserve_Rack:
	    	PerformAndTransmit("Pick R "+cell.row);
	    	break;
	    case BoardLocation:
	    	if(!cell.isEmpty())
	    		{
	    		SantoriniState state = b.getState();
	    		SantoriniChip top = cell.topChip();
	    		if((state==SantoriniState.PUZZLE_STATE) 
	    				|| ((state!=SantoriniState.BuildAgain_State)
	    						&& (b.isDest(cell) || top.isMan())))
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
	    		}}
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
        if(!(id instanceof SantorId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
   		SantorId hitObject = (SantorId)hp.hitCode;
        SantoriniState state = b.getState();
		SantoriniCell cell = hitCell(hp);
		SantoriniChip chip = (cell==null) 
								? ((hp.hitObject instanceof SantoriniChip) 
										? (SantoriniChip)hp.hitObject 
										: null) : cell.topChip();
		switch (hitObject)
        {
        default:
    		if(chip!=null && chip.isGod()) 
    			{ bigGod = bigGod==null ? hitObject : null;
     			}
    		else { throw  G.Error("Hit Unknown: %s", hitObject); }
    		break;
        case Godless:
        	lastGodSelected = null;
        	break;
        case GodsId:
        	PerformAndTransmit("select "+chip.id);
        	lastGodSelected = chip;
         	break;
        case RightView: useLeftView = !useLeftView; newView=true; break;
        case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case MoveOrPassState:
			case MAN1_STATE:
			case MAN2_STATE:
			case Build2_State:
			case BuildAgain_State:
			case BuildOrMoveState:
			case MoveOnLevelState:
			case BUILD_STATE:
				if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				break;
	        case CONFIRM_WIN_STATE:
			case CONFIRM_STATE:
			case MOVE_STATE:
			case MoveOpponentState:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ 	SantoriniChip top = cell.topChip();
					String op = "Dropb ";
					if(top!=null && top.isMan())
					{
						switch(b.activeGod(b.whoseTurn))
						{
						default: throw G.Error("Not expecting god "+b.activeGod(b.whoseTurn));
						case Ares: 
							op = "droppush ";
							break;
						case Apollo:
							op = "dropswap ";
							break;
						case Godless:
							break;
						}
					}
					if(cell!=null) { PerformAndTransmit(op+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
				}
				break;
			}
			break;
        case Reserve_Rack:
        case Cylinder_Rack:
        case Cube_Rack:
        	{
        	int mov = b.movingObjectIndex();
        	String col = hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                	case MOVE_STATE:
            		PerformAndTransmit(RESET);
            		break;
               	case BUILD_STATE:
                case MAN1_STATE:
                case BuildAgain_State:
                case MAN2_STATE:
                case BuildOrMoveState:
              	case PUZZLE_STATE:
            		PerformAndTransmit("Drop "+col+" "+cell.row);
            		break;
            	}
			}
         	}
            break;
        }
        }
    }

    private void sizeToRectangle(SantoriniBoard gb,boolean leftView,Rectangle r)
    {
    	if(usePerspective())
    	{
	    if(leftView)
    	{
    	gb.SetDisplayParameters(0.90,1.0,
	    		0.1,-0.3,
	    		-20.0,	// rotation
	    		0.14,		// x skew
	    		0.18,		// y skew
	    		0.13		// slice
	    		);
    	} else
    	{gb.SetDisplayParameters(0.80,0.9,
    		    		0.2,-0.4,
    		    		26.0,	// rotation
    		    		0.12,		// x skew
    		    		0.16,		// y skew
    		    		-0.09		// slice
    			);
    	}}
    	else
    	{
    		gb.SetDisplayParameters(0.95,1,
		    		0,0.1,
		    		0);	
    	}
    gb.SetDisplayRectangle(r);	
    }
    		
    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     */
    public void drawCanvas(Graphics offGC, boolean complete,HitPoint hp)
    {	
       	complete |= newView;
       	newView = false;
		super.drawCanvas(offGC,complete,hp);
 
    }
    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Santorini_SGF); }

    
    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
        b.doInit(token);
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


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new SantoriniPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 15271 files visited 0 problems
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
}

