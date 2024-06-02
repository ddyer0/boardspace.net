/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package lib;

import bridge.GregorianCalendar;
import bridge.Locale;
import bridge.SimpleDateFormat;
import bridge.SystemDate;

import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TimeZone;
/**
 * BSDate (Boardspace Date) is an extension of java.util.Date
 * with behavior I like better, particularly understandable
 * and reliable parsing of ad-hoc dates and times.
 * 
 * @author Ddyer
 *
 */

public class BSDate extends SystemDate
{	/**
	 * 
	 */
 static final long serialVersionUID = 1L;

 /** default constructor */
 public BSDate() { super(); }
	public BSDate(long d) { super(d); }
 
 /** initialize a data from an ad-hoc date/time string using {@link #simpleDateParse}*/
	public BSDate(String time) 
	{ 	super(simpleDateParse(time));
	}

 public String shortTime() 
 { 	return(G.shortTime(getTime()));
 }
 /** initialize a data from an ad-hoc date/time string using {@link #simpleDateParse}
  * format is ignored 
  */
	public BSDate(String time,String format) 
	{ 	super(simpleDateParse(time));	// ignore the format, just do it
	}

	/*
	 * 
	 * the rest of this is a simple ad-hoc parser for dates.  It accepts most
	 * dd-mm-yy hh:mm:ss type formats, with ambiguity in favor of month-first
	 * if months and days are represented as numbers.
	 *  
	 */
	public static String weekdays[] = {"mon","tue","wed","thu","fri","sat","sun"};
	public static String uweekdays[] = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
	public static String months[] = {"jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"};

	@SuppressWarnings("deprecation")
	public String getDayString()
	{	int day = getDay();
		return uweekdays[day];
	}
	@SuppressWarnings("deprecation")
	public String getMonthString()
	{
		return months[getMonth()];
	}
	@SuppressWarnings("deprecation")
	public String getDateString()
	{
		return ""+getDate();
	}
	
	private static boolean isDayOfWeek(String d)
	{
			if(d!=null && d.length()>=3)
			{
				String sub = d.substring(0,3);
				for(String w : weekdays) { if(w.equalsIgnoreCase(sub)) { return(true); }}
			}
			return(false);
	}
	private static boolean isMonth(String d)
	{
			if(d!=null && d.length()>=3)
			{
				String sub = d.substring(0,3);
				for(String w : months) { if(w.equalsIgnoreCase(sub)) { return(true); }}
			}
			return(false);
	}
	private static boolean isDayNumber(String s)
	{
		if(s!=null 
			&& s.length()<=3	//including optional dot
			&& Character.isDigit(s.charAt(0))
			)
		{	
			int val = G.IntToken(s);
			if((val>0)&& val<=31) { return(true); }
		}
		return(false);
	}
	private static boolean isMonthNumber(String s)
	{
		if(s!=null 
				&& s.length()<=2
				&& Character.isDigit(s.charAt(0)))
		{	int val = G.IntToken(s);
			if((val>0)&& val<=12) { return(true); }
		}
		return(false);
	}
	private static boolean isYearNumber(String s)
	{
		if(s!=null
				&& (s.length()==4)
				&& (Character.isDigit(s.charAt(0)))
				)
		{	int val = G.IntToken(s);
			if(val>=0) { return(true); }
		}
		return(false);
	}
	
	private static boolean isPm(String str)
	{
		return("pm".equalsIgnoreCase(str)||("p".equalsIgnoreCase(str)));
	}
	private static boolean isAm(String str)
	{
		return("am".equalsIgnoreCase(str)||("a".equalsIgnoreCase(str)));
	}
	private static boolean isHourNumber(String s)
	{
		if(s!=null && (s.length()<=2))
		{	int val = G.IntToken(s);
			if(val>=0 && val<24) { return(true); }
		}
		return(false);
	}
	private static boolean isMinuteNumber(String s)
	{
		if(s!=null 
				&& (s.length()<=2)
				&& Character.isDigit(s.charAt(0)))
		{	int val = G.IntToken(s);
			if(val>=0 && val<60) { return(true); }
		}
		return(false);
	}
	private static boolean isTimezone(String s)
	{
		if(s!=null)
		{	int len = s.length();
			if(len==3 || len==4)
			{
			String end = s.substring(len-2,len);
			return("mt".equalsIgnoreCase(end) || "dt".equalsIgnoreCase(end) || "st".equalsIgnoreCase(end));
			}
		}
		return(false);
	}
	private static String koreanYear = "\ub144";
	private static String koreanMonth = "\uc6d4";
	private static String koreanDay = "\uc77c";
	private static String delims = ",-//: "+koreanYear+koreanDay+koreanMonth;
	private static long simpleDateParse(String time)
	{	StringTokenizer tok = new StringTokenizer(time.trim(),delims,true);
		String month = null;
		String year=null;
		String day=null;
		String hour=null;
		String minute=null;
		String second=null;
		String str = null;
		String nextTok = null;
		String prevstr = " ";
		String meridian = "";
		String timezone = null;
		do
		{	prevstr = str;
			if(nextTok!=null) { str = nextTok; nextTok = null; }
			else if(tok.hasMoreTokens()) { str = tok.nextToken().toLowerCase(); }
			if(str!=null)
			{
			if(koreanYear.equals(str) && isYearNumber(prevstr)) { year = prevstr; }
			else if(koreanMonth.equals(str) && isMonthNumber(prevstr)) { month = months[G.IntToken(prevstr)-1]; ; }
			else if (koreanDay.equals(str) && isDayNumber(prevstr)) { day = prevstr; }
			else if(":".equals(str))
			{	// time
				if((hour==null) && isHourNumber(prevstr)) { hour=prevstr; }
				else if(minute==null && isMinuteNumber(prevstr)) { minute=prevstr; }
				else if(second==null && isMinuteNumber(prevstr)) { second=prevstr; }
			}
			else {
			if(isDayOfWeek(str)) { }
			else if(month==null && isMonth(str))	// month as a string name
					{ 	
						month = str; 
					}
			else if(year!=null && (month==null) && isMonthNumber(str)) { month = months[G.IntToken(str)-1]; }
			else if(day==null && isDayNumber(str)) { day = ""+G.IntToken(str); }	// day as a number for sure, integerized and reprinted
			else if(month==null && isMonthNumber(str))				// month as a number or a day number
				{ month = months[G.IntToken(str)-1]; 
				}
			else if(month==null && isDayNumber(str) && isMonthNumber(day))
			{	month = months[Integer.parseInt(day)-1]; 			// reassign unambiguous mm-dd-yyyy
				day = str;
			}
			else if(year==null && isYearNumber(str)) { year = str; }
			else if(hour!=null && isPm(str)) { meridian=" p"; }
			else if(hour!=null && isAm(str)) { meridian=" a"; }
			else if(isTimezone(str)) { timezone = " "+str.toUpperCase(); }
			else { 
				//G.print("Not handled: "+str+" from "+time);
				// most likely timezone
			}
			}}
		} while(nextTok!=null || tok.hasMoreTokens());
		 
		if(day!=null && month!=null && year!=null)
		{	if(hour==null) { hour = "00";} else if (hour.length()==1) { hour = "0"+hour; }
			if(minute==null) { minute ="00"; } else if(minute.length()==1) { minute="0"+minute; }
			if(second==null) { second = "00"; } else if(second.length()==1) { second="0"+second; }
			String dateformat = "yyyy MMM dd HH:mm:ss";
			if(!"".equals(meridian))
			{
				dateformat += " a";
			}
			if(timezone!=null) { dateformat += " zzz"; }
			else { timezone = ""; }
			SimpleDateFormat sd = new SimpleDateFormat(dateformat,Locale.ENGLISH);
			// at the end of our ad-hoc parsing, construct a completely predictable date/time
			// and feed it to the system parser 
			String datestr = year+" "+month.substring(0,3)+" "+((day.length()==2)?day:("0"+day))
					+ " "+hour + ":" + minute+":"+second+meridian+timezone;
			
			try { 
				sd.setTimeZone(TimeZone.getTimeZone(timezone)); 
				java.util.Date date = sd.parse(datestr);
				return(date.getTime());
				
			}
			catch(Exception err)
			{
				G.Advise(false,"Parse exception %s",err);
			}
		}
		return(new BSDate().getTime());
		
	}
	/** 
	 * @param tt
	 * @return return true if this date is before tt
	 */
	public boolean before(BSDate tt) {
		return(getTime()<tt.getTime());
	}
	/** 
	 * @param tt
	 * @return return true if this date is after tt
	 */
	public boolean after(BSDate tt) {
		return(getTime()>tt.getTime());
	}
	/**
	 * return a date/time string in a format suitable to be part of a file name.
	 * @param stime
	 * @return a string for the date
	 */
	public String DateString()
	{
		GregorianCalendar xtime = new GregorianCalendar();
	    xtime.setTimeZone(TimeZone.getTimeZone("GMT"));
	    xtime.setTime(this);
	
	    int year = xtime.get(Calendar.YEAR);
	    int month = 1 + xtime.get(Calendar.MONTH);
	    int day = xtime.get(Calendar.DAY_OF_MONTH);
	    int hrs = xtime.get(Calendar.HOUR_OF_DAY);
	    int mins = xtime.get(Calendar.MINUTE);
	
	    return (year + "-" + ((month < 10) ? "0" : "") + month + "-" +
	    ((day < 10) ? "0" : "") + day + "-" + ((hrs < 10) ? "0" : "") + hrs +
	    ((mins < 10) ? "0" : "") + mins);
	}
	/** 
	 * parse a date string with a specific format.  This is intended
	 * only for non-critical uses - if there is any trouble with the 
	 * parsing it returns the current date/time.
	 * @param date
	 * @return a Date
	 */
	public static BSDate parseDate(String date)
	{	
		BSDate df = new BSDate(date);
		return(df);
	}
	public static void putStrings()
	{	InternationalStrings.put(months);
		InternationalStrings.put(weekdays);
	}
}
