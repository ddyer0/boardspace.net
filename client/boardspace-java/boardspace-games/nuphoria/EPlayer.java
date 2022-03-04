package nuphoria;


import java.util.Hashtable;

import lib.Random;
import online.game.replayMode;
import lib.AR;
import lib.G;
import lib.IntObjHashtable;


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
ArtifactChip usedAlternateArtifact = null;	// used an alternate as for AlexandraTheHeister
EuphoriaCell alternateArtifacts = null;

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

static final int KnowledgeLimit = 16;			// a constant, but can be modified by market penalty
int knowledge = 3;				// current knowledge
int morale = 1;					// current morale
int totalWorkers = 0;			// workers in hand + on board.  Limit is 4
int finalRandomizer = 0;		// final randomizer for this player, the final tie breaker
int marketStars = 0;			// authority tokens on markets
//
// these variables reflect events on the current turn
//
boolean hasLostAWorker = false;
private boolean hasAddedArtifact = false;
private boolean hasAddedArtifactLast = false;
private boolean hasPeeked = false;		// has seen the new card
private boolean hasUsedGeekTheOracle = false;
private boolean hasGainedWorker = false;
private boolean hasUsedBrianTheViticulturist = false;
boolean hasUsedDarrenTheRepeater = false;

private boolean mandatoryEquality = false;	// market penalty for mandatory equality
int lostToAltruism = 0;			// lost resources to altryism this turn
boolean someLostToAltruism = false;
int bearsGained = 0;
int balloonsGained = 0;
int energyGainedThisTurn = 0;
int commodityKnowledge = 0;	// differential for knowledge on commodity spaces, PmaiTheNurse

boolean hasUsedSamuelTheZapper = false;		// true when we reroll additional workers 
boolean usingSamuelTheZapper =false;		// true when we have paid and willretrieve
int samuelTheZapperLevel = 0;				// the number of retrieved workers when we start
boolean usingMwicheTheFlusher = false;		// true if actually paying mwiche 3 waters.
boolean askedShaheenaTheDigger = false;		// true if traded stone for artifact
int terriAuthorityHeight = 0;	//


private boolean triggerPedroTheCollector = false;	// true if uses 3 artifacts
public boolean getTriggerPedroTheCollector() { return triggerPedroTheCollector; }
public void setTriggerPedroTheCollector(boolean v) 
{ if(recruitAppliesToMe(RecruitChip.PedroTheCollector))
	{ triggerPedroTheCollector = v; }
}

public EuphoriaCell placedWorkers[] = new EuphoriaCell[MAX_WORKERS];
public void placeWorker(EuphoriaCell c)
{   WorkerChip top = (WorkerChip)c.topChip();
	b.Assert(top.color == color,"worker color matches");

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
	if((ch==RecruitChip.BokTheGameMaster)
		&& (knowledge>4))
	  { setKnowledge(4);
		//b.p1("BokTheGameMaster saves");
	  	b.logGameEvent(UseBokTheGameMasterNow);
	  }
}

// notification when a new market opens.
public void newMarketOpened()
{	checkMandatoryEquality();	// new market opened, maybe has a new penalty
	clearCostCache();
}

// clear status at the start of a new turn
public void startNewTurn(replayMode replay)
{	hasLostAWorker = false;
	hasAddedArtifactLast = hasAddedArtifact;
	hasAddedArtifact = false;
	hasPeeked = false;
	hasUsedGeekTheOracle = false;
	hasGainedWorker = false;
	bearsGained = 0;
	lostToAltruism = 0;
	someLostToAltruism = false;
	balloonsGained = 0;
	energyGainedThisTurn = 0;
	commodityKnowledge = 0;
	resultsInDoubles = false;
	hasUsedJonathanTheGambler = false;
	hasUsedBrianTheViticulturist = false;
	usingSamuelTheZapper = false;
	usingMwicheTheFlusher = false;
	usedAlternateArtifact = null;
	triggerPedroTheCollector = false;
	askedShaheenaTheDigger = false;
	usingMwicheTheFlusher = false;
	commodityKnowledge = 0;
	samuelTheZapperLevel = authority.height();
	if(hasUsedSamuelTheZapper)
	{	hasUsedSamuelTheZapper = false;
		collectBenefit(Benefit.Morale,replay);
	}
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
	  if(recruitAppliesToMe(RecruitChip.BokTheGameMaster))
	  {	  if(knowledge>4)
	  		{	knowledge = 4;
	  			b.logGameEvent(UseBokTheGameMaster);
	  		}
	  }
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
Benefit incrementMorale() 
{ 
	if(morale>=3 && penaltyAppliesToMe(MarketChip.IIB_ConcertHallOfHarmoniousDischord))
	{	MarketPenalty p = MarketChip.IIB_ConcertHallOfHarmoniousDischord.marketPenalty;
		//b.p1("inhibit morale gain");
		b.logGameEvent(p.explanation);
		return(Benefit.Morale);
	}
	if(morale<MAX_MORALE_TRACK) { morale++; return(null); } return(Benefit.Morale);

}
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
	if(c==ArtifactChip.Bear) { bearsGained++; }	// count for carythecarebear 
	if(c==ArtifactChip.Balloons) { balloonsGained++; }	//count for lars the ballooner
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
		if(penaltyAppliesToMe(MarketChip.IIB_AthenaeumOfMandatoryGuidelines))
			{
			MarketPenalty p = MarketChip.IIB_AthenaeumOfMandatoryGuidelines.marketPenalty;
			if(decrementMorale())
				{ //b.p1("lose morale due to IIB_AthenaeumOfMandatoryGuidelines" );
				  b.logGameEvent(p.explanation);	
				}
			}
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

// true if there's a worker on the board with 6 knowledge
public boolean hasBoardWorkerWith6()
{
	for(EuphoriaCell c : placedWorkers)
	{
		if(c!=null)
		{
			EuphoriaChip ch = c.topChip();
			if(ch.isWorker() && (ch.knowledge()==6))
			{
				return true;
			}
		}
	}
	return(false);
}
public boolean hasDoubles()
{
	for(int lim = workers.height()-1; lim>0; lim--)
	{	EuphoriaChip ch = workers.chipAtIndex(lim);
		for(int i = lim-1; i>=0; i--)
		{
			if(ch==workers.chipAtIndex(i)) { return(true);}
		}
	}
	return(false);
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
	alternateArtifacts = new EuphoriaCell(ArtifactChip.Subtype(),r,null,color);
	
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
	hasUsedSamuelTheZapper = false;
	usingSamuelTheZapper =false;
	usingMwicheTheFlusher = false;
	askedShaheenaTheDigger = false;
	triggerPedroTheCollector = false;
	samuelTheZapperLevel = 0;
	terriAuthorityHeight = authority.height();
	originalHiddenRecruit = null;
	originalActiveRecruit = null;
	penaltyMoves = 0;
	cardsLost = 0;
	workersLost = 0;
	bearsGained = 0;
	lostToAltruism = 0;
	someLostToAltruism = false;
	balloonsGained = 0;
	placements = 0;
	energyGainedThisTurn = 0;
	commodityKnowledge = 0;
	retrievals = 0;
	marketStars = 0;
	
	hasLostAWorker = false;
	hasAddedArtifact = false;
	hasAddedArtifactLast = false;
	hasPeeked = false;		// has seen the new card
	hasUsedGeekTheOracle = false;
	hasGainedWorker = false;
	hasUsedBrianTheViticulturist = false;
	hasUsedDarrenTheRepeater = false;

	
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
	v ^= alternateArtifacts.Digest();
	v ^= r.nextLong()*energyGainedThisTurn;
	v ^= r.nextLong()*commodityKnowledge;
	v ^= r.nextLong()*bearsGained;
	v ^= r.nextLong()*lostToAltruism;	// somelosttoaltruism is not included
	v ^= r.nextLong()*balloonsGained;
	v ^= EuphoriaChip.Digest(r,usedAlternateArtifact);
	v ^= r.nextLong()*(usingMwicheTheFlusher?1:0);
	v ^= r.nextLong()*(askedShaheenaTheDigger?1:0);
	v ^= r.nextLong()*(triggerPedroTheCollector?1:0);
    v ^= r.nextLong()*(hasLostAWorker ? 1 : 0);
    v ^= r.nextLong()*(hasAddedArtifact ? 1 : 0);
    v ^= r.nextLong()*(hasAddedArtifactLast ? 1 : 0);
    v ^= r.nextLong()*(hasPeeked ? 1 : 0);
    v ^= r.nextLong()*(hasUsedGeekTheOracle ? 1 : 0);
    v ^= r.nextLong()*(hasGainedWorker ? 1 : 0);
    v ^= r.nextLong()*(hasUsedBrianTheViticulturist ? 1 : 0);
    v ^= r.nextLong()*(hasUsedDarrenTheRepeater ? 1 : 0);
    v ^= r.nextLong()*samuelTheZapperLevel;
    v ^= r.nextLong()*terriAuthorityHeight;
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
	G.Assert(alternateArtifacts.sameContents(other.alternateArtifacts),"alternate artifacts mismatch");
	G.Assert(usedAlternateArtifact == other.usedAlternateArtifact,"used alternate artifact mismatch");
	G.Assert(energyGainedThisTurn==other.energyGainedThisTurn,"energy gained mismatch");
	G.Assert(bearsGained == other.bearsGained, "bears gained mismatch");
	G.Assert(lostToAltruism == other.lostToAltruism, "lostToAltruism mismatch");	// somelosttoaltruism is not included
	G.Assert(balloonsGained == other.balloonsGained, "balloons gained mismatch");
	G.Assert(commodityKnowledge==other.commodityKnowledge,"commodity dif mismatch");
	G.Assert(usingMwicheTheFlusher==other.usingMwicheTheFlusher,"usingMwicheTheFlusher mismatch");
	G.Assert(askedShaheenaTheDigger==other.askedShaheenaTheDigger,"askedShaheenaTheDigger mismatch");
	G.Assert(triggerPedroTheCollector==other.triggerPedroTheCollector,"triggerPedroTheCollector mismatch");
	G.Assert(   hasLostAWorker == other.hasLostAWorker, "hasLostAWorker mismatch");
	G.Assert(    hasAddedArtifact == other.hasAddedArtifact, "hasAddedArtifact mismatch");
	G.Assert(    hasAddedArtifactLast == other.hasAddedArtifactLast, "hasAddedArtifactLast mismatch");
	G.Assert(    hasPeeked == other.hasPeeked, "hasPeeked mismatch");
	G.Assert(    hasUsedGeekTheOracle == other.hasUsedGeekTheOracle, "hasUsedGeekTheOracle mismatch");
	G.Assert(    hasGainedWorker == other.hasGainedWorker, "hasGainedWorker mismatch");
	G.Assert(    hasUsedBrianTheViticulturist == other.hasUsedBrianTheViticulturist, "hasUsedBrianTheViticulturist mismatch");
	G.Assert(    hasUsedDarrenTheRepeater == other.hasUsedDarrenTheRepeater, "hasUsedDarrenTheRepeater mismatch");
	G.Assert(terriAuthorityHeight==other.terriAuthorityHeight,"terriAuthorityHeight mismatch");
	G.Assert(samuelTheZapperLevel==other.samuelTheZapperLevel,"samuelTheZapperLevel mismatch");

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
    hasUsedSamuelTheZapper = other.hasUsedSamuelTheZapper;
    usingSamuelTheZapper = other.usingSamuelTheZapper;
    usingMwicheTheFlusher = other.usingMwicheTheFlusher;
    askedShaheenaTheDigger = other.askedShaheenaTheDigger;
    triggerPedroTheCollector = other.triggerPedroTheCollector;
    samuelTheZapperLevel = other.samuelTheZapperLevel;
    terriAuthorityHeight = other.terriAuthorityHeight;
    originalHiddenRecruit = other.originalHiddenRecruit;
    originalActiveRecruit = other.originalActiveRecruit;
    bearsGained = other.bearsGained;
    lostToAltruism = other.lostToAltruism;
    someLostToAltruism = other.someLostToAltruism;
    balloonsGained = other.balloonsGained;
    energyGainedThisTurn = other.energyGainedThisTurn;
    commodityKnowledge = other.commodityKnowledge;
    alternateArtifacts.copyFrom(other.alternateArtifacts);
    usedAlternateArtifact = other.usedAlternateArtifact;
    
    hasLostAWorker = other.hasLostAWorker;
    hasAddedArtifact = other.hasAddedArtifact;
    hasAddedArtifactLast = other.hasAddedArtifactLast;
    hasPeeked = other.hasPeeked;
    hasUsedGeekTheOracle = other.hasUsedGeekTheOracle;
    hasGainedWorker = other.hasGainedWorker;
    hasUsedBrianTheViticulturist = other.hasUsedBrianTheViticulturist;
    hasUsedDarrenTheRepeater = other.hasUsedDarrenTheRepeater;

    
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
	totalWorkers--;
	workers.removeChip(c);
}

// check total knowledge and dump a worker if needed.  
// return true if a worker was lost.
public boolean knowledgeCheck(int extraKnowledge,replayMode replay)			// check for knowledge and remove if necessary
{	
	int know = totalKnowlege()+extraKnowledge;
	int limit = KnowledgeLimit;
	lostWorkerKnowledge = know;
	if(penaltyAppliesToMe(MarketChip.IIB_TheCarousel))
	{
		limit = 14;
	}

	if(recruitAppliesToMe(RecruitChip.AminaTheBlissBringer))
	{	// amina makes you immune to the carousel
		if(know==16)
			{ gainMorale(); 
			}
		if((know>=limit)&&(know<17))
		{	//b.p1("saved by amina");
			b.logGameEvent(SavedByAminaTheBlissBringer);
		}
		limit = 17;

	}
	if(know>=limit && !hasLostAWorker) 
	{	
		if(know-extraKnowledge<limit)
		{	//b.p1("lost worker due to OrwellianOptimism");
			MarketPenalty p = MarketChip.IIB_InstituteOfOrwellianOptimism.marketPenalty;
			b.logGameEvent(p.explanation);
		}
		
		if(know<KnowledgeLimit)
		{	// we're losing only due to penalty
			//b.p1("lose worker due to the Carousel");
			MarketPenalty p = MarketChip.IIB_TheCarousel.marketPenalty;
			b.logGameEvent(p.explanation);
		}

		return true;
	}
	return false;
}

int lostWorkerKnowledge =0;

// nuke the worker anyway
void loseWorker(replayMode replay)
{	
	WorkerChip worker = null;
	int workerIndex = -1;
	workersLost++;
	
	hasLostAWorker = true;		// can only lose 1 worker per turn
	// remove the smartest worker
	int nworkers = workers.height();
	for(int lim=nworkers; lim>=0; lim--)
	{
		WorkerChip c = (WorkerChip)workers.chipAtIndex(lim);
		if((worker==null)||(c.knowledge()>worker.knowledge())) { worker = c; workerIndex = lim; }
	}
	if(worker==null) { b.p1("No worker"); }
	b.logGameEvent(LoseAWorker,""+lostWorkerKnowledge,worker.shortName()); 
	if(replay!=replayMode.Replay)
	{
		b.animateSacrificeWorker(workers,worker);
	}
	removeWorker(worker);

	if(b.usedChaseTheMiner
			&& (workerIndex==nworkers))
	{	b.usedChaseTheMiner = false;			// we lost the new guy
	}
}

// return true if we need a UI to discard a card
public boolean moraleCheck(replayMode replay)
{	int nart = artifacts.height();
	if(morale<nart)
	{
	cardsLost += nart-morale;
	if(allOneArtifactType())  
		{ while(nart-->morale)
			{  b.recycleArtifact(artifacts.removeTop()); 
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
public int nArtifactsOfType(ArtifactChip a)
{	int n = 0;
	for(int lim = artifacts.height()-1; lim>=0; lim--)
	{
		if(artifacts.chipAtIndex(lim)==a) { n++;}
	}
	return n;
}
public int nTypesOfArtifact()
{	int n = 0;
	int mask = 0;
	for(int lim = artifacts.height()-1; lim>=0; lim--)
	{
		ArtifactChip a = (ArtifactChip)artifacts.chipAtIndex(lim);
		int chip = a.typeMask();
		if((chip&mask)==0) { mask |= chip; n++; }
	}
	return(n);
}
// either a natural pair or a wildcard match
public boolean artifactsMatch(ArtifactChip a, ArtifactChip b) {
	return ((a==b)||alternateArtifacts.containsChip(a)||alternateArtifacts.containsChip(b));
}

// either a natural pair or a wildcard pair
public ArtifactChip hasArtifactPair()
{	int na = artifacts.height();
	if(na>=2)
	{
	int hasMask = 0;
	for(int lim=na-1; lim>=0; lim--)
	{
		ArtifactChip ch = (ArtifactChip)artifacts.chipAtIndex(lim);
		int mask = 1<<ch.id.ordinal();
		if(alternateArtifacts.containsChip(ch)) { return(ch); }
		if((hasMask&mask)!=0) { return(ch); }
		hasMask |= mask;
	}}
	return(null);
}
// get a mask of the artifact types that have pairs in hand
public int getArtifactPairMask()
{	
	int pairs = 0;
	int na = artifacts.height();
	int singles = 0;
	int alltypes = 0;
	boolean hasWild = false;
	for(int lim=na-1; lim>=0; lim--)
	{
		ArtifactChip ch = (ArtifactChip)artifacts.chipAtIndex(lim);
		
		hasWild |= alternateArtifacts.containsChip(ch);
		
		int mask = 1<<ch.id.ordinal();
		alltypes |= mask;
		if((singles&mask)==0) { singles |= mask; } else { pairs |= mask; }
	}
	return(hasWild ? alltypes : pairs);
}

public int countArtifactPairs()
{	int pairMask = 0;
	int hasMask = 0;
	int na = artifacts.height();
	for(int lim=na-1; lim>=0; lim--)
	{
		ArtifactChip ch = (ArtifactChip)artifacts.chipAtIndex(lim);
		if(alternateArtifacts.containsChip(ch)) { return na-1; }
		int mask = 1<<ch.id.ordinal();
		if((mask&hasMask)==0) { hasMask |= mask; } else { pairMask|=mask; }
	}

	return(G.bitCount(b.revision>=105 ? pairMask : hasMask));
}
//
// pay an artifact pair, but only if there is just one way to do it.
//
private Cost payUniqueArtifactPair(Cost residual,replayMode replay)
{
	int na = artifacts.height();
	int pairMask = 0;
	int match1 = -1;
	int match2 = -1;
	ArtifactChip match1Chip = null;
	ArtifactChip match2Chip = null;
	
	for(int lim = na-1; lim>=0; lim--)
	{
		ArtifactChip ch = (ArtifactChip)artifacts.chipAtIndex(lim);
		for(int second = lim-1; second>=0; second--)
		{
			ArtifactChip ch2 = (ArtifactChip)artifacts.chipAtIndex(second);
			if(artifactsMatch(ch,ch2))
			{
				int mask = ch.typeMask()|ch2.typeMask();
				if(pairMask==0) 
					{ match1 = lim;
					  match2 = second; 
					  pairMask = mask; 
					  match1Chip = ch;
					  match2Chip = ch2;
				}
				else if(pairMask!=mask) { return residual; }
			}
		}
	}
	b.Assert(pairMask!=0,"must be a pair");
	if(G.bitCount(pairMask)==2)
	{
		// must be using a wildcard for one
		if(alternateArtifacts.containsChip(match1Chip)) { setUsedAlternate(match1Chip); }
		// it is theoretically possible to make a pair from 2 wildcard chips, but I rule
		// that one is one resource being used as a wildcard.
		if(alternateArtifacts.containsChip(match2Chip)) { setUsedAlternate(match2Chip); }
	}
	sendArtifact(match1,replay);
	sendArtifact(match2,replay);
	return null;
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

boolean gainMorale()
{
	return (incrementMorale()==null);
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

private Cost alternateCostForBrianTheViticulturist_V2(EuphoriaCell dest,Cost cost)
{
	if((dest!=null) && (dest.allegiance==Allegiance.Icarite))
	{
	switch(cost)
		{
		case Free:	// applies to the bliss factory.  Mostly free is free except to Brian
			return(Cost.Morale);
		case Resourcex3:		// mostly_resourcex3 is just resourcex3 except to brian
			return(Cost.Morale_Resourcex3);
		case Bliss_Commodity:	// mostly bliss_commodity is just bliss_commodity except to brian
										// but he can still pay food
			return(Cost.Morale_BlissOrFoodPlus1);
		case Artifactx3:		// mostly_artifactx3 is just artifactx3 except to brian
			return(Cost.Morale_Artifactx3);
		default: break;

		}
	}
	return(alternateCostForBrianTheViticulturist(cost));

}
private Cost alternateCostForBrianTheViticulturist(Cost cost)
{
	switch(cost)
	{
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
private Cost alternateCostForJonTheHandyman(Cost cost)
{	Cost newcost = null;
	switch(cost)
	{
	case ConstructionSiteGold:	newcost = Cost.GoldOrCommodityX3; break;
	case ConstructionSiteStone: newcost = Cost.StoneOrCommodityX3; break;
	case ConstructionSiteClay: newcost = Cost.ClayOrCommodityX3; break;
	default: return cost;
	}
	return newcost;
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


private IntObjHashtable<Cost> cachedCost = new IntObjHashtable<Cost>();
private Cost getCachedCost(EuphoriaCell dest,Cost cost)
{
	int key = (dest==null ? 0 : dest.rackLocation().ordinal()<<10) | cost.ordinal();
	return cachedCost.get(key);
}
private void setCachedCost(EuphoriaCell dest,Cost cost,Cost newcost)
{
	int key = (dest==null ? 0 : dest.rackLocation().ordinal()<<10) | cost.ordinal();
	cachedCost.put(key, newcost);
}
		
boolean verify = G.debug();
void clearCostCache()
{ cachedCost.clear(); 
}

/**
 * get the actual cost to be used, considering recruit capabilities
 * 
 * @param cost
 * @return
 */

Cost alternateCostWithRecruits(EuphoriaCell dest,Cost cost)
{	Cost alt = getCachedCost(dest,cost);
	if(!verify && (alt!=null)) { return(alt); }
	Cost originalCost = cost;
	alternateArtifacts.reInit();
	if(b.variation.isIIB())
	{
	if((dest!=null) && recruitAppliesToMe(RecruitChip.GaryTheForgetter))
	{
		EuphoriaId loc = dest.rackLocation();
		switch(loc)
		{
		case WorkerActivationA:
		case WorkerActivationB:
			switch(cost)
			{
			case Waterx3:	cost = Cost.Waterx3OrBlissx3; break;
			case Energyx3:	cost = Cost.Energyx3OrBlissx3; break;
			default: b.Error("not expecting cost "+cost); break;
				
			}
			break;

		default: break;
		}
	}
	if((dest!=null) && recruitAppliesToMe(RecruitChip.MosiThePatron))
	{	switch(dest.rackLocation())
		{
		default: break;
		case IcariteWindSalon:
		case WastelanderUseMarket:
		case EuphorianUseMarket:
		case SubterranUseMarket:
			
			switch(cost)
			{
			default: throw b.Error("Not expecting costs %s"+cost);
			case Artifactx3:
				cost = Cost.Artifactx3OrArtifactAndBliss;
			}
		}
	}

	if(recruitAppliesToMe(RecruitChip.JonTheAmateurHandyman))
	{
		cost = alternateCostForJonTheHandyman(cost);
	}
	if(recruitAppliesToMe(RecruitChip.AhmedTheArtifactDealer))
		{
		 // TODO: when AhmedTheArtifactDealer is in effect, modify the cost displays
		 if((dest!=null) && (dest.rackLocation()==EuphoriaId.ArtifactBazaar))
		 {	 //b.p1("use Ahmed the artifact dealer");
			 switch(cost)
			 {
			 case Free:	break;
			 case Commodity: cost = Cost.Free; break;
			 case CommodityX2: cost = Cost.Commodity; break;
			 default: G.Error("Not expecting cost %s",cost);
			 }
		 }	
		}
	if((dest!=null) && recruitAppliesToMe(RecruitChip.TedTheContingencyPlanner))
		{
		switch(dest.rackLocation())
		{
		default: break;
		case EuphorianTunnelMouth:
			cost = Cost.BlissOrEnergy;
			break;
		case WastelanderTunnelMouth:
			cost = Cost.BlissOrFood;
			break;
		case SubterranTunnelMouth:
			cost = Cost.BlissOrWater;
			break;
			
		}
		}
	if(recruitAppliesToMe(RecruitChip.JavierTheUndergroundLibrarian))
		{
			alternateArtifacts.addChip(ArtifactChip.Book);
		}
	if(recruitAppliesToMe(RecruitChip.AlexandraTheHeister))
		{
			alternateArtifacts.addChip(ArtifactChip.Balloons);
		}
	if(recruitAppliesToMe(RecruitChip.MiroslavTheConArtist))
		{
		alternateArtifacts.addChip(ArtifactChip.Bear);
		}
	if(recruitAppliesToMe(RecruitChip.EkaterinaTheCheater))
		{
		alternateArtifacts.addChip(ArtifactChip.Box);
		}
	if(recruitAppliesToMe(RecruitChip.JoseThePersuader))
		{
		alternateArtifacts.addChip(ArtifactChip.Bat);
		}
	if(recruitAppliesToMe(RecruitChip.MakatoTheForger))
		{
		alternateArtifacts.addChip(ArtifactChip.Bifocals);
		}
	}
	else
	{
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
		cost = alternateCostForBrianTheViticulturist_V2(dest,cost);
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
	}
	
	if(verify && (alt!=null))
		{ 
		
		b.Assert(cost==alt,"cached cost is %s but live cost is %s",alt,cost); 
		}
	else 
		{
		setCachedCost(dest,originalCost,cost); 
		}
	
	return(cost);
}
boolean hasArtifact(EuphoriaChip euphoriaChip)
{	// for StorageOfInsufficientCapacity
	return artifacts.containsChip(euphoriaChip);
}
boolean hasArtifactOrAlternate(ArtifactChip ch)
{	if(artifacts.containsChip(ch)) { return(true); }
	for(int sz = alternateArtifacts.height()-1; sz>=0; sz--)
	{
		if(artifacts.containsChip(alternateArtifacts.chipAtIndex(sz))) { return(true); }
	}
	return(false);
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
boolean canPay(EuphoriaCell dest,Cost item0)
{	Cost item = alternateCostWithRecruits(dest,item0);
	return canPayAlt(dest,item);
}
boolean canPayAlt(EuphoriaCell dest,Cost item)
{
	switch(item)
	{
	default: throw G.Error("Unexpected payment test for %s",item);
	case Artifactx3OrArtifactAndBliss: 
		return (canPayAlt(dest,Cost.Artifactx3) ||(( bliss.height()>=2) && (artifacts.height()>0)));
	case Energyx3OrBlissx3: return((bliss.height()>=3) || (energy.height()>=3));
	case Waterx3OrBlissx3: return((bliss.height()>=3) || (water.height()>=3));
	
	// agency of progressive backstabbing adds a commodity
	case StoneOrCommodityX3: return ((stone.height()>=1) || (totalCommodities()>=3));
	case GoldOrCommodityX3: return ((gold.height()>=1) || (totalCommodities()>=3));
	case ClayOrCommodityX3: return ((clay.height()>=1) || (totalCommodities()>=3));
	case WaterAndCommodity:  return (water.height()>=1 && totalCommodities()>=2); 	
	case EnergyAndCommodity: return (energy.height()>=1 && totalCommodities()>=2);
	case FoodAndCommodity: return(food.height()>=1 && totalCommodities()>=2);
	case EnergyX3AndCommodity: return(energy.height()>=3 && totalCommodities()>=4);
	case WaterX3AndCommodity: return(water.height()>=3 && totalCommodities()>=4);
	case ArtifactX3AndCommodity: 
		// courthouse of hasty judgement is not a factor, this is only in IIB 
		return((totalCommodities()>0) 
				&& ((artifacts.height()>=3) || (hasArtifactPair()!=null)));
			
	case BlissAndNonBlissAndCommodity:
		{
		int tc = totalCommodities();
		int bc = bliss.height();
		return (tc>=3 && bc>=1 &&bc<tc);	// some bliss and some nonbliss
		}
	case IsEuphorianAndCommodity:
		return(hasActiveRecruit(Allegiance.Euphorian) && (totalCommodities()>0));
	case IsSubterranAndCommodity:
		return(hasActiveRecruit(Allegiance.Subterran) && (totalCommodities()>0));
	case IsWastelanderAndCommodity:
		return(hasActiveRecruit(Allegiance.Wastelander) && (totalCommodities()>0));
	case ResourceX3AndCommodity:
		return((totalResources()>=3) && (totalCommodities()>0));
		
	case CommodityX2: return(totalCommodities()>=2);
	
	case Commodity:	return (totalCommodities()>=1);
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
	case BlissOrEnergy:
		return((bliss.height()>0) || (energy.height()>0));
	case BlissOrWater:
		return((bliss.height()>0) || (water.height()>0));
		
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
		return((totalCommodities()>=1) && hasArtifactOrAlternate(ArtifactChip.Bear));
	case Commodity_Bifocals:
		return((totalCommodities()>=1) && hasArtifactOrAlternate(ArtifactChip.Bifocals));
	case Commodity_Balloons:
		return((totalCommodities()>=1) && hasArtifactOrAlternate(ArtifactChip.Balloons));
	case Commodity_Box:
		return((totalCommodities()>=1) && hasArtifactOrAlternate(ArtifactChip.Box));
	case Commodity_Bat:
		return((totalCommodities()>=1) && hasArtifactOrAlternate(ArtifactChip.Bat));
	case Commodity_Book:
		return((totalCommodities()>=1) && hasArtifactOrAlternate(ArtifactChip.Book));

	case Bear:
		return(hasArtifactOrAlternate(ArtifactChip.Bear));
	case Bifocals:
		return(hasArtifactOrAlternate(ArtifactChip.Bifocals));
	case Balloons:
		return(hasArtifactOrAlternate(ArtifactChip.Balloons));
	case Box:
		return(hasArtifactOrAlternate(ArtifactChip.Box));
	case Bat:
		return(hasArtifactOrAlternate(ArtifactChip.Bat));
	case Book:
		return(hasArtifactOrAlternate(ArtifactChip.Book));

	// dilemma costs
	case BearOrCardx2:
		return((hasArtifactOrAlternate(ArtifactChip.Bear)) || (artifacts.height()>=2) );
	case BatOrCardx2:
		return((hasArtifactOrAlternate(ArtifactChip.Bat)) || (artifacts.height()>=2) );
	case BoxOrCardx2:
		return((hasArtifactOrAlternate(ArtifactChip.Box)) || (artifacts.height()>=2) );
	case BalloonsOrCardx2:
		return((hasArtifactOrAlternate(ArtifactChip.Balloons)) || (artifacts.height()>=2) );
	case BookOrCardx2:
		return((hasArtifactOrAlternate(ArtifactChip.Book)) || (artifacts.height()>=2) );
	case BifocalsOrCardx2:
		return((hasArtifactOrAlternate(ArtifactChip.Bifocals)) || (artifacts.height()>=2) );
	
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

	// costs new to IIB
	case Balloon_StoneAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Balloon_Stone:
		return (hasArtifactOrAlternate(ArtifactChip.Balloons) 
				&& (stone.height()>0));

	case Box_Food_BlissAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Box_Food_Bliss:
		return (hasArtifactOrAlternate(ArtifactChip.Box)
				&& (food.height()>0)
				&& (bliss.height()>0));
	case Balloon_Energy_BlissAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Balloon_Energy_Bliss:
		return (hasArtifactOrAlternate(ArtifactChip.Balloons)
				&& (energy.height()>0)
				&& (bliss.height()>0));
	case Glasses_Water_BlissAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Glasses_Water_Bliss:
		return (hasArtifactOrAlternate(ArtifactChip.Bifocals)
				&& (water.height()>0)
				&& (bliss.height()>0));

	case Book_Energy_WaterAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Book_Energy_Water:
		return (hasArtifactOrAlternate(ArtifactChip.Book)
				&& (energy.height()>0)
				&& (water.height()>0));
	case Bear_Energy_FoodAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Bear_Energy_Food:
		return (hasArtifactOrAlternate(ArtifactChip.Bear)
				&& (energy.height()>0)
				&& (food.height()>0));
		
	case Glasses_GoldAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Glasses_Gold:
		return (hasArtifactOrAlternate(ArtifactChip.Bifocals)
				&& (gold.height()>0));
		
	case Bear_GoldAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Bear_Gold:
		return (hasArtifactOrAlternate(ArtifactChip.Bear)
				&& (gold.height()>0));

	case Book_BrickAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Book_Brick:
		return (hasArtifactOrAlternate(ArtifactChip.Book)
				&& (clay.height()>0));
			
	case Box_GoldAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Box_Gold:
		return (hasArtifactOrAlternate(ArtifactChip.Box)
				&& (gold.height()>0));
		

	case Book_CardAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Book_Card:
		return ((artifacts.height()>=2)
				&& hasArtifactOrAlternate(ArtifactChip.Book));
	case Box_BrickAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Box_Brick:
		return (hasArtifactOrAlternate(ArtifactChip.Box)
				&& (clay.height()>0));

	case Bat_StoneAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Bat_Stone:
		return (hasArtifactOrAlternate(ArtifactChip.Bat)
				&& (stone.height()>0));

	case Book_StoneAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Book_Stone:
		return (hasArtifactOrAlternate(ArtifactChip.Book)
				&& (stone.height()>0));
	case Glasses_BrickAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Glasses_Brick:
		return (hasArtifactOrAlternate(ArtifactChip.Bifocals)
				&& (clay.height()>0));

	case Bat_BrickAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Bat_Brick:
		return (hasArtifactOrAlternate(ArtifactChip.Bat)
				&& (clay.height()>0));

		
	case EnergyMwicheTheFlusherAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case EnergyMwicheTheFlusher:
		return ((energy.height()>=1) || (water.height()>=3));
	
	case FoodMwicheTheFlusherAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case FoodMwicheTheFlusher:
		return ((food.height()>=1) || (water.height()>=3));
		
	case WaterMwicheTheFlusherAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case WaterMwicheTheFlusher:
		return (water.height()>=3);
		

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
	if(water.height()>0) 
		{  b.addWater(water.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnWater(water); }}
	else if(food.height()>0) { b.addFood(food.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnFood(food); }}
	else if(energy.height()>0) {  b.addEnergy(energy.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnEnergy(energy); }}
	else if(bliss.height()>0) {  b.addBliss(bliss.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnBliss(bliss); }}
	else { throw G.Error("Ran out of commodities"); }
}
private void shedOneNonBliss(replayMode replay)
{
	if(water.height()>0) {  b.addWater(water.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnWater(water); }}
	else if(food.height()>0) { b.addFood(food.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnFood(food); }}
	else if(energy.height()>0) {  b.addEnergy(energy.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnEnergy(energy); }}
	else { throw G.Error("Ran out of non-bliss commodities"); }
}


private boolean shedCards(int n,replayMode replay)
{	if(artifacts.height()>=n)
	{while(n-- > 0)
	{
		b.recycleArtifact(artifacts.removeTop()); 
		if(replay!=replayMode.Replay)
	  		{  b.animateReturnArtifact(artifacts);
	  		}
	}
	return(true);
	}
	return(false);
}

// supply a card instead of a resource if it is unambiguous
private Cost doFlartner(replayMode replay)
{
	incrementMorale();
	b.logGameEvent(FlartnerTheLudditeEffect);
	return payCostAlt(Cost.Artifact,replay);
}

//
// sacrifice a worker on top of cell c
//
void sacrificeWorker(EuphoriaCell c,replayMode replay)
{
	EuphoriaChip ch = c.removeTop();
	b.Assert(ch.isWorker() && (ch.color==color),"expected one of our workers");
	if(c.onBoard) { unPlaceWorker(c); }
	totalWorkers--;
	if(replay!=replayMode.Replay) { b.animateSacrificeWorker(c,(WorkerChip)ch);}
}
void sendStone(int n,replayMode replay)
{	if(stone.height()>=n)
	{
	for(int i=0;i<n;i++) 
		{ b.addStone(stone.removeTop()); if(replay!=replayMode.Replay)
		{ b.animateReturnStone(stone); }}
	return;
	}
	G.Error("Not enough stone");
}
void sendGold(int n,replayMode replay)
{	if(gold.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addGold(gold.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnGold(gold); }}
	return;
	}
	G.Error("Not enough gold");
}
void sendClay(int n,replayMode replay)
{	if(clay.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addClay(clay.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnClay(clay); }}
	return;
	}
	G.Error("Not enough clay");
}

void sendEnergy(int n,replayMode replay)
{	if(energy.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addEnergy(energy.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnEnergy(energy); }}
	return;
	}
	b.p1("not enough energy");
	G.Error("Not enough energy");
}
void sendFood(int n,replayMode replay)
{	if(food.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addFood(food.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnFood(food); }}
	return;
	}
	G.Error("Not enough food");
}

void sendWater(int n,replayMode replay)
{	if(water.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addWater(water.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnWater(water); }}
	return;
	}
	G.Error("Not enough water");
}

void sendBliss(int n,replayMode replay)
{	if(bliss.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addBliss(bliss.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnBliss(bliss); }}
	return;
	}
	G.Error("Not enough Bliss");	
}
Cost sendArtifacts(int n,replayMode replay)
{	if(artifacts.height()>=n)
	{
	for(int i=0;i<n;i++) { b.recycleArtifact(artifacts.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }}
	return(null);
	}
	throw G.Error("should succeed");
}

//note, before rev 123 this version had a bug that didn't have an immediately
//visible effect - it failed torecycle the card.  To maintain compatibility,
//we have to maintain the bug!
Cost artifactChipBuggy(ArtifactChip which,Cost cost,replayMode replay)
{
	if(b.revision>=123)
	{
	Cost residual = uniqueArtifactChip(which,cost,replay);
	return residual;
	}
	else
	{
	b.Assert(alternateArtifacts.height()==0,"no alternats artifacts allowed");
	artifacts.removeChip(which);	
	// do not recycle the artifact, that's the bug
	if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }
	return(null);
	}
}
private int uniqueMatchingIndex(ArtifactChip which)
{	int ind = artifacts.findChip(which);
	for(int alt = alternateArtifacts.height()-1; alt>=0; alt--)
	{
		ArtifactChip target = (ArtifactChip)alternateArtifacts.chipAtIndex(alt);
		int altInd = artifacts.findChip(target);
		if(altInd>=0)
		{
			if(ind<0) { ind = altInd; }
			else if(altInd!=ind) { return(-1); }
		}
	}
	return(ind);
}
// ch is an expected artifact type, or null if we're just looking
// for a wildcard in a pair
private void checkHasPaidArtifact(ArtifactChip ch)
{	
	ArtifactChip alt = null;
	int found = 0;
	for(int n = b.droppedDestStack.size()-1; n>=0; n--)
	{
		EuphoriaCell c =b.droppedDestStack.elementAt(n);
		if(c==b.usedArtifacts)
		{	found++;
			ArtifactChip top = (ArtifactChip)c.chipAtIndex(c.height()-found);
			if(top==ch) { }
			else if (alternateArtifacts.containsChip(top))
				{	alt = top;
				}
		}
	}
	if(alt!=null)
	{	// b.p1("paid with alternate "+alt.id+" for "+(ch==null ? "any" : ch.id));
		setUsedAlternate(alt); 
    }
}
//
// return a named chip or one of its current alternates
// there must be one of them 
// return the specified cost if the choice is ambiguous, or null if it was successfully and uniquely removed
//
private Cost uniqueArtifactChip(ArtifactChip which,Cost cost,replayMode replay)
{	int ind = uniqueMatchingIndex(which);
	if(ind<0) { return(cost); }	
	sendMatchingArtifact(ind,which,replay);
	return null;
}
//
// send a particular artifact, and record if it isn't an exact match.
// this sets up the extra resource for AlexandraTheHeister and her friends
//
private void sendMatchingArtifact(int ind,ArtifactChip which,replayMode replay)
{
	ArtifactChip alt = (ArtifactChip)artifacts.chipAtIndex(ind);
	if(alt!=which) { setUsedAlternate(alt); }
	sendArtifact(ind,replay);
}
private void setUsedAlternate(ArtifactChip alt)
{
	if((usedAlternateArtifact!=null) && (usedAlternateArtifact!=alt)) 
	{ b.p1("used artifact twice"); 
		G.Error("used artifact twice");
	}
	usedAlternateArtifact = alt;
}
private void sendArtifact(int ind,replayMode replay)
{
	ArtifactChip removed = (ArtifactChip)artifacts.removeChipAtIndex(ind);
	b.recycleArtifact(removed);
	if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }
}
// used by pamhidzai
void returnArtifact(EuphoriaCell to,replayMode replay)
{
	ArtifactChip top = (ArtifactChip)artifacts.removeTop();
	to.addChip(top);
	if(replay!=replayMode.Replay) 
	{
		b.animatePlacedItem(artifacts,to);
	}
}

// pay 1 of a specified type + 1 other, return false if there is ambiguity and interaction is necessary
// for the second card
private Cost artifact1XPlus1(ArtifactChip a,Cost cost,replayMode replay)
{	
	Cost residual = uniqueArtifactChip(a,Cost.Artifact,replay);	// return null if paid, cost if ambiguous, throws and error if not possible
	if(residual==null)
	{
		if(allOneArtifactType()) { sendArtifacts(1,replay); return null;  }
		return Cost.Artifact;
	}
	// payment is ambiguous
	return cost;

}
//
// this extracts the payment for fullfilling the secret goal
// 1 specific card or any 2.
private Cost artifactx2OrSpecific(ArtifactChip which,replayMode replay)
{	
	if(b.revision>=103)
	{	int match = uniqueMatchingIndex(which);	// match exact or alternate
		if(match>=0 && allOneArtifactType())
		{	sendMatchingArtifact(match,which,replay);	// will succeed and register alternate chip matching
			return(null);
		}
		else if((artifacts.height()==2) && (match<0))	// size 2 and no matching artifact
		{
			sendArtifacts(2,replay);
			return(null);
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
				  return(null); 
				}
			// not matching
			break;
	default: if(!allOneArtifactType()) { break; }	// not uniform, force interaction
		//$FALL-THROUGH$
	case 2:	
		sendArtifacts(2,replay);
		return(null);
	}}
	return Cost.Artifactx2;
	
}
void payCostOrElse(EuphoriaCell dest,Cost item,replayMode replay)
{	Cost residual = payCost(dest,item,replay);
	if(residual!=null)
	{
	b.p1("mandatory payment "+item+" failed with "+residual);
	G.Error("Payment %s failed");
	}
}
// 
// if andCommodity, all or nothing, either pay the entire cost of leave both sides unsatisfied
//
private Cost payCostArtifactx3(boolean andCommodity0,replayMode replay)
{	
	boolean andCommodity = andCommodity0;
	boolean payCommodity = andCommodity && nKindsOfCommodity()==1;
	
	if(payCommodity) { shedOneCommodity(replay); andCommodity = false; }

	if(penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement))
	{	b.Assert(!andCommodity,"shouldn't be combined");
		return(payCostAlt(Cost.Artifactx3Only,replay));
	}
	
	if(allOneArtifactType()
		&& (hasArtifactPair()!=null))
		{	if(payCostAlt(Cost.ArtifactPair,replay)!=null)
				{b.p1("artifactx3 didn't succeed2");
				 G.Error("must succeed");
				}
			return(andCommodity ? Cost.Commodity : null);
		}
	if((artifacts.height()==3)
			&& (countArtifactPairs()==0)) 
		{ 
		b.Assert(payCostAlt(Cost.Artifactx3Only,replay)==null,"must succeed"); 
		setTriggerPedroTheCollector(true);
		return(andCommodity ? Cost.Commodity : null);
		}
	return(andCommodity ? Cost.ArtifactX3AndCommodity : Cost.Artifactx3);
}

// return true if paying n commodities would be ambiguous.  If there is any 
// required type, it's known to be available.
boolean commoditiesIsAmbiguous(int n)
{	if (nKindsOfCommodity()==1) return(false);	// only one choice
	return(totalCommodities()!=n);
}
Cost sendCommodity(int n,Cost cost,replayMode replay)
{
	if((nKindsOfCommodity()==1) || (totalCommodities()==n))
		{ for(int i=0;i<n;i++) { shedOneCommodity(replay); }  
		return null;
		}
	return(cost);
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
Cost payCost(EuphoriaCell dest,Cost item0,replayMode replay)
{	
	return payCostAlt(alternateCostWithRecruits(dest,item0),replay);
}

Cost payCostAlt(Cost item,replayMode replay)
{	int commoditiesNeeded = 0;
	switch(item)
	{
	case Closed:
	case DisplayOnly:
	case MarketCost:
	case TunnelOpen:
				
	default: throw G.Error("Unexpected payment test for %s",item);
	
	case Artifactx3OrArtifactAndBliss:
		if(artifacts.height()==1) 		// can't pay with multiple cards
				{  sendBliss(2,replay); 
				   sendArtifact(0,replay); 
				   //b.p1("use mosi the patron");
				   return(null); 
				}				
		if(bliss.height()<2)
		{	// cant pay with bliss
			return(payCostAlt(Cost.Artifactx3,replay));
		}
		//b.p1("can use mosi the patron");
		return(item);

	case Waterx3OrBlissx3:
		if(bliss.height()<3) { return(payCostAlt(Cost.Waterx3,replay)); }
		if(water.height()<3) { return(payCostAlt(Cost.Blissx3,replay)); }
		return item;
		
	case Energyx3OrBlissx3:
		if(bliss.height()<3) { return(payCostAlt(Cost.Energyx3,replay)); }
		if(energy.height()<3) { return(payCostAlt(Cost.Blissx3,replay)); }
		return item;
		
	case StoneOrCommodityX3:
		if(stone.height()==0) { return(payCostAlt(Cost.CommodityX3,replay)); }
		if(totalCommodities()<3) { return(payCostAlt(Cost.ConstructionSiteStone,replay)); }
		return(item);
		
	case GoldOrCommodityX3:
		if(gold.height()==0) { return(payCostAlt(Cost.CommodityX3,replay)); }
		if(totalCommodities()<3) { return(payCostAlt(Cost.ConstructionSiteGold,replay)); }
		return(item);
		
	case ClayOrCommodityX3:
		if(clay.height()==0) { return(payCostAlt(Cost.CommodityX3,replay)); }
		if(totalCommodities()<3) { return(payCostAlt(Cost.ConstructionSiteClay,replay)); }
		return(item);
		
	case ResourceX3AndCommodity:
		boolean payCommodity = (nKindsOfCommodity()==1);
		boolean payResource = (nKindsOfResource()==1)||(totalResources()==3);
		
		if(payCommodity) { shedOneCommodity(replay); }
		if(payResource)
		{
			shedOneResource(replay);
			shedOneResource(replay);
			shedOneResource(replay);
			return(payCommodity ? null : Cost.Commodity);
		}
		else { return(payCommodity ? Cost.Resourcex3 : null); }
		
	// agency of progressive backstabbing adds a commodity to afixed cost
	// pay the fixed cost and if there is no choice, also pay the kicker
	case EnergyAndCommodity:
		sendEnergy(1,replay);
		return sendCommodity(1,Cost.Commodity,replay);
		
	case WaterAndCommodity:
		sendWater(1,replay);
		return sendCommodity(1,Cost.Commodity,replay);
		
	case FoodAndCommodity:
		sendFood(1,replay);
		return sendCommodity(1,Cost.Commodity,replay);
		
	case WaterX3AndCommodity:
		sendWater(3,replay);
		return sendCommodity(1,Cost.Commodity,replay);

	case EnergyX3AndCommodity:
		sendEnergy(3,replay);
		return sendCommodity(1,Cost.Commodity,replay);

	case BlissAndNonBlissAndCommodity:
		sendBliss(1,replay);
		return(payCostAlt(Cost.NonBlissAndCommodity,replay));

	case CommodityX3:
		return sendCommodity(3,Cost.CommodityX3,replay);
	case CommodityX2:	
		return sendCommodity(2,Cost.CommodityX2,replay);
		
	case IsEuphorianAndCommodity:
	case IsWastelanderAndCommodity:
	case IsSubterranAndCommodity:
	case Commodity:
		return(sendCommodity(1,Cost.Commodity,replay));
		
	case ResourceAndKnowledgeAndMoraleOrArtifact:
		// combo of michael the engineer and flartner the luddite
		if(artifacts.height()>0)
			{	if(totalResources()>0) { return(Cost.ResourceAndKnowledgeAndMoraleOrArtifact); }	// have both, must decide
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
			{
			if((shed==EuphoriaChip.Gold))
				{	// special effect of MichaelTheEngineer
				b.Assert(recruitAppliesToMe(RecruitChip.MichaelTheEngineer),"should be MichaelTheEngineer");				
				incrementKnowledge(replay);
				incrementMorale();
				b.logGameEvent(MichaelTheEngineerGold,getPlayerColor(),getPlayerColor());
				}
			else { b.logGameEvent(MichaelTheEngineerAny); }
			}
			return(null);
		}
		return(item);
		
	// MatthewTheThief
	case WaterOrKnowledge:
		if(!b.variation.isIIB() && (water.height()==0)) 
			{ if(b.revision>=108)
				{
				b.Assert(knowledge<MAX_KNOWLEDGE_TRACK,"can gain knowledge");
				incrementKnowledge(replay);
				b.logGameEvent(MatthewTheThiefWater,getPlayerColor());
				}
			  return(null); 
			}
		if(knowledge==MAX_KNOWLEDGE_TRACK) { sendWater(1,replay); return(null); }
		return(Cost.WaterOrKnowledge);
		
	case EnergyOrKnowledge:
		if(!b.variation.isIIB() && (energy.height()==0)) 
			{ if(b.revision>=108)
				{b.Assert(knowledge<MAX_KNOWLEDGE_TRACK,"can gain knowledge");
				incrementKnowledge(replay);
			  	b.logGameEvent(MatthewTheThiefEnergy,getPlayerColor());
				}
			  return(null); 
			}
		if(knowledge==MAX_KNOWLEDGE_TRACK) { sendEnergy(1,replay); return(null); }
		return(Cost.EnergyOrKnowledge);
		
	case FoodOrKnowledge:
		if(!b.variation.isIIB() && (food.height()==0))
			{ if(b.revision>=108)
				{b.Assert(knowledge<MAX_KNOWLEDGE_TRACK,"can gain knowledge");
			     incrementKnowledge(replay);
			     b.logGameEvent(MatthewTheThiefFood,getPlayerColor());
				}
			  return(null); 
			}
		if(knowledge==MAX_KNOWLEDGE_TRACK) {  sendFood(1,replay); return(null); }
		return(Cost.FoodOrKnowledge);
		
	case IsEuphorian:
	case IsWastelander:
	case IsIcarite:
	case IsSubterran:
		// we come here to pay the cost, which is actually free to qualified workers.
		// we never get here if we're not qualified.
		return(null);
		
	case Free: return(null);
	
	case SacrificeWorker:
		totalWorkers--;
		EuphoriaChip worker = newWorkers.removeTop();
		if(replay!=replayMode.Replay) { b.animateSacrificeWorker(newWorkers,(WorkerChip)worker);}
		return(null);
	case GoldOrArtifact:
		if(gold.height()==0) { return(doFlartner(replay)); }
		if(artifacts.height()>0) { return(Cost.GoldOrArtifact); }
		// or fall into regular gold
		//$FALL-THROUGH$
	case Gold:
	case ConstructionSiteGold:		// building markets
		sendGold(1,replay);
		return(null);
		
	case StoneOrArtifact:
		if(stone.height()==0) { return(doFlartner(replay)); }
		if(artifacts.height()>0) { return(Cost.StoneOrArtifact); }
		// or fall into regular gold	
		//$FALL-THROUGH$
	case ConstructionSiteStone:		// building markets
		sendStone(1,replay);
		return null;
		
	case ClayOrArtifact:
		if(clay.height()==0) { return(doFlartner(replay)); }
		if(artifacts.height()>0) { return(Cost.ClayOrArtifact); }
		// or fall into regular clay	
		//$FALL-THROUGH$
	case ConstructionSiteClay:		// building markets
		sendClay(1,replay);
		return(null);
		
		
	case Foodx4:	
		sendFood(4,replay);
		return(null);
	case Food:	
		sendFood(1,replay);
		return(null);
		
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
		return(null);
		
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
		return(null);
		
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
		return(null);

	case Artifactx2:
		if((artifacts.height()==2)||allOneArtifactType())
		{	// only one way to do it.
			sendArtifacts(2,replay);
			return(null);
		}
		return(Cost.Artifactx2);
		
	case Artifactx3Only:		// various markets.  Usually take 3 cards or a pair
		b.Assert(artifacts.height()>=3,"not enough artifacts");
		if((artifacts.height()==3) || allOneArtifactType())
			{ sendArtifacts(3,replay);
			  return(null);  
			}
		return(Cost.Artifactx3Only);	// requires decision
		

	case ArtifactPair:
		{
		ArtifactChip ch = hasArtifactPair();
		b.Assert(ch!=null,"has an artifact pair");
		return payUniqueArtifactPair(Cost.ArtifactPair,replay);
		}
		
	case ArtifactJackoTheArchivist_V2:
		if(artifacts.height()==1)
		{	// only one way to do it.  Otherwise he gets to choose
			if(b.revision<117)
				{b.Assert((knowledge+2)<=MAX_KNOWLEDGE_TRACK,"knowledge <=4");
				incrementKnowledge(replay);
				incrementKnowledge(replay);
				}
			sendArtifacts(1,replay);
			return(null);
		}
		return(Cost.ArtifactJackoTheArchivist_V2);	// must choose
		
	case Smart_Artifact:
		// this is used when brian the viticulturist has to discard a card
		// and he has no choice about which one to discard because he has
		// to preserve his pair.
		b.Assert((hasArtifactPair()!=null)
				&& ((artifacts.height()==3)
				&& !penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement)),"must be forced artifact");
		if(artifacts.chipAtIndex(0)==artifacts.chipAtIndex(1)) { artifacts.removeChipAtIndex(2); }
		else if(artifacts.chipAtIndex(0)==artifacts.chipAtIndex(2)) { artifacts.removeChipAtIndex(1); }
		else { artifacts.removeChipAtIndex(0); }
		return(null);
		
	case Morale_Artifactx3:
		// this is special logic for BrianTheViticulturist
		if(morale<=artifacts.height()) 
			{ return(Cost.Morale_Artifactx3); } 	// have to interact
		b.Assert(morale>=2,"morale>=2");
		decrementMorale();	
		if(b.revision<123)
		{
			// keep the old bugs for replay
			Cost residual = payCostArtifactx3(false,replay);
			if(residual!=null)
			{
				return Cost.Morale_Artifactx3;
			}
			return null;
		}
		//$FALL-THROUGH$
	case Artifactx3:
		return payCostArtifactx3(false,replay);
	case ArtifactX3AndCommodity:
		return payCostArtifactx3(true,replay);
		
	case Morale_Resourcex3:
		b.Assert(morale>=2,"morale>=2");
		decrementMorale();
		//$FALL-THROUGH$
	case Resourcex3:	// nimbus loft
		if((totalResources()==3) || (nKindsOfResource()==1))
		{	int count = 0;
			while(count<3)
			{	shedOneResource(replay);
				count++;
			}
			return(null);
		}
		return(Cost.Resourcex3);
		
	case NonBlissCommodity:
		if(nKindsOfCommodityExceptBliss()==1)
			{
			shedOneNonBliss(replay);
			return(null);
			}
		return Cost.NonBlissCommodity;
	case Morale_BlissOrFoodPlus1:
		b.Assert(morale>=2,"morale>=2");
		decrementMorale();
		//$FALL-THROUGH$
	case BlissOrFoodPlus1:	// breeze bar and sky lounge with BrianTheViticulturist
		if(food.height()>0)				// have food
		{	if(b.revision<123)
			{
			// preserve old bugs
			if((bliss.height()==0)						// have no bliss, so the substitution is required
					&& ((totalCommodities()==2) 		// we only have 2 commodities
							|| ((water.height()==0)&&(energy.height()==0)))	// or we only have food
					)
					{	// don't set hasUsedBrianTheViticulturist here, because it only 
						// counts for non-icarite markets
						shedOneCommodity(replay);
						shedOneCommodity(replay);
						b.logGameEvent(BrianTheViticulturistEffect);
						return(null);
					}
			int count = (b.revision>=101) 						// bug fix 7/6/2014 number of kinds of commodity, not the number of commodity
						? nKindsOfCommodityExceptBliss() 
						: totalCommoditiesExceptBliss();
			if(count!=1)
					{ return Cost.BlissOrFoodPlus1;
					}
			}
			else 
			{
			if(	(totalCommoditiesExceptBliss()<=2)
					&& (bliss.height()>0))
				{	
					sendBliss(1,replay);
					sendFood(1,replay);
					return null;
				}
			if(bliss.height()==0)					// have no bliss, so the substitution is required
				{
				sendFood(1,replay);
				b.logGameEvent(BrianTheViticulturistEffect);
				return payCostAlt(Cost.NonBlissCommodity,replay);
				}

			return Cost.BlissOrFoodPlus1;
			}
		}	// end of have food
		//$FALL-THROUGH$
	case Bliss_Commodity:	// breeze bar and sky lounge
		sendBliss(1,replay);
		if(b.revision>=123)
			{
			return payCostAlt(Cost.NonBlissCommodity,replay);
			}		
		if(((b.revision>=101) 						// bug fix 7/6/2014 number of kinds of commodity, not the number of commodity
				? nKindsOfCommodityExceptBliss() 
				: totalCommoditiesExceptBliss())==1) 	// only have 1
			{
			shedOneCommodity(replay);
			return(null);
			}
		return Cost.NonBlissCommodity;

		
	case EnergyOrBlissOrFood:
		{ // pay with energy instead of food or bliss
		if(energy.height()>0)
			{
			if((bliss.height()==0) && (food.height()==0)) 
			{ sendEnergy(1,replay);
			  return null;
			}
			return(Cost.EnergyOrBlissOrFood);		// force interaction
			}
		}
		// or fall into blissorfood
		//$FALL-THROUGH$
	case BlissOrFood:		// pay for retrieval
		
		if(bliss.height()==0) { return(payCostAlt(Cost.Food,replay)); } 
		else if(food.height()==0) { return(payCostAlt(Cost.Bliss,replay)); }
		return(Cost.BlissOrFood);		// force interaction
		
	case BlissOrEnergy:		// pay for retrieval
		
		if(bliss.height()==0) {  return(payCostAlt(Cost.Energy,replay)); }
		if(energy.height()==0) { return(payCostAlt(Cost.Bliss,replay)); }
		return Cost.BlissOrEnergy;
		
	case BlissOrWater:		// pay for retrieval
		
		if(bliss.height()==0) {  return(payCostAlt(Cost.Water,replay)); }
		if(energy.height()==0) { return(payCostAlt(Cost.Bliss,replay)); }
		return Cost.BlissOrWater;
		
	/* prices for open markets */
	case Energyx4_StoneOrBlissOrFood:
		if(food.height()>0)
			{
			if((bliss.height()==0) && (stone.height()==0)) 
				{ sendFood(1,replay);
				  sendEnergy(4,replay); 
				  return null;
				}
			if(b.revision>=123)
			{	sendEnergy(4,replay);
				return Cost.StoneOrBlissOrFood;
			}
			return(Cost.Energyx4_StoneOrBlissOrFood);	// must interact
			}
		// or fall into stoneOrBliss
		//$FALL-THROUGH$
	case Energyx4_StoneOrBliss:
		sendEnergy(4,replay);
		//$FALL-THROUGH$
	case StoneOrBliss:
		if(bliss.height()==0) { sendStone(1,replay); return null; }
		if(stone.height()==0) { sendBliss(1,replay); return null; }
		return(Cost.StoneOrBliss);
		
				
	case Energyx4_Stone:
		sendStone(1,replay);
		sendEnergy(4,replay);
		return(null);

	case Waterx4_ClayOrBlissOrFood:
		sendWater(4,replay);
		//$FALL-THROUGH$
	case ClayOrBlissOrFood:
		if(clay.height()==0) { return payCostAlt(Cost.BlissOrFood,replay); }
		if(bliss.height()==0) { return payCostAlt(Cost.ClayOrFood,replay); }
		if(food.height()==0) { return payCostAlt(Cost.ClayOrBliss,replay); }
		return Cost.ClayOrBlissOrFood;
	case ClayOrBliss:
		if(clay.height()==0) { sendBliss(1,replay); return(null); }
		if(bliss.height()==0) { sendClay(1,replay); return(null); }
		return Cost.ClayOrBliss;
	case ClayOrFood:
		if(clay.height()==0) { sendBliss(1,replay); return(null); }
		if(food.height()==0) { sendClay(1,replay); return(null); }
		return Cost.ClayOrFood;
		
	case Waterx4_ClayOrBliss:
		sendWater(4,replay);
		return payCostAlt(Cost.ClayOrBliss,replay); 

	case Waterx4_Clay:
		{	sendClay(1,replay);
			sendWater(4,replay);
			return(null);
		}
	
	case Waterx4_GoldOrBlissOrFood:
		if(b.revision<123)
		{
			// keep the oldbugs for replay games
			if(food.height()>0)
			{
				if((bliss.height()==0) 
						&& (gold.height()>0)) 
					{ sendWater(4,replay);
					  sendFood(1,replay); 
					  return(null); 
					}
				return(Cost.GoldOrBlissOrFood); 	// have to interact
			}
			if(bliss.height()>0)
				{
				if(gold.height()>0) { return(Cost.GoldOrBliss); } 	// interact
				sendBliss(1,replay);
				sendWater(4,replay);
				b.logGameEvent(JoshTheNegotiatorEffect);
				return(null);
				}
		}
		sendWater(4,replay);
		//$FALL-THROUGH$
	case GoldOrBlissOrFood:
		if(food.height()==0) { return(payCostAlt(Cost.GoldOrBliss,replay)); }
		if(gold.height()==0) { return(payCostAlt(Cost.BlissOrFood,replay)); }
		if(bliss.height()==0) { return(payCostAlt(Cost.GoldOrFood,replay)); }
		return Cost.GoldOrBlissOrFood;
		
	case GoldOrFood:
		if(gold.height()==0) { sendFood(1,replay); return(null); }
		if(food.height()==0) { sendGold(1,replay); return(null); }
		return Cost.GoldOrFood;

	case Waterx4_GoldOrBliss:
		sendWater(4,replay);
		//$FALL-THROUGH$
	case GoldOrBliss:
		if(bliss.height()==0) { sendGold(1,replay); return(null); }
		if(gold.height()==0) { sendBliss(1,replay); return(null); }
		return Cost.GoldOrBliss;

	case Waterx4_Gold:
		sendGold(1,replay);
		sendWater(4,replay);
		return null;

	case Foodx4_GoldOrBliss:
		sendFood(4,replay);
		return(payCostAlt(Cost.GoldOrBliss,replay));

	case Foodx4_Gold:
		sendGold(1,replay);
		sendFood(4,replay);
		return null;

	case Energyx4_ClayOrBlissOrFood:
		sendEnergy(4,replay);
		return payCostAlt(Cost.ClayOrBlissOrFood,replay);

	case Energyx4_ClayOrBliss:
		sendEnergy(4,replay);
		return payCostAlt(Cost.ClayOrBliss,replay);

	case Energyx4_Clay:
		sendClay(1,replay);
		sendEnergy(4,replay);
		return null;

	case Foodx4_StoneOrBlissOrFood:
		sendFood(4,replay);
		//$FALL-THROUGH$
	case StoneOrBlissOrFood:
		if(food.height()==0) { return payCostAlt(Cost.StoneOrBliss,replay); }
		if(bliss.height()==0) { return payCostAlt(Cost.StoneOrFood,replay); }
		if(stone.height()==0) { return payCostAlt(Cost.BlissOrFood,replay); }
		return Cost.StoneOrBlissOrFood;

	case StoneOrFood:
		if(stone.height()==0) { sendFood(1,replay); return(null); }
		if(food.height()==0) { sendStone(1,replay); return(null); }
		return Cost.StoneOrFood;

		// or fall into stone or bliss
		//$FALL-THROUGH$
	case Foodx4_StoneOrBliss:
		sendFood(4,replay);
		return payCostAlt(Cost.StoneOrBliss,replay);

	case Foodx4_Stone:
		sendStone(1,replay);
		sendFood(4,replay);
		return null;

	case Card_ResourceOrBlissOrFood:
		if(b.revision>=123)
			{
			if(totalResources()==0) { return payCostAlt(Cost.Card_BlissOrFood,replay); }
			if(food.height()==0) { return payCostAlt(Cost.Card_ResourceOrBliss,replay); }
			if(bliss.height()==0) { return payCostAlt(Cost.Card_FoodOrResource,replay); }
			}
		if(!allOneArtifactType()) { return(Cost.Card_ResourceOrBlissOrFood); }	// choice of artifact is implied
		if(nKindsOfResource()>1) { return(Cost.Card_ResourceOrBlissOrFood); }		// choice of resource is implied
		if(food.height()>0)
		{	if(nKindsOfResource()>0) { return(Cost.Card_ResourceOrBlissOrFood); }
			if(bliss.height()>0) { return(Cost.Card_ResourceOrBlissOrFood); }
			sendFood(1,replay);
			sendArtifacts(1,replay);
			return null;
		}
		// no food, try bliss and resource
		//$FALL-THROUGH$
	case Card_ResourceOrBliss:
		if(!allOneArtifactType()) { return(Cost.Card_ResourceOrBliss); }
		if(nKindsOfResource()>1) { return(Cost.Card_ResourceOrBliss); }
		if(bliss.height()>0)
		{	if(nKindsOfResource()>0) { return(Cost.Card_ResourceOrBliss); }
			b.logGameEvent(JoshTheNegotiatorEffect);
			sendBliss(1,replay);
			sendArtifacts(1,replay);
			return null;
		}
		// else fall into regular card_resource
		//$FALL-THROUGH$
	case Card_Resource:
		if(b.revision>=123)
		{
			if(nKindsOfResource()==1) { shedOneResource(replay); return payCostAlt(Cost.Card,replay); }
			if(allOneArtifactType()) { sendArtifacts(1,replay); return payCostAlt(Cost.Resource,replay); }
			return Cost.Card_Resource;
		}
	
		if(!allOneArtifactType()) { return(Cost.Card_Resource); }
		if(nKindsOfResource()>1) { return(Cost.Card_Resource); }
		sendArtifacts(1,replay);
		shedOneResource(replay);
		return(null);
		
	case Waterx4_Card:
		sendWater(4,replay); 
		return payCostAlt(Cost.Artifact,replay);

	case Energyx4_Card:
		{
		Cost residual = payCostAlt(Cost.Artifact,replay);
		if(residual!=null) 
			{ if(b.revision>=115) { sendEnergy(4,replay); } 
			  return residual; 
			}
		sendEnergy(4,replay);  //preserve and old bug for replay games
		return null;
		}
	case Foodx4_Card:
		sendFood(4,replay);
		return payCostAlt(Cost.Artifact,replay);
		
	case BlissOrFoodx4_Resource:

		if(b.revision>=123)
		{
		if(nKindsOfResource()==1) { shedOneResource(replay); return payCostAlt(Cost.BlissOrFoodx4,replay); }
		}
		if(nKindsOfResource()!=1) { return(Cost.BlissOrFoodx4_Resource); }	// interact
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
					return(null);
				}
			}}
			return(Cost.BlissOrFoodx4_Resource);
		}
		// fall into standard blissx4_resource
	case Blissx4_Resource:
		sendBliss(4,replay);
		return payCostAlt(Cost.Resource,replay);

	case BlissOrFoodx4_ResourceOrBlissOrFood:
		{
		int nFood = food.height();
		if(nFood>0)
		{
		// effectively, at most one resource, plus any mix of food or bliss to make 5
		int nRes = nKindsOfResource();
		int nBliss = bliss.height();
		if(nRes==0) 		 // no resources
			{ if(nBliss==0) { sendFood(5,replay);  return null; }
			  if((nFood+nBliss)==5) { sendFood(nFood,replay);  sendBliss(nBliss,replay); return null;  }
			  return(Cost.BlissOrFoodx4_ResourceOrBlissOrFood); 	// interact
			}
		if(nRes==1)
			{	if((nFood+nBliss)==4) 
				{  shedOneResource(replay); 
				   sendFood(nFood,replay);
				   sendBliss(nBliss,replay); 
				   return null;
				}
			}	
		return(Cost.BlissOrFoodx4_ResourceOrBlissOrFood);
		}}
		// or fall into the no food case
		//$FALL-THROUGH$
	case Blissx4_ResourceOrBliss:
		sendBliss(4,replay);

		//$FALL-THROUGH$
	case ResourceOrBliss:
		{
		int nRes = nKindsOfResource();
		if(nRes==0) 
			{ 
			b.logGameEvent(JoshTheNegotiatorEffect);
			sendBliss(1,replay);
			return null;
			}
		else if((nRes==1)&&(bliss.height()==0)) 
			{ shedOneResource(replay);
			  b.logGameEvent(JoshTheNegotiatorEffect);			  
			  return null;
			}
		return(Cost.ResourceOrBliss);
		}
	case BlissOrFoodx4_Card:
		if(!allOneArtifactType()) { return(Cost.BlissOrFoodx4_Card); }	// interact
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
					return(null);
				}
			}}
		return(Cost.BlissOrFoodx4_Card); 
		}
		// fall into standard blissx4_card
	case Blissx4_Card:
		sendBliss(4,replay);
		return payCostAlt(Cost.Artifact,replay);
		
	case Commodity_Bear:
		// note this ineffecient construction is to preserve
		// a bug that lost discards before rev 123
		if(nKindsOfCommodity()==1)
		{	shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Bear,Cost.Bear,replay);
		}
		return 	payCostAlt(Cost.Bear,replay)==null ? Cost.Commodity : Cost.Commodity_Bear;
		
	case Commodity_Bifocals:
		// note this ineffecient construction is to preserve
		// a bug that lost discards before rev 123
		if(nKindsOfCommodity()==1)
		{	shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Bifocals,Cost.Bifocals,replay);
		}
		return payCostAlt(Cost.Bifocals,replay)==null ? Cost.Commodity : Cost.Commodity_Bifocals;

	case Commodity_Balloons:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Balloons,Cost.Balloons,replay);
		}
		return (payCostAlt(Cost.Balloons,replay)==null) ? Cost.Commodity : Cost.Commodity_Balloons;

	case Commodity_Box:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Box,Cost.Box,replay);
		}
		return payCostAlt(Cost.Box,replay)==null ? Cost.Commodity : Cost.Commodity_Box;

	case Commodity_Bat:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Bat,Cost.Bat,replay);
		}
		return payCostAlt(Cost.Bat,replay)==null ? Cost.Commodity : Cost.Commodity_Bat;

	case Commodity_Book:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Book,Cost.Book,replay);
		}
		return payCostAlt(Cost.Book,replay)==null ? Cost.Commodity : Cost.Commodity_Book;


		// artifact costs
	case Bear:
		return(artifactChipBuggy(ArtifactChip.Bear,Cost.Bear,replay));
			
	case Box:
		return(artifactChipBuggy(ArtifactChip.Box,Cost.Box,replay));
			
	case Book:
		return(artifactChipBuggy(ArtifactChip.Book,Cost.Book,replay));
			
	case Bifocals:
		return(artifactChipBuggy(ArtifactChip.Bifocals,Cost.Bifocals,replay));
		
	case Bat:
		return(artifactChipBuggy(ArtifactChip.Bat,Cost.Bat,replay));
			
	case Balloons:
		return(artifactChipBuggy(ArtifactChip.Balloons,Cost.Balloons,replay));

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
		b.Assert((knowledge+1)<=MAX_KNOWLEDGE_TRACK,"knowledge <=2");
		incrementKnowledge(replay);
		return(null);
		
	case Knowledgex2:
		b.Assert((knowledge+2)<=MAX_KNOWLEDGE_TRACK,"knowledge <=4");
		incrementKnowledge(replay);
		incrementKnowledge(replay);
		return(null);
	case Morale:
		b.Assert(morale>=2,"morale>=2");
		decrementMorale();
		return(null);
	case Moralex2:
		b.Assert(morale>=3,"morale>=3");
		decrementMorale();
		decrementMorale();
		return(null);
	//
	// these are not normally paid per se, but are extracted
	// as a penalty when morale declines.
	//
	case Cardx6:	commoditiesNeeded++;
		//$FALL-THROUGH$
	case Cardx5:	commoditiesNeeded++;
		//$FALL-THROUGH$
	case Cardx4:	commoditiesNeeded++;
		//$FALL-THROUGH$
	case Cardx3:	commoditiesNeeded++;
		//$FALL-THROUGH$
	case CardForGeekx2:
	case Cardx2:	commoditiesNeeded++;
		//$FALL-THROUGH$
	case Card:
	case Artifact:
	case CardForGeek:commoditiesNeeded++;
		if(allOneArtifactType() || (artifacts.height()==commoditiesNeeded)) { shedCards(commoditiesNeeded,replay) ;  return null; };
		return(item);
		
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
		if(tot==0) { return(null); } 	// paid nothing from nothing
		int sum = totalCommodities()+totalResources();
		if((tot==1)||(sum<=commoditiesNeeded)) { payCommodityOrResource(Math.min(commoditiesNeeded,sum),replay); return(null); }	// no choice
		else { return(item); }
		}
		
	// costs new to IIB
	case Balloon_StoneAndCommodity:
		{
		sendStone(1,replay);					// balloon and stone are prepaid
		Cost residual = uniqueArtifactChip(ArtifactChip.Balloons,Cost.Balloons,replay);
		if(nKindsOfCommodity()==1) { shedOneCommodity(replay); return(residual); }
		return (residual==null) ?Cost.Commodity : Cost.Commodity_Balloons;
		}
		
	case Balloon_Stone:
		sendStone(1,replay);
		return uniqueArtifactChip(ArtifactChip.Balloons,Cost.Balloons,replay);

	case Box_Food_Bliss:
		sendBliss(1,replay);
		sendFood(1,replay);
		return uniqueArtifactChip(ArtifactChip.Box,Cost.Box,replay);
		
	case Balloon_Energy_Bliss:
		sendEnergy(1,replay);
		sendBliss(1,replay);
		return uniqueArtifactChip(ArtifactChip.Balloons,Cost.Balloons,replay);

	case Glasses_Water_BlissAndCommodity:
		sendWater(1,replay);		
		sendBliss(1,replay);	// prepay bliss
		//$FALL-THROUGH$
	case Glasses_Commodity:
		{
		Cost residual = uniqueArtifactChip(ArtifactChip.Bifocals,Cost.Bifocals,replay);	
		if(nKindsOfCommodity()==1) { shedOneCommodity(replay); return(residual);}
		return( residual==null ? Cost.Commodity : Cost.Glasses_Commodity);
		}
		
	case Glasses_Water_Bliss:
		sendBliss(1,replay);
		sendWater(1,replay);
		return uniqueArtifactChip(ArtifactChip.Bifocals,Cost.Bifocals,replay);

	case Book_Energy_Water:
		sendWater(1,replay);
		sendEnergy(1,replay);
		return uniqueArtifactChip(ArtifactChip.Book,Cost.Book,replay);

	case Bear_Energy_Food:
		sendEnergy(1,replay);
		sendFood(1,replay);
		return uniqueArtifactChip(ArtifactChip.Bear,Cost.Bear,replay);		
		
	case Glasses_Gold:
		sendGold(1,replay);
		return uniqueArtifactChip(ArtifactChip.Bifocals,Cost.Bifocals,replay);

	case Bear_GoldAndCommodity:
		sendGold(1,replay);		// prepay gold
		return payCostAlt(Cost.Commodity_Bear,replay);

	case Bear_Gold:
		sendGold(1,replay);
		return uniqueArtifactChip(ArtifactChip.Bear,Cost.Bear,replay);
				
	case Book_Brick:
		sendClay(1,replay);
		return uniqueArtifactChip(ArtifactChip.Book,Cost.Book,replay);
			
	case Box_Gold:
		sendGold(1,replay);
		return uniqueArtifactChip(ArtifactChip.Box,Cost.Box,replay);	

	case Book_Card:	// prepay the book
		return(artifact1XPlus1(ArtifactChip.Book,Cost.Book_Card, replay));

	case Box_BrickAndCommodity:
		sendClay(1,replay);
		return payCostAlt(Cost.Commodity_Box,replay);
		
	case Box_Brick:
		sendClay(1,replay);
		return uniqueArtifactChip(ArtifactChip.Box,Cost.Box,replay);
		
	case Bat_StoneAndCommodity:
		sendStone(1,replay);		// prepay bat and stone
		return payCostAlt(Cost.Commodity_Bear,replay);

	case Bat_Stone:
		sendStone(1,replay);		// prepay bat and stone
		return payCostAlt(Cost.Bat,replay);
		
	case Book_StoneAndCommodity:
		sendStone(1,replay);					// send the stone
		return payCostAlt(Cost.Commodity_Book,replay);
	
	case Book_Stone:
		sendStone(1,replay);
		return uniqueArtifactChip(ArtifactChip.Book,Cost.Book,replay);
		
	case Glasses_BrickAndCommodity:
		sendClay(1,replay);							// prepay clay
		return payCostAlt(Cost.Glasses_Commodity,replay);

	case Glasses_Brick:
		sendClay(1,replay);
		return uniqueArtifactChip(ArtifactChip.Bifocals,Cost.Bifocals,replay);
		
	case Bat_BrickAndCommodity:
		sendClay(1,replay);						// prepay brick
		return payCostAlt(Cost.Commodity_Bat,replay);

	case Bat_Brick:
		sendClay(1,replay);
		return payCostAlt(Cost.Bat,replay);

	//mwiche the flusher
	case EnergyMwicheTheFlusher:
		if(water.height()<3) { return payCostAlt(Cost.Energy,replay); }	// not enough water, send energy
		if(energy.height()==0) { usingMwicheTheFlusher = true; return payCostAlt(Cost.Waterx3,replay); }	// not enough food, send water
		//b.p1("mwitche option for energy");
		return(Cost.EnergyMwicheTheFlusher);  	// have to interact
	
	case WaterMwicheTheFlusher:
		if(water.height()<3) { sendWater(1,replay); return null; }
		//b.p1("mwitche option for water");
		return(Cost.WaterMwicheTheFlusher);

	case FoodMwicheTheFlusher:
		if(water.height()<3)  { sendFood(1,replay); return null; }
		if(food.height()==0) { usingMwicheTheFlusher = true;  sendWater(3,replay); return null;}
		//b.p1("mwitche option for food");
		return(Cost.FoodMwicheTheFlusher);
		
	case WaterMwicheTheFlusherAndCommodity:
		{
		Cost rest = payCostAlt(Cost.Commodity,replay);
		Cost main = payCostAlt(Cost.WaterMwicheTheFlusher,replay);
		if(rest==null) { return(main); }
		if(main==null) { return(rest); }
		return(Cost.WaterMwicheTheFlusherAndCommodity);
		}
		
	case FoodMwicheTheFlusherAndCommodity:
		{
		Cost rest = payCostAlt(Cost.Commodity,replay);
		Cost main = payCostAlt(Cost.FoodMwicheTheFlusher,replay);
		if(rest==null) { return(main); }
		if(main==null) { return(rest); }
		return(Cost.FoodMwicheTheFlusherAndCommodity);
		}
	
	case EnergyMwicheTheFlusherAndCommodity:
		{
		Cost rest = payCostAlt(Cost.Commodity,replay);
		Cost main = payCostAlt(Cost.EnergyMwicheTheFlusher,replay);
		if(rest==null) { return(main); }
		if(main==null) { return(rest); }
		return(Cost.EnergyMwicheTheFlusherAndCommodity);
		}	
	}

}

// get different mixes of food and knowledge depending
// on the knowledge of the workers in the farm.
Benefit doFoodSelection(EuphoriaBoard b,replayMode replay,int know)
{	int n = 1;

	if(hasActiveRecruit(Allegiance.Wastelander) 
			&& b.getAllegianceValue(Allegiance.Wastelander)>=ALLEGIANCE_TIER_1)
	{	// get extra food if you have the correct allegiance
		boolean secrets = penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets);
		if(secrets)
			{MarketChip.RegistryOfPersonalSecrets.logGameEvent(b,NoExtraFood); }
		else 
		{			if(recruitAppliesToMe(RecruitChip.ChristineTheAnarchist))
		{
			b.logGameExplanation(NoBonusChristineTheAnarchist,"Food");
		}else
		{ n++;	// 1 extra
		}}}
	
	if(know<=4) 
	{	
		b.incrementAllegiance(Allegiance.Wastelander,replay);
	}
	else if(know<=8)
	{	
		decrementKnowledge();
	}
	else
	{	// handles joseph the antiquer
		n ++;	// to 2 or 3
		incrementKnowledge(replay);
	}
	n = doMonotony(n);
	if(b.variation.isIIB())
	{	
		Benefit bene = null;
		if(	(n>=2)
			&& recruitAppliesToMe(RecruitChip.JosephTheAntiquer))
		{	b.logGameExplanation(ArtifactInPlaceOf,"Food");
			switch(n)
			{	
			case 2:	bene = Benefit.ArtifactOrFoodX2; break;
			case 3: bene = Benefit.ArtifactOrFoodX3; break;
			default: throw G.Error("Not expecting %s",n);
			}	
		}
		if((know>=9) && recruitAppliesToMe(RecruitChip.GwenTheMinerologist))
			{
			b.logGameExplanation(ResourceInPlaceOfCommodity,"Clay","Food");
			//b.p1("use gwen for food");
			if(bene!=null) 
				{
				switch(bene)
				{
				case ArtifactOrFoodX2:	bene = Benefit.ArtifactOrClayOrFoodX2; break;
				case ArtifactOrFoodX3:	bene = Benefit.ArtifactOrClayOrFoodX3; break;
				default: throw G.Error("Not expecting %s",n);
				}
				//b.p1("both joseph and gren for clay");
				}
				else 
				{
					doFood(n-1,b,replay);
					bene = Benefit.ClayOrFood;
				}
			}
		if(bene!=null) { return(bene); }
		}
	doFood(n,b,replay);
	return null;
}

boolean hasMyAuthorityToken(EuphoriaCell c)
{
	return(c.containsChip(myAuthority));
}
boolean penaltyAppliesToMe(MarketChip c)
{	if(c.isIIB() != b.variation.isIIB()) { return false;}	// not an appropriate test, so doesn't appluy
	EuphoriaCell m = b.getOpenMarketCell(c);
	if((m!=null) && !hasMyAuthorityToken(m)) { return(true); }
	return(false);
}
boolean recruitAppliesToMe(RecruitChip c)
{	if(c.isIIB() != b.variation.isIIB()) { return false; }	// from a different variation
	if(mandatoryEquality) { return(false); }
	if(hasUsedGeekTheOracle && (c==RecruitChip.GeekTheOracle)) { return(false); }	// once per turn
	return(activeRecruits.containsChip(c));
}

//get different mixes of energy and knowledge depending
//on the knowledge of the workers in the farm.
Benefit doPowerSelection(EuphoriaBoard b,replayMode replay,int know)
{	int n = 1;

	if(hasActiveRecruit(Allegiance.Euphorian) 
			&& b.getAllegianceValue(Allegiance.Euphorian)>=ALLEGIANCE_TIER_1)
	{	// get extra food if you have the correct allegiance
		boolean secrets = penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets);
		if(secrets) 
		{
			MarketChip.RegistryOfPersonalSecrets.logGameEvent(b,NoExtraEnergy);
		}
		else 
		{if(recruitAppliesToMe(RecruitChip.ChristineTheAnarchist))
			{
				b.logGameExplanation(NoBonusChristineTheAnarchist,"Energy");
			}else
		{ n++;	// 1 extra
		}}
	}
	
	if(know<=4) 
	{	
		b.incrementAllegiance(Allegiance.Euphorian,replay);
	}
	else if(know<=8)
	{	
		decrementKnowledge();
	}
	else
	{	// handles joseph the antiquer
		n ++;	// to 2 or 3
		incrementKnowledge(replay);
	}
	n = doMonotony(n);
	if(b.variation.isIIB())
		{
		Benefit bene = null;
		if( (n>=2) 
			&& recruitAppliesToMe(RecruitChip.JosephTheAntiquer))
		{	b.logGameExplanation(ArtifactInPlaceOf,"Energy");
			switch(n)
			{	
			case 2:	bene = Benefit.ArtifactOrEnergyX2; break;
			case 3: bene = Benefit.ArtifactOrEnergyX3; break;
			default: throw G.Error("Not expecting %s",n);
			}
		}
		if((know>=9) && recruitAppliesToMe(RecruitChip.GwenTheMinerologist))
		{
		//b.p1("use gwen for energy");
		b.logGameExplanation(ResourceInPlaceOfCommodity,"Gold","Energy");
		if(bene!=null) 
			{ 
			
			switch(bene)
			{
			case ArtifactOrEnergyX2:	bene = Benefit.ArtifactOrGoldOrEnergyX2; break;
			case ArtifactOrEnergyX3:	bene = Benefit.ArtifactOrGoldOrEnergyX3; break;
			default: throw G.Error("Not expecting %s",n);
			}
			//b.p1("both joseph and gren for gold");
			}
			else {
				doEnergy(n-1,b,replay);
				bene = Benefit.GoldOrEnergy;
			}
		
		}
		if(bene!=null) { return(bene); }		
		}
	
	return doEnergy(n,b,replay);
}

//get different mixes of food and knowledge depending
//on the knowledge of the workers in the farm.
Benefit doWaterSelection(EuphoriaBoard b,replayMode replay,int know)
{	
	int n = 1;
	if(recruitAppliesToMe(RecruitChip.FaithTheElectrician))
		{
		if(artifacts.height()==0) 
			{	doEnergy(1,b,replay);	// yes, it's supposed to be energy
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
		{ 
			if(recruitAppliesToMe(RecruitChip.ChristineTheAnarchist))
			{
				b.logGameExplanation(NoBonusChristineTheAnarchist,"Water");
			}else
			{n++;	// 1 extra
			}
		}
	}
	
	if(know<=4) 
	{	
		b.incrementAllegiance(Allegiance.Subterran,replay);
	}
	else if(know<=8)
	{	
		decrementKnowledge();
	}
	else
	{	// handles joseph the antiquer
		n++;	// to 2 or 3
		incrementKnowledge(replay);
	}	
	n = doMonotony(n);
	if(b.variation.isIIB())
	{	Benefit bene = null;
		if( (n>=2) 
			&& recruitAppliesToMe(RecruitChip.JosephTheAntiquer))
		{	b.logGameExplanation(ArtifactInPlaceOf,"Water");
			//b.p1("try the antiquer for water ");
			switch(n)
			{	
			case 2:	bene = Benefit.ArtifactOrWaterX2; break;
			case 3: bene = Benefit.ArtifactOrWaterX3; break;
			default: throw G.Error("Not expecting %s",n);
			}
		}
		if((know>=9) && recruitAppliesToMe(RecruitChip.GwenTheMinerologist))
		{
			b.logGameExplanation(ResourceInPlaceOfCommodity,"Stone","Water");
			//b.p1("use gwen for water");
			if(bene!=null) 
			{
				switch(bene)
				{
				case ArtifactOrWaterX2:	bene = Benefit.ArtifactOrStoneOrWaterX2; break;
				case ArtifactOrWaterX3:	bene = Benefit.ArtifactOrStoneOrWaterX3; break;
				default: throw G.Error("Not expecting %s",n);
				}
				//b.p1("both joseph and gren for stone");
				}
				else {
					doWater(n-1,b,replay);
					bene = Benefit.StoneOrWater;
				}
		}
		if(bene!=null) { return(bene); }		
		}

	doWater(n,b,replay);
	return null;
}

//get different mixes of food and knowledge depending
//on the knowledge of the workers in the farm.
Benefit doBlissSelection(EuphoriaBoard b,replayMode replay,int know)
{	
	int n = 1;

	if(hasActiveRecruit(Allegiance.Icarite) 
			&& b.getAllegianceValue(Allegiance.Icarite)>=ALLEGIANCE_TIER_1)
	{	// get extra food if you have the correct allegiance
		boolean secrets = penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets);
		if(secrets)  { MarketChip.RegistryOfPersonalSecrets.logGameEvent(b,NoExtraBliss); }
		else 
			{ 
			if(recruitAppliesToMe(RecruitChip.ChristineTheAnarchist))
			{
				b.logGameExplanation(NoBonusChristineTheAnarchist,"Bliss");
			}else
			{				n++;	// +1 extra
			} 
			}
	}

	if(know<=4) 
	{
		b.incrementAllegiance(Allegiance.Icarite,replay);
	}
	else if(know<=8)
	{
		decrementKnowledge();
	}
	else
	{	n++;		// to two or 3
		boolean knowMore = incrementKnowledge(replay);
		if(knowMore
				&& canPayAlt(null,Cost.Knowledge)
				&& recruitAppliesToMe(RecruitChip.BrendaTheKnowledgeBringer))
		{	// option to also lose knowledge and gain artifact
			b.makeBrenda(this);	// proceed normally
		}
	}		
	n = doMonotony(n);
	if(b.variation.isIIB() 
			&& (n>=2)
			&& recruitAppliesToMe(RecruitChip.JosephTheAntiquer))
		{	b.logGameExplanation(ArtifactInPlaceOf,"Bliss");
			switch(n)
			{	
			case 2:	return Benefit.ArtifactOrBlissX2;
			case 3: return Benefit.ArtifactOrBlissX3;
			default: throw G.Error("Not expecting %s",n);
			}
		}
		else
		{
		doBliss(n,b,replay);
		return null;
		}
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
 	 case WaterMwicheTheFlusher:
 	 case EnergyMwicheTheFlusher:
 	 case FoodMwicheTheFlusher:
 		 usingMwicheTheFlusher = (dest.size()==3);
 		 break;
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
			b.Assert(recruitAppliesToMe(RecruitChip.FlartnerTheLuddite),"should be flartner");
			incrementMorale();
			b.logGameEvent(FlartnerTheLudditeEffect,getPlayerColor());
			break;
		}
		//$FALL-THROUGH$
	case ResourceAndKnowledgeAndMorale:
		b.Assert(recruitAppliesToMe(RecruitChip.MichaelTheEngineer),"should be michael");
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
	case Energyx4_Stone:	// it takes both BrianTheViticulturist and JoshTheNegotiator to get these here.
	case Waterx4_Gold:
	case Blissx4_Card:
	case Energyx4_Clay:
	case Waterx4_Clay:
	case Foodx4_Stone:
	case Blissx4_Resource:
	case Resource:
		if(b.containsFood(dest)) { useBrianTheViticulturist(); }	// needed for awarding of influence
		break;
	case Bliss_Commodity:
	case Cardx6:
	case Cardx5:
	case Cardx4:
	case Cardx3:
	case Cardx2:
	case Card:			// discards due to morale checks, not payments
	case Artifact:
	case ConstructionSiteStone:
	case ConstructionSiteGold:
	case Gold:
	case ConstructionSiteClay:
	case Card_Resource:
	case BlissOrFood:
	case NonBlissCommodity:
	case Smart_Artifact:

	case Resourcex3:
	case CommodityOrResourcePenalty:
	case Foodx4_Card:
	case CardForGeek:
	case CardForGeekx2:
	case CommodityOrResourcex2Penalty:
	case CommodityOrResourcex3Penalty:
	case CommodityOrResourcex4Penalty:
		
	case Energyx4_Card:
	case Waterx4_Card:
	case Water:		// with mwiche these can be unresolved
	case Energy:
	case Food:
	case Commodity:
	case CommodityX2:
	case IsEuphorian:
	case IsSubterran:
	case IsWastelander:
	case Waterx3:
	case Energyx3:

		break;

	case Artifactx3:
	case Artifactx2:
	case Morale_Artifactx3:
		{	
		int ds = b.droppedDestStack.size();
		if(ds==3) { setTriggerPedroTheCollector(true); }
		if(ds==2) {	checkHasPaidArtifact(null);	}// any wildcard artifact in a pair
		}
		break;
		
	case Balloon_Stone:
	case Commodity_Balloons:
	case Balloon_Energy_Bliss:
	case BalloonsOrCardx2:
		checkHasPaidArtifact(ArtifactChip.Balloons);
		break;
		
	case Commodity_Bear:
	case Bear_Gold:
	case Bear_Energy_Food:
	case BearOrCardx2:
		checkHasPaidArtifact(ArtifactChip.Bear);
		break;
		
	case Commodity_Bat:
	case Bat_Stone:
	case Bat_Brick:
	case BatOrCardx2:
		checkHasPaidArtifact(ArtifactChip.Bat);
		break;
		
	case Commodity_Book:
	case Book_Stone:
	case Book_Brick:
	case Book_Card:
	case BookOrCardx2:
	case Book_Energy_Water:
		checkHasPaidArtifact(ArtifactChip.Book);
		break;
		
	case Commodity_Bifocals:
	case Glasses_Gold:
	case Glasses_Brick:
	case Glasses_Water_Bliss:
	case BifocalsOrCardx2:
		checkHasPaidArtifact(ArtifactChip.Bifocals);
		break;
		
	case Commodity_Box:
	case Box_Gold:
	case Box_Brick:
	case Box_Food_Bliss:
	case BoxOrCardx2:
		checkHasPaidArtifact(ArtifactChip.Box);
		break;
		
	default: throw G.Error("not expecting %s",cost);
	}
}
private void artifactIsDest(CellStack dest)
{	int n = artifacts.height();
	for(int lim=dest.size()-1; lim>=0; lim--)
	{
		EuphoriaCell c = dest.elementAt(lim);
		if(c.rackLocation()==EuphoriaId.PlayerArtifacts) 
			{ hasAddedArtifact = true; 
			  n--;
			  EuphoriaChip chip = artifacts.chipAtIndex(n);
			  if(chip==ArtifactChip.Balloons) { balloonsGained++; }
			  else if(chip==ArtifactChip.Bear) { bearsGained++; }
			  }
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
		
	case IcariteInfluenceAndCardx2:		// breeze bar, take 2 cards 1 at a time
	case IcariteAuthorityAndInfluence:	// other icarite spaces can get a card
	case Artifact:
	case FirstArtifact:
	case ResourceOrCommodity:
	case FreeArtifact:
	case Artifactx2for1:
	case StoneOrWater:
	case GoldOrEnergy:
	case ClayOrFood:
		if(dest!=null)
			{b.Assert(dest.size()==1,"should be one resouce");
			}
		break;
		
	case ArtifactOrGoldOrEnergyX2:
	case ArtifactOrGoldOrEnergyX3:
		if(b.hasTaken(b.goldMine))
		{
			b.logGameEvent(UseGwenTheMinerologist,"Gold","Energy");
		}
		//$FALL-THROUGH$
	case ArtifactOrStoneOrWaterX2:
	case ArtifactOrStoneOrWaterX3:
		if(b.hasTaken(b.quarry))
		{
			b.logGameEvent(UseGwenTheMinerologist,"Stone","Water");
		}
		//$FALL-THROUGH$
	case ArtifactOrClayOrFoodX2:
	case ArtifactOrClayOrFoodX3:		
	    if(b.hasTaken(b.clayPit))
	    {
	    	b.logGameEvent(UseGwenTheMinerologist,"Clay","Food");
	    }
		//$FALL-THROUGH$
	case ArtifactOrWaterX2:
	case ArtifactOrBlissX2:
	case ArtifactOrFoodX2:
	case ArtifactOrEnergyX2:
	case ArtifactOrEnergyX3:
	case ArtifactOrBlissX3:
	case ArtifactOrFoodX3:
	case ArtifactOrWaterX3:	
		if(dest.size()==1)
		{
			b.logGameEvent(UseJosephTheAntiquer);
		}
		break;
	case Commodityx3:
		if(dest!=null) { b.Assert(dest.size()==3,"should be 3 commodities"); }		
		break;
	case Commodityx2:
	case ResourceAndCommodity:
		if(dest!=null) { b.Assert(dest.size()==2,"should be 2 items"); }
		break;
	default: throw b.Error("Not expecting to collect deferred benefit %s",benefit);
	}
}


// place a star on one of the markets or in the market territory
// and gain influence on the authority track.
Benefit collectMarketAuthority(EuphoriaBoard b,Benefit bene,Allegiance a,replayMode replay,boolean influence)
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
		return(null);
		}
		return(bene);	// have to decide
}

// limit increases of commodities due to market penalty IIB_LotteryOfDiminishingReturns
int doLottery(int n,EuphoriaCell c)
{
	int height = c.height();
	if((height+n>2)
		&& penaltyAppliesToMe(MarketChip.IIB_LotteryOfDiminishingReturns))
		{  
		n = Math.min(n, 2-height);
		MarketPenalty mp = MarketChip.IIB_LotteryOfDiminishingReturns.marketPenalty;
		//b.p1("limit commodities due to IIB_LotteryOfDiminishingReturns ");
		b.logGameEvent(mp.explanation);
		}
	return(n);
}

void doBliss(int n0,EuphoriaBoard b,replayMode replay)
{	int n = doLottery(n0,bliss);
	for(int i=0;i<n;i++) { addBliss(b.getBliss()); if(replay!=replayMode.Replay) { b.animateNewBliss(bliss); }}
}
void doFood(int n0,EuphoriaBoard b,replayMode replay)
{	int n = doLottery(n0,food);
	for(int i=0;i<n;i++) { addFood(b.getFood()); if(replay!=replayMode.Replay) { b.animateNewFood(food); }}
}
void doWater(int n0,EuphoriaBoard b,replayMode replay)
{	int n = doLottery(n0,water);
	for(int i=0;i<n;i++) { addWater(b.getWater()); if(replay!=replayMode.Replay) { b.animateNewWater(water); }}
}

Benefit doEnergy(int n0,EuphoriaBoard b,replayMode replay)
{	
	int n = doLottery(n0,energy);
	int egg = energyGainedThisTurn;
	energyGainedThisTurn += n;
	for(int i=0;i<n;i++) { addEnergy(b.getEnergy()); if(replay!=replayMode.Replay) { b.animateNewEnergy(energy); }}
	if((egg<3) 
			&& (energyGainedThisTurn>=3)
			&& recruitAppliesToMe(RecruitChip.FrazerTheMotivator))
	{
		//b.p1("use frazerthemotivator for "+n0);
		energy.removeTop();   // take one back
		return Benefit.Commodity;
	}
	return null;
}

private void doClay(int n0,EuphoriaBoard b,replayMode replay)
{	int n = doForcedAltruism(n0);
	while(n-- > 0)
	{
	addClay(b.getClay());		
	if(replay!=replayMode.Replay) { b.animateNewClay(clay); }
	}
}
private void doGold(int n0,EuphoriaBoard b,replayMode replay)
{	int n = doForcedAltruism(n0);
	while(n-- > 0)
	{
	addGold(b.getGold());		
	if(replay!=replayMode.Replay) { b.animateNewGold(gold); }
	}
}
int doForcedAltruism(int n)
{
	if(penaltyAppliesToMe(MarketChip.IIB_PalaceOfForcedAltruism))
	{
		int tot = totalResources();
		int newn = Math.min(Math.max(0,3-tot),3);
		if(newn<n)
		{	b.p1("lose resoruces to altruism");
			int lost = (n-newn);
			b.logGameEvent(LoseResourcesFromPalace,""+lost);
			lostToAltruism += lost;
			someLostToAltruism = true;
			return(newn);
		}
	}
	return n;
}
private void doStone(int n0,EuphoriaBoard b,replayMode replay)
{	
	int n = doForcedAltruism(n0);
	while(n-- > 0)
	{
	addStone(b.getStone());		
	if(replay!=replayMode.Replay) { b.animateNewStone(stone); }
	}
}
public Benefit doArtifact(int n,EuphoriaBoard b,replayMode replay)
{	// IIB can't just give you an artifact
	if(b.variation.isIIB())
	{	G.Assert(n==1,"only one");
		return Benefit.Artifact;
	}
	while(n-- > 0)
	{
		EuphoriaChip a = b.getArtifact();
		if(a!=null)
		{	addArtifact(a);
			if(replay!=replayMode.Replay) { b.animateNewArtifact(artifacts); }			
		}
	}
	return(null);
}

// gain 1 less commodity, but at least 1
public int doMonotony(int n)
{
	if((n>1) && penaltyAppliesToMe(MarketChip.IIB_TheaterOfEndlessMonotony))
	{
		MarketPenalty p = MarketChip.IIB_TheaterOfEndlessMonotony.marketPenalty;
		//b.p1("lose commodity due to IIB_TheaterOfEndlessMonotony"); tested feb 6
		b.logGameEvent(p.explanation);
		return(n-1);
	}
	return n;
}
boolean collectBenefitOrElse(Benefit benefit,replayMode replay)
{
	return G.Assert(collectBenefit(benefit,replay)==null,"collection must succeed");
}
private void checkGaryTheForgetter()
{
	if(recruitAppliesToMe(RecruitChip.GaryTheForgetter))
	{	//b.p1("use gary the forgetter");
		b.logGameEvent("lose 2 Knowledge (Gary the Forgetter)");
		decrementKnowledge();
		decrementKnowledge();
	}
}
/**
 *  return true if the choice action is fully resolved, 
 * @param benefit
 * @param b
 * @param replay
 * @return false if we need to interact with the user to resolve it.
 */
Benefit collectBenefit(Benefit benefit,replayMode replay)
{	
	switch(benefit)
	{
	default: throw b.Error("Unexpected benefit code %s",benefit);
	
	case ArtifactOrClayOrFoodX2:	// interaction with gwen the minerologist
	case ArtifactOrClayOrFoodX3:
	case ArtifactOrStoneOrWaterX2:
	case ArtifactOrStoneOrWaterX3:
	case ArtifactOrGoldOrEnergyX2:
	case ArtifactOrGoldOrEnergyX3:
		return(benefit);
		
	case None:	
		return(null);
	case Artifactx2for1:	// always interact
		return(benefit);
	case Stonex2:
		doStone(doMonotony(2),b,replay);
		return(null);
	case Goldx2:
		doGold(doMonotony(2),b,replay);
		return(null);
	case Clayx2:
		doClay(doMonotony(2),b,replay);
		return(null);
	case Bliss: 	//AmandaTheBroker
		doBliss(1,b,replay);
		return(null);
	case Blissx4:	//ShepphardTheLobotomist
		doBliss(doMonotony(4),b,replay);
		return(null);
	case Commodity:
	case Commodityx2:
	case Commodityx3:
	case Resource:
	case WaterOrStone:
	case WaterOrEnergy:
		return(benefit);
	case Artifactx2:	// collect 2 artifact cards
		if(b.variation.isIIB()) { return(benefit); }	// must interact
		return doArtifact(2,b,replay);
	case FirstArtifact:	// first of 2
	case Artifact:	// collect a single card
		return doArtifact(1,b,replay);
		
	case FoodSelection:
		return(doFoodSelection(b,replay,commodityKnowledge));
	case PowerSelection:
		return(doPowerSelection(b,replay,commodityKnowledge));
	case Water:
		doWater(1,b,replay);
		return(null);
	case WaterSelection:
		return(doWaterSelection(b,replay,commodityKnowledge));
	case BlissSelection:
		doBlissSelection(b,replay,commodityKnowledge);
		return null;

		
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
		checkGaryTheForgetter();
		doAlbertTheFounder(replay);
		return(null);
		
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
		checkGaryTheForgetter();
		doAlbertTheFounder(replay);
		return(null);
		
	case CardAndGold:
		{
		if(penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets))
		{
		MarketChip.RegistryOfPersonalSecrets.logGameEvent(b); 
		return(collectBenefit(Benefit.CardOrGold,replay));
		}
		else {
		doGold(1,b,replay);
		return doArtifact(1,b,replay);
		}}
		
	case CardAndStone:
		if(penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets))
		{
		MarketChip.RegistryOfPersonalSecrets.logGameEvent(b); 
		return(collectBenefit(Benefit.CardOrStone,replay));
		}
		else
		{
		 doStone(1,b,replay);
		 return doArtifact(1,b,replay);
		}
	case CardAndClay:
		if(penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets))
		{
		MarketChip.RegistryOfPersonalSecrets.logGameEvent(b); 
		return(collectBenefit(Benefit.CardOrClay,replay));
		}
		else
		{doClay(1,b,replay);
		 return doArtifact(1,b,replay);
		}
	case Waterx3:
		doWater(doMonotony(3),b,replay);
		return(null);
	case Energy:
		return doEnergy(1,b,replay);

	case Energyx2:
		return doEnergy(doMonotony(2),b,replay);
		
	case Energyx3:
		int n = doMonotony(3);
		return doEnergy(n,b,replay);

	case Foodx4:
		doFood(doMonotony(4),b,replay);
		return(null);
	case Foodx3:
		doFood(doMonotony(3),b,replay);
		return(null);
	case Food:
		doFood(1,b,replay);
		return(null);
	case Foodx2:
		doFood(2,b,replay);
		return(null);
	case MoraleOrKnowledge:
		if(morale == MAX_MORALE_TRACK)
		{	decrementKnowledge(); 
			b.logGameEvent(EsmeTheFiremanKnowledge,getPlayerColor());
			return(null); 
		} if(knowledge==1)
			{ incrementMorale();
				b.logGameEvent(EsmeTheFiremanMorale,getPlayerColor());
			  return(null);
			}
		return(benefit);
	case Moralex2OrKnowledgex2:
		if(morale == MAX_MORALE_TRACK)
		{	decrementKnowledge();
			decrementKnowledge();
			b.logGameEvent(EsmeTheFiremanKnowledgex2,getPlayerColor());
			return(null); 
		} if(knowledge==1)
			{ incrementMorale();
			  incrementMorale();
			  b.logGameEvent(EsmeTheFiremanMoralex2,getPlayerColor());
			  return(null);
			}
		return(benefit);
		
	case Moralex2AndKnowledgex2:
		decrementKnowledge();
		decrementKnowledge();
		incrementMorale();
		incrementMorale();
		b.logGameEvent(EsmeTheFiremanKnowledgex2,getPlayerColor());
		return(null); 
		
	case IcariteInfluenceAndCardx2:
		if(b.variation.isIIB()) { return(benefit); }	// must interact
		else {b.incrementAllegiance(Allegiance.Icarite,replay);
		 doArtifact(2,b,replay);
		}
		return(null);

	case IcariteAuthorityAndInfluence:
		doAuthorityAndInfluence(b,Allegiance.Icarite,replay,true);
		if(hasActiveRecruit(Allegiance.Icarite) 
			&& (b.getAllegianceValue(Allegiance.Icarite)>=ALLEGIANCE_TIER_2))
			{
			if(penaltyAppliesToMe(MarketChip.RegistryOfPersonalSecrets)) 
			{ MarketChip.RegistryOfPersonalSecrets.logGameEvent(b);
			}
			else if(b.variation.isIIB()) { return(Benefit.Artifact); }	// must interact
			else 
			{ doArtifact(1,b,replay);
			}
			
			}
		return(null);
		
	case EuphorianStar:
		doAuthority(b,Allegiance.Euphorian,replay);
		return(null);

	case WastelanderStar:
		doAuthority(b,Allegiance.Wastelander,replay);
		return(null);

	case SubterranStar:
		doAuthority(b,Allegiance.Subterran,replay);
		return(null);
		
	case EuphorianAuthorityAndInfluence:
		doAuthorityAndInfluence(b,Allegiance.Euphorian,replay,true);
		return(null);
		
	case WastelanderAuthorityAndInfluence:
		doAuthorityAndInfluence(b,Allegiance.Wastelander,replay,true);
		return(null);
		
	case SubterranAuthorityAndInfluence:
		doAuthorityAndInfluence(b,Allegiance.Subterran,replay,true);
		return(null);
		
	case WastelanderAuthority2:
		return(collectMarketAuthority(b,benefit,Allegiance.Wastelander,replay,true));
		
	case EuphorianAuthority2:
		return(collectMarketAuthority(b,benefit,Allegiance.Euphorian,replay,true));
		
	case SubterranAuthority2:
		return(collectMarketAuthority(b,benefit,Allegiance.Subterran,replay,true));

	case WaterOrMorale:		// soulless the plumber
		if(morale==MAX_MORALE_TRACK) { doWater(1,b,replay); return(null); }
		return(benefit);
		
	case Gold:	// LauraThePhilanthropist gives this away
		doGold(1,b,replay);
		return(null);
	case Clay:
		doClay(1,b,replay);
		return(null);
	case Stone:
		doStone(1,b,replay);
		return(null);
	case MoraleOrEnergy:
		if(morale==MAX_MORALE_TRACK)
		{
			return doEnergy(1,b,replay);
		}
		return(benefit);
	case KnowledgeOrFood:	// scarby the harvester
		if(knowledge==1) 	// if knowledge is already at minimum, take food
		{
			doFood(1,b,replay);
			return(null);
		}
		return(benefit);
	case KnowledgeOrBliss:
		if(knowledge==1)
		{
			doBliss(1,b,replay);
			return(null);
		}
		return(benefit);
		


	case IcariteInfluenceAndResourcex2:
	case CardOrGold:
	case CardOrStone:
	case CardOrClay:
		return(benefit);		// interact to get these
	
	case Morale:	// samuel the zapper
		return incrementMorale();

	}

}
private void doAlbertTheFounder(replayMode replay)
{
	if(recruitAppliesToMe(RecruitChip.AlbertTheFounder)
			&& (totalWorkers<MAX_WORKERS))
	{	// b.p1("use albert the founder");
		// gain another worker and a resoruce
		addNewWorker(WorkerChip.getWorker(color,1));	// will be rerolled
		totalWorkers++;
		if(replay!=replayMode.Replay)
		{
			b.animateNewWorkerA(newWorkers);
		}
		b.collectResourceNext();
	}
}

public boolean canResolveDilemma()
{	return(!dilemmaResolved && canPay(null,((DilemmaChip)dilemma.topChip()).cost));
}

// check a version 3 recruit if we think it's handled for this purpose
public void checkV3DropWorker(EuphoriaCell dest,replayMode replay) {
	for(int lim=activeRecruits.height()-1; lim>=0; lim--)
	{
		RecruitChip ch = (RecruitChip)activeRecruits.chipAtIndex(lim);
		if(ch.isIIB())
		{
		switch(ch.recruitId)
		{
		case 234:	// DougTheBuilder, sacrifice a worker instead of paying 
		case 240:	// chaga the gamer, pay box to activate another recruit
		case 242:	// RowenaTheMentor ignore market penalties for 1 turn
		case 247:	// julia the acolyte, no action on drop, special actions when bumped
		case 249:	// lionel the cook, use food to ignore market penalty
		case 251:	// GeorgeTheLazyCraftsman self-bump when placing on some markets
			
		default:	
			b.p1("drop with "+ch.name+" #"+ch.recruitId);
			//throw b.Error("recruit %s #%s not handled for DropWorker",ch,ch.recruitId);
			break;
		case 221:	// Lieve the Briber pay bliss for card on tunnel spaces
		case 222:	// AhmedTheArtifactDealer, discount on artifacts
		case 223:	// amina the bliss bringer, lose worker at 17 instead of 16
		case 224:	// terri the bliss trader, trade bliss for other resource when gain star
		case 225:	// ted the contingency planner, can use bliss in tunnels
		case 226:	// gary the forgetter can use bliss to train workers, lose extra knowledge
		case 227:	// BokTheGameMaster, max 4 on the knowledge track
		case 228:	// KebTheInformationTrader gets resource or commodity on risky roll
		case 229:	// BrendaTheKnowledgeBringer get extra card for extra knowledge
		case 230:	// Mosi the Patron, use bliss instead of card in artifact markets
		case 232:	// zara the solophist, double knowledge on commodity areas
		case 233:	// jon the amateur handyman allows using 3 commodity to build a market
		case 235:	// cary the care bear, extra stuff when gaining a bear
		case 236:	// EkaterinaTheCheater, box artifact wildcard
		case 237:	// MiroslavTheConArtist, bear artifact wildcard 
		case 239:	// PmaiTheNurse lets you move down in commodity knowledge
		case 241:	// ha-joon the gold trader,no action on drop
		case 243:	// frazerTheMotivator, gain any commodity instead of third energy
		case 244:   // samuel the zapper, believed to be complete 2/1/2022
					// lots of nuances for samuelthezapper!		
		case 245:	// lars the ballooner, take a second action if you gain a balloon
		case 246:	// xyonthebrainsurgeon rescues a worker with a card
		case 250:	// alexandra the heister, artifact balloon wildcard
		case 252:	// gwen the minerologist, get resource instead of 1 commodity
		case 253:	// JoseThePersuader, bat artifact wildcard\
		case 254:	// jedidiahtheinciter, bump a worker from commodity area
		case 255:	// DarrenTheRepeater, lose morale to bump yourself
		case 256:	// high general baron, no action on drop
		case 257:	// Joseph the antiquer, switch to artifact benefit 
		case 258:	// Milos the Brainwasher, pay to skip knowledge check
		case 259:	// kaleef the browuiser, action on bump worker
		case 260:	// pedro the collector, get commodity and resource when paying 3 cards
		case 261:	// shaheena the digger, artifact for stone at the end of turn
		case 263:	// mwichwe the flusher, special cost/benedif at tunnels
		case 264:	// MakatoTheForger, bifocals artifact wildcard
		case 265:	// albert the founder
		case 262:	// dustytheenforcer saves workers 
		case 266:	// PamhidzaiTheReader trade card for 3 resources
		case 267:	// borna the storyteller, no action on drop
		case 268:	// javier the underground librarian, artifact book wildcard
		case 269:	// christine the anarchist, bonus when retrieving
		case 271:	// jeroen the hoarder, gain resource when retrieving workers
			break;
		
		}}
	}
}

// return true if any of our workers are on a commodity space
public boolean hasCommodityWorkers()
{	
	for(EuphoriaCell c : placedWorkers)
	{	if(c!=null)
		{
		EuphoriaId rack = c.rackLocation();
		if(G.arrayContains(CommodityIds, rack)) { return(true); }
		}
	}
	return(false);
}


}