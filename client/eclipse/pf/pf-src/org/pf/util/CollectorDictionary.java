// ===========================================================================
// CONTENT  : CLASS CollectorDictionary
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 17/02/2006
// HISTORY  :
//  27/01/2001  duma  CREATED
//	20/12/2003	duma	implemented	->	containsValue(), putAll()
//	17/02/2006	mdu		bugfix			->	putAll()
//
// Copyright (c) 2001-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * This class implements the java.util.Map interface.
 * Its main difference to a java.util.Hashtable is that for duplicate
 * key additions the corresponding values are kept in a list.
 * All values of this class are lists (java.util.List)
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class CollectorDictionary implements Map
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Map objectDictionary = null ;
  protected Map getObjectDictionary() { return objectDictionary ; }
  protected void setObjectDictionary( Map newValue ) { objectDictionary = newValue ; }

  private boolean acceptDuplicates = false ;
  public boolean getAcceptDuplicates() { return acceptDuplicates ; }
  public void setAcceptDuplicates( boolean newValue ) { acceptDuplicates = newValue ; }
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public CollectorDictionary()
  {
  	this.setObjectDictionary( new Hashtable() ) ;
  } // CollectorDictionary() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with default values.
   */
  public CollectorDictionary( int initialCapacity )
  {
  	this.setObjectDictionary( new Hashtable( initialCapacity ) ) ;
  } // CollectorDictionary() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Removes all mappings from this map (optional operation).
   */
  public void clear()
  {
	  this.getObjectDictionary().clear() ;
  } // clear() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if this map contains a mapping for the specified key.
   */
  public boolean containsKey(Object key)
  {
	  return this.getObjectDictionary().containsKey( key ) ;
  } // containsKey() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if this map maps one or more keys to the specified value.
   * More formally, returns true if and only if this map contains at least
   * one mapping to a value v such that (value==null ? v==null : value.equals(v)).
   *
   * @param value - value whose presence in this map is to be tested.
   * @return true if this map maps one or more keys to the specified value.
   */
  public boolean containsValue(Object value)
  {
	  Iterator iter ;
	  Object key ;
	  List list ;
	  
	  iter = this.keys() ;
	  while ( iter.hasNext())
		{
			key = iter.next();
			list = this.getValues( key ) ;
			if ( list.contains(value) )
				return true ;
		}
	  
		return false ;
  } // containsValue() 

  // -------------------------------------------------------------------------

  /**
   * Returns a set view of the mappings contained in this map.
   */
  public Set entrySet()
  {
	  return this.getObjectDictionary().entrySet() ;
  } // entrySet() 

  // -------------------------------------------------------------------------

  /**
   * Compares the specified object with this map for equality.
   * Returns true if the given object is also a map and the two Maps
   * represent the same mappings.
   */
  public boolean equals(Object obj)
  {
	  return this.getObjectDictionary().equals( obj ) ;
  } // equals() 

  // -------------------------------------------------------------------------

  /**
   * Returns the hash code value for this map.
   */
  public int hashCode()
  {
	  return this.getObjectDictionary().hashCode() ;
  } // hashCode() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if this map contains no key-value mappings.
   */
  public boolean isEmpty()
  {
	  return this.getObjectDictionary().isEmpty() ;
  } // isEmpty() 

  // -------------------------------------------------------------------------

  /**
   * Returns a set view of the keys contained in this map.
   */
  public Set keySet()
  {
	  return this.getObjectDictionary().keySet() ;
  } // keySet() 

  // -------------------------------------------------------------------------

  /**
   * Stores the given value object under the specified key object.
   */
  public Object put(Object key, Object value)
  {
	  List list ;
	  
		list = this.getValues( key );
	  if ( list == null )
	  {
	    list = this.newValueWith( value ) ;
	    this.getObjectDictionary().put( key, list ) ;
	  }
	  else
	  {
	    if ( ( this.getAcceptDuplicates() ) || ( ! list.contains( value ) ) )
	    {
		    list.add( value ) ;
	    }
	  }
	  return null ;
  } // put() 

  // -------------------------------------------------------------------------

  /**
   * Copies all of the mappings from the specified map to this map.
   */
  public void putAll(Map map)
  {
		Iterator iter ;
		Map.Entry entry ;
		Object key ;  
		Object value ;

		if ( map == null )
			return ;

		iter = map.entrySet().iterator() ;
		while ( iter.hasNext())
		{
			entry = (Map.Entry)iter.next();
			key = entry.getKey() ;
			value = entry.getValue() ;
			if ( value instanceof Collection )
			{
				this.putAll( key, (Collection)value ) ;
			}
			else
			{
				this.put( key, value ) ;
			}
		}
  } // putAll() 

  // -------------------------------------------------------------------------

  /**
   * Removes the mapping for this key from this map if present.
   */
  public Object remove(Object key)
  {
	  return this.getObjectDictionary().remove( key ) ;
  } // remove() 

  // -------------------------------------------------------------------------

  /**
   * Returns the number of key-value mappings in this map.
   */
  public int size()
  {
	  return this.getObjectDictionary().size() ;
  } // size() 

  // -------------------------------------------------------------------------

  /**
   * Returns a collection view of the values contained in this map.
   * In this case all elements of the returned collection are java.util.List
   * objects which contain the real values !
   */
  public Collection values()
  {
		return this.getObjectDictionary().values() ;
  } // values() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with default values.
   */
  public CollectorDictionary( boolean allowDuplicates )
  {
  	this() ;
  	this.setAcceptDuplicates( allowDuplicates ) ;
  } // CollectorDictionary() 

  // -------------------------------------------------------------------------

  public Object get(Object key)
  {
	  return this.getValues(key) ;
  } // get() 

  // -------------------------------------------------------------------------

  /**
   * Returns an iterator over all key objects in this map.
   */
  public Iterator keys()
  {
		return this.keySet().iterator() ;
  } // keys() 

  // -------------------------------------------------------------------------

  /**
   * Returns a collection view of the values contained in this map.
   * In this case all elements of the returned collection are java.util.List
   * objects which contain the real values !
   */
  public int valuesSize()
  {
		Iterator iterator		= null ;
		List list						= null ;
		int sum							= 0 ;

		iterator = this.getObjectDictionary().values().iterator() ;
		while ( iterator.hasNext() )
		{
			list = (List)iterator.next() ;
			sum = sum + list.size() ;
		}
		return sum ;
  } // valuesSize() 
  
	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected List getValues(Object key)
	{
		return (List)this.getObjectDictionary().get(key) ;
	} // getValues() 

	// -------------------------------------------------------------------------

	protected List newValue()
	{
		return new Vector() ;
	} // newValue() 

	// -------------------------------------------------------------------------

	protected List newValueWith( Object obj )
	{
		List value  = this.newValue() ;
		value.add( obj ) ;
		return value ;
	} // newValueWith() 

	// -------------------------------------------------------------------------
  
	/**
	 * Copies all of the given values to the collection under the specified key
	 */
	protected void putAll( Object key, Collection values )
	{
		Iterator iter ;	

		iter = values.iterator() ;
		while ( iter.hasNext())
		{
			this.put( key, iter.next() ) ;
		}
	} // putAll() 

	// -------------------------------------------------------------------------
  
} // class CollectorDictionary 
