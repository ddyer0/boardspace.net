// ===========================================================================
// CONTENT  : CLASS MethodSpy
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

import org.pf.joi.AbstractObjectSpy;
import org.pf.joi.ElementSpy;

/**
 * Represents a method rather than a field as element in the inspector.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class MethodSpy extends ElementSpy
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private Method method = null;
	protected Method getMethod()
	{
		return method;
	} // getMethod()
	protected void setMethod( Method newValue )
	{
		method = newValue;
	} // setMethod()

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public MethodSpy( AbstractObjectSpy object, Method method )
	{
		super( object );
		this.setMethod( method );
	} // MethodSpy()

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	public Class getType()
	{
		return this.getMethod().getReturnType();
	} // getType()

	// --------------------------------------------------------------------------

	public String getName()
	{
		return this.getMethod().getName() + "()" ;
	} // getName()

	// --------------------------------------------------------------------------

	public int getModifiers()
	{
		return this.getMethod().getModifiers();
	} // getModifiers()

	// --------------------------------------------------------------------------

	public Object getValue() throws Exception
	{
		return this.getMethod().invoke( this.getContainer().getObject(), (Object[])null ) ;
	} // getValue()

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	/**
	 * Currently allows to edit String values and primitive types only
	 */
	public boolean isEditable()
	{
		return false;
	} // isEditable()

	// -------------------------------------------------------------------------

} // class MethodSpy
