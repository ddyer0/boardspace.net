package manhattan;

import java.awt.Rectangle;

import lib.Bitset;
import lib.Digestable;
import lib.G;
import lib.OStack;
import lib.Random;
import static manhattan.ManhattanMovespec.*;

import manhattan.ManhattanConstants.Benefit;
import online.game.CommonMoveStack;
import online.game.replayMode;

class PendingBenefitStack extends OStack<Benefit> implements Digestable
{
	public Benefit[] newComponentArray(int sz) {
		return new Benefit[sz];
	}
	public long Digest(Random r) {
		long v = 0;
		for(int i=0;i<size(); i++) { v ^= r.nextInt()*(elementAt(i).ordinal()+124125); }
		return v;
	}
}

public class PlayerBoard implements ManhattanConstants 
{	
	private char colId;
	ManhattanCell allCells;
	int nPlacedWorkers = 0;
	boolean hasTestedBomb = false;
	boolean approvedNorthKorea = false;
	// this is used by the robot to sequence setting and approving the contribution
	// it's reset every time someone makes a change, so the robot will reconsider
	boolean hasSetContribution = false;
	int koreanContribution = 0;
	int nUranium = 0;
	int nPlutonium = 0;
	int nEspionage = 0;
	private Bitset<TurnOption> turnOptions = new Bitset<TurnOption>();
	public boolean testOption(TurnOption op)
	{
		return turnOptions.test(op);
	}
	public void setOption(TurnOption op)
	{
		turnOptions.set(op);
	}
	public long saveOptions() { return turnOptions.members(); }
	public void restoreOptions(long v) { turnOptions.setMembers(v); }
	
	public String toString() { return ("<pb "+color+" "+boardIndex+">"); }
	
	private ManhattanCell C(ManhattanId id,Type t,int row) 
	{ ManhattanCell c = new ManhattanCell(id); 
	  c.next = allCells;
	  c.type = t;
	  c.color = color;
	  allCells = c;
	  c.col = colId;
	  c.row = row;
	  // the randomv values for the stockpile and buildings are troublesome
	  // because they are dynamically created, and copy boards create them
	  // in a different sequence.
	  c.randomv = new Random((id.ordinal()+1)*(c.row+1)).nextLong();
	  return c;
	}
	private ManhattanCell newBuildingCell(int n) 
	{ 
		ManhattanCell c = C(ManhattanId.Building,Type.Building,n);
		c.row = n;
		return c;
		
	}
	private ManhattanCell[] CA(ManhattanId id,Type type,int n)
	{	ManhattanCell ca[] = new ManhattanCell[n];
		for(int i=0;i<n;i++) 
		{ ManhattanCell c = ca[i] = C(id,type,i); 
		  c.col = colId;
		  c.color = color;
		  c.type = type;
		  switch(type)
		  {
		  case Fighter:
		  case Bomber:
			  	// this changes the pickup behavior
			  	c.cost = Cost.FixedPool;
		  		break;
		  default: break;
		  }
		  allCells = c;
		}
		return ca;
	}
	
	MColor color;
	ManhattanChip chip;
	ManhattanBoard b;
	ManhattanChip background;
	ManhattanChip fighter;
	ManhattanChip bomber;
	ManhattanChip scientist;
	ManhattanChip engineer;
	ManhattanChip worker;
	static final int MAXFIGHTERS = 10;
	static final int MAXBOMBERS = 10;
	ManhattanCell airstrikeHelp = C(ManhattanId.AirstrikeHelp,Type.Help,0);
	// cells on the player board
	ManhattanCell fighters[] = CA(ManhattanId.Fighters,Type.Fighter,MAXFIGHTERS+1);
	int nFighters = 0;
	ManhattanCell bombers[] = CA(ManhattanId.Bombers,Type.Bomber,MAXBOMBERS+1);
	int nBombers = 0;
	CellStack buildings = new CellStack();
	ManhattanCell workers = C(ManhattanId.Workers,Type.Worker,0);
	ManhattanCell scientists = C(ManhattanId.Scientists,Type.Worker,0);
	ManhattanCell engineers = C(ManhattanId.Engineers,Type.Worker,0);
	ManhattanCell cashDisplay = C(ManhattanId.Cash,Type.Coin,0);
	ManhattanCell yellowcakeDisplay = C(ManhattanId.Yellowcake,Type.Yellowcake,0);
	ManhattanCell personality = C(ManhattanId.Personality,Type.Personalities,0);
	CellStack stockpile = new CellStack();
	ManhattanCell selectedBomb = null;
	ManhattanCell bombtest = C(ManhattanId.Bombtest,Type.Bombtest,0);
	
	PendingBenefitStack pendingChoices = new PendingBenefitStack();
	
	public void addFighter(int n,replayMode replay)
	{
		ManhattanCell c = fighters[nFighters];
		if(c.topChip()!=null) { c.removeTop(); }
		int newpos = Math.min(nFighters+n,fighters.length-1);
		G.Assert(newpos>=0,"negative fighters!");
		ManhattanCell d = fighters[newpos];
		d.addChip(fighter);
		nFighters = newpos;
		if(replay.animate){
			b.animationStack.push(c);
			b.animationStack.push(d);
		}
		if(n<0) { bumpForLemay(replay); }
	}
	public void addBomber(int n,replayMode replay)
	{
		ManhattanCell c = bombers[nBombers];
		if(c.topChip()!=null) { c.removeTop(); }
		int newpos = Math.min(nBombers+n,bombers.length-1);
		G.Assert(newpos>=0,"negative bombers!");
		ManhattanCell d = bombers[newpos];
		d.addChip(bomber);
		nBombers = newpos;
		if(replay.animate){
			b.animationStack.push(c);
			b.animationStack.push(d);
		}
		if(n<0) { bumpForLemay(replay); }
	}
	
	public void positionCells()
	{	airstrikeHelp.setPosition(0.978,0.16);
		ManhattanCell.setPosition(fighters,  0.09,0.11,  0.92,0.11);
		ManhattanCell.setPosition(bombers,   0.09,0.215, 0.92,0.215);
		ManhattanCell.setPosition2(buildings, 0.14,0.45, 0.87,0.77 );
		workers.setPosition(0.05,-0.015);
		scientists.setPosition(0.65,-0.015);
		engineers.setPosition(0.35,-0.015);
		
		cashDisplay.setPosition(-0.07,0.21);
		yellowcakeDisplay.setPosition(0.95,-0.0);
	}
	public void setPosition(ManhattanCell c,Rectangle r,double xoff,double yoff)
	{	c.setPosition(boardRect,r,xoff,yoff);
	}
	public int setPosition(CellStack c,Rectangle r)
	{	int size =c.size();
		int rows = Math.max(2,(int)(Math.sqrt(size)+0.9));
		int w = G.Width(r);
		int h = G.Height(r);
		int xstep = w/rows;
		int ystep= h/rows;
		int t = G.Top(r)+ystep/2;
		int left = G.Left(r)+xstep/2;
		int idx = 0;
		for(int x=0;x<rows;x++)
		{	for(int y=0;y<rows && idx<size; y++)
			{
			ManhattanCell cell = c.elementAt(idx);
			cell.setPosition(boardRect,(int)(left+y*xstep),(int)(t+x*ystep));
			idx++;
			}
		}
		return xstep;
	}


	int boardIndex = 0;
	
	PlayerBoard(ManhattanBoard bo,char col,MColor co,ManhattanChip ch,
			ManhattanChip back,ManhattanChip fi,ManhattanChip bom,ManhattanChip wo,ManhattanChip sc,ManhattanChip en)
	{
		b = bo;
		colId = col;
		color = co;
		chip = ch;
		fighter = fi;
		bomber = bom;
		background = back;
		scientist = sc;
		worker = wo;
		engineer = en;
		doInit();
	}
	public void doInit()
	{	for(ManhattanCell c = allCells; c!=null; c=c.next) { c.reInit(); c.color = color; }
		nFighters = 0;
		nBombers = 0;
		addFighter(1,replayMode.Replay);
		addBomber(1,replayMode.Replay);
		pendingChoices.clear();
		
		// special handling for the stockpile, which is expandable with newly created cells
		while(stockpile.size()>0) 
		{
			ManhattanCell c = stockpile.pop();
			removeCell(c);
		}
		selectedBomb = null;
		expandStockpile(1);
		// special handling for the stockpile, which is expandable with newly created cells
		while(buildings.size()>10)
		{
			ManhattanCell c = buildings.pop();
			removeCell(c);
		}
		expandBuildings(9);
		airstrikeHelp.reInit();
		airstrikeHelp.addChip(ManhattanChip.Question);
		nPlacedWorkers = 0;
		nUranium = 0;
		nPlutonium = 0;
		nEspionage = 0;
		turnOptions.clear();
		hasTestedBomb = false;
		approvedNorthKorea = false;
		hasSetContribution = false;
		koreanContribution = 0;
		//yellowcakeDisplay.addChip(ManhattanChip.Yellowcake);
	}
	public boolean hasPersonality(ManhattanChip ch)
	{
		return personality.topChip()==ch;
	}
	public void bumpForLemay(replayMode replay)
	{
		if(hasPersonality(ManhattanChip.Lemay))
		{	// give fighters and/or bombers
		if(nFighters<MAXFIGHTERS && !testOption(TurnOption.LemayFighter))
			{ 
			
			addFighter(1,replay);
			setOption(TurnOption.LemayFighter);
			if(replay.animate) {
				b.animationStack.push(personality);
				b.animationStack.push(fighters[nFighters]);
			}
			}
		if(nBombers<MAXBOMBERS  && !testOption(TurnOption.LemayBomber)) 
			{ 
			
			addBomber(1,replay); 
			setOption(TurnOption.LemayBomber);
			if(replay.animate) {
				b.animationStack.push(personality);
				b.animationStack.push(bombers[nBombers]);
			}
			}
		}
	}

	public void startTurn(replayMode replay)
	{
		turnOptions.clear();
		bumpForLemay(replay);
	}
	public void endTurn(replayMode replay)
	{
		bumpForLemay(replay);
	}
	public long Digest(Random r)
	{	long v = 0;
		v ^= b.Digest(r,boardIndex);
		v ^= b.Digest(r,fighters);
		v ^= b.Digest(r,bombers);
		v ^= b.Digest(r,nFighters);
		v ^= b.Digest(r,nBombers);
		v ^= b.Digest(r,buildings);
		v ^= b.Digest(r,workers);
		v ^= b.Digest(r,engineers);
		v ^= b.Digest(r,scientists);
		v ^= b.Digest(r,cashDisplay);
		v ^= b.Digest(r,yellowcakeDisplay);
		v ^= b.Digest(r,stockpile);
		v ^= b.Digest(r,bombtest);
		v ^= b.Digest(r,pendingChoices);
		v ^= b.Digest(r,nPlacedWorkers);
		v ^= b.Digest(r,nUranium);
		v ^= b.Digest(r,hasTestedBomb);
		v ^= b.Digest(r,approvedNorthKorea);
		//v ^= b.Digest(r,hasSetContribution); excluded from digest and sameboard because it is ephemeral
		v ^= b.Digest(r,koreanContribution);
		v ^= b.Digest(r,nPlutonium);
		v ^= b.Digest(r,nEspionage);
		v ^= b.Digest(r,turnOptions);
		return v;
	}

	public ManhattanCell getCell(ManhattanId id,int row)
	{
		switch(id)
		{
		default: throw G.Error("Not expecting id ",id);
		case AirstrikeHelp: return airstrikeHelp;
		case Workers:	return(workers);
		case Scientists:	return scientists;
		case Engineers: return engineers;
		case Fighters: return fighters[row];
		case Bombers: return bombers[row];
		case Building: 
			// special case, the buildings array is expandable
			expandBuildings(row);
			return buildings.elementAt(row); 
		case Cash: return cashDisplay;
		case Yellowcake: return yellowcakeDisplay;
		case Personality: return personality;
		case Stockpile: 
			// special case, the stockpile array is expandable
			expandStockpile(row); 
			return stockpile.elementAt(row);
		}
	}
	
	public ManhattanCell getCell(ManhattanCell c)
	{
		return (c==null) 
					? null 
					: getCell(c.rackLocation(),c.row);
	}
	
	// if the number of building slots is shrinking, remove a cell from allCells
	private void removeCell(ManhattanCell c)
	{
		ManhattanCell prev = null;
		for(ManhattanCell d = allCells; d!=null; prev=d,d=d.next)
		{
			if(c==d) 
				{ if(prev!=null) { prev.next = c.next; }
				else { allCells = c.next; }
				return;
				}
			}
		G.Error("can't find %s to remove",c);
	}
	public void setBuildingsSize(int n)
	{	while(buildings.size()<n) { expandBuildings(n-1); }
		while(buildings.size()>n) 
		{ ManhattanCell c = buildings.pop(); 
		  removeCell(c);
		}
	}	
	private ManhattanCell expandBuildings(int newindex)
	{	int oldSize = buildings.size();
		ManhattanCell newcell = null;
		while(oldSize<newindex+1) 
			{ newcell = newBuildingCell(oldSize);
			  buildings.push(newcell); 
			  oldSize++;
			}
		return newcell;
	}
	
	public ManhattanCell findEmptyStockPile()
	{
		for(int lim=stockpile.size()-1; lim>=0; lim--)
		{
		ManhattanCell oldCell = stockpile.elementAt(lim);
		if(oldCell.isEmpty()) { return oldCell; }
		}
		return expandStockpile();
	}
	public void expandStockpile(int n)
	{
		while(stockpile.size()<n+1) { expandStockpile(); }
	}
	public void setStockpileSize(int n)
	{	while(stockpile.size()<n) { expandStockpile(); }
		while(stockpile.size()>n) 
		{ ManhattanCell c = stockpile.pop(); 
		  removeCell(c);
		}
	}
	public ManhattanCell  expandStockpile()
	{	
		ManhattanCell newcell = C(ManhattanId.Stockpile,Type.Bomb,stockpile.size());
		stockpile.push(newcell);
		return newcell;
	}
	
	public void copyFrom(PlayerBoard other)
	{
		boardIndex = other.boardIndex;
		b.copyFrom(fighters,other.fighters);
		nFighters = other.nFighters;
		b.copyFrom(bombers,other.bombers);
		nBombers = other.nBombers;
		setBuildingsSize(other.buildings.size());
		b.copyFrom(buildings,other.buildings);
		
		b.copyFrom(cashDisplay,other.cashDisplay);
		yellowcakeDisplay.copyFrom(other.yellowcakeDisplay);
		
		setStockpileSize(other.stockpile.size()); 

		
		b.copyFrom(stockpile,other.stockpile);
		selectedBomb = getCell(other.selectedBomb);
		bombtest.copyFrom(other.bombtest);
		
		workers.copyFrom(other.workers);
		scientists.copyFrom(other.scientists);
		engineers.copyFrom(other.engineers);
		pendingChoices.copyFrom(other.pendingChoices);
		personality.copyFrom(other.personality);
		nPlacedWorkers = other.nPlacedWorkers;
		nUranium = other.nUranium;
		hasTestedBomb = other.hasTestedBomb;
		approvedNorthKorea = other.approvedNorthKorea;
		hasSetContribution = other.hasSetContribution;
		koreanContribution = other.koreanContribution;
		nPlutonium = other.nPlutonium;
		turnOptions.copy(other.turnOptions);
		nEspionage = other.nEspionage;
		sameBoard(other);
	}
	public void sameBoard(PlayerBoard other)
	{	G.Assert(color==other.color,"color mismatch");
		G.Assert(boardIndex==other.boardIndex,"boardIndex mismatch");
		G.Assert(b.sameContents(fighters,other.fighters),"fighter mismatch");
		G.Assert(b.sameContents(bombers,other.bombers),"bomber mismatch");
		G.Assert(nFighters==other.nFighters,"nFighters mismatch");
		G.Assert(nBombers==other.nBombers,"nBombers mismatch");
		G.Assert(b.sameContents(buildings,other.buildings),"seeBuilding mismatch");
		G.Assert(workers.sameContents(other.workers),"workers mismatch");
		G.Assert(scientists.sameContents(other.scientists),"scientists mismatch");
		G.Assert(engineers.sameContents(other.engineers),"engineers mismatch");
		G.Assert(pendingChoices.eqContents(other.pendingChoices),"pending choices mismatch");
		G.Assert(cashDisplay.sameContents(other.cashDisplay),"cashdisplay mismatch");
		G.Assert(yellowcakeDisplay.sameContents(other.yellowcakeDisplay),"yellowcake mismatch");
		G.Assert(personality.sameContents(other.personality),"personality mismatch");
		G.Assert(nPlacedWorkers==other.nPlacedWorkers,"nplacedworkers mismatch");
		G.Assert(nUranium==other.nUranium,"nUranium mismatch");
		G.Assert(hasTestedBomb==other.hasTestedBomb,"hasTestedBomb mismatch");
		G.Assert(approvedNorthKorea==other.approvedNorthKorea,"approvedNorthKorea mismatch");
		// excluded from digest and sameboard because it is ephemeral
		//G.Assert(hasSetContribution==other.hasSetContribution,"hasSetContribution mismatch");
		G.Assert(koreanContribution==other.koreanContribution,"koreanContribution mismatch");
		G.Assert(nPlutonium==other.nPlutonium,"nPlutonium mismatch");
		G.Assert(nEspionage==other.nEspionage,"nEspionage mismatch");
		G.Assert(turnOptions.members()==other.turnOptions.members(),"same turn options");
		G.Assert(b.sameContents(stockpile,other.stockpile),"stockpile mismatch");
		/// for debugging only, normally this is redudnant because of the digest check in the main board
		///G.Assert(Digest1(new Random(424))==other.Digest1(new Random(424)),"sameboard ok, digest mismatched");
	}
	
	
	private int changeDisplay(int old,int n,ManhattanCell display[],replayMode replay)
	{	int newv = Math.max(0,Math.min(n+old,display.length-1));
		display[old].removeChip(chip);
		display[newv].addChip(chip);
		if(replay.animate)
		{
			b.animationStack.push(display[old]);
			b.animationStack.push(display[newv]);
		}
		return newv;
	}
	public void addUranium(int n,replayMode replay)
	{	nUranium = changeDisplay(nUranium,n,b.seeUranium,replay);
	}

	public void addPlutonium(int n,replayMode replay)
	{	nPlutonium = changeDisplay(nPlutonium,n,b.seePlutonium,replay);
	}
	public void addEspionage(int n,replayMode replay)
	{	nEspionage = changeDisplay(nEspionage,n,b.seeEspionage,replay);
	}
	

	Rectangle boardRect = new Rectangle();
	public void setRectangle(Rectangle r)
	{	if(!G.sameRects(boardRect,r))
		{	G.copy(boardRect,r);
			positionCells();
		}
	}
	
	public int cellToX(ManhattanCell c)
	{
		return G.Left(boardRect) +(int)( G.Width(boardRect)*c.xpos);
	}
	public int cellToY(ManhattanCell c)
	{
		return G.Top(boardRect)+(int)(G.Height(boardRect)*c.ypos);
	}
	

	private  void dropWorker(CommonMoveStack all,ManhattanCell c,int who,int op)
	{	if(all!=null) { all.push(new ManhattanMovespec(op,workers,-1,c,who)); }
	}
	private  void dropScientist(CommonMoveStack all,ManhattanCell c,int who,int op,ManhattanCell source)
	{	if(all!=null) 
		{ ManhattanMovespec m = new ManhattanMovespec(op,source,-1,c,who);
		  all.push(m); 
		}
	}
	private  void dropEngineer(CommonMoveStack all,ManhattanCell c,int who,int op,ManhattanCell source)
	{	if(all!=null) { all.push(new ManhattanMovespec(op,source,-1,c,who)); }
	}
	void returnWorker(ManhattanCell c,ManhattanChip ch,replayMode replay)
	{	ManhattanCell dest = null;
		switch(ch.workerType)
		{
		case S:	dest = scientists; break;
		case E: dest = engineers; break;
		case L: dest = workers; break;
		default: G.Error("Not expecting %s",ch);
		}
		dest.addChip(ch);
		if(replay.animate)
		{
			b.animationStack.push(c);
			b.animationStack.push(dest);
		}
	}
	void retrieveGrays(ManhattanCell c,replayMode replay)
	{
		for(int lim=c.height()-1; lim>=0; lim--)
		{
			ManhattanChip ch = c.chipAtIndex(lim);
			if(ch.color==MColor.Gray) 
				{ b.availableWorkers.addChip(ch); 
				  c.removeChipAtIndex(lim);
				  if(replay.animate)
				  {
					  b.animationStack.push(c);
					  b.animationStack.push(b.availableWorkers);
				  }
				}
		}
	}
	
	void retrieveAllWorkers(replayMode replay)
	{
		retrieveGrays(workers,replay);
		retrieveGrays(scientists,replay);
		retrieveGrays(engineers,replay);
		for(int lim=buildings.size()-1; lim>=0;lim--)
		{	
			retrieveFrom(buildings.elementAt(lim),replay);
		}
		for(int lim = stockpile.size()-1; lim>=0; lim--)
		{
			retrieveFrom(stockpile.elementAt(lim),replay);
		}
		nPlacedWorkers=0;
	}
	private void retrieveFrom(ManhattanCell c,replayMode replay)
	{
			if(c.height()>1)
			{	// the bottom of the stack is a building, and there's something on top
				for(int lim=c.height()-1; lim>=1; lim--)
				{
					ManhattanChip ch = c.chipAtIndex(lim);
					switch(ch.type)
					{
					case Worker:
						c.removeChipAtIndex(lim);
						b.returnWorker(c,ch,replay);
						break;
					case Bombbuilt:
					case Bombtest:
					case Bombloaded:
					case Bomber:
					case Damage:	// leave the damage
						break;
					default:
							throw G.Error("not expecting %s",ch);
					}					
					// with espionage, some of these workers may be visitors
				}
			}
			c.partiallyVacated = false;	// definitely not partially partiallyVacated
		
	}
	//
	// retrieve workers of a particular color from buildings of another player
	// this can leave behind cells which are partially populated by gray workers
	//
	void retrieveColoredWorkers(MColor color,replayMode replay)
	{
		for(int blim=buildings.size()-1; blim>=0; blim--)
		{	boolean some = false;
			ManhattanCell c = buildings.elementAt(blim);
			if(c.height()>1)
			{	// the bottom of the stack is a building, and there's something on top
				for(int lim=c.height()-1; lim>=1; lim--)
				{
					ManhattanChip ch = c.chipAtIndex(lim);
					if(ch.type==Type.Damage) {}
					else {
					G.Assert(ch.type==Type.Worker,"should be a worker");
					if(ch.color==color) 
					{ 
					c.removeChipAtIndex(lim);  
					// with espionage, some of these workers may be visitors
					b.returnWorker(c,ch,replay);
					}
					else { some = true; }
					}
				}
			}
			c.partiallyVacated = some;	// some true if partially partiallyVacated
		}
	}
	int nAvailableWorkers()
	{
		return workers.height()+engineers.height()+scientists.height();
	}
	// if there is only one kind of worker available, return it
	// otherwise return null
	ManhattanCell onlyKindOfWorker()
	{	ManhattanCell c = engineers.height()>0 ? engineers : null;
		if(scientists.height()>0) { if(c!=null) { return null; } else { c = scientists; }}
		if(workers.height()>0) { if(c!=null) { return null; } else { c = workers; }}
		return c;
	}
	boolean hasDesignsAvailable()
	{	
		for(int lim = stockpile.size()-1; lim>=0; lim--)
		{	if(designIsAvailable(stockpile.elementAt(lim))) { return true; }
		}
		return false;
	}
	int nDesignsAvailable()
	{	int n=0;
		for(int lim = stockpile.size()-1; lim>=0; lim--)
		{	if(designIsAvailable(stockpile.elementAt(lim))) 
			{ n++;
			}
		}
		return n;
	}
	public boolean designIsAvailable(ManhattanCell c)
	{
		if(c.height()==1)
			{
				ManhattanChip ch = c.chipAtIndex(0);
				if(ch.type==Type.Bomb) { return true; }
			}
		return false;
	}
	// an ordinary worker is picked or known to be available, can it be placed on 'c'
	private boolean workerSatisfies(CommonMoveStack all,ManhattanCell c,Cost requirements,int who,int op)
	{	boolean some = false;
		int prepicked = (op==MOVE_FROM_TO) ? 0 : 1 ;
		switch(requirements)
		{
		case None:
		default: throw G.Error("Not expecting cost %s",requirements);
		case Airstrike:
			// at present, there's no requirement you actually have any planes
			// you can take airstrike action as a defensive measure.
			some = true; // nFighters>0 || nBombers>0;
			break;
		case AnyWorkerAndBomb:
			some = hasDesignsAvailable();
			break;
		case ScientistOrWorkerAndMoney:	// buying a building
			some = b.seeBuilding[0].height()>0 && cashDisplay.cash>=b.seeBuilding[0].cash;
			break;
		
		case Cash:	// purchase a building by a regular worker
			some = c.cash<=cashDisplay.cash;
			break;
			
		case AnyWorker:
			some = true;
			break;
		case AnyWorkerAnd3:			
			some = cashDisplay.cash>=3;
			break;
			
		case AnyWorkerAnd5:
			some = cashDisplay.cash>=5;
			break;
			
		case AnyWorkerAnd3Y:
			some = yellowcakeDisplay.height()>=3;
			break;

		case Scientist:
		case ScientistAnd2Y:
		case ScientistAnd2YAnd3:
		case EngineerAndMoney:
		case Engineer:
		case ScientistOrEngineer:
		case ScientistAndEngineerAndBombDesign:
		case ScientistAndBombDesign:
			break;
			
		case  Any2WorkersAndRetrieve:	// germany
			some = ((nAvailableWorkers()+prepicked>=2) && hasRetrieveSorEMoves());
			break;
			
		// building cost
			
		case Any2WorkersAndCash:
			if(cashDisplay.cash==0) { break; }	// must have cash
			//$FALL-THROUGH$
		case Any2Workers:
			{
			some = nAvailableWorkers()+prepicked>=2;
			}
			break;
		case Any3Workers:
			{
			some=nAvailableWorkers()+prepicked>=3; 
			}
			break;
			
		case Engineer2:
		case Engineer3:
		case Scientist2And5YellowcakeAnd2:
		case Scientists2And6YellowcakeAnd7:
		case ScientistAnd3YellowcakeAnd1:
		case ScientistAnd2YellowcakeAnd2:
		case ScientistAnd1YellowcakeAnd3:
		case Scientist2And4YellowcakeAnd3:
		case ScientistAnd4YellowcakeAnd4:
		case Scientists2And3YellowcakeAnd4:
		case ScientistAnd3YellowcakeAnd5:
		case Scientist2And2YellowcakeAnd5:
		case ScientistAnd1Yellowcake: 
		case ScientistAnd1UraniumOr2Yellowcake:
		case ScientistAnd1Uranium:
		case Scientist2And3Yellowcake:
		case Scientists2And1UraniumOr4Yellowcake:
		case ScientistAnd5Yellowcake:
		case Scientist2And6Yellowcake:
		case Scientists2And1UraniumOr7Yellowcake:
		case Scientists3And8Yellowcake:
		case Scientist2And1UraniumOr3Yellowcake:
			break;
			
			// bomb cost
		case ScientistAnd3Uranium: 
		case ScientistAndEngineerAnd3Uranium:
		case ScientistAndEngineerAnd4Uranium:
		case ScientistAndEngineer2And4Uranium:
		case ScientistAndEngineer2And5Uranium:
		case Scientist2AndEngineer2And5Uranium:
		case Scientist2AndEngineer2And6Uranium:
		case ScientistAndEngineerAnd4Plutonium:
		case ScientistAndEngineer2And4Plutonium:
		case Scientist2AndEngineer2And5Plutonium:
		case Scientist2AndEngineer2And6Plutonium:
		case ScientistAndEngineer2And5Plutonium:
		case Scientist2AndEngineer3And6Plutonium:			
		case Scientist2AndEngineer3And7Plutonium:
		case Scientist2AndEngineer4And7Plutonium:
			break;
		// israeli bomb costs
		case Uranium3:
		case ScientistOrEngineerAnd3Uranium:
		case ScientistOrEngineerAnd4Uranium:
		case ScientistOrEngineer2And4Uranium:
		case ScientistOrEngineer2And5Uranium:
		case Scientist2OrEngineer2And6Uranium:
		case Scientist2OrEngineer2And5Uranium:
		case ScientistOrEngineerAnd4Plutonium:
		case ScientistOrEngineer2And4Plutonium:
		case Scientist2OrEngineer2And6Plutonium:
		case Scientist2OrEngineer2And5Plutonium:
		case ScientistOrEngineer2And5Plutonium:
		case Scientist2OrEngineer3And6Plutonium:
		case Scientist2OrEngineer3And7Plutonium:
		case Scientist2OrEngineer4And7Plutonium:
			break;
		}
		
		if(some)
		{
			dropWorker(all,c,who,op);
		}
		return some;
	}
	int nScientistsAvailable()
	{	int h = scientists.height();
		if(hasPersonality(ManhattanChip.Oppenheimer)
				&& !testOption(TurnOption.OppenheimerWorker))
		{
			if(h>0 || workers.height()>0) { h++; }
		}
		return h;
	}
	int nEngineersAvailable()
	{	int h = engineers.height();
		if(hasPersonality(ManhattanChip.Groves)
				&& !testOption(TurnOption.GrovesWorker))	
		{ 	if(h>0 || workers.height()>0) { h++; }
		}
		return h;
	}
	// an engineer is picked or known to be available
	private boolean engineerSatisfies(CommonMoveStack all,ManhattanCell c,Cost requirements,int who,int op,ManhattanCell source)
	{	boolean some = false;
		int prepicked = (op==MOVE_FROM_TO) ? 0 : 1;
		int actualPicked = prepicked;
		if(hasPersonality(ManhattanChip.Groves)
				&& source==engineers		// using 1 engineer as 2
				&& !testOption(TurnOption.GrovesWorker))	
		{ 	// pretend we picked 2 workers up
			prepicked++; 
		}
		switch(requirements)
		{
		case None:
		default: throw G.Error("Not expecting cost %s",requirements);
		
		case Airstrike:
			// at present, there's no requirement you actually have any planes
			// you can take airstrike action as a defensive measure.
			some = true; // nFighters>0 || nBombers>0;
			break;
		case AnyWorkerAndBomb:
			some = hasDesignsAvailable();
			break;
		
		case Cash:	// purchase a building by an engineer
			some = c.row<=1 || c.cash<=cashDisplay.cash;
			break;
			
		case AnyWorker:
			some = true;
			break;
			
		case AnyWorkerAnd3:
			some = cashDisplay.cash>=3;
			break;
		case AnyWorkerAnd5:
			some= cashDisplay.cash>=5;
			break;
		case AnyWorkerAnd3Y:
			some = yellowcakeDisplay.height()>=3;
			break;

		case Scientist:
		case ScientistAnd2Y:
		case ScientistAnd2YAnd3:
			break;
			
		case Engineer:
			dropEngineer(all,c,who,op,source);
			some = true;
			break;
			
		case EngineerAndMoney:	// buying a building
			some = b.seeBuilding[0].height()>0 && ((c.row<=1) || (cashDisplay.cash >= b.seeBuilding[2].cash));
			break;
			
		case ScientistOrWorkerAndMoney:	// no, we're playing an engineer
			break;

		case ScientistOrEngineer:
			some = true;
			break;
			
		case ScientistAndEngineerAndBombDesign:
			some = nScientistsAvailable()>=1 && b.seeCurrentDesigns.height()>0;
			break;
			
		// building cost
		case Any2WorkersAndCash:
			if(cashDisplay.cash==0) { break; }	// must have cash
			//$FALL-THROUGH$
		case Any2Workers:
			some = nAvailableWorkers()+actualPicked>=2;	// groves not applicable
			break;
		case Engineer2:
			some = engineers.height()+prepicked>=2;
			break;
		case Any3Workers:
			some = nAvailableWorkers()+actualPicked>=3;	// groves not applicable
			break;
		case Engineer3:
			some = engineers.height()+prepicked>=3;
			break;
			
		case Scientist2And5YellowcakeAnd2:
		case Scientists2And6YellowcakeAnd7:
		case ScientistAnd3YellowcakeAnd1:
		case ScientistAnd2YellowcakeAnd2:
		case ScientistAnd1YellowcakeAnd3:
		case Scientist2And4YellowcakeAnd3:
		case ScientistAnd4YellowcakeAnd4:
		case Scientists2And3YellowcakeAnd4:
		case ScientistAnd3YellowcakeAnd5:
		case Scientist2And2YellowcakeAnd5:
		case ScientistAnd1Yellowcake:
		case ScientistAnd1UraniumOr2Yellowcake:
		case ScientistAnd1Uranium:
		case Scientist2And3Yellowcake:
		case Scientists2And1UraniumOr4Yellowcake:
		case ScientistAnd5Yellowcake:
		case Scientist2And6Yellowcake:
		case Scientists2And1UraniumOr7Yellowcake:
		case Scientists3And8Yellowcake:
		case Scientist2And1UraniumOr3Yellowcake:
		case ScientistAndBombDesign:	// france
			break;

			// israeli bomb costs
		case Uranium3:
			break;
		case ScientistOrEngineerAnd3Uranium:	// no scientist required
			some = nUranium>=3;
			break;
			
		case ScientistOrEngineerAnd4Uranium:
			some = nUranium>=4;		// no scientist required
			break;
			
		case ScientistOrEngineer2And4Uranium:  // no scientist required
			some = (engineers.height()+prepicked>=2) && nUranium>=4;
			break;

		case ScientistOrEngineer2And5Uranium:
			some = (engineers.height()+prepicked>=2) && nUranium>=5;
			break;
			
		case Scientist2OrEngineer2And6Uranium:
			some = (engineers.height()+prepicked>=2) && nUranium>=6;
			break;
		case Scientist2OrEngineer2And5Uranium:
			some = (engineers.height()+prepicked>=2) && nUranium>=5;
			break;
			
		case ScientistOrEngineerAnd4Plutonium:
			some = nPlutonium>=4;
			break;
			
		case ScientistOrEngineer2And4Plutonium:
			some = (engineers.height()+prepicked>=2) && nPlutonium>=4;
			break;
		case Scientist2OrEngineer2And6Plutonium:
			some = (engineers.height()+prepicked>=2) && nPlutonium>=6;
			break;
			
		case ScientistOrEngineer2And5Plutonium:
		case Scientist2OrEngineer2And5Plutonium:
			some = (engineers.height()+prepicked>=2) && nPlutonium>=5;
			break;
			
		
		case Scientist2OrEngineer3And6Plutonium:
			some = (engineers.height()+prepicked>=3) && nPlutonium>=6;
			break;
			
		case Scientist2OrEngineer3And7Plutonium:
			some = (engineers.height()+prepicked>=3) && nPlutonium>=7;
			break;
			
		case Scientist2OrEngineer4And7Plutonium:
			some = (engineers.height()+prepicked>=4) && nPlutonium>=7;
			break;
				
		case  Any2WorkersAndRetrieve:	// germany
			some = ((nAvailableWorkers()+actualPicked>=2) && hasRetrieveSorEMoves());
			break;
		
		// bomb cost
		case ScientistAnd3Uranium: 
			break;
		case ScientistAndEngineerAnd3Uranium:
			some = nUranium>=3 && nScientistsAvailable()>=1;
			break;
		case ScientistAndEngineerAnd4Uranium:
			some = nUranium>=4 && nScientistsAvailable()>=1;
			break;
			
		case ScientistAndEngineer2And4Uranium:
			some = nUranium>=4 && nScientistsAvailable()>=1 && engineers.height()+prepicked>=2;
			break;
			
		case ScientistAndEngineer2And5Uranium:
			some = nUranium>=5 && nScientistsAvailable()>=1 && engineers.height()+prepicked>=2;
			break;

		case Scientist2AndEngineer2And5Uranium:
			some = nUranium>=5 && nScientistsAvailable()>=2 && engineers.height()+prepicked>=2;
			break;
			
		case Scientist2AndEngineer2And6Uranium:
			some = nUranium>=6 && nScientistsAvailable()>=2 && engineers.height()+prepicked>=2;
			break;
			
		case ScientistAndEngineerAnd4Plutonium:
			some = nPlutonium>=4 && nScientistsAvailable()>=1;
			break;
			
		case ScientistAndEngineer2And4Plutonium:
			some = nPlutonium>=4 && nScientistsAvailable()>=1 && engineers.height()+prepicked>=2;
			break;
			
		case Scientist2AndEngineer2And5Plutonium:
			some = nPlutonium>=5 && nScientistsAvailable()>=2 && engineers.height()+prepicked>=2;
			break;
		
		case Scientist2AndEngineer2And6Plutonium:
			some = nPlutonium>=6 && nScientistsAvailable()>=2 && engineers.height()+prepicked>=2;
			break;
			
		case ScientistAndEngineer2And5Plutonium:
			some = nPlutonium>=5 && nScientistsAvailable()>=1 && engineers.height()+prepicked>=2;
			break;
						
			
		case Scientist2AndEngineer3And6Plutonium:
			some = nPlutonium>=6 && nScientistsAvailable()>=2 && engineers.height()+prepicked>=3;
			break;
			
		case Scientist2AndEngineer3And7Plutonium:
			some = nPlutonium>=7 && nScientistsAvailable()>=2 && engineers.height()+prepicked>=3;
			break;
			
		case Scientist2AndEngineer4And7Plutonium:
			some = nPlutonium>=7 && nScientistsAvailable()>=2 && engineers.height()+prepicked>=4;
			break;

		}
		if(some)
		{
			dropEngineer(all,c,who,op,source);
		}
		return some;
	}
	
	// a scientist is picked or known to be available
	private boolean scientistSatisfies(CommonMoveStack all,ManhattanCell c,Cost requirements, int who,int op,ManhattanCell source)
	{	boolean some = false;
		int prepicked = (op==MOVE_FROM_TO) ? 0 : 1;
		int actualPicked = prepicked;
		if(hasPersonality(ManhattanChip.Oppenheimer) 
				&& source==scientists		// using a scientits as 2
				&& !testOption(TurnOption.OppenheimerWorker))	
			{ // pretend we picked 2 workers up
			prepicked++; 
			}

		switch(requirements)
		{
		case None:
		default: throw G.Error("Not expecting cost %s",requirements);
		
		case ScientistAndBombDesign:
			some= (b.nBombsAvailable()>0 || b.seeCurrentDesigns.height()>0);
			break;
			
		case Airstrike:
			// at present, there's no requirement you actually have any planes
			// you can take airstrike action as a defensive measure.
			some = true; // nFighters>0 || nBombers>0;
			break;
		case AnyWorkerAndBomb:
			some = nDesignsAvailable()>0;
			break;
		
		case Cash:	// purchase a building by a scientist
			some = c.cash<=cashDisplay.cash;
			break;
		case AnyWorker:
			some = true;	
			break;
			
		case AnyWorkerAnd3:
			some = cashDisplay.cash>=3;
			break;
			
		case AnyWorkerAnd5:
			some = cashDisplay.cash>=5;
			break;
			
		case AnyWorkerAnd3Y:
			some = yellowcakeDisplay.height()>=3;
			break;

		case Scientist:
			some = true;
			break;
			
		case ScientistAnd2Y:
			some = yellowcakeDisplay.height()>=2;
			break;
			
		case ScientistAnd2YAnd3:
			some = cashDisplay.cash>=3 && yellowcakeDisplay.height()>=2;
			break;

		case Engineer:
		case EngineerAndMoney:	// no, we're playing a scientist
			break;
			
		case ScientistOrWorkerAndMoney:	// buying a building
			some = b.seeBuilding[0].height()>0 && cashDisplay.cash>=b.seeBuilding[0].cash;
			break;
		case ScientistOrEngineer:
			some = true;
			break;
			
		case  Any2WorkersAndRetrieve:	// germany
			some = ((nAvailableWorkers()+actualPicked>=2) && hasRetrieveSorEMoves());
			break;
			
		case ScientistAndEngineerAndBombDesign:	
			some = nEngineersAvailable()>=1 && b.seeCurrentDesigns.height()>0;
			break;
		
			// building cost
		case Any2WorkersAndCash:
			if(cashDisplay.cash==0) { break; }	// must have cash
			//$FALL-THROUGH$
		case Any2Workers:
			some = nAvailableWorkers()+actualPicked>=2;	// oppenheimer not applicable
			break;
		case Engineer2:
		case Engineer3:
			break;
		case Any3Workers:
			some = nAvailableWorkers()+actualPicked>=3;	// oppenheimer not applicable
			break;
		case Scientist2And5YellowcakeAnd2:
			some = yellowcakeDisplay.height()>=5 && cashDisplay.cash>=2 && scientists.height()+prepicked>=2;
			break;
		case Scientists2And6YellowcakeAnd7:
			some = yellowcakeDisplay.height()>=6 && cashDisplay.cash>=7 && scientists.height()+prepicked>=2;
			break;
		case ScientistAnd3YellowcakeAnd1:
			some = yellowcakeDisplay.height()>=3 && cashDisplay.cash>=1;
			break;
			
		case ScientistAnd2YellowcakeAnd2:
			some = yellowcakeDisplay.height()>=2 && cashDisplay.cash>=2;
			break;
			
		case ScientistAnd1YellowcakeAnd3:
			some = yellowcakeDisplay.height()>=1 && cashDisplay.cash>=3;
			break;
		case Scientist2And4YellowcakeAnd3:
			some = yellowcakeDisplay.height()>=4 && cashDisplay.cash>=3 && scientists.height()+prepicked>=2;
			break;
			
		case ScientistAnd4YellowcakeAnd4:
			some = yellowcakeDisplay.height()>=4 && cashDisplay.cash>=4;
			break;
			
		case Scientists2And3YellowcakeAnd4:
			some = yellowcakeDisplay.height()>=3 && cashDisplay.cash>=4 && scientists.height()+prepicked>=2;
			break;
			
		case ScientistAnd3YellowcakeAnd5:
			some = yellowcakeDisplay.height()>=3 && cashDisplay.cash>=5;
			break;
			
		case Scientist2And2YellowcakeAnd5:
			some = yellowcakeDisplay.height()>=2 && cashDisplay.cash>=5 && scientists.height()+prepicked>=2;
			break;
			
		case ScientistAnd1Yellowcake:
			some = yellowcakeDisplay.height()>=1;
			break;
			
		case ScientistAnd1UraniumOr2Yellowcake:
			// this will sometimes need to invoke a choice
			some = nUranium>=1 || yellowcakeDisplay.height()>=2;
			break;
			
		case ScientistAnd1Uranium:
			some = nUranium>=1;
			break;
			
		case Scientist2And3Yellowcake:
			some = yellowcakeDisplay.height()>=3 && scientists.height()+prepicked>=2;
			break;
			
		case Scientists2And1UraniumOr4Yellowcake:
			some = (nUranium>=1 || yellowcakeDisplay.height()>=4) && (scientists.height()+prepicked>=2);
			break;
		
		case ScientistAnd5Yellowcake:
			some = yellowcakeDisplay.height()>=5;
			break;
			
		case Scientist2And6Yellowcake:
			some = (yellowcakeDisplay.height()>=6) && (scientists.height()+prepicked>=2);
			break;
			
		case Scientists2And1UraniumOr7Yellowcake:
			some = (nUranium>=1 || yellowcakeDisplay.height()>=7) && (scientists.height()+prepicked>=2);
			break;
			
		case Scientists3And8Yellowcake:
			some = (yellowcakeDisplay.height()>=8) && (scientists.height()+prepicked>=3);
			break;
		case Scientist2And1UraniumOr3Yellowcake:
			some = (nUranium>=1 || yellowcakeDisplay.height()>=3) && (scientists.height()+prepicked>=2);			
			break;

			// bomb cost
		case ScientistAnd3Uranium:
			some = nUranium>=3;
			break;
		case ScientistAndEngineerAnd3Uranium:
			some = nUranium>=3 && nEngineersAvailable()>=1;
			break;
		case ScientistAndEngineerAnd4Uranium:
			some = nUranium>=4 && nEngineersAvailable()>=1;
			break;
		case ScientistAndEngineer2And4Uranium:
			some = nUranium>=4 && nEngineersAvailable()>=2;
			break;
						
		case ScientistAndEngineer2And5Uranium:
			some = nUranium>=5 && nEngineersAvailable()>=2;
			break;
			
		case Scientist2AndEngineer2And5Uranium:
			some = nUranium>=5 && nEngineersAvailable()>=2 && scientists.height()+prepicked>=2;
			break;
			
		case Scientist2AndEngineer2And6Uranium:
			some = nUranium>=6 && nEngineersAvailable()>=2 && scientists.height()+prepicked>=2;
			break;
			
		case ScientistAndEngineerAnd4Plutonium:
			some = nPlutonium>=4 && nEngineersAvailable()>=1;
			break;
			
		case ScientistAndEngineer2And4Plutonium:
			some = nPlutonium>=4 && nEngineersAvailable()>=2;
			break;
			
		case Scientist2AndEngineer2And5Plutonium:
			some = nPlutonium>=5 && nEngineersAvailable()>=2 && scientists.height()+prepicked>=2;
			break;
			
		case Scientist2AndEngineer2And6Plutonium:
			some = nPlutonium>=6 && nEngineersAvailable()>=2 && scientists.height()+prepicked>=2;
			break;
			
		case ScientistAndEngineer2And5Plutonium:
			some = nPlutonium>=5 && nEngineersAvailable()>=2;
			break;

		
		case Scientist2AndEngineer3And6Plutonium:
			some = nPlutonium>=6 && nEngineersAvailable()>=3 && scientists.height()+prepicked>=2;
			break;
			
		case Scientist2AndEngineer3And7Plutonium:
			some = nPlutonium>=7 && nEngineersAvailable()>=3 && scientists.height()+prepicked>=2;
			break;
		case Scientist2AndEngineer4And7Plutonium:
			some = nPlutonium>=7 && nEngineersAvailable()>=4 && scientists.height()+prepicked>=2;
			break;
			
		case Uranium3:
			// special case, this is a bomb which needs 1 engineer, but modified by the
			// israel card to not require any.  So you can't build it with a scientist
			// in hand.  It has to be selected as a special case.
			break;
			// israeli bomb costs
		case ScientistOrEngineerAnd3Uranium:
			some = nUranium>=3;
			break;
			
		case ScientistOrEngineer2And4Uranium:
		case ScientistOrEngineerAnd4Uranium:
			some = nUranium>=4;	// no engineers needed
			break;
		case ScientistOrEngineer2And5Uranium:
			some = nUranium>=5;
			break;
		case Scientist2OrEngineer2And5Uranium:
			some = (scientists.height()+prepicked>=2) && nUranium>=5;
			break;
		case Scientist2OrEngineer2And6Uranium:
			some = (scientists.height()+prepicked>=2) && nUranium>=6;
			break;
		case Scientist2OrEngineer2And5Plutonium:
			some = (scientists.height()+prepicked>=2) && nPlutonium>=5;
			break;
		case ScientistOrEngineer2And4Plutonium:
			some = nPlutonium>=4;
			break;
		case ScientistOrEngineerAnd4Plutonium:
			some = nPlutonium>=4;
			break;
			
		case ScientistOrEngineer2And5Plutonium:	
			some = nPlutonium>=5;
			break;
		
		
		case Scientist2OrEngineer2And6Plutonium:
		case Scientist2OrEngineer3And6Plutonium:
			some = (scientists.height()+prepicked>=2) && nPlutonium>=6;
			break;
			
		case Scientist2OrEngineer4And7Plutonium:
		case Scientist2OrEngineer3And7Plutonium:
			some = (scientists.height()+prepicked>=2) && nPlutonium>=7;
			break;
		}
		if(some)
		{
			dropScientist(all,c,who,op,source);
		}
		return some;
	}

	// nothing is picked yet. This is used to generate moves only for the initial
	// placement.  It looks for an available complement of workers plus whatever
	// other resources are needed.  After the initial placement, the state machine
	// counts down the required workers and when all are present, it collects the
	// ancillary resources.  Sometimes this involves a supplementary dialog.
	//
	private boolean satisfies(CommonMoveStack all,boolean allowPersonalities,ManhattanCell c,Cost requirements,int who)
	{	
		boolean some = false;
		if(workers.height()>0) 
			{ some |= workerSatisfies(all,c,requirements,who,MOVE_FROM_TO); 
			  //
			  // if we placed an ordinary worker, robots don't need to try scientists or engineers as well.
			  //
			  if(some && (b.robot!=null || all==null)) { return some; }
			  if(allowPersonalities)
			  {
			  if(hasPersonality(ManhattanChip.Groves) 
					  && !testOption(TurnOption.GrovesWorker))
			  {	// use a regular worker as an engineer
				  some |= engineerSatisfies(all,c,requirements,who,MOVE_FROM_TO,workers); 
			  }
			  if(hasPersonality(ManhattanChip.Oppenheimer) 
					&& !testOption(TurnOption.OppenheimerWorker))
				  {	// use a regular worker as an engineer
					  some |= scientistSatisfies(all,c,requirements,who,MOVE_FROM_TO,workers); 
				  }}
			}
		// if we placed an ordinary worker where 
		if(some && all==null) { return some; }
		if(engineers.height()>0) 
			{ some |= engineerSatisfies(all,c,requirements,who,MOVE_FROM_TO,engineers); 
			  // if a robot placed an engineer, we don't need to also try scientists
			  if(some && b.robot!=null) { return some; }
			}
		if(some && all==null) { return some; }
		if(scientists.height()>0)
			{ some |= scientistSatisfies(all,c,requirements,who,MOVE_FROM_TO,scientists); }
		return some;
	}
	
	// add available moves to "all", or if all is null, just return true if some could have been added
	// "worker" is any worker currently picked up in the UI
	private boolean satisfies(CommonMoveStack all,boolean allowPersonalities,ManhattanChip worker,ManhattanCell c,Cost requirements,int op,int who)
	{	if(c.available())
		{if(worker==null)
			{
		 	return satisfies(all,allowPersonalities,c,requirements,who);
			}
		if(worker.type==Type.Worker)
			{
			// usually it's a worker, but can also be a plane or a bomb test
			switch(worker.workerType)
			{
			default: throw G.Error("Not expecting %s",worker);
			
			case S:
				return scientistSatisfies(all,c,requirements,who,op,scientists);
				
			case E:
				return engineerSatisfies(all,c,requirements,who,op,engineers);
				
			case L:
				boolean some = workerSatisfies(all,c,requirements,who,op);
				if(all==null && some) { return some; }
				// 
				// this is when a laborer is being used on a scientist or engineer slot
				// enabled by groves or oppenheimer.  The nasty case is when playing 
				// directly on the building market, which could "imply" the use of groves
				// but we don't allow.
				//
				if(allowPersonalities)
				{
				if(hasPersonality(ManhattanChip.Groves)
						&& !testOption(TurnOption.GrovesWorker))
				{
					some |= engineerSatisfies(all,c,requirements,who,op,workers);
				}
				if(all==null && some) { return some; }
				if(hasPersonality(ManhattanChip.Oppenheimer)
						&& !testOption(TurnOption.OppenheimerWorker))
				{
					some |= scientistSatisfies(all,c,requirements,who,op,workers);
				}
				}
				return some;
			}}
		}
		return false;
	}
	// select any worker
	public boolean addSelectWorkerMoves(CommonMoveStack all,int op,int who)
	{	boolean some = false;
		if(workers.height()>0)
		{	if(all==null) { return true; }
			all.push(new ManhattanMovespec(op,workers,-1,who));
			some = true;
		}
		if(engineers.height()>0)
		{	if(all==null) { return true; }
			all.push(new ManhattanMovespec(op,engineers,-1,who));
			some = true;
		}
		if(scientists.height()>0)
		{	if(all==null) { return true; }
			all.push(new ManhattanMovespec(op,scientists,-1,who));
			some = true;
		}
		return some;
	}
	// select any worker
	public boolean addSelectWorkerMoves(CommonMoveStack all,int op,WorkerType picked,ManhattanCell dest,int who)
	{	boolean some = false;
		if((workers.height()>0)||(picked==WorkerType.L))
		{	if(all==null) { return true; }
			all.push(new ManhattanMovespec(op,workers,-1,dest,who));
			some = true;
			if(some && b.robot!=null) { return some; }	// use workers if possible
		}
		if((engineers.height()>0)||(picked==WorkerType.E))
		{	if(all==null) { return true; }
			all.push(new ManhattanMovespec(op,engineers,-1,dest,who));
			some = true;
		}
		if((scientists.height()>0)||(picked==WorkerType.S))
		{	if(all==null) { return true; }
			all.push(new ManhattanMovespec(op,scientists,-1,dest,who));
			some = true;
		}
		return some;
	}	
	public boolean addSelectWorkerMoves(CommonMoveStack all,int op,ManhattanCell src,ManhattanCell dest,int who)
	{
		if(all==null) { return true; }
		all.push(new ManhattanMovespec(op,src,-1,dest,who)); 
		return true;
	}
	
	public boolean addEngineerMoves(CommonMoveStack all,ManhattanChip picked,ManhattanCell dest,int who)
	{

		 boolean some = false;
		 if(picked!=null || engineers.height()>0)
		 {
			 some |= addSelectWorkerMoves(all,picked==null ? MOVE_FROM_TO : MOVE_DROP,engineers,dest,who);
		 }
		 if(all==null && some) { return some; }
		 if((picked==null)
				 && hasPersonality(ManhattanChip.Groves) 
				 && !testOption(TurnOption.GrovesWorker)
				 && workers.height()>0)
		 { some |= addSelectWorkerMoves(all,MOVE_FROM_TO,workers,dest,who);
		 }
		 return some;
	}
	public boolean addScientistMoves(CommonMoveStack all,ManhattanChip picked,ManhattanCell dest,int who)
	{

		 boolean some = false;
		 if(picked!=null || scientists.height()>0)
			 {	some |= addSelectWorkerMoves(all,picked==null ? MOVE_FROM_TO : MOVE_DROP,scientists,dest,who);
			 }
		 if(all==null && some) { return some; }
		 if((picked==null)
				 && hasPersonality(ManhattanChip.Oppenheimer) 
				 && !testOption(TurnOption.OppenheimerWorker)
				 && workers.height()>0)
		 { some |= addSelectWorkerMoves(all,MOVE_FROM_TO,workers,dest,who);
		 }
		 return some;
	}
	
	public boolean addWorkerMoves(CommonMoveStack all,boolean allowPersonalities,ManhattanChip picked,ManhattanCell c,int op,int who)
	{
		return satisfies(all,allowPersonalities,picked,c,c.getEffectiveCost(),op,who);
	}
	public boolean addDiscardBombMoves(CommonMoveStack all,boolean robot,int who)
	{
	 boolean some = false;
	 for(int lim=stockpile.size()-1; lim>=0 && (all!=null || !some); lim--)
	 {	ManhattanCell c = stockpile.elementAt(lim);
	 	if((c.height()==1) 
 				// for the robot, only turn things on, never off
	 			&& (!robot || !b.selectedCells.contains(c))
	 			&& c.chipAtIndex(0).type==Type.Bomb)
	 	{
		 if(all!=null) { all.push(new ManhattanMovespec(MOVE_SELECT,c,0,who));}
		 some = true;
	 	}
	 }
	 return some;
	}
	public boolean addRetrieveSorEMoves(CommonMoveStack all,MColor targetColor,int who)
	{	boolean some = addAddRetrieveSorEMoves(all,buildings,targetColor,who);
		some |= addAddRetrieveSorEMoves(all,stockpile,targetColor,who);
		return some;
	}
	 public boolean addAddRetrieveSorEMoves(CommonMoveStack all,CellStack from,MColor targetColor,int who)
	 {	boolean some = false;
		for(int lim=from.size()-1; lim>=0  && (all!=null || !some); lim--)
				{ManhattanCell c = from.elementAt(lim);
				 if(c.height()>0)
				 {
					 some |= addRetrieveSorEMoves(all,from.elementAt(lim),targetColor,who);
				 }
			 }
		 return some;
	 }
	 public boolean addRetrieveSorEMoves(CommonMoveStack all,ManhattanCell from,MColor targetColor,int who)
	 {	boolean some = false;
	 	for(int lim=from.height()-1; lim>=0; lim--)
	 	{ManhattanChip ch=from.chipAtIndex(lim);
	 	 if(ch.type==Type.Worker
	 			 && (ch.color==targetColor)
	 			 && ((ch.workerType==WorkerType.S) || ch.workerType==WorkerType.E))
	 	 {
	 	 some = true;
	 	 if(all==null) { return some; }
		 all.push(new ManhattanMovespec(MOVE_SELECT,from,lim,who)); 
	 	 }
	 	}
	 	return some;
	 }
	 
	public boolean hasRepairMoves()
	{
		return addRepairMoves(null,5,boardIndex);
	}
	public boolean addRepairMoves(CommonMoveStack all,int cost,int who)
	{
		 boolean some = false;
		 if(cashDisplay.cash>=cost)
		 {
			 for(int lim=buildings.size()-1; lim>=0  && (all!=null || !some); lim--)
				{ManhattanCell c = buildings.elementAt(lim);
				 if(!c.inhibited)
				 {
				 ManhattanChip ch = c.topChip();
				 if(ch!=null && ch.type==Type.Damage)
				 {
					 if(all!=null) { all.push(new ManhattanMovespec(MOVE_REPAIR,c,-1,who)); }
					 some = true;
				 }}
			 }
		 }
		 return some;
	}
	
	public boolean addNorthKoreaMoves(CommonMoveStack all,int who)
	{	if(all==null) { return true; }
		// note that contrary to the usual practice, this move generator is used
		// only for the robot.  the robot sets contribution then approves
		if(!hasSetContribution 
				&& (b.northKoreaSteps<2*b.players_in_game)
				&& (who!=b.northKoreanPlayer) && cashDisplay.cash>0)
		{
		int sofar = 0;
		for(PlayerBoard pb : b.pbs) { 
			if(pb!=this)
			{
				sofar += pb.koreanContribution;
			}
		}
		// don't overpledge if some have already weighed in
		for(int i=0,lim=Math.max(0,Math.min(3-sofar,cashDisplay.cash)); i<=lim; i++)
				{
				all.push(new ManhattanMovespec(b.robot==null ? EPHEMERAL_CONTRIBUTE : MOVE_CONTRIBUTE,color,i,who));
				}}
		else 
		{
		all.push(new ManhattanMovespec(b.robot==null ? EPHEMERAL_APPROVE : MOVE_APPROVE,color,who));
		}
		return true;
	}
	public boolean addPlayerBoardMoves(CommonMoveStack all, ManhattanChip pickedObject, int who)
	{	
		boolean some = addPlayerBoardMoves(all,pickedObject,buildings,who);
		if(some && all==null) { return some; }
		some |= addPlayerBoardMoves(all,pickedObject,stockpile,who);
		if(hasPersonality(ManhattanChip.Lemay) && !testOption(TurnOption.LemayAirstrike))
		{
			some |= b.addAirstrikeMoves(all,true,who);
		}
		if(hasPersonality(ManhattanChip.Szilard))
		{	// can use the main board reactors
			some |= addWorkerMoves(all,true,b.pickedObject,b.playMakePlutonium,MOVE_DROP,who);
			some |= addWorkerMoves(all,true,b.pickedObject,b.playMakeUranium,MOVE_DROP,who);
		}
		return some;
	}
	

	public boolean hasBuildBombMoves()
	{
		boolean some = addIsraelBombMoves(null,null,boardIndex);
		return some;
	}
	
	public boolean addIsraelBombMoves(CommonMoveStack all,ManhattanChip pickedObject,int who)
	{	boolean some = false;
		for(int lim=stockpile.size()-1; lim>=0 && (all!=null || !some); lim--)
		{
		ManhattanCell c = stockpile.elementAt(lim);
		if(c.height()==1) {
			some |=addIsraelBombMoves(all,pickedObject,c,who);
		}
		}
		return some;
	}
	// playermoves on some list of buildings, possibly not our own
	public boolean addPlayerBoardMoves(CommonMoveStack all, ManhattanChip pickedObject, CellStack from,int who)
	{	boolean some = false;
		for(int i=0,size=from.size();i<size;i++)
		{
			ManhattanCell c = from.elementAt(i);
			if(!c.inhibited)
			{
			some |= addPlayerBoardMoves(all,pickedObject,c,who);
			if(some && all==null) { return some; }
			}
		}
		return some;
	}
	// has other player has mines, damaged or occupied ok (for australia)
	public boolean hasOtherMines()
	{
		for(PlayerBoard op : b.pbs)
		{	if(op!=this)
			 { if(hasMines(op.buildings)) { return true; }
			 }
		}
		return false;
	}
	
	public boolean hasPlacedWorkers()
	{
		for(int lim=buildings.size()-1; lim>=0; lim--)
		{
			ManhattanCell c = buildings.elementAt(lim);
			if(c.containsChip(worker)) { return true; }
		}
		return false;
	}
	
	// has mines, damaged or occupied ok (for australia)
	public boolean hasMines(CellStack buildings)
	{
		for(int lim=buildings.size()-1; lim>=0; lim--)
		{
			ManhattanCell c = buildings.elementAt(lim);
			if((c.height()>0) && c.chipAtIndex(0).isAMine()) 
				{ return true; }
		}
		return false;
	}
	
	public boolean hasOpenBuildings()
	{	return addPlayerBoardMoves(null,null,buildings,boardIndex);
	}
	// true if there's a building we cound espionage (for russia)
	public boolean hasOtherOpenBuildings()
	{	for(PlayerBoard op : b.pbs)
		{	if(op!=this)
			 { boolean some = addPlayerBoardMoves(null,null,op.buildings,boardIndex);
			   if(some) { return true; }
			 }
		}
		return false;
	}

	public boolean addPlayerBoardMoves(CommonMoveStack all, ManhattanChip pickedObject, ManhattanCell c,int who)
	{	boolean some = false;
		if(c.height()>0)
		{	
			if((c.color==color) || (c.chipAtIndex(0).type!=Type.Nations))
			{
				// can't spy on other players nations card
				if(c.type==Type.Bomb)
				{
					some = addBombMoves(all,pickedObject,c,who);
					if(all==null && some) { return some; }
				}
				some |=satisfies(all,true,pickedObject,c,c.getEffectiveCost(),MOVE_DROP,who);
			}
		}
		return some;
	}
	public boolean addIsraelBombMoves(CommonMoveStack all,ManhattanChip pickedObject,ManhattanCell c,int who)
	{	
		Cost is = c.chipAtIndex(0).getIsraeliCost();
		// special case for "little boy" which needs no workers in israel mode
		if((is==Cost.Uranium3)
				&& (nUranium>=3)
				&& (pickedObject==null) 
				&& c.height()==1)
		{	if(all!=null)
			{
			all.push(new ManhattanMovespec(MOVE_SELECT,c,-1,who));
			}
			return true;
		}
		else
		{
		return satisfies(all,true,pickedObject,c,is,MOVE_DROP,who);
		}
	}
	
	private boolean isLoaded(ManhattanCell c)
	{
		for(int i=2,size=c.height();i<size;i++) { if (c.chipAtIndex(i)==bomber) { return true; }}
		return false;
	}
	
	public int calculateScore()
	{	int value = 0;
		for(int i=0,size=stockpile.size(); i<size;i++)
		{
			ManhattanCell bomb = stockpile.elementAt(i);
			value += bombValue(bomb);
		}
		return value;
	}
	/**
	 * bomb stacking protocol: 
	 * 	the 0 element is always the bomb card
	 *  the 1 element is always the "built" chip for built bombs.  It may be a worker if a build is in progress.
	 *  the higher elements may be workers or a bomber or a bombtest card.
	 *  the workers will disappear when workers are retrieved, leaving the built, bomber, and bombtest chips
	 * @param bomb
	 * @return
	 */
	public int bombValue(ManhattanCell bomb)
	{	int value = 0;
		int h = bomb.height();
		if(h>=2 && bomb.chipAtIndex(1)==ManhattanChip.built)
		{
			ManhattanChip bombCard = bomb.chipAtIndex(0);
			ManhattanChip testChip = null;
			ManhattanChip bomberChip = null;
			ManhattanChip dismantleChip = null;
			for(int j = 2;j<h; j++)
			{
				ManhattanChip ch = bomb.chipAtIndex(j);
				if(ch.type==Type.Bombtest) { testChip = ch; }
				else if(ch.type==Type.Bomber) { bomberChip = ch; }
				else if(ch.type==Type.Damage) { dismantleChip = ch; }
			}
			if(testChip!=null) { value = testChip.bombValue(); }	// and that's all
			else
			{ 	value += hasTestedBomb ? bombCard.bombTestedValue() : bombCard.bombValue();
				if(bomberChip!=null) { value += 5; }
				if(dismantleChip!=null) { value += 5; }
			}
		}
		return value;
	}
	// add moves which upgrade bombs
	private boolean addBombMoves(CommonMoveStack all,ManhattanChip pickedObject,ManhattanCell c,int who)
	{	boolean some = false;
		int size = c.height();
		if(size>=2)
		{	ManhattanChip bomb = c.chipAtIndex(0);
			G.Assert(c.chipAtIndex(1)==ManhattanChip.built,"should be a built bomb");
			if((!hasTestedBomb)
					&& bomb.isPlutoniumBomb() 
					&& (( pickedObject==null) || (pickedObject.type==Type.Bombtest)))
			{
				if(all!=null) { all.push(new ManhattanMovespec(MOVE_FROM_TO,b.seeBombtests,-1,c,who)); }
				some = true;
			}
			if(!isLoaded(c) 
					&& (nBombers>0)
					&& ((pickedObject==null) || (pickedObject.type==Type.Bomber))
					&& !c.containsChip(bomber)
					&& !c.contains(Type.Bombtest)
					&& !c.containsChip(ManhattanChip.Damage)
					&& (cashDisplay.cash>=bomb.loadingCost()))
			{
				if(all!=null) { all.push(new ManhattanMovespec(MOVE_FROM_TO,bombers[nBombers],0,c,who));}
				some = true;
			}
			
		}

		return some;
	}
	// add coins to the destination cell and animate their movement
	public void transferCoins(ManhattanCell from,ManhattanCell to,int n,replayMode replay)
	{
		int h = from.height();
		from.addCash(-n);
		to.addCash(n);
		if(replay.animate)
		{
			int newh = to.height();
			h = Math.min(h,newh-1);	// at least one phantom coin
			while(h++<newh)
			{
			b.animationStack.push(from);
			b.animationStack.push(to);
			}
		}
	}
	
	// add coins to the destination cell and animate their movement
	public void addCoinsFromBank(ManhattanCell dest,int n,replayMode replay)
	{
		int h = dest.height();
		dest.addCash(n);
		if(replay.animate)
		{
			int newh = dest.height();
			h = Math.min(h,newh-1);	// at least one phantom coin
			while(h++<newh)
			{
			b.animationStack.push(b.seeBank);
			b.animationStack.push(dest);
			}
		}
	}
	
	// add coins to the destination cell and animate their movement
	public void sendCoinsToBank(ManhattanCell src,int n,replayMode replay)
	{
		int h = src.height();
		G.Assert(src.cash>=n,"cash going negative!");
		src.addCash(-n);
		if(replay.animate)
		{
			int newh = src.height();
			h = Math.max(1,newh-h);	// at least one phantom coin
			while(h-->0)
			{
			b.animationStack.push(src);
			b.animationStack.push(b.seeBank);
			}
		}
	}

	
	public void payYellowcake(int n,replayMode replay)
	{
		for(int i=0;i<n;i++)
		{	
			yellowcakeDisplay.removeTop();
			if(replay.animate)
			{
				b.animationStack.push(yellowcakeDisplay);
				b.animationStack.push(b.seeYellowcake);
			}	
		}
	}
	private void buyWithEngineer(ManhattanCell cell,replayMode replay)
	{
		if(cell.row>=2)
		{	// freebie in first 2 slots
			int cost = b.selectedCells.top().cash;
			sendCoinsToBank(cashDisplay,cost,replay);
			b.logGameEvent("- "+cost+"$");
		}
	}

	private void buyWithScientist(ManhattanCell cell,replayMode replay)
	{	// pay full price
		int cost = b.selectedCells.top().cash;
		sendCoinsToBank(cashDisplay,cost,replay);
		b.logGameEvent("- "+cost+"$");
	}
	
	void payCost(ManhattanCell cell,replayMode replay)
	{
		ManhattanChip top = cell.topChip();
		switch(top.type)
		{
		case Fighter:
			addFighter(-cell.height(),replay);
			break;
		case Bomber:
			addBomber(-cell.height(),replay);
			break;
		case Coin:
			sendCoinsToBank(cashDisplay,cell.cash,replay);
			b.logGameEvent("- "+cell.cash+"$");
			break;
		case Yellowcake:				
			payYellowcake(cell.height(),replay);
			b.logGameEvent("- "+cell.height()+" yellowcake");
			break;
		case Uranium:
			addUranium(-cell.height(),replay);
			b.logGameEvent("- "+cell.height()+" uranium");
			break;
		default: throw G.Error("not expecting %s",top);
		}
	}
	
	// 1 worker has been placed on top
	public void payCost(ManhattanCell cell,Cost requirements,replayMode replay)
	{	b.pendingCost = requirements;
		b.pendingBenefit = Benefit.None;
		switch(requirements)
		{
		default: throw G.Error("Not expecting %s",requirements);
		
		case ScientistAnd3Uranium:
		case ScientistAndEngineerAnd3Uranium:
			addUranium(-3,replay);
			b.logGameEvent("-3 uranium");
			break;

		case ScientistAndEngineerAnd4Uranium:
		case ScientistAndEngineer2And4Uranium:
			addUranium(-4,replay);
			b.logGameEvent("-4 uranium");
			break;

		case ScientistAndEngineer2And5Uranium:
		case Scientist2AndEngineer2And5Uranium:
			addUranium(-5,replay);
			b.logGameEvent("-5 uranium");
			break;
		case Scientist2AndEngineer2And6Uranium:
			addUranium(-6,replay);
			b.logGameEvent("-6 uranium");
			break;
		case ScientistAndEngineerAnd4Plutonium:
		case ScientistAndEngineer2And4Plutonium:
			addPlutonium(-4,replay);
			b.logGameEvent("-4 plutonium");
			break;
		case Scientist2AndEngineer2And5Plutonium:
		case ScientistAndEngineer2And5Plutonium:
			addPlutonium(-5,replay);
			b.logGameEvent("-5 plutonium");
			break;
		case Scientist2AndEngineer2And6Plutonium:
		case Scientist2AndEngineer3And6Plutonium:
			addPlutonium(-6,replay);
			b.logGameEvent("-6 plutonium");
			break;
		case Scientist2AndEngineer3And7Plutonium:
		case Scientist2AndEngineer4And7Plutonium:
			addPlutonium(-7,replay);
			b.logGameEvent("-7 plutonium");
			break;
		case AnyWorker:
		case Scientist:
		case Engineer:
		case ScientistOrEngineer:
		case ScientistAndBombDesign:	// france
		case  Any2WorkersAndRetrieve:	// germany	
		case None: break;
		case Scientists2And1UraniumOr4Yellowcake:
		case Scientist2And1UraniumOr3Yellowcake:
		case Scientists2And1UraniumOr7Yellowcake:
			//$FALL-THROUGH$
		case ScientistAnd1UraniumOr2Yellowcake:
			{
				// the yellowcake or uranium ought to be selected
				ManhattanCell selected = b.selectedCells.top();
				G.Assert(selected!=null,"a payment should be selected");
				payCost(selected,replay);
				
		}
		break;
		case AnyWorkerAndBomb:
			{
			ManhattanCell discard = b.selectedCells.pop();
			G.Assert(discard!=null,"should be there");
			ManhattanChip ch = discard.chipAtIndex(0);
			discard.reInit();
			b.seeDiscardedBombs.addChip(ch);
			if(replay.animate)
			{
				b.animationStack.push(discard);
				b.animationStack.push(b.seeDiscardedBombs);
			}
			///b.pendingBenefit = Benefit.Nations_PAKISTAN;
			}
			break;
		case Any3Workers:
		case Any2Workers:
		case Any2WorkersAndCash:
			break;

		case Engineer3:
		case Engineer2:
			break;
			
		case ScientistAndEngineerAndBombDesign:
			break;
			
		case AnyWorkerAnd5:			
			sendCoinsToBank(cashDisplay,5,replay);
			b.logGameEvent("- 5$");
			break;
			
		case AnyWorkerAnd3:
			sendCoinsToBank(cashDisplay,3,replay);
			b.logGameEvent("- 1$ 2$");
			break;
		case ScientistAnd2YellowcakeAnd2:
			payYellowcake(2,replay);
			sendCoinsToBank(cashDisplay,2,replay);
			b.logGameEvent("-2$");
			break;
		case Scientist2And4YellowcakeAnd3:
			payYellowcake(4,replay);
			sendCoinsToBank(cashDisplay,3,replay);
			b.logGameEvent("-1$ 2$");
			break;
		case Scientists2And6YellowcakeAnd7:
			payYellowcake(6,replay);
			sendCoinsToBank(cashDisplay,7,replay);
			b.logGameEvent("-6 yellowcake 1$ 2$ 5$");
			break;
			
		case Scientists3And8Yellowcake:
			payYellowcake(8,replay);
			b.logGameEvent("-8 yellowcake");
			break;
			
		case Scientist2And6Yellowcake:
			payYellowcake(6,replay);
			b.logGameEvent("-6 yellowcake");
			break;
			
		case ScientistAnd1Uranium:
			addUranium(-1,replay);
			b.logGameEvent("-1 uranium");
			break;
		case ScientistAnd3YellowcakeAnd5:
			payYellowcake(3,replay);
			sendCoinsToBank(cashDisplay,5,replay);
			b.logGameEvent("-3 yellowcake -5$");
			break;
		case ScientistAnd4YellowcakeAnd4:
			payYellowcake(4,replay);
			sendCoinsToBank(cashDisplay,4,replay);
			b.logGameEvent("- 4$ -4 yellowcake ");
			break;
		case ScientistAnd1YellowcakeAnd3:
			payYellowcake(1,replay);
			sendCoinsToBank(cashDisplay,3,replay);
			b.logGameEvent("- 1$ 2$ -1 yellowcake ");
			break;
		case Scientist2And5YellowcakeAnd2:
			payYellowcake(5,replay);
			sendCoinsToBank(cashDisplay,2,replay);
			b.logGameEvent("- 2$ -5 yellowcake ");
			break;
		case Scientists2And3YellowcakeAnd4:
			payYellowcake(3,replay);
			sendCoinsToBank(cashDisplay,4,replay);
			b.logGameEvent("- 2$ 2$ -3 yellowcake ");
			break;
		case ScientistAnd3YellowcakeAnd1:
			payYellowcake(3,replay);
			sendCoinsToBank(cashDisplay,1,replay);
			b.logGameEvent("- 1$ -3 yellowcake ");
			break;
		case Scientist2And2YellowcakeAnd5:
			payYellowcake(2,replay);
			sendCoinsToBank(cashDisplay,5,replay);
			b.logGameEvent("- 5$ -2 yellowcake ");
			break;
		case ScientistAnd2Y:
			payYellowcake(2,replay);
			b.logGameEvent("-2 yellowcake ");
			break;
		case ScientistAnd5Yellowcake:
			payYellowcake(5,replay);
			b.logGameEvent("-5 yellowcake ");
			break;
		case ScientistAnd1Yellowcake:
			payYellowcake(1,replay);
			b.logGameEvent("-1 yellowcake ");
			break;
		case Scientist2And3Yellowcake:
			payYellowcake(3,replay);
			b.logGameEvent("-3 yellowcake ");
			break;
		case ScientistAnd2YAnd3:
			payYellowcake(2,replay);
			sendCoinsToBank(cashDisplay,3,replay);
			b.logGameEvent("- 1$ 2$ -2 yellowcake ");
			break;

		case AnyWorkerAnd3Y:
			payYellowcake(3,replay);
			b.logGameEvent("-3 yellowcake ");
			break;
			
		case Cash:
			// buy a building by dropping on the building directly
			ManhattanChip ch = cell.removeTop();
			ManhattanCell dest = null;
			b.selectedCells.push(cell);
			switch(ch.workerType)
			{
			case E:
				dest = b.playBuyBuilding;
				buyWithEngineer(cell,replay);
				break;
			case L:
			case S:
				dest = b.playBuyBuilding2;
				buyWithScientist(cell,replay);
				break;
			default: throw G.Error("Not expecting %s ",ch);
			}
			dest.addChip(ch);
			if(replay.animate) {
				b.animationStack.push(cell);
				b.animationStack.push(dest);
			}
			break;
		case EngineerAndMoney:
			buyWithEngineer(b.selectedCells.top(),replay);
			break;
		case ScientistOrWorkerAndMoney:
			buyWithScientist(b.selectedCells.top(),replay);
			break;
			

		}
	}
	
	/**
	 * collect workers or temporary workers from the general supply
	 * @param n	howmany
	 * @param t what type
	 * @param dest where to put them
	 * @param replay animation
	 */
	private void collectWorkers(int n,WorkerType t,ManhattanCell dest,replayMode replay)
	{	
		String ev = "train";
		for(int i=0;i<n;i++)
		{	ManhattanChip ch = b.getAvailableWorker(color,t);
			if(ch==null) { ch = b.getAvailableWorker(MColor.Gray,t);}
			if(ch!=null)
			{
				dest.addChip(ch);
				if(b.robot==null) { ev += " "+ch.color+"-"+t; }
				if(replay.animate) 
				{
					b.animationStack.push(b.availableWorkers);
					b.animationStack.push(dest);
				}
			}
		}
		b.logGameEvent(ev);

	}
	private void collectWorker(ManhattanChip worker,replayMode replay)
	{	WorkerType t = worker.workerType;
		switch(t)
		{
		case L:	
			collectWorkers(1,t,workers,replay);
			break;
		case S:
			collectWorkers(1,t,scientists,replay);
			break;
		case E:
			collectWorkers(1,t,engineers,replay);
			break;
		default: throw G.Error("not expecting %s",worker);
		}
	}
	private void collectYellowcake(int n,replayMode replay)
	{	ManhattanChip ch = b.seeYellowcake.topChip();
		for(int i=0;i<n;i++)
		{	yellowcakeDisplay.addChip(ch);
			if(replay.animate) 
				{
					b.animationStack.push(b.seeYellowcake);
					b.animationStack.push(yellowcakeDisplay);
				}
		}
	}
	public ManhattanCell findBuildingSlot()
	{	for(int i=0,size=buildings.size(); i<size; i++)
		{
			ManhattanCell c = buildings.elementAt(i);
			if(c.isEmpty()) { return c; }
		}
		return expandBuildings(buildings.size());
	}
	

	public void collectBuilding(ManhattanCell selected,replayMode replay)
	{
		G.Assert(selected!=null,"a building should be selected");
		// expand so there's an empty slot.  This serves as a visual reminder
		// that there's still room for more
		ManhattanCell d = findBuildingSlot();
		
		ManhattanChip ch = selected.removeTop();
		d.addChip(ch); 
		if(b.robot!=null) { b.robot.setInhibitions(d); }
		if(replay.animate)
			{
			b.animationStack.push(selected);
			b.animationStack.push(d);
			}

		switch(selected.row) {
		default: break;
		case 0:
			transferCoins(b.seeBribe,cashDisplay,b.seeBribe.cash,replay);
			break;
		case 4:
		case 5:
		case 6:
			// add a buck to the bribe pile
			addCoinsFromBank(b.seeBribe,1,replay);		
		}
		// finally, move the cards down
		b.shuffleBuildings(selected,replay);
	}
	private void chooseFoB(Benefit be,int n)
	{	b.prepareChoices(2);
		for(int i=0;i<n;i++) { b.choice[0].addChip(fighter);}
		for(int i=0;i<n;i++) { b.choice[1].addChip(bomber); }
		b.pendingBenefit = be;
		b.setState(ManhattanState.CollectBenefit);
	}
	public boolean hasBuiltBombs()
	{	
		for(int i=0,size=stockpile.size(); i<size;i++)
		{
			ManhattanCell c = stockpile.elementAt(i);
			if(c.height()>=2
				&& (c.chipAtIndex(1)==ManhattanChip.built)
				&& !c.contains(Type.Bombtest)
				&& !c.containsChip(ManhattanChip.Damage))
			{
				return true;
			}
		}
		return false;
	}
	private void loadDismantleChoices(Benefit be)
	{	b.prepareChoices(0);
		b.pendingBenefit = be;
		boolean some = false;
		for(int i=0,size=stockpile.size(); i<size;i++)
		{
			ManhattanCell c = stockpile.elementAt(i);
			if(c.height()>=2
				&& (c.chipAtIndex(1)==ManhattanChip.built)
				&& (cashDisplay.cash>=c.chipAtIndex(0).loadingCost())
				&& !c.contains(Type.Bombtest)
				&& !c.containsChip(ManhattanChip.Damage))
			{
				b.displayCells.push(c);
				some = true;
			}
		}
		if(some) { b.setState(ManhattanState.CollectBenefit); }
	}
	
	private void addTradeChoices(Benefit be)
	{	
		b.prepareChoices(0);
		b.pendingBenefit = be;
		boolean some = loadTradeCells(b.choice, nFighters>=1, nBombers>=1, cashDisplay.cash>=3, yellowcakeDisplay.height()>=2);
		if(some)
		{
		loadTradeCells(b.choiceOut,true,true,true,true);
		b.setState(ManhattanState.CollectBenefit); 
		}
	}
	private boolean loadTradeCells(ManhattanCell from[],boolean fighters, boolean bombers, boolean cash, boolean yellow)
	{	int idx = 0;
		if(fighters) 
			{	from[idx].reInit();
				from[idx].addChip(fighter);
				b.displayCells.push(from[idx]);
				idx++;
			}
		if(bombers) 
		{ 	from[idx].reInit();
			from[idx].addChip(bomber);
			b.displayCells.push(from[idx]);
			idx++;
		}
		if(cash)
		{
			from[idx].reInit();
			addCoinsFromBank(from[idx],3,replayMode.Replay);
			b.displayCells.push(from[idx]);
			idx++;
		}
		if(yellow)
		{
			from[idx].reInit();
			from[idx].addChip(ManhattanChip.Yellowcake);
			from[idx].addChip(ManhattanChip.Yellowcake);
			b.displayCells.push(from[idx]);
			idx++;
		}
		return idx>0;
	}
	public void chooseScientistOrEngineer(Benefit be,int ns,int ne)
	{	b.prepareChoices(2);
		{
		ManhattanChip sci1 = null;
		for(int i=0;i<ns;i++)
			{ 
			// we need extra logic becuase the request can be 2 scientists
			// and the pattern may be mixed colored and gray
    		ManhattanChip sci = (b.countAvailableWorkers(color,WorkerType.S)>i)
    							? scientist
    							: (b.countAvailableWorkers(MColor.Gray,WorkerType.S)
    									> ((sci1==ManhattanChip.GrayScientist) ? i : 0))
    							? ManhattanChip.GrayScientist
    								: ManhattanChip.xx;;
 
 			sci1 = sci;
 			if(i==0 || sci!=ManhattanChip.xx) 
			{
 			// show one chip if two are not available
			b.choice[0].addChip(sci); 
			}
			}}
		{
		ManhattanChip eng1 = null;
		for(int i=0;i<ne;i++) 
		{     		
		// we need extra logic becuase the request can be 2 scientists
		// and the pattern may be mixed colored and gray
		ManhattanChip eng = (b.countAvailableWorkers(color,WorkerType.E)>i)
							? engineer
							: (b.countAvailableWorkers(MColor.Gray,WorkerType.E)
								> (eng1==(ManhattanChip.GrayEngineer) ? i : 0))
								? ManhattanChip.GrayEngineer
								: ManhattanChip.xx;
		eng1 = eng;
		if(i==0 || eng!=ManhattanChip.xx) 
			{// show one chip if two are not available
			b.choice[1].addChip(eng); 
			}
		}}
		b.pendingBenefit = be;
		b.setState(ManhattanState.CollectBenefit);
		
	}
	private void choosePlanesOrRepair()	//for UK
	{
		b.prepareChoices(2);
		b.choice[0].addChip(fighter);
		b.choice[0].addChip(fighter);
		if(hasRepairMoves())
		{
		b.choice[1].addChip(ManhattanChip.Damage);
		b.setState(ManhattanState.CollectBenefit);
		}
		else
		{
			b.displayCells.pop();
			b.selectedCells.push(b.choice[0]);
			b.setState(ManhattanState.ConfirmBenefit);
		}
		b.pendingBenefit = Benefit.Nations_UK;
		
	}
	public int nUsableBuildings()
	{
		int n = 0;
		for(int lim = buildings.size()-1; lim>=0; lim--)
		{	ManhattanCell c = buildings.elementAt(lim);
			if(c.height()>0 && c.topChip()!=ManhattanChip.Damage) { n++; }
		}
		return n;
	}
	public boolean hasOtherUniversities()
	{	
		for(PlayerBoard pb : b.pbs)
		{
		if(pb!=this)
		{
		CellStack buildings = pb.buildings;
		for(int lim=buildings.size()-1; lim>=0; lim--)
		{
			ManhattanCell c = buildings.elementAt(lim);
			if(!c.inhibited && c.height()>0)
			{
				ManhattanChip building = c.chipAtIndex(0);
				if(building.isAUniversity()
					&& cashDisplay.cash>= building.nWorkersRequired()
					&& (c.topChip()!=ManhattanChip.Damage))
				{	
					return true;
				}
			}
		}}}
		return false;
	}
	private void chooseUniversity()	// for india
	{
		b.prepareChoices(0);	
		boolean some = false;
		for(PlayerBoard pb : b.pbs)
			{
			if(pb!=this)
			{
			CellStack buildings = pb.buildings;
			for(int lim=buildings.size()-1; lim>=0; lim--)
			{
				ManhattanCell c = buildings.elementAt(lim);
				if(!c.inhibited && c.height()>0)
				{
					ManhattanChip building = c.chipAtIndex(0);
					if(building.isAUniversity()
						&& cashDisplay.cash>= building.nWorkersRequired()
						&& (c.topChip()!=ManhattanChip.Damage))
					{	b.displayCells.push(c);
						some = true;
					}
				}
			}
			}}
		// if no universities are actually available, it's dumb but legal
		if(some) 
			{ b.pendingBenefit = Benefit.Nations_INDIA;
			  b.setState(ManhattanState.CollectBenefit); 
			}
		
	}
	
	// true if has any building in need of repair
	boolean repairNeeded()
	{
		for(int lim=buildings.size()-1; lim>=0; lim--)
		{	ManhattanCell c = buildings.elementAt(lim);
			ManhattanChip top = c.topChip();
			if(top!=null && top.type==Type.Damage) { return true; }
		}
		return false;
	}
	
	// true if we can retrieve a scientist or engineer (for germany)
	 public boolean hasRetrieveSorEMoves()
	 {
		 boolean some =  b.addRetrieveSorEMoves(null,boardIndex);
		 return some;
	 }
	//
	// collect benefits either automatic, or if a choice was involved
	// then set up a followup dialog to make the choice.
	//
	public void collectBenefit(ManhattanCell dest,Benefit benefit,ManhattanCell selected,replayMode replay)
	{
		b.pendingBenefit = benefit;
		switch(benefit)
		{
		default: 
			b.p1("benefit-"+benefit);
			throw G.Error("Not expecting collectBenefit %s",benefit);
		case None:
			break;
		
		case Repair:
			b.repairCounter = 3;
			b.pendingBenefit = benefit;
			b.setState(ManhattanState.Repair);
			for(PlayerBoard pb : b.pbs) {
				if(pb!=this)
				{
					if(pb.repairNeeded() && (pb.cashDisplay.cash>=b.repairCost()))
					{	// if needs it and can afford it
						pb.pendingChoices.push(Benefit.PaidRepair);
					}
				}
			}
			break;
		case PaidRepair:
			b.repairCounter = 3;
			b.setState(ManhattanState.PaidRepair);
			break;
		case Airstrike:
			b.pendingBenefit = benefit;
			b.setState(ManhattanState.Airstrike);
			break;
		case Espionage:
			addEspionage(1,replay);
			b.espionageSteps = nEspionage;
			b.setState(ManhattanState.PlayEspionage);
			break;
		case Plutonium4:
			addPlutonium(4,replay);
			b.logGameEvent("+4 plutonium");
			break;
		case Plutonium3:
			addPlutonium(3,replay);
			b.logGameEvent("+3 plutonium");
			break;
		case Uranium3:
			addUranium(3,replay);
			b.logGameEvent("+3 uranium");
			break;
		case Uranium2:
			addUranium(2,replay);
			b.logGameEvent("+2 uranium");
			break;
		case Plutonium2:
			addPlutonium(2,replay);
			b.logGameEvent("+2 plutonium");
			break;
		case MainUranium:
			if(hasPersonality(ManhattanChip.Szilard))
			{
				addUranium(2,replay);
				b.logGameEvent("+2 Uranium");
				break;
			}
			//$FALL-THROUGH$
		case Uranium:
			addUranium(1,replay);
			b.logGameEvent("+1 uranium");

			break;
		case MainPlutonium:
			if(hasPersonality(ManhattanChip.Szilard))
			{
				addPlutonium(2,replay);
				b.logGameEvent("+2 plutonium");
				break;
			}
			//$FALL-THROUGH$
		case Plutonium:
			addPlutonium(1,replay);
			b.logGameEvent("+1 plutonium");
			break;
						
		case BomberAnd2:
			addBomber(1,replay);
			addCoinsFromBank(cashDisplay,2,replay);
			b.logGameEvent("2 bomber-"+color+" + 2$");
			break;
		case Bomber3And3:
			addBomber(3,replay);
			addCoinsFromBank(cashDisplay,3,replay);
			b.logGameEvent("3 bomber-"+color+" + 2$");
			break;
		case FighterAnd2: 
			addFighter(1,replay);
			addCoinsFromBank(cashDisplay,2,replay);
			b.logGameEvent("2 fighter-"+color+" + 2$");
			break;
		case Fighter2AndBomber2:
			addFighter(2,replay);
			addBomber(2,replay);
			b.logGameEvent("2 fighter-"+color +" 2 bomber-"+color);
			break;
		case Fighter2: 
			addFighter(2,replay);
			b.logGameEvent("+2 fighter-"+color);
			break;
		case Bomber2:
			addBomber(2,replay);
			b.logGameEvent("+2 bomber-"+color);
			break;
		case Fighter3And3:
			addFighter(3,replay);
			addCoinsFromBank(cashDisplay,3,replay);
			b.logGameEvent("1 fighter-"+color+" + 1$ 2$");
			break;
		case Fighter2OrBomber2:
			// fighter or 2 in a new dialog
			chooseFoB(benefit,2);
			break;

		case Nations_USSR:
			b.espionageSteps = 1;
			b.setState(ManhattanState.PlayEspionage);
			break;
		case FighterOrBomber:
			chooseFoB(benefit,1);
			break;
		case Nations_SOUTH_AFRICA:
			loadDismantleChoices(Benefit.Dismantle);
			break;
			
		case Nations_GERMANY:
			b.pendingBenefit = benefit;
			b.setState(ManhattanState.RetrieveSorE);
			break;
		case Nations_INDIA:
			// use someone else's university, for a price
			chooseUniversity();
			break;
		case Nations_UK:
			choosePlanesOrRepair();
			break;
		case Nations_AUSTRALIA:
			// one yellowcake for each mine owned by anyone, even if damaged
			{
			int n = 0;
			for(PlayerBoard pb : b.pbs)
				{
				if(pb!=this)
				{
				CellStack buildings = pb.buildings;
				for(int lim=buildings.size()-1; lim>=0; lim--)
				{
					ManhattanCell c = buildings.elementAt(lim);
					if(c.height()>0)
					{
						ManhattanChip building = c.chipAtIndex(0);
						if(building.isAMine())
						{
							n++;
						}
					}
				}
				}}
			collectYellowcake(n,replay);
			b.logGameEvent("+ "+n+" yellowcake");
			}
			break;
		case Nations_CHINA:
			// retrieve your regular workers from buildings.  This excludes the china card itself,
			// buildings occupied by scientists or engineers or temporary workers.
			for(int lim=buildings.size()-1; lim>=0; lim--)
			{	ManhattanCell c= buildings.elementAt(lim);
				int h = c.height();
				if(c.height()>1)
				{
					ManhattanChip ch = c.chipAtIndex(0);
					if(ch.type==Type.Building)
					{
						for(int i=h-1;i>=1;i--)
						{
							ManhattanChip worker = c.chipAtIndex(i);
							if(worker.color==color)
							{
								switch(worker.workerType)
								{
								case L: 
									workers.addChip(c.removeChipAtIndex(i));
									// flag this cell so it can be used if partially full
									// current blief is this only matters for cells with requirements
									// any2workers or any3workers
									c.partiallyVacated = true;
									nPlacedWorkers--;
									if(replay.animate){
										{
										b.animationStack.push(c);
										b.animationStack.push(workers);
										}}
									break;
								case S:
								case E:	break;
								default: throw G.Error("Not expecting %s",ch);
								}
							}
						}
					}
				}
			}
			b.setState(ManhattanState.Confirm);
			break;
		case Nations_BRAZIL:
			addTradeChoices(Benefit.Trade);
			break;
			
		case Nations_JAPAN:
			b.prepareChoices(2);
			b.choice[0].addChip(fighter);
			b.choice[0].addChip(fighter);
			b.choice[1].addChip(ManhattanChip.JapanAirstrike);
			b.setState(ManhattanState.CollectBenefit);
			break;
		case Nations_PAKISTAN:
			b.prepareChoices(3);
			// $10 5 fighters or 6 yellowcake
			addCoinsFromBank(b.choice[0],10,replayMode.Replay);
			b.choice[2].addChip(ManhattanChip.Yellowcake);
			for(int i=0;i<5;i++) { b.choice[1].addChip(fighter); b.choice[2].addChip(ManhattanChip.Yellowcake); }
			b.setState(ManhattanState.CollectBenefit);
			break;
		case Nations_ISRAEL:
			b.setState(ManhattanState.BuildIsraelBomb);
			break;
		case Nations_FRANCE:
			b.loadBombDesigns(true);
			b.bombDesigner = b.whoseTurn;
			b.setState(ManhattanState.CollectBenefit);
			break;
		case Nations_NORTH_KOREA:
			b.northKoreanPlayer = b.whoseTurn;
			b.northKoreaSteps = 0;
			for(PlayerBoard pb : b.pbs) 
				{ pb.approvedNorthKorea = false;
				  pb.hasSetContribution = false;
				  pb.koreanContribution=0; 
				}
			b.setState(ManhattanState.North_Korea_Dialog);
			if(b.robot!=null)
				{ b.setNextPlayer(replayMode.Replay); 
				}
			break;
		case Nations_USA:
			// 2 bombers or 1 bomber and 3$
			b.prepareChoices(2);
			b.choice[0].addChip(bomber);
			b.choice[0].addChip(bomber);
			if(nBombers>0)
			{
			b.choice[1].insertChipAtIndex(0,ManhattanChip.BomberSale);
			b.setState(ManhattanState.CollectBenefit);
			}
			else
			{	b.selectedCells.push(b.choice[0]);
				b.displayCells.pop();
				b.setState(ManhattanState.ConfirmBenefit);
			}
			
			
			break;
		case Engineer2OrScientist:
			chooseScientistOrEngineer(benefit,1,2);
			break;
			
		case ScientistOrEngineer:
			chooseScientistOrEngineer(benefit,1,1);
			break;
			
		case Scientist2OrEngineer:
			chooseScientistOrEngineer(benefit,2,1);
			break;
		case FighterOr2:
			// fighter or 2 in a new dialog
			b.prepareChoices(2);
			b.choice[0].addChip(fighter);
			addCoinsFromBank(b.choice[1],2,replayMode.Replay);
			b.setState(ManhattanState.CollectBenefit);
			break;
		case BomberOr2:
			// bomber or $2
			b.prepareChoices(2);
			b.choice[0].addChip(bomber);
			addCoinsFromBank(b.choice[1],2,replayMode.Replay);
			b.setState(ManhattanState.CollectBenefit);
			break;
			
		case Five:
			addCoinsFromBank(cashDisplay,5,replay);
			b.logGameEvent("+ 5$");
			break;
			
		case ThreeAnd1:
			addCoinsFromBank(cashDisplay,3,replay);
			b.logGameEvent("+ 1$ 2$");
			addCoinsFromBank(b.seeBribe,1,replay);
			break;
			
		case FiveAnd1:
			// we get 5 and one to the bribe pile
			addCoinsFromBank(cashDisplay,5,replay);
			b.logGameEvent("+ 5$");
			addCoinsFromBank(b.seeBribe,1,replay);
			break;
		case FiveAnd2:
			// we get 5 and everyone else gets 2
			for(PlayerBoard pb : b.pbs)
				{ 
				int amount = pb==this ? 5 : 2;
				pb.addCoinsFromBank(pb.cashDisplay,amount,replay);
				b.logGameEvent(""+pb.color+"-chip"+ " + "+amount+"$");
				}
			addCoinsFromBank(b.seeBribe,1,replay);
			break;
		case Worker4:
			collectWorkers(4,WorkerType.L,workers,replay);
			break;
		case Worker3:	// recruiting
			collectWorkers(3,WorkerType.L,workers,replay);
			break;
		case Scientist3:
			collectWorkers(3,WorkerType.S,scientists,replay);
			break;
		case Scientist2:	// recruiting
			collectWorkers(2,WorkerType.S,scientists,replay);
			break;
		case Scientist:	// recruiting
			collectWorkers(1,WorkerType.S,scientists,replay);
			break;
		case Engineer3:
			collectWorkers(3,WorkerType.E,engineers,replay);
			break;
		case Engineer:	// recruiting
			collectWorkers(1,WorkerType.E,engineers,replay);
			break;
			
		case Yellowcake6:
			collectYellowcake(6,replay);
			b.logGameEvent("+ 6 yellowcake ");
			break;
		case Yellowcake4:
			collectYellowcake(4,replay);
			b.logGameEvent("+ 4 yellowcake ");
			break;
		case Yellowcake3:
			collectYellowcake(3,replay);
			b.logGameEvent("+ 3 yellowcake ");
			break;
		case Yellowcake2:
			collectYellowcake(2,replay);
			b.logGameEvent("+ 2 yellowcake ");
			break;
		case Yellowcake:
			collectYellowcake(1,replay);
			b.logGameEvent("+ 1 yellowcake ");
			break;
		case Yellowcake3And1:
			// 3 yellowcake for us, 1 for everyone else
			for(PlayerBoard pb : b.pbs)
			{
			if(pb==this) 
				{ collectYellowcake(3,replay); 
				  b.logGameEvent(""+color+"-chip +3 yellowcake");
				}
			else { pb.collectYellowcake(1,replay);
			 		b.logGameEvent(""+pb.color+"-chip +1 yellowcake");
			}
			}
			break;
			
		case SelectedBuilding:
			collectBuilding(selected,replay);
			break;
		case BombDesign:
			b.loadBombDesigns(false);
			b.bombDesigner = b.whoseTurn;
			b.setState(ManhattanState.CollectBenefit);
			break;
			// bomb yields
		case P09T09L1:
		case P10T10L1:	
		case P11T11L1:	
		case P12T12L1:
		case P13T13L1:
		case P14T14L2:
		case P15T15L2:
		case P16T16L2:
		case P18T18L2:
		case P20T20L3:
		case P22T22L3:
		case P24T24L3:
		case P26T26L3:
		case P28T28L4:	
		case P30T30L5:	
		case P08T13L2:	
		case P08T14L2:	
		case P09T15L2:
		case P11T19L3:	
		case P11T20L3:	
		case P09T16L2:
		case P10T17L2:
		case P10T18L3:
		case P12T22L4:
		case P12T24L4:
		case P13T26L5:
		case P14T28L6:
		case P16T30L7:
		case P18T32L8:
		case P20T34L9:
			dest.insertChipAtIndex(1,ManhattanChip.built);
			break;
			
		}
	}

	public void distributeBenefit(Benefit pendingBenefit, ManhattanCell selected,replayMode replay) 
	{	switch(pendingBenefit)
		{	
		case Nations_GERMANY:
			// get a worker
			{
			ManhattanChip worker = selected.chipAtIndex(selected.selectedIndex);
			G.Assert(worker.type==Type.Worker
					&& (worker.workerType==WorkerType.S || worker.workerType==WorkerType.E),
					"should be a scientist or engineer");
			selected.removeChip(worker);
			returnWorker(selected,worker,replay);
			break;
			}
		case Nations_INDIA:
			// selected is a university
			{	
			ManhattanChip c = selected.chipAtIndex(0);
			PlayerBoard pb = b.getPlayerBoard(selected.color);
			int price = c.nWorkersRequired();
			G.Assert(cashDisplay.cash>=price,"not enough cash");
			// transfer coins to the other player
			transferCoins(cashDisplay,pb.cashDisplay,price,replay);
			collectBenefit(null,c.benefit,selected,replay);
			}
			break;
		case Nations_NORTH_KOREA:
			{
			int total = 0;
			for(PlayerBoard pb : b.pbs)
				{
				total += pb.koreanContribution;
				}
			if(total==3)
				{	// take the money
				PlayerBoard me = b.pbs[b.whoseTurn];
				for(PlayerBoard pb : b.pbs) 
					{
					if(pb!=me) 
						{
						transferCoins(pb.cashDisplay,me.cashDisplay,pb.koreanContribution,replay);
						}
					}
				b.setState(ManhattanState.ConfirmWorker);
				}
				else { 
				b.prepareChoices(2);
				b.choice[0].addChip(ManhattanChip.Uranium);
				b.choice[1].addChip(ManhattanChip.Plutonium);
				b.pendingBenefit = Benefit.UraniumOrPlutonium;
				b.pendingCost = Cost.None;
				b.setState(ManhattanState.CollectBenefit);
				}
			}
			break;
		case Dismantle:
			sendCoinsToBank(cashDisplay,selected.chipAtIndex(0).loadingCost(),replay);
			selected.addChip(ManhattanChip.Damage);
			break;
		default:
		case Nations_UK:
			if(selected.height()>0)
			{
			ManhattanChip something = selected.topChip();
			switch(something.type) 
			{
			case Personalities:
				{
				if(personality.height()>0)
					{ b.availablePersonalities.addChip(personality.removeTop()); 
					  if(replay.animate) {
						  b.animationStack.push(personality);
						  b.animationStack.push(b.seePersonalities);
					  }
					}
				personality.addChip(something);
				if(hasPersonality(ManhattanChip.Lemay))
				{	// set these now to prevent giving out new aircraft immediately
					setOption(TurnOption.LemayFighter);
					setOption(TurnOption.LemayBomber);
				}
				if(replay.animate)
					{
					b.animationStack.push(b.seePersonalities);
					b.animationStack.push(personality);
					}
				}
				b.availablePersonalities.removeChip(something);
				b.setState(ManhattanState.ConfirmPersonality);
				break;
			case BomberSale:
				addBomber(-1,replay);
				addCoinsFromBank(cashDisplay,3,replay);
				b.logGameEvent("+ 1$ 3$");
				break;
			case JapanAirstrike:
				b.setState(ManhattanState.JapanAirstrike);
				break;
			case Yellowcake:
				collectYellowcake(selected.height(),replay);
				b.logGameEvent(""+selected.height()+" yellowcake");
				break;
			case Worker:
				while(selected.height()>0) { collectWorker(selected.removeTop(),replay); }
				break;
			case Fighter:	
				addFighter(selected.height(),replay);
				b.logGameEvent("+ fighter");
				break;
			case Bomber: 
				addBomber(selected.height(),replay);	
				b.logGameEvent("+ bomber");
				break;
			case Coin: 
				addCoinsFromBank(cashDisplay,selected.cash,replay);
				b.logGameEvent("+ "+selected.cash+"$");
				ManhattanChip ch = selected.chipAtIndex(0);
				switch(ch.type)
				{
				case Coin:	break;
				case Bomber: // usa nations card
					addBomber(1,replay);
					b.logGameEvent("+ bomber");
					break;
				default: throw G.Error("Not expecting %s under coins",ch);
				}
				break;
			case Plutonium:
				addPlutonium(1,replay);
				b.logGameEvent("+ plutonium");
				break;
			case Uranium:
				addUranium(1,replay);
				b.logGameEvent("+ uranium");
				break;
			case Damage:
				b.repairCounter = selected.height();
				b.setState(ManhattanState.Repair);
				break;
			case Other:
				// selected a blank type. This is the 'xx' character from choose scientist or engineer 
				break;
			case Bomb:
				if(something==ManhattanChip.BombBack)
				{	// nation france selects a blind card
					ManhattanCell designs = findEmptyStockPile();
					ManhattanChip newb = b.getABomb();
					if(newb!=null) 
						{ designs.addChip(newb); 
						  if(b.robot!=null) { b.robot.setInhibitions(designs); }
						}
					if(replay.animate)
					{
						b.animationStack.push(b.seeBombs);
						b.animationStack.push(designs);
					}
				}
				else
				{
				ManhattanCell designs = findEmptyStockPile();
				designs.addChip(something);
				selected.removeTop();
				b.seeCurrentDesigns.removeChip(something);
				// reload the bomb display
				b.loadBombDesigns(false);
				if(replay.animate)
				{
					b.animationStack.push(b.seeCurrentDesigns);
					b.animationStack.push(designs);
				}
				}
				
				break;
			default: G.Error("Not expecting %s",something);
			
			}
		}
		}
	}
	private void makeOnly(ManhattanCell ca[],ManhattanCell c)
	{
		for(ManhattanCell cell : ca)
		{
			if(cell!=c && cell.height()>0) { cell.removeTop(); }
		}
	}
	// the the UI is used to change a marker
	public void changeMarker(ManhattanCell c) {
		switch(c.rackLocation())
		{
		case Fighters: 
				nFighters = c.row; 
				makeOnly(fighters,c);
				break;
		case Bombers: 
				nBombers = c.row; 
				makeOnly(bombers,c);
				break;
		case SeeUranium: 
				nUranium = c.row; 
				break;
		case SeePlutonium: 
				nPlutonium = c.row; 
				break;
		case SeeEspionage: 
				nEspionage = c.row; 
				break;
		default: G.Error("Not expecting %s",c);
		}
		
	}
	public void discardBombs(ChipStack selectedChips,replayMode replay) 
	{
		while(b.selectedCells.size()>0)
		{
			ManhattanCell c = b.selectedCells.pop();
			ManhattanChip ch = c.chipAtIndex(0);
			c.reInit();
			b.seeDiscardedBombs.addChip(ch);	
			if(replay.animate){
				b.animationStack.push(c);
				b.animationStack.push(b.seeBombs);
			}
		}
	}
	
	public void addAirstrikeMoves(CommonMoveStack all,ManhattanChip playing,PlayerBoard victim,boolean japan,boolean lemay,int who) 
	{
		// add airstrike moves on the victim
		if(nFighters>0 && (playing==null || playing==fighter))
		{
			if(victim.nFighters>0) 
			{ 
				for(int i = 1,dest=victim.nFighters-1;i<=(lemay?1:nFighters) && dest>=0;i++,dest--)
				{
				all.push(new ManhattanMovespec(MOVE_ATTACK,fighters[nFighters],0,
					victim.fighters[dest],who));
				}
			}
			if(victim.nBombers>0)
			{
				for(int i = 1,dest=victim.nBombers-1;i<=(lemay?1:nFighters) && dest>=0;i++,dest--)
				{
				all.push(new ManhattanMovespec(MOVE_ATTACK,fighters[nFighters],0,
						victim.bombers[dest],who)); 
				}
			}
		}
		if(nBombers>0 && victim.nFighters==0 && (playing==null || playing==bomber))
		{
			// open season, bomb away!
			for(int lim=victim.buildings.size()-1; lim>=0; lim--)
			{ 	ManhattanCell dest = victim.buildings.elementAt(lim);
				if(!dest.inhibited && (dest.type==Type.Building) && dest.height()>0)
				{	// excluding nations
				ManhattanChip ch = dest.chipAtIndex(0);
				if(ch.type==Type.Building)	// not a nations card
					{
					all.push(new ManhattanMovespec(MOVE_ATTACK,bombers[nBombers],0,
							dest,who)); 
					}
				}	
			}
		}
		if (japan
				&& nFighters>0 
				&& (playing==null || playing==fighter)
				&& victim.nFighters==0)
			{
			// open season, bomb away!
			for(int lim=victim.buildings.size()-1; lim>=0; lim--)
			{ 	ManhattanCell dest = victim.buildings.elementAt(lim);
			if((dest.type==Type.Building) && dest.height()>0)
				{	// excluding nations
				ManhattanChip ch = dest.chipAtIndex(0);
				if(ch.type==Type.Building)	// not a nations card
				{
				all.push(new ManhattanMovespec(MOVE_ATTACK,fighters[nFighters],0,
						dest,who)); 
				}}
			}
		}
	
	}
	public void setInhibitions(ManhattanPlay m,CellStack from)
	{
		for(int lim = from.size()-1; lim>=0; lim--)
		{	ManhattanCell c = from.elementAt(lim);
			c.inhibited = false;
			m.setInhibitions(c);
		}
	}
	public void setInhibitions(ManhattanPlay m) {
		setInhibitions(m,buildings);
		setInhibitions(m,stockpile);
	}
	
}
