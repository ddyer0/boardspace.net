// ===========================================================================
// CONTENT  : CLASS StringRenderer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 27/12/2004
// HISTORY  :
//  26/06/2000  duma  CREATED
//	27/12/2004	mdu		changed	-->	Do not quote string if configuration sys so
//
// Copyright (c) 2000-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.renderer;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.joi.ObjectRenderer;
import org.pf.joi.Preferences;

/**
 * This class is responsible to convert String instances to their
 * String representation in inspectors.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class StringRenderer implements ObjectRenderer
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public StringRenderer()
  {
  	super() ;
  } // StringRenderer()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the string representation of the specified object.
   *
   * @param obj Must be a String !
   */
  public String inspectString( Object obj )
  {
  	if ( this.encloseInQuotes() )
		{
  		return ( "\"" + (String)obj + "\"" ) ;
		}
		else
		{
			return (String)obj ;
		}
  } // inspectString()  

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected boolean encloseInQuotes() 
	{
		return Preferences.instance().getQuoteStrings() ;
	} // encloseInQuotes()

	// -------------------------------------------------------------------------
  
} // class StringRenderer