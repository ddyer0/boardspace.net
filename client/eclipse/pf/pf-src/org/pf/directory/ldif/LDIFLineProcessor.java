// ===========================================================================
// CONTENT  : CLASS LDIFLineProcessor
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 21/01/2007
// HISTORY  :
//  18/04/2004  mdu  CREATED
//	21/01/2007	mdu		changed	--> extracted processCompletedLine() 	
//
// Copyright (c) 2004-2007, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory.ldif ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import org.pf.directory.ldap.LDAPUtil;
import org.pf.file.LineProcessor;
import org.pf.text.StringPattern;
import org.pf.text.StringUtil;
import org.pf.util.NamedText;

/**
 * A generic line processor for LDIF input.
 * Allows subclasses to concentrate on handling the prepared content of an
 * LDIF input (e.g. file).
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
abstract public class LDIFLineProcessor implements LineProcessor
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String OBJECT_ID_ATTR			= LDAPUtil.DISTINGUISHED_NAME ;
	protected static final String LDIF_VERSION_ATTR		= "version" ;

	protected static final String KEY_VALUE_SEPARATOR	= ":" ;
	protected static final String BASE64_INDICATOR		= ":" ;
	protected static final String COMMENT_INDICATOR		= "#" ;
	protected static final String CONTINUATION_INDICATOR = " " ;
	protected static final String OBJECT_START_INDICATOR = 
																				OBJECT_ID_ATTR + KEY_VALUE_SEPARATOR ;

	protected static final int LINE_TYPE_UNKNOWN			= 0 ;
	protected static final int LINE_TYPE_EMPTY				= 1 ;
	protected static final int LINE_TYPE_COMMENT			= 2 ;
	protected static final int LINE_TYPE_OBJECT				= 3 ;
	protected static final int LINE_TYPE_ATTRIBUTE		= 4 ;
	protected static final int LINE_TYPE_CONTINUED		= 5 ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private StringPattern attributePattern = null ;
	protected StringPattern getAttributePattern() { return attributePattern ; }
	protected void setAttributePattern( StringPattern newValue ) { attributePattern = newValue ; }		

  private StringBuffer lineBuffer = null ;
  protected StringBuffer getLineBuffer() { return lineBuffer ; }
  protected void setLineBuffer( StringBuffer newValue ) { lineBuffer = newValue ; }
  
  private boolean bufferUnused = true ;
  protected boolean isBufferUnused() { return bufferUnused ; }
  protected void setBufferUnused( boolean newValue ) { bufferUnused = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected LDIFLineProcessor()
  {
    super() ;
    this.setAttributePattern( new StringPattern( "*:*" ) ) ;
    this.resetBuffer() ;
  } // LDIFLineProcessor() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * Handles each line provided by the stream reader.
	 * <b>Do not call this method directly. It is only public due to the 
	 * implementation of interface LineProcessor</b>
	 * @see org.pf.file.LineProcessor#processLine(java.lang.String, int)
	 */
	public boolean processLine(String line, int lineNo)
	{
		String dataLine ;
		int lineType ;
		
		if ( this.isBufferUnused() )
		{
			this.addToBuffer( line.trim() ) ;
		}
		else
		{			
			lineType = this.detectLineType( line ) ;
			if ( lineType == LINE_TYPE_CONTINUED ) 
			{
				this.addToBuffer( line.substring(1) ) ;			
			} 
			else 
			{
				dataLine = this.getLineBuffer().toString() ;
				this.resetBuffer() ;
				this.addToBuffer( line.trim() ) ;
				
				return this.processCompletedLine( dataLine );
			}				
		}
		return true ;
	} // processLine() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // ABSTRACT PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Is called to handle the start of a new object.
	 * Returns true if line processing should be continued, otherwise false.
	 * 
	 * @param objectId The id name (i.e. "dn") and its associated value
	 */
	abstract protected boolean handleNewObject( NamedText objectId ) ;
	
	// -------------------------------------------------------------------------
	
	/**
	 * Is called to handle the attribute with the given name and value.
	 * Returns true if line processing should be continued, otherwise false.
	 * 
	 * @param keyValuePair The attribute name and its associated value
	 * @param encoded If true then the value is base64 encoded
	 */
	abstract protected boolean handleAttribute( NamedText keyValuePair, boolean encoded ) ;
	
	// -------------------------------------------------------------------------
	
	/**
	 * Is called to handle a comment inside the LDIF data. The comment indicator
	 * (i.e. '#') is already removed from the parameter line.
	 * Returns true if line processing should be continued, otherwise false.
	 * 
	 * @param line The comment without leading comment line indicator
	 */
	abstract protected boolean handleComment( String line ) ;
	
	// -------------------------------------------------------------------------
	
	/**
	 * Is called to handle an aempty inside the LDIF data. 
	 * Returns true if line processing should be continued, otherwise false.
	 */
	abstract protected boolean handleEmptyLine() ;

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected boolean processCompletedLine( String dataLine )
	{
		int lineType;
		
		lineType = this.detectLineType( dataLine ) ;
		switch ( lineType )
		{
			case LINE_TYPE_ATTRIBUTE :
				return this.processAttribute( dataLine ) ;

			case LINE_TYPE_COMMENT :
				return this.processComment( dataLine ) ;

			case LINE_TYPE_OBJECT :
				return this.processNewObject( dataLine ) ;

			case LINE_TYPE_EMPTY :
				return this.processEmptyLine() ;
		}
		return true ;
	} // processCompletedLine()

	// -------------------------------------------------------------------------
		
	protected boolean processNewObject( String line )
	{
		NamedText keyValue ;
		
		keyValue = this.parseKeyValue( line ) ;
		if ( keyValue != null )
		{
			keyValue.text( this.ldap().normalizeDN( keyValue.text() ) ) ;
			return this.handleNewObject( keyValue ) ;
		}
		return true ;
	} // processNewObject() 

	// -------------------------------------------------------------------------

	protected boolean processAttribute( String line )
	{
		NamedText keyValue ;
		boolean encoded ;
		
		keyValue = this.parseKeyValue( line ) ;
		if ( keyValue != null )
		{
			encoded = keyValue.text().startsWith( BASE64_INDICATOR ) ; 
			if ( encoded )
				keyValue.text( keyValue.text().substring(1).trim() ) ;
			return this.handleAttribute( keyValue, encoded ) ;			
		}
		return true ;
	} // processAttribute() 

	// -------------------------------------------------------------------------
  
	protected boolean processComment( String line )
	{
		return this.handleComment( line.substring(1) ) ; 
	} // processComment() 

	// -------------------------------------------------------------------------

	protected boolean processEmptyLine() 
	{
		return this.handleEmptyLine() ;
	} // processEmptyLine() 

	// -------------------------------------------------------------------------
  
	protected int detectLineType( String line )
	{
		if ( line.trim().length() == 0 )
			return LINE_TYPE_EMPTY ;
			
		if ( line.startsWith( COMMENT_INDICATOR ) )
			return LINE_TYPE_COMMENT ;
			
		if ( line.startsWith( OBJECT_START_INDICATOR ) )
			return LINE_TYPE_OBJECT ;

		if ( line.startsWith( CONTINUATION_INDICATOR ) )
			return LINE_TYPE_CONTINUED ;
		
		if ( this.getAttributePattern().matches( line ) )
			return LINE_TYPE_ATTRIBUTE ;
		
		return LINE_TYPE_UNKNOWN ; 
	} // detectLineType() 

	// -------------------------------------------------------------------------

	protected NamedText parseKeyValue( String line )
	{
		NamedText keyValue ;
		String[] parts ;
		
		parts = this.str().splitNameValue( line, KEY_VALUE_SEPARATOR ) ;
		keyValue = new NamedText( parts[0].trim(), parts[1].trim() ) ;
		
		return keyValue ;
	} // parseKeyValue() 

	// -------------------------------------------------------------------------
	
	protected void addToBuffer( String line ) 
	{
		this.getLineBuffer().append( line ) ;
		this.setBufferUnused( false ) ;
	} // addToBuffer() 

	// -------------------------------------------------------------------------
	
	protected void resetBuffer() 
	{
		this.setLineBuffer( new StringBuffer(100) ) ;
		this.setBufferUnused( true ) ;
	} // resetBuffer() 

	// -------------------------------------------------------------------------
	
	protected LDAPUtil ldap() 
	{
		return LDAPUtil.current() ;
	} // ldap() 

	// -------------------------------------------------------------------------
	
	protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	// -------------------------------------------------------------------------
	
} // class LDIFLineProcessor 
