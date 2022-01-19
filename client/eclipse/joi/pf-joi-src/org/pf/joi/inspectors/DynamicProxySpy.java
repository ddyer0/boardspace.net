// ===========================================================================
// CONTENT  : CLASS DynamicProxySpy
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 07/12/2008
// HISTORY  :
//  07/12/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.pf.joi.AbstractObjectSpy;
import org.pf.joi.ElementSpy;

/**
 * Provides all methods with no arguments that are supported by a dynamic proxy. 
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class DynamicProxySpy extends AbstractObjectSpy
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
	public DynamicProxySpy( Object obj )
	{
		super( obj );
	} // DynamicProxySpy()

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected void addAllElements() throws SecurityException
	{
		ElementSpy elementSpy;
		Method[] methods ;

		methods = this.collectMethods() ;
		for (int i = 0; i < methods.length; i++ )
		{
			elementSpy = new MethodSpy( this, methods[i] );
			this.getElementHolders().add( elementSpy );
		}
	} // addAllElements()

	// --------------------------------------------------------------------------

	protected Method[] collectMethods() 
	{
		Class[] interfaces ;
		Method[] methods ;
		List result ;
		
		result = new ArrayList(50) ;
		interfaces = this.getObject().getClass().getInterfaces() ;
		for (int i = 0; i < interfaces.length; i++ )
		{
			methods = interfaces[i].getMethods() ;
			for (int j = 0; j < methods.length; j++ )
			{
				if ( methods[j].getParameterTypes().length == 0 )
				{
					result.add( methods[j] ) ;					
				}
			}
		}
		return (Method[]) result.toArray(new Method[0]) ;
	} // collectMethods()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns whether or not the elements
	 * of the underlying object can be sorted.
	 */
	protected boolean canBeSorted()
	{
		return true;
	} // canBeSorted()

	// --------------------------------------------------------------------------

} // class DynamicProxySpy
