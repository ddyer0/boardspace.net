// ===========================================================================
// CONTENT  : CLASS Classpath
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 30/08/2012
// HISTORY  :
//  02/02/2003  mdu		CREATED
//	07/11/2003	mdu		changed	-->	Use FileUtil.getClasspath()
//	25/12/2004	mdu		added		-->	Support of wildcards in element definition
//	26/12/2004	mdu		added		-->	removeDuplicates()
//  30/08/2012	mdu		changed	-->	appendElement()
//
// Copyright (c) 2003-2012, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.file ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.pf.text.StringPattern;
import org.pf.text.StringUtil;

/**
 * Represents a classpath definition whereas the elements are already parsed
 * an validated.
 * <br>
 * Since version 1.2 elements are also collected from patterns with wildcards
 * (e.g. "base.jar:lib/*.jar:xalan.jar")
 *
 * @author Manfred Duchrow
 * @version 1.3
 */
public class Classpath
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  protected final static String DEFAULT_SEPARATOR = File.pathSeparator ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private List elements = null ;
  protected List getElements() { return elements ; }
  protected void setElements( List newValue ) { elements = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Returns a new Classpath object initialized with the given path elemments.
   * If the method is called with no parameter (i.e. an empty array), 
   * the default (system) classpath will be returned.
   * 
   * @param classpathElements The arbitrary number of elements to be put into the classpath.
   * @return A newly created Classpath instance.
   */
  public static Classpath create(String... classpathElements) 
	{
  	if (classpathElements.length == 0)
		{
			return new Classpath();
		}
		return new Classpath(classpathElements);
	} // create()
	
	// -------------------------------------------------------------------------
  
	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with the classpath from System property 
	 * "java.class.path" or from the Class-Path value in the manifest file of
	 * the sole JAR file if the program was started with option -jar. 
	 */
	public Classpath()
	{
		this((String)null);
	} // Classpath() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a classpath definition.
	 * 
	 * @param classpath A classpath definition with the current platform's path separator
	 */
	public Classpath(String classpath)
	{
		this(classpath, DEFAULT_SEPARATOR);
	} // Classpath() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with a classpath definition where the separator
	 * is the one given as parameter.
	 * 
	 * @param classpath A classpath definition with the specified path separator
	 * @param separator The separator thst is used to separate the elements
	 */
	public Classpath(String classpath, String separator)
	{
		super();
		if (classpath == null)
		{
			classpath = fileUtil().getClasspath();
		}
		this.init(classpath, separator == null ? DEFAULT_SEPARATOR : separator);
	} // Classpath() 

	// -------------------------------------------------------------------------  

	/**
	 * Initialize the new instance with the given elements.
	 * 
	 * @param classpathElements An array with the elements of the classpath
	 */
	public Classpath(String[] classpathElements)
	{
		super();
		this.initFromParts(classpathElements);
	} // Classpath() 

	// -------------------------------------------------------------------------  

	/**
	 * Initialize the new instance with the given elements.
	 * 
	 * @param classpathElements A collection of String elements that build the classpath
	 */
	public Classpath(Collection classpathElements)
	{
		super();
		this.initFromParts(this.str().asStrings(classpathElements));
	} // Classpath() 

	// -------------------------------------------------------------------------  

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns all classpath elements.
	 */
	public ClasspathElement[] allElements()
	{
		return this.elementArray(this.getElements());
	} // allElements() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the classpath elements that are valid.
	 */
	public ClasspathElement[] validElements()
	{
		return this.collectElements(true);
	} // validElements() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the classpath elements that are invalid.
	 */
	public ClasspathElement[] invalidElements()
	{
		return this.collectElements(false);
	} // invalidElements() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all elements that contain a file with the specified name.
	 */
	public ClasspathElement[] elementsContaining(String filename)
	{
		return this.elementsWithFile(filename);
	} // elementsContaining() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the first element that contains a file with the specified name or
	 * null if the file cannot be found.
	 */
	public ClasspathElement firstElementContaining(String filename)
	{
		ClasspathElement[] cpElements;

		cpElements = this.elementsWithFile(filename, true);
		if ((cpElements == null) || (cpElements.length == 0))
			return null;

		return cpElements[0];
	} // firstElementContaining() 

	// -------------------------------------------------------------------------

	/**
	 * Removes all duplicates of classpath elements that are already in the 
	 * classpath. 
	 */
	public void removeDuplicates()
	{
		List newList;
		Iterator iter;
		Object element;

		newList = new ArrayList(this.getElements().size());
		iter = this.getElements().iterator();
		while (iter.hasNext())
		{
			element = iter.next();
			if (!newList.contains(element))
			{
				newList.add(element);
			}
		}
		this.setElements(newList);
	} // removeDuplicates() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the string representation of this classpath using the current 
	 * platform's path separator.
	 */
	public String toString()
	{
		return this.toString(DEFAULT_SEPARATOR);
	} // toString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the string representation of this classpath using the given 
	 * path separator.
	 * 
	 * @param separator The separator to separate the elements 
	 */
	public String toString(String separator)
	{
		StringBuffer buffer;
		ClasspathElement element;

		buffer = new StringBuffer(this.getElements().size() * 20);

		for (int i = 0; i < this.getElements().size(); i++)
		{
			element = (ClasspathElement)this.getElements().get(i);
			if (i > 0)
			{
				buffer.append(separator);
			}
			buffer.append(element.getName());
		}
		return buffer.toString();
	} // toString() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected void init(String classpath, String separator)
	{
		String[] parts;

		if (classpath == null)
		{
			this.setElements(new ArrayList());
			return;
		}

		parts = str().parts(classpath, separator);
		this.initFromParts(parts);
	} // init() 

	// -------------------------------------------------------------------------

	protected void initFromParts(String[] parts)
	{
		String elementName;

		if (this.str().isNullOrEmpty(parts))
		{
			this.setElements(new ArrayList());
			return;
		}

		this.setElements(new ArrayList(parts.length));
		for (int i = 0; i < parts.length; i++)
		{
			elementName = parts[i].trim();
			this.appendElement(elementName);
		}
	} // initFromParts() 

	// -------------------------------------------------------------------------

	protected void appendElement(final String elementName)
	{
		File[] files;
		String pattern;

		if (StringPattern.containsWildcard(elementName))
		{
			pattern = elementName;
			if (pattern.endsWith("*"))
			{
				pattern += ".jar";
			}
			files = FileFinder.findFiles(pattern);
			for (int i = 0; i < files.length; i++)
			{
				this.basicAppendElement(files[i].getPath());
			}
		}
		else
		{
			this.basicAppendElement(elementName);
		}
	} // appendElement() 

	// -------------------------------------------------------------------------

	protected void basicAppendElement(String elementName)
	{
		this.getElements().add(new ClasspathElement(elementName));
	} // basicAppendElement() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all elements that contain a file with the given name.
	 */
	protected ClasspathElement[] elementsWithFile(String filename)
	{
		return this.elementsWithFile(filename, false);
	} // elementsWithFile() 

	// -------------------------------------------------------------------------

	/**
	 * Returns all elements or the first element that contain(s) a file with the 
	 * given name.
	 */
	protected ClasspathElement[] elementsWithFile(String filename, boolean onlyFirst)
	{
		List collection;
		ClasspathElement element;

		collection = new ArrayList(this.getElements().size());
		for (int i = 0; i < this.getElements().size(); i++)
		{
			element = (ClasspathElement)this.getElements().get(i);
			if (element.contains(filename))
			{
				collection.add(element);
				if (onlyFirst)
					break;
			}
		}

		return this.elementArray(collection);
	} // elementsWithFile() 

	// -------------------------------------------------------------------------

	protected ClasspathElement[] collectElements(boolean valid)
	{
		Iterator iter;
		List collection;
		ClasspathElement element;

		collection = new ArrayList(this.getElements().size());

		iter = this.getElements().iterator();
		while (iter.hasNext())
		{
			element = (ClasspathElement)iter.next();
			if (valid)
			{
				if (element.isValid())
					collection.add(element);
			}
			else
			{
				if (!element.isValid())
					collection.add(element);
			}
		}
		return this.elementArray(collection);
	} // collectElements() 

	// -------------------------------------------------------------------------

	protected ClasspathElement[] elementArray(List elementList)
	{
		ClasspathElement[] array;

		array = new ClasspathElement[elementList.size()];
		return (ClasspathElement[])elementList.toArray(array);
	} // elementArray() 

	// -------------------------------------------------------------------------

	protected FileUtil fileUtil()
	{
		return FileUtil.current();
	} // fileUtil() 

	// -------------------------------------------------------------------------

	protected StringUtil str()
	{
		return StringUtil.current();
	} // str() 

	// -------------------------------------------------------------------------

} // class Classpath 
