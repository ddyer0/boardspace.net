package online.common;
/**
 * LaunchUser is a bridge between the lobby's information about players
 * and the game setup engine.
 * @author Ddyer
 *
 */
public class LaunchUser
{	public boolean primaryUser;		// true if this is a primary seat using the gui
	public boolean otherUser;			// true if this is a secondary seat using the gui
	public int seat;			// positional seat in the game
	public int order;			// order of play in the game.  Not related to seat position.
	public String host;			// unique host name, same if playing from the same screen.
	public String ranking;		// ranking for this game
	public User user;			// the full lobby user info table
	
    public String toString()
    {
    	return("<lu "+user.prettyName()+"@"+host+" s "+seat+" o "+order+">");
    }
}
