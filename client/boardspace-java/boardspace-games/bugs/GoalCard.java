package bugs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Hashtable;

import bugs.BugsConstants.BugsId;
import bugs.data.Profile;
import bugs.data.Taxonomy;
import lib.AR;
import lib.CellId;
import lib.G;
import lib.GC;
import lib.Graphics;
import lib.HitPoint;
import lib.Image;
import lib.Random;
import lib.exCanvas;

/** note that to conform to the standard expectations for "chips" the goalcards
 * have to be immutable and have a sharable integer identity
 * 
 * possible new variations on goal cards:
 *  "exclusive" cards that only score for the current player
 *  3 valued goal cards
 *  
 */
public class GoalCard extends BugsChip
{
	static Random r = new Random(32637); 
	static boolean PRECALCULATE = false;
	static Hashtable<String,GoalCard> goalCards = new Hashtable<String,GoalCard>();
	static Hashtable<Integer,GoalCard> indexCards = new Hashtable<Integer,GoalCard>();
	String key = null;
	Taxonomy cat1;
	Taxonomy cat2;
	public int chipNumber()
	{
		return TAXONOMYOFFSET + cat1.getUid()+ cat2.getUid()*100;
	}
	public String toString() { return "<goal "+key+">";}
	public GoalCard(String k,Taxonomy c1,Taxonomy c2)
	{	super();
		key = k;
		cat1 = c1;
		cat2 = c2;
		id = BugsId.GoalCard;
		randomv = r.nextInt();		
	}
	
	private Image makeGoalImage(exCanvas drawOn, int i, int j) {
		// TODO Auto-generated method stub
		return null;
	}
	public boolean drawChip(Graphics gc,exCanvas drawOn,int squareWidth,int e_x,
			int e_y,HitPoint highlight,CellId rackLocation,String helptext,double sscale,double expansion)
	{	
	 	if(helptext==BACK)
    	{
    	boolean hit = goalCardBack.drawChip(gc,drawOn,squareWidth/2,e_x,e_y,highlight,rackLocation,null,1,1);	
    	if(hit) { 
    		highlight.hitData = this;
    		highlight.spriteColor = Color.red;
    		highlight.spriteRect = new Rectangle(e_x-squareWidth/2,e_y-squareWidth/4,squareWidth,squareWidth/2);
     		}
    	return hit;
       	}
	 	else
	 	{
	 	if(PRECALCULATE)
	 	{
	 	if(image==null)
	 	{
	 		image = makeGoalImage(drawOn,256,128);
	 	}
	 	if(image!=null && image.getWidth()>squareWidth)
	 	{
	 		image.drawChip(gc,drawOn,squareWidth,e_x-squareWidth/2,e_y-squareWidth/4,null);
	 		gc=null;
	 	}}
	 	HitPoint action = helptext==NOHIT ? null : highlight;
 		return actualDrawChip(gc,  drawOn.standardPlainFont(),  action, (BugsId)rackLocation, squareWidth,e_x,e_y,highlight);
	 	}
	}
	
	public boolean actualDrawChip(Graphics gc, Font baseFont,
			HitPoint highlight, BugsId id,
			int SQUARESIZE,	int cx, int cy,HitPoint hitAny) 
		{
 		boolean hit = false;
		Profile prof1 = cat1.getProfile();
		Profile prof2 = cat2.getProfile();
		Image im1 = prof1.illustrationImage;
		Image im2 = prof2.illustrationImage;
		
		int xp = cx-SQUARESIZE/2;
		int yp = cy-SQUARESIZE/4;
		int cardH = (int)(SQUARESIZE/1.45);
		Rectangle r = new Rectangle(xp,yp,SQUARESIZE,cardH);
		hit = G.pointInRect(highlight,r);
		if(hit)
		{	highlight.hitData = this;
			highlight.spriteColor = Color.red;
    		highlight.spriteRect = r;
    		highlight.hitCode = id;
		}
		blankBack.getImage().centerImage(gc,r);
		
		if(prof1==prof2)
		{	Rectangle r1 = new Rectangle(cx-SQUARESIZE/6,cy-SQUARESIZE/6,SQUARESIZE/2,SQUARESIZE/2);
			im1.centerImage(gc,r1);
			HitPoint.setHelpText(hitAny,r,cat1.getCommonName()+"\n"+cat1.getScientificName());
			GC.setFont(gc,lib.Font.getFont(baseFont,lib.Font.Style.Bold,SQUARESIZE/5));
			GC.Text(gc,true,cx-(int)(SQUARESIZE*0.43),cy+SQUARESIZE/5,SQUARESIZE/4,SQUARESIZE/8,Color.black,null,"2x");
		}
		else
		{	int xs = (int)(SQUARESIZE*0.4);
			int yo = (int)(SQUARESIZE*0.15);
			Rectangle r1 = new Rectangle(cx-xs,cy-yo,xs,xs);
			Rectangle r2 = new Rectangle(cx+(int)(SQUARESIZE*0.05),cy-yo,xs,xs);
			im1.centerImage(gc,r1);
			im2.centerImage(gc,r2);
			HitPoint.setHelpText(hitAny,r1,cat1.getCommonName()+"\n"+cat1.getScientificName());
			HitPoint.setHelpText(hitAny,r2,cat2.getCommonName()+"\n"+cat1.getScientificName());
			GC.setFont(gc,lib.Font.getFont(baseFont,lib.Font.Style.Bold,SQUARESIZE/8));
			GC.Text(gc,true,cx-SQUARESIZE/2,cy+SQUARESIZE/5,SQUARESIZE,SQUARESIZE/5,Color.black,null,"1x + 1x");
		}
		return hit;
	}
	public static GoalCard getGoalCard(int ind)
	{
		return indexCards.get(ind);
	}
	public static GoalCard getGoalCard(Taxonomy cat1,Taxonomy cat2,boolean create)
	{
		boolean rev = cat1.getCommonName().compareTo(cat2.getCommonName())>0;
		Taxonomy c1 = rev ? cat2 : cat1;
		Taxonomy c2 = rev ? cat1 : cat2;
		String key = c1.getKey()+"|"+c2.getKey();
		GoalCard card =  goalCards.get(key);
		if(create && card==null)
		{
			goalCards.put(key,card = new GoalCard(key,c1,c2));
			indexCards.put(card.chipNumber(),card);
		}
		return card;
	}
	public static void buildGoalDeck(BugsChip drawableImages[],BugsCell c)
	{	
		// build goal cards with random pairs of categories
		int map[] = AR.intArray(drawableImages.length);
		Random r = new Random(52626260);
		Taxonomy animals = Taxonomy.get("Animalia");
		r.shuffle(map);
		c.reInit();
		for(int i=0;i<drawableImages.length-1;i+=2)
		{	BugCard chip1 = (BugCard)(drawableImages[map[i]]);
			BugCard chip2 = (BugCard)(drawableImages[map[i+1]]);
			Taxonomy cat1 = (Taxonomy)(chip1.getProfile().getCategory());
			Taxonomy cat2 = (Taxonomy)(chip2.getProfile().getCategory());
			if(!(cat1==animals || cat2==animals))
			{
			c.addChip(getGoalCard(cat1,cat2,true));
			}
		}
	}

}
