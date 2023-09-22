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
package online.common;

import bridge.Color;
import common.GameInfo;
import lib.CompareTo;
import lib.ExtendedHashtable;
import lib.G;

public class User implements LobbyConstants,CompareTo<User>
{	
    public String name=""; 			//the name we're known by
    boolean isGuest = false;
    boolean isNewbie = false;
    public boolean dead=false;			// marked for deletion when the user list is not held by the mouse
    public String uid = "0"; 	// immutable user id
    public String publicName; 	//public name (different for guests)
    public int serverIndex; 	//the number by which the server identifies us
    public int messages=0;		// the number of chat messages we've received
    public int displayIndex; 			//position at which this user is displayed
    boolean isRobot;
    boolean isLocal;
    public boolean ignored; //we're ignoring this guy
    public boolean nochallenge; //we're not accepting challenges from this guy
    public boolean mutedMe; //he muted us!
    public boolean nochallengeMe; //he's not accepting challenges from us
    public boolean automute; //consensus mute
    public int ignoreCount; //how many others are ignoring him
    private PlayerClass playerClass = PlayerClass.Beginner; //player class - beginner, regular, or master
    public boolean markedInLastRefresh;
    private Session session = null;
    private int playLocation;
    GameInfo preferredGame;
    
    public void setSession(Session s,int pl) 
    {	if(session!=null) { session.setPlayer(this,-1); }
    	session = s; 
    	playLocation = pl;
    	if(s!=null) { s.setPlayer(this,pl); }
    }
    public Session session() 
    {
    	Session sess = session;
    	if(sess!=null)
    	{
    		if(sess.players[playLocation]==this)  { return(sess); }
    		session = null;
    		playLocation = 0;
    	}
    	return(null);
    }
    public int playLocation() { return(playLocation); }
    
    // yes, this is supposed to be a static variable.  It's used to 
    // affect the sort in the lobby list of names.
    static public boolean waitingForMaster; //waiting for a master game
    public Session playingLocation; //playing in a session
    public Session spectatingLocation; //spectating in a session
    public Session clickSession;
    public int inviteSession;
    public long chatTime = 0;
    public String sessionKey = "";
    public String localIP = "";
    ExtendedHashtable info = new ExtendedHashtable(true);
    ExtendedHashtable ranking = new ExtendedHashtable(true);

 
    enum PlayerClass
    {	
    	Beginner(-1,new Color(240, 180, 180)), 		//beginning player, less than 100 games
    	Intermediate(0,new Color(220, 220, 220)), 	//intermediate player, more thatn 100 games
    	Expert(1,new Color(250, 250, 0)), 			//reached 900 points
    	Master(2,new Color(180, 255, 180)); //master players, reached 950 
    	int value = 0;
    	Color color;
    	PlayerClass(int v,Color c) { value = v; color=c; }
    	public int intValue() { return(value); }
    }
    public String getHostUID()
    {
    	String host = getInfo(OnlineConstants.HOSTUID);
    	return((host == null) ? "unknown" : host);
    }
    
    Color playerNameColor()
    {	return(playerClass.color);
    }
    boolean setPlayerClass(int val)
    {  	if(!isRobot)
    	{for(PlayerClass cl : PlayerClass.values())
    		{	if(val==cl.value)
    				{ playerClass = cl;
    			return(true);
    		}
    		}}
    return(false);
    }
    
    int sortscore(boolean master)
    {
        if (isRobot)
        {
            return (0);
        }
        Session wait = session;
        Session play = playingLocation;
        if(play==null && wait==null) { play = spectatingLocation; }

        int val = (wait !=null) 
        			? 1*100000+wait.gameIndex	 // actively waiting
                     : ((play == null)
                    		? 2000000 // not anywhere
                    		: 3000000+play.gameIndex); // actively playing

        if (master && (playerClass != PlayerClass.Master))
        {
            val += 10;
        }

        return (val);
    }

    void setInfo(String key, String val)
    { //not putString because we allow null and change in value
      //System.out.println("store " + name+": "+ key + "("+val+")");
        info.put(key, val);
        if(PREFERREDGAME.equalsIgnoreCase(key))
        {
        	preferredGame = GameInfo.findByNumber(G.IntToken(val));
        }
    }

    public String getInfo(String key)
    {
        return ((String) info.get(key));
    }

    void setChatTime()
    {
        chatTime = G.Date();
    }

    public String getRanking(String variation)
    {
        return((String)ranking.get(variation));
   }

    public boolean setRanking(String variation, String rank)
    {	
    	String old = getRanking(variation);
        ranking.put(variation, rank);

        return ((rank == null) ? (old != null) : rank.equalsIgnoreCase(old));
    }

    public String rankingString()
    {
        StringBuilder val = new StringBuilder();

        for(String key : ranking.keySet())
        {	if(key!=null)
        {
                String v = (String) ranking.get(key);
        	if (v != null)
        	{	G.append(val," ",uid," ",key," ",v);
        }
         }}

        return (val.toString());
    }
    public String toString() { return("<user "+prettyName()+"#"+uid+">"); }
    public int altCompareTo(User bb) { return(compareTo(bb)); }
    public int compareTo(User b)
    { boolean mas = waitingForMaster;
      int as = sortscore(mas);
      int bs = b.sortscore(mas);
      int val = (as<bs) 
    		  ? -1
    		  : as>bs 
        ? 1   
    		  	: name.compareToIgnoreCase(b.prettyName())<=0
    		  		? -1
    		  		: 1;
      return(val);
    }
   
    public String prettyName()
    {
    	String p = publicName;
        return((p!=null) ? p : G.getTranslations().get(UNKNOWNPLAYER)); 
    }

	public PlayerClass getPlayerClass() { return(playerClass); }

	public static String prettyName(User u)
	{
		return(u==null ? null : u.prettyName());
	}
}
