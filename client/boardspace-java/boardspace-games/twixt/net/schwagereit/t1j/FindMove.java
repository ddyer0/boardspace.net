/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package twixt.net.schwagereit.t1j;

import java.util.Map;
import java.util.HashMap;


/**
 * Generate all moves and try to find best one. This class is a singleton.
 * 
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
public final class FindMove
{
   private static final int MILLI_PER_SEC = 1000;
   private static final int GAMEOVER = MILLI_PER_SEC;

   private static Match match = null;

   private static final FindMove FINDMOVES = new FindMove();


   /** the bestMove found yet */
   private Move bestMove;

   private static int maxTime;

   /** The searchddepth currently used */
   private static int currentMaxPly;

   private int currentPlayer;

   private static final int INITIAL_CAPACITY = 10000;
   private final Map<Object,Object> zobristMap = new HashMap<Object,Object>(INITIAL_CAPACITY);
   private Stopwatch clock;
   /** use alphabeta for highest ply. */
   private boolean usealphabeta;
   private static final int WAIT_MILLIS = 20;


   /**
    * Cons'tor - no external instance.
    */
   private FindMove()
   {
   }

   /**
    * Return the FindMove-Object. (Singleton)
    * 
    * @return Findmove-Object
    */
   public static FindMove getFindMove()
   {
      return FINDMOVES;
   }

   /**
    * Find best move for computerplayer.
    * 
    * @param player X- or Y-player, the next player
    * @return a computermove
    */
   public Move computeMove(final int player)
   {
      currentPlayer = player;
      // the first pins are set by simple rules
      Move initMove = InitialMoves.getInstance().initialMove(match, player);
      if (initMove != null)
      {
         // if move was found
         try
         {
            Thread.sleep(WAIT_MILLIS);
         }
         catch (InterruptedException e)
         {
            System.out.println("Sleep Interrupted");
         }
         return initMove;
      }

      clock = new Stopwatch();

      internalComputeMove(player);

      System.out.println("Elapsed: " + clock.getTime() + " msec.");

      return bestMove;
   }

   /**
    * Find best move for computerplayer by using alpha-beta.
    *
    * @param player X- or Y-player, the next player
    */
   private void internalComputeMove(final int player)
   {
      int maxPly;if (GeneralSettings.getInstance().mdFixedPly)
      {
         maxPly = GeneralSettings.getInstance().mdPly;
         maxTime = -1;
      }
      else
      {
         //move moves with time-limit
         maxPly = Integer.MAX_VALUE; // will never be reached
         maxTime = GeneralSettings.getInstance().mdTime;
      }

      // use alpha-beta
      OrderedMoves.initOrderedMoves();
      usealphabeta = false;
      for (currentMaxPly = 3; currentMaxPly <= maxPly; currentMaxPly++)
      {
         if (currentMaxPly != 4 || currentMaxPly == maxPly)
         {
            zobristMap.clear();
            alphaBeta(player, currentMaxPly, -Integer.MAX_VALUE, Integer.MAX_VALUE);
            usealphabeta = true;
            if (maxTime > 0 && maxTime * MILLI_PER_SEC <= clock.getTime())
               break;
         }
      }
   }




   /**
    * Set the match.
    * 
    * @param matchIn The match to set.
    */
   public void setMatch(final Match matchIn)
   {
      FindMove.match = matchIn;
   }


   /**
    * Evaluate situation.
    * @param player player who has next turn
    * @return value of current situation on board - the higher the better for y-player
    */
   private int evaluatePosition(int player)
   {
      int val1, val2;

      // evaluate
      match.getBoardY().getEval().evaluateY(player);
      val1 = match.getBoardY().getEval().valueOfY(false, player);
      match.getBoardX().getEval().evaluateY(-player);
      val2 = match.getBoardX().getEval().valueOfY(false, -player);


      // at the first move only defensive moves are good
      if (match.getMoveNr() < 8)
      {
         if (currentPlayer == Board.YPLAYER)
         {
            val1 = 0;
         }
         else
         {
            val2 = 0;
         }

      }
      return val2 - val1;

      //on equal moves, takes the one nearest to midth
      //vals = (vals << 7) + Math.abs(ownBoard.getXsize() / 2 - element.getX())
      //+ Math.abs(ownBoard.getYsize() / 2 - element.getY());
   }

   /**
    * Make a move on both boards.
    * @param move the move
    * @param player the player
    */
   private void makeMove(Move move, int player)
   {
      match.getBoardY().setPin(move.getX(), move.getY(), player);
      match.getBoardX().setPin(move.getY(), move.getX(), -player);
   }


   /**
    * Unmake a move on both boards.
    * @param move the move
    * @param player the player
    */
   private void unmakeMove(Move move, int player)
   {
      match.getBoardY().removePin(move.getX(), move.getY(), player);
      match.getBoardX().removePin(move.getY(), move.getX(), -player);
   }

   /**
    * Recursive minimax with alpha-beta pruning to find best move.
    * @param player player who has next turn
    * @param ply current depth, decreasing
    * @param alpha alpha-value
    * @param beta beta-value
    * @return value computed
    */
   private int alphaBeta(int player, int ply, int alpha, int beta)
   {
      int val;
      Move move;

//      if (zobristMap.containsKey(new Integer(match.getBoardY().getZobristValue())))
//      {
//         //System.out.println("Treffer bei ply = " + ply + " Hashsize:" + zobristMap.size());
//         return ((Integer) zobristMap.get(new Integer(match.getBoardY().getZobristValue())))
//               .intValue();
//      }
//      zobristVal = zobristMap.get(new Integer(match.getBoardY().getZobristValue()));
//      if (zobristVal != null)
//      {
//         //System.out.println("Treffer bei ply = " + ply + " Hashsize:" + zobristMap.size());
//         return ((Integer) zobristVal).intValue();
//      }

      if (maxTime > 0 && maxTime * MILLI_PER_SEC <= clock.getTime())
      {
         return 0;
      }

      if (ply == 0)
      {
         Object zobristVal=zobristMap.get(match.getBoardY().getZobristValue());
         if (zobristVal != null)
         {
            //System.out.println("Treffer bei ply = " + ply + " Hashsize:" + zobristMap.size());
            return ((Integer) zobristVal).intValue();
         }
         //return evaluatePosition(player);
         val = evaluatePosition(player);
         zobristMap.put(match.getBoardY().getZobristValue(), val);
         return val;
      }

      OrderedMoves moveSet = new OrderedMoves(match);
      moveSet.generateMoves(player, ply == currentMaxPly);

      // a check for game over
      if (moveSet.isGameover())
      {
         // the earlier the better
         return (GAMEOVER + ply) * player;
      }

      // minimizing node
      if (player == Board.XPLAYER)
      {
         while ((move = moveSet.getMove()) != null)
         {  
            makeMove(move, player);
            val = alphaBeta (-player, ply - 1, alpha, beta);
            //zobristMap.put(new Integer(match.getBoardY().getZobristValue()), new Integer(val));

            if (ply == currentMaxPly)
            {
               OrderedMoves.addValuedMove(move, val);
            }

            if (val < beta)
            {
               //
               //
               //
               if (ply == currentMaxPly)
               {
                  // a new best move is only accepted if time is not over
                  if (maxTime <= 0 || maxTime * MILLI_PER_SEC > clock.getTime())
                  {
                     bestMove = move;
                  }

                  //if (currentMaxPly < maxPly)
                  //{
                  //   OrderedMoves.addValuedMove(move, val);
                  //}

                  if (usealphabeta)
                  {
                     beta = val;
                  }
               }
               else
               {
                  beta = val;
                  OrderedMoves.addKiller(move, player);
               }
            }
            unmakeMove(move, player);

            if (beta <= alpha)
            {
               return alpha;
            }
         }
         return beta;
      }
      else
      //maximizing node
      {
         while ((move = moveSet.getMove()) != null)
         {
            makeMove(move, player);
            val = alphaBeta (-player, ply - 1, alpha, beta);
            // zobristMap.put(new Integer(match.getBoardY().getZobristValue()), new Integer(val));
            if (ply == currentMaxPly)
            {
               OrderedMoves.addValuedMove(move, val);
            }
            if (val > alpha)
            {
               if (ply == currentMaxPly)
               {
                  // a new best move is only accepted if time is not over
                  if (maxTime <= 0 || maxTime * MILLI_PER_SEC > clock.getTime())
                  {
                     bestMove = move;
                  }

//                  if (currentMaxPly < maxPly)
//                  {
//                     OrderedMoves.addValuedMove(move, val);
//                  }

                  if (usealphabeta)
                  {
                     alpha = val;
                  }
               }
               else
               {
                  alpha = val;
                  OrderedMoves.addKiller(move, player);
               }
            }


            unmakeMove(move, player);

            if (beta <= alpha)
            {
               return beta;
            }
         }
         return alpha;
      }
   }

}
