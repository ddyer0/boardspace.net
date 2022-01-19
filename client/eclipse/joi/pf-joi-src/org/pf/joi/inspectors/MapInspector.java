// ===========================================================================
// CONTENT  : CLASS MapInspector
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 22/07/2007
// HISTORY  :
//  11/01/2000  duma  CREATED
//	13/03/2004	duma	changed	-->	Support tabbed inspection
//	22/07/2007	mdu		changed	-->	Moved most code up to KeyValueInspector
//
// Copyright (c) 2000-2007, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.joi.AbstractObjectSpy;

/**
 * This is a specialized Inspector for objects that implement the Map interface.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class MapInspector extends KeyValueInspector
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
  public MapInspector()
  {
  } // MapInspector() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns the correct wrapper class (spy) for the given object.   <br>
	 * Here it is always a MapSpy.
	 *
	 * @param obj The object to inspect
	 */
	protected AbstractObjectSpy objectSpyFor( Object obj ) 
	{
		return ( new MapSpy( obj ) ) ;
	} // objectSpyFor() 

  // -------------------------------------------------------------------------

	protected String getInspectorId()
	{
		return "MapInspector" ;
	} // getInspectorId() 

	// -------------------------------------------------------------------------
   
} // class MapInspector 
