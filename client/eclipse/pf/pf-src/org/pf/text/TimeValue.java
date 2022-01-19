// ===========================================================================
// CONTENT  : CLASS TimeValue
// AUTHOR   : M.Duchrow
// VERSION  : 1.0 - 13/09/2013
// HISTORY  :
//  13/09/2013  mdu  CREATED
//
// Copyright (c) 2013, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Represents a time value and provides parsing of strings and 
 * various different getters for various time units.
 *
 * @author M.Duchrow
 * @version 1.0
 */
public class TimeValue
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private long milliseconds;

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with a milliseconds value.
	 */
	public TimeValue(long millisecondsValue)
	{
		super();
		this.setMilliseconds(millisecondsValue);
	} // TimeValue() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a value and unit.
	 */
	public TimeValue(long value, TimeUnit unit)
	{
		this(value * unit.getMsFactor());
	} // TimeValue() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a string that must contain digits,
	 * optionally followed by the short name of a unit (@see {@link TimeUnit}.
	 * <br>
	 * The given string will be parsed, taking any optional unit string at the end into account.
	 * Allowed unit strings are the short names defined in TimeUnit (i.e. "h", "m", "s", "ms").
	 * 
	 * @param strValue The string to parse (must not be null).
	 * @param defaultUnit The unit to use if the string does not contain an explicit short name (must not be null).
	 * @throws NumberFormatException If the string does not contain a valid long value.
	 */
	public TimeValue(String strValue, TimeUnit defaultUnit)
	{
		this(0L);
		this.setMilliseconds(this.parseToMilliseconds(strValue, defaultUnit));
	} // TimeValue() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a string that must contain digits,
	 * optionally followed by the short name of a unit (@see {@link TimeUnit}.
	 * <br>
	 * The default unit is "ms" if not explicitly set in the string.
	 * @throws NumberFormatException If the string does not contain a valid long value.
	 */
	public TimeValue(String strValue)
	{
		this(strValue, TimeUnit.MILLISECONDS);
	} // TimeValue() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	public long asMilliseconds()
	{
		return this.getMilliseconds();
	} // asMilliseconds() 

	// -------------------------------------------------------------------------

	public long asSeconds()
	{
		return this.convertTo(TimeUnit.SECONDS);
	} // asSeconds() 

	// -------------------------------------------------------------------------

	public long asMinutes()
	{
		return this.convertTo(TimeUnit.MINUTES);
	} // asMinutes() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the underlying time in hours.
	 */
	public long asHours()
	{
		return this.convertTo(TimeUnit.HOURS);
	} // asHours() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the value converted to the specified unit.
	 * @param unit The unit of the result value (must not be null).
	 */
	public long convertTo(TimeUnit unit)
	{
		return this.getMilliseconds() / unit.getMsFactor();
	} // convertTo() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	/**
	 * Parse the given string, taking any optional unit string at the end into account.
	 * Allowed unit strings are the short names defined in TimeUnit (i.e. "h", "m", "s", "ms").
	 * 
	 * @param strValue The string to parse
	 * @param defaultUnit The unit to use if the string does not contain an explicit short name.
	 * @return The parsed value as milliseconds
	 * @throws NumberFormatException If the string does not contain a valid long value or an invalid unit.
	 */
	protected long parseToMilliseconds(String strValue, TimeUnit defaultUnit)
	{
		long millis;
		TimeUnit unit = null;
		StringPair stringPair;

		stringPair = this.splitDigitsAndUnit(strValue.trim());
		for (TimeUnit timeUnit : TimeUnit.values())
		{
			if (timeUnit.getShortName().equals(stringPair.getString2()))
			{
				unit = timeUnit;
				break;
			}
		}
		if (unit == null) // Needing default unit?
		{
			if (StringUtil.current().notNullOrEmpty(stringPair.getString2()))
			{
				throw new NumberFormatException("Invalid time unit specified: " + strValue);
			}
			unit = defaultUnit;
		}
		millis = Long.parseLong(stringPair.getString1()) * unit.getMsFactor();
		return millis;
	} // parseToMilliseconds() 
	
	// -------------------------------------------------------------------------

	protected StringPair splitDigitsAndUnit(String str)
	{
		StringPair pair;
		int noneDigitIndex = 0;

		pair = new StringPair();
		for (char ch : str.toCharArray())
		{
			if (!Character.isDigit(ch))
			{
				break;
			}
			noneDigitIndex++;
		}
		if (noneDigitIndex >= str.length())
		{
			pair.setString1(str);
			pair.setString2("");
		}
		else
		{
			pair.setString1(str.substring(0, noneDigitIndex));
			pair.setString2(str.substring(noneDigitIndex).trim());
		}
		return pair;
	} // splitDigitsAndUnit() 
	
	// -------------------------------------------------------------------------

	protected long getMilliseconds()
	{
		return milliseconds;
	} // getMilliseconds() 
	
	// -------------------------------------------------------------------------

	protected void setMilliseconds(long milliseconds)
	{
		this.milliseconds = milliseconds;
	} // setMilliseconds() 
	
	// -------------------------------------------------------------------------

} // class TimeValue 
