// ===========================================================================
// CONTENT  : INTERFACE AttributeReadWriteAccess
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 21/05/2004
// HISTORY  :
//  21/05/2004  mdu  CREATED
//
// Copyright (c) 2004, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.reflect;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Specifies the methods an object must implement to provide generic write 
 * access to its attributes.<br>
 * Such attributes can be instance variables of an object or values stored
 * in properties or maps. 
 *
 * @author M.Duchrow
 * @version 1.0
 */
public interface AttributeReadWriteAccess extends AttributeReadAccess
{
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Sets the current value of the attribute with the given name.    <br>
	 *
	 * @param name The attribute's name ( case sensitive )
	 * @param value The value to be put into the attributes 'slot'
	 * @throws NoSuchFieldException If there is no attribute with the given name
	 */
	public void setAttributeValue( String name, Object value )
		throws NoSuchFieldException ;

	// -------------------------------------------------------------------------
	
} // interface AttributeReadWriteAccess 
