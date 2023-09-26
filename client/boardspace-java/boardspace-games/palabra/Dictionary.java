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
package palabra;

/* dictionary for palabra 
 * 
 * based on a scrabble dictionary, cross-correlated with a word frequency dictionary
 * 
 * */

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import lib.G;

class Entry
{
	String word;
	int order;
	
	Entry(String n) { word = n; order = -1; }

}
public class Dictionary extends Hashtable<String,Entry>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String readToken(BufferedInputStream stream) throws IOException
	{	StringBuilder b = new StringBuilder();
		int ch = 0;
		while ( ((ch = stream.read())>0)
				&& !Character.isAlphabetic(ch)) {};
		if(ch<0) { return(null); }
		b.append((char)ch);
		while(((ch = stream.read())>0)
				&& Character.isAlphabetic(ch)) { b.append((char)ch); }
		
		return(b.toString().toLowerCase());

	}
	//
	// load the contents of a file. If extensions, load only the words
	// that are extensions of existing words.
	//
	public int load(String file,boolean extensions)
	{	int loaded = 0;
		int excluded = 0;
		try {
			FileInputStream rawStream = new FileInputStream(file);
			BufferedInputStream stream = new BufferedInputStream(rawStream);
			String msg = null;
			while( (msg = readToken(stream))!=null)
			{
				if(get(msg)==null)
				{	if(extensions)
						{
						int len = msg.length();
						boolean keep = false;
						for(int lim=len-2;!keep && lim>0;lim--)
							{
							if(get(msg.substring(0,lim))!=null) 
								{ keep = true; 
								}
							if(get(msg.substring(len-lim))!=null) 
								{ keep = true; }
							}
						if(keep) { put(msg,new Entry(msg)); loaded++; }
						else { excluded++; }
						}
					else {
						put(msg,new Entry(msg));
						loaded++;
						}
				}
				else { G.print("Duplicate word "+msg); }
			}
		} catch (IOException  e) {
			G.Error("error reading %s %s",file,e);
		}
		G.print("file "+file+"\nloaded "+loaded+" excluded "+excluded);
		return(loaded);
	}
	public int loadAlways(String file) { return(load(file,false)); }
	public int loadExtensions(String file) { return(load(file,true)); }
	
	public int loadOrder(String file)
	{	int loaded = 0;
		int excluded = 0;
		try {
			FileInputStream rawStream = new FileInputStream(file);
			BufferedInputStream stream = new BufferedInputStream(rawStream);
			String msg = null;
			while( (msg = readToken(stream))!=null)
			{	Entry existing = get(msg);
				if(existing!=null)
				{
					if(existing.order>0) { G.print("Duplicate word "+msg+" was "+existing.order+" at "+loaded); }
					else
					{ loaded++; 
					  existing.order = loaded;
					}
				}
				else { excluded++; }
			}
		} catch (IOException  e) {
			G.Error("error reading %s %s",file,e);
		}
		G.print("file "+file+"\nloaded "+loaded+" excluded "+excluded);
		return(loaded);
	}
	public void removeFakes()
	{
		for(Enumeration<String>k = keys(); k.hasMoreElements(); )
		{	String name = k.nextElement();
			Entry item = get(name);
			if(item.order<=0)
			{
				remove(name);
				if((name.length()<=3)||(name.length()>7)) { G.print("Fake word: "+name); }
			}
		}
	}
	public void load()
	{	
	loadAlways("g:/share/projects/boardspace-java/boardspace-games/palabra/words/2-letter-words.txt");
	loadAlways("g:/share/projects/boardspace-java/boardspace-games/palabra/words/3-letter-words.txt");
	loadAlways("g:/share/projects/boardspace-java/boardspace-games/palabra/words/4-letter-words.txt");
	loadAlways("g:/share/projects/boardspace-java/boardspace-games/palabra/words/5-letter-words.txt");
	loadAlways("g:/share/projects/boardspace-java/boardspace-games/palabra/words/6-letter-words.txt");
	loadAlways("g:/share/projects/boardspace-java/boardspace-games/palabra/words/7-letter-words.txt");
	loadExtensions("g:/share/projects/boardspace-java/boardspace-games/palabra/words/8-letter-words.txt");
	loadExtensions("g:/share/projects/boardspace-java/boardspace-games/palabra/words/9-letter-words.txt");
	loadExtensions("g:/share/projects/boardspace-java/boardspace-games/palabra/words/10-letter-words.txt");
	loadExtensions("g:/share/projects/boardspace-java/boardspace-games/palabra/words/11-letter-words.txt");
	loadExtensions("g:/share/projects/boardspace-java/boardspace-games/palabra/words/12-letter-words.txt");
	loadExtensions("g:/share/projects/boardspace-java/boardspace-games/palabra/words/13-letter-words.txt");
	loadExtensions("g:/share/projects/boardspace-java/boardspace-games/palabra/words/14-letter-words.txt");
	loadExtensions("g:/share/projects/boardspace-java/boardspace-games/palabra/words/15-letter-words.txt");
	loadOrder("g:/share/projects/boardspace-java/boardspace-games/palabra/words/wiki-100k.txt");
	//G.print("size is "+size());
	//removeFakes();
	G.print("final size is "+size());
	}
	
}
