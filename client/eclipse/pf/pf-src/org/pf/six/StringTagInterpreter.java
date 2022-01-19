// ===========================================================================
// CONTENT  : CLASS StringTagInterpreter
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.3 - 17/01/2014
// HISTORY  :
//  28/02/2002  duma  CREATED
//	17/03/2003	duma	changed	->	Reduced visibility from public to default
//	04/08/2009	mdu		changed	->	Leave content of CDATA[] sections unchanged
//  17/01/2014  mdu   changed ->  Support processing instructions
//
// Copyright (c) 2002-2014, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.six;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pf.pax.BaseXMLTagInterpreter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This interpreter is used to read in XML data and build a simple
 * String base element hierarchy.
 *
 * @author Manfred Duchrow
 * @version 1.3
 */
class StringTagInterpreter extends BaseXMLTagInterpreter
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String tagName = null ;
  protected void setTagName( String newValue ) { tagName = newValue ; }
  
  private Element element = null ;
  protected Element getElement() { return element ; }
  protected void setElement( Element newValue ) { element = newValue ; }
  
  private List<XmlItem> children = null ;
  protected List<XmlItem> getChildren() { return children ; }
  protected void setChildren( List<XmlItem> newValue ) { children = newValue ; }
    
  private boolean isTextCDATA = false ;
  protected boolean isTextCDATA() { return isTextCDATA ; }
  protected void setIsTextCDATA( boolean newValue ) { isTextCDATA = newValue ; }
  
  private List bufferList = new ArrayList() ;
  protected List getBufferList() { return bufferList ; }
//  protected void setBufferList( List newValue ) { bufferList = newValue ; }
  
  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with the given tag name.
   */
  public StringTagInterpreter(String aTagName)
  {
    super();
    this.setTagName(aTagName);
  } // StringTagInterpreter() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns the name of the tag, the interpreter is responsible for. <br>
   *
   * @return A string containing the name of the tag the receiver is made for.
   */
  public String getTagName()
  {
    return tagName;
  } // getTagName() 

  // -------------------------------------------------------------------------

  public void start(String elementName, Attributes attributes) throws SAXException
  {
    this.setElement(new Element());
    this.getElement().setName(elementName);
    for (int i = 0; i < attributes.getLength(); i++)
    {
      this.getElement().setAttribute(attributes.getQName(i), attributes.getValue(i));
    }
  } // start() 

  // -------------------------------------------------------------------------

  public void restart(String subTagName, Object subResult) throws SAXException
  {
    this.addChild((Element)subResult);
  } // restart() 

  // -------------------------------------------------------------------------

  public Object getResult()
  {
    return this.getElement();
  } // getResult() 

  // -------------------------------------------------------------------------

  @Override
  public void characterData(String data)
  {
    TaggedBuffer currentBuffer;

    currentBuffer = this.getLastTextBuffer();
    if ((currentBuffer != null) && (currentBuffer.isTextCDATA() != this.isTextCDATA()))
    {
      currentBuffer = null;
    }
    if (currentBuffer == null)
    {
      currentBuffer = new TaggedBuffer(this.isTextCDATA(), data.length());
      this.getBufferList().add(currentBuffer);
    }
    currentBuffer.append(data);
  } // characterData() 

  // -------------------------------------------------------------------------

  @Override
  public void startCDATA() throws SAXException
  {
    this.setIsTextCDATA(true);
  } // startCDATA() 

  // -------------------------------------------------------------------------

  @Override
  public void endCDATA() throws SAXException
  {
    this.setIsTextCDATA(false);
  } // endCDATA() 

  // -------------------------------------------------------------------------
  
  @Override
  public void processingInstruction(String target, String data) throws SAXException
  {
    this.addChild(new XmlProcessingInstruction(target, data));
  } // processingInstruction()
  
  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

  protected void addChild(XmlItem child)
  {
    if (this.getChildren() == null)
    {
      this.setChildren(new ArrayList<XmlItem>());
    }

    this.getChildren().add(child);
  } // addChild() 

  // -------------------------------------------------------------------------

  /**
   * This method is called just before control is passed back to the controller.   <br>
   * It gives the interpreter a chance to do any necessary completion of
   * its result object.
   */
  @Override
  protected void finalizeResult()
  {
    if (this.getChildren() != null)
    {
      this.getElement().setChildItems(this.getChildren());
    }
    this.getElement().setText(this.finishElementText());
  } // finalizeResult() 

  // -------------------------------------------------------------------------

  protected String finishElementText()
  {
    Iterator iter;
    TaggedBuffer part;
    StringBuffer buffer;

    if (this.getBufferList().isEmpty())
    {
      return null;
    }
    buffer = new StringBuffer(this.calcTextLength());
    iter = this.getBufferList().iterator();
    while (iter.hasNext())
    {
      part = (TaggedBuffer)iter.next();
      if (part.isTextCDATA())
      {
        buffer.append(part.toString());
      }
      else
      {
        buffer.append(this.removeWhitespaces(part.toString()));
      }
    }
    if (buffer.length() == 0)
    {
      return null;
    }
    return buffer.toString();
  } // finishElementText() 

  // -------------------------------------------------------------------------

  /**
   * This method removes all whitespace characters from the given text 
   * and replaces them by just a single blank, except at the beginning and
   * the very end of the text.
   */
  protected String removeWhitespaces(String text)
  {
    char[] chars;
    StringBuffer buffer;
    char ch;
    boolean addBlank = false;

    chars = text.toCharArray();
    buffer = new StringBuffer(chars.length);
    int i = 0;
    while (i < chars.length)
    {
      ch = chars[i];
      if ((ch == ' ') || (ch == '\n') || (ch == '\r') || (ch == '\t'))
      {
        addBlank = true;
      }
      else
      {
        if (addBlank && (buffer.length() > 0))
        {
          buffer.append(' ');
          addBlank = false;
        }
        buffer.append(ch);
        addBlank = false;
      }
      i++;
    }
    return buffer.toString();
  } // removeWhitespaces() 

  // -------------------------------------------------------------------------

  protected TaggedBuffer getLastTextBuffer()
  {
    if (this.getBufferList().isEmpty())
    {
      return null;
    }
    return (TaggedBuffer)this.getBufferList().get(this.getBufferList().size() - 1);
  } // getLastTextBuffer() 

  // -------------------------------------------------------------------------

  protected int calcTextLength()
  {
    Iterator iter;
    TaggedBuffer part;
    int length = 0;

    if (this.getBufferList().isEmpty())
    {
      return 0;
    }
    iter = this.getBufferList().iterator();
    while (iter.hasNext())
    {
      part = (TaggedBuffer)iter.next();
      length += part.length();
    }
    return length;
  } // calcTextLength() 

  // -------------------------------------------------------------------------

} // class StringTagInterpreter 
