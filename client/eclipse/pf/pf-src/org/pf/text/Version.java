// ===========================================================================
// CONTENT  : CLASS Version
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 01/06/2008
// HISTORY  :
//  06/02/2004  mdu  CREATED
//	04/06/2006	mdu		changed	-->	to support letters in version numbers
//	01/06/2008	mdu		changed	-->	extended with more compare methods
//
// Copyright (c) 2004-2008, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class provides a representation of version identifiers of pattern
 * "x.y.z" in a way that they can be compared and sorted according to their
 * real meaning and not just by simple string comparison.
 * <p>
 * The last element (i.e. most right) may contain non-numeric characters and
 * which will be compared as String. Such characters are limited to ascii
 * letters, digits and '-' and '_'.
 * <p>
 * Examples:
 * <br>3.1 &gt; 3.0
 * <br>3.1.1 &gt; 3.1
 * <br>2.4 &lt; 10.0
 * <br>0.11.2 &gt; 0.1.22
 * <br>1.4.7b &gt; 1.4.7a
 * <br>1.5.0_02 &lt; 1.5.0_17
 * <br>1.4.2_02 &lt; 1.4.10_01
 *
 * @author Manfred Duchrow
 * @version 1.2
 */
public class Version
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	private static final int LESS = -1;
	private static final int EQUAL = 0;
	private static final int GREATER = 1;

	private static final String JAVA_VERSION_PROPERTY = "java.version";
	private static final String JAVA_PATCH_SEPARATOR = "_";

	private static final Integer NOT_SET = Integer.valueOf(-1);

	/**
	 * Contains the seperator between the version elements. (".")
	 */
	public static final String SEPARATOR = ".";

	/**
	 * A definition of characters that are allowed in a version element.
	 */
	public static final String SPECIAL_CHARACTERS = "-_";

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private List<VersionElement> elements = null;
	protected List<VersionElement> getElements()
	{
		return elements;
	}
	protected void setElements(List<VersionElement> newValue)
	{
		elements = newValue;
	}

	// =========================================================================
	// CLASS METHODS
	// =========================================================================
	/**
	 * Returns true if the given string represents a valid version.
	 * 
	 * @param str The string to be checked if it is a valid version
	 */
	public static boolean isValidVersion(String str)
	{
		Version version;

		if (str == null)
		{
			return false;
		}
		version = new Version(str);
		return version.isValid();
	} // isValidVersion() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the version of the current JVM.
	 */
	public static Version getJavaVersion()
	{
		return new Version(System.getProperty(JAVA_VERSION_PROPERTY));
	} // getJavaVersion() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the version of the current JVM with out the patch level.
	 * That is, for Java version "1.5.0_12" the version "1.5.0" will be returned.
	 */
	public static Version getJavaBaseVersion()
	{
		String versionStr;

		versionStr = System.getProperty(JAVA_VERSION_PROPERTY);
		versionStr = StringUtil.current().cutTail(versionStr, JAVA_PATCH_SEPARATOR);
		return new Version(versionStr);
	} // getJavaBaseVersion() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance. 
	 */
	protected Version()
	{
		super();
		this.setElements(new ArrayList());
	} // Version() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a version string of type "x.y.z".
	 * The elements in the string separated by dots is not limited! 
	 */
	public Version(String versionString)
	{
		this();
		this.parse(versionString);
	} // Version() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with the values from another a version.
	 */
	public Version(Version version)
	{
		this();

		Iterator otherElements = version.getElements().iterator();
		while (otherElements.hasNext())
		{
			VersionElement vElement = (VersionElement)otherElements.next();
			this.getElements().add(vElement.copy());
		}
	} // Version() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a major, minor, micro version
	 * and a qualifier.
	 * For any negative parameter value zero will be used instead! 
	 */
	public Version(int major, int minor, int micro, String qualifier)
	{
		this(major, minor, micro);
		this.getLastElement().setStringPart(qualifier);
	} // Version() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a major, minor and micro version.
	 * For any negative parameter value zero will be used instead! 
	 */
	public Version(int major, int minor, int micro)
	{
		this(major, minor);
		this.addElement(micro);
	} // Version() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a major and minor version.
	 * For any negative parameter value zero will be used instead! 
	 */
	public Version(int major, int minor)
	{
		this(major);
		this.addElement(minor);
	} // Version() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a major version.
	 * For a negative parameter value zero will be used instead! 
	 */
	public Version(int major)
	{
		this();
		this.addElement(major);
	} // Version() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns if this version is greater than the specified version.
	 */
	public boolean isGreaterThan(Version version)
	{
		return this.compareTo(version) == GREATER;
	} // isGreaterThan() 

	// -------------------------------------------------------------------------

	/**
	 * Returns if this version is less than the given version.
	 */
	public boolean isLessThan(Version version)
	{
		return this.compareTo(version) == LESS;
	} // isLessThan() 

	// -------------------------------------------------------------------------

	/**
	 * Returns if this version is greater than the given version.
	 */
	public boolean isGreaterThan(String version)
	{
		return this.isGreaterThan(new Version(version));
	} // isGreaterThan() 

	// -------------------------------------------------------------------------

	/**
	 * Returns if this version is less than the specified version.
	 */
	public boolean isLessThan(String version)
	{
		return this.isLessThan(new Version(version));
	} // isLessThan() 

	// -------------------------------------------------------------------------

	/**
	 * Returns if this version is greater or equal compared to the given version.
	 */
	public boolean isGreaterOrEqual(Version version)
	{
		int result;

		result = this.compareTo(version);
		return (result == GREATER) || (result == EQUAL);
	} // isGreaterOrEqual() 

	// -------------------------------------------------------------------------

	/**
	 * Returns if this version is less or equal compared to the given version.
	 */
	public boolean isLessOrEqual(Version version)
	{
		int result;

		result = this.compareTo(version);
		return (result == LESS) || (result == EQUAL);
	} // isLessOrEqual() 

	// -------------------------------------------------------------------------

	/**
	 * Returns if this version is greater or equal compared to the given version.
	 */
	public boolean isGreaterOrEqual(String strVersion)
	{
		return this.isGreaterOrEqual(new Version(strVersion));
	} // isGreaterOrEqual() 

	// -------------------------------------------------------------------------

	/**
	 * Returns if this version is less or equal compared to the given version.
	 */
	public boolean isLessOrEqual(String strVersion)
	{
		return this.isLessOrEqual(new Version(strVersion));
	} // isLessOrEqual() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if this version is equal to the version
	 * represented by the given string.
	 * If the strVersion is no valid version false will be returned. 
	 */
	public boolean isEqualTo(String strVersion)
	{
		if (!isValidVersion(strVersion))
		{
			return false;
		}
		Version otherVersion;

		otherVersion = new Version(strVersion);
		return this.equals(otherVersion);
	} // isEqualTo() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if this version is equal to the given object.
	 * The object must be of type <b>Version</b> or <b>String</b>.
	 */
	public boolean equals(Object obj)
	{
		if (obj instanceof Version)
		{
			return this.compareTo(obj) == EQUAL;
		}

		if (obj instanceof String)
		{
			return this.compareTo(new Version((String)obj)) == EQUAL;
		}

		return false;
	} // equals() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a hash code
	 */
	public int hashCode()
	{
		int hash = 0;

		for (int i = 0; i < this.getElements().size(); i++)
		{
			hash = hash ^ this.getElements().get(i).hashCode();
		}
		return hash;
	} // hashCode() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the version as string
	 */
	public String toString()
	{
		StringBuffer buffer ;
		VersionElement element;
		
		buffer = new StringBuffer(20) ;
		
		for (int i = 0; i < this.getElements().size(); i++)
		{
		  element = this.getElements().get(i);
			if (this.needsSeparator(i, element))
			{
				buffer.append(SEPARATOR) ;
			}
			buffer.append(element) ;
		}
		
		return buffer.toString() ;
	} // toString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns a new version object with the same value as this one.
	 */
	public Version copy()
	{
		return new Version(this);
	} // copy() 

	// -------------------------------------------------------------------------

	/**
	 * Returns an array where all sub-elements are contained.
	 */
	public String[] getVersionElements()
	{
		String[] subElements;
		VersionElement elem;

		subElements = new String[this.getElements().size()];
		for (int i = 0; i < this.getElements().size(); i++)
		{
			elem = (VersionElement)this.getElements().get(i);
			subElements[i] = elem.toString();
		}
		return subElements;
	} // getVersionElements() 

	// -------------------------------------------------------------------------

	/**
	 * Compares this object with the specified object for order.  Returns a
	 * negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object.<p>
	 *
	 * @throws IllegalArgumentException if the specified object is not a Version
	 */
	public int compareTo(Object obj)
	{
		Version otherVersion;
		List otherElemets;
		int compResult;
		VersionElement element;
		VersionElement otherElement;
		int i;

		if (obj instanceof Version)
		{
			otherVersion = (Version)obj;
			otherElemets = otherVersion.getElements();
			for (i = 0; i < otherElemets.size(); i++)
			{
				if (i >= this.getElements().size())
					return LESS;

				element = (VersionElement)this.getElements().get(i);
				otherElement = (VersionElement)otherElemets.get(i);
				compResult = element.compare(otherElement);

				if (compResult == 0)
				{
					// Still equal - go to next element to compare
				}
				else
				{
					return (compResult < 0) ? LESS : GREATER;
				}
			}
			return (i == this.getElements().size()) ? EQUAL : GREATER;
		}
		throw new IllegalArgumentException("The object to compare is not a Version");
	} // compareTo() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if this version contains only positive numeric sub parts.
	 */
	public boolean isNumeric()
	{
		Iterator iter;
		VersionElement element;

		if (this.getElements().isEmpty())
		{
			return false;
		}
		iter = this.getElements().iterator();
		while (iter.hasNext())
		{
			element = (VersionElement)iter.next();
			if (!element.isNumeric())
			{
				return false;
			}
		}
		return true;
	} // isNumeric() 

	// -------------------------------------------------------------------------

	/**
	 * Returns true if this version contains only valid sub parts.
	 */
	public boolean isValid()
	{
		Iterator<VersionElement> iter ;
		VersionElement element ;
		VersionElement lastElement ;
		
		if ( this.getElements().isEmpty() )
		{
			return false ;
		}
		lastElement = this.getLastElement();
		iter = this.getElements().iterator() ;
		while ( iter.hasNext() )
		{
			element = iter.next();
			if ( ! element.isValid() )
			{
				return false ;
			}
			if ((element != lastElement) && element.hasStringPart())
			{
				return false ;
			}
		}
		return true ;
	} // isValid() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected void parse(String versionString)
	{
		String[] parts;
		VersionElement element;

		parts = this.str().parts(versionString, SEPARATOR);
		if (this.str().notNullOrEmpty(parts))
		{
			for (int i = 0; i < parts.length; i++)
			{
				element = new VersionElement(parts[i]);
				this.getElements().add(element);
			}

			element = this.getElements().get(this.getElements().size() - 1);
			if (element.isPureString())
			{
				element.makeInvalid();
			}
		}
	} // parse() 

	// -------------------------------------------------------------------------

	protected boolean needsSeparator(int elementIndex, VersionElement element)
	{
		if (elementIndex == 0)
		{
			return false;
		}
		if (elementIndex == (this.getElements().size() - 1))
		{
			if (element.isNumeric() || element.isNumericWithString())
			{
				return true;
			}
			return false;
		}
		return true;
	} // needsSeparator()

	// -------------------------------------------------------------------------

	protected void addElement(int value)
	{
		int num;

		num = (value < 0) ? 0 : value;
		this.getElements().add(new VersionElement(num));
	} // addElement() 

	// -------------------------------------------------------------------------

	protected VersionElement getLastElement()
	{
		return this.getElements().get(this.getElements().size() - 1);
	} // getLastElement()

	// -------------------------------------------------------------------------

	protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// INNER CLASSES
	// =========================================================================
	private class VersionElement
	{
		private Integer intPart = NOT_SET;
		private String strPart = "";

		VersionElement(String versionString)
		{
			super();
			this.parse(versionString);
		} // VersionElement() 

		// -----------------------------------------------------------------------		

		VersionElement(int intPart)
		{
			this.intPart = Integer.valueOf(intPart);
		} // VersionElement() 

		// -----------------------------------------------------------------------    

		private VersionElement(int intPart, String strPart)
		{
			this.intPart = Integer.valueOf(intPart);
			this.strPart = strPart;
		} // VersionElement() 
		
		// -----------------------------------------------------------------------    
		
		public String toString()
		{
			if (intPart.equals(NOT_SET))
			{
				return strPart;
			}
			return intPart.toString() + strPart;
		} // toString() 

		// -----------------------------------------------------------------------

		protected void parse(String versionPart)
		{
			StringBuffer numBuffer;
			StringBuffer strBuffer;
			boolean isNumber = true;
			int value;
			char ch;

			numBuffer = new StringBuffer(20);
			strBuffer = new StringBuffer(20);
			for (int i = 0; i < versionPart.length(); i++)
			{
				ch = versionPart.charAt(i);
				isNumber = isNumber && Character.isDigit(ch);
				if (isNumber)
				{
					numBuffer.append(ch);
				}
				else
				{
					strBuffer.append(ch);
				}
			}

			try
			{
				if (numBuffer.length() > 0)
				{
					value = Integer.parseInt(numBuffer.toString());
					intPart = Integer.valueOf(value);
				}
			}
			catch (NumberFormatException e)
			{
				intPart = NOT_SET;
			}
			strPart = strBuffer.toString();
		} // parse() 

		// -----------------------------------------------------------------------		

		protected int compare(VersionElement otherElement)
		{
			int compResult;

			compResult = intPart.compareTo(otherElement.intPart);
			if (compResult == EQUAL)
			{
				compResult = strPart.compareTo(otherElement.strPart);
			}
			return compResult;
		} // compare() 

		// -----------------------------------------------------------------------

		protected void makeInvalid()
		{
			this.intPart = NOT_SET;
			this.setStringPart("");
		}

		// -------------------------------------------------------------------------

		protected void setStringPart(String qualifier)
		{
			this.strPart = qualifier;
		}
		
		// -------------------------------------------------------------------------

		public boolean equals( Object object ) 
		{
			if ( object instanceof VersionElement )
			{
				return this.compare( (VersionElement)object ) == EQUAL ;
			}
			return false ;
		} // equals() 
		
		// -------------------------------------------------------------------------

		public int hashCode() 
		{
			if ( this.isNumeric() )
			{
				return intPart.intValue() ;
			}
			return strPart.hashCode() ;
		} // hashCode() 
		
		// -------------------------------------------------------------------------
		
		protected VersionElement copy()
		{
			return new VersionElement(intPart, strPart) ;
		} // copy() 
		
		// -------------------------------------------------------------------------
		
		protected boolean isValid()
		{
			if (intPart.equals(NOT_SET))
			{
				return str().notNullOrEmpty(strPart);
			}
			return true;
		} // isValid() 

		// -----------------------------------------------------------------------

		protected boolean hasStringPart()
		{
			return str().notNullOrEmpty(strPart);
		}
		
		// -------------------------------------------------------------------------

		protected boolean isNumeric()
		{
			return (!intPart.equals(NOT_SET)) && str().isNullOrEmpty(strPart);
		} // isNumeric() 

		// -----------------------------------------------------------------------

		protected boolean isNumericWithString()
		{
			return (!intPart.equals(NOT_SET)) && this.hasStringPart();
		} // isNumericWithString() 

		// -----------------------------------------------------------------------

		protected boolean isPureString()
		{
			return !(this.isNumeric() || this.isNumericWithString());
		} // isPureString()

		// -------------------------------------------------------------------------

	} // class VersionElement

	// -------------------------------------------------------------------------

} // class Version 
