// ===========================================================================
// CONTENT  : INTERFACE XMLTagInterpreter
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.1 - 04/09/2009
// HISTORY  :
//  10/07/1999 	duma  CREATED
//	28/02/2002	duma	changed	-> Support for SAX 2.0
//	04/09/2009	mdu		changed	-> Added LexicalHandler
//
// Copyright (c) 1999-2009, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * This defines the API for a tag interpreter class in
 * the PAX framework.   <br>
 * The concept is based on the idea, that there is a corresponding
 * tag interpreter object (implementing this interface) to each different tag
 * of an XML document.
 *
 * @author Manfred Duchrow
 * @version 2.1
 * @see org.xml.sax.ContentHandler
 */
public interface XMLTagInterpreter extends ContentHandler, LexicalHandler
{
  // -------------------------------------------------------------------------


  /**
   * Returns the name of the tag, the interpreter is responsible for.   <br>
   * The tag name must be returned without brackets.  <br>
   * WRONG: "&lt;Sample&gt;"   <br>
   * CORRECT: "Sample"
   *
   * @return A string containing the name of the tag the receiver is made for.
   */
  public String getTagName() ;  

  // -------------------------------------------------------------------------

  /**
   * Sets the controller, the interpreter has to return control to,
   * when finished with interpretation of its corresonding tag contents.
   *
   * @param controller The controller of the XML interpretation process.
   */
  public void setController( XMLTagInterpreterController controller ) ;  

  // -------------------------------------------------------------------------

  /**
   * Starts the work of the receiver for the first time.   <br>
   * That means, when the parser reached the start tag corresponding to
   * this tag interpreter. Here the business object that results from the
   * tag's data contents should be created and initialized with the given
   * attribute values.
   *
   * @param elementName The name of the found element. Must be the same as this.getTagName().
   * @param attributes The attributes defined in the start tag.
   * @see #getTagName()
   * @throws SAXException If the element name is wrong or any attribute is invalid.
   */
  public void start( String elementName, Attributes attributes )
	  throws SAXException ;

  // -------------------------------------------------------------------------

  /**
   * Restarts the receiver after returning from interpretation of a sub tag.   <br>
   * By receiving the last subtag name and the resulting object of its
   * interpretaion it is possible to plug this result-object into the right
   * slot of the receiver's result-object.
   *
   * @param subTagName The name of the sub element to which the result belongs.
   * @param subResult The object created from the subtag's contents.
   * @throws SAXException If the subtag is not allowed here.
   */
  public void restart( String subTagName , Object subResult )
	  throws SAXException ;

  // -------------------------------------------------------------------------


} // interface XMLTagInterpreter