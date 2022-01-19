// ===========================================================================
// CONTENT  : CLASS XmlStreamReader
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 17/01/2014
// HISTORY  :
//  07/03/2002  duma  CREATED
//	21/06/2002	duma	added		->	Some extra methods to read XML data
//	17/03/2003	duma	adedd		->	Javadoc comments to public methods
//	19/03/2003	duma	added		->	New methods readDOM( String, boolean )
//																readFrom( String, boolean )
//																added FileEntityResolver
//	26/03/2004	duma	changed	->	Use only FileEntityResolver
//	30/07/2004	duma	added		->	implements SAXConstants
//	22/10/2004	duma	added		->	Methods that support InputSource
//  17/01/2014  mdu   added   ->  Methods to load a whole XmlDocument
//
// Copyright (c) 2002-2014, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.six;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.pf.logging.Logger;
import org.pf.pax.SAXConstants;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * This is the most simple service class to read a XML stream
 * into a hierarchical String based structure or into a DOM tree.
 * It is completely build on JAXP, so any compliant parser package
 * can be used with it.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class XmlStreamReader implements SAXConstants
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Instances are not supported
   */
  private XmlStreamReader()
  {
    super();
  } // XmlStreamReader() 

  // =========================================================================
  // PUBLIC CLASS METHODS
  // =========================================================================

  /**
   * Read the file with the given name as XML stream and return the element 
   * structure.
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param xmlFileName The name of the file to read
   * @return The element tree representing the XML file or null in case of exceptions
   */
  public static Element readFrom(String xmlFileName)
  {
    return readFrom(xmlFileName, false);
  } // readFrom() 

  // -------------------------------------------------------------------------

  /**
   * Read the file with the given name as XML stream and return the element 
   * structure.
   * The second argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param xmlFileName The name of the file to read
   * @param validating if true the parser will validate against any defined DTD
   * @return The element tree representing the XML file or null in case of exceptions
   */
  public static Element readFrom(String xmlFileName, boolean validating)
  {
    return readFrom(new File(xmlFileName), validating);
  } // readFrom() 

  // -------------------------------------------------------------------------
  /**
   * Read the given file as XML stream and return the element structure.  
   * The second argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param file The file to read
   * @param validating if true the parser will validate against any defined DTD
   * @return The element tree representing the XML file or null in case of exceptions
   */
  public static Element readFrom(File file, boolean validating)
  {
    Element result = null;
    InputStream stream = null;
    String fName = null;

    try
    {
      fName = file.getAbsolutePath();
      stream = new FileInputStream(fName);
      result = readFrom(stream, fName, validating);
    }
    catch (IOException ex1)
    {
      logger().logException(ex1);
    }
    return result;
  } // readFrom() 

  // -------------------------------------------------------------------------

  /**
   * Read the given stream with an XML parser and return the element structure.  
   * The second argument is the URI of the stream to enable the lookup for
   * the DTD.
   * The third argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param xmlStream The XML stream to read
   * @param uri The URI of the stream
   * @param validating if true the parser will validate against any defined DTD
   * @return The element tree representing the XML file or null in case of exceptions
   */
  public static Element readFrom(InputStream xmlStream, String uri, boolean validating)
  {
    Element result = null;

    try
    {
      result = loadFrom(xmlStream, uri, validating);
    }
    catch (Throwable ex1)
    {
      if (uri != null)
      {
        logger().logError("Error parsing: " + uri, ex1);
      }
      logger().logException(ex1);
    }
    return result;
  } // readFrom() 

  // -------------------------------------------------------------------------

  /**
   * Read the file with the given filename using an XML SAX parser and return the 
   * document.  
   * The second argument specifies the encoding to be used.
   * The third argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param filename The name of the file to read the XML from (must not be null).
   * @param charset The character encoding to be used when reading the file (may be null). 
   * @param validating if true the parser will validate against any defined DTD.
   * @return The document representing the XML file or null in case of exceptions.
   */
  public static XmlDocument readXmlFrom(String filename, Charset charset, boolean validating)
  {
    File file;
    
    file = new File(filename);
    return readXmlFrom(file, charset, validating);
  } // readXmlFrom() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Read the given file with an XML parser and return the element structure.  
   * The second argument specifies the encoding to be used.
   * The third argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param file The file to read the XML from (must not be null).
   * @param charset The character encoding to be used when reading the file (may be null). 
   * @param validating if true the parser will validate against any defined DTD.
   * @return The document representing the XML file or null in case of exceptions.
   */
  public static XmlDocument readXmlFrom(File file, Charset charset, boolean validating)
  {
    XmlDocument result;
    InputStream stream;
    
    try
    {
      stream = new FileInputStream(file);
    }
    catch (Throwable ex1)
    {
      logger().logException(ex1);
      return null;
    }
    result = readXmlFrom(stream, charset, file.toURI().toString(), validating);
    return result;
  } // readXmlFrom() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Read the given stream with an XML parser and return the element structure.  
   * The second argument is the URI of the stream to enable the lookup for
   * the DTD.
   * The third argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param reader The reader to get the XML from (must not be null).
   * @param uri The URI of the stream (may be null).
   * @param validating if true the parser will validate against any defined DTD.
   * @return The document representing the XML data or null in case of exceptions.
   */
  public static XmlDocument readXmlFrom(Reader reader, String uri, boolean validating)
  {
    XmlDocument result = null;
    InputSource source;
    
    source = createInputSource(uri, reader);
    try
    {
      result = loadXmlFrom(source, validating);
    }
    catch (Throwable ex1)
    {
      logger().logException(ex1);
    }
    return result;
  } // readXmlFrom() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Read the given stream with an XML parser and return the element structure.  
   * The second argument is the URI of the stream to enable the lookup for
   * the DTD.
   * The third argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param xmlStream The stream to read the XML from (must not be null).
   * @param uri The URI of the stream (may be null).
   * @param charset The character encoding to be used when reading the XML (may be null).
   * @param validating if true the parser will validate against any defined DTD.
   * @return The document representing the XML data or null in case of exceptions.
   */
  public static XmlDocument readXmlFrom(InputStream xmlStream, Charset charset, String uri, boolean validating)
  {
    XmlDocument result = null;
    InputSource source;
    
    source = createInputSource(uri, xmlStream, charset);
    try
    {
      result = loadXmlFrom(source, validating);
    }
    catch (Throwable ex1)
    {
      logger().logException(ex1);
    }
    return result;
  } // readXmlFrom() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Read the given source with an XML SAX parser and return the XML document.  
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param source Defines where the XML can be found.
   * @param validating if true the parser will validate against any defined DTD
   * @return The document representing the XML data or null in case of exceptions.
   */
  public static XmlDocument readXmlFrom(InputSource source, boolean validating)
  {
    XmlDocument result = null;
    
    try
    {
      result = loadXmlFrom(source, validating);
    }
    catch (Throwable ex1)
    {
      if (source.getPublicId() != null)
      {
        logger().logError("Error parsing: " + source.getPublicId(), ex1);
      }
      logger().logException(ex1);
    }
    return result;
  } // readXmlFrom() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Read the file with the given name as XML stream and return the DOM 
   * tree structure.
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param xmlFileName The name of the file to read
   * @return The document tree representing the XML file or null in case of exceptions
   */
  public static Document readDOM(String xmlFileName)
  {
    return readDOM(xmlFileName, false);
  } // readDOM() 

  // -------------------------------------------------------------------------

  /**
   * Read the file with the given name as XML stream and return the DOM 
   * tree structure.
   * The second argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param xmlFileName The name of the file to read
   * @param validating if true the parser will validate against any defined DTD
   * @return The document tree representing the XML file or null in case of exceptions
   */
  public static Document readDOM(String xmlFileName, boolean validating)
  {
    return readDOM(new File(xmlFileName), validating);
  } // readDOM() 

  // -------------------------------------------------------------------------
  /**
   * Read the given file as XML stream and return the DOM tree structure.  
   * The second argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param file The file to read
   * @param validating if true the parser will validate against any defined DTD
   * @return The DOM tree representing the XML file or null in case of exceptions
   */
  public static Document readDOM(File file, boolean validating)
  {
    Document result = null;
    InputStream stream = null;
    String fName = null;

    try
    {
      fName = file.getAbsolutePath();
      stream = new FileInputStream(fName);
      result = readDOM(stream, fName, validating);
    }
    catch (IOException ex1)
    {
      logger().logException(ex1);
    }
    return result;
  } // readDOM() 

  // -------------------------------------------------------------------------

  /**
   * Read the given stream with an XML parser and return the DOM tree structure.  
   * The second argument is the URI of the stream to enable the lookup for
   * the DTD.
   * The third argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param xmlStream The XML stream to read
   * @param uri The URI of the stream
   * @param validating if true the parser will validate against any defined DTD
   * @return The DOM tree representing the XML from the given stream or null in case of exceptions
   */
  public static Document readDOM(InputStream xmlStream, String uri, boolean validating)
  {
    Document result = null;

    try
    {
      result = loadDOM(xmlStream, uri, validating);
    }
    catch (Throwable ex1)
    {
      if (uri != null)
        logger().logError("Error parsing: " + uri);
      logger().logException(ex1);
    }
    finally
    {
      try
      {
        xmlStream.close();
      }
      catch (Throwable e)
      {
      }
    }
    return result;
  } // readDOM() 

  // -------------------------------------------------------------------------

  /**
   * Read the given stream with an XML parser and return the element structure.  
   * The second argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param xmlStream The XML stream to read
   * @param validating if true the parser will validate against any defined DTD
   * @return The element tree representing the XML from the given stream or null in case of exceptions
   */
  public static Element readFrom(InputStream xmlStream, boolean validating)
  {
    return readFrom(xmlStream, null, validating);
  } // readFrom() 

  // -------------------------------------------------------------------------

  /**
   * Read the given stream with an XML parser and return the element structure.  
   * The parser will not validate the file against any DTD.
   * <p>
   * All exceptions will be caught and logged using the current component logger
   * (see {@link LoggerProvider}).
   * 
   * @param xmlStream The XML stream to read
   * @return The element tree representing the XML from the given stream or null in case of exceptions
   */
  public static Element readFrom(InputStream xmlStream)
  {
    return readFrom(xmlStream, null, false);
  } // readFrom() 

  // -------------------------------------------------------------------------

  /**
   * Read the given stream with an XML parser and return the element structure.  
   * The parser will not validate the file against any DTD.
   * 
   * @param xmlStream The XML stream to read
   * @return The element tree representing the XML from the given stream
   * @throws IOException If anything is wrong with reading the stream
   * @throws ParserConfigurationException See javax.xml.parsers for details
   * @throws SAXException See org.xml.sax for details
   */
  public static Element loadFrom(InputStream xmlStream) throws IOException, SAXException, ParserConfigurationException
  {
    return loadFrom(xmlStream, false);
  } // loadFrom() 

  // -------------------------------------------------------------------------

  /**
   * Read the given stream with an XML parser and return the element structure.  
   * The second argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * 
   * @param xmlStream The XML stream to read
   * @param validating if true the parser will validate against any defined DTD
   * @return The element tree representing the XML from the given stream
   * @throws IOException If anything is wrong with reading the stream
   * @throws ParserConfigurationException See javax.xml.parsers for details
   * @throws SAXException See org.xml.sax for details
   */
  public static Element loadFrom(InputStream xmlStream, boolean validating) throws IOException, SAXException, ParserConfigurationException
  {
    return loadFrom(xmlStream, null, validating);
  } // loadFrom() 

  // -------------------------------------------------------------------------

  /**
   * Read the given stream with an XML parser and return the element structure.  
   * The second argument is the URI of the stream to enable the lookup for
   * the DTD.
   * The third argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * 
   * @param xmlStream The XML stream to read
   * @param uri The URI of the stream
   * @param validating if true the parser will validate against any defined DTD
   * @return The element tree representing the XML from the given stream 
   * @throws IOException If anything is wrong with reading the stream
   * @throws ParserConfigurationException See javax.xml.parsers for details
   * @throws SAXException See org.xml.sax for details
   */
  public static Element loadFrom(InputStream xmlStream, String uri, boolean validating) throws IOException, SAXException, ParserConfigurationException
  {
    InputSource source;

    source = createInputSource(uri, xmlStream, null);
    try
    {
      return loadFrom(source, validating);
    }
    finally
    {
      xmlStream.close();
    }
  } // loadFrom() 

  // -------------------------------------------------------------------------

  /**
   * Read the given input source with an XML parser and return the element 
   * structure.  
   * The second argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * 
   * @param source The input source that contains the XML stream to read
   * @param validating if true the parser will validate against any defined DTD
   * @return The element tree representing the XML from the given stream 
   * @throws IOException If anything is wrong with reading the stream
   * @throws ParserConfigurationException See javax.xml.parsers for details
   * @throws SAXException See org.xml.sax for details
   */
  public static Element loadFrom(InputSource source, boolean validating) throws IOException, SAXException, ParserConfigurationException
  {
    XmlDocument xmlDocument;
    
    xmlDocument = loadXmlFrom(source, validating);
    return xmlDocument.getRootElement();
  } // loadFrom() 

  // -------------------------------------------------------------------------

  /**
   * Read the given input source with an XML parser and return the element 
   * structure.  
   * The second argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * 
   * @param source The input source that contains the XML stream to read
   * @param validating if true the parser will validate against any defined DTD
   * @return The element tree representing the XML from the given stream 
   * @throws IOException If anything is wrong with reading the stream
   * @throws ParserConfigurationException See javax.xml.parsers for details
   * @throws SAXException See org.xml.sax for details
   */
  public static XmlDocument loadXmlFrom(InputSource source, boolean validating) throws IOException, SAXException, ParserConfigurationException
  {
    StringTagInterpreterController controller = null;
    XMLReader xmlReader = null;
    XmlDocument xmlDocument;

    xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
    xmlReader.setFeature(FEATURE_VALIDATING, validating);
    xmlReader.setEntityResolver(new FileEntityResolver(validating));
    controller = new StringTagInterpreterController(xmlReader);
    xmlReader.parse(source);
    xmlDocument = controller.getXmlDocument();
    xmlDocument.setEncoding(source.getEncoding());
    return xmlDocument; 
  } // loadXmlFrom() 

  // -------------------------------------------------------------------------

  /**
   * Read the given stream with an XML parser and return the DOM tree structure.  
   * The second argument is the URI of the stream to enable the lookup for
   * the DTD.
   * The third argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * 
   * @param xmlStream The XML stream to read
   * @param uri The URI of the stream
   * @param validating if true the parser will validate against any defined DTD
   * @return The DOM tree representing the XML file
   * @throws IOException If anything is wrong with reading the stream
   * @throws ParserConfigurationException See javax.xml.parsers for details
   * @throws SAXException See org.xml.sax for details
   */
  public static Document loadDOM(InputStream xmlStream, String uri, boolean validating) throws IOException, SAXException, ParserConfigurationException
  {
    InputSource source = null;

    source = new InputSource(xmlStream);
    if (uri != null)
    {
      uri = uri.replace('\\', '/');
      if (uri.indexOf("://") < 0)
      {
        uri = "file://" + uri;
      }
      source.setSystemId(uri);
    }
    return loadDOM(source, validating);
  } // loadDOM() 

  // -------------------------------------------------------------------------

  /**
   * Read the given source with an XML parser and return the DOM tree structure.  
   * The second argument is the URI of the stream to enable the lookup for
   * the DTD.
   * The third argument specifies whether or not the parser should
   * validate the file against its DTD (if defined).
   * 
   * @param source The input source that contains the XML stream to read
   * @param validating if true the parser will validate against any defined DTD
   * @return The DOM tree representing the XML file
   * @throws IOException If anything is wrong with reading the stream
   * @throws ParserConfigurationException See javax.xml.parsers for details
   * @throws SAXException See org.xml.sax for details
   */
  public static Document loadDOM(InputSource source, boolean validating) throws IOException, SAXException, ParserConfigurationException
  {
    Document document;
    DocumentBuilderFactory factory;
    DocumentBuilder docBuilder = null;

    factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(validating);
    docBuilder = factory.newDocumentBuilder();
    docBuilder.setEntityResolver(new FileEntityResolver(validating));
    document = docBuilder.parse(source);
    return document;
  } // loadDOM() 

  // -------------------------------------------------------------------------

  // =========================================================================
  // PROTECTED CLASS METHODS
  // =========================================================================

  /**
   * Returns the current logger used by this component to report
   * errors and exceptions. 
   */
  protected static Logger logger()
  {
    return LoggerProvider.getLogger();
  } // logger() 

  // -------------------------------------------------------------------------
  
  protected static InputSource createInputSource(String uri, InputStream stream, Charset charset) 
  {
    InputSource source;

    source = new InputSource(stream);
    initInputSource(source, uri, charset);
    return source;
  } // createInputSource() 
  
  // -------------------------------------------------------------------------  

  protected static InputSource createInputSource(String uri, Reader reader) 
  {
    InputSource source;
    
    source = new InputSource(reader);
    initInputSource(source, uri, null);
    return source;
  } // createInputSource() 
  
  // -------------------------------------------------------------------------  
  
  protected static void initInputSource(InputSource source, String uri, Charset charset) 
  {
    if (uri != null)
    {
      uri = uri.replace('\\', '/');
      source.setSystemId(uri);
    }
    if (charset != null)
    {
      source.setEncoding(charset.name());
    }
  } // initInputSource() 
  
  // -------------------------------------------------------------------------  
  
} // class XmlStreamReader 
