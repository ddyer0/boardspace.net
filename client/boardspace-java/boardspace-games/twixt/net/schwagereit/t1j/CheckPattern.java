/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package twixt.net.schwagereit.t1j;

import java.io.*;
import java.util.*;

/**
 * All definition and methods to load and check patterns.
 * Patterns are used to find better defensive or offensive moves.
 *
 * Created by IntelliJ IDEA.
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
public final class CheckPattern
{
   private BufferedReader reader;
//   private static final String FILENAME = "patterns.properties";
   private static final String FILENAME = "patterns.properties";

   private static final String OFFENSIVE = "OFF"; //use uppercase
   private static final String DEFENSIVE = "DEF"; //use uppercase

   private static final String FREE_STR  = "FREE";    // Free    2 free pin
   private static final String OWN_STR = "OWN";       // Own     2 own pin
   private static final String OPP_STR = "OPP";       // Opp     2 opponent pin
   private static final String SET_STR = "SET";       // Set     2 Set pin (if allowed and free)
   private static final String BPOSS_STR = "BPOSS";   // BPoss   4 Bridge possible?
   private static final String BEXIST_STR = "BEXIST"; // BExist  4 Bridge exists?
   private static final String STRONG_STR = "STRONG"; // Strong  2 Pin is strong (Connected)
   private static final String NSTRONG_STR = "NSTRONG";// NStrong 2 Pin is not strong

   private static final int FREE_NR  = 0;  // Free   2 free pin
   private static final int OWN_NR = 1;    // Own    2 own pin
   private static final int OPP_NR = 2;    // Opp    2 opponent pin
   private static final int SET_NR = 3;    // Set    2 Set pin (if allowed and free)
   private static final int BPOSS_NR = 4;  // BPoss  4 Bridge possible?
   private static final int BEXIST_NR = 5; // BExist 4 Bridge exists?
   private static final int STRONG_NR = 6; // Strong 2
   private static final int NSTRONG_NR = 7;// Not Strong? 2

   private final List<Pattern> offensivePatterns = new LinkedList<Pattern>();
   private final List<Pattern> defensivePatterns = new LinkedList<Pattern>();
   private Set<Move> moves;

   /**
    * A Pattern consists of several PatternElements.
    * A PatternElement defines a condition (e.g. a required own pin)
    */
   public static final class PatternElement
   {
      final static Map<String, Integer> checks = new HashMap<String,Integer>();
      int condition;
      int x1, x2, y1, y2;

      /**
       * Fill the HashMap with all known conditions.
       */
      static void fillChecks()
      {
         checks.put(FREE_STR, FREE_NR);
         checks.put(OWN_STR, OWN_NR);
         checks.put(OPP_STR, OPP_NR);
         checks.put(SET_STR, SET_NR);
         checks.put(BPOSS_STR, BPOSS_NR);
         checks.put(BEXIST_STR, BEXIST_NR);
         checks.put(STRONG_STR, STRONG_NR);
         checks.put(NSTRONG_STR, NSTRONG_NR);
      }

      /**
       * Cons'tor.
       * @param input String which contains condition
       */
      PatternElement(String input)
      {
         String[] tok = input.split(" +"); // use 1 or more blanks as delimiter

         if (checks.containsKey(tok[0]))
         {
            int checkNr = ((Integer) checks.get(tok[0])).intValue();
            int argNr = (checkNr == BPOSS_NR || checkNr == BEXIST_NR) ? 4 : 2;

            condition = checkNr;
            // check Number of arguments
            if (tok.length - 1 != argNr)
            {
               throw new IllegalArgumentException(
                     "Illegal syntax: '" + tok[0] + "' requires " + argNr + " arguments, but " +
                           (tok.length - 1) + " were given.");
            }

            if (argNr >= 1)
            {
               x1 = Integer.parseInt(tok[1]);
            }
            if (argNr >= 2)
            {
               y1 = Integer.parseInt(tok[2]);
            }
            if (argNr >= 3)
            {
               x2 = Integer.parseInt(tok[3]);
            }
            if (argNr >= 4)
            {
               y2 = Integer.parseInt(tok[4]);
               // if 4 arguments, check correct distance
               if (Math.abs(x1 - x2) + Math.abs(y1 - y2) != 3)
               {
                  throw new IllegalArgumentException(
                        "Illegal syntax: Bridge has incorrect distance.");

               }
            }
         }
         else
         {
            throw new IllegalArgumentException(
                  "Illegal syntax: '" + tok[0] + "' is no legal operator");

         }
      }

      /**
       * Cons'tor for swapped copy.
       * Nota bene: Copy is mirrored!
       * @param source Element to copy
       */
      PatternElement(PatternElement source)
      {
         this.condition = source.condition;
         this.x1 = -source.x1;
         this.y1 = source.y1;
         this.x2 = -source.x2;
         this.y2 = source.y2;
      }

      /**
       * Print object as String.
       * @return Representation as String
       */
      public String toString()
      {
         return "" + condition + ": " + x1 + ", " + y1 + ", " + x2 + ", " + y2;
      }
   }

   /**
    * A Pattern contains all the conditions which must be matched to fire.
    */
   public static final class Pattern
   {
      /** Name. */
      final String name;
      /** List of PatternElement. */
      final List<PatternElement> elements;
      /** Is pattern symmetric, i.e. it is not mirrored */
      final boolean symmetric;

      /**
       * Cons'tor.
       * @param text name of pattern
       * @param sym Is pattern symmetric
       */
      Pattern(String text, boolean sym)
      {
         name = text;
         elements = new LinkedList<PatternElement>();
         symmetric = sym;
      }

      /**
       * Cons'tor to create copy with mirrored elements.
       * @param source Pattern to copy
       */
      Pattern(Pattern source)
      {
         name = source.name + " mirrored";
         symmetric = source.symmetric;
         elements = new LinkedList<PatternElement>();
         for (Iterator<PatternElement> iterator = source.elements.iterator(); iterator.hasNext();)
         {
            PatternElement patternElement = (PatternElement) iterator.next();
            // Elements are mirrored in cons'tor
            this.elements.add(new PatternElement(patternElement));
         }
      }

      /**
       * Add Element to pattern.
       * @param patternElement pattern to addElement
       */
      public void addElement(PatternElement patternElement)
      {
         elements.add(patternElement);
      }

      /**
       * Print object as String.
       * @return Representation as String
       */
      public String toString()
      {
         String ln = System.getProperty("line.separator");
         StringBuffer ret = new StringBuffer().append(name).append(":");
         for (Iterator<PatternElement> iterator = elements.iterator(); iterator.hasNext();)
         {
            PatternElement patternElement = (PatternElement) iterator.next();
            ret = ret.append(ln).append("   ").append(patternElement);
         }
         return ret.toString();
      }

      /**
       * Check all the Conditions of this pattern.
       * @param critPos The critical position (peg plus direction)
       * @param board the board
       * @param player the next player
       * @param offense is this move offensive or defensive?
       */
      private void checkPatternElements(Evaluation.CritPos critPos, Board board, int player,
                                        boolean offense)
      {
         int px = -1; //reset move-coordinates
         int py = -1;
         int hx, hy, ix, iy; // helping vars
         boolean okay = true;

         for (Iterator<PatternElement> iterator = elements.iterator(); iterator.hasNext();)
         {
            PatternElement element = (PatternElement) iterator.next();
            hx = critPos.getX() + element.x1;
            hy = critPos.getY() + element.y1 * (critPos.isDir() ? 1 : -1);
            ix = critPos.getX() + element.x2;
            iy = critPos.getY() + element.y2 * (critPos.isDir() ? 1 : -1);

            if (hx < 0 || hy < 0 || hy > board.getYsize() || hx > board.getXsize())
            {
               okay = false;
               break;
            }
            //System.out.println("Check " + element.condition + "-" + new Move(hx, hy));
            switch (element.condition)
            {
               case FREE_NR:
                  okay = board.getPin(hx, hy) == 0;
                  break;
               case OWN_NR:
                  okay = board.getPin(hx, hy) == (offense ? Board.YPLAYER : Board.XPLAYER);
                  break;
               case OPP_NR:
                  okay = board.getPin(hx, hy) == (offense ? Board.XPLAYER : Board.YPLAYER);
                  break;
               case SET_NR:
                  px = hx;
                  py = hy;
                  okay = board.pinAllowed(px, py, (offense ? Board.YPLAYER : Board.XPLAYER));
                  break;
               case BPOSS_NR:
                  okay = board.isBridgeAllowed(hx, hy, ix, iy);
                  break;
               case BEXIST_NR:
                  okay = board.isConnected(hx, hy, ix, iy);
                  break;
               case STRONG_NR:
                  okay = board.isStrong(hx, hy);
                  break;
               case NSTRONG_NR:
                  okay = !board.isStrong(hx, hy);
                  break;
               default:
                  //should never happen
                  System.out.println("Error: " + element.condition + " not handled in switch.");
            }
            if (!okay)
            {
               break; // break checking elements
            }
         } //for elements

         //System.out.println("pattern: " + name + "-" + okay + " to " + critPos);
         if (okay && px >= 0)
         {
            if (offense)
            {
               CheckPattern.getInstance().moves.add(new Move(px, py, player));
            }
            else
            {
               //swap coordinates for defensive move
               CheckPattern.getInstance().moves.add(new Move(py, px, player));
            }
         }
      }

   } // inner class pattern


   private static final CheckPattern ourInstance = new CheckPattern();

   /**
    * Return the CheckPattern-Object.
    *
    * @return CheckPattern-Object
    */
   public static CheckPattern getInstance()
   {
      return ourInstance;
   }

   /**
    * Constructor - no external instance.
    */
   private CheckPattern()
   {
   }

   /**
    * Return first word in string. Separator is space.
    * @param input String to analyse
    * @return Word
    */
   static private String getFirstWord (String input)
   {
      return input.split(" +")[0]; // use 1 or more blanks as delimiter
   }

   /**
    * Read a line from the file if possible.
    * @return String or null, if nothing found
    */
   private String getLine()
   {
      String line;
      try
      {
         while ((line = reader.readLine()) != null)
         {
            int pos = line.indexOf("#");
            if (pos >= 0)
            {
               line = line.substring(0, pos);
            }
            line = line.trim();
            if (line.length() > 0)
            {
               return line;
            }
         }
      }
      catch (IOException e)
      {
         System.out.println("IO-Error reading file '" + FILENAME + "'.");
      }
      do
      {

      } while (false);
      return null;
   }

   /**
    * Load all the patterns from file.
    */
   public final void loadPattern()
   {
      String line;
      Pattern currPattern = null;

      PatternElement.fillChecks();
      InputStream stream = getClass().getResourceAsStream(FILENAME);

      if (stream == null)
      {
         System.out.println("Can't read resource '" + FILENAME + "'");
         return;
      }

      try
      {
         reader = new BufferedReader(new InputStreamReader(stream));

         while ((line = getLine()) != null)
         {
            line = line.toUpperCase();
            String start = getFirstWord(line);
            if (start.startsWith(OFFENSIVE))
            {
               //new offensive pattern
               currPattern = new Pattern(line.substring(3).trim(), start.endsWith("S"));
               offensivePatterns.add(currPattern);
            } else if (start.startsWith(DEFENSIVE))
            {
               //new defensive pattern
               currPattern = new Pattern(line.substring(3).trim(), start.endsWith("S"));
               defensivePatterns.add(currPattern);
            }
            else
            {
               if (currPattern == null)
               {
                  throw new IllegalArgumentException(
                        "Illegal syntax: Line '" + line + "' in '" + FILENAME +
                              "' doesn't belong to any pattern");
               }
               else
               {
                  // addElement line to current pattern
                  currPattern.addElement(new PatternElement(line.trim()));
                }
            }

         }

         reader.close();

         //mirror non-symmetric patterns
         List<Pattern> mirr = new LinkedList<Pattern>();
         for (Iterator<Pattern> iterator = offensivePatterns.iterator(); iterator.hasNext();)
         {
            Pattern pattern = (Pattern) iterator.next();
            if (!pattern.symmetric)
            {
               mirr.add(new Pattern(pattern));
            }
         }
         offensivePatterns.addAll(mirr);

         mirr = new LinkedList<Pattern>();
         for (Iterator<Pattern> iterator = defensivePatterns.iterator(); iterator.hasNext();)
         {
            Pattern pattern = (Pattern) iterator.next();
            if (!pattern.symmetric)
            {
               mirr.add(new Pattern(pattern));
            }
         }
         defensivePatterns.addAll(mirr);

      } catch (FileNotFoundException e)
      {
         System.out.println("File '" + FILENAME + "' not found.");
      }
      catch (IOException e)
      {
         System.out.println("IO-Error reading file '" + FILENAME + "'.");
      }

   }

   /**
    * Find all moves for one position resulting from patterns.
    * @param offense Is it offensive or defensive?
    * @param board The board
    * @param critPos the relevant critical pin
    * @param player Player for next move
    * @return a set of moves
    */
   public Set<Move> findPatternMoves(boolean offense, Board board, Evaluation.CritPos critPos,
                               int player)
   {
      moves = new HashSet<Move>();

      List<?> patterns = offense ? offensivePatterns : defensivePatterns;
      for (Iterator<?> iterator = patterns.iterator(); iterator.hasNext();)
      {
         Pattern pattern = (Pattern) iterator.next();
         pattern.checkPatternElements(critPos, board, player, offense);
      } // for Patterns
      return moves;
   }
}
