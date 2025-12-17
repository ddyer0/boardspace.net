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
package frogs;

import bridge.*;
import common.GameInfo;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.Enumeration;
import java.util.Hashtable;
import bridge.SystemFont;
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
import lib.Tokenizer;



/**
 * 
 * Change History
 *
 * June 2006  initial work in progress.  

*/
public class FrogViewer extends CCanvas<FrogCell,FrogBoard> implements FrogConstants
{
     /**
	 * 
	 */
	   
    // file names for jpeg images and masks
    static final String ImageDir = "/frogs/images/";
    static final int BACKGROUND_TILE_INDEX=0;
    static final int YELLOW_FELT_INDEX = 1;
    static final int BROWN_FELT_INDEX = 2;
    static final String TextureNames[] = 
    { "background-tile" ,"yellow-felt-tile","brown-felt-tile"};



	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color logrectHighlightColor = new Color(0.9f,0.9f,0.3f);
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    private Color rackBackGroundColor = new Color(150,197,150);
    private Color boardBackgroundColor = new Color(215,197,157);
    
    public Color[] FrogsMouseColors = 
	{ Color.green ,Color.blue, 
	  Color.white, Color.red
	  };
    

    /** default for center dots of mouse sprites */
    public Color[] FrogsMouseDotColors = 
	{ Color.white, Color.white, 
		Color.black, Color.black
    };
    private Font gameLogBoldFont=null;
    private Font gameLogFont = null;
    // images
    private static Image[] textures = null;// background textures
    // private state
    private FrogBoard b = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private final double INITIAL_TILE_SCALE = 3.0;
     // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle bagRect = addRect("bagRect");
    private Rectangle repRect = addRect("reprect");
    private Rectangle handRect[] = addRect("hand",4);
    private Rectangle chipRect[] = addRect("chip",4);
    

  
    public synchronized void preloadImages()
    {	FrogPiece.preloadImages(loader,ImageDir);
	    if (textures == null)
	        { // note that dfor this to work correctly, the images and masks must be the same size.  
	          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
            textures = loader.load_images(ImageDir,TextureNames);
	        }
	    gameIcon = FrogPiece.getChip(2).image;
    }
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {   
        int randomv = info.getInt(OnlineConstants.RANDOMSEED,-1);
        int pl = info.getInt(OnlineConstants.PLAYERS_IN_GAME,2);
        enableAutoDone = true;
        super.init(info,frame);
        use_grid = false;
        gridOption.setState(false);

        MouseDotColors = FrogsMouseDotColors;
        MouseColors = FrogsMouseColors;
        int FontHeight = standardFontSize();
        gameLogBoldFont = SystemFont.getFont(standardPlainFont(), SystemFont.Style.Bold, FontHeight+2);
        gameLogFont = SystemFont.getFont(standardPlainFont(),SystemFont.Style.Plain,FontHeight);
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),FrogId.ZoomSlider);
        zoomRect.min=2.0;
        zoomRect.max=5.0;
        zoomRect.value=INITIAL_TILE_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = HighlightColor;
        b = new FrogBoard(info.getString(GameInfo.GAMETYPE, Frogs_INIT),randomv,pl,getStartingColorMap());
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
        	 zoomRect.setValue(INITIAL_TILE_SCALE);
        	 board_center_x = board_center_y = 0.0;
        	 startFirstPlayer();
        	}
   }



    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unit)
    {	Rectangle chip = chipRect[player];
    	Rectangle hand = handRect[player];
    	Rectangle done = doneRects[player];
    	
    	commonPlayer pl0 = getPlayerOrTemp(player);
    	int u2 = unit/2;
		int sz = unit*2;		
	    G.SetRect(chip, x+sz*3+u2, y, sz, sz+unit);
	    G.SetRect(hand,x,y,sz*3,sz+unit);
	    G.SetRect(done, x, y+sz+unit+unit/2, plannedSeating()?sz*3:0, sz+u2);
	    Rectangle box = pl0.createRectangularPictureGroup(x+sz*5,y,unit);
		G.union(box,chip,done,hand);
		pl0.displayRotation = rotation;
    	return(box);
    }


    public void setLocalBounds(int x,int y,int width,int height)
    {	
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();	
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*14;	
       	int minChatW = fh*35;	
        int minLogH = fh*12;	
        int margin = fh/2;
        int ncols = 20;
        int buttonW = fh*8;
        int vcrW = fh*18;
        int zoomW = fh*12;
        layout.strictBoardsize = false;
        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.55,	// % of space allocated to the board
    			1.4,	// 1.4:1 aspect ratio for the board
    			fh*2.0,	// maximum cell size based on font size
    			0.4		// preference for the designated layout, if any
    			);
    	
    	// place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	// however, if that doesn't work out the main rectangle will shrink.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeDoneEditRep(buttonW, buttonW*2, doneRect,editRect,repRect);
        layout.placeRectangle(bagRect,buttonW,buttonW,BoxAlignment.Edge);

        Rectangle main = layout.getMainRectangle();
        int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/ncols);
    	CELLSIZE = (int)cs;
     	// center the board in the remaining space
        int stateH = fh*5/2;
    	int boardW = mainW;    	
    	int boardX = mainX;
    	int boardY = mainY+stateH;
    	int boardBottom = mainY+mainH-stateH;
    	int boardH = boardBottom-boardY;
    	int stateY = boardY-stateH;
    	placeStateRow( boardX,stateY,boardW,stateH,iconRect,stateRect,annotationMenu,numberMenu,noChatRect);
     	

    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);

    	SetupVcrRects(boardX+fh,boardBottom-vcrW/2-fh,vcrW,vcrW/2);
        
    	placeRow(boardX, boardBottom, boardW, stateH,goalRect);
    	G.placeRight(goalRect, zoomRect, zoomW);
        setProgressRect(progressRect,goalRect);
        
        positionTheChat(chatRect,Color.white,Color.white);
         
    }
  
    private void DrawBag(Graphics gc,HitPoint hitPoint)
    {	drawBag(gc,hitPoint,bagRect,0);
    }
    private void drawBag(Graphics gc,HitPoint hitpoint,Rectangle bag,double rotation)
    {	int cx = G.centerX(bag);
    	int cy = G.centerY(bag);
    	GC.setRotation(gc, rotation, cx, cy);
    	boolean can = b.LegalToHitBag();
    	int wid = G.Width(bag)/2;
    	if(b.bag.drawChip(gc, this,
    			FrogPiece.getChip(FrogPiece.BAG_INDEX), can?hitpoint:null,
    			G.Width(bag),G.Left(bag)+wid,G.centerY(bag),""+b.bag.height()))
    	{
    		hitpoint.arrow = b.pickedObject==null ? StockArt.UpArrow : StockArt.DownArrow;
    		hitpoint.spriteColor = Color.red;
    		hitpoint.awidth = wid/2;
    		hitpoint.hitObject = b.bag;
		}
    	GC.setRotation(gc, -rotation, cx, cy);
    }


	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void DrawChipPool(Graphics gc, FrogState state, commonPlayer pl,int player,HitPoint highlight)
    {	
    	
    	Rectangle chip = chipRect[player];
    	Rectangle hand_r = handRect[player];
    	FrogCell hand[] = b.hand[player];
    	
    	Drawable disk = getPlayerIcon(player);
    	// draw a sample chip to establish the player's color
    	disk.drawChip(gc,this,G.Width(chip)*2,G.centerX(chip),G.centerY(chip),null);
    	
    	int wid = G.Width(hand_r)/2;
    	int xp = G.Left(hand_r)+wid/2;
    	int yp = G.centerY(hand_r);
    	
    	for(int i=0;i<hand.length;i++)
    	{	FrogCell c = hand[i];
    		boolean can = b.LegalToHitChips(c,player);
    		if((c.activeAnimationHeight()==0)
    			&& c.drawChip(gc,this,c.topChip(),can?highlight:null,3*wid/2,xp,yp,null))
    		{	highlight.arrow = b.pickedObject==null ? StockArt.UpArrow : StockArt.DownArrow;
    			highlight.awidth = wid/2;
    			highlight.hitObject = c;
    			highlight.spriteColor = Color.red;
    		}
    		xp += wid;
    		pl.rotateCurrentCenter(c, xp, yp);
    	}
		     	
    }

    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {  	int cellS =  b.cellSize()*2;
       	FrogPiece p = FrogPiece.getChip(idx);
       	p.drawChip(g,this,cellS,xp,yp,null);
    }
    

    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);   
         textures[reviewBackground ? BROWN_FELT_INDEX:YELLOW_FELT_INDEX].tileImage(gc,
          		boardRect); 
      GC.frameRect(gc,Color.black,boardRect);
     // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method
      //gb.DrawGrid(gc, tbRect, use_grid, boardBackgroundColor, Color.blue, Color.blue,Color.black);

     }


    /* draw the board and the chips on it. */
      private void drawBoardElements(Graphics gc, FrogBoard gb, Rectangle tbRect,HitPoint ourTurnSelect,HitPoint anySelect)
     {	
         Rectangle oldClip = GC.combinedClip(gc,tbRect);
    	 int cs = gb.cellSize();
         int cellSize = cs*2;
    	 boolean canHit = !draggingBoard() && G.pointInRect(ourTurnSelect,tbRect);     	
      	//
        // now draw the contents of the board and anything it is pointing at
        //
    	 Hashtable<FrogCell,FrogCell> dests = gb.getDests();
    	 numberMenu.clearSequenceNumbers();

          FrogCell sourceCell = gb.sourceCell();
          FrogCell destCell = gb.destCell();
          FrogCell hitCell = null;
          if(ourTurnSelect!=null) {  ourTurnSelect.awidth = cellSize; }
          int left = G.Left(tbRect);
          int top = G.Bottom(tbRect);
          for(Enumeration<FrogCell> cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements();)
              { //where we draw the grid
        	  FrogCell ccell = cells.nextElement();
              int xpos = left+gb.cellToX(ccell);
              int ypos = top - gb.cellToY(ccell);
              numberMenu.saveSequenceNumber(ccell,xpos,ypos);
                  boolean isADest = dests.get(ccell)!=null;
                  boolean canHitThis = canHit && gb.LegalToHitBoard(ccell);
                  String labl = "";
                  if(use_grid) { labl+=""+ccell.col+ccell.row; }
                  if((ccell.activeAnimationHeight()==0) 
                		  && ccell.drawChip(gc,this,ccell.topChip(),canHitThis?ourTurnSelect:null,cellSize,xpos,ypos,labl))
                  {	 //if(gc!=null) { gc.drawOval(xpos-cellSize/2,ypos-cellSize/2,cellSize,cellSize); }
                	 hitCell = ccell;
                  }
                  if (gc != null)
                  {
                      if((sourceCell==ccell)||(destCell==ccell))
                      {
                    	  StockArt.Dot.drawChip(gc,this,cellSize/2+1,xpos,ypos,null);  
                      }

                 if(isADest)
                  	{
                  	StockArt.SmallO.drawChip(gc,this,CELLSIZE*3,xpos,ypos,labl);

                  	}
              }
          }
         if(hitCell!=null)
         {
          boolean isEmpty = hitCell.topChip()==null;
          ourTurnSelect.awidth = cellSize/3;
          ourTurnSelect.hitCode = FrogId.BoardLocation;
          ourTurnSelect.arrow = isEmpty?StockArt.DownArrow:StockArt.UpArrow;
          ourTurnSelect.spriteColor = Color.red;
         }
    	
         doBoardDrag(tbRect,anySelect,cs,FrogId.InvisibleDragBoard);
         numberMenu.drawSequenceNumbers(gc,CELLSIZE*3,labelFont,labelColor);
   		GC.setClip(gc,oldClip);
      }


    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  FrogBoard gb = disB(gc);
	if(gc!=null)
	{
	// note this gets called in the game loop as well as in the display loop
	// and is pretty expensive, so we shouldn't do it in the mouse-only case

    	gb.SetDisplayParameters(zoomRect.value,1.0,board_center_x,board_center_y,30.0); // shrink a little and rotate 30 degrees
    	gb.SetDisplayRectangle(boardRect);
	}
       int nPlayers = gb.nPlayers();
       boolean moving = hasMovingObject(selectPos);
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;
       HitPoint buttonSelect = moving?null:ourTurnSelect;
       HitPoint nonDraggingSelect = (moving && !reviewMode()) ? null : selectPos;
       FrogState state = gb.getState();

       gameLog.playerIcons = true;
       gameLog.redrawGameLog2(gc, nonDraggingSelect, logRect, Color.black,logrectHighlightColor,gameLogBoldFont,gameLogFont);
        drawBoardElements(gc, gb, boardRect, ourTurnSelect,nonDraggingSelect);
        boolean planned = plannedSeating();
        for(int i=0;i<nPlayers;i++)
        {
        commonPlayer pl = getPlayerOrTemp(i);
        pl.setRotatedContext(gc, selectPos,false);
        DrawChipPool(gc,state,pl,i,ourTurnSelect);
        if(planned && (i==gb.whoseTurn))
        	{
        	HitPoint hit = b.DoneState() ? buttonSelect : null;
        	handleDoneButton(gc,doneRects[i],hit,HighlightColor, rackBackGroundColor);
        	}
        pl.setRotatedContext(gc, selectPos, true);
        }
        
        DrawBag(gc,ourTurnSelect);
        
        zoomRect.draw(gc,nonDraggingSelect);
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
        double messageRotation = pl.messageRotation();
        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for 
        GC.setFont(gc,standardBoldFont());
		if (state != FrogState.PUZZLE_STATE)
        {	HitPoint hit = b.DoneState() ? buttonSelect : null;
        	if(!planned && !autoDoneActive()) { handleDoneButton(gc,doneRect,hit,HighlightColor, rackBackGroundColor); }
        	handleEditButton(gc,messageRotation,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
        }

		drawPlayerStuff(gc,(state==FrogState.PUZZLE_STATE),nonDraggingSelect,
				HighlightColor, rackBackGroundColor);
		

        standardGameMessage(gc,messageRotation,
            		state==FrogState.GAMEOVER_STATE?gameOverMessage(gb):s.get(state.getDescription()),
            				state!=FrogState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
    	// draw a sample chip to establish the player's color
    	getPlayerIcon(gb.whoseTurn).drawChip(gc,this,iconRect,null);
        
        goalAndProgressMessage(gc,nonDraggingSelect,s.get(GoalMessage),progressRect, goalRect);
        
        drawVcrGroup(nonDraggingSelect, gc);

    }

    double textIconScale[] = new double[] {1,1,0.2,-0.2};
    public Drawable getPlayerIcon(int pl)
    {	playerTextIconScale = textIconScale;
        FrogPiece disk = FrogPiece.getChip(FrogPiece.DISC_OFFSET+b.getColorMap()[pl]);
        return disk;
    }
 
	public int getLastPlacement() {
		return (b.dropState);
	}

    /**
     * Execute a move by the other player, or as a result of local mouse activity,
     * or retrieved from the move history, or replayed form a stored game. 
     * @param m the parameter is a commonMove so the superclass commonCanvas can
     * request execution of moves in a generic way.
     * @return true if all went well.  Normally G.Error would be called if anything went
     * seriously wrong.
     */
     public boolean Execute(commonMove m,replayMode replay)
    {
        handleExecute(b,m,replay);
        numberMenu.recordSequenceNumber(b.activeMoveNumber());
        startBoardAnimations(replay,b.animationStack,(int)(b.cellSize()*2.2),MovementStyle.Sequential);
        if(replay.animate) { playSounds(m); }
         return (true);
    }
     void playSounds(commonMove mm)
     {
    	 switch(mm.op)
    	 {
    	 case MOVE_MOVE:
    		 playASoundClip(light_drop,100);
    		 //$FALL-THROUGH$
		 case MOVE_DROPB:
    	 case MOVE_PICKB:
    	 case MOVE_PICK:
    	 case MOVE_DROP:
    		 playASoundClip(light_drop,200);
    		 break;
    	 default: break;
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
        return (new FrogMovespec(st, player));
    }
   

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof FrogId)// not dragging anything yet, so maybe start
        {
       	FrogId hitObject = (FrogId) hp.hitCode;
		FrogCell cell =  hitCell(hp);
		FrogPiece bug = (cell==null) ? null : cell.topChip();
		switch(hitObject)
	    {
	    default: break;
	 
	    case Frog_Hand0:
	    case Frog_Hand1:
	    case Frog_Hand2:
	    case Frog_Hand3:
	    	pickFromHand(hitObject,cell);
	    	break;
	    case InvisibleDragBoard:
        	break;
		case ZoomSlider:
	    case BoardLocation:
	    	if(bug!=null)
	    	{
	    	PerformAndTransmit("Pickb "+cell.col+" "+cell.row);
	    	}
	    	break;
        }
        }
    }
    private void pickFromHand(FrogId hitObject,FrogCell cell)
    {
        {
        	int pl = hitObject.handNum();
        	if(b.pickedObject!=null) 
        	{ PerformAndTransmit("Drop "+pl+" "+cell.row);
        	}
        	else
        	{ PerformAndTransmit("Pick "+pl+" "+cell.row);
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
        if(!(id instanceof FrogId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	FrogId hitObject = (FrogId)hp.hitCode;
        FrogState state = b.getState();
		FrogCell cell = hitCell(hp);
		FrogPiece bug = (cell==null) ? null : cell.topChip();
       	
		
		switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case Frog_Bag:
        case Frog_Hand0:
        case Frog_Hand1:
        case Frog_Hand2:
        case Frog_Hand3:
        	pickFromHand(hitObject,cell);
        	break;
        case ZoomSlider:
        case InvisibleDragBoard:
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case MOVE_FROG_STATE:
			case PUZZLE_STATE:
			{	if(cell!=null)
				{
				int obj = b.movingObjectIndex();
				if(obj>=0)
				{ PerformAndTransmit("Dropb "+cell.col+" "+cell.row);
				}
				else if(bug!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row);
				}
				}
				break;
			}}
			break;
			
        }
        }

    }

    	 
    // return what will be the init type for the game
    public String gameType() { return(b.gametype+" "+b.randomKey+" "+b.nPlayers()); }
    
    public String sgfGameType() { return(Frogs_SGF); }
    public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int ran = his.intToken();
    	int np = his.intToken();
        b.doInit(token,ran,np);
        adjustPlayers(np);

     }



    public BoardProtocol getBoard()   {    return (b);   }
    

    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() { return(new FrogPlay()); }

    public boolean replayStandardProps(String name,String value)
    {	nextIntCompatabilityKludge(b,name,value,"Aug 25 2012");
    	return(super.replayStandardProps(name,value));
    }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/24/2023
     * 	4082 files visited 0 problems
     */
    public void ReplayMove(sgf_node no)
    {
        String comments = "";
        sgf_property prop = no.properties;

        while (prop != null)
        {
            String name = prop.getName();
            String value = (String) prop.getValue();

            //System.out.println("prop " + name + " " + value);
            if (setup_property.equals(name))
            {	Tokenizer tok = new Tokenizer(value);
            	String key = tok.nextToken();
            	long ran = tok.longToken();
            	int np = tok.intToken();
                b.doInit(key,ran,np);
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

