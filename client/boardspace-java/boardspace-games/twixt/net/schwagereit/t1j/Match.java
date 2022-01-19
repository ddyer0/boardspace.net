/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package twixt.net.schwagereit.t1j;

import java.awt.Cursor;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import lib.SimpleObservable;


/**
 * Store all the data of a match.
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 *  
 */
public final class Match extends SimpleObservable implements Runnable
{
   /** the real board. */
   private final Board boardY; 
   
   /** the mirrored board. */
   private final Board boardX;

   /** the board which is displayed. */
   private final Board boardDisplay;
   
   private MatchData matchData;

   /** next player to place a pin. */
   private int nextPlayer;

   private static final int INIT_ARRAY_SZ = 20;

   /** all the moves played yet. */
   @SuppressWarnings("rawtypes")
private final List moves = new ArrayList(INIT_ARRAY_SZ);

   /** highest move number already used in this game. */
   private int highestMoveNr;

   /** current move number. */
   private int moveNr;
   
   /** move number of move which ended game. */
   private int gameOverMoveNr;
   
   /** True, if game is already over. */
   private boolean gameOver = false;

   /** Calling Component - the window. */
   private GuiMainWindow frame;
   
   /** List of all moves as String. */
   private String moveList;

   /** No moves allowed during computermove. */
   private boolean guiBlocked = false;


   /**
    * Cons'tor for new match.
    */
   Match()
   {
      boardY = Board.getBoard(Board.YPLAYER);
      boardY.setZobristEnabled(true);
      boardX = Board.getBoard(Board.XPLAYER);
      boardDisplay = Board.getBoardDisplay();
      matchData = null;
      //evaluationY = new Evaluation(Board.YPLAYER);
      //evaluationX = new Evaluation(Board.XPLAYER);
      FindMove.getFindMove().setMatch(this);
   }

   /**
    * Get x-size of board.
    * @return x-size of board.
    */
   int getXsize()
   {
      return boardY.getXsize();
   }

   /**
    * Get y-size of board.
    * @return y-size of board.
    */
   int getYsize()
   {
      return boardY.getYsize();
   }

   /**
    * prepare a new match.
    * 
    * @param md Data of new game
    * @param computerMove First move of computer if starting player
    */
   void prepareNewMatch(final MatchData md, final boolean computerMove)
   {
      this.matchData = md;
      nextPlayer = matchData.mdYstarts ? Board.YPLAYER : Board.XPLAYER;
      boardY.clearBoard();
      boardX.clearBoard();
      moves.clear();
      moveNr = 0;
      highestMoveNr = 0;
      gameOverMoveNr = -1;
      gameOver = false;
      setGuiBlocked(false);

      boardY.setSize(matchData.mdXsize, matchData.mdYsize);
      
      boardY.getEval().setupForY();
      boardX.setSize(matchData.mdYsize, matchData.mdXsize);
      boardX.getEval().setupForY();
      resetMoveList();

      //two computer players?
      if (computerVsComputer())
      {
         evaluateAndUpdateGui();
      }

      // the first computerMove?
      if (computerMove)
      {
         computeMove();
      }
      
      //    update GUI
      RightPanel.getInstance().getEndMatchButton().setEnabled(true);
      Board.copyYtoDisplay();
      setChanged();
    }

   /**
    * Update data of a match.
    *
    * @param md Data of new game
    */
   void updateMatchData(final MatchData md)
   {
      this.matchData = md;
      RightPanel.getInstance().getEndMatchButton().setEnabled(true);
      //    update GUI
      setChanged();
   }

   /**
    * Get current move-nr.
    * @return current move-nr
    */
   int getMoveNr()
   {
      return moveNr;
   }

   /**
    * Get highest move-nr used in this match.
    * @return highest move-nr used in this match
    */
   int getHighestMoveNr()
   {
      return highestMoveNr;
   }

   /**
    * Get x-value of specified move.
    * @param nr move-nr
    * @return x of move nr
    */
   int getMoveX(final int nr)
   {
      return ((Move) moves.get(nr - 1)).getX();
   }

   /**
    * Get y-value of specified move.
    * @param nr move-nr
    * @return y of move nr
    */
   int getMoveY(final int nr)
   {
      return ((Move) moves.get(nr - 1)).getY();
   }

   /**
    * A move was done, now some data has to be updated.
    * 
    * @param xin x
    * @param yin y
    * @return true, if move okay
    */
   @SuppressWarnings("unchecked")
boolean setlastMove(final int xin, final int yin)
   {
      if (boardY.setPin(xin, yin, nextPlayer)) // a legal move?
      {
         // set same pin on mirrored board
         if (!boardX.setPin(yin, xin, -nextPlayer))
         {  // shold never happen
            throw new IllegalStateException("Error when setting pin " + yin
                  + ":" + xin + " on  mirrorboard.");
         }
         
         //if last move was undone after gameOver, this is no longer valid
         gameOverMoveNr = -1;
         
         //switch players
         nextPlayer = -nextPlayer;

         //addElement to list of moves
         if (moves.size() > moveNr)
         {
            ((Move) moves.get(moveNr)).setX(xin);
            ((Move) moves.get(moveNr)).setY(yin);
         } else
         {
            moves.add(moveNr, new Move(xin, yin));
         }
         moveNr++;
         highestMoveNr = moveNr;
         appendLastMoveList();
         
         return true;
      } else
      {  // move was not legal
         return false;
      }

   }

   /**
    * Evaluate situation and update GUI.
    */
   public void evaluateAndUpdateGui()
   {

      //update GUI
      Board.copyYtoDisplay();
      setChanged();
 
      new Thread(this).start();   // start run() to compute move

   }

   /**
    * Move is computed in separate thread.
    */
   public void run()
   {
      pieRule();
      checkForGameOver();
      do
      {
         computeMove();
      } while (!gameOver && (nextPlayer == Board.XPLAYER && !matchData.mdXhuman ||
            nextPlayer == Board.YPLAYER && !matchData.mdYhuman));

   }

   /**
    * Check if pie-rule is used.
    */
   private void pieRule()
   {
      boolean swap = false;

      // if both players are the computer, the first pin is always accepted
      if (!matchData.mdXhuman && !matchData.mdYhuman)
      {
         return;
      }

      if (moveNr == 1 && matchData.mdPieRule)
      {
         if (nextPlayer == Board.XPLAYER && !matchData.mdXhuman
               || nextPlayer == Board.YPLAYER && !matchData.mdYhuman)
         {
            Move thisMove = (Move) moves.get(moveNr - 1);
            // normalize to middle of field
            /*
            double x = thisMove.getX() - matchData.mdXsize / 2 - HALF;
            double y = thisMove.getY() - matchData.mdYsize / 2 - HALF;
            double xout = matchData.mdXsize / 2 - HALF ;
            double xmid = matchData.mdXsize / 2 - HALF ;
            double ymid = matchData.mdYsize / 2 - HALF ;
            double yinn = matchData.mdYsize / 2 - HALF ;

            if (nextPlayer == Board.YPLAYER) // currentPlayer is computer
            {  // swap
               double z = x;
               x = y;
               y = z;
            } 
            // inner ellipse
            double di = x * x / (xmid * xmid) + y * y / (yinn * yinn); 
            // outer ellipse
            double da = x * x / (xout * xout) + y * y / (ymid * ymid); 
            boolean swapping = (di <= 1 || (da < 1 && new Random()
                  .nextBoolean()));
            */
            double x = thisMove.getX();
            double y = thisMove.getY();

            boolean swapping =
                  x > 2 && x < matchData.mdXsize - 3 && y > 2 && y < matchData.mdYsize - 3;

            if (swapping)
            {
               JOptionPane.showMessageDialog(Control.getMainWindow(),
                     Messages.getString("Match.computerSwap"),
                     Messages.getString("Match.pierule"), JOptionPane.INFORMATION_MESSAGE);
               swap = true;
            } else
            {
               JOptionPane.showMessageDialog(Control.getMainWindow(),
                     Messages.getString("Match.computerAccept"),
                     Messages.getString("Match.pierule"), JOptionPane.INFORMATION_MESSAGE);
            }
         } else
         {
            // human has to decide
            Object[] possibleValues = {Messages.getString("Match.accept"),
                  Messages.getString("Match.swap")};
            int ret = JOptionPane.showOptionDialog(Control.getMainWindow(), Messages
                  .getString("Match.pieruleQ"), Messages
                  .getString("Match.pierule"), JOptionPane.DEFAULT_OPTION,
                  JOptionPane.QUESTION_MESSAGE, null, possibleValues,
                  possibleValues[0]);
            swap = (ret == 1);
         }
         if (swap)
         {
            //swap sides
            String swaps = matchData.mdPlayerX;
            //noinspection SuspiciousNameCombination
            matchData.mdPlayerX = matchData.mdPlayerY;
            matchData.mdPlayerY = swaps;
            boolean swapb = matchData.mdXhuman;
            matchData.mdXhuman = matchData.mdYhuman;
            matchData.mdYhuman = swapb;
            setChanged();
            computeMove();
         }
      }
   }
   
   /**
    * Compute next move for the computer, if necessary.
    */
   public synchronized void computeMove()
   {
      //do nothing if there is no computerplayer
      Cursor cursor;
      Move move = null;
      if (!isGameOver()
            && (nextPlayer == Board.XPLAYER && !matchData.mdXhuman || nextPlayer == Board.YPLAYER
                  && !matchData.mdYhuman))
      {
         setGuiBlocked(true);
         //wait-cursor
         cursor = frame.getCursor();
         frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

         move = FindMove.getFindMove().computeMove(nextPlayer);
         // System.out.println(moveNr);

         setGuiBlocked(false);
         //reset cursor
         frame.setCursor(cursor);
      }
      if (move == null)
      {  // no computer move was needed
         return;
      }
      if (!setlastMove(move.getX(), move.getY()))
      {  // should never happen
         throw new IllegalStateException(
               "Computermove at " + move.getX() + "," + move.getY() + " is illegal - " + moveNr);
      }

      //    update GUI
      Board.copyYtoDisplay();
      setChanged();
      pieRule();
      checkForGameOver();
   }
   
   /** 
    * Check is game is now over.
    */
   private void checkForGameOver()
   {
      // check for finished match
      String matchOverMessage = "";
      if (boardY.checkGameOver())
      {
         matchOverMessage = matchData.mdPlayerY + " "
         + Messages.getString("Match.topDown");
      }
      else if (boardX.checkGameOver())
      {
         matchOverMessage = matchData.mdPlayerX + " "
         + Messages.getString("Match.leftRight");
      }
      else if (computerVsComputer() && moveNr * 3 > boardY.getXsize() * boardY.getYsize())
      {
         //Match is draw, if computer plays against computer and too many holes are occupied
         matchOverMessage = Messages.getString("Match.draw");
      }
      if (matchOverMessage.length() > 0)
      {
         // game is now over
         JOptionPane.showMessageDialog(Control.getMainWindow(), matchOverMessage, 
               Messages.getString("Match.gameOver"),
               JOptionPane.INFORMATION_MESSAGE);
         gameOverMoveNr = moveNr;
         gameOver = true;
         // after computerVsComputer, both sides are set to human
         if (computerVsComputer())
         {
            matchData.mdYhuman = true;
            matchData.mdXhuman = true;
            //set empty names which are corrected in correct()
            matchData.setDefaultNames(true);
            setChanged();
         }
      }
   }
   
   /**
    * Undo a single move.
    */
   private void removeSingleMove()
   {
      moveNr--;
      Move thisMove = (Move) moves.get(moveNr);
      if (boardY.removePin(thisMove.getX(), thisMove.getY(), -nextPlayer)) // a legal remove?
      {
         boardX.removePin(thisMove.getY(), thisMove.getX(), nextPlayer);
         
         nextPlayer = -nextPlayer;
         
         removeLastMoveList();
      }
   }
   
   /**
    * Undo one or two already done moves.
    */
   void removeMove()
   {
      gameOver = false;
      removeSingleMove();
      // if opponent is computer-player, remove another pin
      if (!getMatchData().mdXhuman && nextPlayer == Board.XPLAYER
            || !getMatchData().mdYhuman && nextPlayer == Board.YPLAYER)
      {
         removeSingleMove();
      }
      
      //update GUI
      Board.copyYtoDisplay();
      setChanged();
   }

   /**
    * redo a single undone move.
    */
   private void redoSingleMove()
   {
      Move thisMove = (Move) moves.get(moveNr);
      if (boardY.setPin(thisMove.getX(), thisMove.getY(), nextPlayer)) // a legal redo?
      {
         boardX.setPin(thisMove.getY(), thisMove.getX(), -nextPlayer);
         
         moveNr++;

         nextPlayer = -nextPlayer;

         appendLastMoveList();
      }
   }
   /**
    * redo one or two undone moves.
    */
   void redoMove()
   {
      redoSingleMove();
      
      // if opponent is computer-player, remove another pin
      if (!getMatchData().mdXhuman && nextPlayer == Board.XPLAYER
            || !getMatchData().mdYhuman && nextPlayer == Board.YPLAYER)
      {
         redoSingleMove();
      }
      
      if (gameOverMoveNr == moveNr)
      {
         gameOver = true;
      }
      
      //update GUI
      Board.copyYtoDisplay();
      setChanged();
   }

   /**
    * Returns the nextPlayer.
    * @return The next Player
    */
   public int getNextPlayer()
   {
      return nextPlayer;
   }

   /**
    * Get Board.
    * @return Returns the board.
    */
   public Board getBoardY()
   {
      return boardY;
   }

   /**
    * Get mirrored Board.
    * @return Returns the board.
    */
   public Board getBoardX()
   {
      return boardX;
   }

   /**
    * Get Board to display.
    * @return Returns the board.
    */
   public Board getBoardDisplay()
   {
      return boardDisplay;
   }

   /**
    * Get Matchdata.
    * @return Returns the matchData.
    */
   public MatchData getMatchData()
   {
      return matchData;
   }

   /**
    * Get the current move-list.
    * @return Returns the moveList.
    */
   public String getMoveList()
   {
      return moveList;
   }

   /**
    * Set moveList to empty value.
    */
   private void resetMoveList()
   {
      this.moveList = "";
   }
   
   /**
    * Append last move to list.
    */
   private void appendLastMoveList()
   {
      moveList += getMoveString(getMoveNr());
   }
   
   /**
    * remove the last entry from the move list.
    */
   private void removeLastMoveList()
   {
      String listText = getMoveList();
      int spos = listText.indexOf("" + (getMoveNr() + 1) + "."); //$NON-NLS-1$
      moveList  = (spos > 0) ? listText.substring(0, spos - 1) : ""; //$NON-NLS-1$
   }

   /**
    * get the Last Move as String.
    * 
    * @param nr
    *           no of move
    * @return String of last move, e.g. "A5"
    */
   private String getMoveString(final int nr)
   {
      int x, y;
      x = getMoveX(nr);
      y = getMoveY(nr);

      return new StringBuffer()
            .append(getMoveNr() <= 1 ? "" : System.getProperty("line.separator")).append(
            getMoveNr()).append(".  ").append(GuiBoard.getHoleName(x, y, true)).toString();
   }

   /**
    * Get Board for specified player.
    * @param player x- or y-player
    * @return Returns the board.
    */
   public Board getBoard(final int player)
   {
      return (player == Board.XPLAYER) ? boardX : boardY;
   }

   /**
    * Set the calling component (the window).
    * @param compoIn The compo to set.
    */
   public void setFrame(final GuiMainWindow compoIn)
   {
      this.frame = compoIn;
   }

   /**
    * Get MainWindow.
    * @return guiMainWindow
    */
   public GuiMainWindow getFrame()
   {
      return frame;
   }

   /**
    * Check if game is over.
    * @return Returns the gameOver.
    */
   public boolean isGameOver()
   {
      return gameOver;
   }


   /**
    * Check if GUI is blocked.
    * @return Returns the guiBlocked.
    */
   public boolean isGuiBlocked()
   {
      return guiBlocked;
   }

   /**
    * Block or unblock GUI.
    * @param guiBlockedIn true for block
    */
   private void setGuiBlocked(boolean guiBlockedIn)
   {
      this.guiBlocked = guiBlockedIn;
   }

   /**
    * Check if program plays against itself.
    * @return true if computer plays against computer
    */
   public boolean computerVsComputer()
   {
      return !matchData.mdXhuman && !matchData.mdYhuman;
   }
}