package crosswordle;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import com.codename1.io.BufferedInputStream;
import com.codename1.io.gzip.GZIPInputStream;

import bridge.File;
import bridge.FileOutputStream;
import dictionary.Dictionary;
import dictionary.DictionaryHash;
import dictionary.Entry;
import lib.G;
import lib.Random;
import lib.StringStack;
/**
 * this class builds "dense" crosswords - with no blanks - on a nxk grid.  It's
 * mostly intended as a source for "crosswordle" puzzles.    It's fast enough
 * that it can generate a unique puzzle from a random seed in realtime.
 * 
 * The algorithm fills the rows randomly with candidate words.  At each step, each of
 * the columns is vetted that the vertical prefix exists in the candidate dictionary
 * before continuing to the next row.  At the last row, the column must be a valid word.
 * 
 * The key to making this fast was using a sorted word list, and doing a binary search
 * to determine if the column prefix exists.
 * 
 * Despite the speed of the algorithm, some random keys point to very sparse areas
 * in the search space, so there is an arbitraty limit to the number of search steps,
 * after which the search aborts and a new one is started.
 * 
 * @author ddyer
 *
 */
public class Builder implements CrosswordleConstants
{
	private static Builder instance = null;
	public static Builder getInstance() 
	{
		if(instance==null)
			{ instance = new Builder(); 
			  instance.loadDefinitions();
			}
		return instance;
	}
	 int inverses = 0;
	 private boolean CollectAll = false;
	 private long steps = 0;						// search steps building the current puzzle
	 private int LIMIT = 200000000;				// limit of steps before abandoning a search.
	 											// 200,000,000 is about 3 seconds on a fast machine. 
	 private double partDone = 0;				// crude estimate of the % of the space searched so far
	 											// most searches end at 0%, meaning that the first word
	 											// in the row 1 list is used.
	 private int iPartDone = 0;
	 
	 private String[][] acrossDictionary = null;// each row gets its own randomly shuffled copy of the dictionary
	 											// this make sure the puzzles are independently random even though
	 											// the words in the dictionary are tried in a fixed order.
	 private Hashtable<String,String>dups = null;	// words used during the descent, no duplicates!
	 												// unrestricted duplicates tends to result in "palindrome"
	 												// puzzles that are the same across and down.
	 private char grid[][] = null;					// working storage, the grid being built
	 private String verticalWordList[] = null;		// the vertical word list, sorted
	 private int verticalWordVocabulary = 0;
	 private StringBuilder results = new StringBuilder();	// result puzzles, separated by spaces
	 private Hashtable<String,Long>dupResults = new Hashtable<String,Long>();
	 private int seq = 0;							// sequence number for output files
	 
	 int vocabulary = 25000;
	 	// 25576;// 99999;//
	 	// 25576;	corresponds to "robotVocabulary" yields about 4000 5 letter words
	 									// and 1210 valid grids (excluding inverses, which doubles that)
	 private int nrows = 5;
	 private int ncols = 5;
	 Dictionary dictionary =  Dictionary.getInstance();
	 /*
	  * for a 5x5 crossword, the default vocabulary 
	  * 1167 has no results. robotVocabulary = 7721
	  * 2490 has (estimated) robotVocabulary = 20576 hundreds to thousands of results, at least 10% duplicates
	  * 2995 for robotVocabulary 25576, 
	  * 3990 for 36008 too many to count
	  * 
	  * full vocabulary (about 8000 words) yields 47415 puzzles (excluding inverses)
	  * 	using 4093 distinct words max 3936
	  * medium vocabulary (about 4000 words) yields 1211 puzzles (excluding inverses)
	  * 	using 1930 distinct words max 90 occurences
	  * small vocabulary (about 2000 words) yields no puzzles
	  * 
	  * 6x6 vocabulary full list yields 23,000 words, many solutions, 50% time expired, no duplicates, but many obscure words
	  * 6x6 vertical list 3811 for 25000 few solutions 
	  * 				5094 for 35000 few solutions
	  * 				5526 for 45000 few solutions
	  */
	 /*
	  * for 6x5 , vocab 45000, horizontal word list is 5526, vertical word list is 4216
	  * and about 1000 puzzles are available (random search) 1615 complete
	  * 
	  * vocab 35000, horizontal word list is 5094, vertical word list is 3889
	  * exactly 708 puzzles available
	  */
	 
	 /**
	  * generate a puzzle based on the seed, nc and nr
	  * 
	  * @param seed
	  * @param nc columns
	  * @param nr rows
	  */
	 public String generateCrosswords(long seed,int nc,int nr)
	 {	Random r = new Random(seed);
	 	results = new StringBuilder();
	 	int dups = 0;
	 	int fails = 0;
	 	int consecutiveDups = 0;
	 	inverses = 0;
	 	vocabulary = 45000;
	 	CollectAll = true;
		double totalTime = 0;
		if(CollectAll) 
			{ 
		 	  long key = r.nextLong();
		 	  generate1Crossword(key,nc,nr); 
			}
		else {
		 // collect random puzzles, watch for duplicates
		 for(int i=1;consecutiveDups<50 && i<= 10000; i++ ) 
		 	{ long now =G.Date();
		 	  long key = r.nextLong();
		 	  String result = generate1Crossword(key,nc,nr); 
		 	  long later = G.Date();
		 	  totalTime += later-now;
		 	   if(result==null) { i--; fails++; consecutiveDups++; }
		 	   else if(dupResults.get(result)!=null) { i--; consecutiveDups++; dups++; G.print("Duplicate ",key,"\n",result); }
		 	   else
		 	   {  dupResults.put(result,key);
		 	      consecutiveDups=0;
		 	   	  results.append(getCompactResult()); 
		 		  G.print("#",i," ",(later-now)/1000," seconds ",key,"\n",result);
		 		  if(i%100==0)
		 		  {	 seq++;
		 			 File out = new File(G.concat("crossword ",ncols,"x",nrows,"-",seq,".txt"));
		 			 G.print("dups ",dups," fails ",fails," average time ",totalTime/(i*1000));
		 			 saveResultToFile(out,results.toString());
		 		  }
		 	   }
		 	}}
		 G.print("result: "+results.toString());
		 seq++;
		 File out = new File(G.concat("crossword ",ncols,"x",nrows,"-",seq,".txt"));
		 G.print("File: ",out);

		 String result = results.toString();
		 saveResultToFile(out,result);
		 
		 return result;
	 }

	public void saveResultToFile(File out,String result)
	{
		 try {
		     FileOutputStream fs = new FileOutputStream(out);
			 PrintStream os = new PrintStream(fs);
			 os.print(result.toString());
			 os.print("\n");
			 os.flush();
			 fs.close();
		 }
		 catch (IOException err) { G.print("IOError "+err); }
	}
	private String getCompactResult()
	{	StringBuilder str = new StringBuilder();
		for(int row = 0;row<grid.length;row++)
		 {	char g[] = grid[row];
			for(int cha = 0; cha<g.length;cha++)
			{	char ch = g[cha];
				str.append(ch);
			}
		 }
		 str.append(" ");
		 return str.toString();
	}
	
	public String getDaysPuzzle(long now,int cols,int rows,boolean hard)
	{
		long date = (now / (1000*60*60*24));	// divide by milliseconds in a day
		String base[] = getPuzzleList(cols,rows,hard);
		if(base!=null)
		{
			int index = (int)(date%base.length);
			G.print(PuzzleN,index," ",base[index]);
			return base[index];
		
		}
		return null;
	}
	
	public String[] getPuzzleList(int cols,int rows,boolean hard)
	{
		switch(cols*rows)
			{
			default: break;
		case 25:
			return hard ? crosswords_5x5_hard : crosswords_5x5_medium;
		case 30: 
			return hard ? crosswords_6x5_hard : crosswords_6x5_medium;
		}
		return null;
	}
	 public String generate1Crossword(long seed,int nc,int nr)
	 {	Random r = new Random(seed);
	 
	 	int oldNrows = nrows;
	 	ncols = nc;
	 	nrows = nr;
	// 	if(nc>=6 && nr>=6) { vocabulary = 9999999; }	//solution space is sparse
	 	
	 	if(oldNrows!=nrows) { verticalWordList = null; }
	 	steps = 0;
	 	acrossDictionary = new String[nrows][];
	 	dups =new Hashtable<String,String>();
	 	grid = new char[nrows][ncols];
	 	boolean success = placeAcross(r,0);		// this does the actual building by recursive descent
	 	
	 	G.print("Steps = ",+steps," ",success," @ ",(int)(partDone),"%");
	 	
	 	if(success)
		 {
			 return getGrid();
			 
		 } else { 
			 G.print("No result for ",seed);
			 return(null);
		 }
		 

	 }
	 private String[] getRamdomizedSubdictionary(Random r,int size,int row)
	 {	 
		 String dicts[][] = acrossDictionary;

	 	 if(dicts[row]==null)
	 	 {
		 DictionaryHash dict = dictionary.getSubdictionary(size);
		 StringStack entries = dict.toStringStack(vocabulary);
		 entries.shuffle(r);
		 dicts[row] = entries.toArray();
	 	 }
		 return dicts[row];
		 
	 }
	 
	 // the vertical word list is sorted, so we can probe vertical words
	 // as they are built, to see if the prefix exists
	 private String[] getVerticalWordList()
	 {
		 if((verticalWordList==null)||(verticalWordVocabulary!=vocabulary))
		 {	StringStack list = dictionary.getSubdictionary(nrows).toStringStack(vocabulary);		 
		 	list.sort();
		 	verticalWordList = list.toArray();
		 	verticalWordVocabulary = vocabulary;
		 	G.print("Vertical list ",verticalWordList.length," for ",vocabulary);
		 }
		 return verticalWordList;
	 }
	 
	 // clear a horizontal row
	 private void reEmpty(char row[])
		{
			for(int i=0,lim=row.length; i<lim; i++) { row[i] = (char)0; }
		}
	 
	 // place a horizontal word
	 private void placeWord(char row[],String word)
		{	
			for(int i=0,lim=word.length(); i<lim; i++)
			{	row[i] = word.charAt(i);
			}
		}
	 
	 // get the current grid in a somewhat readable format
	 public String getGrid()
	 {
		 StringBuilder b = new StringBuilder();
		 char all[][] = grid; 
		 for(int rown = 0; rown<all.length; rown++)
		 {
			 for(char ch : all[rown]) { b.append(' '); b.append(ch==0 ? ' ' : ch); }
			 b.append('\n');
		 }
		 return b.toString();
	 }
	 
	 // get the current grid, swapping rows and columns
	 // for puzzles where rows=cols, the swapping rows and columns will also be a solution
	 // but isn't really a different puzzle
	 public String getReverseGrid()
	 {
		 StringBuilder b = new StringBuilder();
		 char all[][] = grid; 
		 for(int coln = 0;coln<ncols; coln++)
		 {
			 for(int rown=1; rown<=nrows; rown++)
			 {
				 char ch = all[rown-1][coln];
				 b.append(' ');
				 b.append(ch);
				 
			 }
			 b.append('\n');
		 }
		 return b.toString();
	 }
	 
	 // recursive descent, place a word across, check prefixes, and if
	 // still a possible solution, continue down
	 private boolean placeAcross(Random r,int rowN)
	 {	 String wordlist[] = getRamdomizedSubdictionary(r,ncols,rowN);
	 	 char all[][] = grid;
	 	 steps++;
	 	 //if(steps%1000000==0) { G.print("Step ",steps," ",partDone,"%\n",getGrid());}
	  	 for(int i=0,lim=wordlist.length; i<lim; i++)
	 	 {	String word = wordlist[i];
	 	 	if(dups.get(word)==null)
	 	 	{
	 	 	placeWord(all[rowN],word);
	 	 	 steps++;
	 	 	 if(!CollectAll && steps>LIMIT) { return false; }
	 	 	 //if(steps%1000000==0) { G.print("Step ",steps," ",partDone,"%\n",getGrid());}
	 	 	dups.put(word,word);
	 		boolean success = rowN+1==all.length 
	 				? checkDownComplete() 
	 				: checkDownPossible(getVerticalWordList()) && placeAcross(r,rowN+1);
	 		if(success) { return true; }
	 		dups.remove(word);
	 		if(rowN==0) 
	 			{ partDone = i*100.0/lim; 
	 			  int ip = iPartDone;
	 			  iPartDone = (int)partDone;
	 			  if(ip!=iPartDone) { G.print(iPartDone,"%"); }
	 			}
	 		reEmpty(all[rowN]);
	  	 	}}
	 	 return(false);
	 }
	 // not a general purpose comparison
	 // target is known to be shorter than probe
	 private int compareStrings(String target,String probe)
	 {
		 int tlen = target.length();
		 for(int i=0;i<tlen; i++)
		 {
			 char tchar = target.charAt(i);
			 char pchar = probe.charAt(i);
			 if(tchar<pchar) { return -1; }
			 if(tchar>pchar) { return 1; }
		 }
		 return 0;
	 }
	 
	 // binary search for a matching prefix
	 private String findPrefixWord(String target,String []wordList)
	 {
		 int min = 0;
		 int max = wordList.length-1;
		 while (min<max)
		 {	int probeIdx = (min+max)/2;
		 	if(probeIdx==min)
		 	{
		 		return null;
		 	}
			 String probe = wordList[probeIdx];
			 int direction = compareStrings(target,probe);
			 if(direction==0) 
			 	{ return probe; }
			 if(direction<0) 
			 	{ max = probeIdx; }
			 else 
			 	{ min = probeIdx; }
		 }
		 return null;
	 }
	 
	 // check the vertical words, that each word is a prefix of some word
	 // in the dictionary
	 private boolean checkDownPossible(String wordlist[])
	 {	char all[][] = grid;
	 	for(int col=0,lim=all[0].length; col<lim;col++)
	 	{	String word = getVerticalWord(all,col);
	 		String target = findPrefixWord(word,getVerticalWordList());
	 		boolean ok = target!=null;
	 		/**
	 		boolean ok = findClosest
	 		for(int i=0;!ok && i<wordlist.length;i++)
	 		{ 	String w = wordlist[i];
	 			ok |= canPlaceWord(all,col,w) && (dups.get(w)==null);
	 		} */
	 		if(!ok) 
	 			{ //G.print("Not ",col,"\n",getGrid());
	 			  return false; 
	 			}
	 	}
	 	//G.print("Yes\n",getGrid());
		return true;
	 }
	 
	 private StringStack localDups = new StringStack();
	 // check that each of the vertical words is in the dictionary
	 private boolean checkDownComplete()
	 {	char all[][] = grid;
	 	localDups.clear();
	 	//G.print(getGrid());
	 	for(int col=0,lim=all[0].length; col<lim;col++)
	 	{
	 		String w = getVerticalWord(all,col);
	 		if(dups.get(w)!=null || localDups.contains(w)) { return false; }
	 		localDups.push(w);
	 		Entry e = dictionary.get(w);
	 		if((e==null) || (e.order>=vocabulary))
	 		{ 	
	 			//G.print("Step ",steps," col ",col,"\n",getGrid());
	 			return false; 
	 		}
	 	}
	 	if(CollectAll)
	 	{	String grid = getGrid();
	 		if(dupResults.get(grid)!=null) { 
	 			G.print("Unexpected duplicate: ",grid);
	 		}
	 	 	if(nrows==ncols)
 	 		{ String rev = getReverseGrid();
 	 		  if(dupResults.get(rev)!=null)
 	 		  	{ G.print("inverse\n",rev); inverses++; grid = null; }
 	 		  
 	 		}
	 	 	if(grid!=null)
	 	 	{
	 		dupResults.put(grid,steps);
	 		results.append(getCompactResult()); 
	 		results.append(" ");
	 		int sz = dupResults.size();
	 		G.print("#",sz," inv ",inverses," ",(int)partDone,"%\n",grid);
	 		if(sz%100==0)
	 		{	 seq++;
	 			 File out = new File(G.concat("crossword ",ncols,"x",nrows,"-",seq,".txt"));
	 			 saveResultToFile(out,results.toString());
	 		}}
	 		return false;
	 	}
		return true;
	 }
	 // get a vertical word in some column
	 private String getVerticalWord(char all[][],int col)
	 {	StringBuilder b = new StringBuilder();
	 	for(int i=0,lim=all.length; i<lim; i++)
	 	{	char c = all[i][col];
	 		if(c!=0) {  b.append(c); }
	 	}
		return b.toString();
	 }
	 // get a vertical word in some column
	 private String getHorizontalWord(char all[][],int row)
	 {	StringBuilder b = new StringBuilder();
	 	char r[] = all[row];
	 	for(char ch : r)
	 	{
	 		b.append(ch);
	 	}
		return b.toString();
	 }
	  
	 static boolean definitionsLoaded = false;
	 static boolean loaded = false;
	 static String crosswords_5x5_medium[] = null;
	 static String crosswords_5x5_hard[] = null;
	 static String crosswords_5x5_hard_only[] = null;
	 static String crosswords_6x5_medium[] = null;
	 static String crosswords_6x5_hard[] = null;
	 
	 static String CrosswordleDefinitionsDir = "/crosswordle/images/";
	 
	@SuppressWarnings("unused")
	private String[] difference(String main[],String remove[])
	 {
		 StringStack mainSet = new StringStack();
		 HashSet<String>removeSet =new HashSet<String>();
		 for(String str : remove) { removeSet.add(str);}
		 for(String str :main) { if(!removeSet.contains(str)) { mainSet.addElement(str); }}
		 String []val = mainSet.toArray();
		 G.print("main ",main.length," remove ",remove.length," result ",val.length);
		 return val;
	 }
		public void loadDefinitions()
		{	
			if(!loaded)
			{
			crosswords_5x5_medium = load("crossword-5x5-medium-complete.txt.gz");
			crosswords_5x5_hard = load("crossword-5x5-difficult-only.txt.gz");
			crosswords_6x5_medium = load("crossword-6x5-medium.txt.gz");
			
			crosswords_6x5_hard = load("crossword-6x5-hard.txt.gz");
			
			//save("new-crossword-6x5-medium-complete.txt",crosswords_6x5_medium);
			//String hard_only[] = difference(crosswords_6x5_hard,crosswords_6x5_medium);
			//save("new-crossword-6x5-hard-only.txt",hard_only);
			
			//wordStats(crosswords_5x5_medium,5,5);
			//crosswords_5x5_hard = load("crossword-5x5-difficult-complete.txt");
			//wordStats(crosswords_5x5_hard,5,5);
			//crosswords_5x5_hard_only = difference(crosswords_5x5_hard,crosswords_5x5_medium);
			//randomize and save the word list
			//save("new-crossword-5x5-medium-complete.txt",crosswords_5x5_medium);
			//save("new-crossword-5x5-difficult-complete.txt",crosswords_5x5_hard);
			//save("new-crossword-5x5-difficult-only.txt",crosswords_5x5_hard_only);
			loaded = true;
			}
		}
		
		private boolean isAlphabetic(int a)
		{
			return (a>='A' && a<='Z') || (a>='a' && a<='z');
		}
		
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

		private String[] load(BufferedInputStream stream) throws IOException
		{
			StringStack puzzles = new StringStack();
			String msg = null;
			while( (msg = readToken(stream))!=null)
			{	puzzles.push(msg);
			}
			
			return puzzles.toArray();
		}
		private class freq implements Comparable<freq>{
			String name;
			int count;
			boolean alist;
			boolean blist;
			public int compareTo(freq o) {
				return count-o.count;
			}
			freq(String s,int c,boolean a) 
				{ name=s; count=c;
				  if(a) { alist=true; } else {blist=true; }
				}
		}
		/*
		 * load the contents of a file. If extensions, load only the words
		 * that are extensions of existing words.
		 * 
		 */
		public String[] load(String file)
		{	String []loaded = null;
			try {
				G.print("Loading "+file);
				InputStream rawStream = G.getResourceAsStream(CrosswordleDefinitionsDir+file);
				if(rawStream!=null)
				{
				BufferedInputStream stream = new BufferedInputStream(file.endsWith(".gz")
								? new GZIPInputStream(rawStream)
								: rawStream);
				loaded = load(stream);
				rawStream.close();
				}
				else { G.Error("Resource %s not found",file); }
			} catch (IOException  e) {
				G.Error("error reading %s %s",file,e);
			}
			return(loaded);
		}
		
		// randomize the order and save a new copy of the word list
		@SuppressWarnings("unused")
		private void save(String file,String[]v)
		{	StringBuilder b = new StringBuilder();
			new Random().shuffle(v);
			for(int i=0;i<v.length;i++)
			{
				b.append(v[i]);
				b.append('\n');
			}
			saveResultToFile(new File(file),b.toString());
		}
		private void accumulateStats(Hashtable<String,freq> h, String w,boolean a)
		{	G.Assert(dictionary.get(w)!=null,"is a word");
			freq old = h.get(w);
			if(old==null)
				{ old = new freq(w,1,a);
				  h.put(w,old);
				}
			else 
				{ old.count++; 
				  if(a) { old.alist=true; } else { old.blist=true; }
				}
		}
		
		private Hashtable<String,freq> h = new Hashtable<String,freq>();
		@SuppressWarnings("unused")
		private void wordStats(String puzzles[],int ncols,int nrows)
		{	boolean isAlist = h.size()==0;
			char[][]grid = new char[nrows][ncols];
			for(int i=0;i<puzzles.length;i++)
			{
				loadPuzzle(grid,puzzles[i]);
				for(int col = 0; col<ncols; col++)
				{
					accumulateStats(h,getVerticalWord(grid,col),isAlist);
				}
				for(int row=0;row<nrows;row++)
				{
					accumulateStats(h,getHorizontalWord(grid,row),isAlist);
				}
			}
			int max = 0;
			String winner = null;
			freq stor[] = new freq[h.size()];
			int idx=0;
			for(Enumeration<String> e = h.keys(); e.hasMoreElements();)
			{	String w =e.nextElement();
				freq val = h.get(w);
				int count = val.count;
				stor[idx++] = val;
				if(count>max)
				{
					max = count;
					winner = w;
				}
			}
			Arrays.sort(stor);
			
			G.print("distinct words: ",h.size()," max is ",winner,":",max);
			
			for(int i=0;i<stor.length;i++)
			{
				freq w = stor[i];
				G.print(w.name," ",w.count," ",w.alist ? "A":"",w.blist?"B":"");
			}
		}
		private void loadPuzzle(char [][]grid,String puzzle)
		{	int idx = 0;
			for(int row = 0; row<nrows; row++)
			{
				for(int col=0;col<ncols; col++)
				{
					grid[row][col] = puzzle.charAt(idx++);
				}
			}
		}
		
}
