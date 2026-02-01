package lib;

import com.codename1.ui.Font;
import bridge.Color;
import online.game.PlacementProvider;

public interface NumberMenuHost {

    // 
    /**
     * overridable support for NumberMenu drawing functions
     * draw a number (nominally, a move number) associated with a location
     * this draws the numbers just beyond the point of the arrow.
     * 
     * @param gc
     * @param source source location
     * @param dest destination location
     * @param cellSize
     * @param font
     * @param color
     * @param str
     * @param from
     */
	   public default void drawNumber(Graphics gc,PlacementProvider source,PlacementProvider dest,int cellSize,int x,int y,Font font,Color color, String str)
      {	
       	  GC.setFont(gc,font);
       	  GC.drawOutlinedText(gc,true,x-cellSize/2,y-cellSize/2,cellSize,cellSize,color,Color.black,
       			str);
      	
      }
      /**
       * an alternate method to draw a Drawable icon instead of a string  as a number marker.
       * this is used to mark "no number" positions for the most recent move
       * 
       * @param gc
       * @param source
       * @param dest
       * @param cellSize
       * @param x
       * @param y
       * @param font
       * @param color
       * @param str
       */
      public default void drawNumber(Graphics gc,PlacementProvider source,PlacementProvider dest,int cellSize,int x,int y,Font font,Color color, Drawable str)
      {	
    	  GC.setFont(gc,font);
       	  str.drawChip(gc,null,cellSize,x,y,null);
      }

       	  
      /**
       * overridable support for NumberMenu drawing functions
       * draw an arrow from - to
       * @param gc
       * @param src
       * @param dest
       * @param x1
       * @param y1
       * @param x2
       * @param y2
       * @param color
       * @param arrowOpacity
       * @param ticksize
       * @param linew
       */
      public default void drawArrow(Graphics gc,PlacementProvider src,PlacementProvider dest,
    		  int x1,int y1,int x2,int y2,
    		  Color color,double arrowOpacity,
    		  int ticksize,double linew)
      { Color oldColor = GC.getColor(gc);
  	    GC.setColor(gc,color);
		GC.setOpacity(gc,arrowOpacity);		 	
  	 	GC.drawArrow(gc,x1,y1,x2,y2,ticksize,linew);
	 	GC.setColor(gc,oldColor);	// also resets opactity
      }
      
      public default int getLastPlacement() {
			throw G.Error("getLastPlacement must be overriden");
		}

}
