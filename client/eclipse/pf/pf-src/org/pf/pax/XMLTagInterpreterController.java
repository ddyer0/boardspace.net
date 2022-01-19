// ===========================================================================
// CONTENT  : INTERFACE XMLTagInterpreterController
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.1 - 04/09/2009
// HISTORY  :
//  10/07/1999	duma  CREATED
//	28/02/2002	duma	changed	-> Support for SAX 2.0
//	04/09/2009	mdu		added		-> PROP_LEXICAL_HANDLER
//
// Copyright (c) 1999-2009, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * This defines the API for tag interpreter controller classes in
 * the PAX framework.
 * Such a controller has the task to invoke the right tag interpreter
 * object for each start element the SAX parser detects.
 *
 * @author Manfred Duchrow
 * @version 2.1
 */
public interface XMLTagInterpreterController extends ContentHandler
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	public static final String PROP_LEXICAL_HANDLER = "http://xml.org/sax/properties/lexical-handler" ;  
	
	// =========================================================================
	// METHODS
	// =========================================================================
  /**
   * Gives back the control of the element interpretation to the controller.   <br>
   *
   * @param result The resulting object from interpretation of a sub tag.
   */
  public void returnControl( Object result )
	  throws SAXException ;

} // XMLTagInterpreterController