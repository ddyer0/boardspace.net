package carnac;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;

import carnac.CarnacChip.FaceColor;
import carnac.CarnacChip.FaceOrientation;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.*;
import lib.Graphics;
import lib.*;


/**
 * 
 * Change History
 *
 *
*/
public class CarnacViewer extends CCanvas<CarnacCell,CarnacBoard> implements CarnacConstants
{
    // file names for jpeg images and masks
    static final String ImageDir = "/carnac/images/";
    static final String Carnac_SGF = "Carnac";
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private Color veryLtGreen = new Color(0.95f,1.0f,0.95f);
    private Color rackBackGroundColor = new Color(0.4f,0.8f,0.4f);
     // private state
    private CarnacBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    private static int SUBCELL = 4;	// number of cells in a square
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle firstPlayerChipRect = addRect("firstPlayerChipRect");
    private Rectangle secondPlayerChipRect = addRect("secondPlayerChipRect");
    private Rectangle reverseViewRect = addRect("reverse");
    private Rectangle chipSetRect = addRect("chipset");
    private Rectangle poolRect = addRect("poolRect");
    
    private JCheckBoxMenuItem reverseOption = null;
   
    private TextButton passButton = addButton(PASS,GameId.HitPassButton,ExplainPass,
			HighlightColor, rackBackGroundColor);
    private Rectangle repRect = addRect("repRect");
     
    private int currentChipSet = 0;
    private int showChipSet = 0;
    private boolean chipSetLock = false;
    public int getAltChipset() { return(showChipSet); }
    
    public void preloadImages()
    {	
       	CarnacChip.preloadImages(loader,ImageDir);
       	gameIcon = CarnacChip.findChip(FaceOrientation.Up,
    					FaceColor.White,FaceColor.Red,FaceColor.White).image;
     }

    public Slider sizeRect = null;

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
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//
        sizeRect = addSlider("sizeRect",s.get(MenhirSize),CarnacId.ZoomSlider);
        sizeRect.min=0.5;
        sizeRect.max=1.2;
        sizeRect.value=1.0;
        //zoomRect.barColor=ZoomColor;
        sizeRect.highlightColor = HighlightColor;

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new CarnacBoard(info.getString(OnlineConstants.GAMETYPE, variation.getDefault()),
        		randomKey,getStartingColorMap());
        useDirectDrawing();
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

    	
 	int bcols = 18;
	int brows = 9;	// size of full sized board
	int chatCols = 16;
    public int setLocalBoundsSize(int width,int height,boolean wideMode,boolean tallMode)
    {	
        int chatHeight = selectChatHeight(height);
      	int sncols = bcols*SUBCELL+(tallMode ? -20 : (wideMode ? chatCols+5 : 1)); // more cells wide to allow for the aux displays
      	int snrows = brows*SUBCELL+(tallMode ? 14 : 6);  
      	int cellw = width / sncols;
      	int cellh = (height-(wideMode ? 0 : chatHeight)) / snrows;
      	CELLSIZE = Math.max(1,Math.min(cellw, cellh)); //cell size appropriate for the aspect ration of the canvas
       	SQUARESIZE = CELLSIZE*SUBCELL;
      	return(SQUARESIZE);
    }
    public void setLocalBoundsWT(int x, int y, int width, int height,boolean wideMode,boolean tallMode)
    {   
        int chatHeight = selectChatHeight(height);
     	boolean noChat = (chatHeight==0);
     	int ideal_logwidth = CELLSIZE * 18;
        int CS = CELLSIZE;
        int C2 = CELLSIZE/2;
        G.SetRect(fullRect,x,y,width,height);
 
        // game log.  This is generally off to the right, and it's ok if it's not
        // completely visible in all configurations.
        int boardW = SQUARESIZE * bcols;
        int boardH = SQUARESIZE * brows;
        int boardX = tallMode ? (width-boardW)/2 : C2;
        G.SetRect(boardRect,boardX, (wideMode ? 0 : chatHeight)+SQUARESIZE-CS, boardW , boardH);      
        {
        int stateH = CELLSIZE*2;
        G.placeRow(x + CS,
        		(wideMode ? 0 : chatHeight) +CS/3,
        		tallMode ? width-stateH*2 : width/2, 
        		stateH,stateRect,noChatRect);
        
        }
        G.SetRect(firstPlayerChipRect,
        			tallMode ? x+C2 : G.centerX( boardRect)+CS,
        			tallMode ? G.Bottom(boardRect) : G.Top( boardRect),
        			SQUARESIZE,SQUARESIZE);
       	int buttonWidth = CS*6;
       	int buttonHeight = CS*3;
        {
            commonPlayer pl0 = getPlayerOrTemp(0);
            commonPlayer pl1 = getPlayerOrTemp(1);
            Rectangle p0time = pl0.timeRect;
            Rectangle p1time = pl1.timeRect;
            Rectangle p0alt = pl0.extraTimeRect;
            Rectangle p1alt = pl1.extraTimeRect;
            Rectangle p0anim = pl0.animRect;
            Rectangle p1anim = pl1.animRect;
            
            Rectangle firstPlayerRect = pl0.nameRect;
            Rectangle firstPlayerPicRect = pl0.picRect;
            Rectangle secondPlayerRect = pl1.nameRect;
            Rectangle secondPlayerPicRect = pl1.picRect;
            
            //first player name
            G.SetRect(firstPlayerRect, G.Right(firstPlayerChipRect)+CS,G.Top( firstPlayerChipRect), CS * 6, CS*2);
  
             // time display for first player
            G.SetRect(p0time, G.Right(firstPlayerRect)+CS/3,G.Top(firstPlayerRect), CS * 4, CS);
            G.AlignLeft(p0alt,G.Bottom(p0time),p0time);
            // first player "i'm alive" animation ball
            G.SetRect(p0anim, G.Right(p0time),G.Top( p0time),CS,CS);
            // first player portrait
            G.SetRect(firstPlayerPicRect, G.Right(p0anim)+CELLSIZE,G.Top( p0anim), CS * 8, CS * 8);
            
     
            // player 2 portrait
            G.AlignXY(secondPlayerPicRect,
            		tallMode ? G.centerX(boardRect) : G.Left(boardRect)+CELLSIZE,
            		tallMode ? G.Bottom(boardRect) : G.Bottom(boardRect)-CELLSIZE*5-G.Height(firstPlayerPicRect),
            		firstPlayerPicRect);
    
            G.AlignXY(secondPlayerChipRect,G.Right(secondPlayerPicRect)+CELLSIZE,G.Bottom(secondPlayerPicRect)-G.Height(firstPlayerChipRect),
            	firstPlayerChipRect);
            
            //second player name
            G.AlignXY(secondPlayerRect, G.Right(secondPlayerChipRect)+CELLSIZE, G.Bottom(secondPlayerChipRect) - G.Height(firstPlayerRect),
            		firstPlayerRect);

            // time display for second player
            G.AlignXY(p1time, G.Right(secondPlayerRect),G.Top(secondPlayerRect), p0time);
            G.AlignLeft(p1alt,G.Bottom(p1time), p1time);

            G.AlignXY(p1anim,G.Right(p1time),G.Top( p1time), p0anim);
            int poolH = SQUARESIZE*3;
            int poolWidth = SQUARESIZE*2;
            G.SetRect(poolRect,
            		tallMode ? x+CS: G.Right(boardRect)-poolWidth,
            		tallMode ? G.Bottom(boardRect)-poolH : G.Top(boardRect) + brows*SQUARESIZE/2-SQUARESIZE,
            		poolWidth,poolH);

          int logWidth = tallMode ? G.Left(firstPlayerPicRect)-CS : ideal_logwidth;
          int logx =
            		(tallMode&noChat)
            			? x+C2 :wideMode||noChat
            					? width-logWidth-CELLSIZE
            					: (tallMode ? width : G.Right(boardRect))-logWidth-CELLSIZE;
           int logY = noChat ? tallMode ? G.Bottom(firstPlayerChipRect)+CELLSIZE : G.Bottom(poolRect)+CS :  y;
            G.SetRect(logRect,
            		logx,
            		logY,
            		logWidth, 
            		noChat ? height-logY-C2 : wideMode ? CS*20 : chatHeight);
            
        
            G.SetRect( passButton, 
            		tallMode ? CS*15 : G.Right(p1anim)+CELLSIZE*5, 
            		G.Bottom(boardRect)-9*CELLSIZE,
            		buttonWidth,buttonHeight);
 
        
        // "done" rectangle, should always be visible, but only active when a move is complete.
        G.SetRect(doneRect, G.Left(passButton), G.Bottom(passButton)+CS/2,buttonWidth,buttonHeight);
        G.SetRect(editRect, G.Right(doneRect)+CS/2,G.Top( doneRect),buttonWidth,buttonHeight);
        
        int chatY = wideMode ? G.Bottom(poolRect)+CELLSIZE: 0;
        int chatX = wideMode ? G.Right(boardRect)-6*CELLSIZE : 0;
        int chatW = wideMode ? width-chatX-CS/2 : logx-chatX-C2;
        int chatH = wideMode ? height-chatY-C2 : chatHeight;
        G.SetRect(chatRect,
        		chatX,
        		chatY,
        		chatW,
        		chatH);

        }
        //this sets up the "vcr cluster" of forward and back controls.
        int vcrW = CS*10;
        SetupVcrRects(
        		tallMode ? width-vcrW-C2 : x+CS,G.Top(boardRect),
            vcrW,
            vcrW/2);
 
        
        G.SetRect(sizeRect, G.Left( vcrRect), G.Bottom(vcrRect)+CELLSIZE, CELLSIZE*8, CS+C2);
       
        G.SetRect(reverseViewRect,
        		tallMode ? x+CS : G.Left( sizeRect),
        		tallMode ? G.Top(boardRect) : G.Bottom(sizeRect)+CELLSIZE,
        		CELLSIZE*8, (CELLSIZE*8*G.Height(boardRect))/G.Width(boardRect));

        int CS4 = CELLSIZE*4;
        G.SetRect(chipSetRect,
        		tallMode ? G.Right(poolRect) : G.Left( sizeRect),
        		tallMode ? G.Bottom(poolRect)-CS4-C2 : G.Bottom(reverseViewRect)+CS, CS4, CS4);

        int gh = CELLSIZE*2;
        G.SetRect(goalRect, G.Right(chipSetRect),G.Bottom(boardRect)-2*CELLSIZE, CELLSIZE*35, gh);
        int repX = G.Right(editRect)+CS;
        G.SetRect(repRect, repX,G.Top( goalRect)-gh,  G.Right(goalRect)-repX,  gh);

        setProgressRect(progressRect,goalRect);

        positionTheChat(chatRect,veryLtGreen,rackBackGroundColor);
        generalRefresh();
    }
    
	
    private void DrawReverseMarker(Graphics gc, CarnacBoard gb,Rectangle r,HitPoint highlight)
    {	boolean sav = gb.reverseY();
    	Rectangle br = G.clone(r);
    	G.SetTop(br, G.Top(br)+G.Height(br)/10);
    	gb.setReverseY(!gb.reverseY());
	    setBoardParameters(gb,br);
	   gb.rules.board.image.centerImage(gc, br);
		drawBoardElements(gc, gb, br,null);
	    gb.setReverseY(sav);
		GC.frameRect(gc,Color.black,r);
		HitPoint.setHelpText(highlight,r,CarnacId.ReverseViewButton,s.get(ReverseViewExplanation));
    	
    	setBoardParameters(gb,boardRect);
     }  
	
    private void DrawChipsetMarker(Graphics gc, CarnacBoard gb,Rectangle r,HitPoint highlight)
    {	CarnacChip ch = CarnacChip.findChip(FaceOrientation.Horizontal,
    					FaceColor.Red,FaceColor.Red,FaceColor.White);
    	ch = ch.getAltChip(1); 
    	ch.drawChip(gc,this,G.Width(r)*3/4,G.Left(r)+G.Width(r)/3,G.centerY(r),null);
		GC.frameRect(gc,Color.black,r);
    	if(HitPoint.setHelpText(highlight,r, CarnacId.FlatViewButton,s.get(FlatViewExplanation)))
    	{	
    		showChipSet = chipSetLock ? currentChipSet : currentChipSet^1;
    	}
    	else { showChipSet = currentChipSet; chipSetLock = false; }
    	setBoardParameters(gb,boardRect);
     } 
	// draw a selection of starting pieces
    private void DrawCommonChipPool(Graphics gc, CarnacBoard gb,Rectangle r, HitPoint highlight)
    {	CarnacCell pool[] = gb.pool;
    	int np = pool.length;
    	int w = Math.min(G.Width(r)/2,G.Height(r)/3);
    	int x = G.Left(r) + w/2+(G.Width(r)-2*w)/2;
    	int y = G.Top(r) + w+w/3;
    	for(int pl = 0; pl<np; pl++)
    	{
    	CarnacCell thisCell = gb.pool[pl];
        boolean canHit = gb.LegalToHitChips(pl);
        CarnacChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = null;
        thisCell.drawStack(gc,pt,x + (pl&1)*w,y+((pl&2)>>1)*w,this,0,(int)(w*sizeRect.value),0,msg);
        if((highlight!=null) && (highlight.hitObject==thisCell))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = G.Width(r)/4;
        	highlight.spriteColor = Color.red;
        }
        GC.setFont(gc,largeBoldFont());
        GC.Text(gc,true,G.Left(r),G.Bottom(r)-w/2,G.Width(r),w/2,Color.black,null,s.get(NLeftMessage,gb.poolSize));
    	}
     }
	// draw a symbol for each player
    private void DrawPlayerSymbol(Graphics gc,CarnacBoard gb, Rectangle r, int pl)
    {	
    (gb.getColorMap()[pl]==0 ? CarnacChip.redSymbol.image : CarnacChip.blackSymbol.image).centerImage(gc,r);
    	int nDolmonds = gb.getNDolmonds(pl);
    	GC.Text(gc,true,G.centerX(r),G.centerY(r),G.Width(r)/2,G.Height(r)/2,
    			veryLtGreen,null,""+nDolmonds);
    	if(nDolmonds>0)
    	{
    	 int sz[][] = gb.getDolmondCounts();
    	 int sz0[] = sz[pl];
    	 String msg = "";
    	 for(int idx = sz0.length-1; idx>=0; idx--)
    	 {
    		if(sz0[idx]==0) { break; }
    		msg += ""+sz0[idx]+" ";
    	 }
    	 GC.Text(gc,false,G.Left(r),G.Bottom(r),G.Width(r),G.Height(r)/5, Color.black,null,msg);
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
    	CarnacChip ch = CarnacChip.getChip(obj);// Tiles have zero offset
    	if(b.reverseXneqReverseY())
    	{	CarnacCell a1 = b.getCell('A',1);
     		switch(ch.getFaceOrientation())
    		{
     		default: throw G.Error("not expected");
     		case Up: break;
    		case Horizontal:	
        		{
        		CarnacCell a2 = a1.exitTo(CarnacBoard.CELL_RIGHT());
        		int dx = b.cellToX(a1)-b.cellToX(a2);
        		int dy = b.cellToY(a1)-b.cellToY(a2);

    			xp -= dx;
    			yp -= dy;
        		}
    			break;
    		case Vertical:
           		{
           		CarnacCell a2 = a1.exitTo(CarnacBoard.CELL_UP());
           		int dx = b.cellToX(a1)-b.cellToX(a2);
        		int dy = b.cellToY(a1)-b.cellToY(a2);

    			xp -= dx;
    			yp += dy;
           		}
           		break;
    		}
    	}
    	ch.drawChip(g,this,(int)(b.adjustedCellSize()*SIZE_ADJUST*sizeRect.value),xp,yp,null);
     }

    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    public Point spriteDisplayPoint()
	{   return(new Point(G.Right(boardRect)-SQUARESIZE/2,G.Bottom(boardRect)-SQUARESIZE/2));
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
    { boolean backgroundReview = reviewMode() && !mutable_game_record;
      // erase
      GC.setColor(gc,backgroundReview ? reviewModeBackground : boardBackgroundColor);
      //G.fillRect(gc, fullRect);
     CarnacChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(backgroundReview)
      {	 
       CarnacChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     b.rules.board.image.centerImage(gc, boardRect);
      
      setBoardParameters(b,boardRect); 

      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,b.rules.gridColor);
    }
    private CarnacCell reverseViewCell(CarnacCell c)
    {
    	CarnacChip top = c.topChip();
		switch(top.getFaceOrientation())
		{	
		default: return(c);
		case Horizontal: return(c.exitTo(CarnacBoard.CELL_RIGHT()));
		case Vertical: return(c.exitTo(CarnacBoard.CELL_UP()));
		}
    }
    double SIZE_ADJUST = 128/135.0;
    // 
    // the sort order of the cells is such that almost everything overlaps correctly.
    // a few cases have to be redrawn, and if redrawn then recursively other cells
    // might need to be redrawn too.  This double strikes the shadows but oh well.
    //
    // the "repair" algorithm involves redrawing cells that have already been drawn,
    // which has the undesirable side effect of darkening shadows.  If "count" is
    // specified, then instead of actually drawing and redrawing we just count the
    // number of times a cell would be drawn, and on a second pass draw only on what
    // is known to be the last time.
    //
    //int seq = 0;
    private void drawAndRepair(Graphics gc,CarnacBoard gb,Rectangle brect,boolean reverse,
    						CellStack placed,CarnacCell c,boolean count)
    {	//if(reverse) { G.print("Draw "+c); }
    	//if((c.col=='E') && (c.row==2)) 
    	//{	seq++;
    	//	G.print("C "+c+" "+seq);
    	//	seq+=0;
    	//}
    	if(c!=null)
    	{
    	CarnacChip top = c.topChip();
    	if(c.topChip()!=null)
    	{
        CarnacCell d = reverse ? reverseViewCell(c) : c;
        int ypos = G.Bottom(brect) - gb.cellToY(d);
        int xpos = G.Left(brect) + gb.cellToX(d);
        int SQ = (int)(gb.adjustedCellSize()*SIZE_ADJUST*sizeRect.value);
        if(gc!=null) 
        	{ if(count) { d.sweep_counter--; } 
        	  // real drawing here.  if we are counting, only draw on the last instance
        	  if(!count || (d.sweep_counter<=0))
        		{ 
                //StockArt.SmallO.drawChip(gc,this,SQ,xpos,ypos,null);
        		d.drawChip(gc,this,top,SQ,xpos,ypos,null); } 
        	}
        else { if(count) { d.sweep_counter++; }
        
        }
        if(reverse)
        {	CarnacCell next = c.exitTo(CarnacBoard.CELL_LEFT());
        	if(next!=null)
        	{	CarnacCell next1 = next.exitTo(CarnacBoard.CELL_DOWN());
        		if(next1!=null && placed.contains(next1))
        		{	CarnacChip next1Top = next1.topChip();
                	switch(next1Top.getFaceOrientation())
                	{
                	default: throw G.Error("Not expected");
            	    case Horizontal:
            	    case Up:
            	    		break;
                 	case Vertical:
                		{	drawAndRepair(gc,gb,brect,reverse,placed,next1,count);
                			CarnacCell next2 = next1.exitTo(CarnacBoard.CELL_LEFT());
                			{
                				if(next2!=null)
                				{	CarnacCell next3 = next2.exitTo(CarnacBoard.CELL_UP());
                					if(placed.contains(next3))
                						{
                						drawAndRepair(gc,gb,brect,reverse,placed,next3,count);
                						}
                					if(placed.contains(next2))
                						{
                						drawAndRepair(gc,gb,brect,reverse,placed,next2,count);
                						}
                				}
                			}
                		}
                	}
         
        			
        		}
        		{	next1 = next.exitTo(CarnacBoard.CELL_LEFT());	
        			if(placed.contains(next))
        			{
        				drawAndRepair(gc,gb,brect,reverse,placed,next,count);
        			}
        			else if(next1!=null)
        			{	
        				if(  placed.contains(next1) 
        						&& (next1.topChip().getFaceOrientation()==FaceOrientation.Horizontal)
        						)
        				{	drawAndRepair(gc,gb,brect,reverse,placed,next1,count);
        				}
        			}
        		}
         	}
        	next = c.exitTo(CarnacBoard.CELL_UP());
        	if(next!=null && placed.contains(next))
        	{
        		drawAndRepair(gc,gb,brect,reverse,placed,next,count);
        	}
        }
        else 
        {
        switch(top.getFaceOrientation())
        {
        default: throw G.Error("Not expected");
        case Up:
        case Horizontal:
        	{
        	CarnacCell right =c.exitTo(CarnacBoard.CELL_RIGHT());
        	CarnacCell next = right!=null ? right.exitTo(CarnacBoard.CELL_DOWN()) : null;
        	if((next!=null) && placed.contains(next))
        		{
        		drawAndRepair(gc,gb,brect,reverse,placed,next,count);
        		}
        		else if(next!=null)
        		{	
        		next = next.exitTo(CarnacBoard.CELL_RIGHT());
        		if(placed.contains(next))
        		{
        			drawAndRepair(gc,gb,brect,reverse,placed,next,count);
        		}
        		}
        	}
        	break;
        case Vertical:
        	{
            {CarnacCell next = c.exitTo(CarnacBoard.CELL_UP()).exitTo(CarnacBoard.CELL_RIGHT());
               	if(placed.contains(next))
            		{
            		drawAndRepair(gc,gb,brect,reverse,placed,next,count);
            		while(next!=null && (next = next.exitTo(CarnacBoard.CELL_RIGHT()))!=null)
            		{
            		CarnacChip nextTop = next.topChip();
            		if(nextTop==null) { next = null; }
            		if(placed.contains(next)) 
            				{drawAndRepair(gc,gb,brect,reverse,placed,next,count);
            				 if(nextTop.getFaceOrientation()==FaceOrientation.Horizontal)
            				 {
            					 next = next.exitTo(CarnacBoard.CELL_RIGHT());
            				 }
            				}
            		}
            		}}
        	{CarnacCell next = c.exitTo(CarnacBoard.CELL_RIGHT());
           	if(placed.contains(next))
        		{
        		drawAndRepair(gc,gb,brect,reverse,placed,next,count);
        		while(next!=null && (next = next.exitTo(CarnacBoard.CELL_RIGHT()))!=null)
        		{
        		CarnacChip nextTop = next.topChip();
        		if(nextTop==null) { next = null; }
        		if(placed.contains(next)) 
        				{drawAndRepair(gc,gb,brect,reverse,placed,next,count);
        				 if(nextTop.getFaceOrientation()==FaceOrientation.Horizontal)
        				 {
        					 next = next.exitTo(CarnacBoard.CELL_RIGHT());
        				 }
        				}
        		}
        		}}
        }}}
    	}}
        
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, CarnacBoard gb, Rectangle brect, HitPoint highlight)
    {	
       	//
        // now draw the contents of the board and anything it is pointing at
        //
    	boolean reverse = gb.reverseXneqReverseY();
        int SQ = (int)(gb.adjustedCellSize()*SIZE_ADJUST*sizeRect.value);
        CarnacCell lastPlaced = gb.lastPlaced;
        CarnacChip lastPlacedChip = gb.lastPlacedChip;
        boolean tipmode = (lastPlaced!=null)
        					&& (gb.pickedObject==null)
        					&& ((gb.board_state==CarnacState.TIP_OR_PASS_STATE)||(gb.board_state==CarnacState.PUZZLE_STATE))
        					&& (lastPlacedChip.getFaceOrientation()==FaceOrientation.Up);
 
        // for(CarnacCell c = gb.allCells; c!=null; c=c.next) 
    	//{ 
		//int xpos = brect.x + gb.cellToX(c);
		//int ypos = (brect.y + brect.height) - gb.cellToY(c);
		//StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
        //
        //   	}
        int top = G.Bottom(brect);
        int left = G.Left(brect);
        for(CarnacCell c = gb.allCells; c!=null; c=c.next) 
        	{ c.canTip = false; 
         	}
        if(tipmode)
        	{		
			
        	for(int dir = 0; dir<CarnacBoard.CELL_FULL_TURN(); dir++)
        	{
        	if(gb.canTipInDirection(lastPlaced,dir))
        		{	// mark the base level of the cells we could tip to
        			CarnacCell t1 = lastPlaced.exitTo(dir);
        			int ypos1 = top - gb.cellToY(t1);
        			int xpos1 = left + gb.cellToX(t1);
        			t1.canTip = true;
        			StockArt.SmallO.drawChip(gc,this,SQ,xpos1,ypos1,null);
        			CarnacCell t2 = t1.exitTo(dir);
        			int ypos2 = top - gb.cellToY(t2);
        			int xpos2 = left + gb.cellToX(t2);
        			StockArt.SmallO.drawChip(gc,this,SQ,xpos2,ypos2,null);
        			t2.canTip = true;
        			
        		}
        	}
        }

        CellStack placed = gb.placedMenhir;
        placed.sort(reverse);
        for(CarnacCell c = gb.allCells; c!=null; c=c.next) 
        	{ c.sweep_counter=0; 
        	}
        for(int lim = placed.size(),i=0; i<lim;i++)
        {	drawAndRepair(null,gb,brect,reverse,placed,placed.elementAt(i),true);
        }
        for(int lim = placed.size(),i=0; i<lim;i++)
        {	drawAndRepair(gc,gb,brect,reverse,placed,placed.elementAt(i),true);
        }
      
        CarnacCell close = gb.closestCell(highlight,brect);
        if(close!=null)
        {
        	boolean canHit = gb.LegalToHitBoard(close);
        	if(canHit)
        	{
        		int xpos = left + gb.cellToX(close);
        		int ypos = top - gb.cellToY(close);
        		if(close.findChipHighlight(highlight,SQ,SQ,xpos,ypos))
        		{
        		if(close.canTip) 
            	  	{ highlight.hitCode = CarnacId.HitTipCell;
            	  	  highlight.arrow = StockArt.DownArrow;
            	  	}
            	  	else
            	  	{	CarnacChip picked = gb.pickedObject;
            	  		highlight.hitCode = close.rackLocation;
            	  		
            	  		if((picked==null)&&!placed.contains(close))
            	  		{	// if this isn't an official location, it must be the other end of a reclining menhir
            	  			CarnacChip ctop = close.topChip();
            	  			switch(ctop.getFaceOrientation())
            	  			{
            	  			default: throw G.Error("Not expecting this");
            	  			case Horizontal:	
            	  					highlight.hitObject = close.exitTo(CarnacBoard.CELL_LEFT());
            	  					break;
            	  			case Vertical:
            	  					highlight.hitObject = close.exitTo(CarnacBoard.CELL_DOWN());
            	  					break;
            	  				
            	  			}
            	  		}
            	  		highlight.arrow = (picked!=null) 
            	  		? StockArt.DownArrow 
            	  				: (close.topChip()!=null)?StockArt.UpArrow:null;
            	  	}
        		highlight.awidth = SQ/2;
        		highlight.spriteColor = Color.red;
        		}
        	}
        }
        if(tipmode)
        {
			int ypos0 = G.Bottom(brect) - gb.cellToY(lastPlaced);
			int xpos0 = G.Left(brect) + gb.cellToX(lastPlaced);
			StockArt.LandingPad.drawChip(gc,this,SQ/2,xpos0,ypos0-SQ,null);
        }
    }
     public void drawAuxControls(Graphics gc,CarnacBoard gb,HitPoint highlight)
    { 
       DrawReverseMarker(gc,gb,reverseViewRect,highlight);
       DrawChipsetMarker(gc,gb,chipSetRect,highlight);
       sizeRect.draw(gc,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  CarnacBoard gb = disB(gc);
	if(gc!=null)
	{
	// note this gets called in the game loop as well as in the display loop
	// and is pretty expensive, so we shouldn't do it in the mouse-only case
       setBoardParameters(gb,boardRect);
	}
     boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ourTurnSelect = ourTurn ? highlight : null;	// hit if our turn
      HitPoint ourButtonSelect = moving?null:ourTurnSelect;	// hit if our turn and not dragging
      HitPoint vcrSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      CarnacState vstate = gb.getState();
      redrawGameLog(gc,vcrSelect, logRect,  veryLtGreen,boardBackgroundColor,standardBoldFont(),standardBoldFont());
    
      drawBoardElements(gc, gb, boardRect, ourTurnSelect);
      DrawCommonChipPool(gc, gb,poolRect,ourTurnSelect);
      DrawPlayerSymbol(gc,gb,firstPlayerChipRect,0);
      DrawPlayerSymbol(gc,gb,secondPlayerChipRect,1);
 
      GC.setFont(gc,standardBoldFont());
      if (vstate != CarnacState.PUZZLE_STATE)
        {
			handleDoneButton(gc,doneRect,(gb.DoneState() ? ourButtonSelect : null), 
					HighlightColor, rackBackGroundColor);
			
			handleEditButton(gc,editRect,ourButtonSelect, highlight,HighlightColor, rackBackGroundColor);

            if((vstate==CarnacState.TIP_OR_PASS_STATE)||(vstate==CarnacState.CONFIRM_PASS_STATE))
            {
            	passButton.show(gc,ourButtonSelect);
            }
            
        }

 		drawPlayerStuff(gc,(vstate==CarnacState.PUZZLE_STATE),ourButtonSelect,HighlightColor,rackBackGroundColor);
 

        
 		standardGameMessage(gc,0,
            		veryLtGreen,
            		vstate==CarnacState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
            				vstate!=CarnacState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
            goalAndProgressMessage(gc,vcrSelect,s.get(VictoryCondition),progressRect,goalRect);
        
        DrawRepRect(gc,gb.Digest(),repRect);
        drawAuxControls(gc,gb,vcrSelect);
        drawVcrGroup(vcrSelect, gc);

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
        return (new CarnacMovespec(st, player));
    }
    

private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_RACK_BOARD:
    case MOVE_TIP:
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
    	if(hp.hitCode instanceof CarnacId)
        {
        CarnacId hitObject = (CarnacId)hp.hitCode;
		CarnacCell cell = hitCell(hp);
		CarnacChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    default: break;
	    case TopViewButton:
	    case ReverseViewButton:
	    case ZoomSlider:
	    case HitTipCell:
	    case FlatViewButton: break;
  	    case Chip_Pool:
	    	PerformAndTransmit("Pick "+cell.row);
	    	break;
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	if(b.board_state==CarnacState.CONFIRM_TIP_STATE)
	    	{
	    	}
	    	else if(cell.topChip()!=null)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row);
	    		}
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
    {
       
        CellId id = hp.hitCode;
        if (!(id instanceof CarnacId)) { missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
       	CarnacId hitObject = (CarnacId)id;
        CarnacState state = b.getState();
		CarnacCell cell = hitCell(hp);
		CarnacChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        		// handle anything in the vcr group
            throw G.Error("Hit Unknown: %s", id);
            
        	case ZoomSlider: break;
        	case ReverseViewButton:
 	       	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
	       	 generalRefresh();
	       	 break;

        	case FlatViewButton:
        		showChipSet = currentChipSet ^= 1;
        		chipSetLock = true;
        		break;
        	case HitTipCell:
        		{
        		CarnacCell last = b.lastPlaced;
        		PerformAndTransmit("Tip "+last.col+" "+last.row+" "+cell.col+" "+cell.row);
        		}
        		break;
        		
          case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_TIP_STATE:
	    		PerformAndTransmit("Untip"); 
	    		break;
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col + " " + cell.row);
				}
				break;
			}
			break;
			
         case Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
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
        }}
    }


    public void setBoardParameters(CarnacBoard gb,Rectangle r)
    {	
       	switch(gb.rules)
       	{
       	default: throw G.Error("Not expected");
       	case carnac_14x9:
	       	{
	       	double LL[] = {0.187,0.429};
	       	double LR[] = {0.755,0.183};
	       	double UL[] = {0.35,0.919};
	       	double UR[] = {0.885,0.73};
		    gb.SetDisplayParameters( LL,LR,UL,UR);
		    SIZE_ADJUST = 128/135.0;
	       	}
		    break;
       	case carnac_10x7:
       		{
	       	double LL[] = {0.29,0.44};
	       	double LR[] = {0.670,0.25};
	       	double UL[] = {0.40,0.84};
	       	double UR[] = {0.76,0.68};
		    gb.SetDisplayParameters( LL,LR,UL,UR);
		    SIZE_ADJUST = 128/135.0*0.9;
       		}
       		break;
       	case carnac_8x5:
       		{	
    	       	double LL[] = {0.34,0.48};
    	       	double LR[] = {0.642,0.333};
    	       	double UL[] = {0.42,0.77};
    	       	double UR[] = {0.71,0.645};
		    gb.SetDisplayParameters( LL,LR,UL,UR);
		    SIZE_ADJUST = 128/135.0*0.9;
       		}
       	}
	    
   		gb.SetDisplayRectangle(r);
 
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
    public String sgfGameType() { return(Carnac_SGF); }

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
    public SimpleRobotProtocol newRobotPlayer() { return(new CarnacPlay()); }


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
