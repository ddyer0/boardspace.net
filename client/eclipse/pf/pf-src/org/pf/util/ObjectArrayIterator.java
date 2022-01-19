// ===========================================================================
// CONTENT  : CLASS ObjectArrayIterator
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 21/01/2007
// HISTORY  :
//  21/01/2007  mdu  CREATED
//
// Copyright (c) 2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Iterator ;
import java.util.NoSuchElementException;

/**
 * Provides iteration over object arrays (i.e. Object[]).
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class ObjectArrayIterator<T> implements Iterator<T>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private T[] array ;
	private int index = 0 ;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
	public ObjectArrayIterator(T[] objects)
  {
    super() ;
    array = objects ;
  } // ObjectArrayIterator() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns <tt>true</tt> if the iteration has more elements. (In other
   * words, returns <tt>true</tt> if <tt>next</tt> would return an element
   * rather than throwing an exception.)
   *
   * @return <tt>true</tt> if the iterator has more elements.
   */
  public boolean hasNext()
  {
  	if ( array == null )
		{
			return false ;
		}
  	if ( index < array.length )
		{
			return true ;
		}
  	return false;
  } // hasNext()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the next element in the iteration.
   *
   * @return the next element in the iteration.
   * @exception NoSuchElementException iteration has no more elements.
   */
  public T next()
  {
  	T object ;
  	
  	if ( array == null )
  	{
  		throw new NoSuchElementException( "Object array is null" ) ;
  	}
  	if ( index >= array.length )
		{
			throw new NoSuchElementException( "Element " + index + " not found." ) ;
		}
  	object = array[index];
  	index++ ;
  	return object ;
  } // next()
  
  // -------------------------------------------------------------------------
  
  /**
   * Remove is actually not supported. It is not reasonable anyway on an
   * immutable object array. <br/>
   * So nothing happens here.
   */
  public void remove()
  {
  	// Do nothing
  } // remove()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class ObjectArrayIterator 
