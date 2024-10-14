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
package yspahan;

import lib.Image;
import lib.Random;
import lib.*;
import online.common.OnlineConstants;
import online.game.chip;

/*
 * generic "playing piece class, provides canonical playing pieces, 
 * image artwork, scales, and digests.  For our purposes, the squares
 * on the board are pieces too, so there are four of them.
 * 
 */
public class YspahanChip extends chip<YspahanChip> implements YspahanConstants,OnlineConstants
{	
	private ycard card=null;
	private ycube cube=null;
	public ycube getCube() { G.Assert(isCube(),"not a cube"); return(cube); }
	private ydie die = null;
	private ymisc misc = null;
	public yclass type = null;
	String helpText = null;
	Object contents = null;
	public boolean isCard() { return(card!=null); }
	public ycard getCard() { return(card); }
	public boolean isCube() { return(cube!=null); }
	public boolean isDie() { return(die!=null); }
	public boolean isMisc() { return(misc!=null); }
	public String contentsString() { return(" "+contents.toString()); }
	public String toString() { return("<chip"+contentsString()+">"); }
	private int chipNum;
	public int chipNumber() { return(chipNum); }

	public int dieValue() 
	{	if(die!=null) { return(die.faceValue); }
		return(-1);
	}
	public boolean isYellow() 
	{	if(die!=null) { return(die.yellow); }
		return(false);
	}
	private YspahanChip() {};
	private static YspahanChip[] chipArray(int n)
	{
		YspahanChip ch[] = new YspahanChip[n];
		for(int i=0;i<n;i++)
			{ ch[i]=new YspahanChip(); 
			};
		return(ch);
	}
	
	private YspahanChip(int n,String name,Image im,long rv,double scl[])
	{	chipNum = n;
		image = im;
		file = name;
		randomv = rv;
		scale = scl;
		contents = image;
	}
	private YspahanChip(ycard ca,int n,Image im,long rv,double scl[])
	{	this(n,ca.name(),im,rv,scl);
		contents = card = ca;
		type = ca.type;
		helpText = ca.helpText;
		card.chip = this;
	}
	private YspahanChip(ydie ca,int n,Image im,long rv,double scl[])
	{	this(n,ca.name(),im,rv,scl);
		contents = die = ca;
		type = ca.type;
		die.chip = this;
	}
	private YspahanChip init(ymisc ca,int n,Image im,long rv,double scl[])
	{	chipNum = n;
		file = ca.name();
		image = im;
		randomv = rv;
		scale = scl;
		contents = misc = ca;
		type = ca.type;
		misc.chip = this;
		return(this);
	}
	private YspahanChip(ycube ca,int n,Image im,long rv,double scl[])
	{	this(n,ca.name(),im,rv,scl);
		contents = cube = ca;
		type = ca.type;
		cube.chip = this;
	}

	static DrawableImageStack CANONICAL_PIECE = null;
	static YspahanChip getChip(int n) { return((YspahanChip)CANONICAL_PIECE.elementAt(n)); }
	static YspahanChip getDie(int value,boolean yellow)
	{	ydie[] vals = ydie.values();
		ydie h = vals[value-1+(yellow?6:0)];
		G.Assert((h.yellow==yellow) && (h.faceValue == value), "right die");
		return(h.chip);
	}
	public ydie getDie() { return(die); }
	
	public static YspahanChip[] miscChips = chipArray(ymisc.values().length);
	public static YspahanChip getMiscChip(ymisc c)
	{
		return(miscChips[c.ordinal()]);
	}
	
	private static double  rowScale[] = {0.5,0.5,1.0};
	private static String rowNames[] = {"bag-row","barrel-row","chest-row","vase-row"};
	public static YspahanChip rows[] = {
		new YspahanChip(1,rowNames[0],null,0,rowScale),
		new YspahanChip(2,rowNames[1],null,0,rowScale),
		new YspahanChip(3,rowNames[2],null,0,rowScale),
		new YspahanChip(4,rowNames[3],null,0,rowScale),
	};
	
	public static YspahanChip cardBack = new YspahanChip(ycard.back,0,null,0,ycard.cardScale);
	public static YspahanChip plus2 = new YspahanChip(-1,"plus2",null,0,new double[]{0.5,0.5,1.0});
	public static YspahanChip plusCube = new YspahanChip(-1,"pluscube",null,0,new double[]{0.5,0.5,1.0});
	
	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(CANONICAL_PIECE==null)
		{
		DrawableImageStack CP = new DrawableImageStack();
		Random r = new Random(6364642);
		// load cards
		Image cardMask = forcan.load_image(ImageDir,"card-mask");
		ycard[] ycards = ycard.values();
		Image IMCard[] = forcan.load_images(ImageDir,G.getNames(ycards),cardMask);
		for(int i=0; i<IMCard.length; i++) 
		{	YspahanChip ch =  new YspahanChip(ycards[i],CP.size(),IMCard[i],r.nextLong(),ycard.cardScale);
			if(ycards[i]==ycard.back) 
				{ // this is a kludge to allow the card back chip to be a load time constant
				  // without redoing the entire load sequence for yspahan images.
				  cardBack.image = ch.image;
				  cardBack.randomv = ch.randomv;
				  cardBack.chipNum = ch.chipNum;
				  cardBack.file = ch.file;
				  ch = cardBack;
				}
			CP.push(ch);
		}
		
		// load cubes
		Image cubeMask = forcan.load_image(ImageDir,"cube-mask");
		ycube[] ycubes = ycube.values();
		Image IMCube[] = forcan.load_images(ImageDir,G.getNames(ycubes),cubeMask);
		for(int i=0;i<IMCube.length;i++)
		{	CP.push( new YspahanChip(ycubes[i],CP.size(),IMCube[i],r.nextLong(),ycube.cubeScale));
		}
		
		// load dice
		ydie dice[] = ydie.values();
		Image IMDice[] = forcan.load_masked_images(DICEPATH,G.getNames(dice));
		for(int i=0;i<IMDice.length;i++)
		{	CP.push( new YspahanChip(dice[i],CP.size(),IMDice[i],r.nextLong(),dice[i].scale));
		}
		
		ymisc[] misc = ymisc.values();
		Image IMMisc[] = forcan.load_masked_images(ImageDir,G.getNames(misc));
		for(int i=0;i<IMMisc.length;i++)
		{	CP.push(miscChips[i].init(misc[i],CP.size(),IMMisc[i],r.nextLong(),misc[i].scale));
		}
		
		Image[] rowImages = forcan.load_masked_images(ImageDir,rowNames);
		for(int i=0;i<rowImages.length;i++)
		{
			rows[i].image = rowImages[i];
		}
		plusCube.image = forcan.load_image(ImageDir,plusCube.file,cubeMask);
		plus2.image = forcan.load_image(ImageDir,plus2.file,forcan.load_image(ImageDir,plus2.file+"-mask"));
		Image.registerImages(CP);
		CANONICAL_PIECE = CP;
		}
	}


}
