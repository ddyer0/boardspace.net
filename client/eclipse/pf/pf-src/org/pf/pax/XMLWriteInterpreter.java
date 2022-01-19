// ===========================================================================
// CONTENT  : CLASS XMLWriteInterpreter
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 28/02/2002
// HISTORY  :
//  28/07/2000  duma  CREATED
//	28/02/2002	duma	changed	-> Support for SAX 2.0
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * The instances of this class are responsible for writing the tag's
 * name, attributes and contents to the given MarkupWriter.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class XMLWriteInterpreter extends BaseXMLTagInterpreter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String tagName = null ;
  public String getTagName() { return tagName ; }  
  protected void setTagName( String newValue ) { tagName = newValue ; }  

  private MarkupWriter markupWriter = null ;
  protected MarkupWriter getMarkupWriter() { return markupWriter ; }  
  protected void setMarkupWriter( MarkupWriter newValue ) { markupWriter = newValue ; }  

  protected boolean isEmptyTag       = true ;
  protected boolean lastWasInnerTag  = false ;

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public XMLWriteInterpreter( String name, MarkupWriter writer )
  {
		super() ;
		this.setTagName( name ) ;
		this.setMarkupWriter( writer ) ;
  } // XMLWriteInterpreter()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	public void startElement( String uri, String localName, String qName,
    		                     Attributes attributes)
		throws SAXException
  {
		this.checkEmptyTag() ;
		markupWriter.newIncIndentedLine();
		super.startElement( uri, localName, qName, attributes ) ;
  } // startElement()  

  // -------------------------------------------------------------------------

  public void start( String elementName, Attributes attributes )
	  throws SAXException
  {
		markupWriter.beginStartTag( elementName ) ;
		for ( int index = 0 ; index < attributes.getLength() ; index++ )
		{
		  markupWriter.writeAttribute(  attributes.getQName(index),
										attributes.getValue(index));
		}
  } // start()  

  // -------------------------------------------------------------------------

  public void restart( String subTagName , Object subResult )
	  throws SAXException
  {
		markupWriter.decIndent() ;
		lastWasInnerTag = true ;
  } // restart()  

  // -------------------------------------------------------------------------

  /**
   * Returns the result object, created by this interpreter from the XML data .   <br>
   *
   * @return The business object resulting from the parsed XML data.
   */
  public Object getResult()
  {
		return null ;
  } // getResult()  

  // -------------------------------------------------------------------------

  /**
   * This method is called whenever character data is received from the parser.   <br>
   * In contrary to the ContentHandler.charachters() method it gets the
   * data as string rather than a char[] array with start and length. <br>
   * Subclasses must override this method, if their tag contains PCDATA.
   * Here it's not defined as an abstract method, because the method's empty
   * implementation is convenient for tag interpreters without PCDATA.
   * <br>
   * @param data The data received from the parser.
   * @see org.xml.sax.ContentHandler#characters(char[],int,int)
   */
  public void characterData( String data )
  {
		this.checkEmptyTag() ;
		markupWriter.writeText( data );
		lastWasInnerTag = false ;
  } // characterData()  

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  protected void checkEmptyTag()
  {
		if ( isEmptyTag )
		{
		  isEmptyTag = false ;
		  markupWriter.finishTag() ;
		}
  } // checkEmptyTag()  

  // -------------------------------------------------------------------------

  /**
   * This method is called just before control is passed back to the controller.   <br>
   * It gives the interpreter a chance to do any necessary completion of
   * its result object.
   */
	protected void finalizeResult()
	{
		if ( isEmptyTag )
		  markupWriter.finishEmptyTag() ;
		else
		{
		  if ( lastWasInnerTag )
			markupWriter.newIndentedLine() ;
		  markupWriter.writeEndTag( this.getTagName() ) ;
		}
	} // finalizeResult()

  // -------------------------------------------------------------------------

} // class XMLWriteInterpreter