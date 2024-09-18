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
package blackdeath;


import blackdeath.BlackDeathConstants.BlackDeathColor;
import blackdeath.BlackDeathConstants.CardEffect;
import blackdeath.BlackDeathConstants.Dice;
import blackdeath.BlackDeathConstants.DiseaseMod;
import lib.DrawableImageStack;
import lib.G;
import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import lib.exCanvas;
import online.common.OnlineConstants;
import online.game.chip;

class ChipStack extends OStack<BlackDeathChip>
{
	public BlackDeathChip[] newComponentArray(int n) { return(new BlackDeathChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by blackdeath;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */

public class BlackDeathChip extends chip<BlackDeathChip> implements OnlineConstants
{
	private int index = 0;
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	private DiseaseMod mod = null;
	public DiseaseMod getDiseaseMod()
	{
		return(mod);
	}
	private Dice die = null;
	public BlackDeathColor color;
	public CardEffect cardEffect;
	public boolean isCard() { return(cardEffect!=null); }

	public boolean isDie() { return(die!=null); }
	public int dieValue()
	{
		G.Assert(die!=null,"not a die");
		return(die.faceValue);
	}
	// constructor for the chips on the board, which are the only things that are digestable.
	private BlackDeathChip(String na,BlackDeathColor col,double[]sc)
	{	index = allChips.size();
		scale=sc;
		file = na;
		color = col;
		randomv = r.nextLong();
		allChips.push(this);
	}
	private double cardScale[] = {0.5,0.5,1};
	
	private BlackDeathChip(String na,CardEffect eff)
	{	index = allChips.size();
		file = na;
		cardEffect = eff;
		randomv = r.nextLong();
		scale = cardScale;
		allChips.push(this);
	}
	
	private BlackDeathChip(Image im,BlackDeathColor c)
	{	index = allChips.size();
		image = im;
		scale = defaultScale;
		file = im.getName();
		color = c;
		randomv = r.nextLong();
		allChips.push(this);
	}
	// constructure for dice
	private BlackDeathChip(Dice d,Image im)
	{
		die = d;
		image = im;
		scale = d.scale;
		file = d.getName();
		randomv = r.nextLong();
		allChips.push(this);
	}
	public static BlackDeathChip BDice[] = new BlackDeathChip[Dice.values().length];

	// constructor for all the other random artwork.
	private BlackDeathChip(String na,double[]sc,DiseaseMod modchip)
	{	index = allChips.size();
		scale=sc;
		file = na;
		mod = modchip;
		allChips.push(this);
	}
	
	public int chipNumber() { return(index); }
	private static double[] defaultScale = {0.5,0.5,1.0};
	static final BlackDeathChip getChip(int n) { return((BlackDeathChip)allChips.elementAt(n)); }

	static public BlackDeathChip board = new BlackDeathChip("bigcolormap-nomask",defaultScale,null); 
	static public BlackDeathChip backgroundTile = new BlackDeathChip("background-tile-nomask",defaultScale,null);
    static public BlackDeathChip backgroundReviewTile = new BlackDeathChip("background-review-tile-nomask",defaultScale,null);
  
    // player id chips
    static public BlackDeathChip[] PlayerChips = null;
    static public BlackDeathChip PlayerFrame = new BlackDeathChip("player-frame-nomask",null,defaultScale);
    // diseast mods
    static public BlackDeathChip Mod_Normal = new BlackDeathChip("mod-normal-nomask",defaultScale,DiseaseMod.None);
    static public BlackDeathChip Mod_Crowd = new BlackDeathChip("mod-crowd-nomask",defaultScale,DiseaseMod.Crowd);
    static public BlackDeathChip Mod_Wet = new BlackDeathChip("mod-wet-nomask",defaultScale,DiseaseMod.Wet);
    static public BlackDeathChip Mod_Cold = new BlackDeathChip("mod-cold-nomask",defaultScale,DiseaseMod.Cold);
    static public BlackDeathChip Mod_Warm = new BlackDeathChip("mod-warm-nomask",defaultScale,DiseaseMod.Warm);
    static public BlackDeathChip ModChips[] = {
    	Mod_Normal, Mod_Crowd, Mod_Wet, Mod_Cold, Mod_Warm	
    };
    static public BlackDeathChip Track = new BlackDeathChip("vandm-nomask",null,defaultScale);
    static public BlackDeathChip SkullIcon = new BlackDeathChip("disease-skull",null,defaultScale);
    static public BlackDeathChip War = new BlackDeathChip("disease-war",null,defaultScale);
    static public BlackDeathChip Quaranteen = new BlackDeathChip("disease-quaranteen",null,defaultScale);
    static public BlackDeathChip Pogrom = new BlackDeathChip("disease-pogrom",null,defaultScale);
  
    private static String PlayerChipNames[] = { "disease-1","disease-2","disease-3","disease-4","disease-5","disease-6" };
    
    public static BlackDeathChip Cards[] =
    	{
    	new BlackDeathChip("card-11-1",CardEffect.SlowTravel),
    	new BlackDeathChip("card-11-2",CardEffect.SlowTravel),
    	new BlackDeathChip("card-11-3",CardEffect.HighVirulence),
    	new BlackDeathChip("card-11-4",CardEffect.Crusade),
    	new BlackDeathChip("card-11-5",CardEffect.Famine),
    	new BlackDeathChip("card-11-6",CardEffect.LowVirulence),
    	new BlackDeathChip("card-11-7",CardEffect.Fire),
    	new BlackDeathChip("card-11-8",CardEffect.Quaranteen),
    	new BlackDeathChip("card-11-9",CardEffect.Quaranteen),
    	new BlackDeathChip("card-11-10",CardEffect.MutationVirulenceOrMortality),
    	new BlackDeathChip("card12-1",CardEffect.MutationSwap),
    	new BlackDeathChip("card12-2",CardEffect.MutationVirulenceAndMortality),
    	new BlackDeathChip("card12-3",CardEffect.Mongols),
    	new BlackDeathChip("card12-4",CardEffect.Smugglers),
    	new BlackDeathChip("card12-5",CardEffect.TradersPlus1),
    	new BlackDeathChip("card12-6",CardEffect.TradersPlus2),
    	new BlackDeathChip("card12-7",CardEffect.Pogrom),
    	new BlackDeathChip("card12-8",CardEffect.Pogrom),
    	new BlackDeathChip("card12-9",CardEffect.War),
    	new BlackDeathChip("card12-10",CardEffect.War),
    	};
    public static BlackDeathChip getCard(CardEffect type)
    {
    	for(BlackDeathChip card : Cards) { if(card.cardEffect==type) { return(card); }}
    	throw G.Error("Card type %s not found", type);
    }
    public BlackDeathChip getCard(String name) { return(getCard(CardEffect.find(name))); }
    
    static BlackDeathChip getDie(int value)
	{	return(BDice[value-1]);
	}
	public static BlackDeathChip cardBack = new BlackDeathChip("cards",null,defaultScale);
	
	public static String BACK = NotHelp+"_back_";	// the | causes it to be passed in rather than used as a tooltip
	
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,double xscale,int cx,int cy,String label)
	{   boolean isBack = BACK.equals(label);
		if(cardBack!=null && isBack)
		{
		 cardBack.drawChip(gc,canvas,SQUARESIZE,xscale, cx,cy,null);
		}
		else
		{ super.drawChip(gc, canvas, SQUARESIZE, xscale, cx, cy, label);
		}
	}


    /**
     * this is a fairly standard preloadImages method, called from the
     * game initialization.  It loads the images into the stack of
     * chips we've built
     * @param forcan the canvas for which we are loading the images.
     * @param Dir the directory to find the image files.
     */
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!imagesLoaded)
		{	
		Image cardMask = forcan.load_image(Dir,"card-mask");
		cardBack.image = forcan.load_image(Dir, "cards",cardMask);
		forcan.load_images(Dir,Cards,cardMask);
		forcan.load_masked_images(Dir,allChips);
		Image chips[] = forcan.load_images(Dir, PlayerChipNames,forcan.load_image(Dir,"disease-mask"));		
		PlayerChips = new BlackDeathChip[chips.length];
		

		BlackDeathColor colors[] = BlackDeathColor.values();
		for(int i=0;i<PlayerChips.length;i++) 
		{ BlackDeathColor color = colors[i];
		  color.chip = PlayerChips[i] = new BlackDeathChip(chips[i],color);
		}

		String names[] = Dice.getNames();
		Image dice[] = forcan.load_masked_images(DICEPATH, names);
		Dice dv[] = Dice.values();
		for(int i=0;i<dv.length; i++)
			{
			BDice[i] = dv[i].chip = new BlackDeathChip(dv[i],dice[i]);		
		}
		imagesLoaded = true;
		}
	}
}
