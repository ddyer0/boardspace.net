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
	
	UIState uiState = UIState.Normal;
	int score;
	CellStack selectedCells = new CellStack();
	int boardIndex = -1;
	int turnOrder = -1;
	PlayerBoard(BugsBoard b,BugsChip ch,int idx)
	{	// constants
		parent = b;
		chip = ch;
		boardIndex = idx;
		// variable
		cell = new BugsCell(parent,BugsId.PlayerChip,(char)('A'+idx));
		cell.addChip(ch);
		bugs = new BugsCell(parent,BugsId.PlayerBugs,(char)('A'+idx));
		goals = new BugsCell(parent,BugsId.PlayerGoals,(char)('A'+idx));
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
		lastDroppedObject = null;
		turnOrder = -1;
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

	public void getListOfMoves(CommonMoveStack all,int who) 
	{
		BugsState state = parent.board_state;
		switch(state)
		{
		case Puzzle:
			parent.addPickDrop(all,bugs,pickedObject,who);
			parent.addPickDrop(all,goals,pickedObject,who);
			break;
		case Bonus:
			break;
		case Play:
			switch(uiState)
			{
			default: throw G.Error("not expecting uistate %s",uiState);
			case Ready:
			case Confirm: break;
			case Normal:
				if(pickedObject!=null)
				{
					parent.addMovesOnBoard(all,MOVE_DROPB,bugs,(BugCard)pickedObject,boardIndex);
				}
				else
				for(int lim=bugs.height()-1; lim>=0; lim--)
				{
					parent.addMovesOnBoard(all,MOVE_PICK,bugs,(BugCard)bugs.chipAtIndex(lim),boardIndex);
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
        	c.xPlayer = c.player;
        	c.player = chip;
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
    	case Play:	
    		setUIState(UIState.Confirm);
    		break;
    	default: break;
    	}
    }
	public void Execute(BugsMovespec m, replayMode replay) 
	{
		switch(m.op)
		{

        case MOVE_DROP: // drop on chip pool;
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
		case Play:
			// finished play of a bug
			BugCard ch = (BugCard)droppedObject;
			if(!pickedSource.onBoard) { score += ch.pointValue(); }
			setUIState(UIState.Normal);
			acceptPlacement();
			break;
		default: G.Error("Not expected");
		}
		
	}
	 public void getUIMoves(CommonMoveStack all,int player)
	 {	 	
	 	if((pickedObject!=null) && (pickedSource!=null))
	 		{
	 			all.push(new  BugsMovespec(pickedSource.onBoard ? MOVE_DROPB : MOVE_DROP,pickedSource,pickedObject,pickedSource,player));
	 		}
	 	if((droppedObject!=null) && (droppedDest!=null))
			{
			all.push(new BugsMovespec(droppedDest.onBoard ? MOVE_PICKB : MOVE_PICK,droppedDest,droppedObject,droppedDest,player));
			}
	 }
}
