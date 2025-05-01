package pendulum;

import static pendulum.PendulumMovespec.*;

import java.awt.Rectangle;
import java.io.PrintStream;

import lib.CompareTo;
import lib.Digestable;
import lib.G;
import lib.Random;
import online.game.CommonMoveStack;
import online.game.replayMode;

public class PlayerBoard implements PendulumConstants,Digestable,CompareTo<PlayerBoard>
{
	int boardIndex = 0;
	int gameIndex = 0;
	int matIndex = 0;
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
	PendulumCell votes = newcell(PendulumId.PlayerVotes);
	PendulumCell councilVotes = new PendulumCell(PendulumId.PlayerVotes);
	private PendulumCell grandeReserves = newcell(PendulumId.PlayerGrandeReserves);
	private PendulumCell meepleReserves = newcell(PendulumId.PlayerMeepleReserves);
	PendulumCell legendary = newcell(PendulumId.PlayerLegendary);
	private PendulumCell militaryReserves = newcell(PendulumId.PlayerMilitaryReserves);
	private PendulumCell cultureReserves = newcell(PendulumId.PlayerCultureReserves);
	private PendulumCell cashReserves = newcell(PendulumId.PlayerCashReserves);
	private PendulumCell votesReserves = newcell(PendulumId.PlayerVotesReserves);
	private PendulumCell max3Cards = newcell(PendulumId.PlayerMax3Cards);
	private PendulumCell freeD2 = newcell(PendulumId.PlayerFreeD2Card);
	
	static int MAXVPVALUE = 21;
	private PendulumCell powerVP[] = newcell(PendulumId.PlayerPowerVP,MAXVPVALUE+1);
	private PendulumCell prestigeVP[] = newcell(PendulumId.PlayerPrestigeVP,MAXVPVALUE+1);
	private PendulumCell popularityVP[] = newcell(PendulumId.PlayerPopularityVP,MAXVPVALUE+1);
	
	private PendulumCell blueBenefits = newcell(PendulumId.PlayerBlueBenefits);
	private PendulumCell yellowBenefits = newcell(PendulumId.PlayerYellowBenefits);
	private PendulumCell redBenefits = newcell(PendulumId.PlayerRedBenefits);
	private PendulumCell brownBenefits = newcell(PendulumId.PlayerBrownBenefits);
	PendulumCell tucked[] = { blueBenefits, yellowBenefits, redBenefits, brownBenefits};
	private int powerVPvalue = 0;
	private int prestigeVPvalue = 0;
	private int popularityVPvalue = 0;
	private int totalRecruits = 0;
	// stats
	int blackActions = 0;
	int greenActions = 0;
	int purpleActions = 0;
	int blackMissedActions = 0;
	int greenMissedActions = 0;
	int purpleMissedActions = 0;
	int yellowBenefitCount = 0;
	int blueBenefitCount = 0;
	int brownBenefitCount = 0;
	int redBenefitCount = 0;
	int yellowBenefitMultiplier = 0;
	int blueBenefitMultiplier = 0;
	int brownBenefitMultiplier = 0;
	int redBenefitMultiplier = 0;
	
	public int uiCount = 0;
	public UIState uiState = UIState.Normal;
	public PendulumCell refill = newcell(PendulumId.PlayerRefill,BC.C5Board,BB.Reload);
	public String toString() { return "<pb "+color+" "+uiState+">"; }
	
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
		matIndex = gameIndex = ind;
		for(PendulumCell c = allCells; c!=null; c=c.next) { c.col = (char)('A'+gameIndex); }
		meeple = PendulumChip.chips[ind];
		grande = PendulumChip.bigchips[ind];
		mat = PendulumChip.mats[ind];
		hexagon = PendulumChip.hexes[ind];
		cylinder = PendulumChip.cylinders[ind];
		beginnerMat = mat;
		advancedMat = PendulumChip.advancedmats[ind];
	}
	public void setMat(int ind)
	{
		beginnerMat = PendulumChip.mats[ind];
		advancedMat = PendulumChip.advancedmats[ind];
		if(parent.revision>=102)
		{
			matIndex = ind;
		}
		else 
		{
			matIndex = gameIndex;
		}
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
		
		v ^= parent.Digest(r,powerVP);
		v ^= parent.Digest(r,prestigeVP);
		v ^= parent.Digest(r,popularityVP);
		v ^= parent.Digest(r,powerVPvalue);
		v ^= parent.Digest(r,prestigeVPvalue);
		v ^= parent.Digest(r,popularityVPvalue);
		v ^= parent.Digest(r,totalRecruits);
		v ^= parent.Digest(r,pickedSource);
		v ^= parent.Digest(r,selectedCell);
		v ^= parent.Digest(r,selectedCell2);
		v ^= parent.Digest(r,droppedDest);
		v ^= parent.Digest(r,pickedObject);
		v ^= parent.Digest(r,droppedObject);
		v ^= parent.Digest(r,dropState);
		v ^= parent.Digest(r,dropPair);
		v ^= parent.Digest(r,dropStateCount);
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
		
		int startIndex = advanced ? matIndex*8+4 : matIndex*8;
		for(int i=0;i<4;i++)
		{	
			stratCards.addChip(PendulumChip.stratcards[startIndex+i]);
		}
		brownBenefits.pb = mat.pb[0];
		yellowBenefits.pb = mat.pb[3];
		blueBenefits.pb = mat.pb[2];
		redBenefits.pb = mat.pb[1];
		
		grandes.addChip(grande);
		meeples.addChip(meeple);
		totalRecruits = 2;
		
		grandeReserves.addChip(grande);
		meepleReserves.addChip(meeple);
		meepleReserves.addChip(meeple);
		pickedSource = droppedDest = selectedCell = selectedCell2 = null;
		pickedObject = droppedObject = null;
		dropState = null;
		dropPair = false;
		dropStateCount = 0;
		pickedIndex = -1;
		setUIState(UIState.Normal,0);
		
		int resources[] = mat.resources;
		int initialvp[] = mat.vps;
		setPowerVP(initialvp[0],replayMode.Replay);
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
		refill.addChip(PendulumChip.Refill);
		
		// stats
		blackActions = 0;
		greenActions = 0;
		purpleActions = 0;
		blackMissedActions = 0;
		greenMissedActions = 0;
		purpleMissedActions = 0;
		yellowBenefitCount = 0;
		brownBenefitCount = 0;
		blueBenefitCount = 0;
		redBenefitCount = 0;
		yellowBenefitMultiplier = 0;
		brownBenefitMultiplier = 0;
		blueBenefitMultiplier = 0;
		redBenefitMultiplier = 0;
	}
	
	private void setPopularityVP(int i,replayMode replay) {
		int oldval = popularityVPvalue;
		int newval = Math.max(0,Math.min(i,MAXVPVALUE));
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
		int newval = Math.max(0,Math.min(i,MAXVPVALUE));
		if(oldval!=newval || prestigeVP[oldval].isEmpty())
		{
		prestigeVPvalue = newval;
		prestigeVP[oldval].reInit();
		prestigeVP[newval].addChip(PendulumChip.bluePost);
		if(replay.animate) { parent.animate(prestigeVP[oldval],prestigeVP[newval]); }
		}
	}
	private void setPowerVP(int i,replayMode replay) {
		int oldval = powerVPvalue;
		int newval = Math.max(0,Math.min(i,MAXVPVALUE));
		if(newval!=oldval || powerVP[oldval].isEmpty())
		{
			powerVPvalue = newval;
			powerVP[oldval].reInit();
			powerVP[newval].addChip(PendulumChip.redPost);
			if(replay.animate) { parent.animate(powerVP[oldval],powerVP[newval]); }
		}
	}
	public void copyFrom(PlayerBoard other) {
		stratCards.copyFrom(other.stratCards);
		refill.copyFrom(other.refill);
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
		councilVotes.copyFrom(other.councilVotes);
		parent.copyFrom(powerVP,other.powerVP);
		parent.copyFrom(prestigeVP,other.prestigeVP);
		parent.copyFrom(popularityVP,other.popularityVP);
		
		brownBenefits.copyFrom(other.brownBenefits);
		blueBenefits.copyFrom(other.blueBenefits);
		yellowBenefits.copyFrom(other.yellowBenefits);
		redBenefits.copyFrom(other.redBenefits);
		uiState = other.uiState;
		uiCount = other.uiCount;
		powerVPvalue = other.powerVPvalue;
		prestigeVPvalue = other.prestigeVPvalue;
		popularityVPvalue = other.popularityVPvalue;
		totalRecruits = other.totalRecruits;
		pickedSource = parent.getCell(other.pickedSource);
		selectedCell = parent.getCell(other.selectedCell);
		selectedCell2 = parent.getCell(other.selectedCell2);
		droppedDest = parent.getCell(other.droppedDest);
		pickedObject = other.pickedObject;
		droppedObject = other.droppedObject;
		dropState = other.dropState;
		dropPair = other.dropPair;
		dropStateCount = other.dropStateCount;
		pickedIndex = other.pickedIndex;
		
		// stats
		blackActions = other.blackActions;
		greenActions = other.greenActions;
		purpleActions = other.purpleActions;
		blackMissedActions = other.blackMissedActions;
		greenMissedActions = other.greenMissedActions;
		purpleMissedActions = other.purpleMissedActions;
		yellowBenefitCount = other.yellowBenefitCount;
		brownBenefitCount = other.brownBenefitCount;
		blueBenefitCount = other.blueBenefitCount;
		redBenefitCount = other.redBenefitCount;
		yellowBenefitMultiplier = other.yellowBenefitMultiplier;
		brownBenefitMultiplier = other.brownBenefitMultiplier;
		blueBenefitMultiplier = other.blueBenefitMultiplier;
		redBenefitMultiplier = other.redBenefitMultiplier;

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
		
		G.Assert(parent.sameContents(powerVP,other.powerVP),"powerVP mismatch");
		G.Assert(parent.sameContents(prestigeVP,other.prestigeVP),"prestigeVP mismatch");
		G.Assert(parent.sameContents(popularityVP,other.popularityVP),"popularityVP mismatch");
		G.Assert(powerVPvalue==other.powerVPvalue,"powerVPvalue mismatch");
		G.Assert(prestigeVPvalue==other.prestigeVPvalue,"prestigeVPvalue mismatch");
		G.Assert(popularityVPvalue==other.popularityVPvalue,"popularityVPvalue mismatch");
		G.Assert(totalRecruits==other.totalRecruits,"totalRecruits mismatch");
		G.Assert(parent.sameCells(pickedSource,other.pickedSource),"pickedSource mismatch");
		G.Assert(parent.sameCells(selectedCell,other.selectedCell),"selectedCell mismatch");
		G.Assert(parent.sameCells(selectedCell2,other.selectedCell2),"selectedCell2 mismatch");
		G.Assert(parent.sameCells(droppedDest,other.droppedDest),"droppedDest mismatch");
		G.Assert(pickedObject==other.pickedObject,"pickedObject mismatch");
		G.Assert(droppedObject==other.droppedObject,"droppedObject mismatch");
		G.Assert(dropState==other.dropState,"dropState mismatch");
		G.Assert(dropPair==other.dropPair,"dropPair mismatch");
		G.Assert(pickedIndex==other.pickedIndex,"pickedIndex mismatch");
		G.Assert(uiState==other.uiState,"uiState mismatch");
		G.Assert(uiCount==other.uiCount,"uiCount mismatch");
	}
	
	public PendulumCell getCell(PendulumId id,int idx)
	{
		switch(id)
		{
		case PlayerRefill: return refill;
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
		case PlayerPowerVP: return powerVP[idx];
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
		refill.setLocation(1.5,1.5,0.164, 0.4, 0.055);
		stratCards.setLocation(0,0,0.28, 0.4, 0.1);
		grandes.setLocation(0,0,0.63, 0.48, 0.14);
		meeples.setLocation(0,0,0.68, 0.48, 0.14);
		military.setLocation(0,0,0.6, 0.7, 0.14);
		culture.setLocation(0,0,0.76, 0.7, 0.14);
		cash.setLocation(0,0,0.90, 0.7, 0.14);
		votes.setLocation(0,0,0.84, 0.48, 0.12);
		max3Cards.setLocation(0,0,0.1, 0.5, 0.14);
		freeD2.setLocation(0,0,0.0, 0.5, 0.14);
		legendary.setLocation(0,0,0.655, 0.34, 0.17);
		militaryReserves.setLocation(0,0,0.25, 0.60, 0.06);
		cultureReserves.setLocation(0,0,0.25, 0.64, 0.06);
		cashReserves.setLocation(0,0,0.25, 0.68, 0.06);
		votesReserves.setLocation(0,0,0.25, 0.74, 0.06);
		grandeReserves.setLocation(0,0,0.2, 0.6, 0.1);
		meepleReserves.setLocation(0,0,0.2, 0.7, 0.1);
		brownBenefits.setLocation(0,1,0.13, 0.78, 0.24);
		redBenefits.setLocation(0,1,0.38, 0.78, 0.24);
		blueBenefits.setLocation(0,1,0.625, 0.78, 0.24);
		yellowBenefits.setLocation(0,1,0.87, 0.78, 0.24);
		playedStratCards.setLocation(0,0,0.085, 0.66, 0.1);
		PendulumCell.setHLocation(0,0,powerVP, 0.034, 1.01, 0.09, 0.1);
		PendulumCell.setHLocation(0,0,prestigeVP, 0.038, 1.01, 0.165, 0.1);
		PendulumCell.setHLocation(0,0,popularityVP, 0.038, 1.01, 0.24, 0.1);
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
	
	PendulumCell selectedCell = null;
	PendulumCell selectedCell2 = null;
	PendulumCell pickedSource = null;
	PendulumCell droppedDest = null;
	int pickedIndex = -1;
	PendulumChip pickedObject = null;
	PendulumChip droppedObject = null;
	UIState dropState = null;
	boolean dropPair = false;		// true if a drop is hard-paired with the pick. Ie; from a robot
	int dropStateCount = 0;
	
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    public boolean pickObject(PendulumCell c,PendulumChip item)
    {	G.Assert(c.col=='@' || c.col==(char)('A'+gameIndex),"not my cell %s",c);
    	int index = c.findChip(item);
    	if(index<0)
    	{	// if the item isn't found, it's because some other player has already
    		// picked it up.  The UI tries to avoid this, but it's inevitable.
    		Privilege result = parent.resolvePrivilege(this,item);
    		switch(result)
    		{
    		case Ignore:	return(false);
    		case Override:
    			index = c.findChip(item);
    			break;
    		case Error: break;
    		default: throw G.Error("Not expecting %s",result);
    		}
    	}
    	if(index>=0) 
    	{
    	pickedSource = c;
    	pickedIndex = index;
    	pickedObject = c.removeChipAtIndex(index); 
    	PColor co = pickedObject.color;
    	droppedDest = null;
    	droppedObject = null;
    	dropState = null;
    	dropPair = false;
    	dropStateCount = 0;
    	G.Assert(co==null || co==color || co==parent.neutralColor(),"mismatched color %s",co);
    	return true;	// succeeded
    	}
    	else
    	{	// this is the case where simultaneous access is attempted but not resolved
    		throw G.Error("item not found %s",item);
    	}
    }
    // undo an object pickup.  this ought to always be possible, but due to realtime interactions
    // it sometimes is not.
    public boolean unPickObject(PendulumMovespec m)
    {	
    	if(pickedSource.col=='@')
    	{
    		if(!parent.verifyUnpickOk(this,m,pickedSource,pickedObject)) 
    			{ return(false); 
    			}
    	}
    	// save this for the contingency of resolving privilege
    	pickedSource.dropper = m;
    	alwaysUnpickObject();
    	return true;
    }
    // unconditionally unpick
    public void alwaysUnpickObject()
    {    	
    	if(pickedIndex==-1 || pickedSource.height()<pickedIndex) 
    		{ pickedSource.addChip(pickedObject); 
    		}
    	else { pickedSource.insertChipAtIndex(pickedIndex,pickedObject); }
    	pickedObject = null;
    	pickedSource = null;
    	pickedIndex = -1;
    }
    
    // drop the picked-up item.  PickedSource remains, so use pickedObject!=null as the test for something moving.
    public boolean dropObject(PendulumCell c,PendulumMovespec m,boolean pair)
    {	G.Assert(c.col=='@' || c.col==(char)('A'+gameIndex),"not my cell %s",c);
    	if(c.col=='@')
    	{
    		if(!parent.verifyDropOk(this,m,c,pickedObject)) { return(false); }
    	}
    	droppedDest = c;
    	dropPair = pair;
    	droppedObject = pickedObject;
    	dropState = uiState;
    	dropStateCount = uiCount;
    	c.addChip(pickedObject);
    	c.dropper = m;
    	pickedObject = null;
    	return true;
    }
    public PendulumCell selectCell(PendulumCell c,replayMode replay)
    {	PendulumCell wasSelected = selectedCell;
    	int amount = 1;
    	if(c==selectedCell) { selectedCell=null;  }
    	else { selectedCell = c; }
    	switch(uiState)
    	{
    	case P1P1P1Twice:
    		selectedCell = wasSelected;
    		if(c==selectedCell) { selectedCell=selectedCell2; selectedCell2=null; }
    		else if (c==selectedCell2) { selectedCell2=null; }
    		else if(selectedCell==null) { selectedCell=c; }
    		else { selectedCell2=c; }
    		return selectedCell;
    		
    	case CollectResources:
    		amount = -1;
			//$FALL-THROUGH$
		case PayResources:
    		switch(c.rackLocation())
    		{
    		// "getTargets" transforms move_from_to to select for these moves,
    		// so the GUI can just flash the resources across instead of 
    		// requiring dragging
    		case PlayerCash: transfer(amount,cash,cashReserves,replay); break;
    		case PlayerMilitary: transfer(amount,military,militaryReserves,replay); break;
    		case PlayerCulture: transfer(amount,culture,cultureReserves,replay); break;
    		default: G.Error("Not expecting %s",c);
    		}
    		uiCount--;
    		selectedCell = null;
    		if(uiCount==0) { setUIStateNormal(); }
    		return null; 
    	case SwapVotes:
    		parent.p1("swap votes");
    		if(selectedCell!=null && wasSelected!=null)
    		{
    		changeVP(wasSelected,-1,replay);
    		changeVP(selectedCell,1,replay);
    		setUIStateNormal();
    		}
    		return null;
    	case P1P1P1Once:
    	case P1P1P1:
    	case P2P2P2:
    	default: 
    		return selectedCell;
    	}
    }
    // change one of the victory point values
    public void changeVP(PendulumCell c,int change,replayMode replay)
    {
    	switch(c.rackLocation())
		{
		case PlayerPowerVP: 
			setPowerVP(powerVPvalue+change,replay);
			break;
		case PlayerPrestigeVP:
			setPrestigeVP(prestigeVPvalue+change,replay);
			break;
		case PlayerPopularityVP:
			setPopularityVP(popularityVPvalue+change,replay);
			break;
		default: G.Error("Not expecting %s",c);
		}
    }
	public boolean isSelected(PendulumCell c) {
		return c==selectedCell || c==selectedCell2 ;
	}

	// reverse the dropping of an object.  As currently implemented, this can
	// only be done for droppings that did not have a direct cost or benefit
    public void unDropObject()
    {
    	uiState = (dropState);
    	pickedObject = droppedDest.removeChip(droppedObject);
    	uiCount = dropStateCount;
    	droppedDest = null;
    	droppedObject = null;
    	dropStateCount = 0;
    	if(dropPair) { alwaysUnpickObject(); }
    	dropPair = false;
    	dropState = null;
    }
    
    // finalize placement, can no longer pick up a dropped piece etc.
	public void acceptPlacement() {
		pickedSource = droppedDest = null;
		droppedObject = pickedObject = null;
		dropState = null;
		dropPair = false;
		dropStateCount = 0;
		pickedIndex = -1;
	}
	public void unSelect()
	{
		selectedCell = null;
	}
	public boolean isDest(PendulumCell c) { return c==droppedDest; }
	public boolean isSource(PendulumCell c) { return c==pickedSource;}

	public boolean canPayCost(BC cost,PendulumChip chip) 
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
			return canPayCost(chip.pc);
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
		case M2Retrieve: return military.height()>=2 && parent.hasRetrievableWorkers(boardIndex);
		case M3: return military.height()>=3;
		case M5: return military.height()>=5;
		case M7: return military.height()>=7;
		case M7Recruit: return military.height()>=7 && totalRecruits<4;
		case V2: return votes.height()>=2;
		case V3: return votes.height()>=3;
		case V3Recruit: return votes.height()>=3 && totalRecruits<4;
		case V4: return votes.height()>=4;
		case V4Recruit: return votes.height()>=4  && totalRecruits<4;
		case C1: return culture.height()>=1;
		case C2: return culture.height()>=2;
		case C2Retrieve: return culture.height()>=2 && parent.hasRetrievableWorkers(boardIndex);
		case C3: return culture.height()>=3;
		case C5: return culture.height()>=5;
		case C4V2: return culture.height()>=4 && votes.height()>=2;
		case C7: return culture.height()>=7;
		case C7Recruit:  return culture.height()>=7 && totalRecruits<4;
		case D2: return cash.height()>=2;
		case D5: return cash.height()>=5;
		case D7: return cash.height()>=7;
		case D7Recruit: return cash.height()>=7 && totalRecruits<4;
		case Pow2: return powerVPvalue>=2;
		case M4D4Recruit: return military.height()>=4 && cash.height()>=4  && totalRecruits<4;
		case R2: return totalResources()>=2;
		case R2Retrieve: return totalResources()>=2 && parent.hasRetrievableWorkers(boardIndex);
		case R8Recruit: return totalResources()>=8 && totalRecruits<4;
		case R10: return totalResources()>=10 && legendary.isEmpty();

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
		case Vote: return hasSwapVoteMoves();
		case CanRetrieve: return parent.hasRetrievableWorkers(boardIndex);
		default: throw G.Error("Not expecting cost %s",cost);
		}
	}
	//
	// this is separate from paycost so the costs that are capabilities
	// can be accounted for separately.  In particular if you're trying
	// to retrieve a meeple, but someone flips a timer and there is no
	// meeple to retrieve
	//
	public boolean costIsFree(PC cost)
	{
		switch(cost)
		{
		case Vote:	// no actual cost, there just have to be votes available to swap
		case CanRetrieve:
		case MeepleAndGrande:	// no cost
		case None:	
			return true;
		default: 
			return false;
		}
	}
	public boolean payCost(PendulumCell c,PC cost,replayMode replay)
	{	
		if(costIsFree(cost)) { return true ; }
		
		switch(cost)
		{
		
		case M2:
		case M2Retrieve:
			transfer(2,military,militaryReserves,replay);
			break;			
		case M3:
			transfer(3,military,militaryReserves,replay);
			break;			
		case M5:
			transfer(5,military,militaryReserves,replay);
			break;			
		case M7Recruit:	// these "recruit" variants are where you have to pau
						// and also have to be able to actually recruit a worker.
		case M7: 
			transfer(7,military,militaryReserves,replay);
			break;
		case V2:
			transfer(2,votes,votesReserves,replay);
			break;
		case V3: 
		case V3Recruit:
			transfer(3,votes,votesReserves,replay);
			break;
		case V4Recruit: // these "recruit" variants are where you have to pau
						// and also have to be able to actually recruit a worker.
		case V4:
			transfer(4,votes,votesReserves,replay);
			break;
			
		case C1: 
			transfer(1,culture,cultureReserves,replay);
			break;
		case C2: 
		case C2Retrieve:
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
		case C7Recruit:
			transfer(7,culture,cultureReserves,replay);
			break;

		case D2:
			transfer(2,cash,cashReserves,replay);
			break;

		case D5:
			transfer(5,cash,cashReserves,replay);
			break;
		case D7:
		case D7Recruit:
			transfer(7,cash,cashReserves,replay);
			break;
		case Pow2: 
		case Pow2Recruit:
			setPowerVP(powerVPvalue-2,replay);
			break;
		case M4D4Recruit: 
			transfer(4,military,militaryReserves,replay);
			transfer(4,cash,cashReserves,replay);
			break;
		case R2: 
		case R2Retrieve:
			setUIState(UIState.PayResources,2);
			break;
		case R8Recruit: 
			setUIState(UIState.PayResources,8);
			break;
		case R10: 
			setUIState(UIState.PayResources,10);
			break;
		default: throw G.Error("Not expecting cost %s",cost);
		}
		return false;
	}

	public void payCost(PendulumCell c, PendulumChip chip, replayMode replay) {
		boolean allowUndrop = false;
		switch(c.cost){
		default: throw G.Error("Not expecting cost %s",c.cost);
		case Achievement:	// no actual cost, but must have the named resources.
			allowUndrop = true;
			break;
		case None:
			allowUndrop = true;
			break;
		case C5Board:
			transfer(5,culture,cultureReserves,replay);
			break;
		case PerCard:
			{
			allowUndrop = payCost(c,chip.pc,replay);
			}
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
		if(!allowUndrop) 
		{ acceptPlacement(); 
		  unSelect();
		}
	}
	// transfer, insist that the items are available (used for payments)
	public void transfer(int n,PendulumCell from,PendulumCell to,replayMode replay)
	{	if(n<0) { transfer(-n,to,from,replay); }
		else
		{
		G.Assert(from.height()>=n,"not enough to transfer");
		for(int i=0;i<n;i++)
		{
			to.addChip(from.removeTop());
			if(replay.animate) { parent.animate(from,to); }
		}}
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
	{	collectBenefit(c,c.pb,false,replay);
		PendulumId rack = c.rackLocation();
		for(int lim=c.height()-1; lim>=0; lim--)
		{
			PendulumChip ch = c.chipAtIndex(lim);
			PB benefits[] = ch.pb;
			switch(rack)
			{
			case PlayerRedBenefits:
				collectBenefit(c,benefits[1],false,replay);
				redBenefitCount++;
				redBenefitMultiplier += c.height()+1;
				break;
			case PlayerBlueBenefits:
				collectBenefit(c,benefits[3],false,replay);
				blueBenefitCount++;
				blueBenefitMultiplier += c.height()+1;
				break;
			case PlayerYellowBenefits:
				collectBenefit(c,benefits[0],false,replay);
				yellowBenefitCount++;
				yellowBenefitMultiplier += c.height()+1;
				break;
			case PlayerBrownBenefits:
				collectBenefit(c,benefits[2],false,replay);
				brownBenefitCount++;
				brownBenefitMultiplier += c.height()+1;
				break;
				
			default: throw G.Error("Not expecting %c",rack);
			}
			
		}
	}
	public void setUIStateNormal()
	{	setUIState(UIState.Normal,0);
	}
	public void setUIState(UIState d,int n)
	{	if(pickedObject!=null) { G.print("\nshouldn't be moving ",pickedObject); }
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

	public boolean collectBenefit(PendulumCell c,PB benefit,boolean councilMode,replayMode replay)
	{	boolean allowUndo = false;
		switch(benefit)
		{
		case None: break;
		case P1P1P1:
			if(parent.revision>=101)
				{
				if(hasVPChoiceMoves()) { setUIState(UIState.P1P1P1,1); }
				}
			else
				{	// this was a bug, gave 1 of each
				setPowerVP(powerVPvalue+1,replay);
				setPrestigeVP(prestigeVPvalue+1,replay);
				setPopularityVP(popularityVPvalue+1,replay);
				}
			break;
		case P2P2P2:
			if(hasVPChoiceMoves())  { setUIState(UIState.P2P2P2,1);}
			break;
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
			setPowerVP(powerVPvalue+1,replay);
			break;
			
		case Pow3:
			setPowerVP(powerVPvalue+3,replay);
			break;
		case SwapVotes:
			setUIState(UIState.SwapVotes,1);
			allowUndo = true;
			break;
		case Retrieve:
			/*
			The "should be" problem where privilege is applicable is for
			instantaneous events, in this case 4 of the
			(1) pick up a card
			(2) drop the card
			(3) pick up a worker
			(4) drop a worker.
			
			Inbetween all these instantaneous events, a timer or timers might
			flip in such a way that the next event isn't legal, or changes 
			context in a way that makes the whole 1-4 sequence undesirable,
			pointless, or illegal.
			
			in interval 1-2 you can always put the card back.
			in interval 2-3 you can always un-drop and un-pickup the card
			in interval 3-4 you can always put the worker back on your player board.
			or back where it came from.
			*/
			if(parent.hasRetrieveWorkerMoves(boardIndex)) { setUIState(UIState.RetrieveWorker,1); }
			allowUndo = true;
			break;
			
		case ProvinceReward:
			setUIState(UIState.ProvinceReward,1);
			allowUndo = true;
			break;
		case Province:
			if(parent.hasProvincesAvailable()) { setUIState(UIState.Province,1); }
			allowUndo = true;
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
		case M5:
			transferIfAvailable(5,militaryReserves,military,replay);
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
		case V3Exactly: // for the neutral player
			// give back 
			transfer(votes.height(),votes,votesReserves,replay);
			//$FALL-THROUGH$
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
			setPowerVP(powerVPvalue+1,replay);
			setPrestigeVP(prestigeVPvalue+1,replay);
			setPopularityVP(popularityVPvalue+1,replay);
			break;
		case Pow1Pres1:
			setPowerVP(powerVPvalue+1,replay);
			setPrestigeVP(prestigeVPvalue+1,replay);
			break;
		case Pow1Pop1:
			setPowerVP(powerVPvalue+1,replay);
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
			totalRecruits++;
			transferIfAvailable(1,meepleReserves,meeples,replay);
			break;
		case V5:
			transferAlwaysAvailable(5,votesReserves,votes,replay);
			break;
		case Grande:
			// if there are 2 meeples deployed, enter a UI state to designate which one
			// if there is 1 deployed, find and swap automatically
			// also flip the card so it can't be chosen again
			setUIState(UIState.PromoteMeeple,1);
			allowUndo = true;
			break;
		default: 
			throw G.Error("Not expecting benefit %s",benefit);
		}
		return allowUndo;
	}
	public void collectBenefit(PendulumCell c, PendulumChip chip,replayMode replay) {
		boolean allowUndrop = false;
		
		// collect stats on actions taken
		switch(c.rackLocation())
		{
		case GreenActionA:
		case GreenActionB:
			greenActions++;
			break;
		case BlackActionA:
		case BlackActionB:
			blackActions++;
			break;
		case PurpleActionA:
		case PurpleActionB:
			purpleActions++;
			break;
		default: break;
		}
		switch(c.benefit)
		{
		default: throw G.Error("Not expecting benefit #1",c.benefit);
		case Reload:
			transfer(playedStratCards.height(),playedStratCards,stratCards,replay);
			break;
		case Achievement:
			{
			int index = c.findChip(PendulumChip.legendary);
			if(index>=0  && legendary.isEmpty())
			{	PlayerBoard deciding = parent.someoneIsDeciding();
				boolean skip = false;
				if(deciding!=null)
					{
					int my = parent.getPlayerPrivilege(color);
					int his = parent.getPlayerPrivilege(deciding.color);
					parent.privilegeResolutions++;
					parent.p1("achievement conflict");
					if(my>his) 
						{deciding.setUIStateNormal();
						 // poor guy, lost out on the legendary achievement
						 deciding.unDropObject();
						}
						else 
						{ skip = true;
						  unDropObject();
						}
					}
				if(!skip) { setUIState(UIState.AchievementOrLegandary,1);	}
			}
			else
			{
			allowUndrop = collectBenefit(c,c.chipAtIndex(0).pb[0],false,replay);
			}}
			break;
			
		case PerCard:
			allowUndrop = collectBenefit(c,chip.pb[0],false,replay);
			break;
		case Popularity1Prestige1Vote1:
			setPopularityVP(popularityVPvalue+1,replay);
			setPrestigeVP(prestigeVPvalue+1,replay);
			transferAlwaysAvailable(1,votesReserves,votes,replay);
			break;
		case Province:
			if(parent.hasProvincesAvailable()) { setUIState(UIState.Province,1); }
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
			setPowerVP(powerVPvalue+1,replay);
			transferAlwaysAvailable(2,votesReserves,votes,replay);
			break;
		case None: 
			switch(c.rackLocation())
			{
			case BlackMeepleA:
			case BlackMeepleB:
				// break here fixes the drop/pick problem for black zone, but there's still
				// a problem for green and purple
				//break;
			case GreenMeepleA:
			case GreenMeepleB:
			case PurpleMeepleA:
			case PurpleMeepleB:
				// note that it's important that dropping on the meeple placement spots 
				// is undoable, because that's what allows privilege to be resolved.
			default:
				allowUndrop = true;
				break;
			}
			break;
		}
		if(!allowUndrop) 
		{ acceptPlacement(); 
		  unSelect();
		}
		
	}
	public void printStats(PrintStream s)
	{	s.println(""+color);
		s.println("Black actions "+blackActions+" missed "+blackMissedActions);
		s.println("Green actions "+greenActions+" missed "+greenMissedActions);
		s.println("Purple actions "+purpleActions+" missed "+purpleMissedActions);
		s.println("Yellow Benefits "+yellowBenefitCount+" x "+yellowBenefitMultiplier);
		s.println("Blue Benefits "+blueBenefitCount+" x "+blueBenefitMultiplier);
		s.println("Brown Benefits "+brownBenefitCount+" x "+brownBenefitMultiplier);
		s.println("Red Benefits "+redBenefitCount+" x "+redBenefitMultiplier);
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
		 	{	PendulumChip top = (pickedSource==c && pickedObject!=null)
		 							? pickedObject 
		 							: c.topChip();
		 		if(top!=null)
		 		{
		 		some |= addCollectProvinceMovesTo(all,c,top);
		 		}
		 	}
		 return some;
	 }
	 private boolean addCollectProvinceMovesTo(CommonMoveStack all,PendulumCell from,PendulumChip top)
	 {
	 		if(all!=null)
	 			{ all.push(new PendulumMovespec(MOVE_FROM_TO,from,top,yellowBenefits,boardIndex));
	 			  all.push(new PendulumMovespec(MOVE_FROM_TO,from,top,brownBenefits,boardIndex));
	 			  all.push(new PendulumMovespec(MOVE_FROM_TO,from,top,redBenefits,boardIndex));
	 			  all.push(new PendulumMovespec(MOVE_FROM_TO,from,top,blueBenefits,boardIndex));
	 			}
	 		return true;
	 }
 
	 private boolean addCollectResourceMoves(CommonMoveStack all,PendulumCell from,PendulumCell to)
	 {
		 if(from.height()>0)
		 {	
			if(all!=null) { all.push(new PendulumMovespec(MOVE_FROM_TO,from,from.topChip(),to,boardIndex)); }
			return true;
		 }
		 return false;
	 }
	 public void setNextStateAfterDrop(PendulumCell c,replayMode replay)
	 {	 if(c!=null && c.rackLocation()==PendulumId.Province) 
	 		{ return; 	//refill a province, doesn't change state
	 		}
		 switch(uiState)
     		{
     		case ProvinceReward:
      		case Normal: break;
     		case RetrieveWorker:
    		case AchievementOrLegandary:
     		case PayResources:
    		case Province:
     		case SwapVotes:
     			uiCount--;
     			if(uiCount<=0) { setUIStateNormal(); }
     			break;
     		case CollectResources:
     			uiCount--;
     			if(!hasCollectResourceMoves()) { uiCount = 0; }	// ran out of resources
     			if(uiCount<=0) { setUIStateNormal(); }
     			break;
     		default: G.Error("Not expecting %s",uiState);
     		}
	 }
	 
	 public void collectCouncil(replayMode replay)
	 {	PendulumChip card = selectedCell.removeTop();
	 	PendulumChip dropCard = card;
	 	PendulumCell dest = parent.councilRewardsUsed;
	 	PB benefit = card.pb[0];
	 	boolean toHand = card.councilCardToHand;
	 	if(card==PendulumChip.defcard)
	 	{
	 		dest = parent.councilCards[4];
	 	}
	 	else if(card==PendulumChip.flipcard)
	 	{
	 		dropCard = PendulumChip.flipBack;
	 		dest = parent.councilCards[5];
	 	}
	 	else if(toHand || (parent.revision<102 && benefit==PB.Pow1Pres1Pop1) )
	 	{
	 		// permanent strategem cards
	 		dest = stratCards;
	 		benefit = PB.None;
	 	}
	
	 	dest.addChip(dropCard);

	 	collectBenefit(dest,benefit,true,replay);
		 
		 if(replay.animate) 
	 		{
	 		parent.animate(selectedCell,dest);
	 		}
		 unSelect();
	 }

	 public boolean doDrop(PendulumMovespec m,PendulumCell c,PendulumState state,boolean pair,replayMode replay)
	 {	PendulumChip po = pickedObject;
		if(dropObject(c,m,pair))
		{
			payCost(c,po,replay);
			collectBenefit(c,po,replay);
			return true;
		}
		return false;
	 }
	 public boolean canPlayStratCard(PendulumChip card)
	 {
		 return canPayCost(card.pc);
	 }
	 public void addPlayStrategem(CommonMoveStack all) 		{	G.Assert(uiState==UIState.Normal,"should be");
		if((pickedObject!=null && pickedSource.rackLocation()==PendulumId.PlayerStratCard))
		{	if(canPlayStratCard(pickedObject))
				{
				all.push(new PendulumMovespec(MOVE_FROM_TO,stratCards,pickedObject,playedStratCards,boardIndex));
				}
		}
		else if(pickedObject==null)
		{
		if(playedStratCards.height()>0 && culture.height()>=5)
			{
				all.push(new PendulumMovespec(MOVE_REFILL,boardIndex));
			}
		for(int lim=stratCards.height()-1; lim>=0; lim--)
		{
			PendulumChip ch = stratCards.chipAtIndex(lim);
			if(canPlayStratCard(ch))
			{
				all.push(new PendulumMovespec(MOVE_FROM_TO,stratCards,ch,playedStratCards,boardIndex));
			}
		}}
	}

	public boolean isResting() {
		return (uiState==UIState.Rest);
	}
	public boolean isReady() {
		return (uiState==UIState.Ready);
	}

	public int privilegeScore()
	{	return (votes.height()*100+parent.getPlayerPrivilege(color));
	}
	static int parchmentLevel = 13;
	static int LEGENDARY_SCORE = 10000;
	static int PARCHMENT_SCORE = 100;
	public int winnerScore()
	{	// not quite complete, but a good start
		boolean legendaryAchievement = !legendary.isEmpty();
		int val = (prestigeVPvalue+powerVPvalue+popularityVPvalue) + ( legendaryAchievement ? LEGENDARY_SCORE : 0);
		if(legendaryAchievement)
		{
			if( prestigeVPvalue>parchmentLevel 
				&& powerVPvalue>parchmentLevel
				&& popularityVPvalue>parchmentLevel)
			{// min 300
			 // max 2400
			 val += ((prestigeVPvalue+powerVPvalue+popularityVPvalue)-3*parchmentLevel)*100;
			}
			else 
			{	// max 260
				int min = Math.min(prestigeVPvalue,Math.min(powerVPvalue,popularityVPvalue))*63;
				val += min;
			}
		}
		return val;
	}
	public int compareTo(PlayerBoard o) {
		return G.signum(o.privilegeScore()-privilegeScore());
	}

	// clear votes at the beginning of the council phase
	public void clearVotes(replayMode replay) {
		councilVotes.copyFrom(votes);
		transfer(votes.height(),votes,votesReserves,replay);
	}
	public int provinceCardLimit()
	{
		return max3Cards.isEmpty() ? 2 : 3;
	}

	public boolean needsCouncilTrim() {
		int limit = provinceCardLimit();
		for(PendulumCell c : tucked)
		{
			if(c.height()>limit)
			{
				return true;
			}
		}
		return false;
	}
	public void addCouncilTrimMoves(CommonMoveStack all) 
	{
		int limit = provinceCardLimit();
		if(pickedObject!=null)
		{
			all.push(new PendulumMovespec(MOVE_FROM_TO,pickedSource,pickedObject,parent.trash,boardIndex));
		}
		else
		{for(PendulumCell c : tucked)
		{	int h = c.height();
			if(h>limit)
			{
				for(int i=0;i<h;i++)
				{
					all.push(new PendulumMovespec(MOVE_FROM_TO,c,c.chipAtIndex(i),parent.trash,boardIndex));
				}
			}
		}}
	}
	public void addPromoteMeepleMoves(CommonMoveStack all)
	{
		if(meeples.height()>0)
		{
			all.push(new PendulumMovespec(MOVE_SELECT,meeples,meeples.topChip(),boardIndex));
		}
	}
	public void unRest() 
	{
		if(uiState==UIState.Rest) { setUIStateNormal(); }
	}
	public void doMeeplePromotion(replayMode replay) 
	{
		switch(selectedCell.rackLocation())
		{
		case PlayerMeeples:
			meepleReserves.addChip(meeples.removeTop());
			grandes.addChip(grandeReserves.removeTop());
			if(replay.animate) 
				{ 
				parent.animate(meeples,meepleReserves);
				parent.animate(grandeReserves,grandes);
				}
			break;

		default:
			{
			int ind = selectedCell.findChip(meeple);
			meepleReserves.addChip(selectedCell.removeChipAtIndex(ind));
			selectedCell.addChip(grandeReserves.removeTop());
			if(replay.animate)
				{
				parent.animate(selectedCell,meepleReserves);
				parent.animate(grandeReserves,selectedCell);
				}
			}
		}
		unSelect();
	}

	public void addPayResourceMoves(CommonMoveStack all) 
	{	
		if((pickedObject==null) ? military.height()>0 : pickedSource==military)
		{
		all.push(new PendulumMovespec(MOVE_FROM_TO,military,PendulumChip.redCube,militaryReserves,boardIndex));
		}
		if((pickedObject==null) ? culture.height()>0 : pickedSource==culture)
		{
			all.push(new PendulumMovespec(MOVE_FROM_TO,culture,PendulumChip.blueCube,cultureReserves,boardIndex));
		}
		if((pickedObject==null) ? cash.height()>0 : pickedSource==cash)
		{
			all.push(new PendulumMovespec(MOVE_FROM_TO,cash,PendulumChip.yellowCube,cashReserves,boardIndex));
		}
		
	}
	
	public boolean hasSwapVoteMoves()
	{
		return addSwapVoteMoves(null);
	}
	public boolean hasVPChoiceMoves()
	{
		return addVPChoiceMoves(null,1);
	}
	public boolean addVPChoiceMoves(CommonMoveStack all,int n)
	{	boolean some = false;
		if(powerVPvalue<MAXVPVALUE) 
		{
			if(all==null) { return true;}
			some = true;
			all.push(new PendulumMovespec(MOVE_SELECT,
					powerVP[powerVPvalue],powerVP[powerVPvalue].topChip(),
					boardIndex));
		}
		if(prestigeVPvalue<MAXVPVALUE) 
		{
			if(all==null) { return true;}
			some = true;
			all.push(new PendulumMovespec(MOVE_SELECT,
					prestigeVP[prestigeVPvalue],prestigeVP[prestigeVPvalue].topChip(),
					boardIndex));
		}
		if(popularityVPvalue<MAXVPVALUE) 
		{
			if(all==null) { return true;}
			some = true;
			all.push(new PendulumMovespec(MOVE_SELECT,
					popularityVP[popularityVPvalue],popularityVP[popularityVPvalue].topChip(),
					boardIndex));
		}
		return some;
	}
	public boolean addSwapVoteMoves(CommonMoveStack all) 
	{
		int initialvp[] = mat.vps;
		boolean some = false;
		if(powerVPvalue>initialvp[0]) 
		{
			some |= addSwapVotesMoves(all,powerVP[powerVPvalue]);
			if(some && all==null) { return true;}
		}
		if(prestigeVPvalue>initialvp[1])
		{
			some |= addSwapVotesMoves(all,prestigeVP[prestigeVPvalue]);
			if(some && all==null) { return true; }
		}
		if(popularityVPvalue>initialvp[2])
		{
			some |= addSwapVotesMoves(all,popularityVP[popularityVPvalue]);
			if(some && all==null) { return true; }
		}
		return some;
	}
	public boolean addSwapVotesMoves(CommonMoveStack all,PendulumCell from)
	{	boolean some = false;
		if(from.rackLocation()!=PendulumId.PlayerPowerVP) 
			{ 	if(all!=null)
				{
				all.push(new PendulumMovespec(MOVE_SWAPVOTES,from,from.topChip(),powerVP[powerVPvalue],boardIndex));
				some = true;
				}
				else { return true; }
			}
		if(from.rackLocation()!=PendulumId.PlayerPrestigeVP) 
			{ 	if(all!=null)
				{
				all.push(new PendulumMovespec(MOVE_SWAPVOTES,from,from.topChip(),prestigeVP[prestigeVPvalue],boardIndex));
				some = true;
				}
				else { return true; }
			}
		if(from.rackLocation()!=PendulumId.PlayerPopularityVP) 
			{ 	if(all!=null)
				{	
				all.push(new PendulumMovespec(MOVE_SWAPVOTES,from,from.topChip(),popularityVP[popularityVPvalue],boardIndex));
				some = true;
				}
				else { return true; }
			}
		return some;
	}
	// undo partial moves.  This is used by the robot to normalize
	// the state of the other players before starting a run
	public void revertPartialMoves()
	{
		if(droppedDest!=null) { acceptPlacement(); }
		else if(pickedObject!=null) { alwaysUnpickObject(); }
	}

	// this is called when a timer flips to cement the placements of the timed rows
	public void acceptPlacements(PendulumCell[] row) {
		for(PendulumCell c : row) { if(c==droppedDest) { acceptPlacement(); }}
	}

	// count the meeples that are our color in this row.  The row
	// is an action or meeple row, and contains only meeples.
	public int countMeeples(PendulumCell[] row) {
		int n=0;
		for(PendulumCell c : row)
		{
			for(int lim=c.height()-1; lim>=0; lim--)
			{
				PendulumChip ch = c.chipAtIndex(lim);
				if(ch.color==color) { n++; }
			}
		}
		return n;
	}
	public void collectVPIncreaseTwice(replayMode replay)
	{
		if(selectedCell2!=null)
		{
			collectVPIncrease(1,replay);
			selectedCell = selectedCell2;
			selectedCell2 = null;
			collectVPIncrease(1,replay);
		}
		else
		{
			collectVPIncrease(2,replay);
		}
	}
	public void collectVPIncrease(int i,replayMode replay) {
		changeVP(selectedCell,i,replay);
		selectedCell=null;
		setUIStateNormal();
	}
}
