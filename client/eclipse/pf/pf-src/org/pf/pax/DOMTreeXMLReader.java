// ===========================================================================
// CONTENT  : CLASS DOMTreeXMLReader
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.4 - 18/01/2014
// HISTORY  :
//  27/07/2000  duma  CREATED
//	28/02/2002	duma	changed	-> Support for SAX 2.0
//	28/11/2003	duma	changed	-> Supporting namespaces
//	30/07/2004	duma	changed	-> implements SAXConstants
//	07/02/2010	mdu		changed	-> Allow also parsing of DOM Element instead of Document only
//  18/01/2014  mdu   changed -> Support PI nodes at document level
//
// Copyright (c) 2000-2014, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This parser implements the org.xml.sax.Parser interface, but uses a
 * DOM tree as input rather than a XML stream.  <br>
 * So this class enables developers to use a DOM tree as source for
 * being handled by a ContentHandler. Of course this also includes to
 * use a DOM tree as source for the XMLTagInterpreter functionality of PAX.
 *
 * @author Manfred Duchrow
 * @version 2.4
 */
public class DOMTreeXMLReader implements XMLReader, SAXConstants
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	/**
	 * The "xmlns" prefix that is used in XML to declare a namespace prefixes.
	 */
	public static final String NAMESPACE_DECLARATION_PREFIX = "xmlns";

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private ContentHandler contentHandler = null;

	// -------------------------------------------------------------------------

	private Map features = null;
	protected Map getFeatures()
	{
		return features;
	} // getFeatures() 
	protected void setFeatures(Map newValue)
	{
		features = newValue;
	} // setFeatures() 

	// =========================================================================
	// CLASS METHODS
	// =========================================================================

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with default values.
	 */
	public DOMTreeXMLReader()
	{
		super();
		this.initFeatures();
	} // DOMTreeXMLReader() 

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Allow an application to register a document event handler.
	 */
	public void setContentHandler(ContentHandler newValue)
	{
		contentHandler = newValue;
	} // setContentHandler() 

	// -------------------------------------------------------------------------

	/**
	 * This method iterates over all elements of the specified DOM document
	 * and calls the corresponding callback-methods of its ContentHandler.
	 */
	public void parse(Document document) throws SAXException
	{
		if (document == null)
		{
			throw new SAXException("The given document is null !");
		}
		else
		{
			this.parseDocument(document);
		}
	} // parse() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * This method iterates over all elements of the specified DOM root element
	 * and calls the corresponding callback-methods of its ContentHandler.
	 */
	public void parse(Element element) throws SAXException
	{
		if (element == null)
		{
			throw new SAXException("The given element is null !");
		}
		else
		{
			this.parseElement(element);
		}
	} // parse() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not allowed here, because this parser can handle
	 * Document Object Model documents only !  <br>
	 * Therefore this method throws always an Exception !
	 */
	public void parse(InputSource source) throws SAXException
	{
		this.notSupportedError("parse( InputSource source )");
	} // parse() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not allowed here, because this parser can handle
	 * Document Object Model documents only !  <br>
	 * Therefore this method throws always an Exception !
	 */
	public void parse(String systemId) throws SAXException
	{
		this.notSupportedError("parse( String systemId )");
	} // parse() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not supported !  <br>
	 * It's just an empty implementation that does nothing.
	 */
	public void setDTDHandler(DTDHandler handler)
	{
	} // setDTDHandler() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not supported !  <br>
	 * It's just an empty implementation that does nothing.
	 */
	public void setEntityResolver(EntityResolver resolver)
	{
	} // setEntityResolver() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not supported !  <br>
	 * It's just an empty implementation that does nothing.
	 */
	public void setErrorHandler(ErrorHandler handler)
	{
	} // setErrorHandler() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not supported !  <br>
	 * Therefore this method throws always an Exception !
	 */
	public void setLocale(java.util.Locale locale) throws SAXException
	{
		this.notSupportedError("setLocale( java.util.Locale locale )");
	} // setLocale() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not supported !  <br>
	 * Therefore this method throws always an Exception !
	 */
	public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException
	{
		throw new SAXNotRecognizedException(name);
	} // getProperty() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not supported !  <br>
	 * Therefore this method throws always an Exception !
	 */
	public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException
	{
		throw new SAXNotRecognizedException(name);
	} // setProperty() 

	// -------------------------------------------------------------------------

	/**
	 * Returns whether or not the feature with the given name is on.
	 * 
	 * @param name The name of the feature
	 */
	public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException
	{
		Boolean bool;

		bool = this.lookupFeature(name);
		if (bool == null)
    {
      throw new SAXNotRecognizedException("Unknown feature <" + name + ">");
    }

		return bool.booleanValue();
	} // getFeature() 

	// -------------------------------------------------------------------------

	/**
	 * Switch the feature with the given name on or off.
	 */
	public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException
	{
		boolean currentValue;

		currentValue = this.getFeature(name); // To check if the feature is known
		if (value != currentValue)
		{
			this.basicSetFeature(name, value);
		}
	} // setFeature() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not supported !  <br>
	 * Therefore this method always returns null !
	 */
	public DTDHandler getDTDHandler()
	{
		return null;
	} // getDTDHandler() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not supported !  <br>
	 * Therefore this method always returns null !
	 */
	public ErrorHandler getErrorHandler()
	{
		return null;
	} // getErrorHandler() 

	// -------------------------------------------------------------------------

	/**
	 * This method is not supported !  <br>
	 * Therefore this method always returns null !
	 */
	public EntityResolver getEntityResolver()
	{
		return null;
	} // getEntityResolver() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================

	protected void notSupportedError(String text) throws SAXException
	{
		throw new SAXException("This method is not supported: " + text);
	} // notSupportedError() 

	// -------------------------------------------------------------------------

	public ContentHandler getContentHandler()
	{
		return contentHandler;
	} // getContentHandler() 

	// -------------------------------------------------------------------------

	/**
	 * This method iterates over all elements of the specified DOM document
	 * and calls the corresponding callback-methods of its ContentHandler.
	 */
	protected void parseDocument(Document document) throws SAXException
	{
		this.getContentHandler().startDocument();
		this.handleNodes(document.getChildNodes());
		this.getContentHandler().endDocument();
	} // parseDocument() 

	// -------------------------------------------------------------------------

	/**
	 * This method iterates over all elements of the specified DOM root element
	 * and calls the corresponding callback-methods of its ContentHandler.
	 */
	protected void parseElement(Element rootElement) throws SAXException
	{
	  this.getContentHandler().startDocument();
	  this.handleNode(rootElement);
	  this.getContentHandler().endDocument();
	} // parseDocument() 
	
	// -------------------------------------------------------------------------
	
	protected void handleNodes(NodeList nodes) throws SAXException
	{
		for (int index = 0; index < nodes.getLength(); index++)
		{
			this.handleNode((Node)nodes.item(index));
		}
	} // handleNodes() 

	// -------------------------------------------------------------------------

	protected void handleNode(Node node) throws SAXException
	{
		switch (node.getNodeType())
		{
			case Node.ELEMENT_NODE :
				this.handleElement((Element)node);
				break;
			case Node.TEXT_NODE :
				this.handleText((Text)node);
				break;
			case Node.PROCESSING_INSTRUCTION_NODE :
				this.handleProcessingInstruction((ProcessingInstruction)node);
				break;
			case Node.CDATA_SECTION_NODE :
				this.handleCData((CDATASection)node);
				break;
			case Node.COMMENT_NODE :
			case Node.ENTITY_NODE :
			case Node.ENTITY_REFERENCE_NODE :
			case Node.NOTATION_NODE :
			case Node.DOCUMENT_TYPE_NODE :
				// Deliberately ignored !
				break;
			case Node.DOCUMENT_NODE :
			case Node.DOCUMENT_FRAGMENT_NODE :
			case Node.ATTRIBUTE_NODE :
				throw new SAXException("Element at wrong place found: " + node.toString());
			default :
				throw new SAXException("Unknown element found: " + node.toString());
		} // switch()
	} // handleNode() 

	// -------------------------------------------------------------------------

	protected void handleElement(Element element) throws SAXException
	{
		AttributesImpl attrList = new AttributesImpl();
		Attr attribute = null;
		NamedNodeMap attrMap = element.getAttributes();
		Name name;
		Properties prefixDeclarations = new Properties();

		for (int index = 0; index < attrMap.getLength(); index++)
		{
			attribute = (Attr)attrMap.item(index);

			name = new Name(attribute);
			attrList.addAttribute(name.namespaceURI, name.localName, name.qName, "CDATA", attribute.getValue());
			if ((this.supportNamespaces()) && ( name.isNamespaceAttribute() ) )
			{
				prefixDeclarations.setProperty( name.getWithoutPrefix(), attribute.getValue());
			}
		}

		name = new Name(element);
		if (this.supportNamespaces())
		{
			this.triggerPrefixEvent(prefixDeclarations, true);
		}
		this.getContentHandler().startElement(name.namespaceURI, name.localName, name.qName, attrList);
		this.handleNodes(element.getChildNodes());
		this.getContentHandler().endElement(name.namespaceURI, name.localName, name.qName);
		if (this.supportNamespaces())
		{
			this.triggerPrefixEvent(prefixDeclarations, false);
		}
	} // handleElement() 

	// -------------------------------------------------------------------------

	protected void handleText(Text text) throws SAXException
	{
		String data;
		
		data = text.getData().trim();
		if ( data.length() > 0 )
		{
			this.getContentHandler().characters(data.toCharArray(), 0, data.length());
		}
	} // handleText() 

	// -------------------------------------------------------------------------

	protected void handleProcessingInstruction(ProcessingInstruction instruction) throws SAXException
	{
		this.getContentHandler().processingInstruction(instruction.getTarget(), instruction.getData());
	} // handleProcessingInstruction() 

	// -------------------------------------------------------------------------

	protected void handleCData(CDATASection cdata) throws SAXException
	{

	} // handleCData() 

	// -------------------------------------------------------------------------

	protected void triggerPrefixEvent(Properties declarations, boolean start) throws SAXException
	{
		Enumeration prefixes;
		String prefix;
		String uri;

		if ((declarations == null) || (declarations.isEmpty()))
			return;

		prefixes = declarations.propertyNames();
		while (prefixes.hasMoreElements())
		{
			prefix = (String)prefixes.nextElement();
			uri = declarations.getProperty(prefix);
			if (start)
				this.getContentHandler().startPrefixMapping(prefix, uri);
			else
				this.getContentHandler().endPrefixMapping(prefix);
		}

	} // triggerPrefixEvent() 

	// -------------------------------------------------------------------------

	protected Boolean lookupFeature(String featureName)
	{
		return (Boolean)this.getFeatures().get(featureName);
	} // lookupFeature() 

	// -------------------------------------------------------------------------

	protected boolean supportNamespaces()
	{
		return this.isFeatureOn(FEATURE_NAMESPACES);
	} // supportNamespaces() 

	// -------------------------------------------------------------------------

	protected boolean supportNamespacePrefixes()
	{
		return this.isFeatureOn(FEATURE_NAMESPACE_PREFIXES);
	} // supportNamespacePrefixes() 

	// -------------------------------------------------------------------------

	protected boolean isFeatureOn(String featureName)
	{
		try
		{
			return this.getFeature(featureName);
		}
		catch (Throwable e)
		{
			return false;
		}
	} // isFeatureOn() 

	// -------------------------------------------------------------------------

	protected void basicSetFeature(String name, boolean value)
	{
		this.getFeatures().put(name, value ? Boolean.TRUE : Boolean.FALSE);
	} // basicSetFeature() 

	// -------------------------------------------------------------------------

	protected void initFeatures()
	{
		this.setFeatures(new HashMap());
		this.basicSetFeature(FEATURE_NAMESPACES, true);
		this.basicSetFeature(FEATURE_NAMESPACE_PREFIXES, false);
	} // initFeatures() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// INNER CLASSES
	// =========================================================================
	class Name
	{
		final static String EMPTY = "";
		private boolean is_xmlns = false ; 
		String namespaceURI;
		String localName;
		String qName;

		Name(String uri, String local, String qName)
		{
			this.namespaceURI = uri;
			this.localName = local;
			this.qName = qName;
		} // Name() 

		// -----------------------------------------------------------------------

		Name(Node node)
		{
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				qName = ((Element)node).getTagName();
			}
			else
			{
				qName = node.getNodeName();
			}

			if (!supportNamespacePrefixes())
			{
				qName = nameWithoutPrefix(qName);
			}

			if ( node.getNodeType() == Node.ATTRIBUTE_NODE )
			{
				is_xmlns = node.getNodeName().startsWith(NAMESPACE_DECLARATION_PREFIX) ;
			}
			
			if (supportNamespaces())
			{
				if ( is_xmlns )
				{
					namespaceURI = EMPTY;
					if ( supportNamespacePrefixes() )
					{
						localName = EMPTY;
					}
					else
					{
						localName = node.getLocalName() ;
					}
				}
				else
				{
					namespaceURI = node.getNamespaceURI();
					if (namespaceURI == null)
					{
						namespaceURI = EMPTY;
					}
					localName = node.getLocalName();
					if (localName == null)
					{
						localName = nameWithoutPrefix(qName);
					}
				}
			}
			else
			{
				namespaceURI = EMPTY;
				localName = EMPTY;
			}
		} // Name() 

		// -----------------------------------------------------------------------

		/**
		 * Returns the name of this element without any prefix
		 */
		public String getWithoutPrefix() 
		{
			if ( ( localName != null ) && ( localName.length() > 0 ) )
			{
				return localName ;
			}
			return this.nameWithoutPrefix(qName) ;
		} // getWithoutPrefix() 
		
		// -------------------------------------------------------------------------
		
		/**
		 * Returns the (namespace) prefix of this name or "" if none is available
		 */
		public String getPrefix()
		{
			int index ;
			
			index = qName.indexOf(':') ;
			if ( index > 0 )
			{
				return qName.substring( 0, index ) ;
			}
			return EMPTY ;
		} // getPrefix() 
		
		// -----------------------------------------------------------------------
		
		public boolean isNamespaceAttribute() 
		{
			return is_xmlns ;
		} // isNamespaceAttribute() 
		
		// -------------------------------------------------------------------------
		
		private String nameWithoutPrefix(String name)
		{
			if ( name == null )
			{
				return null ;
			}
			int index = name.indexOf(':');
			if (index > 0)
			{
				return name.substring(index + 1);
			}
			return name;
		} // nameWithoutPrefix() 
		
		// -----------------------------------------------------------------------
		
	} // class Name

	// -------------------------------------------------------------------------

} // class DOMTreeXMLReader 
