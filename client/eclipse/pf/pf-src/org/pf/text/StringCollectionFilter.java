// ===========================================================================
// CONTENT  : CLASS StringCollectionFilter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 24/02/2006
// HISTORY  :
//  28/05/2005  mdu  CREATED
//	24/02/2006	mdu		changed	-> to extend AStringFilter rather than implementing StringFilter
//
// Copyright (c) 2005-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Collection;

/**
 * This filter is based on a collection of strings. It matches all
 * strings that are in the underlying collection. 
 * By default it compares case-insensitive. Case sensitivity must be switched on
 * explicitly.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class StringCollectionFilter extends AStringFilter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String[] internalStrings = null ;
  protected String[] getInternalStrings() { return internalStrings ; }
  protected void setInternalStrings( String[] newValue ) { internalStrings = newValue ; }

  private boolean unchanged = true ;
  
  private boolean ignoreCase = true ;
  /**
   * Returns true if the string comparison is done case-insensitive 
   */
  public boolean getIgnoreCase() { return ignoreCase ; }
  /**
   * Sets whether or not the string comparison is done case-insensitive 
   */
  public void setIgnoreCase( boolean newValue ) { ignoreCase = newValue ; }
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with an array of strings.
   * 
   * @param strings The strings that define the set that matches this filter
   */
  public StringCollectionFilter( String[] strings )
  {
    super() ;
    this.setInternalStrings( strings ) ;
  } // StringCollectionFilter()
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a collection of strings.
   * 
   * @param strings A list that must only contain String objects
   */
  public StringCollectionFilter( Collection strings )
  {
    super() ;
    this.setInternalStrings( this.str().asStrings( strings ) ) ;
  } // StringCollectionFilter()
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a List of strings provided as one
   * string where the values are separated by the specified separators.
   * 
   * @param strings The strings to add (separated values)
   * @param separators Each charater is treated as a separator between two string values
   */
  public StringCollectionFilter( String strings, String separators )
  {
    super() ;
    this.setInternalStrings( this.str().parts( strings, separators ) ) ;
  } // StringCollectionFilter()
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a List of strings provided as one
   * string where the values are separated by comma (",").
   * 
   * @param strings The strings to add (comma separated values)
   */
  public StringCollectionFilter( String strings )
  {
    this( strings, "," ) ;
  } // StringCollectionFilter()
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true if the given string is in the internal collection of strings.
   * If the given string is null, false will be returned.
   * 
   * @param string The string to look up in the collection
   */
	public boolean matches( String string )
	{
		boolean inArray ;

		if ( ( string == null ) || ( this.getInternalStrings() == null ) )
			return false;
		
		inArray = this.str().contains( this.getInternalStrings(), string, this.getIgnoreCase() ) ;
		return unchanged ? inArray : ! inArray ;
	} // matches()

	// -------------------------------------------------------------------------
	
	/**
	 * Inverts the (match) logic of this filter
	 */
	public void negate() 
	{
		unchanged = ! unchanged ;
	} // negate()

	// -------------------------------------------------------------------------

	/**
	 * Returns true if this filter matches a string if it is found in the 
	 * internal collection.
	 * Returns false if this filter matches a string if it is NOT found in the 
	 * internal collection.
	 */
	public boolean matchesIfInCollection() 
	{
		return unchanged ;
	} // matchesIfInCollection()

	// -------------------------------------------------------------------------
	
	/**
	 * Add the given string to the internal string collection.
	 * If the argument is null it will be ignored.
	 */
	public void add( String string ) 
	{
		String[] newArray ;
		
		if ( string != null )
		{
			if ( this.getInternalStrings() == null )
			{
				newArray = new String[] { string } ;
			}
			else
			{
				newArray = this.str().append( this.getInternalStrings(), string ) ;
			}
			this.setInternalStrings( newArray ) ;
		}
	} // add()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected StringUtil str()
	{
		return StringUtil.current() ;
	} // str() 
 
	// -------------------------------------------------------------------------

} // class StringCollectionFilter
