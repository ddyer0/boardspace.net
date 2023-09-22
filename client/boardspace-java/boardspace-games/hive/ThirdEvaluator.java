/* copyright notice */package hive;

import hive.HiveConstants.HiveId;
import hive.HiveConstants.PieceType;
import hive.HiveConstants.variation;
import lib.DStack;
import online.game.BoardProtocol;
import online.search.DefaultEvaluator;
import online.search.Evaluator;

class ThirdEvaluator extends DefaultEvaluator implements Evaluator
{	
	// constructor
	static final String Weights_Base =
"-0.018584958894430178 -5.9607722893773 -0.2506482226885917 0.322338127929888 -0.04094558464007641 5.185950995467738 2.333897838280516 -0.0014423365882161511 2.5646978216471292 5.375333032031982 -0.06854814055684029 0.010856043600981061 0.015974031630836813 1.1136272225315211 0.048346930153368536 9.940744397750683 -0.25293361972763884 -0.6435088779522982 0.07463528011830942 -39.153749047643615 0.5956730699784664 -0.20045405778756936 -0.15271948207321864 1.9722672929904126 -2.123574712341893 -1.1734520073701027 -0.09280593971005305 -0.10566128167985545 -0.23598951833615445 0.384072269910032 0.7287486789833081 0.20547154069477322 0.08504467890607145 3.017705807511966 0.4418913786387829 -0.45575530604530423 -0.014764680042827352 -0.7048416663590092 0.7614986193709418 -4.247794906643744 0.41268792115415065 0.11606221375605938 -0.06862544748292548 0.1193442607927773 0.48496149964005947 -0.23245000223075016 -1.3236555321406864 -0.5892910703560355 -0.12356298553141858 -0.2871372274803395 0.0740011283838306 0.04192305816200461 4.410412355901778 -0.7636457214392119 0.48757720217126327 0.8221936678493638 1.1300373493747142 0.46723357114208186 -0.5873436942462048";
	static final String Weights_PLM =
// after play against dumbot
"-0.018584958894430178 -0.5748030282855511 -0.2506482226885917 0.322338127929888 -0.04094558464007641 2.0569016721205644 0.2197711849400764 -0.008366265672940706 0.3114095915495381 1.7570341346438416 0.01144641947770602 0.010856043600981061 0.015974031630836813 0.5268886085173732 0.008952046707466802 9.940744397750683 -0.25293361972763884 -0.03747519264128676 0.008999759657762512 -7.760199074257204 0.21787601720525882 -0.20045405778756936 -0.15271948207321864 0.6848687900559763 -0.8246942090759956 -1.1734520073701027 -0.09016964369114074 -0.0032924556980133 -0.15487626313113764 0.20565526029500958 0.21493712686717187 0.0336298338772119 0.044205856068081154 0.19082301961985249 0.4418913786387829 -0.09827971462307483 -0.014764680042827352 -0.7048416663590092 0.4649280534404685 -1.7227166926113493 0.11396005495748351 0.08543285575447969 -0.049943957257051204 0.06811966704568598 0.14995567146949385 -0.19480258869489753 -0.892392720970456 -0.5892910703560355 -0.08759478790573728 -0.07847606104015555 -0.0021906658965499355 0.0304416971703978 0.31577105528357496 -0.16567536003738684 0.48757720217126327 0.8221936678493638 1.1300373493747142 0.14950445117360503 -0.5748030282855511";
// after selfplay
	//"-1335.992884943874 -349.370074628522 -16.071049266960276 267.2623401554154 199.8704152216773 1120.9378502813424 454.50179427743933 -47.82559125472814 114.41966569812793 910.6571299162953 5.495270426007971 -22.897323266939207 233.83585189018083 1245.039194527057 -17.334800767035244 42385.055357131976 -65.21336258663966 -88.68823788775906 -147.89528766149505 -77882.68309113357 101.29221258400501 -10101.515354736484 -202849.8417745857 139.0613246675812 -2191.2441650501823 -43.45341483389004 -59.20490791212647 -80.4493397991524 -201.95103126318594 129.50382145098797 4.312859819493901 148.32236024257602 7.940453914559048 5.979766656473108 1827.978201314127 -236.7805953259633 -29255.31805063422 -485.5565531044866 14823.230256420926 -318.8413245424129 1.6492361472850374 798.4431213727323 -33.02168294147962 80.34530934997305 132.75796497723158 -3602.801903094264 -1537.9508997834696 -57.747130275696136 -3.1441922834790033 -21.812601417693717 17.490829172183055 155.59520810105923 36.82318104190145 -41.59224996698877 177.935397311542 1.7249109950257349 2.4619682761911297 3.3426164219814596";
	public ThirdEvaluator() 
	{
		setWeights(Weights_PLM);
	};
	public ThirdEvaluator(variation var)
	{
		String weights = Weights_PLM; 
			switch(var)
		{
		case hive_plm:
			weights = Weights_PLM;
			break;
		case hive:
			weights = Weights_Base;
			break;
		default:
			break;
		
		}
	setWeights(weights);
	}
	// simple evaluation based on piece mobility and importance
	double beetle_fear_weight = -0.20;		// hand set
	double queen_afinity_weight = -0.13;	// penalty for distance from q
	double queen_pinned_weight = -0.32;		// penalty for queen with beetle on top
	double QueenCrowdingWeight = 0.30;		// bonus for mobile, friendly adj to q
	double beetleOnTopWeight = 0.018;	// bonus for beetle on top
	double pillbugEmptyWeight =  -0.23310259958359636;	// crowding the pillbug
	double siblingWeight = 0.333;		// scale factor for mobile pairs where only one can move
	double siblingDistanceWeight = 3.0;	// scale distance factor when only one can move
	double noSlitherWeight = 0.0;		// penalty for other guy having no way t slither to the Q
	double protectedWeight = 0;			// penalty for the other guy having protected spaces next to his Q
	 


	double piece_mobility_weight[] = // bonus based on number of dests a piece can reach
		{
		0.1954878410521335,			// queen
		0.14401175010970335,		// ant (maxed at 5 dests)
		0.1822731141156575,			// grasshopper
		0.2101057366540612,			// beetle
		-0.04247326391985723,		// spider
		0.148733511022795,			// mosquito
		0.06445810103379561,		// ladybug
		0.0,						// original pillbug
		0.011184947154578255,		// pillbug
		0.0};						// blank
	 
	 
	double piece_immobility_weight[] = 	// penalty when piece is immobile
		{ 
			-0.24416538988838943,			// queen
			-0.051212597856382616,			// ant	
			0.042116388811914665,			// grasshopper
			-0.17441644008535184,			// beetle
			-0.3314519763919989,			// spider
			9.039590091999943E-4,			// mosquito
			-0.15102742957091922,			// ladybug
			0.0,							// original pillbug
			-0.016324953763798646,			// pillbug
			0.0};							// blank
 
	double queen_safety[] = 	// index by number of pieces adjacent to queen
		{
		0.3312311766081359,		//
		0.6968242263743153,		// 1
		0.3889573809198819,		// 2
		-0.015551427470404448,	// 3
		-0.3619181413055389,		// 4
		-0.9773396660947903,		// 5
		-10.0 ,					// 6
		-10.0					// 7
		-10.0,					// 8
		-10.0};					// 9

	double queen_distance_multiplier[] =
			{ 
			0.0028344360651988074,		// queen
			-0.08375581425872755,		// ant
			-0.07272794223613122,		// grasshopper
			-0.27700381709426597,		// beetle
			-0.06729291363335439,		// spider
			0.001163567023074361,		// mosquito
			-0.04632807178571668,		// ladybug
			-0.0,						// original pillbug
			-0.09893828632751857,		// pillbug
			-0.0 };						// blank

	double reserve_value[] = {
			-0.12249580505971708,		// queen
			-0.43750473100167814,		// ant
			-0.3087575906340444,		// grasshopper
			0.09572218224162143,		// beetle
			-0.04152310077623027,		// spider
			-0.3642632349479147,		// mosquito
			-0.19858628254368976,		// ladybug
			0.0,						// original pillbug
			0.06887526356957187,		// pillbug
			0.0							// blank
	};

	public double[] getWeights_beetle()
	{	DStack v = new DStack();
		int ind = PieceType.BEETLE.ordinal();
		v.push(beetle_fear_weight);
		v.push(beetleOnTopWeight);
		v.push(reserve_value[ind]);
		v.push(piece_mobility_weight[ind]);
		v.push(piece_immobility_weight[ind]);
		v.push(queen_distance_multiplier[ind]);
		return(v.toArray());
	}
	public void setWeights_beetle(double v[])
	{	int i = 0;
		int ind = PieceType.BEETLE.ordinal();
		beetle_fear_weight = v[i++];
		beetleOnTopWeight = v[i++];
		reserve_value[ind] = v[i++];
		piece_mobility_weight[ind] = v[i++];
		piece_immobility_weight[ind] = v[i++];
		queen_distance_multiplier[ind] = v[i++];
		// check
		//double rv[] = getWeights();
		//G.Assert(G.sameArrayContents(v, rv),"check setWeights failed");
	}
	
	public double[] getWeights_all()
	{	DStack v = new DStack();
	v.push(beetle_fear_weight);
	v.push(queen_afinity_weight);
	v.push(queen_pinned_weight);
	v.push(QueenCrowdingWeight);
	for(double d : piece_mobility_weight) { v.push(d); }
	for(double d : queen_safety) { v.push(d); }
	for(double d : queen_distance_multiplier)  { v.push(d); }
	v.push(beetleOnTopWeight);
	v.push(pillbugEmptyWeight);
	for(double d : reserve_value)  { v.push(d); }
	for(double d : piece_immobility_weight)  { v.push(d); }
	v.push(siblingWeight);
	v.push(siblingDistanceWeight);
	v.push(noSlitherWeight);
	v.push(protectedWeight);
	return(v.toArray());
}
	public double[] getWeights()
	{	return getWeights_all();
	}	

	public void setWeights_all(double v[])
	{	int idx = 0;
		beetle_fear_weight = v[idx++];
		queen_afinity_weight = v[idx++];
		queen_pinned_weight = v[idx++];
		QueenCrowdingWeight = v[idx++];
		for(int i=0;i<piece_mobility_weight.length;i++) {  piece_mobility_weight[i] = v[idx++]; }
		for(int i=0;i<queen_safety.length;i++) { queen_safety[i] = v[idx++]; }
		for(int i=0;i<queen_distance_multiplier.length;i++)  { queen_distance_multiplier[i]=v[idx++]; }
		beetleOnTopWeight = v[idx++];
		pillbugEmptyWeight =  v[idx++]; 
		for(int i=0;i<reserve_value.length;i++) 
			{ if(idx<v.length) { reserve_value[i]=v[idx++]; }}
		for(int i=0;i<piece_immobility_weight.length;i++) 
		{ if(idx<v.length) { piece_immobility_weight[i]=v[idx++]; }}
		
		if(idx<v.length) { siblingWeight = v[idx++]; }
		if(idx<v.length) { siblingDistanceWeight = v[idx++]; }
		if(idx<v.length) { noSlitherWeight = v[idx++]; }
		if(idx<v.length)  { protectedWeight = v[idx++]; }
		// check
		//double rv[] = getWeights();
		//G.Assert(G.sameArrayContents(v, rv),"check setWeights failed");
	}

	public void setWeights(double v[])
	{	setWeights_all(v);
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
		boolean hasSlither = false;
		int myQueenAdjacent = 0;
		HiveCell rack[] = board.rackForPlayer(pl);
		for(int lim=rack.length-1; lim>=0; lim--)
			{ HiveCell rackCell = rack[lim];
			  int h = rackCell.height();
			  if(h>0)
				  { double lval = h*reserve_value[lim];
				    val += lval;
				  }
			}
		if(print && val>0) { msg+= "Reserve "+val; }
		// calculate queen distance overland_gradient
		if((oql!=null) && oql.onBoard)
		{ 	distance=true;
			overland_base_gradient = ++board.sweep_counter;
			board.sweepAndCountBoard(oql,0);
		  	slither_base_gradient = ++board.sweep_counter;
		  	int nProtected = board.slitherAndCountBoard(oql,oql,null,0,0);
		  	if(print && nProtected>0) { msg += "prot "+nProtected+"="+(nProtected*protectedWeight); }
		  	val += nProtected*protectedWeight;
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
				{	double weight = 0.6*piece_mobility_weight[pieceordinal];
					double distanceweight = queen_distance_multiplier[pieceordinal];
					HiveCell tempDests[] = board.getTempDest();
					int ndests = board.legalDests(loc,false,bug,tempDests,null,pl,false);
					if(ndests>0 || distance)
					{
					double nd = Math.min(ndests, 5);
					// 
					// look for sibling mobility enable, where a pair of pieces are both
					// mobile, but only one can actually move
					//
					if(print) 
					{ msg += " "+bug.exactBugName()+"[";
					}
					if(nd>0)
					{	boolean sibmo = board.sibMobile(loc);
						if(sibmo)
							{weight=weight*siblingWeight;			// factor of 3 actively penalizes pairing ants
							 distanceweight = distanceweight*siblingDistanceWeight;
							if(print) { msg+="sibmobile "; }
							}
						val += nd*weight;
						if(!sibmo
								&&(loc.height()==1)
								&&loc.isAdjacentTo(myQueenLoc))
							{ myQueenAdjacent++; 	// count the number of our pieces adjacent to the our q which are mobile
							}
						 msg+=""+ndests+"="+(nd*weight); 
					}
					else {
						double w = piece_immobility_weight[pieceordinal];
						if(print) { msg+= " imm "+w; }
						val += w;
					}
					if(distance && nd>0)
					{	// score distance from queen, but only for mobile pieces
						PieceType ebugType = bugtype;
						int slither_distance = loc.slither_gradient-slither_base_gradient;
						int queen_distance = (loc.overland_gradient-overland_base_gradient);
						if(slither_distance>0) { hasSlither = true; }
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
						default:
							if(slither_distance>=0)
							{	// penalty for distance from opponent queen
								double mul = slither_distance*distanceweight;
								val += mul;
								if(print) { msg += " sd="+slither_distance+"="+mul; }
								break;
							}
							//$FALL-THROUGH$
						case BEETLE:
						case LADYBUG:
						case PILLBUG:
							{
							
							double mul = queen_distance*distanceweight;
							val += mul;
							if(print) { msg += " od="+queen_distance+"="+mul; }
							}
							break;
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
							if(print) { msg += " Beetle fear="+beetle_fear_weight; }
							val += beetle_fear_weight;
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
		if(!hasSlither) 
		{
			msg += "noslither "+noSlitherWeight;
			val += noSlitherWeight;
		}
		if(myQueenLoc.onBoard)
		{	// evaluate queen safety, number adjacent-number mobile adjacent
			int na = myQueenLoc.nOccupiedAdjacent()-myQueenAdjacent/2;
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