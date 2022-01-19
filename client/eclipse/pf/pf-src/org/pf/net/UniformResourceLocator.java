// ===========================================================================
// CONTENT  : CLASS UniformResourceLocator
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 30/03/2012
// HISTORY  :
//  17/11/2010  mdu  CREATED
//	15/11/2011	mdu		added -> public void removeParameter(String name)
//	30/03/2012	mdu		added	-> clearParameters()
//
// Copyright (c) 2010-2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.net ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

import org.pf.text.StringUtil;
import org.pf.util.CollectionUtil;
import org.pf.util.NamedText;
import org.pf.util.NamedTextList;

/**
 * Helper class to parse and manipulate URLs in a convenient way.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class UniformResourceLocator
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final StringUtil SU = StringUtil.current();
	private static final CollectionUtil CU = CollectionUtil.current();
	
	public static final String SCHEME_HTTP 					= "http";
	public static final String SCHEME_HTTPS 				= "https"; 
	
	public static final int HTTP_DEFAULT_PORT				= 80 ;
	public static final int HTTPS_DEFAULT_PORT			= 443 ;
  
	protected static final int DEFAULT_PORT					= 0 ;
	protected static final String DEFAULT_SCHEME 			= SCHEME_HTTP ;

	/** "://" */
	public static final String SCHEME_HOST_SEPARATOR	= "://" ;

	/** "http://" */
	public static final String HTTP_SCHEME_PREFIX		= SCHEME_HTTP + SCHEME_HOST_SEPARATOR ;
	/** "https://" */
	public static final String HTTPS_SCHEME_PREFIX	= SCHEME_HTTPS + SCHEME_HOST_SEPARATOR ;
	
	/** The character '?' that is used to separate the query parameters from the URL */
	public static final String URL_QUERY_SEPARATOR 		= "?";
	/** The character '&' that is used to separate consecutive query parameters from each other */
	public static final String URL_PARAM_SEPARATOR 		= "&";
	/** The character '=' that is used to separate name and value of query parameters */
	public static final String URL_PARAM_ASSIGN 			= "=";
	/** The character ':' that is used to separate hostname from the port */
	public static final String URL_PORT_SEPARATOR			= ":";
	/** The character '/' that is used to separate naming elements of the URL */
	public static final String SLASH 									= "/" ;
		
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private NamedTextList parameters = null ;
	protected NamedTextList getParameters() { return parameters ; }
	protected void setParameters( NamedTextList newValue ) { parameters = newValue ; }
	
	private String serverName = null ;
	protected String serverName() { return serverName ; }
	protected void serverName( String newValue ) { serverName = newValue ; }
	
	private String requestURI = null ;
	protected String requestURI() { return requestURI ; }
	protected void requestURI( String newValue ) { requestURI = newValue ; }		

	private String scheme = DEFAULT_SCHEME ;
	protected String scheme() { return scheme ; }
	protected void scheme( String newValue ) { scheme = newValue ; }

	private int port = DEFAULT_PORT ;
  protected int port() { return port ; }
  protected void port( int newValue ) { port = newValue ; }	

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	/**
	 * Initialize the new instance with the given URL
	 */
	public UniformResourceLocator(String url) throws MalformedURLException
	{
		this();
		this.parseURL(url);
	} // UniformResourceLocator() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with default values.
	 */
	public UniformResourceLocator()
	{
		super();
		this.setParameters(new NamedTextList());
	} // UniformResourceLocator() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns true if this URL contains at least one query parameter
	 */
	public boolean hasQueryParameter()
	{
		return this.getParameters().size() > 0;
	} // hasQueryParameter() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the query string, that is everything following the '?'.
	 */
	public String getQueryString()
	{
		StringBuffer buffer;
		NamedTextList params;

		if (this.getParameters().size() == 0)
			return null;

		buffer = new StringBuffer();
		params = this.getParameters();
		for (int i = 0; i < params.size(); i++)
		{
			if (i > 0)
				buffer.append(URL_PARAM_SEPARATOR);

			buffer.append(params.nameAt(i));
			buffer.append(URL_PARAM_ASSIGN);
			buffer.append(params.textAt(i));
		}
		return buffer.toString();
	} // getQueryString() 

	// -------------------------------------------------------------------------

	public void setQueryString(String query)
	{
		this.parseQueryString(query);
	} // setQueryString() 

	// -------------------------------------------------------------------------

	/**
	 * Return the server name
	 */
	public String getServerName()
	{
		return this.serverName();
	} // getServerName() 

	// -------------------------------------------------------------------------

	public void setServerName(String name)
	{
		this.serverName(name);
	} // setServerName() 

	// -------------------------------------------------------------------------

	/**
	 * Return the defined port. If the port is not defined (i.e zero (0))
	 * it returns the default port for the current scheme.
	 */
	public int getPort()
	{
		if (DEFAULT_PORT == this.port())
		{
			if (SCHEME_HTTP.equals(this.getScheme()))
			{
				return HTTP_DEFAULT_PORT;
			}
			if (SCHEME_HTTPS.equals(this.getScheme()))
			{
				return HTTPS_DEFAULT_PORT;
			}
		}
		return this.port();
	} // getPort() 

	// -------------------------------------------------------------------------

	/**
	 * Set the port of the URL
	 */
	public void setPort(int newValue)
	{
		this.port(newValue);
	} // setPort() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the request URI
	 */
	public String getRequestURI()
	{
		return this.requestURI();
	} // getRequestURI() 

	// -------------------------------------------------------------------------

	public void setRequestURI(String uri)
	{
		if (!uri.startsWith(SLASH))
		{
			this.requestURI(SLASH + uri);
		}
		else
		{
			this.requestURI(uri);
		}
	} // setRequestURI() 

	// -------------------------------------------------------------------------

	/**
	 * Sets the given filename to the end of the requestURI.
	 * If there is already a filename it will be replaced.
	 */
	public void setFilename(String filename)
	{
		String uri;

		if (filename != null)
		{
			uri = this.getRequestURI();
			if (uri.endsWith(SLASH))
			{
				uri += filename;
			}
			else
			{
				uri = SU.cutTail(uri, SLASH);
				uri = uri + SLASH + filename;
			}
			this.setRequestURI(uri);
		}
	} // setFilename() 

	// -------------------------------------------------------------------------

	public String getScheme()
	{
		return this.scheme();
	} // getScheme() 

	// -------------------------------------------------------------------------

	public void setScheme(String scheme)
	{
		this.scheme(scheme);
	} // setScheme() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the names of all parameters 
	 */
	public Enumeration<String> getParameterNames()
	{
		return CU.asEnumeration(this.getParameters().names());
	} // getParameterNames() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the number of query parameters 
	 */
	public int getNumberOfParameters()
	{
		return this.getParameters().size();
	} // getNumberOfParameters() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the value of the parameter with the given name or null.
	 */
	public String getParameter(String name)
	{
		NamedText namedText;

		namedText = this.getParameters().findNamedText(name);
		if (namedText == null)
		{
			return null;
		}
		return namedText.text();
	} // getParameter() 

	// -------------------------------------------------------------------------

	public void setParameter(String name, String value)
	{
		NamedText param;

		param = this.getParameters().findNamedText(name);
		if (param == null)
		{
			this.getParameters().add(name, value);
		}
		else
		{
			param.text(value);
		}
	} // setParameter() 

	// -------------------------------------------------------------------------

	/**
	 * Remove the parameter with the given named from the list of query parameters.
	 */
	public void removeParameter(String name)
	{
		this.getParameters().removeKey(name);
	} // removeParameter() 

	// -------------------------------------------------------------------------

	/**
	 * Remove all query parameters from this URL.
	 */
	public void clearParameters()
	{
		this.setParameters(new NamedTextList());
	} // clearParameters() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if the port is not explicitly set which means the default 
	 * port has to be used.
	 */
	public boolean isDefaultPort()
	{
		if (DEFAULT_PORT == this.port())
			return true;

		if ((SCHEME_HTTP.equals(this.getScheme())) && (HTTP_DEFAULT_PORT == this.port()))
		{
			return true;
		}

		if ((SCHEME_HTTPS.equals(this.getScheme())) && (HTTPS_DEFAULT_PORT == this.port()))
		{
			return true;
		}

		return false;
	} // isDefaultPort() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string containing the server name and optionally the port number,
	 * separated by a colon (':').
	 * The port will NOT be appended to the server name if
	 * <ul>
	 * <li>the port is 0
	 * <li>the scheme is http and the port is 80
	 * <li>the scheme is https and the port is 443
	 * </ul>
	 * In all other cases the port will be appended to the server name.
	 */
	public String getServerNameAndPort()
	{
		StringBuffer buffer;

		buffer = new StringBuffer(40);
		this.appendServerNameAndPort(buffer);
		return buffer.toString();
	} // getServerNameAndPort() 

	// -------------------------------------------------------------------------

	/**
	 * Creates a copy of this object.
	 * Changing the copy has no impact on the original object.
	 */
	public UniformResourceLocator copy()
	{
		UniformResourceLocator copy;

		copy = this.newInstance();
		copy.setScheme(this.getScheme());
		copy.setServerName(this.getServerName());
		copy.setPort(this.getPort());
		copy.setRequestURI(this.getRequestURI());
		copy.setParameters(this.copyParameters());
		return copy;
	} // copy() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if this is a relative URL
	 */
	public boolean isRelative()
	{
		return (this.getScheme() == null) || (this.getServerName() == null);
	} // isRelative() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if this is an absolute URL
	 */
	public boolean isAbsolute()
	{
		return !this.isRelative();
	} // isAbsolute() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the full URL including parameters as its string representation
	 */
	public String toString()
	{
		StringBuffer buffer;

		buffer = new StringBuffer(100);

		if (this.isAbsolute())
		{
			buffer.append(this.getScheme());
			buffer.append(SCHEME_HOST_SEPARATOR);
			this.appendServerNameAndPort(buffer);
		}
		buffer.append(this.getRequestURI());
		if (this.getQueryString() != null)
		{
			buffer.append(URL_QUERY_SEPARATOR);
			buffer.append(this.getQueryString());
		}
		return buffer.toString();
	} // toString() 

	// -------------------------------------------------------------------------

	public URL toURL()
	{
		try
		{
			return new URL(this.toString());
		}
		catch (MalformedURLException ex)
		{
			return null;
		}
	} // toURL() 

	// -------------------------------------------------------------------------

	public URI toURI()
	{
		try
		{
			return new URI(this.toString());
		}
		catch (URISyntaxException ex)
		{
			return null;
		}
	} // toURI() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected void parseURL(String url) throws MalformedURLException
	{
		String[] parts;
		boolean relative;

		relative = url.startsWith(SLASH);
		if (relative)
		{
			parts = this.SU.splitNameValue(url, URL_QUERY_SEPARATOR);
		}
		else
		{
			parts = this.SU.splitNameValue(url, SCHEME_HOST_SEPARATOR);
			this.setScheme(parts[0]);
			parts = this.SU.splitNameValue(parts[1], URL_QUERY_SEPARATOR);
		}
		if (parts[1].length() > 0)
		{
			this.setQueryString(parts[1]);
			this.parseQueryString(parts[1]);
		}
		if (relative)
		{
			this.setRequestURI(parts[0]);
		}
		else
		{
			parts = this.SU.splitNameValue(parts[0], SLASH);
			this.setRequestURI("/" + parts[1]);
			parts = this.SU.splitNameValue(parts[0], URL_PORT_SEPARATOR);
			this.setServerName(parts[0]);
			if (parts[1].length() > 0)
			{
				try
				{
					this.setPort(Integer.parseInt(parts[1]));
				}
				catch (NumberFormatException e)
				{
					throw new MalformedURLException("Invalid port definition: " + parts[1]);
				}
			}
		}
	} // parseURL() 

	// -------------------------------------------------------------------------

	protected void parseQueryString(String queryString)
	{
		String[] params;
		String[] keyValue;

		this.clearParameters();

		if (queryString == null)
		{
			return;
		}
		params = this.SU.parts(queryString, URL_PARAM_SEPARATOR);
		for (int i = 0; i < params.length; i++)
		{
			keyValue = this.SU.splitNameValue(params[i], URL_PARAM_ASSIGN);
			this.setParameter(keyValue[0], keyValue[1]);
		}
	} // parseQueryString() 

	// -------------------------------------------------------------------------

	protected void appendServerNameAndPort(StringBuffer buffer)
	{
		buffer.append(this.getServerName());
		if (!this.isDefaultPort())
		{
			buffer.append(URL_PORT_SEPARATOR);
			buffer.append(this.getPort());
		}
	} // appendServerNameAndPort() 

	// -------------------------------------------------------------------------

	protected NamedTextList copyParameters()
	{
		NamedTextList copy;

		copy = new NamedTextList();
		for (int i = 0; i < parameters.size(); i++)
		{
			copy.add(parameters.nameAt(i), parameters.textAt(i));
		}
		return copy;
	} // copyParameters() 

	// -------------------------------------------------------------------------

	protected UniformResourceLocator newInstance()
	{
		return new UniformResourceLocator();
	} // newInstance() 

	// -------------------------------------------------------------------------

} // class UniformResourceLocator 
