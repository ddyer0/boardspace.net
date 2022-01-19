// ===========================================================================
// CONTENT  : INTERFACE VariableResolver
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 01/07/2002
// HISTORY  :
//  24/06/1999 	duma  CREATED
//	25/01/2000	duma	moved		-> from package 'com.mdcs.text'
//	01/07/2002	duma	changed	-> added knownVariableName() and renamed getValueFor()
//
// Copyright (c) 1999-2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

import java.util.Set;

/**
 * This interface defines the method an object must support,
 * if it wants to provide values associated to specific variable names.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public interface VariableResolver
{
  /**
   * Returns the value for the variable with the given name.
   *
   * @param varName The case sensitive name of the variable.
   * @return The value associated to the given variable (even null is a valid value)
   * @throws UnknownVariableException The receiver is not knowing the variable. 
   */
  public Object getValue( String varName )
                throws UnknownVariableException ;
  
  // -------------------------------------------------------------------------

  /**
   * Returns if the variable with the given name can be resolved by the receiver.
   *
   * @param varName The case sensitive name of the variable.
   * @return Whether the variable with the given name is known or not.
   */
  public boolean isKnownVariable( String varName ) ;
  
  // -------------------------------------------------------------------------

	/**
	 * Returns all variable name the resolver currently knows
	 */
	public Set knownVariableNames() ;

  // -------------------------------------------------------------------------

} // interface VariableResolver