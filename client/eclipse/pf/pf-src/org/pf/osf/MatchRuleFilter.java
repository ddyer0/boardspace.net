// ===========================================================================
// CONTENT  : CLASS MatchRuleFilter
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 18/06/2006
// HISTORY  :
//  18/06/2006  mdu  CREATED
//
// Copyright (c) 2006, by M.Duchrow. All rights reserved.
// ===========================================================================
package org.pf.osf ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Map;

import org.pf.bif.filter.IObjectFilter;
import org.pf.reflect.AttributeReadAccess;
import org.pf.text.MatchRule;
import org.pf.text.StringUtil;

/**
 * A special filter that uses internally a MatchRule to check whether
 * an object matches or not.
 * The objects it can check must implement either java.util.Map or
 * org.pf.reflect.AttributeReadAccess.
 *
 * @author M.Duchrow
 * @version 1.0
 * @see org.pf.reflect.AttributeReadAccess
 */
public class MatchRuleFilter implements IObjectFilter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private MatchRule matchRule = null ;
  public MatchRule getMatchRule() { return matchRule ; }
  protected void setMatchRule( MatchRule newValue ) { matchRule = newValue ; }

  protected MapFacade mapFacade = new MapFacade( new DummyObject() ) ; 
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with a match rule.
   * 
   * @param rule The match rule to be used for object matching
   * @throws IllegalArgumentException if the given parameter is null
   */
  public MatchRuleFilter( MatchRule rule )
  {
    super() ;
    if ( rule == null )
		{
			throw new IllegalArgumentException( "rule must not be null" ) ; 
		}
    this.setMatchRule( rule ) ;
  } // MatchRuleFilter() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns true if the given object matches this filter. That is, its 
   * attributes match the underlying MatchRule.
   * 
   * @param object Either an instance of Map or AttributeReadAccess
   * @see AttributeReadAccess
   */
  public boolean matches( Object object )
  {
  	Map map = null ;
  	
		if ( object instanceof Map )
		{
			map = (Map)object ;
		}
		else if ( object instanceof AttributeReadAccess )
		{
			mapFacade.setObject( (AttributeReadAccess)object ) ;
			map = mapFacade ;
		}
		if ( map == null )
		{
			return false ;
		}
		return this.getMatchRule().matches( map ) ; 
  } // matches()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	// -------------------------------------------------------------------------
  // --- INNER CLASSES -------------------------------------------------------
  // -------------------------------------------------------------------------
  
  protected class DummyObject implements AttributeReadAccess
	{
		public String[] getAttributeNames()
		{
			return StringUtil.EMPTY_STRING_ARRAY ;
		} // getAttributeNames() 
		
		// -----------------------------------------------------------------------
		
		public Object getAttributeValue( String name ) throws NoSuchFieldException
		{
			return null;
		} // getAttributeValue() 
	} // class DummyObject
  
  // -------------------------------------------------------------------------
  
} // class MatchRuleFilter 
