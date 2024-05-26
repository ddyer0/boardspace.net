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
package bridge;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
	public int getHours()
	{
		return calendar.get(java.util.Calendar.HOUR_OF_DAY);
	}
	public int getMinutes()
	{
		return calendar.get(java.util.Calendar.MINUTE);
	}
	
	public int getDay()
	{
		return calendar.get(java.util.Calendar.DAY_OF_WEEK);
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
		calendar.set(java.util.Calendar.DAY_OF_MONTH,d);
		setTime(calendar.getTime().getTime());
	}
	public void setMonth(int t)
	{	calendar.set(java.util.Calendar.MONTH,t);
		setTime(calendar.getTime().getTime());
	}
	
	public void setHours(int n)
	{
		calendar.set(java.util.Calendar.HOUR,n);
	}
}
