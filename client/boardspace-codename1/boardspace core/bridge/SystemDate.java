package bridge;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import lib.G;

public class SystemDate extends Date
{	static final String GMT = "GMT";
	Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(GMT));
		
	public SystemDate(long t)
	{
		super(t);
		calendar.setTimeZone(TimeZone.getTimeZone(GMT));
		calendar.setTime(this);
	}
	public SystemDate()
	{	super();
		calendar.setTimeZone(TimeZone.getTimeZone(GMT));
		calendar.setTime(this);
	}
	public int getYear()
	{
		return calendar.get(java.util.Calendar.YEAR)-1900;
	}
	public int getDate()
	{	return calendar.get(java.util.Calendar.DAY_OF_MONTH)-1;
	}
	public int getMonth()
	{	return calendar.get(java.util.Calendar.MONTH);
	}
	public void setYear(int y)
	{
		calendar.set(java.util.Calendar.YEAR,y+1900);
		setTime(calendar.getTime().getTime());
	}
	public void setDate(int d)
	{	
		calendar.set(java.util.Calendar.DAY_OF_MONTH,d+1);
		setTime(calendar.getTime().getTime());
	}
	public void setMonth(int t)
	{	calendar.set(java.util.Calendar.MONTH,t);
		setTime(calendar.getTime().getTime());
	}
}
