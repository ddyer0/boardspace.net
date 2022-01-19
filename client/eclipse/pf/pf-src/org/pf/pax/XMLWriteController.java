// ===========================================================================
// CONTENT  : CLASS XMLWriteController
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 12/01/2008
// HISTORY  :
//  28/07/2000  duma  CREATED
//	30/07/2004	duma	added		-->	useSingleQuotes() , useDoubleQuotes() 
//	12/01/2008	mdu		added		-->	getIndetIncrement(), setIndentIncrement()
//
// Copyright (c) 2000-2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Writer;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * This is the controller for wriring a DOM tree to an XML stream.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class XMLWriteController extends BaseXMLTagInterpreterController
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private MarkupWriter writer = null;
	protected MarkupWriter getWriter() { return writer; }
	protected void setWriter(MarkupWriter newValue) {	writer = newValue; }

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with the given XML reader and writer.
	 */
	public XMLWriteController(XMLReader xmlReader, Writer writer)
	{
		super(xmlReader);
		this.setWriter(new MarkupWriter(writer));
	} // XMLWriteController()  
	
	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Configures the writer to write single quotes ( ' ) around attribute values.
	 */
	public void useSingleQuotes() 
	{
		this.getWriter().useSingleQuotes() ;
	} // useSingleQuotes() 

	// -------------------------------------------------------------------------
	
	/**
	 * Configures the writer to write double quotes ( " ) around attribute values.
	 */
	public void useDoubleQuotes() 
	{
		this.getWriter().useDoubleQuotes() ;
	} // useDoubleQuotes() 

	// -------------------------------------------------------------------------
	
	/**
	 * Receive notification of the end of a document.
	 */
	public void endDocument()
	{
		this.getWriter().flush() ;
	} // endDocument()
	
	// -------------------------------------------------------------------------

	/**
	 * Returns the number of spaces that are additionally prepended per hierarchy level 
	 * on each line.
	 */
	public int getIndentIncrement() 
	{
		return this.getWriter().getIndentIncrement() ;
	} // getIndentIncrement()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Sets the number of spaces that are additionally prepended per hierarchy level 
	 * on each line.
	 */
	public void setIndentIncrement( int increment ) 
	{
		this.getWriter().setIndentIncrement( increment ) ;
	} // setIndentIncrement()
	
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
	} // handleFinalResult()  

	// -------------------------------------------------------------------------

	/**
	 * Returns a new instance of a tag interpreter factory.   <br>
	 * This method is called during initialization of the controller.
	 * Its returned object is put into the variable interpreterFactory.
	 */
	protected XMLTagInterpreterFactory createTagInterpreterFactory()
	{
		return null;
	} // createTagInterpreterFactory()  

	// -------------------------------------------------------------------------

	/**
	 * Returns an instance of a tag interpreter class corresponding to the
	 * specified tag name. Here it is always the receiver itself.
	 *
	 * @param tagName The name of the tag, the interpreter is looked up for.
	 * @return The tag interpreter instance or null, if the tag isn't supported.
	 */
	protected XMLTagInterpreter getInterpreterFor(String tagName)
		throws SAXException
	{
		return new XMLWriteInterpreter(tagName, this.getWriter());
	} // getInterpreterFor()  

	// -------------------------------------------------------------------------

} // class XMLWriteController