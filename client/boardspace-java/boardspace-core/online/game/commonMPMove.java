package online.game;

import lib.ExtendedHashtable;
/**
 * this is a suitable base class for the "move" class of games which have more
 * than 2 players.
 * 
 * @author ddyer
 *
 */
public abstract class commonMPMove extends commonMove implements Play6Constants {

	public double []playerScores = null;
	static public void addStandardMoves(ExtendedHashtable D,Object... more)
	{
		D.addStringPairs(
			P2,THIRD_PLAYER_INDEX,
			P3,FOURTH_PLAYER_INDEX,
			P4,FIFTH_PLAYER_INDEX,
			P5,SIXTH_PLAYER_INDEX);
		add2PStandardMoves(D,more);
	}
	public void setNPlayers(int n) 
	{
		if(playerScores==null) { playerScores = new double[n]; }
	}
	public double minPlayerScore()
	{	double min = playerScores[0];
		for(int i=1,lim=playerScores.length; i<lim; i++) 
		{	double sc = playerScores[i];
			if(sc<min) { min = sc; }
		}
		return(min);
	}
	public double maxPlayerScore()
	{	double max = playerScores[0];
		for(int i=1,lim=playerScores.length; i<lim; i++) 
		{	double sc = playerScores[i];
			if(sc>max) { max = sc; }
		}
		return(max);
	}
    public double reScorePosition(int forplayer,double VALUE_OF_WIN)
    {	
	    int nplay = playerScores.length;
	   	double val = playerScores[forplayer];
	   	if(nplay>1)
	   	{
		double max = val;
		for(int i=0;i<nplay;i++)
		{
			if(i!=forplayer) 
			{ double thisv =playerScores[i];
			  if(max<thisv) { max = thisv; }
			}
		}
	    if(val>=VALUE_OF_WIN) { return(VALUE_OF_WIN); }
	    if(max>=VALUE_OF_WIN) { return(-VALUE_OF_WIN); }
	    double v = val-max;
	    return(v);
	   	}
	   	else {
		    if(val>=VALUE_OF_WIN) { return(VALUE_OF_WIN); }
		    return(val);
	   	}
   } 
    
	public void checkNPlayers(int n)
	{ // any number is ok
	}

}
