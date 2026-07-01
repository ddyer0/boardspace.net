package graphicstest;

import java.awt.Color;
import java.awt.Shape;

import graphicstest.GraphicsViewer.TestAble;
import lib.Graphics;
import lib.Image;

class test_rotation implements TestAble
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
		gc.setColor(0x1a1a1a);
		gc.fillRect(0,0,w,h);
		gc.setColor(0);
		gc.drawLine(0,0,w,h);
		gc.drawLine(0,w,h,0);
		gc.setColor(0xff00);
		gc.fillRect(0,0,w,4);
		gc.fillRect(0,0,4,h);
		gc.fillRect(0,h-4,w,4);
		gc.fillRect(w-4,0,4,h);
		}
	return pimage;
}
int paints = 0;
public void draw(Graphics gc,int w,int h)
{	
	double imagexscale = 0.24;
	double imageyscale = 0.1;
	int imageW = (int)(w*imagexscale);
	int imageH = (int)(h*imageyscale);
	Image testImage = prepare(null,imageW,imageH); 
	Shape originalClip = gc.getClip();
	{
	gc.setColor(Color.lightGray);
	gc.fillRect(0,0,w,h);
	gc.setClip(w/4,h/5,w/2,h/2);
	gc.setColor(Color.blue);
	gc.fillRect(0,0,w,h);
	int steps = 20;
	for(int i=0;i<steps;i++)
	{
		for(int j=0;j<steps; j++)
		{
			int xp =i*w/steps;
			int yp =j*h/steps;
			double r = Math.PI/3;
			gc.setRotation(r,xp,yp);
			gc.setColor(Color.red);
			gc.setOpacity(1);
			int aw = w/steps/2;
			int ah = h/steps/2;
			//gc.fillRect(xp,yp,aw,ah);
			gc.drawImage(testImage,xp,yp,aw,ah);
			gc.setRotation(-r,xp,yp);
			gc.setColor(Color.green);
			//gc.setOpacity(0.01);
			//gc.fillRect(0,0,w,h);
			gc.setOpacity(1);
		}
	}
	gc.setClip(originalClip);

}}}


