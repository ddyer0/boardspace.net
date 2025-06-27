package bugs;

import lib.CompareTo;
import lib.G;
import lib.Random;
import online.game.CommonMoveStack;
import online.game.replayMode;

import static bugs.BugsMovespec.*;

public class PlayerBoard implements BugsConstants,CompareTo<PlayerBoard>
{
	BugsChip chip;
	BugsCell cell;
	BugsBoard parent;
	BugsCell bugs;
	BugsCell goals;
	BugsChip pickedObject = null;
	BugsChip droppedObject = null;
	int pickedIndex = 0;
	BugsCell pickedSource = null;
	BugsCell droppedDest = null;
	UIState dropState = null;
	BugsChip lastPicked = null;
	BugsChip lastDroppedObject = null;
	boolean progress = false;
	UIState uiState = UIState.Normal;
	int score;
	CellStack selectedCells = new CellStack();
	int boardIndex = -1;
	int turnOrder = -1;
	PlayerBoard(BugsBoard b,int idx)
	{	// constants
		parent = b;
		boardIndex = idx;
		// variable
		cell = new BugsCell(parent,BugsId.PlayerChip,(char)('A'+idx));
		bugs = new BugsCell(parent,BugsId.PlayerBugs,(char)('A'+idx));
		goals = new BugsCell(parent,BugsId.PlayerGoals,(char)('A'+idx));
	}
	public void setChip(BugsChip ch)
	{
		chip = ch;
		cell.reInit();
		cell.addChip(ch);
	}
	public boolean doneState()
	{
		return uiState==UIState.Confirm;
	}
	public void doInit() 
	{	score = STARTING_POINTS;
		uiState = UIState.Normal;
		acceptPlacement();
		lastPicked = null;
		progress = false;
		lastDroppedObject = null;
		turnOrder = boardIndex;
		bugs.reInit();
		goals.reInit();
		selectedCells.clear();
	}
	public int getScore() { return score; }
	public void copyFrom(PlayerBoard other) 
	{	score = other.score;
		pickedObject = other.pickedObject;
		droppedObject = other.droppedObject;
		droppedDest = parent.getCell(other.droppedDest);
		pickedSource = parent.getCell(other.pickedSource);
		pickedIndex = other.pickedIndex;
		lastDroppedObject = other.lastDroppedObject;
		lastPicked = other.lastPicked;
		progress = other.progress;
		dropState = other.dropState;
		turnOrder = other.turnOrder;
		uiState = other.uiState;
		bugs.copyFrom(other.bugs);
		cell.copyFrom(other.cell);	// copy so the screen coordinates are copied
		goals.copyFrom(other.goals);
		parent.getCell(selectedCells,other.selectedCells);
	}
	
	public void sameBoard(PlayerBoard other) 
	{	G.Assert(score==other.score,"score mismatch");
		G.Assert(uiState==other.uiState,"uiState mismatch");
		bugs.sameContents(other.bugs);
		G.Assert(pickedIndex == other.pickedIndex,"pickedIndex mismatch");
		G.Assert(turnOrder==other.turnOrder,"turnOrder mismatch");
		goals.sameContents(other.goals);
		G.Assert(dropState==other.dropState,"dropState mismatch");
		G.Assert(progress==other.progress,"progress mismatch");
		G.Assert(parent.sameCells(pickedSource,other.pickedSource),"pickedSource mismatch");
		G.Assert(parent.sameCells(droppedDest,other.droppedDest),"droppedDest mismatch");
		G.Assert(pickedObject==other.pickedObject,"pickedObject mismatch");
		G.Assert(droppedObject==other.droppedObject,"droppedObject mismatch");
	
		G.Assert(parent.sameCells(selectedCells,(other.selectedCells)),"selectedCells mismatch");
	}
	
	public long Digest(Random r) 
	{
		long v = 0;
		v ^= parent.Digest(r,score);
		v ^= uiState.Digest(r);
		if(dropState!=null) { v ^= dropState.Digest(r);}
		v ^= bugs.Digest(r);
		v ^= parent.Digest(r,turnOrder);
		v ^= goals.Digest(r);
		v ^= parent.Digest(r,pickedIndex);
		v ^= parent.Digest(r,pickedObject);
		v ^= parent.Digest(r,droppedObject);
		v ^= parent.Digest(r,progress);
		v ^= parent.Digest(r,pickedSource);
		v ^= parent.Digest(r,droppedDest);
		v ^= parent.Digest(r,selectedCells);
		return v;
	}
	
	public BugsCell getCell(BugsId rack,char c,int r)
	{
		switch(rack)
		{
		case PlayerChip: return cell;
		case PlayerBugs: return bugs;
		case PlayerGoals: return goals;
		default:
			throw G.Error("not expecting %s",rack);
		}
	}
	public void addBonusMoves(CommonMoveStack all)
	{	if(pickedObject!=null)
		{
		addBonusMoves(all,(GoalCard)pickedObject);
		}
		else
		{
		for(int lim=goals.height()-1; lim>=0; lim--)
		{
		addBonusMoves(all,(GoalCard)goals.chipAtIndex(lim));
		}
		}
	}
	
	public boolean hasBonusMoves(BugsCell cell,BugsChip chip)
	{	if(chip.isGoalCard()) 
			{
			return addBonusMoves(null,(GoalCard)chip);
			}
		return false;
	}
	
	public boolean addBonusMoves(CommonMoveStack all,GoalCard goal)
	{	boolean some = false;
		for(BugsCell cell = parent.allCells; cell!=null; cell = cell.next)
		{
			if(addBonusMoves(all,goal,cell))
			{
				if(all==null) { return true; }
				some = true;
			}
			if(addBonusMoves(all,goal,cell.above))
			{
				if(all==null) { return true; }
				some = true;
			}
			if(addBonusMoves(all,goal,cell.below))
			{
				if(all==null) { return true; }
				some = true;
			}

		}
		return some;
	}
	public boolean addBonusMoves(CommonMoveStack all,GoalCard goal,BugsCell cell)
	{	boolean some = false;
		if(parent.bonusPointsForPlayer(goal,cell,chip)>0)
		{
			if(all==null) { return true; }
			some = true;
			if(pickedObject==null) 
			{
				all.push(new BugsMovespec(parent.robot==null ? MOVE_PICK : MOVE_TO_BOARD,goals,goal,cell,boardIndex));
			}
			else 
			{
				all.push(new BugsMovespec(MOVE_DROPB,goals,goal,cell,boardIndex));
			}
		}
		return some;
	}
	public boolean canPlayCard(BugsCell cell, BugsChip chip)
	{
		switch(cell.rackLocation())
		{
		case PlayerGoals:
			return hasBonusMoves(cell,chip);
			
		case PlayerBugs:
			return hasBugMoves(cell,chip);
			
		default: return true;
		}
	}
	public boolean hasBugMoves(BugsCell cell,BugsChip chip)
	{	if(chip.isBugCard())
		{
		return parent.addMovesOnBoard(null,MOVE_DROPB,bugs,(BugCard)chip,boardIndex);
		}
		return false;
	}
	public void addMovesOnBoard(CommonMoveStack all)
	{
		for(int lim=bugs.height()-1; lim>=0; lim--)
		{
			parent.addMovesOnBoard(all,parent.robot==null ? MOVE_PICK : MOVE_TO_BOARD,bugs,(BugCard)bugs.chipAtIndex(lim),boardIndex);
		}
	}
	public void addMovesInBoard(CommonMoveStack all)
	{
		parent.addMovesInBoard(all,this);
	}
	public void getListOfMoves(CommonMoveStack all,BugsChip po) 
	{
		BugsState state = parent.board_state;
		switch(state)
		{
		case Puzzle:
			parent.addPickDrop(all,bugs,po,boardIndex);
			parent.addPickDrop(all,goals,po,boardIndex);
			break;
		case Bonus:
			switch(uiState)
			{
			case Normal:
				addBonusMoves(all); 
				if(pickedObject==null) { all.push(new BugsMovespec(MOVE_READY,boardIndex)); }
				break;
			case Confirm:
				all.push(new BugsMovespec(MOVE_DONE,boardIndex));
				break;
			case Ready:
				break;
			default:
				throw G.Error("not expected");
			}			
			break;
		case Play:
			switch(uiState)
			{
			default: throw G.Error("not expecting uistate %s",uiState);
			case Ready:
				break;
			case Confirm: 
				all.push(new BugsMovespec(MOVE_DONE,boardIndex));
				if(parent.robot!=null && !parent.rotated)
				{
					all.push(new BugsMovespec(MOVE_ROTATECW,droppedDest,null,droppedDest,boardIndex));
					all.push(new BugsMovespec(MOVE_ROTATECCW,droppedDest,null,droppedDest,boardIndex));
				}
				break;
			case Normal:
				if(pickedObject!=null)
				{
					parent.addMovesOnBoard(all,MOVE_DROPB,bugs,(BugCard)pickedObject,boardIndex);
				}
				else
				{
				addMovesOnBoard(all);
				addMovesInBoard(all);
				all.push(new BugsMovespec(MOVE_READY,boardIndex));
				}
			}
			break;
		case Purchase:
		default:
			G.Error("Not expecting %s",state);
		}
	}

	
	public void setUIState(UIState t)
	{
		uiState= t;
	}
	
	public boolean isReady() { return uiState==UIState.Ready; }
	public boolean isSource(BugsCell c)
	{
		return c==pickedSource;
	}
	public boolean isDest(BugsCell c)
	{
		return c==droppedDest;
	}
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(BugsCell c,BugsChip ch)
    {	pickedSource = c;
    	pickedIndex = c.findChip(ch);
    	BugsChip rem = c.removeChipAtIndex(pickedIndex);
    	G.Assert(rem!=null,"should be there");
    	lastPicked = pickedObject = ch;
    	droppedObject = null;
    	lastDroppedObject = null;
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private BugsCell unDropObject()
    {	BugsCell rv = droppedDest;
    	pickedObject = droppedObject;
    	rv.removeChip(droppedObject);
    	droppedObject = null;
    	rv.player = rv.xPlayer;
    	rv.placedInRound = rv.xPlacedInRound;
    	setUIState(dropState);
    	return(rv);
    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	BugsCell rv = pickedSource;
    	rv.insertChipAtIndex(pickedIndex,pickedObject);
    	pickedIndex = -1;
    	pickedObject = null;
    	droppedObject = null;
    }
    
    // 
    // drop the floating object.
    //
    private void dropObject(BugsCell c)
    {
       droppedDest = c;
       dropState = uiState;
       switch (c.rackLocation())
        {
        default:
        	throw G.Error("Not expecting dest " + c.rackLocation);
        case PlayerGoals:
        case PlayerBugs:
        case ActiveDeck:
        case Goal:
        case Market:
        case BoardTopLocation:
        case BoardBottomLocation:
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        	c.addChip(pickedObject);
        	droppedObject = pickedObject;     	
        	if(droppedObject.isBugCard()) 
        		{ c.xPlayer = c.player; 
        		  c.player = chip; 
        		  c.xPlacedInRound = c.placedInRound;
        		  c.placedInRound = parent.roundNumber;
        		}
        	pickedObject = null;
            break;

        }
     }
    //
    // accept the current placements as permanent
    //
    public void acceptPlacement()
    {	
        droppedDest = null;
        dropState = null;
        pickedSource = null;
        pickedObject = null;
        droppedObject = null;
     }
    public void setNextStateAfterDrop()
    {
    	switch(parent.board_state)
    	{
    	case Bonus:
    	case Play:	
    		setUIState(UIState.Confirm);
    		break;
    	default: break;
    	}
    }
    public void findPickedObject()
    {
    	if(pickedObject==null)
    	{
    		parent.findPickedObject(this);
    	}
    }
	public void Execute(BugsMovespec m, replayMode replay) 
	{
		switch(m.op)
		{

        case MOVE_DROP: // drop on chip pool;
        	findPickedObject();
        	if(pickedObject!=null)
        	{
            BugsCell dest = parent.getCell(m.source,m.to_col,m.to_row);
            if(isSource(dest)) { unPickObject(); }
            else 
            	{
		        if(replay==replayMode.Live)
	        	{ lastDroppedObject = pickedObject.getAltDisplayChip(dest);
	        	  //G.print("last ",lastDroppedObject); 
	        	}      	
            	dropObject(dest); 
            
            	}
        	}
            break;
 

        case MOVE_DROPB:
    	{
    	findPickedObject();
		BugsChip po = pickedObject;
		BugsCell dest =  parent.getCell(BugsId.BoardLocation,m.to_col,m.to_row);
		if(isSource(dest)) 
			{ unPickObject(); 
			}
			else 
			{
			m.chip = pickedObject;
	           
            dropObject(dest);
            /**
             * if the user clicked on a board space without picking anything up,
             * animate a stone moving in from the pool.  For Hex, the "picks" are
             * removed from the game record, so there are never picked stones in
             * single step replays.
             */
            if(replay.animate && (po==null))
            	{ parent.animate(replay,pickedSource,dest);
             	}
            setNextStateAfterDrop();
 			}
    	 }
         break;
         
        case MOVE_TO_BOARD:
        	{  	
        	BugsCell src = parent.getCell(m.source,m.from_col,m.from_row);
        	BugsCell to = parent.getCell(m.to_col,m.to_row);
        	pickObject(src,m.chip);
        	dropObject(to);
        	setNextStateAfterDrop();
        	parent.animate(replay,src,to);
        	}
        	break;
		case MOVE_PICK:
		case MOVE_PICKB:
			{BugsCell src = parent.getCell(m.source,m.from_col,m.from_row);
 			if(isDest(src)) { unDropObject(); }
 			else
 			{
        	// be a temporary p
        	pickObject(src,m.chip);
 			}}
			break;
		case MOVE_SELECT:
			{
			BugsCell c = parent.getCell(m.source,m.to_col,m.to_row);
			BugsCell rem = selectedCells.remove(c,false);
			if(rem==null) { selectedCells.push(c); }
			setUIState(UIState.Normal);
			}
			break;
		case MOVE_READY:
			setUIState(uiState==UIState.Ready ? UIState.Normal : UIState.Ready);
			break;
		default:
			parent.cantExecute(m);
		}
	}

	public boolean isSelected(BugsCell c) {
		return selectedCells.contains(c);
	}

	// give this player a copy of all the bugs and goals they selected
	// and collect the costs
	public void doPurchases(replayMode replay) {
		while(selectedCells.size()>0)
		{
			BugsCell c = selectedCells.pop();
			BugsId rack = c.rackLocation();
			switch(rack)
			{
			case Goal:
				{
				GoalCard ch = (GoalCard)c.topChip();
				goals.addChip(ch);
				parent.animate(replay,c,goals);
				score -= c.cost;
				c.purchased = true;
				}
				break;
			case Market:
				{
				BugCard ch = (BugCard)c.topChip();
				bugs.addChip(ch);
				parent.animate(replay,c,bugs);
				score -= c.cost;
				c.purchased = true;
				}
				break;
			default: G.Error("Not expecting %s",rack);
			}
			
		}
		
	}
	// used setting turn order
	public int compareTo(PlayerBoard o) {
		return G.signum((score-o.score)*1000 + (turnOrder-o.turnOrder));
	}
	public void doDone(replayMode replay) {
		switch(parent.board_state)
		{
		case Bonus:
			{
			BugsCell cell = droppedDest;
			parent.scoreBonusForPlayers(this,cell,replay);
			setUIState(UIState.Normal);
			acceptPlacement();
			}
			break;
		case Play:
			// finished play of a bug
			BugCard ch = (BugCard)droppedObject;
			if(!pickedSource.onBoard) { score += ch.pointValue(); parent.progress++; }
			setUIState(UIState.Normal);
			acceptPlacement();
			break;
		default: G.Error("Not expected");
		}
		
	}
	 public void getUIMoves(CommonMoveStack all)
	 {	 	
	 	if((pickedObject!=null) && (pickedSource!=null))
	 		{
	 			all.push(new  BugsMovespec(pickedSource.onBoard ? MOVE_DROPB : MOVE_DROP,pickedSource,pickedObject,pickedSource,boardIndex));
	 		}
	 	if((droppedObject!=null) && (droppedDest!=null))
			{
			all.push(new BugsMovespec(droppedDest.onBoard ? MOVE_PICKB : MOVE_PICK,droppedDest,droppedObject,droppedDest,boardIndex));
			}
	 }
}
