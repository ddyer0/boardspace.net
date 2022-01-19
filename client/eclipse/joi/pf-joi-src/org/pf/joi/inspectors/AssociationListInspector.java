// ===========================================================================
// CONTENT  : CLASS AssociationListInspector
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 24/04/2004
// HISTORY  :
//  24/04/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.joi.AbstractObjectSpy;
import org.pf.joi.BasicInspector;

/**
 * The specialized inspector for AssociationList objects
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class AssociationListInspector extends BasicInspector
{
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
  public AssociationListInspector()
  {
    super() ;
  } // AssociationListInspector()
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the correct wrapper class (spy) for the given object.   <br>
	 * Here it is always a AssociationListSpy.
	 *
	 * @param obj The object to inspect
	 */
	protected AbstractObjectSpy objectSpyFor( Object obj ) 
	{
		return ( new AssociationListSpy( obj ) ) ;
	} // objectSpyFor() 

  // -------------------------------------------------------------------------

	protected String getInspectorId()
	{
		return "AssociationListInspector" ;
	} // getInspectorId() 

	// -------------------------------------------------------------------------
   	
} // class AssociationListInspector
