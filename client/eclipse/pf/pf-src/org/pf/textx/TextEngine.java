// ===========================================================================
// CONTENT  : CLASS TextEngine
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.7.2 - 16/08/2007
// HISTORY  :
//  25/06/1999 	duma  CREATED
//	03/10/1999	duma	renamed	-> 	from TextCompletioner
//	09/12/1999	duma	added		-> 	indentation support
//	25/01/2000	duma	moved		-> 	from package 'com.mdcs.text'
//	25/01/2000	duma	changed	->	refactored
//	25/01/2000	duma	added		->	control elements #IF(), #ELSE, #ENDIF
//  01/02/2000  duma  changed ->  Getter/Setter for resolver are public now
//  07/02/2000  duma  added   ->  control element #IFDEF
//	24/03/2002	duma	added		->	support of special name characters and replacable delimiters
//	24/10/2002	duma	added		->	allowMissingPlaceholders(), useDollarCurlyBrackets()
//	06/03/2004	duma	changed	->	all constants to be protected static final and not private final
//	16/08/2007	mdu		changed	->	cleanup style, use StringBuffer
//
// Copyright (c) 1999-2007, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;

// ===========================================================================
// IMPORTS
// ===========================================================================


import org.pf.util.Bool ;
import org.pf.text.StringUtil ;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.Character;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.Vector;

/**
 * Instances of this class are able to replace 
 * all variables by their values in a given text
 *
 * @author Manfred Duchrow
 * @version 1.7.2
 */ 
public class TextEngine
{
	// =======================================================================
	// CONSTANTS
	// =======================================================================
  protected static final Character DEFAULT_VAR_START_DELIMITER	= Character.valueOf( '%' ) ;
  protected static final Character DEFAULT_VAR_END_DELIMITER	= Character.valueOf( '%' ) ;
  protected static final Character DEFAULT_TEXT_DELIMITER    	= Character.valueOf( '\'' ) ;
  protected static final Character DEFAULT_PARAM_SEPARATOR   	= Character.valueOf( ',' ) ;
  protected static final Character DEFAULT_FUNCPARAM_START   	= Character.valueOf( '(' ) ;
  protected static final Character DEFAULT_FUNCPARAM_END     	= Character.valueOf( ')' ) ;
  protected static final Character CONTROL_INDICATOR			= Character.valueOf( '#' ) ;
  
  protected static final String CONTROL_IF											= "IF" ;
  protected static final String CONTROL_ELSE										= "ELSE" ;
  protected static final String CONTROL_ENDIF									= "ENDIF" ;
  protected static final String CONTROL_IFDEF									= "IFDEF" ;

	protected static final int EMPTY_CACHE											= -99 ;

	// =======================================================================
	// INSTANCE VARIABLES
	// =======================================================================
	private VariableResolver variableResolver = null ;
	/** Returns the variable resolver */
	public VariableResolver getVariableResolver() { return variableResolver ; }
	/** Sets the variable resolver */
	public void setVariableResolver( VariableResolver vr ) { variableResolver = vr ; }

	private FunctionResolver functionResolver = null ;
	/** Returns the function resolver */
	public FunctionResolver getFunctionResolver() { return functionResolver ; }
	/** Sets the function resolver */
	public void setFunctionResolver( FunctionResolver fr ) { functionResolver = fr ; }

	private StringReader sourceBuffer = null ;
	protected StringReader getSourceBuffer() { return sourceBuffer ; }
	protected void setSourceBuffer( StringReader sb ) { sourceBuffer = sb ; }

	private StringWriter resultBuffer = null ;
	protected StringWriter getResultBuffer() { return resultBuffer ; }
	protected void setResultBuffer( StringWriter rb ) { resultBuffer = rb ; }

	private Character nextChar = null ;
	protected Character getNextChar() { return nextChar ; }
	protected void setNextChar( Character ch ) { nextChar = ch ; }

	private boolean nextCharFilled = false ;
	protected boolean getNextCharFilled() { return nextCharFilled ; }
	protected void setNextCharFilled( boolean flag ) { nextCharFilled = flag ; }

  private int charCache = EMPTY_CACHE ;
  protected int getCharCache() { return charCache ; }
  protected void setCharCache( int newValue ) { charCache = newValue ; }

  private Stack executionControl = new Stack() ;
  protected Stack getExecutionControl() { return executionControl ; }
  protected void setExecutionControl( Stack newValue ) { executionControl = newValue ; }

  private boolean supressMissingVariableException = false ;
  protected boolean supressMissingVariableException() { return supressMissingVariableException ; }
  protected void supressMissingVariableException( boolean newValue ) { supressMissingVariableException = newValue ; }
  
  private Character varStartPrefix = null ;
  /**
   * Returns the character that is specified as prefix of the placeholder
   * delimiter or null, if none is used.
   * <br>
   * Example: 
   *  %varName% -> Has no prefix, this method returns null  <br>
   *  ${varName} -> Uses '$' as prefix, this method returns Character('$')
   */
  public Character getVarStartPrefix() { return varStartPrefix ; }
  /**
   * Sets the character that is is used as prefix of the placeholder
   * start delimiter.  <br>
   * Set to null, if no prefix is desired.
   * 
   * @param prefix The prefix character of null to use none.
   */
  public void setVarStartPrefix( Character prefix ) { varStartPrefix = prefix ; }  
  
  private Character varStartDelimiter = DEFAULT_VAR_START_DELIMITER ;
  /**
   * Returns the delimiter that marks the start of a variable
   */
  public Character getVarStartDelimiter() { return varStartDelimiter ; }
  /**
   * Sets the delimiter that marks the start of a variable
   */
  public void setVarStartDelimiter( Character newValue ) { varStartDelimiter = newValue ; }
  
  private Character varEndDelimiter = DEFAULT_VAR_END_DELIMITER ;
  /**
   * Returns the delimiter that marks the end of a variable
   */
  public Character getVarEndDelimiter() { return varEndDelimiter ; }
  /**
   * Sets the delimiter that marks the end of a variable
   */
  public void setVarEndDelimiter( Character newValue ) { varEndDelimiter = newValue ; }  
  
	private String indentationFiller = null ;
	protected String getIndentationFiller() { return indentationFiller ; }
	protected void setIndentationFiller( String newValue ) { indentationFiller = newValue ; }

  private String specialNameCharacters = "" ;
  /**
   * Returns all extra charcters, that are allowed in placeholder names.
   * <br>
   * By default only [A-Z][a-z][0-9] and '_' are allowed.
   */
  public String getSpecialNameCharacters() { return specialNameCharacters ; }
  /**
   * Sets all extra charcters, that are allowed in placeholder names.
   * <br>
   * By default only [A-Z][a-z][0-9] and '_' are allowed.
   */
  public void setSpecialNameCharacters( String newValue ) { specialNameCharacters = newValue ; }
                    
	// =======================================================================
	// CONSTRUCTOR
	// =======================================================================

  /**
   * Initializes the new created instance with the given objects
   * for resolving variable and function names to values.
   *
   * @param varResolver The object that provides values for variables
   * @see #setVariableResolver( VariableResolver )
   */
	public TextEngine( VariableResolver varResolver )
	{
		this.setVariableResolver( varResolver ) ;
	} // TextEngine()

	// -----------------------------------------------------------------------

  /**
   * Initializes the new created instance with the given objects
   * for resolving variable and function names to values.
   *
   * @param varResolver The object that provides values for variables
   * @param funcResolver The object that provides values for function calls
   * @see #setVariableResolver( VariableResolver )
   * @see #setFunctionResolver( FunctionResolver )
   */
	public TextEngine(  VariableResolver varResolver,
                      FunctionResolver funcResolver )
	{
		this( varResolver ) ;
		this.setFunctionResolver( funcResolver ) ;
	} // TextEngine()

	// =======================================================================
	// PUBLIC INSTANCE METHODS
	// =======================================================================
	/**
	 * Returns the given text, after replaceing all placeholders with according text.
	 *
	 * @param text The input text containing placeholders.
	 * @return the given input text with all placeholders replaced
	 */
	public String completeText( String text )
				 throws TextReplacementException
	{
		this.initializeBuffers( text ) ;

		this.copyText() ;

		return this.resultString() ;
	} // completeText()

	// -----------------------------------------------------------------------
	
	/**
	 * Sets the number of spaces a text must be indented.    <br>
	 *
	 * @param indentSize the number of indentation spaces ( 0 means no indentation )
	 */
	public void setIndentation( int indentSize )
	{
		if ( indentSize <= 0 )
		{
			this.setIndentationFiller( null ) ;
		}
		else
		{
			this.setIndentationFiller( this.strUtil().repeat( StringUtil.CH_SPACE, indentSize ) ) ;
		}
	} // setIndentation()

	// -----------------------------------------------------------------------

	/**
	 * Sets the character that indicates the start of a placeholder or
	 * directive to the specified value.
	 */
  public void setVarStartDelimiter( char newValue ) 
  { 
  	this.setVarStartDelimiter( Character.valueOf( newValue ) ) ; 
  } // setVarStartDelimiter()

	// -----------------------------------------------------------------------

	/**
	 * Sets the character that indicates the end of a placeholder or
	 * directive to the specified value.
	 */
  public void setVarEndDelimiter( char newValue ) 
  { 
  	this.setVarEndDelimiter( Character.valueOf( newValue ) ) ; 
  } // setVarEndDelimiter()

	// -----------------------------------------------------------------------

	/**
	 * Sets the delimiters to ${}
	 * <p>
	 * Placeholders then must look like ${varName}.
	 */
	public void useDollarCurlyBrackets()
	{
		this.setVarStartPrefix( Character.valueOf('$') ) ;
		this.setVarStartDelimiter( Character.valueOf( '{' ) ) ;
		this.setVarEndDelimiter( Character.valueOf( '}' ) ) ;
	} // useDollarCurlyBrackets()

	// -------------------------------------------------------------------------

	/**
	 * Allows placeholders that cannot be resolved by the variable resolver.
	 * That means, that the placeholder's name itself will be placed in
	 * the text.
	 * <br>
	 * The default behaviour of a TextEngine is to throw an exception, if 
	 * a placeholder can't be resolved.
	 */
	public void allowMissingPlaceholders()
	{
		this.supressMissingVariableException(true) ;
	} // allowMissingPlaceholders()

	// -------------------------------------------------------------------------

	/**
	 * Forbids placeholders that cannot be resolved by the variable resolver.
	 * That means, that an exception will be thrown, if a placeholder can't
	 * be resolved.
	 * <br>
	 * The default behaviour of a TextEngine is to throw an exception, if 
	 * a placeholder can't be resolved.
	 */
	public void forbidMissingPlaceholders()
	{
		this.supressMissingVariableException(false) ;
	} // forbidMissingPlaceholders()

	// -------------------------------------------------------------------------

	// =======================================================================
	// PROTECTED INSTANCE METHODS
	// =======================================================================
	protected void initializeResultBuffer( int initialCapacity )
	{
		this.setResultBuffer( new StringWriter( initialCapacity ) ) ;
		if ( this.isIndentationOn() )
		{
			this.getResultBuffer().write( this.getIndentationFiller() ) ;
		}
	} // initializeResultBuffer()

	// -----------------------------------------------------------------------

	protected void initializeSourceBuffer( String source )
	{
		this.setSourceBuffer( new StringReader( source ) ) ;
	} // initializeSourceBuffer()

	// -----------------------------------------------------------------------

	protected void initializeBuffers( String text )
	{
		this.initializeResultBuffer( text.length() ) ;
		this.initializeSourceBuffer( text ) ;		
	} // initializeBuffers()

	// -----------------------------------------------------------------------

	protected void copyText()
						throws TextReplacementException
	{
		Character ch = null ;

		while ( this.hasMoreChars() )
		{
			ch = this.nextCharacter() ;
			if ( isInsertionStart(ch) )
			{
				this.writeToResultBuffer( this.getPlaceholderValue() ) ;
			}
			else
			{
				this.writeToResultBuffer( ch.charValue() ) ;
				if ( ( this.isIndentationOn() ) && ( ch.charValue() == '\n' ) )
				{
					 this.writeToResultBuffer( this.getIndentationFiller() ) ;
				}
			}
		} // while
	} // copyText()

	// -----------------------------------------------------------------------

	protected boolean isInsertionStart(Character ch)
	{
		if ( this.hasDelimiterPrefix() )
		{
			if ( ch.equals( this.getVarStartPrefix() ) )
			{
				ch = this.nextCharacter() ;
				if ( ch.equals( this.getVarStartDelimiter() ) )
				{
					return true ;
				}
				else
				{
					this.putBackNextChar() ;
				}
			}
			return false ;
		}
		return ch.equals( this.getVarStartDelimiter() );
	} // isInsertionStart()

	// -----------------------------------------------------------------------

	protected boolean hasDelimiterPrefix()
	{
		return this.getVarStartPrefix() != null ;
	} // hasDelimiterPrefix()

	// -------------------------------------------------------------------------

	protected void writeToResultBuffer( String text )
	{
		if ( this.isWritingOn() )
		{
			this.getResultBuffer().write( text ) ;
		}
	} // writeToResultBuffer()

	// -----------------------------------------------------------------------

	protected void writeToResultBuffer( char ch )
	{
		if ( this.isWritingOn() )
		{
			this.getResultBuffer().write( ch ) ;
		}
	} // writeToResultBuffer()

	// -----------------------------------------------------------------------

	protected String getPlaceholderName()
	{
		StringBuffer buffer ;
		Character ch ;
		
		buffer = new StringBuffer(40) ;
		while ( this.hasMoreChars() )
		{
			ch = this.nextCharacter() ;
			if ( this.isValidPlaceholderNameCharacter( ch.charValue() ) )
			{
				buffer.append( ch ) ;
			}
			else
			{
				this.putBackNextChar() ;
				break ;
			}
		}  // while
				
		return buffer.toString() ;
	} // getPlaceholderName()

	// -----------------------------------------------------------------------

	protected String getPlaceholderValue()
						throws TextReplacementException
	{
		Character ch              = null ;
		String  placeholderName   = null ;

		if ( ! this.hasMoreChars() )
		{
			throw new TextReplacementException( "Unexpected end of string reached !" ) ;
		}

		ch = this.nextCharacter() ;
		
		// Duplication of start delimiter will produce the delimiter itself
		if ( ch.equals( this.getVarStartDelimiter() ) )
		{
			return this.getVarStartDelimiter().toString() ;
		}

		if ( ch.equals( this.getControlIndicator() ) )
		{
			return this.evaluateControl() ;
		}

		this.putBackNextChar() ;

		placeholderName = this.getPlaceholderName() ;

		ch = this.nextCharacter() ;

		if ( ch.equals( this.getVarEndDelimiter() ) )
		{
			return this.evaluatePlaceholder( placeholderName ) ;
		}

		if ( ch.equals( this.getFunctionParameterStart() ) )
		{
			return this.evaluateFunction( placeholderName ) ;
		}

		throw ( new TextReplacementException( "Invalid termination of placeholder: "
																					+ placeholderName )  ) ;
	} // getPlaceholderValue()

	// -----------------------------------------------------------------------

	protected void checkEndAfterFunction( String funcName )
				throws TextReplacementException
	{
		Character ch	= null ;
		String msg		= null ;

		ch = this.skipSpaces() ;
		if ( ! ch.equals( this.getVarEndDelimiter() ) )
		{
			msg = "Invalid character '" + ch + "' after '" + funcName + "' !" ;
			throw new TextReplacementException( msg ) ;
		}
	} // checkEndAfterFunction()

	// -----------------------------------------------------------------------

  protected void checkFunctionStart( String funcName )
				throws TextReplacementException
  {
		Character ch				= null ;
		String msg					= null ;

		ch = this.skipSpaces() ;
		if ( ! ch.equals( this.getFunctionParameterStart() ) )
		{
			msg = "Missing opening bracket in '" + funcName + "' ! " ;
			throw new TextReplacementException( msg ) ;
		}
	} // checkFunctionStart()

	// -----------------------------------------------------------------------

  protected void checkFunctionEnd( String funcName )
				throws TextReplacementException
  {
		Character ch				= null ;
		String msg					= null ;

		ch = this.skipSpaces() ;
		if ( ! ch.equals( this.getFunctionParameterEnd() ) )
		{
			msg = "Invalid end '" + ch + "' of " + funcName ;
			throw new TextReplacementException( msg ) ;
		}
	} // checkFunctionEnd()

	// -----------------------------------------------------------------------

  protected void checkParameterCount( String funcName, int expected, int actual )
				throws TextReplacementException
  {
		String msg					= null ;

		if ( actual != expected )
		{
			msg = "The number of parameters in " + funcName + " must be " ;
      msg = msg + Integer.toString( expected ) + " !" ;
			throw new TextReplacementException( msg ) ;
		}
	} // checkParameterCount()

	// -----------------------------------------------------------------------

	protected String evaluateControlIF()
				throws TextReplacementException
	{
		final String IF_CTRL = CONTROL_IF
				+ this.getFunctionParameterStart() + this.getFunctionParameterEnd() ;
		Vector parameters ;

    this.checkFunctionStart( IF_CTRL );
		parameters = this.getFunctionParameters() ;
    this.checkFunctionEnd( IF_CTRL );
		this.checkEndAfterFunction( IF_CTRL ) ;
		this.checkParameterCount( IF_CTRL, 1, parameters.size() ) ;

		if ( Bool.get( (String)parameters.get(0) ).isTrue() )
		{
			this.switchWritingOn() ;
		}
		else
		{
			this.switchWritingOff() ;
		}

		return "" ;
	} // evaluateControlIF()

	// -----------------------------------------------------------------------

	protected String evaluateControlIFDEF()
				throws TextReplacementException
	{
		final String IFDEF_CTRL = CONTROL_IFDEF
				+ this.getFunctionParameterStart() + this.getFunctionParameterEnd() ;
		String varName 	        = null ;

    this.checkFunctionStart( IFDEF_CTRL );
		varName = this.getFunctionParameterName() ;
    this.checkFunctionEnd( IFDEF_CTRL );
		this.checkEndAfterFunction( IFDEF_CTRL ) ;

    if (  ( this.isDefinedPlaceholder( varName ) ) &&
          ( this.evaluatePlaceholder( varName ).length() > 0 )
        )
    {
 			this.switchWritingOn() ;
    }
		else
    {
			this.switchWritingOff() ;
    }
		return "" ;
	} // evaluateControlIFDEF()

	// -----------------------------------------------------------------------

	protected String evaluateControlELSE()
				throws TextReplacementException
	{
		this.checkEndAfterFunction( CONTROL_ELSE ) ;

		try
		{
			this.toggleWritingSwitch() ;
		}
		catch ( EmptyStackException ex )
		{
			throw new TextReplacementException( "Unmatched " + CONTROL_ELSE + " reached !" ) ;
		}				

		return "" ;
	} // evaluateControlELSE()

	// -----------------------------------------------------------------------

	protected String evaluateControlENDIF()
				throws TextReplacementException
	{
		this.checkEndAfterFunction( CONTROL_ENDIF ) ;
		
		try
		{
			this.removeLastWritingSwitch() ;
		}
		catch ( EmptyStackException ex )
		{
			throw new TextReplacementException( "Unmatched " + CONTROL_ENDIF + " reached !" ) ;
		}				
		return "" ;
	} // evaluateControlENDIF()

	// -----------------------------------------------------------------------

	protected String evaluateControl()
				throws TextReplacementException
	{
		String controlName		= null ;
		String msg						= null ;
		
		controlName = this.getPlaceholderName() ;

		if ( controlName.equals( CONTROL_IF ) )
		{
			return this.evaluateControlIF() ;
		}

		if ( controlName.equals( CONTROL_ENDIF ) )
		{
			return this.evaluateControlENDIF() ;
		}

		if ( controlName.equals( CONTROL_ELSE ) )
		{
			return this.evaluateControlELSE() ;
		}

		if ( controlName.equals( CONTROL_IFDEF ) )
		{
			return this.evaluateControlIFDEF() ;
		}

		msg = "Unknown control element: " + CONTROL_INDICATOR + controlName ;
		throw new TextReplacementException( msg ) ;
	} // evaluateControl()

	// -----------------------------------------------------------------------

	protected String getTextConstant()
					throws TextReplacementException 
	{
		Character ch      = null ;
		boolean ready     = false ;
    StringBuffer buffer ;

    buffer = new StringBuffer(100) ;
		while ( this.hasMoreChars() && !ready )
		{
			ch = this.nextCharacter() ;
			if ( ch.equals( this.getTextDelimiter() ) )
			{
        if ( this.hasMoreChars() )
        {
          ch = this.nextCharacter() ;
          if ( ch.equals( this.getTextDelimiter() ) )
          {
            buffer.append( ch.toString() ) ;
          }
          else
          {
            ready = true ;
            this.putBackNextChar() ;
          }
        }
        else
        {
        	ready = true ;
        }
			}
			else
			{
        buffer.append( ch.toString() ) ;
			}
		} // while
		return buffer.toString() ;
	} // getTextConstant()

	// -----------------------------------------------------------------------

	protected String asString( Object anObject )
	{
		String resultString	= null ;

		if ( anObject instanceof TextRepresentation )
		{
			resultString = ((TextRepresentation)anObject).asText() ;
		}
		else
		{
			resultString = anObject.toString() ;
		}
		
		return resultString ;
	} // asString()

	// -----------------------------------------------------------------------
	
	protected String evaluatePlaceholder( String varName )
		throws UnknownVariableException
	{
		Object value ;
		
    // Writing could be switched off by IFDEF and therefore there must not
    // be an exception, if a varName is not known.
    // Actually the evaluation can be skipped, because writing switched off !
    if ( this.isWritingOn() )
    {
    	if ( this.supressMissingVariableException() )
    	{
				try
				{
					value = this.getVariableResolver().getValue( varName ) ;
				}
				catch (UnknownVariableException e)
				{
					// Ignore the exception and use the variable name instead
					value = varName ;
				}
    	}
    	else
    	{
    		value = this.getVariableResolver().getValue( varName ) ;
    	}
 		  return this.asString( value ) ;
   }

    return "" ;
	} // evaluatePlaceholder()

	// -----------------------------------------------------------------------

	protected boolean isDefinedPlaceholder( String varName )
	{
		return this.getVariableResolver().isKnownVariable( varName ) ;
	} // isDefinedPlaceholder()

	// -----------------------------------------------------------------------

	protected Vector getFunctionParameters()
						throws TextReplacementException
	{
		Character ch      = null ;
		Vector parameters = new Vector() ;
    String arg        = null ;
    String varName		= null ;

		ch = this.skipSpaces() ;
		
		do
		{
			if ( ch.equals( this.getTextDelimiter() ) )
			{
				arg = this.getTextConstant() ;
			}
			else
			{
        this.putBackNextChar() ;
        varName = this.getPlaceholderName() ;
        arg = this.evaluatePlaceholder( varName ) ;
			}
			
			parameters.add( arg ) ;
			
			ch = this.skipSpaces() ;
		} 
		while ( ch.equals( this.getFunctionParameterSeparator() ) ) ;

		this.putBackNextChar() ;

		return parameters ;
	} // getFunctionParameters()

	// -----------------------------------------------------------------------

  /**
   * Returns the next name for a function parameter.
   */
	protected String getFunctionParameterName()
						throws TextReplacementException
	{
		Character ch      = null ;
    String varName		= null ;

		ch = this.skipSpaces() ;

		if ( ch.equals( this.getTextDelimiter() ) )
		{
			throw ( new TextReplacementException( "No text constant allowed here !" ) ) ;
		}
		else
		{
      this.putBackNextChar() ;
      varName = this.getPlaceholderName() ;
		}

		return varName ;
	} // getFunctionParameterName()

	// -----------------------------------------------------------------------

	protected String evaluateFunction( String funcName, Vector parameter )
						throws UnknownFunctionException, InvalidParameterException
	{
		if ( this.getFunctionResolver() == null )
		{
			throw new UnknownFunctionException( funcName ) ;
		}
		return this.asString( this.getFunctionResolver().executeFunction( funcName, parameter ) ) ;
	} // evaluateFunction()

	// -----------------------------------------------------------------------

	protected String evaluateFunction( String funcName )
						throws TextReplacementException
	{
		Vector parameters ;

		parameters = this.getFunctionParameters() ;

    this.checkFunctionEnd( funcName ) ;
    this.checkEndAfterFunction( funcName ) ;

		return this.evaluateFunction( funcName, parameters ) ;
	} // evaluateFunction()

	// -----------------------------------------------------------------------

	protected void readAhead()
						throws IOException
	{
		int readResult  = -1 ;

		if ( this.getCharCache() == EMPTY_CACHE )	// Reads only, if cache is empty !
		{
			readResult = this.getSourceBuffer().read() ;
	
			if ( readResult < 0 )
			{
				throw ( new IOException( "End of String" ) ) ;
			}
			else
			{
				this.setCharCache( readResult ) ;
			}
		}
	} // readAhead()

	// -----------------------------------------------------------------------

	protected void readNext()
				throws IOException
	{
		char ch		= '@' ;
		
		this.readAhead() ;	// Reads only, if cache is empty !

		ch = (char)this.getCharCache() ;
		this.setCharCache( EMPTY_CACHE ) ;
		this.setNextChar( Character.valueOf(ch) ) ;
		this.setNextCharFilled( true ) ;
	} // readNext()

	// -----------------------------------------------------------------------

	protected boolean hasMoreChars()
	{
		boolean hasChar = true ;

		if ( isNextCharBufferEmpty() )
		{
			try
			{
				this.readAhead() ;
			}
			catch ( IOException ex )
			{
				hasChar = false ;
			}
		}
		return hasChar ;
	} // hasMoreChars()

	// -----------------------------------------------------------------------

	protected Character nextChar()
						throws IOException
	{
		if ( isNextCharBufferEmpty() )
		{
			this.readNext() ;
		}
		this.setNextCharFilled( false ) ;
		return this.getNextChar() ;
	} // nextChar()

	// -----------------------------------------------------------------------

  /**
   * This is a convinient method for retreiving the
   * next character, without always catching IOException.
   * It should only be used in combination with hasMoreChars().
   */
  protected Character nextCharacter()
  {
    Character ch = null ;
    try
    {
      ch = this.nextChar() ;
    }
    catch ( IOException ex ) { /* deliberately ignored */ }
    return ch ;
  } // nextCharacter()

	// -----------------------------------------------------------------------

	protected void putBackNextChar()
	{
		if ( isNextCharBufferEmpty() )
		{
		  this.setNextCharFilled( true ) ;
		}
	} // putBackNextChar()
	// -----------------------------------------------------------------------

	protected boolean isNextCharBufferEmpty()
	{
		return ( ! this.getNextCharFilled() ) ;
	} // isNextCharBufferEmpty()

	// -----------------------------------------------------------------------

	protected Character skipSpaces()
	{
		Character ch 			= Character.valueOf( StringUtil.CH_SPACE ) ;
		
		while ( this.hasMoreChars() && Character.isWhitespace( ch.charValue() ) )
			ch = this.nextCharacter() ;
			
		return ch ;
	} // skipSpaces()

	// -----------------------------------------------------------------------

	protected String resultString()
	{
		return this.getResultBuffer().toString() ;
	} // resultString()

	// -----------------------------------------------------------------------

	protected Character getFunctionParameterStart()
	{
		return DEFAULT_FUNCPARAM_START ;
	} // getFunctionParameterStart()

	// -----------------------------------------------------------------------

	protected Character getFunctionParameterEnd()
	{
		return DEFAULT_FUNCPARAM_END ;
	} // getFunctionParameterEnd()

	// -----------------------------------------------------------------------

	protected Character getFunctionParameterSeparator()
	{
		return DEFAULT_PARAM_SEPARATOR ;
	} // getFunctionParameterSeparator()

	// -----------------------------------------------------------------------

	protected Character getTextDelimiter()
	{
		return DEFAULT_TEXT_DELIMITER ;
	} // getTextDelimiter()

	// -----------------------------------------------------------------------

	protected boolean isValidPlaceholderNameCharacter( char ch )
	{
		if 	(	( Character.isJavaIdentifierStart( ch ) ) ||
					( Character.isDigit( ch ) ) ||
					( this.getSpecialNameCharacters().indexOf( ch ) >= 0 ) 
				)
		{
			return true ;
		}
		return false ;
	} // isValidPlaceholderNameCharacter()

	// -----------------------------------------------------------------------

	protected Character getControlIndicator()
	{
		return CONTROL_INDICATOR ;
	} // getControlIndicator()

	// -----------------------------------------------------------------------

	protected boolean isIndentationOn()
	{
		return ( this.getIndentationFiller() != null ) ;
	} // isIndentationOn()

	// -----------------------------------------------------------------------

	protected boolean isWritingOn()
	{
		boolean bool 	= true ;

		if ( this.getExecutionControl().empty() )
			return true ;

		try
		{
			bool = ( ((Bool)this.getExecutionControl().peek()).isTrue() ) ;
		}
		catch ( EmptyStackException ex )
		{
			// Can't be, because is checked above !
		}
		return bool ;
	} // isWritingOn()

	// -----------------------------------------------------------------------

	protected boolean isConditionalControlOn()
	{
		return ( ! this.getExecutionControl().empty() ) ;
	} // isConditionalControlOn()

	// -----------------------------------------------------------------------

	protected void removeLastWritingSwitch()
				throws EmptyStackException
	{
		this.getExecutionControl().pop() ;
	} // removeWritingSwitch()

	// -----------------------------------------------------------------------

	protected void switchWritingTo( Bool bool )
	{
		this.getExecutionControl().push( bool ) ;
	} // switchWritingTo()

	// -----------------------------------------------------------------------

	protected void toggleWritingSwitch()
				throws EmptyStackException
	{
		Bool old = (Bool)this.getExecutionControl().pop() ;
		this.getExecutionControl().push( old.not() ) ;
	} // toggleWritingSwitch()

	// -----------------------------------------------------------------------

	protected void switchWritingOn()
	{
		this.switchWritingTo( Bool.getTrue() ) ;
	} // switchWritingOn()

	// -----------------------------------------------------------------------

	protected void switchWritingOff()
	{
		this.switchWritingTo( Bool.getFalse() ) ;
	} // switchWritingOff()

	// -----------------------------------------------------------------------
	
	protected StringUtil strUtil()
	{
		return StringUtil.current() ;
	} // strUtil()

	// -----------------------------------------------------------------------
	
} // class TextEngine