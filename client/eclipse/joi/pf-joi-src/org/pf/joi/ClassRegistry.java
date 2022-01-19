// ===========================================================================
// CONTENT  : CLASS ClassRegistry
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 22/09/2002
// HISTORY  :
//  22/09/2002  duma  CREATED
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Hashtable;
import java.util.Map;

/**
 * Provides a mapping of class names to other objects, where the search
 * for a class entry will be done recursivly through all superclasses
 * and interfaces.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
class ClassRegistry
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Map registry = null ;
  protected Map registry() { return registry ; }
  protected void registry( Map newValue ) { registry = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected ClassRegistry()
  {
    super() ;
    this.registry( new Hashtable() ) ;
  } // ClassRegistry()

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected Object findForClassNamed( String className )
	{
		Class aClass ;
		
		try
		{
			aClass = this.getClass().forName( className ) ;
			return this.findForClass( aClass ) ;
		}
		catch (ClassNotFoundException e)
		{
			return null ;
		}
	} // findForClassNamed()

	// -------------------------------------------------------------------------

  protected Object findForClass( Class aClass )
	{
		Object result					= null ;
		Class[] interfaces		= null ;
		int index							= 0 ;

		if ( aClass != null )
		{
			result = this.getRegistered( aClass ) ;
			if ( result == null )
			{
				interfaces = aClass.getInterfaces() ;
				while ( ( result == null ) && ( index < interfaces.length ) )
				{
					result = this.findForClass( interfaces[index] ) ;
					index++ ;
				}

				if ( ( result == null ) && ( ! aClass.isInterface() ) )
				{
					result = findForClass( aClass.getSuperclass() ) ;
				}
			}
		}
		return result ;
	} // findForClass()

  // -------------------------------------------------------------------------

	protected Object getRegistered( String className )
	{
		return this.registry().get( className ) ;
	} // getRegistered()

	// -------------------------------------------------------------------------

	protected Object getRegistered( Class aClass )
	{
		return this.getRegistered( aClass.getName() ) ;
	} // getRegistered()

	// -------------------------------------------------------------------------

	protected void register( String className, Object obj )
	{
		this.registry().put( className, obj ) ;
	} // register()

	// -------------------------------------------------------------------------

	protected void register( Class aClass, Object obj )
	{
		this.register( aClass.getName(), obj ) ;
	} // register()

  // -------------------------------------------------------------------------

} // class ClassRegistry