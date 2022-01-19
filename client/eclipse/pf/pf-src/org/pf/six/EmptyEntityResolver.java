// ===========================================================================
// CONTENT  : CLASS EmptyEntityResolver
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 17/06/2002
// HISTORY  :
//  17/06/2002  duma  CREATED
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.six;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.StringReader;
import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This resolver always returns an empty stream as the result of its 
 * method resolveEntity(). It can be used to provide an empty DTD, if
 * validating is not desired.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class EmptyEntityResolver implements EntityResolver
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public EmptyEntityResolver()
  {
    super();
  } // EmptyEntityResolver()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * @param publicId The public identifier of the external entity
   *        being referenced, or null if none was supplied.
   * @param systemId The system identifier of the external entity
   *        being referenced.
   * @return An InputSource object describing the new input source,
   *         or null to request that the parser open a regular
   *         URI connection to the system identifier.
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @exception java.io.IOException A Java-specific IO exception,
   *            possibly the result of creating a new InputStream
   *            or Reader for the InputSource.
   * @see org.xml.sax.InputSource
   */
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
  {
    InputSource source;
    StringReader reader;

    reader = new StringReader("");
    source = new InputSource(reader);
    return source;
  } // resolveEntity()

  // -------------------------------------------------------------------------

} // class EmptyEntityResolver