// ===========================================================================
// CONTENT  : CLASS DistinguishedNameElement
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 27/06/2006
// HISTORY  :
//  27/06/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.util.NamedText ;

/**
 * Represents a single element of a Distinguished Name.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class DistinguishedNameElement extends NamedText
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String ASSIGN_SEPARATOR 	= "=" ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public DistinguishedNameElement( String key, String value )
  {
    super( key, value ) ;
  } // DistinguishedNameElement()

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  public String toString() 
	{
		StringBuffer buffer ;
		
		buffer = new StringBuffer(50) ;
		buffer.append( this.name() ) ;
		buffer.append( ASSIGN_SEPARATOR ) ;
		buffer.append( this.text() ) ;
		return buffer.toString() ;
	} // toString()
	
	// -------------------------------------------------------------------------

  public boolean equalsIgnoreCase( DistinguishedNameElement element ) 
	{
		return this.name().equalsIgnoreCase( element.name() ) 
				&& this.text().equalsIgnoreCase( element.text() ) ; 
	} // equalsIgnoreCase()
	
	// -------------------------------------------------------------------------
  
  public boolean equals( DistinguishedNameElement element ) 
  {
  	return this.name().equals( element.name() )	&& this.text().equals( element.text() ) ; 
  } // equalsIgnoreCase()
  
  // -------------------------------------------------------------------------
  
  public int hashCode() 
	{
		return this.name().hashCode() ^ this.text().hashCode() ;
	} // hashCode()
	
	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class DistinguishedNameElement
