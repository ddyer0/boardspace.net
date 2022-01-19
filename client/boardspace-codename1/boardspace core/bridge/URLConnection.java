package bridge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import lib.G;


/**
 * this is an ad-hoc partial implementation of URLConnection, which does just the
 * things I need it to do.
 * 
 * @author Ddyer
 *
 */
public class URLConnection extends ConnectionRequest 		// native actionevents, not bridge 
{

	URL url = null;
	int timeout = 30000;
    public URLConnection(URL u) 
    {
		url = u;
		setUrl(u.urlString);
		setTimeout(timeout);
		setHttpMethod("GET");
		setFailSilently(true);
		setPost(false); 	// this papers over a bug in android, where http method is effectively ignored.
	}
    ByteArrayOutputStream output = null;
	boolean doOutput = false;
	Vector<String> headers = null;		// reconstructed headers
	Vector<String> headerKeys = null;	// reconstructed header keys
	//
	// capture the HTTP headers for later use and/or debugging.
	//
	public void readHeaders(Object connection) throws IOException 
	{
		super.readHeaders(connection);
		String names[] = getHeaderFieldNames(connection);
		headers = new Vector<String>();
		headerKeys = new Vector<String>();
		for(String h : names)
		{
			String con[] = getHeaders(connection,h);
			if(con!=null)
			{for(String s : con)
				{
				if(s!=null)
				{
				headerKeys.addElement(h);
				headers.addElement(s);
				}}
			}
			else { 	// getHeaders(nul) returns null instead of the HTTP header
					String s = getHeader(connection,h);
					if(s!=null) 
					{ headerKeys.addElement(h);
					  headers.addElement(s); 
					}
			}
        }	
	}
	
	public void buildRequestBody(OutputStream os) throws IOException
	{	super.buildRequestBody(os);
		if(doOutput && (output!=null))
		{
			output.close();
			byte data[] = output.toByteArray();
			os.write(data);
		}
	}

	public ByteArrayInputStream getResponse()
	{
		String err = getResponseErrorMessage();
		if(err!=null) { G.print("response error "+err); }
        byte[] c_result = getResponseData();
        ByteArrayInputStream in = c_result==null ? null : new ByteArrayInputStream(c_result);
        return(in);
	}
	public void setDoOutput(boolean b) {	doOutput = b; setWriteRequest(b); 	}

	public OutputStream getOutputStream()
	{
       	setPost(true);       
		output = new Utf8OutputStream();
		return(output);
	}

	public InputStream getInputStream() throws IOException {
        NetworkManager.getInstance().addToQueueAndWait(this);        
        return(getResponse());
	}

	public String getHeaderField(int hidx) {
		if(headers==null || hidx<0 || headers.size()<=hidx) { return(null); }
		return(headers.elementAt(hidx));
	}
	public String getHeaderFieldKey(int hidx) {
		if(headers==null || hidx<0 || headers.size()<=hidx) { return(null); }
		return(headers.elementAt(hidx));
	}

	public void openConnection() 
	{	setUrl(url.urlString);
	}

}
