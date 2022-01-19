// ===========================================================================
// CONTENT  : CLASS XPathAccess
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 13/01/2012
// HISTORY  :
//  13/01/2012  mdu  CREATED
//
// Copyright (c) 2012, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.pax;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simplified access via XPath to a Document.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class XPathAccess
{
	// =========================================================================
	// CONSTANTS
	// =========================================================================
	private static final XmlUtil XU = XmlUtil.current();

	// =========================================================================
	// INSTANCE VARIABLES
	// =========================================================================
	private Element element = null;
	public Element getElement()	{	return element;	}
	public void setElement(Element newValue)	{	element = newValue;	}

	// =========================================================================
	// CONSTRUCTORS
	// =========================================================================
	/**
	 * Initialize the new instance with the element to work on.
	 */
	public XPathAccess(final Element element)
	{
		super();
		if (element == null)
		{
			throw new IllegalArgumentException("Parameter 'element' must not be null.");
		}
		this.setElement(element);
	} // XPathAccess() 

	// -------------------------------------------------------------------------

	/**
	 * Initialize the new instance with the given document's root element.
	 */
	public XPathAccess(final Document document)
	{
		this(document.getDocumentElement());
	} // XPathAccess() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// PUBLIC INSTANCE METHODS
	// =========================================================================
	/**
	 * Evaluates the given XPath expression against the underlying element and returns
	 * the text of the found element or attribute.
	 * That implies that the given XPath must locate either an Element or an Attribute
	 * but no other kind of Node.
	 * <p>
	 * The method automatically creates the necessary NamespaceContext from the underlying 
	 * element in order to support prefixed names in the XPath.
	 *
	 * @param xpathStr The XPath expression to evaluate
	 * @return The text of the found Element or Attribute node or null if not found
	 * @throws XPathAccessException If the given expression cannot be evaluated
	 */
	public String getNodeText(final String xpathStr)
	{
		Node node;

		node = this.getNode(xpathStr);
		if (node == null)
		{
			return null;
		}
		if ((node.getNodeType() == Node.ELEMENT_NODE))
		{
			return ((Element)node).getTextContent();
		}
		if ((node.getNodeType() == Node.ATTRIBUTE_NODE))
		{
			return ((Attr)node).getTextContent();
		}
		return null;
	} // getNodeText() 

	// -------------------------------------------------------------------------

	/**
	 * Evaluates the given XPath expression against the underlying element and returns
	 * the found element.
	 * That implies that the given XPath must locate an Element and no other kind of Node.
	 * <p>
	 * The method automatically creates the necessary NamespaceContext from the underlying 
	 * element in order to support prefixed names in the XPath.
	 *
	 * @param xpathStr The XPath expression to evaluate
	 * @return The found Element node or null if not found or the found node is no Element
	 * @throws XPathAccessException If the given expression cannot be evaluated
	 */
	public Element getElement(final String xpathStr)
	{
		Node node;
		
		node = this.getNode(xpathStr);
		if (node == null)
		{
			return null;
		}
		if ((node.getNodeType() == Node.ELEMENT_NODE))
		{
			return (Element)node;
		}
		return null;
	} // getElement() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Evaluates the given XPath expression against the underlying element and returns
	 * the found elements.
	 * That implies that the given XPath must locate one or more Elements and no other kind of Node.
	 * <p>
	 * The method automatically creates the necessary NamespaceContext from the underlying 
	 * element in order to support prefixed names in the XPath.
	 *
	 * @param xpathStr The XPath expression to evaluate
	 * @return The list of found element nodes (might be empty)
	 * @throws XPathAccessException If the given expression cannot be evaluated
	 */
	public List<Element> getElements(final String xpathStr)
	{
		List<Node> nodes;
		List<Element> elements;
		
		nodes = this.getNodes(xpathStr);
		elements = new ArrayList<Element>(nodes.size());
		for (Node node : nodes)
		{
			if (node instanceof Element)
			{
				elements.add((Element)node);
			}
		}
		return elements;
	} // getElements() 
	
	// -------------------------------------------------------------------------
	
	/**
	 * Evaluates the given XPath expression against the underlying element and returns
	 * the found node or null.
	 * That implies that the given XPath must locate a single Node.
	 * <p>
	 * The method automatically creates the necessary NamespaceContext from the underlying 
	 * element in order to support prefixed names in the XPath.
	 *
	 * @param xpathStr The XPath expression to evaluate
	 * @return The text of the found Element or Attribute node or null if not found
	 * @throws XPathAccessException If the given expression cannot be evaluated
	 */
	public Node getNode(final String xpathStr) 
	{
		return (Node)this.evaluate(xpathStr, XPathConstants.NODE);
	} // getNode() 

	// -------------------------------------------------------------------------

	/**
	 * Evaluates the given XPath expression against the underlying element and returns
	 * the found nodes as list.
	 * <p>
	 * The method automatically creates the necessary NamespaceContext from the underlying 
	 * element in order to support prefixed names in the XPath.
	 *
	 * @param xpathStr The XPath expression to evaluate
	 * @return The list of found nodes (might be an empty list)
	 * @throws XPathAccessException If the given expression cannot be evaluated
	 */
	public List<Node> getNodes(final String xpathStr) 
	{
		NodeList nodeList;
		List<Node> result;
		
		nodeList = (NodeList)this.evaluate(xpathStr, XPathConstants.NODESET);
		result = XU.nodeListToListOfNodes(nodeList);
		return result;
	} // getNodes() 
	
	// -------------------------------------------------------------------------
	
	// =========================================================================
	// PROTECTED INSTANCE METHODS
	// =========================================================================
	protected Object evaluate(final String xpathStr, QName returnType) 
	{
		XPathFactory xPathFactory;
		XPath xpath;
		XPathExpression expression;
		
		if (xpathStr == null)
		{
			return null;
		}
		xPathFactory = XPathFactory.newInstance();
		xpath = xPathFactory.newXPath();
		xpath.setNamespaceContext(XU.extractNamespacesFrom(this.getElement()));
		try
		{
			expression = xpath.compile(xpathStr);
			return expression.evaluate(this.getElement(), returnType);
		}
		catch (XPathExpressionException ex)
		{
			throw new XPathAccessException("Error in accessing XML data via XPath expression: " + xpathStr, ex);
		}
	} // evaluate() 
	
	// -------------------------------------------------------------------------
	

} // class XPathAccess 
