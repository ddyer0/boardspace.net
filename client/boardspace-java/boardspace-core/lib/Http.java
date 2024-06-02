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

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import bridge.Config;
import bridge.Utf8OutputStream;
import bridge.Utf8Printer;


/**
 * utilities for HTTP requests.
 * @author ddyer
 *
 */
public class Http implements Config {
	private static String hostName = null;
	public static String httpProtocol = defaultProtocol;
	public static String getDefaultProtocol() { return(httpProtocol); }
	public static void setDefaultProtocol(String n)
	{
		if(n!=null && !"".equals(n))
		{
			if(!n.endsWith(":")) { n = n + ":"; }
			httpProtocol = n.toLowerCase();
		}
	}
	/**
	 * set the default host name used for Http activity by {@link #postAlert} and {@link #postError}
	 * @param str
	 */
	public static void setHostName(String str)
	{    int index = str==null?-1 : str.indexOf(':');
	     if(index>=0) { str=str.substring(0,index); }	// strip off the port
	     hostName=str; 	
	}
	public static String getHostName() 
	{ return(hostName); 
	}
    /**
     * perform a http GET to the specified server and url. result is a string array
     * @param server
     * @param urlStr
     * @param sockets an array of socket numbers to try, or null to try the standard list {@link bridge.Config#web_server_sockets}
     * @return a String[] where element 0 is an error message, or element 1 is the text returned from the url
     */
    static public UrlResult getURL(String server, String urlStr,int sockets[])
    {
        return(postURL(server, urlStr,"",sockets));
    }
/**
 * perform a http POST to the specified server and url, using data as the body of the message
 * @param server
 * @param urlStr
 * @param data
 * @param sockets an array of socket numbers to try, or null to try the standard list {@link bridge.Config#web_server_sockets}
 * @return a String[] where element 0 is an error message, or element 1 is the text returned from the url
 */
    static public UrlResult postURL(String server, String urlStr, String data,int sockets[])
    {
    	return(postURL(server,urlStr,data,sockets,new UrlResult()));
    }
    
    
    static public UrlResult postAsyncEncryptedURL(final String server,final String urlStr,final String data,final int sockets[])
    {	final UrlResult result=new UrlResult();
    	Runnable r = new Runnable() 
    		{ public void run() 
    			{ try { 
    				postEncryptedURL( server,  urlStr,  data,sockets,result);
    			}
    			finally { result.isComplete = true;
    				}   			  
    			}
    		};
    	new Thread(r,httpProtocol).start();
    	return(result);
    }
    static public UrlResult postEncryptedURL(String server,String urlStr,String data,int sockets[])
    {
    	return postEncryptedURL(server,urlStr,data,sockets,new UrlResult());
    }
    /** 
     * post to a url with an encrypted payload, expect the result to be encrypted the same way.
     *
     * 
     * @param server
     * @param urlStr
     * @param data
     * @param sockets
     * @return a UrlResult
     */
    static public UrlResult postEncryptedURL(String server,String urlStr,String data,int sockets[],UrlResult result)
    {
    	String params = data;
    	params = "params=" + XXTEA.combineParams(params, XXTEA.getTeaKey());
    	if(G.debug()) { G.print(params); }
    	Http.postURL(server,urlStr,params,sockets,result);
    	if(result.error==null)
    	{
    	String dec = XXTEA.Decode(result.text,XXTEA.getTeaKey());
    	String valid = XXTEA.validate(dec);
    	if(valid==null)
    		{	// this case corresponds to a result which is not internally consistent. 
    			// the most common cause of this is if the result contained extra text left
    			// over from some debugging print statement, or possible unexpected text from
    			// some uncontrolled part of the perl script.  It would also correspond to 
    			// where the result had been damaged or tampered with in transit.
    			// In any case, these can be difficult to diagnose so it's important to try
    			// to report them back to the server.
    			result.error = "result validation failed";
    			String err = G.concat("postEncryped returned an invalid result\n",
    					"url=",urlStr,
    					"\nparams=",params,
    					"\nresult=",result.text,
    					"\ndecoded=",dec);
    			postError(result,err,null);
    			//logError
    		}
    		else
    		{
    		int ind = valid.indexOf('\n');
    		if(ind>=0)
    		{
    			String msg = valid.substring(0,ind);
    			result.text = valid.substring(ind+1);
    			if(!"OK".equalsIgnoreCase(msg))
    			{	// these are the cases where there was a controlled error, reported 
    				// through the expected and controlled mechanisms. and it's intended
    				// that the user sees something about it.
    				result.error = result.text;
    			}
    		}
    		}
    	}
    	return(result);
    }
/**
 * perform a http POST to the specified server and url, using data as the body of the message
 * @param server
 * @param urlStr
 * @param data
 * @param sockets an array of socket numbers to try, or null to try the standard list {@link bridge.Config#web_server_sockets}
 * @param result the results array to be returned.  It must not be null
 * @return a String[] where element 0 is an error message, or element 1 is the text returned from the url
 */
    static public UrlResult postURL(String server, String urlStr, String data,int sockets[],UrlResult result)
    {	
        if(hostName==null) { setHostName(server); }
        for(int myS : sockets==null?web_server_sockets:sockets)
        {	if(result.text==null)
        	{        	
        	InputStream dis = postURLStream(server,myS,urlStr,data,result);
        	if(result.getRawStream) { result.inputStream = dis; }
        	else if(dis!=null)
        	{
                Utf8Reader reader = new Utf8Reader(dis);
                try
                {  String res = null;
                
                    if (dis != null)
                    {
                        String r;

                        do
                        {
                            r = reader.readLine();

                            if (r != null)
                            {
                                if (res == null)
                                {
                                    res = r;
                                }
                                else
                                {
                                    res += ("\r\n" + r);
                                }
                            }
                        }
                        while (r != null);
                    }
                    result.text = res;
                    if(res==null)
                    {
                    	result.error = "no result";
                    }
                    dis.close();
                    reader.close();
                }
                catch (IOException e)
                { G.print("posturl error"+e);
                }
        	}}}
    
    return(result);    
    }
      
    private static boolean httpError(String urlStr,UrlResult result,Throwable e)
    {	boolean retry = false;
    	if(httpProtocol.equals("https:")
    			&& (hostName!=null)
        		&& e instanceof IOException)
        	{
        		setDefaultProtocol("http:");
        		Plog.log.addLog("Downgrade https to http because of ",urlStr,e);
        		retry = true;
        	}
            else
            {
            	String msg = "error getting " + urlStr+" "+ e;
            	Plog.log.addLog(msg);
	                if(result!=null) { result.error = msg; }
            }
    	return(retry);
    }
    
    
    /**
     * get the url contents using a socket number as spec.  This is the preferred method because it uses 
     * standard applet HTTP 1.1 behavior instead of hand-rolled HTTP 1.0 requests used by the old method.
     * It's bad practice to use this - it can be hacked by using outgoing proxys
     * @param host
     * @param socket socket number to use (default should be 80)
     * @param url
     * @return a binary stream which presents the data from the url
     * @throws IOException
     */
    // bad practice to use this - it can be hacked by using outgoing proxys
    static public InputStream getURLStream(String host, int socket, String url)
        throws IOException
    {  // this must be null not "" so the method will be GET
    	// method POST would fail to read directories in codename1
    	return(postURLStream(host,socket,url,null,new UrlResult()));
    }
    
    // bad practice to use this - it can be hacked by using outgoing proxys
    static public InputStream getURLStream(String host, int socket, String url,UrlResult res)
    		throws IOException
    {	// this must be null not "" so the method will be GET
    	// method POST would fail to read directories in codename1
    	return(postURLStream(host,socket,url,null,res));
    }
    /**
     * get the url contents using a socket number as spec.  This is the preferred method because it uses 
     * standard applet HTTP 1.1 behavior instead of hand-rolled HTTP 1.0 requests used by the old method.
     * @param host
     * @param socket socket number to use (default should be 80)
     * @param url
     * @return a text stream which presents the data from the url
     * @throws IOException
     */
    static public Utf8Reader getURLReader(String host, int socket, String url)
            throws IOException
        {  return(postURLReader(host,socket,url,"",new UrlResult()));
        }


    /**
     * open a http connection to the speficied host, port, and url, using method POST 
     * if data is not null, or GET if it is null, with the specified data string as the body.
     * It's ok to supply both ?xx with the url and data.
     * @param host
     * @param socket
     * @param url
     * @param data
     * @param result is an array of 3 strings, or null.  If supplied, postURLStream will set the
     *  third strings to the concatenated result headers.
     * @return a binary reader stream for the data returned from the URL
     * @throws IOException
     */
     static public InputStream postURLStream(String host, int socket, String url,
            String data,UrlResult result) 
        {	String subdata = data==null ? "<null>" : "data["+data.length()+"]";
     		Plog.log.addLog("postURLStream ",url,"\n",subdata);
   	 		if(hostName!=null) { setHostName(host); }
        // if data is not null, also subsume any data in the url. 
        // you can still force a "get" style url by having data
     	if(data!=null)
    	 	{
                int idx = url.indexOf('?');
                if (idx >= 0)
                { //if there is a query string, remove it and append to the data
                	String msg = url.substring(idx+1);
                	data += "&" + msg;
                    url = url.substring(0, idx);
                }
             }

    	 	if(socket==NORMAL_HTTP_PORT)
    	 	{	// use defaulted socket
    	 		socket = 0;
    	 	}
    	 	if(result!=null)
    	 	{
    	 	result.host = host;
    	 	result.socket = socket;
    	 	result.url = url;
    	 	result.data = data;
    	 	}
    	 	boolean retry = false;
    	 	do {
    	 	if(result!=null) { result.clear(); }
    	 	try {
	 		URL u = new URL(httpProtocol+"//" + host + ((socket>0) ? ":"+socket : "")+ url);
	 		//G.print("post "+u);
	 		URLConnection c = u.openConnection();
        	if(data!=null)
        	{
        	c.setDoOutput(true);
        	if(c instanceof HttpURLConnection)
        		{ ((HttpURLConnection)c).setRequestMethod("POST");
        		}
        	OutputStream os = 	c.getOutputStream();
        	if(os!=null)
        	{
        	OutputStreamWriter out = new OutputStreamWriter(os);
        	// output your data here
        	out.write(data);
        	out.close();
    	 	}}
    	 	
        	InputStream ins = getInputStream(c);
        	if(ins==null)
        	{	G.print("no response from "+u);
        		throw new IOException("no response from "+u);
        	}
        	else
        	{
        		String s="";
        		boolean done = false;
        		int hidx = 0;
        		do { String key = c.getHeaderFieldKey(hidx); 
        			 String val = c.getHeaderField(hidx);
        			 if(key!=null || val!=null) {   s += key+":"+val+"\n"; } 
        			 else { done = true; }
        			 hidx++; 
        		} while (!done);
        		if(result!=null) { result.headers = s; }	// result gets the headers
             return (ins);
        	}
    	 	}
    	 	catch (Throwable err)
    	 	{
    	 		retry = httpError(url,result,err);
    	 	}
        	} while (retry);
    	 	return(null);
        	}
     
     private static SimpleLock streamLock = new SimpleLock("httpLock");
     private static InputStream getInputStream(URLConnection c) throws IOException
     {	try {
    	 // based on evidence of deep seated deadlocks in getInputStream,
    	 // force it to serialize there.
    	 streamLock.getLock();
    	 return c.getInputStream();
     	}
     	finally 
     	{
     		streamLock.Unlock();
     	}
     }
     
     /**
      * open a http connection to the specified host, port, and url, using method POST 
      * if data is not null, or GET if it is null, with the specified data string as the body.
      * It's ok to supply both ?xx with the url and data.
      * @param host
      * @param socket
      * @param url
      * @param data
      * @param result is an array of 3 strings, or null.  If supplied, postURLStream will set the
      *  third strings to the concatenated result headers.
      * @return a text reader stream for the data returned from the URL
      * @throws IOException
      */
        static public Utf8Reader postURLReader(String host, int socket, String url,
             String data,UrlResult result) throws IOException
             {
     	 			InputStream s = postURLStream(host,socket,url,data,result);
    	 			return(s==null ? null : new Utf8Reader(s));
             }
     
     static int errors_posted = 0;

     /**
     * this post a message to the server and registers an alert email. This is
     * used only for very alarming or unusual occurrences.
     * @param caller an arbitrary string, the alert will be from: user
     * @param message
     * @return true if the alert was posted
     */
     static public boolean postAlert(String caller, String message)
     {
         if ((hostName!=null) && G.logErrorsWithHttp() && errors_posted < 10)
         {
             errors_posted++;
             String finalmsg = "&tagname=postalert&name="+Http.escape(caller)
            	+ "&data=" + Http.escape(message);
             UrlResult res = postEncryptedURL(hostName, getEncryptedURL, finalmsg,web_server_sockets);
             return(res.error==null);
         }

         return (false);
     }
     /** produce a stack trace headed by message.  This is used automatically by the error logging system.
      * 
      * @param msg
      * @return a String
      */
     public static String stackTrace(String msg)
     {
         try
         {
        	 throw new ErrorX(msg);
         }
         catch (ErrorX err)
         {
             return (getErrorMessage("", err));
         }
     }

     /**
      * get a error message combined with a stack trace.
      * This is used automatically by the error logging system
          * @param message
          * @param err
          * @return a String
          */
     static public String getErrorMessage(String message, Throwable err)
     {
         ByteArrayOutputStream b = new Utf8OutputStream();
         PrintStream os = Utf8Printer.getPrinter(b);
         os.print(G.getSystemProperties() + " ");

         Thread thr = Thread.currentThread();
         os.print("(" + thr.getName() + " " + Thread.activeCount() +
             " threads)");
         os.println(message);

         if (err != null)
         {	 os.println("error is : "+err.toString());
         	 G.printStackTrace(err,os);
         }
         
         String errorContext = Plog.log.getUnseen();
         if(errorContext!=null)
         {	os.println("[history:\n"+errorContext+"]");
         }
         os.flush();
         String msg = b.toString();
         return (msg);
     }

     /**
      * this posts an error to the web site when there is no connection to the server.
      * @param caller
      * @param message
      * @param err
      * @return true if the error was posted
      */
    static public boolean postError(Object caller, String message, Throwable err)
    	    {
    		if(G.isCheerpj())
    			{ G.infoBox(message,err==null ? "no error caught" : ""+err+err.getStackTrace()); 
    			}
    		try {
    			G.setPostedError(message+":"+err);
    	        if (!(err instanceof ThreadDeath) 	// don't post ThreadDeath events
    	        		&& (errors_posted < 10))
    	        {
    	            errors_posted++;

    	            //sometimes processes get stuck in an error loop. Don't flood the
    	            //server with too many reports.
    	            String msg = getErrorMessage(message, err);
    	            @SuppressWarnings("deprecation")
					String cname =  (caller==null) ? "null" :
									(caller instanceof String) 
    	            					? (String)caller 
    	            					: (caller.getClass().getName());
    	            Plog.log.addLog("Log request from " ,cname);
    	            Plog.log.addLog(msg);
    	            String finalmsg = "&tagname=posterror&name="+Http.escape(cname)
    	            	+ "&data=" + Http.escape(msg);
    	            if((hostName!=null) 
    	        		 && G.logErrorsWithHttp() )
    	            {
    	            UrlResult res = Http.postEncryptedURL(hostName,	getEncryptedURL , finalmsg,web_server_sockets);
    	            G.setPostedError(message+":"+err);
    	            return(res.error==null);
    	            }
    	            
    	        }
     	        return (false);
    	    }
        	catch (ThreadDeath err2) { throw err2;}
    		catch (Throwable err2)
    		{
    			G.print("Recursive error in postError "+err2+" original error was "+err);
    			G.infoBox("Recursive error in postError+err2"," original error was "+err);
    		}
    		return(false);
   }



static public UrlResult postAsyncUrl(final String server,final String urlStr,final String data,final int sockets[])
{	return(postAsyncUrl(server,urlStr,data,sockets,false));
}
static public UrlResult postAsyncUrl(final String server,final String urlStr,final String data,final int sockets[],boolean raw)
{	final UrlResult result=new UrlResult();
	result.getRawStream = raw;
	Runnable r = new Runnable() 
		{ public void run() 
			{ try {
				postURL( server,  urlStr,  data,sockets,result); 
				}
				finally
				{	result.isComplete = true;
				}
			}
		};
	new Thread(r,httpProtocol).start();
	return(result);
}
//
//public static void main(String args[])
//{
//	String p[] = postURL("boardspace.net","/cgi-bin/boardspace_rankings.cgi?game=sm&language=french","",new int[]{80});
//	System.out.println("Headers: "+p[2]);
//	System.out.println("Main: "+p[1]);
//}
/** email a game record */
public static void emailGame(String to,String subject,String body)
{
	String msg = "mailto:"+to
					+"?subject="+Http.escape(subject)
					+"&body="+Http.escape(body);
	G.showDocument(msg);
}
/*  */
/**
 * escape an arbitrary string for transmission as form data. This should be used on the data
 * portion of a parameter, ie; "?foo="+escape(bar)
 */
static public String escape(String d)
{
    int idx = 0;
    int len = d.length();
    ByteArrayOutputStream b = new Utf8OutputStream();
    PrintStream os = Utf8Printer.getPrinter(b);

    while (idx < len)
    {
        char c = d.charAt(idx++);

        if (((c >= 'A') && (c <= 'Z'))
        		|| ((c >= 'a') && (c <= 'z'))
        		|| ((c >= '0') && (c <= '9'))
        		)
        {
            os.print(c);
        }
        else 
        {	// this is not completely correct if some unicode sneaks in
        	os.print("%" + Http.HexDig[(c&0xff) / 16] + Http.HexDig[c % 16]);
         }
    }

    os.flush();

    return (b.toString());
}

public static final char[] HexDig = 
{
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
    'E', 'F'
};
public static String encodeEntities(String s) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        out.append((c<=' ') || (c > 0x7f) || (c == '"') || (c == '&') || (c == '<') || (c == '>') ? "&#" + (int) c + ";" : c);
    }
    return out.toString();
}

/**
 * get a HTTP url for host + url
 * @param host
 * @param url
 * @return
 */
//static public URL getHttpURL(String host,String url)
//{	try { return(new URL(protocol+"//"+host+(url.charAt(0)=='/'?"":"/")+url)); }
//	catch (MalformedURLException err) { System.out.println("Unexpected exception "+err); }
//	return(null);
//}

}