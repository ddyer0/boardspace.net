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
package manhattan;

import static manhattan.ManhattanMovespec.*;

import java.awt.Rectangle;

import java.util.*;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * ManhattanBoard knows all about the game of Manhattan
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves. 
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
 *
 */

class ManhattanBoard extends RBoard<ManhattanCell>	// for a square grid board, this could be rectBoard or squareBoard 
	implements BoardProtocol,ManhattanConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	
	/**
	 * contect saves imformation that's needed to return after executing an out of turn action.
	 * there aren't many of these in Manahattan, mainly initial worker selection and "french"
	 * bomb discards.
	 */
	class Context 
	{
		ManhattanState state;
		int whoseTurn;
		Benefit benefit;
		Cost cost;
		public Context()
		{
			state = ManhattanBoard.this.board_state;
			whoseTurn = ManhattanBoard.this.whoseTurn;
			benefit = ManhattanBoard.this.pendingBenefit;
			cost = ManhattanBoard.this.pendingCost;
		}
	}
	class ContextStack extends OStack<Context>
	{
		public Context[] newComponentArray(int sz) {
			return new Context[sz];
		}
		public void save() { push(new Context()); }
		public void restore() 
		{ 	if(size()>0)
			{
				Context c = pop();
				board_state = c.state;
				whoseTurn = c.whoseTurn;
				pendingBenefit = c.benefit;
				pendingCost = c.cost;
			}
		}
	}
	ContextStack contextStack = new ContextStack();
	Benefit pendingBenefit = null;
	Cost pendingCost = null;
	ManhattanVariation variation = ManhattanVariation.manhattan;
	private ManhattanState board_state = ManhattanState.Puzzle;	
    private AfterWorkerDispatch afterWorkerDispatch = null;
    int espionageSteps = 0;
    int northKoreanPlayer = 0;
    int northKoreaSteps = 0;
	private ManhattanState unresign = null;	// remembers the orignal state when "resign" is hit
	private StateStack robotState = new StateStack();
	public ManhattanState getState() { return(board_state); }
	private Random cellCounter = new Random(2424264);
	private ManhattanCell C(ManhattanId id,Type t, Cost required,Benefit bene) 
	{ ManhattanCell c = new ManhattanCell(id,required,bene); 
	  c.onBoard = true;
	  c.next = allCells;
	  c.type = t;
	  allCells = c;
	  c.color = MColor.Board;
	  c.col = '@';
	  c.row = 0;
	  c.randomv = cellCounter.nextLong();
	  return c;
	}
	private ManhattanCell[] CA(ManhattanId id,Type t,Cost[] requires,Benefit[]bene)
	{	ManhattanCell ca[] = new ManhattanCell[requires.length];
		for(int i=0;i<requires.length;i++) 
		{ ManhattanCell c = ca[i] = new ManhattanCell(id,requires[i],bene[i],i); 
		  c.onBoard = true;
		  c.next = allCells;
		  c.col = '@';
		  c.row = i;
		  c.color = MColor.Board;
		  c.type = t;
		  c.randomv = cellCounter.nextLong();
		  allCells = c;
		}
		return ca;
	}
	private ManhattanCell[] CA(ManhattanId id,Type t,int n,Cost requires,Benefit bene)
	{	ManhattanCell ca[] = new ManhattanCell[n];
		for(int i=0;i<n;i++) 
		{ ManhattanCell c = ca[i] = new ManhattanCell(id,requires,bene,i); 
		  c.onBoard = true;
		  c.next = allCells;
		  c.color = MColor.Board;
		  c.col = '@';
		  c.row = i;
		  c.type = t;
		  allCells = c;
		}
		return ca;
	}
	// choices is a scratch array for UI presentations
	// size of choices is dictated by the number personalities or of bomb designs (players+1)
	// plus one for france
	ManhattanCell choice[] = CA(ManhattanId.Select,Type.Other,7,Cost.None,Benefit.None);
	// choice out is a secondary scratch used to indicate a second row of chocies in the gui
	// brazil uses it.
	ManhattanCell choiceOut[] = CA(ManhattanId.SelectOut,Type.Other,4,Cost.None,Benefit.None);
	
	int repairCounter = 0;
	int needScientists = 0;
	int needEngineers = 0;
	int bombDesigner = 0;
	CellStack selectedCells = new CellStack();
	ChipStack selectedChips = new ChipStack();
	
	public boolean isSelected(ManhattanCell c) { return selectedCells.contains(c); }
	public boolean isSelected(ManhattanChip c) { return selectedChips.contains(c); }
	public int winningScore()
	{
		return WinningScore[players_in_game];
	}
	public int scoreForPlayer(int p)
	{
		return pbs[p].calculateScore();
	}
	// cells on the main board
	ManhattanCell seeBribe = C(ManhattanId.SeeBribes,Type.Coin, Cost.None,Benefit.None);
	ManhattanCell seeBank = C(ManhattanId.Bank,Type.Coin,Cost.FixedPool,Benefit.None);
	ManhattanCell seeDamage = C(ManhattanId.Damage,Type.Damage,Cost.FixedPool,Benefit.None);
	ManhattanCell seeYellowcake = C(ManhattanId.Yellowcake,Type.Yellowcake,Cost.FixedPool,Benefit.None);
	ManhattanCell availableWorkers = C(ManhattanId.AvailableWorkers,Type.WorkerPool, Cost.None,Benefit.None);
	ManhattanCell seeEspionage[] = CA(ManhattanId.SeeEspionage,Type.Marker,7, Cost.None,Benefit.None);
	ManhattanCell seeBuilding[] = CA(ManhattanId.Building,Type.BuildingMarket,7, Cost.Cash,Benefit.SelectedBuilding);
	ManhattanCell seeBuildings = C(ManhattanId.SeeBuildingPile,Type.Building, Cost.None,Benefit.None);
	ManhattanCell seeBombs = C(ManhattanId.SeeBombPile,Type.Bomb, Cost.None,Benefit.Inspect);
	ManhattanCell seeDiscardedBombs = C(ManhattanId.SeeDiscardedBombPile,Type.Bomb, Cost.None,Benefit.Inspect);
	ManhattanCell seePersonalities = C(ManhattanId.SeePersonalityPile,Type.Personalities, Cost.None,Benefit.Inspect);
	ManhattanCell seeNations = C(ManhattanId.SeeNationsPile,Type.Nations, Cost.None,Benefit.Inspect);
	ManhattanCell seeBombtests = C(ManhattanId.Bombtest	,Type.Bombtest, Cost.None,Benefit.Inspect);
	ManhattanCell availablePersonalities = C(ManhattanId.Personality,Type.Personalities,Cost.None,Benefit.Inspect);
	ManhattanCell seeUranium[] = CA(ManhattanId.SeeUranium,Type.Marker,9, Cost.None,Benefit.None);
	ManhattanCell seePlutonium[] = CA(ManhattanId.SeePlutonium,Type.Marker,9, Cost.None,Benefit.None);
	// places to play on the main board
	ManhattanCell playEspionage = C(ManhattanId.Espionage,Type.Worker, Cost.AnyWorkerAnd3,Benefit.Espionage);
	ManhattanCell playMine[] = CA(ManhattanId.Mine,Type.Worker,
								new Cost[]{Cost.AnyWorkerAnd5,Cost.AnyWorker,Cost.Engineer},
								new Benefit[]{Benefit.Yellowcake4,Benefit.Yellowcake3And1,Benefit.Yellowcake2});
	ManhattanCell playUniversity[] = CA(ManhattanId.University,Type.Worker,
				new Cost[] {Cost.AnyWorker,Cost.AnyWorker,Cost.AnyWorker,Cost.AnyWorkerAnd3},
				new Benefit[] {Benefit.Worker3,Benefit.Engineer,Benefit.Scientist,Benefit.ScientistOrEngineer});
	ManhattanCell playDesignBomb = C(ManhattanId.DesignBomb,Type.Worker, Cost.ScientistAndEngineerAndBombDesign,Benefit.BombDesign);
	ManhattanCell bombtestHelp = C(ManhattanId.BombHelp,Type.Help, Cost.None,Benefit.None);
	ManhattanCell seeCurrentDesigns = C(ManhattanId.CurrentDesigns,Type.Other, Cost.None,Benefit.Inspect);
	ManhattanCell playMakePlutonium = C(ManhattanId.MakePlutonium,Type.Worker, Cost.ScientistAnd2Y,Benefit.MainPlutonium);
	ManhattanCell playMakeUranium = C(ManhattanId.MakeUranium,Type.Worker, Cost.ScientistAnd2YAnd3,Benefit.MainUranium);
	ManhattanCell playAirStrike[] = CA(ManhattanId.AirStrike,Type.Worker,2, Cost.AnyWorker,Benefit.Airstrike);
	ManhattanCell playRepair = C(ManhattanId.Repair,Type.Worker, Cost.AnyWorkerAnd5,Benefit.Repair);
	ManhattanCell playMakeFighter = C(ManhattanId.MakeFighter,Type.Worker, Cost.AnyWorker,Benefit.Fighter2);
	ManhattanCell playMakeBomber = C(ManhattanId.MakeBomber,Type.Worker, Cost.AnyWorker,Benefit.Bomber2);
	ManhattanCell playMakeMoney[] = CA(ManhattanId.MakeMoney,Type.Worker,
			new Cost[] {Cost.AnyWorkerAnd3Y,Cost.AnyWorker,Cost.ScientistOrEngineer},
			new Benefit[]{Benefit.FiveAnd1,Benefit.FiveAnd2,Benefit.ThreeAnd1});
	ManhattanCell playBuyBuilding = C(ManhattanId.BuyWithEngineer,Type.Worker,Cost.EngineerAndMoney,Benefit.SelectedBuilding);
	ManhattanCell playBuyBuilding2 = C(ManhattanId.BuyWithWorker,Type.Worker,Cost.ScientistOrWorkerAndMoney,Benefit.SelectedBuilding);
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(ManhattanState st) 
	{ 	unresign = (st==ManhattanState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

     // boards in color order
    private PlayerBoard playerBoardOrder[] = new PlayerBoard[ManhattanChip.playerColors.length];
    // player boards by board index
    PlayerBoard pbs[] = null;
	Bitset<Options> options = new Bitset<Options>();
	public boolean testOption(Options op)
	{
		return options.test(op);
	}
    
    // get the chip pool and chip associated with a player.  these are not 
    // constants because of the swap rule.
	public ManhattanChip getPlayerChip(int p) { return(pbs[p].chip); }
	public MColor getPlayerColor(int p) { return(pbs[p].color); }	
	public ManhattanChip getCurrentPlayerChip() { return(getPlayerChip(whoseTurn)); }
	public PlayerBoard getPlayerBoard(int n) { return(pbs[n]);}
	public PlayerBoard getCurrentPlayerBoard() { return pbs[whoseTurn]; }
	public PlayerBoard getPlayerBoard(MColor c)
	{
		for (PlayerBoard pb : pbs) { if(pb.color==c) { return pb; }}
		return null;
	}
	public ManhattanPlay robot = null;
	
	 public boolean p1(String msg0)
		{	String msg = msg0.replace(':','-');
			msg = msg.replace('<','-');
			msg = msg.replace('>','-');
			if(G.p1(msg) && robot!=null)
			{	String dir = "g:/share/projects/boardspace-html/htdocs/manhattan/manhattangames/robot/";
				robot.saveCurrentVariation(dir+msg+".sgf");
				return(true);
			}
			return(false);
		}
	
// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() { setState(ManhattanState.Draw); }	CellStack animationStack = new CellStack();
 
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public ManhattanChip pickedObject = null;
    public ManhattanChip lastPicked = null;
    public int dropSequence = 0;
    CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private IStack pickedIndex = new IStack();
    private IStack droppedIndex = new IStack();
    private StateStack stateStack = new StateStack();
    private LStack optionStack = new LStack();
    public ManhattanCell displayCell = null;		// cell for the gui to display in a "select" dialog
    public CellStack displayCells = new CellStack();
    
    // save strings to be shown in the game log
    StringStack gameEvents = new StringStack();
    InternationalStrings s = G.getTranslations();
    
 	void logGameEvent(String str)
 	{	if(robot==null)
 		{
 		 gameEvents.push(str);
 		}
 	}

    private ManhattanState resetState = ManhattanState.Puzzle; 
    public DrawableImage<?> lastDroppedObject = null;	// for image adjustment logic

	
	// constructor 
    public ManhattanBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
         
        for(int i=0;i<ManhattanChip.playerColors.length;i++)
        {
        	playerBoardOrder[i] = new PlayerBoard(this,(char)('A'+i),
        			ManhattanChip.playerColors[i],
        			ManhattanChip.playerChips[i],
        			ManhattanChip.playerBoards[i],
        			ManhattanChip.playerFighters[i],
        			ManhattanChip.playerBombers[i],
        			ManhattanChip.laborers[i],
        			ManhattanChip.scientists[i],
        			ManhattanChip.engineers[i]);
        	
        }
        setColorMap(map, players);
        
		// do this once at construction

        doInit(init,key,players,rev); // do the initialization 
    }
    
    public String gameType() 
    { StringBuilder b = new StringBuilder();
      G.append(b,gametype," ",players_in_game," ",randomKey," ",revision);
      for(Options op : Options.values())
      {
    	  G.append(b," ",op," ",testOption(op) ? "true" : "false");
      }
      G.append(b," ","end");
      return b.toString();
    }
    

    public void doInit(String gtype,long key)
    {
    	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	options.clear();
    	parseOptions(tok);
    	
    	doInit(typ,ran,np,rev);
    }
    public void parseOptions(StringTokenizer tok)
    {
    	while(tok.hasMoreTokens())
    	{	String nx = tok.nextToken();
    		if("end".equals(nx)) { break; }
    		Options op = Options.valueOf(nx);
    		boolean v = G.BoolToken(tok);
    		if(v) { options.set(op); }
    	}
    }
    private void add4IfInUse(ManhattanChip c)
    {
    	PlayerBoard pb = getPlayerBoard(c.color);
    	if(pb!=null)
    	{
    		add4(c);
    	}
    }
    private void add4(ManhattanChip c)
    {
    	for(int i=0;i<4;i++) { availableWorkers.addChip(c); }
    }
    
	private int getAvailableWorkerIndex(MColor c,WorkerType t)
	{
		for(int lim=availableWorkers.height()-1; lim>=0; lim--)
		{
			ManhattanChip ch = availableWorkers.chipAtIndex(lim);
			if(ch.workerType==t && ch.color==c)
			{	return lim;
			}
		}
		return -1;
	}
	boolean hasAvailableWorker(MColor c,WorkerType t)
	{	return getAvailableWorkerIndex(c,t)>=0;
	}
	ManhattanChip getAvailableWorker(MColor c,WorkerType t)
	{	int ind = getAvailableWorkerIndex(c,t);
		if(ind>=0)
		{
				return availableWorkers.removeChipAtIndex(ind);
		}
		return null;
	}
	int countAvailableWorkers(MColor c,WorkerType t)
	{
		int n=0;
		for(int lim=availableWorkers.height()-1; lim>=0; lim--)
		{
			ManhattanChip ch = availableWorkers.chipAtIndex(lim);
			if(ch.workerType==t && ch.color==c)
			{	n++;
			}
		}
		return n;
	}
	int buildingCost[] = {2,3,5,7,10,14,20};

	// shuffle the building array after one has been removed;
	public void shuffleBuildings(ManhattanCell selected,replayMode replay)
	{
		int last = seeBuilding.length-1;
		// it's theoretically possible we could run out and have some empty slots
		for(int i=selected.row;i<last && !seeBuilding[i+1].isEmpty(); i++)
			{
			seeBuilding[i].addChip(seeBuilding[i+1].removeTop());
			if(replay.animate) 
				{
				animationStack.push(seeBuilding[i+1]);
				animationStack.push(seeBuilding[i]);
				}
			}
		if(seeBuildings.topChip()!=null)
		{
		// it's theoretically possible we could run out.
		seeBuilding[last].addChip(seeBuildings.removeTop());
		if(replay.animate)
		{
			animationStack.push(seeBuilding[last]);
			animationStack.push(seeBuildings);
		}}
	}
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	// magic numbers
    	// 317628249 japan+north korea
    	// 2 -1349594329 india pakistan
    	// 2  127441678 germany usa
    	// 2 -1539186274 israel usa
    	// 2  485836965 100 australia france
    	//key = 317628249 ;
    	randomKey = key;
    	dropMoveNumber = -1;
    	pickMoveNumber = -1;
    	prevLastPicked = -1;
    	prevLastDropped = -1;
    	adjustRevision(rev);
    	players_in_game = players;
    	win = new boolean[players];
    	contextStack.clear();
		options.set(Options.Personalities);
		options.set(Options.Nations);
    	dropSequence = 0;
    	// pbs is the player board indexed by boardindex, mapped to the player's preferred color
        int map[] = getColorMap();
        pbs = new PlayerBoard[players];
        for(int i=0;i<players;i++) 
        	{ PlayerBoard pb = pbs[i] = playerBoardOrder[map[i]]; 
        	  pb.boardIndex = i;
        	  pb.doInit();
        	  
        	}
        
        Random r = new Random(randomKey);
        seeBribe.reInit();
        seeBank.reInit();
        seeBank.addCash(8);
        
        seeDamage.reInit();
        seeDamage.addChip(ManhattanChip.Damage);
        
        seeYellowcake.reInit();
        seeYellowcake.addChip(ManhattanChip.Yellowcake);
        seeYellowcake.addChip(ManhattanChip.Yellowcake);
        
        reInit(choice);
        reInit(choiceOut);
        repairCounter = 0;
        bombDesigner = 0;
        needScientists = 0;
        needEngineers = 0;
        selectedCells.clear();
        selectedChips.clear();
        pendingBenefit = Benefit.None;
        pendingCost = Cost.None;
        reInit(seeBuilding);
        for(int i = 0; i<seeBuilding.length;i++)
        {
        	seeBuilding[i].cash = buildingCost[i];
        }
        
        availableWorkers.reInit();
        for(ManhattanChip c : ManhattanChip.laborers)
        	{ add4IfInUse(c);
        	}
        for(ManhattanChip c : ManhattanChip.engineers) 
    	{ add4IfInUse(c);
    	}
        for(ManhattanChip c : ManhattanChip.scientists)
    	{ add4IfInUse(c);
    	}   
        for(ManhattanChip c : ManhattanChip.GreyGuys)
        {	add4(c);
        	
        }
        // randomize and place the starting buildings
        seeBuildings.reInit();
        for(ManhattanChip ch : ManhattanChip.startingBuildings) { seeBuildings.addChip(ch); }
        seeBuildings.shuffle(r);
        int nstarters = ManhattanChip.startingBuildings.length;
        for(int i=0;i<nstarters;i++) { seeBuilding[i].addChip(seeBuildings.chipAtIndex(i)); }
        
        // randomize and place the first 2 regular buildings
        seeBuildings.reInit();
        for(ManhattanChip ch : ManhattanChip.buildings) { seeBuildings.addChip(ch); }
        seeBuildings.shuffle(r);
        for(int i=nstarters;i<seeBuilding.length;i++) { seeBuilding[i].addChip(seeBuildings.removeTop()); }
        
        seeBombs.reInit();
        for(ManhattanChip ch : ManhattanChip.bombs)  { seeBombs.addChip(ch); }
        seeBombs.shuffle(r);
        seeDiscardedBombs.reInit();
        
        seePersonalities.reInit();
        availablePersonalities.reInit();
        for(ManhattanChip ch : ManhattanChip.Personalities)  
        { seePersonalities.addChip(ch); 
          if(ch.type==Type.Personalities) { availablePersonalities.addChip(ch); }
        }
 
        seeNations.reInit();
        for(ManhattanChip ch : ManhattanChip.Nations)  { seeNations.addChip(ch); }
        if(options.test(Options.Nations))
        {
        seeNations.shuffle(r);
        for(PlayerBoard pb : pbs) 
        	{ pb.findBuildingSlot().addChip(seeNations.removeTop());  }
        }
        
        seeBombtests.reInit();
        // the set of bomb test cards varies depending on the number of players
        for(int i : UseBombTests[players_in_game-2]) { seeBombtests.addChip(ManhattanChip.BombTests[i]); }
        
        reInit(seeEspionage);
        reInit(seeUranium);
        reInit(seePlutonium);
        for(PlayerBoard p : pbs)
        {
        	seeUranium[0].addChip(p.chip);
        	seePlutonium[0].addChip(p.chip);
        	seeEspionage[0].addChip(p.chip);
        }
        
        playEspionage.reInit();
        reInit(playMine);
        reInit(playUniversity);
        playDesignBomb.reInit();
        bombtestHelp.reInit();
        bombtestHelp.addChip(ManhattanChip.Question);
        seeCurrentDesigns.reInit();
        loadNewDesigns(replayMode.Replay);
         
        playMakePlutonium.reInit();
        playMakeUranium.reInit();
        reInit(playAirStrike);
        playRepair.reInit();
        playMakeFighter.reInit();
        playMakeBomber.reInit();
        reInit(playMakeMoney);
        playBuyBuilding.reInit();
        playBuyBuilding2.reInit();
        
 		setState(ManhattanState.Puzzle);
		variation = ManhattanVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		robotState.clear();
		gametype = gtype;

	    
	    whoseTurn = FIRST_PLAYER_INDEX;
	    droppedDestStack.clear();
	    pickedSourceStack.clear();
	    pickedIndex.clear();
	    droppedIndex.clear();
	    stateStack.clear();
	    reInit(choice);
	    reInit(choiceOut);
	    for(PlayerBoard pb : pbs)
	    {	// give everyone 4 regular workers
	    	pb.addCoinsFromBank(pb.cashDisplay,10,replayMode.Replay);
	    	for(int i=0;i<4;i++)
	    		 { pb.workers.addChip(getAvailableWorker(pb.color,WorkerType.L)); 
	    		 }
	    }
	    pickedObject = null;
	    resetState = null;
	    lastDroppedObject = null;
	    animationStack.clear();
        moveNumber = 1;
        int bonuscash[] = new int[] { 0,2, 4, 2, 4 };
        
        for(int i=0,fp = whoseTurn;i<players_in_game;i++)
        {
        	PlayerBoard pb = pbs[fp];
        	pb.addCoinsFromBank(pb.cashDisplay,bonuscash[i],replayMode.Replay);     
        	fp = nextPlayer(fp);
        }
        afterWorkerDispatch = null;
        displayCell = null;
        espionageSteps = 0;
        northKoreanPlayer = 0;
        northKoreaSteps = 0;
        displayCells.clear();
        
        // this bit caused quite a bit of trouble, because it causes 
        // out-of-turn moves to be triggered when the game starts.
        if(players_in_game>3)
        {	for(int lim=players_in_game-1; lim>=3;lim--)
        	pbs[lim].pendingChoices.push(Benefit.ScientistOrEngineer);   
        }

        gameEvents.clear();
        // note that firstPlayer is NOT initialized here
    }
    /**
     * load the available bombs into the display, but only if there are enough
     * to completely fill the display.
     * @param replay
     */
    public void loadNewDesigns(replayMode replay)
    {	int needed = players_in_game+1-seeCurrentDesigns.height();
        if(needed>0 && nBombsAvailable()>=needed)
        {
        // only load bombs if fully available
        for(int i=0;i<needed;i++) 
        	{ 	ManhattanChip bomb = getABomb();
        		seeCurrentDesigns.addChip(bomb); 
        		if(replay.animate)
        		{
        			animationStack.push(seeBombs);
        			animationStack.push(seeCurrentDesigns);
        		}
        	}}
    }
    /** create a copy of this board */
    public ManhattanBoard cloneBoard() 
	{ ManhattanBoard dup = new ManhattanBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((ManhattanBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ManhattanBoard from_b)
    {
    	rawCopyFrom(from_b);
    	if(G.debug())
    	{
    	long d1 = Digest();
    	doInit();
    	rawCopyFrom(from_b);
    	long d2 = Digest();
    	G.Assert(d1==d2,"init copyfrom mismatch");
    	}
    }
    private void rawCopyFrom(ManhattanBoard from_b)
    {    	
        super.copyFrom(from_b);
        for(int i=0;i<players_in_game;i++) { pbs[i].copyFrom(from_b.pbs[i]); }
        dropMoveNumber = from_b.dropMoveNumber;
        pickMoveNumber = from_b.pickMoveNumber;
        prevLastPicked = from_b.prevLastPicked;
        prevLastDropped = from_b.prevLastDropped;
        contextStack.copyFrom(from_b.contextStack);
        seeBribe.copyFrom(from_b.seeBribe);
        copyFrom(choice,from_b.choice);
        copyFrom(choiceOut,from_b.choiceOut);
        repairCounter = from_b.repairCounter;
        bombDesigner = from_b.bombDesigner;
        needScientists = from_b.needScientists;
        needEngineers = from_b.needEngineers;
        
        getCell(selectedCells,from_b.selectedCells);
        selectedChips.copyFrom(from_b.selectedChips);
        pendingBenefit = from_b.pendingBenefit;
        pendingCost = from_b.pendingCost;
        availableWorkers.copyFrom(from_b.availableWorkers);
        copyFrom(seeEspionage,from_b.seeEspionage);
        copyFrom(seeBuilding,from_b.seeBuilding);
        seeBuildings.copyFrom(from_b.seeBuildings);
        seeBombs.copyFrom(from_b.seeBombs);
        seeDiscardedBombs.copyFrom(from_b.seeDiscardedBombs);
        availablePersonalities.copyFrom(from_b.availablePersonalities);
        seeNations.copyFrom(from_b.seeNations);
        seeBombtests.copyFrom(from_b.seeBombtests);
        copyFrom(seeUranium,from_b.seeUranium);
        copyFrom(seePlutonium,from_b.seePlutonium);
        
        playEspionage.copyFrom(from_b.playEspionage);
        copyFrom(playMine,from_b.playMine);
        copyFrom(playUniversity,from_b.playUniversity);
        playDesignBomb.copyFrom(from_b.playDesignBomb);
        seeCurrentDesigns.copyFrom(from_b.seeCurrentDesigns);
        playMakePlutonium.copyFrom(from_b.playMakePlutonium);
        playMakeUranium.copyFrom(from_b.playMakeUranium);
        copyFrom(playAirStrike,from_b.playAirStrike);
        playRepair.copyFrom(from_b.playRepair);
        playMakeFighter.copyFrom(from_b.playMakeFighter);
        playMakeBomber.copyFrom(from_b.playMakeBomber);
        copyFrom(playMakeMoney,from_b.playMakeMoney);
        copyFrom(playBuyBuilding,from_b.playBuyBuilding);
        copyFrom(playBuyBuilding2,from_b.playBuyBuilding2);
        robotState.copyFrom(from_b.robotState);
        unresign = from_b.unresign;
        afterWorkerDispatch = from_b.afterWorkerDispatch;
        displayCell = getCell(from_b.displayCell);
        getCell(displayCells,from_b.displayCells);
        espionageSteps = from_b.espionageSteps;
        northKoreanPlayer = from_b.northKoreanPlayer;
        northKoreaSteps = from_b.northKoreaSteps;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        pickedIndex.copyFrom(from_b.pickedIndex);
        droppedIndex.copyFrom(from_b.droppedIndex);
        stateStack.copyFrom(from_b.stateStack);
        pickedObject = from_b.pickedObject;
        resetState = from_b.resetState;
        lastPicked = null;
        options.copy(from_b.options);
        if(G.debug())
        {
        sameboard(from_b); 
        }
    }

    

    public void sameboard(BoardProtocol f) { sameboard((ManhattanBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ManhattanBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        for(int i=0;i<players_in_game;i++) { pbs[i].sameBoard(from_b.pbs[i]); }
        
        
        G.Assert(seeBribe.sameContents(from_b.seeBribe),"seebribe mismatch");
        G.Assert(sameCells(selectedCells,from_b.selectedCells),"selected mismatch");
        G.Assert(pendingBenefit==from_b.pendingBenefit,"pendingBenefit mismatch");
        G.Assert(pendingCost==from_b.pendingCost,"pendingCost mismatch");
        G.Assert(sameContents(choice,from_b.choice),"choice mismatch");
        G.Assert(sameContents(choiceOut,from_b.choiceOut),"choiceOut mismatch");
        G.Assert(repairCounter==from_b.repairCounter,"repairCounter mismatch");
        G.Assert(bombDesigner==from_b.bombDesigner,"bombDesigner mismatch");
        G.Assert(needScientists==from_b.needScientists,"needScientists mismatch");
        G.Assert(needEngineers==from_b.needEngineers,"needEngineers mismatch");
        G.Assert(availableWorkers.sameContents(from_b.availableWorkers),"availableWorkers mismatch");
        G.Assert(sameContents(seeEspionage,from_b.seeEspionage),"seeEspionage mismatch");
        G.Assert(sameContents(seeBuilding,from_b.seeBuilding),"seeBuilding mismatch");
        G.Assert(seeBuildings.sameContents(from_b.seeBuildings),"buildings mismatch");
        G.Assert(seeBombs.sameContents(from_b.seeBombs),"seebombs mismatch");
        G.Assert(seeDiscardedBombs.sameContents(from_b.seeDiscardedBombs),"seediscardedbombs mismatch");
        G.Assert(seeNations.sameContents(from_b.seeNations),"seenations mismatch");
        G.Assert(seeBombtests.sameContents(from_b.seeBombtests),"seeBombtests mismatch");
        G.Assert(availablePersonalities.sameContents(from_b.availablePersonalities),"seepersonalities mismatch");
        G.Assert(sameContents(seeUranium,from_b.seeUranium),"seeUranium mismatch");
        G.Assert(sameContents(seePlutonium,from_b.seePlutonium),"seeplutoniom mismatch");
        
        G.Assert(playEspionage.sameContents(from_b.playEspionage),"playEspionage mismatch");
        G.Assert(sameContents(playMine,from_b.playMine),"playMine mismatch");
        G.Assert(sameContents(playUniversity,from_b.playUniversity),"playUniversity mismatch");
        G.Assert(playDesignBomb.sameContents(from_b.playDesignBomb),"playDesignBomb mismatch");
        G.Assert(seeCurrentDesigns.sameContents(from_b.seeCurrentDesigns),"seeCurrentDesigns mismatch");
        G.Assert(playMakePlutonium.sameContents(from_b.playMakePlutonium),"playMakePlutonium mismatch");
        G.Assert(playMakeUranium.sameContents(from_b.playMakeUranium),"playMakeUranium mismatch");
        G.Assert(playBuyBuilding.sameContents(from_b.playBuyBuilding),"playBuyBuilding mismatch");
        G.Assert(playBuyBuilding2.sameContents(from_b.playBuyBuilding2),"playBuyBuilding2 mismatch");
                       		
        G.Assert(sameContents(playAirStrike,from_b.playAirStrike),"playAirStrike mismatch");
        G.Assert(playRepair.sameContents(from_b.playRepair),"playRepair mismatch");
        G.Assert(playMakeFighter.sameContents(from_b.playMakeFighter),"playFighter mismatch");
        G.Assert(playMakeBomber.sameContents(from_b.playMakeBomber),"playMakeBomber mismatch");
        G.Assert(sameContents(playMakeMoney,from_b.playMakeMoney),"playMakeMoney mismatch");
        
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(sameCells(displayCell,from_b.displayCell),"displaycell mismatch");
        G.Assert(sameCells(displayCells,from_b.displayCells),"displaycells mismatch");
        G.Assert(afterWorkerDispatch==from_b.afterWorkerDispatch,"afterWorkerDispatch mismatch");
        G.Assert(espionageSteps==from_b.espionageSteps,"espionageSteps mismatch");
        G.Assert(northKoreanPlayer==from_b.northKoreanPlayer,"northKoreanPlayer mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
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
        
        
        v ^= Digest(r,seeBribe);
        v ^= Digest(r,choice);
        v ^= Digest(r,choiceOut);
        v ^= Digest(r,repairCounter);
        v ^= Digest(r,bombDesigner);
        v ^= Digest(r,needScientists);
        v ^= Digest(r,needEngineers);
        v ^= Digest(r,selectedCells);
        v ^= Digest(r,selectedChips);
        v ^= Digest(r,pendingCost);
        v ^= Digest(r,pendingBenefit);
        v ^= Digest(r,availableWorkers);
        v ^= Digest(r,seeEspionage);
        v ^= Digest(r,seeBuilding);
        v ^= seeBuildings.Digest(r);
        v ^= seeBombs.Digest(r);
        v ^= seeDiscardedBombs.Digest(r);
        v ^= seeNations.Digest(r);
        v ^= seeBombtests.Digest(r);
        v ^= availablePersonalities.Digest(r);
        v ^= Digest(r,seeUranium);
        v ^= Digest(r,seePlutonium);
        
        v ^= playEspionage.Digest(r);
        v ^= Digest(r,playMine);
        v ^= Digest(r,playUniversity);
        v ^= playDesignBomb.Digest(r);
        v ^= seeCurrentDesigns.Digest(r);
        v ^= playMakePlutonium.Digest(r);
        v ^= playMakeUranium.Digest(r);
        v ^= Digest(r,playAirStrike);
        v ^= playRepair.Digest(r);
        v ^= playMakeFighter.Digest(r);
        v ^= playMakeBomber.Digest(r);
        v ^= Digest(r,playMakeMoney);
        v ^= Digest(r,afterWorkerDispatch);
        v ^= Digest(r,displayCell);
        v ^= Digest(r,displayCells);
        v ^= Digest(r,espionageSteps);
        v ^= Digest(r,northKoreanPlayer);
        v ^= Digest(r,playBuyBuilding);
        v ^= Digest(r,playBuyBuilding2);
        
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
        for(PlayerBoard pb : pbs) { v^= pb.Digest(r); }
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,pickedIndex);
		v ^= Digest(r,droppedIndex);
		v ^= Digest(r,revision);
		v ^= Digest(r,board_state);
		v ^= Digest(r,whoseTurn);
		v ^= Digest(r,options);
        return (v);
    }


    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {	pbs[whoseTurn].endTurn(replay);
        switch (board_state)
        {
        default:
        case ConfirmWorker:
        case ConfirmSelectBuilding:
        	throw G.Error("Move not complete, can't change the current player in state %s",board_state);
        case Puzzle:
            break;
        case Play:
        	// some damaged games have 2 dones in a row
        	if(replay==replayMode.Live) { throw G.Error("Move not complete, can't change the current player in state ",board_state); }
			//$FALL-THROUGH$
        case Confirm:
        case ConfirmRetrieve:
 		case ConfirmPersonality:
        case ConfirmRepair:
        case ConfirmBenefit:
        case PlayLocal:
        case Airstrike:
        case JapanAirstrike:
        case Repair:
        case NoMovesState:
        case PaidRepair:
        case North_Korea_Dialog:
        case Resign:
            moveNumber++; //the move is complete in these states
    		espionageSteps = 0;	// done spying too
    		setWhoseTurn(nextPlayer(whoseTurn));
        }
        pbs[whoseTurn].startTurn(replay);
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	
    	return(board_state.doneState());
    }
    // this is the default, so we don't need it explicitly here.
    // but games with complex "rearrange" states might want to be
    // more selective.  This determines if the current board digest is added
    // to the repetition detection machinery.
    public boolean DigestState()
    {	
    	return(board_state.digestState());
    }
 
    public boolean optionalDoneState() { return board_state.optionalDoneState(); }
    

    public boolean gameOverNow() { return(board_state.GameOver()); }
    public boolean winForPlayerNow(int player)
    {	if(win[player]) { return(true); }
    	boolean isWin = (scoreForPlayer(player)>=winningScore());
    	win[player] = isWin;
    	return(isWin);
    }

    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        pickedSourceStack.clear();
        optionStack.clear();
        pickedIndex.clear();
        droppedIndex.clear();
        stateStack.clear();
        pickedObject = null;
        selectedCells.clear();
        selectedChips.clear();
     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private ManhattanCell unDropObject()
    {	ManhattanCell rv = droppedDestStack.pop();
    	int ri = droppedIndex.pop();
    	pbs[whoseTurn].restoreOptions(optionStack.pop());
    	resetState = stateStack.pop();
    	dropMoveNumber = moveNumber-1;
    	rv.lastDropped = prevLastDropped;
    	setState(stateStack.pop());
    	if(rv.cost==Cost.FixedPool)
    	{
    	
    	switch(rv.type)
    	{
 	   	case Fighter:
 	   	case Bomber:
 		   pickedObject = rv.removeChipAtIndex(ri);
 		   break;
 	   default: 
 		   	pickedObject = rv.chipAtIndex(ri);
 		   	break;
 	   	}
    	}
    	else
    	{
    	pickedObject = rv.removeChipAtIndex(ri); 	// SetBoard does ancillary bookkeeping
    	}
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	ManhattanCell rv = pickedSourceStack.pop();
    	int ri = pickedIndex.pop();
    	resetState = stateStack.pop();
    	pickMoveNumber = moveNumber-1;
    	rv.lastPicked = prevLastPicked;
    	setState(stateStack.pop());
    	if(rv.cost==Cost.FixedPool)
    	{}
    	else
    	{
    	rv.insertChipAtIndex(ri,pickedObject);
    	if(rv.type==Type.Marker)
       	{
    	PlayerBoard pb = getPlayerBoard(pickedObject.color);
    	pb.changeMarker(rv);
       	}
    	}
    	pickedObject = null;
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    int pickMoveNumber = -1;
    int prevLastPicked = -1;
    private void pickObject(ManhattanCell c,int index)
    {	pickedSourceStack.push(c);
    	pickedIndex.push(index);
    	stateStack.push(board_state);
    	stateStack.push(resetState);
    	if(moveNumber!=pickMoveNumber) 
    		{ prevLastPicked = c.lastPicked;
    		  c.lastPicked = moveNumber; 
    		  pickMoveNumber = moveNumber; 
    		}
    	if(c.cost==Cost.FixedPool)
    	{
    	pickedObject = c.chipAtIndex(index);
    	}
    	else
    	{
        pickedObject = c.removeChipAtIndex(index);
    	}
    }  
    // 
    // drop the floating object.
    //
    int dropMoveNumber = -1;
    int prevLastDropped = -1;
    private void dropObject(ManhattanCell c,int index,replayMode replay)
    {
       droppedDestStack.push(c);
       droppedIndex.push(index);
       ManhattanId rack = c.rackLocation();
       if(moveNumber != dropMoveNumber)
       	{ prevLastDropped = c.lastDropped;
       	  c.lastDropped = moveNumber; 
       	  dropMoveNumber = moveNumber; 
       	}
       if((rack==ManhattanId.Stockpile)||(rack==ManhattanId.Building))
       {
    	   PlayerBoard pb = getPlayerBoard(c.color);
    	   if(pb!=null)
    	   		{ pb.selectedBomb = c; 
    	   		}
       }
       if(c.cost==Cost.FixedPool)
       {  switch(c.type)
    	   {
    	   case Fighter:
    	   case Bomber:
    		   c.insertChipAtIndex(index,pickedObject);
    		   if(c.color==pickedObject.color) 
    		   {	PlayerBoard pb = getPlayerBoard(pickedObject.color);
    			   	pb.changeMarker(c);
    		   }
    		   break;
    	   default: break;
    	   }
       }
       else
       {
	       c.insertChipAtIndex(index,pickedObject);
	       switch(c.type)
	       {
	       case Marker:
	       	{
	    	PlayerBoard pb = getPlayerBoard(pickedObject.color);
	    	pb.changeMarker(c);
	       	}
	       	break;
	       	default:
	       }}
      	
    pickedObject = null;
    stateStack.push(board_state);
    stateStack.push(resetState);
    optionStack.push(pbs[whoseTurn].saveOptions());
    }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ManhattanCell c)
    {	return(droppedDestStack.top()==c);
    }
    public ManhattanCell getDest()
    {	return(droppedDestStack.top());
    }
 
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { ManhattanChip ch = pickedObject;
      if(ch!=null)
    	{	return(1); 
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
    private ManhattanCell getCell(ManhattanId source, int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s" , source);
        case Personality: return availablePersonalities;
        case Select: return choice[row];
        case SelectOut: return choiceOut[row];
        case SeeEspionage: return seeEspionage[row];
        case Espionage: return playEspionage;
        case SeeBribes: return seeBribe;
        case Bank: return seeBank;
        case Damage: return seeDamage;
        case Yellowcake: return seeYellowcake;
        case Building: return seeBuilding[row];
        case Mine: return playMine[row]; 
        case University: return playUniversity[row];
        case BombHelp: return bombtestHelp;
        case DesignBomb: return playDesignBomb;
        case MakePlutonium: return playMakePlutonium;
        case MakeUranium: return playMakeUranium;
        case AirStrike: return playAirStrike[row];
        case Repair: return playRepair;
        case MakeFighter: return playMakeFighter;
        case MakeBomber: return playMakeBomber;
        case MakeMoney: return playMakeMoney[row];
        case BuyWithEngineer: return playBuyBuilding;
        case BuyWithWorker: return playBuyBuilding2;
        case SeeBuildingPile: return seeBuildings;
        case SeePlutonium: return seePlutonium[row];
        case SeeUranium: return seeUranium[row];
        case SeeBombPile: return seeBombs;
        case SeeDiscardedBombPile: return seeDiscardedBombs;
        case SeePersonalityPile: return seePersonalities;
        case SeeNationsPile: return seeNations;
        case Bombtest: return seeBombtests;
        case CurrentDesigns: return seeCurrentDesigns;
        case AvailableWorkers: return availableWorkers;
        } 	
    }
    public ManhattanCell getCell(ManhattanId source,MColor color, int row)
    {	
    	if(color==MColor.Board)
    	{	
    		return getCell(source,row);
    	}
    	PlayerBoard pb = getPlayerBoard(color);
    	G.Assert(pb!=null,"should be one");
    	return pb.getCell(source,row);
    }
    /**
     * this is called when copying boards, to get the cell on the new (ie current) board
     * that corresponds to a cell on the old board.
     */
    public ManhattanCell getCell(ManhattanCell c)
    {	if(c==null) { return null; }
    	if(c.onBoard || (c.color==MColor.Board)) { return getCell(c.rackLocation(),c.row); }
    	PlayerBoard pb = getPlayerBoard(c.color);
    	if(pb!=null) { return pb.getCell(c.rackLocation(),c.row); }
    	return null;
    }

    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(ManhattanCell c)
    {	return(c==pickedSourceStack.top());
    }
    public ManhattanCell getSource()
    {	return(pickedSourceStack.top());
    }
    public void doRepair(ManhattanCell c,int cost,replayMode replay)
    {
    	ManhattanChip ch = c.topChip();
    	G.Assert(ch.type==Type.Damage,"should be damage");
    	c.removeTop();
    	PlayerBoard pb = getPlayerBoard(c.color);
    	if(cost!=0) { pb.sendCoinsToBank(pb.cashDisplay,cost,replay); }
    	if(replay.animate)
    	{
    		animationStack.push(c);
    		animationStack.push(seeDamage);
    	}
    }
    private void doAirstrike(ManhattanCell src,ManhattanCell dest,replayMode replay)
    {	PlayerBoard pb = pbs[whoseTurn];
		switch(dest.type)
			{
			case Fighter:
				{
				PlayerBoard victim = getPlayerBoard(dest.color);
				int distance = victim.nFighters - dest.row;
				dest.removeTop();
				dest.addChip(victim.fighter);
				ManhattanCell newf = pb.fighters[pb.nFighters-distance];
				ManhattanCell oldf = pb.fighters[pb.nFighters];
				newf.addChip(pb.fighter);
				if(replay.animate)
				{	animationStack.push(victim.fighters[victim.nFighters]);
					animationStack.push(dest);
					animationStack.push(oldf);
					animationStack.push(newf);
				}
				pb.changeMarker(newf);
				victim.changeMarker(dest);
				}
				break;

			case Bomber:
				{
				PlayerBoard victim = getPlayerBoard(dest.color);
				int distance = victim.nBombers - dest.row;
				dest.removeTop();
				dest.addChip(victim.bomber);
				ManhattanCell newf = pb.fighters[pb.nFighters-distance];
				ManhattanCell oldf = pb.fighters[pb.nFighters];
				newf.addChip(pb.fighter);
				if(replay.animate)
				{	animationStack.push(victim.fighters[victim.nFighters]);
					animationStack.push(dest);
					animationStack.push(oldf);
					animationStack.push(newf);
				}
				pb.changeMarker(newf);
				victim.changeMarker(dest);
				}
				break;
			case Building:
				// add a damage token, reduce bombers by one
				// in japan airstrikes, the source might be a fighter
				ManhattanCell bdest = (src.type==Type.Fighter)
					? pb.fighters[pb.nFighters-1]
					: pb.bombers[pb.nBombers-1];
				bdest.addChip( (src.type==Type.Fighter)?pb.fighter : pb.bomber);
				pb.changeMarker(bdest);
				dest.removeTop();
				dest.addChip(ManhattanChip.Damage);
				if(replay.animate)
				{
					animationStack.push(bdest);
					// this won't do the right thing for occupied buildings
					animationStack.push(dest);	
				}
				break;
			default: throw G.Error("Not expecting airstrike on %s",dest);
			}
    }
    //
    // make a choice dialog for uranium or yellowcake
    // if there is only one actual choice, construct the
    // choice box but select the (only) source 
    //
    private ManhattanState plusUandY(int nU,int nY)
    {
    	PlayerBoard pb = pbs[whoseTurn];
    	prepareChoices(2);
		for(int i=0;i<nU;i++) { choice[0].addChip(ManhattanChip.Uranium); }
		choice[1].reInit();
		for(int i=0;i<nY;i++) {	choice[1].addChip(ManhattanChip.Yellowcake); }
		// premake the choice if there is no real choice
		if(pb.nUranium<nU) 
			{ selectedCells.push(choice[1]); 
			displayCells.remove(0,true);
			return ManhattanState.ConfirmPayment; }
		else if(pb.yellowcakeDisplay.height()<nY) 
			{selectedCells.push(choice[0]); 
			displayCells.remove(1,true);
			return ManhattanState.ConfirmPayment; }
		else { 
			pendingBenefit = Benefit.None;
			return ManhattanState.ResolvePayment; }
    }
    enum AfterWorkerDispatch implements Digestable
    {
    	plusUandY17,plusUandY14,plusUandY13,;

    	public long Digest(Random r) {
			return r.nextLong()*(ordinal()+643235);
		}
    };
    
    // this handles cases where the user has to designate resources to be used, after placing workers
    private ManhattanState afterWorkerDispatch()
    {	AfterWorkerDispatch v = afterWorkerDispatch;
    	afterWorkerDispatch = null;
    	if(v!=null)
    	{
    		switch(v)
    		{
    		case plusUandY17:	return plusUandY(1,7);
    		case plusUandY14:	return plusUandY(1,4);
    		case plusUandY13:	return plusUandY(1,3);
    		default: throw G.Error("Not expecting %s",v);
    		}
    	}
    	return ManhattanState.ConfirmWorker;
    }
    
    // this is called when 1 worker has been played to trigger a chain of additional
    // workers to complete a bomb
    private ManhattanState setNeedWorkers(ManhattanCell c,int scientists,int engineers)
    {	// we've already played one worker of some type.
    	ManhattanChip worker = c.topChip();
    	ManhattanChip bomb = c.chipAtIndex(0);
    	G.Assert(c.height()==1 || scientists==bomb.nScientistsRequired() && engineers==bomb.nEngineersRequired(),
    			"wrong requirements for %s",c.chipAtIndex(0));
    	PlayerBoard pb = pbs[whoseTurn];
    	WorkerType wtype = worker.workerType;
    	if(wtype==WorkerType.L) {
    		// must be oppie or groves
    		if(pb.hasPersonality(ManhattanChip.Oppenheimer)
    				&& !pb.testOption(TurnOption.OppenheimerWorker)
    				&& scientists>0)
    		{
    			wtype = WorkerType.S; 
				pb.setOption(TurnOption.OppenheimerWorker);
    		}
    		else if(pb.hasPersonality(ManhattanChip.Groves)
    				&& !pb.testOption(TurnOption.GrovesWorker)
    				&& engineers>0)
    		{
    			wtype = WorkerType.E; 
				pb.setOption(TurnOption.GrovesWorker);
    		}
    		else {
    			G.Error("bad combination of Laborers and permissions");
    		}
    	}
    	switch(wtype)
    	{
    	case S:	scientists--; 
    			if((scientists>0)
    					&& pb.hasPersonality(ManhattanChip.Oppenheimer)
    					&& !pb.testOption(TurnOption.OppenheimerWorker))
    			{	// use 1 as 2
    				scientists--;
    				pb.setOption(TurnOption.OppenheimerWorker);
    			}
    			break;
    	case E: engineers--; 
			if((engineers>0)
					&& pb.hasPersonality(ManhattanChip.Groves)
					&& !pb.testOption(TurnOption.GrovesWorker))
			{	// use 1 as 2
				engineers--;
				pb.setOption(TurnOption.GrovesWorker);
			}
			break;
    	default: 
    		G.Error("not expecting %s",worker);
    		break;
    	}
    	
     	needScientists = scientists;
    	needEngineers = engineers;
    	return nextWorkerState(scientists,engineers);
    }
    //
    // for bombs that require multiple workers, we count down the numbers
    // and switch to specialized states when only one type is required.
    // This interacts with the Israel nations card which wipes out one
    // of the requirements
    //
    private ManhattanState nextWorkerState(int scientists,int engineers)
    {
    	if(engineers<=0)
    	{
    		switch(scientists)
    		{
    		case 0: return ManhattanState.ConfirmWorker;
    		case 1: return ManhattanState.PlayScientist;
    		case 2: return ManhattanState.Play2Scientists;
    		default: 
    			break;
    			
    		}
    	}
    	else if(scientists<=0)
    	{
    		switch(engineers)
    		{
    		case 0: return ManhattanState.ConfirmWorker;
    		case 1: return ManhattanState.PlayEngineer;
    		case 2: return ManhattanState.Play2Engineers;
    		case 3: return ManhattanState.NeedWorkers;
    		default: 
    			break;
    		}
    	}
    	return ManhattanState.NeedWorkers;
    }
    
    // using a worker where an engineer is required
    public void checkForGroves(ManhattanCell c)
    {
   		ManhattanChip top = c.topChip();
   		if(top.workerType==WorkerType.L)
   			{
   			PlayerBoard pb = pbs[whoseTurn];
   			if(pb.hasPersonality(ManhattanChip.Groves)
   					&& !pb.testOption(TurnOption.GrovesWorker))
   				{
   					pb.setOption(TurnOption.GrovesWorker);
   				}
   			else { G.Error("invalid use of worker");}
   			}
   		}
    	
    // using a worker where a scientist is required
    public void checkForOppenheimer(ManhattanCell c)
    {
   		ManhattanChip top = c.topChip();
   		if(top.workerType==WorkerType.L)
   			{
   			PlayerBoard pb = pbs[whoseTurn];
   			if(pb.hasPersonality(ManhattanChip.Oppenheimer)
   					&& !pb.testOption(TurnOption.OppenheimerWorker))
   				{
   					pb.setOption(TurnOption.OppenheimerWorker);
   				}
   			else { G.Error("invalid use of worker or permission");}
   			}
   		}
    
    // using a worker where either scientists or engineers are required
    public void checkForGrovesOrOppenheimer(ManhattanCell c)
    {
   		ManhattanChip top = c.topChip();
   		if(top.workerType==WorkerType.L)
   			{
   			PlayerBoard pb = pbs[whoseTurn];
   			if(pb.hasPersonality(ManhattanChip.Groves)
   					&& !pb.testOption(TurnOption.GrovesWorker))
   				{
   					pb.setOption(TurnOption.GrovesWorker);
   				}
   			else if(pb.hasPersonality(ManhattanChip.Oppenheimer)
   					&& !pb.testOption(TurnOption.OppenheimerWorker))
   				{
   					pb.setOption(TurnOption.OppenheimerWorker);
   				}
   			else { G.Error("invalid use of worker or permission");}
   			}
   		}
 
    //
    // this is where the action is; after the first worker is placed, we may need to 
    // select other workers or resources to be ready to click "done"
    //
    private ManhattanState requirementsSatisfied(ManhattanCell c,Cost requirements)
    {
    	switch(requirements)
    	{

       	default: 
    		p1("requirements-"+requirements);
    		throw G.Error("Not expecting %s",requirements);
       	case ScientistOrEngineer:
       		{
       		checkForGrovesOrOppenheimer(c);
       		return ManhattanState.ConfirmWorker;
       		}
       	case Airstrike:
       		// this happens when the action is a "lemay" airstrike
       		return ManhattanState.ConfirmSingleAirstrike;
       		
    	case Engineer:
    		return setNeedWorkers(c,0,1);
    		
     	case AnyWorker:
     	case Cash:
    	case AnyWorkerAnd3:
    	case AnyWorkerAnd3Y:
    	case AnyWorkerAnd5:
     	case None: 	
    		return ManhattanState.ConfirmWorker;
 		
       	case Scientist:
       	case ScientistAnd1Yellowcake:
    	case ScientistAnd2YAnd3:
    	case ScientistAnd1Uranium:
    	case ScientistAnd2YellowcakeAnd2:
    	case ScientistAnd2Y:
    	case ScientistAnd5Yellowcake:
    	case ScientistAnd3Uranium:
    	case ScientistAnd3YellowcakeAnd5:
    	case ScientistAnd1YellowcakeAnd3:
    	case ScientistAnd4YellowcakeAnd4:
    	case ScientistAnd3YellowcakeAnd1:
    	case ScientistAndBombDesign:
    		return setNeedWorkers(c,1,0);
    		
    		
    	case ScientistAndEngineerAnd3Uranium:
     	case ScientistAndEngineerAnd4Plutonium:
    	case ScientistAndEngineerAnd4Uranium:
    		return setNeedWorkers(c,1,1);
    	
    	case ScientistAndEngineer2And4Uranium:
    	case ScientistAndEngineer2And5Uranium:
    	case ScientistAndEngineer2And4Plutonium:
    	case ScientistAndEngineer2And5Plutonium:
    		return setNeedWorkers(c,1,2);
    	
    	case Scientist2AndEngineer2And5Uranium:
    	case Scientist2AndEngineer2And6Uranium:
    	case Scientist2AndEngineer2And5Plutonium:
    	case Scientist2AndEngineer2And6Plutonium:
    	
    		return setNeedWorkers(c,2,2);
  
    	case Scientist2AndEngineer3And6Plutonium:
    	case Scientist2AndEngineer3And7Plutonium:
    		return setNeedWorkers(c,2,3);
    	case Scientist2AndEngineer4And7Plutonium:
    		return setNeedWorkers(c,2,4);
    	
    		
    		
    	case AnyWorkerAndBomb:	
    		loadDisplayWithBombDesigns();
    		pendingBenefit = Benefit.DiscardOneBomb;
    		return ManhattanState.DiscardOneBomb;
    		
    	case Engineer3:
    		// use the generic so groves personality is automatic
    		return setNeedWorkers(c,0,3);
    		
    	case Engineer2:
    		return setNeedWorkers(c,0,2);
    		
    	case Scientists3And8Yellowcake:
    		return setNeedWorkers(c,3,0);
    		    		
		case Scientist2And3Yellowcake:
		case Scientist2And2YellowcakeAnd5:
    	case Scientists2And3YellowcakeAnd4:  		
    	case Scientist2And5YellowcakeAnd2:
    	case Scientist2And6Yellowcake:
    	case Scientists2And6YellowcakeAnd7:
    	case Scientist2And4YellowcakeAnd3:
    		return setNeedWorkers(c,2,0);
    		
    	case ScientistAndEngineerAndBombDesign:
    		return setNeedWorkers(c,1,1);
  
    	case EngineerAndMoney:
    		checkForGroves(c);
    		return ManhattanState.SelectBuilding;
    	case ScientistOrWorkerAndMoney:
    		return ManhattanState.SelectBuilding;
    	
    	case ScientistAnd1UraniumOr2Yellowcake:
			return plusUandY(1,2);    		
    		
    	case Scientists2And1UraniumOr7Yellowcake:
    		afterWorkerDispatch = AfterWorkerDispatch.plusUandY17;
    		return setNeedWorkers(c,2,0);
    		
    	case Scientists2And1UraniumOr4Yellowcake:
    		afterWorkerDispatch = AfterWorkerDispatch.plusUandY14;
    		return setNeedWorkers(c,2,0);
    		
    	case Scientist2And1UraniumOr3Yellowcake:
    		afterWorkerDispatch = AfterWorkerDispatch.plusUandY13;
    		return setNeedWorkers(c,2,0);
    		
    	case Any3Workers:
    		return ManhattanState.PlayAny3Workers;

    	case Any2Workers:
    	case Any2WorkersAndRetrieve:	// germany
    	case Any2WorkersAndCash:
     		return ManhattanState.PlayAny2Workers;


    	}
    }
    private void loadDisplayWithBombDesigns()
    {
    	displayCells.clear();
    	CellStack stockpile = pbs[whoseTurn].stockpile;
		for(int i=0,siz=stockpile.size(); i<siz;i++)
		{	ManhattanCell cc = stockpile.elementAt(i);
			if(pbs[whoseTurn].designIsAvailable(cc)) { displayCells.push(cc); }
		}
    }
    public void setNextPlayState(ManhattanCell c)
    {	ManhattanState n = requirementsSatisfied(c,c.getEffectiveCost());
    	
    	setState(n);
    }
    private void setIsrailiBombRequirement(ManhattanCell c)
    {
    	ManhattanChip bomb = c.chipAtIndex(0);
    	ManhattanChip worker = c.chipAtIndex(1);
    	G.Assert(bomb.type==Type.Bomb && worker.type==Type.Worker,"should be a worker and a bomb");
    	WorkerType type = worker.workerType;
    	if(type==WorkerType.L)
    	{	// oppenheimer or groves at work
    		PlayerBoard pb = pbs[whoseTurn];
    		if(pb.hasPersonality(ManhattanChip.Oppenheimer)) { type = WorkerType.S; }
    		else if(pb.hasPersonality(ManhattanChip.Groves)) { type = WorkerType.E; }
    		else { G.Error("invalid worker type"); }
    	}
    	switch(type){
    	case E:	
    		needEngineers = bomb.nEngineersRequired();
    		needScientists = 0;
    		break;
    	case S:
    		needScientists = bomb.nScientistsRequired();
    		needEngineers = 0;
    		break;
    	default: 
    		
    		throw G.Error("Not expecting %s",worker);
    	}
    	
    	
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(ManhattanCell dest,replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        case JapanAirstrike:
        	setState(ManhattanState.ConfirmJapanAirstrike);
        	break;
        case Airstrike:
        	setState(ManhattanState.ConfirmAirstrike);
        	break;
        case PlayAny3Workers:
        	setState(ManhattanState.PlayAny2Workers);
        	break;
        case Play2Scientists:
        	{
        	PlayerBoard pb = pbs[whoseTurn];
        	if(pb.hasPersonality(ManhattanChip.Oppenheimer) && !pb.testOption(TurnOption.OppenheimerWorker))
            		{
            		pb.setOption(TurnOption.OppenheimerWorker);
            		setState(ManhattanState.ConfirmWorker);
            		}
            		else 
            		{
            			setState(ManhattanState.PlayScientist);
            		}
        	}
        	break;
        case Play2Engineers:
        	{
        	PlayerBoard pb = pbs[whoseTurn];
        	if(pb.hasPersonality(ManhattanChip.Groves) && !pb.testOption(TurnOption.GrovesWorker))
        		{
        		pb.setOption(TurnOption.GrovesWorker);
        		setState(ManhattanState.ConfirmWorker);
        		}
        		else 
        		{
        		setState(ManhattanState.PlayEngineer);
        		}
        	}
        	break;
        	
        case BuildIsraelBomb:
        	// normal bombs start by dropping a worker on a bomb card.
        	// israelii bombs start with the israel card, then drop a 
        	// first worker on the bomb to be built.
        	// set the numbers needed for the whole ignoring the worker not dropped
        	 setIsrailiBombRequirement(dest);
			//$FALL-THROUGH$
		case NeedWorkers:
        	
        	{
        	ManhattanChip top = dest.topChip();
        	WorkerType type = top.workerType;
        	PlayerBoard pb = pbs[whoseTurn];
 
        	switch(type)
        	{
        	case L: 
        		
        		if(needScientists>0
        				&& pb.hasPersonality(ManhattanChip.Oppenheimer)
        				&& !pb.testOption(TurnOption.OppenheimerWorker))
        			{ needScientists--; 
        				pb.setOption(TurnOption.OppenheimerWorker);
        			}
        		else if(needEngineers>0 
        				&& pb.hasPersonality(ManhattanChip.Groves)
        				&& !pb.testOption(TurnOption.GrovesWorker))       
					{ needEngineers--; 
					  pb.setOption(TurnOption.GrovesWorker);
					}
        		else { G.Error("invalid worker type"); }
        		break;
        	case S: 
        		needScientists--; 
        		if(pb.hasPersonality(ManhattanChip.Oppenheimer) 
        				&& needScientists>0
        				&& !pb.testOption(TurnOption.OppenheimerWorker))
					{ needScientists--; 
					  pb.setOption(TurnOption.OppenheimerWorker);
					}
        		break;
        	case E: needEngineers--; 
        		if(pb.hasPersonality(ManhattanChip.Groves) 
        				&& needEngineers>0
        				&& !pb.testOption(TurnOption.GrovesWorker)
        				) 
        			{ needEngineers--; 
        			  pb.setOption(TurnOption.GrovesWorker);
        			}
        		break;
        	default: throw G.Error("not expecting %s",top);
        	}
        	setState(nextWorkerState(needScientists,needEngineers));
        	break;
        	}
        case PlayScientist:
        case PlayEngineer:
        case PlayAny2Workers:
        	{
        	ManhattanState disp = afterWorkerDispatch();
        	if(disp!=null) { setState(disp); }
        	else setState(ManhattanState.ConfirmWorker);
        	}
        	break;
		case Play:
        case PlayLocal:
        case PlayEspionage:
        case PlayOrRetrieve:
        	{
 			setNextPlayState(dest);
 			if(board_state==ManhattanState.ConfirmWorker)
 			{
 	       	ManhattanState disp = afterWorkerDispatch();
        	if(disp!=null)
        		{ setState(disp);
        		}
        	}}
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
        resetState = board_state;
    }
    private int nextPlayer(int n)
    {
    	return((n+1)%players_in_game);
    }
    
    private void setNextStateAfterDone(replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state "+board_state);
    	case Airstrike:
    	case JapanAirstrike:
    	case Repair:
    	case CollectBenefit:
    	case RetrieveSorE:
    	case PlayEspionage:
    	case BuildIsraelBomb:
    	case North_Korea_Dialog:
    	case SelectPersonality:
    	case Gameover: 
    		break;
    	case ConfirmJapanAirstrike:
    	case ConfirmAirstrike:
    		setState(resetState);	// airstrike or japan airstrike
    		break;
     	case ConfirmRepair:
    		if(resetState!=ManhattanState.PaidRepair) break;
			//$FALL-THROUGH$
		case PaidRepair:
    	case ConfirmDiscard:
    	case ConfirmChoice:
    		break;
    	case Resign:
    		setState(ManhattanState.Resign);
    		break;
       	case ConfirmPayment:
     	case ConfirmBenefit:
    	case ConfirmWorker:
    	case ConfirmSelectBuilding:
       	case ConfirmSingleAirstrike:
    	case ConfirmRetrieve1:
       		// if we still have workers, allow continuing
       		// otherwise fall through into the next player
			setState(espionageSteps>0 ? ManhattanState.PlayEspionage : ManhattanState.PlayLocal);
			break;
    	case NoMovesState:
		case ConfirmPersonality:
    		setNextPlayer(replay);
			//$FALL-THROUGH$
      	case ConfirmRetrieve:
       	case ConfirmNichols:
   		case Confirm:
    	case Puzzle:
    	case PlayLocal:
    	case Play:
    		{
    		PlayerBoard pb = getPlayerBoard(whoseTurn);
    		if(pb.nAvailableWorkers()==0) { setState(ManhattanState.Retrieve); }
    		else if(pb.nPlacedWorkers==0) { setState(ManhattanState.Play); }
    		else { setState(ManhattanState.PlayOrRetrieve); }
    		}
    		break;
    	}
       	if(!hasMoves()) {
       		setState(ManhattanState.NoMovesState);
       	}
    }
    
    private void startNextChoice()
    {	int who = whoseTurn;
    	for(int i=0;i<players_in_game; i++,who=nextPlayer(who))
    	{	PlayerBoard pb = pbs[who];
    		if(pb.pendingChoices.size()>0)
    		{
    			startChoice(pb,who);
    			break;
    		}
    	}
    }
    private void finishChoice()
    {
    	contextStack.restore();
    }
    private void startChoice(PlayerBoard pb,int who)
    {
    	Benefit c = pb.pendingChoices.pop();
    	switch(c)
    	{
    	default: throw G.Error("Not prepared for %s",c);
    	case PaidRepair:
    		contextStack.save();
    		whoseTurn = who;
    		pendingCost = Cost.None;
    		pendingBenefit = Benefit.PaidRepair;
    		repairCounter = 3;
    		setState(ManhattanState.PaidRepair);
    		break;
     	case DiscardBombs:
    		contextStack.save();
    		whoseTurn = who;
    		pendingCost = Cost.None;
    		pendingBenefit = Benefit.DiscardBombs;
    		loadDisplayWithBombDesigns();
    		setState(ManhattanState.DiscardBombs);
    		break;
    	case ScientistOrEngineer:
    		contextStack.save();
    		pendingBenefit = c;
    		whoseTurn = who;
    		pb.chooseScientistOrEngineer( Benefit.ScientistOrEngineer,1,1);
    		pendingCost = Cost.None;
    		setState(ManhattanState.ResolveChoice);
    	}
    }

    private void finishWorker(ManhattanCell dest,replayMode replay)
    {
    	if(dest!=null)
    	{
      	PlayerBoard pb = pbs[whoseTurn];
      	ManhattanChip top = dest.topChip();
      	switch(top.type)
      	{
      	case Worker:
      		{
	    	pb.nPlacedWorkers++;
	    	pb.payCost(dest,dest.getEffectiveCost(),replay);
	    	displayCell = null;
	    	pb.collectBenefit(dest,dest.getEffectiveBenefit(),selectedCells.top(),replay);
	        
	        if(winForPlayerNow(whoseTurn)) 
			{ win[whoseTurn]=true;
			  setState(ManhattanState.Gameover); 
			} }
      		break;
      	case Bombtest:
      		pb.hasTestedBomb = true;
      		break;
      	case Bomber:
      		{
      		ManhattanChip ch = dest.chipAtIndex(0);
      		int cost = ch.loadingCost();
      		pb.sendCoinsToBank(pb.cashDisplay,cost,replay);
      		pb.addBomber(-1,replay);
      		}
      		break;
      	default: throw G.Error("Not expecting %s",top);
      	}}
    	else if(resetState==ManhattanState.BuildIsraelBomb)
    	{	// very special case, the "little boy" bomb is selected rather than
    		// having a worker dropped on it, because it reuires no Workers
    		ManhattanCell idest = selectedCells.pop();
    		PlayerBoard pb = pbs[whoseTurn];
    		pb.collectBenefit(idest,idest.getEffectiveBenefit(),idest,replay);
    	}
 
    }
    private void discardBombs(replayMode replay)
    {
     	PlayerBoard pb = pbs[whoseTurn];
     	pb.discardBombs(selectedChips,replay);
     	 	
    }
    private void distributeBenefit(replayMode replay)
    {
      	PlayerBoard pb = pbs[whoseTurn];
      	if(pendingBenefit==Benefit.Trade)
      	{
      	while(selectedCells.size()>0)
      		{
      		ManhattanCell c = selectedCells.pop();
      		if(c.rackLocation()==ManhattanId.Select)
      		{
      			// we give up
      			pb.payCost(c,replay);
      		}
      		else {	// we get
      			pb.distributeBenefit(pendingBenefit,c,replay);
      		}
      		}
      	}
      	else
      	{
      	pb.distributeBenefit(pendingBenefit,selectedCells.top(),replay);
      	}
      	pendingBenefit = Benefit.None;
      	acceptPlacement();
    }
 
    private void doRetrieve(replayMode replay)
    {
		PlayerBoard pb =pbs[whoseTurn];
   		MColor color = pb.color;
  		pb.retrieveAllWorkers(replay);
  		for(PlayerBoard p : pbs)
  		{
  			if(p!=pb) { p.retrieveColoredWorkers(color,replay); }
  		}
   		retrieveMainBoardWorkers(color,replay);
    }
    int repairCost()
    {
    	if(resetState==ManhattanState.Repair) { return 0; }
    	if(repairCounter<=0) { return 9999; }	// infinite, we already did 3
    	return RepairCost[RepairCost.length-repairCounter];
    }
    private void doDone(replayMode replay)
    {	
    	ManhattanState startingState = board_state;
    	boolean frenchDiscard = false;
    	boolean frenchDiscarded = false;
    	PlayerBoard pb = pbs[whoseTurn];
    	
    	switch(startingState)
    	{
   		
 		//$FALL-THROUGH$

    	case PlayEspionage:
    		espionageSteps = 0;		// end or espionage if you hit done without doing anything
			//$FALL-THROUGH$
		case BuildIsraelBomb:
    		setState(ManhattanState.ConfirmWorker);
    		break;
		case PaidRepair:
			repairCounter = 0;
			setState(ManhattanState.ConfirmWorker);
			break;
    	case ConfirmRepair:
    		{
    		ManhattanCell dest = selectedCells.pop();
    		int cost = repairCost();
    		doRepair(dest,cost,replay);
    		repairCounter--;
    		
    		// set up for the next repair, or continue
    		
    		if(!pb.repairNeeded()) { repairCounter = 0; }	// all repaired
    		if(pb.cashDisplay.cash<repairCost())
    		{	// can't afford more
    			repairCounter = 0;
    		}
    		setState(repairCounter==0 ? ManhattanState.ConfirmWorker : resetState); 
    		}
    		
    		break;
    	case Repair:
    	case Airstrike:
    	case JapanAirstrike:
    		//setNextPlayer(replay);
    		//setState(ManhattanState.Confirm);
    		setState(ManhattanState.ConfirmWorker);
    		break;
    		
    	case ConfirmJapanAirstrike:
			{
			ManhattanCell dest = getDest();
			doAirstrike(getSource(),dest,replay);
			setState(ManhattanState.JapanAirstrike);
			}
			break;

    	case ConfirmNichols:
    		{
    		ManhattanChip victim = seeBuilding[0].removeTop();
    		seeBuildings.insertChipAtIndex(0,victim);
    		if(replay.animate)
    		{
    			animationStack.push(seeBuilding[0]);
    			animationStack.push(seeBuildings);
    		}
   			pbs[whoseTurn].addCoinsFromBank(seeBribe,1,replay);
			pbs[whoseTurn].setOption(TurnOption.NicholsShuffle);
			logGameEvent(NicholsActionMessage);
			shuffleBuildings(seeBuilding[0],replay);
    		}
    		break;
    	case ConfirmSingleAirstrike:
			{
			ManhattanCell dest = getDest();
			doAirstrike(getSource(),dest,replay);
			pbs[whoseTurn].setOption(TurnOption.LemayAirstrike);
			}
			break;
	
    	case ConfirmAirstrike:
    		{
    		ManhattanCell dest = getDest();
    		doAirstrike(getSource(),dest,replay);
    		setState(ManhattanState.Airstrike);
    		}
    		break;
    	case ConfirmDiscard:
    		discardBombs(replay);
    		frenchDiscarded = true;
    		break;
      	case ConfirmChoice:
      	case North_Korea_Dialog:
      	case ConfirmRetrieve1:
    		distributeBenefit(replay);
    		break;
    	case ConfirmBenefit:
    		Benefit pend = pendingBenefit;
    		distributeBenefit(replay);
    		switch(pend)
    		{
    		case FrenchBombDesign:
    			{
    			// everyone has to discard down to 3\
    			if(seeCurrentDesigns.height()<players_in_game+1)
    			{	ManhattanChip ch = getABomb();
    				if(ch!=null)
    				{
    				seeCurrentDesigns.addChip(ch);
    				if(replay.animate)
    				{
    					animationStack.push(seeBombs);
    					animationStack.push(seeCurrentDesigns);
    				}}
    			}
    			frenchDiscard = true;
    			}
    			break;
    		case BombDesign:
	    		{	pendingBenefit = pend;
				// everyone gets a bomb!
				setNextPlayer(replay);
				if(whoseTurn == bombDesigner)
					{	// whatever's left, we get
						if(seeCurrentDesigns.height()>0)
							{selectedCells.push(seeCurrentDesigns);
						     distributeBenefit(replay);
							}
						loadNewDesigns(replay);
					}
					else if(seeCurrentDesigns.height()>0)
					{
					setState(ManhattanState.CollectBenefit);
					}
					else {
						whoseTurn = bombDesigner;
					}
	    		}
	    		break;
	    	default:
	    		break;
    		}
    		
    		break;
    	case ConfirmPayment:
    	case ConfirmSelectBuilding:
    	case ConfirmWorker:
    		{
    		ManhattanCell dest = getDest();
    		boolean espionage = dest!=null && dest.color!=pb.color;
    		if(espionage)
    		{
    		if(espionageSteps>0)
    			{
    			espionageSteps--;
    			}
    		else if(pb.hasPersonality(ManhattanChip.Fuchs)
    				&& (dest.color!=MColor.Board)
    				&& (dest.color!=pb.color))
    			{
    			pb.setOption(TurnOption.FuchsEspionage);
    			}	
    		}
    		finishWorker(dest,replay);
    		switch(board_state)
    		{
    		case ConfirmBenefit:
    				// nations usa can have no bombers to sell
    				distributeBenefit(replay);
    				break;
    		case Confirm:
    				// china does this to cause the turn to end
    				setNextPlayer(replay);
    				break;
    		default: break;
    		}
    		if(board_state==ManhattanState.Confirm) 
    			{ 
    				
    			}
    		}
    		break;
    	case RetrieveSorE:
    		setState(ManhattanState.ConfirmRetrieve1);	// chose not to (or didn't have any)
    		break;
    	case ConfirmRetrieve:
    		doRetrieve(replay);
       		if(options.test(Options.Personalities))
    		{	
       			preparePersonalityMoves();
    			break;
    		}
			//$FALL-THROUGH$
		case Confirm:
    	case PlayLocal:
    		if(winForPlayerNow(whoseTurn)) 
    			{ setState(ManhattanState.Gameover); 
    			//p1("gameover "+moveNumber);
    			}
    		else { setNextPlayer(replay); }
    		break;
    	case Resign:
    		setState(ManhattanState.Gameover);
    		break;
    	case Gameover:
    	default:
    		break;
    	}
    	acceptPlacement();
    	setNextStateAfterDone(replay);
    	
		switch(board_state)
		{
		case PaidRepair:
			break;
		default : finishChoice();
			break;
		}
		 
    	
    	if(frenchDiscard)
    	{	// there's a very special case, if you discard one of your own bombs
    		// and it was the only one that gave you moves.
    		for(PlayerBoard apb : pbs)
			{
			if(apb.nDesignsAvailable()>3)
				{
				apb.pendingChoices.push(Benefit.DiscardBombs);
				}
			}
    	}
    	
    	switch(board_state)
    	{
    	case Repair:
    	case PaidRepair:
    		break;
    	default: 
    			// this arrangement caused quite a bit of trouble with restarting games, because
    			// 4 or 5 player games trigger an extra worker placement at the start of the game.
    			// the problem was that "start" ought to be idempotent, but in the original arrangement,
    			// start was done twice, and the second start didn't find these on the pending queue
    			// the fix was to add several calls to resetHistory() to make sure there was no extra
    			// start to be executed.
    			startNextChoice();
    			break;
    	}
    	if(frenchDiscarded) { loadNewDesigns(replay); }
		// in rare circumstances, there can be no moves - either at top level
		// or after a french discard which discards the only available move
		while( ( (frenchDiscarded && (board_state==ManhattanState.PlayLocal))
				|| (board_state==ManhattanState.Play))
			&& !hasMoves())
		{   setState(ManhattanState.Confirm);
			setNextPlayer(replay);
			setNextStateAfterDone(replay);
		}

       	resetState = board_state;
    }
    // return a worker to his home; a player or the workers pile
    void returnWorker(ManhattanCell c,ManhattanChip ch,replayMode replay)
    {
    	G.Assert(ch.workerType!=WorkerType.N,"must be a worker");
    	MColor color = ch.color;
    	switch(color) 
    	{
    	default: throw G.Error("Not expecting %s",ch);
    	case Gray:	
    		availableWorkers.addChip(ch);
    		// return all the gray workers
    		if(replay.animate)
    			{
    			animationStack.push(c);
    			animationStack.push(availableWorkers);
    			}
    		break;
    	case Red:
    	case Green:
    	case Blue:
    	case Yellow:
    	case Purple:
    		PlayerBoard pb = getPlayerBoard(color);
    		pb.returnWorker(c,ch,replay);
    	}
    		
    }
    /**
     * return the workers of a particular color and gray workers
     * 
     * @param color
     * @param replay
     */
    private void retrieveMainBoardWorkers(MColor color,replayMode replay)
    {
    	for(ManhattanCell c = allCells; c!=null; c=c.next)
    	{	if(c!=availableWorkers)
    	{
    		switch(c.type)
    		{
    		case Worker:
    			// this will be a cell with only workers on it.
    				for(int lim=c.height()-1; lim>=0; lim--)
    				{
    					ManhattanChip ch = c.chipAtIndex(lim);
    					if(ch.color==MColor.Gray || ch.color==color)
    						{
    						c.removeChipAtIndex(lim);
    						returnWorker(c,ch,replay);
    						}
    				}
    				
    			break;
    		default: break;
    		}
    	}
    	}
    }
	void selectChips(ManhattanChip newselected)
	{
       	boolean multi = false;
    	boolean remove = selectedChips.remove(newselected,true)!=null;
    	ManhattanState next = resetState;

    	if(!remove)
    		{ 
    		if(!multi) { selectedChips.clear(); }
    		selectedChips.push(newselected);
    		}
    	
    	switch(pendingBenefit)
    	{
    	//case DiscardOneBomb: 
    	//	if(!remove) { next = ManhattanState.ConfirmPayment; }
    	//	break;
   
    	default: G.Error("Not expecting %s",resetState); 
    		//next = ManhattanState.ConfirmWorker; break;
    	}
    	setState(next);
	}
	
	private void selectCells(ManhattanCell newselected)
	{
		boolean nichols = (board_state!=ManhattanState.ConfirmWorker)
						&& (board_state!=ManhattanState.SelectBuilding)
						&& (board_state!=ManhattanState.ConfirmSelectBuilding)
						&&  ( newselected==seeBuilding[0]);	// for the nichols personality option
       	boolean multi = !nichols && pendingBenefit==Benefit.DiscardBombs;
       	boolean tworows = !nichols && pendingBenefit==Benefit.Trade;
    	boolean remove = selectedCells.remove(newselected,true)!=null;
    	ManhattanState next = resetState;
    	if(!remove)
    		{ 
    		if(tworows)
    		{
    		for(int lim=selectedCells.size()-1; lim>=0; lim--)
    			{
    			ManhattanCell c = selectedCells.elementAt(lim);
    			if(c!=newselected && c.rackLocation()==newselected.rackLocation)
    				{
    				selectedCells.remove(c,true);
    				}
    			}
    		selectedCells.push(newselected);
    		}
    		else {
    			if(!multi) { selectedCells.clear(); }
    			selectedCells.push(newselected);
    			}
    		}
    	if(tworows)
    	{
    		if(selectedCells.size()==2) { next = ManhattanState.ConfirmBenefit; }
    	}
    	else if(nichols)
    	{
    		if(!remove) { next = ManhattanState.ConfirmNichols; }
    	}
    	else
    	{
    	switch(resetState)
    	{
    	case DiscardBombs:
    		if(displayCells.size()-selectedCells.size()==3) { next = ManhattanState.ConfirmDiscard; }
    		break;
       	case RetrieveSorE:
       		if(!remove) { next = ManhattanState.ConfirmRetrieve1; } 
       		break;
     	case PaidRepair:
    	case Repair: 
    		if(!remove) { next = ManhattanState.ConfirmRepair; } 
    		break;
       	case CollectBenefit: 
       		if(!remove) { next = ManhattanState.ConfirmBenefit; } 
       		break;
       	case SelectPersonality:
    	case ResolveChoice: 
    		if(!remove) { next = ManhattanState.ConfirmChoice;} 
    		break;
    	case DiscardOneBomb:
    	case ResolvePayment: 
    		if(!remove) { next = ManhattanState.ConfirmPayment; } 
    		break;
    	case SelectBuilding:
    		if(!remove) { next = ManhattanState.ConfirmSelectBuilding; }
    		break;
    	case BuildIsraelBomb:
    	case PlayLocal: 
    	case Play: next = ManhattanState.ConfirmWorker; break;
    	case Puzzle: break;
    	default: throw G.Error("Not expecting %s",resetState); 
    	}}
    	setState(next);
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	ManhattanMovespec m = (ManhattanMovespec)mm;
        if(replay.animate) { animationStack.clear(); }
        //m.startingDigest = Digest();
        //G.print("E "+m+" for "+whoseTurn);
        switch (m.op)
        {        	
        case MOVE_DONE:
        	doDone(replay);
            break;
        case MOVE_REPAIR:
        case MOVE_SELECT:      	
        	{
        	ManhattanCell newselected = getCell(m.dest,m.to_color,m.to_row);
        	switch(pendingBenefit)
        	{
        	//	some future state that uses a single cell's stacj
        	// selectChips(newselected.chipAtIndex(m.to_index));
        	//	break;
        	case DiscardBombs:
           	case DiscardOneBomb:
         	case PaidRepair:
        	case Repair: 
           	default:
           		newselected.selectedIndex = m.to_index;
        		selectCells(newselected);
        		break;
        	
        	}
 
        	}
        	break;
        case MOVE_PICK:
			{
			ManhattanCell src =getCell(m.source,m.from_color,m.from_row);
 			if(isDest(src) && (board_state!=ManhattanState.Puzzle)) 
 				{ unDropObject(); 
 				}
	 			else
	 			{
	        	// be a temporary p
	        	pickObject(src,m.from_index);
	        	m.chip = pickedObject;
	 			}
 			}
			break;
        case MOVE_ATTACK:
        case MOVE_FROM_TO:
        	{
        	ManhattanCell src = getCell(m.source,m.from_color,m.from_row);
        	pickObject(src,m.from_index);
        	m.chip = pickedObject;
        	}
			//$FALL-THROUGH$
		case MOVE_DROP: // drop on chip pool;
        	G.Assert(pickedObject!=null,"nothing moving");
        	{
            ManhattanCell dest = getCell(m.dest,m.to_color,m.to_row);
            if(isSource(dest) && (board_state!=ManhattanState.Puzzle)) { unPickObject(); }
            else 
            	{
		        if(replay==replayMode.Live)
	        	{ lastDroppedObject = pickedObject.getAltDisplayChip(dest);
	        	  //G.print("last ",lastDroppedObject); 
	        	}
		    m.cell = dest;
           	dropObject(dest,m.to_index,replay); 
           	if(replay.animate)
           	{
           		animationStack.push(getSource());
           		animationStack.push(dest);
           	}
           	setNextStateAfterDrop(dest,replay);
            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            // if it's a cold start, queue up the worker choice
            int nextp = nextPlayer(whoseTurn);
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(ManhattanState.Puzzle);	// standardize the current state
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(ManhattanState.Gameover); 
               	}
            else {  doDone(replay); }
            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?ManhattanState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
        	for(PlayerBoard pb : pbs) { pb.pendingChoices.clear(); }
            setState(ManhattanState.Puzzle);
            resetState = board_state;
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(ManhattanState.Gameover);
    	   break;

       case MOVE_RETRIEVE:
       		{
       		setState((board_state==ManhattanState.ConfirmRetrieve) ? resetState : ManhattanState.ConfirmRetrieve);   
       		}
       		break;
       case EPHEMERAL_APPROVE:
       		{
	    	   m.player = getPlayerBoard(m.from_color).boardIndex;
	    	   PlayerBoard pb = getPlayerBoard(m.from_color);
	    	   pb.approvedNorthKorea = !pb.approvedNorthKorea;
	    	   if(allApprovedExcept() && (pb.boardIndex==northKoreanPlayer)) { doDone(replay); }     		 
       		}
     	   break;
		case MOVE_APPROVE:
       		{
       		 PlayerBoard pb = getPlayerBoard(m.from_color);
       		 pb.approvedNorthKorea = true;
       		 pb.hasSetContribution = true;	// even if not really
       		 if(allApproved()) { doDone(replay); }     	
       		 else if(robot!=null) { setNextPlayer(replayMode.Replay); }
       		}
       		break;
       case EPHEMERAL_CONTRIBUTE:
       	{ PlayerBoard pb = getPlayerBoard(m.from_color);
    	   m.player = pb.boardIndex;
    	   northKoreaSteps++;
    	   if(pb.koreanContribution != m.from_index)
    	   {
    	   for(PlayerBoard p : pbs) { p.approvedNorthKorea = false; p.hasSetContribution=false; }
    	   }
       	}
			//$FALL-THROUGH$
		case MOVE_CONTRIBUTE:
       		{
       			PlayerBoard pb = getPlayerBoard(m.from_color);
         	    pb.hasSetContribution = true;
     			pb.koreanContribution =  m.from_index;
     			
       		}
       		break;
        default:
        	cantExecute(m);
        }
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

 
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(ManhattanMovespec m)
    {
        robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        try {
        Execute(m,replayMode.Replay);
        }
        catch (Throwable err)
        {	String name = err.toString();
        	int ind = name.indexOf(':');
        	p1("error-"+moveNumber+"-"+name.substring(0,ind));
        	throw err;
        }
       
    }
 
// 
// nichols can punch out the first building, causing all the other buildings
// to cascade down.
//
 private boolean addNicholsMoves(CommonMoveStack all,PlayerBoard pb,int who)
 {	boolean some = false;
	 if(pb.hasPersonality(ManhattanChip.Nichols) 
			 && !pb.testOption(TurnOption.NicholsShuffle)
			 && seeBuilding[0].height()>0)
	 	{	some = true;
	 		if(all!=null)
	 		{	// personality nichols can trash the first building
	 			all.push(new ManhattanMovespec(MOVE_SELECT,seeBuilding[0],0,whoseTurn));
	 		}
	 	}
	 return some;
 }
 
 //
 // add moves on the main board
 //
 private boolean addMainboardMoves(CommonMoveStack all,int who)
 {
	 PlayerBoard pb = pbs[who];
	 boolean some = false;
	 for(ManhattanCell c = allCells; c!=null && (all!=null || !some); c=c.next)
	 { if(!c.inhibited)
		 {
		 boolean allowPersonalities = true;
		 switch(c.type)
		 {
		 default: break;
		 case BuildingMarket:	// this lets us drop directly on the buildings
			 if(c.height()==0) { break; }
			 // if we have groves, don't allow him to be used accidentally when buying a building.
			 // it can still be done by playing a worker on the engineer spot.
			 allowPersonalities = false;
		 //$FALL-THROUGH$
		 case Worker:	
		 {		if(c.getEffectiveCost()!=Cost.None)
		 		{
			 	some |= pb.addWorkerMoves(all,allowPersonalities,pickedObject,c,MOVE_DROP,who);
		 		}
		 }
		 }
	 }}
	 some |= addNicholsMoves(all,pb,who);
	 if(  (!some || robot==null)
			 && pb.hasPersonality(ManhattanChip.Fuchs)
			 && !pb.testOption(TurnOption.FuchsEspionage))
	 	{
		 some |= addPlayerEspionageMoves(all,pbs[whoseTurn],whoseTurn); 
	 	}
	 
	 return some;
 }

 public boolean addAddWorkersMoves(CommonMoveStack all,ManhattanCell dest,int who)
 {
	 PlayerBoard pb = pbs[who];
	 return pb.addSelectWorkerMoves(all,MOVE_FROM_TO,pickedObject==null ? null : pickedObject.workerType,dest,who);
 }
 public boolean addEngineerMoves(CommonMoveStack all,ManhattanCell dest,int who)
 {
	 PlayerBoard pb = pbs[who];
	 return pb.addEngineerMoves(all,pickedObject,dest,who);
 }
 
 public boolean addScientistMoves(CommonMoveStack all,ManhattanCell dest,int who)
 {
	 PlayerBoard pb = pbs[who];
	 return pb.addScientistMoves(all,pickedObject,dest,who);
 }
 /**
  * the Israel nations card causes several complications in the game flow.
  * here, we will substitute a complete set of bomb building requirements 
  * with "or" instead of "and" for the worker requirements.  These have
  * to be handled in all 3 of the worker/scientist/engineer worker tests.
  * then the bomb building is initiated in the usual way by adding the first
  * worker.  there's one bomb that requires only one scientists to build, so it
  * can be built with no workers at all, and instead it is just selected
  * @param all
  * @param who
  * @return
  */
 public boolean addIsraelBombMoves(CommonMoveStack all,int who)
 {	if(all==null) { return true; }
	PlayerBoard pb = pbs[who];
	boolean some = pb.addIsraelBombMoves(all,pickedObject,who);
	if(!some) { all.push(new ManhattanMovespec(MOVE_DONE,who)); }
	return true;
 }
 /** from the construction space on the main board, next select the building to buy
  * 
  * @param all
  * @param dest
  * @param who
  * @return
  */
 public boolean addSelectBuildingMoves(CommonMoveStack all,ManhattanCell dest,int who)
 {
	 PlayerBoard pb = pbs[who];
	 ManhattanChip worker = dest.topChip();
	 boolean some = false;
	 if((dest.rackLocation()==ManhattanId.BuyWithEngineer)
			 && worker.workerType==WorkerType.L)
	 {	 // special case, groves uses his upgrade to build for free
		 G.Assert(pb.hasPersonality(ManhattanChip.Groves),"must be groves");
		 worker = pb.engineer;
	 }
	 for(ManhattanCell c : seeBuilding)
	 {	if(c.height()>0)
	 	{
		// don't allow personality groves to be invoked, we already decided above
		// if that's the case, by switching the worker. 
		some |= pb.addWorkerMoves(all,false,worker,c,MOVE_SELECT,who);
		if(all==null && some) { break; }
	 	}
	 }
	 return some;
 }

 public boolean addRepairMoves(CommonMoveStack all,int who)
 { 	PlayerBoard pb = pbs[who];
 	return pb.addRepairMoves(all,repairCost(),who);
 }
 

 public boolean addRetrieveSorEMoves(CommonMoveStack all,int who)
 {	
 	PlayerBoard mpb = pbs[who];
	boolean some = false;
	MColor color = mpb.color;
	for(ManhattanCell c = allCells; c!=null; c=c.next)
	 	{
		 if(c.type==Type.Worker)
			 {  some |= mpb.addRetrieveSorEMoves(all,c,color,who);
			 }
	 	}
	 for (PlayerBoard pb : pbs)
	 {	 some |= pb.addRetrieveSorEMoves(all,color,who);
	 	 some |= pb.addRetrieveSorEMoves(all,color,who);
	 }
	 return some;
 }
 


 public boolean addDiscardBombMoves(CommonMoveStack all,int who)
 {
	 PlayerBoard pb = pbs[who];
	 return pb.addDiscardBombMoves(all,robot!=null,who);
 }
 public boolean addPlayerBoardMoves(CommonMoveStack all,PlayerBoard pb,int who)
 {	 
	 return pb.addPlayerBoardMoves(all,pickedObject,who);
 }
 public boolean addAirstrikeMoves(CommonMoveStack all,boolean lemay,int who)
 {	 
 	 if(all==null) { return true; }
 	 PlayerBoard mypb = pbs[who];
	 for(PlayerBoard pb : pbs)
	 {
		 if(pb!=mypb)
		 { mypb.addAirstrikeMoves(all,pickedObject,pb,board_state==ManhattanState.JapanAirstrike,lemay,who); 
		 }
	 }

	 return true;
 }
 
 public boolean addPlayerEspionageMoves(CommonMoveStack all,PlayerBoard pb,int who)
 {	boolean some = false;
 	for(PlayerBoard other : pbs)
 		{
 		// you can use your own buildings during espionage
 		if(!other.hasPersonality(ManhattanChip.Landsdale))
 		{	// personality Landsdale is immune to espionage
	 	if(other!=pb) { some |= pb.addPlayerBoardMoves(all,pickedObject,other.buildings,who); } 
 		}
 		}
 	return some;
 }
 public void addSelectableMoves(CommonMoveStack all)
 {
	 all.push(new ManhattanMovespec(MOVE_DROP,seeCurrentDesigns,-1,whoseTurn));
	 all.push(new ManhattanMovespec(MOVE_DROP,seePersonalities,-1,whoseTurn));
	 
 }
 public boolean addPickAnyMoves(CommonMoveStack all)
 {	boolean some = false;
	if(pickedObject==null)
	{
	for(ManhattanCell c = allCells; c!=null && (all!=null ||!some); c=c.next)
		{
		if(c.topChip()!=null) 
			{ if(all!=null) { all.addElement(new ManhattanMovespec(MOVE_PICK,c,-1,whoseTurn)); }}
			  some = true;
		}
	for(PlayerBoard pb : pbs) 
		{ 
		for (ManhattanCell c = pb.allCells; c!=null && (all!=null || !some); c=c.next)
			{
			if(c.topChip()!=null) { if(all!=null) { all.addElement(new ManhattanMovespec(MOVE_PICK,c,-1,whoseTurn)); }}
			some = true;
			}
		}
	}
	else
	{	Type pickedType = pickedObject.type;
		for(ManhattanCell c = allCells; c!=null && (all!=null | !some); c=c.next)
		{
		if(pickedType.canDropOn(c)) 
			{ 	
				if(all!=null) { all.addElement(new ManhattanMovespec(MOVE_DROP,c,-1,whoseTurn)); }
				some = true;
			}
		}
		for(PlayerBoard pb : pbs) 
		{ 
		pb.findEmptyStockPile();
		pb.findBuildingSlot();
		for (ManhattanCell c = pb.allCells; c!=null && (all!=null || !some); c=c.next)
			{
			if(pickedType.canDropOn(c)) 
				{ boolean ok = false;
				  if(pickedType==Type.Worker)
				  {	// keep the worker source cells populated with the right type
					  switch(c.rackLocation())
					  {
					  case Workers:
						  ok = pickedObject.workerType==WorkerType.L;
						  break;
					  case Scientists:
						  ok = pickedObject.workerType==WorkerType.S;
						  break;
					  case Engineers:
						  ok = pickedObject.workerType==WorkerType.E;
						  break;
					  default: ok = true;
							  
					  }
				  }
				  else
				  {	ok = true;
				  }
				  if(ok)
				  {
				  if(all!=null) 
				  	{ ManhattanMovespec m = new ManhattanMovespec(MOVE_DROP,c,-1,whoseTurn);
				  	  all.addElement(m);
				  	}
				  some = true;
				  }
				
				}
			}
		}
	}
	return some;
 }
 /**
  * all players except the korean player have approved the payment
  * @return
  */
 public boolean allApprovedExcept()
 {
 	 for(PlayerBoard pb : pbs) 
 	 	{ if((pb.boardIndex!=northKoreanPlayer) && !pb.approvedNorthKorea) { return false; } 	 	
 	 	}
 	 return true;
 }
 /**
  * all players including the korean player have approved the payment
  * @return
  */
 private boolean allApproved()
 {
 	 for(PlayerBoard pb : pbs) 
 	 	{ if(!pb.approvedNorthKorea) { return false; } 	 	
 	 	}
 	 return true;
 }
 private boolean addNorthKoreaMoves(CommonMoveStack all,int who)
 {	 
 	 if(all==null) { return true; }
 	 // only generate moves for the bot
	 return pbs[who].addNorthKoreaMoves(all,who);
 }
 public boolean hasMoves()
 {
	 boolean has = addMovesForState(null,board_state);
	// test for consistency between the quick method and the full method
	 if(G.debug())
	 {
	 CommonMoveStack c = new CommonMoveStack();
	 boolean has2 = addMovesForState(c,board_state);
	 if((has!=has2) || ((c.size()!=0) != has))
	 {	if(board_state!=ManhattanState.Gameover)
	 	{
		 G.Error("has moves after all "+c.top());
	 	}
	 }}
	 return has;
 }
 CommonMoveStack  GetListOfMoves(boolean forGui)
 {	CommonMoveStack all = new CommonMoveStack();
  	addMovesForState(all,board_state);
 	if(all.size()==0)
 		{ p1("nomoves-"+moveNumber+"-"+board_state);
 		  // this is rare but can be legitimate, if we most recently retrieved
 		  // and there are no moves available (excluding the bot's sensible exclusions)
 		  // this will never happen in a real game, only in the bot lookahead.
 		  ManhattanMovespec m = new ManhattanMovespec(MOVE_DONE,whoseTurn);
 		  m.from_index = 99;	// mark this as a desperation move
 		  all.push(m);
 		}
 	return all;
 }
 private void preparePersonalityMoves()
 {		
 		int n =availablePersonalities.height();
 		prepareChoices(n);
	 	for(int i=0;i<n; i++)
	 	{	ManhattanChip ch = availablePersonalities.chipAtIndex(i);
	 		choice[i].addChip(ch);
	 	}
	 	pendingBenefit = Benefit.Personality;
	 	pendingCost = Cost.None;
	 	setState(ManhattanState.SelectPersonality);
 }
 private boolean addMovesForState(CommonMoveStack all,ManhattanState state)
 {	boolean some = false;
 	switch(state)
 	{
 	case Puzzle:
 		{	// allow anything to be picked up, and if picked up, to be dropped in any compatible cell
 			some = addPickAnyMoves(all);
 		}
 		break;
 	case North_Korea_Dialog:
 		some |= addNorthKoreaMoves(all,whoseTurn);
 		break;
 	case ConfirmPayment:
 	case ConfirmChoice:
 	case ConfirmBenefit:
 		if(robot!=null) 
 			{ 
 			if(all!=null) { all.push(new ManhattanMovespec(MOVE_DONE,whoseTurn)); }
 			some = true;
 			break; 	// the robot doesn't second guess itself
 			}
		//$FALL-THROUGH$
	case ResolvePayment:
 	case CollectBenefit:
 	case ResolveChoice:
 	case SelectPersonality:
 		ManhattanId notfrom = null;
 		boolean isRobot = robot!=null;
 		if(isRobot)
 		{
 		switch(pendingBenefit)
 		{
 		case Trade:
 			// for the robot, ignore the row we already selected from
 			if(selectedCells.size()>0) { notfrom = selectedCells.top().rackLocation(); }
 			break;
 		default: break;
 		}}
		for(int i=0,lim=displayCells.size();i<lim && (all!=null || !some); i++)
 		{
 		ManhattanCell c = displayCells.elementAt(i);
 		if((c.topChip()!=null)
 				// for the robot, only turn things on, never off
 				&& (!isRobot || !selectedCells.contains(c))
 				&& c.rackLocation()!=notfrom)
 			{ if(all!=null) { all.push(new ManhattanMovespec(MOVE_SELECT,c,-1,whoseTurn)); } }
 				some |= true;
 			}
 		break;
 	case ConfirmNichols:
 		if(robot==null) { some |= addNicholsMoves(all,pbs[whoseTurn],whoseTurn); }
		//$FALL-THROUGH$
	case ConfirmSingleAirstrike:
	case ConfirmAirstrike:
	case ConfirmJapanAirstrike:
 	case Confirm:
 	case ConfirmWorker:
 	case ConfirmRetrieve:
 		if(all!=null) { all.push(new ManhattanMovespec(MOVE_DONE,whoseTurn)); }
 		some = true;
 		break;
 	case Airstrike:
 	case JapanAirstrike:
 		some = addAirstrikeMoves(all,false,whoseTurn);
 		if(all!=null) { all.push(new ManhattanMovespec(MOVE_DONE,whoseTurn)); }
 		some = true;	// can always pass out of airstrikes
 		break;
 	case PlayOrRetrieve:
 		some = true;
 		if(all!=null) { all.push(new ManhattanMovespec(MOVE_RETRIEVE,getPlayerColor(whoseTurn),whoseTurn)); }
 		else { break; }
		//$FALL-THROUGH$
	case Play:
		some |= addMainboardMoves(all,whoseTurn);
		if(some && all==null) { break; }
		some |= addPlayerBoardMoves(all,pbs[whoseTurn],whoseTurn);
		// under rare circumstances, there can be legitimately no moves
		break;
	case Resign:
	case NoMovesState:
		if(all!=null) { all.push(new ManhattanMovespec(MOVE_DONE,whoseTurn)); }
		some = true;
		break;
		
	case PlayEspionage:
		some = addPlayerEspionageMoves(all,pbs[whoseTurn],whoseTurn);
		//$FALL-THROUGH$
	case PlayLocal:
		// in playlocal, you can always do nothing
		some |= addPlayerBoardMoves(all,pbs[whoseTurn],whoseTurn);
		PlayerBoard pb = pbs[whoseTurn];
		if(pb.hasPersonality(ManhattanChip.Fuchs)
				&& !pb.testOption(TurnOption.FuchsEspionage))
		{
			some |= addPlayerEspionageMoves(all,pb,whoseTurn);
		}
		if(all!=null) { all.push(new ManhattanMovespec(MOVE_DONE,whoseTurn)); }
		some = true;
 		break;
 		

	case ConfirmSelectBuilding:
		if(robot!=null) 
		{ 
		if(all!=null) { all.push(new ManhattanMovespec(MOVE_DONE,whoseTurn)); }
		some = true;
		break; 	// the robot doesn't second guess itself
		}
		//$FALL-THROUGH$
	case SelectBuilding:
		some = addSelectBuildingMoves(all,getDest(),whoseTurn);
		break;
	case PlayScientist:
	case Play2Scientists:
		some = addScientistMoves(all,getDest(),whoseTurn);
		break;
	case BuildIsraelBomb:
		some = addIsraelBombMoves(all,whoseTurn);
		break;
	case Play2Engineers:
	case PlayEngineer:
		some = addEngineerMoves(all,getDest(),whoseTurn);
		break;
	case NeedWorkers:
		if(needEngineers>0) { some = addEngineerMoves(all,getDest(),whoseTurn); }
		if(some && (all==null || robot!=null)) { break;}
		if(needScientists>0) {  some |= addScientistMoves(all,getDest(),whoseTurn); }
		break;
		
	case ConfirmRepair:	// include the selections in the confirm state
		if(robot!=null) 
			{ 
			if(all!=null) { all.push(new ManhattanMovespec(MOVE_DONE,whoseTurn)); }
			some = true;
			break; 	// the robot doesn't second guess itself
			}
		//$FALL-THROUGH$
	case Repair:
	case PaidRepair:
		some = addRepairMoves(all,whoseTurn);
		if(all!=null) { all.push(new ManhattanMovespec(MOVE_DONE,whoseTurn)); }
		some = true;	// can always pass out of repair
		break;

	case ConfirmRetrieve1:	// include the selection moves in the confirm state
		if(robot!=null) 
		{ 
		if(all!=null) { all.push(new ManhattanMovespec(MOVE_DONE,whoseTurn)); }
		some = true;
		break; 	// the robot doesn't second guess itself
		}
		//$FALL-THROUGH$
	case RetrieveSorE:	// germany
		some = addRetrieveSorEMoves(all,whoseTurn);
		break;
	case ConfirmDiscard:
		if(robot!=null) 
		{ 
		if(all!=null) { all.push(new ManhattanMovespec(MOVE_DONE,whoseTurn)); }
		some = true;
		break; 	// the robot doesn't second guess itself
		}
		//$FALL-THROUGH$
	case DiscardBombs:
	case DiscardOneBomb:
		some = addDiscardBombMoves(all,whoseTurn);
		break;
	case PlayAny3Workers:
	case PlayAny2Workers:
		some = addAddWorkersMoves(all,getDest(),whoseTurn);
		break;
 	case Retrieve:
 		if(all!=null) { all.push(new ManhattanMovespec(MOVE_RETRIEVE,getPlayerColor(whoseTurn),whoseTurn)); }
 		some = true;
 		break;
 	case Gameover:
 		some = true;	// not true but has the desired effect for hasMoves()
 		break;
 	default:
 			G.Error("Not expecting state %s",state);
 	}
 	return(some);
 }
 /**
  * set inhibited for some set of cells.  This is how the robot prevents
  * unreasonable or useless moves.
  * @param m
  */
 public void setInhibitions(ManhattanPlay m)
 {
	 for(ManhattanCell c = allCells; c!=null; c=c.next)
	 {
		 c.inhibited = false;
		 m.setInhibitions(c);
	 }
	 for(PlayerBoard pb : pbs)
	 {
		 pb.setInhibitions(m);
	 }
 }
 public void initRobotValues(ManhattanPlay m)
 {	robot = m;
 }


 
 /**
  *  get the board cells that are valid targets right now, intended to be used
  *  by the user interface to determine where it's legal to play.  The standard
  *  method is to call the move generator, and filter the results to generate
  *  the cells of interest.  It's usually more complicated than just that,
  *  but using the move generator to drive the selection of cells to point 
  *  at avoids duplicating a lot of tricky logic.
  *  
  * @return
  */
 public Hashtable<ManhattanCell, ManhattanMovespec> getTargets() 
 {
 	Hashtable<ManhattanCell,ManhattanMovespec> targets = new Hashtable<ManhattanCell,ManhattanMovespec>();
 	CommonMoveStack all = GetListOfMoves(true);
 	
 	if(pickedObject==null)
 	{
 	addSelectableMoves(all);
 	ManhattanCell dest = getDest();
 	if(dest!=null)
 	 	{	// if we moved something, allow picking it up
 	 		targets.put(dest,new ManhattanMovespec(MOVE_PICK,dest,-1,whoseTurn));
 	 	}
 	 for(int lim=all.size()-1; lim>=0; lim--)
 	 {	ManhattanMovespec m = (ManhattanMovespec)all.elementAt(lim);
 		switch(m.op)
 		{
 		case MOVE_ATTACK:
 		case MOVE_FROM_TO:
 			m.op = MOVE_PICK;
			//$FALL-THROUGH$
		case MOVE_PICK:
 			targets.put(getCell(m.source,m.from_color,m.from_row),m);
 			break;	
		case MOVE_SELECT:
		case MOVE_DROP:
 	 			targets.put(getCell(m.dest,m.to_color,m.to_row),m);
 	 			break;
		case MOVE_REPAIR:
			targets.put(getCell(m.dest,m.to_color,m.to_row),m);
			break;

 		case MOVE_RETRIEVE:
 		case MOVE_DONE:
 		case MOVE_APPROVE:
 		case MOVE_CONTRIBUTE:
 		case EPHEMERAL_APPROVE:
 			break;

 		default: G.Error("Not expecting "+m);
 		
 		}
 	}}
 	else
 		{	// something already picked
 	 	ManhattanCell src = getSource();
 	 	if(src!=null)
 	 	{
 	 		targets.put(src,new ManhattanMovespec(MOVE_DROP,src,pickedIndex.top(),whoseTurn));
 	 	}
 		for(int lim=all.size()-1; lim>=0; lim--)
 	 	{	ManhattanMovespec m = (ManhattanMovespec)all.elementAt(lim);
 	 	
 	 		switch(m.op)
 	 		{
 	 		case MOVE_ATTACK:
 	 		case MOVE_FROM_TO:
 	 			m.op = MOVE_DROP;
				//$FALL-THROUGH$
 			case MOVE_DROP:
 	 			targets.put(getCell(m.dest,m.to_color,m.to_row),m);
 	 			break;
 	 			
 	
	 		case MOVE_SELECT:
 	 		case MOVE_PICK:
  	 		case MOVE_RETRIEVE:
 	 		case MOVE_DONE:
 	 			break;

 	 		default: G.Error("Not expecting "+m);
 	 		
 	 		}	 		{
 	 		
 	 		}
 		}
 	
 	}
 	
 	return(targets);
 }
 
Rectangle boardRect = new Rectangle();
public void setRectangle(Rectangle r)
{	if(!G.sameRects(boardRect,r))
	{	G.copy(boardRect,r);
		positionCells();
	}
}

public int cellToX(ManhattanCell c)
{
	return G.Left(boardRect) +(int)( G.Width(boardRect)*c.xpos);
}
public int cellToY(ManhattanCell c)
{
	return G.Top(boardRect)+(int)(G.Height(boardRect)*c.ypos);
}

 public void positionCells()
 {
	 seeBribe.setPosition(0.04,0.2);
	 seeBank.setPosition(0.03,0.08);
	 seeDamage.setPosition(0.07,0.38);
	 seeYellowcake.setPosition(0.03,0.13);
	 availableWorkers.setPosition(0.12,0.35);
	 ManhattanCell.setPosition(seeEspionage,  0.023,0.92,  0.026,0.532);
	 playEspionage.setPosition(0.09,0.54);
	 // original board playDesignBomb.setPosition(0.6,0.79);
	 playDesignBomb.setPosition(0.575,0.75);
	 bombtestHelp.setPosition(0.64,0.69);
	 seeBombtests.setPosition(0.64,0.92);
	 seeCurrentDesigns.setPosition(0.56,0.89);
	 playMakePlutonium.setPosition(0.69,0.79);
	 playMakeUranium.setPosition(0.75,0.79);
	 playRepair.setPosition(0.49,0.55);
	 playMakeFighter.setPosition(0.565,0.55);
	 playMakeBomber.setPosition(0.615,0.55);
	 seeBuildings.setPosition(0.93,0.175);
	 seeBombs.setPosition(0.09,0.68);
	 seeDiscardedBombs.setPosition(0.08,0.68);
	 seePersonalities.setPosition(0.09,0.78);
	 seeNations.setPosition(0.09,0.88);
	 playBuyBuilding.setPosition(0.16,0.52);
	 playBuyBuilding2.setPosition(0.27,0.52);
 	 
	 ManhattanCell.setPosition(playMine,    0.19,0.79,   0.288,0.79);
	 ManhattanCell.setPosition(seeBuilding,    0.16,0.2,   0.8, 0.2);
	 ManhattanCell.setPosition(playUniversity,  0.355,0.79,   0.49,0.79);
	 ManhattanCell.setPosition(playAirStrike,  0.36,0.55,   0.405,0.55);
	 ManhattanCell.setPosition(playMakeMoney,   0.67,0.55,  0.768,0.55);
	 ManhattanCell.setPosition(seePlutonium,   0.87,0.89,  0.875,0.45);
	 ManhattanCell.setPosition(seeUranium,   0.94,0.89,  0.945,0.45);
}

ManhattanChip getABomb()
{	
	if(seeBombs.height()==0) 
	{ while(seeDiscardedBombs.height()>0) { seeBombs.addChip(seeDiscardedBombs.removeTop()); }
	  seeBombs.shuffle(new Random((moveNumber*players_in_game+whoseTurn)+randomKey));
	}
	if(seeBombs.height()>0) { return seeBombs.removeTop(); }
	return null;
}
int nBombsAvailable()
{
	return seeBombs.height()+seeDiscardedBombs.height();
}
//
// load the current bomb designs, in random order, into the choice array.
// plus a "blind" bomb for the french nations style of selection
//
public void loadBombDesigns(boolean france) 
{
	int nChoices = seeCurrentDesigns.height();
	choiceOut[0].copyFrom(seeCurrentDesigns);
	choiceOut[0].shuffle(new Random((moveNumber*players_in_game+whoseTurn)+randomKey)); 
	displayCells.clear();
	selectedCells.clear();
	for(int i=0;i<nChoices;i++)
	{
		choice[i].reInit();
		choice[i].addChip(choiceOut[0].chipAtIndex(i));
		displayCells.push(choice[i]);
	}
	if(france)
	{
	if(nBombsAvailable()>0)
	{
	choice[nChoices].reInit();
	choice[nChoices].addChip(ManhattanChip.BombBack);
	displayCells.push(choice[nChoices]);
	}
	pendingBenefit = Benefit.FrenchBombDesign;
	}
	else
	{
	pendingBenefit = Benefit.BombDesign;
	}
}
//
// this produces the lines that are displayed at the top of the board
//
public String getStateDescription(ManhattanState state)
{
	String b = state.description();
	switch(state)
	{
	case PlayEspionage:
		b = s.get(b,espionageSteps);
		break;
	case NeedWorkers:
		b = s.get(b,needScientists,needEngineers);
		break;
	case Repair:
	case PaidRepair:
		b = s.get(b,repairCounter);
		break;
	default: 
		b = s.get(b);
		break;
	}
	return b;
}
// prepare displayCells to make the first N choices
// choices are cleared and ready to be filled.
public void prepareChoices(int n) {
	displayCells.clear();
	selectedCells.clear();
	for(int i=0;i<n;i++)
	{	choice[i].reInit();
		displayCells.push(choice[i]);
	}
	
}

}
