// ===========================================================================
// CONTENT  : CLASS NamedValueList
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 28/03/2010
// HISTORY  :
//  14/07/2002  duma  CREATED
//	26/09/2003	duma	added		-->	valueAt( String name )
//	06/02/2004	duma	added		-->	setNamedValueAt()
//	12/02/2004	duma	added		-->	remove( name )
//	27/05/2005	mdu		added		-->	keyClass()
//	27/07/2006	mdu		added		-->	namedValueArray(filter)
//	28/03/2010	mdu		changed to support generic types
//
// Copyright (c) 2002-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.pf.bif.filter.IObjectFilter;

/**
 * A container that holds a collection of NamedValue objects.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class NamedValueList<V> extends AssociationList<String,V>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public static final NamedValue[] EMPTY_NAMED_VALUE_ARRAY = new NamedValue[0] ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public NamedValueList()
  {
    super() ;
  } // NamedValueList() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with values from the given map.
   * All entries where the key is not a String will be skipped.
   */
  public NamedValueList( Map<String,V> map )
  {
    super(map) ;
  } // NamedValueList() 
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Adds the specified named value.
	 * 
	 * @param namedValue The named value to add (must not be null!)
	 */
	public void add( NamedValue<V> namedValue )
	{
		if ( this.isValidAssociation( namedValue ) )
			this.basicAdd( namedValue ) ;
	} // add() 

	// -------------------------------------------------------------------------

	/**
	 * Adds the specified association only if it is an instance of NamedValue.
	 * 
	 * @param association The association to add (must not be null!)
	 */
	public void add( Association<String,V> association )
	{
		if ( association instanceof NamedValue )
			this.add( (NamedValue)association ) ;
	} // add() 

	// -------------------------------------------------------------------------

	/**
	 * Adds the specified name and value as new NamedValue.
	 * 
	 * @param name The name of the named value to add
	 * @param value The value of the named value to add
	 */
	@Override
	public void add( String name, V value )
	{
		this.add( (NamedValue)this.newElement( name, value ) ) ;
	} // add() 

	// -------------------------------------------------------------------------

	/**
	 * Adds all named value elements of the given array to the list.
	 * 
	 * @param namedValues The values to add
	 */
	public void addAll( NamedValue<V>... namedValues )
	{
		if ( namedValues != null )
		{
			for( int i = 0 ; i < namedValues.length ; i++ )
			{
				this.add( namedValues[i] ) ;
			}
		}
	} // addAll() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all named values as an array
	 */
	public NamedValue<V>[] namedValueArray()
	{
		return this.namedValueArray( null ) ;
	} // namedValueArray() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of all those elements contained in this list that match
	 * the given filter. Each NamedValue element of this list gets passed to the
	 * matches() method of the filter. If the filter is null, all elements will 
	 * be returned.
	 * 
	 * @param filter The filter that determines which elements to return in the result array
	 * @return Always an array, never null
	 */
	public NamedValue<V>[] namedValueArray( IObjectFilter filter )
	{
		Collection result ;
		
		result = this.collectElements( filter );
		if ( this.collUtil().isNullOrEmpty( result ) )
		{
			return EMPTY_NAMED_VALUE_ARRAY ;
		}
		return (NamedValue[])this.collUtil().toArray( result ) ;
	} // namedValueArray() 

	// -------------------------------------------------------------------------
	
  /**
   * Returns the named value at the specified index.
   * 
   * @param index The index of the NamedValue
   */
	public NamedValue<V> namedValueAt( int index )
	{
		return (NamedValue)this.associationAt( index ) ;
	} // namedValueAt() 

	// -------------------------------------------------------------------------

	/**
	 * Puts the given named value at the specified index.
	 * 
	 * @param index The index where to put the namedValue 
	 * @param namedValue The named value object to be put at the given index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public void setNamedValueAt( int index, NamedValue<V> namedValue )
	{
		this.setAssociationAt( index, namedValue ) ;
	} // setNamedValueAt() 

	// -------------------------------------------------------------------------

  /**
   * Returns the name (key) of the named value at the specified index.
   * 
   * @param index The index of the NamedValue
   */
	public String nameAt( int index )
	{
		return this.namedValueAt( index ).name() ;
	} // nameAt() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the first named value with the specified name or null if none
	 * can be found.
	 * 
	 * @param name The name of the named value to look for
	 */
	public NamedValue<V> findNamedValue( String name )
	{
		return (NamedValue)this.findAssociation( name ) ;
	} // findNamedValue() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if this list contains an entry with the given name (case-sensitive)
	 */
	public boolean containsName( String name ) 
	{
		return this.findAssociation( name ) != null ;
	} // containsName()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sorts the elements in this list by name in ascending order.
	 */
	public void sort()
	{
		this.sort( true ) ;
	} // sort() 

	// -------------------------------------------------------------------------

	/**
	 * Sorts the elements in this list by name.
	 * 
	 * @param ascending If true, elements will be sorted in ascending order. Otherwise in descending order.
	 */
	public void sort( boolean ascending )
	{
		NamedValue[] elements ;
		
		elements = this.namedValueArray() ;
		Arrays.sort( elements ) ;
		if ( ! ascending )
		{
			CollectionUtil.current().reverse( elements ) ;
		}
		this.clear() ;
		this.addAll( elements ) ;
	} // sort() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the value associated with the specified name or null if the name
	 * cannot be found.
	 * 
	 * @param name The identifier for the desired value
	 */
	public V valueAt( String name )
	{
		return this.findValue( name ) ;
	} // valueAt() 

	// -------------------------------------------------------------------------

	/**
	 * Remove the NamedValue identified by the given name.
	 */
	public NamedValue<V> remove( String name )
	{
		return (NamedValue)this.removeKey( name ) ;
	} // remove() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all names in this list
	 */
	public List<String> names()
	{
		return this.keys() ;
	} // names() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	@Override
	protected Association newElement( String key, V value )
	{
		return new NamedValue( (String)key, value ) ;
	} // newElement() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the class all contained elements must be an instance of
	 * Subclasses usually must override this method.
	 */
	protected Class elementClass()
	{
		return NamedValue.class ;
	} // elementClass() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the class all contained keys must be an instance of.
	 * Here it returns String.class
	 */
	protected Class keyClass()
	{
		return String.class ;
	} // keyClass() 

	// -------------------------------------------------------------------------

} // class NamedValueList 
