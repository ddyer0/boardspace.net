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
package tammany;

import bridge.*;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;

import java.util.Hashtable;
import java.util.StringTokenizer;

import online.common.*;
import lib.Graphics;
import lib.AR;
import lib.CellId;
import lib.DefaultId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.InternationalStrings;
import lib.LFrameProtocol;
import lib.MenuParentInterface;
import lib.PopupManager;
import lib.StockArt;
import lib.TextChunk;
import lib.Text;
import lib.Image;
import lib.TextGlyph;
import online.game.*;
import online.game.BaseBoard.BoardState;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import vnc.VNCService;

import static tammany.TammanyMovespec.*;


/**
 * 
*/
public class TammanyViewer extends CCanvas<TammanyCell,TammanyBoard> implements TammanyConstants, GameLayoutClient,ColorNames
{	static final long serialVersionUID = 1000;
     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackgroundColor = new Color(250,250,230);
    private Color rackBackGroundColor = new Color(178,161,112);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    
    private Color TammanyMouseColors[] = {
    		Color.red,new Color(178,123,76),Color.black,
    		new Color(103,39,149),Color.yellow
    };
    private Color TammanyMouseDotColors[] = {
    		Color.black,Color.black,Color.white,
    		Color.black,Color.black
    };
    // private state
    private TammanyBoard bb = null; //the board from which we are displaying
    private int CELLSIZE; 	//size of the layout cell
    private TammanyChip giantCard = null;
 
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle wardMapRect = addRect(".wardMap");
    private String preparedVote[] = null;
    private boolean preparedVoteSent[] = null;
    private int preparedWard = -1;
    private Rectangle infoRect = addRect("InfoRect");
    
    private Rectangle playerIconRect[] = addRect("icon",5);
    private Rectangle playerRoleRect[] = addRect("role",5);	
    private Rectangle playerInfluenceRect[] = addRect("influence",5);
    private Rectangle playerEyeRect[] = addRect("eye",5);

/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	TammanyChip.preloadImages(loader,ImageDir);	// load the images used by stones
    	gameIcon = TammanyChip.board.image;
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
       	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default

        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	InternationalStrings.put(TammanyStrings);
        	InternationalStrings.put(TammanyStringPairs);
        }
         
        MouseColors  = TammanyMouseColors;
        MouseDotColors = TammanyMouseDotColors;

        String type = info.getString(GAMETYPE, TammanyVariation.tammany.name);
        // recommended procedure is to supply players and randomkey, even for games which
        // are current strictly 2 player and no-randomization.  It will make it easier when
        // later, some variant is created, or the game code base is re purposed as the basis
        // for another game.
        bb = new TammanyBoard(s,type,players_in_game,randomKey,getStartingColorMap(),TammanyBoard.REVISION);
        adjustPlayers(players_in_game);
        useDirectDrawing(true); // not tested yet
        doInit(false);

    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        bb.doInit(bb.gametype);						// initialize the board
        if(reviewOnly) { bb.setSimultaneousPlay(false); }
        if(!preserve_history)
    	{   adjustPlayers(bb.nPlayers());
    	startFirstPlayer();
    	}
    }


    public Rectangle createPlayerGroup(int pl,int x,int y,double rotation,int unit)
    {	commonPlayer pl0 = getPlayerOrTemp(pl);
    	Rectangle icon = playerIconRect[pl];
    	Rectangle role = playerRoleRect[pl];
    	Rectangle influence = playerInfluenceRect[pl];
    	Rectangle done = doneRects[pl];
    	Rectangle eye = playerEyeRect[pl];
    	int doneW = G.offline() ? unit*3 : 0;		// we use the done rect even in unplanned seating
    	int px = x+unit*4+doneW;
    	int influenceW = unit*4;
    	int influenceY = y+unit*2;
    	int u3 = unit/3;
    	int u23 = unit*2/3;
    	int u2 = unit/2;
    	G.SetRect(influence, x, influenceY, influenceW,unit*2);
    	G.SetRect(icon, x, y, unit*2, unit*2);
    	Rectangle box = pl0.createRectangularPictureGroup(px,y, unit);
    	G.union(box, icon,influence);
    	G.SetRect(role,G.Right(box), y,unit*3, G.Height(box));
    	int doneX = x+influenceW+u2;
    	int doneY = influenceY+u3;
    	G.SetRect(done, doneX, doneY,doneW ,doneW/2);
    	G.SetRect(eye, doneX+doneW-unit,doneY-u23,u23,u23);
    	G.union(box, role);
    	pl0.displayRotation = rotation;
        return(box);
    }

    static final int boardCols = 17;
    static final int boardRows = 20;  

    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect, x, y, width, height);
    	if(remoteViewer>=0)
    	{
    		for(TammanyCell c = bb.allCells; c!=null; c=c.next) { c.setCurrentCenter(width/2,height/4); }
    	}
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
        int vcrW = fh*15;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.15,	// 60% of space allocated to the board
    			0.75,	// 3:4 aspect ratio for the board
    			fh*2,
    			fh*3,	// maximum cell size
    			0.4		// preference for the designated layout, if any
    			);

        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*3/2);
    	
       	layout.placeDoneEditRep(buttonW,buttonW*4/3,doneRect,editRect);
       	layout.placeTheVcr(this,vcrW,vcrW*3/2);
           	
    	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	boolean portrait = mainH>mainW;
    	// calculate a suitable cell size for the board
    	int cols = portrait ? boardCols : boardRows;
    	int rows = portrait ? boardRows : boardCols;
    	double cs = Math.min((double)mainW/cols,(double)mainH/rows);
    	CELLSIZE = (int)cs;
        int C2 = CELLSIZE/2;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(cols*CELLSIZE);
    	int boardH = (int)(rows*CELLSIZE);
    	int extraW = (mainW-boardW)/2;
    	int extraH = (mainH-boardH)/2;
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
        int stateH = fh*3;
        G.placeRow(stateX+stateH,stateY,boardW-stateH ,stateH,stateRect,annotationMenu,noChatRect);
        G.SetRect(iconRect, stateX, stateY, stateH, stateH);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	if(!portrait)
    	{	contextRotation = Math.PI/2;
    		G.setRotation(boardRect, Math.PI/2);
    	}
        int wardW = CELLSIZE+C2;
    	{
    	int rbx = G.Left(boardRect);
    	int rby = G.Top(boardRect);
    	int rbw = G.Width(boardRect);
    	int rbh = G.Height(boardRect);
        G.SetRect(infoRect,rbx + (int)(rbw*0.72),
        		rby + (int)(rbh*0.7),
        		rbw/5,rbh/6);
            
        int wardX = rbx+rbw-wardW;
        G.SetRect(wardMapRect,wardX,rby+rbh-wardW-C2,wardW,wardW);
    	}
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	if(portrait)
    	{
    	int goalX = boardX+wardW*3/2;
        G.SetRect(goalRect,goalX, boardBottom-stateH,boardW-(goalX-boardX)-C2,stateH);  
    	}
    	else {
            G.SetRect(goalRect,boardX+C2, boardBottom-stateH,boardW-wardW-CELLSIZE,stateH);     		
    	}
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);

    }
          
            
    private void drawPlayerIcon(Graphics gc,TammanyPlayer p,Rectangle icon)
    {
    	drawPlayerChip(gc,p,icon);
     	GC.Text(gc,true,
    			G.Right(icon),G.Top(icon),G.Width(icon),G.Height(icon),
    			Color.black,null,""+p.score);

    }
    private void drawPlayerChip(Graphics gc,TammanyPlayer p,Rectangle icon)
    {
    	TammanyChip myChip = p.myBoss;
    	myChip.drawChip(gc,this,icon,null);
    }

    private void drawPlayerInfluence(Graphics gc,TammanyPlayer p,commonPlayer pl,TammanyBoard gb,
    		Rectangle influence,Hashtable<TammanyCell,TammanyMovespec>targets,HitPoint highlight)
    {
    	int step = G.Width(influence)/9;
    	int stepw = (int)(step*2.3);
    	int x = G.Left(influence)+step;
    	int y = G.centerY(influence)+G.Height(influence)/3;
    	boolean hit = false;
    	hit |= p.slander.drawStack(gc,this,
    				gb.legalToHitChips(p.slander,targets)?highlight:null,
    				stepw,x,y,0,-0.07,0.1,""+p.slander.height());
    	x+= step+step/2;
    	pl.rotateCurrentCenter(p.slander,x,y);
    	for(TammanyCell c : p.influence)
    	{	int off = ((c.row&1)==0) ? -step/4 : step/4;
        	hit |= c.drawStack(gc,this,gb.legalToHitChips(c,targets)?highlight:null,
        			stepw,x,y+off,0,0.1,""+c.height());
        	pl.rotateCurrentCenter(c,x,y+off);
    		if((targets!=null) && targets.get(c)!=null) 
    		{
    			StockArt.SmallO.drawChip(gc,this,step,x,y+off,null);
    		}
 
        	x+= step+step/2; 		
    	}
    	if(hit)
    	{	highlight.arrow = (gb.pickedObject==null) ? StockArt.UpArrow : StockArt.DownArrow;
    		highlight.awidth = step;
    	}
    	
    }
    private void drawPlayerRole(Graphics gc,TammanyPlayer p,TammanyBoard gb,Rectangle role,HitPoint any)
    {
    	int x = G.Left(role);
		int y = G.centerY(role);
		int w = G.Width(role);
		int step = w/4;
    	if(p.myRoleCard!=null)
    	{	TammanyChip card = p.myRoleCard;
     		card.drawChip(gc,this,role,any,TammanyId.RoleCard,card.helpText);
     		if(p.myRole==Role.CouncilPresident)
     		{
     			for(int i=0;i<p.locksAvailable;i++)
     			{
     				TammanyChip.freezer.drawChip(gc,this,step,x,y,null);
     				x+=step/2;
     			}
     		}
       	}
       	else if(p.myIndex==gb.firstPlayer)
    		{
       		StockArt.LandingPad.drawChip(gc,this,step*2,x+step*2,y,"First\nPlayer");
    		}

    	}
	// draw a box of spare chips. For tammany it's purely for effect, but if you
    // wish you can pick up and drop chips.
    private void drawPlayerBoard(Graphics gc, int player,
    							 HitPoint highlight,HitPoint any,TammanyBoard gb,
    							 Hashtable<TammanyCell,TammanyMovespec>targets)
    {	TammanyPlayer p = gb.getPlayer(player);
    	commonPlayer pl = getPlayerOrTemp(player);
       	Rectangle r = pl.playerBox;
    	GC.frameRect(gc,Color.black,r);
    	GC.setFont(gc,largeBoldFont());
    	
    	drawPlayerIcon(gc,p,playerIconRect[player]);
    	drawPlayerInfluence(gc,p,pl,gb,playerInfluenceRect[player],targets,highlight);
    	drawPlayerRole(gc,p,gb,playerRoleRect[player],highlight);
    	Rectangle done = doneRects[player];
    	if(G.offline())
    	{	if(gb.getState().isElection())
    		{
			showVoteButton(gc,p,highlight,done,playerEyeRect[player],p.normalShow,true);
    		}
    		else if(plannedSeating() && gb.whoseTurn==player) 
    		{
			handleDoneButton(gc,done,(gb.DoneState() ? highlight : null), 
					HighlightColor, rackBackGroundColor);
    		}
    	}

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
    	TammanyChip.getChip(obj).drawChip(g,this,CELLSIZE, xp, yp, null);
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  


    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    { // erase
    	boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     TammanyChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       TammanyChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       	drawFixedBoard(gc);
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      }
     Image scaled = null;
     // called after supplying the rotation context
     public void drawFixedBoard(Graphics gc,Rectangle rect)
     {
    	 if(remoteViewer<0) { scaled = TammanyChip.board.image.centerScaledImage(gc, rect,scaled);	} 
         setDisplayParameters(bb,boardRect);      	
     }
    /**
     * translate the mouse coordinate x,y into a size-independent representation
     * presumably based on the cell grid.  This is used to transmit our mouse
     * position to the other players and spectators, so it will be displayed
     * at approximately the same visual spot on their screen.  
     * 
     * Some trickier logic may be needed if the board has several orientations,
     * or if some mouse activity should be censored.
     */
    //public String encodeScreenZone(int x, int y,Point p)
    //{
    //	return(super.encodeScreenZone(x,y,p));
    //}
    private boolean drawElectionDisc(Graphics gc,boolean showVotes,boolean canVoteNow,HitPoint highlight,TammanyPlayer myPlayer,int unitsize,TammanyCell cell,int xpos,int ypos)
    {	boolean some = false;
    	if(showVotes)
    		{
    		String msg = "?/";
    		TammanyChip ch = cell.topChip();
    		boolean canVote =  (preparedVote[myPlayer.myIndex]==null) && canVoteNow;
    		int votes = (ch==null) ? 0 : myPlayer.votes[ch.myInfluenceIndex()];
    		if(votes>=0) { msg = ""+votes+"/"; }
        	if(cell.drawStack(gc,this,canVote ?highlight:null ,unitsize,xpos,ypos,0,0.1,0.0,msg+cell.height()))
        		{
        		some = cell.drawStack(gc,this, highlight ,unitsize+unitsize/3,xpos,ypos,0,0.1,0.0,msg+cell.height());
        		}
    		}
    		else
    		{
    		// draw everyone else's chips
            some = cell.drawStack(gc,this, null ,unitsize,xpos,ypos,0,0.1,0.0,"?/"+cell.height());              			
    		}
    	return(some);
    }
    private boolean drawElectionStack(Graphics gc,boolean seetotal,int unitSize,TammanyCell cell,int xpos,int ypos)
    {
    	// never mouse sensitive
    	String label = cell.label;
    	if(!seetotal)
    	{
    		if((label!=null) && (cell.rackLocation()==TammanyId.ElectionBox))
    			{	int ind = label.indexOf('/');
    			    if(ind>=0) 
    					{ label = label.substring(ind); }
     			}
    		else  { label = null; }
    	}
    	return(cell.drawStack(gc,this, null ,unitSize,xpos,ypos,0,0.1,0.0,label));

    }

   /** draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */
    private void drawBoardElements(Graphics gc, TammanyBoard gb, Rectangle brect,
    		HitPoint highlight,HitPoint any,   	Hashtable<TammanyCell,TammanyMovespec> targets)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  Either gc or highlight might be
    	// null, but not both.
        //
        // this enumerates the cells in the board in an arbitrary order.  A more
        // conventional double xy loop might be needed if the graphics overlap and
        // depend on the shadows being cast correctly.
    	TammanyChip.wardMap.drawChip(gc,this,wardMapRect,null);
    	if(G.pointInRect(any,wardMapRect))
    	{	
    		GC.setFont(gc,largeBoldFont());
    		for(TammanyCell cell : gb.wardBosses)
    		{	if(cell.zone!=null)
    			{
            	int ypos = gb.cellToY(cell);
                int xpos = gb.cellToX(cell);
                TammanyChip.slander.drawChip(gc,this,(int)(CELLSIZE*1.6),xpos+CELLSIZE/2,ypos+(int)(CELLSIZE*0.45),""+cell.row);
    			}
    		}
    		return;
    	}
    	
    	if(giantCard!=null) { highlight = null; }
    	
    	String year = gb.electionMode ? s.get(Election) : s.get(Year,gb.currentYear);
    	String info = gb.electionMode ? s.get(Ward,gb.electionWard())
    								: gb.GameOver() 
    									? s.get(BoardState.GameOverStateDescription)
    									: ((gb.currentYear%4==0) 
    											? ( ((gb.board_state==TammanyState.DistributeRoles)
    													|| (gb.board_state==TammanyState.ConfirmNewRoles))
    													? s.get(TammanyDistribute)
    													: s.get(NextElection) )
    											: "");
    	GC.setFont(gc,largeBoldFont());
    	GC.Text(gc,true,infoRect,Color.black,null,year+"\n"+info);
    	
    	gb.setYearMarker();
    	gb.setScores();
    	gb.setElectionView();
    	
    	{
       	TammanyState state = gb.getState();
       	int whoseMove = gb.whoseTurn;
   	
     	TammanyPlayer myPlayer = simultaneous_turns_allowed() ? gb.players[getActivePlayer().boardIndex] : gb.players[whoseMove];
       	for(TammanyCell cell = gb.uiCells; cell!=null; cell=cell.next)
          {
           boolean hitCell = gb.LegalToHitBoard(cell,targets);
            boolean drawhighlight = hitCell 
   				|| ((gb.pickedObject==null) && gb.isDest(cell)) 		// is legal for a "drop" operation
   				|| ((gb.pickedObject!=null) && gb.isSource(cell));	// is legal for a "pick" operation+
         	int ypos = gb.cellToY(cell);
            int xpos = gb.cellToX(cell);
            HitPoint hit = drawhighlight ? highlight : null;
            boolean some = false;
            switch(cell.rackLocation())
            {
            case Bag:
            	some = TammanyChip.bag.drawChip(gc,this,hit,TammanyId.Bag,CELLSIZE,xpos,ypos,null);
            	cell.rotateCurrentCenter(gc,xpos,ypos);
            	if(bb.getDest()==bb.bag)
            	{	// draw the chip we just dropped on top of the bag
            		bb.bag.topChip().drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            	}
            	if(some) { hit.hitObject = bb.bag; }
            	break;
            case WardBoss:
            case WardCube:
            	{
                if(gb.electionMode && cell.row==gb.electionWard())
                	{
                		StockArt.LandingPad.drawChip(gc,this,3*CELLSIZE/2,xpos,ypos,null);
                	}

            	// spread out the stack if we might be trying to pick something from it.
                boolean isSlander = ((state==TammanyState.SlanderPayment)||(state==TammanyState.DoubleSlander));
            	double space = ((gb.pickedObject==null)
            					 && ((gb.getDest()==null)||isSlander)
            					 && G.pointInRect(hit,xpos-CELLSIZE,ypos-CELLSIZE/2,(int)(CELLSIZE*(cell.height()+1.5)*0.45),CELLSIZE))
            					? 0.45 : 0.2;
            	some = cell.drawStack(gc,this, hit ,CELLSIZE,xpos,ypos,0,space,0.0,null);
            	// special case for slander, we can only slander opponent bosses
            	if(some && isSlander)
            		{
            		int ind = hit.hit_index;
            		TammanyChip ch = ((ind>=0) && (ind<cell.height())) ? cell.chipAtIndex(hit.hit_index) : null;
            		if((bb.pickedObject!=null) ||
            			((ch!=null)
            				&& (((ch==TammanyChip.slander) && (gb.slanderPayment==null))|| ch.isBoss())
            				&& (ch!=myPlayer.myBoss))) 
            			{
            			}
            		else
            			{ hit.hitCode = DefaultId.HitNoWhere; 
            			  hit.hitObject = null; 
            			  some=false;  
            			}
            		}
            	}
            	break;
            case BossPlacement:
            case SlanderPlacement:
            case InfluencePlacement:
            	if(!state.isElection())
            	{
                	some = cell.drawStack(gc,this, hit ,CELLSIZE,xpos,ypos,0,0.1,0.0,null);
          		
            	}
           		break;
            case ElectionDisc:
            	// influence discs in the election grid display
            	if(state.isElection())
            		{	TammanyPlayer diskPlayer = bb.getPlayerOrNull(cell.boss);
            			if(diskPlayer!=null)
            			{
            			boolean show = reviewOnly 
            					|| (G.offline() 
            							? diskPlayer.normalShow 
            							: (cell.boss==myPlayer.myBoss));
            			boolean hasVoted = (bb.players[diskPlayer.myIndex].pendingVote!=null);
            			boolean canvote = !hasVoted && (G.offline() ? show : true);
            			drawElectionDisc(gc, show,canvote,highlight, diskPlayer,CELLSIZE, cell, xpos, ypos);
            			}
            	}
            	break;
            case ElectionBox:
            	// bosses in the election grid display
            	if(state.isElection())
            	{	boolean show = reviewOnly 
            				|| (G.offline()
            						? bb.getPlayerNormalShow(cell.boss) 
            						: (cell.boss==myPlayer.myBoss));
            	    some = drawElectionStack(gc,show,CELLSIZE, cell,xpos,ypos);
            	}
            	break;

            case ElectionBoss:
            	// bosses in the election grid display
            	if(state.isElection())
            	{   some = drawElectionStack(gc,true,CELLSIZE, cell,xpos,ypos);
            	}
            	break;
            case IrishLeader:			// ethnic controls
            case EnglishLeader:
            case GermanLeader:
            case ItalianLeader:
            	some = cell.drawStack(gc,this, hit ,CELLSIZE,xpos,ypos,0,0.4,0.0,null);
            	break;
            	
            case DeputyMayor:
            case CouncilPresident:
            case PrecinctChairman:
            case ChiefOfPolice:
            case Mayor:
            	// 
            	// add a hint what the powers are
            	//
            	switch(cell.rackLocation())
            	{
            	case CouncilPresident:
            		TammanyChip.freezer.drawChip(gc,this,CELLSIZE,xpos+CELLSIZE,ypos,null);
            		break;
            	case ChiefOfPolice:
            		TammanyChip.bag.drawChip(gc,this,7*CELLSIZE/16,xpos+CELLSIZE+CELLSIZE/4,ypos,null);
               		TammanyChip.english.drawChip(gc,this,2*CELLSIZE/3,xpos+CELLSIZE+CELLSIZE/4,ypos,null);
            		break;
            	case DeputyMayor:
            		for(int off = 0;off<TammanyChip.influence.length;off++)
            		{
            		TammanyChip.influence[off].drawChip(gc,this,5*CELLSIZE/6,xpos+CELLSIZE+off*3,
            				ypos-CELLSIZE/4+off*3,null);
            		}
            		break;
            	case PrecinctChairman:
            		TammanyChip.cubeMove.drawChip(gc,this,CELLSIZE,xpos+(int)(1.2*CELLSIZE),ypos-(int)(0.15*CELLSIZE),null);
            		TammanyChip.english.drawChip(gc,this,2*CELLSIZE/3,xpos+(int)(0.8*CELLSIZE),ypos-CELLSIZE/3,null);
            		TammanyChip.english.drawChip(gc,this,2*CELLSIZE/3,xpos+CELLSIZE+CELLSIZE/2,ypos+CELLSIZE/6,null);
             		break;
            		default: ;
            	}
            	some = cell.drawStack(gc,this, hit ,CELLSIZE,xpos,ypos,0,0.1,0.0,null);
            	TammanyChip card = TammanyChip.getRoleCard(cell.rackLocation());
            	if((bb.pickedObject==null) 
            		&& (any.hitCode == DefaultId.HitNoWhere)
            		&& HitPoint.setHelpText(any,xpos-CELLSIZE,ypos-CELLSIZE,CELLSIZE*4,CELLSIZE*2,
            			card.helpText))
            	{
            		any.hitCode = TammanyId.RoleCard;
            		any.hitObject = card; 
            	}
            	break;
            case Trash:
            	some = cell.drawStack(gc,this, hit ,CELLSIZE,xpos,ypos,0,0.3,0.0,null);
            	break;
           default:
            	some = cell.drawStack(gc,this, hit ,CELLSIZE,xpos,ypos,0,0.1,0.0,null);
            	break;
            } 
            if(hit!=null) 
            {
            	StockArt.SmallO.drawChip(gc, this, CELLSIZE,xpos,ypos,null);
            }
            if(some) {
            	hit.awidth = CELLSIZE;
            	hit.arrow = (gb.pickedObject!=null)?StockArt.DownArrow:StockArt.UpArrow;
            }
           }
        }
    	
    	if(giantCard!=null)
    	{
    		giantCard.drawChip(gc, this, brect ,any, TammanyId.RoleCard);
    }
    
    }
    
    public void drawElectionElements(Graphics gc,TammanyPlayer myPlayer,TammanyBoard gb,HitPoint hit,Rectangle r,boolean chosenShow)
    {	GC.frameRect(gc,Color.green,r);
    	int minx=0;
    	int miny=0;
    	int maxx=0;
    	int maxy=0;
    	boolean first=true;
    	// first pass, just size the rectangle for the election elements
      	for(TammanyCell cell = gb.uiCells; cell!=null; cell=cell.next)
      	{
      	switch(cell.rackLocation())
      	{
      	default: break;
      	case ElectionDisc:
      	case ElectionBox:
      	case ElectionBoss:
          	int ypos = gb.cellToY(cell);
          	int xpos = gb.cellToX(cell);
          	if(first) { minx = maxx = xpos; miny=maxy=ypos; first=false; }
          	else
          	{ 	minx = Math.min(xpos, minx); 
          		maxx = Math.max(xpos, maxx);
          		miny = Math.min(ypos, miny);
          		maxy = Math.max(ypos, maxy);
          	}
      	}}
      	
      	int width = G.Width(r);
      	int height = G.Height(r);
      	int left = G.Left(r)+width/10;
      	int space = height/10;
      	int top = G.Top(r)+height/7;
      	double scale = Math.max((double)(maxy-miny)/(height*2/3),(double)(maxx-minx)/width);
       	int size = Math.min(width,height)/4;
      	for(TammanyCell cell = gb.uiCells; cell!=null; cell=cell.next)
      	{
      	switch(cell.rackLocation())
      		{
      		default: break;
      	
      		case ElectionDisc:
      			{
      			int cx = gb.cellToX(cell);
      			int cy = gb.cellToY(cell);
              	int xpos = (int)((cx-minx)/scale)+left;
              	int ypos = (int)((cy-miny)/scale)+top+space;
              	// influence discs in the election grid display
              	boolean show = (cell.boss==myPlayer.myBoss) && chosenShow;
    			boolean canvote = (myPlayer.pendingVote==null);
              	drawElectionDisc(gc, show, canvote, hit, myPlayer,size, cell, xpos, ypos);
      			}
      		break;
      	case ElectionBox:
			{
			int xpos = (int)((gb.cellToX(cell)-minx)/scale)+left;
	        int ypos = (int)((gb.cellToY(cell)-miny)/scale)+top+space;
	        boolean show = (cell.boss==myPlayer.myBoss) && chosenShow;
	  		drawElectionStack(gc,show,size,cell,xpos,ypos);
			}
			break;
      	case ElectionBoss:
			{
			int xpos = (int)((gb.cellToX(cell)-minx)/scale)+left;
            int ypos = (int)((gb.cellToY(cell)-miny)/scale)+top;
      		drawElectionStack(gc,true,size,cell,xpos,ypos);
			}
      		break;
    	}}
    }
    
    int gameLogScroll = 0;
    TammanyMovespec logState[] = new TammanyMovespec[2];
    
    Color YellowPlayerColor = new Color(174,144,21);
    Color PurplePlayerColor = new Color(149,90,183);
    Color BrownPlayerColor = new Color(220,204,204);
    Color RedPlayerColor = new Color(171,48,44);
    
    // these key words are colorized in the game log
    double chipScales[] = {1.0,1.3,0.0,-0.4};
    double cubeScales[] = {1.0,1.7,0.0,-0.2};
    double discScales[] = {1.2,1.2,0.0,-0.2};
    
    Text coloredChunks[] = 
    	{	TextGlyph.create(IrishInfluence,"xx",TammanyChip.greenDisc,this,chipScales),
    		TextGlyph.create(GermanInfluence,"xx",TammanyChip.orangeDisc,this,chipScales),
    		TextGlyph.create(EnglishInfluence,"xx",TammanyChip.whiteDisc,this,chipScales),
    		TextGlyph.create(ItalianInfluence,"xx",TammanyChip.blueDisc,this,chipScales),
    		TextGlyph.create(EnglishToken,"xx",TammanyChip.english,this,cubeScales),
    		TextGlyph.create(ItalianToken,"xx",TammanyChip.italian,this,cubeScales),
    		TextGlyph.create(IrishToken,"xx",TammanyChip.irish,this,cubeScales),
    		TextGlyph.create(GermanToken,"xx",TammanyChip.german,this,cubeScales),
    		TextGlyph.create(RedColor,"xx",TammanyChip.red,this,chipScales),
    		TextGlyph.create(BrownColor,"xx",TammanyChip.brown,this,chipScales),
    		TextGlyph.create(PurpleColor,"xx",TammanyChip.purple,this,chipScales),
    		TextGlyph.create(YellowColor,"xx",TammanyChip.yellow,this,chipScales),
    		TextGlyph.create(BlackColor,"xx",TammanyChip.black,this,chipScales), 
    		TextGlyph.create(SlanderToken,"xx",TammanyChip.slander,this,chipScales),
    		TextGlyph.create(FreezeToken,"xx",TammanyChip.freezer,this,chipScales),
    	};
    
    public Text colorize(String str)
    {	return(TextChunk.colorize(str,s,coloredChunks));
    }

    public Text censoredMoveText(commonMove sp,int idx)
    {	String str = ((TammanyMovespec)sp).censoredMoveString(History,idx,bb);
            String votestring = "vote ward";
    	if(str.startsWith(votestring))
            {	
    		StringTokenizer msg = new StringTokenizer(str.substring(votestring.length()));
		    return TextChunk.join(
                        	TextChunk.create(votestring),
                        	TextChunk.create(" "+msg.nextToken()),
		    		TextGlyph.create("xxx","xxx",msg.nextToken(),TammanyChip.getBoss(sp.player),this,discScales),
                        	TextGlyph.create("xxx","xxx",msg.nextToken(),TammanyChip.greenDisc,this,discScales),
                        	TextGlyph.create("xxx","xxx",msg.nextToken(),TammanyChip.whiteDisc,this,discScales),
                        	TextGlyph.create("xxx","xxx",msg.nextToken(),TammanyChip.orangeDisc,this,discScales),
                        	TextGlyph.create("xxx","xxx",msg.nextToken(),TammanyChip.blueDisc,this,discScales)
                			);
                		}

    	return(colorize(str)); 
                }

    public int ScoreForPlayer(commonPlayer p)
    {
    	return(bb.scoreForPlayer(p.boardIndex));
    }
    public void showVoteButton(Graphics gc,TammanyPlayer p,HitPoint hp,Rectangle r,Rectangle eyeR,boolean eyeState,boolean onlyifactive)
    {	HitPoint select = ((p.allVotesSet
    						&& bb.playerCanVote(p.myIndex)
    						&&(preparedVote[p.myIndex]==null)) ? hp : null);
		if ((select!=null) || !onlyifactive)
			{
			if(GC.handleRoundButton(gc, r, 
        		select, s.get(TammanyVote),
                HighlightColor, rackBackGroundColor))
				{	
				hp.hitCode = TammanyId.Vote;
				hp.hit_index = p.myIndex;
				}
			}
		else if (preparedVote[p.myIndex]!=null)
		{
			GC.Text(gc,true,r,Color.black,null,s.get(YouVotedMessage));
		}
		else if (bb.playerCanVote(p.myIndex))
		{
			GC.Text(gc,true,r,Color.black,null,s.get(VoteNowMessage));
		}
		if(eyeR!=null)
		{
			StockArt icon = eyeState ? StockArt.NoEye : StockArt.Eye;
			if(icon.drawChip(gc, this,eyeR,hp, TammanyId.ShowVotes))
			{
				hp.hit_index = p.myIndex;
				}
			}
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
    	
       TammanyBoard gb = disB(gc);
       setDisplayParameters(gb,boardRect);
       int nPlayers = gb.nPlayers();
       TammanyState state = gb.getState();
       boolean moving = hasMovingObject(selectPos);
       boolean simultaneous = !getActivePlayer().spectator && state.simultaneousTurnsAllowed();
       boolean ourMove = OurMove() || simultaneous;
  
       // 
       // if it is not our move, we can't click on the board or related supplies.
       // we accomplish this by supressing the highlight pointer.
       //
       HitPoint ourTurnSelect = ourMove ? selectPos : null;
       //
       // even if we can normally select things, if we have already got a piece
       // moving, we don't want to hit some things, such as the vcr group
       //
       HitPoint buttonSelect = moving ? null : ourTurnSelect;
       // hit anytime nothing is being moved, even if not our turn or we are a spectator
       HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;
       
       gameLog.redrawGameLog2(gc, nonDragSelect, logRect,
    		   Color.black,boardBackgroundColor,
    		   standardBoldFont(),standardBoldFont());
       Hashtable<TammanyCell,TammanyMovespec> targets = gb.getTargets();
       
       GC.setRotatedContext(gc,boardRect,selectPos,contextRotation);
       drawBoardElements(gc, gb, boardRect, ourTurnSelect,selectPos,targets);
       GC.unsetRotatedContext(gc,selectPos);
           
       
       drawHiddenWindows(gc, selectPos);

       for(int i=0;i<nPlayers;i++)
       {	commonPlayer pl = getPlayerOrTemp(i);
       		pl.setRotatedContext(gc, selectPos, false);
       		drawPlayerBoard(gc,i,ourTurnSelect,selectPos,gb,targets);
       		pl.setRotatedContext(gc, selectPos, true);
       }
       


		if (state != TammanyState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(state.isElection())
			{   if(preparedWard!=gb.electionWard())
				{
				AR.setValue(preparedVote , null);
				AR.setValue(preparedVoteSent , false);
				}
			if(!G.offline())
			{
				int whoseMove = simultaneous_turns_allowed() ? getActivePlayer().boardIndex : gb.whoseTurn;
				TammanyPlayer p = gb.players[whoseMove];
				showVoteButton(gc,p,selectPos,doneRect,null,false,false);
			}}
			else if(!plannedSeating())
				{	
				handleDoneButton(gc,doneRect,(gb.DoneState() ? buttonSelect : null), 
						HighlightColor, rackBackGroundColor);
            }

	
			commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
			double messageRotation = pl.messageRotation();
		  			
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos, HighlightColor, rackBackGroundColor);

        }

 
        drawPlayerStuff(gc,state==TammanyState.Puzzle,nonDragSelect,HighlightColor,rackBackGroundColor);
  
        drawStateMessage(gc,state,stateRect,false);
        drawPlayerChip(gc,bb.getPlayer(bb.whoseTurn),iconRect);

        goalAndProgressMessage(gc,nonDragSelect,Color.black,s.get(TammanyVictoryCondition),progressRect, goalRect);
         
        // draw the vcr controls
        drawVcrGroup(nonDragSelect, gc);

    }
    private void drawStateMessage(Graphics gc,TammanyState state,Rectangle r,boolean hidden)
    {
 
        // draw the avatars
        	String message = null;
        	switch(state)
        	{
        	case Gameover:	
        		message = gameOverMessage();
        		break;
        	case SimultaneousElection:
        	case Election:
        	case SerialElection:
        		{
        		int activePlayer = getActivePlayer().boardIndex;
        		if(!bb.playerCanVote(activePlayer) || (preparedVote[activePlayer]!=null)) 
        			{ message = WaitMessage; }
        		else { message = s.get(state.description(),bb.electionWard()); }
        		//message = "first: "+bb.firstPlayer+" cur: "+bb.whoseTurn+" me "+my.boardIndex;
        		}
        		break;
        	default: 
        		message = s.get(state.description());
        	}
        	//message = my.toString()+message;
    		commonPlayer pl = getPlayerOrTemp(bb.whoseTurn);
    		double messageRotation = hidden ? 0 : pl.messageRotation();

            standardGameMessage(gc,messageRotation,
            		message,
            				(state!=TammanyState.Puzzle)&&!simultaneous_turns_allowed(),
            				bb.whoseTurn,
            				r);
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
    {	//G.print("e "+mm);
        handleExecute(bb,mm,replay);
        
        /**
         * animations are handled by a simple protocol between the board and viewer.
         * when stones are moved around on the board, it pushes the source and destination
         * cells onto the animationStck.  startBoardAnimations converts those points into
         * animation sprites.  drawBoardElements arranges for the destination stones, which
         * are already in place, to disappear until the animation finishes.  The actual drawing
         * is done by drawSprites at the end of redrawBoard
         */
        startBoardAnimations(replay,bb.animationStack,CELLSIZE,MovementStyle.Simultaneous);
        
		lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
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


 public void runAsyncRobots()
 {	
   	if(bb.getState().simultaneousTurnsAllowed())
 	{
 		{
 		for(commonPlayer pp : players)
 			{ if(pp!=null)
 				{ startRobotTurn(pp); }
 			}
 		}
 	}
 }
 public void startRobotTurn(commonPlayer pp)
 {	if(!reviewMode() 
		 && ( ((bb.getState().simultaneousTurnsAllowed()
				 && bb.playerCanVote(pp.boardIndex)
				 &&!bb.playerHasPlayed(pp.boardIndex)) || (bb.whoseTurn==pp.boardIndex))))
 	{
 	super.startRobotTurn(pp);
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
        return (new TammanyMovespec(st, player));
    }

    /** 
     * this method is called from deep inside PerformAndTransmit, at the point
     * where the move has been executed and the history has been edited.  It's
     * purpose is to verify that the history accurately represents the current
     * state of the game, and that the fundamental game machinery is in a consistent
     * and reproducible state.  Basically, it works by creating a duplicate board
     * resetting it and feeding the duplicate the entire history, and then verifying 
     * that the duplicate is the same as the original board.  It's perfectly ok, during
     * debugging and development, to temporarily change this method into a no-op, but
     * be warned if you do this because it is throwing an error, there are other problems
     * that need to be fixed eventually.
     */
    //public void verifyGameRecord()
    //{	//DISABLE_VERIFY = true;
    //	super.verifyGameRecord();
    //}
    
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
        if (hp.hitCode instanceof TammanyId)// not dragging anything yet, so maybe start
        {
        TammanyId hitCode =  (TammanyId)hp.hitCode;
        TammanyCell hitCell = hitCell(hp);
 	    switch(hitCode)
	    {
	    default: break;
	    
	    case WardBoss: 
	    case WardCube:
	       	if(bb.pickedObject==null)
	    	{
	    		PerformAndTransmit("Pickc "+hitCode.shortName+" "+hitCell.row+" "+hp.hit_index );
	    		break;
	    	}
			//$FALL-THROUGH$
		case Zone1Init:
	    case Zone2Init:
	    case Zone3Init:
        case IrishLeader:
        case EnglishLeader:
        case CastleGardens:
        case Mayor:
        case DeputyMayor:
        case CouncilPresident:
        case PrecinctChairman:
        case ChiefOfPolice:
	    	PerformAndTransmit("Pickb "+hitCode.shortName+" "+hitCell.row);
	    	break;
        }

        }
    }
   
   PopupManager electionMenu = new PopupManager(); 
   TammanyChip electionChip = null;
   TammanyChip electionPlayer = null;
   void electionMenu(MenuParentInterface parent,TammanyCell c,int cx,int cy)
   {
	   electionMenu.newPopupMenu(parent,deferredEvents);
	   electionMenu.addMenuItem(s.get(ChipsNone),0);
	   electionMenu.addMenuItem(s.get(ChipsOne),1);
	   electionChip = c.topChip();
	   electionPlayer = c.boss;
	   for(int i=2;i<=c.height();i++) { electionMenu.addMenuItem(s.get(ChipsMany,i),i); }
	   electionMenu.show(cx,cy)  ;
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
    {	int remoteIndex = remoteWindowIndex(hp);
        CellId id = hp.hitCode;
       	if(!(id instanceof TammanyId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        TammanyId hitCode = (TammanyId)id;
        TammanyCell hitObject = hitCell(hp);
        switch (hitCode)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitCode);
        case RoleCard:
        	if(remoteIndex<0)
        		{ TammanyChip newcard = (TammanyChip)hp.hitObject;
        		  if(newcard==giantCard) { newcard=null; }
        		  giantCard = newcard;
        		}
        	break;
        case ShowVotes:
        	{
        		TammanyPlayer p = bb.players[hp.hit_index];
        		if(remoteIndex>=0) { p.hiddenShow = !p.hiddenShow; }
        		else { p.normalShow = !p.normalShow; }
            }
        	break;
        case Vote:
        	{
        	int whoseMove = hp.hit_index;
        	TammanyPlayer p = bb.players[whoseMove];
        	String votes = ""+bb.bossVotes(whoseMove);
        	for(int i : p.votes) { votes += " "+((i>=0) ? i : 0); }
        	if(bb.getState()==TammanyState.SimultaneousElection)
        	{
       		preparedVote[whoseMove] = "Vote "+votes;
       		preparedWard = bb.electionWard();
        	}
        	else
        	{
        	PerformAndTransmit("Vote "+votes);
        	}
        	}
        	break;
        case ElectionDisc:
        	// pop up a menu of choices
        	{
        	HiddenGameWindow hidden = findHiddenWindow(hp);
        	electionMenu(hidden==null ? this : hidden,hitObject,hp.hit_x,hp.hit_y);
        	}
        	break;
        case InfluencePlacement:
        	{
        	TammanyPlayer p = bb.players[bb.whoseTurn];
        	int row = hitObject.row;
        	TammanyCell d = p.influence[row];
        	if(bb.pickedObject!=null)
        	{
            	PerformAndTransmit("Dropb "+hitCode.shortName+" "+hitObject.row);       		
        	}
        	else
        	{
        	if(bb.getState()==TammanyState.SlanderPayment)
        		{ PerformAndTransmit("pickb "+hitCode.shortName+" "+hitObject.row); }
        	else { PerformAndTransmit("move "+hitCode.shortName+" "+hitObject.col+" "+row+" "+d.rackLocation().shortName +" "+d.col+" "+d.row); }
        	//  move InfluencePlacement @ 0 Influence A 0
        	}}
        	break;
        case PlayerSlander:
        case PlayerInfluence:
        	PerformAndTransmit(((bb.pickedObject!=null) ? "Drop ":"Pick ")+hitCode.shortName+" "+hitObject.col+" "+hitObject.row);
        	break;
        case WardBoss:
        case WardCube:
        	if(bb.pickedObject==null)
        	{
        		PerformAndTransmit("Pickc "+hitCode.shortName+" "+hitObject.row+" "+hp.hit_height );
        		break;
        	}
			//$FALL-THROUGH$
		case Zone1Init:	// we hit an occupied part of the board
        case Zone2Init:
        case Zone3Init:
        case IrishLeader:
        case EnglishLeader:
        case CastleGardens:
        case Mayor:
        case DeputyMayor:
        case CouncilPresident:
        case PrecinctChairman:
        case ChiefOfPolice:
        case BossPlacement:
        case Bag:
        case YearIndicator:
        case LockPlacement:
        case ScoringTrack:
        case SlanderPlacement:
        case Trash:
         	PerformAndTransmit(((bb.pickedObject!=null) ? "Dropb ":"Pickb ")+hitCode.shortName+" "+hitObject.row);

			break;
			   
        }
        }
    }


    private boolean setDisplayParameters(TammanyBoard gb,Rectangle r)
    { 	double inner_x = 0.01;	// ad-hoc adjustment from the boardrect to the actual map coordinates
    	double inner_y = 0.03;
    	double inner_w = 0.01;
    	double inner_h = 0.030;
    	Rectangle innerRect = new Rectangle(G.Left(r)+(int)(inner_x*G.Width(r)),
				  G.Top(r)+(int)(inner_y*G.Height(r)),
				  G.Width(r) - (int)((inner_x+inner_w)*G.Width(r)),
				  G.Height(r) - (int)((inner_y+inner_h)*G.Height(r)));
      	boolean complete = false;
      	
      	gb.SetDisplayRectangle(innerRect);
      						  
      	return(complete);
    }
   	
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
    public String sgfGameType() { return(Tammany_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	int np = G.IntToken(his);	// players always 2
    	long rv = G.IntToken(his);
    	int rev = G.IntToken(his);	// rev does't get used either
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        // int rk = G.IntToken(his);
    	// bb.doInit(token,rk);
        bb.doInit(token,rv,np,rev);
    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {	boolean is = electionMenu.selectMenuTarget(target);
        if(is)
        	{
        	TammanyPlayer p = bb.getPlayer(electionPlayer);
        	TammanyChip top = electionChip;
        	if(top!=null)
        	{
        		p.votes[top.myInfluenceIndex()] = electionMenu.value;
        		repaint();
         	}
        	return(true);
        	}
        else { return(super.handleDeferredEvent(target,command)); }
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
    public boolean canAutoElect()
    {
    	return ( (bb.getState()==TammanyState.Election)
    			&& !reviewMode()
    			&& (players[bb.whoseTurn]!=null)
    			&& (players[bb.whoseTurn].robotPlayer==null)
    
    			);
    }
    public boolean allowBackwardStep()
    {
    	return (super.allowBackwardStep()
    			|| canAutoElect());
    }
    public void ViewerRun(int wait)
    {
        super.ViewerRun(wait);
        if(simultaneous_turns_allowed())
        { 
        	runAsyncRobots();
        }
        //
        // delay sending votes until it's our natural turn
        // the usual "OurMove" logic is not correct because
        // it explicitly considers simultunous moves as ok
        //
        if(canAutoElect() && OurMove())
        { 	
        	PerformAndTransmit("Elect "+bb.nextElectionWard());
        }
        else if(!reviewMode() && (preparedVote!=null))
        {	int who = bb.whoseTurn;
        	String sp = preparedVote[who];
        	if( (sp!=null) && !preparedVoteSent[who])
        	{
        		preparedVoteSent[who] = true;
        	PerformAndTransmit(sp);
        }
        }
    }
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
    {  return new TammanyPlay();
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/23/2023
		57 files visited 0 problems
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
    // create the appropriate set of hidden windows for the players when
    // the number of players changes.
 
 
    public void adjustPlayers(int np)
    {
        int HiddenViewWidth = 800;
        int HiddenViewHeight = 400;
        super.adjustPlayers(np);
    	preparedVoteSent = new boolean[np];
    	preparedVote = new String[np];
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
    	TammanyPlayer p = bb.getPlayer(index);
    	if(p!=null)
    	{
     	commonPlayer pl = getPlayerOrTemp(index);
    	TammanyState state = bb.getState();
    	String name = prettyName(index);
    	int margin = G.minimumFeatureSize()/4;
    	int h = G.Height(r);
    	int w = G.Width(r);
    	int l = G.Left(r);
    	int t = G.Top(r);
    	int topPart = h/10;
    	int topSpace = topPart*2;
    	int textW = w-margin*2;
    	int textX = l+topSpace;
    	Rectangle infoRect = addRect("sideinfo",textX,t,textW,topPart);
    	Rectangle alertRect =addRect("sidealert",textX, t+h-topPart,textW,topPart);
    	Font myfont = G.getFont(largeBoldFont(), topPart/2);
   	   	GC.setFont(gc,myfont);
   	   	if(remoteViewer<0)
    	{
    	GC.fillRect(gc,rackBackGroundColor,r);
    	}
   	   	else { labelFont =myfont; }
    	GC.Text(gc,true,infoRect,Color.black,null,s.get(ServiceName,name));
  
    	GC.setFont(gc,myfont);
    	Rectangle icon = addRect("sideicon",l,t,topSpace,topSpace);
    	int rolew = topSpace*3;
    	int inflw = w-rolew-margin*2;
       	Rectangle role = addRect("siderole",l+margin+inflw,t,rolew,topSpace*5);
       	Rectangle influence = addRect("sideInfluence",l+margin,t+topSpace,inflw,topSpace*3);
    	Rectangle stateRect =addRect("sidestate",l+topSpace*2,t+topSpace/2,w-margin*2,topPart);
    	drawPlayerIcon(gc,p,icon);
    	
    	drawStateMessage(gc,state,stateRect,true);
    	int tsize = topSpace*3;
    	int donew = topSpace*2;
    	int ew = margin+donew;
    	int et = topSpace+margin;
		Rectangle er = addRect("sideer",l+ew,t+et,w-ew-margin,h-et-margin);
		Rectangle done = addRect("sidedone",l+margin,t+h-tsize,donew,topSpace);

		
 
    	if(state.isElection())
    	{	bb.setElectionView();		// prepare the election UI elements
    		int ts = topSpace/2;
    		int t23 = topSpace*2/3;
    		Rectangle eye = new Rectangle(G.Right(done)-t23,G.Top(done)-t23,ts,ts);
    		drawElectionElements(gc,p,bb,hp,er,p.hiddenShow);
    		showVoteButton(gc,p,hp,done,eye,p.hiddenShow,true); 
    	}
    	else {
        	drawPlayerInfluence(gc,p,pl,bb,influence,null,null);
        	drawPlayerRole(gc,p,bb,role,hp);
    	}

    	int who = bb.whoseTurn;  	
    	if(who==index)
    	{
    		GC.setFont(gc, myfont);
    		GC.Text(gc, true, alertRect,
    				Color.red,null,s.get(YourTurnMessage));
    	}
    }}
    
    public void performUndo()
    {	super.performUndo();
    //	if(allowBackwardStep()) { doUndoStep(); }
    //	else if(allowUndo()) { doUndo(); }
    	TammanyState state = bb.getState();
    	if(state.isElection()) 
    		{ 
    		AR.setValue(preparedVote,null); 
    		AR.setValue(preparedVoteSent,false); 
    		}
    }
    public boolean allowUndo()
    {		
    	if(bb.movingObjectIndex()>0) { return true; }
    	TammanyState state = bb.getState();   	
    	return (state.allowUndo 
    			&& super.allowUndo());
    }
	/**
	 * this is the key to limiting "runaway undo" in situations where the player
	 * might have made a lot of moves, and undo should limit the damage.  One
	 * example of this is in perliminary setup such as arimaa or iro
	 */
	public boolean allowPartialUndo()
	{
		return super.allowPartialUndo() || canAutoElect() ;
	}
    public void doUndoStep()
    {
    	super.doUndoStep();
    	TammanyState state = bb.getState();
    	if(state.isElection()) 
    		{ 
    		AR.setValue(preparedVote,null); 
    		AR.setValue(preparedVoteSent,false); 
    		}
    }
    public void doUndo()
    {
    	super.doUndo();
    }
	  public boolean allowOpponentUndoNow() 
	  {
		  return super.allowOpponentUndoNow();
	  }
	  public boolean allowOpponentUndo() 
	  {
		  return super.allowOpponentUndo();
	  }


    //public boolean allowBackwardStep()
    //{	BoardProtocol b = getBoard();
  	//return(b==null?false:b.movingObjectIndex()>=0);
    //}
}

