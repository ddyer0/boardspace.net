package nuphoria;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
/*
 * extension of EuphoriaChip for recruit cards.  Remember that these are treated as Immutable.
 * 
 * there are just 6 ethical dilemmas, and they're all the same except for which
 * of the artifact cards is special to them.
 * 
 */
public class DilemmaChip extends EuphoriaChip implements EuphoriaConstants
{
	public boolean active;
	public Cost cost;
	public String name;
	public int recruitId;
	public static String dilemmaCardBaseName = "SecretAgendas_2ndEd-PQ_Page_";
	public static int dilemmaCardOffset = 600;
	public static Random dilemmaCardRandom = new Random(0x536581ad);
	public static double dilemmaCardScale[] = {0.5,0.5,1.0};
	public EuphoriaChip getSpriteProxy() { return(CardBack); }
	public EuphoriaChip subtype() { return(CardBack); }
	public static EuphoriaChip Subtype() { return(CardBack); }
	public boolean acceptsContent(EuphoriaChip ch)
	{
		return(super.acceptsContent(ch) || (ch.subtype().isAuthorityMarker()));
	}
	private DilemmaChip(Cost a,int idx,String c)
	{	super(dilemmaCardOffset+idx,
			dilemmaCardBaseName+(""+idx),
			dilemmaCardScale,
			dilemmaCardRandom.nextLong());
		cost = a;
		name = c;
		recruitId = idx;
	}
	static boolean ImagesLoaded = false;
	static DilemmaChip CardBack = null;
	
	static DilemmaChip allDilemmas[] = 
		{
		new DilemmaChip(null,1,"Card Back"),	
		new DilemmaChip(Cost.BookOrCardx2,2,"Read a Book"),
		new DilemmaChip(Cost.BalloonsOrCardx2,3,"Help a Friend"),
		new DilemmaChip(Cost.BifocalsOrCardx2,4,"Publish an Exposee"),
		new DilemmaChip(Cost.BoxOrCardx2,5,"Let workers relax"),
		new DilemmaChip(Cost.BearOrCardx2,6,"Choose true love"),
		new DilemmaChip(Cost.BatOrCardx2,7,"Fight the Opressor")		
	};
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!ImagesLoaded)
		{
		String rDir = Dir + "dilemmas/";
		String imageNames[] = new String[allDilemmas.length];
		Image mask = forcan.load_image(rDir, "dilemmacard-mask");
		for(int i=0;i<imageNames.length; i++) { imageNames[i] = allDilemmas[i].file; }
		
		Image images[] = forcan.load_images(rDir, imageNames,mask);
		
		int idx = 0;
		for(DilemmaChip c : allDilemmas) { c.image = images[idx]; idx++; }
		CardBack = allDilemmas[0];     
        check_digests(allDilemmas);	// verify that the chips have different digests
        
        ImagesLoaded = true;
		}
	}   
}