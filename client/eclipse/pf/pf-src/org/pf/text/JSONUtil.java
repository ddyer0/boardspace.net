// ===========================================================================
// CONTENT  : CLASS JSONUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 21/01/2012
// HISTORY  :
//  21/01/2012  mdu  CREATED
//
// Copyright (c) 2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.text ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import static org.pf.bif.text.IJSONConvertible.*;

import java.util.Map;

import org.pf.bif.text.IJSONConvertible;

/**
 * Convenience methods for JavaScript Object Notation (JSON) handling.
 * {@link  <a href="http://www.json.org/" target="blank">JSON Web Site</a>}
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class JSONUtil
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // CLASS VARIABLES
  // =========================================================================
  private static JSONUtil soleInstance = new JSONUtil() ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================

  /**
   * Returns the only instance this class supports (design pattern "Singleton")
   */
  public static JSONUtil current()
  {
    return soleInstance ;
  } // current() 

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  protected JSONUtil()
  {
    super() ;
  } // JSONUtil() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Converts the given object to a valid JSON string representation.
   * @param jsonConvertible The object to convert.
   * @return The JSON string representation
   */
	public String convertToJSON(IJSONConvertible jsonConvertible)
	{
		StringBuffer buffer;

		if (jsonConvertible == null)
		{
			return IJSONConvertible.JSON_LITERAL_NULL;
		}
		buffer = new StringBuffer(100);
		jsonConvertible.appendAsJSONString(buffer);
		return buffer.toString();
	} // convertToJSON() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Converts the given object array to a valid JSON string representation.
	 * 
	 * @param objects The object array to convert.
	 * @return The JSON string representation
	 */
	public String arrayToJSON(Object... objects)
	{
		StringBuffer buffer;
		
		buffer = new StringBuffer(50 * objects.length);
		this.appendJSONArray(buffer, objects);
		return buffer.toString();
	} // arrayToJSON() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Converts the given map to a valid JSON string representation.
	 * 
	 * @param map The map to convert.
	 * @return The JSON string representation
	 */
	public String mapToJSON(Map<String, Object> map)
	{
		StringBuffer buffer;
		
		if (map == null)
		{
			return JSON_LITERAL_NULL;
		}
		buffer = new StringBuffer(50 * map.size());
		this.appendJSONMap(buffer, map);
		return buffer.toString();
	} // mapToJSON() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the given string as JSON string literal (i.e. enclosed in quotes).
	 * 
	 * @param str The string to make JSON compatible (might by null)
	 * @return The quoted string or "null" string without quotes for str being null.
	 */
	public String toJSONStringLiteral(String str) 
	{
		if (str == null)
		{
			return JSON_LITERAL_NULL;
		}
		return JSON_STRING_DELIMITER + str + JSON_STRING_DELIMITER; 
	} // toJSONStringLiteral() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Appends the given name and value as JSON pair member to the given buffer.
	 *  
	 * @param buffer The buffer to append to
	 * @param name The name of the pair
	 * @param value The value of the pair
	 */
	public void appendJSONPair(StringBuffer buffer, String name, Object value)
	{
		this.appendJSONString(buffer, name);
		buffer.append(JSON_PAIR_SEPARATOR);
		this.appendJSONObject(buffer, value);
	} // appendJSONPair() 

	// -------------------------------------------------------------------------
	
	/**
	 * Appends the given string to the buffer as a valid JSON string literal.
	 * That is, surrounded by quotes.
	 */
	public void appendJSONString(StringBuffer buffer, String str)
	{
		buffer.append(JSON_STRING_DELIMITER);
		buffer.append(str);
		buffer.append(JSON_STRING_DELIMITER);
	} // appendJSONString() 
	
	// -------------------------------------------------------------------------
	
  /**
   * Converts the given object to a valid JSON string representation and appends 
   * it to the given buffer.
   * 
   * @param jsonConvertible The object to convert and append.
   */
	public void appendJSONConvertible(StringBuffer buffer, IJSONConvertible jsonConvertible)
	{
		if (jsonConvertible == null)
		{
			buffer.append(IJSONConvertible.JSON_LITERAL_NULL);
		}
		else
		{
			jsonConvertible.appendAsJSONString(buffer);
		}
	} // appendJSONConvertible() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Appends the given object array to the given buffer in a valid JSON 
	 * string representation.
	 * 
	 * @param objects The object array to append.
	 */
	public void appendJSONArray(StringBuffer buffer, Object... objects)
	{
		boolean isFirst = true;
		
		buffer.append(JSON_ARRAY_START);
		for (Object object : objects)
		{
			if (isFirst)
			{
				isFirst = false;
			}
			else
			{
				buffer.append(JSON_ELEMENT_SEPARATOR);
			}
			this.appendJSONObject(buffer, object);
		}
		buffer.append(JSON_ARRAY_END);
	} // appendJSONArray() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Appends the given object to the buffer as a valid JSON string.
	 */
	public void appendJSONObject(StringBuffer buffer, Object object)
	{
		if (object == null)
		{
			buffer.append(JSON_LITERAL_NULL);
		} 
		else if (object instanceof String)
		{
			this.appendJSONString(buffer, (String)object);
		} 
		else if (object instanceof IJSONConvertible)
		{
			IJSONConvertible convertible = (IJSONConvertible)object;
			this.appendJSONConvertible(buffer, convertible);
		} 
		else if (object instanceof Number)
		{
			buffer.append(object.toString());
		}
		else if (object instanceof Character)
		{
			Character ch = (Character)object;
			this.appendJSONString(buffer, String.valueOf(ch.charValue()));
		}
		else if (object instanceof Boolean)
		{
			Boolean bool = (Boolean)object; 
			buffer.append(bool.booleanValue() ? JSON_LITERAL_TRUE : JSON_LITERAL_FALSE);
		}
		else
		{
			this.appendJSONString(buffer, object.toString());
		}
	} // appendJSONObject() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Adds the given map to the buffer as JSON representation.
	 * <p/>
	 * Example:<br/>
	 * {"key1":"value1","key2":"value2","key3":"value3"}
	 */
	public void appendJSONMap(StringBuffer buffer, Map<String, Object> map) 
	{
		boolean isFirst = true;
		if (map == null)
		{
			buffer.append(JSON_LITERAL_NULL);
			return;
		}
		buffer.append(JSON_OBJECT_START);
		for (Map.Entry<String, Object> entry : map.entrySet())
		{
			if (isFirst)
			{
				isFirst = false;
			}
			else
			{
				buffer.append(JSON_ELEMENT_SEPARATOR);
			}
			this.appendJSONPair(buffer, entry.getKey(), entry.getValue());
		}
		buffer.append(JSON_OBJECT_END);
	} // appendJSONMap() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Tries to convert the given object to a JSON string.
	 * If the object is not any of the following then its toString() result will
	 * be returned.
	 * <ul>
	 * <li>null</li>
	 * <li>IJSONConvertible</li>
	 * <li>String</li>
	 * <li>Character</li>
	 * <li>Boolean</li>
	 * <li>Number</li>
	 * </ul>
	 * 
	 * @param object Any object (even null).
	 * @return A JSON value representing the given object
	 */
	public String objectToJSONValue(Object object)
	{
		StringBuffer buffer;
		
		buffer = new StringBuffer(100);
		this.appendJSONObject(buffer, object);
		return buffer.toString();
	} // objectToJSONValue() 
	
	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class JSONUtil 
