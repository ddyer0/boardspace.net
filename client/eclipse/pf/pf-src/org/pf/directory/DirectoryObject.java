// ===========================================================================
// CONTENT  : CLASS DirectoryObject
// AUTHOR   : M.Duchrow
// VERSION  : 1.1 - 27/06/2006
// HISTORY  :
//  23/04/2004  mdu CREATED
//	27/06/2006	mdu	added		-->	equals(), hashCode()
//
// Copyright (c) 2004-2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory ;

import org.pf.osf.MapFacade;
import org.pf.text.MatchRule;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Represents one object in a directory.
 *
 * @author M.Duchrow
 * @version 1.2
 */
public class DirectoryObject extends MultiValueAttributes
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String identifier = null ;
  protected String getIdentifier() { return identifier ; }
  protected void setIdentifier( String newValue ) { identifier = newValue ; }
  
  private MapFacade mapFacade = null ;
  protected MapFacade getMapFacade() { return mapFacade ; }
  protected void setMapFacade( MapFacade newValue ) { mapFacade = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a unique identifier.
   * 
   * @param id The unique identifier of the new object
   */
  public DirectoryObject( String id )
  {
    super() ;
    this.setIdentifier( id ) ;
    this.setMapFacade( new MapFacade( this ) ) ;
  } // DirectoryObject()
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the identifier of this object
   */
  public String getID() 
	{
		return this.getIdentifier() ;
	} // getID()

	// -------------------------------------------------------------------------
  
  /**
   * Returns true, if this object matches the given rule
   */
  public boolean matches( MatchRule rule ) 
	{
		return rule.matches( this.getMapFacade() ) ;
	} // matches()

	// -------------------------------------------------------------------------

  /**
   * Returns true if the given object is of the identical class as this
   * object and if both IDs are equal (case-sensitive).
   */
  public boolean equals( Object object ) 
	{
		if ( object.getClass() != this.getClass()  )
		{
			return false ;
		}
		DirectoryObject otherDirObject = (DirectoryObject)object ;
		return this.getID().equals( otherDirObject.getID() ) ;
	} // equals()
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the hash code of this object
   */
  public int hashCode() 
	{
		return this.getID().hashCode() ;
	} // hashCode()
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class DirectoryObject
