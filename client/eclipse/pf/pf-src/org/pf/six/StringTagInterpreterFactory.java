// ===========================================================================
// CONTENT  : CLASS StringTagInterpreterFactory
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 17/03/2003
// HISTORY  :
//  28/02/2002  duma  CREATED
//	17/03/2003	duma	changed	->	Reduced visibility from public to default
//
// Copyright (c) 2002-2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.six;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.pax.XMLTagInterpreter;
import org.pf.pax.XMLTagInterpreterFactory;
import org.xml.sax.SAXException;

/**
 * This interpreter factory returns a new StringTagInterpreter for
 * each requested tag interpreter.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
class StringTagInterpreterFactory implements XMLTagInterpreterFactory
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public StringTagInterpreterFactory()
  {
    super() ;
  } // StringTagInterpreterFactory()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns an instance of a tag interpreter class corresponding to the
   * specified tag name.
   *
   * @param tagName The name of the tag, the interpreter is looked up for.
   * @return The tag interpreter instance or null, if the tag isn't supported.
   */
  public XMLTagInterpreter getInterpreterFor( String tagName )
  	throws SAXException
  {
   	return new StringTagInterpreter( tagName ) ;
  } // getInterpreterFor()   

  // -------------------------------------------------------------------------

} // class StringTagInterpreterFactory