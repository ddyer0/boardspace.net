// ===========================================================================
// CONTENT  : CLASS StringArrayRenderer
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 15/04/2006
// HISTORY  :
//  15/04/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi.renderer ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.pf.joi.ObjectRenderer2;
import org.pf.text.StringUtil;

/**
 * Displays a string array as table with two columns
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class StringArrayRenderer implements ObjectRenderer2
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final String[] COLUMN_NAMES = { "Index", "String" } ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================


  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public StringArrayRenderer()
  {
    super() ;
  } // StringArrayRenderer()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * @see org.pf.joi.ObjectRenderer2#inspectComponent(java.lang.Object)
	 */
	public Component inspectComponent( Object obj )
	{
		String[] strings ;
		JTable table ;
		JScrollPane scrollPane ;
		JPanel tablePanel ;
		String[][] data ;
		
		if ( obj instanceof String[] )
		{
			strings = (String[])obj ;
			data = new String[strings.length][2] ;
			for (int i = 0; i < strings.length; i++ )
			{
				data[i][0] = Integer.toString(i) ;
				data[i][1] = strings[i] ;
			}
			table = new JTable( data, COLUMN_NAMES ) ;
			table.getColumn( COLUMN_NAMES[0] ).setMaxWidth( 60 ) ;
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
		if ( obj instanceof String[] )
		{
			return StringUtil.current().asString( (String[])obj, "\n" ) ;
		}
		return null ;
	} // inspectString()
	
	// -------------------------------------------------------------------------
	

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class StringArrayRenderer
