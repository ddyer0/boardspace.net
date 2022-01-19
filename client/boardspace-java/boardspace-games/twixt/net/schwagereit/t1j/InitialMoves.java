/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package twixt.net.schwagereit.t1j;

import java.util.Random;

import lib.G;

/**
 * Compute the initial moves. (The first four pins on the board)
 * This class is a singleton.
 *
 * Created by IntelliJ IDEA.
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
public final class InitialMoves
{
   private static final InitialMoves ourInstance = new InitialMoves();
   private static final int FOUR = 4;
   private static final int FIVE = 5;
   private static final int SIX = 6;

   private Match match;
   private int currentPlayer;

   /**
    * Return the InitialMoves-Object.
    * @return InitialMoves-Object
    */
   public static InitialMoves getInstance()
   {
      return ourInstance;
   }

   
   /**
    * Constructor - no external instance.
    */
   private InitialMoves()
   {
   }

   /**
    * Check for pie-rule and initial moves.
    *
    * @param matchIn the match
    * @param player player who has next turn
    * @return An initial move if it exists
    */
   public Move initialMove(final Match matchIn, int player)
   {
      match = matchIn;
      currentPlayer = player;
      Move retMove = null;
      G.print("InitialMove "+match.getMoveNr());
      if (match.getMoveNr() == 0)
      {
         retMove = firstMove();
      }
      else if (match.getMoveNr() <= 3) // simple answer with predefined moves
      {
         retMove = secondToFourthMove();
      }
      else if (match.getMoveNr() <= 5)
      {
         retMove = fifthOrMoreMove();
      }
      return (retMove != null && retMove.getX() >= 0) ? retMove : null;
   }

   /**
    * Compute fifth (or more) move. This is a defensive move.
    * @return a move if possible
    */
   private Move fifthOrMoreMove()
   {
      Move retMove = null;
      int mostFar = 0;
      int distance;
      int bx;
      int by;
      //check all opponent pins
      // find conter-move which is not near other pins
      for (int i = match.getMoveNr(); i >= 1; i-=2)
      {
         bx = match.getMoveX(i);
         by = match.getMoveY(i);
         Move possMove = getCounterPin(bx,by);
         distance = nearestPin(possMove, i);
         if (distance > 3 && distance > mostFar)
         {
            mostFar = distance;
            retMove = possMove;
         }

      }
      return retMove;
   }

   /**
    * Calculate smallest distance to any other pin.
    * @param someMove a move
    * @param except movenumber to exclude from computation
    * @return distance to next pin
    */
   private int nearestPin(Move someMove, int except)
   {
      int dist = Integer.MAX_VALUE;
      int cDist;
      if (someMove == null)
      {
         return -1;
      }
      //check for smallest dist to any other pin
      for (int i = match.getMoveNr(); i >= 1; i--)
      {
         cDist = Math.abs(match.getMoveX(i) - someMove.getX())
               + Math.abs(match.getMoveY(i) - someMove.getY());
         if (except == match.getMoveNr())
         {
            //little bonus for last move
            cDist--;
         }
         if (i != except && cDist < dist)
         {
            dist = cDist;
         }
      }
      return dist;
   }

   /**
    * Try to find a move which blocks oppenent pin.
    * @param bx x of pin to block
    * @param by y of pin to block
    * @return a possible Move
    */
   private Move getCounterPin(final int bx, final int by)
   {
      int rx, ry;
      // ignore if too near to boarder
      if (bx < 4 || by < 4  || by > match.getYsize() - 4 || bx > match.getXsize() - 4)
      {
         return null;
      }
      if (currentPlayer == Board.XPLAYER)
      {
         ry = (by < match.getYsize() / 2) ? by + 4 : by - 4;
         rx = bx;
         if (bx < match.getXsize() / 2 - 3)
         {
            rx = bx + 1;
         }
         else if (bx > match.getXsize() / 2 + 3)
         {
            rx = bx - 1;
         }
      }
      else
      {
         rx = (bx < match.getXsize() / 2) ? bx + 4 : bx - 4;
         ry = by;
         if (by < match.getYsize() / 2 - 3)
         {
            ry = by + 1;
         }
         else if (by > match.getYsize() / 2 + 3)
         {
            ry = by - 1;
         }
      }
      return new Move(rx, ry);
   }

   /**
    * Compute second, third and fourth move.
    * @return a move if possible
    */
   private Move secondToFourthMove()
   {
      Random rand = new Random();
      int bx;
      int by;
      // find previous pin
      bx = match.getMoveX(match.getMoveNr());
      by = match.getMoveY(match.getMoveNr());
      if (currentPlayer == Board.XPLAYER)
      {
         if (by < match.getYsize() / 2)
         {
            by = ((match.getYsize() - by) / 2 + by);
         }
         else
         {
            by /= 2;
         }
         // some random
         by = by - 1 + rand.nextInt(3);
         if (bx <= SIX)
         {
            bx = bx + 2 + rand.nextInt(2);
            if (bx <= FOUR)
            {
               bx++;
            }
         }
         if (bx >= match.getXsize() - SIX - 1)
         {
            bx = bx - 2 - rand.nextInt(2);
            if (bx >= match.getYsize() - FOUR - 1)
            {
               bx--;
            }
         }
      }
      else
      {
         if (bx <= match.getXsize() / 2)
         {
            bx = ((match.getXsize() - bx) / 2 + bx);
         }
         else
         {
            bx /= 2;
         }
         // some random
         bx = bx - 1 + rand.nextInt(3);
         if (by <= SIX)
         {
            by = by + 2 + rand.nextInt(2);
            if (by <= FOUR)
            {
               by++;
            }
         }
         if (by >= match.getYsize() - SIX - 1)
         {
            by = by - 2 - rand.nextInt(2);
            if (by >= match.getYsize() - FOUR - 1)
            {
               by--;
            }
         }
      }
      if (match.getMoveNr() == 2 || match.getMoveNr() == 3)
      {
         // check if 3rd move is really different from 1st
         if (Math.abs(bx - match.getMoveX(match.getMoveNr() - 1)) <= 2
               && Math.abs(by - match.getMoveY(match.getMoveNr() - 1)) <= 2)
         {
            // in this unlikely case an Ares (5/0) will improve my situation
            bx = match.getMoveX(match.getMoveNr() - 1);
            by = match.getMoveY(match.getMoveNr() - 1);
            if (currentPlayer == Board.XPLAYER)
            {
               bx += (bx >= match.getXsize() / 2) ? -FIVE : FIVE;
            }
            else
            {
               by += (by >= match.getYsize() / 2) ? -FIVE : FIVE;
            }
         }
         if (match.getMoveNr() == 3
               && Math.abs(bx - match.getMoveX(match.getMoveNr() - 2)) <= 2
               && Math.abs(by - match.getMoveY(match.getMoveNr() - 2)) <= 2)
         {
            bx = -1;
            by = -1; // should be extremely seldom - take normal mechanism
         }
      }
      return new Move(bx, by);
   }

   /**
    * Compute the first pin.
    * @return Pin
    */
   private Move firstMove()
   {
      Random rand = new Random();

      int[][] startPos = {{2, 10}, {3, 4}, {3, FIVE}, {3, SIX}, {4, 3}, {FIVE, 3}, {SIX, 3}};
      //int[][] startPos = { { 4, 11 }, { 4, 10 }, { FIVE, 10 }, { FIVE, 9 }, { SIX, 8 },
      //      { 6, 6 }, { 5, 6 }, { 6, 5 }, { 9, SIX }, { 4, TWELVE } };

      int bx;
      int by;
      if (match.getMatchData().mdPieRule) // first move with pie-rule
      {
         // previous fixed pin for other board than 24x24
         //bx = match.getXsize() / 6;
         //by = match.getYsize() / 6;

         // for any board-size
         int p = rand.nextInt(startPos.length);
         bx = startPos[p][0] - 1;
         by = startPos[p][1] - 1;

         if (currentPlayer == Board.XPLAYER)
         { // swap
            int k = bx;
            bx = by;
            by = k;
         }
         // mirror coordinates
         if (rand.nextBoolean())
         {
            bx = match.getXsize() - bx - 1;
         }
         if (rand.nextBoolean())
         {
            by = match.getYsize() - by - 1;
         }
         // std::cout << (int) p << " " << (int) bx << "-" << (int) by <<
         // "\n";
      }
      else
      {
         // firstmove is simple without pie-rule
         // some random elements to make playing witout pierule more variable
         int var = match.getXsize() / 4;
         bx = match.getXsize() / 2 + rand.nextInt(var) - var / 2;
         var = match.getYsize() / 4;
         by = match.getYsize() / 2 + rand.nextInt(var) - var / 2;
      }
      return new Move(bx, by);
   }

}
