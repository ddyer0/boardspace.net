/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package twixt.net.schwagereit.t1j;
import java.util.*;


/**
 * Compute value of position and find critical pin.
 *
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
public final class Evaluation
{


   /**
    * A critPos is a pin which is critical for evaluation of board.
    */
   static final class CritPos
   {
      static final boolean UP = false; // up resp. left

      static final boolean DOWN = true; // down resp. right

      private final int x;
      private final int y;

      /* Direction. */
      private final boolean dir;

      /**
       * Cons'tor.
       *
       * @param xin
       *           x
       * @param yin
       *           y
       * @param dirin
       *           direction (UP or DOWN)
       */
      public CritPos(final int xin, final int yin, final boolean dirin)
      {
         x = xin;
         y = yin;
         dir = dirin;
      }

      /**
       * Return the Direction. Down/Right is true.
       * @return Returns the dir.
       */
      public boolean isDir()
      {
         return dir;
      }

      /**
       * Get value of x.
       * @return Returns the x.
       */
      public int getX()
      {
         return x;
      }

      /**
       * Get value of y.
       * @return Returns the y.
       */
      public int getY()
      {
         return y;
      }

      public String toString()
      {
         return new Move(x ,y) + "-" + dir;
      }
   }

   /**
    * a Position on the board.
    */
   static final class PinPosition
   {
      private final int x;
      private final int y;

      /**
       * Cons'tor.
       *
       * @param xin
       *           x
       * @param yin
       *           y
       */
      public PinPosition(final int xin, final int yin)
      {
         x = xin;
         y = yin;
      }

      /**
       * Returns the x.
       * @return Returns the x.
       */
      public int getX()
      {
         return x;
      }

      /**
       * Returns the y.
       * @return Returns the y.
       */
      public int getY()
      {
         return y;
      }
   }

   /**
    * Data of a hole on the board.
    */
   protected static final class FieldData
   {
      private int value; // only used for set pins

      private int fatherX, fatherY; // next own pin upwards
      private int relX, relY; // computed relevant own pin upwards

      /**
       * Return the X of father-pin.
       * @return Returns the fatherX.
       */
      public int getFatherX()
      {
         return fatherX;
      }

      /**
       * Return the Y of father-pin.
       * @return Returns the fatherY.
       */
      public int getFatherY()
      {
         return fatherY;
      }

      /**
       * Set father-pin.
       * @param fatherXIn The fatherX to set.
       * @param fatherYIn The fatherY to set.
       */
      public void setFather(final int fatherXIn, final int fatherYIn)
      {
         this.fatherX = fatherXIn;
         this.fatherY = fatherYIn;
      }

      /**
       * Get value.
       * @return Returns the value.
       */
      public int getValue()
      {
         return value;
      }

      /**
       * Set value.
       * @param valueIn The value to set.
       */
      public void setValue(final int valueIn)
      {
         this.value = valueIn;
      }

      /**
       * Return x of relevant pin.
       * @return Returns the relX.
       */
      public int getRelX()
      {
         return relX;
      }

      /**
       * Set relevant pin.
       * @param relXin The relX to set.
       * @param relYin The relY to set.
       */
      public void setRel(final int relXin, final int relYin)
      {
         this.relX = relXin;
         this.relY = relYin;
      }

      /**
       * Return y of relevant pin.
       * @return Returns the relY.
       */
      public int getRelY()
      {
         return relY;
      }
   }

   /** Array of internal data for evaluation. */
   final FieldData[][] data = new FieldData[Board.MAXDIM][Board.MAXDIM]; // 36

   /** handling the internal stack. */
   private static final int GRAPH_SZ = 200;

   private final int[] graphstX = new int[GRAPH_SZ];

   private final int[] graphstY = new int[GRAPH_SZ];

   private int stackcnt;

   private final Board board;

   private static final int BLOCKED_FIELD_VAL = 99;

   private static final int UNUSED = -1;

   private static final int NO_FATHER = -2;

   /** Multiplicator for distance in value. */
   private static final int MULT = 10;

   private static final int MALUS_1 = 2 * MULT + 1;
   private static final int MALUS_2 = MULT + 3;
   private static final int BLOCKED_FIELD_VAL_MULT = BLOCKED_FIELD_VAL * MULT;

   /** set of all critical positions. */
   private final Set<CritPos> critPoss = new HashSet<CritPos>();

   /** number of own pins in given row. */
   private final int[] noPins = new int[Board.MAXDIM];

   /** positions of own pins in given rows. */
   private final int[][] ownPin = new int[Board.MAXDIM][Board.MAXDIM]; // 36 is max.
   
   /** Is this board mirrored?. */
   // private final boolean mirrored;

   /**
    * Cons'tor.
    * @param boardIn Board for this player
    */
   public Evaluation(final Board boardIn)
   {
      board = boardIn;
      
      // mirrored = (player == Board.XPLAYER);

      // create the objects for the data-array
      for (int i = 0; i < data.length; i++)
      {
         for (int j = 0; j < data[i].length; j++)
         {
            data[i][j] = new FieldData();
         }
      }
   }

   /**
    * Recursive method to give all pins of a structure the same value.
    * 
    * @param val value to propagate
    * @return smallest row visited
    */
   private int evalStructure(final int val)
   {
      int x1, y1, x2, y2;
      while (stackcnt > 0)
      {
         // handle top of stack
         stackcnt--;

         x1 = graphstX[stackcnt];
         y1 = graphstY[stackcnt];
         /*
          * TO DO ermitteln der kleinsten Zeile if (dir == Board.XPLAYER) { if
          * (x1 < smin) smin = x1; } else { if (y1 < smin) smin = y1; }
          */
         data[x1][y1].setValue(val);

         // addElement all the 8 neighbours
         x2 = x1 + 1;
         y2 = y1 - 2;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > val)
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 + 2;
         y2 = y1 - 1;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > val)
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 + 2;
         y2 = y1 + 1;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > val)
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 + 1;
         y2 = y1 + 2;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > val)
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }

         x2 = x1 - 1;
         y2 = y1 + 2;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > val)
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 - 2;
         y2 = y1 + 1;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > val)
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 - 2;
         y2 = y1 - 1;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > val)
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 - 1;
         y2 = y1 - 2;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > val)
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
      }
      return 0; // TO DO: smin; //return smallest x or y found because re-check
      // may be necessary
   }

   /**
    * Search recursive for pin which connects to next father.
    * @param xc x
    * @param yc y
    * @return Position of Pin  which connects
    */
   private PinPosition getFatherConn(final int xc, final int yc)
   {
      int x1, y1, x2, y2;
      graphstX[0] = xc;
      graphstY[0] = yc;
      stackcnt = 1;
      while (stackcnt > 0)
      {
         //handle top of stack
         stackcnt--;
   
         x1 = graphstX[stackcnt];
         y1 = graphstY[stackcnt];
         
         if (data[x1][y1].getRelX() > NO_FATHER)
         {
            //connection to next father (or border) found 
            return new PinPosition(x1, y1);
         }
         data[x1][y1].setValue(-(BLOCKED_FIELD_VAL));
   
         //addElement all the 8 neighbours
         x2 = x1 + 1;
         y2 = y1 - 2;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > -(BLOCKED_FIELD_VAL))
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 + 2;
         y2 = y1 - 1;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > -(BLOCKED_FIELD_VAL))
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 + 2;
         y2 = y1 + 1;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > -(BLOCKED_FIELD_VAL))
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 + 1;
         y2 = y1 + 2;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > -(BLOCKED_FIELD_VAL))
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
   
         x2 = x1 - 1;
         y2 = y1 + 2;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > -(BLOCKED_FIELD_VAL))
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 - 2;
         y2 = y1 + 1;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > -(BLOCKED_FIELD_VAL))
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 - 2;
         y2 = y1 - 1;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > -(BLOCKED_FIELD_VAL))
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
         x2 = x1 - 1;
         y2 = y1 - 2;
         if (board.isConnected(x1, y1, x2, y2) && data[x2][y2].getValue() > -(BLOCKED_FIELD_VAL))
         {
            graphstX[stackcnt] = x2;
            graphstY[stackcnt] = y2;
            stackcnt++;
         }
      }
      return new PinPosition(-1, -1); // TO DO: richtig?
   
   }


   /**
    * Calculate distance to next own pin above.
    * 
    * @param xi
    *           x
    * @param yi
    *           y
    * @return true, if pin was found
    */
   private boolean calculateDistForY(final int xi, final int yi)
   {
      boolean pinFound = false;
      if (yi <= 0)
      {
         if (yi < 0)
         {
            return false; // not allowed
         }
         // first line
         data[xi][yi].setFather(xi, 0);
         if (board.getPin(xi, yi) == Board.YPLAYER)
         { // own pin
            data[xi][yi].setValue(0);
         }
      }
      else
      {
         int faX, faY = UNUSED;
         // blocking bridges
         if (board.isBridged(xi - 1, yi, 3) || board.isBridged(xi + 1, yi, 0))
         {
            faX = BLOCKED_FIELD_VAL;
         }
         else if (board.getPin(xi, yi - 1) == Board.XPLAYER)
         { // enemy pin above
            pinFound = true;
            faX = BLOCKED_FIELD_VAL;
         }
         else if (board.getPin(xi, yi - 1) == Board.YPLAYER)
         { // own pin above
            pinFound = true;
            faX = xi;
            faY = yi - 1;
         }
         else
         {
            faX = data[xi][yi - 1].getFatherX();
            faY = data[xi][yi - 1].getFatherY();
         }
         // check if own pin left or right above
         if (board.getPin(xi - 1, yi - 2) == Board.YPLAYER
               && board.bridgeAllowed(xi, yi, 1))
         { // left
            faX = xi - 1;
            faY = yi - 2;
         }
         else if (board.getPin(xi + 1, yi - 2) == Board.YPLAYER
               && board.bridgeAllowed(xi, yi, 2))
         { // right
            faX = xi + 1;
            faY = yi - 2;
         }
         else if (board.getPin(xi - 2, yi - 1) == Board.YPLAYER
               && board.bridgeAllowed(xi, yi, 0))
         { // right
            faX = xi - 2;
            faY = yi - 1;
         }
         else if (board.getPin(xi + 2, yi - 1) == Board.YPLAYER
               && board.bridgeAllowed(xi, yi, 3))
         { // right
            faX = xi + 2;
            faY = yi - 1;
         }
         data[xi][yi].setFather(faX, faY);
      }
      return pinFound; // TO DO ist das optimal?
   }

   /**
    * setup the evaluation data from scratch for YPlayer.
    */
   public void setupForY()
   {
      final int xmax = board.getXsize();
      final int ymax = board.getYsize();
      int xi, yi;

      // setup first row
      noPins[0] = 0;
      for (xi = 0; xi < xmax; xi++)
      {
         calculateDistForY(xi, 0);
         if (board.getPin(xi, 0) == Board.YPLAYER)
         {
            ownPin[0][noPins[0]] = xi;
            noPins[0]++;
         }
      }

      for (yi = 1; yi < ymax; yi++)
      {
         noPins[yi] = 0;
         for (xi = 0; xi < xmax; xi++)
         {
            calculateDistForY(xi, yi);
            // addElement own pin to list of own pins
            if (board.getPin(xi, yi) == Board.YPLAYER)
            {
               ownPin[yi][noPins[yi]] = xi;
               noPins[yi]++;
            }
         }
      }
   }

   
   
   /**
    * Update rows below.
    * @param xin x Position of new or removed pin
    * @param yin y Position of new or removed pin
    * @param player x or y-player
    */
   private void updateRows(final int xin, final int yin, final int player)
   {
      // check the fields below in this column
      for (int yi = yin; yi < board.getYsize(); yi++)
      {
         // break if other pin found (regardless of color)
         if (calculateDistForY(xin, yi) && yi > yin + 1)
         {
            break;
         }
      }
      // check the columns to the left and right as well
      if (xin >= 2)
      {
         for (int yi = yin - 1; yi < board.getYsize(); yi++)
         {
            // break if other pin found (regardless of color)
            if (calculateDistForY(xin - 1, yi) && yi > yin + 2)
            {
               break;
            }
         }
      }
      if (xin >= 3)
      {
         calculateRow(xin - 2, yin);            
      }
      if (xin < board.getXsize() - 2)
      {
         for (int yi = yin - 1; yi < board.getYsize(); yi++)
         {
            // break if other pin found (regardless of color)
            if (calculateDistForY(xin + 1, yi) && yi > yin + 2)
            {
               break;
            }
         }
      }
      if (xin < board.getXsize() - 3)
      {
         calculateRow(xin + 2, yin);            
      }
      //in some case it is necessary to check 3rd row as well
      if (player == Board.XPLAYER)
      {
         if (xin < board.getXsize() - 3
               && (board.getPin(xin + 2, yin + 1) == Board.XPLAYER 
                || board.getPin(xin + 2, yin - 1) == Board.XPLAYER))
         {
            calculateRow(xin + 3, yin);            
         }
         if (xin >= 3
               && (board.getPin(xin - 2, yin + 1) == Board.XPLAYER || board
                     .getPin(xin - 2, yin - 1) == Board.XPLAYER))
         {
            calculateRow(xin - 3, yin);            
         }
      }
   }

   /**
    * Calculate the distance to next pin for own row.
    * @param xin x of position to start
    * @param yin y
    */
   private void calculateRow(final int xin, final int yin)
   {
      for (int yi = yin - 1; yi < board.getYsize(); yi++)
      {
         // break if other pin found (regardless of color)
         if (calculateDistForY(xin, yi) && yi > yin + 2)
         {
            break;
         }
      }
   }
   
   /**
    * Add a pin in Evaluation for Y-Player. This pin can either be an own one or
    * enemy-pin.
    * 
    * @param xin
    *           x
    * @param yin
    *           y
    */
   public void addForY(final int xin, final int yin)
   {
      final int player = board.getPin(xin, yin);
      updateRows(xin, yin, player);
      // addElement own pin to list of own pins
      if (player == Board.YPLAYER)
      {
         ownPin[yin][noPins[yin]] = xin;
         noPins[yin]++;
      }
   } // addForY

   /**
    * Remove a pin in Evaluation for Y-Player. This pin can either be an own one
    * or enemy-pin.
    * 
    * @param xin
    *           x
    * @param yin
    *           y
    * @param player
    *           player who removes pin
    */
   public void removeForY(final int xin, final int yin, final int player)
   {
      updateRows(xin, yin, player);
      // remove own pin from list of own pins
      if (player == Board.YPLAYER)
      {
         noPins[yin]--;
      }

   } // removeForY

   /**
    * Computes value of given position by checking distance to next pin.
    * 
    * @param x
    *           x-coord
    * @param y
    *           y-coord
    * @return value
    */
   private int getDistVal(final int x, final int y)
   {
      int dv = BLOCKED_FIELD_VAL_MULT;
      
      if (x < 1 || y < 0 || x >= board.getXsize()) // y > board.getYsize() never happens
      {
         return dv; //out of borders
      }
      
      FieldData thisData = data[x][y];
      
      if (thisData.getFatherX() == BLOCKED_FIELD_VAL)
      {
         return dv;
      }
      
      if (thisData.getFatherY() == 0)
      {
         dv = y * MULT;
      }
      else 
      {
         dv = (y - thisData.getFatherY()) * MULT
         + data[thisData.getFatherX()][thisData.getFatherY()].getValue();
      }
      return dv
      + (Math.abs(thisData.getFatherX() - x) == 1 ? 1 : (Math
            .abs(thisData.getFatherX() - x) == 2 ? 3 : 0));
   }
   
   /**
    * Assign values to all own pins.
    * @param nextPlayer who is playing next
    */
   void evaluateY(int nextPlayer)
   {
      final int ymax = board.getYsize();
      int i, n, yi, pval, zval, col;
      int relevantXpos, relevantYpos;
      // reset the values of all own pins
      for (yi = 1; yi < ymax; yi++) // each row
      {
         for (i = 0, n = noPins[yi]; i < n; i++)
         {
            data[ownPin[yi][i]][yi].setValue(BLOCKED_FIELD_VAL_MULT);
            data[ownPin[yi][i]][yi].setRel(NO_FATHER, NO_FATHER);
            // fatherX[ownPin[yi][i]][yi] = NO_FATHER;
         }
      }
      // calculate the values
      for (yi = 0; yi < ymax; yi++) // each row
      {
         for (i = 0, n = noPins[yi]; i < n; i++) // each own pin in the row
         {
            col = ownPin[yi][i];
            
            pval = getDistVal(col, yi);
            relevantXpos = data[col][yi].getFatherX();
            relevantYpos = data[col][yi].getFatherY();
            //System.out.print("<pval: " + pval + "> " + relevantXpos);
            
            // if pin is block by opponent's pins, lets look to the side 
            if (relevantXpos == BLOCKED_FIELD_VAL)
            {
               // lets look to the left
               zval = getDistVal(col - 1, yi - 2) + MALUS_1;
               if (board.pinAllowed(col - 1, yi - 2, Board.YPLAYER)
                     && board.bridgeAllowed(col, yi, 1) && zval < pval)
               {
                  pval = zval;
                  relevantXpos = data[col - 1][yi - 2].getFatherX();
                  relevantYpos = data[col - 1][yi - 2].getFatherY();
                  // System.out.print("(l)");
               }
               // look to the right
               zval = getDistVal(col + 1, yi - 2) + MALUS_1;
               if (board.pinAllowed(col + 1, yi - 2, Board.YPLAYER)
                     && board.bridgeAllowed(col, yi, 2) && zval < pval)
               {
                  pval = zval;
                  relevantXpos = data[col + 1][yi - 2].getFatherX();
                  relevantYpos = data[col + 1][yi - 2].getFatherY();
                  // System.out.print("(r)");
               }
               
               // look to the far left
               zval = getDistVal(col - 2, yi - 1) + MALUS_2;
               if (board.pinAllowed(col - 2, yi - 1, Board.YPLAYER)
                     && board.bridgeAllowed(col, yi, 0) && zval < pval)
               {
                  pval = zval;
                  relevantXpos = data[col - 2][yi - 1].getFatherX();
                  relevantYpos = data[col - 2][yi - 1].getFatherY();
                  // System.out.print("(ll)");
               }
               else
               {
                  // look to the far right
                  zval = getDistVal(col + 2, yi - 1) + MALUS_2;
                  if (board.pinAllowed(col + 2, yi - 1, Board.YPLAYER)
                        && board.bridgeAllowed(col, yi, 3) && zval < pval)
                  {
                     pval = zval;
                     relevantXpos = data[col + 2][yi - 1].getFatherX();
                     relevantYpos = data[col + 2][yi - 1].getFatherY();
                     // System.out.print("(rr)");
                  }
               }
            }

            //Value is only taken from baseline if not 2 columns away (if its not my turn)
            //   and not blocked by races
            // ('relevantYpos != 0' means: tests only on baseline)
            boolean acceptFather = relevantYpos != 0 ||
                  (checkPlausiTop(col, relevantXpos, relevantYpos, nextPlayer))
                        && Races.getRaces().checkTop(col, yi, board, nextPlayer);

            //System.out.println("pval: " + pval + " - " + data[col][yi].getValue() + ":" + acceptFather);

            // take new value only if smaller than current value
            if (acceptFather && (pval < data[col][yi].getValue() || yi == 0))
            {
               data[col][yi].setRel(relevantXpos, relevantYpos);
               // start recursive analysis
               graphstX[0] = col;
               graphstY[0] = yi;
               stackcnt = 1;
               evalStructure(pval);
            }
         } // for x
      } // for y
   } // evaluateY

   /**
    * Compute value of situation. EvaluateY has to be called before.
    * @param computeCritical Compute critical pin as well?
    * @param nextPlayer Who's turn is next?
    * @return value of situation. The smaller the better. '0' for game-over.
    */
   int valueOfY(final boolean computeCritical, final int nextPlayer)
   {
      int bestVal = BLOCKED_FIELD_VAL * MULT, checkX, checkY;
      int bestValTenth = bestVal / 10;
      int distTenth;
      int ySz = board.getYsize() - 1;

      Set<Move> startingPoints = new HashSet<Move>();

      for (int xi = 1; xi < board.getXsize() - 1; xi++) // each field of last row
      {
         if (board.getPin(xi, ySz) == Board.YPLAYER)
         { // own pin in last row
            if (data[xi][ySz].getValue() <= bestVal)
            {
               bestVal = data[xi][ySz].getValue();
            }
            if (computeCritical)
            {
               distTenth = data[xi][ySz].getValue() / 10;
               if (distTenth < bestValTenth)
               {
                  startingPoints.clear();
                  bestValTenth = distTenth;
               }
               if (distTenth <= bestValTenth)
               {
                  startingPoints.add(new Move(xi, ySz));
               }
            }
         }
         else
         {
            int dist = getDistVal(xi, ySz);
            distTenth = dist / 10;
            if (data[xi][ySz].getFatherY() > 0 && distTenth <= bestValTenth)
            {
               checkX = data[xi][ySz].getFatherX();
               checkY = data[xi][ySz].getFatherY();
               //check for races
               //never accept pin which are too far to the side if
               //   next pin is set by opponent
               if (checkPlausiBottom(xi, checkX, checkY, nextPlayer)
                     && Races.getRaces().checkButtom(checkX, checkY, board, nextPlayer))
               {
                  if (computeCritical)
                  {
                     if (distTenth < bestValTenth)
                     {
                        startingPoints.clear();
                        bestValTenth = distTenth;
                     }
                     if (distTenth <= bestValTenth)
                     {
                        startingPoints.add(new Move(checkX, checkY));
                     }
                  }
                  if (dist < bestVal)
                  {
                  bestVal = dist;
                  }
               }
            }
         } // else
      } // for
      // now the best value is known and the position of the pin to compute criticals
      // The critical pins can be found if needed
      if (computeCritical)
      {
         critPoss.clear();
         for (Iterator<Move> iterator = startingPoints.iterator(); iterator.hasNext();)
         {
            Move move = (Move) iterator.next();
            computeCritical(move.getX(), move.getY());
         }
      }
      return bestVal;
   } // valueOfY

   /**
    * Get all the critical Pins and their directions.
    * valueOfY has to be called before.
    * 
    * @param xin x-Position of last pin in critical path
    * @param yin y
    */
   private void computeCritical(final int xin, final int yin)
   {
      int xc = xin, yc = yin;
      PinPosition fp;
      boolean finish = false;
      FieldData thisData;

      // critPoss.clear();

      do
      {  // if not in the last row and not empty in the first
         if (yc < board.getYsize() - 1 && board.getPin(xc, yc) == Board.YPLAYER)
         {
            critPoss.add(new CritPos(xc, yc, CritPos.DOWN));
         }

         //start recursive procedure to find northpole
         fp = getFatherConn(xc, yc);
         if (fp.getX() < 0)
         {
            finish = true;
         } else
         {
            if (fp.getY() > 0)
            {
               critPoss.add(new CritPos(fp.getX(), fp.getY(), CritPos.UP));
            }
            thisData = data[fp.getX()][fp.getY()];
            if (thisData.getRelX() < 0 || fp.getY() == 0)
            {
               finish = true;
            } else
            {
               xc = thisData.getRelX();
               yc = thisData.getRelY();
            }
         }
      } while (!finish);

   } //computeCritical

   /**
    * Get all critical Positions.
    * @return Set of critical pins
    */
   public Set<CritPos> getCritical()
   {
      return critPoss;
   }

   /**
    * Check if a pin near the border could be successful.
    * @param col relevant column
    * @param rx x-pos of pin
    * @param ry y-pos of pin
    * @param nextPlayer who's next?
    * @return true, if pin is acceptable
    */
   private boolean checkPlausiTop(int col, int rx, int ry, int nextPlayer)
   {
      /** vert is the vertical distance to reference-pin. */
      int vert = Math.abs(col - rx);

      // okay, if no vertical distance
      if (vert == 0)
      {
         return true;
      }

      // not okay, if vert to big and other players turn
      if (vert >= 2 && nextPlayer == Board.XPLAYER)
      {
         return false;
      }

      /**/
      // okay, if near border
      if (ry < 6)
      {
         return true;
      }

      // no matter whos next, the blocking pins may not be too far away
      // - this means (not exactly), the blocking pins must be near
      if (vert == 1)
      {
         return (board.getPin(rx, ry - 1) == Board.XPLAYER ||
               board.getPin(rx, ry - 2) == Board.XPLAYER ||
               board.getPin(rx, ry - 3) == Board.XPLAYER ||
               board.getPin(rx, ry - 4) == Board.XPLAYER);
      }
      else if (col == rx - 2)
      {
         return (board.getPin(rx - 1, ry - 1) == Board.XPLAYER ||
               board.getPin(rx - 1, ry - 2) == Board.XPLAYER ||
               board.getPin(rx - 1, ry - 3) == Board.XPLAYER ||
               board.getPin(rx - 1, ry - 4) == Board.XPLAYER);
      }
      else if (col == rx + 2)
      {
         return (board.getPin(rx + 1, ry - 1) == Board.XPLAYER ||
               board.getPin(rx + 1, ry - 2) == Board.XPLAYER ||
               board.getPin(rx + 1, ry - 3) == Board.XPLAYER ||
               board.getPin(rx + 1, ry - 4) == Board.XPLAYER);
      }

      return false;
      /**/
      //return true;
   }

   /**
    * Check if a pin near the border could be successful.
    * @param col relevant column
    * @param rx x-pos of pin
    * @param ry y-pos of pin
    * @param nextPlayer who's next?
    * @return true, if pin is acceptable
    */
   private boolean checkPlausiBottom(int col, int rx, int ry, int nextPlayer)
   {
      /** vert is the vertical distance to reference-pin. */
      int vert = Math.abs(col - rx);

      // okay, if no vertical distance
      if (vert == 0)
      {
         return true;
      }

      // not okay, if vert to big and other players turn
      if (vert >= 2 && nextPlayer == Board.XPLAYER)
      {
         return false;
      }

      /**/
      // okay, if near border
      if (ry > board.getXsize() - 6)
      {
         return true;
      }

      // no matter whos next, the blocking pins may not be too far away
      // - this means (not exactly), the blocking pins must be near
      if (vert == 1)
      {
         return (board.getPin(rx, ry + 1) == Board.XPLAYER ||
               board.getPin(rx, ry + 2) == Board.XPLAYER ||
               board.getPin(rx, ry + 3) == Board.XPLAYER ||
               board.getPin(rx, ry + 4) == Board.XPLAYER);
      }
      else if (col == rx - 2)
      {
         return (board.getPin(rx - 1, ry + 1) == Board.XPLAYER ||
               board.getPin(rx - 1, ry + 2) == Board.XPLAYER ||
               board.getPin(rx - 1, ry + 3) == Board.XPLAYER ||
               board.getPin(rx - 1, ry + 4) == Board.XPLAYER);
      }
      else if (col == rx + 2)
      {
         return (board.getPin(rx + 1, ry + 1) == Board.XPLAYER ||
               board.getPin(rx + 1, ry + 2) == Board.XPLAYER ||
               board.getPin(rx + 1, ry + 3) == Board.XPLAYER ||
               board.getPin(rx + 1, ry + 4) == Board.XPLAYER);
      }

      return false;
      /**/
      //return true;
   }

}
