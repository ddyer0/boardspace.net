// ===========================================================================
// CONTENT  : CLASS NilLogger
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
 * This class implements the Logger interface but doesn't write anything
 * to any output device.
 * It could be used to provide a dummy logger implementation for an
 * application that wants to use logging, but has no logging component
 * available (in the classpath).
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class NilLogger implements Logger
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public NilLogger()
  {
    super() ;
  } // NilLogger() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Initialize the logger from the given properties settings.
   **/
  public void initialize( Properties properties )
  {
  } // initialize() 
  
  // -------------------------------------------------------------------------

  /**
   * Writes the given exception to the log output device(s).
   * The log level will be ignored.
   **/
  public void logException( Throwable ex )
  {
  } // logException() 
  
  // -------------------------------------------------------------------------

  /**
   * If the logging level is DEBUG the given message will be written to
   * the log output device(s).
   **/
  public void logDebug( String message ) 
  {
  } // logDebug() 
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is INFO or DEBUG the given message will be 
   * written to the log output device(s).
   **/
  public void logInfo( String message ) 
  {
  } // logInfo() 
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO or WARNING the given message will 
   * be written to the log output device(s).
   **/
  public void logWarning( String message )
  {
  } // logWarning() 
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO, WARNING or ERROR the given message 
   * will be written to the log output device(s).
   **/
  public void logError( String message ) 
  {
  } // logError() 
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO or WARNING the given message
   * and the exception will be written to the log output device(s).
   **/
  public void logWarning( String message, Throwable exception )
  {
  } // logWarning() 
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO, WARNING or ERROR the given message 
   * and the exception will be written to the log output device(s).
   **/
  public void logError( String message, Throwable exception ) 
  {
  } // logError() 
  
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true, if debug messages will be written to the output device(s).
   **/
  public boolean isLoggingDebugs() 
  {
    return false ;
  } // isLoggingDebugs() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true, if info messages will be written to the output device(s).
   **/
  public boolean isLoggingInfos() 
  {
    return false ;
  } // isLoggingInfos() 
  
  
  // -------------------------------------------------------------------------
  /**
   * Returns true, if warnings will be written to the output device(s).
   **/
  public boolean isLoggingWarnings() 
  {
    return false ;
  } // isLoggingWarnings() 
  
  // -------------------------------------------------------------------------
  /**
   * Returns true, if errors will be written to the output device(s).
   **/
  public boolean isLoggingErrors()
  {
    return false ;
  } // isLoggingErrors() 
  
  // -------------------------------------------------------------------------
    
  /**
   * Changes the log level to the specified level. Returns true if the level
   * is supported and was set, otherwise false.
   * 
   * @return Always false for this logger
   */
  public boolean setLogLevel( String logLevel ) 
  {
  	return false ;
  } // setLogLevel()
  
  // -------------------------------------------------------------------------
  
} // class NilLogger 
