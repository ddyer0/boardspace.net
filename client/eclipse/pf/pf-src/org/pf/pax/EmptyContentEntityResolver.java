// ===========================================================================
// CONTENT  : CLASS EmptyContentEntityResolver
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 18/01/2014
// HISTORY  :
//  18/01/2014  mdu  CREATED
//
// Copyright (c) 2014, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.io.StringReader;

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
public class EmptyContentEntityResolver implements EntityResolver
{
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  public EmptyContentEntityResolver()
  {
    super();
  } // EmptyContentEntityResolver()

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

} // class EmptyContentEntityResolver
