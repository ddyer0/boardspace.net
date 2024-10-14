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
package fanorona;

import lib.Image;
import lib.ImageLoader;
import lib.Random;
import online.game.chip;

public class FanoronaChip extends chip<FanoronaChip>
{
	public int colorIndex;
	static FanoronaChip getChip(int n) {	return(fCHIPS[n]); }
	
	private FanoronaChip(int idx,String fil,Image im,long rv,double []sca)
	{	colorIndex = idx;
		file = fil;
		image = im;
		randomv = rv;
		scale = sca;
	};

	static double SCALES[][] = 
	{{0.562,0.447,1.19},	// white chip
	 {0.572,0.473,1.08},	// dark chip
	};
	static final String[] ImageNames = 
       {"white-chip","black-chip"};
	static FanoronaChip[] fCHIPS=null;
    static final String[] chipColorString = { "L", "D" };

	// call from the viewer's preloadImages
	public static void preloadImages(ImageLoader forcan,String ImageDir)
	{	if(fCHIPS==null)
		{
		int nColors = ImageNames.length;
        Image IM[]=forcan.load_masked_images(ImageDir,ImageNames);
        FanoronaChip CC[] = new FanoronaChip[nColors];
        Random r = new Random(230694);
        for(int i=0;i<nColors;i++) 
        	{
        	CC[i]=new FanoronaChip(i,ImageNames[i],IM[i],r.nextLong(),SCALES[i]); 
        	}
        fCHIPS = CC;
        Image.registerImages(CC);
        check_digests(CC);
		}
	}


}
