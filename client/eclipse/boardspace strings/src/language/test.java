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
package language;
import java.util.Calendar;

public class test 
{
	    public static void main(String[] args) {
	        // length of a day
	        long DAY_MILLIS = 1000 * 60 * 60 * 24;

	        Calendar cal = Calendar.getInstance();

	        cal.set(1900, 0, 1, 0, 0, 0);
	        System.out.println(cal.getTime());

	        cal.setTimeInMillis(cal.getTimeInMillis() + DAY_MILLIS);
	        System.out.println(cal.getTime());
	    }
}
