package lib;

import java.io.IOException;
import java.io.InputStream;

/**
 * utilities for HTTP requests.
 * 
 * The lower level of get/post urls use this structure to convey
 * the results of the post.
 * @author ddyer
 *
 */
public class UrlResult
{	/** the error string, if any.  If this is non-null, the
	transaction is complete.
	*/
	public String error=null;	
/**
 * normally the result body as a string.  If this is non-null, the
 * transaction is complete.
 */
	public String text=null;	
	/**
	 * the result headers as a string
	 */
	public String headers=null;	
	/**
	 * if true, return the raw inputstream rather than reading it as a string.
	 */
	public boolean getRawStream = false;	
	/**
	 * the raw input stream (after skipping headers).  This is to
	 * be used by receivers than need to parse the results in non
	 * standard ways.
	 */
	public InputStream inputStream;
	public String host;
	public int socket;
	public String url;
	public String data;
	
	/** clear the results, prepare for re-use.  This is used
	 * when starting an atomatic retry of the same operation.
	 */
	public void clear() 
	{ 
		error = null;
    	text = null;
    	headers = null;
    	if(inputStream!=null) { try {
			inputStream.close();
    		} catch (IOException e) {
    		}
    	inputStream=null;
    	}
	}
	/** if true, the transaction is complete, otherwise it is
	 * not started or in progress
	 * @return true the operation is complete
	 */
	public boolean isComplete() { return((error!=null)||(inputStream!=null)||(text!=null)); } 
}