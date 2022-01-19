// ===========================================================================
// CONTENT  : CLASS NamedValues
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 02/07/2003
// HISTORY  :
//  02/07/2003  mdu  CREATED
//
// Copyright (c) 2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.settings.impl ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.List;

import org.pf.text.StringUtil;

/**
 * Hold multiple values under a specific name.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
 class NamedValues extends GenericNamedObject
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private List values = new ArrayList() ;
	protected List getValueList() { return values ; }
	protected void setValueList( List newValue ) { values = newValue ; }
	
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
	protected NamedValues(String aName, boolean checkCase)
	{
		super(aName, checkCase);
	} // NamedValues()
   
	// -------------------------------------------------------------------------
	
	protected NamedValues(String aName, String aValue, boolean checkCase)
	{
		super(aName, aValue, checkCase);
	} // NamedValues()
   
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected String[] getValues()
	{
		String[] array ;
		
		array = new String[this.getValueList().size()] ;
		return (String[])this.getValueList().toArray(array) ;
	} // getValues()
  
	// -------------------------------------------------------------------------
  
	/** 
	 * Set the value list to contain only the given values
	 * 
	 * @param someValues Must not be null
	 */
	protected void setValues(String[] someValues)
	{
		if ( someValues != null )
		{
			this.clearValueList() ;
			this.addValues( someValues ) ;
		}
	} // setValues()
  
	// -------------------------------------------------------------------------
	
	/* (non-Javadoc)
	 * @see org.pf.settings.impl.GenericNamedObject#getValue()
	 */
	protected String getValue()
	{
		if ( this.isEmpty() )
			return null;
			
		return (String)this.getValueList().get(0) ;
	} // getValue()
  
	// -------------------------------------------------------------------------
	
	/** 
	 * Adds the value to the value list
	 * 
	 * @param aValue Must not be null
	 */
	protected void setValue(String aValue)
	{
			this.addValue( aValue ) ;
	} // setValue()
  
	// -------------------------------------------------------------------------
	
	/**
	 * Add the given value to the value list, if it is not contained already 
	 */
	protected void addValue(String aValue)
	{
		if ( aValue != null )
		{
			if ( ! this.getValueList().contains( aValue ) )
				this.getValueList().add( aValue ) ;
		}
	} // addValue()
  
	// -------------------------------------------------------------------------
	
	protected void addValues(String[] someValues)
	{
		if ( someValues != null )
		{
			for (int i = 0; i < someValues.length; i++)
			{
				this.addValue( someValues[i] ) ;				
			}
		}
	} // addValues()
  
	// -------------------------------------------------------------------------

	protected void removeValue( String aValue )
	{
		if ( aValue != null )
			this.getValueList().remove( aValue ) ;
	} // removeValue()

	// -------------------------------------------------------------------------
	
	protected void removeValues( String[] someValues )
	{
		if ( someValues != null )
		{
			for (int i = 0; i < someValues.length; i++)
			{
				this.removeValue( someValues[i] ) ;				
			}
		}
	} // removeValues()

	// -------------------------------------------------------------------------
	
	protected void clearValueList()
	{
		this.setValueList( new ArrayList() ) ;
	} // clearValueList()
  
	// -------------------------------------------------------------------------
	
	protected boolean isEmpty()
	{
		return this.getValueList().isEmpty() ;
	} // isEmpty()
  
	// -------------------------------------------------------------------------
	
	// Rendering for JOI 
	protected String inspectString()
	{
		StringBuffer buffer = new StringBuffer( 100 ) ;
		
		buffer.append( "Name=\"" ) ;
		buffer.append( this.getName() ) ;
		buffer.append( "\"\nValues=[" ) ;
		buffer.append( StringUtil.current().asString( this.getValues()  ) ) ;
		buffer.append( "]" ) ;
		return buffer.toString() ; 
	} // inspectString()

	// -------------------------------------------------------------------------
		
} // class NamedValues
