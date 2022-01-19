// ===========================================================================
// CONTENT  : ABSTRACT CLASS AbstractAuthenticator
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
import java.net.InetAddress;
import java.net.PasswordAuthentication;

/**
 * This class implements (nearly) the same methods as java.net.Authenticator.
 * Beyond that it provides public setter methods for all authentication relevant
 * fields. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
abstract public class AbstractAuthenticator
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String requestingHost = null ;
  public String getRequestingHost() { return requestingHost ; }
  public void setRequestingHost( String newValue ) { requestingHost = newValue ; }
  
  private int requestingPort = 0 ;
  public int getRequestingPort() { return requestingPort ; }
  public void setRequestingPort( int newValue ) { requestingPort = newValue ; }
  
  private String requestingPrompt = null ;
  public String getRequestingPrompt() { return requestingPrompt ; }
  public void setRequestingPrompt( String newValue ) { requestingPrompt = newValue ; }  
  
  private String requestingProtocol = null ;
  public String getRequestingProtocol() { return requestingProtocol ; }
  public void setRequestingProtocol( String newValue ) { requestingProtocol = newValue ; }  
  
  private String requestingScheme = null ;
  public String getRequestingScheme() { return requestingScheme ; }
  public void setRequestingScheme( String newValue ) { requestingScheme = newValue ; }
  
  private InetAddress requestingSite = null ;
  public InetAddress getRequestingSite() { return requestingSite ; }
  public void setRequestingSite( InetAddress newValue ) { requestingSite = newValue ; }  
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public AbstractAuthenticator()
  {
    super() ;
  } // AbstractAuthenticator() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Called when password authorization is needed.  Subclasses should
   * override the default implementation, which returns null.
   * @return The PasswordAuthentication collected from the
   *		user, or null if none is provided.
   */
  public PasswordAuthentication getPasswordAuthentication()
	{
		return null;
	} // getPasswordAuthentication()

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class AbstractAuthenticator 
