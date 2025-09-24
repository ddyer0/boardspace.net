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
class NolookEvaluator extends DefaultEvaluator implements Evaluator
{	

	public int canRandomize(BoardProtocol b,int who)
	{	HiveGameBoard board = (HiveGameBoard)b;
		HiveCell myQueenLoc = board.pieceLocation.get(board.playerQueen(who));
		HiveCell hisQueenLoc = board.pieceLocation.get(board.playerQueen(who^1));
		return !hisQueenLoc.onBoard && !myQueenLoc.onBoard && (board.getState()!=HiveState.QUEEN_PLAY_STATE)
				? (30 - board.moveNumber) 
				: 0; 
	}

	private double trustYourGutThreshold = 50;			// full evaluation changes less than this 
	private double trustYourGutTrigger = 100;			// absolute score more than this
	
	// simple evaluation based on piece mobility and importance
	private double pillbugSafeBonus = 5.0;	// pillbugs untroubled by beetles or other pillbugs
	private double totalChoicesWeight = 0.05;			// benefit of any choice at all.
	private double pillbug_queen_afinity_weight = 6.0;	// bonus for pillbug adjacent or next to adjacent to Q
	private double QueenUnderBonus = 5.0;		// bonus for burying the opposing Q
	private double beetleOnTopBonus = 2.5;		// bonus for beetle on top.  Making this greater is a bad idea
	
	
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
		0.0,			// spider
		0.5,			// mosquito
		0.2,			// ladybug
		0.1,			// original pillbug
		-1.0,			// pillbug, combined with queen affinity gets it DOWN
		0.2};			// blank
	
    private double PinPenaltyWeight = 4;
 	// these should be mostly the same as pin penalty, except for the pillbug
	// which is only deactivated by being buried
	double burialPenalty[] = 
		{ 
		0.5,			// queen
		0.125,			// ant	
		0.15,			// grasshopper
		0.15,			// beetle
		0.04,			// spider
		0.15,			// mosquito
		0.15,			// ladybug
		4,				// original pillbug
		4,				// pillbug, combined with queen affinity gets it DOWN
		0.0};			// blank
	
	
	private double MaxQueenSafety = 150;
	private double friendlyMobileAtQueenCount = 1;	// partial credit for friendlies that can be moved
	private double QueenMobileCount = 0.5;	// penalty to queen crowding if it is mobile, fractions of a piece, not points
	double queenDangerPoints[] = { 0.0, 5, 10.0, 15.0, 40.0, 80.0, 120,130,140,MaxQueenSafety};

	
	double queenAdjacentValue = 1;	// all pieces get this when adjacent to the Q
	double FutureDistanceScale = 1;
	double PresentDistanceScale = 1;
	double SibmobileScale = 0.5;
	private double present_queen_distance_multiplier[] =
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
		for(double d : queenDangerPoints) { v.push(d); }
		for(double d : present_queen_distance_multiplier)  { v.push(d); }
		return(v.toArray());
	}
	public void setWeights(double v[])
	{	int idx = 0;
		pillbugSafeBonus = v[idx++];
		pillbug_queen_afinity_weight = v[idx++];
		QueenUnderBonus = v[idx++];
		for(int i=0;i<queenDangerPoints.length;i++) { queenDangerPoints[i] = v[idx++]; }
		for(int i=0;i<present_queen_distance_multiplier.length;i++)  { present_queen_distance_multiplier[i]=v[idx++]; }
		// check
		//double rv[] = getWeights();
		//G.Assert(G.sameArrayContents(v, rv),"check setWeights failed");
	}
	
    public double countDropDests(HiveGameBoard board,HiveId targetColor,HiveCell q)
    {	
     	int sweep = ++board.sweep_counter;
     	CellStack occupiedCells = board.occupiedCells;
     	int sz = occupiedCells.size();
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
						  return someDropBonus;
					  	}
					}
			}
	   		break;
     	}
     	return 0;
    }
	// return the number of adjacent cells owned by a player
	public double nOccupiedOrOwnedAdjacent(HiveCell queen, HiveId targetColor)
	{	double n=0;
		int myQueenAdj = 0;
		boolean pillbugAdjacent = false;
		HivePiece bug = queen.topChip();
		if(bug.color!=targetColor) { n += 0.75; }	// someone else on top
		HiveCell prev = null;
		for(int lim=5;lim>=0;lim--)
		{ HiveCell c = queen.exitTo(lim);
		  int h = c.height();
		  if(h>0) 
		  	{ n++; 
		  	  HivePiece top = c.topChip();
		  	  if(top.color==targetColor) 
		  	  	{ myQueenAdj++; 
		  	  	  pillbugAdjacent |= top.type==PieceType.PILLBUG;
		  	  	}
		  	  else if(h>1)
		  	  {	// something acting as a beetle
		  		// must be adjacent to an empty to count
		  		HiveCell l1 = queen.exitTo(lim-1);
		  		HiveCell l2 = prev!=null ? prev : queen.exitTo(lim+1);
		  		if((l1.height()==0) || (l2.height()==0))
		  		  	{ // partial credit for a bombing position
		  			n+= 0.75;
		  			}		  		  	
		  	  }	
		  	}
		  prev = c;
		}
		if(myQueenAdj>0)
		{ double dif = (pillbugAdjacent ? 1.0 : friendlyMobileAtQueenCount);
		  n -= dif;
		}	
		return(n);
	}	

	public double evaluate(BoardProtocol boardp,int pl,boolean print)
	{ 	HiveGameBoard board = (HiveGameBoard)boardp;
		HiveCell oql = board.pieceLocation.get(board.playerQueen(pl^1));
		HiveCell myQueenLoc = board.pieceLocation.get(board.playerQueen(pl));
		HiveId targetColor = board.playerColor(pl);
		HiveId otherColor = board.playerColor(pl^1);
//		if(pl==0) { return -100; }
		double plusPoints = 0.0;
		double minusPoints = 0.0;
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
			  double nv = countDropDests(board,targetColor,oql);
			  if(print) { msg += "res "+" drop="+nv;}
			  plusPoints += nv;
			}
		}
		
		// score pieces on the board
		
		boolean distance = (oql!=null) && oql.onBoard;
		if(distance)
		{
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
					if(print) { msg += " "+bug.exactBugName()+"["; }
					if(top)
					{
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
						if(print) { msg += " qa "+queenAdjacentValue; }
						plusPoints += queenAdjacentValue;
						}
						else
						{
							double stepval = PresentDistanceScale*present_queen_distance_multiplier[pieceordinal]/Math.max(1,crude_queen_distance);
							plusPoints += stepval;
							if(print) { msg += stepval; }
						}
					}
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
					
					if(print) { msg += "="+((plusPoints-minusPoints)-(plusPoints0-minusPoints0))+"]"; }
				}
				else // opponent color
				{
					if(top)
					{
					}
					else
					{	// buried bugs get the same penalty as pinned bugs. If more then the beetles won't leave!
						double pen = burialPenalty[pieceordinal]*PinPenaltyWeight;
						if(print) { msg += " under=-"+ pen; }
						plusPoints += pen;

					}
				}
				}	
				}
				}
		}
		
		if(oql.onBoard)
		{	// evaluate queen safety, considering friendlies that can move away
			double occ = nOccupiedOrOwnedAdjacent(oql,otherColor);		
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
		return(plusPoints-minusPoints);
	}
	

	public boolean pushAnalysis(BoardProtocol boardp,boolean depthLimit,commonMove m)
	{	
		if(depthLimit)
		{	HiveGameBoard board = (HiveGameBoard)boardp;
			HiveCell myQueenLoc = board.pieceLocation.get(board.playerQueen(board.whoseTurn));
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
	
	public commonMove gutAnalysis(CommonDriver search_state,double eval,commonMove m, int repetitions)
	{ 
      commonMove move = m;  
  	  if(eval>=trustYourGutTrigger)
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
  				  }
  			  }
  		  if(altBest!= null) 
  		  {	  G.print("using TRUST to play ",altBest," instead of ",move);
  			  move = (Hivemovespec)altBest;
  		  }
  		  }
  	  }
  	  return move;
	}
}