package pendulum;

import static pendulum.PendulumMovespec.MOVE_FROM_TO;

import java.awt.Rectangle;

import lib.Digestable;
import lib.G;
import lib.Random;
import online.game.CommonMoveStack;
import online.game.replayMode;

public class PlayerBoard implements PendulumConstants,Digestable{
	int boardIndex = 0;
	int gameIndex = 0;
	public PendulumCell allCells = null;
	PendulumBoard parent;
	boolean advanced = false;
	PendulumChip meeple = null;
	PendulumChip grande = null;
	PendulumChip cylinder = null;
	PendulumChip hexagon = null;
	PColor color = null;
	PendulumChip mat = null;
	PendulumCell stratCards = newcell(PendulumId.PlayerStratCard);
	private PendulumChip beginnerMat = null;
	private PendulumChip advancedMat = null;
	PendulumCell meeples = newcell(PendulumId.PlayerMeeples);
	PendulumCell grandes = newcell(PendulumId.PlayerGrandes);
	private PendulumCell playedStratCards = newcell(PendulumId.PlayerPlayedStratCard,BC.PerCard,BB.PerCard);
	private PendulumCell military = newcell(PendulumId.PlayerMilitary);
	private PendulumCell culture = newcell(PendulumId.PlayerCulture);
	private PendulumCell cash = newcell(PendulumId.PlayerCash);
	private PendulumCell votes = newcell(PendulumId.PlayerVotes);
	private PendulumCell grandeReserves = newcell(PendulumId.PlayerGrandeReserves);
	private PendulumCell meepleReserves = newcell(PendulumId.PlayerMeepleReserves);
	PendulumCell legendary = newcell(PendulumId.PlayerLegendary);
	private PendulumCell militaryReserves = newcell(PendulumId.PlayerMilitaryReserves);
	private PendulumCell cultureReserves = newcell(PendulumId.PlayerCultureReserves);
	private PendulumCell cashReserves = newcell(PendulumId.PlayerCashReserves);
	private PendulumCell votesReserves = newcell(PendulumId.PlayerVotesReserves);
	private PendulumCell max3Cards = newcell(PendulumId.PlayerMax3Cards);
	private PendulumCell freeD2 = newcell(PendulumId.PlayerFreeD2Card);
	
	private PendulumCell militaryVP[] = newcell(PendulumId.PlayerMilitaryVP,22);
	private PendulumCell prestigeVP[] = newcell(PendulumId.PlayerPrestigeVP,22);
	private PendulumCell popularityVP[] = newcell(PendulumId.PlayerPopularityVP,22);
	
	private PendulumCell blueBenefits = newcell(PendulumId.PlayerBlueBenefits);
	private PendulumCell yellowBenefits = newcell(PendulumId.PlayerYellowBenefits);
	private PendulumCell redBenefits = newcell(PendulumId.PlayerRedBenefits);
	private PendulumCell brownBenefits = newcell(PendulumId.PlayerBrownBenefits);
	PendulumCell tucked[] = { blueBenefits, yellowBenefits, redBenefits, brownBenefits};
	private int militaryVPvalue = 0;
	private int prestigeVPvalue = 0;
	private int popularityVPvalue = 0;
	public int uiCount = 0;
	public UIState uiState = UIState.Normal;
	
	public String toString() { return "<pb "+color+">"; }
	
	private PendulumCell newcell(PendulumId id)
	{
		PendulumCell cell = new PendulumCell(id,(char)('A'+gameIndex));
		cell.next = allCells;
		allCells = cell;
		return cell;
	}
	private PendulumCell newcell(PendulumId id,BC co,BB be)
	{
		PendulumCell c = newcell(id);
		c.cost = co;
		c.benefit = be;
		return c;
	}
	private PendulumCell[] newcell(PendulumId id,int n)
	{	PendulumCell cells[] = new PendulumCell[n];
		for(int i=0;i<n;i++) { 
			PendulumCell c = cells[i] = newcell(id);
			c.row = i;
		}
		return cells;
	}

	PlayerBoard(PendulumBoard parentBoard,PColor i,int ind)
	{	color = i;
		parent = parentBoard;
		gameIndex = ind;
		for(PendulumCell c = allCells; c!=null; c=c.next) { c.col = (char)('A'+gameIndex); }
		meeple = PendulumChip.chips[ind];
		grande = PendulumChip.bigchips[ind];
		mat = PendulumChip.mats[ind];
		hexagon = PendulumChip.hexes[ind];
		cylinder = PendulumChip.cylinders[ind];
		beginnerMat = mat;
		advancedMat = PendulumChip.advancedmats[ind];
	}
	
	public long Digest(Random r)
	{
		long v = 0;	
		v ^= stratCards.Digest(r);
		v ^= playedStratCards.Digest(r);
		v ^= meeples.Digest(r);
		v ^= grandes.Digest(r);
		v ^= meepleReserves.Digest(r);
		v ^= grandeReserves.Digest(r);
		v ^= military.Digest(r);
		v ^= culture.Digest(r);
		v ^= cash.Digest(r);
		v ^= max3Cards.Digest(r);
		v ^= freeD2.Digest(r);
		v ^= legendary.Digest(r);
		v ^= votes.Digest(r);
		v ^= cashReserves.Digest(r);
		v ^= militaryReserves.Digest(r);
		v ^= cultureReserves.Digest(r);
		v ^= votesReserves.Digest(r);
		
		v ^= redBenefits.Digest(r);
		v ^= brownBenefits.Digest(r);
		v ^= blueBenefits.Digest(r);
		v ^= yellowBenefits.Digest(r);
		
		v ^= parent.Digest(r,militaryVP);
		v ^= parent.Digest(r,prestigeVP);
		v ^= parent.Digest(r,popularityVP);
		v ^= parent.Digest(r,militaryVPvalue);
		v ^= parent.Digest(r,prestigeVPvalue);
		v ^= parent.Digest(r,popularityVPvalue);
		v ^= parent.Digest(r,pickedSource);
		v ^= parent.Digest(r,droppedDest);
		v ^= parent.Digest(r,pickedObject);
		v ^= parent.Digest(r,droppedObject);
		v ^= parent.Digest(r,dropState);
		v ^= parent.Digest(r,pickedIndex);
		
		v ^= uiState.Digest(r);
		v ^= parent.Digest(r,uiCount);
		
		return v;
	}
	public void doInit(boolean adv,int board)
	{	advanced = adv;
		boardIndex = board;
		for(PendulumCell c = allCells; c!=null; c=c.next) { c.reInit(); }

		mat = advanced ? advancedMat : beginnerMat;
		
		for(int i=0;i<4;i++)
		{
			stratCards.addChip(PendulumChip.stratcards[advanced ? gameIndex*8+4+i : gameIndex*8+i]);
		}
		brownBenefits.pb = mat.pb[0];
		yellowBenefits.pb = mat.pb[3];
		blueBenefits.pb = mat.pb[2];
		redBenefits.pb = mat.pb[1];
		
		grandes.addChip(grande);
		meeples.addChip(meeple);
		
		grandeReserves.addChip(grande);
		meepleReserves.addChip(meeple);
		meepleReserves.addChip(meeple);
		pickedSource = droppedDest = null;
		pickedObject = droppedObject = null;
		dropState = null;
		pickedIndex = -1;
		setUIState(UIState.Normal,0);
		
		int resources[] = mat.resources;
		int initialvp[] = mat.vps;
		setMilitaryVP(initialvp[0],replayMode.Replay);
		setPrestigeVP(initialvp[1],replayMode.Replay);
		setPopularityVP(initialvp[2],replayMode.Replay);
		for(int i=0;i<STARTING_MILITARY;i++) 
		{
			militaryReserves.addChip(PendulumChip.redCube);
		}
		for(int i=0;i<STARTING_CASH;i++) 
			{
			cashReserves.addChip(PendulumChip.yellowCube);
			}
		for(int i=0;i<STARTING_CULTURE;i++) 
			{
			cultureReserves.addChip(PendulumChip.blueCube);
			}
		for(int i=0;i<STARTING_VOTES;i++) 
			{
			votesReserves.addChip(PendulumChip.vote);
			}
		for(int i=0;i<resources[0];i++) { military.addChip(militaryReserves.removeTop()); }
		for(int i=0;i<resources[1];i++) { culture.addChip(cultureReserves.removeTop()); }
		for(int i=0;i<resources[2];i++) { cash.addChip(cashReserves.removeTop()); }
		for(int i=0;i<resources[3];i++) { votes.addChip(votesReserves.removeTop()); }
	}
	
	private void setPopularityVP(int i,replayMode replay) {
		int oldval = popularityVPvalue;
		int newval = Math.max(0,Math.min(i,21));
		if(oldval!=newval || popularityVP[oldval].isEmpty())
		{
		popularityVPvalue = newval;
		popularityVP[oldval].reInit();
		popularityVP[newval].addChip(PendulumChip.yellowPost);
		if(replay.animate) { parent.animate(popularityVP[oldval],popularityVP[newval]); }
		}
		
	}
	private void setPrestigeVP(int i,replayMode replay) {
		int oldval = prestigeVPvalue;
		int newval = Math.max(0,Math.min(i,21));
		if(oldval!=newval || prestigeVP[oldval].isEmpty())
		{
		prestigeVPvalue = newval;
		prestigeVP[oldval].reInit();
		prestigeVP[newval].addChip(PendulumChip.bluePost);
		if(replay.animate) { parent.animate(prestigeVP[oldval],prestigeVP[newval]); }
		}
	}
	private void setMilitaryVP(int i,replayMode replay) {
		int oldval = militaryVPvalue;
		int newval = Math.max(0,Math.min(i,21));
		if(newval!=oldval || militaryVP[oldval].isEmpty())
		{
			militaryVPvalue = newval;
			militaryVP[oldval].reInit();
			militaryVP[newval].addChip(PendulumChip.redPost);
			if(replay.animate) { parent.animate(militaryVP[oldval],militaryVP[newval]); }
		}
	}
	public void copyFrom(PlayerBoard other) {
		stratCards.copyFrom(other.stratCards);
		playedStratCards.copyFrom(other.playedStratCards);
		meeples.copyFrom(other.meeples);
		grandeReserves.copyFrom(other.grandeReserves);
		meepleReserves.copyFrom(other.meepleReserves);
		grandes.copyFrom(other.grandes);
		military.copyFrom(other.military);
		culture.copyFrom(other.culture);
		cash.copyFrom(other.cash);
		max3Cards.copyFrom(other.max3Cards);
		freeD2.copyFrom(other.freeD2);
		legendary.copyFrom(other.legendary);
		cashReserves.copyFrom(other.cashReserves);
		militaryReserves.copyFrom(other.militaryReserves);
		cultureReserves.copyFrom(other.cultureReserves);
		votesReserves.copyFrom(other.votesReserves);
		
		votes.copyFrom(other.votes);
		parent.copyFrom(militaryVP,other.militaryVP);
		parent.copyFrom(prestigeVP,other.prestigeVP);
		parent.copyFrom(popularityVP,other.popularityVP);
		
		brownBenefits.copyFrom(other.brownBenefits);
		blueBenefits.copyFrom(other.blueBenefits);
		yellowBenefits.copyFrom(other.yellowBenefits);
		redBenefits.copyFrom(other.redBenefits);
		uiState = other.uiState;
		uiCount = other.uiCount;
		militaryVPvalue = other.militaryVPvalue;
		prestigeVPvalue = other.prestigeVPvalue;
		popularityVPvalue = other.popularityVPvalue;
		pickedSource = parent.getCell(other.pickedSource);
		droppedDest = parent.getCell(other.droppedDest);
		pickedObject = other.pickedObject;
		droppedObject = other.droppedObject;
		dropState = other.dropState;
		pickedIndex = other.pickedIndex;
	}

	public void sameboard(PlayerBoard other) {
		G.Assert(stratCards.sameContents(other.stratCards),"stratCards mismatch");
		G.Assert(playedStratCards.sameContents(other.playedStratCards),"playedStratCards mismatch");
		G.Assert(meeples.sameContents(other.meeples),"meeples ,mismatch");
		G.Assert(grandes.sameContents(other.grandes),"grandes ,mismatch");
		G.Assert(meepleReserves.sameContents(other.meepleReserves),"meepleReserves ,mismatch");
		G.Assert(grandeReserves.sameContents(other.grandeReserves),"grandeReserves ,mismatch");
		G.Assert(military.sameContents(other.military),"military mismatch");
		G.Assert(culture.sameContents(other.culture),"culture mismatch");
		G.Assert(cash.sameContents(other.cash),"cash mismatch");
		G.Assert(max3Cards.sameContents(other.max3Cards),"max3Cards mismatch");
		G.Assert(freeD2.sameContents(other.freeD2),"freeD2 mismatch");
		G.Assert(votes.sameContents(other.votes),"votes mismatch");
		G.Assert(legendary.sameContents(other.legendary),"legendary mismatch");

		G.Assert(militaryReserves.sameContents(other.militaryReserves),"militaryReserves mismatch");
		G.Assert(cultureReserves.sameContents(other.cultureReserves),"cultureReserves mismatch");
		G.Assert(cashReserves.sameContents(other.cashReserves),"cashReserves mismatch");
		G.Assert(votesReserves.sameContents(other.votesReserves),"votesReserves mismatch");

		G.Assert(brownBenefits.sameContents(other.brownBenefits),"brownBenefits mismatch");
		G.Assert(blueBenefits.sameContents(other.blueBenefits),"blueBenefits mismatch");
		G.Assert(yellowBenefits.sameContents(other.yellowBenefits),"yellowBenefits mismatch");
		G.Assert(redBenefits.sameContents(other.redBenefits),"redBenefits mismatch");
		
		G.Assert(parent.sameContents(militaryVP,other.militaryVP),"militaryVP mismatch");
		G.Assert(parent.sameContents(prestigeVP,other.prestigeVP),"prestigeVP mismatch");
		G.Assert(parent.sameContents(popularityVP,other.popularityVP),"popularityVP mismatch");
		G.Assert(militaryVPvalue==other.militaryVPvalue,"militaryVPvalue mismatch");
		G.Assert(prestigeVPvalue==other.prestigeVPvalue,"prestigeVPvalue mismatch");
		G.Assert(popularityVPvalue==other.popularityVPvalue,"popularityVPvalue mismatch");
		G.Assert(parent.sameCells(pickedSource,other.pickedSource),"pickedSource mismatch");
		G.Assert(parent.sameCells(droppedDest,other.droppedDest),"droppedDest mismatch");
		G.Assert(pickedObject==other.pickedObject,"pickedObject mismatch");
		G.Assert(droppedObject==other.droppedObject,"droppedObject mismatch");
		G.Assert(dropState==other.dropState,"dropState mismatch");
		G.Assert(pickedIndex==other.pickedIndex,"pickedIndex mismatch");
		G.Assert(uiState==other.uiState,"uiState mismatch");
		G.Assert(uiCount==other.uiCount,"uiCount mismatch");
	}
	
	public PendulumCell getCell(PendulumId id,int idx)
	{
		switch(id)
		{
		case PlayerGrandeReserves: return grandeReserves;
		case PlayerMeepleReserves: return meepleReserves;
		case PlayerMeeples: return meeples;
		case PlayerGrandes: return grandes;
		case PlayerStratCard: return stratCards;
		case PlayerPlayedStratCard: return playedStratCards;
		case PlayerMilitary: return military;
		case PlayerCash: return cash;
		case PlayerCulture: return culture;
		case PlayerVotes: return votes;
		case PlayerMilitaryVP: return militaryVP[idx];
		case PlayerPrestigeVP: return prestigeVP[idx];
		case PlayerPopularityVP: return popularityVP[idx];
		case PlayerLegendary: return legendary;
		case PlayerMax3Cards: return max3Cards;
		case PlayerFreeD2Card: return freeD2;
		case PlayerMilitaryReserves: return militaryReserves;
		case PlayerCashReserves: return cashReserves;
		case PlayerCultureReserves: return cultureReserves;
		case PlayerVotesReserves: return votesReserves;
		case PlayerBrownBenefits: return brownBenefits;
		case PlayerYellowBenefits: return yellowBenefits;
		case PlayerBlueBenefits: return blueBenefits;
		case PlayerRedBenefits: return redBenefits;
		
		default: throw G.Error("cell %s doesn't exist");
		}
	}
	
	public void setLocations() {
		stratCards.setLocation(0.2,0.4,0.1);
		grandes.setLocation(0.63,0.48,0.14);
		meeples.setLocation(0.68,0.48,0.14);
		military.setLocation(0.6,0.7,0.14);
		culture.setLocation(0.76,0.7,0.14);
		cash.setLocation(0.90,0.7,0.14);
		votes.setLocation(0.84,0.47,0.14);
		max3Cards.setLocation(0.1,0.5,0.14);
		freeD2.setLocation(0.0,0.5,0.14);
		legendary.setLocation(0.655,0.34,0.17);
		militaryReserves.setLocation(0.25,0.60,0.06);
		cultureReserves.setLocation(0.25,0.64,0.06);
		cashReserves.setLocation(0.25,0.68,0.06);
		votesReserves.setLocation(0.25,0.74,0.06);
		grandeReserves.setLocation(0.2,0.6,0.1);
		meepleReserves.setLocation(0.2,0.7,0.1);
		brownBenefits.setLocation(0.13,0.875,0.24);
		redBenefits.setLocation(0.38,0.875,0.24);
		blueBenefits.setLocation(0.625,0.875,0.24);
		yellowBenefits.setLocation(0.87,0.875,0.24);
		playedStratCards.setLocation(0.085,0.66,0.1);
		PendulumCell.setHLocation(militaryVP,0.034,1.01, 0.09, 0.08);
		PendulumCell.setHLocation(prestigeVP,0.038,1.01, 0.165, 0.08);
		PendulumCell.setHLocation(popularityVP,0.038,1.01, 0.24, 0.08);
	}
	
	public Rectangle bbox = null;
	public void setDisplayRectangle(Rectangle r) { bbox = r; }
	public int cellToX(PendulumCell c)
	{	G.Assert(c.col=='A'+gameIndex,"should be my cell");
		return G.interpolate(c.posx,G.Left(bbox),G.Right(bbox));
	}
	public int cellToY(PendulumCell c)
	{	G.Assert(c.col=='A'+gameIndex,"should be my cell");
		return G.interpolate(c.posy,G.Top(bbox),G.Bottom(bbox));
	}
	public int cellSize(PendulumCell c) 
	{	G.Assert(c.col=='A'+gameIndex,"should be my cell");
		return (int)(c.scale*G.Width(bbox)); 
	}
	
	PendulumCell pickedSource = null;
	PendulumCell droppedDest = null;
	int pickedIndex = -1;
	PendulumChip pickedObject = null;
	PendulumChip droppedObject = null;
	PendulumState dropState = null;
	
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    public void pickObject(PendulumCell c,int idx)
    {	
    	pickedSource = c;
    	pickedIndex = idx;
    	pickedObject = idx==-1 ? c.removeTop() : c.removeChipAtIndex(idx); 
    }
    public void unPickObject()
    {	if(pickedIndex==-1) { pickedSource.addChip(pickedObject); }
    	else { pickedSource.insertChipAtIndex(pickedIndex,pickedObject); }
    	pickedObject = null;
    	pickedSource = null;
    	pickedIndex = -1;
    }
    public void dropObject(PendulumCell c,PendulumState state)
    {	droppedDest = c;
    	droppedObject = pickedObject;
    	dropState = state;
    	c.addChip(pickedObject);
    	pickedObject = null;
    }
    public PendulumState unDropObject()
    {
    	PendulumState ds = dropState;
    	pickedObject = droppedDest.removeChip(droppedObject);
    	droppedDest = null;
    	droppedObject = null;
    	dropState = null;
    	return ds;
    }
	public void acceptPlacement() {
		pickedSource = droppedDest = null;
		droppedObject = pickedObject = null;
		dropState = null;
		pickedIndex = -1;
		
	}
	public boolean isDest(PendulumCell c) { return c==droppedDest; }
	public boolean isSource(PendulumCell c) { return c==pickedSource;}

	public boolean canPayCost(BC cost) 
	{
		switch(cost)
		{
		case None:	return true;
		case D2Board: return cash.height()>=2 || !freeD2.isEmpty();
		case M4Board: return military.height()>=4;
		case Achievement:
			{
			PendulumChip ch = parent.currentAchievement.chipAtIndex(0);
			return canPayCost(ch.pc);
			}
		case PerCard:
			return canPayCost(droppedObject.pc);
		default: throw G.Error("Not expecting cost %s",cost);
		}
	}
	private int totalResources()
	{
		return military.height()+culture.height()+cash.height();
	}
	public boolean canPayCost(PC cost)
	{
		switch(cost)
		{
		case None:	return true;
		case M2: return military.height()>=2;
		case M3: return military.height()>=3;
		case M5: return military.height()>=5;
		case M7: return military.height()>=7;
		case V2: return votes.height()>=2;
		case V3: return votes.height()>=3;
		case V4: return votes.height()>=4;
		case C1: return culture.height()>=1;
		case C2: return culture.height()>=2;
		case C3: return culture.height()>=3;
		case C5: return culture.height()>=5;
		case C4V2: return culture.height()>=4 && votes.height()>=2;
		case C7: return culture.height()>=7;
		case D2: return cash.height()>=2;
		case D5: return cash.height()>=5;
		case D7: return cash.height()>=7;
		case Pow2: return militaryVPvalue>=2;
		case M4D4: return military.height()>=4 && cash.height()>=4;
		case R2: return totalResources()>=2;
		case R8: return totalResources()>=8;
		case R10: return totalResources()>=10;

		case C4D4V3: return culture.height()>=4 && cash.height()>=4 && votes.height()>=3;
		case M6XC2V3: return military.height()>=6 && culture.height()>=2 && votes.height()>=3;
		case M2C6V3: return military.height()>=2 && culture.height()>=6 && votes.height()>=3;
		case R12V3: return totalResources()>=12 && votes.height()>=3;
		case M8V3: return military.height()>=8 && votes.height()>=3;
		case D8V3: return cash.height()>=8 && votes.height()>=3;
		case D3M3C3V3: return cash.height()>=3 && military.height()>=3 && culture.height()>=3 && votes.height()>=3;
		case M4C4V3: return military.height()>=4 && culture.height()>=4 && votes.height()>=3;
		case M4D4V3: return military.height()>=4 && cash.height()>=4 && votes.height()>=3;
		case C8V3: return culture.height()>=8 && votes.height()>=3;
		case MeepleAndGrande: return grandeReserves.height()>=1;
		case NoMax3: return max3Cards.isEmpty();
		default: throw G.Error("Not expecting cost %s",cost);
		}
	}
	public void payCost(PendulumCell c,PC cost,replayMode replay)
	{
		switch(cost)
		{
		case MeepleAndGrande:	// no cost
		case None:	
			break;
		case M2:
			transfer(2,military,militaryReserves,replay);
			break;			
		case M3:
			transfer(3,military,militaryReserves,replay);
			break;			
		case M5:
			transfer(5,military,militaryReserves,replay);
			break;			
		case M7: 
			transfer(7,military,militaryReserves,replay);
			break;
		case V2:
			transfer(2,votes,votesReserves,replay);
			break;
		case V3: 
			transfer(3,votes,votesReserves,replay);
			break;
		case V4: 
			transfer(4,votes,votesReserves,replay);
			break;
			
		case C1: 
			transfer(1,culture,cultureReserves,replay);
			break;
		case C2: 
			transfer(2,culture,cultureReserves,replay);
			break;
		case C3:
			transfer(3,culture,cultureReserves,replay);
			break;
		case C5:
			transfer(5,culture,cultureReserves,replay);
			break;

		case C4V2: 
			transfer(4,culture,cultureReserves,replay);
			transfer(2,votes,votesReserves,replay);
			break;
		case C7: 
			transfer(7,culture,cultureReserves,replay);
			break;

		case D2:
			transfer(2,cash,cashReserves,replay);
			break;

		case D5:
			transfer(5,cash,cashReserves,replay);
			break;

		case D7:
			transfer(7,cash,cashReserves,replay);
			break;
		case Pow2: 
			setMilitaryVP(militaryVPvalue-2,replay);
			break;
		case M4D4: 
			transfer(4,military,militaryReserves,replay);
			transfer(4,cash,cashReserves,replay);
			break;
		case R2: 
			setUIState(UIState.PayResources,2);
			break;
		case R8: 
			setUIState(UIState.PayResources,8);
			break;
		case R10: 
			setUIState(UIState.PayResources,10);
			break;
		default: throw G.Error("Not expecting cost %s",cost);
		}
	}
	private Object effectiveCost(PendulumCell c)
	{
		if((c.cost==BC.PerCard) && droppedObject!=null)
		{
			return droppedObject.pc;
		}
		else { return c.cost; }
	}
	public void payCost(PendulumCell c, replayMode replay) {
		G.Assert(canPayCost(c.cost),"can't pay cost #1",effectiveCost(c));
		switch(c.cost){
		default: throw G.Error("Not expecting cost %s",c.cost);
		case Achievement:	// no actual cost, but must have the named resources.
			break;
		case None:
			break;
		case PerCard:
			payCost(c,droppedObject.pc,replay);
			break;
		case M4Board:
			for(int i=0;i<4;i++)
			{
				militaryReserves.addChip(military.removeTop());
				if(replay.animate) { parent.animate(military,militaryReserves); }
			}
			break;
		case D2Board: 
			if(freeD2.isEmpty())
			{
			transfer(2,cash,cashReserves,replay);
			}
			else
			{
			transfer(1,freeD2,playedStratCards,replay);
			}
			break;
		}
		
	}
	// transfer, insist that the items are available (used for payments)
	public void transfer(int n,PendulumCell from,PendulumCell to,replayMode replay)
	{	G.Assert(from.height()>=n,"not enough to transfer");
		for(int i=0;i<n;i++)
		{
			to.addChip(from.removeTop());
			if(replay.animate) { parent.animate(from,to); }
		}
	}
	
	// transfer, up to the number but stop quietly if not enough are available. (used for adding resources)
	public void transferIfAvailable(int n,PendulumCell from,PendulumCell to,replayMode replay)
	{	for(int i=0;i<n;i++)
		{
		if(from.height()>0) 
		{ to.addChip(from.removeTop());
		  if(replay.animate) { parent.animate(from,to); }
		}}
	}
	// transfer, add new items if we run out (used for adding votes)
	public void transferAlwaysAvailable(int n,PendulumCell from,PendulumCell to,replayMode replay)
	{	for(int i=0;i<n;i++)
		{
		if(from.height()==0) { from.addChip(to.topChip()); } 
		to.addChip(from.removeTop());
		if(replay.animate) { parent.animate(from,to); }
		}
	}
	public void collectProvinceBenefits(PendulumCell c,replayMode replay)
	{	collectBenefit(c,c.pb,replay);
		PendulumId rack = c.rackLocation();
		for(int lim=c.height()-1; lim>=0; lim--)
		{
			PendulumChip ch = c.chipAtIndex(lim);
			PB benefits[] = ch.pb;
			switch(rack)
			{
			case PlayerRedBenefits:
				collectBenefit(c,benefits[1],replay);
				break;
			case PlayerBlueBenefits:
				collectBenefit(c,benefits[3],replay);
				break;
			case PlayerYellowBenefits:
				collectBenefit(c,benefits[0],replay);
				break;
			case PlayerBrownBenefits:
				collectBenefit(c,benefits[2],replay);
				break;
				
			default: throw G.Error("Not expecting %c",rack);
			}
			
		}
	}
	public void setUIState(UIState d)
	{	setUIState(d,0);
	}
	public void setUIState(UIState d,int n)
	{
		uiCount = n+1;	// +1 so the first setNextState does nothing
		uiState = d;
	}
	private void setCollectResources(int nn)
	{	int n = Math.min(nn,MAX_AVAILABLE_RESOURCES-totalResources());
		if(n>0)
		{
			setUIState(UIState.CollectResources,n); 
		}
	}

	public void collectBenefit(PendulumCell c,PB benefit,replayMode replay)
	{
		switch(benefit)
		{
		case None: break;
		case Pres1:
			setPrestigeVP(prestigeVPvalue+1,replay);
			break;
		case Pres3:
			setPrestigeVP(prestigeVPvalue+3,replay);
			break;
		case Legendary:
			legendary.addChip(PendulumChip.legendary);
			break;
		case Pop1:
			setPopularityVP(popularityVPvalue+1,replay);
			break;
		case Pop3:
			setPopularityVP(popularityVPvalue+3,replay);
			break;
		case Pow1:
			setMilitaryVP(militaryVPvalue+1,replay);
			break;
			
		case Pow3:
			setMilitaryVP(militaryVPvalue+3,replay);
			break;
		case SwapVotes:
			// TODO: consider the bizarre case where no votes have been earned yet.
			setUIState(UIState.SwapVotes,1);
			break;
		case Retrieve:
			if(parent.hasRetrieveWorkerMoves(boardIndex)) { setUIState(UIState.RetrieveWorker,1); }
			break;
			
		case Province:
			setUIState(UIState.Province);
			break;
		case D1:
			transferIfAvailable(1,cashReserves,cash,replay);
			break;
		case D2:
			transferIfAvailable(2,cashReserves,cash,replay);
			break;
		case D3:
			transferIfAvailable(3,cashReserves,cash,replay);
			break;
			
		case R1:
			setCollectResources(1);
			break;
		case R3:
			setCollectResources(3);
			break;
		case R4:
			setCollectResources(4);
			break;
		case R5:
			if(hasCollectResourceMoves()) { setCollectResources(5); }
			break;
		case M1:
			transferIfAvailable(1,militaryReserves,military,replay);
			break;
		case M2:
			transferIfAvailable(2,militaryReserves,military,replay);
			break;
		case M3:
			transferIfAvailable(3,militaryReserves,military,replay);
			break;
		case M4:
			transferIfAvailable(4,militaryReserves,military,replay);
			break;
		case M3D2:
			transferIfAvailable(3,militaryReserves,military,replay);
			transferIfAvailable(3,cashReserves,cash,replay);
			break;
		case V1:
			transferIfAvailable(1,votesReserves,votes,replay);
			break;
		case V2:
			transferIfAvailable(2,votesReserves,votes,replay);
			break;
		case V3:
			transferIfAvailable(3,votesReserves,votes,replay);
			break;
		case C1:
			transferIfAvailable(1,cultureReserves,culture,replay);
			break;
		case C2:
			transferIfAvailable(2,cultureReserves,culture,replay);
			break;
		case C3:
			transferIfAvailable(3,cultureReserves,culture,replay);
			break;
		case C4:
			transferIfAvailable(4,cultureReserves,culture,replay);
			break;
		case C5:
			transferIfAvailable(5,cultureReserves,culture,replay);
			break;
		case FreeD2:
			transfer(1,c,freeD2,replay);
			break;
		case Max3:
			transfer(1,c,max3Cards,replay);
			break;		
		case RetrieveAll:
			transfer(playedStratCards.height(),playedStratCards,stratCards,replay);
			break;
		case M2C2D2:
			transferIfAvailable(2,militaryReserves,military,replay);
			transferIfAvailable(2,cultureReserves,culture,replay);
			transferIfAvailable(2,cashReserves,cash,replay);
			break;
		case BluePB:
			collectProvinceBenefits(blueBenefits,replay);
			break;
		case RedPB:
			collectProvinceBenefits(redBenefits,replay);
			break;
		case Pow1Pres1Pop1:
			setMilitaryVP(militaryVPvalue+1,replay);
			setPrestigeVP(prestigeVPvalue+1,replay);
			setPopularityVP(popularityVPvalue+1,replay);
			break;
		case Pow1Pres1:
			setMilitaryVP(militaryVPvalue+1,replay);
			setPrestigeVP(prestigeVPvalue+1,replay);
			break;
		case Pow1Pop1:
			setMilitaryVP(militaryVPvalue+1,replay);
			setPopularityVP(popularityVPvalue+1,replay);
			break;
		case Pres1Pop1:
			setPrestigeVP(prestigeVPvalue+1,replay);
			setPopularityVP(popularityVPvalue+1,replay);
			break;
		case D5:
			transferIfAvailable(5,cashReserves,cash,replay);
			break;
		case Recruit:
			if(meepleReserves.height()>0)
			{
			transferIfAvailable(1,meepleReserves,meeples,replay);
			}
			break;
		case V5:
			transferAlwaysAvailable(5,votesReserves,votes,replay);
			break;
		case Grande:
			// if there are 2 meeples deployed, enter a UI state to designate which one
			// if there is 1 deployed, find and swap automatically
			// also flip the card so it can't be chosen again
			throw G.Error("Not implemented");
		default: 
			throw G.Error("Not expecting benefit %s",benefit);
		}
	}
	public void collectBenefit(PendulumCell c, replayMode replay) {

		switch(c.benefit)
		{
		default: throw G.Error("Not expecting benefit #1",c.benefit);
		case Achievement:
			{
			int index = c.findChip(PendulumChip.legendary);
			if(index>=0  && legendary.isEmpty())
			{
			setUIState(UIState.AchievementOrLegandary,1);	
			}
			else
			{
			collectBenefit(c,c.chipAtIndex(0).pb[0],replay);
			}}
			break;
		case PerCard:
			collectBenefit(c,droppedObject.pb[0],replay);
			break;
		case Popularity1Prestige1Vote1:
			setPopularityVP(popularityVPvalue+1,replay);
			setPrestigeVP(prestigeVPvalue+1,replay);
			transferAlwaysAvailable(1,votesReserves,votes,replay);
			break;
		case Province:
			setUIState(UIState.Province,1);
			break;
		case Resource1:
			setUIState(UIState.CollectResources,1);
			break;
		case Vote1:
			transferAlwaysAvailable(1,votesReserves,votes,replay);
			break;
		case Culture2:
			setPopularityVP(popularityVPvalue+2,replay);
			break;
		case YellowPB:
			collectProvinceBenefits(yellowBenefits,replay);
			break;
		case RedPB:
			collectProvinceBenefits(redBenefits,replay);
			break;
		case BluePB:
			collectProvinceBenefits(blueBenefits,replay);
			break;
		case BrownPB:
			collectProvinceBenefits(brownBenefits,replay);
			break;
		case Military1Vote2:
			setMilitaryVP(militaryVPvalue+1,replay);
			transferAlwaysAvailable(2,votesReserves,votes,replay);
			break;
		case None: break;
		}
		acceptPlacement();
		
	}

	public boolean isUIQuiet() {
		return (uiState==UIState.Normal) && pickedObject==null;
	}
	
	 boolean hasCollectResourceMoves()
	 {
		 return addCollectResourceMoves(null);
	 }
	 
	 public boolean addCollectResourceMoves(CommonMoveStack all)
	 {	 boolean some = (pickedObject==null || pickedSource==militaryReserves)
		 					? addCollectResourceMoves(all,militaryReserves,military)
		 					: false;
		 some |= (pickedObject==null || pickedSource==cultureReserves)
				 			? addCollectResourceMoves(all,cultureReserves,culture)
				 			: false;
		 some |= (pickedObject==null || pickedSource==cashReserves)
				 	? addCollectResourceMoves(all,cashReserves,cash)
				 	: false;
		 return some;
	 }
	 
	 public boolean addCollectProvinceMoves(CommonMoveStack all)
	 {	
		 return addCollectProvinceMoves(all,parent.provinces);
	 }
	 private boolean addCollectProvinceMoves(CommonMoveStack all,PendulumCell from[])
	 {	boolean some = false;
	 	for(PendulumCell c : from)
		 	{	if((c.topChip()!=null) || (pickedSource==c && pickedObject!=null))
		 		{
		 		some |= addCollectProvinceMovesTo(all,c);
		 		}
		 	}
		 return some;
	 }
	 private boolean addCollectProvinceMovesTo(CommonMoveStack all,PendulumCell from)
	 {
	 		if(all!=null)
	 			{ all.push(new PendulumMovespec(MOVE_FROM_TO,from,-1,yellowBenefits,boardIndex));
	 			  all.push(new PendulumMovespec(MOVE_FROM_TO,from,-1,brownBenefits,boardIndex));
	 			  all.push(new PendulumMovespec(MOVE_FROM_TO,from,-1,redBenefits,boardIndex));
	 			  all.push(new PendulumMovespec(MOVE_FROM_TO,from,-1,blueBenefits,boardIndex));
	 			}
	 		return true;
	 }
 
	 private boolean addCollectResourceMoves(CommonMoveStack all,PendulumCell from,PendulumCell to)
	 {
		 if(from.height()>0)
		 {
			if(all!=null) { all.push(new PendulumMovespec(MOVE_FROM_TO,from,-1,to,boardIndex)); }
			return true;
		 }
		 return false;
	 }
	 public void setNextStateAfterDrop(replayMode replay)
	 {
		 switch(uiState)
     		{
     		case Normal: break;
     		case RetrieveWorker:
     		case Province:
     		case CollectResources:
     		case AchievementOrLegandary:
     			uiCount--;
     			if(!hasCollectResourceMoves()) { uiCount = 0; }	// ran out of resources
     			if(uiCount<=0) { setUIState(UIState.Normal); }
     			break;
     		default: G.Error("Not expecting %s",uiState);
     		}
	 }
	 public void doDrop(PendulumCell c,PendulumState state,replayMode replay)
	 {
		dropObject(c,state); 
		payCost(c,replay);
		collectBenefit(c,replay);
		acceptPlacement();
	 }

	public void addPlayStrategem(CommonMoveStack all, int who) 
	{
		if((pickedSource!=null && pickedSource.rackLocation()==PendulumId.PlayerStratCard))
		{	if(canPayCost(pickedObject.pc))
				{
				all.push(new PendulumMovespec(MOVE_FROM_TO,stratCards,pickedIndex,playedStratCards,boardIndex));
				}
		}
		else
		{
		for(int lim=stratCards.height()-1; lim>=0; lim--)
		{
			PendulumChip ch = stratCards.chipAtIndex(lim);
			PC cost =ch.pc;
			if(canPayCost(cost))
			{
				all.push(new PendulumMovespec(MOVE_FROM_TO,stratCards,lim,playedStratCards,boardIndex));
			}
		}}
	}
}
