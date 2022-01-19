// ===========================================================================
// CONTENT  : CLASS ReverseComparator
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 22/12/2005
// HISTORY  :
//  22/12/2005  mdu  CREATED
//
// Copyright (c) 2005, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Comparator ;

/**
 * Provides a comparator implementation that is a wrapper around another 
 * comparator with the purpose to reverse the compare order of the given objects.
 * That allows to easily reverse the sort order of any object collection for
 * which a comparator is available.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class ReverseComparator implements Comparator
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Comparator comparator = null ;
  protected Comparator getComparator() { return comparator ; }
  protected void setComparator( Comparator newValue ) { comparator = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with another comparator
   */
  public ReverseComparator( Comparator aComparator )
  {
    super() ;
    this.setComparator( aComparator ) ;
  } // ReverseComparator() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Compares its two arguments for order. Returns a negative integer, zero, 
   * or a positive integer as the first argument is greater than, equal to, 
   * or less than the second.
   */
  public int compare(Object o1, Object o2) 
	{
		return this.getComparator().compare( o2, o1 ) ;
	} // compare() 

	// -------------------------------------------------------------------------

} // class ReverseComparator
