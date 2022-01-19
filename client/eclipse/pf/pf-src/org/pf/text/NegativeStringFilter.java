// ===========================================================================
// CONTENT  : CLASS NegativeStringFilter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 28/08/2012
// HISTORY  :
//  28/08/2012  mdu  CREATED
//
// Copyright (c) 2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * A simple filter that negates the result of its inner string filter.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class NegativeStringFilter extends AStringFilter
{
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private StringFilter innerFilter = null ;
  protected StringFilter getInnerFilter() { return innerFilter ; }
  protected void setInnerFilter( StringFilter newValue ) { innerFilter = newValue ; }

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with another filter which it negates.
   * 
   * @param filter The filter to negate (must not be null).
   * @throws IllegalArgumentException if the given filter is null.
   */
  public NegativeStringFilter(StringFilter filter)
  {
    super() ;
    if (filter == null)
		{
			throw new IllegalArgumentException("filter must not be null!");
		}
    this.setInnerFilter(filter);
  } // NegativeStringFilter() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  @Override
  public boolean matches(String string)
  {
  	boolean result;

  	result = this.getInnerFilter().matches(string);
  	return !result;
  } // matches()
  
  // -------------------------------------------------------------------------
  
} // class NegativeStringFilter 
