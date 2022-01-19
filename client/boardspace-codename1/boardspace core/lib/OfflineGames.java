package lib;

import bridge.File;
import bridge.FileInputStream;
import bridge.FileOutputStream;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;

import java.io.IOException;
import java.util.StringTokenizer;

import bridge.Utf8Printer;

/**
 * save, restore, look for, and prune junk for offline games.  The actual format
 * of the offline game doesn't matter except that the entire game record is in
 * one line.  This is closely integrated with the game restart mechanism.
 * 
 */
public class OfflineGames {
	public static String GameKey = "V1";
	public static String GameKeyPlus = GameKey+" ";	// includes the space
	public static String Suffix = ".incomplete";

	public static void pruneOfflineGames(int daysold)
	{	String dir = OfflineGames.incompleteGameDirectory();
		File fdir = new File(dir);
		fdir.mkdir();
		final long now = G.Date();
		final long bugfixdate = 1625964193960L;	// june 30 2021 + 60 days
		final long date = now-daysold*1000*60*60*24;	// 1000*60*24 milliseconds per day
		FileMapper files = new FileMapper();
		files.visit(fdir, new FileVisitor() {
	
			public void visit(File u) {
				String nameKey = u.getName();
			if(nameKey.endsWith(Suffix))
	{	
	    		String body = OfflineGames.readOfflineGame(nameKey);
		
	    		if(body!=null) 
				{	StringTokenizer tok = new StringTokenizer(body);
					String key = tok.nextToken();
					if(GameKey.equalsIgnoreCase(key))
					{
					long adate = tok.hasMoreTokens() ? G.LongToken(tok) : 0;    				
					if(adate>date) 
	    					{ OfflineGames.removeOfflineGame(nameKey);
						}}
				}
				else if(now>bugfixdate) 
					{	// remove other trash starting about sep 1 2021
					OfflineGames.removeOfflineGame(nameKey);
	    			}
	    		}
				}
				
			public void visitZip(File zipFile, ZipEntry e, ZipInputStream s) {
				
			}
			public boolean filter(String name, boolean isDirectory, File parent) {
				return true;
			}
			
		});
	
	}
	public static String restoreOfflineGame(String UIDstring)
	{	
		String body = readOfflineGame(UIDstring);
		if(body!=null)
		{
			if(body.startsWith(GameKeyPlus))
			{
				int idx = body.indexOf(' ',GameKeyPlus.length());
				if(idx>0)
				{
				body = body.substring(idx+1);
				}
			}
		}
		return(body);
	}
	
	// read the raw text
	public static String readOfflineGame(String UIDstring)
	{
		if("".equals(UIDstring)) { return(null); }
		String dir = OfflineGames.incompleteGameDirectory();
		File f = new File(dir
							+ OfflineGames.safeFileName(UIDstring)
							+ (UIDstring.endsWith(Suffix) ? "" : Suffix));
		try {
			FileInputStream fs = new FileInputStream(f);
			Utf8Reader stream = new Utf8Reader(fs);
			String body = stream.readLine();
			fs.close();
			stream.close();
		return(body);
	}
		catch (IOException e) { return(null); }
	
	}
	public static void removeOfflineGame(String UIDstring)
	{
		String dir = OfflineGames.incompleteGameDirectory();
		File f = new File(dir
					+OfflineGames.safeFileName(UIDstring)
					+(UIDstring.endsWith(Suffix) ? "" : Suffix));
		f.delete();
	}

	public static String recordOfflineGame(String UIDstring, String msg)
	{	
		G.Assert(!"".equals(UIDstring),"game UIDString not set");
		String dir = OfflineGames.incompleteGameDirectory();
		String filename = dir+OfflineGames.safeFileName(UIDstring)+Suffix;
		try {
		FileOutputStream fs = new FileOutputStream(new File(filename));
		Utf8Printer pw =  Utf8Printer.getPrinter(fs);
		pw.print(GameKeyPlus);
		pw.print(G.Date());
		pw.print(" ");
		pw.print(msg);
		pw.flush();
		fs.close();
		}
		catch (Exception e)
		{ 
			G.print("Error saving state "+e);
		}
		return(filename);
	}

	public static String safeFileName(String str)
	{
		return str.replace('|','-').replace(' ', '+');
	}

	public static String incompleteGameDirectory()
	{
		String base = G.documentBaseDir();
		String dir = base+"incomplete-games/";
		return(dir);
	}

}
