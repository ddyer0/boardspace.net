package graphicstest;

import java.awt.Color;

import bridge.Polygon;
import graphicstest.GraphicsViewer.TestAble;
import lib.Graphics;

class Test_3037 implements TestAble
    {	int paints=0;
    	public void runTest(Graphics g,int x,int y,int w,int h)
    	{
    		dtest_3037(g,w,h);
    	}
   
	    public void dtest_3037(Graphics g,int w,int h)
	    {	// this is probably subsumed by #3302
	    	// 5/16/2026 pass simulator ios, fail android
    	if(g!=null)
    	{
    	paints++;
    	Polygon p = new Polygon();
    	p.addPoint(-100, 0);
    	p.addPoint(0, -100);
    	p.addPoint(100, 0);
    	p.addPoint(-100,0);

      	g.setColor(new Color(0x8f8f9f7f));
    	g.fillRect(0,0,w,h);
    	g.setColor(Color.black);
       	g.Text("Paint "+paints,100,100);
       	float amount = ((float)(Math.PI+paints/100.0));
    	g.setRotation(amount,w/2,w/4);

    	g.setColor(Color.white);
       	g.drawRect(w/2-100, h/2-200, 200, 100);
    	g.setColor(Color.black);
    	g.translate(w/2, h/2-100);
    	p.fillPolygon(g);			// triangle should be inscribed in the rectangle    
    	g.setColor(Color.green);
    	p.framePolygon(g);
       	g.translate(-w/2, -(h/2-100));
  	
      		
       	g.setRotation(-amount,w/2,w/4);
    }
    }
    }

