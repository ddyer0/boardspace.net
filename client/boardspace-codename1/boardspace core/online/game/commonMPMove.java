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
			P2,2,
			P3,3,
			P4,4,
			P5,5,
			P6,6,
			P7,7,
			P8,8,
			P9,9,
			P10,10,
			P11,11);
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
		double max = 0;
	   	if(nplay>1)
	   	{
	   	boolean first = true;
		for(int i=0;i<nplay;i++)
		{
			if(i!=forplayer) 
			{ double thisv =playerScores[i];
			  if(first || (max<thisv)) { max = thisv; first = true; }
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
    public double reScoreProportional(int forplayer,double VALUE_OF_WIN)
    {	
	    int nplay = playerScores.length;
	   	double val = playerScores[forplayer];
	   	double max = 0;
	   	if(nplay>1)
	   	{
	   	boolean first = true;
		for(int i=0;i<nplay;i++)
		{
			if(i!=forplayer) 
			{ double thisv =playerScores[i];
			  if(first || (max<thisv)) { max = thisv; first = false; }
			}
		}
	    double v = val-max;
	    if(v>=VALUE_OF_WIN) { return VALUE_OF_WIN; }
	    else if(v<=-VALUE_OF_WIN) { return -VALUE_OF_WIN; }
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
