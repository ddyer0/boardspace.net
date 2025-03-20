package lib;

import com.codename1.ui.geom.Rectangle;

import lib.exCanvas.OnlineId;
/**
 * this is a generic helper class to provide "zoom up" magnifiers on windows that already have
 * a global pan/zoom capability.   The only interfaces are the "drawMagnifier" call, which 
 * presents a magnifier, and a hook in SetLocalBounds to call zoomer.reCenter();
 */
public class Zoomer 
{
	exCanvas canvas;
    Rectangle currentZoomRect = null;
    Rectangle centerOnBox = null;
    int centerOnBoxRotation = 0;

    public Zoomer(exCanvas v)
	{
		canvas = v;
	}
	
    /**
     * draw a magnifier that will be handled automatically.
     * @param gc	// graphics for drawing
     * @param hp	// hit point
     * @param r		// the rectangle to be zoomed to
     * @param siz	// magnifier size as a fraction of the box width
     * @param posx	// magnifier position as a fraction of the box width
     * @param posy	// magnifier position as a fraction of the box height
     * @param rot
     */
    public boolean drawMagnifier(Graphics gc,HitPoint hp,Rectangle r,double siz, double posx,double posy,int rot)
    {	
    	if(canvas.getGlobalZoom()<=1.0) { currentZoomRect = null; }
    	return drawMagnifier(gc,hp,r,r==currentZoomRect,siz,posx,posy,rot);
    }
    public boolean drawMagnifier(Graphics gc,HitPoint hp,Rectangle r,boolean unmagnify,double siz, double posx,double posy,int rot)
    {
    	DrawableImage<?> icon = unmagnify ? StockArt.UnMagnifier : StockArt.Magnifier;
    	int size = (int)(G.Width(r)*siz);
    	int xp = G.Left(r)+(int)(posx*G.Width(r));
    	int yp = G.Top(r)+(int)(posy*G.Height(r));
    	if(icon.drawChip(gc,canvas,size,xp,yp,hp,OnlineId.Magnifier,null))
    	{
    		hp.hitData = r;
    		hp.hit_index = rot;
    		return true;
    	}
    	return false;
    }
  
    public void drawMagnifier(Graphics gc,HitPoint hp,Rectangle r,double siz, double posx,double posy,double rot)
    {
    	drawMagnifier(gc,hp,r,siz,posx,posy,G.rotationQuarterTurns(rot));
    }
    /**
     * actually do the calculations and set the new global zoom
     * @param box
     * @param qt
     */
    public void setGlobalMagnifier(Rectangle box,int qt)
    {	if(canvas.getGlobalZoom()<=1) { currentZoomRect = null; }
    	if(box==null) { currentZoomRect=null; canvas.setGlobalZoom(1,0);} 
    	else if(!box.equals(currentZoomRect))
    	{
    	currentZoomRect = box;
		boolean swap = (qt&1)!=0;
		int w = G.Width(box);
		int h = G.Height(box);
		
		double fullh = G.Height(canvas.fullRect);
		double fullw = G.Width(canvas.fullRect);
		double expand = 0.9;
		double hscale = fullh/((swap?w:h));
		double wscale = fullw/((swap?h:w));
		double ratio = expand*Math.min(wscale, hscale);
		canvas.setGlobalZoom(ratio,0);
		centerOnBox = box;
		centerOnBoxRotation = 1;
		canvas.resetBounds();
    	}
    }
    /**
     * call from the end of setLocalBounds
     */
    public void reCenter()
    {
      	if(centerOnBox!=null) 
		{ Rectangle r = G.copy(null,centerOnBox);
		  centerOnBox = null; 
		  G.setRotation(r,centerOnBoxRotation*Math.PI/2);
		  canvas.centerOnBox(r); 
		}
    }
   /**
    * handle hits on the magnifiers
    * @param id
    * @param hp
    * @return
    */
    public boolean performStandardButtons(CellId id,HitPoint hp)
    {
    	if(id==OnlineId.Magnifier)
		{
			Rectangle r = (Rectangle)hp.hitData;
    		if(r.equals(currentZoomRect)) { currentZoomRect = null; canvas.setGlobalZoom(0,0); }
    		else { setGlobalMagnifier(r,hp.hit_index); }
    		return true;
		}
    	return false;
    }
}