// ===========================================================================
// CONTENT  : CLASS IniReaderWriter
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 10/11/2010
// HISTORY  :
//  30/05/2002  duma  CREATED
//	06/08/2002	duma	changed ->	readLines() exits if null line reached
//	18/12/2002	duma	added		->	Set the filename as the name of the read settings
//	04/07/2003	duma	changed	->	Using now a LineProcessor
//	18/03/2005	mdu		changed	->	Added FileLocator support
//	10/11/2010	mdu		added		->	support of escaping characters
//
// Copyright (c) 2002-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.rw ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import org.pf.file.FileLocator;
import org.pf.file.LineProcessor;
import org.pf.settings.Settings;
import org.pf.settings.impl.GenericSettingsImpl;
import org.pf.text.StringExaminer;
import org.pf.text.StringPattern;

/**
 * Read and writes settings from/into Windows ini files.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class IniReaderWriter extends AbstractSettingsFileReaderWriter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String DEFAULT_CATEGORY_NAME	= GenericSettingsImpl.DEFAULT_CATEGORY_NAME ;
	protected static final char DEFAULT_ESCAPE_CHAR	= '\\' ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String commentSeparator = ";" ;
	// -------------------------------------------------------------------------

  private String keyValueSeparator = "=" ;
  // -------------------------------------------------------------------------
  
  private boolean newlineAfterSection = true ;
  /**
   * Returns true if a new line gets added after each [section] 
   */
  public boolean getNewlineAfterSection() { return newlineAfterSection ; }
  /**
   * If set to true then a new line gets added after each [section] 
   */
  public void setNewlineAfterSection( boolean newValue ) { newlineAfterSection = newValue ; }
  
  // -------------------------------------------------------------------------
  
  private boolean supportEscaping = false ;
  /**
   * Returns true if escape character backslash ('\') is used to support special 
   * characters in values. 
   */
  public boolean isSupportingEscaping() { return supportEscaping ; }
  /**
   * Turns the supporting of backslash as escape character on or off.
   */
  public void setSupportEscaping( boolean newValue ) { supportEscaping = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a filename.
   * @param filename The name of the file to read from
   */
  public IniReaderWriter( String filename )
  {
    this( filename, null ) ;
  } // IniReaderWriter() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a filename and a character encoding.
   * 
   * 
   * @param filename The name of the file to read from
   * @param charsetName The character encoding (e.g. "UTF-8") or null
   */
  public IniReaderWriter( String filename, String charsetName )
  {
  	super( filename ) ;
  	this.setEncoding( charsetName ) ;
  } // IniReaderWriter() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a locator that points to an ini file.
   * 
   * @param locator A file locator that points to the file from which to read
   */
  public IniReaderWriter( FileLocator locator )
  {
    this( locator, null ) ;
  } // IniReaderWriter() 

  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a locator that points to an ini file and
   * a name for the character encoding of that file.
   * 
   * @param locator A file locator that points to the file from which to read
   * @param charsetName The character encoding (e.g. "UTF-8") or null
   */
  public IniReaderWriter( FileLocator locator, String charsetName )
  {
  	super( locator ) ;
  	this.setEncoding( charsetName ) ;
  } // IniReaderWriter() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the separator between a key and its associated value
   * The default is "="
   */
	public String getKeyValueSeparator()
	{
		return keyValueSeparator;
	} // getKeyValueSeparator() 

  // -------------------------------------------------------------------------
  
  /**
   * Sets the separator that is used between a key and its associated value
   * @param newValue The new separator (must be exactly one character!)
   * @throws IllegalArgumentException if the given value is not a string of length 1
   */
	public void setKeyValueSeparator(String newValue)
	{
		if ( newValue == null )
		{
			throw new IllegalArgumentException("The comment separator must not be null!");
		}
		if ( newValue.length() != 1 )
		{
			throw new IllegalArgumentException("The comment separator must contain only a single character");
		}
		keyValueSeparator = newValue;
	} // setKeyValueSeparator() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns the separator that indicates the beginning of a comment
	 * The default is ";"
	 */
	public String getCommentSeparator()
	{
		return commentSeparator;
	} // getCommentSeparator() 
	
	// -------------------------------------------------------------------------
	
  /**
   * Set the comment separator that indicates the start of a comment
   * @param newValue The new separator (must be exactly one character!)
   * @throws IllegalArgumentException if the given value is not a string of length 1
   */
	public void setCommentSeparator(String newValue)
	{
		if ( newValue == null )
		{
			throw new IllegalArgumentException("The comment separator must not be null!");
		}
		if ( newValue.length() != 1 )
		{
			throw new IllegalArgumentException("The comment separator must contain only a single character");
		}
		commentSeparator = newValue;
	} // setCommentSeparator() 


  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	/** 
	 * Stores the given settings to the file with this object's filename
	 * in the format of Windows ini files. 
	 * 
	 * @param settings The settings to store
	 * @return true, if the settings have been successfully stored. Otherwise false.
	 */
	public boolean storeSettings( Settings settings ) 
	{
		if ( this.getFileName() == null )
		{
			return false;
		}
		
		return this.writeSettings( this.getFileName(), settings ) ;
	} // storeSettings() 
  
	// ------------------------------------------------------------------------

	// ========================================================================
	// PROTECTED INSTANCE METHODS
	// ========================================================================
	/**
	 * Reads the values from the underlying ini file and returns them as
	 * settings.
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

	protected Settings convertToSettings( Object result, Class settingsClass ) 
	{
		return (Settings)result ;
	} // convertToSettings() 

	// -------------------------------------------------------------------------

	protected void readLines( Settings settings, InputStream stream )
		throws IOException
	{
		Reader reader ;
		
		reader = this.createReader( stream ) ;
		this.readLines( settings, reader ) ;
	} // readLines() 
  
	// ------------------------------------------------------------------------

	protected void readLines( Settings settings, Reader reader )
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
				String[] keyValue ;
				
				line = hanldeEscapingAndStripComment(textLine) ;
				line = line.trim() ;
				if ( line.length() > 0 )
				{
					if ( line.startsWith( getSectionStartIndicator() ) )
					{
						categoryName = getCategoryName( line ) ;
					}
					else
					{
						keyValue = str().splitNameValue( line, getKeyValueSeparator() ) ;
						theSettings.setValueOf( categoryName, keyValue[0], keyValue[1] ) ;
					}
				}
				return true ;			
			} // processLine()
		} ;
		
		this.fileUtil().processTextLines( reader, processor ) ;
	} // readLines() 
	
	// ------------------------------------------------------------------------

	protected boolean writeSettings( String filename, Settings settings )
	{
		OutputStream stream ;
		
		try
		{
			stream = new FileOutputStream( filename ) ;
		}
		catch ( FileNotFoundException e )
		{
			return false ;
		}
		try
		{
			this.writeSettings( stream, settings ) ;
		}
		catch ( IOException e )
		{
			return false ;
		}
		finally
		{
			this.fileUtil().close( stream ) ;
		}
		return true ;
	} // writeSettings() 
  
	// ------------------------------------------------------------------------

	protected void writeSettings( OutputStream stream, Settings settings ) throws IOException
	{
		Writer writer ;
		
		writer = this.createWriter( stream ) ;
		this.writeSettings( writer, settings ) ;
		writer.flush() ;
	} // writeSettings() 
	
	// ------------------------------------------------------------------------
	
	protected void writeSettings( Writer writer, Settings settings ) throws IOException 
	{
		String[] categoryNames ;
		
		categoryNames = settings.getCategoryNames() ;
		if ( this.str().contains( categoryNames, DEFAULT_CATEGORY_NAME ) )
		{
			categoryNames = this.str().copyWithout( categoryNames, new StringPattern(DEFAULT_CATEGORY_NAME) ) ;
			this.writeSettingsCategory( writer, settings, DEFAULT_CATEGORY_NAME ) ;
		}
		for (int i = 0; i < categoryNames.length; i++ )
		{
			this.writeSettingsCategory( writer, settings, categoryNames[i] ) ;
		}
	} // writeSettings() 
	
	// ------------------------------------------------------------------------
	
	protected void writeSettingsCategory( Writer writer, Settings settings, String category ) throws IOException 
	{
		String[] keys ;
		String value;
		
		if ( ! this.str().isNullOrEmpty( category ) )
		{
			writer.write( this.getSectionStartIndicator() ) ;
			writer.write( category ) ;
			writer.write( this.getSectionEndIndicator() ) ;			
			writer.write( NEWLINE ) ;
		}

		keys = settings.getKeyNamesOf( category ) ;
		for (int i = 0; i < keys.length; i++ )
		{
			writer.write( keys[i] ) ;
			writer.write( '=' ) ;
			value = settings.getValueOf( category, keys[i] );
			if ( this.isSupportingEscaping() )
			{
				value = this.escapeCharacters(value);
			}
			writer.write( value ) ;
			writer.write( NEWLINE ) ;
		}
		if ( this.getNewlineAfterSection() )
		{
			writer.write( NEWLINE ) ;				
		}
	} // writeSettingsCategory() 
	
	// -------------------------------------------------------------------------
	
	protected String escapeCharacters(String value)
	{
		StringBuffer buffer;
		StringExaminer stringExaminer;
		char ch;
		char commentSep;
		
		commentSep = this.getCommentSeparator().charAt(0);
		buffer = new StringBuffer(value.length());
		stringExaminer = new StringExaminer(value);
		while ( !stringExaminer.atEnd() )
		{
			ch = stringExaminer.nextChar();
			switch ( ch )
			{
				case '\t' :
					buffer.append(DEFAULT_ESCAPE_CHAR);
					buffer.append('t');
					break;
				case '\r' :
					buffer.append(DEFAULT_ESCAPE_CHAR);
					buffer.append('r');
					break;
				case '\n' :
					buffer.append(DEFAULT_ESCAPE_CHAR);
					buffer.append('n');
					break;
				case '\\' :
					buffer.append(DEFAULT_ESCAPE_CHAR);
					buffer.append('\\');
					break;
				default :
					if ( ch == commentSep )
					{
						buffer.append(DEFAULT_ESCAPE_CHAR);
					}
					buffer.append(ch);
					break;
			}
		}
		return buffer.toString();
	} // escapeCharacters()
	
	// ------------------------------------------------------------------------
	
	protected String getCategoryName( String line )
	{
		return str().getDelimitedSubstring( line, this.getSectionStartIndicator(),
																							this.getSectionEndIndicator() ) ;
	} // getCategoryName() 
  
	// ------------------------------------------------------------------------

	protected String hanldeEscapingAndStripComment(String line) 
	{
		boolean done = false;
		char ch;
		char commentStartChar;
		StringBuffer buffer;
		StringExaminer stringExaminer;

		if ( !this.isSupportingEscaping() )
		{
			return this.stripComment(line);
		}
		
		commentStartChar = this.getCommentSeparator().charAt(0);
		buffer = new StringBuffer(line.length());
		stringExaminer = new StringExaminer(line);
		
		while ( !done && !stringExaminer.atEnd() )
		{
			ch = stringExaminer.nextChar();
			if ( ch == DEFAULT_ESCAPE_CHAR )
			{
				if ( !stringExaminer.atEnd() )
				{
					ch = stringExaminer.nextChar();
					switch ( ch )
					{
						case 't' :
							buffer.append('\t');
							break;
						case 'r' :
							buffer.append('\r');
							break;
						case 'n' :
							buffer.append('\n');
							break;
						default :
							buffer.append(ch);
					}
				}
			}
			else
			{
				if ( ch == commentStartChar )
				{
					done = true;
				}
				else
				{
					buffer.append(ch);
				}
			}
		}
		return buffer.toString();
	} // hanldeEscapingAndStripComment() 
	
	// -------------------------------------------------------------------------
	
	protected String stripComment( String line )
	{
		return str().upTo( line, this.getCommentSeparator() ) ;
	} // stripComment() 
  
	// ------------------------------------------------------------------------

	protected String getSectionStartIndicator()
	{
		return "[" ;
	} // getSectionStartIndicator() 
  
	// ------------------------------------------------------------------------

	protected String getSectionEndIndicator()
	{
		return "]" ;
	} // getSectionEndIndicator() 
  
	// ------------------------------------------------------------------------

} // class IniReaderWriter 
