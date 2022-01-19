// ===========================================================================
// CONTENT  : CLASS StringUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 4.4 - 16/11/2013
// HISTORY  :
//  10/07/1999 	duma  CREATED
//	09/12/1999	duma	added		->	SPACE, repeat()
//										moved		->	from package com.mdcs.util
//	25/01/2000	duma	moved		->	from package com.mdcs.text
//  09/02/2000  duma  changed ->  renamed SPACE to CH_SPACE
//                    added   ->  CH_CR, CH_TAB, ..., STR_SPACE, STR_NEWLINE, ...
//  11/01/2002  duma  added   ->	indexOf(), indexOfIgnoreCase(), contains(), containsIgnoreCase()
//	17/05/2002	duma	added		->	copyFrom()
//	03/07/2002	duma	added		->	cutHead(), prefix(), suffix()
//	06/07/2002	duma	added		->	indexOf() and contains() for StringPattern and reverse()
//	15/08/2002	duma	added		->	upTo(), startingFrom(), asMap()
//	29/09/2002	duma	added		->	allParts() and allSubstrings() that don't skip empty elements
//	06/03/2003	duma	changed	->	append() now uses System.arraycopy()
//										added		->	remove( String[], String[] ), remove( String[], String )
//																removeNull( String[] )
//	21/03/2003	duma	added		->	leftPad(), leftPadCh(), rightPad(), rightPadCh() for int values
//	20/12/2003	duma	changed	->	replaceAll() to use a StringBuffer
//	20/12/2003	duma	added		->	count()
//	06/02/2004	duma	added		->	asStrings()
//	07/03/2004	duma	added		->	EMPTY_STRING_ARRAY and EMPTY_STRING
//	02/04/2004	duma	bugfix	->	cutHead() with separator.length() != 1
//	11/06/2004	duma	added		->	isNullOrEmpty(String), isNullOrBlank(String)
//	19/06/2004	duma	added		->	isNullOrEmpty(String[])
//	27/12/2004	mdu		added		->	asString(Collection), asString(Collection, String)
//	14/01/2005	mdu		bugfix	->	substrings("1.2.3",".") was wrong
//	06/05/2005	mdu		added		->	copy(), copyWithout()
//	28/05/2005	mdu		bugfix	->	getDelimitedSubstring()
//	22/12/2005	mdu		added		->	asSortedStrings() methods, copy()
//	27/07/2006	mdu		changed	->	no lazy initialization for singleton (better performance)
//	21/01/2007	mdu		added		->	addAll()
//	14/02/2007	mdu		added		->	replaceEach()
//	22/02/2008	mdu		changed	->	leftPadCh() methods to handle negative values correctly
//	01/06/2008	mdu		added		->	parts() method based on IStringPair[]
//	26/07/2009	mdu		added		->	translate()
//	10/02/2010	mdu		added		->	Java 1.5
//  10/11/2010  mdu   added   ->  isTrue(), isFalse(), notNullOrEmpty(), notNullOrBlank() 
//  03/01/2011  mdu   added   ->  isAnyNullOrBlank(), isNoneNullOrBlank(), trim()
//	23/06/2011	mdu		added		->	isInteger(), isLong(), asInteger(), asLong()
//	16/11/2013	mdu		added		->  hexToBytes()
//
// Copyright (c) 1999-2013, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORT
// ===========================================================================
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.CharacterIterator;
import java.text.Collator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.pf.bif.text.IStringPair;

/**
 * The sole instance of this class provides several convenience methods
 * for string manipulation such as substring replacement or character 
 * repetition.
 * <br>
 * It also provides many convenience methods to split up strings and to
 * handle string arrays. 
 * <p>
 * Get the instance by calling <i>StringUtil.current()</i>.
 *
 * @author Manfred Duchrow
 * @version 4.4
 */
public class StringUtil
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	/** Constant for the space character **/
	public static final char CH_SPACE = ' ';
	/** Constant for the new line character **/
	public static final char CH_NEWLINE = '\n';
	/** Constant for the carriage return character **/
	public static final char CH_CR = '\r';
	/** Constant for the tabulator character **/
	public static final char CH_TAB = '\t';

	/** Constant for the String representation of the space character **/
	public static final String STR_SPACE = " ";
	/** Constant for the String representation of the new line character **/
	public static final String STR_NEWLINE = "\n";
	/** Constant for the String representation of the carriage return character **/
	public static final String STR_CR = "\r";
	/** Constant for the String representation of the tabulator character **/
	public static final String STR_TAB = "\t";
	/** Constant that defines the separator for package elements and classes (".") **/
	public static final String STR_PACKAGE_SEPARATOR = ".";
	/** Constant that defines the separator for inner classes in full qualified class names ("$") **/
	public static final String STR_INNER_CLASS_SEPARATOR = "$";

	/** An empty String to avoid multiple creation of such an object */
	public static final String EMPTY_STRING = "";
	/** An empty String array to avoid multiple creation of such an object */
	public static final String[] EMPTY_STRING_ARRAY = new String[0];

	/** Defines the delimiter string pair for two quotes: " " */
	public static final IStringPair DELIMITER_QUOTE = new StringPair("\"");
	/** Defines the delimiter string pair for two apostrophes: ' ' */
	public static final IStringPair DELIMITER_APOS = new StringPair("'");
	/** Defines the delimiter string pair for two apostrophes: ' ' */
	public static final IStringPair[] DEFAULT_TEXT_DELIMITERS = new IStringPair[] { DELIMITER_QUOTE, DELIMITER_APOS };

	private static final String WORD_DELIM = STR_SPACE + STR_TAB + STR_NEWLINE + STR_CR;

	private static final String DEFAULT_STRING_SEPARATOR = ",";

	private static final StringUtil singleton = new StringUtil();
	// =========================================================================
	// CLASS VARIABLES
	// =========================================================================

	// =========================================================================
	// PUBLIC CLASS METHODS
	// =========================================================================
	/**
	 * Returns the one and only instance of this class.
	 */
	public static StringUtil current()
	{
		return singleton;
	} // current() 

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Creates new instance 
	 */
	protected StringUtil()
	{
		super();
	} // StringUtil() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Adds all given strings to the specified collection.
	 * All null values in the string array will be skipped.
	 * 
	 * @param collection The collection to which the strings are added
	 * @param strings The strings to add to the collection
	 */
	public void addAll(Collection<String> collection, String[] strings)
	{
		this.addAll(collection, strings, false);
	} // addAll() 

	// -------------------------------------------------------------------------

	/**
	 * Adds all given strings to the specified collection, if they are not 
	 * already in the collection.
	 * All null values in the string array will be skipped.
	 * 
	 * @param collection The collection to which the strings are added
	 * @param strings The strings to add to the collection
	 */
	public void addAllNew(Collection<String> collection, String[] strings)
	{
		this.addAll(collection, strings, true);
	} // addAllNew() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string with all found oldSubStr replaced by newSubStr.   <br>
	 *
	 * Example: StringUtil.current().replaceAll( "Seven of ten", "even", "ix" ) ;<br>
	 * results in: "Six of ten"
	 *
	 * @param sourceStr The string that should be checked for occurrences of oldSubStr
	 * @param oldSubStr The string that is searched for in sourceStr
	 * @param newSubStr The new string that is placed everywhere the oldSubStr was found
	 * @return The original string with all found substrings replaced by new strings
	 */
	public String replaceAll(String sourceStr, String oldSubStr, String newSubStr)
	{
		String part = null;
		StringBuffer result = null;
		int index = -1;
		int subLen = 0;

		result = new StringBuffer(sourceStr.length());
		subLen = oldSubStr.length();
		part = sourceStr;
		while ((part.length() > 0) && (subLen > 0))
		{
			index = part.indexOf(oldSubStr);
			if (index >= 0)
			{
				result.append(part.substring(0, index));
				result.append(newSubStr);
				part = part.substring(index + subLen);
			}
			else
			{
				result.append(part);
				part = EMPTY_STRING;
			}
		} // while

		return result.toString();
	} // replaceAll() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given sourceString with all characters occurring in oldChars
	 * replaced by the characters at the corresponding index in newChars.
	 * <br/>
	 * This is faster and more convenient than calling sourceString.replace( oldChar, newChar)
	 * for each character to replace.
	 * <p/>
	 * Example: replaceEach( "(Test):77", "():7", "[]/9" ) ==> "[Test]/99"
	 * <p/>
	 * If oldChars or newChars is null or empty the source string will be returned
	 * unchanged. The strings oldChars and newChars should have the same length.
	 * However, this method also works with different lengths. 
	 * 
	 * @param sourceStr The string in which to replace the characters
	 * @param oldChars The characters that must be replaced in sourceString
	 * @param newChars The characters that must be used instead of the old characters
	 * 
	 * @return A new string with the old characters replaced by the new characters
	 */
	public String replaceEach(String sourceStr, String oldChars, String newChars)
	{
		int index;
		char[] srcChars;
		char[] destArray;
		char ch;

		if (this.isNullOrEmpty(sourceStr) || this.isNullOrEmpty(oldChars) || this.isNullOrEmpty(newChars))
		{
			return sourceStr;
		}
		destArray = new char[sourceStr.length()];
		srcChars = sourceStr.toCharArray();
		for (int i = 0; i < srcChars.length; i++)
		{
			ch = srcChars[i];
			index = oldChars.indexOf(ch);
			if ((index >= 0) && (index < newChars.length()))
			{
				ch = newChars.charAt(index);
			}
			destArray[i] = ch;
		}
		return new String(destArray);
	} // replaceEach() 

	// -------------------------------------------------------------------------

	/**
	 * Returns how often the given sub string occurs in the source String.   <br>
	 *
	 * Example: StringUtil.current().count( "Seven of ten", "en" ) ;<br>
	 * returns: 2
	 *
	 * @param sourceStr The string that should be checked for occurrences of subStr (must not be null)
	 * @param subStr The string that is searched for in sourceStr (must not be null)
	 * @return The number of occurrences of subStr in sourceStr
	 */
	public int count(String sourceStr, String subStr)
	{
		String part = null;
		int counter = 0;
		int index = 0;
		int subLen = 0;

		subLen = subStr.length();
		part = sourceStr;
		while ((part.length() > 0) && (subLen > 0))
		{
			index = part.indexOf(subStr);
			if (index >= 0)
			{
				counter++;
				part = part.substring(index + subLen);
			}
			else
			{
				part = EMPTY_STRING;
			}
		} // while

		return counter;
	} // count() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string with size of count and all characters initialized with ch.   <br>
	 *
	 * @param ch the character to be repeated in the result string.
	 * @param count the number of times the given character should occur in the result string.
	 * @return A string containing <i>count</i> characters <i>ch</i>.
	 */
	public String repeat(char ch, int count)
	{
		StringBuffer buffer = null;

		buffer = new StringBuffer(count);
		for (int i = 1; i <= count; i++)
		{
			buffer.append(ch);
		}

		return (buffer.toString());
	} // repeat() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of substrings of the given text.    <br>
	 * The delimiters between the substrings are the whitespace characters
	 * SPACE, NEWLINE, CR and TAB.
	 *
	 * @see #parts(String, String)
	 * @param text The string that should be split up into whitespace separated words
	 * @return An array of substrings of the given text
	 */
	public String[] words(String text)
	{
		return this.parts(text, WORD_DELIM);
	} // words() 

	// -------------------------------------------------------------------------

	/**
	 * Splits the given string at each occurrence of the specified separator.
	 * 
	 * @param string The string to be split up.
	 * @param separator The separator (whole value is used as separator not the single characters). Must not be null.
	 * @return The split-up elements or null if the given string was null.
	 * @throws IllegalArgumentException if the given separator is null or empty.
	 */
	public String[] split(final String string, final String separator) 
	{
		if (string == null)
		{
			return null;
		}
		if (this.isNullOrEmpty(separator))
		{
			throw new IllegalArgumentException("The separator must not be null or empty!");
		}
		int sepIndex;
		String remainingString = string;
		List<String> elements = new ArrayList<String>();
		sepIndex = remainingString.indexOf(separator); 
		while (sepIndex >= 0)
		{
			if (sepIndex > 0)
			{
				String str = remainingString.substring(0, sepIndex);
				elements.add(str);				
			}
			int startPos = sepIndex + separator.length();
			if (startPos < remainingString.length())
			{
				remainingString = remainingString.substring(startPos);
				sepIndex = remainingString.indexOf(separator); 				
			}
			else
			{
				remainingString = "";
				sepIndex = -1;
			}
		}		
		if (remainingString.length() > 0)
		{			
			elements.add(remainingString);		
		}
				
		return this.asStrings(elements);
	} // split()
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns an array of substrings of the given text.    <br>
	 * The separators between the substrings are the given delimiters.
	 * Each character in the separators string is treated as a separator.
	 * <br>
	 * All consecutive separators are treated as one separator, that is there
	 * will be no empty strings in the result.
	 * <br>
	 * It is also ensured that all strings in the result array have no leading
	 * or trailing white space characters.
	 *
	 * @see #parts(String, String)
	 * @see #allParts(String, String)
	 * @see #substrings(String, String)
	 * @see #allSubstrings(String, String)
	 * @param text The string that should be split up into substrings
	 * @param separators All characters that should be recognized as a separator of substrings
	 * @return An array of substrings of the given text
	 */
	public String[] trimmedParts(String text, String separators)
	{
		String[] result;
		List list;
		String str;

		result = this.parts(text, separators);
		if (result == null)
			return null;

		list = new ArrayList(result.length);
		for (int i = 0; i < result.length; i++)
		{
			str = result[i].trim();
			if (str.length() > 0)
			{
				list.add(str);
			}
		}
		return this.asStrings(list);
	} // trimmedParts() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of substrings of the given text.    <br>
	 * The separators between the substrings are the given delimiters.
	 * Each character in the separators string is treated as a separator.
	 * <br>
	 * All consecutive separators are treated as one separator, that is there
	 * will be no empty strings in the result.
	 *
	 * @see #parts(String, String, char)
	 * @see #allParts(String, String)
	 * @see #substrings(String, String)
	 * @see #allSubstrings(String, String)
	 * @param text The string that should be split up into substrings
	 * @param separators All characters that should be recognized as a separator of substrings
	 * @return An array of substrings of the given text
	 */
	public String[] parts(String text, String separators)
	{
		return this.parts(text, separators, false);
	} // parts() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of substrings of the given text.    <br>
	 * The separators between the substrings are the given separators.
	 * Each character in the separators string is treated as a separator.
	 * <br>
	 * All consecutive separators are treated as one separator, so there
	 * will be no empty strings in the result.
	 * <p>
	 * Examples:
	 * <p>
	 * parts( "A,B,C", ",", '*' )  --> { "A", "B", "C" }     <br>
	 * parts( "A,*B,C*", ",", '*' )  --> { "A", "B,C" }     <br>
	 * parts( "%A,B;C%;D;E", ";,", '%' )  --> { "A,B;C", "D", "E" }     <br>
	 *
	 * @see #parts(String, String)
	 * @see #allParts(String, String)
	 * @see #substrings(String, String)
	 * @see #allSubstrings(String, String)
	 * @param text The string that should be split up into substrings
	 * @param separators All characters that should be recognized as a separator of substrings
	 * @param quoteChar A character that is used to enclose string that might contain a separator
	 * @return An array of substrings of the given text
	 */
	public String[] parts(String text, String separators, char quoteChar)
	{
		return this.quotedParts(text, separators, new char[] { quoteChar }, false);
	} // parts() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of substrings of the given text.    <br>
	 * The separators between the substrings are the given separators.
	 * Each character in the separators string is treated as a separator.
	 * <br>
	 * All consecutive separators are treated as one separator, so there
	 * will be no empty strings in the result.
	 * <p>
	 * The quote characters define that strings enclosed in a pair of such a
	 * quote character can contain separators and will not be split up.
	 * <p>
	 *
	 * @see #parts(String, String)
	 * @see #allParts(String, String)
	 * @see #substrings(String, String)
	 * @see #allSubstrings(String, String)
	 * @param text The string that should be split up into substrings
	 * @param separators All characters that should be recognized as a separator of substrings
	 * @param quoteChars All characters that can be used to enclose strings that might contain a separator
	 * @return An array of substrings of the given text
	 */
	public String[] parts(String text, String separators, char[] quoteChars)
	{
		return this.quotedParts(text, separators, quoteChars, false);
	} // parts() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of substrings of the given text.    <br>
	 * The separators between the substrings are the given separators.
	 * Each character in the separators string is treated as a separator.
	 * <br>
	 * All consecutive separators are treated as one separator, so there
	 * will be no empty strings in the result.
	 * <p>
	 * The quote pairs define that strings enclosed by the characters of such a
	 * quote pair can contain separators and will not be split up.
	 */
	public String[] parts(String text, String separators, IStringPair[] quotePairs)
	{
		return this.quotedParts(text, separators, quotePairs, false);
	} // parts() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of substrings of the given text.    <br>
	 * The separators between the substrings are specified in parameter separators.
	 * Each character in the separators string is treated as a separator.
	 * <br>
	 * For each separator that is followed immediately by another separator an
	 * empty string will be added to the result. There are no empty strings added
	 * to the result for a delimiter at the very beginning of at the very end.
	 * <p>
	 * Examples:
	 * <p>
	 * allParts( "/A/B//", "/" )  --> { "A", "B", "" }     <br>
	 * allParts( "/A,B/C;D", ",;/" )  --> { "A", "B", "C", "D" }     <br>
	 * allParts( "A/B,C/D", "," )  --> { "A/B", "C/D" }     <br>
	 *
	 * @see #parts(String, String)
	 * @see #substrings(String, String)
	 * @see #allSubstrings(String, String)
	 * @param text The string that should be split up into substrings
	 * @param separators All characters that should be recognized as a separator of substrings
	 * @return An array of substrings of the given text
	 */
	public String[] allParts(String text, String separators)
	{
		return this.parts(text, separators, true);
	} // allParts() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of substrings of the given text.    <br>
	 * The separators between the substrings are specified in parameter separators.
	 * Each character in the separators string is treated as a separator.
	 * <br>
	 * For each separator that is followed immediately by another separator an
	 * empty string will be added to the result. There are no empty strings added
	 * to the result for a delimiter at the very beginning or at the very end.
	 * <p>
	 * Examples:
	 * <p>
	 * allParts( "/A/B//'C/D'", "/", '\'' )  --> { "A", "B", "", "C/D" }     <br>
	 * allParts( "/A,\"B/C\";,;D", ",;/", '"' )  --> { "A", "B/C", "", "", "D" }     <br>
	 *
	 * @see #parts(String, String)
	 * @see #substrings(String, String)
	 * @see #allSubstrings(String, String)
	 * @param text The string that should be split up into substrings
	 * @param separators All characters that should be recognized as a separator of substrings
	 * @param quoteChar A character that is used to enclose string that might contain a separator
	 * @return An array of substrings of the given text
	 */
	public String[] allParts(String text, String separators, char quoteChar)
	{
		return this.quotedParts(text, separators, new char[] { quoteChar }, true);
	} // allParts() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of substrings of the given text.    <br>
	 * The separators between the substrings are specified in parameter separators.
	 * Each character in the separators string is treated as a separator.
	 * <br>
	 * For each separator that is followed immediately by another separator an
	 * empty string will be added to the result. There are no empty strings added
	 * to the result for a delimiter at the very beginning or at the very end.
	 * <p>
	 * The quote character define the delimiters to be used to enclose sub-strings
	 * that contain separators but must not be split-up into different parts.
	 *
	 * @see #parts(String, String)
	 * @see #substrings(String, String)
	 * @see #allSubstrings(String, String)
	 * @param text The string that should be split up into substrings
	 * @param separators All characters that should be recognized as a separator of substrings
	 * @param quoteChars The characters that are used to enclose strings that might contain a separator
	 * @return An array of substrings of the given text
	 */
	public String[] allParts(String text, String separators, char[] quoteChars)
	{
		return this.quotedParts(text, separators, quoteChars, true);
	} // allParts() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given text split up into an array of strings, at
	 * the occurrences of the separator string.
	 * 
	 * In contrary to method parts() the separator is a one or many
	 * character sequence delimiter. That is, only the exact sequence 
	 * of the characters in separator identifies the end of a substring.
	 * Subsequent occurrences of separator will be skipped. Therefore
	 * no empty strings ("") will be in the result array.
	 * 
	 * @see #allSubstrings(String, String)
	 * @see #parts(String, String)
	 * @see #allParts(String, String)
	 * @param text The text to be split up
	 * @param separator The string that separates the substrings
	 * @return An array of substrings not containing any separator anymore
	 */
	public String[] substrings(String text, String separator)
	{
		return this.substrings(text, separator, false);
	} // substrings() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given text split up into an array of strings, at
	 * the occurrences of the separator string.
	 * 
	 * In contrary to method allParts() the separator is a one or many
	 * character sequence delimiter. That is, only the exact sequence 
	 * of the characters in separator identifies the end of a substring.
	 * Subsequent occurrences of separator are not skipped. They are added
	 * as empty strings to the result.
	 * 
	 * @see #substrings(String, String)
	 * @see #parts(String, String)
	 * @see #allParts(String, String)
	 * @param text The text to be split up
	 * @param separator The string that separates the substrings
	 * @return An array of substrings not containing any separator anymore
	 */
	public String[] allSubstrings(String text, String separator)
	{
		return this.substrings(text, separator, true);
	} // allSubstrings() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the first substring that is enclosed by one of the specified 
	 * delimiter pairs. It automatically chooses the delimiters that are
	 * found first from left to right.  
	 * <br>
	 * The delimiters are not included in the return string.
	 *
	 * @param text The input string that contains the delimited part.
	 * @param delimiters The start and end delimiters to be looked for.
	 * @return The substring or if no delimiters are found, an empty string.
	 */
	public String getDelimitedSubstring(String text, IStringPair[] delimiters)
	{
		int start = Integer.MAX_VALUE;
		int stop = 0;
		int delimiterLength = 0;
		int startIndex;
		int stopIndex;
		String startDelimiter;
		String endDelimiter;
		String subStr = EMPTY_STRING;

		if ((text != null) && (delimiters != null))
		{
			for (int i = 0; i < delimiters.length; i++)
			{
				startDelimiter = delimiters[i].getString1();
				startIndex = text.indexOf(startDelimiter);
				if ((startIndex >= 0) && (startIndex < start))
				{
					endDelimiter = delimiters[i].getString2();
					stopIndex = text.indexOf(endDelimiter, startIndex + startDelimiter.length());
					if (stopIndex > startIndex)
					{
						start = startIndex;
						stop = stopIndex;
						delimiterLength = startDelimiter.length();
					}
				}
			}
			if ((delimiterLength > 0) && (stop > start))
			{
				subStr = text.substring(start + delimiterLength, stop);
			}
		}
		return subStr;
	} // getDelimitedSubstring() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the first substring that is enclosed by the specified 
	 * delimiter pair.  
	 * <br>
	 * The delimiters are not included in the return string.
	 *
	 * @param text The input string that contains the delimited part.
	 * @param delimiter The start and end delimiter to be looked for.
	 * @return The substring or if no delimiters are found, an empty string.
	 */
	public String getDelimitedSubstring(String text, IStringPair delimiter)
	{
		return this.getDelimitedSubstring(text, new IStringPair[] { delimiter });
	} // getDelimitedSubstring() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the first substring that is enclosed by the specified delimiters.  
	 * <br>
	 * The delimiters are not included in the return string.
	 * <p>
	 * Example:<br>
	 * getDelimitedSubstring( "This {placeholder} belongs to me", "{", "}" )
	 * --> returns "placeholder"
	 *
	 * @param text The input string that contains the delimited part
	 * @param startDelimiter The start delimiter of the substring
	 * @param endDelimiter The end delimiter of the substring
	 * @return The substring or an empty string, if no delimiters are found.
	 */
	public String getDelimitedSubstring(String text, String startDelimiter, String endDelimiter)
	{
		return this.getDelimitedSubstring(text, new StringPair(startDelimiter, endDelimiter));
	} // getDelimitedSubstring() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the first substring that is enclosed by the specified delimiter.  <br>
	 * The delimiters are not included in the return string.
	 * <p>
	 * Example:<br>
	 * getDelimitedSubstring( "File 'text.txt' not found.", "'", "'" )
	 * --> returns "text.txt"
	 *
	 * @param text The input string that contains the delimited part
	 * @param delimiter The start and end delimiter of the substring
	 * @return The substring or an empty string, if no delimiters are found.
	 */
	public String getDelimitedSubstring(String text, String delimiter)
	{
		return this.getDelimitedSubstring(text, delimiter, delimiter);
	} // getDelimitedSubstring() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that does not contain the optional enclosing delimiters
	 * " or '.
	 * If the given text is null, "" will be returned.
	 * <p>
	 * Examples see {@link #getString(String, IStringPair[])}.
	 * 
	 * @param text The input string that optionally is enclosed in one of the delimiter pairs.
	 * @return The string derived from the input text (never null).
	 */
	public String getString(String text)
	{
		return this.getString(text, DEFAULT_TEXT_DELIMITERS);
	} // getString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that does not contain the optional enclosing delimiters
	 * specified in the second parameter.
	 * If the given text is null, "" will be returned.
	 * <p>
	 * Examples see {@link #getString(String, IStringPair[])}.
	 * 
	 * @param text The input string that optionally is enclosed in one of the delimiter pairs.
	 * @param delimiter The start and end delimiter to be looked for.
	 * @return The string derived from the input text (never null).
	 */
	public String getString(String text, IStringPair delimiter)
	{
		return this.getString(text, new IStringPair[] { delimiter });
	} // getString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that does not contain the optional enclosing delimiters
	 * specified in the second parameter.
	 * If the given text is null, "" will be returned.
	 * <p>
	 * Examples:
	 * <br>"  'The quick brown fox'", StringPair("'") --> "The quick brown fox"
	 * <br>"  last minute ", StringPair("'") --> "last minute"
	 * <br>"     ", StringPair("'") --> ""
	 * <br>" '  '  ", StringPair("'") --> "  "
	 * <br>" 'Paul Newman '  ", StringPair("\"") --> "'Paul Newman '"
	 * <br>" \"Paul Newman \"  ", StringPair("\"") --> "Paul Newman"
	 * 
	 * @param text The input string that optionally is enclosed in one of the delimiter pairs.
	 * @param delimiters The start and end delimiter to be looked for.
	 * @return The string derived from the input text (never null).
	 */
	public String getString(String text, IStringPair[] delimiters)
	{
		String value;

		if (this.isNullOrBlank(text))
		{
			return EMPTY_STRING;
		}
		value = text.trim();
		for (int i = 0; i < delimiters.length; i++)
		{
			if (value.startsWith(delimiters[i].getString1()) && value.endsWith(delimiters[i].getString2()))
			{
				return this.getDelimitedSubstring(value, delimiters[i]);
			}
		}
		return value;
	} // getString() 

	// -------------------------------------------------------------------------

	/**
	 * Prints the stack trace of the specified Throwable to a
	 * string and returns it.
	 */
	public String stackTrace(Throwable throwable)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		pw.close();
		return sw.toString();
	} // stackTrace() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string filled (on the left) up to the specified
	 * length with the given character.     <br>
	 * Example: leftPadCh( "12", 6, '0' ) --> "000012"
	 */
	public String leftPadCh(String str, int len, char ch)
	{
		return this.padCh(str, len, ch, true);
	} // leftPadCh() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string filled (on the left) up to the specified
	 * length with spaces.     <br>
	 * Example: leftPad( "XX", 4 ) --> "  XX"
	 * 
	 * @param str The string that has to be filled up to the specified length
	 * @param len The length of the result string
	 */
	public String leftPad(String str, int len)
	{
		return this.leftPadCh(str, len, CH_SPACE);
	} // leftPad() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given integer as string filled (on the left) up to the 
	 * specified length with the given fill character.     <br>
	 * Example: <br>
	 * leftPad( 24, 5, '*' ) --> "***24"   <br>
	 * leftPadCh( -288, 8, '#' ) --> "-####288"
	 */
	public String leftPadCh(int value, int len, char fillChar)
	{
		if (value < 0)
		{
			StringBuffer buffer = new StringBuffer(len);
			buffer.append('-');
			buffer.append(this.leftPadCh(Math.abs(value), len - 1, fillChar));
			return buffer.toString();
		}
		return this.leftPadCh(Integer.toString(value), len, fillChar);
	} // leftPadCh() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given integer as string filled (on the left) up to the 
	 * specified length with zeroes.     <br>
	 * Example: leftPad( 12, 4 ) --> "0012"
	 */
	public String leftPad(int value, int len)
	{
		return this.leftPadCh(value, len, '0');
	} // leftPad() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given long as string filled (on the left) up to the 
	 * specified length with the given fill character.     <br>
	 * Negative values will also be filled-up but the minus character will
	 * be the most left character. <br>
	 * Examples: 
	 * leftPadCh( 24L, 5, '*' ) --> "***24"    <br>
	 * leftPadCh( -109L, 8, '0' ) --> "-0000109"
	 */
	public String leftPadCh(long value, int len, char fillChar)
	{
		if (value < 0L)
		{
			StringBuffer buffer = new StringBuffer(len);
			buffer.append('-');
			buffer.append(this.leftPadCh(Math.abs(value), len - 1, fillChar));
			return buffer.toString();
		}
		return this.leftPadCh(Long.toString(value), len, fillChar);
	} // leftPadCh() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given long as string filled (on the left) up to the 
	 * specified length with the given fill character.     <br>
	 * Negative values will also be filled-up but the minus character will
	 * be the most left character. <br>
	 * Examples: 
	 * leftPadCh( 24L, 5, '*' ) --> "***24"    <br>
	 * leftPadCh( -109L, 8, '0' ) --> "-0000109"
	 * 
	 * @param buffer The buffer to append the result to.
	 * @param value The value to fill-up to the specified length.
	 * @param len The length of the resulting string.
	 * @param fillChar The character to use for filling up.
	 */
	public void leftPadCh(StringBuffer buffer, long value, int len, char fillChar)
	{
		if (value < 0L)
		{
			buffer.append('-');
			this.leftPadCh(buffer, Math.abs(value), len - 1, fillChar);
		}
		else
		{
			this.padCh(buffer, Long.toString(value), len, fillChar, true);
		}
	} // leftPadCh() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given long as string filled (on the left) up to the 
	 * specified length with zeroes.     <br>
	 * Example: leftPad( 12L, 4 ) --> "0012"
	 */
	public String leftPad(long value, int len)
	{
		return this.leftPadCh(value, len, '0');
	} // leftPad() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string filled (on the right) up to the specified
	 * length with the given character.     <br>
	 * Example: rightPadCh( "34", 5, 'X' ) --> "34XXX"
	 */
	public String rightPadCh(String str, int len, char ch)
	{
		return this.padCh(str, len, ch, false);
	} // rightPadCh() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string filled (on the right) up to the specified
	 * length with spaces.     <br>
	 * Example: rightPad( "88", 6 ) --> "88    "
	 */
	public String rightPad(String str, int len)
	{
		return this.rightPadCh(str, len, CH_SPACE);
	} // rightPad() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given integer as string filled (on the right) up to the 
	 * specified length with the given character.     <br>
	 * Example: rightPad( 32, 4, '#' ) --> "32##"
	 */
	public String rightPadCh(int value, int len, char fillChar)
	{
		return this.rightPadCh(Integer.toString(value), len, fillChar);
	} // rightPadCh() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given integer as string filled (on the right) up to the 
	 * specified length with spaces.     <br>
	 * Example: rightPad( 17, 5 ) --> "17   "
	 */
	public String rightPad(int value, int len)
	{
		return this.rightPadCh(value, len, CH_SPACE);
	} // rightPad() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given long as string filled (on the right) up to the 
	 * specified length with the given character.     <br>
	 * Example: rightPad( 32L, 7, '#' ) --> "32#####"
	 */
	public String rightPadCh(long value, int len, char fillChar)
	{
		return this.rightPadCh(Long.toString(value), len, fillChar);
	} // rightPadCh() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given integer as string filled (on the right) up to the 
	 * specified length with spaces.     <br>
	 * Example: rightPad( 233L, 5 ) --> "233  "
	 */
	public String rightPad(long value, int len)
	{
		return this.rightPadCh(value, len, CH_SPACE);
	} // rightPad() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string filled equally left and right
	 * up to the specified length with the given character.     <br>
	 * Example: centerCh( "A", 5, '_' ) --> "__A__"  <br>
	 * Example: centerCh( "XX", 7, '+' ) --> "++XX+++"
	 */
	public String centerCh(String str, int len, char ch)
	{
		String buffer = null;
		int missing = len - str.length();
		int half = 0;

		if (missing <= 0)
			return str;

		half = missing / 2;
		buffer = this.rightPadCh(str, len - half, ch);
		return this.leftPadCh(buffer, len, ch);
	} // centerCh() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string filled (on the right and right)
	 * up to the specified length with spaces.     <br>
	 * Example: center( "Mike", 10 ) --> "   Mike   "
	 */
	public String center(String str, int len)
	{
		return this.centerCh(str, len, CH_SPACE);
	} // center() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given string array extended by one element
	 * that hold the specified string.
	 */
	public String[] append(String[] strings, String string)
	{
		String[] appStr = { string };
		return this.append(strings, appStr);
	} // append() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of strings that contains all strings given
	 * by the first and second string array. The strings from the 
	 * second array will be added at the end of the first array.
	 * 
	 * @param strings The array of string to which to append
	 * @param appendStrings The string to be appended to the first array
	 */
	public String[] append(String[] strings, String[] appendStrings)
	{
		String[] newStrings = null;

		if (strings == null)
			return appendStrings;

		if (appendStrings == null)
			return strings;

		newStrings = new String[strings.length + appendStrings.length];
		System.arraycopy(strings, 0, newStrings, 0, strings.length);
		System.arraycopy(appendStrings, 0, newStrings, strings.length, appendStrings.length);

		return newStrings;
	} // append() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of strings that contains all strings given
	 * in the first plus the specified string to append, if it is not
	 * already in the given array.
	 */
	public String[] appendIfNotThere(String[] strings, String appendString)
	{
		if (this.contains(strings, appendString))
			return strings;
		else
			return this.append(strings, appendString);
	} // appendIfNotThere() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of strings that contains all strings given
	 * in the first plus all strings of the second array that are not
	 * already in the first array.
	 */
	public String[] appendIfNotThere(String[] strings, String[] appendStrings)
	{
		String[] newStrings = strings;

		if (appendStrings == null)
			return newStrings;

		for (int i = 0; i < appendStrings.length; i++)
		{
			newStrings = this.appendIfNotThere(newStrings, appendStrings[i]);
		}
		return newStrings;
	} // appendIfNotThere() 

	// -------------------------------------------------------------------------

	/**
	 * Removes all string of the second array from the first array.
	 * Returns a new array of string that contains all remaining strings of the
	 * original strings array.
	 * 
	 * @param strings The array from which to remove the strings
	 * @param removeStrings The strings to be removed
	 */
	public String[] remove(String[] strings, String[] removeStrings)
	{
		if ((strings == null) || (removeStrings == null) || (strings.length == 0) || (removeStrings.length == 0))
		{
			return strings;
		}

		return this.removeFromStringArray(strings, removeStrings);
	} // remove() 

	// -------------------------------------------------------------------------

	/**
	 * Removes the given string from the specified string array.
	 * Returns a new array of string that contains all remaining strings of the
	 * original strings array.
	 * 
	 * @param strings The array from which to remove the string
	 * @param removeString The string to be removed
	 */
	public String[] remove(String[] strings, String removeString)
	{
		String[] removeStrings = { removeString };

		return this.remove(strings, removeStrings);
	} // remove() 

	// -------------------------------------------------------------------------

	/**
	 * Removes all null values from the given string array.
	 * Returns a new string array that contains all none null values of the 
	 * input array.
	 * 
	 * @param strings The array to be cleared of null values
	 */
	public String[] removeNull(String[] strings)
	{
		if (strings == null)
			return strings;

		return this.removeFromStringArray(strings, null);
	} // removeNull() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that contains all given strings concatenated
	 * and separated by comma.
	 *
	 * @param strings The array of strings that should be concatenated
	 * @return One string containing the concatenated strings separated by comma (",")
	 */
	public String asString(String[] strings)
	{
		return this.asString(strings, DEFAULT_STRING_SEPARATOR);
	} // asString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that contains all given strings concatenated
	 * and separated by the specified separator.
	 *
	 * @param strings The array of strings that should be concatenated
	 * @param separator The separator between the strings (if null the default is used)
	 * @return One string containing the concatenated strings separated by separator
	 */
	public String asString(String[] strings, String separator)
	{
		StringBuffer buffer = null;
		String stringSeparator;

		if (strings == null)
			return null;

		stringSeparator = (separator == null) ? DEFAULT_STRING_SEPARATOR : separator;

		buffer = new StringBuffer(strings.length * 20);
		if (strings.length > 0)
		{
			buffer.append(strings[0]);
			for (int i = 1; i < strings.length; i++)
			{
				buffer.append(stringSeparator);
				if (strings[i] != null)
					buffer.append(strings[i]);
			}
		}
		return buffer.toString();
	} // asString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that contains all strings from the given collection 
	 * concatenated and separated by comma.
	 *
	 * @param strings The collection of strings that should be concatenated
	 * @return One string containing the concatenated strings separated by comma (",")
	 */
	public String asString(Collection strings)
	{
		return this.asString(strings, DEFAULT_STRING_SEPARATOR);
	} // asString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that contains all strings from the given collection 
	 * concatenated and separated by the specified separator.
	 *
	 * @param strings The collection of strings that should be concatenated
	 * @param separator The separator between the strings
	 * @return One string containing the concatenated strings separated by separator
	 */
	public String asString(Collection strings, String separator)
	{
		StringBuffer buffer = null;
		Iterator iter;

		if (strings == null)
		{
			return EMPTY_STRING;
		}

		buffer = new StringBuffer(strings.size() * 20);
		if (strings.size() > 0)
		{
			iter = strings.iterator();
			while (iter.hasNext())
			{
				if (buffer.length() > 0)
				{
					buffer.append(separator);
				}
				buffer.append(iter.next());
			}
		}
		return buffer.toString();
	} // asString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that contains all given strings sorted (in ascending order), 
	 * concatenated and separated by the specified separator.<br>
	 * The comparator used is the java.text.Collator for the default locale.
	 * <p>
	 * Example: asSortedString( new String[] {"Carmen","Rose","Anna","Rita"} )<br>
	 * returns "Anna,Carmen,Rita,Rose" 
	 *
	 * @param strings The array of strings that should be concatenated
	 * @return One string containing the concatenated strings separated by separator
	 */
	public String asSortedString(final String[] strings)
	{
		return this.asSortedString(strings, DEFAULT_STRING_SEPARATOR);
	} // asSortedString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that contains all given strings sorted (in ascending order), 
	 * concatenated and separated by the specified separator.<br>
	 * The comparator used is the java.text.Collator for the default locale.
	 * <p>
	 * Example: asSortedString( new String[] {"Mike","Ben","Gil"}, "/")<br>
	 * returns "Ben/Gil/Mike" 
	 *
	 * @param strings The array of strings that should be concatenated
	 * @param separator The separator between the strings (must not be null)
	 * @return One string containing the concatenated strings separated by separator
	 */
	public String asSortedString(final String[] strings, final String separator)
	{
		return this.asSortedString(strings, separator, true);
	} // asSortedString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that contains all given strings sorted, concatenated
	 * and separated by the specified separator.<br>
	 * The comparator used is the java.text.Collator for the default locale.
	 * <p>
	 * Example: asSortedString( new String[] {"Fred","Sam","Joe"}, ";", false)<br>
	 * returns "Sam;Joe;Fred" 
	 *
	 * @param strings The array of strings that should be concatenated
	 * @param separator The separator between the strings (must not be null)
	 * @param ascending If true the strings are sorted ascending otherwise descending
	 * @return One string containing the concatenated strings separated by separator
	 */
	public String asSortedString(final String[] strings, final String separator, boolean ascending)
	{
		Comparator comparator;

		if (ascending)
		{
			comparator = Collator.getInstance();
		}
		else
		{
			comparator = new ReverseComparator(Collator.getInstance());
		}
		return this.asSortedString(strings, separator, comparator);
	} // asSortedString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that contains all given strings sorted, concatenated
	 * and separated by the specified separator.
	 *
	 * @param strings The array of strings that should be concatenated
	 * @param separator The separator between the strings (must not be null)
	 * @param comparator A comparator that is used to compare the strings when sorting (must not be null)
	 * @return One string containing the sorted, concatenated strings separated by separator
	 */
	public String asSortedString(final String[] strings, final String separator, final Comparator comparator)
	{
		String[] array;

		if (strings == null)
		{
			return null;
		}

		array = this.copy(strings);
		Arrays.sort(array, comparator);
		return this.asString(array, separator);
	} // asSortedString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string array containing all elements of the given collection.
	 * Of course the elements must be strings, otherwise ClassCastException
	 * will be thrown.  <br>
	 * If the given collection is null, the result is null as well.
	 * 
	 * @param collection A collection of strings
	 */
	public String[] asStrings(Collection<String> collection)
	{
		if (collection == null)
		{
			return null;
		}
		return (String[])collection.toArray(new String[collection.size()]);
	} // asStrings() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string array containing all elements of the given enumeration.
	 * Of course the elements must be strings, otherwise ClassCastException
	 * will be thrown.  <br>
	 * If the given enumeration is null, the result is null as well.
	 * 
	 * @param enumeration An enumeration of strings
	 */
	public String[] asStrings(Enumeration<String> enumeration)
	{
		List<String> list;

		if (enumeration == null)
		{
			return null;
		}
		list = new ArrayList<String>(50);
		while (enumeration.hasMoreElements())
		{
			list.add(enumeration.nextElement());
		}
		return this.asStrings(list);
	} // asStrings() 

	// -------------------------------------------------------------------------

	/**
	 * Converts the given map to a string utilizing the given separators.
	 * If the given elementSeparator is null it defaults to ','.
	 * If the given keyValueSeparator is null it defaults to '='.
	 * On all keys and values from the given map the toString() method gets called
	 * to add the string representation to the result.
	 * If the given map is null the result will also be null.
	 * If the given map is empty the result string will be an empty string.
	 * <p/>
	 * A result string might look like this: "alpha=200,beta=872,delta=N/A"
	 *
	 * @param map The map that contains the key-value pairs
	 * @param elementSeparator The separator between the elements in the string representation
	 * @param keyValueSeparator The separator between the keys and values in the string representation
	 * @see #toMap(String, String, String, Map)
	 */
	public String asString(Map map, String elementSeparator, String keyValueSeparator)
	{
		StringBuffer buffer;
		String elemSep;
		String keyValSep;
		Iterator iter;
		Map.Entry entry;

		if (map == null)
		{
			return null;
		}
		if (map.isEmpty())
		{
			return EMPTY_STRING;
		}
		elemSep = (elementSeparator == null) ? "," : elementSeparator;
		keyValSep = (keyValueSeparator == null) ? "=" : keyValueSeparator;
		buffer = new StringBuffer(map.size() * 30);
		iter = map.entrySet().iterator();
		while (iter.hasNext())
		{
			entry = (Map.Entry)iter.next();
			if (buffer.length() > 0)
			{
				buffer.append(elemSep);
			}
			buffer.append(entry.getKey().toString());
			buffer.append(keyValSep);
			buffer.append(entry.getValue().toString());
		}
		return buffer.toString();
	} // asString() 

	// -------------------------------------------------------------------------

	/**
	 * Converts the given map to a string utilizing the given separator.
	 * If the given elementSeparator is null it defaults to ','.
	 * The separator between keys and values will be '='.
	 * On all keys and values from the given map the toString() method gets called
	 * to add the string representation to the result.
	 * If the given map is null the result will also be null.
	 * If the given map is empty the result string will be an empty string.
	 * <p/>
	 * A result string might look like this: "alpha=200,beta=872,delta=N/A"
	 *
	 * @param map The map that contains the key-value pairs
	 * @param elementSeparator The separator between the elements in the string representation
	 * @see #asString(Map, String, String)
	 * @see #toMap(String, String, String, Map)
	 */
	public String asString(Map map, String elementSeparator)
	{
		return asString(map, elementSeparator, null);
	} // asString() 

	// -------------------------------------------------------------------------

	/**
	 * Converts the given map to a string.
	 * The separator between the elements will be ','.
	 * The separator between keys and values will be '='.
	 * On all keys and values from the given map the toString() method gets called
	 * to add the string representation to the result.
	 * If the given map is null the result will also be null.
	 * If the given map is empty the result string will be an empty string.
	 * <p/>
	 * A result string might look like this: "alpha=200,beta=872,delta=N/A"
	 *
	 * @param map The map that contains the key-value pairs
	 * @see #asString(Map, String, String)
	 * @see #toMap(String, String, String, Map)
	 */
	public String asString(Map map)
	{
		return asString(map, null, null);
	} // asString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the index of the first string in the given string array that
	 * matches the specified string pattern.
	 * If no string is found in the array the result is -1.
	 *
	 * @param strArray An array of string (may contain null elements)
	 * @param pattern The pattern the searched string must match
	 * @return The index of the matching string in the array or -1 if not found
	 */
	public int indexOf(String[] strArray, StringPattern pattern)
	{
		if ((strArray == null) || (strArray.length == 0))
			return -1;

		boolean found = false;
		for (int i = 0; i < strArray.length; i++)
		{
			if (strArray[i] == null)
			{
				if (pattern == null)
					found = true;
			}
			else
			{
				if (pattern != null)
					found = pattern.matches(strArray[i]);
			}
			if (found)
				return i;
		}
		return -1;
	} // indexOf() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the index of the specified string in the given string array.
	 * It returns the index of the first occurrence of the string.
	 * If the string is not found in the array the result is -1.
	 * The comparison of the strings is case-sensitive!
	 *
	 * @param strArray An array of string (may contain null elements)
	 * @param searchStr The string to be looked up in the array (null allowed)
	 * @return The index of the string in the array or -1 if not found
	 */
	public int indexOf(String[] strArray, String searchStr)
	{
		return this.indexOfString(strArray, searchStr, false);
	} // indexOf() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the index of the specified string in the given string array.
	 * It returns the index of the first occurrence of the string.
	 * If the string is not found in the array the result is -1.
	 * The comparison of the strings is case-insensitive!
	 *
	 * @param strArray An array of string (may contain null elements)
	 * @param searchStr The string to be looked up in the array (null allowed)
	 * @return The index of the string in the array or -1 if not found
	 */
	public int indexOfIgnoreCase(String[] strArray, String searchStr)
	{
		return this.indexOfString(strArray, searchStr, true);
	} // indexOfIgnoreCase() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the index of the specified string in the given string array.
	 * It returns the index of the first occurrence of the string.
	 * If the string is not found in the array the result is -1.
	 * The comparison of the strings is case-insensitive if the ignoreCase
	 * parameter is true.
	 *
	 * @param strArray An array of string (may contain null elements)
	 * @param searchStr The string to be looked up in the array (null allowed)
	 * @param ignoreCase If true, do case-insensitive comparison.
	 * @return The index of the string in the array or -1 if not found
	 */
	public int indexOfString(String[] strArray, String searchStr, boolean ignoreCase)
	{
		if ((strArray == null) || (strArray.length == 0))
		{
			return -1;
		}

		boolean found = false;
		for (int i = 0; i < strArray.length; i++)
		{
			if (strArray[i] == null)
			{
				if (searchStr == null)
				{
					found = true;
				}
			}
			else
			{
				if (ignoreCase)
				{
					found = strArray[i].equalsIgnoreCase(searchStr);
				}
				else
				{
					found = strArray[i].equals(searchStr);
				}
			}
			if (found)
			{
				return i;
			}
		}
		return -1;
	} // indexOfString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the index of the specified character in the given char array.
	 * It returns the index of the first occurrence of the character.
	 * If the character is not found in the array the result is -1.
	 *
	 * @param charArray An array of string (may contain null elements)
	 * @param ch The string to be looked up in the array (null allowed)
	 * @param ignoreCase If comparison should be case-insensitive
	 * @return The index of the character in the array or -1 if not found
	 */
	public int indexOf(char[] charArray, char ch, boolean ignoreCase)
	{
		char upper_ch = Character.toUpperCase(ch);

		for (int i = 0; i < charArray.length; i++)
		{
			if (ignoreCase)
			{
				if (Character.toUpperCase(charArray[i]) == upper_ch)
				{
					return i;
				}
			}
			else
			{
				if (charArray[i] == ch)
				{
					return i;
				}
			}
		}
		return -1;
	} // indexOf() 

	// -------------------------------------------------------------------------

	/**
	 * Returns whether or not the specified string can be found
	 * in the given string array.
	 *
	 * @param strArray An array of string (may contain null elements)
	 * @param searchStr The string to be looked up in the array (null allowed)
	 * @param ignoreCase Defines whether or not the comparison is case-sensitive.
	 * @return true, if the specified array contains the given string
	 */
	public boolean contains(String[] strArray, String searchStr, boolean ignoreCase)
	{
		if (ignoreCase)
			return this.containsIgnoreCase(strArray, searchStr);
		else
			return this.contains(strArray, searchStr);
	} // contains() 

	// -------------------------------------------------------------------------

	/**
	 * Returns whether or not a string can be found in the given string array
	 * that matches the specified string pattern.
	 *
	 * @param strArray An array of string (may contain null elements)
	 * @param pattern The string pattern to match against in the array (null allowed)
	 * @return true, if the specified array contains a string matching the pattern
	 */
	public boolean contains(String[] strArray, StringPattern pattern)
	{
		return (this.indexOf(strArray, pattern) >= 0);
	} // contains() 

	// -------------------------------------------------------------------------

	/**
	 * Returns whether or not the specified string can be found
	 * in the given string array.
	 * The comparison of the strings is case-sensitive!
	 *
	 * @param strArray An array of string (may contain null elements)
	 * @param searchStr The string to be looked up in the array (null allowed)
	 * @return true, if the specified array contains the given string
	 */
	public boolean contains(String[] strArray, String searchStr)
	{
		return (this.indexOf(strArray, searchStr) >= 0);
	} // contains() 

	// -------------------------------------------------------------------------

	/**
	 * Returns whether or not the specified string can be found
	 * in the given string array.
	 * The comparison of the strings is case-insensitive!
	 *
	 * @param strArray An array of string (may contain null elements)
	 * @param searchStr The string to be looked up in the array (null allowed)
	 * @return true, if the specified array contains the given string
	 */
	public boolean containsIgnoreCase(String[] strArray, String searchStr)
	{
		return (this.indexOfIgnoreCase(strArray, searchStr) >= 0);
	} // containsIgnoreCase() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all elements of string array <i>from</i> in a new array
	 * from index <i>start</i> up to the end.
	 * If start index is larger than the array's length, an empty array will be
	 * returned.
	 * 
	 * @param from The string array the elements should be copied from
	 * @param start Index of the first element to copy
	 */
	public String[] copyFrom(String[] from, int start)
	{
		if (from == null)
			return null;

		return this.copyFrom(from, start, from.length - 1);
	} // copyFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all elements of string array <i>from</i> in a new array
	 * from index start up to index end (inclusive).
	 * If end is larger than the last valid index, it will be reduced
	 * to the last index.
	 * If end index is less than start index, an empty array will be
	 * returned.
	 * 
	 * @param from The string array the elements should be copied from
	 * @param start Index of the first element to copy
	 * @param end Index of last element to be copied
	 */
	public String[] copyFrom(String[] from, int start, int end)
	{
		String[] result;
		int count;
		int stop = end;

		if (from == null)
			return null;

		if (stop > (from.length - 1))
			stop = from.length - 1;

		count = stop - start + 1;

		if (count < 1)
			return new String[0];

		result = new String[count];

		System.arraycopy(from, start, result, 0, count);

		return result;
	} // copyFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new array that contains all strings of the given array that
	 * matched the specified filter.
	 * 
	 * @param strings The string array to copy from (must not be null)
	 * @param filter The filter that determines which strings to copy (if null an empty array will be returned)
	 */
	public String[] copy(String[] strings, StringFilter filter)
	{
		Collection result;

		result = this.copyStrings(ArrayList.class, strings, filter, true);
		return this.asStrings(result);
	} // copy() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a copy (new array) that contains all the strings of the given array
	 */
	public String[] copy(String... strings)
	{
		String[] newStrings;

		if (strings == null)
		{
			return null;
		}

		newStrings = new String[strings.length];
		System.arraycopy(strings, 0, newStrings, 0, strings.length);
		return newStrings;
	} // copy() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new array that contains all strings of the given array that
	 * do NOT match the specified filter.
	 * 
	 * @param strings The string array to copy from (must not be null)
	 * @param filter The filter that determines which strings to copy (if null an empty array will be returned)
	 */
	public String[] copyWithout(String[] strings, StringFilter filter)
	{
		Collection result;

		result = this.copyStrings(ArrayList.class, strings, filter, false);
		return this.asStrings(result);
	} // copyWithout() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new array that contains all strings of the given array that
	 * matched the specified filter.
	 * 
	 * @param strings A collection of strings to copy from (must not be null)
	 * @param filter The filter that determines which strings to copy (if null an empty collection will be returned)
	 */
	public Collection copy(Collection strings, StringFilter filter)
	{
		return this.copyStrings(strings.getClass(), this.asStrings(strings), filter, true);
	} // copy() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new collection that contains all strings of the given collection 
	 * that do NOT match the specified filter.
	 * 
	 * @param strings A collection of strings to copy from (must not be null)
	 * @param filter The filter that determines which strings to copy (if null an empty collection will be returned)
	 */
	public Collection copyWithout(Collection strings, StringFilter filter)
	{
		return this.copyStrings(strings.getClass(), this.asStrings(strings), filter, false);
	} // copyWithout() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the portion of the given string that comes before the last
	 * occurrence of the specified separator.    <br>
	 * If the separator could not be found in the given string, then the
	 * string is returned unchanged.
	 * <p>
	 * Examples:
	 * <p>
	 * cutTail( "A/B/C", "/" ) ;   // returns "A/B"
	 * <br>
	 * cutTail( "A/B/C", "," ) ;   // returns "A/B/C"
	 * <p>
	 * @see #prefix( String, String )
	 * @see #suffix( String, String )
	 * @see #cutHead( String, String )
	 * @see #startingFrom( String, String )
	 * @see #upTo( String, String )
	 * @param text The string from which to cut off the tail
	 * @param separator The separator from where to cut off
	 * @return the string without the separator and without the characters after the separator
	 */
	public String cutTail(String text, String separator)
	{
		int index;

		if ((text == null) || (separator == null))
		{
			return text;
		}

		index = text.lastIndexOf(separator);
		if (index < 0)
			return text;

		return text.substring(0, index);
	} // cutTail() 

	// ------------------------------------------------------------------------

	/**
	 * Returns the portion of the given string that comes after the last
	 * occurrence of the specified separator.    <br>
	 * If the separator could not be found in the given string, then the
	 * string is returned unchanged.
	 * <p>
	 * Examples:
	 * <p>
	 * cutHead( "A/B/C", "/" ) ;   // returns "C"
	 * <br>
	 * cutHead( "A/B/C", "," ) ;   // returns "A/B/C"
	 * <p>
	 * @see #prefix( String, String )
	 * @see #cutTail( String, String )
	 * @see #suffix( String, String )
	 * @see #startingFrom( String, String )
	 * @see #upTo( String, String )
	 * @param text The string from which to cut off the head
	 * @param separator The separator up to which to cut off
	 * @return the string without the separator and without the characters before the separator
	 */
	public String cutHead(String text, String separator)
	{
		int index;

		if ((text == null) || (separator == null) || (separator.length() == 0))
		{
			return text;
		}

		index = text.lastIndexOf(separator);
		if (index < 0)
		{
			return text;
		}

		return text.substring(index + separator.length());
	} // cutHead() 

	// ------------------------------------------------------------------------

	/**
	 * Returns a string array with two elements where the first is the attribute
	 * name and the second is the attribute value.
	 * Splits the given string at the first occurrence of separator and returns
	 * the piece before the separator in element 0 and the piece after the 
	 * separator in the returned array.
	 * If the separator is not found, the first element contains the full
	 * string and the second an empty string.
	 * 
	 * @param str The string that contains the name-value pair
	 * @param separator The separator between name and value
	 */
	public String[] splitNameValue(String str, String separator)
	{
		IStringPair pair;

		pair = this.splitStringPair(str, separator);
		return pair.asArray();
	} // splitNameValue() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string pair where the first is the string before the first
	 * occurrence of the defined separator and the second string is everything
	 * after that separator.
	 * If the separator is not found, the first element contains the full
	 * string and the second an empty string.
	 * 
	 * @param str The string that contains the name-value pair
	 * @param separator The separator between name and value
	 */
	public IStringPair splitStringPair(String str, String separator)
	{
		StringPair result;
		int index;

		result = new StringPair(EMPTY_STRING);
		if (str != null)
		{
			index = str.indexOf(separator);
			if (index > 0)
			{
				result.setString1(str.substring(0, index));
				result.setString2(str.substring(index + separator.length()));
			}
			else
			{
				result.setString1(str);
			}
		}
		return result;
	} // splitStringPair() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the substring of the given string that comes before the
	 * first occurrence of the specified separator.
	 * If the string starts with a separator, the result will be an empty string.
	 * If the string doesn't contain the separator the method returns null.
	 * <p>
	 * Examples:
	 * <p>
	 * prefix( "A/B/C", "/" ) ;   // returns "A"
	 * <br>
	 * prefix( "A/B/C", "," ) ;   // returns null
	 * <p>
	 * @see #suffix( String, String )
	 * @see #cutTail( String, String )
	 * @see #cutHead( String, String )
	 * @see #startingFrom( String, String )
	 * @see #upTo( String, String )
	 * @param str The string of which the prefix is desired
	 * @param separator Separates the prefix from the rest of the string
	 */
	public String prefix(String str, String separator)
	{
		return this.prefix(str, separator, true);
	} // prefix() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the substring of the given string that comes after the
	 * first occurrence of the specified separator.
	 * If the string ends with a separator, the result will be an empty string.
	 * If the string doesn't contain the separator the method returns null.
	 * <p>
	 * Examples:
	 * <p>
	 * suffix( "A/B/C", "/" ) ;   // returns "B/C"
	 * <br>
	 * suffix( "A/B/C", "," ) ;   // returns null
	 * <p>
	 * @see #prefix( String, String )
	 * @see #cutTail( String, String )
	 * @see #cutHead( String, String )
	 * @see #startingFrom( String, String )
	 * @see #upTo( String, String )
	 * @param str The string of which the suffix is desired
	 * @param separator Separates the suffix from the rest of the string
	 */
	public String suffix(String str, String separator)
	{
		return this.suffix(str, separator, true);
	} // suffix() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the substring of the given string that comes before the
	 * first occurrence of the specified separator.
	 * If the string starts with a separator, the result will be an empty string.
	 * If the string doesn't contain the separator the method returns the
	 * whole string unchanged.
	 * <p>
	 * Examples:
	 * <p>
	 * upTo( "A/B/C", "/" ) ;   // returns "A"
	 * <br>
	 * upTo( "A/B/C", "," ) ;   // returns "A/B/C"
	 * <br>
	 * upTo( "/A/B/C", "/" ) ;   // returns ""
	 * <p>
	 * @see #prefix( String, String )
	 * @see #cutTail( String, String )
	 * @see #cutHead( String, String )
	 * @see #startingFrom( String, String )
	 * @see #suffix( String, String )
	 * @param str The string of which the prefix is desired
	 * @param separator Separates the prefix from the rest of the string
	 */
	public String upTo(String str, String separator)
	{
		return this.prefix(str, separator, false);
	} // upTo() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the substring of the given string that comes after the
	 * first occurrence of the specified separator.
	 * If the string doesn't contain the separator the method returns the
	 * whole string unchanged.
	 * <p>
	 * Examples:
	 * <p>
	 * startingFrom( "A/B/C", "/" ) ;   // returns "B/C"
	 * <br>
	 * startingFrom( "A/B/C", "," ) ;   // returns "A/B/C"
	 * <p>
	 * @see #prefix( String, String )
	 * @see #cutTail( String, String )
	 * @see #cutHead( String, String )
	 * @see #suffix( String, String )
	 * @see #upTo( String, String )
	 * @param str The string of which the suffix is desired
	 * @param separator Separates the suffix from the rest of the string
	 */
	public String startingFrom(String str, String separator)
	{
		return this.suffix(str, separator, false);
	} // startingFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Trimming with null checking.
	 * Returns the given string with leading and trailing white spaces removed.
	 * If the given string is null then null will be returned.
	 */
	public String trim(String str)
	{
		return (str == null) ? null : str.trim();
	} // trim() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a string that contains all characters of the given string in
	 * reverse order.
	 */
	public String reverse(String str)
	{
		if (str == null)
			return null;

		char[] newStr = new char[str.length()];
		StringCharacterIterator iterator = new StringCharacterIterator(str);
		int i = 0;

		for (char ch = iterator.last(); ch != CharacterIterator.DONE; ch = iterator.previous())
		{
			newStr[i] = ch;
			i++;
		}
		return new String(newStr);
	} // reverse() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given map with new entries from the specified String.
	 * If the specified map is null a new empty java.util.Hashtable will be 
	 * created.
	 * <br>
	 * The string is split up into elements separated by the elementSeparator
	 * parameter. If this parameter is null the default separator "," is used.
	 * <br>
	 * After that each part is split up to a key-value pair separated by the
	 * keyValueSeparator parameter. If this parameter is null the default "=" is
	 * used.
	 * <br>
	 * Then the key-value pairs are added to the map and the map is returned.
	 * <p>
	 * <b>Be aware that all leading and trailing white spaces of keys and values
	 * will be removed!</b>
	 * 
	 * @param str The string that contains the list of key-value pairs
	 * @param elementSeparator The separator between the elements of the list
	 * @param keyValueSeparator The separator between the keys and values
	 * @param map The map to which the key-value pairs are added
	 */
	public Map toMap(String str, String elementSeparator, String keyValueSeparator, Map map)
	{
		Map result;
		String elemSep;
		String kvSep;
		String[] assignments;
		String[] nameValue;

		if (str == null)
			return map;

		result = (map == null ? new Hashtable() : map);
		elemSep = (elementSeparator == null) ? "," : elementSeparator;
		kvSep = (keyValueSeparator == null) ? "=" : keyValueSeparator;

		assignments = this.parts(str, elemSep);
		for (int i = 0; i < assignments.length; i++)
		{
			nameValue = this.splitNameValue(assignments[i], kvSep);
			nameValue[0] = nameValue[0].trim();
			nameValue[1] = nameValue[1].trim();
			if (nameValue[0].length() > 0)
				result.put(nameValue[0], nameValue[1]);
		}

		return result;
	} // toMap() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new map object that contains all key-value pairs of the
	 * specified string. 
	 * <br>
	 * The separator between the elements is assumed to be ","
	 * and "=" between key and value.
	 * <p>
	 * Example:<br>
	 * "main=Fred,support1=John,support2=Stella,manager=Oscar"
	 * <p>
	 * <b>Be aware that all leading and trailing white spaces of keys and values
	 * will be removed!</b>
	 * 
	 * @param str The string with the list of key-value pairs
	 */
	public Map<String, String> asMap(String str)
	{
		return this.toMap(str, null, null, null);
	} // asMap() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new map object that contains all key-value pairs of the
	 * specified string. 
	 * <br>
	 * The separator between the keys and values is assumed to be "=".
	 * <p>
	 * <b>Be aware that all leading and trailing white spaces of keys and values
	 * will be removed!</b>
	 * 
	 * @param str The string that contains the list of key-value pairs
	 * @param elementSeparator The separator between the elements of the list
	 */
	public Map asMap(String str, String elementSeparator)
	{
		return this.toMap(str, elementSeparator, null, null);
	} // asMap() 

	// -------------------------------------------------------------------------	

	/**
	 * Returns a new map object that contains all key-value pairs of the
	 * specified string. 
	 * <p>
	 * <b>Be aware that all leading and trailing white spaces of keys and values
	 * will be removed!</b>
	 * 
	 * @param str The string that contains the list of key-value pairs
	 * @param elementSeparator The separator between the elements of the list
	 * @param keyValueSeparator The separator between the keys and values
	 */
	public Map<String, String> asMap(String str, String elementSeparator, String keyValueSeparator)
	{
		return this.toMap(str, elementSeparator, keyValueSeparator, null);
	} // asMap() 

	// -------------------------------------------------------------------------	

	/**
	 * Returns the given map object with all key-value pairs of the
	 * specified string added to it.
	 * <br>
	 * The separator between the keys and values is assumed to be "=".
	 * <p>
	 * <b>Be aware that all leading and trailing white spaces of keys and values
	 * will be removed!</b>
	 * 
	 * @param str The string that contains the list of key-value pairs
	 * @param elementSeparator The separator between the elements of the list
	 * @param map The map to which the key-value pairs are added
	 */
	public Map toMap(String str, String elementSeparator, Map map)
	{
		return this.toMap(str, elementSeparator, null, map);
	} // toMap() 

	// -------------------------------------------------------------------------	

	/**
	 * Adds all key-value pairs of the given string to the specified map.
	 * <br>
	 * The separator between the elements is assumed to be ","
	 * and "=" between key and value.
	 * <p>
	 * <b>Be aware that all leading and trailing white spaces of keys and values
	 * will be removed!</b>
	 * 
	 * @param str The string that contains the list of key-value pairs
	 * @param map The map to which the key-value pairs are added
	 */
	public Map toMap(String str, Map map)
	{
		return this.toMap(str, null, null, map);
	} // toMap() 

	// -------------------------------------------------------------------------	

	/**
	 * Adds all key-value pairs of the given string to a new properties object.
	 * <br>
	 * The separator between the elements is assumed to be ","
	 * and "=" between key and value.
	 * <p>
	 * <b>Be aware that all leading and trailing white spaces of keys and values
	 * will be removed!</b>
	 * 
	 * @param str The string that contains the list of key-value pairs
	 */
	public Properties asProperties(String str)
	{
		return this.toProperties(str, null);
	} // asProperties() 

	// -------------------------------------------------------------------------	

	/**
	 * Adds all key-value pairs of the given string to the specified properties.
	 * <br>
	 * The separator between the elements is assumed to be ","
	 * and "=" between key and value.
	 * <p>
	 * <b>Be aware that all leading and trailing white spaces of keys and values
	 * will be removed!</b>
	 * 
	 * @param str The string that contains the list of key-value pairs
	 * @param properties The properties where the key-value pairs should be added
	 */
	public Properties toProperties(String str, Properties properties)
	{
		Properties props = (properties == null) ? new Properties() : properties;
		return (Properties)this.toMap(str, null, null, props);
	} // toProperties() 

	// -------------------------------------------------------------------------	

	/**
	 * Returns true if any one of the given strings is null or blank.
	 * Blanks means "" as well as strings filled with white spaces only (e.g. "\t  ").
	 */
	public boolean isAnyNullOrBlank(String... strings)
	{
		if (strings == null)
		{
			return true;
		}
		for (String string : strings)
		{
			if (this.isNullOrBlank(string))
			{
				return true;
			}
		}
		return false;
	} // isAnyNullOrBlank() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if none of the given strings is null or blank.
	 * Blanks means "" as well as strings filled with white spaces only (e.g. "\t  ").
	 */
	public boolean isNoneNullOrBlank(String... strings)
	{
		return !this.isAnyNullOrBlank(strings);
	} // isNoneNullOrBlank() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given string array is null or empty
	 * 
	 * @param strings The string array to check
	 */
	public boolean isNullOrEmpty(String[] strings)
	{
		return ((strings == null) || (strings.length == 0));
	} // isNullOrEmpty() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given string is null or empty
	 * 
	 * @param str The string to check
	 */
	public boolean isNullOrEmpty(String str)
	{
		return ((str == null) || (str.length() == 0));
	} // isNullOrEmpty() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given string is null or empty or consists of 
	 * whitespace characters only.
	 * @param str The string to check
	 */
	public boolean isNullOrBlank(String str)
	{
		return ((str == null) || (str.trim().length() == 0));
	} // isNullOrBlank() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given string array is not null and not empty
	 *
	 * @param strings The string array to check
	 */
	public boolean notNullOrEmpty(String[] strings)
	{
		return !this.isNullOrEmpty(strings);
	} // notNullOrEmpty() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given string is not null and not empty
	 *
	 * @param str The string to check
	 */
	public boolean notNullOrEmpty(String str)
	{
		return !this.isNullOrEmpty(str);
	} // notNullOrEmpty() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given string is not null and not empty and does not 
	 * consist of whitespace characters only.
	 * @param str The string to check
	 */
	public boolean notNullOrBlank(String str)
	{
		return !this.isNullOrBlank(str);
	} // notNullOrBlank() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new string that contains only the given character.
	 * 
	 * @param ch The character to build the string from
	 */
	public String asString(char ch)
	{
		char[] chars = { ch };

		return new String(chars);
	} // asString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if all elements of the first given array are in the 
	 * second given array and the length of both arrays are equal.
	 */
	public boolean areEqual(String[] strings1, String[] strings2)
	{
		return this.equals(strings1, strings2, false);
	} // areEqual() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if all elements of the first given array are in the 
	 * second given array and the length of both arrays are equal.
	 * The comparison of the strings will be case-insensitive. 
	 */
	public boolean areEqualIgnoreCase(String[] strings1, String[] strings2)
	{
		return this.equals(strings1, strings2, true);
	} // areEqualIgnoreCase() 

	// -------------------------------------------------------------------------

	/**
	 * Sets the given extension as filename extension to the given filename.
	 * If the filename already has an extension it depends on parameter replace
	 * whether or not it will be replaced by the new one.
	 * If the given extension is null or empty the filename will be returned
	 * unchanged.
	 * 
	 * @param filename The filename to which the extension must be appended
	 * @param extension The extension to append without any leading dot (e.g. "pdf")
	 * @param replace If true any existing extension will be replaced
	 * @return A filename with the given extension at the end
	 */
	public String setFileNameExtension(String filename, String extension, boolean replace)
	{
		StringBuffer buffer;

		if ((filename == null) || (this.isNullOrBlank(extension)))
		{
			return filename;
		}
		buffer = new StringBuffer(filename.length() + extension.length() + 1);
		if (filename.indexOf('.') < 0)
		{
			buffer.append(filename);
		}
		else
		{
			if (replace)
			{
				buffer.append(this.cutTail(filename, "."));
			}
			else
			{
				buffer.append(filename);
			}
		}
		buffer.append('.');
		buffer.append(extension);
		return buffer.toString();
	} // setFileNameExtension() 

	// -------------------------------------------------------------------------

	/**
	 * Modifies the strings in the given array to be all upper case.
	 */
	public void toUpperCase(String[] strings)
	{
		if (strings == null)
		{
			return;
		}
		for (int i = 0; i < strings.length; i++)
		{
			strings[i] = strings[i].toUpperCase();
		}
	} // toUpperCase() 

	// -------------------------------------------------------------------------

	/**
	 * Modifies the strings in the given array to be all lower case.
	 */
	public void toLowerCase(String[] strings)
	{
		if (strings == null)
		{
			return;
		}
		for (int i = 0; i < strings.length; i++)
		{
			strings[i] = strings[i].toLowerCase();
		}
	} // toLowerCase() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a copy of the given string array where all elements 
	 * are converted to upper case. This method does not modify the given
	 * input array.
	 */
	public String[] copyUpperCase(String[] strings)
	{
		String[] copy;

		copy = this.copy(strings);
		this.toUpperCase(copy);
		return copy;
	} // copyUpperCase() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a copy of the given string array where all elements 
	 * are converted to lower case. This method does not modify the given
	 * input array.
	 */
	public String[] copyLowerCase(String[] strings)
	{
		String[] copy;

		copy = this.copy(strings);
		this.toLowerCase(copy);
		return copy;
	} // copyLowerCase() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the package name of the given full qualified class name.
	 * <br>
	 * Example: getPackageName("org.pf.text.StringUtil") -> "org.pf.text" 
	 * 
	 * @param qualifiedClassName The full qualified class name (must not be null)
	 */
	public String getPackageName(String qualifiedClassName)
	{
		if (qualifiedClassName.indexOf(STR_PACKAGE_SEPARATOR) >= 0)
		{
			return this.cutTail(qualifiedClassName, STR_PACKAGE_SEPARATOR);
		}
		return StringUtil.EMPTY_STRING;
	} // getPackageName() 

	// -------------------------------------------------------------------------	

	/**
	 * Returns the class name without the package name.
	 * <br>
	 * Example: getUnqualifiedClassName("org.pf.text.StringUtil") -> "StringUtil" 
	 * 
	 * @param qualifiedClassName The full qualified class name (must not be null)
	 */
	public String getUnqualifiedClassName(String qualifiedClassName)
	{
		return this.cutHead(qualifiedClassName, STR_PACKAGE_SEPARATOR);
	} // getUnqualifiedClassName() 

	// -------------------------------------------------------------------------	

	/**
	 * Returns a string of the same length as the given text parameter.
	 * 
	 * This method operates on each character of text as follows: If a character in text 
	 * is found in charsToReplace, the character in replacementChars that corresponds to 
	 * that in charsToReplace is copied to the result; otherwise, the character in text 
	 * is copied directly to the result. 
	 * If charsToReplace contains duplicates, the leftmost occurrence is used. 
	 * replacementChars is padded with blanks, or truncated, on the right to match the 
	 * length of charsToReplace.
	 *  
	 * @param text to be searched for possible translation of its characters. (must not be null) 
	 * @param replacementChars containing the translation values of characters. (must not be null) 
	 * @param charsToReplace containing the characters that are to be translated. (must not be null)
	 * @throws NullPointerException if any of the three parameters is null
	 */
	public String translate(String text, String replacementChars, String charsToReplace)
	{
		char[] textChars;
		StringBuffer buffer;
		int index;

		if (replacementChars.length() > charsToReplace.length())
		{
			replacementChars = replacementChars.substring(0, charsToReplace.length());
		}
		else
		{
			if (replacementChars.length() < charsToReplace.length())
			{
				replacementChars = this.rightPadCh(replacementChars, charsToReplace.length(), StringUtil.CH_SPACE);
			}
		}

		buffer = new StringBuffer(text.length());
		textChars = text.toCharArray();
		for (int i = 0; i < textChars.length; i++)
		{
			index = charsToReplace.indexOf(textChars[i]);
			if (index < 0)
			{
				buffer.append(textChars[i]);
			}
			else
			{
				buffer.append(replacementChars.charAt(index));
			}
		}
		return buffer.toString();
	} // translate() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given string represents a <em>true</em> setting.
	 * That is, its value is one of strings "true", "on", "yes", "1".
	 * In any other case (also for null) it returns false.
	 */
	public boolean isTrue(String string)
	{
		if (string == null)
		{
			return false;
		}
		return "true".equalsIgnoreCase(string) || "on".equalsIgnoreCase(string) || "yes".equalsIgnoreCase(string)
				|| "1".equals(string);
	} // isTrue() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given string represents a <em>false</em> setting.
	 * That is, its value is one of strings "false", "off", "no", "0".
	 * In any other case (also for null) it returns false.
	 */
	public boolean isFalse(String string)
	{
		if (string == null)
		{
			return false;
		}
		return "false".equalsIgnoreCase(string) || "off".equalsIgnoreCase(string) || "no".equalsIgnoreCase(string)
				|| "0".equals(string);
	} // isFalse() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given value is a string containing only digits
	 * and optionally a leading minus ('-') and is in the range of a valid
	 * integer Integer.MIN_VALUE <= value <= Integer.MAX_VALUE.
	 */
	public boolean isInteger(String value)
	{
		char[] chars;

		if ((value.length() == 0) || (value.length() > 11))
		{
			return false;
		}
		chars = value.toCharArray();
		if (!((chars[0] == '-') || Character.isDigit(chars[0])))
		{
			return false;
		}
		for (int i = 1; i < chars.length; i++)
		{
			if (!Character.isDigit(chars[i]))
			{
				return false;
			}
		}
		// Check if it is between Integer.MIN_VALUE and Integer.MAX_VALUE
		if (value.length() > 9)
		{
			try
			{
				Integer.parseInt(value);
			}
			catch (Throwable e)
			{
				return false;
			}
		}
		return true;
	} // isInteger() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given value is a string containing only digits
	 * and optionally a leading minus ('-') and is in the range of a valid
	 * long - Long.MIN_VALUE <= value <= Long.MAX_VALUE.
	 */
	public boolean isLong(String value)
	{
		char[] chars;

		if ((value.length() == 0) || (value.length() > 20))
		{
			return false;
		}
		chars = value.toCharArray();
		if (!((chars[0] == '-') || Character.isDigit(chars[0])))
		{
			return false;
		}
		for (int i = 1; i < chars.length; i++)
		{
			if (!Character.isDigit(chars[i]))
			{
				return false;
			}
		}
		// Check if it is between Long.MIN_VALUE and Long.MAX_VALUE
		if (value.length() > 18)
		{
			try
			{
				Long.parseLong(value);
			}
			catch (Throwable e)
			{
				return false;
			}
		}
		return true;
	} // isLong() 

	// -------------------------------------------------------------------------

	/**
	 * Converts the given string to an int or returns the given default value
	 * if the string does not represent a valid integer value.
	 * All leading and trailing whites paces are removed before the given string
	 * gets converted.
	 * If the string is null the defaultValue will be returned.
	 */
	public int asInteger(String value, int defaultValue)
	{
		if (value == null)
		{
			return defaultValue;
		}
		try
		{
			return Integer.parseInt(value.trim());
		}
		catch (Throwable e)
		{
			return defaultValue;
		}
	} // asInteger() 

	// -------------------------------------------------------------------------

	/**
	 * Converts the given string to a long or returns the given default value
	 * if the string does not represent a valid long value.
	 * All leading and trailing white spaces are removed before the given string
	 * gets converted.
	 * If the string is null the defaultValue will be returned.
	 */
	public long asLong(String value, long defaultValue)
	{
		if (value == null)
		{
			return defaultValue;
		}
		try
		{
			return Long.parseLong(value.trim());
		}
		catch (Throwable e)
		{
			return defaultValue;
		}
	} // asLong() 

	// -------------------------------------------------------------------------
	
	/**
	 * Converts the given hex string to a byte array.
	 * 
	 * @param hex The hex data to convert (must not be null and must have an even length).
	 */
	public byte[] hexToBytes(String hex) 
	{
		int len;
		byte[] bytes;
		
		len = hex.length() / 2;
		bytes = new byte[len];
		int pos = 0;
		for (int i = 0; i < len; i++)
		{
			bytes[i] = (byte)Integer.parseInt(hex.substring(pos, pos+2), 16);
			pos += 2;
		}
		return bytes;
	} // hexToBytes() 
	
	// -------------------------------------------------------------------------
	/**
	 * Converts the given hex string to a byte array.
	 * 
	 * @param separator An optional separator used between the hex elements.
	 * @param hex The hex data to convert (must not be null and must have an even length).
	 */
	public byte[] hexToBytes(String hex, String separator) 
	{
		String[] hexElements;

		if (separator == null)
		{
			return this.hexToBytes(hex);
		}
		hexElements = this.split(hex, separator);
		return this.hexToBytes(this.asString(hexElements, ""));		
	}
	
	// -------------------------------------------------------------------------
	
	/**
	 * Converts the given bytes to a string with a hex representation for each byte.
	 * 
	 * @param bytes The bytes to convert to a hex string.
	 * @return A hex string representation of the given bytes or null if the bytes were null.
	 */
	public String bytesToHex(byte... bytes) 
	{
		return this.bytesToHex(null, bytes);
	} // bytesToHex() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Converts the given bytes to a string with a hex representation for each byte.
	 * If the given separator is not null it will be but between the hex values.
	 * 
	 * @param separator An optional separator.
	 * @param bytes The bytes to convert to a hex string.
	 * @return A hex string representation of the given bytes or null if the bytes were null.
	 */
	public String bytesToHex(String separator, byte... bytes) 
	{
		StringBuffer buffer;
		String sep;
		
		if (bytes == null)
		{
			return null;
		}
		sep = (separator == null) ? "" : separator;
		buffer = new StringBuffer(bytes.length * (2+sep.length()));
		for (byte b : bytes)
		{
			if (buffer.length() > 0)
			{
				buffer.append(sep);
			}
			buffer.append(String.format("%X", b));
		}
		return buffer.toString();
	} // bytesToHex() 
	
	// -------------------------------------------------------------------------
	
	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	/**
	 * Cuts off all leading and trailing occurences of separator in text.
	 */
	protected String trimSeparator(String text, String separator)
	{
		int sepLen = separator.length();

		while (text.startsWith(separator))
			text = text.substring(separator.length());

		while (text.endsWith(separator))
			text = text.substring(0, text.length() - sepLen);

		return text;
	} // trimSeparator() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array of substrings of the given text.    <br>
	 * The separators between the substrings are the given delimiters.
	 * Each character in the delimiter string is treated as a separator.
	 *
	 * @param text The string that should be split-up into substrings.
	 * @param delimiters All characters that should be recognized as a separator or substrings
	 * @param all If true, empty elements will be returned, otherwise they are skipped
	 * @return An array of substrings of the given text
	 */
	protected String[] parts(String text, String delimiters, boolean all)
	{
		ArrayList result = null;
		StringTokenizer tokenizer = null;

		if (text == null)
		{
			return null;
		}

		if ((delimiters == null) || (delimiters.length() == 0))
		{
			String[] resultArray = { text };
			return resultArray;
		}

		if (text.length() == 0)
		{
			return EMPTY_STRING_ARRAY;
		}
		else
		{
			result = new ArrayList();
			tokenizer = new StringTokenizer(text, delimiters, all);

			if (all)
			{
				this.collectParts(result, tokenizer, delimiters);
			}
			else
			{
				this.collectParts(result, tokenizer);
			}
		}
		return this.asStrings(result);
	} // parts() 

	// -------------------------------------------------------------------------

	protected void collectParts(List list, StringTokenizer tokenizer)
	{
		while (tokenizer.hasMoreTokens())
		{
			list.add(tokenizer.nextToken());
		}
	} // collectParts() 

	// -------------------------------------------------------------------------

	protected void collectParts(List list, StringTokenizer tokenizer, String delimiter)
	{
		String token;
		boolean lastWasDelimiter = false;

		while (tokenizer.hasMoreTokens())
		{
			token = tokenizer.nextToken();
			if (delimiter.indexOf(token) >= 0)
			{
				if (lastWasDelimiter)
					list.add(EMPTY_STRING);
				lastWasDelimiter = true;
			}
			else
			{
				list.add(token);
				lastWasDelimiter = false;
			}
		}
	} // collectParts() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the given text split up into an array of strings, at
	 * the occurrances of the separator string.
	 * 
	 * In contrary to method parts() the separator is a one or many
	 * character sequence delimiter. That is, only the exact sequence 
	 * of the characters in separator identifies the end of a substring.
	 * Parameter all defines whether empty strings between consecutive
	 * separators are added to the result or not.
	 * 
	 * @see #parts(String, String, boolean)
	 * @param text The text to be split up
	 * @param separator The string that separates the substrings
	 * @param all If true, empty strings are added, otherwise skipped
	 * @return An array of substrings not containing any separator anymore
	 */
	protected String[] substrings(String text, String separator, boolean all)
	{
		int index = 0;
		int start = 0;
		int sepLen = 0;
		int strLen = 0;
		String str = text;
		ArrayList strings = new ArrayList();

		if (text == null)
			return EMPTY_STRING_ARRAY;

		if ((separator == null) || (separator.length() == 0))
		{
			if (text.length() == 0)
				return EMPTY_STRING_ARRAY;

			String[] resultArray = { text };
			return resultArray;
		}

		if (!all)
			str = this.trimSeparator(text, separator);

		strLen = str.length();
		if (strLen > 0)
		{
			sepLen = separator.length();

			index = str.indexOf(separator, start);
			while (index >= 0)
			{
				if (all)
				{
					if (index > 0)
					{
						strings.add(str.substring(start, index));
					}
				}
				else
				{
					if (index >= (start + sepLen))
						strings.add(str.substring(start, index));
				}
				start = index + sepLen;
				index = str.indexOf(separator, start);
			}

			if (start < strLen)
				strings.add(str.substring(start));
		}
		return this.asStrings(strings);
	} // substrings() 

	// -------------------------------------------------------------------------

	protected String padCh(String str, int len, char ch, boolean left)
	{
		StringBuffer buffer = null;
		int missing = len - str.length();

		if (missing <= 0)
		{
			return str;
		}
		buffer = new StringBuffer(len);
		this.padCh(buffer, str, len, ch, left);
		return buffer.toString();
	} // padCh() 

	// -------------------------------------------------------------------------

	protected void padCh(final StringBuffer buffer, final String str, final int len, final char ch, final boolean left)
	{
		int missing = len - str.length();

		if (missing <= 0)
		{
			buffer.append(str);
			return;
		}

		if (!left)
		{
			buffer.append(str);
		}
		for (int i = 1; i <= missing; i++)
		{
			buffer.append(ch);
		}
		if (left)
		{
			buffer.append(str);
		}
	} // padCh() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the substring of the given string that comes before the
	 * first occurance of the specified separator.
	 * If the string starts with a separator, the result will be an empty string.
	 * If the string doesn't contain the separator the method returns null or
	 * the whole string, depending on the returnNull flag.
	 * 
	 * @param str The string of which the prefix is desired
	 * @param separator Separates the prefix from the rest of the string
	 * @param returnNull Specifies if null will be returned if no separator is found
	 */
	protected String prefix(String str, String separator, boolean returnNull)
	{
		if (str == null)
			return null;

		if (separator == null)
			return (returnNull ? null : str);

		int index = str.indexOf(separator);
		if (index >= 0)
			return str.substring(0, index);
		else
			return (returnNull ? null : str);
	} // prefix() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the substring of the given string that comes after the
	 * first occurance of the specified separator.
	 * If the string ends with a separator, the result will be an empty string.
	 * If the string doesn't contain the separator the method returns null or
	 * the whole string, depending on the returnNull flag.
	 * 
	 * @param str The string of which the suffix is desired
	 * @param separator Separates the suffix from the rest of the string
	 * @param returnNull Specifies if null will be returned if no separator is found
	 */
	protected String suffix(String str, String separator, boolean returnNull)
	{
		if (str == null)
			return null;

		if (separator == null)
			return (returnNull ? null : str);

		int index = str.indexOf(separator);
		if (index >= 0)
			return str.substring(index + separator.length());
		else
			return (returnNull ? null : str);
	} // suffix() 

	// -------------------------------------------------------------------------

	/**
	 * Removes the given strings from the array.
	 * If removeStrings is null it means that all null values are removed from
	 * the first array.
	 */
	protected String[] removeFromStringArray(String[] strings, String[] removeStrings)
	{
		List list;
		boolean remains;

		list = new ArrayList(strings.length);
		for (int i = 0; i < strings.length; i++)
		{
			if (removeStrings == null)
			{
				remains = strings[i] != null;
			}
			else
			{
				remains = !this.contains(removeStrings, strings[i]);
			}
			if (remains)
			{
				list.add(strings[i]);
			}
		}
		return (String[])list.toArray(new String[list.size()]);
	} // removeFromStringArray() 

	// -------------------------------------------------------------------------

	/**
	 * Quotes are removed from all parts
	 * If str is null an empty array will be returned!
	 * 
	 * @param all If true, empty elements will be returned, otherwise they are skipped
	 */
	protected String[] quotedParts(String str, String separators, char[] quoteChars, boolean all)
	{
		final int bufferSize = 40;
		StringBuffer buffer;
		String[] parts;
		List partList;
		StringExaminer scanner;
		char ch;
		char quoteChar = (char)-2;
		boolean insideQuotation = false;

		if (str == null)
		{
			return EMPTY_STRING_ARRAY;
		}

		if (this.isNullOrEmpty(separators))
		{
			StringPair[] delimiters = new StringPair[quoteChars.length];
			for (int i = 0; i < delimiters.length; i++)
			{
				delimiters[i] = new StringPair(this.asString(quoteChars[i]));
			}
			parts = new String[1];
			parts[0] = this.getDelimitedSubstring(str, delimiters);
		}
		else
		{
			partList = new ArrayList(30);
			scanner = new StringExaminer(str);
			buffer = new StringBuffer(bufferSize);
			while (!scanner.atEnd())
			{
				ch = scanner.nextChar();
				if (insideQuotation)
				{
					if (ch == quoteChar)
					{
						insideQuotation = false;
					}
					else
					{
						buffer.append(ch);
					}
				}
				else
				{
					if (this.indexOf(quoteChars, ch, false) >= 0)
					{
						quoteChar = ch;
						insideQuotation = true;
					}
					else
					{
						if (separators.indexOf(ch) >= 0) // Is a separator ?
						{
							if (all || (buffer.length() > 0))
							{
								partList.add(buffer.toString());
								buffer = new StringBuffer(bufferSize);
							}
						}
						else
						{
							buffer.append(ch);
						}
					}
				}
			} // while
			if (buffer.length() > 0)
			{
				partList.add(buffer.toString());
			}
			parts = this.asStrings(partList);
		}
		return parts;
	} // quotedParts() 

	// -------------------------------------------------------------------------

	protected String[] quotedParts(String str, String separators, IStringPair[] quotePairs, boolean all)
	{
		final int bufferSize = 40;
		StringBuffer buffer;
		String[] parts;
		List partList;
		StringExaminer scanner;
		IStringPair quotePair;
		char ch;

		if (str == null)
		{
			return EMPTY_STRING_ARRAY;
		}

		if (this.isNullOrEmpty(separators))
		{
			parts = new String[1];
			parts[0] = this.getDelimitedSubstring(str, quotePairs);
		}
		else
		{
			partList = new ArrayList(30);
			scanner = new StringExaminer(str);
			buffer = new StringBuffer(bufferSize);
			while (!scanner.atEnd())
			{
				ch = scanner.nextChar();
				quotePair = this.findInString1(quotePairs, ch, false);
				if (quotePair != null)
				{
					int pos = scanner.findPositionOf(quotePair.getString2());
					if (pos >= 0)
					{
						scanner.appendUpToPosition(buffer, pos);
						scanner.skip(quotePair.getString2().length());
					}
					else
					{
						buffer.append(ch);
					}
				}
				else
				{
					if (separators.indexOf(ch) >= 0) // Is a separator ?
					{
						if (all || (buffer.length() > 0))
						{
							partList.add(buffer.toString());
							buffer = new StringBuffer(bufferSize);
						}
					}
					else
					{
						buffer.append(ch);
					}
				}
			} // while
			if (buffer.length() > 0)
			{
				partList.add(buffer.toString());
			}
			parts = this.asStrings(partList);
		}
		return parts;
	} // quotedParts() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the string pair of the specified array that's first string starts
	 * with the specified character.
	 *
	 * @param stringPairs An array of string pairs
	 * @param ch The character to be looked for to be the initial character of string one of a string pair.
	 * @param ignoreCase If comparison should be case-insensitive
	 * @return The found string pair or null if not found
	 */
	protected IStringPair findInString1(IStringPair[] stringPairs, char ch, boolean ignoreCase)
	{
		char upper_ch = Character.toUpperCase(ch);
		char compChar;

		for (int i = 0; i < stringPairs.length; i++)
		{
			compChar = stringPairs[i].getString1().charAt(0);
			if (ignoreCase)
			{
				compChar = Character.toUpperCase(compChar);
				if (compChar == upper_ch)
				{
					return stringPairs[i];
				}
			}
			else
			{
				if (compChar == ch)
				{
					return stringPairs[i];
				}
			}
		}
		return null;
	} // findInString1() 

	// -------------------------------------------------------------------------

	protected Collection copyStrings(Class collClass, String[] strings, StringFilter filter, boolean allMatching)
	{
		Collection result=null;

		try
		{
			result = (Collection)collClass.getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException e)
		{
			return null;
		}
		catch (IllegalAccessException e)
		{
			return null;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		if ((strings == null) || ((filter == null) && allMatching))
		{
			return result;
		}

		for (int i = 0; i < strings.length; i++)
		{
			if (filter != null)
			{
				if (filter.matches(strings[i]))
				{
					if (allMatching)
					{
						result.add(strings[i]);
					}
				}
				else
				{
					if (!allMatching)
					{
						result.add(strings[i]);
					}
				}
			}
			else
			{
				if (!allMatching)
				{
					result.add(strings[i]);
				}
			}
		}
		return result;
	} // copyStrings() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if all elements of the first array are in the second array
	 * and the length of both arrays are equal.
	 */
	protected boolean equals(String[] strings1, String[] strings2, boolean ignoreCase)
	{
		boolean found;

		if (strings1 == strings2)
		{
			return true;
		}
		if ((strings1 == null) || (strings2 == null))
		{
			return false;
		}
		if (strings1.length != strings2.length)
		{
			return false;
		}
		for (int i = 0; i < strings1.length; i++)
		{
			if (ignoreCase)
			{
				found = this.containsIgnoreCase(strings2, strings1[i]);
			}
			else
			{
				found = this.contains(strings2, strings1[i]);
			}
			if (!found)
			{
				return false;
			}
		}
		return true;
	} // equals() 

	// -------------------------------------------------------------------------

	/**
	 * Adds all given strings to the specified collection.
	 * All null values in the string array will be skipped.
	 * 
	 * @param collection The collection to which the strings are added
	 * @param strings The strings to add to the collection
	 * @param justNew If true only new strings are added to the collection
	 */
	protected void addAll(Collection<String> collection, String[] strings, boolean justNew)
	{
		if ((collection == null) || (strings == null))
		{
			return;
		}

		for (int i = 0; i < strings.length; i++)
		{
			if (strings[i] != null)
			{
				if (justNew)
				{
					if (!collection.contains(strings[i]))
					{
						collection.add(strings[i]);
					}
				}
				else
				{
					collection.add(strings[i]);
				}
			}
		}
	} // addAll() 

	// -------------------------------------------------------------------------

} // class StringUtil 
