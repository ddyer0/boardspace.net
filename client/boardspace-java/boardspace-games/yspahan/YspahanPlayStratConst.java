/**
 * 
 */
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

/**
 * @author rosengue
 *
 */
public interface YspahanPlayStratConst {

	static final boolean myDebug = false;
	
	static final int maxStratStages = 10;
	static final int maxStratSteps = 10;
	static final int maxStratValues = 10;
	static final int max_nr_of_players = 4;
	static final int max_nr_of_buildings = 6;
	static final int nr_of_regions = 4;
	static final int nr_of_groups = 4;
	static final int max_nr_of_souks = 6;
	
	static final int goldRow = 5;
	static final int camelRow = 0;
	static final boolean mydebug = false;
	public enum buildings
	{	extra_camel(1,2,0),
		extra_gold(2,2,2),
		extra_movement(3,2,2),
		extra_card(4,3,3),
		extra_points(5,4,4),
		extra_cube(6,4,4);
		buildings(int idx,int cost_camels, int cost_gold)
		{	index = idx;
			camels = cost_camels;
			gold = cost_gold;
		}
		public static int goldCost(int inx)
		{
			for (buildings b : buildings.values()) 
			{
				if (b.index == inx) { return b.gold; }
			}
			return 0;
		}
		public static int camelCost(int inx)
		{
			for (buildings b : buildings.values()) 
			{
				if (b.index == inx) { return b.camels; }
			}
			return 0;
		}

		public static buildings find(int val) 
		{ for(buildings v : values()) { if(v.index==val) { return(v); }}
		  return(null); 
		}

		int index;
		int camels;
		int gold;
	}


}
