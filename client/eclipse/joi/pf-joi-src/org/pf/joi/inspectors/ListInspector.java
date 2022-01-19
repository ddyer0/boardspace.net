// ===========================================================================
// CONTENT  : CLASS ListInspector
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 11/01/2000
// HISTORY  :
//  11/01/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors;

import org.pf.joi.AbstractObjectSpy;
import org.pf.joi.BasicInspector;

// ===========================================================================
// CONTENT  : CLASS ListInspector
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 11/01/2000
// HISTORY  :
//  11/01/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This is a specialized Inspector for objects that implement the List interface.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ListInspector extends BasicInspector
{ 
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ListInspector()
  {
	super() ;
  } // ListInspector()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns the correct wrapper class (spy) for the given object.   <br>
	 * Here it is always a ListSpy.
	 *
	 * @param obj The object to inspect
	 */
	protected AbstractObjectSpy objectSpyFor( Object obj ) 
	{
		return ( new ListSpy( obj ) ) ;
	} // objectSpyFor()  

  // -------------------------------------------------------------------------

	protected String getInspectorId()
	{
		return "ListInspector" ;
	} // getInspectorId()

	// -------------------------------------------------------------------------
  
} // class ListInspector