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


import java.sql.*;
import java.util.*;
import java.util.prefs.Preferences;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import lib.G;
import lib.InternationalStrings;

/**
 * this is the uploader that uploads the contents of masterStrings to a database.
 * 
 * @author ddyer
 *
 */

public class UploadStrings
{	
	private static InternationalStrings getLanguage(String name)
	{	try 
		{ 
		InternationalStrings s = (InternationalStrings)Class.forName(name+"Strings").getDeclaredConstructor().newInstance();	// side effect read the main class
		s.readData(null);
		return(s);
		} 
		catch (Exception err) { throw G.Error("loading "+name+": "+err.toString()); }
	}

  static private String utfEncode(String str)
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

  // direct connection
  private static Connection  connect(String host,String database,String user,String pass) throws SQLException
  {	String url = "jdbc:mysql://"+host+"/"+database;
  	return(DriverManager.getConnection(url, user, pass));	  
  }
 // ssh connection
  static Session session = null;
  private static Connection  connectWithSSH(String host,String sshUser,int sshPort,String sshPass,
		  String database,String user,String pass) throws SQLException
  {	
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

  private static Hashtable<String,String> downloadStrings(Connection conn,String language,boolean lowercase) throws SQLException
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
  
  private static Hashtable<String,String> getLCKeys(Hashtable<String,String>in)
  {
	  Hashtable<String,String>newtable = new Hashtable<String,String>();
	  for(Enumeration<String> key = in.keys(); key.hasMoreElements();)
	  {	String val = key.nextElement();
	  	newtable.put(val.toLowerCase(),val);
	  }
	  return(newtable);
  }


  private static void updateStrings
	(Connection conn,
	  String language,
	  InternationalStrings lang,
	  boolean clear) throws SQLException
{	
	
	System.out.println("Updating language "+language);
	Hashtable<String,String> sensitiveLangKeys = lang.getTranslations(); 
	Hashtable<String,String> insensitiveLangKeys = lang.getCaseInsensitiveTranslations();
	Hashtable<String,String> langContexts = lang.getContexts();
	Hashtable<String,String> oldDbContents = downloadStrings(conn,language,false);
	Hashtable<String,String> lcDBKeys =  getLCKeys(oldDbContents);
	int ok = 0;
	int failed = 0;
	int missing = 0;
	int mismatch = 0;
	int changed = 0;
	int nobsolete = 0;
	for(Enumeration<String> key = lcDBKeys.keys(); key.hasMoreElements();)
		{		
		String lcdbKey = key.nextElement();
  		String dbKey = lcDBKeys.get(lcdbKey);
  		String langTrans = insensitiveLangKeys.get(lcdbKey);
  		if(langTrans==null)
  		{
  			G.print("obsolete key "+dbKey);
  			nobsolete++;
  		}
		}
	if(clear && nobsolete>0)
		  { 
			String v = G.optionBox("Delete these "+nobsolete+" keys","Deleting keys will be hard to undo",
							"Yes","No");
			if("Yes".equals(v))
			{
		  PreparedStatement clearst = conn.prepareStatement("delete from translation where language=?  and keystring=? and collection!='web'");
		  for(Enumeration<String> key = lcDBKeys.keys(); key.hasMoreElements();)
		  {		String lcdbKey = key.nextElement();
		  		String dbKey = lcDBKeys.get(lcdbKey);
		  		String langTrans = insensitiveLangKeys.get(lcdbKey);
		  		if(langTrans==null)
		  		{
		  			clearst.setString(1,language);
		  			clearst.setString(2,utfEncode(dbKey));
		  			G.print("Remove obsolete key "+dbKey);
		  			clearst.execute();
		  		}
		  }}}


	PreparedStatement ins = conn.prepareStatement("insert into translation set keystring=?, collection=?, translation=?,language=?");
	PreparedStatement ren = conn.prepareStatement("update translation set keystring=? where keystring=?");
	PreparedStatement change = conn.prepareStatement("update translation set translation=? where keystring=? and language=?");
	for(Enumeration<String> keys = sensitiveLangKeys.keys(); keys.hasMoreElements();)
	{	  String key = keys.nextElement();
		  String lcKey = key.toLowerCase();
		  String val = insensitiveLangKeys.get(lcKey);
		  String ucdbkey = lcDBKeys.get(lcKey);
		  String dbVal = (ucdbkey==null)?null : oldDbContents.get(ucdbkey);
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
		  }
		  else if(!key.equals(ucdbkey))
		  {	// changing casification
			  ren.setString(1,utfEncode(key));
			  ren.setString(2,utfEncode(ucdbkey));
			  G.print("Changing Key case from "+ucdbkey+" to "+key);
			  try {
				  ren.execute();
			  } catch (SQLException err)
			  {
			  		System.out.println(ins.toString());
			  		System.out.println("Sql exception: "+err.toString());
			  		failed++;				  
			  }
		  }
		  else if(!val.equals(dbVal))
		  	{
			  change.setString(1,val);
			  change.setString(2,key);
			  change.setString(3,language);
			  G.print("Change primary value for "+key+" from\n"+dbVal+" to\n"+val);
			  try {
				  change.execute();
				  changed++;
			  } catch (SQLException err)
			  {
			  		System.out.println(ins.toString());
			  		System.out.println("Sql exception: "+err.toString());
			  		failed++;				  
			  }
		  	}
		  
		}
		// check for a good upload
		Hashtable <String,String> back =  downloadStrings(conn,language,false);
		for(Enumeration<String> keys = sensitiveLangKeys.keys(); keys.hasMoreElements();)
	{	String key = keys.nextElement();
		String original = sensitiveLangKeys.get(key);
		String copy = back.get(key);
		if(copy==null)
			{ missing++; 
			System.out.println("Missing key for '"+key+"'"); 
			}
		else if(!copy.equals(original)) 
			{ mismatch++;
			System.out.println("Mismatch for "+key+ "\nout: " + original+ "\n in: "+copy); 
			}		
	}
		
		System.out.println("Executed "+ok+" ok. "+failed+" failed, " +missing+" missing, "+mismatch+" mismatch, "+" "+changed+" changed.");
}

  static boolean USE_SSH = true;
  private static void updateEnglish(String host,String sshUser,String sshPass,int sshPort,String database,String user,String password)
  {	try {
	  Connection conn = USE_SSH 
			  ? connectWithSSH(host,sshUser,sshPort,sshPass,database,user,password)
			  : connect(host,database,user,password);
  		InternationalStrings master = getLanguage("language.master");
  		updateStrings(conn,"english",master,true);
  		
  		conn.close();
  		if(session!=null) { session.disconnect(); session=null; }
  		//for(String lang : internationalStrings.languages) 
	   	//	{uploadStrings(conn,lang,true,englishKeys);
	   	//	}  
  }
  	catch (SQLException err)
	  	{ 	System.out.println("Sql exception: "+err.toString());
	  	}	  

  }
	
/**
 * a typical preferences string would be
     -sshpass shellaccountpassword
     -p databasepassword
 */
  public static void main(String args[])
  {   
	  String host = "boardspace.net";
	  String user = "root";
  	  String pass = "xxxxx";
  	  String sshUser = "boardspa";
  	  String sshPass = "xxxx";
  	  String database = "boardspace";
  	  int sshPort = 9130;
  	  
  	  Preferences prefs = Preferences.userRoot();
  	  String initialValue = prefs.get("UploadStrings","");
  	  String pars = G.textAreaDialog(null,"Parameters for Upload strings",initialValue);
  	  boolean trouble = true;
  	  if(pars!=null)
  	  {	  trouble = false;
	  	  prefs.put("UploadStrings",pars);
	  	  StringTokenizer tok = new StringTokenizer(pars);
	  	  while(tok.hasMoreTokens())
	  	  {
	  		String str = tok.nextToken();
	  	  	String val = tok.hasMoreTokens() ? tok.nextToken() : "";
	  	  	if(str.equals("-p")) { pass = val; }
	  	  	else if("-sshuser".equalsIgnoreCase(str)) { sshUser = val; }
	  	  	else if("-sshpass".equalsIgnoreCase(str)) { sshPass = val; }
	  	  	else if("-sshport".equalsIgnoreCase(str)) { sshPort = Integer.parseInt(val); }
	  	  	else if("-d".equalsIgnoreCase(str)) { database = val; }
	  	  	else if(str.equals("-h")) { host = val; }
	  	  	else if(str.equals("-u")) { user = val; }
	  	  	else { trouble=true; System.out.println(str + " "+ val+" not understood"); }  
	  	  }
  	  }
  	  if(trouble)
  	  {
  		  G.print("parameters -d <database> -p <databasepassword> -h <databasehost> -u <databaseuser> -sshuser <user> -sshpass <password> -sshport <port>");
  	  }
  	  else {
  		  updateEnglish(host,sshUser,sshPass,sshPort,database,user,pass);
  	  }
	  
  }
}