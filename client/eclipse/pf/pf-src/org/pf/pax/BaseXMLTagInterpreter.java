// ===========================================================================
// CONTENT  : CLASS BaseXMLTagInterpreter
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.2 - 04/09/2009
// HISTORY  :
//  10/07/1999 	duma  CREATED
//	03/10/1999	duma	added		-> finalizeResult()
//	28/02/2002	duma	changed	-> Support for SAX 2.0
//	17/03/2003	duma	changed	-> Erreanous javadoc
//	04/09/2009	mdu		changed	-> Added methods for LexicalHandler
//
// Copyright (c) 1999-2009, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

/**
 * This is a basic implemantation of a tag interpreter that is compliant
 * to the interface {@link XMLTagInterpreter}.  <br>
 * It extends a HandlerBase which is a "fake" implementation of a ContentHandler.
 * In the concept of PAX there normally is one XMLTagInterpreter for each tag
 * that can be found in an XML stream.  <br>
 * A controller ( see {@link XMLTagInterpreterController} ) activates the
 * corresponding interpreter for each new start tag found by the SAX-parser. <br>
 *
 * This class implements already all of the methods necessary to interact with
 * the controller and the parser. But it is still an abstract class. <br>
 * Subclasses must define the following four methods: <br>
 * <ul>
 *  <li>getTagName()  </li>
 *  <li>start()       </li>
 *  <li>restart()     </li>
 *  <li>getResult()   </li>
 * </ul>
 * Additionally there are two other methods that can be overridden by subclasses
 * if necessary:  <br>
 * <ul>
 *  <li>characterData()   </li>
 *  <li>finalizeResult()  </li>
 * </ul>
 *
 * @author Manfred Duchrow
 * @version 2.2
 */
public abstract class BaseXMLTagInterpreter
	extends DefaultHandler
	implements XMLTagInterpreter
{
	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private XMLTagInterpreterController parseController = null;
	protected XMLTagInterpreterController getParseController()
	{
		return parseController;
	} // getParseController() 
	protected void setParseController(XMLTagInterpreterController aValue)
	{
		parseController = aValue;
	} // setParseController() 

	// =========================================================================
	// ABSTRACT PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns the name of the tag, the interpreter is responsible for. <br>
	 * It must be the exact tag name ( case-sensitive ) without opening and
	 * closing bracket ( '<' , '>' ).
	 *
	 * @return A string containing the name of the tag the receiver is made for.
	 */
	abstract public String getTagName();

	// -------------------------------------------------------------------------

	/**
	 * Starts the work of the receiver the first time.   <br>
	 * This method must be overridden by subclasses. The subclass here normally
	 * creates a new object corresponding to the tag and the given attributes.
	 *
	 * @param elementName The name of the found element. Must be the same as this.getTagName().
	 * @param attributes The attributes defined in the start tag.
	 */
	abstract public void start(String elementName, Attributes attributes)
		throws SAXException;

	// -------------------------------------------------------------------------

	/**
	 * Restarts the receiver after interpretation of a sub tag.   <br>
	 * When the work of sub tag interpreter has been finished by reaching its
	 * end tag, the control is passed back
	 * to the previous tag interpreter, one step higher in the hierarchy.
	 * The tag interpreter controller retreives the result object from that
	 * sub tag interpreter and gives it as an argument together with the
	 * sub tag's name to the restart method of the parent tag interpreter.
	 * Here the result of that sub tag can be put into a slot of this
	 * interpreter's object.
	 *
	 * @param subTagName The name of the sub tag that was completed 
	 * @param subResult The resulting object build from the sub tag.
	 */
	abstract public void restart(String subTagName, Object subResult)
		throws SAXException;

	// -------------------------------------------------------------------------

	/**
	 * Returns the result object, created by this interpreter from the XML data .   <br>
	 *
	 * @return The business object resulting from the parsed XML data.
	 */
	abstract public Object getResult();

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
	public void characterData(String data)
	
	{
	} // characterData() 

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initializes any new instance.
	 */
	public BaseXMLTagInterpreter()
	{
		super();
	} // BaseXMLTagInterpreter() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Sets the controller, the interpreter has to return control to, when finished.
	 * Must not be used or overridden by subclasses.
	 * This method is only for the frameworks internal functionality.
	 * <br>
	 * @param controller The controller of the XML interpretation process.
	 */
	public void setController(XMLTagInterpreterController controller)
	{
		this.setParseController(controller);
	} // setController() 

	// -------------------------------------------------------------------------

	/**
	 * A new sub tag was found. The control is passed to the controller
	 * for invocation of the correct tag interpreter. <br>
	 * After finishing this sub tag, the controller returns control to
	 * this interpreter by calling <i>restart()</i>.<br>
	 * The restart method then gets the result object from this sub tag.
	 *
	 * @see #restart(String,Object)
	 * @see org.xml.sax.ContentHandler#startElement(String,String,String,Attributes)
	 */
	public void startElement(
		String uri,
		String localName,
		String qName,
		Attributes attributes)
		throws SAXException
	{
		this.getParseController().startElement(uri, localName, qName, attributes);
	} // startElement() 

	// -------------------------------------------------------------------------

	/**
	 * Is called from the parser for an end tag.    <br>
	 * This end tag's name must be precisely the same as the tag's name
	 * the receiver is responsible for. <br>
	 * Subclasses must not use ore override this method !  <p>
	 * This method calls finalizeResult() and then returns
	 * controll to the overall tag interpreter controller.
	 *
	 * @param uri The URI of the tag's namespace or null if no namespace defined 
	 * @param localName The name of the end tag without any namespace prefix.
	 * @param qName The qualified name (might contain the namespace prefix)
	 * @see org.xml.sax.ContentHandler#endElement(String,String,String)
	 */
	public void endElement(String uri, String localName, String qName)
		throws SAXException
	{
		if (this.getTagName().equals(qName))
		{
			this.finalizeResult();
			this.getParseController().returnControl(this.getResult());
		}
		else
		{
			String msg =
				"</" + this.getTagName() + "> expected ! - Not </" + qName + ">";
			throw (new SAXException(msg));
		}
	} // endElement() 

	// -------------------------------------------------------------------------

	/**
	 * Is called for PCDATA between a start and end tag.  <br>
	 * Don't use or override this method ! It will pass the received data
	 * as String to the method { @link #characterData(String) }.
	 *
	 * @see #characterData(String)
	 */
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		this.characterData(new String(ch, start, length));
	} // characters() 

	// -------------------------------------------------------------------------

	/**
	 * This is a convenience method for throwing an exception, when a
	 * an invalid sub tag is detected. Subclasses should call this
	 * method with the sub tag's name to throw the appropriate exception.
	 * Normally this occurs at the end of the body of method
	 * {@link #restart(String,Object)}.
	 *
	 * @param tagName The name of the invalid sub tag.
	 * @exception SAXException Is always thrown by this method !
	 */
	public void invalidSubTagError(String tagName) throws SAXException
	{
		String msg = null;

		msg = "Invalid subtag <" + tagName + "> found in <";
		msg = msg + this.getTagName() + "> !";
		throw (new SAXException(msg));
	} // invalidSubTagError() 

	// -------------------------------------------------------------------------

	/**
	 * This is a convenience method for throwing an exception, when a
	 * a required attribute of a tag is missing. Subclasses should call this
	 * method with the attribute's name to throw the appropriate exception.
	 * Normally this occurs in the body of method
	 * {@link #start(String,Attributes)}.
	 *
	 * @param attrName The name of the required attribute.
	 * @exception SAXException Is always thrown by this method !
	 */
	public void requiredAttributeError(String attrName) throws SAXException
	{
		String msg = null;

		msg = "Required attribute '" + attrName + "' missing in <";
		msg = msg + this.getTagName() + "> !";
		throw (new SAXException(msg));
	} // requiredAttributeError() 

	// -------------------------------------------------------------------------

	// ================ Interface LexicalHandler ===============================
	
	public void comment( char[] ch, int start, int length ) throws SAXException
	{
	} // comment() 
	
	// -------------------------------------------------------------------------
	
	public void endCDATA() throws SAXException
	{
	} // endCDATA()
	
	// -------------------------------------------------------------------------
	
	public void endDTD() throws SAXException
	{
	} // endDTD()
	
	// -------------------------------------------------------------------------
	
	public void endEntity( String name ) throws SAXException
	{
	} // endEntity()
	
	// -------------------------------------------------------------------------
	
	public void startCDATA() throws SAXException
	{
	} // startCDATA()
	
	// -------------------------------------------------------------------------
	
	public void startDTD( String name, String publicId, String systemId ) throws SAXException
	{
	} // startDTD()
	
	// -------------------------------------------------------------------------
	
	public void startEntity( String name ) throws SAXException
	{
	} // startEntity()
	
	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	/**
	 * This method is called just before control is passed back to the controller.   <br>
	 * It gives the interpreter a chance to do any necessary completion of
	 * its result object.
	 * Subclasses must override this method, if such finalization is necessary.
	 */
	protected void finalizeResult()
	{
		// default is to do nothing
	} // finalizeResult() 

	// -------------------------------------------------------------------------

	/**
	 * Method for debugging output.   <br>
	 * Currently not supported !
	 */
	protected void trace(String text)
	{
		// this.getParseController().trace( text ) ;
	} // trace() 

	// -------------------------------------------------------------------------

} // class BaseXMLTagInterpreter 
