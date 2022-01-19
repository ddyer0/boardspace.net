/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package twixt.net.schwagereit.t1j;

import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * Read Keys from property-file.
 *
 * Created by IntelliJ IDEA.
 * @author mail@johannes-schwagereit.de
 */
public final class Messages
{
   private static final String BUNDLE_NAME = "twixt/net/schwagereit/t1j/messages"; //$NON-NLS-1$

   private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
         .getBundle(BUNDLE_NAME);

   /**
    * Private cons'tor.
    */
   private Messages()
   {
   }

   /**
    * Get Property from property-file.
    * @param key key to look for
    * @return Property found
    */
   public static String getString(final String key)
   {
      try
      {
         return RESOURCE_BUNDLE.getString(key);
      } catch (MissingResourceException e)
      {
         System.out.println(key);
         return "!!" + key + "!!";
      }
   }
}
