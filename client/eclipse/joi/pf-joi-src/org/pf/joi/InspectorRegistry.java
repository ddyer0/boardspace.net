// ===========================================================================
// CONTENT  : CLASS InspectorRegistry
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 01/09/2007
// HISTORY  :
//  01/09/2007  mdu  CREATED
//
// Copyright (c) 2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi ;

import org.pf.reflect.ClassInfo;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Special registry for inspector mappings because of special checking if
 * an inspector can be used.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class InspectorRegistry extends ClassAssociations
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
  InspectorRegistry()
  {
    super( BasicInspector.class ) ;
  } // InspectorRegistry()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true if the found classInfo can be used for the given object.
   */
  protected boolean canBeUsed( ClassInfo classInfo, Object object ) 
	{
  	BasicInspector inspector ;
  	AbstractObjectSpy spy ;
  	
		try
		{
			inspector = (BasicInspector) classInfo.createInstance() ;
			spy = inspector.objectSpyFor( object ) ;
			return ( spy != null ) ;
		}
		catch ( Exception e )
		{
			return false ;
		}
	} // canBeUsed() 
	
	// -------------------------------------------------------------------------
  
} // class InspectorRegistry
