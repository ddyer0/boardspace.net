// ===========================================================================
// CONTENT  : CLASS XmlDocument
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 17/01/2014
// HISTORY  :
//  17/01/2014  mdu  CREATED
//
// Copyright (c) 2014, by MDCS. All rights reserved.
// ===========================================================================
package org.pf.six;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the whole XML document and contains 0..n children which can
 * be of type Element or XmlProcessingInstruction.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class XmlDocument
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String encoding;
  private final List<XmlProcessingInstruction> processingInstructions = new ArrayList<XmlProcessingInstruction>();
  private Element rootElement;

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  public XmlDocument()
  {
    super();
  } // XmlDocument() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  public String getEncoding()
  {
    return encoding;
  } // getEncoding() 
  
  // -------------------------------------------------------------------------
  
  public void setEncoding(String encoding)
  {
    this.encoding = encoding;
  } // setEncoding() 
  
  // -------------------------------------------------------------------------
  
  public Element getRootElement()
  {
    return rootElement;
  } // getRootElement() 
  
  // -------------------------------------------------------------------------
  
  public void setRootElement(Element rootElement)
  {
    this.rootElement = rootElement;
  } // setRootElement() 
  
  // -------------------------------------------------------------------------
  
  public XmlProcessingInstruction[] getProcessingInstructions()
  {
    return XmlProcessingInstruction.toArray(this.processingInstructions);
  } // getProcessingInstructions() 
  
  // -------------------------------------------------------------------------
  
  public void addProcessingInstruction(XmlProcessingInstruction instruction) 
  {
    if (instruction != null)
    {
      this.processingInstructions.add(instruction);
    }
  } // addProcessingInstruction() 
  
  // -------------------------------------------------------------------------

  public void clearProcessingInstruction() 
  {
    this.processingInstructions.clear();    
  } // clearProcessingInstruction() 
  
  // -------------------------------------------------------------------------
  
  public boolean hasProcessingInstructions() 
  {
    return !this.processingInstructions.isEmpty();    
  } // hasProcessingInstructions() 
  
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
    if (this.getRootElement() == null)
    {
      return Element.EMPTY_ARRAY;
    }
    return this.getRootElement().find(tagName, attributes);
  } // find() 
  
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
    if (this.getRootElement() == null)
    {
      return Element.EMPTY_ARRAY;
    }
    return this.getRootElement().find(filter);
  } // find() 
  
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
    if (this.getRootElement() == null)
    {
      return null;
    }
    return this.getRootElement().findFirst(filter);
  } // findFirst() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

} // class XmlDocument 
