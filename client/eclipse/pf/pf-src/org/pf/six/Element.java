// ===========================================================================
// CONTENT  : CLASS Element
// AUTHOR   : Manfred Duchrow
// VERSION  : 3.0 - 17/01/2014
// HISTORY  :
//  28/02/2002  duma  CREATED
//	17/05/2002	duma	adedd		->	find() and findChildren() methods	
//	25/05/2002	duma	added		->	removeAttribute(), removeText()
//	04/07/2002	duma	added		->	Constructor with tag name
//	10/10/2003	duma	added		->	Constructor with tag name and text
//	19/03/2004	duma	changed	->	Support for namespace information
//	12/01/2008	mdu		added		->	EMPTY_ARRAY, toArray(), toList()
//	07/02/2010	mdu		added		->	Better namspace and prefix handling
//  17/01/2014  mdu   added   ->  Support of processing instructions
//
// Copyright (c) 2002-2014, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.six;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.pf.text.StringUtil;
import org.pf.util.CollectionUtil;
import org.pf.util.NamedTextList;

/**
 * Each element represents one tag occurrence in a XML stream.
 * The element's name corresponds to the XML tag name.
 *
 * @author Manfred Duchrow
 * @version 3.0
 */
public class Element implements XmlItem
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	public static final Element[] EMPTY_ARRAY	= new Element[0] ;
	
	protected static final String NAMESPACE_SEPARATOR	= ":" ;
	protected static final String NAMESPACE_PREFIX		= "xmlns" + NAMESPACE_SEPARATOR ;
	private static final int NOT_FOUND_INDEX = -1 ;

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String name = null ;
  private String text = null ;
  private final List<XmlItem> internalChildItems = new ArrayList<XmlItem>();

  private NamedTextList elementAttributes = new NamedTextList() ;
  protected NamedTextList getElementAttributes() { return elementAttributes ; }
  protected void setAttributes( NamedTextList newValue ) { elementAttributes = newValue ; }
  
	private Map namespaces = null ;
	protected Map getNamespaces() { return namespaces ; }
	protected void setNamespaces( Map newValue ) { namespaces = newValue ; }      
      
  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Returns an Element array that contains all Element objects of the given
   * collection. If the given collection is null or empty an empty Element array
   * will be returned.
   * 
   * @param elementCollection The collection to be converted to an array.
   * @throws ClassCastException If any object in the given collection is not an Element.
   */
  public static Element[] toArray(Collection<Element> elementCollection)
  {
    if (coll().isNullOrEmpty(elementCollection))
    {
      return EMPTY_ARRAY;
    }
    return (Element[])coll().toArray(elementCollection);
  } // toArray() 

  // -------------------------------------------------------------------------

  /**
   * Returns an ArrayList that contains all elements of the given array.
   * If the given array is null or empty then an empty ArrayList will 
   * be returned.
   * 
   * @param elements The array to convert to a list
   */
  public static List<Element> toList(Element[] elements)
  {
    if (coll().isNullOrEmpty(elements))
    {
      return new ArrayList();
    }
    return coll().toList(elements);
  } // toList() 

  // -------------------------------------------------------------------------

  private static CollectionUtil coll()
  {
    return CollectionUtil.current();
  } // coll() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public Element()
  {
    super();
  } // Element() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the given name.
   * 
   * @param tagName The name of the new element
   */
  public Element(String tagName)
  {
    this();
    this.setName(tagName);
  } // Element() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with the given name and text.
   * 
   * @param tagName The name of the new element
   * @param text The text content of the new element
   */
  public Element(String tagName, String text)
  {
    this();
    this.setName(tagName);
    this.setText(text);
  } // Element() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns always Type.ELEMENT.
   */
  public Type getItemType()
  {
    return Type.ELEMENT;
  } // getItemType() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the name of the element. This name corresponds to the
   * tag name in the XML representation of this element.
   */
  public String getName()
  {
    return name;
  } // getName() 

  // -------------------------------------------------------------------------

  /**
   * Set the element's name which corresponds to its XML tag name.
   * 
   * @param newName Any valid XML tag name
   */
  public void setName(String newName)
  {
    name = newName;
  } // setName() 

  // -------------------------------------------------------------------------

  /**
   * Returns the local name of this element. That is, without a namespace prefix.
   */
  public String getLocalName()
  {
    return this.getLocalNameOf(this.getName());
  } // getLocalName() 

  // -------------------------------------------------------------------------

  public String getNamePrefix()
  {
    return this.getNamePrefixOf(this.getName());
  } // getNamePrefix() 

  // -------------------------------------------------------------------------

  /**
   * Returns the elements textual content which corresponds to the 
   * XML tag's PCDATA.
   */
  public String getText()
  {
    return text;
  } // getText() 

  // -------------------------------------------------------------------------

  /**
   * Set the element's text.
   * 
   * @param newText Any valid XML character data compliant to the desired encoding
   */
  public void setText(String newText)
  {
    text = newText;
  } // setText() 

  // -------------------------------------------------------------------------

  /**
   * Returns the value of the attribute with the given name. If the attribute
   * is not set (not available) this method returns null.
   * <br>
   * @param attrName The name of the attribute including a namespace prefix if applicable
   */
  public String getAttribute(String attrName)
  {
    return this.getElementAttributes().textAt(attrName);
  } // getAttribute() 

  // -------------------------------------------------------------------------

  /**
   * Sets the value of the attribute with the given name. If the attribute
   * exists already its value will be changed to the new one.
   * 
   * @param attrName The name of the attribute including a namespace prefix if applicable
   * @param value The value to assign to this attribute
   */
  public void setAttribute(String attrName, String value)
  {
    this.getElementAttributes().put(attrName, value);
  } // setAttribute() 

  // -------------------------------------------------------------------------

  /**
   * Removes the attribute with the specified name from the element's 
   * attribute list.
   * 
   * @param attrName The name of the attribute to remove (including namespace prefix)
   */
  public void removeAttribute(String attrName)
  {
    this.getElementAttributes().remove(attrName);
  } // removeAttribute() 

  // -------------------------------------------------------------------------

  /**
   * Returns all attributes of this element.
   * This returned map may be modified, since it is a copy.
   * To add or remove attributes will not change the attributes of this element.
   * by Element.
   */
  public Map getAttributes()
  {
    HashMap copy;

    copy = this.getElementAttributes().asHashMap();
    return copy;
  } // getAttributes() 

  // -------------------------------------------------------------------------

  /**
   * Returns the names of all attributes assigned to this element.
   * If the element has no attribute an empty array will be returned.
   * The returned names are qualified names, that means they may contain
   * namespace prefixes.
   */
  public String[] getAttributeNames()
  {
    return this.getAttributeNames(true);
  } // getAttributeNames() 

  // -------------------------------------------------------------------------

  /**
   * Returns the names of all attributes assigned to this element.
   * If the element has no attribute an empty array will be returned.
   * The returned names are local names, that means they don't contain
   * namespace prefixes.
   */
  public String[] getAttributeLocalNames()
  {
    return this.getAttributeNames(false);
  } // getAttributeLocalNames() 

  // -------------------------------------------------------------------------

  /**
   * Returns the number of contained child items.
   */
  public int getChildItemCount() 
  {
    return this.getInternalChildItems().size();
  } // getChildItemCount() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns the number of child elements.
   */
  public int getChildrenCount()
  {
    int counter = 0;
    
    for (XmlItem item : this.getInternalChildItems())
    {
      if (item instanceof Element)
      {
        counter++;
      }
    }
    return counter;
  } // getChildrenCount() 

  // -------------------------------------------------------------------------

  /**
   * Returns all child elements.
   */
  public Element[] getChildren()
  {
    return Element.toArray(this.collectContainedElements());
  } // getChildren() 

  // -------------------------------------------------------------------------

  /**
   * Returns the first child element or null if there is no child element.
   */
  public Element getFirstChild()
  {
    for (XmlItem item : this.getInternalChildItems())
    {
      if (item instanceof Element)
      {
        return (Element)item;
      }
    }
    return null;
  } // getFirstChild() 

  // -------------------------------------------------------------------------
  
  /**
   * Returns true if the element has any child (Element or {@link XmlProcessingInstruction}).
   * For more specific item types see {@link #hasChildren()} and {@link #hasProcessingInstructions()}.  
   */
  public boolean hasChildItems() 
  {
    return !this.getInternalChildItems().isEmpty();
  } // hasChildItems() 
  
  // -------------------------------------------------------------------------

  /**
   * Returns true if the element contains at least on child element.
   * That does NOT include processing instructions.
   */
  public boolean hasChildren()
  {
    for (XmlItem item : this.getInternalChildItems())
    {
      if (item.getItemType() == Type.ELEMENT)
      {
        return true;
      }
    }
    return false;
  } // hasChildren() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if the element contains at least one processing instruction.
   */
  public boolean hasProcessingInstructions()
  {
    for (XmlItem item : this.getInternalChildItems())
    {
      if (item.getItemType() == Type.PI)
      {
        return true;
      }
    }
    return false;
  } // hasProcessingInstructions() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Removes the given child from this element. The child itself must be in
   * the children list. That is, comparison is done by == rather than equals().
   * 
   * @param childElement The child element to be removed
   * @return true if the child element was found and removed
   */
  public boolean removeChild(Element childElement)
  {
    return this.removeChildItem(childElement);
  } // removeChild() 

  // -------------------------------------------------------------------------

  /**
   * Removes the given child from this element. The child itself must be in
   * the children list. That is, comparison is done by == rather than equals().
   * 
   * @param item The child element to be removed
   * @return true if the child element was found and removed
   */
  public boolean removeChildItem(XmlItem item)
  {
    int index;
    
    index = this.indexOfChild(item);
    if (index == NOT_FOUND_INDEX)
    {
      return false;
    }
    return (item == this.getInternalChildItems().remove(index));
  } // removeChildItem() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Removes all children of this element. That includes all types 
   * {@link Element} and {@link XmlProcessingInstruction}).
   */
  public void removeAllChildren()
  {
    this.getInternalChildItems().clear();
  } // removeAllChildren() 

  // -------------------------------------------------------------------------

  /**
   * Removes the element's text content.
   */
  public void removeText()
  {
    this.setText(null);
  } // removeText() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if the element has any text.
   */
  public boolean hasText()
  {
    return (this.getText() != null);
  } // hasText() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if the element has any attribue.
   */
  public boolean hasAttributes()
  {
    return ((this.getElementAttributes() != null) && (this.getElementAttributes().size() > 0));
  } // hasAttributes() 

  // -------------------------------------------------------------------------

  /**
   * Returns true if the element has an attribue with the given name.
   */
  public boolean hasAttribute(String attrName)
  {
    return (this.getAttribute(attrName) != null);
  } // hasAttribute() 

  // -------------------------------------------------------------------------

  /**
   * Returns the element's name as a XML tag.
   * Example: "<Person>"
   */
  public String toString()
  {
    return "<" + this.getName() + ">";
  } // toString() 

  // -------------------------------------------------------------------------

  /**
   * Adds the given element as a child to the receiver
   * 
   * @param childElement The element to add
   */
  public void addChild(Element childElement)
  {
    this.addChildItem(childElement);
  } // addChild() 

  // -------------------------------------------------------------------------

  /**
   * Adds the given item as a child to the receiver.
   * 
   * @param item The item to add.
   */
  public void addChildItem(XmlItem item)
  {
    if (item == null)
    {
      return;
    }
    this.getInternalChildItems().add(item);
  } // addChildItem() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Inserts the given newChild after the specified old child.
   * Returns true if the old child was found and the new child successfully
   * inserted after it.
   */
  public boolean insertChildAfter(XmlItem oldChild, XmlItem newChild)
  {
    int index;

    if ((oldChild == null) || (newChild == null))
    {
      return false;
    }
    index = this.indexOfChild(oldChild);
    if (index == NOT_FOUND_INDEX)
    {
      return false;
    }
    this.insertAfter(index, newChild);
    return true;
  } // insertChildAfter() 

  // -------------------------------------------------------------------------

  /**
   * Inserts the given newChild before the specified old child.
   * Returns true if the old child was found and the new child successfully
   * inserted after it.
   */
  public boolean insertChildBefore(XmlItem oldChild, XmlItem newChild)
  {
    int index;

    if ((oldChild == null) || (newChild == null))
    {
      return false;
    }

    index = this.indexOfChild(oldChild);
    if (index == NOT_FOUND_INDEX)
    {
      return false;
    }
    this.insertBefore(index, newChild);
    return true;
  } // insertChildBefore() 

  // -------------------------------------------------------------------------

  /**
   * Replaces the given oldChild by the given newChild.
   * Returns true if the old child was found and replaced.
   */
  public boolean replaceChild(XmlItem oldChild, XmlItem newChild)
  {
    int index;

    if ((oldChild == null) || (newChild == null))
    {
      return false;
    }

    index = this.indexOfChild(oldChild);
    if (index == NOT_FOUND_INDEX)
    {
      return false;
    }
    this.getInternalChildItems().set(index, newChild);
    return true;
  } // replaceChild() 

  // -------------------------------------------------------------------------

  /**
   * Returns the first element in the hierarchy that matches the given filter
   * or null if none matches.
   * 
   * @param filter The filter that defines which elements match (must not be null).
   * @return The found element or null.
   */
  public Element findFirst(IElementFilter filter) 
  {
    Element[] elements;
    
    elements = this.find(filter);
    if (elements.length > 0)
    {
      return elements[0];
    }
    return null;
  } // findFirst() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns all sub elements matching the given filter.
   * This method returns all matching elements down the whole hierarchy.
   * 
   * @param filter The filter that defines which elements match (must not be null).
   * @return An array of elements (never null).
   */
  public Element[] find(IElementFilter filter) 
  {
    List<Element> result;
    
    result = this.collect(filter, true);
    if (filter.matches(this))
    {
      result.add(0, this);
    }
    return Element.toArray(result);
  } // find() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Returns all sub elements with the given tag name plus this element,
   * if it's name is equal to the given tagName.
   * This method returns all matching elements down the whole hierarchy.
   * 
   * @return An array of elements (never null)
   */
  public Element[] find(String tagName)
  {
    return this.find(tagName, null);
  } // find() 

  // -------------------------------------------------------------------------

  /**
   * Returns all sub elements with the given tag name and the specified
   * attribute values plus this element if it matches the tagName and
   * attributes.
   * This method returns all matching elements down the whole hierarchy.
   * 
   * @param tagName The name of the elements to be found (must not be null).
   * @param attributes Definition of attributes the elements must match (may be null).
   * @return An array of elements (never null)
   */
  public Element[] find(String tagName, Map<String, String> attributes)
  {
    List<Element> result;
    IElementFilter filter;

    filter = new ElementAndFilter(new ElementNameFilter(tagName), new ElementAttributesFilter(attributes));
    result = this.collect(filter, true);
    if (filter.matches(this))
    {
      result.add(0, this);
    }
    return Element.toArray(result);
  } // find() 

  // -------------------------------------------------------------------------

  /**
   * Returns all sub elements with the given tag name. 
   * That is, all direct children
   * and children of those children and so on. It goes down recursively the
   * whole hierarchy.
   * 
   * @return An array of elements (never null)
   */
  public Element[] findSubElements(String tagName)
  {
    return this.collectElements(tagName, null, true);
  } // findSubElements() 

  // -------------------------------------------------------------------------

  /**
   * Returns all sub elements with the given tag name and the specified
   * attribute values. That is, all direct children
   * and children of those children and so on. It goes down recursively the
   * whole hierarchy.
   */
  public Element[] findSubElements(String tagName, Map<String, String> attributes)
  {
    return this.collectElements(tagName, attributes, true);
  } // findSubElements() 

  // -------------------------------------------------------------------------

  /**
   * Returns all children with the given tag name. 
   * This method returns only direct children of this element.
   * It does not go deeper into the hierarchy.
   * 
   * @return An array of elements (never null)
   */
  public Element[] findChildren(String tagName)
  {
    return this.collectElements(tagName, null, false);
  } // findChildren() 

  // -------------------------------------------------------------------------

  /**
   * Returns all children with the given tag name and the specified 
   * attribute values. 
   * This method returns only direct children of this element.
   * It does not go deeper into the hierarchy.
   * 
   * @return An array of elements (never null)
   */
  public Element[] findChildren(String tagName, Map<String, String> attributes)
  {
    return this.collectElements(tagName, attributes, false);
  } // findChildren() 

  // -------------------------------------------------------------------------

  /**
   * Returns a shallow copy of the element, that is a new element with the same
   * name, same attributes and same text but without children.
   */
  public Element shallowCopy()
  {
    Element copy;
    String[] names;

    copy = new Element(this.getName());
    if (this.hasAttributes())
    {
      names = this.getAttributeNames();
      for (int i = 0; i < names.length; i++)
      {
        copy.setAttribute(names[i], this.getAttribute(names[i]));
      }
    }
    if (this.hasText())
    {
      copy.setText(this.getText());
    }
    return copy;
  } // shallowCopy() 

  // -------------------------------------------------------------------------

  /**
   * Returns a deep copy of the element. That is a new element with the same
   * name, same attributes, the same text and with a copy of all child elements.
   */
  public Element deepCopy()
  {
    Element copy;
    Element element;

    copy = this.shallowCopy();
    if (this.hasChildren())
    {
      for (XmlItem item : this.getInternalChildItems())
      {
        switch (item.getItemType())
        {
          case ELEMENT :
            element =(Element)item;
            copy.addChildItem(element.deepCopy());            
            break;
          case PI :
            copy.addChildItem(item);                        
          default :
            break;
        }
      }
    }
    return copy;
  } // deepCopy() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of the element with all child elements copied as well.
   * This is a deep copy.
   * 
   * @see #deepCopy()
   */
  public Element copy()
  {
    return this.deepCopy();
  } // copy() 

  // -------------------------------------------------------------------------

  /**
   * Returns a map with the names of namespaces as keys and the corresponding
   * namespace URIs as values.
   * The map contains only those namespace definitions that are set by
   * attributes of this element.
   * If no namespace is declared in this element the returned map is empty. 
   */
  public Map getNamespaceDeclarations()
  {
    if (this.getNamespaces() == null)
    {
      this.setNamespaces(this.detectNamespaceDeclarations());
    }
    return this.getNamespaces();
  } // getNamespaceDeclarations() 

  // -------------------------------------------------------------------------

  /**
   * Returns all children of all types. That is Elements and XmlProcessinGinstructions.
   */
  public XmlItem[] getChildItems() 
  {
    return Element.coll().toArray(this.getInternalChildItems(), XmlItem.class);
  } // getChildItems() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  /**
   * Returns all elements with the given tag name.
   */
  protected List<Element> collect(IElementFilter elementFilter, boolean recursive)
  {
    ArrayList result;
    Element element;

    if (!this.hasChildren())
    {
      return new ArrayList();
    }
    result = new ArrayList(this.getChildrenCount());

    for (XmlItem item : this.getInternalChildItems())
    {
      if (item instanceof Element)
      {
        element = (Element)item;
        if (elementFilter.matches(element))
        {
          result.add(element);
        }
        if (recursive)
        {
          result.addAll(element.collect(elementFilter, true));
        }
      }
    }
    return result;
  } // collect() 

  // -------------------------------------------------------------------------

  protected Element[] collectElements(String tagName, Map<String, String> attributes, boolean all)
  {
    IElementFilter filter;
    
    filter = new ElementAndFilter(new ElementNameFilter(tagName), new ElementAttributesFilter(attributes));
    return Element.toArray(this.collect(filter, all));
  } // collectElements() 

  // -------------------------------------------------------------------------

  protected void setChildItems(List<XmlItem> items)
  {
    this.removeAllChildren();
    this.getInternalChildItems().addAll(items);
  } // setChildItems() 
  
  // -------------------------------------------------------------------------
  
  protected boolean matches(Element element, String tagName, Map<String, String> attributes)
  {
    Iterator iterator;
    Map.Entry entry;
    String key;
    String expectedValue;
    String value;

    if (tagName.equals(element.getName()))
    {
      if (attributes != null)
      {
        iterator = attributes.entrySet().iterator();
        while (iterator.hasNext())
        {
          entry = (Map.Entry)iterator.next();
          key = (String)entry.getKey();
          value = element.getAttribute(key);
          if (value == null)
          {
            return false;
          }
          expectedValue = (String)entry.getValue();
          if (!value.equals(expectedValue))
          {
            return false;
          }
        }
      }
      return true;
    }
    return false;
  } // matches() 

  // -------------------------------------------------------------------------

  protected Map detectNamespaceDeclarations()
  {
    String[] attrNames;
    Map foundNamespaces;
    String nsId;
    String nsUri;

    foundNamespaces = new HashMap();
    attrNames = this.getAttributeNames();
    for (int i = 0; i < attrNames.length; i++)
    {
      if (attrNames[i].startsWith(NAMESPACE_PREFIX))
      {
        nsId = this.str().suffix(attrNames[i], NAMESPACE_SEPARATOR);
        nsUri = this.getAttribute(attrNames[i]);
        foundNamespaces.put(nsId, nsUri);
      }
    }
    return foundNamespaces;
  } // detectNamespaceDeclarations() 

  // -------------------------------------------------------------------------

  protected int indexOfChild(XmlItem child)
  {
    if ((this.getChildItemCount() > 0) && (child != null))
    {
      int i = 0;
      for (XmlItem item : this.getInternalChildItems())
      {
        if (item == child)
        {
          return i;
        }
        i++;
      }
    }
    return NOT_FOUND_INDEX;
  } // indexOfChild() 

  // -------------------------------------------------------------------------

  /**
   * Inserts the given child after the given index
   */
  protected void insertAfter(int index, XmlItem item)
  {
    if (item == null)
    {
      return;
    }
    this.getInternalChildItems().add(index+1, item);
  } // insertAfter() 

  // -------------------------------------------------------------------------

  /**
   * Inserts the given child before the given index
   */
  protected void insertBefore(int index, XmlItem item)
  {
    if (item == null)
    {
      return;
    }
    this.getInternalChildItems().add(index, item);
  } // insertBefore() 

  // -------------------------------------------------------------------------

  protected String[] getAttributeNames(boolean withPrefix)
  {
    String attrName;
    String[] names = StringUtil.EMPTY_STRING_ARRAY;
    Iterator nameIterator;
    int index = 0;

    if (this.hasAttributes())
    {
      names = new String[this.getElementAttributes().size()];
      nameIterator = this.getElementAttributes().names().iterator();
      while (nameIterator.hasNext())
      {
        attrName = (String)nameIterator.next();
        if (!withPrefix)
        {
          attrName = this.getLocalNameOf(attrName);
        }
        names[index] = attrName;
        index++;
      }
    }
    return names;
  } // getAttributeNames() 

  // -------------------------------------------------------------------------

  protected String getNamePrefixOf(String aName)
  {
    if (aName.indexOf(NAMESPACE_SEPARATOR) > 0)
    {
      return this.str().cutTail(aName, NAMESPACE_SEPARATOR);
    }
    return StringUtil.EMPTY_STRING;
  } // getNamePrefixOf() 

  // -------------------------------------------------------------------------

  protected String getLocalNameOf(String aName)
  {
    if (aName.indexOf(NAMESPACE_SEPARATOR) > 0)
    {
      return this.str().cutHead(aName, NAMESPACE_SEPARATOR);
    }
    return aName;
  } // getLocalNameOf() 

  // -------------------------------------------------------------------------
  
  protected List<XmlItem> getInternalChildItems()
  {
    return internalChildItems;
  } // getInternalChildItems() 
  
  // -------------------------------------------------------------------------
  
  protected List<Element> collectContainedElements() 
  {
    List<Element> result;
    
    result = new ArrayList(this.internalChildItems.size());
    for (XmlItem item : this.internalChildItems)
    {
      if (item instanceof Element)
      {
        result.add((Element)item);
      }
    }
    return result;
  } // collectContainedElements() 
  
  // -------------------------------------------------------------------------

  protected StringUtil str()
  {
    return StringUtil.current();
  } // str() 

  // -------------------------------------------------------------------------

} // class Element 
