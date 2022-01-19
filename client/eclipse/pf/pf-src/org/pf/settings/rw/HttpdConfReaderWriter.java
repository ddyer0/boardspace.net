// ===========================================================================
// CONTENT  : CLASS HttpdConfReaderWriter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 018/03/2005
// HISTORY  :
//  02/07/2003  mdu  CREATED
//	18/03/2005	mdu		changed	->	Added FileLocator support
//
// Copyright (c) 2003-2005, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.rw ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.IOException;
import java.io.InputStream;

import org.pf.file.FileLocator;
import org.pf.file.LineProcessor;
import org.pf.settings.Settings;
import org.pf.settings.impl.MultiValueSettingsImpl;
import org.pf.text.StringExaminer;
import org.pf.util.NamedText;

/**
 * This reader can read Apache httpd.conf files into a MultiValueSettings
 * object.
 * <p>
 * Storing such settings is not yet supported!
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class HttpdConfReaderWriter extends AbstractSettingsFileReaderWriter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public HttpdConfReaderWriter()
  {
    super() ;
  } // HttpdConfReaderWriter()
  
  // -------------------------------------------------------------------------
   
	/**
	 * Initialize the new instance with a file name.
	 */
	public HttpdConfReaderWriter( String filename )
	{
		super( filename ) ;
	} // HttpdConfReaderWriter()
  
	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a file locator.
	 */
	public HttpdConfReaderWriter( FileLocator locator )
	{
		super( locator ) ;
	} // HttpdConfReaderWriter()
  
	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/** 
	 * Stores the given settings to the file with this object's filename
	 * in the format of Windows ini files. 
	 * <H2>NOT YET IMPLEMENTED</H2>
	 * @param settings The settings to store
	 * @return true, if the settings have been successfully stored. Otherwise false.
	 */
	public boolean storeSettings( Settings settings ) 
	{
		return false ;
	} // storeSettings()
   
	// ------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/**
	 * Reads the content of a file in any intermediate object. That also can
	 * already be a valid Settings object.
	 */
	protected Object readFromStream( InputStream stream, Class settingsClass ) 
		throws IOException 
	{
		Settings settings 	= null ;

		settings = this.newSettings( settingsClass ) ;
		this.readLines( settings, stream ) ;
		return settings ;
	} // readFromStream()
  
	// ------------------------------------------------------------------------

	/**
	 * Reads all lines from the given streams and puts them into the given
	 * settings object
	 */
	protected void readLines( Settings settings, InputStream stream ) 
		throws IOException 
	{
		final Settings theSettings = settings ;
		LineProcessor processor ;
				
		processor = new LineProcessor()
			{
				String categoryName = null ;
				public boolean processLine( String textLine, int lineNo )
				{
					String line ;
					NamedText keyValue ;
					
					line = stripComment( textLine ) ;
					line = line.trim() ;
					if ( line.length() > 0 )
					{
						if ( line.startsWith( getSectionNameStartIndicator() ) )
						{
							if ( categoryName == null )
								categoryName = getCategoryName( line ) ;  // start tag
							else
								categoryName = null ;  // closing tag
						}
						else
						{
							keyValue = splitNameValue( line ) ;
							theSettings.setValueOf( categoryName, keyValue.name(), keyValue.text() ) ;
						}
					}
					return true ;			
				} // processLine()
			} ;
		
		this.fileUtil().processTextLines( stream, processor ) ;
	} // readLines()
    
	// ------------------------------------------------------------------------

	/**
	 * Converts the intermediate object read with 'readFromStream()' to a Settings
	 * object.
	 */
	protected Settings convertToSettings( Object result, Class settingsClass )
	{
		return (Settings)result ;
	} // convertToSettings()
 	
	// -------------------------------------------------------------------------

	protected NamedText splitNameValue( String line )
	{
		NamedText keyValue = null ;
		StringBuffer nameBuffer ;
		StringExaminer scanner ;
		char ch ;
		
		nameBuffer = new StringBuffer(20) ;
		scanner = new StringExaminer( line ) ;
		ch = scanner.nextChar() ;
		while ( ! ( ( ch == scanner.END_REACHED ) || Character.isWhitespace(ch) ) )
		{
			nameBuffer.append( ch ) ;
			ch = scanner.nextChar() ;
		}
		keyValue = new NamedText( nameBuffer.toString(), "" ) ;
		if ( ch != scanner.END_REACHED )
			keyValue.text( scanner.upToEnd().trim() ) ;
			
		return keyValue ;
	} // splitNameValue()
  
	// -------------------------------------------------------------------------

	protected String getCategoryName( String line )
	{
		return str().getDelimitedSubstring( line, this.getSectionNameStartIndicator(),
																							this.getSectionNameEndIndicator() ) ;
	} // getCategoryName()
    
	// ------------------------------------------------------------------------

	protected String stripComment( String line )
	{
		return str().upTo( line, this.getCommentSeparator() ) ;
	} // stripComment()
    
	// ------------------------------------------------------------------------

	protected String getSectionNameStartIndicator()
	{
		return "<" ;
	} // getSectionNameStartIndicator()
    
	// ------------------------------------------------------------------------

	protected String getSectionNameEndIndicator()
	{
		return ">" ;
	} // getSectionNameEndIndicator()
    
	// ------------------------------------------------------------------------

	protected String getCommentSeparator()
	{
		return "#" ;
	} // getCommentSeparator()
    
	// ------------------------------------------------------------------------

	protected Class getDefaultSettingsClass()
	{
		return MultiValueSettingsImpl.class ;
	} // getDefaultSettingsClass()
 
	// -------------------------------------------------------------------------
	
} // class HttpdConfReaderWriter
