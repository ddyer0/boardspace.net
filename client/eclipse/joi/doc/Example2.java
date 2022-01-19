package org.pf.joi.example;
import org.pf.joi.Inspector;

public class Example2
{
  public static void main( String[] args )
  {
    Inspector.inspectWait( System.getProperties() ) ;
    System.exit(0);
  }
}
