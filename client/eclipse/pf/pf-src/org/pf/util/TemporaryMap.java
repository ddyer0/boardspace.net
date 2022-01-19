// ===========================================================================
// CONTENT  : CLASS TemporaryMap
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.4 - 05/08/2006
// HISTORY  :
//  12/10/2001  duma  CREATED
//	26/07/2002	duma	changed	-> Completely reimplemented
//	04/09/2002	duma	changed	-> Superclass from Object to MapWrapper
//	05/12/2002	duma	changed -> use HashMap and synchronize in removeExpiredEntries()
//	05/08/2006	mdu		bugfix	-> overridden remove() to avois returning Association
//
// Copyright (c) 2001-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This implementation of a Map behaves like a HashMap except that it
 * associates a timestamp with each entry. Periodically it checks all
 * its entries' timestamp against an expire period.
 * If an entry is expired it will be automatically removed.
 * Therefore all elements just live temporarily in this map.
 *
 * @author Manfred Duchrow
 * @version 1.4
 */
public class TemporaryMap extends MapWrapper implements TriggerClient
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private long expirationTime = 60000 ;
  protected long expirationTime() { return expirationTime ; }
  protected void expirationTime( long newValue ) { expirationTime = newValue ; }
      
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with values that define the expiration
   * of any object in the map and the interval for checking if expired
   * objects are in the map.
   * 
   * @param expiresAfterMs Defines after what time (milliseconds) an unsued 
   * 	object must be removed (must be >= 100)
   * @param checkAfterMs Defines the interval for lookup and remove expired 
   * 	objects (must be >= 10)
   */
  public TemporaryMap( long expiresAfterMs, long checkAfterMs )
  {
    super() ;
    if ( expiresAfterMs < 100 )
    	throw new IllegalArgumentException( "expiresAfterMs must be greater than 99 ms" ) ;
    	
    if ( checkAfterMs < 10 )
    	throw new IllegalArgumentException( "checkAfterMs must be greater than 9 ms" ) ;
    	
    this.expirationTime( expiresAfterMs ) ;	
    
    Trigger.launch( "TemporaryMap.Trigger", this, checkAfterMs ) ;
  } // TemporaryMap()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * This method returns true, if the given key can be found in this map.
	 * If an entry with this key is found, its associated timestamp will be 
	 * set anew, to ensure that any immediate call to get() still returns 
	 * the object.
	 * Otherwise it could happen, that the object is already removed by the 
	 * background process that checks the timestamps and expiration times.
	 * 
	 * @see java.util.Map#containsKey(Object)
	 */
  @Override
	public boolean containsKey(Object key)
	{
		return ( this.get( key ) != null ) ;
	} // containsKey()

  // -------------------------------------------------------------------------

	/**
	 * This method returns true, if the given value is a value of any entry
	 * in this map.
	 * If such an entry is found, its associated timestamp will be set anew,
	 * to ensure that any immediate call to get() still returns the object.
	 * Otherwise it could happen, that the object is already removed by the 
	 * background process that checks the timestamps and expiration times.
	 * 
	 * @see java.util.Map#containsValue(Object)
	 */
	@Override
	public boolean containsValue(Object value)
	{
		Map.Entry entry ;
		Association assoc ;
		
		entry = this.findByValue( value ) ;
		if ( entry == null )
			return false ;		
			
		assoc = (Association)entry.getValue() ;	
		this.touch( assoc ) ;	
		return true ;
	} // containsValue()

  // -------------------------------------------------------------------------

	/**
	 * This method automatically <i>touches</i> every entry, so that its
	 * expire period starts anew.
	 * 
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set entrySet()
	{
		Iterator iterator ;
		Set entries ;
		Map.Entry entry ;
		Map.Entry newEntry ;
		Association assoc ;
		
		entries = new HashSet( this.size() ) ;
		iterator = this.internalMap().entrySet().iterator() ;
		while ( iterator.hasNext() )
		{
			entry = (Map.Entry)iterator.next() ;
			assoc =  (Association)entry.getValue() ;
			this.touch( assoc ) ;
			newEntry = new MapEntry( entry.getKey(), assoc.value() ) ;
			entries.add( newEntry ) ;
		}
		return entries ;
	} // entrySet()

  // -------------------------------------------------------------------------

	/**
	 * Returns the object that was registered under the specified key.
	 * Here this method also renews the timestamp associated with the key's
	 * entry. That is, the expiration time starts from 0 again.
	 * 
	 * @see java.util.Map#get(Object)
	 */
	@Override
	public Object get(Object key)
	{
		Object value ;
		Association assoc ;
		
		value = this.internalMap().get( key ) ;
		if ( value != null )
		{
			assoc = (Association)value ;
			this.touch( assoc ) ;  // Accessing entry refreshes timestamp!
			value = assoc.value() ;
		}
		return value ;
	} // get()

  // -------------------------------------------------------------------------

	/**
	 * Returns all keys in this map.
	 * <br>
	 * <b>Resets the timestamp of all entries to ensure that the immediate usage
	 * of the returned keys with the <i>get()</i> method produce the associated
	 * values!</b>
	 * 
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set keySet()
	{
		this.touchAll() ; 
		return this.internalMap().keySet() ;
	} // keySet()

  // -------------------------------------------------------------------------

	/**
	 * Puts the given value under the specified key into the map.
	 * It also associates a timestamp with this entry. Therefore it can be
	 * automatically removed after the defined expiration time.
	 * 
	 * @see java.util.Map#put(Object, Object)
	 */
	@Override
	public Object put(Object key, Object value)
	{
		Association assoc ;
		
		assoc = new Association( this.newTimestamp(), value ) ;
		return this.internalMap().put( key, assoc ) ;
	} // put()

  // -------------------------------------------------------------------------

	/**
	 * Each entry of the given map will be added. 
	 * Each added entry gets an associated timestamp.
	 * 
	 * @see java.util.Map#putAll(Map)
	 */
	@Override
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
	 * Returns all values of the map.
	 * With this method the timestamps of all entries in this map will be set
	 * anew. That means, their expiration time starts again at 0.
	 * 
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection values()
	{
		Iterator iterator ;
		Association assoc ;
		Collection values ;
		
		values = new ArrayList( this.size() ) ;
		iterator = this.internalMap().values().iterator() ;
		while ( iterator.hasNext() )
		{
			assoc = (Association)iterator.next() ;
			this.touch( assoc ) ;
			values.add( assoc.value() ) ;
		}
		return values ;
	} // values()

  // -------------------------------------------------------------------------

	/**
	 * Remove the entry with the given key and return the associated object.
	 * 
	 * @param key The key of the entry to be removed
	 * @return The associated object or null if the key could not be found
	 */
	@Override
	public Object remove( Object key )
	{
		Association assoc ;
		
		assoc = (Association) this.internalMap().remove( key ) ;
		if ( assoc == null )
		{
			return null ;
		}
		return assoc.value() ;
	} // remove()
	
	// -------------------------------------------------------------------------

	// -------------------------------------------------------------------------
	// ---- Implementation of Interface TriggerClient --------------------------
	// -------------------------------------------------------------------------
	/**
	 * <b>FOR INTERNAL USE ONLY!</b>
	 * <br>
	 * Will be called by a Trigger to check if object clean-up can be executed.
	 * 
	 * @see org.pf.util.TriggerClient#canBeTriggeredBy(Trigger)
	 */
	public boolean canBeTriggeredBy(Trigger trigger)
	{
		return true ;
	} // canBeTriggeredBy()

  // -------------------------------------------------------------------------

	/**
	 * <b>FOR INTERNAL USE ONLY!</b>
	 * <br>
	 * Will be called by a Trigger to execute the clean-up of expired objects.
	 * 
	 * @see org.pf.util.TriggerClient#triggeredBy(Trigger)
	 */
	public boolean triggeredBy(Trigger trigger)
	{
		this.removeExpiredEntries() ;
		return true ;
	} // triggeredBy()

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void touch( Association assoc )
	{
		assoc.key( this.newTimestamp() ) ;
	} // touch()

	// -------------------------------------------------------------------------

	protected void touchAll()
	{
		Iterator iterator ;
		Association assoc ;

		iterator = this.internalMap().values().iterator() ;
		while ( iterator.hasNext() )
		{
			assoc = (Association)iterator.next() ;
			this.touch( assoc ) ;
		}
	} // touchAll()

  // -------------------------------------------------------------------------

	protected Long newTimestamp()
	{
		Long timestamp ;
		
		timestamp = Long.valueOf( System.currentTimeMillis() ) ;
		return timestamp ;
	} // newTimestamp()

	// -------------------------------------------------------------------------
	
	protected void removeExpiredEntries()
	{
		Iterator iterator ;
		Map.Entry entry ;
		
		synchronized( this.internalMap() )
		{
			iterator = this.internalMap().entrySet().iterator() ;
			while ( iterator.hasNext() )
			{
				entry = (Map.Entry)iterator.next() ;
				if ( this.isExpired( (Association)entry.getValue() ) )
				{
					iterator.remove() ;
				}
			}
		}
	} // removeExpiredEntries()

	// -------------------------------------------------------------------------

	protected boolean isExpired( Association assoc )
	{
		long ts = ((Long)assoc.key()).longValue() ;
		long diff = System.currentTimeMillis() - ts ;
		return ( diff > this.expirationTime() ) ;
	} // isExpired()

	// -------------------------------------------------------------------------

	protected Map.Entry findByValue( Object value )
	{
		Iterator iterator ;
		Map.Entry entry ;
		Association assoc ;

		if ( value == null )
			return null ;			
		
		iterator = this.internalMap().entrySet().iterator() ;
		while ( iterator.hasNext() )
		{
			entry = (Map.Entry)iterator.next() ;
			assoc = (Association)entry.getValue() ;
			if ( value.equals( assoc.value() ) )
			{
				return entry ;
			}
		}	
		return null ;	
	} // findByValue()

	// -------------------------------------------------------------------------

	/**
	 * Provide a synchronized HashMap.
	 */
	protected Map createEmptyMap()
	{
		return Collections.synchronizedMap( new HashMap() ) ;
	} // createEmptyMap()

	// -------------------------------------------------------------------------

} // class TemporaryMap