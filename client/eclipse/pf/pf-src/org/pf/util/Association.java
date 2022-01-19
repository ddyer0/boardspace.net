// ===========================================================================
// CONTENT  : CLASS Association
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 27/03/2010
// HISTORY  :
//  14/07/2002  duma  CREATED
//	12/03/2004	duma	added		-->	Implementation of Map.Entry
//	16/07/2004	duma	added		-->	copyAssociation()
//	30/07/2004	duma	added		-->	Serializable
//	27/03/2010	mdu		changed	-->	To support generic types
//
// Copyright (c) 2002-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Serializable;
import java.util.Map;

/**
 * Simple association of a key object and a value object.
 * <br>
 * Access to key and value is provided by
 * <ul>
 *   <li>key()
 *   <li>key( newKey )
 *   <li>value()
 *   <li>value( newValue )
 * </ul>
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class Association<K,V> implements Map.Entry<K,V>, Serializable
{
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private K key 		= null ;
	private V value 	= null ;

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public Association()
  {
    this( null, null ) ;
  } // Association() 

	// -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a key an a value.
   */
  public Association( K key, V value )
  {
    super() ;
    this.key( key ) ;
    this.value( value ) ;
  } // Association() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	public boolean equals( Object obj )
	{
		if ( obj == null )
			return false ;
			
		if ( ! ( obj instanceof Association ) )
			return false ;	
		
		Association assoc = (Association)obj ;
		
		return 	( this.key() == null 	? assoc.key() == null 
																	: this.key().equals(assoc.key())
						)  
    		&& 	(	this.value() == null 	? assoc.value() == null 
    																: this.value().equals(assoc.value())
    				) ;
 	} // equals() 
  
	// -------------------------------------------------------------------------

	public int hashCode()
	{
		int code ;
		
    code = this.key() == null ? 0 : this.key().hashCode() ;
    code = code ^ (this.value() == null ? 0 : this.value().hashCode()) ;
    return code ;		
	} // hashCode() 

	// -------------------------------------------------------------------------
  /**
   * Returns the key of the association.
   */
	public K key()
	{
		return key ;
	} // key() 

	// -------------------------------------------------------------------------

  /**
   * Sets the key of the association.
   */
	public void key( K newKey )
	{
		key = newKey ;
	} // key() 

	// -------------------------------------------------------------------------

  /**
   * Returns the value of the association.
   */
	public V value()
	{
		return value ;
	} // value() 

	// -------------------------------------------------------------------------

  /**
   * Sets the value of the association.
   */
	public void value( V newValue )
	{
		value = newValue ;
	} // value() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the toString() value of key and value separated by "->".
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		String kStr = ( this.key() == null ) ? "null" : this.key().toString() ;
		String vStr = ( this.value() == null ) ? "null" : this.value().toString() ;
		return kStr + "->" + vStr ;
	} // toString() 
	
	// -------------------------------------------------------------------------

	// =========================================================================
	// IMPLEMENTATION OF Map.Entry
	// =========================================================================
	/**
	 * @see java.util.Map.Entry#getKey()
	 */
	public K getKey()
	{
		return this.key();
	} // getKey() 

	// -----------------------------------------------------------------------

	/**
	 * @see java.util.Map.Entry#getValue()
	 */
	public V getValue()
	{
		return this.value();
	} // getValue() 
		
	// -----------------------------------------------------------------------

	/**
	 * @see java.util.Map.Entry#setValue(java.lang.Object)
	 */
	public V setValue(V value)
	{
		this.value( value ) ;
		return value;
	} // setValue() 

	// -----------------------------------------------------------------------

	/**
	 * Returns a copy of this object
	 */
	public Association<K,V> copyAssociation()
	{
		Association<K,V> copy ;
		
		copy = new Association<K,V>( this.key(), this.value() ) ;
		return copy ;
	} // copyAssociation() 

	// -------------------------------------------------------------------------

} // class Association 
