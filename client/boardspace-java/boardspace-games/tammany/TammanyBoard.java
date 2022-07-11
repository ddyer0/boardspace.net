package tammany;

import java.util.Hashtable;
import java.util.StringTokenizer;

import lib.*;
import online.game.*;
import static tammany.TammanyMovespec.*;

/**
 * TammanyBoard knows all about the game of Tammany Hall
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
 
 class TammanyPlayer
 {	int myIndex;
    TammanyChip myBoss;
    long randomV;
    int score = 0;
    int xscore = 0;		// extended score (currently, unopposed wards)
    int bossVotes = 0;
    int maxVotes = 0;
    int actualVotes = 0;
    boolean normalShow = false;
    boolean hiddenShow = true;
    boolean allVotesSet = false;
    TammanyMovespec pendingVote = null;
    int ethnicCubes[] = new int[TammanyChip.ethnics.length];
    int robotEthnicControl[] = new int[TammanyChip.ethnics.length];
    
    int wardsControlled;
    long tiebreakScore;
    int votes[] = new int[TammanyChip.ethnics.length];
    TammanyCell influence[] = new TammanyCell[TammanyChip.ethnics.length];
    TammanyCell slander = null;
    TammanyCell ballotBox = null;
    Role myRole = null;
    TammanyChip myRoleCard = null;
    int locksAvailable = 0;		// relevant if role is CouncilPresident
    boolean hasUsedSlander = false;
    public String toString() { return("<player "+myBoss+" "+myIndex+">"); }
    public String shortName() { return(myBoss.myBoss.toString()); }
	 TammanyPlayer(Random r,int ind,TammanyChip b) 
		 { randomV = r.nextLong();
		   myIndex = ind;
		   myBoss = b;
		   score = 0;
		   char myC = (char)('A'+ind);
		   slander = new TammanyCell(r,TammanyId.PlayerSlander,b,myC);
		   for(int i=0;i<3;i++) { slander.addChip(TammanyChip.slander); }
		   for(int i=0;i<4;i++) 
		   { TammanyCell c = influence[i]=new TammanyCell(r,TammanyId.PlayerInfluence,b,myC,i); 
		   	 c.setCurrentCenter(-1,-1);
		   }
		   assignNewRole(null);	// no role yet
		 }
	 
	 int totalInfluence() 
	 { 	int tot = 0; 
	 	for(TammanyCell c : influence) { tot += c.height(); }
	 	return(tot);
	 }
	 //
	 // count the wards we control.  Called just after the election, so our boss
	 // be the top chip of the ward
	 //
	 int totalWards(TammanyCell wards[])
	 {	 int tot = 0;
		 for(TammanyCell w : wards) { if(w.topChip()==myBoss) { tot++; }}
		 return(tot);
	 }
	 // calculate a single long int which includes the ward count
	 // and all secondary tiebreakers, so that the greatest number
	 // is the winner.
	 long tiebreakScore()
	 {	// first total chips, the ethnics in order
		long total = 0;
		long parts = 0;
		long shift = 0;
		for(int i=0;i<influence.length;i++)
		{	long n = influence[i].height();
			parts = (parts<<8) | n;
			total += n;
			shift += 8;
		}
		// be anal about casting to longs - java will do the "wrong" thing
		// if any of the operands are ints
		long wc =  ((long)wardsControlled<<(shift+8));
		long tot = (total<<shift);
		long val = wc | tot | parts;
		return(val);
	 }
	 // similar to the in-game tiebreak score, but mayor wins the last tiebreak
	 // and score rather than number of districts is the first de
	 long endgameTiebreakScore()
	 {	long total = 0;
		long parts = 0;
		long shift = 0;
		for(int i=0;i<influence.length;i++)
		{	long n = influence[i].height();
			parts = (parts<<8) | n;
			total += n;
			shift += 8;
		}
		parts |= (total<<shift);
		shift+=8;
		parts = (parts<<1) | ((myRole==Role.Mayor)?1:0);
		shift ++;
		return((long)score<<shift | parts);		 
	 }
	 // c is a stack of ethnic cubes we're thinking about slandering
	 boolean hasMatchingDisc(TammanyCell c)
	 {
		 for(int lim=c.height()-1; lim>=0; lim--)
		 {
			 TammanyChip ch = c.chipAtIndex(lim);
			 int ind = ch.myInfluenceIndex();
			 if(influence[ind].height()>0) { return(true); }
		 }
		 return(false);
	 }
	 
	 // count the number of each ethnic group in a cell full of cubes.
	 // ignore freezer chips
	 void collectCubeStatistics(TammanyCell cubes,int stats[])
	 {
		 for(int lim=cubes.height()-1; lim>=0; lim--)
		 {
			 TammanyChip ch = cubes.chipAtIndex(lim);
			 if(ch!=TammanyChip.freezer) { stats[ch.myInfluenceIndex()]++; }
		 }
	 }
	 //
	 // count wards controlled and enthnic cubes owned. 
	 // this is usually called with the regular list of bosses and cubes, but the 
	 // robot may call it with a fake list.
	 //
	 void collectWardStatistics(TammanyCell wardBosses[],TammanyCell wardCubes[])
	 {
		 wardsControlled = 0;
		 tiebreakScore = 0;
		 AR.setValue(ethnicCubes,0);
		 for(int i=0;i<wardBosses.length;i++)
		 {
			 if(wardBosses[i].topChip()==myBoss)
			 {	TammanyCell cubes = wardCubes[i];
				 wardsControlled++;
				 collectCubeStatistics(cubes,ethnicCubes);
			 }
		 }
		 tiebreakScore = tiebreakScore();
	 }
	 // set the differential ethnic control for this player compared to each other player
	 void setRobotEthnicControl(TammanyPlayer players[])
	 {
		 for(int lim=ethnicCubes.length-1; lim>=0; lim--)
		 {	int max = 0;
			 for(TammanyPlayer p : players)
			 {
				 if(p!=this) { max = Math.max(p.ethnicCubes[lim],max); }
			 }
		 
		 robotEthnicControl[lim] = ethnicCubes[lim]-max;
		 }
	 }
	 void startElection()
	 {	 bossVotes = 0;
	 	 maxVotes = 0;
	 	 ballotBox = null;
	 	 allVotesSet = false;
		 AR.setValue(votes,-1);
	 }
	 void setBoxVotes() 
	 {	if(ballotBox!=null)
	 	{	int total = 0;
	 		boolean all = true;
	 		for(int i=0;i<votes.length;i++)
	 		{	int v = votes[i]; 	 		
	 			{ if(v>=0) { total+=v; }
	 			  else { all = false; };
	 			}
	 		}
	 		allVotesSet = all;
	 		ballotBox.label = ((total+bossVotes)+"/"+maxVotes);
	 	}
	 }

	 public void assignNewRole(Role m)
	 {
		 myRole = m;
		 myRoleCard = TammanyChip.getRoleCard(m);
		 hasUsedSlander = false;
		 if(m!=null)
		 {
		 switch(m)
		 {
		 case CouncilPresident: 	
			 locksAvailable = 2;	// council president gets 2 locks
		 	 break;
		 case Mayor: 
			 score += 3;	// mayor gets 3 points
			 break;
		 case PrecinctChairman:
		 case ChiefOfPolice:
		 case DeputyMayor:
			 break;
			default: throw G.Error("Not expecting role "+m);
			 
		 }}
	 }
	 void copyFrom(TammanyPlayer other)
	 {	randomV = other.randomV;
	 	myIndex = other.myIndex;
	    myBoss = other.myBoss;
	    slander.copyFrom(other.slander);
	    score = other.score;
	    actualVotes = other.actualVotes;
	    hasUsedSlander = other.hasUsedSlander;
	    locksAvailable = other.locksAvailable;
	    myRole = other.myRole;
	    
	    TammanyCell.copyFrom(influence,other.influence);
	 }
	 
	 long Digest(Random r)
	 {	long val = randomV+(score*23)*randomV+(locksAvailable*824);
	 	val ^= slander.Digest(r);
	 	val ^= (1+actualVotes)*randomV*462342;
	 	val ^= TammanyCell.Digest(r,influence);
	 	return(val);
	 }
	 void samePlayer(TammanyPlayer other)
	 {	G.Assert(score==other.score,"score mismatch");
	 	G.Assert(myIndex==other.myIndex,"myIndex mismatch");
	 	G.Assert(myBoss==other.myBoss,"myBoss mismatch");
	 	G.Assert(randomV==other.randomV,"randomV mismatch");
	 	G.Assert(actualVotes==other.actualVotes,"votes mismatch");
	 	G.Assert(slander.sameCell(other.slander),"slander mismatch");
	 	G.Assert(locksAvailable==other.locksAvailable,"lock count mismatch");
	 	G.Assert(myRole==other.myRole,"myRole mismatch");
	 	G.Assert(hasUsedSlander == other.hasUsedSlander,"slander mismatch");
	 	G.Assert(TammanyCell.sameCell(influence,other.influence),"influence mismatch");
	 	
	 }
	 
	 
 }
 
 
 public class TammanyBoard extends TammanyBoardConstructor
 {	static int REVISION = 104;			// 100 represents the initial version of the game
 										// revision 101 fixes the start player problem
 										// revision 102 fixes refilling castle gardens
 										// revision 103 fixes the castle gardens reload
 										// revision 104 fixes double slander to slander only the same boss
	public int getMaxRevisionLevel() { return(REVISION); }
 	TammanyVariation variation = TammanyVariation.tammany;
    private boolean robotBoard = false;
    private TammanyPlay.Evaluator evaluator = TammanyPlay.Evaluator.None;
    public void setRobotBoard(TammanyPlay.Evaluator ev) {evaluator = ev; robotBoard = true; }

 	
 	TammanyState board_state = TammanyState.Puzzle;	
 	private TammanyState unresign = null;	// remembers the orignal state when "resign" is hit
 	public TammanyState getState() { return(board_state); }
 	public void SetDrawState() {throw G.Error("not expected"); };	
 	public void setState(TammanyState st) 
	 { 	unresign = (st==TammanyState.Resign)?board_state:null;
		 board_state = st;
		 if(!board_state.GameOver()) 
			 { AR.setValue(win,false); 	// make sure "win" is cleared
			 }
	 }

 	public TammanyPlayer[]players = null;
 	private TammanyPlayer currentLeader = null;
 	private TammanyPlayer currentLoser = null;
 	public TammanyPlayer getPlayerForBoss(TammanyChip c)
 	{
 		for(TammanyPlayer p : players) { if(p.myBoss==c) { return(p); }}
 		throw G.Error("NO associated player for "+c);
 	}
 	private void clearXscore()
 	{
		for(TammanyPlayer p : players) { p.xscore = p.score; }
 	}
 	private void setXscore()
 	{	clearXscore();
  		for(TammanyCell c : wardBosses)
 		{
 			if(c.height()==1)
 				{ TammanyChip top = c.topChip();
 				  if (top.isBoss())
 				  	{ getPlayerForBoss(top).xscore++; 
 				  	}
 				}
 		}
 	}
 	private void setLeaders()
 	 {	TammanyPlayer leader = null;
 	 	TammanyPlayer loser = null;
 	 	switch(evaluator)
 	 	{
 	 	case BashVoter:
 	 		setXscore();
 	 		break;
 	 	default:
 	 		clearXscore();
 	 	}
 	 	for(TammanyPlayer p : players)
 	 	{	if(loser==null) { loser = p; }
 	 		else if(loser.xscore>p.xscore) {loser = p; }
 	  		if(leader==null)  { leader = p; }
 	 		else if(leader.xscore<p.xscore) { leader = p; }
 	  	}
 	 	if(leader.xscore==0) { leader = players[whoseTurn]; }
 	 	currentLeader = leader;
 	 	currentLoser = loser;
 	 }
 	 
 	public TammanyPlayer getPlayer(TammanyChip b) 
 	{ 	TammanyPlayer p = getPlayerOrNull(b);
 		if(p!=null) { return(p); }
 		throw G.Error("Player %s not found",b);
 	}
 	public TammanyPlayer getPlayerOrNull(TammanyChip b)
 	{
 		for(TammanyPlayer p : players)
 		{ if(p.myBoss==b) { return(p); }}
 		return(null);
 	}
 	public boolean getPlayerNormalShow(TammanyChip b)
 	{	TammanyPlayer p = getPlayerOrNull(b);
 		if(p!=null) 
 			{ return(p.normalShow); 
 			}
 		return(false);
 	}
 	
 	StringStack gameEvents = new StringStack();
    InternationalStrings s = null;
    
	public int currentYear = 0;
	public int lastElectionYear = 0;
	public int electionOrderIndex = 0;
	private int maxBossVotes = 0;		// the maximum boss votes in this election, 
	public boolean started = false;
	public int firstPlayer = 0;
	public int slanderWard = 0;			// the ward where slander is in progress
	public TammanyChip slanderPayment = null;			// the payment type of chip
	public TammanyChip slanderedBoss = null;			// the boss removed by slander
	public TammanyChip slanderedSecondBoss = null;		// a second boss slandered
	public boolean electionMode = false;
	
	public boolean MASTER_SIMULTANEOUS_PLAY = true;
	//
	// if simultaneous play starts out true, it becomes false when the game record is 
	// canonicalized to non-ephemeral opcodes.  After that point, the game behaves as
	// if it had never had simultaneous play.
	//
	public boolean REINIT_SIMULTANEOUS_PLAY = MASTER_SIMULTANEOUS_PLAY;
	public boolean SIMULTANEOUS_PLAY = MASTER_SIMULTANEOUS_PLAY;
	public void setSimultaneousPlay(boolean val)
	{
		MASTER_SIMULTANEOUS_PLAY = REINIT_SIMULTANEOUS_PLAY = SIMULTANEOUS_PLAY = val; 
	}
	
 	CellStack animationStack = new CellStack();
 	
 	
	 // intermediate states in the process of an unconfirmed move should
	 // be represented explicitly, so unwinding is easy and reliable.
	 public TammanyChip pickedObject = null;
	 public TammanyChip castleChip = null;	// chip picked from castle gardens
	 public TammanyChip lastPicked = null;
	 public TammanyChip lastDroppedObject = null;	// for image adjustment logic
	 private CellStack pickedSourceStack =  new CellStack(); 
	 private IStack pickedIndexStack = new IStack();
	 
	 private CellStack droppedDestStack = new CellStack();
	 private StateStack stateStack = new StateStack();
	 	 
	 // constructor 
	 public TammanyBoard(InternationalStrings strs,String init,int players,long key,int map[],int rev) // default constructor
	 {	s = strs;
	 	setColorMap(map);
        electionBox = electionBox(43,75,73,95);
	 	buildNetwork();
	 	doInit(init,key,players,rev); // do the initialization 
	 }
	 
	 public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
	 
	 
	 // count the cubes controlled by a boss.  This is called
	 // immediately after the election when there is only one boss.
	 public int countCubesControlled(TammanyChip boss,TammanyChip cube)
	 {	int n=0;
		 for(int lim=wardBosses.length-1; lim>=0; lim--)
		 {
			 if(wardBosses[lim].topChip()==boss)
			 {
				 n+=wardCubes[lim].countChips(cube); 
			 }
		 }
		 return(n);
	 }

	 public void doInit(String gtype,long key)
	 {
		 StringTokenizer tok = new StringTokenizer(gtype);
		 String typ = tok.nextToken();
		 int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
		 long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
		 int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
		 doInit(typ,ran,np,rev);
	 }
	 public boolean playerHasPlayed(int index)
	 {	// true of this player has registered his vote
		 return(players[index].pendingVote!=null);
	 }
	 /* initialize a board back to initial empty state */
	 public void doInit(String gtype,long key,int np,int rev)
	 {	 randomKey = key;
	   	 adjustRevision(rev);
		 currentYear = 0;
		 lastElectionYear = 0;
		 electionOrderIndex = 0;
		 slanderWard = 0;
		 slanderPayment = null;
		 slanderedBoss = null;
		 slanderedSecondBoss = null;
		 electionMode = false;
		 SIMULTANEOUS_PLAY = REINIT_SIMULTANEOUS_PLAY;
		 gameEvents.clear();
		 players_in_game = np;
		 win = new boolean[np];
		 players = new TammanyPlayer[np];
		 int map[] = getColorMap();
		 Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
		 for(int i=0;i<np;i++) { players[i]=new TammanyPlayer(r,i,TammanyChip.getBoss(map[i])); }
		 
		 setState(TammanyState.Puzzle);
		 variation = TammanyVariation.findVariation(gtype);
		 G.Assert(variation!=null,WrongInitError,gtype);
		 gametype = gtype;
		 switch(variation)
		 {
		 default: throw G.Error("Not expecting variation %s",variation);
		 case tammany:
			 for(TammanyCell c = allCells; c!=null; c=c.next) { c.reInit(); }
		 }
 		 
		 // 25 chips of each color
		 for(int i=0;i<25;i++)
		 {	for(TammanyChip ch : TammanyChip.ethnics) { bag.addChip(ch); }
		 }
		 
		 for(int i=0;i<zone1Init.length;i++) 
		 	{	TammanyChip v = zone1InitValues[i];
		 		bag.removeChip(v);
		 		zone1Init[i].reInit(); zone1Init[i].addChip(v);
		 	}
		 for(int i=0;i<zone2Init.length;i++)
		 	{	TammanyChip v = zone2InitValues[i];
	 			bag.removeChip(v);
	 			zone2Init[i].reInit(); zone2Init[i].addChip(v);
		 	}
		 for(int i=0;i<zone3Init.length;i++)
		 	{	TammanyChip v = zone3InitValues[i];
	 			bag.removeChip(v);
	 			zone3Init[i].reInit(); zone3Init[i].addChip(zone3InitValues[i]);
		 	}
		 
		 replenishInfluence();
		 firstPlayer = whoseTurn = Random.nextInt(new Random(randomKey*525256),players_in_game);
		 started = false;
		 droppedDestStack.clear();
		 pickedSourceStack.clear();
		 pickedIndexStack.clear();
		 stateStack.clear();		 
		 pickedObject = null;
		 castleChip = null;
		 lastDroppedObject = null;
		 emptyTrash();
		 animationStack.clear();
		 moveNumber = 1;
		 startNewYear();
		 // note that firstPlayer is NOT initialized here
	 }
	 public void emptyTrash()
	 {	
		 wasteBasket.reInit();
		 wasteBasket.addChip(TammanyChip.trash);
	 }
	 public void replenishInfluence()
	 {	
		for(TammanyCell c : influence)
		 {
			 if(c.topChip()==null) { c.addChip(TammanyChip.influence[c.row]); }
		 }

	 }
	 public void replenishAvailableInfluence(TammanyPlayer p,TammanyCell chips)
	 {	
		for(TammanyCell c : influence)
		 {	c.reInit();
		 	if((p.influence[c.row].height()>0) && (chips.containsChip(TammanyChip.ethnics[c.row]))) 
		 		{ c.addChip(TammanyChip.influence[c.row]); 
		 		}
		 }

	 }	 

	 /** create a copy of this board */
	 public TammanyBoard cloneBoard() 
	 { TammanyBoard dup = new TammanyBoard(s,gametype,players_in_game,randomKey,getColorMap(),revision); 
	   dup.copyFrom(this);
	   return(dup); 
	   }
	 public void copyFrom(BoardProtocol b) { copyFrom((TammanyBoard)b); }
 
	 /* make a copy of a board.  This is used by the robot to get a copy
	  * of the board for it to manipulate and analyze without affecting 
	  * the board that is being displayed.
	  *  */
	 public void copyFrom(TammanyBoard from_b)
	 {
		 super.copyFrom(from_b);
		 board_state = from_b.board_state;
		 unresign = from_b.unresign;
		 currentYear = from_b.currentYear;
		 lastElectionYear = from_b.lastElectionYear;
		 maxBossVotes = from_b.maxBossVotes;
		 slanderWard = from_b.slanderWard;
		 slanderPayment = from_b.slanderPayment;
		 slanderedBoss = from_b.slanderedBoss;
		 slanderedSecondBoss = from_b.slanderedSecondBoss;
		 electionOrderIndex = from_b.electionOrderIndex;
		 electionMode = from_b.electionMode;
		 SIMULTANEOUS_PLAY = from_b.SIMULTANEOUS_PLAY;
		 REINIT_SIMULTANEOUS_PLAY = from_b.REINIT_SIMULTANEOUS_PLAY;
		 
		 variation = from_b.variation;
		 getCell(droppedDestStack,from_b.droppedDestStack);
		 getCell(pickedSourceStack,from_b.pickedSourceStack);
		 pickedIndexStack.copyFrom(from_b.pickedIndexStack);
		 stateStack.copyFrom(from_b.stateStack);	 
		 pickedObject = from_b.pickedObject;
		 castleChip = from_b.castleChip;
		 lastPicked = null;
		 firstPlayer = from_b.firstPlayer;
		 started = from_b.started;
		 robotBoard = from_b.robotBoard;
		 evaluator = from_b.evaluator;
		 
		 for(int i=0;i<players.length;i++) { players[i].copyFrom(from_b.players[i]); }
  
		 sameboard(from_b); 
	 }
 
	 
	 public void sameboard(BoardProtocol f) 
	 	{ sameboard((TammanyBoard)f); 
	 	}
 
	 /**
	  * Robots use this to verify a copy of a board.  If the copy method is
	  * implemented correctly, there should never be a problem.  This is mainly
	  * a bug trap to see if BOTH the copy and sameboard methods agree.
	  * @param from_b
	  */
	 public void sameboard(TammanyBoard from_b)
	 {
		 super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
		 G.Assert(unresign==from_b.unresign,"unresign mismatch");
		 G.Assert(currentYear==from_b.currentYear,"year mismatch");
		 G.Assert(lastElectionYear==from_b.lastElectionYear,"electionyear mismatch");
		 G.Assert(electionOrderIndex==from_b.electionOrderIndex,"election ward mismatch");
		 G.Assert(maxBossVotes==from_b.maxBossVotes,"maxBossVotes mismatch");
		 
		 G.Assert(slanderWard==from_b.slanderWard,"slander ward mismatch");
		 G.Assert(slanderPayment==from_b.slanderPayment,"slander payment mismatch");
		 G.Assert(slanderedBoss==from_b.slanderedBoss,"slandered boss mismatch");
		 G.Assert(slanderedSecondBoss==from_b.slanderedSecondBoss,"slandered second boss mismatch");
		 G.Assert(electionMode==from_b.electionMode,"election mode mismatch");
		 G.Assert(variation==from_b.variation,"variation matches");
		 G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
		 G.Assert(castleChip==from_b.castleChip,"castle chip matches");
		 if(board_state!=TammanyState.Puzzle)
		 {	// not reliable in UI rearrangement
		 G.Assert(sameCells(pickedSourceStack,from_b.pickedSourceStack),"pickedSource mismatch");
		 }
		 G.Assert(sameContents(pickedIndexStack,from_b.pickedIndexStack),"pickedindex mismatch");
		 G.Assert(sameCells(droppedDestStack,from_b.droppedDestStack),"droppedDest mismatch");
		 for(int i=0;i<players.length;i++) { players[i].samePlayer(from_b.players[i]); }
		 G.Assert(firstPlayer==from_b.firstPlayer,"mismatch firstPlayer");
		 // this is a good overall check that all the copy/check/digest methods
		 // are in sync, although if this does fail you'll no doubt be at a loss
		 // to explain why.
		 G.Assert(Digest()==from_b.Digest(),"Digest matches");
 
	 }
	 
	 public TammanyPlayer getPlayer(int n){ return(players[n]); }
 
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
		 for(TammanyPlayer p : players) { v ^= p.Digest(r); }
		 v ^= chip.Digest(r,pickedObject);
		 v ^= chip.Digest(r,castleChip);
		 if(board_state!=TammanyState.Puzzle)
			 {	// not reliable in UI rearrangement
			 v ^= Digest(r,pickedSourceStack);
			 }
		 v ^= Digest(r,pickedIndexStack);
		 v ^= Digest(r,droppedDestStack);
		 v ^= Digest(r,currentYear);
		 v ^= Digest(r,lastElectionYear);
		 v ^= Digest(r,slanderWard);
		 v ^= TammanyChip.Digest(r,slanderPayment);
		 v ^= TammanyChip.Digest(r,slanderedBoss);
		 v ^= TammanyChip.Digest(r,slanderedSecondBoss);
		 v ^= Digest(r,firstPlayer);
		 v ^= Digest(r,electionOrderIndex*12356);
		 v ^= Digest(r,electionMode);
		 v ^= Digest(r,droppedDestStack);
		 v ^= r.nextLong()+(board_state.ordinal()*10+whoseTurn);

		 return (v);
	 }
 
	 private int nextPlayer(int who)
	 {
		 return((who+1)%players_in_game);
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
		 case ConfirmNewRoles:
			 moveNumber++; //the move is complete in these states
			 setWhoseTurn(firstPlayer);
			 break;
			 
		 case SimultaneousElection:
		 case SerialElection:
		 case ConfirmPlacementOrSlander:
		 case ConfirmPlacement:
		 case ConfirmUseRole:
		 case ConfirmSlander:
		 case DoubleSlander:
		 case Lock:
		 case Arrest:
		 case Move:
		 case Resign:
		 case Disc:
		 case Election:
			 moveNumber++; //the move is complete in these states
			 setWhoseTurn(nextPlayer(whoseTurn));
			 return;
		 }
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
 
 
	 public boolean WinForPlayerNow(int player)
	 {	if(win[player]) { return(true); }
	 	return(false);
	 }

	 //
	 // accept the current placements as permanent
	 //
	 public void acceptPlacement()
	 {	droppedDestStack.clear();
		 pickedSourceStack.clear();
		 pickedIndexStack.clear();
		 stateStack.clear();
		 pickedObject = null;
		 castleChip = null;
	  }
	 //
	 // undo the drop, restore the moving object to moving status.
	 //
	 private TammanyCell unDropObject(replayMode replay)
	 {	if(droppedDestStack.size()>0) 
		 {	TammanyCell rv = droppedDestStack.pop();
			 pickedObject = rv.removeTop();
			 if(pickedObject==TammanyChip.freezer)
			 {
				 players[whoseTurn].locksAvailable++;
			 }
			 setState(stateStack.pop());
			 if(slanderedSecondBoss!=null) { slanderedSecondBoss = null; }
			 else {
				 if(pickedObject==slanderedBoss) { slanderedBoss = null; }
				 else if(pickedObject==slanderPayment) { slanderPayment=null; }
			 }
			 	if(pickedObject==TammanyChip.slander)
			 	{	// need to clean up the chip stack which placing the slander chip disturbed.
			 		replenishInfluence();
			 		if(castleChip!=null)
			 		{
			 			int inf = castleChip.myInfluenceIndex();
			 			influence[inf].removeTop();
			 		}
			 	}
				if(pickedObject==castleChip)
				{	int inf = castleChip.myInfluenceIndex();
					TammanyCell from = players[whoseTurn].influence[inf];
					TammanyCell to = influence[inf]; 
					TammanyChip ch = from.removeTop();
					to.addChip(ch);
					if(replay!=replayMode.Replay)
					{
						animationStack.push(from);
						animationStack.push(to);
					}
				}

			 return(rv);
		 }
		 return(null);
	 }
	 // 
	 // undo the pick, getting back to base state for the move
	 //
	 private void unPickObject(replayMode replay)
	 {	if(pickedObject!=null)
		 {	TammanyCell rv = pickedSourceStack.pop();
		 	int rind = pickedIndexStack.pop();
			if(rind==-1) { rind=rv.height(); }
			rv.insertChipAtIndex(rind,pickedObject);
			setState(stateStack.pop());
			
		 	if((board_state==TammanyState.PlayOption)
					 && (rv.rackLocation()==TammanyId.CastleGardens))
			 	{
				 castleChip = null;
			 	}		 
			pickedObject = null;
		 }
   }
	 // 
	 // drop the floating object.
	 //
	 private void dropObject(TammanyCell c)
	 {	if(pickedObject!=null)
		 {	G.Assert(c.topChip()!=TammanyChip.freezer,"missed a freeze");
		 	c.addChip(pickedObject);
			stateStack.push(board_state);
			droppedDestStack.push(c);
			pickedObject = null;
		 }
	  }
	 
	 // pick something up.  Note that when the something is the board,
	 // the board location really becomes empty, and we depend on unPickObject
	 // to replace the original contents if the pick is cancelled.
	 private void pickObject(TammanyCell c,int ind)
	 {	pickedSourceStack.push(c);
	 	pickedIndexStack.push(ind);
	 	if(ind==-1) { ind = c.height()-1; }
	 	pickedObject = c.removeChipAtIndex(ind);
	 	if((board_state==TammanyState.PlayOption)
				 && (c.rackLocation()==TammanyId.CastleGardens))
		 	{
			 castleChip = pickedObject;
		 	}		 


		 stateStack.push(board_state);
	 }
    //
    // true if c is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(TammanyCell c)
    {	return(droppedDestStack.top()==c);
    }
    public boolean isDest(TammanyCell c,int ind) 
    {
    	return(isDest(c)&&((ind==-1)||(ind==(c.height()-1))));
    }
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    // caution! this method is called in the mouse event process
    public int movingObjectIndex()
    { TammanyChip ch = pickedObject;
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
    private TammanyCell getCell(TammanyId source, char col,int row)
    {
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case Trash:
        	return(wasteBasket);
        case YearIndicator:
        	setYearMarker();
        	return(yearIndicator[row-1]);
        case ScoringTrack:
        	setScores();
         	return(row<scoreTop.length ? scoreTop[row] : scoreSide[row-scoreTop.length]);
        case Bag:
        	return(bag);
        // role marker cells
        case PrecinctChairman:
        	return(precinctChairmanRole);
        case Mayor:
        	return(mayorRole);
        case DeputyMayor:
        	return(deputyMayorRole);
        case ChiefOfPolice:
        	return(policeChiefRole);
        case CouncilPresident:
        	return(councilPresidentRole);
        	
        case PlayerSlander:
        	return(players[col-'A'].slander);
        case PlayerInfluence:
        	return(players[col-'A'].influence[row]);
        case InfluencePlacement:
        	return(influence[row]);
        case CastleGardens:
        	return(castleGardens[row]);
        case SlanderPlacement:
        	return(slanderPlacement);
        case LockPlacement:
        	return(lockPlacement[row]);
        case BossPlacement:
        	return(bossPlacement[row]);
        case WardCube:
        	return(wardCubes[row]);
        case WardBoss:
        	return(wardBosses[row]);
        case Zone1Init:
        	return(zone1Init[row]);
        case Zone2Init:
        	return(zone2Init[row]);
        case Zone3Init:
        	return(zone3Init[row]);
        } 	
    }
    public TammanyCell getCell(TammanyCell c)
    {
    	return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
    }

    //	
    //true if cell is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(TammanyCell c)  {	return(c==pickedSourceStack.top());
    }
    public TammanyCell getSource() { return(pickedSourceStack.top()); }
    public TammanyCell getDest() { return(droppedDestStack.top()); }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //
    private boolean castleGardensHasCubes()
    {
    	for(TammanyCell c : castleGardens) { if(c.topChip()!=null) { return(true); }}
    	return(false);
    }
    private boolean allRolesDistributed()
    {
    	for(TammanyCell c : bossPlacement) { if (c.topChip()!=null) { return(false); }}
    	return(true);
    }
    private void setNextStateAfterPick()
    {
    	switch(board_state)
    	{
    	default: break;
    	case Puzzle:
    		if(pickedObject==TammanyChip.pawn) {  }
    		else if(pickedObject.isBoss())
    		{	TammanyCell c = getSource();
    			switch(c.rackLocation())
    			{
    			case ScoringTrack:
    				break;
    			case Mayor:	
            	case DeputyMayor:
            	case ChiefOfPolice: 
            	case PrecinctChairman: 
            	case CouncilPresident:
           			TammanyPlayer pl = getPlayer(pickedObject);
               		pl.assignNewRole(null); 
            		break;
				default:
					break;
    			}
    			
    		}
    		break;
    	}
    }

    //
    // get here when we drop a slander disc on a ward.
    //
    private void startingSlander()
    {
    	TammanyCell dest = getDest();
    	TammanyChip top = dest.topChip();
    	G.Assert(top==TammanyChip.slander,"must be starting slander");
    		// starting a slander
   		slanderWard = dest.row;
   		slanderPayment = null;
   		// null will mean we have finished the after-placement options, and need to
   		// start the next player.  Non-null means we need to resume after-placement moves.
		replenishAvailableInfluence(players[whoseTurn],wardCubes[slanderWard]);
   		setState(TammanyState.SlanderPayment);
    }
    
    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case DistributeRoles:
        	if(allRolesDistributed()) { setState(TammanyState.ConfirmNewRoles); }
        	break;
        case Play:
        	setState(castleGardensHasCubes() ? TammanyState.PlayOption : TammanyState.Play2);
 			break;
        case ConfirmSlander:
        case SerialElection:
        	break;
        case ConfirmPlacementOrSlander:
        	startingSlander();
        	break;
        case DoubleSlander:
        	if(getDest()==wasteBasket)
        	{
        		TammanyChip top = wasteBasket.topChip();
        		if(top.isBoss()) 
        		{ slanderedSecondBoss = top;
        		  if(revision>=104) 
        		  	{ setState(TammanyState.ConfirmSlander); 
        		  	} 
        		}
        	}
        	G.Assert(slanderedSecondBoss!=null,"expecting someone to be slandered");
        	break;
        case SlanderPayment:
        	if(getDest()==wasteBasket)
        	{	TammanyChip top = wasteBasket.topChip();
        		if(top.isInfluence()) { slanderPayment = top; }
        		else if(top.isBoss()) { slanderedBoss = top; }
        	}
        	if((slanderedBoss!=null) && (slanderPayment!=null))
        	{	
        		setState(hasDoubleSlanderMoves() ? TammanyState.DoubleSlander : TammanyState.ConfirmSlander);
        	}
        	break;
        	
        case Lock:
        	if(revision>=101)
        	{
        	TammanyCell dest = getDest();
        	if(dest.topChip()==TammanyChip.freezer) 
        	{
        		TammanyPlayer p = players[whoseTurn];
        		p.locksAvailable--;
        	}}
			//$FALL-THROUGH$
		case Arrest:
        case Move:
        case Disc:
        	setState(TammanyState.ConfirmUseRole);
        	break;
        	
        case TakeDisc:
        case PlaceCube:     	
        	setState(TammanyState.ConfirmUseRole);
        	break;
        	
        case PlayOption:
        	if(castleChip!=null)
        	{	// if the move is to place an ethnic cube, give him a corresponding chip
        		TammanyPlayer p = players[whoseTurn];
        		TammanyChip ch = castleChip.getInfluenceDisc();
        		int index = ch.myInfluenceIndex();
        		TammanyCell src = influence[index];
        		TammanyCell dest = p.influence[index];
        		dest.addChip(ch);
        		src.removeTop();
        		if(replay!=replayMode.Replay)
        		{
        			animationStack.push(src);
        			animationStack.push(dest);
        		}
        	}
			//$FALL-THROUGH$
		case Play2:
        	setState( ((currentYear>4) && hasSlanderMoves()) 
        				?  TammanyState.ConfirmPlacementOrSlander
        				: TammanyState.ConfirmPlacement);
        	break;
        case Puzzle:
        	{
        	TammanyCell dest = getDest();
        	TammanyChip top = dest.topChip();
        	// fix various game states for the display only cells
        	if(top==TammanyChip.pawn)
        	{
        		switch(dest.rackLocation())
        		{
        		default: break;
        		case YearIndicator:
        			currentYear = dest.row;
        			setYearMarker();
        			break;
        		}
        	}
        	else if(top.isBoss())
        	{
        	TammanyPlayer pl = getPlayer(top);
        	switch(dest.rackLocation())
        	{
        	case ScoringTrack:
        		pl.score = dest.row;
        		setScores();
        		break;
        	case Mayor:	
        		pl.assignNewRole(Role.Mayor);
        		break;
        	case DeputyMayor:
        		pl.assignNewRole(Role.DeputyMayor); 
        		break;
        	case ChiefOfPolice: 
        		pl.assignNewRole(Role.ChiefOfPolice); 
        		break;
        	case PrecinctChairman: 
        		pl.assignNewRole(Role.PrecinctChairman);
        		break;
        	case CouncilPresident:
        		pl.assignNewRole(Role.CouncilPresident); 
        		break;
        		
        	default: break;
        	}}
			acceptPlacement();
        	}
            break;
        }
    }
    
    private TammanyPlayer electNewMayor()
    {
    	for(TammanyPlayer p : players)
    	{
    		p.collectWardStatistics(wardBosses,wardCubes);
    	}
    	TammanyPlayer winner = null;
    	TammanyPlayer second = null;
    	for(TammanyPlayer p : players)
    	{	if(winner == null) { winner = p; }
    		else if(p.tiebreakScore>=winner.tiebreakScore)
    		{
    			second = winner;
    			winner = p;
    		}
    	}
    	if((second!=null) && (winner.tiebreakScore == second.tiebreakScore))
    	{
    		winner = null;
    	}
    	return(winner);
    }
    
    private void assignEthnicControl(replayMode replay)
    {
    	for(TammanyChip c : TammanyChip.ethnics)
    	{	int max = 0;
    		int index = c.myInfluenceIndex();
    		ethnicControls[index].reInit();
    		for(TammanyPlayer p : players)
    		{
    			max = Math.max(max,countCubesControlled(p.myBoss,c));
    		}
    		if(max>0)
    		{
    			for(TammanyPlayer p : players)
    			{
    				if(max==countCubesControlled(p.myBoss,c))
    				{	
    					ethnicControls[index].addChip(p.myBoss);
    					for(int i=0;i<3;i++)
    					{	// award 3 influence chips
    						p.influence[index].addChip(TammanyChip.influence[index]);
    						if(replay!=replayMode.Replay)
    						{
    							animationStack.push(influence[index]);
    							animationStack.push(p.influence[index]);
    						}
    					}
    				}
    			}
    		}
    	}
    }
    private void leaveElectionMode(replayMode replay)
    {
    	electionMode = false;
    	lastElectionYear = currentYear;
    	assignEthnicControl(replay);			// assign new chips, then elect mayor
    	TammanyPlayer p = electNewMayor();
    	if(p!=null)
    	{	int i=0;
    		// set up the boss cells and assign the mayor
			logGameEvent(NewMayorMessage,p.shortName());
			for(TammanyCell c : bossPlacement) { c.reInit(); }
    		for(TammanyPlayer pl : players)
    			{ pl.assignNewRole(null);
    			  if(pl!=p) { bossPlacement[i].addChip(pl.myBoss); i++; }
    			}
    		for(TammanyCell c : roleCells) { c.reInit(); }
    		firstPlayer = p.myIndex;	// mayor is the first player
    		p.assignNewRole(Role.Mayor);
    		mayorRole.addChip(p.myBoss);
     		setWhoseTurn(firstPlayer);
     		emptyTrash();
    		setState(TammanyState.DistributeRoles);
    	}
    	else
    	{
       	logGameEvent(NoNewMayor);
     	setWhoseTurn(firstPlayer);
  		emptyTrash();
    	setNextPlayState();
    	}
    }
    private void doGameOver()
    {	for(TammanyPlayer p : players) 
    	{
    		if(p.slander.height()>0)
    		{
    			p.score += p.slander.height();
    			logGameEvent(UnusedSlander,p.shortName(),""+p.slander.height());
    		}
    	}
    	// award points for ethnic control
    	for(TammanyChip ethnic : TammanyChip.ethnics)
    	{	int max = -1;
    		int ind = ethnic.myInfluenceIndex();
    		for(TammanyPlayer p : players)
    		{	max = Math.max(p.influence[ind].height(),max);
    		}
    		if(max>0)
    		{	// 2 points for each player who has the right number of chips
    			for(TammanyPlayer p : players)
        		{	if(p.influence[ind].height()==max)
        			{
        			p.score += 2;
        			logGameEvent(PointsForEthnicControl,p.shortName(),ethnic.myCube.toString());
        			}
        		}
    		}
    		
    	}
    	long max = 0;
    	TammanyPlayer best = null;
    	for(TammanyPlayer p : players)
    	{	long sc = p.endgameTiebreakScore();
    		if((best==null) || (sc>max))
    		{
    			best = p;
    			max = sc;
    		}
    	}
    	AR.setValue(win,false);
    	win[best.myIndex] = true;
    	
    	setState(TammanyState.Gameover);
    }
    private void setNewElectionWard(replayMode replay)
    {	for(TammanyPlayer p : players) { p.pendingVote = null; p.actualVotes = 0; }
    	electionOrderIndex++;
       	G.Assert(electionOrderIndex<electionOrder.length,"too many elections");
       	emptyTrash();
    	
    	{
    	int wardNumber = electionWard();
    	if(wardCubes[wardNumber].height()==0)
    	{
    		// an inactive district
    		setElectionState(replay);
    	}
    	else
    	{
    	TammanyCell ward = wardBosses[wardNumber];
    	TammanyPlayer winner = null;
    	int contestants = 0;
    	int voters = 0;
    	maxBossVotes = 0;
    	whoseTurn = firstPlayer;
  
    	for(TammanyPlayer p : players) 
    		{ p.startElection();
    		  int bossVotes = ward.countChips(p.myBoss);
    		  maxBossVotes = Math.max(maxBossVotes,bossVotes);
    		  if(bossVotes>0) 
    		  	{ winner = p; 
    		  	  contestants++;
    		  	  if(playerCanVote(p)) { voters++; }
    		  	} 
    		}
    	
    	switch(contestants)
    	{
    	case 0:	logGameEvent(VacantMessage,""+wardNumber); 
    			if(wardCubes[wardNumber].topChip()==TammanyChip.freezer) { wardCubes[wardNumber].removeTop(); }
       			setElectionState(replay);
          		break;
    	case 1: awardElection(winner,replay); 
	    		break;
    	default: 
    		if(voters==0)
        		{	
    			// no voters, award to the inevitable winner
        		finishElection(replay);
        		}
        		else
        		{	int pl = whoseTurn;
        			while(!playerCanVote(players[whoseTurn])) 
        				{ setNextPlayer();
        				  G.Assert(pl!=whoseTurn,"nobody can vote");
        				}
        		}
    		break;   	
    	}
    	}
    	}
    }
    private void setElectionState(replayMode replay)
    {	

    	if( ((electionOrderIndex+1)>=electionOrder.length)
    		|| (wardCubes[nextElectionWard()].height()==0))
    	{
    		leaveElectionMode(replay);
    		if(currentYear >= 16) { doGameOver(); }
    	}
    	else
    	{
    	moveNumber++;
        setWhoseTurn(firstPlayer);
    	setState(TammanyState.Election);
    	}
    }
    private void setNewElection(replayMode replay)
    {	
    	electionOrderIndex = -1;
    	electionMode = true;
    	reInit(lockPlacement);
    	setElectionState(replay);
    }
    private void awardElection(TammanyPlayer winner,replayMode replay)
    {	int ward = electionWard();
    	TammanyCell boss = wardBosses[ward];
    	TammanyCell cube = wardCubes[ward];
    	int initialHeight = boss.height();
    	boss.reInit();		// get rid of all the bosses
    	if(cube.topChip()==TammanyChip.freezer)
    		{ cube.removeTop(); }	// unfreeze
    	if(winner!=null)
    	{
    	logGameEvent(WinnerMessage,""+ward,""+s.get(winner.shortName()));
   		boss.addChip(winner.myBoss);
		winner.score++;
		if(cube==tammanyHallCubes) { winner.score++; }
    	switch(electionOrderIndex)
    	{	// electionOrderIndex is the next election, not the one just finished
    	case 0:	
    	case 1:	
    			setWhoseTurn(winner.myIndex);
    			loadCastleGardensWith4();	// empty and reload
    			setState(TammanyState.PlaceCube);
    			break;
    	case 2:
    	case 3:	setWhoseTurn(winner.myIndex);
    			replenishInfluence();
     			setState(TammanyState.TakeDisc);
    			break;
    	default:
    			setElectionState(replay);
    	}}
    	else 
    	{
       	// no winner, no special states, just go on
    	logGameEvent((initialHeight==0)?EmptyMessage:TieMessage,""+ward);
       	setElectionState(replay);
       	}
    }
    // finish an election, give points to the winner, change state for the next election or return to normal play
    private void finishElection(replayMode replay)
    {
    	for(TammanyPlayer p : players)
		{	TammanyMovespec m = p.pendingVote;
			p.pendingVote = null;
			processVotes(p,m,replay);
		}
     	TammanyPlayer winner = null;
    	TammanyPlayer tie = null;
    	for(TammanyPlayer p : players)
    	{
    		if(p.actualVotes>0)
    		{	if(winner==null) { winner = p; }
    			else if(p.actualVotes>=winner.actualVotes) 
    			{	tie = winner;
    				winner = p;
    			}
    		}
    		p.pendingVote = null;
    	}
    	if((tie!=null)
    			&& (winner!=null) 
    			&& (tie.actualVotes==winner.actualVotes))
    		{ winner = null; 
    		} 
    	awardElection(winner,replay);
  
    }
    public int nextElectionWard()
    {
      	return(electionOrder[electionOrderIndex+1]);
      	   	
    }
    public int electionWard()
    {
    	return(electionOrder[Math.max(0,electionOrderIndex)]);
    }
    
    // assign the rest of the roles to the players.  Mayor
    // has already been assigned, and points awarded, so be
    // sure not to assign it again.
    public void assignRoles()
    {
    	for(TammanyPlayer p : players)
    	{
    		for(TammanyCell c : roleCells)
    		{	TammanyChip ch = c.topChip();
    			if(ch==p.myBoss)
    			{	Role r = Role.getRole(c.rackLocation());
    				if(r!=p.myRole)
    				{
    					p.assignNewRole(r);
    				}
    				break;
    			}
    		}
    	}
    }
    //
    // start normal play for a new player.  First get player role action
    // or if none, then place first boss.
    //
    private void setNextPlayState()
    {
		replenishInfluence();
		
		if((revision>=102) && !castleGardensHasCubes())
			{
			if(revision==102) { loadCastleGardensWith4(); }
			else { loadCastleGardensWithPlayersPlus2();
			}}

		if(whoseTurn==firstPlayer) { startNewYear(); }
			startNewPlayer(players[whoseTurn]);
	   		TammanyPlayer p = players[whoseTurn];
    		TammanyState nextState = TammanyState.Play;

	   		if(p.myRole!=null)
				{
				switch(p.myRole)
					{
	 			case DeputyMayor:	
	 				nextState = TammanyState.Disc; 
					replenishInfluence();
	 				break;
				case CouncilPresident:
					if(hasLockMoves()) { nextState = TammanyState.Lock; }
					break;
					
				case ChiefOfPolice:	
					if(hasArrestMoves()) { nextState = TammanyState.Arrest; } 
					break;
					
				case PrecinctChairman:
					if(hasRelocateMoves()) { nextState = TammanyState.Move; }
					break;
				case Mayor: // no special power
					break;
				default:
					break;
					}
				}
		setState(nextState);
    }
    private void setNextPlayState(replayMode replay)
    {
		// slander is always last
		if(electionTime())
 				{ setNewElection(replay); 
 				}
			else { setNextPlayer();
				setNextPlayState();
				}

    }
    private void setNextStateAfterDone(replayMode replay)
    {	// current player not yet changed
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case ConfirmNewRoles:
    		assignRoles();
    		startNewYear();
    		setWhoseTurn(firstPlayer);
    		startNewPlayer(players[whoseTurn]);
    		setState(TammanyState.Play);
    		break;
    	case DoubleSlander:
    	case ConfirmSlander:
    		// extract the chip payment
    		{
    		TammanyPlayer p = players[whoseTurn];
    		G.Assert(slanderPayment!=null,"slander payment made");
    		G.Assert(slanderedBoss!=null,"slandered boss selected");
    		emptyTrash();
    		TammanyCell from = p.influence[slanderPayment.myInfluenceIndex()];
    		from.removeTop();
    		p.slander.removeTop();
    		if(slanderedSecondBoss!=null)
    		{
    			from.removeTop();
    			from.removeTop();
    		}

    		p.hasUsedSlander = true;
    		slanderWard = 0;
    		slanderPayment = null;
    		slanderedBoss = null;
    		slanderedSecondBoss = null;
    		
    		setNextPlayState(replay);
    		}
    		break;
    		
       	case ConfirmUseRole:
       		replenishInfluence();
    		setState(TammanyState.Play);
    		break;

    	case ConfirmPlacementOrSlander:
    	case ConfirmPlacement:
     		setNextPlayState(replay);
    		break;
    	
    	case Lock:
    	case Arrest:
    	case Move:
    	case Disc:

    		
    	case Puzzle:
    		startNewPlayer(players[whoseTurn]);
			//$FALL-THROUGH$
		case Play:
     		setState(TammanyState.Play);
    		
    		break;
    	}
    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();

        if (board_state==TammanyState.Resign)
        {
            win[nextPlayer(whoseTurn)] = true;
    		setState(TammanyState.Gameover);
        }
        else
        {	
        	if(WinForPlayerNow(whoseTurn)) 
        		{ win[whoseTurn]=true;
        		  setState(TammanyState.Gameover); 
        		}
        	else {
        		if(electionMode)
        		{	// done during an election
            		setNextPlayer();
            		if(!board_state.isElection())
        			{	// returning to election after placing a cube or a taking a disc
         				setElectionState(replay);
         			}
        			else
        			{
        			while ((whoseTurn!=firstPlayer) && (!playerCanVote(players[whoseTurn])))
        					{ setNextPlayer(); 	// skip players who can't vote
        					}
        			if(whoseTurn==firstPlayer)
        				{
        				if((board_state==TammanyState.SimultaneousElection)
        						|| (board_state==TammanyState.SerialElection))
        				{
        					// process the actual votes
        					finishElection(replay);
        				}
        				else {
        					throw G.Error("Not expecting state %s",board_state);
        				}
        				}
         			
        			}
        			// else remain in election mode
        		}
        		else {
         			setNextStateAfterDone(replay);
        		}
        		}
         }
    }
    private void makePile(TammanyCell pile,TammanyCell dist[])
    {	
		for(int i=0;i<dist.length;i++)
		{	TammanyCell from = dist[i];
			if(from!=pile)
				{ while(from.height()>0) { pile.addChip(from.removeTop()); }
				}
		}
    }
    private void distributeChips(Random r,Zone zone,TammanyCell dist[])
    {	// first pile all the chips on one cell
    	TammanyCell pile = dist[0];
    	makePile(pile,dist);
    	
    	// shuffle the pile
    	pile.shuffle(r);
    	// distribute the shuffled pile to empty wards with the same zone
    	for(TammanyCell c : wardCubes)
    	{	if((c.zone==zone) && (c.topChip()==null))
    		{ 	c.addChip(pile.removeTop());
    		}
    	}
    }
   	
    // true if it's time to break for an election
    private boolean electionTime()
    {
    	return((nextPlayer(whoseTurn)==firstPlayer) 
    			&& ((currentYear%4)==0) 
    			&&  (lastElectionYear<currentYear));
    }			
 
    private void startNewYear()
    {
      	currentYear++;
    	if(currentYear%4 == 1)
    	{	int election = currentYear/4;
    		// distribute ethnics to the new wards
    		if(zone1Init[0].height()>0)
    		{	tammanyHallCubes.addChip(zone1Init[0].removeTop());
    			distributeChips(new Random(randomKey*123),Zone.Zone1,zone1Init);
    		}
    		if((zone2Init[0].height()>0) 
    				&& ((players_in_game>3) || (election>0)))
    		{	distributeChips(new Random(randomKey*352),Zone.Zone2,zone2Init);
   			}
    		if(zone3Init[0].height()>0)
    				{
    				switch(players_in_game)
    				{
    				case 3:	
    					if(election<2) { break; }
						//$FALL-THROUGH$
					case 4:
    					if(election<1) { break; }
						//$FALL-THROUGH$
					case 5:
    					distributeChips(new Random(randomKey*453),Zone.Zone3,zone3Init);
						//$FALL-THROUGH$
					default:
						break;
    				}
    		}
    	  // return any castle gardens cells to the bag
    		makePile(bag,castleGardens);
        }
    	loadCastleGardensWithPlayersPlus2();
    	

    }
    
    private void loadCastleGardensWithPlayersPlus2()
    {
    	bag.shuffle(new Random(randomKey*(currentYear+2325)));
    	// distribute top of the bag to castlegardens
    	if(TammanyCell.isEmpty(castleGardens))
    	{for(int i=0;i<players.length+2;i++)
    	{	if(bag.topChip()!=null) { castleGardens[i].addChip(bag.removeTop()); }
    	}
    	}
    }
    
    private void loadCastleGardensWith4()
    {	// load castle gardens for "free cube" mode
    	makePile(bag,castleGardens);
    	for(int lim = bag.height()-1,tot=TammanyChip.ethnics.length; (lim>=0) && (tot>0);lim--)
    	{	// put one chip of each color, but only if there are some in the bag
    		TammanyChip ch = bag.chipAtIndex(lim);
    		int ind = ch.myInfluenceIndex();
    		TammanyCell c = castleGardens[ind];
    		if(c.topChip()==null) { bag.removeChipAtIndex(lim); c.addChip(ch); tot--; }
    	}
    }
    
    private void startNewPlayer(TammanyPlayer p)
    {
    	reInit(bossPlacement);
    	bossPlacement[0].addChip(p.myBoss);
    	bossPlacement[1].addChip(p.myBoss);
    	
    	slanderPlacement.reInit();
    	if(!p.hasUsedSlander) { slanderPlacement.addChip(TammanyChip.slander); }
    	
    	reInit(lockPlacement);
    	
    	for(int i=0;i<p.locksAvailable;i++)
    	{
    		lockPlacement[i].addChip(TammanyChip.freezer);
    	}
    }
    
    // called to set the board for GUI use
    public void setYearMarker()
    {	for(int i=0; i<yearIndicator.length; i++)
    	{	TammanyCell c = yearIndicator[i];
    		c.reInit();
    		if(i==currentYear-1) { c.addChip(TammanyChip.pawn); }
    	}
    }    
    // called to set the board for GUI use
    public void setScores()
    {	for(TammanyCell c : scoreTop) { c.reInit(); }
    	for(TammanyCell c : scoreSide) { c.reInit(); }
    	int split = scoreTop.length;
    	for(TammanyPlayer p : players)
    	{	int sc = p.score;
    		if(sc>=split)
    		{	int idx = Math.min(scoreSide.length-1,sc-split);
    			scoreSide[idx].addChip(p.myBoss);
    		}
    		else { 
    			scoreTop[sc].addChip(p.myBoss);
    		}
    	}
    }
    public int scoreForPlayer(int p)
    {
    	return(players[p].score);
    }
    public double scoreEstimate(int p)
    {  	TammanyPlayer pl = players[p];
    	return(pl.score + pl.slander.height()+pl.totalInfluence()/2.0+pl.totalWards(wardBosses));
    }
    public void setElectionView()
    {	// copy the election related data from the players to the election view
    	if(board_state.isElection())
    	{	int ward = electionOrder[electionOrderIndex];
    		TammanyCell bosses = wardBosses[ward];
     		TammanyCell cubes = wardCubes[ward];
    		int row = 0;
    		cell.reInit(electionBox);
    		for(TammanyPlayer p : players)
    		{	int count = bosses.countChips(p.myBoss);
    			if(count>0)
    			{	int max = 0;
    				TammanyCell electionRow[] = electionBox[row];
    				int col = 1;
    				p.bossVotes = count;
    				// load the boss chip
    				{TammanyCell n = electionRow[0];
    				 n.addChip(p.myBoss);
    				 n.label = ""+count;
    				 n.boss = p.myBoss;
    				} 
    				// load the influence chips
    				for(int groupn = 0;groupn<TammanyChip.ethnics.length; groupn++)
    				{	TammanyChip ethnic = TammanyChip.ethnics[groupn];
    					if(cubes.containsChip(ethnic))
    					{	TammanyCell n = electionRow[col];
    						n.copyFrom(p.influence[groupn]);
   					   		int siz = n.height();
    						n.rackLocation = TammanyId.ElectionDisc;
    		   				n.boss = p.myBoss;
    		   				 
    		   				if(siz==0) { p.votes[groupn]=0; }
    						max += siz;
    	   					col++;
       					}
    					else { p.votes[groupn]=0; }
    				}
    				// load the ballot box
    				{
    				TammanyCell n = electionRow[col];
    				p.ballotBox = n;
    				p.maxVotes = count+max;
    				p.setBoxVotes();
      				n.boss = p.myBoss;
      				n.addChip(TammanyChip.ballotBox);
    				n.rackLocation = TammanyId.ElectionBox;
     				}
      				
    				row++;
    			}
    		}
    	}
    }
    private void doMove(replayMode replay,TammanyCell from,TammanyMovespec m,TammanyCell to)
    {
    	int ind = m.from_cube;
     	pickObject(from,ind);
       	setNextStateAfterPick();
        m.object = pickedObject;
    	dropObject(to);
       	setNextStateAfterDrop(replay);
   	
    	if(replayMode.Replay!=replay)
    		{
    		animationStack.push(from);
    		animationStack.push(to);
    		}
    }
    public void processVotes(TammanyPlayer p,TammanyMovespec m,replayMode replay)
    {	int ward = electionWard();
    	int boss = wardBosses[ward].countChips(p.myBoss);
		int sum = 0;
    	if(m!=null)
    	{
     	int votes[] = TammanyMovespec.decodeVotes(m.to_row);
     	int wardCode = ((ward<<8) | boss);
     	G.Assert(m.from_row==wardCode,"bosses match");
    	for(int i=0;i<p.influence.length;i++)
    		{ int v = votes[i];
    		  sum+= votes[i];
    		  
    		  while(v-->0) 
    		  	{ TammanyChip top = p.influence[i].removeTop();		// remove the vote tokens he used
    		  	  wasteBasket.addChip(top);
    		  	  if(replay!=replayMode.Replay)
    		  	  {	  
    		  		  animationStack.push(p.influence[i]);
    		  		  animationStack.push(wasteBasket);
    		  	  }
    		  	}
    		}}
     	p.actualVotes = boss+sum;
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	TammanyMovespec m = (TammanyMovespec)mm;
        if(replay!=replayMode.Replay) { animationStack.clear(); gameEvents.clear(); }
   
        switch (m.op)
        {
        case MOVE_DONE:
         	doDone(replay);
            break;
        case MOVE_SLANDER:
        	{
        	TammanyCell from = slanderPlacement;
        	TammanyCell to = getCell(m.dest,m.to_col,m.to_row);
        	doMove(replay,from,m,to);
        	}
        	break;
        case MOVE_CUBE:
        case MOVE_FROM_TO:
        	{
        	TammanyCell from = getCell(m.source,m.from_col,m.from_row);
        	TammanyCell to = getCell(m.dest,m.to_col,m.to_row);
        	
        	doMove(replay,from,m,to);
        	}
        	break;
        case MOVE_DROPB:
        	{

			TammanyChip po = pickedObject;
			TammanyCell dest = getCell(m.source,m.to_col,m.to_row); 
			if(isSource(dest)) { unPickObject(replay); }
			else 
			{
            dropObject(dest);
            /**
             * if the user clicked on a board space without picking anything up,
             * animate a stone moving in from the pool.  For Hex, the "picks" are
             * removed from the game record, so there are never picked stones in
             * single step replays.
             */
            if((replay==replayMode.Single) && (po!=null))
            	{ animationStack.push(getSource());
            	  animationStack.push(dest); 
            	}
           setNextStateAfterDrop(replay);
        	}}
            break;

        case MOVE_PICK:
        case MOVE_PICK_CUBE:
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	// be a temporary p
        	{
        	TammanyCell src = getCell(m.source,m.from_col,m.from_row);
        	// special case - when slandering we need to be able to drop a slander cube
        	// and then either undo it, or proceed by picking up a boss.  In all other cases, picking
        	// from the same pile is an undo
        	boolean isDest = (board_state==TammanyState.SlanderPayment) ? isDest(src,m.from_cube) : isDest(src);
        	if(isDest) { unDropObject(replay); }
        		else
        		{ pickObject(src,m.from_cube);
        		  m.object = pickedObject;
        		}
        	}
        	setNextStateAfterPick();
            break;

        case MOVE_DROP: // drop on chip pool;
        	{
        	TammanyCell dest = getCell(m.source,m.to_col,m.to_row);
        	if(isSource(dest)) { unPickObject(replay); }
        	else { 
        		dropObject(dest);
        		setNextStateAfterDrop(replay);
        		}
        	}
            break;
        case MOVE_VOTE:
        	players[whoseTurn].pendingVote = m;
        	doDone(replay);
        	break;
         case MOVE_START:
         	 if(started || (revision<101))
        	 {
        		 setWhoseTurn(m.player);
        	 }
        	 else 
        	 { started = false; 
        	   setWhoseTurn(firstPlayer);
        	 }
            acceptPlacement();
            unPickObject(replay);
            int nextp = nextPlayer(whoseTurn);
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            if(currentYear==0)
            {	startNewYear();
            }
            if((win[whoseTurn]=WinForPlayerNow(whoseTurn))
               ||(win[nextp]=WinForPlayerNow(nextp)))
               	{ setState(TammanyState.Gameover); 
               	}
            else if(electionMode)
            { setState(SIMULTANEOUS_PLAY ? TammanyState.SimultaneousElection : TammanyState.SerialElection); 
            } 
            else {   setState(TammanyState.Puzzle);	// standardize the current state
            	setNextStateAfterDone(replay); 
            }

            break;
       case MOVE_ELECT:
    	   G.Assert(electionOrderIndex<electionOrder.length,"too many elections");
    	   setState(SIMULTANEOUS_PLAY ? TammanyState.SimultaneousElection : TammanyState.SerialElection);
    	   makePile(bag,castleGardens);	// return any castle gardens cubes to the bag
      	   replenishInfluence();
      	   emptyTrash();
    	   setNewElectionWard(replay);
    	   break;
       case MOVE_RESIGN:
    	   	setState(unresign==null?TammanyState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
        	slanderPlacement.reInit();
        	reInit(bossPlacement);

            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(TammanyState.Puzzle);
 
            break;
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(TammanyState.Gameover);
			break;

        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        if(gameEvents.size()>0) { m.gameEvents = gameEvents.toArray(); gameEvents.clear(); }
        return (true);
    }

    // legal to hit the chip storage area
    public boolean legalToHitChips(TammanyCell c,Hashtable<TammanyCell,TammanyMovespec> targets)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
		case Election:
			return(false);
        case ConfirmPlacementOrSlander:
        case ConfirmPlacement:
        case ConfirmUseRole:
        case ConfirmNewRoles:
        case Play:
		case Resign:
		case Gameover:
		case PlaceCube:
		case DistributeRoles:
		case SimultaneousElection:
		case SerialElection:
		case Play2:
		case Arrest:
		case SlanderPayment:
		case Move:
		case Lock:
		case ConfirmSlander:
		case DoubleSlander:
		case PlayOption:
		case Disc:
		case TakeDisc:
			return((pickedObject!=null)? (targets!=null) && targets.get(c)!=null : isDest(c));
        case Puzzle:
            return (pickedObject==null ? c.topChip()!=null : true);
        }
    }

    public boolean LegalToHitBoard(TammanyCell c,Hashtable<TammanyCell,TammanyMovespec>targets)
    {	if(c==null) { return(false); }
        switch (board_state)
        {
        case Election:
        	return(false);
		case Play:
		case Play2:
		case PlayOption:
		case SimultaneousElection:
		case SerialElection:
		case PlaceCube:
		case TakeDisc:
		case DistributeRoles:
		case Move:
		case Arrest:
		case SlanderPayment:
		case Lock:
		case DoubleSlander:
		case Disc:
		case ConfirmPlacementOrSlander:
			return(targets.get(c)!=null);
			
		case Gameover:
		case Resign:
			return(false);
		case ConfirmPlacement:
		case ConfirmUseRole:
		case ConfirmSlander:
		case ConfirmNewRoles:
			return(isDest(c));
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case Puzzle:
            return (pickedObject==null ? c.topChip()!=null : true);
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(TammanyMovespec m)
    {
        // to undo state transistions is to simple put the original state back.
        
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
    	//G.print("E "+m);
	    if (Execute(m,replayMode.Replay))
        {
        }
        else { throw G.Error("Execute failed for "+m);}
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(TammanyMovespec m)
    {
    	throw G.Error("shouldn't be called");
    }
    
 public Hashtable<TammanyCell,TammanyMovespec>getDests()
 {	Hashtable<TammanyCell,TammanyMovespec> val = new Hashtable<TammanyCell,TammanyMovespec>();
 
 	if(pickedObject!=null)
 	{
 	CommonMoveStack  all = GetListOfMoves();
 	while(all.size()>0)
 		{
 		TammanyMovespec m = (TammanyMovespec)all.pop();
 		switch(m.op)
 		{	default: break;
			case MOVE_SLANDER:
 			case MOVE_CUBE:
 			case MOVE_FROM_TO:
 				TammanyCell c = getCell(m.dest,m.to_col,m.to_row);
 				val.put(c,m);
 		}
 		}
 	}
 
 	return(val);
	 
 }
 
 public Hashtable<TammanyCell,TammanyMovespec>getTargets()
 {
	 return((pickedObject==null)?getSources() : getDests());
 }
 
 public Hashtable<TammanyCell,TammanyMovespec>getSources()
 {	Hashtable<TammanyCell,TammanyMovespec> val = new Hashtable<TammanyCell,TammanyMovespec>();
 
 	if(pickedObject==null)
 	{
 	CommonMoveStack  all = GetListOfMoves();
 	while(all.size()>0)
 		{
 		TammanyMovespec m = (TammanyMovespec)all.pop();
 		switch(m.op)
 		{	default: break;
 			case MOVE_SLANDER:
 				val.put(slanderPlacement,m);
 				break;
  			case MOVE_CUBE:
 			case MOVE_FROM_TO:
 				TammanyCell c = getCell(m.source,m.from_col,m.from_row);
 				val.put(c,m);
 		}
 		}
 	}
 
 	return(val);
	 
 }
 
 //
 // set robotEthnicControl for all players
 //
 private void robotGuessEthnicControl()
 {	for(TammanyCell c : robotBosses) { c.reInit(); }
	for(TammanyPlayer p : players)
	{	// generate a fake list of boss cells, corresponding to the predicted control
		for(int lim = wardBosses.length-1; lim>=0; lim--)
	 	{	TammanyCell dest = wardBosses[lim];
	 		TammanyCell cubes = wardCubes[lim];
	 		TammanyCell fake = robotBosses[lim];
			int dif = voteDifferential(p,dest,cubes,0,null,true);
			if(dif>0) 
			{	G.Assert(fake.height()==0,"only one owner");
				fake.addChip(p.myBoss);
			}
	 	}
		p.collectWardStatistics(robotBosses,wardCubes);	
	}
	// now all the players ethnic cubes counters are set to the estimated control
	for(TammanyPlayer p : players) { p.setRobotEthnicControl(players); }

 }
 
 private double ethnicBalanceChange(TammanyPlayer pl,TammanyCell dest,TammanyCell cubes,TammanyChip boss)
 {	double ev = 1.0;
 	if(robotBosses[dest.row].topChip()!=boss)	// we do not already own it.
 	{
 		// if we currently have no influence
 		int newdif = voteDifferential(pl,dest,cubes,1,null,true);
 		if(newdif>0)
 		{  
 		// and we will own it.
 		double improvement = 0;
 		int capture = 0;
 	 	int ethnicControl[] = new int[TammanyChip.ethnics.length];
 	 	int robotEthnicControl[] = pl.robotEthnicControl;
 	 	pl.collectCubeStatistics(cubes,ethnicControl);
 	 	for(int lim=ethnicControl.length-1; lim>=0; lim--)
 	 	{	int step = ethnicControl[lim];
 	 		if(step>0)
 	 		{
 	 		int basis = -robotEthnicControl[lim];
 	 		int sum = step+robotEthnicControl[lim];
 	 		if(sum>=0) { capture++; }
 	 		if(basis>0) { improvement += (double)step/basis; }
 	 		}}
 	 	ev += improvement;
 	 	ev += capture;
 		}
 	}
	return(ev);
 }
 private TammanyChip canSlanderVoter(TammanyChip bosschip,TammanyCell boss)
 {	int count = 0;
 	TammanyChip b=null;
 	for(int lim = boss.height()-1; lim>=0; lim--)
 	{	TammanyChip ch = boss.chipAtIndex(lim);
 		if(ch.isBoss())
 		{	if(ch==bosschip) { return(null); }	// no need to add a boss
 			count++;
 			b = ch;
 		}
 	}
 	return((count==1) ? b : null);
 }
 private boolean isSlanderYear()
 {
	 return(((currentYear>=8) && ((currentYear%4)==0)));
 }

 private void addBossPlacementMoves(CommonMoveStack all, int who, TammanyCell src,TammanyChip ch)
 {	 TammanyPlayer myPlayer = players[who];
 	 boolean expander = false;
 	 switch(evaluator)
 	 {
 	 case Expander:
 	 	{
 		if(hasVacantWard()) { expander = true; } 
 	 	}
		break;
	default: break;
 	 }
	 for(TammanyCell dest : wardBosses)
	 {	TammanyChip top = wardCubes[dest.row].topChip();
	 	if((top!=null) && (top!=TammanyChip.freezer))	// there must be a cube to make the ward active
	 	{double ev = 1.0;
	 	 if(robotBoard)
	 	 {
	 		 if(expander && (dest.height()==0)) 
	 		 	{ ev *= 2.0; 
	 		 	}
	 		 switch(evaluator)
		 	 {
		 	 case CubeCounter:
		 	 	{
		 		ev *= ethnicBalanceChange(myPlayer,dest,wardCubes[dest.row],ch); 
		 	 	}
				//$FALL-THROUGH$
		 	 case Basher:
		 	 case LessBasher:
		 	 case Voter:
		 	 case BashVoter:
		 	 case LiteVoter:
		 	 case EducatedVoter:
		 	 case SlanderVoter:
		 	 case QuarterlyVoter:
		 	 case Expander:
		 	 case WeightedVoter:
		 	 case Contender:
		 	 	{
		 		int dif = voteDifferential(myPlayer,dest,wardCubes[dest.row],1,null,false);	
		 		if(dif>0)
		 		{
		 		if(dest.containsChip(players[who].myBoss))
		 		{
		 			ev /= dif;	// discourage piling on
		 		}
		 		else
		 		{	switch(evaluator)
		 			{
		 			case BashVoter:
		 				if((currentYear<=4) && !dest.containsChip(myPlayer.myBoss))
		 				{
		 				TammanyChip currentWinner = dest.otherBossSingle(myPlayer.myBoss);
		 				if(currentWinner!=null)
		 				{
		 				double difs = currentLeader.xscore-currentLoser.xscore;
		 				double part = G.interpolateD((getPlayerForBoss(currentWinner).xscore-currentLoser.xscore)/difs,0,difs);
		 				ev *= 5*part;
		 				break;
		 				}}
		 			//$FALL-THROUGH$
				default:
			 			ev *= (dif+1);	// reward if were getting in a fight to win		 				
		 			}
		 		}
		 		}
		 		else if(!isSlanderYear())	// if it's not a slander year 
		 		{	// punish adding where we're still not winning
		 			switch(evaluator)
		 			{
		 			case BashVoter:
		 				if((currentYear<=4) && !dest.containsChip(myPlayer.myBoss))
		 				{
		 				TammanyChip currentWinner = dest.otherBossSingle(myPlayer.myBoss);
		 				
		 				if(currentWinner!=null)
		 				{
		 				double difs = currentLeader.xscore-currentLoser.xscore;
		 				double part = G.interpolateD((getPlayerForBoss(currentWinner).xscore-currentLoser.xscore)/difs,0,difs);
		 				ev *= 5*part;
		 				break;
		 				}}
						//$FALL-THROUGH$
					default:
		 			ev /= -(dif-1); 	// penalize if we don't
		 			}
		 		}
		 		else	// it's a slander year, consider we can slander him 
		 		{	switch(evaluator)
		 			{
		 			case Expander:
		 			case QuarterlyVoter:
		 			case LiteVoter:
		 			case BashVoter:
				 	case EducatedVoter:
				 	case SlanderVoter:
			 			TammanyChip slander = canSlanderVoter(ch,dest);
			 			if(slander!=null) { 
			 				TammanyPlayer slanderBoss = getPlayerForBoss(slander);
			 				double difs = currentLeader.xscore-currentLoser.xscore;
			 				double part = G.interpolateD((slanderBoss.xscore-currentLoser.xscore)/difs,0,difs);
			 				double sl = (evaluator==TammanyPlay.Evaluator.SlanderVoter) ? Math.sqrt(wardCubes[dest.row].height()) : 1.0;
			 				ev += sl*10*part/difs;
			 			}
			 			else 
			 			{
			 				ev /= -(dif-1);
			 			}	
		 				break;
		 			default: 
		 				ev /= -(dif-1);
		 				;
		 			}
		 		}
		 	 	}
		 	 	break;
		 	 case Preferred:	
		 	 	{
	 	 		TammanyCell cubes = wardCubes[dest.row];
	 	 		ev *= cubes.height();		// encourage fighting for more cubes
	 	 		ev /= (dest.height()+1);	// punish piling on more bosses
		 	 	}
		 		 break;
		 	 default: break;
		 	 }}
		 all.push(new TammanyMovespec(MOVE_FROM_TO,ev,src,dest,who));
	 	}
	 }
 }
 private int votesAvailable(TammanyPlayer p,TammanyCell bosses,TammanyCell cubes,int bossVotes,TammanyChip chipAdded)
 {
	 int nvotes = bossVotes+bosses.countChips(p.myBoss);
	 if(nvotes>0)
		 {for(int lim = p.influence.length-1; lim>=0; lim--)
		 	{	int inf = p.influence[lim].height();
		 		TammanyChip thisChip = TammanyChip.ethnics[lim];
		 	if((inf>0) 
		 			&& ((chipAdded==thisChip) || cubes.containsChip(thisChip)))
		 		{ nvotes += inf; 
		 		}
		 	}
		 }
	 return(nvotes);
 }
 

 private int voteDifferential(TammanyPlayer p,TammanyCell bosses,TammanyCell cubes,int bossVotes,TammanyChip chipAdded,boolean truecount)
 {
	 int myVotes = votesAvailable(p,bosses,cubes,bossVotes,chipAdded);
	 int oVotes = 0;
	 for(TammanyPlayer other : players) 
	 {
		 if(other!=p) 
		 { int votes = votesAvailable(other,bosses,cubes,0,chipAdded);
		   switch(evaluator)
		   {
		   case Basher:
		   case CubeCounter:
		   case SlanderVoter:
		   case EducatedVoter:
		   case LiteVoter:
		   case BashVoter:
		   case QuarterlyVoter:
		   case Expander:
		   case Voter:
		   case WeightedVoter:
		   case LessBasher:
			   if(!truecount && (players[whoseTurn].score<currentLeader.score) && (other.score==currentLeader.score)) 
			   {  votes = Math.max(0,votes-2); 
			   }
			break;
		default:
			   break;
		   }
		   oVotes = Math.max(oVotes,votes); 
		 }
	 }
	 switch(evaluator)
	 {
 	 case SlanderVoter:
 	 case LessBasher:
	 case CubeCounter:
 	 case LiteVoter:
	 case BashVoter:
 	 case EducatedVoter:
 	 case QuarterlyVoter:
	 case Expander:
	 case Voter:
	 case Basher: 
		 if(!truecount && (oVotes==0)) { return(5); }
		//$FALL-THROUGH$
	default: return(myVotes-oVotes);
	 }
 }
 // add cube placements from castle gardens
 private boolean addCastleCubePlacementMoves(CommonMoveStack all, double balance,int who, TammanyCell src,TammanyChip ch)
 {	boolean some = false;
	 for(TammanyCell dest : wardCubes)
	 {	TammanyChip top = dest.topChip();
	    if((top!=null)&&(top!=TammanyChip.freezer))
	 	{// there has to already be a cube in the ward, all active wards have cubes.
	    double ev = balance;
	    if(robotBoard)
	    {	TammanyCell bosses = wardBosses[dest.row];
	    	
	      	if(bosses.height()>0) 
	      	{	      
	    	switch(evaluator)
	    	{
	    	case LessBasher:
	    	case Voter:
	    	case LiteVoter:
	    	case BashVoter:
	    	case EducatedVoter:
	    	case SlanderVoter:
	    	case QuarterlyVoter:
	    	case Expander:
	    	case WeightedVoter:
	    	case Basher:
	    	case CubeCounter:
	    	case Contender:
	    		{
	    		int dif = voteDifferential(players[who],bosses,dest,0,ch,false);	// adding a cube
    			if(dif>0)
    				{ev*=(dif+1); } 	// reward if we already have an advantage
    				else 
    				{ 
  						ev /= -(dif-1); 	// penalize if we don't  				
    				}
	    		}
	    		break;
	    	case Preferred: 
	    		
	    		{ 
	    		  TammanyChip myBoss = players[who].myBoss;
	    		  if(!bosses.containsChip(myBoss))
	    		  {
	    			ev /= 10;  	// discourage placement where we have no boss
	    		  }
	    		}
	    		break;
	    	default: break;
	    	}}
	      	else { ev = 0; }	// no bosses at all
	    }
		 all.push(new TammanyMovespec(MOVE_FROM_TO,ev,src,dest,who));
		 some = true;
	 	}
	 }
	 return(some);
 }
 private void addBossPlacementMoves(CommonMoveStack  all,int who)
 {	switch(evaluator)
	 {
	 case CubeCounter:
		 // prepare for ethnic control calculations
		 robotGuessEthnicControl();
		 break;
	 default: 
	 }
	if(pickedObject!=null)
	{
		if(pickedObject.isBoss())
		{
			addBossPlacementMoves(all,who,getSource(),pickedObject);
		}
	}
	else
	{
	for(TammanyCell s : bossPlacement)
	{	TammanyChip top = s.topChip();
		if(top!=null)
		{
			addBossPlacementMoves(all,who,s,s.topChip());
			if(robotBoard) break;	// bot only needs 1
		}
	}
	TammanyCell src = bossPlacement[0];
 	if(src.topChip()==null) { src = bossPlacement[1]; }
 	
	}
 }
 
 private int countColors(TammanyCell cells[])
 {
	 int n = 0;
	 int mask = 0;
	 for(TammanyCell c : cells)
	 {
		 TammanyChip top = c.topChip();
		 if(top!=null)
		 {
			int bit = 1<<top.myInfluenceIndex();
			if((bit&mask)==0) { mask |= bit; n++; }
		 }
	 }
	 return(n);
 }
 private boolean hasVacantWard()
 {
	 for(int lim = wardBosses.length-1; lim>=0; lim--)
	 {
		 if((wardCubes[lim].height()>0) && (wardBosses[lim].height()==0)) { return(true); }
	 }
	 return(false);
 }
 // add moves from castle gardens to the board
 private boolean addCastleCubePlacementMoves(CommonMoveStack  all,int who)
 {	int colorSet = 0;
 	boolean some = false;
 	double balance = 1.0*countColors(castleGardens);
 	switch(evaluator)
 	{
 	case Expander:
 		{
 		if(hasVacantWard()) { balance *= 0.5; }	// downgrade cubes if wards are still available.
 		}
 		break;
 	default: break;
 	}
 	if((pickedObject!=null) )
 	{	if(pickedObject.isCube())
 		{
 		return(addCastleCubePlacementMoves(all,balance,who,getSource(),pickedObject));
 		}
 	}
 	else
 	{
 	for(TammanyCell src : castleGardens)
 	{
 		TammanyChip top = src.topChip();
 		if(top!=null)
 		{
 			int colorBit = (1<<top.myCube.ordinal());
 			if((colorSet & colorBit)==0)
 			{
 				if(robotBoard) { colorSet |= colorBit; }
 				some |= addCastleCubePlacementMoves(all,balance,who,src,top);
 			}
 		}
 	}
 	}
 	return(some);
 }
 
 private void addDiscPlacementMoves(CommonMoveStack  all,int who)
 {	TammanyPlayer p = players[who];
 	 if(pickedObject!=null)
 	 {
 		 if(pickedObject.isInfluence())
 		 {
 	     TammanyCell c = getSource();
 		 int ind = pickedObject.myInfluenceIndex();
		 all.push(new TammanyMovespec(MOVE_FROM_TO,1.0,c,p.influence[ind],who));
 		 }
 		 
 	 }
 	else 
 	 {for(TammanyCell c : influence)
	 {	TammanyChip top = c.topChip();
	 	if(top!=null)
	 		{int ind = top.myInfluenceIndex();
	 		 all.push(new TammanyMovespec(MOVE_FROM_TO,1.0,c,p.influence[ind],who));
	 		}
	 }}
 }
 private int deployableVotes(TammanyPlayer p,TammanyCell bosses,TammanyCell cubes)
 {
	 int boss = bosses.countChips(p.myBoss);
	 if(boss>0)
	 {	for(int lim = TammanyChip.ethnics.length-1; lim>=0; lim--)
	 	{	if(cubes.containsChip(TammanyChip.ethnics[lim]))
	 		{ boss += p.influence[lim].height();
	 		}
	 	}
	 }
	 return(boss);
 }
 private int maxOtherDeployableVotes(TammanyPlayer p,TammanyCell bosses,TammanyCell cubes)
 {	int m = 0;
 	for(TammanyPlayer other : players)
 	{
 		if(other!=p) { m = Math.max(m,deployableVotes(other,bosses,cubes)); }
 	}
	return(m);
 }
 private boolean addVoteMoves(CommonMoveStack  all,int who)
 {	int ward = electionWard();
 	TammanyCell bosses = wardBosses[ward];
 	TammanyCell cubes = wardCubes[ward];
 	TammanyPlayer p = players[who];
 	int nBosses = bosses.countChips(p.myBoss);
 	int minRequired = 0;
 	boolean smartVote = false;
 	double cubeWeight = 0.0;
 	double base_value = 1.0;
 	
 	switch(evaluator)
 	{
 	case WeightedVoter: 
 		cubeWeight = Math.sqrt(cubes.height());
		//$FALL-THROUGH$
	case QuarterlyVoter:
 	case SlanderVoter:
 	case EducatedVoter:
 	case BashVoter:
 	case Expander:
 	case LiteVoter:
 	case Voter: smartVote = true;
 		break;
 	default: smartVote = false;
 	}

 	for(TammanyPlayer other : players)
 		{	if(other!=p) { minRequired = Math.max(minRequired,bosses.countChips(other.myBoss)-nBosses+1); }
 		}
 	boolean some = false;
 	if(nBosses>0)
 	{	// if we're allowed to vote
 	 	int maxOther = 1+maxOtherDeployableVotes(p,bosses,cubes);
  		int allowedIrish = cubes.containsChip(TammanyChip.ethnics[0]) ? p.influence[0].height() : 0;
  		int allowedEnglish = cubes.containsChip(TammanyChip.ethnics[1]) ? p.influence[1].height() : 0;
 		int allowedGerman = cubes.containsChip(TammanyChip.ethnics[2]) ? p.influence[2].height() : 0;
 		int allowedItalian = cubes.containsChip(TammanyChip.ethnics[3]) ? p.influence[3].height() : 0;
 		int maxVotes = 1+allowedIrish+allowedEnglish+allowedGerman+allowedItalian+nBosses;
 		
 		if(all==null) { return(maxVotes>(nBosses+1)); }	// can vote, just we shouldn't
 		if(maxVotes>=minRequired)
 		{
 		int nWays[] = new int[maxVotes-minRequired+1];
 		if(maxVotes>=maxBossVotes)	// can influence the election
 		{
 		for(int irish=0;irish<=allowedIrish;irish++)
 		{	for(int english=0; english<=allowedEnglish;english++)
 			{
	 			for(int german=0;german<=allowedGerman;german++)
	 			{
	 				for(int italian=0;italian<=allowedItalian;italian++)
	 				{	
	 					if(italian>0 || german>0 || english>0 || irish>0)
	 					{
	 					// a vote that commits at least one chip
	 					int vtotal = irish+english+german+italian+nBosses;
	 					if((vtotal>minRequired) && (vtotal<=maxOther))	// don't waste votes
	 					{	
	 					some = true;
	 					// vtotal is temporary
	 					nWays[vtotal-minRequired]++;	// count how many use this total
	 					all.push(new TammanyMovespec(MOVE_VOTE,smartVote?vtotal : base_value,ward,nBosses,irish,english,german,italian,who));
	 					}}
	 				}
	 			}
 			}
 		}
 	 	if(smartVote)
 	 	{
 	 		for(int lim=all.size()-1; lim>=0; lim--)
 	 		{
 	 			TammanyMovespec m = (TammanyMovespec)all.elementAt(lim);
 	 			int votes = (int)(m.montecarloWeight+0.01);
 	 			int dif = votes-minRequired;
 	 			switch(evaluator)
 	 			{
 	 			default:
 	 				m.montecarloWeight = cubeWeight*dif+nWays[dif];
 	 				break;
 	 			case LiteVoter:
 	 				m.montecarloWeight = cubeWeight*dif+Math.sqrt(nWays[dif]);
 	 				break;
 	 			}
 	 		}
 	 		base_value = 0.5;
 	 	}
 		} 		
 	}
 	}
 	if(all==null) { return(some); }
 	// add the null vote
 	all.push(new TammanyMovespec(MOVE_VOTE,base_value,ward,nBosses,0,0,0,0,who));
 	return(true);
 }
 
 // true if player has votes more than just empty
 private boolean playerCanVote(TammanyPlayer p)
 {
	 return(addVoteMoves(null,p.myIndex));
 }
 public boolean playerCanVote(int p)
 {
	 return(playerCanVote(players[p]));
 }
 public int bossVotes(int pl)
 {	int ward = electionWard();
	 return((ward<<8) | wardBosses[ward].countChips(players[pl].myBoss));
 }
 private void addRoleAssignmentMoves(CommonMoveStack  all,TammanyCell from, int who)
 {
	 for(TammanyCell role : roleCells)
	 {
		 if(role.topChip()==null)
		 {
			 all.push(new TammanyMovespec(MOVE_FROM_TO,1.0,from,role,who));
		 }
	 }
 }
 private void addRoleAssignmentMoves(CommonMoveStack  all,int who)
 {	if((pickedObject!=null)&&(pickedObject.isBoss()))
 	{
 		addRoleAssignmentMoves(all,getSource(),who);
 	}
 	else
	for(TammanyCell boss : bossPlacement)
	 {
		 TammanyChip top = boss.topChip();
		 if(top!=null)
		 {
			 addRoleAssignmentMoves(all,boss,who);
		 }
	 }
 }
 private boolean addArrestMoves(CommonMoveStack  all,int who)
 {	
	 if(pickedObject!=null)
	 {
		 if(pickedObject.isCube())
		 {	if(all!=null)
			 {all.push(new TammanyMovespec(MOVE_CUBE,1.0,getSource(),pickedIndexStack.top(),bag,who));
			 }
			 return(true);
		 }
		 return(false);
	 }
	 else
	 {
	 boolean some = false;
	 	for(int lim = wardCubes.length-1; lim>=0; lim--)
	 	{
		 TammanyCell c = wardCubes[lim];
		 int mask = 0;
		 int h = c.height();
		 if((c.topChip()!=TammanyChip.freezer) && (h>=2))
		 {	
	     while (h-- > 1)
		 	{
			TammanyChip ch = c.chipAtIndex(h);
			if(ch!=TammanyChip.freezer)
			{
			int maskBit = 1<<ch.myInfluenceIndex();
			if((mask&maskBit)==0)
			{
			if(all==null) { return(true); }
			if(!robotBoard) { mask |= maskBit; }
			some = true;
			all.push(new TammanyMovespec(MOVE_CUBE,1.0,c,h,bag,who)); 
		 	}}
		 	}
		 }
 	}
 	return(some);
	 }
 }
 private boolean hasArrestMoves()
 {
	 return(addArrestMoves(null,whoseTurn));
 }
 
 private boolean hasRelocateMoves()
 {
	 return(addRelocateMoves(null,whoseTurn));
 }
 private boolean addRelocateMoves(CommonMoveStack  all,TammanyCell c,TammanyChip ch,int h,int who)
 {	boolean some = false;
 	for(int lim=c.nAdjacentCells()-1; lim>=0; lim--)
 	{	TammanyCell dest = c.exitTo(lim);
 		if((dest.height()>0) && (dest.topChip()!=TammanyChip.freezer))
			{
			if(all==null) { return(true); }
			some = true;
			all.push(new TammanyMovespec(MOVE_CUBE,1.0,c,h,dest,who)); 
			}
	 	}
	 return(some);
	}

 private boolean addRelocateMoves(CommonMoveStack  all,int who)
 {	boolean some = false;
	if(pickedObject!=null)
	{	if(pickedObject.isCube())
		{
		some |= addRelocateMoves(all,getSource(),pickedObject,pickedIndexStack.top(),who);
		if(some&&(all==null)) { return(true); }
		}
	}
	else 
	{for(int lim = wardCubes.length-1; lim>=0; lim--)
 	{
	 TammanyCell c = wardCubes[lim];
	 int mask = 0;
	 int h = c.height();
	 if((c.topChip()!=TammanyChip.freezer) && (h>=2))
	 { while(h-- > 1)
	 	{
		TammanyChip ch = c.chipAtIndex(h);
		if(ch!=TammanyChip.freezer)	// there can be 2
		{
		int maskBit = 1<<ch.myInfluenceIndex();
		if((mask&maskBit)==0)
		{
		if(!robotBoard) { mask |= maskBit; }
		some |= addRelocateMoves(all,c,ch,h,who);
		if(some && (all==null)) { return(some); }
		}}
	 	}}
 	}}
	return(some);
 }
 
 private void addLockMoves(CommonMoveStack all,TammanyCell from,int who)
 {	TammanyChip myBoss = players[who].myBoss;
 	for(int lim=wardBosses.length-1; lim>=0; lim--)
 	{	TammanyCell c = wardCubes[lim];
 		TammanyChip top = c.topChip();
 		TammanyCell to = wardBosses[lim];
 		if((top!=null)
 				&& (!robotBoard || (to.height()>0))
 				&& (top!=TammanyChip.freezer))
 		{	double progress = 1+((currentYear-1)%4);
 			if(!to.containsChip(myBoss)) { progress/= 5; }
 			int dif = voteDifferential(players[who],to,c,0,null,false);
 			if(dif>0) { progress *= dif+1; }
 			else if(dif<0) { progress /= -(dif-1); }
 			all.push(new TammanyMovespec(MOVE_FROM_TO,progress,from,c,who));
 		}
 	} 
 }
 private void addLockMoves(CommonMoveStack  all,int who)
 {	if(pickedObject!=null)
 	{
	 if(pickedObject==TammanyChip.freezer)
	 {
		 addLockMoves(all,getSource(),who);
	 }
 	}
 	else
 	{
 		for(int lim = lockPlacement.length-1; lim>=0; lim--)
 		{	TammanyCell c = lockPlacement[lim];
 			if(c.topChip()!=null)
 			{
 				addLockMoves(all,c,who);
 				if(robotBoard) { break; }
 			}
 		}
 	}
 }
 private boolean hasLockMoves()
 {
	 for(TammanyCell c : lockPlacement) { if (c.topChip()!=null) { return(true); }}
	 return(false);
 }

 private boolean addDoubleSlanderMoves(CommonMoveStack  all,TammanyCell boss,TammanyCell cubes,int who)
 {		
	 if((pickedObject!=null)&&pickedObject.isBoss())
	 {
		 return(addSlanderDiscardMove(all,getSource(),pickedIndexStack.top(),who,null));
	 }
	 else
	 {	TammanyPlayer p =players[who];
 		boolean some = false;
 		int h = boss.height();
		int slandered = 0;
		while(h-- >= 1)
			{	TammanyChip other = boss.chipAtIndex(h);
				if(other!=TammanyChip.slander)
				{
				int bitmask = 1<<other.myBossIndex();
				if((other!=p.myBoss)	// don't slander ourselves
					&& ((slandered&bitmask)==0)
					&& ((revision<104) || (other==slanderedBoss))		// same color as first boss
					&& p.hasMatchingDisc(cubes))	// but we have to pay 
				{
				if(all==null) { return(true); }
				if(robotBoard) { slandered |= bitmask; }
				some = true;
				addSlanderDiscardMove(all,boss,h,who,currentLeader);
				}}
			}
		return(some);
	 }
 }
 boolean isEarlierPlayer(TammanyPlayer victim,int who)
 {	int next = victim.myIndex;
	 while( (next = nextPlayer(next)) != firstPlayer)	{ if(next==who) { return(true); }}
	 return(false);
 }
 private boolean addStartSlanderMoves(CommonMoveStack  all,int who)
 {	TammanyPlayer p =players[who];
	if(robotBoard && !isSlanderYear()) { return(false); }
 	boolean some = false;
 	for(int lim = wardBosses.length-1; lim>=0; lim--)
 	{	TammanyCell boss = wardBosses[lim];
  		TammanyCell cubes = wardCubes[lim];
 		TammanyChip top = cubes.topChip();
 		if(	boss.containsChip(p.myBoss)			// we have a presence
 			&& boss.hasOtherBosses(p.myBoss)	// someone else has a presence
 			&& (top!=null) 	// there are cubes
 			&& (top!=TammanyChip.freezer)	// not locked
 			&& (p.hasMatchingDisc(cubes))
 			)
 		{	if(all==null) { return(true); }
 			double weight = cubes.height();							// bonus for slandering for more cubes
 			switch(evaluator)
 			{
 			default: break;
 			case QuarterlyVoter:
 			case EducatedVoter:
 			case LiteVoter:
 			case BashVoter:
 			case SlanderVoter:
			case Expander:
 			{
	 			TammanyChip victim = boss.otherBossSingle(p.myBoss);	// victim we can slander
	 			if(victim!=null) 
	 				{ TammanyPlayer victimPlayer = getPlayerForBoss(victim);
	 				  double difs = currentLeader.xscore-currentLoser.xscore;
	 				  double bonus = 1.0+10*G.interpolateD((victimPlayer.xscore-currentLoser.xscore)/difs,0,difs)/difs;
	 				  weight += bonus * ((evaluator==TammanyPlay.Evaluator.SlanderVoter) ? Math.sqrt(cubes.height()) : 1.0);
	 				  if((victimPlayer.hasUsedSlander)
	 						  || isEarlierPlayer(victimPlayer,who))
	 				  {	  // bonus for slandering someone who can't retaliate
	 					  weight *= 2.0; 
	 				  }
	 				}
	 				else 
	 				{ weight = 0; }	// no point in slandering when there's more than one
	 			 break;
 			}
  			}
 			some = true;
 			all.push(new TammanyMovespec(MOVE_SLANDER,weight,boss,who));
 		}
 	} 
 	return(some);
 }
 
 private boolean hasSlanderMoves()
 {
	 return(addSlanderMoves(null,whoseTurn));
 }
 private boolean addSlanderMoves(CommonMoveStack  all,int who)
 {
	 if((pickedObject==TammanyChip.slander)
			 || (slanderPlacement.topChip()!=null)) 
	 	{ return(addStartSlanderMoves(all,who)); 
	 	}
	 return(false);
 }
 private boolean addSlanderDiscardMove(CommonMoveStack  all,TammanyCell from,int lvl,int who,TammanyPlayer leader)
 {	if(all==null) { return(true); }
 	double ev = 1.0;
 	if(robotBoard && (leader!=null))
 	{
 	switch(evaluator)
 	{
 	case Basher:
 	case CubeCounter:
 	case LessBasher:
 	case Voter:
 	case QuarterlyVoter:
 	case SlanderVoter:
	case EducatedVoter:
	case BashVoter:
	case LiteVoter:
 	case Expander:
 	case WeightedVoter:
 		{
 		TammanyChip boss = from.chipAtIndex(lvl);
 		// also used to select slander chips
 		TammanyPlayer bossPlayer = boss.isBoss() ? getPlayerForBoss(boss) : null; 
 		// deploy slander against the leader
 		if((bossPlayer!=null)
 				&& (players[who].xscore<leader.xscore)
 				&& (bossPlayer.xscore==leader.xscore)) 
 					{
 					ev *= 3; 
 					}
 				else {
 					ev = 0;
 				}
 			
 		}
		break;
	default:
		break;
 	}}
    all.push(new TammanyMovespec(MOVE_CUBE,ev,from,lvl,wasteBasket,who));
    return(true);
 }
 private void addSlanderPaymentMoves(CommonMoveStack  all,int who)
 {	if(pickedObject!=null)
 	{
	 addSlanderDiscardMove(all,getSource(),pickedIndexStack.top(),who,null);
 	}
 	else 
 	{	
 		TammanyPlayer p = players[who];
 		// if we haven't placed anything yet, we need both a chip and a boss
 		
 		if(slanderedBoss==null)
 		{	int mask = 0;
 			TammanyCell ward = wardBosses[slanderWard];
 			G.Assert(ward.topChip()==TammanyChip.slander,"slander in progress");
 			for(int lim = ward.height()-2;lim>=0;lim--)
 			{
 				TammanyChip ch = ward.chipAtIndex(lim);
 				if((ch!=p.myBoss) && (ch!=TammanyChip.slander))
 				{	// can slander other bosses, not outselves, and not slander chips
 					int maskBit = 1<<ch.myBossIndex();
 					if((maskBit&mask)==0)
 					{	if(robotBoard) { mask |= maskBit; }
 						addSlanderDiscardMove(all,ward,lim,who,currentLeader);
 					}
 				}
 			}
 		}
 		if(slanderPayment==null)
 		{
 			// can pay with chips from any color in the ward
 			for(int ind = 0;ind<p.influence.length;ind++)
 			{
 				if(p.influence[ind].height()>0)
 				{
 					if(wardCubes[slanderWard].containsChip(TammanyChip.ethnics[ind]))
 					{	// take chips from the display area, not the players rack
 						addSlanderDiscardMove(all,influence[ind],-1,who,null);
 					}
 				}
 			}
 		}
 		
 	}
 }
 private boolean addDoubleSlanderMoves(CommonMoveStack  all,int who)
 {	 
	 if(pickedObject!=null)
	 {
		 return(addSlanderDiscardMove(all,getSource(),pickedIndexStack.top(),who,null));
	 }
	 else
	 {
	 boolean some = false;
	 TammanyPlayer p = players[who];
	 TammanyCell cubes = wardCubes[slanderWard];
	 int influence = slanderPayment.myInfluenceIndex();
	 TammanyChip ethnicGroup = TammanyChip.ethnics[influence];

	 if(p.influence[influence].height()>=3)	// we need 2 more, and 1 is already committed to the first slander 
	 {
	 for(int lim = cubes.nAdjacentCells()-1; lim>=0; lim--)
	 {	TammanyCell c = cubes.exitTo(lim);
	 	TammanyCell cb = wardBosses[c.row];
	 	TammanyChip top = c.topChip();
		 if(	 cb.hasOtherBosses(p.myBoss)
				 && c.containsChip(ethnicGroup)
				 && (top!=TammanyChip.freezer))
		 {
			 // there must be something to slander
			 some |= addDoubleSlanderMoves(all,cb,c,who);
			 if(some && (all==null)) { return(true); }
		 }
	 }}
	 return(some);
	 }
 }
 // true if double slander moves are available.  Called when
 // a single slander has been completed.
 private boolean hasDoubleSlanderMoves()
 {
	 return(addDoubleSlanderMoves(null, whoseTurn));
 }
 
 CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = new CommonMoveStack();
 	int who = whoseTurn;
 	setLeaders();
 	switch(board_state)
 	{
 	default:  	throw G.Error("move generation not implemented for "+board_state);
 	case DoubleSlander:
 		addDoubleSlanderMoves(all,who);
 		all.push(new TammanyMovespec(MOVE_DONE,who));
 		break;
 		
 	case Lock:
 		addLockMoves(all,who);
 		all.push(new TammanyMovespec(MOVE_DONE,who));
 		break;
 		
 	case SlanderPayment:
 		addSlanderPaymentMoves(all,who);
 		break;
 		
 	case Move:
 		addRelocateMoves(all,who);
 		break;

 	case Arrest:
 		addArrestMoves(all,who);
 		break;
 		
 	case DistributeRoles:
 		addRoleAssignmentMoves(all,who);
 		break;
 	case Election:
 		all.push(new TammanyMovespec(MOVE_ELECT,nextElectionWard(),who));
 		break;
 	case SerialElection:
 	case SimultaneousElection:
 		addVoteMoves(all,who);
 		break;
 	case ConfirmPlacementOrSlander:
 		addSlanderMoves(all,who);
		//$FALL-THROUGH$
	case ConfirmPlacement:
 	case ConfirmUseRole:
 	case ConfirmNewRoles:
 	case ConfirmSlander:
 	case Resign:
 			all.push(new TammanyMovespec(MOVE_DONE,who));
 			break;

 	case Puzzle: 
 	case Gameover:
 		break;

 	case Disc:
 	case TakeDisc:
 		addDiscPlacementMoves(all,who);
 		break;
 	case PlaceCube:
 		addCastleCubePlacementMoves(all,who);
 		break;
 	case PlayOption:
 		addCastleCubePlacementMoves(all,who);
		//$FALL-THROUGH$
	case Play:	// place a boss
 	case Play2:
 		addBossPlacementMoves(all,who);
 		break;
 		
 	}
 	return(all);
 }
	void logGameEvent(String str,String... args)
	{	if(!robotBoard)
		{String trans = s.get(str,args);
		 gameEvents.push(trans);
		}

	}
}
