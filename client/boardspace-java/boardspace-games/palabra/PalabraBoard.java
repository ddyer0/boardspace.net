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
package palabra;

import lib.AR;
import lib.G;
import lib.Random;
import online.game.BoardProtocol;
import online.game.CommonMoveStack;
import online.game.chip;
import online.game.commonMove;
import online.game.replayMode;
import online.game.squareBoard;

/**
 * RajBoard knows all about the game of Raj
 * 
 * Unusual features of this board
 * 
 * each player has his own pick/drop information, so simultaneous moves do not
 * interfere with each other.   In live play there are no "done" moves, the game moves on when all players have played
 * their cards.  In robot lookahead, we run the moves synchronously.
 * 
 * sometimes it's necessary to know which player is "my" player.  This is very unusual so use this knowledge very carefully. 
 * 
 * the state engine is a little odd too.  In "play" state it is really "all play", all players
 * are free to pick and drop cards.   When a player perceives all other players as having dropped
 * their cards, he enters "confirm_card_state", which is synchronous.  So if it's your turn, and
 * in confirm_card_state, the viewer will generate a commit move.
 * 
 * 2DO: record the deck shuffle in the game records
 * this will disentangle game records from the live random number generator

 * @author ddyer
 *
 */

class PalabraBoard extends squareBoard<PalabraCell> implements BoardProtocol,PalabraConstants
{	
	public boolean SIMULTANEOUS_PLAY = true;

	private PalabraState unresign;
	private PalabraState board_state;
	public PalabraState getState() {return(board_state); }
	public void setState(PalabraState st) 
	{ 	unresign = (st==PalabraState.RESIGN_STATE)?board_state:null;
	board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	public void SetDrawState() {throw G.Error("not expected"); };	
	//
	// these are the seed locations for the players to drop cards, except in 4 player games
	// the fourth player takes the fifth location.  The robots always drop their cards on
	// the seed location.  For the benefit of human players, the colors associated with
	// locations are spread around, but each cell on the board is owned by one player.
	//
	static int cardSeeds[][] = { {'A',7}, {'J',7}, {'J',4},{'G',2},{'B',3}};

	class PlayerBoard {
		// pick/drop logic per player
	    public PalabraChip pickedObject = null;
	    public int pickedHeight = 0;
	    public PalabraChip lastPicked = null;
	    private PalabraCell pickedSource = null; 
	    private PalabraCell droppedDest = null;
	    public int droppedHeight = 0;
		public PalabraCell droppedCard = null;
		public PalabraCell committedCard = null;
		public int droppedCardOrigin = 0;
		public String unPickMove()
		{
			if(pickedObject!=null)
			{
				return("edrop "+color.ordinal()+" PC "+(char)('A'+color.ordinal())+" "+pickedHeight);
			}
			return(null);
		}
		public String unDropMove()
		{
			if(droppedDest!=null)
			{
				return("epickb "+color.ordinal()+" "+droppedDest.col+" "+droppedDest.row);
			}
			return(null);
		}
		// constructor
		public PlayerBoard(Random r,PalabraColor c,PalabraCell hint)
		{	color = c;
			locationHint = hint;
			int ord = c.ordinal();
			char col = (char)('A'+ord);
			cards = new PalabraCell(r,PalabraId.PlayerCards,col);
			playedCards = new PalabraCell(r,PalabraId.PlayerDiscards,col);
			playedCards.showCardFace = true;
			wonPrizes = new PalabraCell(r,PalabraId.PlayerPrizes,col);
			reInit();
		}
		public void setDroppedCard(PalabraCell c,int h)
		{
			droppedCardOrigin = h;
			droppedCard = c;
		}
		public void setCommittedCard(PalabraCell c)
		{
			committedCard = c;
		}
		public void setCommittedCard() 
		{ if(droppedCard!=null) { setCommittedCard(droppedCard); }
		}
		public String toString() { return("<PlayerBoard "+color+">"); }
		public int score() 
		{	return(0);
		}
		public boolean hasDroppedCard()
			{ return(droppedCard!=null);
			}
		public boolean hasCommittedCard()
		{ return(committedCard!=null);
		}
		private void acceptPlacement()
		{	if(pickedObject!=null) { unPickObject(); }
			committedCard = null;
			droppedCard = null;
			droppedDest = null;
			pickedSource = null;
			pickedHeight = -1;
			droppedHeight = -1;
			droppedCardOrigin = -1;
		}
	    //
	    // undo the drop, restore the moving object to moving status.
	    //
	    private PalabraCell unDropObject()
	    {	PalabraCell rv = droppedDest;
	    	if(rv!=null) 
	    	{	pickedObject = (droppedHeight == -1) ? rv.removeTop() : rv.removeChipAtIndex(droppedHeight);
	    		droppedHeight = -1;
	    		droppedDest = null;
	     	}
	    	return(rv);
	    }
		// pick something up.  Note that when the something is the board,
	    // the board location really becomes empty, and we depend on unPickObject
	    // to replace the original contents if the pick is cancelled.
	    private PalabraChip pickObject(PalabraCell c,int row)
	    {	pickedSource = c;
	        switch (c.rackLocation())
	        {
	        default:
	        	throw G.Error("Not expecting rackLocation %s", c.rackLocation);
	        case BoardLocation:
	        	{
	         	boolean wasDest = isDestCell(c);
	        	unDropObject(); 
	        	if(!wasDest)
	        	{
	            lastPicked = pickedObject = c.topChip();
	            pickedHeight = -1;
	         	droppedDest = null;
				c.removeTop();
	        	}}
	            break;
	        case PlayerCards:
	        case PlayerDiscards:
	        case PlayerPrizes:
	        case PrizePool:
	        	lastPicked = pickedObject = (row==-1)?c.removeTop():c.removeChipAtIndex(row);
	        	pickedHeight = row;
	        }
	        return(pickedObject);
	    }
	    // 
	    // drop the floating object.
	    //
	    private void dropObject(PalabraCell c,int lvl)
	    {
	        switch (c.rackLocation())
	        {
	        default:
	        	throw G.Error("not expecting dest %s", c.rackLocation);
	        case BoardLocation:	// already filled board slot, which can happen in edit mode
	        case EmptyBoard:
	        	lvl = -1;
				//$FALL-THROUGH$
			case PlayerPrizes:
	        case PrizePool:
	        case PlayerCards:
	        case PlayerDiscards:
	           	if(lvl<0)
	           		{ c.addChip(pickedObject) ; } 
	           		else 
	           		{ c.insertChipAtIndex(lvl,pickedObject); }
	           	droppedDest = c;
	            lastDroppedObject = pickedObject;
	            droppedHeight = lvl;
	            pickedObject = null;
	            break;
	        }
	     }
	    public void doDropb(Palabramovespec m,replayMode replay)
	    {
	    	{
				PalabraCell dest =  getCell(PalabraId.BoardLocation,m.to_col,m.to_row);
				if(isSource(dest)) { unPickObject(); }
				else {  dropObject(dest,-1);
	            		setNextStateAfterDrop(m,replay); 
					}
	        	}
	    }
	    public void doMove(Palabramovespec m,replayMode replay)
	    {
        	PalabraCell src = getCell(m.source,m.from_col,m.from_row);
        	PalabraCell dest = getCell(PalabraId.EmptyBoard,m.to_col,m.to_row);
        	pickObject(src,m.from_row);
        	dropObject(dest,m.to_row);
        	if(replay.animate)
        		{	animationStack.push(src);
        			animationStack.push(dest);
        			
        		}
        	if(m.op==MOVE_CMOVE)
        		{m.moveInfo = "Card "+dest.topChip();
        		}
	    }
	    public void doSelect(Palabramovespec m,replayMode replay)
	    {
        	PalabraChip prize = pickObject(prizes,m.from_row);
        	currentPrize = getCell('D',7);
         	dropObject(currentPrize,-1);
        	setNextStateAfterDone(replay);
        	if(replay.animate)
        		{
        		animationStack.push(prizes);
        		animationStack.push(currentPrize);
        		m.moveInfo = "Select "+prize;
        		
        		}
	    }
	    public void doReset()
	    {
	    	PalabraCell dd = droppedDest;
			PalabraCell ps = pickedSource;
			if(pickedObject!=null) { unPickObject(); } 
			unDropObject();
			if((dd!=null) && (ps!=null))
			{
 				animationStack.push(dd);
				animationStack.push(ps);
			}
			unPickObject();
	    }
	    public void doPickb(Palabramovespec m,replayMode replay)
	    {
        	// come here only where there's something to pick, which must
        	// be a temporary p
	       	int row = m.from_row;
	       	PalabraCell src = getCell(m.source,m.from_col,row);
         	if(isDestCell(src)) 
        		{ unDropObject();
        		  setState(PalabraState.PLAY_STATE); 
        		}
        	else 
        		{ 	pickObject(src,row);
        		}
	    }
    	public void doDrop(Palabramovespec m,replayMode replay)
    	{  	PalabraCell dst = getCell(m.source,m.to_col,m.to_row);
    		if(isSource(dst)) { unPickObject(); }
    		else
    		{   dropObject(dst,m.to_row); 
    			//setNextStateAfterDrop(m,replay);
    		}
    	}

	    public boolean isDestCell(PalabraCell c) { return(droppedDest==c); }
	    
		//get the index in the image array corresponding to movingObjectChar 
	    // or HitNoWhere if no moving object.  This is used to determine what
	    // to draw when tracking the mouse.
	    // caution! this method is called in the mouse event process
	    public int movingObjectIndex()
	    { PalabraChip ch = pickedObject;
	      if(ch!=null)
	    	{	return(ch.chipNumber()); 
	    	}
	      	return (NothingMoving);
	    }

	    //	
	    //true if cell is the place where something was picked up.  This is used
	    // by the board display to provide a visual marker where the floating chip came from.
	    //
	    public boolean isSource(PalabraCell c)
	    {	return(c==pickedSource);
	    }
	    // 
	    // undo the pick, getting back to base state for the move
	    //
	    private void unPickObject()
	    {	if(pickedSource!=null) 
	    		{ if(pickedHeight==-1) { pickedSource.addChip(pickedObject); }
	    			else { pickedSource.insertChipAtIndex(pickedHeight,pickedObject); }
	    		}
		  pickedSource = null;
		  pickedHeight = -1;
		  pickedObject = null;
	    }
		public void reInit()
		{	cards.reInit();
			playedCards.reInit();
		   droppedCard = null;
		   committedCard = null;
	       droppedDest = null;
	       pickedSource = null;
	       pickedObject = null;
	       pickedHeight = -1;
	       droppedHeight = -1;
	       droppedCardOrigin = -1;
			wonPrizes.reInit();
		}
		public long Digest(Random r)
		{	long v = cards.Digest(r);
			v ^= playedCards.Digest(r);
			v ^= wonPrizes.Digest(r);
			v ^= chip.Digest(r,pickedObject);
			v ^= droppedHeight*+r.nextLong();
			v ^= pickedHeight*+r.nextLong();
			return(v);
		}
		public void copyFrom(PlayerBoard other)
		{	cards.copyFrom(other.cards);
			playedCards.copyFrom(other.playedCards);
			wonPrizes.copyFrom(other.wonPrizes);
			locationHint = getCell(other.locationHint);
			droppedCard = getCell(other.droppedCard);
			committedCard = getCell(other.committedCard);
			droppedCardOrigin = other.droppedCardOrigin;
	        droppedDest = getCell(other.droppedDest);
	        pickedSource = getCell(other.pickedSource);
	        pickedObject = other.pickedObject;
	        pickedHeight = other.pickedHeight;
	        droppedHeight = other.droppedHeight;
	        lastPicked = null;
		}
		public boolean sameBoard(PlayerBoard other)
		{	G.Assert(pickedObject==other.pickedObject, "picked Object matches");
			G.Assert(pickedHeight==other.pickedHeight,"picked height matches");
			G.Assert(droppedHeight==other.droppedHeight,"dropped height matches");		
			G.Assert(PalabraCell.sameCell(droppedCard,other.droppedCard),"dropped Card matches");
			G.Assert(PalabraCell.sameCell(committedCard,other.committedCard),"committed Card matches");
			G.Assert(droppedCardOrigin==other.droppedCardOrigin,"droppedCardOrigin mismatch");
			return(cards.sameCell(other.cards)
					&& playedCards.sameCell(other.playedCards)
					&& wonPrizes.sameCell(other.wonPrizes));
		}
		public PalabraCell goodLocation()
		{	return(locationHint);
		}
		PalabraColor color;
		PalabraCell cards;
		PalabraCell playedCards;
		PalabraCell wonPrizes;
		PalabraCell locationHint;
		
	}
	public int award_prize_timer = 0;
	public PalabraCell prizes;			// the unplayed prizes
	public PalabraCell currentPrize;	// the current prize or prizes
	public PlayerBoard[] playerBoards = null;
    public PlayerBoard currentPlayerBoard()
    {
    	return playerBoards[SIMULTANEOUS_PLAY?myIndex:whoseTurn];
    }
	public CellStack animationStack = new CellStack();

	private CellStack robotStack = null;	// non null in robot boards
	public void setRobotStack(CellStack r) { robotStack = r; }


    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
	private int myIndex = 0;
    private PalabraState resetState = PalabraState.PUZZLE_STATE; 
    public PalabraChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public PalabraCell newcell(char c,int r)
	{	return(new PalabraCell(PalabraId.BoardLocation,c,r));
	}
	public void setMyIndex(int n,boolean sim)
	{	SIMULTANEOUS_PLAY = sim;
		myIndex = Math.max(0,n);
	}
	public int scoreForPlayer(int pl)
	{
		return(playerBoards[pl].score());
	}
	
	// get the id of the piece we have picked up with the local UI
	// for most games, only the one we've picked up can be moving.
	public int movingObjectIndex()
	{
		return(currentPlayerBoard().movingObjectIndex());
	}

	public boolean isDestCell(PalabraCell c)
	{
		return(currentPlayerBoard().isDestCell(c));
	}
	public boolean isSource(PalabraCell c)
	{
		return(currentPlayerBoard().isSource(c));
	}	
	public boolean WinForPlayer(int pl)
	{	// for 2 player games, also for montebot in multiplayer games
		int myScore = scoreForPlayer(pl);
		int oScore = -100;
		for(int i=0;i<playerBoards.length; i++) { if(i!=pl) { oScore = Math.max(oScore,scoreForPlayer(i)); }}
		return(myScore>oScore);
	}
	public boolean winForPlayerNow(int pl)
	{	int myscore = scoreForPlayer(pl);
	 	int best = -1;
	 	for(int i=0;i<players_in_game; i++)
	 	{	if(i!=pl) { best=Math.max(best,scoreForPlayer(i)); }
	 	}
	 	return(myscore>best);
	}

    public PalabraBoard(String init,long key,int np) // default constructor
    {	super.initBoard(10,10);	// not used really
    	Random r = new Random(12345);
    	allCells.setDigestChain(r);
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        prizes = new PalabraCell(r,PalabraId.PrizePool);
		prizes.showPrizeBack = true;

        currentPrize = null;
        doInit(init,key,np,0); // do the initialization 
    }
    public PalabraBoard cloneBoard() 
	{ PalabraBoard dup = new PalabraBoard(gametype,randomKey,players_in_game); 
	  dup.copyFrom(this);
	  return(dup); 
   	}

    public void copyFrom(BoardProtocol b) { copyFrom((PalabraBoard)b); }

    public void sameboard(BoardProtocol f) { sameboard((PalabraBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(PalabraBoard from_b)
    {
        super.sameboard(from_b); // hexboard compares the cells of the board using cell.sameCell
        G.Assert(PalabraCell.sameCell(prizes,from_b.prizes),"prizes %s and %s mismatch",prizes,from_b.prizes);
        G.Assert(PalabraCell.sameCell(currentPrize,from_b.currentPrize),"prizes %s and %s mismatch",currentPrize,from_b.currentPrize);

        for(int i=0;i<players_in_game;i++)
        {	G.Assert(playerBoards[i].sameBoard(from_b.playerBoards[i]), 
        		"playerBoards %s and %s match ",playerBoards[i],from_b.playerBoards[i]);
        }
        for (int i = 0; i < win.length; i++)
        {
            G.Assert(win[i] == from_b.win[i], "Win[] matches");
        }
        G.Assert(SIMULTANEOUS_PLAY==from_b.SIMULTANEOUS_PLAY, "simultaneous play mismatch");
 
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are germane to the ordinary operation
     * of the game, others are for system record keeping use; so it is important that the
     * game Digest be consistent both within a game and between games over a long period
     * of time which have the same moves. 
     * (1) Digest is used by the default implementation of EditHistory to remove moves
     * that have returned the game to a previous state; ie when you undo a move or
     * hit the reset button.  
     * (2) Digest is used after EditHistory to verify that replaying the history results
     * in the same game as the user is looking at.  This catches errors in implementing
     * undo, reset, and EditHistory
	 * (3) Digest is used by standard robot search to verify that move/unmove 
	 * returns to the same board state, also that move/move/unmove/unmove etc.
	 * (4) Digests are also used as the game is played to look for draw by repetition.  The state
     * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
     * (5) games where repetition is forbidden (like xiangqi/arimaa) can also use this
     * information to detect forbidden loops.
	 * (6) Digest is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game, and a midpoint state of the game. Other site machinery
     * looks for duplicate digests.  
     * (7) digests are also used in live play to detect "parroting" by running two games
     * simultaneously and playing one against the other.
     */
    public long Digest()
    {
    	long v = 0;
 
        // the basic digestion technique is to xor a bunch of random numbers. 
    	// many object have an associated unique random number, including "chip" and "cell"
    	// derivatives.  If the same object is digested more than once (ie; once as a chip
    	// in play, and once as the chip currently "picked up", then it must be given a
    	// different identity for the second use.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        
        v ^= players_in_game*r.nextLong();
        v ^= prizes.Digest(r);
        for(PlayerBoard bb : playerBoards)  {	v ^= bb.Digest(r); }
        
		// note we can't change this without invalidating all the existing digests.
		for(PalabraCell c=allCells; c!=null; c=c.next)
		{	
            v ^= c.Digest(r);
		}
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(PalabraBoard from_b)
    {	
    	super.copyFrom(from_b);
    	
    	prizes.copyFrom(from_b.prizes);
        currentPrize = getCell(from_b.currentPrize);
        for(int i=0;i<players_in_game;i++)
        {	playerBoards[i].copyFrom(from_b.playerBoards[i]);
        }
        SIMULTANEOUS_PLAY=from_b.SIMULTANEOUS_PLAY;
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        resetState = from_b.resetState;
 
        sameboard(from_b);
    }

	public void doInit()
	{	doInit(gametype,randomKey,players_in_game,myIndex);
	}
	public void doInit(String gtype,long key)
	{
		doInit(gtype,key,players_in_game,myIndex);
	}
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key, int np,int my)
    {
        myIndex = my;
	   if(Palabra_init.equalsIgnoreCase(gtype)) { }
	   else { throw G.Error(WrongInitError,gtype); }
	   for(PalabraCell c = allCells; c!=null; c=c.next) { c.reInit(); }
	   gametype = gtype;
	   randomKey = key;
       players_in_game = np;
       award_prize_timer = 0;
       win = new boolean[np];
	   setState(PalabraState.PUZZLE_STATE);
	   prizes.reInit();
	   currentPrize = null;
	   Random rr = new Random(randomKey);
	   Random r = new Random(7346);	// always the same
	   prizes.shuffle(rr);
	   playerBoards= new PlayerBoard[players_in_game];
	   {	int i=0;
	   		for(PalabraColor color : PalabraColor.values())
	   			{	int ord = color.ordinal();
	   				int loc[] = cardSeeds[((players_in_game==4)&&(ord==3))?4:ord];
	   				PalabraCell hint = getCell((char)loc[0],loc[1]);
	   				hint.color = color;
	   				playerBoards[i++] = new PlayerBoard(r,color,hint); 
	   				if(i>=players_in_game) { break; }
	   			}}
	   // spread the colors around so the humans have a bunch of cells in a zone near
	   // their player goodies to choose from.  It should look natural.
	   boolean progress = false;
	   int pass = 0;
	   do {	// spread the colors around
		   progress = false;
		   pass++;
		   for(PalabraCell c =allCells; c!=null; c=c.next)
		   {   PalabraColor color = c.color;
		   	   if((color!=null) && (c.sweep_counter!=pass))
		   		{
			   		for(int dir = 0;dir<CELL_FULL_TURN; dir++)
			   		{	PalabraCell d = c.exitTo(dir);
			   			if((d!=null) && (d.color==null)) 
			   				{ progress=true;
			   				  d.sweep_counter = pass;
			   				  d.color = color;
			   				}
			   		}
			   }
		   }
	   } while(progress);
	   whoseTurn = FIRST_PLAYER_INDEX;
       resetState = PalabraState.PUZZLE_STATE;
       lastDroppedObject = null;
    
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }


    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player");
         case PUZZLE_STATE:
            break;
        case CONFIRM_STATE:
		case CONFIRM_CARD_STATE:
        case RESIGN_STATE:
            moveNumber++; //the move is complete in these states
			setWhoseTurn((whoseTurn+1)%players_in_game);
            return;
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	
        switch (board_state)
        {
        case RESIGN_STATE:
        case CONFIRM_STATE:
            return (true);

        default:
            return (false);
        }
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(DoneState());
    }


    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	for(int lim=playerBoards.length-1; lim>=0;lim--)
    	{
    		playerBoards[lim].acceptPlacement();
    	}
     }

    public PalabraCell getCell(PalabraCell c)
    {
    	return(c==null?null:getCell(c.rackLocation(),c.col,c.row));
    }
    /**
     * get the cell represented by a source code, and col,row
     * @param source
     * @param col
     * @param row
     * @return
     */
    private PalabraCell getCell(PalabraId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case PlayerDiscards:
        	return(playerBoards[col-'A'].playedCards);
        case PlayerCards:
        	return(playerBoards[col-'A'].cards);
        case PlayerPrizes:
        	return(playerBoards[col-'A'].wonPrizes);
        case PrizePool:
        	return(prizes);
        case EmptyBoard:
        case BoardLocation:
        	return(getCell(col,row));

        } 	
    }

    // true if all players have played a card.  This is the trigger
    // for us to move from async to sync play if it is also our turn.
    private boolean allDroppedCard()
    {	
    	for(PlayerBoard pl : playerBoards)
			{
				if(!pl.hasDroppedCard()) 
					{ return(false); }
			}
    	return(true);
    }
    
    // true of all players have committed to their dropped card, so now
    // we can award the prize if it is our turn.
    private boolean allCommittedCard()
    {
    	for(PlayerBoard pl : playerBoards)
		{
			if(!pl.hasCommittedCard()) 
				{ return(false); }
		}
	return(true);
    	
    }
    private void setNextStateAfterDrop(Palabramovespec m,replayMode replay)
    {
        switch (board_state)
        {
        case CONFIRM_CARD_STATE:
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        
        case CONFIRM_STATE:
        	setNextStateAfterDone(replay);
        	break;
        case PLAY_STATE:
        	{	if(!SIMULTANEOUS_PLAY) { setState(PalabraState.CONFIRM_STATE); }
        		else if((m.op==EPHEMERAL_DROPB)||(m.op==EPHEMERAL_MOVE_FROM_TO))
        		{
        		if(allDroppedCard())
        			{
        			setState(PalabraState.CONFIRM_CARD_STATE);	// all go to confirm
        			}
        		}
        		else {	setState(PalabraState.CONFIRM_STATE); 	// using synchronous moves, goto confirm state
        		}}
			break;
        case PUZZLE_STATE:
			acceptPlacement();
            break;
        }
    }
    public boolean hasDroppedCard(int pl)
    {
    	return(playerBoards[pl].hasDroppedCard());
    }
    public String cardMove()
    {
    	PlayerBoard pb = playerBoards[whoseTurn];
    	PalabraCell to = pb.droppedCard;
    	if(to!=null)
    	{ 
    		// normally, "to" is the card we dropped. If we changed our mind
    		// before the last player locked in, we might not have committed
    		// and to can be null.  In that case, we are allowed to continue
    		// dithering until a card is dropped.
    	return("Cmove "+((char)('A'+whoseTurn))+" "+pb.droppedCardOrigin+" "+to.col+" "+to.row);
    	}
    	return(null);
    }
    private void setNextStateAfterDone(replayMode replay)
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case SELECT_PRIZE_STATE:
    		setState(PalabraState.PLAY_STATE);
    		break;
    	case GAMEOVER_STATE: 
    		break;
    	case CONFIRM_STATE:
    		setState(PalabraState.PLAY_STATE);
			//$FALL-THROUGH$
		case CONFIRM_CARD_STATE:
    		if(allCommittedCard()) 
    			{	award_prize_timer = 0; 
    				setState(PalabraState.AWARD_PRIZE_STATE); 
    			}
    		break;
    	case PUZZLE_STATE:
    		setState((gameOverNow()||(prizes.height()==0))?PalabraState.GAMEOVER_STATE:PalabraState.SELECT_PRIZE_STATE);
    		break;
    	}
       	resetState = board_state;
    }
    public boolean gameOverNow()
    	{ return((prizes.height()==0) && (currentPrize==null));
    	}

    private void doDone(replayMode replay)
    {
        if (board_state==PalabraState.RESIGN_STATE)
        {	win[whoseTurn] = false;		// this will only be allowed in 2 player games
        	win[(whoseTurn+1)%players_in_game] = true;
            setState(PalabraState.GAMEOVER_STATE);
        }
        else
        {	if(gameOverNow())
        		{ 
        		  setState(PalabraState.GAMEOVER_STATE); 
        		}
        	else {playerBoards[whoseTurn].setCommittedCard();
        		  setNextPlayer();
        		  setNextStateAfterDone(replay);
        	}
        }
    }

    public String unPickMove()
    {	
    	return(playerBoards[myIndex].unPickMove());
    }
    public String unDropMove()
    {	return(playerBoards[myIndex].unDropMove());
     }
    public boolean Execute(commonMove mm,replayMode replay)
    {	Palabramovespec m = (Palabramovespec)mm;
        
        if(replay.animate) { animationStack.clear(); }

        switch (m.op)
        {

        case MOVE_DONE:
        	
         	doDone(replay);
           break;

	        
        case EPHEMERAL_DROPB:
        	G.Assert(SIMULTANEOUS_PLAY,"shouldnt' be used");
			//$FALL-THROUGH$
		case MOVE_DROPB:
        	playerBoards[m.player].doDropb(m,replay);
            break;

        case EPHEMERAL_PICK:
        case EPHEMERAL_PICKB:
        	G.Assert(SIMULTANEOUS_PLAY,"shoulnt' be used");
			//$FALL-THROUGH$
		case MOVE_PICK:
        case MOVE_PICKB:
        	playerBoards[m.player].doPickb(m,replay);
            break;

        case EPHEMERAL_DROP:
        case MOVE_DROP: // drop on chip pool;
        	playerBoards[m.player].doDrop(m,replay);

            break;
        case MOVE_CMOVE:
        	// undo the ephemeral moves
        	{
        	PlayerBoard pb = playerBoards[m.player];
        	PalabraCell dest = getCell(PalabraId.BoardLocation,m.to_col,m.to_row);
        	PalabraCell src = getCell(m.source,m.from_col,m.from_row);
        	if(dest.topChip()!=null)
        		{ pb.pickObject(dest,m.to_row);
        		  pb.dropObject(src,m.from_row);
        		}
        	pb.setCommittedCard(dest);
        	playerBoards[m.player].doMove(m,replayMode.Replay);	// no animation of the re-move
        	setState(PalabraState.CONFIRM_CARD_STATE);	
        	setNextPlayer();
        	setNextStateAfterDone(replay);
        	}
        	break;
        case EPHEMERAL_MOVE_FROM_TO:
        	{PlayerBoard pb = playerBoards[m.player];
        	 pb.doMove(m,replay);
        	 setNextStateAfterDrop(m,replay);
        	}
        	break;
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(PalabraState.PUZZLE_STATE);	// standardize the current state
            setNextStateAfterDone(replay); 
            break;

       case MOVE_RESIGN:
            setState(unresign==null?PalabraState.RESIGN_STATE:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(PalabraState.PUZZLE_STATE);
 
            break;
        case MOVE_SELECT:
        	playerBoards[m.player].doSelect(m,replay);
        	acceptPlacement();
        	break;
        case MOVE_GAMEOVERONTIME:
      	   win[whoseTurn] = true;
      	   setState(PalabraState.GAMEOVER_STATE);
      	   break;
        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);
        //G.print(m + " "+Digest());
        return (true);
    }

    public boolean LegalToHitCards(PalabraCell c)
    {	
	    switch (board_state)
	    {
	    default:
	    	throw G.Error("Not expecting state %s", board_state);
		case CONFIRM_CARD_STATE:
			// if we got into confirm state without comitting a card, we need to be 
			// able to pick up a card.  This is unusual, only happens if we changed
			// out mind and other players committed before seeing it.
	    case PLAY_STATE:
	    	{
	    	if(c.rackLocation!=PalabraId.PlayerCards) { return(false); }
	    	PlayerBoard pb = currentPlayerBoard();
	    	if(pb.pickedObject!=null)
	    		{	PalabraChip ch = pb.pickedObject;
	    			return( ch.isCard() );
	    		}
	    	return(pb.droppedCard==null);
	    	}
	    case CONFIRM_STATE:
	    case SELECT_PRIZE_STATE:
		case AWARD_PRIZE_STATE:
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);
	    case PUZZLE_STATE:
	    	{
	    	PalabraChip pickedObject = currentPlayerBoard().pickedObject;
	        return ((pickedObject==null)
	                 ?true
	                 :pickedObject.isCard());    
	    	}}
    	
    }


    public boolean LegalToHitBoard(PalabraCell c,boolean any)
    {	if(c==null) { return(false); }
    	PalabraChip pickedObject = currentPlayerBoard().pickedObject;
        switch (board_state)
        {
		case PLAY_STATE:
			if(pickedObject==null)
			{
				return(any ? (c.topChip()!=null) : currentPlayerBoard().isDestCell(c));
			}
			else 
			{	// each cell has an invisible "color" which determines who owns the cell for
				// purposes of dropping cards.  This keeps the players from stomping on each
				// other's cards.
				if(pickedObject.isCard()) { return(false); }
				return((c.topChip()==null));
			}
			
		case GAMEOVER_STATE:
		case AWARD_PRIZE_STATE:
		case SELECT_PRIZE_STATE:
		case CONFIRM_CARD_STATE:
		case RESIGN_STATE:
			return(false);
		case CONFIRM_STATE:
		//case WAIT_STATE:
			return(currentPlayerBoard().isDestCell(c));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case PUZZLE_STATE:
            return ((c.topChip()!=null)!=(pickedObject!=null));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Palabramovespec m)
    {	m.state = board_state;
        m.undoInfo = robotStack.size()*100; //record the starting state. The most reliable
        //G.print("R "+m);
        if (Execute(m,replayMode.Replay))
        {
            if ((m.op == MOVE_DONE)||(m.op==MOVE_SELECT)||(m.op==MOVE_AWARD))
            {
            }
            else if (DoneState())
            {
                doDone(replayMode.Replay);
            }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
    }
    
    //
    // when the robot is started, some players may have already placed their cards,
    // so it's necessary to "forget" that extra piece of information.
    //
    public void undoEphemeralMoves()
    {
    	if(board_state==PalabraState.PLAY_STATE)
    		{ for(PlayerBoard pl : playerBoards) { pl.doReset(); }
    		}
    }

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Palabramovespec m)
    {
        //G.print("U "+m);

        switch (m.op)
        {
   	    default:
   	    	throw G.Error("Can't un execute %s", m);
        case MOVE_DONE:
            break;
        case MOVE_AWARD:
        	// no spesical action needed, but unwindoing the robot 
        	// stack later in this function does the real work.
        	break;
        case MOVE_SELECT:
        	{
        	PalabraCell src = currentPrize;
        	PalabraCell dest = prizes;
        	playerBoards[m.player].pickObject(src,-1);
        	playerBoards[m.player].dropObject(dest,m.from_row);
        	acceptPlacement();
        	}
        	break;
        case EPHEMERAL_MOVE_FROM_TO:
    		{
        	PalabraCell src = getCell(m.source,m.from_col,m.from_row);
        	PalabraCell dest = getCell(PalabraId.EmptyBoard,m.to_col,m.to_row);
        	PlayerBoard pb = playerBoards[m.player];
        	pb.pickObject(dest,m.from_row);
        	pb.dropObject(src,m.from_row);
        	pb.acceptPlacement();
        	}
    		break;
        case MOVE_DROPB:
        	getCell(m.to_col,m.to_row).removeTop();
        	break;
        case MOVE_RESIGN:

            break;
        }

        setState(m.state);
        int size = m.undoInfo/100;
        while(robotStack.size()>size)
        {
        		PalabraCell dest = robotStack.pop();
        		PalabraCell src = robotStack.pop();
        		src.addChip(dest.removeTop());
        }
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
 void addPlayCardsMoves(CommonMoveStack  all,int who)
 {
	 PlayerBoard bd = playerBoards[who];
	 PalabraCell cards = bd.cards;
	 PalabraCell location = bd.goodLocation();
	 for(int lim = cards.height()-1; lim>=0; lim--)
	 {
		 all.push(new Palabramovespec(EPHEMERAL_MOVE_FROM_TO,cards.col,lim,location.col,location.row,whoseTurn));
	 }
 }
 // in the real game we just take the top of the stack, but for the robot
 // we need to randomize the order of the stack it evaluates differently
 // from that which will actually come up.
 void addSelectPrizeMoves(CommonMoveStack  all,int who)
 {
	 for(int lim = prizes.height()-1; lim>=0; lim--)
	 {
		 all.push(new Palabramovespec(MOVE_SELECT,lim,whoseTurn));
	 }
		 
 }
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case PLAY_STATE:
 		// can play any of the cards left
 		addPlayCardsMoves(all,whoseTurn);
 		break;
 	case AWARD_PRIZE_STATE:
 		all.push(new Palabramovespec(MOVE_AWARD,whoseTurn));
 		break;
 	case SELECT_PRIZE_STATE:
 		addSelectPrizeMoves(all,whoseTurn);
 		break;
 	case CONFIRM_CARD_STATE:
 		all.push(new Palabramovespec(cardMove(),whoseTurn));
 		break;
 	default:
 		throw G.Error("Not implemented");
 	}
 	return(all);
 }
 


}
