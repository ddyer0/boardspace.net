package graphicstest;

import java.awt.Color;

import graphicstest.GraphicsViewer.TestAble;
import lib.Graphics;
import lib.Image;


class Test_4914 implements TestAble
{	int size = 150;
	Image test;
	
	Test_4914()
	{
		prepare();
	}
	
	public void runTest(Graphics gc,int x,int y,int w,int h)
	{
	testFrame(gc,x,y,w,h);
	}

	public void prepare()
	{
	test = Image.createImage(size,size);
	Graphics gc = test.getGraphics();
	gc.setColor(Color.blue);
	gc.fillRect(0,0,size,size);
	gc.setColor(Color.black);
	gc.drawLine(0,0,size,size);
	gc.drawLine(0,size,size,0);
	gc.setColor(Color.green);
	gc.fillRect(0,0,size,4);
	gc.fillRect(0,0,4,size);
	gc.fillRect(0,size-4,size,4);
	gc.fillRect(size-4,0,4,size);
	}
	
	
	int paints = 0;
	public void testFrame(Graphics g,int x,int y,int w,int h)
	{	
		paints++;
		g.setColor(new Color(0x8f8f9f7f));
		g.fillRect(x,y,w,h);
		g.setColor(Color.black);
		g.Text("Paint "+paints,10,30);
		
		Image im = test.rotate(paints*Math.PI/100.0, 0x3f3f3f);
		Image im2 = im.makeTransparent(0.5);
		g.drawImage(im2,w/2,h/2);
		int tw = test.getWidth();
		int th = test.getHeight();
		g.drawImage(test,100+x,100+y);
		g.drawImage(test,w-tw,0);
		g.drawImage(test,x,y+h-th);
		g.drawImage(test,x+w-tw,y+h-th);
	}

}
