package goban;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;

import online.common.*;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;
import goban.shape.shape.ShapeLibrary;
import java.util.*;

import lib.Graphics;
import lib.CellId;
import lib.ErrorX;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.OStack;
import lib.PopupManager;
import lib.StockArt;

import static goban.GoMovespec.*;
/**
 * This code shows the overall structure appropriate for a game view window.
*/
public class GoViewer extends CCanvas<GoCell,GoBoard> implements GoConstants, GameLayoutClient
{	
    static final String Go_SGF = "Go"; // sgf game name
    static final String ImageDir = "/goban/images/";
    // gzip is a compressed lisp file.  Use this when doing the 
    // one-time conversion from lisp data to codename1 externalizable form
	//static final String ShapeLocation = "/goban/data/shape-data.gzip";
	// zip is a serialized (for codename1) processed file
	static final String ShapeLocation = "/goban/shape/data/shape-data.zip";

	class ScoredGame implements Comparable<ScoredGame>
	{ double score; 
	  sgf_game game;
	  ScoredGame(double s,sgf_game g) { score = s; game = g; }
	  public int compareTo( ScoredGame o)
	  {	
		return(Math.abs(score) > Math.abs(o.score) ? 1 : Math.abs(score)==Math.abs(o.score) ? 0 : -1); 
	}
	}
	class ScoredGameStack extends OStack<ScoredGame>
	{
		public ScoredGame[] newComponentArray(int n) { return(new ScoredGame[n]); }
	}
	

		
	static final long serialVersionUID = 1L;
	// colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    private Color chatColor = new Color(239,230,214);
    private PopupManager handicapMenu = null;
    private PopupManager komiMenu = null;
    // private state
    private GoBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
     private Rectangle chipRects[] = addRect("chip",2);
    
    private Rectangle reverseViewRect = addRect("reverse");
    private Rectangle numberRect = addRect("number");
    private Rectangle annotationRect = addRect("annotation");
    
    private JCheckBoxMenuItem reverseOption = null;
    private Rectangle captiveRect[] = addRect("Captives",2);
    
    private Rectangle pleaseUndoRect = addRect("PleaseUndo");
    private Rectangle okUndoRect = addRect("OkUndo");
    private Rectangle dontUndoRect = addRect("DontUndo");

    private Rectangle passRect = addRect("passRect");
    private Rectangle komiRects[] = addRect("komiRect",2);
    private Rectangle classifyRect = addRect("classifyRect");
    private Rectangle classifyAllRect = addRect("classifyAllRect");

    public void preloadImages()
    {	
       	GoChip.preloadImages(loader,ImageDir);
       	gameIcon = GoChip.GoIcon.image;
    }

    public void setRootProperties(sgf_node root)
    {
    	for(Enumeration<String> prop = b.properties.keys();
    		prop.hasMoreElements();)
    	{
    		String propname = prop.nextElement(); 
    		root.set_property(propname,b.getProp(propname));
    	}

    }
    /**
     * this modifies the standard behavior during scoring.  
     * If the non-current player moves his mouse, it has the effect of calling this
     * in the current player, to ask him to give up control.  During scoring, we
     * issue a "nextPlayer" to hand off control to the active player.
     */
    public void setControlToken(boolean to,long time)
    {	super.setControlToken(to,time);
     	if(OurMove() && !to && (b.getState()==GoState.Score))
    	{
    		PerformAndTransmit("NextPlayer");
    	}	
    }
    public boolean playerChanging()
    {
    	switch(b.getState())
    	{
    	default: return super.playerChanging();
    	case Score: return(false);	// inhibit the noise while scoring
    	}
    }
    

    
	/**
	 * 
	 * this is the real instance initialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	// int players_in_game = Math.max(3,info.getInt(exHashtable.PLAYERS_IN_GAME,4));
    	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
    	super.init(info,frame);
    	//
    	// load the shape library for use in scoring
    	//
    	ShapeLibrary.load(ShapeLocation);
    	MouseDotColors = new Color[]{Color.white,Color.black};
    	MouseColors = new Color[]{Color.black,Color.white};
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//

        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       
        b = new GoBoard(info.getString(OnlineConstants.GAMETYPE, Variation.Go_19.name),
        		randomKey,players_in_game,getStartingColorMap());
        useDirectDrawing(true);
        doInit(false);
        reverseOption = myFrame.addOption(s.get(ReverseView),b.reverseY(),deferredEvents);
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	GoConstants.putStrings();
        }

        
     }

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
    	PerformAndTransmit("Start P0",false,replayMode.Replay);
    	}

    }

    /**
     * translate the mouse coordinate x,y into a size-independent representation
     * presumably based on the cell grid.  This is used to transmit our mouse
     * position to the other players and spectators, so it will be displayed
     * at approximately the same visual spot on their screen.  
     * The results of this function only have to be interpreted by {@link #decodeScreenZone}
     * Some trickier logic may be needed if the board has several orientations,
     * or if some mouse activity should be censored.
     */
   // public String encodeScreenZone(int x, int y,Point p)
   // {
   // 	return(super.encodeScreenZone(x,y,p));
   // }

    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int CSIZE)
    {   
    	commonPlayer pl0 = getPlayerOrTemp(player);
    	Rectangle chipRect = chipRects[player];
    	Rectangle p0time = pl0.timeRect;
    	Rectangle p0alt = pl0.extraTimeRect;
    	Rectangle p0anim = pl0.animRect;
    	Rectangle firstPlayerRect = pl0.nameRect;
    	Rectangle firstPlayerPicRect = pl0.picRect;
    	int chipW = CSIZE*4;
    	Rectangle komiR = komiRects[player];
    	
    	G.SetRect(chipRect, x, y, chipW,chipW);
    	//first player name
    	G.SetRect(firstPlayerRect,x+chipW+CSIZE,y,
    			CSIZE * 8,CSIZE*2);

    	Rectangle cr0 = captiveRect[player];
    	int cr0y = y+chipW+CSIZE; 
    	G.SetRect(cr0,
    		G.Left(firstPlayerRect),
    		cr0y,
    		CSIZE*2,CSIZE*2);
    
    	// first player portrait
    	G.SetRect(firstPlayerPicRect,
    		G.Right(cr0)+CSIZE/2 ,
    		y+CSIZE*2,
    		CSIZE * 6,CSIZE * 6);
  
   	
    	// time display for first player

    	G.SetRect(p0time, G.Right(firstPlayerRect),G.Top(firstPlayerRect),
    			CSIZE * 4,CSIZE);
    	G.AlignLeft(p0alt,G.Bottom(p0time),
    			p0time);


    	// first player "i'm alive" animation ball
    	G.SetRect(p0anim,G.centerX(p0time),G.Bottom(p0alt),
    			CSIZE,CSIZE);
    	Rectangle box = pl0.playerBox;
    	G.SetHeight(box, -1);
    	
        G.SetRect(komiR,x,y+chipW,chipW,chipW/3);

    	G.union(box, firstPlayerRect,komiR,chipRect,firstPlayerPicRect,p0time);
    	pl0.displayRotation = rotation;
    	return(box);

    }
    public void setLocalBounds(int x, int y, int width, int height)
    {
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
        int buttonW = fh*8;
        int margin = fh/2;
       	int undosize = Math.max(G.getFontMetrics(standardBoldFont()).stringWidth(s.get(UndoRequest)),
       						buttonW*2+margin*2);
        int nrows = b.boardRows;  
        int ncols = b.boardColumns;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	//double bestPercent = 
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.7,	// 60% of space allocated to the board
    			0.9,	// aspect ratio for the board
    			fh,
    			fh*1.5,	// maximum cell size
    			0.3		// preference for the designated layout, if any
    			);
    	
        // place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    						       logRect, minLogW,  minLogH,  minLogW, minLogH*3/2);
    	
    	// allocate the undo group as a single piece, then split it into a "please" button
    	// on top with "ok" and "no" buttons below.
      	int undoH = fh*4;
      	layout.placeRectangle(pleaseUndoRect, undosize,undoH*2+buttonW/2+margin*2,BoxAlignment.Center);
      	//G.Assert(G.Width(pleaseUndoRect)==undosize, "wrong size");
       	G.SetHeight(pleaseUndoRect, fh*4);
       	int undoX = G.Left(pleaseUndoRect);
       	int undoY = G.Top(pleaseUndoRect);
       	G.SetRect(okUndoRect, undoX,undoY+fh*4,undosize/2,fh*4);
       	G.AlignTop(dontUndoRect,undoX+undosize/2, okUndoRect);
       	G.SetRect(passRect, undoX, undoY+undoH*2+margin,buttonW,buttonW/2);
       	G.AlignTop(doneRect,undoX+buttonW+margin,passRect);
        G.AlignLeft(editRect, undoY,doneRect);

    	layout.placeTheVcr(this,vcrW,vcrW);
    	// place if there's a space
    	if(G.debug())
    	{
      	layout.placeRectangle(classifyRect, buttonW, buttonW/2,buttonW, buttonW/2,BoxAlignment.Edge,true);
       	layout.placeRectangle(classifyAllRect, buttonW, buttonW/2,buttonW, buttonW/2,BoxAlignment.Edge,true);
    	}
            	
       	Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int stateH = (int)(fh*2.5);
     	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH)/nrows);
    	CELLSIZE = (int)cs;
    	//G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(nrows*cs);
    	int extraW = Math.max(0, (mainW-boardW)/2);
    	int extraH = Math.max(0, (mainH-stateH-boardH)/2);
    	int boardX = mainX+extraW;
    	int boardY = mainY+stateH+extraH;
    	int boardBottom = boardY+boardH;
    	//
    	// state and top ornaments snug to the top of the board.  Depending
    	// on the rendering, it can occupy the same area or must be offset upwards
    	//
        int stateY = boardY-stateH;
        int stateX = boardX;
        G.placeStateRow(stateX,stateY,boardW ,stateH,iconRect,stateRect,annotationRect,numberRect,reverseViewRect,noChatRect);
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	
    	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	G.SetRect(goalRect, boardX, boardBottom-stateH,boardW,stateH);       
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatColor,chatColor);
 	
    }

	
    private void DrawNumberMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	int width = G.Width(r);
    	if(GoChip.white.drawChip(gc,this,highlight,GoId.NumberViewButton,width,G.centerX(r),G.centerY(r),"#"))
    	{	highlight.spriteRect = r;
    		highlight.spriteColor = Color.red;
			highlight.setHelpText(s.get(NumberViewExplanation));
    	}
     }  
    
	
    private void DrawAnnotationMarker(Graphics gc, Rectangle r,HitPoint highlight)
    {	
    	GoChip.triangle.drawChip(gc,this,r,highlight,GoId.AnnotationViewButton,s.get(AnnotationViewExplanation));
    	
     }
    
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, GoBoard gb, int forPlayer, Rectangle r, int player, HitPoint highlight)
    {	
        GoCell thisCell = gb.rack[forPlayer];
        boolean canHit = gb.LegalToHitChips(thisCell);
        GoChip thisChip = thisCell.topChip();
        boolean canDrop = hasMovingObject(highlight);
        boolean canPick = (thisChip!=null);
        boolean scoring = gb.getState().isScoringOrOver() ;
        HitPoint pt = (canHit && (canPick||canDrop))? highlight : null;
        double dscore = gb.territoryForPlayer(forPlayer);
        double komi = gb.getKomi();
        boolean isint = (komi-(int)komi)==0.0;
        String scorestr = isint ? ""+(int)dscore : ""+dscore;
        String score = scoring ? scorestr : null;
        GC.setFont(gc,largeBoldFont());

        if(thisCell.drawStack(gc,this,r,pt,0,0,score))
        {	highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
        	highlight.awidth = G.Width(r)/2;
        	highlight.spriteColor = Color.red;
        }
     }
    private String komiMessage()
    {	double komi = b.getKomi();
    	return((komi==0.0) ? s.get(NoKomiAction) : s.get(KomiPoints,""+komi));

    }
    private void showKomi(Graphics gc,Rectangle r,GoBoard gb)
    {
    	String msg = komiMessage();
    	GC.frameRect(gc,Color.black,r);
    	GC.Text(gc,true,r,Color.black,null,msg);
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
    	GoChip ch = GoChip.getChip(obj);// Tiles have zero offset
    	ch.drawChip(g,this,CELLSIZE,xp,yp,null);
     }


    /** this is used by the game controller to supply entertainment strings to the lobby */
    public String gameProgressString()
    {	// this is what the standard method does
    	// return ((reviewer ? s.get(ReviewAction) : ("" + viewMove)));
    	return(super.gameProgressString());
    }



    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {	boolean reviewBackground = reviewMode()&&!mutable_game_record;
      // erase
      setupBoardRect(b,boardRect);
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //G.fillRect(gc, fullRect);
     GoChip.backgroundTile.image.tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       GoChip.backgroundReviewTile.image.tileImage(gc,boardRect);   
      }
       
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
     b.variation.image.image.centerImage(gc, boardRect);

      b.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.black,Color.black);
      // add the hoshi points
      int centerx = (b.boardColumns+1)/2;
      int centery = (b.boardRows+1)/2;
      drawHoshi(gc,b,centerx,centery,boardRect);
   
      drawHoshi(gc,b,4,4,boardRect);
      drawHoshi(gc,b,4,b.boardRows-3,boardRect);
      drawHoshi(gc,b,b.boardColumns-3,b.boardRows-3,boardRect);
      drawHoshi(gc,b,b.boardColumns-3,4,boardRect);
      
      drawHoshi(gc,b,centerx,4,boardRect);
      drawHoshi(gc,b,centerx,b.boardColumns-3,boardRect);
      drawHoshi(gc,b,b.boardRows-3,centerx,boardRect);
      drawHoshi(gc,b,4,centery,boardRect);
    }
    private void drawHoshi(Graphics gc,GoBoard gb,int x,int y,Rectangle r)
    {
    	GoCell c = gb.getCell((char)('A'+x-1),y);
    	int xp = gb.cellToX(c);
    	int yp = gb.cellToY(c);
    	GoChip.hoshi.drawChip(gc,this,Math.max(5, CELLSIZE/5),G.Left(r)+xp,G.Bottom(r)-yp,null);
    	
    }
    String scoringErrata = null;
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, GoBoard gb, Rectangle brect, HitPoint highlight)
    {
    	GoState state = gb.getState();
    	boolean scoring = state.isScoring(); 
    	Hashtable<GoCell,GoChip> ghostChips = scoring ? gb.getGhostCells() : null;
    	int CS = CELLSIZE/4;
    	GoCell last = gb.lastHit;
   	
    	if(scoring && (gc!=null))
    	{
    		gb.classifyBoard();
    		scoringErrata = gb.classificationExceptions();
    		SimpleGroup exceptions = gb.exceptions;
    		if(exceptions.size()==0) { scoringErrata=null; }
    		else {
    			for(GoCell cell = gb.allCells; cell!=null; cell=cell.next) 
    			{
    				if(exceptions.containsPoint(cell)!=null)
    				{
    	            int ypos = G.Bottom(brect) - gb.cellToY(cell.col, cell.row);
    	            int xpos = G.Left(brect) + gb.cellToX(cell.col, cell.row);
    	            
    	            StockArt.LandingPad.drawChip(gc,this,CELLSIZE*2,xpos,ypos,null);
    	            }
    			}
    		}
    		
    	}
    	for(GoCell cell = gb.allCells; cell!=null; cell=cell.next)  { cell.displayString = null; }
    	boolean twoDigits = false;
    	switch(NumberingMode.selected)
    	{	case None:
    			break;
    		default:
    		{
    		int beginning =0;
    		int end = reviewMode()?getReviewPosition():History.size();
    		commonMove target = null;
    		String relativeTo = null;
    		switch(NumberingMode.selected)
    		{
    		case From_Here:
    		case Last_Branch:	
    			target = NumberingMode.starting; 
    			relativeTo = (target==null) ? null : target.getSliderNumString();
    			/* fall through */
				//$FALL-THROUGH$
			case All: beginning = 0; break;
    		case Last:
    			twoDigits = true;
    			beginning = Math.max(0,end-1); break;
    		case Last_5:
    			twoDigits = true;
    			beginning = Math.max(0,end-6); break;
    		default: break;
    		}
    		for(int i = beginning; i<end; i++)
    		{
    			GoMovespec m = (GoMovespec)History.elementAt(i);
    			GoCell c = gb.getCell(m.to_col,m.to_row);
    			if(m==target) { target=null; }
    			if(c!=null && (target==null))
    			{	String dstring = m.getSliderNumString();
    				if(relativeTo!=null)
    				{	try {
    					int base = G.IntToken(dstring);
    					int ext = G.IntToken(relativeTo)-1;
    					dstring = ""+(base-ext);
    				}
    				catch (NumberFormatException err) { }
    				}
    				c.displayString = dstring;
    			}
    		}
    		}
    	}
    	// conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
    	GoCell hitCell = null;
    	for(GoCell cell = gb.allCells; cell!=null; cell=cell.next)
        	{ 
            int ypos = G.Bottom(brect) - gb.cellToY(cell.col, cell.row);
            int xpos = G.Left(brect) + gb.cellToX(cell.col, cell.row);
            HitPoint hitNow = gb.legalToHitBoard(cell,ghostChips) ? highlight : null;
            String num = cell.displayString;
            if(twoDigits && num!=null && num.length()>2) { num = num.substring(num.length()-2); }
            
            int finalSize = CELLSIZE;
            if(scoring && G.debug())
            {
            	Kind k = cell.getKind();
            	switch(k)
            	{
            	case SafeWhite:
            	case SafeBlack:
            	case DeadWhite:
            	case DeadBlack:
            		finalSize = (int)(finalSize*0.9);
            		break;
           		default: break;
            	}
            }

           
            boolean hit = cell.drawStack(gc,this,hitNow,finalSize,xpos,ypos,0,0.1,num);
            if((cell.annotation!=null) && (cell.annotationStep==gb.moveNumber))
            {
            	hit |= cell.annotation.drawChip(gc,this,hitNow,GoId.AnnotationViewButton,CELLSIZE,xpos,ypos,null);
            }
            if( hit) 
            	{ // draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
            	hitCell = cell;
            	}
            if(cell==last) 
            	{	int sz = 3*CS/2;
            		//StockArt.Dot.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            		GC.frameRect(gc,Color.gray,xpos-sz/2,ypos-sz/2,sz,sz); 
            	}
            if(scoring)
                {
                if(G.debug() && G.pointInRect(highlight,xpos-CELLSIZE/2,ypos-CELLSIZE/2,CELLSIZE,CELLSIZE))
                {
                	highlight.setHelpText(""+cell.getGroup());
                }
            	//if(cell.topChip()==null)
            	{	GoChip ghost = ghostChips.get(cell);
            		if(ghost!=null)
            		{
            			ghost.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            		}
            		Kind kind = cell.getKind();

            		switch(kind)
            		{
            		case SafeWhite:
            			StockArt.SmallO.drawChip(gc,this,CELLSIZE/2,xpos,ypos,null);
            			break;
            		case SafeBlack:
            			StockArt.Dot.drawChip(gc,this,CELLSIZE/2,xpos,ypos,null);
            			break;
            		case BlackTerritory:
            		case BlackSnapbackTerritory:
               			GoChip.black.drawChip(gc,this,CELLSIZE/2,xpos,ypos,null);
               			break;
            		case WhiteTerritory:
            		case WhiteSnapbackTerritory:
            			GoChip.white.drawChip(gc,this,CELLSIZE/2,xpos,ypos,null);
            			break;
            		case FalseEye:
             		case FillBlack:
             		case FillWhite:
    				case ReservedForWhite:
    				case ReservedForBlack:

             			if(G.debug())
             			{
             			StockArt.SmallO.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
             			}
						//$FALL-THROUGH$
					case DeadWhite:
             		case DeadBlack:
             		case RemovedWhite:
             		case RemovedBlack:
             		case OutsideDame:
             		case BlackDame:
             		case WhiteDame:
             			StockArt.SmallX.drawChip(gc,this,CELLSIZE,xpos,ypos,null);
            			break;
            		case Dame:
   
             		default:
            				break;
            		}
            	}
            }
    	}
    	if(hitCell!=null)
    	{
    		highlight.arrow =hasMovingObject(highlight) 
      				? StockArt.DownArrow 
      				: hitCell.topChip()!=null?StockArt.UpArrow:null;
            highlight.awidth = CELLSIZE/2;
            highlight.spriteColor = Color.red;
    	}
    }
    
    // draw a small stone with the number of captives
    public void drawCaptives(Graphics gc,Rectangle r,CellStack stack,GoChip chip)
    {
    	GC.frameRect(gc,Color.black,r);
    	chip.drawChip(gc,this,r,""+stack.size());
    }
     public void drawAuxControls(Graphics gc,GoBoard gb,HitPoint highlight)
    {  
       DrawReverseMarker(gc,reverseViewRect,highlight,GoId.ReverseViewButton);
       DrawNumberMarker(gc,numberRect,highlight);
       DrawAnnotationMarker(gc,annotationRect,highlight);
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  GoBoard gb = disB(gc);
      setupBoardRect(gb,boardRect);
      boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
      GoState vstate = gb.getState();
       redrawGameLog(gc, ourSelect, logRect, boardBackgroundColor);
    
        drawBoardElements(gc, gb, boardRect, ot);
       	boolean firstMove = gb.isFirstMove() || (vstate==GoState.ConfirmHandicapState);
       
         for(int i=FIRST_PLAYER_INDEX;i<=SECOND_PLAYER_INDEX;i++)
          {	commonPlayer pl = getPlayerOrTemp(i);
          	pl.setRotatedContext(gc, highlight, false);
            DrawCommonChipPool(gc, gb,i,chipRects[i], gb.whoseTurn,ot);
            drawCaptives(gc,captiveRect[i],gb.captureStack[i^1],gb.rack[i^1].topChip());
            if(firstMove && (pl.boardIndex==b.whoseTurn))
            {
            if(GC.handleRoundButton(gc,komiRects[i],highlight,
					gb.komiIsSet ?komiMessage() : s.get(SetKomiAction),
					HighlightColor,rackBackGroundColor))
				{
				highlight.hitCode = GoId.HitSetKomiButton;
				}}
           	pl.setRotatedContext(gc, highlight, true);
          }
        commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
        double messageRotation = pl.messageRotation();
        
        GC.setFont(gc,standardBoldFont());
		if (vstate != GoState.Puzzle)
			
        {	boolean doneState = b.DoneState();
 
			if(G.debug())
			{
				if(GC.handleRoundButton(gc, classifyRect, 
	            		select, "classify",
	                    HighlightColor, rackBackGroundColor))
				{
					select.hitCode = GoId.HitClassifyButton;
				}
				if(GC.handleRoundButton(gc, classifyAllRect, 
	            		select, "classify all",
	                    HighlightColor, rackBackGroundColor))
				{
					select.hitCode = GoId.HitClassifyAllButton;
				}
			}
        	if(doneState)
        	{
            if (GC.handleRoundButton(gc, doneRect, 
            		select, s.get(DoneAction),
                    HighlightColor, rackBackGroundColor))
	            {	// always display the done button, but only make it active in
	            	// the appropriate states
            		switch(vstate)
            		{	
            		case Score:
            		case Score2:
            			select.hitCode = GoId.DoneScoring;
            			break;
            		default:
            			select.hitCode = GameId.HitDoneButton;
            		}
	            }
        	}
        	else { 
        		if (!undoPending && !reviewMode() && GC.handleRoundButton(gc, doneRect, 
                		highlight, s.get(UndoAction),
                        HighlightColor, rackBackGroundColor))
    	            {	// always display the done button, but only make it active in
    	            	// the appropriate states
    	                highlight.hitCode = undoPending ? null : GoId.HitUndoButton;
    	            }
        		if(undoPending) 
        		{
        			GC.handleSquareButton(gc, pleaseUndoRect, 
                    		null, s.get(UndoRequest),
                            HighlightColor, new Color(0.9f,0.9f,0.2f));
        			HitPoint hit = G.offline()|reviewOnly|!undoRequest ? highlight : null;
        			if(GC.handleSquareButton(gc,okUndoRect,hit,s.get(YesUndoMessage),HighlightColor,rackBackGroundColor))
        				{ highlight.hitCode = GoId.HitUndoActionButton;
        				}
        			if(GC.handleSquareButton(gc,dontUndoRect,hit,s.get(DenyUndoMessage),HighlightColor,rackBackGroundColor))
        				{ highlight.hitCode = GoId.HitDenyUndoButton;
        				}
        		}
        	}
	        if(firstMove)
			{
				if(GC.handleRoundButton(gc, messageRotation,passRect, 
	            		highlight, s.get(SetHandicapAction),
	                    HighlightColor, rackBackGroundColor))
	                    {
	                    	highlight.hitCode = GoId.HitSetHandicapButton;
	                    }
			}
	        else
	        {
            String passMessage = null;
            CellId passCode = GameId.HitPassButton;
            HitPoint passSelect = (b.DoneState()? null : select);
            showKomi(gc,komiRects[0],gb);
            switch(vstate)
            {
            default: break;
            case Play: 
            case Confirm:
            	passMessage = PASS; break;
            case Play1: 
            	passMessage = StartScoreMessage;
            	break;
            case Score:
            case Score2: 
            	passMessage = ResumePlayMessage;
            	passCode = GoId.ResumePlay;
            	passSelect = select;
            	break;
            			
            }
            if ((passMessage!=null)
            	&& GC.handleRoundButton(gc,messageRotation,passRect,
            		passSelect, s.get(passMessage),
            		HighlightColor, rackBackGroundColor))
            {
            	select.hitCode = passCode;
            }}
            if (allowed_to_edit && !undoPending)
            {
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
            }
        }

 		drawPlayerStuff(gc,(vstate==GoState.Puzzle),ourSelect,HighlightColor,rackBackGroundColor);



            standardGameMessage(gc,messageRotation,
            		vstate==GoState.Gameover
            			?gameOverMessage()
            			:s.get(vstate.getDescription()),
            				vstate!=GoState.Puzzle,
            				gb.whoseTurn,
            				stateRect);
            gb.playerChip[gb.whoseTurn].drawChip(gc,this,iconRect,null);
            if( vstate.isScoring() && (scoringErrata!=null))
            {
            if(GC.handleSquareButton(gc, goalRect, ourSelect,
            		s.get(ScoringFailedMessage),
            		Color.red,boardBackgroundColor))
            		{
            		ourSelect.hitCode = GoId.HitShowScoring;
            		}
            	}
            else
            {
            goalAndProgressMessage(gc,ourSelect,Color.black,s.get(VictoryCondition),progressRect, goalRect);
            }
        DrawRepRect(gc,b.Digest(),pleaseUndoRect);
        drawAuxControls(gc,gb,ourSelect);
        redrawChat(gc,highlight);
        drawVcrGroup(ourSelect, gc);
    }
    
    // undo a "mark as dead" move
    void handleUndead(GoMovespec m)
    {
    	int dead = findDeadMoveIndex(m.to_col,m.to_row);
    	if(dead>0)
    	{	removeHistoryElement(dead);
    	}
    }
    // undo all scoring moves, back to where we started scoring.
    void handleResume()
    {
    	int pass = findPassMoveIndex();
    	if(pass>0)
    	{
    		rewindHistory(pass);
    		truncateHistory();
    	}
    }
    // undo one move
    void handleUndo()
    {  	doScrollTo(BACKWARD_ONE);
    	truncateHistory();
    }
    void handleUndoRequest()
    {
    	
    }
    private boolean undoRequest = false;	// we requested an undo
    private boolean undoPending = false;	// someone requested an undo

    /**
     * Execute a move by the other player, or as a result of local mouse activity,
     * or retrieved from the move history, or replayed form a stored game. 
     * @param mm the parameter is a commonMove so the superclass commonCanvas can
     * request execution of moves in a generic way.
     * @return true if all went well.  Normally G.Error would be called if anything went
     * seriously wrong.
     */
     public boolean Execute(commonMove mm,replayMode replay)
    {  	 boolean hasUndoRequest = undoRequest;
    	 undoRequest = false;
    	 undoPending = false;
    	 switch(mm.op)
    	 {
   	 	case MOVE_UNDEAD:
	 		{
	 			handleUndead((GoMovespec)mm);
	 		}
	 		break;
   	 	case MOVE_RESUMEPLAY:
    	 	{
    		handleResume();
    	 	}
    	 	break;
    	 case MOVE_OKUNDO:
    		 handleUndo();		// do the undo
    		 break;
    	 case MOVE_DENY_UNDO:
     		 break;
    	 case MOVE_UNDOREQUEST:
    		 undoPending = true;
    		 undoRequest = hasUndoRequest;
    		 break;
    	 default:
        	{ 
    		switch(mm.op)
        	{
        	case MOVE_DROP_BLACK:
    			lastDropped = GoChip.black;	// so adjust stones works
    			break;
    		case MOVE_DROP_WHITE:
    			GoCell cell = b.getCell(((GoMovespec)mm).to_col,((GoMovespec)mm).to_row);
    			lastDropped = (cell==null) ? null : GoChip.white.getAltDisplayChip(cell);	// so adjust stones works
    			break;
    		case MOVE_DROPB:
    			lastDropped = b.pickedObject;
    			break;
        	case MOVE_PICK: 
        	case MOVE_PICKB: 
        		break;
    		default:
    			mm.setLineBreak(true);
        	}
 
	        handleExecute(b,mm,replay);
	        // go currently doesn't generate any animations, but if it did
	        // this would be all-at-once movements.
	        startBoardAnimations(replay,b.animationStack,b.cellSize(),MovementStyle.Simultaneous);
	        if(replay!=replayMode.Replay) { playSounds(mm); }
	    	 }
        	break;
    	 }
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
        return (new GoMovespec(st, player));
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

    public commonMove EditHistory(commonMove nmove)
    {	
    	switch(nmove.op)
    	{
    	case MOVE_NEXTPLAYER: return(null);
    	case MOVE_HANDICAP:
    		{
    		GoMovespec m = (GoMovespec)History.top();
    		if(m.op==MOVE_HANDICAP) { popHistoryElement(); }
    		if(((GoMovespec)nmove).to_row==0) { return(null); }
    		}
    		return(nmove);
    	case MOVE_KOMI:
    		{
    		GoMovespec m = (GoMovespec)History.top();
    		if(m.op==MOVE_KOMI) { popHistoryElement(); }
    		return(nmove);
    		}
     	case MOVE_OKUNDO:
    	case MOVE_DENY_UNDO:
    	case MOVE_UNDOREQUEST:
    	case MOVE_RESUMEPLAY:
       	case MOVE_UNDEAD:
   			return(null);
      	case MOVE_DEAD:
       		return(EditHistory(nmove,true));
       	case MOVE_DROP_WHITE:
    	case MOVE_DROP_BLACK:
    	case MOVE_ADD_BLACK:
    	case MOVE_ADD_WHITE:
    		{	// allow pass moves "tt" from standard sgf records to proceed.
    			commonMove h = History.top();
    			if((h!=null) && (h.op==MOVE_PICK)) { popHistoryElement(); }
    			GoMovespec m = (GoMovespec)nmove;
    			return(EditHistory(nmove,(m.to_col=='T')));
    		}
    	default: return(super.EditHistory(nmove));
    	}
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
    
private void playSounds(commonMove m)
{
    // add the sound effects
    switch(m.op)
    {
    case MOVE_RACK_BOARD:
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
        if (hp.hitCode instanceof GoId) // not dragging anything yet, so maybe start
        {
        GoId hitObject = (GoId)hp.hitCode;
		GoCell cell = hitCell(hp);
		GoChip chip = (cell==null) ? null : cell.topChip();
		if(chip!=null)
		{
	    switch(hitObject)
	    {
	    case Black_Chip_Pool:
	    	PerformAndTransmit("Pick B "+cell.row+" "+chip.id.shortName);
	    	break;
	    case White_Chip_Pool:
	    	PerformAndTransmit("Pick W "+cell.row+" "+chip.id.shortName);
	    	break;
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	GoState state = b.getState();
	    	switch(state)
	    	{
	    	case Score:
			case Score2: break;
			default:
	    	if(cell.height()>0)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.id.shortName);
	    		}
	    	}
	    	break;
		default:
			break;
        }
        }}
    }
    // find pass/done/pass that started the scoring sequence
    int findPassMoveIndex()
    {
    	for(int lim=History.size()-2; lim>=0; lim--)
    	{
       		commonMove m = History.elementAt(lim);
     		if(m.op==MOVE_PASS)
    		{ commonMove m1 = History.elementAt(lim+1);
    		  if((m1.op==MOVE_PASS)
    				|| ((m1.op==MOVE_DONE) && (History.elementAt(lim+2).op==MOVE_PASS)))
    			{
    			return(lim);
    			}
    		}
    	}
    	return(-1);
    }
    int findDeadMoveIndex(char col,int row)
    {
    	for(int lim = History.size()-1; lim>=0; lim--)
    	{
    		commonMove m = History.elementAt(lim);
    		GoMovespec mm = (GoMovespec)m;
    		switch(mm.op)
    		{
    		case MOVE_DONE:
    		case MOVE_NEXTPLAYER: break;
    		case MOVE_DEAD:
    			CellStack stack = mm.viewerInfo;
        		for(int limit = stack.size()-1; limit>=0; limit--)
        		{
        			GoCell c = stack.elementAt(limit);
        			if((c.col==col)&&(c.row==row)) { return(lim); }
        		}
        		break;
    		case MOVE_PASS:
    			return(-1);
    		default: break;
    		}    		
    		
    	}
    	return(-1);
    }
    public void selectAnnotation(Annotation ann)
    {
    	if(ann!=null) { PerformAndTransmit("Pick "+"A" + " "+ann); }
    }
    public void compareCS(String oldCS,String newCS)
    {
    	System.out.println(b.compareCS(oldCS,newCS));
    }
    public double parseScore(String score)
    {	if(score==null) { return(999); }
    	StringTokenizer tok = new StringTokenizer(score," +\t:");
    	double best = 0.0;
    	boolean forWhite = false;
    	boolean forBlack = false;
    	boolean scored = false;
    	boolean skipping = true;
    	try {
    	while(tok.hasMoreTokens())
    	{
    		String str = tok.nextToken();
    		if("jigo".equalsIgnoreCase(str)||"jigo.".equalsIgnoreCase(str)) 
    			{ best = 0.0; forBlack=true;  skipping=true;  scored = true;
    			}
    		else if("resigned".equalsIgnoreCase(str)|| "r".equalsIgnoreCase(str)|| "resigns".equalsIgnoreCase(str)) { best += 999; scored = true;}
     		else if("white".equalsIgnoreCase(str) || "w".equalsIgnoreCase(str)) { forWhite=true; skipping=false; }
    		else if("black".equalsIgnoreCase(str) || "b".equalsIgnoreCase(str)) { forBlack=true; skipping=false; }
    		else if("3/4".equalsIgnoreCase(str)) { best += 0.75; scored=true; }
    		else if("1/4".equalsIgnoreCase(str)) { best += 0.25; scored=true; }
     		else if(".5".equalsIgnoreCase(str) || "0.5".equalsIgnoreCase(str) || "half".equalsIgnoreCase(str) || "1/2".equalsIgnoreCase(str)) { best += 0.5; scored=true; }
    		else if((str.charAt(0)=='(') || skipping ) { skipping = true; }
    		else if(Character.isDigit(str.charAt(0))) 
    			{ best = G.DoubleToken(str);
    			  scored=true;
    			}
    		else if("or".equalsIgnoreCase(str)
    				|| "a".equalsIgnoreCase(str)
    				|| "wins".equalsIgnoreCase(str) 
    				|| "won".equalsIgnoreCase(str)
    				|| "by".equalsIgnoreCase(str) ) { }
    		else if( "pts".equalsIgnoreCase(str)
    				|| "pts.".equalsIgnoreCase(str)
    				|| "pt".equalsIgnoreCase(str)
    				|| "pt.".equalsIgnoreCase(str)
    				|| "point".equalsIgnoreCase(str) 
    				|| "points".equalsIgnoreCase(str)
    				|| "point.".equalsIgnoreCase(str)
    				|| "points.".equalsIgnoreCase(str)) { skipping=true; }
    		else { System.out.println("Score "+score+"  token "+str+" not parsed"); }
    	}
    	if(scored && (forBlack || forWhite)) 
    	{
    		return(forWhite ? -best : best); 
    	}
    	} 
    	catch (NumberFormatException oops)
    	{
    		System.out.println("Score not parseable "+score);
    	}
    	return(999);
    }
    String badKomi = "";
    sgf_game gameName = null;
    class classification
    {
    	double difScore;
    	String exceptions;
    	classification(double d,String exc)
    	{
    		difScore = d;
    		exceptions = exc;
    	}
    	boolean isOk()
    	{
    		return(difScore==0.0 && "".equals(exceptions));
    	}
    }
    public classification doClassification()
    {	String oldCS = b.getProp("CSx");
    	if(oldCS==null)
    	{
    		oldCS = b.getProp("CS");
    		if(oldCS!=null) { b.putProp("CSx",oldCS); }
    	}
    	String oldRE = b.getProp("REx");
    	if(oldRE==null)
    	{
    		oldRE = b.getProp("RE");
    		if(oldRE!=null) { b.putProp("REx",oldRE); }
    	}
    	if(oldRE==null)
    		{ G.print("No result property for "+gameName);
    		  badKomi += "No result property for "+gameName+"\n";
    		}
     	String newCS = b.classificationString();
     	String exc = b.classificationExceptions();
     	
     	if(!"".equals(exc)) { badKomi+= ""+gameName+"\n"+exc; G.print(exc);}
    	b.putProp("CS",newCS);
    	if(newCS.equals(oldCS)) { }
    	else if(oldCS!=null)
    	{	//allOk = false;	//without this, consider changes in classification as neutral
    		//G.print("Old:\n"+oldCS+"\nNew:\n"+newCS);
    		compareCS(oldCS,newCS);
    	}
    	double result = b.territoryForPlayer(0)-b.territoryForPlayer(1);
    	String RE = (result>0) 
    					? "Black wins by " + result + " points"
    					: (result<0) ? ("White wins by " + (-result) +" points")
    							: "Jigo";
    	double oldScore = -999;
    	if(oldRE!=null)
    	{
    		oldScore = parseScore(oldRE);
    		if(oldScore<200)
    		{
    			if((oldScore!=(int)oldScore) != (result!=(int)result))
    			{
    				badKomi += ""+gameName+"\n";
    			}
    			b.putProp("OK",((oldScore==result)?"true ":"false ")+RE+" expected "+oldRE);
    			if(oldScore!=result)
    			{	
    				System.out.println("Expected score "+oldRE);
    			}
    		}
    	}
    	System.out.println(RE);
    	return(new classification(result-oldScore,exc));
    }
    sgf_gamestack correct = new sgf_gamestack();
    sgf_gamestack unscored = new sgf_gamestack();
    // hash as object so even doubles that are the same will be considered different
    ScoredGameStack badScores = new ScoredGameStack();
    
    private boolean collectingStats = false;
    private boolean hasKomiProp = false;
    private boolean hasGameNameProp = false;
    private String gameComment = null;
    public void doReplayCollection()
    {
    	collectingStats = true;
    	badKomi="";
    	correct.clear();
     	badScores.clear();
     	unscored.clear();
    	try {
    		super.doReplayCollection();
    	}
    	finally 
    		{ collectingStats = false; 
    		}
    }
    private double parseKomi(String comment)
    {	comment = comment.toLowerCase();
    	int komiIndex = 0;
    	while(komiIndex>=0)
    		{ komiIndex = comment.indexOf("komi",komiIndex);
    		  if(komiIndex>=0)
    		  {
    		  double parsed = parseKomiFrom(comment,komiIndex);
    		  if(parsed>=0) {
    			  System.out.println("Komi parsed as "+parsed);
    			  return(parsed); 
    			  }
    		  komiIndex +=4;
    		  }
    		}
    	return(-1);
    }
    private double parseKomiFrom(String comment,int komiIndex)
    {
		int start = comment.lastIndexOf('\n',komiIndex);
		int end = comment.indexOf('\n',komiIndex);
		{
		int start1 = comment.lastIndexOf("(",komiIndex);
		int end1 = comment.indexOf(')',komiIndex);
		if(start1>start && end1<end) { start = start1 ; end=end1; } 
		}
		if(end<0) { end = comment.length(); }
		if(start>=0 && end>=0)
		{
		String sub = comment.substring(start,end);
		StringTokenizer tok = new StringTokenizer(sub);
		boolean hasValue = false;
		double komi = 0;
		while(tok.hasMoreTokens())
		{	
			String m = tok.nextToken();
			if (".5".equalsIgnoreCase(m) || "0.5".equalsIgnoreCase(m) || "half".equalsIgnoreCase(m) || "1/2".equalsIgnoreCase(m)) { komi += 0.5; hasValue = true; }
			else if ("white".equalsIgnoreCase(m) || "black".equalsIgnoreCase(m))
				{ return(-1); }	// from the report of what the lispm used
			else if(Character.isDigit(m.charAt(0))) 
				{ komi = G.DoubleToken(m);
				  hasValue = true;
				}
			else if ("none.".equalsIgnoreCase(m) || "none".equalsIgnoreCase(m) || "no".equalsIgnoreCase(m)) { komi = 0; hasValue = true; }
			else if ("points".equalsIgnoreCase(m) || "pts".equalsIgnoreCase(m)) { return(komi); }
		}
		if(hasValue) { return(komi); }
		else { System.out.println("Parsekomi failed for "+sub);
		}
		}
		else { System.out.println("Parsekomi failed no start or end"); }
		return(-1);
    }
    boolean COLLECTSCORABLE = false;
    public void ReplayGame(sgf_game ga)
    {	//System.out.println("Game "+ga);
		b.clearProps();
		hasKomiProp = false;
		hasGameNameProp = false;
		gameName=ga;
		gameComment = null;
		
		String gameName = ga.short_name();
		if((gameName==null) && (ga.source_file!=null)) 
			{ ga.set_short_name(ga.source_file); 
			}
		try {
		super.ReplayGame(ga);
		}
		catch (ErrorX err)
		{	String msg = "Problem replaying "+ga+":" + err;
			badKomi += msg + "\n";
			System.out.println(msg);
		}
		if(COLLECTSCORABLE)
		{
			String resultProp = b.getProp("RE");
			double parsed = parseScore(resultProp);
			if((resultProp==null)||(Math.abs(parsed)>200))
			{
				unscored.push(ga);
			}
			else 
			{
				correct.push(ga);
			}
		}
		else
		{
		if(!hasGameNameProp)
		{
			badKomi += "No game name for "+ga+"\n";
		}
		if(!hasKomiProp)
		{
		String comment = gameComment;
		if(comment!=null)
			{
			double parsed = parseKomi(comment);
				if(parsed>=0) 
					{ b.setKomi(parsed);
					  b.putProp("KM",""+parsed);
					}
			}
		}
    	if(collectingStats)
    	{	
    		classification result = doClassification();
    		sgf_game newgame = save_game(History);
    		if(result.isOk()) { correct.push(newgame); }
    		else 
    		{ 
    		  badScores.push(new ScoredGame(result.difScore,newgame));
    		}
     	}}
    }
    void doShowScoring()
    {
    	G.infoBox(ScoringFailedMessage,scoringErrata);
    }
    void doClassifyAll()
    {	
    	doReplayCollection();
    	

    	if(!"".equals(badKomi)) { System.out.println("bad komi : "+badKomi); }
    	System.out.println("Correct "+correct.size()+" Incorrect "+badScores.size());
    	
    	{String ss = sgf_reader.do_sgf_dialog(FileDialog.SAVE,"go", "correct.sgf");
    	  if(ss!=null) { sgf_reader.sgf_save(ss, correct.toArray()); }
     	}
    	
    	if(unscored.size()>0)
       	{String ss = sgf_reader.do_sgf_dialog(FileDialog.SAVE,"go", "unscored.sgf");
       	if(ss!=null) { sgf_reader.sgf_save(ss, unscored.toArray()); }
       	}
  	
    	if(badScores.size()>0)
    	{
    	 sgf_gamestack incorrect = new sgf_gamestack();
    	 // sort the incorrect scores and reorder the
    	 // incorrect games in order of badness
    	 badScores.sort();
       	 incorrect.clear();
       	 while(badScores.size()>0)
       	 {	ScoredGame nextScore = badScores.pop();
       	    sgf_game nextGame = nextScore.game;
       	    incorrect.push(nextGame);
       	 }
       	 String ss = sgf_reader.do_sgf_dialog(FileDialog.SAVE, "go","incorrect.sgf");
    		if(ss!=null) { sgf_reader.sgf_save(ss,incorrect.toArray()); }
    	}
    }
    public boolean allowUndo() { return(false); }
 	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging( HitPoint hp)
    {	CellId id = hp.hitCode;
    	if(!(id instanceof GoId)) 
    		{ // handle all the actions that aren't ours
    			missedOneClick = performStandardActions(hp,missedOneClick); 
    		}
    	else {
    	missedOneClick = false;
        GoId hitObject = (GoId)id;
		GoState state = b.getState();
		GoCell cell = hitCell(hp);
		GoChip chip = (cell==null) ? null : cell.topChip();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
        case HitClassifyButton:
        	doClassification();
        	break;
        case HitClassifyAllButton:
        	doClassifyAll();
        	break;
        case HitShowScoring:
        	doShowScoring();
        	break;
       case HitSetKomiButton:
        	showKomiMenu(G.Left(hp),G.Top(hp));
        	break;
        case HitSetHandicapButton:
        	showHandicapMenu(G.Left(hp),G.Top(hp));
        	break;
        case AnnotationViewButton:
        	Annotation.showMenu(this,G.Left(hp)-getSX(),G.Top(hp)-getSY());
        	break;
        case NumberViewButton:
	    	NumberingMode.showMenu(this,G.Left(hp)-getSX(),G.Top(hp)-getSY());
	    	break;
        case ReverseViewButton:
	       	 { boolean v = !b.reverseY(); b.setReverseY(v); reverseOption.setState(v); }
	       	 generalRefresh();
	       	 break;
        case HitUndoActionButton:
        	if(!OurMove()) { PerformAndTransmit("OkUndo"); }
        	PerformAndTransmit("OkUndo");
        	break;
        case HitUndoButton:
        	undoRequest = true;
        	PerformAndTransmit("UndoRequest");
        	break;
        case HitDenyUndoButton:
        	PerformAndTransmit("DenyUndo");
        	break;
        case DoneScoring:
        	PerformAndTransmit("DoneScoring");
        	break;
        case ResumePlay:
        	PerformAndTransmit("Resume");
        	break;
        case BoardLocation:	// we hit the board 
        	GoChip po = b.pickedObject;
        	if(po!=null && po.isAnnotation())
        	{
			    PerformAndTransmit("Annotate "+cell.col+" "+cell.row); 
        	}
        	else
        	{
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case Score2:
				PerformAndTransmit("ResumeScoring");
				//$FALL-THROUGH$
			case Score:
				if(cell.topChip()==null)
				{
				// find the culprit in the recent dead moves
				int m = findDeadMoveIndex(cell.col,cell.row);
				if(m>0) 
					{ 
					  PerformAndTransmit("Undead "+cell.col+" "+cell.row);
					}
				  PerformAndTransmit("Safe "+cell.col+" "+cell.row);
				}
				else
				{
				PerformAndTransmit("Dead "+cell.col+" "+cell.row);
				}
				break;
			case Confirm:
			case Play:
			case Play1:
			case Puzzle:
			case Gameover:
				{	
				    if(cell.topChip()==null)
				    {
				    	GoChip ch = chip;	
				    	if(ch==null) { ch = po; }
				    	if((ch==null) && (state==GoState.Puzzle)) { ch = b.lastChip; }
					    if(ch==null) { ch = b.rack[b.whoseTurn].topChip(); }
					    String coord = ""+cell.col+(char)('A'+(b.boardRows-cell.row));
					    if(ch==GoChip.black)
					    	{
					    	PerformAndTransmit("B "+coord);
					    	}
					    else if(ch==GoChip.white)
					    	{
					    	PerformAndTransmit("W "+coord);
					    	}
				    }
				    else if(b.pickedObject==null)
				    {
					PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.id.shortName);
					}
					break;
				}
			}}
			break;
			
			
        case White_Chip_Pool:
        case Black_Chip_Pool:
        	{
        	int mov = b.movingObjectIndex();
        	String col =  hitObject.shortName;
            if(mov>=0) 
			{//if we're dragging a black chip around, drop it.
            	switch(state)
            	{
            	default: throw G.Error("can't drop on rack in state %s",state);
                case Play:
                case Play1:
            		PerformAndTransmit(RESET);
            		break;

               	case Puzzle:
            		PerformAndTransmit("Drop "+col+" "+cell.row+" "+mov);
            		break;
            	}
			}
         	}
            break;

        }}
    }

    private void setupBoardRect(GoBoard gb,Rectangle br)
    {
        gb.SetDisplayParameters(0.94,1.0,  0.12,0.1,  0);
        gb.SetDisplayRectangle(br);

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
    public String sgfGameType() { return(Go_SGF); }

    
    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a go init spec
    	long rk = G.LongToken(his);
    	int np = G.IntToken(his);
    	// make the random key part of the standard initialization,
    	// even though games like go probably don't use it.
        b.doInit(token,rk);
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

    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {	if((handicapMenu!=null)&&(handicapMenu.selectMenuTarget(target)))
    		{
    		PerformAndTransmit("Handicap "+handicapMenu.value);
    		return(true);
    		}
    	if((komiMenu!=null)&&(komiMenu.selectMenuTarget(target)))
    	{
    		PerformAndTransmit("KM "+komiMenu.rawValue);
    		return(true);
    	}
    	if(NumberingMode.selectMenu(target)) { return(true); }
    	if(Annotation.selectMenu(target)) { return(true); }
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
    

    public BoardProtocol getBoard()   {    return (b);   }
    public SimpleRobotProtocol newRobotPlayer()
    { throw G.Error("no robots");//(new GoPlay()); 
    }

    public void showHandicapMenu(int x,int y)
    {
    	if(handicapMenu==null) { handicapMenu = new PopupManager(); }
    	handicapMenu.newPopupMenu(this,deferredEvents,this);
    	int items[][] = b.getHandicapValues();
    	handicapMenu.addMenuItem(NoHandicapAction,0);
    	for(int i[] : items) { handicapMenu.addMenuItem(""+i[0],i[0]); }
    	handicapMenu.show(x,y);
    }
    public void showKomiMenu(int x,int y)
    {
    	if(komiMenu==null) { komiMenu = new PopupManager(); }
    	komiMenu.newPopupMenu(this,deferredEvents,this);
    	String items[][] = {{NoKomiAction,"0.0"},{"1/2 point","0.5"},{"4 1/2 points","4.5"},
    			{"5 1/2 points","5.5"},{"6 1/2 points","6.5"},{"7 1/2 points","7.5"}};
   
    	for(String i[] : items) { komiMenu.addMenuItem(i[0],i[1]); }
    	komiMenu.show(x,y);
    }

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
                adjustPlayers(b.nPlayers());
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (name.equals("PW")) { parsePlayerCommand("P1","id " + G.quote(value)); } 	// white player
            else if (name.equals("PB")) { parsePlayerCommand("P0","id " + G.quote(value)); } 	// black player
            else if (name.equals("LB")) { }
            else if (name.equals("TW")) { }
            else if (name.equals("TB")) { }
            else if (name.equals("Mark")) { }
            else if (name.equals("BL")) { }
            else if (name.equals("WL")) { }
            else if (name.equals("CS")// after scoring position
            		|| name.equals("CSx")
            		||	name.equals("AS")) 
            	{ b.putProp(name,value); 
            	}	
            else if (name.equals("KM")||name.equals("GKM")) 
            	{ hasKomiProp = true;
            	  b.putProp(name,value);
            	  PerformAndTransmit("KM "+value,false,replayMode.Replay); 
            	}
            else if (name.equals("SZ")) { b.setBoardSize(G.IntToken(value)); }
            else if (name.equals("FP")) { }	// final position, saved with some ancient go program records
            else if (name.equals("AB")) { PerformAndTransmit("AB "+value,false,replayMode.Replay); }
            else if (name.equals("AW")) { PerformAndTransmit("AW "+value,false,replayMode.Replay); }
            else if (name.equals("B")) 
            {   
            	PerformAndTransmit("B "+value,false,replayMode.Replay); 
            }
            else if (name.equals("W")) 
            {  
            	PerformAndTransmit("W "+value,false,replayMode.Replay); 
            }
            else if (name.equals(game_property) && value.equals("1"))
            {
            	// the equals sgf_game_type case is handled in replayStandardProps
            }
            else if (parseVersionCommand(name,value,2)) {}
            else if (parsePlayerCommand(name,value)) {}
            else 
            {	b.putProp(name,value);
            	if(gamename_property.equals(name)) { hasGameNameProp = true; }
                replayStandardProps(name,value);
            }

            prop = prop.next;
        }

        if (!"".equals(comments))
        {	if(gameComment==null) { gameComment=comments; }
            setComment(comments);
        }
    }
}