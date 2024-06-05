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
package medina;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Font;
import javax.swing.JCheckBoxMenuItem;

import common.GameInfo;
import lib.Graphics;
import lib.Image;
import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import vnc.VNCService;

import java.util.*;

import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;

/**
 * 
 * Change History
 *
 * April 2010 added "reverse y" option
 *
*/
public class MedinaViewer extends CCanvas<MedinaCell,MedinaBoard> implements MedinaConstants
{
     // colors

    /**
	 * 
	 */
	 
    // file names for jpeg images and masks
    static final String ImageDir = "/medina/images/";
    static final String Medina_SGF = "Medina"; // sgf game name

    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    private Color chatColor = new Color(0.95f,0.9f,0.8f);
    
    public boolean usePerspective()
    {
        return(getAltChipset()==0);
    }

    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// masked images
    // private state
    private MedinaBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  

    private Rectangle reverseRect = addRect("reverse");
    private Rectangle unownedRect = addRect("unowned");
    
    private Rectangle playerChipRect[] = addRect(".chip",4);
    private Rectangle playerPieceRect[] = addRect(".piece",4);    
    Rectangle playerCardRect[] = addRect(".card",4);
    Rectangle eyeRect[] = addRect(".eye",4);
    private boolean showAllRacks = false;
    JCheckBoxMenuItem showAll = null;

    public boolean concealedMode(int idx,boolean alwaysShow)
    {
        return ((showAllRacks 
        		|| mutable_game_record 
         		|| (isOfflineGame() ? alwaysShow : getActivePlayer().boardIndex==idx)
        		)
         		? false 
        		: true);
    }
    public synchronized void preloadImages()
    {	
       	MedinaChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
      // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
    	}
        gameIcon = textures[ICON_INDEX];
    }

    Color MedinaMouseColors[] = { Color.blue,Color.green,Color.red,Color.yellow};
    Color MedinaMouseDotColors[] = { Color.white,Color.white,Color.white,Color.black};
	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,4));
        super.init(info,frame);
        MouseColors = MedinaMouseColors;
        MouseDotColors = MedinaMouseDotColors;
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        String game = info.getString(GameInfo.GAMETYPE, Variation.Medina_V1.shortName);
        b = new MedinaBoard(game,randomKey,players_in_game,getStartingColorMap(),MedinaBoard.REVISION);
        //useDirectDrawing(); // not tested yet
        doInit(false);

        if(extraactions)
        {
            showAll = myFrame.addOption("Show All Racks", false,deferredEvents);
        }
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gameType());						// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}
        
    }
    int pieceRectX = 12;
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {   int np = nPlayers();
    	commonPlayer pl0 = getPlayerOrTemp(player);
    	int doneW = unitsize*4;
    	Rectangle box = pl0.createRectangularPictureGroup(x+unitsize*3,y,unitsize);
    	Rectangle pieceRect = playerPieceRect[player];
    	Rectangle eye = eyeRect[player];
    	Rectangle chipRect = playerChipRect[player];
    	Rectangle cardRect = playerCardRect[player];
    	Rectangle done = doneRects[player];
    	int boxh = G.Height(box);
    	int cardW = unitsize*3;
        int cardH = cardW*2;
        G.SetRect(done,G.Right(box)+unitsize/2,y+unitsize/2,plannedSeating()?doneW:0,doneW/2);
        G.SetRect(chipRect, x, y, unitsize*2, unitsize*2);
        G.SetRect(cardRect, x, y+unitsize*2, cardW, cardH);
        G.union(box,chipRect,cardRect);
        G.SetRect(eye, G.Right(pl0.picRect),y+boxh-unitsize,unitsize,unitsize);    
        G.SetRect(pieceRect,x+cardW,y+boxh, unitsize*((np==2)?12:10),unitsize*((np==2)?5:4));
        pl0.displayRotation = rotation;
        G.union(box, pieceRect,eye,done);
        return(box);
     }
    
    public void setLocalBounds(int x,int y,int width,int height)
    {
    	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();		// always 3 for triad
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*16;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;

        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 30% of space allocated to the board
    			1.3,	// 1.3:1 aspect ratio for the board
    			fh*2,	// maximum cell size based on font size
    			0.5		// preference for the designated layout, if any
    			);

    	// place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	// however, if that doesn't work out the main rectangle will shrink.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW, minLogH*2);
     	int buttonW = fh*8;
     	layout.placeDoneEdit(buttonW, buttonW*3/2,doneRect,editRect);
        layout.placeTheVcr(this,minLogW,minLogW*3/2);
        int unsize = fh*8;
        layout.placeRectangle(unownedRect,unsize,unsize,unsize,unsize,BoxAlignment.Bottom,true);
        Rectangle main = layout.getMainRectangle();
        int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	boolean rotate = mainW<mainH;
        int nrows = rotate ? 13 : 10;  
        int ncols = rotate ? 10 : 13;

        // calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/(ncols+1),(double)mainH/nrows);
    	CELLSIZE = (int)cs;
    	int C2 = CELLSIZE/2;
    	// center the board in the remaining space
        int stateH = (int)(fh*2.5);
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)(nrows*CELLSIZE);
    	
    	int extraW = Math.max(0, (mainW-CELLSIZE-boardW)/2);
    	int extraH = (mainH-boardH)/2;
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	int stateY = boardY-stateH+C2;
       	layout.returnFromMain(extraW,extraH);
   	
    	G.placeStateRow( boardX,stateY,boardW,stateH,iconRect,stateRect,annotationMenu,viewsetRect,reverseRect,noChatRect);
    	
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	G.SetRect(goalRect, boardX, boardBottom-stateH, boardW, stateH);
    	setProgressRect(progressRect,goalRect);
    	if(rotate)
    	{
    		G.setRotation(boardRect, -Math.PI/2);
    		contextRotation = -Math.PI/2;
    	}
        positionTheChat(chatRect,chatColor,chatColor);
  
    }    	
   
    private void drawCards(Graphics gc,MedinaCell cards,Rectangle r)
    {	int yoff = G.Top(r)+G.Width(r)/3;
    	if(cards.col>='C') { yoff += (3-cards.height())*G.Width(r)/3; }
    	cards.drawStack(gc,this,null,G.Width(r),G.centerX(r),yoff,0,-0.5,null);
    }
    private void drawScaledStack(Graphics gc,commonPlayer pl,HitPoint highlight,MedinaCell c,int x,int y,double siz,double hspace,double vspace,MedinaBoard gb)
    {	int height = Math.min(4,c.height());
    	int scl = Math.min(unitSize(gb),(int)(siz*(1.0+(3-height)*0.3)));
    	int off = (int)(siz*((4.0-height)/4));
    	if(c.drawStack(gc,this,highlight,scl,x,y+off,0,hspace,vspace,null))
    	{
    		highlight.spriteColor = Color.red;
    		highlight.awidth = scl/2;
   			highlight.arrow = (gb.pickedObject!=null)?StockArt.DownArrow:StockArt.UpArrow;
   			if(gb.pickedObject==null)
   			{
   				MedinaChip top = c.topChip();
   				if(top!=null) { highlight.setHelpText(top.file+" : "+c.height()); }
   			}
    	}
    	if(gc==null && pl!=null)
    	{
    		pl.rotateCurrentCenter(c,x, y);
    	}
    }
    private void drawRack(Graphics gc,commonPlayer pl,HitPoint highlight,int index,Rectangle r,MedinaBoard gb,boolean always)
    {	
    	if(concealedMode(index,always))
    	{
    	images[SCREEN_INDEX].centerImage(gc, r);
    		// inhibit the actual drawing and locating, but go
    		// through the motions so the pieces centers are recorded
    		// and animations are pretty.
    		gc = null;
    		highlight = null;
    	}
    	
    	{
    	GC.frameRect(gc, Color.blue, r);
    	int np = nPlayers();
    	MedinaCell palaces[]=gb.palaces[index];
    	double cwd = G.Width(r)/9;
    	int cw = (int)cwd;
    	int ch = G.Height(r)/8;
    	double cs = Math.min(cw,ch);
    	double spac = cs*((np==2)?2.5:4);
    	int bot = G.Top(r)+((np==2)?ch/2:ch);
    	int off = G.Left(r)+cw/2;
       	double spacing = -0.4;
       	int wallbot = G.Top(r);
      	double wallspacing = -0.35;
       	double wspacing = -0.6;
    	Variation variation = gb.variation;
    	
    	boolean canHitDomes = gb.LegalToHitChips(index,gb.domes[index]);
       	drawScaledStack(gc,pl,canHitDomes?highlight:null,gb.domes[index],off,bot,spac,0,wspacing,gb);

       	if(variation==Variation.Medina_V2)
       	{
       	if(gb.nPlayers()<=3)
       		{boolean canHitNeutrals = gb.LegalToHitChips(index,gb.neutralDomes[index]);
       		 drawScaledStack(gc,pl,canHitNeutrals?highlight:null,gb.neutralDomes[index],off,bot+(int)(1.4*spac),spac*0.6,0,-wspacing,gb);
       		}
       	
       	}
       	
       	off += cw;
        
       	boolean canHitStables = gb.LegalToHitChips(index,gb.stables[index]);
       	drawScaledStack(gc,pl,canHitStables?highlight:null,gb.stables[index],off,bot,spac*0.9,0,wspacing,gb);
       	
       	boolean canHitTea = gb.LegalToHitChips(index,gb.teaCards[index]);
       	drawScaledStack(gc,pl,canHitTea?highlight:null,gb.teaCards[index],off,bot+(int)(2.5*cs),spac*0.7,0,wspacing/4,gb);
       	off += cw;
       	
     	
    	for(int i=0; i<palaces.length;i++,off+=cw)
    	{	MedinaCell c = palaces[i];
       		boolean canHitPalace = gb.LegalToHitChips(index,c);
    		drawScaledStack(gc,pl,canHitPalace?highlight:null,c,off,bot,spac,0,wspacing,gb); 
 
    	}
    	
    	boolean canHitMeeple = gb.LegalToHitChips(index,gb.meeples[index]);
       	drawScaledStack(gc,pl,canHitMeeple?highlight:null,gb.meeples[index],off,bot,spac,0,spacing,gb);
       	off += cw;
      	bot -= CELLSIZE;
      	boolean canHitWall = gb.LegalToHitChips(index,gb.walls[index]);

      	drawScaledStack(gc,pl,canHitWall?highlight:null,gb.walls[index],off,wallbot,spac,0,wallspacing,gb);
    	}
    }
        
    private void drawPerspectiveView(Graphics gc,HitPoint highlight,Rectangle brect)
    {	
    	drawViewsetMarker(gc,viewsetRect,highlight);
    }
    
    private void drawReverseView(Graphics gc,HitPoint highlight,Rectangle brect)
    {	
    	StockArt.Rotate.drawChip(gc,this,brect,highlight, MedinaId.ReverseButton);
    }
    
	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, HitPoint highlight, int forPlayer, 
    		Rectangle r,MedinaBoard bd,boolean score)
    {	
        MedinaChip thisChip = MedinaChip.getDome(b.getColorMap()[forPlayer]);
        thisChip.drawChip(gc,this,r,score ? ""+bd.score[forPlayer] : null);
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
    	MedinaChip ch = MedinaChip.getChip(obj);// Tiles have zero offset
    	ch.drawChip(g,this,CELLSIZE,xp,yp,null);
     }



    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }

    int bigNumberPos34[][] = {{'A',1,4}, 
			   {'A',13,1},
			   {'R',1,2},
			   {'R',13,3}
			};
    int bigNumberPos2[][] = {{'A',1,4}, 
			   {'A',12,1},
			   {'P',1,2},
			   {'P',12,3}
			};
    private int[][]bigNumberPos()
    {	return((b.nPlayers()==2)
    			? bigNumberPos2
    			: bigNumberPos34);
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
    Image scaled = null;
    Image background = null;
    
    public void drawFixedBoard(Graphics gc,Rectangle brect)
    { boolean reviewBackground = reviewMode() && !mutable_game_record;
      // erase
      MedinaBoard gb = disB(gc);
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,brect);   
      }
      setBoardParameters(gb,brect,usePerspective());
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     if(remoteViewer<0)
    	 { Image board = images[ gb.boardImageIndex(usePerspective())];
    	   if(board!=background) { scaled = null; }
    	   background = board;
    	   scaled = board.centerScaledImage(gc,   brect, scaled); 

      gb.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
      { int ybase = G.Bottom(brect);
   		int xbase = G.Left(brect);
   		Font giantFont = G.getFont(standardPlainFont(), G.Style.Bold, 60);
   		Color bignum = (gb.variation==Variation.Medina_V1)
   							?new Color(0.7f,0.3f,0.3f)
   							:new Color(0.4f,0.7f,0.2f);
   		// these are ad-hoc coordinates to get us close to where the
   		// corner numbers were in the original artwork.
   		int[][] pos = bigNumberPos();
        for(int corner = 0; corner<pos.length; corner++)
        { int row[] = pos[corner];
          char col = (char)row[0];
          int rw = row[1];
          int rev = gb.reverseY() ? -1 : 1;
          String msg = ""+row[2];
          int xp = xbase + gb.cellToX(col,rw)+rev*((col>'A')?CELLSIZE/2:-CELLSIZE/2);
          int yp = ybase - gb.cellToY(col,rw)+rev*((rw>1)?-CELLSIZE/2:CELLSIZE/4);
        GC.setFont(gc,giantFont);
        //G.frameRect(gc,Color.red,xp,yp,CELLSIZE*2,CELLSIZE*2);
        GC.Text(gc,true,xp-CELLSIZE,yp-CELLSIZE,CELLSIZE*2,CELLSIZE*2,bignum,null,msg);
        }
      }}
    }
    void drawTowerMerchants(Graphics gc,MedinaBoard gb,Rectangle brect)
    {	int ybase = G.Bottom(brect);
		int xbase = G.Left(brect);
		int [][]pos = bigNumberPos();
		int rev = b.reverseY() ? -1 : 1;
    	for(int corner = 0; corner<pos.length; corner++)
        { int row[] = pos[corner];
          char col = (char)row[0];
          int rw = row[1];
          int xp = xbase + gb.cellToX(col,rw)+rev*((col>'A')?3*CELLSIZE/4:-2*CELLSIZE/3);
          int yp = ybase - gb.cellToY(col,rw)+rev*((rw>1)?0:0);

          gb.towerMerchants[row[2]-1].drawStack(gc,this,null,gb.cellSize(),xp,yp,0,-0.5,null);
        }
    }
    
    private void showDestination(Graphics gc,Hashtable<MedinaCell,MedinaCell>dests,MedinaCell cell,int unitSize,int xpos,int ypos)
    {
    	if(dests!=null)
        {
        if(dests.get(cell)!=null)
        	{int ndests = dests.size();
        	 int sz = ndests>8?unitSize:(ndests<=4)?unitSize*3:unitSize*2;
        	 StockArt.SmallO.drawChip(gc,this,sz,xpos,ypos,null);
        	}
        }
    }
    
   int unitSize(MedinaBoard gb)
   {
	   return (int)(gb.cellSize()*(usePerspective()?1.69:1.48));
   }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, MedinaBoard gb, Rectangle brect, HitPoint highlight)
    {      
    	//setBoardParameters(b,boardRect,usePerspective());

     	boolean moving = hasMovingObject(highlight);
     	Hashtable<MedinaCell,MedinaCell> dests = moving ? gb.getDests() : null;
     	// this is an ad hoc adjustment so we don't have to mess with the scales already established for th echips
     	int unitSize = unitSize(gb);
     	int ybase = G.Bottom(brect);
     	int xbase = G.Left(brect);
     	drawTowerMerchants(gc,gb,brect);
     	Enumeration<MedinaCell>cells = gb.getIterator(Itype.LRTB);
     	while(cells.hasMoreElements())
       	{	
            MedinaCell cell = cells.nextElement();
            int ypos = ybase - gb.cellToY(cell);
            int xpos = xbase + gb.cellToX(cell);
            char thiscol = cell.col;
            int row = cell.row;
            boolean canhit = gb.LegalToHitBoard(cell);
            showDestination(gc,dests,cell,unitSize,xpos,ypos);
 
            if(cell.drawStack(gc,this,canhit?highlight : null,unitSize,xpos,ypos,0,0.5,null))
            	{
	     		highlight.arrow =moving ? StockArt.DownArrow : StockArt.UpArrow;
	      		highlight.awidth = unitSize/3;
	      		highlight.spriteColor = Color.red;
	      		
            	// draw row and column markers to aid pointing
            	{
            	int xx = (gb.cellToX(thiscol, 0)+gb.cellToX(thiscol, 1))/2;
                int yy = (gb.cellToY(thiscol, 0)+gb.cellToY(thiscol, 1))/2;
                StockArt.SmallO.drawChip(gc,this,unitSize*2,xbase+xx,ybase-yy,null);
                //StockArt.SmallX.drawChip(gc,this,unitSize,xbase+xx,ybase-yy,null);
            	}
            	{
            	int xx = (gb.cellToX('@', row)+gb.cellToX('A', row))/2;
                int yy = (gb.cellToY('@', row)+gb.cellToY('A', row))/2;
                StockArt.SmallO.drawChip(gc,this,unitSize*2,xbase+xx,ybase-yy,null);
               // StockArt.SmallX.drawChip(gc,this,unitSize,xbase+xx,ybase-yy,null);
            	}
 
            	}
    	}
       	{
       	int xpos = G.Right(brect);
       	int ypos = G.Bottom(brect)-CELLSIZE*2;
       	HitPoint high = gb.LegalToHitBoard(gb.teaDiscards)?highlight:null;
       	gb.teaPool.drawStack(gc,this,null,unitSize,xpos,G.Top(brect)+(int)(0.7*G.Height(brect)),0,0.1,null);
      	showDestination(gc,dests,gb.teaDiscards,unitSize,xpos,ypos);
       	if(gb.teaDiscards.drawStack(gc,this,high,unitSize,xpos,ypos,0,0.1,null))
       		{
       		//G.print("High");
       		}
       	}
       	int cw = G.Width(unownedRect);
       	int cl = G.Left(unownedRect);
       	int ct = G.Top(unownedRect);
       	b.unownedPalaces.drawStack(gc, this, null, cw/2, cl+cw/4,ct+cw/8,0,0,-0.5,null);
       	b.unownedTowers.drawStack(gc, this, null, cw/2, cl+cw-cw/4,ct+cw/8,0,0,-0.5,null);
    }
    public void drawPlayerInfo(Graphics gc,HitPoint ot,HitPoint any,int player,MedinaBoard gb)
    {	commonPlayer pl = getPlayerOrTemp(player);
	   	pl.setRotatedContext(gc, ot, false);
        drawRack(gc,pl,ot,player,playerPieceRect[player],gb,visibleChips[player]); 
        drawCards(gc,gb.cards[player],playerCardRect[player]);
        StockArt icon = visibleChips[player] ? StockArt.NoEye : StockArt.Eye;
        if(isOfflineGame())
        {
        if(icon.drawChip(gc, this, eyeRect[player],any, MedinaId.VisibleChip))
        	{
        	any.hit_index = player;
        	}
        }
        if(gb.whoseTurn==player && plannedSeating())
        {
        	handleDoneButton(gc,doneRects[player],(gb.DoneState() ? ot : null), 
					HighlightColor, rackBackGroundColor);
        }
        GC.setFont(gc,largeBoldFont());
        GC.setColor(gc,usePerspective()?Color.yellow:Color.black);
        
        DrawCommonChipPool(gc,ot, player,playerChipRect[player],gb,true);

        pl.setRotatedContext(gc, ot,true);

    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    { 

      MedinaBoard gb = disB(gc);
      int nPlayers = gb.nPlayers();
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      
      gb.buildClusters();
      MedinaState vstate = gb.getState();
      gameLog.redrawGameLog2(gc, ourSelect, logRect, Color.black, boardBackgroundColor, standardBoldFont(), standardPlainFont());

      GC.setRotatedContext(gc,boardRect,highlight,contextRotation);
      drawBoardElements(gc, gb, boardRect, ot);
      GC.unsetRotatedContext(gc,highlight);
       
       drawPlayerStuff(gc,(vstate==MedinaState.PUZZLE_STATE),ourSelect,HighlightColor, rackBackGroundColor);

       {	int tx = G.Left(boardRect)+CELLSIZE;
       		int ty = G.Bottom(boardRect);
       		MedinaChip.WASTE.drawChip(gb.trash.height()>0?gc:null,this,CELLSIZE*2,tx+CELLSIZE/2,ty,null);
       		gb.trash.drawStack(gc,this,null,CELLSIZE,tx,ty,0,0.1,0.3,null);
       }
       for(int i=0;i<nPlayers;i++)
       {
    	   drawPlayerInfo(gc,ot,highlight,i,gb);
       }

       
       if(gc!=null)
       {
       
 
       }
        boolean assigned = plannedSeating();

		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

		if (vstate != MedinaState.PUZZLE_STATE)
        {
			GC.setFont(gc,standardBoldFont());
			if(!assigned) 
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			
			handleEditButton(gc,messageRotation,editRect,select,highlight, HighlightColor, rackBackGroundColor);

        }

		standardGameMessage(gc,gb,messageRotation,stateRect,vstate,gb.whoseTurn);
		DrawCommonChipPool(gc,null,gb.whoseTurn,iconRect,gb,false);
        goalAndProgressMessage(gc,ourSelect,s.get(VictoryCondition),progressRect, goalRect);

        drawPerspectiveView(gc,ourSelect,viewsetRect);
        drawReverseView(gc,ourSelect,reverseRect);
  
         drawVcrGroup(ourSelect, gc);
      
         drawHiddenWindows(gc, highlight);

    }
    private void standardGameMessage(Graphics gc,MedinaBoard gb,double messageRotation,Rectangle stateRect,MedinaState vstate,int whose)
    {
        standardGameMessage(gc,messageRotation,
        		vstate==MedinaState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=MedinaState.PUZZLE_STATE,
        				whose,
        				stateRect);

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
        startBoardAnimations(replay,b.animationStack,unitSize(b),MovementStyle.Chained);
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
        return (new MedinaMovespec(st, player));
    }
    


private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_RACK_BOARD:
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
        if  (hp.hitCode instanceof MedinaId)// not dragging anything yet, so maybe start
        {

        MedinaId hitObject = (MedinaId)hp.hitCode;
		MedinaCell cell = hitCell(hp);
		MedinaChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
        case PalaceLocation:
        case DomeLocation:
        case StableLocation:
        case NeutralDomeLocation:
        case MeepleLocation:
        case TeaCardLocation:
        case TeaDiscardLocation:
        case WallLocation:
        	if(cell.chipIndex>=0)
        	{PerformAndTransmit("Pick "+hitObject.shortName+" "+cell.col+" "+cell.row);
        	}
        	break;
	    case BoardLocation:
	    	if(cell.chipIndex>0)
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
        if(!(id instanceof MedinaId)) 
        { 	missedOneClick = performStandardActions(hp,missedOneClick);
        }
        else {
        missedOneClick = false;
    	MedinaId hitObject = (MedinaId)hp.hitCode;
        MedinaState state = b.getState();
		MedinaCell cell = hitCell(hp);
		MedinaChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case VisibleHiddenChip:
        	visibleHiddenChips[hp.hit_index] = !visibleHiddenChips[hp.hit_index];
        	break;
        case VisibleChip:
        	visibleChips[hp.hit_index] = !visibleChips[hp.hit_index];
        	break;
        case ReverseButton:
        	b.setReverseY(!b.reverseY());
       	 	generalRefresh();
       	 	break;
        case PalaceLocation:
        case DomeLocation:
        case StableLocation:
        case MeepleLocation:
        case NeutralDomeLocation:
        case WallLocation:
         	{PerformAndTransmit("Drop "+hitObject.shortName+" "+cell.col+" "+cell.row);
        	}
        	break;
        case TeaCardLocation:
        case TeaDiscardLocation:
        	{
        	if(b.movingObjectIndex()>=0)
        		{
        		PerformAndTransmit("Drop "+hitObject.shortName+" "+cell.col+" "+cell.row);
        		}
        		else
        		{
            	PerformAndTransmit("Pick "+hitObject.shortName+" "+cell.col+" "+cell.row);
        		}
        	}
        	break;
         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case CONFIRM2_STATE:
			case PLAY_STATE:
			case PLAY_MEEPLE_STATE:
			case DOME_STATE:
			case PLAY2_STATE:
			case PUZZLE_STATE:
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
				}
				break;
			}
			break;
			
         }}
    }


    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     */
    void setBoardParameters(MedinaBoard gb,Rectangle r,boolean perspective)
    {
    	switch(gb.variation)
    	{
    	default: throw G.Error("Not expecting variation %s",gb.variation);
    	case Medina_V2:
    	   	if(perspective)
    	   	{	if(gb.nPlayers()==2)
    	   		{
    	   		double []ll = {0.11,0.22};
    	   		double []lr = {0.88,0.081};
    	   		double []ul = {0.19,0.955};
    	   		double []ur = {0.96,0.82};
    		    gb.SetDisplayParameters(ll,lr,ul,ur);
    	   		}
	    	   	else
	    	   	{
	       	  	double []ll = {0.07,0.21};
	    	  	double []lr = {0.93,0.082};
	    	  	double []ul = {0.16,0.96};
	    	  	double []ur = {0.985,0.885};
	    		gb.SetDisplayParameters(ll,lr,ul,ur);	
	    	   	}
    	   	}
    	   	else
    	   	{	if(gb.nPlayers()==2)
    	   		{
    	   		gb.SetDisplayParameters(
    		    		0.93,		// xscale
    					1.0,		// yscale
    					0.25,		// xoff
    					0.08,		// yoff  
    					0			// rotation		
    					);
    	   		}
    	   		else
    	   		{gb.SetDisplayParameters(
    		    		0.89,		// xscale
    					0.99,		// yscale
    					0.25,		// xoff
    					-0.3,		// yoff  
    					0			// rotation		
    					);
    	   		}
    			
    	   	}
    	   	break;
    	case Medina_V1:
		   	if(perspective)
		   	{
		    double []ll = {0.073,0.34};
	   		double []lr = {0.95,0.19};
	   		double []ul = {0.213,0.838};
	   		double []ur = {1.01,0.72};
		    gb.SetDisplayParameters(ll,lr,ul,ur);
		   	}
		   	else
		   	{
			    gb.SetDisplayParameters(
			    		0.943,		// xscale
						0.73,		// yscale
						0.02,		// xoff
						0.6,		// yoff  
						0			// rotation
						
						);
				
		   	}
		   	break;
    	}
    gb.SetDisplayRectangle(r);
    if(usePerspective() && (gb.variation==Variation.Medina_V2))
    {
    	gb.displayParameters.CELLSIZE = gb.cellSize()*0.65;
    }
}

    public String gameType() { return(b.gameType()); }
    public String sgfGameType() { return(Medina_SGF); }


    
    // interact with the board to initialize a game
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	int np = G.IntToken(his);			// should be the number of players in the game
    	int rev = 1;						// old game records have just the number of players
    	int ran = 0;						// and no random seed
    	if(np>=100) 
    		{ rev = np;				// newer game records start with a revision number whichis >=100 
    		  np = G.IntToken(his); 
    		  ran=G.IntToken(his); 
    		}
        b.doInit(token,ran,np,rev);
        adjustPlayers(np);
    }


    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {   

    	if (target == showAll)
    	{
        showAllRacks = showAll.getState();
        return(true);
    	}

        return(super.handleDeferredEvent(target,command));
     }

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new MedinaPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     *  886 files visited 0 problems
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
            {   b.doInit(value);
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
    
    // create the appropriate set of hidden windows for the players when
    // the number of players changes.
 
    public void adjustPlayers(int np)
    {
        int HiddenViewWidth = 600;
        int HiddenViewHeight = 250;
        
        super.adjustPlayers(np);
        if(RpcService.isRpcServer() || VNCService.isVNCServer() || G.debug())
        {
        createHiddenWindows(np,HiddenViewWidth,HiddenViewHeight);
        }
    }
    
    public String nameHiddenWindow()
    {	return ServiceName;
    }


    public HiddenGameWindow makeHiddenWindow(String name,int index,int width,int height)
	   	{
	   		return new HiddenGameWindow(name,index,this,width,height);
	   	}
    
    public boolean visibleChips[] = new boolean[4];			// used only in the UI
    public boolean visibleHiddenChips[] = new boolean[4];	// used only in the UI

    public void drawHiddenWindow(Graphics gc,HitPoint hp,int index,Rectangle r)
    {	if(index<nPlayers())
    	{
     	String name = prettyName(index);
    	int h = G.Height(r);
    	int w = G.Width(r);
    	int l = G.Left(r);
    	int t = G.Top(r);
    	int topPart = h/10;
    	int topSpace = topPart*2;
    	int textW = w-topSpace*2;
    	int textX = l+topSpace;
    	Rectangle chipRect = new Rectangle(l,t,topSpace,topSpace);
    	Rectangle stateRect = new Rectangle(textX,t+topPart,textW,topPart);
    	Rectangle eyeRect = new Rectangle(l+w-topSpace,t,topSpace,topSpace);
    	Rectangle infoRect = new Rectangle(textX,t,textW,topPart);
    	Rectangle rackRect = new Rectangle(l+topPart,t+topSpace+topPart,w-topSpace,h-2*topSpace);
    	Rectangle alertRect = new Rectangle(textX, t+h-topPart,textW,topPart);

    	GC.setFont(gc,largeBoldFont());
    	if (remoteViewer<0)
    	{
    	GC.fillRect(gc,rackBackGroundColor,r);
    	}
    	MedinaChip ch = MedinaChip.getDome(b.getColorMap()[index]);
    	ch.drawChip(gc,this, chipRect,""+b.score[index]);
    	
    	GC.Text(gc,true,infoRect,Color.black,null,s.get(ServiceName,name));
    	StockArt icon = visibleHiddenChips[index] ? StockArt.NoEye : StockArt.Eye;
        if(icon.drawChip(gc, this, eyeRect,hp, MedinaId.VisibleHiddenChip))
        	{
        	hp.hit_index = index;
        	}
 
        drawRack(gc,null,hp,index,rackRect,b,visibleHiddenChips[index]); 

		standardGameMessage(gc,b,0,stateRect,b.getState(),b.whoseTurn);
		 
    	if(b.whoseTurn==index)
    	{
    		GC.setFont(gc, largeBoldFont());
    		GC.Text(gc, true, alertRect,
    				Color.black,null,s.get(YourTurnMessage));
    	}
    }}
}

