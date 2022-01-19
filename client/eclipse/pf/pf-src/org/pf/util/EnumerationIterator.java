// ===========================================================================
// CONTENT  : CLASS EnumerationIterator
// AUTHOR   : M.Duchrow
// VERSION  : 1.1 - 15/01/2012
// HISTORY  :
//  04/04/2008  mdu  CREATED
//	15/01/2012	mdu		changed to generic type
//
// Copyright (c) 2008-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Enumeration;
import java.util.Iterator ;

/**
 * Provides the Iterator interface on a given Enumeration.
 *
 * @author M.Duchrow
 * @version 1.1
 */
public class EnumerationIterator<E> implements Iterator<E>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	protected Enumeration<E> enumeration ;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with an enumeration.
   */
  public EnumerationIterator(Enumeration<E> enumeration)
  {
    super() ;
    this.enumeration = enumeration ;
  } // EnumerationIterator() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true if another element is available.
   */
  public boolean hasNext()
  {
  	return enumeration.hasMoreElements();
  } // hasNext()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the next element.
   */
  public E next()
  {
  	return enumeration.nextElement() ;
  } // next()
  
  // -------------------------------------------------------------------------
  
  /**
   * Not supported but required by interface Iterator.
   * Empty method.
   */
  public void remove()
  {
  	// not applicable on Enumeration
  } // remove()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  
} // class EnumerationIterator 
