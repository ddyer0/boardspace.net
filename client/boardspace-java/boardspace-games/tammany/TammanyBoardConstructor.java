/* copyright notice */package tammany;

import lib.G;
import lib.Random;
import online.game.BoardProtocol;
import online.game.RBoard;

// nitty gritty of constructing an ad-hoc board.  This is really part of the 
// TammanyBoard class, but we separate it here to leave the main board class
// uncluttered by ad-hoc constants and such.
abstract class TammanyBoardConstructor extends RBoard<TammanyCell> implements BoardProtocol,TammanyConstants
{	Random constructor = new Random(0x3435236f);

	// needed to disambiguate method selection
	public void sameboard(TammanyBoardConstructor f) { super.sameboard(f); }
	public void copyFrom(TammanyBoardConstructor b) { super.copyFrom(b); }
	private TammanyCell boardCell(TammanyId id,double cx,double cy)
	{	TammanyCell c = new TammanyCell(id,cx,cy);
		addCell(c);
		return(c);
	}
	TammanyCell uiCells = null;
	TammanyCell firstUiCell = null;

	// a cell that doesn't participate in digest and copy
	public TammanyCell offboardCell(TammanyId id,TammanyChip bossChip,int idx,double cx,double cy)
	{
		TammanyCell c = new TammanyCell(id,idx,cx,cy);
		c.next = uiCells;
		c.boss = bossChip;
		uiCells = c;
		if(firstUiCell==null) { firstUiCell = c; }
		return(c);
	}
	private TammanyCell boardCell(TammanyId id,int idx,Zone z,int elec,double cx,double cy)
	{	switch(id)
		{
		case WardCube:
		case WardBoss: 
			cy+= 2;			// this allows the positions of the bosses and cubes to be
			cx -=3;			// adjusted as a group in ad-hoc ways.
			break;
		default: ;
		}
		TammanyCell c = new TammanyCell(id,idx,z,cx,cy);
		addCell(c);
		if(elec>=1) { electionOrder[elec-1] = idx; }
		return(c);
	}
	private TammanyCell initCell(TammanyId id,int idx,double cx,double cy)
	{	TammanyCell c = new TammanyCell(id,idx,cx,cy);
		addCell(c);
		return(c);
	}
	private TammanyCell initCell(TammanyId id,int idx)
	{
		return(new TammanyCell(id,idx,0,0));
	}
	private void conn(int n,int k)
	{	wardCubes[n].addLink(wardCubes[n],wardCubes[k]);
	}
	TammanyCell irishLeader = boardCell(TammanyId.IrishLeader,12,44);
	TammanyCell englishLeader = boardCell(TammanyId.EnglishLeader,12,48.5);
	TammanyCell germanLeader = boardCell(TammanyId.GermanLeader,12,53);
	TammanyCell italianLeader = boardCell(TammanyId.ItalianLeader,12,57);
	TammanyCell ethnicControls[] = {
			irishLeader,englishLeader,germanLeader,italianLeader
	};
	
	TammanyCell mayorRole = boardCell(TammanyId.Mayor,79,14);
	TammanyCell deputyMayorRole = boardCell(TammanyId.DeputyMayor,79,20);
	TammanyCell councilPresidentRole = boardCell(TammanyId.CouncilPresident,79,26);
	TammanyCell policeChiefRole = boardCell(TammanyId.ChiefOfPolice,79,34);
	TammanyCell precinctChairmanRole = boardCell(TammanyId.PrecinctChairman,79,44);
	
	TammanyCell roleCells[] = 
		{ 	mayorRole,
			deputyMayorRole,
			councilPresidentRole,
			precinctChairmanRole,
			policeChiefRole 
		};
	
	/**
	 * note that these cells are not added to the board, but only to the 
	 * auxiliary chain uiCells which it prepended to allCells
	 * these cells do not participate in the clone/digest/sameboard protocol
	 * 
	 * @param first
	 * @param last
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	TammanyCell[] cellArray(TammanyId id,TammanyChip bossChip,int first,int last, double left,double top,double right,double bottom)
	{	int ncells = (last-first+1);
		TammanyCell ar[] = new TammanyCell[ncells];
		boolean hmode = (first<=1);
		double cellw = hmode ? (right-left)/ncells : right-left;
		double cellh = hmode ? (bottom-top) : (bottom-top)/ncells;
		left += cellw/2;
		top += cellh/2;
		for(int i=0;i<ncells;i++)
		{	double x = hmode ? left+i*cellw : left;
			double y = hmode ? top : top+i*cellh;
			ar[i] = offboardCell(id,bossChip,i+first,x,y);
			
		}
		return(ar);
	}
	
	TammanyCell scoreTop[] = cellArray(TammanyId.ScoringTrack,null,0,23,  6,5,94,8);		// scoring track for UI
	TammanyCell scoreSide[] = cellArray(TammanyId.ScoringTrack,null,24,40, 90,8,94,62);		// scoring track for UI
	TammanyCell yearIndicator[] = cellArray(TammanyId.YearIndicator,null,1,16, 31.8,10, 71,12);	// year track for UI
	
	TammanyCell castleGardens[] = {
			initCell(TammanyId.CastleGardens,0,	15.5,	89.5),
			initCell(TammanyId.CastleGardens,1,	18.5,	89.5),
			initCell(TammanyId.CastleGardens,2,	21.5,	89.5),
			initCell(TammanyId.CastleGardens,3,	13.0,	92.5),
			initCell(TammanyId.CastleGardens,4,	16.0,	92.5),
			initCell(TammanyId.CastleGardens,5,	19,	92.5),
			initCell(TammanyId.CastleGardens,6,	22,	92.5),
	};

	TammanyCell zone1Init[] = {
			initCell(TammanyId.Zone1Init,0,	8.5,	15.5),
			initCell(TammanyId.Zone1Init,1,	10.0,	18.0),
			initCell(TammanyId.Zone1Init,2,	11.5,	15.5),
			initCell(TammanyId.Zone1Init,3,	14.0,	18.0),
			initCell(TammanyId.Zone1Init,4,	16.0,	15.5),
			initCell(TammanyId.Zone1Init,5,	16.2,	13.5),
	};
	TammanyChip zone1InitValues[] = 
			{ TammanyChip.irish,
			  TammanyChip.english,TammanyChip.english,
			  TammanyChip.german,TammanyChip.german,
			  TammanyChip.irish}; 
	
	TammanyCell zone2Init[] = {
			initCell(TammanyId.Zone2Init,0,	8.5,	25.5),
			initCell(TammanyId.Zone2Init,1,	10.0,	23.2),
			initCell(TammanyId.Zone2Init,2,	12.5,	25.5),
			initCell(TammanyId.Zone2Init,3,	13.5,	23.2),
			initCell(TammanyId.Zone2Init,4,	16.0,	23.5),
	};
	TammanyChip zone2InitValues[] = {
			TammanyChip.irish,TammanyChip.irish,
			TammanyChip.english,TammanyChip.english,
			TammanyChip.german
	};
	TammanyCell zone3Init[] = {
			initCell(TammanyId.Zone3Init,0,	9,	31),
			initCell(TammanyId.Zone3Init,1,	10.5,	33),
			initCell(TammanyId.Zone3Init,2,	12.5,	31),
			initCell(TammanyId.Zone3Init,3,	16,	31),
	};
	TammanyChip zone3InitValues[] = {
			TammanyChip.irish,TammanyChip.irish,
			TammanyChip.english,
			TammanyChip.german
	};
	
	// to keep the wards indexed by ward number in a natural way, several nonexistent
	// wards are on board but "off the map"
	TammanyCell wardCubes[] = {
			boardCell(TammanyId.WardCube,0,null,0,0,0),	// nonexistent
			boardCell(TammanyId.WardCube,1,Zone.Zone1,1,	21,		74),
			boardCell(TammanyId.WardCube,2,Zone.Zone1,2,	30,		69.5),
			boardCell(TammanyId.WardCube,3,Zone.Zone2,11,	22,		61),
			boardCell(TammanyId.WardCube,4,Zone.Zone1,3,	38.5,	66),
			boardCell(TammanyId.WardCube,5,Zone.Zone2,10,	26,		51.5),
			boardCell(TammanyId.WardCube,6,Zone.Zone1,5,	37,		58.5),
			boardCell(TammanyId.WardCube,7,Zone.Zone1,4,	57.5,	62.5),
			boardCell(TammanyId.WardCube,8,Zone.Zone2,9,	32,		43),
			boardCell(TammanyId.WardCube,9,Zone.Zone2,7,	29,	27.5),
			boardCell(TammanyId.WardCube,10,Zone.Zone3,15,	49.5,	55),
			boardCell(TammanyId.WardCube,11,Zone.Zone3,13,	69.5,	45.5),
			boardCell(TammanyId.WardCube,12,null,0,0,0),	// off the map
			boardCell(TammanyId.WardCube,13,Zone.Zone3,14,	63,	56),
			boardCell(TammanyId.WardCube,14,Zone.Zone1,6,	42,	50),
			boardCell(TammanyId.WardCube,15,Zone.Zone2,8,	44.5,	32),
			boardCell(TammanyId.WardCube,16,null,0,0,0),		// off the map
			boardCell(TammanyId.WardCube,17,Zone.Zone3,12,	59,	37)
	};
	// to keep the wards indexed by ward number in a natural way, several nonexistent
	// wards are on board but "off the map"
	TammanyCell wardBosses[] = {
			boardCell(TammanyId.WardBoss,0,null,0,0,0),	// nonexistent
			boardCell(TammanyId.WardBoss,1,Zone.Zone1,1,	21,		71),
			boardCell(TammanyId.WardBoss,2,Zone.Zone1,2,	30,		66.5),
			boardCell(TammanyId.WardBoss,3,Zone.Zone2,11,	22,		58),
			boardCell(TammanyId.WardBoss,4,Zone.Zone1,3,	38.5,	63),
			boardCell(TammanyId.WardBoss,5,Zone.Zone2,10,	26,		48.5),
			boardCell(TammanyId.WardBoss,6,Zone.Zone1,5,	37,		55.5),
			boardCell(TammanyId.WardBoss,7,Zone.Zone1,4,	57.5,	59.5),
			boardCell(TammanyId.WardBoss,8,Zone.Zone2,9,	32,		40),
			boardCell(TammanyId.WardBoss,9,Zone.Zone2,7,	29,	24.5),
			boardCell(TammanyId.WardBoss,10,Zone.Zone3,15,	49.5,	52),
			boardCell(TammanyId.WardBoss,11,Zone.Zone3,13,	69.5,	42.5),
			boardCell(TammanyId.WardBoss,12,null,0,0,0),	// off the map
			boardCell(TammanyId.WardBoss,13,Zone.Zone3,14,	63,	53),
			boardCell(TammanyId.WardBoss,14,Zone.Zone1,6,	42,	47),
			boardCell(TammanyId.WardBoss,15,Zone.Zone2,8,	44.5,	29),
			boardCell(TammanyId.WardBoss,16,null,0,0,0),		// off the map
			boardCell(TammanyId.WardBoss,17,Zone.Zone3,12,	59,	34)
	};
	
	// a spare set of boss cells for the robot to use
	TammanyCell robotBosses[] = {
			initCell(TammanyId.WardBoss,0),				// nonexistent
			initCell(TammanyId.WardBoss,1),
			initCell(TammanyId.WardBoss,2),
			initCell(TammanyId.WardBoss,3),
			initCell(TammanyId.WardBoss,4),
			initCell(TammanyId.WardBoss,5),
			initCell(TammanyId.WardBoss,6),
			initCell(TammanyId.WardBoss,7),
			initCell(TammanyId.WardBoss,8),
			initCell(TammanyId.WardBoss,9),
			initCell(TammanyId.WardBoss,10),
			initCell(TammanyId.WardBoss,11),
			initCell(TammanyId.WardBoss,12),	// off the map
			initCell(TammanyId.WardBoss,13),
			initCell(TammanyId.WardBoss,14),
			initCell(TammanyId.WardBoss,15),
			initCell(TammanyId.WardBoss,16),		// off the map
			initCell(TammanyId.WardBoss,17)
	};
	
	static int electionOrder[] = new int[15];
	
	TammanyCell tammanyHallCubes = wardCubes[14];	// tammanyHall is special

	TammanyCell wasteBasket = boardCell(TammanyId.Trash,38,80.5);	// will be last in the drawing order

	TammanyCell bossPlacement[] = {
			initCell(TammanyId.BossPlacement,0,	44.5,	77),
			initCell(TammanyId.BossPlacement,1,	47.5,	77),
			// we need 4 cells because it's used in role assignment.  Mayor is already assigned.
			initCell(TammanyId.BossPlacement,2,50.5,	77),	
			initCell(TammanyId.BossPlacement,3,53.5,	77)
	};
	TammanyCell slanderPlacement = boardCell(TammanyId.SlanderPlacement,44.5,85);

	TammanyCell lockPlacement[] = {
			initCell(TammanyId.LockPlacement,0,	50.5,	80),
			initCell(TammanyId.LockPlacement,0,	53.5,	80),
			
	};
	
	TammanyCell placementRoleCard = boardCell(TammanyId.RoleCard,65,80);
	
	// influence disc display during normal play
	TammanyCell influence[] = {
			initCell(TammanyId.InfluencePlacement,0,50.5,87),
			initCell(TammanyId.InfluencePlacement,1,54.5,87),
			initCell(TammanyId.InfluencePlacement,2,58.5,87),
			initCell(TammanyId.InfluencePlacement,3,62.5,87),
	};
	
	TammanyCell bag = boardCell(TammanyId.Bag,36,90);
	
	TammanyCell[][] electionBox(double l,double t,double r,double b)
	{	TammanyCell boxes[][] = new TammanyCell[5][6];
		double xstep = (r-l)/5.0;
		double ystep = (b-t)/6.0;
		double y = t+ystep/2;
		for(int row=0;row<6;row++)
		{	
			double x = l+xstep/2;
			for(int col=0;col<5;col++)
			{	TammanyCell newCell = offboardCell( (row==0)||(row==5)
											?TammanyId.ElectionBoss
											:TammanyId.ElectionDisc,
											TammanyChip.getBoss(col),
											col+1,x,y);
				boxes[col][row] = newCell; 
				newCell.row = col+1;
				newCell.col = (char)('A'+row);
				x += xstep;
			}
			y += ystep;
		}
		
		return(boxes);
	}
	TammanyCell electionBox[][] = null;
	
	public void buildNetwork()
	{	conn(1,2);
		conn(1,3);
		conn(2,3);
		conn(2,6);
		conn(2,4);
		conn(3,5);
		conn(3,6);
		conn(4,6);
		conn(4,7);
		conn(5,6);
		conn(5,8);
		conn(6,10);
		conn(6,14);
		conn(7,6);
		conn(7,10);
		conn(7,13);
		conn(8,9);
		conn(8,15);
		conn(8,14);
		conn(9,15);
		conn(10,13);
		conn(10,14);
		conn(10,17);
		conn(11,13);
		conn(11,17);
		conn(14,15);
		conn(15,17);
		firstUiCell.next = allCells;

	}

	public int positionToX(double pos)
	{	return((int)(G.Left(boardRect)+((G.Width(boardRect)*pos))));
	}
	@Override
	public int cellToX(TammanyCell c) 
	{
		if((c!=null)&&(boardRect!=null))
		{
			return(positionToX(c.center_x/100.0));
		}
		return(0);
	}
	public int positionToY(double pos)
	{	return((int)(G.Top(boardRect)+((G.Height(boardRect)*pos))));
	}
	@Override
	public int cellToY(TammanyCell c) 
	{
		if((c!=null)&&(boardRect!=null))
		{
		return(positionToY(c.center_y/100.0));
		}
		return 0;
	}

}
