// ===========================================================================
// CONTENT  : CLASS PrintStreamLogger
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 20/12/2003
// HISTORY  :
//  30/11/2001  duma  CREATED
//	17/10/2003	duma	changed	-->	log level constants and methods are now public
//	06/11/2003	duma	changed	-->	Check properties == null in initialize()
//	20/12/2003	duma	changed	-->	Visibility of setLogLevel() from protected to public
//
// Copyright (c) 2001-2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.logging;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Properties ;
import java.io.* ;

/**
 * This logger supports simple output to a print stream. By default that
 * print stream is stdout. But it can changed by setting the property
 * 'logging.printstream.file' to a filename. Then it will open that file
 * at first access and appends all output to it.
 * <p>
 * The initial log level is ERROR. It can be changed via the property 
 * 'logging.level' (e.g. logging.level=WARNING). 
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class PrintStreamLogger implements Logger
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  /**
   * This log level specifies that no message at all will be logged
   */
  public static final int LEVEL_NONE     = 0 ;
	/**
	 * This log level specifies that only error messages will be logged
	 */
	public static final int LEVEL_ERROR    = 1 ;
	/**
	 * This log level specifies that only error and warning messages will be 
	 * logged
	 */
	public static final int LEVEL_WARN     = 2 ;
	/**
	 * This log level specifies that only error, warning and info messages will 
	 * be logged
	 */
	public static final int LEVEL_INFO     = 3 ;
	/**
	 * This log level specifies that all messages will be logged
	 */
	public static final int LEVEL_DEBUG    = 4 ;

	/**
	 * This is the log level string representation for NONE
	 */
	public static final String LL_NONE    = "NONE" ;
	/**
	 * This is the log level string representation for ERROR
	 */
	public static final String LL_ERROR    = "ERROR" ;
	/**
	 * This is the log level string representation for WARNING
	 */
	public static final String LL_WARNING    = "WARNING" ;
	/**
	 * This is the log level string representation for INFO
	 */
	public static final String LL_INFO    = "INFO" ;
	/**
	 * This is the log level string representation for DEBUG
	 */
	public static final String LL_DEBUG    = "DEBUG" ;

	/**
	 * The property that specifies a filename to redirect the log output
	 * <p>"logging.printstream.file"<p>
	 */
	public static final String PROP_OUTPUT_FILE    = "logging.printstream.file" ;
	/**
	 * The property to set the log level. The value must be one of the following 
	 * strings: "NONE", "ERROR", "WARNING", "INFO", "DEBUG"
	 * <p>"logging.level"<p>
	 */
	public static final String PROP_LOG_LEVEL    	= "logging.level" ;
  
  private static final String[] LEVEL_INDICATOR = { "", "E", "W", "I", "D", "X" } ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private int logLevel = LEVEL_ERROR ;
  /**
   * Returns the current log level
   */  
  public int getLogLevel() { return logLevel ; }
  /**
   * Set the current log level of this logger.
   * <br>
   * The default is LEVEL_ERROR.
   * @param newLevel The new log level (i.e. one of the LEVEL_ constants of this class)
   */
  public void setLogLevel( int newLevel ) { logLevel = newLevel ; }
  
  // -------------------------------------------------------------------------
  
  private PrintStream printStream = System.out ;
  protected PrintStream getPrintStream() { return printStream ; }
  protected void setPrintStream( PrintStream newValue ) { printStream = newValue ; }  
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public PrintStreamLogger()
  {
    super() ;
  } // PrintStreamLogger()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Initialize the logger from the given properties settings.
   * Currently the following properties are supported:
   * <dl>
   * <dt>logging.printstream.file</dt>
   * <dd>The name of a file to which all logging should be redirected</dd>
   * <dt>logging.level</dt>
   * <dd>The log level. Must be one of "NONE", "ERROR", "WARNING", "INFO", "DEBUG"</dd>
   * </dl>
   **/
  public void initialize( Properties properties )
  {
    String value    = null ;
    
    if ( properties != null )
    {
	    value = properties.getProperty( PROP_OUTPUT_FILE ) ;
	    this.initPrintStream( value ) ;
	    
			value = properties.getProperty( PROP_LOG_LEVEL ) ;
			this.initLogLevel( value ) ;
    }
  } // initialize()
  
  // -------------------------------------------------------------------------

  /**
   * Writes the given exception to the log output device(s).
   * The log level will be ignored.
   **/
  public void logException( Throwable ex )
  {
    this.printException( ex ) ;
  } // logException()
  
  // -------------------------------------------------------------------------

  /**
   * If the logging level is DEBUG the given message will be written to
   * the log output device(s).
   **/
  public void logDebug( String message ) 
  {
    if ( this.isLoggingDebugs() )
      this.println( LEVEL_DEBUG, message ) ;
  } // logDebug()  
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is INFO or DEBUG the given message will be 
   * written to the log output device(s).
   **/
  public void logInfo( String message ) 
  {
    if ( this.isLoggingInfos() )
      this.println( LEVEL_INFO, message ) ;
  } // logInfo()
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO or WARNING the given message will 
   * be written to the log output device(s).
   **/
  public void logWarning( String message )
  {
    if ( this.isLoggingWarnings() )
      this.println( LEVEL_WARN, message ) ;
  } // logWarning()
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO, WARNING or ERROR the given message 
   * will be written to the log output device(s).
   **/
  public void logError( String message ) 
  {
    if ( this.isLoggingErrors() )
      this.println( LEVEL_ERROR, message ) ;
  } // logError()
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO or WARNING the given message
   * and the exception will be written to the log output device(s).
   **/
  public void logWarning( String message, Throwable exception )
  {
    if ( this.isLoggingWarnings() )
    {
      this.println( LEVEL_WARN, message ) ;
      this.printException( exception ) ;
    }
  } // logWarning()
  
  // -------------------------------------------------------------------------
  
  /**
   * If the logging level is DEBUG, INFO, WARNING or ERROR the given message 
   * and the exception will be written to the log output device(s).
   **/
  public void logError( String message, Throwable exception ) 
  {
    if ( this.isLoggingErrors() )
    {
      this.println( LEVEL_ERROR, message ) ;
      this.printException( exception ) ;
    }
  } // logError()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true, if debug messages will be written to the output device(s).
   **/
  public boolean isLoggingDebugs() 
  {
    return ( this.getLogLevel() >= LEVEL_DEBUG ) ;
  } // isLoggingDebugs()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true, if info messages will be written to the output device(s).
   **/
  public boolean isLoggingInfos() 
  {
    return ( this.getLogLevel() >= LEVEL_INFO ) ;
  } // isLoggingInfos()
  
  // -------------------------------------------------------------------------
  /**
   * Returns true, if warnings will be written to the output device(s).
   **/
  public boolean isLoggingWarnings() 
  {
    return ( this.getLogLevel() >= LEVEL_WARN ) ;
  } // isLoggingWarnings()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns true, if errors will be written to the output device(s).
   **/
  public boolean isLoggingErrors()
  {
    return ( this.getLogLevel() >= LEVEL_ERROR ) ;
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
  	return this.initLogLevel( logLevel ) ;
  } // setLogLevel()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  protected void print( String text )
  {
    this.getPrintStream().print( text ) ;
  } // print()
  
  protected void print( int level, String text )
  {
    if ( this.useLevelIndicators() )
    {
      this.print( this.getLevelIndicator( level ) ) ;
      this.print( " " ) ;
    }
    this.print( text ) ;
  } // print()
  
  // -------------------------------------------------------------------------

  protected void println( String text )
  {
    this.print( text ) ;
    this.print( "\n" ) ;
  } // println()
  
  // -------------------------------------------------------------------------

  protected void println( int level, String text )
  {
    this.print( level, text ) ;
    this.println() ;
  } // println()
  
  // -------------------------------------------------------------------------

  protected void println()
  {
    this.println( "" ) ;
  } // println()
  
  // -------------------------------------------------------------------------

  protected void printException( Throwable ex )
  {
    ex.printStackTrace( this.getPrintStream() ) ;
  } // print()
  
  // -------------------------------------------------------------------------

  protected String getLevelIndicator( int level )
  {
    if ( ( level < 0 ) || ( level >= LEVEL_INDICATOR.length ) )
      return LEVEL_INDICATOR[LEVEL_INDICATOR.length-1] ;
    else
      return LEVEL_INDICATOR[level] ;
  } // getLevelIndicator()
  
  // -------------------------------------------------------------------------

  protected boolean useLevelIndicators()
  {
    return true ;
  } // useLevelIndicators()
  
  // -------------------------------------------------------------------------

  protected void initPrintStream( String filename ) 
  {
    File file           = null ;
    FileOutputStream os = null ;
    PrintStream ps      = null ;
    
    if ( filename != null )
    {
      try
      {
        file = new File( filename ) ;
        os = new FileOutputStream( file ) ;
        ps = new PrintStream( os ) ;
        this.setPrintStream( ps ) ;
      }
      catch ( IOException ex )
      {
        this.logError( "Failed to create file '" + filename + "' for logging", ex ) ;
      }
    }
  } // initPrintStream()
  
  // -------------------------------------------------------------------------

	protected boolean initLogLevel( String level )
	{
		String levelName ;
		
		if ( level == null )
			return false ;
			
		levelName = level.toUpperCase() ; 
		
		if ( LL_NONE.equals( levelName ) )
			this.setLogLevel( LEVEL_NONE ) ;
		else if ( LL_ERROR.equals( levelName ) )
			this.setLogLevel( LEVEL_ERROR ) ;
		else if ( LL_WARNING.equals( levelName ) )
			this.setLogLevel( LEVEL_WARN ) ;
		else if ( LL_INFO.equals( levelName ) )
			this.setLogLevel( LEVEL_INFO ) ;
		else if ( LL_DEBUG.equals( levelName ) )
			this.setLogLevel( LEVEL_DEBUG ) ;
		else
			return false ;
		
		return true ;
	} // initLogLevel()

	// -------------------------------------------------------------------------

} // class PrintStreamLogger