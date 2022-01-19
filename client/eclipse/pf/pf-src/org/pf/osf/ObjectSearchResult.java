// ===========================================================================
// CONTENT  : CLASS ObjectSearchResult
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 07/08/2004
// HISTORY  :
//  07/08/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.osf ;

import org.pf.reflect.AttributeReadAccess;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Apart from returning the found objects a search result can contain 
 * information whether or not the size limit has been reached. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class ObjectSearchResult extends ObjectContainer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	/** Represents the definition of an unlimited result container (value = 0) */
	public static final int UNLIMITED_SIZE	= 0 ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private int sizeLimit = UNLIMITED_SIZE ;
  /**
   * Returns the size limit that was set for the search that produced this 
   * result
   */
  public int getSizeLimit() { return sizeLimit ; }
  /**
   * Sets the limit of objects this result may contain.
   * Setting a value 0 means that there is no limit. 
   * 
   * @param limit The maximum number of objects to be contained by this result
   */
  public void setSizeLimit( int limit ) { sizeLimit = limit ; }
  
  private boolean sizeLimitExceeded = false ;
  protected boolean getSizeLimitExceeded() { return sizeLimitExceeded ; }
  protected void setSizeLimitExceeded( boolean newValue ) { sizeLimitExceeded = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ObjectSearchResult()
  {
    super() ;
  } // ObjectSearchResult() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with an initial size.
   * 
   * @param initialCapacity Initial size reserved for elements
   */
  public ObjectSearchResult( int initialCapacity )
  {
    super( initialCapacity ) ;
  } // ObjectSearchResult() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an initial size and a maximum size.
   * If the given initial size is greater than the maximum size, then the
   * maximum size will be used as initial size too.
   * 
   * @param initialCapacity Initial size reserved for elements
   */
  public ObjectSearchResult( int initialCapacity, int maximumSize )
  {
    super( initialCapacity > maximumSize ? maximumSize : initialCapacity ) ;
    this.setSizeLimit( maximumSize ) ;
  } // ObjectSearchResult() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Adds the given object to the result if it is not yet full.
	 * If it is full, the object will not be added and the flag sizeLimitExceeded
	 * will be set to true.
	 * 
	 * @see org.pf.osf.ObjectContainer#add(org.pf.reflect.AttributeReadAccess)
	 */
	public void add( AttributeReadAccess object )
	{
		if ( this.isFull() )
		{
			this.setSizeLimitExceeded( true ) ;
			return ;
		}
		super.add( object );
	} // add() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Resets the result to contain nothing at all.
	 * 
	 * @see org.pf.osf.ObjectContainer#clear()
	 */
	public void clear()
	{
		super.clear();
		this.setSizeLimitExceeded( false ) ;
	} // clear() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see org.pf.osf.ObjectContainer#remove(org.pf.reflect.AttributeReadAccess)
	 */
	public boolean remove( AttributeReadAccess object )
	{
		boolean removed ;
		
		removed = super.remove( object );
		if ( this.isSizeLimitExceeded() && removed )
		{
			this.setSizeLimitExceeded( false ) ;
		}		
		return removed ;
	} // remove() 
	
	// -------------------------------------------------------------------------
	
  /**
   * Returns true if the result is full and no further objects can be added.
   * An attempt to add another object will set the flag sizeLimitExceeded to 
   * true and the object will not be added.
   */
  public boolean isFull() 
	{
		if ( this.getSizeLimit() <= UNLIMITED_SIZE ) 
		{
			return false ; 
		}
		return this.size() >= this.getSizeLimit() ;
	} // isFull() 

	// -------------------------------------------------------------------------

  /**
   * Returns true if the size limit has been exceeded.
   * That is, there has been an attempt to add another object to the result
   * after it was full according to the defined size limit.
   */
  public boolean isSizeLimitExceeded() 
  { 
  	return this.getSizeLimitExceeded() ; 
  } // isSizeLimitExceeded() 
  
  // -------------------------------------------------------------------------  

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class ObjectSearchResult 
