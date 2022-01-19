package euphoria;


import java.util.Hashtable;

import lib.Random;

import online.game.replayMode;
import lib.AR;
import lib.G;


/**
 * EPlayer contains the per-player information and logic for use and distribution of player resources
 * 
 * @author ddyer
 *
 */
class EPlayer implements EuphoriaConstants
{
enum PlayerView 
{
	Normal, Artifacts, AutoArtifacts,Dilemma, ActiveRecruits, HiddenRecruits;
}
EuphoriaBoard b = null;			// my board 
Colors color;					// the color of this player (shared by the worker dice and various markers)
EuphoriaChip myAuthority;		// a sample of my authority token
EuphoriaCell workers;			// workers available to be placed
EuphoriaCell newWorkers;		// workers to be re-rolled and knowledge checked.
EuphoriaCell activeRecruits;	// active recruit cards 
EuphoriaCell hiddenRecruits;	// hidden (and inactive) recruit cards
EuphoriaCell newRecruits[];		// recruit cards the user has to choose from in the special recruit GUI
EuphoriaCell authority;			// authority tokens
EuphoriaCell dilemma;			// ethical dilemma card
EuphoriaCell allegianceStars;	// stars on the active recruits
boolean dilemmaResolved=false;	// true if the card has been resolved.
RecruitChip originalHiddenRecruit = null;
RecruitChip originalActiveRecruit = null;
EuphoriaCell discardedRecruits = null;	// discarded, destined for the global unusedRecruits

// commodities are stacked, height is the number the player has. 
EuphoriaCell water;				// one water per water
EuphoriaCell food;
EuphoriaCell bliss;
EuphoriaCell energy;

// likewise for resources and artifacts
EuphoriaCell gold ;
EuphoriaCell stone;
EuphoriaCell clay;
EuphoriaCell artifacts;

EuphoriaCell ephemeralPickedSource = null;
EuphoriaChip ephemeralPickedObject = null;

EuphoriaCell marketBasket;		// temp for MoraleOrKnowledge

int knowlegeLimit = 16;			// actually a constant, but we'll treat it as a variable.
int knowledge = 3;				// current knowledge
int morale = 1;					// current morale
int totalWorkers = 0;			// workers in hand + on board.  Limit is 4
int finalRandomizer = 0;		// final randomizer for this player, the final tie breaker
int marketStars = 0;			// authority tokens on markets

//
// these variables reflect events on the current turn
//
private boolean hasLostAWorker = false;
private boolean hasAddedArtifact = false;
private boolean hasAddedArtifactLast = false;
private boolean hasPeeked = false;		// has seen the new card
private boolean hasUsedGeekTheOracle = false;
private boolean hasGainedWorker = false;
private boolean hasUsedBrianTheViticulturist = false;
private boolean mandatoryEquality = false;	// market penalty for mandatory equality

public EuphoriaCell placedWorkers[] = new EuphoriaCell[MAX_WORKERS];
public void placeWorker(EuphoriaCell c)
{
	if(c.rackLocation()==EuphoriaId.UnusedWorkers)
	{    		
		totalWorkers--;
		return;
	}
	else 
	{for(int lim=placedWorkers.length-1; lim>=0; lim--)
	{
		if(placedWorkers[lim]==null) { placedWorkers[lim] = c; return; }
	}}
	throw G.Error("No room for worker");
}
public void unPlaceWorker(EuphoriaCell c)
{
	for(int lim=placedWorkers.length-1; lim>=0; lim--)
	{
		if(placedWorkers[lim]==c) { placedWorkers[lim]=null; return; }
	}
	throw G.Error("No worker found");
}
boolean hasUsedJonathanTheGambler = false;

boolean resultsInDoubles = false;	

//
// stats, which may be used by the robot to help score the games it plays
//
int penaltyMoves = 0;				// likely bad, but not illegal moves
int cardsLost = 0;					// cards lost to morale checks
int workersLost = 0;				// workers lost to knowledge checks.
int placements = 0;					// total worker placements 
int retrievals = 0;					// total worker retrievals
int lostGoods = 0;					// goods lost to penalties

// add a new recruit chip to the active list
public void addActiveRecruit(EuphoriaChip ch)
{
	activeRecruits.addChip(ch);
	clearCostCache();			// uncache the costs
}

// notification when a new market opens.
public void newMarketOpened()
{	checkMandatoryEquality();	// new market opened, maybe has a new penalty
	clearCostCache();
}

// clear status at the start of a new turn
public void startNewTurn()
{	hasLostAWorker = false;
	hasAddedArtifactLast = hasAddedArtifact;
	hasAddedArtifact = false;
	hasPeeked = false;
	hasUsedGeekTheOracle = false;
	hasGainedWorker = false;
	resultsInDoubles = false;
	hasUsedJonathanTheGambler = false;
	hasUsedBrianTheViticulturist = false;
	checkMandatoryEquality();
}

// cache the status of AcadamyOfMandatoryEquality, which interferes with recruit abilities.
public void checkMandatoryEquality()
{
	mandatoryEquality = penaltyAppliesToMe(MarketChip.AcademyOfMandatoryEquality);
}
public void useGeekTheOracle() { hasUsedGeekTheOracle = true; }
public boolean hasUsedBrianTheViticulturist() { return(hasUsedBrianTheViticulturist);}
private void useBrianTheViticulturist() { hasUsedBrianTheViticulturist = true; }
// assist for the UI
PlayerView hiddenView = PlayerView.Normal;
PlayerView view = PlayerView.Normal;
PlayerView pendingView = PlayerView.Normal;

EuphoriaCell cardArray[] = new EuphoriaCell[6];

public boolean hasReducedRecruits()
{	
	return(newRecruitCardCount()==0);
}
boolean incrementKnowledge(replayMode replay) 
{ 	
	if(knowledge<MAX_KNOWLEDGE_TRACK) 
	{ knowledge++;
	  if(recruitAppliesToMe(RecruitChip.NickTheUnderstudy))
	  {
		  incrementMorale();
		  b.logGameEvent(NickTheUnderstudyEffect,getPlayerColor());
	  }
	  return(true);
	}
	return(false);
}
public void setKnowledge(int n) { knowledge = n; }
boolean decrementKnowledge() 
	{ if(knowledge>MIN_KNOWLEDGE_TRACK) { knowledge--; return(true); } return(false); }
boolean incrementMorale() { if(morale<MAX_MORALE_TRACK) { morale++; return(true); } return(false); }
private boolean decrementMorale() 
{ if(morale>MIN_KNOWLEDGE_TRACK)	 
	{ morale--; 
	  //G.print("dec m "+this+" ="+artifacts.height()+" "+morale);
	  if(recruitAppliesToMe(RecruitChip.CurtisThePropagandist))
	  {	  // also lose knowledge
		  if(decrementKnowledge())
		  {
			  b.logGameEvent(CurtisThePropagandistEffect,getPlayerColor());
		  }
	  }
	  return(true);
	} 
	return(false);
}
public void setMorale(int n) { morale = n; }
public void addArtifact(EuphoriaChip c) 
{	hasAddedArtifact = true;		// bookkeeping for GeekTheOracle and JonathanTheGambler
	artifacts.addChip(c); 
	//G.print("add a "+this+" ="+artifacts.height()+" "+morale);
}
public boolean setHasPeeked(boolean v) 
{ 	if(v) { hasPeeked |= hasAddedArtifact|hasAddedArtifactLast; } 
	return(hasPeeked); 
}
public boolean hasPeeked() { return(hasPeeked); }
public boolean hasAddedArtifact() { return(hasAddedArtifact); }
public boolean hasGainedWorker() { return(hasGainedWorker); }
public void addStone(EuphoriaChip c) { stone.addChip(c); }
public void addGold(EuphoriaChip c) { gold.addChip(c); }
public void addWater(EuphoriaChip c) { water.addChip(c); }
public void addEnergy(EuphoriaChip c) { energy.addChip(c); }
public void addBliss(EuphoriaChip c) { bliss.addChip(c); }
public void addClay(EuphoriaChip c) { clay.addChip(c); }
public void addFood(EuphoriaChip c) { food.addChip(c); }


public EuphoriaChip getAuthorityToken() 
{	if(authority.height()>0)
		{
		clearCostCache();
		return(authority.removeTop());
		}
	else { return(null); }
}
public int authorityTokensRemaining() { return(authority.height()); }

// test for doubles
public int hasWorkersLike(WorkerChip c)
{	int n = 0;
	for(int i=workers.height()-1; i>=0; i--) { if(workers.chipAtIndex(i)==c) { n++;  }}
	return(n);
}

// test if this market has our authority token on it.
public boolean hasAuthorityOnMarket(EuphoriaCell c)
{
	return(c.containsChip(EuphoriaChip.getAuthority(color)));
}

// test if we have an active recruit with a certain allegiance
public boolean hasActiveRecruit(Allegiance aa)
{	if(aa!=null)
	{
	for(int lim=activeRecruits.height()-1; lim>=0; lim--) 
		{	RecruitChip recruit = (RecruitChip)activeRecruits.chipAtIndex(lim);
			if(recruit.allegiance==aa) { return(true); }
		}}
	return(false);
}

//test if we have an active recruit with a certain allegiance
public int nActiveRecruits(Allegiance aa)
{	int n=0;
	if(aa!=null)
	{
	for(int lim=activeRecruits.height()-1; lim>=0; lim--) 
		{	RecruitChip recruit = (RecruitChip)activeRecruits.chipAtIndex(lim);
			if(recruit.allegiance==aa) { n++; }
		}}
	return(n);
}

//test if we have an active recruit with a certain allegiance
public boolean hasHiddenRecruit(Allegiance aa)
{	if(aa!=null)
	{
	for(int lim=hiddenRecruits.height()-1; lim>=0; lim--) 
		{	RecruitChip recruit = (RecruitChip)hiddenRecruits.chipAtIndex(lim);
			if(recruit.allegiance==aa) { return(true); }
		}}
	return(false);
}
//test if we have an active recruit with a certain allegiance
public int nHiddenRecruits(Allegiance aa)
{	int n=0;
	if(aa!=null)
	{
	for(int lim=hiddenRecruits.height()-1; lim>=0; lim--) 
		{	RecruitChip recruit = (RecruitChip)hiddenRecruits.chipAtIndex(lim);
			if(recruit.allegiance==aa) { n++; }
		}
	}
	return(n);
}


public String toString() { return("<EP "+color+">"); }
public String getPlayerColor() { return(color.toString()); }

public void addAllegianceStar(replayMode replay)
{	EuphoriaChip tok = getAuthorityToken();
	if(tok!=null) 
		{ allegianceStars.addChip(tok); 
		 if((allegianceStars.height()>3) || (allegianceStars.height()>activeRecruits.height()))
		 	{throw G.Error("too many allegiance stars");}
		  if(replay!=replayMode.Replay)
		  {
			  b.animatePlacedItem(authority,allegianceStars);
		  }
		}
}
//
// award the start for current recruits.
//
void awardAllegianceStars(Allegiance faction,replayMode replay)
{
	for(int lim=activeRecruits.height()-1; lim>=0; lim--)
	{
		RecruitChip ch = (RecruitChip)activeRecruits.chipAtIndex(lim);
		if(ch.allegiance==faction)
		{
			if(authority.height()>0) { addAllegianceStar(replay); } 
		}
	}
}
// number of unresolved recruit cards.  If nonzero triggers the recruit choice gui
int newRecruitCardCount() { int sum=0; for(EuphoriaCell c : newRecruits) { sum += c.height(); } return(sum); }
// when the recruit choice has been made, call this to discard the rest.
void discardNewRecruits(boolean remember) 
	{ for(EuphoriaCell c : newRecruits) { while(c.height()>0) { discardedRecruits.addChip(c.removeTop()); }}
	  if(remember)
	  {
		  originalActiveRecruit = (RecruitChip)activeRecruits.chipAtIndex(0);
		  originalHiddenRecruit = (RecruitChip)hiddenRecruits.chipAtIndex(0);
	  }
	}
void transferDiscardedRecruits(EuphoriaCell c)
{
	while(discardedRecruits.height()>0) { c.addChip(discardedRecruits.removeTop()); }
}
// bookkeeping - keep track of all the cells for this player.
private EuphoriaCell allCells = null;
private Hashtable <EuphoriaId,EuphoriaCell> allCellsHash = new Hashtable<EuphoriaId,EuphoriaCell>();
private void add(EuphoriaCell c) { allCellsHash.put(c.rackLocation(), c); c.next = allCells; allCells = c; }
// get the cell with a given ID.  All the player cells have distinct ids (no arrays)
EuphoriaCell getCell(EuphoriaId r)
{	EuphoriaCell c = allCellsHash.get(r);
	return(c);
}

int boardIndex;

EPlayer(EuphoriaBoard myBoard,int idx,Colors cl,int finalrandom)
{	Random r = new Random(645577*(idx+121));
	color = cl; 
	b = myBoard;
	boardIndex = idx;
	finalRandomizer = finalrandom;
	add(allegianceStars= new EuphoriaCell(EuphoriaChip.AuthorityMarkers[0].subtype(),r,EuphoriaId.PlayerAuthority,color));
	add(food = new EuphoriaCell(EuphoriaChip.Food.subtype(),r,EuphoriaId.PlayerFood,color));
	add(workers = new EuphoriaCell(WorkerChip.Subtype(),r,EuphoriaId.PlayerWorker,color));
	add(newWorkers = new EuphoriaCell(WorkerChip.Subtype(),r,EuphoriaId.PlayerNewWorker,color));
	add(water = new EuphoriaCell(EuphoriaChip.Water.subtype(),r,EuphoriaId.PlayerWater,color));
	add(bliss = new EuphoriaCell(EuphoriaChip.Bliss.subtype(),r,EuphoriaId.PlayerBliss,color));
	add(energy = new EuphoriaCell(EuphoriaChip.Energy.subtype(),r,EuphoriaId.PlayerEnergy,color));
	add(gold = new EuphoriaCell(EuphoriaChip.Gold.subtype(),r,EuphoriaId.PlayerGold,color));
	add(stone = new EuphoriaCell(EuphoriaChip.Stone.subtype(),r,EuphoriaId.PlayerStone,color));
	add(clay = new EuphoriaCell(EuphoriaChip.Clay.subtype(),r,EuphoriaId.PlayerClay,color));
	add(authority = new EuphoriaCell(EuphoriaChip.AuthorityMarkers[0].subtype(),r,EuphoriaId.PlayerAuthority,color));
	add(activeRecruits = new EuphoriaCell(RecruitChip.Subtype(),r,EuphoriaId.PlayerActiveRecruits,color));
		activeRecruits.label = ActiveRecruit;
	add(hiddenRecruits = new EuphoriaCell(RecruitChip.Subtype(),r,EuphoriaId.PlayerHiddenRecruits,color));
		hiddenRecruits.label = HiddenRecruit;
	add(marketBasket = new EuphoriaCell(null,r,EuphoriaId.PlayerBasket,color));
	// temp holding tank for unselected recruits
	discardedRecruits = new EuphoriaCell(RecruitChip.Subtype(),r,null,color);
	
	newRecruits = new EuphoriaCell[STARTING_RECRUITS];
	for(int i=0;i<newRecruits.length;i++)
	{
		add(newRecruits[i]=new EuphoriaCell(RecruitChip.Subtype(),r,RecruitIds[i],color));
	}
	add(artifacts = new EuphoriaCell(ArtifactChip.Subtype(),r,EuphoriaId.PlayerArtifacts,color));
	add(dilemma = new EuphoriaCell(DilemmaChip.Subtype(),r,EuphoriaId.PlayerDilemma,color));	
	for(int i=0;i<cardArray.length;i++)
	{
		add(cardArray[i]=new EuphoriaCell(ArtifactChip.Subtype(),r,ArtifactIds[i],color));
	}
}

// initialize the player - called when the board is initialized
void doInit(EuphoriaCell rec,EuphoriaCell dil)
{	for(EuphoriaCell c = allCells; c!=null; c=c.next) { c.reInit(); }
	knowledge = 3;
	morale = 1;
	dilemmaResolved = false;
	myAuthority = EuphoriaChip.getAuthority(color);
	for(int i=0;i<STARTING_RECRUITS;i++) { newRecruits[i].reInit(); newRecruits[i].addChip(rec.removeTop()); }		// 4 random unusedRecruits
	for(int i=0;i<STARTING_AUTHORITY_TOKENS;i++) {  authority.addChip(myAuthority); }
	dilemma.addChip(dil.removeTop());		// 1 randomly selected ethical dilemma
	view = PlayerView.Normal;
	hiddenView = PlayerView.Normal;
	pendingView = PlayerView.Normal;
	hasAddedArtifact = false;
	hasAddedArtifactLast = false;
	hasPeeked = false;
	mandatoryEquality = false;
	hasUsedJonathanTheGambler = false;
	originalHiddenRecruit = null;
	originalActiveRecruit = null;
	penaltyMoves = 0;
	cardsLost = 0;
	workersLost = 0;
	placements = 0;
	retrievals = 0;
	marketStars = 0;
	AR.setValue(placedWorkers,null);
}

// called when the board is digested
public long Digest(Random r)
{	long v = color.ordinal();
	for(EuphoriaCell c = allCells; c!=null; c=c.next) { v ^= c.Digest(); }
	v ^= knowledge*r.nextLong();
	v ^= morale*r.nextLong();
	v ^= totalWorkers*r.nextLong();
	v ^= (dilemmaResolved?2:1)*r.nextLong();
	v ^= r.nextLong()*(hasAddedArtifact?1:2);
	v ^= r.nextLong()*(mandatoryEquality?2:1);
	return(v);
}

public void sameBoard(EPlayer other)
{	for(EuphoriaCell c = allCells,d=other.allCells; c!=null; c=c.next,d=d.next) 
		{
		G.Assert(c.sameCell(d),"mismatch player cells %s and %s",c,d);
		}
	G.Assert(dilemmaResolved==other.dilemmaResolved,"dilemmaResolved mismatch");
	G.Assert(knowledge==other.knowledge,"knowledge mismatch");
	G.Assert(morale==other.morale,"morale mismatch");
	G.Assert(marketStars==other.marketStars,"morale mismatch");
	G.Assert(totalWorkers==other.totalWorkers,"totalWorkers mismatch");
	G.Assert(dilemmaResolved==other.dilemmaResolved,"dilemma resolved mismatch");
 
}
public static void sameBoard(EPlayer p,EPlayer o)
{	G.Assert((p==null)==(o==null),"board players mismatch");
	if(p!=null) { p.sameBoard(o); }
}
public void copyFrom(EPlayer other)
{	for(EuphoriaCell c = allCells,d=other.allCells; c!=null; c=c.next,d=d.next) 
	{
		c.copyFrom(d);
	}
	boardIndex = other.boardIndex;
	knowledge = other.knowledge;
	penaltyMoves = other.penaltyMoves;
	cardsLost = other.cardsLost;
	workersLost = other.workersLost;				// workers lost to knowledge checks.
	placements = other.placements;
	retrievals = other.retrievals;
	marketStars = other.marketStars;
	morale = other.morale;
	totalWorkers = other.totalWorkers;
	dilemmaResolved = other.dilemmaResolved;
    hasAddedArtifact = other.hasAddedArtifact;		// bookeeping for geek the oracle
    hasAddedArtifactLast = other.hasAddedArtifactLast;
    hasPeeked = other.hasPeeked;
    mandatoryEquality = other.mandatoryEquality;
    hasUsedJonathanTheGambler = other.hasUsedJonathanTheGambler;
    originalHiddenRecruit = other.originalHiddenRecruit;
    originalActiveRecruit = other.originalActiveRecruit;
    for(int lim=placedWorkers.length-1; lim>=0; lim--)
    {
    	placedWorkers[lim] = b.getCell(other.placedWorkers[lim]);
    }
	sameBoard(other);
}
void clearUCTStats()
{
	penaltyMoves = 0;
	cardsLost = 0;
	workersLost = 0;
	placements = 0;
	retrievals = 0;
}
//
//	Service to the main board 
//
public boolean hasWorkersOnBoard()
{
	return((workers.height()+newWorkers.height()) < totalWorkers);
}
public boolean hasNewWorkers() { return(newWorkers.height()>0); }
public int workersOnBoard() { return(totalWorkers-(workers.height()+newWorkers.height())); }
public boolean hasWorkersInHand()
{
	return(workers.height()>0);
}
public int totalKnowlege()			// total knowlege for this player
{
	int v = knowledge;
	for(int lim = workers.height()-1; lim>=0; lim--)
	{
		WorkerChip ch = (WorkerChip)workers.chipAtIndex(lim);
		v += ch.knowledge();
	}
	return(v);
}
public void removeWorker(WorkerChip c)	// remove one worker with value c
{	// when the complete rules are implemented, have to check for worker sacrifice
	workers.removeChip(c);
	totalWorkers--;
}

// check total knowledge and dump a worker if needed.  
// return true if a worker was lost.
public boolean knowledgeCheck(replayMode replay)			// check for knowledge and remove if necessary
{	WorkerChip worker = null;
	int workerIndex = -1;
	int know = totalKnowlege();
	if(know>=knowlegeLimit && !hasLostAWorker) 
	{	workersLost++;
		int nworkers = workers.height();
		hasLostAWorker = true;		// can only lose 1 worker per turn
		// remove the smartest worker
		for(int lim=nworkers; lim>=0; lim--)
		{
			WorkerChip c = (WorkerChip)workers.chipAtIndex(lim);
			if((worker==null)||(c.knowledge()>worker.knowledge())) { worker = c; workerIndex = lim; }
		}
		b.logGameEvent(LoseAWorker,""+know,""+worker.shortName()); 
		if(replay!=replayMode.Replay)
		{
			b.animateSacrificeWorker(workers,worker);
		}
		removeWorker(worker);

		if(b.usedChaseTheMiner
				&& (workerIndex==nworkers))
		{	b.usedChaseTheMiner = false;			// we lost the new guy
		}
		return(true);
	}
	return(false);
}
// return true if we need a UI to discard a card
public boolean moraleCheck(replayMode replay)
{	int nart = artifacts.height();
	if(morale<nart)
	{
	cardsLost += nart-morale;
	if(allOneArtifactType())  
		{ while(nart-->morale)
			{  b.addArtifact(artifacts.removeTop()); 
		  	   if(replay!=replayMode.Replay)  { b.animateReturnArtifact(artifacts); } 
		  	}
		}
		else { return(true);//need to interact
		}
	}
return(false);
}
public Cost moraleCost()
{	int ct = artifacts.height()-morale;
	switch(ct)
	{
	default: throw G.Error("Not expecting to lose %s cards",ct);
	case 1:	return(Cost.Card);
	case 2:	return(Cost.Cardx2);
	case 3: return(Cost.Cardx3);
	case 4: return(Cost.Cardx4);
	case 5:	return(Cost.Cardx5);
	case 6: return(Cost.Cardx6);
	}
}
public void addWorker(WorkerChip ch)
{	// add to the new worker pool, which will trigger a roll and knowlege check
	if(workers.containsChip(ch)) { resultsInDoubles = true; }
	workers.addChip(ch);
}
public void addNewWorker(WorkerChip ch)
{	// add to the new worker pool, which will trigger a roll and knowlege check
	newWorkers.addChip(ch);
}
/**
 * 
 * @return true there are artifacts in hand and all are the same.  This might save a user interaction if the user has lots of the same card.
 */
// 
public boolean allOneArtifactType()
{	if(artifacts.height()==0) { return(false); }
	EuphoriaChip ch = artifacts.topChip();
	for(int lim=artifacts.height()-2; lim>=0; lim--)
	{
		if(ch!=artifacts.chipAtIndex(lim)) { return(false); }
	}
	return(true);
}
public ArtifactChip hasArtifactPair()
{
	int hasMask = 0;
	for(int lim=artifacts.height()-1; lim>=0; lim--)
	{
		ArtifactChip ch = (ArtifactChip)artifacts.chipAtIndex(lim);
		int mask = 1<<ch.id.ordinal();
		if((hasMask&mask)!=0) { return(ch); }
		hasMask |= mask;
	}
	return(null);
}
// get a mask of the artifact types that have pairs in hand
public int getArtifactPairMask()
{	int singles = 0;
	int pairs = 0;
	for(int lim=artifacts.height()-1; lim>=0; lim--)
	{
		ArtifactChip ch = (ArtifactChip)artifacts.chipAtIndex(lim);
		int mask = 1<<ch.id.ordinal();
		if((singles&mask)==0) { singles |= mask; } else { pairs |= mask; }
	}
	return(pairs);
}

public int countArtifactPairs()
{	int pairMask = 0;
	int hasMask = 0;
	for(int lim=artifacts.height()-1; lim>=0; lim--)
	{
		ArtifactChip ch = (ArtifactChip)artifacts.chipAtIndex(lim);
		int mask = 1<<ch.id.ordinal();
		if((mask&hasMask)==0) { hasMask |= mask; } else { pairMask|=mask; }
	}
	return(G.bitCount(b.revision>=105 ? pairMask : hasMask));
}
int totalResources()
{
	return(stone.height()+gold.height()+clay.height());
}
int nKindsOfResource()
{
	int types = 0;
	if(stone.height()>0) { types++; }
	if(gold.height()>0) { types++; }
	if(clay.height()>0) { types++; }
	return(types);
}
int totalCommoditiesExceptBliss()
{
	return(water.height()+food.height()+energy.height());
}
int totalCommodities()
{
	return(bliss.height()+totalCommoditiesExceptBliss());
}
int  nKindsOfCommodityExceptBliss()
{	int types = 0;
	if(water.height()>0) { types++; }
	if(energy.height()>0) { types++; }
	if(food.height()>0) { types++; }
	return(types);
	
}
int nKindsOfCommodity()
{
	return(nKindsOfCommodityExceptBliss()+ ((bliss.height()==0)?0:1));
}
int nKindsOfCommodityOrResource()
{	return(nKindsOfCommodity()+nKindsOfResource());
}

// this is only called when we know only one kind is available
void payCommodityOrResource(int n,replayMode replay)
{	while(n-- > 0)
	{
	if(water.height()>0) { b.addWater(water.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnWater(water); } }
	else if(energy.height()>0) { b.addEnergy(energy.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnEnergy(energy); } }
	else if(food.height()>0) { b.addFood(food.removeTop());  if(replay!=replayMode.Replay) { b.animateReturnFood(food); }}
	else if(bliss.height()>0) { b.addBliss(bliss.removeTop());  if(replay!=replayMode.Replay) { b.animateReturnBliss(bliss); }}
	else if(clay.height()>0) { b.addClay(clay.removeTop());  if(replay!=replayMode.Replay) { b.animateReturnClay(clay); }}
	else if(gold.height()>0) { b.addGold(gold.removeTop());  if(replay!=replayMode.Replay) { b.animateReturnGold(gold); }}
	else if(stone.height()>0) { b.addStone(stone.removeTop());  if(replay!=replayMode.Replay) { b.animateReturnStone(stone); } }
	else { throw G.Error("Ran out of things"); }
	}
}
boolean loseMorale(replayMode replay)	// retrieving workers without paying.  return true if we also have to lose a card
{
	decrementMorale();
	return(moraleCheck(replay));
}

void gainMorale()
{
	incrementMorale();
}

//
// JoshTheNegotiator lets you use bliss instead of a resource to open a market
//
private boolean resourceOrJoshTheNegotiator(EuphoriaCell c)
{
	if(c.height()>0) { return(true); }
	if(bliss.height()>0) { return(true); }
	return(false);

}

private Cost alternateCostForBrianTheViticulturist_V2(Cost cost)
{
	switch(cost)
	{
	case Mostly_Free:	// applies to the bliss factory.  Mostly free is free except to Brian
		return(Cost.Morale);
	case Mostly_Resourcex3:		// mostly_resourcex3 is just resourcex3 except to brian
		return(Cost.Morale_Resourcex3);
	case Mostly_Bliss_Commodity:	// mostly bliss_commodity is just bliss_commodity except to brian
									// but he can still pay food
		return(Cost.Morale_BlissOrFoodPlus1);
	case Mostly_Artifactx3:		// mostly_artifactx3 is just artifactx3 except to brian
		return(Cost.Morale_Artifactx3);
	default: return(alternateCostForBrianTheViticulturist(cost));
	}
}
private Cost alternateCostForBrianTheViticulturist(Cost cost)
{
	switch(cost)
	{
	case Mostly_Bliss_Commodity:
	case Bliss_Commodity:
		return(Cost.BlissOrFoodPlus1);	// use food for some of the bliss
	case Blissx4_Resource:
		return(Cost.BlissOrFoodx4_Resource);
	case Blissx4_Card: 
		return(Cost.BlissOrFoodx4_Card);
		
	// additional interactions, only possible in combo with JoshTheNegotiator
	 
    case Blissx4_ResourceOrBliss: 
    	return(Cost.BlissOrFoodx4_ResourceOrBlissOrFood);

    case Energyx4_StoneOrBliss:
		return(Cost.Energyx4_StoneOrBlissOrFood);

	case Waterx4_ClayOrBliss:
		return(Cost.Waterx4_ClayOrBlissOrFood);
		
	case Waterx4_GoldOrBliss:
		return(Cost.Waterx4_GoldOrBlissOrFood);
		
	 case Energyx4_ClayOrBliss:
		 return(Cost.Energyx4_ClayOrBlissOrFood);
		 
	 case Foodx4_StoneOrBliss:
		 return(Cost.Foodx4_StoneOrBlissOrFood);
		 
	 case Card_ResourceOrBliss:
		 return(Cost.Card_ResourceOrBlissOrFood);

	default: return(cost);
	}
}

private Cost alternateCostForFlartnerTheLuddite(Cost cost,int revision)
{
	 switch(cost)
	 {
	 case ResourceAndKnowledgeAndMorale: 
		 if(revision>=118) { return(Cost.ResourceAndKnowledgeAndMoraleOrArtifact); }
		 return(cost);
	 case ConstructionSiteStone: return(Cost.StoneOrArtifact);
	 case ConstructionSiteGold: return(Cost.GoldOrArtifact);
	 case ConstructionSiteClay: return(Cost.ClayOrArtifact);
	 default: return(cost); 
	 }
	
}

private Cost alternateCostForJeffersonTheShockArtist(Cost cost)
{
	 switch(cost)
	 {
	 case BlissOrFood: return(Cost.EnergyOrBlissOrFood);
	 
	 default: return(cost); 
	 }
	
}

private Cost alternateCostForJoshTheNegotiator(Cost cost)
{	

	 switch(cost)
	 {
	 case Energyx4_Stone: return(Cost.Energyx4_StoneOrBliss);
	 case Waterx4_Clay: return(Cost.Waterx4_ClayOrBliss);
	 case Waterx4_Gold: return(Cost.Waterx4_GoldOrBliss);
	 case Blissx4_Resource: return(Cost.Blissx4_ResourceOrBliss);
	 case Energyx4_Clay: return(Cost.Energyx4_ClayOrBliss);
	 case Foodx4_Stone: return(Cost.Foodx4_StoneOrBliss);
	 case Card_Resource: return(Cost.Card_ResourceOrBliss);
	 // interactions with BrianTheVitculturist
	 
	 default: return(cost); 
	 }
	
}

Cost alternateCostForKadanTheInfiltrator(Cost cost)
{	//
	// this cost switch is the complete cost for kadan, who gets to use the
	// tunnel mines even if he're not the appropriate allegiance, for 2 knowledge.
	//
	switch(cost)
	{
	case IsIcarite: if(!hasActiveRecruit(Allegiance.Icarite)) { cost = Cost.Knowledgex2; }
		break;
	case IsSubterran:	if(!hasActiveRecruit(Allegiance.Subterran)) { cost = Cost.Knowledgex2; }
		break;
	case IsEuphorian:	if(!hasActiveRecruit(Allegiance.Euphorian)) { cost = Cost.Knowledgex2; }
		break;
	case IsWastelander:	if(!hasActiveRecruit(Allegiance.Wastelander)) { cost = Cost.Knowledgex2; }
		break;
	default: break;
	
	}
	return(cost);
}
private Cost alternateCostForMichaelTheEngineer(Cost cost)
{
	switch(cost)
	{
	case ConstructionSiteGold:
	case ConstructionSiteStone:
	case ConstructionSiteClay:
		//
		// this actually applies only to placement on construction sites.  I don't
		// think there's any conflict, but if there is, it should be resolved by duplicating
		// the costs
		//
		return(Cost.ResourceAndKnowledgeAndMorale);
	default: break;
	}
	return(cost);
}
private Cost alternateCostForMatthewTheThief(Cost cost)
{
	switch(cost)
	{
	case WaterForMatthewTheThief:
		return(Cost.WaterOrKnowledge);
	case EnergyForMatthewTheThief:
		return(Cost.EnergyOrKnowledge);
	case FoodForMatthewTheThief:
		return(Cost.FoodOrKnowledge);
	default: break;
	}
	return(cost);
}

public void checkForMandatoryEquality(EuphoriaCell c)
{	if(c!=null)
	{
	switch(c.rackLocation())
	{
	case Market:
		{ 	EuphoriaChip market = c.chipAtIndex(0);
			if(market==MarketChip.AcademyOfMandatoryEquality)
			{	clearCostCache();
			}
		}
		break;
	default: break;
	}}
}


private Hashtable<Cost,Cost>cachedCost = new Hashtable<Cost,Cost>();
boolean verify = false;
void clearCostCache()
{ cachedCost.clear(); 
}

/**
 * get the actual cost to be used, considering recruit capabilities
 * 
 * @param cost
 * @return
 */

Cost alternateCostWithRecruits(Cost cost)
{	Cost alt = cachedCost.get(cost);
	if(!verify && (alt!=null)) { return(alt); }
	Cost originalCost = cost;
	
	if(recruitAppliesToMe(RecruitChip.MatthewTheThief))
		{
			cost = alternateCostForMatthewTheThief(cost);
		}
	if(recruitAppliesToMe(RecruitChip.MichaelTheEngineer))
		{
			cost = alternateCostForMichaelTheEngineer(cost);
		}
	if(recruitAppliesToMe(RecruitChip.JoshTheNegotiator))
		{	// interacts with BrianTheViticulturist, do this first
			cost = alternateCostForJoshTheNegotiator(cost);
		}
	if(recruitAppliesToMe(RecruitChip.BrianTheViticulturist_V2))
		{
		cost = alternateCostForBrianTheViticulturist_V2(cost);
		}
	if(recruitAppliesToMe(RecruitChip.BrianTheViticulturist))
		{	// interacts with JoshTheNegotiator, do this second
			cost = alternateCostForBrianTheViticulturist(cost);
		}
	if(recruitAppliesToMe(RecruitChip.FlartnerTheLuddite))
		{
			cost = alternateCostForFlartnerTheLuddite(cost,b.revision);
		}
	if(recruitAppliesToMe(RecruitChip.JeffersonTheShockArtist))
		{
			cost = alternateCostForJeffersonTheShockArtist(cost);
		}
	if(recruitAppliesToMe(RecruitChip.KadanTheInfiltrator))
		{
			cost = alternateCostForKadanTheInfiltrator(cost);
		}
	if(verify && (alt!=null))
		{ 
		
		G.Assert(cost==alt,"cached cost is %s but live cost is %s",alt,cost); 
		}
	else 
		{
		cachedCost.put(originalCost,cost); 
		}
	return(cost);
}
/**
 * resource tests, for placement.  This tests the actual resources to see if a cost could be paid.
 * this is used by the move generator and the pick/drop engine to see what spaces should be allowed.
 * In cases where recruits affect what can be use to pay a cost, we substutite a suitable new cost
 * so the upstream code mostly doesn't worry about recruits.
 * @param item
 * @param b
 * @return true if the payment is possible without interaction
 */
// without considering special rules that might apply to the transaction
boolean canPay(Cost item0)
{	Cost item = alternateCostWithRecruits(item0);
	switch(item)
	{
	default: throw G.Error("Unexpected payment test for %s",item);
	case Bliss:	return(bliss.height()>=1);
	case Waterx4:	return(water.height()>=4);
	case Energyx4: return(energy.height()>=4);
	case Foodx4:	return(food.height()>=4);
	case Blissx4:	return(bliss.height()>=4);

	case WaterOrKnowledge:
		return((water.height()>0) || (knowledge<MAX_KNOWLEDGE_TRACK));
		
	case EnergyOrKnowledge:
		return((energy.height()>0) || (knowledge<MAX_KNOWLEDGE_TRACK));
		
	case FoodOrKnowledge:
		return((food.height()>0) || (knowledge<MAX_KNOWLEDGE_TRACK));
		
	case IsIcarite:
		return(hasActiveRecruit(Allegiance.Icarite));
	case IsEuphorian:
		return(hasActiveRecruit(Allegiance.Euphorian));
	case IsSubterran:
		return(hasActiveRecruit(Allegiance.Subterran));
	case IsWastelander:
		return(hasActiveRecruit(Allegiance.Wastelander));
	
	case ResourceAndKnowledgeAndMoraleOrArtifact:	// combo of michael the engineer and flartner the luddite
		return(artifacts.height()>0 || (totalResources()>0));
		
	case ResourceAndKnowledgeAndMorale:	// special for MichaelTheEngineer, the knowledge and morale are optional
	case Resource:
		return(totalResources()>0);
		
	case SacrificeWorker:
		return(totalWorkers>1);
		
	case Closed: return(false); 	// never open to workers
	
	case TunnelOpen:
		return(false);	// condition will change to free once the tunnel is open

	case MarketCost:
		return(false); 	// market not open, condition will change when the market is open
		
	case Mostly_Free:
	case Free: return(true);
	
	case GoldOrArtifact:
		if(artifacts.height()>=1) { return(true); }
		// or fall into regular gold cost
		//$FALL-THROUGH$
	case Gold:		// laurathephilanthropist
	case ConstructionSiteGold:		// building markets
		return(gold.height()>=1);
		
	case StoneOrArtifact:
		if(artifacts.height()>=1) { return(true); }
		// or fall into regular gold cost	
		//$FALL-THROUGH$
	case ConstructionSiteStone:		// building markets
		return(stone.height()>=1);
		
	case ClayOrArtifact:
		if(artifacts.height()>=1) { return(true); }
		// or fall into regular clay
		//$FALL-THROUGH$
	case ConstructionSiteClay:		// building markets
		return(clay.height()>=1);
		
	case Artifactx2:		// JackoTheMerchant
		return(artifacts.height()>=2);
		
	case Artifactx3Only:		// various markets.  Usually take 3 cards or a pair
		return(artifacts.height()>=3);
	case ArtifactPair:
		return(hasArtifactPair()!=null);
		
	case Smart_Artifact:
		{	// returns true only if we must be smart about what artifact to discard
		boolean canUse2 = (hasArtifactPair()!=null) && !penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement);
		return(canUse2 && (artifacts.height()==3));
		}
	case Morale_Artifactx3:
		{
		boolean canUse2 = (hasArtifactPair()!=null) && !penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement);
		int needed = canUse2 ? 3 : 4;
		if(morale<needed) { return(false); }	// to lost one morale and pay a pair of artifacts, you have to have at least 3 to start
		}
		//$FALL-THROUGH$
	case Mostly_Artifactx3:
	case Artifactx3:
		if(penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement))
		{
			return(artifacts.height()>=3);
		}
		return((artifacts.height()>=3) || (hasArtifactPair()!=null));
		
	case ArtifactJackoTheArchivist_V2:
		return((artifacts.height()>0)	// jacko can pay just 1 in this case
				&& ((b.revision>=117) || ((knowledge+2)<=MAX_KNOWLEDGE_TRACK)));
	case Energy:		// euphorian mine
		return(energy.height()>=1);
	case Food:		// wastelander mine
		return(food.height()>=1);
	case Water:		// subterran mine
		return(water.height()>=1);
		
	case Morale_Resourcex3:
		if(morale<=1) { return(false); }
		//$FALL-THROUGH$
	case Resourcex3:	// nimbus loft
	case Mostly_Resourcex3:
		return((stone.height()+clay.height()+gold.height())>=3);
		
	case Energyx3:	// worker training
		// you can pay for the other effects even if you have 4 workers
		return(energy.height()>=3);
	case Waterx3:	// worker training
		// you can pay for the other effects even if you have 4 workers
		return(water.height()>=3);
	case EnergyOrBlissOrFood:
		if(energy.height()>0) { return(true); }
		// or fall into regular blissorfood
		//$FALL-THROUGH$
	case BlissOrFood:
		// no special effect for brian the viticulturist
		
		return((bliss.height()>0) || (food.height()>0));
		
	// prices for opened markets
	case Morale_BlissOrFoodPlus1:
		if(morale<=1)  { return(false); }
		// fall through
		//$FALL-THROUGH$
	case BlissOrFoodPlus1:	// used by BrianTheViticulturist
		if((food.height()>=1)&& (totalCommodities()>=2)) 
				{ return(true);	// use food for bliss 
				}
		// fall into standard bliss_commodity
		//$FALL-THROUGH$
	case Bliss_Commodity:	// breeze bar and sky lounge
	case Mostly_Bliss_Commodity:
		return((bliss.height()>=1) && (totalCommoditiesExceptBliss()>=1));
		
	case Card_ResourceOrBlissOrFood:
		if((artifacts.height()>=1) && (totalResources()+(food.height()+bliss.height())>0)) { return(true); }
		return(false);
		
	case Card_ResourceOrBliss:	// JoshTheNegotiator
		if((artifacts.height()>=1) && (bliss.height()>0)) { return(true); }
		// or fall into regular Card_Resource
		//$FALL-THROUGH$
	case Card_Resource:
		if((artifacts.height()>=1) && (totalResources()>=1)) { return(true); }
		// assuming no interaction with BrianTheViticulturist
		return(false);
		
	case Waterx4_Card:
		return((water.height()>=4) && (artifacts.height()>=1));
	case Energyx4_StoneOrBliss:
		return((energy.height()>=4)	&& resourceOrJoshTheNegotiator(stone));
		
	case Energyx4_Stone:
		return((energy.height()>=4) && (stone.height()>0));

	case Waterx4_ClayOrBliss:
		return((water.height()>=4) && resourceOrJoshTheNegotiator(clay));
		
	case Waterx4_Clay:
		return((water.height()>=4) && (clay.height()>0));

	case Waterx4_ClayOrBlissOrFood:
		return((water.height()>=4) && ((clay.height()>0)||(bliss.height()>0)||(food.height()>0)));
		
	case Energyx4_ClayOrBlissOrFood:
		return((energy.height()>=4) && ((clay.height()>0)||(bliss.height()>0)||(food.height()>0)));
	case Waterx4_GoldOrBliss:
		return((water.height()>=4) && resourceOrJoshTheNegotiator(gold));
		
	case Waterx4_Gold:
		return((water.height()>=4) && (gold.height()>0));
		
	case Energyx4_Card:
		return((energy.height()>=4) && (artifacts.height()>=1));
		
	case Foodx4_Gold:
		return((food.height()>=4) && (gold.height()>=1));
	case Foodx4_Card:
		return((food.height()>=4) && (artifacts.height()>=1));
		
	case Waterx4_GoldOrBlissOrFood:
		return((water.height()>=4) && ( (gold.height()>0) || (bliss.height()>0) || (food.height()>0)));
		
	case Foodx4_StoneOrBlissOrFood:
		return((food.height()>=5)
				|| ((food.height()>=4) && ((stone.height()>=1)||(bliss.height()>=1))));
	case BlissOrFoodx4_ResourceOrBlissOrFood:
		if(totalResources()>=1) { return((bliss.height()+food.height())>=4); }
		return((bliss.height()+food.height())>=5);
	case BlissOrFoodx4_Resource:
		if(	((food.height()+bliss.height())>=4)
				&& (totalResources()>=1)) { return(true); }
		// fall into standard Blissx4_resource
		//$FALL-THROUGH$
	case Blissx4_Resource:
		{
		int tbliss = bliss.height();
		int total = totalResources();
		if((tbliss>=4) && (total>=1)) { return(true); }
		return(false);
		}
	case Blissx4_ResourceOrBliss:
		if(bliss.height()>=5) { return(true); }	// pure bliss!
		return((bliss.height()>=4) && (totalResources()>0));

	case Energyx4_StoneOrBlissOrFood:
		return((energy.height()>=4) && ((stone.height()>0)||(bliss.height()>0)||(food.height()>0)));
	case Energyx4_ClayOrBliss:
		return((energy.height()>=4) && resourceOrJoshTheNegotiator(clay));
	case Energyx4_Clay:
		return((energy.height()>=4) && (clay.height()>0));
		
	case BlissOrFoodx4_Card:
		return(((artifacts.height()>0) && (food.height()+bliss.height())>=4));
		// fall into standard blissx4_card
	case Blissx4_Card:
		if(artifacts.height()>=1)
		{
		int tbliss = bliss.height();
		if (tbliss>=4) { return(true); }  
		}
		return(false);
	case Foodx4_StoneOrBliss:
		return((food.height()>=4) && resourceOrJoshTheNegotiator(stone));
		
	case Foodx4_Stone:
		return((food.height()>=4) && (stone.height()>=1));
	case Commodity_Bear:
		return((totalCommodities()>=1) && artifacts.containsChip(ArtifactChip.Bear));
	case Commodity_Bifocals:
		return((totalCommodities()>=1) && artifacts.containsChip(ArtifactChip.Bifocals));
	case Commodity_Balloons:
		return((totalCommodities()>=1) && artifacts.containsChip(ArtifactChip.Balloons));
	case Commodity_Box:
		return((totalCommodities()>=1) && artifacts.containsChip(ArtifactChip.Box));
	case Commodity_Bat:
		return((totalCommodities()>=1) && artifacts.containsChip(ArtifactChip.Bat));
	case Commodity_Book:
		return((totalCommodities()>=1) && artifacts.containsChip(ArtifactChip.Book));

	case Bear:
		return(artifacts.containsChip(ArtifactChip.Bear));
	case Bifocals:
		return(artifacts.containsChip(ArtifactChip.Bifocals));
	case Balloons:
		return(artifacts.containsChip(ArtifactChip.Balloons));
	case Box:
		return(artifacts.containsChip(ArtifactChip.Box));
	case Bat:
		return(artifacts.containsChip(ArtifactChip.Bat));
	case Book:
		return(artifacts.containsChip(ArtifactChip.Book));

	// dilemma costs
	case BearOrCardx2:
		return((artifacts.containsChip(ArtifactChip.Bear)) || (artifacts.height()>=2) );
	case BatOrCardx2:
		return((artifacts.containsChip(ArtifactChip.Bat)) || (artifacts.height()>=2) );
	case BoxOrCardx2:
		return((artifacts.containsChip(ArtifactChip.Box)) || (artifacts.height()>=2) );
	case BalloonsOrCardx2:
		return((artifacts.containsChip(ArtifactChip.Balloons)) || (artifacts.height()>=2) );
	case BookOrCardx2:
		return((artifacts.containsChip(ArtifactChip.Book)) || (artifacts.height()>=2) );
	case BifocalsOrCardx2:
		return((artifacts.containsChip(ArtifactChip.Bifocals)) || (artifacts.height()>=2) );
	
	// loss of morale
	case Morale:
		return(morale>=2);
	case Moralex2:
		return(morale>=3);		// have to have it in the bank (for use with MaximeTheAmbassador)
	case Knowledge:
		return((knowledge+1)<=MAX_KNOWLEDGE_TRACK);
	case Knowledgex2:			// have to have 2 available, for GeekTheOracle
		return((knowledge+2)<=MAX_KNOWLEDGE_TRACK);
	// 
	// this are not normally paid per se, but are extracted as a penalty
	// when your morale declines.
	//
	case Cardx6:
		return(artifacts.height()>=6);
	case Cardx5:
		return(artifacts.height()>=5);
	case Cardx4:
		return(artifacts.height()>=4);
	case Cardx3:
		return(artifacts.height()>=3);
	case Cardx2:
	case CardForGeekx2:
		return(artifacts.height()>=2);
	case Card:
	case Artifact:
	case CardForGeek:
		return(artifacts.height()>=1);	// shouldn't actually be called, becase we only exact the penalty when there is a cost
		
	case CommodityOrResourcex4Penalty:
	case CommodityOrResourcex3Penalty:
	case CommodityOrResourcex2Penalty:
	case CommodityOrResourcePenalty:	// some markets impose this cost to non-participants
		return(true); 			// you can always pay this because you can pay nothing if you have nothing.
	}
}

private EuphoriaChip shedOneResource(replayMode replay)
{
	if(stone.height()>0)
		{ b.addStone(stone.removeTop()); 
		  if(replay!=replayMode.Replay) { b.animateReturnStone(stone); }
		  return(RecruitChip.Stone);
		}
	else if(gold.height()>0) 
		{ b.addGold(gold.removeTop());  if(replay!=replayMode.Replay) { b.animateReturnGold(gold); }
		  return(RecruitChip.Gold);
		}
	else if(clay.height()>0) 
		{ b.addClay(clay.removeTop());  if(replay!=replayMode.Replay) { b.animateReturnClay(clay); }
		  return(RecruitChip.Clay);
		}
	else { throw G.Error("ran out of resources");}
}
private void shedOneCommodity(replayMode replay)
{
	if(water.height()>0) {  b.addWater(water.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnWater(water); }}
	else if(food.height()>0) { b.addFood(food.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnFood(food); }}
	else if(energy.height()>0) {  b.addEnergy(energy.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnEnergy(energy); }}
	else if(bliss.height()>0) {  b.addBliss(bliss.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnBliss(bliss); }}
	else { throw G.Error("Ran out of commodities"); }
}
private void shedCards(int n,replayMode replay)
{	while(n-- > 0)
	{
		b.addArtifact(artifacts.removeTop()); 
		if(replay!=replayMode.Replay)
	  		{  b.animateReturnArtifact(artifacts);
	  		}
	}
}

// supply a card instead of a resource if it is unambiguous
private boolean doFlartner(replayMode replay)
{
	if(!allOneArtifactType()) { return(false); }	// dialog required if both artifacts and gold, or mixed artifacts
	
	b.addArtifact(artifacts.removeTop());
	incrementMorale();
	b.logGameEvent(FlartnerTheLudditeEffect);
	if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts);}
	return(true);
	
}

//
// sacrifice a worker on top of cell c
//
void sacrificeWorker(EuphoriaCell c,replayMode replay)
{
	EuphoriaChip ch = c.removeTop();
	G.Assert(ch.isWorker() && (ch.color==color),"expected one of our workers");
	if(c.onBoard) { unPlaceWorker(c); }
	totalWorkers--;
	if(replay!=replayMode.Replay) { b.animateSacrificeWorker(c,(WorkerChip)ch);}
}
void sendStone(int n,replayMode replay)
{
	for(int i=0;i<n;i++) { b.addStone(stone.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnStone(stone); }}
}
void sendGold(int n,replayMode replay)
{
	for(int i=0;i<n;i++) { b.addGold(gold.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnGold(gold); }}
}
void sendClay(int n,replayMode replay)
{
	for(int i=0;i<n;i++) { b.addClay(clay.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnClay(clay); }}
}

void sendEnergy(int n,replayMode replay)
{
	for(int i=0;i<n;i++) { b.addEnergy(energy.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnEnergy(energy); }}
}
void sendFood(int n,replayMode replay)
{
	for(int i=0;i<n;i++) { b.addFood(food.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnFood(food); }}
}
void sendWater(int n,replayMode replay)
{
	for(int i=0;i<n;i++) { b.addWater(water.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnWater(water); }}
}

void sendBliss(int n,replayMode replay)
{
	for(int i=0;i<n;i++) { b.addBliss(bliss.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnBliss(bliss); }}
}
void sendArtifacts(int n,replayMode replay)
{
	for(int i=0;i<n;i++) { b.addArtifact(artifacts.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }}
}

boolean artifactChip(ArtifactChip which,replayMode replay)
{
	artifacts.removeChip(which);
	if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }
	return(true);
}

boolean artifactx2OrSpecific(ArtifactChip which,replayMode replay)
{	
	if(b.revision>=103)
	{
		if((artifacts.topChip()==which) && allOneArtifactType())
		{
			sendArtifacts(1,replay);
			return(true);
		}
		else if((artifacts.height()==2) && !artifacts.containsChip(which))
		{
			sendArtifacts(2,replay);
			return(true);
		}
	}
	else
	{
	// buggy version before 103, which takes extra cards
	int h = artifacts.height();
	switch(h)
	{
	case 0:	break;	// no choice
	case 1: if(artifacts.topChip()==which) 
				{ sendArtifacts(1,replay);
				  return(true); 
				}
			// not matching
			break;
	default: if(!allOneArtifactType()) { break; }	// not uniform, force interaction
		//$FALL-THROUGH$
	case 2:	
		sendArtifacts(2,replay);
		return(true);
	}}

	return(false);

	
}
/**
 * 
   it is an error if the cost can't be paid, you should have checked first.
   In cases where recruits alter the things that can be paid, we substitute a different cost, so all
   the upstream code mostly thinks about costs, not recruit options.   
   
   The intention is that if there is only one way to pay the cost, it's done automatically,
   but if there are multiple ways, nothing is done and false is returned.  This will force
   an interaction with the user.  Sometimes there is a fixed cost that is payable plus
   a variable part.  This is handled upstream by paying the fixed cost and substituting
   a new variable-only cost.
   
 * @param item0
 * @param b
 * @param replay
 * @return true of the action is fully resolved, false, if unresolved and requires GUI interaction.
 * ALWAYS check the return value even for payments which are thought to succeed. 
 */
// 
boolean payCost(Cost item0,replayMode replay)
{	int commoditiesNeeded = 0;
	Cost item = alternateCostWithRecruits(item0);
	switch(item)
	{
	case Closed:
	case DisplayOnly:
	case MarketCost:
	case TunnelOpen:
	default: throw G.Error("Unexpected payment test for %s",item);
	case ResourceAndKnowledgeAndMoraleOrArtifact:
		// combo of michael the engineer and flartner the luddite
		if(artifacts.height()>0)
			{	if(totalResources()>0) { return(false); }	// have both, must decide
				return(doFlartner(replay));
			}
			// no artifacts, fall into michael the engineer resource test
		//$FALL-THROUGH$
	case ResourceAndKnowledgeAndMorale:
	case Resource:
		if(nKindsOfResource()==1)
		{
			EuphoriaChip shed = shedOneResource(replay);
			if(item==Cost.ResourceAndKnowledgeAndMorale)
			if((shed==EuphoriaChip.Gold))
				{	// special effect of MichaelTheEngineer
				G.Assert(recruitAppliesToMe(RecruitChip.MichaelTheEngineer),"should be MichaelTheEngineer");				
				incrementKnowledge(replay);
				incrementMorale();
				b.logGameEvent(MichaelTheEngineerGold,getPlayerColor(),getPlayerColor());
				}
			else { b.logGameEvent(MichaelTheEngineerAny); }
			return(true);
		}
		return(false);
		
	// MatthewTheThief
	case WaterOrKnowledge:
		if(water.height()==0) 
			{ if(b.revision>=108)
				{
				G.Assert(knowledge<MAX_KNOWLEDGE_TRACK,"can gain knowledge");
				incrementKnowledge(replay);
				b.logGameEvent(MatthewTheThiefWater,getPlayerColor());
				}
			  return(true); 
			}
		if(knowledge==MAX_KNOWLEDGE_TRACK) { sendWater(1,replay); return(true); }
		return(false);
		
	case EnergyOrKnowledge:
		if(energy.height()==0) 
			{ if(b.revision>=108)
				{G.Assert(knowledge<MAX_KNOWLEDGE_TRACK,"can gain knowledge");
				incrementKnowledge(replay);
			  	b.logGameEvent(MatthewTheThiefEnergy,getPlayerColor());
				}
			  return(true); 
			}
		if(knowledge==MAX_KNOWLEDGE_TRACK) { sendEnergy(1,replay); return(true); }
		return(false);
		
	case FoodOrKnowledge:
		if(food.height()==0)
			{ if(b.revision>=108)
				{G.Assert(knowledge<MAX_KNOWLEDGE_TRACK,"can gain knowledge");
			     incrementKnowledge(replay);
			     b.logGameEvent(MatthewTheThiefFood,getPlayerColor());
				}
			  return(true); 
			}
		if(knowledge==MAX_KNOWLEDGE_TRACK) { sendFood(1,replay); return(true); }
		return(false);
		
	case IsEuphorian:
	case IsWastelander:
	case IsIcarite:
	case IsSubterran:
		// we come here to pay the cost, which is actually free to qualified workers.
		// we never get here if we're not qualified.
		return(true);
		
	case Mostly_Free:
	case Free: return(true);
	
	case SacrificeWorker:
		totalWorkers--;
		EuphoriaChip worker = newWorkers.removeTop();
		if(replay!=replayMode.Replay) { b.animateSacrificeWorker(newWorkers,(WorkerChip)worker);}
		return(true);
	case GoldOrArtifact:
		if(gold.height()==0) { return(doFlartner(replay)); }
		if(artifacts.height()>0) { return(false); }
		// or fall into regular gold
		//$FALL-THROUGH$
	case Gold:
	case ConstructionSiteGold:		// building markets
		sendGold(1,replay);
		return(true);
		
	case StoneOrArtifact:
		if(stone.height()==0) { return(doFlartner(replay)); }
		if(artifacts.height()>0) { return(false); }
		// or fall into regular gold	
		//$FALL-THROUGH$
	case ConstructionSiteStone:		// building markets
		sendStone(1,replay);
		return(true);
		
	case ClayOrArtifact:
		if(clay.height()==0) { return(doFlartner(replay)); }
		if(artifacts.height()>0) { return(false); }
		// or fall into regular clay	
		//$FALL-THROUGH$
	case ConstructionSiteClay:		// building markets
		sendClay(1,replay);
		return(true);
		
		
	case Foodx4:	
		sendFood(4,replay);
		return(true);
	case Food:	
		sendFood(1,replay);
		return(true);
		

	case Energyx4:
			commoditiesNeeded++;
		//$FALL-THROUGH$
	case Energyx3:
			commoditiesNeeded++;
			commoditiesNeeded++;
		//$FALL-THROUGH$
	case Energy:
			commoditiesNeeded++;
			sendEnergy(commoditiesNeeded,replay);
		return(true);
		
	case Waterx4:
		commoditiesNeeded++;
		//$FALL-THROUGH$
	case Waterx3:	// worker training
		commoditiesNeeded++;
		commoditiesNeeded++;
		//$FALL-THROUGH$
	case Water:
		commoditiesNeeded++;
		sendWater(commoditiesNeeded,replay);
		return(true);
		
	case Blissx4:
		commoditiesNeeded++;
		//$FALL-THROUGH$
	case Blissx3:	// worker training
		commoditiesNeeded++;
		commoditiesNeeded++;
		//$FALL-THROUGH$
	case Bliss:
		commoditiesNeeded++;
		sendBliss(commoditiesNeeded,replay);
		return(true);

	case Artifactx2:
		if((artifacts.height()==2)||allOneArtifactType())
		{	// only one way to do it.
			sendArtifacts(2,replay);
			return(true);
		}
		return(false);
	case Artifactx3Only:		// various markets.  Usually take 3 cards or a pair
		G.Assert(artifacts.height()>=3,"not enough artifacts");
		if((artifacts.height()==3) || allOneArtifactType())
			{ sendArtifacts(3,replay);
			  return(true);  
			}
		return(false);	// requires decision
		

	case ArtifactPair:
		{
		ArtifactChip ch = hasArtifactPair();
		G.Assert(ch!=null,"has an artifact pair");
		if(countArtifactPairs()==1) 
		{	// doesn't have 2 pair, so no choice in the matter
			sendArtifacts(2,replay);
			return(true);
		}
		return(false);	// has to interact
		}
		
	case ArtifactJackoTheArchivist_V2:
		if(artifacts.height()==1)
		{	// only one way to do it.  Otherwise he gets to choose
			if(b.revision<117)
				{G.Assert((knowledge+2)<=MAX_KNOWLEDGE_TRACK,"knowledge <=4");
				incrementKnowledge(replay);
				incrementKnowledge(replay);
				}
			sendArtifacts(1,replay);
			return(true);
		}
		return(false);	// must choose
		
	case Smart_Artifact:
		// this is used when brian the viticulturist has to discard a card
		// and he has no choice about which one to discard because he has
		// to preserve his pair.
		G.Assert((hasArtifactPair()!=null)
				&& ((artifacts.height()==3)
				&& !penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement)),"must be forced artifact");
		if(artifacts.chipAtIndex(0)==artifacts.chipAtIndex(1)) { artifacts.removeChipAtIndex(2); }
		else if(artifacts.chipAtIndex(0)==artifacts.chipAtIndex(2)) { artifacts.removeChipAtIndex(1); }
		else { artifacts.removeChipAtIndex(0); }
		return(true);
		
	case Morale_Artifactx3:
		if(morale<=artifacts.height()) 
			{ return(false); } 	// have to interact
		G.Assert(morale>=2,"morale>=2");
		decrementMorale();
		//$FALL-THROUGH$
	case Artifactx3:
	case Mostly_Artifactx3:
		if(penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement))
		{
			return(payCost(Cost.Artifactx3Only,replay));
		}
		if(allOneArtifactType()) 
			{  
			return(payCost(Cost.ArtifactPair,replay));
			}
		if((artifacts.height()==3)
				&&(countArtifactPairs()==0)) 
			{ 
			return(payCost(Cost.Artifactx3Only,replay)); 
			}
		return(false);
		
	case Morale_Resourcex3:
		G.Assert(morale>=2,"morale>=2");
		decrementMorale();
		//$FALL-THROUGH$
	case Resourcex3:	// nimbus loft
	case Mostly_Resourcex3:
		if((totalResources()==3) || (nKindsOfResource()==1))
		{	int count = 0;
			while(count<3)
			{	shedOneResource(replay);
				count++;
			}
			return(true);
		}
		return(false);
	case Morale_BlissOrFoodPlus1:
		G.Assert(morale>=2,"morale>=2");
		decrementMorale();

		//$FALL-THROUGH$
	case BlissOrFoodPlus1:	// breeze bar and sky lounge with BrianTheViticulturist
		if((food.height()>0)							// have food
			&& (bliss.height()==0)						// have no bliss, so the substitution is required
			&& ((totalCommodities()==2) 		// we only have 2 commodities
					|| ((water.height()==0)&&(energy.height()==0)))	// or we only have food
			)
			{	// don't set hasUsedBrianTheViticulturist here, because it only 
				// counts for non-icarite markets
				shedOneCommodity(replay);
				shedOneCommodity(replay);
				b.logGameEvent(BrianTheViticulturistEffect);
				return(true);
			}
		// otherwise fall into standard bliss_commodity
		//$FALL-THROUGH$
	case Bliss_Commodity:	// breeze bar and sky lounge
	case Mostly_Bliss_Commodity:
		if((bliss.height()>0)							// not redundant because of BrianTheHorticulturist 
			&& ((b.revision>=101) 						// bug fix 7/6/2014 number of kinds of commodity, not the number of commodity
				? nKindsOfCommodityExceptBliss() 
				: totalCommoditiesExceptBliss())==1) 	// only have 1
		{
			sendBliss(1,replay);
			{
				shedOneCommodity(replay);
			}
			return(true);
		}
		return(false);
		
	case EnergyOrBlissOrFood:
		{ // pay with energy instead of food or bliss
		if(energy.height()>0)
			{
			if((bliss.height()==0) && (food.height()==0)) 
			{sendEnergy(1,replay);
			 return(true);
			}
			return(false);		// force interaction
			}
		}
		// or fall into blissorfood
		//$FALL-THROUGH$
	case BlissOrFood:		// pay for retrieval
		
		if(bliss.height()==0) { if(food.height()>0) { sendFood(1,replay); return(true); } } 
		else if(food.height()==0) {sendBliss(1,replay); return(true); }
		return(false);		// force interaction
		
	/* prices for open markets */
	case Energyx4_StoneOrBlissOrFood:
		if(food.height()>0)
			{
			if((bliss.height()==0) && (stone.height()==0)) { sendFood(1,replay); sendEnergy(4,replay); return(true); }
			return(false);	// must interact
			}
		// or fall into stoneOrBliss
		//$FALL-THROUGH$
	case Energyx4_StoneOrBliss:
		if(bliss.height()>0)
		{
			if(stone.height()>0) { return(false); }	// interact
			sendBliss(1,replay);
			sendEnergy(4,replay);
			b.logGameEvent(JoshTheNegotiatorEffect);
			return(true);
		}
		// fall into energyx4_stone
		//$FALL-THROUGH$
	case Energyx4_Stone:
		{	b.addStone(stone.removeTop());
			b.animateReturnStone(stone);
			sendEnergy(4,replay);
			return(true);
		}

	case Waterx4_ClayOrBlissOrFood:
		if(food.height()>0)
		{
			if((clay.height()==0)&&(bliss.height()==0)) { sendWater(4,replay); sendFood(1,replay); return(true); }
			return(false); 	// have to interact
		}
		// or fall into ClayOrBliss
		//$FALL-THROUGH$
	case Waterx4_ClayOrBliss:
		if(bliss.height()>0)
		{
			if(clay.height()>0) { return(false); } // interact
			sendBliss(1,replay);
			sendWater(4,replay);
			b.logGameEvent(JoshTheNegotiatorEffect);
			return(true);
		}
		// or fall into regular waterx4_clay
		//$FALL-THROUGH$
	case Waterx4_Clay:
		{	sendClay(1,replay);
			sendWater(4,replay);
			return(true);
		}
	
	case Waterx4_GoldOrBlissOrFood:
		if(food.height()>0)
		{
			if((bliss.height()==0) && (gold.height()>0)) { sendWater(4,replay); sendFood(1,replay); return(true); }
			return(false); 	// have to interact
		}
		// fall into goldOrBliss
		//$FALL-THROUGH$
	case Waterx4_GoldOrBliss:
		if(bliss.height()>0)
		{
			if(gold.height()>0) { return(false); } 	// interact
			sendBliss(1,replay);
			sendWater(4,replay);
			b.logGameEvent(JoshTheNegotiatorEffect);
			return(true);
		}
		// else fall into regular waterx4_gold
		//$FALL-THROUGH$
	case Waterx4_Gold:
		{	sendGold(1,replay);
			sendWater(4,replay);
			return(true);
		}
	case Foodx4_GoldOrBliss:
		if(bliss.height()>0)
		{
			if(gold.height()>0) { return(false); } 	// interact
			sendBliss(1,replay);
			sendFood(4,replay);
			return(true);
		}
		// else fall into regular foodx4_gold
		//$FALL-THROUGH$
	case Foodx4_Gold:
		{
		sendGold(1,replay);
		sendFood(4,replay);
		return(true);
		}

	case Energyx4_ClayOrBlissOrFood:
		if(food.height()>0)
		{	if((clay.height()==0)&&(bliss.height()==0)) { sendEnergy(4,replay); sendFood(1,replay); return(true); } 
			return(false);	// have to interact
		}
		// or fall into regular clayOrbliss
		//$FALL-THROUGH$
	case Energyx4_ClayOrBliss:
		if(bliss.height()>0)
		{
			if(clay.height()>0) { return(false); } 	// interact
			sendBliss(1,replay);
			sendEnergy(4,replay);
			b.logGameEvent(JoshTheNegotiatorEffect);

			return(true);
		}
		// else fall into regular energyx4_clay
		//$FALL-THROUGH$
	case Energyx4_Clay:
		{
		sendClay(1,replay);
		sendEnergy(4,replay);
		return(true);
		}


	case Foodx4_StoneOrBlissOrFood:
		if(food.height()>0)
		{
		if((stone.height()==0)&&(bliss.height()==0)&&(food.height()>=5)) { sendFood(5,replay); return(true); }
		return(false);	// have to interact
		}
		// or fall into stone or bliss
		//$FALL-THROUGH$
	case Foodx4_StoneOrBliss:
		if(bliss.height()>0)
		{
			if(stone.height()>0) { return(false); } 	// interact
			sendBliss(1,replay);
			sendFood(4,replay);
			b.logGameEvent(JoshTheNegotiatorEffect);

			return(true);
		}
		// else fall into regular foodx4_stone
		//$FALL-THROUGH$
	case Foodx4_Stone:
		{
		sendStone(1,replay);
		sendFood(4,replay);
		return(true);
		}
	case Card_ResourceOrBlissOrFood:
		if(!allOneArtifactType()) { return(false); }	// choice of artifact is implied
		if(nKindsOfResource()>1) { return(false); }		// choice of resource is implied
		if(food.height()>0)
		{	if(nKindsOfResource()>0) { return(false); }
			if(bliss.height()>0) { return(false); }
			sendFood(1,replay);
			sendArtifacts(1,replay);
			return(true);
		}
		// no food, try bliss and resource
		//$FALL-THROUGH$
	case Card_ResourceOrBliss:
		if(!allOneArtifactType()) { return(false); }
		if(nKindsOfResource()>1) { return(false); }
		if(bliss.height()>0)
		{	if(nKindsOfResource()>0) { return(false); }
			sendBliss(1,replay);
			sendArtifacts(1,replay);
			b.logGameEvent(JoshTheNegotiatorEffect);
			return(true);
		}
		// else fall into regular card_resource
		//$FALL-THROUGH$
	case Card_Resource:
		if(!allOneArtifactType()) { return(false); }
		if(nKindsOfResource()>1) { return(false); }
		sendArtifacts(1,replay);
		shedOneResource(replay);
		return(true);
		
	case Waterx4_Card:
		if(!allOneArtifactType()) { return(false); }
		sendArtifacts(1,replay);
		sendWater(4,replay);
		return(true);

	case Energyx4_Card:
		if(!allOneArtifactType()) { return(false); }
		sendArtifacts(1,replay);
		sendEnergy(4,replay);
		return(true);
		
	case Foodx4_Card:
		if(!allOneArtifactType()) { return(false); }
		sendArtifacts(1,replay);
		sendFood(4,replay);
		return(true);
		
	case BlissOrFoodx4_Resource:
		if(nKindsOfResource()!=1) { return(false); }	// interact
		{
		int blissH =  bliss.height();
		int foodH = food.height();
		int blissandfood = blissH+foodH;
		if(foodH>0)
		{
		if((blissH==0) || (blissandfood==4))
			{
				if(blissandfood>=4)
				{	// use all the available bliss and food for the rest
					sendBliss(blissH,replay);
					sendFood(4-blissH,replay);
					shedOneResource(replay);
					useBrianTheViticulturist();
					return(true);
				}
			}}
			return(false);
		}
		// fall into standard blissx4_resource
	case Blissx4_Resource:
		
		if(nKindsOfResource()==1)
		{	shedOneResource(replay);
			sendBliss(4,replay);
			return(true);
		}
		return(false);

	case BlissOrFoodx4_ResourceOrBlissOrFood:
		{
		int nFood = food.height();
		if(nFood>0)
		{
		// effectively, at most one resource, plus any mix of food or bliss to make 5
		int nRes = nKindsOfResource();
		int nBliss = bliss.height();
		if(nRes==0) 		 // no resources
			{ if(nBliss==0) { sendFood(5,replay); return(true); }
			  if((nFood+nBliss)==5) {sendFood(nFood,replay); sendBliss(nBliss,replay); return(true); }
			  return(false); 	// interact
			}
		if(nRes==1)
			{	if((nFood+nBliss)==4) {  shedOneResource(replay); sendFood(nFood,replay); sendBliss(nBliss,replay); return(true); }
			}	
		return(false);
		}}
		// or fall into the no food case
		//$FALL-THROUGH$
	case Blissx4_ResourceOrBliss:
		{
		int nRes = nKindsOfResource();
		if(nRes==0) 
			{ sendBliss(5,replay);
			b.logGameEvent(JoshTheNegotiatorEffect);
			return(true); 
			}
		else if((nRes==1)&&(bliss.height()==4)) 
			{ shedOneResource(replay);
			  sendBliss(4,replay);
			  b.logGameEvent(JoshTheNegotiatorEffect);
			  return(true); 
			}
		return(false);
		}
	case BlissOrFoodx4_Card:
		if(!allOneArtifactType()) { return(false); }	// interact
		{
		int blissH =  bliss.height();
		int foodH = food.height();
		int blissandfood = blissH+foodH;
		if(foodH>0)
		{
			if((blissH==0) || (blissandfood==4))
			{
				if(blissandfood>=4)
				{	// use all the available bliss and food for the rest
					sendBliss(blissH,replay);
					sendFood(4-blissH,replay);
					sendArtifacts(1,replay);
					useBrianTheViticulturist();
					return(true);
				}
			}}
		return(false); 
		}
		// fall into standard blissx4_card
	case Blissx4_Card:
		if(allOneArtifactType())
			{
			sendBliss(4,replay);
			sendArtifacts(1,replay);
			return(true);
			}
		return(false);
	case Commodity_Bear:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			b.addArtifact(artifacts.removeChip(ArtifactChip.Bear));
			if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }
			return(true);
		}
		return(false);
	case Commodity_Bifocals:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			b.addArtifact(artifacts.removeChip(ArtifactChip.Bifocals));
			if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }
			return(true);
		}
		return(false);	
	case Commodity_Balloons:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			b.addArtifact(artifacts.removeChip(ArtifactChip.Balloons));
			if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }
			return(true);
		}
		return(false);
	case Commodity_Box:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			b.addArtifact(artifacts.removeChip(ArtifactChip.Box));
			if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }
			return(true);
		}
		return(false);
	case Commodity_Bat:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			b.addArtifact(artifacts.removeChip(ArtifactChip.Bat));
			if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }
			return(true);
		}
		return(false);
	case Commodity_Book:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			b.addArtifact(artifacts.removeChip(ArtifactChip.Book));
			if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }
			return(true);
		}
		return(false);


		// artifact costs
	case Bear:
		return(artifactChip(ArtifactChip.Bear,replay));
			
	case Box:
		return(artifactChip(ArtifactChip.Box,replay));
			
	case Book:
		return(artifactChip(ArtifactChip.Book,replay));
			
	case Bifocals:
		return(artifactChip(ArtifactChip.Bifocals,replay));
		
	case Bat:
		return(artifactChip(ArtifactChip.Bat,replay));
			
	case Balloons:
		return(artifactChip(ArtifactChip.Balloons,replay));

	// artifact costs
	case BearOrCardx2:
		return(artifactx2OrSpecific(ArtifactChip.Bear,replay));
		
	case BoxOrCardx2:
		return(artifactx2OrSpecific(ArtifactChip.Box,replay));
		
	case BookOrCardx2:
		return(artifactx2OrSpecific(ArtifactChip.Book,replay));
		
	case BifocalsOrCardx2:
		return(artifactx2OrSpecific(ArtifactChip.Bifocals,replay));
	
	case BatOrCardx2:
		return(artifactx2OrSpecific(ArtifactChip.Bat,replay));
		
	case BalloonsOrCardx2:
		return(artifactx2OrSpecific(ArtifactChip.Balloons,replay));

	case Knowledge:
		G.Assert((knowledge+1)<=MAX_KNOWLEDGE_TRACK,"knowledge <=2");
		incrementKnowledge(replay);
		return(true);
		
	case Knowledgex2:
		G.Assert((knowledge+2)<=MAX_KNOWLEDGE_TRACK,"knowledge <=4");
		incrementKnowledge(replay);
		incrementKnowledge(replay);
		return(true);
	case Morale:
		G.Assert(morale>=2,"morale>=2");
		decrementMorale();
		return(true);
	case Moralex2:
		G.Assert(morale>=3,"morale>=3");
		decrementMorale();
		decrementMorale();
		return(true);
	//
	// these are not normally paid per se, but are extracted
	// as a penalty when morale declines.
	//
	case Cardx6:
		if(allOneArtifactType() || (artifacts.height()==6)) { shedCards(6,replay) ; return(true); };
		return(false);
	case Cardx5:
		if(allOneArtifactType() || (artifacts.height()==5)) { shedCards(5,replay) ; return(true); };
		return(false);

	case Cardx4:
		if(allOneArtifactType() || (artifacts.height()==4)) { shedCards(4,replay) ; return(true); };
		return(false);
	case Cardx3:
		if(allOneArtifactType() || ( (artifacts.height()==3) && (hasArtifactPair()==null))) { shedCards(3,replay) ; return(true); };
		return(false);
	case Cardx2:
	case CardForGeekx2:
		if(allOneArtifactType() || (artifacts.height()==2)) { shedCards(2,replay) ; return(true); };
		return(false);
	case Card:
	case Artifact:
	case CardForGeek:
		if(allOneArtifactType() || (artifacts.height()==1)) { shedCards(1,replay) ; return(true); };
		return(false);
		
	// unlike the rest of the costs, these are an optional penalty,
	// which you only have to pay what you have.
	case CommodityOrResourcex4Penalty:	commoditiesNeeded++;
		//$FALL-THROUGH$
	case CommodityOrResourcex3Penalty: commoditiesNeeded++;
		//$FALL-THROUGH$
	case CommodityOrResourcex2Penalty:	commoditiesNeeded++;
		//$FALL-THROUGH$
	case CommodityOrResourcePenalty:	commoditiesNeeded++;
		{
		int tot = nKindsOfCommodityOrResource();
		lostGoods += commoditiesNeeded;
		if(tot==0) { return(true); } 	// paid nothing from nothing
		int sum = totalCommodities()+totalResources();
		if((tot==1)||(sum<=commoditiesNeeded)) { payCommodityOrResource(Math.min(commoditiesNeeded,sum),replay); return(true); }	// no choice
		else { return(false); }
		}
	}
}

// get different mixes of food and knowledge depending
// on the knowledge of the workers in the farm.
boolean doFoodSelection(EuphoriaBoard b,replayMode replay)
{	int know = b.totalKnowledgeOnFarm();
	if(hasActiveRecruit(Allegiance.Wastelander) 
			&& b.getAllegianceValue(Allegiance.Wastelander)>=ALLEGIANCE_TIER_1)
	{	// get extra food if you have the correct allegiance
		boolean secrets = penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets);
		if(secrets) {MarketChip.RegistryOfPersonalSecrets.logGameEvent(b,NoExtraFood); }
		else 
		{ addFood(b.getFood()); 
			if(replay!=replayMode.Replay)
				{  b.animateNewFood(food);
				}
			}
		}
	if(know<=4) 
	{
		addFood(b.getFood());
		b.incrementAllegiance(Allegiance.Wastelander,replay);
		if(replay!=replayMode.Replay) { b.animateNewFood(food); }
	}
	else if(know<=8)
	{
		addFood(b.getFood());
		decrementKnowledge();
		if(replay!=replayMode.Replay) { b.animateNewFood(food); }
	}
	else
	{
		addFood(b.getFood());
		addFood(b.getFood());
		if(replay!=replayMode.Replay) { b.animateNewFood(food);  b.animateNewFood(food); }
		incrementKnowledge(replay);
	}
	return(true);
}
boolean hasMyAuthorityToken(EuphoriaCell c)
{
	return(c.containsChip(myAuthority));
}
boolean penaltyAppliesToMe(MarketChip c)
{	EuphoriaCell m = b.getOpenMarketCell(c);
	if((m!=null) && !hasMyAuthorityToken(m)) { return(true); }
	return(false);
}
boolean recruitAppliesToMe(RecruitChip c)
{	if(mandatoryEquality) { return(false); }
	if(hasUsedGeekTheOracle && (c==RecruitChip.GeekTheOracle)) { return(false); }	// once per turn
	return(activeRecruits.containsChip(c));
}

//get different mixes of energy and knowledge depending
//on the knowledge of the workers in the farm.
boolean doPowerSelection(EuphoriaBoard b,replayMode replay)
{	int know = b.totalKnowledgeOnGenerator();
	if(hasActiveRecruit(Allegiance.Euphorian) 
			&& b.getAllegianceValue(Allegiance.Euphorian)>=ALLEGIANCE_TIER_1)
	{	// get extra food if you have the correct allegiance
		boolean secrets = penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets);
		if(secrets) 
		{
			MarketChip.RegistryOfPersonalSecrets.logGameEvent(b,NoExtraEnergy);
		}
		else 
		{ energy.addChip(b.getEnergy()); 
		  if(replay!=replayMode.Replay) 
			{ b.animateNewEnergy(energy);  
			}
		}
	}
	if(know<=4) 
	{
		addEnergy(b.getEnergy());
		b.incrementAllegiance(Allegiance.Euphorian,replay);
		if(replay!=replayMode.Replay) { b.animateNewEnergy(energy); }
	}
	else if(know<=8)
	{
		addEnergy(b.getEnergy());
		decrementKnowledge();
		if(replay!=replayMode.Replay) { b.animateNewEnergy(energy); }
	}
	else
	{
		addEnergy(b.getEnergy());
		addEnergy(b.getEnergy());
		if(replay!=replayMode.Replay) { b.animateNewEnergy(energy); b.animateNewEnergy(energy);}
		incrementKnowledge(replay);
	}
	return(true);
}

//get different mixes of food and knowledge depending
//on the knowledge of the workers in the farm.
boolean doWaterSelection(EuphoriaBoard b,replayMode replay)
{	
	int know = b.totalKnowledgeOnAquifer();
	if(recruitAppliesToMe(RecruitChip.FaithTheElectrician))
	{
		if(artifacts.height()==0) 
			{	addEnergy(b.getEnergy());
				if(replay!=replayMode.Replay) { b.animateNewEnergy(energy); }
				b.logGameEvent(FaithTheHydroelectricianEffect);
			}
	}
	if(hasActiveRecruit(Allegiance.Subterran) 
			&& b.getAllegianceValue(Allegiance.Subterran)>=ALLEGIANCE_TIER_1)
	{	// get extra food if you have the correct allegiance
		boolean secrets = penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets);
		
		if(secrets)
		{MarketChip.RegistryOfPersonalSecrets.logGameEvent(b,NoExtraWater);
		}
		else
		{ addWater(b.getWater()); 
		
		if(replay!=replayMode.Replay) 
			{b.animateNewWater(water);  
			}
		}
	}
	if(know<=4) 
	{	
		addWater(b.getWater());
		if(replay!=replayMode.Replay) { b.animateNewWater(water); }
		b.incrementAllegiance(Allegiance.Subterran,replay);
	}
	else if(know<=8)
	{
		addWater(b.getWater());
		if(replay!=replayMode.Replay) { b.animateNewWater(water); }
		decrementKnowledge();
	}
	else
	{
		addWater(b.getWater());
		addWater(b.getWater());
		if(replay!=replayMode.Replay) { b.animateNewWater(water); b.animateNewWater(water); }
		incrementKnowledge(replay);
	}
	return(true);
}
//get different mixes of food and knowledge depending
//on the knowledge of the workers in the farm.
boolean doBlissSelection(EuphoriaBoard b,replayMode replay)
{	int know = b.totalKnowledgeOnCloudMine();
	if(hasActiveRecruit(Allegiance.Icarite) 
			&& b.getAllegianceValue(Allegiance.Icarite)>=ALLEGIANCE_TIER_1)
	{	// get extra food if you have the correct allegiance
		boolean secrets = penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets);
		if(secrets)  { MarketChip.RegistryOfPersonalSecrets.logGameEvent(b,NoExtraBliss); }
		else { addBliss(b.getBliss()); 
			if(replay!=replayMode.Replay) { b.animateNewBliss(bliss); }
			} 
	}
	if(know<=4) 
	{
		addBliss(b.getBliss());
		b.incrementAllegiance(Allegiance.Icarite,replay);
		if(replay!=replayMode.Replay) { b.animateNewBliss(bliss); }
	}
	else if(know<=8)
	{
		addBliss(b.getBliss());
		if(replay!=replayMode.Replay) { b.animateNewBliss(bliss); }
		decrementKnowledge();
	}
	else
	{
		addBliss(b.getBliss());
		addBliss(b.getBliss());
		if(replay!=replayMode.Replay) { b.animateNewBliss(bliss);  b.animateNewBliss(bliss);}
		incrementKnowledge(replay);
	}
	return(true);
}
// authority token and influence for the main territory track
private void doAuthorityAndInfluence(EuphoriaBoard b,Allegiance aa,replayMode replay,boolean influence)
{
		if(influence) { b.incrementAllegiance(aa,replay); }
		doAuthority(b,aa,replay);
}

private void doTheaterOfRevelatoryPropaganda(EuphoriaBoard b,replayMode replay)
{	if(penaltyAppliesToMe(MarketChip.TheaterOfRevelatoryPropaganda))
	{
	if(incrementKnowledge(replay))
		{
		if(replay!=replayMode.Replay)
			{ MarketChip.TheaterOfRevelatoryPropaganda.logGameEvent(b);
			}
		}
	
	}
}

private void doAuthority(EuphoriaBoard b,Allegiance aa,replayMode replay)
{		EuphoriaCell c = b.getAvailableAuthorityCell(aa);
		if(c!=null)
		{	EuphoriaChip chip = getAuthorityToken();
			if(chip!=null)
			{
			b.placeAuthorityToken(c,chip);
			doTheaterOfRevelatoryPropaganda(b,replay);
			if(replay!=replayMode.Replay) { b.animatePlacedItem(authority,c); }
			}
		}
}
// authority token and influence for a market cell
private void doMarketAndInfluence(EuphoriaBoard b,replayMode replay,Allegiance aa,EuphoriaCell market,boolean influence)
{
		if(influence) { b.incrementAllegiance(aa,replay); }
		EuphoriaChip chip = getAuthorityToken();
		if(chip!=null)
		{
		b.placeAuthorityToken(market,chip);
		doTheaterOfRevelatoryPropaganda(b,replay);
		}
}
// just influence
private void doInfluence(EuphoriaBoard b,replayMode replay,Allegiance aa)
{
		b.incrementAllegiance(aa,replay);
}

void distributeResources(EuphoriaCell c)
{
	while(c.height()>0)
	{
		EuphoriaChip ch = c.removeTop();
		distributeItem(ch);
	}
}
//
// this should be the ONLY portal by which stuff is added
// to the player's stocks or resources, artifacts, or commodities.
//
void distributeItem(EuphoriaChip ch)
{
		if(ch==EuphoriaChip.Clay) { addClay(ch); }
		else if(ch==EuphoriaChip.Gold) { addGold(ch); }
		else if(ch==EuphoriaChip.Stone) { addStone(ch); }
		else if(ch.isArtifact()) { addArtifact(ch); }
		else if(ch==EuphoriaChip.Water) { addWater(ch); }
		else if(ch==EuphoriaChip.Energy) { addEnergy(ch); }
		else if(ch==EuphoriaChip.Bliss) { addBliss(ch); }
		else { throw G.Error("Not expecting resource %s",ch); }
}
/**
 * confirm payment of the placement cost (and do whatever bookeeping needs to be done)
 * Note that "cost" is the original one, not the derived cost based on recruits and
 * partial payments of fixed costs.  "actualCost" is the actual payment being made.
 * and fixed resources.
 * @param b
 * @param cost
 * @param actualCost
 * @param dest
 */
void confirmPayment(Cost cost,Cost actualCost,CellStack dest,replayMode replay)
{	
	switch(actualCost)
	{
	 case StoneOrBliss:				// these are the one we expect to encounter, after the fixed costs
	 case ClayOrBliss:
	 case GoldOrBliss:
	 case ResourceOrBliss:
	 case Energyx4_StoneOrBliss:	// these are the raw costs including the fixed part.
	 case Waterx4_ClayOrBliss:
	 case Waterx4_GoldOrBliss:
	 case Blissx4_ResourceOrBliss:
	 case Energyx4_ClayOrBliss:
	 case Foodx4_StoneOrBliss:
	 case Card_ResourceOrBliss:
			// brian the viticulturist lets us use food instead of bliss
	 		{
			boolean hasBliss = false;
			for(int lim=dest.size()-1; lim>=0; lim--)
			{
				hasBliss |= (dest.elementAt(lim).rackLocation()==EuphoriaId.BlissPool);
			}
			if(hasBliss) { b.logGameEvent(JoshTheNegotiatorEffect); }
	 		}
			break;
	case BlissOrFoodx4_Card:
	case BlissOrFoodx4_ResourceOrBlissOrFood:
	case Energyx4_StoneOrBlissOrFood:
	case Waterx4_ClayOrBlissOrFood:
	case Waterx4_GoldOrBlissOrFood:
	case Energyx4_ClayOrBlissOrFood: 
 	case Foodx4_StoneOrBlissOrFood: 
 	case Card_ResourceOrBlissOrFood:
	case BlissOrFoodx4_Resource:
	case Morale_BlissOrFoodPlus1:
	case BlissOrFoodPlus1:
		{
		// brian the viticulturist lets us use food instead of bliss
		boolean hasFood = false;
		boolean hasBliss = false;
		for(int lim=dest.size()-1; lim>=0; lim--)
		{	EuphoriaId rack = dest.elementAt(lim).rackLocation();
			switch(rack)
			{ 
			case FarmPool: hasFood = true; break;
			case BlissPool: hasBliss = true; break;
			default: break;
			}
		}
		if(hasFood) { b.logGameEvent(BrianTheViticulturistEffect); }
		if(hasBliss) { b.logGameEvent(JoshTheNegotiatorEffect); }
		}
		break;
	case GoldOrArtifact:
	case StoneOrArtifact:
	case ClayOrArtifact:
		if(dest.top().rackLocation()==EuphoriaId.ArtifactDiscards)
		{	incrementMorale();
			b.logGameEvent(FlartnerTheLudditeEffect,getPlayerColor());
		}
		break;
		
	case ResourceAndKnowledgeAndMoraleOrArtifact:	// either michael the engineer or flartner the luddite
		if(dest.top().topChip().isArtifact())
		{
			G.Assert(recruitAppliesToMe(RecruitChip.FlartnerTheLuddite),"should be flartner");
			incrementMorale();
			b.logGameEvent(FlartnerTheLudditeEffect,getPlayerColor());
			break;
		}
		//$FALL-THROUGH$
	case ResourceAndKnowledgeAndMorale:
		G.Assert(recruitAppliesToMe(RecruitChip.MichaelTheEngineer),"should be michael");
		if(dest.top().topChip()==EuphoriaChip.Gold)
			{
			incrementKnowledge(replay);
			incrementMorale();
			b.logGameEvent(MichaelTheEngineerGold,getPlayerColor(),getPlayerColor());
			}
			else
			{	b.logGameEvent(MichaelTheEngineerAny);
			}
		
		break;
	default: break;
	}
	switch(cost)
	{
	case Foodx4_Gold:		// if ended up with only food, we must have used brian
		if(b.containsNFood(dest)==5) { useBrianTheViticulturist(); }
		break;
	case Energyx4_Stone:	// it takes both BrianTheViticulturist and JoshTheNegotiator to get these here.
	case Waterx4_Gold:
	case Blissx4_Card:
	case Energyx4_Clay:
	case Waterx4_Clay:
	case Foodx4_Stone:
	case Blissx4_Resource:
		if(b.containsFood(dest)) { useBrianTheViticulturist(); }	// needed for awarding of influence
		break;
	case Bliss_Commodity:
	case Mostly_Bliss_Commodity:
	case Cardx6:
	case Cardx5:
	case Cardx4:
	case Cardx3:
	case Cardx2:
	case Card:
	case Artifact:
	case ConstructionSiteStone:
	case ConstructionSiteGold:
	case Gold:
	case ConstructionSiteClay:
	case Card_Resource:
	case BlissOrFood:
	case NonBlissCommodity:
	case Artifactx3:
	case Morale_Artifactx3:
	case Smart_Artifact:
	case Mostly_Artifactx3:
	case Commodity_Balloons:
	case Commodity_Bear:
	case Commodity_Bat:
	case Commodity_Book:
	case Commodity_Bifocals:
	case Commodity_Box:
	case Resourcex3:
	case Mostly_Resourcex3:
	case CommodityOrResourcePenalty:
	case Foodx4_Card:
	case CardForGeek:
	case CardForGeekx2:
	case CommodityOrResourcex2Penalty:
	case CommodityOrResourcex3Penalty:
	case CommodityOrResourcex4Penalty:
	case BearOrCardx2:
	case BoxOrCardx2:
	case BifocalsOrCardx2:
	case BalloonsOrCardx2:
	case BatOrCardx2:
	case BookOrCardx2:
	case Energyx4_Card:
	case Waterx4_Card:
	case Artifactx2:
		break;
	default: throw G.Error("not expecting %s",cost);
	}
}
private void artifactIsDest(CellStack dest)
{
	for(int lim=dest.size()-1; lim>=0; lim--)
	{
		EuphoriaCell c = dest.elementAt(lim);
		if(c.rackLocation()==EuphoriaId.PlayerArtifacts) { hasAddedArtifact = true; }
	}
}
/**
 * come here to finalize a benefit that involved a user interaction.  Put the stuff in
 * it's final resting place (if not already there) and adjust any non-tangibles such
 * as morale.  This also sets up animation and notes "game events" when hidden things
 * have happened.
 * 
 * @param b
 * @param replay
 * @param benefit
 * @param dest
 */
// after the benefit choice dialog, come back here with c as a stack of the selections.
// c can be null if we used the "normal" ui to collect the benefits
// always call this to collect other non-optional benefits associated with the benefit.
//
void satisfyBenefit(replayMode replay,Benefit benefit,CellStack dest)
{	artifactIsDest(dest); 	// check for artifacts
	switch(benefit)
	{
	case MoraleOrKnowledge:
		{
		EuphoriaCell top = dest.top();
		EuphoriaChip ch = top.topChip();
		if(ch==EuphoriaChip.getKnowledge(color)) 
			{ decrementKnowledge(); b.logGameEvent(EsmeTheFiremanKnowledge,getPlayerColor()); }
		else if(ch==EuphoriaChip.getMorale(color)) { incrementMorale(); b.logGameEvent(EsmeTheFiremanMorale,getPlayerColor()); }
		else {throw G.Error("not expecting %s",ch); }
		top.removeTop();	// clear the heart or head from the player temp cell
		}
		break;
	case Moralex2OrKnowledgex2:
		{
		EuphoriaCell top = dest.top();
		EuphoriaChip ch = top.topChip();
		if(ch==EuphoriaChip.getKnowledge(color)) 
			{decrementKnowledge();
			 decrementKnowledge();
			 b.logGameEvent(EsmeTheFiremanKnowledgex2,getPlayerColor()); }
		else if(ch==EuphoriaChip.getMorale(color))
			{ incrementMorale(); 
			 incrementMorale(); 
			 b.logGameEvent(EsmeTheFiremanMoralex2,getPlayerColor()); 
			 }
		else { throw G.Error("not expecting %s",ch); }
		top.removeTop();	// clear the heart or head from the player temp cell
		}
		break;
	case EuphorianAuthority2:
	case WastelanderAuthority2:
	case SubterranAuthority2:
		// we placed a star manually, also add influence
		b.incrementAllegiance(benefit.placementZone(),replay);	
		switch(dest.top().rackLocation())
		{
		case Market:	marketStars++;
			break;
		default: break;
		}
		doTheaterOfRevelatoryPropaganda(b,replay);
		checkForMandatoryEquality(dest.top());
		break;
	case IcariteInfluenceAndResourcex2:
		b.incrementAllegiance(Allegiance.Icarite,replay);
		if(b.revision<109) { doTheaterOfRevelatoryPropaganda(b,replay); }
		break;
	case WastelanderAuthorityAndInfluence:
	case EuphorianAuthorityAndInfluence:
	case SubterranAuthorityAndInfluence:
		doTheaterOfRevelatoryPropaganda(b,replay);
		break;

	case MoraleOrEnergy:
		b.logGameEvent(GaryTheElectricianEnergy);
		break;
	case KnowledgeOrFood:
		b.logGameEvent(ScarbyTheHarvesterFood);
		break;
	case KnowledgeOrBliss:
		b.logGameEvent(SarineeTheCloudMinerBliss);
		break;
	case WaterOrMorale:
		b.logGameEvent(SoullessThePlumberWater);
		break;
	case WaterOrEnergy:
		b.logGameEvent( (dest.top().rackLocation()==EuphoriaId.PlayerWater)
						? ZongTheAstronomerWater
						: ZongTheAstronomerEnergy);
		break;
	case WaterOrStone:
		b.logGameEvent( (dest.top().rackLocation()==EuphoriaId.PlayerWater)
				? MaggieTheOutlawWater
				: MaggieTheOutlawStone);
		break;

	case CardAndStone:
	case CardAndGold:
	case CardAndClay:		// we can get here if MarketPenalty.NoAllegianceBonus applies
	case CardOrStone:
	case CardOrGold:
	case CardOrClay:
	case Commodity:
	case Resource:
	case Artifactx2for1:
		if(dest!=null)
			{G.Assert(dest.size()==1,"should be one resouce");
			}
		break;
	default: throw G.Error("Not expecting to collect deferred benefit %s",benefit);
	}
}


void distributeBenefit(replayMode replay,Benefit benefit,EuphoriaCell c)
{	satisfyBenefit(replay,benefit,null);
	distributeResources(c);
}
// place a star on one of the markets or in the market territory
// and gain influence on the authority track.
boolean collectMarketAuthority(EuphoriaBoard b,Allegiance a,replayMode replay,boolean influence)
{	EuphoriaCell mA = b.getMarketA(a);
	EuphoriaCell mB = b.getMarketB(a);
	EuphoriaCell mC = b.getAvailableAuthorityCell(a);
	
	// place a star on a market (if open) or on the authority territory
	boolean marketAvailableA = b.marketIsOpen(mA) && !hasAuthorityOnMarket(mA);
	boolean marketAvailableB = b.marketIsOpen(mB) && !hasAuthorityOnMarket(mB);
	boolean territoryAvailable = mC!=null;
	int sum = (marketAvailableA ? 1 :0) + (marketAvailableB ? 1 : 0) + (territoryAvailable ? 1 : 0);
	
	if(sum<=1)
		{	// no way or 1 way to do it.
		if(marketAvailableA) { doMarketAndInfluence(b,replay,a,mA,influence); }
			else if(marketAvailableB) { doMarketAndInfluence(b,replay,a,mB,influence); }
			else if(territoryAvailable) { doAuthorityAndInfluence(b,a,replay,influence); }
			else { doInfluence(b,replay,a); }	// not an error if there is no place to put a star
		return(true);
		}
		return(false);	// have to decide
}
private void doBliss(int n,EuphoriaBoard b,replayMode replay)
{
	for(int i=0;i<n;i++) { addBliss(b.getBliss()); if(replay!=replayMode.Replay) { b.animateNewBliss(bliss); }}
}
private void doFood(int n,EuphoriaBoard b,replayMode replay)
{
	for(int i=0;i<n;i++) { addFood(b.getFood()); if(replay!=replayMode.Replay) { b.animateNewFood(food); }}
}
private void doWater(int n,EuphoriaBoard b,replayMode replay)
{
	for(int i=0;i<n;i++) { addWater(b.getWater()); if(replay!=replayMode.Replay) { b.animateNewWater(water); }}
}
private void doEnergy(int n,EuphoriaBoard b,replayMode replay)
{
	for(int i=0;i<n;i++) { addEnergy(b.getEnergy()); if(replay!=replayMode.Replay) { b.animateNewEnergy(energy); }}
}
private void doClay(int n,EuphoriaBoard b,replayMode replay)
{	while(n-- > 0)
	{
	addClay(b.getClay());		
	if(replay!=replayMode.Replay) { b.animateNewClay(clay); }
	}
}
private void doGold(int n,EuphoriaBoard b,replayMode replay)
{	while(n-- > 0)
	{
	addGold(b.getGold());		
	if(replay!=replayMode.Replay) { b.animateNewGold(gold); }
	}
}
private void doStone(int n,EuphoriaBoard b,replayMode replay)
{	while(n-- > 0)
	{
	addStone(b.getStone());		
	if(replay!=replayMode.Replay) { b.animateNewStone(stone); }
	}
}
/**
 *  return true if the choice action is fully resolved, 
 * @param benefit
 * @param b
 * @param replay
 * @return false if we need to interact with the user to resolve it.
 */
boolean collectBenefit(Benefit benefit,replayMode replay)
{	
	switch(benefit)
	{
	default: throw G.Error("Unexpected benefit code %s",benefit);
	case None:	
		return(true);
	case Artifactx2for1:	// always interact
		return(false);
	case Stonex2:
		doStone(2,b,replay);
		return(true);
	case Goldx2:
		doGold(2,b,replay);
		return(true);
	case Clayx2:
		doClay(2,b,replay);
		return(true);
	case Bliss: 	//AmandaTheBroker
		doBliss(1,b,replay);
		return(true);
	case Blissx4:	//ShepphardTheLobotomist
		doBliss(4,b,replay);
		return(true);
	case Commodity:
	case Resource:
	case WaterOrStone:
	case WaterOrEnergy:
		return(false);
	case Artifactx2:	// collect 2 artifact cards
		addArtifact(b.getArtifact());
		if(replay!=replayMode.Replay) { b.animateNewArtifact(artifacts); }
		// fall into one card
		//$FALL-THROUGH$
	case Artifact:	// collect a single card
		addArtifact(b.getArtifact());
		if(replay!=replayMode.Replay) { b.animateNewArtifact(artifacts); }
		return(true);
		
	case FoodSelection:
		return(doFoodSelection(b,replay));
	case PowerSelection:
		return(doPowerSelection(b,replay));
	case Water:
		doWater(1,b,replay);
		return(true);
	case WaterSelection:
		return(doWaterSelection(b,replay));
	case BlissSelection:
		return(doBlissSelection(b,replay));
	case NewWorkerAndKnowledge:
		
		if( (totalWorkers>=2) 
				&& (totalWorkers<=3)
				&& penaltyAppliesToMe(MarketChip.LaboratoryOfSelectiveGenetics)
				)
		{
			MarketChip.LaboratoryOfSelectiveGenetics.logGameEvent(b);
		}
		else if(totalWorkers<MAX_WORKERS)
		{	// pretty expensive, but you can do it
			addNewWorker(WorkerChip.getWorker(color,1));	// will be rerolled
			if(replay!=replayMode.Replay)
			{
				b.animateNewWorkerA(newWorkers);
			}
			totalWorkers++;
			hasGainedWorker=true;
		}
		decrementKnowledge();
		decrementKnowledge();	
		return(true);
		
	case NewWorkerAndMorale:
		if((totalWorkers>=2) 
				&& (totalWorkers<=3)
				&& penaltyAppliesToMe(MarketChip.LaboratoryOfSelectiveGenetics))
		{
			MarketChip.LaboratoryOfSelectiveGenetics.logGameEvent(b); 
		}
		else if(totalWorkers<MAX_WORKERS)
		{	// only if there's room
			addNewWorker(WorkerChip.getWorker(color,1));	// will be rerolled
			if(replay!=replayMode.Replay)
			{
				b.animateNewWorkerB(newWorkers);
			}	
		totalWorkers++;
		hasGainedWorker=true;
		}	
		incrementMorale();
		incrementMorale();
		return(true);
		
	case CardAndGold:
		{
		if(penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets))
		{
		MarketChip.RegistryOfPersonalSecrets.logGameEvent(b); 
		return(collectBenefit(Benefit.CardOrGold,replay));
		}
		else
		{
		addGold(b.getGold());
		EuphoriaChip art = b.getArtifact();
		if(art!=null) {addArtifact(art); }	// should never be null, but..
		if(replay!=replayMode.Replay) { b.animateNewGold(gold); if(art!=null) { b.animateNewArtifact(artifacts); }} 

		}}
		return(true);
		
	case CardAndStone:
		if(penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets))
		{
		MarketChip.RegistryOfPersonalSecrets.logGameEvent(b); 
		return(collectBenefit(Benefit.CardOrStone,replay));
		}
		else
		{
		 addStone(b.getStone());
		 EuphoriaChip art = b.getArtifact();
		 if(art!=null) { addArtifact(art); }	// should never be null, but..		
		 if(replay!=replayMode.Replay) { b.animateNewStone(stone); if(art!=null) { b.animateNewArtifact(artifacts); }} 
		}
		return(true);
	case CardAndClay:
		if(penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets))
		{
		MarketChip.RegistryOfPersonalSecrets.logGameEvent(b); 
		return(collectBenefit(Benefit.CardOrClay,replay));
		}
		else
		{
		 addClay(b.getClay());
		 EuphoriaChip art = b.getArtifact();
		 if(art!=null) { addArtifact(art); }	// should never be null, but..		
		 if(replay!=replayMode.Replay) { b.animateNewClay(clay); if(art!=null) { b.animateNewArtifact(artifacts); }} 
		}
		return(true);
	case Waterx3:
		doWater(3,b,replay);
		return(true);
	case Energy:
		doEnergy(1,b,replay);
		return(true);
	case Energyx2:
		doEnergy(2,b,replay);
		return(true);
		
	case Energyx3:
		doEnergy(3,b,replay);
		return(true);
	case Foodx4:
		doFood(4,b,replay);
		return(true);
	case Foodx3:
		doFood(3,b,replay);
		return(true);
	case Food:
		doFood(1,b,replay);
		return(true);
	case MoraleOrKnowledge:
		if(morale == MAX_MORALE_TRACK)
		{	decrementKnowledge(); 
			b.logGameEvent(EsmeTheFiremanKnowledge,getPlayerColor());
			return(true); 
		} if(knowledge==1)
			{ incrementMorale();
				b.logGameEvent(EsmeTheFiremanMorale,getPlayerColor());
			  return(true);
			}
		return(false);
	case Moralex2OrKnowledgex2:
		if(morale == MAX_MORALE_TRACK)
		{	decrementKnowledge();
			decrementKnowledge();
			b.logGameEvent(EsmeTheFiremanKnowledgex2,getPlayerColor());
			return(true); 
		} if(knowledge==1)
			{ incrementMorale();
			  incrementMorale();
			  b.logGameEvent(EsmeTheFiremanMoralex2,getPlayerColor());
			  return(true);
			}
		return(false);
		
	case Moralex2AndKnowledgex2:
		decrementKnowledge();
		decrementKnowledge();
		incrementMorale();
		incrementMorale();
		b.logGameEvent(EsmeTheFiremanKnowledgex2,getPlayerColor());
		return(true); 
		
	case IcariteInfluenceAndCardx2:
		{b.incrementAllegiance(Allegiance.Icarite,replay);
		 EuphoriaChip art = b.getArtifact();
		 if(art!=null) 
		 	{ addArtifact(art); 
		 	  art = b.getArtifact();
		 	  if(art!=null) { addArtifact(art); }
		 	  if(replay!=replayMode.Replay) { b.animateNewArtifact(artifacts); b.animateNewArtifact(artifacts); }
		 	}	// should never be null, but..		
		}
		return(true);

	case IcariteAuthorityAndInfluence:
		doAuthorityAndInfluence(b,Allegiance.Icarite,replay,true);
		if(hasActiveRecruit(Allegiance.Icarite) 
			&& (b.getAllegianceValue(Allegiance.Icarite)>=ALLEGIANCE_TIER_2))
			{
			if(penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets)) 
			{ MarketChip.RegistryOfPersonalSecrets.logGameEvent(b);
			}
			else 
			{ addArtifact(b.getArtifact()); 
			  if(replay!=replayMode.Replay)
			  	{ b.animateNewArtifact(artifacts); 
			  	}
			}
			
			}
		return(true);
		
	case EuphorianStar:
		doAuthority(b,Allegiance.Euphorian,replay);
		return(true);

	case WastelanderStar:
		doAuthority(b,Allegiance.Wastelander,replay);
		return(true);

	case SubterranStar:
		doAuthority(b,Allegiance.Subterran,replay);
		return(true);
		
	case EuphorianAuthorityAndInfluence:
		doAuthorityAndInfluence(b,Allegiance.Euphorian,replay,true);
		return(true);
		
	case WastelanderAuthorityAndInfluence:
		doAuthorityAndInfluence(b,Allegiance.Wastelander,replay,true);
		return(true);
		
	case SubterranAuthorityAndInfluence:
		doAuthorityAndInfluence(b,Allegiance.Subterran,replay,true);
		return(true);
		
	case WastelanderAuthority2:
		return(collectMarketAuthority(b,Allegiance.Wastelander,replay,true));
		
	case EuphorianAuthority2:
		return(collectMarketAuthority(b,Allegiance.Euphorian,replay,true));
		
	case SubterranAuthority2:
		return(collectMarketAuthority(b,Allegiance.Subterran,replay,true));

	case WaterOrMorale:		// soulless the plumber
		if(morale==MAX_MORALE_TRACK) { doWater(1,b,replay); return(true); }
		return(false);
		
	case Gold:	// LauraThePhilanthropist gives this away
		doGold(1,b,replay);
		return(true);
	case Clay:
		doClay(1,b,replay);
		return(true);
	case Stone:
		doStone(1,b,replay);
		return(true);
	case MoraleOrEnergy:
		if(morale==MAX_MORALE_TRACK)
		{
			doEnergy(1,b,replay);
			return(true);
		}
		return(false);
	case KnowledgeOrFood:	// scarby the harvester
		if(knowledge==1) 	// if knowledge is already at minimum, take food
		{
			doFood(1,b,replay);
			return(true);
		}
		return(false);
	case KnowledgeOrBliss:
		if(knowledge==1)
		{
			doBliss(1,b,replay);
			return(true);
		}
		return(false);
		


	case IcariteInfluenceAndResourcex2:
	case CardOrGold:
	case CardOrStone:
	case CardOrClay:
		return(false);		// interact to get these
		

	}

}
public boolean canResolveDilemma()
{	return(!dilemmaResolved && canPay(((DilemmaChip)dilemma.topChip()).cost));
}

}