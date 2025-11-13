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
package imagine;

import static imagine.Imaginemovespec.*;
import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;

class PlayerBoardStack extends OStack<PlayerBoard> implements Digestable
{
	public PlayerBoard[] newComponentArray(int sz) {
		return new PlayerBoard[sz];
	}
	public void getPb(PlayerBoardStack from,PlayerBoard[]pbs)
	{
		clear();
		for(int i=0;i<from.size();i++)
		{
			push(pbs[from.elementAt(i).boardIndex]);
		}
	}
	public long Digest(Random r) {
		long v = 0;
		for(int i=0;i<size();i++) 
			{ PlayerBoard pb = elementAt(i);
			  v ^= r.nextLong()*(pb.boardIndex+24526);
			}
		return(v);
	}
	
}
class PlayerBoard implements ImagineConstants,Digestable
{	
    static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

	ImagineBoard parent ;
	Colors color;
	int boardIndex;
	int score = 0;
	public String toString() { return "<playerBoard "+boardIndex+">"; }
	public boolean isSelected(ImagineCell c) { return(selectedCard==c); }
	public ImagineCell cards[] = new ImagineCell[CARDS_PER_PLAYER];
	public ImagineCell[] getCards() { return(cards); }

	// cells that are part of the user interface
	String story = null;
	ImagineCell selectedCard = null;
	int stake = 1;
	boolean stakeSet;
	int bet = 1;
	boolean betSet;
	boolean ready = false;
	long uid = 0;
	int selectedPresentation=-1;
	
	PlayerBoard(ImagineBoard p,Colors co,int boardi)
	{	color = co;
		uid = new Random(boardi+10104).nextLong();
		parent = p;
		boardIndex = boardi;
		for(int idx=0;idx<cards.length;idx++) { cards[idx] = new ImagineCell(ImagineId.Card,(char)('A'+boardIndex),idx); }
	}
	void doInit()
	{	selectedCard = null;
		selectedPresentation = -1;
		story = null;
		stake = 1;
		stakeSet = false;
		betSet = false;
		ready = false;
		bet = 1;
		score = 0;
		ImagineCell.reInit(cards);
	}
	
	public void newStoryTeller()
	{	
		stake = -1;
		stakeSet = false;
		bet = -1;
		betSet = false;
		selectedCard = null;
		ready = false;
		selectedPresentation = -1;
		story = null;
	}
	public void prepareToSelect()
	{
		selectedCard = null;
		stake = -1;
		stakeSet = false;
	}
	public void prepareToVote()
	{
		bet = -1;
		betSet = false;
		ready = false;
		selectedPresentation = -1;
	}
	public void prepareToAppreciate()
	{
		ready = false;
	}
	void copyFrom(PlayerBoard p)
	{	score = p.score;
		stake = p.stake;
		uid = p.uid;
		stakeSet = p.stakeSet;
		bet = p.bet;
		betSet = p.betSet;
		ready = p.ready;
		selectedCard = parent.getCell(p.selectedCard);
		selectedPresentation = p.selectedPresentation;
		parent.copyFrom(cards,p.cards);
		story = p.story;
	}
	public long Digest(Random r,boolean v)
	{
		return(r.nextLong()^(v?uid*54215:uid*13515));
	}
	public long Digest(Random r,int v)
	{
		return(r.nextLong()^(uid*(377235+v)));
	}
	public long Digest(Random r,long v)
	{
		return(r.nextLong()^(uid*(377235+v)));
	}
	public long Digest(Random r)
	{
		long v = parent.Digest(r,cards);
		v += Digest(r,boardIndex);
		// story is not digested
		v += Digest(r,score);
		v += Digest(r,stake);
		v += Digest(r,stakeSet);
		v += Digest(r,bet);
		v += Digest(r,betSet);
		v += Digest(r,ready);
		v += Digest(r,parent.Digest(r,selectedCard));
		v += Digest(r,selectedPresentation);
		return(v);
	}
	public void sameBoard(PlayerBoard other)
	{	// selected and story are not considered
		G.Assert(score==other.score, "score mismatch");
		G.Assert(stake==other.stake, "stake mismatch");
		G.Assert(stakeSet==other.stakeSet, "stakeSet mismatch");
		G.Assert(bet==other.bet, "bet mismatch");
		G.Assert(betSet==other.betSet, "betSet mismatch");
		G.Assert(ready==other.ready, "ready mismatch");
		G.Assert(parent.sameCells(selectedCard, other.selectedCard), "selected card mismatch");
		G.Assert(selectedPresentation == other.selectedPresentation, "selected presentation mismatch");

		G.Assert(parent.sameContents(cards, other.cards),"cards mismatch");
	}
	public void select(ImagineChip ch)
	{
		for(ImagineCell card : cards) { if(card.topChip()==ch) { selectedCard = card; return; } }
		G.Error("No card matches "+ch);
	}
	public void select(ImagineCell c)
	{
		if(c==selectedCard) { selectedCard = null; } else { selectedCard = c; }
	}
	public void setReady()
	{
		ready = true;
	}
	public boolean isReady() { return(ready); }
	
	public void vote(int n)
	{	
		if(n==selectedPresentation) { selectedPresentation = -1; }
		else { selectedPresentation=n; }
	}
	public ImagineCell getSelectedCard() { return(selectedCard); }
	public int getSelectedPresentation() { return(selectedPresentation); }
	public String getStory() { return(story); }
	public void setStory(String msg) { story = ((msg==null) || "".equals(msg)) ? null : msg; }
	public int getStake() { return(stake); }
	public int getBet() { return(bet); }
	public int getScore() { return(score); }
	public void setStake(int sta) 
	{ 	if(stake!=sta) { stake = sta; stakeSet = true; }
		else { stake = -1; stakeSet = false; }
	}
	public void setBet(int sta) 
	{ 	if(bet!=sta) { bet = sta; betSet = true; }
		else { bet = -1; betSet = false; }
	}

	public void setScore(int sc) { score = sc; }
	public boolean isMyCard(ImagineChip ch)
	{
		for(ImagineCell c : cards)
		{
			if(ch==c.topChip()) { return(true); }
		}
		return(false);
	}
}

class ImagineBoard 
	extends RBoard<ImagineCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,ImagineConstants
{	static int REVISION = 102;			
					// 100 represents the initial version of the game
					// revision 101 adds deck1a, double the images
					// revision 102 triggers the endgame immediately after the last round.

	static final String[] GRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
	public int getMaxRevisionLevel() { return(REVISION); }
	ImagineVariation variation = ImagineVariation.Imagine;
	
	private ImagineState board_state = ImagineState.Puzzle;	
	private ImagineState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public ImagineState getState() { return(board_state); }
	
    public int scoreForPlayer(int idx)
    {
    	return getPlayerBoard(idx).getScore();
    }
   
    
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(ImagineState st) 
	{ 	unresign = (st==ImagineState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

	PlayerBoard pbs[] = new PlayerBoard[0];
	public PlayerBoard getPlayerBoard(int n) { return(pbs[n]); }
	public int getSelectedPresentation(int pl) { return(getPlayerBoard(pl).getSelectedPresentation()); }
	public ImagineCell getSelectedCell(int pl) { return(getPlayerBoard(pl).getSelectedCard()); }
	public ImagineChip getSelectedChip(int pl) 
	{
		ImagineCell chip = getPlayerBoard(pl).getSelectedCard();
		return((chip==null)? null : chip.topChip());
	}
	public int getStake(int pl) { return(getPlayerBoard(pl).getStake()); }
	public int getBet(int pl) { return(getPlayerBoard(pl).getBet()); }
	public String getStory(int pl) { return(getPlayerBoard(pl).getStory()); }
	public String getSelectedStory() { return(selectedStory==null?"<no story is set>":selectedStory); }
	ImagineCell deck = new ImagineCell(ImagineId.Deck);
	ImagineCell discards = new ImagineCell(ImagineId.Discards);
	ImagineCell backs = new ImagineCell(ImagineId.Deck);
    boolean simultaneousPlay = true;
    int nPasses = 0;
    

// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() 
		{ setGameOver(replayMode.Replay);
		};	
	CellStack animationStack = new CellStack();
     // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public ImagineChip pickedObject = null;
    public ImagineChip lastPicked = null;
    private ImagineCell whiteChipPool = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private StateStack stateStack = new StateStack();
    int numberOfRounds = 0;
    int currentRound = 0;
    ImagineChip selectedCard = null;
    String selectedStory = null;
    private int architect = -1;
    public int architect() { return(architect); }
    public boolean isArchitect(int pl) { return(pl==architect); }
    public ImagineCell selectedPresentation[] = null;
    public PlayerBoardStack presentationVotes[] = null;
    
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str,String... args)
 	{	//if(!robotBoard)
 		{String trans = s.get(str,args);
 		 gameEvents.push(trans);
 		}
 	}
    private ImagineState resetState = ImagineState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	
	// constructor 
    public ImagineBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = GRIDSTYLE;
        setColorMap(map, players);
        
        doInit(init,key,players,rev); // do the initialization 
    }
    
    public String gameType() { return(G.concat(gametype," ",players_in_game," ",randomKey," ",revision)); }
    

    public void doInit(String gtype,long key)
    {
    	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	doInit(typ,ran,np,rev);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	adjustRevision(rev);
    	architect = -1;
    	nPasses = 0;
    	currentRound = 0;
    	players_in_game = players;
    	win = new boolean[players];
 		setState(ImagineState.Puzzle);
		variation = ImagineVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;
		selectedCard = null;
		selectedStory = null;
		int map[] = getColorMap();
		if(selectedPresentation==null || selectedPresentation.length!=players)
		{	int nVoteCards = Math.max(MIN_VOTING_CARDS, players);
			selectedPresentation = new ImagineCell[nVoteCards];
			presentationVotes = new PlayerBoardStack[nVoteCards];
			for(int i=0; i<nVoteCards;i++)
			{
				selectedPresentation[i] = new ImagineCell(ImagineId.Presentation,'@',i);
				presentationVotes[i] = new PlayerBoardStack();
			}
		}
		reInit(selectedPresentation);
		reInit(presentationVotes);
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case Imagine:
			Colors color[] = Colors.values();
			if(pbs.length!=players_in_game)
			{
				PlayerBoard newpbs[] = new PlayerBoard[players_in_game];
				for(int i=0;i<players_in_game;i++) { newpbs[i] = new PlayerBoard(this,color[map[i]],i); }
				pbs = newpbs;
			}
			for(int i=0; i<players_in_game;i++) { pbs[i].color = color[map[i]]; }
			switch(players)
			{
			case 3:
				numberOfRounds = 4*players;
				break;
			case 4:
				numberOfRounds = 3*players;
				break;
			case 5:
			case 6:
			default:
				numberOfRounds = 2*players;	// also 2 player games for testing
				break;
			
			}
			// using reInitBoard avoids thrashing the creation of cells 
			// when reviewing games.
			// or initBoard(variation.firstInCol,variation.ZinCol,null);
			// Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
			// allCells.setDigestChain(r);		// set the randomv for all cells on the board
		}

 		for(PlayerBoard p : pbs) { p.doInit(); }
 		
 		deck.reInit();
 		backs.reInit();
 		discards.reInit();
 		for(ImagineChip ch : ImagineChip.deck1) { deck.addChip(ch); backs.addChip(ImagineChip.cardBack); }
 		if(revision>=101)
 		{
 			for(ImagineChip ch : ImagineChip.deck1a) { deck.addChip(ch); backs.addChip(ImagineChip.cardBack); }
 		}
 		Random r = new Random(randomKey);
 		deck.shuffle(r);
 
 		for(PlayerBoard pb :pbs) 
 			{ for(ImagineCell c : pb.cards) { c.addChip(draw()); }
 			}
 		
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    stateStack.clear();
	    
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
   
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }

    /** create a copy of this board */
    public ImagineBoard cloneBoard() 
	{ ImagineBoard dup = new ImagineBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((ImagineBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ImagineBoard from_b)
    {
        super.copyFrom(from_b);
        for(int i=0;i<pbs.length;i++) { pbs[i].copyFrom(from_b.pbs[i]);}
        deck.copyFrom(from_b.deck);
        backs.copyFrom(from_b.backs);
        discards.copyFrom(from_b.discards);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        nPasses = from_b.nPasses;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        copyFrom(selectedPresentation,from_b.selectedPresentation);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        selectedCard = from_b.selectedCard;
        selectedStory = from_b.selectedStory;
        numberOfRounds = from_b.numberOfRounds;
        currentRound = from_b.currentRound;
        lastPicked = null;

        if(G.debug()) { sameboard(from_b); }
    }
    public PlayerBoard getOwner(ImagineChip ch)
    {
    	for(PlayerBoard pb : pbs) { if(pb.isMyCard(ch)) { return(pb); }}
    	return(null);
    }
    public void buildPresentation()
    {	ChipStack pre = new ChipStack();
    	for(int i=0;i<nPlayers(); i++)
    	{	PlayerBoard pb = pbs[i];
    		ImagineCell card = pb.getSelectedCard();
    		if(card!=null)
    		{
    		pre.push(card.topChip());
    		}
    		else { throw G.Error("no selected card for %s",pb); }
    	}
    	// draw up to the minimum number of cards
    	while(pre.size()<selectedPresentation.length) { pre.push(draw()); }
    	
    	pre.shuffle(new Random(randomKey^moveNumber()*123652));
    	reInit(selectedPresentation);
    	for(ImagineCell c : selectedPresentation) { c.addChip(pre.pop()); }
    }
    public ImagineChip draw()
    {
    	if(deck.height()==0) 
    	{	
    		deck.copyFrom(discards);
    		backs.reInit();
    		for(int lim=deck.height(); lim>0;lim--) { backs.addChip(ImagineChip.cardBack); }
    		discards.reInit();
    		deck.shuffle(new Random(randomKey ^ moveNumber()*23526));
    	}
    	backs.removeTop();
    	return(deck.removeTop());
    }

    public void discardAndDraw(replayMode replay)
    {
    	for(int i=0;i<nPlayers();i++)
    	{
    		int who = (architect+i)%nPlayers();
    		PlayerBoard pb = pbs[who];
    		ImagineCell sel = pb.getSelectedCard();
    		if(sel!=null && sel.topChip()!=null)
    		{	discards.addChip(sel.removeTop());
    			sel.addChip(draw());
    			if(replay.animate)
    			{	
    				animationStack.push(sel);
    				animationStack.push(discards);
    				animationStack.push(deck);
    				animationStack.push(sel);
    			}
    		}
    	}
    }

    public void sameboard(BoardProtocol f) { sameboard((ImagineBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ImagineBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        for(int i=0;i<pbs.length;i++) { pbs[i].sameBoard(from_b.pbs[i]); }
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameContents(selectedPresentation,from_b.selectedPresentation),"selected presentation mismatch");
        G.Assert(selectedCard==from_b.selectedCard,"selected card mismatch");
        G.Assert(numberOfRounds==from_b.numberOfRounds,"number of rounds mismatch");
        G.Assert(currentRound==from_b.currentRound,"currentRound mismatch");
        G.Assert(selectedStory==null ? selectedStory==from_b.selectedStory : selectedStory.equals(from_b.selectedStory), "story mismatch");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");

    }

    /** 
     * Digest produces a 64 bit hash of the game state.  This is used in many different
     * ways to identify "same" board states.  Some are relevant to the ordinary operation
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
        // the basic digestion technique is to xor a bunch of random numbers. 
    	// many object have an associated unique random number, including "chip" and "cell"
    	// derivatives.  If the same object is digested more than once (ie; once as a chip
    	// in play, and once as the chip currently "picked up", then it must be given a
    	// different identity for the second use.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);
        for(int i=0;i<pbs.length;i++) { v ^= pbs[i].Digest(r); }
        v ^= deck.Digest(r);
        v ^= discards.Digest(r);
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,selectedPresentation);
		v ^= Digest(r,revision);
		v ^= Digest(r,currentRound);
		v ^= Digest(r,numberOfRounds);
		v ^= Digest(r,nPasses);
		v ^= Digest(r,selectedStory==null ? 0 : selectedStory.hashCode());
		v ^= Digest(r,selectedCard);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
        return (v);
    }



    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player in state ",board_state);
        case Puzzle:
            break;
        case Appreciate:
        case Play:
        case Vote:
        case Story:
        case Skip:
        case Resign:
            moveNumber++; //the move is complete in these states
            setWhoseTurn((whoseTurn+1)%nPlayers());
            return;
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState(int player)
    {	boolean maybe = board_state.doneState();
    	if(maybe)
    		{switch(board_state)
    		{
    		case Appreciate:
 				{
				PlayerBoard pb = pbs[player];
				maybe = !pb.isReady();
				}
				break;
       		case Vote:
       			{
				PlayerBoard pb = pbs[player];
				maybe = !pb.isReady() && (pb.betSet && (pb.getSelectedPresentation()>=0));
       			}
       			break;
       		case Story: 
    			{
    			PlayerBoard pb = pbs[player];
    			maybe = ((player==whoseTurn) && pb.stakeSet && (pb.getSelectedCard()!=null) && (pb.story!=null));
    			}
    			break;
    		case Play:
    			{
    				PlayerBoard pb = pbs[player];
    				maybe = !pb.isReady() && (pb.stakeSet && (pb.getSelectedCard()!=null));
    			}
    			break;
    		default: break;
    		}}
    		
    	return(maybe);
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }



    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	boolean win = false;
    	return(win);
    }

    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
        pickedObject = null;
     }

    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ImagineCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { ImagineChip ch = pickedObject;
      if(ch!=null)
    	{	return(ch.chipNumber()); 
    	}
      	return (NothingMoving);
    }
   /**
     * get the cell represented by a source code, and col,row
     * @param source
     * @param col
     * @param row
     * @return
     */
    private ImagineCell getCell(ImagineId source, char col, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case Presentation:
        	return(selectedPresentation[row]);
        case Card:
        	{
        	PlayerBoard pb = pbs[col-'A'];
        	return(pb.cards[row]);
        	}
        case HitPlayerChip:
        	return(whiteChipPool);
        } 	
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public ImagineCell getCell(ImagineCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }

    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(ImagineCell c)
    {	return(c==pickedSourceStack.top());
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //


    public boolean allPlayersReady()
    {
    	for(PlayerBoard pb : pbs) { if(!pb.ready) { return(false); }}
    	return(true);
    }

    public void doScoring(boolean commit)
    {
    	PlayerBoard teller = pbs[architect];
    	// give each player point equal to his stake times the number of votes
    	reInit(presentationVotes);
    	
    	// collect the votes from the players other than the storyteller
    	ImagineCell story = pbs[architect].getSelectedCard();
    	ImagineChip winner = story.topChip();
    	for(PlayerBoard pb : pbs)
    	{	if(pb!=teller)
    		{
    		int presentation = pb.getSelectedPresentation();
    		if(presentation>=0)
    		{
    		PlayerBoardStack vote = presentationVotes[presentation];
    		vote.push(pb);
    		if(commit)
    		{
     		// presentation card is a non-storyteller vote
    		ImagineChip presentationCard = selectedPresentation[presentation].topChip();
     		int bet =  pb.getBet();
     		int score = presentationCard==winner ? bet:-bet;
     		logGameEvent(BetAndScored,pb.color.name(),""+bet,""+score);
     		pb.score += score;    			
    		}}}
    	}
    	if(commit)
    	{
    	// score the non-storytellers according to the number of votes they get
    	// score the storyteller unless he got all the votes.
    	for(PlayerBoard pb : pbs)
    	{	ImagineChip selected = pb.getSelectedCard().topChip();
    		boolean found = false;
    		for(int sel = 0; !found && (sel < selectedPresentation.length); sel++)
    		{
    		if(selectedPresentation[sel].topChip()==selected)
    		{
    		int numhits = presentationVotes[sel].size();
    		if((pb==teller) && numhits>=(nPlayers()-1)) { numhits = 0; }	// punish if everyone guessed
    		// if no one guesses, lose the stake.  If 1 or more guesses, gain the stake*n
    		int stake = pb.getStake();
    		int points = stake*(numhits==0 ? -1 : numhits);
    		logGameEvent("#1 stakes #2, scores #3",pb.color.name(),""+stake,""+points);
    		pb.score += points;
    		}   		
    		}
    	}}
    	
    }
    private void setGameOver(replayMode replay)
    {	discardAndDraw(replay);
    	setNextPlayer(replay);
    	selectedStory="";
    	for(PlayerBoard pb : pbs) { pb.newStoryTeller(); }
    	setState(ImagineState.Gameover);
    }
    private void setNextStateAfterDone(replayMode replay)
    {	
    	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: break;
    	
    	case Appreciate:
    		if(allPlayersReady())
    		{
    			currentRound++;
     			if(currentRound>=numberOfRounds)
    			{
    				setGameOver(replay);
    			}
    			else {
    				G.Assert(whoseTurn==architect,"should still be the storyteller");
    				discardAndDraw(replay); 
    				setNextPlayer(replay);
    				for(PlayerBoard pb : pbs) { pb.newStoryTeller(); }
    				architect = whoseTurn;
    				setState(ImagineState.Story); 
    				}
    		}
    		break;
    		
    	case Play:
    		G.Assert(allPlayersReady(),"all players should be ready");
    		buildPresentation();
    		for(PlayerBoard pb : pbs) { pb.prepareToVote(); }
    		pbs[architect].setReady();
    		setState(ImagineState.Vote);
    		doScoring(false);
    		break;
  
    	case Vote:
    		G.Assert(allPlayersReady(),"all players should have voted");
    		doScoring(true);
    		for(PlayerBoard pb : pbs) { pb.prepareToAppreciate(); }
    		if((revision>=102) && (currentRound+1>=numberOfRounds))
    			{	// end the game immediately instead of a final appreciation
    				setState(ImagineState.Gameover);
    			}
    			else {
    				setState(ImagineState.Appreciate);
    			}
    		break;
    	case Story:
    		setState(ImagineState.Play);
    		for(PlayerBoard pb : pbs) { if(pb.boardIndex!=architect) { pb.prepareToVote(); } }
     		break;
    	case Puzzle:
    	case Skip:
    	case Getnew:
    		setState(ImagineState.Story);
    		for(PlayerBoard pb : pbs)
    			{
    			  pb.newStoryTeller(); 
    			}
    		architect = whoseTurn;
    		selectedCard = null;
    		selectedStory = null;
    		break;
    	}
       	resetState = board_state;
    }
    private void discardAndRedraw(replayMode replay)
    {	PlayerBoard pb = pbs[whoseTurn];
    	pb.score -= 2;
    	ImagineCell[] cards = pb.cards;
    	for(int i=0;i<cards.length;i++)
    	{	ImagineCell sel =cards[i];
    		if(sel.topChip()!=null)
    			{ discards.addChip(sel.removeTop());
    			  if(replay.animate)
    			  {
    				  animationStack.push(sel);
    				  animationStack.push(discards);
    			  }
    			}
    		sel.addChip(draw());
    		if(replay.animate)
    		{
    			animationStack.push(deck);
    			animationStack.push(sel);
    		}
    		
    	}
    }
    private void doDone(replayMode replay)
    {
    	switch(board_state)
    	{
    	case Getnew:
    		discardAndRedraw(replay);
    		break;
    	case Skip:
    		setNextPlayer(replay);
           	nPasses++;
        	if(nPasses>=nPlayers()) { setState(ImagineState.Gameover); }
    		break;
    	case Appreciate: 
    		pbs[whoseTurn].setReady();
    		break;
    	default: break; 
    	}
        acceptPlacement();
        setNextStateAfterDone(replay);
        
    }
    public boolean isReady(int pl) { return(pbs[pl].isReady()); }
    public boolean isStoryTeller(int pl) { return(pl==architect); }

    
    public boolean readyToScore()
    {
    	switch(board_state)
    	{case Vote:	return(allPlayersReady());
    	 default: return(false);
    	}
    }
    public boolean readyToProceed()
	{
		switch(board_state)
		{
		case Appreciate:
		case Play:	return(allPlayersReady());
		
		default: return(false);
		
		}
	}
    private void vote(PlayerBoard pb,ImagineChip chip)
    {
		for(int i=0;i<selectedPresentation.length;i++)
		{ ImagineCell pres = selectedPresentation[i];
		   if(pres.topChip()==chip)
		   	{
			pb.vote(i);
		   	break;
		   	}
		}
    }
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	Imaginemovespec m = (Imaginemovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        
      //  G.print("E "+m+" for "+whoseTurn+" "+board_state);
 
       switch (m.op)
        {
        case MOVE_GETNEW:
        	setState( board_state==ImagineState.Getnew ? ImagineState.Story : ImagineState.Getnew);
        	break;
        case MOVE_SKIP:
        	setState( board_state==ImagineState.Skip ? ImagineState.Story : ImagineState.Skip);
         	break;
        case MOVE_SCORE:
        case MOVE_COMMIT:
        case MOVE_DONE:
         	doDone(replay);
            break;
         
        case SET_READY:
        case EPHEMERAL_SET_READY:
        	{
        	int player = m.to_col-'A';
        	PlayerBoard pb = pbs[player];
        	m.player = player;
        	pb.setReady();
        	}
        	break;
        case SET_STAKE:
        case EPHEMERAL_SET_STAKE:
        	{
        	// select a stake or bet
        	int pn = m.to_col-'A';
        	m.player = pn;
        	PlayerBoard pb = pbs[pn];
        	switch(board_state)
        	{
        	case Vote:
        		pb.setBet(m.to_row);
        		break;
        	case Play:
        	case Story:
        	case Puzzle:
        		pb.setStake(m.to_row);
        		break;
        	default: G.Error("Not expecting setstake in "+board_state);
        	}
        	}
        	break;
        case EPHEMERAL_MOVE_SELECT:
        case MOVE_SELECT:
        	{
        	// select an image or one of the presentation
        	ImagineCell src = getCell(m.source,m.to_col,m.to_row);
        	int pn = m.to_col-'A';
        	m.player = pn;
        	PlayerBoard pb = pbs[pn];
        	G.Assert(!pb.isReady(),"can't select when already ready");
        	if(m.source==ImagineId.Presentation)
        	{
        	vote(pb,src.topChip());
        	}
        	else
        	{
         	pb.select(src);
        	}}
        	break;

        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(ImagineState.Puzzle);	// standardize the current state
            setNextStateAfterDone(replay); 

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?ImagineState.Resign:unresign);
            break;
            
       case MOVE_EDIT:
        	acceptPlacement();
            setState(ImagineState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(ImagineState.Gameover);
    	   break;

       case EPHEMERAL_SET_CHOICE:
       case SET_CHOICE:
  		{	
  			int who = m.to_col-'A';
  			m.player = who;
  			PlayerBoard pb = pbs[who];
  			pb.vote(-1);
  			vote(pb,m.chip);
   			pb.setBet(-1);	// set to -1 to clear it
  			pb.setBet(m.to_row);
  			pb.setReady();
  			doScoring(false);
    		}
    		break;
   	   
       case EPHEMERAL_SET_CANDIDATE:
       case SET_CANDIDATE:
       		{
           	   int who = m.to_col-'A';
           	   m.player = who;
        	   PlayerBoard pb = pbs[who];
        	   pb.select(m.chip);
        	   pb.setStake(-1);	// set to -1 to clear it
        	   pb.setStake(m.to_row);
        	   pb.setReady();
       		}
       		break;
       case SET_STORY:
       	   {
       	   // storyteller commits to his selections
       	   int who = m.player;
    	   PlayerBoard pb = pbs[who];
    	   pb.select(m.chip);
    	   pb.setStory(m.story);
    	   pb.setStake(-1);	// set to -1 to clear it
    	   pb.setStake(m.to_row);
    	   nPasses = 0;
     	   selectedCard = m.chip;
    	   selectedStory = m.story;
    	   pb.setReady();
    	   setNextStateAfterDone(replay);
    	   gameEvents.push(m.story);
       	   }
    	   break;
        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        
 
        
        return (true);
    }

    // legal to hit the chip storage area
    public boolean legalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting LegalToHit state " + board_state);
        case Vote:
        case Play:
        case Getnew:
        case Skip:
        case Story:
        	// for pushfight, you can pick up a stone in the storage area
        	// but it's really optional
        	return(player==whoseTurn);
		case Resign:
        case Appreciate:
		case Gameover:
			return(false);
        case Puzzle:
            return (true);
        }
    }

    public boolean legalToHitBoard(ImagineCell c)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
		case Play:
			return(c.isEmpty());
		case Gameover:
		case Resign:
			return(false);
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
            return (true);
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Imaginemovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        Execute(m,replayMode.Replay);
        acceptPlacement();
       
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Imaginemovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
    	ImagineState state = robotState.pop();
        switch (m.op)
        {
        default:
   	    	throw G.Error("Can't un execute " + m);
        case MOVE_DONE:
            break;
            
        case MOVE_RESIGN:
            break;
        }
        setState(state);
        if(whoseTurn!=m.player)
        {	moveNumber--;
        	setWhoseTurn(m.player);
        }
 }
  

 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	G.Error("Not implemented");
 	return(all);
 }

public int cellToX(ImagineCell c) {
	return 0;
}
public int cellToY(ImagineCell c) {
	return 0;
}




}
