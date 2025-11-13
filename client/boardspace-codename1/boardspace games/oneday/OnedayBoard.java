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
package oneday;

import static oneday.OnedayMovespec.*;

import lib.*;
import online.game.BoardProtocol;
import online.game.CommonMoveStack;
import online.game.RBoard;
import online.game.chip;
import online.game.commonMove;
import online.game.replayMode;
/**
 * OnedayBoard knows all about the game of OnedayInLondon.
 * It gets a lot of logistic support from game.rectBoard, 
 * which knows about the coordinate system.  
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
 *  restrictive about what to allow in each state, and have a lot of trip wires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

public class OnedayBoard extends RBoard<OnedayCell> implements BoardProtocol,OnedayConstants
{	public boolean SIMULTANEOUS_PLAY = true;

	interface OnedayLocation extends Digestable
	{	public double getX();
		public double getY();
		public Station getStation();
		public Stop getStop();
		public Line getLine();
		public Platform getPlatform();
		public Train getTrain();
	}
	class PlayerBoard
	{	int index = 0;
		double displayScale = 3.0;
		// fields used in Safari
		OnedayLocation myLocation = null;
		OnedayLocation nextLocation = null;
		private SafariState safariState = SafariState.Waiting;	
		long stateStartTime = 0;
		double position = 0;
		public void setState(SafariState newstate)
		{
			if(newstate!=safariState)
			{	switch(newstate)
				{	default: throw G.Error("Not expecting newstate %s",newstate); 
					case WalkAndEnterTrain:	// already walking
							break;	// no timing change
					case Walking:
					case Waiting:
					case WaitAndEnterTrain:
					case OnTrain:
						position = 0;
						stateStartTime = simulatedTime;
						break;
					case ExitingTrain:
						break;
				}
				safariState = newstate;
				
			}
		}
		public boolean canTransitionToWalking()
		{	switch(safariState)
			{
			default: return(false);
			case Waiting:
			case WaitAndEnterTrain:
				return(true);
			}
		}
		// board the next available train
		public void setBoardTrain()
		{
			switch(getState())
			{
			default: throw G.Error("Not expecting state %s",safariState);
			case WalkAndEnterTrain:
			case WaitAndEnterTrain:
				break;
			case Waiting:	setState(SafariState.WaitAndEnterTrain);
				break;
			case Walking:	setState(SafariState.WalkAndEnterTrain);
				break;
			}
		}
		// board the next available train
		public void setExitTrain()
		{
			switch(getState())
			{
			default: throw G.Error("Not expecting state %s",safariState);
			case OnTrain:
			case ExitingTrain:
				setState(SafariState.ExitingTrain);
				break;
			}
		}
			
		// set the destination within the station
		public void walkTo(OnedayLocation l)
		{	nextLocation = l;
			G.Assert(canTransitionToWalking(),"illegal state transition to walking");
			setState(SafariState.Walking);
		}
		public void claimPrizes(Platform p)
		{
			if(p.prize!=null)
			{
				winnings.addChip(p.prize);
				p.prize = null;
				OnedayCell c = getStationCell(p.getStation());
				c.hasPrize = false;
			}
		}
		public void setLocation(OnedayLocation l)
		{	
			myLocation = l; 
		}
		public OnedayLocation getLocation()
		{ 	return(myLocation); 
		}
		public void setNextLocation(OnedayLocation l)
		{
			nextLocation = l;
		}
		public OnedayLocation getNextLocation() 
		{
			return(nextLocation);
		}
		public SafariState getState() { return(safariState); }
		
		OnedayCell winnings = null;
		
		// fields used in Standard
	    OnedayCell rack[] = null;			// main player card racks
	    
	    
	    // constructor for clone
	    PlayerBoard() { }
	    // constructor
	    PlayerBoard(int ind,Random r) 
	    {	index = ind;
	    	rack = new OnedayCell[CARDSINRACK];
	    	winnings = new OnedayCell(r,OneDayId.Winnings);
	    	
	    }
	    
	    public void sameboard(PlayerBoard other)
	    {
	    	sameCells(rack,other.rack);
	    	sameCells(winnings,other.winnings);
	    	G.Assert(myLocation==other.myLocation, "myLocation mismatch");
	    	G.Assert(safariState==other.safariState,"safari state mismatch");
	    	G.Assert(nextLocation==other.nextLocation, "nextLocation mismatch");
	    }
	    	
	    
	    public long Digest(Random r)
	    {	long v = 0;
	    	v ^= OnedayCell.Digest(r,rack);
	    	v ^= OnedayBoard.this.Digest(r,myLocation);
	    	v ^= OnedayBoard.this.Digest(r,nextLocation);
	    	v ^= OnedayBoard.this.Digest(r,safariState.ordinal());
	    	return(v);
	    }
	    public void copyfrom(PlayerBoard other)
	    {	index = other.index;
	    	copyFrom(rack,other.rack);
	    	myLocation = other.myLocation;
	    	nextLocation = other.nextLocation;
	    	safariState = other.safariState;
	    	copyFrom(winnings,other.winnings);
	    }
	    public PlayerBoard clone()
	    { PlayerBoard b  = new PlayerBoard();
	      b.copyfrom(this);
	      return(b);
	    }
	    
	    public void update(long simtime)
	    {
	    	// update the player position based on the new simulated time.  
	    	SafariState state = getState();
	    	switch(state)
	    	{
	    	default: break;
	    	case Walking:
	    	case WalkAndEnterTrain:
	    		{
	    		long walkingTime = Interchange.timeFromTo(myLocation,nextLocation);
	    		if(simtime>walkingTime+stateStartTime)
	    			{
	    				setLocation(nextLocation);
	    				claimPrizes(nextLocation.getPlatform());
	    				setNextLocation(null);
	    				position = 0;
	    				switch(state)
	    				{
	    				default: throw G.Error("not expecting %s",state);
	    				case Walking: setState(state=SafariState.Waiting); break;
	    				case WalkAndEnterTrain: setState(state=SafariState.WaitAndEnterTrain); break;
	    				}
	    			}
	    			else
	    			{
	    				position = (double)(simtime-stateStartTime)/walkingTime;
	    			}
	    		}
	    		break;
	    	}
	    	
	    	// mount and dismount trains
	    	switch(state)
	    	{
	    	default: break;
	    	
	    	case WaitAndEnterTrain:
	    		{
	    		Station myStation = myLocation.getStation();
	    		Platform myPlatform = myLocation.getPlatform();
	    		Line myLine = myLocation.getLine();
	    		for(int lim=trains.size()-1;lim>=0;lim--)
	    		{	Train tr = trains.elementAt(lim);
	    			if((tr.getLine()==myLine)
	    					&& (tr.getStation()==myStation)
	    					&& (tr.nextStop!=null)
	    					&& (tr.status==Train.Status.stopped)  
	    					&& (tr.getPlatform()==myPlatform)
	    					)
	    			{	// mount the train
	    				setLocation(tr);
	    				setState(SafariState.OnTrain);
	    			}
	    		}}
	    		break;
	    	case OnTrain:
	    		{
	    			Train myTrain = myLocation.getTrain();
	    			if(myTrain.status==Train.Status.ended)
	    			{
	    				// end of the line for this train, must get off
	    				Platform newPlatform = myTrain.getPlatform();
	    				claimPrizes(newPlatform);
	    				setLocation(newPlatform);
	    				
    					setState(SafariState.Waiting);
	    			}
	    		}
	    		break;
	    	case ExitingTrain:
	    		{
	    		Train myTrain = myLocation.getTrain();
	    	
	    		for(int lim=trains.size()-1;lim>=0;lim--)
	    			{	Train tr = trains.elementAt(lim);
	    				if((tr==myTrain) 
	    					&& (tr.status==Train.Status.stopped))
	    				{	Platform plat = tr.getPlatform();
	    					setLocation(plat);
	    					claimPrizes(plat);
	    					setState(SafariState.Waiting);
	    				}
	    			}
	    		}
	    		break;
	}
	    	
	    }
	}	// end of playerboard
	
	
	PlayerBoard playerBoard[] = null;
	OnedayVariation variation = OnedayVariation.Standard;
	//
	// variables for Safari
	public long simulatedTime = 0;			// the "now" on the clock, 0 for the start of the simulation
	public long currentSimulatedTime() { return(simulatedTime); }
	public boolean running = false;
	public double simulationRate = Default_Simulation_Rate;			// multiplier to realtime
	public TrainStack trains = new TrainStack();
	public TrainLauncherStack launchers = new TrainLauncherStack();
	//
	// variables for Standard game
    public OnedayCell drawPile = null;			// the main draw pile
    public OnedayCell discardPile[] = null;		// the three discard piles
    public OnedayCell blankCard = null;			// a blank card face for the viewer to draw
    public OnedayCell cardBack = null;			// the card back for the viewer to draw
    public OnedayCell tempCell = null;			// trash cell for the viewer to use
    public OnedayCell startingPile[] = null;	// player starting piles of 10 cards
   

    public CellStack animationStack = new CellStack();
    public void SetDrawState() { setState(OnedayState.Draw); }
   
    //
    // stuff for robot players to keep track of undos
    //
	private CellStack robotCellStack = new CellStack();
	private ChipStack robotShuffleStack = new ChipStack();
	private ChipStack robotStationStack = new ChipStack();
	private boolean robotGame=false;
	private int myRobotPlayer = -1;
	private Random robotRandom = null;
	
	public void setRobotGame(int my,Random r)
	{  	robotGame=true;
		myRobotPlayer = my;
		robotRandom = r;
		SIMULTANEOUS_PLAY = false;
	}
	
    //
    // private variables
    //
    private OnedayState resetState = null;		// state to revert to when a reset is some
    private OnedayState undropState = null;		// state to revert to when undoing a drop on the rack
    private boolean[] resigned = null;			// true for players who have resigned
    private OnedayState board_state = OnedayState.Play;	// the current board state
    private OnedayState unresign = null;					// remembers the previous state when "resign"
    public OnedayState getState() { return(board_state); } 
	public void setState(OnedayState st) 
	{ 	unresign = (st==OnedayState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
   
    
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public OnedayChip pickedObject = null;
    private CellStack pickedSourceStack = new CellStack();
    private CellStack droppedDestStack = new CellStack();
	private IStack exposedCardStack = new IStack();
   
    public void initBoard()
    {
    	super.initBoard();
    	for(int i=0;i<Station.nStations(); i++)
    	{
    		OnedayCell c = new OnedayCell(i);
    		addCell(c);
    		Station sta = Station.getStation(i);
    		c.addChip(sta);
    		//stations.put(sta,c);
    	}
    	allCells.setDigestChain(new Random(534662));
    }
    public OnedayCell getStationCell(Station st)
    {
    	for(OnedayCell c = allCells; c!=null; c=c.next)
    	{	Station top = (Station)c.topChip();
    		if(top.station==st.station) { return(c); }
    	}
    	//throw G.Error("Cell for station %s not found",st);
    	return(null);
    }
    public OnedayBoard(String init,long rv,int np) // default constructor
    {   
        doInit(init,rv,np); // do the initialization 
     }


    public void sameboard(BoardProtocol b) { sameboard((OnedayBoard)b); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if clone,digest and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(OnedayBoard from_b)
    {
    	super.sameboard(from_b);	// calls sameCell for each cell, also for inherited class variables.
    	G.Assert(unresign==from_b.unresign,"unresign mismatch");
       	G.Assert(AR.sameArrayContents(win,from_b.win),"win array contents match");
       	G.Assert(AR.sameArrayContents(resigned,from_b.resigned),"resigned mismatch");
       	G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(sameContents(exposedCardStack,from_b.exposedCardStack),"exposed cells mismatch");
        G.Assert(sameCells(drawPile,from_b.drawPile),"drawpile mismatch");
        for(int i=0;i<players_in_game;i++) { playerBoard[i].sameboard(from_b.playerBoard[i]); }
        switch(variation)
        {
        default: throw G.Error("Not expected");
        case Standard:
            G.Assert(sameCells(startingPile,from_b.startingPile),"startingPile mismatch");
        G.Assert(sameCells(discardPile,from_b.discardPile),"discardpile mismatch");
            break;
        case Safari:
        	break;
        }
        
        G.Assert(pickedObject==from_b.pickedObject,"pickedObject doesn't match");
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
    {   // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long v = super.Digest(r);

		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,exposedCardStack);
		v ^= Digest(r,drawPile);
	    for(PlayerBoard b : playerBoard) { v ^= b.Digest(r); }
	
		switch(variation)
		{
		default: throw G.Error("Not expected");
		case Safari:
		    break;
		case Standard:
		v ^= Digest(r,discardPile);
		    v ^= Digest(r,startingPile);
			break;
		}

    	

	    v ^= Digest(r,resigned);
		v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
        return (v);
    }
   public OnedayBoard cloneBoard() 
	{ OnedayBoard copy = new OnedayBoard(gametype,randomKey,players_in_game);
	  copy.copyFrom(this); 
	  return(copy);
	}
   public void copyFrom(BoardProtocol b) { copyFrom((OnedayBoard)b); }


    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(OnedayBoard from_b)
    {	
        super.copyFrom(from_b);			// copies the standard game cells in allCells list
        pickedObject = from_b.pickedObject;	
        resetState = from_b.resetState;
        undropState = from_b.undropState;
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        getCell(droppedDestStack,from_b.droppedDestStack);
        copyFrom(exposedCardStack,from_b.exposedCardStack);
        copyFrom(drawPile,from_b.drawPile);
        for(int i=0;i<players_in_game;i++) { playerBoard[i].copyfrom(from_b.playerBoard[i]); }     	
        switch(variation)
        {
        default: throw G.Error("not expected");
        case Standard:
        copyFrom(discardPile,from_b.discardPile);
            copyFrom(startingPile,from_b.startingPile);
        	break;
        case Safari:
        	break;
        }
        AR.copy(resigned,from_b.resigned);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        if(G.debug()) { sameboard(from_b); }
    }
    public void doInit(String gtype,long rv)
    {
    	doInit(gtype,rv,players_in_game);
    }
    public void setCompatibility(String date)
    {
    	BSDate dd = new BSDate(date);
    	BSDate od = new BSDate("Oct 21 2016");
    	// a bug fix in the next stop code had the unintended side
    	// effect of changing the deck and invalidating all the games
    	// played before the bug fix.
    	boolean oldk = Stop.NEXTSTOP_COMPATIBILITY_KLUDGE;
    	Stop.NEXTSTOP_COMPATIBILITY_KLUDGE = dd.before(od);
    	if(oldk!=Stop.NEXTSTOP_COMPATIBILITY_KLUDGE)
    		{ Station.findAllStops() ; 
    		  doInit();
    		}

    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long rv,int np)
    {  	
    	randomKey = rv;
    	players_in_game = np;
    	setColorMap(null,np);
    	win = new boolean[np];
    	resigned = new boolean[np];
    	Random r = new Random(rv);
    	
      	variation = OnedayVariation.findVariation(gtype); 
       	G.Assert(variation!=null,WrongInitError,gtype);
       	gametype = variation.name;
    	
    	initBoard();

    	resetState = null;
    	undropState = null;
    	
    	simulationRate = Default_Simulation_Rate/Default_Realtime_Ratio;
    	simulatedTime = 0;
    	trains.clear();
    	launchers.clear();
    	
    	drawPile = new OnedayCell(r,OneDayId.DrawPile);
     	for(int i=0;i<Station.nCards();i++)
    	{	drawPile.addChip(Station.getCard(i));
     	}
    	Random rx = new Random(rv);
       	drawPile.shuffle(rx);	
    	
    	tempCell = new OnedayCell(r,DefaultId.HitNoWhere);
    	blankCard = new OnedayCell(r,OneDayId.BlankCard);
    	cardBack = new OnedayCell(r,OneDayId.BlankCard);
    	cardBack.addChip(Station.back);
    	blankCard.addChip(Station.blank);
    	
    	playerBoard = new PlayerBoard[players_in_game];
    	for(int i=0;i<players_in_game;i++)
    	{	playerBoard[i] = new PlayerBoard(i,r);
    	}
    	
    	switch(variation)
    	{
    	case Safari:
    		for(int idx = Line.nLines()-1; idx>=0; idx--)
    		{
    		Line l = Line.getLine(idx);
    		if(l.included)
    			{
    			launchers.push(new TrainLauncher(l,1));
    			launchers.push(new TrainLauncher(l,-1));
    			}
    		}
    		
    		String startingStations[] = new String[]{ "Waterloo","King's Cross St. Pancras" };
    		// pick one of the stations to start in
    		Station startingStation = Station.getStation(startingStations[Random.nextInt(r, startingStations.length)]);
    		OnedayCell startingCell = getStationCell(startingStation);
     		// pick one of the lines to start on
    		Platform startingPlatform = startingStation.randomPlatform(r);
    		for(PlayerBoard p : playerBoard)
    		{	p.setLocation(startingPlatform);
    			p.setState(SafariState.Waiting);
    		}
    		// add prizes depending on the number of players.
    		int nPrizes = 1+players_in_game*5;
    		while(nPrizes > 0)
    			{ Station stop = (Station)drawPile.removeTop();
    			  OnedayCell c = getStationCell(stop);
    			  if(c!=null && !c.hasPrize && (c!=startingCell))
    			  	{ c.hasPrize = true;
    			  	  Platform plat = stop.randomPlatform(r);
    			  	  plat.prize = stop;
    			  	  nPrizes--;
    			  	}
    			}
    		break;

		case Standard:
    		{
    		startingPile = new OnedayCell[np];	
	    	discardPile = new OnedayCell[3];
	    	for(int i=0;i<discardPile.length;i++)
	    		{ discardPile[i]=new OnedayCell(r,i,OneDayId.DiscardPile); 
	    		  discardPile[i].exposed = true;
	    		}

        	for(int pl=0;pl<np;pl++)
        	{	OnedayCell c = startingPile[pl] = new OnedayCell(r,(char)('A'+pl),0);
        		c.rackLocation = OneDayId.StartingPile;
        		for(int idx = 0;idx<CARDSINRACK;idx++)
        		{	playerBoard[pl].rack[idx] = new OnedayCell(r,(char)('A'+pl),idx);
        		}
        	}
         	}
        	break;
		default:
			break;
    	}

    	

	    setState(resetState = OnedayState.Puzzle);
	    whoseTurn = FIRST_PLAYER_INDEX;
		pickedSourceStack.clear();
		droppedDestStack.clear();
		exposedCardStack.clear();
		pickedObject = null;
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
        case Puzzle:
            break;
        case Confirm:
        case Draw:
        case Resign:
        case Play:
        case PlayFromDraw:
        case SynchronousPlace:
           moveNumber++; //the move is complete in these states
           {
           int nresigned = 0;
           do { nresigned++;
           		setWhoseTurn((whoseTurn+1)%players_in_game);
           		} while(resigned[whoseTurn] && nresigned<players_in_game);
           }
        }
        return;
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
         case Draw:
            return (true);

        default:
            return (false);
        }
    }

	public boolean winningRack(OnedayCell cells[])
	{	
		return(OnedayCell.lengthOfChain(cells,0)==cells.length);
	}
    public boolean WinForPlayerNow(int player)
    {	// return true if the conditions for a win exist for player right now
    	if(board_state==OnedayState.Gameover) { return(win[player]); }
    	return(winningRack(playerBoard[player].rack));
    }
    // estimate the value of the board position. for static evaluator
    public double ScoreForPlayer(int player,boolean print,boolean dumbot)
    {  	double finalv=dumbot?dumbScoreForPlayer(player):scoreForPlayer(player);
    	return(finalv);
    }
    /* return the score for the player in the range 10-100
     * sum of squares of run lengths
     */
    public int scoreForPlayer(int pl)
    {	if(resigned[pl]) { return(0); }
    	int finalv=0;
    	int start = 0;
    	OnedayCell cells[] = playerBoard[pl].rack;
    	while(start<cells.length)
    	{
    		int run = Math.max(1,OnedayCell.lengthOfChain(cells,start));
    		finalv += run*run;
    		start += run;
    	}
    	return(finalv);
    }
    /* return the score for the player in the range 1-10
     * sum of squares of run lengths
     */
    int dumbScoreForPlayer(int pl)
    {
    	int finalv=0;
    	int start = 0;
    	OnedayCell cells[] = playerBoard[pl].rack;
    	while(start<cells.length)
    	{
    		int run = Math.max(1,OnedayCell.lengthOfChain(cells,start));
    		finalv += run-1;
    		start += run;
    	}
    	return(finalv);
    }
    //
    // finalize all the state changes for this move.
    //
    public void acceptPlacement()
    {	
        pickedObject = null;
        exposedCardStack.clear();
        droppedDestStack.clear();
        pickedSourceStack.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
    G.Assert(pickedObject==null, "nothing should be moving");
    if(droppedDestStack.size()>0)
    	{
    	OnedayCell dr = droppedDestStack.pop();
    	dr.exposed = exposedCardStack.pop()==1;		// restore the exposure state

		pickedObject = dr.removeTop(); 
	    	
    	}
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    void unPickObject()
    {	OnedayChip po = pickedObject;
    	OnedayChip nextPicked = null;
    	if(po!=null)
    	{
    		OnedayCell ps = pickedSourceStack.pop();
    		switch(ps.rackLocation())
    		{
    		default: throw G.Error("Not expecting rackLocation %s",ps.rackLocation);
    		case RackLocation:
   		
    			if(ps.topChip()!=null)
    			{	nextPicked = ps.removeTop();	// swap the picked card
    			}
				//$FALL-THROUGH$
			case DrawPile:
    		case DiscardPile:
    		case StartingPile:
    		case BoardLocation: ps.addChip(po); break;
     		}
    		pickedObject = nextPicked;
     	}
     }

    // 
    // drop the floating object.
    //
    private void dropObject(OnedayCell c)
    {   G.Assert(pickedObject!=null,"pickedObject should not be null"); 
    	OnedayChip nextPicked = null;
    	switch(c.rackLocation())
		{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case RackLocation:
			if(c.topChip()!=null)
			{	pickedSourceStack.push(c);
				nextPicked = c.removeTop();
			}
			//$FALL-THROUGH$
		case DrawPile:	
		case StartingPile:
		case DiscardPile:
		case BoardLocation: c.addChip(pickedObject); break;
		}
    	pickedObject = nextPicked;
    	exposedCardStack.push(c.exposed?1:0);	// save if the cell was previously exposed
       	droppedDestStack.push(c);
       	
    }
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(OnedayCell cell)
    {	return((droppedDestStack.size()>0) && (droppedDestStack.top()==cell));
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.  
    public int movingObjectIndex()
    {	OnedayChip ch = pickedObject;
    	if(ch!=null)
    		{ return(ch.getNumber());
    		}
        return (NothingMoving);
    }
    // get the moving object and it's encoded source.  The source allows
    // the clients to conceal the card if appropriate
    public int publicMovingObjectIndex()
    {	int ch = movingObjectIndex();
    	if(ch>=0)
    	{
    		OnedayCell from = pickedSourceStack.top();
  		  	if(from!=null) { return(ch+from.rackLocation().ordinal()*1000); }
    	}
        return (NothingMoving);
    }
    public OnedayCell getCell(OneDayId source,char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case StartingPile:
        	return(startingPile[col-'A']);
        case RackLocation:
        	return(playerBoard[col-'A'].rack[row]);
        case DiscardPile:
        	return(discardPile[row]);
        case DrawPile:
        	return(drawPile);
        case BoardLocation:
        	return((OnedayCell)getCellArray()[row]);
        }
    }
    public OnedayCell getCell(OnedayCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(OnedayCell c)
    {	G.Assert(pickedObject==null,"pickedObject should be null");
    	switch(c.rackLocation())
    	{
		default: throw G.Error("Not expecting rackLocation %s",c.rackLocation);
		case DrawPile:
		case DiscardPile:
		case RackLocation:
		case StartingPile:
		case BoardLocation: 
			pickedObject = c.removeTop(); 
			break;
    	
    	}
    	pickedSourceStack.push(c);
   }

    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(OnedayCell target)
    {	undropState = board_state;
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case Confirm:
        case Draw:
        	setNextStateAfterDone(); 
        	break;
        case Place:
        	// place state doesn't use the pick/drop stack
        	if(racksFull()) { setState(resetState = OnedayState.NormalStart); }
        	break;
        case SynchronousPlace:
        	acceptPlacement();
        	if(rackFull(playerBoard[whoseTurn].rack))
        	{	setNextPlayer();
        		if(racksFull()) { setState(resetState = OnedayState.NormalStart); }
        	}
        	break;
        case Play:
        case PlayFromDraw:
        	resetState = board_state;
			//$FALL-THROUGH$
		case Discard:
			switch(target.rackLocation())
			{
			case DiscardPile:
				setState(OnedayState.Confirm); 
				break;
			case RackLocation:
				setState(OnedayState.Discard);
				break;
			default:
				break;
			}
			break;

        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(OnedayCell c)
    {	return(getSource()==c);
    }
    public boolean isAnySource(OnedayCell c)
    {
    	for(int i=0;i<pickedSourceStack.size(); i++)
    	{
    		if(pickedSourceStack.elementAt(i)==c) { return(true); }
    	}
    	return(false);
    }
    public OnedayCell getSource()
    {
    	return((pickedSourceStack.size()>0) ?pickedSourceStack.top() : null);
    }

    private boolean rackFull(OnedayCell r[])
    {
    	for(OnedayCell c : r) 
   		{	if(c.topChip()==null) { return(false); }
   		}
    	return(true);
    }
    public boolean rackFull(int pl) { return(rackFull(playerBoard[pl].rack)); } 
   	private boolean racksFull()
   	{
   		for(PlayerBoard p : playerBoard)
   		{ 
   			if(!rackFull(p.rack)) { return(false); }
   		}
   		return(true);
   	}
    private void setNextStateAfterDone()
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
     	case Gameover: 
    		break;

        case Draw:
        	setState(OnedayState.Gameover);
        	break;
    	case Puzzle:
    		if(racksFull()) { setState(resetState = OnedayState.Play); }
    		else { setState(resetState=SIMULTANEOUS_PLAY?OnedayState.Place:OnedayState.SynchronousPlace); }
    		break;
       	case Place:
       		setState(OnedayState.NormalStart);
       		break;
       	case SynchronousPlace:
     	case Confirm:
    	case PlayFromDraw:
    	case Play:
    	case Resign:
    		setState(resetState = OnedayState.Play);
    		break;
    	}

    }
   

    
    private void doDone()
    {	
        acceptPlacement();

        if (board_state==OnedayState.Resign)
        {	resigned[whoseTurn] = true;
        	win[whoseTurn] = false;
        	setNextPlayer();
        	if(players_in_game==2) { win[whoseTurn]=true; setState(OnedayState.Gameover); }
        	else { setNextStateAfterDone(); }
        }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
           	if(win1)  {  setState(OnedayState.Gameover); win[whoseTurn]=true; }
        	else {setNextPlayer(); setNextStateAfterDone(); }
        }
    }

   
    private OnedayChip stackHiddenCells()
    {  	OnedayChip saved = null;
    	// if we're in "place" or playFromDraw state, save the actual top of the draw pile
    	// because we will need to get to know it before placing it. 
    	switch(board_state)
    	{
    	case SynchronousPlace:
    	case Place:
			{	
			OnedayCell c = startingPile[myRobotPlayer];
			saved = c.topChip();
			// put something else in its place to so the height is correct
			if(saved!=null) { c.removeTop(); c.addChip(drawPile.removeTop()); }
			}
			break;
    	case PlayFromDraw:
    		saved = drawPile.removeTop();
			break;
		default: break;
    	}
    	for(int i=0;i<players_in_game;i++)
    	{	OnedayCell draw = startingPile[i];
    		OnedayCell[] rackRow = playerBoard[i].rack; 
    		// put all the cells that aren't exposed back in the drawpile 
    		for(int j = 0,lim = rackRow.length; j<lim; j++)
    		{	OnedayCell c = rackRow[j];
    			// mark empty cells as exposed to flag that their contents goes to the draw pile
    			if(c.topChip()==null) { drawPile.addChip(draw.removeTop()); c.exposed=true; }
    			else if(!c.exposed && (i!=myRobotPlayer)) { drawPile.addChip(c.removeTop()); }
    		}
    	}
    	return(saved);
    }
    
    private void unstackHiddenCells(OnedayChip saved)
    {	// process cells in the reverse order of stackHiddenCells
       	for(int i=players_in_game-1;i>=0;i--)
    	{

    	OnedayCell[] rackRow = playerBoard[i].rack; 
		OnedayCell draw = startingPile[i];
		// put a new random cell in the rack for each hidden cell
    	for(int j=rackRow.length-1; j>=0; j--)
    		{
    		OnedayCell c = rackRow[j];
    		if(c.exposed) { if(c.topChip()==null) { draw.addChip(drawPile.removeTop()); c.exposed=false; }}
    		else if(i!=myRobotPlayer){ c.addChip(drawPile.removeTop()); }
    		}
    	}
       	
    	if(saved!=null)
    	{	switch(board_state)
    		{	case SynchronousPlace:
    			case Place: 
    				OnedayCell c = startingPile[myRobotPlayer];
    				drawPile.addChip(c.removeTop());
    				c.addChip(saved);
    				break;
    			case PlayFromDraw:
    				drawPile.addChip(saved);
    				break;
    			default: break;
    		}
    	}
    	

    }
    //
    // this is used once per simulation run to re-randomize all the 
    // hidden information, so the robot isn't getting any hidden
    // benefit because the hidden cells contents are unavailable.
    // in the drawpile, and will be exposed by the simulation
    // 
    void reRandomize()
    {	OnedayChip saved = stackHiddenCells();
    
    	for(int i=drawPile.height()-1; i>=0 ;i--)
    	{	robotShuffleStack.push(drawPile.chipAtIndex(i));
    	}
    	drawPile.shuffle(robotRandom);
    	
    	unstackHiddenCells(saved);
     }
    
    private void undoReRandomize()
    {	OnedayChip saved = stackHiddenCells();
    	
    	int h = drawPile.height();
    	drawPile.reInit();
    	while(h-- > 0)
    	{
    		drawPile.addChip(robotShuffleStack.pop());
    	}
    	
    	unstackHiddenCells(saved);
    }
    private void undoShuffleDiscardPile(int lvl)
    {	
		while(robotCellStack.size()>lvl)
    	{
    		OnedayCell c = robotCellStack.pop();
    		OnedayChip s = robotStationStack.pop();
    		OnedayChip save = c.removeTop();
    		drawPile.removeTop();
    		c.addChip(s);
    		c.addChip(save);
    	}
    }

    //
    // if the draw pile is empty, reload from all but the top
    // of the discard piles and reshuffle
    // call this before drawing from the drawpile
    private void reshuffleDiscardPile()
    {
    	if(drawPile.topChip()==null)
    	{
    		for(OnedayCell ch : discardPile)
    		{	if(ch.topChip()!=null)
    			{
    			OnedayChip st = ch.removeTop();	// keep the top
    			while(ch.topChip()!=null)
    			{	OnedayChip top = ch.removeTop();
        			if(robotGame)
        				{robotCellStack.push(ch);
        				 robotStationStack.push(top);
        				}
    				drawPile.addChip(top);	// move to the drawpile
    			}
    			ch.addChip(st);		// add the top back
    			}
    		}
    		drawPile.shuffle(new Random(Digest()));
    	}
    }
    
    private void loadStartingPiles()
    {
    	for(int pl=0;pl<players_in_game;pl++)
    	{	int ct=0;
    		OnedayCell chips[] = playerBoard[pl].rack;
    		for(OnedayCell chip : chips) { if(chip.topChip()==null) { ct++; }}
    		OnedayCell priv = startingPile[pl];
    		// adjust the private drawpile for each player to contain the right number of cards
    		while(priv.height()>ct) { drawPile.addChip(priv.removeTop());  }
    		while(priv.height()<ct) { priv.addChip(drawPile.removeTop());  }
    	}
    	for(OnedayCell c : discardPile)
    	{ if(c.topChip()==null) 
    		{
    		if(c.topChip()==null) 
    			{ reshuffleDiscardPile();
    			  c.addChip(drawPile.removeTop()); 
    			}
    		}
    	}
    }
    public boolean Execute(commonMove mm,replayMode replay)
    { 	
    	switch(variation)
    	{
    	default: throw G.Error("Not expecting %s",variation);
    	case Safari:	return(ExecuteSafari(mm,replay));
    	case Standard:	return(ExecuteStandard(mm,replay));
    	}
    	
    }
    

    public void updateSimulations(long newSimulatedTime)	// now is the real clock
    {	if(running && newSimulatedTime>simulatedTime)
    	{
    	simulatedTime = newSimulatedTime;
    	
    	// launch new trains
    	for(int lim=launchers.size()-1; lim>=0; lim--)
    	{
    		Train tr = launchers.elementAt(lim).launch(simulatedTime);
    		if(tr!=null) 
    			{ trains.push(tr); 
    			  tr.start(simulatedTime);
    			}
    	}

    	// run existing trains
    	for(int lim=trains.size()-1; lim>=0; lim--)
    	{
    		Train t = trains.elementAt(lim);
    		t.update(simulatedTime);
    		if(t.isEnded()) 
    			{ 
    				for(PlayerBoard bd : playerBoard)
    				{
    					bd.update(simulatedTime);
    	}
    				trains.remove(t,false); 

    			} 
    	}
    	
    	// run the walking players, move between trains and platforms
    	for(PlayerBoard bd : playerBoard)
    		{
    		bd.update(simulatedTime);
    	}
    
    }
    }
    
    public boolean ExecuteSafari(commonMove mm,replayMode replay)
    {	OnedayMovespec m = (OnedayMovespec)mm;
    	switch(m.op)
    	{
    	default: throw G.Error("not expecting %s",mm);
    	case MOVE_RUN:
    		updateSimulations(m.timeStep);
    		break;
    	case MOVE_EXIT:
    		{
        	PlayerBoard pb = playerBoard[m.player];
        	pb.setExitTrain();
    		}
    		break;
    	case MOVE_BOARD:
    		{
    		PlayerBoard pb = playerBoard[m.player];
    		pb.setBoardTrain();
    		}
    		break;
    	case MOVE_WALK:
    		{
    			PlayerBoard pb = playerBoard[m.player];
    			pb.walkTo(m.location);
    		}
    		break;
    	case MOVE_START:
    		running = true;
    		setState(OnedayState.Running);
    		break;
    	case MOVE_EDIT:
    		setState(OnedayState.Puzzle);
    		running = false;
    	}
    	return(true);
    }
    public boolean ExecuteStandard(commonMove mm,replayMode replay)
    {	OnedayMovespec m = (OnedayMovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        //System.out.println("E "+m+" for "+whoseTurn + pickedObject);
        switch (m.op)
        {
        case MOVE_PASS:
        	// can occur in robot games one player has finished filling his rack 
        	// and the robot has not.
        	setNextPlayer();
        	break;
        case MOVE_DONE:
         	doDone();
            break;
        case NORMALSTART:
        	setState(OnedayState.Puzzle);
        	setNextStateAfterDone();
        	break;
        case MOVE_DRAW:
        	setState(resetState = OnedayState.PlayFromDraw); 
        	break;
        case MOVE_SHUFFLE:
        	reRandomize();
        	break;
        case MOVE_TO_RACK_AND_DISCARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting state %s",board_state);
        		case Play:
        		case PlayFromDraw:
        			G.Assert(pickedObject==null,"something is moving");
        			OnedayCell from = getCell(m.source,m.from_col,m.from_row);
        			OnedayCell to = getCell(OneDayId.RackLocation,m.to_col, m.to_row);
        			OnedayCell disc = getCell(OneDayId.DiscardPile,'@',m.discard);
        			if(replay.animate)
        			{
        				animationStack.push(to);		//save in reverse order
        				animationStack.push(disc);
        				animationStack.push(from);
        				animationStack.push(to);
        			}
        			pickObject(from);
        			dropObject(to);
        			m.exposed = to.exposed;
        			to.exposed = ((from.rackLocation==OneDayId.DiscardPile)
        							||(to.rackLocation==OneDayId.DiscardPile));
        			dropObject(disc);
        			reshuffleDiscardPile();
       			    setState(OnedayState.Confirm);
                    break;
        	}
        	break;
        case MOVE_TO_DISCARD:
           	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Play:
        		case PlayFromDraw:
        			G.Assert(pickedObject==null,"something is moving");
        			OnedayCell from = getCell(m.source,m.from_col,m.from_row);
        			OnedayCell to = getCell(OneDayId.DiscardPile,'@',m.discard); 
        			pickObject(from);
        			dropObject(to);
        			to.exposed = true;
           			if(replay.animate)
        			{
        				animationStack.push(from);
        				animationStack.push(to);
        			}

        			reshuffleDiscardPile();
       			    setState(OnedayState.Confirm);
                    break;
        	}
           	break;
         case MOVE_TO_RACK:
         case EPHEMERAL_TO_RACK:
        	switch(board_state)
        	{	default: throw G.Error("Not expecting robot in state %s",board_state);
        		case Place:
        		case SynchronousPlace:
        			// note that this can't use the picked object stack so it can be
        			// called asynchronously.
        			OnedayCell from = getCell(OneDayId.StartingPile, m.to_col, m.to_row);
           			OnedayCell target = getCell(OneDayId.RackLocation, m.to_col,m.to_row);
           			target.addChip(from.removeTop());
           			if(replay.animate)
        			{
        				animationStack.push(from);
        				animationStack.push(target);
        			}
    			    target.exposed = false;
       			    m.exposed = false;
       			    setNextStateAfterDrop(target);
        			break;
        	}
        	break;
        case EPHEMERAL_DROP:
        case MOVE_DROP: // drop on chip pool;
        	OnedayCell target = getCell(m.source, m.to_col, m.to_row);
            if(isSource(target))
            	{ 
            		unPickObject(); 
            		if(((resetState==OnedayState.Play)||(resetState==OnedayState.PlayFromDraw)) && (droppedDestStack.size()>0)) 
            			{// restore the previous state of the card exposed
            			 droppedDestStack.pop().exposed=exposedCardStack.pop()==1; 
            			}
            		setState(resetState);
            	}
            	else
            	{ 
            	OnedayCell src = getSource();
            	dropObject(target);	// remembers the previous exposed state
            	target.exposed = src.rackLocation==OneDayId.DiscardPile;
    			reshuffleDiscardPile();
    			setNextStateAfterDrop(target);
             	}
            break;
        case EPHEMERAL_PICK:
        case MOVE_PICK:
        	{
        	OnedayCell c = getCell(m.source, m.from_col, m.from_row);
        	if(isDest(c)) 
        		{ unDropObject(); 
        		  setState(undropState);
        		}
        	else
        	{
            pickObject(c);
            if((c.rackLocation==OneDayId.DrawPile)&&(board_state!=OnedayState.Puzzle)) 
            	{ setState(resetState = OnedayState.PlayFromDraw); }
            else { resetState = board_state; }
        	}}
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(OnedayState.Puzzle);
            {	boolean win1 = WinForPlayerNow(whoseTurn);
             	if(win1) { setState(OnedayState.Gameover); win[whoseTurn]=true; }
            	else
            	{	loadStartingPiles();
            		setNextStateAfterDone(); 
            	}
            }
            break;

        case MOVE_RESIGN:
        	setState(unresign==null?OnedayState.Resign:unresign);
            break;
        case MOVE_EDIT:
    		acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
    		// standardize "gameover" is not true
            setState(OnedayState.Puzzle);
 
            break;
        case MOVE_GAMEOVERONTIME:
      	   win[whoseTurn] = true;
      	   setState(OnedayState.Gameover);
      	   break;

        default:
        	cantExecute(m);
        }

 
        return (true);
    }
    private OnedayCell getEmptyDiscard()
    {
    	for(OnedayCell c : discardPile) { if(c.topChip()==null) { return(c); }}
    	return(null);
    }
    // legal to hit the chip storage area
    public boolean LegalToHitChips(int player,OnedayCell c)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Running:
        case Resign:
        case NormalStart:
        	return(false);	// no user actions, handled by the viewer
         case Confirm:
        	 return(isDest(c)); 
         case Draw:
         case Place:
         case SynchronousPlace:
        	 switch(c.rackLocation())
        	 {
        	 default: return(false);
        	 case StartingPile: return((pickedObject==null)?(c.topChip()!=null):true);
        	 case RackLocation:
        		 return((pickedObject!=null) && (c.topChip()==null));
        	 }
         case PlayFromDraw:
        	 if(pickedObject==null)
        	 {
        	 switch(c.rackLocation())
        	 	{
        	 	case DrawPile: return(true);
        	 	default: isDest(c);
        	 	}
        	 }
        	 else
        	 {	if(c.rackLocation==OneDayId.DrawPile) { return(false); }
        	 	if(isSource(c)) {return(true); }
        	 	switch(c.rackLocation())
        	 	{
        	 	case DiscardPile:
            	 	OnedayCell emp = getEmptyDiscard();
            	 	if(emp!=null) { return(c==emp); }
            	 	return(true);
        	 	case RackLocation: 
        	 		return(player==whoseTurn);
				default:
					break;
        	 	}
         	 }
        	 return(false);
         case Discard:
         	{
        	 if(isSource(c)) { return(true); }
        	 OnedayCell em = getEmptyDiscard();
        	  return((c.rackLocation==OneDayId.DiscardPile)
        			 && (em==null)?true:c==em);
         	}
         case Play: 
        	 if(pickedObject==null)
        	 {
        	 switch(c.rackLocation())
        	 	{
        	 	case DrawPile:
        	 	case DiscardPile: return(true);
        	 	default: isDest(c);
        	 	}
        	 }
        	 else
        	 {	if(c.rackLocation==OneDayId.DrawPile) { return(false); }
        	 	if(isSource(c)) {return(true); }
        	 	switch(c.rackLocation())
        	 	{
        	 	case DiscardPile:
            	 	OnedayCell emp = getEmptyDiscard();
            	 	if(emp!=null) { return(c==emp); }
            	 	return(true);
        	 	case RackLocation: 
        	 		return(player==whoseTurn);
				default:
					break;
        	 	}
         	 }
        	 return(false);
		case Gameover:
			return(false);
        case Puzzle:
        	return((pickedObject==null)
        			? (c.height()>0)
        			: ((c.rackLocation==OneDayId.RackLocation) ? (c.height()==0) : true)
        			);
        }
    }
  
    // true if it's legal to drop something  originating from fromCell on toCell
    public boolean LegalToDropOnBoard(OnedayCell fromCell,OnedayChip chip,OnedayCell toCell)
    {	
		return(false);

    }
    public boolean LegalToHitBoard(OnedayCell cell)
    {	
        switch (board_state)
        {
 		case Play:
			return(LegalToDropOnBoard(pickedSourceStack.top(),pickedObject,cell));

		case Gameover:
			return(false);
		case Confirm:
		case Draw:
			return(isDest(cell));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
        	return(pickedObject==null?(cell.chipIndex>0):true);
        }
    }
  public boolean canDropOn(OnedayCell cell)
  {		OnedayCell top = (pickedObject!=null) ? pickedSourceStack.top() : null;
  		return((pickedObject!=null)				// something moving
  			&&(top.onBoard 			// on the main board
  					? (cell!=top)	// dropping on the board, must be to a different cell 
  					: (cell==top))	// dropping in the rack, must be to the same cell
  				);
  }
 
  StateStack robotState = new StateStack();
  IStack robotLevel = new IStack();
  
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(OnedayMovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        robotLevel.push(robotCellStack.size());
        // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
        	if(DoneState()) { doDone(); }
        }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(OnedayMovespec m)
    {
        //System.out.println("U "+m+" for "+whoseTurn);
        
        undoShuffleDiscardPile(robotLevel.pop());

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_SHUFFLE:
   	    	undoReRandomize();
   	    	break;
        case NORMALSTART:
        case MOVE_DONE:
        case MOVE_PASS:
        case MOVE_DRAW:
            break;
        case MOVE_TO_RACK_AND_DISCARD:
           	{G.Assert(pickedObject==null,"something is moving");
        	pickObject(getCell(OneDayId.DiscardPile,'@',m.discard));
        	OnedayCell rackCell = getCell(OneDayId.RackLocation,m.to_col, m.to_row);
        	dropObject(rackCell);      
        	rackCell.exposed = m.exposed;
        	dropObject(getCell(m.source,m.from_col,m.from_row));
       		acceptPlacement();
           	}
        	break;
        case MOVE_TO_DISCARD:
           	{G.Assert(pickedObject==null,"something is moving");
       		pickObject(getCell(OneDayId.DiscardPile,'@',m.discard));
       		dropObject(getCell(m.source,m.from_col,m.from_row));
       	    acceptPlacement();
           	}
         	break;
       case MOVE_TO_RACK:
        	{G.Assert(pickedObject==null,"something is moving");
        	OnedayCell rackCell = getCell(OneDayId.RackLocation, m.to_col,m.to_row);
       		pickObject(rackCell); 
       		rackCell.exposed = m.exposed;
        	dropObject(getCell(OneDayId.StartingPile, m.to_col, m.to_row));
       		acceptPlacement();
        	}
         	break;
        case MOVE_RESIGN:
            break;
        }
        setState(robotState.pop());
        if(whoseTurn!=m.player)
        {  	moveNumber--;
         	setWhoseTurn(m.player);
        }
 }

 void addPlaceMoves(CommonMoveStack  all,int who)
 {	OnedayCell r[] =playerBoard[who].rack;
 	boolean added = false;
 	for(OnedayCell c : r)
 	{
 		if(c.topChip()==null)
 		{	added = true;
 			all.push(new OnedayMovespec(((board_state==OnedayState.Place)?EPHEMERAL_TO_RACK:MOVE_TO_RACK),c.col,c.row,who));
 		}
 	}
 	if(!added)
 	{	// since the real game is running asynchronous moves, and we are running 
 		// synchronous moves for the robot, if a player has finished early he will
 		// have nothing to do.
 		all.push(new OnedayMovespec(MOVE_PASS,who));
 	}
 }
 private void addPlayMovesFrom(OnedayCell c,OnedayCell disc,CommonMoveStack  all,int who)
 {	for(OnedayCell d : playerBoard[who].rack)
 		{
 			all.push(new OnedayMovespec(MOVE_TO_RACK_AND_DISCARD,c.rackLocation(),c.col,c.row,d.col,d.row,disc.row,who));
 		}
 	if((c.rackLocation==OneDayId.DiscardPile) && (c.row==disc.row)) {}	// disallow move discard to self
 	else { all.push(new OnedayMovespec(MOVE_TO_DISCARD,c.rackLocation(),c.col,c.row,disc.row,who));}
 }
 
 private void addPlayFromDrawMoves(CommonMoveStack  all,int who)
 {
	 for(OnedayCell d : discardPile) { addPlayMovesFrom(drawPile,d,all,who); }
	 
 }
 void addPlayMoves(CommonMoveStack  all,int who)
 {
	 all.push(new OnedayMovespec(MOVE_DRAW,who));
	 // drop from drawpile to any discard pile
	 for(OnedayCell c : discardPile)
	 	{ if(c.height()==1) { addPlayMovesFrom(c,c,all,who); }
	 		else { for(OnedayCell d : discardPile) { addPlayMovesFrom(c,d,all,who); }}
	 	}
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	switch(board_state)
 	{
 	case NormalStart:
 		all.push(new OnedayMovespec(NORMALSTART,whoseTurn));
 		break;
 	case Place:
 	case SynchronousPlace:
 		addPlaceMoves(all,whoseTurn);
 		break;
 	case PlayFromDraw:
 		addPlayFromDrawMoves(all,whoseTurn);
 		break;
 	case Play:
 		addPlayMoves(all,whoseTurn);
 		break;
 	default:  	throw G.Error("Not implemented");
 	}
 	G.Assert(all.size()>0,"No moves");
  	return(all);
 }
@Override
public int cellToX(OnedayCell c) {
	OnedayChip top = c.topChip();
	if((top!=null) && top.isStation() && (boardRect!=null))
	{
		return((int)(G.Width(boardRect)*((Station)top).xpos/100));
	}
	return 0;
}
@Override
public int cellToY(OnedayCell c) {
	OnedayChip top = c.topChip();
	if((top!=null)&&top.isStation() && (boardRect!=null))
	{
		return((int)(G.Height(boardRect)*(1.0-((Station)top).ypos/100)));
	}
	return 0;
}

 
}
