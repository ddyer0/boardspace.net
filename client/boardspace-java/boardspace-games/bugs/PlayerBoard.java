package bugs;

import lib.G;
import lib.Random;
import online.game.CommonMoveStack;

public class PlayerBoard implements BugsConstants
{
	BugsId color;
	BugsChip chip;
	BugsBoard parent;
	BugsCell bugs;
	BugsCell goals;
	UIState uiState = UIState.Normal;
	int score;
	CellStack selectedCells = new CellStack();
	int boardIndex = -1;
	PlayerBoard(BugsBoard b,BugsId col,BugsChip ch,int idx)
	{	// constants
		parent = b;
		color = col;
		chip = ch;
		boardIndex = idx;
		// variable
		bugs = new BugsCell(BugsId.PlayerBugs,(char)('A'+idx));
		goals = new BugsCell(BugsId.PlayerGoals,(char)('A'+idx));
	}
	
	public void doInit() 
	{	score = STARTING_POINTS;
		uiState = UIState.Normal;
		bugs.reInit();
		goals.reInit();
		selectedCells.clear();
	}
	public int getScore() { return score; }
	public void copyFrom(PlayerBoard other) 
	{	score = other.score;
		uiState = other.uiState;
		bugs.copyFrom(other.bugs);
		goals.copyFrom(other.goals);
		selectedCells.copyFrom(other.selectedCells);
	}
	
	public void sameBoard(PlayerBoard other) 
	{	G.Assert(score==other.score,"score mismatch");
		G.Assert(uiState==other.uiState,"uiState mismatch");
		bugs.sameContents(other.bugs);
		goals.sameContents(other.goals);
		G.Assert(parent.sameCells(selectedCells,(other.selectedCells)),"selectedCells mismatch");
	}
	
	public long Digest(Random r) 
	{
		long v = 0;
		v ^= parent.Digest(r,score);
		v ^= uiState.Digest(r);
		v ^= bugs.Digest(r);
		v ^= goals.Digest(r);
		return v;
	}
	public BugsCell getCell(BugsId rack,char c,int r)
	{
		switch(rack)
		{
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
			parent.addPickDrop(all,bugs,who);
			parent.addPickDrop(all,goals,who);
			break;
		case Purchase:
		default:
			G.Error("Not expecting %s",state);
		}
	}

}
