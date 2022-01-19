// ===========================================================================
// CONTENT  : INTERFACE FunctionResolver
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 26/06/1999
// HISTORY  :
//  26/06/1999	duma  CREATED
//	25/01/2000	duma	moved		-> from package 'com.mdcs.text'
//
// Copyright (c) 1999-2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Vector ;

/**
 * This interface defines the method an object must support,
 * if it wants to provide values for specific function calls.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface FunctionResolver
{
  /**
   * Returns the value for the function with the given name.
   *
   * @param functionName The case sensitive name of the function.
   * @param parameter A collection of parameters for the function.
   * @return The evaluation of the function with the given parameters (null is a valid return value)
   * @throws UnknownFunctionException The receiver is not knowing the function.
   */
  public Object executeFunction( String functionName, Vector parameter )
                throws UnknownFunctionException, InvalidParameterException ;

  /**
   * Returns if the function with the given name can be resolved by the receiver.
   *
   * @param functionName The case sensitive name of the function.
   * @return Whether the function with the given name is known or not.
   */
  public boolean isKnownFunction( String functionName ) ;

} // interface FunctionResolver