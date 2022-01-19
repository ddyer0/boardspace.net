// ===========================================================================
// CONTENT  : CLASS MapSpy
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 11/01/2000
// HISTORY  :
//  11/01/2000  duma  CREATED
//
// Copyright (c) 2000, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.inspectors;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.pf.joi.AbstractObjectSpy;

/**
 * An instance of this class is a wrapper for one inspected object that implements
 * the map interface.
 * It provides the API an inspector is using, to display internal information
 * about the inspected array.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */ 
public class MapSpy extends AbstractObjectSpy
{ 
	// --------------------------------------------------------------------------
 
  // =========================================================================
  // CONSTANTS
  // =========================================================================
   
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
			
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MapSpy( Object obj )
  	throws SecurityException
  {
  	super( obj ) ; 
  } // MapSpy()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	// --------------------------------------------------------------------------
	  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	
	/**
	 * Returns the wrapped map (is doing the type cast).
	 */
	protected Map getMap()
	{
		return ( (Map)this.getObject() ) ;
	} // getMap()

	// --------------------------------------------------------------------------

	protected void addAllElements()
		throws SecurityException 
	{
		AssociationSpy elementSpy ;
		Map.Entry entry ;
		Set elementSet ;
		Iterator iterator ;
					
		elementSet = this.getMap().entrySet() ;
		iterator = elementSet.iterator() ;
		while ( iterator.hasNext() )
		{
			entry = (Map.Entry)iterator.next() ;
			elementSpy = new AssociationSpy( this, entry ) ;
			this.getElementHolders().add( elementSpy ) ;
		}
	} // addAllElements()

	// --------------------------------------------------------------------------
 
} // class MapSpy