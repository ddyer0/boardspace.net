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
package euphoria;

import lib.Image;
import lib.ImageLoader;
import lib.Random;

/*
 * extension of EuphoriaChip for recruit cards.  Remember that these are treated as Immutable.
 * 
 * there are 6 types of card and 6 copies of each in the standard deck
 * 
 */
public class ArtifactChip extends EuphoriaChip implements EuphoriaConstants
{
	public Artifact id;
	public static String artifactCardBaseName = "artifact-";
	public static int artifactCardOffset = 701;
	public static Random artifactCardRandom = new Random(0x254671fd);
	public static double artifactCardScale[] = {0.5,0.5,1.5};
	public static int nCopies = 6;		// number of copies of each card
	public boolean isArtifact() { return(true); }
	public EuphoriaChip getSpriteProxy() { return(CardBack); }
	static final int FIRST_ARTIFACT = 2;
	public EuphoriaChip subtype() { return(CardBack); }
	public static EuphoriaChip Subtype() { return(CardBack); }
	public int typeMask()
	{	return(1<<id.ordinal());
	}
	public static int allTypesMask() { return(0x3f); }
	
	private ArtifactChip(Artifact a,String c,RecruitChip bene)
	{	
		super(artifactCardOffset+a.ordinal(),
			artifactCardBaseName+a,
			artifactCardScale,
			artifactCardRandom.nextLong());
		id = a;
		name = c;
		benefactor = bene;	// associated recruit for wild card use and bonus resources
	}
	public RecruitChip benefactor = null; 
	private ArtifactChip(String a,int n,String c)
	{	
		super(artifactCardOffset+n,
			artifactCardBaseName+a,
			artifactCardScale,
			artifactCardRandom.nextLong());
		id = null;
		name = c;
	}
	
	static boolean ImagesLoaded = false;
	static ArtifactChip CardBack = new ArtifactChip("back",-1,"Card Back");
	static ArtifactChip CardBlank = new ArtifactChip("blank",-2,"Card Blank");
	static ArtifactChip Bear = new ArtifactChip(Artifact.Bear,"Choose true love",RecruitChip.MiroslavTheConArtist);
	static ArtifactChip Book = new ArtifactChip(Artifact.Book,"Read a Book",RecruitChip.JavierTheUndergroundLibrarian);
	static ArtifactChip Balloons = new ArtifactChip(Artifact.Balloon,"Help a Friend",RecruitChip.AlexandraTheHeister);
	static ArtifactChip Bifocals = new ArtifactChip(Artifact.Bifocals,"Publish an Exposee",RecruitChip.MakatoTheForger);
	static ArtifactChip Box = new ArtifactChip(Artifact.Box,"Let workers relax",RecruitChip.EkaterinaTheCheater);
	static ArtifactChip Bat = new ArtifactChip(Artifact.Bat,"Fight the Opressor",RecruitChip.JoseThePersuader);
	
	static ArtifactChip allArtifacts[] = 
		{
		CardBack,
		CardBlank,
		Bear,
		Book,
		Balloons,
		Bifocals,
		Box,
		Bat				
	};
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!ImagesLoaded)
		{
		String rDir = Dir + "artifacts/";
		String imageNames[] = new String[allArtifacts.length];
		Image mask = forcan.load_image(rDir, "artifact-mask");
		for(int i=0;i<imageNames.length; i++) { imageNames[i] = allArtifacts[i].file.toLowerCase(); }
		
		Image images[] = forcan.load_images(rDir, imageNames,mask);
		
		int idx = 0;
		for(ArtifactChip c : allArtifacts) { c.image = images[idx]; idx++; }
		Image.registerImages(allArtifacts);
        ImagesLoaded = true;
		}
	}   
}