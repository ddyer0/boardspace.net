package graphicstest;

import java.awt.Color;
import java.awt.Shape;
import java.util.Vector;


import bridge.Polygon;
import graphicstest.GraphicsViewer.TestAble;
import lib.Graphics;
import lib.Image;

class test_3921 implements TestAble
{
public void runTest(Graphics gc,int x,int y,int w,int h)
{
	draw_3921(gc,w,h,(GraphicsViewer.step&8)!=0,(GraphicsViewer.step&4)!=0,(GraphicsViewer.step&2)!=0,(GraphicsViewer.step&1)!=0);
}

Image test = Image.createImage(100,100);
boolean prepared_3921;
public void prepare_3921()
{	prepared_3921 = true;
	Graphics gc = test.getGraphics();
	gc.setColor(Color.blue);
	gc.fillRect(0,0,100,100);
	gc.setColor(Color.black);
	gc.drawLine(0,0,100,100);
	gc.drawLine(0,100,100,0);
	gc.setColor(Color.green);
	gc.fillRect(0,0,100,4);
	gc.fillRect(0,0,4,100);
	gc.fillRect(0,96,100,4);
	gc.fillRect(96,0,4,100);
}
public void draw_3921(Graphics gc,int w,int h,boolean prescale,boolean globalClip,boolean localClip,boolean rotate)
{	
	if(!prepared_3921) {  prepare_3921(); }
	gc.setColor(new Color(0xfff0f0f));
	gc.fillRect(0,0,w,h);
	Vector<Polygon>images = new Vector<Polygon>();
	float scale = 2.5f;
	int drawW = 60;
	int drawH = 50;
	int tx = -134;
	int ty = -399;
	int cx = w/6;
	int cy = h/4;
	int cw = w/2;
	int ch = h/2;
		
	gc.setColor(Color.black);
	gc.fillRect(w/10,h/10,w/2,h/10);
	gc.setColor(Color.white);
	gc.Text("precale "+prescale+" globalclip "+globalClip+" localClip "+localClip+" rotate "+rotate,w/10+w/20,h/10+h/20);
	if(globalClip)
		{
		gc.setColor(new Color(0xf000));
		gc.drawRect(cx-1,cy-1,cw+2,ch+2);
		gc.drawRect(cx-3,cy-3,cw+6,ch+6);
		gc.setColor(Color.black);
		gc.setClip(cx,cy,cw,ch);
		}
	if(prescale)
	{
	gc.scale(scale,scale);
	gc.translate(tx,ty);
	}
	for(int xp=0;xp<w;xp+=120)
				for(int yp=0;yp<h;yp+=120)
	{	
		//gc.drawLine(0,0,i,i);
		Shape clip = gc.getClip();
		
		if(rotate) { gc.setRotation((float)Math.PI/3,xp,yp); }
		if(localClip) { gc.clipRect(xp+2,yp+2,56,56); }
		Polygon p = new Polygon();
		p.addPoint(gc.transform(xp,yp));
		p.addPoint(gc.transform(xp+drawW,yp));
		p.addPoint(gc.transform(xp+drawW,yp+drawH));
		p.addPoint(gc.transform(xp,yp+drawH));
		p.addPoint(gc.transform(xp,yp));
		images.addElement(p);
		gc.drawImage(test,xp,yp,drawW,drawH);
		// this is the actual point of failure.  In this context,
		// getclip + setclip is not idempotent
		if(rotate) { gc.setRotation(-(float)Math.PI/3,xp,yp); }
		//gc.setClip(clip);
		gc.setClip(clip);
	}
	if(prescale)
	{
	gc.translate(-tx,-ty);
	gc.scale(1/scale,1/scale);
	}
	if(globalClip) { gc.setClip(0,0,w,h); }
	for(int i=0;i<images.size();i++)
	{
		Polygon p = images.elementAt(i);
		gc.setColor(Color.black);
		p.framePolygon(gc);
		
	}
}


}
