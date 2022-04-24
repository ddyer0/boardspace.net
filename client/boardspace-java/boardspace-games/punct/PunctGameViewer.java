package punct;

import java.awt.*;
import static java.lang.Math.atan2;

import online.common.*;
import java.util.*;

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
 * Dec 2005 First complete implementation.
 * 
*/
@SuppressWarnings("unused")
public class PunctGameViewer extends CCanvas<punctCell,PunctGameBoard> implements PunctConstants, GameLayoutClient
{
 
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color GridColor = Color.black;
    private Color DotMarkerColor = new Color(0.6f,0.2f,0.7f);
    private Color PunctMarkerColor = new Color(1.0f,0.3f,0.4f);
    private Color rackBackGroundColor = new Color(174,197,169);
    private Color boardBackgroundColor = new Color(220,165,155);
    private Color chatBackgroundColor = new Color(230,250,230);
   

   // private boolean adjustDots=true;	// adjust dot positions rather than piece positions
    // images
    private static Image[] textures = null;// background textures
    private static Image[] arrows = new Image[6]; // rotated arrows
    // private state
    private PunctGameBoard b = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private int CELLRADIUS; //cell raduis, about CELLSIZE/2
    private int showLevels=0;
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //private Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //private Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle punctRects[] = addRect("punct",2);
    
    private Rectangle repRect = addRect("repRect");
    private Rectangle chipRects[] = addRect("chip",2);
    
    public BoardProtocol getBoard()   {    return (b);   }

    public void preloadImages()
    {	
	    if (textures == null)
	    { // note that for this to work correctly, the images and masks must be the same size.  
	      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
	        PunctPiece.preloadImages(loader,ImageDir);
	        arrows[0] = PunctPiece.images[RECYCLE_INDEX];
	        for(int i=1;i<6;i++) { arrows[i] = arrows[0].rotate((i*Math.PI/3),0x0); }
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
 
        b = new PunctGameBoard(info.getString(OnlineConstants.GAMETYPE, "Punct"),getStartingColorMap());
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
    	{startFirstPlayer();
    	}
    }
    
    public commonMove EditHistory(commonMove m)
    {	int sz = History.size()-1;
    	if(m.op==MOVE_PICKB) { return(m); /* never remove picks, preserv where the floating piece came from */ }
    	if(m.op==MOVE_DROPB && (sz>=1))
    	{	Punctmovespec prev = (Punctmovespec)History.elementAt(sz);
    		Punctmovespec prev2 = (Punctmovespec)History.elementAt(sz-1);
    		Punctmovespec newmove = (Punctmovespec)m;
    		if((prev.op == MOVE_PICKB) && (prev.to_col==newmove.to_col) && (prev.to_row==newmove.to_row))
    		{	// second part of a rotate
     			if((prev2.op==MOVE_DROPB) && (prev2.to_col==newmove.to_col) && (prev2.to_row==newmove.to_row))
    			{	// drop followed by rotate
     	  			popHistoryElement();
     	  		 	popHistoryElement();
    			}
    		}
    		else if((prev.op==MOVE_PICKB) 
    				&& (prev2.op==MOVE_DROPB) 
    				&& (prev2.to_col==prev.to_col)
    				&& (prev2.to_row==prev.to_row))
    			{	// drop and relocate 
    				popHistoryElement();
    				popHistoryElement();
    			}
    	}
    	return(super.EditHistory(m));
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
    	int minLogW = fh*12;	
    	int vcrW = fh*16;
       	int minChatW = fh*40;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int buttonW = fh*8;
        int nrows = b.nrows+3;  // b.boardRows
        int ncols = b.nrows;	 // b.boardColumns
        	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.65,	// 60% of space allocated to the board
    			0.8,	// aspect ratio for the board
    			fh*2.0,
    			fh*3,	// maximum cell size
    			0.3		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
    	layout.placeTheVcr(this,vcrW,vcrW*3/2);
      	//layout.placeDrawGroup(G.getFontMetrics(standardPlainFont()),acceptDrawRect,declineDrawRect);

    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	CELLRADIUS = CELLSIZE/2;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY;
        int stateX = boardX;
        int stateH = fh*2;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,liftRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
        return boardW*boardH;
    }
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle punct = punctRects[player];
    	Rectangle box =  pl.createRectangularPictureGroup(x+2*unitsize,y,unitsize);
    	Rectangle done = doneRects[player];
    	int doneW = plannedSeating()? unitsize*4 : 0;
    	G.SetRect(punct,x,y,unitsize*2,unitsize*2);
    	G.SetRect(done,G.Right(box)+unitsize/2,G.Top(box)+unitsize/2,doneW,doneW/2);
       	if(flatten)
       	{
       		G.SetRect(chip,	G.Right(done)+unitsize/3,	y,unitsize*16,	4*unitsize);
       	}
       	else
       	{
       		G.SetRect(chip,	x,	G.Bottom(box),unitsize*16,	4*unitsize);
       	}
       	G.union(box, done,chip,punct);
    	pl.displayRotation = rotation;
    	return(box);
    }


	// draw a graphic at x,y.  The images are loaded and composited
    // at init time, so they're always completely ready to draw.
    private void drawChip(Graphics gc, Image im, double []scaleset,int x, int y, double boxw, 
    			double jitter,int height)
    {	drawImage(gc,im,scaleset,x,y,boxw,1.0,jitter,(height>showLevels)?""+(height+1):null,true);
    }


	// draw a box of spare chips. For hex it's purely for effect.
    private void DrawChipPool(Graphics gc, commonPlayer pl,   HitPoint highlight)
    {	int player = pl.boardIndex;
    	Rectangle r = chipRects[player];
    	Rectangle pr = punctRects[player];
        boolean canhit = b.LegalToHitChips(player) && G.pointInRect(highlight, r);
        boolean somehit=false;
		PunctId chipcode = playerChipCode[player];
		if(gc!=null) 
		{
		PunctPiece pc = b.playerChip[player];
		pc.drawChip(gc, this, G.Width(pr)*2/3,G.centerX(pr),G.centerY(pr),null);
		b.WinForPlayerNow(player);	// make sure the bookeeping is up to date
		int infowidth = 50;
		int infoleft = G.Right(r)-infowidth;
		int infoheight = 15;
    	int left = NUMREALPIECES-b.piecesOnBoard[player];
    	int cent = b.centerScore[player];
    	GC.Text(gc,false,infoleft,G.Bottom(r)-infoheight,infowidth,infoheight,Color.black,null,s.get("#1 left",""+left));
    	GC.Text(gc,false,infoleft-infowidth,G.Bottom(r)-infoheight,infowidth,infoheight,Color.black,null,s.get("#1 center",""+cent));
  		GC.frameRect(gc, Color.black, r); 
		}
       { // draw the unused pices
       	PunctPiece pieces[] = b.pieces[player];
       	punctCell prack[] = b.rack[player];
       //tri(6) str(2) str2(4) y1(2) y2(2) y3(3)
       	int cellw = G.Width(r)/11;
       	int cellh = G.Height(r)/2;
       	int cellhinc = cellh/6;
       	int cellwinc = cellw/7;
       	int lasttype = -1;
       	int lastpunct = -1;
       	int rx = G.Left(r) - cellw/3;
       	int ry = G.Top(r)+cellh/4;
       	int ry0 = ry;
      	int piecetype = -1;
    	double vadj[] = {0.55,0.1,0.8,0.4,0.0,-0.3};
       	double hadj[] = {0.0,0.0,-0.05,0.1,0.3,0.1};
        if (canhit)
        {  if(hasMovingObject(highlight)) { somehit=true; }
           highlight.hitCode = chipcode;
      	   highlight.arrow = StockArt.DownArrow ;
       	   highlight.awidth = 3*cellw/2;
       	   highlight.spriteRect = r;
       	   highlight.spriteColor = Color.red;
       }
       	for(int i=1; i<pieces.length;i++)
  
       	{PunctPiece p = pieces[i];
       	 punctCell cell = prack[i];
       	 int type = p.typecode;
       	 int punct = p.punct_index;
       	 if((punct!=lastpunct) || (type!=lasttype))	// next cell
       	 {	rx += cellw+cellw/4;
       	 	ry = ry0;
       	 	lasttype=type;
       	 	lastpunct=punct;
       	 	piecetype++;
       	 }
         int yadj = (int)(vadj[piecetype]*cellh);
       	 int xadj = (int)(hadj[piecetype]*cellw);
       	 int left = rx + (cellw / 2);
       	 int top = ry + (cellh / 2);
       	 
       	 pl.rotateCurrentCenter( cell, left,top);
       	 
       	 if(canhit 
       		&& (p.level==POOL_LEVEL) 
       		&& !somehit && G.pointNearCenter(highlight,left+xadj,top-yadj,cellw,cellw))
       	 { 
       	   highlight.hitCode = chipPoolIndex[player];
       	   highlight.hitObject = p;
       	   highlight.hit_x = left+xadj;
       	   highlight.hit_y = top;
       	   highlight.arrow = hasMovingObject(highlight) ? StockArt.DownArrow : StockArt.UpArrow;
       	   highlight.awidth = 3*cellw/2;
       	   highlight.spriteRect = null;
       	   highlight.spriteColor = Color.red;
       	   somehit=true;
       	 }
       	 if(gc!=null) 
       	 { if(p.level==POOL_LEVEL) 
       	 	{ 
       		 p.drawChip(gc, this, (int)(cellw*0.9),left+xadj,top-yadj,null);
       	 	}
       	 }
       	 ry += cellhinc;
       	 rx += cellwinc;
       	}	
      }
     }

   public void drawSprite(Graphics g,int obj,int xp,int yp)
   {
      	PunctPiece pp = b.allPieces[obj];
        pp.drawChip(g,this, CELLSIZE,xp, yp,null);  
   }

   public Point spriteDisplayPoint()
   {	return(new Point(G.Left(boardRect)+CELLSIZE*3,G.Top(boardRect)+CELLSIZE*2));
   }


    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean review = reviewMode() && !mutable_game_record;
      // erase
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //G.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
		b.SetDisplayParameters(0.91, 0.99, 0.03,-0.09,0); //
 		b.SetDisplayRectangle(boardRect);
      // for us, the board is one large graphic, for which the target points
      // are carefully matched with the abstract grid
     PunctPiece.images[BOARD_INDEX].centerImage(gc, boardRect);
      
      // draw a picture of the board. In this version we actually draw just the grid.
      b.DrawGrid(gc, boardRect, use_grid, boardBackgroundColor, GridColor, GridColor, GridColor);
       
      // draw the tile grid - this is used to match the grid to the graphic, and also 
      // is useful when tuning the position of the pieces
      /**
      if(useAuxSliders)
      {//if true, paint a dot grid to guide the layout process
       for (int col = 0; col < b.ncols; col++)
       {
           char thiscol = (char) ('A' + col);
           int lastincol = b.nInCol[col];

           for (int thisrow0 = lastincol, thisrow = lastincol +
                   b.firstRowInCol[col]; thisrow0 >= 1; thisrow0--, thisrow--) // start at row 1 (0 is the grid) 
           { //where we draw the grid
              int ypos = G.Bottom(boardRect) - b.cellToY(thiscol, thisrow);
              int xpos = G.Left(boardRect) + b.cellToX(thiscol, thisrow);
              drawChip(gc,PunctPiece.images[HEXTILE_INDEX],hextile_scale,xpos,ypos,CELLSIZE,0.0,0);
              //punctCell c = (punctCell)b.GetBoardCell(thiscol,thisrow);
              //if(c.plines[0]>0)
            	//  {G.Text(gc,true,xpos-CELLSIZE/2,ypos-CELLSIZE/2,CELLSIZE,CELLSIZE,
            	//	  Color.white,null,""+c.plines[0]);}
          }
       }
      }
       */
 
   }

   int protation=0;
   boolean isClockwise(PunctGameBoard gb,PunctPiece p,int hitx,int hity,int x,int y)
   {	int cx = gb.cellToX(p.cols[0],p.rows[0]);
   		int cy = gb.cellToY(p.cols[0],p.rows[0]);
   		double ang0 = atan2(cy-hity,cx-hitx);
   		double ang1 = atan2(cy-y,cx-x);
   		if(Math.abs(ang0-ang1)>Math.PI) 
   			{ //watch out for the cut between pi and -pi
   			 //System.out.println("BC "+ang0 +" " + "P "+ang1);
   			ang1+=((ang1<ang0)?2:-2)*Math.PI; 
   			}
   		// convert -pi to pi to 0 to 5. Save as a side effect (sorry!)
   		protation = ((int)(((ang0+2*Math.PI+Math.PI/2+Math.PI/12)*6)/(2*Math.PI)))%6;
  
   		//System.out.println("C "+ang0 +" " + "P "+ang1);
   		return(ang0<=ang1);
    }
   private void drawPdot(Graphics gc,PunctGameBoard gb,Rectangle brect,char col,int row,Color clr)
   { int xpos = G.Left(brect)+gb.cellToX(col,row);
	 int ypos = G.Bottom(brect)-gb.cellToY(col,row);
     int sz = Math.max(3,CELLSIZE/10);
     GC.cacheAACircle(gc,xpos-sz/2,ypos+sz/2+CELLSIZE/3,sz, clr,Color.black,true);

   }
   /* draw the board and the chips on it. */
     private void drawBoardElements(Graphics gc, PunctGameBoard gb, Rectangle brect, HitPoint highlight)
    {
        //
        // now draw the contents of the board and anything it is pointing at
        //
    	int pieceSize = (int)(CELLSIZE*1.1);
    	boolean useLift = doLiftAnimation();
     	int liftdiv = 20;
     	

     	punctCell hitCell = null;
        boolean somehit = false;
        int someXpos = '@';
        int someYpos = 0;
        int height=-1;
        int maxheight=-1;
        int rotateIndex = 0;
        
        if(!useLift && highlight!=null)
        { int hx = G.Left(highlight)-G.Left(brect);
          int hy = G.Bottom(brect)-G.Top(highlight);
          punctCell closestCell = gb.closestCell(hx,hy);
          if(closestCell!=null)
          {
          int cx = gb.cellToX(closestCell.col,closestCell.row);
          int cy = gb.cellToY(closestCell.col,closestCell.row);
          double dis = G.distance(hx,hy,cx,cy);
          if(dis<CELLRADIUS) 
          	{ hitCell = closestCell;
          	  if(hitCell.level()>=0) { highlight.hitCode=PunctId.HitOtherPiece; }
          	}
          }
        }
 
        if(gc!=null)
        {
        punctCell punctcell = b.getPunctCell();
        if((punctcell==null)&&(hitCell!=null))
        {	PunctPiece p = hitCell.topPiece();
        	if(p!=null)
        	{ if((p.cols[0]==hitCell.col)&&(p.rows[0]==hitCell.row)) { punctcell=hitCell; }
        	}
        }
         if(punctcell!=null)
        {// draw purple dots on the dots in legal directions
         int hitz = (punctcell.col-punctcell.row);
          for(punctCell c = gb.allCells; c!=null; c=c.next)
         { if((punctcell.col==c.col) || (punctcell.row==c.row) ||(hitz==(c.col-c.row)))
        		 {
        	 	 drawPdot(gc,gb,brect,c.col,c.row,DotMarkerColor);
        		 }
         }}
        }
        //don't count the blobs, it will confuse the robot
        //if(useAlternateBoard && (gc!=null)) { gb.countBlobs(true); }
        int left = G.Left(brect);
        int top = G.Bottom(brect);
        
        while(height++<=maxheight)
        {
        // draw columns right to left so pieces at the same level won't shadow
        // each other.  the artwork shadows fall from right to left.
        	for(Enumeration<punctCell>cells = gb.getIterator(Itype.TBRL);cells.hasMoreElements();)
            { //where we draw the grid
        		punctCell cell = cells.nextElement();
        		 int thisrow = cell.row;
        		 char thiscol = cell.col;
            	 int cellypos = gb.cellToY(cell);
                 int ypos = top - cellypos;
                 int cellxpos = gb.cellToX(cell);
                int xpos = left + cellxpos;
                boolean hitpoint = !somehit 
                	&& (cell==hitCell) 
                	&& (cell.level()<=height)
                	&& gb.LegalToHitBoard(thiscol,thisrow);
                int thislevel = cell.level();
                maxheight = Math.max(thislevel,maxheight);
                PunctPiece piece = cell.pieceAt(height); 
                boolean isPunct = (piece!=null) 
                	&& ((piece.cols[0]==cell.col)&&(piece.rows[0]==cell.row));
                // drawing
                if(gb!=b && (piece==null) && (height==0) && (gc!=null))
                {	int bloBits = cell.bloBits;
                	if(bloBits!=0)
                		{Color bc = new Color(0xff-(bloBits&0xff),0xff-((bloBits>>8)&0xff),0xff-((bloBits>>16)&0xff));
                	     int cs = CELLSIZE/5;
                	     GC.setColor(gc,bc);
                	     GC.fillOval(gc,G.Left(boardRect)+cellxpos-cs,G.Bottom(boardRect)-cellypos+CELLSIZE/4,cs*2,cs*2);
                		}
                }
                cell.rotateCurrentCenter(gc,xpos,ypos);
         //   b.playerChip[1].drawChip(gc, this, CELLSIZE, xpos, ypos, null);     
                if (hitpoint && ((piece==null)||(height==thislevel)))
                {	somehit=true;
            		someXpos = xpos;
            		someYpos = ypos;
            		boolean fordrop = ((piece==null)||hasMovingObject(highlight));
                    highlight.hitCode = fordrop?PunctId.EmptyBoard 
                    		: isPunct ? PunctId.BoardLocation 
                    	    : isClockwise(gb,piece,cellxpos,cellypos,G.Left(highlight)-G.Left(brect),G.Bottom(brect)-G.Top(highlight)) 
                    	    	? PunctId.RotatePieceCW 
                    	    	: PunctId.RotatePieceCCW;
                    rotateIndex = protation; 	// side effect from isClockwise
                    highlight.col = fordrop ? thiscol : piece.cols[0];
                    highlight.row = fordrop ? thisrow : piece.rows[0];
                    highlight.hit_x = xpos;
                    highlight.hit_y = ypos+CELLSIZE/3;
                    highlight.spriteColor = Color.red;
                }

                if (gc != null)
                {	
                    if(piece!=null)
                	{
                    // show the actual coordinates occupied, so we can
                    // see if they match the graphic
                    //G.Text(gc,false,xpos+CELLSIZE*2,ypos+CELLSIZE,CELLSIZE*2,CELLSIZE,
                    //		Color.red,null,""+cell.col+cell.row+" "+pi+"/"+pl);
                    //double scale[] = piece.dotScaleSet();
                    int liftYval = height*(useLift?(liftSteps*CELLSIZE)/liftdiv : 0);
                    int liftXval = height*(useLift?(liftSteps*CELLSIZE)/(2*liftdiv) : 0);
            
                    if (isPunct)
                    {
                    //if(adjustDots) { adjustScales(scale,piece); }
                    
                    {
                    //double pscale[] = piece.scaleSet();
                    //if(!adjustDots) { adjustScales(pscale,piece); }
                    int ah = cell.activeAnimationHeight();
                    if(height<=cell.level()-ah)
                    {
                    piece.drawChip(gc,this, pieceSize,xpos+liftXval, ypos-liftYval,(height>showLevels)?""+(height+1):null);
                    }}
 
                    if(gb.isDest(thiscol,thisrow))
                        {	drawPdot(gc,gb,brect,thiscol,thisrow,PunctMarkerColor);
                        }
                         // show the location of the punct so we can see
                        // if it agrees with the graphic
                        // G.Text(gc,false,xpos+CELLSIZE,ypos,CELLSIZE,CELLSIZE,Color.green,null,""+cell.col+cell.row);
                	}
                	}
                }
            }
        }
        if(gc!=null)
        {   punctCell punctcell = b.getPunctCell();
        	if(punctcell!=null)
        	{ PunctPiece p = b.getPunctPiece();
        	  char col = punctcell.col;
        	  int row = punctcell.row;
        	  int xx = G.Left(brect) + gb.cellToX(col, row);
        	  int yy = G.Bottom(brect) - gb.cellToY(col, row);
        	  p.drawChip(gc, this,CELLSIZE,xx,yy,null);
        	}

        if (somehit && (highlight.hitCode instanceof PunctId))
        { // checking for pointable position
        	highlight.awidth = CELLSIZE;
        	switch((PunctId)highlight.hitCode)
        	{
        	default: throw G.Error("unexpected hitcode");
        	case BoardLocation:
                highlight.arrow = StockArt.UpArrow;
                break;
        	case RotatePieceCCW:
         	case RotatePieceCW:
         		{
         		// still drawn the old way until ver convert the main pieces to chips
         		int aindex = (rotateIndex+((highlight.hitCode==PunctId.RotatePieceCW)?0:3))%6;
                drawChip(gc, arrows[aindex], recycle_scale,someXpos, someYpos, CELLSIZE, 0.0,0);
         		}
                break;
        	case EmptyBoard:
                highlight.arrow = StockArt.DownArrow;
                break;
        	}

             //G.Text(gc,false,someXpos,someYpos,CELLSIZE,CELLSIZE,Color.black,null,""+cell.col+cell.row);
        }
    }

    }

    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  PunctGameBoard gb = disB(gc);
       PunctState vstate = gb.getState();
       boolean moving = hasMovingObject(selectPos);
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;		// hit if our turn
       HitPoint buttonSelect = moving?null:ourTurnSelect;	// hit if nothing dragging on our turn
       HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;	// hit if nothing dragging
       drawBoardElements(gc, gb, boardRect, ourTurnSelect);
       
       boolean planned = plannedSeating();
       for(int player = FIRST_PLAYER_INDEX; player<=SECOND_PLAYER_INDEX; player++)
       {
    	   commonPlayer pl = getPlayerOrTemp(player);
    	   pl.setRotatedContext(gc, selectPos,false);
    	   DrawChipPool(gc, pl, ourTurnSelect);
    	   if(planned && gb.whoseTurn==player)
    	   {
    		   handleDoneButton(gc,doneRects[player],(gb.DoneState() ? buttonSelect : null), 
   					HighlightColor, rackBackGroundColor);
    	   }
    	   
       	   pl.setRotatedContext(gc, selectPos,true);
       }
      

       GC.setFont(gc,standardBoldFont());
       
       drawPlayerStuff(gc,(vstate==PunctState.PUZZLE_STATE),nonDragSelect,
	   			HighlightColor, rackBackGroundColor);

       commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
       double messageRotation = pl.messageRotation();
 
       if (vstate != PunctState.PUZZLE_STATE)
        {
    	   if(!planned)
    		   {handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
					HighlightColor, rackBackGroundColor);
    		   }
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);
         }

      
        standardGameMessage(gc,messageRotation,
        		vstate==PunctState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=PunctState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
        
        gb.playerChip[gb.whoseTurn].drawChip(gc,this,G.Width(iconRect),G.centerX(iconRect),G.Top(iconRect),null);
        
        goalAndProgressMessage(gc,nonDragSelect,s.get("punctgoal"),progressRect, goalRect);

        DrawRepRect(gc,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca

        drawLiftRect(gc,liftRect,nonDragSelect,textures[LIFT_ICON_INDEX]);
        redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
        drawVcrGroup(nonDragSelect, gc);
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
        handleExecute(b,m,replay);						 // let the board do the dirty work
        startBoardAnimations(replay,b.animationStack,CELLSIZE,MovementStyle.Simultaneous);
        if(replay!=replayMode.Replay) { playSounds(m); }
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
        return (new Punctmovespec(st, player));
    }

/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof PunctId)// not dragging anything yet, so maybe start
        {

        PunctId hitObject = (PunctId)hp.hitCode;
 	    switch(hitObject)
	    {
	    default: 
	    	break;
	    case Black_Chip_Pool:
	    case White_Chip_Pool:
	    	{PunctPiece pp = (PunctPiece)(hp.hitObject);
	    	 if(pp!=null) { PerformAndTransmit("Pick "+playerChar[b.playerIndex(pp)]+" "+pp.id); }
	    	}
	    	break;
	    case BoardLocation:
	    	PerformAndTransmit("Pickb "+hp.col+" "+hp.row);
	    	break;
        }
        }
    }
	private void doDropChip(char col,int row,int rotate,boolean fromrack)
	{	PunctState state = b.getState();
		switch(state)
		{
		default: throw G.Error("Not expecting state %s",state);
		case PLAY_STATE:
		case PUZZLE_STATE:
			{
			int mo = getMovingObject(null);
			if(mo<0) { mo=0; }
			PunctPiece pp = b.allPieces[mo];
			int newrot = (pp.rotation+rotate)%6;
			newrot = b.nextValidRotation(pp,col,row,newrot,fromrack);
			PerformAndTransmit("dropb "+mo+" "+col+" "+row+" "+newrot);
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
        if(!(id instanceof PunctId)) {  missedOneClick = performStandardActions(hp,missedOneClick); }
    	else {
    	missedOneClick = false;
    	PunctId hitObject = (PunctId)id;
		PunctState state = b.getState();
		int dorot=0;
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{int mo = getMovingObject(hp);
        	if(hasMovingObject(hp))
        	{PunctPiece mp = b.allPieces[mo];
        	 PerformAndTransmit("Drop "+playerChar[b.playerIndex(mp)]+" "+mp.id);
        	}}
        	break;
        	
	    case RotatePieceCW:
	    case RotatePieceCCW:
	    	PerformAndTransmit("Pickb "+hp.col+" "+hp.row);
	    	dorot=(hitObject==PunctId.RotatePieceCW)?1:5;
			//$FALL-THROUGH$
		case BoardLocation:	// we hit an occupied part of the board 
        case EmptyBoard:
	    	{int mo = getMovingObject(hp);
	    	if(mo>=0)
	    	{
			switch(state)
			{
				default:
					throw G.Error("Not expecting hit in state %s",state);
				case CONFIRM_STATE:
				case DRAW_STATE:
				case PLAY_STATE:
					{ 
					  doDropChip(hp.col,hp.row,dorot,b.pickedSourceLevel==POOL_LEVEL);
					}
					break;
				case PUZZLE_STATE:
					doDropChip(hp.col,hp.row,dorot,false);
					break;
			}
	    	}}
			break;
			
        case HitOtherPiece:   
        	performReset();
            break;
        }}
    }

    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Punct_SGF); }

    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        b.doInit(token);
    }


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() { return(new PunctPlay()); }


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

            //System.out.println("prop " + name + " " + value);
            if (setup_property.equals(name))
            {
                b.doInit(value);
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (name.equals(game_property) && value.equalsIgnoreCase("punct"))
            {
            	// the equals sgf_game_type case is handled in replayStandardProps
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
