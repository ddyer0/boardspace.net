// ===========================================================================
// CONTENT  : CLASS LocationCredentials
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 12/09/2004
// HISTORY  :
//  12/09/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.security.authentication ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.net.PasswordAuthentication;

import org.pf.text.StringPattern;
import org.pf.text.StringPatternCollection;
import org.pf.text.StringUtil;

/**
 * Holds the userId and a password for a collection of URL patterns.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class LocationCredentials
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String realmName = null ;
  protected String getRealmName() { return realmName ; }
  protected void setRealmName( String newValue ) { realmName = newValue ; }	
	
  private StringPatternCollection locationPatterns = null ;
  protected StringPatternCollection getLocationPatterns() { return locationPatterns ; }
  protected void setLocationPatterns( StringPatternCollection newValue ) { locationPatterns = newValue ; }
  
  private String userId = null ;
  protected String getUserId() { return userId ; }
  protected void setUserId( String newValue ) { userId = newValue ; }  
  
  private PasswordAuthentication credentials = null ;
  protected PasswordAuthentication getCredentials() { return credentials ; }
  protected void setCredentials( PasswordAuthentication newValue ) { credentials = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a userId and corresponding passwords.
   * URL patterns can be added later with method addPattern().
   * 
   * @param userId The userId to be used for authentication
   * @param password The password to be used for authentication
   */
  public LocationCredentials( String userId, String password )
  {
    super() ;
    this.setUserId( userId ) ;
    this.setLocationPatterns( new StringPatternCollection() ) ;
    if ( password != null )
		{
      this.setCredentials( new PasswordAuthentication( userId, password.toCharArray() ) ) ;
		}
  } // LocationCredentials() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a userId and corresponding passwords.
   * URL patterns can be added later with method addPattern().
   * 
   * @param realm The realm that is accessible with the given credentials
   * @param userId The userId to be used for authentication
   * @param password The password to be used for authentication
   */
  public LocationCredentials( String realm, String userId, String password )
  {
    this( userId, password ) ;
    this.setRealm( realm ) ;
  } // LocationCredentials() 
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the password based authentication data.
   */
  public PasswordAuthentication getPasswordAuthentication() 
	{
  	if ( this.getCredentials() == null )
		{
			this.fillCredentials() ;
		}
		return this.getCredentials() ;
	} // getPasswordAuthentication() 

	// -------------------------------------------------------------------------

  /**
   * Returns the realm this credentials apply to
   */
  public String getRealm() 
	{
		return this.getRealmName() ;
	} // getRealm() 

	// -------------------------------------------------------------------------
  
  /**
   * Sets the realm this credentials apply to
   */
  public void setRealm( String realm ) 
	{
		this.setRealmName( realm ) ;
	} // setRealm() 

	// -------------------------------------------------------------------------
  
  /**
   * Adds the given pattern to the URL patterns that can be accessed with this 
   * object's authentication credentials.
   * 
   * @param pattern A URL pattern that might contain '*' as wildcards 
   */
  public void addPattern( String pattern ) 
	{
		StringPattern strPattern ;
		
		if ( ! this.str().isNullOrBlank( pattern ) )
		{
			strPattern = StringPattern.create( pattern ) ;
			this.addPattern( strPattern ) ;
		}
	} // addPattern() 

	// -------------------------------------------------------------------------
  
  /**
   * Adds the given pattern to the URL patterns that can be accessed with this 
   * object's authentication credentials.
   * 
   * @param pattern A URL pattern that might contain '*' as wildcards 
   */
  public void addPattern( StringPattern pattern ) 
	{
		if ( pattern != null )
		{
			this.getLocationPatterns().add( pattern ) ;
		}
	} // addPattern() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the given realm and URL match the settings in this object.
   */
  public boolean appliesTo( String realm, String url ) 
	{
  	boolean applies = true ;
  	
  	if ( ( realm == null ) && ( url == null ) )
		{
			return false ;
		}
  	
  	if ( realm != null )
  		applies = applies && this.appliesToRealm( realm ) ;
  	
  	if ( url != null )
  		applies = applies && this.appliesToURL( url ) ;
  	
  	return applies ;
	} // appliesTo() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the given URL matches any of the underlying patterns.
   */
  public boolean appliesToURL( String url ) 
	{
  	if ( url == null )
  		return false ;
  	
		return this.getLocationPatterns().matchesAny( url ) ;
	} // appliesToURL() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the given realm matches the realm name of this object.
   */
  public boolean appliesToRealm( String realm ) 
	{
  	if ( ( realm == null ) || ( this.getRealm() == null ) )
  		return false ;
  	
		return this.getRealm().equalsIgnoreCase( realm ) ;
	} // appliesToRealm() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Subclasses must override this method in order to get the credentials from
   * somewhere and set a PasswordAuthentication object using setCredentials().
   * The userId might be set already and can be accessed via getUserId().
   */
  protected void fillCredentials() 
	{
		// Can't do anything here
	} // fillCredentials()

	// -------------------------------------------------------------------------
  
  protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	//-------------------------------------------------------------------------
  
} // class LocationCredentials 
