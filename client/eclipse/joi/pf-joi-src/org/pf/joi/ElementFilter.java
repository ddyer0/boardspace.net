// ===========================================================================
// CONTENT  : CLASS ElementFilter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 16/04/2006
// HISTORY  :
//  26/01/2002  duma  CREATED
//	14/03/2004	duma	added		-->	copy()
//	16/04/2006	mdu		bugfix	-->	copy() din't copy defaultFlag
//
// Copyright (c) 2002-2006, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Modifier;

/**
 * With instances of this object filters for the elements
 * to be displayed can be defined.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class ElementFilter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public static final int DEFAULT				= -1 ;
	public static final int STATIC				= Modifier.STATIC ;	
	public static final int FINAL					= Modifier.FINAL ;	
	public static final int TRANSIENT			= Modifier.TRANSIENT ;	
	public static final int PUBLIC				= Modifier.PUBLIC ;	
	public static final int PRIVATE				= Modifier.PRIVATE ;	
	public static final int PROTECTED			= Modifier.PROTECTED ;	

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private int modifiers = 0 ;
  protected int getModifiers() { return modifiers ; }
  protected void setModifiers( int newValue ) { modifiers = newValue ; }
  
  private boolean defaultFlag = false ;
  protected boolean getDefaultFlag() { return defaultFlag ; }
  protected void setDefaultFlag( boolean newValue ) { defaultFlag = newValue ; }  
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public ElementFilter( int initialFilter )
  {
    super() ;
    this.setModifiers( initialFilter ) ;
  } // ElementFilter()

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with default values.
   */
  public ElementFilter( int initialFilter, boolean defFlag )
  {
  	this( initialFilter ) ;
  	this.setDefaultFlag( defFlag ) ;
  } // ElementFilter()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns true, if any bit in the given mod is also set in this element 
	 * filter.
	 */
	public boolean matchesAny( int mod )
	{
		if ( isDefaultVisibility( mod ) )
		{
			if ( this.isDefaultSet() )
				return true ;
		}
		
		return ( ( mod & this.getModifiers() ) > 0 ) ;			
	} // matchesAny()

  // -------------------------------------------------------------------------

  /**
   * Toggle the filter flag specified by the given value
   */
	public void toggleSwitch( int filterFlag )
	{
		int filter		= 0 ;
		
		if ( filterFlag == DEFAULT )
		{
			this.setDefaultFlag( ! this.getDefaultFlag() ) ;
		}
		else
		{
			filter = this.getModifiers() ;
			
			if ( ( filter & filterFlag ) > 0 )
				filter &= ~filterFlag ;		// switch off
			else
				filter |= filterFlag ; 		// switch on
				
			this.setModifiers( filter ) ;
		}	
	} // toggleSwitch()

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the switch for 'transient' is set
	 */
	public boolean isTransientSet()
	{
		return this.isFlagSet( TRANSIENT ) ;
	} // isTransientSet()

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the switch for 'static' is set
	 */
	public boolean isStaticSet()
	{
		return this.isFlagSet( STATIC ) ;
	} // isStaticSet()

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the switch for 'final' is set
	 */
	public boolean isFinalSet()
	{
		return this.isFlagSet( FINAL ) ;
	} // isFinalSet()

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the switch to filter default visibility is set
	 */
	public boolean isDefaultSet()
	{
		return this.getDefaultFlag() ;
	} // isDefaultSet()

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the switch to filter public visibility is set
	 */
	public boolean isPublicSet()
	{
		return this.isFlagSet( PUBLIC ) ;
	} // isPublicSet()

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the switch to filter protected visibility is set
	 */
	public boolean isProtectedSet()
	{
		return this.isFlagSet( PROTECTED ) ;
	} // isProtectedSet()

  // -------------------------------------------------------------------------

	/**
	 * Returns whether or not the switch to filter private visibility is set
	 */
	public boolean isPrivateSet()
	{
		return this.isFlagSet( PRIVATE ) ;
	} // isPrivateSet()

  // -------------------------------------------------------------------------

	/**
	 * Returns a copy of this element filter
	 */
	public ElementFilter copy()
	{
		return new ElementFilter( this.getModifiers(), this.getDefaultFlag() ) ; 
	} // copy()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns whether or not the switch to filter default visibility is set
	 */
	protected boolean isFlagSet( int flag )
	{
		return ( ( this.getModifiers() & flag ) > 0 ) ;
	} // isFlagSet()

  // -------------------------------------------------------------------------

	protected boolean isDefaultVisibility( int theModifiers )
	{
		final int explicitVisibility = PUBLIC | PROTECTED | PRIVATE ;
		
		return ( ( theModifiers & explicitVisibility ) == 0 ) ;
	} // isDefaultVisibility()

  // -------------------------------------------------------------------------


} // class ElementFilter