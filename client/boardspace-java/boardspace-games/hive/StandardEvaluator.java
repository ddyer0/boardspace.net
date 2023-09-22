/* copyright notice */package hive;

import hive.HiveConstants.HiveId;
import hive.HiveConstants.PieceType;
import lib.DStack;
import lib.G;
import online.game.BoardProtocol;
import online.search.DefaultEvaluator;
import online.search.Evaluator;

class StandardEvaluator extends DefaultEvaluator implements Evaluator
{	
	// simple evaluation based on piece mobility and importance
	double beetle_fear_weight = -2.0;
	double queen_afinity_weight = 5.0;
	double queen_pinned_weight = -10.0;
	double QueenCrowdingWeight = -2.0;
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
			0.9,		// beetle
			1.0,		// spider
			0.2,		// mosquito
			0.5,		// ladybug
			0.5,		// original pillbug
			0.5,		// pillbug
			0.2 };		// blank

	public double[] getWeights()
	{	DStack v = new DStack();
		v.push(beetle_fear_weight);
		v.push(queen_afinity_weight);
		v.push(queen_pinned_weight);
		v.push(QueenCrowdingWeight);
		for(double d : piece_mobility_weight) { v.push(d); }
		for(double d : queen_safety) { v.push(d); }
		for(double d : queen_distance_multiplier)  { v.push(d); }
		return(v.toArray());
	}
	public void setWeights(double v[])
	{	int idx = 0;
		beetle_fear_weight = v[idx++];
		queen_afinity_weight = v[idx++];
		queen_pinned_weight = v[idx++];
		QueenCrowdingWeight = v[idx++];
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

		// note, a subtle bug in this evaluator resulted because the order
		// of cells in occupiedcells is unstable.  Floating point values
		// are still slightly unstable!
		HiveId targetColor = board.playerColor(pl);
		for(int i=0,lim=board.occupiedCells.size(); i<lim; i++)
			{	
				HiveCell loc = board.occupiedCells.elementAt(i);
				HivePiece bug = loc.topChip();
				if(bug.color==targetColor)
				{
				PieceType bugtype = bug.type;
				int pieceordinal = bugtype.ordinal();
				if(board.pieceTypeIncluded.test(bugtype) && loc.onBoard)
				{	double weight = 0.6*piece_mobility_weight[pieceordinal];
					HiveCell tempDests[] = board.getTempDest();
					int ndests = board.legalDests(loc,false,bug,tempDests,null,pl,false);
					if(ndests>0 || distance)
					{
					if(print) 
						{ msg += " "+bug.exactBugName()+"[";
						  if(ndests>0) { msg+=""+ndests+"="+(ndests*weight); }
						}
					if(ndests>0)
					{
						val += ndests*weight;
						if((loc.height()==1)&&loc.isAdjacentTo(myQueenLoc))
							{ myQueenAdjacent++; 	// count the number of our pieces adjacent to the our q which are mobile
							}
					}
					if(distance && (ndests>0))
					{	// score distance from queen, but only for mobile pieces
						switch(bugtype)
						{
						case BEETLE:
						case GRASSHOPPER:
							{
							int queen_distance = (loc.overland_gradient-overland_base_gradient);
							int queen_dmul = queen_distance<0?0:(18-queen_distance);
							double mul = queen_dmul*queen_distance_multiplier[pieceordinal];
							val += mul;
							if(print) { msg += " d="+queen_distance+"="+mul; }
							}
							break;
						default:
							// don't use sweep_counter, as it may have been changed since the
							// slither gradient was calculated. [may 2017]
							if(loc.slither_gradient>=slither_base_gradient)
							{
							int queen_distance = (loc.slither_gradient-slither_base_gradient);
							int queen_dmul = queen_distance<0?0:(18-queen_distance);
							double mul = queen_dmul*queen_distance_multiplier[pieceordinal];
							val += mul;
							if(print) { msg += " d="+queen_distance+"="+mul; }
							}
						}
					}
					}
					if(bug.isPillbug())
						{	if(loc.isAdjacentToDanger(bug.color))
							{
							if(print) { msg += " Beetle fear="+beetle_fear_weight; }
							val += beetle_fear_weight;
							}
							if(board.isAdjacentToQueen(loc,oql,myQueenLoc))
							{
							if(print) { msg += " Qadj="+queen_afinity_weight; }
							val += queen_afinity_weight;
							}
						}
					if(print) { msg +="]"; }

		        board.returnTempDest(tempDests);
				}
			}}
		if(myQueenLoc.onBoard)
		{	// evaluate queen safety, number adjacent-number mobile adjacent
			int na = myQueenLoc.nOccupiedAdjacent()-myQueenAdjacent;
			if(print)
			{
				for(int dir = 0;dir<6;dir++)
				{
					HiveCell c = myQueenLoc.exitTo(dir);
					G.print(""+c+(c.slither_gradient-slither_base_gradient));
				}
			}
			if(myQueenAdjacent>1)
			{	double qw = (myQueenAdjacent-1)*QueenCrowdingWeight;
				val += qw;
				if(print) { msg += "Qcrowd="+qw;}
			}
			if(na>0)
			{
			boolean isOnTop = (myQueenLoc.topChip().type==PieceType.QUEEN);
			double topval = isOnTop?0.0:queen_pinned_weight;
			if(print) { msg += " qsaf "+na+"="+(queen_safety[na]+topval); }
			val += queen_safety[na]+topval;
			}
		}

		if(print) { System.out.println(msg); }
		return(val);
	}
}