package graphicstest;

import java.awt.Color;

import bridge.Polygon;
import graphicstest.GraphicsViewer.TestAble;
import lib.Graphics;
import lib.Image;

class Dtest_translate implements TestAble
{
	 
	 public double SCALE = 1.5;
	 public void runTest(Graphics gc,int x,int y,int w,int h)
	 {  
	 	gc.setColor(0xff0000);
	 	gc.fillRect(0,0,w,h);
	  	
	 	gc.setColor(0);
	 	gc.translate(100,50);
	 	int ax = gc.getTranslateX();
	 	int ay = gc.getTranslateY();
	  		
	 	gc.drawString("gc.translate(100,50)",100,100);
	 	gc.drawString("gc.getTranslateX() = "+ax,100,150);
	 	gc.drawString("gc.getTranslateY() = "+ay,100,200);
		

        }
int paints=0;
 
public void testFrame(Graphics g,int w,int h,Polygon p,String message)
    {
       	paints++;
     	g.setColor(new Color(0x8f8f9f7f));
    	g.fillRect(0,0,w,h);
    	g.setColor(Color.black);
       	g.Text("Paint "+paints,10,30);
       	g.Text(message, 10, 60);
       	g.drawRect(1,1,144*4,125*4);
       	int cx = w/2;
       	int cy = h/2;
    	int x0 = w/2-20;
    	int y0 = h/2-20;
       	g.setRotation(((float)(Math.PI+paints/100.0)),cx,cy);
         
    	g.setColor(Color.white);
       	g.drawRect(x0-20,y0-20, 40, 20);
       	g.setColor(Color.red);
       	g.fillRect(cx-10, cy-10, 20, 20);
    	g.setColor(Color.black);
    	g.translate(x0,y0);
    	p.fillPolygon(g);			// triangle should be inscribed in the rectangle    
    	g.setColor(Color.green);
    	p.framePolygon(g);
       	g.translate(-x0,-y0);
    	         		
       	g.setRotation(-((float)(Math.PI+paints/100.0)),cx,cy);
       	
       	
      	// draw a grid of larger triangles scaled
       	for(int x=1;x<5;x++)
       	{ for(int y=1;y<5;y++)
       		{	int xp = x*30;
       			int yp = y*30;
       			g.scale(x,y);
       			g.translate(xp,yp);
      			
       	      	g.setColor(Color.white);
               	g.drawRect(-20,-20,40,20);
            	g.setColor(Color.black);
            	p.fillPolygon(g);			// triangle should be inscribed in the rectangle    
            	g.setColor(Color.green);
            	p.framePolygon(g);
                	
      			g.translate(-xp, -yp);
              	g.scale(1.0f/x,1.0f/y);
     			
     			g.setColor(Color.red);
     			g.fillRect(xp*x,yp*y,x*4,y*4);
     			g.setColor(Color.black);
     			g.Text(""+xp+","+yp,xp*x+x*4,yp*y);
       		}
       	}

    }

public void dtest(Graphics realG,boolean buffer,int w,int h)
    {	Polygon p = new Polygon();
    	// 5/16/2026 fail in direct to screen  simulator ios, android
    	float rotation = 0.0f;

    	p = new Polygon();
    	p.addPoint(-20, 0);
    	p.addPoint(0, -20);
    	p.addPoint(20, 0);
    	p.addPoint(-20,0);

     	if(buffer)
    	{
    	Image offscreen = Image.createImage(w,h);
    	Graphics g = offscreen.getGraphics();
    	testFrame(g,w,h,p,"draw to buffer");
    	realG.setRotation(rotation,w/2,h/2);
    	realG.drawImage(offscreen,0,0);
    	realG.setRotation(-rotation,w/2,h/2);
    	}
    	else
    	{	realG.setRotation(rotation,w/2,h/2);
    		testFrame(realG,w,h,p,"draw to screen");
    		realG.setRotation(-rotation,w/2,h/2);
    	}    	

    }
}
