// ===========================================================================
// CONTENT  : CLASS PluginRegistry
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 25/02/2007
// HISTORY  :
//  25/02/2007  mdu  CREATED
//
// Copyright (c) 2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.plugin ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.pf.file.Classpath;
import org.pf.file.ClasspathElement;
import org.pf.file.FileUtil;
import org.pf.file.PropertyFileLoader;
import org.pf.reflect.ClassInfo;
import org.pf.reflect.Dynamic;
import org.pf.reflect.ReflectUtil;
import org.pf.text.StringPattern;
import org.pf.text.StringUtil;
import org.pf.util.CollectionUtil;
import org.pf.util.NamedValueList;
import org.pf.util.OrderedProperties;

/**
 * A special registry that is capable to hold any number of plug-ins of
 * any type.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class PluginRegistry
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final boolean DEBUG = "true".equals( System.getProperty("org.pf.plugin.PluginRegistry.debug") ) ;
	protected static final String DEBUG_PREFIX = "org.pf.plugin.PluginRegistry: " ;

	protected static final String PLUGIN_DEF_SEPARATOR = ";" ;
	protected static final String OPTIONS_START = "[" ;
	protected static final String OPTIONS_END = "]" ;
	protected static final StringPattern PLUGIN_OPTIONS_PATTERN = 
		StringPattern.create( OPTIONS_START + "*" + OPTIONS_END + "?*" ) ;
		
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private NamedValueList registry ;
  protected NamedValueList getRegistry() { return registry ; }
  protected void setRegistry( NamedValueList newValue ) { registry = newValue ; }	

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public PluginRegistry()
  {
    super() ;
    this.setRegistry( new NamedValueList() ) ;
  } // PluginRegistry() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Load all plug-ins that are defined in any library in the specified file.
   * 
   * @param filename The name of the file(s) where the plug-in defeinition is looked up
   * 
   *  @return The number of plug-ins that have been loaded to the registry
   */
  public int loadPluginsFrom( String filename ) 
	{
  	int sum ;
  	
		sum = this.readPlugins( filename, this.getPluginClasspath() ) ;
		if ( DEBUG )
		{
			this.debug( "Registered " + sum + " plug-ins" ) ;
		}
		return sum ;
	} // loadPluginsFrom() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns the plug-in with the specified ID or null
   * if no such plug-in can be found in the registry.
   * If more than one plug-in is registered under the same ID the result
   * is the first one found.
   * 
   * @param pluginId The identifier of the plug-in (must not be null)
   * @return An instance of the found plug-in or null
   */
  public Object getPlugin( String pluginId ) 
	{
  	PluginHolder pluginHolder ;
  	
  	pluginHolder = (PluginHolder)this.getRegistry().valueAt( pluginId ) ;
  	if ( pluginHolder != null )
		{
			return pluginHolder.getInstance() ;
		}
  	return null ;
	} // getPlugin() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the plug-in with the specified ID and the given type or null
   * if no such plug-in can be found in the registry.
   * 
   * @param pluginId The identifier of the plug-in (must not be null)
   * @param pluginType The type of the plug-in (must not be null)
   * @return An instance of the found plug-in or null
   */
  public Object getPlugin( String pluginId, Class pluginType ) 
  {
  	PluginHolder pluginHolder ;
  	
  	for (int i = 0; i < this.getRegistry().size(); i++ )
  	{
  		if ( pluginId.equals( this.getRegistry().nameAt(i) ) )
  		{
  			pluginHolder = (PluginHolder)this.getRegistry().valueAt(i) ;
  			if ( pluginHolder.isPluginType( pluginType ) )
  			{
  				return pluginHolder.getInstance() ;
  			}
  		}
  	}
  	return null ;
  } // getPlugin() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns all plug-ins that are of the specified type. That also implies
   * plug-ins that are subclasses or implementors of the given type.
   * 
   * @param pluginType The type the plug-ins to look for must comply with
   * @return An array of found plug-in instances (never null)
   */
  public Object[] getPluginsOfType( Class pluginType ) 
	{
  	List found ;
  	PluginHolder pluginHolder ;
  	Iterator iter ;
  	Object plugin ;
  	
  	found = new ArrayList( this.getRegistry().size() ) ;
		iter = this.getRegistry().values().iterator() ;
		while ( iter.hasNext() )
		{
			pluginHolder = (PluginHolder) iter.next();
			if ( pluginHolder.isPluginType( pluginType ) )
			{
				plugin = pluginHolder.getInstance() ;
				if ( plugin != null )
				{
					found.add( plugin ) ;					
				}
			}
		}
		
		if ( found.size() == 0 )
		{
			return (Object[])Array.newInstance( pluginType, 0 ) ;
		}
		return this.coll().toArray( found, pluginType ) ;
	} // getPluginsOfType() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the IDs of all plug-ins that are of the specified type. That also implies
   * plug-ins that are subclasses or implementors of the given type.
   * 
   * @param pluginType The type the plug-ins to look for must comply with
   * @return An array of IDs (never null)
   */
  public String[] getPluginIDsOfType( Class pluginType ) 
  {
  	List found ;
  	PluginHolder pluginHolder ;
  	
  	found = new ArrayList( this.getRegistry().size() ) ;
		for (int i = 0; i < this.getRegistry().size(); i++ )
		{
			pluginHolder = (PluginHolder)this.getRegistry().valueAt(i) ;
			if ( pluginHolder.isPluginType( pluginType ) )
			{
				found.add( this.getRegistry().nameAt(i) ) ;
			}
		}
  	if ( found.size() == 0 )
  	{
  		return StringUtil.EMPTY_STRING_ARRAY ;
  	}
  	return this.str().asStrings( found ) ;
  } // getPluginIDsOfType() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected boolean registerPlugin( PluginHolder pluginHolder ) 
	{
		String id ;
		Object object ;
		Method method ;
		
		object = pluginHolder.getInstance() ;
		if ( object == null )
		{
			return false ;
		}
		id = pluginHolder.getExternalId() ;
		method = this.find_getPluginId_Method( object ) ;
		if ( method != null )
		{
			id = (String)Dynamic.invoke( object, method, null ) ;
		}
		this.getRegistry().add( id, pluginHolder ) ;
		return true ;
	} // registerPlugin() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Read all classes specified in the given file, if it exists in the specified
   * classpath element. Add those classes to the registry if they can be
   * instantiated and are of the correct type.
   *  
   * @param filename The name of the properties file that contains the class definitions
   * @param classpath The classpath the specified filename to be looked up in
   * 
   * @return The number of plug-ins that have been added to the registry
   */
	protected int readPlugins( String filename, Classpath classpath )
	{
		ClasspathElement[] elements ;
		int sum = 0 ;
	
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
			sum += this.readPluginsFrom( elements[i], filename ) ;			
		}
		return sum ;
	} // readPlugins() 

	// -------------------------------------------------------------------------

	protected int readPluginsFrom( ClasspathElement element, String filename )
	{
		InputStream stream = null ;
		OrderedProperties properties ;

		try
		{
			stream = element.open( filename ) ;
			properties = PropertyFileLoader.loadFullPropertiesFile( stream ) ;
		}
		catch (IOException e)
		{
			if ( DEBUG )
			{
				this.debug( "Exception reading file <" + filename + "> : " + e.toString() ) ;
			}
			return 0 ;
		}
		finally
		{
			this.fileUtil().close(stream) ;
		}
		return this.addDefinitionsToRegistry( properties ) ;
	} // readPluginsFrom() 

	// -------------------------------------------------------------------------

	protected int addDefinitionsToRegistry( OrderedProperties definitions  ) 
	{
		String[] keys ;
		int sum = 0 ;
		
		keys = definitions.getPropertyNames() ;
		for (int i = 0; i < keys.length; i++ )
		{
			if ( this.parseAndAddToRegistry( keys[i], definitions.getProperty( keys[i] ) ) )
			{
				sum++ ;
			}
		}
		return sum ;
	} // addDefinitionsToRegistry() 
	
	// -------------------------------------------------------------------------
	
	protected boolean parseAndAddToRegistry( String id, String pluginDefinition ) 
	{
		PluginHolder holder ;
		String[] parts ;
		ClassInfo classInfo ;
		String className ;
		String classOptions = null ;
		Properties config ;
		String[] keyValue ;
		
		parts = this.str().parts( pluginDefinition, PLUGIN_DEF_SEPARATOR ) ;
		if ( this.str().isNullOrEmpty( parts ) )
		{
			return false ;
		}
		className = parts[0] ;
		if ( DEBUG )
		{
			this.debug( "Found <" + className + "> with id <" + id + ">" ) ;
		}
		if ( PLUGIN_OPTIONS_PATTERN.matches( className ) )
		{
			classOptions = this.str().getDelimitedSubstring( className, OPTIONS_START, OPTIONS_END ) ;
			className = this.str().cutHead( className, OPTIONS_END ) ;
		}
		classInfo = new ClassInfo( className ) ;
		if ( ! this.validateClass( classInfo ) )
		{
			return false ;
		}
		holder = new PluginHolder( id, classInfo ) ;
		if ( "1".equals( classOptions ) )
		{
			holder.isSingleton( true ) ;
		}
		config = new Properties() ;
		for (int i = 1; i < parts.length; i++ )
		{
			keyValue = this.str().splitNameValue( parts[i], "=" ) ;
			if ( !this.str().isNullOrEmpty( keyValue ) )
			{
				config.setProperty( keyValue[0], keyValue.length > 1 ? keyValue[1] : "" ) ;
			}
		}
		holder.setConfiguration( config ) ;
		return this.registerPlugin( holder ) ;
	} // parseAndAddToRegistry() 
	
	// -------------------------------------------------------------------------
	
	protected boolean validateClass( ClassInfo classInfo ) 
	{
		if ( this.validateClassFound( classInfo ) )
		{
			return this.validateInstanceCreation( classInfo ) ;
		}
		return false ;
	} // validateClass() 
	
	// -------------------------------------------------------------------------
	
	protected boolean validateClassFound( ClassInfo classInfo ) 
	{
		Class aClass;

		aClass = classInfo.getClassObject();
		if ( aClass == null )
		{
			LoggerProvider.getLogger().logError( "Plug-in class <" + classInfo.getClassName() + "> not found!" );
			return false ;
		}
		return true ;
	} // validateClassFound() 

	// -------------------------------------------------------------------------

	protected boolean validateInstanceCreation( ClassInfo classInfo )
	{
		try
		{
			classInfo.newInstance() ;
			return true ;
		}
		catch (Throwable ex)
		{
			LoggerProvider.getLogger().logError( "Plug-in class <" + classInfo.getClassName() + "> cannot be instantiated.", ex ) ;
			return false ;
		}
	} // validateInstanceCreation() 
	
	// -------------------------------------------------------------------------

	protected Method find_getPluginId_Method( Object object ) 
	{
		return ReflectUtil.current().findMethod( object.getClass(), "getPluginId", null, Modifier.PUBLIC );
	} // find_getPluginId_Method() 
	
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

	protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	//-------------------------------------------------------------------------
	
	protected CollectionUtil coll() 
	{
		return CollectionUtil.current();
	} // coll() 
	
	// -------------------------------------------------------------------------
	
	protected void debug( String text )
	{
		LoggerProvider.getLogger().logDebug( DEBUG_PREFIX + text ) ;	
	} // debug() 

	// -------------------------------------------------------------------------
		
} // class PluginRegistry 
