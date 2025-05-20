package bugs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Hashtable;

import bridge.Platform.Style;
import bugs.BugsConstants.BugsId;
import bugs.data.DataHelper;
import bugs.data.DataHelper.Diet;
import bugs.data.DataHelper.Flying;
import bugs.data.DataHelper.Habitat;
import bugs.data.Profile;
import bugs.data.Taxonomy;
import lib.CellId;
import lib.DrawableImageStack;
import lib.G;
import lib.GC;
import lib.Graphics;
import lib.HitPoint;
import lib.Image;
import lib.InternationalStrings;
import lib.StockArt;
import lib.TextContainer;
import lib.exCanvas;

public class BugCard extends BugsChip {
	private static DrawableImageStack bugCards = new DrawableImageStack();
	public boolean isBugCard() { return true; }
	public String key = null;
	public Profile profile = null;
	public Profile getProfile() { return profile; }
	public DataHelper<?> species = null;
	private static Hashtable<String,BugCard> cards = new Hashtable<String,BugCard>();
	public static BugCard get(String key) { return cards.get(key); }
	public BugCard(String k,Profile p,DataHelper<?> m)
	{
		key = k;
		profile = p;
		species = m;
		randomv = key.hashCode();
		bugCards.push(this);
		cards.put(k,this);
	}
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
	private void drawTerrainBackground(Graphics gc,int xp, int yp, int w,int h)
	{
		boolean isWater = profile.hasWaterHabitat();
		boolean isGround = profile.hasGroundHabitat();
		boolean isGrass = profile.hasGrassHabitat();
		boolean isForest = profile.hasForestHabitat();
		
		int nh = (isWater ? 1 : 0) + (isGround ? 1 : 0) + (isGrass ? 1 : 0) + (isForest ? 1 : 0);

		if(nh==0) 
			{ String members = profile.habitat.memberString(Habitat.values());
			  G.print("profile "+members+" has no habitat");
			}
		
		BugsChip firstColor = getTerrainBackground(1,isWater,isGround,isGrass,isForest);
		Rectangle r = new Rectangle(xp,yp,w,h);
		firstColor.getImage().centerImage(gc,r);
		switch(nh)
		{
		default:
		case 0: 
		case 1: 
			break;
		case 2:
			{
			BugsChip secondColor = getTerrainBackground(2,isWater,isGround,isGrass,isForest);
			Rectangle cl = GC.setClip(gc,xp,yp,w/2,h);
			secondColor.getImage().centerImage(gc,r);
			GC.setClip(gc,cl);
			}
			break;
		case 3:
			{
				Rectangle cl = GC.setClip(gc,xp,yp,w/3,h);
				BugsChip secondColor = getTerrainBackground(2,isWater,isGround,isGrass,isForest);
				secondColor.getImage().centerImage(gc,r);
				BugsChip thirdColor = getTerrainBackground(3,isWater,isGround,isGrass,isForest);
				GC.setClip(gc,xp+w/3,yp,w/3,h);
				thirdColor.getImage().centerImage(gc,r);
				GC.setClip(gc,cl);
			}
			break;
		case 4:
			{
				Rectangle cl = GC.setClip(gc,xp,yp,w/2,h/2);
				BugsChip secondColor = getTerrainBackground(2,isWater,isGround,isGrass,isForest);
				secondColor.getImage().centerImage(gc,r);
				GC.setClip(gc,xp+w/2,yp,w/2,h/2);
				BugsChip thirdColor = getTerrainBackground(3,isWater,isGround,isGrass,isForest);
				thirdColor.getImage().centerImage(gc,r);
				BugsChip fourthColor =  getTerrainBackground(4,isWater,isGround,isGrass,isForest);
				GC.setClip(gc,xp,yp+h/2,w/2,h/2);
				fourthColor.getImage().centerImage(gc,r);
				GC.setClip(gc,cl);
			}
			break;
		}
		GC.frameRect(gc,Color.black,xp,yp,w,h);

	}
	public Image makeCardImage(exCanvas canvas,int w,int h)
	{	// make one large image that will be downsized, so it remains consistent
		// problems to be resolved: fonts aren't right for the giant image
		// and the temporary pixellated images are frozen into the big image.
		Image im = Image.createImage(w,h);
		Graphics gc = im.getGraphics();
		gc.alwaysHighres = true;
		actualDrawChip(gc,canvas.standardPlainFont(),null,null,w,w/2,h/2,null);
		image = im;
		image.setUrl(profile.getScientificName());
		return im;
	}
	private TextContainer textContainer = new TextContainer(BugsId.Description);

	public void drawChip(Graphics gc, exCanvas canvas,Rectangle r,String thislabel)
	{
		drawChip(gc,canvas,r,null,null,thislabel,1.0);
	}
	
	public boolean drawChip(Graphics gc,exCanvas canvas,Rectangle r,HitPoint highlight,CellId rackLocation,String tooltip,double sscale)
	{	return drawChip(gc,canvas,G.Width(r),G.centerX(r),G.centerY(r),highlight,rackLocation,tooltip,sscale,1.0);
	}
	public boolean drawChip(Graphics gc,exCanvas canvas,BugsCell c,HitPoint highlight,int squareWidth,double scale,int cx,int cy,String label)
	{
		return drawChip(gc,canvas,squareWidth,cx,cy,highlight,c.rackLocation(),label,1.0,1.0);
	}
	static boolean PRECALCULATE = false;
	public boolean drawChip(Graphics gc,exCanvas drawOn,int squareWidth,int e_x,
			int e_y,HitPoint highlight,CellId rackLocation,String helptext,double sscale,double expansion)
	{	
	 	if(helptext==BACK)
    	{
    	cardBack.drawChip(gc,drawOn,squareWidth,e_x,e_y,null);	
    	return false;
    	}
	 	else
	 	{
	 	if(PRECALCULATE)
	 	{
	 	if(image==null)
	 	{
	 		image = makeCardImage(drawOn,256,128);
	 	}
	 	if(image!=null && image.getWidth()>squareWidth)
	 	{
	 		image.drawChip(gc,drawOn,squareWidth,e_x-squareWidth/2,e_y-squareWidth/4,null);
	 		gc=null;
	 	}}
 		return actualDrawChip(gc,  drawOn.standardPlainFont(),  highlight, (BugsId)rackLocation, squareWidth,e_x,e_y,highlight);
	 	}
	}
	public boolean actualDrawChip(Graphics gc, Font baseFont,
			HitPoint highlight, BugsId id,
			int SQUARESIZE,	int cx, int cy,HitPoint hitAny) 
		{
    	InternationalStrings s = G.getTranslations();
		Image image = profile.illustrationImage;
		boolean hit = false;
		if(image!=null)
		{	int xp = cx-SQUARESIZE/2;
			int yp = cy-SQUARESIZE/4;
			Rectangle r = new Rectangle(xp,yp,SQUARESIZE,SQUARESIZE/2);
			hit = G.pointInRect(highlight,r);
			if(hit)
			{
				highlight.spriteColor = Color.red;
	    		highlight.spriteRect = r;
	    		highlight.hitCode = id;
			}
			int headHeight = SQUARESIZE/15;
			int subheadHeight = SQUARESIZE/30;
			int subheadWidth = (int)(SQUARESIZE*0.45);
			
			//drawCardBackground(gc,xp,yp,SQUARESIZE,SQUARESIZE/2);
			drawTerrainBackground(gc,xp,yp,SQUARESIZE,SQUARESIZE/2);
			
			image.centerImage(gc,xp,yp+headHeight,SQUARESIZE/2,SQUARESIZE/2-headHeight);
			//canvas.drawImage(gc,image,xp,yp+headHeight,SQUARESIZE/2,SQUARESIZE/2-headHeight);
			GC.setFont(gc,G.getFont(baseFont,Style.Bold,headHeight));
			GC.Text(gc,true,xp,yp,headHeight,headHeight,Color.black,null,  ""+profile.cardPoints);	
			GC.Text(gc,true,xp+headHeight+headHeight/2,yp,SQUARESIZE-headHeight*2,headHeight,Color.black,null,  getCommonName());

			GC.setFont(gc,G.getFont(baseFont,Style.Italic,subheadHeight));
			GC.Text(gc,true,xp+SQUARESIZE/2,yp+headHeight+subheadHeight/4,subheadWidth,subheadHeight,Color.blue,null, "("+getScientificName()+")" );
	
			String sd = profile.getShortDescription();
			if(sd!=null)
			{	
				textContainer.setBounds(xp+SQUARESIZE/2,yp+headHeight+subheadHeight,subheadWidth,SQUARESIZE/4);
				textContainer.setText(sd);
				textContainer.setFont(G.getFont(baseFont,SQUARESIZE/5));
				textContainer.selectFontSize();
				textContainer.flagExtensionLines = false;
				textContainer.frameAndFill=false;
				textContainer.setVisible(true);
				textContainer.redrawBoard(gc,null);
			}
			Profile cat = profile.getCategory().getProfile();
			if(cat!=null)
			{	Image im = cat.illustrationImage;
				int ims = SQUARESIZE/6;
				String name = cat.getCommonName();
				int l = xp+SQUARESIZE-ims;
				int t = yp+SQUARESIZE/2-ims;
				if(im!=null)
				{
				im.centerImage(gc,l,t,ims,ims);	
				HitPoint.setHelpText(hitAny,l,t,ims,ims,"Group: "+name);
				}
				else
				{
				GC.Text(gc,true,xp,yp+SQUARESIZE/2-subheadHeight,SQUARESIZE/2,subheadHeight,Color.blue,null,name);
				}
			drawDietIcon(gc,profile.diet,l-ims*2/3,t+ims/3,ims*2/3,hitAny);
			if(profile.flying==Flying.YES)
				{
				BugsChip.Wings.getImage().centerImage(gc,l-ims*6/5,t+ims*2/5,ims/2,ims/2);
				HitPoint.setHelpText(hitAny,l-ims*6/5,t+ims*2/5,ims/2,ims/2,s.get("Can Fly"));
				}
			}
		}
		else
		{
		StockArt.SmallO.getImage().centerImage(gc,cx,cy,SQUARESIZE,SQUARESIZE/2);
		}
		return hit;
	}

	public void drawDietIcon(Graphics gc,Diet diet,int l,int t,int ims,HitPoint hitany)
	{	BugsChip icon = null;
		String h = "";
		if(profile.getScientificName().equals("Ameletus ludens"))
		{
			G.print("P ",profile,profile.diet);
		}
		InternationalStrings s = G.getTranslations();
		switch(diet)
		{
		case UNKNOWN:
		default: break;
		case NEGAVORE:
			icon = BugsChip.Negavore;
			h = s.get("Doesn't eat");
			break;
		case HERBIVORE:
		case FUNGIVORE:
				icon = BugsChip.Vegetarian;
				h = s.get("Vegetarian");
				break;
		case CARNIVORE:
		case OMNIVORE:
				icon = BugsChip.Predator;
				h = s.get("Predator");
				break;
		case DETRITIVORE:
				icon = BugsChip.Scavenger;
				h = s.get("Scavenger");
				break;
		case PARASITE:
				icon = BugsChip.Parasite;
				h = s.get("Parasite");
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
}
