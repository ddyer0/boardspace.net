package raj;

import lib.AR;
import lib.G;
import lib.IStack;
import lib.Random;
import lib.Sort;
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

class RajBoard extends squareBoard<RajCell> implements BoardProtocol,RajConstants
{	
	public boolean SIMULTANEOUS_PLAY = true;

	private RajState unresign;
	private RajState board_state;
	public RajState getState() {return(board_state); }
	public void setState(RajState st) 
	{ 	unresign = (st==RajState.RESIGN_STATE)?board_state:null;
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
	public RajCell goodLocation(RajColor color,int x,int y)
	{	RajCell best = null;
		double bestD = 0;
		// find the nearest card seed to the x,y
		for(int []seed : cardSeeds)
		{
			RajCell c = getCell((char)seed[0],seed[1]);
			if(c.topChip()==null)
			{	int cx = cellToX(c.col,c.row);
				int cy = cellToY(c.col,c.row);
				double dis = G.distance(x, y, cx, cy);
				if(best==null || bestD>dis)
				{
					best = c;
					bestD = dis;
				}
			}
		}
		best.color = color;
		return(best);
	}
	class PlayerBoard {
		// pick/drop logic per player
		public int myIndex = -1;
	    public RajChip pickedObject = null;
	    public int pickedHeight = -1;
	    public RajChip lastPicked = null;
	    private RajCell pickedSource = null; 
	    private RajCell droppedDest = null;
	    public int droppedHeight = 0;
		public RajCell droppedCard = null;
		public RajCell committedCard = null;
		public boolean hiddenShowCards = true;
		public int droppedCardOrigin = 0;
		public RajChip cardBack = null;
		public String unPickMove()
		{
			if(pickedObject!=null)
			{
				return("edrop "+myIndex+" PC "+(char)('A'+myIndex)+" "+pickedHeight);
			}
			return(null);
		}
		public String unDropMove()
		{
			if(droppedDest!=null)
			{
				return("epickb "+myIndex+" "+droppedDest.col+" "+droppedDest.row);
			}
			return(null);
		}
		// constructor
		public PlayerBoard(Random r,int idx,RajColor c)
		{	color = c;
			int ord = myIndex = idx;
			char col = (char)('A'+ord);
			cardBack = RajChip.getCardBack(c);
			cards = new RajCell(r,RajId.PlayerCards,col);
			playedCards = new RajCell(r,RajId.PlayerDiscards,col);
			playedCards.showCardFace = true;
			wonPrizes = new RajCell(r,RajId.PlayerPrizes,col);
			reInit();
		}
		public void setDroppedCard(RajCell c,int h)
		{
			droppedCardOrigin = h;
			droppedCard = c;
		}
		public void setCommittedCard(RajCell c)
		{
			committedCard = c;
		}
		public void setCommittedCard() 
		{ if(droppedCard!=null) { setCommittedCard(droppedCard); }
		}
		public String toString() { return("<PlayerBoard "+color+">"); }
		public int score() 
		{	return(wonPrizes.totalPrizeValue());
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
	    private RajCell unDropObject()
	    {	RajCell rv = droppedDest;
	    	if(rv!=null) 
	    	{	pickedObject = (droppedHeight == -1) ? rv.removeTop() : rv.removeChipAtIndex(droppedHeight);
	    		droppedHeight = -1;
	    		unDropCard(rv,pickedObject);
	    		droppedDest = null;
	     	}
	    	return(rv);
	    }
		// pick something up.  Note that when the something is the board,
	    // the board location really becomes empty, and we depend on unPickObject
	    // to replace the original contents if the pick is cancelled.
	    private RajChip pickObject(RajCell c,int row)
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
	         	lastDroppedObject = null;
	         	droppedDest = null;
				c.removeTop();
				unDropCard(c,pickedObject);
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
	    private void dropObject(RajCell c,int lvl)
	    {
	        switch (c.rackLocation())
	        {
	        default:
	        	throw G.Error("not expecting dest %s", c.rackLocation);
	        case BoardLocation:	// already filled board slot, which can happen in edit mode
	        case EmptyBoard:
	        	dropCard(c,pickedObject,pickedHeight);
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
	    public void doDropb(Rajmovespec m,replayMode replay)
	    {
	    	{
				RajCell dest =  getCell(RajId.BoardLocation,m.to_col,m.to_row);
				if(isSource(dest)) { unPickObject(); }
				else {  dropObject(dest,-1);
	            		setNextStateAfterDrop(m,replay); 
					}
	        	}
	    }
	    public void doMove(Rajmovespec m,replayMode replay)
	    {
        	RajCell src = getCell(m.source,m.from_col,m.from_row);
        	RajCell dest = getCell(RajId.EmptyBoard,m.to_col,m.to_row);
        	pickObject(src,m.from_row);
        	dropObject(dest,m.to_row);
        	pickedHeight = -1;		// move is final, keep pickedHeight -1 for Digest
        	if(replay!=replayMode.Replay)
        		{	animationStack.push(src);
        			animationStack.push(dest);
        			
        		}
        	if(m.op==MOVE_CMOVE)
        		{m.moveInfo = "Card "+(dest.topChip().cardValue());
        		}
	    }
	    public void doSelect(Rajmovespec m,replayMode replay)
	    {
        	RajChip prize = pickObject(prizes,m.from_row);
        	currentPrize = getCell('D',7);
         	dropObject(currentPrize,-1);
        	setNextStateAfterDone(replay);
        	if(replay!=replayMode.Replay)
        		{
        		animationStack.push(prizes);
        		animationStack.push(currentPrize);
        		m.moveInfo = "Select "+prize.prizeValue();
        		
        		}
	    }
	    public void doReset()
	    {
	    	RajCell dd = droppedDest;
			RajCell ps = pickedSource;
			if(pickedObject!=null) { unPickObject(); } 
			unDropObject();
			if((dd!=null) && (ps!=null))
			{
 				animationStack.push(dd);
				animationStack.push(ps);
			}
			unPickObject();
	    }
	    public void doPickb(Rajmovespec m,replayMode replay)
	    {
        	// come here only where there's something to pick, which must
        	// be a temporary p
	       	int row = m.from_row;
	       	RajCell src = getCell(m.source,m.from_col,row);
         	if(isDestCell(src)) 
        		{ unDropObject();
        		  setState(RajState.PLAY_STATE); 
        		}
        	else 
        		{ 	pickObject(src,row);
        		}
	    }
    	public void doDrop(Rajmovespec m,replayMode replay)
    	{  	RajCell dst = getCell(m.source,m.to_col,m.to_row);
    		if(isSource(dst)) { unPickObject(); }
    		else
    		{   dropObject(dst,m.to_row); 
    			//setNextStateAfterDrop(m,replay);
    		}
    	}

	    public boolean isDestCell(RajCell c) { return(droppedDest==c); }
	    
		//get the index in the image array corresponding to movingObjectChar 
	    // or HitNoWhere if no moving object.  This is used to determine what
	    // to draw when tracking the mouse.
	    // caution! this method is called in the mouse event process
	    public int movingObjectIndex()
	    { RajChip ch = pickedObject;
	      if(ch!=null)
	    	{	return(ch.chipNumber()); 
	    	}
	      	return (NothingMoving);
	    }
	    
	    //	
	    //true if cell is the place where something was picked up.  This is used
	    // by the board display to provide a visual marker where the floating chip came from.
	    //
	    public boolean isSource(RajCell c)
	    {	return(c==pickedSource);
	    }
	    // 
	    // undo the pick, getting back to base state for the move
	    //
	    private void unPickObject()
	    {	if(pickedSource!=null) 
	    		{ if(pickedHeight==-1) { pickedSource.addChip(pickedObject); }
	    			else { pickedSource.insertChipAtIndex(pickedHeight,pickedObject); }
	    		  dropCard(pickedSource,pickedObject,pickedHeight);
	    		}
		  pickedSource = null;
		  pickedHeight = -1;
		  pickedObject = null;
	    }
		public void reInit()
		{	cards.reInit();
			playedCards.reInit();
			for(int i=1;i<=NUMBER_OF_PRIZES;i++)
			{	cards.addChip(RajChip.getCard(color,i));
			}
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
			//System.out.println("DP1 "+v);
			v ^= playedCards.Digest(r);
			//System.out.println("DP2 "+v);
			v ^= wonPrizes.Digest(r);
			//System.out.println("DP3 "+v);
			v ^= chip.Digest(r,pickedObject);
			//System.out.println("DP4 "+v);
			v ^= droppedHeight*+r.nextLong();
			//System.out.println("DP5 "+v);
			v ^= pickedHeight*+r.nextLong();
			//System.out.println("DP6 "+v+" "+pickedHeight);
			return(v);
		}
		public void copyFrom(PlayerBoard other)
		{	cards.copyFrom(other.cards);
			playedCards.copyFrom(other.playedCards);
			wonPrizes.copyFrom(other.wonPrizes);
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
			G.Assert(RajCell.sameCell(droppedCard,other.droppedCard),"dropped Card matches");
			G.Assert(RajCell.sameCell(committedCard,other.committedCard),"committed Card matches");
			G.Assert(droppedCardOrigin==other.droppedCardOrigin,"droppedCardOrigin mismatch");
			return(cards.sameCell(other.cards)
					&& playedCards.sameCell(other.playedCards)
					&& wonPrizes.sameCell(other.wonPrizes));
		}
		RajColor color;
		RajCell cards;
		RajCell playedCards;
		RajCell wonPrizes;
		
	}
	public int award_prize_timer = 0;
	public RajCell prizes;			// the unplayed prizes
	public RajCell currentPrize;	// the current prize or prizes
	private PlayerBoard[] playerBoards = null;
    public PlayerBoard currentPlayerBoard()
    { return getPlayerBoard(SIMULTANEOUS_PLAY?myIndex:whoseTurn);
    }
    public PlayerBoard getPlayerBoard(int ind)
    {	PlayerBoard pb[] = playerBoards;
    	if(pb!=null && ind<pb.length) { return pb[ind]; }
    	return(null);
    }
	public CellStack animationStack = new CellStack();

	private CellStack robotStack = null;	// non null in robot boards
	public void setRobotStack(CellStack r) { robotStack = r; }
    //
    // private variables
    //
	public void unDropCard(RajCell c,RajChip ch)
	{
		if((ch.isCard()&&(c.rackLocation==RajId.BoardLocation)))
		{
			playerBoards[playerOwning(ch.cardColor())].setDroppedCard(null,-1);
		}
	}
	public void dropCard(RajCell c,RajChip ch,int h)
	{
		if(ch.isCard()&&(c.rackLocation==RajId.BoardLocation))
		{	
			playerBoards[playerOwning(ch.cardColor())].setDroppedCard(c,h);
		}
	}

    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
	private int myIndex = 0;
    private RajState resetState = RajState.PUZZLE_STATE; 
    public RajChip lastDroppedObject = null;	// for image adjustment logic

	// factory method to generate a board cell
	public RajCell newcell(char c,int r)
	{	return(new RajCell(RajId.BoardLocation,c,r));
	}
	public void setMyIndex(int n,boolean sim)
	{	SIMULTANEOUS_PLAY = sim;
		myIndex = Math.max(0,n);
	}
	public int ScoreForPlayerNow(int pl)
	{
		return(playerBoards[pl].score());
	}
	
	// get the id of the piece we have picked up with the local UI
	// for most games, only the one we've picked up can be moving.
	public int movingObjectIndex()
	{	PlayerBoard pb = currentPlayerBoard();
		return(pb==null ? NothingMoving : pb.movingObjectIndex());
	}

	public boolean isDestCell(RajCell c)
	{
		return(currentPlayerBoard().isDestCell(c));
	}
	public boolean isSource(RajCell c)
	{
		return(currentPlayerBoard().isSource(c));
	}	
	public boolean WinForPlayer(int pl)
	{	// for 2 player games, also for montebot in multiplayer games
		int myScore = ScoreForPlayerNow(pl);
		int oScore = -100;
		for(int i=0;i<playerBoards.length; i++) { if(i!=pl) { oScore = Math.max(oScore,ScoreForPlayerNow(i)); }}
		return(myScore>oScore);
	}
	public boolean winForPlayerNow(int pl)
	{	int myscore = ScoreForPlayerNow(pl);
	 	int best = -1;
	 	for(int i=0;i<players_in_game; i++)
	 	{	if(i!=pl) { best=Math.max(best,ScoreForPlayerNow(i)); }
	 	}
	 	return(myscore>best);
	}

    public RajBoard(String init,long key,int np,int[]map) // default constructor
    {	setColorMap(map);
    	super.initBoard(10,10);	// not used really
    	Random r = new Random(12345);
    	allCells.setDigestChain(r);
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        prizes = new RajCell(r,RajId.PrizePool);
		prizes.showPrizeBack = true;

        currentPrize = null;
        doInit(init,key,np,0); // do the initialization 
    }
    public RajBoard cloneBoard() 
	{ RajBoard dup = new RajBoard(gametype,randomKey,players_in_game,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}

    public void copyFrom(BoardProtocol b) { copyFrom((RajBoard)b); }

    public void sameboard(BoardProtocol f) { sameboard((RajBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(RajBoard from_b)
    {
        super.sameboard(from_b); // hexboard compares the cells of the board using cell.sameCell
        G.Assert(RajCell.sameCell(prizes,from_b.prizes),"prizes %s and %s mismatch",prizes,from_b.prizes);
        G.Assert(RajCell.sameCell(currentPrize,from_b.currentPrize),"prizes %s and %s mismatch",currentPrize,from_b.currentPrize);
        for(int i=0;i<players_in_game;i++)
        {	G.Assert(playerBoards[i].sameBoard(from_b.playerBoards[i]), 
        		"playerBoards %s and %s match ",playerBoards[i],from_b.playerBoards[i]);
        }
        G.Assert(SIMULTANEOUS_PLAY==from_b.SIMULTANEOUS_PLAY, "simultaneous play mismatch");

        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Digest matches");
        G.Assert(myIndex==from_b.myIndex,"Myindex mismatch");
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
       //System.out.println();
       //v ^= players_in_game*r.nextLong();
       //System.out.println("D1 "+v);
       //v ^= prizes.Digest();
       //System.out.println("D2 "+v);
       for(PlayerBoard bb : playerBoards) 
       	{	v ^= bb.Digest(r); 
        //System.out.println("D3 "+v);

       	}
        
		// note we can't change this without invalidating all the existing digests.
		for(RajCell c=allCells; c!=null; c=c.next)
		{	
            v ^= c.Digest(r);
		}
	      //System.out.println("D4 "+v);

		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		v ^= r.nextLong()*myIndex;
	     // System.out.println("D5 "+v);

        return (v);
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(RajBoard from_b)
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
        myIndex = from_b.myIndex;
 
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
	   if(Raj_INIT.equalsIgnoreCase(gtype)) { }
	   else { throw G.Error(WrongInitError,gtype); }
	   for(RajCell c = allCells; c!=null; c=c.next) { c.reInit(); }
	   gametype = gtype;
	   randomKey = key;
	   players_in_game = np;
       award_prize_timer = 0;
       win = new boolean[np];
	   setState(RajState.PUZZLE_STATE);
	   prizes.reInit();
	   currentPrize = null;
	   for(int i=MIN_PRIZE_VALUE;i<=MAX_PRIZE_VALUE;i++)
	    {	if(i!=0) { prizes.addChip(RajChip.getPrize(i)); }
	    }
	   Random rr = new Random(randomKey);
	   Random r = new Random(7346);	// always the same
	   prizes.shuffle(rr);
	   int map[]=getColorMap();
	   RajColor colors[] = RajColor.values();
	   playerBoards= new PlayerBoard[players_in_game];
	   for(int i=0;i<players_in_game;i++)
	   		{	RajColor color = colors[map[i]];
	   			playerBoards[i] = new PlayerBoard(r,i,color); 
	   		}
	   
	   // spread the colors around so the humans have a bunch of cells in a zone near
	   // their player goodies to choose from.  It should look natural.
	   boolean progress = false;
	   int pass = 0;
	   do {	// spread the colors around
		   progress = false;
		   pass++;
		   for(RajCell c =allCells; c!=null; c=c.next)
		   {   RajColor color = c.color;
		   	   if((color!=null) && (c.sweep_counter!=pass))
		   		{
			   		for(int dir = 0;dir<CELL_FULL_TURN; dir++)
			   		{	RajCell d = c.exitTo(dir);
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
       resetState = RajState.PUZZLE_STATE;
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
    // more selecteive.  This determines if the current board digest is added
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

    public RajCell getCell(RajCell c)
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
    private RajCell getCell(RajId source, char col, int row)
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
    private void setNextStateAfterDrop(Rajmovespec m,replayMode replay)
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
        	{	if(!SIMULTANEOUS_PLAY) { setState(RajState.CONFIRM_STATE); }
        		else if((m.op==EPHEMERAL_DROPB)||(m.op==EPHEMERAL_MOVE_FROM_TO))
        		{
        		if(allDroppedCard())
        			{
        			setState(RajState.CONFIRM_CARD_STATE);	// all go to confirm
        			}
        		}
        		else {	setState(RajState.CONFIRM_STATE); 	// using synchronous moves, goto confirm state
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
    	RajCell to = pb.droppedCard;
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
    		setState(RajState.PLAY_STATE);
    		break;
    	case GAMEOVER_STATE: 
    		break;
    	case CONFIRM_STATE:
    		setState(RajState.PLAY_STATE);
			//$FALL-THROUGH$
		case CONFIRM_CARD_STATE:
    		if(allCommittedCard()) 
    			{	award_prize_timer = 0; 
    				setState(RajState.AWARD_PRIZE_STATE); 
    			}
    		break;
    	case PUZZLE_STATE:
    		setState((gameOverNow()||(prizes.height()==0))?RajState.GAMEOVER_STATE:RajState.SELECT_PRIZE_STATE);
    		break;
    	}
       	resetState = board_state;
    }
    public boolean gameOverNow()
    	{ return((prizes.height()==0) && (currentPrize==null));
    	}

    private void doDone(replayMode replay)
    {
        if (board_state==RajState.RESIGN_STATE)
        {	win[whoseTurn] = false;		// this will only be allowed in 2 player games
        	win[(whoseTurn+1)%players_in_game] = true;
            setState(RajState.GAMEOVER_STATE);
        }
        else
        {	if(gameOverNow())
        		{ 
        		  setState(RajState.GAMEOVER_STATE); 
        		}
        	else {playerBoards[whoseTurn].setCommittedCard();
        		  setNextPlayer();
        		  setNextStateAfterDone(replay);
        	}
        }
    }
    int playerOwning(RajColor c)
    {	RajColor[] colors = RajColor.values();
    	int map[] = getColorMap();
    	for(int i=0;i<colors.length;i++) { if(colors[map[i]]==c) { return(i); }}
    	throw G.Error("Not player owns "+c);
    }
    // actually award the prize
    public String awardPrize(replayMode mode)
    {	ChipStack chips  = new ChipStack();
    	int tileTotal = 0;
     	for(RajCell c = allCells; c!=null; c=c.next)
    	{	RajChip top = c.topChip();
    		if(top!=null)
    		{
    		if(top.isPrize())
        		{	// total prize value
        			tileTotal += c.totalPrizeValue();
         		}
    		else
    		{
    		// in the rare case that players manage to place 2 cards on the same square
    		// be sure to collect all of them 
    		while((top=c.topChip())!=null)
    		{	G.Assert(top.isCard(),"must be a card");
    			PlayerBoard dest = playerBoards[playerOwning(top.cardColor())];
    			chips.push(top); 
    			c.removeTop();
    			dest.playedCards.addChip(top);
    			if(robotStack!=null)
    			{
    				robotStack.push(c);
    				robotStack.push(dest.playedCards);
    			}
    			if(mode!=replayMode.Replay)
    			{	
    				animationStack.push(c);
    				animationStack.push(dest.playedCards);
    			}
    		}}
    		
    	}}
    	// sort the cards by value lowest first if the prize is negative or zero
    	G.Assert(chips.size()==players_in_game,"one card per player");
    	RajChip cards[] = chips.toArray();
    	Sort.sort(cards,0,cards.length-1,tileTotal>=0);
    	
    	int winner = 0;
    	int skipValue = 0;
    	String prizeString = "Tied, No Winner";
    	while(winner<cards.length)
    	{	int cardValue = cards[winner].cardValue();
    		if((cardValue!=skipValue)					// not a duplicate value we're skipping 
    				&& (((winner+1)==cards.length) 		// end of the line
    						|| (cardValue!=cards[winner+1].cardValue())))	// or a different value is next
    		{ // we have a winner
    		RajChip winningCard = cards[winner];
    		PlayerBoard winningPlayer = playerBoards[playerOwning(winningCard.cardColor())];
    		if(mode!=replayMode.Replay) { prizeString = "Won by "+winningPlayer.color+" : "; }
    		for(RajCell c = allCells; c!=null; c=c.next)
    			{
    			// move all the prizes to the player's stack
    			RajChip prize = c.topChip();
    			if((prize!=null)&&(prize.isPrize()))
    			{	
    				while(c.topChip()!=null) 
    					{ RajChip prize0 = c.removeTop();
    					  winningPlayer.wonPrizes.addChip(prize0);
    					  if(robotStack!=null)
    					  {
    						  robotStack.push(c);
    						  robotStack.push(winningPlayer.wonPrizes);
    					  }
    					  if(mode!=replayMode.Replay)
    					  {
    						  animationStack.push(c);
    						  animationStack.push(winningPlayer.wonPrizes);
    						  prizeString += " "+ prize0.prizeValue();
    					  }}
    			}
    			}
    		 winner = cards.length;	// get us out
    		}
    	else
    		{	// a tie, we have to skip all the tied players
    		skipValue = cardValue;
    		winner++;
    		}
    	}
    	acceptPlacement();
    	// if we drop through here, all the high cards are duplicates and there
    	// is no winner.  The cards get removed, and the prizes remain
    	return(prizeString);
    }
    public String unPickMove()
    {	
    	return(playerBoards[myIndex].unPickMove());
    }
    public String unDropMove()
    {	return(playerBoards[myIndex].unDropMove());
     }
    public boolean Execute(commonMove mm,replayMode replay)
    {	Rajmovespec m = (Rajmovespec)mm;
        
        if(replay!=replayMode.Replay) { animationStack.clear(); }

        switch (m.op)
        {

        case MOVE_DONE:
        	
         	doDone(replay);
           break;

        case MOVE_AWARD:
	        String pstring = awardPrize(replay);
	        if(replay!=replayMode.Replay)
	        	{m.setLineBreak(true);
	        	 m.moveInfo = pstring;
	        	}
	        setState((gameOverNow()||(prizes.height()==0))?RajState.GAMEOVER_STATE:RajState.SELECT_PRIZE_STATE);
	        break;
	        
        case EPHEMERAL_DROPB:
        	G.Assert(SIMULTANEOUS_PLAY,"shouldnt' be used");
			//$FALL-THROUGH$
		case MOVE_DROPB:
			if(board_state==RajState.PUZZLE_STATE)
			{
			for(PlayerBoard pb : playerBoards)
				{
				if(pb.pickedObject!=null)
				{
					pb.doDropb(m,replay);
					break;
				}}
			}
			else
			{
        	playerBoards[m.player].doDropb(m,replay);
			}
            break;

		case EPHEMERAL_UNMOVE:
			{
			PlayerBoard pl = playerBoards[m.player]; 
        	pl.doPickb(m,replay);
        	pl.dropObject(pl.cards,pl.pickedHeight); 
 			}
        	break;
        case EPHEMERAL_PICK:
        case EPHEMERAL_PICKB:
        	G.Assert(SIMULTANEOUS_PLAY,"shouldnt' be used");
			//$FALL-THROUGH$
		case MOVE_PICK:
			playerBoards[m.player].doPickb(m,replay);
			break;
		case MOVE_PICKB:
        	{
        	RajCell dest = getCell(m.source,m.from_col,m.from_row);
        	RajChip top = dest.topChip();
        	if(top.isCard()) {
            	PlayerBoard pl = playerBoards[playerOwning(top.cardColor())];
            	pl.doPickb(m, replay);
        	}
        	else {
        		playerBoards[m.player].doPickb(m,replay);
        	}
        	}
            break;

        case EPHEMERAL_DROP:
        case MOVE_DROP: // drop on chip pool;
        	playerBoards[m.player].doDrop(m,replay);

            break;
        case MOVE_CMOVE:
        	// undo the ephemeral moves
        	{
        	PlayerBoard pb = playerBoards[m.player];
        	RajCell dest = getCell(RajId.BoardLocation,m.to_col,m.to_row);
        	RajCell src = getCell(m.source,m.from_col,m.from_row);
        	if(dest.topChip()!=null)
        		{ pb.pickObject(dest,m.to_row);
        		  pb.dropObject(src,m.from_row);
        		}
        	pb.setCommittedCard(dest);
        	playerBoards[m.player].doMove(m,replayMode.Replay);	// no animation of the re-move
        	setState(RajState.CONFIRM_CARD_STATE);	
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
            setState(RajState.PUZZLE_STATE);	// standardize the current state
            setNextStateAfterDone(replay); 
            break;

       case MOVE_RESIGN:
            setState(unresign==null?RajState.RESIGN_STATE:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(RajState.PUZZLE_STATE);
 
            break;
        case MOVE_SELECT:
        	playerBoards[m.player].doSelect(m,replay);
        	acceptPlacement();
        	break;

        case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(RajState.GAMEOVER_STATE);
			break;

        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);
        //G.print(m + " "+Digest());
        return (true);
    }
    public boolean LegalToHitCards(RajCell c)
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
	    	if(c.rackLocation!=RajId.PlayerCards) { return(false); }
	    	PlayerBoard pb = playerBoards[c.col-'A'];
	    	PlayerBoard pp = currentPlayerBoard();
	    	RajChip po = pb.pickedObject;
	    	if(po!=null && po.isCard()) { return(pb==pp); }
	    	if(pp.pickedObject!=null) { return(false); }
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
	    	PlayerBoard pb = currentPlayerBoard();
	    	RajChip pickedObject = pb.pickedObject;
	        return ((pickedObject==null)
	                 ?true
	                 :(pickedObject.isCard()
	                		 &&(c.col==pb.cards.col)));    
	    	}}
    }
    // legal to hit the chip storage area
    public boolean LegalToHitPrizes(RajCell pool)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case SELECT_PRIZE_STATE:
        case AWARD_PRIZE_STATE:
        case CONFIRM_STATE:
		case CONFIRM_CARD_STATE:
        case PLAY_STATE:
        case RESIGN_STATE:
		case GAMEOVER_STATE:
			return(false);
        case PUZZLE_STATE:
        	{
        	RajChip pickedObject = currentPlayerBoard().pickedObject;
            return (pickedObject==null?(pool.topChip()!=null):pickedObject.isPrize());
        	}
        }
    }

    public boolean LegalToHitBoard(RajCell c,boolean any)
    {	if(c==null) { return(false); }
    	RajChip pickedObject = currentPlayerBoard().pickedObject;
        switch (board_state)
        {
		case CONFIRM_CARD_STATE:
		case PLAY_STATE:
			if(pickedObject==null)
			{	boolean v = any ? (c.topChip()!=null) : currentPlayerBoard().isDestCell(c);
				return(v);
			}
			else 
			{	// each cell has an invisible "color" which determines who owns the cell for
				// purposes of dropping cards.  This keeps the players from stomping on each
				// other's cards.
				return((c.topChip()==null));
			}
			
		case GAMEOVER_STATE:
		case AWARD_PRIZE_STATE:
		case SELECT_PRIZE_STATE:
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
    
   StateStack robotState = new StateStack();
   IStack robotUndo = new IStack();
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Rajmovespec m)
    {	robotState.push( board_state);
        robotUndo.push(robotStack.size()*100); //record the starting state. The most reliable
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
    	if(board_state==RajState.PLAY_STATE)
    		{ for(PlayerBoard pl : playerBoards) { pl.doReset(); }
    		}
    }

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Rajmovespec m)
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
        	RajCell src = currentPrize;
        	RajCell dest = prizes;
        	playerBoards[m.player].pickObject(src,-1);
        	playerBoards[m.player].dropObject(dest,m.from_row);
        	acceptPlacement();
        	}
        	break;
        case EPHEMERAL_MOVE_FROM_TO:
    		{
        	RajCell src = getCell(m.source,m.from_col,m.from_row);
        	RajCell dest = getCell(RajId.EmptyBoard,m.to_col,m.to_row);
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

        setState(robotState.pop());
        int undo = robotUndo.pop();
        int size =undo/100;
        while(robotStack.size()>size)
        {
        		RajCell dest = robotStack.pop();
        		RajCell src = robotStack.pop();
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
	 RajCell cards = bd.cards;
	 RajCell location = goodLocation(bd.color,cards.centerX(),cards.centerY());
	 for(int lim = cards.height()-1; lim>=0; lim--)
	 {
		 all.push(new Rajmovespec(EPHEMERAL_MOVE_FROM_TO,cards.col,lim,location.col,location.row,whoseTurn));
	 }
 }
 // in the real game we just take the top of the stack, but for the robot
 // we need to randomize the order of the stack it evaluates differently
 // from that which will actually come up.
 void addSelectPrizeMoves(CommonMoveStack  all,int who)
 {
	 for(int lim = prizes.height()-1; lim>=0; lim--)
	 {
		 all.push(new Rajmovespec(MOVE_SELECT,lim,whoseTurn));
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
 		all.push(new Rajmovespec(MOVE_AWARD,whoseTurn));
 		break;
 	case SELECT_PRIZE_STATE:
 		addSelectPrizeMoves(all,whoseTurn);
 		break;
 	case CONFIRM_CARD_STATE:
 		all.push(new Rajmovespec(cardMove(),whoseTurn));
 		break;
 	default:
 		throw G.Error("Not implemented");
 	}
 	return(all);
 }
 


}
