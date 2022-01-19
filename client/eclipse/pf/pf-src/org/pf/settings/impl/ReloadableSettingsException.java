// ===========================================================================
// CONTENT  : CLASS ReloadableSettingsException
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 15/06/2002
// HISTORY  :
//  15/06/2002  duma  CREATED
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.impl ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This exception will be used for all problems occuring with reloadable
 * settings
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ReloadableSettingsException extends Exception
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public final static int INVALID_PARAMETER		= 1 ;
	public final static int FILE_NOT_FOUND			= 2 ;
	public final static int LOADING_FAILED			= 3 ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private int errorCode = 0 ;
  protected void setErrorCode( int newValue ) { errorCode = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected ReloadableSettingsException( int code, String msg )
  {
    super( msg ) ;
    this.setErrorCode( code ) ;
  } // ReloadableSettingsException()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the error code that specifies, what problem occured.
   * There is a constant defined in this class for each error code.
   */
  public int getErrorCode() { return errorCode ; }

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  // -------------------------------------------------------------------------

} // class ReloadableSettingsException