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
import hive.HiveConstants.PieceType;
import lib.DStack;
import online.game.BoardProtocol;
import online.search.DefaultEvaluator;
import online.search.Evaluator;

class ThirdStandardEvaluator extends DefaultEvaluator implements Evaluator
{	
	
	public int canRandomize(BoardProtocol b, int who) 
	{	int move = b.moveNumber();
		return (move <=8) 
						? (30 - move) 
						: 0; 
	}

	// simple evaluation based on piece mobility and importance
	double pillbug_fear_weight = -2.0;	// fear of beetles by pillbugs
	
	double queen_afinity_weight = 5.0;
	double queen_pinned_weight = -11.0;
	double pillbug_pinned_weight = -5.0;
	double QueenCrowdingWeight = -2.0;
	double pillbugEmptyWeight = -0.5;	// penalty for pillbug adjacent filled
	double beetleOnTopWeight = 0.5;		// bonus for beetle on top

	double piece_mobility_weight[] = 
		{ 5.0,			// queen
		0.19,			// ant	
		0.15,			// grasshopper
		0.3,			// beetle
		0.1,			// spider
		0.18,			// mosquito
		0.3,			// ladybug
		0.1,			// original pillbug
		0.1,			// pillbug
		0.2};			// blank
	double queen_safety[] = { 0.0, 0.0, -1.0, -15.0, -40.0, -65.0, -120,-120,-120,-120};
	double queen_distance_multiplier[] =
			{ 0.1,		// queen
			0.03,		// ant
			0.4,		// grasshopper
			1.0,		// beetle
			1.0,		// spider
			0.2,		// mosquito
			0.5,		// ladybug
			0.5,		// original pillbug
			0.5,		// pillbug
			0.2 };		// blank

	public double[] getWeights()
	{	DStack v = new DStack();
		v.push(pillbug_fear_weight);
		v.push(queen_afinity_weight);
		v.push(queen_pinned_weight);
		v.push(QueenCrowdingWeight);
		v.push(pillbugEmptyWeight);
		for(double d : piece_mobility_weight) { v.push(d); }
		for(double d : queen_safety) { v.push(d); }
		for(double d : queen_distance_multiplier)  { v.push(d); }
		return(v.toArray());
	}
	public void setWeights(double v[])
	{	int idx = 0;
		pillbug_fear_weight = v[idx++];
		queen_afinity_weight = v[idx++];
		queen_pinned_weight = v[idx++];
		QueenCrowdingWeight = v[idx++];
		pillbugEmptyWeight = v[idx++];
		for(int i=0;i<piece_mobility_weight.length;i++) {  piece_mobility_weight[i] = v[idx++]; }
		for(int i=0;i<queen_safety.length;i++) { queen_safety[i] = v[idx++]; }
		for(int i=0;i<queen_distance_multiplier.length;i++)  { queen_distance_multiplier[i]=v[idx++]; }
		// check
		//double rv[] = getWeights();
		//G.Assert(G.sameArrayContents(v, rv),"check setWeights failed");
	}
	
	public double evaluate(BoardProtocol boardp,int pl,boolean print)
	{ 	HiveGameBoard board = (HiveGameBoard)boardp;
		HiveCell oql = board.pieceLocation.get(board.playerQueen(pl^1));
		HiveCell myQueenLoc = board.pieceLocation.get(board.playerQueen(pl));
		double val = 0.0;
		String msg = "";
		boolean distance = false;
		int overland_base_gradient = 0;
		int slither_base_gradient =0;
		int myQueenAdjacent = 0;
		// calculate queen distance overland_gradient
		if((oql!=null) && oql.onBoard)
		{ 	distance=true;
			overland_base_gradient = ++board.sweep_counter;
			board.sweepAndCountBoard(oql,0);
		  	slither_base_gradient = ++board.sweep_counter;
		  	board.slitherAndCountBoard(oql,oql,null,0,0);
		}
		HiveId targetColor = board.playerColor(pl);
		// note, a subtle bug in this evaluator resulted because the order
		// of cells in occupiedcells is unstable.  Floating point values
		// are still slightly unstable!
		for(int i=0,lim=board.occupiedCells.size(); i<lim; i++)
			{	
				HiveCell loc = board.occupiedCells.elementAt(i);
				HivePiece bug = loc.topChip();
				if(loc.height()>1)
				{
					HivePiece bottomChip = loc.chipAtIndex(0);
					if(bottomChip.color == targetColor)
					{
						switch(bottomChip.type)
						{
						default: break;
						case QUEEN:
							val += queen_pinned_weight;
							break;
						case ORIGINAL_PILLBUG:
						case PILLBUG:
							val += pillbug_pinned_weight;
							break;
							
						}
					}
				}
				if(bug.color==targetColor)
				{
				PieceType bugtype = bug.type;
				int pieceordinal = bugtype.ordinal();
				if(board.pieceTypeIncluded.test(bugtype) && loc.onBoard)
				{	double weight = 0.6*piece_mobility_weight[pieceordinal];
					double distancemul = queen_distance_multiplier[pieceordinal];
					CellStack tempDests = board.getTempDest();
					boolean some = board.legalDests(loc,false,bug,tempDests,null,pl,false);
					if(some || distance)
					{
						// 
						// look for sibling mobility enable, where a pair of pieces are both
						// mobile, but only one can actually move
						//
						if(print) 
						{ msg += " "+bug.exactBugName()+"[";
						}
					if(some)
					{	int ndests = tempDests.size();
						boolean sibmo = board.sibMobile(loc);
						if(sibmo)
							{weight=weight/3;			// factor of 3 actively penalizes pairing ants
							distancemul = distancemul/3;
							if(print) { msg+="sibmobile "; }
							}
						val += ndests*weight;
						if(!sibmo
								&& (loc.height()==1)
								&&loc.isAdjacentTo(myQueenLoc))
							{ myQueenAdjacent++; 	// count the number of our pieces adjacent to the our q which are mobile
							}
					}
					if(distance && some)
					{	// score distance from queen, but only for mobile pieces
						int slither_distance = loc.slither_gradient-slither_base_gradient;
						int queen_distance = (loc.overland_gradient-overland_base_gradient);
						PieceType ebugType = bugtype;
						if((bugtype==PieceType.MOSQUITO) 
								&& loc.isAdjacentToAnt()
								&& slither_distance>=0)
							{
							ebugType = PieceType.ANT;
							}

						switch(ebugType)
						{
						case GRASSHOPPER:	// grasshopper, the number of places isn't very relevant
							slither_distance = -1;
							queen_distance = 1;
							//$FALL-THROUGH$
						case ANT:
							slither_distance = Math.min(1, slither_distance);	// ants are always adjacent
							//$FALL-THROUGH$
						case BEETLE:
							{
							int queen_dmul = queen_distance<0?0:(18-queen_distance);
							double mul = queen_dmul*distancemul;
							val += mul;
							if(print) { msg += " d="+queen_distance+"="+mul; }
							}
							break;
						default:
							// don't use sweep_counter, as it may have been changed since the
							// slither gradient was calculated. [may 2017]
							if(loc.slither_gradient>=slither_base_gradient)
							{
							int queen_dmul = queen_distance<0?0:(18-queen_distance);
							double mul = queen_dmul*distancemul;
							val += mul;
							if(print) { msg += " d="+queen_distance+"="+mul; }
							}
						}
					}
					if(loc.height()>1) 
					{ // mobile and on top 
					  msg += " Top="+beetleOnTopWeight;
					  val += beetleOnTopWeight;
					}
					}
					if(bug.isPillbug())
						{	if(loc.isAdjacentToDanger(bug.color))
							{
							if(print) { msg += " fear="+pillbug_fear_weight; }
							val += pillbug_fear_weight;
							}
							if(board.isAdjacentToQueen(loc,oql,myQueenLoc))
							{
							if(print) { msg += " Qadj="+queen_afinity_weight; }
							val += queen_afinity_weight;
							}
							int nadj = loc.nOccupiedAdjacent();
							if(print) { msg += " adj="+nadj+"="+(nadj*pillbugEmptyWeight); }
							val += nadj*pillbugEmptyWeight;							

						}
					if(print) { msg +="]"; }

		        board.returnTempDest(tempDests);
				}
			}}
		if(myQueenLoc.onBoard)
		{	// evaluate queen safety, number adjacent-number mobile adjacent
			int na = myQueenLoc.nOccupiedAdjacent()-myQueenAdjacent;
			if(myQueenAdjacent>1)
			{	double qw = (myQueenAdjacent-1)*QueenCrowdingWeight;
				val += qw;
				if(print) { msg += "Qcrowd="+qw;}
			}
			if(na>0)
			{
				val += queen_safety[na];
				if(print) { msg += " qsaf "+na+"="+(queen_safety[na]); }
			}
		}

		if(print) { System.out.println(msg); }
		return(val);
	}
}