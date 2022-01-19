// ===========================================================================
// CONTENT  : CLASS MatchRulePrinter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 04/12/2003
// HISTORY  :
//  23/08/2002  duma  CREATED
//	27/12/2002	duma	changed	->	Support of new operators <,<=,>,>=
//	04/12/2003	duma	added		->	Support for ' enclosed values
//
// Copyright (c) 2002-2003, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Converts a MatchRule to a String using the default syntax.
 * The only necessary method to call is:<p>
 * asString( matchRule ) <p>
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class MatchRulePrinter implements MatchRuleVisitor
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================


  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private StringBuffer buffer = null ;
  protected StringBuffer getBuffer() { return buffer ; }
  protected void setBuffer( StringBuffer newValue ) { buffer = newValue ; }

  private MatchRuleChars ruleChars = new MatchRuleChars() ;
  protected MatchRuleChars getRuleChars() { return ruleChars ; }
  protected void setRuleChars( MatchRuleChars newValue ) { ruleChars = newValue ; }

  private boolean groupStarted = true ;
  protected boolean groupStarted() { return groupStarted ; }
  protected void groupStarted( boolean newValue ) { groupStarted = newValue ; }

  private boolean useNewSyntax = false ;
  /** 
   * Returns true, if the printer uses the new syntax which is using an equals 
   * character rather than the curly brackets for single values.   <br>
   * The default is NOT to use the new syntax to be compatible to old usage.
   */
  public boolean useNewSyntax() { return useNewSyntax ; }
  /** 
   * Sets if the printer uses the new syntax.
   */
  public void useNewSyntax( boolean newValue ) { useNewSyntax = newValue ; }  
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public MatchRulePrinter()
  {
    super() ;
  } // MatchRulePrinter()

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with match rule characters.
   */
  public MatchRulePrinter( MatchRuleChars ruleCharacters )
  {
    this() ;
    this.setRuleChars( ruleCharacters ) ;
  } // MatchRulePrinter()

  // -------------------------------------------------------------------------

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
  } // walkThroughFinished

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
    this.getBuffer().append( this.getGroupStartChar() ) ;
    this.groupStarted( true ) ;
  } // startGroup()

  // -------------------------------------------------------------------------

  /**
   * This method will be called for each group end occurence.
   */
  public void endGroup() 
  {
    this.getBuffer().append( this.getGroupEndChar() ) ;
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
      
		this.getBuffer().append( name ) ;
    
		if ( this.useSetSyntax( compareOperator, values ) )
		{
			this.appendValueList( values ) ;
		}
		else
		{
			switch ( compareOperator )
			{
				case MatchAttribute.OPERATOR_EQUALS :
					this.getBuffer().append( this.getRuleChars().getEqualsChar() ) ;
					break ;
				case MatchAttribute.OPERATOR_GREATER :
					this.getBuffer().append( this.getRuleChars().getGreaterChar() ) ;
					break ;
				case MatchAttribute.OPERATOR_LESS :
					this.getBuffer().append( this.getRuleChars().getLessChar() ) ;
					break ;
				case MatchAttribute.OPERATOR_GREATER_OR_EQUAL :
					this.getBuffer().append( this.getRuleChars().getGreaterChar() ) ;
					this.getBuffer().append( this.getRuleChars().getEqualsChar() ) ;
					break ;
				case MatchAttribute.OPERATOR_LESS_OR_EQUAL :
					this.getBuffer().append( this.getRuleChars().getLessChar() ) ;
					this.getBuffer().append( this.getRuleChars().getEqualsChar() ) ;
					break ;
			} 
			this.appendValue( values[0] ) ;
		}
		this.groupStarted( false ) ;
	} // attribute()                        	

  // -------------------------------------------------------------------------

	/**
	 * Returns the specified match rule as a string.
	 */
	public String asString( MatchRule matchRule )
	{
		String str ;
		
		if ( matchRule == null )
			return null ;
			
		matchRule.apply( this ) ;
    str = this.getBuffer().toString() ;

		if ( ! matchRule.getRootGroup().getNot() )    
	   	str = str.substring( 1, str.length() - 1 ) ;
    
    return str ;
	} // asString()

	// -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  protected void appendOperators( boolean andOperator, boolean notOperator )
  {
    if ( ! this.groupStarted() )
      this.getBuffer().append( andOperator ? this.getAndChar() : this.getOrChar() ) ;
      
    if ( notOperator )
      this.getBuffer().append( this.getNotChar() ) ;
  } // appendOperators()

  // -------------------------------------------------------------------------

	protected void appendValueList( String[] values )
	{
		this.getBuffer().append( this.getValueStartChar() ) ;
		for ( int i = 0 ; i < values.length ; i++ )
		{
			if ( i > 0 )
				this.getBuffer().append( this.getValueSeparatorChar() ) ;

			this.appendValue( values[i] ) ;
		}
		this.getBuffer().append( this.getValueEndChar() ) ;		
	} // appendValueList()

	// -------------------------------------------------------------------------

	protected void appendValue( String value )
	{
		if ( this.containsSpecialCharacter( value ) )
		{
			this.getBuffer().append( this.getRuleChars().getValueDelimiterChar() ) ;
			this.getBuffer().append( value ) ;
			this.getBuffer().append( this.getRuleChars().getValueDelimiterChar() ) ;
		}
		else
		{
			this.getBuffer().append( value ) ;
		}
	} // appendValue()

	// -------------------------------------------------------------------------

	protected boolean useSetSyntax(int compareOperator, String[] values)
	{
		if ( compareOperator == MatchAttribute.OPERATOR_EQUALS )
		{
			if ( ! this.useNewSyntax() ) 
				return true ;
				
			if ( values.length > 1 )
				return true ;
		}
		return false ;
	} // useSetSyntax()

	// -------------------------------------------------------------------------

  /** Returns the character for AND operations ( DEFAULT = '&' ) */
  protected char getAndChar()
  {
    return this.getRuleChars().getAndChar() ;
  } // getAndChar()

  // -------------------------------------------------------------------------

  /** Sets the character for AND operations */
  protected void setAndChar( char newValue )
  {
    this.getRuleChars().setAndChar( newValue );
  } // setAndChar()

  // -------------------------------------------------------------------------

  /** Returns the character for OR operations ( DEFAULT = '|' ) */
  protected char getOrChar()
  {
    return this.getRuleChars().getOrChar() ;
  } // getOrChar()

  // -------------------------------------------------------------------------

  /** Sets the character for OR operations */
  protected void setOrChar( char newValue )
  {
    this.getRuleChars().setOrChar( newValue );
  } // setOrChar()

  // -------------------------------------------------------------------------

  /** Returns the character for NOT operations ( DEFAULT = '!' ) */
  protected char getNotChar()
  {
    return this.getRuleChars().getNotChar() ;
  } // getNotChar()

  // -------------------------------------------------------------------------

  /** Sets the character for NOT operations */
  protected void setNotChar( char newValue )
  {
    this.getRuleChars().setNotChar( newValue );
  } // setNotChar()

  // -------------------------------------------------------------------------

  /** Returns the character for separation of values ( DEFAULT = ',' ) */
  protected char getValueSeparatorChar()
  {
    return this.getRuleChars().getValueSeparatorChar() ;
  } // getValueSeparatorChar()

  // -------------------------------------------------------------------------

  /** Sets the character that separates values in a value list */
  protected void setValueSeparatorChar( char newValue )
  {
    this.getRuleChars().setValueSeparatorChar( newValue ) ;
  } // setValueSeparatorChar()

  // -------------------------------------------------------------------------

  /** Returns the character that starts a list of values ( DEFAULT = '{' ) */
  protected char getValueStartChar()
  {
    return this.getRuleChars().getValueStartChar() ;
  } // getValueStartChar()

  // -------------------------------------------------------------------------

  /** Sets the character that starts a value list */
  protected void setValueStartChar( char newValue )
  {
    this.getRuleChars().setValueStartChar( newValue );
  } // setValueStartChar()

  // -------------------------------------------------------------------------

  /** Returns the character ends a list of values ( DEFAULT = '}' ) */
  protected char getValueEndChar()
  {
    return this.getRuleChars().getValueEndChar() ;
  } // getValueEndChar()

  // -------------------------------------------------------------------------

  /** Sets the character that ends a value list */
  protected void setValueEndChar( char newValue )
  {
    this.getRuleChars().setValueEndChar( newValue ) ;
  } // setValueEndChar()

  // -------------------------------------------------------------------------

  /** Returns the character that starts a logical group ( DEFAULT = '(' ) */
  protected char getGroupStartChar()
  {
    return this.getRuleChars().getGroupStartChar() ;
  } // getGroupStartChar()

  // -------------------------------------------------------------------------

  /** Sets the character that starts a group */
  protected void setGroupStartChar( char newValue )
  {
    this.getRuleChars().setGroupStartChar( newValue ) ;
  } // setGroupStartChar()

  // -------------------------------------------------------------------------

  /** Returns the character that ends a logical group ( DEFAULT = ')' ) */
  protected char getGroupEndChar()
  {
    return this.getRuleChars().getGroupEndChar() ;
  } // getGroupEndChar()

  // -------------------------------------------------------------------------

  /** Sets the character that ends a group */
  protected void setGroupEndChar( char newValue )
  {
    this.getRuleChars().setGroupEndChar( newValue ) ;
  } // setGroupEndChar()

  // -------------------------------------------------------------------------

	protected boolean containsSpecialCharacter( String value )
	{
		if ( value.indexOf( this.getValueSeparatorChar() ) >= 0 )
			return true ;

		if ( value.indexOf( this.getValueEndChar() ) >= 0 )
			return true ;

		if ( value.indexOf( this.getValueStartChar() ) >= 0 )
			return true ;

		if ( value.indexOf( this.getGroupStartChar() ) >= 0 )
			return true ;

		if ( value.indexOf( this.getGroupEndChar() ) >= 0 )
			return true ;

		if ( value.indexOf( this.getNotChar() ) >= 0 )
			return true ;

		if ( value.indexOf( this.getAndChar() ) >= 0 )
			return true ;

		if ( value.indexOf( this.getOrChar() ) >= 0 )
			return true ;

		if ( value.indexOf( this.getRuleChars().getEqualsChar() ) >= 0 )
			return true ;

		if ( value.indexOf( this.getRuleChars().getLessChar() ) >= 0 )
			return true ;

		if ( value.indexOf( this.getRuleChars().getGreaterChar() ) >= 0 )
			return true ;

		if ( value.indexOf( ' ' ) >= 0 )  // SPACE
			return true ;
			
		if ( value.indexOf( '\t' ) >= 0 )  // TAB
			return true ;
			
		if ( value.indexOf( '\n' ) >= 0 )  // NEW LINE
			return true ;
			
		if ( value.indexOf( '\r' ) >= 0 )  // RETURN
			return true ;
			
		return false ;
	} // containsSpecialCharacter()

	// -------------------------------------------------------------------------

} // class MatchRulePrinter