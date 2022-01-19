// ===========================================================================
// CONTENT  : CLASS EmptyList
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 20/06/2004
// HISTORY  :
//  20/06/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;

/**
 * Implements an immutable empty List. That means, no elements can be added 
 * to this list.
 * See Collection.EMPTY_LIST 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
final class EmptyList extends ArrayList
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  EmptyList()
  {
    super(1) ;
  } // EmptyList() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	public void add( int index, Object element )
	{
		// Do never add anything
	} // add()
	
	// -------------------------------------------------------------------------
	
	public boolean add( Object o )
	{
		// Do never add anything
		return false ;
	} // add()
	
	// -------------------------------------------------------------------------
	
	public boolean addAll( Collection c )
	{
		// Do never add anything
		return false ;
	} // addAll()
	
	// -------------------------------------------------------------------------
	
	public boolean addAll( int index, Collection c )
	{
		// Do never add anything
		return false ;
	} // addAll()
	
	// -------------------------------------------------------------------------
	
	public Object clone()
	{
		return this ;
	} // clone()
	
	// -------------------------------------------------------------------------
	
} // class EmptyList 
