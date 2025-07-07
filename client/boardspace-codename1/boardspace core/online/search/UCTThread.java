package online.search;

import online.game.BoardProtocol;
import online.game.commonMove;

public interface UCTThread
{

	public double Static_Evaluate_Move(commonMove child);
	public double initialWinRateWeight();
	public void incrementTreeSizeThisRun();
	public commonMove[] getNewChildren();
	public void setStop(boolean b);
	public int a1Priority();
	public commonMove getCurrent2PVariation();
	public commonMove getCurrentVariation();
	public RobotProtocol robot();
	public void setA1Priority(int i);
	public void setBoardCopy(BoardProtocol object);
	public void setCopyTheBoard(boolean b);
	public BoardProtocol boardCopy();
	public void requestExit();
	public boolean stopped();
	public boolean isAlive();
	public void setStopped(boolean b);
	public void simUsingTree();
	public long pausedTime();
	public void setPausedTime(long i);
	public void start();
	public StringBuffer getStackTrace(StringBuffer p);
	public int getPriority();
	public void setPriority(int i);

}
