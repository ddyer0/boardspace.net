package plateau.common;

import java.awt.*;
import javax.swing.JCheckBoxMenuItem;


import lib.CellId;
import lib.ChatInterface;
import lib.DefaultId;

import java.util.*;

import lib.Graphics;
import lib.Image;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import online.game.*;
import online.game.sgf.*;
import online.search.SimpleRobotProtocol;


/*
Change History

Feb 29 2004  Iniital work in progress, support for plateau
Dec 1, 2004 Essentally complete and function version, except
 no robot, and communication not secured.

*/
/* communications plan

The initial version simply transmits all the actions
in a slightly obscure direct translation of the board.
The primary gestures are "flip" "pick" and "drop". Anyone
watching the communications stream would rapidly deduce
the identities of all the pieces.  A fully secure communiction
would have the following characteristics.

1) no hidden information is transmitted until it is supposed to be revealed.
2) once revealed, the recepient can verify that the revealed information was
predetermined; Ie; the opponent didn't decide after you captured it that
what you captured is a blue instead of an ace.
3) disconnections can be recovered, whichever partner is disconnected,
as long as some survivor knows the public state of the game.
4) allowing the robot to take over for abandoned games is possible.
5) spectators get a non-priveleged view of the game.

The plan:  The key point to hide is the identity of pieces being
onboarded.  The "actual move" transmitted will be the same as now,
but in place of the actual move, we will transmit a MD5-type hash
of the actual move, plus a "public synopsis" which contains the public
part of the move (ie; stack of 1 with red on top).  At the point the
actual move is to be revealed, it will be retransmitted in the clear,
and the recepient will verify that it hashes to the previously transmitted
hash.  That's the simple version, but complications ensue if your assume
(as you must) that a dishonest opponent will try to decode the hashes
in advance.  This could be done, for example, but generating all possible
moves, hashing all of them, and seeing which one matches the given hash.
To avoid this kind of attack, each move is filled out by a unique filler
string, such that the sequence of fillers is known only to the player.
So, the player has a "master key" which is specific to the player and
this partular game.  The master key generates a sequence of filler strings,
which are matched with the sequence of actual moves.  This takes care
of 1 and 2, such that both players can be reasonable sure that no one
known anything he isn't supposed to, and no one is taking liberties
with the concealed information.

To handle disconnections, the player will escrow his master key with
the server, and the server will pass it back only to the player who
is taking over the slot.  A reconnected player will retrieve the public
record of the game and his master key, and use the "all possible moves"
strategy previously noted to regenerate his moves for the game.

Similarly, to allow robot takeovers; once the other player is disconnected
the server will give up his escrowed master key to the opponent, which
allows the robot to take over and reconstruct as above.

*/
public class PlateauGameViewer extends commonCanvas implements PlateauConstants
{
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	static final Color reviewModeBackground = new Color(1.0f, 0.90f, 0.90f);
    static final Color ButtonColor = new Color(0.5f, 0.5f, 1.0f);
    static final Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    static final Color boardBackgroundColor = new Color(153, 217, 228);

    // vcr stuff
    static final Color vcrButtonColor = new Color(0.7f, 0.7f, 0.75f);
    JCheckBoxMenuItem showBoth = null;
    private boolean showBothRacks = false;
    private boolean anonymized = false;
    PlateauBoard b = null; //the board from which we are displaying
    public int CELLSIZE; //size of the layout cell
    //private Rectangle fullRect = addRect("fullRect"); //the whole canvas
    //private Rectangle boardRect = addRect("boardRect"); // just the board
    //public Rectangle stateRect = addRect("stateRect");
    //public Rectangle noChatRect = addRect("nochat");
    private Rectangle movingStackRect = addRect(".movingStackRect"); // moving stack during replay
    private Rectangle blackBarRect = addRect("blackBarRect");
    private Rectangle whiteBarRect = addRect("whiteBarRect");
    private Rectangle blackTradeRect = addRect("blackTradeRect");
    private Rectangle whiteTradeRect = addRect("whiteTradeRect");
    private Rectangle blackExchangeRect = addRect("blackExchangeRect");
    private Rectangle whiteExchangeRect = addRect("whiteExchangeRect");
    private Rectangle blackRackRect = addRect("blackRackRect");
    private Rectangle whiteRackRect = addRect("whiteRackRect");
    private Rectangle CensoredRect = addRect("CensoredRect");

 
    private Rectangle repRect = addRect("repRect");
    
    public BoardProtocol getBoard()
    {
        return (b);
    }
    public static boolean imagesLoaded = false;
    public static Image Icon = null;
    public synchronized void preloadImages()
    {	
	    if(!imagesLoaded)
	        	{ Image mask_images[] = loader.load_images(ImageDir, MaskFileNames);
	    
	   	        if (piece.top_images == null)
	   	        {
	   	            piece.top_images = loader.load_images(ImageDir, "top-", PLAYERCOLORS,
	   	                    ImageFileNames, mask_images[TOP_MASK_INDEX]);
	   	        }
	   	
	   	        if (piece.single_images == null)
	   	        {
	   	            piece.single_images = loader.load_images(ImageDir, "", PLAYERCOLORS,
	   	                    MuteFileNames, mask_images[BOTTOM_MASK_INDEX]);
	   	        }
	   	
	   	        if (piece.stack_images == null)
	   	        {
	   	            piece.stack_images = loader.load_images(ImageDir, "stack-", PLAYERCOLORS,
	   	                    PLAYERCOLORS, mask_images[MIDDLE_MASK_INDEX]);
	   	        }
	   	        Icon = loader.load_images(ImageDir,new String[] {"plateau-icon-nomask"})[0];
	   	        imagesLoaded = true;
	        	}
	    gameIcon = Icon;
	    
    }
    
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {
        super.init(info,frame);
         // piece.bottom_images=load_images("inv-");    //preload the images for the pieces
        if (extraactions)
        {
            showBoth = myFrame.addOption("Show Both Racks", false,deferredEvents);
        }

        b = new PlateauBoard(info.getString(GAMETYPE, "Plateau"));
        // believed to be difficult 5/2022
        //useDirectDrawing(); // not tested yet
        doInit(false);
    }

    /* used when loading a new game */
    public void doInit(boolean preserve_history)
    { //initHistory();
        super.doInit(preserve_history);
        b.doInit(b.gametype);
        if (anonymized)
        {
            b.anonymize(getActivePlayer().boardIndex);
        }
        if(!preserve_history)
        	{
        	startFirstPlayer(); 
        	}
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
    public int setLocalBoundsSize(int width,int height,boolean wideMode,boolean tallMode)
    {	
        int chatHeight = selectChatHeight(height);
        boolean noChat = chatHeight==0;
    	double lncols = (b.ncols * 8) +(wideMode ? 67 : (tallMode ? 34 : 37)); // 
        double lnrows = (b.nrows * 8) +17 + (tallMode ? 17 : wideMode ? -1 : noChat ? 10 : 0);
        double cellh = (height-(wideMode ? 0 : chatHeight)) / lnrows;
        double cellw = width / lncols;
        CELLSIZE = (int)(4*Math.min(cellw, cellh)); //cell size
        return(CELLSIZE);
    }

    public void setLocalBoundsWT(int x, int y, int width, int height,boolean wideMode,boolean tallMode)
    {   
        int chatHeight = selectChatHeight(height);
        boolean noChat = chatHeight==0;
        G.SetRect(fullRect,x,y,width, height);
        int C2 = CELLSIZE/2;
        int C4 = CELLSIZE/4;
        int minChatHeight = CELLSIZE*3;
        int boardX = C2;
        int boardY = CELLSIZE + (wideMode ? 0 : chatHeight);
        int boardW = CELLSIZE * b.ncols * 2;
        int boardH = CELLSIZE * b.nrows * 2;
        int boardRight = boardX+boardW;
        int boardBottom = boardY+boardH;
        G.SetRect(boardRect, 
        		boardX, 
        		boardY,
        		boardW,boardH);
        b.lineStrokeWidth = boardW/400.0;
        int boardB = boardBottom + C2;
        int auxY = tallMode ? boardY + CELLSIZE*5 : boardB;
        int auxX = tallMode ? boardRight+C2 : boardX;
        int doneW = CELLSIZE*2;
        int vcrW = CELLSIZE*4;
        G.SetRect(doneRect, 
        		tallMode ? auxX : auxX+vcrW+C2,
        		auxY,
        		doneW, 3*CELLSIZE/4 );

        
        SetupVcrRects(
        		tallMode ? auxX+doneW+C2 : G.Left(boardRect), auxY, vcrW, vcrW/2);

        G.SetRect(movingStackRect, boardRight-CELLSIZE, boardBottom-CELLSIZE, CELLSIZE * 2, CELLSIZE * 2);

        G.SetRect(progressRect, 2*CELLSIZE,boardB-C2, 6 * CELLSIZE, C2);
        
        G.SetRect(repRect, 3*CELLSIZE, boardBottom-C2,CELLSIZE*3,C2);
 
 
        // "edit" rectangle
        G.AlignLeft(editRect,G.Bottom( doneRect) + C2,doneRect);


        {
            commonPlayer pl0 =getPlayerOrTemp(0);
            commonPlayer pl1 = getPlayerOrTemp(1);
            Rectangle p0time = pl0.timeRect;
            Rectangle p1time = pl1.timeRect;
            Rectangle p0anim = pl0.animRect;
            Rectangle p1anim = pl1.animRect;
            Rectangle startBlackRect = pl0.nameRect;
            Rectangle startWhiteRect = pl1.nameRect;
            Rectangle blackPicRect = pl0.picRect;
            Rectangle whitePicRect = pl1.picRect;  
            
            int bbH = CELLSIZE;
            int playerX = tallMode ? boardX : boardRight+C2;
            int playerY = tallMode ? boardBottom+CELLSIZE : boardY-C2;
            // start player1 rectangle
            G.SetRect(startBlackRect,playerX,playerY,CELLSIZE*5/2,bbH / 2);
            
 
            
            // time display for player1
            G.SetRect(p0time, G.Right(startBlackRect),G.Top( startBlackRect), CELLSIZE * 2,G.Height( startBlackRect));

            G.SetRect(p0anim, G.Right(p0time),G.Top( p0time),G.Height( p0time),G.Height( p0time));
 
            // player portrait
            G.SetRect(blackPicRect, G.Right(p0anim)+CELLSIZE/4,G.Top(startBlackRect), CELLSIZE * 3,CELLSIZE*3);
 
            //player1 stacks
            int rackx = playerX;

            G.SetRect(blackBarRect, rackx,G.Bottom(startBlackRect) + (CELLSIZE / 3), CELLSIZE * 5, bbH);          

            //player1 stacks
            G.SetRect(blackTradeRect, rackx,G.Bottom( blackBarRect) + (C4),G.Width(blackBarRect), CELLSIZE);

            //player1 exchange area text
            int beT = G.Bottom( blackBarRect);
            G.SetRect(blackExchangeRect, rackx,beT,
            		G.Width( blackTradeRect),G.Top(blackTradeRect) - beT);

            int rackw = Math.max(CELLSIZE * 5, //as wide as the other bars
                    Math.min(G.Right(blackPicRect)-rackx,
                        (width - rackx - C2)));

            int rackx2 = G.Right(blackPicRect)+C2;
            int rackw2 = width-rackx2-C2;
            G.SetRect(blackRackRect, 
            		tallMode ? rackx2 : rackx,
            		tallMode ? G.Top(blackBarRect)+C4 : G.Bottom(blackTradeRect) + (C4), 
            		tallMode ? rackw2 : rackw, (CELLSIZE * 2));
          
            G.AlignLeft(whiteRackRect,G.Bottom(blackRackRect) + (C4),blackRackRect);
        
            //player2 trade
            G.SetRect(whiteTradeRect,
            		rackx,
            		(tallMode ? G.Bottom(blackTradeRect) : G.Bottom( whiteRackRect)) + (C4),
            		CELLSIZE * 5,CELLSIZE);

            //player2 captives
            G.SetRect(whiteBarRect, rackx,G.Bottom( whiteTradeRect) +C4,CELLSIZE * 5, CELLSIZE);

            int weY = G.Bottom(whiteTradeRect);
            G.SetRect(whiteExchangeRect, rackx,weY,G.Width( whiteTradeRect),G.Top(whiteBarRect) - weY);
            // start player2 rectangle
            G.SetRect(startWhiteRect,G.Left(startBlackRect),G.Bottom(whiteBarRect) + C4,
            		G.Width(startBlackRect),G.Height(startBlackRect));


            // time display for player2
            G.SetRect(p1time,G.Left( p0time),G.Top(startWhiteRect),CELLSIZE * 2,G.Height( startWhiteRect));

            G.SetRect(p1anim, G.Right(p1time),G.Top( p1time),G.Height( p1time),G.Height( p1time));
            int cenY = G.Bottom(blackTradeRect);
            G.SetRect(CensoredRect, boardRight,cenY, rackw + ((rackx - boardRight) * 2),G.Top( whiteTradeRect) - cenY);

            G.SetRect(whitePicRect,G.Left( blackPicRect),G.Top( whiteTradeRect),G.Width( blackPicRect),G.Height( blackPicRect));
     
            boolean logBottom = ((tallMode&!noChat)|(!wideMode&noChat))&((height-G.Bottom(whitePicRect))>minChatHeight);
            int chatX = wideMode ? G.Right(blackPicRect)+C2 : x+C2;
            int chatW = wideMode|logBottom ? width-chatX-C2
            		: width-CELLSIZE*6 ;
            int logX = wideMode|logBottom ? chatX : noChat ? auxX : chatX+chatW+C4;
            int logY = logBottom ?  G.Bottom(whitePicRect)+C2 : y;
            int logH = logBottom 
            			? height-G.Bottom(whitePicRect)-CELLSIZE 
            			:noChat ? tallMode ? auxY-logY-C2 : minChatHeight : chatHeight;
            int chatY = wideMode ? y+logH+C2 : y;
            int chatH = wideMode&!noChat ? height-chatY-C2 : chatHeight;
            int logW = Math.min(width/2,width-logX-C2);
            G.SetRect(logRect, logX,logY,logW, logH);
            G.SetRect(chatRect, chatX,chatY,chatW ,chatH);

        }

 
        // message rectangle
        int stateX = C2;
        int stateY = boardY-3*CELLSIZE/4;
        int stateH = C2;
        G.placeRow(stateX,stateY,boardW, stateH,stateRect,noChatRect);

        positionTheChat(chatRect,Color.white,Color.white);
 
        generalRefresh();
    }

    // pointing precision of 1/10 of a cell
    public String encodeScreenZone(int x, int y,Point p)
    {
        if (CensoredRect.contains(x, y)&&!mutable_game_record)
        {	G.SetLeft(p, -1);
        	G.SetTop(p,-1);
        	return("off");
        }
        return (super.encodeScreenZone(x,y,p));
    }

    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
        int stackcol = obj / 10000;
        int height = (obj % 10000) / 1000;
        int color = obj % 1000;
        pstack ps = new pstack(b, UNKNOWN_ORIGIN);
        int ystep = G.Height(boardRect) / ((b.nrows * 2) + 2); // 1/2 y square size
        int xstep = G.Width(boardRect) / ((b.ncols * 2) + 2); // 1/2 x square size

        while (height-- > 0)
        {
            new piece(stackcol, color).addToStack(ps);
        }

        ps.Draw(g, xp-xstep, yp-2*ystep, xstep*2, ystep*2, 4, null);
	
    }
    public Point spriteDisplayPoint()
    {   return(new Point(G.Left(movingStackRect),G.Top(movingStackRect)));
    }


    public int concealedmode(int idx)
    {
        return ((showBothRacks 
        		|| mutable_game_record 
         		|| (!getActivePlayer().spectator && (getActivePlayer().boardIndex == idx)))
        		? idx 
        		: (-1));
    }

    //
    // draw the board and balls on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {
    	GC.fillRect(gc, reviewMode() ? reviewModeBackground
                    : boardBackgroundColor,fullRect);
        
        PlateauBoard bd = (PlateauBoard) disB(gc);
        PlateauState vstate = bd.getState();
        boolean moving = hasMovingObject(selectPos);
        if(selectPos!=null) 
        { // this is not standard practice, but lower levels want the dragging flag to be set
          // if anything is moving
        	if(!reviewMode()) { selectPos.dragging |= moving; } 
        }
        HitPoint ourTurnSelect = OurMove()? selectPos : null;
        HitPoint buttonSelect = moving ? null : ourTurnSelect;
        HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;
        
  
        bd.DrawBoard(gc, boardRect, ourTurnSelect, use_grid);

        bd.DrawTrade(gc, blackTradeRect, FIRST_PLAYER_INDEX, ourTurnSelect,
            s.get(PlacePrisonersMessage));
        bd.DrawTrade(gc, whiteTradeRect, SECOND_PLAYER_INDEX, ourTurnSelect,
            s.get(PlacePrisonersMessage));

        bd.DrawBar(gc, blackBarRect, FIRST_PLAYER_INDEX, ourTurnSelect,
            s.get(BlackPoolMessage));
        bd.DrawBar(gc, whiteBarRect, SECOND_PLAYER_INDEX, ourTurnSelect,
            s.get(WhitePoolMessage));
        bd.DrawRack(gc, blackRackRect, concealedmode(FIRST_PLAYER_INDEX),
        		ourTurnSelect);
        bd.DrawRack(gc, whiteRackRect, concealedmode(SECOND_PLAYER_INDEX),
        		ourTurnSelect);
        DrawRepRect(gc,b.Digest(),repRect);

        // if there are points on the bar
        if (gc != null)
        {
            bd.DrawExchangeSummary(gc, FIRST_PLAYER_INDEX, blackExchangeRect);
            bd.DrawExchangeSummary(gc, SECOND_PLAYER_INDEX, whiteExchangeRect);
        }
        GC.setFont(gc,standardBoldFont());
        
        drawPlayerStuff(gc,(vstate==PlateauState.PUZZLE_STATE),nonDragSelect,
 	   			HighlightColor, boardBackgroundColor);

        // draw the board control buttons 
        if (vstate != PlateauState.PUZZLE_STATE)
        {
			handleDoneButton(gc,doneRect,(bd.DoneState() ? buttonSelect : null), 
					HighlightColor, boardBackgroundColor);
			
			handleEditButton(gc,editRect,buttonSelect,selectPos, HighlightColor, boardBackgroundColor);
        }
		commonPlayer pl = getPlayerOrTemp(b.whoseTurn);
		double messageRotation = pl.messageRotation();

        standardGameMessage(gc,messageRotation,
        		vstate==PlateauState.GAMEOVER_STATE?gameOverMessage():s.get(vstate.getDescription()),
        				vstate!=PlateauState.PUZZLE_STATE,
        				bd.whoseTurn,
        				stateRect);
        goalAndProgressMessage(gc,nonDragSelect,
				s.get(GoalMessage),progressRect, progressRect);

        redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
        
        drawVcrGroup(nonDragSelect, gc);

    }

    public boolean Execute(commonMove m,replayMode replay)
    {	//if(my.spectator) { System.out.println("E "+m); }
        handleExecute(b,m,replay);
        if(replay!=replayMode.Replay) { playSounce((plateaumove)m); }
        generalRefresh();
       return (true);
    }
    void playSounce(plateaumove m)
    {
    	switch(m.op)
    	{
    	case MOVE_ONBOARD:
    	case MOVE_PICK:
    	case MOVE_DROP:
    		// TODO: plateau we have to be careful to not make sounds
    		// that give clues about the opponents' activities
    		//playASoundClip(light_drop,100);
    		break;
    	default: break;
    	}
    }
    // parse a move that arrives over the comm interface
    public commonMove ParseNewMove(String st, int player)
    {
        commonMove cm = new plateaumove(st,player);

        return (cm);
    }
    
    // this is the "old" version based on explicit move tests.
    public commonMove EditHistory(commonMove mm)
    { // edit history so redundant moves are eliminated
    	plateaumove currentMove = (plateaumove)mm;
    	switch(currentMove.op)
    	{
    	case MOVE_RESET:
    	case MOVE_ALLOWUNDO:
    	case MOVE_UNDO:
    	case MOVE_DONTUNDO: return(null);
    	default: break;
    	}
        commonMove retm = mm;
        int size = History.size() - 1;
        int idx = size;
        PlateauState state = b.getState();
        if (idx >= 0)
        {
            while (idx >= 0)
            {	int start_idx = idx;
                plateaumove lastmove = (plateaumove) History.elementAt(idx);
                if(lastmove.next!=null) { idx = -1; }
                else {
                if (lastmove.player != currentMove.player)
                {
                    idx = -1;
                }
                else
                {
                    switch (currentMove.op)
                    {
                    case MOVE_RESIGN:
                    	switch(lastmove.op)
                    	{ 
                    	case MOVE_RESIGN:
                    		retm = null;
                    		popHistoryElement();
							//$FALL-THROUGH$
						default:
                    		idx = -1;
                    		break;
                     	}
                    	break;
                   
                    case MOVE_FLIP:
                        // move we're considering is a flip
                        if ((lastmove.op == MOVE_FLIP) 
                        	&& (currentMove.pick == lastmove.pick))	// two flips of the same stack
                        {   //System.out.println("Remove "+lastmove+" and " +mm);
                        
                            popHistoryElement();
                            retm = null;
                            idx = -1;
                        }
                        else { idx--; }
                        break;
                     case MOVE_DROP:
                    	// move we're considering is a drop
                        if (currentMove.drop() == -1)
                        {	// drop off board, which is a reset
                            switch (lastmove.op)
                            {
                            case MOVE_ONBOARD:
                            case MOVE_DROP:
                            if(state==PlateauState.PUZZLE_STATE) { idx=-1; break; }
 								//$FALL-THROUGH$
							case MOVE_PICK:
                            {
                                pstack ps = b.GetStack(lastmove.drop());

                                if ((ps.origin == BOARD_ORIGIN) ||
                                        (retm != null))
                                { // first pass, or continued operation on the board

                                    //System.out.println("Remove "+lastmove);
                                    popHistoryElement(); // get the stack it was actually picked from
                                    idx--;
                                }

                                if (ps.origin != BOARD_ORIGIN)
                                { //punt out if not undoing a board move.  Only board moves are chained.
                                  //System.out.println("Break");

                                    idx=-1;
                                }

                                retm = null; // and dispose this piece at the end

                                // continue removing a sequence of picks and drops
                            }

                            break;

                            default: // other types of moves stop the chain. 
                                     // at the moment this means a "done" indicating
                                     // for exmaple a switch from shuffling the rack
                                     // to moving on the board

                                //if(retm==null) { System.out.println("Break2"); }
                                idx = -1;
                            }

                        }
                        else if(lastmove.op==MOVE_PICK)
                        { // pick followed by drop on the same spot is a noop, except if 
                          // it's a "stomp capture".  The board knows.
                         if( (currentMove.drop()==lastmove.drop())
                        	  && (currentMove.level>=99)		// and drop on top
                              && ((currentMove.state_after_execute==PlateauState.PLAY_STATE)
                            	  || (currentMove.state_after_execute==PlateauState.FLIPPED_STATE)
                            	  || (currentMove.state_after_execute==PlateauState.ONBOARD2_STATE)
                            	  //|| (pm.state_after_execute==PlateauState.PLAY_UNDONE_STATE)
                            	  || (currentMove.state_after_execute==PlateauState.CAPTIVE_SHUFFLE_STATE)
                            	  || ((currentMove.state_after_execute==PlateauState.PLAY_CAPTURE_STATE)
                            			  &&(lastmove.undostate==PlateauState.PLAY_CAPTURE_STATE))
                            	  || (currentMove.state_after_execute== PlateauState.PLAY_DONE_STATE)))
                        	{	
                        	 	popHistoryElement();
                        		retm=null;
                        		idx=-1;
                        	}
                         if(idx>1)
                         {	
                         plateaumove m2 = (plateaumove)History.elementAt(idx-1);
                         plateaumove m3 = (plateaumove)History.elementAt(idx-2);
                         pstack ps = b.GetStack(lastmove.drop());
                         if((m3.op==MOVE_PICK)
                         		&& (m2.op==MOVE_DROP)
                         		&& (lastmove.pick==m3.pick)
                         		&& (ps.origin==BOARD_ORIGIN)
                         		)
                         	{
                        	if( m3.drop()==currentMove.drop())
                        		{
                        		// this corresponds to the normal "stutter" of pick/drop/pick/drop back
                        		retm = null;
                        		popHistoryElement();
                        		if(m2.drop()==m3.drop()) {}	// pick/drop on the same cell, a capture attempt?
                        		else {
                        		popHistoryElement();	// pick/drop to different cells
                        		popHistoryElement();
                        		}
                        		}
                        	else if(currentMove.level==100)
                        	{	popHistoryElement();
                        		popHistoryElement();
                        	}
                         	idx = -1;
                         	}
                         		
                         }
                        	
                		  idx=-1;
                        }
                        else	// other than pick, we're done
                        {
                		idx=-1;
                        }
                        break;

                    case MOVE_PICK:
                    	// current move is a pick
                    	if(state!=PlateauState.PUZZLE_STATE)
                    	{
                        switch (lastmove.op)
                        {
                        case MOVE_ONBOARD:
                            popHistoryElement();
                            retm = null;
                            break;
                        case MOVE_DROP:
                        int drop = lastmove.drop();
                        if(drop>=0)
                        {
                        pstack ps = b.GetStack(drop); // movingStack
                        {
                       	int lc = currentMove.level;
                       	plateaumove m2 = (plateaumove)History.elementAt(idx-1);
                       	if((lc==ps.size()) && ((lc>0)||(m2.pick==currentMove.pick)))
                       			{ lc = 100; }
                        // beware of sequences where complicated indecision occur during trading.
                        if((ps.origin==BOARD_ORIGIN) 
                        		&& (lastmove.drop()==currentMove.drop()) 
                        		&& ((lc==lastmove.level)||(currentMove.level==lastmove.level))
                        		&& (currentMove.pick==lastmove.pick))
                         {	 // dropped then picked the same piece up again
                        	 popHistoryElement();
                        	 retm=null;
                         }}}
                         break;
                         default: ;
                         }}
                        idx = -1;
                        break;

                    case MOVE_ONBOARD:default:
                        idx = -1; // don't look further
                    }
                }
                }
                G.Assert(idx!=start_idx,"edit history progress");
            }
        }
        return (retm);
    }
  //  public commonMove EditHistory(commonMove mm)
  //  { // edit history so redundant moves are eliminated
  //  	commonMove rval = mm;
  //  	if((mm.op==MOVE_FLIP)&&(History.size()>0))
  //  	{	int sz = History.size();
  //  		commonMove prev = History.elementAt(sz-1);
  //  		if(prev.digest==b.Digest())
  //  		{	// special case for plateau - two sided pieces can be flipped but sometimes
  //  			// the other side is the same.
  //  			if(prev.op==MOVE_FLIP) { rval = null; }
  //  			return(rval);
  //  		}
  //  	}
  //  	return(super.EditHistory(rval));
  //  }

    public void StopMoving()
    { 	//HitPoint hp = highlightPoint;
    	//nt hitObject = (hp == null) ? HitNoWhere : hp.hitCode;
    
        if (hasMovingObject(null))
        {
 
            if ((b.movingStack() != null) &&
                    (allowed_to_edit || (whoseTurn() == getActivePlayer())))
            {
                PerformAndTransmit("Drop -1 0");
            }
        }
    }

    public void StartDragging(HitPoint hp)
    {

        CellId hitObject = hp.hitCode;

        if ((hp.dragging == false) &&
                (hitObject == PlateauId.HitAChip))
        {
            piece which = (piece) hp.hitObject;

            if (!which.hittop)
            {
                StopMoving();
                PerformAndTransmit("Pick " + which.piecenumber + " " +
                    which.locus());
                hp.dragging = true;
            }
        }
    }
    boolean nostomp = false;
    public void StopDragging(HitPoint hp)
    {
        
        CellId id = hp.hitCode;
        if(id==DefaultId.HitNoWhere) { leaveLockedReviewMode(); }

        else if(!(id instanceof PlateauId)) {  missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    		missedOneClick = false;
    		PlateauId hitObject = (PlateauId)id;
    		pstack st = b.movingStack();
         if (st!=null)
         {//moving something

            switch (hitObject)
            {
            default: throw G.Error("Hit Unknown: %s", hitObject);
            case HitEmptyRack:
            {
                pstack which = (pstack) hp.hitObject;
                PlateauState bstate = b.getState();
                nostomp = false;
                if ((which.origin == BOARD_ORIGIN) &&
                        ((bstate == PlateauState.ONBOARD_DROP_STATE) ||
                        (bstate == PlateauState.ONBOARD2_DROP_STATE)))
                {
                    PerformAndTransmit("Onboard " + which.locus() +
                        " 100 " + st.allColors() + " " + st.pieces());
                }
                else
                {	PerformAndTransmit("Drop " + " " + which.stacknumber +
                        " 100" + " " + which.locus());
                }
            }

            break;

            case HitAChip:
            {
                piece which = (piece) hp.hitObject;
                pstack stack = which.mystack;

                if ((st.origin == RACK_ORIGIN) &&
                        (stack.origin == BOARD_ORIGIN))
                {	nostomp = false;
                    PerformAndTransmit("Onboard " + stack.locus() + " " +
                        (which.hittop ? 100 : which.height()) + " " +
                        st.topColorString() + " " + st.pieces());
                }
                else
                {
                    PerformAndTransmit("Drop " + stack.stacknumber + " " +
                        (which.hittop ? nostomp ? 99 : 100 : which.height()) + " " +
                        stack.locus());
                    nostomp = !nostomp;
                }
                }

                break;
                }
 
            StopMoving();
        }
        else 
        {
        switch(hitObject)
        {
        default: throw G.Error("Hit Unknown: %s", hitObject);
        case HitAChip:
	        {
            piece which = (piece) hp.hitObject;

            if ((which.hittop) && (which.depth() == 0))
            {
                PerformAndTransmit("Flip " + which.piecenumber + " " +
                    which.mystack.locus() + " " + which.bottomColorString());
            }
	        }
	        break;
         }
        }}
        generalRefresh();
    }

    public String gameType() { return(b.gametype); }
    public String sgfGameType() { return(Plateau_SGF); }
    

    public void performHistoryInitialization(StringTokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
        b.doInit(token);
    }


    public void doShowSgf()
    {
        if (mutable_game_record || G.debug())
        {
            super.doShowSgf();
        }
        else
        {
            theChat.postMessage(ChatInterface.GAMECHANNEL, KEYWORD_CHAT,
                s.get(CensoredGameRecordString));
        }
    }


    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
        if (target == showBoth)
        {
            handled = true;
            showBothRacks = showBoth.getState();
        }

        return (handled);
    }


    public String gameProgressString()
    {
        return ((mutable_game_record 
        		? s.get(Reviewing) 
        		: "" + History.viewStep + " " + b.pieceSummary(FIRST_PLAYER_INDEX) +
        		  " " + b.pieceSummary(SECOND_PLAYER_INDEX)));
    }

    public boolean GameOver()
    {		
        boolean isover = super.GameOver();
        if(isover && anonymized)
        {
       		anonymized=false;
            b.revealAll();
        }

        return (isover);
    }

    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() { return(null); }

    commonPlayer prevPlayer = null;
    public boolean parsePlayerInfo(commonPlayer p,String first,StringTokenizer tokens)
    {	// games without "done" end without gameover if they stack six
       	if("time".equals(first) && b.DoneState())
		{
		PerformAndTransmit("Done",false,replayMode.Replay);
		}

    	return super.parsePlayerInfo(p, first, tokens);
    }
    public boolean parsePlayerExecute(commonPlayer p,String first,StringTokenizer tokens)
    {	if(needStart)
		{ needStart = false; 
		PerformAndTransmit("Start P"+p.boardIndex,false,replayMode.Replay);
		}
    	String msg = first + " "+ G.restof(tokens);

        if((prevPlayer!=null) && (prevPlayer!=p)) 
             { PerformAndTransmit("Done",false,replayMode.Replay);
             } 
        prevPlayer = p;
        int who = b.whoseTurn;
        boolean val = PerformAndTransmit(msg, false,replayMode.Replay);
        if(b.whoseTurn!=who) { prevPlayer=null; }
        return(val);
    }
    boolean needStart = false;

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
            {	needStart = true;
                b.doInit(value);
                prevPlayer = null;
                //b.setFirstPlayer(0);
                b.setWhoseTurn(0);
            }

            if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (name.equals(game_property) && value.equalsIgnoreCase("plateau"))
            {	// the equals sgf_game_type case is handled in replayStandardProps
            }
            else if (parseVersionCommand(name,value,2)) {}
            else if (parsePlayerCommand(name,value)) 
            	{
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