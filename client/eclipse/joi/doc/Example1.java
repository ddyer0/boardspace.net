package org.pf.joi.example;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.UIManager;

import org.pf.joi.Inspector;

public class Example1 extends WindowAdapter
{
  public static void main( String[] args )
  {
    try
    {
      UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() ) ;
    }
    catch ( Exception ex ) {}
    Example1 example = new Example1() ;
    example.run() ;
  }

  public void run()
  {
    Frame frame = new Frame() ;
    frame.addWindowListener( this ) ;
    frame.setSize( new Dimension( 100, 100 ) ) ;
    frame.add( new TextArea() ) ;
    frame.validate() ;
    frame.setVisible( true ) ;
    Inspector.inspect( frame ) ; // <===== HERE IS THE INSPECTION
  }

  public void windowClosing(WindowEvent e)
  {
    super.windowClosing(e) ;
    System.exit( 0 ) ;
  }
}
