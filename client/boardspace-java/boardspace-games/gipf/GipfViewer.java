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
package gipf;


import java.awt.*;
import java.awt.Rectangle;
import javax.swing.JCheckBoxMenuItem;

import common.GameInfo;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

import java.util.*;

import lib.Graphics;
import lib.Image;
import lib.*;
/*
Change History

Nov 12 2004  Iniital work in progress, support for Gipf
*/
public class GipfViewer extends CCanvas<GipfCell,GipfBoard> implements GipfConstants
{	//public void verifyGameRecord(){}; // temporarily disable verify
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// things you can point at.  >=0 is a ball.
    static final Color reviewModeBackground = new Color(1.0f, 0.90f, 0.90f);
    static final Color ButtonColor = new Color(170,177,203);
    static final Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    static final Color GridColor = Color.black;
    static final Color rackBackGroundColor = new Color(170,177,203);
    static final Color boardBackgroundColor = new Color(210, 238, 255);
    static final Color chatBackGroundColor = new Color(220,220,253);
    // move commands, actions encoded by movespecs
    static final String Gipf_SGF = "gipf"; // sgf game number allocated for gipf
    static final String ImageDir = "/gipf/images/";
    
    static final int BOARD_INDEX = 0;
    static final int BOARD_FLAT_INDEX = 1;
    static final int MATRX_INDEX = 2;
    static final int MATRX_FLAT_INDEX = 3;
    static final String ImageFileNames[] = { "board","board-flat","matrx","matrx-flat" };
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final String TextureNames[] = 
    	{"background-tile" ,
  	  	"background-review-tile"};
    
    // vcr stuff
    static final Color vcrButtonColor = new Color(0.7f, 0.7f, 0.75f);
    static Image []images=null;
    static Image []textures=null;
    
    // private state
    GipfBoard b = null; //the board from which we are displaying
    public int CELLSIZE; //size of the layout cell
    private Rectangle reserveRects[] =  addRect("reserve",2);
    private Rectangle captureRects[] = addRect("capture",2);
    private Rectangle chipRects[] = addRect("chip",2);
    public void verifyGameRecord()
    {	//DISABLE_VERIFY = true;
    	super.verifyGameRecord();
    	
    }
    private Rectangle standardRects[] = addRect("standard",2);
    boolean usePerspective() { return(super.getAltChipset()==0); }
    boolean isMatrx() { return (b.variation==Variation.Gipf_Matrx); }
    public int getAltChipset()
    {	// matrx pieces are 2 and 3, regular pieces are 0 and 1
    	return (isMatrx() ? 2 : 0) + (usePerspective() ? 1 : 0);
    }
    // this is a debugging hack to print the robot's evaluation of the
    // current position.  Not important in real play
    JCheckBoxMenuItem startEvaluator = null;

    public BoardProtocol getBoard()   {    return (b);   }


    /** this is used by the game controller to supply entertainment strings to the lobby */
    public String gameProgressString()
    {	// this is what the standard method does
    	// return ((reviewer ? s.get(Reviewing) : ("" + viewMove)));
    	return(super.gameProgressString()+" "+b.playerState(0)+" "+b.playerState(1));
    }


    public synchronized void preloadImages()
    {	GipfChip.preloadImages(loader,ImageDir);
    	if(images==null)
    	{
    	textures = loader.load_images(ImageDir,TextureNames);
    	images = loader.load_masked_images(ImageDir,ImageFileNames);
    	}
    	gameIcon = images[BOARD_INDEX];
    }
    
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	enableAutoDone = false;		// not suitable becuase of the remove phase
        super.init(info,frame);
        
        if (extraactions)
        {
            startEvaluator = myFrame.addOption("Start Evaluator", false,deferredEvents);
        }
        String name =info.getString(GameInfo.GAMENAME,"Gipf");
        String name2 = "matrx".equals(name) ? "gipf-matrx" : info.getString(GameInfo.GAMETYPE,"gipf");
        b = new GipfBoard(name2,getStartingColorMap(),GipfBoard.REVISION);
        useDirectDrawing(true);
        doInit(false);
    }

    /* used when loading a new game */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);
        b.doInit(b.gametype);
        if(!preserve_history)
    	{ startFirstPlayer();
    	  removeIndexes = false;
    	}
    }

    public commonMove EditHistory(commonMove m)
    {	boolean okno = ((m.op==MOVE_REMOVE));
  	  GipfState state = b.getState();
   	  if((m.op==MOVE_DROPB)
    			&&((state==GipfState.SLIDE_STATE)|(state==GipfState.SLIDE_GIPF_STATE))
    		)
    	{ GipfCell dest = b.getDest();
    	  GipfCell src = b.getSource();
    	  int sz = History.size();
    	  if((sz>=2) && (dest!=null) && dest.isEdgeCell() && (src!=null) && (src.isEdgeCell()))
    	  {	{
    		commonMove m1 = History.elementAt(sz-1);
    	   	commonMove m2 = History.elementAt(sz-2);
    	  	if((m1.op==MOVE_PICKB)&&(m2.op==MOVE_DROPB))
    	  	{	// this handles drop-on-edge, pick, drop-on-edge.
    	  		popHistoryElement();
    	  		popHistoryElement();
    	  		sz -= 2;
    	  		// if the sequence is drop on edge, pick, drop-inside, pick, drop-on-edge, the pick and second drop
    	  		// have just been deleted, and the current drop is a duplicate of the original drop.  oksame makes it go away too.
     	  	}}
	  		{Gipfmovespec m3 = (Gipfmovespec)History.elementAt(sz-1);
	  		if(m3.op==MOVE_DROPB)
	  			{ GipfCell c3 = b.getCell(m3.to_col,m3.to_row);;
	  			  if(c3.isEdgeCell())
	  			  	{ popHistoryElement(); }
	  			}}

    	  }
    	}
    	return(EditHistory(m,okno));
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
    	int vcrW = fh*16;
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			1.0,	// aspect ratio for the board
    			fh*3,	// maximum cell size
    			0.3		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	layout.placeTheVcr(this,vcrW,vcrW*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
 
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	int ncols = b.ncols;
    	int nrows = b.nrows;
    	double cs = Math.min((double)mainW/(ncols+2),(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
        int boardW = CELLSIZE * (ncols + 2);
        int boardH = CELLSIZE * nrows;
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
        placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,numberMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow( boardX, boardBottom-stateH,boardW,stateH,goalRect,viewsetRect);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackGroundColor,rackBackGroundColor);
        if(isMatrx()) { CELLSIZE = CELLSIZE*9/10; }
        return boardW*boardH;
    }
    public int cellSize() { return b.cellSize()*2/3; }
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	int chipw = unitsize*2;
    	boolean matrx = isMatrx();
        Rectangle box = pl.createRectangularPictureGroup(x+chipw+unitsize/2,y,unitsize);
        Rectangle chip = chipRects[player];
        Rectangle player1Reserve = reserveRects[player];
        Rectangle player1Captures = captureRects[player];
    	Rectangle done = doneRects[player];
    	Rectangle standard = standardRects[player];
    	boolean perspective = usePerspective();
    	int doneW = plannedSeating()? unitsize*5 : 0;
    	int CELLSIZE = unitsize*3;
    	int rbox = G.Right(box);
    	G.SetRect(done,rbox+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
    	int bbox = G.Bottom(box);
    	int boxh = unitsize*3/2;
    	G.SetRect(standard, G.Left(box), bbox,G.Width(box),boxh);
    	G.SetRect(chip, x, y, chipw, chipw);
    	bbox+= boxh;
        // in perspective mode, tall columns of chips
    	G.union(box, done);
    	int bx = G.Right(box)+unitsize/2;
    	if(perspective)
    	{
    	if(flatten)
    	{	
    		G.SetRect(player1Reserve,
    	        		bx,
    	        		y, 
    	        		CELLSIZE*(matrx?2:1),
    	        		CELLSIZE*2);          
    		G.SetRect(player1Captures, 
    					G.Right(player1Reserve),
    	        		y,
    	        		CELLSIZE,CELLSIZE*2);
   	
    	}
    	else 
    	{
        G.SetRect(player1Reserve,
        		x,
        		bbox, 
        		CELLSIZE*(matrx?2:1),
        		CELLSIZE*2);          
        G.SetRect(player1Captures, 
        		G.Right(player1Reserve)+unitsize,
        		G.Top( player1Reserve),
        		CELLSIZE,CELLSIZE*2);
    	}}
    	else
    	{
    	if(flatten)
    	{
    		G.SetRect(player1Reserve,
   	        		bx,
   	        		y, 
   	        		CELLSIZE*4,
   	        		CELLSIZE*(matrx?2:1));          
   	        G.SetRect(player1Captures, 
   	        		bx,
   	        		G.Bottom( player1Reserve)+unitsize,
   	        		CELLSIZE*4,
   	        		CELLSIZE);
 		
    	}
    	else
    	{
    	       G.SetRect(player1Reserve,
    	        		x,
    	        		bbox, 
    	        		CELLSIZE*4,
    	        		CELLSIZE*(matrx?2:1));          
    	        G.SetRect(player1Captures, 
    	        		x,
    	        		G.Bottom( player1Reserve)+unitsize,
    	        		CELLSIZE*4,CELLSIZE);
    	}		
    	}
    	G.union(box, done,player1Reserve,player1Captures,chip,standard);
    	pl.displayRotation = rotation;
    	return(box);
    }   	

 
    public void drawTile(Graphics g, int X, int Y, int radius, String contents,
        Color fillColor, Color bgcolor, Color textColor)
    {
        drawBlankTile(g, X, Y, radius, fillColor, bgcolor);

        if (contents != null)
        {
            GC.Text(g, true, X - radius, Y - radius, radius * 2, radius * 2,
                textColor, null, contents);
        }
    }

    GipfCell dummyCell = new GipfCell(null,GipfId.NoHit,-1);
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {	dummyCell.reInit();
    	GipfChip chip = GipfChip.getChip(obj);
    	dummyCell.addChip(chip);
    	if(b.pickedHeight>1) { dummyCell.addChip(chip); }
    	dummyCell.drawStack(g,this,null,CELLSIZE,xp,yp,0,0.2,null);
    }

    Image background = null;
    Image scaled = null;
    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      // erase
      GipfBoard gb = disB(gc);
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid
      boolean perspective = usePerspective();
      Image board = images[isMatrx() 
    		  				? perspective ? MATRX_INDEX : MATRX_FLAT_INDEX 
    		  				: perspective ? BOARD_INDEX : BOARD_FLAT_INDEX];
      if(board!=background) { scaled = null; }
      background = board;
      scaled = board.centerScaledImage(gc, boardRect,scaled);
      setDisplayParameters(gb,perspective);
      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    private void setDisplayParameters(GipfBoard gb,boolean perspective)
    {
        if(isMatrx())
        {
            if(perspective)
            {
            gb.SetDisplayParameters(
      	    		0.775,	// x scale 
      	    		 0.96,	// y scale
      	    		 0.26,	// x offset
      	    		 -0.52,	// y offset
      	    		 22.0,	// rotation
      	    		 0.28,	// x perspective
      	    		 0.19,	// y perspective
      	    		 -0.00	// y as a function of x
      	    		 );
            }
            else {
                gb.SetDisplayParameters(
        	    		1.01,	// x scale 
        	    		0.98,	// y scale
        	    		 0.0,	// x offset
        	    		 -0.60,	// y offset
        	    		021,	// rotation
        	    		0,	// x perspective
        	    		0,	// y perspective
        	    		0	// y as a function of x
        	    		 );

            }     	  
        }
        else
        {
        if(perspective)
        {
        gb.SetDisplayParameters(
  	    		0.895,	// x scale 
  	    		 0.90,	// y scale
  	    		 0.17,	// x offset
  	    		 -0.68,	// y offset
  	    		13.1,	// rotation
  	    		 0.19,	// x perspective
  	    		 0.15,	// y perspective
  	    		 -0.084	// y as a function of x
  	    		 );
        }
        else {
            gb.SetDisplayParameters(
    	    		1.06,	// x scale 
    	    		 0.89,	// y scale
    	    		 -0.05,	// x offset
    	    		 -0.48,	// y offset
    	    		0,	// rotation
    	    		0,	// x perspective
    	    		0,	// y perspective
    	    		0	// y as a function of x
    	    		 );

        }}
        
    	 gb.SetDisplayRectangle(boardRect);

    }
    private void drawRack(Graphics gc,Rectangle r,HitPoint p,GipfBoard rb,GipfCell rack[],HitPoint any)
    {	// draw for matrx reserve
    	int w = G.Width(r);
    	int h = G.Height(r);
    	int x = G.Left(r);
    	int y = G.Top(r);
    	int n = rack.length;
    	int xsteps = w>h ? n/2 : 2;
    	int ysteps = w>h ? 2 : n/2;
    	double xstep = w/xsteps;
    	double ystep = h/ysteps;
    	int i=0;
    	for(int xi=0;xi<xsteps;xi++)
    	{
    	for(int yi=0;yi<ysteps;yi++)
    	{
    		Rectangle r1 = new Rectangle((int)(x+xstep*xi),(int)(y+ystep*yi),w/xsteps,h/ysteps);
    		drawRack(gc,r1,p,rb,rack[i],rack[i].height()>0?rack[i].potential.menuItem():null,any);
    		i++;
    	}
    	// also draw the gipf pieces on the board, if there are some
    	GipfCell gipf = rack[Potential.None.ordinal()];
    	GipfChip top = gipf.topChip();
    	if(top!=null)
    	{	boolean canhit = rb.legalToHitChips(gipf);
    		boolean perspective = usePerspective();
    		double position[] = top.color==GColor.B 
    				? perspective ? new double[] { 0.18,0.21,-Math.PI*0.15} : new double[] { 0.16,0.18,-Math.PI*0.1} 
    				: perspective ? new double[] { 0.76,0.71,Math.PI*0.55} : new double[] { 0.77,0.84,Math.PI*0.55};
    		int xp= G.interpolate(position[0],G.Left(boardRect),G.Right(boardRect));
    		int yp= G.interpolate(position[1],G.Top(boardRect),G.Bottom(boardRect));
    		double xs = Math.sin(position[2]);
    		double ys = Math.cos(position[2]);
    		drawStack(gc,gipf,canhit?p:null,
    				(int)(CELLSIZE*(perspective && position[1]<0.5?0.85:1)),xp,yp,xs/3,ys/3);
    		
    	}
    	}
    }
    private void drawRack(Graphics gc,Rectangle r,HitPoint p,GipfBoard rb,GipfCell rack,String tip,HitPoint any)
    {	boolean canhit = rb.legalToHitChips(rack);
		int w = G.Width(r);
		int h = G.Height(r);
		boolean vertical = w<h;
		boolean matrx = isMatrx() ;
    	int cs = Math.min((int)((matrx ? 1 : 0.8) * Math.min(h,w)),CELLSIZE);
    	int xp = vertical ? G.centerX(r) : G.Left(r)+cs/2;
    	int yp = vertical ? G.Bottom(r)-cs/2 : G.centerY(r);
    	double sz = rack.height();
    	double scl = sz==0 ? 1 : (sz*sz*7);
    	drawStack(gc,rack,canhit?p:null,cs,xp,yp,
    			vertical ? 0 : Math.min((w-cs)/scl,0.2),
    			vertical ? Math.min((h-cs)/scl,0.2) : 0);
    	HitPoint.setHelpText(any,r,tip);
    }
    private void drawStack(Graphics gc,GipfCell rack,HitPoint hp,int cs,int xp,int yp,double xscale,double yscale)
    {
    	boolean hit = rack.drawStack(gc,this,hp,cs,xp,yp,0,
    			xscale,
    			yscale,
    			null);
        if(hit)
        {
        hp.arrow = hasMovingObject(hp) ? StockArt.DownArrow : StockArt.UpArrow;;
        hp.awidth = CELLSIZE/2;
        hp.spriteColor = Color.red;
        }
    }

    private int scaleCellSize(int cellsize,int x,int y,Rectangle r)
    {	if(usePerspective() && G.pointInRect(x,y,r))
    	{ double scl = (((y-G.Top(r))*0.2)/G.Height(r))+0.8;
    	  int cs = (int)(cellsize*scl);
    	  return(cs);
    	}
      return(cellsize);
     }
    

    /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, GipfBoard rb, Rectangle brect, HitPoint highlight,HitPoint any)
    {	//Hashtable dests = null;//rb.getMoveDests();
    	setDisplayParameters(rb,usePerspective());
     	int dotsize = Math.max(2,CELLSIZE/15);
     	Hashtable<GipfCell,GipfCell> dests = rb.getMoveDests();
     	GipfState state = rb.getState();
     	numberMenu.clearSequenceNumbers();
        // now draw the contents of the board and anything it is pointing at
        //
     	GipfCell hitCell = null;
     	boolean perspective = usePerspective();
     	boolean picked = rb.pickedObject!=null;
     	boolean matrx = isMatrx();
       	Enumeration<GipfCell>cells = rb.getIterator(Itype.TBRL);
       	while(cells.hasMoreElements())
       	{
       		GipfCell c = cells.nextElement();
            boolean isADest = dests.get(c)!=null;
            boolean canHit = rb.legalToHitBoard(c,dests);
            boolean isCaptured = c.isMarkedForCapture();
            int ypos = G.Bottom(brect) - rb.cellToY(c);
            int xpos = G.Left(brect) + rb.cellToX(c);
            numberMenu.saveSequenceNumber(c,xpos,ypos);
            int cs = perspective ? scaleCellSize(CELLSIZE,xpos,ypos,brect) : CELLSIZE;
            //StockArt.SmallO.drawChip(gc, this, CELLSIZE, xpos, ypos,null);   
            if(matrx)
            {
            	 c.drawStack(gc,this,canHit?highlight:null,cs,xpos,ypos,0,
                 		perspective ? 0 : 0.1 ,
                 		perspective ? 0.15 :0.15,
                 		c.preserved ? "P" : null);
            }
            else
            {	c.drawStack(gc,this,canHit?highlight:null,cs,xpos,ypos,0,
            			perspective ? 0 : 0.2,
                		perspective ? 0.2 : 0,
                		c.preserved ? "P" : null);
            }
           
            String help = c.height()==1 || c.isGipf() ? c.topChip().potential.menuItem() : null;
            if(help!=null) { HitPoint.setHelpText(any,cs,xpos,ypos,help);}
            if((highlight!=null) && (highlight.hitObject==c))
            {
            hitCell = c;
            }
          // temp for grid setup
          //GC.DrawAACircle(gc,xpos+dotsize,ypos,dotsize,Color.red,Color.gray,true);
          if(isCaptured)
           	{	
           		if(state==GipfState.DONE_CAPTURE_STATE)
           		{StockArt.SmallX.drawChip(gc,this,7*CELLSIZE/8,xpos,ypos,null);
           		}
           		else
           		{StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
           		}
           	}
           	if(picked && isADest)
	       	{
	       		GC.cacheAACircle(gc,xpos+dotsize,ypos,dotsize,Color.red,Color.gray,true);
	       	}
           	// draw ghost cells of the recently captured
            if((c.topChip()==null)
        			&& c.lastContents!=null 
        			&& c.lastCaptured>0
        			 )
                	{	int vis = numberMenu.getVisibleNumber(c.lastCaptured);
                		if(vis>0)
                		{
            			c.lastContents.drawChip(gc,this,CELLSIZE/2,xpos,ypos,numberMenu.moveNumberString(vis));
                		StockArt.SmallX.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
                		}
                	}
        }
       	if(hitCell!=null)
       	{
            highlight.arrow = (hasMovingObject(highlight)||(hitCell.topChip()==null))
					? StockArt.DownArrow 
					: StockArt.UpArrow;
            highlight.awidth = CELLSIZE/2;
            highlight.spriteColor = Color.red;
       	}
       	numberMenu.drawSequenceNumbers(gc,CELLSIZE*2/3,labelFont,labelColor);
     }
    
   
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  GipfBoard gb = disB(gc);
           
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      GipfState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
     
     drawBoardElements(gc, gb, boardRect, ot,highlight);
     
     GC.setFont(gc,standardBoldFont());
     boolean planned = plannedSeating();
     boolean matrx = isMatrx();
     for(int player=0;player<2;player++)
    	{ commonPlayer pl = getPlayerOrTemp(player);
    	  pl.setRotatedContext(gc, select,false);
    	String count = matrx ? ""+gb.gipfsOnBoard(player) : null;
    	gb.playerChip[player].drawChip(gc, this, chipRects[player],count);
    	if(isMatrx())
    		{ 
    		drawRack(gc,reserveRects[player],ot,gb,gb.rack[player],highlight);
    		}
    		else {
    			drawRack(gc,reserveRects[player],ot,gb,gb.rack[player][Potential.None.ordinal()],null,highlight);
    		}
    	drawRack(gc,captureRects[player],ot,gb,gb.captures[player],null,null);
    	if(b.canTogglePlacementMode())
        {	if(GC.handleRoundButton(gc,standardRects[player],
        			b.canTogglePlacementMode(player)?select:null,
        			s.get(b.placingGipfPieces(player)
        					?"Placing Gipf Pieces":"Placing Standard Pieces"),
        		HighlightColor, rackBackGroundColor))
        	{ select.hitCode = GipfId.Hit_Standard_Button;
        	}
        }
      if(planned && gb.whoseTurn==player)
 	   {
 		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
 	   }
    	   pl.setRotatedContext(gc, select,true);
    	}

     drawPlayerStuff(gc,(vstate==GipfState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);
		  

     commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
     double messageRotation = pl.messageRotation();
     
//     DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),standardRects[gb.whoseTurn]);	// Not needed for barca

		if (vstate != GipfState.PUZZLE_STATE)
        {
			if(!planned && !autoDoneActive()) { handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState()? select : null),HighlightColor, rackBackGroundColor); }
        	handleEditButton(gc,messageRotation,editRect,select,highlight,HighlightColor, rackBackGroundColor);
        }

        

        standardGameMessage(gc,messageRotation,
        		vstate==GipfState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=GipfState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
     	boolean edgeCap = gb.edgeCaptureMode();
     	if(edgeCap)
     	{
     	boolean gipfCap = gb.hasGipfCaptures();
     	int left = G.Left(boardRect);
     	int top = G.Bottom(boardRect)-G.Height(stateRect)*3;
     	int width = G.Width(stateRect);
     	int height = G.Height(stateRect);
     	GC.setFont(gc,largePlainFont());
     	GC.Text(gc,false,left,top,width,height, Color.lightGray,null,s.get(ClickEdgeMessage));
     	top += height*2/3;
      	if(gipfCap) 
     		{ 
     		GC.Text(gc,false,left,top,width,height, Color.lightGray,null,s.get(RetainGipfMessage));
     		
     		}
     	}
        goalAndProgressMessage(gc,ourSelect,s.get(isMatrx()?GoalMessageP:GoalMessage),progressRect, goalRect);
        gb.playerChip[gb.whoseTurn].drawChip(gc, this, iconRect,null);
        // no repetitions are possible in tzaar
        // DrawRepRect(gc,b.Digest(),repRect);
        drawVcrGroup(ourSelect, gc);
        drawViewsetMarker(gc,viewsetRect,ourSelect);
 
    }
    private boolean convertGames = true;
    private boolean removeIndexes = false;	// this is a flag that we're replaying an old file and need to convert SLIDE moves
    public boolean PerformAndTransmit(commonMove mm, boolean transmit,replayMode replay)
    {	if(removeIndexes) 
    		{ mm.setIndex(-1);	// remove indexes implies renumbering the moves 
    		}
    	if(convertGames && (mm.op==MOVE_SLIDE) && (replay==replayMode.Replay))
    	{	// old game records contain MOVE_SLIDE, which we want to remove so the animations look nice.
    		Gipfmovespec m = (Gipfmovespec)mm;
    		removeIndexes = true;
    		PerformAndTransmit(new Gipfmovespec(MOVE_DROPB,m.from_col,m.from_row,m.player),transmit,replay);
    		commonMove mc = m.Copy(null);
    		mc.op = MOVE_SLIDEFROM;
    		return PerformAndTransmit(mc,transmit,replay);
    	}
    	else
    	{
    		return super.PerformAndTransmit(mm, transmit, replay);
    	}
    	
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	
    	if(mm.op==MOVE_STANDARD) {  
        		mm.setLineBreak(true);
        	}
    	
    	handleExecute(b,mm,replay);
    	if(b.pickedObject!=null) { lastDropped = b.pickedObject; }
    	numberMenu.recordSequenceNumber(b.activeMoveNumber()); 
    	
        startBoardAnimations(replay,b.animationStack,animationCellSize(b.animationStack),MovementStyle.Stack);
        return (true);
    }
    
    // gipf scales the cells individually, so use the
    // average size for an animation.
    public int animationCellSize(CellStack cs)
    {
    	int tots = 0;
    	int ns = 0;
    	for(int lim=cs.size()-1; lim>=0;lim--)
    	{	GipfCell c = cs.elementAt(lim);
    		if(c!=null && c.onBoard)
    		{
        	int ypos = G.Bottom(boardRect) - b.cellToY(c.col, c.row);
        	int xpos = G.Left(boardRect) + b.cellToX(c.col, c.row);
        	int siz = scaleCellSize(CELLSIZE,xpos,ypos,boardRect);
        	tots += siz;
        	ns++;
        	}
    		else { tots+= CELLSIZE; ns++; }
    	}
    	return(ns>0 ? tots/ns : CELLSIZE);
    }
    public commonMove ParseNewMove(String st, int player)
    {
        return (new Gipfmovespec(st, player));
    }


    public void StartDragging(HitPoint hp)
    {	
        if (hp.hitCode instanceof GipfId) // not dragging anything yet, so maybe start
        {

        GipfId hitCode = (GipfId)hp.hitCode;
        GipfCell hitCell = hitCell(hp);
        GipfChip hitChip = hitCell==null?null:hitCell.topChip();
        if(hitCell!=null)
        {
        switch(hitCode)
        {
        default: throw G.Error("Not expcting hit code %s",hitCode);
        case Hit_Standard_Button: break;	
        case First_Player_Reserve:
        case Second_Player_Reserve:
        case First_Player_Captures:
        case Second_Player_Captures:
        	PerformAndTransmit("Pick "+hitCode.shortName+" "+hitCell.row);
        	break;
	    case BoardLocation:
	    	if(hitChip!=null)
	    		{
	    		switch(b.getState())
	    		{
	    		case DONE_CAPTURE_STATE:
	    		case DESIGNATE_CAPTURE_STATE:
	    		case DESIGNATE_CAPTURE_OR_DONE_STATE:
	    		case MANDATORY_CAPTURE_STATE:
	    			break;
	    		case PUZZLE_STATE:
	    			PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    			break;
	    		default:  
	    			{
	    			if(b.isDest(hitCell.col,hitCell.row))
	    				{ PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    				}
	    			}
	    			break;
	    		}
	    		}
	    	break;

         }
    	}
        }
    }
    public boolean toggleGipf(GipfCell cell)
    {
    	if(cell.isGipf())
    	{	
    		if(cell.rowcode!=0)
    		{	if(cell.preserved)
    			{
    			PerformAndTransmit("Remove "+cell.col+" "+cell.row);
    			}
    			else
    			{
    			PerformAndTransmit("Preserve "+cell.col+" "+cell.row);
    			}
    		}
    		return(true);
    	}
    	return(false);
    }
    public void StopDragging(HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof GipfId)) 
        	{  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
   		missedOneClick = false;
   		GipfId hitObject =  (GipfId)hp.hitCode;
        GipfCell hitCell = hitCell(hp);
        boolean movingObject = hasMovingObject(hp);
        switch(hitObject)
        {
        case Hit_Standard_Button: 
        	{ PerformAndTransmit("Standard");
        	}
        	break;
        default: throw G.Error("not expecting %s",hitObject);
        case BoardLocation:
        	if(movingObject)
        		{PerformAndTransmit("Dropb "+hitCell.col+" "+hitCell.row);}
        	else {
        		switch(b.getState())
				{
				case DONE_CAPTURE_STATE:
					if(toggleGipf(hitCell)) { break; }
					PerformAndTransmit(RESET);	// doing a proper unwind at this point is just too complicated
					break;
					// otherwise drop into error
				default: throw G.Error("Not expecting %s in state %s",hitObject,b.getState());
				case DESIGNATE_CAPTURE_OR_DONE_STATE:
	    		case DESIGNATE_CAPTURE_STATE:
	    		case MANDATORY_PRECAPTURE_STATE:
	    		case MANDATORY_CAPTURE_STATE:
	    		case PRECAPTURE_STATE:
	    			if(toggleGipf(hitCell)) {}
	    			else if(b.getCell(hitCell)==b.getDest()) { PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row); }
	    			else { PerformAndTransmit("Remove "+hitCell.col+" "+hitCell.row); }
	    			break;

	   			case PRECAPTURE_OR_START_NORMAL_STATE:
  	   			case PRECAPTURE_OR_START_GIPF_STATE:
 	    			if(toggleGipf(hitCell)) { break;}
					//$FALL-THROUGH$
				case SLIDE_STATE:
	    		case PUZZLE_STATE:
	    		case SLIDE_GIPF_STATE:
	    		case PLACE_STATE:
	    		case PLACE_POTENTIAL_STATE:
	    		case PLACE_GIPF_STATE:
	    			PerformAndTransmit("Dropb "+hitCell.col+" "+hitCell.row);
	    			break;
	    		case PLACE_TAMSK_FIRST_STATE:
	    		case PLACE_TAMSK_LAST_STATE:
	    		case PLACE_OR_MOVE_POTENTIAL_STATE:
	    		case MOVE_POTENTIAL_STATE:
	    			if(b.pickedObject==null && hitCell.topChip()!=null)
	    			{
	    			PerformAndTransmit("Pickb "+hitCell.col+" "+hitCell.row);
	    			}
	    			else {
	    			PerformAndTransmit("Dropb "+hitCell.col+" "+hitCell.row);
	    			}
				}
        	}
        	break;
        case First_Player_Reserve: 
        case Second_Player_Reserve:
        case First_Player_Captures:
        case Second_Player_Captures:
        	PerformAndTransmit("Drop "+hitObject.shortName+" "+hitCell.row);
        	break;
        }
    	}
        generalRefresh();
    }

    public boolean handleDeferredEvent(Object target, String command)
    {	
    	if(numberMenu.selectMenu(target,this)) { return(true); }
    	return super.handleDeferredEvent(target,command);
     }

    public void drawBlankTile(Graphics g, int X, int Y, int radius,
        Color color, Color bgcolor)
    { //if(outlineColor!=null)

        //  {
        //   DrawAACircle(g,X, Y, radius,outlineColor,Color.white,false);
        //   radius--;
        //  }
        GC.cacheAACircle(g,X, Y, radius, color, bgcolor, true);
    }

    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     */
 
    public String gameType() { return(b.gametype+" "+"0"+" "+b.revision); }
    public String sgfGameType() { return(Gipf_SGF); }
    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int ran = G.IntToken(his);			// should be the number of players in the game
    	int rev = G.IntToken(his);			// random key for the game
       b.doInit(token,ran,rev);
    }


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() { return(new GipfPlay()); }

/**
 * summary: 5/26/2023
 * 	10487 files visited 0 problems
 * 
 * 

Note: these games are unique in that they have a [1,0] color map.  They cause problems if
one is careless about playerIndex(chip)

gipfgames\archive-2020\games-Feb-7-2020.zip G-Dumbot0-Elisa14-2020-01-31-2217.sgf 
gipfgames\archive-2020\games-Feb-7-2020.zip G-Dumbot0-Elisa14-2020-01-31-2309.sgf 
gipfgames\archive-2020\games-Feb-7-2020.zip G-Dumbot0-Elisa14-2020-01-31-2320.sgf 
gipfgames\archive-2020\games-Feb-7-2020.zip G-Dumbot0-Jojajo-2020-02-02-1609.sgf 
gipfgames\archive-2020\games-Feb-7-2020.zip G-SmartBot0-Jojajo-2020-02-01-2248.sgf

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
            {	StringTokenizer his = new StringTokenizer(value);
            	String token = his.nextToken();
            	long rand = his.hasMoreTokens() ? G.LongToken(his) : 0;
            	boolean hasExplicitRevision = his.hasMoreTokens();
            	int rev = ( hasExplicitRevision ? G.IntToken(his.nextToken()) : b.revision);
                b.doInit(token,rand,rev);
                b.hasExplicitRevision = hasExplicitRevision;
                b.setFirstPlayer(0);
                b.setWhoseTurn(0);
            }

            if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (parseVersionCommand(name,value,2)) {}
            else if (parsePlayerCommand(name,value)) {}
            else
            {
            	if(replayStandardProps(name,value))
            	{	if(name.equals(date_property))
            			{ b.setRevisionDate(value);
            			}
            	}
            }

            prop = prop.next;
        }

        if (!"".equals(comments))
        {
            setComment(comments);
        }
    }
	public int getLastPlacement(boolean empty) {
		return b.lastPlacedIndex;
	}
}
