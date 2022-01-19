// ===========================================================================
// CONTENT  : CLASS CollectionEnumeration
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 27/03/2010
// HISTORY  :
//  27/03/2010  mdu  CREATED
//
// Copyright (c) 2010, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * An enumeration object that can be used to iterate over all elements 
 * of an underlying collection.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class CollectionEnumeration<E> implements Enumeration<E>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private Iterator<E> iterator;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with an iterator.
   * @param iter An iteration (must not be null)
   */
  public CollectionEnumeration( Iterator<E> iter )
  {
    super() ;
    if ( iter == null )
		{
			throw new IllegalArgumentException("The given iterator must not be null");
		}
    this.iterator = iter;
  } // CollectionEnumeration() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a collection.
   * @param coll A collection (must not be null)
   */
  public CollectionEnumeration( Collection<E> coll )
  {
  	this( coll.iterator() ) ;
  } // CollectionEnumeration() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  public boolean hasMoreElements()
  {
  	return this.iterator.hasNext();
  } // hasMoreElements()
  
  // -------------------------------------------------------------------------
  
  public E nextElement()
  {
  	return this.iterator.next();
  } // nextElement()
  
  // -------------------------------------------------------------------------
  
} // class CollectionEnumeration 
