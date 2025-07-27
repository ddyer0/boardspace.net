package bugs;


import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;
import bridge.Color;

import bridge.SystemFont.Style;
import java.util.Hashtable;
import bugs.data.Taxonomy;
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
public class GoalCard extends BugsChip implements BugsConstants
{
	static Random r = new Random(32637); 
	static boolean PRECALCULATE = false;
	static Hashtable<String,GoalCard> goalCards = new Hashtable<String,GoalCard>();
	static Hashtable<Integer,GoalCard> indexCards = new Hashtable<Integer,GoalCard>();
	public boolean isGoalCard() { return true; }
	String key = null;
	Goal cat1;
	Goal cat2;
	public int chipNumber()
	{
		return TAXONOMYOFFSET + cat1.getUid()+ cat2.getUid()*100;
	}
	public String toString() { return "<goal "+key+">";}
	public GoalCard(String k,Goal c1,Goal c2)
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
	
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{
		drawChip(gc,canvas,SQUARESIZE,cx,cy,null,null,null,1.0,1.0);
	}
	public boolean drawChip(Graphics gc,exCanvas canvas,Rectangle r,HitPoint highlight,CellId rackLocation,String tooltip,double sscale)
	{
		return drawChip(gc,canvas,G.Width(r),G.centerX(r),G.centerY(r),highlight,rackLocation,tooltip,sscale,1.0);
	}
	public boolean drawChip(Graphics gc,exCanvas drawOn,int squareWidth,int e_x,
			int e_y,HitPoint highlight,CellId rackLocation,String helptext,double sscale,double expansion)
	{	int squareH = (int)(squareWidth/aspectRatio());
		Rectangle r = new Rectangle(e_x-squareWidth/2,e_y-squareH/2,squareWidth,squareH);
	 	if(helptext==BACK)
    	{
    	boolean hit = goalCardBack.drawChip(gc,drawOn,squareWidth,e_x,e_y,highlight,rackLocation,null,1,1);	
    	if(hit) { 
    		highlight.hitData = this;
    		highlight.spriteColor = Color.red;
    		highlight.spriteRect = new Rectangle(e_x-squareWidth/2,e_y-squareH/2,squareWidth,squareH);
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
	 		image.drawChip(gc,drawOn,squareWidth,e_x-squareWidth/2,e_y-squareH/2,null);
	 		gc=null;
	 	}}
	 	HitPoint action = (helptext==DROP || helptext==PICK) ? highlight : null;
	 	HitPoint inaction = (helptext==NOTHING) ? null : highlight;
		Font f = GC.getFont(gc);

 		boolean hit = actualDrawChip(gc,  drawOn.standardPlainFont(),  action,action==null?BugsId.HitChip: rackLocation, r,inaction,helptext==BugsChip.TOP);
		GC.setFont(gc,f);
 		int yh = (int)(squareH/4);
 		if( (helptext==PICK || helptext==BIGCHIP)
 				&& G.pointInRect(highlight,e_x-squareWidth/2,e_y+yh,squareWidth,yh))
 			{
 			highlight.spriteRect = new Rectangle(e_x-squareWidth/2,e_y+yh,squareWidth,yh);
 			highlight.spriteColor = Color.green;
 			highlight.hitCode = BugsId.HitChip;
 			highlight.hitData = this;
 			hit = true;
 			}
 		return hit;
	 	}
	}
	public boolean drawExtendedChip(Graphics gc, exCanvas canvas,HitPoint highlight, Rectangle r, CellId hitchip) 
	{
		blankBack.getImage().centerImage(gc,r);
		int xp = G.Left(r);
		int yp = G.Top(r);
		int w = G.Width(r);
		int h = G.Height(r);
		Font font = canvas.standardPlainFont();
		if(cat1==cat2)
		{
			cat1.drawExtendedChip(gc,font,xp,yp,w,h,true);
		}
		else
		{
			cat1.drawExtendedChip(gc,font,xp,yp,w/2,h,false);
			cat2.drawExtendedChip(gc,font,xp+w/2,yp,w/2,h,false);
		}
		
	
		return registerHit(highlight,hitchip,this,r);
	}

	public double aspectRatio() { return goalAspectRatio(); }
	public static double goalAspectRatio()
	{	
		return (double)blankBack.getWidth()/blankBack.getHeight();
	}
	public boolean actualDrawChip(Graphics gc, Font baseFont,
			HitPoint highlight, CellId id,
			Rectangle r,HitPoint hitAny,boolean frameOnly) 
		{
 		boolean hit = false;
		Image im1 = cat1.getIllustrationImage();
		Image im2 = cat2.getIllustrationImage();
		
		int xp = G.Left(r);
		int yp = G.Top(r);
		int SQUARESIZE = G.Width(r);
		double aspectRatio = aspectRatio();
		int cardH = (int)(SQUARESIZE/aspectRatio);
		int cx = xp+SQUARESIZE/2;
		int cy = yp+cardH/2;
		hit = G.pointInRect(hitAny,r);
		//GC.frameRect(gc,Color.red,r);
		if(hit)
		{	hitAny.hitData = this;
			hitAny.spriteColor = id==BugsId.HitChip ? Color.green : Color.red;
			hitAny.spriteRect = r;
			hitAny.hitCode = id;
		}
		blankBack.getImage().centerImage(gc,r);
		if(SQUARESIZE>10)
		{
		if(!frameOnly)
		{
		if(cat1==cat2)
		{	Rectangle r1 = new Rectangle(cx-SQUARESIZE/6,cy-cardH/4,SQUARESIZE/2,SQUARESIZE/2);
			im1.centerImage(gc,r1);
			String m1 = cat1.getHelpText();
			HitPoint.setHelpText(hitAny,r,m1);
			
			GC.setFont(gc,lib.Font.getFont(baseFont,lib.Font.Style.Bold,SQUARESIZE/5));
			if(SQUARESIZE>150)
			{	
				GC.Text(gc,true,xp+SQUARESIZE/20,yp+SQUARESIZE/40,SQUARESIZE-SQUARESIZE/10,cardH/5,Color.black,null,m1);
			}
			
			GC.Text(gc,true,xp+SQUARESIZE/20,cy+SQUARESIZE/7,SQUARESIZE/4,SQUARESIZE/10,Color.black,null,
					cat1.legend(true));
		}
		else
		{	boolean caption = SQUARESIZE>300;
			int xs = (int)(SQUARESIZE*0.4);
			int yo = caption ? cardH/4 : cardH/3;
			int centerTweak = (int)(SQUARESIZE*0.05);
			Rectangle r1 = new Rectangle(cx-xs,cy-yo,xs-centerTweak,xs);
			Rectangle r2 = new Rectangle(cx+centerTweak,cy-yo,xs,xs);
			im1.centerImage(gc,r1);
			im2.centerImage(gc,r2);
			String m1 = cat1.getHelpText();
			String m2 = cat2.getHelpText();
			HitPoint.setHelpText(hitAny,r1,m1);
			HitPoint.setHelpText(hitAny,r2,m2);
			if(caption)
			{	GC.setFont(gc,lib.Font.getFont(baseFont,Style.Bold,xs/4));
				GC.Text(gc,false,cx-xs,yp,xs,xs/2,Color.black,null,m1);
				GC.Text(gc,false,cx+xs/10,yp,xs,xs/2,Color.black,null,m2);
			}
			GC.setFont(gc,lib.Font.getFont(baseFont,lib.Font.Style.Bold,SQUARESIZE/8));
			GC.Text(gc,true,cx-SQUARESIZE/2,cy+SQUARESIZE/5,SQUARESIZE/2,SQUARESIZE/10,Color.black,null,
					cat1.legend(false));
			GC.Text(gc,true,cx,cy+SQUARESIZE/5,SQUARESIZE/2,SQUARESIZE/10,Color.black,null,
					cat2.legend(false));
			GC.Text(gc,true,cx-SQUARESIZE/2,cy+SQUARESIZE/5,SQUARESIZE,SQUARESIZE/10,Color.black,null,
					"+");
		}}}
		return hit;
	}
	public static GoalCard getGoalCard(int ind)
	{
		return indexCards.get(ind);
	}
	public static GoalCard getGoalCard(Goal cat1,Goal cat2,boolean create)
	{	String k1 = cat1.getCommonName();
		String k2 = cat2.getCommonName();
		boolean rev = k1.compareTo(k2)>0;
		Goal c1 = rev ? cat2 : cat1;
		Goal c2 = rev ? cat1 : cat2;
		String key = rev ? (k2+"|"+k1) : (k1+"|"+k2);
		GoalCard card =  goalCards.get(key);
		if(create && card==null)
		{
			goalCards.put(key,card = new GoalCard(key,c1,c2));
			indexCards.put(card.chipNumber(),card);
		}
		return card;
	}
	private static Goal randomGoal(Random r,BugsBoard b,BugsChip bugs[])
	{
		if(r.nextDouble()>DIET_GOAL_PERCENTAGE)
		{	
			Taxonomy animals = Taxonomy.get("Animalia");
			Taxonomy cat ;
			do { 
			BugCard chip = (BugCard)(bugs[r.nextInt(bugs.length)]);
			cat = chip.getProfile().getCategory();
			} while(cat==animals);
			return cat;
		}
		else
		{
			return DietGoal.randomGoal(r,b,bugs);
		}
	}
	
	public static void buildGoalDeck(long key,BugsBoard b,BugsChip bugs[],BugsCell c)
	{	
		// build goal cards with random pairs of categories
		Random r = new Random(key);
		int deckSize = (int)(bugs.length*GOAL_MULTIPLIER);
		c.reInit();
		while(deckSize>0)
		{	Goal cat1 = randomGoal(r,b,bugs);
			Goal cat2 = randomGoal(r,b,bugs);
			c.addChip(getGoalCard(cat1,cat2,true));
			deckSize--;
		}
	}
	private static void count(Goal goal,Hashtable<Goal,Integer>sum)
	{
		Integer old = sum.get(goal);
		if(old==null) { old = 0; }
		old++;
		sum.put(goal,old);
	}
	public static Hashtable<Goal,Integer> goalDeckSummary(BugsCell deck)
	{	Hashtable<Goal,Integer> sum = new Hashtable<Goal,Integer>();
		for(int lim = deck.height()-1; lim>=0; lim--)
		{
			GoalCard card = (GoalCard)deck.chipAtIndex(lim);
			count(card.cat1,sum);
			count(card.cat2,sum);
		}
		return sum;
	}

}
