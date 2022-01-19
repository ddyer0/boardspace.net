// ===========================================================================
// CONTENT  : INTERFACE VariableContainer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 11/07/2002
// HISTORY  :
//  01/07/2002  duma  CREATED
//	11/07/2002	duma	added		->	setValueFor()
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Extends the VariableResolver interface with write capability methods. 
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public interface VariableContainer extends VariableResolver
{ 
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Set the value of the specified variable.
	 * 
	 * @param varName The name under which the value has to be stored
	 * @param value The value to store
	 */
	public void setValue( String varName, String value ) ;

  // -------------------------------------------------------------------------
  
  /**
   * Sets the value of the variable with the given name.
   *
   * @param varName The case sensitive name of the variable. Must not be null !
   * @param value The new value of the variable. Must not be null !
   */
  public void setValueFor( String varName, Object value ) ;

  // -------------------------------------------------------------------------
  
	/**
	 * Remove the variable with the specified name.
	 * 
	 * @param varName The name of the variable to be removed
	 */
	public void removeVariable( String varName ) ;

  // -------------------------------------------------------------------------
  
} // interface VariableContainer