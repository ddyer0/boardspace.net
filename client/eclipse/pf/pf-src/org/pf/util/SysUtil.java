// ===========================================================================
// CONTENT  : CLASS SysUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.5 - 23/06/2011
// HISTORY  :
//  25/12/2004  mdu  CREATED
//	03/06/2006	mdu		added		-->	get OS family name
//	28/07/2006	mdu		changed	-->	determine if isEclipse only once
//	02/03/2008	mdu		added		-->	support for system exit listener
//	17/11/2010	mdu		added		-->	isAppletEnvironment()
//	23/06/2011	mdu		changed	--> Added other names for Unix OS detection
//
// Copyright (c) 2004-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pf.bif.lifecycle.ISystemExitListener;

/**
 * Provides generally useful methods for system inquiries and other system related
 * functionality.
 * <p>
 * For example it is strongly recommended to uset {@link SysUtil#exit(int)} rather
 * than {@link System#exit(int)} to terminate a JVM. It allows registered
 * listeners to be notified just before the exit happens.
 *
 * @author Manfred Duchrow
 * @version 1.5
 */
public class SysUtil
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/** "unknown" */
	public static final String OS_FAMILY_UNKNOWN 			= "unknown";
	/** "windows" */
	public static final String OS_FAMILY_WINDOWS 			= "windows";
	/** "unix" */
	public static final String OS_FAMILY_UNIX 				= "unix";
	/** "macintosh" */
	public static final String OS_FAMILY_MACINTOSH 		= "macintosh";

	protected final static String OS_PROPERTY_NAME 		= "os.name";
	protected final static String PF_OS_PROPERTY_NAME = "org.pf.util.os.name";

	protected static final String SOLARIS = "SOLARIS";
	protected static final String SUN_OS = "SUNOS";
	protected static final String LINUX = "LINUX";
	protected static final String AIX = "AIX";
	protected static final String HP_UX = "HP-UX";
	protected static final String FREE_BSD = "FREEBSD";
	protected static final String DIGITAL_UNIX = "DIGITAL";
	protected static final String OSF1 = "OSF1";
	protected static final String IRIX = "IRIX";

	protected static final String[] UNIX_SYSTEMS = { SOLARIS, SUN_OS, AIX, HP_UX, LINUX, FREE_BSD, DIGITAL_UNIX, OSF1, IRIX };

	private static final String OS_FAMILY = determineOsFamilyName() ; 
	private static final boolean IS_WINDOWS = checkIfWindows() ; 
	
	private static final SysUtil soleInstance = new SysUtil() ;
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private Boolean isEclipse = null ;
	
  private List exitListeners = new ArrayList() ;
  protected List getExitListeners() { return exitListeners ; }
  protected void setExitListeners( List newValue ) { exitListeners = newValue ; }
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Returns the only instance this class supports (design pattern "Singleton")
   */
  public static SysUtil current()
  {
    return soleInstance ;
  } // current() 
  
  // -------------------------------------------------------------------------

  protected static boolean checkIfWindows() 
	{
		return OS_FAMILY_WINDOWS.equals( determineOsFamilyName() ) ;
	} // checkIfWindows() 

	// -------------------------------------------------------------------------
  
  protected static String	determineOsFamilyName()
  {
  	String osName ;
  	
  	osName = System.getProperty( PF_OS_PROPERTY_NAME ) ;
  	if ( osName == null )
		{
  		osName = System.getProperty( OS_PROPERTY_NAME, "" ) ;
		}
  	return determineOsFamilyName( osName ) ;
  } // determineOsFamilyName() 
  
  // -------------------------------------------------------------------------
  
  protected static String	determineOsFamilyName( String operationSystemName )
  {
  	String osName ;
  	
  	osName = operationSystemName.toUpperCase() ;
  	if ( osName.startsWith( "WINDOWS" ) )
  	{
  		return OS_FAMILY_WINDOWS ;
  	}
  	if ( osName.startsWith( "MAC" ) )
  	{
  		return OS_FAMILY_MACINTOSH ;
  	}
  	if ( CollectionUtil.current().indexOf( UNIX_SYSTEMS, osName ) >= 0 )
  	{
  		return OS_FAMILY_UNIX ;
  	}
  	return OS_FAMILY_UNKNOWN ;
  } // determineOsFamilyName() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected SysUtil()
  {
    super() ;
  } // SysUtil() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true if the current operating-system is Windows. 
   */
  public boolean isWindows() 
	{
		return IS_WINDOWS ;
	} // isWindows() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the environment is based on eclipse.
   * That is, this class is loaded within an eclipse plug-in.
   * Can also be defined by setting system property "org.pf.util.isEclipse"
   * to true or false.
   */
  public boolean isEclipse() 
	{
  	if ( isEclipse == null )
		{
			isEclipse = Bool.toBoolean( this.determineIfEclipse() ) ;
		}
  	return isEclipse.booleanValue() ;
	} // isEclipse() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the code is running as part of an applet in a browser
   * plugin JVM. 
   */
  public boolean isAppletEnvironment() 
	{
		return Bool.isTrue(System.getProperty("java.version.applet"));
	} // isAppletEnvironment() 
	
	// -------------------------------------------------------------------------
  
	/**
	 * Returns the family name of the current operating system according to the 
	 * name specified in system property "os.name".
	 * 
	 * @return "windows or "unix" or "macintosh" or "unknown" for all others
	 * @see #OS_FAMILY_UNKNOWN
	 * @see #OS_FAMILY_UNIX
	 * @see #OS_FAMILY_MACINTOSH
	 * @see #OS_FAMILY_WINDOWS
	 */
  public String getOsFamily() 
	{
		return OS_FAMILY ;
	} // getOsFamily() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Sets the current thread to sleep for the specified time in milliseconds
   * or until it gets interrupted.
   * Avoids throwing an InterruptedException.
   * 
   * @param milliseconds The time to sleep
   */
  public void sleep( long milliseconds ) 
	{
		try
		{
			Thread.currentThread().sleep( milliseconds ) ;
		}
		catch ( InterruptedException e )
		{
			// Don't care, just wake up.
		}
	} // sleep() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Calling this method terminates the JVM.
   * <br>
   * It should be used instead of System.exit(int) because it will notify
   * all registered ISystemExitListener before it actually calls System.exit(rc).
   * 
   *  @param rc The return code to the program that started the JVM. 
   */
  public void exit( int rc ) 
	{
  	this.notifyExitListeners( rc ) ;
		System.exit( rc ) ;
	} // exit() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Calling this method terminates the JVM after the specified sleep time.
   * <br>
   * It should be used instead of System.exit(int) because it will notify
   * all registered ISystemExitListener before it actually calls System.exit(rc).
   * <br>
   * This method waits the specified sleep time before it actually exits. 
   * 
   * @param rc The return code to the program that started the JVM. 
   * @param sleepBeforeExit The time to sleep (in milliseconds) before exiting.
   */
  public void exit( int rc, long sleepBeforeExit ) 
  {
  	this.sleep( sleepBeforeExit ) ;
  	this.exit( rc ) ;
  } // exit() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Add the given listener to an internal list so that it would be called right
   * before system exit gets executed via the {@link #exit(int)} method.
   * <br>
   * If the listener is already in the internal listener list it will not be
   * added again.
   * 
   * @param listener The listener to add. Will be ignored if null.
   */
  public void addSystemExitListener( ISystemExitListener listener ) 
	{
		if ( ( listener != null ) && ( !this.getExitListeners().contains( listener ) ) )
		{
			this.getExitListeners().add( listener ) ;
		}
	} // addSystemExitListener() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Removes the given listener from the internal list so that it would not 
   * be notified anymore before system exit gets executed via the {@link #exit(int)} 
   * method.
   * <br>
   * If the listener is not in the internal listener list, nothing happens.
   * 
   * @param listener The listener to remove. Will be ignored if null.
   */
  public void removeSystemExitListener( ISystemExitListener listener ) 
  {
  	if ( listener != null )
  	{
  		this.getExitListeners().remove( listener ) ;
  	}
  } // removeSystemExitListener() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Inform all exit listeners that now the system will be exited.
   * 
   * @param rc The return code for the exit.
   */
  protected void notifyExitListeners( final int rc ) 
	{
		Iterator iter ;
		ISystemExitListener listener;
		
		iter = this.getExitListeners().iterator() ;
		while ( iter.hasNext() )
		{
			listener = (ISystemExitListener) iter.next();
			listener.systemAboutToExit( rc ) ;
		}
	} // notifyExitListeners() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the environment is based on eclipse.
   * That is, this class is loaded within an eclipse plug-in.
   * Can also be defined by setting system property "org.pf.util.isEclipse"
   * to true or false.
   */
  protected boolean determineIfEclipse() 
	{
  	String option ;
  	
  	option = System.getProperty( "org.pf.util.isEclipse" ) ;
  	if ( option != null )
		{
			return Bool.isTrue( option ) ;
		}
  	try
		{
			Class clazz = this.getClass().forName( "org.eclipse.core.runtime.Platform" ) ;
			return clazz != null ;
		}
		catch ( Exception e )
		{
			return false ;
		}
	} // determineIfEclipse() 

	// -------------------------------------------------------------------------
  
  protected void reset() 
	{
  	isEclipse = null ;
	} // reset() 
	
	// -------------------------------------------------------------------------
  
} // class SysUtil 
