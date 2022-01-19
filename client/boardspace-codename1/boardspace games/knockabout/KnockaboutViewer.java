package knockabout;

import bridge.*;

import com.codename1.ui.geom.Rectangle;

import lib.Graphics;
import lib.Image;
import online.common.*;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.CellId;
import lib.DefaultId;
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
 * This code is derived from the "HexGameViewer" and other viewer classes.  Refer to the
 * documentation there for overall structure notes.
 * 
*/
public class KnockaboutViewer extends CCanvas<KnockaboutCell,KnockaboutBoard> implements KnockaboutConstants, GameLayoutClient
{	// colors
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(164,184,180);
    private Color boardBackgroundColor = new Color(202,215,212);
    
    // images
    private static StockArt[] images = null; // images of black and white gobblets and board
    private static Image[] textures = null;// background textures
    // private state
    private KnockaboutBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle scoreRects[] = addRect("score",2);

    private Rectangle repRect = addRect("reprect");
    private KnockaboutCell roll_anim_cell = null;
    private long roll_anim_stop = 0;

    public void preloadImages()
    {	
    	if (textures == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        KnockaboutChip.preloadImages(loader,ImageDir);
        images = StockArt.preLoadArt(loader,ImageDir, ImageFileNames,SCALES); // load the main images
        textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = images[BOARD_INDEX].image;
    }


	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
        int randomKey = sharedInfo.getInt(OnlineConstants.RANDOMSEED,-1);
        b = new KnockaboutBoard(randomKey,info.getString(OnlineConstants.GAMETYPE, Knockabout_Standard_Init),
        		getStartingColorMap());
        doInit(false);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey);			// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
   }

    	
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = scoreRects[player];
    	int chipW = unitsize*3;
    	int chipH = unitsize*3;
    	int doneW = unitsize*5;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	Rectangle done = doneRects[player];
     	
    	G.SetRect(chip, x, y+unitsize/2, chipW, chipH);
    	G.SetRect(done, G.Right(box)+unitsize/2,y+unitsize/2,doneW,plannedSeating()?doneW/2:0);
    	
    	pl.displayRotation = rotation;
    	
    	G.union(box, chip,done);
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
    	int minLogW = fh*20;	
    	int vcrw = fh*16;
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        int nrows = b.nrows;  
        int ncols = b.ncols;
    	boolean rotate = seatingFaceToFaceRotated();

       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*2.5,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);

        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,vcrw,vcrw*3/2);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	int CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = rotate ?  (int)(nrows*CELLSIZE) : (int)(ncols*CELLSIZE);
    	int boardH = rotate ? (int)(ncols*CELLSIZE) : (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH-CELLSIZE/2)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateH = fh*3;
        int stateY = boardY;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW,stateH,iconRect,stateRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	if(rotate)
    	{
    		G.setRotation(boardRect, -Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH-stateH/2,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
        

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
    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	KnockaboutChip ic = KnockaboutChip.getChip(idx);
    	ic.drawChip(g,this,scaleCell(SQUARESIZE,xp,yp,boardRect),xp,yp,null);
    }


    /** this is used by the game controller to supply entertainment strings to the lobby */
    public String gameProgressString()
    {	// this is what the standard method does
    	// return ((reviewer ? s.get(Reviewing) : ("" + viewMove)));
    	return(super.gameProgressString()
    			+" "+(5-b.chipsInGutter[0])
    			+" "+(5-b.chipsInGutter[1]));
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
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
       
      // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid


     images[BOARD_INDEX].getImage(loader).centerImage(gc, brect);
      b.SetDisplayParameters(
       		 0.965, //0.93,	// scale 
       		 0.8,	// yscale
       		 -0.02,	// xoff
       		 0.48,//-0.1,	// yoff
       		 00,	// rot
       		 0.0,	// xperspective
       		 0.0,	// yperspective
   		 0.0	// skew
   		 );
      	b.SetDisplayRectangle(brect);
        b.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    private void DrawScore(Graphics gc,Rectangle r,int player)
    {	if(gc!=null)
    	{
    	int idx = 5-b.chipsInGutter[player];
    	KnockaboutChip cc = (idx>0) 
    					? KnockaboutChip.getChip(b.playerColor[player],6,idx)
    					: null;
    	if(cc!=null) { cc.drawChip(gc,this,r,null,0.8); }
    	}
    }
    

   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, KnockaboutBoard rb, Rectangle brect, HitPoint highlight)
    {	Hashtable<KnockaboutCell,KnockaboutCell> dests = rb.getMoveDests();
    	KnockaboutState state = rb.getState();
    	
     	int dotsize = Math.max(2,SQUARESIZE/15);
     	int mov = rb.movingObjectIndex();
     	long now = G.Date();
     	if(roll_anim_cell!=null) 
     		{ 
     		if(now > roll_anim_stop) { roll_anim_cell=null; } else { repaint(20); }
     		} 
     	//
        // now draw the contents of the board and anything it is pointing at
        //
       	Enumeration<KnockaboutCell>cells = rb.getIterator(Itype.TBRL);
       	while(cells.hasMoreElements())
            {
       		KnockaboutCell cell = cells.nextElement();
            boolean isADest = dests.get(cell)!=null;
            boolean isSource = rb.isSource(cell);
            boolean canHit = rb.LegalToHitBoard(cell);
            int ypos = G.Bottom(brect) - rb.cellToY(cell);
            int xpos = G.Left(brect) + rb.cellToX(cell);
       		int scl = scaleCell(SQUARESIZE,xpos,ypos,boardRect);

            if(cell==roll_anim_cell)
            {	KnockaboutChip chip = cell.topChip();
            	if(chip!=null)
            	{	int show = (int)(now%chip.numSides)+1;
             		KnockaboutChip alt = KnockaboutChip.getChip(chip.color,chip.numSides,show);
            		cell.drawChip(gc,this,alt,scl,xpos,ypos,null);
            		
            	}
            }
            else
            	{ cell.drawStack(gc,canHit?highlight:null,xpos,ypos,this,0,scl,1.0,null);
            	}
            if((cell.topChip()!=null) 
            		&& (state==KnockaboutState.PUZZLE_STATE)
            		&& (mov<0)
            		&&(highlight!=null)
            		&&(highlight.hitCode==DefaultId.HitNoWhere)
            		)
            {	// mouse adjacent to a chip in puzzle mode
            	if((Math.abs(G.Top(highlight)-ypos)<SQUARESIZE/2)
            		&& G.pointInside(highlight, xpos, ypos, 3*SQUARESIZE/5))
            	{	boolean left = (xpos<G.Left(highlight));
            		highlight.hitCode = left?KnockId.HitPlusCode:KnockId.HitMinusCode;
            		highlight.arrow = images[left? PLUS1_INDEX :MINUS1_INDEX];
            		highlight.awidth = SQUARESIZE;
            		highlight.hitObject = cell;
            		highlight.hit_x = G.Left(highlight);
            		highlight.hit_y = G.Top(highlight);
            	}
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
         	{	StockArt.Rotate_CW.drawChip(gc,this,3*SQUARESIZE/2,xpos,ypos,null);
         	}
        }
       	
        if((highlight!=null)&& (highlight.hitObject!=null) && (highlight.hitCode instanceof KnockId))
        {	KnockId hit = (KnockId)highlight.hitCode;
        		switch(hit)
        		{
        		case HitPlusCode:
        		case HitMinusCode:
        			break;
        		default:
       			highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
        		highlight.awidth = SQUARESIZE/2;
        		highlight.spriteColor = Color.red;
        		}
        }
    }
    
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  KnockaboutBoard gb = disB(gc);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      KnockaboutState vstate = gb.getState();
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
       
       boolean planned = plannedSeating();
       GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
       drawBoardElements(gc, gb, boardRect, ot);
       GC.unsetRotatedContext(gc,highlight);
       
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
       {	commonPlayer pl = getPlayerOrTemp(i);
       	pl.setRotatedContext(gc, highlight, false);
       	DrawScore(gc,scoreRects[i],i);
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
  	   drawPlayerStuff(gc,(vstate==KnockaboutState.PUZZLE_STATE),ourSelect,
  	   			HighlightColor, rackBackGroundColor);
  	 
       if (vstate != KnockaboutState.PUZZLE_STATE)
        {
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
 
        }
 
        standardGameMessage(gc,messageRotation,
        		vstate==KnockaboutState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=KnockaboutState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
       	DrawScore(gc,iconRect,gb.whoseTurn);
        goalAndProgressMessage(gc,ourSelect,s.get(GoalMessage),progressRect, goalRect);

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
        if((mm.op==MOVE_DONE) && (b.rollCell!=null))
        {	roll_anim_stop = G.Date()+600;
        	roll_anim_cell = b.rollCell;
        }
        else { roll_anim_cell = null; }
        
        handleExecute(b,mm,replay);
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Sequential);
         
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
        return (new KnockaboutMovespec(st, player));
    }
         

private void playSounds(commonMove m)
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
    case MOVE_ROLL:
    case MOVE_BOARD_BOARD:
    	{ playASoundClip(light_drop,50);
    	}
    	break;
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
    	if (hp.hitCode instanceof KnockId) // not dragging anything yet, so maybe start
        {

        KnockId hitObject = (KnockId)hp.hitCode;
		KnockaboutCell cell = hitCell(hp);
		KnockaboutChip chip = (cell==null) ? null : cell.topChip();
		
        if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case HitPlusCode:	
        	  PerformAndTransmit("roll "+cell.col+" "+cell.row+" "+chip.add(1));
        	  break;
        case HitMinusCode:
      	  	  PerformAndTransmit("roll "+cell.col+" "+cell.row+" "+chip.add(-1));
      	  	  break;              
	    case BoardLocation:
	    	if(cell.chip!=null)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.pieceNumber());
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
        if(!(id instanceof KnockId)) {  missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
   		missedOneClick = false;
    	KnockId hitObject = (KnockId)hp.hitCode;
		KnockaboutCell cell = hitCell(hp);
		KnockaboutChip cup = (cell==null) ? null : cell.topChip();
		KnockaboutState state = b.getState();	// state without resignation
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case HitPlusCode:
        case HitMinusCode:
        	break;
        case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) 
					{ 
					  PerformAndTransmit("dropb "+cell.col+" "+cell.row + " "+b.nextRandom()); 
					}
				}
				else if(cup!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+cup.pieceNumber());
				}
				break;
			}
			break;
			
        }
         }
     }

    public String gameType() { return(b.gametype+" "+b.randomKey); }
    public String sgfGameType() { return(Knockabout_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long ran = G.LongToken(his);
	    b.doInit(token,ran);
	}


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new KnockaboutPlay()); }


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

