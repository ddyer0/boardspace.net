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
package blackdeath;

import static blackdeath.BlackDeathMovespec.*;

import java.util.*;

import blackdeath.BlackDeathConstants.BlackDeathColor;
import blackdeath.BlackDeathConstants.BlackDeathId;
import blackdeath.BlackDeathConstants.CardEffect;
import blackdeath.BlackDeathConstants.DiseaseMod;
import lib.*;
import lib.Random;
import online.game.*;

/**
 * BlackDeathBoard knows all about the game of BlackDeath
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
 */
class PlayerBoard implements Digestable,CompareTo<PlayerBoard>
{	static final int INITIAL_CHIP_COUNT = 18;
	static final int INITIAL_VIRULENCE = 3;
	static final int INITIAL_MORTALITY = 3;
	static final int INITIAL_BODYCOUNT = 0;
	static final int INITIAL_TOTAL_PIPS = 6;
	BlackDeathColor color;
	BlackDeathChip chip;
	BlackDeathCell chipCell;
	BlackDeathCell cards;
	BlackDeathChip modChip;
	
	BlackDeathCell temporaryCards;						// cards in effect until the player's next turn
	public CellStack closedRegions = new CellStack();	// regions closed by quarantine cards
	public LinkStack closedLinks = new LinkStack();		// routes closed by War cards
	public CellStack pogromRegions = new CellStack(); 	// regions poisoned by Pogrom
	public boolean hasCapability(CardEffect cap)
	{
		for(int lim=temporaryCards.height()-1; lim>=0; lim--)
		{
			if(temporaryCards.chipAtIndex(lim).cardEffect == cap) { return(true); }
		}
		return(false);
	}
	boolean virulenceSet = false;
	boolean initialPlacementDone = false;
	boolean initialInfectionDone = false;
	int index;
	int chipCount = INITIAL_CHIP_COUNT;
	int chipsOnBoard = 0;	// not maintained just for debugging use
	private int virulence = INITIAL_VIRULENCE;
	private int mortality = INITIAL_MORTALITY;
	private int baseVirulence = INITIAL_VIRULENCE;
	private int baseMortality = INITIAL_MORTALITY;
	int usedAutomaticWin = 0;
	public BlackDeathCell virulenceCells[] = new BlackDeathCell[7];
	public BlackDeathCell mortalityCells[] = new BlackDeathCell[7];
	public void resetVirulenceAndMortality()
	{
		setVirulence(baseVirulence,true);
		setMortality(baseMortality,true);
	}
	public int automaticWinLimit()
	{
		switch(modChip.getDiseaseMod())
		{
		case None: return(2);
		case Warm:
		case Cold:
		case Crowd:
		case Wet:	return(1);
		default: throw G.Error("Not expecting %s",modChip);
		}
	}
	public boolean canWinAutomatically(BlackDeathCell city)
	{	DiseaseMod mod = modChip.getDiseaseMod();
		switch(mod)
		{
		default: throw G.Error("Not expecting %s", modChip);
		case None:
			return(usedAutomaticWin<automaticWinLimit());
		case Warm:
		case Cold:
		case Crowd:
		case Wet:
			return((usedAutomaticWin<automaticWinLimit())
					&& (city.climate!=null)
					&& city.climate.test(mod));
		}
	}
	
	public int getVirulence() { return(virulence); }
	public int getMortality() { return(mortality); }

	public void setVirulence(int v,boolean permanant)
	{
		virulenceCells[virulence].reInit();
		virulence = v;
		if(permanant) { baseVirulence = v; }
		virulenceCells[virulence].addChip(chip);
	}
	public void setVirulence(int v) { setVirulence(v,true); }
	
	public void setMortality(int v,boolean permanant)
	{
		mortalityCells[mortality].reInit();
		mortality = v;
		if(permanant) { baseMortality = v; }
		mortalityCells[mortality].addChip(chip);
	}
	public void setMortality(int v) { setMortality(v,true); }
	
	public int adjustedVirulence(BlackDeathCell c)
	{	int v = virulence;
		if((c.climate!=null) && c.climate.test(modChip.getDiseaseMod())) { return(v++); }
		return(v);
	}
	public int adjustedMortality(BlackDeathCell c)
	{
		return(mortality);
	}
	
	int bodyCount = 0;
	
	BlackDeathBoard parent;
	PlayerBoard() { }
	PlayerBoard(int i,BlackDeathColor c,BlackDeathBoard b,BlackDeathChip ch,BlackDeathChip mod)
	{	
		index = i;
		color = c;
		chip = ch;
		parent = b;
		modChip = mod;
		bodyCount = 0;
		usedAutomaticWin = 0;
		Random r0 = new Random(3455346);
		Random r = new Random(924535+color.Digest(r0));
		
		chipCell = new BlackDeathCell(r,BlackDeathId.PlayerChips,color);
		cards = new BlackDeathCell(r,BlackDeathId.Cards,color);
		temporaryCards = new BlackDeathCell(r,BlackDeathId.TemporaryCards,color);
		for(int idx=0;idx<virulenceCells.length;idx++)
		{
			virulenceCells[idx] = new BlackDeathCell(r,BlackDeathId.PlayerVirulence,color,idx);
			mortalityCells[idx] = new BlackDeathCell(r,BlackDeathId.PlayerMortality,color,idx);
		}
		doInit();
	}
	public String toString() { return("<pb "+chip.color+">"); }
	public void doInit() 
	{
		chipCount = INITIAL_CHIP_COUNT;
		usedAutomaticWin = 0;
		cards.reInit();
		temporaryCards.reInit();
		chipCell.reInit();
		parent.reInit(virulenceCells);
		parent.reInit(mortalityCells);
		setVirulence(INITIAL_VIRULENCE);
		setMortality(INITIAL_MORTALITY);
		chipCell.addChip(chip);
		closedRegions.clear();
		closedLinks.clear();
		virulenceSet = false;
		initialPlacementDone = false;
		
	}

	public PlayerBoard clone() 	{ 	return(copyTo(null));	}
	
	public PlayerBoard copyTo(PlayerBoard to)
	{
		if(to==null ) { to = new PlayerBoard(); }
		
		// these need copyall because the cell incorporates the color which is changeable
		to.chipCell.copyAllFrom(chipCell);	// needed for animations of board copy
		to.cards.copyAllFrom(cards);
		to.temporaryCards.copyAllFrom(temporaryCards);

		to.index = index;
		to.color = color;
		to.parent = parent;
		to.chipCount = chipCount;
		to.virulence = virulence;
		to.mortality = mortality;
		to.baseVirulence = baseVirulence;
		to.baseMortality = baseMortality;
		to.usedAutomaticWin = usedAutomaticWin;
		to.virulenceSet = virulenceSet;
		to.bodyCount = bodyCount;
		to.initialPlacementDone = initialPlacementDone;
		to.initialInfectionDone = initialInfectionDone;
		parent.getCell(to.closedRegions,closedRegions);
		to.closedLinks.copyFrom(closedLinks);
		parent.copyFrom(to.mortalityCells,mortalityCells);
		parent.copyFrom(to.virulenceCells,virulenceCells);
		return(to);
	}
	
	public long Digest(Random r) {
		long v = color.Digest(r);
		v ^= chipCount*r.nextLong();
		v ^= virulence*r.nextLong();
		v ^= mortality*r.nextLong();
		v ^= bodyCount*r.nextLong();
		v ^= baseVirulence*r.nextLong();
		v ^= baseMortality*r.nextLong();
		v ^= usedAutomaticWin*r.nextLong();
		v ^= (virulenceSet?1235:023562)*r.nextLong();
		v ^= (initialPlacementDone?523:252646)*r.nextLong();
		v ^= cards.Digest(r);
		v ^= parent.Digest(r,initialInfectionDone);
		v ^= temporaryCards.Digest(r);
		v ^= parent.Digest(r,closedRegions);
		v ^= closedLinks.Digest(r);
		return v;
	}
	public void sameBoard(PlayerBoard other)
	{
		G.Assert(index==other.index,"index mismatch");
		G.Assert(color==other.color,"Color mismatch");
		G.Assert(parent==other.parent,"parent mismatch");
		G.Assert(chipCount==other.chipCount,"chipcount mismatch");
		G.Assert(initialInfectionDone==other.initialInfectionDone,"initialInfectionDone mismatch");
		G.Assert(virulence==other.virulence,"virulence mismatch");
		G.Assert(baseVirulence==other.baseVirulence,"baseVirulence mismatch");
		G.Assert(usedAutomaticWin==other.usedAutomaticWin,"automaticWin mismatch");
		G.Assert(virulenceSet==other.virulenceSet,"virulenceSet mismatch");
		G.Assert(initialPlacementDone==other.initialPlacementDone,"initialPlacementDone mismatch");
		G.Assert(mortality==other.mortality,"mortality mismatch");
		G.Assert(baseMortality==other.baseMortality,"baseMortality mismatch");
		G.Assert(bodyCount==other.bodyCount,"Bodycount mismatch");
		G.Assert(cards.sameContents(other.cards),"Cards mismatch");
		G.Assert(temporaryCards.sameContents(other.temporaryCards),"temporaryCards mismatch");
		G.Assert(parent.sameCells(closedRegions, other.closedRegions),"closedRegions mismatch");
		G.Assert(closedLinks.sameContents(other.closedLinks),"closed links mismatch");
		long v1 = Digest(new Random(35325));
		long v2 = other.Digest(new Random(35325));
		G.Assert(v1==v2,"Digest matches");
	}
	
	private long randomLong()
	{	Random r = new Random(253636098);
		return(new Random(Digest(r)).nextLong());
	}

	public int compareTo(PlayerBoard o) {
		// this is used to order players by body count
		if(bodyCount<o.bodyCount) { return(1); }
		if(bodyCount>o.bodyCount) { return(-1); }
		// same body count
		if(chipCount<o.chipCount) { return(1); }
		if(chipCount>o.chipCount) { return(-1); }
		// same chip count too, resolve randomly but consistently
		return(G.signum(randomLong()-o.randomLong()));
	}
	public int altCompareTo(PlayerBoard o) {
		return -compareTo(o);
	}

}

class BlackDeathLink implements Digestable
{
	BlackDeathCell from;
	BlackDeathCell to;
	int cost;
	public String toString() { return("<link "+from+" "+to+">"); }
	BlackDeathLink(BlackDeathCell fr,BlackDeathCell toc,int c)
	{
		from = fr;
		to = toc;
		cost = c;
	}
	static BlackDeathLink zeroCostLink = new BlackDeathLink(null,null,0);
	{	
	}
	public long Digest(Random r) {
		long v = 0;
		if(from!=null) { v ^= from.Digest(r)*12677243; }
		if(to!=null) { v ^= to.Digest(r)*152736457; }
		return(v);
	}
}
class LinkStack extends OStack<BlackDeathLink> implements Digestable
{
	public BlackDeathLink[] newComponentArray(int sz) {
		return new BlackDeathLink[sz];
	}
	public long Digest(Random r) {
		long v = 0;
		for(int lim=size()-1; lim>=0; lim--) { v ^= elementAt(lim).Digest(r); } 
		return(v);
	}
}

public class BlackDeathBoard extends RBoard<BlackDeathCell> implements BoardProtocol,BlackDeathConstants
{	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	static final int MAX_PLAYERS = 6;
	static final int MIN_INITIAL_VIRULENCE = 1;
	static final int MAX_INITIAL_VIRULENCE = 5;
	static final int MIN_VIRULENCE = 0;
	static final int MAX_VIRULENCE = 6;
	BlackDeathVariation variation = BlackDeathVariation.blackdeath;
	private BlackDeathState board_state = BlackDeathState.Puzzle;	
	private BlackDeathState unresign = null;	// remembers the orignal state when "resign" is hit
	public int nextPlayer() { return(nextPlayer(whoseTurn)); }
	public int nextPlayer(int n) 
	{ 	for(int i=0;i<players_in_game;i++)
		{ if(turnOrder[i]==whoseTurn) 
			{ return(turnOrder[(i+1)%players_in_game]);
			}			
		}
		throw G.Error("current player not found!");
	}
	public int getAdjustedMortality(PlayerBoard pbc)
	{	int baseline =pbc.getMortality();
		for(PlayerBoard pb : pbs)
		{
			for(int lim=pb.temporaryCards.height()-1; lim>=0; lim--)
			{
				BlackDeathChip chip = pb.temporaryCards.chipAtIndex(lim);
				switch(chip.cardEffect)
				{
				case Famine:
					p1("famine in effect");
					baseline++;
					break;
				case MutationSwap:
					p1("Mutation Swap ");
					break;
				case MutationVirulenceOrMortality:
					p1("Mutation VirulenceOrMortality ");
					break;
				case MutationVirulenceAndMortality:
					p1("Mutation VirulenceAndMortality ");
					break;
				default: break;
				}
			}
		}
		return(baseline);
	}
	public BlackDeathCell drawPile;
	public BlackDeathCell discardPile;
	
	PlayerBoard pbs[] = null;
	public int nDice = 3;
	public int turnOrder[] = AR.intArray(MAX_PLAYERS);
	private int movementPoints = 0;
	private boolean mongolsInEffect = false;
	private boolean crusadeInEffect = false;
	StringStack gameEvents = new StringStack();
	InternationalStrings s = G.getTranslations();
	public int getMovementPoints() { return(movementPoints); }
	public void setMovementPoints(int n) { movementPoints = n; }
	public int adjustMovementPoints(int startingN)
	{	// adjust movement points according to temporary cards
		int n = startingN;
		crusadeInEffect = false;
		mongolsInEffect = false;
		
		for(PlayerBoard pb : pbs)
		{
			for(int lim = pb.temporaryCards.height()-1; lim>=0; lim--)
			{
				BlackDeathChip chip = pb.temporaryCards.chipAtIndex(lim);
				switch(chip.cardEffect)
				{
				case SlowTravel:
				case Famine:
					p1("lower movement");
					n--;
					break;
				case Crusade:
					crusadeInEffect = true;
					break;
				case Mongols:
					p1("higher movement");
					mongolsInEffect = true;
					n++;
					break;
				default: break;
				}
			}
		}
		return(Math.max(0, n));
	}
	int closeLinkPoints = 0;
	private int infectionPoints = 0;
	public int getInfectionPoints() { return(infectionPoints); }
	public void setInfectionPoints(int n) { infectionPoints = n; }
	public int adjustInfectionPoints(int startingN)
	{	// adjust movement points according to temporary cards
		int n = startingN;
		for(PlayerBoard pb : pbs)
		{
			for(int lim = pb.temporaryCards.height()-1; lim>=0; lim--)
			{
				BlackDeathChip chip = pb.temporaryCards.chipAtIndex(lim);
				switch(chip.cardEffect)
				{
				case LowVirulence:
					p1("lower virulence");
					n--;
					break;
				case HighVirulence:
					p1("higher virulence");
					n++;
					break;
				default: break;
				}
			}
		}
		return(Math.max(0, n));
	}
	public int killPoints = 0;
	public int killVictim = 0;
	public boolean escapeState = false;
	public boolean perfectlyRolling = false;
	public int initialTotalPips = PlayerBoard.INITIAL_TOTAL_PIPS;
	public int sweep_counter = 1;
	public PlayerBoard getCurrentPlayer() { return(pbs[whoseTurn]); }
	public PlayerBoard getPlayer(int n) { return(pbs[n]); }
	public PlayerBoard getPlayer(BlackDeathColor c) 
		{ for(PlayerBoard pb : pbs)
			{	if(pb.color==c) { return(pb);}
			}
		throw G.Error("Color %s not found",c);
		}
	public BlackDeathState getState() { return(board_state); }
    /**
     * this is the preferred method when using the modern "enum" style of game state
     * @param st
     */
	public void setState(BlackDeathState st) 
	{ 	unresign = (st==BlackDeathState.Resign)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}

	private boolean allVirulenceSet()
	{
		for(PlayerBoard pb : pbs) { if(!pb.virulenceSet) { return(false); }}
		return(true);
	}
	private boolean allPlacementsMade()
	{
		for(PlayerBoard pb : pbs) { if(!pb.initialPlacementDone) { return(false); }}
		return(true);
	}
	private boolean allInitialInfectionsDone()
	{
		for(PlayerBoard pb : pbs) 
			{ if(!pb.initialInfectionDone) 
				{ return(false); }
			}
		return(true);
	}
	

// this is required even if it is meaningless for this game, but possibly important
// in other games.  When a draw by repetition is detected, this function is called.
// the game should have a "draw pending" state and enter it now, pending confirmation
// by the user clicking on done.   If this mechanism is triggered unexpectedly, it
// is probably because the move editing in "editHistory" is not removing last-move
// dithering by the user, or the "Digest()" method is not returning unique results
// other parts of this mechanism: the Viewer ought to have a "repRect" and call
// DrawRepRect to warn the user that repetitions have been seen.
	public void SetDrawState() {throw G.Error("not expected"); };	
	CellStack animationStack = new CellStack();

	// intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public BlackDeathChip pickedObject = null;
    public int pickedIndex = -1;
    public BlackDeathChip lastPicked = null;
    private CellStack pickedSourceStack = new CellStack(); 
    private CellStack droppedDestStack = new CellStack();
    private CommonMoveStack destMoveStack = new CommonMoveStack();
    private StateStack stateStack = new StateStack();
    private int dropCost = 0;
    
    public BlackDeathChip lastRoll = null;
    
    BlackDeathState resetState = BlackDeathState.Puzzle; 
    public BlackDeathChip lastDroppedObject = null;	// for image adjustment logic


	public BlackDeathChip getPlayerChip(int pl) { return(pbs[pl].chip); }
	public BlackDeathChip getPlayerDisease(int pl) { return(pbs[pl].modChip); }
	public BlackDeathColor getPlayerColor(int pl) { return(pbs[pl].color); }
	public PlayerBoard getPlayerBoard(int pl) { return(pbs[pl]); }
	
	// constructor 
    public BlackDeathBoard(String init,int players,long key,int map[],int rev) // default constructor
    {
        setColorMap(map, players);
        reloadCells();
        doInit(init,key,players,rev); // do the initialization 
    }
    
    public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
    private Hashtable<String,BlackDeathCell>cities = new Hashtable<String,BlackDeathCell>();
    // construct a city cell
    private Random randomInit;
    private BlackDeathCell bd(String name,double px,double py, int co,Bitset<DiseaseMod> set)
    {	
    	BlackDeathCell c = new BlackDeathCell(randomInit,BlackDeathId.BoardLocation,name);
    	if(set==null) { set = new Bitset<DiseaseMod>(); }
    	c.climate = set;
    	c.xpos = px;
    	c.ypos = py;
    	c.cost = co;
    	c.onBoard = true;
    	c.next = allCells;	// allcells is a list of all cities
    	allCells = c;
    	int ind = name.indexOf('#');
    	if(ind>=0)
    	{	// parent has a list of alternate squares for the city
    		// each subcity points back to the parent.
    		BlackDeathCell parent = findCity(name.substring(0,ind));
    		c.sisterCity = parent.sisterCity;
    		c.parentCity = parent;
    		c.name=name;
    		parent.sisterCity = c;
    	}
    	else
	    	{
    	   	c.name = name;
        	c.sisterCity = null;
	    	c.parentCity = c;
	    	}
   		cities.put(name,c);
   		return(c);
    }
    
    private BlackDeathCell findCity(String name)
    {
    	BlackDeathCell c = cities.get(name);
    	G.Assert(c!=null,"city %s not found",name);
    	return(c);
    }
    private void ln(String fr,String toc,int cost)
    {
    	BlackDeathCell from = findCity(fr);
    	BlackDeathCell to = findCity(toc);
    	BlackDeathLink link = new BlackDeathLink(from,to,cost);
    	BlackDeathLink link2 = new BlackDeathLink(to,from,cost);
    	from.addCityLink(link);
    	to.addCityLink(link2);
    }
    private CellStack standardInitialPlacement = new CellStack();
    private CellStack extendedInitialPlacement = new CellStack();
    private CellStack initialPlacement = null;
    private CellStack externalPlacement = new CellStack();
    public BlackDeathCell mortalityTable = null;
    public BlackDeathCell initialDice1 = null;
    public BlackDeathCell initialDice2 = null;
    public BlackDeathCell initialDice3 = null;
    public String easternNames[] = {"Moscow","Damascus","Jerusalem","Cairo"};
    
    private void reloadCells()
    {	allCells = null;
    	standardInitialPlacement.clear();
    	extendedInitialPlacement.clear();
    	randomInit = new Random(125261234);
    	drawPile = new BlackDeathCell(randomInit,BlackDeathId.DrawPile,"drawPile");
    	discardPile = new BlackDeathCell(randomInit,BlackDeathId.DiscardPile,"discardPile");
    	    	
    	mortalityTable = bd("mortality",4,22,0,null);	// mortality table, display dice location
       	initialDice1 = bd("dice_top",19,18,0,null);		// display dice location
       	initialDice2 = bd("dice_middle",19,22,0,null);	// display dice location
        initialDice3 = bd("dice_bottom",19,26,0,null);	
       	bd("Oslo",43.9,24.6,-1,new Bitset<DiseaseMod>(DiseaseMod.Cold));
    	bd("Stockholm",52,25.4,-1,null);
    	bd("Copenhagen",46.3,34.0,0,null);
    	bd("Edinburgh",26.3,32.4,0,null);
    	bd("London",28.5,44,1,new Bitset<DiseaseMod>(DiseaseMod.Wet));
    	bd("London#3",27,40,0,new Bitset<DiseaseMod>(DiseaseMod.Wet));
    	bd("London#2",30,40,0,new Bitset<DiseaseMod>(DiseaseMod.Wet));
    	bd("Amsterdam",36.5,41.8,-1,null);	
    	bd("Brussels",35,47.4,0,null);
    	bd("Lubeck",42.4,40.4,-1,null);
    	bd("Lubeck#2",45.2,40.4,0,null);
    	bd("Gdansk",54.4,37.4,0,null);
    	bd("Warsaw",58.2,42.4,0,null);
    	bd("Prague",49.3,48,0,null);
    	bd("Paris",31.2,51.8,0,new Bitset<DiseaseMod>(DiseaseMod.Crowd));
    	bd("Paris#2",28.2,51.8,0,new Bitset<DiseaseMod>(DiseaseMod.Crowd));
    	bd("Paris#3",29.7,55.6,1,new Bitset<DiseaseMod>(DiseaseMod.Crowd));
    	bd("Munich",44.5,52.8,-1,null);
    	bd("Vienna",52.5,52.9,0,null);
    	bd("Budapest",57,54,0,null);
    	bd("Geneva",37.5,57,-1,null);
    	bd("Lyon",33.8,59.6,0,null);
    	bd("Marseille",35,65,0,null);
    	bd("Genoa",41,61.5,0,null);
    	bd("Genoa#2",41,65,1,null);
    	bd("Venice",46.4,59.7,0,new Bitset<DiseaseMod>(DiseaseMod.Wet));
    	bd("Venice#2",49.3,59.7,1,new Bitset<DiseaseMod>(DiseaseMod.Wet));
    	bd("Belgrade",59.7,60.4,0,null);
    	bd("Bucharest",68.8,59,0,null);
    	bd("Rome",45.7,67.1,0,new Bitset<DiseaseMod>(DiseaseMod.Wet));
    	bd("Rome#",48.4,67.1,1,new Bitset<DiseaseMod>(DiseaseMod.Wet));
    	bd("Barcelona",29.5,69,0,null);
    	bd("Algiers",30,80.7,0,null);
    	bd("Tunis",43.2,80.8,0,new Bitset<DiseaseMod>(DiseaseMod.Warm));
    	bd("Naples",50.9,72.1,0,null);
    	bd("Naples#2",53.6,72.1,1,null);
    	bd("Cadiz",13.2,78.4,0,new Bitset<DiseaseMod>(DiseaseMod.Warm));
    	bd("Lisbon",9.4,71.6,0,null);
    	bd("Madrid",20.5,70,1,new Bitset<DiseaseMod>(DiseaseMod.Crowd));
    	bd("Madrid#2",17.7,70,0,new Bitset<DiseaseMod>(DiseaseMod.Crowd));
    	bd("Bilbao",21.9,63.2,0,null);
    	bd("Dublin",20.7,38.3,0,null);
    	bd("Reykjavik",12.5,8.5,-1,null);
    	bd("Helsinki",59.7,22.1,-1,new Bitset<DiseaseMod>(DiseaseMod.Cold));
    	bd("Novgorod",67.5,24,1,new Bitset<DiseaseMod>(DiseaseMod.Cold));
    	standardInitialPlacement.push(bd("Moscow",75.5,28,0,new Bitset<DiseaseMod>(DiseaseMod.Cold,DiseaseMod.Crowd,DiseaseMod.Wet)));
    	standardInitialPlacement.push(bd("Moscow#2",78.3,28,1,new Bitset<DiseaseMod>(DiseaseMod.Cold,DiseaseMod.Crowd,DiseaseMod.Wet)));
    	bd("Kaffa",82.2,53.4,0,null);
    	bd("Istanbul",73.3,64.3,0,null);
    	bd("Istanbul#2",76,64.3,1,null);
    	bd("Istanbul#3",73.3,68,0,null);
    	bd("Istanbul#4",76,68,0,null);
    	bd("Athens",67,75.8,0,null);
    	standardInitialPlacement.push(bd("Damascus",90.8,78.6,0,new Bitset<DiseaseMod>(DiseaseMod.Warm)));
    	standardInitialPlacement.push(bd("Damascus#2",93.5,78.6,1,new Bitset<DiseaseMod>(DiseaseMod.Warm)));
    	standardInitialPlacement.push(bd("Jerusalem",90.5,84,0,null));
    	bd("Alexandria",80.6,86.5,1,null);
    	bd("Alexandria#2",80.6,90.1,0,null);
    	standardInitialPlacement.push(bd("Cairo",84.5,90.1,0,new Bitset<DiseaseMod>(DiseaseMod.Crowd,DiseaseMod.Warm)));
    	standardInitialPlacement.push(bd("Cairo#2",87.5,90.1,1,new Bitset<DiseaseMod>(DiseaseMod.Crowd,DiseaseMod.Warm)));
    	standardInitialPlacement.push(bd("Cairo#3",84.5,93.9,0,new Bitset<DiseaseMod>(DiseaseMod.Crowd,DiseaseMod.Warm)));
    	standardInitialPlacement.push(bd("Cairo#4",87.5,93.9,0,new Bitset<DiseaseMod>(DiseaseMod.Crowd,DiseaseMod.Warm)));  
    	bd("Greenland",2,1,-100,null);
    	ln("Greenland","Reykjavik",-1);
    	externalPlacement.push(bd("Sahara",43.4,99,-100,null));
    	ln("Tunis","Sahara",-1);
    	externalPlacement.push(bd("Ethiopia",87,99,-100,null));
    	ln("Cairo","Ethiopia",-1);
    	externalPlacement.push(bd("Jordan",99,89,-100,null));
    	ln("Jerusalem","Jordan",-1);
    	externalPlacement.push(bd("Baghdad",99,77.4,-100,null));
    	ln("Damascus","Baghdad",-1);
    	externalPlacement.push(bd("Siberia",99,17.5,-100,null));
    	externalPlacement.push(bd("mecca",99,19.5,-100,null));
    	ln("Cairo","mecca",-1);
    	ln("Moscow","Siberia",-1);
    	ln("Reykjavik","Dublin",-2);
    	ln("Dublin","Edinburgh",0);
    	ln("Edinburgh","Oslo",-1);
    	ln("Oslo","Stockholm",-1);
    	ln("Oslo","Amsterdam",0);
    	ln("Edinburgh","London",-2);
    	ln("London","Paris",0);
    	ln("London","Bilbao",0);
    	ln("London","Amsterdam",0);
    	ln("Bilbao","Paris",-1);
    	ln("Paris","Brussels",0);
    	ln("Brussels","Amsterdam",0);
    	ln("Amsterdam","Lubeck",0);
    	ln("Lubeck","Copenhagen",0);
    	ln("Copenhagen","Oslo",0);
    	ln("Lubeck","Munich",0);
    	ln("Stockholm","Copenhagen",0);
    	ln("Lubeck","Prague",0);
    	ln("Prague","Gdansk",0);
    	ln("Prague","Vienna",0);
    	ln("Lubeck","Gdansk",-1);
    	ln("Bilbao","Madrid",0);
    	ln("Bilbao","Lisbon",0);
    	ln("Lisbon","Cadiz",0);
    	ln("Cadiz","Madrid",0);
    	ln("Madrid","Barcelona",0);
    	ln("Cadiz","Barcelona",0);
    	ln("Bilbao","Barcelona",-1);
    	ln("Paris","Lyon",0);
    	ln("Lyon","Geneva",0);
    	ln("Lyon","Marseille",0);
    	ln("Marseille","Algiers",0);
    	ln("Cadiz","Algiers",0);
    	ln("Barcelona","Algiers",0);
    	ln("Algiers","Tunis",0);
    	ln("Cadiz","Tunis",-1);
    	ln("Algiers","Genoa",0);
    	ln("Genoa","Tunis",-1);
    	ln("Tunis","Naples",0);
    	ln("Rome","Naples",0);
    	ln("Rome","Venice",0);
    	ln("Genoa","Venice",0);
    	ln("Genoa","Rome",0);
    	ln("Genoa","Naples",0);
    	ln("Geneva","Genoa",0);
    	ln("Munich","Venice",0);
    	ln("Venice","Vienna",-1);
    	ln("Munich","Vienna",0);
    	ln("Venice","Naples",0);
    	ln("Vienna","Budapest",0);
    	ln("Budapest","Belgrade",0);
    	ln("Belgrade","Bucharest",0);
    	ln("Venice","Athens",0);
    	ln("Tunis","Athens",0);
    	ln("Athens","Alexandria",0);
    	ln("Tunis","Alexandria",-1);
    	ln("Alexandria","Cairo",0);
    	ln("Cairo","Jerusalem",0);
    	ln("Jerusalem","Damascus",0);
    	ln("Damascus","Istanbul",0);
    	ln("Istanbul","Kaffa",0);
    	ln("Kaffa","Moscow",-1);
    	ln("Moscow","Gdansk",-1);
    	ln("Gdansk","Novgorod",0);
    	ln("Novgorod","Moscow",0);
    	ln("Novgorod","Helsinki",0);
    	ln("Helsinki","Stockholm",0);
    	ln("Gdansk","Warsaw",0);
    	ln("Warsaw","Budapest",-1);
    	ln("Warsaw","Bucharest",0);
    	ln("Bucharest","Kaffa",0);
    	ln("Istanbul","Jerusalem",0);
    	ln("Istanbul","Alexandria",0);
    	ln("Istanbul","Athens",0);
    	ln("Athens","Naples",0);
    	ln("Dublin","London",0);
    	setDistances();	// calculate distances for mongols and crusade cards

    	extendedInitialPlacement.copyFrom(standardInitialPlacement);
    	extendedInitialPlacement.push(getCity("Alexandria"));
    	extendedInitialPlacement.push(getCity("Alexandria#2"));
    	extendedInitialPlacement.push(getCity("Kaffa"));
    };
    
    public void doInit(String gtype,long key)
    {
    	StringTokenizer tok = new StringTokenizer(gtype);
    	String typ = tok.nextToken();
    	int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
    	long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
    	int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
    	doInit(typ,ran,np,rev);
    }
	void logGameEvent(String str,String... args)
	{	//if(!robotBoard)
		{String trans = s.get(str,args);
		 gameEvents.push(trans);
		}
	}
	void logTranslatedGameEvent(String str)
	{
		gameEvents.push(str);
	}
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key,int players,int rev)
    {	randomKey = key;
    	win = new boolean[players];
    	initialPlacement = players>=5 ? extendedInitialPlacement : standardInitialPlacement;
    	adjustRevision(rev);
    	players_in_game = players;
    	pbs = new PlayerBoard[players];
    	Random r = new Random(key);
    	lastRoll = null;
    	crusadeInEffect = false;
    	mongolsInEffect = false;
    	gameEvents.clear();
    	int map[] = getColorMap();
    	for(int i=0;i<players;i++)
    		{ int modidx = r.nextInt(DiseaseMod.values().length);
    		  pbs[i] = new PlayerBoard(i,BlackDeathColor.values()[map[i]],this,
    				BlackDeathChip.PlayerChips[map[i]],
    				BlackDeathChip.ModChips[modidx]
    				); 
    		}
    	
		setState(BlackDeathState.Puzzle);
		variation = BlackDeathVariation.findVariation(gtype);
		G.Assert(variation!=null,WrongInitError,gtype);
		gametype = gtype;
		resetState = BlackDeathState.Puzzle;
		initialTotalPips = PlayerBoard.INITIAL_TOTAL_PIPS;
		switch(variation)
		{
		default: throw G.Error("Not expecting variation %s",variation);
		case blackdeath:
			nDice = 2;
			break;
		case blackdeath_low:	// low luck variant
			nDice = 3;
			break;
		}
		
		drawPile.reInit();
		for(BlackDeathChip ch : BlackDeathChip.Cards) { drawPile.addChip(ch); }
		drawPile.shuffle(r);
		discardPile.reInit();
		turnOrder = new int[players_in_game];
	    whoseTurn = r.nextInt(players_in_game);	// random first player
	    reorderPlayersFrom(whoseTurn);
	    setMovementPoints(0);
	    infectionPoints = 0;
	    closeLinkPoints = 0;
	    killPoints = 0;
	    killVictim = -1;
	    perfectlyRolling = false;
	    acceptPlacement();
	    resetState = null;
	    lastDroppedObject = null;
	    // set the initial contents of the board to all empty cells
		for(BlackDeathCell c = allCells; c!=null; c=c.next) { c.reInit();  }
		
   
        animationStack.clear();
        moveNumber = 1;

        // note that firstPlayer is NOT initialized here
    }
    public Random currentRandomSelector()
    {	BlackDeathCell des = getDest();
    	// should be specific to this game and the target in the current context
    	long k = (randomKey+moveNumber*(123+infectionPoints));
    	if(des!=null) { k += des.name.toLowerCase().hashCode(); }
    	return(new Random(k));
    }
     // reorder players around the table, starting with someone
    public void reorderPlayersFrom(int who)
    {
	    for(int i=0;i<players_in_game;i++)
	    {
	    	turnOrder[i] = (who+i)%players_in_game;
	    }
    }
    public void reorderPlayersByBodycount()
    {	PlayerBoard nextPlayers[] = new PlayerBoard[players_in_game];
    	AR.copy(nextPlayers,pbs);
    	Sort.sort(nextPlayers);
    	
    }

    /** create a copy of this board */
    public BlackDeathBoard cloneBoard() 
	{ BlackDeathBoard dup = new BlackDeathBoard(gametype,players_in_game,randomKey,getColorMap(),revision); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((BlackDeathBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manipulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(BlackDeathBoard from_b)
    {
        super.copyFrom(from_b);
        for(int i=0;i<pbs.length;i++) { from_b.pbs[i].copyTo(pbs[i]); }
        
        unresign = from_b.unresign;
        board_state = from_b.board_state;
        getCell(droppedDestStack,from_b.droppedDestStack);
        destMoveStack.copyFrom(from_b.destMoveStack);
        getCell(pickedSourceStack,from_b.pickedSourceStack);
        stateStack.copyFrom(from_b.stateStack);
        dropCost = from_b.dropCost;
        pickedObject = from_b.pickedObject;
        pickedIndex = from_b.pickedIndex;
        resetState = from_b.resetState;
        setMovementPoints(from_b.getMovementPoints());
        infectionPoints = from_b.infectionPoints;
        closeLinkPoints = from_b.closeLinkPoints;
        killPoints = from_b.killPoints;
        killVictim = from_b.killVictim;
        perfectlyRolling = !perfectlyRolling;
        lastRoll = from_b.lastRoll;
        lastPicked = null;
        perfectlyRolling = from_b.perfectlyRolling;
        mongolsInEffect = from_b.mongolsInEffect;
        crusadeInEffect = from_b.crusadeInEffect;
        
        AR.copy(turnOrder,from_b.turnOrder);
		drawPile.copyFrom(from_b.drawPile);
		discardPile.copyFrom(from_b.discardPile);

  
        sameboard(from_b); 
    }

    

    public void sameboard(BoardProtocol f) { sameboard((BlackDeathBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(BlackDeathBoard from_b)
    {
        super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
        G.Assert(board_state==from_b.board_state,"board_state mismatch");
        G.Assert(unresign==from_b.unresign,"unresign mismatch");
        G.Assert(variation==from_b.variation,"variation matches");
        G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
        G.Assert(pickedIndex==from_b.pickedIndex, "picked index mismatch");
        G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedsourceStack mismatch");
        G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDestStack mismatch");
        G.Assert(AR.sameArrayContents(turnOrder, from_b.turnOrder),"turn order mismath");
        G.Assert(movementPoints==from_b.movementPoints,"movement points mismatch");
        G.Assert(infectionPoints==from_b.infectionPoints,"infection points mismatch");
        G.Assert(closeLinkPoints==from_b.closeLinkPoints,"closeLink points mismatch");
        G.Assert(killPoints==from_b.killPoints,"kill points mismatch");
        G.Assert(crusadeInEffect==from_b.crusadeInEffect,"crusade mismatch");
        G.Assert(mongolsInEffect==from_b.mongolsInEffect,"Mongols mismatch");
        G.Assert(killVictim==from_b.killVictim,"kill victim mismatch");
        G.Assert(perfectlyRolling==from_b.perfectlyRolling,"perfectlyRolling mismatch");
        G.Assert(lastRoll==from_b.lastRoll,"lastRoll mismatch");
        for(int i=0;i<pbs.length;i++) { pbs[i].sameBoard(from_b.pbs[i]); }
        G.Assert(dropCost==from_b.dropCost,"dropCost mismatch");
        G.Assert(drawPile.sameCell(from_b.drawPile),"same draws");
        G.Assert(discardPile.sameCell(from_b.discardPile), "different discards");
        // this is a good overall check that all the copy/check/digest methods
        // are in sync, although if this does fail you'll no doubt be at a loss
        // to explain why.
        long v1 = Digest();
        long v2 = from_b.Digest();
        G.Assert(v1==v2,"Digest matches");

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
		// many games will want to digest pickedSource too
		// v ^= cell.Digest(r,pickedSource);
		v ^= chip.Digest(r,pickedObject);
		v ^= Digest(r,pickedIndex);
		v ^= Digest(r,pickedSourceStack);
		v ^= Digest(r,droppedDestStack);
		v ^= Digest(r,revision);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		for(int i=0;i<pbs.length;i++) { v ^= pbs[i].Digest(r); }
		v ^= Digest(r,turnOrder);
		v ^= Digest(r,movementPoints);
		v ^= Digest(r,infectionPoints);
		v ^= Digest(r,closeLinkPoints);
		v ^= Digest(r,killPoints);
		v ^= Digest(r,killVictim);
		v ^= Digest(r,perfectlyRolling);
		v ^= Digest(r,lastRoll);
		v ^= Digest(r,mongolsInEffect);
		v ^= Digest(r,crusadeInEffect);
		v ^= Digest(r,dropCost);
		v ^= Digest(r,drawPile);
		v ^= Digest(r,discardPile);
		return (v);
    }
    public void setWhoseTurn(int n)
    {
    	setWhoseTurn(n,replayMode.Replay);
    }
    public void setWhoseTurn(int n,replayMode replay)
    {
    	super.setWhoseTurn(n);
    	PlayerBoard b = getPlayer(n);
    	for(int lim=b.temporaryCards.height()-1; lim>=0; lim--)
    		{
    	    	if(replay!=replayMode.Replay)
    	    	{
    			animationStack.push(b.temporaryCards);
    			animationStack.push(discardPile);
    	    	}
    	    	discardPile.addChip(b.temporaryCards.removeTop());
    	}
     	b.temporaryCards.reInit();		// get rid of temporary cards
    	b.closedRegions.clear();		// maybe open some closed regions 
    	b.closedLinks.clear();          // maybe open some closed links
    	b.pogromRegions.clear();        // maybe remove some pogrom markers
    	b.resetVirulenceAndMortality();	// undo effect of card-altered virulence and mortality 
    }
    public boolean regionIsClosed(BlackDeathCell c,BlackDeathState forState)
    {	if(forState!=BlackDeathState.CloseLinks)
    	{
    	for(PlayerBoard pb : pbs)
    	{
    		if(pb.closedRegions.contains(c)) { return(true); }
    	}}
    	return(false);
    }
    public boolean regionIsPogrom(BlackDeathCell c,BlackDeathState forState)
    {	
    	{
    	for(PlayerBoard pb : pbs)
    	{
    		if(pb.pogromRegions.contains(c)) { return(true); }
    	}}
    	return(false);
    }
    public boolean linkIsClosed(BlackDeathLink l)
    {
       	for(PlayerBoard pb : pbs)
    	{
    		if(pb.closedLinks.contains(l)) 
    			{ return(true); 
    			}
    	}
    	return(false);
    	
    }
    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player in state %s",board_state);
        case Puzzle:
            break;
        case SetVirulence:
        	setWhoseTurn(nextPlayer(),replay);
        	break;

        case Kill:
        case AnyCatastrophicKill:
        case CatastrophicKill:
        	if(killPoints>0) { p1("premature end of massacre"); }
        	G.Assert(killPoints==0,"premature end of the massacre");
			//$FALL-THROUGH$
        case Confirm:
        case Resign:
        case Mortality:
            moveNumber++; //the move is complete in these states
            int next = nextPlayer();
             if(next==turnOrder[0])
             	{
            	// starting a new year
            	reorderPlayersByBodycount();
            	next = turnOrder[0];
             	}
             setWhoseTurn(next,replay);
            return;
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	
    	switch(board_state)
    	{
    	case Infection:
    	case FirstInfect:
    	case TradersPlus1:
    	case TradersPlus2:
    		// in rare circumstances, we can run out of places to even try to infect
    		// most particularly in Traders phases where only a few spaces are eligable
    		return(!hasInfectionMoves(board_state));
 
    	case Kill:
    	case CatastrophicKill:
    	case AnyCatastrophicKill:
    	case Cure:
    		return(killPoints==0 || escapeState);
    	default:
    		return(board_state.doneState());
    	}
    }
    public boolean rollState()
    {
    	switch(board_state)
    	{
    	case Roll:
    	case Mortality:
    	case Roll2: return(true);
    	default: return(false);
    	}
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
    
    static int winningBodyCount[] = { 10,20,20,15,15,10 };	// index by number of players
    
    public boolean winForPlayerNow(int player)
    {  	PlayerBoard pb = getPlayer(player);
    	if(pb.bodyCount>=winningBodyCount[players_in_game-1]) 
    		{
    		  win[player]=true; return(true); 
    		}
    	return(false);
    }
    public int scoreForPlayer(int p)
    {
    	return(getPlayer(p).bodyCount);
    }

    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDestStack.clear();
        destMoveStack.clear();
        pickedSourceStack.clear();
        stateStack.clear();
        dropCost = 0;
        pickedObject = null;
        pickedIndex = -1;
        perfectlyRolling = false;
     }
    public BlackDeathCell getSource()
    {
    	return(pickedSourceStack.top());
    }
    public BlackDeathCell getDest()
    {
    	return(droppedDestStack.top());
    }
    public BlackDeathMovespec getDestMove(BlackDeathCell c)
    {	int ind = droppedDestStack.indexOf(c);
    	if(ind>=0) 
    		{return((BlackDeathMovespec)destMoveStack.elementAt(ind));
    		}
    	return(null);
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private BlackDeathCell unDropObject()
    {	BlackDeathCell rv = droppedDestStack.pop();
    	destMoveStack.pop();
    setMovementPoints(getMovementPoints() - dropCost);
    	dropCost = 0;
    	setState(stateStack.pop());
    	pickedObject = rv.removeTop(); 	// SetBoard does ancillary bookkeeping
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	BlackDeathCell rv = pickedSourceStack.pop();
    	setState(stateStack.pop());
    	switch(board_state)
    	{
    	case FirstInfect:
    	case Infection:
    		// just discard
    		break;
    	case Puzzle:
    	case Roll2:
    	case Movement:
    	case WesternMovement:
    	case EasternMovement:
    	case TradersPlus1:
    	case TradersPlus2:
    	case FirstMovement:
    		rv.insertChipAtIndex(pickedIndex, pickedObject);
        	break;
        default:
        	G.Error("Not expecting state %s",board_state);
        	break;
    	}
    	pickedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(BlackDeathCell c,replayMode replay,int movementCost,BlackDeathMovespec m)
    {
       droppedDestStack.push(c);
       destMoveStack.push(m);
       stateStack.push(board_state);
       dropCost = movementCost;
       setMovementPoints(getMovementPoints() + movementCost);

       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case Cards:
        case TemporaryCards:
        case DrawPile:
        case DiscardPile:
        	c.addChip(pickedObject);
        	lastDroppedObject = pickedObject;
            pickedObject = null;
        	break;
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        case EmptyBoard:
           	c.addChip(pickedObject);
           	c.whenPlaced = moveNumber;
            lastDroppedObject = pickedObject;
            pickedObject = null;
            break;
        }
     }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(BlackDeathCell c)
    {	return(droppedDestStack.top()==c);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { BlackDeathChip ch = pickedObject;
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
    public BlackDeathCell getCell(BlackDeathId source, String name)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case BoardLocation:
        	return(getCity(name));
        } 	
    }
    private BlackDeathCell getCell(BlackDeathId source, BlackDeathColor who,int row)
    {	PlayerBoard pb = getPlayer(who);
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source " + source);
        case PlayerVirulence:
        	return(pb.virulenceCells[row]);
        case PlayerMortality:
        	return(pb.mortalityCells[row]);
        case Cards:
        	return(pb.cards);
        case TemporaryCards:
        	return(pb.temporaryCards);
        } 	
    } 
    public BlackDeathCell getCell(BlackDeathCell c)
    {	if(c==null) { return(null); }
    	switch(c.rackLocation())
    	{	default: throw G.Error("Not expecting source ", c);
			case Cards:
			case TemporaryCards:
    		case PlayerMortality:
    		case PlayerVirulence:
    			return(getCell(c.rackLocation(),c.color,c.row));
    		case PlayerChips:
    			{
    			PlayerBoard pb = getPlayer(c.color);
    			return(pb.chipCell);
    			}
    		case DiscardPile:
    			return(discardPile);
    		case DrawPile:
    			return(drawPile);
    		case BoardLocation:
    			return(getCity(c.name));
    	}
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(BlackDeathCell c,int index)
    {	pickedSourceStack.push(c);
    	stateStack.push(board_state);
    	pickedIndex = index<0 ? c.height()-1 : index;
        switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting rackLocation " + c.rackLocation);
        case PlayerChips:
        	pickedObject = c.topChip();
        	pickedSourceStack.push(c);
        	break;
        	
        case Cards:
        case TemporaryCards:
        case DiscardPile:
        case DrawPile:
        	pickedIndex = index;
        	lastPicked = pickedObject = c.removeChipAtIndex(index);
        	break;
        case BoardLocation:
        	{
            lastPicked = pickedObject = c.topChip();
         	lastDroppedObject = null;
         	switch(board_state)
         	{
         	case Puzzle:
         	case Movement:
         	case WesternMovement:
         	case EasternMovement:
         	case FirstMovement:
         		c.removeTop();
         		break;
         	default: 
         		break;
        	}}
            break;

        }
    }
    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(BlackDeathCell c)
    {	return(c==pickedSourceStack.top());
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state " + board_state);
        case FirstMovement:
        case WesternMovement:
        case EasternMovement:
        case Movement:
        	setState(BlackDeathState.Confirm);
        	break;
        case Confirm:
        	setNextStateAfterDone(replay);
         	break;
        case SelectInfection:
			setState(BlackDeathState.Confirm);
			break;
        case Puzzle:
			acceptPlacement();
            break;
        }
    }
    private BlackDeathState performCard(BlackDeathChip card)
    {
    	switch(card.cardEffect)
    	{
    	case Crusade:		// move toward jerusalem, handled in state transitions
    	case Smugglers:
    	case SlowTravel:	// -1 on movement rolls
    	case Famine:		// enhanced mortality, reduced movement until resended
    	case Mongols:		// enhanced movement, away from the east
       	case HighVirulence:	// bad weather, handled in the rolls
       	case LowVirulence:	// good weather, handled in the rolls
       		break;
       	case Pogrom:
       		return(BlackDeathState.Pogrom);
    	case Quaranteen:
    		return(BlackDeathState.CloseRegion);    
    		
    	case TradersPlus2:
    		if(hasInfectionMoves(BlackDeathState.TradersPlus2))
    		{
    		infectionPoints = 2;
    		return(BlackDeathState.TradersPlus2);
    		}
    		break;
       	case TradersPlus1:	// extra infection attempt
       		if(hasInfectionMoves(BlackDeathState.TradersPlus1))
       		{
       		infectionPoints = 1;
       		return(BlackDeathState.TradersPlus1);
       		}
       		break;
       	case Fire:	
    		killPoints = 1;
    		return(BlackDeathState.AnyCatastrophicKill);
    	case MutationVirulenceAndMortality:
    		return(BlackDeathState.MutationAnd);
    	case MutationSwap:
    		return(BlackDeathState.MutationSwap);   		
    	case MutationVirulenceOrMortality:
    		return(BlackDeathState.MutationOr);
    	case War:
    		closeLinkPoints = 2;
    		return(BlackDeathState.CloseLinks);
 
    	default: G.Error("Not expecting effect %s",card.cardEffect);
    	}
    	return(null);
    }
    private void setNextStateAfterDone(replayMode replay)
    {	BlackDeathState nextState = resetState;
       	switch(board_state)
    	{
    	default:
    		p1("unexpected done in state "+board_state);
    		throw G.Error("Not expecting after Done state "+board_state);
    	case Gameover: 
    		break;
     	case Cure:
    		// we're already the next player
    		if(allInitialInfectionsDone()) { setState(BlackDeathState.Roll2); }
    		else { setState(BlackDeathState.FirstInfect);	// initial infection after miraculous cure
    			   if(!hasInfectionMoves(BlackDeathState.FirstInfect)) 
    			   	{ infectionPoints = 0; 
    			   	  setState(BlackDeathState.Movement); 
    			   	}
    		}
    		break;
    	case Roll2:
    		if(initialDice1.topChip()==initialDice2.topChip())
    			{ setState(BlackDeathState.Infection); 
    			}
    			else 
    			{ setState(BlackDeathState.SelectInfection);
    			}
    		break;
    	case Movement:
    	case FirstMovement:
    	case EasternMovement:
    	case WesternMovement:
    		setMovementPoints(0);
			//$FALL-THROUGH$
		case Roll:
    	case Infection:
    	case Confirm:
    	case ConfirmCard:
    		if(board_state==BlackDeathState.ConfirmCard)
    		{	PlayerBoard pb = pbs[whoseTurn];
    			BlackDeathState next = performCard(pb.temporaryCards.topChip());
    			if(next!=null) { setState(next); }
    			else { setState(resetState); }
    			break;
    		}
			//$FALL-THROUGH$
		case Kill:
    	case AnyCatastrophicKill:
    	case CatastrophicKill:
    		
    	case Puzzle:
    	case TradersPlus1:
    	case TradersPlus2:
    	case FirstInfect:
    	case SelectInfection:
    		switch(resetState)
    		{
    		default: throw G.Error("Not expecting resetState %s",resetState);
    		
    	   	case Kill:
        	case CatastrophicKill:
        		setNextPlayer(replay);
				//$FALL-THROUGH$
    	   	case Cure:				// 	cure is done by the next player
    	   	case CloseRegion:
    	   	case CloseLinks:
    	   	case Pogrom:
    	   	case MutationSwap:
    	   	case MutationOr:
    	   	case MutationAnd:
    	   	case TradersPlus1:
    	   	case TradersPlus2:
			case AnyCatastrophicKill:	// skip the "next player"
        		if(allInitialInfectionsDone())
        			{ setState(BlackDeathState.Roll2); 
        			}
        		else { setState(BlackDeathState.FirstInfect); 
        			   if(!hasInfectionMoves(BlackDeathState.FirstInfect))
        			   { infectionPoints = 0;
        			     setState(BlackDeathState.Movement); 
        			   }
        		}
        		break;

    		case Puzzle:
    		case SetVirulence:
    			{
    			PlayerBoard pb = pbs[whoseTurn];
    			if(pb.cards.topChip()==null) 
    			{ pb.cards.addChip(drawPile.removeTop()); 
    			  if(replay!=replayMode.Replay)
    			  {
    				  animationStack.push(drawPile);
    				  animationStack.push(pb.cards);
    			  }
    			}
     			setNextPlayer(replay);
    			if(allVirulenceSet()) { setState(BlackDeathState.InitialPlacement); }
    			else { setState(resetState);}
    			}
    			break;
   
			case InitialPlacement:
				setNextPlayer(replay);
				if(allPlacementsMade())
				{
					setState(BlackDeathState.FirstInfect);
				}
				else
				{
				setState(resetState);
				}
				break;
			case Roll2:
			case SelectInfection:
				setState(BlackDeathState.Infection);
				break;
				
			case FirstInfect:
				setState(BlackDeathState.FirstMovement);
				break;
			case Infection:
				if(mongolsInEffect && hasMovements(BlackDeathState.WesternMovement)) 
					{ setState(BlackDeathState.WesternMovement); 
					}
				else if(crusadeInEffect && hasMovements(BlackDeathState.EasternMovement))
					{
					 setState(BlackDeathState.EasternMovement);
					}
				else { setState(BlackDeathState.Movement); }
				break;
			case EasternMovement:
				// if both mongols and crusades, mongols first so check for crusade
				mongolsInEffect = false;
				if(crusadeInEffect && hasMovements(BlackDeathState.WesternMovement))
				{
				 setState(BlackDeathState.WesternMovement);
				 break;
				}
				//$FALL-THROUGH$
			case WesternMovement:
				if(board_state==BlackDeathState.WesternMovement) { crusadeInEffect = false; }
				else { nextState = BlackDeathState.Movement; }
				//$FALL-THROUGH$
			case FirstMovement:
			case Movement:
				if((movementPoints==0) || !hasMovements(board_state)) 
					{	movementPoints = 0;
						setState(BlackDeathState.Mortality); 
					}
				else { setState(nextState); }
				break;
			case Mortality:
				{
				setNextPlayer(replay);
				PlayerBoard pb = pbs[whoseTurn];
				setState(pb.virulenceSet?BlackDeathState.SelectInfection:BlackDeathState.SetVirulence);
				}
    		}
    		resetState = board_state;
    		break;
    	}
       	switch(board_state)
       	{
       	case Infection:
       		if(!hasInfectionMoves(board_state)) 
       			{ infectionPoints = 0;
       			  setState(BlackDeathState.Movement); 
       			}
       		break;
       	case FirstInfect:
       		{
       		PlayerBoard pb = pbs[whoseTurn];
   			infectionPoints = pb.getMortality();
			movementPoints = pb.getVirulence();
			initialDice1.reInit();
			initialDice1.addChip(BlackDeathChip.getDie(infectionPoints));
			initialDice1.label = InfectionAttempts;
			initialDice2.reInit();
			initialDice2.addChip(BlackDeathChip.getDie(movementPoints));
			initialDice2.label = Movements;
			killPoints = 0;
			killVictim = -1;
			perfectlyRolling = false;
       		}
       		break;
       	default: break;

       	}
       	resetState = board_state;
    }
    

    private void doDone(replayMode replay)
    {	BlackDeathCell dest = getDest();
        acceptPlacement();
        PlayerBoard pb = pbs[whoseTurn];
        switch(resetState)
        {
        case SetVirulence:
        	pb.virulenceSet = true;
        	break;
        case InitialPlacement:
        	pb.initialPlacementDone = true;
        	break;
        case FirstInfect:
        	pb.initialInfectionDone = true;
        	break;
        case Movement:
           	if(regionIsPogrom(dest,resetState))
        	{
        		doKill(MOVE_KILL,dest,replay);
        	}
			break;

        default: break;
        }
        switch(board_state)
        {
        case Resign:
        	win[nextPlayer()] = true;
    		setState(BlackDeathState.Gameover);
    		break;
        case Movement:
        case TradersPlus1:
        case TradersPlus2:
        case FirstMovement:
 		case ConfirmCard:
        case FirstInfect:
        case Kill:
        case Infection:
        case CatastrophicKill:
        case AnyCatastrophicKill:
        case Roll:
        case Roll2:
        case Cure:
        case Confirm:
        	if(winForPlayerNow(whoseTurn)) 
    		{ win[whoseTurn]=true;
    		  setState(BlackDeathState.Gameover); 
    		}
        	else {
        		setNextStateAfterDone(replay);
        	}
        	break;
        default: 
        	p1("Not expecting done in state "+board_state);
        	G.Error("Not expecting done in state %s",board_state);
        	
        }
    }

	public BlackDeathCell getCity(String name)
	{
		BlackDeathCell c = cities.get(name);
		G.Assert(c!=null,"city not found ",name);
		return(c);
	}
	public BlackDeathLink getLink(BlackDeathCell fr,BlackDeathCell toc)
	{	
		BlackDeathCell from = fr.parentCity;
		BlackDeathCell to = toc.parentCity;
		if(from==null) { return(BlackDeathLink.zeroCostLink); }	// from is a player chip pool
		LinkStack links = from.links;
		if(from==to ) { return(BlackDeathLink.zeroCostLink); }	// link to self
		for(int lim=links.size()-1; lim>=0; lim--)
		{
			BlackDeathLink l = links.elementAt(lim);
			if((l.from==from && l.to==to)||(l.to==from&&l.from==to))
			{
				return(l);
			}
		}
		throw G.Error("link not found %1 %2",from,to);
	}
	private void checkChips()
	{
		for(PlayerBoard pb : pbs) { pb.chipsOnBoard = 0; }
		if((pickedObject!=null) && pickedObject!=null && !pickedObject.isDie() && pickedObject!=BlackDeathChip.SkullIcon)
		{
		if(pickedObject.color!=null) { getPlayer(pickedObject.color).chipsOnBoard++; }
		}
		for(BlackDeathCell c = allCells;c!=null;  c=c.next)
		{	for(int lim=c.height()-1; lim>=0; lim--)
			{BlackDeathChip top = c.chipAtIndex(lim);
			if(top!=null && !top.isDie() && top!=BlackDeathChip.SkullIcon) { getPlayer(top.color).chipsOnBoard++; }
			}
		}
		for(PlayerBoard pb : pbs) 
			{ G.Assert((pb.chipCount+pb.chipsOnBoard)==PlayerBoard.INITIAL_CHIP_COUNT,
				"Chip count wrong for %s\ncount %s onBoard %s",
				pb,pb.chipCount,pb.chipsOnBoard);
			
		}
	}
	private boolean canBeRemoved(BlackDeathCell city)
	{	if(city.topChip()==BlackDeathChip.SkullIcon) { return(false); }	// already removed
   		int dead = 0;
   		int avail = 0;
   		for(BlackDeathCell p = city.parentCity ; p!=null; p=p.sisterCity)
   			{
   			avail++;
   			if(p.topChip()==BlackDeathChip.SkullIcon)
   				{
   				dead++;
   				}
   			}
   		return((dead+1)<=(avail/2));
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	BlackDeathMovespec m = (BlackDeathMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); }
        checkChips();
        //G.print("E "+moveNumber+" "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_SELECT:	// select die to represent infection
        	{
        	BlackDeathCell c = getCity(m.to_name);
        	if(c==initialDice1)
        		{
        			infectionPoints = initialDice1.topChip().dieValue();
        			setMovementPoints(adjustMovementPoints(initialDice2.topChip().dieValue()));
        			initialDice1.label = InfectionAttempts;
        			initialDice2.label = Movements;
        			m.selection = initialDice1.topChip();
        		}
        	else if(c==initialDice2)
        		{
       			infectionPoints = initialDice2.topChip().dieValue();
    			setMovementPoints(adjustMovementPoints(initialDice1.topChip().dieValue()));
    			initialDice2.label = InfectionAttempts;
    			initialDice1.label = Movements;
    			m.selection = initialDice2.topChip();
           		}
        	else { G.Error("Not expecting select %s", c);
        		}
        	}
        	setState(BlackDeathState.Confirm);
        	break;
        case MOVE_ROLL2:
        	{
        	int v1 = m.to_row/10;
        	int v2 = m.to_row%10;
        	initialDice1.reInit();
        	initialDice2.reInit();
        	initialDice1.addChip(BlackDeathChip.getDie(v1));
        	initialDice2.addChip(BlackDeathChip.getDie(v2));
        	setState(BlackDeathState.SelectInfection);
        	}
        	break;
        case MOVE_USE_PERFECTROLL:      	
        	perfectlyRolling = !perfectlyRolling;
        	break;
        case MOVE_PERFECTROLL:
        case MOVE_ROLL:
        	{
       		PlayerBoard pb = pbs[whoseTurn];
    		int v = m.to_row;
    		switch(board_state)
        	{
        	default: 
        		throw G.Error("Not expecting roll in state %s",board_state);
        		
        	case Mortality:
        	{	mortalityTable.reInit();
        		mortalityTable.addChip(BlackDeathChip.getDie(v));
        		boolean skull = false;
        		boolean cure = false;
        		int fraction = 0;
        		int mort = getAdjustedMortality(pb);
        		switch(mort)
        		{
        		case 0:
        		case 1:	// mortality 0,1
        			switch(v)
        			{
          			default: throw G.Error("roll %d",v);
          			case 4:
          			case 5:
          				fraction = 0;
          				break;
        			case 1:
        			case 2:
        			case 3:	fraction = 1;
        				break;
        			case 6:
        				fraction = 1;
        				cure = true;
         				break;
        			}
        			break;
        		case 2:	// mortality 2
        			switch(v)
        			{
        			default: throw G.Error("roll %s",v);
        			case 1:	skull = true;
        					fraction = 2;
        					break;
        			case 2:
        			case 3:
        			case 4:	fraction = 1;
        					break;
        			case 5:	
        				fraction = 0;
        				break;
        			case 6:	fraction = 1;
        					cure = true;
        					break;
        			}
        			break;
        		case 3:
        			switch(v)
        			{
        			default: throw G.Error("roll %s", v);
        			case 1:	skull = true;
        					fraction = 2;
        					break;
        			case 2: fraction = 2;
        					break;
        			case 3:
        			case 4:
        			case 5:
        				fraction = 1;
        				break;
        			case 6:
        				fraction = 1;
        				cure = true;      			
        			}
        			break;
        		case 4:	// mortality 4
        			switch(v)
        			{
        			default: throw G.Error("roll %s", v);
        			case 1:	skull = true;
        					fraction = 3;
        					break;
        			case 2:
        			case 3:
        				fraction = 2;
        				break;
        			case 4:
        			case 5:
        			case 6:
        				fraction = 1;
        				break;        				
        			}
        			break;
        		default: // 5, 6
        			switch(v)
        			{
        			default: throw G.Error("roll %s", v);
        			case 1:
        				skull = true;
        				fraction = 3;
        				break;
        			case 2:
        				skull = true;
						//$FALL-THROUGH$
					case 3:
        			case 4:
        				fraction = 2;
        				break;
        			case 5:
        			case 6:
        				fraction = 1;
        				break;       				
        			}
        			break;
        		}
    			killVictim = whoseTurn;
    			if(cure) 
        			{
        			setNextPlayer(replay);
        			setState(resetState = BlackDeathState.Cure); 
        			}
        		else if(skull) { setState(resetState = BlackDeathState.CatastrophicKill); }
        		else { setState(resetState = BlackDeathState.Kill); }
        		int chipsOnBoard = (PlayerBoard.INITIAL_CHIP_COUNT-pb.chipCount);
           		killPoints = chipsOnBoard*fraction/6;
    			String msg = "Mortality is "+mort+", "+fraction+"/6 of "+chipsOnBoard+" = "+killPoints
    					+ (skull
    						? " catastrophic "
    						: cure ? " cure " : "");
    			logTranslatedGameEvent(msg);
    			m.selection = BlackDeathChip.SkullIcon;
        		}	// end of mortality case
        		break;
        	case Roll:
         	{	BlackDeathCell dest = getDest();
        		BlackDeathCell src = getSource();
         		lastRoll = BlackDeathChip.getDie(v);
         		int desth = dest.height();
         		boolean success = false;
        		switch(v)
        		{
        		case 1:	// always success
        			logGameEvent(AlwaysSucceeds);
        			success = true;
        			break;
        		default:
        			{
        			BlackDeathLink link = getLink(src,dest);  
        			int qv = regionIsClosed(dest,BlackDeathState.Puzzle) ? -1 : 0;
        			int adjustedVirulence = pb.adjustedVirulence(src);
        			int finalVirulence = adjustedVirulence+link.cost+dest.cost+(desth-1)+qv;
        			if(resetState==BlackDeathState.TradersPlus1) { adjustedVirulence++; }
        			success = v<=finalVirulence;
        			logGameEvent(RollDetails,""+finalVirulence,success?Success:Failure);
        			}
        			break;
				case 6:
					logGameEvent(AlwaysFails);
					break;
        		}
        		if(success)
        		{
     				// remove the victim and give him the chip back
    				if(desth>1)
    	        	{	BlackDeathChip rem = dest.removeChipAtIndex(0);
    	        		PlayerBoard rempb = getPlayer(rem.color);
    	        		rempb.chipCount++;
    	        		if(replay!=replayMode.Replay)
    	        		{	animationStack.push(dest);
    	        			animationStack.push(rempb.chipCell);
    	        		}
    	        	}
    				if(regionIsPogrom(dest,resetState))
    				{	// moving into a pogrom, die!
    					doKill(MOVE_KILL,dest,replay);
    				}
        		}
        		else
        		{
        			unDropObject();
        			unPickObject();
        			pb.chipCount++;	// give back the chip
        			if(replay!=replayMode.Replay) {
        				animationStack.push(dest);
        				animationStack.push(src);
        			}
         		}
        		infectionPoints--;
        		if(infectionPoints>0 
        				&& (pb.chipCount>0) 
        				&& hasInfectionMoves(BlackDeathState.FirstInfect)) 
        			{ setState(resetState); 
        			}
        		else 
        		{ infectionPoints = 0; 
        		  doDone(replay); 
        		}
        	}
        		break;
        	}
    		acceptPlacement();
        	}
        	break;
        case MOVE_DONE:
        	if(!DoneState())
        		{
        		p1("illegal done");
        		G.Error("illegal done");
        		}
			//$FALL-THROUGH$
		case MOVE_ESCAPE:			// escape when there are no legal moves in kill states
         	doDone(replay);

            break;
        case MOVE_TEMPORARY_CLOSE:
   			{	PlayerBoard pb = pbs[whoseTurn];
   				BlackDeathCell src = getCity(m.from_name);
   		    	BlackDeathCell dest = getCity(m.to_name);
   		    	BlackDeathLink l = getLink(src,dest);
   		    	if(pb.closedLinks.contains(l)) { pb.closedLinks.remove(l,true); closeLinkPoints++;  }
   		    	else { pb.closedLinks.push(l); closeLinkPoints--; }
   		    	setState((closeLinkPoints==0) ? BlackDeathState.Confirm : resetState);
   			}
   			break;
        case MOVE_FROM_TO:
	   		{
	    	BlackDeathCell src = getCity(m.from_name);
	    	BlackDeathCell dest = getCity(m.to_name);
	    	int cost = m.cost;
	    	boolean animate = false;
	    	BlackDeathCell exS = getSource();
	    	if(dest==exS) { unPickObject(); }
	    	else
	    	{
	    	if(!(src==getSource())) { pickObject(src,-1); animate=true;}
			PlayerBoard pb = getPlayer(pickedObject.color);
			dropObject(dest,replay,cost,m);
			dest.whenMoved = moveNumber;
			setState(BlackDeathState.Confirm);
			if(animate && replay!=replayMode.Replay)
        	{
        		animationStack.push(src);
        		animationStack.push(dest);
        	}
			if(dest.isOffMap())
			{	dest.removeTop();
				pb.chipCount++;			// give it back
				if(replay!=replayMode.Replay)
				{
				animationStack.push(dest);
				animationStack.push(pb.chipCell);
				}
			}
	    	}}
	    	break;
	    	
        case MOVE_REINFECT:	//reinfect from off the board
  			{
        	BlackDeathCell dest = getCity(m.to_name);
        	PlayerBoard pb = getPlayer(whoseTurn);
        	BlackDeathCell src = pb.chipCell;
        	if(isDest(dest)) 
        	{ unDropObject(); 
        	  unPickObject();
        	  pb.chipCount++;
        	  setState(resetState);
        	}
        	else {  pickObject(src,-1);
        			dropObject(dest,replay,0,m);
        			pb.chipCount--;
                	setState(BlackDeathState.Roll);
                	if(replay!=replayMode.Replay)
                	{
                		animationStack.push(src);
                		animationStack.push(dest);
                	}
        	}
  			}    
  			
  			break;
        case MOVE_INFECTION_ATTEMPT:
       		{
        	BlackDeathCell src = getCity(m.from_name);
        	BlackDeathCell dest = getCity(m.to_name);
        	PlayerBoard pb = getPlayer(whoseTurn);
        	if(isDest(dest)) 
        	{ unDropObject(); 
        	  unPickObject();
        	  pb.chipCount++;
        	  setState(resetState);
          	checkChips();
      	}
        	else {  
        			pickObject(src,-1);
        			dropObject(dest,replay,0,m);
        			pb.chipCount--;
                	setState(BlackDeathState.Roll);
                	if(replay!=replayMode.Replay)
                	{
                		animationStack.push(src);
                		animationStack.push(dest);
                	}
       	}
       		}       	
        	break;
        case MOVE_INFECT:	// initial infections
        	{
        	PlayerBoard pb = pbs[whoseTurn];
        	BlackDeathCell sr = pb.chipCell;
        	BlackDeathCell dest = getCity(m.to_name);
        	if(isDest(dest)) { unDropObject(); pb.chipCount++; }
        	else {  pickObject(sr,-1);
        			dropObject(dest,replay,0,m);
        			pb.chipCount--;
        			dest.whenPlaced = 0;
        			dest.whenMoved = 0;
        	}
        	if(droppedDestStack.size()==2) { setState(BlackDeathState.Confirm); }
        	else { setState(resetState); }
        	}
        	break;
        case MOVE_DROPB:
        	{
			BlackDeathChip po = pickedObject;
			BlackDeathCell dest =  getCity(m.to_name);
			BlackDeathCell src = getSource();
			if(isSource(dest)) 
				{ unPickObject(); 
				}
				else 
				{
				switch(board_state)
				{
				case Movement:
				case FirstMovement:
				case EasternMovement:
				case WesternMovement:
					throw G.Error("Should be converted into a MOVE_FROM_TO");
				case Puzzle:	
					acceptPlacement(); 
					break;
				default:
					// drop twice in a row, undo the previous drop.  This is peculiar to blackdeath.
					// where we allow you to change your mind by just clicking somewhere else.
					if(droppedDestStack.size()>0) { unDropObject(); } 
					break;
				}
    			dest.whenMoved = moveNumber;
	            dropObject(dest,replay,0,m);
	            
	            /**
	             * if the user clicked on a board space without picking anything up,
	             * animate a stone moving in from the pool.  For Hex, the "picks" are
	             * removed from the game record, so there are never picked stones in
	             * single step replays.
	             */
	            if(replay!=replayMode.Replay && (po==null))
	            	{ animationStack.push(src);
	            	  animationStack.push(dest); 
	            	}
	            setNextStateAfterDrop(replay);
				}
        	}
             break;
        case MOVE_PLAYCARD:
        	{
        	BlackDeathCell src = getCell(m.source,m.color,0);
        	PlayerBoard pb = getPlayer(m.color);
        	BlackDeathCell dest = pb.temporaryCards;
        	// find a card
        	pickObject(src,m.to_row);
        	CardEffect e = CardEffect.find(m.from_name);
        	if(e!=pickedObject.cardEffect) { 
        		// drop the intended card, regargless what what actually picked
        		// this makes game records more robust 
        		pickedObject = BlackDeathChip.getCard(e);
        	}
        	dropObject(dest,replay,0,m);
        	setState(BlackDeathState.ConfirmCard);
        	if(replay!=replayMode.Replay)
        		{
        		animationStack.push(src);
        		animationStack.push(dest);
        		}
        	}
        	break;
        case MOVE_PICK:
        	// come here only where there's something to pick, which must
 			{
 			BlackDeathCell src = getCell(m.source,m.color,0);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src,m.to_row);
 			}}
            break;

 		case MOVE_PICKB:
        	// come here only where there's something to pick, which must
 			{
 			BlackDeathCell src = getCell(m.source,m.from_name);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src,-1);
        	switch(board_state)
        	{
        	case Puzzle:
         		break;
        	case Confirm:
        		setState(BlackDeathState.SelectInfection);
        		break;
        	default: ;
        	}}}
            break;

        case MOVE_DROP: // drop on player board;
        	if(pickedObject!=null)
        	{
            BlackDeathCell dest = getCell(m.source,m.color,0);
            if(isSource(dest)) { unPickObject(); }
            else 
            	{ dropObject(dest,replay,0,m);
            	  setState(BlackDeathState.ConfirmCard);
            	}
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player,replay);
            reorderPlayersFrom(whoseTurn);
            acceptPlacement();
            int nextp = nextPlayer();

            setState(BlackDeathState.Puzzle);	// standardize the current state

            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            if((win[whoseTurn]=winForPlayerNow(whoseTurn))
               ||(win[nextp]=winForPlayerNow(nextp)))
               	{ setState(BlackDeathState.Gameover); 
               	}
            else if(allVirulenceSet()) 
            	{
            	if(allPlacementsMade())
            		{
            		setState(BlackDeathState.SelectInfection);
            		}
            		else
            		{	
            			setState(BlackDeathState.InitialPlacement);
            		}
            	}
            else 
            	{
            	setState(BlackDeathState.SetVirulence);
            	for(int i=0;i<players_in_game;i++)
            		{
            		// find the next player whose virulence isn't set
            		PlayerBoard pb = pbs[whoseTurn];
            		if(pb.virulenceSet) { setNextPlayer(replay); }
            		else { break; }
            		}
            	}
            resetState = board_state;
          break;
        case MOVE_POGROM:
   			{
	   			PlayerBoard pb = getPlayer(whoseTurn);
	   			BlackDeathCell city = getCity(m.to_name).parentCity;
	   			if(pb.pogromRegions.top()==city)
	   			{
	   				pb.pogromRegions.remove(city,true);
	   				setState(BlackDeathState.Pogrom);
	   			}
	   			else 
	   			{ pb.pogromRegions.push(city);
	   			  setState(BlackDeathState.Confirm);
	   			}
   			}
   		break;
       	
        case MOVE_QUARANTINE:
       		{
       			PlayerBoard pb = getPlayer(whoseTurn);
       			BlackDeathCell city = getCity(m.to_name).parentCity;
       			if(pb.closedRegions.top()==city)
       			{
       				pb.closedRegions.remove(city,true);
       				setState(BlackDeathState.CloseRegion);
       			}
       			else 
       			{ pb.closedRegions.push(city);
       			  setState(BlackDeathState.Confirm);
       			}
       		}
       		break;
       case MOVE_CURE:
       case MOVE_KILL:
       case MOVE_KILLANDREMOVE:
       		{
       		BlackDeathCell city = getCity(m.to_name);
       		doKill(m.op,city,replay);
       		}
       		break;
       case MOVE_RESIGN:
    	   	setState(unresign==null?BlackDeathState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(BlackDeathState.Puzzle);
 
            break;
       case MOVE_TEMPORARY_SWAP_VIRULENCE:
	  		{
	  			PlayerBoard pb = getPlayer(m.color);
	      		pb.setVirulence(pb.getVirulence()+m.to_row,false);
	      		pb.setMortality(pb.getMortality()-m.to_row,false);
	      		setState(BlackDeathState.Confirm);
	  		}
	  		break;
       case MOVE_TEMPORARY_CHANGE_BOTH:
       		{
       			PlayerBoard pb = getPlayer(m.color);
           		pb.setMortality(pb.getMortality()+m.to_row,false);
           		pb.setVirulence(pb.getVirulence()+m.to_row,false);
           		setState(BlackDeathState.Confirm);
       		}
       		break;
       case MOVE_TEMPORARY_CHANGE_VIRULENCE:
  		{
  			PlayerBoard pb = getPlayer(m.color);
      		pb.setVirulence(pb.getVirulence()+m.to_row,false);
      		setState(BlackDeathState.Confirm);
  		}
  		break;
       case MOVE_TEMPORARY_CHANGE_MORTALITY:
  		{
  			PlayerBoard pb = getPlayer(m.color);
      		pb.setMortality(pb.getMortality()+m.to_row,false);
       		setState(BlackDeathState.Confirm);
  		}
  		break;
       case MOVE_MORTALITY:
       		{
       		PlayerBoard pb = getPlayer(m.color);
       		pb.setMortality(m.to_row);
       		if(board_state==BlackDeathState.SetVirulence) { pb.setVirulence(initialTotalPips-m.to_row); }
       		if(board_state!=BlackDeathState.Puzzle) { setState(BlackDeathState.Confirm); };  
       		}
       		break;

       case MOVE_VIRULENCE:
       		{
       		PlayerBoard pb = getPlayer(m.color);
       		pb.setVirulence(m.to_row);
       		if(resetState==BlackDeathState.SetVirulence) { pb.setMortality(initialTotalPips-m.to_row); }

       		if(resetState!=BlackDeathState.Puzzle) 
       			{ setState(BlackDeathState.Confirm); 
       			};  
       		}
       		break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(BlackDeathState.Gameover);
    	   break;

        default:
        	cantExecute(m);
        }
        checkChips();
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }
    private void doKill(int op,BlackDeathCell city,replayMode replay)
    {
   		
   		BlackDeathChip chip = city.height()>0 ? city.removeTop() : null;
   		// during a fire, state AnyCatastrophiKill, city might be empty
   		if(chip!=null)
   		{
   		PlayerBoard pb = getPlayer(chip.color);
   		pb.chipCount++;		// give him the chip back
   		if(replay!=replayMode.Replay)
				{
					animationStack.push(city);
					animationStack.push(pb.chipCell);
				}
   		if((op==MOVE_KILL)||(op==MOVE_KILLANDREMOVE)) 
   			{ pb.bodyCount++;

   			  switch(pb.bodyCount)
   			  {
   			  default: break;
   			  case 5:
   			  case 10:
   			  case 20:
   				  // give another card
   				  if(drawPile.height()>0)
   				  {
   					  pb.cards.addChip(drawPile.removeTop());
   					  if(replay!=replayMode.Replay)
   					  {
   						  animationStack.push(drawPile);
   						  animationStack.push(pb.cards);
   					  }
   				  }
   				  else {
   					  p1("out of cards");
   				  }
   			  }
   			}
   		}
   		killPoints--;
   		if(killPoints==0) { setState(BlackDeathState.Confirm);}
   		if(op==MOVE_KILLANDREMOVE)
   		{
   		// if this is part of a multi-square city, mark as catastrophic
			city.addChip(BlackDeathChip.SkullIcon);
			if(replay!=replayMode.Replay)
				{ animationStack.push(getPlayer(whoseTurn).chipCell);
				  animationStack.push(city);
				}
   		}
   		
    }
    // legal to hit the chip storage area
    public boolean LegalToHitChips(BlackDeathCell c,Hashtable<BlackDeathCell,BlackDeathMovespec>targets)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting Legal Hit state " + board_state);
        case SelectInfection:
        	// for blackdeath, you can pick up a stone in the storage area
        	// but it's really optional
        	return(targets.get(c)!=null);
        case Confirm:
        case ConfirmCard:
        	return(isDest(c));
		case Resign:
		case Gameover:
			return(false);
        case Puzzle:
            return (true);
        }
    }

    public boolean LegalToHitBoard(BlackDeathCell c,Hashtable<BlackDeathCell,BlackDeathMovespec>targets)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case FirstInfect:
        case InitialPlacement:
        case Infection:
		case SelectInfection:
		case FirstMovement:
		case Movement:
		case WesternMovement:
		case EasternMovement:
		case CatastrophicKill:
		case AnyCatastrophicKill:
		case CloseRegion:
		case Pogrom:
		case TradersPlus1:
		case TradersPlus2:
		case Kill:
		case Cure:
			return(isDest(c)||(targets.get(c)!=null));
		case Gameover:
		case Resign:
        case SetVirulence:
        case MutationOr:
        case MutationSwap:
        case MutationAnd:
        case CloseLinks:
			return(false);
		case Confirm:
		case ConfirmCard:
		case Roll:
		case Roll2:
		case Mortality:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting Hit Board state " + board_state);
        case Puzzle:
            return ((c.topChip()==null)!=(pickedObject==null));
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(BlackDeathMovespec m)
    {
        Execute(m,replayMode.Replay);

    }
 

  private void putAndExtend(Hashtable<BlackDeathCell,BlackDeathMovespec> targets,BlackDeathMovespec m,BlackDeathCell city)
  {
	  commonMove e = targets.get(city);
	  m.next = e;
	  targets.put(city,m);
  }
 
  // filter the full results to just those relevant to the UI
  public Hashtable<BlackDeathCell,BlackDeathMovespec> getTargets()
  {	Hashtable<BlackDeathCell,BlackDeathMovespec> targets = new Hashtable<BlackDeathCell,BlackDeathMovespec>();
  	BlackDeathState forstate = board_state;
  	if(resetState!=null)
  		{switch(resetState)
	  	{
	  	case SetVirulence:
	  		forstate = resetState;
	  		break;
	  	default: break;
	  	}}
  	CommonMoveStack all = GetListOfMoves(forstate);
  	escapeState = false;
  	for(int lim = all.size()-1; lim>=0; lim--)
  	{
  	BlackDeathMovespec m = (BlackDeathMovespec)all.elementAt(lim);
 
  	switch(forstate)
	  {
	  default:
		  G.Error("Not expecting getTargets in state %s",board_state);
		  break;
	  case MutationSwap:
	  case MutationAnd:
	  case MutationOr:
	  	{
		  PlayerBoard p = getPlayer(m.color);
		  switch(m.op) 
		  {
		  case MOVE_TEMPORARY_SWAP_VIRULENCE:
			  targets.put(getCell(BlackDeathId.PlayerVirulence,m.color,p.getVirulence()+m.to_row),m);
			  targets.put(getCell(BlackDeathId.PlayerMortality,m.color,p.getMortality()-m.to_row),m);
			  break;
		  case MOVE_TEMPORARY_CHANGE_VIRULENCE:
		  case MOVE_TEMPORARY_CHANGE_MORTALITY:
		  case MOVE_TEMPORARY_CHANGE_BOTH:
			  targets.put(getCell(BlackDeathId.PlayerVirulence,m.color,p.getVirulence()+m.to_row),m);
			  targets.put(getCell(BlackDeathId.PlayerMortality,m.color,p.getMortality()+m.to_row),m);
			  break;
		  default: G.Error("Not expecting ",m);
		  }}
		  break;
	  case Mortality:
	  case SetVirulence:
		  targets.put(getCell(BlackDeathId.PlayerVirulence,m.color,m.to_row),m);
		  targets.put(getCell(BlackDeathId.PlayerMortality,m.color,6-m.to_row),m);
		  break;
	  case Confirm:		  
	  case Resign:
	  case ConfirmCard:		  
      case Puzzle: 
    	  break;
	  case Roll:
	  case Roll2:
		  switch(m.op)
		  {
		  default: break;
		  case MOVE_PLAYCARD:
			  if(pickedObject!=null)
			  {
				  targets.put(getCell(m.dest,m.color,0),m);
			  }
			  else
			  {
			  targets.put(getCell(m.source,m.color,0), m);
			  }
		  }
		  break;
	  case CloseLinks:
		  switch(m.op)
		  {
  		  case MOVE_FROM_TO:
  			putAndExtend(targets,m,getCity(m.to_name));
  			break;
  		  default: break;
		  }
		  break;
	  case Movement:
	  case WesternMovement:
	  case EasternMovement:
	  case FirstMovement:
		  switch(m.op)
		  {
		  case MOVE_TEMPORARY_CLOSE:
  		  case MOVE_FROM_TO:
  			  if(pickedObject==null)
  			  {
  			putAndExtend(targets,m,getCity(m.from_name));
  			  }
  			  else { 
  				putAndExtend(targets,m,getCity(m.to_name));
  			  }
  			
  			break;
  		  default: break;
		  }
		  break;
	  case InitialPlacement:
	  case Infection:
	  case Cure:
	  case Kill:
	  case CatastrophicKill:
	  case AnyCatastrophicKill:
	  case FirstInfect:
	  case CloseRegion:
	  case Pogrom:
	  case TradersPlus1:
	  case TradersPlus2:
	  case SelectInfection:
	  		switch(m.op) 
	  		{ default: break;
	  		  case MOVE_ESCAPE:
	  			  escapeState = true;
	  			  break;
	  		  case MOVE_INFECTION_ATTEMPT:
	  		  case MOVE_SELECT:
	  		  case MOVE_KILL:
	  		  case MOVE_KILLANDREMOVE:
	  		  case MOVE_QUARANTINE:
	  		  case MOVE_POGROM:
	  		  case MOVE_CURE:
	  		  case MOVE_INFECT:
	  		  case MOVE_REINFECT:
	  			  targets.put(getCity(m.to_name),m);
	  		}
	  }}
	return(targets);  
  }
 private boolean addFamilyGroup(CommonMoveStack all,int sweep,boolean move,int cost,BlackDeathState forState,
		 BlackDeathCell from,BlackDeathCell toc)
 {	BlackDeathCell to = toc.parentCity;
 	BlackDeathChip fromChip = from.topChip();
 	boolean some = false;
 	while(to!=null)
	 {
		 if(to.sweep_counter!=sweep && (move || !to.isOffMap()))
		 {
			 to.sweep_counter = sweep;
			 BlackDeathChip top = to.topChip();
			 // if we're moving or this is the first round, the destination has to be empty
			 // otherwise, it can be any color except our own or the skull marking a dead square
			 boolean first = false;
			 boolean permitted = true;
			 boolean links = false;
			 int op = move ? MOVE_FROM_TO : MOVE_INFECTION_ATTEMPT;
			 
			 switch(forState)
			 	{
			 	case FirstMovement:
			 	case FirstInfect:
			 		first = true;
			 		break;
			 	case WesternMovement:
			 		// must move away from the edge
			 		permitted = from.distanceToEast<to.distanceToEast;
			 		break;
			 	case EasternMovement:
			 		// must move to jerusalem
			 		permitted = (from.distanceToJerusalem>to.distanceToJerusalem);
			 		break;
			 	case CloseLinks:
			 		op = MOVE_TEMPORARY_CLOSE;
			 		links = true;
			 		permitted = from.parentCity!=to.parentCity;
			 		break;
			 	default: break;
			 	}
			 if(permitted
					 && ((first || move) 
							 ? (links || top==null) 
							 : (top!=BlackDeathChip.SkullIcon) && (top!=fromChip)))
			 {	if(all==null) { return(true); }
			 	some = true;
			 	BlackDeathMovespec m = move 
			 			? new BlackDeathMovespec(op,from,to,cost,whoseTurn)
			 			: new BlackDeathMovespec(op,from,to,whoseTurn);
			 	all.push(m);
			 }
		 }
		 to = to.sisterCity;
	 }
	return(some);
 }
 private boolean addMovements(CommonMoveStack all,BlackDeathCell c,BlackDeathChip top,BlackDeathState forState)
 {	 PlayerBoard pb = pbs[whoseTurn];
 	 boolean smuggler = pb.hasCapability(CardEffect.Smugglers); 
 	 int steps = smuggler ? 2 : 1;
 	 return addMovements(all,c,c,top,forState,movementPoints,steps,0);
 }
 
 private boolean addMovements(CommonMoveStack all,BlackDeathCell c,BlackDeathCell pivot,BlackDeathChip top,
		 BlackDeathState forState,int availableMovements,int steps,int prevCost)
 {	int sweep = sweep_counter;
 	boolean some = false;
 	BlackDeathCell parent = pivot.parentCity;
 	LinkStack links = parent.links;
 	boolean linkMode = forState==BlackDeathState.CloseLinks;
 	some |= (linkMode||prevCost>0) ? false : addFamilyGroup(all,sweep,true,-1,forState,c,c);
 	if(all==null && some) { return(some); }
 	if(!regionIsClosed(parent,forState) ) 
 	{	
 	// closed regions can't spread
 	for(int lim = links.size()-1; lim>=0; lim--)
		{	BlackDeathLink l = links.elementAt(lim);
			if(linkMode || Math.abs(l.cost)<availableMovements)
			{
			if(!regionIsClosed(l.to,forState))
			{
			some |= addFamilyGroup(all,sweep,true,prevCost+l.cost-1,forState,c,l.to);
		 	if(all==null && some) { return(some); }
		 	if(steps>1 && !linkMode)
		 		{
		 		// smuggler lets you move twice
		 		addMovements(all,c,l.to,top,forState,availableMovements-l.cost-1,steps-1,prevCost+l.cost-1);
		 		}
			}
			if(!regionIsClosed(l.from,forState)) 
			{
			some |= addFamilyGroup(all,sweep,true,prevCost+l.cost-1,forState,c,l.from);
		 	if(all==null && some) { return(some); }
		 	if(steps>1)
	 		{
	 		// smuggler lets you move twice
	 		addMovements(all,c,l.from,top,forState,availableMovements-l.cost-1,steps-1,prevCost+l.cost-1);
	 		}

			}}
		}}
 	return(some);
 }
 
 public boolean addInfectionMoves(CommonMoveStack all,BlackDeathState forState,int whoseTurn)
 {	boolean some = false;
 	PlayerBoard pb = pbs[whoseTurn];
 	BlackDeathChip chip = pb.chip;
 	sweep_counter++;
 	int sweep = sweep_counter;
 	switch(forState)
 	{
 	default: throw G.Error("not expecting state %s",forState);
 	case TradersPlus1:
 	case TradersPlus2:
 		// traders never infect from within the map
 		break;
 	case FirstInfect:
 	case Infection:
 	
 	for(BlackDeathCell c = allCells;c!=null; c=c.next)
 		{
 			if((c.topChip()==chip) && (c.whenPlaced<moveNumber))	// placed on a previous turn
 			{
 			BlackDeathCell parent = c.parentCity;
 			LinkStack links = parent.links;
 			some |= addFamilyGroup(all,sweep,false,0,forState,c,c);
 			if(!regionIsClosed(parent,forState))
 			{
 			if(some && all==null) { return(true); } 
 			for(int lim = links.size()-1; lim>=0; lim--)
 			{	BlackDeathLink l = links.elementAt(lim);
 				if(!linkIsClosed(l))
 				{
 				if(!regionIsClosed(l.to,forState))
 				{
 				some |= addFamilyGroup(all,sweep,false,0,forState,c,l.to);
 				if(some && all==null) { return(true); }
 				}
 				if(!regionIsClosed(l.from,forState))
 				{
				some |= addFamilyGroup(all,sweep,false,0,forState,c,l.from);
 				if(some && all==null) { return(true); }
 				}}
 			}
 			}}
		}}
		
 	if(!some)
		{	// no units on the map can infect, try to get back onto the map
			// except that it can try to displace an existing player
			for(int lim=initialPlacement.size()-1; lim>=0; lim--)
			{
			BlackDeathCell c = initialPlacement.elementAt(lim);
			BlackDeathChip top = c.topChip();
			boolean firstTurn = (forState==BlackDeathState.FirstMovement) || (forState==BlackDeathState.FirstMovement);
			if((firstTurn ? top==null : (top!=pb.chip)) && (top!=BlackDeathChip.SkullIcon))
				{
				if(all==null) { return(true); }
				else { all.push(new BlackDeathMovespec(MOVE_REINFECT,c,whoseTurn)); }
				}
			}			
		}
	
	 return(some);
 }
 public boolean hasInfectionMoves(BlackDeathState forState)
 {	return(addInfectionMoves(null,forState,whoseTurn));
 }
 public boolean addMovements(CommonMoveStack all,BlackDeathState forState)
 {
	 PlayerBoard pb = pbs[whoseTurn];
	 BlackDeathChip chip = pb.chip;
	 boolean some = false;
		sweep_counter++;
		if(pickedObject!=null && pickedSourceStack.top().onBoard)
		{
			some |= addMovements(all,pickedSourceStack.top(),pickedObject,forState);
		}
		else
		{
		boolean linkMode = forState==BlackDeathState.CloseLinks;
		for(BlackDeathCell c = allCells;c!=null; c=c.next)
			{
			BlackDeathChip top = c.topChip();
			boolean firstTurn = (forState==BlackDeathState.FirstMovement) || (forState==BlackDeathState.FirstMovement);
			boolean canMove = linkMode || 
								(c.whenMoved<moveNumber) 
									&& (firstTurn ? (top==chip) : ((top!=null) && (top!=BlackDeathChip.SkullIcon)));
			if(canMove)	// placed on a previous turn
				{	
					sweep_counter++;
					some |= addMovements(all,c,top,forState);

				}
			}
		}
	return(some);
 }
 public boolean hasMovements(BlackDeathState forstate)
 {
	 return(addMovements(null,forstate));
 }
 public void addPlayCardMoves(CommonMoveStack all,PlayerBoard pb)
 {	if(pickedObject!=null)
 	{
	 addPlayCardMoves(all,pb,pickedObject.cardEffect,pickedIndex);
 	}
 	else
 	{
	BlackDeathCell cards = pb.cards;
	for(int lim = cards.height()-1; lim>=0; lim--)
	{	addPlayCardMoves(all,pb,pb.cards.chipAtIndex(lim).cardEffect,lim);
	}}
 }
 public void addPlayCardMoves(CommonMoveStack all,PlayerBoard pb,CardEffect e,int ind)
 {
	 all.push(new BlackDeathMovespec(MOVE_PLAYCARD,pb.cards,pb.color,ind,e,pb.temporaryCards,whoseTurn));
 }
 
 public CommonMoveStack  GetListOfMoves()
 {	return GetListOfMoves(board_state);
 }
 private void changeMutationMoves(CommonMoveStack all,BlackDeathState forState,int who)
 {	
 	int range = 1;
 	int op;
 	int op2 = 0;
 	switch(forState)
 	{
 	case MutationSwap:
 		op = MOVE_TEMPORARY_SWAP_VIRULENCE;
 		range = 2;
 		break;
 	case MutationAnd:
 		op = MOVE_TEMPORARY_CHANGE_BOTH;
 		break;
 	case MutationOr:
 		op = MOVE_TEMPORARY_CHANGE_VIRULENCE;
 		op2 = MOVE_TEMPORARY_CHANGE_MORTALITY;
 		break;
 	default: throw G.Error("Not expecting forstate %s",forState);
 	}
    for(PlayerBoard pb : pbs)
	 {	
		 int vir = pb.getVirulence();
		 int mort = pb.getMortality();
		 // pair changes +-2 symmetrically up and down
		 // single changes +=1 both in the same direction
		 for(int change = -range; change<=range; change++)
		 {	
		 	
			 boolean test = false;
			 boolean test2 = false;
			 switch(forState)
			 {
			 	case MutationSwap:
			 		// both move in opposite directions 
			 		test = (vir+change<=MAX_VIRULENCE) 
							&& (mort-change<=MAX_VIRULENCE) 
							&& (vir+change>=MIN_VIRULENCE)
							&& (mort-change>=MIN_VIRULENCE);
			 		break;
			 	case MutationAnd:
			 		// both move in the same direction
			 		test = (vir+change<=MAX_VIRULENCE) 
			 				&& (mort+change<=MAX_VIRULENCE) 
			 				&& (vir+change>=MIN_VIRULENCE) 
			 				&& (mort+change>=MIN_VIRULENCE);
					break;
			 	case MutationOr:
			 		// one moves
			 		test = (vir+change<=MAX_VIRULENCE) 
							&& (vir+change>=MIN_VIRULENCE);
			 		test2 = (mort+change<=MAX_VIRULENCE) 
			 				&& (mort+change>=MIN_VIRULENCE);
			 		break;
			 	default: throw G.Error("Not expecting forstate %s",forState);
			 }

			 if(test)
				 {all.push(new BlackDeathMovespec(op,pb.color,change,who));
				 }
			 if(test2)
			 	{
				 all.push(new BlackDeathMovespec(op2,pb.color,change,who));
			 	}
		 	
		 }}
 }
		 
 private CommonMoveStack GetListOfMoves(BlackDeathState forState)
 {	CommonMoveStack all = new CommonMoveStack();
 	boolean cure = false;
 	boolean catastrophic = false;
 	switch(forState)
 	{
 	case Roll2:
 		{
 		PlayerBoard pl = getPlayer(whoseTurn);
 		BlackDeathColor color = pl.color;
 		addPlayCardMoves(all,pl);
 		for(int i=1;i<=6;i++)
 			{ for(int j=1;j<=6;j++) 
 				{
 				all.push(new BlackDeathMovespec(MOVE_ROLL2,color,i*10+j,whoseTurn));
 				}
 			}
 		}
 		break;
 		
 	case Roll:
  	case Mortality:
 		{
 		PlayerBoard pl = getPlayer(whoseTurn);
 		BlackDeathColor color = pl.color;
 		for(int i=1;i<=6;i++) { all.push(new BlackDeathMovespec(MOVE_ROLL,color,i,whoseTurn)); }
 		if(forState==BlackDeathState.Roll)
 		{	
 			BlackDeathCell from = getSource();
 			if(pl.canWinAutomatically(from))
 			{	// roll of 1 always wins
 				all.push(new BlackDeathMovespec(MOVE_PERFECTROLL,color,1,whoseTurn));	
 			}
 		}}
 		break;
 	case Resign:
 	case Confirm:
 	case ConfirmCard:
 		all.push(new BlackDeathMovespec(MOVE_DONE,whoseTurn));
 		break;
 	case SetVirulence:
 		{
 		PlayerBoard pb = getPlayer(whoseTurn);
 		BlackDeathColor color = pb.color;
 		int cv = pb.getVirulence();
 		for(int i=MIN_INITIAL_VIRULENCE; i<=MAX_INITIAL_VIRULENCE; i++ )
 		{	if(i!=cv || (board_state==BlackDeathState.SetVirulence))
 		{
 			all.push(new BlackDeathMovespec(MOVE_VIRULENCE,color,i,whoseTurn));
 			}
 		}}
 		break;
 	case MutationSwap:
 	case MutationAnd:
 	case MutationOr:
 		// temporary virulence or mortality
 		changeMutationMoves(all,forState,whoseTurn);
 		break;
 	case FirstMovement:
	case Movement:
		// you can always decline to move anything
		all.push(new BlackDeathMovespec(MOVE_DONE,whoseTurn)); 	
		//$FALL-THROUGH$
 	case CloseLinks:
	case EasternMovement:		// we don't get into these states unless there is movement available
 	case WesternMovement:
		addMovements(all,forState);

	 	break;
 	case FirstInfect:
 	case TradersPlus1:
 	case TradersPlus2:
	case Infection:
	 	addInfectionMoves(all,forState,whoseTurn);
	 	if(all.size()==0) {  all.push(new BlackDeathMovespec(MOVE_DONE,whoseTurn)); }
 		break;
 	case AnyCatastrophicKill:
		{
		boolean some = false;
		for(BlackDeathCell c = allCells;c!=null; c=c.next)
		{	// remove a cell, not necessarily an occupied one
			if(canBeRemoved(c))
			{	some = true;
				all.push(new BlackDeathMovespec(MOVE_KILLANDREMOVE,c,whoseTurn));
			}
		}
		if(!some) { all.push(new BlackDeathMovespec(MOVE_ESCAPE,whoseTurn));}
		}
		break;
 	case Pogrom:
 	case CloseRegion:
 		{
 		int op = (forState==BlackDeathState.CloseRegion) ? MOVE_QUARANTINE : MOVE_POGROM;
 		sweep_counter++;
		for(BlackDeathCell c = allCells;c!=null; c=c.next)
		{	// remove a cell, not necessarily an occupied one
			BlackDeathCell p = c.parentCity;
			if((p==c) && (p.sweep_counter!=sweep_counter))
			{	p.sweep_counter = sweep_counter;
				all.push(new BlackDeathMovespec(op,c,whoseTurn));
			}
		}
 		}
		break;
 	case Cure:
 		cure = true;
		//$FALL-THROUGH$ 		
 	case CatastrophicKill:
 		catastrophic = true;
		//$FALL-THROUGH$
	case Kill:
	 	{	PlayerBoard victim = pbs[killVictim];
			BlackDeathChip chip = victim.chip;
			boolean some = false;
			if(killPoints>0)
			{
			for(BlackDeathCell c = allCells;c!=null; c=c.next)
			{
			if(c.topChip()==chip)	// placed on a previous turn
				{	boolean canremove = catastrophic && canBeRemoved(c);
					all.push(new BlackDeathMovespec(cure 
														? MOVE_CURE 
														: canremove ? MOVE_KILLANDREMOVE : MOVE_KILL,
													c,
													whoseTurn));
					some = true;
				}
			}
			}
			if(!some) { all.push(new BlackDeathMovespec(MOVE_ESCAPE,whoseTurn));}
	 	}
	 	break;
 	case InitialPlacement:
 		{
 		for(int lim=initialPlacement.size()-1; lim>=0; lim--)
 			{
 			BlackDeathCell c = initialPlacement.elementAt(lim);
 			if(c.topChip()==null)
 				{
 				all.push(new BlackDeathMovespec(MOVE_INFECT,c,whoseTurn));
 				}
 			}
 		}
 		break;
 	case SelectInfection:
 			all.push(new BlackDeathMovespec(MOVE_SELECT,initialDice1,whoseTurn));
 			all.push(new BlackDeathMovespec(MOVE_SELECT,initialDice2,whoseTurn));
 			break;
 	case Gameover:
 	case Puzzle: break;
 		default: G.Error("Not expecting state %s", forState);
 	}
 	if((forState!=BlackDeathState.Puzzle) && (all.size()==0))
 	{
 		p1("No moves generated, state "+forState);
 		if(robot!=null) { G.Error("Robot oops"); }
 	}
 	return(all);
 }
 
 BlackDeathPlay robot = null;

 public boolean p1(String msg)
	{
		if(G.p1(msg) && (robot!=null))
		{	String dir = "g:/share/projects/boardspace-html/htdocs/blackdeath/blackdeathgames/robot/";
			robot.saveCurrentVariation(dir+msg+".sgf");
			return(true);
		}
		return(false);
	}
 
 public void initRobotValues()
 {

 }


public int cellToX(BlackDeathCell c) {
	return((int)(G.Left(boardRect)+c.xpos*G.Width(boardRect)/100));
}

public int cellToY(BlackDeathCell c) {
	return((int)(G.Top(boardRect)+c.ypos*G.Height(boardRect)/100));
}

public void setDistances()
{	sweep_counter++;
	setDistances(getCity("Jerusalem"),0);
	for(BlackDeathCell c = allCells;c!=null;c=c.next) { c.distanceToJerusalem = c.distance; }
	sweep_counter++;
	for(String n : easternNames)
	{	BlackDeathCell c = getCity(n);
		setDistances(c,0);
	}
	for(BlackDeathCell c = allCells;c!=null;c=c.next) { c.distanceToEast = c.distance; }
}

private void setDistances(BlackDeathCell city,int distance)
{
	if((city.sweep_counter!=sweep_counter) || (city.distance>distance))
	{	for(BlackDeathCell parent = city.parentCity; parent!=null; parent=parent.sisterCity)
		{
		parent.distance = distance;
		parent.sweep_counter = sweep_counter;
		}
		LinkStack links = city.parentCity.links;
		for(int lim=links.size()-1; lim>=0; lim--)
		{
			BlackDeathLink l = links.elementAt(lim);
			setDistances(l.to,distance+1);
			setDistances(l.from,distance+1);
			
		}
	}
}

}
