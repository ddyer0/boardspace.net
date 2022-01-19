// ===========================================================================
// CONTENT  : ABSTRACT CLASS AStringFilter
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 24/02/2006
// HISTORY  :
//  24/02/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

import org.pf.bif.filter.IObjectFilter;

/**
 * Subclasses of this abstract string filter class usually must override
 * only method matches(String).
 * This abstract class ensures that filters can be used with any mechanism
 * based on either org.pf.text.ObjectFilter or org.pf.bif.IObjectFilter.
 *
 * @author M.Duchrow
 * @version 1.0
 */
abstract public class AStringFilter implements StringFilter, IObjectFilter<String>
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
  public AStringFilter()
  {
    super() ;
  } // AStringFilter() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns false if the given object is no string, otherwise it calls
   * method matches(String).
   * 
   * @param object The object to be checked against this string filter
   * @see #matches(String)
   */
	public boolean matches(String object)
	{
		if (object instanceof String)
		{
			return this.matches((String)object);
		}
		return false;
	} // matches()

  // -------------------------------------------------------------------------
  
} // class AStringFilter 
