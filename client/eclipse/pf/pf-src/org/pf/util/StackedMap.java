// ===========================================================================
// CONTENT  : CLASS StackedMap
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 19/03/2004
// HISTORY  :
//  19/03/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides a map that allows to push() and pop() internal settings to/from
 * a (LIFO) stack.
 * Accessing an element via get(key) always looks in the last map and then
 * continues looking for it (if not found) in the next map on the stack.  
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class StackedMap extends MapWrapper
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private StackedMap parentMap = null ;
	protected StackedMap parentMap() { return parentMap ; }
	protected void parentMap( StackedMap newValue ) { parentMap = newValue ; }
	
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public StackedMap()
  {
    super() ;
  } // StackedMap() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with an internal map.
	 */
	public StackedMap( Map map )
	{
		super( map ) ;
	} // StackedMap() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a parent and avalue map.
	 */
	protected StackedMap( StackedMap parent, Map valueMap )
	{
		super() ;
		this.parentMap( parent ) ;
		this.internalMap( valueMap ) ;
	} // StackedMap() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Removes all entries from this map, but not from the maps on the stack!
	 * 
	 * @see java.util.Map#clear()
	 */
	public void clear()
	{
		super.clear() ;
	} // clear() 

	// -------------------------------------------------------------------------

	/**
	 * This method returns true, if the given key can be found in this map or
	 * any map on the stack.
	 * 
	 * @see java.util.Map#containsKey(Object)
	 */
	public boolean containsKey(Object key)
	{
		boolean found ;
		
		found = super.containsKey( key ) ;
		if ( ( ! found ) && ( this.hasParent() ) )
		{
			found = this.parentMap().containsKey( key ) ;	
		}
		return found ;
	} // containsKey() 

	// -------------------------------------------------------------------------

	/**
	 * This method returns true, if the given value is a value of any entry
	 * in this map or any map on the stack.
	 * 
	 * @see java.util.Map#containsValue(Object)
	 */
	public boolean containsValue(Object value)
	{
		boolean found ;
		
		found = super.containsValue( value ) ;
		if ( ( ! found ) && ( this.hasParent() ) )
		{
			found = this.parentMap().containsValue( value ) ;	
		}
		return found ;
	} // containsValue() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all entries of the map and all stacked maps.
	 * 
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet()
	{
		Set set ;
		
		set = super.entrySet() ;
		if ( this.hasParent() )
		{
			set.addAll( this.parentMap().entrySet() ) ;	
		}		
		return set ;
	} // entrySet() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the object that was registered under the specified key in this
	 * map or in any of the maps on the stack.
	 * 
	 * @see java.util.Map#get(Object)
	 */
	public Object get(Object key)
	{
		Object value ;
		
		value = super.get( key ) ;
		if ( ( value == null ) && ( this.hasParent() ) )
		{
			value = this.parentMap().get( key ) ;
		}
		return value ;
	} // get() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true, if there is no entry in this map and in all maps on the stack.
	 * 
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty()
	{
		boolean empty ;
		
		empty = super.isEmpty() ;
		if ( empty && this.hasParent() )
		{
			empty = this.parentMap().isEmpty() ;
		}
		return empty ;
	} // isEmpty() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all keys in this map and the maps on the stack
	 * 
	 * @see java.util.Map#keySet()
	 */
	public Set keySet()
	{
		Set keys ;
		
		keys = super.keySet() ;
		if ( this.hasParent() )
		{
			keys.addAll( this.parentMap().keySet() ) ;
		}
		return keys ;
	} // keySet() 

	// -------------------------------------------------------------------------

	/**
	 * Puts the given value under the specified key into this map.
	 * 
	 * @see java.util.Map#put(Object, Object)
	 */
	public Object put(Object key, Object value)
	{
		return super.put( key, value ) ;
	} // put() 

	// -------------------------------------------------------------------------

	/**
	 * Each entry of the given map will be added to this map. 
	 * 
	 * @see java.util.Map#putAll(Map)
	 */
	public void putAll(Map map)
	{
		super.putAll( map ) ;
	} // putAll() 

	// -------------------------------------------------------------------------

	/**
	 * Removes the entry with the specified key from this map.
	 * It will not be removed from any map on the stack.
	 * 
	 * @see java.util.Map#remove(Object)
	 */
	public Object remove(Object key)
	{
		return super.remove( key ) ;
	} // remove() 

	// -------------------------------------------------------------------------

	/**
	 * Returns how many entries currently are in this map and the maps on the 
	 * stack.
	 * 
	 * @see java.util.Map#size()
	 */
	public int size()
	{
		int size ;
		
		size = super.size() ;
		if ( this.hasParent() )
		{
			size += this.parentMap().size() ;
		}
		return size ;
	} // size() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all values of this map and the maps on the stack.
	 * 
	 * @see java.util.Map#values()
	 */
	public Collection values()
	{
		Collection values ;
		
		values = super.values() ;
		if ( this.hasParent() )
		{
			values.addAll( this.parentMap().values() ) ;
		}
		return values ;
	} // values() 

	// -------------------------------------------------------------------------

	/**
	 * Pushes the given map onto the top of this stack.
	 */
	public void push( Map map )
	{
		StackedMap newParent ;
		
		newParent = new StackedMap( this.parentMap(), this.internalMap() ) ;
		this.parentMap( newParent ) ;
		this.internalMap( ( map == null ) ? this.createEmptyMap() : map ) ; 
	} // push()

	// -------------------------------------------------------------------------

	/**
	 * Pushes the an newly created map onto the top of this stack.
	 */
	public void push()
	{
		this.push( null ) ;
	} // push()

	// -------------------------------------------------------------------------

	/**
	 * Removes the map at the top of this stack and returns that map 
	 * as the value of this function.
	 */
	public Map pop()
	{
		Map valueMap ;
		
		valueMap = this.internalMap() ;
		if ( this.hasParent() )
		{
			this.internalMap( this.parentMap().internalMap() ) ;
			this.parentMap( this.parentMap().parentMap() ) ;
		}
		return valueMap ;
	} // pop()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Subclasses may override this method to provide an instance of a different 
	 * implementation class than HashMap.
	 */
	protected Map createEmptyMap()
	{
		return new HashMap() ;
	} // createEmptyMap() 

	// -------------------------------------------------------------------------

	protected boolean hasParent()
	{
		return ( this.parentMap() != null ) ;
	} // hasParent() 

	// -------------------------------------------------------------------------

} // class StackedMap 
