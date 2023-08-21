package mogul;

import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;

import static mogul.MogulMovespec.*;
/**
 * MogulBoard knows all about the game of Mogul
 * It gets a lot of logistic support from 
 * common.trackBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
 *  
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of tripwires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

class MogulBoard extends trackBoard<MogulCell> implements BoardProtocol,MogulConstants
{
	// map a track around the board with 10 across and 6 down
	private MogulState board_state = MogulState.Puzzle;	
	private MogulState unresign = null;	// remembers the original state when "resign" is hit
	public MogulState getState() { return(board_state); }
	private MogulViewer canvas=null;
	private IStack robotUndoStack = new IStack();
	private StateStack robotState = new StateStack();
	public int robotDepth = 0;
	MogulMovespec terminatedWithPrejudice = null;
	private MogulState terminatedWithPrejudiceState = null;

    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(MogulState st) 
	{ 	unresign = (st==MogulState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
	class MogulPlayer
	{	MogulCell chips = null;
		MogulCell cards[] = new MogulCell[MogulChip.nColors];
		boolean hasTakenLoan = false;
		boolean hasTakenMoney = false;
		private int vp = 0;
		public boolean hide = false;
		int myIndex = 0;
		MogulPlayer(Random r,int ind)
		{	myIndex = ind; 
			chips = new MogulCell(r,MogulId.HitChips,(char)('A'+ind),0);
			for(int i=0;i<cards.length;i++) 
				{ cards[i]=new MogulCell(r,MogulId.HitCards,(char)('A'+ind),i); 
				}
		}
		public void addCard(MogulChip ch)
		{	cards[ch.getBackgroundIndex()].addChip(ch);
		}
		
		// get the cell where this player will store this card
		public MogulCell getCardBackgroundCell(MogulChip ch)
		{
			return(cards[ch.getBackgroundIndex()]);
		}		// get the cell where this player will store this card
		public MogulCell getCardBorderCell(MogulChip ch)
		{
			return(cards[ch.getBorderIndex()]);
		}
		public int cardsOfBackgroundColor(MogulChip ch)
		{	if(ch==null) { return(0); }
			return(cards[ch.getBackgroundIndex()].height());
		}
		public int cardsOfBorderColor(MogulChip ch)
		{	if(ch==null) { return(0); }
			int bi = ch.getBorderIndex();
			if(bi<0) { return(0); }
			return(cards[bi].height());
		}
		public int getPoints() { return(vp); }
		public void givePoints(int n)
		{	vp += n;
		}
		public long Digest(Random r)
		{	
			long v = (long)vp*r.nextLong();
			v ^= MogulBoard.this.Digest(r,cards);
			v ^= MogulBoard.this.Digest(r,chips);
			v ^= (hasTakenLoan ? 0x056806023 : 0xf120045) | (hasTakenMoney ? 0x98932985 : 0x7063467e);
			return(v);
		}
		
		public void copyFrom(MogulPlayer other)
		{	vp = other.vp;
			hasTakenLoan = other.hasTakenLoan;
			hasTakenMoney = other.hasTakenMoney;
			MogulBoard.this.copyFrom(cards,other.cards);
			MogulBoard.this.copyFrom(chips,other.chips);
		}
		public void samePlayer(MogulPlayer other)
		{	G.Assert(sameCells(chips,other.chips),"chips mismatch");
			G.Assert(sameCells(cards,other.cards),"cards mismatch");
			G.Assert(vp==other.vp,"vp mismatch");
			G.Assert(hasTakenMoney==other.hasTakenMoney,"TakenMoney mismatch");
			G.Assert(myIndex==other.myIndex,"index mismatch");
			G.Assert(hasTakenLoan==other.hasTakenLoan,"loan mismatch");
		
		}
		// return final victory points for player, offset & 100 + shares as a tiebreaker
		int finalVP()
		{	return((vp + chips.height()/5)*100 + totalShares());
		}
		int trueScore(boolean gameover)
		{
			return((gameover?chips.height()/5:0)+vp);
		}
		int totalShares()
		{	int tot = 0;
			for(MogulCell card : cards) { tot += card.height(); }
			return(tot);
		}
	}

	public double simpleEvaluation(int pl,boolean print)
	{	return(players[pl].finalVP());
	}
	public int trueScore(int pl)
	{
		return(players[pl].trueScore(board_state==MogulState.Gameover));
	}
	public int scoreForPlayer(int pl)
	{
		MogulPlayer p = players[pl];
		return(p.trueScore((board_state==MogulState.Gameover)));
	}
	private MogulPlayer players[] = null;
	public MogulPlayer getPlayer(int n)
	{	MogulPlayer pp[] = players;
		if(pp!=null && n>=0 && n<pp.length) { return(pp[n]) ; }
		return(null);
	}
	public MogulCell bank = null;
	public MogulCell deck = null;
	public MogulCell discards = null;
	public MogulCell auction = null;
	public MogulCell auctionCopy = null;	// same as auction, except it doesn't get taken during play
	public MogulCell pot = null;
	public MogulCell tempCardCell = null;
	private CellStack robotStack=new CellStack();
	private Random robotRandom = null;
	void setRobotRandom(Random r) { robotRandom = r; }
	private String setupString = null;
	public String setupString() { return(setupString);}
	public int secondPlayer = -1;
	public int startPlayer = -1;
	private int defaultWhoseTurn = FIRST_PLAYER_INDEX;
	private int cardSaleValue = -1;
	public void SetDrawState() { throw G.Error("shouldn't happen"); }

	public CellStack animationStack = new CellStack();

   // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public MogulChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
    
	// factory method
	public MogulCell newcell(char c,int r)
	{	return(new MogulCell(c,r));
	}
    public MogulBoard(MogulViewer can,String init,long rv,int np,int map[]) // default constructor
    {   canvas = can;
    	setColorMap(map, np);
        doInit(init,rv,np); // do the initialization 
     }

	public MogulCell getCell(int spot)
	{	int loc = spot%TRACK_AROUND;
		while(loc<=0) { loc += TRACK_AROUND; }	// handle negative scores.  We only expect small negatives.
		return(getCell('A',loc));
	}
	public void sameboard(BoardProtocol f) { sameboard((MogulBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(MogulBoard from_b)
    {
        // note that special logic in MogulCell.sameContents makes board cells sensitive 
        // only to the height, This is needed to avoid something really complicated
        // in the robot unmakemove when undoing vp changes.
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(secondPlayer==from_b.secondPlayer,"second player mismatch");
    	G.Assert(startPlayer==from_b.startPlayer,"start player mismatch");
    	G.Assert(cardSaleValue==from_b.cardSaleValue,"Card sale value mismatch");
    	G.Assert(sameCells(bank,from_b.bank),"bank mismatch");
    	G.Assert(sameCells(pot,from_b.pot),"pot mismatch");
    	G.Assert(sameCells(deck,from_b.deck),"deck mismatch");
    	G.Assert(sameCells(auction,from_b.auction),"auction mismatch");
    	G.Assert(sameCells(auctionCopy,from_b.auctionCopy),"auctionCopy mismatch");
    	G.Assert(sameCells(discards,from_b.discards),"discards mismatch");
    	for(int i=0;i<players.length;i++) { players[i].samePlayer(from_b.players[i]); }
    	
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
		G.Assert(unresign==from_b.unresign,"unresign mismatch");
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

        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        
        // note that special logic in MogulCell.Digest makes board cells sensitive 
        // only to the height, This is needed to avoid something really complicated
        // in the robot unmakemove when undoing vp changes.
        long v = super.Digest(r);

    	v ^= Digest(r,bank);
    	v ^= Digest(r,pot);
    	v ^= Digest(r,deck);
    	v ^= Digest(r,auction);
    	v ^= Digest(r,auctionCopy);
    	v ^= Digest(r,discards);
    	v ^= (secondPlayer+1)*r.nextLong();
    	v ^= (startPlayer+1)*r.nextLong();
    	v ^= cardSaleValue*r.nextLong();
    	for(int i=0;i<players.length;i++) { v ^=players[i].Digest(r); }
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
		v ^= r.nextLong()*moveNumber;	// unusual, but there are no "done" states
		
        return (v);
    }
   public MogulBoard cloneBoard() 
	{ MogulBoard copy = new MogulBoard(canvas,gametype,randomKey,players_in_game,getColorMap());
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((MogulBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(MogulBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        robotDepth = from_b.robotDepth;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
         for(int i=0;i<players.length;i++) { players[i].copyFrom(from_b.players[i]); }
        secondPlayer = from_b.secondPlayer;
        startPlayer = from_b.startPlayer;
        defaultWhoseTurn = from_b.defaultWhoseTurn;
        terminatedWithPrejudice = from_b.terminatedWithPrejudice;
        cardSaleValue = from_b.cardSaleValue;
    	copyFrom(bank,from_b.bank);
    	copyFrom(pot,from_b.pot);
    	copyFrom(deck,from_b.deck);
    	copyFrom(auction,from_b.auction);
    	copyFrom(auctionCopy,from_b.auctionCopy);
    	copyFrom(discards,from_b.discards);
    	unresign = from_b.unresign;
    	robotRandom = from_b.robotRandom;
    	if(robotRandom!=null) 
    		{ robotRandom = new Random(robotRandom.nextLong());
    		}
    	board_state = from_b.board_state;
        sameboard(from_b);
    }
    // reshuffle the deck, this is used by the robot
    // to avoid using the actual deck as it's "random" deck.
    public void reShuffle(Random r,MogulCell temp)
    {	if(temp.height()>1)
    	{
    	//G.print("Reshuffle "+temp.height());
    	MogulChip crash = temp.removeChip(MogulChip.crashCard);
    	temp.shuffle(r);
    	temp.insertChipAtIndex(Random.nextInt(r,Math.min(temp.height(),5)),crash);	// crash card at position 0-4
    	}
    }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game);
    }
    public void doInit(String gtype,long rv,int np)
    {
    	doInit(gtype,rv,np,setupString);
    }
    public void replaySetupString(String setup)
    {
    	if(setup!=null)
    	{	
    		StringTokenizer tok = new StringTokenizer(setup);
    		startPlayer = G.IntToken(tok);
    		for(MogulPlayer p : players)
    		{	int next = G.IntToken(tok);
    			p.cards[0].addChip(MogulChip.getChip(next));
    		}
    		while(tok.hasMoreTokens())
    		{	int next = G.IntToken(tok);
    			deck.addChip(MogulChip.getChip(next));
    		}
    	}

    }
    private String makeSetupString()
    {	StringBuffer str = new StringBuffer();
    	str.append(startPlayer);
    	
    	for(MogulPlayer p : players)
    	{	int chip = p.cards[0].topChip().chipNumber();
    		str.append(" ");
    		str.append(chip);
    	}
    	for(int i=0,lim=deck.height(); i<lim; i++)
    	{	int chip = deck.chipAtIndex(i).chipNumber();
    		str.append(" ");
    		str.append(chip);
    	}
      	return(str.toString());
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int np,String setup)
    {  	
     	{
         	if(Mogul_INIT.equalsIgnoreCase(gtype)) 
         		{ 
         		}
         	else { throw G.Error(WrongInitError,gtype); }
         	gametype = gtype;
     	}
        win = new boolean[np];	
    	drawing_style = DrawingStyle.STYLE_NOTHING; // STYLE_CELL or STYLE_LINES
    	Grid_Style = MOGULGRIDSTYLE; //coordinates left and bottom
    	randomKey = rv;
    	terminatedWithPrejudice = null;
    	players_in_game = np;
    	win = new boolean[np];
    	players = new MogulPlayer[np];
    	Random r = new Random(6883463);
    	randomKey = rv;
  
    	pot = new MogulCell(r,MogulId.HitPot);
    	bank = new MogulCell(r,MogulId.HitBank);
    	auction = new MogulCell(r,MogulId.HitAuction);
    	auctionCopy = new MogulCell(r,MogulId.HitAuction);
    	deck = new MogulCell(r,MogulId.HitDeck);
    	tempCardCell = new MogulCell(r,MogulId.HitDeck);
    	discards = new MogulCell(r,MogulId.HitDiscards);
    	robotUndoStack.clear();
    	robotState.clear();
    	robotDepth = 0;
	    initBoard(TRACK_ACROSS,TRACK_DOWN); //this sets up a dummy board for the scoring track
  	
	    for(int i=0;i<N_CHIPS;i++) { bank.addChip(MogulChip.pokerChip); }
	    int map[]=getColorMap();
    	for(int i=0;i<np;i++) 
    		{ MogulPlayer p = players[i] = new MogulPlayer(r,i);
    		  getCell(0).addChip(MogulChip.getPlayerChip(map[i]));
    		  MogulCell ch = p.chips;
    	    		  for(int j=0;j<CHIPS_PER_PLAYER;j++) 
    	    		  { ch.addChip(bank.removeTop()); 
    	    		  }
    		}

	    setState(MogulState.Puzzle);
	    
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		pickedObject = null;
        allCells.setDigestChain(r);
        AR.setValue(win,false);
        moveNumber = 1;
        cardSaleValue = -1;
        secondPlayer = -1;

	    //
        // new games are initialized based on the random key
        // replayed games are initialized based on the saved sequence from when it was new.
        //
	    if(setup==null)
	    {
	    // deal the cards in a prescribed way.
	    MogulCell tempDeck = new MogulCell(r,MogulId.HitDeck);
	    Random deckRandom = new Random(randomKey);
	    // shuffle the starting cards
	    for(int i : MogulChip.startingCards) 
	    {	MogulChip ch = MogulChip.getCard(i);
	    	tempDeck.addChip(ch);
	    }
	    tempDeck.shuffle(deckRandom);
	    // give each player a starting card
	    for(MogulPlayer player : players) { player.addCard(tempDeck.removeTop()); }
	    // add the rest of the cards to the deck except for the crash card
	    for(int i=0; i<MogulChip.nCards; i++) 
	    {	MogulChip ch = MogulChip.getCard(i);
	    	if(!ch.isStartingCard()) { tempDeck.addChip(ch); }
	    }
	    tempDeck.shuffle(deckRandom);
	    // deal 4 off the deck, add the crash card
	    for(int i=0;i<4;i++) { deck.addChip(tempDeck.removeTop()); }
	    deck.addChip(MogulChip.crashCard);
	    // shuffle 4 cards with the crash card
	    deck.shuffle(deckRandom);
	    // add the rest of the deck.
	    while(tempDeck.height()>0) { deck.addChip(tempDeck.removeTop()); }
        defaultWhoseTurn = startPlayer = Random.nextInt(deckRandom,players_in_game);
	    setupString = makeSetupString();
	    }
	    else
	    {	setupString = setup;
	    	replaySetupString(setup);
	    }
    }

    private int cardSaleValue()
    {
    	int n = 0;
    	MogulChip  target = auctionCopy.topChip();
    	for(MogulPlayer p : players)
    	{	n += p.cardsOfBorderColor(target);
    	}
    	return(n);
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
        case Puzzle:
            break;
        case Confirm:
        case Resign:
        case Play:
            moveNumber++; //the move is complete in these states
            int current = whoseTurn;
            int next = current;
            // skip players who have withdrawn from the auction
            do { next = (next +1) % players_in_game;
            } while(players[next].hasTakenMoney);
            int nextNext = next;
            // find the next next player, see if it's the same
            do { nextNext = (nextNext+1) % players_in_game; 
            } while( players[nextNext].hasTakenMoney && (nextNext!=next));
            if(nextNext == next) 
            {
            	// we're the last player to drop out of the auction
            	secondPlayer = current;
             	setState((board_state==MogulState.WonAuction)
             				?MogulState.SellCard
             				:MogulState.WonAuction);
            }
            whoseTurn = next;
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	
        switch (board_state)
        {case Resign:
         case Confirm:
            return (true);

        default:
            return (false);
        }
    }


    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==MogulState.Gameover) { return(win[player]); }
    	throw G.Error("not implemented");
    }
    // estimate the value of the board position.
    public double ScoreForPlayer(int player,boolean print,double cup_weight,double ml_weight,boolean dumbot)
    {  
    	throw G.Error("not implemented");
    }


    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        droppedDestStack.clear();
        pickedSourceStack.clear();
     }

    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	MogulChip po = pickedObject;
    	if(po!=null)
    	{
    		MogulCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case HitCards:	// player cards
    		case HitChips:	// player chips
    		case HitDeck:
    		case HitBank:
    		case HitAuction:
    		case HitDiscards:
    		case BoardLocation: ps.addChip(po); break;
   		}
    		pickedObject = null;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(MogulCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 	    		
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case HitCards:	// player cards
		case HitChips:	// player chips
		case HitDeck:
		case HitBank:
        case HitPot:
		case HitAuction:
		case HitDiscards:
		case BoardLocation: c.addChip(pickedObject); break;
		}
    	pickedObject = null;
       	droppedDestStack.push(c);
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(MogulCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    // Caution! This method is called in the mouse process
    public int movingObjectIndex()
    {	MogulChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.chipNumber());
    		}
        return (NothingMoving);
    }
    
    public MogulCell getCell(MogulId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case HitChips:	// player chips
        	return(players[col-'A'].chips);
        case HitCards:
        	return(players[col-'A'].cards[row]);
        case HitBank:
        	return(bank);
        case HitDeck:
        	return(deck);
        case HitPot:
        	return(pot);
        case HitAuction:
        	return(auction);
        case HitDiscards:
        	return(discards);
            
        case BoardLocation:
        	return(getCell(col,row));
       }
    }
    public MogulCell getCell(MogulCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(MogulCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case HitCards:	// player cards
		case HitChips:	// player chips
		case HitDeck:
        case HitPot:
		case HitBank:
		case HitAuction:
		case HitDiscards:
		case BoardLocation: 
			pickedObject = c.removeTop(); 
			break;
   	
    	}
    	pickedSourceStack.push(c);
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);

        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(MogulCell c)
    {	return(getSource()==c);
    }
    public MogulCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }


    private void givePoints(MogulPlayer p,int n,replayMode replay)
    {
    	int oldPoints = p.getPoints();
    	p.givePoints(n);
    	MogulCell oldCell = getCell(oldPoints);
    	MogulCell newCell = getCell(oldPoints+n);
    	MogulChip chip = MogulChip.getPlayerChip(getColorMap()[p.myIndex]);
    	oldCell.removeChip(chip);
    	newCell.addChip(chip);
    	if(replay!=replayMode.Replay)
    	{
    		animationStack.push(oldCell);
    		animationStack.push(newCell);
    	}
    }
    
    // pay the bonus for cards matching the new auction card
    private void payBonus(replayMode replay,boolean unpay)
    {	MogulChip auctionChip = auction.topChip();
    	if(auctionChip!=MogulChip.crashCard)
    	{
    	for(MogulPlayer p : players) 
    		{ 
    		  givePoints(p,(p.cardsOfBackgroundColor(auctionChip)*(unpay?-1:1)),replay);
    		}
    	}
    }
    private void setGameOver()
    {	setState(MogulState.Gameover);
    	int max = 0;
    	boolean dup = false;
    	int best = -1;
    	for(MogulPlayer p : players)
    	{	//  move the player markers 1 point per 5 chips
    		int sc = p.finalVP();
    		int vp = p.vp;
    		int vpp = p.trueScore(true);
    		if(vpp!=vp)
    		{
    			getCell(vpp).addChip(getCell(vp).removeChip(MogulChip.getPlayerChip(getColorMap()[p.myIndex])));
    		}
    		if(sc>max) { max = sc; dup = false; best = p.myIndex; }
    		else if (sc==max) { dup = true; }
    		win[p.myIndex]=false;
    	}
    	if(!dup) { win[best] = true; }
    }
    private void startRound(replayMode replay)
    {
    	if(auction.topChip()==null) { auction.addChip(deck.removeTop()); }
    	auctionCopy.copyFrom(auction);
    	if(auction.topChip()==MogulChip.crashCard) 
    	{
    		setGameOver();
    	}
    	else 
    		{ setState(MogulState.Play); 
    		  payBonus(replay,false);
    		  if(pot.height()==0)
    		  {
    		  setWhoseTurn(startPlayer);
    		  defaultWhoseTurn = startPlayer;
    		  for(MogulPlayer p : players) { p.hasTakenLoan = false; p.hasTakenMoney=false; }
    		  secondPlayer = -1;
    		  startPlayer = -1;
    		  cardSaleValue = cardSaleValue();
    		  }
    		}
    }
    public String auctionCardBackgroundColor()
    {
    	MogulChip auctionCard = auctionCopy.topChip();
    	if(auctionCard!=null)
    	{
    	return(auctionCard.getBackgroundColor());
    	}
    	return("none");
    	
    }
    public String auctionCardBorderColor()
    {	MogulChip auctionCard = auctionCopy.topChip();
    	if(auctionCard!=null)
    	{
    	return(auctionCard.getBorderColor());
    	}
    	return("none");
    }
    public int cardsAvailableForSale()
    {
    	MogulPlayer p = players[whoseTurn];
    	return(p.cardsOfBorderColor(auctionCopy.topChip()));
    }
    public void takeAuctionCard(MogulPlayer p,replayMode replay)
    {
    	MogulCell from = auction;
    	MogulChip top = auction.topChip();
    	startPlayer = p.myIndex;
    	if(top!=null)
    	{
    	MogulCell to = p.getCardBackgroundCell(top);
    	to.addChip(from.removeTop());
       	if(replay!=replayMode.Replay)
        	{
        	animationStack.push(from);
        	animationStack.push(to);
        	}
    	}
    }
    //
    // sell one card, award the points, move the player marker
    // if there are no more cards to sell, start the next round.
    // otherwise continue selling cards
    //
    public boolean sellOneCard(replayMode replay)
    {
    	MogulPlayer p = players[whoseTurn];
    	MogulCell from = p.getCardBorderCell(auctionCopy.topChip());
    	MogulCell to = discards;
    	to.addChip(from.removeTop());
    	givePoints(p,cardSaleValue,replay);
    	if(board_state==MogulState.WonAuction) { startPlayer = secondPlayer; }
      	if(replay!=replayMode.Replay)
	    	{
	    	animationStack.push(from);
	    	animationStack.push(to);
	    	}
    	if(from.height()==0) 
    		{  takeAuctionCard(players[startPlayer],replay);
    		   moveNumber++;
    		   startRound(replay); 
    		   return(false);
    		} 
    		else 
    		{ setState(MogulState.SellOneCard); 
    		  return(true);
    		}
    }
    int undoPot = 0;
    public boolean Execute(commonMove mm,replayMode replay)
    {	MogulMovespec m = (MogulMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn+ " " +Digest());
        switch (m.op)
        {
        case MOVE_SHUFFLE:
        	// for the robot only
        	{
        		MogulCell newdeck = new MogulCell(robotRandom,MogulId.HitDeck);
        		newdeck.copyFrom(deck);
        		robotStack.push(newdeck);
        		reShuffle(robotRandom,deck);
        	}
        	break;
	     default:
	        {	MogulId mop = MogulId.findOp(m.op);
	        	if(mop!=null)
	        		{
	        		switch(mop)
	        		{
	        		case HitChips:
	        		{
	        	MogulPlayer p = players[m.player];
	        	MogulCell from = p.chips;
	        	MogulCell to = pot;
	        	to.addChip(from.removeTop());
	        	if(replay!=replayMode.Replay)
		        	{
		        	animationStack.push(from);
		        	animationStack.push(to);
		        	}
	        	setNextPlayer();
	        	}
	        	break;
	        case HitBank:
	        	{
	        	MogulPlayer p = players[m.player];
	        	MogulCell from = bank;
	        	MogulCell to = p.chips;
	        	givePoints(p,-2,replay);				// 2 chips for 2 vp
	        	to.addChip(from.removeTop());
	        	to.addChip(from.removeTop());
	        	p.hasTakenLoan = true;
	           	if(replay!=replayMode.Replay)
		        	{
		        	animationStack.push(from);
		        	animationStack.push(to);
		        	}
	        	}
	        	break;
	        	
	        case HitPot:
	        	{
	        	MogulPlayer p = players[m.player];
	        	MogulCell from = pot;
	        	MogulCell to = p.chips;
	        	undoPot = from.height();		// record the number taken for undo
	        	while(from.height()>0) 
	        	{ to.addChip(from.removeTop()); 
		           	if(replay!=replayMode.Replay)
		        	{
		        	animationStack.push(from);
		        	animationStack.push(to);
		        	}
	        	}
	        	p.hasTakenMoney = true;
	        	setNextPlayer();
	        	}
	        	break;
	        	
	        case HitAuction:
	        	{
	        	MogulPlayer p = players[m.player];
	        	takeAuctionCard(p,replay);
	    		moveNumber++;
	    		setWhoseTurn(secondPlayer);
	    		secondPlayer = -1;
	        	if(cardsAvailableForSale()>0)
	        	{
	        	setState(MogulState.SellCard);
	        	}
	        	else 
	        		{
	        		startRound(replay); 
	        		}
	        	}
	        	break;
	        case HitSellCard:
	        	undoPot = 1;		// save for unexecute
	        	sellOneCard(replay);
	        	break;
	        	
	        case HitSellAll:
	        	undoPot = 1;		// save for unexecute
	         	while(sellOneCard(replay)) { undoPot++; };
		    	break;
	     	
	        case HitNoSale:
	        	{	MogulChip top = auction.topChip();
	        		if((top!=null)&&(secondPlayer>=0))
	        		{ 
	        		 takeAuctionCard(players[secondPlayer],replay);
	        		}
	        	 	moveNumber++;
	        		startRound(replay);
	        	}
	        	break;
	       	default: cantExecute(m);
	       		}}
	        		else
	        		{
	        			cantExecute(m);
	        		}
	        }
        		break;
        case MOVE_DROP: // drop on chip pool;
            dropObject(getCell(m.source, m.from_col, m.from_row));
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
        	{
        	MogulCell c = getCell(m.source, m.from_col, m.from_row);
            pickObject(c);
        	}
            break;


        case MOVE_START:
        	startPlayer = m.player;
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(MogulState.Puzzle);
            startRound(replay);
            break;
        case MOVE_GAMEOVERONTIME:
	     	   win[whoseTurn] = true;
	     	   setState(MogulState.Gameover);
	     	   break;

        case MOVE_RESIGN:
            setState(unresign==null?MogulState.Resign:unresign);
            break;
        case MOVE_EDIT:
     		acceptPlacement();
    		setState(MogulState.Puzzle);
 
            break;
            
        }

        return (true);
    }
    //
    // true if the the chip can place in the cell. 
    // all the cells are the same type, but each has a particular type of chip
    // it expects to hold.
    //
    private boolean compatibleSpot(MogulChip ch,MogulCell c)
    {
    	if(ch.isPlayerChip())
    	{
    		return(c.onBoard);
    	}
    	else if(ch.isPokerChip())
    	{
    		switch(c.rackLocation())
    		{
    		default: break;
    		case HitBank:
    		case HitPot:
    		case HitChips:
    			return(true);
    		}
    	}
    	else if(ch.isCard())
    	{
    		switch(c.rackLocation())
    		{
    		default: break;
    		case HitDeck:
    		case HitAuction:
    		case HitDiscards:
    		case HitCards:
    			return(true);
    		}
    		
    	}
    	return(false);
    }
 

    public boolean LegalToHitBoard(MogulCell cell)
    {	
        switch (board_state)
        {
        case SellCard:
        case SellOneCard:
        	switch(cell.rackLocation())
        	{
        	case HitCards:	return(cell==players[whoseTurn].getCardBorderCell(auctionCopy.topChip()));
        	default: break;
        	}
        	return(false);
        case WonAuction:
        	switch(cell.rackLocation())
        	{
        	case HitCards:		// selling cards
        		return(cell==players[whoseTurn].getCardBorderCell(auctionCopy.topChip()));
        	case HitAuction:	// taking the card
        		return(true);
			default:
				break;       		
        	}
        	return(false);
 		case Play:
 			switch(cell.rackLocation())
 			{
 			default: break;
 			case HitChips:	// player chip
 				return((cell.height()>0) && (cell.col-'A'==whoseTurn));
 			case HitPot:
 				return(true);
 			case HitBank:
 				return(bank.height()>=2 && !players[whoseTurn].hasTakenLoan);
 			}
			return(false);

		case Gameover:
			return(false);
		case Confirm:
			return(isDest(cell));
        default:
            throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return((pickedObject==null)
        			?(cell.topChip()!=null)
        			:compatibleSpot(pickedObject,cell));
        }
    }
   public void terminateWithPrejudice(MogulMovespec m)
   {   terminatedWithPrejudiceState = board_state;
	   terminatedWithPrejudice = m;
	   setState(MogulState.Gameover);
   }
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(MogulMovespec m)
    {	
    	MogulChip preAuction = auction.topChip();
    	MogulChip preAuctionCopy = auctionCopy.topChip();
    	robotDepth++;
    	int deckHeight = deck.height();
    	int money = 0;
    	for(MogulPlayer p : players) { money=money<<2; money |= p.hasTakenMoney ? 1 :0; money |= p.hasTakenLoan ? 2 : 0; }
    	robotUndoStack.push(money);
    	robotState.push(board_state);
        int undoInfo = ((secondPlayer+1)<<4)
        			| ((startPlayer+1)<<7)
        			| ((cardSaleValue+1)<<10)
        			| ((preAuctionCopy!=null)?(preAuctionCopy.chipNumber()<<14):0)
        			;
         
         // to undo state transistions is to simple put the original state back.
        //G.print("R "+m);
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        undoPot = 0;
        if (Execute(m,replayMode.Replay))
        {	if(deck.height()<deckHeight)
        		{ undoInfo |= (1<<22); 	// turned a card
        		}
        	if((preAuction!=null) && (preAuction!=auction.topChip()))
        	{	// gave away the card
			  undoInfo |= (((preAuction.getBackgroundIndex())+1)<<23);
        	}
        }
        robotUndoStack.push(undoInfo);
        robotUndoStack.push(undoPot);
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(MogulMovespec m)
    {	robotDepth--;
    	if(m==terminatedWithPrejudice)
    	{
    	 setState(terminatedWithPrejudiceState);
    	 terminatedWithPrejudice = null;
    	 terminatedWithPrejudiceState = null;
    	 return;
    	}
    	int undoPot = robotUndoStack.pop();
    	int state0 = robotUndoStack.pop();
    	int money = robotUndoStack.pop();
     	int new_second = ((state0>>4) & 0x7)-1;
     	int new_start = ((state0>>7) &7)-1;
    	int new_cardsale = ((state0>>10) &0xf)-1;
    	int new_auctioncopy = ((state0>>14)&0xff);
    	boolean turncard = ((state0>>22)&1)!=0;
    	int gaveCard = (state0>>23)&0xf;
    	//G.print("U "+m);
    	if(board_state==MogulState.Gameover)
    	{	for(MogulPlayer p:players)
    		{
    		int vp = p.vp;
    		int vpp = p.trueScore(true);
    		if(vpp!=vp)
    		{	// undo the final shift in marker position
    			getCell(vp).addChip(getCell(vpp).removeChip(MogulChip.getPlayerChip(getColorMap()[p.myIndex])));
    		}}
    	}
    	if(turncard)
    	{
    		payBonus(replayMode.Replay,true);	// un-pay the bonus
    		deck.addChip(auction.removeTop());
     	}
   		if(gaveCard>0)
		{
			MogulPlayer p = players[(m.op==MogulId.HitAuction.opcode())?m.player:new_second];
			MogulCell from = p.cards[gaveCard-1];
			MogulChip top = from.removeTop();
			auction.addChip(top);
		}
   		auctionCopy.reInit();
   		if(new_auctioncopy>0) { auctionCopy.addChip(MogulChip.getChip(new_auctioncopy)); }
   		
        switch (m.op)
        {
        case MOVE_SHUFFLE:
        	{	// special move for robot re-randomization
        		MogulCell newdeck = robotStack.pop();
        		deck.copyFrom(newdeck);
        	}
        	break;
        default:
        	{
        	MogulId mop = MogulId.findOp(m.op);
        	if(mop!=null)
        	{
        	switch(mop)
        	{
        	case HitSellAll:
        	case HitSellCard:
        	{
        	int nsold = undoPot;
        	MogulPlayer p = players[m.player];
        	MogulCell to = p.getCardBorderCell(auctionCopy.topChip());
        	MogulCell from = discards;
        	int payout = nsold*new_cardsale;
        	givePoints(p,-payout,replayMode.Replay);
        	while(nsold-- > 0) { to.addChip(from.removeTop()); }
        	}
        	break;
        	case HitAuction:
        	case HitNoSale:
        	// we might have given away the card, but that's already handled
        	break;
        	case HitChips:
        	{
        	MogulPlayer p = players[m.player];
        	MogulCell from = pot;
        	MogulCell to = p.chips;
        	to.addChip(from.removeTop());
        	}
        	break;
        	case HitPot:
        	{
        	MogulPlayer p = players[m.player];
         	MogulCell from = p.chips;
        	MogulCell to = pot;
        	int new_height = undoPot;
        	while(new_height-->0) { to.addChip(from.removeTop()); }  
        	}
        	break;
        	case HitBank:
        	{
        	MogulPlayer p = players[m.player];
        	givePoints(p,2,replayMode.Replay);
          	MogulCell from = p.chips;
        	MogulCell to = bank;
        	to.addChip(from.removeTop());
        	to.addChip(from.removeTop());
        	}
        	break;
			default:
				break;
        	}}
        	else {
        		cantUnExecute(m);    		
        	}
        	break;
        	}
        }
        
        startPlayer = new_start;
        secondPlayer = new_second;
        cardSaleValue = new_cardsale;
        win[FIRST_PLAYER_INDEX]=win[SECOND_PLAYER_INDEX]=false; 
        setState(robotState.pop());
        for(int nn=players.length-1; nn>=0; nn--)
        {	
        	MogulPlayer pl = players[nn];
        	pl.hasTakenMoney = (money&1)!=0;
        	pl.hasTakenLoan = (money&2)!=0;
        	money = money>>2;
        	
        }
        if(turncard || (whoseTurn!=m.player))
        {  	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	MogulPlayer p = players[whoseTurn];
 	switch(board_state)
 	{
 	default: throw G.Error("Not implemented");
 	case Play:
 		{
 		
 		all.addElement(new MogulMovespec(MogulId.HitPot.opcode(),whoseTurn));								// take the money
 		if(p.chips.height()>0) { all.addElement(new MogulMovespec(MogulId.HitChips.opcode(),whoseTurn)); }	// pay a chip
 		if(!p.hasTakenLoan && (bank.height()>2)) { all.addElement(new MogulMovespec(MogulId.HitBank.opcode(),whoseTurn)); }		// take a loan
 		}
 		break;
 	case WonAuction:
 		{
 		all.addElement(new MogulMovespec(MogulId.HitAuction.opcode(),whoseTurn));	// take the card
 		if(p.getCardBorderCell(auctionCopy.topChip()).height()>0) 
 			{ all.addElement(new MogulMovespec(MogulId.HitSellAll.opcode(),whoseTurn));	// sell the lot
 			  all.addElement(new MogulMovespec(MogulId.HitSellCard.opcode(),whoseTurn));	// sell one or more
 			}
 		all.addElement(new MogulMovespec(MogulId.HitNoSale.opcode(),whoseTurn)); 	// sell none
 		}
 		break;
 	case SellCard:
 		{	
 			if(p.getCardBorderCell(auctionCopy.topChip()).height()>0) 
 				{all.addElement(new MogulMovespec(MogulId.HitSellAll.opcode(),whoseTurn));	// sell one or more
 				}
 			all.addElement(new MogulMovespec(MogulId.HitNoSale.opcode(),whoseTurn)); 	// sell none
 		}
 		break;
 	case SellOneCard:
			if(p.getCardBorderCell(auctionCopy.topChip()).height()>0) 
			{all.addElement(new MogulMovespec(MogulId.HitSellCard.opcode(),whoseTurn));	// sell one or more
				}
			all.addElement(new MogulMovespec(MogulId.HitNoSale.opcode(),whoseTurn)); 	// sell none
 		
 		break;
 	}
  	return(all);
 }
 
}
