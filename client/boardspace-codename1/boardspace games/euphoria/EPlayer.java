package euphoria;


import java.util.Hashtable;

import lib.Random;
import online.game.replayMode;
import lib.AR;
import lib.Bitset;
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
EuphoriaCell spareRecruits;		// per-player additional recruits to replace factionless that will be discarded

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

enum PFlag {
	SteveTheDoubleAgent_Subterran,		// steve star for subterran
	SteveTheDoubleAgent_Wastelander,	// steve star for wastelander
	HasResolvedDilemma,					// dilemma resolved
	ReceiveStartingBonus,			// need a starting bonus
	RetrievePrisonersDilemma,		// retrieve only turn for prisoners dilemma]
	RetrievePrisonersDilemmaAfter,
	RetrievePrisonersDilemmaBefore,
	StartWithKofiTheHermit,
}
Bitset<PFlag> pf = new Bitset<PFlag>();

public void setPFlag(PFlag val) { pf.set(val); }
public boolean testPFlag(PFlag val) { return(pf.test(val)); }
public void clearPFlag(PFlag val) { pf.clear(val); }
//
//these variables reflect events on the current turn
//
enum TFlag {
	HasLostWorker,
	AddedArtifact,
	AddedArtifactLast,
	UsedGeekTheOracle,
	GainedWorker,
	Peeked,		// has seen the new card
	UsedBrianTheViticulturist,
	UsedSpirosTheModelCitizen,
	UsedDarrenTheRepeater,
	UsedSamuelTheZapper,		// true when we reroll additional workers 
	UsingSamuelTheZapper,		// true when we have paid and willretrieve
	UsingMwicheTheFlusher,		// true if actually paying mwiche 3 waters.
	AskedShaheenaTheDigger,		// true if traded stone for artifact
	SomeLostToAltruism,
	PlayedLostSound,
	TriggerPedroTheCollector,	// true if uses 3 artifacts
	TriggerGeorgeTheLazyCraftsman,	// true if placed on a new territory
	UsedJadwigaTheSleepDeprivator,	// true if we used the capability to play again
	UsingRowenaTheMentor,			// true if we have activated rowena, and ignore market penalties
	TriggerLarsTheBallooneer,		// can ask about larstheballooneer
	UsedLarsTheBallooneer,			// we used him
	UsedJonathanTheGambler,
	ResultsInDoubles,	
	UsedJackoTheActivist,			// used jacko, everyone gets a card
	TriggerMaggieTheOutlaw,			// all retrieved are euphorian
	UsedChaseTheMiner,
	UsedLionelTheCook,			// ignore a market penalty for food
	TriggerKofiTheHermit,		// retrieved for free, get a new turn
	UseChagaTheGamer,			// used it
	UsedFrazerTheMotivator,		// used him once
	UsedBornaTheStoryteller, HasLostMorale, UsedJonathanTheGamblerThisWorker, UsedJonathanTheGamblerThisTurn,
	AskedJonathanTheGambler,	// used her once
}
Bitset<TFlag> tf = new Bitset<TFlag>();

public void setTFlag(TFlag val) 
{ 	tf.set(val);
}
public boolean testTFlag(TFlag val)
{	return tf.test(val);

}
public void clearTFlag(TFlag val)
{ 	tf.clear(val);
}

private boolean mandatoryEquality = false;	// market penalty for mandatory equality
int lostToAltruism = 0;			// lost resources to altryism this turn
int bearsGained = 0;
int energyGainedThisTurn = 0;
int commodityKnowledge = 0;	// differential for knowledge on commodity spaces, PmaiTheNurse

int samuelTheZapperLevel = 0;				// the number of retrieved workers when we start
int terriAuthorityHeight = 0;	//
int taedAuthorityHeight = 0;	// gained authority for taed the brick trader


public boolean getTriggerPedroTheCollector() { return testTFlag(TFlag.TriggerPedroTheCollector); }
public void setTriggerPedroTheCollector(boolean v) 
{ if(recruitAppliesToMe(RecruitChip.PedroTheCollector))
	{ if(v) 
		{
		//b.p1("trigger pedro the collector");
		setTFlag(TFlag.TriggerPedroTheCollector); 
		}
		else { clearTFlag(TFlag.TriggerPedroTheCollector); }
	}
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
	throw b.Error("No room for worker");
}
public void unPlaceWorker(EuphoriaCell c)
{
	for(int lim=placedWorkers.length-1; lim>=0; lim--)
	{
		if(placedWorkers[lim]==c) { placedWorkers[lim]=null; return; }
	}
	throw b.Error("No worker found");
}

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
public void addActiveRecruit(EuphoriaChip ch,replayMode replay)
{
	activeRecruits.addChip(ch);
	if(ch==RecruitChip.SteveTheDoubleAgent)
	{	
		for(Allegiance faction : Allegiance.values())
		{
		if(b.getAllegianceValue(faction)>=(AllegianceSteps-1)) 
			{ 
			//b.p1("Activate steve the double agent "+faction);
			activateForSteveTheDoubleAgent(faction,replay);
			}
		}			
	}
	if((ch==RecruitChip.BokTheGameMaster)
		&& (knowledge>4))
	  { setKnowledge(4,replay);
		//b.p1("BokTheGameMaster saves");
	  	b.logGameEvent(UseBokTheGameMasterNow);
	  }
	
	setupAlternateArtifacts();
}

// notification when a new market opens.
public void newMarketOpened()
{	checkMandatoryEquality();	// new market opened, maybe has a new penalty
}

// clear status at the start of a new turn
public void startNewTurn(replayMode replay)
{	boolean aa = testTFlag(TFlag.AddedArtifact);
	boolean sam = testTFlag(TFlag.UsedSamuelTheZapper);
	startNewWorker();
	tf.clear();
	if(aa) { setTFlag(TFlag.AddedArtifactLast); }
	bearsGained = 0;
	lostToAltruism = 0;
	energyGainedThisTurn = 0;
	commodityKnowledge = 0;
	commodityKnowledge = 0;
	samuelTheZapperLevel = authority.height();
	if(sam)
	{	
		collectBenefit(Benefit.Morale,replay);
	}
	checkMandatoryEquality();
	
}

public void startNewWorker()
{
	usedAlternateArtifact = null;
	tf.clear(TFlag.AddedArtifact,
			 TFlag.GainedWorker, 
			 TFlag.UsedJonathanTheGamblerThisWorker,
			 TFlag.AskedJonathanTheGambler);

}

// cache the status of AcadamyOfMandatoryEquality, which interferes with recruit abilities.
public void checkMandatoryEquality()
{
	mandatoryEquality = penaltyAppliesToMe(MarketChip.AcademyOfMandatoryEquality);
}
public void useGeekTheOracle() { setTFlag(TFlag.UsedGeekTheOracle); }
public boolean hasUsedBrianTheViticulturist() { return(testTFlag(TFlag.UsedBrianTheViticulturist));}
private void useBrianTheViticulturist() { setTFlag(TFlag.UsedBrianTheViticulturist); }
// assist for the UI
PlayerView hiddenView = PlayerView.Normal;
PlayerView view = PlayerView.Normal;
PlayerView pendingView = PlayerView.Normal;

EuphoriaCell cardArray[] = new EuphoriaCell[6];

public boolean hasReducedRecruits()
{	
	return(newRecruitCardCount()==0);
}

// count both pending and current recruits with a given allegiance
int countRecruits(Allegiance a)
{	int n = 0;
	for(EuphoriaCell c : newRecruits)
	{	n+= countRecruits(c,a);
	}
	n += countRecruits(activeRecruits,a);
	n += countRecruits(hiddenRecruits,a);
	n += countRecruits(discardedRecruits,a);
	return(n);
}

int countRecruits(EuphoriaCell activeRecruits,Allegiance a)
{	int n=0;
	for(int lim=activeRecruits.height()-1; lim>=0; lim--)
	{	RecruitChip recruit = (RecruitChip)(activeRecruits.chipAtIndex(lim));
		if (recruit.allegiance == a) { n++; }
	}
	return(n);
}
void animateKnowledge(int step,replayMode replay)
{
	if(replay!=replayMode.Replay) {
		b.animationStack.push(b.knowlegeTrack[knowledge-1]);
		b.animationStack.push(b.knowlegeTrack[knowledge+step-1]);
	}
}
void animateMorale(int step,replayMode replay)
{
	if(replay!=replayMode.Replay) {
		b.animationStack.push(b.moraleTrack[morale-1]);
		b.animationStack.push(b.moraleTrack[morale+step-1]);
	}
}
boolean incrementKnowledge(replayMode replay) 
{ 	
	if(knowledge<MAX_KNOWLEDGE_TRACK) 
	{ 
	  animateKnowledge(1,replay);
	  knowledge++;
	  if(recruitAppliesToMe(RecruitChip.BokTheGameMaster))
	  {	  if(knowledge>4)
	  		{	knowledge = 4;
	  			b.logGameEvent(UseBokTheGameMaster);
	  			b.useRecruit(RecruitChip.BokTheGameMaster,"limit");
	  		}
	  }
	  if(recruitAppliesToMe(RecruitChip.NickTheUnderstudy))
	  {
		  incrementMorale(replay);
		  b.logGameEvent(NickTheUnderstudyEffect,getPlayerColor());
		  b.useRecruit(RecruitChip.NickTheUnderstudy,"morale");
	  }
	  return(true);
	}
	return(false);
}
public void setKnowledge(int n,replayMode replay) { knowledge = n; }
boolean decrementKnowledge(replayMode replay) 
{ if(knowledge>MIN_KNOWLEDGE_TRACK)
		{ animateKnowledge(-1,replay);
		knowledge--; 
		return(true); } 
	return(false); 
}
Benefit incrementMorale(replayMode replay) 
{ 
	if(morale>=3 && penaltyAppliesToMe(MarketChip.IIB_ConcertHallOfHarmoniousDischord))
	{	MarketPenalty p = MarketChip.IIB_ConcertHallOfHarmoniousDischord.marketPenalty;
		//b.p1("inhibit morale gain");
		b.logGameEvent(p.explanation);
		return(Benefit.Morale);
	}
	if(morale<MAX_MORALE_TRACK)
	{ animateMorale(1,replay);
	  morale++; 
	  return(null); 
	}
	return(Benefit.Morale);

}
private boolean decrementMorale(replayMode replay) 
{ if(morale>MIN_KNOWLEDGE_TRACK)	 
	{ animateMorale(-1,replay);
	  morale--; 
	  //G.print("dec m "+this+" ="+artifacts.height()+" "+morale);
	  if(recruitAppliesToMe(RecruitChip.CurtisThePropagandist))
	  {	  // also lose knowledge
		  if(decrementKnowledge(replay))
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
{	setTFlag(TFlag.AddedArtifact);		// bookkeeping for GeekTheOracle and JonathanTheGambler
	artifacts.addChip(c); 
	if(c==ArtifactChip.Bear) { bearsGained++; }	// count for carythecarebear 
	if(c==ArtifactChip.Balloons) { setTFlag(TFlag.TriggerLarsTheBallooneer); }	//count for lars the ballooner
	//G.print("add a "+this+" ="+artifacts.height()+" "+morale);
}
public boolean setHasPeeked(boolean v) 
{ 	if(v) 
		{ if(testTFlag(TFlag.AddedArtifact)|testTFlag(TFlag.AddedArtifactLast)) 
			{
			setTFlag(TFlag.Peeked);
			}
		}
	return(testTFlag(TFlag.Peeked)); 
}
public boolean hasPeeked() { return(testTFlag(TFlag.Peeked)); }
public boolean hasGainedWorker() { return(testTFlag(TFlag.GainedWorker)); }
public void addStone(EuphoriaChip c) { stone.addChip(c); }
public void addGold(EuphoriaChip c) { gold.addChip(c); }
public void addWater(EuphoriaChip c) { water.addChip(c); }
public void addEnergy(EuphoriaChip c) { energy.addChip(c); }
public void addBliss(EuphoriaChip c) { bliss.addChip(c); }
public void addClay(EuphoriaChip c) { clay.addChip(c); }
public void addFood(EuphoriaChip c) { food.addChip(c); }


public EuphoriaChip getAuthorityToken(replayMode replay) 
{	if(authority.height()>0)
		{
		if(penaltyAppliesToMe(MarketChip.IIB_AthenaeumOfMandatoryGuidelines))
			{
			MarketPenalty p = MarketChip.IIB_AthenaeumOfMandatoryGuidelines.marketPenalty;
			if(decrementMorale(replay))
				{ //b.p1("lose morale due to IIB_AthenaeumOfMandatoryGuidelines" );
				  b.logGameEvent(p.explanation);	
				}
			}
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
{	EuphoriaChip tok = getAuthorityToken(replay);
	if(tok!=null) 
		{ allegianceStars.addChip(tok); 
		  if(replay!=replayMode.Replay)
		  {
			  b.animatePlacedItem(authority,allegianceStars);
		  }
		}
}
void activateForSteveTheDoubleAgent(Allegiance faction,replayMode replay)
{
	PFlag flag = null;
	//b.p1("Use steve for "+faction);
	b.useRecruit(RecruitChip.SteveTheDoubleAgent,faction.name());
	switch(faction)
	{
	case Subterran:	
		b.logGameEvent(UseSteveFor,color.name(),"Subterran");
		flag = PFlag.SteveTheDoubleAgent_Subterran; 
		break;
	case Wastelander: 
		b.logGameEvent(UseSteveFor,color.name(),"Wastelander");
		flag = PFlag.SteveTheDoubleAgent_Wastelander; 
		break;
	case Icarite:
	case Factionless:
	case Euphorian: break;
	default: throw b.Error("Not expecting "+faction);
	}
	if(flag!=null && !testPFlag(flag))
	{	setPFlag(flag);
		if(authority.height()>0) { addAllegianceStar(replay); } 
	}
}
//
// award the start for current recruits.
//
void awardAllegianceStars(Allegiance faction,replayMode replay)
{	
	if(recruitAppliesToMe(RecruitChip.SteveTheDoubleAgent))
	{	activateForSteveTheDoubleAgent(faction,replay);
	}
	{
	for(int lim=activeRecruits.height()-1; lim>=0; lim--)
	{
		RecruitChip ch = (RecruitChip)activeRecruits.chipAtIndex(lim);
		if((ch!=RecruitChip.SteveTheDoubleAgent) && (ch.allegiance==faction))
		{	b.logGameEvent(GainStarFor,color.name(),faction.name());
			if(authority.height()>0) { addAllegianceStar(replay); } 
		}
	}}
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
	add(discardedRecruits = new EuphoriaCell(RecruitChip.Subtype(),r,EuphoriaId.PlayerDiscardedRecruits,color));
	spareRecruits = new EuphoriaCell(RecruitChip.Subtype(),r,EuphoriaId.PlayerSpareRecruits,color);
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
	pf.clear();		// no permanant conditions resolved
	
	myAuthority = EuphoriaChip.getAuthority(color);
	reloadNewRecruits(rec);
	int na = countRecruits(Allegiance.Factionless);
	// load the replacements for factionless recruits.  You're only
	// allowed to keep one, but the replacements might be factionless too.
	while(na>1)
		{ RecruitChip recruit = (RecruitChip)rec.removeTop();
		  spareRecruits.addChip(recruit);		// add them even if they are factionless
		  if(recruit.allegiance!=Allegiance.Factionless) 
		  	{ na--; 
		  	}
		}
	
	for(int i=0;i<STARTING_AUTHORITY_TOKENS;i++) {  authority.addChip(myAuthority); }
	dilemma.addChip(dil.removeTop());		// 1 randomly selected ethical dilemma
	view = PlayerView.Normal;
	hiddenView = PlayerView.Normal;
	pendingView = PlayerView.Normal;
	mandatoryEquality = false;
	samuelTheZapperLevel = 0;
	taedAuthorityHeight = terriAuthorityHeight = authority.height();
	originalHiddenRecruit = null;
	originalActiveRecruit = null;
	penaltyMoves = 0;
	cardsLost = 0;
	workersLost = 0;
	bearsGained = 0;
	lostToAltruism = 0;
	placements = 0;
	energyGainedThisTurn = 0;
	commodityKnowledge = 0;
	retrievals = 0;
	marketStars = 0;
	
	tf.clear();
	
	AR.setValue(placedWorkers,null);
}
void reloadNewRecruits(EuphoriaCell from)
{
	for(int i=0;i<STARTING_RECRUITS;i++) 
		{ if(newRecruits[i].topChip()==null) 
			{ 
			if(from.topChip()==null)
				{ // randomizing recruits can get it wrong
				from.addChip(b.getRecruit()); 
				}
			newRecruits[i].addChip(from.removeTop()); 
			}
		}		
}

// called when the board is digested
public long Digest(Random r)
{	long v = color.ordinal();
	for(EuphoriaCell c = allCells; c!=null; c=c.next) { v ^= c.Digest(); }
	v ^= knowledge*r.nextLong();
	v ^= morale*r.nextLong();
	v ^= totalWorkers*r.nextLong();
	v ^= r.nextLong()*pf.members();
	v ^= r.nextLong()*(mandatoryEquality?2:1);
	v ^= alternateArtifacts.Digest();
	v ^= r.nextLong()*energyGainedThisTurn;
	v ^= r.nextLong()*commodityKnowledge;
	v ^= r.nextLong()*bearsGained;
	v ^= r.nextLong()*lostToAltruism;	// somelosttoaltruism is not included
	v ^= EuphoriaChip.Digest(r,usedAlternateArtifact);
	v ^= r.nextLong()*tf.members();
	
	v ^= r.nextLong()*samuelTheZapperLevel;
    v ^= r.nextLong()*terriAuthorityHeight;
    v ^= r.nextLong()*taedAuthorityHeight;
	return(v);
}

public void sameBoard(EPlayer other)
{	for(EuphoriaCell c = allCells,d=other.allCells; c!=null; c=c.next,d=d.next) 
		{
		b.Assert(c.sameCell(d),"mismatch player cells %s and %s",c,d);
		}
	b.Assert(pf.equals(other.pf),"permanant flags mismatch");
	b.Assert(knowledge==other.knowledge,"knowledge mismatch");
	b.Assert(morale==other.morale,"morale mismatch");
	b.Assert(marketStars==other.marketStars,"morale mismatch");
	b.Assert(totalWorkers==other.totalWorkers,"totalWorkers mismatch");
	b.Assert(alternateArtifacts.sameContents(other.alternateArtifacts),"alternate artifacts mismatch");
	b.Assert(usedAlternateArtifact == other.usedAlternateArtifact,"used alternate artifact mismatch");
	b.Assert(energyGainedThisTurn==other.energyGainedThisTurn,"energy gained mismatch");
	b.Assert(bearsGained == other.bearsGained, "bears gained mismatch");
	b.Assert(lostToAltruism == other.lostToAltruism, "lostToAltruism mismatch");	// somelosttoaltruism is not included
	b.Assert(commodityKnowledge==other.commodityKnowledge,"commodity dif mismatch");
	b.Assert(tf.equals(other.tf), "per turn flags mismatch");
	b.Assert(terriAuthorityHeight==other.terriAuthorityHeight,"terriAuthorityHeight mismatch");
	b.Assert(taedAuthorityHeight==other.taedAuthorityHeight,"taedAuthorityHeight mismatch");
	b.Assert(samuelTheZapperLevel==other.samuelTheZapperLevel,"samuelTheZapperLevel mismatch");

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
	pf.copy(other.pf);	// copy all the permanent flags
    mandatoryEquality = other.mandatoryEquality;
    samuelTheZapperLevel = other.samuelTheZapperLevel;
    terriAuthorityHeight = other.terriAuthorityHeight;
    taedAuthorityHeight = other.taedAuthorityHeight;
  
    originalHiddenRecruit = other.originalHiddenRecruit;
    originalActiveRecruit = other.originalActiveRecruit;
    bearsGained = other.bearsGained;
    lostToAltruism = other.lostToAltruism;
    energyGainedThisTurn = other.energyGainedThisTurn;
    commodityKnowledge = other.commodityKnowledge;
    alternateArtifacts.copyFrom(other.alternateArtifacts);
    usedAlternateArtifact = other.usedAlternateArtifact;
    
    tf.copy(other.tf);	// copy per turn flags
    

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
public int workersInHand() { return(workers.height()); }
public int workersInHandOrAir() 
{	boolean air =(b.pickedObject!=null) && (b.pickedObject instanceof WorkerChip);
	return(workers.height()+(air ? 1 : 0)); 
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
	if((c==b.doublesElgible) && (b.revision>=123))
	{	// bug let you continue even if the doubles worker was killed by a morale check
		if(b.doublesCount--<=1)
			{ b.doublesElgible = null; 
			  b.usingDoubles = false;
			}
	}
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
			{ gainMorale(replay); 
			  b.logGameEvent(MoraleFromAminaTheBlissBringer,color.name());
			  b.useRecruit(RecruitChip.AminaTheBlissBringer,"Gain Morale");
			}
		if((know>=limit)&&(know<17))
		{	//b.p1("saved by amina");
			b.logGameEvent(SavedByAminaTheBlissBringer);
			b.useRecruit(RecruitChip.AminaTheBlissBringer,"Saved");
		}
		limit = 17;

	}
	if(know>=limit && !testTFlag(TFlag.HasLostWorker))
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
	setTFlag(TFlag.HasLostWorker); // can only lose 1 worker per turn
	
	// remove the smartest worker
	int nworkers = workers.height();
	for(int lim=nworkers; lim>=0; lim--)
	{
		WorkerChip c = (WorkerChip)workers.chipAtIndex(lim);
		if((worker==null)||(c.knowledge()>worker.knowledge())) { worker = c; workerIndex = lim; }
	}
	if(worker==null) { b.p1("No worker"); }
	b.logGameEvent(LoseAWorker,""+lostWorkerKnowledge,worker.shortName(),color.name()); 
	if(replay!=replayMode.Replay)
	{
		b.animateSacrificeWorker(workers,worker);
	}
	removeWorker(worker);

	if(workerIndex==nworkers)
	{	clearTFlag(TFlag.UsedChaseTheMiner);			// we lost the new guy
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
	default: throw b.Error("Not expecting to lose %s cards",ct);
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
	if(workers.containsChip(ch)) { setTFlag(TFlag.ResultsInDoubles); }
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
	else { throw b.Error("Ran out of things"); }
	}
}
boolean loseMorale(replayMode replay)	// retrieving workers without paying.  return true if we also have to lose a card
{
	decrementMorale(replay);
	return(moraleCheck(replay));
}

boolean gainMorale(replayMode replay)
{
	return (incrementMorale(replay)==null);
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
			return(Cost.Morale_Brian);
		case Resourcex3:		// mostly_resourcex3 is just resourcex3 except to brian
			return(Cost.Morale_Resourcex3_Brian);
		case Bliss_Commodity:	// mostly bliss_commodity is just bliss_commodity except to brian
										// but he can still pay food
			return(Cost.Morale_BlissOrFoodPlus1_Brian);
		case Artifactx3:		// mostly_artifactx3 is just artifactx3 except to brian
			return(Cost.Morale_Artifactx3_Brian);
		default: break;

		}
	}
	return(alternateCostForBrianTheViticulturist(cost));

}
private Cost alternateCostForBrianTheViticulturist(Cost cost)
{	//b.p1("brian alternate 2");
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


private Cost alternateCostForJeffersonTheShockArtist(Cost cost)
{
	 switch(cost)
	 {
	 case BlissOrFoodRetrieval: return(Cost.EnergyOrBlissOrFoodRetrieval);
	 
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


// set up alternate artifact wildcards
private void setupAlternateArtifacts()
{
	alternateArtifacts.reInit();

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


/**
 * get the actual cost to be used, considering recruit capabilities
 * 
 * @param cost
 * @return
 */

Cost alternateCostWithRecruits(EuphoriaCell dest,Cost cost0,boolean placed)
{	Cost cost = cost0;
	if(cost==Cost.MarketCost) { return cost; }	// open market, can't place there
	if(b.isIIB())
	{
	if(dest!=null)
		{ 
		EuphoriaId loc = dest.rackLocation();
		switch(loc)
		{
		default: break;
			
		case WorkerActivationA:
		case WorkerActivationB:
			if(recruitAppliesToMe(RecruitChip.GaryTheForgetter)	// #226
					&& canPayX(Cost.Blissx3)
					)
			{
			
			switch(cost)
			{
			case Waterx3:	
				b.useRecruit(RecruitChip.GaryTheForgetter,"can use water"+(water.height()>=3));
				cost = Cost.Waterx3OrBlissx3; 
				break;
			case Energyx3:	
				b.useRecruit(RecruitChip.GaryTheForgetter,"can use energy "+(energy.height()>=3));
				cost = Cost.Energyx3OrBlissx3; 
				break;
			default: b.Error("not expecting cost "+cost); break;
				
			}}
			if(recruitAppliesToMe(RecruitChip.KofiTheHermit))
			{
				cost = Cost.Infinite;	// not available
			}
			break;

		case IcariteWindSalon:
		case WastelanderUseMarket:
		case EuphorianUseMarket:
		case SubterranUseMarket:
			if(recruitAppliesToMe(RecruitChip.MosiThePatron)	// #230
					&& canPayX(Cost.ArtifactAndBlissx2))
			{
			b.useRecruit(RecruitChip.MosiThePatron,"can use "+nTypesOfArtifact());
			switch(cost)
			{
			default: throw b.Error("Not expecting costs %s"+cost);
			case Artifactx3:
				cost = Cost.Artifactx3OrArtifactAndBlissx2;
			}
			}
			break;
			
		case WastelanderTunnelMouth:
		case EuphorianTunnelMouth:
		case SubterranTunnelMouth:
			
			{
			
	    	Cost original = cost;
			if ((dest.height()==(placed?1:0))
				&& (recruitAppliesToMe(RecruitChip.DavaaTheShredder)))	// use tunnels for free if no one is there
				{
				b.useRecruit(RecruitChip.DavaaTheShredder,"can use");
				cost = Cost.Free;
				}
			if (recruitAppliesToMe(RecruitChip.TedTheContingencyPlanner)
					  && (canPayX(Cost.Bliss)))
					{
					b.useRecruit(RecruitChip.TedTheContingencyPlanner,"can use");
					switch (cost)
					{
					case Free: cost = Cost.BlissOrFree; break;
					case Energy: cost = Cost.BlissOrEnergy; break;
					case Food: cost = Cost.BlissOrFoodExactly; break;
					case Water: cost = Cost.BlissOrWater; break;
					default: b.Error("not expecting cost "+cost);
					}
					}
			
	    	if(recruitAppliesToMe(RecruitChip.MwicheTheFlusher)	// # 263
	    			&& (canPayX(Cost.Waterx3)))	// can pay water, not dependent on special placement
	    	{	// pay 3 water, but also get a benefit
    			b.useRecruit(RecruitChip.MwicheTheFlusher,cost.name());
    			switch(cost)
	    			{	
	    			case Free:
	    				//b.p1("use mwiche forfree");
	    				switch(original)
	    				{
	    				case Energy: cost = Cost.FreeOrEnergyMwicheTheFlusher; break;
	    				case Food:	cost = Cost.FreeOrFoodMwicheTheFlusher; break;
	    				case Water: cost = Cost.FreeOrWaterMwicheTheFlusher; break;
	    				default: b.Error("not expecting cost "+original);
	    				}
	    				break;
	    			case BlissOrFree: cost = Cost.BlissOrFreeMwicheTheFlusher; break;
	    			case BlissOrEnergy: cost = Cost.BlissOrEnergyMwicheTheFlusher; break;
	    			case BlissOrFoodExactly: cost = Cost.BlissOrFoodMwicheTheFlusher; break;
	    			case BlissOrWater:	cost = Cost.BlissOrWaterMwicheTheFlusher; break;
	    			case Energy: cost = Cost.EnergyMwicheTheFlusher; break;
	    			case Food: cost = Cost.FoodMwicheTheFlusher; break;
	    			case Water: cost = Cost.WaterMwicheTheFlusher; break;
	    			default: b.Error("not expecting cost "+cost);
	    		}
	    	}
			}
			break;
			
		case WastelanderBuildMarketA:
		case WastelanderBuildMarketB:
		case EuphorianBuildMarketA:
		case EuphorianBuildMarketB:
		case SubterranBuildMarketA:
		case SubterranBuildMarketB:

			if(recruitAppliesToMe(RecruitChip.JonTheAmateurHandyman) // #233
				&& canPayX(Cost.CommodityX3))
			{	b.useRecruit(RecruitChip.JonTheAmateurHandyman,"can use "+nKindsOfCommodity());
				b.logGameExplanation(CanUseJonTheAmateurHandyman);
				switch(cost)
				{
				case Gold:	cost = Cost.GoldOrCommodityX3; break;
				case Stone: cost = Cost.StoneOrCommodityX3; break;
				case Clay: cost = Cost.ClayOrCommodityX3; break;
				default: b.Error("Not expecting cost "+cost);
				}
			}
			if((workersInHandOrAir()>(placed ? 0 : 1))
					&& recruitAppliesToMe(RecruitChip.DougTheBuilder))	// #234
			{	b.useRecruit(RecruitChip.DougTheBuilder,"can use");
				switch(cost)
				{
				case Gold:	
					//b.p1("use doug the builder");
					cost = Cost.SacrificeOrGold; break;
				case Stone: cost = Cost.SacrificeOrStone; break;
				case Clay: cost = Cost.SacrificeOrClay; break;
				case GoldOrCommodityX3: 
					//b.p1("use doug and jon "+totalCommodities()+" "+totalResources());
					cost = Cost.SacrificeOrGoldOrCommodityX3; break;
				case StoneOrCommodityX3: cost = Cost.SacrificeOrStoneOrCommodityX3; break;
				case ClayOrCommodityX3: cost = Cost.SacrificeOrClayOrCommodityX3; break;
				default: b.Error("Not expecting cost "+cost); 
				}
			}
			break;
		case ArtifactBazaar:
			if(penaltyAppliesToMe(MarketChip.IIB_DepartmentOfBribeRegulation))
			{
				switch(cost)
				{
				case Free: break;
				case Commodity:	cost = Cost.CommodityX2; break;
				case CommodityX2: cost = Cost.CommodityX3; break;
				default: b.Error("not expecting "+cost);
				}
			}

			if(recruitAppliesToMe(RecruitChip.AhmedTheArtifactDealer))	// #222
			{// TODO: when AhmedTheArtifactDealer is in effect, modify the cost displays
			 b.useRecruit(RecruitChip.AhmedTheArtifactDealer,"can use");
			 switch(cost)
			 {
			 case Free:	break;
			 case Commodity:
				 cost = Cost.Free; 
				 b.logGameEvent(UseAhmed,"Free");
				 break;
			 case CommodityX2: 
				 cost = Cost.Commodity; 
				 b.logGameEvent(UseAhmed,"Commodity");
				 break;
			 case CommodityX3: 
				 cost = Cost.CommodityX2; 
			 	 break;
			 default: b.Error("Not expecting cost %s",cost);
			 }	
			}
			break;
		}
		
    	
    	if ((dest.height()>(placed ? 1 : 0))
    			 && (dest.chipAtIndex(0).color==color)
    			 && penaltyAppliesToMe(MarketChip.IIB_AgencyOfProgressiveBackstabbing))
    	{	// this is going to cause problems because it interacts with all the other costs
    		// that can be complicated by recruits.  Many cases are handled here, but we'll
    		// definitily need to invent a mechanism to separate the commodity-and into 
    		// a separate step.
    		switch(cost)
    			{
    			case Energy:	cost = Cost.EnergyAndCommodity; break;
    			case Food:	cost = Cost.FoodAndCommodity; break;
    			case Water: cost = Cost.WaterAndCommodity; break;
    			case Waterx3: cost = Cost.WaterX3AndCommodity; break;
    			case Energyx3: cost = Cost.EnergyX3AndCommodity; break;
    			case Artifactx3: cost = Cost.ArtifactX3AndCommodity; break;
    			case IsEuphorian: cost = Cost.IsEuphorianAndCommodity; break;
    			case IsWastelander: cost = Cost.IsWastelanderAndCommodity; break;
    			case IsSubterran: cost = Cost.IsSubterranAndCommodity; break;
    			
    			case Bliss_Commodity:	// mostly_ is the same in IIB
    					cost = Cost.BlissAndNonBlissAndCommodity;
    				break;
     			case Resourcex3:	cost = Cost.ResourceX3AndCommodity;
    				break;
    			case CommodityX2: cost = Cost.CommodityX3; break;
    			case Balloon_Stone: cost = Cost.Balloon_StoneAndCommodity; break;
    			case Box_Food_Bliss: cost = Cost.Box_Food_BlissAndCommodity; break;
     			case Balloon_Energy_Bliss: cost = Cost.Balloon_Energy_BlissAndCommodity; break;
    			case Bifocals_Water_Bliss: cost = Cost.Bifocals_Water_BlissAndCommodity; break;
    			case Book_Energy_Water: cost = Cost.Book_Energy_WaterAndCommodity; break;
    			case Bear_Energy_Food: cost = Cost.Bear_Energy_FoodAndCommodity; break;
    			case Bifocals_Gold: cost = Cost.Bifocals_GoldAndCommodity; break;
    			case Bear_Gold: cost = Cost.Bear_GoldAndCommodity; break;
    			case Book_Brick: cost = Cost.Book_BrickAndCommodity; break;
    			case Box_Gold: cost = Cost.Box_GoldAndCommodity; break;
    			case Book_Card: cost = Cost.Book_CardAndCommodity; break;
    			case Box_Brick: cost = Cost.Box_BrickAndCommodity; break;
    			case Bat_Stone: cost = Cost.Bat_StoneAndCommodity; break;
    			case Book_Stone: cost = Cost.Book_StoneAndCommodity; break;
    			case Bifocals_Brick: cost = Cost.Bifocals_BrickAndCommodity; break;
    			case Bat_Brick: cost = Cost.Bat_BrickAndCommodity; break;
    			case EnergyMwicheTheFlusher: cost = Cost.EnergyMwicheTheFlusherAndCommodity; break;
    			case FoodMwicheTheFlusher: cost = Cost.FoodMwicheTheFlusherAndCommodity; break;
    			case WaterMwicheTheFlusher: cost = Cost.WaterMwicheTheFlusherAndCommodity; break;
    			case Energyx3OrBlissx3: cost = Cost.Energyx3OrBlissx3AndCommodity; break;
    			case Waterx3OrBlissx3: cost = Cost.Waterx3OrBlissx3AndCommodity; break;
    			case BlissOrEnergy: cost= Cost.BlissOrEnergyAndCommodity; break;
    			case BlissOrWater: cost= Cost.BlissOrWaterAndCommodity; break;
    			case BlissOrFoodExactly: cost = Cost.BlissOrFoodAndCommodity; break;
    			case Artifactx3OrArtifactAndBlissx2:
    				cost = Cost.Artifactx3OrArtifactAndBlissx2AndCommodity; break;
      			case BlissOrFoodMwicheTheFlusher: 
    				cost= Cost.BlissOrFoodMwicheTheFlusherAndCommodity; break;
      			case BlissOrWaterMwicheTheFlusher: 
    				cost= Cost.BlissOrWaterMwicheTheFlusherAndCommodity; break;
      			case BlissOrEnergyMwicheTheFlusher: 
    				cost= Cost.BlissOrEnergyMwicheTheFlusherAndCommodity; break;
      			case FreeOrWaterMwicheTheFlusher:
      				cost = Cost.FreeOrWaterMwicheTheFlusherAndCommodity; break;
      			case FreeOrFoodMwicheTheFlusher:
      				cost = Cost.FreeOrFoodMwicheTheFlusherAndCommodity; break;
      			case FreeOrEnergyMwicheTheFlusher:
      				cost = Cost.FreeOrEnergyMwicheTheFlusherAndCommodity; break;
      			case BlissOrFreeMwicheTheFlusher:
      				cost = Cost.BlissOrFreeMwicheTheFlusherAndCommodity; break;
     			default: 
    				b.Error("Not expecting %s",cost);
    			}
    	}
		
		}
	
	
	}
	else	// before IIB
	{
	if(dest!=null)
	{	Cost jacko = null;
		switch(dest.rackLocation())
		{	default: break;
		
			case WastelanderBuildMarketA:
			case WastelanderBuildMarketB:
			case EuphorianBuildMarketA:
			case EuphorianBuildMarketB:
			case SubterranBuildMarketA:
			case SubterranBuildMarketB:
				if(recruitAppliesToMe(RecruitChip.MichaelTheEngineer))
					{	// use any resource, special bonus for gold
					switch(cost)
						{
						case Clay:
						case Stone:
						case Gold: cost = Cost.ResourceAndKnowledgeAndMorale;
							break;
						default: b.Error("Not expecting "+cost);
						}
					}
				
				if(recruitAppliesToMe(RecruitChip.FlartnerTheLuddite))
				{
					switch(cost)
					{
					case ResourceAndKnowledgeAndMorale: 
						if(b.revision>=118) { return(Cost.ResourceAndKnowledgeAndMoraleOrArtifact); }
						break;
					case Stone:  cost = Cost.StoneOrArtifact; break;
					case Gold:  cost = Cost.GoldOrArtifact; break;
					case Clay: cost = Cost.ClayOrArtifact; break;
					default: b.Error("Not expecting cost "+cost);
					}
				}
				
				
				break;
			
			case EuphorianTunnelMouth:
				jacko = Cost.EnergyOrKnowledge;
				break;
			case WastelanderTunnelMouth:
				jacko = Cost.FoodOrKnowledge;
				break;
			case SubterranTunnelMouth:
				jacko = Cost.WaterOrKnowledge;
				break;
			case WastelanderUseMarket:
				if( ((b.revision>117) || canPayX(Cost.Knowledgex2))
						&& recruitAppliesToMe(RecruitChip.JackoTheArchivist_V2))
				{	// may pay 1 card instead of 3. everyone else gets a card
					switch(cost)
					{
					case Artifactx3: cost = Cost.ArtifactJackoTheArchivist_V2;	break;
					default: b.Error("Not expecting cost "+cost);
					}
					
				}
			}
		if((jacko!=null)
			&& (dest.height()>(placed ? 1 : 0))
			&& ((b.revision<108) ||(dest.chipAtIndex(0).color!=color))		
			&& recruitAppliesToMe(RecruitChip.MatthewTheThief))
		{
			cost = jacko;
		}
					
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
	if(recruitAppliesToMe(RecruitChip.JeffersonTheShockArtist))
		{
			cost = alternateCostForJeffersonTheShockArtist(cost);
		}
	if(recruitAppliesToMe(RecruitChip.KadanTheInfiltrator))
		{
			cost = alternateCostForKadanTheInfiltrator(cost);
		}
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
boolean canPay(EuphoriaCell dest)
{
	Cost item = alternateCostWithRecruits(dest,dest.placementCost,false);
	return canPayX(item);
}


/**
 * canPay considers the full cost-chain, including alternates such as food for bliss 
 * or bat for any artifact.
 * 
 * @param item0
 * @return
 */
boolean canPay(Cost item0)
{
	Cost alt = alternateCostWithRecruits(null,item0,true);
	//if(alt!=item0) { b.p1("Cost "+item0+" could be "+alt); }
	return canPayX(alt);
}
/**
 * canPayX considers an exact cost, already digested for alternates.  Normally it should
 * be used only for non-fungible items such as knowledge or morale.
 * 
 * @param item
 * @return
 */
boolean canPayX(Cost item)
{
	switch(item)
	{
	case FreeOrWaterMwicheTheFlusherAndCommodity:
	case FreeOrFoodMwicheTheFlusherAndCommodity:
	case FreeOrEnergyMwicheTheFlusherAndCommodity:

	
	default: throw b.Error("Unexpected payment test for %s",item);
	
	case BlissOrFreeMwicheTheFlusher:
	case BlissOrFree: 
		return true;
	
	case Infinite: return(false);	// Kofi the hermit
	case Artifactx3OrArtifactAndBlissx2AndCommodity:
		return (canPayX(Cost.ArtifactX3AndCommodity)
				|| canPayX(Cost.ArtifactAndBlissx2AndCommodity));
			
	case Artifactx3OrArtifactAndBlissx2: 
		return (canPayX(Cost.Artifactx3) || canPayX(Cost.ArtifactAndBlissx2));
	case ArtifactAndBlissx2AndCommodity:
		if(totalCommodities()<3) { return(false); }
		//$FALL-THROUGH$
	case ArtifactAndBlissx2:
		return artifacts.height()>0 && bliss.height()>=2;
		
	case Energyx3OrBlissx3: return((bliss.height()>=3) || (energy.height()>=3));
	case Waterx3OrBlissx3: return((bliss.height()>=3) || (water.height()>=3));

	case Waterx3OrBlissx3AndCommodity: return (((bliss.height()>=3) || (water.height()>=3)) && (totalCommodities()>=4));
	case Energyx3OrBlissx3AndCommodity: return (((bliss.height()>=3) || (energy.height()>=3)) && (totalCommodities()>=4));

	case SacrificeAvailableWorker: return workersInHandOrAir()>1;

	case SacrificeOrGoldOrCommodityX3:	if(totalCommodities()>=3) { return(true); }
	//$FALL-THROUGH$
	case SacrificeOrGold:	return ((workersInHandOrAir()>1) || (gold.height()>0));
	
	case SacrificeOrStoneOrCommodityX3:	if(totalCommodities()>=3) { return(true); }
	//$FALL-THROUGH$
	case SacrificeOrStone:	return ((workersInHandOrAir()>1) || (stone.height()>0));
	
	case SacrificeOrClayOrCommodityX3:	if(totalCommodities()>=3) { return(true); }
		//$FALL-THROUGH$
	case SacrificeOrClay:	return ((workersInHandOrAir()>1) || (clay.height()>0));
	
	// agency of progressive backstabbing adds a commodity to bumpable spaces
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
	case CommodityX3: return(totalCommodities()>=3);
	case Commodity:	return (totalCommodities()>=1);
	case Bliss:	return(bliss.height()>=1);
	case Waterx4:	return(water.height()>=4);
	case Energyx4: return(energy.height()>=4);
	case Foodx4:	return(food.height()>=4);
	case Blissx4:	return(bliss.height()>=4);
	case Blissx2: return(bliss.height()>=2);
	case Blissx3: return(bliss.height()>=3);
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
		
		
	case SacrificeRetrievedWorker:
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
		return(gold.height()>=1);
		
	case StoneOrArtifact:
		if(artifacts.height()>=1) { return(true); }
		// or fall into regular gold cost	
		//$FALL-THROUGH$
	case Stone:
		return(stone.height()>=1);
		
	case ClayOrArtifact:
		if(artifacts.height()>=1) { return(true); }
		// or fall into regular clay
		//$FALL-THROUGH$
	case Clay:		// building markets
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
	case Morale_Artifactx3_Brian:
		{
		boolean canUse2 = (hasArtifactPair()!=null) && !penaltyAppliesToMe(MarketChip.CourthouseOfHastyJudgement);
		int doubles = (b.revision>=123 && b.usingDoubles && !testTFlag(TFlag.HasLostMorale)) ? 1 : 0;
		int needed = doubles+(canUse2 ? 3 : 4);
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
		
	case Morale_Resourcex3_Brian:
		{
		int limit = ((b.revision>=123) && b.usingDoubles && !testTFlag(TFlag.HasLostMorale)) ? 2 : 1;
		if(morale<=limit) { return(false); }
		}
		//$FALL-THROUGH$
	case Resourcex3:	// nimbus loft
		return((stone.height()+clay.height()+gold.height())>=3);
		
	case Energyx3:	// worker training
		// you can pay for the other effects even if you have 4 workers
		return(energy.height()>=3);
	case Waterx3:	// worker training
		// you can pay for the other effects even if you have 4 workers
		return(water.height()>=3);
	case EnergyOrBlissOrFoodRetrieval:
		if(energy.height()>0) { return(true); }
		// or fall into regular blissorfood
		//$FALL-THROUGH$
	case BlissOrFoodRetrieval:
	case BlissOrFoodExactly:
		// no special effect for brian the viticulturist
		
		return((bliss.height()>0) || (food.height()>0));
		
	case BlissOrFoodAndCommodity:
		return ( ((bliss.height()>0) || (food.height()>0))
				&& totalCommodities()>=2);
	case BlissOrEnergy:
		return((bliss.height()>0) || (energy.height()>0));
	case BlissOrEnergyAndCommodity:
		return (((bliss.height()>0) || (energy.height()>0))
				&& totalCommodities()>=2);
	case BlissOrWater:
		return((bliss.height()>0) || (water.height()>0));
	case BlissOrWaterAndCommodity:
		return (((bliss.height()>0) || (water.height()>0))
				&& (totalCommodities()>=2));
	
	// prices for opened markets
	case Morale_BlissOrFoodPlus1_Brian:
		int limit = ((b.revision>=123) && b.usingDoubles && !testTFlag(TFlag.HasLostMorale)) ? 2 : 1;
		if(morale<=limit)  { return(false); }
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
	case Morale_Brian:
		{
		int doubles = (b.revision>=123 && b.usingDoubles && !testTFlag(TFlag.HasLostMorale)) ? 3 : 2;
		return morale>=doubles;
		}
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

	case Box_Food_BlissAndCommodity:	// lottery of dimishing returns, pay all 3 plus a commodity
		if(totalCommodities()<3) { return(false);}
		//$FALL-THROUGH$
	case Box_Food_Bliss:
		return (hasArtifactOrAlternate(ArtifactChip.Box)
				&& (food.height()>0)
				&& (bliss.height()>0));
		
	case Balloon_Energy_BlissAndCommodity:	// institute of orwellian optimism, pay all 3 plus a commodity
		if(totalCommodities()<3) { return(false);}
		//$FALL-THROUGH$
	case Balloon_Energy_Bliss:
		return (hasArtifactOrAlternate(ArtifactChip.Balloons)
				&& (energy.height()>0)
				&& (bliss.height()>0));
		
	case Bifocals_Water_BlissAndCommodity:		// natural floridated spring, pay all 3 plus a commodity
		if(totalCommodities()<3) { return(false);}
		//$FALL-THROUGH$
	case Bifocals_Water_Bliss:
		return (hasArtifactOrAlternate(ArtifactChip.Bifocals)
				&& (water.height()>0)
				&& (bliss.height()>0));

	case Book_Energy_WaterAndCommodity:	// field of agoraphobia, pay all 3 plus a commodity
		if(totalCommodities()<3) { return(false);}
		//$FALL-THROUGH$
	case Book_Energy_Water:
		return (hasArtifactOrAlternate(ArtifactChip.Book)
				&& (energy.height()>0)
				&& (water.height()>0));
		
	case Bear_Energy_FoodAndCommodity:	// dilemmas prison, pay all three plus a commodity
		if(totalCommodities()<3) { return(false);}
		//$FALL-THROUGH$
	case Bear_Energy_Food:
		return (hasArtifactOrAlternate(ArtifactChip.Bear)
				&& (energy.height()>0)
				&& (food.height()>0));
		
	case Bifocals_GoldAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Bifocals_Gold:
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
	case Bifocals_BrickAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Bifocals_Brick:
		return (hasArtifactOrAlternate(ArtifactChip.Bifocals)
				&& (clay.height()>0));

	case Bat_BrickAndCommodity:
		if(totalCommodities()==0) { return(false);}
		//$FALL-THROUGH$
	case Bat_Brick:
		return (hasArtifactOrAlternate(ArtifactChip.Bat)
				&& (clay.height()>0));

	case FreeOrFoodMwicheTheFlusher:
	case FreeOrWaterMwicheTheFlusher:
	case FreeOrEnergyMwicheTheFlusher:
		return true;

	case BlissOrEnergyMwicheTheFlusherAndCommodity:
		{
		int tot = totalCommodities();
		int toteb = energy.height()+bliss.height();
		if(toteb==0) { return((tot>=4) && (water.height()>=3)); }	// have to use water
		return (tot>=2);
		}

	case EnergyMwicheTheFlusherAndCommodity:
		{
		int tot = totalCommodities();
		int tote = energy.height();
		if(tote==0) { return((tot>=4) && (water.height()>=3)); }	// have to use water
		return (tot>=2);
		}
		
	case BlissOrEnergyMwicheTheFlusher:
		if(bliss.height()>0) { return(true); }
		//$FALL-THROUGH$
	case EnergyMwicheTheFlusher:
		return ((energy.height()>=1) || (water.height()>=3));
	case MwicheTheFlusher:
		return (water.height()>=3);
	case MwicheTheFlusherAndCommodity:
		return ((water.height()>=3) && (totalCommodities()>=4));
		
	case FoodMwicheTheFlusherAndCommodity:
		{
		int tot = totalCommodities();
		int totfood = food.height();
		if(totfood==0) { return((tot>=4) && (water.height()>=3)); }	// have to use water
		return (tot>=2);
		}
		

	case BlissOrFoodMwicheTheFlusherAndCommodity:
		return ( ((bliss.height()>=1) && (totalCommodities()>=2))
				 || ((water.height()>=3) && (totalCommodities()>=4)));
		
	case BlissOrFoodMwicheTheFlusher:
		if (bliss.height()>=1) { return(true); }
		//$FALL-THROUGH$
	case FoodMwicheTheFlusher:
		return ((food.height()>=1) || (water.height()>=3));
		
	case WaterMwicheTheFlusherAndCommodity:
		{
		int tot = totalCommodities();
		int wat = water.height();
		return ((wat>=1) && (tot>=2));
		}
	case BlissOrWaterMwicheTheFlusherAndCommodity:
		return (((bliss.height()>=1)||(water.height()>=1)) 	// has a bliss or a water and 2 commodities total
					&& (totalCommodities()>=2));
		
	case BlissOrWaterMwicheTheFlusher:
		if(bliss.height()>=1) { return(true); }
		//$FALL-THROUGH$
	case WaterMwicheTheFlusher:
		return (water.height()>=1);

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
	else { throw b.Error("ran out of resources");}
}
private void shedOneCommodity(replayMode replay)
{
	if(water.height()>0) 
		{  b.addWater(water.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnWater(water); }}
	else if(food.height()>0) { b.addFood(food.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnFood(food); }}
	else if(energy.height()>0) {  b.addEnergy(energy.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnEnergy(energy); }}
	else if(bliss.height()>0) {  b.addBliss(bliss.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnBliss(bliss); }}
	else { throw b.Error("Ran out of commodities"); }
}
private void shedOneNonBliss(replayMode replay)
{
	if(water.height()>0) {  b.addWater(water.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnWater(water); }}
	else if(food.height()>0) { b.addFood(food.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnFood(food); }}
	else if(energy.height()>0) {  b.addEnergy(energy.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnEnergy(energy); }}
	else { throw b.Error("Ran out of non-bliss commodities"); }
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
	incrementMorale(replay);
	b.logGameEvent(FlartnerTheLudditeEffect);
	return payCost(Cost.Artifact,replay);
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
	if(b.isIIB()||(b.revision>=123)) { if(ch==b.doublesElgible)
	{ if(b.doublesCount--<=1) 
		{ b.doublesElgible = null;
		  b.usingDoubles = false;
		}} }
	if(replay!=replayMode.Replay) { b.animateSacrificeWorker(c,(WorkerChip)ch);}
}
private void sendStone(int n,replayMode replay)
{	if(stone.height()>=n)
	{
	for(int i=0;i<n;i++) 
		{ b.addStone(stone.removeTop()); if(replay!=replayMode.Replay)
		{ b.animateReturnStone(stone); }}
	return;
	}
	b.Error("Not enough stone");
}
private void sendGold(int n,replayMode replay)
{	if(gold.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addGold(gold.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnGold(gold); }}
	return;
	}
	b.Error("Not enough gold");
}
private void sendClay(int n,replayMode replay)
{	if(clay.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addClay(clay.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnClay(clay); }}
	return;
	}
	b.Error("Not enough clay");
}

private void sendEnergy(int n,replayMode replay)
{	if(energy.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addEnergy(energy.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnEnergy(energy); }}
	return;
	}
	b.p1("not enough energy");
	b.Error("Not enough energy");
}
private void sendFood(int n,replayMode replay)
{	if(food.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addFood(food.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnFood(food); }}
	return;
	}
	b.Error("Not enough food");
}

private void sendWater(int n,replayMode replay)
{	if(water.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addWater(water.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnWater(water); }}
	return;
	}
	b.Error("Not enough water");
}

private void sendBliss(int n,replayMode replay)
{	if(bliss.height()>=n)
	{
	for(int i=0;i<n;i++) { b.addBliss(bliss.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnBliss(bliss); }}
	return;
	}
	b.Error("Not enough Bliss");	
}
private Cost sendArtifacts(int n,replayMode replay)
{	if(artifacts.height()>=n)
	{
	for(int i=0;i<n;i++) { b.recycleArtifact(artifacts.removeTop()); if(replay!=replayMode.Replay) { b.animateReturnArtifact(artifacts); }}
	return(null);
	}
	throw b.Error("should succeed");
}

//note, before rev 123 this version had a bug that didn't have an immediately
//visible effect - it failed torecycle the card.  To maintain compatibility,
//we have to maintain the bug!
private Cost artifactChipBuggy(ArtifactChip which,Cost cost,replayMode replay)
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
	boolean match = false;
	for(int n = b.droppedDestStack.size()-1; n>=0; n--)
	{
		EuphoriaCell c =b.droppedDestStack.elementAt(n);
		if(c==b.usedArtifacts)
		{	found++;
			if(ch==null) { match=true; }
			else {
			ArtifactChip top = (ArtifactChip)c.chipAtIndex(c.height()-found);
			if(top==ch) { match = true;; }
			else if (alternateArtifacts.containsChip(top))
				{	alt = top;
				    match = true;
				}}
		}
	}
	if(alt!=null)
	{	// b.p1("paid with alternate "+alt.id+" for "+(ch==null ? "any" : ch.id));
		setUsedAlternate(alt); 
    }
	b.Assert(match,"didn't pay %s",ch==null ? "artifact" : ch.id.name());
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
		b.Error("used artifact twice");
	}
	usedAlternateArtifact = alt;
	if(alt==ArtifactChip.Book) { b.useRecruit(RecruitChip.JavierTheUndergroundLibrarian,"used"); }
	else if(alt==ArtifactChip.Balloons) { b.useRecruit(RecruitChip.AlexandraTheHeister,"used"); }
	else if(alt==ArtifactChip.Bear) { b.useRecruit(RecruitChip.MiroslavTheConArtist,"used"); }
	else if(alt==ArtifactChip.Box) { b.useRecruit(RecruitChip.EkaterinaTheCheater,"used"); }
	else if(alt==ArtifactChip.Bat) { b.useRecruit(RecruitChip.JoseThePersuader,"used"); }
	else if(alt==ArtifactChip.Bifocals) { b.useRecruit(RecruitChip.MakatoTheForger,"used"); }
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
/**
 * pay a cost for an item that is supposed to be in hand and unambiguous
 * 
 * @param item
 * @param replay
 */
void payCostOrElse(Cost item,replayMode replay)
{	Cost residual = payCost(item,replay);
	if(residual!=null)
	{
	b.p1("mandatory payment "+item+" failed with "+residual);
	b.Error("Payment %s failed",item);
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
		return(payCost(Cost.Artifactx3Only,replay));
	}
	
	if(allOneArtifactType()
		&& (hasArtifactPair()!=null))
		{	if(payCost(Cost.ArtifactPair,replay)!=null)
				{b.p1("artifactx3 didn't succeed2");
				 b.Error("must succeed");
				}
			return(andCommodity ? Cost.Commodity : null);
		}
	if((artifacts.height()==3)
			&& (countArtifactPairs()==0)) 
		{ 
		b.Assert(payCost(Cost.Artifactx3Only,replay)==null,"must succeed"); 
		setTriggerPedroTheCollector(true);
		return(andCommodity ? Cost.Commodity : null);
		}
	return(andCommodity ? Cost.ArtifactX3AndCommodity : Cost.Artifactx3);
}

private Cost sendCommodity(int n,Cost cost,replayMode replay)
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
   
 * @param dest
 * @param replay
 * @return the residual amount or null
 */
// 
Cost payCost(EuphoriaCell dest,replayMode replay)
{	// originalcost of preserved so SacrificeAvailableWorker can eventually
	// know what bonus to pay 
	Cost original = originalCost = dest.placementCost;
	return payCost(alternateCostWithRecruits(dest,original,true),replay);
}
Cost originalCost = null;

/**
 * Pay as much of an exact cost as possible, and return a cost for the residual amount, or null
 * if the entire cost was paid.  Also queue animation of the costs being paid.
 * @param item
 * @param replay
 * @return the resuidual unpaid cost, or null
 */
Cost payCost(Cost item,replayMode replay)
{	int commoditiesNeeded = 0;
	switch(item)
	{
	case Closed:
	case DisplayOnly:
	case MarketCost:
	case TunnelOpen:

	case BlissOrFreeMwicheTheFlusher:
		if(bliss.height()==0) { return payCost(Cost.FreeMwicheTheFlusher,replay); }
		if(water.height()<3) { return payCost(Cost.BlissOrFree,replay); }
		return item;
		
	case BlissMwicheTheFlusher:
		if(bliss.height()==0) { sendWater(3,replay); return null; }
		if(water.height()<3) { sendBliss(1,replay); return(null); }
		return item;
		
	case FreeMwicheTheFlusher:
		if(water.height()<3) { return null; }	// have to be free
		return item;
		
	case BlissMwicheTheFlusherAndCommodity:
		if(bliss.height()==0) { return payCost(Cost.MwicheTheFlusherAndCommodity,replay); }
		if(water.height()<3) { return payCost(Cost.Bliss_Commodity,replay); }
		//b.p1("pay "+item);
		return item;
	case BlissOrFoodMwicheTheFlusherAndCommodity:
		if(bliss.height()==0) { return payCost(Cost.FoodMwicheTheFlusherAndCommodity,replay); }
		if(food.height()==0) { return payCost(Cost.BlissMwicheTheFlusherAndCommodity,replay); }
		if(water.height()<3) { return payCost(Cost.BlissOrFoodAndCommodity,replay); }
		//b.p1("pay "+item);
		return item;
	case BlissOrWaterMwicheTheFlusherAndCommodity:
		if(bliss.height()==0) { return payCost(Cost.WaterMwicheTheFlusherAndCommodity,replay); }
		if(water.height()==0) { return payCost(Cost.Bliss_Commodity,replay); }
		//b.p1("pay "+item);
		return item;
	case BlissOrEnergyMwicheTheFlusherAndCommodity:
		if(bliss.height()==0) { return payCost(Cost.EnergyMwicheTheFlusherAndCommodity,replay); }
		if(energy.height()==0) { return payCost(Cost.BlissMwicheTheFlusherAndCommodity,replay); }
		//b.p1("pay "+item);
		return item;
	case FreeOrWaterMwicheTheFlusherAndCommodity:
		if(water.height()==0) { return null; }	// have to take free
		b.p1("pay "+item);
		return item;
	case FreeOrMwicheTheFlusherAndCommodity:
		if(water.height()<3) { return null; }	// have to take free
		b.p1("pay "+item);
		return item;
	case FreeOrFoodMwicheTheFlusherAndCommodity:
		if(food.height()==0) { return payCost(Cost.FreeOrMwicheTheFlusherAndCommodity,replay); }	
		b.p1("pay "+item);
		return item;
	case FreeOrEnergyMwicheTheFlusherAndCommodity:
		if(energy.height()==0) { return payCost(Cost.FreeOrMwicheTheFlusherAndCommodity,replay); }
		b.p1("pay "+item);
		return item;
	default: throw b.Error("Unexpected payment for %s",item);
	
	case BlissOrFree:
		if(bliss.height()==0) { return null; }	// have to accept free
		return item;	// we always have to choose
		
	case SacrificeOrCommodityX3:
		if(totalCommodities()<3) { return payCost(Cost.SacrificeAvailableWorker,replay); }
		if(!hasWorkersInHand()) { return payCost(Cost.CommodityX3,replay); }
		return item;
		
	case SacrificeOrGoldOrCommodityX3:
		if(totalCommodities()>=3)
		{	// could pay the commodities
			if(gold.height()==0) { return payCost(Cost.SacrificeOrCommodityX3,replay); }
			if(!hasWorkersInHand() ) { return(payCost(Cost.StoneOrCommodityX3,replay)); }
			return(item);
		}	
		//$FALL-THROUGH$
	case SacrificeOrGold:
		if(gold.height()==0) { return( payCost(Cost.SacrificeAvailableWorker,replay)); }
		if(hasWorkersInHand()) 
			{ return(item); 
			}
		sendGold(1,replay);
		return null;
		
	case SacrificeOrClayOrCommodityX3:
		if(totalCommodities()>=3)
		{	// could pay the commodities
			if(clay.height()==0) { return payCost(Cost.SacrificeOrCommodityX3,replay); }
			if(!hasWorkersInHand() ) { return(payCost(Cost.StoneOrCommodityX3,replay)); }
			return(item);
		}	
		//$FALL-THROUGH$
	case SacrificeOrClay:
		if(clay.height()==0) {return( payCost(Cost.SacrificeAvailableWorker,replay)); }
		if(hasWorkersInHand()) { return(item); }
		sendClay(1,replay);
		return null;
	
	case SacrificeOrStoneOrCommodityX3:
		if(totalCommodities()>=3)
		{	// could pay the commodities
			if(stone.height()==0) { return payCost(Cost.SacrificeOrCommodityX3,replay); }
			if(!hasWorkersInHand() ) { return(payCost(Cost.StoneOrCommodityX3,replay)); }
			return(item);
		}	
		//$FALL-THROUGH$
	case SacrificeOrStone:
		if(stone.height()==0) { return( payCost(Cost.SacrificeAvailableWorker,replay)); }
		if(hasWorkersInHand()) { return(item); }
		sendStone(1,replay);
		return null;
		
	case SacrificeAvailableWorker:
		//b.p1("auto sacrifice worker");
		if(workersInHand()==1) 
			{
			  sacrificeWorker(workers,replay);
			  finishDougTheBuilder(originalCost,replay);
			  return null;
			}
		return item;
		
	case NonBlissAndCommodity:
		if(nKindsOfCommodityExceptBliss()==1)
		{
			shedOneNonBliss(replay);
			return payCost(Cost.Commodity,replay);
		}
		return item;
	
	case Energyx3OrBlissx3AndCommodity:
		if(energy.height()<3) { sendBliss(3,replay); return(payCost(Cost.Commodity,replay)); }
		if(bliss.height()<3) { sendEnergy(3,replay); return(payCost(Cost.Commodity,replay)); }
		return item;
	case Waterx3OrBlissx3AndCommodity:
		if(water.height()<3) { sendBliss(3,replay); return(payCost(Cost.Commodity,replay)); }
		if(bliss.height()<3) { sendWater(3,replay); return(payCost(Cost.Commodity,replay)); }
		return item;
	
	case BlissOrFoodAndCommodity:
		if(food.height()==0) { sendBliss(1,replay); return(payCost(Cost.Commodity,replay)); }
		if(bliss.height()==0) { sendFood(1,replay); return(payCost(Cost.Commodity,replay)); }
		if(totalCommodities()==2) { sendFood(1,replay); sendBliss(1,replay); return(null); }
		return(item);
		
	case BlissOrEnergyAndCommodity:
		if(energy.height()==0) { sendBliss(1,replay); return(payCost(Cost.Commodity,replay)); }
		if(bliss.height()==0) { sendEnergy(1,replay); return(payCost(Cost.Commodity,replay)); }
		if(totalCommodities()==2) { sendBliss(1,replay); sendEnergy(1,replay); return(null); }
		return(item);
		
	case BlissOrWaterAndCommodity:
		if(water.height()==0) { sendBliss(1,replay); return(payCost(Cost.Commodity,replay)); }
		if(bliss.height()==0) { sendWater(1,replay); return(payCost(Cost.Commodity,replay)); }
		if(totalCommodities()==2) { sendWater(1,replay); sendBliss(1,replay); return(null); }
		return(item);
		
	case ArtifactAndBlissx2:
		sendBliss(2,replay);
		return(payCost(Cost.Artifact,replay));
		
	case ArtifactAndBlissx2AndCommodity:
		{	
		sendBliss(2,replay);
		Cost residual = payCost(Cost.Artifact,replay);
		if(nKindsOfCommodity()==1) 
			{ sendCommodity(1,Cost.Commodity,replay); 
			  return residual;
		    }
		if(residual==null) { return(Cost.Commodity);}
		return item;
		}
		
	case Artifactx3OrArtifactAndBlissx2AndCommodity:
		{
		boolean canpay3 = canPayX(Cost.ArtifactX3AndCommodity);
		boolean canpayb = canPayX(Cost.ArtifactAndBlissx2AndCommodity);
		if(canpay3 && canpayb) { return(item); }
		return canpay3
				? payCost(Cost.ArtifactX3AndCommodity,replay) 
				: payCost(Cost.ArtifactAndBlissx2AndCommodity,replay);
		}
		
	case Artifactx3OrArtifactAndBlissx2:
		if(artifacts.height()==1) 		// can't pay with multiple cards
				{  sendBliss(2,replay); 
				   sendArtifact(0,replay); 
				   //b.p1("use mosi the patron");
				   return(null); 
				}
		if(bliss.height()<2)
		{	// cant pay with bliss
			return(payCost(Cost.Artifactx3,replay));
		}
		//$FALL-THROUGH$
		if(!canPayX(Cost.Artifactx3)) { sendBliss(2,replay); return(payCost(Cost.Artifact,replay)); }
				
		//b.p1("can use mosi the patron");
		return(item);

	case Waterx3OrBlissx3:
		if(bliss.height()<3) { return(payCost(Cost.Waterx3,replay)); }
		if(water.height()<3) { return(payCost(Cost.Blissx3,replay)); }
		return item;
		
	case Energyx3OrBlissx3:
		if(bliss.height()<3) { return(payCost(Cost.Energyx3,replay)); }
		if(energy.height()<3) { return(payCost(Cost.Blissx3,replay)); }
		return item;
		
	case StoneOrCommodityX3:
		if(stone.height()==0) { return(payCost(Cost.CommodityX3,replay)); }
		if(totalCommodities()<3) { return(payCost(Cost.Stone,replay)); }
		return(item);
		
	case GoldOrCommodityX3:
		if(gold.height()==0) { return(payCost(Cost.CommodityX3,replay)); }
		if(totalCommodities()<3) { return(payCost(Cost.Gold,replay)); }
		return(item);
		
	case ClayOrCommodityX3:
		if(clay.height()==0) { return(payCost(Cost.CommodityX3,replay)); }
		if(totalCommodities()<3) { return(payCost(Cost.Clay,replay)); }
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
		else { return(payCommodity ? Cost.Resourcex3 : item); }
		
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
		return(payCost(Cost.NonBlissAndCommodity,replay));

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
				incrementMorale(replay);
				b.logGameEvent(MichaelTheEngineerGold,getPlayerColor(),getPlayerColor());
				}
			else { b.logGameEvent(MichaelTheEngineerAny); }
			}
			return(null);
		}
		return(item);
		
	// MatthewTheThief
	case WaterOrKnowledge:
		if(!b.isIIB() && (water.height()==0)) 
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
		if(!b.isIIB() && (energy.height()==0)) 
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
		if(!b.isIIB() && (food.height()==0))
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
	
	
	case SacrificeRetrievedWorker:
		// pete the cannibal and sheppard the lobotomist sacrice a worker before
		// re-rolling, it doesn't matter who gets hit.  There's
		// a no potential interaction with recruits that let you select a roll
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
		sendGold(1,replay);
		return(null);
		
	case StoneOrArtifact:
		if(stone.height()==0) { return(doFlartner(replay)); }
		if(artifacts.height()>0) { return(Cost.StoneOrArtifact); }
		// or fall into regular gold	
		//$FALL-THROUGH$
	case Stone:
		sendStone(1,replay);
		return null;
		
	case ClayOrArtifact:
		if(clay.height()==0) { return(doFlartner(replay)); }
		if(artifacts.height()>0) { return(Cost.ClayOrArtifact); }
		// or fall into regular clay	
		//$FALL-THROUGH$
	case Clay:		// building markets
		sendClay(1,replay);
		return(null);
	case ClayX2:
		sendClay(2,replay);
		return null;
	case ClayX3:
		sendClay(3,replay);
		return null;
	case ClayX4:
		sendClay(4,replay);
		return null;
	case ClayX5:
		sendClay(5,replay);
		return null;
	case ClayX6:
		sendClay(6,replay);
		return null;
	case ClayX7:
		sendClay(7,replay);
		return null;
	case ClayX8:
		sendClay(8,replay);
		return null;
	case ClayX9:
		sendClay(9,replay);
		return null;
		
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
			setTFlag(TFlag.UsedJackoTheActivist);
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
		
	case Morale_Artifactx3_Brian:
		// this is special logic for BrianTheViticulturist
		if(morale<=artifacts.height()) 
			{ return(Cost.Morale_Artifactx3_Brian); } 	// have to interact
		b.Assert(morale>=2,"morale>=2");
		decrementMorale(replay);	
		if(b.revision<123)
		{
			// keep the old bugs for replay
			Cost residual = payCostArtifactx3(false,replay);
			if(residual!=null)
			{
				return Cost.Morale_Artifactx3_Brian;
			}
			return null;
		}
		//$FALL-THROUGH$
	case Artifactx3:
		return payCostArtifactx3(false,replay);
	case ArtifactX3AndCommodity:
		return payCostArtifactx3(true,replay);
		
	case Morale_Resourcex3_Brian:
		b.Assert(morale>=2,"morale ge 2");
		decrementMorale(replay);
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
	case Card_BlissOrFood:
		{
		Cost residualFood = payCost(Cost.BlissOrFoodExactly,replay);
		Cost residualCard = payCost(Cost.Artifact,replay);
		if(residualFood==null) { return(residualCard); }
		if(residualCard==null) { return(residualFood); }
		b.Assert((residualFood==Cost.BlissOrFoodExactly) && (residualCard==Cost.Artifact),
				"not expecting residuals %s %s",residualFood,residualCard);
		return item;
		}
	case NonBlissCommodity:
		if(nKindsOfCommodityExceptBliss()==1)
			{
			shedOneNonBliss(replay);
			return(null);
			}
		return Cost.NonBlissCommodity;
	case Morale_BlissOrFoodPlus1_Brian:
		b.Assert(morale>=2,"morale ge 2");
		decrementMorale(replay);
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
				return payCost(Cost.NonBlissCommodity,replay);
				}

			return Cost.BlissOrFoodPlus1;
			}
		}	// end of have food
		//$FALL-THROUGH$
	case Bliss_Commodity:	// breeze bar and sky lounge
		sendBliss(1,replay);
		if(b.revision>=123)
			{
			return payCost(Cost.NonBlissCommodity,replay);
			}		
		if(((b.revision>=101) 						// bug fix 7/6/2014 number of kinds of commodity, not the number of commodity
				? nKindsOfCommodityExceptBliss() 
				: totalCommoditiesExceptBliss())==1) 	// only have 1
			{
			shedOneCommodity(replay);
			return(null);
			}
		return Cost.NonBlissCommodity;

		
	case EnergyOrBlissOrFoodRetrieval:
		{ // pay with energy instead of food or bliss
		if(energy.height()>0)
			{
			if((bliss.height()==0) && (food.height()==0)) 
			{ sendEnergy(1,replay);
			  return null;
			}
			return(item);		// force interaction
			}
		}
		// or fall into blissorfood
		//$FALL-THROUGH$
	case BlissOrFoodRetrieval:		// pay for retrieval
	case BlissOrFoodExactly:
		
		if(bliss.height()==0) { return(payCost(Cost.Food,replay)); } 
		else if(food.height()==0) { return(payCost(Cost.Bliss,replay)); }
		return(item);		// force interaction
		
	case BlissOrEnergy:		// pay for retrieval
		
		if(bliss.height()==0) {  return(payCost(Cost.Energy,replay)); }
		if(energy.height()==0) { return(payCost(Cost.Bliss,replay)); }
		return Cost.BlissOrEnergy;
		
	case BlissOrWater:		// pay for retrieval
		
		if(bliss.height()==0) {  return(payCost(Cost.Water,replay)); }
		if(energy.height()==0) { return(payCost(Cost.Bliss,replay)); }
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
		if(clay.height()==0) { return payCost(Cost.BlissOrFoodExactly,replay); }
		if(bliss.height()==0) { return payCost(Cost.ClayOrFood,replay); }
		if(food.height()==0) { return payCost(Cost.ClayOrBliss,replay); }
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
		return payCost(Cost.ClayOrBliss,replay); 

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
		if(food.height()==0) { return(payCost(Cost.GoldOrBliss,replay)); }
		if(gold.height()==0) { return(payCost(Cost.BlissOrFoodExactly,replay)); }
		if(bliss.height()==0) { return(payCost(Cost.GoldOrFood,replay)); }
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
		return(payCost(Cost.GoldOrBliss,replay));

	case Foodx4_Gold:
		sendGold(1,replay);
		sendFood(4,replay);
		return null;

	case Energyx4_ClayOrBlissOrFood:
		sendEnergy(4,replay);
		return payCost(Cost.ClayOrBlissOrFood,replay);

	case Energyx4_ClayOrBliss:
		sendEnergy(4,replay);
		return payCost(Cost.ClayOrBliss,replay);

	case Energyx4_Clay:
		sendClay(1,replay);
		sendEnergy(4,replay);
		return null;

	case Foodx4_StoneOrBlissOrFood:
		sendFood(4,replay);
		//$FALL-THROUGH$
	case StoneOrBlissOrFood:
		if(food.height()==0) { return payCost(Cost.StoneOrBliss,replay); }
		if(bliss.height()==0) { return payCost(Cost.StoneOrFood,replay); }
		if(stone.height()==0) { return payCost(Cost.BlissOrFoodExactly,replay); }
		return Cost.StoneOrBlissOrFood;

	case StoneOrFood:
		if(stone.height()==0) { sendFood(1,replay); return(null); }
		if(food.height()==0) { sendStone(1,replay); return(null); }
		return Cost.StoneOrFood;

		// or fall into stone or bliss
		//$FALL-THROUGH$
	case Foodx4_StoneOrBliss:
		sendFood(4,replay);
		return payCost(Cost.StoneOrBliss,replay);

	case Foodx4_Stone:
		sendStone(1,replay);
		sendFood(4,replay);
		return null;
	case FoodOrResource:
		if(food.height()==0) { return(payCost(Cost.Resource,replay));}
		if(totalResources()==0) { return(payCost(Cost.Food,replay)); }
		return(item);

	case Card_FoodOrResource:
		{
		if(food.height()==0) { return(payCost(Cost.Card_Resource,replay)); }
		if(totalResources()==0) { return(payCost(Cost.Card_Food,replay)); }
		Cost residual = payCost(Cost.Artifact,replay);
		if(residual==null) { return(payCost(Cost.FoodOrResource,replay)); }
		return(item);
		}		
	case ResourceOrBlissOrFood:
		if(totalResources()==0) { return payCost(Cost.BlissOrFoodExactly,replay); }
		if(food.height()==0) { return payCost(Cost.ResourceOrBliss,replay); }
		if(bliss.height()==0) { return payCost(Cost.FoodOrResource,replay); }
		return(item);
		
	case Card_ResourceOrBlissOrFood:
		if(b.revision>=123)
			{
			Cost residualCard = payCost(Cost.Artifact,replay);
			Cost residualBliss = payCost(Cost.ResourceOrBlissOrFood,replay);
			if(residualCard==null) {  return residualBliss; }
			if(residualBliss==null) { return residualCard; }
			switch(residualBliss)
			{
			case ResourceOrBlissOrFood: return item;
			case BlissOrFoodExactly: return Cost.Card_BlissOrFood;
			case ResourceOrBliss: return Cost.Card_ResourceOrBliss;
			case FoodOrResource: return Cost.Card_FoodOrResource;
			default: throw b.Error("not expecting residual %s",residualBliss);
			}
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
		if(b.revision>=123)
		{
			Cost residualCard = payCost(Cost.Artifact,replay); 
			Cost residualBliss = payCost(Cost.ResourceOrBliss,replay);
			if(residualCard==null) { return residualBliss; }
			if(residualBliss==null) { return residualCard; }
			b.Assert((residualBliss==Cost.ResourceOrBliss)&&(residualCard==Cost.Artifact),
						"not expecing residual %s",residualBliss);
			return item;
		}
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
			if(nKindsOfResource()==1) { shedOneResource(replay); return payCost(Cost.Card,replay); }
			if(allOneArtifactType()) { sendArtifacts(1,replay); return payCost(Cost.Resource,replay); }
			return Cost.Card_Resource;
		}
	
		if(!allOneArtifactType()) { return(Cost.Card_Resource); }
		if(nKindsOfResource()>1) { return(Cost.Card_Resource); }
		sendArtifacts(1,replay);
		shedOneResource(replay);
		return(null);
		
	case Waterx4_Card:
		sendWater(4,replay); 
		return payCost(Cost.Artifact,replay);

	case Energyx4_Card:
		{
		Cost residual = payCost(Cost.Artifact,replay);
		if(residual!=null) 
			{ if(b.revision>=115) { sendEnergy(4,replay); } 
			  return residual; 
			}
		sendEnergy(4,replay);  //preserve and old bug for replay games
		return null;
		}
	case Foodx4_Card:
		sendFood(4,replay);
		return payCost(Cost.Artifact,replay);
	
	case BlissOrFoodx4_Resource:

		if(b.revision>=123)
		{
		if(nKindsOfResource()==1) { shedOneResource(replay); return payCost(Cost.BlissOrFoodx4,replay); }
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
		return payCost(Cost.Resource,replay);

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
		// brian the viticulturist
		if(b.revision>=123)
		{	// tested 3/31
			Cost residualFood = payCost(Cost.BlissOrFoodx4,replay);
			Cost residualCard = payCost(Cost.Artifact,replay);
			if(residualFood==null) { return(residualCard); }
			if(residualCard==null) { return(residualFood); }
			b.Assert((residualCard==Cost.Artifact)
					 && (residualFood==Cost.BlissOrFoodx4), "not expexcting residuals for BlissOrFoodx4_Card");
			return item;
		}
		if(!allOneArtifactType())
			{ 
			if(b.revision<123)	return(Cost.BlissOrFoodx4_Card); 
			Cost residual = payCost(Cost.BlissOrFoodx4,replay);
			if(residual==null) { return(Cost.Artifact); }
			switch(residual)
			{
			case BlissOrFoodx4: return( item); 
			default: throw b.Error("not expecting residual %s",residual);
			}
			}// interact
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
		
	case BlissOrFoodx4:
		{
		// 4 with some mix of bliss and food
		int blissH =  bliss.height();
		int foodH = food.height();
		// b.p1("bliss and foodx4 "+foodH+" "+blissH); // tested 3/31
		if(blissH==0) { sendFood(4,replay); return null; }
		if(foodH==0) {  sendBliss(4,replay); return null;  }
		if(blissH+foodH==4) { sendFood(foodH,replay); sendBliss(blissH,replay);  return null; }
		return Cost.BlissOrFoodx4;
		}
		// fall into standard blissx4_card
	case Blissx4_Card:
		sendBliss(4,replay);
		return payCost(Cost.Artifact,replay);
		
	case Commodity_Bear:
		// note this ineffecient construction is to preserve
		// a bug that lost discards before rev 123
		if(nKindsOfCommodity()==1)
		{	shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Bear,Cost.Bear,replay);
		}
		return 	payCost(Cost.Bear,replay)==null ? Cost.Commodity : Cost.Commodity_Bear;
		
		
	case Bifocals_Water_BlissAndCommodity:
		sendWater(1,replay);		
		sendBliss(1,replay);	// prepay bliss
		//$FALL-THROUGH$
	case Commodity_Bifocals:
		// note this ineffecient construction is to preserve
		// a bug that lost discards before rev 123
		if(nKindsOfCommodity()==1)
		{	shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Bifocals,Cost.Bifocals,replay);
		}
		return payCost(Cost.Bifocals,replay)==null ? Cost.Commodity : Cost.Commodity_Bifocals;

	case Commodity_Balloons:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Balloons,Cost.Balloons,replay);
		}
		return (payCost(Cost.Balloons,replay)==null) ? Cost.Commodity : Cost.Commodity_Balloons;

	case Commodity_Box:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Box,Cost.Box,replay);
		}
		return payCost(Cost.Box,replay)==null ? Cost.Commodity : Cost.Commodity_Box;

	case Commodity_Bat:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Bat,Cost.Bat,replay);
		}
		return payCost(Cost.Bat,replay)==null ? Cost.Commodity : Cost.Commodity_Bat;

	case Commodity_Book:
		if(nKindsOfCommodity()==1)
		{
			shedOneCommodity(replay);
			return uniqueArtifactChip(ArtifactChip.Book,Cost.Book,replay);
		}
		return payCost(Cost.Book,replay)==null ? Cost.Commodity : Cost.Commodity_Book;


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
	case Morale_Brian:
	case Morale:
		b.Assert(morale>=2,"morale>=2");
		decrementMorale(replay);
		return(null);
	case Moralex2:
		b.Assert(morale>=3,"morale>=3");
		decrementMorale(replay);
		decrementMorale(replay);
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

	case Box_Food_BlissAndCommodity:
		{
		Cost residual = payCost(Cost.Box_Food_Bliss,replay);
		if(residual==null) { return(payCost(Cost.Commodity,replay)); }
		Cost residual_commod = payCost(Cost.Commodity,replay);
		if(residual_commod==null) { return(residual); }
		switch(residual)
		{
		case Box:	return Cost.Commodity_Box;
		default: throw b.Error("Not expecting residual %s",residual);
		}
		}
		
	case Box_Food_Bliss:
		sendBliss(1,replay);
		sendFood(1,replay);
		return uniqueArtifactChip(ArtifactChip.Box,Cost.Box,replay);
		
	case Balloon_Energy_BlissAndCommodity:
		sendEnergy(1,replay);
		sendBliss(1,replay);
		return payCost(Cost.Commodity_Balloons,replay);

	case Balloon_Energy_Bliss:
		sendEnergy(1,replay);
		sendBliss(1,replay);
		return uniqueArtifactChip(ArtifactChip.Balloons,Cost.Balloons,replay);

		
	case Bifocals_Water_Bliss:
		sendBliss(1,replay);
		sendWater(1,replay);
		return uniqueArtifactChip(ArtifactChip.Bifocals,Cost.Bifocals,replay);

	case Book_Energy_WaterAndCommodity:
		{
		sendWater(1,replay);
		sendEnergy(1,replay);
		Cost residualBook = payCost(Cost.Book,replay);
		Cost residualCommodity = payCost(Cost.Commodity,replay);
		if(residualBook==null) { return residualCommodity; }
		if(residualCommodity==null) { return residualBook; }
		b.Assert((residualBook==Cost.Book) && (residualCommodity==Cost.Commodity),
				"unexpected residuals %s %s",residualBook,residualCommodity);
		return Cost.Commodity_Book;
		}
	case Book_Energy_Water:
		sendWater(1,replay);
		sendEnergy(1,replay);
		return uniqueArtifactChip(ArtifactChip.Book,Cost.Book,replay);

	case Bear_Energy_FoodAndCommodity:
		sendEnergy(1,replay);
		sendFood(1,replay);
		return payCost(Cost.Commodity_Bear,replay);

	case Bear_Energy_Food:
		sendEnergy(1,replay);
		sendFood(1,replay);
		return uniqueArtifactChip(ArtifactChip.Bear,Cost.Bear,replay);		
		
	case Bifocals_GoldAndCommodity:
		sendGold(1,replay);
		return payCost(Cost.Commodity_Bifocals,replay);
		
	case Bifocals_Gold:
		sendGold(1,replay);
		return uniqueArtifactChip(ArtifactChip.Bifocals,Cost.Bifocals,replay);

	case Bear_GoldAndCommodity:
		sendGold(1,replay);		// prepay gold
		return payCost(Cost.Commodity_Bear,replay);

	case Bear_Gold:
		sendGold(1,replay);
		return uniqueArtifactChip(ArtifactChip.Bear,Cost.Bear,replay);
			
	case Book_BrickAndCommodity:
		sendClay(1,replay);
		return payCost(Cost.Commodity_Book,replay);
		
	case Book_Brick:
		sendClay(1,replay);
		return uniqueArtifactChip(ArtifactChip.Book,Cost.Book,replay);
		
	case Box_GoldAndCommodity:
		sendGold(1,replay);
		return payCost(Cost.Commodity_Box,replay);
		
	case Box_Gold:
		sendGold(1,replay);
		return uniqueArtifactChip(ArtifactChip.Box,Cost.Box,replay);	

	case Book_CardAndCommodity:
		{	// palace of forced altruism + agency of progressive backstabbing
		b.p1("book card and commodity");
		Cost residual_commodity = payCost(Cost.Commodity,replay);
		Cost residual_book = payCost(Cost.Book_Card,replay);
		if(residual_book==null) { return(residual_commodity); }
		if(residual_commodity==null) { return(residual_book); }
		b.Assert(residual_commodity==Cost.Commodity,"residual should be Cost.Commodity");
		switch(residual_book)
		{
		case Artifact:	return Cost.Commodity_Artifact; 
		case Book_Card: return item;
		default: throw b.Error("Expecting residual %s for Cost.Book_Card");
		}

		}
	case Commodity_Artifact:
		{
		if(nKindsOfCommodity()==1)
			{ sendCommodity(1,Cost.Commodity,replay); 
			  return(payCost(Cost.Artifact,replay));
			}
		if(payCost(Cost.Artifact,replay) == null) { return(Cost.Commodity); }
		return item;
		}
	case Book_Card:	// prepay the book
		return(artifact1XPlus1(ArtifactChip.Book,Cost.Book_Card, replay));

	case Box_BrickAndCommodity:
		sendClay(1,replay);
		return payCost(Cost.Commodity_Box,replay);
		
	case Box_Brick:
		sendClay(1,replay);
		return uniqueArtifactChip(ArtifactChip.Box,Cost.Box,replay);
		
	case Bat_StoneAndCommodity:
		sendStone(1,replay);		// prepay bat and stone
		return payCost(Cost.Commodity_Bat,replay);

	case Bat_Stone:
		sendStone(1,replay);		// prepay bat and stone
		return payCost(Cost.Bat,replay);
		
	case Book_StoneAndCommodity:
		sendStone(1,replay);					// send the stone
		return payCost(Cost.Commodity_Book,replay);
	
	case Book_Stone:
		sendStone(1,replay);
		return uniqueArtifactChip(ArtifactChip.Book,Cost.Book,replay);
		
	case Bifocals_BrickAndCommodity:
		sendClay(1,replay);							// prepay clay
		return payCost(Cost.Commodity_Bifocals,replay);

	case Bifocals_Brick:
		sendClay(1,replay);
		return uniqueArtifactChip(ArtifactChip.Bifocals,Cost.Bifocals,replay);
		
	case Bat_BrickAndCommodity:
		sendClay(1,replay);						// prepay brick
		return payCost(Cost.Commodity_Bat,replay);

	case Bat_Brick:
		sendClay(1,replay);
		return payCost(Cost.Bat,replay);

	case FreeOrEnergyMwicheTheFlusher:
		if((energy.height()==0) && (water.height()<3)) { return null; } // must take free
		return(item);
	case FreeOrFoodMwicheTheFlusher:
		if((food.height()==0)&&(water.height()<3)) { return null; }	// must take free
		return(item);
	case FreeOrWaterMwicheTheFlusher:
		if(water.height()==0) { return null; }	// must take free
		return(item);
		
	case BlissOrEnergyMwicheTheFlusher:
		if(bliss.height()>0) { return item; }
		//$FALL-THROUGH$
	case EnergyMwicheTheFlusher:
		if(water.height()<3) { return payCost(Cost.Energy,replay); }	// not enough water, send energy
		if(energy.height()==0) { setTFlag(TFlag.UsingMwicheTheFlusher); return payCost(Cost.Waterx3,replay); }	// not enough food, send water
		//b.p1("mwitche option for energy");
		return(Cost.EnergyMwicheTheFlusher);  	// have to interact

	case MwicheTheFlusher:
		sendWater(3,replay);
		return null;
		
	case MwicheTheFlusherAndCommodity:
		sendWater(3,replay);
		return payCost(Cost.Commodity,replay);
		
		//mwiche the flusher
	case BlissOrWaterMwicheTheFlusher:
		if(bliss.height()>0) { return item; }			
		//$FALL-THROUGH$
	case WaterMwicheTheFlusher:
		if(water.height()<3) { sendWater(1,replay); return null; }
		//b.p1("mwitche option for water");
		return(Cost.WaterMwicheTheFlusher);

	case BlissOrFoodMwicheTheFlusher:
		if(bliss.height()>0) { return(item); }
		//$FALL-THROUGH$
	case FoodMwicheTheFlusher:
		if(water.height()<3)  { sendFood(1,replay); return null; }
		if(food.height()==0) { setTFlag(TFlag.UsingMwicheTheFlusher);  sendWater(3,replay); return null;}
		//b.p1("mwitche option for food");
		return(Cost.FoodMwicheTheFlusher);
		
		
	case WaterMwicheTheFlusherAndCommodity:
		{
		// the order of these matters, the "rest" will only fill if the user has only water
		// if he has exactly 3 water, he'll be left with 2 which cant support mwiche. 
		Cost rest = payCost(Cost.Commodity,replay);
		Cost main = payCost(Cost.WaterMwicheTheFlusher,replay);
		if(rest==null) { return(main); }
		if(main==null) { return(rest); }
		return(Cost.WaterMwicheTheFlusherAndCommodity);
		}
		
	case FoodMwicheTheFlusherAndCommodity:
		{
		Cost rest = payCost(Cost.Commodity,replay);
		Cost main = payCost(Cost.FoodMwicheTheFlusher,replay);
		if(rest==null) { return(main); }
		if(main==null) { return(rest); }
		return(Cost.FoodMwicheTheFlusherAndCommodity);
		}
	
	case EnergyMwicheTheFlusherAndCommodity:
		{
		Cost rest = payCost(Cost.Commodity,replay);
		Cost main = payCost(Cost.EnergyMwicheTheFlusher,replay);
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
		{		
		if(recruitAppliesToMe(RecruitChip.ChristineTheAnarchist))
		{	//b.p1("no bonus for allegiance - food");
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
		decrementKnowledge(replay);
	}
	else
	{	// handles joseph the antiquer
		n ++;	// to 2 or 3
		incrementKnowledge(replay);
	}
	n = doMonotony(n);
	if(b.isIIB())
	{	
		Benefit bene = null;
		if(	(n>=2)
			&& recruitAppliesToMe(RecruitChip.JosephTheAntiquer))
		{	b.logGameExplanation(ArtifactInPlaceOf,"Food");
			b.useRecruit(RecruitChip.JosephTheAntiquer,"Food");
			switch(n)
			{	
			case 2:	bene = Benefit.ArtifactOrFoodX2; break;
			case 3: bene = Benefit.ArtifactOrFoodX3; break;
			default: throw b.Error("Not expecting %s",n);
			}	
		}
		if((know>=9) && recruitAppliesToMe(RecruitChip.GwenTheMinerologist))
			{
			b.logGameExplanation(ResourceInPlaceOfCommodity,"Clay","Food");
			b.useRecruit(RecruitChip.GwenTheMinerologist,"food");
			//b.p1("use gwen for food");
			if(bene!=null) 
				{
				switch(bene)
				{
				case ArtifactOrFoodX2:	bene = Benefit.ArtifactOrClayOrFoodX2; break;
				case ArtifactOrFoodX3:	bene = Benefit.ArtifactOrClayOrFoodX3; break;
				default: throw b.Error("Not expecting %s",n);
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
{	if(c.isIIB() != b.isIIB()) { return false;}	// not an appropriate test, so doesn't appluy
	if(testTFlag(TFlag.UsingRowenaTheMentor)) { return(false); }
	EuphoriaCell m = b.getOpenMarketCell(c);
	if(m!=null) 
		{if(m.ignoredForPlayer==color) 
			{	b.logGameEvent(SavedByLionelTheCook);
				//b.p1("Ignored market for "+m.ignoredForPlayer);
				b.useRecruit(RecruitChip.LionelTheCook,"has effect");
				return(false);
			}
		 if(!hasMyAuthorityToken(m)){ return(true); }
		
		}
	return(false);
}
boolean recruitAppliesToMe(RecruitChip c)
{	if(c.isIIB() != b.isIIB()) { return false; }	// from a different variation
	if(mandatoryEquality) { return(false); }
	if(testTFlag(TFlag.UsedGeekTheOracle) && (c==RecruitChip.GeekTheOracle)) { return(false); }	// once per turn
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
			{	//b.p1("no bonus for allegiance - energy");
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
		decrementKnowledge(replay);
	}
	else
	{	// handles joseph the antiquer
		n ++;	// to 2 or 3
		incrementKnowledge(replay);
	}
	n = doMonotony(n);
	if(b.isIIB())
		{
		Benefit bene = null;
		if( (n>=2) 
			&& recruitAppliesToMe(RecruitChip.JosephTheAntiquer))
		{	b.logGameExplanation(ArtifactInPlaceOf,"Energy");
			b.useRecruit(RecruitChip.JosephTheAntiquer,"Energy");
			switch(n)
			{	
			case 2:	bene = Benefit.ArtifactOrEnergyX2; break;
			case 3: bene = Benefit.ArtifactOrEnergyX3; break;
			default: throw b.Error("Not expecting %s",n);
			}
		}
		if((know>=9) && recruitAppliesToMe(RecruitChip.GwenTheMinerologist))
		{
		//b.p1("use gwen for energy");
		b.logGameExplanation(ResourceInPlaceOfCommodity,"Gold","Energy");
		b.useRecruit(RecruitChip.GwenTheMinerologist,"food");
		if(bene!=null) 
			{ 
			
			switch(bene)
			{
			case ArtifactOrEnergyX2:	bene = Benefit.ArtifactOrGoldOrEnergyX2; break;
			case ArtifactOrEnergyX3:	bene = Benefit.ArtifactOrGoldOrEnergyX3; break;
			default: throw b.Error("Not expecting %s",n);
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
			{	//b.p1("no bonus for allegiance - water");
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
		decrementKnowledge(replay);
	}
	else
	{	// handles joseph the antiquer
		n++;	// to 2 or 3
		incrementKnowledge(replay);
	}	
	n = doMonotony(n);
	if(b.isIIB())
	{	Benefit bene = null;
		if( (n>=2) 
			&& recruitAppliesToMe(RecruitChip.JosephTheAntiquer))
		{	b.logGameExplanation(ArtifactInPlaceOf,"Water");
			b.useRecruit(RecruitChip.JosephTheAntiquer,"Water");
			//b.p1("try the antiquer for water ");
			switch(n)
			{	
			case 2:	bene = Benefit.ArtifactOrWaterX2; break;
			case 3: bene = Benefit.ArtifactOrWaterX3; break;
			default: throw b.Error("Not expecting %s",n);
			}
		}
		if((know>=9) && recruitAppliesToMe(RecruitChip.GwenTheMinerologist))
		{
			b.logGameExplanation(ResourceInPlaceOfCommodity,"Stone","Water");
			b.useRecruit(RecruitChip.GwenTheMinerologist,"food");
			//b.p1("use gwen for water");
			if(bene!=null) 
			{
				switch(bene)
				{
				case ArtifactOrWaterX2:	bene = Benefit.ArtifactOrStoneOrWaterX2; break;
				case ArtifactOrWaterX3:	bene = Benefit.ArtifactOrStoneOrWaterX3; break;
				default: throw b.Error("Not expecting %s",n);
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
			{	//b.p1("no bonus for allegiance - bliss");
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
		decrementKnowledge(replay);
	}
	else
	{	n++;		// to two or 3
		boolean knowMore = incrementKnowledge(replay);
		if(knowMore
				&& canPayX(Cost.Knowledge)
				&& recruitAppliesToMe(RecruitChip.BrendaTheKnowledgeBringer))
		{	// option to also lose knowledge and gain artifact
			b.makeBrenda(this);	// proceed normally
		}
	}		
	n = doMonotony(n);
	if(b.isIIB() 
			&& (n>=2)
			&& recruitAppliesToMe(RecruitChip.JosephTheAntiquer))
		{	b.logGameExplanation(ArtifactInPlaceOf,"Bliss");
			b.useRecruit(RecruitChip.JosephTheAntiquer,"bliss");
			switch(n)
			{	
			case 2:	return Benefit.ArtifactOrBlissX2;
			case 3: return Benefit.ArtifactOrBlissX3;
			default: throw b.Error("Not expecting %s",n);
			}
		}
		else
		{
		doBliss(n,b,replay);
		return null;
		}
	}

// authority token and influence for the main territory track
private boolean doAuthorityAndInfluence(EuphoriaBoard b,Allegiance aa,replayMode replay,EuphoriaCell market,boolean influence)
{	
		if((b.isIIB()
				|| (b.revision>=123))
			&& (market!=null) 
			&& (authorityTokensRemaining()>0)
			&& !hasMyAuthorityToken(market))
		{
			EuphoriaCell c = b.getAvailableAuthorityCell(this,aa);
			if(c==null)
			{if(influence) { b.incrementAllegiance(aa,replay); }
			 EuphoriaChip tok = getAuthorityToken(replay);
			 if(tok!=null)
			 {
			 // incrementing allegiance can use up a token, so it's not an error
			 // if there are none left.
			 market.addChip(tok); 
			 if(replay!=replayMode.Replay)
			 	{
				 b.animationStack.push(authority);
				 b.animationStack.push(market);
			 	}
			 }
			 return(true);
			}
		return(false); 
		}
		if(influence) { b.incrementAllegiance(aa,replay); }
		doAuthority(b,aa,replay);
		return true;
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
{		EuphoriaCell c = b.getAvailableAuthorityCell(this,aa);
		if(c!=null)
		{	EuphoriaChip chip = getAuthorityToken(replay);
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
		EuphoriaChip chip = getAuthorityToken(replay);
		if(chip!=null)
		{
		if(recruitAppliesToMe(RecruitChip.GeorgeTheLazyCraftsman)
			&& !market.containsChip(chip))
		{	//b.p1("trigger george the lazy craftsman");
			b.useRecruit(RecruitChip.GeorgeTheLazyCraftsman,"use");
			b.logGameEvent(UseGeorgeTheLazyCraftsman);
			setTFlag(TFlag.TriggerGeorgeTheLazyCraftsman);
		}
		b.placeAuthorityToken(market,chip);
		doTheaterOfRevelatoryPropaganda(b,replay);
		if(replay!=replayMode.Replay) { b.animatePlacedItem(authority,market); }
		}
}
// just influence
private void doInfluence(EuphoriaBoard b,replayMode replay,Allegiance aa)
{
		b.incrementAllegiance(aa,replay);
}

private void finishDougTheBuilder(Cost cost,replayMode replay)
{	//b.p1("Finish doug the builder "+cost);
	b.logGameEvent(UsedDougTheBuilder,cost.name());
	switch(cost)
	{
	case Gold:	doGold(1,b,replay); break;
	case Stone: doStone(1,b,replay); break;
	case Clay: doClay(1,b,replay); break;
	default: b.Error("Not expecting cost %s",cost);
	}
}

// true if we paid a pair, when we want to pay if we can.
// we also don't care if there are more artifacts
boolean hasPaidArtifactPair(CellStack droppedDestStack,EuphoriaCell usedArtifacts)
{	if(droppedDestStack.size()>=2)
	{
	ArtifactChip first = null;
	ArtifactChip second = null;
	ArtifactChip third = null;
	int idx = usedArtifacts.height()-1;
	for(int lim = droppedDestStack.size()-1; lim>=0; lim--)
		{
		if(droppedDestStack.elementAt(lim)==usedArtifacts)
		{	third = second;
			second = first;
			first = (ArtifactChip)usedArtifacts.chipAtIndex(idx);
			idx--;
			if((third!=null) && artifactsMatch(third,first)) 
				{ // we already compared third and second
				  return(true);
				}
			if((second!=null)
				&& artifactsMatch(first,second))
				{
				return(true);
				}
		}
		}}
	return false;
 }
private boolean hasPaid3DifferentArtifacts(CellStack droppedDestStack,EuphoriaCell usedArtifacts)
{	int total = 0;
	if(droppedDestStack.size()>=3)
	{
	ArtifactChip first = null;
	ArtifactChip second = null;
	ArtifactChip third = null;
	int idx = usedArtifacts.height()-1;
	for(int lim = droppedDestStack.size()-1; lim>=0; lim--)
		{
		if(droppedDestStack.elementAt(lim)==usedArtifacts)
		{	ArtifactChip chip = (ArtifactChip)usedArtifacts.chipAtIndex(idx);
			total++;
			if(!alternateArtifacts.containsChip(chip)) 	// it's there but it doesn't match anything
			{
			third = second;
			second = first;
			first = chip;			
			idx--;
			if(third==first) 
				{ // we already compared third and second
				  return(false);
				}
			if(second==first)
				{
				return(false);
				}
		}}
		}}
	return total>=3;
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
	case SacrificeOrCommodityX3:
	case SacrificeOrGoldOrCommodityX3:
	case SacrificeOrStoneOrCommodityX3:
	case SacrificeOrClayOrCommodityX3:
	case SacrificeOrGold:
	case SacrificeOrClay:
	case SacrificeOrStone:
	case SacrificeAvailableWorker:
		// after using doug the builder, give him a resource
		if(dest.top()==b.trash)
		{
		totalWorkers--;
		
		WorkerChip sacrificed = (WorkerChip)b.trash.topChip();
		if(((b.isIIB()||b.revision>=123)) && (sacrificed==b.doublesElgible)) 
			{ if(b.doublesCount-- <=1) 
				{ b.doublesElgible = null;
				  b.usingDoubles = false;
				} 
			}
		unPlaceWorker(b.trash);
		b.animateSacrificeWorker(b.getSource(),sacrificed);
		finishDougTheBuilder(cost,replay);
		}
		break;
		
		

	case MwicheTheFlusher:
	case BlissMwicheTheFlusher:
	case FreeMwicheTheFlusher:
	case BlissOrFreeMwicheTheFlusher:
	case FreeOrEnergyMwicheTheFlusher:
	case FreeOrFoodMwicheTheFlusher:
	case FreeOrWaterMwicheTheFlusher:
	case BlissOrEnergyMwicheTheFlusher:
	case BlissOrFoodMwicheTheFlusher:
	case BlissOrWaterMwicheTheFlusher:
		if(dest.size()==3) { setTFlag(TFlag.UsingMwicheTheFlusher); }
		break;
		
	case MwicheTheFlusherAndCommodity:
	case FreeOrEnergyMwicheTheFlusherAndCommodity:
	case FreeOrFoodMwicheTheFlusherAndCommodity:
	case FreeOrWaterMwicheTheFlusherAndCommodity:
	case BlissOrFreeMwicheTheFlusherAndCommodity:
	case BlissOrFoodMwicheTheFlusherAndCommodity:
	case BlissOrEnergyMwicheTheFlusherAndCommodity:
	case EnergyMwicheTheFlusherAndCommodity:
	case FoodMwicheTheFlusherAndCommodity:
	case WaterMwicheTheFlusherAndCommodity:
	case BlissMwicheTheFlusherAndCommodity:
	case BlissOrWaterMwicheTheFlusherAndCommodity:
		if(dest.size()==4) { setTFlag(TFlag.UsingMwicheTheFlusher); }
		break;
		
	case WaterMwicheTheFlusher:
	case EnergyMwicheTheFlusher:
	case FoodMwicheTheFlusher:
 		 if(dest.size()==3) { setTFlag(TFlag.UsingMwicheTheFlusher); }
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
	case Morale_BlissOrFoodPlus1_Brian:
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
		{	incrementMorale(replay);
			b.logGameEvent(FlartnerTheLudditeEffect,getPlayerColor());
		}
		break;
		
	case ResourceAndKnowledgeAndMoraleOrArtifact:	// either michael the engineer or flartner the luddite
		if(dest.top().topChip().isArtifact())
		{
			b.Assert(recruitAppliesToMe(RecruitChip.FlartnerTheLuddite),"should be flartner");
			incrementMorale(replay);
			b.logGameEvent(FlartnerTheLudditeEffect,getPlayerColor());
			break;
		}
		//$FALL-THROUGH$
	case ResourceAndKnowledgeAndMorale:
		b.Assert(recruitAppliesToMe(RecruitChip.MichaelTheEngineer),"should be michael");
		if(dest.top().topChip()==EuphoriaChip.Gold)
			{
			incrementKnowledge(replay);
			incrementMorale(replay);
			b.logGameEvent(MichaelTheEngineerGold,getPlayerColor(),getPlayerColor());
			}
			else
			{	b.logGameEvent(MichaelTheEngineerAny);
			}
		
		break;
		
	case BalloonsOrCardx2:
		if(b.numberOfArtifactsPaid()>=2) { break;}
		//$FALL-THROUGH$
	case Balloon_Stone:
	case Commodity_Balloons:
	case Balloon_Energy_Bliss:
	case Balloons:
		checkHasPaidArtifact(ArtifactChip.Balloons);
		break;
		
	case BearOrCardx2:
		if(b.numberOfArtifactsPaid()>=2) { break;}
		//$FALL-THROUGH$
	case Commodity_Bear:
	case Bear_Gold:
	case Bear_Energy_Food:
	case Bear:
		checkHasPaidArtifact(ArtifactChip.Bear);
		break;
		
	case BatOrCardx2:
		if(b.numberOfArtifactsPaid()>=2) { break;}
		//$FALL-THROUGH$
	case Commodity_Bat:
	case Bat_Stone:
	case Bat_Brick:
	case Bat:
		checkHasPaidArtifact(ArtifactChip.Bat);
		break;
		
	case BookOrCardx2:
		if(b.numberOfArtifactsPaid()>=2) { break;}
		//$FALL-THROUGH$
	case Commodity_Book:
	case Book_Stone:
	case Book_Brick:
	case Book_Card:
	case Book_Energy_Water:
	case Book:
		checkHasPaidArtifact(ArtifactChip.Book);
		break;
		
	case BifocalsOrCardx2:
		if(b.numberOfArtifactsPaid()>=2) { break;}
		//$FALL-THROUGH$
	case Commodity_Bifocals:
	case Bifocals_Gold:
	case Bifocals_Brick:
	case Bifocals_Water_Bliss:
	case Bifocals:
		checkHasPaidArtifact(ArtifactChip.Bifocals);
		break;
		
	case BoxOrCardx2:
		if(b.numberOfArtifactsPaid()>=2) { break;}
		//$FALL-THROUGH$
	case Commodity_Box:
	case Box_Gold:
	case Box_Brick:
	case Box_Food_Bliss:
	case Box:
		checkHasPaidArtifact(ArtifactChip.Box);
		break;

	default: 
		if(actualCost.name().indexOf("Flusher")>0) 
		{
			b.p1("Probably should not be default "+actualCost);
		}
		break;
	}
	
	// consider the original cost and maybe make some annotation
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

	case Artifactx3:
	case Artifactx2:
	case Morale_Artifactx3_Brian:
		setTriggerPedroTheCollector(hasPaid3DifferentArtifacts(b.droppedDestStack,b.usedArtifacts));
		break;
	default: break;	
	}
}
private void artifactIsDest(CellStack dest)
{	int n = artifacts.height();
	for(int lim=dest.size()-1; lim>=0; lim--)
	{
		EuphoriaCell c = dest.elementAt(lim);
		if(c.rackLocation()==EuphoriaId.PlayerArtifacts) 
			{ setTFlag(TFlag.AddedArtifact); 
			  n--;
			  EuphoriaChip chip = artifacts.chipAtIndex(n);
			  if(chip==ArtifactChip.Balloons) { setTFlag(TFlag.TriggerLarsTheBallooneer); }
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
			{ decrementKnowledge(replay); b.logGameEvent(EsmeTheFiremanKnowledge,getPlayerColor()); }
		else if(ch==EuphoriaChip.getMorale(color)) { incrementMorale(replay); b.logGameEvent(EsmeTheFiremanMorale,getPlayerColor()); }
		else {throw b.Error("not expecting %s",ch); }
		top.removeTop();	// clear the heart or head from the player temp cell
		}
		break;
	case Moralex2OrKnowledgex2:
		{
		EuphoriaCell top = dest.top();
		EuphoriaChip ch = top.topChip();
		if(ch==EuphoriaChip.getKnowledge(color)) 
			{decrementKnowledge(replay);
			 decrementKnowledge(replay);
			 b.logGameEvent(EsmeTheFiremanKnowledgex2,getPlayerColor()); }
		else if(ch==EuphoriaChip.getMorale(color))
			{ incrementMorale(replay); 
			 incrementMorale(replay); 
			 b.logGameEvent(EsmeTheFiremanMoralex2,getPlayerColor()); 
			 }
		else { throw b.Error("not expecting %s",ch); }
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
		break;
	case IcariteInfluenceAndResourcex2:
		b.incrementAllegiance(Allegiance.Icarite,replay);
		if(b.revision<109) { doTheaterOfRevelatoryPropaganda(b,replay); }
		break;
	case WastelanderAuthorityAndInfluenceA:
	case EuphorianAuthorityAndInfluenceA:
	case SubterranAuthorityAndInfluenceA:
	case WastelanderAuthorityAndInfluenceB:
	case EuphorianAuthorityAndInfluenceB:
	case SubterranAuthorityAndInfluenceB:
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
	case FreeArtifactOrResource:
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
	case Resourcex9:
		if(dest!=null) { b.Assert(dest.size()==9,"should be 9 resources"); }		
		break;
	case Resourcex8:
		if(dest!=null) { b.Assert(dest.size()==8,"should be 8 resources"); }		
		break;
	case Resourcex7:
		if(dest!=null) { b.Assert(dest.size()==7,"should be 7 resources"); }		
		break;
	case Resourcex6:
		if(dest!=null) { b.Assert(dest.size()==6,"should be 6 resources"); }		
		break;
	case Resourcex5:
		if(dest!=null) { b.Assert(dest.size()==5,"should be 5 resources"); }		
		break;
	case Resourcex4:
		if(dest!=null) { b.Assert(dest.size()==4,"should be 4 resources"); }		
		break;

	case Resourcex3:
	case Commodityx3:
		if(dest!=null) { b.Assert(dest.size()==3,"should be 3 items"); }		
		break;
	case Commodityx2:
	case Resourcex2:
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
	EuphoriaCell mC = b.getAvailableAuthorityCell(this,a);
	
	// place a star on a market (if open) or on the authority territory
	boolean marketAvailableA = b.marketIsOpen(mA) && !hasMyAuthorityToken(mA);
	boolean marketAvailableB = b.marketIsOpen(mB) && !hasMyAuthorityToken(mB);
	boolean territoryAvailable = mC!=null;
	int sum = (marketAvailableA ? 1 :0) + (marketAvailableB ? 1 : 0) + (territoryAvailable ? 1 : 0);
	
	if(sum<=1)
		{	// no way or 1 way to do it.
		if(marketAvailableA) { doMarketAndInfluence(b,replay,a,mA,influence); }
			else if(marketAvailableB) { doMarketAndInfluence(b,replay,a,mB,influence); }
			else if(territoryAvailable) { doAuthorityAndInfluence(b,a,replay,null,influence); }
			else { doInfluence(b,replay,a); }	// not an error if there is no place to put a star
		return(null);
		}
		return(bene);	// have to decide
}

// limit increases of commodities due to market penalty IIB_LotteryOfDiminishingReturns
int doLottery(int n0,EuphoriaCell c)
{
	int height = c.height();
	if((height+n0>2)
		&& penaltyAppliesToMe(MarketChip.IIB_LotteryOfDiminishingReturns))
		{  
		int n = Math.max(0,Math.min(n0, 2-height));
		//b.p1("limit commodities due to IIB_LotteryOfDiminishingReturns ");
		if(n<n0)
			{
			b.logGameEvent(UseDiminishingReturns,""+(n0-n),c.rackLocation().prettyName);
			}
		return n;
		}
	return(n0);
}
void doTrash(EuphoriaCell from,Benefit type,int n, int n0,replayMode replay)
{
	if(n<n0)
	{	//b.p1("trash "+n+"-"+n0);
		for(int i=n; i<n0; i++) { b.animateTrash(from); } 		
	}
}
void doBliss(int n0,EuphoriaBoard b,replayMode replay)
{	int n = doLottery(n0,bliss);
	for(int i=0;i<n;i++) { addBliss(b.getBliss()); if(replay!=replayMode.Replay) { b.animateNewBliss(bliss); }}
	doTrash(b.bliss,Benefit.Bliss,n,n0,replay);
}
void doFood(int n0,EuphoriaBoard b,replayMode replay)
{	int n = doLottery(n0,food);
	for(int i=0;i<n;i++) { addFood(b.getFood()); if(replay!=replayMode.Replay) { b.animateNewFood(food); }}
	doTrash(b.farm,Benefit.Bliss,n,n0,replay);
}
void doWater(int n0,EuphoriaBoard b,replayMode replay)
{	int n = doLottery(n0,water);
	for(int i=0;i<n;i++) { addWater(b.getWater()); if(replay!=replayMode.Replay) { b.animateNewWater(water); }}
	doTrash(b.aquifer,Benefit.Water,n,n0,replay);
}

Benefit doEnergy(int n0,EuphoriaBoard b,replayMode replay)
{	
	int n = doLottery(n0,energy);
	int egg = energyGainedThisTurn;
	energyGainedThisTurn += n;
	for(int i=0;i<n;i++) { addEnergy(b.getEnergy()); if(replay!=replayMode.Replay) { b.animateNewEnergy(energy); }}
	if(replay!=replayMode.Replay) { for(int i=n; i<n0; i++) { b.animateTrash(bliss); } }
	if((egg<3) 
			&& (energyGainedThisTurn>=3)
			&& !testTFlag(TFlag.UsedFrazerTheMotivator) // only once per turn
			&& recruitAppliesToMe(RecruitChip.FrazerTheMotivator))
	{
		//b.p1("use frazerthemotivator for "+n0);
		b.useRecruit(RecruitChip.FrazerTheMotivator,"use");
		b.logGameEvent(UsedFrazerTheMotivator);
		setTFlag(TFlag.UsedFrazerTheMotivator);
		energy.removeTop();   // take one back
		return Benefit.Commodity;
	}
	return null;
}

private void doClay(int n0,EuphoriaBoard b,replayMode replay)
{	int n = doForcedAltruism(n0,Benefit.Clay.name());
	for(int i=n; i<n0; i++)
	{
	b.trash.addChip(b.getClay());
	if(replay!=replayMode.Replay)
		{ b.animateTrash(bliss); 
		}
	}
	while(n-- > 0)
	{
	addClay(b.getClay());		
	if(replay!=replayMode.Replay) { b.animateNewClay(clay); }
	}

}
private void doGold(int n0,EuphoriaBoard b,replayMode replay)
{	int n = doForcedAltruism(n0,Benefit.Gold.name());

	for(int i=n; i<n0; i++)
	{	b.trash.addChip(b.getGold());
	if(replay!=replayMode.Replay) 
		{  b.animateTrash(bliss); } 
	}	

	while(n-- > 0)
	{
	addGold(b.getGold());		
	if(replay!=replayMode.Replay) { b.animateNewGold(gold); }
	}

}
int doForcedAltruism(int n,String type)
{
	int tot = totalResources();
	int newn = Math.min(Math.max(0,3-tot),3);
	if((newn<n)
			// check the penalty only if it would hurt, so the game log will be the most informative
			&& penaltyAppliesToMe(MarketChip.IIB_PalaceOfForcedAltruism))
		{	//b.p1("lose resources to altruism "+type);
			int lost = (n-newn);
			b.logGameEvent(LoseResourcesFromPalace,""+lost,type);
			lostToAltruism += lost;
			setTFlag(TFlag.SomeLostToAltruism);
			clearTFlag(TFlag.PlayedLostSound);
			return(newn);
		}	
	return n;
}

private void doStone(int n0,EuphoriaBoard b,replayMode replay)
{	
	int n = doForcedAltruism(n0,Benefit.Stone.name());
	for(int i=n; i<n0; i++) 
		{ b.trash.addChip(b.getStone());
		  if(replay!=replayMode.Replay)  { b.animateTrash(bliss);  }
		}
	while(n-- > 0)
	{
	addStone(b.getStone());		
	if(replay!=replayMode.Replay) { b.animateNewStone(stone); }
	}
}
public Benefit doArtifact(int n,EuphoriaBoard b,replayMode replay)
{	// IIB can't just give you an artifact
	if(b.isIIB())
	{	b.Assert(n==1,"only one");
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
	return b.Assert(collectBenefit(benefit,replay)==null,"collection must succeed");
}
private void checkGaryTheForgetter(replayMode replay)
{
	if(recruitAppliesToMe(RecruitChip.GaryTheForgetter))
	{	//b.p1("use gary the forgetter");
		b.logGameEvent("lose 2 Knowledge (Gary the Forgetter)");
		b.useRecruit(RecruitChip.GaryTheForgetter,"gain worker");
		decrementKnowledge(replay);
		decrementKnowledge(replay);
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
	case ResourceX9AndWaterX9:
		doWater(9,b,replay);
		return(Benefit.Resourcex9);
	case ResourceX8AndWaterX8:
		doWater(8,b,replay);
		return(Benefit.Resourcex8);
	case ResourceX7AndWaterX7:
		doWater(7,b,replay);
		return(Benefit.Resourcex7);
	case ResourceX6AndWaterX6:
		doWater(6,b,replay);
		return(Benefit.Resourcex6);
	case ResourceX5AndWaterX5:
		doWater(5,b,replay);
		return(Benefit.Resourcex5);
	case ResourceX4AndWaterX4:
		doWater(4,b,replay);
		return(Benefit.Resourcex4);
	case ResourceX3AndWaterX3:
		doWater(3,b,replay);
		return(Benefit.Resourcex3);
	case ResourceX2AndWaterX2:
		doWater(2,b,replay);
		return(Benefit.Resourcex2);
	case ResourceAndWater:
		doWater(1,b,replay);
		return(Benefit.Resource);
		
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
		if(b.isIIB()) { return(benefit); }	// must interact
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
		return doBlissSelection(b,replay,commodityKnowledge);

		
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
			setTFlag(TFlag.GainedWorker);
		}
		decrementKnowledge(replay);
		decrementKnowledge(replay);	
		checkGaryTheForgetter(replay);
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
		setTFlag(TFlag.GainedWorker);
		}	
		incrementMorale(replay);
		incrementMorale(replay);
		checkGaryTheForgetter(replay);
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
		{	decrementKnowledge(replay); 
			b.logGameEvent(EsmeTheFiremanKnowledge,getPlayerColor());
			return(null); 
		} if(knowledge==1)
			{ incrementMorale(replay);
				b.logGameEvent(EsmeTheFiremanMorale,getPlayerColor());
			  return(null);
			}
		return(benefit);
	case Moralex2OrKnowledgex2:
		if(morale == MAX_MORALE_TRACK)
		{	decrementKnowledge(replay);
			decrementKnowledge(replay);
			b.logGameEvent(EsmeTheFiremanKnowledgex2,getPlayerColor());
			return(null); 
		} if(knowledge==1)
			{ incrementMorale(replay);
			  incrementMorale(replay);
			  b.logGameEvent(EsmeTheFiremanMoralex2,getPlayerColor());
			  return(null);
			}
		return(benefit);
		
	case Moralex2AndKnowledgex2:
		decrementKnowledge(replay);
		decrementKnowledge(replay);
		incrementMorale(replay);
		incrementMorale(replay);
		b.logGameEvent(EsmeTheFiremanKnowledgex2,getPlayerColor());
		return(null); 
		
	case IcariteInfluenceAndCardx2:
		if(b.variation.isIIB()) { return(benefit); }	// must interact
		else {b.incrementAllegiance(Allegiance.Icarite,replay);
		 doArtifact(2,b,replay);
		}
		return(null);

	case IcariteAuthorityAndInfluence:
		doAuthorityAndInfluence(b,Allegiance.Icarite,replay,null,true);
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
	
	case EuphorianAuthorityAndInfluenceB:
		if(!doAuthorityAndInfluence(b,Allegiance.Euphorian,replay,b.getMarketB(Allegiance.Euphorian),true)) { return(benefit); }
		return(null);
		
	case WastelanderAuthorityAndInfluenceB:
		if(!doAuthorityAndInfluence(b,Allegiance.Wastelander,replay,b.getMarketB(Allegiance.Wastelander),true)) { return(benefit); }
		return(null);
		
	case SubterranAuthorityAndInfluenceB:
		if(!doAuthorityAndInfluence(b,Allegiance.Subterran,replay,b.getMarketB(Allegiance.Subterran),true)) { return(benefit); }
		return(null);

	case EuphorianAuthorityAndInfluenceA:
		if(!doAuthorityAndInfluence(b,Allegiance.Euphorian,replay,b.getMarketA(Allegiance.Euphorian),true)) { return(benefit); }
		return(null);
		
	case WastelanderAuthorityAndInfluenceA:
		if(!doAuthorityAndInfluence(b,Allegiance.Wastelander,replay,b.getMarketA(Allegiance.Wastelander),true)) { return(benefit); }
		return(null);
		
	case SubterranAuthorityAndInfluenceA:
		if(!doAuthorityAndInfluence(b,Allegiance.Subterran,replay,b.getMarketB(Allegiance.Subterran),true)) { return(benefit); }
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
		return incrementMorale(replay);

	}

}
private void doAlbertTheFounder(replayMode replay)
{
	if(recruitAppliesToMe(RecruitChip.AlbertTheFounder)
			&& (totalWorkers<MAX_WORKERS))
	{	// b.p1("use albert the founder");
		// gain another worker and a resoruce
		b.useRecruit(RecruitChip.AlbertTheFounder,"use");
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
{	return(!testPFlag(PFlag.HasResolvedDilemma) && canPayX(((DilemmaChip)dilemma.topChip()).cost));
}

// check a version 3 recruit if we think it's handled for this purpose
public void checkDropWorker(EuphoriaCell dest,replayMode replay) {
	for(int lim=activeRecruits.height()-1; lim>=0; lim--)
	{
		RecruitChip ch = (RecruitChip)activeRecruits.chipAtIndex(lim);
		switch(ch.recruitId)
		{
			
		default:	
			b.p1("drop with "+ch.name+" #"+ch.recruitId);
			//throw b.Error("recruit %s #%s not handled for DropWorker",ch,ch.recruitId);
			break;
		case 101: // maxime the ambassador
		case 102: // laura the philanthropist
		case 103: // zong the astronomer
		case 104: // maggie the outlaw
		case 105: // flartner the luddite
		case 106: // professor reitz the archeologist
		case 107: // jacko the activist
		case 108: // jonathan the artist
		case 109: // amanda the broker
			
		case 110: // pete the cannibal
		case 111: // sarinee the cloud miner
		case 112: // Major dave the demolitionist
		case 113: // yordy the demotivator
		case 114: // kathy the dietician
		case 115: // gary the electrician
		case 116: // michael the engineer
		case 117: // xander the excavator
		case 118: // esme the fireman
		case 119: // raw the foreman
			
		case 120: // bradley the futurist
		case 121: // jonathan the gambler
		case 122: // lee the gossip
		case 123: // josia the hacker
		case 124: // sir scarby the harvester
		case 125: // ian the horticulturist
		case 126: // faith the hydroelectrician
		case 127: // gidgit the hypnotist
		case 128: // kadan the infiltrator
		case 129: // sheppard the lobotomist
			
		case 130: // brett the lockpicker
		case 131: // ben the lodologist
		case 132: // flavio the merchant
		case 133: // chase the miner
		case 134: // josh the negotiator
		case 135: // geek the oracle
		case 136: // rebecca the peddler
		case 137: // soulless the plumber
		case 138: // curtis the propagandist
		case 139: // kyle the scavenger
			
		case 140: // steven the scholar
		case 141: // jefferson the shock artist
		case 142: // andrew the spelunker
		case 143: // phil the spy
		case 144: // matthew the thief
		case 145: // julia the thought inspector
		case 146: // nakagawa the tribute
		case 147: // nick the understudy
		case 148: // brian the viticulturist
			
		case 201: // zong the astronomer v2
		case 202: // jacko the activist v2
		case 203: // amanda the broker v2
		case 204: // dave the demolitionist v2
		case 205: // yordy the demotivator v2
	// case 206:
		case 207: // esme the fireman v2
		case 208: // jonathan the gambler v2
		case 209: // brett the lock picker v2
			
		case 210: // flavio the merchant
		case 211: // geek the oracle v2
		case 212: // steven the scholar v2
		case 213: // phil the spy v2
		case 214: // julia the thought inspector v2
		case 215: // nakagawa the tribute
		case 216: // brian the viticulturist v2
			
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
		case 231: 	// jadwiga the sleep deprevator pay 2 knowledge to play again			
		case 232:	// zara the solophist, double knowledge on commodity areas
		case 233:	// jon the amateur handyman allows using 3 commodity to build a market
		case 234:	// DougTheBuilder, sacrifice a worker instead of paying 
		case 235:	// cary the care bear, extra stuff when gaining a bear
		case 236:	// EkaterinaTheCheater, box artifact wildcard
		case 237:	// MiroslavTheConArtist, bear artifact wildcard 
		case 238:	// steve the double agent get * when reaching tier 4 of non-euphorian
		case 239:	// PmaiTheNurse lets you move down in commodity knowledge
			
		case 240:	// chaga the gamer, pay box to activate another recruit
		case 241:	// ha-joon the gold trader,no action on drop
		case 242:	// RowenaTheMentor ignore market penalties for 1 turn
		case 243:	// frazerTheMotivator, gain any commodity instead of third energy
		case 244:   // samuel the zapper, believed to be complete 2/1/2022
					// lots of nuances for samuelthezapper!		
		case 245:	// lars the ballooner, take a second action if you gain a balloon
		case 246:	// xyonthebrainsurgeon rescues a worker with a card
		case 247:	// julia the acolyte, special actions when bumped
		case 248:   // taed the brick trader pay clay to gain resource and water
		case 249:	// lionel the cook, use food to ignore market penalty

		case 250:	// alexandra the heister, artifact balloon wildcard
 		case 251:	// GeorgeTheLazyCraftsman self-bump when placing on some markets
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
		case 262:	// dusty the enforcer, pay morale to rescue a worker if you have a bar
		case 263:	// mwichwe the flusher, special cost/benedif at tunnels
		case 264:	// MakatoTheForger, bifocals artifact wildcard
		case 265:	// albert the founder
		case 266:	// PamhidzaiTheReader trade card for 3 resources
		case 267:	// borna the storyteller, no action on drop
		case 268:	// javier the underground librarian, artifact book wildcard
		case 269:	// christine the anarchist, bonus when retrieving
	
		case 270: 	// kofi the hermit, use 1 worker in opening game
		case 271:	// jeroen the hoarder, gain resource when retrieving workers
		case 272:	// spiros the model citizen, pay morale to play again
		case 273:	// davaa the shredder, use tunnels for free if not bumping
		case 274: 	// youssef the tunneler, gain both resources in tunnels 
			break;
		
		}
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
public boolean canUseChagaTheGamer() {
	return (recruitAppliesToMe(RecruitChip.ChagaTheGamer)
			&& canPay(Cost.Box)	// includes alternates for box
			&& !testTFlag(TFlag.UseChagaTheGamer)
			&& (hiddenRecruits.height()>0));
}
public boolean canUseRowenaTheMentor() {
	 return (recruitAppliesToMe(RecruitChip.RowenaTheMentor)
			 && canPayX(Cost.Knowledge)
			 && !testTFlag(TFlag.UsingRowenaTheMentor)
			 && b.anyMarketPenaltiesApply(this));
}


}