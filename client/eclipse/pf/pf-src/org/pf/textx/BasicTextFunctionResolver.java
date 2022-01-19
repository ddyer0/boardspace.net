// ===========================================================================
// CONTENT  : CLASS BasicTextFunctionResolver
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 06/03/2004
// HISTORY  :
//  26/06/1999 	duma  CREATED
//	25/01/2000	duma	moved		-> from package 'com.mdcs.text'
//	06/03/2004	duma	changed	-> chack parater == null in funcdate()
//
// Copyright (c) 1999-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.textx;
// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Vector ;
import java.util.Date ;
import java.text.SimpleDateFormat  ;

/**
 * This class provides support for some basic functions,
 * that are useful in text replacement done by a TextEngine.
 *
 * @author Manfred Duchrow
 * @version 1.1
 * @see TextEngine
 */
public class BasicTextFunctionResolver implements FunctionResolver
{
  // ========================================================================
  // PRIVATE CONSTANTS
  // ========================================================================
  /**
   * Name for a function that gets one string parameter and returns it
   * with a capitalized first letter.
   * Example: UC1("random") ==> "Random"
   */
  public static final String UPPERCASE_FIRST_LETTER = "UC1" ;
  /**
   * Name for a function that gets one string parameter and returns it
   * with a lower case first letter.
   * Example: LC1("FirstEntry") ==> "firstEntry"
   */
  public static final String LOWERCASE_FIRST_LETTER = "LC1" ;
  /**
   * Name for a function that gets one string parameter and returns it
   * with all letters changed to upper case.
   * Example: UC1("random") ==> "RANDOM"
   */
  public static final String UPPERCASE = "UC" ;
  /**
   * Name for a function that gets one string parameter and returns it
   * with all letters changed to lower case.
   * Example: LC("FirstEntry") ==> "firstentry"
   */
  public static final String LOWERCASE = "LC" ;
  /**
   * Name for a function that gets one string parameter and returns it
   * with a lower case first letter.
   * Example: Date() ==> "January 12, 1999"
   */
  public static final String DATE = "Date" ;

  // ========================================================================
  // IMPLEMENTATION OF INTERFACE FunctionResolver
  // ========================================================================

  /**
   * Returns the value for the function with the given name.
   *
   * @param functionName The case sensitive name of the function.
   * @param parameter A collection of parameters for the function.
   * @return The evaluation of the function with the given parameters (null is a valid return value)
   * @throws UnknownFunctionException The receiver is not knowing the function.
   */
  public Object executeFunction( String functionName, Vector parameter )
                throws UnknownFunctionException, InvalidParameterException
  {
    if ( functionName.equals( UPPERCASE_FIRST_LETTER ) )
      return this.funcUppercaseFirstLetter( parameter ) ;
    if ( functionName.equals( LOWERCASE_FIRST_LETTER ) )
      return this.funcLowercaseFirstLetter( parameter ) ;
    if ( functionName.equals( UPPERCASE ) )
      return this.funcUppercase( parameter ) ;
    if ( functionName.equals( LOWERCASE ) )
      return this.funcLowercase( parameter ) ;
    if ( functionName.equals( DATE ) )
      return this.funcDate( parameter ) ;
    throw ( new UnknownFunctionException( functionName ) ) ;
  } // executeFunction()

  // ------------------------------------------------------------------------

  /**
   * Returns if the function with the given name can be resolved by the receiver.
   *
   * @param functionName The case sensitive name of the function.
   * @return Whether the function with the given name is known or not.
   */
  public boolean isKnownFunction( String functionName )
  {
    if ( functionName.equals( UPPERCASE_FIRST_LETTER ) ) { return true ; }
    if ( functionName.equals( LOWERCASE_FIRST_LETTER ) ) { return true ; }
    if ( functionName.equals( UPPERCASE ) ) { return true ; }
    if ( functionName.equals( LOWERCASE ) ) { return true ; }
    if ( functionName.equals( DATE ) ) { return true ; }
    return false ;
  } // isKnownFunction()

  // ========================================================================
  // SUPPORTED FUNCTIONS : PROTECTED INSTANCE METHODS
  // ========================================================================

  protected String upperLowerFirstLetter( Vector parameter, boolean upper )
            throws InvalidParameterException
  {
    String param1 = null ;
    String result = null ;
    
    this.checkParameterCount( UPPERCASE_FIRST_LETTER, parameter, 1 ) ;
    
    param1 = (String)parameter.elementAt(0) ;
    if ( param1.length() > 0 )
  	{
    	result = param1.substring( 0, 1 ) ;
    	if ( upper )
    		result = result.toUpperCase() + param1.substring( 1 ) ;
    	else
    		result = result.toLowerCase() + param1.substring( 1 ) ;
  	}
  	else
  	{
  		result = param1 ;
  	}
    return result ;
  } // upperLowerFirstLetter()

  // ------------------------------------------------------------------------

  protected String funcUppercaseFirstLetter( Vector parameter )
            throws InvalidParameterException
  {
    return this.upperLowerFirstLetter( parameter, true ) ;
  } // funcUppercaseFirstLetter()

  // ------------------------------------------------------------------------

  protected String funcLowercaseFirstLetter( Vector parameter )
            throws InvalidParameterException
  {
    return this.upperLowerFirstLetter( parameter, false ) ;
  } // funcLowercaseFirstLetter()

  // ------------------------------------------------------------------------

  protected String funcUppercase( Vector parameter )
            throws InvalidParameterException
  {
    this.checkParameterCount( UPPERCASE, parameter, 1 ) ;
    String param = (String)parameter.elementAt(0) ;
    return param.toUpperCase() ;
  } // funcUppercase()

  // ------------------------------------------------------------------------

  protected String funcLowercase( Vector parameter )
            throws InvalidParameterException
  {
    this.checkParameterCount( LOWERCASE, parameter, 1 ) ;
    String param = (String)parameter.elementAt(0) ;
    return param.toLowerCase() ;
  } // funcLowercase()

  // ------------------------------------------------------------------------

  protected String funcDate( Vector parameter )
            throws InvalidParameterException
  {
    SimpleDateFormat  df = null ;

		if ( parameter != null )
		{
	    if ( parameter.size() == 0 )
	    {
	      df = new SimpleDateFormat() ;
	    } 
	    else if ( parameter.size() == 1 )
	    {
	      df = new SimpleDateFormat( (String)parameter.elementAt(0) ) ;
	    }
	    else
	    {
	      this.checkParameterCount( DATE, parameter, 1 ) ;
	    }
		}
		if ( df == null )
			df = new SimpleDateFormat() ;
		
    return df.format( new Date() ) ;
  } // funcDate()

  // ========================================================================
  // PROTECTED INSTANCE METHODS
  // ========================================================================
  protected void checkParameterCount( String funcName, Vector parameter, int count )
            throws InvalidParameterException
  {
    String message = null ;

    if ( parameter.size() != count )
    {
      message = "Wrong number of parameters ! ";
      message = message + "Required: " + Integer.toString( count ) + " ; " ;
      message = message +  "Given: " + Integer.toString( parameter.size() ) + "." ;
      throw ( new InvalidParameterException( funcName, message ) ) ;
    }
  } // checkParameterCount()


} // interface FunctionResolver