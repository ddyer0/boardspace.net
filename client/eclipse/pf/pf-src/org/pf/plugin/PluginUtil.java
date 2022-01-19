// ===========================================================================
// CONTENT  : CLASS PluginUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 02/03/2007
// HISTORY  :
//  27/12/2004  mdu  CREATED
//	25/03/2006	mdu		changed	-->	Extracted determination of lookup path to FileUtil
//	02/03/2007	mdu		changed	-->	removed dead code
//
// Copyright (c) 2004-2007, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.plugin ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.file.Classpath;
import org.pf.file.FileUtil;

/**
 * Provides helper methods around the PF plug-in mechanism.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class PluginUtil
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String SYS_PROP_PLUGIN_PATH = "org.pf.plugin.path" ;

  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static PluginUtil soleInstance = new PluginUtil() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  /**
   * Returns the only instance this class supports (design pattern "Singleton")
   */
  public static PluginUtil current()
  {
    return soleInstance ;
  } // current() 

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected PluginUtil()
  {
    super() ;
  } // PluginUtil() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the classpath that is used to lookup any plug-in information files.
	 */
	public Classpath getPluginClasspath() 
	{
		return this.fileUtil().getLookupPath( SYS_PROP_PLUGIN_PATH ) ;
	} // getPluginClasspath() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected FileUtil fileUtil()
	{
		return FileUtil.current() ;
	} // fileUtil() 

	// -------------------------------------------------------------------------

} // class PluginUtil 
