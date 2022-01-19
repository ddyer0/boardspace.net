package common;

import lib.OStack;

/**
 * this is the master file that determines what games are in boardspace menus.
 * there ought to (eventually) be a way all of this is encoded in the games themselves
 * so the process of adding a game would only consist of dropping a new jar in the directory.
 * 
 * @author Ddyer
 *
 */
public class GameInfoStack extends OStack<GameInfo>
{
	public GameInfo[] newComponentArray(int n) { return(new GameInfo[n]); }
}