// ===========================================================================
// CONTENT  : CLASS SqlMatchRuleVisitor
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 24/10/2003
// HISTORY  :
//  17/08/2001  duma  CREATED
//  12/11/2001  duma  changed ->  Redesign with inner class GroupInfo
//	27/12/2002	duma	changed	->	Support of new operators <,<=,>,>=
//	24/10/2003	duma	bugfix	->	Removed trailing space char in generated string
//							duma	added		->	Operator constants and IN() generation 
//
// Copyright (c) 2001-2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Walks over a MatchRule to create a SQL WHERE clause out of it.
 *
 * @author Manfred Duchrow
 * @version 1.3
 */
public class SqlMatchRuleVisitor implements MatchRuleVisitor
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  protected static final char MULTICHAR_WILDCARD = '%' ;
  
	protected static final String SQL_OPERATOR_NOT			= "NOT " ;
	protected static final String SQL_OPERATOR_AND			= "AND " ;
	protected static final String SQL_OPERATOR_OR				= "OR " ;
  protected static final String SQL_OPERATOR_IN				= "IN " ;
	protected static final String SQL_OPERATOR_LIKE			= "LIKE " ;
	protected static final String SQL_OPERATOR_EQUAL		= "= " ;
	protected static final String SQL_OPERATOR_GE				= ">= " ;
	protected static final String SQL_OPERATOR_LE				= "<= " ;
	protected static final String SQL_OPERATOR_GREATER 	= "> " ;
	protected static final String SQL_OPERATOR_LESS			= "< " ;
	protected static final String OPEN_PARENTHESIS			= "( " ;
	protected static final String CLOSE_PARENTHESIS			= ") " ;
	protected static final String COMMA									= ", " ;
	protected static final String START_QUOTE						= "'" ;
	protected static final String END_QUOTE							= "' " ;

  
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private StringBuffer buffer = null ;
  protected StringBuffer getBuffer() { return buffer ; }
  protected void setBuffer( StringBuffer newValue ) { buffer = newValue ; }

  private boolean groupStarted = true ;
  protected boolean groupStarted() { return groupStarted ; }
  protected void groupStarted( boolean newValue ) { groupStarted = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public SqlMatchRuleVisitor()
  {
    super() ;
  } // SqlMatchRuleVisitor()
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * This method will be called right before the MatchRule walks
   * through its elements.
   */
  public void walkThroughInit()
  {
    this.setBuffer( new StringBuffer(100) );
    this.groupStarted( true ) ;
  } // walkThroughInit()
  
  // -------------------------------------------------------------------------

  /**
   * This method will be called when the MatchRule has finished to walk
   * through its elements.
   */
  public void walkThroughFinished()
  {
    // NOP
  } // walkThroughFinished()
  
  // -------------------------------------------------------------------------

  /**
   * This method will be called for each start of a new group.
   *
   * @param andOperator If true it is an AND combination otherwise it is OR
   * @param notOperator Is only true for a NOT operation
   */
  public void startGroup( boolean andOperator, boolean notOperator )
  {
    this.appendOperators( andOperator, notOperator ) ;
    this.getBuffer().append( OPEN_PARENTHESIS ) ;
    this.groupStarted( true ) ;
  } // startGroup()
  
  // -------------------------------------------------------------------------

  /**
   * This method will be called for each group end occurence.
   */
  public void endGroup()
  {
    this.getBuffer().append( CLOSE_PARENTHESIS ) ;
    this.groupStarted( false ) ;
  } // endGroup()
  
  // -------------------------------------------------------------------------

  /**
   * This method will be called for each attribute.
   *
   * @param name The attribute's name
   * @param compareOperator The operator used to compare values
   * @param values All values the attrubute my match (implicit OR combination !)
   * @param andOperator If true it is an AND combination otherwise it is OR
   * @param notOperator Is only true for a NOT operation
   */
  public void attribute( String name, int compareOperator, String[] values,
                          boolean andOperator, boolean notOperator )
  {
    this.appendOperators( andOperator, notOperator ) ;

    if ( this.canBeInOperator( compareOperator, values ) )
    {
    	this.append_IN( name, values ) ;
    }
    else
    {
    	this.appendAttributeValues( name, compareOperator, values ) ;
    }

    this.groupStarted( false ) ;
  } // attribute()
  
  // -------------------------------------------------------------------------

  /**
   * Converts the given match rule into a SQL conditional clause.
   *
   * @param matchRule The rule to be converted
   */
  public String asSqlClause( MatchRule matchRule )
  {
    matchRule.apply( this ) ;
    return this.getBuffer().toString().trim() ;
  } // asSqlClause()
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  protected void appendOperators( boolean andOperator, boolean notOperator )
  {
    if ( ! this.groupStarted() )
      this.getBuffer().append( andOperator ? SQL_OPERATOR_AND : SQL_OPERATOR_OR ) ;
      
    if ( notOperator )
      this.getBuffer().append( SQL_OPERATOR_NOT ) ;
  } // appendOperators()
  
  // -------------------------------------------------------------------------

  protected void appendAttribute( String name, int compareOperator, String value )
  {
    this.getBuffer().append( name ) ;
    this.getBuffer().append( ' ' ) ;
    if ( compareOperator == MatchAttribute.OPERATOR_EQUALS )
    {
	    if ( value.indexOf( StringPattern.DEFAULT_MULTICHAR_WILDCARD ) >= 0 )
	    {
	      value = value.replace( StringPattern.DEFAULT_MULTICHAR_WILDCARD, MULTICHAR_WILDCARD ) ;
	      this.getBuffer().append( SQL_OPERATOR_LIKE ) ;
	    }
	    else
	    {
	      this.getBuffer().append( SQL_OPERATOR_EQUAL ) ;
	    }
    }
    else
    {
    	switch ( compareOperator )
    	{
    		case MatchAttribute.OPERATOR_GREATER :
    			this.getBuffer().append( SQL_OPERATOR_GREATER ) ;
    			break ;
    		case MatchAttribute.OPERATOR_LESS :
    			this.getBuffer().append( SQL_OPERATOR_LESS ) ;
    			break ;
    		case MatchAttribute.OPERATOR_GREATER_OR_EQUAL :
    			this.getBuffer().append( SQL_OPERATOR_GE ) ;
    			break ;
    		case MatchAttribute.OPERATOR_LESS_OR_EQUAL :
    			this.getBuffer().append( SQL_OPERATOR_LE ) ;
    			break ;
    	} 
    }
    this.appendValue( value ) ;
  } // appendAttribute()
  
  // -------------------------------------------------------------------------

	protected void appendValue( String value )
	{
		this.getBuffer().append( START_QUOTE ) ;
		this.getBuffer().append( value ) ;
		this.getBuffer().append( END_QUOTE ) ;
	} // appendValue()

	// -------------------------------------------------------------------------

	protected void appendAttributeValues( String name, int compareOperator, String[] values )
	{
		boolean manyValues ;

		manyValues = values.length > 1 ;

		if ( manyValues ) 
			this.getBuffer().append( OPEN_PARENTHESIS ) ;

		this.appendAttribute( name, compareOperator, values[0] ) ;
		for ( int i = 1 ; i < values.length ; i++ )
		{
			this.getBuffer().append( SQL_OPERATOR_OR ) ;
			this.appendAttribute( name, compareOperator, values[i] ) ;
		}		
		if ( manyValues ) 
			this.getBuffer().append( CLOSE_PARENTHESIS ) ;
	} // appendAttributeValues()
  
	// -------------------------------------------------------------------------

	protected void append_IN( String attrName, String[] values )
	{
		this.getBuffer().append( attrName ) ;
		this.getBuffer().append( ' ' ) ;
		this.getBuffer().append( SQL_OPERATOR_IN ) ;
		this.getBuffer().append( OPEN_PARENTHESIS ) ;
		for ( int i = 0 ; i < values.length ; i++ )
		{
			if ( i > 0 )
				this.getBuffer().append( COMMA ) ;
				
			this.appendValue( values[i] ) ;
		}		
		this.getBuffer().append( CLOSE_PARENTHESIS ) ;
	} // append_IN()
  
	// -------------------------------------------------------------------------

	protected boolean canBeInOperator( int compareOperator, String[] values )
	{	
		if ( compareOperator != MatchAttribute.OPERATOR_EQUALS )
			return false ;
			
		if ( values.length < 2 )
			return false ;
			
		for (int i = 0; i < values.length; i++)
		{
			if ( values[i].indexOf( StringPattern.DEFAULT_MULTICHAR_WILDCARD ) >= 0 ) 
				return false ;

			if ( values[i].indexOf( StringPattern.DEFAULT_SINGLECHAR_WILDCARD ) >= 0 ) 
				return false ;
		}
		
		return true ;
	} // canBeInOperator()

	// -------------------------------------------------------------------------

} // class SqlMatchRuleVisitor