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
package micropul;


import lib.Graphics;
import lib.Image;
import lib.ImageLoader;
import lib.G;
import lib.GC;
import lib.Random;
import lib.StockArt;
import lib.exCanvas;
import online.game.chip;

public class MicropulChip extends chip<MicropulChip>
{	static final int JEWEL_OFFSET = 60;
	static final int TINT_OFFSET = 62;
	static final int BLANK_OFFSET = 66;
	static final int SQUARE_OFFSET = 67;	// fake chip

	public int index = 0;
	private int components[]=null;
	// components[x] identifying pips
	static final int x = 0;		// blank
	static final int WM = 1;	// white micropul
	static final int BM = 2;	// black micropul
	static final int C1 = 3;	// catalyst 1
	static final int C2 = 4;	// catalyst 2
	static final int PL = 5;	// plus
	
	static final int BIG_CODE = 6;	// first big micropul code
	static final int WM4P = 6;	// big white micropul with plus
	static final int BM4P = 7;	// big black microput with plus
	static final int WM41 = 8;	// big white micropul with catalyst
	static final int BM41 = 9;	// big black micropul with catalyst
	// jewels and tints are not found in the ordinary tiles
	static final int BJ = 10;	// blue jewel
	static final int RJ = 11;	// red jewel;
	static final int BT = 12; 	// blue tint
	static final int RT = 13;	// red tint
	static final int XT = 14;	// bad tint
	
	// codes for activity associated with a pair of pips
	static final int C_N = 0;		// no activity
	static final int C_BAD = 1;		// incompatible
	static final int C_LIKE = 2;	// same micropul, glue
	static final int C_ACT = 4;		// single activation
	static final int C_ACT2 = 8;	// double activation
	static final int C_PLUS = 16;	// extra turn
	static final int C_PLUS2 = 32;	// 2 extra turns
	static final int C_L = 64;		// local (for left operand) activation
	static final int C_R = 128;		// remove (for right operand) activation
	
	static final int BIG_REMOTE = 1024;	// existing big micrpul activated 
	int blips = 0;
	int pipCompat[][] = 
	{/* 0 */
		{C_N,C_N,C_N,C_N,C_N,   C_N,C_N,C_N,C_N,C_N},	// x compatible with all, no activations
	  /* 1 */
		{C_N,
		 C_LIKE,		//white x white
		 C_BAD,			//white x black
		 C_ACT|C_R, 	// white x dot, remote dot activates
		 C_ACT2|C_R,	// white x double, remote dots activate
		 C_PLUS|C_R,	// white x plus, remote plus activates
		 C_PLUS|C_LIKE|C_R, // white x big white plus, remote plus activates
		 C_BAD,			// white x black
		 C_LIKE|C_ACT|C_R,	// white x bit white dot, remote dot activates
		 C_BAD},	// white micro
	 /* 2 */
		 {C_N,
		  C_BAD,	// black x white
		  C_LIKE,	// black x black
		  C_ACT|C_R,	// black x dot, remote dot activates
		  C_ACT2|C_R,	// black x double dot, remote dots activate
		  C_PLUS|C_R,	// black x plus, remote plus activates
		  C_BAD,		// black x big white plus
		  C_PLUS|C_LIKE|C_R,	// black x big black plus, remote plus activates
		  C_BAD,				// black x big white dot
		  C_LIKE|C_ACT|C_R},	// black x bit black dot, remote dot activates
	 /* 3 */
		{C_N,
		 C_ACT|C_L,		// dot x white, local dot activates
		 C_ACT|C_L,		// dot x black, local dot activates
		 C_N,			// dot x dot
		 C_N,			// dot x double dot 
		 C_N,			// dot x plus
		 C_ACT|C_L,		// dot x big white plus, local dot activates
		 C_ACT|C_L,		// dot x big black plus, local dot activates
		 C_ACT|C_L,		// dot x big white dot, local dot activates
		 C_ACT|C_L},	// dot x bit black dot, local dot activates
	/* 4 */
		{C_N,			// 
	     C_ACT2|C_L,	// double dot x white, local activates
	     C_ACT2|C_L,	// doubl dot x black, local activates
	     C_N,			// double x dot
	     C_N,			// double x double
	     C_N,			// double x plus
	     C_ACT2|C_L,	// double x big white plus, local activates
	     C_ACT2|C_L,	// double x big black plus, local activates
	     C_ACT2|C_L,	// double x big white dot, local activates
	     C_ACT2|C_L}, 	// double x big black dot, local activates
	 /* 5 */
		{C_N,
	     C_PLUS|C_L,	// plus x white, local activates
	     C_PLUS|C_L,	// plus x black, local activates
	     C_N,			// plus x dot
	     C_N,			// plus x double
	     C_N,			// plus x plus
	     C_PLUS|C_L,	// plus x big white plus, local activates
	     C_PLUS|C_L,	// plus x big black plus, loal activates
	     C_PLUS|C_L,	// plus x big white dot, local activates
	     C_PLUS|C_L},	// plus x big black dot, local activates
		// big micropul
	  /* 6 */
		{C_N,
	     C_LIKE|C_PLUS|C_L,	// big white plus x white, local activates
	     C_BAD,				// big white x black
	     C_ACT|C_R,			// big white plus x dot, remote activates
	     C_ACT2|C_R,		// big white plus x double, remote activates
	     C_PLUS|C_R,		// big white plus x plus, remote activates
	     C_LIKE|C_PLUS2|C_L|C_R,	// big white plus x big white plus, both activate
	     C_BAD,						// big white plus x black
	     C_LIKE|C_L|C_R|C_ACT|C_PLUS,			// big white plus x big white dot, both activate
	     C_BAD},			// big white plus x big black dot
	  /* 7 */
		{C_N,
	     C_BAD,		// big black plus x white
	     C_LIKE|C_PLUS|C_L,	// big black plus x black, local activates
	     C_ACT|C_R,				// big black plus x dot, remote activates
	     C_ACT2|C_R,			// big black plus x double, remote activates
	     C_PLUS|C_R,			// big black plus x plus, remote activates
	     C_BAD,					// big black plus x big white plus
	     C_LIKE|C_R|C_L|C_PLUS2,	// big black plus x big black plus, both activate
	     C_BAD,					// big black plus x big white dot
	     C_LIKE|C_R|C_L|C_ACT|C_PLUS},	// big black plus x big black dot, both activate
	  /* 8 */
		{C_N,
	     C_LIKE|C_ACT|C_L,			// big white dot x white, local activatges
	     C_BAD,						// big white dot x black
	     C_ACT|C_R,					// big white dot x dot, remote activates
	     C_ACT2|C_R,				// big black dot x double, remote activates
	     C_PLUS|C_R,					// big black dot x plus, remote activates
	     C_LIKE|C_ACT|C_PLUS|C_R|C_L,	// big white dot x big white plus, both activate
	     C_BAD,							// bit white dot x black
	     C_LIKE|C_ACT2|C_R|C_L,			// big white dot x big white dot, both activate
	     C_BAD},						// big white dot x big black dot
	  /* 9  */
		{C_N,
	     C_BAD,				// big black dot x white 
	     C_LIKE|C_ACT|C_L,	// big black dot x black, local acivates
	     C_ACT|C_R,			// big black dot x dot, remote activates
	     C_ACT2|C_R,		// big black dot x double, remote activates
	     C_PLUS|C_R,		// big nlack dot x plus, remote activates
	     C_BAD,				// big black dot x big white plus
	     C_LIKE|C_ACT|C_PLUS|C_R|C_L,	// big black dot x big black dot, both activate
	     C_BAD,							// big black dot x big white dot
	     C_LIKE|C_ACT2|C_R|C_L},		// big black dot x bit black dot, both activate
		
			
	};
	// true if these pips are compatible
	boolean pipsCompatible(int component,MicropulChip other,int other_component)
	{	int myStart = components[component];
		int hisStart = other.components[other_component];
		int code = pipCompat[myStart][hisStart];
		return(code!=C_BAD);
	}
	// true if these pips attract
	boolean pipsAttach(int component,MicropulChip other,int other_component)
	{	int myStart = components[component];
		int hisStart = other.components[other_component];
		int code = pipCompat[myStart][hisStart];
		return((code&C_LIKE)!=0);
	}
	// number of activations in this pair of pips, either local activated
	// by a remote micropul, or remove activated by a local micropul
	int activationCount(int component,MicropulChip other,int other_component)
	{	int myStart = components[component];
		int hisStart = other.components[other_component];
		int code = pipCompat[myStart][hisStart];
		int ac = ((code&C_ACT2)!=0)
					?2
					:(((code&C_ACT)!=0)?1:0);
		if((code&C_L)!=0)
			{int mask =  (myStart>=BIG_CODE)?1:(1<<component); // big micropul, left has only one component
			 if((mask&blips)!=0) 
			 { 	// local activation already counted
				if((code&C_R)!=0) 
					{ ac--; //shared local and remove activation 
					}
					else 
					{ ac=0;	// purely local activateion
					}
			 }
			 else { blips |= mask; }
			}
		if(((code&C_R)!=0)&&(hisStart>=BIG_CODE))
			{	// remote activation of a big micropul
			if((blips&BIG_REMOTE)!=0) { ac=0; }
			blips |=BIG_REMOTE;
			}
		return(ac);
	}
	// number of new turns in this pair of pips, either local activated
	// by a remote micropul, or remove activated by a local micropul
	int newturnCount(int component,MicropulChip other,int other_component)
	{	int myStart = components[component];
		int hisStart = other.components[other_component];
		int p1[] = pipCompat[myStart];
		int code = p1[hisStart];
		int ac = ((code&C_PLUS2)!=0)
				?2
				:(((code&C_PLUS)!=0)?1:0);
		if((code&C_L)!=0)
		{int mask =  (myStart>=BIG_CODE)?1:(1<<component); 	// big micropul, left has only one component
		 if((mask&blips)!=0) 
		 { 	// local activation already counted
			if((code&C_R)!=0) 
				{ ac--; //shared local and remove activation 
				}
				else 
				{ ac=0;	// purely local activateion
				}
		 }
		 else { blips |= mask; }
		 }
		
		if(((code&C_R)!=0)&&(hisStart>=BIG_CODE))
		{	// remote activation of a big micropul
		if((blips&BIG_REMOTE)!=0) { ac=0; }
		blips |=BIG_REMOTE;
		}
		return(ac);
	}
	public boolean isJewel() { return(index>=JEWEL_OFFSET); }
	public int getComponent(int n) { return(components[n]); }
	public static int micropulType(int pip)
	{
		switch(pip)
		{
		default: //G.Error("Not a component value");
		case x:
		case C1:
		case C2:
		case PL: return(x);
		
		case BM:
		case BM4P:
		case BM41: 
			return(BM);
		case WM:
		case WM4P:
		case WM41:
			return(WM);
		}
	}
	public static boolean isMicropul(int pip)
	{	switch(pip)
		{
		default: throw G.Error("Not a component value");
		case x:
		case C1:
		case C2:
		case PL: return(false);
		case BM:
		case WM:
		case WM4P:
		case BM4P:
		case WM41:
		case BM41:	return(true);
		}
	}
	// jewels must be placed on a micropul that is not already claimed
	// rotation here refers to the quadrant we are going to place over
	// 0 = upper left
	public boolean legalToPlaceJewel(MicropulCell c,int rota)
	{	if(isJewel()) { return(false); }
		int position = (4+rota-c.rotation[0])%4;
		int pip = components[position];
		return(isMicropul(pip) && (c.tintCode(rota)==null));
	}
	
	// legal to place this chip on this cell. Depends on having
	// a matching pip, and not having any opposing pip
    public boolean legalToPlaceMicropul(MicropulCell c,int rota)
    {
    	boolean some_attach = false;
    	if(isJewel()) { return(c.topChip()!=null); }
    	for(int dir=0;dir<c.geometry.n;dir++)
    	{
    		MicropulCell adj = c.exitTo(dir);		// adjacent in some direction
    		MicropulChip top = adj.bottomChip();
    		if(top!=null) 
    			{ 
    			  int ldr1 = (4+dir-rota)%4;
    			  int odr1 = (dir+2-1+4-adj.rotation[0])%4;
    			  int ldr2 = (4+ldr1-1)%4;
    			  int odr2 = (odr1+1)%4;
    			  //G.print("dir "+dir+":"+rota+" ldir1 "+ldr1+"<>"+odr1+" "+ldr2+" "+ldr2+"<>"+odr2);
    			  // must not be adjacent to an opposing micropul, marked by c_bad
     			  if(!pipsCompatible(ldr1,top,odr1)) { return(false); }
     			  if(!pipsCompatible(ldr2,top,odr2)) { return(false); }
     			  // must be adjacent to some friendly micropul
    			  if(!some_attach) { some_attach |= pipsAttach(ldr1,top,odr1); }
    			  if(!some_attach) { some_attach |= pipsAttach(ldr2,top,odr2); }
    			}
    	}
    	//if(some_attach) { G.print("ok"); } else { G.print("allowed"); }
    	return(some_attach);
    }
    
    // return a rotation value that allows this chip to be placed, or -1
    public int legalMicropulRotation(MicropulCell c)
    {	for(int i=0;i<4;i++) { if(legalToPlaceMicropul(c,i)) { return(i); }}
    	return(-1);
    }
    
    // count the number of activations caused by this placement
    public int countActivations(MicropulCell c)
    {	int activations = 0;
    	int rota = c.rotation[0];
    	blips = 0;
    	for(int dir=0;dir<c.geometry.n;dir++)
    	{
    		MicropulCell adj = c.exitTo(dir);		// adjacent in some direction
    		MicropulChip top = adj.bottomChip();
    		if(top!=null) 
    			{ 
    			  int ldr1 = (4+dir-rota)%4;
    			  int odr1 = (dir+2-1+4-adj.rotation[0])%4;
    			  int ldr2 = (4+ldr1-1)%4;
    			  int odr2 = (odr1+1)%4;
    			  blips &= ~BIG_REMOTE;
    			  activations += activationCount(ldr1,top,odr1);
    			  activations += activationCount(ldr2,top,odr2);
    			  
     			  //G.print("dir "+dir+":"+rota+" ldir1 "+ldr1+"<>"+odr1+" "+ldr2+" "+ldr2+"<>"+odr2);
    			  // must not be adjacent to an opposing micropul, marked by c_bad
    			}
    	}
    	//if(some_attach) { G.print("ok"); } else { G.print("allowed"); }
    	return(activations);
    }
    
    // count the number of new turns generated by this placement
    public int countNewturns(MicropulCell c)
    {	int newturns = 0;
    	int rota = c.rotation[0];
    	blips = 0;
    	for(int dir=0;dir<c.geometry.n;dir++)
    	{
    		MicropulCell adj = c.exitTo(dir);		// adjacent in some direction
    		MicropulChip top = adj.bottomChip();
    		if(top!=null) 
    			{ 
    			  int ldr1 = (4+dir-rota)%4;
    			  int odr1 = (dir+2-1+4-adj.rotation[0])%4;
    			  int ldr2 = (4+ldr1-1)%4;
    			  int odr2 = (odr1+1)%4;
    			  blips &= ~BIG_REMOTE;
    			  newturns += newturnCount(ldr1,top,odr1);
    			  newturns += newturnCount(ldr2,top,odr2);
     			  //G.print("dir "+dir+":"+rota+" ldir1 "+ldr1+"<>"+odr1+" "+ldr2+" "+ldr2+"<>"+odr2);
    			  // must not be adjacent to an opposing micropul, marked by c_bad
    			}
    	}
    	//if(some_attach) { G.print("ok"); } else { G.print("allowed"); }
    	return(newturns);
    }   
    
    // fake contents for jewels and tints
    static final int jewelpieces[][] = 
    {
    	{RJ,RJ,RJ,RJ},
    	{BJ,BJ,BJ,BJ},
    	{RT,RT,RT,RT},
    	{BT,BT,BT,BT},
    	{XT,XT,XT,XT}
    };
    static final int blankpieces[] = {x,x,x,x};
    // contents for the chips
	static final int pieces[][] = 
	{	// clockwise from upper left corner
		{WM,WM,BM,BM},
		{BM,x,x,x},
		{WM,BM,x,PL},
		{BM,WM,x,PL},
		{PL,WM,BM,WM},
		{PL,BM,WM,BM},
		
		{WM,x,C1,x},
		{BM,x,C1,x},
		{WM,BM,C1,x},
		{BM,WM,C1,x},
		{C1,WM,BM,WM},
		{C1,BM,WM,BM},
		
		{WM,x,x,C1},
		{BM,x,x,C1},
		{WM,WM,C1,x},
		{BM,BM,C1,x},
		{WM,WM,WM,C1},
		{BM,BM,BM,C1},
		
		{WM,C1,x,PL},
		{BM,C1,x,PL},
		{WM,WM,x,C2},
		{BM,BM,x,C2},
		{WM,WM,BM,C2},
		{BM,BM,WM,C2},
		
		{WM,C1,C1,x},
		{BM,C1,C1,x},
		{WM,x,WM,x},
		{BM,x,BM,x},
		{WM,WM,BM,WM},
		{BM,BM,WM,BM},
		
		{WM,C2,C1,x},
		{BM,C2,C1,x},
		{WM,x,BM,C1},
		{BM,x,WM,C1},
		{WM,BM,WM,BM},
		{BM,WM,BM,WM},
		
		// two rows of 4
		{WM,PL,WM,C1},
		{BM,PL,BM,C1},
		{BM,BM,WM,WM},
		{WM,x,x,x},

		{WM,C1,BM,C1},
		{BM,C1,WM,C1},
		{WM,WM,WM,WM},
		{BM,BM,BM,BM},
		// four big micropul last
		{WM4P,WM4P,WM4P,WM4P},
		{BM4P,BM4P,BM4P,BM4P},
		{WM41,WM41,WM41,WM41},
		{BM41,BM41,BM41,BM41}
	};
	
	// constructor
	private MicropulChip(int i,Image im,int[]comp,double[]sc,long ran)
	{	index = i;
		components = comp;
		scale=sc;
		image=im;
		randomv = ran;
	}
	public int chipNumber() { return(index); }
	public String contentsString() { return("#"+index); }

	// draw this chip unrotated
	public void drawChip(Graphics gc,exCanvas canvas,int SQUARESIZE,int cx,int cy,String label)
    {	if((index<JEWEL_OFFSET)&&(label==null)) { drawChip(gc,0,canvas,SQUARESIZE,cx,cy,label); }
    	else { super.drawChip(gc,canvas,SQUARESIZE,cx,cy,label); }
    }
	
    private int dys[] = { -1, -1, 1, 1};
    private int dxs[] = { -1, 1, 1, -1};
	// draw this chip rotated.  This involves drawing the pips separately
	// in the appropriate rotated position.
    public void drawChip(Graphics gc,int rotation,exCanvas canvas,int SQUARESIZE,int cx,int cy,String label)
	{ 
      if(gc!=null)
	  {	if(rotation<0) { super.drawChip(gc,canvas,SQUARESIZE,cx,cy,label); return; } 
		if(index>=JEWEL_OFFSET)
	  		{ int step = SQUARESIZE/4;
	  		  int xp = ((rotation==0) || (rotation==3)) ? cx-step : cx+step;
	  		  int yp = (rotation>1) ? cy+step : cy-step;
	  		  super.drawChip(gc,canvas,SQUARESIZE,xp,yp,label); 
	  		  return; 
	  		}
		StockArt alt = Elements[square_index+(((index&3)+rotation)%4)];
		//StockArt alt = base.getAltChip(canvas.getAltChipset());
	 	double pscale[]=alt.getScale();
	    // use this to tune piece position
	    canvas.adjustScales(pscale,alt);
	    canvas.drawImage(gc, alt.getImage(canvas.loader), pscale,cx, cy, SQUARESIZE, 1.0,0.0,null,true);
	    
	    // now draw ornaments
	    int firstx = components[0];
	    if(firstx>=BIG_CODE)
	    {	int cxp = cx-SQUARESIZE/10;
	    	int cxq = cx-SQUARESIZE/15;
	    	int cyq = cy-SQUARESIZE/20;
	    	switch(firstx)
	    	{
	    	default: throw G.Error("not expecting %s",firstx);
	    	case WM4P:	Elements[red_index].drawChip(gc,canvas,SQUARESIZE,cxp,cy,null);	// big white micropul with plus
	    				Elements[plus_index].drawChip(gc,canvas,SQUARESIZE,cxq,cyq,null);
	    		break;
	    	case BM4P:	Elements[blue_index].drawChip(gc,canvas,SQUARESIZE,cxp,cy,null);	// big black microput with plus
						Elements[plus_index].drawChip(gc,canvas,SQUARESIZE,cxq,cyq,null);
	    		break;
	    	case WM41:	Elements[red_index].drawChip(gc,canvas,SQUARESIZE,cxp,cy,null);	// big white micropul with catalyst
						Elements[dot_index].drawChip(gc,canvas,SQUARESIZE,cxp,cy,null);
	    		break;
	    	case BM41:	Elements[blue_index].drawChip(gc,canvas,SQUARESIZE,cxp,cy,null);	// big black micropul with catalyst    
	    				Elements[dot_index].drawChip(gc,canvas,SQUARESIZE,cxp,cy,null);
    			break;
	    	}
	    }
	    else	// draw 4 small ornaments
	    {	int dx = SQUARESIZE/5;
	    	int ss = (SQUARESIZE-SQUARESIZE/4)/2;
	    	int s3 = 2*SQUARESIZE/3;
	    	int idx = (4-rotation)%4;
	    	for(int ii=0;ii<4;ii++)
	    	{	int xx = dxs[ii]*dx+cx;
	    		int yy = dys[ii]*dx+cy;
	    		int comp = components[idx];
	    		switch(comp)
	    		{
	    		case x: break;
	    		case WM: Elements[red_index].drawChip(gc,canvas,ss,xx,yy,null);
	    			break;
	    		case BM: Elements[blue_index].drawChip(gc,canvas,ss,xx,yy,null);
	    			break;
	    		case C1: Elements[dot_index].drawChip(gc,canvas,s3,xx,yy,null);
	    			break;
	    		case C2: 
	    			// keep the double dots pointing parallel to the circumference as we rotate
	    			if((ii&1)!=0)
	    				{Elements[dot_index].drawChip(gc,canvas,s3,xx-dx/4,yy-dx/4,null);
	    				 Elements[dot_index].drawChip(gc,canvas,s3,xx+dx/3,yy+dx/3,null);
	    				}
	    				else
	    				{Elements[dot_index].drawChip(gc,canvas,s3,xx+dx/4,yy-dx/4,null);
	    				 Elements[dot_index].drawChip(gc,canvas,s3,xx-dx/3,yy+dx/3,null);
	    				}
	    			break;
	    		case PL: Elements[plus_index].drawChip(gc,canvas,s3,xx,yy,null);
	    			break;
				default:
					break;
	    		}
	    		idx = (idx+1)%4;
	    	}
	    }
	    
	    // 
	 	  GC.Text(gc,true,cx-SQUARESIZE/2,cy-SQUARESIZE/2,SQUARESIZE,SQUARESIZE,
	 			  null,null,label);
	  }
	}
    //
    // basic image strategy is to use jpg format because it is compact.
    // .. but since jpg doesn't support transparency, we have to create
    // composite images wiht transparency from two matching images.
    // the masks are used to give images soft edges and shadows
    //
	static StockArt Elements[] = null;
    static final double[][] SCALES=
    {{0.48,0.43,2.0}
    ,{0.48,0.43,2.0}
    ,{0.48,0.43,2.0}
    ,{0.48,0.43,2.0}
	,{0.5,0.5,1.7}	// red
	,{0.5,0.5,1.7}	// blue
	,{0.3,0.5,.6}	// dot
	,{0.22,0.5,.5}	// plus
	,{0.5,0.5,0.6}		// jewels
	,{0.5,0.5,0.6}
	,{0.48,0.43,2.0}	// tints
	,{0.48,0.43,2.0}
	,{0.48,0.43,2.0}
	,{0.48,0.43,2.0}	// blank
    };
	static final int square_index = 0;
	static final int red_index = 4;
	static final int blue_index = 5;
	static final int dot_index = 6;
	static final int plus_index = 7;
	static final int jewel_index = 8;
	static final int tint_index = 10;
	static final int blank_index = 13;
    static final String[] ImageNames = 
        {   // square is borrowed from dash.  circles are modified gray ball from zertz
    		// plus is a mutilated cross from the web.
    		"square","square-r1","square-r2","square-r3",
    		"red","blue","dot","plus",
    		"red-jewel","blue-jewel",
    		"red-tint","blue-tint","bad-tint",
    		"blank"
       };
 	// call from the viewer's preloadImages
    static MicropulChip CANONICAL_PIECE[] = null;
    static MicropulChip JEWELS[] = null;
    static MicropulChip TINTS[] = null;
    static MicropulChip BLANK = null;
    static MicropulChip SQUARE = null;	// unadorned scrabble tile
    
    static final MicropulChip getChip(int n) 
    	{ return((n>=JEWEL_OFFSET)?JEWELS[n-JEWEL_OFFSET]:CANONICAL_PIECE[n]); 
    	}
    static final MicropulChip getJewel(int n) 
    	{ return(JEWELS[n]); 
    	}
    static MicropulChip red;
    static MicropulChip blue;

    static final MicropulChip getTint(int n)
    	{ return(TINTS[n]);
    	}
    static final int nChips = pieces.length;
	public static void preloadImages(ImageLoader forcan,String Dir)
	{	if(blue==null)
		{
		Random rv = new Random(5129624);
        Elements = StockArt.preLoadArt(forcan,Dir,ImageNames,SCALES);
        MicropulChip CC[] = new MicropulChip[nChips];
        for(int i=0;i<nChips;i++) 
        	{CC[i]=new MicropulChip(i,Elements[square_index].image,
        			pieces[i],SCALES[square_index],rv.nextLong()); 
        	}
       CANONICAL_PIECE = CC;
       Image.registerImages(CC);
       JEWELS = new MicropulChip[2];
       for(int i=0;i<2;i++) 
       	{JEWELS[i]=new MicropulChip(JEWEL_OFFSET+i,Elements[jewel_index+i].image,
       			jewelpieces[i],SCALES[jewel_index+i],rv.nextLong()); 
       	}
       TINTS = new MicropulChip[3];
       for(int i=0;i<3;i++)
       {
       	 TINTS[i] = new MicropulChip(TINT_OFFSET+i,Elements[tint_index+i].image,
       			jewelpieces[2+i],SCALES[tint_index+i],rv.nextLong());
       	}
       BLANK = new MicropulChip(BLANK_OFFSET,Elements[blank_index].image,
      			blankpieces,SCALES[blank_index],rv.nextLong());
       check_digests(CC);
       SQUARE = new MicropulChip(SQUARE_OFFSET,Elements[square_index].image,null,SCALES[square_index],rv.nextLong());
       
       red = getJewel(0);
       blue = getJewel(1);
		}
	}   
}
