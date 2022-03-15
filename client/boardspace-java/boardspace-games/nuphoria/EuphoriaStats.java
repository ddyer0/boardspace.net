package nuphoria;

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
import lib.ErrorX;
import lib.OStack;
import nuphoria.EPlayer.PFlag;
import nuphoria.EuphoriaConstants.*;
class StatStack extends OStack<EuphoriaStats>
{
	public EuphoriaStats[] newComponentArray(int n) { return(new EuphoriaStats[n]); }
}
public class EuphoriaStats {
	
	String gameName;
	String playerName;
	Colors playerColor; 
	RecruitChip firstRecruit;
	RecruitChip hiddenRecruit;
	int morale;
	int knowledge;
	int allegiance;
	int workers;
	int market;
	boolean dilemma;
	int placedWorker = 0;
	int lostWorker = 0;
	int lostArtifact = 0;
	int lostGoods = 0;
	int retrievedWorker = 0;
	
	int euphorianTunnel = 0;
	int wastelanderTunnel = 0;
	int subterranTunnel = 0;
	int euphorianAllegiance = 0;
	int wastelanderAllegiance = 0;
	int subterranAllegiance = 0;
	int icariteAllegiance = 0;
	
	EuphoriaStats(String ga,EuphoriaBoard b,EPlayer p,String na)
	{	gameName = ga;
		if((ga!=null) && ga.endsWith(".sgf")) { ga=ga.substring(0,ga.length()-4); }
		playerName = na;
		playerColor = p.color;
		firstRecruit = p.originalActiveRecruit;
		hiddenRecruit = p.originalHiddenRecruit;
		morale = p.morale;
		knowledge = p.knowledge;
		allegiance = p.allegianceStars.height();
		dilemma = p.testPFlag(PFlag.HasResolvedDilemma);
		market = p.marketStars;
		workers = p.totalWorkers;
		placedWorker = p.placements;
		retrievedWorker = p.retrievals;
		lostWorker = p.workersLost;
		lostArtifact = p.cardsLost;
		lostGoods = p.lostGoods;
	
		// per game stats
		wastelanderAllegiance = b.getAllegianceValue(Allegiance.Wastelander);
		euphorianAllegiance = b.getAllegianceValue(Allegiance.Euphorian);
		subterranAllegiance = b.getAllegianceValue(Allegiance.Subterran);
		icariteAllegiance = b.getAllegianceValue(Allegiance.Icarite);
		wastelanderTunnel = b.getTunnelPosition(Allegiance.Wastelander);
		euphorianTunnel = b.getTunnelPosition(Allegiance.Euphorian);
		subterranTunnel = b.getTunnelPosition(Allegiance.Subterran);
	}
	static void printLegend(PrintStream out)
	{	printC(out,"Row#");
		printC(out,"Game Name");
		// per game stats
		printC(out,"Euphorian Tunnel");
		printC(out,"Wastelander Tunnel");
		printC(out,"Subterran Tunnel");
		printC(out,"Euphorian Allegiance");
		printC(out,"Wastelander Allegiance");
		printC(out,"Subterran Allegiance");
		printC(out,"Icarite Allegiance");
		// per player stats		
		printC(out,"Player Name");
		printC(out,"Player Color");
		printC(out,"First Recruit");
		printC(out,"Allegiance");
		printC(out,"Stars left");
		printC(out,"Dilemma");
		printC(out,"Markets");
		printC(out,"Knowledge");
		printC(out,"Morale");
		printC(out,"Workers");
		printC(out,"Placed Workers");
		printC(out,"Retrieve Workers");
		printC(out,"Lost Workers");
		printC(out,"Lost Artifacts");
		
	
		printC(out,"Lost Goods");
		printC(out,"Hidden Recruit");
		out.print("Hidden Allegiance");
		out.print("\n");
	}
	void printRecord(PrintStream out)
	{	printC(out,gameName);

		// per game stats
		printC(out,euphorianTunnel);
		printC(out,wastelanderTunnel);
		printC(out,subterranTunnel);
		printC(out,euphorianAllegiance);
		printC(out,wastelanderAllegiance);
		printC(out,subterranAllegiance);
		printC(out,icariteAllegiance);
	
		// per player stats
		printC(out,playerName); 
		printC(out,playerColor.toString());
		printC(out,(firstRecruit==null)?"":firstRecruit.name);
		printC(out,(firstRecruit==null)?"":firstRecruit.allegiance.toString());
		printC(out,allegiance);
		printC(out,dilemma);
		printC(out,market);
		printC(out,knowledge);
		printC(out,morale);
		printC(out,workers);
		printC(out,placedWorker);
		printC(out,retrievedWorker);
		printC(out,lostWorker);
		printC(out,lostArtifact);
		printC(out,lostGoods);
		printC(out,(hiddenRecruit==null)?"":hiddenRecruit.name);
		out.print((hiddenRecruit==null)?"":hiddenRecruit.allegiance.toString());
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
	
	public static void collectStats(sgf_game g,EuphoriaBoard b,commonPlayer players[])
	{
		for(EPlayer p : b.players)
		{	commonPlayer player = commonPlayer.findPlayerByIndex(players,p.color.ordinal());
			allStats.push(new EuphoriaStats(g.short_name(),b,p,player.trueName()));
		}
	}
	public static void saveStats()
	{
		String ss = sgf_reader.do_sgf_dialog(FileDialog.SAVE,"nuphoria", "*.txt");
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
	                	EuphoriaStats stat = allStats.elementAt(i);
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
