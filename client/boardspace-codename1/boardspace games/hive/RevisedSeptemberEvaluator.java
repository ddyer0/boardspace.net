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
package hive;

import hive.HiveConstants.HiveId;
import hive.HiveConstants.HiveState;
import hive.HiveConstants.PieceType;
import lib.DStack;
import lib.G;
import online.game.BoardProtocol;
import online.game.commonMove;
import online.search.CommonDriver;
import online.search.DefaultEvaluator;
import online.search.Evaluator;

/**
 * Split on Sep 1 2025
 * 
 * This revision makes these changes, based on observing the previous version RevisedAugustEvaluator vs some expert players.
 * (1) do not randomize if the opponents Q has been deployed
 * 
 * (2) add a small penalty for pinned pieces.  I observed a horizon effect with the current
 * 4-ply lookahead, that a piece woud be deployed, pinned, another piece deployed and not immediately pinned.
 * this effectively sacrifices the first piece for no benefit, and no cost except the deployment penalty.  If
 * this penalty is too strong, making rings (which free pieces) becomes too attractive.
 * Use this penalty for both pinned and buried bugs
 * (3) "revision n"
 * major reshuffle with no net change to make almost all the weights positive, instead of a rich mix
 * of positive and negative effects.  The purpose of this is to be compatible with the "sprint" logic in
 * hiveplay.java, which asserts that if you're far enough ahead, you can mostly ignore what the opponent
 * is doing.  This allows closing in on the win even if in the process you must ignore what the poor luser
 * is doing;
 * (4) revision "W" add ring awareness.  forming and destroying rings previously caused wild
 * swings in the evaluation.
 * 
 * revision second D increases the bonus for pillbug adjacent to Q
 * revision second E adds a small bonus for gated approaches to the Q, and restricts the "immune" bonus to
 * cells adjacent to the Q and actual pillbugs 
 * 
 * revision H applies pin penalties to beetles and mosquitos next to Q
 * 
 * Suggestions for the next level:
 * 
 * reduce the "pillbug danger" penalty
 * ameliorate a numerical instability when evaluating identical positions
 * 
 * Notes from the august evaluator:
 * 
 * this is a complete rethinking of the evaluator for dumbot, but mostly along
 * the same lines of thought, with more attention to the details of the incentives
 * 
 * 1) the manhattan distance to the opponent queen is a primary component.  the distance
 * is scored twice, once for the actual distance, and again, with different weights, for
 * the closest you can get with one move.  This motivates bugs to converge on the opposing
 * queen. Pieces that are immobile and not adjacent to the Q do not score at all, so effectively
 * become invisible.
 * 
 * 2) pieces that are mobile are scored based on the number of places they can move to, up
 * to a maximum.  More choices are better.  But ants don't automatically look super powerful.
 * Pieces that have reached the queen are scored as maximally mobile, so they don't lose points
 * just for being there, and so pinning against the Q doesn't improve your score.  There's also
 * a small general bonus for total mobility.
 * 
 * 3) pieces that are still in the rack score modestly, so deploying them only seems to 
 * improve the situatation where the deployment gains points.
 * 
 * 4) as the queen is increasingly surrounded, it's very bad.
 * 
 * in addition to these general considerations, several special cases are included in
 * the logic.   
 * a) queen, pillbug, and beetle are bad if buried
 * b) mobile beetle or mosquito on top is a good thing.   It's also important that the first level sort
 *    places the beetle "up" move at the top of the list, because sometimes the "up" move goes away on
 *    the next ply.  This came up in a game where the PV was "up/cover q" which evaluated exactly the
 *    same as "move to side/cover q" except that as played out, the cover q move went away.
 * c) having no spawn points (where there are still pieces in the rack) is a bad thing.
 * d) having no moves is very bad
 * e) pairs of pieces where both appear mobile, but if one is moved the other becomes immobile
 *    have to be considered - their apparent mobility is downgraded.
 * f) pillbugs are feared, and extra empty spaces adjacent to a pillbug is good.
 * 
 * given all of these considerations, its possible to tweak the numbers such that
 * any particular move can be coerced, but it's a balancing act to get overall behavior
 * that wins.  You can spend foever tuning these numbers.
 * 
 * set "L" downgrades total mobility, so breaking and forming rings is not so attractive
 * and changes the count for queen crowding to give only partial credit for beetles on top,
 * and only when they actually have a spot to drop into.
 */
class RevisedSeptemberEvaluator extends DefaultEvaluator implements Evaluator
{	

	public int canRandomize(BoardProtocol b,int who)
	{	HiveGameBoard board = (HiveGameBoard)b;
		HiveCell myQueenLoc = board.pieceLocation.get(board.playerQueen(who));
		HiveCell hisQueenLoc = board.pieceLocation.get(board.playerQueen(who^1));
		return !hisQueenLoc.onBoard && !myQueenLoc.onBoard && (board.getState()!=HiveState.QUEEN_PLAY_STATE)
				? (30 - board.moveNumber) 
				: 0; 
	}

	private double trustYourGutThreshold = 15;			// full evaluation changes less than this 
	private double trustYourGutTrigger = 80;			// absolute score less than this, so the game looks like a draw
	
	// simple evaluation based on piece mobility and importance
	private double pillbugSafeBonus = 2.0;				// pillbugs untroubled by beetles or other mosquitos
	private double totalChoicesWeight = 0.05;			// benefit of any choice at all.
	private double pillbug_queen_afinity_weight = 6.0;	// bonus for pillbug adjacent or next to adjacent to Q
	private double QueenUnderBonus = 10.0;		// bonus for burying the opposing Q
	private double beetleOnTopBonus = 2.5;		// bonus for beetle on top.  Making this greater is a bad idea
	private double upBonus = 0.2;	// on top but not adjacent to Q
	
	private double someDropBonus = 5;			// penalty for having no way to play in
	private double shutoutPenalty = 25;			// penalty for no moves. If this is too high we'll never abandon a shutout
	// value of a piece still in the rack.  This is to counter the "just play it" impulse
	// and punish playing pieces that are immediately pinned.
	// TODO: next experiment, increase the grasshopper reserve, based on start-04-k-win where the grasshoppers
	// were deployed even though they would be pinned
	// TODO: recognise dangerous spawn points for grasshoppers etc
	double ReserveWeight = 4;
	double reserve_weight[] = 
		{ 
		0.0,			// queen
		0.6,			// ant	
		0.25,			// grasshopper
		0.3,			// beetle
		0.1,			// spider
		0.5,			// mosquito
		0.2,			// ladybug
		0.1,			// original pillbug
		-1.0,			// pillbug, combined with queen affinity gets it DOWN
		0.2};			// blank
	
    double PinPenaltyWeight = 6;
    // note that if these penalties are too large, you can 
    // end up in pin/unpin loops
	double pinPenalty[] = 
		{ 
		0.0,			// queen
		0.15,			// ant	
		0.15,			// grasshopper
		0.15,			// beetle
		0.01,			// spider
		0.25,			// mosquito
		0.15,			// ladybug
		0.1,			// original pillbug
		0.05,			// pillbug, combined with queen affinity gets it DOWN
		0.0};			// blank
	// these should be mostly the same as pin penalty, except for the pillbug
	// which is only deactivated by being buried
	double burialPenalty[] = 
		{ 
		0.5,			// queen
		0.125,			// ant	
		0.15,			// grasshopper
		0.15,			// beetle
		0.04,			// spider
		0.25,			// mosquito
		0.15,			// ladybug
		1,				// original pillbug
		1,				// pillbug, combined with queen affinity gets it DOWN
		0.0};			// blank
	

	// when a piece is mobile, these are bonuses apply
	// depending on the number of moves available up to a
	// limit.  this makes "more mobility" better
	// 
	boolean ringCheck = true;
	double MobilityScale = 1.0;		// too much emphasis on mobility makes rings look like a brilliant idea
										// see the endgame of start-09
	double piece_mobility_weight[] = 
		{2.0,			// queen
		3.0,			// ant	
		1.0,			// grasshopper
		3.0,			// beetle
		1,				// spider
		3.0,			// mosquito
		1.0,			// ladybug
		1.0,			// original pillbug
		0.0,			// pillbug
		0.2};			// blank
	
	int piece_maximum_mobility[] = 
		{1,			// queen
		10,			// ant	
		6,			// grasshopper
		3,			// beetle
		2,			// spider
		10,			// mosquito
		10,			// ladybug
		6,			// original pillbug
		6,			// pillbug
		5};			// blank
	
	private double MaxQueenSafety = 150;
	private double QueenDropCount = 0.25;	// count for drop space adjacent to enemy Q. 
											// if too high this will make the bot treat the spaces as "as good as filled" 
											// refer to the standard development of start-07-s.  If too low, the opportunity
											// to get into drop position goes unused
											// refer to puzzle game "queendrop"
	private double immuneToPillbugBonus = 5.0;
	private double enemyBeetleAtQueenCount = 0.75;
	private double enemyBeetleAtQueenAdjacentCount = 0.25;
	private double friendlyPillbugAtQueenCount = 0.75;	// partial credit for a friendly pillbug
	private double friendlyMobileAtQueenCount = 0.25;	// partial credit for friendlies that can be moved
	private double QueenMobileCount = 0.5;	// penalty to queen crowding if it is mobile, fractions of a piece, not points
	double queenDangerPoints[] = { 0.0, 5, 10.0, 15.0, 40.0, 80.0, 120,130,140,MaxQueenSafety};
	double queenAttackWeight = 1.0;
	
	double queenAdjacentValue = 1;	// all pieces get this when adjacent to the Q
	double FutureDistanceScale = 1;
	double PresentDistanceScale = 1;
	double present_queen_distance_multiplier[] =
		{0.0,			// queen
		 0.75,			// ant	
		 1.0,			// grasshopper
		 3.0,			// beetle
		 1.0,			// spider
		 1.0,			// mosquito
		 1.5,			// ladybug
		 1.0,			// original pillbug
		 0.0,			// pillbug
		 0.2};			// blank
	double future_queen_distance_multiplier[] =
		{0.0,			// queen
		 0.75,			// ant	
		 1.0,			// grasshopper
		 3.0,			// beetle
		 1.0,			// spider
		 1.0,			// mosquito
		 1.5,			// ladybug
		 1.0,			// original pillbug
		 0.0,			// pillbug
		 0.2};			// blank

	public double[] getWeights()
	{	DStack v = new DStack();
		v.push(pillbugSafeBonus);
		v.push(pillbug_queen_afinity_weight);
		v.push(QueenUnderBonus);
		for(double d : piece_mobility_weight) { v.push(d); }
		for(double d : queenDangerPoints) { v.push(d); }
		for(double d : present_queen_distance_multiplier)  { v.push(d); }
		return(v.toArray());
	}
	public void setWeights(double v[])
	{	int idx = 0;
		pillbugSafeBonus = v[idx++];
		pillbug_queen_afinity_weight = v[idx++];
		QueenUnderBonus = v[idx++];
		for(int i=0;i<piece_mobility_weight.length;i++) {  piece_mobility_weight[i] = v[idx++]; }
		for(int i=0;i<queenDangerPoints.length;i++) { queenDangerPoints[i] = v[idx++]; }
		for(int i=0;i<present_queen_distance_multiplier.length;i++)  { present_queen_distance_multiplier[i]=v[idx++]; }
		// check
		//double rv[] = getWeights();
		//G.Assert(G.sameArrayContents(v, rv),"check setWeights failed");
	}
	
    public double scoreDropDests(HiveGameBoard board,HiveId targetColor)
    {	
     	int sweep = ++board.sweep_counter;
     	CellStack occupiedCells = board.occupiedCells;
     	int sz = occupiedCells.size();
     	int ndrops = 0;
     	switch(sz)
     	{
    	case 1:	// second move
       	case 0:	// first move
      		return someDropBonus;
      	default:
	     		
	   		for(int i=0,lim=sz;i<lim;i++)
			{	HiveCell c = occupiedCells.elementAt(i);
				for(int dir=0;dir<6;dir++)
					{ HiveCell ca = c.exitTo(dir);
					  if((ca.sweep_counter!=sweep) && board.legalDropDest(ca,targetColor)) 
					  	{
						  ndrops++;
					  	}
					}
			}
	   		break;
     	}
     	// a tiny bonus for more drops.
     	return ndrops==0 ? 0 : someDropBonus + 0.001*ndrops;
    }
    private double pocketScoreWeight = 0.08;	// group r is 0.12
    private static double curve = 0.15;
    private static double pocket = 0.4;
    private static double gate = 1.5;
    private static double surround = 1.5;
    private static double flat = 0.0;
    private static double open = 0.0;
    private static double pocketScore[] = new double[32];

    static {
    	//  not all completely open, but rare
    	for(int i=0;i<pocketScore.length;i++) { pocketScore[i]=open;}
    	
    	pocketScore[0b11111] = surround;
    	
    	pocketScore[0b11110] = gate;
    	pocketScore[0b11101] = gate;
    	pocketScore[0b11011] = gate;
    	pocketScore[0b10111] = gate;
    	pocketScore[0b01111] = gate;
    	
    	pocketScore[0b11100] = pocket;
    	pocketScore[0b11001] = pocket;
    	pocketScore[0b10011] = pocket;
    	pocketScore[0b00111] = pocket;
    	
    	pocketScore[0b11000] = curve;
    	pocketScore[0b01100] = curve;
    	pocketScore[0b00110] = curve;
    	pocketScore[0b00011] = curve;
    	
    	pocketScore[0b10000] = flat;
    	pocketScore[0b01000] = flat;
    	pocketScore[0b00100] = flat;
    	pocketScore[0b00010] = flat;
    	pocketScore[0b00001] = flat;
   	
    }
    public double pocketScore(HiveCell loc, int direction)
    {
    	int mask = 0;
    	for(int firstdir = direction-2,lastdir = direction+2; firstdir<=lastdir; firstdir++)
    	{
    		HiveCell d = loc.exitTo(firstdir);
    		mask = mask<<1;
    		if(d.height()>0) { mask = mask|1; }
    	}
    	return pocketScore[mask];
    }
	// return the number of adjacent cells owned by a player
	public double nOccupiedOrOwnedAdjacent(HiveGameBoard board,HiveCell queen, int targetPlayer)
	{	
		boolean myQueenAdj = false;
		int pillbugAdjacentSimple = 0;
		HiveId targetColor = board.playerColor(targetPlayer);
		HivePiece bug = queen.topChip();
		boolean enemyBeetle = bug.color!=targetColor;
		int rawCount = 0;
		double credits = 0;
		
		if(enemyBeetle) { credits += enemyBeetleAtQueenCount; }	// someone else on top
		HiveCell prev = null;
		for(int direction=5;direction>=0;direction--)
		{ HiveCell c = queen.exitTo(direction);
		  int h = c.height();
		  if(h>0) 
		  	{
		  	  rawCount++;
		  	  HivePiece top = c.topChip();
		  	  if(top.color==targetColor) 
		  	  	{ 
		  		  if(!myQueenAdj) { myQueenAdj = board.legalDests(c,false,top,null,null,targetPlayer,true); }
		  		  if( ( (top.type==PieceType.PILLBUG)
		  				  ||((top.type==PieceType.MOSQUITO) && c.actingAsType(PieceType.PILLBUG))))
		  			  { 
		  			  // that's good if it has a flip target
		  			  pillbugAdjacentSimple++;
		  			  }
		  	  	}
		  	  else if((h==1) 
		  			  && (top.type==PieceType.BEETLE 
		  			  		|| (top.type==PieceType.MOSQUITO && c.actingAsType(PieceType.BEETLE)))
		  			  && board.legalDests(c,false,top,null,null,targetPlayer^1,true))
		  	  {
		  		  // enemy beetle adjacent to Q, can mount!
		  		  credits += enemyBeetleAtQueenAdjacentCount;
		  	  }
		  	  else if(h>1)
		  	  {	// something acting as a beetle
		  		// must be adjacent to an empty to count
		  		HiveCell l1 = queen.exitTo(direction-1);
		  		HiveCell l2 = prev!=null ? prev : queen.exitTo(direction+1);
		  		if((l1.height()==0) || (l2.height()==0))
		  		  	{ // partial credit for a bombing position
		  			credits += enemyBeetleAtQueenCount;
		  			}		  		  	
		  	  }	
		  	}
		  else
		  {	  // vacant space next to queen
			  if(enemyBeetle)
			  {	// extra danger if the queen is topped by an enemy beetle
				  if(board.legalDropDest(c,board.playerColor(targetPlayer^1)))
				  {
					  credits +=QueenDropCount;	// drop destination directly next to Q
				  }
			  }
			  double pocketScore = pocketScoreWeight*pocketScore(c,direction);
			  credits -= pocketScore;
			  //G.print("pocket "+pocketScore);	
		  }
		  prev = c;
		}

		if(rawCount<5)
		{
		// don't give credits for pillbug and mobility if the real count is 5.
		if(myQueenAdj) { credits -= friendlyMobileAtQueenCount; }
		if(pillbugAdjacentSimple>0) 
			{	
			    credits -= friendlyPillbugAtQueenCount;
			}
		 
		}
		return(rawCount+credits);
	}	

	public double evaluate(BoardProtocol boardp,int pl,boolean print)
	{ 	HiveGameBoard board = (HiveGameBoard)boardp;
		HiveCell oql = board.pieceLocation.get(board.playerQueen(pl^1));
		HiveCell myQueenLoc = board.pieceLocation.get(board.playerQueen(pl));
		HiveId targetColor = board.playerColor(pl);
//		if(pl==0) { return -100; }
		double plusPoints = 0.0;
		double minusPoints = 0.0;
		double ringPoints = 0.0;	// total ring values
		double ring1Points = 0.0;	// best ring value
		double ring2Points = 0.0;	// secondbest
		int ringCount = 0;
		String msg = "";
		int totalChoices = 0;
		// score the pieces still in the rack
		{
		boolean some = false;			// some bug is on the rack
		HiveCell rack[] = board.rackForPlayer(pl);
		for(int lim = rack.length-1; lim>=0; lim--)
		{	HiveCell c = rack[lim];
			int h = c.height();
			if(h>0)
			{	some = true;
				double v = reserve_weight[lim]*h*ReserveWeight;
				if(print) { msg += c.topChip()+"="+v;}
				plusPoints += v;
			}
		}
		
		if(some)
			{ // a bonus or deficit if there's no place to put a bug
			  double nv = scoreDropDests(board,targetColor);
			  if(print) { msg += "res "+" drop="+nv;}
			  plusPoints += nv;
			}
		}
		
		// score pieces on the board
		
		boolean distance = (oql!=null) && oql.onBoard;
		if(distance)
		{
		CellStack tempDests = board.getTempDest();
		for(int i=0,lim=board.occupiedCells.size(); i<lim; i++)
			{	HiveCell loc = board.occupiedCells.elementAt(i);
				int height = loc.height();
				double plusPoints0 = plusPoints;
				double minusPoints0 = minusPoints;
				for(int position=0;position<height;position++)
				{
				HivePiece bug = loc.chipAtIndex(position);
				boolean top = position+1==height;
				PieceType bugtype = bug.type;
				if(board.pieceTypeIncluded.test(bugtype))
				{
				int pieceordinal = bugtype.ordinal();
				if(bug.color==targetColor)
				{	
					int crude_queen_distance = board.hex_cell_distance(oql,loc);
					int mobile_queen_distance = crude_queen_distance;
					if(print) { msg += " "+bug.exactBugName()+"["; }
					double mobilescale = FutureDistanceScale*future_queen_distance_multiplier[pieceordinal];
					if(top)
					{
						tempDests.clear();
						

						if(height>=2 && crude_queen_distance<=1)
						{ // mobile and on top, but not on a stack of beetles, and near the q
						  msg += " Top="+beetleOnTopBonus;
						  plusPoints += beetleOnTopBonus;
						}
	
						// score bugs based on where they can get
						if(crude_queen_distance<=1)
						{
						// always score pieces adjacent to the Q as maximally mobile at a distance of 2
						// so it doesn't lose score for hitting the target
						boolean some = board.legalDests(loc,false,bug,null,null,pl,true);
						int nq = piece_maximum_mobility[pieceordinal];
						// except for beetles and mosquitos that can act as beetles, which can meaningfully
						// be pinned even against the Q
						if(!some)
						{ // immobile pieces
						  if(bugtype==PieceType.BEETLE
							|| ((bugtype==PieceType.MOSQUITO) && loc.actingAsType(PieceType.BEETLE)))
							{
							// also apply the pin penalty
							nq = 0;
							double pinmul = pinPenalty[pieceordinal]*PinPenaltyWeight;
							if(print) { msg += " pin -"+pinmul; }
							minusPoints += pinmul;
							}
						}
						double nv = mobilescale*nq/2;
						totalChoices += nv;
						if(print) { msg += "qmob "+nq+" "+nv+" qa "+queenAdjacentValue; }
						plusPoints += nv+queenAdjacentValue;
						}
						else
						{
						//
						// not adjacent to queen
						//
						boolean some = board.legalDests(loc,false,bug,tempDests,null,pl,false);
						int nmoves = tempDests.size();
						if(some)
						{	
						//
						// mobile and not adjacent to queen
						// score for approaching Q
						double startingPoints = plusPoints;
						
						double mobilemul = piece_mobility_weight[pieceordinal];

						double stepval = PresentDistanceScale*present_queen_distance_multiplier[pieceordinal]/Math.max(1,crude_queen_distance);
						plusPoints += stepval;
						if(print) { msg += stepval; }
				
						// sibmo detects the case where a pair of pieces are both mobile, but if either
						// of them actually moves, the other becomes immobile. 
						totalChoices += Math.min(piece_maximum_mobility[pieceordinal],nmoves);	// avoid making ants look absurdly powerful
						// pieces that have already reached the Q get fullmarks for mobility
						HiveCell sibmo = board.sibMobileAny(loc);
						if(sibmo!=null)
							{
							 HivePiece alt = sibmo.topChip();
							 double altmul = PresentDistanceScale*piece_mobility_weight[alt.type.ordinal()];
							 double sum = mobilemul+altmul;
							 if(sum>0)
							 {
							 double factor = G.interpolateD(mobilemul/sum,0,1);
							 // split the mobility proportionally between the pair, according
							 // to the importance of the mobility
							 if(print) { msg+=" sibmobile*"+factor; }
							 mobilemul *= factor;
							 }
							}
						mobilemul *= MobilityScale;
						boolean ring = ringCheck && board.isRing(loc);

						if(print) { msg += " mob "+(ring ? "ring " : "")+mobilemul; }
						
						if(height>1)
						{
							if(print) { msg += " up "+upBonus; }
							plusPoints += upBonus;
						}
						plusPoints += mobilemul;
						for(int dis = nmoves-1; dis>=0; dis--)
						{
							mobile_queen_distance = Math.min(mobile_queen_distance,board.hex_cell_distance(oql,tempDests.elementAt(dis)));
						}
	
						
						double mstepval = mobilescale/Math.max(1,mobile_queen_distance);
						if(print) { msg += "+"+mstepval; }
						plusPoints += mstepval;
						
						if(ring)
						{
							ringCount++;
							double points = plusPoints - startingPoints; 
							ringPoints += points;
							// keep track of the top 2
							if(points>ring1Points) { ring2Points = ring1Points; ring1Points = points; }
							else if(points>ring2Points) { ring2Points = points; }
						}
						}
						else
						{	// a piece pinned bug not against the q
							double pinmul = pinPenalty[pieceordinal]*PinPenaltyWeight;
							if(print) { msg += " pin -"+pinmul; }
							minusPoints += pinmul;
						}}
						
					if(bug.isPillbug())
					{	if(!loc.isAdjacentToDanger(targetColor))
						{
							if(print) { msg += " saf="+pillbugSafeBonus; }
							plusPoints += pillbugSafeBonus;
						}
						int myQdis = board.hex_cell_distance(myQueenLoc,loc);
						switch(myQdis)
						{
						default:
						case 0: break;
						case 1: 
							if(print) { msg += " Qadj="+pillbug_queen_afinity_weight; }
							plusPoints += pillbug_queen_afinity_weight;
							break;
						case 2:
							if(print) { msg += " Qadj="+pillbug_queen_afinity_weight/2; }
							plusPoints += pillbug_queen_afinity_weight/2;
						}
					}
					
					} // end of our piece on top
					
					else 
					{	// our piece, buried
						
					if(crude_queen_distance<=1)
					{
						for(int idx = loc.geometry.n-1; idx>=0; idx--)
						{
							HiveCell adj = loc.exitTo(idx);
							HivePiece atop = adj.topChip();
							if( (atop!=null) 
									&& (atop.color!=targetColor) 
									&& ((atop.type==PieceType.PILLBUG)
											|| ((atop.type==PieceType.MOSQUITO)
													&& adj.isAdjacentToPillbug())))
								{
									if(print) { msg += " immune "; }
									plusPoints += immuneToPillbugBonus;
								}
							}
						if(bugtype==PieceType.BEETLE || bugtype==PieceType.MOSQUITO)
							{
							double pen = burialPenalty[pieceordinal]*PinPenaltyWeight;
							if(print) { msg += " buried "+pen; }
							minusPoints += pen;
							}
						}
					else
					{
						// buried bugs get the same penalty as pinned bugs. If more the beetles won't leave!
						double pen = burialPenalty[pieceordinal]*PinPenaltyWeight;
						if(height==2 
								&& (position == 0)
								&& (loc.topChip().color == bug.color)
								// if it would have been mobile
								&& board.legalDests(loc,true,bug,null,null,pl^1,true)
								)
							{	msg += " half ";
								pen = pen/2;	// pinned by our own beetle, less severe
							}
							if(print) { msg += " under="+ pen+" "; }
							minusPoints += pen;
						}
					} // end of our bug buried
					
					if(print) { msg += "="+((plusPoints-minusPoints)-(plusPoints0-minusPoints0))+"]"; }
				}	// end of our bug
				}	
				}
				}
		board.returnTempDest(tempDests);

		}
		if(ringCount>2)
		{	// the current theory is that the point value associated with rings is illusory, so if
			// there are more than 2 mobile bugs in a ring, only the top 2 are credited.  This prevents
			// wild swings in the evaluator as rings are formed and destroyed.
			double adj = (ringPoints - ring1Points - ring2Points);
			plusPoints -= adj;
			if(print) { msg += " RING "+ringCount+" -"+adj; }
		}
		if(oql.onBoard)
		{	// evaluate queen safety, considering friendlies that can move away
			double occ = nOccupiedOrOwnedAdjacent(board,oql,pl^1);		
			HivePiece top = oql.topChip();
			boolean isOnTop = (oql.topChip().type==PieceType.QUEEN);
			boolean mobile = (top.type==PieceType.QUEEN)
								&& board.legalDests(oql,false,oql.topChip(),null,null,pl^1,true);
			if(mobile)
			{	// cozying up the the queen isn't as effective if its mobile
				occ -= QueenMobileCount;
			}
			if(occ>0)
			{
			double topval = isOnTop?0.0:QueenUnderBonus;
			int na = (int)occ;
			double frac = occ-na;
			double saf = queenDangerPoints[na];
			if(frac>0.0)
			{
				saf += frac*(queenDangerPoints[na+1]-saf);
			}
			saf *= queenAttackWeight;
			if(print) { msg += " qsaf "+occ+"="+(saf+topval); }
			plusPoints += (saf + topval);
			}
		}
		if(totalChoices==0) 
			{ if(print) { msg += "no moves -"+shutoutPenalty; }
			  minusPoints += shutoutPenalty; 
			}
		else {
			double dwe = totalChoices*totalChoicesWeight;
			plusPoints += dwe;
			if(print) { msg += " choices="+dwe; }
		}
		if(print) { msg += " f= "+(plusPoints-minusPoints); System.out.println(msg); }
		//
		// this is a numerical accuracy hack.  Observed - when we get to the same variation
		// from 2 different paths, the numbers are added in a different sequence resulting
		// in floating point loss of precision, "way down" in the 12'th digit.  We ought to
		// consider them the same, which with cause the alpha-beta search to choose the first,
		// which was higher valued at a higher level.  This is an attempt to hack around the
		// numerical fluke
		long v = Math.round(plusPoints*10000)-Math.round(minusPoints*10000);
		return(v/10000.0);
	}
	

	public boolean pushAnalysis(BoardProtocol boardp,boolean depthLimit,commonMove m)
	{	
		if(depthLimit)
		{	HiveGameBoard board = (HiveGameBoard)boardp;
			HiveCell myQueenLoc = board.pieceLocation.get(board.playerQueen(board.whoseTurn^1));
			if(myQueenLoc!=null)
			{
				int n = myQueenLoc.nOccupiedAdjacent();
				if(n==5)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	// 
	// this is an experiment that doesn't work reliably to find wins when we're clearly winning.
	// In particular the endgame
	// of start-13, trust results in a looped draw when there are several obvious
	// wins.  Maybe this could be salvaged if it only kicks in when there are repetitions
	// SPRINT heuristic seems to be better
	//
	public commonMove gutAnalysis(CommonDriver search_state,double eval,commonMove m, int repetitions)
	{ 
      commonMove move = m;  
  	  if(repetitions>1 && Math.abs(eval)<trustYourGutTrigger)
  	  {
  		  double finalEval = move.evaluation();
  		  commonMove altBest = null;
  		  double altBestEval = 0;
  		  if(Math.abs(eval-finalEval)<trustYourGutThreshold)	// going nowhere
  		  {
  			  // if we're winning and not making progress, use the static best rather than the evaluated best
  			  // this is an anti-draw heuristic
  			  commonMove top[] = search_state.top_level_moves();
  			  for(int i=0;i<top.length;i++)
  			  {
  				  commonMove alt = top[i];
  				  if(!alt.Same_Move_P(move))
  				  {
  				  double altEval = alt.evaluation();
  				  double altLocal = alt.local_evaluation();
  				  
  				  if(Math.abs(altEval-eval)<trustYourGutThreshold)	// not a terrible move, we already know none are winners
  				  {	
  					  if(altBest==null || altLocal>altBestEval)
  					  {   // pick the move with the best top level evaluation that isn't a proven loser
  						  //G.print("trust ",alt,altLocal);
  						  altBest = alt;
  						  altBestEval = altLocal;
  					  }
  				  }}
  			  }
  		  if(altBest!= null) 
  		  {	  G.print("using TRUST to play ",altBest," instead of ",move);
  			  move = (Hivemovespec)altBest;
  		  }
  		  }
  	  }
  	  return move;
	}
	/*
	 * 
	 * Training Notes for version SU
	 * 
	 * start-16 ends in a situation where black is static with 2 remaining, and all the beetles
	 * are stacked.  White is in a shutout with the Q protected by a pillbug.  There is a simple
	 * plan that wins: take several moves to give bM mosquito power, march it to the P then close,
	 * but no way in hell that's going to be found by any hill climbing bot.
	 * 
	 */
}