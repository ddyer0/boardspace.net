package org.pf.joi.test ;

import java.awt.Color;
import org.pf.joi.ObjectRenderer;
import org.pf.text.StringUtil;

public class ColorRenderer implements ObjectRenderer
{
  public String inspectString( Object obj )
  {
    Color color ;
    StringBuffer buffer ;

    if ( obj instanceof Color )
    {
      color = (Color)obj ;
      buffer = new StringBuffer(20) ;
      buffer.append( "#" ) ;
      buffer.append( this.toHex( color.getRed() ) ) ;
      buffer.append( this.toHex( color.getGreen() ) ) ;
      buffer.append( this.toHex( color.getBlue() ) ) ;
      return buffer.toString() ;
    }
    return "" + obj ;
  } // inspectString()

  // -------------------------------------------------------------------------

  protected String toHex( int value )
  {
    String hex ;

    hex = StringUtil.current().leftPadCh( Integer.toHexString(value), 2, '0' ) ;
    return hex.toUpperCase() ;
  } // toHex()

  // -------------------------------------------------------------------------

} // class ColorRenderer
