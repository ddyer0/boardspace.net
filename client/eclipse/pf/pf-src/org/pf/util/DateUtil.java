// ===========================================================================
// CONTENT  : CLASS DateUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 27/03/2010
// HISTORY  :
//  11/06/2005  mdu  CREATED
//	27/03/2010	mdu		added	-->	constants for time zones and various methods for time zone based date creation
//
// Copyright (c) 2005-2010, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.util ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * This class is intended to provide convenience methods around the java.util.Date 
 * class.
 * It is not always necessary to fiddle around with the awkward GregorianCalendar
 * and not everybody wants to use the deprecated methods of java.util.Date.
 * <p>
 * <strong>Be aware that this class always treats months 1-based NOT zero-based.
 * That is: 1=January, 2=February, 3=March, ..., 12=December</strong>
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class DateUtil
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  public static final TimeZone TIMEZONE_UTC     = TimeZone.getTimeZone("UTC");
  public static final TimeZone TIMEZONE_GMT     = TimeZone.getTimeZone("GMT");
  public static final TimeZone TIMEZONE_GERMANY = TimeZone.getTimeZone("Europe/Berlin");
  
  public static final SimpleDateFormat DF_GERMAN_DATE   = new SimpleDateFormat("dd.MM.yyyy");
  public static final SimpleDateFormat DF_UK_DATE       = new SimpleDateFormat("dd/MM/yyyy");
  public static final SimpleDateFormat DF_US_DATE       = new SimpleDateFormat("MM/dd/yyyy");
  public static final SimpleDateFormat DF_GERMAN_DATE_TIME   = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
  
  private static final String ZULU_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static DateUtil soleInstance = new DateUtil() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  /**
   * Returns the only instance this class supports (design pattern "Singleton")
   */
  public static DateUtil current()
  {
    return soleInstance ;
  } // current() 

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected DateUtil()
  {
    super() ;
  } // DateUtil() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns a Date object representing the given parameters.
   * 
   * @param year The year of the date to create 
   * @param month The month of the date to create (1-based, 1=January)
   * @param day The day of the date to create 
   */
  public Date newDate( int year, int month, int day) 
	{
		GregorianCalendar cal ;
		
		cal = new GregorianCalendar( year, month-1, day ) ;
		return cal.getTime() ;
	} // newDate() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns a Date object representing the given parameters.
   * 
   * @param timezone The time zone to which the other parameters are related
   * @param year The year of the date to create 
   * @param month The month of the date to create (1-based, 1=January)
   * @param day The day of the date to create 
   */
  public Date newDate( TimeZone timezone, int year, int month, int day) 
  {
    GregorianCalendar calendar ;
    
    calendar = new GregorianCalendar(timezone);
    calendar.set(year, month-1, day, 0, 0 ,0) ;
    return calendar.getTime() ;
  } // newDate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a Date object representing the given parameters.
   * 
   * @param year The year of the date to create 
   * @param month The month of the date to create (1-based, 1=January)
   * @param day The day of the date to create 
   * @param hour The hours (0-23)
   * @param minute The minutes (0-59)
   */
  public Date newDate( int year, int month, int day, int hour, int minute) 
  {
  	GregorianCalendar cal ;
  	
  	cal = new GregorianCalendar( year, month-1, day, hour, minute ) ;
  	return cal.getTime() ;
  } // newDate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a Date object representing the given parameters.
   * 
   * @param year The year of the date to create 
   * @param month The month of the date to create (1-based, 1=January)
   * @param day The day of the date to create 
   * @param hour The hours (0-23)
   * @param minute The minutes (0-59)
   * @param second The seconds (0-59)
   */
  public Date newDate( int year, int month, int day, int hour, int minute, int second) 
  {
  	GregorianCalendar cal ;
  	
  	cal = new GregorianCalendar( year, month-1, day, hour, minute, second ) ;
  	return cal.getTime() ;
  } // newDate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a Date object in time zone GMT representing the given parameters.
   * 
   * @param year The year of the date to create 
   * @param month The month of the date to create (1-based, 1=January)
   * @param day The day of the date to create 
   * @param hour The hours (0-23)
   * @param minute The minutes (0-59)
   * @param second The seconds (0-59)
   */
  public Date newGMTDate( int year, int month, int day, int hour, int minute, int second) 
  {
    return this.newDate( TIMEZONE_GMT, year, month, day, hour, minute, second );
  } // newGMTDate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns a Date object representing the given parameters.
   * 
   * @param timezone The time zone to which the other parameters are related
   * @param year The year of the date to create 
   * @param month The month of the date to create (1-based, 1=January)
   * @param day The day of the date to create 
   * @param hour The hours (0-23)
   * @param minute The minutes (0-59)
   * @param second The seconds (0-59)
   */
  public Date newDate( TimeZone timezone, int year, int month, int day, int hour, int minute, int second) 
  {
    GregorianCalendar cal ;
    
    cal = new GregorianCalendar(timezone);
    cal.set( year, month-1, day, hour, minute, second ) ;
    return cal.getTime() ;
  } // newDate() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the current date
   */
  public Date today() 
	{
		return new Date() ;
	} // today() 

	// -------------------------------------------------------------------------
  
  /**
   * Returns the given date as Calendar in the current machines time zone.
   */
  public Calendar asCalendar( Date date ) 
  {
    Calendar calendar;
    
    calendar = new GregorianCalendar();
    calendar.setTime(date);
    return calendar;
  } // asCalendar()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the given date as Calendar in the specified time zone.
   */
  public Calendar asCalendar( TimeZone timezone, Date date ) 
  {
    Calendar calendar;
    
    calendar = new GregorianCalendar(timezone);
    calendar.setTime(date);
    return calendar;
  } // asCalendar()
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the given date as Calendar in the GMT time zone.
   */
  public Calendar asGMTCalendar( Date date ) 
  {
    return this.asCalendar( TIMEZONE_GMT, date );
  } // asCalendar()
  
  // -------------------------------------------------------------------------
  
  /**
   * Converts the given date into the so-called zulu time.
   * That is: "yyyy-MM-dd'T'HH:mm:ssZ"
   */
  public String convertDateIntoZuluTime( Date date )
  {
    SimpleDateFormat format = new SimpleDateFormat( ZULU_DATE_PATTERN );
    format.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
    return format.format( date ) + "Z";    
  } // convertDateIntoZuluTime()
  
  // -------------------------------------------------------------------------

  /**
   * Converts the given string that must be formatted as so-called zulu-time
   * to a Date object.
   */
  public Date convertZuluTimeIntoDate( String str ) throws ParseException
  {
    SimpleDateFormat format = new SimpleDateFormat( ZULU_DATE_PATTERN );
    format.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
    if ( str.endsWith("Z") )
    {
      str = str.substring(0, str.length()-1 );
    }
    return format.parse( str );    
  } // convertZuluTimeIntoDate()

  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class DateUtil 
