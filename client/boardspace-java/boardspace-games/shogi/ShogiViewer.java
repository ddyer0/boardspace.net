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
package shogi;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.Hashtable;
import java.util.StringTokenizer;
import lib.Graphics;
import lib.Image;
import lib.*;
/**
 * 
 * Change History
 *
 * March 2010 Initial work in progress. 
 * 
 *
*/
public class ShogiViewer extends CCanvas<ShogiCell,ShogiBoard> implements ShogiConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private Color ShogiMouseColors[] = {Color.red,Color.black};
    private Color ShogiMouseDotColors[] = { Color.black,Color.white};
	private Color YELLOWDOT = new Color(0.7f,0.7f,0.5f);
	private Color BLUEDOT = new Color(0.4f,0.4f,0.8f);
	private Color GRAYDOT = new Color(0.3f,0.3f,0.3f);
 
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;
    private static StockArt ornaments[] = null;
    // private state
    private ShogiBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //public Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //public Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle chatRect = addRect("chatRect"); // the chat window
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
     private Rectangle chipRects[] = addRect("chip",2);
    private Rectangle rackRects[] =  addRect("rack",2);
    private Rectangle acceptDrawRect = addRect("acceptDraw");
    private Rectangle declineDrawRect = addRect("declineDraw");
    private Toggle eyeRect = new Toggle(this,"eye",
			StockArt.NoEye,ShogiId.ToggleEye,NoeyeExplanation,
			StockArt.Eye,ShogiId.ToggleEye,EyeExplanation
			);

    private Rectangle repRect = addRect("repRect");
    private Rectangle altchipRect = addRect("altChip");
    private Rectangle reverseViewRect = addRect("reverse");
    private boolean traditional_chips = false;
    public int getAltChipset()
    	{ 
    		return((b.reverseY()?2:0)+(traditional_chips?1:0)); 
    	}
    private JCheckBoxMenuItem chipsetOption = null;
    private JCheckBoxMenuItem reverseOption = null;
    
    private JMenuItem drawAction = null;
    public synchronized void preloadImages()
    {	
       	ShogiChip.preloadImages(loader,ImageDir);
        if (ornaments == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
        ornaments = StockArt.preLoadArt(loader,ImageDir,ExtraImageNames,ExtraImageScale);
    	}
        gameIcon = ShogiChip.getChip(8).image;
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
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        enableAutoDone = true;
    	super.init(info,frame);
    	MouseDotColors = ShogiMouseDotColors;
    	MouseColors = ShogiMouseColors;
    	
        b = new ShogiBoard(info.getString(GAMETYPE, Shogi_INIT),randomKey,repeatedPositions);
        useDirectDrawing(true);
        doInit(false);
        chipsetOption = myFrame.addOption(s.get(TraditionalPieces),traditional_chips,deferredEvents);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        drawAction = myFrame.addAction(s.get(OFFERDRAW),deferredEvents);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);						// initialize the board
        if(!preserve_history)
    	{	startFirstPlayer();
    	}
   }

    
    private boolean flatten = false;
    public void setLocalBounds(int x, int y, int width, int height)
    {
    	setLocalBoundsV(x,y,width,height,new double[] {1,-1});
    }

    public double setLocalBoundsA(int x, int y, int width, int height,double a)
    {	flatten = a<0;	
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*14;
    	int vcrW = fh*15;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.70,	// % of space allocated to the board
    			0.8,	// 1:1 aspect ratio for the board
    			fh*2,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
    	layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);
       	layout.placeDoneEdit(buttonW,3*buttonW/2,doneRect,editRect);
       	layout.placeTheVcr(this,vcrW,vcrW*3/2);
       	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
        int stateH = fh*3;
    	int mainH = G.Height(main);
        boolean rotate = seatingFaceToFaceRotated();
        int nrows = rotate ? b.boardColumns : b.boardRows;  
        int ncols = rotate ? b.boardRows : b.boardColumns;

    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH)/nrows);
    	SQUARESIZE = (int)cs;
     	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(nrows*cs);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH-stateH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH+stateH;
    	int boardBottom = boardY+boardH;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH/2;
        int stateX = boardX;

        G.placeRow(stateX,stateY,boardW ,stateH,stateRect,annotationMenu,eyeRect,reverseViewRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	G.placeRow(boardX, boardBottom-stateH,boardW,stateH,goalRect,altchipRect);       
    	if(rotate)
    	{
    		G.setRotation(boardRect, -Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        return boardW*boardH;
    }

    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unit)
    {	commonPlayer pl0 = getPlayerOrTemp(player);
    	int chipW = unit*3;
    	int rackW = unit*8;
    	Rectangle chip = chipRects[player];
    	Rectangle rack = rackRects[player];
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()?unit*6:0;
    	Rectangle box = pl0.createRectangularPictureGroup(x+chipW, y, unit);
    	G.SetRect(chip, x, y, chipW,chipW);
    	int bb = G.Bottom(box);
    	if(flatten)
    	{
    	   G.SetRect(done,G.Right(box)+unit/3,y,doneW,doneW/2);
    	   G.SetRect(rack,G.Right(done),y,rackW*3/2,rackW*2/3); 			   
    	}
    	else {
    	   	G.SetRect(rack, x, bb, rackW,rackW);
        	G.SetRect(done, x+rackW+unit/2, bb+unit/2, doneW, doneW/2);
        		
    	}
    	G.union(box, chip,done,rack);
    	pl0.displayRotation = rotation;
        return(box);
    }

	
    private void DrawPlayerMarker(Graphics gc, int forPlayer, Rectangle r)
    {	ShogiChip king = ShogiChip.getChip(forPlayer,ShogiChip.PieceType.General);
    	king.drawChip(gc,this,r,null);
     }
	
    private void DrawChipsetMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	ShogiChip truking = ShogiChip.getChip(0,ShogiChip.PieceType.General);
    	ShogiChip king = truking.alt_image;
    	king.drawChip(gc,this,r,null);
    	if(HitPoint.setHelpText(highlight,r,s.get(traditional_chips ? SwitchWesternMessage : SwitchTraditionalMessage)))
    	{	highlight.spriteRect = r;
    		highlight.spriteColor = Color.red;
    		highlight.hitCode = ShogiId.ChangeChipsetButton;
    	}
     }
	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	ShogiChip king = ShogiChip.getChip(b.reverseY()?1:0,ShogiChip.PieceType.General);
    	ShogiChip reverse = ShogiChip.getChip(b.reverseY()?0:1,ShogiChip.PieceType.General);
    	int w = 3*G.Width(r)/4;
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	reverse.drawChip(gc,this,w,cx,cy-w/3,null);
    	king.drawChip(gc,this,w,cx,cy+w/3,null);
    	HitPoint.setHelpText(highlight,r, ShogiId.ReverseViewButton,s.get(ReverseViewExplanation));
     }   
	
    private void DrawCommonChipPool(Graphics gc, int player, Rectangle r, int forPlayer, HitPoint highlight)
    {	ShogiCell chips[]= b.rack[player];
    	commonPlayer cp = getPlayerOrTemp(player);
        boolean canHit = b.LegalToHitChips(player);
        int w = G.Width(r);
        int h = G.Height(r);
        boolean wide = w>h;
        int rows = wide ? 2 : 3;
        int cols = wide ? 5 : 3;
        int xs = w/cols;
        int ys = h/rows;
        int x = G.Left(r) + xs/2;
        int y = G.Top(r) + ys*2/3;
        int bishop = ShogiChip.PieceType.Bishop.ordinal();
      
        GC.frameRect(gc,Color.black,r);
        for(ShogiChip.PieceType type : ShogiChip.PieceType.values())
        {
        	if(type.demoted==type)
        	{	int ord = type.ordinal();
                ShogiCell thisCell = chips[ord];
                ShogiChip thisChip = thisCell.topChip();
                boolean canDrop = hasMovingObject(highlight);
                boolean canPick = (thisChip!=null);
                HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
                String msg = null;
                double dx = (type==ShogiChip.PieceType.Pawn) 
                		? Math.min(0.2,0.2-0.1/15*(thisCell.height()-5)) 
                		: 0.0;
                double dy = (dx==0.0) ? 0.15 : 0.0;
                int xp = x+xs*((ord-bishop)%cols);
                int yp = y+ys*((ord-bishop)/cols);
                thisCell.drawUnpromotedStack(gc,this,pt,ys,xp,yp,0,dx,dy,msg);
                if((highlight!=null) && (highlight.hitObject==thisCell))
                {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
                	highlight.awidth =ys/2;
                	highlight.spriteColor = Color.red;
                }
                cp.rotateCurrentCenter(thisCell, xp, yp);
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
    	ShogiChip ch = ShogiChip.getChip(obj);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);
     }


 
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
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { boolean review = reviewMode() && !mutable_game_record;
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      scaled = images[BOARD_INDEX].centerScaledImage(gc, brect, scaled);
	    b.SetDisplayParameters(0.87,1.08,  0.16,-0.02,  0);
	    b.SetDisplayRectangle(brect);

      b.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, ShogiBoard gb, Rectangle brect, HitPoint highlight)
    {
     	//
        // now draw the contents of the board and anything it is pointing at
        //
    	ShogiState state = gb.getState();
     	boolean moving = hasMovingObject(highlight);
     	boolean show = eyeRect.isOnNow();
     	Hashtable<ShogiCell,ShogiCell> dests = gb.getDests();
       	drawArrow(gc,gb,YELLOWDOT,gb.pickedSource,gb.droppedDest,brect);
       	drawArrow(gc,gb,YELLOWDOT,gb.prevPickedSource,gb.prevDroppedDest,brect);

        // drawing order doesn't matter for these graphics
    	for(ShogiCell cell = gb.allCells;
    	    cell!=null;
    	    cell =cell.next)
    	{	
            boolean canhit = gb.LegalToHitBoard(cell);
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
            if( cell.drawStack(gc,this,canhit?highlight:null,SQUARESIZE,xpos,ypos,0,0.1,null)) 
            	{ 
          		highlight.arrow = moving 
              			? StockArt.DownArrow 
              			: cell.height()>0?StockArt.UpArrow:null;
              		highlight.awidth = SQUARESIZE/2;
              		highlight.spriteColor = Color.red;
            	}
            if(moving)
            {
           	int scale = Math.max(2,(int)(0.1 * SQUARESIZE));
             if(dests.get(cell)!=null) 
            	{ GC.cacheAACircle(gc,xpos,ypos-scale/2,scale,BLUEDOT,GRAYDOT,true);
            	}
            if(gb.isSource(cell)) 
            	{ 
            	GC.cacheAACircle(gc,xpos,ypos-scale/2,scale,YELLOWDOT,GRAYDOT,true);
            	}
            }
            else
                if(show && canhit)
                {
                	StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
                }

            
            if(!(gb.originalDroppedObject!=null)
            	&& (cell.topChip()!=null)
            	&& (state==ShogiState.Puzzle)?(gb.prevDroppedDest==cell):(gb.droppedDest==cell)
        		&& (gb.canPromote(gb.pickedSource,cell, gb.originalDroppedObject,gb.whoseTurn)
            	&& !gb.mustPromote(cell,gb.originalDroppedObject,gb.whoseTurn)
            	))
            {
            	ShogiChip top = cell.topChip();
	            		if(top!=null)
	            		{
	            		ShogiChip alt = top.getFlipped();
	            			if(top!=alt)
	            			{	int index = FLIPPER_INDEX;
	            				if(G.pointInRect(highlight, xpos-SQUARESIZE/2,ypos,SQUARESIZE,SQUARESIZE/2))
	            				{	index = DARK_FLIPPER_INDEX;
	            					highlight.hitObject = cell;
	            					highlight.hitCode = ShogiId.FlipButton;
	            				}
	            				ornaments[index].drawChip(gc,this,(int)(SQUARESIZE*1),xpos,ypos+SQUARESIZE/4,null);
	            			}
	            		}
	            	}

    	}
    }
    private void drawArrow(Graphics gc,ShogiBoard gb,Color color,ShogiCell from,ShogiCell to,Rectangle brect)
    {
        if((gc!=null) && (from!=null) && (to!=null))
        {
        if(from.onBoard)
        	{int fx = G.Left(brect)+gb.cellToX(from.col,from.row);
        	 int fy = G.Bottom(brect)-gb.cellToY(from.col,from.row);
             ornaments[SQUARE_INDEX].drawChip(gc,this,(int)(SQUARESIZE*0.7),fx,fy,null);
       	}
        if(to.onBoard)
        {
            int tx = G.Left(brect)+gb.cellToX(to.col,to.row);
            int ty = G.Bottom(brect)-gb.cellToY(to.col,to.row);
            ornaments[SQUARE_INDEX].drawChip(gc,this,(int)(SQUARESIZE*1.2),tx,ty,null);       	
        }
         //gc.setColor(color);
        //G.DrawArrow(gc, fx, fy, tx, ty, SQUARESIZE / 6);
        }
	
    }
   //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  ShogiBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
       ShogiState vstate = gb.getState();
       gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
       	GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
        drawBoardElements(gc, gb, boardRect, ot);
        GC.unsetRotatedContext(gc,highlight);
        
        boolean planned = plannedSeating();
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
          {	commonPlayer pl = getPlayerOrTemp(i);
          	pl.setRotatedContext(gc, highlight, false);
            DrawCommonChipPool(gc, i,rackRects[i], gb.whoseTurn,ot);
            DrawPlayerMarker(gc,(pl.displayRotation==0)?i:nextPlayer[i],chipRects[i]);            
          	if(planned && (i==gb.whoseTurn))
          	{
          		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
      					HighlightColor, rackBackGroundColor);
          	}
          	pl.setRotatedContext(gc, highlight, true);
          }	
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);


        DrawChipsetMarker(gc,altchipRect,highlight);
        DrawReverseMarker(gc,reverseViewRect,highlight);

        GC.setFont(gc,standardBoldFont());
        double standardRotation = pl.messageRotation();
		if (vstate != ShogiState.Puzzle)
        {
			if((vstate == ShogiState.QueryDraw)
						|| (vstate==ShogiState.AcceptDraw)
						|| (vstate==ShogiState.DeclineDraw))
			{
			if(GC.handleRoundButton(gc,standardRotation,acceptDrawRect,select,s.get(ACCEPTDRAW),
					HighlightColor,rackBackGroundColor))
			{ select.hitCode = GameId.HitAcceptDrawButton;
			}
			if(GC.handleRoundButton(gc,standardRotation,declineDrawRect,select,s.get(DECLINEDRAW),
					HighlightColor,rackBackGroundColor))
			{ select.hitCode = GameId.HitDeclineDrawButton;
			}
			}
			else if(vstate==ShogiState.OfferDraw)
			{	if(GC.handleRoundButton(gc,acceptDrawRect,select,s.get(OFFERDRAW),
					HighlightColor,(ShogiState.OfferDraw==vstate)?HighlightColor:rackBackGroundColor))
					{ select.hitCode = GameId.HitOfferDrawButton;
					}
			}
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,standardRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
			
        }

 		drawPlayerStuff(gc,(vstate==ShogiState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);
 		eyeRect.activateOnMouse = true;
 		eyeRect.draw(gc,select);
  		standardGameMessage(gc,standardRotation,
        		vstate==ShogiState.Gameover?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=ShogiState.Puzzle,
        				gb.whoseTurn,
        				stateRect);
            goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);

            DrawRepRect(gc,standardRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca
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
        return (new ShogiMovespec(st, player));
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
      	 playASoundClip(heavy_drop,100);
      	break;
    case MOVE_DONE:
    	if(b.getState()==ShogiState.Check)
    	{
    		playASoundClip(light_drop,60);
    		playASoundClip(light_drop,60);
    		playASoundClip(light_drop,60);
    		playASoundClip(light_drop,60);
    	}
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
        if (hp.hitCode instanceof ShogiId)// not dragging anything yet, so maybe start
        {
        ShogiId hitObject = (ShogiId)hp.hitCode;
		ShogiCell cell = hitCell(hp);
		ShogiChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
 	    case Up_Chip_Pool:
	    	PerformAndTransmit("Pick U "+cell.row+" "+chip.chipNumber());
	    	break;
	    case Down_Chip_Pool:
	    	PerformAndTransmit("Pick D "+cell.row+" "+chip.chipNumber());
	    	break;
	    case BoardLocation:
	    	if(cell.chipIndex>=0)
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
        if(!(id instanceof ShogiId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
   		missedOneClick = false;
   		ShogiId hitObject = (ShogiId)hp.hitCode;
        ShogiState state = b.getState();
		ShogiCell cell = hitCell(hp);
		ShogiChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
          case FlipButton:
        	  PerformAndTransmit("Flip "+cell.col+" "+cell.row);
        	  break;
          case ToggleEye:
        	  eyeRect.toggle();
        	  break;
         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Confirm:
	        case IllegalMove:
			case Play:
			case Check:
			case Puzzle:
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
         case ReverseViewButton:
        	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
        	 generalRefresh();
        	 break;
         case ChangeChipsetButton:
         	chipsetOption.setState(traditional_chips = !traditional_chips);
         	break;
			
        case Down_Chip_Pool:
        case Up_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
            	case Check:
               	case Play:
            		PerformAndTransmit(RESET);
            		break;
               	case Puzzle:
            		PerformAndTransmit("Drop "+col+" "+cell.row+" "+mov);
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
    public String sgfGameType() { return(Shogi_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link #gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a init spec
    	long rk = G.LongToken(his);
    	// make the random key part of the standard initialization,
        b.doInit(token,rk);
    }


    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {	if(target==reverseOption)
    	{
    	b.setReverseY(reverseOption.getState());
    	generalRefresh();
    	return(true);
    	}
    	else if(target==chipsetOption)
    	{
    	traditional_chips = chipsetOption.getState();
    	repaint(20);
    	return(true);
    	}
    	else if (target==drawAction)
    	{
    		if(OurMove()) 
    			{ 
        		if(b.canOfferDraw())
    			{
    			PerformAndTransmit(OFFERDRAW);
    			}
        		else { G.infoBox(null,s.get(DrawNotAllowed)); }
    			}
    		else {
                theChat.postMessage(ChatInterface.GAMECHANNEL, KEYWORD_CHAT,
                    s.get(CantDraw));
            }
    		return(true);
    	}
      	return(super.handleDeferredEvent(target,command));
     }

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new ShogiPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
	 * 337 files visited 0 problems
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
}

