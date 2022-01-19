// ===========================================================================
// CONTENT  : CLASS EmptyMap
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
import java.util.HashMap;
import java.util.Map;

/**
 * Implements an immutable empty Map. That means, no elements can be added 
 * to this map.
 * See Collection.EMPTY_MAP
 *  
 * @author Manfred Duchrow
 * @version 1.0
 */
final class EmptyMap extends HashMap
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  EmptyMap()
  {
    super() ;
  } // EmptyMap() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	public Object clone()
	{
		return this ;
	} // clone()
	
	// -------------------------------------------------------------------------
	
	public Object put( Object key, Object value )
	{
		// Never add anything
		return null ;
	} // put()

	// -------------------------------------------------------------------------
	
	public void putAll( Map m )
	{
		// Never add anything
	} // putAll()
	
	// -------------------------------------------------------------------------
	
} // class EmptyMap 
