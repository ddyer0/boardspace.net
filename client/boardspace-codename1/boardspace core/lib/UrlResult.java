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