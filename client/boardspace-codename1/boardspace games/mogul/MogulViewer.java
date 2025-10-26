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
package mogul;

import bridge.*;
import common.GameInfo;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;


import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import vnc.VNCService;

import java.io.PrintStream;
import java.util.*;

import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.DefaultId;
import lib.Drawable;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Text;
import lib.TextChunk;
import mogul.MogulBoard.MogulPlayer;

import static mogul.MogulMovespec.*;

/**
 * 
 * Change History
 *
 * Feb 2013 initial work in progress. 
 *
 */
public class MogulViewer extends CCanvas<MogulCell,MogulBoard> implements MogulConstants
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.90f);
    private Color rackBackGroundColor = new Color(104,205,205);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] textures = null;// background textures
    private static StockArt[] images = null;
    // private state
    private MogulBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a cell in the board area
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    
    private Rectangle chipRect[] = addRect("chip",MAX_PLAYERS);
    private Rectangle cardRect[] = addRect("card",MAX_PLAYERS);
    private Rectangle infoRect = addRect("infoRect");

    public String gameProgressString()
    {
    	return(mutable_game_record ? "Review" : ""+(MogulChip.nCards-b.deck.height()));
    }
    public synchronized void preloadImages()
    {	
       	MogulChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = StockArt.preLoadArt(loader,ImageDir,ImageNames,null);
    	}
        gameIcon = textures[ICON_INDEX];
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
    	MogulMovespec.setTranslations(s);
        MouseColors = new Color[]{Color.yellow,bsOrange,
					Color.blue,Color.magenta,Color.green,Color.red};
        MouseDotColors = new Color[]{Color.black,Color.black,Color.white,
        				Color.white,Color.white,Color.white};

       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        int np = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,3));
        adjustPlayers(np);
        b = new MogulBoard(this,info.getString(GameInfo.GAMETYPE, Mogul_INIT),
        		randomKey,np,getStartingColorMap());
        if(G.debug()) 
        	{ InternationalStrings.put(MogulStrings);
        	  InternationalStrings.put(MogulStringPairs);
        	}
        useDirectDrawing(true);
        doInit(false);
        
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey,b.nPlayers());			// initialize the board
        if(!preserve_history)
    	{
        adjustPlayers(b.nPlayers());
    		startFirstPlayer();
    	}

    }


    private Rectangle createPlayerGroup(commonPlayer pl0,int unitsize,Rectangle chipRect,Rectangle cardRect,int x,int y)
    {	
    	int CX3 = unitsize*3;
        //first player name
    	G.SetRect(chipRect, x,y, CX3, unitsize*2);
    	Rectangle box = pl0.createSquarePictureGroup(x+CX3, y, unitsize*2);
        G.SetRect(cardRect, G.Right(box)+unitsize/2, y,  unitsize*25,G.Height(box));      
        G.union(box, chipRect,cardRect);
    	return(box);
    }
    public Rectangle createPlayerGroup(int player, int x, int y, double d,int unitsize) 
    {
    	commonPlayer pl0 = getPlayerOrTemp(player);
    	Rectangle box = createPlayerGroup(pl0,unitsize,chipRect[player],cardRect[player],x,y);
    	pl0.displayRotation = d;
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
    public void setLocalBounds(int x, int y, int width, int height)
    {	
    	setLocalBoundsV(x,y,width,height,aspects);
    }
    static double aspects[] = {1.2,1.7,1.5};
    
    public double setLocalBoundsA(int x, int y, int width, int height,double aspect)
     {
        G.SetRect(fullRect,x,y,width,height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
    	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	int fh = standardFontSize();
     	int margin = fh/2;
       	int logW = fh*10;		// just a default
    	int chatHeight = selectChatHeight(height);
        int chatW = fh*35;		// just a default
        int logH = fh*10;

    	layout.selectLayout(this, nPlayers,width,height,margin, Math.max(0.5, 0.7-nPlayers*0.09),aspect,
    			fh*1.25,fh*2, 0.25);
 
        Text info = TextChunk.split(s.get(DeckDescriptionStringKey));
        FontMetrics fm = lib.Font.getFontMetrics(standardBoldFont());
        int infoH = info.height(fm);
    	int infoW = info.width(fm);
        int vcrW = fh*16;
        // log and vcr are approximately the same size, make the log at least as wide
        // this helps allocate rectangles efficiently
        logW = Math.max(logW, vcrW);
        int buttonW = vcrW/2;
        int buttonH = buttonW/2;

        layout.placeTheChatAndLog(chatRect,
        		chatW,chatHeight,chatW*2,3*chatHeight/2,logRect,
        		logW,logH,3*logW/2,logH);
 
        layout.placeRectangle(infoRect,infoW,infoH,infoW*2,infoH*2,BoxAlignment.Center,true);
        layout.placeTheVcr(this,vcrW, vcrW*3/2);
        layout.placeRectangle(editRect,buttonW,buttonH,buttonW*3/2,buttonH*3/2,BoxAlignment.Center,true);
       	Rectangle main = layout.getMainRectangle();
    	
      	int left = G.Left(main);
    int top = G.Top(main);
    int right = G.Right(main);
    int bottom = G.Bottom(main);
	int boardAW = right-left;				// available width and height
	int boardAH = bottom-top;
	int boardCellH = (int)(boardAH/(TRACK_DOWN+1.5));
    int boardCellW = boardAW/(TRACK_ACROSS);
    SQUARESIZE = Math.min(boardCellW, boardCellH);
	int stateH = fh*5/2;
	int boardW = (TRACK_ACROSS)*SQUARESIZE;
	int boardH = (TRACK_DOWN)*SQUARESIZE;
	int boardX = left + (boardAW-boardW)/2;
	int boardY = top+stateH+(boardAH-SQUARESIZE-boardH)/2;
	G.SetRect(boardRect, boardX, boardY, boardW, boardH);
    
	int stateX = boardX;
	int stateY = boardY-stateH;
	placeStateRow(stateX, stateY,boardW,stateH,iconRect,stateRect,annotationMenu,noChatRect);
    placeRow(stateX, boardY+boardH,boardW, stateH,goalRect);
    
        setProgressRect(progressRect,goalRect);
        
        positionTheChat(chatRect,Color.white,Color.white);
        generalRefresh();
    return(boardW*boardH);
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
    	MogulChip ch = MogulChip.getChip(obj);// Tiles have zero offset
    	ch.drawChip(g,this,SQUARESIZE,xp,yp,null);
     }


    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }

    Image scaled = null;
    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { boolean review = reviewMode() && !mutable_game_record;
      // erase
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      
      
      if(remoteViewer<0) 
      	{	scaled = images[BOARD_INDEX].getImage().centerScaledImage(gc, boardRect, scaled);
      	}

      //gb.DrawGrid(gc,brect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    private MogulChip playerChip(int pl)
    {
    	return MogulChip.getPlayerChip(b.getColorMap()[pl]);
    }
    private double[] textScale = new double[] {1,1,0,-0.3};
    public Drawable getPlayerIcon(int p)
    {	playerTextIconScale = textScale;
    	return playerChip(p);
    }
    private void drawPlayerCard(Graphics gc,HitPoint highlight,MogulBoard b,MogulBoard.MogulPlayer pl,Rectangle chip,Rectangle card)
    {	int ind = pl.myIndex;
    	MogulChip ch = playerChip(ind);
    	int w = G.Width(chip);
    	int x = G.Left(chip);
    	int bot = G.Bottom(chip);
    	ch.drawChip(gc,this, w, G.centerX(chip), G.Top(chip)+w/2,null);
    	if(b.getState()!=MogulState.Puzzle)
    	{
    	if(pl.hasTakenMoney) 
    		{ 
    		  StockArt.SmallX.drawChip(gc,this,w,x+w/2,bot+w/2,null); 
    		  HitPoint.setHelpText(highlight, chip,OutOfAuction);
    		}
    	else  { HitPoint.setHelpText(highlight,chip,StillInAuction); }
    	}
    	if(pl.myIndex==b.secondPlayer)
    	{ StockArt.VCRForwardStep.drawChip(gc,this,w,x+w/3,bot,null);
    	}
    	if(pl.myIndex==b.startPlayer)
    	{ StockArt.VCRForwardStep.drawChip(gc,this,w,x+w/3,bot,null);
    	}

    	GC.frameRect(gc,Color.black,card);
    }
    private void drawPlayerBoard(Graphics gc,HitPoint highlight,HitPoint any,MogulBoard gb,MogulBoard.MogulPlayer pl,Rectangle r,boolean showChips)
    {	CellId startHitCode = highlight==null?DefaultId.HitNoWhere:highlight.hitCode;
    	int xstep = Math.min((int)(G.Height(r)*0.5),G.Width(r)/5);
    	int xstart = G.Left(r)+xstep/2;
    	int top = G.Top(r);
    	int ypos = top+(int)(G.Height(r)*0.65);
    	int chipYpos =G.Top(r) + (int)(G.Height(r)*0.7);
    	int chipsiz = 2*xstep/3;
    	int chipw = (int)(chipsiz*1.4); 
    	int stackW = xstep;
		boolean showNow = !showChips
				&& (allPlayersLocal() || (pl.myIndex==gb.whoseTurn))
				&& (remoteWindowIndex(any)<0)
				&& (StockArt.Eye.drawChip(gc, this,  chipsiz, xstart,top+chipsiz/2+xstep/8,highlight,MogulId.SeeChips,null))
				;
		HitPoint hitChip = gb.LegalToHitBoard(pl.chips) ? highlight : null;
		if(showNow) 
		{ 	if(!highlight.down) { showNow = false; }
			setDraggingBoard(showNow);	// nothing to do with dragging, disables touch zoom
			highlight.hitObject = pl.chips; 
		}
    	if(showChips||showNow)
    	{ String msg = s.get(ChipHelpText,pl.chips.height());
    	  HitPoint.setHelpText(any,xstep,xstart,ypos,msg);
    	  double excessHeight = Math.max(0.01,0.05-Math.max(0.0,0.03*((pl.chips.height()-6)/10)));

    	  if(remoteViewer>=0) { SQUARESIZE = stackW; }

    	  if(pl.chips.drawStack(gc,this,showNow?null:hitChip,stackW,
    			  xstart,chipYpos,0,0,0.04+excessHeight,null))
    	  {
    		  highlight.setHelpText(s.get(ChipHelpText,pl.chips.height()));
    	  }
    	  GC.setFont(gc, largeBoldFont());
    	  GC.Text(gc,true,xstart-xstep/2+xstep/10,chipYpos,xstep-xstep/5,xstep,
    			  Color.black,null,msg);
     	}
    	else if((hitChip!=null)&&(pl.myIndex==gb.whoseTurn))
    	{	
    		if(GC.handleSquareButton(gc, new Rectangle(xstart-chipw/2,ypos,chipw,chipsiz), 
                		highlight, s.get(PayChip),
                        HighlightColor, rackBackGroundColor))
    		{	highlight.hitObject = pl.chips;
    			highlight.hitCode = MogulId.HitChips;
    		}
    	}
    	pl.chips.rotateCurrentCenter(gc,xstart,chipYpos);
    	xstart += xstep;
    	xstep = xstep*3/4;
    	for(MogulCell card : pl.cards)
    	{	HitPoint hit = gb.LegalToHitBoard(card) ? highlight : null;
    		HitPoint.setHelpText(any,xstep,xstart,ypos,s.get(CardHelpText,card.height()));
    		if(card.drawStack(gc,this, hit, xstep,xstart, ypos, 0, 0, 0.2,null))
    		{
    			highlight.setHelpText(s.get(CardHelpText,card.height()));
    		}
    		xstart += xstep;
    	}
    	CellId endHitCode = (highlight==null)?DefaultId.HitNoWhere:highlight.hitCode;
    	if(startHitCode!=endHitCode)
    	{
       		MogulCell hit = hitCell(highlight);
        	highlight.arrow =hasMovingObject(highlight)
  				? StockArt.DownArrow 
  				: hit.topChip()!=null?StockArt.UpArrow:null;
        	highlight.awidth = SQUARESIZE/2;   		
    	}
    	
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, MogulBoard gb, Rectangle brect, HitPoint highlight,HitPoint any)
    {	CellId startHitCode = highlight==null ? DefaultId.HitNoWhere : highlight.hitCode;
    	int nPlayers = gb.nPlayers();
    	MogulState state = gb.getState();
    	int l = G.Left(brect);
    	int t = G.Top(brect);
    	int w = G.Width(brect);
    	int h = G.Height(brect);
    	int unit = (int)(SQUARESIZE*0.9);
    	int cardSize = (int)(unit*1.9);
    	int S2 = unit/2;
    	// draw the deck
    	{	
    		MogulCell deck = gb.deck;
    		MogulCell auction = gb.auction;
    		MogulCell disc = gb.discards;
    		MogulCell bank = gb.bank;
    		MogulCell pot = gb.pot;
    		boolean endGameState = auction.topChip()==MogulChip.crashCard;
    		if(state==MogulState.Play)
    			{GC.setFont(gc, standardBoldFont());
    			 GC.Text(gc,true,l,(int)(t+h*0.75),w,(int)(h*0.07),
    				Color.yellow,null,
    				s.get(AuctionDescription,gb.auctionCardBackgroundColor(),gb.auctionCardBorderColor()));
    			}
       		int patchXp = (int)(l +w*0.71);
    		int oatchYp = (int)(t + h*0.5);
    		if((state!=MogulState.Puzzle)&&(state!=MogulState.Gameover)) 
    			{ images[PATCH_INDEX].drawChip(gc,this,(int)(unit*2.7),patchXp,oatchYp,null);
    			}
    		HitPoint.setHelpText(any,unit,patchXp,oatchYp,s.get(ChipHelpText,pot.height()));
 
    		
    		int ypos = t+h/2;
    		int cardYPos = t+(int)(h*0.55);
    		int discardx = l+(int)(w*0.33);
    		int mainCardx = l+(int)(w*0.5);
    		{ 
    		HitPoint.setHelpText(any,unit*2,discardx,cardYPos,s.get(DiscardDescriptionString,disc.height()));
    		disc.drawBacks(gc,this,gb.LegalToHitBoard(disc)?highlight:null,
    				cardSize,discardx,cardYPos,0,0.03,null);
    		}
    		
    		
    		if(endGameState)
    		{	// unplayed cards, spread out face up
        		deck.drawStack(gc,this,gb.LegalToHitBoard(deck)?highlight:null,
        				cardSize,mainCardx,cardYPos,0,0.2,0.2,null);
        		auction.drawStack(gc,this,gb.LegalToHitBoard(auction)?highlight:null,
        				cardSize,l+(int)(w*0.75),ypos, 0,0.005,0.0,null);
   			
    		}else
    		{
    		// unplayed cards, in a stack
    		deck.drawBacks(gc,this,gb.LegalToHitBoard(deck)?highlight:null,
    				cardSize,mainCardx,cardYPos,0,0.02,null);
    		auction.drawStack(gc,this,gb.LegalToHitBoard(auction)?highlight:null,
    				cardSize,l+(int)(w*0.52),ypos, 0,0.005,0.0,null);
    		}
    		
    		{
    		// stack of chips that can be bought for points
    		int bankx = l+(int)(w*0.21);
    		int banky = t+(int)(h*0.73);
    		HitPoint.setHelpText(any,unit,bankx,banky,s.get(BankDescriptionString,bank.height()));
    		bank.drawStack(gc,this,gb.LegalToHitBoard(bank)?highlight:null,
    				(int)(unit*1.0),bankx,banky,0,0.05,null);
    		}
    		
    		{
   			Rectangle rsellnone = new Rectangle(patchXp-unit,oatchYp+unit,unit*2,S2);
   			GC.setFont(gc,standardBoldFont());
    		switch(state)
    		{
    		case WonAuction:
    			Rectangle rtake = new Rectangle(patchXp-unit,oatchYp-unit-S2,unit*2,S2);
    			if(GC.handleSquareButton(gc, rtake, 
                		highlight, s.get(TakeCardString,gb.auctionCardBackgroundColor()),
                        HighlightColor, rackBackGroundColor))
    			{	highlight.hitCode = MogulId.HitAuction;
    				highlight.hitObject = auction;
    			}
				//$FALL-THROUGH$
			case SellCard:
    			{
    			
    			int cardstosell = gb.cardsAvailableForSale();
    			if(cardstosell>0)
    			{
        		Rectangle rsellall = new Rectangle(patchXp-unit,oatchYp-unit,unit*2,S2);
        		if(GC.handleSquareButton(gc, rsellall, 
                		highlight, s.get(SellAllCardsString,gb.auctionCardBorderColor()),
                        HighlightColor, rackBackGroundColor))
    			{	highlight.hitCode = MogulId.HitSellAll;
    				highlight.hitObject = auction;
    			}}}
    			// fall through
				//$FALL-THROUGH$
			case SellOneCard:
       			{ 
       			int cardstosell = gb.cardsAvailableForSale();
      			boolean fallthrough = (state!=MogulState.SellOneCard);
      			if(cardstosell>0)
      			{
           		Rectangle rsellone = new Rectangle(patchXp-unit,oatchYp+S2/2,unit*2,S2);
       			if(GC.handleSquareButton(gc, rsellone, 
                		highlight, s.get(fallthrough?SellSomeCardsString:SellAnotherCardString,gb.auctionCardBorderColor()),
                        HighlightColor, rackBackGroundColor))
    			{	highlight.hitCode = MogulId.HitSellCard;
    				highlight.hitObject = auction;
    			}}
        		
      			if(GC.handleSquareButton(gc, rsellnone, 
                		highlight, s.get(fallthrough ? SellNoneString : DoneSellingString),
                        HighlightColor, rackBackGroundColor))
    			{	highlight.hitCode = MogulId.HitNoSale;
    				highlight.hitObject = auction;
    			}}
    			break;
    		case Play:
        		if(pot.height()==0)
        		{
           			if(GC.handleSquareButton(gc, rsellnone, 
                    		highlight, s.get(NoChipsString),
                            HighlightColor, rackBackGroundColor))
        			{	highlight.hitCode = MogulId.HitPot;
        				highlight.hitObject = auction;
        			}
        		}
     			break;
    		default: break;
    		}

      		pot.drawStack(gc,this,gb.LegalToHitBoard(pot)?highlight:null,
      				unit*1,patchXp,cardYPos,
    				0,0.15,null);
    		}

    	}
    	for(MogulCell cell = gb.allCells; cell!=null; cell=cell.next)
       	{ 
     		int ypos0 = gb.cellToY(cell.col, cell.row);
    		int xpos0 = gb.cellToX(cell.col, cell.row);
            int ypos = t + ypos0;
            int xpos = l + xpos0;
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE*2,xpos,ypos,""+cell.row);
            int max = -9999;
            if(cell.height()>0)
            	{ 	
            		for(int i=0;i<nPlayers;i++) 
            		{ int vp = gb.scoreForPlayer(i); 
            		  if(gb.getCell(vp)==cell) { max = Math.max(vp,max); }
            		}
            		HitPoint.setHelpText(any,unit,xpos,ypos,s.get(PointCount,max)); 
            	}
            String msg = null;
            switch(max/TRACK_AROUND)
            {
            default: break;
            case 0: if(max<0) { msg = "- "; } break;
            case 1: msg = "+"; break;
            case 2: msg = "++"; break;
            };
            cell.drawStack(gc,this,gb.LegalToHitBoard(cell)?highlight:null,
            		unit,xpos,ypos,0,0.1,msg);
            
     	}
    	
    	CellId endHitCode = highlight==null ? DefaultId.HitNoWhere : highlight.hitCode;
    	if(endHitCode!=startHitCode)
    	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
    		MogulCell hit = hitCell(highlight);
        	highlight.arrow =hasMovingObject(highlight) 
  				? StockArt.DownArrow 
  				: hit.topChip()!=null?StockArt.UpArrow:null;
        	highlight.awidth = S2;
        	highlight.spriteColor = Color.red;

    	}

    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  GC.Text(gc,true,infoRect,Color.black,null,s.get(DeckDescriptionStringKey));
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    { 
    	MogulBoard gb = disB(gc);
      int nPlayers = gb.nPlayers();
  	if(gc!=null)
  	{
  	// note this gets called in the game loop as well as in the display loop
  	// and is pretty expensive, so we shouldn't do it in the mouse-only case

	      gb.SetDisplayParameters(1.03,1.07,-0.033,-0.05,0);
	      gb.SetDisplayRectangle(boardRect);
  	}
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      MogulState vstate = gb.getState();
      gameLog.playerIcons = true;
      gameLog.redrawGameLog2(gc, ourSelect, logRect, Color.black,boardBackgroundColor,standardBoldFont(),standardPlainFont());
    
        drawBoardElements(gc, gb, boardRect, ot, highlight);

		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

        GC.setFont(gc,standardBoldFont());
		if (vstate != MogulState.Puzzle)
        {
			handleEditButton(gc,messageRotation,editRect,select,highlight,HighlightColor,
                            rackBackGroundColor);
                }
        drawHiddenWindows(gc, highlight);		// draw before the main screen draw, so the animations will see the main
		
		for(int i=0;i<nPlayers;i++)
		{	boolean isSpectator = isSpectator();
			boolean show = allowed_to_edit
							|| ((isSpectator||isPassAndPlay())
								 ? false
								 : (i==getActivePlayer().boardIndex));
			commonPlayer pl0 = getPlayerOrTemp(i);
			pl0.setRotatedContext(gc, highlight, false);
			MogulPlayer mpl = gb.getPlayer(i);
			if(mpl!=null)
			{
			drawPlayerCard(gc,highlight,gb,mpl,chipRect[i],cardRect[i]);
			drawPlayerBoard(gc,ot,highlight,gb,mpl,cardRect[i],show);
			}
			pl0.setRotatedContext(gc, highlight,true);
		}

 		drawPlayerStuff(gc,(vstate==MogulState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);



        standardGameMessage(gc,messageRotation,
        		vstate==MogulState.Gameover?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=MogulState.Puzzle,
        				gb.whoseTurn,
        				stateRect);
        playerChip(gb.whoseTurn).drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,ourSelect,s.get(VictoryCondition),progressRect, goalRect);
         
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
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Simultaneous);
        if(replay.animate) { playSounds(mm); }
 
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
        return (new MogulMovespec(st, player));
    }
 

private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
   
     case MOVE_PICK:
    	 playASoundClip(light_drop,100);
    	 break;
     case MOVE_DROP:
      	 playASoundClip(heavy_drop,100);
      	break;
     default: 
    	if(MogulId.opName(m.op)!=null)
    	{playASoundClip(light_drop,100);
    	}    	
    	break;
    }
	
}

 
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof MogulId)// not dragging anything yet, so maybe start
        {

        MogulId hitObject = (MogulId)hp.hitCode;
		MogulCell cell = hitCell(hp);
		MogulChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
 	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
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
    private void doPickDrop(MogulCell cell,MogulId hitObject)
    {
   	 if(b.pickedObject!=null)
	 {
		PerformAndTransmit("Drop "+hitObject.shortName+" "+cell.col+" "+cell.row); 
	 }
	 else
	 {	PerformAndTransmit("Pick "+hitObject.shortName+" "+cell.col+" "+cell.row); 
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
        if(!(id instanceof MogulId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	MogulId hitObject = (MogulId)hp.hitCode;
		MogulCell cell = hitCell(hp);
		MogulState state = b.getState();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case SeeChips: 
        	break;
        case PlayerEye:
        	{
        	int ind = remoteWindowIndex(hp);
        	MogulPlayer mpl = b.getPlayer(ind);
        	if(ind>=0) 
        		{ 
        		  mpl.hide = !mpl.hide;
        		}
        	}
        	break;
        case HitNoSale:
        case HitSellCard:
        case HitSellAll:
        case HitChips:
        case HitPot:
        case HitBank:
        case HitAuction:
        case HitDiscards:
        case HitDeck:
        case HitCards:
        case BoardLocation:	// we hit the board 
        	 
        	if(state==MogulState.Puzzle)  { doPickDrop(cell,hitObject); }
        	 else
        	 {
        	if(hitObject==MogulId.HitCards)
        		{	hitObject = (state==MogulState.SellCard) ? MogulId.HitSellAll : MogulId.HitSellCard;
        		}
        	 PerformAndTransmit(hitObject.shortName);
        	 }
        	 break;
        }
         }
    }

    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parseable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() { return(gameType(gameOver || mutable_game_record)); }
    

    public String gameType(boolean mutable) 
    { 	
    	StringBuffer sb = new StringBuffer();
    	sb.append(b.gametype);
    	sb.append(" ");
    	sb.append(b.randomKey);
    	sb.append(" ");
    	sb.append(b.nPlayers());
    	String part2 = b.setupString();
    	if(mutable) { sb.append(" "); sb.append(part2); }
    	String part3 = sb.toString();
    	// strange test is looking for the mysterious "extra space"
    	return(part3); 
   }
    
    // this is a little dodge to get the full history into the immediate game record
    public void formHistoryString(PrintStream os,boolean includeTimes,boolean startAtZero)
    {
        os.print(gameType(true) + " " + ENDTOKEN+" " + 0 );

        if (History.size() > 0)
        {
            commonMove mv = History.elementAt(0);
            // this was always "start at zero"
            mv.formHistoryTree(os,includeTimes);
        }
    } 
    
    public String sgfGameType() { return(Mogul_SGF); }

    
    static String ENDTOKEN = ".endmogulinit.";
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a mogul init spec
    	long rk = G.LongToken(his);
    	int np = G.IntToken(his);
    	// make the random key part of the standard initialization,
    	// even though games like checkers probably don't use it.
    	StringBuffer buf = new StringBuffer();
    	boolean done=false;
    	String space = "";
    	while(his.hasMoreTokens() && !done)
    	{
    		String next = his.nextToken();
    		if(ENDTOKEN.equals(next)) { done=true; }
    		else { buf.append(space); buf.append(next); space=" ";}
    	}
        b.doInit(token,rk,np,buf.toString());
        adjustPlayers(np);
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

/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically
 *  */
    
    //   public void ViewerRun(int wait)
    //   {
    //       super.ViewerRun(wait);
    //   }


    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer() { return(new MogulPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     *  701 files visited 0 problems
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
            	int np = G.IntToken(st);
            	String rest = st.hasMoreTokens()?G.restof(st):null;
                b.doInit(typ,ran,np,rest);
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
    
    /**
     * support for hidden windows
     * 
     */
    
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
    
    public void drawHiddenWindow(Graphics gc,HitPoint hp,int index,Rectangle r)
    {
    	MogulPlayer pl = b.getPlayer(index);
    	if(pl!=null)
    	{
    	String name = prettyName(index);
    	int fs = G.minimumFeatureSize();
    	int margin = fs/4;
    	int h = G.Height(r)-margin*2;
    	int w = G.Width(r)-margin*2;
    	int l = G.Left(r)+margin;
    	int t = G.Top(r)+margin;
    	int topPart = h/10;
    	int step = Math.min(h/4,w/6);
    	Rectangle stateRect = new Rectangle(l,t,w/2-margin,topPart);
    	Rectangle eyeRect = new Rectangle(l,t+topPart,step,step);
    	Rectangle chipRect = new Rectangle(l+step,t+topPart,step*4/3,step*4/3);
    	Font myfont = SystemFont.getFont(largeBoldFont(), topPart/2);
    	MogulState vstate = b.getState();
    	GC.setFont(gc,myfont);
    	if(remoteViewer<0) { GC.fillRect(gc,rackBackGroundColor,r); }
    	standardGameMessage(gc,
        		vstate==MogulState.Gameover?gameOverMessage(b):s.get(vstate.getDescription()),
        				vstate!=MogulState.Puzzle,
        				b.whoseTurn,
        				stateRect);
 
       	Rectangle info = new Rectangle(l+w/3,t+topPart,2*w/3,topPart*2);
    	GC.Text(gc,true,info,Color.black,null,s.get(DeckDescriptionStringKey));
        
    	MogulChip ch = MogulChip.getPlayerChip(b.getColorMap()[index]);
    	ch.drawChip(gc,this,chipRect,null);
    	GC.Text(gc,true,l+w/2,t,w/2-fs,topPart,Color.black,null,s.get(ServiceName,name));
    	boolean show = !pl.hide;
    	StockArt eye = show ? StockArt.NoEye : StockArt.Eye;
    	eye.drawChip(gc, this,eyeRect, hp, MogulId.PlayerEye);
    	drawPlayerBoard(gc,hp,hp,b,pl,new Rectangle(l,t+topPart+step,w,h-topPart-step),show);
    	if(b.LegalToHitBoard(pl.chips))
    	{
        	GC.setFont(gc,myfont);
    		GC.Text(gc, false, l+step, t+topPart/2,w/3,topPart,
    				Color.red,null,s.get(YourTurnMessage));
    	}
    }}

}

