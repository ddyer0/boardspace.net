// ===========================================================================
// CONTENT  : INTERFACE XMLTagInterpreterFactory
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.2 - 04/09/2009
// HISTORY  :
//  10/07/1999 	duma  CREATED
//	28/02/2002	duma	changed	-> Support for SAX 2.0
//	29/06/2002	duma	bugfix	-> After end tag, this controller must become content handler again
//	04/09/2009	mdu		added		-> Support for LexicalHandler
//
// Copyright (c) 1999-2009, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This defines the API for classes that can create tag interpreter
 * in instances for specific tag names in a defined tag set.
 *
 * @author Manfred Duchrow
 * @version 2.2
 */
public abstract class BaseXMLTagInterpreterController extends DefaultHandler
	  implements XMLTagInterpreterController
{
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private XMLReader reader = null ;
  protected XMLReader getReader() { return reader ; }  
  protected void setReader( XMLReader aValue ) { reader = aValue ; }  

  private Stack interpreterStack = null ;
  protected Stack getInterpreterStack() { return interpreterStack ; }  
  protected void setInterpreterStack( Stack aValue ) { interpreterStack = aValue ; }  

  private XMLTagInterpreterFactory interpreterFactory = null ;
  protected XMLTagInterpreterFactory getInterpreterFactory() { return interpreterFactory ; }  
  protected void setInterpreterFactory( XMLTagInterpreterFactory aValue ) { interpreterFactory = aValue ; }  

  private boolean lexicalHandlerSupported = false ;
  /** Returns true if the LexicalHandler callback is supported by the XML parser */
  protected boolean isLexicalHandlerSupported() { return lexicalHandlerSupported ; }
  protected void setLexicalHandlerSupported( boolean newValue ) { lexicalHandlerSupported = newValue ; }
  
  private LexicalHandlerDispatcher lexicalHandlerHolder = new LexicalHandlerDispatcher() ;
  public LexicalHandlerDispatcher getLexicalHandlerHolder() { return lexicalHandlerHolder ; }
  public void setLexicalHandlerHolder( LexicalHandlerDispatcher newValue ) { lexicalHandlerHolder = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initializes the instance variables with default values.    <br>
   * Subclasses, that are overriding this constructor must call
   * super() in the first line of their constructor !
   */
  protected BaseXMLTagInterpreterController()
  {
		this.setInterpreterStack( new Stack() ) ;
		this.setInterpreterFactory( this.createTagInterpreterFactory() ) ;
  } // BaseXMLTagInterpreterController()  
	
  // -------------------------------------------------------------------------

  /**
   * Initializes the instance variables with default values.    <br>
   * Subclasses, that are overriding this constructor must call
   * super(parser) in the first line of their constructor !
   *
   * @param xmlReader The SAX reader that reads the XML source.
   */
  public BaseXMLTagInterpreterController( XMLReader xmlReader )
  {
		this() ;
		this.setXmlReader( xmlReader ) ;
		this.initLexicalHandler() ;
  } // BaseXMLTagInterpreterController()  

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Gives back the control of the element interpretation to the receiver.   <br>
   * The controller than takes the last tag interpreter from its stack
   * and passes over control to it.
   *
   * @param result The result of a sub tag interpretation.
   */
  public void returnControl( Object result )
	  throws SAXException
  {
		XMLTagInterpreter previous  = null ;
		XMLTagInterpreter current   = null ;
	
		current = this.popFromStack() ;
		this.trace( "Returned from " + current.getTagName() + " - interpreter." ) ;
		if ( this.isStackEmpty() )  // This only happens after the documents end tag
		{
			this.getReader().setContentHandler(this) ;
		  this.handleFinalResult( result ) ;
		}
		else
		{
		  previous = this.peekStack() ;
		  this.trace( "Restarting interpreter for " + previous.getTagName() ) ;
		  this.restartInterpreter( previous, current.getTagName(), result ) ;
		}
  } // returnControl()  

  // -------------------------------------------------------------------------

  /**
   * Supports SAX 2.0
   *
   * @see org.xml.sax.ContentHandler#startElement(String,String,String,Attributes)
   */
	public void startElement( String uri, String localName, String qName,
    		                     Attributes attributes)
    throws SAXException

	{
		this.startElement( qName, attributes ) ;
	} // startElement()
	
  // -------------------------------------------------------------------------

  public void startElement( String name, Attributes attributes )
		throws SAXException
  {
		XMLTagInterpreter interpreter = null ;
	
		interpreter = this.getInterpreterFor( name ) ;
		if ( interpreter != null )
		{
		  this.putOnStack( interpreter ) ;
		  interpreter.setController( this ) ;
		  this.startInterpreter( interpreter, name, attributes ) ;
		}
		else
		{
		  throw ( new SAXException( "Interpreter for <" + name + "> not found !" ) ) ;
		}
  } // startElement()  

	
  // -------------------------------------------------------------------------

	/**
	 * Returns the XML reader that will be used to parse XML streams
	 */
  public XMLReader getXmlReader() 
  { 
  	return this.getReader() ; 
  } // getXmlReader()  
  
	
  // -------------------------------------------------------------------------

	/**
	 * Sets the XML reader that will be used to parse XML streams
	 * 
	 * @param aReader The reader this controller should use to parse XML streams
	 */
  public void setXmlReader( XMLReader aReader ) 
  { 
  	if ( this.getReader() != null )
		{
			this.getReader().setContentHandler( null ) ;
		}
			
  	this.setReader( aReader ) ; 
  	
  	if ( aReader != null )
		{
			aReader.setContentHandler( this ) ;
		}
  } // setXmlReader() 
	
  // -------------------------------------------------------------------------

  // =========================================================================
  // ABSTRACT PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * This method is called, when the end tag of the document was reached.    <br>
   * It gets the result of the last tag interpreter.
   *
   * @param result The result of the all embracing document tag.
   */
  abstract protected void handleFinalResult( Object result ) ;  

  /**
   * Returns a new instance of a tag interpreter factory.   <br>
   * This method is called during initialization of the controller.
   * Its returned object is put into the variable interpreterFactory.
   */
  abstract protected XMLTagInterpreterFactory createTagInterpreterFactory() ;  

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Starts the given tag interpreter for the first time.    <br>
   * The first time is, when the corresponding start tag was found.
   * The interpreter will now be installed in the SAX parser as the
   * current document handler until it returns control to this controller
   * through the method <i>returnControl()</i> or <i>startElement()</i>.<br>
   *
   * @param interpreter The tag interpreter to be started.
   * @param elementName The name of the tag.
   * @param attributes All attributes and their values found in the start tag.
   * @see #returnControl()
   * @see #startElement()
   */
  protected void startInterpreter(  XMLTagInterpreter interpreter,
									String elementName,
									Attributes attributes )
	  throws SAXException
  {
  	this.trace( "Start interpreter for <" + elementName + "> first time" ) ;
		interpreter.start( elementName, attributes ) ;
		this.getReader().setContentHandler( interpreter ) ;
		this.getLexicalHandlerHolder().setLexicalHandler( interpreter ) ;
  } // startInterpreter()  

  // -------------------------------------------------------------------------

  /**
   * Restarts the given tag interpreter after a sub tag interpretation
   * was finished.    <br>
   * If an XML tag contains inner tags, the control is passed to the
   * controller and from there to an appropriate tag interpreter.
   * When this tag interpreter has finished, it returns a business object,
   * which then is passed over to the superior tag interpreter via the
   * <i>restart()</i> method. So this superior interpreter can put this
   * result into its own business object.
   *
   * @param interpreter The tag interpreter to be restarted.
   * @param subTagName The name of the sub tag, the result corresponds to.
   * @param result The result object of the interpretation of the sub tag.
   * @see #XMLTagInterpreter.restart()
   */
  protected void restartInterpreter(  XMLTagInterpreter interpreter,
									  String subTagName,
									  Object result )
	  throws SAXException
  {
		interpreter.restart( subTagName, result ) ;
		this.getReader().setContentHandler( interpreter ) ;
  } // restartInterpreter()  

  // -------------------------------------------------------------------------

  protected XMLTagInterpreter getInterpreterFor( String tagName )
  			throws SAXException
  {
		XMLTagInterpreter interpreter = null ;
	
		interpreter = this.getInterpreterFactory().getInterpreterFor( tagName ) ;
		return interpreter ;
  } // getInterpreterFor()  

  // -------------------------------------------------------------------------

  /**
   * Returns the last element from the stack and removes it.
   *
   * @return The interpreter on top of the stack.
   */
  protected XMLTagInterpreter popFromStack()
  {
		return ( (XMLTagInterpreter)this.getInterpreterStack().pop() ) ;
  } // popFromStack()  

  // -------------------------------------------------------------------------

  /**
   * Returns the last element from the stack without removing it.
   *
   * @return The interpreter on top of the stack.
   */
  protected XMLTagInterpreter peekStack()
  {
		return ( (XMLTagInterpreter)this.getInterpreterStack().peek() ) ;
  } // peekStack()  

  // -------------------------------------------------------------------------

  /**
   * Puts the given tag interpreter on top of the stack.
   */
  protected void putOnStack( XMLTagInterpreter interpreter )
  {
		this.getInterpreterStack().push( interpreter ) ;
  } // putOnStack()  

  // -------------------------------------------------------------------------

  /**
   * Returns whether the stack is empty or not.
   */
  protected boolean isStackEmpty()
  {
		return this.getInterpreterStack().isEmpty() ;
  } // isStackEmpty()  

  // -------------------------------------------------------------------------

  protected void initLexicalHandler() 
	{
		try
		{
			if ( this.getXmlReader().getProperty( PROP_LEXICAL_HANDLER ) == null )
			{
				this.getXmlReader().setProperty( PROP_LEXICAL_HANDLER, this.getLexicalHandlerHolder() ) ;
				this.setLexicalHandlerSupported( true ) ;
			}
		}
		catch ( SAXException ex )
		{
			this.setLexicalHandlerSupported( false ) ;
			this.trace( "LexicalHandler not supported by parser." ) ;
		}
	} // initLexicalHandler()
	
	// -------------------------------------------------------------------------
  
  /**
   * Method for debugging output.   <br>
   * Must be overridden by subclasses for producing output.
   * Is used also by all interpreters.
   */
  protected void trace( String text )
  {
		// Nothing by default
  } // trace()  

  // -------------------------------------------------------------------------

} // class BaseXMLTagInterpreterController