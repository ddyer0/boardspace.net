package yspahan;

import java.util.ArrayList;

public interface IYspahanPlayStrat {

	/***
	 * If you are the start player of a round, you can decide to use additional yellow dices!
	 * 
	 */
	public abstract void throwDices();

	public abstract void doMoveMain();
	
	public abstract void cardsAfterMoveMainAction();

	public abstract void doMoveBuilding(); // end do move
	
	public abstract void copyBoardState(YspahanBoard bd, boolean[] robots); 
	
	public abstract ArrayList<YspahanMovespec> getMoveList();

	public abstract boolean supervisorSendCubeToCaravan(YspahanCell cell);

	public abstract void supervisorDesignateCube();

}