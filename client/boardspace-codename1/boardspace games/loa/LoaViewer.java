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
package loa;

import bridge.Color;
import common.GameInfo;

import com.codename1.ui.geom.Rectangle;

import lib.Graphics;
import lib.Image;
import java.util.Enumeration;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.Tokenizer;
import lib.CellId;
import online.common.OnlineConstants;
import online.game.BoardProtocol;
import lib.GameLayoutManager;

import online.game.commonCanvas;
import online.game.commonMove;
import online.game.commonPlayer;
import online.game.replayMode;
import online.game.sgf.sgf_game;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;

public class LoaViewer extends commonCanvas implements UIC
{	
	static int REVISION = 100;			// revision numbers start at 100
										// revision 100 adds revision numbers and randomkeys
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	static final Color HighlightColor = Color.green;
    static final int EmptyBoard = -1; // highlight points to empty board
    static final Color vcrButtonColor = new Color(0.7f, 0.7f, 0.75f);
    Loa_Board b = null;
    Square_State selectedStone = null; // selected point
    private int CELLSIZE = 0;
    //private Rectangle fullRect = addRect("fullRect");
    //private Rectangle boardRect = addRect("boardRect");
    private Rectangle counts[] = addRect("count",2);
    private Rectangle points[] = addRect("points",2);
    private Rectangle undoRect =  addRect("undo") ;
    private Rectangle repRect = addRect("reprect");
 
    /* end of viewer protocol methods */
    public boolean invalid = true;
    private boolean last_use_grid = true;
    boolean invert_y = true;
    private boolean last_invert_y = true;
    int last_board_size = 0;
    double lineStrokeWidth = 1.0;
   
   private int paintedSize = 0;
   public boolean repositionBoxes()
   {	if(paintedSize!=b.size)
   		{ resetBounds();
   		  return(true);
   		}
     return(false);
   }
   public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unit)
   {	commonPlayer pl = getPlayerOrTemp(player);
   		Rectangle box = createPlayerGroup(pl,counts[player],points[player],x,y,unit);
   		pl.displayRotation = rotation;
   		return(box);
   }
   private Rectangle createPlayerGroup(commonPlayer pl0,Rectangle count,Rectangle point,int playerx,int playery,int unit)
   {  
       Rectangle p0time = pl0.timeRect;
       Rectangle p0xtime = pl0.extraTimeRect;
       Rectangle p0anim = pl0.animRect;
       Rectangle nameRect = pl0.nameRect;
       Rectangle picRect = pl0.picRect;
       Rectangle box = pl0.playerBox;
       //player1 balls
       G.SetRect(nameRect,playerx, playery, unit * 3, (unit*2/3));

       // player portrait
       G.SetRect(picRect,G.Left( nameRect),G.Bottom( nameRect), unit * 5/2,unit * 5/2);      

       // time display for player1
       G.SetRect(p0time, G.Right(nameRect),G.Top( nameRect),3*unit/2,G.Height( nameRect));
       
       G.SetRect(p0xtime,G.Left( p0time),G.Bottom(p0time),G.Width( p0time),G.Height(p0time));
       
       // player1 piece count
       G.SetRect(count,G.Right( picRect),G.Bottom(p0xtime),unit,3*unit/4);
       
       G.AlignLeft(point,G.Bottom( count),count);

       G.SetRect(p0anim, G.Right(picRect),G.Top(picRect),unit/2,unit/2);
       
       G.SetHeight(box, -1);
       G.union(box, picRect,p0time,nameRect);
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
   	int minLogW = fh*15;	
   	int vcrW = fh*18;
      	int minChatW = fh*35;	
       int minLogH = fh*10;	
       int margin = fh/2;
       int nrows =  b.boardSize();
       int buttonW = fh*8;
       	// this does the layout of the player boxes, and leaves
   	// a central hole for the board.
   	//double bestPercent = 
   	layout.selectLayout(this, nPlayers, width, height,
   			margin,	
   			0.8,	// 60% of space allocated to the board
   			1.0,	// 1.5:1 aspect ratio for the board
   			fh*3,
   			fh*4,	// maximum cell size
   			0.3		// preference for the designated layout, if any
   			);
       // place the chat and log automatically, preferring to place
   	// them together and not encroaching on the main rectangle.
   	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*3/2,3*chatHeight/2,logRect,
   			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    layout.placeRectangle(undoRect, buttonW, buttonW/2, BoxAlignment.Right);
   	layout.placeTheVcr(this,vcrW,vcrW*3/2);
    layout.placeRectangle(repRect,fh*12,fh*4,BoxAlignment.Center);
   	Rectangle main = layout.getMainRectangle();
   	int mainX = G.Left(main);
   	int mainY = G.Top(main);
   	int mainW = G.Width(main);
   	int mainH = G.Height(main);
    int stateH = fh*5/2;
   	
   	// calculate a suitable cell size for the board
   	double cs = Math.min((double)mainW/(nrows+1),(double)(mainH-stateH)/(nrows+1));
   	SQUARESIZE = (int)cs;
   	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
   	// center the board in the remaining space
    int stateY = mainY;
   	int boardW = (int)(nrows*SQUARESIZE);
   	int boardH = (int)(nrows*SQUARESIZE);
   	int extraW = Math.max(0, (mainW-boardW)/2);
   	int extraH = Math.max(0, (mainH-boardH-stateH*3)/2);
   	int boardX = mainX+extraW;
   	int boardY = stateY+stateH+extraH;
   	int boardBottom = boardY+boardH;
   	layout.returnFromMain(extraW,extraH);
   	//
   	// state and top ornaments snug to the top of the board.  Depending
   	// on the rendering, it can occupy the same area or must be offset upwards
   	//
    placeStateRow(boardX,stateY,boardW,stateH,iconRect,stateRect,annotationMenu,noChatRect);
   	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
   	lineStrokeWidth = Math.max(1.0, boardW/350.0);
   	// goal and bottom ornaments, depending on the rendering can share
   	// the rectangle or can be offset downward.  Remember that the grid
   	// can intrude too.
   	placeRow( boardX, boardBottom+stateH/2,boardW,stateH,goalRect);       
       setProgressRect(progressRect,goalRect);
       positionTheChat(chatRect,Color.white,Color.white);
  
   }

   int SQUARESIZE = 0;

    public commonMove ParseNewMove(String st, int player)
    {	return new Move_Spec(st, b.whoseTurnStone(),player);
    }

    private Move_Spec currentMove()
    {
        int v = (History.viewStep >= 0) ? (History.viewStep - 1) : (History.size() - 1);

        if (v >= 0)
        {
            return ((Move_Spec) History.elementAt(v));
        }

        return (null);
    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	boolean review = reviewMode();
        Move_Spec m = (Move_Spec) mm;
        if(m.op==M_Undo) 
        	{ if(selectedStone!=null) { selectedStone=null; }
        	else { doUndo(); } 
        	}
        else {
        int step = (b.moveNumber());
        if(!review) { History.viewMove = Math.max(step, History.viewMove);} // maintain the max move for the parents
        mm.setSliderNumString("" + step);

        b.Test_Move(m); //side effect of blessing and setting capture flag

        //System.out.println("Execute "+m);
        if (m.N() == M_Select)
        { // just select a point

            int fx = m.fromX();
            int fy = m.fromY();
            selectedStone = ((fx >= 0) && (fy >= 0)) ? b.getSquare(fx, fy) : null;
        }
        else
        {	mm.player = b.whoseTurn;
            b.Execute(mm,replayMode.Replay);
        }
        if(!review) { setDoneState(false); }
        generalRefresh();
        GameOverNow();
        }
        return (true);
    }

    public boolean allowPartialUndo()
    {	// there's never any partial move in the LOA implementation
    	return(false);
    }
    public commonMove EditHistory(commonMove m)
    { // add to history, but also edit history so redundant moves are eliminated

        Move_Spec newmove = (Move_Spec) m;
        switch(newmove.N()) 
        {
        case M_Select:
        case M_Undo:
            return (null);
        default:
        	return (newmove);
        }
    }
    public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	b.doInit(token,his.longToken(),his.intToken());
    	}

    public synchronized void ReplayGame(sgf_game ga)
    {	// we need to make sure the state is "standard" so lax sgf files can assume that
    	// as a default.
    	b.setCurrentSetup("standard");	// make sure we start as a standard setup
    	super.ReplayGame(ga);
    }
    
    public String gameType() { return(b.gameTypeString()); }
    public String sgfGameType() { return(LOA_SGF); }
    


    public void StartDragging(HitPoint hp)
    {	
    }

    public void StopDragging(HitPoint hp)
    {

        CellId hitObject =  hp.hitCode;

        if(!(hitObject instanceof LoaId)) { missedOneClick = performStandardActions(hp,missedOneClick); }
        else
        {
        missedOneClick = false;
        LoaId id = (LoaId)hitObject;
        switch(id)
    	{ 
        default: throw G.Error("Hit unknown object ",id);
        
        case HitBoard:
                Stone Black = b.blackStone();
                Stone White = b.whiteStone();
            Square_State selected = selectedStone;
            Square_State square = (Square_State)hp.hitObject;
            if (hp != null) // did the user click in a square?
                {
                Stone contents = square.contents;

                if (selected == null) // yes (he hit a square), no selected stone?
                    { // yes, (no selected stome);

                        if ((contents == Black) || (contents == White)) // is square occupied?
                        {
                        SelectStone(square); // yes, make this the selected stone
                        }
                    }
                    else if (GameOverNow() || (reviewMode() && !allowed_to_edit))
                    {
                        SelectStone(null);
                    }
                    else
                    { // we have a selected stone

                    LoaMove theMove = b.isLegalMove(selected.x,selected.y, square.x,square.y);
                        SelectStone(null); // un-select the stone

                        if (theMove != null)
                        {
                            PerformAndTransmit(theMove.moveString());
                        }
                    }

                    repaint(20);
                }
        }}
        generalRefresh();
        }
       
    // board things that you can only do when it's your turn
    int dragSizeAdj = 0;
    HitPoint dragPoint = null;

    public BoardProtocol getBoard()   {    return (b);   }
    
    public commonPlayer currentGuiPlayer()
    { // if(reviewMode())
      // { return(viewTurn);
      // }
      // else

        {
            Stone who = b.whoseTurnStone();
            int map[] = b.getColorMap();
            int idx = (who == b.black.color) ? BLACK_INDEX : WHITE_INDEX;
            return(commonPlayer.findPlayerByIndex(players,map[idx]));
        }
    }

    static Image textures[]=null;
    static Image images[]=null;
    static Image icons[] = null;
    public synchronized void preloadImages()
    {	
    	if (textures == null)
    	{ 	// note that for this to work correctly, the images and masks must be the same size.  
        	// Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    		
    		// images and textures are static variables, so they're shared by
    		// the entire class and only get loaded once.  Special synchronization
    		// tricks are used to make sure.
          images = loader.load_images(StonesDir, ImageNames, 
        		  loader.load_images(StonesDir, ImageNames,"-mask")); // load the main images
          textures = loader.load_images(StonesDir,TextureNames);
          icons = loader.load_images(ImageDir,IconNames);
    	}
        gameIcon = icons[ICON_INDEX];
    }
    Color LoaMouseColors[] = { Color.black,Color.white};
    Color LoaMouseDotColors[] = { Color.white,Color.black};
    
    public void init(ExtendedHashtable stuff,LFrameProtocol frame)
    {
        super.init(stuff,frame);
        MouseDotColors = LoaMouseDotColors;
        MouseColors = LoaMouseColors;
        long randomKey = stuff.getInt(OnlineConstants.RANDOMSEED,-1);
        b = new Loa_Board(sharedInfo.getString(GameInfo.GAMETYPE,"LOA"),getStartingColorMap(),randomKey);
        useDirectDrawing(true);
        if(G.debug()) { UIC.putStrings(); }
        doInit(false);
   }

    public void doInit(boolean preserve_history)
    {
        super.doInit(preserve_history);
        b.doInit(b.gametype);
        if(!preserve_history)
    	{ PerformAndTransmit(reviewOnly?EDIT:"Start Black", false,replayMode.Replay);
    	}
    }


    public void drawSprite(Graphics g,int idx,int xp, int yp)
    { //not used
    }


    Color table_color()
    {
        return (table_color);
    }

    private Stone selectedColor()
    {
        Square_State p = selectedStone;

        if (p != null)
        {
            return (p.contents);
        }

        return (null);
    }


    void drawStone(Graphics g,Rectangle rec,Color fg,Color bg,String msg,int idx)
    {	int cx = G.Width(rec)/2;
    	int cy = G.Height(rec)/2;
    	int map[] = b.getColorMap();
    	drawImage(g, images[map[idx]], SCALES[map[idx]],
        		G.Left(rec)+cx,G.Top(rec)+cy,G.Width(rec), 1.0,0.0,msg,false);
        
    }
    public void drawBoardElements(Graphics g,HitPoint p)
    {
    	
    }
    
    public void redrawBoard(Graphics g,HitPoint p)
    {	Loa_Board gb = (Loa_Board)disB(g);
        LoaMove cm = currentMove();
        Rectangle rb = boardRect;
        int n = b.boardSize();
        int height = G.Height(rb);
        int width = G.Width(rb);
        int squaresize = SQUARESIZE;
        boolean isLOAP= (b.setup==Setup_Type.LOAP);
          
        
        int leftmargin = G.Left(rb) + ((width - (n * squaresize)) / 2);
        int topmargin = G.Top(rb) + ((height - (n * squaresize)) / 2);


        int off = Math.max(1, (int) (squaresize * 0.05));
        int wff = off * 2;
        Stone Empty = b.emptyStone();
        Stone White = b.whiteStone();
        Stone Black = b.blackStone();
        Stone Blocked = b.blockedStone();
        int nwhite = 0;
        int nblack = 0;
        int center = n/2;
        int edge = n-2;
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++)
            {
                int xc = leftmargin + (squaresize * x);
                int yc = topmargin + (squaresize * y);
                int s2 = squaresize / 2;
                boolean light = (((x + y) & 1) == (isLOAP?1:0));
                int chipindex = light ? LIGHT_TILE_INDEX : DARK_TILE_INDEX;
                //Color squarecolor =  light ? white_square_color
                //                           : black_square_color;
                if(isLOAP)
                	{ if ((x==center)&&(y==center)) 
                		{ //squarecolor = loap_7_color;
                		  chipindex = GREEN_TILE_INDEX;
                		}
                	else if(((x==edge)&&((y==1)||(y==edge)))
                			|| ((x==1)&&((y==1)||(y==edge))))
                		{ //squarecolor=loap_3_color;	
                		  chipindex = YELLOW_TILE_INDEX;
                		}
                	}
                	
              //  g.setColor(squarecolor);
              //  g.fillRect(xc, yc, squaresize, squaresize);
                if(G.pointInRect(p, xc, yc, squaresize, squaresize)) 
                	{ p.hitCode = LoaId.HitBoard; 
                	  p.hitObject = b.getSquare(x,invert_y ? n-y-1 : y);
                	}
                

                drawImage(g, textures[chipindex], TSCALES[chipindex],
                		xc+s2,yc+s2,squaresize);
                
                {
                    Stone st = b.squareContents(x, invert_y ? (n - y - 1) : y);

                    if (st == Black)
                    {	nblack++;
                        //G.DrawAACircle(g, xc + s2, yc + s2, s2 - off - 2,
                        //    black_stone_color, squarecolor, true);
                        drawImage(g, images[LIGHT_TILE_INDEX], SCALES[LIGHT_TILE_INDEX],
                        		xc+s2,yc+s2,squaresize);

                    }
                    else if (st == White)
                    {	nwhite++;
                        //G.DrawAACircle(g, xc + s2, yc + s2, s2 - off - 2,
                        //    white_stone_color, squarecolor, true);
                        drawImage(g, images[DARK_TILE_INDEX], SCALES[DARK_TILE_INDEX],
                        		xc+s2,yc+s2,squaresize);
                   }
                    else if (st == Blocked)
                    {
                         GC.fillRect(g,blocked_square_color,xc + (off * 3), yc + (off * 3),
                            squaresize - (wff * 3), squaresize - (wff * 3));
                    }
                    else if (st == Empty)
                    {
                    }
                    else
                    {
                        throw G.Error("Color %s not handled",st);
                    }
                }
            }
        int map[] = b.getColorMap();

        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX; i++)
        {
        commonPlayer cpl = getPlayerOrTemp(i);
        cpl.setRotatedContext(g, null, false);
        GC.setFont(g,largeBoldFont());
        drawStone(g,counts[i],black_stone_color,table_color(),
        		" "+String.valueOf(map[i]==0 ? nblack : nwhite),i);

        if(isLOAP)
        { 
          GC.frameRect(g,Color.black,points[map[i]]);
          GC.Text(g,true,points[i],Color.black,null,""+(i==0 ? b.black.loaps_points : b.white.loaps_points));
        }
        
        cpl.setRotatedContext(g, null, true);
        }

        Square_State selected = selectedStone;
        GC.setFont(g,standardBoldFont());
        if (selected != null)
        {
            int x = selected.x;
            int y = invert_y ? (n - selected.y - 1) : selected.y;
            int ox = leftmargin + (squaresize * x) + (squaresize / 2);
            int oy = topmargin + (squaresize * y) + (squaresize / 2);
            GC.setColor(g,selected_arrow_color);

            for (Enumeration<Move_Spec> moves = b.getLegalMoves(selected.x,selected.y);
                    moves.hasMoreElements();)
            {
                LoaMove m = moves.nextElement();
                x = m.toX();
                y = m.toY();

                if (invert_y)
                {
                    y = n - y - 1;
                }

                int dest_x = leftmargin + (squaresize * x) + (squaresize / 2);
                int dest_y = topmargin + (squaresize * y) + (squaresize / 2);

                //log.println("bS " + squaresize + " " + squaresize/6);
                GC.drawArrow(g, ox, oy, dest_x, dest_y, squaresize / 6,lineStrokeWidth);
                //log.println("aS " + squaresize + " " + squaresize/6);
            }
        }

        if ((cm != null) && (cm.N() > 0) /* n<=0 are special values */)
        {
            int last_from_x = cm.fromX();
            int last_to_x = cm.toX();
            int last_from_y = cm.fromY();
            int last_to_y = cm.toY();

            if (invert_y)
            {
                last_to_y = n - last_to_y - 1;
                last_from_y = n - last_from_y - 1;
            }

            GC.setColor(g,lastmove_arrow_color);
            GC.drawArrow(g,
                (squaresize / 2) + leftmargin + (squaresize * last_from_x),
                (squaresize / 2) + topmargin + (squaresize * last_from_y),
                (squaresize / 2) + leftmargin + (squaresize * last_to_x),
                (squaresize / 2) + topmargin + (squaresize * last_to_y),
                squaresize / 6,
                lineStrokeWidth);
        }

    	
        drawPlayerStuff(g,false,p,HighlightColor,	table_color);

        commonPlayer pl = getPlayerOrTemp(b.whoseTurn);
        Stone col = b.whoseTurnStone();
        Stone contents = selectedColor();
        String msg = ((contents == null) || (contents != col))
            ? s.get(SelectSourceMessage, s.get(col.name))
            : s.get(SelectDestMessage);
        standardGameMessage(g,pl.messageRotation(),
        		b.GameOver()?gameOverMessage(gb):msg,
        				true,
        				b.whoseTurn,
        				stateRect);


        drawStone(g,iconRect,null,null,null,b.whoseTurn);
        goalAndProgressMessage(g,p,s.get(LoaGoal),progressRect, goalRect);

       // draw clocks, sprites, and other ephemera
        {
            drawMice(g);
            gameLog.redrawGameLog(g, p, logRect, table_color);

            
            ShowStats(g,p, G.centerX(boardRect), G.Bottom(boardRect) + 10);
            showRectangles(g, p, CELLSIZE);
            //DrawTileSprite(offGC);      //draw the floating tile, if present
        }
        drawVcrGroup(p, g);
		double messageRotation = pl.messageRotation();
        DrawRepRect(g,messageRotation,Color.black, gb.Digest(),repRect);	// Not needed for barca

        handleUndoButton(g,undoRect,p,HighlightColor,vcrButtonColor);

    }

    public void drawFixedElements(Graphics g)
    {	Loa_Board gb = (Loa_Board)disB(g);
            LoaMove cm = currentMove();
            Rectangle rb = boardRect;
            int n = gb.boardSize();
            int height = G.Height(rb);
            int width = G.Width(rb);
            int squaresize = SQUARESIZE;
            boolean isLOAP= (gb.setup==Setup_Type.LOAP);
            
           textures[BACKGROUND_TILE_INDEX].tileImage(g, fullRect);   
            if(reviewMode())
            {	 
             textures[BACKGROUND_REVIEW_TILE_INDEX].tileImage(g,boardRect);   
            }
               
            
            int leftmargin = G.Left(rb) + ((width - (n * squaresize)) / 2);
            int topmargin = G.Top(rb) + ((height - (n * squaresize)) / 2);
            int bottommargin = topmargin + (n * squaresize);


            if (use_grid)
            {
                for (int i = 0; i < n; i++)
                {
                    String number = String.valueOf(invert_y ? (n - i) : (i + 1));
                    String letter = String.valueOf((char) ('A' + i));
                    GC.Text(g, true, leftmargin - (squaresize / 2),
                        topmargin + (i * squaresize), squaresize / 2, squaresize,
                        Color.black, null, number);
                    GC.Text(g, true, leftmargin + (i * squaresize), bottommargin,
                        squaresize, squaresize / 2, Color.black, null, letter);
                }
            }

            int off = Math.max(1, (int) (squaresize * 0.05));
            int wff = off * 2;
            Stone Empty = gb.emptyStone();
            Stone White = gb.whiteStone();
            Stone Black = gb.blackStone();
            Stone Blocked = gb.blockedStone();
            Rectangle[][] rects = new Rectangle[n][n];
            int center = n/2;
            int edge = n-2;
            for (int y = 0; y < n; y++)
                for (int x = 0; x < n; x++)
                {
                    int xc = leftmargin + (squaresize * x);
                    int yc = topmargin + (squaresize * y);
                    int s2 = squaresize / 2;
                    boolean light = (((x + y) & 1) == (isLOAP?1:0));
                    int chipindex = light ? LIGHT_TILE_INDEX : DARK_TILE_INDEX;
                    //Color squarecolor =  light ? white_square_color
                    //                           : black_square_color;
                    if(isLOAP)
                    	{ if ((x==center)&&(y==center)) 
                    		{ //squarecolor = loap_7_color;
                    		  chipindex = GREEN_TILE_INDEX;
                    		}
                    	else if(((x==edge)&&((y==1)||(y==edge)))
                    			|| ((x==1)&&((y==1)||(y==edge))))
                    		{ //squarecolor=loap_3_color;	
                    		  chipindex = YELLOW_TILE_INDEX;
                    		}
                    	}
                    	
                    rects[y][x] = new Rectangle(xc, yc, squaresize, squaresize);
                  //  g.setColor(squarecolor);
                  //  g.fillRect(xc, yc, squaresize, squaresize);
                    
                    drawImage(g, textures[chipindex], TSCALES[chipindex],
                    		xc+s2,yc+s2,squaresize);
                    
                    {
                        Stone st = gb.squareContents(x, invert_y ? (n - y - 1) : y);

                        if (st == Black)
                        {	
                            //G.DrawAACircle(g, xc + s2, yc + s2, s2 - off - 2,
                            //    black_stone_color, squarecolor, true);
                            drawImage(g, images[LIGHT_TILE_INDEX], SCALES[LIGHT_TILE_INDEX],
                            		xc+s2,yc+s2,squaresize);

                        }
                        else if (st == White)
                        {	
                            //G.DrawAACircle(g, xc + s2, yc + s2, s2 - off - 2,
                            //    white_stone_color, squarecolor, true);
                            drawImage(g, images[DARK_TILE_INDEX], SCALES[DARK_TILE_INDEX],
                            		xc+s2,yc+s2,squaresize);
                       }
                        else if (st == Blocked)
                        {
                             GC.fillRect(g,blocked_square_color,xc + (off * 3), yc + (off * 3),
                                squaresize - (wff * 3), squaresize - (wff * 3));
                        }
                        else if (st == Empty)
                        {
                        }
                        else
                        {
                            throw G.Error("Color %s not handled",st);
                        }
                    }
                }

            Square_State selected = selectedStone;
            GC.setFont(g,standardBoldFont());
            if (selected != null)
            {
                int x = selected.x;
                int y = invert_y ? (n - selected.y - 1) : selected.y;
                int ox = leftmargin + (squaresize * x) + (squaresize / 2);
                int oy = topmargin + (squaresize * y) + (squaresize / 2);
                GC.setColor(g,selected_arrow_color);

                for (Enumeration<Move_Spec> moves = gb.getLegalMoves(selected.x, selected.y);
                        moves.hasMoreElements();)
                {
                    LoaMove m = moves.nextElement();
                    x = m.toX();
                    y = m.toY();

                    if (invert_y)
                    {
                        y = n - y - 1;
                    }

                    int dest_x = leftmargin + (squaresize * x) + (squaresize / 2);
                    int dest_y = topmargin + (squaresize * y) + (squaresize / 2);

                    //log.println("bS " + squaresize + " " + squaresize/6);
                    GC.drawArrow(g, ox, oy, dest_x, dest_y, squaresize / 6,lineStrokeWidth);

                    //log.println("aS " + squaresize + " " + squaresize/6);
                }
            }

            if ((cm != null) && (cm.N() > 0) /* n<=0 are special values */)
            {
                int last_from_x = cm.fromX();
                int last_to_x = cm.toX();
                int last_from_y = cm.fromY();
                int last_to_y = cm.toY();

                if (invert_y)
                {
                    last_to_y = n - last_to_y - 1;
                    last_from_y = n - last_from_y - 1;
                }

                GC.setColor(g,lastmove_arrow_color);
                GC.drawArrow(g,
                    (squaresize / 2) + leftmargin + (squaresize * last_from_x),
                    (squaresize / 2) + topmargin + (squaresize * last_from_y),
                    (squaresize / 2) + leftmargin + (squaresize * last_to_x),
                    (squaresize / 2) + topmargin + (squaresize * last_to_y),
                    squaresize / 6,lineStrokeWidth);
            }

    }
    public void drawCanvas(Graphics offGC, boolean complete,HitPoint hp) // paint the board
    {	if(startedOnce)
    {	
        //System.out.println("Paint_board " + repaint_grid);
        //LoaGame game = c.getGame();
        int n = b.boardSize();
        if ((last_board_size != n) || (last_use_grid != use_grid))
        {
            complete = true;
            last_use_grid = use_grid;
            last_board_size = n;
        }

        if (last_invert_y != invert_y)
        {
            complete = true;
            last_invert_y = invert_y;
        }
 
        GC.setFont(offGC,standardBoldFont());
        drawFixedElements(offGC,complete);
 
        redrawClientBoard(offGC,hp);
        if(runTheChat())
        	{ 
        	theChat.redrawBoard(offGC, hp); 
        	drawKeyboard(offGC,hp);
        	}
        drawMice(offGC);

    	DrawArrow(offGC,hp);
     	invalid = false;
     }
     }


    public void SelectStone(Square_State square)
    {
        String str = (square == null) ? "null" : U.LocationToString(square.x,square.y);
        PerformAndTransmit("Select " + str);
    }

    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() { return(new RoboPlay()); }


    /*
     * summary: 12/1/2025
 *  13546 files visited 0 problems
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
            {
                b.doInit(value);
                b.setWhoseTurn(0);
            }

            if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (name.equals(game_property) && value.equalsIgnoreCase("loa"))
            {
            	// the equals sgf_game_type case is handled in replayStandardProps
            }
            else if (name.equals(version_property))
            {
                if (!value.equals("1"))
                {
                	throw G.Error("Game version %s not handled",value);
                }
            }
            else if ("PB".equals(name) || "PW".equals(name))
            {
                int player = "PB".equals(name) ? 0 : 1;
                commonPlayer p = players[player];

                if (p == null)
                {
                    p = players[player] = new commonPlayer(player);

               }
               if((p.trueName==null) || "".equals(p.trueName))
               	{  p.setPlayerName(value,true,this);
               	} 
               else
            	  { Tokenizer tokens = new Tokenizer(value);
                    String first = tokens.nextToken();
                    parsePlayerInfo(p,first,tokens);
            	  	}
               // pb/pw also set time
            }
            else if ("P0".equals(name) || "P1".equals(name))
            {	Tokenizer tok = new Tokenizer(value);
            	commonPlayer p = "P0".equals(name)? players[0] : players[1];
            	parsePlayerInfo(p,tok.nextToken(),tok);
            }
            else if ("W".equals(name) || "B".equals(name))
            {
                commonPlayer p = "B".equals(name) ? players[0] : players[1];
                Tokenizer tokens = new Tokenizer(value);
                String first = tokens.nextToken();

                if(parsePlayerInfo(p,first,tokens)) {}
                else
                {
                    String msg = first + " " + tokens.getRest();
                    b.setWhoseTurn(p.boardIndex);
                    PerformAndTransmit(msg, false,replayMode.Replay);
                }
            }
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