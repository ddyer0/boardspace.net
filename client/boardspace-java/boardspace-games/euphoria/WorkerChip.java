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
 */
public class WorkerChip extends EuphoriaChip implements EuphoriaConstants
{
	public boolean active;
	public int idx;
	public static int WorkerOffset = 300;
	public static Random workerRandom = new Random(0x7612358d);
	public static double workerScale[] = {0.67,0.37,1.5};
	public String toSting() { return("<worker "+color+" "+idx+">"); }
	public boolean isWorker() { return(true); }
	public EuphoriaChip subtype() { return(allWorkers[0][0]); }
	static EuphoriaChip Subtype () { return(allWorkers[0][0]); }
	private WorkerChip(Colors co,int n)
	{	super(WorkerOffset+co.ordinal()*6+n,
			""+co+"-d6-"+n,
			workerScale,
			workerRandom.nextLong());
		color = co;
		idx = n;
	}
	static boolean ImagesLoaded = false;
	static WorkerChip CardBack = null;
	public int knowledge() { return(idx); }
	public WorkerChip reRoll(Random r)
	{	int n = Random.nextInt(r,6)+1;
		return(getWorker(color,n));
	}
	public String shortName() { return(color.name()+" "+knowledge()); }
	static WorkerChip allWorkers[][] = 
		{
		// dice in canonical color order, red green blue black white purple
	{new WorkerChip(Colors.Red,1),
	 	 new WorkerChip(Colors.Red,2),
		 new WorkerChip(Colors.Red,3),
		 new WorkerChip(Colors.Red,4),
		 new WorkerChip(Colors.Red,5),
		 new WorkerChip(Colors.Red,6)},
		 
		 {new WorkerChip(Colors.Green,1),
		 	 new WorkerChip(Colors.Green,2),
			 new WorkerChip(Colors.Green,3),
			 new WorkerChip(Colors.Green,4),
			 new WorkerChip(Colors.Green,5),
			 new WorkerChip(Colors.Green,6)},
		 
	   {new WorkerChip(Colors.Blue,1),
 	     new WorkerChip(Colors.Blue,2),
	     new WorkerChip(Colors.Blue,3),
	     new WorkerChip(Colors.Blue,4),
	     new WorkerChip(Colors.Blue,5),
	     new WorkerChip(Colors.Blue,6)},
		 
	    {new WorkerChip(Colors.Black,1),
		 	 new WorkerChip(Colors.Black,2),
			 new WorkerChip(Colors.Black,3),
			 new WorkerChip(Colors.Black,4),
			 new WorkerChip(Colors.Black,5),
			 new WorkerChip(Colors.Black,6)},
			 
		{new WorkerChip(Colors.White,1),
				 new WorkerChip(Colors.White,2),
				 new WorkerChip(Colors.White,3),
				 new WorkerChip(Colors.White,4),
				 new WorkerChip(Colors.White,5),
				 new WorkerChip(Colors.White,6)},
				 

		 {new WorkerChip(Colors.Purple,1),
		 	 new WorkerChip(Colors.Purple,2),
			 new WorkerChip(Colors.Purple,3),
			 new WorkerChip(Colors.Purple,4),
			 new WorkerChip(Colors.Purple,5),
			 new WorkerChip(Colors.Purple,6)},
	};
	public static WorkerChip getWorker(Colors c,int n) { return(allWorkers[c.ordinal()][n-1]); }
	
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(!ImagesLoaded)
		{
		String imageNames[] = new String[allWorkers.length*6];
		{int idx = 0;
		for(WorkerChip ca[] : allWorkers) { for(WorkerChip c : ca) { imageNames[idx++] = c.file.toLowerCase(); }}
		}
		Image images[] = forcan.load_masked_images(Dir+"dice/", imageNames);
		
		{int idx = 0;
		for(WorkerChip ca[] : allWorkers) { for(WorkerChip c : ca)
			{ c.image = images[idx];
			  idx++; 
			  Image.registerImages(ca);
			}
		}
		}
        
        ImagesLoaded = true;
		}
	}   
}