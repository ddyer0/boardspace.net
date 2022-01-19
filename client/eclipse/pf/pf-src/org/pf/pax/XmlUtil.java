// ===========================================================================
// CONTENT  : CLASS XmlUtil
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.1 - 13/01/2012
// HISTORY  :
//  30/01/2011  mdu  CREATED
//	13/01/2012	mdu		added	--> parse()
//
// Copyright (c) 2011-2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class provides general helper classes that normally should be part
 * of the Java API.
 *
 * @author Manfred Duchrow
 * @version 1.1
 */
public class XmlUtil
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	/**
	 * The prefix name that is used for the default namespace (i.e. "dns").
	 */
	public static final String DEFAULT_NS_PREFIX = "dns";

	// =========================================================================
	// CLASS VARIABLES
	// =========================================================================
	private static XmlUtil soleInstance = new XmlUtil();

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================

	// =========================================================================
	// CLASS METHODS
	// =========================================================================

	/**
	 * Returns the only instance this class supports (design pattern "Singleton")
	 */
	public static XmlUtil current()
	{
		return soleInstance;
	} // current() 

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	protected XmlUtil()
	{
		super();
	} // XmlUtil() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Returns the document object read from the specified XML file.
	 * If the file cannot be found directly with the given name it will
	 * be looked-up on the classpath.
	 */
	public Document readXmlFile(final String filename) throws Exception
	{
		File file;
		URL url;
		InputStream inputStream;
		Document document;
		InputSource inputSource;

		file = new File(filename);
		if (!file.exists())
		{
			url = XmlUtil.class.getClassLoader().getResource(filename);
			file = new File(url.getFile());
		}
		inputStream = new FileInputStream(file);
		inputSource = new InputSource(inputStream);
		try
		{
			document = this.parse(inputSource);
		}
		finally
		{
			this.close(inputStream);
		}
		return document;
	} // readXmlFile() 

	// -------------------------------------------------------------------------

  /**
   * Returns the Document object for the specified XML file.
   * Validates the XML content against the schema in the specified schema file.
   * Both filenames are treated in the following way:<br>
   * <ol>
   * <li>If the filename is absolute, the file will be read directly.</li>
   * <li>If the filename is relative, the file will looked-up relative to the current working directory.</li>
   * <li>If the filename is relative, and has not been found in the current working directory, it will be looked-up in the classpath.</li>
   * <li>If a file with the given filename is still not found, a FileNotFoundException will be thrown.</li>
   * </ol>
   * @param xmlFileName The name of the XML file to read (must not be null)
   * @param schemaFileName The name of the XML schema (XSD) file (may be null)
   */
	public Document readXmlDocument(String xmlFileName, String schemaFileName) throws Exception
	{
		Schema schema;

		schema = this.readSchema(schemaFileName);
		return this.readXmlDocument(xmlFileName, schema);
	} // readXmlDocument() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the Document object for the specified XML file.
	 * Validates the XML content against the given schema (if not null).
	 * The xmlFileName is treated in the following way:<br>
	 * <ol>
	 * <li>If the filename is absolute, the file will be read directly.</li>
	 * <li>If the filename is relative, the file will looked-up relative to the current working directory.</li>
	 * <li>If the filename is relative, and has not been found in the current working directory, it will be looked-up in the classpath.</li>
	 * <li>If a file with the given filename is still not found, a FileNotFoundException will be thrown.</li>
	 * </ol>
	 * @param xmlFileName The name of the XML file to read (must not be null)
	 * @param schema The schema to validate the XML data against (may be null)
	 */
	public Document readXmlDocument(final String xmlFileName, final Schema schema) throws Exception
	{
		File file;
		InputStream inputStream;
		Document document = null;

		file = this.findFile(xmlFileName);
		inputStream = new FileInputStream(file);
		InputSource inputSource = new InputSource(inputStream);

		try
		{
			document = this.parse(inputSource, schema);
		}
		finally
		{
			this.close(inputStream);
		}
		return document;
	} // readXmlDocument() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns the Schema object for the specified XML schema file.
	 * The schemaFileName is treated in the following way:<br>
	 * <ol>
	 * <li>If the filename is null of an empty or blank string, null will be returned.</li>
	 * <li>If the filename is absolute, the file will be read directly.</li>
	 * <li>If the filename is relative, the file will looked-up relative to the current working directory.</li>
	 * <li>If the filename is relative, and has not been found in the current working directory, it will be looked-up in the classpath.</li>
	 * <li>If a file with the given filename is still not found, a FileNotFoundException will be thrown.</li>
	 * </ol>
	 * @param schemaFileName The name of the XML schema file to read (may be null)
	 * @return The schema object representing the schema definition in the specified file or
	 * null if the given filename was null or blank/empty.
	 */
	public Schema readSchema(String schemaFileName) throws FileNotFoundException, SAXException
	{
		SchemaFactory schemaFactory;
		File schemaFile;

		if ((schemaFileName == null) || (schemaFileName.length() == 0))
		{
			return null;
		}
		schemaFile = this.findFile(schemaFileName.trim());
		schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		return schemaFactory.newSchema(schemaFile);
	} // readSchema() 
	
	// -------------------------------------------------------------------------

	/**
	 * Adds all namespaces that are declared in the given element with their
	 * prefix to the provided namespace prefix mapper.
	 *
	 * @param namespaces An implementation of a NamespaceContext that can collect prefix to namespace mappings (must not be null)
	 * @param element The element from which to extract namespace declarations.
	 */
	public void addNamespacesFrom(NamespacePrefixMapper namespaces, Element element)
	{
		Attr attribute;
		NamedNodeMap attributes;
		String prefix;
		String nsPrefix;
		String nsURI;

		attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++)
		{
			attribute = (Attr)attributes.item(i);
			prefix = attribute.getPrefix();
			if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))
			{
				nsPrefix = attribute.getLocalName();
				nsURI = attribute.getTextContent();
				namespaces.addMapping(nsPrefix, nsURI);
			}
		}
	} // addNamespacesFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Adds all namespaces that are declared in the given element and all child elements
	 * together with their prefixes to the provides namespace prefix mapper.
	 *
	 * @param namespaces An implementation of a NamespaceContext that can collect prefix to namespace mappings (must not be null)
	 * @param element The element from which to start recursivly extracting namespace declarations.
	 */
	public void addNamespacesFromElementAndChildren(NamespacePrefixMapper namespaces, Element element)
	{
		NodeList childNodes;
		Node node;

		this.addNamespacesFrom(namespaces, element);
		childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++)
		{
			node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				this.addNamespacesFromElementAndChildren(namespaces, (Element)node);
			}
		}
	} // addNamespacesFromElementAndChildren() 

	// -------------------------------------------------------------------------

	/**
	 * Extracts all namespace definitions from the given root element and returns 
	 * them in a NamespaceContext implementation.
	 */
	public NamespaceContext extractNamespacesFrom(final Element rootElement)
	{
		NamespacePrefixMapper mapper;

		mapper = new NamespacePrefixMapper();
		if (rootElement.hasAttribute(XMLConstants.XMLNS_ATTRIBUTE))
		{
			mapper.addMapping(DEFAULT_NS_PREFIX, rootElement.getAttribute(XMLConstants.XMLNS_ATTRIBUTE));
		}
		this.addNamespacesFromElementAndChildren(mapper, rootElement);
		return mapper;
	} // extractNamespacesFrom() 

	// -------------------------------------------------------------------------

	/**
	 * Extracts all namespace definitions from the given document and returns 
	 * them in a NamespaceContext implementation.
	 */
	public NamespaceContext extractNamespacesFrom(final Document document)
	{
		return this.extractNamespacesFrom(document.getDocumentElement());
	} // extractNamespacesFrom() 

	// -------------------------------------------------------------------------  

  /**
   * Returns the namespace URI of the given element or null if none can be found.
   * This method is doing a recursive look-up for the namespaceURI. That is, if
   * the element has no namespaceURI set the parent element will be used and so on
   * until no parent element exists.
   * @param element The element of interest.
   * @return The name of the element's namespace URI or null if nonen can be determined.
   */
  public String getNamespaceURIOf(Element element) 
  {
    String nsURI;
    
    if (element == null)
    {
      return null;
    }
    nsURI = element.getNamespaceURI();
    if (nsURI != null)
    {
      return nsURI;
    }
    return this.getNamespaceURIOf((Element)element.getParentNode());
  } // getNamespaceURIOf() 
  
  // -------------------------------------------------------------------------
  
	/**
	 * Returns the first element in the given document that matches the specified
	 * tagName. If no such element can be found this method returns null.
	 * The element will be searched for in the whole hierarchy under the given document.
	 *
	 * @param document The document in which to find the element (must not be null)
	 * @param tagName The name of the element to find (must not be null)
	 * @return The found element or null
	 */
	public Element getFirstElementByTagName(Document document, String tagName)
	{
		NodeList nodes;

		if ((document == null) || (tagName == null))
		{
			return null;
		}
		nodes = document.getElementsByTagName(tagName);
		switch (nodes.getLength())
		{
			case 0 :
				return null;
			default :
				return (Element)nodes.item(0);
		}
	} // getFirstElementByTagName() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns the first element under the given element that matches the specified
	 * tagName. If no such element can be found this method returns null.
	 * The element will be searched for in the whole hierarchy under the given element.
	 *
	 * @param element The element under which to find the element (must not be null)
	 * @param tagName The name of the element to find (must not be null)
	 * @return The found element or null
	 */
	public Element getFirstElementByTagName(Element element, String tagName)
	{
		NodeList nodes;

		if ((element == null) || (tagName == null))
		{
			return null;
		}
		nodes = element.getElementsByTagName(tagName);
		switch (nodes.getLength())
		{
			case 0 :
				return null;
			default :
				return (Element)nodes.item(0);
		}
	} // getFirstElementByTagName() 

	// -------------------------------------------------------------------------
	
	/**
	 * Returns a list of all direct children of the given element.
	 * If no child element exists an empty list will be returned.
	 */
	public List<Element> getChildElements(Element element)
	{
		NodeList nodes;
		Node item;
		List<Element> children;

		if (element == null)
		{
			return null;
		}
		children = new ArrayList<Element>();
		nodes = element.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++)
		{
			item = nodes.item(i);
			if (item instanceof Element)
			{
				children.add((Element)item);
			}
		}
		return children;
	} // getChildElements() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns the first Element under the given element.
	 * If no child element exists null will be returned.
	 */
	public Element getFirstChildElement(Element element)
	{
		NodeList nodes;
		Node item;

		if (element == null)
		{
			return null;
		}
		nodes = element.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++)
		{
			item = nodes.item(i);
			if (item instanceof Element)
			{
				return (Element)item;
			}
		}
		return null;
	} // getFirstChildElement() 
	
	// -------------------------------------------------------------------------

	/**
	 * Returns the value of the attribute with the given local name
	 * in the specified element.
	 * If no attribute with that name exists in the element null will be returned.
	 *
	 * @param element The element from which to retrieve the attribute
	 * @param localName The local name (i.e. without any prefix) of the attribute
	 */
	public String getAttributeLN(Element element, String localName)
	{
		NamedNodeMap nodes;
		Node item;
		Attr attribute;

		if (element == null)
		{
			throw new IllegalArgumentException("element may not be null");
		}

		if (localName == null)
		{
			throw new IllegalArgumentException("localName may not be null");
		}

		nodes = element.getAttributes();
		for (int i = 0; i < nodes.getLength(); i++)
		{
			item = nodes.item(i);
			if (item instanceof Attr)
			{
				attribute = (Attr)item;

				if (localName.equals(attribute.getLocalName()))
				{
					return attribute.getNodeValue();
				}
			}
		}
		return null;
	} // getAttributeLN() 

	// -------------------------------------------------------------------------

	public String asString(Element element) throws TransformerException
	{
		Transformer transformer;
		StringWriter stringWriter;

		stringWriter = new StringWriter(2000);
		transformer = TransformerFactory.newInstance().newTransformer();
		transformer.transform(new DOMSource(element), new StreamResult(stringWriter));
		return stringWriter.toString();
	} // asString() 

	// -------------------------------------------------------------------------

	public Document parseXmlString(String xml) throws Exception
	{
		StringReader reader;

		reader = new StringReader(xml);
		return this.parse(new InputSource(reader));
	} // parseXmlString() 

	// -------------------------------------------------------------------------
	
	/**
	 * Converts a nodeList to a list of Node objects.
	 */
	public List<Node> nodeListToListOfNodes(NodeList nodeList) 
	{
		List<Node> result;
		Node node;

		result = new ArrayList<Node>(nodeList.getLength());
		for (int i = 0; i < nodeList.getLength(); i++)
		{
			node = nodeList.item(i);
			result.add(node);
		}
		return result;
	} // nodeListToListOfNodes()
	
	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
  protected Document parse(InputSource inputSource) throws SAXException, IOException, ParserConfigurationException
  {
    return parse(inputSource, null);
  } // parse()
  
  // -------------------------------------------------------------------------
  
  protected Document parse(InputSource inputSource, Schema schema) throws SAXException, IOException, ParserConfigurationException
  {
    DocumentBuilderFactory builderFactory;
    DocumentBuilder builder;
    Document document;

    builderFactory = DocumentBuilderFactory.newInstance();
    builderFactory.setNamespaceAware(true);
	  builderFactory.setValidating(false);
	  builderFactory.setExpandEntityReferences(false);
	  builderFactory.setCoalescing(true);
    if (schema != null)
    {
      builderFactory.setSchema(schema);
    }
    builder = builderFactory.newDocumentBuilder();
    builder.setEntityResolver(new EmptyContentEntityResolver());
    document = builder.parse(inputSource);
    return document;
  } // parse()

  // -------------------------------------------------------------------------
  
  /**
   * Tries to find the file with the given name. 
   * The file look-up is done in the following steps: 
   * <ol>
   * <li>If the filename is absolute, the file will be read directly.</li>
   * <li>If the filename is relative, the file will looked-up relative to the current working directory.</li>
   * <li>If the filename is relative, and has not been found in the current working directory, it will be looked-up via the 
   * class loader of XmlUtil.</li>
   * <li>If a file with the given filename is still not found, a FileNotFoundException will be thrown.</li>
   * </ol>
   * @param filename The name of the file to find
   * @return A file object that exists on the file system
   * @throws FileNotFoundException If the file could not be found.
   */
  protected File findFile(String filename) throws FileNotFoundException
  {
    File file;
    URL url;
    
    file = new File(filename) ;
    if (file.exists())
    {
      return file;
    }
    if (!file.isAbsolute())
    {
      url = XmlUtil.class.getClassLoader().getResource(filename);
      if (url != null)
      {
        file = new File(url.getFile());        
      }
    }
    if (!file.exists())
    {
      throw new FileNotFoundException(file.getAbsolutePath());
    }
    return file;
  } // findFile() 
  
  // -------------------------------------------------------------------------
  
	protected void close(Closeable closeable)
	{
		try
		{
			closeable.close();
		}
		catch (IOException ex)
		{
			// Deliberately ignore that.
		}
	} // close() 

	// -------------------------------------------------------------------------

} // class XmlUtil 
