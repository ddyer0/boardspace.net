/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package graphicstest;

import lib.Image;
import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.HitPoint;
import lib.Tokenizer;
import lib.LFrameProtocol;
import lib.SoundManager;
import online.game.*;
import online.game.sgf.sgf_node;

import online.search.SimpleRobotProtocol;

import java.util.Enumeration;
import java.util.Vector;

import com.codename1.ui.geom.Rectangle;
import bridge.Clip;
import bridge.Clip.AudioFormat;
import bridge.Color;
import bridge.Polygon;
import bridge.XJMenu;


public class GraphicsViewer extends CCanvas<GraphicsCell,GraphicsBoard> implements GraphicsConstants
{		
    static final String Prototype_SGF = "graphicstest"; // sgf game name

    // file names for jpeg images and masks
    static final String ImageDir = "/graphicstest/images/";
     
    // private state
    private GraphicsBoard bb = null; //the board from which we are displaying
 
     
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	GraphicsChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = GraphicsChip.Icon.image;
    }

    /**
     * this is the hook for substituting alternate tile sets.  This is called at a low level
     * from drawChip, and the result is passed to the chip's getAltChip method to substitute
     * a different chip.
     */
    public int getAltChipset() { return(0); }
    
    public XJMenu testMenu = null;
    
    interface TestAble
    {
    	public void runTest(Graphics gc);
    }
    
    
    class Test
    {	Test(String sn,String ln,TestAble r)
    	{
    		run = r;
    		shortName = sn;
    		longName = ln;
    	}
    	TestAble run;
    	String shortName;
    	String longName;
    	Object menuItem;
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default
        
        
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	GraphicsConstants.putStrings();
        }
        bb = new GraphicsBoard();
        //
        // this gets the best results on android, but requires some extra care in
        // the user interface and in the board's copyBoard operation.
        // in the user interface.
        useDirectDrawing(true);
        
        testMenu = new XJMenu("Test Actions",true);
        myFrame.addToMenuBar(testMenu);
        for(Test t : tests)
        {
        	t.menuItem = myFrame.addAction(testMenu,t.shortName,deferredEvents);
        }
        testMenu.setVisible(true);
        doInit(false);
    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        bb.doInit(bb.gametype);						// initialize the board
        if(!preserve_history)
    	{ 
        	// the color determines the first player
        	startFirstPlayer();
        	//
        	// alternative where the first player is just chosen
        	//startFirstPlayer();
 	 		//PerformAndTransmit(reviewOnly?"Edit":"Start P"+first, false,replayMode.Replay);

    	}
    }

    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect,x,y,width,height);
    }

    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
    }

	
    boolean toggle = false;
    int paints = 0;
    int step = 15;

    public void drawBoardElements(Graphics gc, HitPoint highlight)
    {
    	if(gc!=null)
    	{
    		selectedTest.run.runTest(gc);
    	
    	}

    }

    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  

       drawBoardElements(gc,selectPos);
       repaint(100);

    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	mm.player = 0;
        handleExecute(bb,mm,replay);
        
         return (true);
    }

    public commonMove ParseNewMove(String st,int pl)
    {
        return (new GraphicsMovespec());
    }

      public commonMove EditHistory(commonMove nmove)
      {	  return nmove;
      }

    public void StartDragging(HitPoint hp)
    {
    }


    public void StopDragging(HitPoint hp)
    {
        step++; 
        toggle=!toggle;
        CellId id = hp.hitCode;
       	if(!(id instanceof PrototypeId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        PrototypeId hitCode = (PrototypeId)id;
        // if direct drawing, hp.hitObject is a cell from a copy of the board
        GraphicsCell hitObject = bb.getCell(hitCell(hp));
        switch (hitCode)
        {
        default:
        	if (performStandardButtons(hitCode, hp)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " + hitObject);
            }
        	break;
        }
        }
    }

     public String gameType() 
    	{
    	return("undefined"); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Prototype_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence

    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
        if(!handled )
        {
        	for(Test t : tests)
        	{
        		if(t.menuItem.equals(target))
        		{
        			selectedTest = t;
        			handled = true;
        		}
        	}
        }
        return (handled);
    }

    public BoardProtocol getBoard()   {    return (bb);   }

    public SimpleRobotProtocol newRobotPlayer() 
    {  throw G.Error("No robots");
    }

    public void ReplayMove(sgf_node no)
    {
    }

    public String gameProgressString()
    {	// this is what the standard method does
    	 return("");
    }

    public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit) {
		return null;
	}
 
    
class Test_4914 implements TestAble
	{
	public void paint(Graphics gc)
	{
		runTest(gc);
	}
	public void runTest(Graphics gc)
	{
		dtest(gc,getWidth(),getHeight());
		repaint();
	}
	Image test = Image.createImage(100,100);
	boolean prepared;
	public void prepare()
	{	prepared = true;
		int size = 350;
		Graphics gc = test.getGraphics();
		gc.setColor(Color.blue);
		gc.fillRect(0,0,size,size);
		gc.setColor(Color.black);
		gc.drawLine(0,0,size,size);
		gc.drawLine(0,size,size,0);
		gc.setColor(Color.green);
		gc.fillRect(0,0,size,4);
		gc.fillRect(0,0,4,size);
		gc.fillRect(0,96,size,4);
		gc.fillRect(96,0,4,size);
	}
	public void Rotate(int[]ipix,int []opix,int w,int h,double angle,int fillColor)
	{
	     double sina = Math.sin(angle);
	     double cosa = Math.cos(angle);
	     int h1 = h-1;
	     int w1 = w-1;
	     for(int dx=0,centerx=w/2;dx<w;dx++) 
	     	{ for(int dy=0,centery=h/2;dy<h;dy++) 
	     	{
	      	 int sidxx = (int)(cosa*(dx-centerx)-sina*(dy-centery))+centerx;
	    	 int sidyy = (int)(sina*(dx-centerx)+cosa*(dy-centery))+centery;
	    	 // consider the actual border pixel to be absent, and use the fill color instead
	    	 // this avoids messy edges from jpegs or scaled images.
	    	 boolean inbounds = (sidxx>0) && (sidxx<w1) && (sidyy>0) && (sidyy<h1);
	     	 opix[dx+w*dy] = inbounds
	     	 	? ipix[sidxx+sidyy*w] 
	     	    : fillColor;  	// outside value of black/transparent
	      }}
	}
	
	Image createImageFromInts(int opix[],int w,int h)
	   {
	   		Image im = new Image("fin");
	   		im.createImageFromInts(opix,w,h,0,w);
	   		return im;
	   		
	   }
	   
	public Image rotate(Image im, double angle, int fillColor)
	{
	int w = im.getWidth();
	int h = im.getHeight();
	int ipix[] = new int[w*h];
	int opix[] = new int[ipix.length];
	im.getRGB(0,0,w,h,ipix,0,w);
	Rotate(ipix,opix,w,h,angle,fillColor);
	
	Image fin = createImageFromInts(opix,w,h);
	
	 return(fin);
	
	}
	
	public Image makeTransparent(Image im,double percent)
	{
	int w = im.getWidth();
	int h = im.getHeight();
	int ipix[] = new int[w*h];
	im.getRGB(0,0,w,h,ipix,0,w);
	
	   for(int lim = ipix.length-1; lim>=0; lim--)
	   {
		   int pix = ipix[lim];
		   int trans = 0xff&(pix>>24);
	       trans = (int)(trans*percent);
		   ipix[lim]=(trans<<24)|(0xffffff&pix);
	   }
	   Image fin = createImageFromInts(ipix,w,h);
	   return(fin);
	}
	int paints = 0;
	public void testFrame(Graphics g,int w,int h)
	{	
	   	paints++;
	 	g.setColor(new Color(0x8f8f9f7f));
		g.fillRect(00,00,w,h);
		g.setColor(Color.black);
	   	g.Text("Paint "+paints,10,30);
	   	
	   	Image im = rotate(test,paints*Math.PI/100.0, 0x3f3f3f);
	   	Image im2 = makeTransparent(im,0.5);
	   	g.drawImage(im2,w/2,h/2);
	   	int tw = test.getWidth();
	   	int th = test.getHeight();
	   	g.drawImage(test,100,100);
	   	g.drawImage(test,w-tw,0);
	   	g.drawImage(test,0,h-th);
	   	g.drawImage(test,w-tw,h-th);
	   	
	}
	public void dtest(Graphics realG,int w,int h)
	{	
		// 5/16/2026 fail in direct to screen  simulator ios, android
		prepare();
		testFrame(realG,w,h);
	}
	}
	
class Test_3037 implements TestAble
    {
    	public void runTest(Graphics g)
    	{
    		dtest_3037(g);
    	}
   
	    public void dtest_3037(Graphics g)
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

      	int w = getWidth();
    	int h = getHeight();
     	g.setColor(new Color(0x8f8f9f7f));
    	g.fillRect(0,0,w,h);
    	g.setColor(Color.black);
       	g.Text("Paint "+paints,100,100);

    	g.setRotation(((float)(Math.PI+paints/100.0)),w/2,w/4);

    	g.setColor(Color.white);
       	g.drawRect(w/2-100, h/2-200, 200, 100);
    	g.setColor(Color.black);
    	g.translateMatrix(w/2, h/2-100);
    	p.fillPolygon(g);			// triangle should be inscribed in the rectangle    
    	g.setColor(Color.green);
    	p.framePolygon(g);
       	g.translateMatrix(-w/2, -(h/2-100));
  	
      		
       	g.setRotation(-((float)(Math.PI+paints/100.0)),w/2,w/4);
    }
    }
    }
    
class Test_3921 implements TestAble
    {
    public void runTest(Graphics gc)
    {
    	draw_3921(gc,(step&8)!=0,(step&4)!=0,(step&2)!=0,(step&1)!=0);
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
	public void draw_3921(Graphics gc,boolean prescale,boolean globalClip,boolean localClip,boolean rotate)
	{	
		if(!prepared_3921) { prepare_3921(); }
		int w = getWidth();
		int h = getHeight();
		gc.setColor(new Color(0xfff0f0f));
		gc.fillRect(0,0,w,h);

		float scale = 2.5f;
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
		gc.translateMatrix(tx,ty);
		}
		for(int xp=0;xp<w;xp+=120)
					for(int yp=0;yp<h;yp+=120)
		{	
			//gc.drawLine(0,0,i,i);
			//Shape clip = gc.getClip();
			gc.pushClip();
			if(rotate) { gc.setRotation((float)Math.PI/3,xp,yp); }
			if(localClip) { gc.clipRect(xp+2,yp+2,56,56); }
			gc.drawImage(test,xp,yp,60,60);
			// this is the actual point of failure.  In this context,
			// getclip + setclip is not idempotent
			if(rotate) { gc.setRotation(-(float)Math.PI/3,xp,yp); }
			//gc.setClip(clip);
			gc.popClip();
		}
		if(prescale)
		{
		gc.translateMatrix(-tx,-ty);
		gc.scale(1/scale,1/scale);
		}
		if(globalClip) { gc.setClip(0,0,w,h); }
	}


    }

    class soundclips implements TestAble
    {	// games known to have custom clips:  Arimaa BlackDeath Cannon Euphoria Gobblet Honey Imagine Mutton
    	//    pendulum, quinamid sprint viticulture warp6 yspahan
    	boolean inited = false;
    	public void init(){
    		//SoundManager.preloadSounds(ArimaaViewer.soundNames);
    		//SoundManager.preloadSounds(BlackDeathViewer.soundNames);
    		//SoundManager.preloadSounds(CannonViewer.soundNames);
    		//SoundManager.preloadSounds(EuphoriaViewer.soundNames);
    		//SoundManager.preloadSounds(EuphoriaViewer.DIE_ROLL);
    		//SoundManager.preloadSounds(GobGameViewer.soundNames);
    		//SoundManager.preloadSounds(HoneyViewer.soundNames);
    		//SoundManager.preloadSounds(ImagineViewer.soundNames);
    		//SoundManager.preloadSounds(MuttonGameViewer.Sounds);
    		//SoundManager.preloadSounds(PendulumViewer.soundNames);
    		//SoundManager.preloadSounds(QuinamidViewer.rotates);
    		//SoundManager.preloadSounds(QuinamidViewer.shifts);
    		//SoundManager.preloadSounds(SprintViewer.soundNames);
    		//SoundManager.preloadSounds(ViticultureViewer.soundNames);
    		//SoundManager.preloadSounds(Warp6Viewer.soundNames);
    		//SoundManager.preloadSounds(YspahanViewer.Sounds);
    		inited = true;
    	}
    	Enumeration<String>keys = null;
    	String clip = null;
    	int mystep = 0;
		public void runTest(Graphics gc) {
			if(!inited) { init(); }
			if(keys==null || !keys.hasMoreElements()) 
				{ keys = SoundManager.getInstance().soundClips.keys();
				}
			if(mystep!=step) { clip=null; mystep=step; }
			if(clip==null) { clip = keys.nextElement(); }
			int w = getWidth();
			int h = getHeight();
			gc.setColor(Color.black);
			gc.fillRect(0,0,w,h);
			gc.setColor(Color.white);
			gc.Text(""+clip,w/10,h/10);
			Clip sound = SoundManager.loadASoundClip(clip);
			AudioFormat format = sound.getFormat();
			gc.Text(""+format,w/10,h/10*2);
			if(SoundManager.soundIdle()) 
			{  	gc.Text("playing "+clip,w/10,3*h/10);
				SoundManager.playASoundClip(clip,1000); 
			}
    
			
		}
    }
   
 class Dtest_3108 implements TestAble
 {
	 
	 private int initialBoardScaleIndex = 3;
	 private double boardScaleIndex = initialBoardScaleIndex;
	 private double newBoardScaleIndex = boardScaleIndex;
	 private double boardScales[] = {1.0,2.0,3.0,4.0};
	 private int numBoardScales = boardScales.length;
	 public double SCALE = 1.5;
	 public void init()
	 {

	 	double isi = boardScaleIndex;
	 	double tableScale = SCALE;
	 	isi*=tableScale;
	 	boardScaleIndex = newBoardScaleIndex = Math.min(numBoardScales-1,(int)(isi*SCALE));
	  		
	 }
	 public void runTest(Graphics gc)
	 {             	int w = getWidth();
	 	int h = getHeight();
	 	gc.setColor(new Color(0xff0000));
		gc.fillRect(0,0,w,h);
	  	
	  	try {	
	   	init();
			gc.setColor(Color.black);
			gc.Text("arithmetid #3108 ok "+boardScaleIndex+" "+newBoardScaleIndex,100,100);
	    
	   	}
	   	catch(Throwable err)
	   	{
	   		gc.setColor(Color.black);
	   		gc.Text("arithmetic #3108 "+err,100,150);
	   	}

         }
 }
         
 class dtest_3136 implements TestAble
 {	 // death by gc
	 public Link chain(int n)
	 {
		 Link result=null;
		 while(n-- > 0) { result = new Link(result); }
		 return(result);
	 }
	 public Vector<Link> construct(int totalsize,int chainsize)
	 {
		 Vector<Link> v = new Vector<Link>();
		 int n=0;
		 while(n<totalsize)
		 {
		 v.addElement(chain(chainsize));
		 n+=chainsize;
		 }
	 return(v);
	 }

	 class Link
		 {
		 Link next;
		 Link(Link o)
		 {
		 next = o;
		 }
	 }

	int totalsize = 10000;
 	int chainsize = totalsize/10;
 	int pass = 1;
 	boolean inited = false;
 	String message = "";
 	public void runTest(Graphics gc)
	 {	int w = getWidth();
	 	int h = getHeight();
	 	gc.setColor(new Color(0xf0f0f0));
	 	gc.fillRect(0,0,w,h);
		try {
	 		if(!inited) { 
	 			
		 		chainsize = totalsize/10;
		 		inited = true;
	 		}
	 		while(chainsize<totalsize)
	 		{	for(int i=0;i<10;i++)
	 			{construct(totalsize,chainsize);
	 			 pass++;
	 			 gc.setColor(Color.black);
	 			}
	 			chainsize += totalsize/10;
	 		}
			 gc.Text("Pass "+pass+" totalsize "+totalsize+" chainsize "+chainsize,100,100);
 	    	 gc.Text(message, 100,200);
 			 repaint();
	 		totalsize *=2;
			}
			catch (Throwable err)
			{
				message = "terminated with "+err; 
				repaint();
			}
	 }
 }

 class Test_3302 implements TestAble
 {
 	public void runTest(Graphics gc)
 	{
 		dtest_3302(gc,toggle);
 	}
     public void testFrame_3302(Graphics g,int w,int h,Polygon p,String message)
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
     	g.translateMatrix(x0,y0);
     	p.fillPolygon(g);			// triangle should be inscribed in the rectangle    
     	g.setColor(Color.green);
     	p.framePolygon(g);
        	g.translateMatrix(-x0,-y0);
     	         		
        	g.setRotation(-((float)(Math.PI+paints/100.0)),cx,cy);
        	
        	
       	// draw a grid of larger triangles scaled
        	for(int x=1;x<5;x++)
        	{ for(int y=1;y<5;y++)
        		{	int xp = x*30;
        			int yp = y*30;
        			g.scale(x,y);
        			g.translateMatrix(xp,yp);
       			
        	      	g.setColor(Color.white);
                	g.drawRect(-20,-20,40,20);
             	g.setColor(Color.black);
             	p.fillPolygon(g);			// triangle should be inscribed in the rectangle    
             	g.setColor(Color.green);
             	p.framePolygon(g);
                 	
       			g.translateMatrix(-xp, -yp);
               	g.scale(1.0f/x,1.0f/y);
      			
      			g.setColor(Color.red);
      			g.fillRect(xp*x,yp*y,x*4,y*4);
      			g.setColor(Color.black);
      			g.Text(""+xp+","+yp,xp*x+x*4,yp*y);
        		}
        	}

     }
     public void dtest_3302(Graphics realG,boolean buffer)
     {	Polygon p = new Polygon();
     	// 5/16/2026 fail in direct to screen  simulator ios, android
     	float rotation = 0.0f;

     	p = new Polygon();
     	p.addPoint(-20, 0);
     	p.addPoint(0, -20);
     	p.addPoint(20, 0);
     	p.addPoint(-20,0);

       	int w = getWidth();
     	int h = getHeight();
     	if(buffer)
     	{
     	Image offscreen = Image.createImage(w,h);
     	Graphics g = offscreen.getGraphics();
     	g.setFont(getFont());			// this shouldn't be necessary, but the default font is inappropriate
     	testFrame_3302(g,w,h,p,"draw to buffer");
     	realG.setRotation(rotation,w/2,h/2);
     	realG.drawImage(offscreen,0,0);
     	realG.setRotation(-rotation,w/2,h/2);
     	}
     	else
     	{	realG.setRotation(rotation,w/2,h/2);
     		testFrame_3302(realG,w,h,p,"draw to screen");
     		realG.setRotation(-rotation,w/2,h/2);
     	}    	

     }
	
 }

 class Test_5058 implements TestAble
 {
		 
	
	
	public void paint(Graphics gc)
	{
		runTest(gc);
	}
	public void runTest(Graphics gc)
	{
		dtest(gc,getWidth(),getHeight());
		repaint();
	}
	
	int paints=0;
	
	public void dtest(Graphics gc,int w,int h)
	{	int filledColor = 0xff3f3f;
		gc.setColor(new Color(0xa0ffff));
		gc.fillRect(0,0,w,h);
		gc.setColor(new Color(0));
		paints++;
		gc.Text("paint "+paints,50,50);
		gc.drawRect(200,100,300,300);
		gc.pushClip();
		gc.clipRect(200,100,300,300);
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
		 
	
	public void paint(Graphics gc)
	{
		runTest(gc);
	}
	public void runTest(Graphics gc)
	{
		dtest(gc,getWidth(),getHeight());
		repaint();
	}
	
	int paints=0;
	
	public void dtest(Graphics gc,int w,int h)
	{	int filledColor = 0xff3f3f;
		gc.setColor(new Color(0xa0ffff));
		gc.fillRect(0,0,w,h);
		gc.setColor(new Color(0));
		paints++;
		gc.Text("paint "+paints,50,50);
		gc.drawRect(200,100,300,300);
		gc.clipRect(200,100,300,300);
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
		gc.popClip();
	}
 }

 public void testFrame_3302(Graphics g,int w,int h,Polygon p,String message)
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
     	g.translateMatrix(x0,y0);
     	p.fillPolygon(g);			// triangle should be inscribed in the rectangle    
     	g.setColor(Color.green);
     	p.framePolygon(g);
        	g.translateMatrix(-x0,-y0);
     	         		
        	g.setRotation(-((float)(Math.PI+paints/100.0)),cx,cy);
        	
        	
       	// draw a grid of larger triangles scaled
        	for(int x=1;x<5;x++)
        	{ for(int y=1;y<5;y++)
        		{	int xp = x*30;
        			int yp = y*30;
        			g.scale(x,y);
        			g.translateMatrix(xp,yp);
       			
        	      	g.setColor(Color.white);
                	g.drawRect(-20,-20,40,20);
             	g.setColor(Color.black);
             	p.fillPolygon(g);			// triangle should be inscribed in the rectangle    
             	g.setColor(Color.green);
             	p.framePolygon(g);
                 	
       			g.translateMatrix(-xp, -yp);
               	g.scale(1.0f/x,1.0f/y);
      			
      			g.setColor(Color.red);
      			g.fillRect(xp*x,yp*y,x*4,y*4);
      			g.setColor(Color.black);
      			g.Text(""+xp+","+yp,xp*x+x*4,yp*y);
        		}
        	}

     }
 
 public void dtest_3302(Graphics realG,boolean buffer)
     {	Polygon p = new Polygon();
     	// 5/16/2026 fail in direct to screen  simulator ios, android
     	float rotation = 0.0f;

     	p = new Polygon();
     	p.addPoint(-20, 0);
     	p.addPoint(0, -20);
     	p.addPoint(20, 0);
     	p.addPoint(-20,0);

       	int w = getWidth();
     	int h = getHeight();
     	if(buffer)
     	{
     	Image offscreen = Image.createImage(w,h);
     	Graphics g = offscreen.getGraphics();
     	g.setFont(getFont());			// this shouldn't be necessary, but the default font is inappropriate
     	testFrame_3302(g,w,h,p,"draw to buffer");
     	realG.setRotation(rotation,w/2,h/2);
     	realG.drawImage(offscreen,0,0);
     	realG.setRotation(-rotation,w/2,h/2);
     	}
     	else
     	{	realG.setRotation(rotation,w/2,h/2);
     		testFrame_3302(realG,w,h,p,"draw to screen");
     		realG.setRotation(-rotation,w/2,h/2);
     	}    	

     }

	
 

    Test[] tests = {  
    		new Test("issue 4914","glitchy animation",new Test_4914()),   
    		new Test("issue 5058","ios crash overpush",new Test_5058()), 
    		new Test("issue 5058a","ios crash overpop",new Test_5058a()), 
    		new Test("issue 3921","complex clipping and rotation",new Test_3921()),
    		new Test("issue 3302","scaling and translation",new Test_3302()),
        	new Test("issue 3037","simpler rotation test",new Test_3037()),
        	new Test("sound clips","test all sound clips",new soundclips()),
        	new Test("ios bad code #3108","gets nullpointerexception",new Dtest_3108()),
        	//new Test("issue 3136 gc","death by gc",new dtest_3136()),
        };
        Test selectedTest = tests[0];
     
        
}

