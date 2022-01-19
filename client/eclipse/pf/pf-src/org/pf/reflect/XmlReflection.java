// ===========================================================================
// CONTENT  : CLASS XmlReflection
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 14/01/2012
// HISTORY  :
//  14/01/2012  mdu  CREATED
//
// Copyright (c) 2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.reflect ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Provides a mechanism to create Java object instances from XML meta-data
 * describing the objects to create.<p>
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class XmlReflection
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	private static final ReflectUtil RU = ReflectUtil.current();

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String attrNameForClass = "class" ;
  public String getAttrNameForClass() { return attrNameForClass ; }
  public void setAttrNameForClass( String newValue ) { attrNameForClass = newValue ; }

  private String tagNameForField = "property" ;
  public String getTagNameForField() { return tagNameForField ; }
  public void setTagNameForField( String newValue ) { tagNameForField = newValue ; }
  
  private String attrNameForFieldName = "name" ;
  public String getAttrNameForFieldName() { return attrNameForFieldName ; }
  public void setAttrNameForFieldName( String newValue ) { attrNameForFieldName = newValue ; }
  
  private String attrNameForFieldValue = "value" ;
  public String getAttrNameForFieldValue() { return attrNameForFieldValue ; }
  public void setAttrNameForFieldValue( String newValue ) { attrNameForFieldValue = newValue ; }
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public XmlReflection()
  {
    super() ;
  } // XmlReflection() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  public Object createInstance(final Element xmlElement) 
  {
  	return this.createInstance(xmlElement, Object.class);
  } // createInstance() 
  
  // -------------------------------------------------------------------------
  
  public <T> T createInstance(final Element xmlElement, Class<T> expectedType) 
	{
		String className;
		T newObject;
		
		className = xmlElement.getAttribute(this.getAttrNameForClass());
		newObject = RU.createInstanceOfType(expectedType, className, this);
		return newObject;
	} // createInstance() 
	
	// -------------------------------------------------------------------------

  public Object createInitializedInstance(final Element xmlElement) throws NoSuchFieldException 
  {
  	return this.createInitializedInstance(xmlElement, Object.class);
  } // createInitializedInstance() 
  
  // -------------------------------------------------------------------------
  
  public <T> T createInitializedInstance(Element xmlElement, Class<T> expectedType) throws NoSuchFieldException 
  {
  	T newObject;
  	
  	newObject = this.createInstance(xmlElement, expectedType);
  	if (newObject == null)
		{
			throw new NoSuchFieldException("Attribute '" + this.getAttrNameForClass() + "' not found in XML element <" + xmlElement.getTagName() + ">");
		}
  	this.initProperties(xmlElement, newObject);
  	return newObject;
  } // createInitializedInstance() 
  
  // -------------------------------------------------------------------------

  public <T> List<T> createInitializedInstances(List<Element> xmlElements, Class<T> expectedType) throws NoSuchFieldException 
  {
  	List<T> result;
  	T object;
  	
  	result = new ArrayList<T>(xmlElements.size());
  	for (Element element : xmlElements)
		{
  		object = this.createInitializedInstance(element, expectedType);
			result.add(object);
		}
  	return result;
  } // createInitializedInstances()
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected void initProperties(final Element xmlElement, Object object) throws NoSuchFieldException 
	{
		NodeList fieldTags;
		Node node;
		Element tag;
		String fieldName;
		String textValue;
		Field field;
		Object value;
		
		fieldTags = xmlElement.getElementsByTagName(this.getTagNameForField());
		for (int i = 0; i < fieldTags.getLength(); i++)
		{
			node = fieldTags.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{				
				tag = (Element)node;
				fieldName = tag.getAttribute(this.getAttrNameForFieldName());
				if (this.hasValueAttrName())
				{
					textValue = tag.getAttribute(this.getAttrNameForFieldValue());
					if (textValue == null)
					{
						textValue = tag.getTextContent();
					}
				}
				else
				{
					textValue = tag.getTextContent();
				}
				field = RU.getField(object, fieldName);
				if (field == null)
				{
					throw new NoSuchFieldException("Cannot find field '" + fieldName + "' in class " + object.getClass().getName());
				}
				value = this.convertToType(textValue, field.getType());
				RU.setValueOf(object, fieldName, value);
			}
		}
	} // initProperties() 
  
  // -------------------------------------------------------------------------
  
  protected Object convertToType(String text, Class type) 
	{
  	if (type == String.class)
  	{
  		return text;
  	}
		if ((type == Integer.TYPE) || (type == Integer.class))
		{
			return Integer.parseInt(text);
		}
		if ((type == Long.TYPE) || (type == Long.class))
		{
			return Long.parseLong(text);
		}
		if ((type == Byte.TYPE) || (type == Byte.class))
		{
			return Byte.parseByte(text);
		}
		if ((type == Short.TYPE) || (type == Short.class))
		{
			return Short.parseShort(text);
		}
		if ((type == Float.TYPE) || (type == Float.class))
		{
			return Float.parseFloat(text);
		}
		if ((type == Double.TYPE) || (type == Double.class))
		{
			return Double.parseDouble(text);
		}
		if ((type == Boolean.TYPE) || (type == Boolean.class))
		{
			return Boolean.parseBoolean(text);
		}
		if ((type == Character.TYPE) || (type == Character.class))
		{
			return text.charAt(0);
		}
		try
		{
			Constructor constructor = type.getConstructor(String.class);
			return constructor.newInstance(text);
		}
		catch (Throwable ex)
		{
			ex.printStackTrace();
			return null;
		}
	} // convertToType() 
	
	// -------------------------------------------------------------------------
  
	protected boolean hasValueAttrName()
	{
		if ((this.getAttrNameForFieldValue() == null) || (this.getAttrNameForFieldValue().length() == 0))
		{			
			return false;
		}
		char ch = this.getAttrNameForFieldValue().charAt(0);
		if (ch < 'A')
		{			
			return false;
		}
		if (ch > 'z')
		{			
			return false;
		}
		if ((ch > 'Z') && (ch < 'a'))
		{			
			return false;
		}
		return true;
	} // hasValueAttrName() 
	
	// -------------------------------------------------------------------------
  
} // class XmlReflection 
