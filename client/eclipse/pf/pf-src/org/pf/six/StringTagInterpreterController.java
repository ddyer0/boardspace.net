// ===========================================================================
// CONTENT  : CLASS StringTagInterpreterController
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 17/01/2014
// HISTORY  :
//  28/02/2002  duma  CREATED
//	29/06/2002	duma	changed	-> Use logger instead of System.err
//	05/07/2002	duma	changed	-> Reports errors without stacktrace
//	17/03/2003	duma	changed	->	Reduced visibility from public to default
//  17/01/2014  mdu   changed ->  xmlDocument, processing instructions
//
// Copyright (c) 2002-2014, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.six;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.logging.Logger;
import org.pf.pax.BaseXMLTagInterpreterController;
import org.pf.pax.XMLTagInterpreterFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * This class controls the reading of an XML stream into a Element
 * hierarchy.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
class StringTagInterpreterController extends BaseXMLTagInterpreterController
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private XmlDocument xmlDocument = new XmlDocument() ;
  public XmlDocument getXmlDocument() { return xmlDocument ; }
  protected void setXmlDocument( XmlDocument newValue ) { xmlDocument = newValue ; }
    	
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a parser.
   * @param parser The SAX parser that reads the XML source.
   */
  public StringTagInterpreterController(XMLReader xmlReader)
  {
    super(xmlReader);
    xmlReader.setErrorHandler(this);
  } // StringTagInterpreterController() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  public Element getElement()
  {
    return this.getXmlDocument().getRootElement();
  } // getElement()

  // -------------------------------------------------------------------------
  
  /**
   * @see org.xml.sax.ErrorHandler#error(SAXParseException)
   */
  @Override
  public void error(SAXParseException exception) throws SAXException
  {
    this.reportError(exception);
  } // error() 

  // -------------------------------------------------------------------------

  /**
   * @see org.xml.sax.ErrorHandler#fatalError(SAXParseException)
   */
  @Override
  public void fatalError(SAXParseException exception) throws SAXException
  {
    this.reportError(exception);
  } // fatalError() 

  // -------------------------------------------------------------------------

  /**
   * @see org.xml.sax.ErrorHandler#warning(SAXParseException)
   */
  @Override
  public void warning(SAXParseException exception) throws SAXException
  {
    this.reportError(exception);
  } // warning() 

  // -------------------------------------------------------------------------
  
  @Override
  public void processingInstruction(String target, String data) throws SAXException
  {
    this.getXmlDocument().addProcessingInstruction(new XmlProcessingInstruction(target, data));
//    System.out.println("PI: " + target + " " + data);
  } // processingInstruction() 
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  /**
   * This method is called, when the end tag of the document was reached.    <br>
   * It gets the result of the last tag interpreter.
   *
   * @param result The result of the all embracing document tag.
   */
  protected void handleFinalResult(Object result)
  {
    Element xmlElement;

    xmlElement = (Element)result;
    xmlElement.getNamespaceDeclarations(); // This resolves the declared namespaces
    this.getXmlDocument().setRootElement(xmlElement);
  } // handleFinalResult() 

  // -------------------------------------------------------------------------

  /**
   * Returns a new instance of a tag interpreter factory.   <br>
   * This method is called during initialization of the controller.
   * Its returned object is put into the variable interpreterFactory.
   */
  protected XMLTagInterpreterFactory createTagInterpreterFactory()
  {
    return new StringTagInterpreterFactory();
  } // createTagInterpreterFactory() 

  // -------------------------------------------------------------------------

  protected void reportError(SAXParseException ex) throws SAXException
  {
    String msg;

    if (LoggerProvider.isLoggingSupressed())
    {
      throw ex;
    }

    msg = "XML parser: Error in line: " + ex.getLineNumber() + " column: " + ex.getColumnNumber() + " ( " + ex.getMessage() + " )";
    if (ex.getSystemId() != null)
    {
      msg = msg + " --- " + ex.getSystemId();
    }

    logger().logError(msg);
  } // reportError() 

  // -------------------------------------------------------------------------

  /**
   * Returns the current logger used by this component to report
   * errors and exceptions. 
   */
  protected static Logger logger()
  {
    return LoggerProvider.getLogger();
  } // logger() 

  // -------------------------------------------------------------------------

} // class StringTagInterpreterController 
