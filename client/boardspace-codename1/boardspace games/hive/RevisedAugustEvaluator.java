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
class RevisedAugustEvaluator extends DefaultEvaluator implements Evaluator
{	

	public int canRandomize(BoardProtocol b,int who)
	{	HiveGameBoard board = (HiveGameBoard)b;
		HiveCell myQueenLoc = board.pieceLocation.get(board.playerQueen(who));
		return !myQueenLoc.onBoard && (board.getState()!=HiveState.QUEEN_PLAY_STATE)
				? (30 - board.moveNumber) 
				: 0; 
	}

	private double trustYourGutThreshold = 50;			// full evaluation changes less than this 
	private double trustYourGutTrigger = 100;			// absolute score more than this
	
	// simple evaluation based on piece mobility and importance
	private double pillbug_fear_weight = -5.0;	// fear of beetles by pillbugs
	private double totalChoicesWeight = 0.05;			// benefit of any choice at all.
	private double pillbug_queen_afinity_weight = 6.0;
	private double queen_under_penalty = 5.0;
	private double QueenCrowdingWeight = -4.0;
	private double pillbugEmptyWeight = -0.1;	// penalty for pillbug adjacent filled
	private double beetleOnTopWeight = 2.5;		// bonus for beetle on top.  Making this greater is a bad idea
	private double nodropPenalty = 5;			// penalty for having no way to play in
	private double shutoutPenalty = 25;			// penalty for no moves. If this is too high we'll never abandon a shutout
	// value of a piece still in the rack.  This is to counter the "just play it" impulse
	// and punish playing pieces that are immediately pinned.
	// TODO: next experiment, increase the grasshopper reserve, based on start-04-k-win where the grasshoppers
	// were deployed even though they would be pinned
	// TODO: recognise dangerous spawn points for grasshoppers etc
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

	// when a piece is mobile, these are bonuses apply
	// depending on the number of moves available up to a
	// limit.  this makes "more mobility" better
	// 
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
	
	private double friendlyMobileAtQueen = 0.5;	// partial credit for friendlies that can be moved
	// TODO negate the values and the uses, avoid future negative number screwups
	double queen_safety[] = { 0.0, -5, -10.0, -15.0, -40.0, -65.0, -120,-130,-140,-150};
	// these are penalties that apply depending on the
	// distance from the enemy queen.  This is the main motivator
	// to home in on the enemy queen.
	
	double burial_penalty[] =
		{2.0,			// queen
		 0.5,			// ant	
		 3.0,			// grasshopper
		 5.0,			// beetle
		 0.13,			// spider
		 5.0,			// mosquito
		 3.0,			// ladybug
		 1.0,			// original pillbug
		 15.0,			// pillbug
		 0.0};			// blank

	double queenAdjacentValue = 1;	// all pieces get this when adjacent to the Q
	double present_queen_distance_multiplier[] =
		{0.0,			// queen
		 0.75,			// ant	
		 1.0,			// grasshopper
		 3.0,			// beetle
		 1.0,			// spider
		 1.0,			// mosquito
		 0.5,			// ladybug
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
		 1.0,			// ladybug
		 1.0,			// original pillbug
		 0.0,			// pillbug
		 0.2};			// blank

	public double[] getWeights()
	{	DStack v = new DStack();
		v.push(pillbug_fear_weight);
		v.push(pillbug_queen_afinity_weight);
		v.push(queen_under_penalty);
		v.push(QueenCrowdingWeight);
		v.push(pillbugEmptyWeight);
		for(double d : piece_mobility_weight) { v.push(d); }
		for(double d : queen_safety) { v.push(d); }
		for(double d : present_queen_distance_multiplier)  { v.push(d); }
		return(v.toArray());
	}
	public void setWeights(double v[])
	{	int idx = 0;
		pillbug_fear_weight = v[idx++];
		pillbug_queen_afinity_weight = v[idx++];
		queen_under_penalty = v[idx++];
		QueenCrowdingWeight = v[idx++];
		pillbugEmptyWeight = v[idx++];
		for(int i=0;i<piece_mobility_weight.length;i++) {  piece_mobility_weight[i] = v[idx++]; }
		for(int i=0;i<queen_safety.length;i++) { queen_safety[i] = v[idx++]; }
		for(int i=0;i<present_queen_distance_multiplier.length;i++)  { present_queen_distance_multiplier[i]=v[idx++]; }
		// check
		//double rv[] = getWeights();
		//G.Assert(G.sameArrayContents(v, rv),"check setWeights failed");
	}

	public double evaluate(BoardProtocol boardp,int pl,boolean print)
	{ 	HiveGameBoard board = (HiveGameBoard)boardp;
		HiveCell oql = board.pieceLocation.get(board.playerQueen(pl^1));
		HiveCell myQueenLoc = board.pieceLocation.get(board.playerQueen(pl));
		HiveId targetColor = board.playerColor(pl);

		double val = 0.0;
		String msg = "";
		int totalChoices = 0;
		long dig = board.Digest();
		if(dig==-4830838618191031855L)
		{
		System.out.println("Dig "+dig);
		}
		// score the pieces still in the rack
		{
		boolean some = false;			// some bug is on the rack
		HiveCell rack[] = board.rackForPlayer(pl);
		for(int lim = rack.length-1; lim>=0; lim--)
		{	HiveCell c = rack[lim];
			int h = c.height();
			if(h>0)
			{	some = true;
				double v = reserve_weight[lim]*h*4;
				if(print) { msg += c.topChip()+"="+v;}
				val += v;
			}
		}
		
		if(some)
			{ // a bonus or deficit if there's no place to put a bug
			  int nd = board.countDropDests(targetColor);
			  double nv = nd==0 ? -nodropPenalty : nodropPenalty;
			  if(print) { msg = "res "+msg+" = "+val+"  drop "+nd+"="+nv;}
			  val += nv;
			}
			else
			{	// nothing left to drop, so give the bonus anyway for consistency of the numbers
				double nv = nodropPenalty;
				if(print) { msg = "res "+msg+" = "+val+"  drop "+0+"="+nv;}
			}
		}
		
		// score pieces on the board
		
		int myQueenAdjacent = 0;		// number of my pieces adjacent to my queen that are mobile
		boolean pillbugAdjacent = false;
		boolean distance = (oql!=null) && oql.onBoard;

		if(distance)
		{
		CellStack tempDests = board.getTempDest();
		for(int i=0,lim=board.occupiedCells.size(); i<lim; i++)
			{	HiveCell loc = board.occupiedCells.elementAt(i);
				int height = loc.height();
				double val0 = val;
				for(int position=0;position<height;position++)
				{
				HivePiece bug = loc.chipAtIndex(position);
				boolean top = position+1==height;
				PieceType bugtype = bug.type;
				if(bug.color==targetColor && board.pieceTypeIncluded.test(bugtype))
				{	int pieceordinal = bugtype.ordinal();
					int crude_queen_distance = board.hex_cell_distance(oql,loc);
					int mobile_queen_distance = crude_queen_distance;
					if(print) { msg += " "+bug.exactBugName()+"["; }
					double mobilescale = future_queen_distance_multiplier[pieceordinal];
					if(top)
					{
						tempDests.clear();
						boolean some = board.legalDests(loc,false,bug,tempDests,null,pl,false);

						if(height>=2 && (loc==oql || loc.isAdjacentTo(oql)))
						{ // mobile and on top, but not on a stack of beetles, and near the q
						  msg += " Top="+beetleOnTopWeight;
						  val += beetleOnTopWeight;
						}

						
						double mobilemul = piece_mobility_weight[pieceordinal];
						// score bugs based on where they can get
						if(crude_queen_distance<=1)
						{
						// always score pieces adjacent to the Q as maximally mobile at a distance of 2
						// so it doesn't lose score for hitting the target
						int nq = piece_maximum_mobility[pieceordinal];
						double nv = mobilescale*nq/2;
						totalChoices += nv;
						if(print) { msg += "qmob "+nq+" "+nv+" qa "+queenAdjacentValue; }
						val += nv+queenAdjacentValue;

						}
						else if(some)
						{	// if mobile, score for approaching Q
	
						double stepval = present_queen_distance_multiplier[pieceordinal]/Math.max(1,crude_queen_distance);
						val += stepval;
						if(print) { msg += stepval; }
				
						// sibmo detects the case where a pair of pieces are both mobile, but if either
						// of them actuallt moves, the other becomes immobile. 
						int nmoves = tempDests.size();
						totalChoices += Math.min(piece_maximum_mobility[pieceordinal],nmoves);	// avoid making ants look absurdly powerful
						// pieces that have already reached the Q get fullmarks for mobility
						boolean sibmo = board.sibMobileAny(loc);
						if(sibmo)
							{mobilemul=mobilemul/2;			// factor of 3 actively penalizes pairing ants
							 if(print) { msg+="sibmobile "; }
							}

						if(print) { msg += "mob "+mobilemul; }
						val += mobilemul;
						
						if(loc.isAdjacentTo(myQueenLoc))
							{ myQueenAdjacent++; 	// count the number of our pieces adjacent to the our q which are mobile
							  pillbugAdjacent |= bugtype==PieceType.PILLBUG;
							}
						for(int dis = nmoves-1; dis>=0; dis--)
						{
							mobile_queen_distance = Math.min(mobile_queen_distance,board.hex_cell_distance(oql,tempDests.elementAt(dis)));
						}
	
						
						double mstepval = mobilescale/Math.max(1,mobile_queen_distance);
						if(print) { msg += "+"+mstepval; }
						val += mstepval;
						}
					}
					else
					{	// buried bugs
						double pen = burial_penalty[pieceordinal];
						if(print) { msg += "under=-"+ pen; }
						val -= pen;
					}
					
					
					if(bug.isPillbug())
					{	if(loc.isAdjacentToDanger(bug.color))
						{
						if(print) { msg += " fear="+pillbug_fear_weight; }
						val += pillbug_fear_weight;
						}
						if(board.isAdjacentToQueen(loc,oql,myQueenLoc))
						{
						if(print) { msg += " Qadj="+pillbug_queen_afinity_weight; }
						val += pillbug_queen_afinity_weight;
						}
						//int nadj = loc.nOccupiedAdjacent();
						//if(print) { msg += " adj="+nadj+"="+(nadj*pillbugEmptyWeight); }
						//val += nadj*pillbugEmptyWeight;							

					}
					
					if(print) { msg += "="+(val-val0)+"]"; }
				}
				}	

				}
		board.returnTempDest(tempDests);

		}
		
		if(myQueenLoc.onBoard)
		{	// evaluate queen safety, considering friendlies that can move away
			double occ = myQueenLoc.nOccupiedOrOwnedAdjacent(targetColor);
			
			if(occ>0)
			{
			if(myQueenAdjacent>0)
				{ double dif = (pillbugAdjacent ? 1.0 : friendlyMobileAtQueen);
				  occ -= dif;
				}				
			boolean isOnTop = (myQueenLoc.topChip().type==PieceType.QUEEN);
			double topval = isOnTop?0.0:-queen_under_penalty;
			int na = (int)occ;
			double frac = occ-na;
			double saf = queen_safety[na];
			if(frac>0.0)
			{
				saf += frac*(queen_safety[na+1]-saf);
			}
	
			if(print) { msg += " qsaf "+occ+"="+(saf+topval); }
			val += saf+topval;
			}
		}
		if(totalChoices==0) { if(print) { msg += "no moves -"+shutoutPenalty; } val -=shutoutPenalty; }
		else {
			double dwe = totalChoices*totalChoicesWeight;
			val += dwe;
			if(print) { msg += "choice "+dwe; }
		}
		
		if(print) { msg += " f= "+val; System.out.println(msg); }
		return(val);
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
	
	public commonMove gutAnalysis(CommonDriver search_state,double eval,commonMove m)
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