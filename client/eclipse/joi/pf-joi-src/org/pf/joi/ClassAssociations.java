// ===========================================================================
// CONTENT  : CLASS ClassAssociations
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 01/09/2007
// HISTORY  :
//  22/09/2002  duma  CREATED
//	23/02/2003	duma	changed	-> Renamed and now subclass of org.pf.plugin.ClassRegistry
//	01/09/2007	mdu		added		-> canBeUsed() 
//
// Copyright (c) 2002-2007, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.pf.plugin.ClassRegistry;
import org.pf.reflect.ClassInfo;

/**
 * Provides a mapping of class names to other objects, where the search
 * for a class entry will be done recursivly through all superclasses
 * and interfaces.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
class ClassAssociations<T> extends ClassRegistry<T>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  ClassAssociations()
  {
    super() ;
  } // ClassAssociations() 

	// -------------------------------------------------------------------------
	
  /**
   * Initialize the new instance with a type all classes that are registered
   * must comply to. That is, they must be the identical class, a subclass of 
   * this type or an implementer of this type if it is an interface.
   * 
   * @param typeOfRegisteredClasses The type to which the classes that can be 
   * 				registered must be compatible to
   */
  public ClassAssociations( Class<T> typeOfRegisteredClasses )
  {
    super( typeOfRegisteredClasses ) ;
  } // ClassAssociations() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	public String[] keys()
	{
		Set keySet ;
		
		keySet = this.registry().keySet() ;
		return (String[])keySet.toArray( new String[keySet.size()] ) ;
	} // keys() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected ClassInfo findForClassOfObject( Class aClass, Object object )
	{
		ClassInfo result = null ;
		Class[] interfaces ;
		int index	= 0 ;

		if ( aClass != null )
		{
			result = this.getClassInfo( aClass ) ;
			if ( ( result != null ) && ( !this.canBeUsed( result, object ) ) )
			{
					result = null ;
			}
			if ( result == null )
			{
				interfaces = aClass.getInterfaces() ;
				while ( ( result == null ) && ( index < interfaces.length ) )
				{
					result = this.findForClassOfObject( interfaces[index], object ) ;
					index++ ;
				}

				if ( ( result != null ) && ( !this.canBeUsed( result, object ) ) )
				{
						result = null ;
				}
				if ( ( result == null ) && ( ! aClass.isInterface() ) )
				{
					result = findForClassOfObject( this.getSuperclassOf( aClass ), object ) ;
					if ( ( result != null ) && ( !this.canBeUsed( result, object ) ) )
					{
							result = null ;
					}
				}
			}
		}
		return result ;
	} // findForClassOfObject() 

  // -------------------------------------------------------------------------

  /**
   * Returns the superclass of the given class.
   * If aClass is an array the array class of the superclass of the elements
   * will be returned.
   */
  protected Class getSuperclassOf( Class aClass ) 
	{
  	Class superclass ;
  	Object array ;
  	
  	if ( aClass.isArray() )
		{
			superclass = aClass.getComponentType().getSuperclass() ;
			if ( ( superclass == null ) || ( superclass == Object.class ) )
			{
				return Object.class ;
			}
			array = Array.newInstance( superclass, 0 ) ;
			return array.getClass() ;
		}
		return aClass.getSuperclass() ;
	} // getSuperclassOf()
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns true if the found classInfo can be used for the given object.
   */
  protected boolean canBeUsed( ClassInfo classInfo, Object object ) 
	{
  	Object pluginObject ;
  	
		try
		{
			pluginObject = classInfo.createInstance() ;
			return ( pluginObject != null ) ;
		}
		catch ( Exception e )
		{
			return false ;
		}
	} // canBeUsed() 
	
	// -------------------------------------------------------------------------
  
	protected ClassInfo getClassInfo( Class aClass )
	{
		return this.get( this.nameOfClass( aClass ) ) ;
	} // getClassInfo() 

	// -------------------------------------------------------------------------

	protected String nameOfClass( Class aClass )
	{
		if ( aClass.isArray() )
		{
			return this.nameOfClass( aClass.getComponentType() ) + "[]" ;
		}
		else
		{
			return aClass.getName() ;
		}
	} // nameOfClass() 

	// -------------------------------------------------------------------------

	protected Map getMapping()
	{
		Map mapping ;
		String[] keys ;
		
		keys = this.keys() ;
		mapping = new HashMap( keys.length ) ;
		for (int i = 0; i < keys.length; i++)
		{
			mapping.put( keys[i], this.getClassName(keys[i]) ) ;
		}
		return mapping ;
	} // getMapping() 

	// -------------------------------------------------------------------------	

} // class ClassAssociations 
