package graphicstest;

import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;

import bridge.Color;
import graphicstest.GraphicsViewer.TestAble;
import lib.Graphics;
import lib.Image;

class test_5270 implements TestAble
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
	double rot = Math.PI/2;
	int x = w/10;
	int margin = 1;
	//for(int i=0;i<3;i++)
	{
	paints++;
	Rectangle clip = gc.getClipBounds();
	{
	int y2 = h/4;
	gc.setRotation(rot,x+imageW/2,y2+imageH/2);
	gc.clipRect(x+margin,y2+margin,imageW-margin*2,imageH-margin*2);
	gc.drawImage(testImage,x,y2,imageW,imageH);
	gc.setClip(clip);
	gc.setRotation(-rot,x+imageW/2,y2+imageH/2);
	}
	{
	int y = h-h/4;
	gc.setRotation(rot,x+imageW/2,y+imageH/2);
	gc.clipRect(x+margin,y+margin,imageW-margin*2,imageH-margin*2);
	gc.drawImage(testImage,x,y,imageW,imageH);
	gc.setClip(clip);
	gc.setRotation(-rot,x+imageW/2,y+imageH/2);
	}
	
	gc.setColor(0xff0000);
	gc.fillRect(w/2,h/2,w/3,h/3);
	gc.setColor(0xffff00);
	gc.fillRect(w/3,h/10,w/4,h-h/2);

	}}
	gc.setClip(originalClip);


}


}
