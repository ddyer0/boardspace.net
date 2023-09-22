/* copyright notice */package hive;

import hive.HiveConstants.HiveId;
import lib.DStack;
import online.game.BoardProtocol;
import online.search.DefaultEvaluator;
import online.search.Evaluator;

class MonteEvaluator extends DefaultEvaluator implements Evaluator
{	
	// simple evaluation based on piece mobility and importance
	double queen_pinned_weight = -0.25;
	double pillbug_pinned_weight = -0.1;
	double QueenCrowdingWeight = -0.1;
	double QueenDistanceWeight = -0.001;
	public double[] getWeights()
	{	DStack v = new DStack();
		v.push(queen_pinned_weight);
		v.push(QueenCrowdingWeight);
		v.push(pillbug_pinned_weight);
		v.push(QueenDistanceWeight);
		return(v.toArray());
	}
	public void setWeights(double v[])
	{	int idx = 0;

		queen_pinned_weight = v[idx++];
		QueenCrowdingWeight = v[idx++];
		pillbug_pinned_weight = v[idx++];
		QueenDistanceWeight = v[idx++];
	}
	
	public double evaluate(BoardProtocol boardp,int pl,boolean print)
	{ 	HiveGameBoard board = (HiveGameBoard)boardp;
		double val = 0.0;
		HiveCell oql = board.pieceLocation.get(board.playerQueen(pl^1));
		HiveId targetColor = board.playerColor(pl);
		double dis = 0;
		for(int i=0,lim=board.occupiedCells.size(); i<lim; i++)
			{	
			HiveCell loc = board.occupiedCells.elementAt(i);
			HivePiece bug = loc.topChip();
			int height = loc.height();
			HivePiece bottom = height>1 ? loc.chipAtIndex(0) : bug;
			if(oql!=null && oql.onBoard)
			{
				dis += Math.abs(oql.col-loc.col)+Math.abs(oql.row-loc.row)/2;
			}
			if(bottom.color==targetColor)
			{
				
			switch(bottom.type)
				{
				case QUEEN:
					val += loc.nOccupiedAdjacent()*QueenCrowdingWeight;
					if(bug!=bottom) { val+=queen_pinned_weight; }
					break;
				case PILLBUG:
				case ORIGINAL_PILLBUG:
					if(bug!=bottom) { val+=pillbug_pinned_weight; }
					break;
				default: break;
				}
			}}
		return(val+dis*QueenDistanceWeight);
	}
}