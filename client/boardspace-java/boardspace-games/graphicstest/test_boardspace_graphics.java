package graphicstest;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Vector;

import bridge.Polygon;
import graphicstest.GraphicsViewer.TestAble;
import lib.G;
import lib.Graphics;
import lib.Image;

class test_boardspace_graphics implements TestAble
{		
	public void runTest(Graphics gc,int x,int y,int w,int h)
	{
		draw(gc,w,h,
				(GraphicsViewer.step&8)!=0,
				(GraphicsViewer.step&4)!=0,
				(GraphicsViewer.step&2)!=0,
				(GraphicsViewer.step&1)!=0,
				GraphicsViewer.step&0xF);
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

public void draw(Graphics gc,int w,int h,boolean prescale,boolean globalClip,boolean imageClip,
		boolean rotate,int phase)
{	boolean localClip = false;
	boolean scaleThenClip = true;
	double imagexscale = 0.10;
	double imageyscale = 0.08;
	int imageW = (int)(w*imagexscale);
	int imageH = (int)(h*imageyscale);
	int imageS = Math.max(imageW,imageH);
	htest = prepare(htest,imageW,imageH); 
	vtest = prepare(vtest,imageH,imageW);
	Rectangle initialClip = gc.getClipBounds();
	gc.setColor(new Color(0xff50000));
	gc.fillRect(0,0,w/2,h/2);
	gc.setColor(new Color(0x6ff0060));
	gc.fillRect(w/2,0,w/2,h/2);
	gc.setColor(new Color(0x6fa0f0f));
	gc.fillRect(0,h/2,w/2,h/2);
	gc.setColor(new Color(0xf85020));
	gc.fillRect(w/2,h/2,w/2,h/2);

	Vector<Polygon>images = new Vector<Polygon>();
	int ordinal=0;
	float scale = 1.2f;//2.5f;
	float effectiveScale = prescale ? scale : 1;
	double angle = 1.1;//Math.PI/10;
	int cx = w/6;
	int cy = h/4;
	int cw = w/2;
	int ch = h/2;
	int xspace = imageS*2/3;
	int yspace = imageS*9/13;
	int centerX = w/2;
	int centerY = h/2;
	if(globalClip)
		{
		gc.setColor(new Color(0xf000));
		gc.drawRect(cx-1,cy-1,cw+2,ch+2);
		gc.drawRect(cx-3,cy-3,cw+6,ch+6);
		}
	
	if(prescale && globalClip)
		{ 
		if(scaleThenClip)
		{	// clip will be scaled
		gc.scale(scale,scale);
			gc.setClip((int)(cx/scale),(int)(cy/scale),(int)(cw/scale),(int)(ch/scale));
		}
		else
		{
		gc.setClip(cx,cy,cw,ch);
			gc.scale(scale,scale);
		}
		}
	else if(prescale) { gc.scale(scale,scale); }
	else if(globalClip) { gc.setClip(cx,cy,cw,ch); }
	if(globalClip)
	{
		gc.setColor(Color.darkGray);
		gc.fillRect(0,0,w,h);
	}
	int drawnCx = 0;
	int drawnCy = 0;
	for(int xp0=xspace/2,xstep=1;xp0<w/2;xp0+=xspace,xstep=-xstep)
				for(int yp0=yspace/2,ystep=1;yp0<h/2;yp0+=yspace,ystep=-ystep)
	{	int xp = xstep*xp0+centerX;
		int yp = ystep*yp0+centerY;
		//gc.drawLine(0,0,i,i);
		if(ordinal==0)
		{
			Point drawnCenter = gc.transform(xp,yp);
			drawnCx = G.Left(drawnCenter);
			drawnCy = G.Top(drawnCenter);
		}
		Rectangle clip = gc.getClipBounds();
		ordinal++;
		//if(ordinal==149)
		{
		Image testImage = xstep*ystep>0 ? htest : vtest;
		boolean effectiveRotate = rotate && ordinal>1;
		int iW = testImage.getWidth();
		int iH = testImage.getHeight();
		int iSize = Math.min(imageW,imageH);
		int drawW = iW-(xstep<0 ? 0 : iW/3);
		int drawH = iH+(ystep<0 ? 0 : iH/4);
		double effectiveAngle = angle+0.01*ordinal;
		Rectangle brclip = null;
		if(effectiveRotate) 
			{ 
			  gc.setRotation(effectiveAngle,xp,yp); 
			}
		if(localClip) { gc.combinedClip(xp+2,yp+2,56,56); }
		Polygon p = new Polygon();
		if(effectiveRotate) 
			{ p.addPoint(gc.transform(xp-10,yp-10)); 
			}
		p.addPoint(gc.transform(xp,yp));
		p.addPoint(gc.transform(xp+drawW,yp));
		p.addPoint(gc.transform(xp+drawW,yp+drawH));
		p.addPoint(gc.transform(xp,yp+drawH));
		p.addPoint(gc.transform(xp,yp));
	
		if(gc.predictVisibility ? gc.checkVisibility(xp,yp,drawW,drawH) : true)
		{
		if(imageClip)
			{
			gc.drawImage(testImage,xp,yp,xp+drawW,yp+drawH, iW/5,1, iW-iW/5-1,iH-iH/5);
			}
		else
			{
			gc.drawImage(testImage,xp,yp,drawW,drawH); 
			}
		}
		else
		{	p.addPoint(gc.transform(xp+drawW/2,yp+drawH/2));
		}

		
		gc.setColor(Color.white);
		gc.Text("#"+ordinal,xp,yp+drawH/3);
		//gc.Text(xp+","+yp,xp,yp+drawH*2/3);
		images.addElement(p);
		// this is the actual point of failure.  In this context,
		// getclip + setclip is not idempotent
		if(localClip) { gc.setClip(clip); }
		if(effectiveRotate) 
			{ gc.setRotation(-effectiveAngle,xp,yp); 
			}
		if(globalClip)
		{	gc.setColor(Color.yellow);
			gc.setOpacity(0.5);
			int yx = (int)((cx+cw-iSize/2)/effectiveScale);
			int yy = (int)((cy+ch-iSize/2)/effectiveScale);
			gc.fillRect(yx,	yy,	iSize*2,iSize*2);
			gc.setOpacity(1.0);
		}
	}}
	

	if(prescale && globalClip)
	{
		if(scaleThenClip)
		{	// clip will be scaled
			gc.scale(1/scale,1/scale);
			gc.setClip(initialClip);
		}
		else
		{	
			gc.setClip(initialClip); 
			gc.scale(1/scale,1/scale);
		}
	}
	else if(prescale) { gc.scale(1/scale,1/scale); }
	else if(globalClip) { gc.setClip(initialClip); }

	for(int i=0;i<images.size();i++)
	{
		Polygon p = images.elementAt(i);
		gc.setColor(Color.lightGray);
		Rectangle r = p.getBounds();
		p.framePolygon(gc);
		gc.setColor(Color.yellow);
		gc.Text("."+(i+1),G.centerX(r),G.centerY(r));
		
	}
	gc.setColor(Color.blue);
	gc.setOpacity(0.25);
	gc.drawLine(0,drawnCy,w,drawnCy);
	gc.drawLine(drawnCx,0,drawnCx,h);
	gc.setOpacity(1.0);
		
	gc.setColor(Color.black);
	gc.fillRect(w/10,0,w/2,h/15);
	gc.setColor(Color.white);
	gc.Text("phase "+phase+" prescale "+prescale+" globalclip "+globalClip+" imageClip "+imageClip+" rotate "+rotate,
			
			w/8,h/25);

}


}
