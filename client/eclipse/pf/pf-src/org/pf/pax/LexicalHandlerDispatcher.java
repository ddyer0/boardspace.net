// ===========================================================================
// CONTENT  : CLASS LexicalHandlerDispatcher
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 04/09/2009
// HISTORY  :
//  04/09/2009  mdu  CREATED
//
// Copyright (c) 2009, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.pax ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler ;

/**
 * Forwards all method calls to a delegate LexicalHandler which can be exchanged.
 * That is useful if in fact different LexicalHandler instances are needed
 * during one parsing process.
 * The XMLReader.setProperty( "http://xml.org/sax/properties/lexical-handler", handler )
 * only allows to set the LexicalHandler once.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class LexicalHandlerDispatcher implements LexicalHandler
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private LexicalHandler lexicalHandler = null ;
  public LexicalHandler getLexicalHandler() { return lexicalHandler ; }
  public void setLexicalHandler( LexicalHandler newValue ) { lexicalHandler = newValue ; }

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with no delegate.
   */
  public LexicalHandlerDispatcher()
  {
    super() ;
  } // LexicalHandlerDispatcher()
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an initial handler to delegate to.
   */
  public LexicalHandlerDispatcher( LexicalHandler handler )
  {
  	super() ;
  	this.setLexicalHandler( handler );
  } // LexicalHandlerDispatcher()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	public void comment( char[] ch, int start, int length ) throws SAXException
	{
		if ( this.getLexicalHandler() != null )
		{
			this.getLexicalHandler().comment( ch, start, length );
		}
	} // comment() 
	
	// -------------------------------------------------------------------------
	
	public void endCDATA() throws SAXException
	{
		if ( this.getLexicalHandler() != null )
		{
			this.getLexicalHandler().endCDATA();
		}
	} // endCDATA()
	
	// -------------------------------------------------------------------------
	
	public void endDTD() throws SAXException
	{
		if ( this.getLexicalHandler() != null )
		{
			this.getLexicalHandler().endDTD();
		}
	} // endDTD()
	
	// -------------------------------------------------------------------------
	
	public void endEntity( String name ) throws SAXException
	{
		if ( this.getLexicalHandler() != null )
		{
			this.getLexicalHandler().endEntity( name );
		}
	} // endEntity()
	
	// -------------------------------------------------------------------------
	
	public void startCDATA() throws SAXException
	{
		if ( this.getLexicalHandler() != null )
		{
			this.getLexicalHandler().startCDATA();
		}
	} // startCDATA()
	
	// -------------------------------------------------------------------------
	
	public void startDTD( String name, String publicId, String systemId ) throws SAXException
	{
		if ( this.getLexicalHandler() != null )
		{
			this.getLexicalHandler().startDTD( name, publicId, systemId );
		}
	} // startDTD()
	
	// -------------------------------------------------------------------------
	
	public void startEntity( String name ) throws SAXException
	{
		if ( this.getLexicalHandler() != null )
		{
			this.getLexicalHandler().startEntity( name );
		}
	} // startEntity()
	
	// -------------------------------------------------------------------------
  
} // class LexicalHandlerDispatcher
