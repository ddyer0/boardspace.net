// ===========================================================================
// CONTENT  : CLASS FileEntityResolver
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 06/05/2004
// HISTORY  :
//  19/03/2003  mdu  CREATED
//	26/03/2004	mdu	changed		-->	To be a public class
//	06/05/2004	mdu	bugfix		-->	resolveEntity() must not return null if not validating and protocol is not "file:"
//
// Copyright (c) 2003-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.six ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.pf.text.StringUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Some XML parser packages (e.g. crimson 1.1.3) provide a entity resolver
 * that uses URL connections to retrieve the resources. Unfortunately that 
 * doesn't always work. Especially if the URL connection for files in the local
 * file system are opened with "ftp://" protocol.
 * <p>
 * Therefore this simple entity resolver implementation is capable of returning
 * proper InputResource instances for file ids on the local system.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class FileEntityResolver extends EmptyEntityResolver
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String DTD_EXTENSION = ".DTD" ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private boolean isValidating = true ;
  protected boolean isValidating() { return isValidating ; }
  protected void isValidating( boolean newValue ) { isValidating = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public FileEntityResolver()
  {
    this( true ) ;
  } // FileEntityResolver()

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the setting if the parser is validating
   * the XML input. If true, this resolver must return an InputSource for a
   * URI pointing to a DTD file. If false this resolver must return null
   * for a URI pointing to a DTD file. 
   * 
   * @param validating Determines whether the resolver must return a resource for a DTD or not
   */
  public FileEntityResolver( boolean validating )
  {
    super() ;
    this.isValidating( validating ) ;
  } // FileEntityResolver()

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * If the given systemId specifies a file in the local file system an
   * InputSource with an opended FileInputStream on this file will be returned.
   * If the systemId contains a different protocol (e.g. http:// or ftp:// ) 
   * then null is returned.
   * 
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
  public InputSource resolveEntity( String publicId, String systemId)
		throws SAXException, IOException
	{
		InputSource source ;
		InputStream stream ;
		String filename ;
		File file ;
		
		if ( systemId == null )
			return null ;
			
		if ( ( ! this.isValidating() ) && this.isDTD( systemId ) )
		{	
			return super.resolveEntity( publicId, systemId ) ;
		}
		
		filename = this.extractFilename( systemId ) ;
		if ( filename == null )
			return null ;  // Let the parser open a URI connection
			
		file = new File( filename ) ;
		stream = new FileInputStream( file ) ;
		source = new InputSource( stream ) ;
		return source ;
	} // resolveEntity()
    
  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected boolean isDTD( String filename ) 
	{
  	String fName ;
  	
  	fName = filename.toUpperCase() ;
  	return fName.endsWith( DTD_EXTENSION ) ;
	} // isDTD()

  // -------------------------------------------------------------------------
  
	protected String extractFilename( String id )
	{
		String protocol ;

		protocol = this.str().prefix( id, "//" ) ;
		if ( protocol == null ) 
			return id ;
		
		if ( "file:".equals( protocol ) ) 
		{
			return this.str().suffix( id, "file://" ) ;
		}
		
		return null ;
	} // extractFilename()

	// -------------------------------------------------------------------------

	protected StringUtil str()
	{
		return StringUtil.current() ;
	} // str()

  // -------------------------------------------------------------------------

} // class FileEntityResolver
