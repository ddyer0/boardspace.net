// ===========================================================================
// CONTENT  : CLASS TaggedBuffer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 04/09/2009
// HISTORY  :
//  04/09/2009  mdu  CREATED
//
// Copyright (c) 2009, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.six ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.StringBuffer ;

/**
 * This buffer knows if its content is from a CDATA[] section or not.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
class TaggedBuffer
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private boolean isTextCDATA = false ;
  protected boolean isTextCDATA() { return isTextCDATA ; }
  protected void setIsTextCDATA( boolean newValue ) { isTextCDATA = newValue ; }

  private StringBuffer buffer ;
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default with a CDATA flag.
   */
  public TaggedBuffer( boolean isCDATA, int initialCapacity )
  {
    super() ;
    this.setIsTextCDATA( isCDATA );
    buffer = new StringBuffer(initialCapacity) ;
  } // TaggedBuffer() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  public TaggedBuffer append( String str ) 
	{
		buffer.append( str );
		return this;
	} // append() 
	
	// -------------------------------------------------------------------------
  
  public TaggedBuffer append( char ch ) 
  {
  	buffer.append( ch );
  	return this;
  } // append() 
  
  // -------------------------------------------------------------------------
  
  public String toString()
  {
  	return buffer.toString();
  } // toString() 
  
  // -------------------------------------------------------------------------

  public int length() 
	{
  	return buffer.length() ;
	} // length() 
	
	// -------------------------------------------------------------------------
  
} // class TaggedBuffer 
