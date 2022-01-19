// ===========================================================================
// CONTENT  : CLASS MatchRule
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.6 - 20/12/2004
// HISTORY  :
//  11/07/2001  duma  CREATED
//  20/07/2001  duma  changed -> Use MatchRuleChars instead of static variables
//  08/01/2002  duma  changed -> Made serializable
//	23/08/2002	duma	re-designed	-> Moved parsing and printing to other classes
//	24/10/2003	mdu		added		->	multiCharWildcardMatchesEmptyString
//	04/12/2003	mdu		added		-> optimize()
//	06/07/2004	mdu		bugfix	-> changed inst var 'parser' to transient
//	20/12/2004	mdu		added		-> setDatatypes()
//
// Copyright (c) 2001-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Map ;
import java.io.Serializable ;

/**
 * The purpose of class MatchRule is to provide an easy-to-use component, 
 * that checks a dictionary (i.e. Map) of attributes and corresponding 
 * values against a defined rule. Such a rule is a string containing logical 
 * operations on attributes and their values.<br>
 * Particularly in the context of LDAP objects and their attributes this 
 * component can be very helpful.
 * <p>
 * Any instance of this class can handle one rule.
 * A rule can be create through one of the different parsers.<br>
 * (e.g. {@link DefaultMatchRuleParser} or {@link LdapFilterParser} )
 *
 * If the constructor with a rule string as parameter is used, it 
 * automatically uses the {@link DefaultMatchRuleParser} to parse the rule.
 * <p>
 * Example:
 * <ul><pre>
 * Map attributes = new HashMap() ;
 * attributes.put( "firstname", "Pedro" ) ;
 * attributes.put( "surname", "Vazquez" ) ;
 * attributes.put( "dob", "19610422" ) ;
 * attributes.put( "city", "Madrid" ) ;
 * MatchRule rule = DefaultMatchRuleParser.parse( "surname{V*} & city{Madrid}" ) ;
 * if ( rule.match( attributes ) )
 * .....
 * </pre></ul>
 * 
 * @author Manfred Duchrow
 * @version 1.6
 */
public class MatchRule implements Serializable
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private MatchGroup rootGroup = null ;
  protected MatchGroup getRootGroup() { return rootGroup ; }
  protected void setRootGroup( MatchGroup newValue ) { rootGroup = newValue ; }

	// -------------------------------------------------------------------------

  private boolean ignoreCase  = false ;
  protected boolean getIgnoreCase() { return ignoreCase ; }
  protected void setIgnoreCase( boolean newValue ) { ignoreCase = newValue ; }
  
  // -------------------------------------------------------------------------
  
	private boolean ignoreCaseInNames = false ;
	/**
	 * Returns true, if the parser produces MatchRules that treat
	 * attribute names case-insensitive.
	 */
	protected boolean getIgnoreCaseInNames() { return ignoreCaseInNames ; }
	private void setIgnoreCaseInNames( boolean newValue ) { ignoreCaseInNames = newValue ; }
	
	// -------------------------------------------------------------------------

  private transient DefaultMatchRuleParser parser = null ;
  protected DefaultMatchRuleParser getParser() { return parser ; }
  protected void setParser( DefaultMatchRuleParser newValue ) { parser = newValue ; }
  
  // -------------------------------------------------------------------------
  
	private boolean multiCharWildcardMatchesEmptyString = false ;
	/**
	 * Returns true, if this match rule allows empty strings 
	 * at the position of the multi character wildcard ('*').
	 * <p>
	 * The default value is false. 
	 */
	protected boolean getMultiCharWildcardMatchesEmptyString() { return multiCharWildcardMatchesEmptyString ; }
	/**
	 * Sets whether or not this match rule allows empty 
	 * strings at the position of the multi character wildcard ('*').
	 * <p>
	 * The default value is false. 
	 */
	private void setMultiCharWildcardMatchesEmptyString( boolean newValue ) { multiCharWildcardMatchesEmptyString = newValue ; }	

	// -------------------------------------------------------------------------  
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   * The rule can be set later with the method setRule().
   */
  public MatchRule()
  {
    super() ;
  } // MatchRule()

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with default values.
   * The rule can be set late with the method setRule().
   * Here it can be predefined if case sensitivity is switched off or not
   */
  public MatchRule( boolean ignoreCase )
  {
    this() ;
    this.setIgnoreCase( ignoreCase );
  } // MatchRule()

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a rule for later pattern-matching.
   */
  public MatchRule( String rule )
    throws MatchRuleParseException
  {
    this( rule, null ) ;
  } // MatchRule()

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a root group
   */
  public MatchRule( MatchGroup aGroup )
  {
    this() ;
    this.setRootGroup( aGroup ) ;
  } // MatchRule()

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with rule for later pattern-matching
   * and a set of special characters which are the operators and
   * separators on the rule.
   */
  public MatchRule( String rule, MatchRuleChars charSet )
    throws MatchRuleParseException
  {
    this() ;
    this.parseRule( rule, charSet ) ;
  } // MatchRule()

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Set (and parse) the given rule and keep it for later
   * checks with matches().
   * This replaces any other rule given to this instance before.
   */
  public void setRule( String rule )
    throws MatchRuleParseException
  {
    this.parseRule( rule, null ) ;
    this.ignoreCase( this.getIgnoreCase() ) ;
  } // setRule()

  // -------------------------------------------------------------------------

  /**
   * Defines whether or not the case of characters in value comparison
   * must be ignored.
   */
  public void ignoreCase( boolean ignoreIt )
  {
    this.getRootGroup().ignoreCase( ignoreIt ) ;
    this.setIgnoreCase( ignoreIt ) ;
  } // ignoreCase()

  // -------------------------------------------------------------------------

	/**
	 * Defines whether or not the case of characters in attribute names
	 * must be ignored.
	 */
	public void ignoreCaseInNames( boolean ignoreIt )
	{
		this.getRootGroup().ignoreCaseInName( ignoreIt ) ;
		this.setIgnoreCaseInNames( ignoreIt ) ;
	} // ignoreCaseInNames()
   
	// -------------------------------------------------------------------------

	/**
	 * Sets whether or not this match rule allows empty 
	 * strings at the position of the multi character wildcard ('*').
	 * <p>
	 * The default value is false. 
	 */
	public void multiCharWildcardMatchesEmptyString( boolean yesOrNo ) 
	{ 
		this.getRootGroup().multiCharWildcardMatchesEmptyString( yesOrNo ) ;
		this.setMultiCharWildcardMatchesEmptyString( yesOrNo ) ;
	} // multiCharWildcardMatchesEmptyString()

	// -------------------------------------------------------------------------	

  /**
   * Append the given rule to the receiver with a logical AND
   */
  public void mergeAnd( MatchRule rule )
  {
    MatchGroup group  = null ;

    group = rule.getRootGroup() ;
    if ( group != null )
    {
      group.setAnd( true ) ;
      this.appendGroup( group ) ;
    }
  } // mergeAnd() ;

  // -------------------------------------------------------------------------

  /**
   * Append the given rule to the receiver with a logical OR
   */
  public void mergeOr( MatchRule rule )
  {
    MatchGroup group  = null ;

    group = rule.getRootGroup() ;
    if ( group != null )
    {
      group.setAnd( false ) ;
      this.appendGroup( group ) ;
    }
  } // mergeOr() ;

  // -------------------------------------------------------------------------

  /**
   * Returns true, if the attributes and their values in the given
   * dictionary comply to the rules of the receiver.
   *
   * @param dictionary The attribute-value pairs that have to be checked against the rules
   */
  public boolean matches( Map dictionary )
  {
    return this.getRootGroup().matches( dictionary ) ;
  } // matches()

  // -------------------------------------------------------------------------

  public String toString()
  {
    MatchRulePrinter printer ;
    
    if ( this.getRootGroup() == null )
      return "null" ;
 
   	printer = new MatchRulePrinter( this.parser().getRuleChars() ) ;
    return printer.asString( this ) ;
  } // toString()

  // -------------------------------------------------------------------------

  /**
   * Iterates through all elements of the rule and calls the
   * appropriate methods of the given visitor.
   * <br>
   * As the name indicates, this is implemented following the
   * <b>Visitor</b> design pattern from the book <i>Design Patterns</i>
   * from Erich Gamma et. al.
   *
   * @param visitor the object that receives all callbacks for the visited elements
   */
  public void apply( MatchRuleVisitor visitor )
  {
    visitor.walkThroughInit() ;
    this.getRootGroup().apply( visitor ) ;
    visitor.walkThroughFinished() ;
  } // apply()

  // -------------------------------------------------------------------------

	/**
	 * Optimize the rule for best performance.
	 * This method might change the internal structure of the rule.
	 */
	public void optimize()
	{
		this.getRootGroup().optimize() ;
	} // optimize()()

	// -------------------------------------------------------------------------

	/**
	 * Sets the datatypes specified in the given map. 
	 * The keys are the attribute names and the values are the corresponding
	 * types. 
	 * <p>
	 * Currently supported datatypes are:
	 * <ul>
	 * <li>Float.class
	 * <li>Double.class
	 * <li>BigDecimal.class
	 * <li>Integer.class
	 * <li>Long.class
	 * <li>String.class
	 * <li>SimpleDateFormat
	 * </ul>
	 * @param datatypes The attributes and their corresponding types
	 * @throws MatchRuleException if a value of the rule cannot be converted to the specified datatype 
	 */
  public void setDatatypes( Map datatypes )
  	throws MatchRuleException
	{
  	if ( datatypes != null )
		{
	  	this.getRootGroup().applyDatatypes( datatypes ) ;
		}
	} // applyDatatypes() 
	
	// -------------------------------------------------------------------------	
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected DefaultMatchRuleParser parser()
	{
  	if ( this.getParser() == null )
  	{
  		this.setParser( new DefaultMatchRuleParser() ) ;
  	}
		return this.getParser() ;		
	} // parser()

	// -------------------------------------------------------------------------
	
  protected void parseRule( String rule, MatchRuleChars charSet )
    throws MatchRuleParseException
  {
  	MatchGroup group ;
  	
  	if ( charSet != null )
  	{
  		this.setParser( new DefaultMatchRuleParser( charSet ) ) ;
  	}
  	group = this.parser().parseToGroup( rule ) ;
  	this.setRootGroup( group ) ;
  } // parseRule()

  // -------------------------------------------------------------------------

  protected void appendGroup( MatchGroup group )
  {
    MatchGroup newRoot    = null ;

    newRoot = new MatchGroup() ;
    newRoot.getElements().add( this.getRootGroup() ) ;
    newRoot.getElements().add( group ) ;
    this.setRootGroup( newRoot ) ;
  } // appendGroup()

  // -------------------------------------------------------------------------

} // class MatchRule