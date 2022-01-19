// ===========================================================================
// CONTENT  : CLASS StringInspector
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 26/06/2000
// HISTORY  :
//  26/06/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.joi.AbstractObjectSpy;
import org.pf.joi.BasicInspector;

/**
 * Special inspector that displays strings as an array of characters. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class StringInspector extends BasicInspector
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
  public StringInspector()
  {
  	super() ;
  } // StringInspector()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns the correct wrapper class (spy) for the given object.   <br>
	 * Here it is always a StringSpy.
	 *
	 * @param obj The object to inspect
	 */
	protected AbstractObjectSpy objectSpyFor( Object obj )
	{
		return ( new StringSpy( obj ) ) ;
	} // objectSpyFor()

  // -------------------------------------------------------------------------

	protected String getInspectorId()
	{
		return "StringInspector" ;
	} // getInspectorId()

	// -------------------------------------------------------------------------

} // class StringInspector