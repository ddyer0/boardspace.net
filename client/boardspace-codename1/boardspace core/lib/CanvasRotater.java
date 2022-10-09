package lib;

import com.codename1.ui.geom.Rectangle;

public class CanvasRotater implements CanvasRotaterProtocol
{
	SizeProvider canvas;
	private int canvasRotation=0;
	public  int getCanvasRotation() { return(canvasRotation); }
	public  void setCanvasRotation(int n) {
		canvasRotation = n&3;
    }
	public CanvasRotater(SizeProvider c) { canvas = c; }
	public boolean quarterTurn()
	{
		return (getCanvasRotation()&1)!=0;
	}
	public Rectangle getRotatedBounds() 
	{ int w = canvas.getWidth();
	  int h = canvas.getHeight();
	  boolean qt = quarterTurn();
	  return new Rectangle(canvas.getX(),canvas.getY(),qt ? h : w,qt ? w : h);
	}
	
	public boolean rotateCanvas(Graphics g)
	{	
		if(g==null) { return false; }
		G.Assert(!g._rotated_,"already rotated");
		
		int rot = getCanvasRotation();
		if(rot==0) { g._rotated_ = false; return false; }
		int w = canvas.getWidth()/2;
		int h = canvas.getHeight()/2;
		double r = rot*Math.PI/2;
		switch(rot)
		{
		case 2: 
			GC.setRotation(g,r);
			GC.translate(g,-w*2,-h*2);
			break;
		case 1:
			{
			int wh = (w-h);
			int dx=  wh/2;
			int cx = h+dx;
			int cy = w-dx;			
			GC.translate(g,cx, cy);
		    GC.setRotation(g,r);
		    GC.translate(g,-cx, -cy-wh);
			}
			break;
		case 3:
			{	
			int wh=  (w-h);
			int dx = wh/2;
			int cx = h+dx;
			int cy = w-dx;
			GC.translate(g,cx,cy);
			GC.setRotation(g,r);
			GC.translate(g,wh-cx,-cy);
			}
			break;
		}
		g._rotated_ = true;
		return true;
	}

	public void unrotateCanvas(Graphics g)
	{	//System.out.println("un "+client+" "+g);
		if(g==null) { return; }
		G.Assert(g._rotated_,"not the rotated");
		g._rotated_=false;
		int rot = getCanvasRotation();
		if(rot==0) { return; }
		int w = canvas.getWidth()/2;
		int h = canvas.getHeight()/2;
		double r = -rot*Math.PI/2;
		switch(rot)
		{
		case 2: 
			GC.setRotation(g,r,w,h);
			break;
		case 1:
			{
			int wh =  (w-h);
			int dx = wh/2;
			int cx = h+dx;
			int cy = w-dx;
			GC.translate(g,cx,cy+wh);
			GC.setRotation(g,r);
			GC.translate(g,-cx,-cy);
			}
			break;
		case 3:
			{	
			int wh =  (w-h);
			int dx = wh/2;
			int cx = h+dx;
			int cy = w-dx;
			GC.translate(g,cx-wh,cy);
			GC.setRotation(g,r);
			GC.translate(g,-cx,-cy);
			}
			break;
		}
	}

	public int rotateCanvasX(int x, int y)
	{
		switch(getCanvasRotation())
		{
		default:
		case 0:	return x;
		case 2: return canvas.getWidth()-x;
		case 3: return canvas.getHeight()-y;
		case 1: return y;
		}
	}

	public int rotateCanvasY(int x, int y) 
	{
		switch(getCanvasRotation())
		{
		default:
		case 0:	return y;
		case 2: return canvas.getHeight()-y;
		case 3: return x;
		case 1: return canvas.getWidth()-x;
		}
	}

	public int unrotateCanvasX(int x, int y)
	{
		switch(getCanvasRotation())
		{
		default:
		case 0:	return x;
		case 2: return canvas.getWidth()-x;
		case 1: return canvas.getWidth()-y;
		case 3: return y;
		}
	}
	public int unrotateCanvasY(int x, int y) 
	{
		switch(getCanvasRotation())
		{
		default:
		case 0:	return y;
		case 2: return canvas.getHeight()-y;
		case 1: return x;
		case 3: return canvas.getHeight()-x;
		}
	}

}
