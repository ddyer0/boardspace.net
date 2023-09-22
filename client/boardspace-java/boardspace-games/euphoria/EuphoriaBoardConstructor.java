/* copyright notice */package euphoria;

import java.util.Hashtable;
import lib.G;
import online.game.BoardProtocol;
import online.game.RBoard;

/**
 * this class is really part of EuphoriaBoard, but contains all the grubby
 * initialization that we don't want to see or mess with. This class should not
 * contain any game logic.
 * 
 * @author ddyer
 * 
 */
abstract class EuphoriaBoardConstructor extends RBoard<EuphoriaCell> implements
		BoardProtocol, EuphoriaConstants {
	public void copyFrom(EuphoriaBoardConstructor b) { super.copyFrom(b); }
	private static int DieTrackLength = 6;
	//
	// note that we make a distinction between cells on the board that are for
	// display only, and cells that
	// are part of the game. Display only cells are maintained only when the
	// viewer is active, and are
	// not part of the sameboard/Digest/clone logic.
	//
	// cells are put on the display-only list if their placement cost is
	// DisplayOnly
	// all cells onboard are identified by the cell.onboard and cell.displayOnly
	// booleans
	//
	public EuphoriaCell displayCells = null; // the display cells, followed by
												// all the regular cells
	EuphoriaCell lastDisplayCell = null; // the last display cell in the list,
											// which is linked to the regular
											// cells

	// this locates a board cell by it's EuphoriaId (ie; racklocation()) and
	// array index
	Hashtable<EuphoriaId, EuphoriaCell> allCellsById = new Hashtable<EuphoriaId, EuphoriaCell>();

	private void register(EuphoriaCell c) {
		EuphoriaId rack = c.rackLocation();
		EuphoriaCell p = allCellsById.get(rack);
		if (rack.isArray) {
			c.nextInGroup = p;
		} else {
			G.Assert(p == null, "Unique rack identifier used twice, %s and %s",
					p, c);
		}
		allCellsById.put(c.rackLocation(), c);
	}
	// needed to disambiguate the method selection
	public void sameboard(EuphoriaBoardConstructor f) { super.sameboard(f); }
	/**
	 * Initializations for all the cells on the board. locations of objects on
	 * the board, in percent of full size board.
	 */
	// factory method to generate a board cell which is one of an array of cells
	private EuphoriaCell newCell(Allegiance af, EuphoriaChip cont, EuphoriaId rack, int seq,
			double x, double y, Cost cost, Benefit bene) {
		EuphoriaCell c = new EuphoriaCell(cont,rack, seq, x, y);
		c.allegiance = af;
		if (cost == Cost.DisplayOnly) {
			c.next = displayCells;
			c.displayOnly = true;
			displayCells = c;
			if (lastDisplayCell == null) {
				lastDisplayCell = c;
			}
		} else {
			c.next = allCells;
			allCells = c;
		}
		c.onBoard = true;

		if ( rack.isWorkerCell) { // worker cells must have a cost for placement
									// and a benefit
			G.Assert(cost != null,
					"Default cost for placement must be specified for %s", c);
			c.initialPlacementCost = c.placementCost = cost;
			G.Assert(bene != null,
					"Default benefit for placement must be specified for %s", c);
			c.initialPlacementBenefit = c.placementBenefit = bene;
		}
		register(c);
		return (c);
	}

	// factory method to generate a board cell which is a singleton
	private EuphoriaCell newCell(Allegiance al, EuphoriaChip cont,EuphoriaId rack, double x,
			double y, Cost p) {
		return (newCell(al, cont, rack, -1, x, y, p, null));
	}

	// factory method to generate a board cell which is a worker placement
	// singleton
	private EuphoriaCell newCell(Allegiance al, EuphoriaId rack, double x,
			double y, Cost p, Benefit b) {
		return (newCell(al, WorkerChip.Subtype(), rack, -1, x, y, p, b));
	}

	// factory method to generate a board cell 
	private EuphoriaCell newCell(Allegiance al, EuphoriaId rack,int index,
			double bounds[]) {
		// null for the subtype so stars can be placed manually in puzzle mode
		return (newCell(al, MarketChip.Subtype(),rack, index, (bounds[0] + bounds[2]) / 2,
				(bounds[1] + bounds[3]) / 2, null, null));
	}

	// create a 1d horizontal array of cells
	private EuphoriaCell[] arrayOfCells(Allegiance al, EuphoriaChip sub,int n, double[] bounds,
			EuphoriaId rack,Cost cost) {
		EuphoriaCell v[] = new EuphoriaCell[n];
		double step = (bounds[2] - bounds[0]) / n;
		double cy = (bounds[1] + bounds[3]) / 2;
		double cx = bounds[0] + step / 2;
		G.Assert(rack.isArray, "%s should be an array rack ", rack);
		for (int i = 0; i < n; i++) {
			v[i] = newCell(al, sub,rack, i, cx + i * step, cy,	cost, null);
		}
		return (v);
	}

	// create a visually 2d array of cells, but a 1d array in reality
	private EuphoriaCell[] arrayOfCells(Allegiance al, EuphoriaChip sub,int co, int ro,
			double[] bounds, EuphoriaId rack, Cost p,
			Benefit b) {
		int n = ro * co;
		EuphoriaCell v[] = new EuphoriaCell[n];
		double ystep = (bounds[3] - bounds[1]) / ro;
		double xstep = (bounds[2] - bounds[0]) / co;
		G.Assert(rack.isArray, "%s should be an array rack ", rack);
		for (int y = 0; y < ro; y++) {
			for (int x = 0; x < co; x++) {
				int idx = x + y * co;
				v[idx] = newCell(al, sub,rack, idx, bounds[0] + xstep / 2 + xstep
						* x, bounds[1] + ystep / 2 + ystep * y, p, b);
			}
		}
		return (v);
	}

	static double euphoriaTrackBounds[] = { 3.8, 9.5, 19.0, 16 }; // morale track 1-6 for each player
	static double knowlegeTrackBounds[] = { 3.8, 20, 19, 26 }; // Knowledge track, 1-6/ for each player
	static double allegianceTrackBounds[] = { 69.5, 82, 97, 98 }; // 2d track of allegiance for each region

	EuphoriaCell moraleTrack[] = arrayOfCells(null, EuphoriaChip.MoraleMarkers[0].subtype(),DieTrackLength,	euphoriaTrackBounds, EuphoriaId.MoraleTrack,Cost.DisplayOnly);

	EuphoriaCell knowlegeTrack[] = arrayOfCells(null, EuphoriaChip.KnowledgeMarkers[0].subtype(),DieTrackLength,	knowlegeTrackBounds, EuphoriaId.KnowledgeTrack,Cost.DisplayOnly);

	EuphoriaCell allegianceTrack[] = arrayOfCells(null,EuphoriaChip.AllegianceMarker.subtype(), AllegianceSteps,	allegianceTrackBounds.length, allegianceTrackBounds,
			EuphoriaId.AllegianceTrack, Cost.DisplayOnly, null);

	EuphoriaCell workerActivationA = newCell(null,EuphoriaId.WorkerActivationA, 5.75, 90.5, 
			Cost.Energyx3,
			Benefit.NewWorkerAndKnowledge);
	EuphoriaCell workerActivationB = newCell(null,	EuphoriaId.WorkerActivationB, 13, 90.5, 
			Cost.Waterx3,
			Benefit.NewWorkerAndMorale);
	EuphoriaCell euphorianUseMarket = newCell(Allegiance.Euphorian,	EuphoriaId.EuphorianUseMarket, 12.25, 34.5,
			Cost.Artifactx3, 
			Benefit.EuphorianAuthority2);

	static double euphorianMarketABounds[] = { 20, 31, 30.5, 42.5 }; // this is where the upper market card sits
	static double euphorianMarketBBounds[] = { 20, 44, 30.5, 56.5 }; // this is where the lower market card sits

	EuphoriaCell euphorianBuildMarketA[] = // chits to build a market
	{
			newCell(Allegiance.Euphorian, WorkerChip.Subtype(),EuphoriaId.EuphorianBuildMarketA, 0,	18.25, 32.5, Cost.Clay, Benefit.None),
			newCell(Allegiance.Euphorian, WorkerChip.Subtype(),EuphoriaId.EuphorianBuildMarketA, 1,	18.25, 35.8, Cost.Gold, Benefit.None),
			newCell(Allegiance.Euphorian, WorkerChip.Subtype(),EuphoriaId.EuphorianBuildMarketA, 2,	18.25, 39, Cost.Gold, Benefit.None),
			newCell(Allegiance.Euphorian, WorkerChip.Subtype(),EuphoriaId.EuphorianBuildMarketA, 3,	18.25, 42, Cost.Gold, Benefit.None) 
			};

	EuphoriaCell euphorianMarketA = newCell(Allegiance.Euphorian,
			EuphoriaId.EuphorianMarketA, 28.4, 33, Cost.MarketCost,
			Benefit.EuphorianAuthorityAndInfluenceA);
	EuphoriaCell euphorianBuildMarketB[] = {
			newCell(Allegiance.Euphorian, WorkerChip.Subtype(),EuphoriaId.EuphorianBuildMarketB, 0,18.25, 45.5, Cost.Stone, Benefit.None),
			newCell(Allegiance.Euphorian, WorkerChip.Subtype(),EuphoriaId.EuphorianBuildMarketB, 1,18.25, 48.5, Cost.Gold, Benefit.None),
			newCell(Allegiance.Euphorian, WorkerChip.Subtype(),EuphoriaId.EuphorianBuildMarketB, 2,18.25, 51.5, Cost.Gold, Benefit.None),
			newCell(Allegiance.Euphorian, WorkerChip.Subtype(),EuphoriaId.EuphorianBuildMarketB, 3,18.25, 55, Cost.Gold, Benefit.None) 
			};
	EuphoriaCell euphorianMarketB = newCell(Allegiance.Euphorian,	EuphoriaId.EuphorianMarketB, 28.4, 46,
			Cost.MarketCost,
			Benefit.EuphorianAuthorityAndInfluenceB);

	EuphoriaCell euphorianTunnelMouth = newCell(Allegiance.Euphorian,EuphoriaId.EuphorianTunnelMouth, 7.75, 47.5,
			Cost.Energy,
			Benefit.CardOrGold);
	EuphoriaCell euphorianTunnelEnd = newCell(Allegiance.Euphorian,	EuphoriaId.EuphorianTunnelEnd, 23, 64.5,
			Cost.TunnelOpen,	
			Benefit.Waterx3);
	static double euphorianGeneratorBounds[] = { 33, 45, 39, 55 };
	EuphoriaCell euphorianGenerator[] = arrayOfCells(Allegiance.Euphorian, WorkerChip.Subtype(),3,4, euphorianGeneratorBounds, 
			EuphoriaId.EuphorianGenerator,
			Cost.Free, Benefit.PowerSelection);

	
	EuphoriaCell euphorianAuthority[] = {
			newCell(Allegiance.Euphorian, EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.EuphorianAuthority, 0,	38.5, 34, null, null),
			newCell(Allegiance.Euphorian, EuphoriaChip.AuthorityMarkers[0].subtype(),EuphoriaId.EuphorianAuthority, 1, 41,	31.5, null, null),
			newCell(Allegiance.Euphorian, EuphoriaChip.AuthorityMarkers[0].subtype(),EuphoriaId.EuphorianAuthority, 2, 44,	34, null, null),
			newCell(Allegiance.Euphorian, EuphoriaChip.AuthorityMarkers[0].subtype(),EuphoriaId.EuphorianAuthority, 3, 43,	38, null, null),
			newCell(Allegiance.Euphorian, EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.EuphorianAuthority, 4,	39.5, 38, null, null),
			newCell(Allegiance.Euphorian, EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.EuphorianAuthority, 5, 41,	35, null, null) };

	EuphoriaCell euphorianTunnelSteps[] = {
			newCell(Allegiance.Euphorian,EuphoriaChip.Miner.subtype(), EuphoriaId.EuphorianTunnel, 0, 8.5,54, Cost.DisplayOnly, null),
			newCell(Allegiance.Euphorian,EuphoriaChip.Miner.subtype(), EuphoriaId.EuphorianTunnel, 1, 9.75,56, Cost.DisplayOnly, null),
			newCell(Allegiance.Euphorian,EuphoriaChip.Miner.subtype(), EuphoriaId.EuphorianTunnel, 2, 10.75,58, Cost.DisplayOnly, null),
			newCell(Allegiance.Euphorian,EuphoriaChip.Miner.subtype(), EuphoriaId.EuphorianTunnel, 3, 11.75,	59.5, Cost.DisplayOnly, null),
			newCell(Allegiance.Euphorian,EuphoriaChip.Miner.subtype(), EuphoriaId.EuphorianTunnel, 4, 13,61.5, Cost.DisplayOnly, null),
			newCell(Allegiance.Euphorian,EuphoriaChip.Miner.subtype(), EuphoriaId.EuphorianTunnel, 5, 14.25,	63, Cost.DisplayOnly, null),
			newCell(Allegiance.Euphorian,EuphoriaChip.Miner.subtype(), EuphoriaId.EuphorianTunnel, 6, 15.25,	65, Cost.DisplayOnly, null),// reveal
			newCell(Allegiance.Euphorian,EuphoriaChip.Miner.subtype(), EuphoriaId.EuphorianTunnel, 7, 16.75,	66.5, Cost.DisplayOnly, null),
			newCell(Allegiance.Euphorian,EuphoriaChip.Miner.subtype(), EuphoriaId.EuphorianTunnel, 8, 18.25,	68.5, Cost.DisplayOnly, null),
			newCell(Allegiance.Euphorian,EuphoriaChip.Miner.subtype(), EuphoriaId.EuphorianTunnel, 9, 19.5,	70, Cost.DisplayOnly, null) 
			};

	static double subterranMarketBBounds[] = { 31.25, 86, 42, 98 };
	static double subterranMarketABounds[] = { 31.25, 73, 42, 85 };
	static double subterranAquiferBounds[] = { 44, 88, 49.25, 97.5 };
	EuphoriaCell subterranUseMarket = newCell(Allegiance.Subterran,	EuphoriaId.SubterranUseMarket, 23.8, 76.8,
			Cost.Artifactx3, 
			Benefit.SubterranAuthority2);
	EuphoriaCell subterranBuildMarketA[] = {
			newCell(Allegiance.Subterran, WorkerChip.Subtype(),EuphoriaId.SubterranBuildMarketA, 0,	29.5, 74.5, Cost.Gold, Benefit.None),
			newCell(Allegiance.Subterran, WorkerChip.Subtype(),EuphoriaId.SubterranBuildMarketA, 1,	29.5, 77.5, Cost.Stone, Benefit.None),
			newCell(Allegiance.Subterran, WorkerChip.Subtype(),EuphoriaId.SubterranBuildMarketA, 2,	29.5, 81, Cost.Stone, Benefit.None),
			newCell(Allegiance.Subterran, WorkerChip.Subtype(),EuphoriaId.SubterranBuildMarketA, 3,	29.5, 84, Cost.Stone, Benefit.None) 
			};
	EuphoriaCell subterranMarketA = newCell(Allegiance.Subterran,EuphoriaId.SubterranMarketA, 38.5, 75, 
			Cost.MarketCost,
			Benefit.SubterranAuthorityAndInfluenceA);
	EuphoriaCell subterranBuildMarketB[] = {
			newCell(Allegiance.Subterran, WorkerChip.Subtype(),EuphoriaId.SubterranBuildMarketB, 0,29.5, 87.5, Cost.Clay, Benefit.None),
			newCell(Allegiance.Subterran, WorkerChip.Subtype(),EuphoriaId.SubterranBuildMarketB, 1,29.5, 90.5, Cost.Stone, Benefit.None),
			newCell(Allegiance.Subterran, WorkerChip.Subtype(),EuphoriaId.SubterranBuildMarketB, 2,29.5, 93.75, Cost.Stone, Benefit.None),
			newCell(Allegiance.Subterran, WorkerChip.Subtype(),EuphoriaId.SubterranBuildMarketB, 3,29.5, 97, Cost.Stone, Benefit.None) 
			};

	EuphoriaCell subterranMarketB = newCell(Allegiance.Subterran,EuphoriaId.SubterranMarketB, 38.5, 88, 
			Cost.MarketCost,
			Benefit.SubterranAuthorityAndInfluenceB);

	EuphoriaCell subterranAquifer[] = arrayOfCells(Allegiance.Subterran, WorkerChip.Subtype(),3, 4,subterranAquiferBounds, EuphoriaId.SubterranAquifer,
			Cost.Free, 
			Benefit.WaterSelection);

	EuphoriaCell subterranAuthority[] = {
			newCell(Allegiance.Subterran,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.SubterranAuthority, 0, 49,	77, null, null),
			newCell(Allegiance.Subterran,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.SubterranAuthority, 1, 52,	74.5, null, null),
			newCell(Allegiance.Subterran,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.SubterranAuthority, 2, 55,	77, null, null),
			newCell(Allegiance.Subterran,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.SubterranAuthority, 3,	53.5, 80.5, null, null),
			newCell(Allegiance.Subterran,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.SubterranAuthority, 4, 50,	80.5, null, null),
			newCell(Allegiance.Subterran,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.SubterranAuthority, 5, 52,	78, null, null) 
			};

	EuphoriaCell subterranTunnelMouth = newCell(Allegiance.Subterran,	EuphoriaId.SubterranTunnelMouth, 64.75, 78.0, 
			Cost.Water,
			Benefit.CardOrStone);
	EuphoriaCell subterranTunnelEnd = newCell(Allegiance.Subterran,EuphoriaId.SubterranTunnelEnd, 84, 63, 
			Cost.TunnelOpen,
			Benefit.Foodx3);

	EuphoriaCell subterranTunnelSteps[] = {
			newCell(Allegiance.Subterran,EuphoriaChip.Miner.subtype(), EuphoriaId.SubterranTunnel, 0, 68,77, Cost.DisplayOnly, null),
			newCell(Allegiance.Subterran,EuphoriaChip.Miner.subtype(), EuphoriaId.SubterranTunnel, 1, 70.0,76.1, Cost.DisplayOnly, null),
			newCell(Allegiance.Subterran,EuphoriaChip.Miner.subtype(), EuphoriaId.SubterranTunnel, 2, 72,75.2, Cost.DisplayOnly, null),
			newCell(Allegiance.Subterran,EuphoriaChip.Miner.subtype(), EuphoriaId.SubterranTunnel, 3, 73.5,74.3, Cost.DisplayOnly, null),
			newCell(Allegiance.Subterran,EuphoriaChip.Miner.subtype(), EuphoriaId.SubterranTunnel, 4, 75,73.4, Cost.DisplayOnly, null),
			newCell(Allegiance.Subterran,EuphoriaChip.Miner.subtype(), EuphoriaId.SubterranTunnel, 5, 76.4,72.4, Cost.DisplayOnly, null),
			newCell(Allegiance.Subterran,EuphoriaChip.Miner.subtype(), EuphoriaId.SubterranTunnel, 6, 77.8,71.3, Cost.DisplayOnly, null),// reveal
			newCell(Allegiance.Subterran,EuphoriaChip.Miner.subtype(), EuphoriaId.SubterranTunnel, 7, 79.3,70.3, Cost.DisplayOnly, null),
			newCell(Allegiance.Subterran,EuphoriaChip.Miner.subtype(), EuphoriaId.SubterranTunnel, 8, 81.4,69.0, Cost.DisplayOnly, null),
			newCell(Allegiance.Subterran,EuphoriaChip.Miner.subtype(), EuphoriaId.SubterranTunnel, 9, 82.4,	67.5, Cost.DisplayOnly, null) 
			};

	static double wastelanderMarketABounds[] = { 70.75, 29.5, 81.5, 41.8 };
	static double wastelanderMarketBBounds[] = { 70.75, 42.5, 81.5, 54.75 };
	static double wasteLanderFarmBounds[] = { 82.25, 43.5, 88.25, 54 };
	EuphoriaCell wastelanderTunnelEnd = newCell(Allegiance.Wastelander,	EuphoriaId.WastelanderTunnelEnd, 48, 65, 
			Cost.TunnelOpen,
			Benefit.Energyx3);
	EuphoriaCell wastelanderTunnelMouth = newCell(Allegiance.Wastelander,EuphoriaId.WastelanderTunnelMouth, 73.5, 59.8, 
			Cost.Food,
			Benefit.CardOrClay);
	EuphoriaCell wastelanderTunnelSteps[] = {
			newCell(Allegiance.Wastelander,EuphoriaChip.Miner.subtype(),EuphoriaId.WastelanderTunnel, 0,67.5, 65, Cost.DisplayOnly, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.Miner.subtype(), EuphoriaId.WastelanderTunnel, 1,65.5, 65.5, Cost.DisplayOnly, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.Miner.subtype(), EuphoriaId.WastelanderTunnel, 2,64, 66, Cost.DisplayOnly, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.Miner.subtype(), EuphoriaId.WastelanderTunnel, 3,62, 67, Cost.DisplayOnly, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.Miner.subtype(), EuphoriaId.WastelanderTunnel, 4,60, 67.5, Cost.DisplayOnly, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.Miner.subtype(), EuphoriaId.WastelanderTunnel, 5,58.5, 68, Cost.DisplayOnly, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.Miner.subtype(), EuphoriaId.WastelanderTunnel, 6,56.5, 68, Cost.DisplayOnly, null),// reveal
			newCell(Allegiance.Wastelander,EuphoriaChip.Miner.subtype(), EuphoriaId.WastelanderTunnel, 7,54.5, 68.5, Cost.DisplayOnly, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.Miner.subtype(), EuphoriaId.WastelanderTunnel, 8,52.5, 69, Cost.DisplayOnly, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.Miner.subtype(), EuphoriaId.WastelanderTunnel, 9,50.5, 69.5, Cost.DisplayOnly, null) 
			};

	EuphoriaCell wastelanderUseMarket = newCell(Allegiance.Wastelander,	EuphoriaId.WastelanderUseMarket, 63.5, 33,
			Cost.Artifactx3, 
			Benefit.WastelanderAuthority2);
	EuphoriaCell wastelanderBuildMarketA[] = {
			newCell(Allegiance.Wastelander, WorkerChip.Subtype(),EuphoriaId.WastelanderBuildMarketA,	0, 69, 31, Cost.Stone, Benefit.None),
			newCell(Allegiance.Wastelander, WorkerChip.Subtype(),EuphoriaId.WastelanderBuildMarketA,	1, 69, 34, Cost.Clay, Benefit.None),
			newCell(Allegiance.Wastelander, WorkerChip.Subtype(),EuphoriaId.WastelanderBuildMarketA,	2, 69, 37, Cost.Clay, Benefit.None),
			newCell(Allegiance.Wastelander, WorkerChip.Subtype(),EuphoriaId.WastelanderBuildMarketA,	3, 69, 40.5, Cost.Clay, Benefit.None)
			};
	EuphoriaCell wastelanderMarketA = newCell(Allegiance.Wastelander,EuphoriaId.WastelanderMarketA, 78.75, 31.75,
			Cost.MarketCost, 
			Benefit.WastelanderAuthorityAndInfluenceA);
	EuphoriaCell wastelanderBuildMarketB[] = {
			newCell(Allegiance.Wastelander, WorkerChip.Subtype(),EuphoriaId.WastelanderBuildMarketB,	0, 69, 44, Cost.Gold, Benefit.None),
			newCell(Allegiance.Wastelander, WorkerChip.Subtype(),EuphoriaId.WastelanderBuildMarketB,	1, 69, 47.2, Cost.Clay, Benefit.None),
			newCell(Allegiance.Wastelander, WorkerChip.Subtype(),EuphoriaId.WastelanderBuildMarketB,	2, 69, 50.5, Cost.Clay, Benefit.None),
			newCell(Allegiance.Wastelander, WorkerChip.Subtype(),EuphoriaId.WastelanderBuildMarketB,	3, 69, 53.5, Cost.Clay, Benefit.None) 
			};

	EuphoriaCell wastelanderMarketB = newCell(Allegiance.Wastelander,EuphoriaId.WastelanderMarketB, 78.75, 45,
			Cost.MarketCost,
			Benefit.WastelanderAuthorityAndInfluenceB);

	EuphoriaCell wastelanderFarm[] = arrayOfCells(Allegiance.Wastelander,WorkerChip.Subtype(), 3, 4,	wasteLanderFarmBounds, EuphoriaId.WastelanderFarm,
			Cost.Free,
			Benefit.FoodSelection);
	EuphoriaCell wastelanderAuthority[] = {
			newCell(Allegiance.Wastelander,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.WastelanderAuthority, 0,	88, 32.5, null, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.WastelanderAuthority, 1,	90.5, 30, null, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.WastelanderAuthority, 2, 93.5, 32.5, null, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.WastelanderAuthority, 3,	92.5, 36.5, null, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.WastelanderAuthority, 4,	89, 36.5, null, null),
			newCell(Allegiance.Wastelander,EuphoriaChip.AuthorityMarkers[0].subtype(), EuphoriaId.WastelanderAuthority, 5,	90.5, 33.5, null, null) 
			};

	static double icariteCloudMineBounds[] = { 71, 3.5, 77.25, 14 };
	EuphoriaCell icariteWindSalon = newCell(Allegiance.Icarite,EuphoriaId.IcariteWindSalon, 33.75, 8.5, 
			Cost.Artifactx3,Benefit.IcariteAuthorityAndInfluence);
	EuphoriaCell icariteNimbusLoft = newCell(Allegiance.Icarite,	EuphoriaId.IcariteNimbusLoft, 46.25, 4.5, 
					Cost.Resourcex3,Benefit.IcariteAuthorityAndInfluence);
	EuphoriaCell icariteBreezeBar = newCell(Allegiance.Icarite,	EuphoriaId.IcariteBreezeBar, 57.5, 4.5,
			Cost.Bliss_Commodity,
			Benefit.IcariteInfluenceAndCardx2);
	EuphoriaCell icariteSkyLounge = newCell(Allegiance.Icarite,	EuphoriaId.IcariteSkyLounge, 69, 4.5,
			Cost.Bliss_Commodity,
			Benefit.IcariteInfluenceAndResourcex2);
	EuphoriaCell icariteCloudMine[] = arrayOfCells(Allegiance.Icarite, WorkerChip.Subtype(),3, 4,icariteCloudMineBounds, EuphoriaId.IcariteCloudMine,
			Cost.Free,	Benefit.BlissSelection);
	EuphoriaCell icariteAuthority[] = {
			newCell(Allegiance.Icarite, EuphoriaChip.AuthorityMarkers[0].subtype(),EuphoriaId.IcariteAuthority, 0, 90.5,8.1, null, null),
			newCell(Allegiance.Icarite, EuphoriaChip.AuthorityMarkers[0].subtype(),EuphoriaId.IcariteAuthority, 1, 93,5.1, null, null),
			newCell(Allegiance.Icarite, EuphoriaChip.AuthorityMarkers[0].subtype(),EuphoriaId.IcariteAuthority, 2, 96,7.6, null, null),
			newCell(Allegiance.Icarite, EuphoriaChip.AuthorityMarkers[0].subtype(),EuphoriaId.IcariteAuthority, 3, 95,11.6, null, null),
			newCell(Allegiance.Icarite, EuphoriaChip.AuthorityMarkers[0].subtype(),EuphoriaId.IcariteAuthority, 4, 91.5,11.6, null, null),
			newCell(Allegiance.Icarite, EuphoriaChip.AuthorityMarkers[0].subtype(),EuphoriaId.IcariteAuthority, 5, 93,8.6, null, null) 
			};

	// these are the acutal market cells, not the worker cells that use them.
	EuphoriaCell EuphorianMarketChipA = newCell(Allegiance.Euphorian, EuphoriaId.Market,0,euphorianMarketABounds);
	EuphoriaCell EuphorianMarketChipB = newCell(Allegiance.Euphorian, EuphoriaId.Market,1,euphorianMarketBBounds);
	EuphoriaCell SubterranMarketChipA = newCell(Allegiance.Subterran, EuphoriaId.Market,4,subterranMarketABounds);
	EuphoriaCell SubterranMarketChipB = newCell(Allegiance.Subterran, EuphoriaId.Market,5,subterranMarketBBounds);
	EuphoriaCell WastelanderMarketChipA = newCell(Allegiance.Wastelander, EuphoriaId.Market,2,wastelanderMarketABounds);
	EuphoriaCell WastelanderMarketChipB = newCell(Allegiance.Wastelander, EuphoriaId.Market,3,wastelanderMarketBBounds);
	
	EuphoriaCell markets[] = {
			EuphorianMarketChipA,
			EuphorianMarketChipB,
			SubterranMarketChipA,
			SubterranMarketChipB, 
			WastelanderMarketChipA,
			WastelanderMarketChipB,
			};
	

	
	// misc places
	protected EuphoriaCell usedArtifacts = newCell(null,ArtifactChip.Subtype(),EuphoriaId.ArtifactDiscards, 35, 23, null);
	protected EuphoriaCell unusedArtifacts = newCell(null,ArtifactChip.Subtype(),EuphoriaId.ArtifactDeck, 40, 23, null);
	protected  EuphoriaCell genericSource = newCell(null,EuphoriaChip.Nocard,	EuphoriaId.GenericPool, 38, 29, Cost.Free);
	protected  EuphoriaCell trash = newCell(null,null,	EuphoriaId.Trash, 44, 29, Cost.Free);

	double cardArea[] = { 25,16,47,33 };
	double artifactArea[] = { 26.5, 18, 45.5, 26.0};
	protected EuphoriaCell artifactBazaar[] = 
			arrayOfCells(null, null,4,	artifactArea, EuphoriaId.ArtifactBazaar,null);
	
	protected EuphoriaCell clayPit = newCell(Allegiance.Wastelander,EuphoriaChip.Clay.subtype(),EuphoriaId.ClayPit, 50,31, Cost.DisplayOnly);
	protected EuphoriaCell quarry = newCell(Allegiance.Subterran,EuphoriaChip.Stone.subtype(),EuphoriaId.StoneQuarry, 57,26, Cost.DisplayOnly);
	protected EuphoriaCell goldMine = newCell(Allegiance.Euphorian,EuphoriaChip.Gold.subtype(),EuphoriaId.GoldMine, 51,24, Cost.DisplayOnly);
	protected EuphoriaCell farm = newCell(Allegiance.Wastelander,EuphoriaChip.Food.subtype(),EuphoriaId.FarmPool, 74,26, Cost.DisplayOnly);
	protected EuphoriaCell aquifer = newCell(Allegiance.Subterran,EuphoriaChip.Water.subtype(),EuphoriaId.AquiferPool,
			59.5,22.0, Cost.DisplayOnly);
	protected EuphoriaCell generator = newCell(Allegiance.Euphorian,EuphoriaChip.Energy.subtype(),EuphoriaId.EnergyPool, 
			59.5, 27.0, Cost.DisplayOnly);
	protected EuphoriaCell bliss = newCell(Allegiance.Euphorian,EuphoriaChip.Bliss.subtype(),EuphoriaId.BlissPool, 74, 22, Cost.DisplayOnly);

	protected EuphoriaCell marketBasket = newCell(null,null,EuphoriaId.MarketBasket, 60, 55, null); 	// interchange cell
	
		
	// tight area surrounding the markets, for reward zooming when placing stars
	double euphorianMarketZone[] = {16,27,47,58};
	double wastelanderMarketZone[] = {66,25,97,55};
	double subterranMarketZone[] = {27,69,58,99};

	// general area surrounding the markets, for reward zooming when placing stars
	double euphorianMarketArea[] = {1,17,67,68};
	double wastelanderMarketArea[] = {56,15,100,65};
	double subterranMarketArea[] = {17,59,68,100};

	
	double marketBasketZone[] = {55,50,70,60};
	/*
	 * 
	 * End of the visible cell initializations
	 */
	
	// these are stacks that are only visible in puzzle mode, and there only
	// to enable manual setup.
	protected EuphoriaCell unusedRecruits = newCell(null,RecruitChip.Subtype(),	EuphoriaId.UnusedRecruits, 12, 80, Cost.DisplayOnly);
	protected EuphoriaCell unusedMarkets = newCell(null,MarketChip.Subtype(),		EuphoriaId.UnusedMarkets, 5, 80,  Cost.DisplayOnly);
	protected EuphoriaCell unusedDilemmas = newCell(null,DilemmaChip.Subtype(),		EuphoriaId.UnusedDilemmas, 5, 70,  Cost.DisplayOnly);
	protected EuphoriaCell usedRecruits = newCell(null,	RecruitChip.Subtype(),		EuphoriaId.UsedRecruits, 5, 60,  Cost.DisplayOnly);
	protected EuphoriaCell genericSink = newCell(null,EuphoriaChip.Nocard,	EuphoriaId.GenericSink, 38.4, 29, Cost.DisplayOnly);

	protected EuphoriaCell unusedWorkers[] =
			{
			newCell(null,WorkerChip.Subtype(),EuphoriaId.UnusedWorkers,0, 16, 85,  Cost.DisplayOnly,null),
			newCell(null,WorkerChip.Subtype(),EuphoriaId.UnusedWorkers,1, 13, 85,  Cost.DisplayOnly,null),
			newCell(null,WorkerChip.Subtype(),EuphoriaId.UnusedWorkers,2, 10, 85,  Cost.DisplayOnly,null),
			newCell(null,WorkerChip.Subtype(),EuphoriaId.UnusedWorkers,3, 7, 85,  Cost.DisplayOnly,null),
			newCell(null,WorkerChip.Subtype(),EuphoriaId.UnusedWorkers,4, 4, 85,  Cost.DisplayOnly,null),
			newCell(null,WorkerChip.Subtype(),EuphoriaId.UnusedWorkers,5, 1, 85,  Cost.DisplayOnly,null),
		
			};
	//protected EuphoriaCell lowerRight = newCell(null, null,EuphoriaId.Marker, 0, 100,100, PlacementCost.DisplayOnly, null);
	//protected EuphoriaCell upperLeft =  newCell(null, null,EuphoriaId.Marker, 1, 0,0, PlacementCost.DisplayOnly, null);
	/*
	 * resource and commodity management.  The cells are stocked with a few samples,
	 * but are not added or removed during the game
	 */
	void addGold(EuphoriaChip c) { /* pro forma, the stack is infinite */
		;
	}

	void addStone(EuphoriaChip c) { /* pro forma, the stack is infinite */
		;
	}

	void addClay(EuphoriaChip c) { /* pro forma, the stack is infinite */
		;
	}

	void addEnergy(EuphoriaChip c) { /* pro forma, the stack is infinite */
		;
	}

	void addBliss(EuphoriaChip c) { /* pro forma, the stack is infinite */
		;
	}

	void addFood(EuphoriaChip c) { /* pro forma, the stack is infinite */
		;
	}

	void addWater(EuphoriaChip c) { /* pro forma, the stack is infinite */
		;
	}

	void addRecruit(EuphoriaChip c) {
		usedRecruits.addChip(c);
	}

	EuphoriaChip getFood() {
		if(farm.topChip()==null) { farm.addChip(EuphoriaChip.Food); }
		return (farm.removeTop());
	}

	EuphoriaChip getBliss() {
		if(bliss.topChip()==null) { bliss.addChip(EuphoriaChip.Bliss); }
		return (bliss.removeTop());
	}

	EuphoriaChip getEnergy() {
		if(generator.topChip()==null) { generator.addChip(EuphoriaChip.Energy); }
		return (generator.removeTop());
	}

	EuphoriaChip getWater() {
		if(aquifer.topChip()==null) { aquifer.addChip(EuphoriaChip.Water); }
		return (aquifer.removeTop());
	}

	EuphoriaChip getStone() {
		if(quarry.topChip()==null) { quarry.addChip(EuphoriaChip.Stone); }
		return (quarry.removeTop());
	}

	EuphoriaChip getGold() {
		if(goldMine.topChip()==null) { goldMine.addChip(EuphoriaChip.Gold); }
		return (goldMine.removeTop());
	}

	EuphoriaChip getClay() {
		if(clayPit.topChip()==null) { clayPit.addChip(EuphoriaChip.Clay); }
		return (clayPit.removeTop());
	}
public int positionToX(double pos)
{	return((int)(G.Left(boardRect)+((G.Width(boardRect)*pos))));
}
@Override
public int cellToX(EuphoriaCell c) 
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
public int cellToY(EuphoriaCell c) 
{
	if((c!=null)&&(boardRect!=null))
	{
	return(positionToY(c.center_y/100.0));
	}
	return 0;
}

public double XtoPosition(int x)
{
	return((double)(x-G.Left(boardRect))/G.Width(boardRect));
}
public double YtoPosition(int y)
{
	return((double)(y-G.Top(boardRect))/G.Height(boardRect));
}

class Decoration { double left; double right; double top; double bottom; String name;
	Decoration(double left,double right, double top, double bottom, String name)
	{ this.left = left;
	  this.right = right;
	  this.top = top;
	  this.bottom = bottom;
	  this.name = name; }
}
// 2/5/2020 readjusted these numbers and simultaneously fixed the
// algorithm for spacing multi-line text
	Decoration decorations[] = {
			new Decoration(91.2,95.3,13.9,16.1,"Icarite\nTerritory"),
			new Decoration(88.5,93.3,38.4,40.6,"Wastelander\nTerritory"),

			new Decoration(29.55, 34.95, 14.7,16.4, "Wind Salon"),
			new Decoration(41.0, 46.0,  14.7,16.4, "Nimbus Loft"),
			new Decoration(52.5,57.0,  14.7,16.4, "Breeze Bar"),
			new Decoration(64.1,68.5,  14.7,16.4, "Sky Lounge"),
			new Decoration(58.3,65.2,39.1,41.6,"Ark of Fractured\nMemories"),
			new Decoration(39.0,43.7,40.15,42.25,"Euphorian\nTerritory"),
			new Decoration(39.0,43.7,43.6,45.1,"Generator"),
			new Decoration(6.5,14.3,40.3,42.8,"Incenerator of\nHistorical Accuracy"),
			new Decoration(2.7,9.2,44.0,45.5,"Euphorian Tunnel"),
			new Decoration(3.4,12.6,86.5,88.0,"Worker Activation"),
			new Decoration(19.3,25.1,82.1,84.2,"Free Press of\nHarsh Reality"),
			new Decoration(49.6,54.0,82.1,84.2,"Subterran\nTerritory"),
			new Decoration(50.0,53.8,85.1,86.6,"Aquifer"),
			new Decoration(59.8,66.4,74.6,76.1,"Subterran Tunnel"),
			new Decoration(68.6,76.3,56.0,57.5,"Wastelander Tunnel"),
			new Decoration(79.6,87.1,78.6,80.3,"Allegiance Track"),
			new Decoration(77.2,80.6,82.0,83.9,"Generator"),
			new Decoration(77.5,80.6,86.0,87.7,"Aquifer"),
			new Decoration(77.6,80.6,89.9,91.5,"Farm"),
			new Decoration(77.2,80.6,93.8,95.4,"Cloud Mine"),
			
			new Decoration(84.6,87.6,  82.0,83.6,"Euphorian\nTunnel"),
			new Decoration(84.6,87.6,  86.0,87.6,"Subterran\nTunnel"),
			new Decoration(84.6,87.6,  89.8,91.6,"Wastelander\nTunnel"),
			new Decoration(84.6,87.6,  93.8,95.4,"Icarite\nTerritory"),
			
			new Decoration(89.0,92.8,42.3,43.7,"Farm"),

		};
	public Decoration[]decorations()
	{
	return(decorations);
}

}
