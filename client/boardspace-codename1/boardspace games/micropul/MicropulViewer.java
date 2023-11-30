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
package micropul;
import bridge.*;
import common.GameInfo;

import com.codename1.ui.geom.Rectangle;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Graphics;
import lib.Image;
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
 * Overall Architecture
 * 
 * The site provides the lobby, choice game and opponents, communication between the players, information 
 * for spectators,  rankings, and a host of other services.  Each game has to be concerned only with 
 * the game itself.   An individual game (say, Hex) is launched and each client independently initializes
 * itself to a common starting state.   Thereafter each player specifies messages to be broadcast to the
 * other participants, and receives messages being broadcast by the other participants, which keep everyone
 * informed about the state of the game.  There is no common "true" state of the game - all the participants
 * keep in step by virtue of seeing the same stream of messages.    Messages are mostly simple "pick up a stone"
 * "place a stone on space x" and so on.
 * 
 * The things a game must implement are specified by the class "ViewerProtocol", and a game could just
 * start there and be implemented completely from scratch, but in practice there is another huge pile
 * of things that every game has to do; dealing with graphics, mouse events, saving and restoring the
 * game state from static records, replaying and reviewing games and so on.   These are implemented in the 
 * class "commonCanvas" and by several board-like base classes for hexagonal and square geometry boards.   
 * All the existing games for boardspace use these classes to provide graphics and basic board representation.
 * 
 * For games with robot players, there is another huge pile of things that a robot has to do, generating
 * moves, evaluating and choosing the best, and implementing a lookahead several moves deep.   There's a
 * standard framework for this using the "RobotProtocol" class and the "SearchDriver" class. 
 */

/**
 * 
 * Change History
 */
public class MicropulViewer extends CCanvas<MicropulCell,MicropulBoard> implements MicropulConstants, GameLayoutClient
{
     /**
	 * 
	 */
   	static MicroId JCodes[]  = { MicroId.Jewel0, MicroId.Jewel1, MicroId.Jewel2, MicroId.Jewel3};
		
    static final String Micropul_SGF = "micropul"; // sgf game name
 
 
    // file names for jpeg images and masks
    static final String ImageDir = "/micropul/images/";
    
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BACKGROUND_TABLE_INDEX = 2;
    static final int ICON_INDEX = 3;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "green-felt-tile",
    	  "micropul-icon-nomask"};
    
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(145,192,140);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    
    // private state
    private MicropulBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private final double INITIAL_TILE_SCALE = 3.0;
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle chipRect = addRect("chipRect");		// public chip reserve
    
    private Rectangle rackRects[] = addRect(",rack",2);
    private Rectangle chipRects[] = addRect(",chip",2); 
    private Rectangle jewlRects[] = addRect(",jewel",2);
    private Rectangle scoreRects[] = addRect(",score",2);
    private Rectangle extRects[] =  addRect(",ext",2);
   	
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);

	private static Image textures[] = null;

    public synchronized void preloadImages()
    {	
    	if(textures==null)
    	{	MicropulChip.preloadImages(loader,ImageDir);
            textures = loader.load_images(ImageDir,TextureNames);
    	}
    	gameIcon = textures[ICON_INDEX];
    }
    Color MicropulMouseColors[] = { Color.red,Color.blue };
    Color MicropulMouseDotColors[] = { Color.white,Color.white} ;
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
        use_grid = false;
        gridOption.setState(false);

        // use_grid=reviewer;// use this to turn the grid letters off by default
        MouseDotColors = MicropulMouseDotColors;
        MouseColors = MicropulMouseColors;
        zoomRect = addSlider(TileSizeMessage,s.get(TileSizeMessage),MicroId.ZoomSlider);
        zoomRect.min=1.0;
        zoomRect.max=5.0;
        zoomRect.value=INITIAL_TILE_SCALE;
        zoomRect.barColor=ZoomColor;
        zoomRect.highlightColor = HighlightColor;    
   	 	board_center_x = board_center_y = 0.0;
   
        int randomKey = sharedInfo.getInt(OnlineConstants.RANDOMSEED,-1);

        bb = new MicropulBoard(randomKey,info.getString(GameInfo.GAMETYPE, Micropul_INIT),getStartingColorMap());
        useDirectDrawing(true); 
        doInit(false);
    }

    public commonMove EditHistory(commonMove m) 
    { 	// this ia a simple fix to allow rotate moves to do nothing.
    	return(EditHistory(m,(m.op==MOVE_ROTATE)||(m.op==MOVE_RRACK))); 
    }
    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
         
        bb.doInit(bb.gametype,bb.randomKey);						// initialize the board
        
        if(!preserve_history)
    	{
    	 zoomRect.setValue(INITIAL_TILE_SCALE);
    	 board_center_x = board_center_y = 0.0;
    	 startFirstPlayer();
    	}
    }

    static int SUBCELL = 4;
    public int setLocalBoundsSize(int width, int height,boolean wide,boolean tall)
    {
        double sncols = (16*SUBCELL + (tall?4:(wide ? 34 : 30))); // more cells wide to allow for the aux displays
        int chatHeight = selectChatHeight(height);
        double snrows = 10*SUBCELL+(tall?35:10);  
        double cellw = width / sncols;
        double cellh = (height-(wide ? 0 : chatHeight)) / snrows;
        CELLSIZE = (int)Math.max(1,Math.min(cellw, cellh)); //cell size appropriate for the aspect ration of the canvas
        return(CELLSIZE);
    }
    int playerWidth(boolean horizontal) { return(horizontal ? 33 : 20); }
    
    public Rectangle createPlayerGroup(int player, int x,int y,double rot,int unit)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int chipW = unit*2;
    	int scoreW = unit;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW+unit/4,y,unit);    	
    	Rectangle chip = chipRects[player];
    	G.SetRect(chip, x, y+scoreW+unit/4, chipW, chipW);
    	Rectangle score = scoreRects[player];
    	Rectangle rack = rackRects[player];
    	int rackH = chipW+unit;
    	int rackW = rackH*6;
    	Rectangle ext = extRects[player];
    	Rectangle jewl = jewlRects[player];
    	Rectangle done = doneRects[player];
    	G.SetRect(score, x+(chipW-scoreW)/2, y, scoreW, scoreW);
    	int extY = G.Bottom(box);
    	int extX = G.Right(box);
    	int doneW = scoreW*5;
    	int doneH = doneW/2;
    	G.SetRect(done, extX+unit/2, y,doneW,plannedSeating()?doneH:0);
    	G.SetRect(ext, extX, y+doneH,doneW,scoreW);
    	G.SetRect(rack, x,extY,rackW,rackH);
       	G.SetRect(jewl, extX+doneW+unit, y+unit/2, chipW,chipW);
          	
    	pl.displayRotation = rot;
    	G.union(box, chip,score,rack,ext,jewl);
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
        int margin = fh/2;

    	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 80% of space allocated to the board
    			1,		// aspect ratio for the board
    			fh*2,	// maximum cell size
    			0.5		// preference for the designated layout, if any
    			);
    	int minLogW = fh*20;	
    	int minChatW = fh*35;
        int minLogH = fh*10;	
        int vcrW = fh*18;
        int buttonW = fh*8;
     	CELLSIZE = fh*2;


        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
       	layout.placeTheVcr(this,vcrW,vcrW*3/2);
        
       	layout.placeRectangle(chipRect,CELLSIZE*3,CELLSIZE*7,CELLSIZE*6,CELLSIZE*12,BoxAlignment.Center,false);  
     
    	Rectangle main = layout.getMainRectangle();

    	int boardX = G.Left(main);
    	int boardY = G.Top(main)+CELLSIZE;
    	int boardW = G.Width(main);
    	int boardH = G.Height(main)-CELLSIZE*2;
        
      	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateH = fh*5/2;
        int stateY = boardY-stateH;
        int stateX = boardX;
        int zoomW = CELLSIZE*5;
         
        G.placeStateRow(stateX,stateY,boardW,stateH,iconRect,stateRect,annotationMenu,noChatRect);
        G.placeRight(stateRect, zoomRect, zoomW);
        
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
    }


    // draw the face up chips
    private void DrawRack(Graphics gc,Rectangle r, int player,HitPoint highlight,MicropulBoard gb)
    {	commonPlayer pl = getPlayerOrTemp(player);
        boolean horizontal = G.Width(r)>G.Height(r);
        int dy = horizontal ? 0 : G.Width(r);
        int dx = horizontal ? G.Height(r) : 0;
        int step = horizontal?dx:dy;
        int ww = 4*step/5;
        MicropulCell tiles[] = gb.rack[player];
        for(int i=0,x=G.Left(r)+step/2,y=G.Top(r)+step/2;i<tiles.length;i++,y+=dy,x+=dx)
        {  	MicropulCell cp = tiles[i];
        	MicropulChip chip = cp.topChip();
            boolean canhit = gb.LegalToHitRack(player,cp);
            HitPoint hitp = canhit ? highlight : null;
        	if((chip!=null) && (cp.activeAnimationHeight()==0))
        	{
        	chip.drawChip(gc,cp.masked?-1:cp.rotation[0],this,ww,x,y,null);
        	}
        	else
        	{
        	StockArt.SmallO.drawChip(gc,this,ww,x,y,null);	
        	}
            cp.setLastSize( ww);
           	pl.rotateCurrentCenter(cp, x,y);
            if((hitp!=null)
            		&& ((gb.pickedObject!=null)?(chip==null):(chip!=null)))
            {
            if(cp.closestPointToCell(hitp,ww,x,y))
              {
            	highlight.spriteColor = Color.red;
            if(G.pointInside(hitp, x, y,ww/2))
            	{
            	hitp.arrow = (gb.pickedObject==null)?StockArt.UpArrow:StockArt.DownArrow;
            	hitp.awidth = ww/2;
            }
            	else
                {	
            	hitp.hitCode = MicroId.RotateTile;
            	hitp.awidth = ww;
            	hitp.arrow = StockArt.Rotate_CW;
            	hitp.spriteColor = Color.blue;
           		}
        }   	
            }}
    }


    private void DrawScore(Graphics gc,int n,Rectangle R)
    {
    	GC.frameRect(gc,Color.black,R);
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,true,R,Color.blue,null,""+n);
    }
    private void DrawExtra(Graphics gc,int n,Rectangle R)
    {
    	GC.frameRect(gc,Color.black,R);
    	GC.setFont(gc,standardPlainFont());
    	GC.Text(gc,true,R,Color.blue,null, s.get(PlusTurnMessage,n));
    }
	// draw a box of spare chips. It's purely for visual effect.
    private void DrawJewels(Graphics gc, Rectangle r,  int pl,HitPoint highlight,MicropulBoard gb)
    {	
    	final double dxs[] = {-0.2,0,0.3};
    	final double dys[] = {-0.2,0.2,-0.2};
        boolean canhit = gb.LegalToHitJewels(pl) && G.pointInRect(highlight, r);
        HitPoint hitp = canhit ? highlight : null;
        MicropulCell cp = gb.jewels[pl];
        MicropulChip top = cp.topChip();
        int w = G.Width(r);
        for(int i=0;i<cp.height();i++)
        	{
        	int xp = G.centerX(r)+(int)(dxs[i]*w);
        	int yp = G.Bottom(r)-w/2+(int)(dys[i]*w);
         	if(cp.drawChip(gc,this,top,hitp,w,
        			xp,
        			yp,
        			null))
         	{
         		hitp.arrow = (gb.pickedObject==null)?StockArt.UpArrow:StockArt.DownArrow;
            	hitp.awidth = w/2;
            	hitp.spriteColor = Color.red;
        	}
        }
     }
	// draw a box of spare chips. 
    private void DrawCore(Graphics gc, Rectangle r,  HitPoint highlight,MicropulBoard gb)
    {
        boolean canhit = gb.LegalToHitCore() && G.pointInRect(highlight,r);
        HitPoint hitp = canhit ? highlight : null;
        MicropulCell cp = gb.core;
        labelColor = Color.black;
        if(cp.drawStack(gc,this,hitp,CELLSIZE*2,
        		G.centerX(r),G.Bottom(r)-3*CELLSIZE/2,0,0.05,""+cp.height()))
        {	hitp.arrow = (gb.pickedObject==null)?StockArt.UpArrow:StockArt.DownArrow;
        	hitp.awidth = G.Width(r)/4;
        	hitp.spriteColor = Color.red;
        }
     }
    
	// draw a box of spare chips. It's purely for visual effect.
    private void DrawSupply(Graphics gc, Rectangle r,  int player,HitPoint highlight,MicropulBoard gb)
    {	commonPlayer pl = getPlayerOrTemp(player);
        boolean canhit = gb.LegalToHitSupply(player) && G.pointInRect(highlight,r);
        HitPoint hitp = canhit ? highlight : null;
        MicropulCell cp = gb.supply[player];
        int rw = 4*G.Width(r)/5;
        labelColor = Color.black;
        int xp = G.centerX(r);
        int yp = G.Bottom(r)-3*CELLSIZE/2;
        if(cp.drawStack(gc,this,hitp,rw,
        		xp,yp,0,0.05,""+cp.height()))
        {	hitp.arrow = (gb.pickedObject==null)?StockArt.UpArrow:StockArt.DownArrow;
        	hitp.awidth = rw/2;
        	hitp.spriteColor = Color.red;
        }
        pl.rotateCurrentCenter(cp, xp,yp);
     }
    //
    // sprites are normally a game piece that is "in the air" being moved
    // around.  This is called when dragging your own pieces, and also when
    // presenting the motion of your opponent's pieces, and also during replay
    // when a piece is picked up and not yet placed.  While "obj" is nominally
    // a game piece, it is really whatever is associated with b.movingObject()
    //
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {	boolean inboard = boardRect.contains(xp,yp);
   		int cellS = inboard? bb.cellSize() : G.Width(chipRect) ;
   		int chip = obj/100;
   		int rot = obj%100;
   		MicropulChip mchip = MicropulChip.getChip(chip);
   		if(rot==4) { rot=-1; }
   		if(mchip.isJewel()) { rot = -1; }
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
    	mchip.drawChip(g,rot,this,cellS, xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  


    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { // erase
      boolean review = reviewMode() && !mutable_game_record;
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
         textures[review ? BACKGROUND_REVIEW_INDEX:BACKGROUND_TABLE_INDEX].tileImage(gc,
          		boardRect); 
      GC.frameRect(gc,Color.black,boardRect); 
        
      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      //gb.DrawGrid(gc, brect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);

      // draw the tile grid.  The positions are determined by the underlying board
      // object, and the tile itself if carefully crafted to tile the board
      // when drawn this way.  For games with simple graphics, we could use the
      // simpler loop for(Cell c = b.allCells; c!=null; c=c.next) {}
      // but for more complex graphics with overlapping shadows or stacked
      // objects, this double loop is useful if you need to control the
      // order the objects are drawn in.
 
    }

   /* draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */

    private void drawBoardElements(Graphics gc, MicropulBoard gb, Rectangle tbRect,
    		HitPoint ourTurnSelect,HitPoint anySelect)
    {	
       Rectangle oldClip = GC.combinedClip(gc,tbRect);
   	   MicropulChip po = gb.pickedObject;
   	   boolean movingJewel = (po!=null) && po.isJewel();
       int cellSize = gb.cellSize();
       boolean draggingBoard = draggingBoard();
       gb.sweepJewels();
    	//
        // now draw the contents of the board and anything it is pointing at
        //
        Hashtable<MicropulCell,MicropulCell> dests = gb.movingObjectDests();
        MicropulCell sourceCell = gb.pickedSource; 
        MicropulCell destCell = gb.droppedDest;
        int cellSize4 = cellSize/4;
 
        int left = G.Left(tbRect);
        int top = G.Bottom(tbRect);
        for(Enumeration<MicropulCell> cells = gb.getIterator(Itype.TBRL); cells.hasMoreElements(); )
        { 	MicropulCell cell = cells.nextElement();
            int xpos = left + gb.cellToX(cell);
            int ypos = top - gb.cellToY(cell);
            boolean isADest = dests.get(cell)!=null;
            boolean isASource = (cell==sourceCell)||(cell==destCell);
            MicropulChip topPiece = cell.topChip();
            boolean topJewel = (topPiece!=null) && topPiece.isJewel();
            MicropulChip bottomPiece = cell.bottomChip();
            boolean emptyCell = (topPiece==null);
            boolean canHitThis = !draggingBoard && gb.LegalToHitBoard(cell);
             //G.DrawAACircle(gc,xpos,ypos,1,tiled?Color.green:Color.blue,Color.yellow,true);
            int activeHeight = cell.height()-cell.activeAnimationHeight();
            if(bottomPiece!=null && (activeHeight>0))
            {	String grid = use_grid ? ""+cell.col+cell.row : null;
            	bottomPiece.drawChip(gc,cell.rotation[0],this,cellSize,xpos,ypos,grid); 
            	for(int quad = 0; quad<4;quad++)
            	{	MicropulChip tintCode = cell.tintCode(quad);
            		if(tintCode!=null)
            		{
            		
            		tintCode.drawChip(gc,quad,this,cellSize,xpos,ypos,null);
            		if(cell.tintClosed(quad))
	            		{tintCode.drawChip(gc,quad,this,cellSize,xpos,ypos,null);
	            		 tintCode.drawChip(gc,quad,this,cellSize,xpos,ypos,null);
	            		}
	            	}
            	}
            }
            cell.rotateCurrentCenter(gc,xpos,ypos);
            cell.setLastSize(cellSize);
            if(topJewel || movingJewel)
            {
           	
            if(topJewel && (activeHeight>=2))
            {for(int h=1;h<cell.height();h++)
            {
            MicropulChip ctop = cell.getChipAt(h);
            int toprot = cell.getRotationAt(h);
            if(ctop==null)
            {//G.print("notop");
            }else
            {
        	ctop.drawChip(gc,toprot,this,cellSize,xpos,ypos,null);
        	}}}

            if(canHitThis)
            {
            for(int quad = 0;quad<4;quad++)
            {
            int xp0 = ((quad==0)||(quad==3)) ? xpos-cellSize4 : xpos+cellSize4;
            int yp0 = (quad<2) ? ypos-cellSize4 : ypos+cellSize4;
            
            // my slightly strange data structure.  each cell is a stack of up to 5 elements,
            // the tile at the bottom and jewels piled on top.  Each has a rotation, the jewel
            // rotation indicates which quad of the tile it sits on.  The UI doesn't let us
            // manipulate the items not on the top layer, even though in rare situations, a
            // tile with lots of jewels we ought to be able to pick up any of them.  During
            // games this doesn't matter since the jewels don't move.
            if( (movingJewel 
            		? ((bottomPiece!=null) && bottomPiece.legalToPlaceJewel(cell,quad)) 
            		: topJewel && (quad==cell.getRotationAt(cell.height()-1)))	// jewel on top is at this position
            	&& cell.closestPointToCell(ourTurnSelect,cellSize,xp0,yp0))
            {	ourTurnSelect.hitCode = JCodes[quad];
                ourTurnSelect.arrow = (po!=null)?StockArt.DownArrow:StockArt.UpArrow;
                ourTurnSelect.awidth = cellSize/2;
            	ourTurnSelect.spriteColor = Color.red;
            }}}
            }
            else
            {
            if(canHitThis
            		&& cell.closestPointToCell(ourTurnSelect,cellSize, xpos, ypos))
            {
            ourTurnSelect.spriteColor = Color.red;
        	ourTurnSelect.awidth = cellSize/2;
             
            if((cell.topChip()==null)
        		|| G.pointInsideSquare(ourTurnSelect, xpos, ypos, cellSize4))
            	{
                ourTurnSelect.hitCode = emptyCell?MicroId.EmptyBoard:MicroId.BoardLocation;
                ourTurnSelect.arrow = (po!=null)?StockArt.DownArrow:StockArt.UpArrow;
            	}
            else
            	{
            	ourTurnSelect.hitCode = MicroId.RotateTile;
            	ourTurnSelect.arrow = StockArt.Rotate_CW;
            	ourTurnSelect.awidth=2*cellSize/3;
            	ourTurnSelect.spriteColor = Color.blue;
            	}
            }
            
            if(cell==destCell)
            {	MicropulChip.BLANK.drawChip(gc,this,cellSize,xpos,ypos,null);
            }
            else if(isASource)
	            {
            	StockArt.SmallO.drawChip(gc,this,cellSize*2/3,xpos,ypos,null);
            	//GC.cacheAACircle(gc,xpos,ypos,cellSize/10,Color.green,Color.yellow,true);
            } else
            if(isADest)
	            {
            	StockArt.SmallO.drawChip(gc,this,cellSize*2/3,xpos,ypos,null);
            	//GC.cacheAACircle(gc,xpos,ypos,cellSize/10,Color.red,Color.yellow,true);
            }
            }
        if((po==null)
        		&& (anySelect!=null)
        		&& (anySelect.hitCode==MicroId.EmptyBoard))
        {
        	anySelect.arrow = null;
        	anySelect.spriteColor = null;
        	anySelect.hitCode = DefaultId.HitNoWhere;
        }
        }
        doBoardDrag(tbRect,anySelect,cellSize,MicroId.InvisibleDragBoard);
 		GC.setClip(gc,oldClip);
    }

    /*
     * draw the main window and things on it.  
     * If gc!=null then actually draw, 
     * If selectPos is not null, then as you draw (or pretend to draw) notice if
     * you are drawing under the current position of the mouse, and if so if you could
     * click there to do something.  Care must be taken to consider if a click really
     * ought to be allowed, considering spectator status, use of the scroll controls,
     * if some board token is already actively moving, and if the game is active or over.
     * 
     * This dual purpose (draw, and notice mouse sensitive areas) tends to make the
     * code a little complicated, but it is the most reliable way to make sure the
     * mouse logic is in sync with the drawing logic.
     * 
    General GUI checklist

    vcr scroll section always tracks, scroll bar drags
    lift rect always works
    zoom rect always works
    drag board always works
    pieces can be picked or dragged
    moving pieces always track
    stray buttons are insensitive when dragging a piece
    stray buttons and pick/drop are inactive when not on turn
*/
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  MicropulBoard gb = disB(gc);
       MicropulState state = gb.getState();
   		if(gc!=null)
   		{
   			// note this gets called in the game loop as well as in the display loop
   			// and is pretty expensive, so we shouldn't do it in the mouse-only case
	   	gb.SetDisplayParameters(zoomRect.value,1.0,board_center_x,board_center_y,0.0); // shrink a little and rotate 30 degrees
	   	gb.SetDisplayRectangle(boardRect);
   		}
       boolean moving = hasMovingObject(selectPos);
       // 
       // if it is not our move, we can't click on the board or related supplies.
       // we accomplish this by supressing the highlight pointer.
       //
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;
       //
       // even if we can normally select things, if we have already got a piece
       // moving, we don't want to hit some things, such as the vcr group
       //
       HitPoint buttonSelect = moving ? null : ourTurnSelect;
       // hit anytime nothing is being moved, even if not our turn or we are a spectator
       HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;
       
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,nonDragSelect);
       DrawCore(gc, chipRect, ourTurnSelect,gb);
       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
       boolean planned = plannedSeating();
       
       for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
       {
    	   commonPlayer cpl = getPlayerOrTemp(i);
    	   cpl.setRotatedContext(gc, selectPos,false);
 
           DrawRack(gc, rackRects[i],i, ourTurnSelect,gb);
           DrawSupply(gc,chipRects[i],i, ourTurnSelect,gb);
           DrawJewels(gc, jewlRects[i],i, ourTurnSelect,gb);
           DrawScore(gc,gb.scoreForPlayer(i),scoreRects[i]);
           boolean myTurn = (i==gb.whoseTurn);
           if(myTurn && (gb.extraTurns>0))
           	{DrawExtra(gc,gb.extraTurns,extRects[i]);
           	}
     
    	   if(planned && myTurn)
    		   {handleDoneButton(gc,doneRects[i],(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
    		   }
    	   cpl.setRotatedContext(gc, selectPos,true);
       }
       zoomRect.draw(gc,nonDragSelect);
       GC.setFont(gc,standardBoldFont());

		if (state != MicropulState.PUZZLE_STATE)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!planned) 
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
				}
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);
       }

		drawPlayerStuff(gc,(state==MicropulState.PUZZLE_STATE),nonDragSelect,HighlightColor,rackBackGroundColor);
  
		// draw the avatars
        standardGameMessage(gc,messageRotation,
        		state==MicropulState.GAMEOVER_STATE?gameOverMessage(gb):s.get(state.getDescription()),
        				state!=MicropulState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null,2);
       goalAndProgressMessage(gc,nonDragSelect,s.get(GoalMessage),progressRect, goalRect);
        //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for games with no possible repetition
    
        // draw the vcr controls
        drawVcrGroup(nonDragSelect, gc);

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
        handleExecute(bb,mm,replay);
        startBoardAnimations(replay,bb.animationStack,bb.cellSize(),MovementStyle.Chained);

		lastDropped = bb.lastDroppedDest;	// this is for the image adjustment logic
		if(replay!=replayMode.Replay) { playSounds(mm); }
       return (true);
    }
 void playSounds(commonMove mm)
 {
	 switch(mm.op)
	 {
	 case MOVE_DROPB:
	 case MOVE_PICKB:
	 case MOVE_PICK:
	 case MOVE_DROP:
		 playASoundClip(light_drop,100);
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
        return (new Micropulmovespec(st, player));
    }
   

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof MicroId) // not dragging anything yet, so maybe start
        {

        MicroId hitObject =  (MicroId)hp.hitCode;
        MicropulCell c = hitCell(hp);
 	    switch(hitObject)
	    {
	    default: break;
	    
        case InvisibleDragBoard:
        	break;
        case RotateTile:
        	if(c!=null)
        	{ for(int newr=0;newr<4;newr++)
        		{
        		int rr = (newr+c.rotation[0]+1)%4;
        		if(c.onBoard)
        		{
        		if(c.topChip().legalToPlaceMicropul(c,rr))
        		{
        		PerformAndTransmit("Rotate "+c.col+" "+c.row+" "+rr);
        		break;
        		}
        		}
        		else
        		{
        			PerformAndTransmit("RRack "+c.owner+" "+c.row+" "+rr);
        		}
        		}
        	}
        	break;
	    case Core:
	    case Supply:
	    case Rack:
	    case Jewels:
	    	if(c!=null)
	    		{ MicropulChip top = c.topChip();
	    		  if(top!=null)
	    		  {
	    		  String msg = "Pick "+hitObject.shortName;
	    		  String pla = "";
	    		  String loc = "";
	    		  switch(hitObject)
	    		  {
	    		  case Rack: loc = " "+c.row;
					//$FALL-THROUGH$
	    		  case Jewels:
	    		  case Supply: pla = " "+c.owner;
	    		  	break;
	    		  case Core: break;
	    		  default:
					break;
	    		  }
	    		  PerformAndTransmit(msg+pla+loc+" "+top.index);
	    		  }
	    		}
	    	break;
	    case BoardLocation:
	    	PerformAndTransmit("Pickb "+c.col+" "+c.row);
	    	break;
        }
         }
    }
	private void doDropChip(MicropulCell cell,MicroId hitCode)
	{	MicropulState state = bb.getState();
		int jcode = hitCode.jCode();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case PUZZLE_STATE:
		case CONFIRM_STATE:
		case PLAY_STATE:
			MicropulChip po = bb.pickedObject ;
			int rot = jcode>=0 ? jcode : po.legalMicropulRotation(cell);
			if(rot>=0)
				{
				// looking for the source of "dropb r -1 0" 
				G.Assert(cell.onBoard,"should be a cell on the board");
				PerformAndTransmit("dropb "+cell.col+" "+cell.row+" "+rot);
				}
			break;
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
        if(!(id instanceof MicroId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	MicroId hitCode = (MicroId)hp.hitCode;
        MicropulCell hitObject = bb.getCell(hitCell(hp));
		MicropulState state = bb.getState();
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case RotateTile:
        case InvisibleDragBoard:
        case ZoomSlider:
        	break;
        case Jewel0:
        case Jewel1:
        case Jewel2:
        case Jewel3:
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
				
				if((bb.pickedObject==null) &&!bb.isDest(bb.getCell(hitObject)))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					throw G.Error("shouldn't hit a chip in state %s",state);
					}
				//$FALL-THROUGH$
			case PUZZLE_STATE:
				if(bb.pickedObject!=null) { doDropChip(hitObject,hitCode); break; }
				// fall through and pick up the previously dropped piece
				PerformAndTransmit("Pickb "+hitObject.col+" "+hitObject.row);
				break;
			}
			break;
			
        case EmptyBoard:
			if(bb.pickedObject!=null) { doDropChip(hitObject,hitCode); }
			break;
        case Rack:
        case Supply:
        case Core:
        case Jewels:
           if(bb.pickedObject!=null) 
			{//if we're dragging a black chip around, drop it.
        	    String pla = "";
        	    String row = "";
            	String msg  = "Drop "+hitCode.shortName;
            	switch(hitCode)
            	{
            	case Rack: row=" "+hitObject.row;
					//$FALL-THROUGH$
				case Jewels:
            	case Supply: pla = " "+hitObject.owner;
            		break;
            	case Core: break;
				default:
					break;
            	}
            	PerformAndTransmit(msg+pla+row);
			}
           break;
        }
         }

     }


    // return what will be the init type for the game
    public String gameType() { return(""+bb.gametype+" "+bb.randomKey); }	// this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Micropul_SGF); }	// this is the official SGF number assigned to the game

    
    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        long rk = G.LongToken(his);
        bb.doInit(token,rk);
     }



/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically.
 * 
 * This is a good place to make notes about threads.  Threads in Java are
 * very dangerous and tend to lead to all kinds of undesirable and/or flakey
 * behavior.  The fundamental problem is that there are three or four sources
 * of events from different system-provided threads, and unless you are very
 * careful, these threads will all try to use and modify the same data
 * structures at the same time.   Java "synchronized" declarations are
 * hard to get right, resulting in synchronization locks, or lack of
 * synchronization where it is really needed.
 * 
 * This toolkit addresses this problem by adopting the "one thread" model,
 * and this is where it is.  Any other threads should do as little as possible,
 * mainly leave breadcrumbs that will be picked up by this thread.
 * 
 * In particular:
 * GUI events do not respond in the native thread.  Mouse movement and button
 * events are noted for later.  Requests to repaint the canvas are recorded but
 * not acted upon.
 * Network I/O events, merely queue the data for delivery later.
 *  */
    
    //   public void ViewerRun(int wait)
    //   {
    //       super.ViewerRun(wait);
    //   }
   /**
     * returns true if the game is over "right now", but also maintains 
     * the gameOverSeen instance variable and turns on the reviewer variable
     * for non-spectators.
     */
    //public boolean GameOver()
    //{	// the standard method calls b.GameOver() and maintains
    	// two variables.  
    	// "reviewer=true" means we were a player and the end of game has been reached.
    	// "gameOverSeen=true" means we have seen a game over state 
    //	return(super.GameOver());
    //}
    
    /** this is used by the stock parts of the canvas machinery to get 
     * access to the default board object.
     */
    public BoardProtocol getBoard()   {    return (bb);   }

    /** this is used by the game controller to supply entertainment strings to the lobby */
    public String gameProgressString()
    {	// this is what the standard method does
    	// return ((reviewer ? s.get(Reviewing) : ("" + viewMove)));
    	return(super.gameProgressString()
    			+" "+bb.scoreForPlayer(0)
    			+" "+bb.scoreForPlayer(1));
    }




    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new MicropulPlay());
    }
    public boolean replayStandardProps(String name,String value)
    {	nextIntCompatabilityKludge(bb,name,value,"Oct 25 2013");
    	return(super.replayStandardProps(name,value));
    }
    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     *  5222 files visited 0 problems
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
    			long rk = G.LongToken(tok);
                bb.doInit(gametype,rk);
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
           else if (parseVersionCommand(name,value,2)) {}
           else if (parsePlayerCommand(name,value)) {}
            else
            {	// handle standard game properties, and also publish any
            	// unexpected names in the chat area
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

