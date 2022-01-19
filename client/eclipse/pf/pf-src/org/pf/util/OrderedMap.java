// ===========================================================================
// CONTENT  : CLASS OrderedMap
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 05/08/2006
// HISTORY  :
//  12/03/2004  mdu  CREATED
//	05/08/2006	mdu		bugfix	-->	changed remove() to return the value and not the Association
//
// Copyright (c) 2004-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A Map implementation that pertains the order of its elements according to
 * when they have been added.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class OrderedMap implements Map
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private AssociationList mapping = null ;
	protected AssociationList getMapping() { return mapping ; }
	protected void setMapping( AssociationList newValue ) { mapping = newValue ; }

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public OrderedMap()
	{
		super() ;
		this.setMapping( this.createEmptyMapping() ) ;
	} // OrderedMap() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Removes all entries from this map.
	 * 
	 * @see java.util.Map#clear()
	 */
	public void clear()
	{
		this.getMapping().clear() ;
	} // clear() 

	// -------------------------------------------------------------------------

	/**
	 * This method returns true, if the given key can be found in this map.
	 * 
	 * @see java.util.Map#containsKey(Object)
	 */
	public boolean containsKey(Object key)
	{
		return ( this.getMapping().findAssociation( key ) != null ) ;
	} // containsKey() 

	// -------------------------------------------------------------------------

	/**
	 * This method returns true, if the given value is a value of any entry
	 * in this map.
	 * 
	 * @see java.util.Map#containsValue(Object)
	 */
	public boolean containsValue(Object value)
	{
		return this.getMapping().asHashMap().containsValue( value ) ;
	} // containsValue() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all entries of the map
	 * 
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet()
	{
		Set entrySet ;
		Map.Entry assoc ;
		
		entrySet = new OrderedSet( this.size() ) ;
		for (int i = 0; i < this.size(); i++)
		{
			assoc = this.getMapping().associationAt(i) ;
			entrySet.add( assoc ) ;
		}
		return entrySet ;
	} // entrySet() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the object that was registered under the specified key.
	 * 
	 * @see java.util.Map#get(Object)
	 */
	public Object get(Object key)
	{
		return this.getMapping().valueAt( key ) ;
	} // get() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if there is no entry in this map.
	 * 
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty()
	{
		return ( this.getMapping().size() == 0 ) ;
	} // isEmpty() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all keys in this map.
	 * 
	 * @see java.util.Map#keySet()
	 */
	public Set keySet()
	{
		Set keys ;
		
		keys = new OrderedSet( this.size() ) ;
		keys.addAll( this.getMapping().keys() ) ;
		return keys ;
	} // keySet() 

	// -------------------------------------------------------------------------

	/**
	 * Puts the given value under the specified key into the map.
	 * 
	 * @see java.util.Map#put(Object, Object)
	 */
	public Object put(Object key, Object value)
	{
		this.getMapping().put( key, value ) ;
		return value ;
	} // put() 

	// -------------------------------------------------------------------------

	/**
	 * Each entry of the given map will be added. 
	 * 
	 * @see java.util.Map#putAll(Map)
	 */
	public void putAll(Map map)
	{
		this.getMapping().putAll( map ) ;
	} // putAll() 

	// -------------------------------------------------------------------------

	/**
	 * Removes the entry with the specified key.
	 * 
	 * @see java.util.Map#remove(Object)
	 */
	public Object remove(Object key)
	{
		Association assoc ;
		
		assoc = this.getMapping().removeKey( key ) ;
		if ( assoc == null )
		{
			return null ;
		}
		return assoc.value() ;
	} // remove() 

	// -------------------------------------------------------------------------

	/**
	 * Returns how many entries currently are in this map.
	 * 
	 * @see java.util.Map#size()
	 */
	public int size()
	{
		return this.getMapping().size() ;
	} // size() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all values of the map.
	 * 
	 * @see java.util.Map#values()
	 */
	public Collection values()
	{
		return this.getMapping().values() ;
	} // values() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================

	/**
	 * Subclasses may override this method to provide an instance of a different 
	 * implementation class than Hashtable.
	 */
	protected AssociationList createEmptyMapping()
	{
		return new AssociationList() ;
	} // createEmptyMapping() 

	// -------------------------------------------------------------------------

} // class OrderedMap 
