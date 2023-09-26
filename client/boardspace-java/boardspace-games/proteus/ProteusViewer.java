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
package proteus;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Image;
import lib.Text;
import lib.TextChunk;

import static proteus.ProteusMovespec.*;

/**
 * This code shows the overall structure appropriate for a game view window.
 * todo: rotate board for ftf portrait mode
*/
public class ProteusViewer extends CCanvas<ProteusCell,ProteusBoard> implements ProteusConstants, GameLayoutClient
{
    static final String Proteus_SGF = "Proteus"; // sgf game name
    static final String ImageDir = "/proteus/images/";
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(120,120,120);
    
	private Color canonicalYellow = new Color(170,140,62);
	private Color canonicalBlue = new Color(95,138,155);
	private Color canonicalRed = new Color(179,55,87);
	
    // private state
    private ProteusBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //public Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //public Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle mainChipRect = addRect("mainChipRect");
    private Rectangle whiteChipRect = addRect(".whiteChipRect");
    private Rectangle blackChipRect = addRect(".blackChipRect");
    private Rectangle chipRects[] = addRect("id",2);

   private JCheckBoxMenuItem reverseOption = null;
   
   private Rectangle movementRect = addRect("movement");
   private Rectangle tradeRect = addRect("trade");
   private Rectangle repRect = addRect("repRect");

    public synchronized void preloadImages()
    {	
       	ProteusChip.preloadImages(loader,ImageDir);
       	gameIcon = ProteusChip.Icon.image;
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
    	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
    	enableAutoDone = true;
    	super.init(info,frame);
    	
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	ProteusConstants.putStrings();
         }
        MouseColors = new Color[] { Color.black,Color.white };
        MouseDotColors = new Color[] { Color.white,Color.black };
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new ProteusBoard(info.getString(OnlineConstants.GAMETYPE, Variation.Proteus.name()),
        		randomKey,players_in_game,getStartingColorMap(),ProteusBoard.REVISION);
        useDirectDrawing(true); 
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);

        
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

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int doneW = unitsize*4;
    	int chipW = unitsize*3;
    	Rectangle box = pl.createRectangularPictureGroup(x+doneW,y,unitsize);
    	Rectangle done = doneRects[player];
    	Rectangle chip = chipRects[player];
    	G.SetRect(chip,x,y,chipW,chipW);
    	G.SetRect(done, x,y+chipW+unitsize/2,doneW,plannedSeating()?doneW/2:0);
    	
    	pl.displayRotation = rotation;
    	
    	G.union(box, done,chip);
    	return(box);
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
    	int vcrW = fh*16;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        int ncols = 4;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.5,	// aspect ratio for the board
    			fh*2.5,	// maximum cell size
    			0.2		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
    	layout.placeTheVcr(this,vcrW,vcrW*3/2);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/(ncols+2),(double)mainH/ncols);
    	SQUARESIZE = (int)(cs*1.25);
    	int CELLSIZE = SQUARESIZE/4;
    	int C2 = CELLSIZE/2;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(ncols*cs);
    	int extraW = Math.max(0, (mainW-boardW-SQUARESIZE)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	int boardRight = boardX+boardW;
       	layout.returnFromMain(extraW,extraH);
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateH = 2*CELLSIZE/3;
        int stateY = boardY;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
        setProgressRect(progressRect,goalRect);
        
        
        G.SetRect(whiteChipRect,
        		boardX+(int)(boardW*0.52),
        		boardBottom-(int)(SQUARESIZE*0.65),
        		(int)(boardW*0.35),
        		SQUARESIZE/2);
         G.AlignXY(blackChipRect,
         		boardX+(int)(SQUARESIZE*0.5),
         		boardY+(int)(SQUARESIZE*0.2),
         		whiteChipRect);
        
        G.SetRect(mainChipRect, boardRight-SQUARESIZE/2+CELLSIZE,boardY+SQUARESIZE/2, SQUARESIZE, SQUARESIZE*2);
        
 
        G.SetRect(goalRect,boardX+CELLSIZE, boardBottom-CELLSIZE,boardW-CELLSIZE*2, C2);
     
        setProgressRect(progressRect,goalRect);

        G.SetRect(tradeRect,G.Left( goalRect), G.Bottom(goalRect),G.Width( goalRect)/2,G.Height( goalRect));

        G.AlignXY(movementRect, G.Right(tradeRect), G.Bottom(goalRect),tradeRect);


        positionTheChat(chatRect,Color.white,Color.white);
 	
    }
 

	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void drawCommonChipPool(Graphics gc, ProteusBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight,HitPoint any)
    {	ProteusCell cells[] = gb.originalTiles;
		int yspace = G.Height(r)/5;
		int xspace = G.Width(r)/2;
		int x = xspace/2;
		int y = yspace/2;
		ProteusCell src = gb.getSource();
		ProteusCell hitCell = null;
		commonPlayer pl = getPlayerOrTemp(forPlayer);
		Hashtable<ProteusCell,ProteusMovespec>targets = gb.getTargets(repeatedPositions);
		for(int i=0;i<cells.length;i++)
		{	ProteusCell c = cells[i];
			boolean canHit = gb.legalToHitTiles(c,targets); 
	   		ProteusChip top = c.topChip();
	   		String msg = top==null ? null : s.get(top.getDesc());
	   		int cx = G.Left(r)+x;
	   		int cy = G.Top(r)+y;
			if(c.drawStack(gc,this,canHit?highlight:null,SQUARESIZE,cx,cy,0,1.0,null))
			{	hitCell = c;
			}
			if(msg!=null) { HitPoint.setHelpText(any,SQUARESIZE/2,cx,cy,msg); }
			if(c==src) { StockArt.SmallO.drawChip(gc,this,SQUARESIZE/4,cx,cy,null);}
			if(msg!=null)
			{	TextChunk.create(msg).draw(gc, pl.displayRotation, true, 
					new Rectangle(cx-SQUARESIZE/2,cy-SQUARESIZE/2,SQUARESIZE,SQUARESIZE),
					Color.black,null);
			}
			y+= yspace;
			if(y>G.Height(r)) { x += xspace; y = G.Height(r)/5; }
			
			
		}
		if(hitCell!=null)
		{
 	       highlight.arrow = (gb.pickedObject!=null) ? StockArt.DownArrow : StockArt.UpArrow;
 	       highlight.awidth = xspace/2;
 	       highlight.spriteColor = Color.red;
		}
    }
    private void drawPlayerId(Graphics gc,Rectangle id,int forPlayer)
    {
       	ProteusChip idChip = ProteusChip.PlayerChips[b.getColorMap()[forPlayer]][0];
       	int w = G.Width(id);
    	idChip.drawChip(gc,this,w*3,G.centerX(id)+w/6,G.centerY(id)+w/6,null);
    }
    
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void drawPlayerPieces(Graphics gc, ProteusBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	ProteusCell cells[] = gb.playerChips[forPlayer];
    	Hashtable<ProteusCell,ProteusMovespec>targets = gb.getTargets(repeatedPositions);
    	int space = G.Width(r)/cells.length;
    	int x = space/2;
    	ProteusCell src = gb.getSource();
    	int y = G.Height(r)/2;
    	ProteusCell hitCell = null;
    	for(int i=0;i<cells.length;i++)
    	{	ProteusCell c = cells[i];
    		boolean canHit = gb.legalToHitChips(player,c,targets);
    		int cx = G.Left(r)+x;
    		int cy = G.Top(r)+y;
    		// the ordinary graphics for the player chips are set to work for the main board.
    		// the player pieces are in a row with smaller cell size, but we want them to display
    		// at the same size as on the main board.  This adjustment compensates the mouse sensitivity
    		// for the the oversized display
    		// 
    		c.cellWidth = (double)space/SQUARESIZE;	// adjust the sensitive size
    		if(c.drawStack(gc,this,canHit?highlight:null,SQUARESIZE,cx,cy,0,1.0,null))
    		{
    			hitCell = c;
    		}
			if(c==src) { StockArt.SmallO.drawChip(gc,this,SQUARESIZE/4,cx,cy,null);}

    		x += space;
    	}
    	if(hitCell!=null)
    	{
 	       highlight.arrow = (gb.pickedObject!=null) ? StockArt.DownArrow : StockArt.UpArrow;
 	       highlight.awidth = space/2;
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
    	ProteusChip ch = ProteusChip.getChipNumber(obj);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);
     }

    Image scaled = null;
    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	boolean reviewBackground = reviewMode()&&!mutable_game_record;
      // erase
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
  
     ProteusChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       ProteusChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
      
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      scaled = ProteusChip.board.image.centerScaledImage(gc, boardRect,scaled);

	  b.SetDisplayParameters(0.8,0.89,  0.025,0.05, 0);
	  b.SetDisplayRectangle(boardRect);
      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, ProteusBoard gb, Rectangle brect, HitPoint highlight,HitPoint any)
    {	Hashtable<ProteusCell,ProteusMovespec>targets = gb.getTargets(repeatedPositions);
    	Hashtable<ProteusCell,ProteusMovespec>pieceTargets = gb.getPieceTargets(repeatedPositions);
    	boolean moving = hasMovingObject(any);
    	ProteusCell src = gb.getSource();
	    gb.SetDisplayParameters(0.8,0.89,  0.025,0.05, 0);
	    gb.SetDisplayRectangle(boardRect);
	    ProteusCell hitCell = null;
	    Enumeration<ProteusCell>cells = gb.getIterator(Itype.LRTB);
     	while(cells.hasMoreElements())
       	{	
            ProteusCell cell = cells.nextElement();
            int ypos = G.Bottom(brect) - gb.cellToY(cell);
            int xpos = G.Left(brect) + gb.cellToX(cell);
            HitPoint hitNow = gb.legalToHitBoard(cell,targets) ? highlight : null;
            ProteusChip top = (cell.height()>=1) ? cell.chipAtIndex(0) : null;
            String msg = (top==null)? null : s.get(top.getDesc());
            String liveMsg = gb.shapeIsActive(top) ? msg : null;
            if( cell.drawStack(gc,this,hitNow,SQUARESIZE,xpos,ypos,0,0.0,null)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	hitCell = cell;
            	}
            if(cell==src) { StockArt.SmallO.drawChip(gc,this,SQUARESIZE/6,xpos,ypos,null); }
            if(liveMsg!=null)
            {	ypos+=SQUARESIZE/5;
                for(int x=xpos-3;x<=xpos+3;x++)
            	{for(int y=ypos-3;y<=ypos+3;y++)
            	{
            		GC.Text(gc,true,x-SQUARESIZE/2,y-SQUARESIZE/2,SQUARESIZE,SQUARESIZE,Color.black,null,liveMsg);
            	}}
            	
            	GC.Text(gc,true,xpos-SQUARESIZE/2,ypos-SQUARESIZE/2,SQUARESIZE,SQUARESIZE,Color.yellow,null,liveMsg);
            }
            if(msg!=null) { HitPoint.setHelpText(any,SQUARESIZE,xpos,ypos,msg); }
            //StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
        	
    	}
    	if(hitCell!=null)
    	{
    		highlight.arrow = moving 
  				? StockArt.DownArrow 
  				: hitCell.topChip()!=null?StockArt.UpArrow:null;
        	highlight.awidth = SQUARESIZE/2;
        	highlight.spriteColor = Color.red;
            boolean stuffed = (hitCell.height()==2);
        	if(stuffed 
        			&& (pieceTargets.get(hitCell)!=null)
        			&& G.pointNearCenter(highlight,highlight.hit_x,highlight.hit_y,SQUARESIZE/6,SQUARESIZE/6))
        	{	highlight.hitCode = ProteusId.BoardLocation;
        		highlight.awidth = SQUARESIZE/4;
        		highlight.spriteColor = Color.blue;
        	}
        	else { highlight.hitCode = ProteusId.BoardTile;}
        	
    	}
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  
    }
    
     Text[] colorText = {
    	TextChunk.create("Win:",canonicalYellow),
       	TextChunk.create("goal",canonicalYellow),
      	TextChunk.create("Swap Tiles:",canonicalBlue),
      	TextChunk.create("swap rule",canonicalBlue),
    	TextChunk.create("Move Pieces:",canonicalRed),
    	TextChunk.create("piece movement",canonicalRed),
    
    };
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  ProteusBoard gb = disB(gc);
       boolean ourTurn = OurMove();
       int whoseTurn = gb.whoseTurn;
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      ProteusState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot,highlight);
        
        boolean planned = plannedSeating();
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
          {	commonPlayer pl = getPlayerOrTemp(i);
          	pl.setRotatedContext(gc, highlight, false);
          	drawPlayerId(gc,chipRects[i],i);
          	if(planned && (i==whoseTurn))
          	{
          		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
      					HighlightColor, rackBackGroundColor);
          	}
          	pl.setRotatedContext(gc, highlight, true);
          }	
        commonPlayer pl = getPlayerOrTemp(whoseTurn);
        double messageRotation = pl.messageRotation();
        
        
        drawCommonChipPool(gc, gb,FIRST_PLAYER_INDEX,mainChipRect, whoseTurn,ot,highlight);
        drawPlayerPieces(gc,gb,FIRST_PLAYER_INDEX,blackChipRect,whoseTurn,ot);
        drawPlayerPieces(gc,gb,SECOND_PLAYER_INDEX,whiteChipRect,whoseTurn,ot);
        GC.setFont(gc,standardBoldFont());
		if (vstate != ProteusState.Puzzle)
        {
			if(!planned && !autoDoneActive())
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==ProteusState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);


            standardGameMessage(gc,messageRotation,
            		vstate==ProteusState.Gameover
            			?gameOverMessage()
            			:s.get(vstate.getDescription()),
            				vstate!=ProteusState.Puzzle,
            				whoseTurn,
            				stateRect);
            drawPlayerId(gc,iconRect,whoseTurn);
            GC.setFont(gc,largeBoldFont());
            goalAndProgressMessage(gc,ourSelect,Color.black,"",progressRect, goalRect);
            GC.setFont(gc,largeBoldFont());
            TextChunk.colorize(G.replace(s.get(gb.trade.desc),"\n",": "),s,colorText).draw(gc,messageRotation,true,tradeRect,Color.black, null);
            TextChunk.colorize(G.replace(s.get(gb.move.desc),"\n",": "),s,colorText).draw(gc,messageRotation,true,movementRect,Color.black, null);
            TextChunk.colorize(G.replace(s.get(gb.goal.desc),"\n",": "),s,colorText).draw(gc,messageRotation,true,goalRect,Color.black, null);
         
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);
        drawAuxControls(gc,ourSelect);
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
        return (new ProteusMovespec(st, player));
    }
    


    /**
     * prepare to add nmove to the history list, but also edit the history
     * to remove redundant elements, so that indecisiveness by the user doesn't
     * result in a messy game log.  
     * 
     * For all ordinary cases, this is now handled by the standard implementation
     * in commonCanvas, which uses the board's Digest() method to distinguish new
     * states and reversions to past states.
     * 
     * For reference, the commented out method below does the same thing for "Hex". 
     * You could resort to similar techniques to replace or augment what super.EditHistory
     * does, but your efforts would probably be better spent improving your Digest() method
     * so the commonCanvas method gives the desired result.
     * 
     * Note that it should always be correct to simply return nmove and accept the messy
     * game record.
     * 
     * This may require that move be merged with an existing history move
     * and discarded.  Return null if nothing should be added to the history
     * One should be very cautious about this, only to remove real pairs that
     * result in a null move.  It is vital that the operations performed on
     * the history are identical in effect to the manipulations of the board
     * state performed by "nmove".  This is checked by verifyGameRecord().
     * 
     * in commonEditHistory()
     * 
     */

//    public commonMove EditHistory(commonMove nmove)
//    {
//    	ProteusMovespec newmove = (ProteusMovespec) nmove;
//    	ProteusMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            ProteusMovespec m = (ProteusMovespec) History.elementAt(idx);
//                if(m.next!=null) { idx = -1; }
//                else 
//               {
//                switch (newmove.op)
//                {
//                case MOVE_RESET:
//                	rval = null;	// reset never appears in the record
//                 case MOVE_RESIGN:
//                	// resign unwind any preliminary motions
//                	switch(m.op)
//                	{
//                  	default:	
//                 		if(state==PUZZLE_STATE) { idx = -1; break; }
//                 	case MOVE_PICK:
//                 	case MOVE_PICKB:
//               		UndoHistoryElement(idx);	// undo back to last done
//                		idx--;
//                		break;
//                	case MOVE_DONE:
//                	case MOVE_START:
//                	case MOVE_EDIT:
//                		idx = -1;	// stop the scan
//                	}
//                	break;
//                	
//             case MOVE_DONE:
//             default:
//            		idx = -1;
//            		break;
//               case MOVE_DROPB:
//                	if(m.op==MOVE_PICKB)
//                	{	if((newmove.to_col==m.from_col)
//                			&&(newmove.to_row==m.from_row))
//                		{ UndoHistoryElement(idx);	// pick/drop back to the same spot
//                		  idx--;
//                		  rval=null;
//                		}
//                	else if(idx>0)
//                	{ ProteusMovespec m2 = (ProteusMovespec)History.elementAt(idx-1);
//                	  if((m2.op==MOVE_DROPB)
//                			  && (m2.to_col==m.from_col)
//                			  && (m2.to_row==m.from_row))
//                	  {	// sequence is pick/drop/pick/drop, edit out the middle pick/drop
//                		UndoHistoryElement(idx);
//                	  	UndoHistoryElement(idx-1);
//                	  	idx = idx-2;
//                	  }
//                	  else { idx = -1; }
//                		
//                	}
//                	else { idx = -1; }
//                	}
//                	else { idx = -1; }
//                	break;
//                	
//            	}
//               }
//            G.Assert(idx!=start_idx,"progress editing history");
//            }
//         return (rval);
//    }
//
 
private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_FROM_TO:
    case MOVE_TRADE:
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
        if (hp.hitCode instanceof ProteusId) // not dragging anything yet, so maybe start
        {
        ProteusId hitObject = (ProteusId)hp.hitCode;
		ProteusCell cell = hitCell(hp);
		ProteusChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case MainChips:
	    case BlackChips:
	    case WhiteChips:
	    	PerformAndTransmit("Pick "+hitObject.shortName+" "+cell.row);
	    	break;
	    case BoardTile:
	    	PerformAndTransmit("Pickt "+cell.col+" "+cell.row);
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
    {	CellId id = hp.hitCode;
    	if(!(id instanceof ProteusId)) 
    		{ // handle all the actions that aren't ours
    			missedOneClick = performStandardActions(hp,missedOneClick); 
    		}
    	else {
    	missedOneClick = false;
        ProteusId hitObject = (ProteusId)id;
		ProteusState state = b.getState();
		ProteusCell cell = hitCell(hp);
		ProteusChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case BoardTile:
        case BoardLocation:	// we hit the board 
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( (hitObject==ProteusId.BoardTile?"Pickt " : "Pickb ")+cell.col+" "+cell.row);
				}
			break;
        case MainChips:
        case WhiteChips:
        case BlackChips:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                case Placement:
            		PerformAndTransmit(RESET);
            		break;

               	case Puzzle:
            		PerformAndTransmit("Drop "+col+" "+cell.row);
            		break;
            	}
			}
         	}
            break;

        }}
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
    	return(b.gameType()); 
    }
    public String sgfGameType() { return(Proteus_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a proteus init spec
    	int np = G.IntToken(his);
    	long rv = G.LongToken(his);
    	int rev = G.IntToken(his);
    	// make the random key part of the standard initialization,
    	// even though games like proteus probably don't use it.
        b.doInit(token,rv,np,rev);
        adjustPlayers(np);

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
    	b.setReverseY(reverseOption.getState());
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
    public SimpleRobotProtocol newRobotPlayer() { return(new ProteusPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
		846 files visited 0 problems
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
                adjustPlayers(b.nPlayers());
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

