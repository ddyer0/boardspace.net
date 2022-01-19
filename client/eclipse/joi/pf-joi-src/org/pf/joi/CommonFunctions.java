// ===========================================================================
// CONTENT  : CLASS CommonFunctions
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 17/11/2010
// HISTORY  :
//  17/11/2010  mdu  CREATED
//
// Copyright (c) 2010, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.joi ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.plugin.PluginCollector;
import org.pf.util.SysUtil;
import org.pf.plugin.ClassRegistry;

/**
 * Implements general functions that need no state. All methods are static.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
class CommonFunctions
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final SysUtil SYS = SysUtil.current() ;

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
	/**
	 * Loads all plug-in definitions into the given class registry. Uses the defaultFilename
	 * for scanning over all classpath elements and load the definitions from each
	 * file with that name.
	 * If the current environment is an applet running in a browser then this approach 
	 * doesn't work. Therefore an array of other filenames is provided to load the
	 * definitions from them. These files are looked-up using the current classloader.  
	 */
	static void loadPluginDefinitions(ClassRegistry classRegistry, String defaultFilename, String... filenames) 
	{
    PluginCollector collector ;
    
    collector = new PluginCollector(classRegistry) ;
    if ( SYS.isAppletEnvironment() )
		{			
    	for (String filename : filenames )
			{				
    		collector.loadPluginsViaClassLoader(filename) ;
			}
		}
		else
		{
			collector.loadPlugins(defaultFilename) ;
		}
	} // loadPluginDefinitions()
	
	// -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  private CommonFunctions()
  {
    // no instances supported
  } // CommonFunctions()
  
} // class CommonFunctions
