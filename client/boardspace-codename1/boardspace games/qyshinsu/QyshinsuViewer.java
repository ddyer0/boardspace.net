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
package qyshinsu;

import com.codename1.ui.geom.Point;
import com.codename1.ui.geom.Rectangle;
import bridge.*;
import common.GameInfo;

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
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

/**
 * 
 * Change History
 *
 * Nov 2008 Initial version 
 *

 
*/
public class QyshinsuViewer extends CCanvas<QyshinsuCell,QyshinsuBoard> implements QyshinsuConstants, GameLayoutClient
{	
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,185,185);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private Color chatBackgroundColor = new Color(234,225,225);
    // images
    private static Image[] images = null; // images of black and white gobblets and board
    private static Image[] textures = null;// background textures
    
    // private state
    private QyshinsuBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    public JCheckBoxMenuItem colorBlindOption = null;
    boolean colorBlind = false;
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle repRect = addRect("repRect");
    
    private double lineStrokeWidth = 1;
    public synchronized void preloadImages()
    {	
    	if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    	QyshinsuChip.preloadImages(loader,ImageDir);
        images = loader.load_images(ImageDir,ImageFileNames , 
        		loader.load_images(ImageDir, ImageFileNames,"-mask")); // load the main images
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
        use_grid = false;
        gridOption.setState(false);
        colorBlind = Default.getBoolean(Default.colorblind);
        colorBlindOption = myFrame.addOption(s.get(ColorBlindOption), colorBlind,deferredEvents);
        colorBlindOption.setForeground(Color.blue);

        MouseColors[1] = new Color(0.7f,0.2f,0.2f);
        MouseColors[0] = Color.black;
        MouseDotColors[1]= Color.white;
        MouseDotColors[0]= Color.white;

        b = new QyshinsuBoard(info.getString(GameInfo.GAMETYPE, "Qyshinsu"),getStartingColorMap());
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
        b.doInit(b.gametype);						// initialize the board
        if(!preserve_history)
    	{         
        startFirstPlayer();

    	}
    }

    boolean horizontal = false;
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	boolean planned = plannedSeating();
    	int chipW = unitsize*16;
    	int chipH = unitsize*3;
    	int doneW = planned ? unitsize * 4 : 0;
    	Rectangle box = pl.createRectangularPictureGroup(x+(horizontal ? chipW+unitsize : 0),y,unitsize*3/2);
    	G.SetRect(done, G.Right(box)+unitsize/2, y+unitsize/2, doneW,doneW/2);
    	G.union(box,done);
     	G.SetRect(chip, x, horizontal ? G.Top(box)+unitsize : G.Bottom(box)+unitsize, chipW,chipH);
    	pl.displayRotation = rotation;
    	G.union(box,chip);
    	return(box);
    }
    double aspects[] = { -1,1};
    public void setLocalBounds(int x,int y,int w,int h)
    {
    	setLocalBoundsV(x,y,w,h,aspects);
    }
    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
        int chatHeight = selectChatHeight(height);
       	horizontal = aspect<0;
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*15;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        int nrows = 10;
        int ncols = 10;
        
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.5,	// % of space allocated to the board
    			1,	// aspect ratio for the board
    			fh*2,
    			fh*2,	// maximum cell size
    			0.1		// preference for the designated layout, if any
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
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	SQUARESIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*SQUARESIZE);
    	int boardH = (int)(nrows*SQUARESIZE);
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
        int stateY = boardY;
        int stateX = boardX;
        int stateH = fh*5/2;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	lineStrokeWidth = boardW/400.0;
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,chatBackgroundColor);
        return(boardW*boardH);
    }
    
 

	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, int forPlayer, Rectangle r, int player,
        HitPoint highlight)
    {	QyshinsuCell chips[]= b.rack[forPlayer];
        boolean canhit = G.pointInRect(highlight, r) && b.LegalToHitChips(forPlayer) ;
        boolean canDrop = hasMovingObject(highlight);
        int cellW =	G.Width(r)/chips.length;
        int cellH = G.Height(r);
        commonPlayer cp = getPlayerOrTemp(forPlayer);
        for(int col = 0; col<chips.length; col++)
        {	QyshinsuCell thisCell = chips[col];
        	if(thisCell!=null)
        	{	QyshinsuChip thisChip = thisCell.topChip();
        		int left = G.Left(r)+col*cellW;
	    		int cx = left + cellW/2;
           	 	int cy = G.centerY(r);
           	 	cp.rotateCurrentCenter(thisCell,cx,cy);
	       		if(canhit 
	       				&& (canDrop ? b.canDropOn(thisCell) : b.canPickFrom(thisCell))
	       				&& thisCell.closestPointToCell(highlight,cellH,cx,cy)
	       				)
	       			{ 
	       			highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
	        		highlight.awidth = cellH;
	        		highlight.spriteColor = Color.red;
	       			}
                  if((thisChip!=null) && (gc!=null))
                	{boolean more = (thisCell.chipIndex>0); 
                	 boolean active = (thisCell.activeAnimationHeight()>0);
               		 if(more || !active) { thisChip.drawChip(gc,this,cellH,cx,cy,null);}
                	 if(more && !active)
                	 {	thisChip.drawChip(gc, this, cellH,cx+cellW/7,  cy-cellH/5,null);
                	 }
                	}
	       		}
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
    	QyshinsuChip.getChip(obj).drawChip(g,this,SQUARESIZE,xp,yp,null);
     }

    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    public Point spriteDisplayPoint()
	{   return(new Point(G.Left(boardRect)+SQUARESIZE/2,G.Bottom(boardRect)-5*SQUARESIZE/2));
	}



    Image scaled = null;

    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean review = reviewMode() && !mutable_game_record;
      // erase
      QyshinsuBoard gb = disB(gc);
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      scaled = images[BOARD_INDEX].centerScaledImage(gc,  boardRect, scaled);
      setDisplayParameters(gb);
      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.red,Color.blue,Color.red);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, QyshinsuBoard gb, Rectangle brect, HitPoint highlight)
    {
      	//
        // now draw the contents of the board and anything it is pointing at
        //
     	Hashtable<QyshinsuCell,QyshinsuCell> dests = gb.getMoveCells(false);  	
    	Hashtable<QyshinsuCell,QyshinsuCell> sources = gb.getMoveCells(true);
        int who = gb.whoseTurn();
        int next = nextPlayer[who];
        QyshinsuCell hisLastMove = gb.getLastMove(next);
        QyshinsuChip hisLastChip = gb.getLastChip(next);
        QyshinsuChip myLastChip = gb.getLastChip(who);
        QyshinsuCell myLastMove = gb.getLastMove(who);
        int perspective_offset = 0;
        int dotsize = SQUARESIZE/10;
        QyshinsuCell hitCell = null;
        
        //if(gc!=null) { G.DrawAACircle(gc,brect.x=gb.XOFFSET,brect.y+gb.YOFFSET,dotsize,Color.blue,Color.gray,true); }
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left back-front order so the
        // solid parts will fall on top of existing shadows
        for(Enumeration<QyshinsuCell> cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements(); )
        	{ 
            QyshinsuCell cell = cells.nextElement();
            char thiscol = cell.col;
            int row = cell.row;
            int ypos = G.Bottom(brect) - gb.cellToY(thiscol, row);
            int xpos = G.Left(brect) + gb.cellToX(thiscol, row);
            int ypos1 = G.Bottom(brect) - gb.cellToY('B', row);
       	 	int xpos1 = G.Left(brect) + gb.cellToX('B', row);
            boolean isDest = dests.get(cell)!=null;
            boolean isSource = sources.get(cell)!=null;
            {
             int e_x = xpos;
             int e_y = ypos;
             QyshinsuChip cup = cell.topChip();
             cell.rotateCurrentCenter(gc,xpos,ypos);
             if(cup!=null)
                {	cell.drawChip(gc,this,SQUARESIZE,e_x,e_y,null);
                }
             else if((cell==myLastMove) && isDest && !isSource)
             {	int sz = 3*SQUARESIZE/4;
        	 	QyshinsuChip ghost = QyshinsuChip.getChip(QyshinsuChip.GHOST_CHIP_INDEX);
             	myLastChip.drawChip(gc,this,sz,xpos1,ypos1,null);
             	ghost.drawChip(gc,this,sz,xpos1,ypos1,null);
             }
             
             if((gc!=null) && (hisLastMove==cell) )
             {   
            	 if((cup==null) && (hisLastChip!=null))
            	 {
            	 hisLastChip.drawChip(gc,this,SQUARESIZE,xpos1,ypos1,null);
             	 }
            	 else
            	 {	int mx0 = (xpos+xpos1)/2;
            	 	int my0 = (ypos+ypos1)/2;
            	 	GC.setColor(gc,Color.yellow);
            	 	GC.drawArrow(gc,xpos1,ypos1,mx0,my0,SQUARESIZE/6,lineStrokeWidth);
            	 }
             }
             if((isDest || isSource) && (gc!=null))
             {	GC.cacheAACircle(gc,xpos+dotsize,ypos,dotsize,isSource?Color.blue:Color.red,Color.gray,true);
             }
              if((highlight!=null)
            		&& gb.LegalToHitBoard(cell)
              		&& cell.closestPointToCell(highlight, SQUARESIZE,e_x, e_y-perspective_offset)
                    )
              { 
              	hitCell = cell;
                }}
             }
    	if(hitCell!=null)
    	{
    		highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
          	highlight.awidth = SQUARESIZE;
            	highlight.hitCode = QIds.BoardLocation;
        	highlight.spriteColor = Color.red;
             }
  
    }
   //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  QyshinsuBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
       QyshinsuState vstate = gb.getState();
       gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
        
        boolean planned = plannedSeating();
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
          {	commonPlayer pl = getPlayerOrTemp(i);
          	pl.setRotatedContext(gc, highlight,false);
          	DrawCommonChipPool(gc, i,chipRects[i], gb.whoseTurn,ot);
          	if(planned && (i==gb.whoseTurn))
              {
              	handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
      					HighlightColor, rackBackGroundColor);	
              }
              pl.setRotatedContext(gc, highlight,true);
          }

        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

        GC.setFont(gc,standardBoldFont());
        
        drawPlayerStuff(gc,(vstate==QyshinsuState.PUZZLE_STATE),ourSelect,
 	   			HighlightColor, rackBackGroundColor);

		if (vstate != QyshinsuState.PUZZLE_STATE)
        {
			GC.setFont(gc,standardBoldFont());
			if(!planned && !autoDoneActive())
			{
			handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
			}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
         }
 
        standardGameMessage(gc,messageRotation,
        		vstate==QyshinsuState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=QyshinsuState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalDescription),progressRect, goalRect);
         
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
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
        startBoardAnimations(replay,b.animationStack,b.cellSize(),MovementStyle.Simultaneous);
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
        return (new QyshinsuMovespec(st, player));
    }
    

private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_RACK_BOARD:
    case MOVE_REMOVE:
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
        if (hp.hitCode instanceof QIds) // not dragging anything yet, so maybe start
        {

        QIds hitObject = (QIds)hp.hitCode;
		QyshinsuCell cell = hitCell(hp);
		QyshinsuChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case First_Player_Pool:
	    	PerformAndTransmit("Pick B "+cell.row+" "+chip.pieceNumber());
	    	break;
	    case Second_Player_Pool:
	    	PerformAndTransmit("Pick W "+cell.row+" "+chip.pieceNumber());
	    	break;
	    case BoardLocation:
	    	if(cell.chipIndex>=0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.pieceNumber());
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
        if(!(id instanceof QIds)) { missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
    	missedOneClick = false;
    	QIds hitObject = (QIds)hp.hitCode;
        QyshinsuState state = b.getState();
		QyshinsuCell cell = hitCell(hp);
		QyshinsuChip cup = (cell==null) ? null : cell.topChip();
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
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+cup.pieceNumber());
				}
				break;
			}
			break;
			
        case Second_Player_Pool:
        case First_Player_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col = hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
               	case PLAY_STATE:
               		QyshinsuCell sr = b.pickedSource;
               		if(!sr.onBoard)
               		{
            		performReset();
            		break;
               		}	
					//$FALL-THROUGH$
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

    public void setDisplayParameters(QyshinsuBoard gb)
    {
    	gb.SetDisplayParameters(1.0,1.0,  0.15,0.12, 80);
    	gb.SetDisplayRadius(2.3,4.4);
    	gb.SetDisplayRectangle(boardRect);
     }


    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Qyshinsu_SGF); }

    
    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
        b.doInit(token);
    }


    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {  if (target == colorBlindOption)
    	{	
    	colorBlind = colorBlindOption.getState();
    	Default.setBoolean(Default.colorblind,colorBlind);
    	generalRefresh();
    	return(true);
    	}
       return(super.handleDeferredEvent(target,command));
    }


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new QyshinsuPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 9245 files visited 0 problems
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

