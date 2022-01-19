// ===========================================================================
// CONTENT  : CLASS NamedTextList
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 28/03/2010
// HISTORY  :
//  14/07/2002  duma  CREATED
//	26/09/2003	duma	added		-->	textAt( String name )
//	06/02/2004	duma	added		-->	setNamedTextAt()
//	27/05/2005	mdu		added		-->	NamedTextList(Map), valueClass()
//	27/07/2006	mdu		added		--> namedTextArray(filter)
//	28/03/2010	mdu		changed to support generic types
//
// Copyright (c) 2002-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.pf.bif.filter.IObjectFilter;

/**
 * A container that holds a collection of NamedText objects.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class NamedTextList extends NamedValueList<String>
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public static final NamedText[] EMPTY_NAMED_TEXT_ARRAY = new NamedText[0] ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public NamedTextList()
  {
    super() ;
  } // NamedTextList() 
 
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with values from the given map.
   * Only key/value pairs from the given map are added to the newly 
   * created object where both key and value are of type string. Other
   * entries in the map will be skipped silently.
   * 
   * @param map A containing key/value pairs to be added to the new object
   */
  public NamedTextList( Map<String,String> map )
  {
    super(map) ;
  } // NamedTextList() 
 
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Adds the specified named text.
	 * 
	 * @param namedText The named text to add (must not be null!)
	 */
	public void add( NamedText namedText )
	{
		if ( this.isValidAssociation( namedText ) )
		{
			this.basicAdd( namedText ) ;
		}
	} // add() 
 
	// -------------------------------------------------------------------------

	/**
	 * Adds the specified association only if it is an instance of NamedText.
	 * 
	 * @param association The association to add (must not be null!)
	 */
	public void add( Association<String,String> association )
	{
		if ( association instanceof NamedText )
		{
			this.add( (NamedText)association ) ;
		}
	} // add() 
 
	// -------------------------------------------------------------------------

	/**
	 * Adds the specified name and text as new NamedText.
	 * 
	 * @param name The name of the named text to add
	 * @param text The text of the named text to add
	 */
	@Override
	public void add( String name, String text )
	{
		this.add( (NamedText)this.newElement( name, text ) ) ;
	} // add() 
 
	// -------------------------------------------------------------------------

	/**
	 * Adds all named text elements of the given array to the list.
	 * 
	 * @param namedTexts The named text objects to add
	 */
	public void addAll( NamedText... namedTexts )
	{
		if ( namedTexts != null )
		{
			for( int i = 0 ; i < namedTexts.length ; i++ )
			{
				this.add( namedTexts[i] ) ;
			}
		}
	} // addAll() 
 
	// -------------------------------------------------------------------------

	/**
	 * Returns all named text pairs as an array
	 */
	public NamedText[] namedTextArray()
	{
		return this.namedTextArray( null ) ;
	} // namedTextArray() 
 
	// -------------------------------------------------------------------------
	
	/**
	 * Returns an array of all those elements contained in this list that match
	 * the given filter. Each NamedText element of this list gets passed to the
	 * matches() method of the filter. If the filter is null, all elements will 
	 * be returned.
	 * 
	 * @param filter The filter that determines which elements to return in the result array
	 * @return Always an array, never null
	 */
	public NamedText[] namedTextArray( IObjectFilter filter )
	{
		Collection result ;
		
		result = this.collectElements( filter ) ;
		if ( this.collUtil().isNullOrEmpty( result ) )
		{
			return EMPTY_NAMED_TEXT_ARRAY ;
		}
		return (NamedText[])this.collUtil().toArray( result ) ;
	} // namedTextArray()

	// -------------------------------------------------------------------------
	
	/**
	 * Returns all name/text pairs transformed to a Properties object.
	 */
	public Properties asProperties()
	{
		Map<String,String> map	= new HashMap<String,String>() ;
		Properties props	= new Properties() ;
		this.addAllToMap( map ) ;
		props.putAll( map ) ;
		return props;
	} // asProperties() 
 
	// -------------------------------------------------------------------------

  /**
   * Returns the named text at the specified index.
   * 
   * @param index The index of the NamedText
   */
	public NamedText namedTextAt( int index )
	{
		return (NamedText)this.associationAt( index ) ;
	} // namedTextAt() 
 
	// -------------------------------------------------------------------------

	/**
	 * Puts the given named text at the specified index.
	 * 
	 * @param index The index where to put the namedText 
	 * @param namedText The named text object to be put at the given index
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	public void setNamedTextAt( int index, NamedText namedText )
	{
		this.setNamedValueAt( index, namedText ) ;
	} // setNamedTextAt() 

	// -------------------------------------------------------------------------

  /**
   * Returns the text (value) at the specified index.
   * 
   * @param index The index of the NamedText
   */
	public String textAt( int index )
	{
		return this.namedTextAt( index ).text() ;
	} // textAt() 
 
	// -------------------------------------------------------------------------

	/**
	 * Returns the first named text with the specified name or null if none
	 * can be found.
	 * 
	 * @param name The name of the named text to look for
	 */
	public NamedText findNamedText( String name )
	{
		return (NamedText)this.findNamedValue( name ) ;
	} // findNamedText() 
 
	// -------------------------------------------------------------------------

	/**
	 * Returns the value associated with the specified name or null if the name
	 * cannot be found.
	 * 
	 * @param name The identifier for the desired value
	 */
	public String textAt( String name )
	{
		return (String)this.findValue( name ) ;
	} // textAt() 
 
	// -------------------------------------------------------------------------

	// =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	@Override
	protected Association newElement( String key, String value )
	{
		return new NamedText( key, value ) ;
	} // newElement() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the class all contained elements must be an instance of
	 * Subclasses usually must override this method.
	 */
	protected Class elementClass()
	{
		return NamedText.class ;
	} // elementClass() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the class all contained objects must be an instance of.
	 * Here it returns String.class
	 */
	protected Class valueClass()
	{
		return String.class ;
	} // valueClass() 

	// -------------------------------------------------------------------------
	
} // class NamedTextList 
