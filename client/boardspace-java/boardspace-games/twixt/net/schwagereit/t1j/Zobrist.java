/**
 * 
 * Created by IntelliJ IDEA.
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
package twixt.net.schwagereit.t1j;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;

/**
 * This class implements Zobrist hashing.
 */
public class Zobrist
{

   final private class Node
   {
      private int valueX; //value if pin set by XPlayer
      private int valueY; //value if pin set by YPlayer

      private final int[] bridge = new int[4];
   }


   private static final Zobrist ourInstance = new Zobrist();

   private final Node[][] field = new Node[Board.MAXDIM][Board.MAXDIM];

   private final Random rand = new Random();

   /** Set of already drawn numbers - avoid duplicates. */
   private final Set<Object> alreadyDrawn = new HashSet<Object>(700);

   /**
    * Return the Zobrist-Object.
    *
    * @return Zobrist-Object
    */
   public static Zobrist getInstance()
   {
      return ourInstance;
   }

   /**
    * Constructor - no external instance.
    */
   private Zobrist()
   {
   }

   /**
    * Initialize the data structure.
    */
   public final void initialize()
   {
      for (int j = 0; j < field.length; j++)
      {
         for (int i = 0; i < field.length; i++)
         {
            fill(i,j);
         }
      }
      //never needed again
      alreadyDrawn.clear();

   }

   /**
    * Fill one of the positions.
    * @param i x
    * @param j y
    */
   private void fill(int i, int j)
   {
      Node node = new Node();
      node.valueX = getRandomInt();
      node.valueY = getRandomInt();
      node.bridge[0] = getRandomInt();
      node.bridge[1] = getRandomInt();
      node.bridge[2] = getRandomInt();
      node.bridge[3] = getRandomInt();
      field[i][j] = node;
   }

   /**
    * Get a random number. Avoid duplicates.
    * @return int
    */
   private int getRandomInt()
   {
      int val;
      do
      {
         val = rand.nextInt();
      } while (alreadyDrawn.contains(val));

      alreadyDrawn.add(val);

      return val;
   }

   /**
    * Return the value for a pin set or removed.
    * @param x x
    * @param y y
    * @param player player
    * @return int
    */
   public int getPinValue(int x, int y, int player)
   {
      if (player == Board.XPLAYER)
      {
         return field[x][y].valueX;
      }
      else
      {
         return field[x][y].valueY;
      }
   }
   /**
    * Return the value for a link set or removed.
    * @param x x
    * @param y y
    * @param link no of link (0-3)
    * @return int
    */
   public int getLinkValue(int x, int y, int link)
   {
      return field[x][y].bridge[link];
   }
}
