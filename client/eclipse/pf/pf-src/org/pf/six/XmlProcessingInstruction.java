// ===========================================================================
// CONTENT  : CLASS XmlProcessingInstruction
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
import java.util.List;

import org.pf.util.CollectionUtil;

/**
 * Represents a single processing instruction in a XML document.
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class XmlProcessingInstruction implements XmlItem
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String name;
  private String data;

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  public static XmlProcessingInstruction[] toArray(List<XmlProcessingInstruction> instructions) 
  {
    return CollectionUtil.current().toArray(instructions, XmlProcessingInstruction.class);
  } // toArray() 
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  public XmlProcessingInstruction(String name, String data)
  {
    super();
    this.setName(name);
    this.setData(data);
  } // XmlProcessingInstruction() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  public String getName()
  {
    return this.name;
  } // getName() 
  
  // -------------------------------------------------------------------------
  
  public String getData()
  {
    return data;
  } // getData() 
  
  // -------------------------------------------------------------------------
  
  public Type getItemType()
  {
    return Type.PI;
  } // getItemType() 
  
  // -------------------------------------------------------------------------

  /**
   * Returns the instruction name enclosed in <? ?>.
   * Example: "<?xsl-stylesheet?>"
   */
  public String toString()
  {
    return "<?" + this.getName() + "?>";
  } // toString() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected void setName(String name)
  {
    this.name = name;
  } // setName() 
  
  // -------------------------------------------------------------------------
  
  protected void setData(String data)
  {
    this.data = data;
  } // setData() 
  
  // -------------------------------------------------------------------------

} // class XmlProcessingInstruction 
