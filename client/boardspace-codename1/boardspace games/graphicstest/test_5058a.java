package graphicstest;

import bridge.Color;
import bridge.Polygon;
import graphicstest.GraphicsViewer.TestAble;
import lib.Graphics;


class Test_5058 implements TestAble
{	// overpush

	public void runTest(Graphics gc,int x,int y,int w,int h)
	{
		dtest(gc,x,y,w,h);
	}
	
	int paints=0;
	
	public void dtest(Graphics gc,int x,int y,int w,int h)
	{	int filledColor = 0xff3f3f;
		gc.setColor(new Color(0xa0ffff));
		gc.fillRect(x,y,w,h);
		gc.setColor(new Color(0));
		paints++;
		gc.Text("paint "+paints,x+50,y+50);
		gc.drawRect(x+200,y+100,300,300);
		gc.pushClip();
		gc.clipRect(x+200,y+100,300,300);
		gc.translate(200,100);
		Polygon playPoly = new Polygon();
		playPoly.addPoint(-50,-10);
		playPoly.addPoint(150,-10);
		playPoly.addPoint(150,20);
		playPoly.addPoint(-50,20);
		playPoly.addPoint(-50,-10);
		for (int j=0;j<4;j++) 
		{
			int px = j*100;
			int py = j*50;
			gc.pushClip();
			gc.translate(px,py);
			gc.setColor(new Color(filledColor));
			playPoly.fillPolygon(gc);
			gc.setColor(new Color(0xff));
			playPoly.framePolygon(gc);
			gc.setColor(new Color(0xffffff));
			String name = "player "+j;
			
			gc.Text(name,0,0);
			
			gc.translate(-px,-py);
			gc.popClip();
	  }
		gc.translate(-200,-100);
	}
}
class Test_5058a implements TestAble
{	// overpop
		 

	public void runTest(Graphics gc,int x,int y,int w,int h)
	{
		dtest(gc,x,y,w,h);
	}
	
	int paints=0;
	
	public void dtest(Graphics gc,int x,int y,int w,int h)
	{	int filledColor = 0xff3f3f;
		gc.setColor(new Color(0xa0ffff));
		gc.fillRect(x,y,w,h);
		gc.setColor(new Color(0));
		paints++;
		gc.Text("paint "+paints,x+50,y+50);
		gc.drawRect(x+200,y+100,300,300);
		gc.clipRect(x+200,y+100,300,300);
		gc.translate(200,100);
		Polygon playPoly = new Polygon();
		playPoly.addPoint(-50,-10);
		playPoly.addPoint(150,-10);
		playPoly.addPoint(150,20);
		playPoly.addPoint(-50,20);
		playPoly.addPoint(-50,-10);
		for (int j=0;j<4;j++) 
		{
			int px = j*100;
			int py = j*50;
			gc.pushClip();
			gc.translate(px,py);
			gc.setColor(new Color(filledColor));
			playPoly.fillPolygon(gc);
			gc.setColor(new Color(0xff));
			playPoly.framePolygon(gc);
			gc.setColor(new Color(0xffffff));
			String name = "player "+j;
			
			gc.Text(name,x,y);
			
			gc.translate(-px,-py);
			gc.popClip();
	  }
		gc.translate(-200,-100);
		gc.popClip();
	}
}
