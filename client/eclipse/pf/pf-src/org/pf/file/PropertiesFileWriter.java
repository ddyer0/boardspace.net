// ===========================================================================
// CONTENT  : CLASS PropertiesFileWriter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 17/09/2004
// HISTORY  :
//  27/07/2004  mdu  CREATED
//	17/09/2004	mdu		added		-->	write( Writer, ..)
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Writes ordered properties with preserved comments and blank lines to a file.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class PropertiesFileWriter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final String NEWLINE = System.getProperty("line.separator") ;

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
  public PropertiesFileWriter()
  {
    super() ;
  } // PropertiesFileWriter()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Writes the given properties including all comments and blank lines
   * to a file with the specified name.
   * 
   * @param filename The name of the file to write to
   * @param properties The properties to write
   */
  public void writeTo( String filename, PropertiesFileContent properties )
  	throws IOException
	{
  	FileWriter writer ;
  	
  	writer = new FileWriter( filename ) ;
  	this.writeTo( writer, properties ) ;
	} // writeTo()

	// -------------------------------------------------------------------------
  
  /**
   * Writes the given properties including all comments and blank lines
   * to the given writer.
   * When the method is finished the writer is closed. That is also true
   * if an exception was thrown.
   * 
   * @param writer The writer object to write the properties to
   * @param properties The properties to write
   */
  public void writeTo( Writer writer, PropertiesFileContent properties )
  	throws IOException
	{
  	PropertiesFileElement element ;
  	
		try
		{
			for (int i = 0; i < properties.size(); i++ )
			{
				element = properties.elementAt(i) ;
				writer.write( element.getLine() ) ;
				writer.write( NEWLINE ) ;
			}
		}
		finally
		{
			this.fileUtil().close( writer ) ;
		}		
	} // writeTo()

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected FileUtil fileUtil() 
	{
		return FileUtil.current() ;
	} // fileUtil() 

	// -------------------------------------------------------------------------
	
} // class PropertiesFileWriter
