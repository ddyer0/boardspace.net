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
 * Check for races. The 8 relvant diagonal lines are numbered clockwise 0 to 7,
 * starting with the steep one at B2. This class is a singleton.
 *
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
public final class Races
{

   /** The singleton-instance. */
   private static final Races RACES = new Races();

   /** The pin to test. */
   private int pinX, pinY;

   /** The board. */
   private Board board;

   /** The next player to set pin. */
   private int nextPlayer;

   /**
    * Cons'tor - no external instance.
    */
   private Races()
   {
   }

   /**
    * Return the Races-Object. (Singleton)
    *
    * @return Races-Object
    */
   public static Races getRaces()
   {
      return RACES;
   }

   /**
    * Check for race 0 (upper-left to right, steep).
    * @return true if no blocking race exists
    */
   private boolean checkFor0()
   {
      boolean ret = true;
      int vx = (pinX - 1) * 2;
      int vy = (pinY - 1);

//      System.out.println("0 fuer " + GuiBoard.getHoleName(pinX, pinY, false) + ":"
//            + ((vx >= vy) ? " save" : " not save"));

      if (board.getPin(pinX + 1, pinY + 1) == Board.XPLAYER && vx < vy
            && (board.isConnected(pinX + 1, pinY + 1, pinX, pinY - 1)
                  || (board.isConnected(pinX + 1, pinY + 1, pinX + 2, pinY + 3)
                      && board.bridgeAllowed(pinX + 1, pinY + 1, 1)
                      && nextPlayer == Board.XPLAYER )))
      {
         ret = false;
      }

//      System.out.println("Check: " + (ret ? "okay" : "not okay") +
//            " NextPlayer: " + (nextPlayer == Board.XPLAYER ? "X" : "Y"));
      return ret;
   }

   /**
    * Check for race 1 (upper-left to right, gentle).
    * @return true if no blocking race exists
    */
   private boolean checkFor1()
   {
      boolean ret = true;
      int vx = (pinX - 1);
      int vy = (pinY - 1) * 2;

//      System.out.println("1 fuer " + GuiBoard.getHoleName(pinX, pinY, false) + ":"
//            + ((vx >= vy) ? " save" : " not save"));

      if (board.getPin(pinX + 1, pinY) == Board.XPLAYER && vx < vy
            && (board.isConnected(pinX + 1, pinY, pinX - 1, pinY - 1)
            || (board.isConnected(pinX + 1, pinY, pinX + 3, pinY + 1)
            && nextPlayer == Board.XPLAYER)))
      {
         ret = false;
      }

//      System.out.println("Check: " + (ret ? "okay" : "not okay") +
//            " NextPlayer: " + (nextPlayer == Board.XPLAYER ? "X" : "Y"));
      return ret;
   }

   /**
    * Check for race 2 (upper-right to left, gentle).
    * @return true if no blocking race exists
    */
   private boolean checkFor2()
   {
      boolean ret = true;
      int vx = (board.getXsize() - 2 - pinX);
      int vy = (pinY - 1) * 2;

//      System.out.println("2 fuer " + GuiBoard.getHoleName(pinX, pinY, false) + ":"
//            + ((vx >= vy) ? " save" : " not save"));

      if (board.getPin(pinX - 1, pinY) == Board.XPLAYER && vx < vy
            && (board.isConnected(pinX - 1, pinY, pinX + 1, pinY - 1)
            || (board.isConnected(pinX - 1, pinY, pinX - 3, pinY + 1)
            && nextPlayer == Board.XPLAYER)))
      {
         ret = false;
      }

//      System.out.println("Check: " + (ret ? "okay" : "not okay") +
//            " NextPlayer: " + (nextPlayer == Board.XPLAYER ? "X" : "Y"));
      return ret;
   }

   /**
    * Check for race 3 (upper-right to left, steep).
    * @return true if no blocking race exists
    */
   private boolean checkFor3()
   {
      boolean ret = true;
      int vx = (board.getXsize() - 2 - pinX) * 2;
      int vy = (pinY - 1);

//      System.out.println("3 fuer " + GuiBoard.getHoleName(pinX, pinY, false) + ":"
//            + ((vx >= vy) ? " save " : " not save ") + vx + "," + vy + "-" + (board.getYsize() - 2 - pinY));

      if (board.getPin(pinX - 1, pinY + 1) == Board.XPLAYER && vx < vy
            && (board.isConnected(pinX - 1, pinY + 1, pinX, pinY - 1)
                  || (board.isConnected(pinX - 1, pinY + 1, pinX - 2, pinY + 3)
                      && board.bridgeAllowed(pinX - 1, pinY + 1, 2)
                      && nextPlayer == Board.XPLAYER)))
      {
         ret = false;
      }

//      System.out.println("Check: " + (ret ? "okay" : "not okay") +
//            " NextPlayer: " + (nextPlayer == Board.XPLAYER ? "X" : "Y"));
      return ret;
   }

   /**
    * Check for race 4 (down-right to left, steep).
    * @return true if no blocking race exists
    */
   private boolean checkFor4()
   {
      boolean ret = true;
      int vx = (board.getXsize() - 2 - pinX) * 2;
      int vy = (board.getYsize() - 2 - pinY);

//      System.out.println("4 fuer " + GuiBoard.getHoleName(pinX, pinY, false) + ":"
//            + ((vx >= vy) ? " save " : " not save ") + vx + "," + vy + "-" + (board.getYsize() - 2 - pinY));

      if (board.getPin(pinX - 1, pinY - 1) == Board.XPLAYER && vx < vy
            && (board.isConnected(pinX - 1, pinY - 1, pinX, pinY + 1)
                  || (board.isConnected(pinX - 1, pinY - 1, pinX - 2, pinY - 3)
                      && board.bridgeAllowed(pinX, pinY + 1, 1)
                      && nextPlayer == Board.XPLAYER)))
      {
         ret = false;
      }

//      System.out.println("Check: " + (ret ? "okay" : "not okay") +
//            " NextPlayer: " + (nextPlayer == Board.XPLAYER ? "X" : "Y"));
      return ret;
   }


   /**
    * Check for race 5 (down-right to left, gentle).
    * @return true if no blocking race exists
    */
   private boolean checkFor5()
   {
      boolean ret = true;
      int vx = (board.getXsize() - 2 - pinX);
      int vy = (board.getYsize() - 2 - pinY) * 2;

//      System.out.println("5 fuer " + GuiBoard.getHoleName(pinX, pinY, false) + ":"
//            + ((vx >= vy) ? " save" : " not save"));

      if (board.getPin(pinX - 1, pinY) == Board.XPLAYER && vx < vy
            && (board.isConnected(pinX - 1, pinY, pinX + 1, pinY + 1)
                || (board.isConnected(pinX - 1, pinY, pinX - 3, pinY - 1)
                    && nextPlayer == Board.XPLAYER)))
      {
         ret = false;
      }

//      System.out.println("Check: " + (ret ? "okay" : "not okay") +
//            " NextPlayer: " + (nextPlayer == Board.XPLAYER ? "X" : "Y"));
      return ret;
   }

   /**
    * Check for race 6 (down-left to right, gentle).
    * @return true if no blocking race exists
    */
   private boolean checkFor6()
   {
      boolean ret = true;
      int vx = (pinX - 1);
      int vy = (board.getYsize() - 2 - pinY) * 2; // check for 6

//      System.out.println("6 fuer " + GuiBoard.getHoleName(pinX, pinY, false) + ":"
//            + ((vx >= vy) ? " save" : " not save"));

      if (board.getPin(pinX + 1, pinY) == Board.XPLAYER && vx < vy
            && (board.isConnected(pinX + 1, pinY, pinX - 1, pinY + 1)
                || (board.isConnected(pinX + 1, pinY, pinX + 3, pinY - 1)
                        && nextPlayer == Board.XPLAYER)))
      {
         ret = false;
      }

//      System.out.println("Check: " + (ret ? "okay" : "not okay") +
//            " NextPlayer: " + (nextPlayer == Board.XPLAYER ? "X" : "Y"));
      return ret;
   }

   /**
    * Check for race 7 (down-left to right, steep).
    * @return true if no blocking race exists
    */
   private boolean checkFor7()
   {
      boolean ret = true;
      int vx = (pinX - 1) * 2;
      int vy = (board.getYsize() - 2 - pinY);

//      System.out.println("7 fuer " + GuiBoard.getHoleName(pinX, pinY, false) + ":"
//            + ((vx >= vy) ? " save" : " not save"));

      if (board.getPin(pinX + 1, pinY - 1) == Board.XPLAYER && vx < vy
            && (board.isConnected(pinX + 1, pinY - 1, pinX, pinY + 1)
                  || (board.isConnected(pinX + 1, pinY - 1, pinX + 2, pinY - 3)
                      && board.bridgeAllowed(pinX, pinY + 1, 2)
                      && nextPlayer == Board.XPLAYER )))
      {
         ret = false;
      }

//      System.out.println("Check: " + (ret ? "okay" : "not okay") +
//            " NextPlayer: " + (nextPlayer == Board.XPLAYER ? "X" : "Y"));
      return ret;
   }

   /**
    * Check for blocking pins if opponent (XPlayer) has next turn.
    * Method looks to south.
    * @return false if blocking pin was found
    */
   private boolean blockingButtom()
   {
      // everything okay if my turn or nothing is blocking
      return nextPlayer == Board.YPLAYER
            || checkButtomPin(pinX, pinY + 1)
            && checkButtomPin(pinX, pinY + 2)
            && checkButtomPin(pinX + 1, pinY + 1)
            && checkButtomPin(pinX - 1, pinY + 1)
            && checkButtomPinTwo(pinX, pinY + 3)
            && checkButtomPinTwo(pinX, pinY + 4);
   }

   /**
    * Check for blocking pins if opponent (XPlayer) has next turn.
    * Method looks to north.
    * @return false if blocking pin was found
    */
   private boolean blockingTop()
   {
      // everything okay if my turn or nothing is blocking
      return (nextPlayer == Board.YPLAYER)
            || checkTopPin(pinX, pinY - 1)
            && checkTopPin(pinX, pinY - 2)
            && checkTopPin(pinX + 1, pinY - 1)
            && checkTopPin(pinX - 1, pinY - 1)
            && checkTopPinTwo(pinX, pinY - 3)
            && checkTopPinTwo(pinX, pinY - 4);
   }

   /**
    * Check if this pin is an opponent-pin and has connection.
    * The two connection down are not checked.
    * @param oppX x of pin to check
    * @param oppY y of pin to check
    * @return true if pin is not blocking
    */
   private boolean checkButtomPin(final int oppX, final int oppY)
   {
      return !(board.getPin(oppX, oppY) == Board.XPLAYER
            && (board.isBridged(oppX, oppY, 0)
                || board.isBridged(oppX, oppY, 1)
                || board.isBridged(oppX, oppY, 2)
                || board.isBridged(oppX, oppY, 3)
                || board.isBridged(oppX + 2, oppY + 1, 0)
                || board.isBridged(oppX - 2, oppY + 1, 3)
            ));
   }

   /**
    * Check if this pin is an opponent-pin and has connection.
    * The two connection up are not checked.
    * @param oppX x of pin to check
    * @param oppY y of pin to check
    * @return true if pin is not blocking
    */
   private boolean checkTopPin(final int oppX, final int oppY)
   {
      return !(board.getPin(oppX, oppY) == Board.XPLAYER
            && (board.isBridged(oppX, oppY, 0)
            || board.isBridged(oppX, oppY, 3)
            || board.isBridged(oppX + 2, oppY + 1, 0)
            || board.isBridged(oppX + 1, oppY + 2, 1)
            || board.isBridged(oppX - 1, oppY + 2, 2)
            || board.isBridged(oppX - 2, oppY + 1, 3)
      ));
   }

   /**
    * Check if this pin is an opponent-pin and has connection.
    * Only two connection are checked.
    * @param oppX x of pin to check
    * @param oppY y of pin to check
    * @return true if pin is not blocking
    */
   private boolean checkButtomPinTwo(final int oppX, final int oppY)
   {
      return oppY >= board.getYsize()
            || !(board.getPin(oppX, oppY) == Board.XPLAYER
                 &&  (board.isBridged(oppX, oppY, 0)
                   || board.isBridged(oppX, oppY, 3)));
   }

   /**
    * Check if this pin is an opponent-pin and has connection.
    * Only two connection are checked.
    * @param oppX x of pin to check
    * @param oppY y of pin to check
    * @return true if pin is not blocking
    */
   private boolean checkTopPinTwo(final int oppX, final int oppY)
   {
      return oppY <= 0
            || !(board.getPin(oppX, oppY) == Board.XPLAYER
            && (board.isBridged(oppX - 2, oppY + 1, 3)
               || board.isBridged(oppX + 2, oppY + 1, 0)));
   }

   /**
    * Check if pin can be used as connection to bottom.
    * @param fx x-pos of pin
    * @param fy y-pos of pin
    * @param boardIn the board
    * @param nextPlayerIn who's next?
    * @return true, if allowed
    */
   public boolean checkButtom(final int fx, final int fy, final Board boardIn,
         final int nextPlayerIn)
   {
      pinX = fx;
      pinY = fy;
      board = boardIn;
      nextPlayer = nextPlayerIn;

      //check for direct block or blocking races
      return blockingButtom() && checkFor5() && checkFor6() && checkFor4() && checkFor7();
   }

   /**
    * Check if pin can be used as connection to top.
    * @param fx x-pos of pin
    * @param fy y-pos of pin
    * @param boardIn the board
    * @param nextPlayerIn who's next?
    * @return true, if allowed
    */
   public boolean checkTop(final int fx, final int fy, final Board boardIn,
         final int nextPlayerIn)
   {
      pinX = fx;
      pinY = fy;
      board = boardIn;
      nextPlayer = nextPlayerIn;

      //check for direct block or blocking races
      return blockingTop() && checkFor1() && checkFor2() && checkFor0() && checkFor3();
   }

}
