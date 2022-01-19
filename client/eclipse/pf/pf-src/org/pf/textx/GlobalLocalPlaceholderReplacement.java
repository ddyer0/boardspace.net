// ===========================================================================
// CONTENT  : CLASS GlobalLocalPlaceholderReplacement
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 01/07/2002
// HISTORY  :
//  01/07/2002  duma  CREATED
//
// Copyright (c) 2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Contains all variable settings and provides a mechanism, to replace
 * variable names in a text by their current value.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class GlobalLocalPlaceholderReplacement extends GlobalLocalVariables
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private TextEngine textEngine = null ;
  protected void setTextEngine( TextEngine newValue ) { textEngine = newValue ; }  
        
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public GlobalLocalPlaceholderReplacement()
  {
    super() ;
		this.setTextEngine( new TextEngine( this ) ) ;
  } // GlobalLocalPlaceholderReplacement()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

	/**
	 * Replaces all known variables in the given text and returns the
	 * new string.
	 */
	public String replace( String text )
		throws TextReplacementException
	{
		return this.getTextEngine().completeText( text ) ;
	} // replace()

  // -------------------------------------------------------------------------

	/**
	 * Returns the underlying text engine which might be useful to do further
	 * configuration settings.
	 */
	public TextEngine getTextEngine() 
	{ 
		return textEngine ; 
	} // getTextEngine() 

  // -------------------------------------------------------------------------

	/** 
	 * Returns the function resolver 
	 */
	public FunctionResolver getFunctionResolver() 
	{ 
		return this.getTextEngine().getFunctionResolver() ;
	} // getFunctionResolver()

  // -------------------------------------------------------------------------

	/** 
	 * Sets the function resolver 
	 */
	public void setFunctionResolver( FunctionResolver functionResolver ) 
	{ 
		this.getTextEngine().setFunctionResolver( functionResolver ) ; 
	} // setFunctionResolver()

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class GlobalLocalPlaceholderReplacement