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
package qe;

import javax.swing.JCheckBoxMenuItem;

import common.GameInfo;

import static qe.QEmovespec.*;
import online.common.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import lib.*;

import online.game.*;
import online.game.sgf.sgf_game;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import vnc.VNCService;

// TODO: add proforma color to QE

public class QEViewer extends CCanvas<QECell,QEBoard> implements QEConstants
{	static final long serialVersionUID = 1000;

/** the user interface uses getUIState instead of bb.getState()
     * which provides additional states which are known only to the
     * UI, and never seen in the actual game record.
     * @return
     */
    public QEState getUIState()
    {
    	if(simultaneousTurnsAllowed())
    	{	int myPlayer = getActivePlayer().boardIndex;
    		return(bb.getUIState(myPlayer));

    	}
    	return(bb.getState());
    }
    public QEState getUIState(int myPlayer)
    {
    	if(simultaneousTurnsAllowed())
    	{	return(bb.getUIState(myPlayer));
    	}
    	return(bb.getState());
    }
     
    private boolean canRunNow(commonPlayer pp)
    {
    	if(pp!=null )
    	{
    		QEState state = bb.getUIState(pp.boardIndex);
    		switch(state)
    		{
    		default: return(false);
    		case EphemeralConfirm:
    		case EphemeralRebid:
    		case EphemeralSealedBid:
    			return(true); 
    		}
    	}
    	return(false);
    }
   public boolean allowRobotsToRun(commonPlayer pp)
	{
		if(simultaneousTurnsAllowed() && !canRunNow(pp)) { return false; }
		return true;
	}

public commonMove convertToSynchronous(commonMove m)
{
	switch(m.op)
	{
	default: throw G.Error("Not expecting move %s",m);
	case MOVE_EBID: m.op = MOVE_SECRETBID; break;
	case MOVE_ECOMMIT: m.op = MOVE_DONE; break;
	}
	return(m);
}

public boolean PerformAndTransmit(commonMove m, boolean transmit,replayMode replay)
{
	boolean val = super.PerformAndTransmit(m, transmit, replay);
    if(simultaneousTurnsAllowed())
	{ if(bb.readyToStartNormal())
		{
		bb.normalStart();
		canonicalizeHistory();
		}
	}
    return(val);
}

	private int whoseMove()
	{
		return(simultaneousTurnsAllowed() 
			? getActivePlayer().boardIndex
			: bb.whoseTurn());
	}

	boolean censorship = true;			// master censorship switch
	public boolean censoring() { return(censorship && !mutable_game_record); }
	public boolean censoring(QEPlayer p)
	{
		return(censoring()
				&& (allPlayersLocal()
						? p.publicCensoring 
						: (isSpectator()
								|| (p.index!=getActivePlayer().boardIndex))));
	}
	public boolean censoring(commonMove m)
	{
		return(censoring(bb.getPlayer(m.player)));
	}
     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackgroundColor = new Color(255,230,230);
    private Color rackBackGroundColor = new Color(225,192,182);
    private Color boardBackgroundColor = new Color(220,165,155);
    private Color playerBackgroundColor = new Color(165,165,165);

    private Calculator calculator = null;
    // private state
    QEBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle calculatorRect = addRect(".calculator");
    private Rectangle chipRect[] = addRect("chip",MAX_PLAYERS);
    private Rectangle wonCards[] = addRect("won",MAX_PLAYERS);
    private Rectangle noqeCards[] = addRect(".NoqeRect",MAX_PLAYERS);
    private Rectangle viewBidCards[] = addRect(".ViewBid",MAX_PLAYERS);
    private Rectangle eyeCards[] = addRect(".eye",MAX_PLAYERS);

    private Rectangle openBid = addRect("open bid");
    private Rectangle secretBid = addRect("secret bid");
    private Rectangle pendingCards = addRect("pending cards");
    private Rectangle currentCard = addRect("current card");
    private Rectangle reserveCards = addRect("reserve cards");
    private Toggle scoreCard = new Toggle(this,"scorecard",QEChip.ScoreCard,QEId.ShowScore,true,ScoreSummary);
    private QEChip bigChip = null;
    private commonPlayer viewBid = null;
    
	// private menu items
    private JCheckBoxMenuItem censorshipOption = null;		// rotate the board view
    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	QEChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	gameIcon = QEChip.NoQE.image;
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	int players_in_game = Math.min(chipRect.length,info.getInt(OnlineConstants.PLAYERS_IN_GAME,chipRect.length));
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default

        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	InternationalStrings.put(QEStrings);
        	InternationalStrings.put(QEStringPairs);
        }
         
        censorship = !mutable_game_record;
        censorshipOption = myFrame.addOption("censorship",censorship,deferredEvents);
        
        String type = info.getString(GameInfo.GAMETYPE, QEVariation.qe.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new QEBoard(type,players_in_game,randomKey,QEBoard.REVISION);
        useDirectDrawing(true);
        doInit(false);

    }

    boolean SIMULTANEOUS_PLAY = true;
    public boolean gameHasEphemeralMoves() { return(SIMULTANEOUS_PLAY); }
    
    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        bb.doInit(bb.gametype);						// initialize the board
        bigChip = null;								// take down any overlay
        viewBid = null;
        if(reviewOnly || isTurnBasedGame()) { SIMULTANEOUS_PLAY = false;  setSimultaneousTurnsAllowed(false); }
        if(!preserve_history)
    	{ 
            adjustPlayers(bb.nPlayers());
        	startFirstPlayer();
    	}
    }

    /**
     * update the players clocks.  The normal thing is to tick the clocks
     * only for the player whose turn it is.  Games with a simultaneous action
     * phase need to do something more complicated.
     * @param inc the increment (in milliseconds) to add
     * @param p the current player, normally the player to update.
     */
    public void updatePlayerTime(long inc,commonPlayer p)
    {
    	super.updatePlayerTime(inc,p);
    }
  
	public void doShowSgf()
	{
		if (G.debug() || !censoring())
        {
            super.doShowSgf();
        }
        else
        {
            theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                s.get(CensoredGameRecordString));
        }
	}

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unit)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRect[player];
    	Rectangle noQErect = noqeCards[player];
    	Rectangle eyeRect = eyeCards[player];
    	Rectangle wonRect = wonCards[player];
    	Rectangle done = doneRects[player];
    	Rectangle viewBid = viewBidCards[player];
    	int u2 = unit/2;
    	int chipW = 3*unit;
    	int chipH = 2*unit+u2;
    	int donew = allPlayersLocal()? unit*8:0;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unit+u2);
    	int boxh = G.Height(box);
        G.SetRect(chip,	x,	y, chipW	,chipH	);
        G.SetRect(eyeRect,x,y+chipH,chipH,chipH);
        G.union(box,chip,eyeRect);
        if(bb.players_in_game==5)
        {
        	int bx = G.Right(box);
        	G.SetRect(viewBid,bx,y,boxh,boxh);
        	G.union(box,viewBid);
        }
        int noQEx = G.Right(box);
        G.SetRect(noQErect, noQEx,y,boxh, boxh);
        int donex = noQEx+boxh+u2;
        G.SetRect(done, donex, y,donew,donew/2);
        G.SetRect(wonRect, x, y+boxh, Math.max(unit*30,G.Right(done)-G.Left(box)),unit*10);
        G.union(box, noQErect,wonRect, done);
        pl.displayRotation = rotation;
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
    	int minLogW = fh*25;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        int vcrW = fh*16;
        int nrows = 15;  
        int ncols = 15;

        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.5,	// % of space allocated to the board
    			1.0,	// 1.0:1 aspect ratio for the board
    			fh*0.75,
    			fh*2.5,	// maximum cell size based on font size
    			0.2		// preference for the designated layout, if any
    			);
 
    	// place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	// however, if that doesn't work out the main rectangle will shrink.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    			logRect,minLogW, minLogH, minLogW*3/2, minLogH*3/2);

        layout.placeTheVcr(this,vcrW,vcrW*3/2);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
       
        Rectangle main = layout.getMainRectangle();
        
        int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	int spaceForDone = allPlayersLocal()?0:buttonW;
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)(mainW-spaceForDone)/ncols,(double)mainH/nrows);
    	CELLSIZE = (int)cs;
       	int C2 = CELLSIZE/2;
            	// center the board in the remaining space
        int stateH = fh*5/2;
    	int boardW = (int)(ncols*CELLSIZE);
    	int boardH = (int)((nrows-1)*CELLSIZE);
    	int extraW = (mainW-boardW)/2;
    	int extraH = (mainH-boardH)/2;
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH;
    	int boardBottom = boardY+boardH;
    	int stateY = boardY-stateH+C2;
       	layout.returnFromMain(extraW,extraH);
       	placeRow(boardX,stateY,boardW,stateH,stateRect,annotationMenu,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	int goaly =  boardBottom-stateH+C2;
    	G.SetRect(scoreCard,boardX,goaly-stateH,stateH*2,stateH*2);
    	placeRow( boardX+stateH*2,goaly, boardW-stateH*2, stateH,goalRect);
    	setProgressRect(progressRect,goalRect);
    		
        {
            int l = boardX;
            int t = boardY;
            int sz = CELLSIZE * (int)nrows;
            G.SetRect(calculatorRect,CELLSIZE*3,CELLSIZE*3,CELLSIZE*7,CELLSIZE*10);
            G.SetRect(openBid,(int)(l+0.19*sz),(int)(t+0.58*sz),(int)(0.2*sz),(int)(0.2*sz));
            G.SetRect(reserveCards,(int)(l+0.19*sz),(int)(t+0.18*sz),(int)(0.2*sz),(int)(0.38*sz));
            G.SetRect(secretBid, (int)(l+0.43*sz),(int)(t+0.52*sz),(int)(0.39*sz),(int)(0.30*sz));
            G.SetRect(pendingCards, (int)(l+0.43*sz),(int)(t+0.26*sz),(int)(0.15*sz),(int)(0.15*sz));
            G.SetRect(currentCard, (int)(l+0.65*sz),(int)(t+0.26*sz),(int)(0.15*sz),(int)(0.15*sz));
           }
     
         positionTheChat(chatRect,chatBackgroundColor,chatBackgroundColor);
  
    }


    private void DrawChipPool(Graphics gc, Rectangle r, int player, HitPoint highlight,QEBoard gb,
    		Rectangle viewbid,Rectangle noQE,Rectangle won,Rectangle done,boolean censoring)
    {	QEPlayer pl = gb.getPlayer(player);
    	commonPlayer cp = getPlayerOrTemp(player);
    	int nplayers = gb.nPlayers();
    	QEChip chip = pl.flag;
    	int cy = G.centerY(won);
    	int h = G.Height(won);
    	int left = G.Left(won);
    	int top = G.Top(won);
    	int w = G.Width(won);
    	int unit = w/8;
     	chip.drawChip(gc, this, r,null);
    	if(pl.noQE.drawStack(gc, this, gb.legalToHitChips(pl.noQE)?highlight:null,
    			G.Width(noQE),G.centerX(noQE),G.centerY(noQE),0,0,0,null))
    	{
    		highlight.spriteColor = Color.red;
    		highlight.spriteRect = noQE;
    	}
    	if(bb.players_in_game==5)
    	{	boolean canHit = gb.legalToHitChips(pl.viewBid);
    		if(pl.viewBid.drawStack(gc, this, canHit?highlight:null,
        			G.Width(viewbid),G.centerX(viewbid),G.centerY(viewbid),0,0,0,null))
        	{
        		highlight.spriteColor = Color.red;
        		highlight.spriteRect = viewbid;
        		highlight.hit_index = pl.index;
        		// if we've looked already, it's view again please
        		if(pl.peekedWinningBid!=null) { highlight.hitCode = QEId.ViewAgain; }
        	}
    	}
    	GC.frameRect(gc,Color.black,won);
    	if(censoring)
    	{	
    		GC.drawLine(gc, left,top,left+w,top+h);
    	}
    	// this should be concealed for opponents
    	if(!censoring
    			&&pl.industry.drawStack(gc, this,gb.legalToHitChips(pl.industry)? highlight:null,
    										h/2, left+h/4,top+h/4,0,0,0,null))
    	{
    		highlight.spriteColor = Color.red;
    		highlight.awidth = h/2;
    	}
    	if(!censoring)
    	{
    	int l = left+h/10;
    	int t = top+h-h/4;
    	int ww = h/2;
    	int hh = h/4;
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc, false, l, t, ww*2, hh,Color.black,null, "$"+Calculator.formatDisplay(""+pl.moneySpent));
    	long known[] = pl.knownSpending;
    	long lastKnown[] =pl.lastKnownSpending;
    	l += ww*2+ww/2;
    	int xstep = (2*unit)/Math.max(4,nplayers-1);
    	GC.setFont(gc, standardPlainFont());
    	for(QEPlayer other : gb.players)
    	{
    		if(other!=pl)
    		{
                GC.setFont(gc,largeBoldFont());
                other.flag.draw(gc, this, ww/2,l+xstep/2,t,null);
    			GC.Text(gc, false, l, t+hh/4, xstep*2,hh,Color.black,null,
    					"$ "+Calculator.formatDisplay(""+known[other.index])
    					+ " ($" + Calculator.formatDisplay(""+lastKnown[other.index])
    					+ ")"
    					);
    			l += xstep*2;
    		}
    	}}
    	int tilesize = Math.min(unit+unit/2, w/(pl.tilesWon.height()+1));
    	int xp = left+unit*2;
    	int yp = cy-unit/2;
    	if(pl.tilesWon.drawStack(gc,this,gb.legalToHitChips(pl.tilesWon)?highlight:null,
    			tilesize,xp,yp,0,0.9,0.0,null))
    	{
    		highlight.spriteColor = Color.red;
    		highlight.awidth = h/2;
    	}
    	cp.rotateCurrentCenter(pl.tilesWon,xp,yp);
    	
    	if(done!=null) 
    	{	
    		boolean simultaneous = simultaneousTurnsAllowed();
    		boolean itsme = (gb.whoseTurn==player) || simultaneous; 
            GC.setFont(gc,largeBoldFont());
            
    		if(itsme)
    		{
    		QEState uis = getUIState(player);
    		if(uis!=QEState.EphemeralWait)
    		{
    		HitPoint doneSelect = (itsme && (gb.DoneState()||(uis==QEState.EphemeralConfirm)))
    									? highlight 
    									: null;
    		
            if (handleDoneButton(gc,done,doneSelect,HighlightColor, rackBackGroundColor))
            {	// always display the done button, but only make it active in
            	// the appropriate states
            	if(simultaneous)
            		{ doneSelect.hitCode = QEId.HitEcommitButton;
            		  doneSelect.hit_index = player;
            		}
            }}}
        }
        // set the cell location for animations
        //bb.getPlayerCell(player).current_center_x = G.centerX(r);
        //bb.getPlayerCell(player).current_center_y = G.centerY(r);
    }
    /**
    * sprites are normally a game piece that is "in the air" being moved
    * around.  This is called when dragging your own pieces, and also when
    * presenting the motion of your opponent's pieces, and also during replay
    * when a piece is picked up and not yet placed.  While "obj" is nominally
    * a game piece, it is really whatever is associated with b.movingObject()
    
      */
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
    	QEChip.getChip(obj).draw(g,this,CELLSIZE*2, xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  

    Image scaled = null;
    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { // erase
      QEBoard gb = disB(gc);
      boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     QEChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       QEChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
 
      // draw a picture of the board. In this version we actually draw just the grid
      // to draw the cells, set gb.Drawing_Style in the board init method.  Create a
      // DrawGridCoord(Graphics gc, Color clt,int xpos, int ypos, int cellsize,String txt)
      // on the board to fine tune the exact positions of the text
      if(remoteViewer<0) { scaled = QEChip.Board.getImage(loader).centerScaledImage(gc, boardRect,scaled); }
      gb.SetDisplayRectangle(boardRect);
     }
    
    private void drawStack(boolean showBacks,Graphics gc,QECell cell,HitPoint hit,
    		int size,int x,int y,
    		int steps,double xscale,double yscale,String msg)
    {
        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
    	QECell target = cell;
    	if(showBacks)
    	{
    		target = new QECell(cell);
    		int sz = cell.height();
    		target.reInit();
    		for(int i=0;i<sz;i++) { target.addChip(QEChip.Back);}
    	}
    	if(target.drawStack(gc,this,hit,CELLSIZE*2,
    			x,y,steps,yscale,xscale,msg))
    	{
    		hit.spriteColor = Color.red;
    		hit.awidth = CELLSIZE*2;
    	}
    	if(gc!=null) { cell.copyCurrentCenter(target); }
    }
    public static CalculatorButton QEButtons[] = { 
        	new CalculatorButton(CalculatorButton.id.Ndel,Calculator.Keytop,		0.84,0.4,	0.2),
        	new CalculatorButton(CalculatorButton.id.Clear,Calculator.Keytop,     0.84,0.53,	0.2),
        	new CalculatorButton(CalculatorButton.id.Cancel,Calculator.Keytop,	0.84,0.65,		0.2),

        	new CalculatorButton(CalculatorButton.id.N7,Calculator.Keytop,         0.2,0.4,		0.2),
        	new CalculatorButton(CalculatorButton.id.N8,Calculator.Keytop,         0.38,0.4,	0.2),
        	new CalculatorButton(CalculatorButton.id.N9,Calculator.Keytop,         0.56,0.4,	0.2),
        		
        	new CalculatorButton(CalculatorButton.id.N4,Calculator.Keytop,         0.2,0.53,	0.2),
        	new CalculatorButton(CalculatorButton.id.N5,Calculator.Keytop,         0.38,0.53,	0.2),
        	new CalculatorButton(CalculatorButton.id.N6,Calculator.Keytop,         0.56,0.53,	0.2),

        	new CalculatorButton(CalculatorButton.id.N1,Calculator.Keytop,         0.2,0.65,	0.2),
        	new CalculatorButton(CalculatorButton.id.N2,Calculator.Keytop,         0.38,0.65,	0.2),
        	new CalculatorButton(CalculatorButton.id.N3,Calculator.Keytop,         0.56,0.65,	0.2),
        		
        	new CalculatorButton(CalculatorButton.id.N0,Calculator.Keytop,         0.38,0.78,	0.2),

        	new CalculatorButton(CalculatorButton.id.Text1,"not 0",0.3,0.9,0.45,0.15),
        	new CalculatorButton(CalculatorButton.id.NoQE,Calculator.KeytopW,			0.70,0.79,	0.2),
       		new CalculatorButton(CalculatorButton.id.Ok,Calculator.KeytopW,       		0.70,0.91,	0.2),
        		 
        };

    public void showCalculator(HiddenGameWindow window,int player,int x,int y)
    {	QEPlayer pl = bb.getPlayer(player);
    	G.SetLeft(calculatorRect,x-G.Width(calculatorRect)/2);
    	G.SetTop(calculatorRect,y-G.Height(calculatorRect)/2);
    	Calculator cc = new Calculator(this,QEButtons);
       	cc.setContext(player);
       	cc.setVisible(CalculatorButton.id.NoQE,!pl.hasUsedNoQE()); 
       	
       	
    	long openbid = bb.getOpenBidValue();
    	switch(getUIState(player))
    	{
    	default:
    		cc.setMessage(CalculatorButton.id.Text1,"");
    		break;
    	case OpenBid:
    	case Confirm:
     	case EphemeralSealedBid:
    	case EphemeralRebid:
    	case SealedBid:
    	case EphemeralConfirm:
    	case Rebid:
    		if(openbid>=0) { cc.setMessage(CalculatorButton.id.Text1,
    						s.get(ForbiddenAmount,Calculator.formatDisplay(""+openbid))); 
    					}
    		else { cc.setMessage(CalculatorButton.id.Text1,""); }
    		break;
    	}
     	if(window!=null)
	    	{
      		window.bidCalculator =cc;	
      		window.repaint(0,"calculator");
	    	}
    	else {
    		calculator = cc;
    		repaint();
    		}

    }
    public void drawCalculator(Graphics gc, QEBoard gb, Rectangle crect, HitPoint highlight,Calculator calculator)
    {	//if(calculator==null) { showCalculator(getWidth()/2,getHeight()/2); }
    	if(calculator!=null) { 	calculator.draw(gc,crect,highlight); }
    }
    public void closeCalculator(Calculator calc)
    {
    	if(calc!=null)
    	{
     	if(calc.done)
    		{	boolean cancel = calc.cancel;
    			long bid = calc.value;
    			int contextPlayer = calc.getContext();
     			if(!cancel && (bid!=bb.getOpenBidValue())) 
       			{	switch(getUIState(contextPlayer))
     				{
       				default: break;
     				case OpenBid: 
     					PerformAndTransmit("obid "+bid);
     					break;
     				case EphemeralSealedBid:
     				case EphemeralRebid:
     				case EphemeralConfirm:
     					{
     					QEPlayer pl = bb.getPlayer(contextPlayer);
     					if(!pl.currentBidReady || (pl.currentBid!=bid))
     					{
     					PerformAndTransmit((simultaneousTurnsAllowed() ? "ebid ":"sbid ")+contextPlayer+" "+bid);
     					}}
     					break;
     				case SealedBid:
     				case Rebid: 
     				case Confirm:
     					{
     					QEPlayer pl = bb.getPlayer(contextPlayer);
     					if(!pl.currentBidReady || (pl.currentBid!=bid))
     					{PerformAndTransmit("sbid "+bid);
     					}}
     					break;
     				}
    			}
    		}
    	}
    	repaint();
    }
    double flagScale[] = new double[] {1.0,2.0,0.5,-0.5};
    double indScale[] = new double[] {1.0,1.5,0.5,-0.5};
    Text textSubs[] = {
       		TextGlyph.create(GRName,"xx",QEChip.GR,this,flagScale),
       		TextGlyph.create(UKName,"xx",QEChip.FR,this,flagScale),
       		TextGlyph.create(USName,"xx",QEChip.US,this,flagScale),
       		TextGlyph.create(FRName,"xx",QEChip.FR,this,flagScale),
      		TextGlyph.create(AutosName,"xx",QEChip.Cars,this,indScale),
      		TextGlyph.create(PlanesName,"xx",QEChip.Planes,this,indScale),
      		TextGlyph.create(TrainsName,"xx",QEChip.Trains,this,indScale),
      		TextGlyph.create(BuildingName,"xx",QEChip.Build,this,indScale),
    };       		

    private void drawScoreCard(Graphics gc,QEPlayer p,int x,int y,int xstep,int ystep)
    {
    	QEChip.WhiteBoard.drawChip(gc, this, xstep+xstep/3,0.7,x+ xstep/2, y+ystep/2, null);
    	p.flag.draw(gc, this, xstep/5, x+xstep/7,y+xstep/6,null);
    	String name = prettyName(p.index);
    	GC.setFont(gc,standardBoldFont());
    	GC.Text(gc, true, x, y, xstep, xstep/4,Color.black,null,name);
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,true,x+xstep/20,y+xstep/7,xstep,xstep/10,Color.black,null,s.get(VPMessage,p.effectiveScore()));
    	GC.setFont(gc, standardPlainFont());
    	GC.Text(gc, false, x+xstep/6, y+ystep/12, (int)(xstep*0.9), (int)(ystep*0.9),Color.black,null,
    			TextChunk.colorize(p.getScoreDescription(),s,textSubs));
    }
    /**
	 * draw the board and the chips on it.  This is also called when not actually drawing, to
	 * track the mouse.
	 * 
     * @param gc	the destination, normally an off screen bitmap, or null if only tracking the mouse
     * @param gb	the board being drawn, which may be a robot board if "show alternate board" is in effect
     * @param brect	the rectangle containing the board
     * @param highlight	the mouse location
     */
    public void drawBoardElements(Graphics gc, QEBoard gb, Rectangle brect, HitPoint highlight)
    {	gb.SetDisplayRectangle(brect);
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  
    	for(QECell c = gb.allCells;c!=null; c=c.next)
    	{
        	boolean canHit = gb.legalToHitBoard(c);
    		int x = gb.cellToX(c);
    		int y = gb.cellToY(c);
    		//StockArt.SmallO.drawChip(gc,this,CELLSIZE,x,y,null);
    		if(c.drawStack(gc, this, canHit?highlight:null, CELLSIZE, x, y,0, 0.05,0.05,null))
    		{
    			highlight.spriteColor = Color.red;
    			highlight.awidth = CELLSIZE;
    		}
    	}
    	boolean censorme = censoring();
        // using closestCell is sometimes preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
    	{
    	int ystep = G.Height(reserveCards)/5;
    	int yloc = G.Top(reserveCards)+CELLSIZE;
    	int xloc = G.Left(reserveCards)+CELLSIZE/2;
    	int totalCards = gb.futureAuctions.height();
    	QECell temp = new QECell(gb.futureAuctions);
    	temp.reInit();
    	int card = 0;
    	int nplayers = gb.nPlayers();
    	int firstStep = nplayers==3 ? 2 : 0;
    	while(card<totalCards)
    	{	temp.reInit();
    		for(int j=firstStep;j<nplayers && card<totalCards;j++)
    		{	
    			temp.addChip(gb.futureAuctions.chipAtIndex(card));
    			card++;
    		}
    		firstStep=0;
        	drawStack(censorme,gc,temp,gb.legalToHitBoard(gb.futureAuctions) ? highlight : null,
        			CELLSIZE*3,xloc,yloc,
        			0, 
        			censorme?-0.05:-0.3,
        			censorme?0.05:0.5,
        			null);
        	yloc += ystep;
        	xloc += ystep/10;
    	}
    	
       	if(gc!=null) { gb.futureAuctions.copyCurrentCenter(temp); }
    	}
     	drawStack(censorme,gc,gb.thisRoundAuction,gb.legalToHitBoard(gb.thisRoundAuction) ? highlight : null,CELLSIZE*3,
    			G.centerX(pendingCards),G.Bottom(pendingCards)-CELLSIZE-CELLSIZE/2,
    			0,
    			censorme?-0.05:-0.3,
    			censorme?0.05:0.5,
    			null);
    	drawStack(false,gc,gb.currentAuction,gb.legalToHitBoard(gb.currentAuction)?highlight:null,CELLSIZE*3,
    			G.centerX(currentCard),G.Bottom(currentCard)-CELLSIZE-CELLSIZE/2,
    			0,0.1,0,null);
    	drawStack(false,gc,gb.wasteBasket,gb.legalToHitBoard(gb.wasteBasket)?highlight:null,CELLSIZE*3,
    			G.centerX(secretBid),G.centerY(secretBid),
    			0,0.1,0,null);
        QEState state = gb.getState();
        
        switch(state)
        {
        default: break;
        case Gameover:
        	{
        		int w = (int)(G.Width(brect)*0.7);
        		int h = (int)(G.Height(brect)*0.7);
        		int xstep = w/2;
        		int ystep = h/2;
        		int right = G.Right(brect);
        		int x = (int)(G.Left(brect)+w*0.22);
        		int y = (int)(G.Top(brect)+h*0.22);
        		int x0 = x;
        		for(QEPlayer p : gb.players)
        		{
        			drawScoreCard(gc,p,x,y,xstep,ystep);
        			x += xstep;
        			if(x+xstep>right)
        				{ x=x0; y+=ystep;
        				}
        		}
        	}
        	break;
        case EphemeralWait:
        case EphemeralConfirm:
        case Confirm:
        case SealedBid:
        case EphemeralSealedBid:
        case EphemeralRebid:
        case Rebid:
        case Witness:
        case OpenBid:
        	{
        	QEPlayer ob = gb.getOpenBidPlayer();
        	int obIndex = ob==null ? -1 : ob.index;
        	if(ob!=null) 
        		{  
        			drawBidCard(gc,ob,
        					highlight,
        					openBid,false,false); 
        				}
        	int w = G.Width(secretBid);
        	int h = G.Height(secretBid);
        	int x = G.Left(secretBid);
        	int x0 = x;
        	int y = G.Top(secretBid);
        	int stepx = w/2;
        	int stepy = h/((gb.nPlayers()==5)? 3 : 2);
        	int sizey = (gb.nPlayers()==5) ? (int)(stepy*0.8) : stepy;
        	boolean witnessing = (state==QEState.Witness);
        	y -= (stepy-sizey);
        	for(int i=0;i<gb.nPlayers();i++)
        	{
        	QEPlayer pl = gb.getPlayer(i);
        	if(ob==null || ob!=pl)
        		{	Rectangle r = new Rectangle(x,y,stepx,sizey);
        			commonPlayer ap = getActivePlayer();
        			QEState uis = getUIState(i);
        			HitPoint select = (canHitCard(uis,i) && (allPlayersLocal() || (i==(ap.boardIndex))))?highlight:null;
        			drawBidCard(gc,pl,select,r,witnessing,	
        					witnessing
        						? ob==null || (allPlayersLocal() ? ob.publicCensoring : (isSpectator() || (ap.boardIndex!=obIndex))) 
        						: censoring(pl)); 
        		}
			x += stepx;
			if(x>=x0+w-2) { x = x0; y+= stepy; }
        	}
        	}
        }
    }

    boolean canHitCard(QEState state,int player)
    {	switch(state)
    	{
    	default: return(true);
    	case OpenBid: return(player==bb.whoseTurn);
    	case EphemeralWait:
    	case Confirm:
    	case Witness:
    		return(false);
    	}
    }
    private void drawBidCard(Graphics gc,QEPlayer pl,HitPoint highlight,Rectangle r,boolean witnessing,boolean concealed)
    {
       	int w = G.Width(r);
       	int cx = G.centerX(r);
       	int cy = G.centerY(r);
       	int aw = highlight!=null ? w : w-w/6;

    	QEChip flag = pl.getFLag();
    	int sz = w/5;
    	boolean hit = pl.whiteBoard.drawStack(gc, this, 
    			pl.hasMadeBid()?null : highlight,	// or null if we can't hit it
    			aw,cx,cy,0, 0.1,0.1,null);
       	if(hit)
		{
		highlight.spriteColor = Color.red;
		highlight.awidth = aw;
		}
    	String name = prettyName(pl.index);
     	if(pl.hasOfferedBid()||pl.hasMadeBid())
    		{
     		if(concealed)
    		{	if(!witnessing) { QEChip.BidCard.draw(gc, this,aw, cx,	cy,null,QEId.HitWhiteBoard,null); }   			
    			if(pl.hasOfferedBid())
    			{	GC.setFont(gc, largeBoldFont());
					GC.Text(gc, true, G.Right(r)-sz*2-sz/4,G.centerY(r)-sz/2,sz,sz, Color.blue, null, "?");
    			}    		
            	if(witnessing)
            	{	boolean useNoQE = bb.usingNoQE(pl);
            		boolean highBid = bb.winningBid(pl);
            		if(useNoQE)
            		{
                		GC.setFont(gc, largeBoldFont());
            			GC.Text(gc, true, r, Color.black, null, s.get(NoQEMessage));
            		}
            		if(highBid)
            		{
                   		GC.setFont(gc, largeBoldFont());
                			GC.Text(gc, true, r, Color.black, null, s.get(HighBidMessage));
          			
            		}
            	}
    		}
    		else
    		{
    		GC.setFont(gc, largeBoldFont());
    		String msg = ((pl.currentBid==0)&& !pl.hasUsedNoQE()) 
    				? s.get(NoQEMessage) 
    				: "$ "+Calculator.formatDisplay(""+pl.currentBid);
    		if(pl.hasOfferedBid()) { msg += " ?"; }
    		GC.Text(gc, true, r, Color.black, null, msg);
    		}

    		int flagsz = 3*sz/2;
        	flag.draw(gc, this, flagsz,G.Left(r)+sz,G.Bottom(r)-sz,null);
    		}
    	else 
    		{
    		StockArt.Calculator_Icon.draw(gc,this,sz,cx,cy,null);
    		}
		GC.setFont(gc, largeBoldFont());
		GC.Text(gc, true, G.Left(r), cy-(int)(G.Height(r)*0.3), w,G.Height(r)/5,Color.black,null,name);

    }
    /**
     * draw the main window and things on it.  
     * If gc!=null then actually draw, 
     * If selectPos is not null, then as you draw (or pretend to draw) notice if
     * you are drawing under the current position of the mouse, and if so if you could
     * click there to do something.  Care must be taken to consider if a click really
     * ought to be allowed, considering spectator status, use of the scroll controls,
     * if some board token is already actively moving, and if the game is active or over.
     * <p>
     * This dual purpose (draw, and notice mouse sensitive areas) tends to make the
     * code a little complicated, but it is the most reliable way to make sure the
     * mouse logic is in sync with the drawing logic.
     * <p>
    General GUI checklist
<p>
<li>vcr scroll section always tracks, scroll bar drags
<li>lift rect always works
<li>zoom rect always works
<li>drag board always works
<li>pieces can be picked or dragged
<li>moving pieces always track
<li>stray buttons are insensitive when dragging a piece
<li>stray buttons and pick/drop are inactive when not on turn
*/
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  

       QEBoard gb = disB(gc);
       QEState state = getUIState();
       boolean moving = hasMovingObject(selectPos);
       // 
       // if it is not our move, we can't click on the board or related supplies.
       // we accomplish this by supressing the highlight pointer.
       //
       HitPoint ourTurnSelect = OurMove() ? selectPos : null;
       boolean hasCalculator = calculator !=null;
       //
       // even if we can normally select things, if we have already got a piece
       // moving, we don't want to hit some things, such as the vcr group
       //
       HitPoint buttonSelect = moving||hasCalculator ? null : ourTurnSelect;
       // hit anytime nothing is being moved, even if not our turn or we are a spectator
       HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;
       boolean offline = allPlayersLocal();
       gameLog.redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
       drawBoardElements(gc, gb, boardRect, (hasCalculator||bigChip!=null)?null:ourTurnSelect);
       for(int i=0;i<bb.players_in_game;i++)
       {   QEPlayer pl = gb.getPlayer(i);
       	   commonPlayer p0 = getPlayerOrTemp(i);
       	   p0.setRotatedContext(gc, selectPos, false);
       	   Rectangle box = p0.playerBox;       	   
       	   GC.fillRect(gc, playerBackgroundColor,box);
       	   GC.frameRect(gc, Color.black, box);
    	   DrawChipPool(gc, chipRect[i], i, hasCalculator?null:ourTurnSelect,gb,viewBidCards[i],noqeCards[i],wonCards[i],
    			   offline ? doneRects[i] : null,censoring(pl));
    	   if(offline) { drawEye(gc,eyeCards[i],selectPos,pl.publicCensoring,i); }
       	   p0.setRotatedContext(gc, selectPos, true);
       }
       GC.setFont(gc,standardBoldFont());
       // draw the avatars
		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();
       
       if (state != QEState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);
			if(!offline)
			{
			HitPoint doneSelect = (gb.DoneState()||(state==QEState.EphemeralConfirm) ? buttonSelect : null);
            if (handleDoneButton(gc,doneRect,doneSelect,HighlightColor, rackBackGroundColor))
            {	// always display the done button, but only make it active in
            	// the appropriate states
            	if(simultaneousTurnsAllowed()) 
            		{ doneSelect.hitCode = QEId.HitEcommitButton;
            		  doneSelect.hit_index = whoseMove();
            		}
            }}
        }

		// if the state is Puzzle, present the player names as start buttons.
		// in any case, pass the mouse location so tooltips will be attached.
        drawPlayerStuff(gc,(state==QEState.Puzzle),buttonSelect,HighlightColor,rackBackGroundColor);
 
        // draw player card racks on hidden boards.  Draw here first so the animations will target
        // the main screen location which is drawn next.
        drawHiddenWindows(gc, selectPos);	

        scoreCard.draw(gc,selectPos);

        if(bigChip!=null) { drawBigChip(gc); }
        
        standardGameMessage(gc,messageRotation,
            		state==QEState.Gameover?gameOverMessage(gb):s.get(state.description()),
            				(state!=QEState.Puzzle)&&!simultaneousTurnsAllowed(),
            				gb.whoseTurn,
            				stateRect);
        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(QEVictoryCondition),progressRect, goalRect);
            //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for qe
        
        // draw the vcr controls, last so the pop-up version will be above everything else
        drawVcrGroup(nonDragSelect, gc);
        drawCalculator(gc,gb,calculatorRect,ourTurnSelect,calculator);

    }
    private Text icon(String msg,QEChip target,String msg2)
    {
    	Text icon = TextGlyph.create("xxxxx", target, this,new double[] {2.50, 1,0,-0.25});
    	return TextChunk.join(TextChunk.create(msg),
    				icon,
    				TextChunk.create(msg2));
    }
    private void drawBigChip(Graphics gc)
    {	if(bigChip==QEChip.ViewAgain)
    		{
    		if(viewBid!=null)
    			{
    			if(gc!=null)
    				{
    				int bw = G.Width(boardRect)/2;
    				int bh = G.Height(boardRect)/2;
    				int bx = G.Left(boardRect)+bw/2;
    				int by = G.Top(boardRect)+bh/2;
    				WinningBid bid = bb.getPlayer(viewBid.boardIndex).peekedWinningBid;
    				drawPeek(gc,bid,bx,by,bw,bh);
		
    				}
    			}
    		}
    	else 
    		{ bigChip.drawChip(gc,this,boardRect,null); 
    		} 
    }
    private void drawPeek(Graphics gc,WinningBid bid,int bx,int by,int bw,int bh)
    {
		if(bid!=null && gc!=null)
		{
    	QEChip.Back.drawChip(gc,this,new Rectangle(bx,by,bw,bh),null);
		GC.setFont(gc,largeBoldFont());
		QEPlayer winningPlayer = bb.getPlayer(bid.player); 
		Text msg1= icon(WinningBidMessage,bid.tileWon,"");
		FontMetrics gm = GC.getFontMetrics(gc);
		int offset = msg1.height(gm);
		Text msg2 = icon(ByPlayerMessage,winningPlayer.flag,s.get(IsBidMessage,""+bid.bid));
			GC.Text(gc,true,bx,by-offset/2,bw,bh,Color.black,null,msg1);
			GC.Text(gc,true,bx,by+offset/2,bw,bh,Color.black,null,msg2);
		}
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
    	 // record some state so the game log will look pretty
        
        handleExecute(bb,mm,replay);
                
        /**
         * animations are handled by a simple protocol between the board and viewer.
         * when stones are moved around on the board, it pushes the source and destination
         * cells onto the animationStck.  startBoardAnimations converts those points into
         * animation sprites.  drawBoardElements arranges for the destination stones, which
         * are already in place, to disappear until the animation finishes.  The actual drawing
         * is done by drawSprites at the end of redrawBoard
         */
        startBoardAnimations(replay,bb.animationStack,CELLSIZE*2,MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
		if(replay.animate) { playSounds(mm); }
       return (true);
    }

     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for qe, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
      * destination chip disappear until the animation is finished.
      * @param replay
      */
//     void startBoardAnimations(replayMode replay)
//     {
//        if(replay.animate)
//     	{
//     		double full = G.distance(0,0,G.Width(boardRect),G.Height(boardRect));
//        	while(bb.animationStack.size()>1)
//     		{
//     		QECell dest = bb.animationStack.pop();
//     		QECell src = bb.animationStack.pop();
//    		double dist = G.distance(src.current_center_x, src.current_center_y, dest.current_center_x,  dest.current_center_y);
//    		double endTime = masterAnimationSpeed*0.5*Math.sqrt(dist/full);
    		//
    		// in cases where multiple chips are flying, topChip() may not be the right thing.
    		//
//     		startAnimation(src,dest,dest.topChip(),bb.cellSize(),0,endTime);
//     		}
//     	}
//        	bb.animationStack.clear();
//     } 
 public int adjustAnimationSize(cell<?> to,Drawable top,int size)
 {	
 	if(to.rackLocation==QEId.BoardLocation) 
 		{ return(size/2); 	// flags on board need to display smaller
 		}
	return(size);
 }
 
 void playSounds(commonMove mm)
 {
	 switch(mm.op)
	 {
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
    public commonMove ParseNewMove(String st,int pl)
    {
        return (new QEmovespec(st, pl));
    }
/**
 * prepare to add nmove to the history list, but also edit the history
 * to remove redundant elements, so that indecisiveness by the user doesn't
 * result in a messy game log.  
 * 
 * For all ordinary cases, this is now handled by the standard implementation
 * in commonCanvas, which uses the board's Digest() method to distinguish new
 * states and reversions to past states.
 * 
 * For reference, the commented out method below does the same thing for "Hex". 
 * You could resort to similar techniques to replace or augment what super.EditHistory
 * does, but your efforts would probably be better spent improving your Digest() method
 * so the commonCanvas method gives the desired result.
 * 
 * Note that it should always be correct to simply return nmove and accept the messy
 * game record.
 * 
 * This may require that move be merged with an existing history move
 * and discarded.  Return null if nothing should be added to the history
 * One should be very cautious about this, only to remove real pairs that
 * result in a null move.  It is vital that the operations performed on
 * the history are identical in effect to the manipulations of the board
 * state performed by "nmove".  This is checked by verifyGameRecord().
 * 
 * in commonEditHistory()
 * 
 */
     // public commonMove EditHistory(commonMove nmove)
     // {	  
    //	  commonMove rval = EditHistory(nmove,false);
    //	  return(rval);
    //  }
      public commonMove commonEditHistory(commonMove m)
      {		// we do a lot at start, so need to inhibit removal of the start when we immediately hit edit
    	  if(m.op!=MOVE_EDIT)
    	  {	return(super.commonEditHistory(m));
    	  }
    	  return(m);
      }

 // for reference, here's the standard definition
 //   public void verifyGameRecord()
 //   {	BoardProtocol ourB =  getBoard();
 //   	int ourDig = ourB.Digest();
 //   	BoardProtocol dup = dupBoard = ourB.cloneBoard();
 //   	int dupDig = dup.Digest();
 //   	G.Assert(dupDig==ourDig,"Duplicate Digest Matches");
 //   	dup.doInit();
 //   	int step = History.size();
 //   	int limit = viewStep>=0 ? viewStep : step;
 //   	for(int i=0;i<limit;i++) 
 //   		{ commonMove mv = History.elementAt(i);
 //   		  //G.print(".. "+mv);
 //   		  dup.Execute(mv); 
 //   		}
 //   	int dupRedig = dup.Digest();
 //   	G.Assert(dup.whoseTurn()==ourB.whoseTurn(),"Replay whose turn matches");
 //   	G.Assert(dup.moveNumber()==ourB.moveNumber(),"Replay move number matches");
 //   	if(dupRedig!=ourDig)
 //   	{
 //   	//int d0 = ourB.Digest();
 //   	//int d1 = dup.Digest();
 //   	G.Assert(false,"Replay digest matches");
 //   	}
 //   	// note: can't quite do this because the timing of "SetDrawState" is wrong.  ourB
 //   	// may be a draw where dup is not if ourB is pending a draw.
 //   	//G.Assert(dup.getState()==ourB.getState(),"Replay state matches");
 //   	dupBoard = null;
 //   }
    
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 * <p>
 * Note on debugging: If you get here mysteriously with hitOjbect and hitCode
 * set to default values, instead of the values you expect, you're probably
 * not setting the values when the gc is null.
 */
    public void StartDragging(HitPoint hp)
    {
 
    }

	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
 * <p>
 * Note on debugging: If you get here mysteriously with hitOjbect and hitCode
 * set to default values, instead of the values you expect, you're probably
 * not setting the values when the gc is null.
	 */
    public void StopDragging(HitPoint hp)
    {	
		CellId id = hp.hitCode;
        HiddenGameWindow hidden = findHiddenWindow(hp);
        if(remoteViewer>=0)
        {
        	QEPlayer pl = bb.getPlayer(remoteViewer);
        	pl.showPeeked = false;      	
        }
        else
        {
        	bigChip = null;
        }
        Calculator calc = hidden==null ? calculator : hidden.bidCalculator;
        if(calc!=null && calc.processButton(hp)) 
        	{ repaint(); 
        	  if(calc.done) 
        	  	{ closeCalculator(calc);
        	  	  if(hidden!=null) { hidden.bidCalculator = null; } else { calculator = null; }
        	  	}
        	}
        else if(!(id instanceof QEId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        QEId hitCode = (QEId)id;
        QECell hitObject = bb.getCell(hitCell(hp));
		QEState state = getUIState();
        switch (hitCode)
        {
        default:
            	throw G.Error("Hit Unknown: %s", hitCode);
        case ShowScore:
        	bigChip = (bb.players_in_game==5) ? QEChip.Score5 : QEChip.Score4;
        	break;
        case ShowHidden:
         	{int index = hp.hit_index;
         	 QEPlayer pl = bb.getPlayer(index);
        	 if(hidden!=null) 
        	 { 
        		pl.hiddenCensoring = !pl.hiddenCensoring;
        	 }
        	 else { 
        		pl.publicCensoring = !pl.publicCensoring;
        	 }}
        	 break;
         case HitEcommitButton:
        	PerformAndTransmit("ecommit "+hp.hit_index);
        	break;
        case HitWhiteBoard:
        	showCalculator(hidden,hitObject.row,G.Left(hp),Math.min(G.Top(hp),G.Height(fullRect)-G.Height(calculatorRect)/2));
        	break;
        case HitViewBid:
        	PerformAndTransmit("peek "+hp.hit_index);
			//$FALL-THROUGH$
		case ViewAgain:
			if(hidden==null && remoteViewer<0)
			{
        	bigChip = QEChip.ViewAgain;
        	viewBid = getPlayerOrTemp(hp.hit_index);
			}	
			else {
				QEPlayer pl = bb.getPlayer(hp.hit_index);
				pl.showPeeked = true;
				
			}
        	break;
        case NoQE:
        case HitPending:
        case HitWaste:
        case HitAuction:
        case HitCurrent:
        case HitTilesWon:
        case HitIndustry:
        case HitNoQE:
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Confirm:
				if(!bb.isDest(hitObject))
					{
					// note that according to the general theory, this shouldn't
					// ever occur because inappropriate spaces won't be mouse sensitve.
					// this is just defense in depth.
					throw G.Error("shouldn't hit a chip in state %s",state);
					}
				// fall through and pick up the previously dropped piece
				//$FALL-THROUGH$
			case Puzzle:
				PerformAndTransmit((bb.pickedObject==null ? "Pick ":"Drop ") 
						+ hitCode.shortName+" "
						+ hitObject.row+" "
						+ hp.hit_index
						);
				break;
			}
			break;
        }
        }
    }




    /** this is the place where the canvas is actually repainted.  We get here
     * from the event loop, not from the normal canvas repaint request.
     * <p>
     * if complete is true, we definitely want to start from scratch, otherwise
     * only the known changed elements need to be painted.  Exactly what this means
     * is game specific, but for qe the underlying empty board is cached as a deep
     * background, but the chips are painted fresh every time.
     * <p>
     * this used to be very important to optimize, but with faster machines it's
     * less important now.  The main strategy we employ is to paint EVERYTHING
     * into a background bitmap, then display that bitmap to the real screen
     * in one swell foop at the end.
     * 
     * @param gc the graphics object.  If gc is null, don't actually draw but do check for mouse location anyay
     * @param complete if true, always redraw everything
     * @param hp the mouse location.  This should be annotated to indicate what the mouse points to.
     */
  //  public void drawCanvas(Graphics gc, boolean complete,HitPoint hp)
  //  {	
       	//drawFixedElements(gc,complete);	// draw the board into the deep background
   	
    	// draw the board contents and changing elements.
        //redrawBoard(gc,hp);
        //      draw clocks, sprites, and other ephemera
        //drawClocksAndMice(gc, null);
        //DrawArrow(gc,hp);
 //    }
    /**
     * draw any last-minute items, directly on the visible canvas. These
     * items may appear to flash on and off, if so they probably ought to 
     * be drawn in {@link #drawCanvas}
     * @param offGC the gc to draw
     * @param hp the mouse {@link HitPoint} 
     */
  // public void drawCanvasSprites(Graphics offGC,HitPoint hp)
  //  {
  //     DrawTileSprite(offGC,hp); //draw the floating tile we are dragging, if present
       //
       // draw any animations that are in progress
       //
  //     drawSprites(offGC);       
  //  }
    
    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parseable by {@link online.game.commonCanvas#performHistoryInitialization}
     * @return return what will be the init type for the game
     */
     public String gameType() 
    	{
    	return(bb.gameType()); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(QE_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int np = his.intToken();	// players always 2
    	long rv = his.longToken();
    	int rev = his.intToken();	// rev does't get used either
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // long rk = his.longToken();
    	// bb.doInit(token,rk);
        bb.doInit(token,rv,np,rev);
        adjustPlayers(np);
    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);

        if(target==censorshipOption)
        {	handled=true;
        	censorship = censorshipOption.getState();
        	resetBounds();
        	repaint(20);
        }

        return (handled);
    }
/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically.
 * <p>
 * This is a good place to make notes about threads.  Threads in Java are
 * very dangerous and tend to lead to all kinds of undesirable and/or flakey
 * behavior.  The fundamental problem is that there are three or four sources
 * of events from different system-provided threads, and unless you are very
 * careful, these threads will all try to use and modify the same data
 * structures at the same time.   Java "synchronized" declarations are
 * hard to get right, resulting in synchronization locks, or lack of
 * synchronization where it is really needed.
 * <p>
 * This toolkit addresses this problem by adopting the "one thread" model,
 * and this is where it is.  Any other threads should do as little as possible,
 * mainly leave breadcrumbs that will be picked up by this thread.
 * <p>
 * In particular:
 * GUI events do not respond in the native thread.  Mouse movement and button
 * events are noted for later.  Requests to repaint the canvas are recorded but
 * not acted upon.
 * Network I/O events, merely queue the data for delivery later.
 *  */
    
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


    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new QEPlay());
    }
    public void ReplayGame(sgf_game ga)
    {	bb.players_in_game = 4;
     	super.ReplayGame(ga);
    }
    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 26 files visited 0 problems
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
                bb.doInit(value);
                adjustPlayers(bb.nPlayers());
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
    
    /*
     * support for hidden windows in pass-n-play mode
     * */
    int HiddenViewWidth = 600;
    int HiddenViewHeight = 300;

    public String nameHiddenWindow()
    {	return ServiceName;
    }
    public void adjustPlayers(int n)
    {
    	super.adjustPlayers(n);
        if(RpcService.isRpcServer() || VNCService.isVNCServer() || G.debug())
        {
        createHiddenWindows(n,HiddenViewWidth,HiddenViewHeight);
        }
    }
    public void drawEye(Graphics gc,Rectangle eyeR,HitPoint hp,boolean censor,int index)
    {    	
      	StockArt icon = censor ? StockArt.Eye : StockArt.NoEye;
       	if(icon.draw(gc, this, eyeR, hp, QEId.ShowHidden))
       	{
       		hp.hit_index = index;
       	}
    }
    public void witnessBids(Graphics gc,Rectangle r,QEPlayer myPlayer,boolean censoring)
    {	int step = Math.min(G.Width(r)/3,G.Height(r));
    	int x = G.Left(r);
    	int y = G.Top(r);
    	GC.fillRect(gc, rackBackGroundColor,r);
    	GC.frameRect(gc, Color.black,r);

    	for(int i=0;i<bb.nPlayers();i++)
    	{
    		QEPlayer pl = bb.getPlayer(i);
    		if(myPlayer!=pl)
    		{	Rectangle nr = new Rectangle(x,y,step,step);
				drawBidCard(gc,pl,null,nr, false,censoring); 
				x += step;
    		}
    	}
    }
    public void drawHiddenWindow(Graphics gc,HitPoint hp,int index,Rectangle r)
    {	HiddenGameWindow h = findHiddenWindow(hp);
    	drawHiddenWindow(gc,hp,index,r,h==null ? calculator : h.bidCalculator);
    }
    /*
       * @see online.game.commonCanvas#drawHiddenWindow(Graphics, lib.HitPoint, online.game.HiddenGameWindow)
     */
    public void drawHiddenWindow(Graphics gc,HitPoint hp0,int index,Rectangle bounds,Calculator cal)
    {	
    	QEPlayer pl = bb.getPlayer(index);
    	if(pl!=null)
    	{
        HitPoint hp = pl.showPeeked ? null : hp0;
        int margin = G.minimumFeatureSize()/2;
       	int left = G.Left(bounds)+margin;
    	int top = G.Top(bounds)+margin;
    	int width = G.Width(bounds)-margin*2;
    	int height = G.Height(bounds)-margin*2;
  	    int fh = FontManager.getFontSize(largeBoldFont());
  	    boolean censor = pl.hiddenCensoring;
    	int stateH = fh*8;
    	Font myfont = FontManager.getFont(largeBoldFont(), stateH/3);
        if (remoteViewer<0) 
        { QEChip.backgroundTile.image.tileImage(gc, bounds);
        }
        int stateX = left+CELLSIZE+stateH;
       	
       	int calcW = width/5;
       	int calcX = left+width-calcW;
        int doneW = width/5;
        int doneH = width/15;
        int doneX = left+width-doneW;
        int doneY = top+height-doneH;
        int calcw = width/3;
		int calch = height-height/10;
		int cx = left + width/2 - calcw/2;
		int cy = top + height/2 - calch/2;
		int lineH = stateH/2;
		Rectangle crect = new Rectangle(cx,cy,calcw,calch);								// calculator overlay
       	Rectangle playerIdRect = new Rectangle(left,top+fh/2,stateH,stateH);
       	Rectangle stateR =new Rectangle(stateX,top,calcX-stateX,lineH); 				// game state   
       	Rectangle promptR =new Rectangle(stateX,top+lineH,calcX-stateX,lineH); 				// game state   
       	Rectangle calcR = new Rectangle(calcX+calcW/5,top,calcW*4/5,calcW*2/3);							// our bid card
       	Rectangle viewBidR = new Rectangle(calcX-lineH,top,lineH*3/2,lineH*3/2); 			// view bid card
       	Rectangle noQER = new Rectangle(calcX-lineH,top+lineH*5/4,lineH,lineH); 
       	Rectangle eyeR = new Rectangle(calcX-stateH,top+lineH,lineH,lineH);	// see hidden
       	Rectangle witnessRect = new Rectangle(left+width/4,top+stateH,width/2,height/3);// overlay for showing all bids
       	Rectangle noQERect = new Rectangle(left,top+stateH,width,height-stateH-lineH);
       	Rectangle pDoneRect = new Rectangle(doneX,doneY,doneW,doneH);
       	Rectangle nameR = new Rectangle(left,top,width/3,fh);
      	
       	QEState uistate = getUIState(index);
       	HitPoint select = canHitCard(uistate,index) ? hp : null;
       	drawEye(gc,eyeR,hp,censor,index);

       	drawBidCard(gc,pl,select,calcR,	false, censor);	// draw the current bid
       	
        // draw the game state
        standardGameMessage(gc,
            		uistate==QEState.Gameover?gameOverMessage(bb):s.get(uistate.description()),
            				(uistate!=QEState.Puzzle)&&!simultaneousTurnsAllowed(),
            				bb.whoseTurn,
            				stateR);
 	    DrawChipPool(gc, playerIdRect, index, hp,bb,viewBidR,noQER,
  	    		noQERect,
  	    		pDoneRect,
  	    		censor);
       	String name = prettyName(index);
       	GC.setFont(gc,largeBoldFont());
       	GC.Text(gc, false, nameR, Color.black,null,name);
       	
		if(cal!=null)
		{	
			drawCalculator(gc,bb,crect,hp,calculator);
		}
		else if(uistate.simultaneous || (bb.whoseTurn==index))
		{
		switch(uistate)
		{
		default: break;
		case Witness:
			witnessBids(gc,witnessRect,pl,censor);
			break;
		case SealedBid:
		case OpenBid:
		case EphemeralConfirm:
		case EphemeralSealedBid:
		case Confirm:
			
				{     	
				GC.setFont(gc,myfont);
				GC.Text(gc, true, promptR, Color.red, null,s.get(YourTurnMessage));
				}
		}}
		if(pl.showPeeked)
		{
			drawPeek(gc,pl.peekedWinningBid,left+width/10,top+height/10,width-width/5,height-height/5);
		}
    }}

}

