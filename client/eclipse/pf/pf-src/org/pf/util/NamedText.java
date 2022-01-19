// ===========================================================================
// CONTENT  : CLASS NamedText
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 27/03/2010
// HISTORY  :
//  14/07/2002  duma  CREATED
//	11/02/2004	duma	added		-->	copy()
//	16/07/2004	duma	added		-->	copyNamedText()
//	27/03/2010	mdu		changed	-->	to generic type supprt
//
// Copyright (c) 2002-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * An association of a name string and a text value.
 * Both, name and value can never be null!
 * <br>
 * Access to key and value is provided by
 * <ul>
 *   <li>name()
 *   <li>name( newName )
 *   <li>text()
 *   <li>text( newText )
 * </ul>
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class NamedText extends NamedValue<String>
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with the given name and value.
   * Both, name and value must not be null. If any of them is null it will
   * be automatically translated to an empty String ("").
   */
  public NamedText( String name, String text )
  {
    super( ( name == null ) ? "" : name , ( text == null ) ? "" : text ) ;
  } // NamedText() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Sets the text (value) of the association.
   * 
   * @param newText The new text (a null value will be ignored)
   */
  @Override
  public void value( String newText )
  {
  	if ( newText != null )
  	{			
  		super.value( newText ) ;
  	}
  } // text() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Sets the text (value) of the association.
   * 
   * @param newText The new text (a null value will be ignored)
   */
	public void text( String newText )
	{
		this.value( newText ) ;
	} // text() 

	// -------------------------------------------------------------------------

  /**
   * Returns the text (value) of the association.
   */
	public String text()
	{
		return this.value() ;
	} // text() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a copy of this object
	 */
	public NamedText copy()
	{
		return this.copyNamedText() ;
	} // copy() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a copy of this object
	 */
	public NamedText copyNamedText()
	{
		NamedText copy ;
		
		copy = new NamedText( this.name(), this.text() ) ;
		return copy ;
	} // copyNamedText() 

	// -------------------------------------------------------------------------
	
} // class NamedText 
