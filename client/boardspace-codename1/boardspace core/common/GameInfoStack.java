package common;

import bridge.Preferences;
import lib.G;
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
	
	/**
	 * filter the list by number of players.  0 is match all.
	 * 
	 * @param nplayers
	 * @return
	 */
	public GameInfo[] filterGames(int nplayers)
	{	if(nplayers==0) { return toArray(); }
		GameInfoStack sub = new GameInfoStack();
		for(int i=0,lim=size();  i<lim; i++) { 
			GameInfo g = elementAt(i);
			if((nplayers>=g.minPlayers) && (nplayers<=g.maxPlayers)) { sub.push(g);}
		}
		return sub.toArray();
	}
	/** reload a game list from preferences
	 * 
	 * @param key
	 */
	public void reloadGameList(String key)
	{	clear();
		Preferences prefs = Preferences.userRoot();
		String games = prefs.get(key,null);
		if(games!=null)
		{
			String all[] = G.split(games,',');
			if(all!=null)
			{
			for(String name : all)
				{
				GameInfo g = GameInfo.findByVariation(name);
				if(g!=null) { push(g); }
				}
			}
		}
		
	}
	/**
	 * record a game at the head of the recent list
	 * @param in
	 * @param key
	 * @param limit
	 */
	public void recordRecentGame(GameInfo in,String key,int limit)
	{	// remove any games with the same name - variations are considered to be the same
		for(int lim = size()-1; lim>=0; lim--)
			{
			GameInfo g = elementAt(lim);
			if(g.gameName.equalsIgnoreCase(in.gameName)) { remove(lim,true); }
			}
		// reduce size to the limit size
		while(size()>=limit) { remove(0,false); }
		// insert at the beginning
		insertElementAt(in,0);		// move to the most recent position
		// save as a property
		StringBuilder b = new StringBuilder();
		for(int i=0,lim=size(); i<lim; i++)
		{	GameInfo g = elementAt(i);
			b.append(g.variationName);
			b.append(",");
		}
		Preferences prefs = Preferences.userRoot();
		prefs.put(key,b.toString());
	}

}