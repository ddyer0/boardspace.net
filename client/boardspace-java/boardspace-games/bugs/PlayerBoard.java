package bugs;

import lib.CompareTo;
import lib.G;
import lib.Random;
import online.game.CommonMoveStack;
import online.game.replayMode;

import static bugs.BugsMovespec.*;

import java.awt.Color;

public class PlayerBoard implements BugsConstants,CompareTo<PlayerBoard>
{
	BugsChip chip;
	Color backgroundColor;
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
	private int actualScore;
	public void changeScore(int n)
	{
		if(!resigned) { actualScore += n; }
	}
	CellStack selectedCells = new CellStack();
	int boardIndex = -1;
	int turnOrder = -1;
	boolean resigned = false;
	
	PlayerBoard(BugsBoard b,int idx)
	{	// constants
		parent = b;
		boardIndex = idx;
		// variable
		cell = new BugsCell(parent,BugsId.PlayerChip,(char)('A'+idx));
		bugs = new BugsCell(parent,BugsId.PlayerBugs,(char)('A'+idx));
		goals = new BugsCell(parent,BugsId.PlayerGoals,(char)('A'+idx));
	}
	public void setChip(BugsChip ch,Color bg)
	{
		chip = ch;
		cell.reInit();
		cell.addChip(ch);
		backgroundColor = bg;
	}
	public boolean doneState()
	{	switch(uiState)
		{
		case Confirm:
		case Resign:
		case Pass:
			return true;
		case Normal:
		case Ready:
			return false;
		default: throw G.Error("Not expecing uistate %s",uiState);
		}
	}
	public void doInit() 
	{	actualScore = STARTING_POINTS;
		uiState = UIState.Normal;
		acceptPlacement();
		lastPicked = null;
		progress = false;
		resigned = false;
		lastDroppedObject = null;
		turnOrder = boardIndex;
		bugs.reInit();
		goals.reInit();
		selectedCells.clear();
	}
	public int getScore() { return actualScore; }
	public void copyFrom(PlayerBoard other) 
	{	actualScore = other.actualScore;
		resigned = other.resigned;
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
	{	G.Assert(actualScore==other.actualScore,"score mismatch");
		G.Assert(resigned==other.resigned,"resigned mismatch");
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
		v ^= parent.Digest(r,actualScore);
		v ^= parent.Digest(r,resigned);
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
		case BugMarket:
		case PlayerBugs: return bugs;
		case GoalMarket:
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
	
	public void addPurchaseMoves(CommonMoveStack all)
	{	if(pickedObject!=null)
		{
		if(pickedObject.isGoalCard())
			{
			addPurchaseMoves(all,MOVE_DROP,pickedSource,(GoalCard)pickedObject);
			}
		else if(pickedObject.isBugCard())
			{
			addPurchaseMoves(all,MOVE_DROP,pickedSource,(BugCard)pickedObject);
			}
		}
		else
		{
			int op = parent.robot==null ? MOVE_PICK : MOVE_TO_PLAYER;
			if(goals.height()<HAND_LIMIT)
			{
			BugsCell goals[] = parent.goalMarket;
			for(int i=0;i<goals.length;i++)
				{
				GoalCard ch = (GoalCard)goals[i].topChip();
				if(ch!=null) { addPurchaseMoves(all,op,goals[i],ch); }
				}
			}
			if(bugs.height()<HAND_LIMIT)
			{
			BugsCell bugs[] = parent.bugMarket;
			for(int i=0;i<bugs.length;i++)
				{
				BugCard ch = (BugCard)bugs[i].topChip();
				if(ch!=null) { addPurchaseMoves(all,op,bugs[i],ch); }
				}
			}
		}
	}
	
	public boolean addPurchaseMoves(CommonMoveStack all,int op,BugsCell from,BugCard bug)
	{
		if(all==null) { return true; }
		all.push(new BugsMovespec(op,from,bug,bugs,boardIndex));
		return true;
	}
	public boolean addPurchaseMoves(CommonMoveStack all,int op,BugsCell from,GoalCard bug)
	{
		if(all==null) { return true; }
		all.push(new BugsMovespec(op,from,bug,bugs,boardIndex));
		return true;
	}

	public boolean hasBonusMoves(BugsChip chip)
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
		if(parent.nonWildBonusPointsForPlayer(goal,cell,chip)>0)
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
		if(cell==pickedSource && pickedObject!=null) { return true; }
		switch(cell.rackLocation())
		{
		case PlayerGoals:
			return (pickedObject!=null && pickedObject.isGoalCard()) || hasBonusMoves(chip);
			
		case PlayerBugs:
			return (pickedObject!=null && pickedObject.isBugCard()) || hasBugMoves(chip);
			
		default: return true;
		}
	}
	public boolean hasBugMoves(BugsChip chip)
	{	if(chip.isBugCard())
		{
		return parent.addMovesOnBoard(null,MOVE_DROPB,bugs,(BugCard)chip,this);
		}
		return false;
	}
	public void addMovesOnBoard(CommonMoveStack all)
	{
		for(int lim=bugs.height()-1; lim>=0; lim--)
		{
			parent.addMovesOnBoard(all,parent.robot==null ? MOVE_PICK : MOVE_TO_BOARD,bugs,(BugCard)bugs.chipAtIndex(lim),this);
		}
	}
	public void addMovesInBoard(CommonMoveStack all)
	{
		parent.addMovesInBoard(all,this);
	}
	public void getListOfMoves(CommonMoveStack all,BugsChip po) 
	{
		BugsState state = parent.board_state;
		{
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
			case Resign:
				break;
			default:
				throw G.Error("uiState %s not expected",uiState);
			}			
			break;
		case Play:
		case SequentialPlay:
			switch(uiState)
			{
			default: throw G.Error("not expecting uistate %s",uiState);
			case Ready:
				break;
			case Pass:
				all.push(new BugsMovespec(MOVE_DONE,boardIndex));
				break;
			case Confirm: 
			case Resign:
				all.push(new BugsMovespec(MOVE_DONE,boardIndex));
				if(parent.robot!=null
						&& !parent.rotated 
						&& droppedDest.onBoard
						&& droppedObject.isBugCard())
				{
					all.push(new BugsMovespec(MOVE_ROTATECW,droppedDest,null,droppedDest,boardIndex));
					all.push(new BugsMovespec(MOVE_ROTATECCW,droppedDest,null,droppedDest,boardIndex));
				}
				break;
			case Normal:
				if(pickedObject!=null)
				{
					switch(parent.variation)
					{
					default: throw G.Error("Not expecting variation %s",parent.variation);
					case bugspiel_parallel:
					case bugspiel_parallel_large:
						parent.addMovesOnBoard(all,MOVE_DROPB,bugs,(BugCard)pickedObject,this);
						break;
					case bugspiel_sequential:
					case bugspiel_sequential_large:
						switch(pickedSource.rackLocation())
						{
						case PlayerGoals:
							addBonusMoves(all);
							break;
						case BoardLocation:
						case PlayerBugs:
							parent.addMovesOnBoard(all,MOVE_DROPB,bugs,(BugCard)pickedObject,this);
							break;
						case GoalMarket:
						case BugMarket:
							addPurchaseMoves(all);
							break;
						default: throw G.Error("Not expecting from %s",pickedSource);
						}
					}		
				}
				else
				{
				switch(parent.variation)
				{
				default: throw G.Error("Not expecting variation %s",parent.variation);
				case bugspiel_parallel:
				case bugspiel_parallel_large:
					addMovesOnBoard(all);
					addMovesInBoard(all);
					all.push(new BugsMovespec(MOVE_READY,boardIndex));
					break;
				case bugspiel_sequential:
				case bugspiel_sequential_large:
					addMovesOnBoard(all);
					addMovesInBoard(all);
					addBonusMoves(all); 
					if(parent.robot==null || !parent.robot.randomPhase)
						{
						addPurchaseMoves(all);
						}
					if(parent.robot==null || parent.somePassed() || all.size()==0) 
						{ // the robot doesn't consider passing unless someone else is also
						all.push(new BugsMovespec(MOVE_PASS,boardIndex)); 
						}
					
				}
				
				}
			}
			break;

		case Purchase:
		default:
			G.Error("Not expecting %s",state);
		}}
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
    	//G.print("t "+c.topChip().chipNumber()," ",ch.chipNumber());
    	parent.p1(pickedIndex>=0,"%s should be there",ch);
    	c.removeChipAtIndex(pickedIndex);
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
    	droppedDest = null;
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
    	pickedSource = null;
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
        case GoalMarket:
        case BugMarket:
        case BoardTopLocation:
        case BoardBottomLocation:
        case BoardLocation:	// already filled board slot, which can happen in edit mode
        	c.addChip(pickedObject);
        	droppedObject = pickedObject;   
        	c.xPlayer = c.player; 
  		  	c.player = chip;
        	if(droppedObject.isBugCard() && parent.board_state==BugsState.Play) 
        		{ 
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
    	case SequentialPlay:
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
            	setNextStateAfterDrop();
            	if(parent.board_state==BugsState.Puzzle) { acceptPlacement(); }
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
        	if(parent.board_state==BugsState.Puzzle) { acceptPlacement(); }
 			}
    	 }
         break;
        case MOVE_TO_PLAYER:
    		{
    			BugsCell src = parent.getCell(m.source,m.from_col,m.from_row);
    			switch(src.rackLocation()) 
    			{
    			case BugMarket:
    				pickObject(src,m.chip);
    				dropObject(bugs);
    				setNextStateAfterDrop();
    				parent.animate(replay,src,bugs);
    				if(parent.board_state==BugsState.Puzzle) { acceptPlacement(); }
    				break;
    			case GoalMarket:
    				pickObject(src,m.chip);
    				dropObject(goals);
    				setNextStateAfterDrop();
    				parent.animate(replay,src,goals);
    				if(parent.board_state==BugsState.Puzzle) { acceptPlacement(); }
    				break;
    			default: G.Error("Not expecting ",src);
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
        	if(parent.board_state==BugsState.Puzzle) { acceptPlacement(); }
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
		case MOVE_PASS:
			setUIState(uiState==UIState.Pass ? UIState.Normal : UIState.Pass);
			break;
		case MOVE_RESIGN:
			setUIState(uiState==UIState.Resign?UIState.Normal:UIState.Resign);
			break;
		default:
			parent.cantExecute(m);
		}
	}
	public void setOurTurn()
	{
		if(uiState==UIState.Pass) { uiState=UIState.Normal; }
	}
	public boolean isSelected(BugsCell c) {
		return selectedCells.contains(c);
	}

	// give this player a copy of all the bugs and goalMarket they selected
	// and collect the costs
	public void doPurchases(replayMode replay) {
		while(selectedCells.size()>0)
		{
			BugsCell c = selectedCells.pop();
			BugsId rack = c.rackLocation();
			switch(rack)
			{
			case GoalMarket:
				{
				GoalCard ch = (GoalCard)c.topChip();
				goals.addChip(ch);
				parent.animate(replay,c,goals);
				changeScore(-c.cost);
				c.purchased = true;
				}
				break;
			case BugMarket:
				{
				BugCard ch = (BugCard)c.topChip();
				bugs.addChip(ch);
				parent.animate(replay,c,bugs);
				changeScore(-c.cost);
				c.purchased = true;
				}
				break;
			default: G.Error("Not expecting %s",rack);
			}
			
		}
		
	}
	// used setting turn order
	public int compareTo(PlayerBoard o) {
		return G.signum((getScore()-o.getScore())*1000 + (turnOrder-o.turnOrder));
	}
	public void doDone(replayMode replay) {
		switch(uiState)
		{
		case Confirm:
			{switch(parent.board_state)
			{
			case Gameover:
				setUIState(UIState.Normal);
				break;
			case Bonus:
				{
				BugsCell cell = droppedDest;
				parent.scoreBonusForPlayers(this,cell,replay);
				setUIState(UIState.Normal);
				acceptPlacement();
				}
				break;
			case Play:
			case SequentialPlay:
				BugsId rack = droppedDest.rackLocation();
				switch(rack)
				{
				default: throw G.Error("not expecting %s",rack);
				case PlayerGoals:
					G.Assert(pickedSource.rackLocation()==BugsId.GoalMarket,"should be a goal");
					{
						int cost = COSTS[pickedSource.row];
						changeScore(-cost);
					}
					break;
				case PlayerBugs:
					G.Assert(pickedSource.rackLocation()==BugsId.BugMarket,"should be a bug");
					{
						int cost = COSTS[pickedSource.row];
						changeScore(-cost);
					}
					
					break;
				case BoardLocation:
				// finished play of a bug
					if(droppedObject.isGoalCard())
					{
						BugsCell cell = droppedDest;
						parent.scoreBonusForPlayers(this,cell,replay);
					}
					else
					{
					BugCard ch = (BugCard)droppedObject;
					if(pickedSource.rackLocation()!=BugsId.BoardLocation) 
						{ changeScore(ch.pointValue()); parent.progress++; }
					if(ch.profile.isPredator() && droppedDest.height()>1)
					{
						BugCard rem = (BugCard)droppedDest.removeChipAtIndex(0);
						parent.bugDiscards.addChip(rem);
						parent.animate(replay,droppedDest,parent.bugDiscards);
					}
				}}
				setUIState(UIState.Normal);
				acceptPlacement();
				break;
			default: G.Error("parent state %s Not expected",parent.board_state);
			}}
			break;
		case Pass:
			break;
		case Resign:
			resigned = true;
			setUIState(UIState.Resign);
			if(parent.playersLeft()<=1) { parent.setGameOver(); }
			break;
		default:
			G.Error("uiState %s not expected", uiState);
			break;
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
	public boolean canPass() {
		return (uiState==UIState.Pass) || (uiState==UIState.Normal);
	}
}
