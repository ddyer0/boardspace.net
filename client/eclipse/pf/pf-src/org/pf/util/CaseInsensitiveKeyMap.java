// ===========================================================================
// CONTENT  : CLASS CaseInsensitiveKeyMap
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 30/07/2004
// HISTORY  :
//  04/09/2002  duma  CREATED
//	30/07/2004	duma	added		-->	Initial capacity in constructor
//
// Copyright (c) 2002-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class provides a map that treats all keys as case-insensitive strings.
 * That means, the methods get(key) and containsKey(key) will find entries
 * regardless of the keys case.
 * Be aware the this is to some degree NOT an exact implementation of the Map
 * interface. Iterators on the keys and values are NOT backed by the 
 * corresponding collection. 
 * That means that particularly a Collections.synchronizedMap() doesn't 
 * work with an instance of this class!
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class CaseInsensitiveKeyMap extends MapWrapper
{
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
  public CaseInsensitiveKeyMap()
  {
    super() ;
  } // CaseInsensitiveKeyMap()

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with an initial capacity.
   * 
   * @param initialCapacity Defines how many "slots" for elements are created initially
   */
  public CaseInsensitiveKeyMap( int initialCapacity )
  {
    super( initialCapacity ) ;
  } // CaseInsensitiveKeyMap()

  // -------------------------------------------------------------------------
    
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * This method returns true, if the given key can be found in this map.
	 * 
	 * @see java.util.Map#containsKey(Object)
	 */
	public boolean containsKey(Object key)
	{
		String name ;
		
		name = this.standardizeKey( key ) ;
		if ( name == null )
			return false ;
			
		return this.internalMap().containsKey( name ) ;
	} // containsKey()

  // -------------------------------------------------------------------------
  
	/**
	 * Returns the object that was registered under the specified key.
	 * The given key must be a String. If not null is returned.
	 * 
	 * @see java.util.Map#get(Object)
	 */
	public Object get(Object key)
	{
		Object value = null ;
		String name ;
		NamedValue namedValue ;

		name = this.standardizeKey( key ) ;		
		if ( name != null )
		{
			namedValue = (NamedValue)this.internalMap().get( name ) ;
			if ( namedValue != null )
			{
				value = namedValue.value() ;
			}
			return value ;
		}
		return null ;
	} // get()

  // -------------------------------------------------------------------------

	/**
	 * Puts the given value under the specified key into the map.    <br>
	 * <b>If the key is no java.lang.String nothing will be put to the map
	 * and the method returns null!</b>
	 * 
	 * @see java.util.Map#put(Object, Object)
	 */
	public Object put(Object key, Object value)
	{
		NamedValue namedValue ;
		String name ;

		name = this.standardizeKey( key ) ;		
		if ( name != null )
		{
			namedValue = new NamedValue( (String)key, value ) ;
			namedValue = (NamedValue)this.internalMap().put( name, namedValue ) ;
			if ( namedValue == null )
				return null ;
			else
				return namedValue.value() ;
		}
		return null ;
	} // put()

  // -------------------------------------------------------------------------

	/**
	 * Each entry of the given map will be added. 
	 * Each added entry gets an associated timestamp.
	 * 
	 * @see java.util.Map#putAll(Map)
	 */
	public void putAll(Map map)
	{
		Iterator iterator ;
		Map.Entry entry ;

		iterator = map.entrySet().iterator() ;
		while ( iterator.hasNext() )
		{
			entry = (Map.Entry)iterator.next() ;
			this.put( entry.getKey(), entry.getValue() ) ;
		}
	} // putAll()

  // -------------------------------------------------------------------------

	/**
	 * This method returns true, if the given value is a value of any entry
	 * in this map.
	 * 
	 * @see java.util.Map#containsValue(Object)
	 */
	public boolean containsValue(Object value)
	{
		Map.Entry entry ;
		
		entry = this.findByValue( value ) ;
		return ( entry != null ) ;
	} // containsValue()

  // -------------------------------------------------------------------------

	/**
	 * Returns all values of the map.
	 * 
	 * @see java.util.Map#values()
	 */
	public Collection values()
	{
		Iterator iterator ;
		NamedValue namedValue ;
		Collection values ;
		
		values = new ArrayList( this.size() ) ;
		iterator = this.internalMap().values().iterator() ;
		while ( iterator.hasNext() )
		{
			namedValue = (NamedValue)iterator.next() ;
			values.add( namedValue.value() ) ;
		}
		return values ;
	} // values()

  // -------------------------------------------------------------------------

	/**
	 * Removes the entry with the specified key.
	 * 
	 * @see java.util.Map#remove(Object)
	 */
	public Object remove(Object key)
	{
		String name ;
		NamedValue namedValue ;
		
		name = this.standardizeKey( key ) ;
		if ( name != null )
		{
			namedValue = (NamedValue)this.internalMap().remove( name ) ;
			if ( namedValue == null )	
				return null ;
			else
				return namedValue.value() ;
		}
		return null ;
	} // remove()

  // -------------------------------------------------------------------------

	/**
	 * This method automatically <i>touches</i> every entry, so that its
	 * expire period starts anew.
	 * 
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet()
	{
		Iterator iterator ;
		Set entries ;
		Map.Entry entry ;
		Map.Entry newEntry ;
		NamedValue namedValue ;
		
		entries = new HashSet( this.size() ) ;
		iterator = this.internalMap().entrySet().iterator() ;
		while ( iterator.hasNext() )
		{
			entry = (Map.Entry)iterator.next() ;
			namedValue =  (NamedValue)entry.getValue() ;
			newEntry = new MapEntry( namedValue.key(), namedValue.value() ) ;
			entries.add( newEntry ) ;
		}
		return entries ;
	} // entrySet()

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected String standardizeKey( Object key )
	{
		if ( key instanceof String )
			return ((String)key).toLowerCase() ;
		else
		  return null ;
	} // standardizeKey()

	// -------------------------------------------------------------------------

	protected Map.Entry findByValue( Object value )
	{
		Iterator iterator ;
		Map.Entry entry ;
		NamedValue namedValue ;

		if ( value == null )
			return null ;			
		
		iterator = this.internalMap().entrySet().iterator() ;
		while ( iterator.hasNext() )
		{
			entry = (Map.Entry)iterator.next() ;
			namedValue = (NamedValue)entry.getValue() ;
			if ( value.equals( namedValue.value() ) )
			{
				return entry ;
			}
		}	
		return null ;	
	} // findByValue()

	// -------------------------------------------------------------------------

} // class CaseInsensitiveKeyMap