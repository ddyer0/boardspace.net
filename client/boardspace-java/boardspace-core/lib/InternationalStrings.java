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

import java.awt.*;
import java.util.*;
import bridge.Config;
import bridge.JMenu;
import bridge.JMenuItem;

import java.io.IOException;
import java.io.InputStream;



/**
 * this class is the repository for translation strings and language support in the java applet.
 * <p>
 * all the user interface strings for each language are collected into one instance
 * of the class, which is loaded at runtime.  The class support a limited substitution
 * of variables into otherwise constant strings, and a few other tricks for specific languages.
 * <p>There is also a very simple line-splitter to break regular strings into short lines 
 * with a very limited hyphenation capability.
 * <p>
 * the key strings are <i>usually</i>just the English strings used in the programs, so the
 * code is readable.
 * <p>
 * In the key string, elements such as #1 #2 are variables.  The translation
 * strings should have the same variables, but not necessarily in the same other.
 * 
 * In the current model, strings are distributed in separate groups for the lobby
 * and for each game.  When a new game is added, it's translatable strings are
 * found in xxStrings and xxStringPairs in it's constants.java file.
 * 
 * In the "boardspace strings" project, the file MasterStrings.java contains references to 
 * all the games strings.  The "upload strings" program collects the strings and uploads them
 * to the online database, which serves as the master repository, both for strings from java
 * sources, and strings for dynamically generated web pages.   Translations of the strings
 * are managed by the translation manager online script.   After running upload
 * strings, run "download languages" which produces a set of text files, one per game. These
 * text files are incorporated into the lobby as resources.    To complete the delivery,
 * copy the string files to the delivery build area, currently "v101" and update the data
 * file resources for the mobile builds, in "bsdata.res"
 * 
 * Summary:
 * modify strings in the java sources.
 * add a reference to new strings in masterStrings.java 
 * run uploadStrings
 * run download languages
 * copy text files to v101
 * update bsdata.res with the new text files.
 * 
 * @author ddyer
 *
 */
public abstract class InternationalStrings implements Config
{ 
  public static String loadedLanguage = null;
  public static boolean hasDataFiles = StringsHaveDataFiles;
  public static String languages[] = SupportedLanguages;

  	public static String context = null;
  	public static void setContext(String str) { context = str; }
  	public static Hashtable<String,String> contextStrs = new Hashtable<String,String>(); 
  	public static Hashtable<String,String> strs = new Hashtable<String,String>();
    public static Hashtable<String,String> newkeys = new Hashtable<String,String>();
    
    public static void clearData()
    {
    	strs.clear();
    	contextStrs.clear();
    	newkeys.clear();
    }
    
    @SuppressWarnings("unchecked")
	public Hashtable<String,String> getTranslations()
		{ return((Hashtable<String,String>)G.clone(strs)); 
		}
    public Hashtable<String,String> getCaseInsensitiveTranslations()
    {
    	Hashtable<String,String>val = new Hashtable<String,String>();
    	for(Enumeration<String>keys = strs.keys(); keys.hasMoreElements(); )
    	{	String key = keys.nextElement();
    		String lcKey = key.toLowerCase();
    		String oldval = val.get(lcKey);
    		String newval = strs.get(key);
    		if(oldval!=null) 
    			{ Plog.log.addLog("Key ",key," already exists old: ",oldval," new: ",newval);
    			}
    		val.put(lcKey,newval);
    	}
    	return(val);
    }
    @SuppressWarnings("unchecked")
	public Hashtable<String,String> getContexts() 
		{ return((Hashtable<String,String>)G.clone(contextStrs)); 
		}


    /**
     * add a string and it's translation to the collection
     * 
     * @param key the key string (usually the English string from the program)
     * @param dat the translation string
     * @return the object added
     */
    public static String put(String key, String dat)
    {
       
        if(key!=null) 
        	{ 
        	
            if (G.debug() )
            {
                String old = strs.get(key);

                if (old != null && !old.equals(dat))
                {
                    System.out.println("Duplicate key for " + key);
                    System.out.println(" Old: " + old);
                    System.out.println(" New: " + dat);
                }
            }
     
        	  strs.put(key, dat);
        	  if(context!=null)
        	  { contextStrs.put(key,context);
        	  //G.print("C "+context+" "+key);
        	  }
        	}
        return (dat);
    }

    public static String put(String selfKey) { return(put(selfKey,selfKey)); }
    /**
     * add an array of strings as self keys
     * @param selfKey
     */
    public static void put(String selfKey[]) 
    {
    	for(String str : selfKey) { put(str,str); }
    }
    /** add an array of pairs of strings
     * 
     * @param pairs
     */
    public static void put(String [][]pairs)
    {
    	for(String str[] : pairs) { put(str[0],str[1]); }
    }
    /**
     * this is the standard line splitter for languages that use strings.  One special rule is that
     * the token <br> becomes a forced line break.
     * Japanese gets its own.
     * 
     * @param inStr
     * @param myFM
     * @param useWidth
     * @param Spaces is a string used as leading spaces for split lines.
     * @return a substitute string
     */
    public String realLineSplit(String inStr, FontMetrics myFM, int useWidth,
        String Spaces)
    {
        boolean first = true;
        StringTokenizer myST = new StringTokenizer(inStr);
        String AddedString = "";
        String currentStr = null;

        while (myST.hasMoreElements())
        {
            String nextElement = myST.nextToken();
            if("<br>".equals(nextElement)) 
            { if(currentStr!=null) { AddedString += currentStr;  currentStr = null; }
        	  AddedString += "\n";
            }
            else 
            {
            String nextStr = ((currentStr == null) ? "" : currentStr+" ") + nextElement;

            if (first || (myFM.stringWidth(nextStr) < useWidth))
            {
                currentStr = nextStr;
            }
            else
            {
            	if(currentStr!=null) { AddedString += currentStr; }
                AddedString += "\n" ;
                currentStr = new String(Spaces + nextElement);
            }}

            first = false;
        }

        if (!first && currentStr!=null)
        {
            AddedString += (currentStr + "\n");
        }

        return (AddedString);
    }
/**
 * this indirection is like for "split" below, to allow the 
 * specialized realLineSplit to be called.
 * @param inStr
 * @param myFM
 * @param useWidth
 * @param Spaces
 * @return a subscritute string
 */
     public String lineSplit(String inStr, FontMetrics myFM, int useWidth,
        String Spaces)
    {
        return (realLineSplit(inStr, myFM, useWidth, Spaces));
    }
/**
 * this indirection is like for "split" below, to allow the 
 * specialized realLineSplit to be called.
 * 
 * @param inStr
 * @param myFM
 * @param useWidth
 * @return A String
 */
     public String lineSplit(String inStr, FontMetrics myFM, int useWidth)
    {
        return (realLineSplit(inStr, myFM, useWidth, ""));
    }

  /**
   * Supersede this method to use a different splitting strategy
   * @param sub
   * @return a String
   */
    public String realSplit(String sub)
    {
        String val = strs.get("hyphenate " + sub);

        //System.out.println("Default split "+sub);
        if (val != null)
        {
            return (val);
        }

        return (sub);
    }
/**
 * this double substitution through realsplit is a trick to virtualize
 * the split method.  All the instances of various subclasses of 
 * internationalStrings are cast to plain internationalStrings, so they
 * all come here, not to the split method in the subclass.  This
 * dispatch to realSplit gets us to the real subclassed method
 * @param sub
 * @return a substitute string
 */
    public String split(String sub)
    {  return (realSplit(sub));
    }


    /**
     * this is a limited capability to translate where both the string
     * and the substitution arguments are part of the key string. This is
     * used in "translatable chat" messages.  Call s.getS("foo fred dave") to
     * be equivalent to s.get("foo","fred","dave")  where the translation of foo
     * is put("foo","the message for #1 from #2");
     * @param args
     * @return a substitute string with the argument inserted
     */
    // first word is the message key, the rest are single work arguments
    public String getS(String args)
    {	StringTokenizer at = new StringTokenizer(args);
    	String sub  = at.nextToken();
    	String base = get(sub);
    	for(int i=1;at.hasMoreTokens();i++)
    	{	String target = "#"+i;
    		int index = base.indexOf(target);
    		String tok = at.nextToken();
    		if(index>=0)
    		{ base = base.substring(0, index) + tok + base.substring(index + 2);
    		}
    		else
    		{ Plog.log.addLog("missing index for ",target," in \"",sub,"\"");
    		}
    	}
    	return(base);
    }
    // if into contains ## remove the ## and return the result
    // if into contains a #, substitute num at that position.
    // otherwise concatinate 
    //
    private String resub(String num,String into)
    {	int ind = into.indexOf('#');
    	if(ind>=0)
    	{	if((into.length()>ind) && (into.charAt(ind+1)=='#'))
    			{ return(into.substring(0,ind)+into.substring(ind+2)); 
    			}
    		return(into.substring(0, ind) + num + into.substring(ind+1));
    	}
    	else { return(num+into); }
    }
    /**
     * the pluralization scheme does the following
     * if "num" does not start with a digit, return num+alt.  This is probably an error in the string construction.
     * otherwise, the string is a list of 2 or more items, separated by commas.
     * the n'th item is resubstututed if if num=n,
     * if the default for "all other" numbers is the first item if there are only two items,
     * otherwise the last item.
     * resub substitutes the number into the item as follows.
     * if the item contains ##, remove it and return the rest. example "##no item" returns "no item"
     * if the item contains #, substutute the number at that position in the item and return it.
     * if the item doesn't contain a hashmark, return number+item
     * 
     * for example, {##no way,# way,# ways}   would return "no way" for 0, 1 way for 1, "2 ways" for 2 or any other
     * for example, {##first,##second,##third,#'th} would return first,second,third,4'th, 5'th etc.
     * @param num
     * @param alts
     * @return a pluralized string
     */
    // pluralization scheme
    public String pluralize(String num,String alts)
    {	if((num.length()>0) && (Character.isDigit(num.charAt(0)) || (num.charAt(0)=='-')))
    	{
    		int ordinal = 0;
    	   	int start = -1;
    	   	int comma = 0;
    	   	int firstcomma = -1;
	    	while( (comma=alts.indexOf(',',start+1))>=0)
	    	{
	    		if(num.equals(""+ordinal))
	    		{	// a match
	    			return(resub(num,alts.substring(start+1,comma)));
	    		}
	    		ordinal++;
	    		start = comma;
	    		if(firstcomma<0) { firstcomma = comma; }
	    	}
	    	// ran out of commas.  If ordinal is 2 use first, otherwise use last
	    	if((ordinal==1) && !"1".equals(num))
	    	{	// use the last string
	    		return(resub(num,alts.substring(0,firstcomma)));
	    	}
	    	else
	    	{	//if there are only 2 strings, use the first string for eveything except 1
	    		return(resub(num,alts.substring(start+1)));
	    	}
    	}
    	else { // not a number, probably an error
    		return(num+"{"+alts+"}");
    	}
    }
    public String get(String sub)
    {	if(sub==null) { return(sub); }
    	String str = strs.get(sub);
    	if(str==null) 
	    	{ str = sub;
	    	  newkeys.put(sub,sub);
			  strs.put(sub,sub);
			  Plog.log.addLog("adding key \"" , sub , "\"");
	     	}	// autokey for undefined
    	return(str);
    }
    /**
     * get finds occurrences of #n in the string and substututes the n'th argument.
     * ## marks a single #
     * #n{...} marks a special pluralization substring, {@link #pluralize}
     * @param sub
     * @param args optional args are printed using "format"
     * @return get a translated string, optionally with with substituted parameter
     */
    public String get(String sub, String... args)
    {	if(sub==null) { return(null); }
        String str = get(sub);
        return(subst(str,args));
    }
    
    /** substitute parameters in str after str has been found in the dictionary
     *  or when str is not supposed to be in the dictionary
     * @param str
     * @param args
     * @return
     */
    public String subst(String str,String...args)
    {
        for(int argn=1; argn<=args.length; argn++)
        {  String target = "#" + argn;
           int index = str.indexOf(target);
           // strings encoded with &#123; can look like #1 #2 etc.  This avoid them.
           // but it's still not completely correct
           while((index>0) && (str.charAt(index-1)=='&')) { index = str.indexOf(target,index+1); }
           if (index >= 0)
           {
        	String rest = str.substring(index + target.length());
        	String arg = args[argn-1];
        	// pluralize 
        	if((rest.length()>2) && (rest.charAt(0)=='{'))
        	{
        		int trail = rest.indexOf('}');
        		if(trail>0)
        		{
        			arg = pluralize(arg,rest.substring(1,trail));
        			rest = rest.substring(trail+1);
        		}
        	}
            str = (str.substring(0, index) + arg + rest);
           }
           else {   Plog.log.addLog("missing index for ",target, " in \"" , str , "\""); }
        }
        return (str);
    }
    /**
     * get with one integer argument, so save the ugliness of ""+n
     * @param sub
     * @param arg
     * @return a translated string
     */
    public String get(String sub,int arg0,int...arg)
    {  	String ss[] = new String[arg.length+1];
    	ss[0]=""+arg0;
    	for(int i=0;i<arg.length;i++) { ss[i+1]=""+arg[i]; }
    	return(get(sub,ss));
    }
    /**
     * this is a special variant of "get" where the first argument
     * is ignored silently if no # is found in the translation. 
     * This is useful for sets of translations where some are 
     * Enumerated "1 player" and others are not "cubes" 
     * @param str
     * @param arg
     * @return a translated string
     */
    public String get0or1(String str,int arg)
    {	if(str==null) { return(null); }
    	String text = get(str);
    	if(text.indexOf("#1")<0) { return(text); }
    	return(get(str,arg));
    }
    
    /**
     * this is a special variant of "get" where the first argument
     * is ignored silently if no # is found in the translation. 
     * This is useful for sets of translations where some are 
     * ennumetated "1 player" and others are not "cubes" 
     * @param str
     * @param arg
     * @return a translated string
     */
    public String get0or1(String str,String arg)
    {	if(str==null) { return(null); }
    	String text = get(str);
    	if(text.indexOf("#1")<0) { return(text); }
    	return(get(str,arg));
    }
    
    public String name = DefaultLanguageName;
    public synchronized void loadLanguage(String lang)
    {	context = null;
    	if(lang.equals(loadedLanguage) && (strs.size()>0)) { return; }
    	loadedLanguage = lang;
    	strs.clear();
    	readData(lang);
    }

    
    public void readData(String fileName)
    {	
		if(fileName!=null)
			{ boolean ok = readDataFile(LANGUAGEPATH+fileName+".data"); 
			  if(!ok && !"english".equals(fileName))
			  {
				  readDataFile(LANGUAGEPATH+"english.data"); 
			  }
			}
    }
    
 
    public boolean readDataFile(String name)
    {	if(hasDataFiles)
    	{
    	try
    	{
    		InputStream ins = G.getResourceAsStream(name);
    		readDataStream(ins);
    		return(true);
    	}
     	catch (IOException err) 
    	{ Plog.log.addLog("reading language data ",name," ",err.toString()); 
    	}}
    	return(false);
     }

     public void readDataStream(InputStream ins) throws IOException
    	{
    	//G.print("Read lang "+fileName+" as "+name+" with "+newsu);
        if(ins!=null)
        {
    	Utf8Reader breader = new Utf8Reader(ins);
    	try {
    	boolean eof=false;
    	String savedKey = null;
    	int linen = 0;
    	int stored = 0;
    	String prevVal = null;
    	while(!eof)
    		{
    		String line = breader.readLine();
    		linen++;
    		if(line==null) { eof = true; }
    		else if("".equals(line)) { savedKey = null; }
    		else {
       			// test for an ad hoc case
    			// char []seq = new char[] { 'x', '\\','u','0','0','9','2','\\','u','0','0','9','3','\\','u','0','0','9','4','x'};
       		    //			String str = new String(seq);
    			//line = G.utfDecode(str);
    			line = G.utfDecode(line);
        		if(line.length()==1) { line = line+" "; }	// guard against trailing blanks
    			String key = line.substring(0,2);
    			String val = line.substring(2);
    			if(key.equals("S "))
    			{	strs.put(val,val);
    				savedKey = null;
    				stored++;
    			}
    			else if(key.equals("K "))
    			{	savedKey = val;
    				prevVal = null;
    			}
    			else if(key.equals("V "))
    			{	if(savedKey==null) { throw G.Error("No saved key at line "+linen+" for value "+val); }
    				if(prevVal!=null) 
    					{ val = prevVal +"\n"+val; 	// weld continuation lines together
    					}
    					else 
    					{ stored++; 
    					}
    				strs.put(savedKey,val);
    				prevVal = val;
    			}
    			else if(key.equals("N "))
    			{	int expected = G.IntToken(val);
    				if(stored!=expected) 
    					{ G.print("Expected "+expected+" but got "+stored+" pairs"); 
    					}
    			}
    			else 
    			{ throw G.Error("Unexpected line "+linen+":"+line); }
    			}
    		}
    	}
    	finally {
    	breader.close();
    	}}
      }
     public static InternationalStrings initLanguage()
     {
         // initialize and load the language translations
         String lit = G.getString(G.LANGUAGE, DefaultLanguageName);
         return initLanguage(lit);
     }
     /**
      * create a new translations oject and initialize it with the
      * translations for some named language 
      * @param lit
      * @return
      */
     public static InternationalStrings initLanguage(String lit)
     {
         String languageClass = LANGUAGECLASS + lit+ "Strings";
         try {
         InternationalStrings s = (InternationalStrings) (G.MakeInstance(languageClass));
     	 s.readData(s.name);
     	 G.setTranslations(s);
     	 return(s);
         }
         catch (Throwable err)
         {
 			 Plog.log.addLog("Language ",lit," ",err);
 			 if(!"english".equalsIgnoreCase(lit))
 			 {
 				 G.putGlobal(G.LANGUAGE,"english");
 				 return initLanguage();
 			 }
         }
         return((InternationalStrings)G.MakeInstance(DefaultLanguageClass));
     }
     
     public static void addLanguageNames(JMenu langField,DeferredEventManager ev)
     {	String current = G.getString(G.LANGUAGE, DefaultLanguageName);
     	InternationalStrings s = G.getTranslations();
     	langField.removeAll();
     	JMenuItem m = new JMenuItem(s.get(current),current);
     	langField.add(m);
     	for(String lang : languages) 
			 { if(!lang.equalsIgnoreCase(current))
				 {JMenuItem mi = new JMenuItem(s.get(lang),lang);
			      mi.addActionListener(ev);
			 	  langField.add(mi);
				 }
			 }
     }
     public static boolean selectLanguage(JMenu m,Object target,DeferredEventManager ev)
     {
    	 if(m!=null)
    	 {
    		 int nItems = m.getItemCount();
    		 for(int i = 0; i<nItems; i++)
    		 {	JMenuItem item = (JMenuItem)m.getItem(i);
     		    if(item==target)
    	    		{	
    			 	String old = G.getString(G.LANGUAGE, DefaultLanguageName);
    			 	String newl = item.getValue();
    			 	if(!old.equals(newl)) 
    			 	{
    			 		G.putGlobal(G.LANGUAGE,newl);
    		        	prefs.put(langKey,newl);
    		        	initLanguage();
    			 		addLanguageNames(m,ev);
    			 	}
    			 	return(true);
    	    		}
    		 }}
    	 	return(false);
     }
}