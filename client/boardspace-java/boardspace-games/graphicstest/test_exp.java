package graphicstest;

import java.awt.Color;
import java.awt.Rectangle;

import graphicstest.GraphicsViewer.TestAble;
import lib.G;
import lib.Graphics;
import lib.Image;

class test_exp implements TestAble
{	static Image background = null;
	public void init(int w,int h,int pass)
	 {
		   	background = Image.createImage(w,h);
		 
	    	Graphics g = background.getGraphics();
	    	for(int x=0;x<w; x+= w/2+1)
	    	{
	    		for (int y=0; y<h; y+=h/2+1)
	    		{
	    			g.setColor(new Color(0xf00*pass*2+x+y));
	    	    	g.fillRect(x,y,w/2+1,h/2+1);
	    		}
	    	}
	    	g.setColor(Color.white);
	    	g.drawLine(0,0,w,h);
	    	g.drawLine(0,h,w,0);
	    
		 }
	  		
	 static int paints = 1;
	 public void runTest(Graphics gc,int x,int y,int w,int h)
	 {  
	 	init(w,h,paints++);
	 	gc.setColor(new Color(0xff0000));
	 	gc.fillRect(x,y,w,h);
	 	gc.translate(-1164,-764);
	 	@SuppressWarnings("unused")
		Rectangle cclip = (Rectangle)gc.getClip();
	 	gc.drawImage(background,-12,-4,4329,3050);
	 	gc.translate(1164,764);
	 	gc.setColor(new Color(0xffffff));
	 	gc.fillRect(20,20,400,200);
	 	gc.setColor(new Color(0));
	 	gc.Text("paint "+paints,100,50);
	 	gc.Text("after clip "+G.Left(cclip)+","+G.Top(cclip)+" "+G.Width(cclip)+"x"+G.Height(cclip),100,150);
	 }

}
