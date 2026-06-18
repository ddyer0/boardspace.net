package graphicstest;

import java.util.Vector;

import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.Polygon;
import graphicstest.GraphicsViewer.TestAble;
import lib.G;
import lib.Graphics;
import lib.Image;

class test_scale implements TestAble
{		
	public void runTest(Graphics gc,int x,int y,int w,int h)
	{
		draw(gc,w,h);
	}

Image htest = null;
Image vtest = null;
public Image prepare(Image pimage,int w,int h)
{	if(pimage==null || pimage.getWidth()!=w || pimage.getHeight()!=h)
		{
		pimage = Image.createImage(w,h);
		Graphics gc = pimage.getGraphics();
		gc.setColor(Color.lightGray);
		gc.fillRect(0,0,w,h);
		gc.setColor(Color.black);
		gc.drawLine(0,0,w,h);
		gc.drawLine(0,w,h,0);
		gc.setColor(Color.green);
		gc.fillRect(0,0,w,4);
		gc.fillRect(0,0,4,h);
		gc.fillRect(0,h-4,w,4);
		gc.fillRect(w-4,0,4,h);
		}
	return pimage;
}
private Polygon makePoly(Graphics gc,int xp,int yp,int drawW,int drawH)
{
	Polygon p = new Polygon();
	p.addPoint(gc.transform(xp,yp));
	p.addPoint(gc.transform(xp+drawW,yp));
	p.addPoint(gc.transform(xp+drawW,yp+drawH));
	p.addPoint(gc.transform(xp,yp+drawH));
	p.addPoint(gc.transform(xp,yp));
	return p;
}
public void draw(Graphics gc,int w,int h)
{	
	double imagexscale = 0.04;
	double imageyscale = 0.03;
	int imageW = (int)(w*imagexscale);
	int imageH = (int)(h*imageyscale);
	Image testImage = prepare(null,imageW,imageH); 
	gc.setColor(new Color(0xa000));
	gc.fillRect(0,0,w,h);
	Vector<Polygon>images = new Vector<Polygon>();
	float scale = 1.2f;//2.5f;
	int xp = 100;
	int yp = 100;
	int step = 100;
	
	int initial_x = 100;
	int initial_y = 50;
	gc.translate(initial_x,initial_y);

	gc.setColor(Color.white);
	gc.drawLine(0,xp,w,xp);
	gc.drawLine(xp,0,xp,h);
	gc.drawLine(xp+step,0,xp+step,h);
	gc.drawLine(0,yp+step,w,yp+step);
	gc.setColor(new Color(0xffa0a0));
	gc.drawLine((int)((xp+step)*scale),0,(int)((xp+step)*scale),h);
	gc.drawLine(0,(int)((yp+step)*scale),w,(int)((yp+step)*scale));
	gc.drawLine((int)((xp+step*2)*scale),0,(int)((xp+step*2)*scale),h);
	gc.drawLine(0,(int)((yp+step*2)*scale),w,(int)((yp+step*2)*scale));
	
	int drawW = testImage.getWidth();
	int drawH = testImage.getHeight();


	images.addElement(makePoly(gc,xp,yp,drawW,drawH));
	gc.drawImage(testImage,xp,yp,drawW,drawH); 
			
	
	gc.scale(scale,1);
	images.addElement(makePoly(gc,xp+step,yp,drawW,drawH));
	gc.drawImage(testImage,xp+step,yp,drawW,drawH); 
	gc.setColor(new Color(0xa0));
	gc.fillRect(xp+step*2,yp,drawW,drawH);
	gc.scale(1/scale,1);
	
	gc.scale(1,scale);
	images.addElement(makePoly(gc,xp,yp+step,drawW,drawH));
	gc.drawImage(testImage,xp,yp+step,drawW,drawH); 
	gc.setColor(new Color(0xa0));
	gc.fillRect(xp,yp+step*2,drawW,drawH);
	gc.scale(1,1/scale);
	
	gc.scale(scale,scale);
	images.addElement(makePoly(gc,xp+step,yp+step,drawW,drawH));
	gc.drawImage(testImage,xp+step,yp+step,drawW,drawH); 
	gc.setColor(new Color(0xa0));
	gc.fillRect(xp+step*2,yp+step*2,drawW,drawH);
	gc.scale(1/scale,1/scale);
	
	
	for(int i=0;i<images.size();i++)
	{
		Polygon p = images.elementAt(i);
		gc.setColor(Color.black);
		p.framePolygon(gc);
		
	}
	
	gc.translate(-initial_x,-initial_y);


}


}
