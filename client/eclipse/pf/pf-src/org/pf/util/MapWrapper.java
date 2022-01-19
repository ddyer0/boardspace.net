// ===========================================================================
// CONTENT  : CLASS MapWrapper
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 15/01/2012
// HISTORY  :
//  04/09/2002  duma  CREATED
//	30/07/2004	duma	added		-->	Initial capacity in constructor / Serializable
//	15/01/2012	mdu		changed	-->	generic type
//
// Copyright (c) 2002-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is a wrapper class that implements the java.util.Map interface
 * and contains an internal map. All method calls are routed directly to
 * the internal map.<br>
 * Therefore it makes no sense to use this class directly. It is rather meant
 * to be the superclass for other map-wrapping classes that want to slightly 
 * change the behaviour.
 * These subclasses only have to override those methods they really want to
 * change the behaviour of.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class MapWrapper<K, V> implements Map<K, V>, Serializable
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Map<K, V> internalMap = null ;
  protected Map<K, V> internalMap() { return internalMap ; }
  protected void internalMap( Map<K, V> newValue ) { internalMap = newValue ; }

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	/**
	 * Initialize the new instance with default values.
	 * An empty internal map will be created automatically.
	 */
	public MapWrapper()
	{
		super();
		this.internalMap(this.createEmptyMap());
	} // MapWrapper() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with default values.
	 * An empty internal map will be created automatically.
	 * 
	 * @param initialCapacity Defines how many "slots" for elements are created initially
	 */
	public MapWrapper(int initialCapacity)
	{
		super();
		this.internalMap(this.createEmptyMap(initialCapacity));
	} // MapWrapper() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with the given map
	 */
	public MapWrapper(Map<K, V> mapToBeWrapped)
	{
		super();
		this.internalMap(mapToBeWrapped);
	} // MapWrapper() 

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
		this.internalMap().clear();
	} // clear() 

	// -------------------------------------------------------------------------

	/**
	 * This method returns true, if the given key can be found in this map.
	 * 
	 * @see java.util.Map#containsKey(Object)
	 */
	public boolean containsKey(Object key)
	{
		return this.internalMap().containsKey(key);
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
		return this.internalMap().containsValue(value);
	} // containsValue() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all entries of the map
	 * 
	 * @see java.util.Map#entrySet()
	 */
	public Set<Map.Entry<K, V>> entrySet()
	{
		return this.internalMap().entrySet();
	} // entrySet() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the object that was registered under the specified key.
	 * 
	 * @see java.util.Map#get(Object)
	 */
	public V get(Object key)
	{
		return this.internalMap().get(key);
	} // get() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if there is no entry in this map.
	 * 
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty()
	{
		return this.internalMap().isEmpty();
	} // isEmpty() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all keys in this map.
	 * 
	 * @see java.util.Map#keySet()
	 */
	public Set<K> keySet()
	{
		return this.internalMap().keySet();
	} // keySet() 

	// -------------------------------------------------------------------------

	/**
	 * Puts the given value under the specified key into the map.
	 * 
	 * @see java.util.Map#put(Object, Object)
	 */
	public V put(K key, V value)
	{
		return this.internalMap().put(key, value);
	} // put() 

	// -------------------------------------------------------------------------

	/**
	 * Each entry of the given map will be added. 
	 * 
	 * @see java.util.Map#putAll(Map)
	 */
	public void putAll(Map<? extends K, ? extends V> map)
	{
		this.internalMap().putAll(map);
	} // putAll() 

	// -------------------------------------------------------------------------

	/**
	 * Removes the entry with the specified key.
	 * 
	 * @see java.util.Map#remove(Object)
	 */
	public V remove(Object key)
	{
		return this.internalMap().remove(key);
	} // remove() 

	// -------------------------------------------------------------------------

	/**
	 * Returns how many entries currently are in this map.
	 * 
	 * @see java.util.Map#size()
	 */
	public int size()
	{
		return this.internalMap().size();
	} // size() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all values of the map.
	 * 
	 * @see java.util.Map#values()
	 */
	public Collection<V> values()
	{
		return this.internalMap().values();
	} // values() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================

	/**
	 * Subclasses may override this method to provide an instance of a different 
	 * implementation class than HashMap.
	 */
	protected Map<K, V> createEmptyMap()
	{
		return new HashMap<K, V>();
	} // createEmptyMap() 

	// -------------------------------------------------------------------------

	/**
	 * Subclasses may override this method to provide an instance of a different 
	 * implementation class than HashMap.
	 * 
	 * @param initialCapacity Defines how many "slots" for elements are created initially
	 */
	protected Map<K, V> createEmptyMap(int initialCapacity)
	{
		return new HashMap<K, V>(initialCapacity);
	} // createEmptyMap() 

	// -------------------------------------------------------------------------

} // class MapWrapper 
