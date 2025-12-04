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
package breakingaway;

import bridge.*;
import common.GameInfo;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;
import static breakingaway.BreakingAwayMovespec.*;
import static breakingaway.BreakingAwayBoard.*;
import breakingaway.BreakingAwayPiece.ChipColor;
/* below here should be the same for codename1 and standard java */
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import vnc.VNCService;

import java.util.*;

import lib.Graphics;
import lib.Image;
import lib.TextButton;
import lib.Tokenizer;
import lib.CellId;
import lib.DefaultId;
import lib.Drawable;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.Slider;
import lib.Sort;
import lib.StockArt;
import lib.Text;
// TODO: riders don't rotate with the rotate button
// TODO: add a less-perspective mode for table play
/**
 * 
 * Change History
 *
 * March 2010  initial work in progress.  

*/
public class BreakingAwayViewer extends CCanvas<BreakingAwayCell,BreakingAwayBoard> implements BreakingAwayConstants
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	static final String BreakingAway_SGF = "BreakingAway"; // sgf game name
	   
	    
	    // file names for jpeg images and masks
	static final String ImageDir = "/breakingaway/images/";
	static final int BACKGROUND_TILE_INDEX=0;
	static final int YELLOW_FELT_INDEX = 1;
	static final int BROWN_FELT_INDEX = 2;
	static final int BUBBLE_INDEX = 3;
	static final String TextureNames[] = 
	{ "background-tile" ,"green-felt-tile","brown-felt-tile","bubble-off","bubble-on"};
	static final int BOARD_OBLIQUE_INDEX = 0;
	static final int BOARD_OBLIQUE_REVERSE_INDEX = 1;
	static final String ImageNames[] = 
	{ "board-oblique","board-oblique-reverse"/*,"board" */};
    static final int PLUSONE_INDEX = 0;
    static final int MINUSONE_INDEX = 1;
    static final int FORWARDTIME_INDEX = 2;
    static final int BACKWARDTIME_INDEX = 3;
    static final String ArtNames[] = {"plusone","minusone","forwardtime","backwardtime"};
    static final double ArtScales[][] = {{0.5,0.5,1.0},{0.5,0.5,1.0},{0.5,0.5,1.2},{0.5,0.5,1.2}};

	    	

	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color ZoomColor = new Color(0.0f,0.0f,1.0f);
    private Color rackBackGroundColor = new Color(150,197,150);
    private Color boardBackgroundColor = new Color(0xb8,0xd4,0x97);
    
    
    private JCheckBoxMenuItem riderCountOption = null;		// rotate the board view
    private boolean showRiderCount=false;					// current state

    private boolean animating = false;
    private double animationStep = 0;
    private long lastAnimTime = 0;
 

    private Font gameLogBoldFont=null;
    private Font gameLogFont = null;
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// board etc.
    private static StockArt[] artwork = null;	// other board artwork

    // private state
    private int reviewPlayer = 0;
    private BreakingAwayBoard b = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private final double INITIAL_TILE_SCALE = 2.5;
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle sprintRect = addRect("sprintRect");
    private Rectangle currentRect = addRect("currentRect");
    private Rectangle forwardTimeRect = addRect("forwardTime");
    private Rectangle backwardTimeRect = addRect("backwardTIme");
    private Rectangle timeStepRect = addRect("TimeStep");
    private TextButton animateButton = addButton(AnimateMessage,BreakId.Animate,AnimateHelp,
    		HighlightColor,rackBackGroundColor);
    private TextButton dropButton = addButton(DropMessage,BreakId.DropRider,
    							null,          		
    							HighlightColor, rackBackGroundColor);
    private Rectangle bubbleRect = addRect("bubble");
    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;


    private Rectangle movementRect[] = addRect(".movement",6);
    private Rectangle cycleRect[] = addRect("cycle",6);
    private Rectangle chipRect[] = addRect("chip",6);

     
    public synchronized void preloadImages()
    {	BreakingAwayPiece.preloadImages(loader,ImageDir);
	    if (artwork == null)
	        { // note that dfor this to work correctly, the images and masks must be the same size.  
	          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
            textures = loader.load_images(ImageDir,TextureNames);
            images = loader.load_masked_images(ImageDir,ImageNames);
            artwork = StockArt.preLoadArt(loader,ImageDir,ArtNames,ArtScales);
	        }
	    gameIcon = BreakingAwayPiece.Icon;
    }
    private Slider sizeRect = null;
    public Color playerColor(int pla)
    {	BreakingAwayPiece chip = BreakingAwayPiece.getChip(b.getColorMap()[pla]);
    	return chip.color.realColor;
    }
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {   
        int randomv = info.getInt(OnlineConstants.RANDOMSEED,-1);
        int pl = Math.min(MAXPLAYERS,Math.max(MINPLAYERS,info.getInt(OnlineConstants.PLAYERS_IN_GAME,3)));
    
        super.init(info,frame);
        
                
        MouseDotColors = ChipColor.mouseColors();
        MouseColors = ChipColor.dotColors();
        
        riderCountOption = myFrame.addOption("show rider counts",false,deferredEvents);
        int FontHeight = standardFontSize();
        gameLogBoldFont = SystemFont.getFont(standardPlainFont(), SystemFont.Style.Bold, FontHeight+2);
        gameLogFont = SystemFont.getFont(standardPlainFont(),SystemFont.Style.Plain,FontHeight);
        sizeRect = addSlider(TileSizeMessage,s.get(RiderSize),BreakId.ZoomSlider);
        sizeRect.min=1.5;
        sizeRect.max=4.0;
        sizeRect.value=INITIAL_TILE_SCALE;
        sizeRect.barColor=ZoomColor;
        sizeRect.highlightColor = HighlightColor;
        b = new BreakingAwayBoard(info.getString(GameInfo.GAMETYPE, Variation.Standard.name),
        			randomv,
        			pl,getStartingColorMap()
        			);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        // breakingAway isn't compatible with direct drawing, and apparently
        // not trivial to make it compatible.  Proceed with caution! 12/2021
        //useDirectDrawing();
        doInit(false);
        reviewPlayer = 0;
    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey,b.nPlayers());						// initialize the board
        reviewPlayer = 0;
        if(!preserve_history)
        	{
        	 sizeRect.setValue(INITIAL_TILE_SCALE);
        	 startFirstPlayer();
        	}
   }

    private Rectangle createPlayerGroup(commonPlayer pl0,int x,int y,
    		Rectangle discRect,Rectangle crect,boolean wideMode,Rectangle moveRect,int unitsize)
    	{
    	Rectangle box = pl0.createRectangularPictureGroup(x+unitsize*6,y,unitsize);
  	
    	G.SetRect(crect, x,y, unitsize*6, unitsize*6);
    	int boxR = G.Right(box);
    	G.SetRect(discRect, boxR-unitsize*2,G.Bottom(box),unitsize*2,unitsize*2);
    	G.SetRect(moveRect, boxR,y,unitsize*12, unitsize*6);
    	G.union(box, discRect,moveRect,crect);
 
    	return(box);
    }
 
    public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit)
    {
    	commonPlayer pl0 = getPlayerOrTemp(player);
    	Rectangle box = createPlayerGroup(pl0,x,y,chipRect[player],cycleRect[player],
        		true,   		movementRect[player],unit);
    	pl0.displayRotation = rotation;
    	return(box);
    	}
    /**
     * calculate a metric for one of three layouts, "normal" "wide" or "tall",
     * which should normally correspond to the area devoted to the actual board.
     * these don't have to be different, but devices with very rectangular
     * aspect ratios make "wide" and "tall" important.  
     * @param width
     * @param height
     * @param wideMode
     * @param tallMode
     * @return a metric corresponding to board size
     */
    public void setLocalBounds(int x,int y,int w,int h)
    {	G.SetRect(fullRect,x,y,w,h);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
    	int fh = standardFontSize();
       	int logW = fh*20;		// just a default
        int chatW = fh*30;	// just a default
        double cs = Math.min(w/50.0, w/25.0);
        int margin = fh/2;
    	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	layout.selectLayout(this, nPlayers, w,h,margin,(0.8-0.075*nPlayers),2.3,cs, 0.2);
    	commonPlayer pl0 = getPlayerOrTemp(0);
    	Rectangle playerBox = pl0.playerBox;
    	

    	int playerH = G.Height(playerBox);
        int logH = playerH;
        int chatHeight = selectChatHeight(h);
      
        layout.placeTheChatAndLog(chatRect,chatW,chatHeight,chatW*3/2,3*chatHeight/2,logRect,
        		logW,logH,logW*2,logH*3);

    	Rectangle main = layout.getMainRectangle();
  
        {
        int width = G.Width(main);
        int height = G.Height(main);
		CELLSIZE = Math.min(height/40,width/60);
        }
    	int C2 = CELLSIZE/2;
      	int stateH = fh*5/2;   
        int boardY = G.Top(main);
    	int boardH = G.Height(main)-stateH;
    	int boxW =G.Width(main);
    	int boardW = Math.min(boxW,(int)(boardH*2.0));
    	int extra = (boxW-boardW)/2;
    	int boardX = G.Left(main)+extra;
    	int BCELL = boardW/80;
    	int stateX = boardX;
    	layout.returnFromMain(extra,0);
    	G.SetRect(boardRect, boardX, boardY+stateH, boardW,boardH-stateH);
    	int stateY = boardY;
    	boolean rotate = boardH>boardW;
    	if(rotate)
    	{	// this conspires to rotate the drawing of the board
    		// and contents if the players are sitting opposite
    		// on the short side of the screen.
    		G.setRotation(boardRect,-Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
    

	placeRow(boardX, boardY+boardH,boardW, stateH,goalRect);
    
    setProgressRect(progressRect,goalRect);
      
    int bottomLine = boardY+boardH-7*BCELL/2;
	//this sets up the "vcr cluster" of forward and back controls.
    int vcrX = boardX+C2;
    int vcrY = boardY+boardH - 5*CELLSIZE-C2;
    SetupVcrRects(vcrX,
        vcrY, CELLSIZE * 10,
        5 * CELLSIZE);

    G.SetRect(sprintRect, Math.max(boardX+BCELL,G.Right(vcrZone)),bottomLine, BCELL*15, BCELL*2);

	placeStateRow(stateX, stateY,boardW,stateH,iconRect,stateRect,noChatRect);
	G.placeRight(stateRect, sizeRect ,CELLSIZE*12);

    int timeW = 2*BCELL;
    int timeY = G.Top(vcrZone)-timeW;
           
    G.SetRect(backwardTimeRect, G.Left(vcrZone),timeY,timeW,timeW);
    G.AlignTop(timeStepRect, G.Right(backwardTimeRect)+CELLSIZE/4,backwardTimeRect);
    G.AlignTop(forwardTimeRect, G.Right(timeStepRect)+CELLSIZE/4,timeStepRect);
    
    G.SetRect(animateButton,boardX+boardW-7*BCELL,boardY+BCELL*7, timeW*3, timeW);
    
    int revw = G.Width(animateButton)+BCELL*2;
    G.SetRect(reverseViewRect, G.Right(boardRect)-revw-BCELL,boardY+BCELL*2,
    			revw,BCELL*4);
    
    G.SetRect(bubbleRect, boardX+boardW-BCELL*4+BCELL/2, boardY+boardH-BCELL*4, BCELL*3,BCELL*3);
    int currentW = CELLSIZE*20;
    G.SetRect(currentRect, boardX+boardW-currentW-CELLSIZE/3,
    		boardY+boardH-6*CELLSIZE,
    		currentW, 6*CELLSIZE-C2);

    // "edit" rectangle, available in reviewers to switch to puzzle mode
    int editX = G.Left(currentRect)-15*BCELL;
    int editH = 3*BCELL;
    int editW = 8*BCELL;
    if(plannedSeating())
    {
    int undoW = editW*2/3;
    G.SetRect(editRect, editX, bottomLine-undoW/2, undoW, undoW);
    G.SetRect(doneRect, 0, 0, 0, 0);
    }
    else 
    {
    G.SetRect(editRect, editX, bottomLine,editW, editH);
    // "done" rectangle, should always be visible, but only active when a move is complete.
    G.AlignXY(doneRect,G.Left(editRect),bottomLine-editH-BCELL,editRect);
    }
    int dropW = BCELL*12;
    G.SetRect(dropButton,editX-dropW-BCELL,G.Top(doneRect),
    		dropW, 
    		editH);
    

    positionTheChat(chatRect,Color.white,Color.white);
    generalRefresh();
    	
    }

    public void drawSprite(Graphics g,int idx,int xp,int yp)
    {	boolean inboard = boardRect.contains(xp,yp);
       	int cellS = (int)(CELLSIZE*2.3*sizeRect.value);
       	BreakingAwayPiece p = BreakingAwayPiece.getChip(idx);
       	BreakingAwayPiece alt = p;
       	double comp = 1.0;
       	if(inboard)
       	{
       	int closestRow = b.closestRow(xp-G.Left(boardRect),(yp-G.Top(boardRect)));
       	if(closestRow>=0)
       	{
        comp = (100-b.rowCompress(closestRow))/100.0;
        alt = p.getAltAngle(b.rowCycleIndex(closestRow));
      	}
       	}
       	alt.drawChip(g,this,cellS,comp,xp,yp,null);
    }
    
    Image background = null;
    Image scaled = null;
    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean backgroundReview = reviewMode() && !mutable_game_record;
    
    BreakingAwayBoard gb = disB(gc);
      GC.setColor(gc,backgroundReview ? reviewModeBackground : boardBackgroundColor);
      textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);   
      GC.setRotatedContext(gc,boardRect,null,contextRotation);    
      if(remoteViewer<0) 
      	{ 
    	  textures[backgroundReview ? BROWN_FELT_INDEX:YELLOW_FELT_INDEX].tileImage(gc,
    	          		boardRect); 
    	  Image boardImage = images[(gb.reverseY()?BOARD_OBLIQUE_REVERSE_INDEX:BOARD_OBLIQUE_INDEX)];
    	  if(boardImage!=background) { scaled = null; }
    	  background = boardImage;
    	  scaled = boardImage.centerScaledImage(gc, boardRect,scaled);
  		
      	}
  		
      gb.SetDisplayRectangle(boardRect);
    
      GC.unsetRotatedContext(gc,null);
      // visualize the center lines of the track wedges
     // gc.setColor(Color.blue);
     // for(int i=0;i<BreakingAwayBoard.inner_points.length;i++)
     // {	int ix = gb.BCtoX('A',i);
     //   int iy = gb.BCtoY('A',i);
     //   int ox = gb.BCtoX('Z',i);
     //   int oy = gb.BCtoY('Z',i);
     //   gc.drawLine(tbRect.x+ix, tbRect.y+iy, tbRect.x+ox, tbRect.y+oy);
     // }
      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method
      //gb.DrawGrid(gc, tbRect, use_grid, boardBackgroundColor, Color.blue, Color.blue,Color.black);

     }

    /* draw the board and the chips on it. */
      private void drawBoardElements(Graphics gc, BreakingAwayBoard gb, Rectangle tbRect,HitPoint ourTurnSelect,HitPoint anySelect)
     {	
         Rectangle oldClip = GC.combinedClip(gc,tbRect);
     	 boolean canHit = G.pointInRect(ourTurnSelect,tbRect);
         int csize = (int)(CELLSIZE*2.3*sizeRect.value);
         HitPoint infoSelect = (anySelect!=null) ? new HitPoint(G.Left(anySelect),G.Top(anySelect)) : null; 
  
         if(animating && (animationStep<gb.maxTimeStep))
         {
         long now = G.Date();
         if(now-lastAnimTime>100)
         	{animationStep += 0.1;
        	 lastAnimTime = now;
         	}
         repaint(20);
         } 
         else 
         { animating = false; 
         }
         
      	//
        // now draw the contents of the board and anything it is pointing at
        //
    	 Hashtable<BreakingAwayCell,BreakingAwayCell> dests = gb.getDests();
    	 Hashtable<BreakingAwayCell,BreakingAwayCell> sources = gb.getSources();

          BreakingAwayCell sourceCell = gb.sourceCell();
          BreakingAwayCell destCell = gb.destCell();
          // precalculate the spanning size of the board.
          //System.out.println("cs "+cs/CELLSIZE+ " "+cs+" "+CELLSIZE);
          if(ourTurnSelect!=null) {  ourTurnSelect.awidth = CELLSIZE*2; }
         
          BreakingAwayCell riders[] = b.allRidersByY;
          for(int i=0;i<riders.length;i++)
          {	BreakingAwayCell rider=riders[i];
          	if(animating)
          	{
          	char prevCol = rider.colAtTime(animationStep);
          	char col = rider.colAtTime(animationStep+1); 
          	int  prevRow = rider.rowAtTime(animationStep);
          	int  row0 = rider.rowAtTime(animationStep+1);
          	double frac = animationStep-(int)animationStep;
           	double rowFl = G.interpolateD(frac,prevRow,row0);
           	int row = (int)rowFl;
          	char colF = (char)G.interpolateD(frac,prevCol+0.0,col+0.0);
          	// select a vacant column to avoid flicker
          	boolean dupcol = false;
          	do{	dupcol = false;
          		for(int j=i-1; !dupcol && (j>=0); j--)
          		{	BreakingAwayCell prev = riders[j];
          			if( (prev.animRow<=(rowFl+0.75))
          					&& (prev.animRow>=(rowFl-0.75))
          					&& (prev.animCol==colF))
          			{ dupcol=true; 
          			  if((rider.animCol<(colF+2))
          					  && (rider.animCol>colF-2)) 
          			  	{ colF = rider.animCol; 
          			  	  rider.animCol=0;
          			  	}
          			  else { colF++; }
          			}
          		}
          	} while(dupcol);
          	int prevxpos = gb.BCtoX(colF,row);
           	int xpos = gb.BCtoX(colF,row+1);
           	int prevypos = gb.BCtoY(colF,row);
           	int ypos = gb.BCtoY(colF,row+1);
          	rider.xpos = G.interpolate(rowFl-row,prevxpos,xpos);
          	rider.ypos = G.interpolate(rowFl-row,prevypos,ypos);
          	rider.animRow = row;
          	rider.animCol = colF;
          	}
          	else
          	{
          	int x = gb.BCtoX(rider.col,rider.row);
          	int y = gb.BCtoY(rider.col,rider.row);
          	rider.xpos = x;
          	rider.ypos = y;
          	rider.animRow = rider.row;
          	}
          }
          Sort.sort(riders);		// sort the riders by Y so we can draw them back to front
          if(animating)
          {	BreakingAwayCell prev = riders[0];
          	for(int i=1,lim=riders.length;i<lim;i++)
          	{	BreakingAwayCell rider = riders[i];
          		if(rider.ypos<=prev.ypos) 
          		{ if(rider.player*4+rider.index < prev.player*4+prev.index)
          			{ int pp = prev.ypos;
          			  prev.ypos = rider.ypos;
          			  rider.ypos=pp+1;
          			}
          		else { rider.ypos = prev.ypos+1;
          		}
          		}
          		else
          		{
          		prev = rider;
          		}
          	}
          }
          for(int elem = 0;elem<riders.length;elem++)
          {	 ;
        	 BreakingAwayCell rider = riders[elem];
        	 char thisCol = rider.col;
        	 int thisRow = rider.row;
             int xpos = G.Left(tbRect)+rider.xpos;
             int ypos = G.Top(tbRect)+rider.ypos;
             boolean isAsource = sources.get(rider)!=null;
             boolean isADest = dests.get(rider)!=null;
             boolean hasHitThis = false;
             //boolean isASource = (ccell==sourceCell)||(ccell==destCell) || (sources.get(ccell)!=null);
             boolean canHitThis = canHit && gb.LegalToHitBoard(rider);
             String labl = "";
             if( G.debug() && !use_grid) { labl+=""+thisCol+thisRow; }
             BreakingAwayPiece top = rider.topChip();
             if(top!=null)
             {
             double comp = (100-b.rowCompress(rider.animRow))/100.0;
             BreakingAwayPiece alt = top.getAltAngle(b.rowCycleIndex(rider.animRow));
 
             if(infoSelect!=null)
             {
            	 if(rider.findChipHighlight(infoSelect,alt,csize,csize,xpos,ypos))
            	 {	anySelect.setHelpText(top.file+"#"+(rider.index+1)+" @ "+rider.row+" "+rider.xpos+","+rider.ypos);
            	 }
             }
             if(isAsource && !animating)
             {	StockArt.SmallO.drawChip(gc,this,csize*3,xpos,ypos,labl);
             }
             if(rider.drawChip(gc,alt,this,canHitThis?ourTurnSelect:null,csize,comp,xpos,ypos,labl)
            		 || hasHitThis)
                  {	 //if(gc!=null) { gc.drawOval(xpos-cellSize/2,ypos-cellSize/2,cellSize,cellSize); }
                      boolean isEmpty = top==null;
                      ourTurnSelect.awidth = CELLSIZE*2;
                      ourTurnSelect.hitCode = BreakId.BoardLocation;
                      ourTurnSelect.hitObject = rider;
                      ourTurnSelect.arrow = isEmpty?StockArt.DownArrow:StockArt.UpArrow;
                      canHit = false;
                  }
                  if (gc != null)
                  {	if(rider.dropped) 
                  		{ StockArt.SmallX.drawChip(gc,this,csize/2+1,xpos,ypos,null);
                  		}
                      if((sourceCell==rider)||(destCell==rider))
                      {
                    	  StockArt.Dot.drawChip(gc,this,csize/2+1,xpos,ypos,null);  
                      }

                 if(isADest)
                 {
                 	StockArt.SmallO.drawChip(gc,this,csize,xpos,ypos,labl);

                  	}
                  }
              }
          }
        // draw targets at possible destinations
        boolean wego = ((ourTurnSelect!=null)&&(gb.pickedSource!=null));
        if(wego|| showRiderCount)
        {
        BreakingAwayCell closestCell = null;
        BreakingAwayCell closest = null;
    	boolean puzzle = (gb.board_state==BreakState.PUZZLE_STATE);
        if(wego)
        {
        int xp = G.Left(ourTurnSelect)-G.Left(tbRect);
    	int yp = G.Top(ourTurnSelect)-G.Top(tbRect);
    	closest = gb.closestCellB(xp,yp);
    	if(puzzle) { closestCell = closest; } 
        if(closest!=null) { dests.put(closest,closest); }
        }
        
        if(wego)
        {// if really our turn, not just in always display mode
        for(Enumeration<BreakingAwayCell> e = dests.elements(); e.hasMoreElements();)
        {	BreakingAwayCell c = e.nextElement();
        	int xpos = gb.BCtoX(c.col,c.row);
        	int ypos = gb.BCtoY(c.col,c.row);
        	boolean isAHit = !puzzle 
        				&& (closest!=null)
        				&& (c!=closest)
        				&& ((c.row%TRACK_ROWS)==closest.row);
          	StockArt.SmallO.drawChip(gc,this,csize,G.Left(tbRect)+xpos,G.Top(tbRect)+ypos,null);
          	
          	// draw one at the outer rim
           	int xp1 = G.Left(tbRect)+gb.BCtoX('W',c.row);
        	int yp1 = G.Top(tbRect)+gb.BCtoY('W',c.row);
          		// only if our turn, not just in global display mode
          	StockArt.Dot.drawChip(gc,this,2*csize/3,xp1,yp1,null);
          	
      	
        	if(isAHit)
        	{	closestCell = c;
        	}
        	
         }}
        {	int leader = gb.riderLeading().row;
        	int destrow = (gb.droppedDest!=null) ? gb.droppedDest.row : -1;
        	for(int i=0;i<40 && leader>=0;i++,leader--)
        	{	int tot = gb.ridersInRow[leader];
        		if(tot>0)
        		{	int xpos = gb.BCtoX('Y',leader);
        			int ypos = gb.BCtoY('Y',leader);
        			StockArt.SmallO.drawChip(gc,this,leader==destrow?csize*2:csize,G.Left(tbRect)+xpos,G.Top(tbRect)+ypos,""+tot);
        		}
        	}
        	
        }
        if(closestCell!=null)
        	{
        	ourTurnSelect.hitObject = closestCell;
        	ourTurnSelect.hitCode = BreakId.BoardLocation;
        	ourTurnSelect.arrow = StockArt.DownArrow;
        	ourTurnSelect.awidth = CELLSIZE*2;
        	}
        }
        
   		GC.setClip(gc,oldClip);
      }
    private double[] textIconScale = new double[] {1,1.5,-0.1,-0.1};
    public Drawable getPlayerIcon(int p)
    {	playerTextIconScale = textIconScale;
    	return BreakingAwayPiece.getChip(b.getColorMap()[p]);
    }
    private void drawPlayerCycle(Graphics gc,int pla,BreakingAwayBoard gb,Rectangle r,HitPoint select)
    {	BreakingAwayPiece chip = BreakingAwayPiece.getChip(gb.getColorMap()[pla]);
    	int h22 = G.Height(r)/4;
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	if(gc!=null)
    	{
    	chip.drawChip(gc,this,G.Width(r),cx,cy,null);
    	GC.frameRect(gc,Color.black,G.Left(r),G.Top(r),h22,h22);
        GC.setFont(gc,standardBoldFont());
    	GC.Text(gc,true,G.Left(r),G.Top(r),h22,h22,Color.black,null,""+gb.scoreForPlayer[pla]);
    	}
    	if(select!=null)
    	{
    	if(pla==reviewPlayer) { GC.frameRect(gc,Color.black,r); }
    	if(G.pointInRect(select,r))
    	{	select.hitCode = BreakId.SelectRider;
    		select.spriteRect = r;
    		select.spriteColor = Color.red;
    		select.row = pla;
    		select.hitObject = gb.cycles[pla][0];
    	}}
    }
  
    private void drawSprintInfo(Graphics gc,BreakingAwayBoard gb,Rectangle r)
    {	String msg = null;
    	String msg2 = null;
    	int h2 = G.Height(r)/2;
    	if(gb.ridersAcrossFinish>0)
    	{	msg = s.get(FinishMessage,""+gb.ridersAcrossFinish);
			if(gb.ridersAcrossFinish<FINISHPOINTS.length)
				{msg2 = s.get(FinishBonusMessage,
						""+FINISHPOINTS[gb.ridersAcrossFinish]);}
    	}
    	else if(gb.ridersAcrossSprint2>0)
    	{	msg = s.get(SecondSprintMessage,
    				""+gb.ridersAcrossSprint2);
    		if(gb.ridersAcrossSprint2<SPRINTPOINTS.length)
    		{msg2 = s.get(SprintBonusMessage,
    					""+SPRINTPOINTS[gb.ridersAcrossSprint2]);
    		}

    	}
    	else if(gb.ridersAcrossSprint1>0)
    	{  msg = s.get(FirstSprintMessage,
				""+gb.ridersAcrossSprint1);
			if(gb.ridersAcrossSprint1<SPRINTPOINTS.length)
   			{ msg2 = s.get(SprintBonusMessage,
   					""+SPRINTPOINTS[gb.ridersAcrossSprint1]);
   			}
    	}
    	if(msg!=null)
    	{	GC.Text(gc,false,G.Left(r),G.Top(r),G.Width(r),h2,Color.black,null,msg);
    	}
    	if(msg2!=null)
    	{	GC.Text(gc,false,G.Left(r),G.Top(r)+h2,G.Width(r),h2,Color.black,null,msg2);
    	}
    }
    private void drawBubble(Graphics gc,HitPoint highlight,Rectangle r)
    {
    	Image im = textures[showRiderCount ? BUBBLE_INDEX : BUBBLE_INDEX+1];
    im.centerImage(gc, r);
    	boolean on = G.pointInRect(highlight,r);
    	Color c = on ? Color.red : Color.black;
    	GC.frameRect(gc,c,r);
    	if(on) { highlight.hitCode = BreakId.HitBubble; }
    	
    }
    
    private void drawPlayerMovements(Graphics gc,int pla,BreakingAwayBoard gb,Rectangle r,HitPoint adjust,
    									boolean hidden,HitPoint any,boolean drawChip,boolean drawClose)
    {  	if(G.pointInRect(adjust, r))
    		{ adjust.hitCode = DefaultId.HitNoWhere; 
    		  adjust.hitObject = null;
    		  adjust.spriteRect = null;
    		}
    	BreakingAwayCell cycles[] = gb.cycles[pla];
       	int nrows = cycles.length;
       	int left = G.Left(r);
       	int top = G.Top(r);
       	int right = G.Right(r);
       	int bottom = G.Bottom(r);
    	int xs = G.Width(r)/6;
    	int ys = G.Height(r)/nrows;
      	int buttonSize = xs/2; 
       	int yp = top;
       	GC.fillRect(gc,rackBackGroundColor,r);
       	GC.frameRect(gc, Color.black, r);
       	if(drawChip)
       	{
       	BreakingAwayPiece pchip = BreakingAwayPiece.getChip(b.getColorMap()[pla]);;
       	pchip.drawChip(gc,this,buttonSize*2,left-buttonSize,top+buttonSize*3,null);
       	}
     	for(int row = 0; row<nrows;row++)
    	{	int xp = left;
    		BreakingAwayCell cycle = cycles[row];
    		int movements[] = adjust!=null ? cycle.pendingMovements : cycle.movements;
    		int usedMovement = 0;
    		GC.Text(gc,true,xp,yp,xs*2,ys,Color.black,null,"#"+(row+1)+".."+cycle.row);
    		xp+=xs;
    		if(cycle==gb.droppedDest)
    		{	int picked = Math.max(0,gb.picked_row);
    			usedMovement = gb.droppedDest.row-picked;
    		}
     		for(int i=0;i<movements.length;i++)
    		{	int amount = movements[i];
    			xp += xs;
    			if((usedMovement>0)&&(amount==usedMovement))
    			{	amount = -amount;
    				usedMovement=0;
    			}
    			if(adjust!=null) 
    			{ GC.fillRect(gc,rackBackGroundColor,xp,yp,xs,ys);
    			}
    			GC.frameRect(gc,Color.black,xp,yp,xs,ys);
    			if(!hidden)
    			{
    			if(adjust!=null)
    			{	int size = xs/2;
    				int mx = xp+size/3;
    				int px = xp+xs-size/3;
    				int yy = yp+ys/2;
    				int min = (i==3) ? 0 : 1;	// row 4 can be 0, others must be 1
    				artwork[MINUSONE_INDEX].drawChip(gc,this,size,
    						mx,yy,null);
    				if((amount>min) && cycle.closestPointToCell(adjust, size,mx, yy))
    				{	// minus one square
    					adjust.hitCode = BreakId.MinusOne;
    					adjust.spriteColor = Color.red;
    					adjust.awidth = 2*size/3;
    					adjust.row = row;
    					adjust.col = (char)('A'+i);
    				}
    				artwork[PLUSONE_INDEX].drawChip(gc,this,size,
						px,yy,null);
       				if((amount < 15) && cycle.closestPointToCell(adjust,size, px, yy))
    				{	// plus square
    					adjust.hitCode = BreakId.PlusOne;
    					adjust.spriteColor = Color.red;
    					adjust.awidth = 2*size/3;
    					adjust.row = row;
    					adjust.col = (char)('A'+i);
    				}
    			}}
    			GC.Text(gc,true,xp,yp,xs,ys,amount<0 ? Color.blue : Color.black,null,hidden?"?":""+Math.abs(amount));
    			if(adjust!=null && drawClose)
    			{
    				if(StockArt.CloseBox.drawChip(gc, this, buttonSize,right-buttonSize,bottom-buttonSize,adjust,
    							BreakId.HidePlayerInfoButton,null))
    				{
    					any.hit_index = pla;
    				}
    			}
    		}
       		if(cycle==gb.allRidersInOrder[gb.riderTurnOrder])
    		{	GC.frameRect(gc,Color.blue,G.Left(r),yp,xs*5,ys);
    		}
    		yp += ys;
    		
    	}
    }
	
    private void DrawReverseMarker(Graphics gc, BreakingAwayBoard gb,Rectangle r,HitPoint highlight)
    {images[(gb.reverseY()?BOARD_OBLIQUE_INDEX:BOARD_OBLIQUE_REVERSE_INDEX)].centerImage(gc, r);
    	HitPoint.setHelpText(highlight,r,BreakId.ReverseViewButton,s.get(ReverseViewExplanation));	
     } 
    public Text censoredMoveText(SequenceElement sp,int idx)
    {	BreakingAwayMovespec next = ((idx+1<History.size())
    									? (BreakingAwayMovespec)History.elementAt(idx+1) 
    									: null);
    	return(((BreakingAwayMovespec)sp).shortMoveText(this,next));
    }

    private HitPoint activeDoneButton(HitPoint selectPos,HitPoint buttonSelect,BreakState state)
    {
    	return((b.DoneState() 
				? ((state==BreakState.ADJUST_MOVEMENT_STATE) 
						? b.doneAdjustingUI[getUIPlayerIndex()]?null:selectPos 
						: buttonSelect) 
				: null));
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {
    	BreakingAwayBoard gb = disB(gc);
    	
       boolean moving = hasMovingObject(selectPos);
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;
       HitPoint buttonSelect = moving?null:ourTurnSelect;
       HitPoint vcrSelect = (moving && !reviewMode()) ? null : selectPos;	// hit if not dragging
       boolean planned = plannedSeating();
       BreakState state = gb.getState();
       HitPoint activeDone = activeDoneButton(buttonSelect,selectPos,state);
       gameLog.playerIcons = true;
       gameLog.redrawGameLog2(gc, vcrSelect, logRect, Color.black,Color.lightGray,gameLogBoldFont,gameLogFont);
        
      	HitPoint adjusting = state==BreakState.ADJUST_MOVEMENT_STATE?selectPos:null;

        if(gb.nPlayers()>0)
        {
   		GC.setFont(gc,largeBoldFont()); 
   		if(canUseDone())
   		{	int pl = getUIPlayerIndex();
   			boolean show = planned ? b.visibleInMainUI[pl] : true;
   			
   	       	if(planned && !allowed_to_edit)
   	       	{
   	       	StockArt button = !show ? StockArt.Eye : StockArt.NoEye;
   	       	int left = G.Left(currentRect);
   	       	int top = G.Top(currentRect);
   	       	int buttonSize = G.Width(currentRect)/12;
   	       	button.drawChip(gc,this,3*buttonSize/2,left-buttonSize,
   	       			top+buttonSize,selectPos,BreakId.HideInfoButton,null);
   	       	}
   			drawPlayerMovements(gc,pl,gb,currentRect,adjusting,!show,selectPos,true,false);	
   		}
        // draw the player position and movement info first, so if the riders
        // happen to overlap they will be in front.
        //
		for(int i=0;i<gb.nPlayers();i++)
		{	commonPlayer pl = getPlayerOrTemp(i);
			pl.setRotatedContext(gc, selectPos, false);
			drawPlayerCycle(gc,i,gb,cycleRect[i],
				(state==BreakState.ADJUST_MOVEMENT_STATE)?selectPos:null);
			boolean showMovements = b.showMovements[i];
			if(!allowed_to_edit)
			{
	       	StockArt button = showMovements ? StockArt.NoEye : StockArt.Eye;
	       	if(isOfflineGame() && button.drawChip(gc,this,chipRect[i],selectPos,BreakId.HidePlayerInfoButton))
	       		{
	       		selectPos.hit_index = i;
	       		}
			}
			if(planned)
 			{	
 				boolean show =  !b.showMovements[i] && ((adjusting!=null) ? !b.doneAdjustingUI[i] : (i==getUIPlayerIndex()));
 				// we want to show the individual player's done button only when it
 				// is his turn, so it provides an additional cue that it's time to move
 				if(show)
 					{Rectangle mr = movementRect[i];
			int mrL = G.Left(mr);
			int mrT = G.Top(mr);
			int mrW = G.Width(mr);
			int mrH = G.Height(mr);
 					 GC.fillRect(gc,boardBackgroundColor,mr);
 					 Rectangle doner = new Rectangle(mrL+mrW/4,mrT+mrH/4,mrW/2,mrH/2);
					 HitPoint select = (adjusting!=null) ? selectPos : activeDone;
					 if(GC.handleRoundButton(gc, doner, select,
						adjusting!=null
							? s.get(PlayerDoneAdjusting) 
							: s.get(DoneAction), HighlightColor, rackBackGroundColor))
					 		{	select.hitCode = BreakId.PlayerDone;
					 			select.hit_index = i;
					 		}
					}
				}
			pl.setRotatedContext(gc, selectPos, true);
		}

		if(!animating) { drawSprintInfo(gc,gb,sprintRect); }
		drawPlayerStuff(gc,(state==BreakState.PUZZLE_STATE),vcrSelect,
				HighlightColor, rackBackGroundColor);
        }
      	
        GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);

        drawBoardElements(gc, gb, boardRect, ourTurnSelect,vcrSelect);
        
        GC.unsetRotatedContext(gc,selectPos);
        
        sizeRect.draw(gc,vcrSelect);
        
        drawBubble(gc,selectPos,bubbleRect);
        
        GC.setFont(gc,standardBoldFont());
        animateButton.show(gc,vcrSelect);
        DrawReverseMarker(gc,gb,reverseViewRect,vcrSelect);
        if ((state==BreakState.PLAY_STATE)||(state==BreakState.CONFIRM_DROP_STATE))
        {	BreakingAwayCell c = gb.riderToMove();
        	BreakingAwayCell l = gb.riderLeading();
        	int lead = l.row-c.row;
        	if (G.debug() || ((c.col>='O') && (lead>30))) 
        	{ 	dropButton.show(gc,buttonSelect);
        	}
        }
        if (state == BreakState.PUZZLE_STATE)
        {
            if (GC.handleRoundButton(gc, editRect, 
            		buttonSelect, s.get(AdjustMessage),
                    HighlightColor, rackBackGroundColor))
            {	// always display the done button, but only make it active in
            	// the appropriate states
            	buttonSelect.hitCode = BreakId.HitAdjustButton;
            }
        }

        if (state != BreakState.PUZZLE_STATE)
        {
        	if(allowed_to_edit || !planned) 
        		{ 
        		handleDoneButton(gc,doneRect,activeDone,HighlightColor, rackBackGroundColor);
            }
        	handleEditButton(gc,editRect,buttonSelect,selectPos,HighlightColor, rackBackGroundColor);
        }

		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

        int who = gb.whoseTurn;
        standardGameMessage(gc,messageRotation,
        		state==BreakState.GAMEOVER_STATE?gameOverMessage(gb):s.get(state.getDescription()),
        		state!=BreakState.PUZZLE_STATE,
        		who,stateRect);
        
        BreakingAwayPiece chip = BreakingAwayPiece.getChip(gb.getColorMap()[who]);
        chip.drawChip(gc, this, iconRect, null);
			
        goalAndProgressMessage(gc,vcrSelect,s.get(GoalMessage),progressRect, goalRect);
        GC.setFont(gc,standardBoldFont());
        

        if(artwork[BACKWARDTIME_INDEX].drawChip(gc,this,backwardTimeRect,vcrSelect,BreakId.BackwardTime))
        {	vcrSelect.hitObject = null;
        }
        if(artwork[FORWARDTIME_INDEX].drawChip(gc,this,forwardTimeRect,vcrSelect,BreakId.ForwardTime))
        {	vcrSelect.hitObject = null;
        }
        StockArt.VCRTick.drawChip(gc,this,timeStepRect,""+gb.timeStep);
         drawVcrGroup(vcrSelect, gc);

        // draw player movements when adjusting, possibly at double size
        // do this last so it appears on top of everything else
 		for(int i=0;i<gb.nPlayers();i++)
 		{	commonPlayer cpl = getPlayerOrTemp(i);
 			cpl.setRotatedContext(gc, selectPos, false);
 			boolean showMovements = b.showMovements[i];
 			double rot = cpl.displayRotation;
 			int px = G.centerX(cpl.playerBox);
 			Rectangle mr = movementRect[i];
 			int mrW = G.Width(mr);
 			int mrH = G.Height(mr);
 			int mrL = G.Left(mr);
 			int mrT = G.Top(mr);
 			if(showMovements)
 				{
 				GC.fillRect(gc,Color.gray,mr);
 				GC.frameRect(gc, Color.black, mr);
				if(adjusting!=null && !reviewMode())
 					{ int qt = G.rotationQuarterTurns(rot);
 					  int w = G.Width(fullRect);
 					  switch(qt)
 					{ 
 					  case 0:	// no rotation
 						 mrL = Math.min(mrL, w-mrW*2-mrW/20) ;
 						 mrT = Math.max(mrT, mrH*2+mrW/20);
 						 break;
					  case 3:	// right side
						  mrL = Math.min(mrL,mrL-mrW);
						  break;
 					  case 1:	// left side
 						  mrL = Math.min(mrL,mrL-mrW/2);
 						  break;
 					  case 2: 	// top upside down
 						  mrL = Math.min(mrL,px-mrW);
 						  break;
 					  default: break;
 						  
 					  }
 					  mr = new Rectangle(mrL,mrT-mrH-mrH/2,mrW*2,mrH*2); 
 					}
 				GC.setFont(gc,largeBoldFont()); 
 				drawPlayerMovements(gc,i,gb,mr, b.doneAdjustingUI[i] ? null : adjusting,false,selectPos,false,true); 
 				}
 			cpl.setRotatedContext(gc, selectPos, true);
 		}

        	
         drawHiddenWindows(gc,selectPos);

    }

    public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode replay)
    {	// the super method in commonCanvas is where the history is actually recorded

       	boolean val =  super.PerformAndTransmit(m,transmit,replay);
       	// 
       	// Important note:
       	// it's tempting to do "automove" here, but don't.  The move hasn't
       	// yet been transmitted and therefore the other players will receive
       	// the moves out of order if we generate any here.
         return(val);
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
        if(replay.animate) { playSounds(m); }
         return (true);
    }
     void playSounds(commonMove mm)
     {
    	 switch(mm.op)
    	 {
    	 case MOVE_MOVE:
    		 playASoundClip(light_drop,100);
         	/*$FALL-THROUGH$*/
    	 case MOVE_DROPB:
    	 case MOVE_PICKB:
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
        return (new BreakingAwayMovespec(st, player));
    }
    public commonMove EditHistory(commonMove m)
    {	switch(m.op) {
    		case MOVE_PLUS1:
    		case MOVE_MINUS1:
    			return(null);
    		default: break;
    	}
    	return(EditHistory(m,true));
    }


/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        
        if (hp.hitCode instanceof BreakId) // not dragging anything yet, so maybe start
        {
        BreakId hitObject = (BreakId)hp.hitCode;
		BreakingAwayCell cell = hitCell(hp);
		BreakingAwayPiece bug = (cell==null) ? null : cell.topChip();
		switch(hitObject)
	    {
	    default: break;
        case ZoomSlider:
 	    case BoardLocation:
	    	if(bug!=null)
	    	{
	    	PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+cell.player+" "+cell.index);
	    	}
	    	break;
        }

        }
    }
    private boolean doneAdjusting()
    {	if(isOfflineGame())
    	{
    	if(remoteViewer>=0) { return(false); }
    	for(int i=0;i<b.nPlayers();i++) { if(!b.doneAdjustingUI[i]) { return(false); }}
    	return(true);
    	}
    	else { return b.doneAdjustingUI[b.whoseTurn]; }
    }
    private void sendReady()
    {	PerformAndTransmit("Ready"+b.readyString(getUIPlayerIndex()));
    }
    private void seekTimeStep(int dif)
    {	int step = Math.max(0,b.timeStep+dif);
    	if(step<b.timeStep) { doWayBack(replayMode.Single); }
    	while(b.timeStep<step && reviewMode()) { doForwardStep(replayMode.Replay); }
    }
    
    // get the player index of the player looking at the UI.  
    // In review or pass-and-play this is the play whose turn it is
    // if multiplayer, it's the main player for this UI, no matter whose turn it is.
    public int getUIPlayerIndex()
    {
    	return(allowed_to_edit|isPassAndPlay()
    			? b.whoseTurn
    			: getActivePlayer().boardIndex ); 
    }
	/** 
	 * perform the standard buttons and run the chat, but without the "reset" logic
	 * @param hp
	 * @return true is the event was handled
	 */
    public boolean performStandardActions(HitPoint hp)
    {
    	if(performVcrButton(hp.hitCode, hp)) { return(true); }
    	else { 
    		return(performStandardButtons(hp.hitCode,hp));
    	}
    }
    // simultaneous turns only factor in the beginning set up, and we treat
    // them as synchronous - we make robot moves when their turn is set.
    public boolean allowRobotsToRun(commonPlayer pl)
    	{
    	if(simultaneousTurnsAllowed()
    			&& ((b.whoseTurn!=pl.boardIndex) 
				|| b.isDoneAdjusting(pl.boardIndex))) { return false; }

    	return(true);
    }

    /** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging(HitPoint hp)
    {	CellId id = hp.hitCode;
		BreakState state = b.getState();
		int remoteIndex = remoteWindowIndex(hp);
		int playerIndex = remoteIndex>=0 ? remoteIndex : getUIPlayerIndex();
		
		if ((state==BreakState.ADJUST_MOVEMENT_STATE) 
			&& (id==GameId.HitDoneButton))
			{	
			if(isOfflineGame())
				{
				PerformAndTransmit("DoneAdjust "+playerIndex+" "+b.readyString(playerIndex),isOfflineGame(),replayMode.Live);
			}
			else {
				b.doneAdjusting(playerIndex);
			}}
		else if (performStandardActions(hp)) {}
    	else if (id==DefaultId.HitNoWhere) {if(state!=BreakState.ADJUST_MOVEMENT_STATE) { performReset(); } }
    	else if (!(id instanceof BreakId)) { throw G.Error("Hit Unknown: %s", id);}
    	else {
        BreakId hitObject = (BreakId)id;
		BreakingAwayCell cell = hitCell(hp);
		BreakingAwayPiece bug = (cell==null) ? null : cell.topChip();
       	
		
		switch (hitObject)
        {
        default:
               throw  G.Error("Hit Unknown: %s", hitObject);
        case PlayerDone:
        	{
        		if (state==BreakState.ADJUST_MOVEMENT_STATE) 
        				{	PerformAndTransmit("DoneAdjust "+hp.hit_index+" "+b.readyString(hp.hit_index));
        				}
        		else { PerformAndTransmit("Done"); }
        	}
        	break;
        case HidePlayerInfoButton:
        	{
        	if(remoteIndex>=0) { b.showHiddenMovements[playerIndex] = !b.showHiddenMovements[playerIndex]; }
        	else {	b.showMovements[hp.hit_index] = !b.showMovements[hp.hit_index]; }
        	}
        	break;
        case HideInfoButton:
        	if(remoteIndex>=0) { b.visibleInHiddenUI[playerIndex] = !b.visibleInHiddenUI[playerIndex]; }
        	else { b.visibleInMainUI[playerIndex]=!b.visibleInMainUI[playerIndex]; }
        	break;
        case ReverseViewButton:
          	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
          	 generalRefresh();
          	 break;

        case BackwardTime:
        	seekTimeStep(-1);
        	break;
        case ForwardTime:
        	seekTimeStep(1);
        	break;
        case HitBubble:
        	showRiderCount = !showRiderCount;
        	riderCountOption.setState(showRiderCount);
        	break;
        case Animate:
        	animating = !animating;
        	animationStep = 0;
        	repaint(20);
        	break;
        case DropRider:
        	{
        	BreakingAwayCell c = b.riderToMove();
        	PerformAndTransmit("Drop "+c.player+" "+c.index);
        	}
        	break;
        case SelectRider:
        	// this is a purely local action
        	reviewPlayer = hp.row;
        	break;
        case PlusOne:
        case MinusOne:
        	{
        	int col = hp.col-'A';;
         	PerformAndTransmit(hitObject.name()+" "+cell.player+" "+cell.index+" "+col,isOfflineGame(),replayMode.Live);
        	}
        	break;
        case HitAdjustButton:
        	PerformAndTransmit("Adjust");
        	break;
        case ZoomSlider:
        	break;
        case BoardLocation:	// we hit an occupied part of the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
			{	if(cell!=null)
				{
				int obj = b.movingObjectIndex();
				if(obj>=0)
				{ PerformAndTransmit("Dropb "+cell.col+" "+cell.row+" "+cell.player+" "+cell.index);
				}
				else if(bug!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+ " "+cell.player+" "+cell.index);
				}
				}
				break;
			}}
			break;
        }}
    }

   	
    // return what will be the init type for the game
    public String gameType() { return(b.gametype+" "+b.randomKey+" "+b.nPlayers()); }
    
    public String sgfGameType() { return(BreakingAway_SGF); }
    public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int ran = his.intToken();
    	int np = his.intToken();
    	reviewPlayer = 0;
        b.doInit(token,ran,np);
        adjustPlayers(np);
     }

    public BoardProtocol getBoard()   {    return (b);   }
    

    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() { return(new BreakingAwayPlay()); }



    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 10/31/2025
     * 1061 files visited 0 problems
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
            	adjustPlayers(np);
            	reviewPlayer = 0;
                b.doInit(key,ran,np);
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
    
    public void ViewerRun(int wait)
    {	super.ViewerRun(wait);
      	if(!reviewMode() && OurMove())
   		{
   		switch(b.board_state)
   		{ case ADJUST_MOVEMENT_STATE:
   			// in the preliminary adjustment process, we're ready
   			// and it is our turn
   			if(doneAdjusting()) 
   				{ sendReady(); 
   				}
   			break;
   			// autoplay for riders who have finished or have been dropped
   			//
   			case PLAY_STATE:
   			{	BreakingAwayCell c = b.riderToMove();
   				if((c.row>FINISH_LINE) || c.dropped)
   				{
   				commonPlayer pl = getPlayer(c.player);
   				if(pl!=null && pl.robotRunning()==null)
   					{
   					PerformAndTransmit(b.randomMove());
   					PerformAndTransmit("Done");
   					}
   				}
   			}
   			break;
		default:
			break;
   		}
   		}
	
    }
    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);

       	if(target==reverseOption)
    	{
    	b.setReverseY(reverseOption.getState());
    	generalRefresh();
    	return(true);
    	}
       	else
       	if(target==riderCountOption)
        {	handled=true;
        	showRiderCount = riderCountOption.getState();
         	repaint(20);
        }

        return (handled);
    }
    

    /*
     * support for hidden windows, callback from the remote connection
     */
    
    
    // create the appropriate set of hidden windows for the players when
    // the number of players changes.
    public void adjustPlayers(int np)
    {
        int HiddenViewWidth = 600;
        int HiddenViewHeight = 300;
    	super.adjustPlayers(np);
        if(RpcService.isRpcServer() || VNCService.isVNCServer() || G.debug())
        {
        createHiddenWindows(np,HiddenViewWidth,HiddenViewHeight);
        }
    }
    
    public String nameHiddenWindow()
    {	
    	return (ServiceName);
    }

    public void drawHiddenWindow(Graphics gc,HitPoint hp,int index,Rectangle r)
    {	if(index>=nPlayers()) { return; }
    	String name = prettyName(index);
    	int h = G.Height(r);
    	int w = G.Width(r);
    	int l = G.Left(r);
    	int t = G.Top(r);
    	int topPart = h/10;
    	BreakState state = b.getState();
    	boolean adjust = (state==BreakState.ADJUST_MOVEMENT_STATE);
    	HitPoint select = (adjust && !b.doneAdjustingUI[index])
    							? hp
    							: null;
    	Font myfont = SystemFont.getFont(largeBoldFont(), topPart/2);
    	GC.setFont(gc, myfont);
    	if(remoteViewer<0) { GC.fillRect(gc,boardBackgroundColor,r); }
    	String msg = s.get(ServiceName,name);
    	GC.Text(gc, true,l,t,w,topPart,Color.black,null,msg);
		boolean showMovements = b.showHiddenMovements[index];
       	StockArt button = showMovements ? StockArt.NoEye : StockArt.Eye;
       	if(button.drawChip(gc,this,new Rectangle(l,t,w/10,w/10),hp,BreakId.HidePlayerInfoButton))
       		{
       		hp.hit_index = index;
       		}
       	
    	drawPlayerMovements(gc,index,b,new Rectangle(l+w/8,t+topPart,w-w/4,h-topPart),select,!showMovements,
    				hp,true,false);
    	Rectangle doner =new Rectangle(l+w-(int)(w*0.22),t+h-h/4,w/5,h/5);
    	if(select!=null)
    	{	
    		if(GC.handleRoundButton(gc, doner, select,
						s.get(PlayerDoneAdjusting),
						HighlightColor, rackBackGroundColor))
    			{	select.hitCode = BreakId.PlayerDone;
    				select.hit_index = index;
    			}    		
    	}
    	else if(!adjust && b.whoseTurn==index)
    	{
 			 GC.Text(gc, true, doner,
			Color.red,null,s.get(YourTurnMessage));
   	}
    }

    public HiddenGameWindow makeHiddenWindow(String name,int index,int width,int height)
	   	{
	   		return new HiddenGameWindow(name,index,this,width,height);
	   	}
 
    
    public String serverRecordString()
    {	String base = super.serverRecordString();
    	StringBuilder msg = new StringBuilder();
    	for(int i=0;i<b.nPlayers();i++)
    	{
    		msg.append(" allmovements ");
    		msg.append(i);
    		msg.append(" ");
    		msg.append(b.readyString(i));
    	}   	
    	return(base+msg.toString());
    
    }
    public void useRemoteStoryBuffer(String tok,Tokenizer his)
    {
    	super.useRemoteStoryBuffer(tok,his);
    	while (his.hasMoreTokens())
    	{
    		tok = his.nextToken();
    		if(tok.equals("allmovements"))
    		{
    			b.parseMovements(his);
    		}
    		else { G.Error("Unexpected initialization token ", tok); }
    	}
    }
    
}
