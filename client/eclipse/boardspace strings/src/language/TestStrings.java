package language;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.*;
import java.util.*;

import lib.*;

public class TestStrings
{	public static InternationalStrings getLanguage(String name)
	{	try 
		{ 
		InternationalStrings s = (InternationalStrings)Class.forName("online.language."+name+"Strings").getDeclaredConstructor().newInstance();	// side effect read the main class
		s.readData(null);
		return(s);
		} 
		catch (Exception err) { throw G.Error("loading "+name+": "+err.toString()); }
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

  public static void blessStrings()
  {
    try {
    	InternationalStrings english = getLanguage("english");
    	InternationalStrings.strs.clear();
    	english.readData(null);
		Hashtable<String,String> englishKeys = english.getTranslations();
    	String languages[] = InternationalStrings.languages;
	    for(int i=0;i<languages.length;i++)
	        { String name = languages[i];
	          System.out.println("processing boardspace language " + name);
	          InternationalStrings.strs.clear();
	          InternationalStrings lang = getLanguage(name);	// side effect read a new class
	  		  Hashtable<String,String> langKeys = lang.getTranslations();
          
	          for(Enumeration<String> e = langKeys.keys(); e.hasMoreElements();)
	          { String v = e.nextElement();
	            if(!englishKeys.containsKey(v)) { System.out.println("Obsolete key: " + v); }
	          }

	          for(Enumeration<String> e = englishKeys.keys(); e.hasMoreElements();)
	          { String v = e.nextElement();
	            if(!langKeys.containsKey(v)) { System.out.println("put(\"" + v+"\",\""+v+"\");"); }
	          }
	        }
	      } 
	      catch (Exception err) { System.out.println("unexpected error "+err); }
	        //check against englishstrings
  }
  public static Connection  connect(String host,String database,String user,String pass) throws SQLException
  {	String url = "jdbc:mysql://"+host+"/"+database;
  	return(DriverManager.getConnection(url, user, pass));	  
  }
  
  
  public static Hashtable<String,String> downloadStrings(Connection conn,String language,boolean lowercase) throws SQLException
  {	Hashtable<String,String> trans = new Hashtable<String,String>();
    PreparedStatement ret = conn.prepareStatement("select keystring,translation from translation where language=? and collection!='web'");
    ret.setString(1,language);
    if(ret.execute())
    {
    	ResultSet result = ret.getResultSet();
    	while (result.next())
    	{
    		String key = G.utfDecode(result.getString(1));
    		if(lowercase) { key = key.toLowerCase(); }
    		String value = G.utfDecode(result.getString(2));
    		trans.put(key,value);
    		
    	};
    	
    }
   	return(trans);
  }
  
  public static void uploadStrings
  		(Connection conn,
		  String language,
		  InternationalStrings lang,
		  boolean clear,
		  Hashtable<String,String> englishKeys) throws SQLException
  {	

  	System.out.println("Adding language "+language);
	Hashtable<String,String> langKeys = lang.getTranslations(); 
  	Hashtable<String,String> langContexts = lang.getContexts();
  	int ok = 0;
  	int failed = 0;
  	int missing = 0;
  	int mismatch = 0;
    if(clear)
		  {
		  PreparedStatement clearst = conn.prepareStatement("delete from translation where language=? and collection!='web'");
		  clearst.setString(1,language);
		  clearst.execute();
		  }
	  PreparedStatement ins = conn.prepareStatement("insert into translation set keystring=?, collection=?, translation=?,language=?");
	  for(Enumeration<String> keys = langKeys.keys(); keys.hasMoreElements();)
	  {	  String key = keys.nextElement();
		  String val = langKeys.get(key);
		  String context = langContexts.get(key);
		  if(val == null) { val = key; }
		  if((englishKeys==null) || !val.equals(englishKeys.get(key)))
		  {
		  if(context==null) { context = "common"; }
		  ins.setString(1,utfEncode(key));
		  ins.setString(2,utfEncode(context));
		  ins.setString(3,utfEncode(val));
		  ins.setString(4,language);

		  	try {
		  		ins.execute();
		  		//System.out.println(ins.toString());
		  		ok++;
		  		}
		  	catch (SQLException err)
		  		{
		  		System.out.println(ins.toString());
		  		System.out.println("Sql exception: "+err.toString());
		  		failed++;
		  		}
		  }}
   	// check for a good upload
   	Hashtable <String,String> back =  downloadStrings(conn,language,false);
   	for(Enumeration<String> keys = langKeys.keys(); keys.hasMoreElements();)
  	{	String key = keys.nextElement();
  		String original = langKeys.get(key);
  		String copy = back.get(key);
  		if(copy==null)
  			{ missing++; 
  			//System.out.println("Missing key for '"+key+"'"); 
  			}
  		else if(!copy.equals(original)) 
  			{ mismatch++;
  			System.out.println("Mismatch for "+key+ "\nout: " + original+ "\n in: "+copy); 
  			}		
  	}
   	
   	System.out.println("Executed "+ok+" ok. "+failed+" failed" +" missing "+missing+" mismatch "+mismatch);
  }
  
  public static void updateStrings
	(Connection conn,
	  String language,
	  InternationalStrings lang,
	  boolean clear) throws SQLException
{	
	
	System.out.println("Updating language "+language);
	Hashtable<String,String> sensitiveLangKeys = lang.getTranslations(); 
	Hashtable<String,String> insensitiveLangKeys = lang.getCaseInsensitiveTranslations();
	Hashtable<String,String> langContexts = lang.getContexts();
	Hashtable<String,String> oldDbContents = downloadStrings(conn,language,true);
	int ok = 0;
	int failed = 0;
	int missing = 0;
	int mismatch = 0;
	if(clear)
		  {
		  PreparedStatement clearst = conn.prepareStatement("delete from translation where language=?  and keystring=? and collection!='web'");
		  for(Enumeration<String> key = oldDbContents.keys(); key.hasMoreElements();)
		  {		String dbKey = key.nextElement();
		  		String dbTrans = oldDbContents.get(dbKey);
		  		String langTrans = insensitiveLangKeys.get(dbKey);
		  		if(langTrans==null) { langTrans=""; }
		  		if(!(langTrans.equals(dbTrans))
		  				&& !(langTrans.toLowerCase().equals(dbKey)))
		  		{
		  			clearst.setString(1,language);
		  			clearst.setString(2,utfEncode(dbKey));
		  			G.print("Remove obsolete key "+dbKey);
		  			clearst.execute();
		  		}
		  }}
	PreparedStatement ins = conn.prepareStatement("insert into translation set keystring=?, collection=?, translation=?,language=?");
	for(Enumeration<String> keys = sensitiveLangKeys.keys(); keys.hasMoreElements();)
	{	  String key = keys.nextElement();
		  String lcKey = key.toLowerCase();
		  String val = insensitiveLangKeys.get(lcKey);
		  String dbVal = oldDbContents.get(lcKey);
		  String context = langContexts.get(key);
		  if(val == null) { val = key; }
		  if( dbVal == null)
		  {
		  if(context==null) { context = "common"; }
		  ins.setString(1,utfEncode(key));
		  ins.setString(2,utfEncode(context));
		  ins.setString(3,utfEncode(val));
		  ins.setString(4,language);
		  G.print("Adding new key "+key);
		  	try {
		  		ins.execute();
		  		//System.out.println(ins.toString());
		  		ok++;
		  		}
		  	catch (SQLException err)
		  		{
		  		System.out.println(ins.toString());
		  		System.out.println("Sql exception: "+err.toString());
		  		failed++;
		  		}
		  }}
		// check for a good upload
		Hashtable <String,String> back =  downloadStrings(conn,language,false);
		for(Enumeration<String> keys = insensitiveLangKeys.keys(); keys.hasMoreElements();)
	{	String key = keys.nextElement();
		String original = insensitiveLangKeys.get(key);
		String copy = back.get(key);
		if(copy==null)
			{ missing++; 
			//System.out.println("Missing key for '"+key+"'"); 
			}
		else if(!copy.equals(original)) 
			{ mismatch++;
			System.out.println("Mismatch for "+key+ "\nout: " + original+ "\n in: "+copy); 
			}		
	}
		
		System.out.println("Executed "+ok+" ok. "+failed+" failed" +" missing "+missing+" mismatch "+mismatch);
}


  public static void updateLanguage(String lang,String host,String database,String user,String password)
  {	try {
	  Connection conn = connect(host,database,user,password);
  		InternationalStrings master = getLanguage("master");
  		Hashtable<String,String>masterKeys = master.getTranslations();
  		uploadStrings(conn,lang,getLanguage(lang),true,masterKeys); 
  	}
  	catch (SQLException err)
	  	{ 	System.out.println("Sql exception: "+err.toString());
	  	}	  

  }
  
  public static void doDatabase(String host,String database,String user,String password)
  {	try {
	  Connection conn = connect(host,database,user,password);
  		InternationalStrings master = getLanguage("master");
  		Hashtable<String,String>masterKeys = master.getTranslations();
  		uploadStrings(conn,"english",master,true,null);
  		for(String lang : InternationalStrings.languages) 
	   		{uploadStrings(conn,lang,getLanguage(lang),true,masterKeys);
	   		}  
  }
  	catch (SQLException err)
	  	{ 	System.out.println("Sql exception: "+err.toString());
	  	}	  

  }
  public static void updateEnglish(String host,String database,String user,String password)
  {	try {
	  Connection conn = connect(host,database,user,password);
  		InternationalStrings master = getLanguage("master");
  		updateStrings(conn,"english",master,true);
  		//for(String lang : internationalStrings.languages) 
	   	//	{uploadStrings(conn,lang,true,englishKeys);
	   	//	}  
  }
  	catch (SQLException err)
	  	{ 	System.out.println("Sql exception: "+err.toString());
	  	}	  

  }
	
  public static void printKey(Writer out,String key,String val) throws IOException
  {	String uKey = utfEncode(key);
  	if(key.indexOf('\n')>=0) { throw new Error("line break not allowed in key "+key); }
  	if(key.equals(val))
	  {
		  out.write("S "+uKey);
		  out.write("\n");
	  }
  	else
  	{
  		  out.write("K "+uKey);
		  out.write("\n");
		  for(String str : G.split(val,'\n'))
		  {
			  out.write("V "+utfEncode(str));
			  out.write("\n");
		  }
		 
 	}
  }
  public static void saveStrings(String name,Hashtable<String,String>langKeys,Hashtable<String,String>backupKeys0)
  {		Hashtable<String,String>backupKeys = backupKeys0;
  		if(backupKeys==null) { backupKeys = langKeys; }
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
  public static void downloadLanguage(String lang,String host,String database,String user,String password)
  {	String datadir = "g:\\share\\projects\\eclipse\\boardspace\\online\\data\\";
  	try {
	  Connection conn = connect(host,database,user,password);
	  Hashtable<String,String> englishKeys = downloadStrings(conn,"english",false);
  	  Hashtable<String,String> langKeys = downloadStrings(conn,lang,false);
	  saveStrings(datadir+lang+".data",langKeys,englishKeys); 
  	}
  	catch (SQLException err)
	  	{ 	System.out.println("Sql exception: "+err.toString());
	  	}	  

  }
  public static void downloadData(String host,String database,String user,String password)
  {	String datadir = "g:\\share\\projects\\eclipse\\boardspace\\online\\dictionary\\";
  	try {
	  Connection conn = connect(host,database,user,password);
	  Hashtable<String,String> englishKeys = downloadStrings(conn,"english",false);
	  saveStrings(datadir+"english.data",englishKeys,null);
  	  for(String lang : InternationalStrings.languages) 
	   		{ Hashtable<String,String> langKeys = downloadStrings(conn,lang,false);
	   		  saveStrings(datadir+lang+".data",langKeys,englishKeys);
	   		}  
  }
  	catch (SQLException err)
	  	{ 	System.out.println("Sql exception: "+err.toString());
	  	}	  

  }
  
  public static void main(String args[])
  {  	//String host = "boardspace.net";
  	  //String pass = "greatpumpkin";
	  //String host = "localhost";
	  String host = "boardspace.net";
  	  String pass = "xxxxx";
	  //blessStrings();
	  updateEnglish(host,"boardspace","root",pass);
	  //updateLanguage("chinese",host,"boardspace","root",pass);
	  //downloadLanguage("chinese",host,"boardspace","root",pass);
	  //downloadLanguage("english",host,"boardspace","root",pass);	  
	  //downloadLanguage("swedish",host,"boardspace","root",pass);
	  //doDatabase(host,"boardspace","root",pass);
	  downloadData(host,"boardspace","root",pass);
  }
}