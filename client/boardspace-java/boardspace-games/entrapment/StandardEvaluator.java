/* copyright notice */package entrapment;

import lib.DStack;
import online.game.BoardProtocol;
import online.search.DefaultEvaluator;
import online.search.Evaluator;

public class StandardEvaluator extends DefaultEvaluator implements Evaluator , EntrapmentConstants
{
	double deadRoamerWeight = 100;
	double trappedRoamerWeight = 10;
	double flippedBarrierWeight = 0.5;
	double unplacedBarrierWeight = 1.0;
	double escapeWeight = 10;
	double linkWeight = 3;
	
	public StandardEvaluator()
	{	
	}
	
	public double[] getWeights() {
		DStack weight = new DStack();
		weight.push(deadRoamerWeight);
		weight.push(trappedRoamerWeight);
		weight.push(flippedBarrierWeight);
		weight.push(unplacedBarrierWeight);
		weight.push(escapeWeight);
		weight.push(linkWeight);
		return(weight.toArray());
	}

	public void setWeights(double[] v) {
		int idx = 0;
		deadRoamerWeight = v[idx++];
		trappedRoamerWeight = v[idx++];
		flippedBarrierWeight = v[idx++];
		unplacedBarrierWeight = v[idx++];
		escapeWeight = v[idx++];
		linkWeight = v[idx++];

	}

	public double evaluate(BoardProtocol b, int player, boolean print) 
	{
		EntrapmentBoard board = (EntrapmentBoard)b;
		int nextP = player^1;
    	double finalv=board.deadRoamers[nextP].height()*deadRoamerWeight;
    	finalv += board.nTrapped[nextP]*trappedRoamerWeight;											// 10 points for a trapped roamer
    	finalv += board.flippedBarriers[nextP]*flippedBarrierWeight;									// half a point for each barrier the other guy flipped
    	finalv += (EntrapmentBoard.STARTING_BARRIERS-board.unplacedBarriers[player].height())*unplacedBarrierWeight;	// one point for each barrier deployed
    	EntrapmentCell r[] = board.roamerCells[player];
    	for(int idx=0,lim=r.length;idx<lim; idx++)
    	{	EntrapmentCell c = r[idx];
    		// one point for each non-barrier empty cell
    		if(c!=null)
    			{
    			finalv += linkWeight*(c.linkCount-4);				// minus points for being on the edge
    			board.sweepCounter++;
    			finalv += board.escapeSize(player,c,4)*escapeWeight;
    			}}
    	return(finalv);
    }

}
