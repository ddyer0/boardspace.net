package bugs;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.FontMetrics;
import java.util.Hashtable;

import bugs.BugsChip.Terrain;
import bugs.data.DataHelper;
import bugs.data.DataHelper.Diet;
import bugs.data.DataHelper.Flying;
import bugs.data.DataHelper.Habitat;
import bugs.data.Profile;
import bugs.data.Taxonomy;
import lib.CellId;
import lib.CompareTo;
import lib.DrawingObject;
import lib.FontManager;
import lib.G;
import lib.GC;
import lib.Graphics;
import lib.HitPoint;
import lib.Image;
import lib.InternationalStrings;
import lib.StockArt;
import lib.TextContainer;
import lib.exCanvas;

class TerrainSummary {
	Terrain type;
	int predators;
	int prey;
	int other;
	int flying;
	int total;
	
	public TerrainSummary(Terrain v)
	{	type = v;
	}
	public static TerrainSummary[]  makeSummary()
	{	Terrain vs[] = Terrain.values();
		TerrainSummary v[] = new TerrainSummary[vs.length];
		for(Terrain x : vs)
		{
			v[x.ordinal()] = new TerrainSummary(x);
		}
		return v;
	}
	public void score(BugCard card)
	{	Profile prof = card.getProfile();
		if(prof.isPredator()) { predators++; }
		else if(prof.isPrey()) { prey++; }
		else { other++; }
		if(prof.isFlying()) { flying++; }
		total++;
	}
}

/*
 * possible additional features of bug cards:
 *  "when played" features that allow immediate bonus scoring
 *  "when played" feature to allow a second card to be played
 */
public class BugCard extends BugsChip implements BugsConstants , CompareTo<BugsChip>
{
	public boolean isBugCard() { return true; }
	public String name() { return id.name()+" "+profile.name; }
	public String getName() { return name(); }
	public String key = null;
	public Profile profile = null;
	public Profile getProfile() { return profile; }
	public DataHelper<?> species = null;
	private static Hashtable<String,BugCard> cards = new Hashtable<String,BugCard>();
	private static Hashtable<Integer,BugCard> indexCards = new Hashtable<Integer,BugCard>();
	public static BugCard getBugCard(String key) { return cards.get(key); }
	public static BugCard getBugCard(int key) { return indexCards.get(key); }
	public double aspectRatio() { return bugAspectRatio();}
	public static double bugAspectRatio() { return (double)BugsChip.blueBack.getWidth()/BugsChip.blueBack.getHeight(); }
	
	public String toString() { return "<bug #"+chipNumber()+">"; }

	public BugCard(String k,Profile p,DataHelper<?> m)
	{	id = BugsId.BugCard;
		key = k;
		profile = p;
		species = m;
		randomv = key.hashCode();
		bugCards.push(this);
		cards.put(k,this);
		indexCards.put(chipNumber(),this);
	}
	public int chipNumber() { return(species.getUid()+BUGOFFSET); }
	public static int bugCount() { return bugCards.size(); }
	public static BugCard getCard(int n) { return (BugCard)bugCards.elementAt(n); }
	public String getCommonName() { return species.getCommonName(); }
	public String getScientificName() { return species.getScientificName(); }
	
	Color waterColor = new Color(245,253,254);
	Color groundColor = new Color(246,240,229);
	Color grassColor = new Color(246,255,235);
	Color forestColor = new Color(220,254,221);
	
	private Color getTerrainColor(int nth,boolean isWater,boolean isGround,boolean isGrass,boolean isForest)
	{
		Color firstColor = isWater 
				? (nth<=1 ? waterColor : getTerrainColor(nth-1,false,isGround,isGrass,isForest))
				: isGround 
					? (nth<=1 ? groundColor : getTerrainColor(nth-1,false,false,isGrass,isForest)) 
					: isGrass 
						? (nth<=1 ? grassColor : getTerrainColor(nth-1,false,false,false,isForest)) 
						: isForest ? forestColor : Color.white;
		return firstColor;

	}
	/**
	 * this is the unused version that uses solid backgrounds
	 * 
	 * @param gc
	 * @param xp
	 * @param yp
	 * @param w
	 * @param h
	 */
	@SuppressWarnings("unused")
	private void drawCardBackground(Graphics gc,int xp, int yp, int w,int h)
	{
		boolean isWater = profile.hasWaterHabitat();
		boolean isGround = profile.hasGroundHabitat();
		boolean isGrass = profile.hasGrassHabitat();
		boolean isForest = profile.hasForestHabitat();
		int nh = (isWater ? 1 : 0) + (isGround ? 1 : 0) + (isGrass ? 1 : 0) + (isForest ? 1 : 0);

		Color firstColor = getTerrainColor(1,isWater,isGround,isGrass,isForest);
		GC.fillRect(gc,firstColor,xp,yp,w,h);
		switch(nh)
		{
		default:
		case 0: 
		case 1: 
			break;
		case 2:
			{
			Color secondColor = getTerrainColor(2,isWater,isGround,isGrass,isForest);
			GC.fillPolygon(gc,secondColor,xp,yp,xp+w,yp,xp,yp+h);
			}
			break;
		case 3:
			{
				Color secondColor = getTerrainColor(2,isWater,isGround,isGrass,isForest);
				GC.fillPolygon(gc,secondColor,xp,yp,  xp+w*3/4,yp,  xp,yp+h*2/3);
				Color thirdColor = getTerrainColor(3,isWater,isGround,isGrass,isForest);
				GC.fillPolygon(gc,thirdColor,xp+w,yp+h/3,
						xp+w,yp+h, 
						xp+w/4,yp+h);
			}
			break;
		case 4:
			{
				int cx = xp+w/2;
				int cy = yp+h/2;
				Color secondColor = getTerrainColor(2,isWater,isGround,isGrass,isForest);
				GC.fillPolygon(gc,secondColor,xp,yp,  xp+w,yp,  cx,cy);
				
				Color thirdColor = getTerrainColor(3,isWater,isGround,isGrass,isForest);
				GC.fillPolygon(gc,thirdColor,xp,yp,
						cx,cy, 
						xp,yp+h);
				
				Color fourthColor =  getTerrainColor(4,isWater,isGround,isGrass,isForest);
				GC.fillPolygon(gc,fourthColor,
						xp,yp+h,
						cx,cy, 
						xp+w,yp+h);
				
			}
			break;
		}
		GC.frameRect(gc,Color.black,xp,yp,w,h);

	}

	private BugsChip getTerrainBackground(int nth,boolean isWater,boolean isGround,boolean isGrass,boolean isForest)
	{
		BugsChip firstColor = isWater 
				? (nth<=1 ? BugsChip.blueBack : getTerrainBackground(nth-1,false,isGround,isGrass,isForest))
				: isGround 
					? (nth<=1 ? BugsChip.brownBack : getTerrainBackground(nth-1,false,false,isGrass,isForest)) 
					: isGrass 
						? (nth<=1 ? BugsChip.yellowBack : getTerrainBackground(nth-1,false,false,false,isForest)) 
						: isForest ? BugsChip.greenBack : null;
		return firstColor;

	}

	/**
	 * draw a background in 1-4 parts that denotes the terrains allowed for the bug
	 * 
	 * @param gc
	 * @param xp
	 * @param yp
	 * @param w
	 * @param h
	 */
	private boolean simple = false;
	private void drawTerrainBackground(Graphics gc,int xp, int yp, int w)
	{	simple=false;
		if(simple) { drawCardBackground(gc,xp,yp,w,w/2); return; }
		boolean isWater = profile.hasWaterHabitat();
		boolean isGround = profile.hasGroundHabitat();
		boolean isGrass = profile.hasGrassHabitat();
		boolean isForest = profile.hasForestHabitat();
		int h = (int)(w/aspectRatio());
		int nh = (isWater ? 1 : 0) + (isGround ? 1 : 0) + (isGrass ? 1 : 0) + (isForest ? 1 : 0);

		if(nh==0) 
			{ String members = profile.habitat.memberString(Habitat.values());
			  G.print("profile "+members+" has no habitat");
			}
		
		BugsChip firstColor = getTerrainBackground(1,isWater,isGround,isGrass,isForest);
		Image first = firstColor.getImage();
		int firstW = first.getWidth();
		int firstH = first.getHeight();
		first.drawImage(gc,xp,yp,xp+w,yp+h,0,0,firstW,firstH);
		switch(nh)
		{
		default:
		case 0: 
		case 1: 
			break;
		case 2:
			{
			BugsChip secondColor = getTerrainBackground(2,isWater,isGround,isGrass,isForest);
			secondColor.getImage().drawImage(gc,xp,yp,xp+w/2,yp+h,0,0,firstW/2,firstH);
			}
			break;
		case 3:
			{
				BugsChip secondColor = getTerrainBackground(2,isWater,isGround,isGrass,isForest);				
				secondColor.getImage().drawImage(gc,xp,yp,xp+w/3,yp+h,0,0,firstW/3,firstH);
			
				BugsChip thirdColor = getTerrainBackground(3,isWater,isGround,isGrass,isForest);
				thirdColor.getImage().drawImage(gc,xp+w/3,yp,xp+w*2/3,yp+h,firstW/3,0,firstW*2/3,firstH);
			}
			break;
		case 4:
			{
				BugsChip secondColor = getTerrainBackground(2,isWater,isGround,isGrass,isForest);				
				secondColor.getImage().drawImage(gc,xp,yp,xp+w/4,yp+h,0,0,firstW/4,firstH);
			
				BugsChip thirdColor = getTerrainBackground(3,isWater,isGround,isGrass,isForest);
				thirdColor.getImage().drawImage(gc,xp+w/4,yp,xp+w*2/4,yp+h,firstW/4,0,firstW*2/4,firstH);

				BugsChip fourthColor =  getTerrainBackground(4,isWater,isGround,isGrass,isForest);
				fourthColor.getImage().drawImage(gc,xp+w*2/4,yp,xp+w*3/4,yp+h,firstW*2/4,0,firstW*3/4,firstH);
			}
			break;
		}
		GC.frameRect(gc,Color.black,xp,yp,w,h);

	}
	
	public Image makeCardImage(exCanvas canvas,int w,int h,boolean extended)
	{	// make one large image that will be downsized, so it remains consistent
		// problems to be resolved: fonts aren't right for the giant image
		// and the temporary pixellated images are frozen into the big image.
		Image im = Image.createImage(w,extended ? h*2 : h);
		Graphics gc = im.getGraphics();
		gc.alwaysHighres = true;
		actualDrawChip(gc,canvas.standardPlainFont(),null,null,w,w/2,h/2,null);
		if(extended)
		{
		drawExtendedChip(gc, canvas, null, new Rectangle(0,h,w,h), null); 
		}
		image = im;
		image.setUrl(profile.getScientificName());
		return im;
	}
	private TextContainer theTextContainer=null;
	private int textContainerSize = 0;
	private TextContainer textContainer()
	{
		if(theTextContainer==null)
		{
		theTextContainer = new TextContainer(BugsId.Description);	
		}
		return theTextContainer;
	}

	public void draw(Graphics gc, DrawingObject canvas,Rectangle r,String thislabel)
	{
		draw(gc,canvas,r,null,null,thislabel,1.0);
	}
	
	public void draw(Graphics gc,DrawingObject canvas,int SQUARESIZE,int cx,int cy,String label)
	{	draw(gc,canvas,SQUARESIZE,cx,cy,null,null,null,1.0,1.0);
	}

	public boolean draw(Graphics gc,DrawingObject canvas,Rectangle r,HitPoint highlight,CellId rackLocation,String tooltip,double sscale)
	{	return draw(gc,canvas,G.Width(r),G.centerX(r),G.centerY(r),highlight,rackLocation,tooltip,sscale,1.0);
	}

	static boolean PRECALCULATE = false;
	
	
	public boolean draw(Graphics gc,DrawingObject drawOn0,int squareWidth,int e_x,
			int e_y,HitPoint highlight,CellId rackLocation,String helptext,double sscale,double expansion)
	{	
		exCanvas drawOn = drawOn0.getCanvas();
	 	if(helptext==BACK)
    	{
    	boolean hit = cardBack.draw(gc,drawOn0,squareWidth,e_x,e_y,highlight,rackLocation,null,1,1);	
    	if(hit) { 
    		highlight.hitData = this;
    		highlight.spriteColor = Color.red;
    		//G.print("Hit Back ",highlight);
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
	 		image = makeCardImage(drawOn,256,128,false);
	 	}
	 	if(image!=null && image.getWidth()>squareWidth)
	 	{
	 		image.draw(gc,drawOn0,squareWidth,e_x-squareWidth/2,e_y-squareWidth/4,null);
	 		gc=null;
	 	}}
	 	
	 	HitPoint action = helptext==DROP || helptext==PICK ? highlight : null;
	 	HitPoint inaction = helptext==NOTHING ? null : highlight;
	 	boolean frameOnly = helptext==BugsChip.TOP;
		Font f = GC.getFont(gc);
 		Boolean hit = frameOnly || squareWidth<100 
 				? actualDrawMicroChip(gc,  drawOn.standardPlainFont(),  action, (BugsId)rackLocation, squareWidth,e_x,e_y,inaction,frameOnly)
 				: squareWidth<300 
 					? actualDrawCompactChip(gc,  drawOn.standardPlainFont(),  action, (BugsId)rackLocation, squareWidth,e_x,e_y,inaction)
 					: actualDrawChip(gc,  drawOn.standardPlainFont(),  action, (BugsId)rackLocation, squareWidth,e_x,e_y,inaction);
 		GC.setFont(gc,f);
 		if(hit 
 			&& (helptext==BIGCHIP || helptext==PICK)
 			&& G.pointInRect(highlight,e_x-squareWidth/2,e_y+squareWidth/8,squareWidth,squareWidth/8)
 			)
 			{
 			 highlight.spriteRect = new Rectangle(e_x-squareWidth/2,e_y+squareWidth/8,squareWidth,squareWidth/8);
 			 highlight.spriteColor = Color.green;
 			 highlight.hitCode = BugsId.HitChip;
  			 highlight.hitData = this;
 			 hit = true;
 			}
		return hit;
	 	}
	}
	public int pointValue()
	{
		return profile.cardPoints;
	}
	public boolean actualDrawChip(Graphics gc, Font baseFont,
			HitPoint highlight, BugsId hitCode,
			int SQUARESIZE,	int cx, int cy,HitPoint hitAny) 
		{
    	InternationalStrings s = G.getTranslations();
		Image bugImage = profile.illustrationImage;
		int xp = cx-SQUARESIZE/2;
		int yp = cy-SQUARESIZE/4;
		boolean hit = registerHit(hitAny,highlight==null?BugsId.HitChip:hitCode,xp,yp,SQUARESIZE);
		
		drawTerrainBackground(gc,xp,yp,SQUARESIZE);
		
		if(bugImage!=null && SQUARESIZE>30)
		{	
			int headHeight = SQUARESIZE/15;
			int scoreHeight = SQUARESIZE/10;
			int subheadHeight = SQUARESIZE/30;
			int subheadWidth = (int)(SQUARESIZE*0.45);
			
			bugImage.centerImage(gc,xp,yp+headHeight,SQUARESIZE/2,SQUARESIZE/2-headHeight);
			//canvas.drawImage(gc,image,xp,yp+headHeight,SQUARESIZE/2,SQUARESIZE/2-headHeight);
			GC.setFont(gc,FontManager.getFont(baseFont,FontManager.Style.Bold,scoreHeight));
			GC.Text(gc,true,xp,yp+SQUARESIZE/2-scoreHeight,scoreHeight,scoreHeight,Color.black,null,  ""+pointValue());	
			GC.setFont(gc,FontManager.getFont(baseFont,FontManager.Style.Bold,headHeight));
			GC.Text(gc,true,xp+headHeight+headHeight/2,yp,SQUARESIZE-headHeight*2,headHeight,Color.black,null,  getCommonName());

			GC.setFont(gc,FontManager.getFont(baseFont,FontManager.Style.Italic,subheadHeight));
			GC.Text(gc,true,xp+SQUARESIZE/2,yp+headHeight+subheadHeight/4,subheadWidth,subheadHeight,Color.blue,null, "("+getScientificName()+")" );
	
			String sd = profile.getShortDescription();
			if(sd!=null && gc!=null)
			{	TextContainer textContainer = textContainer();
				textContainer.setBounds(xp+SQUARESIZE/2,yp+headHeight+subheadHeight,subheadWidth,SQUARESIZE/4);
				if(textContainerSize!=subheadWidth)
			{	
				textContainer.setText(sd);
				// this was very inefficient and caused terrible rendering times on mobiles.
				// 4 changes to fix it; 
				// (1) use a smaller and better initial font size.
				// (2) only get here if the card size is 300 rather than 200, 
				// (3) improve the actual selectFontSize to use a binary search
				// (4) only do this at all if we're really drawing.
				textContainer.setFont(FontManager.getFont(baseFont,SQUARESIZE/20));
				textContainer.selectFontSize();
				textContainer.flagExtensionLines = false;
				textContainer.frameAndFill=false;
				textContainerSize = subheadWidth;
				}
				textContainer.setVisible(true);
				textContainer.redrawBoard(gc,null);
			}
			Profile cat = profile.getCategory().getProfile();
			if(cat!=null)
			{	Image im = cat.illustrationImage;
				int ims = SQUARESIZE/6;
				String name = cat.getCommonName()+"\n"+cat.getScientificName();
				int l = xp+SQUARESIZE-ims;
				int t = yp+SQUARESIZE/2-ims;
				if(im!=null)
				{
				im.centerImage(gc,l,t,ims,ims);	
				HitPoint.setHelpText(hitAny,l,t,ims,ims,name);
				}
				else
				{
				GC.Text(gc,true,xp,yp+SQUARESIZE/2-subheadHeight,SQUARESIZE/2,subheadHeight,Color.blue,null,name);
				}
			drawDietIcon(gc,profile.diet,l-ims*2/3,t+ims/3,ims*2/3,hitAny);
			if(profile.flying==Flying.YES)
				{
				BugsChip.Wings.getImage().centerImage(gc,l-ims*6/5,t+ims*2/5,ims/2,ims/2);
				HitPoint.setHelpText(hitAny,l-ims*6/5,t+ims*2/5,ims/2,ims/2,s.get(CanFlyMessage));
				}
			}
		}
		else
		{
		StockArt.SmallO.getImage().centerImage(gc,cx,cy,SQUARESIZE,SQUARESIZE/2);
		}
		return hit;
	}
	
	public boolean actualDrawCompactChip(Graphics gc, Font baseFont,
			HitPoint highlight, BugsId hitCode,
			int SQUARESIZE,	int cx, int cy,HitPoint hitAny) 
		{
    	InternationalStrings s = G.getTranslations();
		Image bugImage = profile.illustrationImage;
		
		int xp = cx-SQUARESIZE/2;
		int yp = cy-SQUARESIZE/4;
		boolean hit = registerHit(hitAny,highlight==null?BugsId.HitChip:hitCode,xp,yp,SQUARESIZE);

		//drawCardBackground(gc,xp,yp,SQUARESIZE,SQUARESIZE/2);
		drawTerrainBackground(gc,xp,yp,SQUARESIZE);
		
		if(SQUARESIZE>20)
		{
		int headHeight = SQUARESIZE/10;
		
		bugImage.centerImage(gc,xp,yp,SQUARESIZE/3,SQUARESIZE/3);
		//GC.frameRect(gc,Color.green,xp,yp,SQUARESIZE/3,SQUARESIZE/3);
		Font font = FontManager.getFont(baseFont,FontManager.Style.Bold,headHeight);
		GC.setFont(gc,font);
		FontMetrics fm =  FontManager.getFontMetrics(font);
		int textW = SQUARESIZE-SQUARESIZE/3;
		String name = getCommonName();
		int nameW = fm.stringWidth(name);
		int split = -1;
		if(nameW>=textW)
		{
			split = name.indexOf(' ',name.length()/2);
			if(split<0) { split = name.indexOf(' '); }
		}
		if(split<0)
		{
			GC.Text(gc,true,xp+SQUARESIZE/4,yp+headHeight,textW,headHeight,Color.black,null,  name);
		}
		else
		{	
			GC.Text(gc,true,xp+SQUARESIZE/4,yp+headHeight/4,textW,headHeight,Color.black,null,  name.substring(0,split));
			GC.Text(gc,true,xp+SQUARESIZE/4,yp+headHeight+headHeight/4,textW,headHeight,Color.black,null,  name.substring(split+1));
		}
		

		{	
			int ims = SQUARESIZE/4;		
			int wingSize = ims*3/4;
			GC.setFont(gc,FontManager.getFont(baseFont,FontManager.Style.Bold,ims));
			GC.Text(gc,true,xp,yp+SQUARESIZE/2-ims,wingSize,ims,Color.black,null,  ""+pointValue());	

			Profile cat = profile.getCategory().getProfile();
			if(cat!=null)
			{	Image im = cat.illustrationImage;
				String sname = cat.getCommonName()+"\n"+cat.getScientificName();
				int l = xp+wingSize*3;
				int cats = ims+ims/4;
				int t = yp+SQUARESIZE/2-ims;
				if(im!=null)
				{
				//GC.frameRect(gc,Color.green,l,t-ims/4,xp+SQUARESIZE-l,cats);
				im.centerImage(gc,l,t-ims/4,xp+SQUARESIZE-l,cats);	
				HitPoint.setHelpText(hitAny,l,t-ims/4,cats,cats,sname);
				}

			drawDietIcon(gc,profile.diet,xp+wingSize*2,t+ims/8,ims*3/4,hitAny);
			if(profile.flying==Flying.YES)
				{
				BugsChip.Wings.getImage().centerImage(gc,xp+wingSize,t+wingSize/4,wingSize,wingSize);
				HitPoint.setHelpText(hitAny,xp+wingSize,t,wingSize,wingSize,s.get(CanFlyMessage));
				}
			}
		}}

		return hit;
	}	
	

	public boolean actualDrawMicroChip(Graphics gc, Font baseFont,
			HitPoint highlight, BugsId hitCode,
			int SQUARESIZE,	int cx, int cy,HitPoint hitAny,boolean frameOnly) 
		{
    	InternationalStrings s = G.getTranslations();
		//Image bugImage = profile.illustrationImage;
		int xp = cx-SQUARESIZE/2;
		int yp = cy-SQUARESIZE/4;
		boolean hit = registerHit(hitAny,highlight==null?BugsId.HitChip:hitCode,xp,yp,SQUARESIZE);

		//drawCardBackground(gc,xp,yp,SQUARESIZE,SQUARESIZE/2);
		drawTerrainBackground(gc,xp,yp,SQUARESIZE);
		if(!frameOnly && SQUARESIZE>10)
		{
		int ims = SQUARESIZE/4;		
	
		//bugImage.centerImage(gc,xp+ims,yp,SQUARESIZE/4,SQUARESIZE/4);

		{	
			int wingSize = ims*3/4;
			GC.setFont(gc,FontManager.getFont(baseFont,FontManager.Style.Bold,ims));
			GC.Text(gc,true,xp,yp+SQUARESIZE/2-ims,wingSize,ims,Color.black,null,  ""+pointValue());	

			Profile cat = profile.getCategory().getProfile();
			if(cat!=null)
			{	Image im = cat.illustrationImage;
				String sname = cat.getCommonName()+"\n"+cat.getScientificName();
				int l = cx;
				int cats = (int)(SQUARESIZE*0.4);
				int t = yp;
				if(im!=null)
				{
				im.centerImage(gc,l,t+ims/4,cats	,cats);	
				HitPoint.setHelpText(hitAny,l,t+ims/4,cats,cats,sname);
				}

			drawDietIcon(gc,profile.diet,xp+wingSize,cy,ims,hitAny);
			if(profile.flying==Flying.YES)
				{
				BugsChip.Wings.getImage().centerImage(gc,xp+ims*2/3,yp+ims/4,ims,ims);
				HitPoint.setHelpText(hitAny,xp+wingSize,yp,wingSize,wingSize,s.get(CanFlyMessage));
				}
			}
		}}

		return hit;
	}	
	
	public void drawDietIcon(Graphics gc,Diet diet,int l,int t,int ims,HitPoint hitany)
	{	BugsChip icon = null;
		String h = "";
		InternationalStrings s = G.getTranslations();
		switch(diet)
		{
		default: 
			G.print("Unaccounted case ",diet);
			break;
		case UNKNOWN:
			break;
		case NEGAVORE:
			icon = BugsChip.Negavore;
			h = s.get(NoEatMessage);
			break;
		case HERBIVORE:
		case FUNGIVORE:
				icon = BugsChip.Vegetarian;
				h = s.get(VegetarianMessage);
				break;
		case CARNIVORE:
		case OMNIVORE:
				icon = BugsChip.Predator;
				h = s.get(PredatorMessage);
				break;
		case DETRITIVORE:
		case SCAVENGER:
				icon = BugsChip.Scavenger;
				h = s.get(ScavengerMessage);
				break;
		case PARASITE:
				icon = BugsChip.Parasite;
				h = s.get(ParasiteMessage);
				break;
		}
		if(icon!=null)
		{	icon.getImage().centerImage(gc,l,t,ims,ims);
			HitPoint.setHelpText(hitany,l,t,ims,ims,h);
		}
	}
	public int compareTo(BugsChip o) {
		Profile op = o.getProfile();
		if(op!=null && profile!=null)
		{
			Taxonomy cat1 = op.getCategory();
			Taxonomy cat2 = profile.getCategory();
			if(cat1!=null && cat2!=null)
			{
				return cat1.getKey().compareTo(cat2.getKey());
			}
			
		}
		return 0;
	}
	//
	// this draws the complete description into a card-sized box
	//
	public boolean drawExtendedChip(Graphics gc, exCanvas canvas,HitPoint highlight, Rectangle bottom, CellId hitchip) 
	{	int w = G.Width(bottom);
		int h = G.Height(bottom);
		int xp = G.Left(bottom);
		int yp = G.Top(bottom);
		int margin = w/20;
		drawTerrainBackground(gc,xp,yp,w);
		
		String sd = profile.getDescription();
		if(sd!=null && gc!=null)
		{	Font baseFont = canvas.standardPlainFont();
			TextContainer textContainer = textContainer();
			textContainer.setBounds(xp+margin,yp+margin,w-margin*2,h-margin*2);
			if(textContainerSize!=w)
			{
			textContainerSize = w;
			textContainer.setText(sd);
			textContainer.setFont(FontManager.getFont(baseFont,w/20));
			textContainer.selectFontSize();
			textContainer.flagExtensionLines = false;
			textContainer.frameAndFill=false;
			}
			textContainer.setVisible(true);
			textContainer.redrawBoard(gc,null);
		}
		return registerHit(highlight,hitchip,this,bottom);
	}
	

}
