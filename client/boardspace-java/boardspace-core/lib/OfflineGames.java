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
package lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import jzlib.ZipEntry;
import jzlib.ZipInputStream;
import java.io.IOException;

import bridge.Utf8Printer;

/**
 * save, restore, look for, and prune junk for incomplete offline games.  The actual format
 * of the offline game doesn't matter except that the entire game record is in
 * one line.  This is closely integrated with the game restart mechanism.
 * 
 */
public class OfflineGames {
	public static String GameKey = "V1";
	public static String GameKeyPlus = GameKey+" ";	// includes the space
	public static String Suffix = ".incomplete";
/**
 * delete incomplete offline games that are more than "daysold" old.  The intent
 * of this is to keep abandoned games from sticking around forever
 * 
 * @param daysold
 */
	public static void pruneOfflineGames(int daysold)
	{	String dir = OfflineGames.incompleteGameDirectory();
		File fdir = new File(dir);
		fdir.mkdir();
		//G.print("Created directory "+fdir);
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
				{	Tokenizer tok = new Tokenizer(body);
					String key = tok.nextToken();
					if(GameKey.equalsIgnoreCase(key))
					{
					long adate = tok.hasMoreTokens() ? tok.longToken() : 0;    				
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
	/**
	 * get a gamedescription from a file.  The content and meaning of the string are
	 * not defined, but ought to be known to the caller.  There's a minimal check that
	 * the file is actually a game description and not some other random file.
	 * 
	 * @param UIDstring the name of the file which ought to contain a game description
	 * 
	 * @return a game description string or null
	 */
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
	private static String readOfflineGame(String UIDstring)
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
	/**
	 * remove an offline game, normally when it has been completed
	 * 
	 * @param UIDstring
	 */
	public static void removeOfflineGame(String UIDstring)
	{
		String dir = OfflineGames.incompleteGameDirectory();
		File f = new File(dir
					+OfflineGames.safeFileName(UIDstring)
					+(UIDstring.endsWith(Suffix) ? "" : Suffix));
		f.delete();
	}
	/**
	 * record an offline game in progress.  Normally this is called for every move
	 * by a human in an offline games.
	 * 
	 * @param UIDstring
	 * @param msg
	 * @return
	 */
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
	/**
	 * convert a game id to something acceptable as a file name.
	 * 
	 * @param str
	 * @return
	 */
	private static String safeFileName(String str)
	{
		return str.replace('|','-').replace(' ', '+');
	}
	/**
	 * 
	 * @return the directory where incomplete offline games will be stored
	 */
	private static String incompleteGameDirectory()
	{
		String base = G.documentBaseDir();
		String dir = base+"incomplete-games/";
		return(dir);
	}

}
