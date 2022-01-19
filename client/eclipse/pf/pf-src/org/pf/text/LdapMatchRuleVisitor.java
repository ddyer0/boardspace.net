// ===========================================================================
// CONTENT  : CLASS LdapMatchRuleVisitor
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 27/12/2002
// HISTORY  :
//  17/08/2001  duma  CREATED
//  12/11/2001  duma  changed ->  Redesign with inner class GroupInfo
//	27/12/2002	duma	changed ->  Redesign with inner class AttributeInfo
//
// Copyright (c) 2001-2002, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Walks over a MatchRule to create a LDAP filter string out of it.
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class LdapMatchRuleVisitor implements MatchRuleVisitor
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

  private GroupInfo rootGroup = null ;
  protected GroupInfo getRootGroup() { return rootGroup ; }
  protected void setRootGroup( GroupInfo newValue ) { rootGroup = newValue ; }

  private Stack stack = null ;
  protected Stack getStack() { return stack ; }
  protected void setStack( Stack newValue ) { stack = newValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public LdapMatchRuleVisitor()
  {
    super() ;
  } // LdapMatchRuleVisitor()

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
    this.setStack( new Stack() ) ;
  } // walkThroughInit()

  // -------------------------------------------------------------------------

  /**
   * This method will be called when the MatchRule has finished to walk
   * through its elements.
   */
  public void walkThroughFinished()
  {
    this.appendGroup( this.getRootGroup() ) ;
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
    GroupInfo group    = null ;

    group = this.createGroup() ;
    group.not_flag = notOperator ;
    if ( this.isFirstGroup() )
    {
      this.setRootGroup( group ) ;
    }
    else
    {
      this.addElementToCurrentGroup( group, andOperator ) ;
    }
    this.push( group ) ;
  } // startGroup()

  // -------------------------------------------------------------------------

  /**
   * This method will be called for each group end occurence.
   */
  public void endGroup()
  {
    this.pop() ;
  } // endGroup() ;

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
    AttributeInfo element      = new AttributeInfo() ;

    element.name = name ;
    element.operator = compareOperator ;
    element.values = values ;
    element.not_flag = notOperator ;

    this.addElementToCurrentGroup( element, andOperator ) ;
  } // attribute()

  // -------------------------------------------------------------------------

  /**
   * Converts the given match rule into a LDAP search string
   * compliant to RFC 1558.
   *
   * @param matchRule The rule to be converted
   */
  public String asSearchString( MatchRule matchRule )
  {
    matchRule.apply( this ) ;
    return this.getBuffer().toString() ;
  } // asSearchString()

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  public void appendGroup( GroupInfo group )
  {
    if ( group.not_flag )
      this.getBuffer().append( "(!" ) ;

    if ( group.neutral_elements == null )
    {
      this.appendGroupElements( group.and_elements, true );
      this.appendGroupElements( group.or_elements, false );
    }
    else
    {
      this.appendGroupElement( (ElementInfo)group.neutral_elements.get(0) );
    }

    if ( group.not_flag )
      this.getBuffer().append( ')' ) ;
  } // appendGroup()

  // -------------------------------------------------------------------------

  protected void appendGroupElement( ElementInfo element )
  {
    if ( element.isGroupInfo() )  // is it a group ?
    {
      this.appendGroup( (GroupInfo)element ) ;
    }
    else
    {
      this.appendAttribute( (AttributeInfo)element ) ;
    }
  } // appendGroupElement()

  // -------------------------------------------------------------------------

  protected void appendGroupElements( List elements, boolean andOperator )
  {
    if ( elements.size() == 0 )
      return ;

    this.getBuffer().append( '(' ) ;
    this.getBuffer().append( andOperator ? '&' : '|' ) ;
    for ( int i = 0 ; i < elements.size() ; i++ )
    {
      this.appendGroupElement( (ElementInfo)elements.get(i) ) ;
    }
    this.getBuffer().append( ')' ) ;
  } // appendGroupElements() ;

  // -------------------------------------------------------------------------

  protected void appendAttribute( AttributeInfo element )
  {
    this.appendAttribute( element.name, element.operator, element.values,
                          element.not_flag ) ;
  } // appendAttribute()

  // -------------------------------------------------------------------------

  protected void appendAttribute( String name, int operator, String[] values,
                                  boolean notOperator )
  {
    boolean manyValues    = false ;

    manyValues = values.length > 1 ;

    if ( notOperator )
      this.getBuffer().append( "(!" ) ;
    if ( manyValues )
      this.getBuffer().append( "(|" ) ;

    for ( int i = 0 ; i < values.length ; i++ )
    {
      this.appendAttribute( name, operator, values[i] ) ;
    }

    if ( manyValues )
      this.getBuffer().append( ")" ) ;
    if ( notOperator )
      this.getBuffer().append( ")" ) ;
  } // appendAttribute()

  // -------------------------------------------------------------------------

  protected void appendAttribute( String name, int operator, String value )
  {
    this.getBuffer().append( '(' ) ;
    this.getBuffer().append( name ) ;
    switch ( operator )
    {
    	case MatchAttribute.OPERATOR_EQUALS :
    		this.getBuffer().append( '=' ) ;
    		break ;
    	case MatchAttribute.OPERATOR_GREATER_OR_EQUAL :
    		this.getBuffer().append( ">=" ) ;
    		break ;
    	case MatchAttribute.OPERATOR_LESS_OR_EQUAL :
    		this.getBuffer().append( "<=" ) ;
    		break ;
    	default :
    		this.getBuffer().append( '=' ) ;
    }
    this.getBuffer().append( value ) ;
    this.getBuffer().append( ')' ) ;
  } // appendAttribute()

  // -------------------------------------------------------------------------

  protected void addElementToCurrentGroup( Object element, boolean andOperator )
  {
    GroupInfo group     = null ;
    List groupSlot    	= null ;

    group = this.currentGroup() ;
    groupSlot = andOperator ? group.and_elements : group.or_elements ;
    if ( group.neutral_elements == null )
    {
      groupSlot.add( element ) ;
    }
    else
    {
      if ( group.neutral_elements.size() == 0 )
      {
        group.neutral_elements.add( element ) ;
      }
      else
      {
        groupSlot.add( group.neutral_elements.get(0) ) ;
        groupSlot.add( element ) ;
        group.neutral_elements = null ;
      }
    }
  } // addElementToCurrentGroup()

  // -------------------------------------------------------------------------

  protected GroupInfo createGroup()
  {
    return new GroupInfo() ;
  } // createGroup()

  // -------------------------------------------------------------------------

  protected GroupInfo pop()
  {
    return (GroupInfo)this.getStack().pop() ;
  } // pop()

  // -------------------------------------------------------------------------

  protected void push( GroupInfo obj )
  {
    this.getStack().push( obj ) ;
  } // push()

  // -------------------------------------------------------------------------

  protected GroupInfo currentGroup()
  {
    return (GroupInfo)this.getStack().peek() ;
  } // currentGroup()

  // -------------------------------------------------------------------------

  protected boolean isFirstGroup()
  {
    return this.getStack().empty() ;
  } // isFirstGroup()

  // -------------------------------------------------------------------------
  // ---- INNER CLASS --------------------------------------------------------
  // -------------------------------------------------------------------------

	private class ElementInfo
	{
    protected boolean not_flag         = false ;

		protected boolean isGroupInfo()
		{
			return false ;
		} // isGroupInfo()

		// -----------------------------------------------------------------------

	} // class ElementInfo

  // -------------------------------------------------------------------------

  private class GroupInfo extends ElementInfo
  {
    protected List neutral_elements   = new ArrayList() ;
    protected List and_elements       = new ArrayList() ;
    protected List or_elements        = new ArrayList() ;

		protected boolean isGroupInfo()
		{
			return true ;
		} // isGroupInfo()

		// -----------------------------------------------------------------------

  } // class GroupInfo

	// -------------------------------------------------------------------------

	private class AttributeInfo extends ElementInfo
	{
		protected String name				= null ;
		protected int operator			= MatchAttribute.OPERATOR_EQUALS ;
		protected String[] values		= null ;

	} // class AttributeInfo


	// -------------------------------------------------------------------------

} // class LdapMatchRuleVisitor