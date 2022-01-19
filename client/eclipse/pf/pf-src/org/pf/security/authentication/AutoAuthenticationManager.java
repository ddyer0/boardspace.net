package org.pf.security.authentication;
// ===========================================================================
// CONTENT  : CLASS AutoAuthenticationManager
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 12/09/2004
// HISTORY  :
//  12/09/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.pf.text.StringUtil;
import org.pf.text.Version;

/**
 * This manager class is responsible to hold a registry of LocationCredentials
 * objects. With the method aboutToAccess( URL ) it selects the appropriate 
 * Authenticator from its regestry and sets in as default into the 
 * java.net.Authenticator class. That allows to do automatic authentication for
 * different (URLs).
 * <br>
 * <b>Be aware that this code is not thread-safe!</b>
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class AutoAuthenticationManager extends Authenticator
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final Version HOST_SUPPORT_VERSION = new Version( "1.4" ) ;
	protected static final Version JAVA_VERSION = new Version( System.getProperty( "java.version" ) ) ;

  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static AutoAuthenticationManager soleInstance = new AutoAuthenticationManager() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Map locationCredentials = new HashMap() ;
  protected Map getLocationCredentials() { return locationCredentials ; }
  protected void setLocationCredentials( Map newValue ) { locationCredentials = newValue ; }
  
  private LocationCredentials activeCredentials = null ;
  protected LocationCredentials getActiveCredentials() { return activeCredentials ; }
  protected void setActiveCredentials( LocationCredentials newValue ) { activeCredentials = newValue ; }
  
  private AbstractAuthenticator fallbackAuthenticator = null ;
  /**
   * Returns the fallback authenticator which will be called if no credentials
   * can be found in the registered credtentials.
   */
  public AbstractAuthenticator getFallbackAuthenticator() { return fallbackAuthenticator ; }
  /**
   * Sets the fallback authenticator which will be called if no credentials
   * can be found in the registered credtentials.
   */
  public void setFallbackAuthenticator( AbstractAuthenticator newValue ) { fallbackAuthenticator = newValue ; }
  
  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================
  /**
   * Must be called once, to activate this authentication manager mechanism
   * within the java.net.Authenticator.
   * 
   * @return The single instance of this class
   */
  public static AutoAuthenticationManager install() 
	{
		Authenticator.setDefault( instance() ) ;
		return instance() ;
	} // install() 

	// -------------------------------------------------------------------------  
  
  /**
   * Can be called to remove this authentication manager from 
   * the java.net.Authenticator.
   */
  public static void uninstall() 
	{
		Authenticator.setDefault( null ) ;
	} // uninstall() 

	// -------------------------------------------------------------------------  
  
  /**
   * Returns the only instance this class supports (design pattern "Singleton")
   */
  public static AutoAuthenticationManager instance()
  {
    return soleInstance ;
  } // instance() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  private AutoAuthenticationManager()
  {
    super() ;
  } // AutoAuthenticationManager() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * This method must be called to activate the authenticator that provides
   * credentials to be able to access the given URL.
   * 
   * @param url The URL that might need authentication to be accessed
   * @return true if an authenticator for the given URL was activated
   */
  public boolean aboutToAccess( String url ) 
	{
  	LocationCredentials credentials ;
  	
  	credentials = this.findCredentialsFor( url ) ;
  	this.setActiveCredentials( credentials ) ;
  	return this.hasActiveCredentials() ;
	} // aboutToAccess() 

	// -------------------------------------------------------------------------
  
  /**
   * This method must be called to activate the authenticator that provides
   * credentials to be able to access the given URL.
   * 
   * @param url The URL that might need authentication to be accessed
   * @return true if an authenticator for the given URL was activated
   */
  public boolean aboutToAccess( URL url ) 
	{
  	if ( url == null )
		{
    	this.setActiveCredentials( null ) ;
    	return this.hasActiveCredentials() ;
		}
		else
		{
	  	return this.aboutToAccess( url.toString() ) ;
		}
	} // aboutToAccess() 

	// -------------------------------------------------------------------------
  
  /**
   * Registers the given location credentials under the specified id.
   * The id can be used to remove the credentials once again. 
   */
  public void register( String id, LocationCredentials credentials ) 
	{
  	if ( ( ! this.str().isNullOrBlank( id ) ) && credentials != null )
		{
			this.getLocationCredentials().put( id, credentials ) ;
		}
	} // register() 

	// -------------------------------------------------------------------------

  /**
   * Removes the location credentials registered under the specified key from
   * the registry.
   * Returns the removed credentials or null if not found.
   */
  public LocationCredentials remove( String id ) 
	{
		if ( this.str().isNullOrBlank( id ) )
			return null ;
		
		return (LocationCredentials)this.getLocationCredentials().remove( id ) ;
	} // remove() 

	// -------------------------------------------------------------------------
  
  /**
   * Removes all registered credentials and the fallback authenticator.
   */
  public void reset() 
	{
		this.setLocationCredentials( new HashMap() ) ;
		this.setFallbackAuthenticator( null ) ;
		this.setActiveCredentials( null ) ;
	} // reset()

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected PasswordAuthentication getPasswordAuthentication()
	{
  	PasswordAuthentication auth ;
  	
		if ( this.hasActiveCredentials() )
		{
			auth = this.getActiveCredentials().getPasswordAuthentication() ;
		}
		else
		{
			auth =  this.findPasswordAuthenticationForRealm() ;
		}
		if ( ( auth == null ) && ( this.getFallbackAuthenticator() != null ) )
		{
			this.fillFallbackAuthenticator() ;
			auth = this.getFallbackAuthenticator().getPasswordAuthentication() ; 
		}
		this.setActiveCredentials( null ) ;
		return auth ;
	} // getPasswordAuthentication() 
  
  // -------------------------------------------------------------------------
  
  protected PasswordAuthentication findPasswordAuthenticationForRealm() 
	{
  	LocationCredentials creds ; 
  	
  	creds = this.findCredentialsForRealm() ;
  	if ( creds == null )
		{
			return null ;
		}
  	return creds.getPasswordAuthentication() ;
	} // findPasswordAuthenticationForRealm()

	// -------------------------------------------------------------------------
  
  /**
   * Returns the location credentials registered under the specified key or 
   * null if not found.
   */
  protected LocationCredentials getCredentials( String id ) 
	{
		if ( this.str().isNullOrBlank( id ) )
			return null ;
		
		return (LocationCredentials)this.getLocationCredentials().get( id ) ;
	} // getCredentials() 

	// -------------------------------------------------------------------------
  
  protected LocationCredentials findCredentialsFor( String url ) 
	{
		Iterator iter ;
		LocationCredentials creds ; 
		
		if ( this.str().isNullOrBlank( url ) )
			return null ;
		
		iter = this.getLocationCredentials().values().iterator() ;
		while ( iter.hasNext() )
		{
			creds = (LocationCredentials)iter.next();
			if ( creds.appliesToURL( url ) ) 
				return creds ;
		}
		return null ;
	} // findCredentialsFor() 

	// -------------------------------------------------------------------------
  
  protected LocationCredentials findCredentialsForRealm() 
	{
  	if ( this.isBasicHttp() )
		{
  		return this.findCredentialsForRealm( this.getRequestingPrompt() ) ;
		}
		else
		{
			return null ;
		}
	} // findCredentialsForRealm() 

	// -------------------------------------------------------------------------
  
  protected LocationCredentials findCredentialsForRealm( String realm ) 
	{
		Iterator iter ;
		LocationCredentials creds ; 
		
		if ( this.str().isNullOrBlank( realm ) )
			return null ;
		
		iter = this.getLocationCredentials().values().iterator() ;
		while ( iter.hasNext() )
		{
			creds = (LocationCredentials)iter.next();
			if ( creds.appliesToRealm( realm ) ) 
				return creds ;
		}
		return null ;
	} // findCredentialsForRealm() 

	// -------------------------------------------------------------------------
  
  protected void fillFallbackAuthenticator() 
	{
  	AbstractAuthenticator auth ;
  	
  	auth = this.getFallbackAuthenticator() ;
  	if ( this.supportsHostNames() )
		{
  		auth.setRequestingHost( this.getRequestingHost() ) ;
		}
  	else
  	{
  		auth.setRequestingHost( null ) ;
  	}
		auth.setRequestingPort( this.getRequestingPort() ) ;
		auth.setRequestingPrompt( this.getRequestingPrompt() ) ;
		auth.setRequestingProtocol( this.getRequestingProtocol() ) ;
		auth.setRequestingScheme( this.getRequestingScheme() ) ;
		auth.setRequestingSite( this.getRequestingSite() ) ;
	} // fillFallbackAuthenticator()

	// -------------------------------------------------------------------------
  
  protected boolean hasActiveCredentials() 
	{
		return this.getActiveCredentials() != null ;
	} // hasActiveCredentials() 

	// -------------------------------------------------------------------------
  
  protected boolean isBasicHttp() 
	{
		return ( "Basic".equalsIgnoreCase( this.getRequestingScheme() )
				&&	"http".equalsIgnoreCase( this.getRequestingProtocol() ) 
				)	;
	} // isBasicHttp() 

	// -------------------------------------------------------------------------
  
  protected boolean supportsHostNames() 
	{
		return ! JAVA_VERSION.isLessThan( HOST_SUPPORT_VERSION ) ;
	} // supportsHostNames()

	// -------------------------------------------------------------------------
  
  protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	//-------------------------------------------------------------------------
  
} // class AutoAuthenticationManager 
