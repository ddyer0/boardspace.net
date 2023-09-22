
/* copyright notice */package trax;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

public class TraxChip extends chip<TraxChip> {

	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	
	// constructor for the chips on the board, which are the only things that are digestable.
	private TraxChip(String na,double[]sc,Image im)
	{	scale=sc;
		image = im;
		file = na;
		randomv = r.nextLong();
	}

    static final String[] ClassicImageFileNames = 
        {
        "classic-cross-black", 
        "classic-cross-white", 
        "classic-curve-r0",
        "classic-curve-r1",
        "classic-curve-r2",
        "classic-curve-r3"
        };
    static final String[] ModernImageFileNames = 
    {
        "new-cross-black", 
        "new-cross-white", 
        "new-curve-r0",
        "new-curve-r1",
        "new-curve-r2",
        "new-curve-r3"
    };
    
    static final double[][] CLASSIC_SCALE =
    {	{0.62,0.47,2.36},
    	{0.62,0.47,2.38},
    	{0.6,0.5,2.0},
    	{0.55,0.5,1.93},
    	{0.6,0.52,1.88},
    	{0.62,0.5,1.88}
    };
    static final double[][] MODERN_SCALE =
    {	{0.61,0.45,2.18},
    	{0.58,0.54,2.36},
    	{0.575,0.48,2.20},
    	{0.555,0.52,2.05},
    	{0.565,0.52,2.13},
    	{0.585,0.48,1.95}    	
    };
    
    public static TraxChip classicChips[] = null;
    public static TraxChip modernChips[] = null;
    public static TraxChip classicWhiteChipLines[] = null;
    public static TraxChip classicBlackChipLines[] = null;
    public static TraxChip modernRedChipLines[] = null;
    public static TraxChip modernWhiteChipLines[] = null;
    private static boolean imagesLoaded = false;
    // note that this image loading code is not fully modern because we do some very 
    // nonstandard things with the masks.
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(!imagesLoaded)
		{	
        Image classicmasks[] = forcan.load_images(ImageDir, ClassicImageFileNames,"-mask"); // load the mask images
        Image classicImages[] = forcan.load_images(ImageDir, ClassicImageFileNames, classicmasks); // load the main images
        Image classicBlackLines[] = Image.CompositeMasks(classicmasks,16,0x004080);
        Image classicWhiteLines[] = Image.CompositeMasks(classicmasks,8,0xa0d0ff);
        classicChips = new TraxChip[classicmasks.length];
        classicBlackChipLines = new TraxChip[classicmasks.length];
        classicWhiteChipLines = new TraxChip[classicmasks.length];
        for(int lim=classicmasks.length-1; lim>=0; lim--)
        {
        	classicChips[lim] = new TraxChip(ClassicImageFileNames[lim],CLASSIC_SCALE[lim],classicImages[lim]);
        	classicBlackChipLines[lim] = new TraxChip(ClassicImageFileNames[lim],CLASSIC_SCALE[lim],classicBlackLines[lim]);
        	classicWhiteChipLines[lim] = new TraxChip(ClassicImageFileNames[lim],CLASSIC_SCALE[lim],classicWhiteLines[lim]);
        }
        Image modernmasks[] = forcan.load_images(ImageDir, ModernImageFileNames,"-mask"); // load the mask images
        Image modernImages[] = forcan.load_images(ImageDir, ModernImageFileNames,modernmasks); // load the main images
        Image modernRedLines[] = Image.CompositeMasks(modernmasks,16,0xe00000);
        Image modernWhiteLines[] = Image.CompositeMasks(modernmasks,8,0xb0e0ff);
        modernChips = new TraxChip[classicmasks.length];
        modernRedChipLines = new TraxChip[classicmasks.length];
        modernWhiteChipLines = new TraxChip[classicmasks.length];
       for(int lim=modernmasks.length-1; lim>=0; lim--)
        {
        	modernChips[lim] = new TraxChip(ModernImageFileNames[lim],MODERN_SCALE[lim],modernImages[lim]);
        	modernRedChipLines[lim] = new TraxChip(ModernImageFileNames[lim],MODERN_SCALE[lim],modernRedLines[lim]);
        	modernWhiteChipLines[lim] = new TraxChip(ModernImageFileNames[lim],MODERN_SCALE[lim],modernWhiteLines[lim]);
        }
       imagesLoaded = true;
		}
	}    
}
