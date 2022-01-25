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
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.prefs.Preferences;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import lib.G;


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
	      String url = "jdbc:mysql://localhost:"+lport+"/"+database;
	      conn = DriverManager.getConnection(url, user, pass);

	      System.out.println ("Database connection established");
	      System.out.println("DONE");

	  	} catch (Throwable err)
	  	{
	  		System.out.println("connection error "+err);
	  	}
	  	return(conn);	  
	  }	  
	  public static String utfDecode(String str)
	    {		int nchars = str.length();
	    		int idx = 0;
	    		StringBuffer out = new StringBuffer();
	    		while(idx<nchars)
	    		{	char ch = str.charAt(idx++);
	    			if((ch!='\\') || (idx==nchars) || (str.charAt(idx)!='u')) { out.append(ch); }
	    			else 
	    			{ idx++;	// got \\u
	    			  int lim = Math.min(idx+4,nchars);
	    			  int v = Integer.parseInt(str.substring(idx,lim),16);
	    			  idx = lim;
	    			  out.append((char)v);
	    			}}
	    	return(out.toString());
	    }
	    
	  public static Hashtable<String,String> downloadStrings(Connection conn,String language) throws SQLException
	  {	Hashtable<String,String> trans = new Hashtable<String,String>();
	    PreparedStatement ret = conn.prepareStatement("select keystring,translation from translation where language=? and collection!='web'");
	    ret.setString(1,language);
	    if(ret.execute())
	    {
	    	ResultSet result = ret.getResultSet();
	    	while (result.next())
	    	{
	    		String key = utfDecode(result.getString(1));
	    		String value = utfDecode(result.getString(2));
	    		// this is based on the observation that the best and least
	    		// confusing way to present language names is "in its own language".
	    		// so while the database conains translations for the languages to the 
	    		// translated language, the data file will always contain each language
	    		// translated into its native spelling.
	    		trans.put(key,value);    		
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
	    	if((idx <= 0) || (idx==msg.length()-1))
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
	  public static void printKey(Writer out,String key,String val) throws IOException
	  {	String uKey = utfEncode(key);
	  	if(key.indexOf('\n')>=0)
	  		{ throw new Error("line break not allowed in key "+key); }
	  	if(key.equals(val))
		  {
			  out.write("S "+uKey);
			  out.write("\n");
		  }
	  	else
	  	{
	  		  out.write("K "+uKey);
			  out.write("\n");
			  for(String str : split(val,'\n'))
			  {
				  out.write("V "+utfEncode(str));
				  out.write("\n");
			  }
			 
	 	}
	  }

	  
	  public static void saveStrings(String name,Hashtable<String,String>langKeys,Hashtable<String,String>backupKeys)
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
				String val = langKeys.get(key);
				String backval = backupKeys.get(key);
				if(val==null) { val=backval; }
				
				// always save languages with translations in their own language
	    		String subst = languageNameTranslations.get(key);
	    		if(subst!=null) { 
	    				val = subst;
	    			}
	    		
				printKey(out,key,val);
				printed++;
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
		    		String value = utfDecode(trans.getString(1));
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
	  public static void downloadData(String host,String database,String user,String password,String datadirs)
	  {	String dirs[] = G.split(datadirs,',');
	    // output can be a comma separated list of directories, strings are saved to each
	  	try {
		  Connection conn = connect(host,database,user,password);
		  Hashtable<String,String> englishKeys = downloadStrings(conn,"english");
		  for(String datadir : dirs)
		  {
		  saveStrings(datadir+"english.data",englishKeys,null);
		  }
	  	  for(String lang : getLanguages(conn)) 
		   		{ Hashtable<String,String> langKeys = downloadStrings(conn,lang);
				  for(String datadir : dirs)
				  {
		   		  saveStrings(datadir+lang+".data",langKeys,englishKeys);
				  }
		   		}  
	  }
	  	catch (SQLException err)
		  	{ 	System.out.println("Sql exception: "+err.toString());
		  	}	  

	  }
	  
	  public static void downloadDataWithSSh(String host,String sshUser,String sshPass,int sshPort,
			  String database,String user,String password,String datadirs)
	  {	String dirs[] = G.split(datadirs,',');
	    // output can be a comma separated list of directories, strings are saved to each
	  	try {
		  Connection conn = connectWithSSH(host,sshUser,sshPort,sshPass,database,user,password);
		  Hashtable<String,String> englishKeys = downloadStrings(conn,"english");
		  for(String datadir : dirs)
		  {
		  saveStrings(datadir+"english.data",englishKeys,null);
		  }
	  	  for(String lang : getLanguages(conn)) 
		   		{ Hashtable<String,String> langKeys = downloadStrings(conn,lang);
				  for(String datadir : dirs)
				  {
		   		  saveStrings(datadir+lang+".data",langKeys,englishKeys);
				  }
		   		}  
	  	  conn.close();
	  	  if(session!=null) { session.disconnect(); session = null; }
	  	}
	  	catch (SQLException err)
		  	{ 	System.out.println("Sql exception: "+err.toString());
		  	}	  

	  }
 
	  public static void main(String args[])
	  {	  String pass = "";
	  	  String host = "";
	  	  String database = "";
	  	  String user = "";
	  	  String sshUser = null;
	  	  String sshPass = null;
	  	  int sshPort = 22;
	  	  String out = null;
	  	  boolean download = false;
	  	  Preferences prefs = Preferences.userRoot();
	  	  String initialValue = prefs.get("DownloadStrings","");
	  	  String pars = G.textAreaDialog(null,"Parameters for download strings",initialValue);
	  	  boolean trouble = true;
	  	  if(pars!=null) 
	  	  { 
	  	  trouble = false;
	  	  prefs.put("DownloadStrings",pars);
	  	  StringTokenizer tok = new StringTokenizer(pars);
	  	  while(tok.hasMoreElements())
	  	  {	String str = tok.nextToken();
	  	  	String val = tok.hasMoreTokens() ? tok.nextToken() : "";
	  	  	if(str.equals("-p")) { pass = val; }
	  	  	else if("-sshuser".equalsIgnoreCase(str)) { sshUser = val; }
	  	  	else if("-sshpass".equalsIgnoreCase(str)) { sshPass = val; }
	  	  	else if("-sshport".equalsIgnoreCase(str)) { sshPort = Integer.parseInt(val); }
	  	  	else if(str.equals("-h")) { host = val; }
	  	  	else if(str.equals("-u")) { user = val; }
	  	  	else if(str.equals("-d")) { database = val; }
	  	  	else if(str.equals("-o")) { out = val; download = true; }
	  	  	else if(str.equals("-download")) { download = true; }
	  	  	else { trouble=true; System.out.println(str + " "+ val+" not understood"); }
	  	  }}
	  	  if(!download ) { trouble = true; }
	  	  if(trouble) 
	  	  	{ System.out.println("use: -sshuser user -sshpass pass -sshport port -h host -u user -p password -d database -o path -download");
	  	  	}
	  	  else {
		  	  if(download) 
		  	  	{ if(sshUser!=null) 
		  	  		{ downloadDataWithSSh(host,sshUser,sshPass,sshPort,database,user,pass,out);
		  	  		}
			  	  	else 
			  	  	{downloadData(host,database,user,pass,out); 
			  	  	}
		  	  	}
	  	  }
	  	  
	  }
	  
}
