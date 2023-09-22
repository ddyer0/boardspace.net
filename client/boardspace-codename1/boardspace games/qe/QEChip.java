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
package qe;

import lib.DrawableImageStack;
import lib.ImageLoader;
import lib.OStack;
import lib.Random;
import online.game.chip;

class ChipStack extends OStack<QEChip>
{
	public QEChip[] newComponentArray(int n) { return(new QEChip[n]); }
}

/**
 * this is a specialization of {@link chip} to represent the stones used by qe;
 * and also other tiles, borders and other images that are used to draw the board.
 * 
 * @author ddyer
 *
 */
public class QEChip extends chip<QEChip> implements QEConstants
{
	private int index = 0;
	
	private static Random r = new Random(5312324);	// this gives each chip a unique random value for Digest()
	private static DrawableImageStack allChips = new DrawableImageStack();
	private static boolean imagesLoaded = false;
	public QEId id = null;
	public QEId flag = null;
	public QEId industry = null;
	int victoryPoints = 0;
	// constructor for the chips on the board, which are the only things that are digestable.
	private QEChip(String na,double[]sc,QEId con,QEId f,QEId i,int vp)
	{	index = allChips.size();
		victoryPoints = vp;
		scale=sc;
		file = na;
		id = con;
		flag = f;
		industry = i;
		randomv = r.nextLong();
		allChips.push(this);
	}
	
	// constructor for all the other random artwork.
	private QEChip(String na,double[]sc)
	{	index = allChips.size();
		scale=sc;
		file = na;
		allChips.push(this);
	}
	
	public int chipNumber() { return(index); }
	private static double whiteScale[]={0.5,0.5,1};
	
	// card deck
	public static QEChip Back = new QEChip("back",whiteScale,
			QEId.Back,null,null,0);
	public static QEChip NoQE = new QEChip("noqe",new double[] {0.5,0.6,0.8},
			QEId.NoQE,null,null,2);
	public static QEChip Score4 = new QEChip("4p-score",whiteScale);
	// 5 player, view bid card
	public static QEChip ViewBid = new QEChip("viewbid",new double[] {0.5,0.5,0.9},
			QEId.ViewBid,null,null,0);
	public static QEChip ViewAgain = new QEChip("viewagain",new double[] {0.5,0.5,0.9},
			QEId.ViewAgain,null,null,0);
	public static QEChip Score5 = new QEChip("5p-score",whiteScale);
	public static QEChip ScoreCard = new QEChip("scorecard",whiteScale);

	// country cards
	public static QEChip GR = new QEChip("flag-gr",whiteScale,
			QEId.GR,QEId.GR,null,0);
	public static QEChip UK = new QEChip("flag-uk",whiteScale,
			QEId.UK,QEId.UK,null,0);
	public static QEChip FR = new QEChip("flag-fr",whiteScale,
			QEId.FR,QEId.FR,null,0);
	public static QEChip US = new QEChip("flag-us",whiteScale,
			QEId.US,QEId.US,null,0);
	public static QEChip JP = new QEChip("flag-jp",whiteScale,QEId.JP,QEId.JP,null,0);
	
	public static QEChip FlagChips4[] = { GR,UK,FR,US};
	public static QEChip FlagChips5[] = { GR,UK,FR,US, JP};
	// industry cards
	public static QEChip Build = new QEChip("build",whiteScale,
			QEId.Build,null,QEId.Build,0);
	public static QEChip Cars = new QEChip("cars",whiteScale,
			QEId.Autos,null,QEId.Autos,0);
	public static QEChip Planes = new QEChip("planes",whiteScale,
			QEId.Planes,null,QEId.Planes,0);
	public static QEChip Trains = new QEChip("trains",whiteScale,
			QEId.Trains,null,QEId.Trains,0);
	public static QEChip Ships = new QEChip("ship",whiteScale,QEId.Ships,null,QEId.Ships,0);
	
	public static QEChip IndustryChips4[] = { Build,Cars,Planes,Trains};
	public static QEChip IndustryChips5[] = { Build,Cars,Planes,Trains, Ships};
	// bid cards
	public static QEChip Gov_1_US = new QEChip("build-1-us",whiteScale,
			QEId.Gov_1_US,QEId.US,QEId.Build,1);
	public static QEChip Gov_2_GR = new QEChip("build-2-gr",whiteScale,
			QEId.Gov_2_GR,QEId.GR,QEId.Build,2);
	public static QEChip Gov_3_FR = new QEChip("build-3-fr",whiteScale,
			QEId.Gov_3_FR,QEId.FR,QEId.Build,3);
	public static QEChip Gov_4_UK = new QEChip("build-4-uk",whiteScale,
			QEId.Gov_4_UK,QEId.UK,QEId.Build,4);
	public static QEChip GovChips[] = {Gov_1_US,Gov_2_GR,Gov_3_FR,Gov_4_UK };
	
	public static QEChip Cars_1_UK = new QEChip("cars-1-uk",whiteScale,
			QEId.Cars_1_UK,QEId.UK,QEId.Autos,1);
	public static QEChip Cars_2_FR = new QEChip("cars-2-fr",whiteScale,
			QEId.Cars_2_FR,QEId.FR,QEId.Autos,2);
	public static QEChip Cars_3_US = new QEChip("cars-3-us",whiteScale,
			QEId.Cars_3_US,QEId.US,QEId.Autos,3);
	public static QEChip Cars_4_GR = new QEChip("cars-4-gr",whiteScale,
			QEId.Cars_4_GR,QEId.GR,QEId.Autos,4);
	public static QEChip CarChips[] = {Cars_1_UK,Cars_2_FR,Cars_3_US ,Cars_4_GR};
	
	public static QEChip Planes_1_FR = new QEChip("planes-1-fr",whiteScale,
			QEId.Planes_1_FR,QEId.FR,QEId.Planes,1);
	public static QEChip Planes_2_UK = new QEChip("planes-2-uk",whiteScale,
			QEId.Planes_2_UK,QEId.UK,QEId.Planes,2);
	public static QEChip Planes_3_GR = new QEChip("planes-3-gr",whiteScale,
			QEId.Planes_3_GR,QEId.GR,QEId.Planes,3);
	public static QEChip Planes_4_US = new QEChip("planes-4-us",whiteScale,
			QEId.Planes_4_US,QEId.US,QEId.Planes,4);
	public static QEChip PlaneChips[] = { Planes_1_FR,Planes_2_UK, Planes_3_GR, Planes_4_US};
	
	public static QEChip Trains_1_GR = new QEChip("trains-1-gr",whiteScale,
			QEId.Trains_1_GR,QEId.GR,QEId.Trains,1);
	public static QEChip Trains_2_US = new QEChip("trains-2-us",whiteScale,
			QEId.Trains_2_US,QEId.US,QEId.Trains,2);
	public static QEChip Trains_3_UK = new QEChip("trains-3-uk",whiteScale,
			QEId.Trains_3_UK,QEId.UK,QEId.Trains,3);
	public static QEChip Trains_4_FR = new QEChip("trains-4-fr",whiteScale,
			QEId.Trains_4_FR,QEId.FR,QEId.Trains,4);
    public static QEChip TrainChips[] = { Trains_1_GR,Trains_2_US,Trains_3_UK,Trains_4_FR};
    public static QEChip BidCard = new QEChip("bid",whiteScale);
	public static QEChip Board = new QEChip("board",whiteScale);
	/*
	public static DrawableImage<?> Cards[] = {
			Back,
			Gov_1_US,Gov_2_GR,Gov_3_FR,Gov_4_UK,
			Cars_1_UK,Cars_2_FR,Cars_3_US,Cars_4_GR,
			Planes_1_FR,Planes_2_UK,Planes_3_GR,Planes_4_US,
			Trains_1_GR,Trains_2_US,Trains_3_UK,Trains_4_FR,	
	};
	*/
	// 5 player expansion tiles
	public static QEChip Ship_2_FR = new QEChip("ship-2-fr",whiteScale,
			QEId.Ship_2_FR,QEId.FR,QEId.Ships,2);
	public static QEChip Ship_4_JP = new QEChip("ship-2-fr",whiteScale,
			QEId.Ship_4_JP,QEId.JP,QEId.Ships,4);
	public static QEChip Ship_3_GR = new QEChip("ship-2-fr",whiteScale,
			QEId.Ship_3_GR,QEId.GR,QEId.Ships,3);
	public static QEChip Planes_3_JP = new QEChip("planes-3-jp",whiteScale,
			QEId.Planes_3_JP,QEId.JP,QEId.Planes,3);
	public static QEChip Cars_2_JP = new QEChip("cars-2-jp",whiteScale,
			QEId.Cars_2_JP,QEId.JP,QEId.Autos,2);
	public static QEChip ShipChips[] = { Ship_2_FR, Ship_3_GR, Ship_4_JP};
	/*
	public static DrawableImage<?> Cards5P[] = {
			Back,
			// remove all 1's
			Gov_2_GR,Gov_3_FR,Gov_4_UK,
			Cars_3_US,Cars_4_GR,		// removed cars-2
			Planes_2_UK,Planes_4_US,	// removed planes-3
			Trains_2_US,Trains_3_UK,Trains_4_FR,	
			
	    	Ship_2_FR,	// 5 new tiles
	    	Ship_4_JP,    	
	    	Ship_3_GR,
	    	Planes_3_JP,
	    	Cars_2_JP,

	};
	 */
	public static QEChip WhiteBoard = new QEChip("whiteboard",new double[] {0.5,0.5,0.9});

	public static QEChip CANONICAL_PIECE[] = {  };

    // indexes into the balls array, usually called the rack
    static final QEChip getChip(int n) { return((QEChip)allChips.elementAt(n)); }
    
    

    /* plain images with no mask can be noted by naming them -nomask */
    static public QEChip backgroundTile = new QEChip("background-tile-nomask",null);
    static public QEChip backgroundReviewTile = new QEChip("background-review-tile-nomask",null);
   
      
   
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
		// this will get the rest of the images that don't use the card mask
		imagesLoaded = forcan.load_masked_images(Dir,allChips);
		}
	}   
}
