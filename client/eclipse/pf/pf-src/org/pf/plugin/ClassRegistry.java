// ===========================================================================
// CONTENT  : CLASS ClassRegistry
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 17/11/2010
// HISTORY  :
//  25/01/2003  mdu CREATED
//	28/02/2003	mdu	added		->	keys(), values(), classes(), classNames(), getClassInfo()
//	17/11/2010	mdu	changed	->	to generic type
//
// Copyright (c) 2003-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.plugin ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pf.reflect.ClassInfo;

/**
 * A class registry is a container that keeps classes registered under a
 * logical key. For a query by key it can return 
 * <ul>
 * <li>the full qualified class name
 * <li>the Class object
 * <li>an instance of the class
 * </ul>
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class ClassRegistry<T>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Class<T> entryType = null ;
  protected Class<T> getEntryType() { return entryType ; }
  protected void setEntryType( Class<T> newValue ) { entryType = newValue ; }

  private Map<Object,ClassInfo<T>> registry = null ;
  protected Map<Object,ClassInfo<T>> registry() { return registry ; }
  protected void registry( Map<Object,ClassInfo<T>> newValue ) { registry = newValue ; }
    
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ClassRegistry()
  {
    super() ;
    this.registry( new HashMap<Object,ClassInfo<T>>() ) ;
  } // ClassRegistry()

	// -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a type all classes that are registered
   * must comply to. That is, they must be the identical class, a subclass of 
   * this type or an implementer of this type if it is an interface.
   * 
   * @param typeOfRegisteredClasses The type to which the classes that can be 
   * 				registered must be compatible to
   */
  public ClassRegistry( Class<T> typeOfRegisteredClasses )
  {
    this() ;
    this.setEntryType( typeOfRegisteredClasses ) ;
  } // ClassRegistry()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the name of the class which is registered under the given key.
   * If nothing is registered under the key this method returns null.
   * 
   * @param key The key under which the class is registered
   */
	public String getClassName( Object key )
	{
		ClassInfo classInfo ;
		
		classInfo = this.get( key ) ;
		return ( classInfo == null ) ? null : classInfo.getClassName() ;
	} // getClassName()

	// -------------------------------------------------------------------------

  /**
   * Returns the class which is registered under the given key.
   * If nothing is registered under the key this method returns null.
   * 
   * @param key The key under which the class is registered
   */
	public Class<T> getClass( Object key )
	{
		ClassInfo<T> classInfo ;
		
		classInfo = this.get( key ) ;
		return ( classInfo == null ) ? null : classInfo.getClassObject() ;
	} // getClass()

	// -------------------------------------------------------------------------
  
  /**
   * Returns a new instance of the class which is registered under the 
   * given key.
   * If nothing is registered under the key this method returns null.
   * 
   * @param key The key under which the class is registered
   */
	public T newInstance( Object key )
	{
		ClassInfo<T> classInfo ;
		
		classInfo = this.get( key ) ;
		return ( classInfo == null ) ? null : classInfo.createInstance() ;
	} // newInstance()

	// -------------------------------------------------------------------------
  
  /**
   * Registers the class with the given name under the specified key.
   * 
   * @param key The key under which the class is registered
   * @param className The fully qualified name of the class
   * @return true, if the class was found, successfully validated and registered
   */
	public boolean register( Object key, String className )
	{
		try
		{
			this.put( key, className ) ;
			return true ;
		}
		catch ( ClassRegistryException ex ) 
		{
			return false ;
		} 
	} // register()

	// -------------------------------------------------------------------------

  /**
   * Registers the class with the given name under the specified key.
   * 
   * @param key The key under which the class is registered
   * @param className The fully qualified name of the class
   * @throws ClassRegistryException If the class can't be found or if it is of 
   * 					the wrong type or can't be instantiated
   */
	public void put( Object key, String className )
		throws ClassRegistryException
	{
		this.assertArgNotNull( "put", "key", key ) ;
		this.assertArgNotNull( "put", "className", className ) ;
					
		ClassInfo<T> classInfo ;			
					
		classInfo = new ClassInfo<T>( className ) ;
		this.put( key, classInfo ) ;
	} // put()

	// -------------------------------------------------------------------------

  /**
   * Registers the class under the specified key.
   * 
   * @param key The key under which the class is registered
   * @param aClass The class to be registered
   * @return true, if the class was successfully validated and registered
   */
	public boolean register( Object key, Class<T> aClass )
	{
		try
		{
			this.put( key, aClass ) ;
			return true ;
		}
		catch ( ClassRegistryException ex ) 
		{
			return false ;
		} 
	} // register()

	// -------------------------------------------------------------------------

  /**
   * Registers the given class under the specified key.
   * 
   * @param key The key under which the class is registered
   * @param aClass The class to be registered
   * @throws ClassRegistryException If the class is of the wrong type or 
   * 					can't be instantiated
   */
	public void put( Object key, Class<T> aClass )
		throws ClassRegistryException
	{
		this.assertArgNotNull( "put", "key", key ) ;
		this.assertArgNotNull( "put", "aClass", aClass ) ;
					
		ClassInfo<T> classInfo ;			
					
		classInfo = new ClassInfo<T>( aClass ) ;
		this.put( key, classInfo ) ;
	} // put()

	// -------------------------------------------------------------------------

	/**
	 * Removes the entry in the registry which is defined by the given key.
	 * @return true, if anything has been found and removed, otherwise false.
	 */
	public boolean remove( Object key )
	{
		Object obj ;
		
		if ( key != null )
		{
			obj = this.registry().remove( key ) ;	
			return obj != null ;
		}
		return false ;
	} // remove()

	// -------------------------------------------------------------------------

	/**
	 * Returns all keys known in this registry.
	 */
	public String[] keys()
	{
		Set keys ;
		
		keys = this.registry().keySet() ;
		return (String[]) keys.toArray(new String[keys.size()]);
	} // keys()

	// -------------------------------------------------------------------------

	/**
	 * Returns all class info objects that are currently in this registry.
	 */
	public ClassInfo[] values()
	{
		Collection values ;
		
		values = this.registry().values() ;
		return (ClassInfo[]) values.toArray(new ClassInfo[values.size()]);
	} // values()

	// -------------------------------------------------------------------------

	/**
	 * Returns all class objects that are currently in this registry.
	 */
	public Class[] classes()
	{
		List classes ;
		
		classes = this.collect( false ) ;
		return (Class[])classes.toArray(new Class[classes.size()]);
	} // classes()

	// -------------------------------------------------------------------------

	/**
	 * Returns all class names of the classes that are currently in this registry.
	 */
	public String[] classNames()
	{
		List names ;
		
		names = this.collect( true ) ;
		return (String[])names.toArray(new String[names.size()]);
	} // classNames()

	// -------------------------------------------------------------------------

	/**
	 * Returns the class information object stored under the given key or null
	 * if nothing is found.
	 */
	public ClassInfo<T> getClassInfo( String key )
	{
		return this.get( key ) ;
	} // getClassInfo()

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected ClassInfo<T> get( Object key )
	{
		if ( key == null )
		{
			return null ;
		}
			
		return this.registry().get( key ) ;
	} // get()

	// -------------------------------------------------------------------------
  
	protected void put( Object key, ClassInfo<T> classInfo )
		throws ClassRegistryException
	{
		this.assertArgNotNull( "put", "key", key ) ;					
		this.validate( classInfo ) ;
		this.registry().put( key, classInfo ) ;
	} // put()

	// -------------------------------------------------------------------------
	
	protected void validate( ClassInfo<T> classInfo )
		throws ClassRegistryException
	{	
		this.assertClassFound( classInfo );
		this.assertValidType( classInfo );		
		this.assertInstanceCreation( classInfo ) ;
	} // validate()

	// -------------------------------------------------------------------------
	
	protected void assertClassFound( ClassInfo<T> classInfo ) 
		throws ClassRegistryException
	{
		Class aClass ;
		
		aClass = classInfo.getClassObject() ;
		if ( aClass == null )
		{
			throw new ClassRegistryException( "Class " + classInfo.getClassName() 
																				+ " not found" ) ;
		}
	} // assertClassFound()

	// -------------------------------------------------------------------------

	protected void assertValidType( ClassInfo classInfo )
		throws ClassRegistryException
	{
		if ( this.isTyped() )
		{
			if ( ! this.getEntryType().isAssignableFrom( classInfo.getClassObject() ) )
			{
				throw new ClassRegistryException( "Class " + classInfo.getClassName() 
																				+ " is not compatible to type "
																				+ this.getEntryType().getName() ) ;
			}
		}
	} // assertValidType()

	// -------------------------------------------------------------------------

	protected void assertInstanceCreation( ClassInfo classInfo )
		throws ClassRegistryException
	{
		try
		{
			classInfo.newInstance() ;
		}
		catch (Throwable ex)
		{
			throw new ClassRegistryException( "Unable to create instances of class " 
						+ classInfo.getClassName() + " (" + ex.getMessage() + ")" ) ;
		}
	} // assertInstanceCreation()

	// -------------------------------------------------------------------------

	protected boolean isTyped()
	{
		return this.getEntryType() != null ;
	} // isTyped()

	// -------------------------------------------------------------------------

	protected void assertArgNotNull( String methodName, String argName, Object arg )
	{
		if ( arg == null )
		{
			throw new IllegalArgumentException( "Argument '" + argName 
			   + "' not valid in method '" + this.getClass().getName()
			   + "." + methodName + "'" ) ;		
		}
	} // assertArgNotNull()

	// -------------------------------------------------------------------------
	
	protected void illegalArgumentException( String methodName, String argName )
	{
		throw new IllegalArgumentException( "Argument '" + argName 
		   + "' not valid in method '" + this.getClass().getName()
		   + "." + methodName + "'" ) ;		
	} // illegalArgumentException()

	// -------------------------------------------------------------------------

	/**
	 * Returns all class objects or all class names that are currently 
	 * in this registry.
	 * 
	 * @param collectNames If true, the names are returned, otherwise the Class objects
	 */
	protected List collect( boolean collectNames )
	{
		Collection values ;
		List result ;
		ClassInfo classInfo ;
		
		values = this.registry().values() ;
		result = new ArrayList( values.size() ) ;
		for (Iterator iter = values.iterator(); iter.hasNext();)
		{
			classInfo = (ClassInfo)iter.next();
			if ( collectNames )
				result.add( classInfo.getClassName() ) ;		
			else
				result.add( classInfo.getClassObject() ) ;		
		}
		
		return result ;
	} // collect()

	// -------------------------------------------------------------------------
	
} // class ClassRegistry
