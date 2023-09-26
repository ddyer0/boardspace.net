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
package viticulture;

import java.awt.FileDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import bridge.Utf8Printer;
import online.game.commonPlayer;
import online.game.sgf.sgf_game;
import online.game.sgf.sgf_reader;
import viticulture.PlayerBoard.ScoreEvent;
import viticulture.PlayerBoard.ScoreStack;
import lib.ErrorX;
import lib.OStack;


class StatStack extends OStack<ViticultureStats>
{
	public ViticultureStats[] newComponentArray(int n) { return(new ViticultureStats[n]); }
}

public class ViticultureStats implements ViticultureConstants
{

	String gameName;
	String playerName;
	ViticultureColor playerColor; 
	int playerPoints = 0;
	int categories[] = new int[ScoreType.values().length];
	
	ViticultureStats(String ga,ViticultureBoard b,PlayerBoard p,String na)
	{	gameName = ga;
		if((ga!=null) && ga.endsWith(".sgf")) { ga=ga.substring(0,ga.length()-4); }
		playerName = na;
		playerColor = p.color;
		playerPoints = p.score;
		ScoreStack ss = p.scoreEvents;
		for(int lim = ss.size()-1; lim>=0; lim--)
		{
			ScoreEvent ev = ss.elementAt(lim);
			categories[ev.type.ordinal()] += ev.changeSummary();
		}
	}
	static void printLegend(PrintStream out)
	{	printC(out,"Row#");
		printC(out,"Game Name");
		printC(out,"Player");
		printC(out,"Points");
		for(ScoreType s : ScoreType.values())
		{
			printC(out,s.name());
		}
		// per game stats
	
		out.print("\n");
	}
	void printRecord(PrintStream out)
	{	printC(out,gameName);
		printC(out,playerName); 
		printC(out,playerPoints);
		
		for(int v : categories)
		{
			printC(out,v);
		}

		out.print("\n");
	}
	static void printC(PrintStream out,String str)
	{
		out.print(str); 
		out.print(",");
	}
	static void printC(PrintStream out,int val)
	{
		out.print(val);
		out.print(",");
	}
	static void printC(PrintStream out,boolean val)
	{
		out.print(val);
		out.print(",");
	}
	public String toString() { return("<Stats for "+gameName+" : "+playerName+">"); }

	static StatStack allStats=new StatStack();
	
	public static boolean bestplayer = false;
	public static boolean otherplayers = true;
	public static void collectStats(sgf_game g,ViticultureBoard b,commonPlayer players[])
	{	if(b.nPlayers()>1)
		{
		PlayerBoard best = null;
		for(PlayerBoard p : b.pbs)
		{	if(best==null || best.tiebreakScore()<p.tiebreakScore()) 
			{	best = p; }
		}
		if(bestplayer) { collect1Stat(g,b,best,players); }
		if(otherplayers)
			{
			for(PlayerBoard p : b.pbs)
			{	if(p!=best)
				{	collect1Stat(g,b,p,players);
				}
			}
			}
		}
	}
	public static void collect1Stat(sgf_game g,ViticultureBoard b,PlayerBoard p,commonPlayer players[])
	{
		commonPlayer player = commonPlayer.findPlayerByIndex(players,p.boardIndex);
			allStats.push(new ViticultureStats(g.short_name(),b,p,
					player==null ? "unknown" : player.trueName));
	}
	public static void saveStats()
	{
		String ss = sgf_reader.do_sgf_dialog(FileDialog.SAVE,"viticulture", "*.txt");
		if(ss!=null)
		{
			File f = new File(ss);
			OutputStream fs = null;
	        PrintStream out = null;
	            try
	            {
	                fs = new FileOutputStream(f);
	                out = Utf8Printer.getPrinter(fs);
	                printLegend(out);
	                for(int i=0;i<allStats.size();i++)
	                {
	                	ViticultureStats stat = allStats.elementAt(i);
	                	printC(out,i+1);
	                	stat.printRecord(out);
	                }
	                out.close(); 
	            	fs.close();
	            }
	            catch (IOException e)
	            { throw new ErrorX(e); 
	            }
		}
	}
	
}
