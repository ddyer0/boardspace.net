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
package language;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;
import java.util.prefs.Preferences;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import lib.G;
import lib.Tokenizer;

class ValueSet
{
	String value;
	String group;
	String context;
	String comment;
	ValueSet next;
	ValueSet(boolean decode,String v,String g,String cv,String cm,ValueSet n)
	{
		value = decode ? G.utfDecode(v) : v;
		group = decode ? G.utfDecode(g) : g;
		context = decode ? G.utfDecode(cv) : cv;
		comment = decode ? G.utfDecode(cm):cm;
		next = n;
	}
}
/**
 * this is the downloader that downloads the contents of masterStrings from a database.  
 * it's only relevant to maintaining the translation strings for a live web site
 * and using it requires that you have an active mysql server set up somewhere.
 * 
 * @author ddyer
 *
 */
public class downloadLanguages {
	static Session session = null;

	  public static Connection  connect(String host,String database,String user,String pass) throws SQLException
	  {	String url = "jdbc:mysql://"+host+"/"+database;
	  	return(DriverManager.getConnection(url, user, pass));	  
	  }
	  // ssh connection
	  public static Connection  connectWithSSH(String host,String sshUser,int sshPort,String sshPass,
			  String database,String user,String pass) throws SQLException
	  {	 session = null;
	  	Connection conn = null;
	  	try {
	  	  int lport = 5656;
	  	  int rport = 3306;	// normal mysql port
		  JSch jsch = new JSch();
		  java.util.Properties config = new java.util.Properties(); 
	  	  config.put("StrictHostKeyChecking", "no");

		  session=jsch.getSession(sshUser, host, sshPort);	// 9130 is the boardspace.net ssh port
	      session.setPassword(sshPass);
	      session.setConfig(config);
	      session.connect();
	      System.out.println("ssh Connected");
	      session.setPortForwardingL(lport, "localhost", rport);
	      System.out.println("Port Forwarded");
	    	
	      //mysql database connectivity
	      //
	      //
	      // note may 8 2024, the getConnection started failing with a complaint
	      // about "CDT" being an undefined or ambigous time zone.  Adding ?serverTimeZone
	      // as suggested by StackOverflow seems to fix the problem.
	      //
	      String url = "jdbc:mysql://localhost:"+lport+"/"+database
	    		  + "?serverTimezone=UTC";
	      conn = DriverManager.getConnection(url, user, pass);

	      System.out.println ("Database connection established");
	      System.out.println("DONE");

	  	} catch (Throwable err)
	  	{
	  		System.out.println("connection error "+err);
	  	}
	  	return(conn);	  
	  }	  

	  
	  // normally we are downloading the strings used by the app, and don't want web strings
	  // when trying to dump the entire database, -includeweb true
	  // the odd duck in this is "japanese" which is both a language name and a web key
	  //
	  private static Hashtable<String,ValueSet> downloadStrings(Connection conn,String language,boolean includeWeb)
			  		throws SQLException
	  {	Hashtable<String,ValueSet> trans = new Hashtable<String,ValueSet>();
	    PreparedStatement ret = conn.prepareStatement("select keystring,translation,context,comment,collection from translation where language=?" 
	    				+ (includeWeb ? "" : " and collection!='web' or keystring=language"));
	    ret.setString(1,language);
	    if(ret.execute())
	    {
	    	ResultSet result = ret.getResultSet();
	    	while (result.next())
	    	{
	    		String key = G.utfDecode(result.getString(1));
	    		String value = result.getString(2);
	    		String context = result.getString(3);
	    		String comment = result.getString(4);
	    		String collection = result.getString(5);
	    		// this is based on the observation that the best and least
	    		// confusing way to present language names is "in its own language".
	    		// so while the database conains translations for the languages to the 
	    		// translated language, the data file will always contain each language
	    		// translated into its native spelling.
	    		if(key!=null)
	    			{ trans.put(key,new ValueSet(true,value,collection,context,comment,trans.get(key)));    
	    			
	    			}
	    	};
	    	
	    }
	   	return(trans);
	  }	 
	  static String utfEncode(String str)
	  {		int nchars = str.length();
	  		StringBuffer out = new StringBuffer();
	  		int idx = 0;
	  		while(idx < nchars)
		  		{
		  		char ch = str.charAt(idx++);
		  		if((ch!='\\') && (ch<128)) { out.append(ch); }
		  		else { 
		  			//have a look at this chart (expand "latin 1 suppliment" and 
		  			//"latin suppliment a"
		  			//http://inamidst.com/stuff/unidata/
		  			//the character you're using is this one:
		  			//http://www.fileformat.info/info/unicode/char/009E/index.htm
		  			//but I think you intended this one:
		  			//http://www.fileformat.info/info/unicode/char/017E/index.htm
		  			//they look much the same.  How does inputting these
		  			//characters work for you?  I have no idea how to type them.

		  			// ad hoc adjustment, unichode \u009e is used instead of \u017e
		  			// this resulted in blobs in czech
		  			   if(ch==0x009e) { ch=(char)(0x017e); }
		  			   String chstring = Integer.toHexString(ch);
			  	  	   out.append( "\\u0000".substring(0,6-chstring.length()));	// leading zeros 
			  	  	   out.append(chstring);
		  		}

			 }
			 return(out.toString());
	  }  
	  
	  static public String[] split(String msg,char ch)
	    {	return(split(msg,ch,0));
	    }
	    // recursive split depth first
	  static private String[] split(String msg,char ch,int depth)
	    {
	    	int idx = msg.indexOf(ch);
	    	if((idx < 0) || (idx==msg.length()-1))
	    		{ String res[] = new String[depth+1];
	    		  res[depth] = (idx<=0) ? msg : msg.substring(0,idx);
	    		  return(res);
	    		}
	    	else
	    	{	String [] res = split(msg.substring(idx+1),ch,depth+1);
	    		res[depth] = msg.substring(0,idx);
	        	return(res);
	    	}
	    }
	  public static void writeMLvalue(Writer out,String prefix, String val) throws IOException
	  {
		  if(val!=null && !"".equals(val))
		  {
		  String spl[] = split(val,'\n');
		  for(String str : spl)
		  {	  String enc = utfEncode(str);
			  out.write(prefix);
			  out.write(enc);
			  out.write("\n");
		  }}
	  }
	  /*
	   * K — key (English source, or the symbolic key)
G — group/collection
C — context, web-collection English rows only, cgi source location
M — comment, free-form, any row
V — translation/value
S — untranslated (bare English, no translation yet)
N — trailing count
	   */
	  public static void printKey(Writer out,boolean includeWeb,String key,ValueSet v) throws IOException
	  {	String uKey = utfEncode(key);
	  	String val = v.value;
	  	if(key.indexOf('\n')>=0)
	  		{ System.out.println("line break not allowed in key "+key); 
	  		}
	  	else if(key.equals(val))
		  {
			  out.write("S "+uKey);
			  out.write("\n");
		  }
	  	else
	  	{
	  		  out.write("K "+uKey);
			  out.write("\n");
			  writeMLvalue(out,"V ",val);
	  	
			 
	 	}
	  	if(includeWeb)
	  	{
	  		writeMLvalue(out,"G ",v.group);
	  		writeMLvalue(out,"C ",v.context);
	  		writeMLvalue(out,"M ",v.comment);
	  	}
	  }
	  /**
	   * this checks that the #1 #2 etc in translations are the same as they are in english.
	   * technically, this ought to be done in the translation interface, but this is a backup
	   * @param name
	   * @param key
	   * @param english
	   * @param translation
	   */
	  private static void checkPlaceholders(String name,String key,String english,String translation)
	  {	  int len = english.length();
		  for(int index=-1;  index<len && (index=english.indexOf('#',index))>=0 && index+1<len;index++)
		  	{
			  char ch = english.charAt(index+1);
			  if(ch=='#') { index += 2; }
			  else {
			  String xl = "#"+ch;
			  if(translation.indexOf(xl)<0)
			  {
				  G.print("\n",name+" transation of ",key," is missing placeholder "+xl);
				  if(!key.equals(english)) { G.print(english); }
				  G.print(translation);
			  }}
		  	}
	  }
	  
	  public static void saveStrings(String name,boolean includeWeb,Hashtable<String,ValueSet>langKeys,Hashtable<String,ValueSet>backupKeys)
	  {		if(backupKeys==null) { backupKeys = langKeys; }
			try
			{
			 FileOutputStream  out1 = new FileOutputStream (name);
			 Writer out = new OutputStreamWriter(out1, "UTF8");
			 String keyarr[] = new String[backupKeys.size()];

			 int i=0;
			 for(Enumeration<String> keys = backupKeys.keys(); keys.hasMoreElements();)
			  	{	keyarr[i++] = keys.nextElement();
			  	}	
			 Arrays.sort(keyarr);	// sort the list so reading it will be rational
			 int printed = 0;
			 for(i=0;i<keyarr.length;i++)
			 {	String key = keyarr[i];
				ValueSet v = langKeys.get(key);
				String val = v==null ? null : v.value;
				ValueSet backv = backupKeys.get(key);
				String backval = backv==null ? null : backv.value;
				if(val==null) { v = backv; val=backval; }
				else { checkPlaceholders(name,key,backval,val); }
				
				// always save languages with translations in their own language
	    		String subst = languageNameTranslations.get(key);
	    		if(subst!=null) { 
	    				val = subst;
	    			}
	    		while(v!=null)
	    		{
				printKey(out,includeWeb,key,v);
				printed++;
				v = v.next;
	    		}
				}
			 out.write("N "+printed+"\n");
			 out.close();
			 System.out.println(name + " "+printed);
			}	
			catch (IOException err)
			{
				System.out.println("Output exception "+ err.toString());
			}

	  }
	  
	  //
	  // this will hold the mapping of a language to its name in its own language.
	  //
	  static Hashtable<String,String>languageNameTranslations = new Hashtable<String,String>();
	  
	  public static String[] getLanguages(Connection conn) throws SQLException
	  {		  		
	  // new version, get the languages list from the actual field
	  		PreparedStatement ret = conn.prepareStatement("show columns from translation where field='language'");
	  		if(ret.execute())
	  		{
	    	ResultSet result = ret.getResultSet();
	    	result.next();
	    	String all = result.getString(2);
	    	
	    	return buildLanguageList(conn,all);
	  		}
	  		else
	  		return null;
	  		}
	  public static String[] buildLanguageList(Connection conn,String all)  throws SQLException
	  {
		  Vector<String>languages=new Vector<String>();	
	  		languageNameTranslations.clear();
	    	
	    	int start = all.indexOf('(');
	    	int end = all.indexOf(')');
	    	String things = all.substring(start+1,end);
	    	
	    	String many[] = things.split(",");
		    PreparedStatement gettrans = conn.prepareStatement("select translation from translation where language=? and keystring=?");
	    	for(String m : many)
	    	{	String lname = m.substring(1,m.length()-1);
	    		languages.addElement(lname);
	    		gettrans.setString(1,lname);
	    		gettrans.setString(2,lname);
		    	languageNameTranslations.put(lname, lname); 
		    	if(gettrans.execute())
		    	{
		    	ResultSet trans = gettrans.getResultSet();
		    	if(trans!=null)
		    	{
		    	while (trans.next())
		    	{
		    		String value = G.utfDecode(trans.getString(1));
		    		if(value!=null && !"".equals(value))
		    		{
		    			System.out.println("Name of "+lname+" is "+value);
		    			languageNameTranslations.put(lname, value);
		    		}
	    	}}}}
	    	
	
	  /* this version downloaded only languages that actually have translations
	   * which makes it less than useful when adding a new language
	   
	    	PreparedStatement ret = conn.prepareStatement("select distinct language from translation");
		    if(ret.execute())
		    {
		    	ResultSet result = ret.getResultSet();
		    	while (result.next())
		    	{
		    		String key = utfDecode(result.getString(1));
		    		if(!"".equals(key)) { languages.addElement(key); }
		    		
		    	};
		    	
		    }
		*/
		  return((String[])languages.toArray(new String[languages.size()]));
	  }
	  public static void downloadData(String host,String database,boolean includeWeb,
			  String user,String password,String datadirs,String only)
	  {	
	    // output can be a comma separated list of directories, strings are saved to each
	  	try {
		  Connection conn = connect(host,database,user,password);
		  downloadStrings(conn,includeWeb,datadirs,only);
		  conn.close();
	  	}
	  	catch (SQLException err)
	  	{ 	System.out.println("Sql exception: "+err.toString());
	  	}
	  }
	  public static void downloadStrings(Connection conn,boolean includeWeb,String datadirs,String only)
	  {	  try {
		  String dirs[] = G.split(datadirs,',');
		  Hashtable<String,ValueSet> englishKeys = downloadStrings(conn,"english",includeWeb);
		  HashSet<String>included = null;
		  if(only!=null)
		  {
			  String languages[] = only.split(",");
			  included = new HashSet<String>();
			  for(String l : languages) { included.add(l.trim().toLowerCase());}
			  
		  }
		  for(String datadir : dirs)
		  {
		  if(included==null || included.contains("english"))
			  { saveStrings(datadir+(datadir.endsWith("/")?"":"/")+"english.data",includeWeb,englishKeys,null);
			  }
		  }
	  	  for(String lang : getLanguages(conn)) 
		   		{ if(included==null || included.contains(lang))
		   		  {
	  		  	  Hashtable<String,ValueSet> langKeys = downloadStrings(conn,lang,includeWeb);
				  for(String datadir : dirs)
				  {
		   		  saveStrings(datadir+(datadir.endsWith("/")?"":"/")+lang+".data",includeWeb,langKeys,englishKeys);
				  }}
		   		}  
	  }
	  catch (Throwable err)
	  {
		System.out.println("error: "+err);
	  }
	  	  
	  }
	  public static void downloadDataWithSSh(String host,boolean includeWeb,
			  String sshUser,String sshPass,int sshPort,
			  String database,String user,String password,String datadirs,String only)
	  {	
		  // output can be a comma separated list of directories, strings are saved to each
		try {
			Connection conn = connectWithSSH(host,sshUser,sshPort,sshPass,database,user,password);
			downloadStrings(conn,includeWeb,datadirs,only);
			conn.close();
			} 
			catch (SQLException err)
	  		{ 	System.out.println("Sql exception: "+err.toString());
	  		}	
  	  
	  	  if(session!=null) { session.disconnect(); session = null; }
	  }
 /**
  * -sshpass xxxx
    -p xxxx
    -o path,path
    

  * @param args
  */
	  public static void main(String args[])
	  {	  String pass = "";
	  	  String host = "unspecified host";
	  	  String database = "boardspace";
	  	  String user = "root";				// database user
	  	  String sshUser = "boardspa";
	  	  String task = args.length>0 ? args[0] : "DownloadStrings";
	  	  String sshPass = null;
	  	  int sshPort = 9130;
	  	  boolean useSsh = true;
	  	  String out = null;
	  	  boolean download = true;
	  	  String only = null;
	  	  boolean includeWeb = false;
	  	  Preferences prefs = Preferences.userRoot();
	  	  String initialValue = prefs.get(task,"");
	  	  String pars = G.textAreaDialog(null,"Parameters for "+task,initialValue);
	  	  boolean trouble = true;
	  	  if(pars!=null) 
	  	  { 
	  	  trouble = false;
	  	  prefs.put(task,pars);
	  	  Tokenizer tok = new Tokenizer(pars);
	  	  while(tok.hasMoreElements())
	  	  {	String str = tok.nextToken();
	  	  	String val = tok.hasMoreTokens() ? tok.nextToken() : "";
	  	  	if(str.equals("-p")) { pass = val; }
	  	  	else if("-usessh".equalsIgnoreCase(str)) { useSsh = Boolean.valueOf(val); }
	  	  	else if("-sshuser".equalsIgnoreCase(str)) { sshUser = val; }
	  	  	else if("-sshpass".equalsIgnoreCase(str)) { sshPass = val; }
	  	  	else if("-sshport".equalsIgnoreCase(str)) { sshPort = Integer.parseInt(val); }
	  	  	else if("-includeweb".equalsIgnoreCase(str)) { includeWeb = Boolean.valueOf(val); }
	  	  	else if("-only".equalsIgnoreCase(str)) { only = val; }
	  	  	else if(str.equals("-h")) { host = val; }
	  	  	else if(str.equals("-u")) { user = val; }
	  	  	else if(str.equals("-d")) { database = val; }
	  	  	else if(str.equals("-o")) { out = val; download = true; }
	  	  	else if(str.equals("-download")) { download = true; }
	  	  	else { trouble=true; System.out.println(str + " "+ val+" not understood"); }
	  	  }}
	  	  if(!download ) { trouble = true; }
	  	  if(trouble) 
	  	  	{ System.out.println("use: \n -only <languages>\n -includeweb false -usessh true -sshuser user -sshpass pass -sshport port -h host -u user -p password -d database -o path -download");
	  	  	}
	  	  else {
		  	  if(download) 
		  	  	{ if(useSsh)
		  	  		{ downloadDataWithSSh(host,includeWeb,sshUser,sshPass,sshPort,database,user,pass,out,only);
		  	  		}
			  	  	else 
			  	  	{downloadData(host,database,includeWeb,user,pass,out,only); 
			  	  	}
		  	  	}
	  	  }
	  	  
	  }
	  
}
