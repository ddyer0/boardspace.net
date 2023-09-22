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
package online.search;
import lib.ExtendedHashtable;
import online.game.BoardProtocol;
import online.game.export.ViewerProtocol;
import online.game.commonMove;
import online.game.commonPlayer;
/**
 * this is the interface to start stop and create robot players.
 * @author ddyer
 *
 */
public interface SimpleRobotProtocol {
    /** initialize the robot, but don't run yet 
     * @param newParam 
     * */
    public void InitRobot(ViewerProtocol newParam, ExtendedHashtable sharedinfo, BoardProtocol gboard,
        String evaluator, int strategy);

    /** start running, run continuously if specified.  Running only means ready to
        generate moves, don't generate any yet */
    public void StartRobot(boolean continuous,commonPlayer runner);

    /** true if moving automatically */
    public boolean Auto();

    /** stop running, but remain ready to restart */
    public void StopRobot();
    /** get the current variation (presumably at an error) */
    public commonMove getCurrentVariation();
    
    /** just stop */
    public void Quit();
    /**
     * pause for debugging
     */
    public void Pause();
    /**
     * resume from a pause
     */
    public void Resume();
    /**
     * Notify a (possibly running) robot of an event
     */
    public void Notify(commonMove m);
    
    /** this is the cue to make a move.  Get the current board state from the game
        and emit a move string */
    public void DoTurnStep(int playerindex);

    /** true if running */
    public boolean Running();
    /** true if being monitored */
    public boolean beingMonitored();

    /** static evaluate position, for debugging **/
    public void StaticEval();
    /** 
     * 
     * @return the main board used by the robot
     */
    public BoardProtocol getBoard();
    /**
     * @return a copy of the main board used by the robot.  During an active search, this will be
     * a copy of board used by the currently selected thread.
     */
    public BoardProtocol disB();
    /** 
     * 
     * @return get the next move, if it's ready.
     */
    public commonMove getResult();
    /**
     * get the progress of a search in progress.  0.0 to 1.0
     * 
     * @return a double
     */
    public double getProgress();
    /** set the progress of a search in progress.
     * this is used to reset the process to 0 or 1
     * @param d
     */
    public void setProgress(double d);
    
    /**
     * getThreads()
     */
    public Thread[]getThreads();
    /**
     * selectThread
     */
    public void selectThread(Thread t);
    
    public Evaluator getEvaluator();
    /** this is a UI hook for robot experiments */
    public void runRobotGameDumbot(ViewerProtocol v,BoardProtocol board,SimpleRobotProtocol r);
    /** this is a UI hook for robot experiments */
    public void runRobotGameSelf(ViewerProtocol v,BoardProtocol board,SimpleRobotProtocol r);
    /** this is a UI hool for robot experiments */
    public void runRobotTraining(ViewerProtocol v,BoardProtocol board,SimpleRobotProtocol r);
    public String getName();
	public void setName(String n);

}
