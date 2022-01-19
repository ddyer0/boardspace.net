// ===========================================================================
// CONTENT  : CLASS LDIFReader
// AUTHOR   : M.Duchrow
// VERSION  : 1.1 - 21/01/2007
// HISTORY  :
//  23/04/2004    CREATED
//	21/01/2007	mdu		bugfix	-->	last line of a file was not processed if no empty line followed	
//
// Copyright (c) 2004-2007, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.directory.ldif ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.io.InputStream;

import org.pf.directory.ldap.LDAPDirEntry;
import org.pf.file.FileUtil;
import org.pf.osf.ObjectContainer;
import org.pf.util.Base64Converter;
import org.pf.util.NamedText;

/**
 * A reader for LDIF input
 *
 * @author M.Duchrow
 * @version 1.1
 */
public class LDIFReader extends LDIFLineProcessor
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private ObjectContainer allObjects = null ;
  protected ObjectContainer getAllObjects() { return allObjects ; }
  protected void setAllObjects( ObjectContainer newValue ) { allObjects = newValue ; }
  
  private LDAPDirEntry currentObject = null ;
  protected LDAPDirEntry getCurrentObject() { return currentObject ; }
  protected void setCurrentObject( LDAPDirEntry newValue ) { currentObject = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
	/**
	 * Returns a new instance of this class ;
	 */
	public static LDIFReader n() 
	{
		return new LDIFReader() ;
	} // n() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public LDIFReader()
  {
    super() ;
  } // LDIFReader() 

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Reads all entries from the LDIF file specified by the given filename.
   * Returns a List of LDAPDirEntry objects. 
   * 
   * @param filename The name of the LDIF file to read
   */
  public ObjectContainer read( String filename ) 
  	throws IOException
	{
		this.reset() ;
		this.futil().processTextLines( filename, this ) ;
		this.processingFinished() ;
		return this.getAllObjects() ;
	} // read() 

	// -------------------------------------------------------------------------
  
  /**
   * Reads all entries from the given LDIF stream.
   * Returns a List of LDAPDirEntry objects.
   * The passed in stream will be closed after execution! 
   * 
   * @param stream The stream from which to read the LDIF data
   */
  public ObjectContainer read( InputStream stream ) 
  	throws IOException
	{
		this.reset() ;
		this.futil().processTextLines( stream, this ) ;
		this.processingFinished() ;
		return this.getAllObjects() ;
	} // read() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * This method gets called after all lines are processed.
	 * Subclasses may override to do final work. That might include looking
	 * once more at the line buffer. If it is not empty its contents must
	 * be interpreted.
	 */
	protected void processingFinished() 
	{
		if ( this.getLineBuffer().length() > 0 )
		{
			this.processCompletedLine( this.getLineBuffer().toString() ) ;
			this.resetBuffer() ;
		}
	} // processingFinished()
	
	// -------------------------------------------------------------------------
	

	/* (non-Javadoc)
	 * @see org.pf.directory.ldif.LDIFLineProcessor#handleAttribute(org.pf.util.NamedText, boolean)
	 */
	protected boolean handleAttribute( NamedText keyValuePair, boolean encoded )
	{
		Object value ;
		
		if ( this.getCurrentObject() != null )
		{
			if ( encoded )
			{
				value = Base64Converter.decode( keyValuePair.text() ) ;
			}
			else
			{
				value = keyValuePair.text() ;
			}
			this.getCurrentObject().addValue( keyValuePair.name(), value ) ;
		}
		return true;
	} // handleAttribute() 
	
	// -------------------------------------------------------------------------
	
	/* (non-Javadoc)
	 * @see org.pf.directory.ldif.LDIFLineProcessor#handleComment(java.lang.String)
	 */
	protected boolean handleComment( String line )
	{
		// NOP
		return true ;
	} // handleComment() 
	
	// -------------------------------------------------------------------------
	
	/* (non-Javadoc)
	 * @see org.pf.directory.ldif.LDIFLineProcessor#handleEmptyLine()
	 */
	protected boolean handleEmptyLine()
	{
		// NOP
		return true;
	} // handleEmptyLine() 
	
	// ------------------------------------------------------------------------- 
	
	/* (non-Javadoc)
	 * @see org.pf.directory.ldif.LDIFLineProcessor#handleNewObject(org.pf.util.NamedText)
	 */
	protected boolean handleNewObject( NamedText objectId )
	{
		LDAPDirEntry entry ;
		
		entry = this.newEntry( objectId.text() ) ;
		this.setCurrentObject( entry ) ;
		this.getAllObjects().add( entry ) ;
		return true;
	} // handleNewObject() 
	
	// -------------------------------------------------------------------------
	
	protected LDAPDirEntry newEntry( String dn ) 
	{
		return new LDAPDirEntry( dn ) ;
	} // newEntry() 

	// -------------------------------------------------------------------------
	
	protected void reset() 
	{
		this.setAllObjects( this.newObjectContainer() ) ;
	} // reset() 

	// -------------------------------------------------------------------------
	
	protected ObjectContainer newObjectContainer() 
	{
		return this.newObjectContainer( 100 ) ;
	} // newObjectContainer() 

	// -------------------------------------------------------------------------
	
	protected ObjectContainer newObjectContainer( int initialSize ) 
	{
		return new ObjectContainer( initialSize ) ;
	} // newObjectContainer() 

	// -------------------------------------------------------------------------
	
	protected FileUtil futil() 
	{
		return FileUtil.current() ;
	} // futil() 

	// -------------------------------------------------------------------------
	
} // class LDIFReader 
