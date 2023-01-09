package online.game;

import lib.OStack;
import online.game.BaseBoard.BoardState;

/**
 * CommonMoveStack is used all over by move generators for the robot, but
 * has a few extra features for the benefit of the game history.
 * 
 * @author Ddyer
 *
 */
public class CommonMoveStack extends OStack<commonMove>  
{	public commonMove[] newComponentArray(int sz) { return(new commonMove[sz]); }
	public int viewStep=-1;					// scrollback position
	public int viewMoveNumber = -1;
	public int sliderPosition=-1;
	public commonPlayer viewTurn = null;	// player whose turn it was when we entered review mode
	
	/** this is the index into the game history at the point
	 * we are currently examining.  -1 means we're not reviewing.
	 */
	public int viewMove = -1; 		// the maximum move number in an incomplete game
	public BoardState pre_review_state;	// board state before entering review mode

    public commonMove currentHistoryMove()
    {
        if (viewStep > 0)
        {
            return (elementAt(viewStep - 1));
        }
        else if(size()>0) 
        	{
        	  return(top()); 
        	}

        return (null);
    }

	public commonMove find(commonMove targetMove) {
		commonMove cm = currentHistoryMove();
		if(cm.Same_Move_P(targetMove)) { return cm; }
		return null;
		
	}

}
