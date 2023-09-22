/* copyright notice */package trax;

import lib.G;

public class lineinfo implements TraxConstants
{	
	int end1_x;
	int end1_y;
	int end2_x;
	int end2_y;
	int end1_x_extension;
	int end1_y_extension;
	int end2_x_extension;
	int end2_y_extension;
	int seed_x;
	int seed_y;
	boolean loop=false;
	int colorIndex;
	TraxGameBoard b;
	// constructor
	lineinfo(TraxGameBoard bd,int pl,int fx,int fy)
	{	b = bd;
		colorIndex = pl;
		scorefrompositionXY(fx,fy);
	}
	
	// start at firstx,firsty which is known not to be empty
	// build a lineinfo for the color pattern specified by linemap,
	// which will be either TrackBlackLine or TrackWhiteLine
	synchronized void scorefrompositionXY(int firstx,int firsty)
	{	boolean isloop=false;
		char realboard[][] = b.realboard;
		int scoreboard[][] = b.scoreboard;
		int limy = realboard.length-1;
		int limx = realboard[0].length-1;
		int sweep = b.sweep;
		int linemap[] = TrackLines[colorIndex];
		char nextchar = realboard[firsty][firstx];
		int nx = firstx;
		int ny = firsty;
		int shift = 0;
		seed_x = firstx;
		seed_y = firsty;
		
		end1_x = end1_x_extension = nx;
		end1_y = end1_y_extension = ny;
		end2_x = end2_x_extension = nx;
		end2_y = end2_y_extension = ny;
		shift = 16;
		//System.out.println("Start Next "+nx+","+ny);
		while((nextchar!=Empty) && !isloop)
		{
		int firstchar=(nextchar-'0');
		int direction=(linemap[firstchar]>>shift)&0xf;
		if(scoreboard[ny][nx]==sweep) { throw G.Error("illegal cycle"); }
		scoreboard[ny][nx]=sweep;
		
		switch(direction)
		{ 
			default: throw G.Error("Oh No");
			case 0: ny += -1;	if(ny<0) { ny=limy; } shift=4; break;  // move north enter south
			case 1: nx += 1;	if(nx>limx) { nx=0; } shift=0; break;  // move east enter west 
			case 2: ny += 1;	if(ny>limy) { ny = 0; } shift=12; break; // move south enter north
			case 3: nx += -1; 	if(nx<0) { nx = limx; } shift=8; break;  // move west enter east
		}
		//System.out.println("Direction "+direction+" Next "+nx+","+ny+" = "+realboard[ny][nx]);
		if((nx==firstx) && (ny==firsty))
		{ 	isloop = true;
		}
		else
		{
		nextchar = realboard[ny][nx];
		end1_x_extension = nx;
		end1_y_extension = ny;
		if(nextchar!=Empty)
		{	//maintain the bounding box
			end1_x = nx;
			end1_y = ny;
		}
		}
		}
		if(!isloop)
		{
			// if we didn't find a win, track left
			nextchar = realboard[firsty][firstx];
			scoreboard[firsty][firstx]--;
			nx = firstx;	// reset back to the beginning
			ny = firsty;
			shift=20;
			//System.out.println("ReStart Next "+nx+","+ny);
			while(nextchar!=Empty)
			{
			int firstchar=(nextchar-'0');
			int direction=((linemap[firstchar])>>shift)&0xf;
			if(scoreboard[ny][nx]==sweep) { throw G.Error("illegal cycle"); }
			scoreboard[ny][nx]=sweep;
			
			switch(direction)
			{ 
				default: throw G.Error("Oh No");
				case 0: ny += -1; if(ny<0) { ny=limy; } shift=4; break;  // move north enter south
				case 1: nx += 1; if(nx>limx) { nx=0; } shift=0; break;  // move east enter west 
				case 2: ny += 1; if(ny>limy) { ny = 0; } shift=12; break; // move south enter north
				case 3: nx += -1; 	if(nx<0) { nx = limx; } shift=8; break;  // move west enter east
			}
			//System.out.println("Direction "+direction+" Next "+nx+","+ny+" = "+realboard[ny][nx]);
			if((nx==firstx) && (ny==firsty))
			{ 	throw G.Error("Shouldn't find a loop here");
			}
			else
			{
			nextchar = realboard[ny][nx];
			end2_x_extension = nx;
			end2_y_extension = ny;
			if(nextchar!=Empty)
			{	//maintain the bounding box
				end2_x = nx;
				end2_y = ny;
			}
			}
			}
		}
		
		loop = isloop;
	}

	
	lineinfo(int play,TraxGameBoard bd)
	{ colorIndex = play; 
	  b = bd;
	}
	
	// the real line score - 10 is a win - 8+2 extensions
	public int currentSpan()
	{	int mx = 0;
		if((end1_x_extension<b.left) && (end2_x_extension>=b.right)) 
			{ mx=Math.max(mx,(end2_x_extension-end1_x_extension+1)); }
		if((end1_x_extension>=b.right) && (end2_x_extension<b.left)) 
			{ mx=Math.max(mx,(end1_x_extension-end2_x_extension+1)); }
		if((end1_y_extension<b.top) && (end2_y_extension>=b.bottom))
			{ mx=Math.max(mx,(end2_y_extension-end1_y_extension+1)); }
		if((end1_y_extension>=b.bottom) && (end2_y_extension<b.top)) 
			{ mx=Math.max(mx,(end1_y_extension-end2_y_extension+1)); }
		return(mx);
	}
	
	public int potentialSpan()
	{	int xext;
		int yext;
		if(end1_x_extension<end2_x_extension)
		{	int r_deficit = Math.min(0,end2_x_extension - b.right);
			int l_deficit = Math.min(0,b.left - end1_x_extension -1); 
			xext = (end2_x_extension-end1_x_extension+1)+r_deficit+l_deficit;
		}
		else 
		{ int r_deficit = Math.min(0,end1_x_extension-b.right);
		  int l_deficit = Math.min(0,b.left-end2_x_extension-1);
		  xext = (end1_x_extension-end2_x_extension+1)+r_deficit+l_deficit;
		}
		if(end1_y_extension<end2_y_extension)
		{	int r_deficit = Math.min(0,end2_y_extension - b.bottom);
			int l_deficit = Math.min(0,b.top - end1_y_extension -1); 
			yext = (end2_y_extension-end1_y_extension+1)+r_deficit+l_deficit;
			
		}
		else 
		{ int r_deficit = Math.min(0,end1_y_extension-b.bottom);
		  int l_deficit = Math.min(0,b.top-end2_y_extension-1);
		  yext = (end1_y_extension-end2_y_extension+1)+r_deficit+l_deficit;
		}
		return(Math.max(xext,yext));
		
	}
	// a measure of loop making potention
	int loopDistanceSQ()
	{	if(loop) { return(0); }
		if((end1_x==end2_x) && (end1_y==end2_y)) { return(3); } 	// single tile, not good
		int dx = end1_x_extension-end2_x_extension;
		int dy = end2_y_extension-end1_y_extension;
		return(dx*dx+dy*dy);
	}
	// 
	public String toString()
	{ char fc = b.XtoCol(end1_x_extension);
	  char tc = b.XtoCol(end2_x_extension);
	  String str = "<lineinfo "+chipColorString[colorIndex]+" "+(loop ? "loop " : "") + fc+b.YtoRow(end1_y_extension)+"-"+tc+b.YtoRow(end2_y_extension)+" ="+currentSpan()+"p"+potentialSpan()+">";
	  return(str);
	}
	
	// return true if this line contains target_x,y.  This 
	// works by simply retracing the line.  Not terribly 
	// clever, but the lines are short.
	public boolean containsXY(int target_x,int target_y)
	{	
		int linemap[] = TrackLines[colorIndex];
		for(int ishift= loop ? 20 : 16 ; ishift<=20; ishift+=4)
		{
		int shift=ishift;
		int nextx = end1_x;
		int nexty = end1_y;
		do {
			if((nextx==target_x) && (nexty==target_y)) { return(true); }
			char thisChar = b.getBoardXY(nextx,nexty);
			if(thisChar==Empty) { break; }
			int thisCharIndex = thisChar-'0';
			int direction=(linemap[thisCharIndex]>>shift)&0xf;
			switch(direction)
			{ 
				default: throw G.Error("Oh No");
				case 0: nexty += -1;	shift=4; 	break;  // move north enter south
				case 1: nextx += 1;		shift=0; 	break;  // move east enter west 
				case 2: nexty += 1;		shift=12; 	break; // move south enter north
				case 3: nextx += -1; 	shift=8; 	break;  // move west enter east
			}
		}
		while(!((nextx==end1_x)&&(nexty==end1_y)));	// exit if loop
		}
		return(false);
	}
}
