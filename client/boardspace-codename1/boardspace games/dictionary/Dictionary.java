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
package dictionary;

/* dictionary for words games.
 * 
 * this uses an array of sub-dictionaries for each word length
 * 
 * */

import bridge.File;
import bridge.FileOutputStream;
import com.codename1.io.BufferedInputStream;
import com.codename1.io.gzip.GZIPInputStream;

import bridge.Config;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import lib.ByteOutputStream;
import lib.G;
import lib.Http;
import lib.Utf8Reader;

/**
 * the main dictionary class.  This uses a set of subdictionaries segregated by word length
 * so it's possible to get enumerators for words of length N
 * @author Ddyer
 *
 * steps to build a dictionary file
 * 1) call "load" on lists of known good words
 * 2) call "loadOrder" on lists of words sorted by use frequency
 *    the current source is "wordfreq.txt" derived from  wordfreq.html from https://gist.github.com/h3xx/1976236
 * 3) call "savewordlist" to save the result as a word list sorted by order
 * 4) manually compress the result with gzip
 * 
 */
public class Dictionary implements Config
{  	
	static final byte[] COMPRESSED_DICTIONARY_MAGIC = { 0x32, 0x12, 0x58, 0x4a};
	
	public static final int MAXLEN = 15;
	
	static DictionaryHash wordlen[];	// subdictionaries
	static boolean loaded = false;		// true if loaded from the data file
	static boolean definitionsLoaded = false;
	static boolean definitionsAllLoaded = false;
	static int definitionStringSize = 0;
	static int definitionByteSize = 0;
	static int definitionCount = 0;
	
	static Dictionary instance = null;	// the canonical instance of the full sized dictionary
	private int orderedSize;				// the break between formally ordered words and "all the rest" of rare words.
	private int totalSize;				// the number of words in the dictionary
	
	/*
	 * get a sub dictionary of words of a specified length
	 * 
	 */
	public DictionaryHash getSubdictionary(int len)
	{
		return((len<0 || len>MAXLEN) ? null : wordlen[len]);
	}
	/**
	 * get the canonical dictionary
	 * @return
	 */
	public synchronized static Dictionary getInstance()
	{
		if(instance==null)
		{ 
			instance = new Dictionary();
		}
		return(instance);
	}
	/*
	 * constructor, don't call, use Dictionary.getInstance()
	 */
	private Dictionary()
	{

		if(wordlen==null)
		{
		wordlen = new DictionaryHash[MAXLEN+1];
		for(int i=1;i<=MAXLEN;i++) {  wordlen[i] = new DictionaryHash(i); }
		}
		new Thread(new Runnable() 
		{ public void run() { 
		try {
		load();
		loadDefinitions();
		}
		catch (Throwable e)
		{
			Http.postError(this,"Loading Dictionary",e);
		}
		}}).start();
	}
	public void waitForLoaded() 
	{	int n = 0;
		while(!loaded) 
			{ if(n>0) { G.print("Wait for loaded "+n); }
			  G.doDelay(100); 
			  n++;
			}
		if(n>1)
	{
			G.print("done waiting");
			}
	}

	public void waitForDefinitions()
	{	int n = 0;
		while(!definitionsAllLoaded) 
			{ if(n>0) { G.print("Wait for definitions "+n); }
			  G.doDelay(100);  
			  n++;
			}
		if(n>1)
	{
			G.print("done waiting");
		}
	}
	public int size() { 
		waitForLoaded();
		return(totalSize); 
	}
	public int orderedSize() {
		waitForLoaded();
		return orderedSize;
	}
	public Entry get(String w)
	{	waitForLoaded();
		return getInternal(w);
	}
	public boolean isLoaded()
	{
		return loaded;
	}
	private Entry getInternal(String w)
	{
		int len = w.length();
		if(len>0 && len<=MAXLEN) { return(wordlen[len].get(w)) ; }
		return(null);
	}
	private void put(String w,Entry e)
	{
		int len = w.length();
		if(len>=1 && len<=MAXLEN) { wordlen[len].put(w,e); }
		else { G.Error("Length out of range for %s", w); }
	}
	private boolean isAlphabetic(int ch)
	{
		return (((ch>='A')&&(ch<='Z'))
				|| (ch=='-')
				|| ((ch>='a')&&(ch<='z')));
	}
	/**
	 * read a token from the input stream
	 */
	private String readToken(BufferedInputStream stream) throws IOException
	{	StringBuilder b = new StringBuilder();
		int ch = 0;
		while ( ((ch = stream.read())>0)
				&& !isAlphabetic(ch)) {};
		if(ch<0) { return(null); }
		b.append((char)ch);
		while(((ch = stream.read())>0)
				&& isAlphabetic(ch)) { b.append((char)ch); }
		
		return(b.toString().toLowerCase());

	}

	/*
	 * load the contents of a file. If extensions, load only the words
	 * that are extensions of existing words.
	 * 
	 */
	private int load(String file,boolean extensions,boolean inorder)
	{	int loaded = 0;
		try {
			G.print("Loading ",file);
			InputStream rawStream = G.getResourceAsStream(file);
			if(rawStream!=null)
			{
			BufferedInputStream stream = new BufferedInputStream(file.endsWith(".gz")
							? new GZIPInputStream(rawStream)
							: rawStream);
			loaded = load(stream,extensions,inorder);
			rawStream.close();
			}
			else { G.Error("Resource %s not found",file); }
		} catch (IOException  e) {
			G.Error("error reading %s %s",file,e);
		}
		return(loaded);
	}
	//
	// load the definitions from a file with lines word<tab>definition
	// the words are looked up in the dictionary, and the definitions
	// stored as an simple array of bytes.  This in effect is a 2x 
	// compression over java strings.
	//
	@SuppressWarnings("unused")
	private void loadDefinitions(Utf8Reader stream) throws IOException
	{
		String word = null;
		G.print("loading definitions");
		long loadtime = 0;	//  1374mS
		long inctime = 0;
		ByteOutputStream def = new ByteOutputStream();
		long now = G.nanoTime();
		while( (word = stream.readToWhitespace(true))!=null)
		{	
			stream.readBinaryLine(def);
			Entry e = getInternal(word);
			if(e==null)
			{	G.print("Non word "+word);
			}
			else 
			{ 
			  int size = e.setCompressedDefinition(def);
			  definitionByteSize += size;
			  definitionStringSize += def.size();
			  definitionCount++;
			  
			  //if(definitionCount%1000==0) { G.print("Defs "+definitionCount+" "+(loadtime/1000000));}
			  //String redef = e.getDefinition();
			  //G.Assert(def.equals(redef),"def mismatch\n%s\n%s",def,redef);
			}
		}
		long later = G.nanoTime();
		  
		long dif = (later-now);
		loadtime += dif;
		G.print(G.format("loaded %d definitions, %smS string size %sK byte size %sK",
				definitionCount,(loadtime/1000000),
				definitionStringSize/1024,definitionByteSize/1024));
	}
	/*
	 * this is the simple version that loads the same file, but stores the definitions
	 * as a java string.
	 */
	@SuppressWarnings("unused")
	private void loadDefinitionsSimple(Utf8Reader stream) throws IOException
	{
		String msg = null;
		G.print("loading definitions");
		long loadtime = 0;
		while( (msg = stream.readLine())!=null)
		{	int ind = msg.indexOf('\t');
			String word = msg.substring(0,ind).toLowerCase();
			String def = msg.substring(ind+1);
			Entry e = getInternal(word);
			if(e==null)
			{	G.print("Non word "+word);
			}
			else 
			{ long now = G.nanoTime();
			  int size = e.setDefinition(def);
			  long later = G.nanoTime();
			  loadtime += (later-now);
			  definitionByteSize += size;
			  definitionStringSize += def.length();
			  definitionCount++;
			  //if(definitionCount%1000==0) { G.print("defs "+definitionCount+" "+(loadtime/1000000));}
			  //String redef = e.getDefinition();
			  //G.Assert(def.equals(redef),"def mismatch\n%s\n%s",def,redef);
			}
		}
		G.print(G.format("loaded %d definitions, %smS, string size %s byte size %s",definitionCount,(loadtime/1000000),definitionStringSize,definitionByteSize));
	}
	
	public void loadDefinitionsAlways(String file)
	{	
		try {
			InputStream rawStream = G.getResourceAsStream(file);
			BufferedInputStream stream = new BufferedInputStream(file.endsWith(".gz")
							? new GZIPInputStream(rawStream)
							: rawStream);
			Utf8Reader reader = new Utf8Reader(stream);
			loadDefinitions(reader);	// or loadDefinitionsSimple string size 12327815 byte size 8401986 1374mS
			/*
			
			loadDefinitionsSimple(reader);	// or loadDefinitionsSimple with smaz
											// loaded 279496 definitions, string size 12327815 byte size 8401986 3660mS,
			 */
			rawStream.close();
		} catch (Throwable  e) {
			Http.postError(this,"error reading "+file,e);
		}
	}
	
	private int load(BufferedInputStream stream,boolean extensions,boolean inorder) throws IOException
	{
		String msg = null;
		int loaded = 0;
		int excluded = 0;
		
		orderedSize = -1;
		put("a",new Entry("a"));
		while( (msg = readToken(stream))!=null)
		{	if("---".equals(msg)) 
				{ orderedSize = loaded; 
				}
			else
			if(getInternal(msg)==null)
			{	if(extensions)
					{
					int len = msg.length();
					boolean keep = false;
					for(int lim=len-2;!keep && lim>0;lim--)
						{
						if(getInternal(msg.substring(0,lim))!=null) 
							{ keep = true; 
							}
						if(getInternal(msg.substring(len-lim))!=null) 
							{ keep = true; }
						}
					if(keep) { put(msg,new Entry(msg)); loaded++; }
					else { excluded++; }
					}
				else {
					loaded++;
					Entry e = new Entry(msg);
					e.order = loaded; 
					put(msg,e);
					}
			}
			else { G.print("Duplicate word "+msg); }
		}
		if(orderedSize<0) { orderedSize = loaded; }
		G.print("loaded ",loaded," excluded ",excluded," ordered ",orderedSize);
		totalSize = loaded;
		return(loaded);
	}
	
	private int loadAlways(String file,boolean inorder) 
	{ return(load(file,false,inorder)); 
	}
	@SuppressWarnings("unused")
	private int loadExtensions(String file) { return(load(file,true,false)); }
	
	@SuppressWarnings("unused")
	private void loadOrder(String file)
	{
		try {
			InputStream rawStream = G.getResourceAsStream(file);
			BufferedInputStream stream = new BufferedInputStream(file.endsWith(".gz")
					? new GZIPInputStream(rawStream)
					: rawStream);
			loadOrder(stream);
			rawStream.close();
		}
		catch (IOException  e) {
			G.Error("error reading %s %s",file,e);
		}
	}
	// load and compare in word order.  Words that previously did not exist
	// are not loaded.  The word order list is very dirty and not to be 
	// relied on for adding new words.
	private int loadOrder(BufferedInputStream stream) throws IOException
	{	int loaded = 0;
		int excluded = 0;
		int duplicates = 0;
		int total = 0;
		int msize = size();
			String msg = null;
			while( (msg = readToken(stream))!=null)
			{	Entry existing = getInternal(msg);
				total++;
				if(existing!=null)
				{
					if(existing.order>0) 
						{ //G.print("Duplicate word "+msg+" was "+existing.order+" at "+loaded);
						  duplicates++;
						}
					else
					{ loaded++; 
					  existing.order = loaded;
					}
				}
				else { excluded++; }
			}
		G.print("\nloaded "+loaded+" excluded "+excluded+ " duplicates "+duplicates+" total "+total+" size "+msize);
		return(loaded);
	}
	@SuppressWarnings("unused")
	private void removeFakes()
	{	
		for(DictionaryHash h : wordlen) { if(h!=null) { h.removeFakes(); }}
	}
	// this saves the raw word list in word use order, with a "---" marker for the
	// break between words with a defined order and all the rest.
	@SuppressWarnings("unused")
	private void saveWordList(String file)
	{
		EntryStack combined = new EntryStack();
		for(DictionaryHash d : wordlen)
		{
			combined.union(d);
		}
		combined.sort(false);
		try {
			OutputStream fstream = new FileOutputStream(new File(file));
			PrintStream stream = new PrintStream(fstream);
			boolean first = true;
			for(int i=0,lim=combined.size();i<lim;i++)
			{
				Entry e = combined.elementAt(i);
				if(first && e.order<=0) { first=false; stream.println("---"); }
				stream.println(e.word);
			}
			stream.close();
			fstream.close();
		}
		catch (IOException e)
		{
			throw G.Error("output file "+file+" %s",e);
		}
	}

	public void loadDefinitions()
	{
		if(!definitionsLoaded)
		{
			definitionsLoaded = true;
			loadDefinitionsAlways(DictionaryDir+"worddefsa.txt.gz");
			definitionsAllLoaded = true;
		}	
	}
	public void load()
	{	
		if(!loaded)
		{
/*	
	loadAlways(path+"2-letter-words.txt");
	loadAlways(path+"3-letter-words.txt");
	loadAlways(path+"4-letter-words.txt");
	loadAlways(path+"5-letter-words.txt");
	loadAlways(path+"6-letter-words.txt");
	loadAlways(path+"7-letter-words.txt");
	loadExtensions(path+"8-letter-words.txt");
	loadExtensions(path+"9-letter-words.txt");
	loadExtensions(path+"10-letter-words.txt");
	loadExtensions(path+"11-letter-words.txt");
	loadExtensions(path+"12-letter-words.txt");
	loadExtensions(path+"13-letter-words.txt");
	loadExtensions(path+"14-letter-words.txt");
	loadExtensions(path+"15-letter-words.txt");
	
	//loadAlways(path+"twl.txt");
	loadAlways(path+"sowpods.txt");
*/
    /* build a sorted wordorder list */
	//loadAlways(path+"zzs.txt.gz",false);
	//loadOrder(path+"wordfreq.txt.gz");
	//saveWordList("g:/share/projects/boardspace-java/boardspace-games/"+path+"wordsinorder.txt");
			
	loadAlways(DictionaryDir+"wordsinorder.txt.gz",true);
	//G.print("size is "+size());
	//removeFakes();
	loaded = true;
	G.print("final size is "+size());
	}
		
	}
	 /*
	  
	  */ 
/**
 *  this generates a bit mask 52 bits wide that indicate which letters
 * are used in a word.  The low order 26 bits indicate which letters
 * are present, the high order bits which letters are present more than
 * once.
 * @param from the mask so far
 * @param letter the new letter to add
 * @return the new composite mask
 */
	 public static long letterMask(long from,char letter)
	 {	if((letter>='A') && letter<='Z') { letter = (char)( letter ^ ('a'^'A')); }
		 if((letter>='a')&&(letter<='z'))
			{
			 long bit = 1<<(letter-'a');
			 if((bit&from)!=0) { bit=bit<<26; }
			 return(bit|from);
			}
		 else { throw G.Error("char "+letter+" out of range");}
	 }
	 /** generates a letterMask for a base lettermask plus the
	  * characters in a string.
	  * @param from
	  * @param letters
	  * @return
	  */
	 public static long letterMask(long from,String letters)
	 {	long val = from;
	 	for(int i=0,lim=letters.length();i<lim; i++)
	 	{ val = letterMask(val,letters.charAt(i)); 
	 	}
	 	return(val);
	 }
}
