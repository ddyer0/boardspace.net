// ===========================================================================
// CONTENT  : CLASS PropertiesRenderer
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 09/04/2004
// HISTORY  :
//  09/04/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.renderer;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.pf.joi.ObjectRenderer2;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * The purpose of this class is to render an instance of Properties to a JTable
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class PropertiesRenderer implements ObjectRenderer2
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final String[] COLUMN_NAMES = { "Key", "Value" } ;
	
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
  public PropertiesRenderer()
  {
    super() ;
  } // PropertiesRenderer() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * @see org.pf.joi.ObjectRenderer2#inspectComponent(java.lang.Object)
	 */
	public Component inspectComponent( Object obj )
	{
		Properties props ;
		JTable table ;
		JScrollPane scrollPane ;
		JPanel tablePanel ;
		String[][] data ;
		Iterator keys ;
		String key ;
		int row = 0 ;
		
		if ( obj instanceof Properties )
		{
			props = (Properties)obj ;
			data = new String[props.size()][2] ;
			keys = props.keySet().iterator() ;
			while ( keys.hasNext() )
			{
				key = (String) keys.next();
				data[row][0] = key ;
				data[row][1] = props.getProperty( key ) ;
				row++ ;
			}
			table = new JTable( data, COLUMN_NAMES ) ;
			scrollPane = new JScrollPane( table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
																		JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED ) ;
			tablePanel = new JPanel( new GridLayout( 1, 0 ) ) ;
			tablePanel.add( scrollPane ) ;
			return tablePanel;
		}
		return null ;
	} // inspectComponent()
	
	// -------------------------------------------------------------------------
	
	/**
	 * @see org.pf.joi.ObjectRenderer#inspectString(java.lang.Object)
	 */
	public String inspectString( Object obj )
	{
		return obj.toString() ;
	} // inspectString()
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class PropertiesRenderer 
