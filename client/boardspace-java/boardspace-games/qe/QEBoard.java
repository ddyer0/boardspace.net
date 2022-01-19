package qe;

import static qe.QEmovespec.*;

import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;

/**
 * QEBoard knows all about the game of Hex, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
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
 class QEPlayer implements QEConstants
 {	int index;
	 QEChip flag;
	 QECell industry = null;
	 QECell tilesWon = null;
	 QECell whiteBoard = null;
	 QECell noQE = null;
	 public boolean publicCensoring = true;
	 public boolean hiddenCensoring = false;
	 public long knownSpending[] = null;		// what each player knows about the spending of other players
	 LStack knownBids = new LStack();	// any bids (including winning) we know
	 LStack winningBids = new LStack();	// winning bids we know
	 long moneySpent = 0;
	 int usedNoQE = 0;
	 String scoreDescription="";
	 public void addToScoreDescription(String message)
	 {
		 scoreDescription += message+"\n";
	 }
	 public String getScoreDescription() { return(scoreDescription); }
	 private InternationalStrings s = G.getTranslations();
	 int moneyScore = 0;
	 public int currentScore(boolean pub)
	 {
		 return(currentScore(false,pub));
	 }
	 public int effectiveScore() { return(killed ? 0 : currentScore(true)); }
	 
	 boolean killed = false;
	 long currentBid = 0;
	 boolean elgibleToBid = true;
	 boolean currentBidMade = false;
	 boolean hasOfferedBid() { return(!currentBidMade && currentBidReady); }
	 boolean currentBidReady = false;
	 boolean hasMadeBid() { return(currentBidMade); }
	 
	 public QEChip getFLag() { return(flag); }
	 QEPlayer(QEChip fl,QEChip in,int ind,int np)
	 {	flag = fl;
	 	knownSpending = new long[np];
	 	index = ind;
	 	industry = new QECell(index,QEId.HitIndustry);
	 	industry.addChip(in);
	 	tilesWon = new QECell(index,QEId.HitTilesWon);
	 	whiteBoard = new QECell(index,QEId.HitWhiteBoard);
	 	whiteBoard.addChip(QEChip.WhiteBoard);
	 	noQE = new QECell(index,QEId.HitNoQE);
	 }
	 void startRound()
		 { noQE.reInit(); 
		   noQE.addChip(QEChip.NoQE); 
		   startAuction(); 
		 }
	 boolean hasUsedNoQE() { return(noQE.isEmpty()); }
	 void addKnownBid(long n) { knownBids.push(n); }
	 void addWinningBid(long n) { winningBids.push(n); }
	 void startAuction() 
	 	{ currentBid = 0; 
	 	  currentBidMade = false;
	 	  currentBidReady = false; 
	 	}
	 int nTilesMatchingNation()
	 {	int n=0;
		 for(int lim=tilesWon.height()-1; lim>=0; lim--)
		 {
			 if(tilesWon.chipAtIndex(lim).flag==flag.id) { n++; }
		 }
		 return(n);
	 }
	 int nTilesMatchingIndustry(QEId ind,boolean pub)
	 {	int n=0;
		 if(pub && (industry.topChip().industry==ind)) { n++; }	// our industry counts for 1
		 for(int lim=tilesWon.height()-1; lim>=0; lim--)
		 {
			 if(tilesWon.chipAtIndex(lim).industry==ind) { n++; }
		 }
		 return(n);
	 }
	 boolean hasIndustry(QEId ind)
	 {
		 for(int lim=tilesWon.height()-1; lim>=0; lim--)
		 {
			 if(tilesWon.chipAtIndex(lim).industry==ind) {return(true); }
		 }
		 return(false);
	 }
 
	 void copy(QEPlayer from)
	 {
		 tilesWon.copyFrom(from.tilesWon);
		 flag = from.flag;
		 knownBids.copyFrom(from.knownBids);
		 AR.copy(knownSpending,from.knownSpending);
		 winningBids.copyFrom(from.winningBids);
		 moneySpent = from.moneySpent;
		 moneyScore = from.moneyScore;
		 usedNoQE = from.usedNoQE;
		 killed = from.killed;
		 noQE.copyFrom(from.noQE);
		 industry.copyFrom(from.industry);
		 currentBid = from.currentBid;
		 elgibleToBid = from.elgibleToBid;
		 currentBidMade = from.currentBidMade;
		 currentBidReady = from.currentBidReady;
	 }
	 long Digest(Random r)
	 {	long v = tilesWon.Digest(r);
		 v ^= flag.Digest();
		 v ^= industry.Digest();
		 v ^= moneySpent*r.nextLong();
		 v ^= usedNoQE*r.nextLong();
		 v ^= knownBids.Digest(r);
		 v ^= winningBids.Digest(r);
		 v ^= moneyScore*r.nextLong();
		 v ^= noQE.Digest();
		 v ^= (currentBid+17+(killed?1234556:534232))*r.nextLong();
		 long rebid = r.nextLong();
		 long norebid = r.nextLong();
		 v ^= elgibleToBid ? rebid : norebid;
		 long bid = r.nextLong();
		 long nobid = r.nextLong();
		 v ^= currentBidMade ? bid : nobid;
		 long bidReady = r.nextLong();
		 long nobidReady = r.nextLong();
		 v ^= currentBidReady ? bidReady : nobidReady;
		 return(v);
	 }
	 void sameAs(QEPlayer from)
	 {	G.Assert(tilesWon.sameContents(from.tilesWon),"tiles won mismatch");
		 G.Assert(industry.sameContents(from.industry),"industry mismatch");
		 G.Assert(knownBids.sameContents(from.knownBids),"known bids mismatch");
		 G.Assert(AR.sameArrayContents(knownSpending,from.knownSpending),"known spending mismatch");
		 G.Assert(winningBids.sameContents(from.winningBids),"winning bids mismatch");
		 G.Assert(flag==from.flag,"flag mismatch");
		 G.Assert(moneySpent==from.moneySpent,"money mismatch");
		 G.Assert(usedNoQE==from.usedNoQE,"usedNoQE mismatch");
		 G.Assert(moneyScore==from.moneyScore,"money bonus mismatch");
		 G.Assert(hasUsedNoQE()==from.hasUsedNoQE(),"noqe mismatch");
		 G.Assert(killed==from.killed,"killed mismatch");
		 G.Assert(currentBid==from.currentBid,"currentbid mismatch");
		 G.Assert(currentBidReady==from.currentBidReady,"currentBidReady mismatch");
		 G.Assert(currentBidMade==from.currentBidMade,"currentBidMade mismatch");
		 G.Assert(elgibleToBid==from.elgibleToBid,"currentBidMade mismatch");
	 }
	 int QEScore(boolean annotate)
	 {	int qescore = usedNoQE*QEChip.NoQE.victoryPoints;
	 	if(annotate) { addToScoreDescription(""+qescore+" "+s.get(NoQEMessage)); }
	 	return(qescore);
	 }
	 int currentScore(boolean annotate,boolean pub)
	 {
		 return( QEScore(annotate)
				 + tileValue(annotate)
				 + nationScore(annotate)
				 + industryScore(annotate,pub)
				 + moneyScore
				 );
	 }
	 // face value of the tiles we won 
	 int tileValue(boolean record)
	 {	int v = 0;
	 	for(int i=0;i<tilesWon.height();i++)
		 {
	 		v += tilesWon.chipAtIndex(i).victoryPoints;
		 }
	 	if(record) 
	 		{ addToScoreDescription(""+v+" "+s.get(TileValueMessage));
	 		}
	 	return(v);
	 }
	 public int nationScore(boolean record)
	 {
		 int n = nTilesMatchingNation();
		 int v = nationTable[n];
		 if(record && v>0)
			{
			 addToScoreDescription(""+v+" "+s.get(NationalMessage,""+n,s.get(flag.id.shortName)));
			}
		 return(v);
	 }
	 public int industryScore(boolean record,boolean pub)
	 {	int v = 0;
	 	int diversity = 0;
	    for(QEChip industry : QEChip.IndustryChips)
	    {
	    	int inds = nTilesMatchingIndustry(industry.id,pub);
	    	int monop = monopolyTable[inds];
	    	v += monop;
	    	if(record && (monop>0))
			{
			 addToScoreDescription(""+monop+" "+s.get(MonopolyValueMessage,""+inds,s.get(industry.id.shortName)));
			}
	    	if(hasIndustry(industry.id)) { diversity++; }
		}
	    int divscore = diversityTable[diversity];
	    v += divscore;
	    if(record && (divscore>0))
		   { 
			 addToScoreDescription(""+divscore+" "+s.get(DiversityMessage,diversity));
		   }
	    return(v);
	 }
 }
 class QEBoard extends RBoard<QECell> implements BoardProtocol,QEConstants
 {	static int REVISION = 100;			// 100 represents the initial version of the game
	public int getMaxRevisionLevel() { return(REVISION); }
	 QEVariation variation = QEVariation.qe;
	 private QEState board_state = QEState.Puzzle;	
	 private int auctionRound = 0;	// keep track of repeated auctions
	 private StateStack robotState = new StateStack();
	 private CellStack moveStack = new CellStack();
	 public QEState getState() { return(board_state); }
	 public QECell futureAuctions = new QECell(QEId.HitAuction);
	 public QECell thisRoundAuction = new QECell(QEId.HitPending);
	 public QECell currentAuction = new QECell(QEId.HitCurrent);
	 public QECell wasteBasket = new QECell(QEId.HitWaste);
	 public LStack openBids = new LStack();
	 
	 QEPlayer players[] = null;
	 public int firstPlayerThisRound = -1;
	 final int TRACKLENGTH = 40;
 
	 /**
	  * this is the preferred method when using the modern "enum" style of game state
	  * @param st
	  */
	 public void setState(QEState st) 
	 { 	
		 board_state = st;
		 if(!board_state.GameOver()) 
			 { AR.setValue(win,false); 	// make sure "win" is cleared
			 }
	 }
	 public boolean winForPlayerNow(int pl)
	 {
		 return(win[pl]);
	 }
	 private void loadStack(ChipStack chips,QEChip from[],Random r)
	 {
		 for(QEChip c : from) { chips.push(c); }
		 chips.shuffle(r);
	 }
	 
	 // each player gets a flag and an industry
	 private void initPlayers(Random r)
	 {
		 ChipStack flags = new ChipStack();
		 ChipStack indus = new ChipStack();
		 loadStack(flags,QEChip.FlagChips,r);
		 loadStack(indus,QEChip.IndustryChips,r);
		 players = new QEPlayer[players_in_game];
		 for(int i=0;i<players_in_game;i++)
		 {	QEPlayer newp = players[i] = new QEPlayer(flags.pop(),indus.pop(),i,players_in_game);
			 getCell(newp.currentScore(false)).addChip(newp.flag);
		 }
	 }
	 void moveScoreMarker(QEChip flag,int from,int to,replayMode replay)
	 {	QECell old = getCell(from%TRACKLENGTH);
	 	QEChip fl = old.removeChip(flag);
		G.Assert(fl!=null,"old score incorrect");
		QECell newc = getCell(to%TRACKLENGTH);
		newc.addChip(fl);
		if(replay!=replayMode.Replay) 
		{
			animationStack.push(old);
			animationStack.push(newc);
		}
	 }
	 private void addTileWon(QEPlayer pl,QEChip ch,replayMode replay)
	 {	int oldscore = pl.currentScore(false);
		pl.tilesWon.addChip(ch);
		moveScoreMarker(pl.flag,oldscore,pl.currentScore(false),replay);
	 }
	 private void addToScore(QEPlayer pl,int n,replayMode replay)
	 {	int oldscore = pl.currentScore(false);
		pl.moneyScore += 6;
		moveScoreMarker(pl.flag,oldscore,pl.currentScore(false),replay);
	 }
	 private void addUsedNoQE(QEPlayer pl,replayMode replay)
	 {	int oldscore = pl.currentScore(false);
		pl.usedNoQE++;
		pl.noQE.removeTop();
		moveScoreMarker(pl.flag,oldscore,pl.currentScore(false),replay);
	 }

	 // ultimately 4 randomized quarters each containing 1 randomized tile of each type
	 private void initAuction(Random r)
	 {	ChipStack[]temp = new ChipStack[]{ new ChipStack(),new ChipStack(),new ChipStack(),new ChipStack()};
		 loadStack(temp[0],QEChip.CarChips,r);
		 loadStack(temp[1],QEChip.TrainChips,r);
		 loadStack(temp[2],QEChip.GovChips,r);
		 loadStack(temp[3],QEChip.PlaneChips,r);
		 futureAuctions.reInit();
		 thisRoundAuction.reInit();
		 currentAuction.reInit();
		 ChipStack p3 = new ChipStack();
		 
		 while(temp[0].size()>0)
		 {	ChipStack sh = new ChipStack();
			 for(ChipStack ch : temp)
			 {
				 sh.push(ch.pop());
			 }
			 sh.shuffle(r);	// shuffle a stack which contains 1 of each industry
			 if(players_in_game==3) { p3.push(sh.pop()); }	// sequester one tile
			 while(sh.size()>0) { futureAuctions.addChip(sh.pop());}
		 }
		 while(p3.size()>0) { futureAuctions.insertChipAtIndex(0,p3.pop()); };
	 }
	 public void initBoard()
	 {	super.initBoard();
		 for(int lim = TRACKLENGTH-1; lim>=0; lim--)
		 {	QECell cc = newcell('A',lim);
			 addCell(cc);
		 }
	 }
	 // get the chip pool and chip associated with a player.  these are not 
	 // constants because of the swap rule.
	 public QEPlayer getPlayer(int p) 
	 { return(p>=0 && p<players.length ? players[p] : null); 
	 }
	 public QEPlayer getPlayer(QEChip flag)
	 {
		 for(QEPlayer p : players)
		 {
			 if(p.flag==flag) { return(p); }
		 }
		 return(null);
	 }
	 public QEChip getPlayerChip(int p)  { return(players[p].flag); };
	 public QEChip getCurrentPlayerChip()  { return(players[whoseTurn].flag); };
 
 // this is required even though it is meaningless for Hex, but possibly important
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
	 public QEChip pickedObject = null;
	 public QEChip lastPicked = null;
	 private CellStack pickedSourceStack = new CellStack(); 
	 private CellStack droppedDestStack = new CellStack();
	 private IStack pickedIndexStack = new IStack();
	 private IStack droppedIndexStack = new IStack();
	 
	 private StateStack stateStack = new StateStack();
	 private QEState resetState = QEState.Puzzle; 
	 public QEChip lastDroppedObject = null;	// for image adjustment logic
 
	 // factory method to generate a board cell
	 public QECell newcell(char c,int r)
	 {	return(new QECell(QEId.BoardLocation,c,r));
	 }
	 
	 // constructor 
	 public QEBoard(String init,int players,long key,int rev) // default constructor
	 {	
		 doInit(init,key,players,rev); // do the initialization 
	 }
	 public long getOpenBidValue()
	 {
		 if(firstPlayerThisRound>=0)
		 {	QEPlayer pl = players[firstPlayerThisRound];
			if(pl.hasMadeBid()) {  return(pl.currentBid); }
			return(0);
		 }
		 return(-1);
	 }
	 
	 
	 public int cellToY(QECell c)
	 {
		 int h = G.Height(boardRect);
		 int top = G.Top(boardRect);
		   int y0 = (int)(top+0.11*h);
		   int y1 = (int)(top+0.90*h);
		 int row = (TRACKLENGTH/4);
		 double rem = c.row%row;
		 switch(c.row/(TRACKLENGTH/4))
		 {
		 default:
		 case 0:
			 return(G.interpolate(rem/row, y0, y1));
		 case 1:
			 return(y1);
		 case 2:
			 return(G.interpolate(rem/row, y1, y0));
			 
		 case 3:
			 return(y0);
		 }
	 } 
	 public int cellToX(QECell c)
	 {	int w = G.Width(boardRect);
		 int left = G.Left(boardRect);
		 int x0 = (int)(left+ 0.10*w);
		 int x1 = (int)(left+0.91*w);
		 int row = (TRACKLENGTH/4);
		 double rem = c.row%row;
		 switch(c.row/row)
		 {
		 default:
		 case 0:
			 // first street
			 return(x1);
		   case 1:
			 return(G.interpolate(rem/row,x1,x0));
		 case 2:
			 return(x0);
		 case 3:
			 return(G.interpolate(rem/row, x0, x1));
		 }
	 }
	 public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
	 
 
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
	 public void doInit(String gtype,long key,int nPlayers,int rev)
	 {	randomKey = key;
	   	adjustRevision(rev);
		 players_in_game = nPlayers;
		 win = new boolean[nPlayers];
		 Random r = new Random(734687);	// this random is used to assign hash values to cells, common to all games of this type.
		 setState(QEState.Puzzle);
		 variation = QEVariation.findVariation(gtype);
		 G.Assert(variation!=null,WrongInitError,gtype);
		 robotState.clear();
		 moveStack.clear();
		 wasteBasket.reInit();
		 gametype = gtype;
		 switch(variation)
		 {
		 default: throw G.Error("Not expecting variation %s",variation);
		 case qe:
			 initBoard();
			 Random initr = new Random(key);
			 initAuction(initr);
			 initPlayers(initr);
			 // hack to make a short game
			 //while(futureAuctions.height()>2) { futureAuctions.removeTop(); }
		 }
		 allCells.setDigestChain(r);		// set the randomv for all cells on the board
	 
		 firstPlayerThisRound = -1;
		 auctionRound = 0;
		 whoseTurn = FIRST_PLAYER_INDEX;
		 acceptPlacement();
 
		 // set the initial contents of the board to all empty cells
		 
		 Random rv = new Random(randomKey);
		 int initial = Random.nextInt(rv,50)-25+30;
		 openBids.clear();
		 openBids.push(initial);						// seed the open bids value
		 for(QEPlayer p : players)
		 {
			 p.knownBids.push(Random.nextInt(rv,20)+initial);	// seed the known bids value
			 p.winningBids.push(Random.nextInt(rv,20)+initial);	// seed the winning bids value
		 }
		 animationStack.clear();
		 moveNumber = 1;
 
		 // note that firstPlayer is NOT initialized here
	 }
 
	 /** create a copy of this board */
	 public QEBoard cloneBoard() 
	 { QEBoard dup = new QEBoard(gametype,players_in_game,randomKey,revision); 
	   dup.copyFrom(this);
	   return(dup); 
	   }
	 public void copyFrom(BoardProtocol b) { copyFrom((QEBoard)b); }
 
	 public QECell getCell(int row)
	 {
		 cell<QECell> ar[] = getCellArray();
		 if(row>=0 && row<TRACKLENGTH) { return((QECell)(ar[row])); }
		 return(null);
	 }
	 /* make a copy of a board.  This is used by the robot to get a copy
	  * of the board for it to manipulate and analyze without affecting 
	  * the board that is being displayed.
	  *  */
	 public void copyFrom(QEBoard from_b)
	 {
		 super.copyFrom(from_b);
		 firstPlayerThisRound = from_b.firstPlayerThisRound;
		 auctionRound = from_b.auctionRound;
		 robotState.copyFrom(from_b.robotState);
		 board_state = from_b.board_state;
		 wasteBasket.copyFrom(from_b.wasteBasket);
		 getCell(droppedDestStack,from_b.droppedDestStack);
		 getCell(pickedSourceStack,from_b.pickedSourceStack);
		 pickedIndexStack.copyFrom(from_b.pickedIndexStack);
		 droppedIndexStack.copyFrom(from_b.droppedIndexStack);
		 stateStack.copyFrom(from_b.stateStack);
		 getCell(moveStack,from_b.moveStack);
		 pickedObject = from_b.pickedObject;
		 resetState = from_b.resetState;
		 lastPicked = null;
		 futureAuctions.copyFrom(from_b.futureAuctions);
		 thisRoundAuction.copyFrom(from_b.thisRoundAuction);
		 currentAuction.copyFrom(from_b.currentAuction);
		 openBids.copyFrom(from_b.openBids);
		 for(int i=0;i<players_in_game;i++) { getPlayer(i).copy(from_b.getPlayer(i));}
  
		 sameboard(from_b); 
	 }
 
	 
 
	 public void sameboard(BoardProtocol f) { sameboard((QEBoard)f); }
 
	 /**
	  * Robots use this to verify a copy of a board.  If the copy method is
	  * implemented correctly, there should never be a problem.  This is mainly
	  * a bug trap to see if BOTH the copy and sameboard methods agree.
	  * @param from_b
	  */
	 public void sameboard(QEBoard from_b)
	 {
		 super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
		 G.Assert(variation==from_b.variation,"variation matches");
		 G.Assert(openBids.sameContents(from_b.openBids),"openbids mismatch");
		 G.Assert(pickedObject==from_b.pickedObject, "picked Object mismatch");
		 G.Assert(wasteBasket.sameContents(from_b.wasteBasket),"wastebasket mismatch");
		 // this is a good overall check that all the copy/check/digest methods
		 // are in sync, although if this does fail you'll no doubt be at a loss
		 // to explain why.
		 G.Assert(futureAuctions.sameContents(from_b.futureAuctions),"auction mismatch");
		 G.Assert(thisRoundAuction.sameContents(from_b.thisRoundAuction),"pending mismatch");
		 G.Assert(currentAuction.sameContents(from_b.currentAuction),"current mismatch");
		 G.Assert(firstPlayerThisRound==from_b.firstPlayerThisRound,"firstPlayer mismatch");
		 G.Assert(auctionRound==from_b.auctionRound,"auctionRound mismatch");
		 for(int i=0;i<players.length;i++) { getPlayer(i).sameAs(from_b.getPlayer(i)); }
 
		 G.Assert(Digest()==from_b.Digest(),"Digest matches");
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
		 long v = super.Digest();
		 // many games will want to digest pickedSource too
		 // v ^= cell.Digest(r,pickedSource);
		 v ^= chip.Digest(r,pickedObject);
		 v ^= Digest(r,pickedSourceStack);
		 v ^= Digest(r,droppedIndexStack);
		 v ^= Digest(r,pickedIndexStack);
		 v ^= Digest(r,droppedDestStack);
		 v ^= Digest(r,revision);
		 v ^= Digest(r,moveStack);
		 v ^= futureAuctions.Digest(r);
		 v ^= thisRoundAuction.Digest(r);
		 v ^= currentAuction.Digest(r);
		 v ^= wasteBasket.Digest(r);
		 v ^= Digest(r,openBids);
		 v ^= r.nextLong()*(firstPlayerThisRound+17+auctionRound*1824);
		 v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		 for(int i=0;i<players.length;i++) { v ^= getPlayer(i).Digest(r); } 
		 return (v);
	 }
 
 
 
 
	 /** this is used to determine if the "Done" button in the UI is live
	  *
	  * @return
	  */
	 public boolean DoneState()
	 {	return(board_state.doneState());
	 }
	 // this is the default, so we don't need it explicitly here.
	 // but games with complex "rearrange" states might want to be
	 // more selecteive.  This determines if the current board digest is added
	 // to the repetition detection machinery.
	 public boolean DigestState()
	 {	
		 return(board_state.digestState());
	 }
 
 
 
	 public boolean gameOverNow() { return(board_state.GameOver()); }
  
	 public int scoreForPlayer(int index)
	 {
		 return(players[index].effectiveScore());
	 }
	 public void doFinalScoring(replayMode replay)
	 {	long lowMoney = -1;
		long highMoney = -1;
		 InternationalStrings s = G.getTranslations();
		 for(QEPlayer p : players)
			 { 
			   p.addToScoreDescription(Calculator.formatDisplay(""+p.moneySpent)+" "+s.get(MoneyMessage));
			   p.currentScore(true,true);	// annotate the score breakdown
				
			   if(lowMoney<0 || p.moneySpent<lowMoney) { lowMoney = p.moneySpent; }
			   if(highMoney<0 || p.moneySpent>highMoney) { highMoney = p.moneySpent; }
			 }
		 for(QEPlayer p : players)
		 {
			 if(p.moneySpent==lowMoney) // 6 points for thrift
				 { addToScore(p,6,replay);
				   p.addToScoreDescription("6 "+s.get(ThriftMessage));
				 }	
			 if(p.moneySpent==highMoney) // zero out the big spenders
				 { p.killed = true;
				   int tot = p.currentScore(false,true);
				   p.addToScoreDescription("-"+Calculator.formatDisplay(""+tot)+" "+s.get(MostMoneyMessage));
				 }	
		 }
		 QEPlayer winner = null;
		 int winnerScore = 0;
		 for(QEPlayer p : players)
		 {	int es = p.effectiveScore();
		 	if(es>0)
			 {
			 if(winner==null || es>winnerScore) { winner = p; winnerScore = es; } 
			 else { if(es==winnerScore)
					 {	if(p.moneySpent<winner.moneySpent)
						 { winner = p;
						   winnerScore = es;
						 }
					 }
				 }
			 }
		 }
		 if(winner!=null) { win[winner.index]=true;  }
	 }
 
	 //
	 // accept the current placements as permanent
	 //
	 public void acceptPlacement()
	 {	
		 droppedDestStack.clear();
		 pickedSourceStack.clear();
		 pickedIndexStack.clear();
		 droppedIndexStack.clear();
		 stateStack.clear();
		 pickedObject = null;
	  }
	 //
	 // undo the drop, restore the moving object to moving status.
	 //
	 private QECell unDropObject()
	 {	QECell rv = droppedDestStack.pop();
	 	setState(stateStack.pop());
	 	pickedObject = rv.removeTop(); 	// SetBoard does ancillary bookkeeping
	 	return(rv);
	 }
	 // 
	 // undo the pick, getting back to base state for the move
	 //
	 private void unPickObject()
	 {	QECell rv = pickedSourceStack.pop();
		 setState(stateStack.pop());
		 rv.addChip(pickedObject);
		 pickedObject = null;
	 }
	 
	 // 
	 // drop the floating object.
	 //
	 private void dropObject(QECell c,int ind)
	 {
		droppedDestStack.push(c);
		droppedIndexStack.push(ind);
		stateStack.push(board_state);
		QEChip po = pickedObject;
		lastDroppedObject = pickedObject;
		pickedObject = null;
		if(ind<0) { c.addChip(po);} else { c.insertChipAtIndex(ind, po); }
	 }
	 //
	 // true if c is the place where something was dropped and not yet confirmed.
	 // this is used to mark the one square where you can pick up a marker.
	 //
	 public boolean isDest(QECell c)
	 {	return(droppedDestStack.top()==c);
	 }
	 
	 //get the index in the image array corresponding to movingObjectChar 
	 // or HitNoWhere if no moving object.  This is used to determine what
	 // to draw when tracking the mouse.
	 // caution! this method is called in the mouse event process
	 public int movingObjectIndex()
	 { QEChip ch = pickedObject;
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
	 private QECell getCell(QEId source, long row)
	 {
		 switch (source)
		 {
		 default:
			 throw G.Error("not expecting source %s", source);
		 case HitWaste:
			 return(wasteBasket);
		 case HitNoQE:
			 	return(players[(int)row].noQE);
		 case HitPending:
				 return(thisRoundAuction);
		 case HitCurrent:
				 return(currentAuction);
		 case HitAuction:
				 return(futureAuctions);
		 case HitTilesWon:
			 	return(players[(int)row].tilesWon);
		 case HitIndustry:
				 return(players[(int)row].industry);
		 case NoQE:
				 return(players[(int)row].noQE);        	
		 } 	
	 }
	 public QECell getCell(QECell c)
	 {
		 return((c==null)?null:getCell(c.rackLocation(),c.row));
	 }
	 // pick something up.  Note that when the something is the board,
	 // the board location really becomes empty, and we depend on unPickObject
	 // to replace the original contents if the pick is cancelled.
	 private void pickObject(QECell c,int ind)
	 {	pickedSourceStack.push(c);
		 pickedIndexStack.push(ind);
		 stateStack.push(board_state);
		 pickedObject = lastPicked = (ind>=0) ? c.removeChipAtIndex(ind) : c.removeTop();
		 lastDroppedObject = null;
	 }
	 //	
	 //true if cell is the place where something was picked up.  This is used
	 // by the board display to provide a visual marker where the floating chip came from.
	 //
	 public boolean isSource(QECell c)
	 {	return(c==pickedSourceStack.top());
	 }
	 
	 public boolean usingNoQE(QEPlayer pl)
	 {	 
		 return((board_state==QEState.Witness) && (pl.currentBid==0) && !pl.hasUsedNoQE());
	 }
	 public boolean consideringBids(QEPlayer pl)
	 {
		 return((board_state==QEState.Witness)&& (pl.index==firstPlayerThisRound)); 
	 }
	 public boolean winningBid(QEPlayer pl)
	 {
		 if(board_state==QEState.Witness)
		 {
			long highbid = -1;
			for(QEPlayer p : players) { if(p.currentBid>highbid) { highbid = p.currentBid; }}
			return(pl.currentBid==highbid);
		 }
		 return(false);
	 }
	 private void finishAuction(QEmovespec m,replayMode replay)
	 {
		 QEPlayer winner = null;
		 QEPlayer master = firstPlayerThisRound>=0 ? players[firstPlayerThisRound] : null;
		 long winningBid = -1;
		 int nWinningBids = 0;
		 for(QEPlayer pl : players)
		 {	long bid = pl.currentBid;
			 if((master!=null) && (bid>0) && (pl!=master))
			 {
				 master.addKnownBid(bid);
			 }
			 if(winner==null || pl.currentBid>winningBid)
			 {	nWinningBids = 1;
				 winner = pl;
				 winningBid = bid;  			
			 }
			 else if(pl.currentBid==winningBid) { nWinningBids++; } 		
			 if(bid==0 && !pl.hasUsedNoQE())
			 {
				 addUsedNoQE(pl,replay);
			 }
		 }
		 if((firstPlayerThisRound==-1) && (nWinningBids==3))
		 {
			 // rare case of a 3-way tie in the final round of a 3 player game
			 // end the game without a winner for the final auction
			 wasteBasket.addChip(currentAuction.removeTop());
			 if(replay!=replayMode.Replay)
			 {
				 animationStack.push(currentAuction);
				 animationStack.push(wasteBasket);
			 }
			 reloadBoard(replay);
		 }
		 else 
		 {if( (auctionRound==3) 
				 && (nWinningBids>1))	// stalled out, award the auction to the highest unique
		 { 
		   long ignoredBid = winningBid;
		   winner = null;
		   winningBid = 0;
		   nWinningBids = 0;
		   for(QEPlayer p : players)
		   {
			   if((p.currentBid!=ignoredBid) 
					   && ((winner==null)||p.currentBid>winningBid))
			   {		winner = p;
						 winningBid = p.currentBid;
						 nWinningBids=1;
			   }
			   else if(p.currentBid==winningBid)
			   {
				   G.Error("should be a single winner");
			   }
		   }
		 }
		 if(nWinningBids == 1)
		 {	
			long openBid = master==null ? 0 : master.currentBid;

			for(QEPlayer pl : players)
		 	{
			pl.knownSpending[winner.index] += (pl==master) 
					? winningBid 
					: Math.max(openBid,pl.currentBid);	// maximum of the open bid or his own bid 
		 	}
			if(master!=null)
				 {
				 master.addWinningBid(winningBid);
				 }
				 winner.moneySpent += winningBid;
			 QEChip won = currentAuction.removeTop();
			 addTileWon(winner,won,replay);
			 m.winner = winner.flag;
			 if(replay!=replayMode.Replay)
			 {
				 animationStack.push(currentAuction);
				 animationStack.push(winner.tilesWon);
			 }
			 reloadBoard(replay);
			   if(board_state!=QEState.Gameover)
			   {
				   auctionRound = 0;
				   //G.print("Start a "+firstPlayerThisRound);
				   setWhoseTurn(firstPlayerThisRound);
				   if((players_in_game==3) && (futureAuctions.isEmpty()))
					{
					setState(QEState.SealedBid);
					firstPlayerThisRound=-1;
					}
					else
					{
					setState(QEState.OpenBid);
					}
				   
			   }
		 }
		 else {
			 // repeat auction among the high bidders
			 auctionRound++;
			 boolean first = true;
			 for(QEPlayer p : players)
			 {
				 if(p.currentBid==winningBid) 
					 { p.startAuction();
					   if(first) { setWhoseTurn(p.index); first=false; }
					 }
			 }
			 	setState(QEState.Rebid);
    	}}
      }
    private void setNextStateAfterDone(QEmovespec m,replayMode replay)
    {	
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting after Done state %s",board_state);
    	case Gameover: break;
    	case Witness:
    		finishAuction(m,replay); 
    		break;
    	case SealedBid:
		case Confirm:
    		{
    			boolean ready = true;
    			QEPlayer thisPlayer = players[m.player];
    			//G.Assert(thisPlayer.currentBidReady,"should be ready");
    			thisPlayer.currentBidMade = true;
    			thisPlayer.currentBidReady = false;
    			int nextP = -1;
    			for(QEPlayer other : players)
    				{	if(other.currentBidReady)
    					{
    					if(other.index==firstPlayerThisRound) 
    						{ // record the current bid
    							openBids.push(other.currentBid);
    						} 
    					}
    					if(other.index!=firstPlayerThisRound)
    					{	if((nextP==-1) && !other.currentBidMade) { nextP = other.index; }
    						ready &= other.currentBidMade;
     					}
    				}
    			if(ready) 
    				{ 	if(m.op==MOVE_DONE)
    						{ normalStart();
    						}
    				}
     			else {
      				// change the current player	
    				setWhoseTurn(nextP);
    				setState(auctionRound>1 ? QEState.Rebid:QEState.SealedBid ) ; 

    			}
    		}
    		break;
    	case Puzzle:
     		reloadBoard(replay);
    		setState(QEState.OpenBid);	
    		break;
    	}
       	resetState = board_state;
    }
    private void doDone(QEmovespec m,replayMode replay)
    {	moveNumber++;
        acceptPlacement();
        setNextStateAfterDone(m,replay);
    }


	public void reloadBoard(replayMode replay)
	{
		if(currentAuction.isEmpty())
			{
				if(thisRoundAuction.isEmpty())
				{
					QECell c = null;
					if(replay!=replayMode.Replay)
						{ c = new QECell(thisRoundAuction);
						}

					for(int i=0;i<players_in_game && !futureAuctions.isEmpty();i++) 
						{ thisRoundAuction.addChip(futureAuctions.removeTop());
						if(replay!=replayMode.Replay)
						{
							animationStack.push(futureAuctions);
							// destination with card backs and the correct height
							c.addChip(QEChip.Back);
							animationStack.push(c);
						}

						}
					for(QEPlayer p : players)
					{
						if(p.noQE.isEmpty()) { p.noQE.addChip(QEChip.NoQE);}
					}
				}
				if(!thisRoundAuction.isEmpty())
					{
					currentAuction.addChip(thisRoundAuction.removeTop());
					if(replay!=replayMode.Replay)
					{
						animationStack.push(thisRoundAuction);
						animationStack.push(currentAuction);
					}
					auctionRound = 1;
					firstPlayerThisRound++;
					firstPlayerThisRound = firstPlayerThisRound%players_in_game;
					for(QEPlayer p : players) { p.startAuction(); }
					}
			}
		if(currentAuction.isEmpty())
		{	doFinalScoring(replay);
			setWhoseTurn(firstPlayerThisRound);
			setState(QEState.Gameover);
		}
	}
	public QEPlayer getOpenBidPlayer()
	{
		return(firstPlayerThisRound>=0 ? players[firstPlayerThisRound] : null);
	}
	private void synchronousBid(QEmovespec m,replayMode replay)
	{
    	QEPlayer pl = players[m.player];
		G.Assert(!pl.currentBidMade,"shouldn't be made already");
		pl.currentBidReady = true;
		pl.currentBid = m.amount;
		stateStack.push(board_state);
		setState(QEState.Confirm);
		m.target = currentAuction.topChip();
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	QEmovespec m = (QEmovespec)mm;
    	if(replay!=replayMode.Replay) { animationStack.clear(); }

        //G.print("E "+m+" for "+board_state);
        switch (m.op)
        {
        case MOVE_EBID:
        	{
        	G.Assert(board_state!=QEState.OpenBid,"can't be openbid");
        	QEPlayer pl = players[m.player];
        	G.Assert(!pl.currentBidMade,"shouldn't be made already");
    		pl.currentBidReady = true;
    		pl.currentBid = m.amount;
    		m.target = currentAuction.topChip();
        	}
    		break;
        case MOVE_OPENBID:
        	G.Assert(board_state==QEState.OpenBid,"must be openbid");
        	synchronousBid(m,replay);
        	break;
		case MOVE_SECRETBID:
         	G.Assert(board_state!=QEState.OpenBid,"must not be open	bid");
        	synchronousBid(m,replay);
        	break;

        case MOVE_ECOMMIT:
        	{
        	QEPlayer p = players[m.player];
        	p.currentBidMade = true;
        	p.currentBidReady = false;
        	moveNumber++;
    		m.target = currentAuction.topChip();
        	}
        	break;
		case MOVE_DONE:
        	
	   		m.target = currentAuction.topChip();
        	doDone(m,replay);
 
            break;

        case MOVE_PICK:
        	// come here only where there's something to pick, which must
 			{
 			QECell src = getCell(m.source,m.amount);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src,m.to_height);
 			}}
            break;

        case MOVE_DROP: // drop on chip pool;
        	if(pickedObject!=null)
        	{
            QECell dest = getCell(m.source,m.amount);
            if(isSource(dest)) { unPickObject(); }
            else { dropObject(dest,m.to_height); }
            acceptPlacement();
        	}
            break;
 
        case MOVE_START:
            setWhoseTurn(m.player);
            if(firstPlayerThisRound==-1) { firstPlayerThisRound = m.player-1; }
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(QEState.Puzzle);	// standardize the current state
            if(futureAuctions.isEmpty())
               	{ setState(QEState.Gameover); 
               	}
            else {  setNextStateAfterDone(m,replay); }

            break;

        case MOVE_EDIT:
        	acceptPlacement();
            setState(QEState.Puzzle);
 
            break;
            
		case MOVE_GAMEOVERONTIME:
			win[whoseTurn] = true;
			setState(QEState.Gameover);
			break;

        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }

    // legal to hit the chip storage area
    public boolean legalToHitChips(QECell c)
    {
        switch (board_state)
        {
        default:
			return(false);
        case Puzzle:
            return (pickedObject==null ? !c.isEmpty() : true);
        }
    }

    public boolean legalToHitBoard(QECell c)
    {	if(c==null) { return(false); }
    	if(c.rackLocation()==QEId.BoardLocation) { return(false); } 
        switch (board_state)
        {
        default:
        	return(false);
        case Puzzle:
            return (pickedObject==null ? !c.isEmpty() : true);
        }
    }
    
    
 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(QEmovespec m)
    {
        //robotState.push(board_state); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
        //G.print("R "+m);
        //G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {
            if (m.op == MOVE_DONE)
            {
            }
            else if (DoneState())
            {
                doDone(m,replayMode.Replay);
            }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
    }
 

 public void normalStart()
 {	setWhoseTurn(firstPlayerThisRound);
 	//G.print("Set "+firstPlayerThisRound);
	setState(QEState.Witness);
 }
 public boolean readyToStartNormal()
 {
 	for(QEPlayer p : players) 
		{ if(!p.hasMadeBid()) 
			{ return(false);  }}
 	return(true);
 }
 public QEState getUIState(int forPlayer)
 {	switch(board_state)
	 {
	 default:	
		 return(board_state);
	 case Rebid:
	 case EphemeralWait:
	 case SealedBid:
	 {
		 QEPlayer p = players[forPlayer];
		 if(p.hasMadeBid()) { return(QEState.EphemeralWait); }
		 if(p.hasOfferedBid()) { return(QEState.EphemeralConfirm); }
		 return((board_state==QEState.Rebid) ? QEState.EphemeralRebid : QEState.EphemeralSealedBid);
	 }
	 }
 	
 }
 
}
