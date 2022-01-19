/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package twixt.net.schwagereit.t1j;

/**
 * Store any move done.
 * This implementation leaves room for enhancements, e.g. link removal
 *
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */

public final class Move
{
   private static final int SHIFT = 5;

   private int x;

   private int y;

   /**
    * Cons'tor.
    * @param xin x
    * @param yin y
    */
   Move(final int xin, final int yin)
   {
      x = xin;
      y = yin;
   }

   /**
    * Cons'tor.
    * @param xin x
    * @param yin y
    * @param player X- or Y-player
    */
   Move(final int xin, final int yin, final int player)
   {
      if (player == Board.YPLAYER)
      {
         x = xin;
         y = yin;
      }
      else
      {
         //swap for xplayer
         y = xin;
         x = yin;
      }
   }

   /**
    * Check if two Moves are the same.
    * @param obj object to compare
    * @return true id equal
    */
   public boolean equals(final Object obj)
   {
      if (obj instanceof Move)
      {
         Move mv = (Move) obj;
         return x == mv.x && y == mv.y;
      }
      return super.equals(obj);
   }
   
   /**
    * Compute hashcode.
    * @return computed hashcode
    */
   public int hashCode()
   {
      return (x << SHIFT) + y;
   }

   /**
    * Return x.
    * @return Returns the x.
    */
   public int getX()
   {
      return x;
   }

   /**
    * Return y.
    * @return Returns the y.
    */
   public int getY()
   {
      return y;
   }

   /**
    * Set x.
    * @param xIn The x to set.
    */
   public void setX(final int xIn)
   {
      this.x = xIn;
   }

   /**
    * Set y.
    * @param yIn The y to set.
    */
   public void setY(final int yIn)
   {
      this.y = yIn;
   }

   public String toString()
   {
      return GuiBoard.getHoleName(x, y, false);
   }
}