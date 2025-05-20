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
package viticulture;

import static viticulture.ViticultureConstants.*;

import java.util.StringTokenizer;

import lib.AR;
import lib.Bitset;
import lib.Digestable;
import lib.G;
import lib.OStack;
import lib.Random;
import online.game.replayMode;
import viticulture.ViticultureConstants.ChipType;
import viticulture.ViticultureConstants.ScoreType;
import viticulture.ViticultureConstants.ViticultureId;
import viticulture.ViticultureConstants.ViticultureState;

class CardPointer implements Digestable
{
	ViticultureId source;
	ViticultureChip card;
	int index = -1;
	public CardPointer(ViticultureId s,ViticultureChip chip,int i)
	{
		source = s;
		card = chip;
		index = i;
	}
	public long Digest(Random r) {
		return (source.ordinal()*card.Digest(r)*(index+10));
	}
	public CardPointer copy() { return new CardPointer(source,card,index); }
}
class CardPointerStack extends OStack<CardPointer> implements Digestable
{
	public CardPointer[] newComponentArray(int sz) {	return new CardPointer[sz]; }
	
	public void copyFrom(OStack<CardPointer> other)
	{
		super.copyFrom(other);
		for(int i=0;i<size();i++) { setElementAt(elementAt(i).copy(),i); }
	}
	public boolean contains(ViticultureChip ch)
	{
		for(int i=0;i<size();i++) { if(elementAt(i).card==ch) { return true; }}
		return false;
	}
	public boolean contains(int index)
	{
		for(int i=0;i<size();i++) { if(elementAt(i).index==index) { return true; }}
		return false;
	}
	public ViticultureChip remove(ViticultureId source,ViticultureChip chip,int index)
	{	int ind = indexOf(source,chip,index);
		if(ind>=0)
		{
			remove(ind);
			return chip;
		}
		return null;
	}
	public ViticultureChip remove(ViticultureId source)
	{	for(int lim=size()-1; lim>=0; lim--)
			{	CardPointer cp = elementAt(lim);
				if(cp.source==source) { remove(lim);  return cp.card; }
			}
		return null;
	}
	
	public int indexOf(ViticultureId source,ViticultureChip chip,int index)
	{
		for(int lim = size()-1; lim>=0; lim--)
		{
			CardPointer item = elementAt(lim);
			if(item.source==source && item.card==chip && item.index==index) 
			{
				return lim;
			}
		}
		return -1;
	}
	public boolean contains(ViticultureId source,ViticultureChip chip,int index)
	{
		return indexOf(source,chip,index)>=0;
	}
	public boolean contains(ViticultureId source)
	{
		for(int lim=size()-1; lim>=0; lim--)
		{
			if(elementAt(lim).source==source) { return true; }
		}
		return false;
	}
	public CardPointer push(ViticultureId source,ViticultureChip chip,int index)
	{	CardPointer cp = new CardPointer(source,chip,index);
		push(cp);
		return cp;
	}

	public long Digest(Random r) {
		long v= 0;
		for(int i=0;i<size();i++) { v ^= elementAt(i).Digest(r);}
		return v;
	}

}
public class PlayerBoard
{
	class ScoreEvent
	{
		int season;
		int year;
		int change;
		int net;
		ViticultureChip visual;
		ScoreType type;
		String message;
		ScoreEvent(int s,int y,int ch,int n,String msg,ViticultureChip hint,ScoreType t)
		{
			season = s;
			type = t;
			year = y;
			change = ch;
			net = n;
			message = msg;
			visual = hint;
		}
		public int changeSummary() {
			switch(type)
			{
			case PlayBlue:
			case PlayYellow:
			case ReceiveBlue:
			case ReceiveYellow:
				return(1);	// count events for these that don't directly change the score
			default: return(change);
			}
		}
	}
	class ScoreStack extends OStack<ScoreEvent>
	{
		public ScoreEvent[] newComponentArray(int sz) {
			return(new ScoreEvent[sz]);
		}
	}
	ScoreStack scoreEvents = new ScoreStack();
	int boardIndex;
	int colorIndex;
	char colCode;
	public boolean publicCensoring = true;
	public boolean hiddenCensoring = true;
	Bitset<Option> selectedOptions = new Bitset<Option>();
	Bitset<Option> unSelectedOptions = new Bitset<Option>();
	ViticultureCell messengerCell;	// place where your messenger was placed in the future
	Viticulturemovespec messengerMove;
	
	ViticultureBoard bb;		// parent board
	ViticultureColor color;		// our color
	ViticultureCell uiCells = null;
	ViticultureCell allCells = null;
	ViticultureCell fields[] = new ViticultureCell[3];
	ViticultureCell vines[] = new ViticultureCell[3];
	ViticultureCell workers;
	ViticultureCell bonusActions;
	ViticultureCell isStartPlayer;
	
	// unbuilt buildings
	ViticultureCell unbuiltTrellis;
	ViticultureCell unbuiltWaterTower;
	ViticultureCell unbuiltWindmill;
	ViticultureCell unbuiltTastingRoom;
	ViticultureCell unbuiltYoke;
	ViticultureCell unbuiltCottage;
	ViticultureCell unbuiltMediumCellar;
	ViticultureCell unbuiltLargeCellar;

	// buildable buildings
	ViticultureCell trellis;
	ViticultureCell waterTower;
	ViticultureCell windmill;
	ViticultureCell tastingRoom;
	ViticultureCell yoke;
	ViticultureCell cottage;
	ViticultureCell mediumCellar;
	ViticultureCell largeCellar;
	ViticultureCell buildable[] = null;
	ViticultureCell unBuilt[] = null;
	
	ViticultureCell stars;
	ViticultureCell yokeWorker;		// worker placement for personal yoke
	ViticultureCell redGrape[] = new ViticultureCell[9];
	ViticultureCell whiteGrape[] = new ViticultureCell[9];
	ViticultureCell redWine[] = new ViticultureCell[9];
	ViticultureCell whiteWine[] = new ViticultureCell[9];
	ViticultureCell roseWine[] = new ViticultureCell[6];
	ViticultureCell champagne[] = new ViticultureCell[3];
	ViticultureCell wineTypes[][] = { whiteWine, redWine, roseWine, champagne };
	ViticultureCell fillableWineOrders;		// used by mercado
	ViticultureCell cards;

	CardPointerStack selectedCards = new CardPointerStack();
	
	public int nSelectedCards() { return  selectedCards.size(); };
	public int topSelectedCardIndex() 
	{ return selectedCards.pop().index;
	}

	public CardPointer removeSelectedCard(boolean bottom) 
		{  
		CardPointer removed = bottom ? selectedCards.remove(0,true) : selectedCards.pop();
		// renumber the other cards in the stack if they refer to the same source
		// and removing the selected card would change the index
		for(int i=0;i<selectedCards.size();i++)
		{	CardPointer item = selectedCards.elementAt(i);
			if(item.source==removed.source && item.index>removed.index) { item.index--; }
		}
		
		return removed;
		}
	
	public void clearSelectedCards()
	{
		selectedCards.clear();
	}
	
	// select a single card from the hand
	public void selectCardFromHand(int index)
	{
		{
		 ViticultureChip ch = cards.chipAtIndex(index);
		 ViticultureChip removed = selectedCards.remove(cards.rackLocation(),ch,index);
		 selectedCards.clear();	// enforce single selection
		 if(removed==null) 
		 	{ selectedCards.push(cards.rackLocation(),ch,index);
       	   	}
		}
	}
	
	ViticultureCell oracleCards = null;			// card for selection by the oracle
	ViticultureCell oracleColors[] = null;
	CellStack selectedCells = new CellStack();	// fields selected for the next operation
	ViticultureCell structures[] = new ViticultureCell[2];
	ViticultureCell wakeupPosition = null;
	ViticultureCell activeWakeupPosition = null;
	ViticultureCell destroyStructureWorker =null;
	ViticultureCell pendingWorker = null;
	ViticultureCell workerCells[] = null;
	ViticultureCell buildStructureCells[] = null;
	ViticultureCell workerTypes =null;
	ViticultureChip grayWorker = null;	// if we get the gray one, it's also here
	boolean isReady = false;
	int usedWindmill = 0;			// year it was used
	int usedTastingRoom = 0;		// year it was used
	ViticultureChip flashChip = null;
	boolean papaResolved = false;
	String initialBonus = "none";
	boolean initialBonusDeclined = false;
	ChipType extraWorker1 = null;
	ChipType extraWorker2 = null;

	int cash;
	int score;
	int wineSalePoints;
	int wineOrderPoints;
	int startingScore;
	int startingCash;
	private int playerSeason;
	public int season() { return playerSeason; }
	public void setSeason(int n) { playerSeason = n; }
	public int statSummary[] = new int[ScoreType.values().length];
	StringBuilder scoreString = new StringBuilder();
	public void buildStatString()
	{	
		AR.setValue(statSummary,0);	
		int statCount[] = new int[ScoreType.values().length];
		for(int lim=scoreEvents.size()-1; lim>=0; lim--)
		{	
			ScoreEvent e = scoreEvents.elementAt(lim);
			int ord  = e.type.ordinal();
			statSummary[ord]+=e.change;
			statCount[ord]++;
		}
		scoreString.append("\n");
		for(ScoreType e : ScoreType.values()) {
			int ord  = e.ordinal();
			if(statCount[ord]>0)
			{
			scoreString.append(statCount[ord]);
			scoreString.append(" ");
			scoreString.append(e.name());
			int ss = statSummary[ord];
			if(ss!=0)
			{
			scoreString.append(" for ");
			scoreString.append(ss);
			}
			scoreString.append("\n");
			}
		}
		
	}
	int nWorkers;
	int residual;
	boolean showCards = false;
	boolean showPlayerBoard = false;
	// user interface only
	ViticultureCell cashDisplay = null;
	ViticultureCell coinDisplay = null;
	ViticultureCell vpDisplay = null;
	ViticultureCell redGrapeDisplay = null;
	ViticultureCell whiteGrapeDisplay = null;
	ViticultureCell roosterDisplay = null;
	ViticultureCell hiddenCardDisplay1;
	ViticultureCell hiddenCardDisplay2;
	boolean hiddenCardsOnTop =false;
	ViticultureCell hiddenBigStack = new ViticultureCell(ViticultureId.CancelBigChip);
	boolean showHiddenBigStack = false;
	
	public String toString() { return("<pb "+color+">"); }
	
	private ViticultureCell newUIcell(ViticultureId id, char charcode, int row, ChipType ctype) 
	{
		ViticultureCell c = new ViticultureCell(id,charcode,row);
		if(ctype!=null) { c.contentType = ctype; }
		c.onBoard = false;
		c.next = uiCells;
		uiCells = c;
		bb.register(c);
		return(c);
	}
	private ViticultureCell newUIcell(ViticultureId id, char charcode, int row, ChipType ctype,String des) 
	{
		ViticultureCell c = newUIcell(id,charcode,row,ctype);
		c.toolTip = des;
		return c;
	}
	public boolean isMyWorker(ViticultureChip ch)
	{
		return(ch.color==color || ch==grayWorker);
	}
	
	public boolean isMyRegularWorker(ViticultureChip ch)
	{
		return( ((ch.color==color) && (ch.type==ChipType.Worker))
				|| (ch==grayWorker));
	}


	private ViticultureCell newcell(ViticultureId loc,char col,int row,ChipType content,String description)
	{
		ViticultureCell c = new ViticultureCell(loc,col,row);
		c.onBoard = false;
		c.toolTip =description;
		bb.register(c);
		if(content!=null) { c.contentType = content; }
		c.next = allCells;
		allCells = c;
		return(c);
	}
	private ViticultureCell newcell(ViticultureId loc,char col,int row,ChipType content,int cost,String description)
	{	
		ViticultureCell c = newcell(loc,col,row,content,description);
		c.cost = cost;
		return(c);
	}
	public void setColor(int cindex)
	{
		colorIndex = cindex;
		color = ViticultureColor.values()[cindex];
	}
	PlayerBoard(ViticultureBoard board,int idx,int colorIndex)
	{	bb = board;
		boardIndex = idx;
		colCode = (char)('A'+boardIndex);
		setColor(colorIndex);

		for(int i=0;i<fields.length;i++)
		{
			fields[i] = newcell(ViticultureId.Field,colCode,i,ChipType.Field,FieldDescription);
			vines[i] = newcell(ViticultureId.Vine,colCode,i,ChipType.GreenCard,null);
		}
		for(int i=0;i<redGrape.length;i++)
		{
			redGrape[i] = newcell(ViticultureId.RedGrape,colCode,i,ChipType.Bead,RedGrapeDescription);
		}
		for(int i=0;i<whiteGrape.length;i++)
		{
			whiteGrape[i] = newcell(ViticultureId.WhiteGrape,colCode,i,ChipType.Bead,WhiteGrapeDescription);
		}
		for(int i=0;i<redWine.length;i++)
		{
			redWine[i] = newcell(ViticultureId.RedWine,colCode,i,ChipType.Bead,RedWineDescription);
		}
		for(int i=0;i<whiteWine.length;i++)
		{
			whiteWine[i] = newcell(ViticultureId.WhiteWine,colCode,i,ChipType.Bead,WhiteWineDescription);
		}
		for(int i=0;i<roseWine.length;i++)
		{
			roseWine[i] = newcell(ViticultureId.RoseWine,colCode,i,ChipType.Bead,RoseWineDescription);
		}
		for(int i=0;i<champagne.length;i++)
		{
			champagne[i] = newcell(ViticultureId.Champaign,colCode,i,ChipType.Bead,ChampaignDescription);
		}
		
		workers = newcell(ViticultureId.Workers,colCode,0,ChipType.Worker,WorkersDescription);
		bonusActions = newcell(ViticultureId.BonusActions,colCode,0,ChipType.Bead,null);
		isStartPlayer = newcell(ViticultureId.StartPlayer,colCode,0,ChipType.StartPlayer,null);
		trellis = newcell(ViticultureId.Trellis,colCode,0,ChipType.Trellis,2,TrellisDescription);
		waterTower = newcell(ViticultureId.WaterTower,colCode,0,ChipType.WaterTower,3,WaterTowerDescription);
		windmill = newcell(ViticultureId.Windmill,colCode,0,ChipType.Windmill,5,WindmillDescription);
		tastingRoom = newcell(ViticultureId.TastingRoom,colCode,0,ChipType.TastingRoom,6,TastingRoomDescription);
		yoke = newcell(ViticultureId.Yoke,colCode,0,ChipType.Yoke,2,YokeDescription);
		cottage = newcell(ViticultureId.Cottage,colCode,0,ChipType.Cottage,4,CottageDescription);
		mediumCellar = newcell(ViticultureId.MediumCellar,colCode,0,ChipType.MediumCellar,4,MediumCellarDescription);
		largeCellar = newcell(ViticultureId.LargeCellar,colCode,0,ChipType.LargeCellar,6,LargeCellarDescription);
		buildable = new ViticultureCell[]{
				trellis,waterTower,windmill,
				tastingRoom,yoke,cottage,
				mediumCellar,largeCellar
		};
		
		hiddenCardDisplay1 = newUIcell(ViticultureId.CardDisplay,colCode,0,ChipType.Card);
		hiddenCardDisplay2 = newUIcell(ViticultureId.CardDisplay,colCode,1,ChipType.Card);
	
		unbuiltTrellis = newUIcell(ViticultureId.UnbuiltTrellis,colCode,0,ChipType.Trellis,TrellisDescription);	// off the books cells
		unbuiltWaterTower = newUIcell(ViticultureId.UnbuiltWaterTower,colCode,0,ChipType.WaterTower,WaterTowerDescription);
		unbuiltTastingRoom = newUIcell(ViticultureId.UnbuiltTastingRoom,colCode,0,ChipType.TastingRoom,TastingRoomDescription);
		unbuiltWindmill = newUIcell(ViticultureId.UnbuiltWindmill,colCode,0,ChipType.Windmill,WindmillDescription);
		unbuiltYoke = newUIcell(ViticultureId.UnbuiltYoke,colCode,0,ChipType.Yoke,YokeDescription);
		unbuiltCottage = newUIcell(ViticultureId.UnbuiltCottage,colCode,0,ChipType.Cottage,CottageDescription);
		unbuiltMediumCellar = newUIcell(ViticultureId.UnbuiltMediumCellar,colCode,0,ChipType.MediumCellar,MediumCellarDescription);
		unbuiltLargeCellar = newUIcell(ViticultureId.UnbuiltLargeCellar,colCode,0,ChipType.LargeCellar,LargeCellarDescription);
		
		unBuilt = new ViticultureCell[] {
				unbuiltTrellis,unbuiltWaterTower,unbuiltWindmill,
				unbuiltTastingRoom,unbuiltYoke,unbuiltCottage,
				unbuiltMediumCellar,unbuiltLargeCellar
		};
		yokeWorker = newcell(ViticultureId.PlayerYokeWorker,colCode,0,ChipType.Worker,YokeDescription);
		stars = newcell(ViticultureId.PlayerStars,colCode,0,ChipType.Star,UnplacedStarsDescription);
		cards = newcell(ViticultureId.Cards,colCode,0,ChipType.Card,CardDescription);
		fillableWineOrders = newcell(ViticultureId.Cards,colCode,1,ChipType.Card,null);
		oracleCards = newcell(ViticultureId.SelectedCards,colCode,1,ChipType.Card,null);
		oracleColors = null;
		structures[0] = newcell(ViticultureId.PlayerStructureCard,colCode,0,ChipType.StructureCard,StructuresDescription);
		structures[1] = newcell(ViticultureId.PlayerStructureCard,colCode,1,ChipType.StructureCard,StructuresDescription);
		destroyStructureWorker = newcell(ViticultureId.DestroyStructureWorker,colCode,0,ChipType.Worker,DestroyStructureDescription);

		cashDisplay = newUIcell(ViticultureId.Cash,colCode,0,ChipType.Coin);
		coinDisplay = newUIcell(ViticultureId.Coins,colCode,0,ChipType.Coin);
		vpDisplay = newUIcell(ViticultureId.VP,colCode,0,ChipType.VP);
		
		redGrapeDisplay = newUIcell(ViticultureId.RedGrapeDisplay,colCode,0,ChipType.RedGrape);

		whiteGrapeDisplay = newUIcell(ViticultureId.WhiteGrapeDisplay,colCode,0,ChipType.WhiteGrape);
		roosterDisplay = newUIcell(ViticultureId.RoosterDisplay,colCode,0,ChipType.Rooster);
		pendingWorker = newUIcell(ViticultureId.PendingWorker,colCode,0,ChipType.Worker);
		workerTypes = newcell(ViticultureId.PendingWorker,colCode,1,ChipType.Worker,null);
		workerCells = new ViticultureCell[]{ pendingWorker,yokeWorker,destroyStructureWorker,structures[0],structures[1],fields[0],fields[1],fields[2]};
		buildStructureCells = new ViticultureCell[] { structures[0],structures[1],fields[0],fields[1],fields[2]};
		doInit();
	}
	public void unselect()
	{
		for(ViticultureCell c =allCells; c!=null; c=c.next) { c.selected = false; } 
		for(ViticultureCell c : unBuilt) { c.selected = false; }
	}
	public void unselectUI()
	{
		for(ViticultureCell c =uiCells; c!=null; c=c.next) { c.selected = false; } 
	}
	public void startNewYear()
	{
		isStartPlayer.reInit();
		oracleColors = null;
		setSeason(0);
		if(bb.revision>=112) { grayWorker=null; }	// reset the gray meeple ownership between years.
		for(ViticultureCell field : fields) 
		{	// clear the "harvested" marker
			if(field.topChip()==ViticultureChip.Bead) { field.removeTop(); }
		}
	}
	public void doInit()
	{	scoreEvents.clear();
		for(ViticultureCell c = allCells; c!=null; c=c.next) { c.reInit(); }
		for(ViticultureCell c = uiCells; c!=null; c=c.next) { c.reInit(); } 
		for(int i=0;i<fields.length;i++)
			{ fields[i].addChip(ViticultureChip.Fields[i]);
			}

		for(int i=0;i<STARS_PER_PLAYER;i++) { stars.addChip(getContent(stars)); }
		for(ViticultureCell c : unBuilt)
		{
			c.addChip(getContent(c));
		}
		hiddenCardsOnTop = false;
		hiddenCensoring = true;
		vpDisplay.addChip(ViticultureChip.VictoryPoint_1);
		redGrapeDisplay.addChip(ViticultureChip.RedGrape);
		whiteGrapeDisplay.addChip(ViticultureChip.WhiteGrape);
		roosterDisplay.addChip(ViticultureChip.Roosters[colorIndex]);
		pendingWorker.reInit();
		selectedCards.clear();
		oracleCards.reInit();
		oracleColors = null;
		workerTypes.reInit();
		scoreString = new StringBuilder();
		selectedCells.clear();
		papaResolved = false;
		wakeupPosition = null;
		activeWakeupPosition = null;
		messengerCell = null;
		grayWorker = null;
		flashChip = null;
		isReady = false;
		cash = 0;
		score = 0;
		wineOrderPoints = 0;
		wineSalePoints = 0;
		nWorkers = 0;
		usedWindmill = 0;
		usedTastingRoom = 0;
		residual = 0;
		playerSeason = 0;
		
		initialBonus = "none";
		initialBonusDeclined = false;
		extraWorker1 = null;
		extraWorker2 = null;

		startingScore = score;
		startingCash = cash;
		selectedOptions.clear();
		unSelectedOptions.clear();
	}
	public long Digest(Random r)
	{
		long v = 0;
		for(ViticultureCell c = allCells; c!=null; c=c.next) { v ^= c.Digest(r); }
		v ^= bb.Digest(r,papaResolved);
		v ^= bb.Digest(r,cash);
		v ^= bb.Digest(r,score);
		v ^= bb.Digest(r,nWorkers);
		v ^= bb.Digest(r,usedTastingRoom);
		v ^= bb.Digest(r,usedWindmill);
		v ^= bb.Digest(r,residual);
		v ^= bb.Digest(r,wakeupPosition);
		v ^= bb.Digest(r,activeWakeupPosition);
		v ^= bb.Digest(r,messengerCell);
		v ^= bb.Digest(r,grayWorker);
		v ^= bb.Digest(r,selectedCells);
		v ^= bb.Digest(r,pendingWorker);
		v ^= bb.Digest(r,workerTypes);
		v ^= selectedCards.Digest(r);
		v ^= bb.Digest(r,oracleCards);
		v ^= bb.Digest(r,roosterDisplay.selected);
		v ^= bb.Digest(r,playerSeason);
		v ^= bb.Digest(r,startingCash);
		v ^= bb.Digest(r,startingScore);
		v ^= bb.Digest(r,isReady);
		
		return(v);
	}
	public void sameBoard(PlayerBoard other)
	{	
		for(ViticultureCell c=allCells,d=other.allCells; c!=null; c=c.next,d=d.next)
		{
			G.Assert(c.sameCell(d),"player cells %s and %s mismatch",c,d);
		}
		G.Assert(papaResolved==other.papaResolved,"cash mismatch");
		G.Assert(bb.sameCells(wakeupPosition, other.wakeupPosition),"wakeupposition mismatch");
		G.Assert(bb.sameCells(activeWakeupPosition, other.activeWakeupPosition),"activeWakeupposition mismatch");
		G.Assert(bb.sameCells(messengerCell, other.messengerCell),"messenger mismatch");
		G.Assert(usedTastingRoom==other.usedTastingRoom,"usedTastingRoom mismatch");
		G.Assert(usedWindmill==other.usedWindmill,"usedWindmill mismatch");
		G.Assert(isReady == other.isReady,"isReady mismatch");
		G.Assert(cash==other.cash,"cash mismatch");
		G.Assert(score==other.score,"score mismatch");
		G.Assert(nWorkers==other.nWorkers,"nWorkers mismatch");
		G.Assert(residual==other.residual,"residual mismatch");
		G.Assert(grayWorker==other.grayWorker,"grayWorker mismatch");
		G.Assert(bb.sameCells(selectedCells,other.selectedCells) , "selectedCells mismatch");
		G.Assert(workerTypes.sameContents(other.workerTypes),"worker types mismatch");
		G.Assert(pendingWorker.sameContents(other.pendingWorker),"pending worker types mismatch");
		G.Assert(oracleCards.sameContents(other.oracleCards),"oracle cards mismatch");
		G.Assert(oracleColors==other.oracleColors,"Oracle Colors mismatch");
		G.Assert(roosterDisplay.selected==other.roosterDisplay.selected,"rooster selection mismatch");
		G.Assert(selectedCards.size()==other.selectedCards.size(),"selectedCards mismatch");
		bb.Assert(startingCash==other.startingCash,"startingCash mismatch");
		G.Assert(startingScore==other.startingScore,"startingCash mismatch");
		
		G.Assert(playerSeason==other.playerSeason,"player season matches");
	}
	public void copyFrom(PlayerBoard other)
	{
		for(ViticultureCell c=allCells,d=other.allCells; c!=null; c=c.next,d=d.next)
		{
			c.copyFrom(d);
		}
		colorIndex = other.colorIndex;
		scoreEvents.copyFrom(other.scoreEvents);
        cashDisplay.copyFrom(other.cashDisplay);
        AR.copy(statSummary,other.statSummary);
		papaResolved = other.papaResolved;
		initialBonus = other.initialBonus;
		initialBonusDeclined = other.initialBonusDeclined;
		extraWorker1 = other.extraWorker1;
		extraWorker2 = other.extraWorker2;
		wakeupPosition = bb.getCell(other.wakeupPosition);
		activeWakeupPosition = bb.getCell(other.activeWakeupPosition);
		messengerCell = bb.getCell(other.messengerCell);
		messengerMove = Viticulturemovespec.copyFrom(other.messengerMove);
		usedWindmill = other.usedWindmill;
		usedTastingRoom = other.usedTastingRoom;
		isReady = other.isReady;
		cash = other.cash;
		score = other.score;
		wineSalePoints = other.wineSalePoints;
		wineOrderPoints = other.wineOrderPoints;
		nWorkers = other.nWorkers;
		residual = other.residual;
		grayWorker = other.grayWorker;
		startingScore = other.startingScore;
		startingCash = other.startingCash;
		bb.getCell(selectedCells,other.selectedCells);
		workerTypes.copyFrom(other.workerTypes);
		pendingWorker.copyFrom(other.pendingWorker);
		selectedCards.copyFrom(other.selectedCards);
		vpDisplay.copyFrom(other.vpDisplay);
		cashDisplay.copyFrom(other.cashDisplay);
		redGrapeDisplay.copyFrom(other.redGrapeDisplay);
		whiteGrapeDisplay.copyFrom(other.whiteGrapeDisplay);
		oracleCards.copyFrom(other.oracleCards);
		oracleColors = other.oracleColors;
		roosterDisplay.selected = other.roosterDisplay.selected;
	    bb.copyFrom(unBuilt,other.unBuilt);
	    selectedCards.copyFrom(other.selectedCards);
	    playerSeason = other.playerSeason;
	    selectedOptions.copy(other.selectedOptions);
	    unSelectedOptions.copy(other.unSelectedOptions);
	}
	
	ViticultureChip mama;
	ViticultureChip papa;
	
	
	public void resolveMama()
	{	// everybody gets three meeples
   		workers.addChip(ViticultureChip.Bigmeeples[colorIndex]);	// technically this is with the papa
   		workers.addChip(ViticultureChip.Meeples[colorIndex]);
   		workers.addChip(ViticultureChip.Meeples[colorIndex]);
   		nWorkers += 3;
		switch(mama.order)
		{
		case 1:	//alaena
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			break;
		case 2:	//alyssa
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			break;
		case 3:	//deann
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			break;
		case 4:	//margot
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			break;
		case 5:	//margret
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.yellowCards.removeTop());
			break;
		case 6:	//nici
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			break;
		case 7:	//teruyo
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			break;
		case 8:	//emily
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.yellowCards.removeTop());
			break;
		case 9:	//rebecca
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			break;
		case 10: //danyel
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			break;
		case 11:	//laura
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			break;
		case 12:	//jess
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			break;
		case 13:	//casey
			cards.addChip(bb.purpleCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			break;
		case 14:	//christine
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			break;
		case 15:	//naja
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			break;
		case 16:	//falon
			cards.addChip(bb.purpleCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			break;
		case 17:	//nicole
			cards.addChip(bb.greenCards.removeTop());
			cards.addChip(bb.blueCards.removeTop());
			cash += 2;
			break;
		case 18:	//ariel
			cards.addChip(bb.yellowCards.removeTop());
			cards.addChip(bb.purpleCards.removeTop());
			cash += 2;
			break;
				
		default: G.Error("Not expecting %s",mama);
		}
	}
	public void resolvePapa(boolean first)
	{	papaResolved = true;
		ViticultureChip building = null;
		String resources = "";
		switch(papa.order)
		{
		case 1:	// andrew
			cash += 4;			
			initialBonus = "trellis";
			if(first) 
			{
			  trellis.addChip(building =  ViticultureChip.Trellises[colorIndex]);
			}
			else 
			{ cash += 2;
			  initialBonusDeclined = true;
			}
			break;
		case 2: // christian
			cash += 3;
			initialBonus = "water tower";
			if(first)
				{ waterTower.addChip(building = ViticultureChip.Watertowers[colorIndex]);
				}
			else { cash += 3; 
			initialBonusDeclined = true;
			}
			break;
		case 3:	// jay
			cash += 5;
			initialBonus = "yoke";
			if(first) { yoke.addChip(building = ViticultureChip.Yokes[colorIndex]); }
			else {  cash +=2;
			initialBonusDeclined = true;}
			break;
		case 4:	// josh
			initialBonus = "medium cellar";
			cash += 3;
			if(first) { mediumCellar.addChip(building = ViticultureChip.MediumCellars[colorIndex]);  }
			else { cash +=4; 
			initialBonusDeclined = true;}
			break;
		case 5:	// kozi
			initialBonus = "cottage";
			cash += 2;
			if(first) { cottage.addChip(building = ViticultureChip.Cottages[colorIndex]); }
			else { cash += 4;
			initialBonusDeclined = true;}
			break;
		case 6:	// matthew
			cash += 1;
			initialBonus = "windmill";
			if(first) 
			{ 
			  windmill.addChip(building = ViticultureChip.Windmills[colorIndex]);
			}
			else 
			{ cash += 5;
			initialBonusDeclined = true;
			}
			break;
		case 7: // matt
			cash += 0;
			initialBonus = "tasting room";
			if(first) {tastingRoom.addChip(building = ViticultureChip.Tastingrooms[colorIndex]);  }
			else { cash += 6;
			initialBonusDeclined = true; }
			break;
		case 8: // paul
			cash += 5;
			initialBonus = "trellis";
			if(first) { trellis.addChip(building = ViticultureChip.Trellises[colorIndex]);  }
			else { cash += 1;
			initialBonusDeclined = true; }
			break;
		case 9:	// stephan
			cash += 4;
			initialBonus = "water tower";
			if(first) { waterTower.addChip(building = ViticultureChip.Watertowers[colorIndex]); }
			else { cash += 2;
			initialBonusDeclined = true;}
			break;
		case 10: // steven
			cash += 6;
			initialBonus = "yoke";
			if(first) { yoke.addChip(building = ViticultureChip.Yokes[colorIndex]); }
			else { cash += 1;
			initialBonusDeclined = true;}
			break;
		case 11: //joel
			cash += 4;
			initialBonus = "medium cellar";
			if(first) { mediumCellar.addChip(building = ViticultureChip.MediumCellars[colorIndex]); }
			else { cash +=3 ;
			initialBonusDeclined = true;}
			break;
		case 12: //raymond
			cash += 3;
			initialBonus = "cottage";
			if(first) { cottage.addChip(building = ViticultureChip.Cottages[colorIndex]); }
			else { cash += 3; 
			initialBonusDeclined = true;}
			break;
		case 13: //jerry
			cash += 2;
			initialBonus = "windmill";
			if(first) { windmill.addChip(building = ViticultureChip.Windmills[colorIndex]); }
			else { cash += 4;
			initialBonusDeclined = true;}
			break;
		case 14: // trevor
			cash += 1;
			initialBonus = "tasting room";
			if(first) { tastingRoom.addChip(building = ViticultureChip.Tastingrooms[colorIndex]); }
			else { cash += 5;
			initialBonusDeclined = true;}
			break;
		case 15: //rafael
			cash += 2;
			initialBonus = "extra worker";
			if(first) { workers.addChip(building = ViticultureChip.Meeples[colorIndex]); nWorkers++; }
			else { cash += 4;
			initialBonusDeclined = true;}
			break;
		case 16: //gary
			cash += 3;
			initialBonus = "extra worker";
			if(first) {  workers.addChip(building = ViticultureChip.Meeples[colorIndex]); nWorkers++; }
			else { cash += 3;
			initialBonusDeclined = true;}
			break;
		case 17: //morten
			cash += 4;
			initialBonus = "vp";
			if(first) { score +=1; resources += " 1VP";}
			else { cash +=3;
			initialBonusDeclined = true;
			}
			break;
		case 18: // alan
			cash += 5;
			initialBonus = "vp";
			if(first) { score += 1; resources += " 1VP";}
			else { cash +=2;
			initialBonusDeclined = true;
			}
			break;
		default: G.Error("Not expecting %s",papa);
		}
		resources += " "+cash+"$";
		if(building!=null) { resources += " "+building.color.name()+"-"+building.type.name(); }
		for(int lim=cards.height()-1; lim>=0; lim--)
		{
			resources += " "+cards.chipAtIndex(lim).type.name();
		}
		bb.logRawGameEvent(resources);
	}
	
	public ViticultureChip getScoreMarker() { return(ViticultureChip.Scoreposts[colorIndex]);}
	public ViticultureChip getResidualMarker() { return(ViticultureChip.Bottles[colorIndex]);}
	public ViticultureChip getTrellis() { return(ViticultureChip.Trellises[colorIndex]); }
	public ViticultureChip getWatertower() { return(ViticultureChip.Watertowers[colorIndex]); }
	public ViticultureChip getTastingroom() { return(ViticultureChip.Tastingrooms[colorIndex]); }
	public ViticultureChip getMediumCellar() { return(ViticultureChip.MediumCellars[colorIndex]); }
	public ViticultureChip getWindmill() { return(ViticultureChip.Windmills[colorIndex]); }
	public ViticultureChip getYoke() { return(ViticultureChip.Yokes[colorIndex]); }
	public ViticultureChip getCottage() { return(ViticultureChip.Cottages[colorIndex]); }
	public ViticultureChip getWorker() { return(ViticultureChip.Meeples[colorIndex]); }
	public ViticultureChip getGrandeWorker() { return(ViticultureChip.Bigmeeples[colorIndex]); }
	public ViticultureChip getStar() { return(ViticultureChip.Stars[colorIndex]); }
	public ViticultureChip getSpecialWorker(String type)
	{
		return(ViticultureChip.getChip(ChipType.find(type),color));
	}
	
	public ViticultureChip getRooster() { return(ViticultureChip.Roosters[colorIndex]); }
	

	public ViticultureChip getContent(ViticultureCell c)
	{
		G.Assert(c.col==colCode,"not our cell");
		return(ViticultureChip.getContent(c.contentType,colorIndex));
	}
	public boolean hasCard(ChipType color)
	{	for(int lim=cards.height()-1; lim>=0; lim--) 
			{ if(cards.chipAtIndex(lim).type==color) { return(true);}
			}
		return(false);
	}
	public int nCards(ChipType color)
	{	int n=0;
		for(int lim=cards.height()-1; lim>=0; lim--) 
			{ if(cards.chipAtIndex(lim).type==color) { n++;}
			}
		return(n);
	}
	
	public boolean canPossiblyFillWineOrder()
	{
		return (hasWine() && hasCard(ChipType.PurpleCard));
	}
	
	public boolean hasWine()
	{	for(ViticultureCell wine[] : wineTypes) 
		{	if(hasWine(wine)) { return(true); }
		}
		return(false);
	}

	public boolean hasWine(ViticultureCell cellar[])
	{	for(ViticultureCell c : cellar) { if (c.topChip()!=null) { return(true); }}
		return(false);
	}
	public boolean hasWineWithValue(int n)
	{	for(ViticultureCell wine[] : wineTypes) 
			{
			if(hasWineWithValue(wine,n-(9-wine.length)-1)) { return(true); }
			}
		return(false);
	}
	
	public int nWinesWithValue(int n)
	{	int nval = 0;
		for(ViticultureCell wine[] : wineTypes) 
			{
			if(hasWineWithValue(wine,n-(9-wine.length)-1)) { nval++; }
			}
		return(nval);
	}
	
	// score including tiebreaks
	public int tiebreakScore()
	{	return tiebreakScore(score,cash,totalWineValue(),totalGrapeValue());
	}
	
	public static int tiebreakScore(int score,int cash,int wines,int grapes)
	{	int sc = score;
		sc = sc*100 + cash;
		sc = sc*100 + wines;
		sc = sc*100 + grapes;
		return(sc);
	}

	private int totalGrapeValue()
	{
		return(totalRowValue(redGrape)+totalRowValue(whiteGrape));
	}
	private int totalWineValue()
	{
		return(totalRowValue(champagne)+totalRowValue(roseWine)+totalRowValue(redWine)+totalRowValue(whiteWine));
	}
	private int totalRowValue(ViticultureCell row[])
	{	int offset = 9-row.length;
		int sum = 0;
		for(int lim=row.length-1; lim>=0; lim--) { if(row[lim].topChip()!=null) {  sum += offset+lim; }}
		return(sum);
	}
	
	public boolean hasWineWithValue(ViticultureCell cellar[],int initialOffset)
	{	for(int i=Math.max(0, initialOffset);i<cellar.length;i++)
		{	if(cellar[i].topChip()!=null) { return(true); } 
		}
		return(false);
	}
	public boolean hasStructuresForPlanting(ViticultureChip vine,ViticultureCell addedStructure)
	{	if(addedStructure!=trellis) { if(vine.requiresTrellis() && (trellis.topChip()==null)) { return(false); }}
		if(addedStructure!=waterTower) { if(vine.requiresWaterTower() && (waterTower.topChip()==null)) { return(false); }}
		return(true);
	}
	
	private boolean addWineOrGrapeChip(ViticultureCell grapes[],ViticultureChip chip,int index,replayMode replay)
	{
		int ind = Math.min(grapes.length-1,index-1);
		while(ind>=0 && (grapes[ind].topChip()!=null)) { ind--; }
		if(ind>=0) 
			{ ViticultureCell dest = grapes[ind];
			  dest.addChip(chip);
			  if(replay.animate) {
				  bb.animationStack.push(bb.getCell(dest.rackLocation(),'@',0));
				  bb.animationStack.push(dest);
			  }
			  return true;
			}
		return false;
	}
	public int[] harvest(ViticultureCell vine,replayMode replay)
	{
		int sumOfWhites = 0;
		int sumOfReds = 0;
		for(int lim=vine.height()-1; lim>=0; lim--)
		{	ViticultureChip card = vine.chipAtIndex(lim);
			sumOfWhites += card.whiteVineValue();
			sumOfReds += card.redVineValue();
		}
		if(sumOfReds>0 && !addWineOrGrapeChip(redGrape,ViticultureChip.RedGrape,sumOfReds,replay))
		{
			sumOfReds = 0;
			bb.logGameEvent(DiscardGrapeMessage,"RedGrapeDiscard");
		}
		if(sumOfWhites>0 && !addWineOrGrapeChip(whiteGrape,ViticultureChip.WhiteGrape,sumOfWhites,replay))
		{
			sumOfWhites = 0;
			bb.logGameEvent(DiscardGrapeMessage,"WhiteGrapeDiscard");
		}
		return new int[] {sumOfReds,sumOfWhites};
	}
	// count fields not sold but empty
	public int nEmptyFields()
	{
		int n=0;
		for(int lim=vines.length-1; lim>=0; lim--)
		{
			if((vines[lim].height()==0) && (fields[lim].topChip().type==ChipType.Field)) { n++; }
		}
		return(n);
	}
	// count fields with vines
	public int nPlantedFields()
	{	int n=0;
		for(ViticultureCell c : vines) { if(c.height()>0) { n++; }}
		return(n);
	}
	// number of vines planted in all fields
	public int nPlantedVines()
	{	int n=0;
		for(ViticultureCell c : vines) { n+= c.height(); }
		return(n);		
	}
	// true if there is at least 1 grape
	public boolean hasGrape()
	{	return(hasRedGrape() || hasWhiteGrape());
	}
	public boolean hasWhiteGrape()
	{	return(leastWhiteGrapeValue()>0);
	}
	// return the value (not the index) of the lowest value red grape
	public int leastRedGrapeValue()
	{	return(leastFilledWineValue(redGrape));
	}
	public int leastFilledWineValue(ViticultureCell row[])
	{
		for(int i=0;i<row.length;i++) { if(row[i].topChip()!=null) { return(i+10-row.length); }}
		return(-1);
	}
	// return the value (not the index) of the lowest value red wine
	public int leastRedWineValue()
	{	return(leastFilledWineValue(redWine));
	}
	
	// return the value (not the index) of the lowest value white wine
	public int leastWhiteWineValue()
	{	return(leastFilledWineValue(whiteWine));
	}
	
	// return the value (not the index) of the lowest value rose wine
	public int leastRoseWineValue()
	{	return(leastFilledWineValue(roseWine));
	}
	// return the value (not the index) of the lowest value champagne
	public int leastChampaignValue()
	{
		return(leastFilledWineValue(champagne));
	}
	public int nGrapes()
	{
		return(nWhiteGrapes()+nRedGrapes());
	}
	public int leastWhiteGrapeValue()
	{
		return(leastFilledWineValue(whiteGrape));
	}
	public boolean hasRedWine()
	{
		return(leastRedWineValue()>0);
	}
	public boolean hasRedGrape()
	{	return(leastRedGrapeValue()>0);
	}
	public boolean hasWhiteWine()
	{
		return(leastWhiteWineValue()>0);
	}
	public boolean hasRoseWine()
	{
		return(leastRoseWineValue()>0);
	}
	public boolean hasChampaign()
	{
		return(leastChampaignValue()>0);
	}
	
	public int nRedGrapes()
	{	return(countof(redGrape));
	}
	private int countof(ViticultureCell ar[])
	{	int n=0;
		for(ViticultureCell c : ar) { if(c.height()>0) { n++; }}
		return(n);
	}
	
	public int nWhiteGrapes() { return(countof(whiteGrape)); }
	public int nRedWines() { return(countof(redWine)); }
	public int nWhiteWines() { return(countof(whiteWine)); }
	public int nRoseWines() { return(countof(roseWine)); }
	public int nChampaigns() { return(countof(champagne)); }
	public int nWines() { return(nRedWines()+nWhiteWines()+nRoseWines()+nChampaigns()); }
	
	// sum of blue and yellow cards
	public int nVisitorCards()
	{	int n=0;
		for(int lim=cards.height()-1; lim>=0; lim--) 
		{
			ViticultureChip card = cards.chipAtIndex(lim);
			if((card.type==ChipType.YellowCard) || (card.type==ChipType.BlueCard)) { n++; }
		}
		return(n);
	}
	public int nStructuresWithValueExactly(int val)
	{
		return(nStructuresWithValueAtLeast(val)-nStructuresWithValueAtLeast(val+1));
	}
	
	public int nStructuresWithValueAtLeast(int val)
	{	int n=0;
		for(ViticultureCell c : buildable)
			{
			ViticultureChip building = c.topChip();
			if(building!=null && c.cost>=val) { n++; }
			}
		for(ViticultureCell struct : structures)
			{
			ViticultureChip building = struct.height()>0 ? struct.chipAtIndex(0) : null;
			if(building!=null && building.costToBuild()>=val) { n++; }			
			}
		return(n);
	}
	
	public ViticultureCell buildableOrangeSlot()
	{
		for(ViticultureCell structure : bb.revision>=114 ? buildStructureCells : structures)
	 	{ 	ViticultureChip top = structure.topChip();
	 		if((top==null) 
	 				|| isEmptyField(structure)
	 				)
			{	return(structure);
			}
	 	}
		return(null);
	}
	// this version is incorrect and obsolete, but used before revision 109 
	public boolean canBuildStructureWithDiscount(int n)
	{	
		for(ViticultureCell c : buildable)
			{
			if((c.topChip()==null) && (c.cost<=n+cash)) { return(true); }
			}
		if(bb.revision>=145) { return(bb.addBuildOrangeMoves(null, this, n, 100)); }
		return(false);
	}
	public void changeResidual(int n)
	{	int newn = Math.max(0, Math.min(bb.residualTrack.length-1, residual+n));
		residual = newn;
	}
	public boolean hasBothCellars()
	{
		return((mediumCellar.topChip()!=null) && (largeCellar.topChip()!=null));
	}
	public boolean hasStructure(ViticultureChip ch)
	{
		for(ViticultureCell structure : structures)
		{
			if((structure.height()>0) && (structure.chipAtIndex(0)==ch)) { return(true); }
		}
		for(ViticultureCell field : fields)
		{
			if((field.height()>1) && (field.chipAtIndex(1)==ch)) { return(true); }
		}
		return(false);
	}
	public boolean hasAcademy()
	{
		return(hasStructure(ViticultureChip.Academy));
	}
	public boolean hasSilo()
	{
		return(hasStructure(ViticultureChip.Silo));
	}
	public boolean hasDock()
	{
		return(hasStructure(ViticultureChip.Dock));
	}
	
	public boolean hasWorkshop()
	{
		return(hasStructure(ViticultureChip.Workshop));
	}
	
	public boolean hasVeranda()
	{
		return(hasStructure(ViticultureChip.Veranda));
	}

	public boolean hasWineParlor()
	{
		return(hasStructure(ViticultureChip.WineParlor));
	}
	public boolean hasHarvestMachine()
	{
		return(hasStructure(ViticultureChip.HarvestMachine));
	}

	public boolean hasLabelFactory()
	{
		return(hasStructure(ViticultureChip.LabelFactory));
	}

	public boolean hasGazebo()
	{
		return(hasStructure(ViticultureChip.Gazebo));
	}
	public boolean hasBarn()
	{
		return(hasStructure(ViticultureChip.Barn));
	}
	public boolean hasCharmat()
	{	
		return(hasStructure(ViticultureChip.Charmat));
	}
	
	public boolean hasInn()
	{	
		return(hasStructure(ViticultureChip.Inn));
	}
	public boolean hasBanquetHall()
	{
		return(hasStructure(ViticultureChip.BanquetHall));
	}
	public boolean hasPenthouse()
	{
		return(hasStructure(ViticultureChip.Penthouse));
	}
	public boolean hasFountain()
	{
		return(hasStructure(ViticultureChip.Fountain));
	}
	public boolean hasStorehouse()
	{
		return(hasStructure(ViticultureChip.Storehouse));
	}
	public boolean hasStatue()
	{
		return(hasStructure(ViticultureChip.Statue));
	}

	public boolean hasTapRoom()
	{	
		return(hasStructure(ViticultureChip.TapRoom));
	}
	public boolean hasTavern()
	{	
		return(hasStructure(ViticultureChip.Tavern));
	}
	
	public boolean hasFermentationTank()
	{
		return(hasStructure(ViticultureChip.FermentationTank));
	}
	public boolean hasStudio()
	{
		return(hasStructure(ViticultureChip.Studio));
	}
	public boolean hasAqueduct()
	{
		return(hasStructure(ViticultureChip.Aqueduct));
	}
	public boolean hasMercado()
	{
		return(hasStructure(ViticultureChip.Mercado));
	}
	public boolean hasDistiller()
	{
		return(hasStructure(ViticultureChip.Distiller));
	}
	public boolean hasPatio()
	{
		return(hasStructure(ViticultureChip.Patio));
	}
	public boolean hasMediumCellar() { return(mediumCellar.topChip()!=null); }
	
	public boolean canFillWineOrder(ViticultureChip order)
	{	return(fillWineOrder(order,false,false,replayMode.Replay));
	}
	// parse a wine order, return true if we can fill it.
	public boolean fillWineOrder(ViticultureChip order,boolean bonus,boolean doit,replayMode replay)
	{	G.Assert(order.type==ChipType.PurpleCard, "needs a wine order card");
		StringTokenizer tok = new StringTokenizer(order.description);
		ViticultureCell rack[] = null;
		int off = 0;
		ViticultureCell used = null;	// we assume no more than 3 of each type
		ViticultureCell used2 = null;
		while(tok.hasMoreTokens())
		{	char cmd = G.CharToken(tok);
			int idx = G.IntToken(tok);
			switch(cmd)
			{
			case 'r' : 
				rack = redWine; off = 1; break;
			case 'w' : rack = whiteWine; off = 1; break;
			case 'b' : rack = roseWine; off = 4; break;
			case 'c' : rack = champagne; off = 7; break;
			case '=' : 
				int residual = G.IntToken(tok); 
				if(doit) 
					{
					 bb.changeScore(this,idx+(bonus?1:0),replay,FillWineOrder+(bonus?"+1":""),order,ScoreType.WineOrder);
					 bb.changeResidual(this,residual,replay);
					}
				return(true);
					
			default: ;
			}
			boolean found = false;
			for(int i=idx-off;i<rack.length;i++)
			{
				ViticultureCell candidate = rack[i];
				if(!found && (candidate!=used) && (candidate!=used2) && (candidate.topChip()!=null))
				{	used2 = used;
				    used = candidate;
					if(doit) 
						{ candidate.removeTop();
							if(replay.animate) 
							{
							bb.animationStack.push(candidate);
							bb.animationStack.push(bb.getCell(candidate.rackLocation(),'@',0));
							}
						}
					found = true;
				}
			}
			if(!found) { return(false); }
		}
		return(false);
	}
	
	// find items in selectedCards in the from deck, remove and claim them
	// the cards to be found are usually, but not always, the top chip
	public boolean findAndClaimSelected(ViticultureCell stack,replayMode replay,boolean reInsert)
	{	boolean some = false;
		for(int lim=selectedCards.size()-1; !some && lim>=0; lim--)
		{	CardPointer target = selectedCards.elementAt(lim);
			if(stack.rackLocation()==target.source)
			{
			// move the selected card
			selectedCards.remove(lim);
			G.Assert(stack.chipAtIndex(target.index)==target.card,"must match");
			stack.removeChipAtIndex(target.index);
			cards.addChip(target.card);
			if(reInsert) {  bb.replaceCard(stack,target.card); }
			some = true;
			if(replay.animate)
				{
				bb.animationStack.push(stack);
				bb.animationStack.push(cards);
				}
			}
		}
		return(some);
	}
	public double progressScore()
	{	double value = (score-MIN_SCORE);
/*		double balance = 1-Math.min(1,(double)(startingScore-MIN_SCORE)/(WINNING_SCORE-MIN_SCORE));

		double fakeScore = balance*(nPlantedFields()*2
									+ (nRedGrapes()+nWhiteGrapes())*0.5
									+ nRedWines()+nWhiteWines()
									+ nRoseWines()*2
									+ nChampaigns()*3);
*/
		return(value);
	}
	
	// count the stars in this cell that are ours
	public int countStars(ViticultureCell starCell)
	{	int n = 0;
		ViticultureChip star = getContent(stars);
		for(int lim=starCell.height()-1; lim>=0; lim--)
		{	if(starCell.chipAtIndex(lim)==star) { n++; }	
		}
		return(n);
	}
	public ViticultureState place2StarState()
	{
		return(stars.height()>0 ? ViticultureState.Place2Star : ViticultureState.Move2Star);
	}
	public ViticultureState placeStarState()
	{
		return(stars.height()>0 ? ViticultureState.Place1Star : ViticultureState.Move1Star);
	}
	public void changeScore(int n,String reason,ViticultureChip hint,ScoreType type)
	{ 	scoreString.append(n);
		scoreString.append(" ");
		scoreString.append(reason);
		scoreString.append('\n');
		score = Math.max(MIN_SCORE, Math.min(score+n,MAX_SCORE));	
    	scoreEvents.push(new ScoreEvent(bb.season(this),bb.year,n,score,reason,hint,type));
	}
	public void recordEvent(String reason,ViticultureChip hint,ScoreType type)
	{
		scoreEvents.push(new ScoreEvent(bb.season(this),bb.year,0,score,reason,hint,type));
	}
	// count soldatos in a row containing this cell, or a single cell
	public int nOpponentSoldato(ViticultureCell cell)
	{	int n = 0;
		if(cell.parentRow!=null)
			{ for(ViticultureCell c : cell.parentRow) 
				{ // note that this logic has to agree with bb.paySoldatos()
				  // soldatos in the grande position do not collect tolls
				  if((bb.revision<149) || (c.row!=GrandeExtraRow))
					{ n+= nOpponentSoldatoCell(c); 
					}
				}
			} 
		return(n);
	}
	//count soldatos in a single cell
	private int nOpponentSoldatoCell(ViticultureCell c)
	{	int n=0;
		for(int lim=c.height()-1; lim>=0; lim--)
		{	ViticultureChip chip = c.chipAtIndex(lim);
			if((chip.type==ChipType.Soldato) && (chip.color!=color)) { n++; }
		}
		return(n);
	}
	// true if we have a worker,or the gray worker we own, in this row
	public boolean hasWorkerInRow(ViticultureCell row[])
	{	if(row!=null)
		{ for(ViticultureCell c : row)
			{	for(int lim=c.height()-1; lim>=0; lim--)
				{
				ViticultureChip worker = c.chipAtIndex(lim);
				if(isMyWorker(worker))
					{	return(true);
					}
				}
		}}
		return(false);
	}
	// innkeeper stealing a card, type = 0 for yellow or blue, 1 for yellow, 2 for blue
	ViticultureChip takeRandomVisitorCard(Random r,int type)
	{   G.Assert(hasCard(ChipType.YellowCard)||hasCard(ChipType.BlueCard),"no visitor cards");
		ViticultureChip card = null;		
		int h = cards.height();
		do 
		{ card = cards.chipAtIndex(r.nextInt(h));
		} 
		while( type==0 ? (card.type!=ChipType.YellowCard) && (card.type!=ChipType.BlueCard)
				: type==1 ? (card.type!=ChipType.YellowCard) : (card.type!=ChipType.BlueCard));
		cards.removeChip(card);
		return(card);
	}
	boolean isEmptyField(ViticultureCell c)
	 {	ViticultureChip top = c.topChip();
	 	return ((top!=null)
	 				&& (top.type==ChipType.Field)	// find a vacant structure cell or field
					&& (vines[c.row].height()==0));
	 }
	//
	// calculate the cost of selected cards in the market.
	// for now, there are 1 or 2 free cards and the rest cost $1
	//
	public int committedCost()
	{
		int nfree = bb.resetState.nFree();
		int cost = 0;
		int ntotal = oracleCards.height();
		for(int i=0,lim=selectedCards.size();  i<lim; i++)
		{	// the top nfree cards in oracleCards are free
			CardPointer item = selectedCards.elementAt(i);
			if(item.index+nfree<ntotal) { cost++; }
		}
		return cost;
	}
	
	public void addWorker(ViticultureCell dest, ViticultureChip worker) {
		dest.addChip(worker);
		nWorkers++;
		switch(worker.type) {
		case Worker:
		case GrandeWorker:
			break;
		default:
			if(extraWorker1==null) { extraWorker1 = worker.type; }
			else if(extraWorker2==null) { extraWorker2 = worker.type; }
		}
		
	}

}