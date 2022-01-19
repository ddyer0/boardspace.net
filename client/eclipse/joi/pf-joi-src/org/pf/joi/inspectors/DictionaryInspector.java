// ===========================================================================
// CONTENT  : CLASS DictionaryInspector
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 22/07/2007
// HISTORY  :
//  22/07/2007  mdu  CREATED
//
// Copyright (c) 2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.joi.AbstractObjectSpy;

/**
 * Inspector for java.util.Dictionary subclasses.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class DictionaryInspector extends KeyValueInspector
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
  public DictionaryInspector()
  {
    super() ;
  } // DictionaryInspector()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Returns the correct wrapper class (spy) for the given object.   <br>
	 * Here it is always a DictionarySpy.
	 *
	 * @param obj The object to inspect
	 */
	protected AbstractObjectSpy objectSpyFor( Object obj ) 
	{
		return ( new DictionarySpy( obj ) ) ;
	} // objectSpyFor() 

  // -------------------------------------------------------------------------

	protected String getInspectorId()
	{
		return "DictionaryInspector" ;
	} // getInspectorId() 

	// -------------------------------------------------------------------------
   
} // class DictionaryInspector
