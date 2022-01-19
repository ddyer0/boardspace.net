// ===========================================================================
// CONTENT  : CLASS SimpleDate
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 19/12/2008
// HISTORY  :
//  19/12/2008  mdu  CREATED
//
// Copyright (c) 2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This class represents a simple date. That means, it is not a timestamp like
 * java.util.Date and it also starts with month 1 for January rather than with
 * zero.
 * <p>
 * For convenience it also provides some commonly string representations.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class SimpleDate implements Serializable, Comparable, Cloneable
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	static final long serialVersionUID = 90001L;
	
	protected static final SimpleDateFormat SORT_FORMAT = new SimpleDateFormat("yyyyMMdd") ;
	protected static final SimpleDateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd") ;
	protected static final SimpleDateFormat GERMAN_FORMAT = new SimpleDateFormat("dd.MM.yyyy") ;
	protected static final SimpleDateFormat BRITISH_FORMAT = new SimpleDateFormat("dd/MM/yyyy") ;
	protected static final SimpleDateFormat US_FORMAT = new SimpleDateFormat("MM/dd/yyyy") ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
//  private Date internalDate = null ;
//  protected Date getInternalDate() { return internalDate ; }
//  protected void setInternalDate( Date newValue ) { internalDate = newValue ; }

  private Calendar calendar = new GregorianCalendar() ;
  protected Calendar getCalendar() { return calendar ; }
  protected void setCalendar( Calendar newValue ) { calendar = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Creates a new SimpleDate from the given dateString which must conform to 
   * the format "yyyyMMdd". 
   */
  public static SimpleDate parseSortDate( String dateString ) throws ParseException 
  {
  	Date date ;
  	
  	date = SORT_FORMAT.parse( dateString );
  	return new SimpleDate(date);
  } // parseSortDate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Creates a new SimpleDate from the given dateString which must conform to 
   * the format "dd.MM.yyyy". 
   */
  public static SimpleDate parseGermanDate( String dateString ) throws ParseException 
	{
		Date date ;
		
		date = GERMAN_FORMAT.parse( dateString );
		return new SimpleDate(date);
	} // parseGermanDate() 
	
	// -------------------------------------------------------------------------

  /**
   * Creates a new SimpleDate from the given dateString which must conform to 
   * the format "dd/MM/yyyy". 
   */
  public static SimpleDate parseBritishDate( String dateString ) throws ParseException 
  {
  	Date date ;
  	
  	date = BRITISH_FORMAT.parse( dateString );
  	return new SimpleDate(date);
  } // parseBritishDate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Creates a new SimpleDate from the given dateString which must conform to 
   * the format "MM/dd/yyyy". 
   */
  public static SimpleDate parseUSDate( String dateString ) throws ParseException 
  {
  	Date date ;
  	
  	date = US_FORMAT.parse( dateString );
  	return new SimpleDate(date);
  } // parseUSDate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Creates a new SimpleDate from the given dateString which must conform to 
   * the format "yyyy-MM-dd". 
   */
  public static SimpleDate parseISO8601Date( String dateString ) throws ParseException 
  {
  	Date date ;
  	
  	date = ISO8601_FORMAT.parse( dateString );
  	return new SimpleDate(date);
  } // parseISO8601Date() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values (i.e. today's date).
   */
  public SimpleDate()
  {
    this( new Date() ) ;
  } // SimpleDate() 
  
  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with a standard calendar.
   */
  public SimpleDate( GregorianCalendar gregorianCalendar )
  {
  	super() ;
  	this.setCalendar( gregorianCalendar ) ;
  } // SimpleDate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Initialize the new instance with a standard date object.
   */
  public SimpleDate( Date date )
  {
  	super() ;
  	this.setInternalDate( date );
  } // SimpleDate() 
  
	// -------------------------------------------------------------------------
  
  /**
   * Returns a Date object representing the given parameters.
   * 
   * @param year The year of the date to create 
   * @param month The month of the date to create (1-based, 1=January)
   * @param day The day of the date to create 
   */
  public SimpleDate( int year, int month, int day) 
	{
		this( DateUtil.current().newDate( year, month, day ) ) ;
	} // SimpleDate() 

	// -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the year as integer (1900..3000).
   */
  public int getYear() 
	{
  	return this.getCalendar().get( Calendar.YEAR ) ;
	} // getYear() 
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the month as integer (1 = January, ..., 12 = December).
   */
  public int getMonth() 
  {
  	return this.getCalendar().get( Calendar.MONTH ) + 1 ;
  } // getMonth() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the day in month as integer (1..31).
   */
  public int getDay() 
  {
  	return this.getCalendar().get( Calendar.DAY_OF_MONTH ) ;
  } // getDay() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the date in the format "yyyyMMdd" ("20021123").
   */
  public String asSortString() 
	{
  	return SORT_FORMAT.format( this.getInternalDate() ) ;
	} // asSortString() 
	
	// -------------------------------------------------------------------------

  /**
   * Returns the date in the format "yyyy-MM-dd" ("2002-11-23").
   */
  public String asISO8601String() 
  {
  	return ISO8601_FORMAT.format( this.getInternalDate() ) ;
  } // asISO8601String() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the date in the format "dd.MM.yyyy" ("23.11.2002").
   */
  public String asGermanString() 
  {
  	return GERMAN_FORMAT.format( this.getInternalDate() ) ;
  } // asGermanString() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the date in the format "dd/MM/yyyy" ("23/11/2002").
   */
  public String asBritishString() 
  {
  	return BRITISH_FORMAT.format( this.getInternalDate() ) ;
  } // asBritishString() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the date in the format "MM/dd/yyyy" ("11/23/2002").
   */
  public String asUSString() 
  {
  	return US_FORMAT.format( this.getInternalDate() ) ;
  } // asUSString() 
  
  // -------------------------------------------------------------------------

  /**
   * Returns this date as Java Date object.
   */
  public Date asDate() 
	{
		return this.getInternalDate();
	} // asDate()
	
	// -------------------------------------------------------------------------
  
  /**
   * Returns the date in the format "yyyy-MM-dd" ("2002-11-23").
   */
  public String toString() 
	{
		return this.asISO8601String() ;
	} // toString() 
	
	// -------------------------------------------------------------------------

  public boolean equals( Object object ) 
	{
		if ( object instanceof SimpleDate )
		{
			SimpleDate simpleDate = (SimpleDate) object ;
			return this.compareTo( simpleDate ) == 0 ;
		}
		return false ;
	} // equals() 
	
	// -------------------------------------------------------------------------
  
  public int hashCode() 
	{
		return this.asSortString().hashCode();
	} // hashCode() 
	
	// -------------------------------------------------------------------------
  
  public int compareTo( Object object )
  {
  	if ( object instanceof SimpleDate )
  	{
  		SimpleDate simpleDate = (SimpleDate) object ;
  		return this.asSortString().compareTo( simpleDate.asSortString() ) ;
  	}
  	return -1;
  } // compareTo() 
  
  // -------------------------------------------------------------------------
  
  public Object clone() throws CloneNotSupportedException
  {
  	SimpleDate clone ;
  	
  	clone = new SimpleDate() ;
  	clone.setCalendar( (Calendar) this.getCalendar().clone() ) ;
  	return clone ;
  } // clone()
  
  // -------------------------------------------------------------------------
  
	// =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected  Date getInternalDate()
  {
  	return this.getCalendar().getTime();
  } // getInternalDate() 
  
  // -------------------------------------------------------------------------
  
  protected void setInternalDate( Date date )
  {
  	this.getCalendar().setTime( date ) ;
  } // setInternalDate() 
  
  // -------------------------------------------------------------------------
  
} // class SimpleDate 
