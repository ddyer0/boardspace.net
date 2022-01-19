// ===========================================================================
// CONTENT  : INTERFACE ExportProvider
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 07/06/2001
// HISTORY  :
//  07/06/2001  duma  CREATED
//
// Copyright (c) 2001, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Frame;

/**
 * Defines the interface that export classes must comply to.   <br>
 * The purpose of an ExportProvider is to write the inspector's displayed
 * object information to external sources (e.g. files).
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public interface ExportProvider
{
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Returns the version of the provider
	 */
  public String getVersion() ;

  // -------------------------------------------------------------------------

	/**
	 * Returns the vendor of the provider
	 */
  public String getVendor() ;

  // -------------------------------------------------------------------------

	/**
	 * An export method that allows the provider to show a
	 * dialog (e.g. file dialog) before actually writing the data.
	 * 
	 * @param objSpy The wrapped object to be exported
	 * @param parent The parent window. Usually that is the inspector window
	 */
  public boolean export( AbstractObjectSpy objSpy, Frame parent )
    throws Exception ;

  // -------------------------------------------------------------------------

	/**
	 * An export method that writes the data immediately to the 
	 * file with the specified name.
	 * 
	 * @param objSpy The wrapped object to be exported
	 * @param filename The name of the output file
	 */
  public boolean export( AbstractObjectSpy objSpy, String filename )
    throws Exception ;

  // -------------------------------------------------------------------------

	/**
	 * Returns the label that will be shown in an inspector's 'File' menu.
	 */
  public String exportLabel() ;

  // -------------------------------------------------------------------------

} // interface ExportProvider