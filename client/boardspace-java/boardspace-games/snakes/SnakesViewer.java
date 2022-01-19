package snakes;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.awt.*;
import java.util.*;

import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.PopupManager;
import lib.StockArt;


/**
 * 
 * Change History
 *
 * May 2007 initial work in progress. 
 *
 * This code is derived from the "HexGameViewer" class.  Refer to the
 * documentation there for overall structure notes.
*/
public class SnakesViewer extends CCanvas<SnakesCell,SnakesBoard> implements SnakesConstants
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] textures = null;// background textures
    
    // private state
    private SnakesBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    private static int SUBCELL = 4;	// number of cells in a square
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle rackRect = addRect("rackRect");
    private Rectangle numberRect = addRect("numberRect");
    private Rectangle patternRect = addRect("patternRect");
    private Rectangle saveGivensRect = addRect("saveGivensRect");

    public void preloadImages()
    {	
       	SnakesChip.preloadImages(loader,ImageDir);
        if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
    	}
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
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new SnakesBoard(info.getString(OnlineConstants.GAMETYPE, Snakes_INIT),randomKey);
        doInit(false);

        
     }

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

 

	/**
	 * this is the main method to do layout of the board and other widgets.  I don't
	 * use swing or any other standard widget kit, or any of the standard layout managers.
	 * they just don't have the flexibility to produce the results I want.  Your mileage
	 * may vary, and of course you're free to use whatever layout and drawing methods you
	 * want to.  However, I do strongly encourage making a UI that is resizable within
	 * reasonable limits, and which has the main "board" object at the left.
	 * 
	 *  The basic layout technique used here is to start with a cell which is about the size
	 *  of a board square, and lay out all the other object relative to the board or to one
	 *  another.  The rectangles don't all have to be on grid points, and don't have to
	 *  be non-overlapping, just so long as the result generally looks good.
	 *  
	 *  When "extraactions" is available, a menu option "show rectangles" works
	 *  with the "addRect" mechanism to help visualize the layout.
	 */ 
    public void setLocalBounds(int x, int y, int width, int height)
    {   int separation=2;
        int sncols = (b.boardColumns*SUBCELL+32); // more cells wide to allow for the aux displays
        int snrows = (b.boardRows+1)*SUBCELL;  
        int cellw = width / sncols;
        int chatHeight = selectChatHeight(height);
        int cellh = (height-chatHeight) / snrows;
        CELLSIZE = Math.max(1,Math.min(cellw, cellh)); //cell size appropriate for the aspect ration of the canvas
        int ideal_logwidth = CELLSIZE * 12;
        SQUARESIZE = CELLSIZE*SUBCELL;
        fullRect.x = 0;			// the whole canvas
        fullRect.y = 0;
        fullRect.width = width;
        fullRect.height = height;

        // game log.  This is generally off to the right, and it's ok if it's not
        // completely visible in all configurations.
        
        boardRect.x = 0;
        boardRect.y = chatHeight+SQUARESIZE-CELLSIZE;
        boardRect.width = SQUARESIZE * b.boardColumns ;
        boardRect.height = SQUARESIZE * b.boardRows;

        int stateX = boardRect.x + CELLSIZE;
        int stateY = chatHeight+CELLSIZE/3;
        int stateH = CELLSIZE;
        G.SetRect(noChatRect, G.Right(boardRect)-stateH,stateY,stateH,stateH);
        
        stateRect.x = stateX;
        stateRect.y = stateY;
        stateRect.width = G.Left(noChatRect)-stateX;
        stateRect.height = stateH ;


        chatRect.x = fullRect.x;
        chatRect.y = fullRect.y;
        chatRect.width = Math.max(boardRect.width,fullRect.width-ideal_logwidth-CELLSIZE);
        chatRect.height = chatHeight;

        logRect.x = chatRect.x + chatRect.width+CELLSIZE/3 ;
        logRect.y = chatRect.y ;
        logRect.width = Math.min(ideal_logwidth,fullRect.width-logRect.x);
        logRect.height = chatRect.height;

        
        // "edit" rectangle, available in reviewers to switch to puzzle mode
        editRect.x = G.Right(boardRect)+CELLSIZE*separation;
        editRect.y = chatHeight+CELLSIZE*2;
        editRect.width = CELLSIZE*6;
        editRect.height = 2*CELLSIZE;

        
        rackRect.x = G.Right(boardRect)+CELLSIZE;;
        rackRect.y = G.Bottom(editRect);
        rackRect.width = CELLSIZE*32;
        rackRect.height = CELLSIZE*25;

        goalRect.x = boardRect.x;		// really just a general message
        goalRect.y = G.Bottom(boardRect)-CELLSIZE;
        goalRect.height = CELLSIZE*2;
        goalRect.width = boardRect.width;
        
        progressRect.x = goalRect.x+goalRect.width/6;	// a simple progress bar when the robot is running.
        progressRect.width = goalRect.width/2;
        progressRect.y = goalRect.y;
        progressRect.height = CELLSIZE/2;



                  
        
        // "done" rectangle, should always be visible, but only active when a move is complete.
        doneRect.x = editRect.x;
        doneRect.y = G.Bottom(rackRect)+CELLSIZE*2;
        doneRect.width = editRect.width;
        doneRect.height = editRect.height;
 
        //this sets up the "vcr cluster" of forward and back controls.
        SetupVcrRects(rackRect.x,G.Bottom(rackRect)+CELLSIZE,
            CELLSIZE * 14,
            7 * CELLSIZE);
 
        numberRect.x = rackRect.x;
        numberRect.y = G.Bottom(rackRect)+CELLSIZE*8;
        numberRect.height = CELLSIZE*2;
        numberRect.width = SQUARESIZE;
        
        saveGivensRect.x = G.Right(numberRect)+CELLSIZE/2;
        saveGivensRect.y = numberRect.y;
        saveGivensRect.width = CELLSIZE*8;
        saveGivensRect.height = numberRect.height;
        
        patternRect.x = rackRect.x;
        patternRect.y = G.Bottom(numberRect)+CELLSIZE;
        patternRect.width = SQUARESIZE;
        patternRect.height = CELLSIZE*2;
        
        positionTheChat(chatRect,Color.white,Color.white);
        generalRefresh();
    }
    
  
    private void drawRackRect(Graphics gc,HitPoint highlight)
    {	int ncols = 8;
    	int rowstep = rackRect.height/3;
    	int row = 2*rowstep/3;
    	int colstep = rackRect.width/(ncols+1);
    	int col = colstep;
    	GC.frameRect(gc,Color.black,rackRect);
    	SnakesCell rack[] = b.rack;
    	for(int idx = 0;idx<rack.length;idx++)
    	{	SnakesCell c = rack[idx];
    		int xpos = rackRect.x+col;
    		int ypos = rackRect.y+row;
    		StockArt.SmallO.drawChip(gc,this,CELLSIZE*2,xpos,ypos,null);
    		boolean canhit = (highlight!=null) && b.LegalToHitChips(c);
    		if(c.drawChip(gc,this,c.topChip(),canhit?highlight:null,CELLSIZE*4,xpos,ypos,"x"))
    		{
    			// draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
    			highlight.arrow =(b.pickedObject!=null) 
    		      			? StockArt.DownArrow 
    		      			: c.height()>0?StockArt.UpArrow:null;
    			highlight.awidth = CELLSIZE*2;
    			highlight.spriteColor = Color.red;
     		}
    		col += colstep*((idx>=8)?2:1);
    		if((col+colstep)>rackRect.width)
    		{	
 			  	col = colstep;
 			  	row += rowstep;
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
    	int chip = obj%100;
    	int rotation = obj/100;
    	SnakesChip ch = SnakesChip.getChip(chip).getRotated(this,rotation);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);
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
      // erase
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //G.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
      
      SnakesChip chip = SnakesChip.getTile(0);
      SnakesChip tchip = SnakesChip.getTile(1);
      for(SnakesCell c = b.allCells; c!=null; c=c.next)
      {	int ypos = G.Bottom(boardRect) - b.cellToY(c.col, c.row);
      	int xpos = G.Left(boardRect) + b.cellToX(c.col, c.row);
      	SnakesChip cc = (c.onTarget ? tchip : chip);
     	cc.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);
      }
	    b.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
	    b.SetDisplayRectangle(boardRect);
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      //G.centerImage(gc,images[BOARD_INDEX], brect,this);

      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, SnakesBoard gb, Rectangle brect, HitPoint highlight)
    {
     	//
        // now draw the contents of the board and anything it is pointing at
        //
     	boolean moving = hasMovingObject(highlight);
        for(SnakesCell c = gb.allCells; c!=null; c=c.next)
        {	TileType role = c.requiredRole;
        	if(role!=null)
        	{	int ypos = G.Bottom(brect) - gb.cellToY(c.col, c.row);
            	int xpos = G.Left(brect) + gb.cellToX(c.col, c.row);
               	GC.setFont(gc,largeBoldFont());
                GC.Text(gc,true,xpos-CELLSIZE,ypos-CELLSIZE,CELLSIZE*2,CELLSIZE*2,Color.yellow,null,role.toShortString());
            }
        }

        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
 		for (int idx = gb.location.length-1; idx>=0; idx--)
        	{ 
 			SnakesCell c = gb.location[idx];
 			if(c.onBoard)
 			{
			// note that these accessors "lastRowInColumn" etc
        	// are not really needed for simple boards, but they
        	// also work for hex boards and boards with cut out corners
            int ypos = G.Bottom(brect) - gb.cellToY(c.col, c.row);
            int xpos = G.Left(brect) + gb.cellToX(c.col, c.row);
            SnakesChip top = c.topChip();
            if(top!=null) 
            {
            top.getRotated(this,c.rotation).drawChip(gc, this, SQUARESIZE,xpos,ypos,null);
        	}           
 			}}
 		
 		if(highlight!=null)
 		{	SnakesCell c = gb.closestCell(highlight,boardRect);
 			if((c!=null) && gb.LegalToHitBoard(c))
 				{
 	 			int ypos = G.Bottom(brect) - gb.cellToY(c.col, c.row);
 	 			int xpos = G.Left(brect) + gb.cellToX(c.col, c.row);
 	 			
				highlight.hitObject = c;
				highlight.hit_x = xpos;
				highlight.hit_y = ypos;
				
				if(moving || (c.height()==0) || !G.pointInsideSquare(highlight,xpos,ypos,SQUARESIZE/3))
 				{	highlight.hitCode = c.rackLocation;
 					highlight.arrow = moving ? StockArt.DownArrow : StockArt.UpArrow;
 	 	         	highlight.awidth = SQUARESIZE/2;
 	 	         	highlight.spriteColor = Color.red;
				}
 				else
 				{	highlight.awidth=2*SQUARESIZE/3;
 					highlight.arrow = StockArt.Rotate_CW;
 					highlight.hitCode=SnakeId.RotateLocation;
 					highlight.spriteColor = Color.blue;
 				}
 			}
 		}
		if(!use_grid)
		{for(SnakesCell c = gb.allCells; c!=null; c=c.next)
		{   int ypos = G.Bottom(brect) - gb.cellToY(c.col, c.row);
        	int xpos = G.Left(brect) + gb.cellToX(c.col, c.row);
        	Coverage cover = c.cover;
        	String contents = cover.type.toShortString();
        	TileType role = c.getTileRole();
            if(gc!=null)
            {	
            GC.setColor(gc,Color.white);
            if(cover.exits[0]) { gc.drawLine(xpos-CELLSIZE,ypos,xpos,ypos); }
            if(cover.exits[1]) { gc.drawLine(xpos,ypos-CELLSIZE,xpos,ypos); }
            if(cover.exits[2]) { gc.drawLine(xpos+CELLSIZE,ypos,xpos,ypos); }
            if(cover.exits[3]) { gc.drawLine(xpos,ypos+CELLSIZE,xpos,ypos); }
           	GC.setFont(gc,largeBoldFont());
            GC.Text(gc,true,xpos-CELLSIZE,ypos-CELLSIZE,CELLSIZE*2,CELLSIZE*2,Color.yellow,null,contents + ((role==null) ? "" : " "+role.toShortString()));
            }
 		}}
  
    }
    PopupManager numbers = new PopupManager();
    public void headsMenu()
    {  	numbers.newPopupMenu(this,deferredEvents);
    	numbers.addMenuItem("Any",0);
    	for(int i=1;i<=5;i++) { numbers.addMenuItem(""+i,i); }
    	numbers.show(numberRect.x,numberRect.y);
    }
    public void drawNumberRect(Graphics gc,HitPoint highlight)
    {	Rectangle r = numberRect;
    	String msg = (b.numberOfHeads==0)? "Any" : ""+b.numberOfHeads;
    	GC.Text(gc,true,r,Color.black,null,msg);
    	if(G.pointInRect(highlight,r))
    			{
    			highlight.hitCode = SnakeId.HeadsLocation;
    			highlight.spriteRect = r;
    			highlight.spriteColor = Color.red;
    			}
    	GC.frameRect(gc,Color.black,r);
    }
    
    PopupManager pattern = new PopupManager();
    public void patternMenu()
    {	pattern.newPopupMenu(this,deferredEvents);
    	  for(targetType target : targetType.values())
    	  {
    		  pattern.addMenuItem(target.name,target);
    	  }
    	pattern.show(patternRect.x,patternRect.y);
    }
    public void drawPatternRect(Graphics gc,HitPoint highlight)
    {	Rectangle r = patternRect;
    	String msg = b.target.name;
    	GC.Text(gc,true,r,Color.black,null,msg);
    	if(G.pointInRect(highlight,r))
    	{
    		highlight.spriteRect = r;
    		highlight.spriteColor = Color.red;
    		highlight.hitCode = SnakeId.PatternLocation;
    	}
    	GC.frameRect(gc,Color.black,r);
    }
    public void saveGivens()
    {
    	b.saveGivens();
    }
    public void drawSaveGivensRect(Graphics gc,HitPoint highlight)
    {
    	Rectangle r = saveGivensRect;
    	String msg = "Save Givens";
    	GC.Text(gc,true,r,Color.black,null,msg);
    	if(G.pointInRect(highlight,r))
    	{
    		highlight.spriteRect = r;
    		highlight.spriteColor = Color.red;
    		highlight.hitCode = SnakeId.SaveGivensLocation;
    	}
    	GC.frameRect(gc,Color.black,r);
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  drawRackRect(gc,highlight);
       drawNumberRect(gc,highlight);
       drawPatternRect(gc,highlight);
       drawSaveGivensRect(gc,highlight);
     }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  SnakesBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
       SnakeState vstate = gb.getState();
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);

        GC.setFont(gc,standardBoldFont());
        if (vstate != SnakeState.PUZZLE_STATE)
        {
			handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
			
			handleEditButton(gc,editRect,select, highlight, HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==SnakeState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);


        standardGameMessage(gc,
        		vstate==SnakeState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=SnakeState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        goalAndProgressMessage(gc,ourSelect,s.get("do what it takes to win"),progressRect, goalRect);
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
        return (new SnakesMovespec(st, player));
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
//    	SnakesMovespec newmove = (SnakesMovespec) nmove;
//    	SnakesMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            SnakesMovespec m = (SnakesMovespec) History.elementAt(idx);
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
//                	{ SnakesMovespec m2 = (SnakesMovespec)History.elementAt(idx-1);
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
   /** 
     * this method is called from deep inside PerformAndTransmit, at the point
     * where the move has been executed and the history has been edited.  It's
     * purpose is to verify that the history accurately represents the current
     * state of the game, and that the fundamental game machinery is in a consistent
     * and reproducible state.  Basically, it works by creating a duplicate board
     * resetting it and feeding the duplicate the entire history, and then verifying 
     * that the duplicate is the same as the original board.  It's perfectly ok, during
     * debugging and development, to temporarily change this method into a no-op, but
     * be warned if you do this because it is throwing an error, there are other problems
     * that need to be fixed eventually.
     */
    //public void verifyGameRecord()
    //{	//DISABLE_VERIFY = true;
    //	super.verifyGameRecord();
    //}
    
private void playSounds(commonMove m)
{
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
    	if (hp.hitCode instanceof SnakeId) // not dragging anything yet, so maybe start
        {
       	SnakeId hitObject = (SnakeId)hp.hitCode;
		SnakesCell cell = hitCell(hp);
		SnakesChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case Snake_Pool:
	    	PerformAndTransmit("Pick "+cell.row);
	    	break;
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(cell.height()>0)
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
        if(!(id instanceof SnakeId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	SnakeId hitObject = (SnakeId)hp.hitCode;
        SnakeState state = b.getState();
		SnakesCell cell = hitCell(hp);
		SnakesChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        case PatternLocation:
        	patternMenu();
        	break;
        case HeadsLocation:
        	headsMenu();
        	break;
        case SaveGivensLocation:
        	saveGivens();
        	break;
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);

       	case RotateLocation:
        	{	SnakesCell loc = b.location[chip.chipNumber()];
        		PerformAndTransmit("Rotate "+loc.col+" "+loc.row);
        	}
        	break;
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
				else if(chip!=null)
				{
				SnakesCell loc = b.location[chip.chipNumber()];
				PerformAndTransmit( "Pickb "+loc.col+" "+loc.row);
				}
				break;
			}
			break;
			
        case Snake_Pool:
        	{
        	int mov = b.movingObjectIndex();
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw  G.Error("can't drop on rack in state %s",state);
                case PLAY_STATE:
            		PerformAndTransmit(RESET);
            		break;

               	case PUZZLE_STATE:
            		PerformAndTransmit("Drop "+cell.row);
            		break;
            	}
			}
         	}
            break;
        }

         }
        repaint(20);
    }

    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parsable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() 
    { 
    	return(""+b.gametype+" "+b.randomKey); 
   }
    public String sgfGameType() { return(Snakes_SGF); }

   
    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
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

    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {	if(numbers.selectMenuTarget(target))
    	{
    	int n = numbers.value;
    	b.numberOfHeads = n;
    	return(true);
    	}
    	else if( pattern.selectMenuTarget(target))
    	{
    	targetType newtarget = (targetType)pattern.rawValue;
    	b.setTarget(newtarget);
    	generalRefresh();
    	return(true);
    	}
    	return(super.handleDeferredEvent(target,command));
     }

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer()
    { return(new SnakesPlay(this)); }


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
}

