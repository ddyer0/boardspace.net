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
package lib;


public class StockArt extends DrawableImage<StockArt> implements Digestable{
	private static StockArtStack all = new StockArtStack();
	private StockArt(String n,double[]s)
	{
		file = n;
		scale = s;
		all.push(this);
	}
	private StockArt(String n,double s[],boolean connected)
	{	
		file = n;
		scale = s;
	}
	/**
	 * this is the recommended constructor if you already have the image.
	 * @param mfile the file name for this image
	 * @param mscale the x,y,scale for this image
	 * @param im the image
	 */
	public StockArt(String mfile,double mscale[],Image im)
	{
		this(mfile,mscale);
		image = im;
	}
	static double defaultAdjust[] = {0.5,0.5,1.0};
	/** a homepage icon */
	static public StockArt Homepage = new StockArt("homepage.png",defaultAdjust);
	
	/** a large, semi-transparent down arrow */
	static public StockArt DownArrow=new StockArt("downarrow",new double[]{1.1,1.1,1.0});
	/** a large, semi-transparent, up arrow */
	static public StockArt UpArrow=new StockArt("uparrow",new double[]{1.1,1.1,1.0} );
	/** a small X */
	static public StockArt SmallX=new StockArt( "smallx",new double[]{0.5,0.5,0.5});
	/** a small dot */
	static public StockArt SmallO=new StockArt( "smallo",new double[]{0.5,0.5,0.5});
	
	static public StockArt Rotate180 = new StockArt("rotate180.png",new double[]{0.5,0.5,1});
	static public StockArt Rotate90 = new StockArt("rotateq-nomask.png",new double[]{0.5,0.5,1});
	static public StockArt Rotate270 = new StockArt("rotateqc-nomask.png",new double[]{0.5,0.5,1});

	/** a semi-transparent clockwise rotation  symbol */
	static public StockArt Rotate_CW=new StockArt( "recycle" ,defaultAdjust);
	/** a semi-transparent counter clockwise rotation  symbol */
	static public StockArt Rotate_CCW=new StockArt( "recycle-l",defaultAdjust);
	/** a semi-transparent square pad */
	static public StockArt LandingPad=new StockArt("landingpad" ,defaultAdjust);
	/** the cbs eye logo */
	static public StockArt Eye = new StockArt( "eye-nomask",defaultAdjust);
	/** the cbs eye wiht an x */
	static public StockArt NoEye = new StockArt("noeye-nomask" ,defaultAdjust);
	/** a semi-transparent hand */
	static public StockArt Hand=new StockArt("hand" ,defaultAdjust);
	/** a small yellow dot */
	static public StockArt Dot=new StockArt("dot" ,defaultAdjust);
	/** a pulldown arrow to mark menus */
	static public StockArt Pulldown=new StockArt("pulldown" ,defaultAdjust);
	static public StockArt Rules = new StockArt("rules",new double[] {0.7,0.5,1.3});
	static public StockArt Video = new StockArt("video",new double[] {0.8,0.4,1.0});
	static public StockArt Rankings = new StockArt("rankings",new double[]{0.6,0.5,1.3});
	static public StockArt Magnifier = new StockArt("magnifier",defaultAdjust);
	static public StockArt UnMagnifier = new StockArt("unmagnifier",defaultAdjust);
	static public StockArt PaperClip = new StockArt("paperclip-nomask",defaultAdjust);
	static public StockArt PaperClipSide = new StockArt("paperclipside-nomask",defaultAdjust);

	// icons rearranged 6/2020 to conform to standard "vcr" iconology
	static public StockArt VCRWayBack = new StockArt("back-branch",defaultAdjust);
	static public StockArt VCRBackPlayer = new StockArt("wayback",defaultAdjust);
	static public StockArt VCRBackStep = new StockArt("back-step",defaultAdjust);
	static public StockArt VCRForwardStep = new StockArt("forward-step",defaultAdjust);
	static public StockArt VCRFarForward = new StockArt("forward-branch",defaultAdjust);
	static public StockArt VCRForwardPlayer = new StockArt("farforward",defaultAdjust);
	static public StockArt VCRBackBranch = new StockArt("back-player",defaultAdjust);
	static public StockArt VCRForwardBranch = new StockArt("forward-player",defaultAdjust);
/*	//these are the original arrangements
	static public StockArt VCRWayBack = new StockArt("wayback",defaultAdjust);
	static public StockArt VCRBackPlayer = new StockArt("back-player",defaultAdjust);
	static public StockArt VCRBackStep = new StockArt("back-step",defaultAdjust);
	static public StockArt VCRForwardStep = new StockArt("forward-step",defaultAdjust);
	static public StockArt VCRFarForward = new StockArt("farforward",defaultAdjust);
	static public StockArt VCRForwardPlayer = new StockArt("forward-player",defaultAdjust);
	static public StockArt VCRBackBranch = new StockArt("back-branch",defaultAdjust);
	static public StockArt VCRForwardBranch = new StockArt("forward-branch",defaultAdjust);
	*/
	static public StockArt VCRPlay = new StockArt("play",defaultAdjust);
	static public StockArt VCRStop = new StockArt("stop",defaultAdjust);
	static public StockArt VCRButton = new StockArt("vcr-button",defaultAdjust);
	static public StockArt VCRBar = new StockArt("vcr-bar",defaultAdjust);
	static public StockArt VCRTick = new StockArt("tick",defaultAdjust);
	static public StockArt VCRFrame = new StockArt("vcr-frame",defaultAdjust);
	static public StockArt VCRFrameShort = new StockArt("vcr-frame-short",defaultAdjust);
	/** a pencil */
	static public StockArt Pencil = new StockArt("pencil",defaultAdjust);
	/** a simple arrow pointing right */
	static public StockArt SolidRightArrow = new StockArt("solid-arrow-right",defaultAdjust);
	/** a simple arrow pointing left */
	static public StockArt SolidLeftArrow = new StockArt("solid-arrow-left",defaultAdjust);
	/** a simple arrow pointing up */
	static public StockArt SolidUpArrow = new StockArt("solid-arrow-up",defaultAdjust);
	/** a simple arrow pointing down */
	static public StockArt SolidDownArrow = new StockArt("solid-arrow-down",defaultAdjust);
	/** a hollow triangle */
	public static StockArt Triangle = new StockArt("triangle",defaultAdjust);
	/** a hollow square */
	public static StockArt Square = new StockArt("square",defaultAdjust);

	static public StockArt[]vcrButtons = { VCRWayBack,VCRBackPlayer,VCRBackStep,VCRForwardStep,VCRForwardPlayer,VCRFarForward };
	static public StockArt FilledCheckbox = new StockArt("filled-checkbox",new double[]{0.5,0.55,1.2});
	static public StockArt EmptyCheckbox = new StockArt("empty-checkbox",new double[]{0.5,0.55,1.2});
	static public StockArt Player = new StockArt("generic-player-nomask",defaultAdjust);

	static public StockArt Chat = new StockArt("chat-nomask",defaultAdjust);
	static public StockArt Nochat = new StockArt("nochat-nomask",defaultAdjust);
	static public StockArt GameIcon = new StockArt("generic-game-icon-nomask.png",defaultAdjust);
	static public StockArt GameSelectorIcon = new StockArt("gameselector-icon-nomask",defaultAdjust);
	public static boolean imagesLoaded = false;
	public static StockArt Rotate = new StockArt("rotate-nomask.png",defaultAdjust);
	
	public static StockArt SideBySide = new StockArt("side-by-side-nomask.png",defaultAdjust);
	public static StockArt FaceToFace = new StockArt("face-to-face-nomask.png",defaultAdjust);
	public static StockArt Calculator_Icon = new StockArt("calculator-icon",defaultAdjust);
	public static StockArt Playtable_h = new StockArt("playtable-h",new double[] {0.51,0.485,1});
	public static StockArt CloseBox = new StockArt("closebox-framed-nomask.png",defaultAdjust);
	public static StockArt FancyCloseBox = new StockArt("fancy-closebox-nomask.png",defaultAdjust);
	public static StockArt FancyEmptyBox = new StockArt("fancy-emptybox-nomask.png",defaultAdjust);
	public static StockArt FancyCheckBox = new StockArt("fancy-checkbox-nomask.png",defaultAdjust);
	public static StockArt Checkmark = new StockArt("checkmark",defaultAdjust);
	public static StockArt Exmark = new StockArt("exmark",new double[] {0.5,0.5,0.75});
	public static StockArt Tooltips = new StockArt("tooltips",defaultAdjust);
	public static StockArt Scrim =  new StockArt("scrim-nomask",defaultAdjust);
	public static StockArt TransparentScrim = new StockArt("scrim",defaultAdjust);
	public static StockArt Gear = new StockArt("gear-nomask.png",defaultAdjust);
	
	//public static StockArt GreenLight = new StockArt("green-light",new double[]{0.5,0.5,1.0});
	public static StockArt RedLight = new StockArt("red-light",new double[]{0.5,0.5,1.0});
	//public static StockArt OffLight = new StockArt("off-light",new double[]{0.5,0.5,1.0});
	
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{
		imagesLoaded = load_masked_images(forcan,ImageDir,all.toArray());
		}
	}

    static double dummyScale[][]={{0.5,0.5,1.0}};

    /**
     * convert a list of images to stockart
     * @param imageNames
     * @param images
     * @param scale
     * @return and array of stockart
     */
    static public StockArt[] preLoadArt(String []imageNames,Image images[],double[][]scale)
    {
    	StockArt art[] = new StockArt[images.length];
    	if(scale==null) { scale=dummyScale; }
        for(int i=0;i<images.length;i++) 
    	{ double sc[] = scale.length==1 ? scale[0] : scale[i];
    	  art[i] = new StockArt(imageNames[i],sc,images[i]); 
    	}
        return(art);
    }
    /**
     * load StockARt for use wth a game, from jpeg images, with standard "-mask" expected
     * for the mask images.
     * 
     * @param forcan the canvas that will supervise the loading
     * @param ImageDir the directory to load from 
     * @param ImageNames an array of image names
     * @param scale an array of x,y,scale specs for the images
     * @return an array of StockArt
     */
    static public StockArt[] preLoadArt(ImageLoader forcan,String ImageDir,String[] ImageNames,double[][]scale)
    {	return(preLoadArt(forcan,ImageDir,ImageNames,true,scale));
    }

	/**
 * Load StockArt for use with a game from jpeg images and masks
 * @param forcan the canvas that will supervise the loading
 * @param ImageDir	the directory to load from
 * @param ImageNames a list of image names 
 * @param masks an array of mask images
 * @param scale an array of x,y,scale specs for the images
 * @return an array of StockArt images
 */
    static public StockArt[] preLoadArt(ImageLoader forcan,String ImageDir,String[] ImageNames,boolean masks,double[][]scale)
    {
        Image [] icemasks = masks ? forcan.load_images(ImageDir,ImageNames,"-mask") : null;
        Image IM[]=forcan.load_images(ImageDir,ImageNames,icemasks);
        return(preLoadArt(ImageNames,IM,scale));
    }
   /**
    * make stockart not connected to the standard preloader
    * @param image
    * @param scale
    * @return the new StockArt
    */
   public static StockArt Make(String image,double scale[])
   {	StockArt m = new StockArt(image,scale,false);
   		return(m);
   }

   public long Digest(Random r) 
   {
	   return r.nextLong();
   }
   
}
