// ===========================================================================
// CONTENT  : CLASS PluginCollector
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.4 - 17/11/2010
// HISTORY  :
//  02/02/2003  mdu  CREATED
//	06/11/2003	mdu	added			->	DEBUG
//	22/12/2003	mdu	changed		->	Use logger for debug output
//	26/12/2004	mdu	changed		->	Support path definition by property "org.pf.plugin.path"
//	17/11/2010	mdu	changed		->	Support loading plugin definitions via getResourceStream()
//
// Copyright (c) 2003-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.plugin ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import org.pf.file.Classpath;
import org.pf.file.ClasspathElement;
import org.pf.file.FileUtil;

/**
 * The responsibility of this class is to detect all properties files with a
 * specific name on a given classpath and load the defined classes that fit to
 * a given class/interface into a class regitry.
 * <p>
 * Since version 1.3 it is possible to specify a system property named
 * "org.pf.plugin.path" to define the path on which the plugin collector
 * must search for plug-ins. 
 * <p>
 * Since version 1.4 it is also possible to load all plugin definitions
 * from a single file that is looked-up via the standard classloader mechanism
 * getResourceStream().
 *
 * @author Manfred Duchrow
 * @version 1.4
 */
public class PluginCollector
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String DEBUG_PREFIX = "org.pf.plugin.PluginCollector: " ;
	
  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
	public static boolean DEBUG = "true".equals( System.getProperty("org.pf.plugin.PluginCollector.debug") ) ;
	
	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
  private ClassRegistry classRegistry = null ;
  /**
   * Returns the class registry that contains all loaded plugins
   */
  public ClassRegistry getClassRegistry() { return classRegistry ; }
  protected void setClassRegistry( ClassRegistry newValue ) { classRegistry = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public PluginCollector()
  {
    this( null ) ;
  } // PluginCollector() 
  
  // -------------------------------------------------------------------------  

  /**
   * Initialize the new instance with a class registry.
   */
  public PluginCollector( ClassRegistry registry )
  {
    super() ;
    this.setClassRegistry( registry == null ? new ClassRegistry() : registry ) ;
  } // PluginCollector() 
  
  // -------------------------------------------------------------------------  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Load the class definitions from the file with the given name.
   * The file will be searched for utilizing the classloader of this class.
   * 
   * @param filename The name of the properties file that contains the class definitions
   * @return The class registry that contains the new definitions if the file has been found.
   */
  public ClassRegistry loadPluginsViaClassLoader(String filename)
  {
  	InputStream stream;
  	
		stream = this.getClass().getResourceAsStream(filename);
		if ( stream != null )
		{
			if ( DEBUG )
			{
				this.debug( "Reading plugin file " + filename + "via classloader.") ;
			}
			this.readMapping(stream, filename);
		}
		else
		{
			if ( DEBUG )
			{
				this.debug( "Classloader could not find file " + filename) ;
			}
		}
  	return this.getClassRegistry() ;	
  } // loadPluginsViaClassLoader() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Load the class definitions from all files with the given name that can
   * be found in the system classpath.
   * 
   * @param filename The name of the properties file that contains the class definitions
   * @return The class registry that contains the new definitions from all the files that have been found.
   */
	public ClassRegistry loadPlugins( String filename )
	{
		return this.loadPlugins( filename, this.getPluginClasspath() ) ;	
	} // loadPlugins() 

	// -------------------------------------------------------------------------

  /**
   * Load the class definitions from all files with the given name that can
   * be found in the specified classpath.
   * 
   * @param filename The name of the properties file that contains the class definitions
   * @param classpath The classpath the specified filename to be looked up in
   */
	public ClassRegistry loadPlugins( String filename, String classpath )
	{
		this.readPlugins( filename, classpath ) ;	
		return this.getClassRegistry() ;	
	} // loadPlugins() 

	// -------------------------------------------------------------------------

  /**
   * Load the class definitions from all files with the given name that can
   * be found in the specified classpath.
   * 
   * @param filename The name of the properties file that contains the class definitions
   * @param classpath The classpath on which to lookup the the specified filename
   */
	public ClassRegistry loadPlugins( String filename, Classpath classpath )
	{
		this.readPlugins( filename, classpath ) ;	
		return this.getClassRegistry() ;	
	} // loadPlugins() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Read all classes specified in the given file, if it exists in the specified
   * classpath element. Add those classes to the registry if they can be
   * instantiated and are of the correct type.
   *  
   * @param filename The name of the properties file that contains the class definitions
   * @param classpath The classpath the specified filename to be looked up in
   */
	protected void readPlugins( String filename, String strClasspath )
	{
		Classpath classpath ;
	
		classpath = new Classpath( strClasspath ) ;
		classpath.removeDuplicates() ;
		this.readPlugins( filename, classpath ) ;
	} // readPlugins() 

	// -------------------------------------------------------------------------

  /**
   * Read all classes specified in the given file, if it exists in the specified
   * classpath element. Add those classes to the registry if they can be
   * instantiated and are of the correct type.
   *  
   * @param filename The name of the properties file that contains the class definitions
   * @param classpath The classpath the specified filename to be looked up in
   */
	protected void readPlugins( String filename, Classpath classpath )
	{
		ClasspathElement[] elements ;
	
		if ( DEBUG )
		{
			this.debug( "Lookup file <" + filename + "> in <" + classpath + ">" ) ;
		}
		elements = classpath.elementsContaining( filename ) ;
		for (int i = 0; i < elements.length; i++)
		{
			if ( DEBUG )
			{
				this.debug( "Found file <" + filename + "> in <" + elements[i].getName() + ">" ) ;
			}
			this.readMapping( elements[i], filename ) ;			
		}
	} // readPlugins() 

	// -------------------------------------------------------------------------

	protected void readMapping( ClasspathElement element, String filename )
	{
		InputStream stream = null ;
				
		try
		{
			stream = element.open( filename ) ;
		}
		catch ( IOException ex )
		{
			if ( DEBUG )
			{
				this.debug( "Error when opening file <" + filename + "> in <" + element.getName() + "> : " + ex.toString() ) ;
			}
			return;
		}
		this.readMapping(stream, filename);
	} // readMapping() 

	// -------------------------------------------------------------------------

	/**
	 * Reads all plugin definitions from the given stream into the underlying registry.
	 * This method definitely closes the given stream (even in case of an exception).
	 * 
	 * @param stream The stream from which to read the definitions. 
	 * @param filename The filename that corresponds to the stream (just for debugging).
	 */
	protected void readMapping(InputStream stream, String filename)
	{
		Properties properties ;

		try
		{
			properties = new Properties() ;
			properties.load( stream ) ;
			this.addMappings( properties ) ;
		}
		catch ( IOException ex )
		{
			if ( DEBUG )
			{
				this.debug( "Failed to load properties from file " + filename + "    : " + ex.toString() ) ;
			}
		}	
		finally
		{
			this.fileUtil().close(stream) ;
		}
	} // readMapping() 
	
	// -------------------------------------------------------------------------
	
	protected void addMappings( Properties properties )
	{
		String key ;
		String value ;
		Iterator iter ;
		
		iter = properties.keySet().iterator() ;
		while ( iter.hasNext() )
		{
			key = (String)iter.next() ;
			value = properties.getProperty(key) ;
			if ( DEBUG )
			{
				this.debug( "Register plugin key=" + key + " | value=" + value ) ;
			}
			this.getClassRegistry().register( key, value ) ;
		}
	} // addMappings() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the classpath that is used to lookup any plug-in information files.
	 */
	protected Classpath getPluginClasspath() 
	{
		return PluginUtil.current().getPluginClasspath() ;
	} // getPluginClasspath() 

	// -------------------------------------------------------------------------
	
	protected FileUtil fileUtil()
	{
		return FileUtil.current() ;
	} // fileUtil() 

	// -------------------------------------------------------------------------

	protected void debug( String text )
	{
		LoggerProvider.getLogger().logDebug( DEBUG_PREFIX + text ) ;	
	} // debug() 

	// -------------------------------------------------------------------------
		
} // class PluginCollector 
