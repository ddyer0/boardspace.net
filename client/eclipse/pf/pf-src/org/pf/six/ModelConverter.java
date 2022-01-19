// ===========================================================================
// CONTENT  : CLASS ModelConverter
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.1 - 18/01/2014
// HISTORY  :
//  29/06/2002  duma  CREATED
//	19/03/2004	duma	added		->	Namespace URI support when converting toDOM()
//	07/02/2010	mdu		added		->	6 toFormattedString() methods
//  18/01/2014  mdu   added   ->  conversion of processing instructions
//
// Copyright (c) 2002-2014, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.six ;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.pf.logging.Logger;
import org.pf.pax.DOMTreeXMLReader;
import org.pf.text.StringUtil;
import org.pf.util.StackedMap;
import org.w3c.dom.Document;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.SAXException;

/**
 * Supports conversion of a XML representation in org.pf.six.Element
 * to a DOM tree and vice versa as well as pretty printing of both kinds
 * of XML representations to Strings. 
 *
 * @author Manfred Duchrow
 * @version 2.1
 */
public class ModelConverter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private Document document = null ;
  protected Document getDocument() { return document ; }
  protected void setDocument( Document newValue ) { document = newValue ; }
  
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
	public ModelConverter()
	{
		super();
	} // ModelConverter() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Converts the given SIX XML document and all its children (Elements and Processing instructions) 
	 * to a DOM Document tree.
	 * 
	 * @param xmlDocument The SIX XML document to convert to a DOM document (must not be null).
	 * @return The document or null in any case of exception
	 */
	public Document toDOM(XmlDocument xmlDocument)
	{
	  this.newDocument();
	  if (xmlDocument.hasProcessingInstructions())
    {
      this.convertProcessingInstructions(xmlDocument);
    }
	  this.convertToDOM(xmlDocument.getRootElement(), false);
		return this.getDocument();
	} // toDOM() 

  // -------------------------------------------------------------------------

	/**
	 * Converts the given SIX element and its children (Elements and Processing instructions)
	 * to a DOM Document tree.
	 * The element needs not necessarily be a root element (document type).
	 * 
	 * @param rootElement The root element for the document to create.
	 * @return The document or null in any case of exception.
	 */
	public Document toDOM(Element rootElement)
	{
	  this.convertToDOM(rootElement, true);
	  return this.getDocument();
	} // toDOM() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Converts the given DOM Document tree to a SIX XmlDocument tree.
	 * 
	 * @param doc The document to be converted to the SIX structure.
	 * @return The root element or null in any case of exception.
	 */
	public XmlDocument toSIXDocument(Document doc)
	{
	  StringTagInterpreterController controller;
	  
	  controller = this.toSIX(doc);
	  return controller.getXmlDocument();
	} // toSIXDocument() 
	
	// -------------------------------------------------------------------------	
	
	/**
	 * Converts the given DOM Document tree to a SIX element tree.
	 * 
	 * @param doc The document to be converted to the SIX structure
	 * @return The root element or null in any case of exception
	 */
	public Element toSIXElement(Document doc)
	{
		StringTagInterpreterController controller;
		
		controller = this.toSIX(doc);
		return controller.getElement();
	} // toSIXElement() 

	// -------------------------------------------------------------------------	

	/**
	 * Converts the given DOM Element tree to a SIX element tree.
	 * 
	 * @param domElement The root element to start converting to the SIX structure
	 * @return The corresponding SIX element or null in any case of exception
	 */
	public Element toSIXElement(org.w3c.dom.Element domElement)
	{
		StringTagInterpreterController controller = null;
		DOMTreeXMLReader xmlReader = null;

		xmlReader = new DOMTreeXMLReader();
		try
		{
			controller = new StringTagInterpreterController(xmlReader);
			xmlReader.parse(domElement);
		}
		catch (SAXException e)
		{
			logger().logException(e);
			return null;
		}
		return controller.getElement();
	} // toSIXElement() 

	// -------------------------------------------------------------------------	

	/**
	 * Returns the element and its attributes and children as string with
	 * new lines and indentation (2 chars).
	 * 
	 * @param element An XML element, which is not necessarily the root of an XML document 
	 */
	public String toFormattedString(Element element)
	{
		return this.toFormattedString(element, 2);
	} // toFormattedString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the element and its attributes and children as string with
	 * new lines and indentation.
	 * 
	 * @param element An XML element, which is not necessarily the root of an XML document
	 * @param indent Specifies how much characters to indent after a new line  
	 */
	public String toFormattedString(Element element, int indent)
	{
		return this.toFormattedString(element, indent, false);
	} // toFormattedString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the element and its attributes and children as string with
	 * new lines and indentation.
	 * 
	 * @param element An XML element, which is not necessarily the root of an XML document
	 * @param indent Specifies how much characters to indent after a new line
	 * @param useSingleQuotes If true attribute values will be enclosed in quotes ('), otherwise in apostrophes (")  
	 */
	public String toFormattedString(Element element, int indent, boolean useSingleQuotes)
	{
		this.toDOM(element);
		return this.toFormattedString(this.getDocument().getDocumentElement(), indent, useSingleQuotes);
	} // toFormattedString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the element and its attributes and children as string with
	 * new lines and indentation (2 chars).
	 * 
	 * @param element A DOM element, which is not necessarily the root of an XML document 
	 */
	public String toFormattedString(org.w3c.dom.Element element)
	{
		return this.toFormattedString(element, 2);
	} // toFormattedString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the element and its attributes and children as string with
	 * new lines and indentation.
	 * 
	 * @param element A DOM element, which is not necessarily the root of an XML document
	 * @param indent Specifies how much characters to indent after a new line  
	 */
	public String toFormattedString(org.w3c.dom.Element element, int indent)
	{
		return this.toFormattedString(element, indent, false);
	} // toFormattedString() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the element and its attributes and children as string with
	 * new lines and indentation.
	 * 
	 * @param element A DOM element, which is not necessarily the root of an XML document
	 * @param indent Specifies how much characters to indent after a new line
	 * @param useSingleQuotes If true attribute values will be enclosed in quotes ('), otherwise in apostrophes (")  
	 */
	public String toFormattedString(org.w3c.dom.Element element, int indent, boolean useSingleQuotes)
	{
		XmlStreamWriter xmlWriter;
		StringWriter stringWriter;

		stringWriter = new StringWriter(500);
		xmlWriter = new XmlStreamWriter();
		xmlWriter.withNamespacePrefix(true);
		xmlWriter.setIndentation(indent > 0 ? indent : 2);
		if (useSingleQuotes)
		{
			xmlWriter.useSingleQuotes();
		}
		xmlWriter.appendDOMElement(element, stringWriter);
		return stringWriter.toString();
	} // toFormattedString() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
  /**
   * Converts the given SIX element and its children to a DOM Document tree.
   * The element needs not necessarily be a root element (document type).
   * 
   * @param rootElement The root element for the document to create.
   * @param createNewDocument If true this method creates a new document, otherwise 
   * it assumes that there is already one available via {@link #getDocument()}.
   * @return The document or null in any case of exception
   */
  protected void convertToDOM(Element rootElement, boolean createNewDocument)
  {
    org.w3c.dom.Element rootDOMElement;
    
    if (createNewDocument)
    {      
      this.newDocument();
    }
    if (rootElement != null)
    {
      rootDOMElement = this.convertElement(rootElement, new NamespaceStack());
      this.getDocument().appendChild(rootDOMElement);
    }
  } // convertToDOM() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Converts the given DOM Document tree to a SIX element tree.
   * 
   * @param doc The document to be converted to the SIX structure.
   * @return The controller used to build the SIX tree.
   */
  protected StringTagInterpreterController toSIX(Document doc)
  {
    StringTagInterpreterController controller = null;
    DOMTreeXMLReader xmlReader = null;

    xmlReader = new DOMTreeXMLReader();
    try
    {
      controller = new StringTagInterpreterController(xmlReader);
      xmlReader.parse(doc);
    }
    catch (SAXException e)
    {
      logger().logException(e);
      return null;
    }
    return controller;
  } // toSIX() 

  // -------------------------------------------------------------------------  

	protected org.w3c.dom.Element convertElement(Element element, NamespaceStack namespaces)
	{
		org.w3c.dom.Element domElement;

		namespaces.push(element.getNamespaceDeclarations());

		domElement = this.createDOMElement(element.getName(), namespaces);

		this.convertAttributes(element, domElement, namespaces);
		this.convertText(element, domElement);
		this.convertChildren(element, domElement, namespaces);

		namespaces.pop();

		return domElement;
	} // convertElement() 

	// -------------------------------------------------------------------------

	protected void convertText(Element element, org.w3c.dom.Element domElement)
	{
		if (element.hasText())
    {
      domElement.appendChild(this.createDOMText(element.getText()));
    }
	} // convertText() 

	// -------------------------------------------------------------------------

	protected void convertAttributes(Element element, org.w3c.dom.Element domElement, NamespaceStack namespaces)
	{
		String[] attrNames;
		String attrName;
		String value;
		String nsURI;

		if (element.hasAttributes())
		{
			attrNames = element.getAttributeNames();
			for (int i = 0; i < attrNames.length; i++)
			{
				attrName = attrNames[i];
				value = element.getAttribute(attrName);
				nsURI = namespaces.namespaceURIFor(attrNames[i]);
				if (nsURI == null)
				{
					domElement.setAttribute(attrName, value);
				}
				else
				{
					domElement.setAttributeNS(nsURI, attrName, value);
				}
			}
		}
	} // convertAttributes() 

	// -------------------------------------------------------------------------

  protected ProcessingInstruction convertProcessingInstruction(XmlProcessingInstruction pi)
  {
    return this.createDOMProcessingInstruction(pi.getName(), pi.getData());
  } // convertProcessingInstruction() 
  
  // -------------------------------------------------------------------------
  
	protected void convertChildren(Element element, org.w3c.dom.Element domElement, NamespaceStack namespaces)
	{
		XmlItem[] childItems;

		if (element.hasChildItems())
		{
			childItems = element.getChildItems();
			for (XmlItem childItem : childItems)
      {
			  switch (childItem.getItemType())
			  {
			    case ELEMENT :
			      domElement.appendChild(this.convertElement((Element)childItem, namespaces));        			      
			      break;
			    case PI :
			      domElement.appendChild(this.convertProcessingInstruction((XmlProcessingInstruction)childItem));        			      			      
			      break;
			    default :
			      break;
			  }
      }
		}
	} // convertChildren() 

  // -------------------------------------------------------------------------

  protected void convertProcessingInstructions(XmlDocument xmlDocument)
  {
    for (XmlProcessingInstruction pi : xmlDocument.getProcessingInstructions())
    {
      this.getDocument().appendChild(this.convertProcessingInstruction(pi));
    }
  } // convertProcessingInstructions() 
  
  // -------------------------------------------------------------------------
  
	protected void newDocument()
	{
		DocumentBuilderFactory factory;
		DocumentBuilder builder;
		Document doc = null;

		factory = DocumentBuilderFactory.newInstance();
		try
		{
			builder = factory.newDocumentBuilder();
			doc = builder.newDocument();
		}
		catch (ParserConfigurationException e)
		{
			logger().logException(e);
		}

		this.setDocument(doc);
	} // newDocument() 

	// -------------------------------------------------------------------------

	protected org.w3c.dom.Element createDOMElement(String name, NamespaceStack namespaces)
	{
		String nsURI;

		nsURI = namespaces.namespaceURIFor(name);
		if (nsURI == null)
		{
			return this.getDocument().createElement(name);
		}
		else
		{
			return this.getDocument().createElementNS(nsURI, name);
		}
	} // createDOMElement() 

	// -------------------------------------------------------------------------

	protected org.w3c.dom.ProcessingInstruction createDOMProcessingInstruction(String target, String data)
	{
   return this.getDocument().createProcessingInstruction(target, data);
	} // createDOMProcessingInstruction() 
	
	// -------------------------------------------------------------------------
	
	protected org.w3c.dom.Text createDOMText(String text)
	{
		return this.getDocument().createTextNode(text);
	} // createDOMText() 

	// -------------------------------------------------------------------------

	/**
	 * Returns the current logger used by this component to report
	 * errors and exceptions. 
	 */
	protected Logger logger()
	{
		return LoggerProvider.getLogger();
	} // logger() 

	// -------------------------------------------------------------------------
	// ---- INNER CLASSES ------------------------------------------------------
	// -------------------------------------------------------------------------
	protected static class NamespaceStack extends StackedMap
	{

		public NamespaceStack()
		{
			super();
		} // NamespaceStack() 

		// -----------------------------------------------------------------------

		public String namespaceURIFor(String name)
		{
			String nsId;

			nsId = StringUtil.current().prefix(name, Element.NAMESPACE_SEPARATOR);
			if (nsId != null)
			{
				return (String)this.get(nsId);
			}
			return null;
		} // namespaceURIFor() 

		// -----------------------------------------------------------------------

	} // class NamespaceStack

	// -------------------------------------------------------------------------

} // class ModelConverter 
