// ===========================================================================
// CONTENT  : CLASS DynamicProxyInspector
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 07/12/2008
// HISTORY  :
//  07/12/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.joi.AbstractObjectSpy;
import org.pf.joi.BasicInspector;

/**
 * Is used to show the methods that are suported by a dynamic proxy
 * (i.e. java.lang.reflect.Proxy)
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class DynamicProxyInspector extends BasicInspector
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
  public DynamicProxyInspector()
  {
    super() ;
  } // DynamicProxyInspector()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the correct wrapper class (spy) for the given object.   <br>
	 * Here it is always a DynamicProxySpy.
	 *
	 * @param obj The object to inspect
	 */
	protected AbstractObjectSpy objectSpyFor( Object obj ) 
	{
		return new DynamicProxySpy( obj ) ;
	} // objectSpyFor() 

  // -------------------------------------------------------------------------

	protected String getInspectorId()
	{
		return "DynamicProxyInspector" ;
	} // getInspectorId() 

	// -------------------------------------------------------------------------
   
} // class DynamicProxyInspector
