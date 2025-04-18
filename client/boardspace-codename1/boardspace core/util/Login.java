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
package util;

import bridge.*;
import lib.Base64;
import lib.G;
import lib.Http;
import lib.InternationalStrings;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.UrlResult;
import lib.XXTEA;
import online.common.OnlineConstants;


/**
 * this runs the boardspace login process, by putting up a UI to collect the particulars,
 * then invoking the standard login form on the server and parsing the result.  The results
 * are stored in a hashtable which is eventually used to server ersatz "applet" parameters.
 * 
 * @author ddyer
 *
 */
@SuppressWarnings("deprecation")
public class Login implements SimpleObserver,Config,OnlineConstants
{	
    
    

    //
    // validate the checksum for something
    // supposed to be a java web start string
    // this isn't really cryptographic, but is not completely
    // trivial to defeat.
    //
    private boolean validate(String str)
    {	
    	int ind = str.indexOf("calc=");
    	if(ind>0)
    	{	int lastIdx = str.indexOf('\n',ind);
    		String mainPart = str.substring(lastIdx+1);
    		
        	int lenidx = str.indexOf("len=");
        	int lenidend = str.indexOf('\n',lenidx);
        	if(lenidx>=0 && lenidend>=0)
        	{
        	String lenstr = str.substring(lenidx+4,lenidend-lenidx);
        	int mainlen = mainPart.length();
        	int len = G.IntToken(lenstr);
        	if(len!=mainlen) { return(false); }
        	}

    		String checksumPart = str.substring(ind+5,lastIdx);
    		int desiredcs = G.IntToken(checksumPart);
    		int actualcs = Base64.simplecs(mainPart);
    		
     		return(actualcs==desiredcs);
    	}
    	return(false);
    }
    //
    // capture the values in "str" whichhave the format of one parameter per line,
    // where each line is name=the line is the value
    // blank lines are accepted, but no other embelishments.
    //
    private void capture(String str)
    {
    	int prev = 0;
    	int next = 0;
    	while((prev>=0) && ((next = str.indexOf('=',prev))>0))
    	{	int eol = str.indexOf('\n',prev);
    		if(eol>next)
    			{ // trim to avoid being screwed by invisible spaces.
    			  String key = str.substring(prev,next).trim();
    			  String val = str.substring(next+1,eol).trim();
    			  while ( ((val.startsWith("\"") && (val.endsWith("\""))))
    					  || ((val.startsWith("'") && (val.endsWith("'")))))
    			  	{ val = val.substring(1,val.length()-1); 		// trim enclosing quotes
    			  	}
    			  if(KEYWORD_GUESTNAME.equals(key))
  			  		{ val = G.utfDecode(val); 	// if the guest name contained unicode it needs to be decoded
    			  	  // and make sure it's a short name,  Encoding can make this 6x as long, and the server
    			  	  // only deals with the encoded version. 
  			  		if(val.length()>9 ) { val = val.substring(0,9); }
  			  		}
    			  else if(G.LANGUAGE.equals(key) && val.endsWith("Strings"))
    			  {
    				  val = val.substring(0,val.length()-"Strings".length());
    			  }
    			  G.putGlobal(key,val);			// load into the applet parameter table
     			  //System.out.println("K "+key+" V '"+val+"'");
    			}
    		if(eol>0) { prev = eol+1; } else { prev = next+1; }
    	}
    	//G.print("Done");
    }
    
    private void doMessage(String msg)
    {	
    	if((msg!=null)
    			&& !msg.toLowerCase().startsWith("http:")
    			&& !msg.toLowerCase().startsWith("https:")
    			)
    	{	// not a url, just show the message box.  This should come from
    		// the include.pl var $jws_message passed by login.cgi
    		G.infoBox("message from the management", 
    				msg);
    	}
    	else 
    	{
    		G.infoBox("message from the management", 
			"There's a message for you.  Click OK to view it, or log in to the site through the regular web page");
    		G.showDocument(msg,"Message");
    	}
    }

    @SuppressWarnings("deprecation")
	public boolean initFromWebStart()
    {	boolean captured = false;
    	boolean exit = false;
    	{
    	while(!exit)
    	{
		PasswordCollector collector = PasswordCollector.createAndShowGUI(this);
		// note than in the codename1 port, createAndShowGui operates as
		// a modal dialog, so this is allready complete and this waiting
		// is superfluous.
		while(collector.exitValue==null) 
			{ try { synchronized(this) { if(collector.exitValue==null) 
					{ 
					  wait(); 
					}}
				}
				catch (InterruptedException e) {}
			}
		G.putGlobal(G.VNCCLIENT,false);
		String lang = PasswordCollector.language;
		G.putGlobal(G.LANGUAGE,lang);
		if(PasswordCollector.TurnBasedMessage.equals(collector.exitValue))
		{
			G.setOffline(true);
			G.setTurnBased(true);
			exit=captured=true;
		}
		else if(PasswordCollector.ReviewMessage.equals(collector.exitValue))
		{	G.setOffline(true);
			exit=captured=true;
		}
		else if(PasswordCollector.PlaytableMessage.equals(collector.exitValue))
		{
			G.setOffline(true);
			G.putGlobal(G.VNCCLIENT,true);
			exit = captured = true;
		}
		// use the exit command
		else if(PasswordCollector.OK.equals(collector.exitValue))
		{	String name = PasswordCollector.name;
			String guestName = name;
			String pass = PasswordCollector.password;
			boolean guest = PasswordCollector.isGuest;
			boolean test = G.getBoolean(TESTSERVER,false);
			if(guest)
			{
				guestName = "".equals(name) ? PasswordCollector.guestName : name;
				name = PasswordCollector.guestName;
				pass = PasswordCollector.guestName;
			}
			String params = "&jws=1&pname="
				+Http.escape(name)
				+"&" + G.LANGUAGE+"="+Http.escape(lang)
				+ (test ? "&test=true" : "")
				+ (G.isCheerpj() ? "&cheerpj=true" : "")
				// guestname is passed through the login process so it is presented on the 
				// logged in side.  The escaping here ends up encoding the name with \u0000 if necessary
				+ (guest ? "&"+KEYWORD_GUESTNAME+"="+Http.escape(guestName) : "&cookie=1")
				;
			// the name of the password parameter is a minor difference between boardspace and tantrix
			int socks[] = {80};
			params = "&" + PasswordParameterName + "="+Http.escape(pass)+ params;
			params = G.platformString() + params;
			params = "&" + TimezoneParameterName + "="+Http.escape("x"+G.getLocalTimeOffset())+params;
			params = "&" + IdentityParameterName + "="+Http.escape("x"+G.getIdentity())+params;
			
			params = "params="+XXTEA.combineParams(params,XXTEA.getTeaKey());
			
			UrlResult result = Http.postURL(Http.getHostName(),loginURL,params,socks);
			InternationalStrings s = G.getTranslations();
			if(result.error!=null)
				{ G.infoBox(s.get(LoginFailedMessage),s.get(LoginFailedHost,Http.getHostName())); 
				}
			else if(result.text!=null)
				{

				if(result.text.startsWith("failed"))
				 {	String recover = s.get(RecoverPasswordMessage);
					String v = G.optionBox(s.get(LoginFailedMessage),s.get(LoginFailedExplanation),
							s.get(TryAgainMessage),recover);
					if(recover.equals(v))
					{
						G.showDocument(Http.httpProtocol+"//"+Http.getHostName()+recoverPasswordUrl,"Change Password");
					}
				 }
				 else if(result.text.startsWith("unavailable")) 
				 {
					 G.infoBox(s.get(ServerUnavailableMessage),s.get(ServerUnavailableExplanation));
				 }
				 else if(result.text.startsWith("message"))
				 {	captured = true;
				 	doMessage(result.text.substring(9));
				 }
				 else if(result.text.startsWith("applet"))
				 {
			     String res = Base64.decodeString(result.text.substring(7));
				 exit = validate(res);
				 if(exit) 
				 { capture(res); captured=true; 
				   //setCookie(url,"jwsname="+name+"; path=/; expires=Never");
				   //setCookie(url,"jwspass="+pass+"; path=/; expires=Never");
				   //setCookie(url,"jwslanh="+lang+"; path=/; expires=Never");
				 }
				 else 
				 	{Http.postError(this,"Login handshake invalid: "+res,null);
					 G.infoBox("Trouble loggin in","Login handshake data is invalid"); 
				 	}
				 }
				 else
				 { 	Http.postError(this,"unexpected result from login: "+result.error+"\n"+result.text,null);
					 G.infoBox("Unexpected result from login",result.text);
					 }
				}
		}
		else if(PasswordCollector.cancel.equals(collector.exitValue)) 
			{ exit = true; }
		else 
		{	
			G.print("Unexpected termination: " + collector.exitValue); exit=true; 
			Http.postError(this,"unexpected result from PasswordCollector : "+collector.exitValue,null);
		}
		
    	}}
    	return(captured);
    }

	public void update(SimpleObservable arg0, Object eventType, Object arg1) {
		
	}


}
