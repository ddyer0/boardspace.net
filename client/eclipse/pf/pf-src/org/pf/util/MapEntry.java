// ===========================================================================
// CONTENT  : CLASS MapEntry
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 27/03/2010
// HISTORY  :
//  04/09/2002  duma  CREATED
//	27/03/2010	mdu		changed --> removed all methods that are inherited from superclass
//
// Copyright (c) 2002-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This is a simple implementation of the java.util.Map.Entry interface.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class MapEntry<K,V> extends Association<K,V>
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MapEntry( K aKey, V aValue )
  {
    super(aKey, aValue) ;
  } // MapEntry()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

} // class MapEntry