// ===========================================================================
// CONTENT  : INTERFACE Logger
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 23/08/2006
// HISTORY  :
//  30/11/2001  duma  CREATED
//	23/08/2006	mdu		added		-->	setLogLevel()
//
// Copyright (c) 2001-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.logging;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Properties ;

/**
 * A simple interface for all necessary logging functions.
 * Different kinds of logging components like log4j or JLog or
 * JDK 1.4 java.util.logging can be wrapped in an implementation
 * that supports this interface.
 * So programming against this interface means, to stay independent 
 * of the underlying logging component.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public interface Logger
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  
  /**
   * Initialize the logger from the given properties settings.
   **/
  public void initialize( Properties properties ) ;
  
  // -------------------------------------------------------------------------

  /**
   * Writes the given exception to the log output device(s).
   * The log level will be ignored.
   **/
  public void logException( Throwable ex ) ;
  
  // -------------------------------------------------------------------------

  /**
   * If the logging level is DEBUG the given message will be written to
   * the log output device(s).
   **/
  public void logDebug( String message ) ;
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is INFO or DEBUG the given message will be 
   * written to the log output device(s).
   **/
  public void logInfo( String message ) ;
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO or WARNING the given message will 
   * be written to the log output device(s).
   **/
  public void logWarning( String message ) ;
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO, WARNING or ERROR the given message 
   * will be written to the log output device(s).
   **/
  public void logError( String message ) ;
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO or WARNING the given message
   * and the exception will be written to the log output device(s).
   **/
  public void logWarning( String message, Throwable exception ) ;
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO, WARNING or ERROR the given message 
   * and the exception will be written to the log output device(s).
   **/
  public void logError( String message, Throwable exception ) ;
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true, if debug messages will be written to the output device(s).
   **/
  public boolean isLoggingDebugs() ;
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true, if info messages will be written to the output device(s).
   **/
  public boolean isLoggingInfos() ;
  
  // -------------------------------------------------------------------------
  /**
   * Returns true, if warnings will be written to the output device(s).
   **/
  public boolean isLoggingWarnings() ;
  
  // -------------------------------------------------------------------------
  /**
   * Returns true, if errors will be written to the output device(s).
   **/
  public boolean isLoggingErrors() ;
  
  // -------------------------------------------------------------------------
  
  /**
   * Changes the log level to the specified level. Returns true if the level
   * is supported and was set, otherwise false.
   */
  public boolean setLogLevel( String logLevel ) ;
  
  // -------------------------------------------------------------------------
  
} // interface Logger