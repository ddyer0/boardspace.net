// ===========================================================================
// CONTENT  : CLASS EmptySet
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
import java.util.Collection;
import java.util.HashSet;

/**
 * An empty set that never can be changed. That is, no elements can be added.
 * It's only purpose is to be used as constant in ColletcionUtil.EMPTY_SET.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
final class EmptySet extends HashSet
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
 	EmptySet()
  {
    super(1) ;
  } // EmptySet() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	public boolean add( Object o )
	{
		// Do never add anything
		return false ;
	} // add()

	// -------------------------------------------------------------------------
	
	public Object clone()
	{
		return this ;
	} // clone()
	
	// -------------------------------------------------------------------------
	
	public boolean addAll( Collection c )
	{
		// Do never add anything
		return false ;
	} // addAll()
	
	// -------------------------------------------------------------------------
	
} // class EmptySet 
