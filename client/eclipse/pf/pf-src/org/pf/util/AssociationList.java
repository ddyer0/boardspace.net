// ===========================================================================
// CONTENT  : CLASS AssociationList
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 27/03/2010
// HISTORY  :
//  14/07/2002  duma  CREATED
//	26/09/2003	duma	added		-->	valueAt( Object key )
//										added		-->	findAssociation( Object key )
//										changed	-->	internal list from Vector to ArrayList
//	06/02/2004	duma	added		-->	setAssociationAt()
//	12/02/2004	duma	added		-->	remove methods
//	12/03/2004	duma	added		-->	put - methods, values(), keys()
//	27/05/2005	mdu		added		-->	AssociationList(Map), keyClass(), valueClass()
//	27/07/2006	mdu		added		-->	associationArray(filter)
//	27/03/2010	mdu		changed to support generic types
//
// Copyright (c) 2002-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.pf.bif.callback.IObjectProcessor;
import org.pf.bif.filter.IObjectFilter;

/**
 * A container that holds a collection of Association objects.
 * No null values are allowed. Neither for association keys nor for association
 * values.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class AssociationList<K,V>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public static final Association[] EMPTY_ASSOCIATION_ARRAY = new Association[0] ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private List<Association<K,V>> list = null ;
  protected List<Association<K,V>> getList() { return list ; }
  protected void setList( List<Association<K,V>> newValue ) { list = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public AssociationList()
  {
    super() ;
    this.clear() ;
  } // AssociationList() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with values from the given map.
   */
  public AssociationList( Map<K,V> map )
  {
    super() ;
    this.clear() ;
    this.addAll( map ) ;
  } // AssociationList() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Removes all association from the list.
   */
	public void clear()
	{
		this.setList( new ArrayList() ) ;
	} // clear() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns the number of associations in this list
	 */
	public int size()
	{
		return this.getList().size() ;
	} // size() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns true if this list has no elements
	 */
	public boolean isEmpty()
	{
		return this.getList().isEmpty() ;
	} // isEmpty() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Adds the specified association.
	 * 
	 * @param association The association to add (must not be null!)
	 */
	public void add( Association<K,V> association )
	{
		if ( this.isValidAssociation( association ) )
			this.basicAdd( association ) ;
	} // add() 
  
	// -------------------------------------------------------------------------

	/**
	 * Adds the specified key and value as new association.
	 * 
	 * @param key The key of the association to add
	 * @param value The value of the association to add
	 */
	public void add( K key, V value )
	{
		this.add( newElement( key, value ) ) ;
	} // add() 
  
	// -------------------------------------------------------------------------

	/**
	 * Add all entries of the specified map as associations
	 * 
	 * @param map The map that contains all the key/value pairs to add
	 */
	public void addAll( Map<K,V> map )
	{
		this.addOrPutAll( map, true ) ;
	} // addAll() 
  
	// -------------------------------------------------------------------------

	/**
	 * Add all entries of the specified properties as associations
	 * 
	 * @param props The map that contains all the key/value pairs to add
	 */
	public void addAll( Properties props )
	{
		this.addOrPutAll( props, true ) ;
	} // addAll() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Adds all association elements of the given array to the list.
	 * 
	 * @param associations The associations to add
	 */
	public void addAll( Association<K,V>... associations )
	{
		if ( associations != null )
		{
			for( int i = 0 ; i < associations.length ; i++ )
			{
				this.add( associations[i] ) ;
			}
		}
	} // addAll() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns all associations as an array
	 */
	public Association<K,V>[] associationArray()
	{
		return this.associationArray( null ) ;
	} // associationArray() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns an array of all those elements contained in this list that match
	 * the given filter. Each Association element of this list gets passed to the
	 * matches() method of the filter. If the filter is null, all elements will 
	 * be returned.
	 * 
	 * @param filter The filter that determines which elements to return in the result array
	 * @return Always an array, never null
	 */
	public Association<K,V>[] associationArray( IObjectFilter filter )
	{
		Collection result ;
		
		result = this.collectElements( filter ) ;
		if ( this.collUtil().isNullOrEmpty( result ) )
		{
			return EMPTY_ASSOCIATION_ARRAY ;
		}
		return (Association[])this.collUtil().toArray( result ) ;
	} // associationArray() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns all associations as list
	 * 
	 * @return List<Association>
	 */
	public List<Association<K,V>> asList()
	{
		List newList ;
		
		newList = new ArrayList( this.size() );
		newList.addAll( this.getList() ) ;
		return newList ;
	} // asList() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns all associations transformed to a Hashtable where the association 
	 * keys became the map keys and the association values became the map values.
	 */
	public Hashtable<K,V> asHashtable()
	{
		Hashtable map	= new Hashtable(this.size()) ;
		return (Hashtable)this.addAllToMap( map ) ;
	} // asHashtable() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns all associations transformed to a HashMap where the association 
	 * keys became the map keys and the association values became the map values.
	 */
	public HashMap<K,V> asHashMap()
	{
		HashMap map	= new HashMap(this.size()) ;
		return (HashMap)this.addAllToMap( map ) ;
	} // asHashMap() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns the given map object with  all associations added to it.
	 * 
	 * @param map A valid map object
	 */
	public Map<K,V> addAllToMap( final Map<K,V> map )
	{
		AAssociationProcessor processor = new AAssociationProcessor<K,V>()
		{
			public boolean processObject( Association<K,V> association )
			{
				map.put( association.key(), association.value() ) ;
				return true;
			}
		} ;
		this.processEach( processor ) ;
		return map ;
	} // addAllToMap() 
  
	// -------------------------------------------------------------------------

	/**
	 * Calls the given associationProcessor once for each association in this
	 * list. 
	 * Each object that gets passed to processObject() is of type Association.
	 * 
	 * @param associationProcessor The processor to be called for each association
	 */
	public void processEach( IObjectProcessor associationProcessor ) 
	{
		// Access elements by index because it is faster than an iterator
		int count ;
		boolean goOn = true ;
		
		count = this.getList().size() ;
		for (int i = 0; (i < count) && goOn; i++ )
		{
			goOn = associationProcessor.processObject( this.getList().get(i) ) ;
		}
	} // processEach() 
	
	// -------------------------------------------------------------------------
	
  /**
   * Returns the association at the specified index.
   * 
   * @param index The index of the Association
   */
	public Association<K,V> associationAt( int index )
	{
		return (Association)this.getList().get( index ) ;
	} // associationAt() 
  
	// -------------------------------------------------------------------------

	/**
	 * Puts the given association at the specified index.
	 * 
	 * @param index The index of the association to set
	 * @param associaction The association to put at the given index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public void setAssociationAt( int index, Association<K,V> associaction )
	{
		this.getList().set( index, associaction ) ;
	} // setAssociationAt() 
  
	// -------------------------------------------------------------------------

  /**
   * Returns the key of the association at the specified index.
   * 
   * @param index The index of the Association
   */
	public K keyAt( int index )
	{
		return this.associationAt( index ).key() ;
	} // keyAt() 
  
	// -------------------------------------------------------------------------

  /**
   * Returns the value of the association at the specified index.
   * 
   * @param index The index of the Association
   */
	public V valueAt( int index )
	{
		return this.associationAt( index ).value() ;
	} // valueAt() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns the value associated with the specified key or null if the key
	 * cannot be found.
	 * 
	 * @param key The identifier for the desired value
	 */
	public V valueAt( K key )
	{
		return this.findValue( key ) ;
	} // valueAt() 
  
	// -------------------------------------------------------------------------

	/**
	 * Returns the association with the given key or null if the key can't be found.
	 * @param key The key to identify the association
	 */
	public Association<K,V> findAssociation( K key )
	{
		Iterator iter ;
		Association assoc ;
		
		if ( key != null )
		{			
			iter = this.getList().iterator() ;
			while ( iter.hasNext() )
			{
				assoc = (Association)iter.next();
				if ( key.equals( assoc.key() ) )
					return assoc ;
			}
		}
		return null ;	
	} // findAssociation() 
  
	// -------------------------------------------------------------------------

	/**
	 * Removes the given association from this list.
	 * Returns true, if the association was found in the list and now removed from
	 * it.
	 */
	public boolean remove( Association<K,V> association )
	{
		return this.getList().remove( association ) ;
	} // remove() 

	// -------------------------------------------------------------------------

	/**
	 * Removes the association at the given index.
	 * Returns the removed association.
	 */
	public Association<K,V> remove( int index )
	{
		return (Association)this.getList().remove( index ) ;
	} // remove() 

	// -------------------------------------------------------------------------

	/**
	 * Removes the association with the given key.
	 * Returns the removed association or null if not found.
	 */
	public Association<K,V> removeKey( K key )
	{
		Association assoc ;
		
		assoc = this.findAssociation( key ) ;
		if ( assoc == null )
			return null ;
		
		if ( this.remove( assoc ) )	
			return assoc ;
		else
			return null ;
	} // removeKey() 

	// -------------------------------------------------------------------------

	/**
	 * Removes all association that are contained in the given collection. 
	 */
	public void removeAll( Collection<Association<K,V>> assocCollection )
	{
		this.getList().removeAll( assocCollection ) ;
	} // removeAll() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all values that are currently stored (might contain duplicates)
	 */
	public List<V> values()
	{
		List values ;
		
		values = new ArrayList( this.size() ) ;
		for (int i = 0; i < this.size(); i++)
		{
			values.add( this.valueAt(i) ) ;
		}
		return values ;
	} // values() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all values that are currently stored (might contain duplicates)
	 */
	public List<K> keys()
	{
		List keys ;
		
		keys = new ArrayList( this.size() ) ;
		for (int i = 0; i < this.size(); i++)
		{
			keys.add( this.keyAt(i) ) ;
		}
		return keys ;
	} // keys() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given key is in the list
	 * 
	 * @param key The key to look for
	 */
	public boolean containsKey( K key )
	{
		return this.findAssociation( key ) != null ;
	} // containsKey() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Puts the given value under the specified key. If the key already exists,
	 * its associated value will be replaced. Otherwise the new key/value pair
	 * will be added at the end of the list.
	 */
	public void put( K key, V value )
	{
		Association assoc ;
		
		assoc = this.findAssociation( key ) ;
		if ( assoc == null )
		{
			assoc = this.newElement( key, value ) ;
			this.add( assoc ) ;
		}
		else
		{
			assoc.value( value ) ;
		}
	} // put() 

	// -------------------------------------------------------------------------
	
	/**
	 * Adds the specified association.
	 * 
	 * @param association The association to add (must not be null!)
	 */
	public void put( Association<K,V> association )
	{
		int index ;
		
		if ( ! this.isCorrectElementType( association ) )
			return ; // Ignore elements of the wrong element type
		
		index = this.indexOf( association.key() ) ;
		if ( index < 0 )
		{
			this.add( association ) ;
		}
		else
		{
			this.getList().set( index, association ) ;
		}
	} // put() 
  
	// -------------------------------------------------------------------------
	
	/**
	 * Adds all association elements of the given array to the list.
	 * 
	 * @param associations The associations to add
	 */
	public void putAll( Association<K,V>[] associations )
	{
		if ( associations != null )
		{
			for( int i = 0 ; i < associations.length ; i++ )
			{
				this.put( associations[i] ) ;
			}
		}
	} // putAll() 
  
	// -------------------------------------------------------------------------
	
	/**
	 * Puts all key-value pairs of the given map to the list.
	 * If elements with the same key already exist then their value will be
	 * replaced.
	 * 
	 * @param map The associations to add
	 */
	public void putAll( Map<K,V> map )
	{
		this.addOrPutAll( map, false ) ;
	} // putAll() 
  
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the index of the given key or -1 if the key can't be found.
	 * @param key The key to look for
	 */
	public int indexOf( K key )
	{
		Association assoc ;
		
		if ( key != null )
		{			
			for (int i = 0; i < this.size(); i++)
			{
				assoc = (Association)this.getList().get(i) ;
				if ( key.equals( assoc.key() ) )
					return i ;
			}
		}
		return -1 ;	
	} // indexOf() 
  
	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	protected boolean isValidAssociation( Association<K,V> association )
	{
		return	(	( association != null ) &&
							( association.key() != null ) &&
							( association.value() != null ) &&
							( this.isCorrectElementType( association ) )
						) ;
	} // isValidAssociation() 
  
	// -------------------------------------------------------------------------

	protected void basicAdd( Association<K,V> association )
	{
		this.getList().add( association ) ;		
	} // basicAdd() 
  
	// -------------------------------------------------------------------------

	protected V findValue( K key )
	{
		Association<K,V> assoc ;

		assoc = this.findAssociation( key ) ;
		if ( assoc == null )		
			return null ;
			
		return assoc.value() ; 	
	} // findValue() 
  
	// -------------------------------------------------------------------------

	protected void addOrPutAll( Map map, boolean add )
	{
		Iterator iter ;
		Map.Entry entry ;
		
		if ( map != null )
		{
			iter = map.entrySet().iterator() ;
			while ( iter.hasNext() )
			{
				entry = (Map.Entry)iter.next();
				if ( ( this.isCorrectKeyType(entry.getKey()) ) 
					&& ( this.isCorrectValueType(entry.getValue()) ) )
				{
					if ( add )
					{
						this.add( (K)entry.getKey(), (V)entry.getValue() ) ;
					}
					else
					{
						this.put( (K)entry.getKey(), (V)entry.getValue() ) ;
					}
				}
			}
		}
	} // addOrPutAll() 
	
	// -------------------------------------------------------------------------	

	protected boolean isCorrectElementType( Association<K,V> association )
	{
		return this.elementClass().isInstance( association ) ;
	} // isCorrectElementType() 

	// -------------------------------------------------------------------------

	protected boolean isCorrectKeyType( Object key )
	{
		return this.keyClass().isInstance( key ) ;
	} // isCorrectKeyType() 

	// -------------------------------------------------------------------------

	protected boolean isCorrectValueType( Object value )
	{
		return this.valueClass().isInstance( value ) ;
	} // isCorrectValueType() 

	// -------------------------------------------------------------------------

	protected Association newElement( K key, V value )
	{
		return new Association( key, value ) ;
	} // newElement() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the class all contained elements must be an instance of
	 * Subclasses usually must override this method.
	 */
	protected Class elementClass()
	{
		return Association.class ;
	} // elementClass() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the class all contained keys must be an instance of
	 * Subclasses usually must override this method.
	 */
	protected Class keyClass()
	{
		return Object.class ;
	} // keyClass() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the class all contained objects must be an instance of
	 * Subclasses usually must override this method.
	 */
	protected Class<V> valueClass()
	{
		return (Class<V>)Object.class ;
	} // valueClass() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of all those elements contained in this list that match
	 * the given filter. Each Association element of this list gets passed to the
	 * matches() method of the filter. If the filter is null, all elements will 
	 * be returned.
	 * 
	 * @param filter The filter that determines which elements to return in the result array
	 */
	protected Collection<Association<K,V>> collectElements( IObjectFilter filter )
	{
		Collection result ;
		
		if ( filter == null )
		{
			result = this.getList();
		}
		else
		{
			result = CollectionUtil.current().copy( this.getList(), filter ) ;
		}
		return result ;
	} // collectElements() 

	// -------------------------------------------------------------------------
	
	protected CollectionUtil collUtil()
	{
		return CollectionUtil.current() ;
	} // collUtil() 
	
	// -------------------------------------------------------------------------
	
} // class AssociationList 
