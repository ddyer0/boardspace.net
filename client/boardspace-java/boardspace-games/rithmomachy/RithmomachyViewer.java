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
package rithmomachy;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import lib.Graphics;
import lib.Image;

import online.common.*;
import online.common.SeatingChart.DefinedSeating;
import lib.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import static rithmomachy.RithmomachyMovespec.*;

/**
 * This code shows the overall structure appropriate for a game view window.
 * TODO: when rotated, also rotate the tiles so they appear upright
*/
public class RithmomachyViewer extends CCanvas<RithmomachyCell,RithmomachyBoard> implements RithmomachyConstants, GameLayoutClient
{
     /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
 
    // images
    private static Image[] textures = null;// background textures
    private static Image[] images = null;	// full scale images
    // private state
    private RithmomachyBoard b = null; 	// the board from which we are displaying
    private int SQUARESIZE;			// size of a board square
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    //public Rectangle fullRect = addRect("fullRect"); //the whole viewer area
    //public Rectangle boardRect = addRect("boardRect"); //the actual board, normally at the left edge
    //public Rectangle chatRect = addRect("chatRect"); // the chat window
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private boolean rotateBoard = false;
    private Rectangle reverseViewRect = addRect("reverse");
    private JCheckBoxMenuItem reverseOption = null;
    private Rectangle resultRect = addRect("result");
    private Rectangle calculatorRect = addRect("Calculator");
    private Rectangle repRect = addRect("repRect");
    
    private Rectangle chipRects[] = addRect("icon",2);
    private Rectangle scoreRects[] = addRect("score",2);
    private Rectangle capturedRects[] = addRect("captured",2);
    

    public synchronized void preloadImages()
    {	
       	RithmomachyChip.preloadImages(loader,ImageDir);
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = loader.load_masked_images(ImageDir,ImageNames);
    	}
        gameIcon = textures[LIFT_ICON_INDEX];
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
     	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(exHashtable.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new RithmomachyBoard(info.getString(GAMETYPE, Rithmomachy_INIT),
        		randomKey,players_in_game,getStartingColorMap());
        useDirectDrawing(true);
        doInit(false);
        b.setReverseY(preferredRotation());
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        
        if(extraactions) { getSolutions =  myFrame.addAction("Get Solutions",deferredEvents); }

        
     }
    public boolean preferredRotation()
    {
        boolean rev = false;
        DefinedSeating defined = seatingChart();
        switch(defined)
        {
        default: 
        	rev = false;
        	break;
        case FaceToFacePortrait: 
        case FaceToFaceLandscapeSide:
        case SideBySide:
        case RightCorner:
        	rev = true;
        }
        return(rev);
    }
    private JMenuItem getSolutions = null;
    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        int np = b.nPlayers();
        b.doInit(b.gametype,b.randomKey,np);			// initialize the board
        if(!preserve_history)
    	{     
    	startFirstPlayer();
    	}

    }
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unitsize)
    {	commonPlayer pl = getPlayerOrTemp(player);
    	Rectangle chip = chipRects[player];
    	Rectangle done = doneRects[player];
    	Rectangle rack = capturedRects[player];
    	Rectangle score = scoreRects[player];
    	int chipW = unitsize*4;
    	int chipH = unitsize*4;
    	int doneW = unitsize*4;
    	int scoreW = chipW*2;
    	Rectangle box = pl.createRectangularPictureGroup(x+chipW,y,unitsize);
    	G.SetRect(chip, x, y, chipW, chipH);
    	G.SetRect(score, x, y+chipH, scoreW, unitsize);
    	G.SetRect(done, G.Right(box),y+unitsize/2,doneW,plannedSeating()?doneW/2:0);
       	G.union(box, chip,done,score);
            	
    	G.SetRect(rack,x,G.Bottom(box),unitsize*10,unitsize*3);
    	G.union(box,rack);
   	
    	pl.displayRotation = rotation;
   
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
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int buttonW = fh*8;
        int margin = fh/2;
        int calcW = minLogW;
        int nrows = b.boardRows;  
        int ncols = b.boardColumns-4;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.75,	// 60% of space allocated to the board
    			width>height?2.0:0.5,	// aspect ratio for the board
    			fh*2.5,	// maximum cell size
    			0.2		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW*3/2, minLogH*3/2);
        layout.placeRectangle(calculatorRect, calcW,calcW,BoxAlignment.Center);
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect,repRect);
    	layout.placeTheVcr(this,minLogW,minLogW*3/2);

        
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int stateH = fh*3;
        rotateBoard = mainH>mainW;
    	if(rotateBoard) { ncols+=3; }
     	// calculate a suitable cell size for the board
    	double cs = rotateBoard ? Math.min((double)(mainH-stateH)/ncols,(double)mainW/nrows) 
    			: Math.min((double)mainW/ncols,(double)mainH/nrows);
    	int CELLSIZE = (int)cs;
    	SQUARESIZE = CELLSIZE;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)((rotateBoard ? nrows : ncols)*CELLSIZE);
    	int boardH = (int)((rotateBoard ? ncols : nrows)*CELLSIZE);
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
        int stateY = boardY+stateH/4;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationMenu,liftRect,reverseViewRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
     	if(rotateBoard) 
      		{ 
      		  G.setRotation(boardRect, Math.PI/2,boardX+boardW/2,boardY+boardH/2);
      		}

    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH*2,boardW,stateH);   
        G.SetRect(resultRect,boardX,boardBottom-stateH,boardW,stateH);
        
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,Color.white,Color.white);
 	
    }

    
	
    private void DrawReverseMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	RithmomachyChip king = RithmomachyChip.getChip(b.reverseY()?0:1,0);
    	RithmomachyChip reverse = RithmomachyChip.getChip(b.reverseY()?1:0,0);
    	int w = G.Width(r);
    	int cx = G.centerX(r);
    	int cy = G.centerY(r);
    	reverse.drawChip(gc,this,w,cx,cy-w/4,null);
     	king.drawChip(gc,this,w,cx,cy+w/4,null);
    	HitPoint.setHelpText(highlight,r, RithId.ReverseViewButton,s.get(ReverseViewExplanation));
     }  

	// draw a box of spare gobblets. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, RithmomachyBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	RithmomachyCell chip= gb.captured[forPlayer];
        boolean canHit = gb.LegalToHitChips(forPlayer);
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (chip.topChip()!=null);
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null; 
        String msg = null;
        int h = G.Height(r)*2/3;
        int ax = G.Left(r)+G.Height(r)/2;
        int ay = G.centerY(r);
        chip.drawStack(gc,this,pt,G.Height(r)*2/3,ax,ay,0,0.4-chip.height()*0.01,0.0,msg);
        if(canDrop && pt!=null)
        {
        	StockArt.SmallO.drawChip(gc, this, h, ax,ay,null);
        }
        if((highlight!=null) && (highlight.hitObject==chip))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = G.Height(r)/2;
        	highlight.spriteColor = Color.red;
        }
     }
    
    private void drawScore(Graphics gc,RithmomachyBoard gb,int forPlayer,Rectangle r)
    {	int perc = (gb.capturedValue[forPlayer]*100)/gb.startingValue[forPlayer];
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,true,r,Color.black,null,s.get(CapturedMessage,gb.captured[forPlayer].height(),perc));
    }
    
    int framedLeft = 12;
    int framedRight = 20;
    
    private void showMessage(Graphics gc,Rectangle r,Wintype typ,int a1,int a2,int a3)
    {
    	GC.Text(gc,false,r,Color.black,null,s.get(typ.toString())+" "+a1+" "+a2+" "+a3);
    }
    private String findAttacks(IStack numbers,int value)
    {
    	String msg = "";
    	String space = "";
    	String wide = "   ";
    	for(int idx1=numbers.size()-1; idx1>=0; idx1--)
    	{	int lv = numbers.elementAt(idx1);
    		for(int idx2 = idx1-1; idx2>=0; idx2--)
    		{
    			int rv=numbers.elementAt(idx2);
    			
    	    	if((lv+rv==value) && numbers.contains(lv+rv))
    	    	{
    	    		msg += space+lv+"+"+rv;
    	    		space = wide;
    	    	}
    	    	if((lv-rv==value) && numbers.contains(lv-rv))
    	    	{	msg += space + lv+ "-"+ rv;
    	    		space = wide;
    	    	}
    	    	if((lv*rv==value) && numbers.contains(lv*rv))
    	    	{	msg += space + lv+"*"+rv;
    	    		space = wide;
    	    	}
    	    	if(((lv/rv)==value) && (lv%rv==0) && numbers.contains(lv/rv) )
    	    	{
    	    		msg += space + lv+"/"+rv;
    	    		space = wide;
    	    	}
    		}
    	}
    	if(space==wide) 
    		{ msg = s.get(AmbushMessage)+": "+msg;
    		  space="\n"+s.get(EruptionMessage)+": "; 
    		}
    	for(int idx1=numbers.size()-1; idx1>=0; idx1--)
    	{	int lv = numbers.elementAt(idx1);
    		if( ((value/lv)<=DEFAULT_COLUMNS) && ((value/lv)>=2) && ((value%lv)==0)) 
    			{ msg+= space + (value/lv)+".."+lv; space=wide; 
    			}
    		if( ((lv/value)<=DEFAULT_COLUMNS) && (lv/value>=2) && ((lv%value)==0)) 
    			{ msg+= space + (lv/value)+".."+lv; space=wide; 
    			}
    	}
    	if(G.eq(space,wide))
    		{ space="\n"+ s.get(EqualityMessage) + ": "; 
    		}
    	for(int idx1=numbers.size()-1; idx1>=0; idx1--)
    	{	int lv = numbers.elementAt(idx1);
    		if(lv==value) { msg += space+lv; idx1=-1; }
    	}
    	return(msg);
    }
    private String findAttacks(RithmomachyBoard b,RithmomachyCell c)
    {	IStack numbers = b.availableNumbers(nextPlayer[b.playerIndex(c)],true);
    	String msg = "";
    	int h = c.height();
    	String space = "";
    	boolean tall = false;
    	numbers.sort();
    	if(h>1) 
    		{ int v = c.stackValue();
    		  msg = findAttacks(numbers,v);
    		  tall = true;
    		  if(msg.length()>0) { msg = v+">>"; space = "\n"; } 
    		}
    	while(h-- >0 )
    	{	int v = c.chipAtIndex(h).value;
    		String msg2 = findAttacks(numbers,v);
    		if(msg2.length()>0)
    			{ if(tall) { msg2=v+">>"+msg2; } 
    			  msg += space+msg2;
    			  space = "\n";
    			}
    	}
    	return(msg);
    }
    PopupManager numberChoice = new PopupManager();
    RithId selectedNumberChoice = null;
    private void chooseNumber(HitPoint hp)
    {	IStack numbers = b.availableNumbers(-1,true);
    	numbers.sort();
    	selectedNumberChoice = (RithId)hp.hitCode;
    	numberChoice.newPopupMenu(this,deferredEvents);
    	for(int i=0;i<numbers.size();i++)
    	{	int v = numbers.elementAt(i);
    		numberChoice.addMenuItem(""+v,v);
    	}
    	numberChoice.show(G.Left(hp),G.Top(hp));
    }
    
    private void drawCalculator(Graphics gc,RithmomachyBoard gb,Rectangle r,HitPoint highlight)
    {	int boxw = G.Width(r)/5;
    	int boxh = G.Width(r)/5;
    	int lv = Math.max(framedLeft,framedRight);
    	int rv = Math.min(framedLeft,framedRight);
    	Rectangle leftRect = new Rectangle(G.Left(r)+boxw/2,G.Top(r),boxw*2,boxh);
    	Rectangle rightRect = new Rectangle(G.Right(leftRect),G.Top(r),boxw*2,boxh);
    	Rectangle msgRect = new Rectangle(G.Left(r)+2,G.Bottom(leftRect),G.Width(r)-4,boxh);
    	IStack numbers = gb.availableNumbers(-1,true);	// both players
    	numbers.sort();
    	
    	GC.setFont(gc,standardBoldFont());
    	boolean inleft = G.pointInRect(highlight,leftRect);
    	GC.Text(gc,true,leftRect,Color.black,inleft?HighlightColor:null,""+framedLeft);
    	if(inleft)
    	{	highlight.hitCode = RithId.LeftNumber;   	 
    	}
     	GC.frameRect(gc,Color.black,leftRect);

     	boolean inRight = G.pointInRect(highlight,rightRect);
    	GC.Text(gc,true,rightRect,Color.black,inRight?HighlightColor:null,""+framedRight);
    	if(inRight)
    	{	highlight.hitCode = RithId.RightNumber;
    	}
       	GC.frameRect(gc,Color.black,rightRect);

       	boxh = boxh*2/3;
    	for(int idx = numbers.size()-1; idx>=0; idx--)
    	{	int n = numbers.elementAt(idx);
    		if(gb.isArithmetic(n,rv,lv)) { showMessage(gc,msgRect,Wintype.arithmetic,n,rv,lv);
    			G.SetTop(msgRect,G.Top(msgRect) + boxh); }
    		if(gb.isGeometric(n,rv,lv)) { showMessage(gc,msgRect,Wintype.geometric,n,rv,lv);
    			G.SetTop(msgRect,G.Top(msgRect) + boxh); }
    		if(gb.isHarmonic(n,rv,lv)) { showMessage(gc,msgRect,Wintype.harmonic,n,rv,lv);
    			G.SetTop(msgRect,G.Top(msgRect) + boxh); }
    		if(gb.isArithmetic(rv,lv,n)) { showMessage(gc,msgRect,Wintype.arithmetic,rv,lv,n);
    			G.SetTop(msgRect,G.Top(msgRect) + boxh); }
    		if(gb.isGeometric(rv,lv,n)) { showMessage(gc,msgRect,Wintype.geometric,rv,lv,n);
    			G.SetTop(msgRect,G.Top(msgRect) + boxh); }
    		if(gb.isHarmonic(rv,lv,n)) { showMessage(gc,msgRect,Wintype.harmonic,rv,lv,n);
    			G.SetTop(msgRect,G.Top(msgRect) + boxh); }
    	}
     	
    	GC.frameRect(gc,Color.black,r);
    	
    }
    
    private boolean registerVictory(IntIntHashtable tab,int n1,int n2,int n3)
    {	int ind = n1 + n2*1000 + n3*1000*1000;
    	if(tab.contains(ind)) { return(false); }
    	tab.put(ind,ind);
    	return(true);
    }
    
    /*
     * 
     * This section calculates a table of Glorious victories.  Only used once
     * but left for completeness (and of course, the next revision)
     * 
     */
	WinStack arithmeticSolutions = new WinStack();
	WinStack geometricSolutions = new WinStack();
	WinStack harmonicSolutions = new WinStack();
	// solutions are registered here to prevent duplicates
	IntIntHashtable arithmeticHash = new IntIntHashtable();
	IntIntHashtable geometricHash = new IntIntHashtable();
	IntIntHashtable harmonicHash = new IntIntHashtable();

	
    private void getGlorious()
    {	b.doInit();
    	arithmeticSolutions.clear();
    	geometricSolutions.clear();
    	harmonicSolutions.clear();
    	arithmeticHash.clear();
    	geometricHash.clear();
    	harmonicHash.clear();
    	IStack standardNumbers = b.availableNumbers(-1,false);	// all individual values for both players, no pyramid stack values
    	standardNumbers.sort();
    	getGloriousFromSet(standardNumbers,"");
    	
    	IStack numbers = b.availableNumbers(-1,true);
    	numbers.sort();
    	getGloriousFromSet(numbers,"  pyramid");		// add the solutions for the full pyramid values
    	
    	RithmomachyCell p0 = b.getPyramid(0);
    	RithmomachyCell p1 = b.getPyramid(1);
 	
    	for(int p0Sub = 0,lastP0Sub = 1<<p0.height(); p0Sub<lastP0Sub; p0Sub++)
    	{	int p0val = p0.subsetValue(p0Sub);
    		for(int p1Sub=0,lastP1Sub = 1<<p1.height(); p1Sub<lastP1Sub; p1Sub++)
    		{
    		int p1val = p1.subsetValue(p1Sub);
    		numbers = b.availableNumbers(-1,false);
    		// do this for all subsets of p0 and p1
    		boolean new0 = (p0val>0) ? numbers.pushNew(p0val) : false;
    		boolean new1 = (p1val>0) ? numbers.pushNew(p1val) : false;
    		numbers.sort();
    		if(new0 || new1)
    			{
    			getGloriousFromSet(numbers,"  partial pyramid");
    			}
    		}
    	}
    	printSolutions(arithmeticSolutions,"Arithmetic",standardNumbers);
    	printSolutions(geometricSolutions,"Geometric",standardNumbers);
    	printSolutions(harmonicSolutions,"Harmonic",standardNumbers);
     	
    }
    private void printSolutions(WinStack sol,String msg,IStack standardNumbers)
    {
    	sol.sort(false);	// sort the solutions so they're presented in a sensible order
    	G.print(msg);
    	for(int i=0,lim=sol.size(); i<lim; i++)
    	{
    		G.print(sol.elementAt(i).dString(standardNumbers));
    	}
    }
    private void getGloriousFromSet(IStack numbers,String suffix)
    {
    	for(int idx1 =0, lim= numbers.size(); idx1<lim; idx1++)
    	{	int n1 = numbers.elementAt(idx1);
    		for(int idx2 = idx1+1; idx2<lim; idx2++)
    		{	int n2 = numbers.elementAt(idx2);
    			for(int idx3 = idx2+1; idx3<lim; idx3++)
    			{
    				int n3 = numbers.elementAt(idx3);
    				if(b.isArithmetic(n1,n2,n3) && registerVictory(arithmeticHash,n1,n2,n3))
    					{ arithmeticSolutions.push(new WinDescription(n1,n2,n3,suffix));  
    					}
    				if(b.isGeometric(n1,n2,n3) && registerVictory(geometricHash,n1,n2,n3)) 
    					{ geometricSolutions.push(new WinDescription(n1,n2,n3,suffix)); 
    					}
    				if(b.isHarmonic(n1,n2,n3) && registerVictory(harmonicHash,n1,n2,n3)) 
    					{ harmonicSolutions.push(new WinDescription(n1,n2,n3,suffix));
    					}; 
    			}
    		}
    	}
    }
    
    // describe the winning condition
    private void drawResult(Graphics gc,RithmomachyBoard gb,int forPlayer,Rectangle r)
    {	RithmomachyState state = gb.getState();
    	if(state==RithmomachyState.Gameover)
    	{	WinStack wins = gb.collectWinsByGloriousVictory(forPlayer,null);
    		int next = nextPlayer[forPlayer];
    		RithmomachyCell captured = gb.captured[next];
    		if(wins.size()>0)
    			{
    			WinDescription win = wins.pop();
    			GC.Text(gc,true,r,Color.black,null,win.getDescription(s));
    			}
    		else if(captured.height()>gb.captureTarget(next))
    		{
    			
    		}
    	}
    }
    private void DrawPlayerIcon(Graphics gc,int forPlayer,Rectangle r)
    {	RithmomachyChip.getChip(b.getColorMap()[forPlayer],0).drawChip(gc, this, 
    		(int)(G.Width(r)*0.8),G.centerX(r),G.centerY(r),null);
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
    	if(obj==RithId.PickedStack.ordinal())
    	{
    	b.pickedStack.drawStack(g,this,null,(int)(SQUARESIZE*0.75),xp,yp,0,0.1,null);
    	}
     }


    Image scaled = null;
    
    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	boolean reviewBackground = reviewMode()&&!mutable_game_record;
    	int cx = G.centerX(boardRect);
    	int cy = G.centerY(boardRect);
    	
    	// erase
    	GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
    	//GC.fillRect(gc, fullRect);
    textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      
    	if(rotateBoard) { GC.setRotation(gc, Math.PI/2,cx,cy); }
    	
    	if(reviewBackground)
	      {	 
	       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
	      }
	       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      scaled = images[BOARD_INDEX].centerScaledImage(gc, boardRect,scaled);
      b.SetDisplayParameters(rotateBoard?0.85:0.878,1.0,  0.18,0.05,  0);
      b.SetDisplayRectangle(boardRect);

      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
      
      if(rotateBoard) 
  		{ GC.setRotation(gc, -Math.PI/2, cx, cy);
  		}

    }

    private void annotate(Graphics gc,RithmomachyBoard gb,Rectangle brect,RithmomachyCell c,String ch)
    {	
        int ypos = G.Bottom(brect) - gb.cellToY(c.col, c.row);
        int xpos = G.Left(brect) + gb.cellToX(c.col, c.row);
        StockArt.SmallO.drawChip(gc, this,SQUARESIZE, xpos-SQUARESIZE/5,ypos+SQUARESIZE/5,""+ch);
   }
    private void showGloriousVictory(Graphics gc,RithmomachyBoard gb,Rectangle brect,int pl)
    {
    	WinStack wins = gb.collectWinsByGloriousVictory(pl,null);
    	while(wins.top()!=null)	
    	{	WinDescription win = wins.pop();
    		annotate(gc,gb,brect,win.anchorCell,""+win.anchor);
    		annotate(gc,gb,brect,win.pivotCell,""+win.pivot);
    		annotate(gc,gb,brect,win.endCell,""+win.end);
    	}
    }
    private void showCaptureByDeceit(Graphics gc,RithmomachyBoard gb,Rectangle brect,int pl)
    {
    	CaptureStack caps = gb.getCaptures(pl);
    	while(caps.top()!=null)	
    	{	CaptureDescription cap = caps.pop();
    		RithmomachyCell dest = cap.victim;
    		switch(cap.type)
    		{
    		default: throw G.Error("not expecting %s",cap);
    		case Deceit:
	    		{
	    		RithmomachyCell a1 = cap.attacker;
	    		RithmomachyCell a2 = cap.also_attacker;
	    		int a1val = cap.attackerValue();
	    		int a2val = cap.also_attackerValue();
	    		annotate(gc,gb,brect,a1,"D"+a1val);
	    		annotate(gc,gb,brect,a2,"D"+a2val);
	    		annotate(gc,gb,brect,dest,"x"+(a1val+a2val));
	    		}
	    		break;
    		case Siege:
    			{
   				annotate(gc,gb,brect,dest,"xx");
    			}
    			break;
    		case Equality:
    			{
    			RithmomachyCell a2 = cap.attacker;
    			annotate(gc,gb,brect,a2,"="+cap.attackerValue());
    			annotate(gc,gb,brect,dest,"=x");
    			}
    			break;
    		case Eruption:
   				{
    			RithmomachyCell a2 = cap.attacker;
    			annotate(gc,gb,brect,a2,"E"+cap.attackerValue());
    			annotate(gc,gb,brect,dest,"Ex");
    			}
   				break;
    		}
    	}
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, RithmomachyBoard gb, Rectangle brect, HitPoint highlight,HitPoint anySelect)
    {	
     	if(rotateBoard) 
    	{ GC.setRotatedContext(gc,boardRect,highlight,Math.PI/2);
    	}
     	boolean dolift = doLiftAnimation();
     	
     	Hashtable<RithmomachyCell,RithmomachyMovespec>dests = gb.getDests();
     	
     	//
        // now draw the contents of the board and anything it is pointing at
        //
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
        RithmomachyCell hitCell = null;
        Enumeration<RithmomachyCell>cells = gb.getIterator(Itype.RLTB);
        int top = G.Bottom(brect);
        int left = G.Left(brect);
     	while(cells.hasMoreElements())
       	{	
     		RithmomachyCell cell = cells.nextElement();
            boolean canHit = !dolift && gb.LegalToHitBoard(cell);
            int ypos = top - gb.cellToY(cell);
            int xpos = left + gb.cellToX(cell);
            if(dests.get(cell)!=null)
            {	StockArt.SmallO.drawChip(gc, this, SQUARESIZE, xpos, ypos,null);
            }
            if( cell.drawStack(gc,this,canHit?highlight:null,(int)(SQUARESIZE*0.75),xpos,ypos,
            			liftSteps,0.1,null)) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	hitCell = cell;
            	}
        	if(cell.height()>0)
        	{
        		HitPoint.setHelpText(anySelect,SQUARESIZE,xpos,ypos,findAttacks(gb,cell));
        	}
    	}
    	if(hitCell!=null)
    	{
        	highlight.arrow =hasMovingObject(highlight) 
  				? StockArt.DownArrow 
  				: hitCell.topChip()!=null?StockArt.UpArrow:null;
        	highlight.awidth = SQUARESIZE/2;
        	highlight.spriteColor = Color.red;
	
    	}
            
		showGloriousVictory(gc,gb,brect,gb.whoseTurn);
		showCaptureByDeceit(gc,gb,brect,gb.whoseTurn);
		showGloriousVictory(gc,gb,brect,nextPlayer[gb.whoseTurn]); 
		showCaptureByDeceit(gc,gb,brect,nextPlayer[gb.whoseTurn]); 
        
    	if(rotateBoard) 
    	{ GC.unsetRotatedContext(gc,highlight);
    	}

    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  drawLiftRect(gc,liftRect,highlight,textures[LIFT_ICON_INDEX]);
       DrawReverseMarker(gc,reverseViewRect,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  RithmomachyBoard gb = disB(gc);
      int whoseTurn = gb.whoseTurn;
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      RithmomachyState vstate = gb.getState();
      gameLog.redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot,highlight);
        
        
        boolean planned = plannedSeating();
        for(int i=FIRST_PLAYER_INDEX; i<=SECOND_PLAYER_INDEX;i++)
          {	commonPlayer pl = getPlayerOrTemp(i);
          	pl.setRotatedContext(gc, highlight, false);
          	
            DrawCommonChipPool(gc, gb,i,capturedRects[i], whoseTurn,ot);
            drawScore(gc,gb,i,scoreRects[i]);
            
            DrawPlayerIcon(gc,i,chipRects[i]);

           	if(planned && (i==whoseTurn))
          	{
           		handleDoneButton(gc,doneRects[i],(gb.DoneState() ? select : null), 
      					HighlightColor, rackBackGroundColor);
          	}
          	pl.setRotatedContext(gc, highlight, true);
          }	
        commonPlayer pl = getPlayerOrTemp(whoseTurn);
 		double messageRotation = pl.messageRotation();
 		 
 		drawResult(gc,gb,whoseTurn,resultRect);

        drawCalculator(gc,gb,calculatorRect,highlight);
        GC.setFont(gc,standardBoldFont());
		if (vstate != RithmomachyState.Puzzle)
        {
			if(!planned)
				{handleDoneButton(gc,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
        }

 		drawPlayerStuff(gc,(vstate==RithmomachyState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);

        standardGameMessage(gc,messageRotation,
        		vstate==RithmomachyState.Gameover
        			?gameOverMessage()
        			:s.get(vstate.getDescription()),
        				vstate!=RithmomachyState.Puzzle,
        				gb.whoseTurn,
        				stateRect);
        gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
        goalAndProgressMessage(gc,ourSelect,s.get(VictoryCondition),progressRect, goalRect);
        DrawRepRect(gc,messageRotation,Color.black,gb.Digest(),repRect);
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
        startBoardAnimations(replay,b.animationStack,b.cellSize(),MovementStyle.Simultaneous);
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
        return (new RithmomachyMovespec(st, player));
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

//    public commonMove EditHistory(commonMove nmove)
//    {
//    	RithmomachyMovespec newmove = (RithmomachyMovespec) nmove;
//    	RithmomachyMovespec rval = newmove;			// default returned value
//        int size = History.size() - 1;
//        int idx = size;
//        int state = b.board_state;
// 
//        while (idx >= 0)
//            {	int start_idx = idx;
//            RithmomachyMovespec m = (RithmomachyMovespec) History.elementAt(idx);
//                if(m.next!=null) { idx = -1; }
//                else 
//               {
//                switch (newmove.op)
//                {
//                case MOVE_RESET:
//                	rval = null;	// reset never appears in the record
//                 case MOVE_RESIGN:
//                	// resign unwind any preliminary motions
//                	switch(m.op)
//                	{
//                  	default:	
//                 		if(state==Puzzle) { idx = -1; break; }
//                 	case MOVE_PICK:
//                 	case MOVE_PICKB:
//               		UndoHistoryElement(idx);	// undo back to last done
//                		idx--;
//                		break;
//                	case MOVE_DONE:
//                	case MOVE_START:
//                	case MOVE_EDIT:
//                		idx = -1;	// stop the scan
//                	}
//                	break;
//                	
//             case MOVE_DONE:
//             default:
//            		idx = -1;
//            		break;
//               case MOVE_DROPB:
//                	if(m.op==MOVE_PICKB)
//                	{	if((newmove.to_col==m.from_col)
//                			&&(newmove.to_row==m.from_row))
//                		{ UndoHistoryElement(idx);	// pick/drop back to the same spot
//                		  idx--;
//                		  rval=null;
//                		}
//                	else if(idx>0)
//                	{ RithmomachyMovespec m2 = (RithmomachyMovespec)History.elementAt(idx-1);
//                	  if((m2.op==MOVE_DROPB)
//                			  && (m2.to_col==m.from_col)
//                			  && (m2.to_row==m.from_row))
//                	  {	// sequence is pick/drop/pick/drop, edit out the middle pick/drop
//                		UndoHistoryElement(idx);
//                	  	UndoHistoryElement(idx-1);
//                	  	idx = idx-2;
//                	  }
//                	  else { idx = -1; }
//                		
//                	}
//                	else { idx = -1; }
//                	}
//                	else { idx = -1; }
//                	break;
//                	
//            	}
//               }
//            G.Assert(idx!=start_idx,"progress editing history");
//            }
//         return (rval);
//    }
//

    
private void playSounds(commonMove m)
{

    // add the sound effects
    switch(m.op)
    {
    case MOVE_BOARD_BOARD:  	
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
       	 break;
     case MOVE_PICKB:
    	playASoundClip(light_drop,100);
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
        if (hp.hitCode instanceof RithId) // not dragging anything yet, so maybe start
        {

        RithId hitObject = (RithId)hp.hitCode;
		RithmomachyCell cell = hitCell(hp);
		RithmomachyChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
 	    case Black_Chip_Pool:
	    	PerformAndTransmit("Pick B "+chip.chipNumber());
	    	break;
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick W "+chip.chipNumber());
	    	break;
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

	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging( HitPoint hp)
    {
        CellId id = hp.hitCode;
        if(!(id instanceof RithId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
    	RithId hitObject = (RithId)hp.hitCode;
		RithmomachyState state = b.getState();
		RithmomachyCell cell = hitCell(hp);
		RithmomachyChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case LeftNumber:
        case RightNumber:
    		chooseNumber(hp);
    		break;
        case ReverseViewButton:
       	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
       	 generalRefresh();
       	 break;

         case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Confirm:
			case Play:
			case Puzzle:
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
			
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  (hitObject==RithId.Black_Chip_Pool) ? " B " : " W ";
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                case Play:
            		PerformAndTransmit(RESET);
            		break;

               	case Puzzle:
            		PerformAndTransmit("Drop"+col+cell.row+" "+mov);
            		break;
            	}
			}
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
    public String gameType() 
    { 
    	return(""+b.gametype+" "+b.randomKey+" "+b.nPlayers()); 
   }
    public String sgfGameType() { return(Rithmomachy_SGF); }

    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a checker init spec
    	long rk = G.LongToken(his);
    	int np = G.IntToken(his);
    	// make the random key part of the standard initialization,
    	// even though games like checkers probably don't use it.
        b.doInit(token,rk);
        adjustPlayers(np);

    }


    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {	
    	if(numberChoice.selectMenuTarget(target))
    	{
    		int v = numberChoice.value;
    		switch (selectedNumberChoice)
    		{
    			case LeftNumber:
    				framedLeft = v;
    				break;
    			case RightNumber:
    				framedRight = v;
    				break;
			default:
				break;
    		}
    		return(true);
    	}
    	else if(target==getSolutions) { getGlorious(); return(true); }
    	else if(target==reverseOption)
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
    public SimpleRobotProtocol newRobotPlayer() { return(new RithmomachyPlay()); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 624 files visited 0 problems
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
}

