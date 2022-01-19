// ===========================================================================
// CONTENT  : INTERFACE XMLTagInterpreterFactory
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 28/02/2002
// HISTORY  :
//  10/07/1999 	duma  CREATED
//	28/02/2002	duma	changed	-> Support for SAX 2.0
//
// Copyright (c) 1999-2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.xml.sax.SAXException;

/**
 * This defines the API for classes that can create tag interpreter
 * instances for specific tag names in a defined tag set.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public interface XMLTagInterpreterFactory
{
  /**
   * Returns an instance of a tag interpreter class corresponding to the
   * specified tag name.
   *
   * @param tagName The name of the tag, the interpreter is looked up for.
   * @return The tag interpreter instance or null, if the tag isn't supported.
   */
   public XMLTagInterpreter getInterpreterFor( String tagName )
   		throws SAXException ;   

} // XMLTagInterpreterFactory