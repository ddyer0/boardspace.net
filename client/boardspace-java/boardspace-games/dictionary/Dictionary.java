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
import bridge.Config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;


import lib.ByteOutputStream;
import lib.G;
import lib.Http;
import lib.Utf8Reader;
import java.util.zip.GZIPOutputStream;

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
	
	boolean bulkable() { return wordlen[1].bulkable();}
	public static final int MAXLEN = 15;
	
	static DictionaryHash wordlen[];	// subdictionaries
	static boolean loaded = false;		// true if loaded from the data file
	static boolean definitionsLoaded = false;
	static boolean definitionsAllLoaded = false;
	static int rawSize = 0;
	static int compressedSize = 0;
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
	{	boolean saveBulk = false;
		if(wordlen==null)
		{
		wordlen = new DictionaryHash[MAXLEN+1];
		for(int i=1;i<=MAXLEN;i++) {  wordlen[i] = new DictionaryHash(i); }
		}
		new Thread(new Runnable() 
		{ public void run() { 
			try {
			if(bulkable() && useBulkLoad)
			{
				loadBulkDictionary(DictionaryDir+"bulkdictionary.gz");
				Entry rep = get("reprise");
				G.Assert(rep!=null,"dictionary looks corrupt");
			}
			else
			{
			load(); 
			loadDefinitions();
			checkForCorruption();
			if(bulkable() && saveBulk)
			{
				saveBulkDictionary("bulkdictionary.gz");
				loadBulkDictionary("bulkdictionary.gz");
				checkForCorruption();
			}
			}}
			catch (Throwable e)
			{	G.print("Error loading dictionary "+e);
				Http.postError(this,"Loading Dictionary",e);
			}
		}}).start();
	}
	
	public void printStats()
	{
		for(int sz = 1; sz<wordlen.length; sz++)
		{
			G.print(sz,": ",wordlen[sz].statsSummary());
		}
	}
	public void waitForLoaded() 
	{	int n = 0;
		while(!loaded) 
			{ if(n>0) { G.print("Wait for loaded "+n); }
			  G.doDelay(1000); 
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
			  G.doDelay(1000);  
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
		return getInternal(ByteKey.create(w));
	}
	public Entry get(ByteKey w)
	{	waitForLoaded();
		return getInternal(w);
	}
	public boolean isLoaded()
	{
		return loaded;
	}
	private Entry getInternal(ByteKey w)
	{	
		int len = w.length();
		if(len>0 && len<=MAXLEN) 
			{ Entry e = (wordlen[len].get(w)) ;
			  return e;
			}
		return(null);
	}
	private void put(ByteKey w,Entry e)
	{	
		int len = w.length();
		if(len>=1 && len<=MAXLEN) 
			{ wordlen[len].put(w,e); 
			}
		else { G.Error("Length out of range for %s", w); }
	}
	
	private boolean isAlphabetic(int ch)
	{
		return (((ch>='A')&&(ch<='Z'))
				|| (ch=='-')
				|| ((ch>='a')&&(ch<='z')));
	}
	/**
	 * read a lower case token from the input stream.  res is a probe (mutable byte key)
	 */
	private ByteKey readToken(Utf8Reader stream,StringBuilder b,ByteKey res) throws IOException
	{	
		int ch = 0;
		b.setLength(0);
		while ( ((ch = stream.read())>0)
				&& !isAlphabetic(ch)) {};
		if(ch<0) { return(null); }
		char chr = Character.toLowerCase((char)ch);
		b.append(chr);
		while(((ch = stream.read())>0)
				&& isAlphabetic(ch)) { b.append(Character.toLowerCase((char)ch)); }
		
		return(res.setData(b));

	}
	private ByteKey readToken(InputStream stream,ByteOutputStream b,ByteKey probe) throws IOException
	{	
		int ch = 0;
		int index = b.getSize();
		while ( ((ch = stream.read())>0)
				&& !isAlphabetic(ch)) {};
		if(ch<0) { return(null); }
		char chr = Character.toLowerCase((char)ch);
		b.write(chr);
		while(((ch = stream.read())>0)
				&& isAlphabetic(ch)) { b.write(Character.toLowerCase((char)ch)); }
		int newIndex = b.getSize();
		return(probe.setData(b.getBuffer(),index,newIndex-index));
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
		G.print("loading definitions");
		long loadtime = 0;	//  1374mS
		long inctime = 0;
		int targetSize = 1024*10;
		int segments = 1;
		ByteOutputStream def = new ByteOutputStream();
		long now0 = G.nanoTime();
		long now = now0;
		int maxsize = 0;
		ByteOutputStream verb = new ByteOutputStream();
		boolean bulkable = bulkable();
		//
		// this is a specialized use of a custom byteoutput stream. 
		// we make links to its actual data array as as fill it.
		//
		ByteOutputStream data = new ByteOutputStream(targetSize,!bulkable);
		StringBuilder b = new StringBuilder();
		Entry storageProbe = null;
		ByteKey probe = createKey();
		while( readToken(stream,b,probe)!=null)
		{	if(bulkable) { data.reset(); }
			else if( targetSize-data.getSize() < maxsize*2)
			{	
			 maxsize = 0;
			 long later = G.nanoTime();
			 G.print("seg "+segments," @"+definitionCount," ",(later-now)/1000000,"mS");
			 now = later;
			 segments++;
			 // discard the original data array and start a  new one.  
			 data = new ByteOutputStream(targetSize,true);
			}

			stream.readBinaryLine(def);
			// this is a careful dance - in onepass mode storageProbe is same as probe
			Entry e = getInternal(probe);
			if( e==null)
			{	G.print("Non word "+probe);
				Entry ee = getInternal(probe);
			}
			else 
			{ // this will make a definition that shares a pointer into the "data" actual buffer
			  int size = e.setCompressedDefinition(def,verb,data);
			  if(bulkable)
			  	{ put(e,e); 
			  	  if(!bulkable) { probe = storageProbe = createEntry(); }
			  	}
			  maxsize = Math.max(maxsize,size);
			  compressedSize += size;
			  rawSize += def.size();
			  definitionCount++;

			 if(false && definitionCount%1000==0)
			 { 
				  long later = G.nanoTime();
				  long dif = (later-now);
				  loadtime += dif;
				  now = later;
				 G.print("Defs "+definitionCount+" "+(loadtime/1000000));
			 }

			  //String redef = e.getDefinition();
			  //G.Assert(def.equals(redef),"def mismatch\n%s\n%s",def,redef);
			}
			probe.reset();
		}
		long later = G.nanoTime();
		  
		long dif = (later-now0);
		loadtime += dif;
		orderedSize = Math.max(orderedSize,definitionCount);
		totalSize = orderedSize;
		G.print(G.format("loaded %d definitions, %smS compressed size %sK raw size %sK %s segments",
				definitionCount,(loadtime/1000000),
				compressedSize/1024,rawSize/1024,segments));
		printStats();
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
		ByteOutputStream f = new ByteOutputStream();
		ByteOutputStream d = new ByteOutputStream();

		while( (msg = stream.readLine())!=null)
		{	int ind = msg.indexOf('\t');
			ByteKey word = ByteKey.create(msg,0,ind,true);
			String def = msg.substring(ind+1);
			Entry e = getInternal(word);
			if(e==null)
			{	G.print("Non word "+word);
			}
			else 
			{ long now = G.nanoTime();
			  int size = e.setDefinition(def,f,d);
			  long later = G.nanoTime();
			  loadtime += (later-now);
			  compressedSize += size;
			  rawSize += def.length();
			  definitionCount++;
			  //if(definitionCount%1000==0) { G.print("defs "+definitionCount+" "+(loadtime/1000000));}
			  //String redef = e.getDefinition();
			  //G.Assert(def.equals(redef),"def mismatch\n%s\n%s",def,redef);
			}
		}
		G.print(G.format("loaded %d definitions, %smS, compressed size %s raw size %s",definitionCount,(loadtime/1000000)
				,compressedSize,rawSize));
	}
	
	public void loadDefinitionsAlways(String file)
	{	
		try {
			InputStream rawStream = G.getResourceAsStream(file);
			if(rawStream!=null)
			{
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
			}
		} catch (Throwable  e) {
			Http.postError(this,"error reading "+file,e);
		}
	}
	// everything is bulkable now, use shared because it doesn't create garbage on the way down 
	public ByteKey createKey() { return new SharedByteKey(); }
	public Entry createEntry() { return new SharedEntry(); }
	public Entry createEntry(ByteKey k) { return new SharedEntry(k); }
	public Entry createEntry(char letter) { return new SharedEntry(letter); }
	public Entry createEntry(String key) { return new SharedEntry(key); }
	
	private int load(BufferedInputStream stream,boolean extensions,boolean inorder) throws IOException
	{
		int loaded = 0;
		int excluded = 0;
		int targetCapacity = 1024*10;
		orderedSize = -1;
		int segments = 1;
		Entry ea = createEntry("a");
		put(ea,ea);
		boolean bulkable = bulkable();
		ByteOutputStream builder = new ByteOutputStream(targetCapacity,bulkable);
		ByteKey probe = createKey();
		ByteKey eprobe = extensions ? createKey() : null;
		ByteKey terminator = new BulkByteKey("---");
		long now = G.nanoTime();
		while( readToken(stream,builder,probe)!=null)
		{	if(probe.equals(terminator)) 
				{ orderedSize = loaded; 
				}
			else
			if(getInternal(probe)==null)
			{	if(extensions)
					{
					int len = probe.length();
					boolean keep = false;
					for(int lim=len-2;!keep && lim>0;lim--)
						{
						eprobe.setData(probe,0,lim,false);
						if(getInternal(eprobe)!=null) 
							{ keep = true; 
							}
						eprobe.setData(probe,len-lim,len,false);
						if(getInternal(eprobe)!=null) 
							{ keep = true; }
						}
					if(keep)
					{
						loaded++; 
						Entry em = createEntry(probe);
						em.setOrder(loaded); 
						em.setLetterMask(em.calcMask());
						put(em,em); 
					 
					}
					else { excluded++; }
					}
				else {
					loaded++;
					Entry e = createEntry(probe);
					e.setOrder(loaded); 
					e.setLetterMask(e.calcMask());
					put(e,e);
					}
			}
			else { G.print("Duplicate word "+probe); }
		 if(bulkable) { builder.reset(); }
		 else if(targetCapacity-builder.getSize()<probe.length()*2)
		 {
			 builder = new ByteOutputStream(targetCapacity,true);
			 long later = G.nanoTime();
			 G.print("seg "+segments," @"+definitionCount," ",(later-now)/1000000,"mS");
			 now = later;
			 segments++;
		 }
		 probe.reset();
		}
		if(orderedSize<0) { orderedSize = loaded; }
		G.print("loaded ",loaded," excluded ",excluded," ordered ",orderedSize," "+segments+" segments");
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
			if(rawStream!=null)
			{
			BufferedInputStream stream = new BufferedInputStream(file.endsWith(".gz")
					? new GZIPInputStream(rawStream)
					: rawStream);
			loadOrder(stream);
			rawStream.close();
		}
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
		int targetSize = 1024*10;
		int segments = 1;
		int msize = size();
		int maxlen = 0;
		long now0 = G.nanoTime();
		long now = now0;
		boolean bulkable = bulkable();
		ByteKey probe = createKey();
		ByteOutputStream builder = new ByteOutputStream(targetSize,!bulkable);
		while(readToken(stream,builder,probe)!=null)
			{	probe.reset();
				Entry existing = getInternal(probe);
				total++;
				maxlen = Math.max(probe.length(),maxlen);
				if(existing!=null)
				{
					if(existing.getOrder()>0) 
						{ //G.print("Duplicate word "+msg+" was "+existing.order+" at "+loaded);
						  duplicates++;
						}
					else
					{ loaded++; 
					  existing.setOrder(loaded);
					}
				}
				else { excluded++; }
			if(bulkable) { builder.reset(); }
			if(targetSize-builder.size()<maxlen*2)
			{
				builder = new ByteOutputStream(targetSize,true);
				 long later = G.nanoTime();
				 G.print("seg "+segments," @"+definitionCount," ",(later-now)/1000000,"mS");
				 now = later;
				segments++;
			}
			}
		G.print("\nloaded "+loaded+" excluded "
				+excluded+ " duplicates "
				+duplicates+" total "
				+total+" size "
				+msize+" "
				+segments+" segments");
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
				if(first && e.getOrder()<=0) { first=false; stream.println("---"); }
				stream.println(e.getString());
			}
			stream.close();
			fstream.close();
		}
		catch (IOException e)
		{
			throw G.Error("output file "+file+" %s",e);
		}
	}
	
	private void saveBulkDictionary(String file)
	{
		try {
			OutputStream stream = new FileOutputStream(new File(file));
			GZIPOutputStream fstream = new GZIPOutputStream(stream);
			for(DictionaryHash d : wordlen)
			{
				if(d!=null) { d.save(fstream); }
			}
			fstream.close();
			stream.close();
		}
		catch (IOException e)
		{
			throw G.Error("output file "+file+" %s",e);
		}
	}
	private void checkForCorruption()
	{
		try {
		Entry reprise = get("reprise");		
		G.Assert(reprise!=null,"dictionary looks corrupted, no reprise");
		String def = reprise.getDefinition();
		G.Assert("to take back by force, also REPRIZE".equals(def),"wrong definition for reprise");
		}
		catch (Throwable err)
		{
			G.Error("error using dictionary "+err);
		}
	}
	private void loadBulkDictionary(String file) 
	{	if(!definitionsLoaded)
		{	
		try {
			long now = G.nanoTime();
			InputStream stream = G.getResourceAsStream(file);// new FileInputStream(new File(file));
			if(stream!=null)
			{
			GZIPInputStream fstream = new GZIPInputStream(stream);
			orderedSize = 0;
			for(DictionaryHash d : wordlen)
			{
				if(d!=null)
					{ d.load(fstream); 
					  orderedSize += d.size();
					}
			}
			fstream.close();
			stream.close();
			long later = G.nanoTime();
			String ss = G.format(" definitions %F seconds",(later-now)/1000000000.0);
			G.print("Loaded bulk dictionary ",orderedSize,ss );
			totalSize = orderedSize;
			definitionsLoaded = loaded = true;
			checkForCorruption();
			}
		}
		catch (IOException e)
		{
			throw G.Error("input file "+file+" %s",e);
	}
		catch (Throwable e)
		{
			G.print("Unexpected error loading bulk dictionary "+e);
		}
		}
	}
	public void loadDefinitions()
	{
		if(!definitionsLoaded)
		{
			definitionsLoaded = true;
			loadDefinitionsAlways(DictionaryDir+"worddefsa.txt.gz");
			// in onepass mode, we also did the inital load
			definitionsAllLoaded = loaded = true;
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

}
