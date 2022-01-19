// ===========================================================================
// CONTENT  : CLASS NamedValue
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 27/03/2010
// HISTORY  :
//  14/07/2002  duma  CREATED
//	16/07/2004	duma	added		-->	copyNamedValue()
//	27/03/2010	mdu		changed	-->	to support generic types
//
// Copyright (c) 2002-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * This specialized Association allows only strings as key.
 * <br>
 * Access to key and value is provided by
 * <ul>
 *   <li>name()
 *   <li>name( newName )
 *   <li>value()
 *   <li>value( newValue )
 * </ul>
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class NamedValue<V> extends Association<String,V> implements Comparable
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a name and a null value.
   * If name is null it will be transformed to an empty String ("").
   */
  public NamedValue( String name )
  {
    this( name, null ) ;
  } // NamedValue() 

	// -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a name and an associated value.
   * If name is null it will be transformed to an empty String ("").
   */
  public NamedValue( String name, V value )
  {
    super( ( name == null ) ? "" : name, value ) ;
  } // NamedValue() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Sets the new key. A null key will be ignored.
   */
  @Override
  public void key( String newKey )
  {
  	if ( newKey != null )
		{			
  		super.key( newKey );
		}
  } // key()
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the name (key) of the association.
   * 
   * @param newName The new key (must not be null !)
   */
	public void name( String newName )
	{
		this.key( newName ) ;
	} // name() 

	// -------------------------------------------------------------------------

  /**
   * Returns the name (key) of the association.
   */
	public String name()
	{
		return this.key() ;
	} // name() 

	// -------------------------------------------------------------------------

	/**
	 * Compares this object with the specified object for order. 
	 * Returns a negative integer, zero, or a positive integer as this object 
	 * is less than, equal to, or greater than the specified object.
	 * <br>
	 * Here the comparison will be done on the names of the NamedValue objects.
	 */
	public int compareTo( Object obj )
	{
		if ( obj instanceof NamedValue )
			return this.name().compareTo( ((NamedValue)obj).name() ) ;
		else
			return 1 ;
	} // compareTo() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns a copy of this object
	 */
	public NamedValue copyNamedValue()
	{
		NamedValue copy ;
		
		copy = new NamedValue( this.name(), this.value() ) ;
		return copy ;
	} // copyNamedValue() 

	// -------------------------------------------------------------------------

} // class NamedValue 
