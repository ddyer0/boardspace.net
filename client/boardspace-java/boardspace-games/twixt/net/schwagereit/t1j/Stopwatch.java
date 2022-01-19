package twixt.net.schwagereit.t1j;

/**
 * show elapsed time between start and getTime (based on an idea of Thomas Wilke
 * as found on the net)
 * 
 * @author mail@johannes-schwagereit.de
 */
final class Stopwatch
{
   /** time of start. */
   private long start;

   /**
    * Cons'tor.
    */
   public Stopwatch()
   {
      start = System.currentTimeMillis();
   }

   /**
    * start clock.
    */
   public void start()
   {
      start = System.currentTimeMillis();
   }

   /**
    * getTime since start.
    * 
    * @return time elapsed.
    */
   public long getTime()
   {
      return System.currentTimeMillis() - start;
   }
}